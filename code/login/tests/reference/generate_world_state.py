import argparse
import hashlib
import pathlib
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument("jar", type=pathlib.Path)
parser.add_argument("--check", action="store_true")
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[2]
jar = args.jar.resolve(strict=True)
source = root / "tests/reference/WorldStateOracle.java"
fixtures = root / "tests/fixtures"
work = root / "target/world-state-native-oracle"
work.mkdir(parents=True, exist_ok=True)
original_hash = hashlib.sha256(jar.read_bytes()).hexdigest()
with tempfile.TemporaryDirectory(dir=work) as temporary:
    isolated = pathlib.Path(temporary)
    snapshot = isolated / "native.jar"
    output = isolated / "fixtures"
    output.mkdir()
    shutil.copyfile(jar, snapshot)
    if hashlib.sha256(snapshot.read_bytes()).hexdigest() != original_hash:
        raise RuntimeError("native jar changed during isolation")
    subprocess.run([
        "javac", "-encoding", "UTF-8", "-cp", str(snapshot), "-d", str(isolated), str(source)
    ], check=True)
    subprocess.run([
        "bwrap", "--unshare-net", "--die-with-parent", "--new-session",
        "--ro-bind", "/", "/", "--bind", str(isolated), str(isolated),
        "--tmpfs", "/tmp", "--proc", "/proc", "--dev", "/dev", "--",
        "java", "-Djava.awt.headless=true", f"-Duser.home={isolated}",
        "-Dbot.enabled=false", "-cp", f"{snapshot}:{isolated}",
        "WorldStateOracle", str(output)
    ], check=True)
    if hashlib.sha256(snapshot.read_bytes()).hexdigest() != original_hash:
        raise RuntimeError("isolated native jar changed during oracle run")
    (output / "world-state-oracle.sha256").write_text(original_hash + "\n", encoding="ascii")
    generated = {path.name: path.read_bytes() for path in output.iterdir()}
    if any(not name.startswith("world-state-") for name in generated):
        raise RuntimeError("oracle wrote an unexpected fixture name")
    if args.check:
        existing = {path.name: path.read_bytes() for path in fixtures.glob("world-state-*")}
        if generated != existing:
            changed = sorted(name for name in generated.keys() | existing.keys() if generated.get(name) != existing.get(name))
            raise RuntimeError("world-state fixtures differ: " + ", ".join(changed))
    else:
        fixtures.mkdir(parents=True, exist_ok=True)
        stale = set(path.name for path in fixtures.glob("world-state-*")) - generated.keys()
        if stale:
            raise RuntimeError("unexpected existing world-state fixtures: " + ", ".join(sorted(stale)))
        for name, contents in generated.items():
            (fixtures / name).write_bytes(contents)
if hashlib.sha256(jar.read_bytes()).hexdigest() != original_hash:
    raise RuntimeError("original native jar changed during oracle run")
print("World-state fixtures " + ("checked" if args.check else "generated") + " fake-only, network-isolated, native jar " + original_hash)
