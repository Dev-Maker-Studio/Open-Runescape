import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = args.jar or root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    sources = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Script.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CoreVmOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/DetailCapabilityOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/ArrayUtils.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/ArraySortFixtures.java",
        "code/cs2/tests/native/generate_array_sort.py",
    ]
    with tempfile.TemporaryDirectory(prefix="openrs-cs2-array-sort-") as directory:
        work = Path(directory)
        copied = work / "native-client.jar"
        shutil.copyfile(jar, copied)
        if digest(copied) != digest(jar):
            raise SystemExit("Native jar copy differs")
        output = work / "fixtures"
        subprocess.run([
            "javac", "-cp", str(copied), "-d", str(work),
            str(crate / "tests/native/NativeFixtures.java"),
            str(crate / "tests/native/ArraySortFixtures.java"),
        ], check=True)
        subprocess.run([
            "java", "-Xss32m", "-cp", os.pathsep.join([str(work), str(copied)]),
            "ArraySortFixtures", str(output),
        ], check=True)
        java = subprocess.run(["java", "-version"], capture_output=True, text=True, check=True)
        files = sorted(output.iterdir())
        manifest = {
            "oracle": "Actual DetailCapabilityOps.dispatch(ARRAY_SORT) and decoded CS2Executor.executeScript from an isolated copy of the pre-existing shaded client jar. No native source rebuild, game entry point, supplied cache, accounts, network or display.",
            "java": java.stderr.splitlines()[0],
            "jar": str(jar.relative_to(root)) if jar.is_relative_to(root) else str(jar),
            "jar_sha256": digest(copied),
            "source_sha256": {name: digest(root / name) for name in sources},
            "fixture_sha256": {path.name: digest(path) for path in files},
            "native_cases": len((output / "array-sort.native.tsv").read_text().splitlines()),
            "stress_cases": 8,
            "stress_pairs_per_case": 5000,
            "native_stack": "-Xss32m permits the native recursive 5000-element maximum-key case; the Rust implementation uses an explicit range stack and no recursion or arbitrary work cutoff.",
            "fixture_encoding": "Ten tab-separated columns: case name, count, key slot, value slot, five logical lengths, five pipe-separated comma-delimited initial arrays, input int stack, native result/error class/message, final int stack, five final arrays. A dash encodes an empty array/stack. Arrays are logical prefixes of native 5000-element backing storage.",
            "coverage": "Exact unstable paired ordering, duplicate keys and values, signed extrema, MAX pivot tie mask, all small prefixes, unequal array lengths, both slot orders, negative/zero/one counts, MIN count decrement overflow, invalid slots and validation order, same-slot rejection, nonconsuming stack underflow, 256 deterministic random cases, eight full-size stress patterns, decoded gosub with shared arrays and ignored byte operand.",
            "reproduction": "python3 code/cs2/tests/native/generate_array_sort.py --check",
        }
        destination = crate / "tests/fixtures"
        manifest_path = destination / "array-sort-oracle.json"
        if args.check:
            for path in files:
                if not (destination / path.name).is_file() or path.read_bytes() != (destination / path.name).read_bytes():
                    raise SystemExit(f"Native fixture differs: {path.name}")
            if json.loads(manifest_path.read_text()) != manifest:
                raise SystemExit("Native array sort manifest differs")
            print(f"Verified {len(files)} native ARRAY_SORT fixtures and exact source/jar hashes")
        else:
            for path in files:
                shutil.copyfile(path, destination / path.name)
            manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
            print(f"Installed {len(files)} native ARRAY_SORT fixtures and provenance manifest")


if __name__ == "__main__":
    main()
