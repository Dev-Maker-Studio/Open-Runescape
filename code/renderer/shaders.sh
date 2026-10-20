#!/bin/sh
set -e
cd "$(dirname "$0")"
cargo run --release --features naga --bin shaderc -- "${1:-../client/src/main/resources/shaders}"
