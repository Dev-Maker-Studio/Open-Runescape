import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def checked(command, work):
    result = subprocess.run(
        command, cwd=work, stdin=subprocess.DEVNULL,
        capture_output=True, text=True, timeout=120, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError((result.stdout + result.stderr)[-6000:])
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/openrs-iteration13-palette/oracle"))
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=True)
    original = args.jar.resolve(strict=True)
    copied = work / "client.jar"
    checksum = sha256(original)
    shutil.copyfile(original, copied)
    if sha256(copied) != checksum:
        raise RuntimeError("oracle jar copy differs from the original")
    classes = work / "classes"
    classes.mkdir(exist_ok=True)
    checked([
        "javac", "--release", "17", "--add-modules", "jdk.unsupported",
        "-cp", str(copied), "-d", str(classes),
        str(root / "tools" / "PaletteOracle.java"),
    ], work)
    output = work / "palette.tsv"
    output.unlink(missing_ok=True)
    stdout = checked([
        "java", "--add-modules=jdk.unsupported", "-Djava.awt.headless=true",
        f"-Djava.util.prefs.userRoot={work / 'prefs'}",
        "-cp", os.pathsep.join((str(classes), str(copied))),
        "PaletteOracle", str(copied), str(output),
    ], work)
    if "NATIVE_PALETTE_ORACLE_OK" not in stdout or not output.exists():
        raise RuntimeError("native palette oracle did not finish")
    if sha256(original) != checksum or sha256(copied) != checksum:
        raise RuntimeError("an oracle jar changed during evaluation")
    fixture = root / "tests" / "fixtures" / "palette.tsv"
    actual = output.read_bytes()
    if args.update:
        fixture.write_bytes(actual)
    elif fixture.read_bytes() != actual:
        raise RuntimeError("native palette oracle differs from the checked-in fixture")
    print(f"Native palette oracle matched {len(actual.splitlines())} rows; unchanged jar SHA-256 {checksum}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
