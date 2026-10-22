#!/bin/sh
set -e
cd "$(dirname "$0")"
cargo build --release --lib --target wasm32-unknown-unknown
wasm-bindgen --target web --no-typescript --out-dir ../web/static --out-name renderer "${CARGO_TARGET_DIR:-target}/wasm32-unknown-unknown/release/renderer.wasm"
