import argparse
import gzip
import importlib.util
import json
import os
import shutil
import struct
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CRATE = HERE.parents[1]
CONFIG = CRATE.parent / "client-runtime" / "tests" / "native" / "generate_config_oracle.py"
spec = importlib.util.spec_from_file_location("generate_config_oracle", CONFIG)
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

SAMPLE_INDEX = 14
CLEAN_SAMPLES = 80
SAMPLE_MUTANTS = 600
SETUP_MUTANTS = 200
MAX_CLEAN_BYTES = 100000
MAX_MUTANT_BASE_BYTES = 30000
HEADER_BYTES = 20
DEFAULT_JAR = CRATE.parent / "renderer/target/native-camera-orbit/native-client.jar"
FIXTURES = CRATE / "tests/fixtures"


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound


def collect(cache):
    index = base.Index(cache, SAMPLE_INDEX)
    setup = index.file(0, 0)
    index.loaded.pop(0, None)
    sizes = {}
    for archive in sorted(index.table):
        if archive == 0:
            continue
        data = index.file(archive, 0)
        if data is not None:
            sizes[archive] = len(data)
        index.loaded.pop(archive, None)
    eligible = sorted((i for i in sizes if sizes[i] <= MAX_CLEAN_BYTES), key=lambda i: (sizes[i], i))
    picked = [eligible[0], eligible[1]]
    step = len(eligible) / (CLEAN_SAMPLES - 2)
    for number in range(CLEAN_SAMPLES - 2):
        picked.append(eligible[min(int(number * step), len(eligible) - 1)])
    picked = sorted(set(picked))
    blobs = {0: setup}
    for archive in picked:
        blobs[archive] = index.file(archive, 0)
        index.loaded.pop(archive, None)
    return blobs


def cases(blobs):
    lines = ["C\t0\t0\t0\t0\t0"]
    ids = sorted(i for i in blobs if i != 0)
    for archive in ids:
        lines.append("C\t%d\t%d\t0\t0\t0" % (archive, archive))
    rng = Lcg(0x5EED5A3D)
    pool = [i for i in ids if len(blobs[i]) <= MAX_MUTANT_BASE_BYTES]
    for number in range(SAMPLE_MUTANTS):
        source = pool[rng.next(len(pool))]
        size = len(blobs[source])
        kind = 1 + rng.next(2)
        if kind == 1:
            pos = HEADER_BYTES + rng.next(size - HEADER_BYTES + 1)
            value = 0
        else:
            pos = HEADER_BYTES + rng.next(size - HEADER_BYTES)
            value = rng.next(256)
        lines.append("C\t%d\t%d\t%d\t%d\t%d" % (100000 + number, source, kind, pos, value))
    size = len(blobs[0])
    for number in range(SETUP_MUTANTS):
        kind = 1 + rng.next(2)
        if kind == 1:
            pos = rng.next(size + 1)
            value = 0
        else:
            pos = rng.next(size)
            value = rng.next(256)
        lines.append("C\t%d\t0\t%d\t%d\t%d" % (200000 + number, kind, pos, value))
    return lines


def bundle(blobs):
    out = bytearray(struct.pack(">i", len(blobs)))
    for archive in sorted(blobs):
        out += struct.pack(">ii", archive, len(blobs[archive])) + blobs[archive]
    return bytes(out)


def run_once(work, jar, blobs, lines, sandbox, classes):
    (work / "msample.bundle").write_bytes(bundle(blobs))
    (work / "msample-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    output = work / "msample-oracle.tsv"
    hang = work / "msample-oracle.tsv.hang"
    if hang.exists():
        hang.unlink()
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "MusicSampleOracle", str(jar), str(work / "msample.bundle"),
                   str(work / "msample-cases.txt"), str(output)],
        capture_output=True, text=True,
    )
    return result, output, hang


def run(work, jar, blobs, lines):
    source = HERE / "MusicSampleOracle.java"
    classes = work / "msample-classes"
    classes.mkdir(exist_ok=True)
    bwrap = shutil.which("bwrap")
    sandbox = []
    if bwrap:
        sandbox = [
            bwrap, "--unshare-net", "--die-with-parent", "--new-session", "--ro-bind", "/", "/",
            "--tmpfs", "/tmp", "--bind", str(work), str(work), "--proc", "/proc", "--dev", "/dev",
            "--chdir", str(work), "--",
        ]
    subprocess.run(sandbox + ["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(classes), str(source)], check=True)
    dropped = []
    while True:
        result, output, hang = run_once(work, jar, blobs, lines, sandbox, classes)
        if result.returncode == 0:
            return output.read_bytes(), result.stdout.strip(), source, lines, dropped
        if result.returncode == 3 and hang.exists():
            bad = hang.read_text().strip()
            dropped.append(bad)
            lines = [line for line in lines if line.split("\t")[1] != bad]
            continue
        raise SystemExit("oracle failed: %s %s" % (result.returncode, result.stderr[-2000:]))


def write_fixture(name, data):
    path = FIXTURES / (name + ".gz")
    with gzip.GzipFile(path, "wb", compresslevel=9, mtime=0) as handle:
        handle.write(data)
    (FIXTURES / (name.rsplit(".", 1)[0] + ".sha256")).write_text(base.sha(data) + "\n")
    return path.stat().st_size


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=DEFAULT_JAR)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".darkanrs/caches/darkan/LIVE")
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work-music")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if base.sha(args.jar.read_bytes()) != base.PINNED:
        raise SystemExit("jar does not match the pinned oracle sha256")
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    if jar.exists():
        jar.chmod(0o644)
        jar.unlink()
    shutil.copyfile(args.jar, jar)
    jar.chmod(0o444)
    if base.sha(jar.read_bytes()) != base.PINNED:
        raise SystemExit("private jar copy differs from the pinned oracle")
    cache = base.lw.Cache(args.cache)
    blobs = collect(cache)
    lines = cases(blobs)
    try:
        tsv, oracle_output, source, lines, dropped = run(work, jar, blobs, lines)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    rows = tsv.decode("ascii").splitlines()
    outputs = {
        "msample-input.bin": bundle(blobs),
        "msample-cases.tsv": ("\n".join(lines) + "\n").encode("ascii"),
        "msample-oracle.tsv": tsv,
    }
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "samplesInBundle": len(blobs) - 1,
        "cases": len(rows),
        "sampleMutants": SAMPLE_MUTANTS,
        "setupMutants": SETUP_MUTANTS,
        "droppedHangs": dropped,
        "errorRows": sum(1 for row in rows if row.endswith("\terror")),
        "tsvSha256": base.sha(tsv),
        "oracleOutput": oracle_output,
    }
    if args.check:
        for name, data in outputs.items():
            stored = gzip.decompress((FIXTURES / (name + ".gz")).read_bytes())
            digest = (FIXTURES / (name.rsplit(".", 1)[0] + ".sha256")).read_text().strip()
            if stored != data or digest != base.sha(data):
                print(json.dumps(evidence, indent=2))
                raise SystemExit("music sample fixture %s differs; rerun without --check to regenerate" % name)
        evidence["check"] = "match"
    else:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        evidence["fixtureBytes"] = {name: write_fixture(name, data) for name, data in outputs.items()}
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
