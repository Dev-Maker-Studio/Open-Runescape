#!/bin/sh
set -e
usage() {
  echo "usage: build-private.sh <private-output-dir>" >&2
  echo "builds the client-runtime and renderer wasm bundles and copies the page sources into the directory" >&2
  echo "env: CARGO_TARGET_DIR_RUNTIME, CARGO_TARGET_DIR_RENDERER (default <crate>/target/private-web)" >&2
  exit 2
}
[ "$#" -eq 1 ] || usage
here=$(cd "$(dirname "$0")" && pwd)
static="$here/static"
mkdir -p "$1"
out=$(cd "$1" && pwd)
shared=$(cd "$static" && pwd)
case "$out/" in
  "$shared"/*) echo "refusing to build into the shared web/static" >&2; exit 1 ;;
esac
case "$shared/" in
  "$out"/*) echo "refusing to build into a directory that contains the shared web/static" >&2; exit 1 ;;
esac
export CARGO_INCREMENTAL=0
runtime_target="${CARGO_TARGET_DIR_RUNTIME:-$here/../client-runtime/target/private-web}"
renderer_target="${CARGO_TARGET_DIR_RENDERER:-$here/../renderer/target/private-web}"
(cd "$here/../client-runtime" && CARGO_TARGET_DIR="$runtime_target" cargo build --release --lib --target wasm32-unknown-unknown --offline)
(cd "$here/../renderer" && CARGO_TARGET_DIR="$renderer_target" cargo build --release --lib --target wasm32-unknown-unknown --offline)
for file in "$static"/*.js "$static"/*.html; do
  name=$(basename "$file")
  case "$name" in
    protocol-runtime.js|renderer.js) continue ;;
  esac
  cp "$file" "$out/$name"
done
wasm-bindgen --target web --no-typescript --out-dir "$out" --out-name protocol-runtime "$runtime_target/wasm32-unknown-unknown/release/openrs_client_runtime.wasm"
wasm-bindgen --target web --no-typescript --out-dir "$out" --out-name renderer "$renderer_target/wasm32-unknown-unknown/release/renderer.wasm"
echo "private web bundle: $out"
