import argparse
import gzip
import importlib.util
import json
import os
import shutil
import struct
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CRATE = HERE.parents[1]
CONFIG = CRATE.parent / "client-runtime" / "tests" / "native" / "generate_config_oracle.py"
spec = importlib.util.spec_from_file_location("generate_config_oracle", CONFIG)
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

MODEL_INDEX = 7
CONFIG_INDEX = 2
NPC_INDEX = 18
ITEM_INDEX = 19
DEFAULTS_INDEX = 28
IDK_GROUP = 3
BAS_GROUP = 32
IDK_PART_TABLE = [8, 11, 4, 6, 9, 7, 10, 0]
CUSTOM_ITEMS = 4000
APPEARANCES = 6000
HEAD_ONLY = 600
NPC_BAS_CASES = 3000
DEFAULT_JAR = CRATE.parent / "scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
FIXTURE = CRATE / "tests/fixtures/entity-oracle.tsv.gz"
DIGEST = CRATE / "tests/fixtures/entity-oracle.sha256"


class Lcg:
    def __init__(self, seed):
        self.state = seed

    def next(self, bound):
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
        return (self.state >> 33) % bound

    def pick(self, values):
        return values[self.next(len(values))]


def signed(value):
    value &= 0xFFFFFFFF
    return value - (1 << 32) if value >= 1 << 31 else value


def write_block(handle, entries, size):
    handle.write(struct.pack(">ii", len(entries), size))
    for key in sorted(entries):
        handle.write(struct.pack(">ii", key, len(entries[key])))
        handle.write(entries[key])


def collect(cache):
    config = base.Index(cache, CONFIG_INDEX)
    idks = {}
    for file in config.ids(IDK_GROUP):
        data = config.file(IDK_GROUP, file)
        if data is not None:
            idks[file] = data
    bases = {}
    for file in config.ids(BAS_GROUP):
        data = config.file(BAS_GROUP, file)
        if data is not None:
            bases[file] = data
    items = {}
    index = base.Index(cache, ITEM_INDEX)
    for archive in sorted(index.table):
        for file in index.table[archive]:
            data = index.file(archive, file)
            if data is not None:
                items[archive << 8 | file] = data
        index.loaded.pop(archive, None)
    npcs = {}
    index = base.Index(cache, NPC_INDEX)
    for archive in sorted(index.table):
        for file in index.table[archive]:
            data = index.file(archive, file)
            if data is not None:
                npcs[archive << 7 | file] = data
        index.loaded.pop(archive, None)
    defaults = base.Index(cache, DEFAULTS_INDEX)
    equipment = defaults.file(6, 0)
    if equipment is None:
        equipment = defaults.file(0, 6)
    if equipment is None:
        raise SystemExit("equipment defaults missing")
    return idks, bases, items, npcs, equipment


def write_bundle(cache, path):
    idks, bases, items, npcs, equipment = collect(cache)
    models = base.Index(cache, MODEL_INDEX)
    ids = sorted(models.table)
    count = 0
    with open(path, "wb") as handle:
        handle.write(struct.pack(">ii", 0, ids[-1] + 1))
        for model in ids:
            data = models.file(model, 0)
            models.loaded.pop(model, None)
            if data is not None:
                handle.write(struct.pack(">ii", model, len(data)))
                handle.write(data)
                count += 1
        end = handle.tell()
        handle.seek(0)
        handle.write(struct.pack(">i", count))
        handle.seek(end)
        write_block(handle, idks, max(idks) + 1)
        write_block(handle, bases, max(bases) + 1)
        write_block(handle, items, max(items) + 1)
        write_block(handle, npcs, max(npcs) + 1)
        handle.write(struct.pack(">i", len(equipment)))
        handle.write(equipment)
    return ids, sorted(idks), sorted(bases), sorted(items), sorted(npcs)


def execute(work, jar, name, cases, bwrap):
    source = HERE / "EntityOracle.java"
    classes = work / "classes"
    classes.mkdir(exist_ok=True)
    (work / "cases.txt").write_text("\n".join(cases) + "\n", encoding="ascii")
    sandbox = []
    if bwrap:
        sandbox = [
            bwrap, "--unshare-net", "--die-with-parent", "--new-session", "--ro-bind", "/", "/",
            "--tmpfs", "/tmp", "--bind", str(work), str(work), "--proc", "/proc", "--dev", "/dev",
            "--chdir", str(work), "--",
        ]
    subprocess.run(sandbox + ["javac", "-nowarn", "-Xlint:none", "-cp", str(jar), "-d", str(classes), str(source)], check=True)
    output = work / (name + ".tsv")
    result = subprocess.run(
        sandbox + ["java", "-Xmx3g", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                   "-Djava.awt.headless=true", "-cp", f"{classes}{os.pathsep}{jar}", "EntityOracle", str(jar),
                   str(work / "entity.bundle"), str(work / "cases.txt"), str(output)],
        check=True, capture_output=True, text=True,
    )
    lines = output.read_text(encoding="ascii").splitlines()
    return lines, result.stdout.strip(), source


