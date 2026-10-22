import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_world_map import JAR_SHA256, digest, real_elements

FIXTURES = ("world-map-effects.native.tsv",)
SOURCES = ("WorldMapFixtures.java", "WorldMapEffectFixtures.java", "WorldMapEffectHooks.java", "WorldMapEffectBoot.java")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-world-map-effects").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    inputs = work / "inputs"
    for path in (classes, output, inputs):
        path.mkdir(parents=True, exist_ok=True)
    for stale in output.iterdir():
        stale.unlink()
    (inputs / "real-elements.tsv").write_text("\n".join(real_elements(root)) + "\n")
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    sources = [crate / "tests/native" / name for name in SOURCES]
    try:
        subprocess.run(["javac", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", *([f"-Dfixture.debug=1"] if os.environ.get("FIXTURE_DEBUG") else []), "-XX:-OmitStackTraceInFastThrow",
            "-cp", str(classes),
            "WorldMapEffectBoot", str(output), str(inputs), str(classes), str(copied),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during world map effect fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected world map effect oracle output files")
    lines = (output / FIXTURES[0]).read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native world map effect fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual QuickChatDisplayOps.dispatch for WORLDMAP_SETZOOM, SETMAP, SETMAP_COORD, SETMAP_COORD_OVERRIDE, JUMPTODISPLAYCOORD, JUMPTOSOURCECOORD, FLASHELEMENT, FLASHELEMENTCATEGORY, CLOSEMAP, LISTELEMENT_START and LISTELEMENT_NEXT, run through the real WorldMapDisplay.reset, WorldMapInteraction.requestZoomChange, StaticMethods.setWorldMapScrollTarget and selectWorldMapElement, WorldMapDisplay.highlightCategory, getFirstActiveCategory and WorldMap.getNextActiveCategory with a real MECType and MECTypeList cache. The jar copy is loaded by a child-first class loader that rewrites only Client$Companion.loadAssets(Z)V and setLoadingWorldMap()V (java.lang.classfile) to record calls in WorldMapEffectHooks, so the asset reload and the loading screen are an effect log and not executed. The real Client.loadAssets work (cache clearing, AbstractToolkit.activeRenderMode) is not modelled.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "driver_sha256": {name: digest(crate / "tests/native" / name) for name in SOURCES},
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "random_seed": "0x727c5228 ^ 0x5eed (java.util.Random)",
        "reproduction": "python3 code/cs2/tests/native/generate_world_map_effects.py --check",
    }
    (destination / "world-map-effects-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
