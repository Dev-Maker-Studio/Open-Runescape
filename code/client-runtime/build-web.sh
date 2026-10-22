#!/bin/sh
set -e
cd "$(dirname "$0")"
cargo build --release --lib --target wasm32-unknown-unknown --offline
target_dir=$(cargo metadata --format-version 1 --no-deps --offline | python3 -c 'import json, sys; print(json.load(sys.stdin)["target_directory"])')
wasm-bindgen --target web --no-typescript --out-dir ../web/static --out-name protocol-runtime "$target_dir/wasm32-unknown-unknown/release/openrs_client_runtime.wasm"
