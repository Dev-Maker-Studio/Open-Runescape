import argparse
import collections
import hashlib
import json
from pathlib import Path
import random
import shutil
import subprocess
import sys
import tempfile


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAR = ROOT / "target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
EVIDENCE = ROOT.parent / "harness/target/loop/iteration15-validation/ground"


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def overlay(identifier, tile, minimap, texture, scale, slot, blend, shadowed, occlude=True):
    return dict(id=identifier, tile_color=tile, minimap_color=minimap, texture=texture,
                scale=scale, slot=slot, blend=blend, shadowed=shadowed, occlude=occlude)


def definitions():
    return dict(
        overlays=[
            overlay(1, 0x4A37, 0x4A37, -1, 512, 2048, False, True),
            overlay(2, 0x2137, 0x7B6E, 17, 764, 2049, True, True),
            overlay(3, 0xF78A, -1, 18, 4, 2306, True, True),
            overlay(4, -1, -1, -1, 512, 2051, True, True),
            overlay(5, -1, 0x1234, -1, 256, 2052, True, True),
            overlay(6, 0, -1, 19, 65532, 2053, False, False),
            overlay(7, 0xFFFF, -1, -1, 65532, 2049, True, False, False),
            overlay(8, 0x7FFF, 0x7FFF, 20, 4, 2311, False, False),
        ],
        underlays=[
            dict(id=1, texture=-1, scale=512, shadowed=True, occlude=True),
            dict(id=2, texture=23, scale=1028, shadowed=False, occlude=True),
            dict(id=3, texture=24, scale=65532, shadowed=True, occlude=False),
            dict(id=128, texture=-1, scale=4, shadowed=False, occlude=False),
            dict(id=255, texture=25, scale=256, shadowed=True, occlude=True),
        ],
        materials=[
            dict(id=17, color_hsl=0xFFFF, details_only=False),
            dict(id=18, color_hsl=0x8123, details_only=False),
            dict(id=19, color_hsl=0x1357, details_only=True),
            dict(id=20, color_hsl=0x2468, details_only=False),
        ],
    )


def matrix(width, height, value=0):
    return [[value for _ in range(height)] for _ in range(width)]


def case(name, width=3, height=3, height_mode=0, seed=0):
    palettes = [0, 1, 127, 128, 1023, 1024, 0x1234, 0x7FFF, 0x8000, 0xFFFF, 0xFC00, 0x0380, 0x007F]
    heights = [[(x * 119 - z * 73 + x * z * 31 + seed * 17) % 2049 - 1024
                for z in range(height + 1)] for x in range(width + 1)]
    current = [[heights[x][z] + 211 + x * 13 - z * 29 for z in range(height + 1)]
               for x in range(width + 1)] if height_mode & 1 else None
    next_heights = [[heights[x][z] - 417 - x * 19 + z * 37 for z in range(height + 1)]
                    for x in range(width + 1)] if height_mode & 2 else None
    return dict(name=name, width=width, height=height, input=dict(
        heights=heights,
        palette_indices=[[palettes[(x * 5 + z * 3 + seed) % len(palettes)] for z in range(height)] for x in range(width)],
        underlay_ids=matrix(width, height), overlay_ids=matrix(width, height),
        shapes=matrix(width, height), rotations=matrix(width, height),
        heights_current=current, heights_next=next_heights, **definitions(),
    ))


def tile(entry, x, z, underlay=0, over=0, shape=0, rotation=0):
    values = entry["input"]
    for key, value in (("underlay_ids", underlay), ("overlay_ids", over), ("shapes", shape), ("rotations", rotation)):
        values[key][x][z] = value


