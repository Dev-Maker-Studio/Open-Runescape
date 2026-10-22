import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import tempfile


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def main():
    root = pathlib.Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description="Generate pure native bootstrap defaults from an existing jar")
    parser.add_argument(
        "jar", nargs="?", type=pathlib.Path,
        default=root.parent / "client/build/libs/darkan-client-shaded-2.0.0-all.jar",
    )
    parser.add_argument("--check", action="store_true", help="compare without writing the fixture")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    source = pathlib.Path(__file__).resolve().with_name("BootstrapDefaults.java")
    generator = pathlib.Path(__file__).resolve()
    fixture = root / "tests/fixtures/bootstrap-defaults.json"
    original_hash = sha256(jar)
    helper_hash = sha256(source)
    generator_hash = sha256(generator)
    tools = {}
    for name in ("bwrap", "javac", "java"):
        location = shutil.which(name)
        if location is None:
            raise RuntimeError(f"required tool unavailable: {name}")
        tools[name] = str(pathlib.Path(location).resolve(strict=True))
    environment = {"PATH": os.defpath, "LANG": "C.UTF-8", "LC_ALL": "C.UTF-8"}
    try:
        with tempfile.TemporaryDirectory(prefix="bootstrap-defaults-") as temporary:
            isolated = pathlib.Path(temporary).resolve()
            snapshot = isolated / "native.jar"
            helper = isolated / source.name
            classes = isolated / "classes"
            home = isolated / "home"
            classes.mkdir()
            home.mkdir()
            shutil.copyfile(jar, snapshot)
            shutil.copyfile(source, helper)
            if sha256(snapshot) != original_hash or sha256(helper) != helper_hash:
                raise RuntimeError("native jar or helper changed during isolation")
            sandbox = [
                tools["bwrap"], "--unshare-net", "--die-with-parent", "--new-session",
                "--ro-bind", "/", "/", "--tmpfs", "/tmp",
                "--bind", str(isolated), str(isolated), "--proc", "/proc", "--dev", "/dev",
                "--chdir", str(isolated), "--clearenv",
                "--setenv", "PATH", os.defpath, "--setenv", "LANG", "C.UTF-8",
                "--setenv", "LC_ALL", "C.UTF-8", "--setenv", "HOME", str(home), "--",
            ]
            subprocess.run(sandbox + [
                tools["javac"], "-J-XX:-UsePerfData", "-encoding", "UTF-8", "-proc:none",
                "-cp", str(snapshot), "-d", str(classes), str(helper),
            ], check=True, env=environment, timeout=60)
            result = subprocess.run(sandbox + [
                tools["java"], "-XX:-UsePerfData", "-Djava.awt.headless=true",
                f"-Duser.home={home}", "-Dbot.enabled=false",
                "-cp", f"{snapshot}{os.pathsep}{classes}", "BootstrapDefaults",
            ], check=True, env=environment, capture_output=True, text=True, timeout=60)
            if result.stderr:
                raise RuntimeError("unexpected native helper stderr: " + result.stderr)
            if sha256(snapshot) != original_hash or sha256(helper) != helper_hash:
                raise RuntimeError("isolated native jar or helper changed during oracle run")
            if any(home.iterdir()):
                raise RuntimeError("pure bootstrap oracle unexpectedly wrote home files")
            report = json.loads(result.stdout)
    finally:
        if sha256(jar) != original_hash:
            raise RuntimeError("original native jar changed during oracle run")
        if sha256(source) != helper_hash or sha256(generator) != generator_hash:
            raise RuntimeError("oracle source changed during run")
    provenance = {
        "source": "Existing native jar; fresh GamePreferences(Game.RUNESCAPE), no persisted preferences",
        "jar": jar.name,
        "jarSha256": original_hash,
        "helperSha256": helper_hash,
        "generatorSha256": generator_hash,
        "nativeCalls": [
            "GamePreferences(Game.RUNESCAPE)", "GamePreferences.encode()",
            "GamePreferences(Packet, Game.RUNESCAPE)", "Loader.loadParams()",
        ],
        "isolation": "bwrap --unshare-net; read-only host root; copied jar; empty temporary home; cleared environment",
        "hardwareInputs": ["GameShell.maxMemory", "GameShell.availableProcessors", "SystemInfo.OS_ARCH"],
        "loaderParameterIds": {
            "affiliateP": "33", "clientKey": "13", "JS5token": "17",
            "sessionToken": "27", "game": "8", "language": "31",
        },
        "loaderScope": "Source-public Loader parameters only; offline.invalid is an unused synthetic host",
    }
    contents = json.dumps({"provenance": provenance, **report}, indent=2, ensure_ascii=True) + "\n"
    if args.check:
        if not fixture.is_file() or fixture.read_bytes() != contents.encode("utf-8"):
            raise SystemExit("Bootstrap defaults fixture differs; rerun without --check to regenerate")
        print("Native bootstrap defaults match fixture; jar SHA256 " + original_hash)
    else:
        fixture.write_text(contents, encoding="utf-8")
        print("Native bootstrap defaults generated; jar SHA256 " + original_hash)


if __name__ == "__main__":
    main()
