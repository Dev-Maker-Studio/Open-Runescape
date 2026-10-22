import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("pure.native.tsv", "pure.stats.tsv")
TABLE = "lower-supplementary.bin"
EVIDENCE = "code/harness/target/loop/iteration20-validation/cs2"
OPERATIONS = ("LOWERCASE", "DATE_ISLEAPYEAR", "DATE_MINUTES_FROMRUNEDAY", "DATE_RUNEDAY_FROMDATE")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    counts = Counter()
    errors = Counter()
    names = set()
    for line in (output / "pure.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        if len(columns) != 9:
            raise SystemExit("Pure fixture does not have nine TSV columns")
        name, opcode = columns[0], columns[1]
        if opcode not in OPERATIONS or (opcode, name) in names:
            raise SystemExit("Invalid pure fixture metadata")
        names.add((opcode, name))
        counts[opcode] += 1
        if columns[5] != "ok":
            errors[opcode + " " + columns[5]] += 1
    if set(counts) != set(OPERATIONS):
        raise SystemExit("Pure fixtures must cover four opcodes")
    return {
        "cases": sum(counts.values()),
        "cases_per_opcode": dict(counts),
        "error_classes": dict(errors),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or root / EVIDENCE / "native-pure").resolve()
    allowed = (crate / "target", root / EVIDENCE)
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or " + EVIDENCE)
    ops = "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/"
    source_paths = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        ops + "MathStringOps.kt",
        ops + "CameraLoginPrefOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/CalendarUtil.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/CalenderUtil.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/PureFixtures.java",
        "code/cs2/tests/native/generate_pure.py",
    ]
    source_hashes = {name: digest(root / name) for name in source_paths}
    original_before = digest(jar)
    if original_before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    work.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    classes = work / "classes"
    output = work / "fixtures"
    for path in (copied, classes, output):
        if not any(path.resolve().is_relative_to(directory) for directory in allowed):
            raise SystemExit("Pure work paths must not resolve outside allowed evidence directories")
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    copied_before = digest(copied)
    if copied_before != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    try:
        subprocess.run([
            "javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes),
            str(crate / "tests/native/NativeFixtures.java"),
            str(crate / "tests/native/PureFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Xmx1g", "-Djava.awt.headless=true", "-Duser.timezone=America/Los_Angeles", "-Duser.language=tr",
            "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "PureFixtures", str(output),
        ], cwd=work, check=True, timeout=600)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during pure fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Pure oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES) | {TABLE}:
        raise SystemExit("Unexpected pure oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Actual MathStringOps.dispatch and CameraLoginPrefOps.dispatch on a real CS2Executor, run headless from an isolated COPY of the pre-existing pinned shaded client JAR, with a non-UTC default time zone and a Turkish default language to prove the operations do not depend on them. No operation is reimplemented as an oracle.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Xmx1g", "-Djava.awt.headless=true", "-Duser.timezone=America/Los_Angeles", "-Duser.language=tr", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/PureFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES + (TABLE,))},
        "table": "src/vm/lower-supplementary.bin: big-endian u32 pairs (code point, lowercase code point) for every supplementary code point whose real LOWERCASE result differs",
        "random_seed": "0x727c5220 (java.util.Random)",
        "tsv_encoding": "Tab separated: case name, opcode, byte operand, input ints (comma separated or dash), input string (hex: UTF-8, null or dash), result (ok or error:exception class), final int stack pointer, final int stack, final string stack (hex:, null, dash for empty).",
        **statistics,
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "pure-oracle.json"
    table_path = crate / "src/vm" / TABLE
    if args.check:
        if not table_path.is_file() or table_path.read_bytes() != (output / TABLE).read_bytes():
            raise SystemExit("Supplementary lowercase table differs")
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native pure fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native pure manifest differs")
        print(f"Verified {len(FIXTURES)} native pure fixture files, {statistics['cases']} cases")
    else:
        shutil.copyfile(output / TABLE, table_path)
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native pure fixture files, {statistics['cases']} cases and provenance manifest")
    print(json.dumps({key: statistics[key] for key in ("cases", "cases_per_opcode", "error_classes")}, sort_keys=True))


if __name__ == "__main__":
    main()
