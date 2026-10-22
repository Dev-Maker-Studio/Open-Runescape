import argparse
import bz2
import gzip
import importlib.util
import json
import lzma
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("textured_oracle", HERE / "ground-lowering-textured-oracle.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)


class Cache:
    def __init__(self, root):
        self.root = Path(root)
        self.dat = open(self.root / "main_file_cache.dat2", "rb")

    def container(self, index, archive):
        idx = (self.root / f"main_file_cache.idx{index}").read_bytes()
        entry = idx[archive * 6:archive * 6 + 6]
        size = int.from_bytes(entry[:3], "big")
        sector = int.from_bytes(entry[3:], "big")
        header = 10 if archive > 0xFFFF else 8
        out = b""
        while len(out) < size:
            self.dat.seek(sector * 520)
            block = self.dat.read(520)
            out += block[header:header + min(size - len(out), 520 - header)]
            sector = int.from_bytes(block[header - 4:header - 1], "big")
        return out


def decode(container):
    kind = container[0]
    length = int.from_bytes(container[1:5], "big")
    if kind == 0:
        return container[5:5 + length]
    expanded = int.from_bytes(container[5:9], "big")
    body = container[9:9 + length]
    if kind == 1:
        return bz2.decompress(b"BZh1" + body)
    if kind == 2:
        return gzip.decompress(body)
    return lzma.LZMADecompressor(format=lzma.FORMAT_ALONE).decompress(body[:5] + expanded.to_bytes(8, "little") + body[5:])


def name_hash(name):
    value = 0
    for byte in name.encode("latin1"):
        value = (value * 31 + byte) & 0xFFFFFFFF
    return value


def named_archives(cache):
    data = decode(cache.container(255, 5))
    at = 1
    if data[0] >= 6:
        at += 4
    flags = data[at]
    at += 1

    def smart():
        nonlocal at
        if data[at] >= 0x80:
            value = int.from_bytes(data[at:at + 4], "big") & 0x7FFFFFFF
            at += 4
        else:
            value = int.from_bytes(data[at:at + 2], "big")
            at += 2
        return value

    count = smart()
    ids = []
    last = 0
    for _ in range(count):
        last += smart()
        ids.append(last)
    names = {}
    if flags & 1:
        for archive in ids:
            names[int.from_bytes(data[at:at + 4], "big")] = archive
            at += 4
    return names


def extract(cache, regions, work):
    names = named_archives(cache)
    found = {"m": [], "um": [], "l": [], "ul": []}
    loc_entries = 0
    for region in regions:
        x, y = region >> 8, region & 255
        for prefix in found:
            archive = names.get(name_hash(f"{prefix}{x}_{y}"))
            if archive is None:
                continue
            data = decode(cache.container(5, archive))
            if prefix in ("m", "um"):
                (work / f"{prefix}{region}.bin").write_bytes(data)
                found[prefix].append(region)
            else:
                found[prefix].append(region)
                if data != b"\x00":
                    loc_entries += 1
    return found, loc_entries


def run_oracle(work, jar, directory, base_tile, size, regions, underwater):
    result = subprocess.run(["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "GroundLightingWorldOracle",
        str(jar), str(directory), str(base_tile[0]), str(base_tile[1]), str(size), ",".join(str(r) for r in regions), "1" if underwater else "0"], check=True, capture_output=True, text=True)
    return result.stdout.strip()


def read_grid(path, key):
    for line in path.read_text().split("\n"):
        fields = line.split("\t")
        if fields[0] == key:
            return [int(v) for v in fields[1:]]
    raise ValueError(key)


def compare_lighting(native_path, rust_path, size):
    grid = (size + 1) ** 2
    native = native_path.read_bytes()
    rust = rust_path.read_bytes()
    if len(native) != grid * 13 or len(rust) != grid * 13:
        raise ValueError(f"lighting length native {len(native)} rust {len(rust)} expected {grid * 13}")
    normal_bytes = grid * 12
    mismatched = [i for i in range(grid) if native[i * 12:i * 12 + 12] != rust[i * 12:i * 12 + 12]]
    interior = sum(1 for z in range(1, size) for x in range(1, size) if native[(x + z * (size + 1)) * 12:(x + z * (size + 1)) * 12 + 12] != b"\x00" * 12)
    shadow_native = native[normal_bytes:]
    shadow_rust = rust[normal_bytes:]
    gw = size + 1

    def signed(value):
        return value - 256 if value > 127 else value

    formula_mismatch = 0
    for z in range(1, size):
        for x in range(1, size):
            s = lambda xx, zz: signed(shadow_native[xx + zz * gw])
            expected = ((s(x, z - 1) >> 2) + (s(x, z) >> 1) + (s(x - 1, z) >> 2) + (s(x, z + 1) >> 3) + (s(x + 1, z) >> 3)) & 255
            if shadow_rust[x + z * gw] != expected:
                formula_mismatch += 1
    return {"gridPoints": grid, "normalBytes": normal_bytes, "normalBytesEqual": normal_bytes - sum(1 for a, b in zip(native[:normal_bytes], rust[:normal_bytes]) if a != b),
        "gridPointMismatches": len(mismatched), "firstMismatches": mismatched[:8], "interiorNonZeroNormals": interior,
        "nativeShadowingNonZero": sum(1 for v in shadow_native if v), "rustIntensityNonZero": sum(1 for v in shadow_rust if v), "intensityFormulaMismatches": formula_mismatch}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--rust", type=Path, required=True)
    parser.add_argument("--regions", default="31620,31621,31876,31877,32132,32133")
    parser.add_argument("--base", default="7904,8456")
    parser.add_argument("--size", type=int, default=104)
    parser.add_argument("--underwater", type=int, default=1)
    args = parser.parse_args()
    work = args.work
    work.mkdir(parents=True, exist_ok=True)
    regions = [int(v) for v in args.regions.split(",")]
    base_tile = [int(v) for v in args.base.split(",")]
    jar = work / "oracle-client.jar"
    base.private_copy(args.jar, jar, base.PINNED)
    source = HERE / "GroundLightingWorldOracle.java"
    subprocess.run(["javac", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    found, loc_entries = extract(Cache(args.cache), regions, work)
    stdout = run_oracle(work, jar, work, base_tile, args.size, regions, bool(args.underwater))
    heights = {}
    for key in ("surface", "normal"):
        native = read_grid(work / "native-heights.tsv", key)
        rust = read_grid(args.rust / "rust-heights.tsv", key)
        heights[key] = {"points": len(native), "mismatches": sum(1 for a, b in zip(native, rust) if a != b) + abs(len(native) - len(rust))}
    heights["normalDiffersFromSurface"] = sum(1 for a, b in zip(read_grid(work / "native-heights.tsv", "normal"), read_grid(work / "native-heights.tsv", "surface")) if a != b)
    lighting = compare_lighting(work / "native-lighting.bin", args.rust / "rust-lighting.bin", args.size)
    evidence = {"jarSha256": base.PINNED, "javaSourceSha256": base.digest(source), "oracleOutput": stdout, "regions": regions, "files": found,
        "locArchivesWithObjects": loc_entries, "underwater": bool(args.underwater), "heights": heights, "lighting": lighting}
    (work / "lighting-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(json.dumps(evidence))


if __name__ == "__main__":
    main()
