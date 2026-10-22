import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("world-map.native.tsv", "world-map.stats.tsv")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def encode_string(text):
    return text.encode("ascii") + b"\x00"


def encode_element(definition):
    sections = definition.get("sections") or []
    data = bytearray()
    data += encode_string(definition["map"])
    data += encode_string(definition["name"])
    data += struct.pack(">i", definition["position"])
    data += struct.pack(">i", definition["anInt9542"])
    data.append(1 if definition["static"] else 0)
    data.append(definition["rawZoom"] & 0xFF)
    data.append(definition["mapSizeId"] & 0xFF)
    data.append(len(sections))
    for section in sections:
        data.append(section["level"])
        for key in ("minX", "minY", "maxX", "maxY", "startX", "startY", "endX", "endY"):
            data += struct.pack(">H", section[key])
    return bytes(data)


def real_elements(root):
    folder = root / "assets/cache/worldmapdata/details"
    files = sorted(folder.glob("*.json"), key=lambda path: int(path.stem))
    lines = []
    for path in files:
        definition = json.loads(path.read_text())
        lines.append(f"{int(path.stem)}\t{encode_element(definition).hex()}")
    return lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-world-map").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    inputs = work / "inputs"
    for path in (classes, output, inputs):
        path.mkdir(parents=True, exist_ok=True)
    (inputs / "real-elements.tsv").write_text("\n".join(real_elements(root)) + "\n")
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    sources = [crate / "tests/native" / name for name in ("WorldMapFixtures.java",)]
    try:
        subprocess.run(["javac", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "WorldMapFixtures", str(output), str(inputs),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during world map fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected world map oracle output files")
    lines = (output / "world-map.native.tsv").read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    elements = sum(1 for line in lines if line.startswith("E\t"))
    trailer = lines[-1].split("\t")
    if int(trailer[1]) != cases or int(trailer[2]) != elements:
        raise SystemExit("Trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native world map fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual WorldMapElement.list (through an allocateInstance Js5ResourceProvider and Js5MasterIndex holding the element bytes), WorldMap.WORLDMAP_DEFS iteration, and QuickChatDisplayOps.dispatch for ten WORLDMAP_* opcodes from an isolated copy of the pinned shaded client JAR. WorldMap.staticElement, mapSquareAbs, WorldMapDisplay.mapCenter, ComponentMinimap.mapAssetLoadingPercentage and WorldMap.worldMapCategories are injected. Real elements are encoded from assets/cache/worldmapdata/details/*.json with the server layout; the packed cache is not present so no byte read from a real packed cache is covered.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/WorldMapFixtures.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "elements": elements,
        "opcodes": sorted({line.split("\t")[9] for line in lines if line.startswith("C\t")}),
        "random_seed": "0x727c5228 (java.util.Random)",
        "reproduction": "python3 code/cs2/tests/native/generate_world_map.py --check",
    }
    (destination / "world-map-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
