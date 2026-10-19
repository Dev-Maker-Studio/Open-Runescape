#!/usr/bin/env bash
# Decompiles scripts from the packed cache and diffs each against its source in the tree.
# usage: readback.sh <id|name>...    (CACHE_PATH defaults to ./data/cache, TREE to ./cache-source)
# A type-only diff (obj vs graphic parameter, a bare 18637 for Obj.obj_18637, renumbered locals) is the
# decompiler's inference, not the bytecode; keep the source. A structural diff is a real mismatch.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
TOOLS=$ROOT/tools/build/install/tools/bin/tools
TREE=${TREE:-$ROOT/cache-source}
export CACHE_PATH=${CACHE_PATH:-$ROOT/data/cache/}
OUT=${OUT:-$(mktemp -d -t readback-XXXXXX)}
status=0
for ref in "$@"; do
  if [[ $ref =~ ^[0-9]+$ ]]; then id=$ref; else
    id=$(python3 -c "import json,sys; m=json.load(open('$TREE/gamevals/clientscript.json'))['entries']; print(next((k for k,v in m.items() if isinstance(v,str) and (v=='$ref' or v.endswith(',$ref]'))), ''))")
    [ -n "$id" ] || { echo "$ref: not in gamevals/clientscript.json"; status=1; continue; }
  fi
  "$TOOLS" cs2 decompile "$id" --source none 2>/dev/null | grep -v '^Darkan Tools\|^Cache loaded' > "$OUT/$id.ts"
  header=$(grep -m1 '^// ' "$OUT/$id.ts")
  src=$(grep -rlxF --include='*.ts' -- "$header" "$TREE/clientscripts" | head -1)
  [ -n "$src" ] || { echo "$id: no source with header '$header'"; status=1; continue; }
  if diff -B <(grep -v '^///' "$src") <(grep -v '^///' "$OUT/$id.ts") > "$OUT/$id.diff"; then
    echo "$id: matches ${src#$TREE/}"
  else
    echo "$id: differs from ${src#$TREE/} ($OUT/$id.diff)"; status=1
    head -${LINES_SHOWN:-40} "$OUT/$id.diff"
  fi
done
exit $status
