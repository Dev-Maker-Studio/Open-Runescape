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

MODEL_INDEX = 7
FRAME_INDEX = 0
BASE_INDEX = 1
DECODE_STRIDE = 3
POOL_LIMIT = 5000
SET_STRIDE = 8
PLAIN_MODELS = 300
SINGLE_CASES = 6000
DOUBLE_CASES = 3000
MASK_BITS = 256
DEFAULT_JAR = CRATE.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
FIXTURE = CRATE / "tests/fixtures/pose-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/pose-oracle.sha256"

LENGTHS = [1, 2, 3, 5, 8, 10, 16, 20, 30, 50, 100, 255]


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound


def frame_base(data):
    return (data[1] << 8) | data[2]


def load_models(cache):
    index = base.Index(cache, MODEL_INDEX)
    ids = sorted(index.table)[::DECODE_STRIDE]
    models = {}
    for model in ids:
        data = index.file(model, 0)
        if data is not None and 0 < len(data) < POOL_LIMIT:
            models[model] = data
        index.loaded.pop(model, None)
    return models


def load_sets(cache):
    index = base.Index(cache, FRAME_INDEX)
    groups = sorted(index.table)[::SET_STRIDE]
    sets = {}
    for group in groups:
        files = {}
        for file in index.ids(group):
            data = index.file(group, file)
            if data is not None and len(data) >= 3:
                files[file] = data
        index.loaded.pop(group, None)
        if files:
            sets[group] = files
    base_index = base.Index(cache, BASE_INDEX)
    bases = {}
    for files in sets.values():
        for data in files.values():
            bases.setdefault(frame_base(data), None)
    for archive in list(bases):
        if archive in base_index.table:
            bases[archive] = base_index.file(archive, 0)
            base_index.loaded.pop(archive, None)
    return sets, {key: value for key, value in bases.items() if value is not None}


def bundle(models, bases, sets):
    out = bytearray(struct.pack(">i", len(models)))
    for model in sorted(models):
        out += struct.pack(">ii", model, len(models[model])) + models[model]
    out += struct.pack(">i", len(bases))
    for archive in sorted(bases):
        out += struct.pack(">ii", archive, len(bases[archive])) + bases[archive]
    out += struct.pack(">i", len(sets))
    for group in sorted(sets):
        out += struct.pack(">ii", group, len(sets[group]))
        for file in sorted(sets[group]):
            out += struct.pack(">ii", file, len(sets[group][file])) + sets[group][file]
    return bytes(out)


def sandbox_for(work):
    bwrap = shutil.which("bwrap")
    if not bwrap:
        return []
    return [
        bwrap, "--unshare-net", "--die-with-parent", "--new-session", "--ro-bind", "/", "/",
        "--tmpfs", "/tmp", "--bind", str(work), str(work), "--proc", "/proc", "--dev", "/dev",
        "--chdir", str(work), "--",
    ]


def execute(work, jar, name, bundle_bytes, lines):
    (work / f"{name}.bundle").write_bytes(bundle_bytes)
    (work / f"{name}-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "PoseOracle.java"
    classes = work / "pose-classes"
    classes.mkdir(exist_ok=True)
    sandbox = sandbox_for(work)
    subprocess.run(sandbox + ["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(classes), str(source)], check=True)
    output = work / f"{name}.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "PoseOracle", str(jar), str(work / f"{name}.bundle"),
                   str(work / f"{name}-cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    return output.read_text(encoding="ascii").splitlines(), result.stdout.strip(), source


def mask_hex(rng):
    density = [0, 1, 2, 3, 4][rng.next(5)]
    raw = bytearray(MASK_BITS // 8)
    for bit in range(MASK_BITS):
        if density == 0:
            on = False
        elif density == 4:
            on = True
        else:
            on = rng.next(density + 1) == 0
        if on:
            raw[bit >> 3] |= 1 << (bit & 7)
    return raw.hex()


def build_cases(models, groups_models, sets, rng):
    with_groups = sorted(groups_models)
    plain = sorted(set(models) - set(groups_models))[:PLAIN_MODELS]
    set_ids = sorted(sets)

    def spec():
        pick = rng.next(100)
        if pick < 8 and plain:
            return str(plain[rng.next(len(plain))])
        if pick < 35:
            return ",".join(str(with_groups[rng.next(len(with_groups))]) for _ in range(2 + rng.next(2)))
        return str(with_groups[rng.next(len(with_groups))])

    def frame(group=None):
        group = set_ids[rng.next(len(set_ids))] if group is None else group
        files = sorted(sets[group])
        return group, files[rng.next(len(files))]

    def pair():
        group, file = frame()
        kind = rng.next(100)
        if kind < 55:
            files = sorted(sets[group])
            nxt = file + 1 if file + 1 in sets[group] else files[rng.next(len(files))]
            return group, file, group, nxt
        if kind < 75:
            return group, file, -1, -1
        other, other_file = frame()
        return group, file, other, other_file

    def timing():
        length = 0 if rng.next(40) == 0 else LENGTHS[rng.next(len(LENGTHS))]
        cycle = rng.next(max(length, 1) + 1) if rng.next(6) else 0
        return cycle, length

    lines = []
    for _ in range(SINGLE_CASES):
        g1, f1, g2, f2 = pair()
        cycle, length = timing()
        lines.append("O\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d" % (spec(), g1, f1, g2, f2, cycle, length, rng.next(8)))
    for _ in range(DOUBLE_CASES):
        g1, f1, g2, f2 = pair()
        cycle, length = timing()
        i1, j1, i2, j2 = pair()
        icycle, ilength = timing()
        mask = mask_hex(rng) if rng.next(10) else "-"
        lines.append("D\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s" % (
            spec(), g1, f1, g2, f2, cycle, length, i1, j1, i2, j2, icycle, ilength, mask))
    return lines


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    models = load_models(cache)
    sets, bases = load_sets(cache)
    visits = ["V\t%d" % model for model in sorted(models)]
    visited, _, _ = execute(work, jar, "pose-visit", bundle(models, {}, {}), visits)
    groups_models = {int(line.split("\t")[1]) for line in visited if line.endswith("\t1")}
    rng = Lcg(0x5EED0B22)
    cases = build_cases(models, groups_models, sets, rng)
    used = set()
    for line in cases:
        for part in line.split("\t")[1].split(","):
            used.add(int(part))
    used_models = {model: models[model] for model in used}
    lines, output, source = execute(work, jar, "pose-oracle", bundle(used_models, bases, sets), cases)
    return "\n".join(lines).encode("ascii") + b"\n", output, source, (len(models), len(groups_models), len(sets), len(bases))


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
        tsv, oracle_output, source, sizes = run(args, work, jar)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    digest = base.sha(tsv)
    lines = tsv.decode("ascii").splitlines()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "modelsSampled": sizes[0],
        "modelsWithVertexGroups": sizes[1],
        "framesetsInBundle": sizes[2],
        "basesInBundle": sizes[3],
        "cases": len(lines),
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("pose oracle fixture differs; rerun without --check to regenerate")
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
