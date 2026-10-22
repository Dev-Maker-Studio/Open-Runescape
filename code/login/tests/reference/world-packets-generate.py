import argparse
import hashlib
import json
import os
import pathlib
import shutil
import struct
import subprocess
import tempfile
import zipfile


ROOT = pathlib.Path(__file__).resolve().parents[2]
REPOSITORY = ROOT.parents[1]
REFERENCE = ROOT / "tests/reference"
FIXTURES = ROOT / "tests/fixtures"
PINNED_JAR = REPOSITORY / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
PINNED_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
SHARED_JAR = REPOSITORY / "code/client/build/libs/darkan-client-shaded-2.0.0-all.jar"
HELPER = REFERENCE / "world-packets-oracle.java"
GENERATOR = REFERENCE / "world-packets-generate.py"
VALIDATION = ROOT / "target/world-packets-validation"
K = "code/client/src/main/kotlin/com/jagex/game/runetek5/"
SOURCE_PATHS = (
    K + "buffer/Packet.kt",
    K + "client/ChatMessageType.kt",
    K + "encoding/Huffman.kt",
    K + "encoding/HuffmanList.kt",
    K + "interaction/Ignore.kt",
    K + "interaction/IgnoreList.kt",
    K + "interaction/chat/ChatBox.kt",
    K + "interaction/chat/ChatLine.kt",
    K + "interaction/chat/clanchat/channel/ClanChannel.kt",
    K + "utils/TextUtils.kt",
    K + "utils/Utils.kt",
    K + "client/Client.kt",
    K + "client/periodic/PulseEvent.kt",
    K + "client/periodic/PulseEventHandler.kt",
    K + "client/periodic/PulseEventQueue.kt",
    K + "entity/pathingentity/player/PlayerUpdate.kt",
    K + "entity/pathingentity/player/PlayerEntity.kt",
    K + "graphics/hint/HintArrow.kt",
    K + "graphics/hint/HintDetails.kt",
    K + "graphics/hint/HintTrail.kt",
    K + "config/cutscene/CutsceneTypeList.kt",
    K + "network/BufferedConnectionContext.kt",
    K + "network/Connection.kt",
    K + "network/packet/PacketDecoder.kt",
    K + "network/packet/ServerProt.kt",
    K + "network/packet/UpdateZonePacket.kt",
    "code/client/src/main/kotlin/com/darkan/bot/Bot.kt",
)
CLASS_PATHS = (
    "com/jagex/game/runetek5/client/Client.class",
    "com/jagex/game/runetek5/client/ChatMessageType.class",
    "com/jagex/game/runetek5/encoding/Huffman.class",
    "com/jagex/game/runetek5/encoding/HuffmanList.class",
    "com/jagex/game/runetek5/interaction/Ignore.class",
    "com/jagex/game/runetek5/interaction/IgnoreList.class",
    "com/jagex/game/runetek5/interaction/chat/ChatBox.class",
    "com/jagex/game/runetek5/interaction/chat/ChatLine.class",
    "com/jagex/game/runetek5/interaction/chat/clanchat/channel/ClanChannel.class",
    "com/jagex/game/runetek5/utils/TextUtils.class",
    "com/jagex/game/runetek5/utils/Utils.class",
    "com/jagex/game/runetek5/client/Client$Companion.class",
    "com/jagex/game/runetek5/client/periodic/PulseEvent.class",
    "com/jagex/game/runetek5/client/periodic/PulseEventHandler.class",
    "com/jagex/game/runetek5/client/periodic/PulseEventQueue.class",
    "com/jagex/game/runetek5/entity/pathingentity/player/PlayerUpdate.class",
    "com/jagex/game/runetek5/entity/pathingentity/player/PlayerEntity.class",
    "com/jagex/game/runetek5/graphics/hint/HintArrow.class",
    "com/jagex/game/runetek5/graphics/hint/HintDetails.class",
    "com/jagex/game/runetek5/graphics/hint/HintTrail.class",
    "com/jagex/game/runetek5/config/cutscene/CutsceneTypeList.class",
    "com/jagex/game/runetek5/network/BufferedConnectionContext.class",
    "com/jagex/game/runetek5/network/Connection.class",
    "com/jagex/game/runetek5/network/packet/PacketDecoder.class",
    "com/jagex/game/runetek5/network/packet/ServerProt.class",
    "com/jagex/game/runetek5/network/packet/UpdateZonePacket.class",
    "com/darkan/bot/Bot.class",
)
NATIVE_FILES = {"world-packets-cases.tsv"}


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            checksum.update(block)
    return checksum.hexdigest()


