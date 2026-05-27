#!/usr/bin/env bash
set -euo pipefail

workflow="${1:?usage: $0 <workflow> <channel>}"
channel="${2:?usage: $0 <workflow> <channel>}"

fail() {
  echo "channel release workflow validation failed: $*" >&2
  exit 1
}

case "$channel" in
  beta|nightly) ;;
  *) fail "unsupported release channel: $channel" ;;
esac

[ -f "$workflow" ] || fail "$channel workflow does not exist: $workflow"

if [ "$channel" = "beta" ]; then
  grep -q 'workflow_dispatch:' "$workflow" \
    || fail "beta workflow must be manually dispatched"

  grep -q 'release_candidate_ref:' "$workflow" \
    || fail "beta workflow must require an explicit release_candidate_ref input"

  grep -q 'ref: \${{ inputs.release_candidate_ref }}' "$workflow" \
    || fail "beta workflow must check out the explicit release_candidate_ref input"
fi

grep -q "scripts/generate-release-metadata.sh[[:space:]]*\\\\" "$workflow" \
  || fail "$channel workflow must generate channel metadata"

grep -q "scripts/validate-release-artifacts.sh[[:space:]]*\\\\" "$workflow" \
  || fail "$channel workflow must validate generated artifacts before publication"

grep -q "^[[:space:]]*$channel[[:space:]]*\\\\" "$workflow" \
  || fail "$channel workflow must pass channel '$channel' to metadata generation"

grep -q "microbot-$channel-\${{ steps.version.outputs.version }}.jar" "$workflow" \
  || fail "$channel workflow must use microbot-$channel-<version>.jar"

release_files=$(awk '
  /name: Create Release/ { in_step=1 }
  in_step && /files:/ { in_files=1; next }
  in_files && /^[^[:space:]-]/ { exit }
  in_files { print }
' "$workflow")

printf '%s\n' "$release_files" | grep -q "microbot-$channel-\*.jar" \
  || fail "GitHub Release upload must include microbot-$channel-*.jar"

printf '%s\n' "$release_files" | grep -q "microbot-$channel-\*.jar.sha256" \
  || fail "GitHub Release upload must include microbot-$channel-*.jar.sha256"

printf '%s\n' "$release_files" | grep -q "update-$channel.json" \
  || fail "GitHub Release upload must include update-$channel.json"

grep -q "source: .*microbot-$channel-\*\.jar" "$workflow" \
  || fail "$channel file hosting upload must include microbot-$channel-*.jar"

grep -q "source: .*microbot-$channel-\*\.jar\.sha256" "$workflow" \
  || fail "$channel file hosting upload must include microbot-$channel-*.jar.sha256"

grep -q "source: .*update-$channel\.json" "$workflow" \
  || fail "$channel file hosting upload must include update-$channel.json"

if grep -qE 'cosign|\.bundle|RELEASE_SIGNING_KEY|SIGNING_PASSPHRASE|GPG_PRIVATE_KEY' "$workflow"; then
  fail "$channel workflow must not introduce release signature artifacts or signing secrets"
fi

echo "channel release workflow validation passed"
