import argparse
import hashlib
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIXTURES = HERE.parent / "tests" / "fixtures"
PINNED = "6b38870f916966c8ab8720f619c9506d5103e23c8cb3918fb15bdd8b7890f0b9"
NAMES = ["matrix", "height", "ray", "tile", "polygon"]


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def private_copy(source, destination):
    if digest(source) != PINNED:
        raise ValueError(f"unexpected SHA-256 for {source}")
    if not destination.exists():
        shutil.copyfile(source, destination)
    if digest(destination) != PINNED:
        raise ValueError(f"unexpected private copy SHA-256 for {destination}")
    destination.chmod(0o444)


def generate(jar_source, work):
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    private_copy(jar_source, jar)
    source = HERE / "PickingOracle.java"
    subprocess.run(["javac", "-nowarn", "-cp", str(jar), "-d", str(work), str(source)], check=True, capture_output=True, text=True)
    command = ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "PickingOracle", str(jar), str(work), PINNED]
    subprocess.run(command, check=True, capture_output=True, text=True)
    return {name: (work / f"picking-{name}.tsv").read_bytes() for name in NAMES}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--work", type=Path)
    parser.add_argument("--fixtures", type=Path, default=FIXTURES)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    work = args.work or Path(tempfile.mkdtemp(prefix="picking-oracle-"))
    outputs = generate(args.jar, work)
    if args.check:
        bad = [name for name, data in outputs.items() if (args.fixtures / f"picking-{name}.tsv").read_bytes() != data]
        print(json.dumps({"checked": len(outputs), "mismatched": bad, "jarSha256": PINNED}))
        raise SystemExit(1 if bad else 0)
    args.fixtures.mkdir(parents=True, exist_ok=True)
    for name, data in outputs.items():
        (args.fixtures / f"picking-{name}.tsv").write_bytes(data)
    print(json.dumps({"written": len(outputs), "jarSha256": PINNED}))


if __name__ == "__main__":
    main()
