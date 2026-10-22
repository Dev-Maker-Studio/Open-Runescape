import argparse
import hashlib
import importlib.util
import json
import struct
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("lighting_oracle", HERE / "ground-lighting-world-oracle.py")
lw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lw)
base = lw.base


def trim(container):
    kind = container[0]
    length = int.from_bytes(container[1:5], "big")
    body = 5 + length if kind == 0 else 9 + length
    if len(container) not in (body, body + 2):
        raise ValueError(f"unexpected container length {len(container)} for body {body}")
    return container[:body]


def smart(data, at, protocol):
    if protocol >= 7:
        if data[at] >= 0x80:
            return int.from_bytes(data[at:at + 4], "big") & 0x7FFFFFFF, at + 4
        return int.from_bytes(data[at:at + 2], "big"), at + 2
    return int.from_bytes(data[at:at + 2], "big"), at + 2


def reference_table(data):
    protocol = data[0]
    at = 1
    if protocol >= 6:
        at += 4
    flags = data[at]
    at += 1
    count, at = smart(data, at, protocol)
    ids = []
    last = 0
    for _ in range(count):
        delta, at = smart(data, at, protocol)
        last += delta
        ids.append(last)
    if flags & 1:
        at += 4 * count
    at += 4 * count
    if flags & 2:
        at += 64 * count
    at += 4 * count
    counts = []
    for _ in range(count):
        value, at = smart(data, at, protocol)
        counts.append(value)
    files = {}
    for archive, number in zip(ids, counts):
        ids_in = []
        last = 0
        for _ in range(number):
            delta, at = smart(data, at, protocol)
            last += delta
            ids_in.append(last)
        files[archive] = ids_in
    return files


def split(payload, file_ids):
    if len(file_ids) == 1:
        return {file_ids[0]: payload}
    chunks = payload[-1]
    at = len(payload) - 1 - chunks * len(file_ids) * 4
    sizes = [[0] * len(file_ids) for _ in range(chunks)]
    totals = [0] * len(file_ids)
    for chunk in range(chunks):
        running = 0
        for index in range(len(file_ids)):
            running += struct.unpack(">i", payload[at:at + 4])[0]
            at += 4
            sizes[chunk][index] = running
            totals[index] += running
    parts = [bytearray() for _ in file_ids]
    cursor = 0
    for chunk in range(chunks):
        for index in range(len(file_ids)):
            size = sizes[chunk][index]
            parts[index] += payload[cursor:cursor + size]
            cursor += size
    return {file_id: bytes(part) for file_id, part in zip(file_ids, parts)}


def loc_ids(rows):
    return sorted({int(row[2]) for row in rows})


def bundle_bytes(entries):
    out = bytearray(struct.pack(">i", len(entries)))
    for key in sorted(entries):
        data = entries[key]
        out += struct.pack(">ii", key, len(data) if data is not None else -1)
        if data is not None:
            out += data
    return bytes(out)


def read_rows(path):
    return [line.split("\t") for line in path.read_text().splitlines() if line]


def placement_rows(rows):
    return [row for row in rows if row[2] not in ("PAYLOAD", "END")]


