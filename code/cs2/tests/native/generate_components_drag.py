import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
DEFAULT_JAR = "code/harness/target/loop/mesh-oracle/oracle-client.jar"
FIXTURES = ("components-drag.native.tsv", "components-drag.stats.tsv")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--jar", default=os.environ.get("OPENRS_ORACLE_JAR"))
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = Path(args.jar).resolve() if args.jar else root / DEFAULT_JAR
    work = (crate / "target/native-drag").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Oracle jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    for path in (classes, output):
        path.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    drivers = ("NativeFixtures.java", "ComponentExtraFixtures.java", "ComponentLayerFixtures.java", "DragFixtures.java")
    sources = [crate / "tests/native" / name for name in drivers]
    try:
        subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-Xmx1g", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "DragFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during drag fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected drag oracle output files")
    lines = (output / "components-drag.native.tsv").read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("T\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native drag fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual DialogOps.dispatch (ccDragPickup, ifDragPickup) through InterfaceRenderer.startDragging, Client.getDragTarget, Component.getDepthChild and Interface.getSub from an isolated copy of the pinned client JAR, with real Component, Interface, IFEvents, SubInterface and IterableNodeMap objects; the Client drag statics are read back after every step",
        "jar": str(jar.relative_to(root)) if jar.is_relative_to(root) else "external private copy",
        "jar_sha256": JAR_SHA256,
        "driver_sha256": {name: digest(crate / "tests/native" / name) for name in drivers},
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "opcodes": sorted({line.split("\t")[2] for line in lines if line.startswith("T\t")}),
        "random_seed": "0x6a6a6a5 (java.util.Random)",
        "not_injected": "Client.frameComponent is a fixed dummy Component; MiniMenu.isOpen is set directly",
        "reproduction": "python3 code/cs2/tests/native/generate_components_drag.py --check",
    }
    (destination / "components-drag-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