def assert_unchanged(jar, expected):
    actual = digest(jar)
    if actual != expected:
        raise RuntimeError("Native jar hash changed: " + actual + " expected " + expected)


def run_sandbox(command, evidence, label):
    bwrap = shutil.which("bwrap")
    if bwrap is None:
        raise RuntimeError("bwrap is required for both javac and java")
    invocation = [
        bwrap, "--unshare-net", "--ro-bind", "/", "/",
        "--bind", str(evidence), str(evidence),
        "--tmpfs", "/tmp", "--proc", "/proc", "--dev", "/dev",
        "--chdir", str(evidence), "--", *command,
    ]
    environment = {
        "PATH": "/usr/bin:/bin",
        "HOME": str(evidence / "home"),
        "TMPDIR": "/tmp",
        "LANG": "C.UTF-8",
        "LC_ALL": "C.UTF-8",
    }
    (evidence / (label + ".command.json")).write_text(
        json.dumps({"argv": invocation, "environment": environment}, indent=2) + "\n",
        encoding="utf-8",
    )
    with (evidence / (label + ".stdout")).open("wb") as stdout:
        with (evidence / (label + ".stderr")).open("wb") as stderr:
            result = subprocess.run(invocation, check=False, env=environment, stdout=stdout, stderr=stderr)
    (evidence / (label + ".exit-status")).write_text(str(result.returncode) + "\n", encoding="ascii")
    if result.returncode != 0:
        error = (evidence / (label + ".stderr")).read_text(encoding="utf-8", errors="replace")
        raise RuntimeError(label + " failed; evidence=" + str(evidence) + "\n" + error)


