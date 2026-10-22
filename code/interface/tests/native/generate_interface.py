import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


FIXTURES = ("decode.native.tsv", "layout.native.tsv")
KOTLIN = "code/client/src/main/kotlin/com/jagex/game/runetek5/"
SOURCES = (
    KOTLIN + "config/rsinterface/Component.kt",
    KOTLIN + "config/rsinterface/ComponentType.kt",
    KOTLIN + "config/rsinterface/ComponentContentType.kt",
    KOTLIN + "graphics/ifcomponent/InterfaceRenderer.kt",
    KOTLIN + "graphics/ifcomponent/InterfaceComponentRenderer.kt",
    KOTLIN + "buffer/Packet.kt",
    KOTLIN + "utils/Utils.kt",
    "code/interface/tests/native/InterfaceDecodeFixtures.java",
)


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/interface"
    work = args.work_dir.resolve()
    if not work.is_relative_to(crate / "target"):
        raise SystemExit("--work-dir must be inside code/interface/target")
    shared = args.jar.resolve()
    shared_before = digest(shared)
    source_hashes = {name: digest(root / name) for name in SOURCES}
    work.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    classes = work / "classes"
    output = work / "fixtures"
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(shared, copied)
    copied_before = digest(copied)
    if copied_before != shared_before:
        raise SystemExit("Isolated native jar copy differs")
    try:
        subprocess.run([
            "javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes),
            str(crate / "tests/native/InterfaceDecodeFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Xmx1g", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "InterfaceDecodeFixtures", str(output),
        ], cwd=work, check=True, timeout=600)
    finally:
        if digest(shared) != shared_before or digest(copied) != copied_before:
            raise SystemExit("Native jar changed during interface fixture generation")
    counts = {}
    for name in FIXTURES:
        lines = (output / name).read_text().splitlines()
        counts[name] = {"records": len(lines), "final": lines[-1].split("\t")}
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Component.decode (real Component, Packet, ComponentType, ComponentContentType) over seeded Java-encoded component streams for every type and format version plus truncated and corrupted streams, and InterfaceRenderer.updateInterfaceDimensions (initSizes, componentPositionAdjustment, scroll clamp, container recursion) over seeded component trees, run headless from an isolated COPY of the shared client jar.",
        "injected": "Component.clientParams is pre-initialised to an empty IterableNodeMap before decode because the Kotlin client leaves it null and decode dereferences it with !! for any component that carries client params; the byte streams are produced by a Java encoder written for the fixture.",
        "java": java.stderr.splitlines()[0],
        "jar_sha256": shared_before,
        "jar_sha256_after": digest(shared),
        "source_sha256": source_hashes,
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(output / name) for name in FIXTURES},
        "random_seed": "0x727c1f4e (java.util.Random)",
        "counts": counts,
    }
    destination = crate / "tests/fixtures"
    destination.mkdir(exist_ok=True)
    manifest_path = destination / "interface-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native interface fixture differs: {name}")
        print("Verified interface fixtures")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print("Installed interface fixtures")
    print(json.dumps(counts, sort_keys=True))


if __name__ == "__main__":
    main()
