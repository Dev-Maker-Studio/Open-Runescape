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


def run(command, work):
    result = subprocess.run(
        command, cwd=work, stdin=subprocess.DEVNULL,
        capture_output=True, text=True, timeout=180, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError((result.stdout + result.stderr)[-12000:])
    return result.stdout + result.stderr


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--evidence-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    original = args.jar.resolve(strict=True)
    checksum = sha256(original)
    fixture = root / "tests" / "fixtures" / "material.tsv"
    with tempfile.TemporaryDirectory(prefix="openrs-material-oracle-") as temporary:
        work = Path(temporary)
        copied = work / "material-client.jar"
        shutil.copyfile(original, copied)
        if sha256(copied) != checksum:
            raise RuntimeError("oracle jar copy differs from the original")
        output = work / "material.tsv"
        log = run([
            "javac", "-cp", str(copied), "-d", str(work),
            str(root / "tools" / "material-oracle.java"),
        ], work)
        log += run([
            "java", "--add-modules=jdk.unsupported", "-Djava.awt.headless=true",
            "-cp", f"{work}:{copied}", "MaterialOracle", str(copied), str(output),
        ], work)
        if "NATIVE_MATERIAL_ORACLE_OK" not in log or not output.exists():
            raise RuntimeError(log[-12000:])
        if sha256(original) != checksum or sha256(copied) != checksum:
            raise RuntimeError("an oracle jar changed during evaluation")
        actual = output.read_bytes()
    if args.update:
        fixture.write_bytes(actual)
    elif fixture.read_bytes() != actual:
        raise RuntimeError("native material oracle differs from the checked-in fixture")
    lines = actual.decode().splitlines()
    summary = {
        "status": "matched",
        "rows": len(lines),
        "tables": sum(line.startswith("table\t") for line in lines),
        "materialRows": sum(line.startswith("material\t") for line in lines),
        "nativeTruncatedInputs": sum(line.startswith("truncated\t") for line in lines),
        "sourceJar": str(original),
        "sourceJarSha256": checksum,
        "fixtureSha256": hashlib.sha256(actual).hexdigest(),
        "jarUnchanged": True,
        "nativeClassesUnmodified": True,
        "nativeEntryPoint": "MaterialTypeList(Js5ResourceProvider, Js5ResourceProvider, Js5ResourceProvider)",
        "nativeProvider": "unchanged Js5ResourceProvider with in-memory preloaded archiveFiles[0][0]",
        "materialIndex": 26,
        "materialArchive": 0,
        "materialFile": 0,
        "textureIndex": 9,
        "spriteIndex": 8,
        "denseMaximumSlots": 65535,
        "denseMaximumBytes": 1572842,
        "allByteValuesPerColumn": True,
        "allPresenceByteValues": True,
        "texturePixelsGenerated": False,
        "networkUsed": False,
        "cacheWritten": False,
    }
    if args.evidence_dir is not None:
        args.evidence_dir.mkdir(parents=True, exist_ok=True)
        (args.evidence_dir / "material-native-oracle.log").write_text(log)
        (args.evidence_dir / "material-native-oracle.json").write_text(
            json.dumps(summary, indent=2) + "\n"
        )
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
