import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


EXPECTED_JAR = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def inspect_fixture(actual):
    cases = {}
    regions = {}
    samples = {}
    sizes = []
    values = [set() for _ in range(5)]
    zero_overlay_with_shape_rotation = 0
    provenance = 0
    capture = 0
    for line in actual.decode().splitlines():
        fields = line.split("\t")
        kind = fields[0]
        if kind == "jar-sha256":
            assert fields == [kind, EXPECTED_JAR]
            provenance += 1
        elif kind == "capture":
            assert fields == [kind, "MapLoader.decodeTilesClient", "pre-bridge", "plane/x/y", "underlay/overlay/render_flags/shape/rotation"]
            capture += 1
        elif kind == "map-size":
            assert len(fields) == 3 and int(fields[1]) == len(sizes)
            sizes.append(int(fields[2]))
        elif kind == "case":
            assert len(fields) == 7
            case_id, size, bx, by, missing = map(int, fields[1:6])
            assert case_id not in cases and len(fields[6]) == 16
            int(fields[6], 16)
            assert size in sizes and missing in (0, 1, 2)
            assert bx % 8 == by % 8 == 0
            cases[case_id] = (size, bx, by, missing)
        elif kind == "region":
            assert len(fields) == 5
            case_id, index, region = map(int, fields[1:4])
            entries = regions.setdefault(case_id, [])
            assert index == len(entries) and fields[4] in ("present", "absent")
            entries.append((region, fields[4]))
        elif kind == "sample":
            assert len(fields) == 10
            case_id, plane, x, y = map(int, fields[1:5])
            attributes = list(map(int, fields[5:]))
            assert 0 <= plane < 4
            assert all(0 <= value <= limit for value, limit in zip(attributes, (174, 255, 32, 11, 3)))
            samples.setdefault(case_id, []).append((plane, x, y, attributes))
            for observed, value in zip(values, attributes):
                observed.add(value)
            if attributes[1] == 0 and attributes[3] != 0 and attributes[4] != 0:
                zero_overlay_with_shape_rotation += 1
        else:
            raise RuntimeError(f"unexpected fixture record {kind}")
    assert provenance == capture == 1
    assert sizes == [104, 120, 136, 168, 72]
    assert set(cases) == set(regions) == set(samples) == set(range(130))
    for case_id, (size, bx, by, missing) in cases.items():
        half = size >> 4
        cx, cy = bx // 8 + half, by // 8 + half
        expected = [
            ((rx << 8) + ry, "absent" if missing == 2 or missing == 1 and index % 3 == 1 else "present")
            for index, (rx, ry) in enumerate(
                (rx, ry)
                for rx in range((cx - half) // 8, (cx + half) // 8 + 1)
                for ry in range((cy - half) // 8, (cy + half) // 8 + 1)
            )
        ]
        assert regions[case_id] == expected
        assert len(samples[case_id]) == 80
        assert {plane for plane, _, _, _ in samples[case_id]} == set(range(4))
        for _, x, y, _ in samples[case_id]:
            assert 0 <= x < size and 0 <= y < size
    for size in sizes:
        matching = [case for case in cases.values() if case[0] == size]
        assert len(matching) == 26
        for dx in (0, 8, 24, 56):
            for dy in (0, 8, 24, 56):
                assert (size, 3200 + dx, 3200 + dy, 0) in matching
        assert (size, 0, 0, 0) in matching
        assert (size, 16384 - size, 16384 - size, 0) in matching
        assert {case[3] for case in matching} == {0, 1, 2}
    assert values[0] == set(range(175))
    assert values[1] == set(range(256))
    assert values[2] == set(range(33))
    assert values[3] == set(range(12))
    assert values[4] == set(range(4))
    assert zero_overlay_with_shape_rotation > 0
    return {
        "cases": len(cases),
        "completeCases": sum(case[3] == 0 for case in cases.values()),
        "mixedAbsentCases": sum(case[3] == 1 for case in cases.values()),
        "allAbsentCases": sum(case[3] == 2 for case in cases.values()),
        "hashedTiles": sum(4 * case[0] ** 2 for case in cases.values()),
        "hashedAttributeBytes": sum(4 * case[0] ** 2 * 5 for case in cases.values()),
        "samples": sum(map(len, samples.values())),
        "sampleAttributeCardinalities": list(map(len, values)),
        "zeroOverlayWithNonzeroShapeAndRotationSamples": zero_overlay_with_shape_rotation,
        "nativeSizes": sizes,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--evidence-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    original = args.jar.resolve(strict=True)
    checksum = sha256(original)
    if checksum != EXPECTED_JAR:
        raise RuntimeError(f"unexpected source jar SHA-256 {checksum}")
    fixture = root / "tests" / "fixtures" / "surface-tiles.tsv"
    with tempfile.TemporaryDirectory(prefix="openrs-surface-tiles-oracle-") as temporary:
        work = Path(temporary)
        copied = work / "client.jar"
        shutil.copyfile(original, copied)
        copied.chmod(0o444)
        if sha256(copied) != checksum:
            raise RuntimeError("oracle jar copy differs from the original")
        output = work / "surface-tiles.tsv"
        command = [
            "jshell",
            "--execution", "local",
            "--feedback", "silent",
            "--class-path", str(copied),
            "-J--add-modules=jdk.unsupported",
            "-J-Xmx512m",
            "-J-Djava.awt.headless=true",
            f"-J-Djava.util.prefs.userRoot={work / 'prefs'}",
            f"-J-Doracle.jar={copied}",
            f"-J-Doracle.output={output}",
            str(root / "tools" / "surface-tiles-oracle.jsh"),
        ]
        result = subprocess.run(
            command, cwd=work, stdin=subprocess.DEVNULL,
            capture_output=True, text=True, timeout=240, check=False,
        )
        if args.evidence_dir is not None:
            args.evidence_dir.mkdir(parents=True, exist_ok=True)
            (args.evidence_dir / "surface-tiles-native-oracle.log").write_text(result.stdout + result.stderr)
        if result.returncode != 0 or "SURFACE_TILES_NATIVE_ORACLE_OK" not in result.stdout or not output.exists():
            raise RuntimeError((result.stdout + result.stderr)[-8000:])
        if sha256(original) != checksum or sha256(copied) != checksum:
            raise RuntimeError("an oracle jar changed during evaluation")
        actual = output.read_bytes()
    coverage = inspect_fixture(actual)
    if args.update:
        fixture.write_bytes(actual)
    elif fixture.read_bytes() != actual:
        raise RuntimeError("native surface tiles oracle differs from the checked-in fixture")
    summary = {
        "status": "matched",
        "rows": len(actual.splitlines()),
        "sourceJar": str(original),
        "sourceJarSha256": checksum,
        "fixtureSha256": hashlib.sha256(actual).hexdigest(),
        "jarUnchanged": True,
        "nativeClassesUnmodified": True,
        "nativeMethod": "MapLoader.decodeTilesClient",
        "captureStage": "immediately after decode, before bridge or floor processing",
        "attributeOrder": ["underlay_id", "overlay_id", "render_flags", "overlay_shape", "overlay_rotation"],
        "tileOrder": "plane/x/y-major",
        "hash": "FNV-1a-64 over all five raw attribute bytes of every scene tile",
        "meshClaim": False,
        **coverage,
    }
    if args.evidence_dir is not None:
        (args.evidence_dir / "surface-tiles-native-oracle.json").write_text(json.dumps(summary, indent=2) + "\n")
        (args.evidence_dir / "surface-tiles-native.tsv").write_bytes(actual)
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
        print(f"surface tiles native oracle failed: {error}", file=sys.stderr)
        sys.exit(1)
