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
    parser = argparse.ArgumentParser(description="Generate isolated native variable packet and provider fixtures")
    parser.add_argument(
        "jar", nargs="?", type=pathlib.Path,
        default=root.parent / "client/build/libs/darkan-client-shaded-2.0.0-all.jar",
    )
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    source = pathlib.Path(__file__).resolve().with_name("VariableFixtures.kt")
    generator = pathlib.Path(__file__).resolve()
    fixture = root / "tests/fixtures/variables-native.json"
    original_hash = sha256(jar)
    helper_hash = sha256(source)
    generator_hash = sha256(generator)
    tools = {}
    for name in ("bwrap", "java"):
        location = shutil.which(name)
        if location is None:
            raise RuntimeError(f"required tool unavailable: {name}")
        tools[name] = str(pathlib.Path(location).resolve(strict=True))
    environment = {"PATH": os.defpath, "LANG": "C.UTF-8", "LC_ALL": "C.UTF-8"}
    try:
        with tempfile.TemporaryDirectory(prefix="variable-fixtures-") as temporary:
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
            compile_result = subprocess.run(sandbox + [
                tools["java"], "-XX:-UsePerfData", "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow", "-Djava.awt.headless=true", f"-Duser.home={home}",
                "-cp", str(snapshot), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                "-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", str(snapshot),
                "-d", str(classes), str(helper),
            ], check=True, env=environment, capture_output=True, text=True, timeout=90)
            if compile_result.stdout or compile_result.stderr:
                raise RuntimeError("unexpected native helper compiler output: " + compile_result.stdout + compile_result.stderr)
            result = subprocess.run(sandbox + [
                tools["java"], "-XX:-UsePerfData", "-Djava.awt.headless=true",
                f"-Duser.home={home}", "-Dbot.enabled=false",
                "-cp", f"{snapshot}{os.pathsep}{classes}", "VariableFixturesKt",
            ], check=True, env=environment, capture_output=True, text=True, timeout=60)
            if result.stderr:
                raise RuntimeError("unexpected native helper stderr: " + result.stderr)
            if sha256(snapshot) != original_hash or sha256(helper) != helper_hash:
                raise RuntimeError("isolated native jar or helper changed during oracle run")
            if any(home.iterdir()):
                raise RuntimeError("pure variable oracle unexpectedly wrote home files")
            report = json.loads(result.stdout)
    finally:
        if sha256(jar) != original_hash:
            raise RuntimeError("original native jar changed during oracle run")
        if sha256(source) != helper_hash or sha256(generator) != generator_hash:
            raise RuntimeError("oracle source changed during run")
    provenance = {
        "source": "Existing native jar; synthetic definition span and exact isolated provider calls",
        "jar": jar.name,
        "jarSha256": original_hash,
        "helperSha256": helper_hash,
        "generatorSha256": generator_hash,
        "nativeCalls": [
            "ServerProt.opcode/size", "Packet.readByte", "Packet.readUShortAddLittle",
            "Packet.readUIntInverseMiddle", "PlayerVarDomain.setVarValueFromServer",
            "PlayerVarDomain.setVarValue", "PlayerVarDomain.getVarValue",
            "PlayerVarDomain.service", "PlayerVarDomain.reset",
        ],
        "syntheticInputs": [
            "VarpTypeList(null).size set reflectively before domain construction; no cache",
            "Before each native time query: Utils.lastSystemTime=2^60 and Utils.timeOffset=desiredTime-2^60; native rollback compensation returns exact desiredTime",
            "Only the helper is compiled with the existing jar's embedded Kotlin compiler; native jar is never rebuilt",
        ],
        "isolation": "bwrap --unshare-net; read-only host root; copied jar; empty temporary home; cleared environment; headless; bot disabled",
        "scope": "VARP_SMALL/VARP_LARGE payload readers plus provider only; no live dispatcher, host CS2, varbit, varc, persistence or gameplay",
    }
    contents = json.dumps({"provenance": provenance, **report}, indent=2, ensure_ascii=True) + "\n"
    if args.check:
        if not fixture.is_file() or fixture.read_bytes() != contents.encode("utf-8"):
            raise SystemExit("Variable fixture differs; rerun without --check to regenerate")
        print("Native variable fixtures match; jar SHA256 " + original_hash)
    else:
        fixture.write_text(contents, encoding="utf-8")
        print("Native variable fixtures generated; jar SHA256 " + original_hash)


if __name__ == "__main__":
    main()
