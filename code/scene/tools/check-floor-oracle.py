import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--evidence-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    original = args.jar.resolve(strict=True)
    checksum = sha256(original)
    fixture = root / "tests" / "fixtures" / "floor.tsv"
    with tempfile.TemporaryDirectory(prefix="openrs-floor-oracle-") as temporary:
        work = Path(temporary)
        copied = work / "client.jar"
        shutil.copyfile(original, copied)
        if sha256(copied) != checksum:
            raise RuntimeError("oracle jar copy differs from the original")
        output = work / "floor.tsv"
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
            str(root / "tools" / "floor-oracle.jsh"),
        ]
        result = subprocess.run(
            command, cwd=work, stdin=subprocess.DEVNULL,
            capture_output=True, text=True, timeout=180, check=False,
        )
        if args.evidence_dir is not None:
            args.evidence_dir.mkdir(parents=True, exist_ok=True)
            (args.evidence_dir / "floor-native-oracle.log").write_text(
                result.stdout + result.stderr
            )
        if result.returncode != 0 or "NATIVE_ORACLE_OK" not in result.stdout or not output.exists():
            raise RuntimeError((result.stdout + result.stderr)[-8000:])
        if sha256(original) != checksum or sha256(copied) != checksum:
            raise RuntimeError("an oracle jar changed during evaluation")
        actual = output.read_bytes()
    if args.update:
        fixture.write_bytes(actual)
    elif fixture.read_bytes() != actual:
        raise RuntimeError("native floor oracle differs from the checked-in fixture")
    summary = {
        "status": "matched",
        "rows": len(actual.splitlines()),
        "sourceJar": str(original),
        "sourceJarSha256": checksum,
        "fixtureSha256": hashlib.sha256(actual).hexdigest(),
        "jarUnchanged": True,
        "nativeClassesUnmodified": True,
        "rgbExhaustiveInputs": 16777216,
        "underlayExhaustiveInputs": 16777216,
    }
    if args.evidence_dir is not None:
        (args.evidence_dir / "floor-native-oracle.json").write_text(
            json.dumps(summary, indent=2) + "\n"
        )
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
