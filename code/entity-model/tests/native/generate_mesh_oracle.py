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
MERGE_CASES = 4000
TRANSFORM_CASES = 4000
DEFAULT_JAR = CRATE.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
FIXTURE = CRATE / "tests/fixtures/mesh-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/mesh-oracle.sha256"


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound


def ops(rng):
    count = rng.next(4)
    chosen = []
    for _ in range(count):
        kind = rng.next(8)
        if kind == 0:
            chosen.append("RF:%d" % rng.next(65536))
        elif kind == 1:
            chosen.append("R:%d:%d" % (rng.next(65536), rng.next(65536)))
        elif kind == 2:
            chosen.append("TF:%d" % rng.next(32768))
        elif kind == 3:
            chosen.append("T:%d:%d" % (rng.next(2000), rng.next(2000)))
        elif kind == 4:
            def angle():
                pick = rng.next(10)
                if pick == 0:
                    return 0
                if pick == 1:
                    return [16384, -5, 40000][rng.next(3)]
                return rng.next(16384)

            chosen.append("ROT:%d:%d:%d" % (angle(), angle(), angle()))
        elif kind == 5:
            chosen.append("UP")
        else:
            chosen.append("TR:%d:%d:%d" % (rng.next(4001) - 2000, rng.next(4001) - 2000, rng.next(4001) - 2000))
    return "|".join(chosen) if chosen else "-"


def build(cache):
    index = base.Index(cache, MODEL_INDEX)
    ids = sorted(index.table)
    decode_ids = ids[::DECODE_STRIDE]
    models = {}
    for model in decode_ids:
        data = index.file(model, 0)
        if data is not None:
            models[model] = data
        index.loaded.pop(model, None)
    pool = [model for model in decode_ids if model in models and 0 < len(models[model]) < POOL_LIMIT]
    cases = ["TRIG\t-\t-"]
    for model in decode_ids:
        if model in models:
            cases.append("D\t%d\t-" % model)
    rng = Lcg(0x5EED0001)
    for number in range(MERGE_CASES):
        parts = 2 + number % 3
        picked = [pool[rng.next(len(pool))] for _ in range(parts)]
        cases.append("M\t%s\t%s" % (",".join(map(str, picked)), ops(rng)))
    for _ in range(TRANSFORM_CASES):
        cases.append("X\t%d\t%s" % (pool[rng.next(len(pool))], ops(rng)))
    return models, cases, len(ids)


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    models, cases, total = build(cache)
    out = bytearray(struct.pack(">i", len(models)))
    for model in sorted(models):
        out += struct.pack(">ii", model, len(models[model]))
        out += models[model]
    (work / "models.bundle").write_bytes(bytes(out))
    (work / "cases.txt").write_text("\n".join(cases) + "\n", encoding="ascii")
    source = HERE / "MeshOracle.java"
    classes = work / "classes"
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
    output = work / "mesh-oracle.tsv"
    result = subprocess.run(
        sandbox + ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "MeshOracle", str(jar), str(work / "models.bundle"),
                   str(work / "cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    return output.read_bytes(), result.stdout.strip(), len(models), total, source


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
        tsv, oracle_output, decoded, total, source = run(args, work, jar)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    digest = base.sha(tsv)
    lines = tsv.decode("ascii").splitlines()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "modelArchives": total,
        "modelsInBundle": decoded,
        "cases": len(lines),
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("mesh oracle fixture differs; rerun without --check to regenerate")
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
