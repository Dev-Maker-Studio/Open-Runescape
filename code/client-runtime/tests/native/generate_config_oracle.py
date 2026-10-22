import argparse
import gzip
import hashlib
import importlib.util
import json
import shutil
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parents[2] / "scene" / "tools"
PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
PARAM, ITEM, NPC, SEQ, STRUCT, CONTAINER, INTERACTION = 1, 2, 3, 4, 5, 6, 7


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, TOOLS / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


lw = load("lighting_oracle", "ground-lighting-world-oracle.py")
lo = load("loc_oracle", "loc-oracle.py")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def private_copy(source, destination):
    if sha(source.read_bytes()) != PINNED:
        raise ValueError(f"unexpected SHA-256 for {source}")
    if not destination.exists():
        shutil.copyfile(source, destination)
    if sha(destination.read_bytes()) != PINNED:
        raise ValueError("unexpected private copy SHA-256")
    destination.chmod(0o444)


class Index:
    def __init__(self, cache, index):
        self.cache = cache
        self.index = index
        self.table = lo.reference_table(lw.decode(cache.container(255, index)))
        self.loaded = {}

    def file(self, archive, file):
        if archive not in self.table or file not in self.table[archive]:
            return None
        if archive not in self.loaded:
            container = lo.trim(self.cache.container(self.index, archive))
            self.loaded[archive] = lo.split(lw.decode(container), self.table[archive])
        return self.loaded[archive][file]

    def ids(self, archive):
        return list(self.table.get(archive, []))


def bundle(cache):
    config = Index(cache, 2)
    npc = Index(cache, 18)
    item = Index(cache, 19)
    seq = Index(cache, 20)
    defaults = Index(cache, 28)
    entries = []
    for file in config.ids(11):
        entries.append((PARAM, file, config.file(11, file)))
    for archive in sorted(item.table):
        for file in item.table[archive]:
            entries.append((ITEM, archive << 8 | file, item.file(archive, file)))
    for archive in sorted(npc.table):
        for file in npc.table[archive]:
            entries.append((NPC, archive << 7 | file, npc.file(archive, file)))
    for archive in sorted(seq.table):
        for file in seq.table[archive]:
            entries.append((SEQ, archive << 7 | file, seq.file(archive, file)))
    for file in config.ids(26):
        entries.append((STRUCT, file, config.file(26, file)))
    for file in config.ids(5):
        entries.append((CONTAINER, file, config.file(5, file)))
    entries.append((INTERACTION, 0, defaults.file(7, 0)))
    out = bytearray(struct.pack(">i", len(entries)))
    for kind, ident, data in entries:
        out += struct.pack(">iii", kind, ident, len(data) if data is not None else -1)
        if data is not None:
            out += data
    return bytes(out), len(entries)


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
    private_copy(args.jar, jar)
    source = HERE / "ConfigOracle.java"
    subprocess.run(["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    cache = lw.Cache(args.cache)
    data, count = bundle(cache)
    (work / "config.bundle").write_bytes(data)
    output = work / "config-oracle.tsv"
    result = subprocess.run(
        ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "ConfigOracle", str(jar), str(work / "config.bundle"), str(output)],
        check=True, capture_output=True, text=True,
    )
    tsv = output.read_bytes()
    evidence = {
        "jarSha256": PINNED,
        "javaSourceSha256": sha(source.read_bytes()),
        "records": count,
        "tsvSha256": sha(tsv),
        "tsvBytes": len(tsv),
        "oracleOutput": result.stdout.strip(),
    }
    (work / "config-oracle-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    if args.fixture:
        args.fixture.parent.mkdir(parents=True, exist_ok=True)
        with gzip.GzipFile(args.fixture, "wb", compresslevel=9, mtime=0) as handle:
            handle.write(tsv)
        evidence["fixtureBytes"] = args.fixture.stat().st_size
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    main()
