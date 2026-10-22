import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("components-layer.native.tsv", "components-layer.stats.tsv")
EVIDENCE = "code/harness/target/loop/iteration24-validation/cs2"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def inspect(output):
    stats = {}
    for line in (output / "components-layer.stats.tsv").read_text().splitlines():
        name, ok, failed = line.split("\t")
        stats[name] = {"ok": int(ok), "error": int(failed)}
    records = {"W": 0, "I": 0, "C": 0, "N": 0, "K": 0, "P": 0, "S": 0, "H": 0, "T": 0, "R": 0, "D": 0, "Z": 0}
    errors = {}
    for line in (output / "components-layer.native.tsv").read_text().splitlines():
        columns = line.split("\t")
        if columns[0] not in records:
            raise SystemExit("Unknown component fixture record")
        records[columns[0]] += 1
        if columns[0] == "R" and columns[1] != "ok":
            errors[columns[1]] = errors.get(columns[1], 0) + 1
    if len(stats) < 23:
        raise SystemExit("Component fixtures must cover 23 opcodes")
    for name, counts in stats.items():
        if counts["ok"] < 16:
            raise SystemExit(f"Fewer than 16 successful oracle cases for {name}")
    if records["T"] != records["R"] or records["Z"] != 1:
        raise SystemExit("Component fixture records are inconsistent")
    return {
        "cases": records["T"],
        "delta_records": records["D"],
        "records": records,
        "per_opcode": stats,
        "error_classes": errors,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (args.work_dir or root / EVIDENCE / "native-components-layer").resolve()
    allowed = (crate / "target", root / EVIDENCE)
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit("--work-dir must be inside code/cs2/target or " + EVIDENCE)
    ops = "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/"
    source_paths = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Interface.kt",
        ops + "ComponentLifecycleOps.kt",
        ops + "ComponentQueryOps.kt",
        ops + "ComponentSetterOps.kt",
        ops + "InterfaceQueryOps.kt",
        ops + "InterfaceSetterOps.kt",
        ops + "CS2SharedOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/param/ParamType.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/param/ParamTypeList.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/IFEvents.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/Client.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/Component.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/Interface.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/periodic/PulseEventQueue.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/ComponentExtraFixtures.java",
        "code/cs2/tests/native/ComponentLayerFixtures.java",
        "code/cs2/tests/native/generate_components_hooks.py",
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
            raise SystemExit("Component work paths must not resolve outside allowed evidence directories")
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
            str(crate / "tests/native/ComponentExtraFixtures.java"),
            str(crate / "tests/native/ComponentLayerFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "ComponentLayerFixtures", str(output),
        ], cwd=work, check=True, timeout=600)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native jar changed during component fixture generation")
    if {name: digest(root / name) for name in source_paths} != source_hashes:
        raise SystemExit("Component oracle sources changed during generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected component oracle output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Layer, 2D angle, id and next-sub-id getters and drag deadtime, drag render behaviour, depth child (SETDRAGGABLE), recolor and retexture setters (CC_/IF_): actual ComponentQueryOps, InterfaceQueryOps, ComponentSetterOps and InterfaceSetterOps dispatch on real Component and Interface objects with a real CS2Executor, run headless from an isolated COPY of the pre-existing pinned shaded client JAR. Pulse events carry the recolor and retexture index. No component operation is reimplemented as an oracle.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "source_provenance": "Hashes identify the current relevant Kotlin sources and the shared Java helper, not a source rebuild; the pinned pre-existing JAR is the executable oracle.",
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/ComponentLayerFixtures.java"),
        "helper_driver_sha256": digest(crate / "tests/native/ComponentExtraFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "random_seed": "0x727c5224 (java.util.Random)",
        "tsv_encoding": "Tab separated records. W name capacity canvas-width canvas-height starts a world, I interface prevent-interaction count, C component columns, N null slot, K initial hook, T world opcode operand int-stack string-stack starts a step, R result (ok or error:exception class) final int stack, final string stack, queued pulse events (recol and retex carry the index after the kind), secondary hooks 1 and 2 (interface:id-hash:slot or dash), D key then the changed component describe line (dragDeadTime, dragType, depth child idHash:slot, recolor and retexture source/destination arrays are the last columns; dash when null or gone), Z total step count. Stacks are comma separated, dash is empty, strings are hex: prefixed or null.",
        **statistics,
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "components-layer-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native component fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native component manifest differs")
        print(f"Verified {len(FIXTURES)} native component fixture files, {statistics['cases']} cases, exact source/driver hashes and unchanged pinned original/copied JAR hashes")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native component fixture files, {statistics['cases']} cases and provenance manifest")
    print(json.dumps({key: statistics[key] for key in ("cases", "delta_records", "error_classes")}, sort_keys=True))


if __name__ == "__main__":
    main()
