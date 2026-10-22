import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import tempfile


EXPECTED_JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def main():
    root = pathlib.Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description="Generate isolated native floor definition and JS5 fixtures")
    parser.add_argument("jar", nargs="?", type=pathlib.Path,
                        default=root.parent / "client/build/libs/darkan-client-shaded-2.0.0-all.jar")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    source = pathlib.Path(__file__).resolve().with_name("FloorFixtures.java")
    common = source.with_name("AssetChainFixtures.java")
    generator = pathlib.Path(__file__).resolve()
    fixture_dir = root / "tests/fixtures"
    original_hash = sha256(jar)
    if original_hash != EXPECTED_JAR_SHA256:
        raise RuntimeError("existing native jar does not match the pinned oracle SHA256")
    sources = {path: sha256(path) for path in (source, common, generator)}
    tools = {}
    for name in ("bwrap", "java", "javac"):
        location = shutil.which(name)
        if location is None:
            raise RuntimeError(f"required tool unavailable: {name}")
        tools[name] = str(pathlib.Path(location).resolve(strict=True))
    environment = {"PATH": os.defpath, "LANG": "C.UTF-8", "LC_ALL": "C.UTF-8"}
    try:
        with tempfile.TemporaryDirectory(prefix="floor-fixtures-") as temporary:
            isolated = pathlib.Path(temporary).resolve()
            snapshot = isolated / "native.jar"
            home = isolated / "home"
            classes = isolated / "classes"
            output = isolated / "output"
            for directory in (home, classes, output):
                directory.mkdir()
            shutil.copyfile(jar, snapshot)
            for path in (source, common):
                shutil.copyfile(path, isolated / path.name)
                if sha256(isolated / path.name) != sources[path]:
                    raise RuntimeError("native helper changed during isolation")
            if sha256(snapshot) != original_hash:
                raise RuntimeError("native jar changed during isolation")
            sandbox = [
                tools["bwrap"], "--unshare-net", "--die-with-parent", "--new-session",
                "--ro-bind", "/", "/", "--tmpfs", "/tmp", "--bind", str(isolated), str(isolated),
                "--proc", "/proc", "--dev", "/dev", "--chdir", str(isolated), "--clearenv",
                "--setenv", "PATH", os.defpath, "--setenv", "LANG", "C.UTF-8",
                "--setenv", "LC_ALL", "C.UTF-8", "--setenv", "HOME", str(home), "--",
            ]
            compile_result = subprocess.run(sandbox + [
                tools["javac"], "-J-XX:-UsePerfData", "-classpath", str(snapshot),
                "-d", str(classes), str(isolated / common.name), str(isolated / source.name),
            ], check=True, env=environment, capture_output=True, text=True, timeout=90)
            if compile_result.stdout or compile_result.stderr:
                raise RuntimeError("unexpected native helper compiler output: " + compile_result.stdout + compile_result.stderr)
            result = subprocess.run(sandbox + [
                tools["java"], "-XX:-UsePerfData", "-Djava.awt.headless=true", f"-Duser.home={home}",
                "-Dbot.enabled=false", "-cp", f"{snapshot}{os.pathsep}{classes}",
                "FloorFixtures", str(output),
            ], check=True, env=environment, capture_output=True, text=True, timeout=60)
            if result.stderr:
                raise RuntimeError("unexpected native helper stderr: " + result.stderr)
            if sha256(snapshot) != original_hash:
                raise RuntimeError("isolated native jar changed during oracle run")
            for path in (source, common):
                if sha256(isolated / path.name) != sources[path]:
                    raise RuntimeError("isolated native helper changed during oracle run")
            if any(home.iterdir()):
                raise RuntimeError("pure floor oracle unexpectedly wrote home files")
            report = json.loads(result.stdout)
            wire = (output / "floors-native.wire").read_bytes()
    finally:
        if sha256(jar) != original_hash:
            raise RuntimeError("original native jar changed during oracle run")
        if any(sha256(path) != digest for path, digest in sources.items()):
            raise RuntimeError("oracle source changed during run")
    provenance = {
        "source": "Existing native jar; synthetic sparse index2 underlay archive1 and overlay archive4",
        "jar": jar.name,
        "jarSha256": original_hash,
        "helperSha256": sources[source],
        "commonHelperSha256": sources[common],
        "generatorSha256": sources[generator],
        "nativeCalls": ["Js5MasterIndex", "Js5ResourceProvider.getArchiveFileCount", "Js5ResourceProvider.getFile",
                        "FluTypeList.list", "FloTypeList.list", "Js5Compression.decompress", "Whirlpool.calculateDigest"],
        "isolation": "bwrap --unshare-net; read-only host root; copied pinned jar; empty temporary home; cleared environment; headless; bot disabled",
        "scope": "Pure reference/archive/definition loading and synthetic JS5 only; no native startup, real cache, network, account or display",
        "opcode8": "Native trace records lazy cache misses; eager fixture decode does not claim equivalent host loading order",
    }
    contents = json.dumps({"provenance": provenance, **report}, indent=2, ensure_ascii=True) + "\n"
    files = {fixture_dir / "floors-native.json": contents.encode("utf-8"),
             fixture_dir / "floors-native.wire": wire}
    for path, data in files.items():
        if args.check:
            if not path.is_file() or path.read_bytes() != data:
                raise SystemExit("Floor fixture differs; rerun without --check to regenerate")
        else:
            path.write_bytes(data)
    print("Native floor fixtures " + ("match" if args.check else "generated") + "; jar SHA256 " + original_hash)


if __name__ == "__main__":
    main()