def cases():
    result = []
    variants = [
        ("plain", 1, 1), ("blending", 1, 2), ("material", 2, 3),
        ("overlay-only", 0, 2), ("underlay-only", 3, 0),
        ("absent-overlay-colors", 1, 4), ("minimap-only", 2, 5),
        ("details-material", 2, 6), ("unshadowed", 2, 8), ("absent-underlay-and-colors", 0, 4),
    ]
    for shape in range(13):
        for rotation in range(4):
            for label, underlay, over in variants:
                entry = case(f"isolated-{shape}-{rotation}-{label}", seed=shape * 4 + rotation)
                tile(entry, 1, 1, underlay, over, shape, rotation)
                result.append(entry)
    for shape in (0, 12):
        for rotation in range(4):
            entry = case(f"boundary-{shape}-{rotation}", width=1, height=1)
            tile(entry, 0, 0, 1, 1 if shape == 0 else 0, shape, rotation)
            result.append(entry)
    for shape in (0, 12):
        for corner, (dx, dz) in enumerate(((-1, -1), (1, -1), (1, 1), (-1, 1))):
            entry = case(f"generated-{13 if shape == 0 else 14}-corner-{corner}", seed=corner)
            tile(entry, 1, 1, 1, 1 if shape == 0 else 0, shape)
            tile(entry, 1 + dx, 1 + dz, 0, 3)
            result.append(entry)
    for shape in range(13):
        for rotation in range(4):
            entry = case(f"split-{shape}-{rotation}", width=5, height=5, height_mode=(shape + rotation) % 4, seed=shape)
            for x in range(5):
                for z in range(5):
                    tile(entry, x, z, (1, 2, 3, 128, 255)[(x + z) % 5])
            tile(entry, 2, 2, 1, 1, shape, rotation)
            for x, z, over, neighbor_shape, neighbor_rotation in ((1, 1, 2, 0, 0), (3, 1, 7, 0, 0),
                (1, 3, 2, 0, 0), (3, 3, 3, 0, 0), (2, 1, 7, 2, 1), (2, 3, 2, 3, 3),
                (1, 2, 3, 7, 0), (3, 2, 2, 8, 2)):
                tile(entry, x, z, 2, over, neighbor_shape, neighbor_rotation)
            result.append(entry)
    for size in range(1, 6):
        entry = case(f"empty-{size}", width=size, height=size, height_mode=size % 4, seed=size)
        for x in range(size):
            for z in range(size):
                tile(entry, x, z, shape=(x * 3 + z) % 13, rotation=(x + z) % 4)
        result.append(entry)
    for seed in range(96):
        rng = random.Random(0x727B1E + seed)
        width, height = (1 + seed % 5, 1 + seed // 5 % 5)
        entry = case(f"mixed-{seed}", width, height, seed % 4, seed)
        for x in range(width):
            for z in range(height):
                tile(entry, x, z, rng.choice((0, 1, 2, 3, 128, 255)), rng.randrange(9), rng.randrange(13), rng.randrange(4))
        result.append(entry)
    for seed in range(16):
        entry = case(f"priority-tie-{seed}", width=5, height=5, height_mode=seed % 4, seed=seed)
        for x in range(5):
            for z in range(5):
                tile(entry, x, z, 1, (2, 7)[(x + z + seed) % 2], (x * 3 + z + seed) % 13, (x + z + seed) % 4)
        result.append(entry)
    for source in ("underlay-votes", "height-diagonal", "height-tie", "signed-underlay"):
        for outcome in range(2):
            entry = case(f"rotation-{source}-{outcome}", width=3, height=3, seed=outcome)
            for x in range(3):
                for z in range(3):
                    tile(entry, x, z, 128 if source == "signed-underlay" else 1)
            if source == "underlay-votes":
                tile(entry, 0 if outcome == 0 else 2, 0, 2)
                tile(entry, 2 if outcome == 0 else 0, 2, 2)
            entry["input"]["heights"] = matrix(4, 4)
            if source != "height-tie":
                entry["input"]["heights"][2][1 if outcome == 0 else 2] = 300
            result.append(entry)
    return result


def write_input(entries, path):
    lines = []
    for entry in entries:
        data = entry["input"]
        lines.append(["case", entry["name"], entry["width"], entry["height"], int(data["heights_current"] is not None), int(data["heights_next"] is not None)])
        for item in data["overlays"]:
            lines.append(["overlay"] + [item[key] for key in ("id", "tile_color", "minimap_color", "texture", "scale", "slot", "blend", "shadowed", "occlude")])
        for item in data["underlays"]:
            lines.append(["underlay"] + [item[key] for key in ("id", "texture", "scale", "shadowed", "occlude")])
        for item in data["materials"]:
            lines.append(["material"] + [item[key] for key in ("id", "color_hsl", "details_only")])
        for x in range(entry["width"]):
            for z in range(entry["height"]):
                lines.append(["tile", x, z] + [data[key][x][z] for key in ("underlay_ids", "overlay_ids", "shapes", "rotations", "palette_indices")])
        for x in range(entry["width"] + 1):
            for z in range(entry["height"] + 1):
                lines.append(["height", x, z, data["heights"][x][z], 0 if data["heights_current"] is None else data["heights_current"][x][z], 0 if data["heights_next"] is None else data["heights_next"][x][z]])
        lines.append(["end"])
    path.write_text("".join("\t".join(str(int(value)) if isinstance(value, bool) else str(value) for value in line) + "\n" for line in lines))


def inspect(entries):
    effective = collections.Counter()
    shape_rotations = set()
    heights = collections.Counter()
    shadows = collections.Counter()
    blending = collections.Counter()
    internal = collections.Counter()
    split_calls = 0
    tie_calls = 0
    no_height_pairs = set()
    no_height_cases = 0
    no_height_calls = 0
    triangles = 0
    calls = 0
    for entry in entries:
        assert 1 <= entry["width"] <= 5 and 1 <= entry["height"] <= 5
        data = entry["input"]
        no_heights = data["heights_current"] is None and data["heights_next"] is None
        no_height_cases += no_heights
        assert entry["loaded_tiles"] == [[x, z] for x in range(entry["width"]) for z in range(entry["height"])
                                          if data["underlay_ids"][x][z] or data["overlay_ids"][x][z]]
        assert len(entry["calls"]) <= entry["width"] * entry["height"]
        assert [(v["x"], v["z"]) for v in entry["calls"]] == sorted((v["x"], v["z"]) for v in entry["calls"])
        for x in range(entry["width"]):
            for z in range(entry["height"]):
                if data["overlay_ids"][x][z] or data["underlay_ids"][x][z]:
                    shape_rotations.add((data["shapes"][x][z], data["rotations"][x][z]))
        for call in entry["calls"]:
            count = len(call["offsets_x"])
            assert count > 0 and count % 3 == 0
            for key in ("offsets_z", "main_colors", "textures", "texture_scales"):
                assert len(call[key]) == count
            for key in ("draw_heights_a", "draw_heights_b", "blending_colors"):
                assert call[key] is None or len(call[key]) == count
            assert all(0 <= value <= 512 for key in ("offsets_x", "offsets_z") for value in call[key])
            assert (call["draw_heights_a"] is not None) == (data["heights_next"] is not None)
            assert (call["draw_heights_b"] is not None) == (data["heights_next"] is not None or data["heights_current"] is not None)
            effective[(call["effective_shape"], call["effective_rotation"])] += 1
            assert len(call["blend_priorities"]) == len(call["blend_function_masks"]) == 13
            if any((mask & ~1).bit_count() > 1 for mask in call["blend_function_masks"]):
                tie_calls += 1
            if no_heights:
                no_height_pairs.add((call["effective_shape"], call["effective_rotation"]))
                no_height_calls += 1
            shadows[call["shadows"]] += 1
            blending[call["blending_colors"] is not None] += 1
            heights[(call["draw_heights_a"] is not None, call["draw_heights_b"] is not None)] += 1
            if call["effective_shape"] in (13, 14):
                assert data["shapes"][call["x"]][call["z"]] in (0, 12)
                internal[(call["effective_shape"], call["effective_rotation"])] += 1
            if not call["unblendable"] and not call["overlay_supports_blending"]:
                split_calls += 1
            calls += 1
            triangles += count // 3
    assert shape_rotations == {(shape, rotation) for shape in range(13) for rotation in range(4)}
    assert set(internal) == {(shape, rotation) for shape in (13, 14) for rotation in range(4)}
    assert all((shape, rotation) in effective for shape in range(1, 12) for rotation in range(4))
    assert set(shadows) == set(blending) == {False, True}
    assert set(heights) == {(False, False), (False, True), (True, True)}
    assert split_calls > 0 and tie_calls > 0
    assert no_height_pairs == {(shape, rotation) for shape in range(15) for rotation in range(4)}
    return dict(cases=len(entries), calls=calls, triangles=triangles, vertices=triangles * 3,
                native_priority_tie_mask_calls=tie_calls, no_secondary_height_cases=no_height_cases,
                no_secondary_height_calls=no_height_calls, no_secondary_height_effective_pairs=len(no_height_pairs),
                input_shape_rotation_pairs=len(shape_rotations),
                effective_shape_rotation_counts={f"{shape}/{rotation}": value for (shape, rotation), value in sorted(effective.items())},
                internally_generated_shape_rotation_counts={f"{shape}/{rotation}": value for (shape, rotation), value in sorted(internal.items())},
                adjacent_split_branch_calls=split_calls,
                shadow_counts={str(key).lower(): value for key, value in shadows.items()},
                blending_array_counts={str(key).lower(): value for key, value in blending.items()},
                height_array_counts={f"{a}/{b}": value for (a, b), value in heights.items()})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path, nargs="?", default=DEFAULT_JAR)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--evidence-dir", type=Path, default=EVIDENCE)
    args = parser.parse_args()
    original = args.jar.resolve(strict=True)
    checksum = sha256(original)
    if checksum != JAR_SHA256:
        raise RuntimeError(f"unexpected source jar SHA-256 {checksum}")
    evidence = args.evidence_dir.resolve()
    if evidence != EVIDENCE.resolve():
        raise RuntimeError(f"evidence is restricted to {EVIDENCE}")
    evidence.mkdir(parents=True, exist_ok=True)
    entries = cases()
    with tempfile.TemporaryDirectory(prefix="ground-oracle-", dir=evidence) as temporary:
        work = Path(temporary)
        copied = work / "client.jar"
        shutil.copyfile(original, copied)
        copied.chmod(0o444)
        if sha256(copied) != checksum:
            raise RuntimeError("copied jar checksum mismatch")
        source = ROOT / "tools/GroundGeometryOracle.java"
        build = subprocess.run(["javac", "--release", "17", "-cp", str(copied), "-d", str(work), str(source)],
                               cwd=work, capture_output=True, text=True, timeout=120, check=False)
        (evidence / "ground-geometry-javac.log").write_text(build.stdout + build.stderr)
        if build.returncode != 0:
            raise RuntimeError(build.stdout + build.stderr)
        input_path = work / "input.tsv"
        output_path = work / "output.jsonl"
        write_input(entries, input_path)
        run = subprocess.run(["java", "-Xmx512m", "-Djava.awt.headless=true", "-cp", str(work) + ":" + str(copied),
                              "GroundGeometryOracle", str(copied), str(input_path), str(output_path)],
                             cwd=work, capture_output=True, text=True, timeout=240, check=False)
        (evidence / "ground-geometry-native.log").write_text(run.stdout + run.stderr)
        if run.returncode != 0 or "GROUND_GEOMETRY_NATIVE_ORACLE_OK" not in run.stdout:
            raise RuntimeError(run.stdout + run.stderr)
        if sha256(original) != checksum or sha256(copied) != checksum:
            raise RuntimeError("an oracle jar changed")
        outputs = [json.loads(line) for line in output_path.read_text().splitlines()]
        if len(outputs) != len(entries):
            raise RuntimeError("native case count mismatch")
        for entry, output in zip(entries, outputs):
            if entry["name"] != output["name"]:
                raise RuntimeError("native case order mismatch")
            for definition, rendered in zip(entry["input"]["overlays"], output["rendered_colors"], strict=True):
                definition["rendered_color"] = rendered
            entry["calls"] = output["calls"]
            entry["loaded_tiles"] = output["loaded_tiles"]
        (evidence / "ground-geometry-input.tsv").write_bytes(input_path.read_bytes())
        (evidence / "ground-geometry-native.jsonl").write_bytes(output_path.read_bytes())
    coverage = inspect(entries)
    fixture = dict(schema=1, jar_sha256=checksum, capture="original MapLoader.addBlendedTiles -> recording Ground.addBlendedTile",
                   order="cases then x/z-major calls; vertex arrays preserve native order; each consecutive triple is a triangle",
                   cases=entries)
    actual = (json.dumps(fixture, separators=(",", ":"), ensure_ascii=True) + "\n").encode()
    fixture_path = ROOT / "tests/fixtures/ground-geometry.json"
    if args.update:
        fixture_path.write_bytes(actual)
    elif fixture_path.read_bytes() != actual:
        raise RuntimeError("native ground geometry differs from checked-in fixture")
    (evidence / "ground-geometry-native.json").write_bytes(actual)
    summary = dict(status="matched", source_jar=str(original), source_jar_sha256=checksum,
                   fixture=str(fixture_path), fixture_sha256=hashlib.sha256(actual).hexdigest(),
                   fixture_bytes=len(actual), jar_unchanged=True, native_classes_unmodified=True,
                   native_method="MapLoader.addBlendedTiles", toolkit="Unsafe-allocated SafeModeToolkit; mocked MaterialProvider",
                   native_height_sampling="Ground.getAverageHeight", plane=0,
                   limitations=["Synthetic bounded maps only; no JS5 decode, palette smoothing, bridge processing, GL upload, or rasterization claim",
                                "Plane-zero geometry only; higher-plane culling and water metadata are outside this fixture",
                                "Overlay slots are explicit postDecode priorities; synthetic equal priorities may use different definitions"],
                   **coverage)
    (evidence / "ground-geometry-oracle.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
        print(f"ground geometry native oracle failed: {error}", file=sys.stderr)
        sys.exit(1)
