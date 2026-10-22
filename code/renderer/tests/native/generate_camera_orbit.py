import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


PINNED_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
SHARED_SHA256 = "6b38870f916966c8ab8720f619c9506d5103e23c8cb3918fb15bdd8b7890f0b9"
ACCEPTED = (PINNED_SHA256, SHARED_SHA256)
FIXTURE = "camera-orbit.native.tsv"
OPENS = ["--add-opens", "java.base/java.lang=ALL-UNNAMED"]


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def sandbox(work):
    return [
        "bwrap", "--unshare-net", "--die-with-parent", "--new-session",
        "--ro-bind", "/", "/", "--tmpfs", "/tmp", "--bind", str(work), str(work),
        "--proc", "/proc", "--dev", "/dev", "--chdir", str(work), "--",
    ]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/renderer"
    jar = (args.jar or root / "code/client/build/libs/darkan-client-shaded-2.0.0-all.jar").resolve()
    work = (crate / "target/native-camera-orbit").resolve()
    before = digest(jar)
    if before not in ACCEPTED:
        raise SystemExit("Native jar does not match a SHA-256 accepted for this oracle")
    JAR_SHA256 = before
    classes = work / "classes"
    output = work / "fixtures"
    for path in (classes, output):
        path.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    source = crate / "tests/native/CameraOrbitOracle.java"
    box = sandbox(work)
    try:
        subprocess.run(box + ["javac", "-cp", str(copied), "-d", str(classes), str(source)], cwd=work, check=True)
        subprocess.run(box + [
            "java", *OPENS, "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "CameraOrbitOracle", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during camera orbit fixture generation")
    destination = crate / "tests/fixtures"
    generated = output / FIXTURE
    lines = generated.read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        if generated.read_bytes() != (destination / FIXTURE).read_bytes():
            raise SystemExit(f"Native camera orbit fixture differs: {FIXTURE}")
        return
    shutil.copyfile(generated, destination / FIXTURE)
    manifest = {
        "oracle": "Real Camera.updateFollowPlayerCamera, Camera.processCamera, Camera.setCameraPosition, MapRegion.tileheightForAbsolutePosition, Ground.getAverageHeight, Client.applyCameraYawRight and StaticMethods.applyCameraYawLeft/PitchUp/PitchDown from an isolated copy of the pinned shaded client JAR, run in a bwrap sandbox without network. Injected by the driver: the MapRegion, Scene, four GroundSM height grids, SettingsBits and lighting from hash functions, the player position, the per-cycle velocity halving, the jitter counter and step flips with scripted random bits, the Ctrl wheel zoom rule and the drawGameArea yaw, roll, base delta and scene clamp.",
        "jar": jar.name,
        "jar_is_pinned": JAR_SHA256 == PINNED_SHA256,
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/CameraOrbitOracle.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {FIXTURE: digest(destination / FIXTURE)},
        "cases": cases,
        "kinds": sorted({line.split("\t")[1] for line in lines if line.startswith("C\t")}),
        "random_seed": "0x0c4a3e72 (java.util.Random)",
        "reproduction": "python3 code/renderer/tests/native/generate_camera_orbit.py --check",
    }
    (destination / "camera-orbit-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
