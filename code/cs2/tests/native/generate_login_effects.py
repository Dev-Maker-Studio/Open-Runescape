import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_world_map import JAR_SHA256, digest

FIXTURES = ("login-effects.native.tsv",)
SOURCES = ("LoginEffectFixtures.java", "LoginEffectHooks.java", "LoginEffectBoot.java")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    work = (crate / "target/native-login-effects").resolve()
    before = digest(jar)
    if before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256")
    classes = work / "classes"
    output = work / "fixtures"
    inputs = work / "inputs"
    for path in (classes, output, inputs):
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
            "java", "-Djava.awt.headless=true", *(["-Dfixture.debug=1"] if os.environ.get("FIXTURE_DEBUG") else []), "-XX:-OmitStackTraceInFastThrow",
            "-cp", str(classes),
            "LoginEffectBoot", str(output), str(inputs), str(classes), str(copied),
        ], cwd=work, check=True)
    finally:
        if digest(jar) != before or digest(copied) != JAR_SHA256:
            raise SystemExit("Native jar changed during login effect fixture generation")
    destination = crate / "tests/fixtures"
    names = {path.name for path in output.iterdir()}
    if names != set(FIXTURES):
        raise SystemExit("Unexpected login effect oracle output files")
    lines = (output / FIXTURES[0]).read_text().splitlines()
    cases = sum(1 for line in lines if line.startswith("C\t"))
    if int(lines[-1].split("\t")[1]) != cases:
        raise SystemExit("Trailer does not match")
    if args.check:
        for name in FIXTURES:
            if (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native login effect fixture differs: {name}")
        return
    for name in FIXTURES:
        shutil.copyfile(output / name, destination / name)
    manifest = {
        "oracle": "Actual CameraLoginPrefOps.dispatch for the LOGIN_*, CREATE_* and LOBBY_* network-backed opcodes, run through the real LoginManager (startWorldLogin, startLobbyLogin, initializeLogin, cancelLogin, resetLoginState and the base64 token path), AccountCreationStage, the real BufferedConnectionContext queue and the real PacketEncoder sign-up and email packets, whose queued bytes are XTEA-decrypted and parsed by the driver. The jar copy is loaded by a child-first class loader that rewrites only Client$Companion.setGameState(GameState)V and logout(Z)V (java.lang.classfile) to call LoginEffectHooks, which mirror the Kotlin branch order, record state changes, prepare-player and open-lobby calls, and close stub Connections, so engine side effects (gameConnection close, music, toolkit, region, flush and engine resets) are an effect log and not executed. No socket is opened.",
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "driver_sha256": {name: digest(crate / "tests/native" / name) for name in SOURCES},
        "generator_sha256": digest(Path(__file__).resolve()),
        "fixture_sha256": {name: digest(destination / name) for name in FIXTURES},
        "cases": cases,
        "random_seed": "0x727c5228 ^ 0x5eed (java.util.Random)",
        "reproduction": "python3 code/cs2/tests/native/generate_login_effects.py --check",
    }
    (destination / "login-effects-oracle.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
