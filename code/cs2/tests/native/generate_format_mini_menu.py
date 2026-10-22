import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_world_map import JAR_SHA256, digest

FIXTURES = ("format-mini-menu.native.tsv",)
SOURCES = ("FormatMiniMenuFixtures.java",)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = Path(os.environ.get("OPENRS_ORACLE_JAR") or root / "code/harness/target/loop/mesh-oracle/oracle-client.jar").resolve()
    work = (crate / "target/native-format-mini-menu").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    for path in (classes, output):
        path.mkdir(parents=True, exist_ok=True)
    for stale in output.iterdir():
        stale.unlink()
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    sources = [crate / "tests/native" / name for name in SOURCES]
    try:
        subprocess.run(["javac", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", *([f"-Dfixture.debug=1"] if os.environ.get("FIXTURE_DEBUG") else []), "-XX:-OmitStackTraceInFastThrow",
            "-cp", f"{classes}:{copied}",
            "FormatMiniMenuFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during mini menu fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected mini menu oracle output files")
    lines = (output / FIXTURES[0]).read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native format mini menu fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual QuickChatDisplayOps.dispatch for FORMATMINIMENU through MiniMenu.close, StaticMedia.resetChatMedia and the 12 style statics from an isolated copy of the pinned client JAR; Resource.SPRITES and Resource.FONT_METRICS are an Unsafe-allocated Js5ResourceProvider whose master index is empty, so the isFileCached prefetch checks are no-ops and are not modelled; the sprite cache clears are render state and not compared; MiniMenu.activeSubmenu is null",
        "jar": str(jar.relative_to(root)) if jar.is_relative_to(root) else "external private copy",
        "jar_sha256": JAR_SHA256,
        "driver_sha256": {name: digest(crate / "tests/native" / name) for name in SOURCES},
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "random_seed": "0x727c5229 (java.util.Random)",
        "reproduction": "python3 code/cs2/tests/native/generate_mini_menu.py --check",
    }
    (destination / "format-mini-menu-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
