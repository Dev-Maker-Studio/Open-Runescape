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
MUSIC = HERE / "generate_music_oracle.py"
spec = importlib.util.spec_from_file_location("generate_music_oracle", MUSIC)
music = importlib.util.module_from_spec(spec)
spec.loader.exec_module(music)
base = music.base

PATCH_BASE = music.PATCH_BASE
CLEAN_STRIDE = 12
MUTANTS = 160
DEFAULT_JAR = music.DEFAULT_JAR
FIXTURES = music.FIXTURES
RATES = (22050, 44100, 11025, 32000)


def script(rng):
    ops = []
    for step in range(rng.next(7) + 3):
        pick = rng.next(14)
        if pick < 5:
            ops.append("F%d" % (64 + rng.next(3000)))
        elif pick < 7:
            ops.append("K%d" % (rng.next(6000)))
        elif pick == 7:
            ops.append("V%d" % rng.next(300))
        elif pick == 8:
            ops.append("W%d" % rng.next(300))
        elif pick == 9:
            ops.append("C%d,%d" % (rng.next(17) - 1, rng.next(300)))
        elif pick == 10:
            ops.append("B%d,%d" % (rng.next(16), rng.next(16384)))
        elif pick == 11:
            ops.append("T%d" % (rng.next(400000)))
        elif pick == 12:
            ops.append("R" if rng.next(2) else "S")
        else:
            ops.append("O")
    ops.append("F%d" % (256 + rng.next(2000)))
    return ";".join(ops)


def cases(blobs):
    lines = []
    tracks = sorted(i for i in blobs if i < PATCH_BASE)
    rng = music.Lcg(0x91D1A7C3)
    number = 0

    def add(source, kind, pos, value):
        nonlocal number
        lines.append(
            "C\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s"
            % (number, source, kind, pos, value, RATES[rng.next(4)], rng.next(2), rng.next(2), script(rng))
        )
        number += 1

    for source in tracks[::CLEAN_STRIDE]:
        add(source, 0, 0, 0)
    for _ in range(MUTANTS):
        source = tracks[rng.next(len(tracks))]
        size = len(blobs[source])
        kind = 1 + rng.next(2)
        if kind == 1:
            add(source, kind, rng.next(size + 1), 0)
        else:
            add(source, kind, rng.next(size), rng.next(256))
    return lines


def run(work, jar, blobs, lines, timeout):
    (work / "music.bundle").write_bytes(music.bundle(blobs))
    (work / "midi-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    source = HERE / "MidiPcmOracle.java"
    classes = work / "midi-classes"
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
    output = work / "midi-oracle.tsv"
    command = (
        sandbox + ["java", "-Xmx3g", "-Xss512k", "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                   "-Djava.awt.headless=true", "-cp", f"{classes}{os.pathsep}{jar}", "MidiPcmOracle", str(jar),
                   str(work / "music.bundle"), str(work / "midi-cases.txt"), str(output)]
    )
    dropped = []
    while True:
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout)
        if result.returncode == 0:
            break
        if result.returncode != 3:
            raise SystemExit("oracle failed: " + result.stderr[-2000:])
        stuck = result.stdout.strip().splitlines()[-1].split()[1]
        dropped.append(stuck)
        lines[:] = [line for line in lines if line.split("\t")[1] != stuck]
        (work / "midi-cases.txt").write_text("\n".join(lines) + "\n", encoding="ascii")
    return output.read_bytes(), result.stdout.strip() + " dropped=" + ",".join(dropped), source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=DEFAULT_JAR)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".darkanrs/caches/darkan/LIVE")
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work-music")
    parser.add_argument("--timeout", type=int, default=1800)
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
    blobs = music.collect(cache)
    lines = cases(blobs)
    try:
        tsv, oracle_output, source = run(work, jar, blobs, lines, args.timeout)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    rows = tsv.decode("ascii").splitlines()
    outputs = {
        "midi-cases.tsv": ("\n".join(lines) + "\n").encode("ascii"),
        "midi-oracle.tsv": tsv,
    }
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "tracksInBundle": sum(1 for i in blobs if i < PATCH_BASE),
        "patchesInBundle": sum(1 for i in blobs if i >= PATCH_BASE),
        "cases": len(rows),
        "injectedMutants": MUTANTS,
        "errorRows": sum(1 for row in rows if "\terror\t" in row),
        "tsvSha256": base.sha(tsv),
        "oracleOutput": oracle_output,
    }
    if args.check:
        for name, data in outputs.items():
            stored = gzip.decompress((FIXTURES / (name + ".gz")).read_bytes())
            digest = (FIXTURES / (name.rsplit(".", 1)[0] + ".sha256")).read_text().strip()
            if stored != data or digest != base.sha(data):
                print(json.dumps(evidence, indent=2))
                raise SystemExit("midi fixture %s differs; rerun without --check to regenerate" % name)
        evidence["check"] = "match"
    else:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        evidence["fixtureBytes"] = {name: music.write_fixture(name, data) for name, data in outputs.items()}
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
