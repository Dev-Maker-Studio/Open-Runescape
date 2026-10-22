import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = ("social-mutations.native.tsv", "social-mutations.stats.tsv")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-social-mutations").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    for path in (classes, output):
        path.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    shutil.copyfile(jar, copied)
    if digest(copied) != JAR_SHA256:
        raise SystemExit("Isolated native jar copy differs")
    sources = [crate / "tests/native" / name for name in ("SocialMutationFixtures.java",)]
    try:
        subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(copied), "-d", str(classes), *map(str, sources)], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]),
            "SocialMutationFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during social mutation fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected social mutation oracle output files")
    lines = (output / "social-mutations.native.tsv").read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("T\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Case count trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native social mutation fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual EnumEmailFriendOps.dispatch from an isolated copy of the pinned shaded client JAR; list state set directly, the real Client.GAME_CONNECTION_CONTEXT (isaac null) queue read back for packet bytes, ChatBox lines for game messages.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "driver_sha256": digest(crate / "tests/native/SocialMutationFixtures.java"),
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "opcodes": sorted({line.split("\t")[1] for line in lines if line.startswith("T\t")}),
        "random_seed": "0x50c1a15 (java.util.Random)",
        "reproduction": "python3 code/cs2/tests/native/generate_social_mutations.py --check",
    }
    (destination / "social-mutations-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
