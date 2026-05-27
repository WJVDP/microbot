#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
validator="$repo_root/scripts/validate-release-signing.sh"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

pass() {
  echo "ok - $*"
}

fail() {
  echo "not ok - $*" >&2
  exit 1
}

expect_success() {
  local name="$1"
  local workflow="$2"

  if "$validator" "$workflow" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
    pass "$name"
  else
    cat "$tmpdir/stderr" >&2
    fail "$name"
  fi
}

expect_failure() {
  local name="$1"
  local workflow="$2"
  local expected="$3"

  if "$validator" "$workflow" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
    cat "$tmpdir/stdout" >&2
    fail "$name"
  fi

  if grep -Fq "$expected" "$tmpdir/stderr"; then
    pass "$name"
  else
    cat "$tmpdir/stderr" >&2
    fail "$name"
  fi
}

valid_workflow="$tmpdir/valid-release.yml"
cat >"$valid_workflow" <<'YAML'
name: Release
jobs:
  build:
    permissions:
      contents: write
      id-token: write
    steps:
      - name: Install cosign
        uses: sigstore/cosign-installer@v3
      - name: Sign stable release jar
        run: |
          cosign sign-blob --yes --bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar.bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar
      - name: Create Release
        with:
          files: |
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar.sha256
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar.bundle
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/update-stable.json
      - name: Upload Jar to Hetzner
        with:
          source: runelite-client/build/libs/microbot-*.jar,runelite-client/build/libs/microbot-*.jar.bundle
YAML

id_token_outside_permissions="$tmpdir/id-token-outside-permissions.yml"
cat >"$id_token_outside_permissions" <<'YAML'
name: Release
jobs:
  build:
    env:
      id-token: write
    permissions:
      contents: write
    steps:
      - name: Install cosign
        uses: sigstore/cosign-installer@v3
      - name: Sign stable release jar
        run: |
          cosign sign-blob --yes --bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar.bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar
      - name: Create Release
        with:
          files: |
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar.bundle
      - name: Upload Jar to Hetzner
        with:
          source: runelite-client/build/libs/microbot-*.jar.bundle
YAML

missing_bundle_upload="$tmpdir/missing-bundle-upload.yml"
cat >"$missing_bundle_upload" <<'YAML'
name: Release
jobs:
  build:
    permissions:
      contents: write
      id-token: write
    steps:
      - name: Install cosign
        uses: sigstore/cosign-installer@v3
      - name: Sign stable release jar
        run: |
          cosign sign-blob --yes --bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar.bundle runelite-client/build/libs/microbot-${{ steps.version.outputs.version }}.jar
      - name: Create Release
        with:
          files: |
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/microbot-*.jar.sha256
            /home/runner/work/Microbot/Microbot/runelite-client/build/libs/update-stable.json
      - name: Upload Jar to Hetzner
        with:
          source: runelite-client/build/libs/microbot-*.jar
YAML

expect_success "current release workflow has stable signing outputs" "$repo_root/.github/workflows/release.yml"
expect_success "minimal valid workflow has stable signing outputs" "$valid_workflow"
expect_failure "id-token permission must be granted in permissions block" "$id_token_outside_permissions" "id-token: write"
expect_failure "bundle must be published beside release artifacts" "$missing_bundle_upload" "microbot-*.jar.bundle"
