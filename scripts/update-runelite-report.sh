#!/usr/bin/env bash
set -euo pipefail

REMOTE="${1:-runelite-upstream}"
REMOTE_BRANCH="${2:-master}"
BASE_REF="${3:-HEAD}"

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if ! git rev-parse --verify --quiet "${BASE_REF}^{commit}" >/dev/null; then
  echo "Base ref not found: ${BASE_REF}" >&2
  exit 1
fi

if git remote get-url "$REMOTE" >/dev/null 2>&1; then
  git fetch "$REMOTE" "$REMOTE_BRANCH" >/dev/null
fi

UPSTREAM_REF="${REMOTE}/${REMOTE_BRANCH}"
if ! git rev-parse --verify --quiet "${UPSTREAM_REF}^{commit}" >/dev/null; then
  if git rev-parse --verify --quiet "${REMOTE_BRANCH}^{commit}" >/dev/null; then
    UPSTREAM_REF="$REMOTE_BRANCH"
  else
    echo "Upstream ref not found: ${UPSTREAM_REF}" >&2
    exit 1
  fi
fi

print_section() {
  printf '\n## %s\n\n' "$1"
}

print_changed_paths() {
  local title="$1"
  shift

  print_section "$title"
  git diff --name-status "${BASE_REF}...${UPSTREAM_REF}" -- "$@" | sed 's/^/- /' || true
}

base_sha="$(git rev-parse --short "$BASE_REF")"
upstream_sha="$(git rev-parse --short "$UPSTREAM_REF")"
merge_base_sha="$(git merge-base "$BASE_REF" "$UPSTREAM_REF" | xargs git rev-parse --short)"
commit_count="$(git rev-list --count "${BASE_REF}..${UPSTREAM_REF}")"

cat <<REPORT
# RuneLite Update Report

- Base ref: ${BASE_REF} (${base_sha})
- Upstream ref: ${UPSTREAM_REF} (${upstream_sha})
- Merge base: ${merge_base_sha}
- Upstream commits ahead of base: ${commit_count}
REPORT

print_section "Current Version Metadata"
grep -E '^(project\.build\.version|microbot\.version)=' gradle.properties | sed 's/^/- /' || true

print_section "Recent Upstream Commits"
git log --oneline --decorate --no-merges -20 "${BASE_REF}..${UPSTREAM_REF}" | sed 's/^/- /' || true

print_changed_paths "Gradle And Dependency Metadata" \
  "gradle.properties" \
  "libs.versions.toml" \
  "settings.gradle.kts" \
  "build.gradle.kts" \
  "*/build.gradle.kts"

print_changed_paths "RuneLite API Changes" \
  "runelite-api"

print_changed_paths "RuneLite Client Plugin Changes" \
  "runelite-client/src/main/java/net/runelite/client/plugins"

print_changed_paths "Client Lifecycle And Runtime-Sensitive Changes" \
  "runelite-client/src/main/java/net/runelite/client/config" \
  "runelite-client/src/main/java/net/runelite/client/eventbus" \
  "runelite-client/src/main/java/net/runelite/client/ui/overlay" \
  "runelite-client/src/main/java/net/runelite/client/callback" \
  "runelite-client/src/main/java/net/runelite/client/game" \
  "runelite-client/src/main/java/net/runelite/client/RuntimeConfig.java" \
  "runelite-client/src/main/java/net/runelite/client/RuneLite.java"

print_changed_paths "Microbot Compatibility Review Candidates" \
  "runelite-client/src/main/java/net/runelite/client/plugins/microbot" \
  "runelite-client/src/test/java/net/runelite/client/plugins/microbot"

print_changed_paths "Resource And Packaging Changes" \
  "runelite-client/src/main/resources" \
  "runelite-client/src/main/scripts" \
  "runelite-client/src/main/assembly" \
  "cache" \
  "runelite-jshell"

print_section "Suggested Verification"
cat <<'VERIFY'
- ./gradlew :client:compileJava
- ./gradlew :client:runUnitTests
- ./gradlew :client:runUnitTests --tests net.runelite.client.plugins.microbot.threadsafety.ClientThreadGuardrailTest --tests net.runelite.client.plugins.microbot.threadsafety.QueryableTerminalGuardrailTest
- ./gradlew :client:runUnitTests --tests net.runelite.client.plugins.PluginManagerTest.testLoadPlugins
- ./gradlew :client:shadowJar
VERIFY