def vsmart(value):
    if value < 32767:
        return struct.pack(">H", value)
    return struct.pack(">I", value | 0x80000000)


def customisation(rng, meta, models):
    equip = meta[:6]
    head = meta[6:10]
    recol = meta[13]
    retex = meta[14]
    flags = rng.next(16)
    out = bytearray([flags])
    if flags & 1:
        out += vsmart(rng.pick(models)) + vsmart(rng.pick(models))
        if equip[1] != -1 or equip[4] != -1:
            out += vsmart(rng.pick(models)) + vsmart(rng.pick(models))
        if equip[2] != -1 or equip[5] != -1:
            out += vsmart(rng.pick(models)) + vsmart(rng.pick(models))
    if flags & 2:
        out += vsmart(rng.pick(models)) + vsmart(rng.pick(models))
        if head[1] != -1 or head[3] != -1:
            out += vsmart(rng.pick(models)) + vsmart(rng.pick(models))
    if flags & 4:
        slots = []
        for _ in range(4):
            if recol > 0 and rng.next(8) != 0:
                slots.append(rng.next(min(recol, 15)) if rng.next(40) else 14)
            else:
                slots.append(15)
        out += struct.pack(">H", slots[0] | slots[1] << 4 | slots[2] << 8 | slots[3] << 12)
        for slot in slots:
            if slot != 15:
                out += struct.pack(">H", rng.next(65536))
    if flags & 8:
        slots = []
        for _ in range(2):
            if retex > 0 and rng.next(4) != 0:
                slots.append(rng.next(min(retex, 15)))
            else:
                slots.append(15)
        out += bytes([slots[0] | slots[1] << 4])
        for slot in slots:
            if slot != 15:
                out += struct.pack(">H", rng.next(65536))
    return out.hex()


def parse_meta(text):
    return [int(part) for part in text.split(",")]


def parse_defaults(text):
    parts = text.split("|")
    slots = parse_meta(parts[0])
    return slots[0], slots[1]


def second_run(rng, model_ids, idk_ids, bas_ids, npc_ids, meta, shield_slot, weapon_slot):
    cases = []
    equipping = [i for i in meta if any(v >= 0 for v in meta[i][:10])]
    for item in equipping:
        for female in (0, 1):
            cases.append("IB\t%d\t%d\t-" % (item, female))
            cases.append("IH\t%d\t%d\t-" % (item, female))
    for _ in range(CUSTOM_ITEMS):
        item = rng.pick(equipping)
        female = rng.next(2)
        cases.append("IB\t%d\t%d\t%s" % (item, female, customisation(rng, meta[item], model_ids)))
        item = rng.pick(equipping)
        cases.append("IH\t%d\t%d\t%s" % (item, rng.next(2), customisation(rng, meta[item], model_ids)))
    by_slot = {}
    for item in equipping:
        slot = meta[item][10]
        if 0 <= slot < 14:
            by_slot.setdefault(slot, []).append(item)
    slots = sorted(by_slot)
    for number in range(APPEARANCES):
        appearance = [0] * 15
        modifiers = []
        for slot in slots:
            if rng.next(10) < 6:
                item = rng.pick(by_slot[slot])
                appearance[slot] = 0x40000000 | item
                if rng.next(5) == 0:
                    modifiers.append("%d:%s" % (slot, customisation(rng, meta[item], model_ids)))
        for part in IDK_PART_TABLE:
            if appearance[part] == 0 and rng.next(10) < 8:
                appearance[part] = signed(0x80000000 | rng.pick(idk_ids))
        female = rng.next(2)
        bas = -1 if rng.next(3) == 0 else rng.pick(bas_ids)
        shield = -1
        weapon = -1
        pick = rng.next(4)
        if pick == 1:
            shield = 65535 if rng.next(2) else rng.pick(equipping)
        elif pick == 2:
            weapon = 65535 if rng.next(2) else rng.pick(equipping)
        elif pick == 3:
            shield = rng.pick(equipping)
            weapon = 65535 if rng.next(2) else rng.pick(equipping)
        recolor = ",".join(str(rng.next(40)) for _ in range(10))
        tail = "%d\t%d\t%s\t%s\t%d\t%d\t%s" % (
            female, bas, ",".join(map(str, appearance)), ";".join(modifiers) if modifiers else "-", shield, weapon, recolor)
        cases.append("PB\t" + tail)
        cases.append("PH\t" + tail)
        cases.append("PHASH\t" + tail)
    for _ in range(HEAD_ONLY):
        cases.append("PP\t%d,%d,%d" % (rng.pick(idk_ids), rng.pick(idk_ids), rng.pick(idk_ids)))
        cases.append("PP\t%d,%d" % (rng.pick(idk_ids), rng.pick(idk_ids)))
    for _ in range(NPC_BAS_CASES):
        cases.append("NB\t%d\t%d" % (rng.pick(npc_ids), rng.pick(bas_ids)))
    return cases


