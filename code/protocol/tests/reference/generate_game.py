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
fixtures = root / "tests/fixtures"
work = root / "target/game-oracle"
work.mkdir(parents=True, exist_ok=True)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


original_hash = digest(jar)
with tempfile.TemporaryDirectory(dir=work) as temporary:
    isolated = pathlib.Path(temporary)
    snapshot = isolated / "native.jar"
    shutil.copyfile(jar, snapshot)
    if digest(snapshot) != original_hash or digest(jar) != original_hash:
        raise RuntimeError("native jar changed during isolation")
    source = root / "tests/reference/GamePacketOracle.java"
    subprocess.run(
        ["javac", "-encoding", "UTF-8", "-cp", str(snapshot), "-d", str(isolated), str(source)],
        check=True,
    )
    output = isolated / "fixtures"
    subprocess.run(
        [
            "java", "-Djava.awt.headless=true", f"-Duser.home={isolated}",
            "-cp", f"{snapshot}:{isolated}", "GamePacketOracle", str(output),
        ],
        check=True,
    )
    if digest(snapshot) != original_hash or digest(jar) != original_hash:
        raise RuntimeError("native jar changed during oracle run")
    files = sorted(output.iterdir())
    manifest = f"{original_hash}  native.jar\n"
    manifest += "".join(f"{digest(path)}  {path.name}\n" for path in files)
    (output / "game-oracle.sha256").write_text(manifest, encoding="ascii")
    for path in sorted(output.iterdir()):
        destination = fixtures / path.name
        if args.check:
            if not destination.is_file() or path.read_bytes() != destination.read_bytes():
                raise RuntimeError(f"native game fixture drift: {path.name}")
        else:
            shutil.copyfile(path, destination)
print(f"Native game packet fixtures {'verified' if args.check else 'generated'} from isolated jar {original_hash}")
