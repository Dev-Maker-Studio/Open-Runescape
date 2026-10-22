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

SOUND_INDEX = 4
MUTANTS = 6000
DEFAULT_JAR = CRATE.parent / "renderer/target/orbit-live-jar/pinned.jar"
FIXTURES = CRATE / "tests/fixtures"
NAMES = ("synth-input.bin", "synth-cases.tsv", "synth-oracle.tsv")


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound


def collect(cache):
    index = base.Index(cache, SOUND_INDEX)
    blobs = {}
    for archive in sorted(index.table):
        for file in index.table[archive]:
            data = index.file(archive, file)
            if data is not None:
                blobs[archive] = data
        index.loaded.pop(archive, None)
    return blobs


def cases(blobs):
    lines = []
    ids = sorted(blobs)
    for archive in ids:
        lines.append("C\t%d\t%d\t0\t0\t0" % (archive, archive))
    rng = Lcg(0x5EED5107)
    for number in range(MUTANTS):
        source = ids[rng.next(len(ids))]
        size = len(blobs[source])
        kind = 1 + rng.next(2)
        if kind == 1:
            pos = rng.next(size + 1)
            value = 0
        else:
            pos = rng.next(size)
            value = rng.next(256)
        lines.append("C\t%d\t%d\t%d\t%d\t%d" % (100000 + number, source, kind, pos, value))
    return lines


def bundle(blobs):
    out = bytearray(struct.pack(">i", len(blobs)))
    for archive in sorted(blobs):
        out += struct.pack(">ii", archive, len(blobs[archive])) + blobs[archive]
    return bytes(out)


def run(args, work, jar, blobs, lines):
    (work / "synth.bundle").write_bytes(bundle(blobs))
    (work / "synth-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "SynthOracle.java"
    classes = work / "synth-classes"
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
    output = work / "synth-oracle.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "SynthOracle", str(jar), str(work / "synth.bundle"),
                   str(work / "synth-cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    return output.read_bytes(), result.stdout.strip(), source


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
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work")
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
        tsv, oracle_output, source = run(args, work, jar, blobs, lines)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    rows = tsv.decode("ascii").splitlines()
    outputs = {
        "synth-input.bin": bundle(blobs),
        "synth-cases.tsv": ("\n".join(lines) + "\n").encode("ascii"),
        "synth-oracle.tsv": tsv,
    }
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "soundEffectsInBundle": len(blobs),
        "cases": len(rows),
        "injectedMutants": MUTANTS,
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
                raise SystemExit("synth fixture %s differs; rerun without --check to regenerate" % name)
        evidence["check"] = "match"
    else:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        evidence["fixtureBytes"] = {name: write_fixture(name, data) for name, data in outputs.items()}
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
