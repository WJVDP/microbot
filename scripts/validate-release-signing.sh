#!/usr/bin/env bash
set -euo pipefail

workflow="${1:-.github/workflows/release.yml}"

fail() {
  echo "release signing validation failed: $*" >&2
  exit 1
}

grep -qE '^[[:space:]]+id-token:[[:space:]]+write[[:space:]]*$' "$workflow" \
  || fail "release workflow must grant id-token: write for keyless cosign signing"

grep -q 'sigstore/cosign-installer' "$workflow" \
  || fail "release workflow must install cosign"

grep -q 'cosign sign-blob' "$workflow" \
  || fail "release workflow must sign the stable jar with cosign sign-blob"

grep -q -- '--bundle' "$workflow" \
  || fail "release workflow must produce a cosign bundle"

grep -q 'microbot-${{ steps.version.outputs.version }}.jar.bundle' "$workflow" \
  || fail "release workflow must use microbot-<version>.jar.bundle"

release_files=$(awk '
  /name: Create Release/ { in_step=1 }
  in_step && /files:/ { in_files=1; next }
  in_files && /^[^[:space:]-]/ { exit }
  in_files { print }
' "$workflow")
printf '%s\n' "$release_files" | grep -q 'microbot-\*.jar.bundle' \
  || fail "GitHub Release upload must include microbot-*.jar.bundle"

scp_sources=$(grep -E 'source: .*microbot-\*\.jar\.bundle' "$workflow" || true)
[ -n "$scp_sources" ] \
  || fail "stable file hosting upload must include microbot-*.jar.bundle"

if grep -qE 'RELEASE_SIGNING_KEY|SIGNING_PASSPHRASE|GPG_PRIVATE_KEY' "$workflow"; then
  fail "release workflow must not use long-lived production signing key secrets"
fi

echo "release signing workflow validation passed"