def run(args, work, jar):
    cache = base.lw.Cache(args.cache)
    model_ids, idk_ids, bas_ids, item_ids, npc_ids = write_bundle(cache, work / "entity.bundle")
    bwrap = shutil.which("bwrap")
    first = ["EQD\t-"]
    first += ["KB\t%d" % i for i in idk_ids]
    first += ["KH\t%d" % i for i in idk_ids]
    first += ["IM\t%d" % i for i in item_ids]
    first += ["NB\t%d\t-1" % i for i in npc_ids]
    first += ["NH\t%d" % i for i in npc_ids]
    lines1, out1, source = execute(work, jar, "entity-run1", first, bwrap)
    meta = {}
    shield_slot = weapon_slot = -1
    for line in lines1:
        fields = line.split("\t")
        if fields[0] == "IM" and fields[-1] not in ("error", "skip"):
            meta[int(fields[1])] = parse_meta(fields[2])
        elif fields[0] == "EQD":
            shield_slot, weapon_slot = parse_defaults(fields[-1])
    rng = Lcg(0x5EED0E17)
    second = second_run(rng, model_ids, idk_ids, bas_ids, npc_ids, meta, shield_slot, weapon_slot)
    lines2, out2, _ = execute(work, jar, "entity-run2", second, bwrap)
    tsv = ("\n".join(lines1 + lines2) + "\n").encode("ascii")
    return tsv, out1 + " | " + out2, len(model_ids), source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=DEFAULT_JAR)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".darkanrs/caches/darkan/LIVE")
    parser.add_argument("--work", type=Path, default=CRATE / "target/oracle-work")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    original = base.sha(args.jar.read_bytes())
    if original != base.PINNED:
        raise SystemExit("jar does not match the pinned oracle sha256")
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    if jar.exists():
        jar.chmod(0o644)
        jar.unlink()
    shutil.copyfile(args.jar, jar)
    jar.chmod(0o444)
    if base.sha(jar.read_bytes()) != base.PINNED:
        raise SystemExit("private jar copy differs from the pinned oracle")
    try:
        tsv, oracle_output, models, source = run(args, work, jar)
    finally:
        if base.sha(args.jar.read_bytes()) != base.PINNED or base.sha(jar.read_bytes()) != base.PINNED:
            raise SystemExit("oracle jar changed during the run")
    digest = base.sha(tsv)
    lines = tsv.decode("ascii").splitlines()
    kinds = {}
    for line in lines:
        kinds[line.split("\t")[0]] = kinds.get(line.split("\t")[0], 0) + 1
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.sha(source.read_bytes()),
        "modelsInBundle": models,
        "cases": len(lines),
        "casesByKind": kinds,
        "errorCases": sum(1 for line in lines if line.endswith("\terror")),
        "skipCases": sum(1 for line in lines if line.endswith("\tskip")),
        "tsvSha256": digest,
        "oracleOutput": oracle_output,
    }
    if args.check:
        stored = gzip.decompress(FIXTURE.read_bytes())
        if stored != tsv or DIGEST.read_text().strip() != digest:
            print(json.dumps(evidence, indent=2))
            raise SystemExit("entity oracle fixture differs; rerun without --check to regenerate")
        evidence["check"] = "match"
    else:
        FIXTURE.parent.mkdir(parents=True, exist_ok=True)
        with gzip.GzipFile(FIXTURE, "wb", compresslevel=9, mtime=0) as handle:
            handle.write(tsv)
        DIGEST.write_text(digest + "\n")
        evidence["fixtureBytes"] = FIXTURE.stat().st_size
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    sys.exit(main())
