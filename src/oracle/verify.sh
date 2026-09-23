#!/bin/sh
# usage: src/oracle/verify.sh <samples.toml> <lakan tests/oracle-schema dir> [DecoderDump options, e.g. --cache <dir> --xtea <json>]
set -eu
samples=$1
schema=$2
shift 2
out=$(mktemp -d)
for run in a b; do
  ./gradlew --no-daemon -q oracleDump --args="--samples $samples --names $schema/names --schema $schema/snapshot.schema.json --out $out/$run $*"
done
diff -r "$out/a" "$out/b"
echo "$(find "$out/a" -name '*.json' | wc -l) snapshots, 2 runs identical, schema valid"
rm -rf "$out"
