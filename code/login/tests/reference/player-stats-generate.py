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
HELPER = REFERENCE / "player-stats-oracle.java"
GENERATOR = REFERENCE / "player-stats-generate.py"
VALIDATION = ROOT / "target/iteration16-validation/player-stats"
SOURCE_PATHS = (
    "code/client/src/main/kotlin/com/jagex/game/runetek5/buffer/Packet.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/client/Client.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/client/SkillConstants.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/BufferedConnectionContext.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/Connection.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/ConnectionPinger.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/PingRequester.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/packet/PacketDecoder.kt",
    "code/client/src/main/kotlin/com/jagex/game/runetek5/network/packet/ServerProt.kt",
    "code/client/src/main/kotlin/com/darkan/bot/Bot.kt",
    "code/client/src/main/kotlin/com/darkan/bot/scripts/ScriptExecutor.kt",
    "code/client/src/main/kotlin/com/darkan/bot/scripts/event/XPDrop.kt",
)
CLASS_PATHS = (
    "com/jagex/game/runetek5/buffer/Packet.class",
    "com/jagex/game/runetek5/buffer/Packet$Bit.class",
    "com/jagex/game/runetek5/client/Client.class",
    "com/jagex/game/runetek5/client/Client$Companion.class",
    "com/jagex/game/runetek5/client/SkillConstants.class",
    "com/jagex/game/runetek5/client/SkillConstants$Companion.class",
    "com/jagex/game/runetek5/network/BufferedConnectionContext.class",
    "com/jagex/game/runetek5/network/Connection.class",
    "com/jagex/game/runetek5/network/ConnectionPinger.class",
    "com/jagex/game/runetek5/network/PingRequester.class",
    "com/jagex/game/runetek5/network/packet/PacketDecoder.class",
    "com/jagex/game/runetek5/network/packet/ServerProt.class",
    "com/darkan/bot/Bot.class",
    "com/darkan/bot/scripts/ScriptExecutor.class",
    "com/darkan/bot/scripts/event/XPDrop.class",
)
NATIVE_FILES = {
    "player-stats-initial.native-values",
    "player-stats-tables.native-values",
    "player-stats-registry.tsv",
    "player-stats-payloads.bin",
    "player-stats-native-values.bin",
    "player-stats-cases.tsv",
}


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            checksum.update(block)
    return checksum.hexdigest()


def assert_pinned(jar):
    actual = digest(jar)
    if actual != PINNED_SHA256:
        raise RuntimeError("Pinned native jar hash mismatch: " + actual)


