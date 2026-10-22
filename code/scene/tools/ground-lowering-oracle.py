import argparse
from hashlib import sha256
import json
from pathlib import Path
from random import Random
import shutil
import subprocess

PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
LIBRARY = "92f32235d2a67e7debedd89fbccd2ac0e937302eaa7e0732fe0ba93072a08a74"
ROOT = Path(__file__).resolve().parents[2]


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


def generate():
    rng = Random(160727)
    cases = []
    for case_id in range(72):
        width, height = [(1, 1), (2, 3), (3, 2), (9, 10), (10, 9), (4, 4)][case_id % 6]
        count = (width + 1) * (height + 1)
        position = [rng.randrange(-20000, 20000) for _ in range(count)]
        normal = [rng.randrange(-500, 500) for _ in range(count)]
        shadows = [rng.randrange(256) for _ in range(count)]
        scale = [64, 128, 256][case_id % 3]
        if case_id % 8 == 0:
            position = [0] * count
            normal = [0] * count
            shadows = [0] * count
        if case_id % 8 == 1:
            position = [2147483647 if i % 2 else -2147483648 for i in range(count)]
            normal = [35000 * (i % (width + 1)) for i in range(count)]
        if case_id % 8 == 2:
            shadows = [255 if i % 2 else 128 for i in range(count)]
        tiles = []
        for x in range(width):
            for z in range(height):
                if case_id == 0 or (x * 7 + z * 11 + case_id) % 7 == 0:
                    continue
                vertices = []
                main = [0, 127, 128, 65535, 12000, 31000][(x + z + case_id) % 6]
                blend = main if case_id % 3 == 0 else ((main + 8192) & 65535)
                points = [(0, 0), (512, 0), (512, 512), (0, 0), (512, 512), (0, 512)]
                if case_id % 4 == 1:
                    points += [(128, 128), (256, 384), (384, 256)]
                if case_id % 4 == 2:
                    points += [(127, 128), (128, 127), (129, 129)] * 2
                if case_id % 4 == 3:
                    points += [(256, 256), (128, 384), (384, 128)] * 2
                for i, (ox, oz) in enumerate(points):
                    color = main if i < 6 else (main + i * 31) & 65535
                    vertices.append([ox, oz, color, blend, -1, scale])
                tiles.append({"x": x, "z": z, "emittedVertices": 3 if case_id == 3 else len(vertices), "hasShadows": bool((x + z + case_id) % 2), "vertices": vertices})
        rng.shuffle(tiles)
        cases.append({"name": f"ordinary-hd-{case_id}", "width": width, "height": height, "textureScale": scale, "positionHeights": position, "normalHeights": normal, "bakedShadows": shadows, "tiles": tiles})
    for number, points in enumerate([[(129, 128), (128, 128), (129, 128)], [(128, 128)] * 3, [(i % 5 * 128, i % 3 * 256) for i in range(36)]]):
        cases.append({"name": f"packed-key-{number}", "width": 1, "height": 1, "textureScale": 128,
            "positionHeights": [17, 51, 100, -23], "normalHeights": [0] * 4, "bakedShadows": [255] * 4,
            "tiles": [{"x": 0, "z": 0, "emittedVertices": len(points), "hasShadows": True,
                "vertices": [[x, z, 12000, 31000, -1, 128] for x, z in points]}]})
    return cases + topology_cases()


def topology_cases():
    path = ROOT / "scene/tests/fixtures/ground-geometry.json"
    fixture = json.loads(path.read_text())
    assert fixture["jar_sha256"] == PINNED
    result = []
    for case in fixture["cases"]:
        source = case["input"]
        calls = case["calls"]
        if source["heights_current"] is not None or source["heights_next"] is not None or not calls:
            continue
        colors = [v for call in calls for v in call["main_colors"] + (call["blending_colors"] or [])]
        textures = [v for call in calls for v in call["textures"]]
        scales = {v for call in calls for v in call["texture_scales"]}
        if any(v != -1 for v in textures) or any(not 0 <= v <= 65535 for v in colors) or len(scales) != 1 or min(scales) <= 0:
            continue
        if any(call["draw_heights_a"] is not None or call["draw_heights_b"] is not None for call in calls):
            continue
        width, height = case["width"], case["height"]
        position = [v for row in source["heights"] for v in row]
        assert len(position) == (width + 1) * (height + 1)
        tiles = []
        for call in calls:
            blend = call["blending_colors"] or call["main_colors"]
            vertices = [list(v) for v in zip(call["offsets_x"], call["offsets_z"], call["main_colors"], blend, call["textures"], call["texture_scales"])]
            tiles.append({"x": call["x"], "z": call["z"], "emittedVertices": len(vertices), "hasShadows": call["shadows"],
                "shape": call["effective_shape"], "rotation": call["effective_rotation"], "hasBlendingColors": call["blending_colors"] is not None, "vertices": vertices})
        result.append({"name": "topology-" + case["name"], "topologyOracleCase": case["name"], "width": width, "height": height,
            "textureScale": min(scales), "positionHeights": position, "normalHeights": position, "bakedShadows": [0] * len(position), "tiles": tiles})
    assert len(result) == 118
    return result


