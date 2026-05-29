#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 4 ]; then
  echo "usage: $0 <channel> <version> <artifact-path> <base-url> [output-dir]" >&2
  exit 64
fi

channel="$1"
version="$2"
artifact_path="$3"
base_url="${4%/}"
output_dir="${5:-$(dirname "$artifact_path")}"

case "$channel" in
  stable|beta|nightly) ;;
  *)
    echo "unsupported release channel: $channel" >&2
    exit 64
    ;;
esac

if [ ! -f "$artifact_path" ]; then
  echo "artifact not found: $artifact_path" >&2
  exit 66
fi

mkdir -p "$output_dir"

artifact_name="$(basename "$artifact_path")"
checksum="$(sha256sum "$artifact_path" | awk '{print $1}')"
size_bytes="$(wc -c < "$artifact_path" | tr -d ' ')"
published_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
commit="${GITHUB_SHA:-$(git rev-parse HEAD 2>/dev/null || true)}"

sha_file="$output_dir/$artifact_name.sha256"
metadata_file="$output_dir/update-$channel.json"

printf '%s  %s\n' "$checksum" "$artifact_name" > "$sha_file"

cat > "$metadata_file" <<JSON
{
  "schemaVersion": 1,
  "channel": "$channel",
  "version": "$version",
  "publishedAt": "$published_at",
  "commit": "$commit",
  "artifact": {
    "name": "$artifact_name",
    "url": "$base_url/$artifact_name",
    "sha256": "$checksum",
    "sha256Url": "$base_url/$artifact_name.sha256",
    "sizeBytes": $size_bytes
  }
}
JSON

echo "wrote $sha_file"
echo "wrote $metadata_file"
