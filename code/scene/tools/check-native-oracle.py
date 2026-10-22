import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--suite", choices=("terrain", "surface"), default="terrain")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    suite = "native" if args.suite == "terrain" else "surface"
    work = root / "target" / f"{suite}-oracle"
    work.mkdir(parents=True, exist_ok=True)
    original = args.jar.resolve(strict=True)
    copied = work / "client.jar"
    checksum = sha256(original)
    shutil.copyfile(original, copied)
    if sha256(copied) != checksum:
        raise RuntimeError("oracle jar copy differs from the original")
    output = work / "native.tsv"
    output.unlink(missing_ok=True)
    command = [
        "jshell",
        "--execution", "local",
        "--feedback", "silent",
        "--class-path", str(copied),
        "-J--add-modules=jdk.unsupported",
        "-J-Djava.awt.headless=true",
        f"-J-Djava.util.prefs.userRoot={work / 'prefs'}",
        f"-J-Doracle.jar={copied}",
        f"-J-Doracle.output={output}",
        str(root / "tools" / f"{suite}-oracle.jsh"),
    ]
    result = subprocess.run(
        command, cwd=work, stdin=subprocess.DEVNULL,
        capture_output=True, text=True, timeout=90, check=False,
    )
    if result.returncode != 0 or "NATIVE_ORACLE_OK" not in result.stdout or not output.exists():
        raise RuntimeError((result.stdout + result.stderr)[-4000:])
    if sha256(original) != checksum or sha256(copied) != checksum:
        raise RuntimeError("an oracle jar changed during evaluation")
    fixture = root / "tests" / "fixtures" / f"{suite}.tsv"
    actual = output.read_bytes()
    if args.update:
        fixture.parent.mkdir(parents=True, exist_ok=True)
        fixture.write_bytes(actual)
    elif fixture.read_bytes() != actual:
        raise RuntimeError("native oracle differs from the checked-in fixture")
    count = len(actual.splitlines())
    print(f"Native oracle matched {count} rows; unchanged jar SHA-256 {checksum}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
