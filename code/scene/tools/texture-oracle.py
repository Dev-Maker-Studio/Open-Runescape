#!/usr/bin/env python3
import argparse
import json
import shutil
import subprocess
from hashlib import sha256
from pathlib import Path

PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
ROOT = Path(__file__).resolve().parents[3]


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def private_copy(source, destination, expected):
    if digest(source) != expected:
        raise ValueError(f"unexpected SHA-256 for {source}")
    if not destination.exists():
        shutil.copyfile(source, destination)
    if digest(destination) != expected:
        raise ValueError(f"unexpected private copy SHA-256 for {destination}")
    destination.chmod(0o444)


def cache_digest(cache):
    names = ["main_file_cache.dat2", "main_file_cache.idx8", "main_file_cache.idx9", "main_file_cache.idx26", "main_file_cache.idx255"]
    return {name: digest(cache / name) for name in names}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=ROOT / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar")
    parser.add_argument("--cache", type=Path, default=ROOT / "assets/cache/.packed")
    parser.add_argument("--work", type=Path, default=ROOT / "code/harness/target/loop/iteration18-validation/texture")
    parser.add_argument("--fixture", type=Path, default=ROOT / "code/scene/tests/fixtures/texture_oracle.json")
    parser.add_argument("--sample-every", type=int, default=0)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    jar = args.work / "oracle-client.jar"
    private_copy(args.jar, jar, PINNED)
    before = cache_digest(args.cache)
    source = Path(__file__).resolve().parent / "TextureOracle.java"
    classes = args.work / "classes"
    classes.mkdir(exist_ok=True)
    subprocess.run(["javac", "-cp", str(jar), "-d", str(classes), str(source)], check=True)
    output = args.work / "texture-native.json"
    dumps = args.work / "dumps"
    log = subprocess.run(
        ["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
         f"-Dopenrs.cache={args.cache}", "-Dbot.enabled=false", "-cp", f"{classes}:{jar}", "TextureOracle", str(output), str(dumps), str(args.sample_every)],
        check=True, capture_output=True, text=True)
    (args.work / "oracle-stdout.txt").write_text(log.stdout)
    (args.work / "oracle-stderr.txt").write_text(log.stderr)
    after = cache_digest(args.cache)
    data = json.loads(output.read_text())
    args.fixture.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(output, args.fixture)
    evidence = {
        "phase": "iteration18",
        "jarSha256": PINNED,
        "jarCopySha256": digest(jar),
        "sourceSha256": digest(source),
        "fixtureSha256": digest(args.fixture),
        "cacheSha256": after,
        "cacheUnchanged": before == after,
        "jarUnchanged": digest(args.jar) == PINNED,
        "counts": data["counts"],
        "materialCount": data["materialCount"],
        "glDrawExecuted": False,
        "mipmapChain": "transcribed from OpenGlTexture.createMipmappedCubeTexture in TextureOracle.mipmapChain, not executed through GL",
        "hdrPath": "getPixelsHdr (hdr && bloomEnabled) not dumped; fixture pixels are the bloom-off upload",
        "stdout": log.stdout.strip(),
    }
    (args.work / "oracle-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(log.stdout.strip())
    print(f"fixture {args.fixture} ({args.fixture.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