def inspect_outputs(output):
    generated = {path.name: path.read_bytes() for path in output.iterdir() if path.is_file()}
    if set(generated) != NATIVE_FILES:
        raise RuntimeError("Unexpected native output set")
    rows = generated["world-packets-cases.tsv"].decode("ascii").splitlines()
    steps = [row for row in rows if row.startswith(("C\t", "M\t", "S\t"))]
    if not rows[-1].startswith("Z\t") or int(rows[-1].split("\t")[1]) != len(steps):
        raise RuntimeError("Unexpected step total")
    if len(steps) < 2000:
        raise RuntimeError("Missing native coverage")
    return generated, len(steps)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=pathlib.Path, nargs="?", default=None)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.jar is not None:
        jar = args.jar.resolve(strict=True)
    elif PINNED_JAR.exists() and digest(PINNED_JAR) == PINNED_SHA256:
        jar = PINNED_JAR.resolve(strict=True)
    else:
        jar = SHARED_JAR.resolve(strict=True)
    if jar not in (PINNED_JAR.resolve(), SHARED_JAR.resolve()):
        raise RuntimeError("Only the pinned jar or the shared client jar is permitted")
    jar_sha256 = digest(jar)
    if jar == PINNED_JAR.resolve() and jar_sha256 != PINNED_SHA256:
        raise RuntimeError("Pinned native jar hash mismatch: " + jar_sha256)
    if FIXTURES.is_symlink():
        raise RuntimeError("Fixture directory must not be a symlink")
    VALIDATION.mkdir(parents=True, exist_ok=True)
    evidence = pathlib.Path(tempfile.mkdtemp(prefix="oracle-", dir=VALIDATION))
    output = evidence / "fixtures"
    classes = evidence / "classes"
    for directory in (output, classes, evidence / "home"):
        directory.mkdir()
    print("World packets oracle evidence: " + str(evidence), flush=True)
    private_jar = evidence / "private-copy.jar"
    shutil.copyfile(jar, private_jar)
    if digest(private_jar) != jar_sha256:
        raise RuntimeError("Private jar copy mismatch")
    (evidence / "jar-before.sha256").write_text(jar_sha256 + "\n", encoding="ascii")
    before = {path: digest(REPOSITORY / path) for path in SOURCE_PATHS}
    helper_hash = digest(HELPER)
    generator_hash = digest(GENERATOR)
    try:
        javac = shutil.which("javac")
        java = shutil.which("java")
        if javac is None or java is None:
            raise RuntimeError("javac and java are required")
        run_sandbox([
            javac, "-J-Duser.home=" + str(evidence / "home"),
            "-J-Djava.io.tmpdir=/tmp", "-encoding", "UTF-8", "--release", "17",
            "-cp", str(private_jar), "-d", str(classes), str(HELPER),
        ], evidence, "javac")
        run_sandbox([
            java, "-Djava.awt.headless=true", "-Duser.home=" + str(evidence / "home"),
            "-Djava.io.tmpdir=/tmp", "-XX:-UsePerfData", "-Dbot.enabled=false",
            "-cp", str(private_jar) + os.pathsep + str(classes),
            "WorldPacketsOracle", str(output), str(private_jar),
        ], evidence, "java")
        generated, count = inspect_outputs(output)
        with zipfile.ZipFile(private_jar) as archive:
            native_classes = {name: hashlib.sha256(archive.read(name)).hexdigest() for name in CLASS_PATHS}
        if digest(HELPER) != helper_hash or digest(GENERATOR) != generator_hash:
            raise RuntimeError("Oracle tooling changed during generation")
        if before != {path: digest(REPOSITORY / path) for path in SOURCE_PATHS}:
            raise RuntimeError("Inspected native sources changed during generation")
        provenance = {
            "schema": "world-packets-native-oracle-v1",
            "jar": {"path": str(jar.relative_to(REPOSITORY)), "sha256": jar_sha256, "pinned": jar_sha256 == PINNED_SHA256, "mode": "private-copy"},
            "helper": {"path": str(HELPER.relative_to(REPOSITORY)), "sha256": helper_hash},
            "generator": {"path": str(GENERATOR.relative_to(REPOSITORY)), "sha256": generator_hash},
            "working_tree_source_sha256": before,
            "jar_class_sha256": native_classes,
            "invocation": "PacketDecoder.decode(BufferedConnectionContext) with preset currentPacket/currentPacketSize, fake Connection, zero-filled receive buffer, pulse event queues and varVerifyId reset per case; drained PulseEvent fields recorded",
            "sandbox": {"javac": "bwrap --unshare-net --ro-bind / /", "java": "bwrap --unshare-net --ro-bind / /", "writable": "unique oracle evidence directory, tmpfs /tmp, proc /proc, dev /dev"},
            "snapshot": {"format": "see WorldPacketsOracle.apply", "strings": "null=~, else quote plus text with non-safe UTF-16 units as percent plus 4 hex digits"},
            "cases": count,
            "native_output_sha256": {name: hashlib.sha256(contents).hexdigest() for name, contents in sorted(generated.items())},
        }
        generated["world-packets-oracle.sha256"] = (jar_sha256 + "\n").encode("ascii")
        generated["world-packets-provenance.json"] = (json.dumps(provenance, indent=2, sort_keys=True) + "\n").encode("utf-8")
        for name in ("world-packets-oracle.sha256", "world-packets-provenance.json"):
            (output / name).write_bytes(generated[name])
        assert_unchanged(jar, jar_sha256)
        assert_unchanged(private_jar, jar_sha256)
        existing_paths = list(FIXTURES.glob("world-packets-*"))
        if any(path.is_symlink() or not path.is_file() for path in existing_paths):
            raise RuntimeError("Unexpected world packets fixture path")
        existing = {path.name: path.read_bytes() for path in existing_paths}
        if args.check:
            changed = sorted(name for name in generated.keys() | existing.keys() if generated.get(name) != existing.get(name))
            if changed:
                raise RuntimeError("World packets fixtures differ: " + ", ".join(changed))
        else:
            stale = set(existing) - set(generated)
            if stale:
                raise RuntimeError("Unexpected existing world packets fixtures: " + ", ".join(sorted(stale)))
            FIXTURES.mkdir(parents=True, exist_ok=True)
            for name, contents in generated.items():
                (FIXTURES / name).write_bytes(contents)
        summary = "World packets fixtures " + ("checked" if args.check else "generated") + "; cases=" + str(count) + "; jar sha256=" + jar_sha256
        (evidence / "result.txt").write_text(summary + "\n", encoding="ascii")
        print(summary)
        print((evidence / "java.stdout").read_text(encoding="utf-8"), end="")
    finally:
        assert_unchanged(jar, jar_sha256)
        (evidence / "jar-after.sha256").write_text(jar_sha256 + "\n", encoding="ascii")


if __name__ == "__main__":
    main()
