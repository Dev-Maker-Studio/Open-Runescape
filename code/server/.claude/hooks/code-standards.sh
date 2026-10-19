#!/usr/bin/env bash
# PreToolUse hook (Edit|Write|MultiEdit|NotebookEdit): injects the project code standards into
# Claude's context before every file edit, for every developer who clones the repo. Edits under
# the cache source tree's clientscripts, interfaces or graphics also get the cs2-engineer skill;
# world content and cache definitions get the content-development skill, and world sources also
# get the kotlin-plugins skill.
set -euo pipefail
input="$(cat)"
skills="$(cd "$(dirname "$0")/.." && pwd)/skills"
strip() { awk 'BEGIN{fm=0} /^---$/{fm++; next} fm>=2{print}' "$1"; }
body=""
[ -f "$skills/darkan-code-standards/SKILL.md" ] && body="$(strip "$skills/darkan-code-standards/SKILL.md")"
path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // .tool_input.notebook_path // ""')"
case "$path" in
  *cache-source/clientscripts/*|*cache-source/interfaces/*|*cache-source/graphics/*)
    [ -f "$skills/cs2-engineer/SKILL.md" ] && body="$body"$'\n\n'"$(strip "$skills/cs2-engineer/SKILL.md")" ;;
  *world/src/main/*|*world/src/test/*|*cache-source/config*|*cache-source/models/*|*cache-source/gamevals/*|*docs/reference/*)
    [ -f "$skills/content-development/SKILL.md" ] && body="$body"$'\n\n'"$(strip "$skills/content-development/SKILL.md")"
    case "$path" in
      *world/src/main/*) [ -f "$skills/kotlin-plugins/SKILL.md" ] && body="$body"$'\n\n'"$(strip "$skills/kotlin-plugins/SKILL.md")" ;;
    esac ;;
esac
[ -n "$body" ] || exit 0
jq -n --arg ctx "$body" '{hookSpecificOutput: {hookEventName: "PreToolUse", additionalContext: $ctx}}'
