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
    "inventory.native.tsv",
    "inventory-defs.tsv",
)
OPERATIONS = (
    "INV_GETOBJ", "INV_GETNUM", "INV_TOTAL", "INV_SIZE", "INV_STOCKBASE", "INVOTHER_GETOBJ", "INVOTHER_GETNUM",
    "INVOTHER_TOTAL", "INV_FREESPACE", "INV_TOTALPARAM", "INV_TOTALPARAM_STACK", "INV_GETTOOLTIP",
    "INV_OTHER_GETTOOLTIP", "OC_FIND", "OC_FINDNEXT", "OC_FINDRESTART", "ITEM_FIND_PARAMINT", "ITEM_FIND_PARAMSTR",
)
KOTLIN = "code/client/src/main/kotlin/com/jagex/game/runetek5/"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    counts = Counter()
    errors = Counter()
    scenarios = 0
    for line in (output / "inventory.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        if columns[0] == "S":
            scenarios += 1
            continue
        if columns[0] != "C" or len(columns) != 7:
            raise SystemExit("Inventory fixture row is malformed")
        counts[columns[1]] += 1
        if columns[4] != "ok":
            if not columns[4].startswith("error:"):
                raise SystemExit("Invalid inventory result")
            errors[columns[1] + ":" + columns[4]] += 1
    if set(counts) != set(OPERATIONS):
        raise SystemExit("Inventory coverage is incomplete")
    return {
        "native_cases": sum(counts.values()),
        "scenarios": scenarios,
        "cases_per_operation": dict(counts),
        "error_cases": dict(errors),
        "definitions": len((output / "inventory-defs.tsv").read_text().splitlines()),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or crate / "target/native-inventory").resolve()
    if not work.is_relative_to(crate / "target"):
        raise SystemExit("--work-dir must be inside code/cs2/target")
    source_paths = [
        KOTLIN + "config/rsinterface/cs2/ops/PlayerWorldOps.kt",
        KOTLIN + "config/rsinterface/cs2/ops/ConfigTypeChatOps.kt",
        KOTLIN + "config/rsinterface/cs2/CS2Interpreter.kt",
        KOTLIN + "config/rsinterface/cs2/CS2QueryResults.kt",
        KOTLIN + "config/rsinterface/itemcontainer/ItemContainer.kt",
        KOTLIN + "config/rsinterface/itemcontainer/ItemContainerType.kt",
        KOTLIN + "config/rsinterface/itemcontainer/ItemContainerTypeList.kt",
        KOTLIN + "config/item/ItemType.kt",
        KOTLIN + "config/item/ItemTypeList.kt",
        KOTLIN + "config/param/ParamType.kt",
        KOTLIN + "utils/Utils.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/InventoryFixtures.java",
        "code/cs2/tests/native/generate_inventory.py",
    ]
    source_hashes = {name: digest(root / name) for name in source_paths}
    original_before = digest(jar)
    if original_before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    work.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    classes = work / "classes"
    output = work / "fixtures"
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
            str(crate / "tests/native/InventoryFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "InventoryFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during inventory fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Native inventory oracle sources changed during generation")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Actual PlayerWorldOps.dispatch (INV_* through ItemContainer.CONTAINER_MAP, TypeLists.CONTAINER, TypeLists.OBJ and TypeLists.PARAMS) and ConfigTypeChatOps.dispatch (OC_FIND*, ITEM_FIND_*, through CS2Interpreter.queryItem and CS2QueryResults) invoked from an isolated COPY of the pinned shaded client JAR.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/InventoryFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "opcodes": list(OPERATIONS),
        **statistics,
        "native_tsv_encoding": "Rows S (scenario start, query results reset) and C: opcode name, initial int stack, initial string stack (u + UTF-16 hex units, null for null), result (ok or error:exception-simple-class), final int stack, final string stack. Dash is an empty stack. State persists across the C rows of one scenario.",
        "definitions_encoding": "Rows param, item, ctype and container describing the TypeLists and ItemContainer.CONTAINER_MAP the oracle ran against.",
        "limitations": [
            "Item ids at or above 32768 are not covered: the Kotlin result array stores ids as signed shorts and re-reads names through them.",
            "Containers whose itemIds and amounts differ in length, and container types whose arrays are shorter than defaultItemSize, raise ArrayIndexOutOfBoundsException in Kotlin and are not generated.",
            "No game entry point, network, cache, accounts or display is used; only isolated javac/java fixture runs against a copied JAR.",
        ],
        "reproduction": "python3 code/cs2/tests/native/generate_inventory.py --check",
        "generation": "python3 code/cs2/tests/native/generate_inventory.py",
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "inventory-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native inventory fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native inventory manifest differs")
        print(f"Verified {len(FIXTURES)} native inventory fixture files, {statistics['native_cases']} cases")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native inventory fixture files, {statistics['native_cases']} cases and provenance manifest")
    print(json.dumps(statistics, sort_keys=True))


if __name__ == "__main__":
    main()
