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
DEFAULT_SURFACE_WATER = [1583160, 40, 127, 63, 0, 0, 0]
MATERIAL_ID_POOL = [0, 1, 7, 16383, 300, 4096, 9000]
SCALE_POOL = [0, 4, 256, 764, 1028, 65532, 65535, 128]


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


def material_uid(material_id, scale):
    water = DEFAULT_SURFACE_WATER
    return (water[2] << 48) | (water[1] << 42) | (water[0] << 28) | (scale << 14) | material_id


def random_materials(rng, count):
    ids = rng.sample(MATERIAL_ID_POOL, count)
    materials = []
    for index, material_id in enumerate(ids):
        alpha = [0, 1, 128, 255, rng.randrange(256)][index % 5]
        brightness = [0, 255, rng.randrange(256), 0, 1][index % 5]
        effect = [0, 4, rng.randrange(-128, 128), 4, 0][(index + count) % 5]
        materials.append({"id": material_id, "alpha": alpha, "brightness": brightness, "effect": effect})
    return materials


def generate():
    rng = Random(170727)
    cases = []
    for case_id in range(60):
        width, height = [(1, 1), (2, 3), (3, 2), (9, 10), (10, 9), (4, 4)][case_id % 6]
        count = (width + 1) * (height + 1)
        position = [rng.randrange(-20000, 20000) for _ in range(count)]
        normal = [rng.randrange(-500, 500) for _ in range(count)]
        shadows = [rng.randrange(256) for _ in range(count)]
        if case_id % 8 == 0:
            position = [0] * count
            normal = [0] * count
            shadows = [0] * count
        if case_id % 8 == 1:
            position = [2147483647 if i % 2 else -2147483648 for i in range(count)]
            normal = [35000 * (i % (width + 1)) for i in range(count)]
        if case_id % 8 == 2:
            shadows = [255 if i % 2 else 128 for i in range(count)]
        materials = random_materials(rng, 1 + case_id % 5)
        colorless = case_id % 4 == 0
        main_pool = [-1, 0, 127, 128, 12000, 31000] if colorless else [0, 127, 128, 65535, 12000, 31000]
        scales = rng.sample(SCALE_POOL, 1 + case_id % 3)
        texture_pool = [m["id"] for m in materials] + [-1]
        if case_id % 7 == 6:
            texture_pool = [m["id"] for m in materials]
        if case_id % 7 == 5:
            texture_pool = [-1]
        tiles = []
        for x in range(width):
            for z in range(height):
                if case_id != 0 and (x * 7 + z * 11 + case_id) % 7 == 0:
                    continue
                vertices = []
                points = [(0, 0), (512, 0), (512, 512), (0, 0), (512, 512), (0, 512)]
                if case_id % 4 == 1:
                    points += [(128, 128), (256, 384), (384, 256)]
                if case_id % 4 == 2:
                    points += [(127, 128), (128, 127), (129, 129)] * 2
                if case_id % 4 == 3:
                    points += [(256, 256), (128, 384), (384, 128)] * 2
                tile_textures = [rng.choice(texture_pool) for _ in range(2)]
                tile_scales = [rng.choice(scales) for _ in range(2)]
                for i, (ox, oz) in enumerate(points):
                    gx, gz = x * 512 + ox, z * 512 + oz
                    main = main_pool[(gx // 128 * 5 + gz // 128 * 3 + case_id) % len(main_pool)]
                    if main == -1:
                        blend = -1 if case_id % 2 else 12000
                    elif case_id % 3 == 0:
                        blend = main
                    elif case_id % 5 == 1 and not colorless and i % 4 == 1:
                        blend = -1
                    else:
                        blend = (main + 8192) & 65535
                    slot = (i // 3) % 2
                    vertices.append([ox, oz, main, blend, tile_textures[slot], tile_scales[slot]])
                tiles.append({"x": x, "z": z, "emittedVertices": 3 if case_id == 3 else len(vertices), "hasShadows": bool((x + z + case_id) % 2),
                    "hasBlendingColors": case_id % 9 != 4, "vertices": vertices})
        rng.shuffle(tiles)
        cases.append({"name": f"textured-hd-{case_id}", "width": width, "height": height, "materials": materials,
            "positionHeights": position, "normalHeights": normal, "bakedShadows": shadows, "tiles": tiles})
    cases += special_cases()
    return cases + topology_cases(rng)


def special_cases():
    base = {"width": 1, "height": 1, "positionHeights": [17, 51, 100, -23], "normalHeights": [0] * 4, "bakedShadows": [255] * 4}
    quad = [(0, 0), (512, 0), (512, 512), (0, 0), (512, 512), (0, 512)]
    materials = [{"id": 5, "alpha": 255, "brightness": 0, "effect": 0}, {"id": 16383, "alpha": 128, "brightness": 64, "effect": 4},
        {"id": 0, "alpha": 0, "brightness": 255, "effect": 1}]
    result = []
    result.append(dict(base, name="special-owner-replacement", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 6, "hasShadows": True,
        "vertices": [[ox, oz, 12000, 12000, [5, 16383][i // 3], [256, 4][i // 3]] for i, (ox, oz) in enumerate(quad)]}]))
    result.append(dict(base, name="special-all-colorless", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 6, "hasShadows": False,
        "vertices": [[ox, oz, -1, -1, [0, 16383][i // 3], 764] for i, (ox, oz) in enumerate(quad)]}]))
    result.append(dict(base, name="special-colorless-onto-colored", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 6, "hasShadows": True,
        "vertices": [[ox, oz, 65535 if i < 3 else -1, 65535 if i < 3 else 7, [5, -1][i // 3], 0] for i, (ox, oz) in enumerate(quad)]}]))
    result.append(dict(base, name="special-mixed-face", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 6, "hasShadows": True,
        "vertices": [[ox, oz, -1 if i in (1, 4) else 31000, 31000, [16383, 0][i % 2], 65535] for i, (ox, oz) in enumerate(quad)]}]))
    result.append(dict(base, name="special-packed-key", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 36, "hasShadows": True,
        "vertices": [[i % 5 * 128, i % 3 * 256, 12000, 31000, [5, 16383, -1][i % 3], [0, 65532][i % 2]] for i in range(36)]}]))
    result.append(dict(base, name="special-no-blend-array", materials=materials, tiles=[{"x": 0, "z": 0, "emittedVertices": 6, "hasShadows": False,
        "hasBlendingColors": False, "vertices": [[ox, oz, 127 + i, 0, 16383, 1028] for i, (ox, oz) in enumerate(quad)]}]))
    return result


def topology_cases(rng):
    path = ROOT / "scene/tests/fixtures/ground-geometry.json"
    fixture = json.loads(path.read_text())
    assert fixture["jar_sha256"] == PINNED
    result = []
    skipped = {"secondary": 0, "colors": 0, "scales": 0, "untextured": 0, "collision": 0}
    for case in fixture["cases"]:
        source = case["input"]
        calls = case["calls"]
        if source["heights_current"] is not None or source["heights_next"] is not None or not calls:
            skipped["secondary"] += 1
            continue
        textures = [v for call in calls for v in call["textures"]]
        if not any(v >= 0 for v in textures):
            skipped["untextured"] += 1
            continue
        if any(call["draw_heights_a"] is not None or call["draw_heights_b"] is not None for call in calls):
            skipped["secondary"] += 1
            continue
        colors = [v for call in calls for v in call["main_colors"] + (call["blending_colors"] or [])]
        scales = [v for call in calls for v in call["texture_scales"]]
        if any(not (v == -1 or 0 <= v <= 65535) for v in colors) or any(not (v == -1 or 0 <= v <= 16383) for v in textures):
            skipped["colors"] += 1
            continue
        if any(not 0 <= v <= 65535 for v in scales):
            skipped["scales"] += 1
            continue
        colorless = any(v == -1 for call in calls for v in call["main_colors"])
        risky = any(main == 65535 and blend in (65535, -1) for call in calls
            for main, blend in zip(call["main_colors"], call["blending_colors"] or call["main_colors"]))
        if colorless and risky:
            skipped["collision"] += 1
            continue
        width, height = case["width"], case["height"]
        position = [v for row in source["heights"] for v in row]
        assert len(position) == (width + 1) * (height + 1)
        materials = []
        for material_id in sorted({v for v in textures if v >= 0}):
            materials.append({"id": material_id, "alpha": rng.choice([0, 255, 128, rng.randrange(256)]),
                "brightness": rng.choice([0, 0, 255, rng.randrange(256)]), "effect": rng.choice([0, 4, rng.randrange(-128, 128)])})
        tiles = []
        for call in calls:
            blend = call["blending_colors"] or call["main_colors"]
            vertices = [list(v) for v in zip(call["offsets_x"], call["offsets_z"], call["main_colors"], blend, call["textures"], call["texture_scales"])]
            tiles.append({"x": call["x"], "z": call["z"], "emittedVertices": len(vertices), "hasShadows": call["shadows"],
                "shape": call["effective_shape"], "rotation": call["effective_rotation"], "hasBlendingColors": call["blending_colors"] is not None, "vertices": vertices})
        result.append({"name": "topology-" + case["name"], "topologyOracleCase": case["name"], "width": width, "height": height, "materials": materials,
            "positionHeights": position, "normalHeights": position, "bakedShadows": [0] * len(position), "tiles": tiles})
    print("topology textured cases", len(result), "skipped", skipped)
    return result


def write_input(cases, path):
    lines = []
    for case in cases:
        lines.append(f"case\t{case['name']}\t{case['width']}\t{case['height']}")
        for key, label in [("positionHeights", "position"), ("normalHeights", "normal"), ("bakedShadows", "shadow")]:
            lines.append(label + "\t" + "\t".join(map(str, case[key])))
        for material in case["materials"]:
            lines.append(f"material\t{material['id']}\t{material['alpha']}\t{material['brightness']}\t{material['effect']}")
        for tile in case["tiles"]:
            lines.append(f"tile\t{tile['x']}\t{tile['z']}\t{tile['emittedVertices']}\t{int(tile['hasShadows'])}\t{int(tile.get('hasBlendingColors', True))}")
            for v in tile["vertices"]:
                lines.append("vertex\t" + "\t".join(map(str, v)))
        lines.append("end")
    path.write_text("\n".join(lines) + "\n")


def reference_selection(case, compiled, material, mode):
    width, height = case["width"], case["height"]
    visible = [mode == 0 or (mode == 1 and i % 3 == 1) or (mode == 2 and i % 5 == 0) for i in range(width * height)]
    indices = []
    chunks_x = (width + 7) // 8
    chunks_z = (height + 7) // 8
    masks = compiled["materials"][material]["masks"]
    for chunk in range(chunks_x * chunks_z):
        for tile, tri in enumerate(compiled["indices"]):
            if not visible[tile] or tri is None:
                continue
            if (tile % width) // 8 + ((tile // width) // 8) * chunks_x != chunk:
                continue
            mask = masks[tile]
            for face in range(len(tri) // 3):
                if mask & (1 << face):
                    indices.extend(tri[face * 3:face * 3 + 3])
    low, high = (min(indices), max(indices)) if indices else (0, -1)
    source = bytes.fromhex(compiled["vertexBytes"])
    colors = bytes.fromhex(compiled["materials"][material]["materialBytes"])
    canonical = bytearray()
    for index in range(low, high + 1):
        base = index * 36
        canonical.extend(source[base:base + 12])
        canonical.extend(colors[index * 4:index * 4 + 4])
        canonical.extend(source[base + 16:base + 24])
        canonical.extend(bytes(4))
        canonical.extend(source[base + 24:base + 36])
    return {"material": material, "visibleTiles": visible, "sourceFirstVertex": low, "vertexBytes": canonical.hex(), "indices": [index - low for index in indices]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native-library", type=Path, required=True)
    parser.add_argument("--work", type=Path, default=ROOT / "harness/target/loop/iteration17-validation/ground-lowering")
    parser.add_argument("--fixture", type=Path, default=ROOT / "scene/tests/fixtures/ground-lowering-textured.json")
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    jar = args.work / "oracle-client.jar"
    native = args.work / "libjaclib.so"
    private_copy(args.jar, jar, PINNED)
    private_copy(args.native_library, native, LIBRARY)
    cases = generate()
    input_path = args.work / "ground-lowering-textured-input.tsv"
    output_path = args.work / "ground-lowering-textured-native.json"
    write_input(cases, input_path)
    source = Path(__file__).with_name("GroundLoweringTexturedOracle.java")
    subprocess.run(["javac", "-cp", str(jar), "-d", str(args.work), str(source)], check=True)
    subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true", "-cp", f"{args.work}:{jar}", "GroundLoweringTexturedOracle", str(jar), str(native), str(input_path), str(output_path)], check=True)
    native_output = json.loads(output_path.read_text())
    assert len(native_output["cases"]) == len(cases)
    selections = 0
    for case, compiled in zip(cases, native_output["cases"]):
        assert case["name"] == compiled["name"]
        expected_keys = {(v[4], v[5]) for tile in case["tiles"] for v in tile["vertices"]}
        actual = [(m["materialId"], m["textureScale"]) for m in compiled["materials"]]
        assert len(actual) == len(set(actual)) == len(expected_keys) and set(actual) == expected_keys, case["name"]
        for material in compiled["materials"]:
            assert material["water"] == DEFAULT_SURFACE_WATER, case["name"]
            assert material["pointer"] == material_uid(material["materialId"], material["textureScale"]), case["name"]
        assert [m["pointer"] for m in compiled["materials"]] == sorted(m["pointer"] for m in compiled["materials"]), case["name"]
        case["compiled"] = compiled
        modes = range(4) if "topologyOracleCase" not in case else [0, 2]
        case["selections"] = [reference_selection(case, compiled, material, mode) for material in range(len(compiled["materials"])) for mode in modes]
        selections += len(case["selections"])
    native_output["cases"] = cases
    native_output["selectionSource"] = "source-contract reference over actual compiled buffers; not a GL draw"
    native_output["javaSourceSha256"] = digest(source)
    native_output["inputSha256"] = digest(input_path)
    for original, copy, expected in [(args.jar, jar, PINNED), (args.native_library, native, LIBRARY)]:
        assert digest(original) == digest(copy) == expected
    args.fixture.write_text(json.dumps(native_output, separators=(",", ":")) + "\n")
    evidence = {"jarSha256": PINNED, "nativeLibrarySha256": LIBRARY, "sourceSha256": digest(source), "fixtureSha256": digest(args.fixture),
        "cases": len(cases), "selections": selections, "materials": sum(c["compiled"]["materialCount"] for c in cases),
        "materialLookups": sum(c["compiled"]["materialLookups"] for c in cases),
        "allocatedVertices": sum(c["compiled"]["allocatedVertices"] for c in cases), "uniqueVertices": sum(c["compiled"]["vertexCount"] for c in cases),
        "originalsUnchanged": True, "glDrawExecuted": False, "materialProvider": "stub installed on AbstractToolkit.materialProvider; only getMaterial is reachable",
        "unwrittenNativeColorBytes": "NativeHeap.allocateBuffer memsets every buffer (objdump of libjaclib.so) and ReuseProbe/MallocProbe/ZeroProbe/PoisonProbe under probe/ observed only zero bytes in fresh buffers, so unwritten material color entries compare as [0,0,0,0]"}
    (args.work / "oracle-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(f"WROTE {args.fixture} {len(cases)} unchanged GroundGL textured cases, {selections} selections")


if __name__ == "__main__":
    main()
