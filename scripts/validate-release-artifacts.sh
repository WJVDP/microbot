#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "usage: $0 <channel> <version> <artifact-path>" >&2
  exit 64
fi

channel="$1"
version="$2"
artifact_path="$3"
artifact_name="$(basename "$artifact_path")"
artifact_dir="$(dirname "$artifact_path")"
sha_file="$artifact_path.sha256"
metadata_file="$artifact_dir/update-$channel.json"

fail() {
  echo "release artifact validation failed: $*" >&2
  exit 1
}

case "$channel" in
  stable)
    expected_name="microbot-$version.jar"
    bundle_file="$artifact_path.bundle"
    expected_base_url="https://files.microbot.cloud/releases/microbot/stable"
    ;;
  beta|nightly)
    expected_name="microbot-$channel-$version.jar"
    expected_base_url="https://files.microbot.cloud/releases/microbot/$channel"
    ;;
  *)
    fail "unsupported release channel: $channel"
    ;;
esac

[ "$artifact_name" = "$expected_name" ] \
  || fail "expected artifact name $expected_name, got $artifact_name"

[ -s "$artifact_path" ] || fail "artifact missing or empty: $artifact_path"
[ -s "$sha_file" ] || fail "checksum missing or empty: $sha_file"
[ -s "$metadata_file" ] || fail "metadata missing or empty: $metadata_file"

if [ "$channel" = "stable" ]; then
  [ -s "$bundle_file" ] || fail "stable cosign bundle missing or empty: $bundle_file"
fi

(cd "$artifact_dir" && sha256sum -c "$(basename "$sha_file")") >/dev/null \
  || fail "checksum does not match artifact"

expected_checksum="$(awk '{print $1}' "$sha_file")"
expected_size="$(wc -c < "$artifact_path" | tr -d ' ')"

python3 - "$metadata_file" "$channel" "$version" "$artifact_name" "$expected_base_url" "$expected_checksum" "$expected_size" <<'PY' \
  || fail "metadata shape or artifact fields are invalid"
import json
import re
import sys

metadata_file, channel, version, artifact_name, expected_base_url, expected_checksum, expected_size = sys.argv[1:]

with open(metadata_file, encoding="utf-8") as handle:
    metadata = json.load(handle)

artifact = metadata.get("artifact")
if not isinstance(artifact, dict):
    raise SystemExit("artifact object missing")

expected = {
    "schemaVersion": 1,
    "channel": channel,
    "version": version,
}
for key, value in expected.items():
    if metadata.get(key) != value:
        raise SystemExit(f"{key} mismatch")

if artifact.get("name") != artifact_name:
    raise SystemExit("artifact name mismatch")
if artifact.get("url") != f"{expected_base_url}/{artifact_name}":
    raise SystemExit("artifact URL mismatch")
if not re.fullmatch(r"[0-9a-f]{64}", str(artifact.get("sha256", ""))):
    raise SystemExit("artifact checksum malformed")
if artifact.get("sha256") != expected_checksum:
    raise SystemExit("artifact checksum mismatch")
if artifact.get("sha256Url") != f"{expected_base_url}/{artifact_name}.sha256":
    raise SystemExit("artifact checksum URL mismatch")
if artifact.get("sizeBytes") != int(expected_size):
    raise SystemExit("artifact size mismatch")
PY

echo "release artifact validation passed"