def write_input(cases, path):
    lines = []
    for case in cases:
        lines.append(f"case\t{case['name']}\t{case['width']}\t{case['height']}\t{case['textureScale']}")
        for key, label in [("positionHeights", "position"), ("normalHeights", "normal"), ("bakedShadows", "shadow")]:
            lines.append(label + "\t" + "\t".join(map(str, case[key])))
        for tile in case["tiles"]:
            lines.append(f"tile\t{tile['x']}\t{tile['z']}\t{tile['emittedVertices']}\t{int(tile['hasShadows'])}\t{int(tile.get('hasBlendingColors', True))}")
            for v in tile["vertices"]:
                lines.append("vertex\t" + "\t".join(map(str, v)))
        lines.append("end")
    path.write_text("\n".join(lines) + "\n")


def reference_selection(case, compiled, mode):
    width, height = case["width"], case["height"]
    visible = [mode == 0 or (mode == 1 and i % 3 == 1) or (mode == 2 and i % 5 == 0) for i in range(width * height)]
    indices = []
    chunks_x = (width + 7) // 8
    chunks_z = (height + 7) // 8
    for chunk in range(chunks_x * chunks_z):
        for tile, tri in enumerate(compiled["indices"]):
            if not visible[tile] or tri is None:
                continue
            if (tile % width) // 8 + ((tile // width) // 8) * chunks_x != chunk:
                continue
            mask = compiled["masks"][tile]
            for face in range(len(tri) // 3):
                if mask & (1 << face):
                    indices.extend(tri[face * 3:face * 3 + 3])
    low, high = (min(indices), max(indices)) if indices else (0, -1)
    source = bytes.fromhex(compiled["vertexBytes"])
    colors = bytes.fromhex(compiled["materialBytes"])
    canonical = bytearray()
    for index in range(low, high + 1):
        base = index * 36
        canonical.extend(source[base:base + 12])
        canonical.extend(colors[index * 4:index * 4 + 4])
        canonical.extend(source[base + 16:base + 24])
        canonical.extend(bytes(4))
        canonical.extend(source[base + 24:base + 36])
    return {"visibleTiles": visible, "sourceFirstVertex": low, "vertexBytes": canonical.hex(), "indices": [index - low for index in indices]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native-library", type=Path, required=True)
    parser.add_argument("--work", type=Path, default=ROOT / "harness/target/loop/iteration16-validation/ground-lowering")
    parser.add_argument("--fixture", type=Path, default=ROOT / "scene/tests/fixtures/ground-lowering.json")
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    jar = args.work / "oracle-client.jar"
    native = args.work / "libjaclib.so"
    private_copy(args.jar, jar, PINNED)
    private_copy(args.native_library, native, LIBRARY)
    cases = generate()
    input_path = args.work / "ground-lowering-input.tsv"
    output_path = args.work / "ground-lowering-native.json"
    write_input(cases, input_path)
    source = Path(__file__).with_name("GroundLoweringOracle.java")
    subprocess.run(["javac", "-cp", str(jar), "-d", str(args.work), str(source)], check=True)
    subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{args.work}:{jar}", "GroundLoweringOracle", str(jar), str(native), str(input_path), str(output_path)], check=True)
    native_output = json.loads(output_path.read_text())
    assert len(native_output["cases"]) == len(cases)
    for case, compiled in zip(cases, native_output["cases"]):
        assert case["name"] == compiled["name"]
        case["compiled"] = compiled
        case["selections"] = [reference_selection(case, compiled, mode) for mode in range(4)]
    native_output["cases"] = cases
    native_output["selectionSource"] = "source-contract reference over actual compiled buffers; not a GL draw"
    native_output["javaSourceSha256"] = digest(source)
    native_output["inputSha256"] = digest(input_path)
    for original, copy, expected in [(args.jar, jar, PINNED), (args.native_library, native, LIBRARY)]:
        assert digest(original) == digest(copy) == expected
    args.fixture.write_text(json.dumps(native_output, separators=(",", ":")) + "\n")
    (args.work / "oracle-evidence.json").write_text(json.dumps({"jarSha256": PINNED, "nativeLibrarySha256": LIBRARY, "sourceSha256": digest(source), "fixtureSha256": digest(args.fixture), "cases": len(cases), "allocatedVertices": sum(c["compiled"]["allocatedVertices"] for c in cases), "uniqueVertices": sum(c["compiled"]["vertexCount"] for c in cases), "originalsUnchanged": True, "glDrawExecuted": False}, indent=2) + "\n")
    print(f"WROTE {args.fixture} {len(cases)} unchanged GroundGL cases")


if __name__ == "__main__":
    main()
