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

FRAME_INDEX = 0
BASE_INDEX = 1
SEQ_INDEX = 20
TRACES = 8000
STEPS = 14
DEFAULT_JAR = CRATE.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
FIXTURE = CRATE / "tests/fixtures/anim-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/anim-oracle.sha256"

DURATIONS = [1, 2, 3, 5, 7, 10, 15, 20, 30, 50, 80, 100, 101, 150, 200, 400, 1000, 5000, 100000]
SPEEDS = [0, 0, 0, 1, 5, 30]


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound


def frame_groups(data):
    if len(data) < 3 or data[0] != 1:
        return None
    count = (data[1] << 8) | data[2]
    need = 3 + count * 6
    if len(data) < need:
        return None
    start = 3 + count * 4
    groups = []
    for i in range(count):
        groups.append((data[start + i * 2] << 8) | data[start + i * 2 + 1])
    return groups


def collect(cache):
    frame_index = base.Index(cache, FRAME_INDEX)
    base_index = base.Index(cache, BASE_INDEX)
    seq_index = base.Index(cache, SEQ_INDEX)
    bases = {}
    for archive in sorted(base_index.table):
        data = base_index.file(archive, 0)
        if data is not None:
            bases[archive] = data
        base_index.loaded.pop(archive, None)
    sets = {}
    for group in sorted(frame_index.table):
        files = {}
        for file in frame_index.ids(group):
            data = frame_index.file(group, file)
            if data is not None:
                files[file] = data
        frame_index.loaded.pop(group, None)
        sets[group] = files
    seqs = {}
    for archive in sorted(seq_index.table):
        for file in seq_index.table[archive]:
            data = seq_index.file(archive, file)
            if data is not None:
                seqs[archive << 7 | file] = data
    return bases, sets, seqs


def cases(bases, sets, seqs):
    lines = []
    for archive in sorted(bases):
        lines.append("B\t%d" % archive)
    for group in sorted(sets):
        lines.append("F\t%d" % group)
    for seq in sorted(seqs):
        lines.append("S\t%d" % seq)
    pool = []
    for seq in sorted(seqs):
        groups = frame_groups(seqs[seq])
        if groups and all(group in sets for group in groups):
            pool.append(seq)
    everything = sorted(seqs)
    rng = Lcg(0x5EED0A11)

    def pick():
        if rng.next(12) == 0:
            return everything[rng.next(len(everything))]
        return pool[rng.next(len(pool))]

    def animate(seq):
        return "a,%d,%d,%d,%d" % (seq, SPEEDS[rng.next(len(SPEEDS))], rng.next(3), rng.next(2))

    for number in range(TRACES):
        steps = [animate(pick())]
        current = int(steps[0].split(",")[1])
        for _ in range(STEPS - 1):
            kind = rng.next(20)
            if kind < 11:
                steps.append("p,%d" % DURATIONS[rng.next(len(DURATIONS))])
            elif kind < 15:
                steps.append("m")
            elif kind < 17:
                steps.append("a,%d,%d,%d,%d" % (current, SPEEDS[rng.next(len(SPEEDS))], rng.next(3), rng.next(2)))
            elif kind < 18:
                steps.append("a,-1,0,0,0")
                current = -1
            else:
                current = pick()
                steps.append(animate(current))
        lines.append("T\t%d\t%s" % (number % 2, ";".join(steps)))
    return lines


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    bases, sets, seqs = collect(cache)
    lines = cases(bases, sets, seqs)
    out = bytearray(struct.pack(">i", len(bases)))
    for archive in sorted(bases):
        out += struct.pack(">ii", archive, len(bases[archive])) + bases[archive]
    out += struct.pack(">i", len(sets))
    for group in sorted(sets):
        out += struct.pack(">ii", group, len(sets[group]))
        for file in sorted(sets[group]):
            out += struct.pack(">ii", file, len(sets[group][file])) + sets[group][file]
    out += struct.pack(">i", len(seqs))
    for seq in sorted(seqs):
        out += struct.pack(">ii", seq, len(seqs[seq])) + seqs[seq]
    (work / "anim.bundle").write_bytes(bytes(out))
    (work / "anim-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "AnimOracle.java"
    classes = work / "anim-classes"
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
    output = work / "anim-oracle.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "AnimOracle", str(jar), str(work / "anim.bundle"),
                   str(work / "anim-cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    return output.read_bytes(), result.stdout.strip(), (len(bases), len(sets), len(seqs)), source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=DEFAULT_JAR)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".darkanrs/caches/darkan/LIVE")
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    original = base.sha(args.jar.read_bytes())
    if original != base.PINNED:
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
    try:
        tsv, oracle_output, sizes, source = run(args, work, jar)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    digest = base.sha(tsv)
    lines = tsv.decode("ascii").splitlines()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "basesInBundle": sizes[0],
        "framesetsInBundle": sizes[1],
        "seqTypesInBundle": sizes[2],
        "cases": len(lines),
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("anim oracle fixture differs; rerun without --check to regenerate")
        evidence["check"] = "match"
    else:
        FIXTURE.parent.mkdir(parents=True, exist_ok=True)
        with gzip.GzipFile(FIXTURE, "wb", compresslevel=9, mtime=0) as handle:
            handle.write(tsv)
        DIGEST.write_text(digest + "\n")
        evidence["fixtureBytes"] = FIXTURE.stat().st_size
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
