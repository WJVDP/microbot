#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
generator="$repo_root/scripts/generate-release-notes.sh"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

pass() {
  echo "ok - $*"
}

fail() {
  echo "not ok - $*" >&2
  exit 1
}

require_line() {
  local file="$1"
  local expected="$2"

  if grep -Fq "$expected" "$file"; then
    pass "$expected"
  else
    cat "$file" >&2
    fail "missing expected release-note text: $expected"
  fi
}

git -C "$tmpdir" init -q
git -C "$tmpdir" config user.name "Microbot Test"
git -C "$tmpdir" config user.email "microbot-test@example.invalid"

printf 'base\n' > "$tmpdir/file.txt"
git -C "$tmpdir" add file.txt
git -C "$tmpdir" commit -q -m "Initial release"
git -C "$tmpdir" tag v1.0.0

printf 'fork\n' >> "$tmpdir/file.txt"
git -C "$tmpdir" commit -q -am "Improve fork launcher"

printf 'runelite\n' >> "$tmpdir/file.txt"
git -C "$tmpdir" commit -q -am "Merge integration/runelite into main"

printf 'microbot\n' >> "$tmpdir/file.txt"
git -C "$tmpdir" commit -q -am "Merge integration/microbot into main"

current_commit="$(git -C "$tmpdir" rev-parse HEAD)"
output="$tmpdir/release-notes.md"

if "$generator" --repo "$tmpdir" --previous-tag v1.0.0 --current "$current_commit" --version v1.1.0 --output "$output" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  pass "generator writes release notes"
else
  cat "$tmpdir/stderr" >&2
  fail "generator writes release notes"
fi

require_line "$output" "# Release v1.1.0"
require_line "$output" "Previous release: v1.0.0"
require_line "$output" "Current commit: ${current_commit:0:12}"
require_line "$output" "## Fork Changes"
require_line "$output" "Improve fork launcher"
require_line "$output" "## Upstream RuneLite Updates"
require_line "$output" "Merge integration/runelite into main"
require_line "$output" "## Upstream Microbot Updates"
require_line "$output" "Merge integration/microbot into main"

auto_output="$tmpdir/auto-release-notes.md"
if "$generator" --repo "$tmpdir" --current "$current_commit" --version v1.1.0 --output "$auto_output" >"$tmpdir/stdout" 2>"$tmpdir/stderr"; then
  pass "generator detects previous release tag"
else
  cat "$tmpdir/stderr" >&2
  fail "generator detects previous release tag"
fi

require_line "$auto_output" "Previous release: v1.0.0"
