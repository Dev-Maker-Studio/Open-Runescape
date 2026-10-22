import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = (
    "coordinate.native.tsv",
    "vm-coordinates.bin",
    "vm-coordinates.decode.tsv",
    "vm-coordinates.vm.tsv",
    "vm-coordinates-child.bin",
    "vm-coordinates-child.decode.tsv",
)
OPERATIONS = {"COORDX": 93, "COORDY": 298, "COORDZ": 536, "MOVECOORD": 48}


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    counts = Counter()
    random_counts = Counter()
    underflow_counts = Counter()
    full_counts = Counter()
    names = set()
    for line in (output / "coordinate.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        if len(columns) != 7:
            raise SystemExit("Coordinate fixture does not have seven TSV columns")
        name, opcode, operand, initial, result, pointer, final = columns
        if opcode not in OPERATIONS or not 0 <= int(operand) <= 255 or (opcode, name) in names:
            raise SystemExit("Invalid coordinate fixture metadata")
        names.add((opcode, name))
        counts[opcode] += 1
        if name.startswith("random-"):
            random_counts[opcode] += 1
        if name.startswith("full-1000-"):
            if len(initial.split(",")) != 1000:
                raise SystemExit("Full coordinate stack case does not have 1000 inputs")
            full_counts[opcode] += 1
        if name.startswith("underflow-"):
            underflow_counts[opcode] += 1
            if int(pointer) >= 0 or final != "-" or not result.startswith("error:ArrayIndexOutOfBoundsException:"):
                raise SystemExit("Native underflow must retain a negative pointer and exact exception")
        elif result != "ok" or int(pointer) != len(final.split(",")) or initial.split(",")[0] != final.split(",")[0] and not name.startswith("bare-"):
            raise SystemExit("Invalid coordinate success record or damaged lower sentinel")
    if any(random_counts[opcode] != 1024 or full_counts[opcode] != 16 for opcode in OPERATIONS):
        raise SystemExit("Coordinate random or full-stack coverage is incomplete")
    if dict(underflow_counts) != {"COORDX": 2, "COORDY": 2, "COORDZ": 2, "MOVECOORD": 8}:
        raise SystemExit("Coordinate underflow coverage is incomplete")
    decoded = {}
    for stem in ("vm-coordinates", "vm-coordinates-child"):
        records = [line.split("\t") for line in (output / (stem + ".decode.tsv")).read_text().splitlines()]
        coordinate_rows = [row for row in records if len(row) == 4 and row[1] in OPERATIONS]
        decoded[stem] = {
            "instructions": sum(row[0].isdigit() for row in records),
            "counts": [int(value) for value in records[1][1:]],
            "coordinate_instructions": dict(Counter(row[1] for row in coordinate_rows)),
            "coordinate_byte_operands": sorted({int(row[3]) for row in coordinate_rows}),
        }
    if decoded["vm-coordinates"]["coordinate_byte_operands"] != list(range(256)):
        raise SystemExit("Decoded root does not cover every ignored byte operand")
    vm_records = [line.split("\t") for line in (output / "vm-coordinates.vm.tsv").read_text().splitlines()]
    if vm_records[0][0] != "instructions":
        raise SystemExit("Native VM execution did not produce instruction records")
    return {
        "native_cases": sum(counts.values()),
        "cases_per_operation": dict(counts),
        "random_cases_per_operation": dict(random_counts),
        "underflow_cases_per_operation": dict(underflow_counts),
        "full_1000_stack_cases_per_operation": dict(full_counts),
        "decoded_scripts": decoded,
        "native_vm_instructions": int(vm_records[0][1]),
        "native_vm_records": dict(Counter(row[0] for row in vm_records[1:])),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or crate / "target/native-coordinates").resolve()
    allowed = (
        crate / "target",
        root / "code/harness/target/loop/iteration14-validation/cs2",
    )
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or code/harness/target/loop/iteration14-validation/cs2")
    source_paths = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Script.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2CallFrame.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CS2Dispatch.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CoreVmOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/PlayerWorldOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/buffer/Packet.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/Utils.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/CoordinateFixtures.java",
        "code/cs2/tests/native/generate_coordinates.py",
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
            raise SystemExit("Native coordinate work paths must not resolve outside allowed evidence directories")
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    copied_before = digest(copied)
    if copied_before != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    try:
        subprocess.run([
            "javac", "-cp", str(copied), "-d", str(classes),
            str(crate / "tests/native/NativeFixtures.java"),
            str(crate / "tests/native/CoordinateFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "CoordinateFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during coordinate fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Native coordinate oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected coordinate oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Actual PlayerWorldOps.dispatch for direct coordinate cases, CS2Script.decodeScript for decoded fixtures, and CS2Executor.executeScript via unchanged NativeFixtures.execution for root/parameterized child execution. All invoked from an isolated COPY of the pre-existing pinned shaded client JAR; no coordinate expression is reimplemented as an oracle.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "source_provenance": "Hashes identify the current relevant Kotlin sources and unchanged shared Java helper, not a source rebuild; the pinned pre-existing JAR is the executable oracle.",
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/CoordinateFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "opcodes": {name: {"wire": wire, "family": 7, "has_int_operand": False} for name, wire in OPERATIONS.items()},
        **statistics,
        "random_seed": "0x727c520e (java.util.Random)",
        "move_cross_product": {"values": [-2147483648, -2147483647, -16384, -1, 0, 1, 7, 8, 15, 16, 16383, 2147483647], "cases": 20736},
        "native_tsv_encoding": "Seven columns without header: case name, opcode name, unsigned byte operand, initial int stack (comma-separated signed decimal), native result (ok or error:exception-simple-class:exact-message; dash for null message), native intStackPtr (signed decimal), final visible int stack. Dash encodes an empty initial/final stack, including all negative-pointer outcomes. Negative native pointers are captured before any array slicing.",
        "decode_tsv_encoding": "Unchanged NativeFixtures.fixture output: name and six local/argument counts, then instruction index, opcode, operand kind, decoded value. .bin is native CS2 bytecode with unsigned byte operands for all four coordinate instructions.",
        "vm_tsv_encoding": "Unchanged NativeFixtures.execution output for vm-coordinates: exact native instruction count, typed final int/string/long stack records, typed root locals, and five logical global-array records. Strings are UTF-8 hex prefixed hex:; null strings are null.",
        "coverage": [
            "Lower int sentinel 727 on ordinary cases, bare unary stack cases, every bit position with one-hot/complement and neighboring positive/negative boundaries, signed extrema and packing edges.",
            "Every full plane nibble 0..15 crossed with both 14-bit field boundaries 0,1,8191,8192,16382,16383; MOVE identity, carry, borrow and out-of-field deltas.",
            "All four MOVE arguments crossed over 12 signed/packing/plane edge values, four axis-focused edge and bit suites, and 1024 deterministic full-width random cases for each opcode.",
            "Every byte operand 0..255 both in native direct-case intOpValues and through actual decoded executor dispatch. Selected signed-shift, carry and overflow results survive parameterized child returns.",
            "Unary empty-stack and MOVE 0..3-stack underflows with byte operands 0/255 preserve exact native exception class/message and negative pointer; sixteen full 1000-element input stacks per opcode.",
            "Direct dispatch asserts unchanged string/long sentinels, locals and instruction/operand state. Decoded parent retains lower int/string/long sentinels and root locals across five gosubs to child 900048 (5 int locals, 4 int args, 1 string arg, 1 long arg).",
        ],
        "limitations": [
            "Only COORDX, COORDY, COORDZ and MOVECOORD are covered; other family-7 operations, especially live-player COORD, are excluded.",
            "No game entry point, client/server/lobby process, supplied cache, accounts, network or display is used. Only isolated javac/java fixture tests run; no JAR/source rebuild or live JAR changes.",
            "The direct TSV records native failure pointer mutation, not a Rust boundary policy. Expected errors are captured from actual native dispatch; no errors are injected into decoded execution.",
            "Random coverage is deterministic and finite, not exhaustive over all 32-bit inputs. Full JVM exception messages and Java version are pinned in the generated evidence and may differ on another JDK.",
            "Final VM stack/local/instruction records are native exact results, not a per-instruction trace; NativeFixtures.java and all pre-existing fixtures/manifests are left unchanged.",
        ],
        "reproduction": "python3 code/cs2/tests/native/generate_coordinates.py --check",
        "generation": "python3 code/cs2/tests/native/generate_coordinates.py",
        "work_directory": "Default code/cs2/target/native-coordinates; optional --work-dir must resolve inside code/cs2/target or code/harness/target/loop/iteration14-validation/cs2. Only the copied JAR/classes and regenerated outputs are written there; the work directory is not part of fixture identity.",
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "coordinate-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native coordinate fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native coordinate manifest differs")
        print(f"Verified {len(FIXTURES)} native coordinate fixture files, {statistics['native_cases']} cases, exact source/driver hashes and unchanged pinned original/copied JAR hashes")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native coordinate fixture files, {statistics['native_cases']} cases and provenance manifest")
    print(json.dumps(statistics, sort_keys=True))


if __name__ == "__main__":
    main()
