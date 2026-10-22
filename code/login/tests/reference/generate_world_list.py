import hashlib
import pathlib
import shutil
import subprocess
import sys
import tempfile

root = pathlib.Path(__file__).resolve().parents[2]
jar = pathlib.Path(sys.argv[1]).resolve(strict=True)
source = root / "tests/reference/WorldListOracle.java"
fixtures = root / "tests/fixtures"
work = root / "target/world-list-native-oracle"
work.mkdir(parents=True, exist_ok=True)
fixtures.mkdir(parents=True, exist_ok=True)
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
        "WorldListOracle", str(output)
    ], check=True)
    if hashlib.sha256(snapshot.read_bytes()).hexdigest() != original_hash:
        raise RuntimeError("isolated native jar changed during oracle run")
    for fixture in output.iterdir():
        shutil.copyfile(fixture, fixtures / fixture.name)
if hashlib.sha256(jar.read_bytes()).hexdigest() != original_hash:
    raise RuntimeError("original native jar changed during oracle run")
(fixtures / "world-list-oracle.sha256").write_text(original_hash + "\n", encoding="ascii")
print("World-list fixtures generated fake-only with no network from isolated jar " + original_hash)
