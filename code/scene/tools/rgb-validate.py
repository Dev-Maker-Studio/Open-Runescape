import json
import os
from pathlib import Path
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parent.parent
EVIDENCE = ROOT.parent / "harness/target/loop/iteration14-validation/rgb"
COMMANDS = [
    ("rgb-lock-offline", ["cargo", "generate-lockfile", "--offline"], {}),
    ("rgb-native-oracle", [
        sys.executable, str(ROOT / "tools/rgb-oracle.py"),
        str(ROOT / "target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"),
    ], {}),
    ("rgb-format", ["cargo", "fmt", "--check"], {}),
    ("rgb-native-release-build", ["cargo", "build", "--release", "--offline"], {}),
    ("rgb-native-release-tests", ["cargo", "test", "--release", "--offline"], {}),
    ("rgb-native-clippy", ["cargo", "clippy", "--offline", "--all-targets", "--", "-D", "warnings"], {}),
    ("rgb-wasm-release-build", ["cargo", "build", "--release", "--offline", "--target", "wasm32-unknown-unknown"], {}),
    ("rgb-wasm-clippy", ["cargo", "clippy", "--offline", "--target", "wasm32-unknown-unknown", "--all-targets", "--", "-D", "warnings"], {}),
    ("rgb-wasm-release-tests", [
        "cargo", "test", "--release", "--offline", "--target", "wasm32-unknown-unknown", "--test", "rgb",
    ], {"CARGO_TARGET_WASM32_UNKNOWN_UNKNOWN_RUNNER": "wasm-bindgen-test-runner"}),
]


def main():
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    results = []
    for name, command, overrides in COMMANDS:
        start = time.monotonic()
        result = subprocess.run(
            command, cwd=ROOT, env={**os.environ, **overrides},
            stdin=subprocess.DEVNULL, capture_output=True, text=True,
            check=False, timeout=600,
        )
        (EVIDENCE / f"{name}.stdout").write_text(result.stdout)
        (EVIDENCE / f"{name}.stderr").write_text(result.stderr)
        results.append({
            "name": name,
            "command": command,
            "env": overrides,
            "exit_code": result.returncode,
            "seconds": round(time.monotonic() - start, 3),
        })
        print(f"{name}: exit={result.returncode}", flush=True)
        if result.returncode:
            print(result.stdout + result.stderr, file=sys.stderr, flush=True)
    failures = sum(result["exit_code"] != 0 for result in results)
    (EVIDENCE / "rgb-validation.json").write_text(json.dumps({
        "status": "passed" if failures == 0 else "failed",
        "results": results,
        "kotlin_build": "centralized isolated build owned by parent; not run by RGB implementation agent",
        "scope": "immutable native-compatible HSL RGB lookup dependency; HSV and independent world/pixel equivalence not claimed",
    }, indent=2) + "\n")
    return int(failures != 0)


if __name__ == "__main__":
    sys.exit(main())
