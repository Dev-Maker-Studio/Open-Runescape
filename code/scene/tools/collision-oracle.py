import argparse
import importlib.util
import json
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIXTURES = HERE.parent / "tests" / "fixtures"


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


lw = load("lighting_oracle", "ground-lighting-world-oracle.py")
lo = load("loc_oracle", "loc-oracle.py")
base = lw.base

SCENES = [
    ("S1", 104, 104, 132 * 64 - 20, 115 * 64 - 20, [(132, 115, "r")]),
    ("S2", 104, 104, 86 * 64 + 30, 95 * 64 + 30, [(86, 95, "r"), (87, 95, "r"), (86, 96, "r"), (87, 96, "r")]),
    ("S3", 104, 104, 132 * 64 + 20, 114 * 64 + 10, [(132, 114, "r"), (133, 114, "r"), (132, 115, "r"), (133, 115, "r")]),
    ("S4", 88, 120, 140 * 64 - 5, 104 * 64 - 30, [(140, 104, "r")]),
    ("S5", 104, 104, 132 * 64 - 20, 115 * 64 - 20, [(132, 115, "s1")]),
    ("S6", 104, 104, 86 * 64 + 30, 95 * 64 + 30, [(86, 95, "s2"), (87, 95, "s3"), (86, 96, "s4"), (87, 96, "s5")]),
    ("S7", 88, 120, 133 * 64 - 5, 114 * 64 - 30, [(133, 114, "s6")]),
]

IDS_PER_REGION = 220
PLACEMENTS_PER_REGION = 1400


def usmart(value):
    if value < 128:
        return bytes([value])
    return (value + 32768).to_bytes(2, "big")


def synthetic_locs(seed, ids):
    import random
    rng = random.Random(seed)
    chosen = sorted(rng.sample(ids, IDS_PER_REGION))
    rows = {}
    for _ in range(PLACEMENTS_PER_REGION):
        loc_id = rng.choice(chosen)
        packed = (rng.randrange(4) << 12) | (rng.randrange(64) << 6) | rng.randrange(64)
        rows.setdefault(loc_id, []).append((packed, (rng.randrange(23) << 2) | rng.randrange(4)))
    out = bytearray()
    last = -1
    for loc_id in sorted(rows):
        out += usmart(loc_id - last)
        last = loc_id
        previous = 0
        for packed, description in sorted(rows[loc_id]):
            out += usmart(packed - previous + 1)
            previous = packed
            out.append(description)
        out += usmart(0)
    out += usmart(0)
    return bytes(out)


OUTPUTS = {
    "native-routes.tsv": "coll-routes.tsv",
    "native-reach.tsv": "coll-reach.tsv",
    "native-movement.tsv": "coll-movement.tsv",
    "native-synth.txt": "coll-synth.txt",
    "native-synth.flags": "coll-synth.flags",
}


def scene_lines():
    lines = []
    regions = {}
    for name, size_x, size_y, base_x, base_y, entries in SCENES:
        parts = []
        for rx, ry, tag in entries:
            region = (rx << 8) | ry
            lkey = region if tag == "r" else 100000 + int(tag[1:]) * 1000 + region % 1000
            parts.append(f"{region}:{lkey}:{rx * 64 - base_x}:{ry * 64 - base_y}")
            regions[(rx, ry, tag)] = (region, lkey)
        lines.append(f"{name} {size_x} {size_y} {base_x} {base_y} {','.join(parts)}")
    return lines, regions


def run_java(jar, work, mode):
    command = ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "CollisionOracle", str(jar), str(work), mode]
    return subprocess.run(command, check=True, capture_output=True, text=True)


