import argparse
import importlib.util
import json
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
spec = importlib.util.spec_from_file_location("textured_oracle", HERE / "ground-lowering-textured-oracle.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)


def parse_case(path):
    lines = path.read_text().split("\n")
    case = {"materials": [], "tiles": []}
    tile = None
    for line in lines:
        fields = line.split("\t")
        key = fields[0]
        if key == "case":
            case["name"], case["width"], case["height"] = fields[1], int(fields[2]), int(fields[3])
        elif key == "position":
            case["positionHeights"] = [int(v) for v in fields[1:]]
        elif key == "normal":
            case["normalHeights"] = [int(v) for v in fields[1:]]
        elif key == "shadow":
            case["bakedShadows"] = [int(v) for v in fields[1:]]
        elif key == "material":
            case["materials"].append({"id": int(fields[1]), "alpha": int(fields[2]), "brightness": int(fields[3]), "effect": int(fields[4])})
        elif key == "tile":
            tile = {"x": int(fields[1]), "z": int(fields[2]), "emittedVertices": int(fields[3]), "hasShadows": int(fields[4]) != 0, "hasBlendingColors": len(fields) < 6 or int(fields[5]) != 0, "vertices": []}
            case["tiles"].append(tile)
        elif key == "vertex":
            tile["vertices"].append([int(v) for v in fields[1:]])
    return case


def masked(case):
    copy = dict(case)
    copy["tiles"] = []
    changed = 0
    for tile in case["tiles"]:
        vertices = []
        for v in tile["vertices"]:
            v = list(v)
            if tile["hasBlendingColors"] and v[3] < -1:
                v[3] &= 0xFFFF
                changed += 1
            vertices.append(v)
        copy["tiles"].append(dict(tile, vertices=vertices))
    return copy, changed


def run_oracle(work, jar, native, case, tag):
    input_path = work / f"native-{tag}-input.tsv"
    output_path = work / f"native-{tag}.json"
    base.write_input([case], input_path)
    subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{work}:{jar}", "GroundLoweringTexturedOracle", str(jar), str(native), str(input_path), str(output_path)], check=True)
    compiled = json.loads(output_path.read_text())["cases"][0]
    selections = {}
    for index, material in enumerate(compiled["materials"]):
        selections[(material["materialId"], material["textureScale"])] = base.reference_selection(case, compiled, index, 0)
    return selections


def compare(work, rust_mesh, selections):
    result = {"materials": []}
    total = {"vertices": 0, "vertexMatches": 0, "indices": 0, "indexMatches": 0, "vertexBytes": 0, "vertexByteMatches": 0}
    keys = set()
    for index, material in enumerate(rust_mesh["materials"]):
        key = (material["materialId"], material["textureScale"])
        keys.add(key)
        rust_vertices = (work / f"rust-{index}.vertices.bin").read_bytes()
        rust_indices = (work / f"rust-{index}.indices.bin").read_bytes()
        rust_indices = [int.from_bytes(rust_indices[i:i + 4], "little") for i in range(0, len(rust_indices), 4)]
        entry = {"materialId": key[0], "textureScale": key[1], "rustVertices": len(rust_vertices) // 40, "rustIndices": len(rust_indices)}
        native = selections.get(key)
        if native is None:
            entry["present"] = False
            result["materials"].append(entry)
            continue
        nv = bytes.fromhex(native["vertexBytes"])
        entry["present"] = True
        entry["nativeVertices"] = len(nv) // 40
        entry["nativeIndices"] = len(native["indices"])
        count = max(len(nv), len(rust_vertices)) // 40
        matches = sum(1 for v in range(min(len(nv), len(rust_vertices)) // 40) if nv[v * 40:v * 40 + 40] == rust_vertices[v * 40:v * 40 + 40])
        index_matches = sum(1 for a, b in zip(native["indices"], rust_indices) if a == b)
        index_count = max(len(native["indices"]), len(rust_indices))
        byte_matches = sum(1 for a, b in zip(nv, rust_vertices) if a == b)
        entry.update({"vertexMatches": matches, "vertexMismatches": count - matches, "indexMatches": index_matches, "indexMismatches": index_count - index_matches,
            "sourceFirstVertexNative": native["sourceFirstVertex"], "sourceFirstVertexRust": rust_mesh["report"]["materials"][index]["sourceFirstVertex"], "exactBytes": nv == rust_vertices and native["indices"] == rust_indices})
        total["vertices"] += count
        total["vertexMatches"] += matches
        total["indices"] += index_count
        total["indexMatches"] += index_matches
        total["vertexBytes"] += max(len(nv), len(rust_vertices))
        total["vertexByteMatches"] += byte_matches
        result["materials"].append(entry)
    result["nativeOnlyMaterials"] = sorted(set(selections) - keys)
    result["totals"] = total
    result["totals"]["vertexMismatches"] = total["vertices"] - total["vertexMatches"]
    result["totals"]["indexMismatches"] = total["indices"] - total["indexMatches"]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native-library", type=Path, required=True)
    parser.add_argument("--work", type=Path, default=ROOT / "harness/target/loop/iteration21-validation/lowering")
    args = parser.parse_args()
    work = args.work
    jar = work / "oracle-client.jar"
    native = work / "libjaclib.so"
    base.private_copy(args.jar, jar, base.PINNED)
    base.private_copy(args.native_library, native, base.LIBRARY)
    source = HERE / "GroundLoweringTexturedOracle.java"
    subprocess.run(["javac", "-cp", str(jar), "-d", str(work), str(source)], check=True)
    case = parse_case(work / "world-input.tsv")
    rust_mesh = json.loads((work / "rust-mesh.json").read_text())
    negative = sum(1 for tile in case["tiles"] if tile["hasBlendingColors"] for v in tile["vertices"] if v[3] < -1)
    variants = {}
    raw = compare(work, rust_mesh, run_oracle(work, jar, native, case, "raw"))
    variants["nativeRawSigned"] = raw
    masked_case, changed = masked(case)
    variants["nativeMasked"] = compare(work, rust_mesh, run_oracle(work, jar, native, masked_case, "masked"))
    evidence = {"jarSha256": base.PINNED, "nativeLibrarySha256": base.LIBRARY, "javaSourceSha256": base.digest(source), "inputSha256": base.digest(work / "world-input.tsv"),
        "tiles": len(case["tiles"]), "vertexRecords": sum(len(t["vertices"]) for t in case["tiles"]), "blendValuesBelowMinusOne": negative, "maskedValuesChanged": changed, "rustMeshSha256": rust_mesh["report"]["sha256"], "variants": variants}
    (work / "lowering-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    for name, value in variants.items():
        print(name, json.dumps(value["totals"]), "nativeOnly", value["nativeOnlyMaterials"])


if __name__ == "__main__":
    main()