def run_sandbox(command, evidence, label):
    bwrap = shutil.which("bwrap")
    if bwrap is None:
        raise RuntimeError("bwrap is required for both javac and java")
    invocation = [
        bwrap, "--unshare-net", "--ro-bind", "/", "/",
        "--bind", str(evidence), str(evidence),
        "--ro-bind", str(PINNED_JAR), str(PINNED_JAR),
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
    if set(generated) != NATIVE_FILES or len(list(output.iterdir())) != len(NATIVE_FILES):
        raise RuntimeError("Unexpected native output set")
    if generated["player-stats-initial.native-values"] != bytes(444):
        raise RuntimeError("Unexpected initial native state")
    tables = generated["player-stats-tables.native-values"]
    if len(tables) != 580:
        raise RuntimeError("Unexpected native tables length")
    values = struct.unpack(">145i", tables)
    if values[:25] != (99,) * 24 + (120,):
        raise RuntimeError("Unexpected native skill caps")
    registry = generated["player-stats-registry.tsv"].decode("ascii")
    if registry != "name\topcode\tlength\nUPDATE_STAT\t140\t6\nRUN_ENERGY\t64\t1\nPLAYER_WEIGHT\t14\t2\n":
        raise RuntimeError("Unexpected native registry")
    rows = generated["player-stats-cases.tsv"].decode("ascii").splitlines()
    if rows[0] != "order\tname\topcode\toffset\tlength":
        raise RuntimeError("Unexpected cases header")
    offset = 0
    names = set()
    for index, row in enumerate(rows[1:]):
        order, name, opcode, start, length = row.split("\t")
        if int(order) != index or name in names or int(start) != offset:
            raise RuntimeError("Invalid native cases ordering")
        names.add(name)
        if {140: 6, 64: 1, 14: 2}.get(int(opcode)) != int(length):
            raise RuntimeError("Invalid native case registry size")
        if struct.unpack_from(">i", generated["player-stats-native-values.bin"], index * 444)[0] != int(length):
            raise RuntimeError("Unexpected native consumed count")
        offset += int(length)
    count = len(rows) - 1
    if len(generated["player-stats-payloads.bin"]) != offset:
        raise RuntimeError("Unexpected combined payload size")
    if len(generated["player-stats-native-values.bin"]) != count * 444:
        raise RuntimeError("Unexpected combined snapshot size")
    required = {"visible-u8-" + str(value) for value in range(256)}
    required.update("energy-u8-" + str(value) for value in range(256))
    required.update("initial-zero-skill-" + str(value) for value in range(25))
    required.update(
        "threshold-skill-" + str(skill) + "-" + str(threshold) + "-" + relation
        for skill in (0, 24) for threshold in range(120) for relation in ("below", "equal", "above")
    )
    if not required <= names:
        raise RuntimeError("Missing native coverage")
    return generated, count


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=pathlib.Path, nargs="?", default=PINNED_JAR)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    if jar != PINNED_JAR.resolve(strict=True):
        raise RuntimeError("Only the read-only pinned native jar is permitted")
    assert_pinned(jar)
    if FIXTURES.is_symlink():
        raise RuntimeError("Fixture directory must not be a symlink")
    VALIDATION.mkdir(parents=True, exist_ok=True)
    evidence = pathlib.Path(tempfile.mkdtemp(prefix="oracle-", dir=VALIDATION))
    output = evidence / "fixtures"
    classes = evidence / "classes"
    for directory in (output, classes, evidence / "home"):
        directory.mkdir()
    print("Player-stats oracle evidence: " + str(evidence), flush=True)
    (evidence / "jar-before.sha256").write_text(PINNED_SHA256 + "\n", encoding="ascii")
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
            "-cp", str(jar), "-d", str(classes), str(HELPER),
        ], evidence, "javac")
        run_sandbox([
            java, "-Djava.awt.headless=true", "-Duser.home=" + str(evidence / "home"),
            "-Djava.io.tmpdir=/tmp", "-XX:-UsePerfData", "-Dbot.enabled=false",
            "-cp", str(jar) + os.pathsep + str(classes),
            "PlayerStatsOracle", str(output), str(jar),
        ], evidence, "java")
        generated, count = inspect_outputs(output)
        with zipfile.ZipFile(jar) as archive:
            native_classes = {name: hashlib.sha256(archive.read(name)).hexdigest() for name in CLASS_PATHS}
        if digest(HELPER) != helper_hash or digest(GENERATOR) != generator_hash:
            raise RuntimeError("Oracle tooling changed during generation")
        if before != {path: digest(REPOSITORY / path) for path in SOURCE_PATHS}:
            raise RuntimeError("Inspected native sources changed during generation")
        provenance = {
            "schema": "player-stats-native-oracle-v1",
            "jar": {"path": str(jar.relative_to(REPOSITORY)), "sha256": PINNED_SHA256, "mode": "direct-read-only"},
            "helper": {"path": str(HELPER.relative_to(REPOSITORY)), "sha256": helper_hash},
            "generator": {"path": str(GENERATOR.relative_to(REPOSITORY)), "sha256": generator_hash},
            "working_tree_source_sha256": before,
            "pinned_jar_class_sha256": native_classes,
            "invocation": "PacketDecoder.decode(BufferedConnectionContext) with preset currentPacket/currentPacketSize and fake Connection",
            "sandbox": {"javac": "bwrap --unshare-net --ro-bind / /", "java": "bwrap --unshare-net --ro-bind / /", "writable": "unique oracle evidence directory, tmpfs /tmp, proc /proc, dev /dev"},
            "snapshot": {"byte_order": "big-endian", "integer_type": "signed-i32", "bytes": 444, "layout": "grouped-arrays-not-interleaved", "fields": ["consumed", "xp[25]", "visible[25]", "actual[25]", "energy", "weight", "cursor", "recent[32]"]},
            "tables": {"byte_order": "big-endian", "integer_type": "signed-i32", "fields": ["caps[25]", "xp_thresholds[120]"]},
            "cases": count,
            "native_output_sha256": {name: hashlib.sha256(contents).hexdigest() for name, contents in sorted(generated.items())},
        }
        generated["player-stats-oracle.sha256"] = (PINNED_SHA256 + "\n").encode("ascii")
        generated["player-stats-provenance.json"] = (json.dumps(provenance, indent=2, sort_keys=True) + "\n").encode("utf-8")
        for name in ("player-stats-oracle.sha256", "player-stats-provenance.json"):
            (output / name).write_bytes(generated[name])
        assert_pinned(jar)
        existing_paths = list(FIXTURES.glob("player-stats-*"))
        if any(path.is_symlink() or not path.is_file() for path in existing_paths):
            raise RuntimeError("Unexpected player-stats fixture path")
        existing = {path.name: path.read_bytes() for path in existing_paths}
        if args.check:
            changed = sorted(name for name in generated.keys() | existing.keys() if generated.get(name) != existing.get(name))
            if changed:
                raise RuntimeError("Player-stats fixtures differ: " + ", ".join(changed))
        else:
            stale = set(existing) - set(generated)
            if stale:
                raise RuntimeError("Unexpected existing player-stats fixtures: " + ", ".join(sorted(stale)))
            FIXTURES.mkdir(parents=True, exist_ok=True)
            for name, contents in generated.items():
                (FIXTURES / name).write_bytes(contents)
        summary = "Player-stats fixtures " + ("checked" if args.check else "generated") + "; cases=" + str(count) + "; pinned jar sha256=" + PINNED_SHA256
        (evidence / "result.txt").write_text(summary + "\n", encoding="ascii")
        print(summary)
        print((evidence / "java.stdout").read_text(encoding="utf-8"), end="")
    finally:
        assert_pinned(jar)
        (evidence / "jar-after.sha256").write_text(PINNED_SHA256 + "\n", encoding="ascii")


if __name__ == "__main__":
    main()
