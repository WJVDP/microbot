#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
validator="$repo_root/scripts/validate-channel-release-workflows.sh"
artifact_validator="$repo_root/scripts/validate-release-artifacts.sh"
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
  local channel="$3"

  if "$validator" "$workflow" "$channel" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
    pass "$name"
  else
    cat "$tmpdir/stderr" >&2
    fail "$name"
  fi
}

expect_success "scheduled nightly publishes jar checksum and metadata" "$repo_root/.github/workflows/nightly.yml" nightly
expect_success "manual nightly publishes jar checksum and metadata" "$repo_root/.github/workflows/manual_nightly.yml" nightly
expect_success "manual beta publishes jar checksum and metadata" "$repo_root/.github/workflows/beta.yml" beta

artifact="$tmpdir/microbot-nightly-1.2.3.jar"
printf 'test jar\n' > "$artifact"
GITHUB_SHA=abc123 "$repo_root/scripts/generate-release-metadata.sh" nightly 1.2.3 "$artifact" "https://files.microbot.cloud/releases/microbot/nightly" "$tmpdir" >/dev/null

if "$artifact_validator" nightly 1.2.3 "$artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  pass "artifact validator accepts matching nightly metadata"
else
  cat "$tmpdir/stderr" >&2
  fail "artifact validator accepts matching nightly metadata"
fi

stable_artifact="$tmpdir/microbot-1.2.3.jar"
printf 'stable jar\n' > "$stable_artifact"
GITHUB_SHA=abc123 "$repo_root/scripts/generate-release-metadata.sh" stable 1.2.3 "$stable_artifact" "https://files.microbot.cloud/releases/microbot/stable" "$tmpdir" >/dev/null

if "$artifact_validator" stable 1.2.3 "$stable_artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  cat "$tmpdir/stdout" >&2
  fail "artifact validator rejects stable release without cosign bundle"
else
  pass "artifact validator rejects stable release without cosign bundle"
fi

printf 'bundle\n' > "$stable_artifact.bundle"
if "$artifact_validator" stable 1.2.3 "$stable_artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  pass "artifact validator accepts stable metadata with cosign bundle"
else
  cat "$tmpdir/stderr" >&2
  fail "artifact validator accepts stable metadata with cosign bundle"
fi

bad_metadata_artifact="$tmpdir/microbot-nightly-2.0.0.jar"
printf 'test jar\n' > "$bad_metadata_artifact"
GITHUB_SHA=abc123 "$repo_root/scripts/generate-release-metadata.sh" nightly 2.0.0 "$bad_metadata_artifact" "https://files.microbot.cloud/releases/microbot/nightly" "$tmpdir" >/dev/null
cat > "$tmpdir/update-nightly.json" <<EOF
"channel": "nightly"
"version": "2.0.0"
"name": "microbot-nightly-2.0.0.jar"
"sha256Url": "https://files.microbot.cloud/releases/microbot/nightly/microbot-nightly-2.0.0.jar.sha256"
EOF

if "$artifact_validator" nightly 2.0.0 "$bad_metadata_artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  cat "$tmpdir/stdout" >&2
  fail "artifact validator rejects malformed update metadata"
else
  pass "artifact validator rejects malformed update metadata"
fi

bad_url_artifact="$tmpdir/microbot-beta-2.0.1.jar"
printf 'test jar\n' > "$bad_url_artifact"
GITHUB_SHA=abc123 "$repo_root/scripts/generate-release-metadata.sh" beta 2.0.1 "$bad_url_artifact" "https://files.microbot.cloud/releases/microbot/beta" "$tmpdir" >/dev/null
python3 - "$tmpdir/update-beta.json" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path, encoding="utf-8") as handle:
    metadata = json.load(handle)
metadata["artifact"]["url"] = "https://example.invalid/microbot-beta-2.0.1.jar"
with open(path, "w", encoding="utf-8") as handle:
    json.dump(metadata, handle)
PY

if "$artifact_validator" beta 2.0.1 "$bad_url_artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  cat "$tmpdir/stdout" >&2
  fail "artifact validator rejects unexpected artifact URL"
else
  pass "artifact validator rejects unexpected artifact URL"
fi

bad_checksum_metadata_artifact="$tmpdir/microbot-nightly-2.0.2.jar"
printf 'test jar\n' > "$bad_checksum_metadata_artifact"
GITHUB_SHA=abc123 "$repo_root/scripts/generate-release-metadata.sh" nightly 2.0.2 "$bad_checksum_metadata_artifact" "https://files.microbot.cloud/releases/microbot/nightly" "$tmpdir" >/dev/null
python3 - "$tmpdir/update-nightly.json" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path, encoding="utf-8") as handle:
    metadata = json.load(handle)
metadata["artifact"]["sha256"] = "0" * 64
with open(path, "w", encoding="utf-8") as handle:
    json.dump(metadata, handle)
PY

if "$artifact_validator" nightly 2.0.2 "$bad_checksum_metadata_artifact" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  cat "$tmpdir/stdout" >&2
  fail "artifact validator rejects metadata checksum mismatch"
else
  pass "artifact validator rejects metadata checksum mismatch"
fi
