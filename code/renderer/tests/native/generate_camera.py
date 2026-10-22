import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("camera.native.tsv", "camera.stats.tsv")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/renderer"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-camera").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    for path in (classes, output):
        path.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    source = crate / "tests/native/CameraFixtures.java"
    try:
        subprocess.run(["javac", "-cp", str(copied), "-d", str(classes), str(source)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "CameraFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during camera fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected camera oracle output files")
    lines = (output / "camera.native.tsv").read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native camera fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Real Matrix43.setViewPosition/rotate, Matrix44.fromMatrix/multiplyBy/setPerspective/getNearClipDistance/getDepthPerspectiveCD, MathUtils.getTheta, OpenGlToolkit.updateViewProjectionMatrix and recalculateFogAndProjection on an Unsafe-allocated ModernGLToolkit, and MapRegion.calculateViewRange with Loader.FOG_DISTANCE_MULTIPLIER, from an isolated copy of the pinned shaded client JAR. Injected by the driver: the batcher Y flip (elements 1,5,9,13), the fog enabled flag (enabled && density >= 0), the batcher colour channels, the environment density formula and the clear colour formula. NaN is recorded as one canonical quiet NaN.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/CameraFixtures.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "kinds": sorted({line.split("\t")[1] for line in lines if line.startswith("C\t")}),
        "random_seed": "0x727c5228 (java.util.Random)",
        "reproduction": "python3 code/renderer/tests/native/generate_camera.py --check",
    }
    (destination / "camera-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
