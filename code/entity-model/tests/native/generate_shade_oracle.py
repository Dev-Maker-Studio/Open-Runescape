import importlib.util
import gzip
import json
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CRATE = HERE.parents[1]
spec = importlib.util.spec_from_file_location("generate_model_oracle", HERE / "generate_model_oracle.py")
model = importlib.util.module_from_spec(spec)
spec.loader.exec_module(model)
base = model.base

FIXTURE = CRATE / "tests/fixtures/shade-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/shade-oracle.sha256"
CASES = 4000
MASKS = [0x10000, 0x10000, 0x10000, 0x10020, 0x10080, 0x10010, 0x10000 | 0x100, 0, 0x20, 0x10]
OFFSETS = [(0, 0, 0)] * 6 + [(0, 0, 0), (16, 0, 0), (0, -16, 0), (0, 0, 8), (64, 64, 64), (-128, 0, 128)]


def build_cases(models, rng):
    ids = sorted(models)

    def spec():
        pick = rng.next(100)
        if pick < 30:
            return ",".join(str(rng.pick(ids)) for _ in range(2 + rng.next(2)))
        return str(rng.pick(ids))

    lines = []
    for _ in range(CASES):
        first = spec()
        roll = rng.next(10)
        second = first if roll < 5 else (spec() if roll < 8 else first.split(",")[0])
        ops = []
        for _ in range(1 + rng.next(4)):
            kind = rng.next(10)
            if kind < 4:
                ops.append("x:%d:%d:%d" % rng.pick(OFFSETS))
            elif kind < 7:
                ops.append("y:%d:%d:%d" % rng.pick(OFFSETS))
            elif kind < 9:
                ops.append("s:%d" % rng.pick(MASKS))
            else:
                ops.append("u:%d" % rng.pick(MASKS))
        ambient, contrast = rng.pick(model.LIGHTS)
        lines.append("X\t%s\t%s\t%d\t%d\t%d\t%d\t%d\t%s" % (
            first, second, rng.pick(MASKS), rng.pick(MASKS), ambient, contrast, rng.pick(model.PREFERENCES), ";".join(ops)))
    return lines


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    models = model.load_models(cache)
    shutil.copyfile(model.MATERIALS, work / "materials.bin")
    cases = build_cases(models, model.Lcg(0x5AD3C0DE))
    used = set()
    for line in cases:
        f = line.split("\t")
        for part in (f[1] + "," + f[2]).split(","):
            used.add(int(part))
    lines, output, source = model.execute(work, jar, "shade-oracle", model.bundle({m: models[m] for m in used}), cases)
    return "\n".join(lines).encode("ascii") + b"\n", output, source


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=model.DEFAULT_JAR)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".darkanrs/caches/darkan/LIVE")
    parser.add_argument("--work", type=Path, default=CRATE / "target/shade-work")
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
    try:
        tsv, oracle_output, source = run(args, work, jar)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    digest = base.sha(tsv)
    lines = tsv.decode("ascii").splitlines()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "cases": len(lines),
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("shade oracle fixture differs; rerun without --check to regenerate")
        evidence["check"] = "match"
    else:
        with gzip.GzipFile(FIXTURE, "wb", compresslevel=9, mtime=0) as handle:
            handle.write(tsv)
        DIGEST.write_text(digest + "\n")
        evidence["fixtureBytes"] = FIXTURE.stat().st_size
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
