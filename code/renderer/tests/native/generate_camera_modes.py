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
FIXTURE = "camera-modes.native.tsv"
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
    work = (crate / "target/native-camera-modes").resolve()
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
    source = crate / "tests/native/CameraModesOracle.java"
    box = sandbox(work)
    try:
        subprocess.run(box + ["javac", "-cp", str(copied), "-d", str(classes), str(source)], cwd=work, check=True)
        subprocess.run(box + [
            "java", *OPENS, "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "CameraModesOracle", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during camera modes fixture generation")
    destination = crate / "tests/fixtures"
    generated = output / FIXTURE
    lines = generated.read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        if generated.read_bytes() != (destination / FIXTURE).read_bytes():
            raise SystemExit(f"Native camera modes fixture differs: {FIXTURE}")
        return
    shutil.copyfile(generated, destination / FIXTURE)
    manifest = {
        "oracle": "Real Camera.moveCamera, setAngle, lookat, smoothReset, startSmoothTransition, calculateCameraPosition, calculateCutsceneCameraPosition, updateFollowPlayerCamera, updateOrbitTargetCamera, updateSmoothTransitionCamera, processCamera and setCameraPosition, MapRegion.tileheightForAbsolutePosition, with Client.cycles, lastCameraSmoothCycle, the spline paths and indices, the shake arrays and the PingRequester/StaticMethods/Client target statics, from an isolated copy of the shaded client JAR, run in a bwrap sandbox without network. Injected or transcribed by the driver: the MapRegion, Scene and GroundSM grids from hash functions, base coordinates, the player position, spline control points from hash functions, the per-cycle phase increment and cycle counter, the Client.drawGameFrame mode dispatch (types 2, 4 and 1), the five-slot shake loop with its Math.random draws taken from a seeded java.util.Random, the pre-shake restore and the scene clamp, and the SET_CAMERA_TARGET and CAM_FOLLOWCOORD tile clamp and target writes.",
        "jar": jar.name,
        "jar_is_pinned": JAR_SHA256 == PINNED_SHA256,
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/CameraModesOracle.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {FIXTURE: digest(destination / FIXTURE)},
        "cases": cases,
        "kinds": sorted({line.split("\t")[1] for line in lines if line.startswith("C\t")}),
        "random_seed": "0x5ce7a9d1 (java.util.Random)",
        "reproduction": "python3 code/renderer/tests/native/generate_camera_modes.py --check",
    }
    (destination / "camera-modes-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