def generate(jar_source, cache_path, work):
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    base.private_copy(jar_source, jar, base.PINNED)
    source = HERE / "CollisionOracle.java"
    subprocess.run(["javac", "-nowarn", "-cp", str(jar), "-d", str(work), str(source)], check=True, capture_output=True, text=True)
    cache = lw.Cache(cache_path)
    names = lw.named_archives(cache)
    lines, regions = scene_lines()
    (work / "scenes.txt").write_text("\n".join(lines) + "\n")
    payloads = {}
    table = lo.reference_table(lw.decode(cache.container(255, 16)))
    all_ids = [(a << 8) | f for a in sorted(table) for f in table[a]]
    for (rx, ry, tag), (region, lkey) in sorted(regions.items()):
        maps = names.get(lw.name_hash(f"m{rx}_{ry}"))
        if maps is not None:
            data = lw.decode(cache.container(5, maps))
            (work / f"coll-m{region}.bin").write_bytes(data)
            payloads[f"m{region}"] = data
        if tag == "r":
            locs = names.get(lw.name_hash(f"l{rx}_{ry}"))
            if locs is None:
                continue
            data = lw.decode(cache.container(5, locs))
        else:
            data = synthetic_locs(lkey, all_ids)
        (work / f"coll-l{lkey}.bin").write_bytes(data)
        payloads[f"l{lkey}"] = data
    (work / "defs.bundle").unlink(missing_ok=True)
    first = run_java(jar, work, "ids")
    used = [int(v) for v in (work / "used-ids.txt").read_text().split()]
    cached = {}

    def definition(loc_id):
        archive = loc_id >> 8
        if archive not in table or (loc_id & 255) not in table[archive]:
            return None
        if archive not in cached:
            cached[archive] = lo.split(lw.decode(lo.trim(cache.container(16, archive))), table[archive])
        return cached[archive][loc_id & 255]

    entries = {loc_id: definition(loc_id) for loc_id in used}
    (work / "defs.bundle").write_bytes(lo.bundle_bytes(entries))
    second = run_java(jar, work, "full")
    if base.digest(jar) != base.PINNED or base.digest(jar_source) != base.PINNED:
        raise ValueError("pinned jar changed")
    files = {"coll-scenes.txt": work / "scenes.txt", "coll-defs.bundle": work / "defs.bundle"}
    for key in payloads:
        files[f"coll-{key}.bin"] = work / f"coll-{key}.bin"
    for name, _, _, _, _, _ in SCENES:
        files[f"coll-flags-{name}.bin"] = work / f"native-scene-{name}.flags"
    for native, fixture in OUTPUTS.items():
        files[fixture] = work / native
    evidence = {
        "jarSha256": base.PINNED,
        "javaSourceSha256": base.digest(source),
        "scenes": [scene[0] for scene in SCENES],
        "regions": sorted(f"{r[0]},{r[1]},{r[2]}" for r in regions),
        "payloads": sorted(payloads),
        "usedLocIds": len(used),
        "definitionsMissingFromTable": sum(1 for v in entries.values() if v is None),
        "idsRun": first.stdout.strip(),
        "fullRun": second.stdout.strip(),
        "movementStderr": second.stderr.strip(),
        "routeQueries": len((work / "native-routes.tsv").read_text().splitlines()),
        "reachQueries": len((work / "native-reach.tsv").read_text().splitlines()),
        "movementCases": len((work / "native-movement.tsv").read_text().splitlines()),
        "syntheticOps": sum(1 for line in (work / "native-synth.txt").read_text().splitlines() if line.startswith("O ")),
        "originalJarUnchanged": True,
    }
    (work / "collision-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    return files, evidence


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--fixtures", type=Path, default=FIXTURES)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        with tempfile.TemporaryDirectory(dir=args.work.parent if args.work.parent.exists() else None) as temp:
            files, evidence = generate(args.jar, args.cache, Path(temp))
            bad = []
            for name, path in sorted(files.items()):
                target = args.fixtures / name
                if not target.exists() or target.read_bytes() != path.read_bytes():
                    bad.append(name)
            print(json.dumps({"checked": len(files), "mismatched": bad, "evidence": evidence}))
            raise SystemExit(1 if bad else 0)
    files, evidence = generate(args.jar, args.cache, args.work)
    args.fixtures.mkdir(parents=True, exist_ok=True)
    for name, path in files.items():
        (args.fixtures / name).write_bytes(path.read_bytes())
    print(json.dumps(evidence))


if __name__ == "__main__":
    main()
