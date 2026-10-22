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
DECODE_STRIDE = 3
POOL_LIMIT = 5000
CASES = 9000
DEFAULT_JAR = CRATE.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
FIXTURE = CRATE / "tests/fixtures/model-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/model-oracle.sha256"
MATERIALS = CRATE / "tests/fixtures/model-oracle-materials.bin"

ENTITY_BITS = [0x20, 0x80, 0x100, 0x400, 0x10000]
LIGHTS = [(64, 768), (64, 850), (0, 768), (-20, 500), (127, 1000), (64 + 40, 5 * 100 + 850), (10, 0), (0, 5)]
PREFERENCES = [0, 0, 0, 0x40, 0x40, 0x41, 0x8000]


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound

    def pick(self, items):
        return items[self.next(len(items))]


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


def bundle(models):
    out = bytearray(struct.pack(">i", len(models)))
    for model in sorted(models):
        out += struct.pack(">ii", model, len(models[model])) + models[model]
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
    source = HERE / "ModelOracle.java"
    classes = work / "model-classes"
    classes.mkdir(exist_ok=True)
    sandbox = sandbox_for(work)
    subprocess.run(sandbox + ["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(classes), str(source)], check=True)
    output = work / f"{name}.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "ModelOracle", str(jar), str(work / f"{name}.bundle"),
                   str(work / "materials.bin"), str(work / f"{name}-cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    return output.read_text(encoding="ascii").splitlines(), result.stdout.strip(), source


def material_count():
    payload = MATERIALS.read_bytes()
    return int.from_bytes(payload[:2], "big")


def build_cases(models, palettes, materials, rng):
    ids = sorted(models)
    with_billboards = [m for m in ids if palettes[m][2] == "1"]

    def spec():
        pick = rng.next(100)
        if pick < 10 and with_billboards:
            return str(rng.pick(with_billboards))
        if pick < 35:
            return ",".join(str(rng.pick(ids)) for _ in range(2 + rng.next(2)))
        return str(rng.pick(ids))

    def palette(spec_text, which):
        values = []
        for part in spec_text.split(","):
            text = palettes[int(part)][which]
            if text != "-":
                values.extend(int(v) for v in text.split(","))
        return values

    def colour(pool):
        if pool and rng.next(10) < 7:
            return rng.pick(pool)
        return rng.next(65536) - 32768 if rng.next(2) else rng.next(8192)

    def material(pool):
        roll = rng.next(10)
        if pool and roll < 5:
            return rng.pick(pool)
        if roll < 7:
            return -1
        return rng.next(materials)

    def ops(spec_text):
        colours = palette(spec_text, 0)
        mats = palette(spec_text, 1)
        result = []
        for _ in range(rng.next(5)):
            kind = rng.next(9)
            if kind == 0:
                result.append("r:%d:%d" % (colour(colours), colour([])))
            elif kind == 1:
                result.append("m:%d:%d" % (material(mats), material([])))
            elif kind == 2:
                result.append("t:%d:%d:%d:%d" % (rng.pick([-1, rng.next(64)]), rng.pick([-1, rng.next(8)]), rng.pick([-1, rng.next(128)]), rng.next(129)))
            elif kind == 3:
                result.append("a:%d" % (rng.next(160) - 20))
            elif kind == 4:
                result.append("c:%d" % rng.pick([768, 850, 1000, 500, rng.next(2000)]))
            elif kind == 5:
                result.append("e:%d" % (rng.next(256) - 128))
            elif kind == 6:
                result.append("f:%d:%d" % (rng.next(256) - 128, rng.next(40)))
            else:
                result.append("s:%d" % rng.pick([0x10000, 0, 0x10020, 0x20]))
        return ";".join(result) if result else "-"

    lines = []
    for _ in range(CASES):
        text = spec()
        mask = 0
        for bit in ENTITY_BITS:
            if rng.next(10) < 7:
                mask |= bit
        mask |= rng.next(0x20) if rng.next(4) == 0 else 0
        ambient, contrast = rng.pick(LIGHTS)
        mode = rng.pick([0, 1, 1, 1, 2, 2])
        bb_material = -1 if rng.next(5) else rng.next(materials)
        lines.append("M\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%s" % (
            text, mask, ambient, contrast, rng.pick(PREFERENCES), mode, bb_material, ops(text)))
    return lines


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    models = load_models(cache)
    shutil.copyfile(MATERIALS, work / "materials.bin")
    visits = ["V\t%d" % model for model in sorted(models)]
    visited, _, _ = execute(work, jar, "model-visit", bundle(models), visits)
    palettes = {}
    for line in visited:
        parts = line.split("\t")
        palettes[int(parts[1])] = parts[2].split("|")
    rng = Lcg(0x5EED0C33)
    cases = build_cases(models, palettes, material_count(), rng)
    used = set()
    for line in cases:
        for part in line.split("\t")[1].split(","):
            used.add(int(part))
    lines, output, source = execute(work, jar, "model-oracle", bundle({m: models[m] for m in used}), cases)
    return "\n".join(lines).encode("ascii") + b"\n", output, source, (len(models), sum(1 for p in palettes.values() if p[2] == "1"))


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
        "materialsSha256": base.sha(MATERIALS.read_bytes()),
        "modelsSampled": sizes[0],
        "modelsWithBillboards": sizes[1],
        "cases": len(lines),
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "nullBuilds": sum(1 for line in lines if line.endswith("/null")),
        "buildErrors": sum(1 for line in lines if line.endswith("/error")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("model oracle fixture differs; rerun without --check to regenerate")
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
