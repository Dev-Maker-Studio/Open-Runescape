import argparse
import gzip
import hashlib
import importlib.util
import json
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("generate_config_oracle", HERE / "generate_config_oracle.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

IDK_GROUP = 3


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
    source = HERE / "IdkOracle.java"
    subprocess.run(["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    cache = base.lw.Cache(args.cache)
    config = base.Index(cache, 2)
    ids = config.ids(IDK_GROUP)
    out = bytearray(struct.pack(">i", len(ids)))
    for file in ids:
        data = config.file(IDK_GROUP, file)
        out += struct.pack(">ii", file, len(data))
        out += data
    (work / "idk.bundle").write_bytes(bytes(out))
    output = work / "idk-oracle.tsv"
    result = subprocess.run(
        ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "IdkOracle", str(jar), str(work / "idk.bundle"), str(output)],
        check=True, capture_output=True, text=True,
    )
    tsv = output.read_bytes()
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "records": len(ids),
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
