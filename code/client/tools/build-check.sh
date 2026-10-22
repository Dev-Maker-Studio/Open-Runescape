#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p ../harness/target/loop
exec flock ../harness/target/loop/client-build.lock env CI_COMMIT_SHORT_SHA="${CI_COMMIT_SHORT_SHA:-unknown}" ./gradlew build shadowJar --offline --console=plain "$@"
