import hashlib
import pathlib
import shutil
import subprocess
import sys
import tempfile

root = pathlib.Path(__file__).resolve().parents[2]
jar = pathlib.Path(sys.argv[1]).resolve(strict=True)
source = root / "tests/reference/LoginOracle.java"
fixtures = root / "tests/fixtures"
work = root / "target/native-oracle"
work.mkdir(parents=True, exist_ok=True)
fixtures.mkdir(parents=True, exist_ok=True)
original_hash = hashlib.sha256(jar.read_bytes()).hexdigest()
with tempfile.TemporaryDirectory(dir=work) as temporary:
    isolated = pathlib.Path(temporary)
    snapshot = isolated / "native.jar"
    shutil.copyfile(jar, snapshot)
    snapshot_hash = hashlib.sha256(snapshot.read_bytes()).hexdigest()
    if snapshot_hash != original_hash or hashlib.sha256(jar.read_bytes()).hexdigest() != original_hash:
        raise RuntimeError("native jar changed during isolation")
    subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(snapshot), "-d", str(isolated), str(source)], check=True)
    subprocess.run(["java", "-Djava.awt.headless=true", f"-Duser.home={isolated}", "-cp", f"{snapshot}:{isolated}", "LoginOracle", str(fixtures)], check=True)
    if hashlib.sha256(snapshot.read_bytes()).hexdigest() != original_hash:
        raise RuntimeError("isolated native jar changed during oracle run")
(fixtures / "oracle.sha256").write_text(original_hash + "\n", encoding="ascii")
print("Native packet fixtures generated from isolated jar " + original_hash)
