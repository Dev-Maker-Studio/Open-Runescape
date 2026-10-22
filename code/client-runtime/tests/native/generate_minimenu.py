import argparse
import gzip
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
SEED = 727
CASES = 1000
FIXTURE_NAME = "minimenu-oracle.tsv.gz"
DIGEST_NAME = "minimenu-oracle.sha256"
MANIFEST_NAME = "minimenu-oracle.json"
FIXTURES = HERE.parent / "fixtures"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def private_copy(source, destination):
    if sha(source.read_bytes()) != PINNED:
        raise ValueError(f"unexpected SHA-256 for {source}")
    if not destination.exists():
        shutil.copyfile(source, destination)
    if sha(destination.read_bytes()) != PINNED:
        raise ValueError("unexpected private copy SHA-256")
    destination.chmod(0o444)


def run_oracle(jar, work, seed, cases, name):
    source = HERE / "MiniMenuOracle.java"
    subprocess.run(
        ["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(work), str(source)],
        check=True,
    )
    output = work / name
    result = subprocess.run(
        [
            "java",
            "--add-opens",
            "java.base/java.lang=ALL-UNNAMED",
            "-Djava.awt.headless=true",
            "-cp",
            f"{work}:{jar}",
            "MiniMenuOracle",
            str(output),
            str(seed),
            str(cases),
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    return output.read_bytes(), result.stdout.strip(), source


def gz(data):
    import io

    buffer = io.BytesIO()
    with gzip.GzipFile(fileobj=buffer, mode="wb", compresslevel=9, mtime=0) as handle:
        handle.write(data)
    return buffer.getvalue()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=SEED)
    parser.add_argument("--cases", type=int, default=CASES)
    parser.add_argument("--fixtures", type=Path, default=FIXTURES)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    jar = args.work / "oracle-client.jar"
    private_copy(args.jar, jar)
    tsv, stdout, source = run_oracle(jar, args.work, args.seed, args.cases, "minimenu-oracle.tsv")
    if sha(args.jar.read_bytes()) != PINNED or sha(jar.read_bytes()) != PINNED:
        raise ValueError("jar changed during the run")
    packed = gz(tsv)
    digest = sha(tsv)
    fixture = args.fixtures / FIXTURE_NAME
    digest_file = args.fixtures / DIGEST_NAME
    if args.check:
        failures = []
        if not fixture.exists() or fixture.read_bytes() != packed:
            failures.append(FIXTURE_NAME)
        if not digest_file.exists() or digest_file.read_text().strip() != digest:
            failures.append(DIGEST_NAME)
        if failures:
            print("stale: " + ", ".join(failures))
            sys.exit(1)
        print(f"ok {digest} {stdout}")
        return
    args.fixtures.mkdir(parents=True, exist_ok=True)
    fixture.write_bytes(packed)
    digest_file.write_text(digest + "\n")
    manifest = {
        "oracle": "the real Kotlin MiniMenu, ColoredMiniMenuEntry and MenuInteraction.processMenuInput driven through reflection with a uniform 6 pixel font, replayed cumulatively in Rust",
        "jar": "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar",
        "jar_sha256": PINNED,
        "driver_sha256": sha(source.read_bytes()),
        "generator_sha256": sha(Path(__file__).read_bytes()),
        "fixture_sha256": digest,
        "cases": args.cases,
        "random_seed": args.seed,
        "oracle_output": stdout,
        "reproduction": f"python3 code/client-runtime/tests/native/generate_minimenu.py --jar <jar> --work <scratch dir> --seed {args.seed} --cases {args.cases} [--check]",
    }
    (args.fixtures / MANIFEST_NAME).write_text(json.dumps(manifest, indent=2) + "\n")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
