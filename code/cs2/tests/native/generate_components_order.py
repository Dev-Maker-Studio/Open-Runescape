import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = (
    "components-order.native.tsv",
    "components-order.stats.tsv",
    "entity-vars.native.tsv",
    "entity-vars.stats.tsv",
)
EVIDENCE = "code/harness/target/loop/iteration25-validation/cs2"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def stats_of(output, name, expected):
    stats = {}
    for line in (output / f"{name}.stats.tsv").read_text().splitlines():
        opcode, ok, failed = line.split("\t")
        stats[opcode] = {"ok": int(ok), "error": int(failed)}
    if set(stats) != set(expected):
        raise SystemExit(f"{name} must cover exactly {sorted(expected)}")
    for opcode, counts in stats.items():
        if counts["ok"] < 16:
            raise SystemExit(f"Fewer than 16 successful oracle cases for {opcode}")
    return stats


def inspect(output):
    order = stats_of(output, "components-order", (
        "CC_CREATE", "CC_DELETE", "CC_DELETEALL", "CC_FIND", "IF_FIND",
        "CC_SENDTOFRONT", "CC_SENDTOBACK", "IF_SENDTOFRONT", "IF_SENDTOBACK",
        "IF_HASSUB", "IF_HASSUBMODAL", "IF_HASSUBOVERLAY",
    ))
    entity = stats_of(output, "entity-vars", ("GET_VARP_OLD", "GET_VARPBIT_OLD"))
    records = {}
    errors = {}
    for line in (output / "components-order.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        records[columns[0]] = records.get(columns[0], 0) + 1
        if columns[0] == "R" and columns[1] != "ok":
            errors[columns[1]] = errors.get(columns[1], 0) + 1
    if records["T"] != records["R"] or records["Z"] != 1:
        raise SystemExit("Order fixture records are inconsistent")
    entity_records = {}
    entity_errors = {}
    for line in (output / "entity-vars.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        entity_records[columns[0]] = entity_records.get(columns[0], 0) + 1
        if columns[0] == "C" and columns[4] != "ok":
            entity_errors[columns[4]] = entity_errors.get(columns[4], 0) + 1
    return {
        "order_cases": records["T"],
        "order_records": records,
        "order_per_opcode": order,
        "order_error_classes": errors,
        "entity_cases": entity_records["C"],
        "entity_records": entity_records,
        "entity_per_opcode": entity,
        "entity_error_classes": entity_errors,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or root / EVIDENCE / "native-components-order").resolve()
    allowed = (crate / "target", root / EVIDENCE)
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or " + EVIDENCE)
    kotlin = "code/client/src/main/kotlin/com/jagex/game/runetek5/"
    ops = kotlin + "config/rsinterface/cs2/ops/"
    drivers = (
        "NativeFixtures.java",
        "ComponentExtraFixtures.java",
        "ComponentLayerFixtures.java",
        "ComponentOrderFixtures.java",
        "EntityVarFixtures.java",
    )
    source_paths = [
        kotlin + "config/rsinterface/cs2/ClientScriptOpCode.kt",
        kotlin + "config/rsinterface/cs2/CS2Executor.kt",
        kotlin + "config/rsinterface/cs2/CS2Interface.kt",
        ops + "ComponentLifecycleOps.kt",
        ops + "InterfaceQueryOps.kt",
        ops + "CoreVmOps.kt",
        kotlin + "config/rsinterface/SubInterface.kt",
        kotlin + "config/rsinterface/Component.kt",
        kotlin + "config/rsinterface/Interface.kt",
        kotlin + "config/varp/VarpType.kt",
        kotlin + "config/varp/VarpTypeList.kt",
        kotlin + "config/varp/VarpbitType.kt",
        kotlin + "config/varp/VarpbitTypeList.kt",
        kotlin + "entity/pathingentity/player/ConfigNodeMap.kt",
        kotlin + "client/Client.kt",
        "code/cs2/tests/native/generate_components_hooks.py",
    ] + ["code/cs2/tests/native/" + name for name in drivers]
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
            raise SystemExit("Work paths must not resolve outside allowed evidence directories")
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    copied_before = digest(copied)
    if copied_before != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    try:
        subprocess.run(
            ["javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes)]
            + [str(crate / "tests/native" / name) for name in drivers],
            cwd=work, check=True,
        )
        for driver in ("ComponentOrderFixtures", "EntityVarFixtures"):
            subprocess.run([
                "java", "-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
                "-cp", os.pathsep.join([str(classes), str(copied)]),
                driver, str(output),
            ], cwd=work, check=True, timeout=600)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during order fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Order oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected order oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "CC_/IF_ SENDTOFRONT and SENDTOBACK, IF_HASSUB, IF_HASSUBMODAL, IF_HASSUBOVERLAY through the actual ComponentLifecycleOps and InterfaceQueryOps dispatch with real Component, Interface and SubInterface objects and a populated Client.OPEN_INTERFACES, and GET_VARP_OLD and GET_VARPBIT_OLD through the actual CoreVmOps dispatch with a real PlayerEntity ConfigNodeMap, an NpcEntity and a null entity, all run headless from an isolated COPY of the pre-existing pinned shaded client JAR. No operation is reimplemented as an oracle.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "random_seeds": ["0x727c5225 (java.util.Random)", "0x727c5226 (java.util.Random)"],
        "tsv_encoding": "components-order: W, I, C, N, K, T, R, D, Z as in components-layer plus O hash sub-interface-id overlay for each open sub-interface. D keys are iN.M (idHash:parentId:slotId|slotChildren|itemSlots), iN.M.sK for children and iN.sub for the interface subComponents; lists are comma separated idHash/slotId with underscore for a null slot and dash for a null array. entity-vars: V stored player varp, T varp type char code, B varbit definition (id varp start end), A default varbit definition, C opcode entity operand result final int stack (the stack pointer is already incremented when an error is thrown), Z total.",
        **statistics,
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "components-order-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native order fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native order manifest differs")
        print(f"Verified {len(FIXTURES)} native order fixture files")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native order fixture files and provenance manifest")
    print(json.dumps({key: statistics[key] for key in ("order_cases", "entity_cases", "order_error_classes", "entity_error_classes")}, sort_keys=True))


if __name__ == "__main__":
    main()
