import argparse
import gzip
import importlib.util
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CRATE = HERE.parents[1]
spec = importlib.util.spec_from_file_location("generate_mixer_oracle", HERE / "generate_mixer_oracle.py")
mix = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mix)
base = mix.base

FIXTURES = CRATE / "tests/fixtures"
NAMES = ("pcm-cases.tsv", "pcm-oracle.tsv")
VALID = 1500
INJECTED = 300


def voice_spec(rng, injected):
    length = rng.pick((4, 8, 16, 24, 40))
    start = rng.between(0, length)
    end = rng.between(start, length)
    if rng.next(3) == 0 and length > 3:
        start = rng.between(0, length - 3)
        end = rng.between(start + 2, length)
    if injected and rng.next(3) == 0:
        end = length + rng.between(1, 4)
    loop = rng.next(2)
    rate = rng.pick((256, 256, 512, 128, 300, 77, 1000, 64, -256))
    volume = rng.pick((1000, 8000, 16384, 30000, rng.between(100, 40000), 0))
    pan = rng.pick((-1, -1, 0, 4096, 8192, 12000, 16384))
    loops = rng.pick((-1, 0, 1, 2, 5))
    return "%s,%d,%d,%d,%d,%d,%d,%d" % (
        mix.samples_hex(rng, length), start, end, loop, rate, volume, pan, loops
    )


def case(rng, number, injected):
    stereo = rng.next(2)
    count = rng.pick((1, 2, 3, 5, 8, 12, 20, 33, 36, 40, 48))
    voices = [voice_spec(rng, injected) for _ in range(count)]
    ops = ["R%d" % rng.between(1, 6)]
    for _ in range(rng.between(2, 8)):
        kind = rng.next(100)
        if kind < 40:
            ops.append("R%d" % rng.between(1, 12))
        elif kind < 48:
            ops.append("K")
        elif kind < 60:
            ops.append("V%d,%d" % (rng.between(0, count - 1), rng.between(0, 400)))
        elif kind < 70:
            ops.append("N%d,%d" % (rng.between(0, count - 1), rng.pick((-1, 0, 8192, 16000))))
        elif kind < 80:
            ops.append("O%d,%d" % (rng.between(0, count - 1), rng.pick((0, 1, 40, 500))))
        elif kind < 88:
            ops.append("X%d" % rng.between(0, count - 1))
        else:
            ops.append("A" + voice_spec(rng, injected))
            count += 1
    ops.append("R%d" % rng.between(1, 10))
    if injected and rng.next(5) == 0:
        ops.append("V%d,5" % (count + 3))
    return "P\t%d\t%d\t%s\t%s" % (number, stereo, "/".join(voices), ";".join(ops))


def cases():
    rng = mix.Lcg(0x5EED0A0D10)
    lines = []
    for number in range(VALID + INJECTED):
        lines.append(case(rng, number, number >= VALID))
    return lines


def run(work, jar, lines):
    (work / "pcm-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "PcmOracle.java"
    classes = work / "pcm-classes"
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
    output = work / "pcm-oracle.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx2g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "PcmOracle", str(jar),
                   str(work / "pcm-cases.txt"), str(output)],
        check=True, capture_output=True, text=True, timeout=mix.JAVA_TIMEOUT,
    )
    return output.read_bytes(), result.stdout.strip(), source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=mix.DEFAULT_JAR)
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
    lines = cases()
    try:
        tsv, oracle_output, source = run(work, jar, lines)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    rows = tsv.decode("ascii").splitlines()
    outputs = {
        "pcm-cases.tsv": ("\n".join(lines) + "\n").encode("ascii"),
        "pcm-oracle.tsv": tsv,
    }
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "cases": len(rows),
        "valid": VALID,
        "injected": INJECTED,
        "errorRows": sum(1 for row in rows if "\terror" in row),
        "tsvSha256": base.sha(tsv),
        "oracleOutput": oracle_output,
    }
    if args.check:
        for name, data in outputs.items():
            stored = gzip.decompress((FIXTURES / (name + ".gz")).read_bytes())
            digest = (FIXTURES / (name.rsplit(".", 1)[0] + ".sha256")).read_text().strip()
            if stored != data or digest != base.sha(data):
                print(json.dumps(evidence, indent=2))
                raise SystemExit("pcm fixture %s differs; rerun without --check to regenerate" % name)
        evidence["check"] = "match"
    else:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        evidence["fixtureBytes"] = {name: mix.write_fixture(name, data) for name, data in outputs.items()}
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
