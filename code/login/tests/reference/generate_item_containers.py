import argparse
import hashlib
import os
import pathlib
import shutil
import subprocess
import tempfile

EXPECTED_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
PREFIX = "item-containers-"


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def require_pinned(path):
    actual = sha256(path)
    if actual != EXPECTED_SHA256:
        raise RuntimeError(f"native jar SHA256 mismatch: expected {EXPECTED_SHA256}, got {actual}")


parser = argparse.ArgumentParser()
parser.add_argument("jar", type=pathlib.Path, nargs="?")
parser.add_argument("--check", action="store_true")
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[2]
jar = (args.jar or root.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar").resolve(strict=True)
source = root / "tests/reference/ItemContainersOracle.kt"
fixtures = root / "tests/fixtures"
require_pinned(jar)
with tempfile.TemporaryDirectory(prefix="item-containers-oracle-") as temporary:
    isolated = pathlib.Path(temporary).resolve()
    snapshot = isolated / "native.jar"
    oracle = isolated / source.name
    classes = isolated / "classes"
    home = isolated / "home"
    output = isolated / "fixtures"
    for directory in (classes, home, output):
        directory.mkdir()
    shutil.copyfile(jar, snapshot)
    shutil.copyfile(source, oracle)
    require_pinned(snapshot)
    require_pinned(jar)
    sandbox = [
        "bwrap", "--unshare-net", "--unshare-pid", "--unshare-ipc",
        "--die-with-parent", "--new-session", "--ro-bind", "/", "/",
        "--tmpfs", "/tmp", "--bind", str(isolated), str(isolated),
        "--proc", "/proc", "--dev", "/dev", "--chdir", str(isolated),
        "--clearenv", "--setenv", "PATH", os.defpath,
        "--setenv", "HOME", str(home), "--setenv", "TMPDIR", str(isolated),
        "--setenv", "LANG", "C.UTF-8", "--"
    ]
    java = [
        "java", "-Djava.awt.headless=true", f"-Duser.home={home}",
        f"-Djava.io.tmpdir={isolated}", "-Dbot.enabled=false"
    ]
    try:
        subprocess.run(sandbox + java + [
            "-cp", str(snapshot), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-classpath", str(snapshot),
            "-jvm-target", "17", "-d", str(classes), str(oracle)
        ], check=True)
        require_pinned(snapshot)
        require_pinned(jar)
        subprocess.run(sandbox + java + [
            "-cp", f"{snapshot}:{classes}", "ItemContainersOracleKt", str(output)
        ], check=True)
    finally:
        require_pinned(snapshot)
        require_pinned(jar)
    (output / "item-containers-oracle.sha256").write_text(EXPECTED_SHA256 + "\n", encoding="ascii")
    generated = {path.name: path.read_bytes() for path in output.iterdir()}
    if any(not name.startswith(PREFIX) for name in generated):
        raise RuntimeError("oracle wrote an unexpected fixture name")
    existing = {path.name: path.read_bytes() for path in fixtures.glob(PREFIX + "*")}
    if args.check:
        if generated != existing:
            changed = sorted(name for name in generated.keys() | existing.keys() if generated.get(name) != existing.get(name))
            raise RuntimeError("item-container fixtures differ: " + ", ".join(changed))
    else:
        stale = existing.keys() - generated.keys()
        if stale:
            raise RuntimeError("unexpected existing item-container fixtures: " + ", ".join(sorted(stale)))
        fixtures.mkdir(parents=True, exist_ok=True)
        for name, contents in generated.items():
            (fixtures / name).write_bytes(contents)
require_pinned(jar)
print("Item-container fixtures " + ("checked" if args.check else "generated") + " fake-only, network-isolated, pinned native jar " + EXPECTED_SHA256)
