#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 [--repo <path>] [--previous-tag <tag>] [--current <ref>] --version <version> --output <path>" >&2
  exit 64
}

repo="."
previous_tag=""
current_ref="${GITHUB_SHA:-HEAD}"
version=""
output=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --repo)
      [ "$#" -ge 2 ] || usage
      repo="$2"
      shift 2
      ;;
    --previous-tag)
      [ "$#" -ge 2 ] || usage
      previous_tag="$2"
      shift 2
      ;;
    --current)
      [ "$#" -ge 2 ] || usage
      current_ref="$2"
      shift 2
      ;;
    --version)
      [ "$#" -ge 2 ] || usage
      version="$2"
      shift 2
      ;;
    --output)
      [ "$#" -ge 2 ] || usage
      output="$2"
      shift 2
      ;;
    *)
      usage
      ;;
  esac
done

[ -n "$version" ] || usage
[ -n "$output" ] || usage

git_in_repo() {
  git -C "$repo" "$@"
}

if [ -z "$previous_tag" ]; then
  previous_tag="$(git_in_repo describe --tags --abbrev=0 "${current_ref}^" 2>/dev/null || true)"
fi

[ -n "$previous_tag" ] || {
  echo "could not identify previous release tag; pass --previous-tag" >&2
  exit 65
}

git_in_repo rev-parse --verify "$previous_tag^{commit}" >/dev/null
current_commit="$(git_in_repo rev-parse --verify "$current_ref^{commit}")"
range="$previous_tag..$current_commit"
short_current="${current_commit:0:12}"

mkdir -p "$(dirname "$output")"

mapfile -t fork_changes < <(
  git_in_repo log --reverse --pretty=format:'- %s (%h)' "$range" |
    grep -Eiv '^- Merge (integration/)?(runelite|microbot)\b' || true
)
mapfile -t runelite_updates < <(
  git_in_repo log --reverse --pretty=format:'- %s (%h)' "$range" |
    grep -Ei '^- Merge (integration/)?runelite\b' || true
)
mapfile -t microbot_updates < <(
  git_in_repo log --reverse --pretty=format:'- %s (%h)' "$range" |
    grep -Ei '^- Merge (integration/)?microbot\b' || true
)

write_section() {
  local title="$1"
  shift
  local entries=("$@")

  {
    printf '\n## %s\n\n' "$title"
    if [ "${#entries[@]}" -eq 0 ]; then
      printf '_None detected in this range._\n'
    else
      printf '%s\n' "${entries[@]}"
    fi
  } >> "$output"
}

{
  printf '# Release %s\n\n' "$version"
  printf 'Previous release: %s\n' "$previous_tag"
  printf 'Current commit: %s\n' "$short_current"
  printf 'Commit range: `%s..%s`\n' "$previous_tag" "$short_current"
} > "$output"

write_section "Fork Changes" "${fork_changes[@]}"
write_section "Upstream RuneLite Updates" "${runelite_updates[@]}"
write_section "Upstream Microbot Updates" "${microbot_updates[@]}"

cat >> "$output" <<'MARKDOWN'

## Maintainer Review

Review and edit these notes before publishing the final GitHub release.
MARKDOWN

echo "wrote $output"