def extra_archives(lines):
    picks = []
    for marker in ("parameters=[", "members=1", "transforms=1", "ignoreClipOnAltRoute=1", "soundGroupIds=[", "recolorPalette=["):
        counts = {}
        for line in lines:
            fields = line.split("\t")
            if any(field.startswith(marker) and field != "parameters=null" and not field.endswith("=[]") and not field.endswith("=null") for field in fields[2:]):
                key = int(fields[0]) >> 8
                counts[key] = counts.get(key, 0) + 1
        if counts:
            picks.append(max(sorted(counts), key=lambda k: counts[k]))
    return set(picks)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--fixtures", type=Path)
    parser.add_argument("--regions", default="")
    parser.add_argument("--base", default="8160,8840")
    parser.add_argument("--size", type=int, default=104)
    parser.add_argument("--sweep", type=int, default=1)
    args = parser.parse_args()
    work = args.work
    work.mkdir(parents=True, exist_ok=True)
    base_x, base_y = (int(v) for v in args.base.split(","))
    if args.regions:
        regions = [int(v) for v in args.regions.split(",")]
    else:
        regions = [(x << 8) | y for x in range(base_x // 64, (base_x + args.size - 1) // 64 + 1) for y in range(base_y // 64, (base_y + args.size - 1) // 64 + 1)]
    jar = work / "oracle-client.jar"
    base.private_copy(args.jar, jar, base.PINNED)
    source = HERE / "LocOracle.java"
    subprocess.run(["javac", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    cache = lw.Cache(args.cache)
    names = lw.named_archives(cache)
    archives = {}
    stub = 0
    data_regions = 0
    for region in regions:
        x, y = region >> 8, region & 255
        for kind in ("l", "ul"):
            archive = names.get(lw.name_hash(f"{kind}{x}_{y}"))
            if archive is None:
                continue
            container = trim(cache.container(5, archive))
            payload = lw.decode(container)
            (work / f"{kind}{region}.container").write_bytes(container)
            archives[(region, kind)] = (container, payload)
            if payload == b"\x00":
                stub += 1
            else:
                data_regions += 1
    java_args = ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "LocOracle", str(jar), str(work), ",".join(str(r) for r in regions)]
    for stale in ("defs.bundle", "native-defs.tsv", "rust-defs.tsv"):
        (work / stale).unlink(missing_ok=True)
    subprocess.run(java_args, check=True, capture_output=True, text=True)
    native_locs = read_rows(work / "native-locs.tsv")
    used = loc_ids(placement_rows(native_locs))
    reference = lw.decode(cache.container(255, 16))
    table = reference_table(reference)
    cache_archives = {}

    def archive_files(archive):
        if archive not in cache_archives:
            container = trim(cache.container(16, archive))
            cache_archives[archive] = (container, split(lw.decode(container), table[archive]))
        return cache_archives[archive]

    def definition(loc_id):
        archive = loc_id >> 8
        if archive not in table or (loc_id & 255) not in table[archive]:
            return None
        return archive_files(archive)[1][loc_id & 255]

    all_ids = [(a << 8) | f for a in sorted(table) for f in table[a]]
    chosen = all_ids if args.sweep else used
    entries = {loc_id: definition(loc_id) for loc_id in chosen}
    (work / "defs.bundle").write_bytes(bundle_bytes(entries))
    result = subprocess.run(java_args, check=True, capture_output=True, text=True)
    rust = subprocess.run(["cargo", "run", "--release", "--quiet", "--example", "loc_dump", "--", str(work), ",".join(str(r) for r in regions)], cwd=HERE.parent, check=True, capture_output=True, text=True)
    native_rows = native_locs
    rust_locs = read_rows(work / "rust-locs.tsv")
    java_place = [row for row in native_rows if row[2] != "PAYLOAD"]
    rust_place = [row for row in rust_locs if row[2] != "PAYLOAD"]
    java_payload = {(row[0], row[1]): (int(row[3]), row[4]) for row in native_rows if row[2] == "PAYLOAD"}
    rust_payload = {(row[0], row[1]): int(row[3]) for row in rust_locs if row[2] == "PAYLOAD"}
    payload_mismatch = 0
    for (region, kind), (container, payload) in archives.items():
        key = (str(region), kind)
        if java_payload[key] != (len(payload), sha(payload)) or rust_payload[key] != len(payload):
            payload_mismatch += 1
    placement_mismatch = abs(len(java_place) - len(rust_place)) + sum(1 for a, b in zip(java_place, rust_place) if a != b)
    xtea_java = [row for row in read_rows(work / "native-xtea.tsv") if row[0] == "XTEA"]
    xtea_rust = read_rows(work / "rust-xtea.tsv")
    xtea_ok = sum(1 for row in xtea_rust if row[4] == "restored=true" and row[5] == "decoded=true")
    xtea_changed = sum(1 for row in xtea_java if row[7] == "changed=true")
    defs_java = (work / "native-defs.tsv").read_text().splitlines()
    defs_rust = (work / "rust-defs.tsv").read_text().splitlines()
    defs_mismatch = abs(len(defs_java) - len(defs_rust))
    first = []
    for a, b in zip(defs_java, defs_rust):
        if a != b:
            defs_mismatch += 1
            if len(first) < 5:
                fa, fb = a.split("\t"), b.split("\t")
                diff = [x for x, y in zip(fa, fb) if x != y][:2]
                first.append({"line": fa[0] + "/" + fa[1], "java": diff[0][:200] if diff else a[:200], "rust": diff[1][:200] if diff else b[:200]})
    errors = sum(1 for line in defs_java if line.split("\t")[2] == "error")
    by_type = {}
    for row in placement_rows(native_rows):
        by_type[row[6]] = by_type.get(row[6], 0) + 1
    if args.fixtures:
        args.fixtures.mkdir(parents=True, exist_ok=True)
        for (region, kind), (container, payload) in archives.items():
            if payload != b"\x00" or kind == "l":
                (args.fixtures / f"loc-{kind}{region}.container").write_bytes(container)
        (args.fixtures / "loc-reference.container").write_bytes(trim(cache.container(255, 16)))
        needed = sorted({loc_id >> 8 for loc_id in used} | extra_archives(defs_java))
        for archive in needed:
            (args.fixtures / f"loc-defs-{archive}.container").write_bytes(archive_files(archive)[0])
        (args.fixtures / "loc-placements.tsv").write_text("\n".join("\t".join(row) for row in native_rows) + "\n")
        subset = [line for line in defs_java if int(line.split("\t")[0]) >> 8 in set(needed)]
        (args.fixtures / "loc-defs.tsv").write_text("\n".join(subset) + "\n")
        for path in sorted(work.glob("xtea-l*.bin")):
            if path.name.startswith("xtea-l") and path.stat().st_size < 4096:
                (args.fixtures / f"loc-{path.name}").write_bytes(path.read_bytes())
        (args.fixtures / "loc-xtea.tsv").write_text((work / "native-xtea.tsv").read_text())
    evidence = {
        "jarSha256": base.PINNED, "javaSourceSha256": base.digest(source), "regions": regions, "base": [base_x, base_y], "size": args.size,
        "locArchives": len(archives), "stubArchives": stub, "archivesWithData": data_regions,
        "payloadBytes": {f"{k[1]}{k[0]}": len(v[1]) for k, v in archives.items() if v[1] != b"\x00"},
        "payloadMismatches": payload_mismatch, "placements": len(placement_rows(native_rows)), "placementMismatches": placement_mismatch,
        "placementsByType": by_type, "distinctLocIds": len(used),
        "xteaCases": len(xtea_java), "xteaRustRestoredAndDecoded": xtea_ok, "xteaEncryptedDiffersFromPlain": xtea_changed,
        "definitionIds": len(entries), "definitionLines": len(defs_java), "definitionMissingFiles": sum(1 for v in entries.values() if v is None),
        "javaDefinitionErrors": errors, "definitionMismatches": defs_mismatch, "firstDefinitionMismatches": first,
        "sweep": bool(args.sweep), "oracleOutput": result.stdout.strip(),
    }
    (work / "loc-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(json.dumps(evidence))


if __name__ == "__main__":
    main()
