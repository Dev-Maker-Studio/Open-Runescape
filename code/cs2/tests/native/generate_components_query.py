import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("components-query.native.tsv", "components-query.stats.tsv")
OPCODES = (
    "CC_GETPARENTLAYER", "IF_GETPARENTLAYER", "IF_GET_GAMESCREEN",
    "IF_DEBUG_GETOPENIFCOUNT", "IF_DEBUG_GETOPENIFID", "IF_DEBUG_GETNAME",
    "IF_DEBUG_GETCOMCOUNT", "IF_DEBUG_GETCOMNAME", "IF_DEBUG_GETSERVERTRIGGERS",
)
EVIDENCE = "code/harness/target/loop/iteration25-validation/cs2"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    stats = {}
    for line in (output / "components-query.stats.tsv").read_text().splitlines():
        opcode, ok, failed = line.split("\t")
        stats[opcode] = {"ok": int(ok), "error": int(failed)}
    if set(stats) != set(OPCODES):
        raise SystemExit(f"components-query must cover exactly {sorted(OPCODES)}")
    for opcode, counts in stats.items():
        if counts["ok"] < 16:
            raise SystemExit(f"Fewer than 16 successful oracle cases for {opcode}")
    records = {}
    errors = {}
    for line in (output / "components-query.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        records[columns[0]] = records.get(columns[0], 0) + 1
        if columns[0] == "R" and columns[1] != "ok":
            errors[columns[1]] = errors.get(columns[1], 0) + 1
    if records["T"] != records["R"] or records["Z"] != 1:
        raise SystemExit("Query fixture records are inconsistent")
    return {
        "query_cases": records["T"],
        "query_records": records,
        "query_per_opcode": stats,
        "query_error_classes": errors,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or root / EVIDENCE / "native-components-query").resolve()
    allowed = (crate / "target", root / EVIDENCE)
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or " + EVIDENCE)
    kotlin = "code/client/src/main/kotlin/com/jagex/game/runetek5/"
    ops = kotlin + "config/rsinterface/cs2/ops/"
    drivers = (
        "NativeFixtures.java",
        "ComponentExtraFixtures.java",
        "ComponentLayerFixtures.java",
        "ComponentQueryFixtures.java",
    )
    source_paths = [
        kotlin + "config/rsinterface/cs2/ClientScriptOpCode.kt",
        kotlin + "config/rsinterface/cs2/CS2Executor.kt",
        ops + "ComponentQueryOps.kt",
        ops + "InterfaceQueryOps.kt",
        ops + "DetailCapabilityOps.kt",
        ops + "WorldMapUserDetailOps.kt",
        kotlin + "config/rsinterface/SubInterface.kt",
        kotlin + "config/rsinterface/Component.kt",
        kotlin + "config/rsinterface/Interface.kt",
        kotlin + "cache/node/IterableNodeMap.kt",
        kotlin + "client/Client.kt",
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
        subprocess.run([
            "java", "-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "ComponentQueryFixtures", str(output),
        ], cwd=work, check=True, timeout=600)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during query fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Query oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected query oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "CC_GETPARENTLAYER, IF_GETPARENTLAYER, IF_GET_GAMESCREEN and the IF_DEBUG_ opcodes GETOPENIFCOUNT, GETOPENIFID, GETNAME, GETCOMCOUNT, GETCOMNAME and GETSERVERTRIGGERS through the actual ComponentQueryOps, InterfaceQueryOps, DetailCapabilityOps and WorldMapUserDetailOps dispatch with real Component, Interface and SubInterface objects, a populated Client.OPEN_INTERFACES IterableNodeMap, Client.BASE_WINDOW_ID and Client.GAME_SCREEN_INTERFACE, all run headless from an isolated COPY of the pre-existing pinned shaded client JAR. No operation is reimplemented as an oracle.",
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
        "random_seeds": ["0x727c5227 (java.util.Random)"],
        "tsv_encoding": "components-query: W, I, C, N as in components-layer; B base window id; Q interface component name (null or hex:UTF-8) serverTriggers; G game screen interface component; O hash sub-interface-id overlay in insertion order; K initial hook secondary interface index; H hook change before the next T; T world opcode operand ints; R result int-stack string-stack - hook1 hook2; Z total.",
        **statistics,
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "components-query-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native query fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native query manifest differs")
        print(f"Verified {len(FIXTURES)} native query fixture files")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native query fixture files and provenance manifest")
    print(json.dumps({key: statistics[key] for key in ("query_cases", "query_error_classes")}, sort_keys=True))


if __name__ == "__main__":
    main()
