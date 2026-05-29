#!/usr/bin/env bash
set -euo pipefail

workflow="${1:-.github/workflows/release.yml}"

fail() {
  echo "release signing validation failed: $*" >&2
  exit 1
}

build_permissions=$(awk '
  /^[[:space:]]+build:[[:space:]]*$/ { in_build=1; next }
  in_build && /^[[:space:]]{2}[A-Za-z0-9_-]+:[[:space:]]*$/ { exit }
  in_build && /^[[:space:]]{4}permissions:[[:space:]]*$/ { in_permissions=1; next }
  in_permissions && /^[[:space:]]{4}[A-Za-z0-9_-]+:/ { exit }
  in_permissions { print }
' "$workflow")

printf '%s\n' "$build_permissions" | grep -qE '^[[:space:]]+id-token:[[:space:]]+write[[:space:]]*$' \
  || fail "release workflow must grant id-token: write in build permissions for keyless cosign signing"

printf '%s\n' "$build_permissions" | grep -qE '^[[:space:]]+contents:[[:space:]]+write[[:space:]]*$' \
  || fail "release workflow must grant contents: write in build permissions for release publication"

grep -q 'sigstore/cosign-installer' "$workflow" \
  || fail "release workflow must install cosign"

grep -q 'cosign sign-blob' "$workflow" \
  || fail "release workflow must sign the stable jar with cosign sign-blob"

grep -q -- '--bundle' "$workflow" \
  || fail "release workflow must produce a cosign bundle"

grep -q 'microbot-${{ steps.version.outputs.version }}.jar.bundle' "$workflow" \
  || fail "release workflow must use microbot-<version>.jar.bundle"

grep -q 'scripts/generate-release-notes.sh' "$workflow" \
  || fail "release workflow must generate release notes before creating the GitHub Release"

grep -q -- '--notes-file' "$workflow" \
  || fail "GitHub Release must use generated release notes"

grep -q -- '--draft' "$workflow" \
  || fail "GitHub Release must be created as a draft for maintainer review"

grep -q 'microbot-${{ steps.version.outputs.version }}.jar.bundle' "$workflow" \
  || fail "GitHub Release upload must include microbot-*.jar.bundle"

grep -q 'scripts/validate-release-artifacts.sh[[:space:]]*stable' "$workflow" \
  || fail "release workflow must run validate-release-artifacts.sh stable before publication"

scp_sources=$(grep -E 'source: .*microbot-\*\.jar\.bundle' "$workflow" || true)
[ -n "$scp_sources" ] \
  || fail "stable file hosting upload must include microbot-*.jar.bundle"

if grep -qE 'RELEASE_SIGNING_KEY|SIGNING_PASSPHRASE|GPG_PRIVATE_KEY' "$workflow"; then
  fail "release workflow must not use long-lived production signing key secrets"
fi

echo "release signing workflow validation passed"
