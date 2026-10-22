import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("clock.native.tsv", "hsv.native.bin")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-clock").resolve()
    original_before = digest(jar)
    if original_before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    work.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    classes = work / "classes"
    output = work / "fixtures"
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    try:
        subprocess.run([
            "javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes),
            str(crate / "tests/native/NativeFixtures.java"),
            str(crate / "tests/native/ClockFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "ClockFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != original_before:
            raise SystemExit("Native jar changed during config fixture generation")
    manifest = {
        "oracle": "Actual MathStringOps.dispatch and CameraLoginPrefOps.dispatch for RANDOM, RANDOMINC, RANDOM_SOUND_PITCH, HSVTORGB, DATE_MINUTES, DATE_RUNEDAY, DATE_YEAR, DATE_RUNEDAY_TODATE, FROMDATE and FORMAT_DATETIME_FROM_MINUTES, with a seeded Math.random, an injected Utils.time(), fixed-offset local zones and the real seeded HSV table, from an isolated COPY of the pinned shaded client JAR.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/ClockFixtures.java"),
        "reproduction": "python3 code/cs2/tests/native/generate_clock.py --check",
    }
    destination = crate / "tests/fixtures"
    manifest_path = destination / "clock-oracle.json"
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native clock fixture differs: {name}")
        if json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native clock manifest differs")
        print("Verified native clock fixtures")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print("Installed native clock fixtures")


if __name__ == "__main__":
    main()
