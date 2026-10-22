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

DEFAULT_JAR = CRATE.parent / "renderer/target/orbit-live-jar/pinned.jar"
FIXTURES = CRATE / "tests/fixtures"
VALID = 12000
INJECTED = 3000
JAVA_TIMEOUT = 1500


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound

    def between(self, low, high):
        return low + self.next(high - low + 1)

    def pick(self, items):
        return items[self.next(len(items))]


SPECIAL = (0x7FC00000, 0x7F800000, 0xFF800000, 0x7F7FFFFF, 0x00000001, 0x80000000, 0x4F000000, 0xCF000000, 0x3F800000, 0xBF800000)


def float_bits(value):
    return struct.unpack(">I", struct.pack(">f", value))[0]


def samples_hex(rng, length, injected):
    shape = rng.next(3)
    out = []
    for index in range(length):
        if injected and rng.next(6) == 0:
            out.append(rng.pick(SPECIAL))
        elif shape == 0:
            out.append(float_bits((rng.next(65536) - 32768) / 32768.0))
        elif shape == 1:
            out.append(float_bits(((index * 23 + 5) % 200 - 100) / 100.0))
        else:
            out.append(float_bits(((index % 4) * 0.5 - 0.7) * 1.0))
    return "".join("%08x" % bits for bits in out)


def make_op(rng, injected):
    kind = rng.next(100)
    if kind < 30:
        if injected and rng.next(4) == 0:
            return "F%d,%d" % (rng.between(-3, 70), rng.between(-3, 90))
        return "F%d,%d" % (rng.between(0, 24), rng.between(0, 40))
    if kind < 40:
        return "K%d" % rng.pick((0, 1, 2, 5, 17, 40, 100, 1000))
    if kind < 47:
        return "D%d,%d" % (rng.pick((0, 1, 3, 10, 64, 200)), rng.between(0, 30000))
    if kind < 53:
        return "P%d,%d,%d" % (rng.pick((0, 2, 9, 50, 300)), rng.between(0, 30000), rng.pick((-1, 0, 4096, 8192, 12000, 16384)))
    if kind < 58:
        return "O%d" % rng.pick((0, 1, 7, 40, 500))
    if kind < 62:
        return "V%d" % rng.between(0, 400)
    if kind < 66:
        return "N%d" % rng.pick((-1, 0, 1000, 8192, 16000, 16384))
    if kind < 74:
        return "L%d" % rng.pick((-1, 0, 1, 2, 3, 7, 40))
    if kind < 80:
        return "T%d" % rng.pick((0, 100, 256, 300, 512, 77, 1024, 9))
    if kind < 84:
        return "B"
    if kind < 90:
        return "S%d" % rng.pick((-5, -1, 0, 256, 512, 1000, 2560, 9000, 100000))
    if kind < 92:
        return "U"
    if kind < 94:
        return "C"
    if kind < 97:
        return "W%d,%d" % (rng.between(0, 30000), rng.pick((-1, 0, 5000, 16384)))
    if kind < 99:
        return "X"
    return "Y"


def buffer_case(rng, number, injected):
    length = rng.pick((1, 2, 3, 4, 5, 8, 13, 24, 40, 40))
    start = rng.between(0, length)
    end = rng.between(start, length)
    if rng.next(4) == 0:
        end = start
    if rng.next(3) == 0 and length > 3:
        start = rng.between(0, length - 3)
        end = rng.between(start + 2, length)
    if injected:
        kind = rng.next(4)
        if kind == 0:
            start, end = end, start
        elif kind == 1:
            end = length + rng.between(1, 5)
        elif kind == 2:
            start = -rng.between(1, 4)
        else:
            length = 0
    stereo = rng.next(2)
    loop = rng.next(2)
    rate = rng.pick((256, 256, -256, 512, 128, 300, -300, 77, -77, 1000, -1000, 700, 64, 128))
    volume = rng.pick((1, 1000, 8000, 16384, 30000, rng.between(100, 40000), rng.between(100, 40000), 0))
    pan = rng.pick((-1, -1, 0, 4096, 8192, 12000, 16384))
    ops = ["F%d,%d" % (rng.between(0, 24), rng.between(1, 40))]
    if rng.next(3) == 0:
        ops.append("L%d" % rng.pick((-1, 1, 2, 5)))
    for _ in range(rng.between(2, 9)):
        ops.append(make_op(rng, injected))
    ops.append("F%d,%d" % (rng.between(0, 24), rng.between(0, 40)))
    data = samples_hex(rng, length, injected)
    return "M\t%d\t%d\t%d\t%d\t%d\t%s\t%d\t%d\t%d\t%s" % (
        number, stereo, loop, start, end, data, rate, volume, pan, ";".join(ops)
    )


def cases():
    rng = Lcg(0xAB0FF3A5)
    lines = []
    for number in range(VALID + INJECTED):
        lines.append(buffer_case(rng, number, number >= VALID))
    return lines


def run(work, jar, lines):
    (work / "abuf-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "AudioBufferStreamOracle.java"
    classes = work / "abuf-classes"
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
    output = work / "abuf-oracle.tsv"
    result = subprocess.run(
        sandbox + ["java", "-Xmx2g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
                   "-cp", f"{classes}{os.pathsep}{jar}", "AudioBufferStreamOracle", str(jar),
                   str(work / "abuf-cases.txt"), str(output)],
        check=True, capture_output=True, text=True, timeout=JAVA_TIMEOUT,
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
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work-abuf")
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
        "abuf-cases.tsv": ("\n".join(lines) + "\n").encode("ascii"),
        "abuf-oracle.tsv": tsv,
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
                raise SystemExit("audio buffer fixture %s differs; rerun without --check to regenerate" % name)
        evidence["check"] = "match"
    else:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        evidence["fixtureBytes"] = {name: write_fixture(name, data) for name, data in outputs.items()}
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
