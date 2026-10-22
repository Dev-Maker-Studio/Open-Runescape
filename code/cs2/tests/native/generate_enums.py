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
    "enum.native.tsv",
    "enum-defs.tsv",
    "vm-enums.bin",
    "vm-enums.decode.tsv",
    "vm-enums.vm.tsv",
    "vm-enums-child.bin",
    "vm-enums-child.decode.tsv",
)
OPERATIONS = {
    "ENUM": 540,
    "ENUM_STRING": 820,
    "ENUM_HASOUTPUT": 60,
    "ENUM_HASOUTPUT_STRING": 840,
    "ENUM_GETOUTPUTCOUNT": 533,
    "ENUM_GETREVERSECOUNT": 149,
    "ENUM_GETREVERSECOUNT_STRING": 438,
    "ENUM_GETREVERSEINDEX": 904,
    "ENUM_GETREVERSEINDEX_STRING": 86,
}


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    counts = Counter()
    errors = Counter()
    names = set()
    for line in (output / "enum.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        if len(columns) != 7:
            raise SystemExit("Enum fixture does not have seven TSV columns")
        name, opcode, ints, strings, result, final_ints, final_strings = columns
        if opcode not in OPERATIONS or name in names:
            raise SystemExit("Invalid enum fixture metadata")
        names.add(name)
        counts[opcode] += 1
        if result != "ok":
            if not result.startswith("error:"):
                raise SystemExit("Invalid enum result")
            errors[opcode] += 1
    if set(counts) != set(OPERATIONS) or any(counts[o] == 0 or errors[o] == 0 for o in OPERATIONS if o != "ENUM_GETOUTPUTCOUNT" and o != "ENUM_STRING"):
        raise SystemExit("Enum coverage is incomplete")
    decoded = {}
    for stem in ("vm-enums", "vm-enums-child"):
        records = [line.split("\t") for line in (output / (stem + ".decode.tsv")).read_text().splitlines()]
        decoded[stem] = {
            "instructions": sum(row[0].isdigit() for row in records),
            "enum_instructions": dict(Counter(row[1] for row in records if len(row) == 4 and row[1] in OPERATIONS)),
        }
    vm_records = [line.split("\t") for line in (output / "vm-enums.vm.tsv").read_text().splitlines()]
    if vm_records[0][0] != "instructions":
        raise SystemExit("Native VM execution did not produce instruction records")
    definitions = (output / "enum-defs.tsv").read_text().splitlines()
    return {
        "native_cases": sum(counts.values()),
        "cases_per_operation": dict(counts),
        "error_cases_per_operation": dict(errors),
        "definitions": len(definitions),
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
    work = (args.work_dir or crate / "target/native-enums").resolve()
    allowed = (
        crate / "target",
        root / "code/harness/target/loop/iteration18-validation/cs2",
    )
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or code/harness/target/loop/iteration18-validation/cs2")
    source_paths = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Script.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2CallFrame.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CS2Dispatch.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CoreVmOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/EnumEmailFriendOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/enumtype/EnumType.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/enumtype/EnumTypeList.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/buffer/Packet.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/Utils.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/EnumFixtures.java",
        "code/cs2/tests/native/generate_enums.py",
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
            raise SystemExit("Native enum work paths must not resolve outside allowed evidence directories")
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
            str(crate / "tests/native/EnumFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "EnumFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during enum fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Native enum oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected enum oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Actual EnumEmailFriendOps.dispatch (reading EnumType through TypeLists.ENUM) for direct enum cases, CS2Script.decodeScript for decoded fixtures, and CS2Executor.executeScript via unchanged NativeFixtures.execution for root/parameterized child execution. All invoked from an isolated COPY of the pre-existing pinned shaded client JAR; no enum expression is reimplemented as an oracle.",
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
        "driver_sha256": digest(crate / "tests/native/EnumFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "opcodes": {name: {"wire": wire, "family": 8, "has_int_operand": False} for name, wire in OPERATIONS.items()},
        **statistics,
        "random_seed": "0x727c520e (java.util.Random)",
        "native_tsv_encoding": "Seven columns without header: case name, opcode name, unsigned byte operand, initial int stack (comma-separated signed decimal), native result (ok or error:exception-simple-class:exact-message; dash for null message), native intStackPtr (signed decimal), final visible int stack. Dash encodes an empty initial/final stack, including all negative-pointer outcomes. Negative native pointers are captured before any array slicing.",
        "decode_tsv_encoding": "Unchanged NativeFixtures.fixture output: name and six local/argument counts, then instruction index, opcode, operand kind, decoded value. .bin is native CS2 bytecode with unsigned byte operands for all four enum instructions.",
        "vm_tsv_encoding": "Unchanged NativeFixtures.execution output for vm-enums: exact native instruction count, typed final int/string/long stack records, typed root locals, and five logical global-array records. Strings are UTF-8 hex prefixed hex:; null strings are null.",
        "coverage": [
            "All nine ENUM opcodes through the real EnumEmailFriendOps.dispatch with a real EnumTypeList (allocated without a JS5 provider, LRU cache pre-populated with real EnumType objects, absent ids hold the empty EnumType exactly as a missing cache file would).",
            "Twelve definitions covering int and string values arrays, int and string parameter maps, Integer values under a string value type, string parameters under an int value type, size/values mismatch, size with neither table, size zero with a populated array, and non-int key types; plus the absent id 99 and id -1.",
            "Grids over key type, value type, key, value, index and a text pool including null, empty, Latin-1 and non-Latin-1 strings; 2000 deterministic random iterations; full 1000-deep stacks per opcode.",
            "Failure outcomes record exact exception class and message and the post-failure stacks, proving all operands are consumed before validation.",
            "Decoded scripts run through the actual executor including a parameterized child (GOSUB_WITH_PARAMS) and compare instruction counts, stacks, locals and global arrays.",
        ],
        "limitations": [
            "Enum definitions come from an explicit host model, not from the JS5 cache; EnumType.decode is not exercised.",
            "Underflow is not generated: the Kotlin executor fails with an ArrayIndexOutOfBoundsException and a negative stack pointer, which the Rust VM reports as a typed error with unchanged stacks; the Rust tests assert the typed error separately.",
            "No game entry point, network, cache, accounts or display is used; only isolated javac/java fixture runs against a copied JAR.",
        ],
        "reproduction": "python3 code/cs2/tests/native/generate_enums.py --check",
        "generation": "python3 code/cs2/tests/native/generate_enums.py",
        "work_directory": "Default code/cs2/target/native-enums; optional --work-dir must resolve inside code/cs2/target or code/harness/target/loop/iteration18-validation/cs2. Only the copied JAR/classes and regenerated outputs are written there; the work directory is not part of fixture identity.",
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "enum-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native enum fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native enum manifest differs")
        print(f"Verified {len(FIXTURES)} native enum fixture files, {statistics['native_cases']} cases, exact source/driver hashes and unchanged pinned original/copied JAR hashes")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native enum fixture files, {statistics['native_cases']} cases and provenance manifest")
    print(json.dumps(statistics, sort_keys=True))


if __name__ == "__main__":
    main()
