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
    parser = argparse.ArgumentParser(description="Generate isolated native varbit definition and sparse loader fixtures")
    parser.add_argument(
        "jar", nargs="?", type=pathlib.Path,
        default=root.parent / "client/build/libs/darkan-client-shaded-2.0.0-all.jar",
    )
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    source = pathlib.Path(__file__).resolve().with_name("VarbitFixtures.kt")
    generator = pathlib.Path(__file__).resolve()
    fixture = root / "tests/fixtures/varbits-native.json"
    original_hash = sha256(jar)
    if original_hash != EXPECTED_JAR_SHA256:
        raise RuntimeError("shared native jar does not match the pinned oracle SHA256")
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
        with tempfile.TemporaryDirectory(prefix="varbit-fixtures-") as temporary:
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
                "-cp", f"{snapshot}{os.pathsep}{classes}", "VarbitFixturesKt",
            ], check=True, env=environment, capture_output=True, text=True, timeout=60)
            if result.stderr:
                raise RuntimeError("unexpected native helper stderr: " + result.stderr)
            if sha256(snapshot) != original_hash or sha256(helper) != helper_hash:
                raise RuntimeError("isolated native jar or helper changed during oracle run")
            if any(home.iterdir()):
                raise RuntimeError("pure varbit oracle unexpectedly wrote home files")
            report = json.loads(result.stdout)
    finally:
        if sha256(jar) != original_hash:
            raise RuntimeError("original native jar changed during oracle run")
        if sha256(source) != helper_hash or sha256(generator) != generator_hash:
            raise RuntimeError("oracle source changed during run")
    provenance = {
        "source": "Existing native jar; synthetic sparse index22 archives and native binary definitions",
        "jar": jar.name,
        "jarSha256": original_hash,
        "helperSha256": helper_hash,
        "generatorSha256": generator_hash,
        "nativeCalls": [
            "IndexReference.INDEX_22.indexId", "SharedConfigsType.VARPBIT.archiveId/archiveFileId",
            "VarpbitType.decode", "VarpbitTypeList.list", "Js5Compression.decompress",
            "Js5MasterIndex", "Js5ResourceProvider.getFile", "Whirlpool.calculateDigest",
            "PlayerVarDomain.getVarBitValue", "PlayerVarDomain.setVarBitValue",
        ],
        "isolation": "bwrap --unshare-net; read-only host root; copied pinned jar; empty temporary home; cleared environment; headless; bot disabled",
        "scope": "Pure definition/loader/provider calls only; no native startup, real cache fetch, client launch, CS2 dispatcher, network, account or display",
        "absentPolicy": "Native loader defaults even for unavailable archives; independent repository only defaults on verified reference absence",
        "strictPolicy": "Unknown opcodes, trailing data, invalid bit spans, missing terminators and oversized definitions fail closed despite native permissiveness",
        "sampleValues": "Varbit0=(318,0,0), varbit1031=(568,10,12) match unpacked cache samples; other definitions synthetic",
    }
    contents = json.dumps({"provenance": provenance, **report}, indent=2, ensure_ascii=True) + "\n"
    if args.check:
        if not fixture.is_file() or fixture.read_bytes() != contents.encode("utf-8"):
            raise SystemExit("Varbit fixture differs; rerun without --check to regenerate")
        print("Native varbit fixtures match; jar SHA256 " + original_hash)
    else:
        fixture.write_text(contents, encoding="utf-8")
        print("Native varbit fixtures generated; jar SHA256 " + original_hash)


if __name__ == "__main__":
    main()
