import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
DEFAULT_JAR = "code/harness/target/loop/mesh-oracle/oracle-client.jar"
FIXTURES = ("sub-interface.native.tsv", "sub-interface.stats.tsv")


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
    work = (crate / "target/native-sub-interface").resolve()
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
    sources = [crate / "tests/native" / name for name in ("SubInterfaceFixtures.java",)]
    try:
        subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "SubInterfaceFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during sub interface fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected sub interface oracle output files")
    lines = (output / "sub-interface.native.tsv").read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("T\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native sub interface fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual DialogOps.dispatch (ifOpenSubClient, ifCloseSubClient) through Interface.openSub and closeChildInterface from an isolated copy of the pinned client JAR; Client.OPEN_INTERFACES and ICOMPONENT_SETTINGS_SLOTS are real IterableNodeMaps seeded directly and read back bucket by bucket",
        "jar": str(jar.relative_to(root)) if jar.is_relative_to(root) else "external private copy",
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/SubInterfaceFixtures.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "opcodes": sorted({line.split("\t")[1] for line in lines if line.startswith("T\t")}),
        "random_seed": "0x5ab1c11 (java.util.Random)",
        "not_injected": "Interface.INTERFACES is empty so no component is ever found: the redraw, updateEntityLayout, resetComponentSequences, unloadInterface and MiniMenu prune side effects are not compared (the Rust model records them as InterfaceRequest values); activeDialogueContinue is null",
        "reproduction": "python3 code/cs2/tests/native/generate_sub_interface.py --check",
    }
    (destination / "sub-interface-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
