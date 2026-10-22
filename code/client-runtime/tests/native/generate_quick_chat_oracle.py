import argparse
import gzip
import importlib.util
import json
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("generate_config_oracle", HERE / "generate_config_oracle.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

INDEXES = (24, 25)


def source(cache, index):
    table = base.Index(cache, index)
    out = bytearray()
    counts = []
    for archive in (0, 1):
        ids = table.ids(archive)
        counts.append(max(ids) + 1 if ids else 0)
    out += struct.pack(">ii", *counts)
    records = 0
    for archive in (0, 1):
        ids = table.ids(archive)
        out += struct.pack(">i", len(ids))
        for file in ids:
            data = table.file(archive, file)
            out += struct.pack(">ii", file, len(data))
            out += data
            records += 1
    return bytes(out), records


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--fixture", type=Path)
    args = parser.parse_args()
    work = args.work
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    base.private_copy(args.jar, jar)
    java = HERE / "QuickChatOracle.java"
    subprocess.run(["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(work), str(java)], check=True)
    cache = base.lw.Cache(args.cache)
    bundle = bytearray()
    records = 0
    for index in INDEXES:
        data, count = source(cache, index)
        bundle += data
        records += count
    (work / "quick-chat.bundle").write_bytes(bytes(bundle))
    output = work / "quick-chat-oracle.tsv"
    result = subprocess.run(
        ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow", "-cp", f"{work}:{jar}", "QuickChatOracle", str(jar), str(work / "quick-chat.bundle"), str(output)],
        check=True, capture_output=True, text=True,
    )
    tsv = output.read_bytes()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(java.read_bytes()),
        "records": records,
        "tsvSha256": base.sha(tsv),
        "tsvBytes": len(tsv),
        "oracleOutput": result.stdout.strip(),
    }
    if args.fixture:
        args.fixture.parent.mkdir(parents=True, exist_ok=True)
        with gzip.GzipFile(args.fixture, "wb", compresslevel=9, mtime=0) as handle:
            handle.write(tsv)
        evidence["fixtureBytes"] = args.fixture.stat().st_size
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    main()
