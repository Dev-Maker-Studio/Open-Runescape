import argparse
import importlib.util
import json
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


loc = load("loc_oracle", "loc-oracle.py")
lw = loc.lw
base = loc.base

SCENES = {
    "a": (39 * 64 + 24, 50 * 64 + 24, 104),
    "b": (40 * 64, 48 * 64, 128),
    "c": (53 * 64 + 20, 53 * 64 + 20, 104),
}


def regions_of(base_x, base_y, size):
    return [(x << 8) | y for x in range(base_x // 64, (base_x + size - 1) // 64 + 1) for y in range(base_y // 64, (base_y + size - 1) // 64 + 1)]


def usmart(out, value):
    if value < 128:
        out.append(value)
    else:
        out += struct.pack(">H", value + 32768)


def encode(locs):
    by_id = {}
    for entry in locs:
        packed = (entry["level"] << 12) | (entry["x"] << 6) | entry["y"]
        by_id.setdefault(entry["id"], []).append((packed, (entry["shape"] << 2) | entry["rotation"]))
    out = bytearray()
    last = -1
    for loc_id in sorted(by_id):
        delta = loc_id - last
        last = loc_id
        while delta >= 32767:
            usmart(out, 32767)
            delta -= 32767
        usmart(out, delta)
        position = 0
        for packed, description in sorted(by_id[loc_id]):
            usmart(out, packed - position + 1)
            position = packed
            out.append(description)
        usmart(out, 0)
    usmart(out, 0)
    return bytes(out)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--maps", type=Path, default=ROOT / "assets/cache/maps")
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--fixtures", type=Path)
    args = parser.parse_args()
    work = args.work
    work.mkdir(parents=True, exist_ok=True)
    jar = work / "oracle-client.jar"
    base.private_copy(args.jar, jar, base.PINNED)
    source = HERE / "StaticShadowOracle.java"
    subprocess.run(["javac", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    streams = {}
    shapes = {}
    used = set()
    for name, (bx, by, size) in SCENES.items():
        for region in regions_of(bx, by, size):
            if region in streams:
                continue
            path = args.maps / f"{region >> 8}_{region & 255}" / "locs.json"
            if not path.exists():
                continue
            locs = json.loads(path.read_text())["locs"]
            streams[region] = encode(locs)
            for entry in locs:
                used.add(entry["id"])
                shapes[entry["shape"]] = shapes.get(entry["shape"], 0) + 1
    for region, data in streams.items():
        (work / f"shadow-l{region}.bin").write_bytes(data)
    cache = lw.Cache(args.cache)
    table = loc.reference_table(lw.decode(cache.container(255, 16)))
    archives = {}

    def definition(loc_id):
        archive = loc_id >> 8
        if archive not in table or (loc_id & 255) not in table[archive]:
            return None
        if archive not in archives:
            archives[archive] = loc.split(lw.decode(loc.trim(cache.container(16, archive))), table[archive])
        return archives[archive][loc_id & 255]

    entries = {loc_id: definition(loc_id) for loc_id in sorted(used)}
    missing = sorted(k for k, v in entries.items() if v is None)
    (work / "shadow-defs.bundle").write_bytes(loc.bundle_bytes(entries))
    outputs = []
    for name, (bx, by, size) in SCENES.items():
        regions = regions_of(bx, by, size)
        run = subprocess.run(
            ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "StaticShadowOracle", str(jar), str(work), name, str(bx), str(by), str(size), ",".join(str(r) for r in regions)],
            check=True, capture_output=True, text=True,
        )
        outputs.append(run.stdout.strip())
    if args.fixtures:
        args.fixtures.mkdir(parents=True, exist_ok=True)
        for region in streams:
            (args.fixtures / f"shadow-l{region}.bin").write_bytes((work / f"shadow-l{region}.bin").read_bytes())
        (args.fixtures / "shadow-defs.bundle").write_bytes((work / "shadow-defs.bundle").read_bytes())
        for name in SCENES:
            for ext in ("bin", "tsv"):
                (args.fixtures / f"native-shadow-{name}.{ext}").write_bytes((work / f"native-shadow-{name}.{ext}").read_bytes())
    evidence = {
        "jarSha256": base.PINNED, "javaSourceSha256": base.digest(source), "regions": sorted(streams), "distinctLocIds": len(used),
        "missingDefinitions": missing, "shapeCounts": shapes, "oracle": outputs,
        "stats": [line for name in SCENES for line in (work / f"native-shadow-{name}.tsv").read_text().splitlines()],
    }
    (work / "shadow-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(json.dumps(evidence, indent=1))


if __name__ == "__main__":
    main()
