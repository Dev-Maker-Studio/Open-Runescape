import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile


SEEDS = [0, 42, 123456789, 987654321, 25214903917, 142379987759401]
BOUNDARIES = ["0000000000000000", "3fefffffffffffff"]
COLOR_PATH = "com/jagex/game/runetek5/utils/ColorUtil.class"
SOURCE_PATH = "src/main/kotlin/com/jagex/game/runetek5/utils/ColorUtil.kt"


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def checked(command, work):
    result = subprocess.run(
        command, cwd=work, stdin=subprocess.DEVNULL,
        capture_output=True, text=True, timeout=120, check=False,
    )
    if result.returncode:
        raise RuntimeError((result.stdout + result.stderr)[-12000:])
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    work = (args.work_dir or root.parent / "harness/target/loop/iteration14-validation/rgb").resolve()
    work.mkdir(parents=True, exist_ok=True)
    jar = args.jar.resolve(strict=True)
    checksum = sha256(jar)
    oracle = root / "tools/RgbOracle.java"
    source = jar.parents[2] / SOURCE_PATH
    current_source = root.parent / "client" / SOURCE_PATH
    with zipfile.ZipFile(jar) as archive:
        class_checksum = hashlib.sha256(archive.read(COLOR_PATH)).hexdigest()
    classes = work / "rgb-classes"
    classes.mkdir(exist_ok=True)
    checked([
        "javac", "--release", "17", "--add-modules", "jdk.unsupported", "-cp", str(jar),
        "-d", str(classes), str(oracle),
    ], work)
    rows = [
        "format\trgb-native-v1",
        f"jar-sha256\t{checksum}",
        f"native-source-sha256\t{sha256(source)}",
        f"current-source-sha256\t{sha256(current_source)}",
        f"oracle-source-sha256\t{sha256(oracle)}",
        f"class-sha256\t{class_checksum}",
        "all-native-classes-unchanged\ttrue",
        "boundary-random-holder-seam\tUnsafe replaces Math$RandomNumberGeneratorHolder.randomNumberGenerator only",
    ]
    fixture_dir = root / "tests/fixtures"
    cases = [(f"seed-{index}", "seed", str(seed)) for index, seed in enumerate(SEEDS)]
    cases += [(f"boundary-{index}", "boundary", value) for index, value in enumerate(BOUNDARIES)]
    outputs = []
    canonical_checksum = None
    for case_id, mode, parameter in cases:
        filename = f"rgb-{case_id}.bin"
        binary = work / filename
        metadata = work / f"rgb-{case_id}.tsv"
        stdout = checked([
            "java", *(["-Xint"] if mode == "boundary" else []),
            "--add-modules", "jdk.unsupported",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-Djava.awt.headless=true", "-cp", os.pathsep.join((str(classes), str(jar))),
            "RgbOracle", str(jar), mode, parameter, str(binary), str(metadata),
        ], work)
        if "NATIVE_RGB_ORACLE_OK" not in stdout:
            raise RuntimeError("native RGB oracle did not finish")
        values = dict(line.split("\t", 1) for line in metadata.read_text().splitlines())
        if values["jar-sha256"] != checksum or values["class-sha256"] != class_checksum:
            raise RuntimeError("native oracle loaded a different jar or ColorUtil class")
        if values["allocation-random-calls"] != "1" or values["table-entries"] != "131072":
            raise RuntimeError("native RGB allocation policy differs")
        if values["native-class-unchanged"] != "true":
            raise RuntimeError("RGB oracle changed the native class")
        if mode == "boundary" and (
            values["interpreted-jvm"] != "true"
            or values["random-holder-seam"] != "Unsafe replaces Math$RandomNumberGeneratorHolder.randomNumberGenerator only"
        ):
            raise RuntimeError("boundary oracle did not use the isolated interpreted RNG holder seam")
        if canonical_checksum is None:
            canonical_checksum = values["canonical-class-sha256"]
            rows.append(f"canonical-class-sha256\t{canonical_checksum}")
        if values["canonical-class-sha256"] != canonical_checksum:
            raise RuntimeError("native ColorUtil changed between fresh oracle JVMs")
        if binary.stat().st_size != 131072 * 4 or sha256(binary) != values["table-sha256"]:
            raise RuntimeError("native RGB binary capture differs from metadata")
        rows.append("\t".join((
            "case", case_id, mode, parameter, values["random-unit-bits"],
            values["gamma-bits"], filename, values["table-sha256"],
        )))
        outputs.append((filename, binary.read_bytes()))
        (work / f"rgb-{case_id}.stdout").write_text(stdout)
    if sha256(jar) != checksum:
        raise RuntimeError("native oracle jar changed during evaluation")
    manifest = ("\n".join(rows) + "\n").encode()
    outputs.append(("rgb.tsv", manifest))
    for filename, actual in outputs:
        fixture = fixture_dir / filename
        if args.update:
            fixture.write_bytes(actual)
        elif fixture.read_bytes() != actual:
            raise RuntimeError(f"native RGB oracle differs from fixture {filename}")
    evidence = {
        "status": "passed",
        "jar": str(jar),
        "jar_sha256_before": checksum,
        "jar_sha256_after": sha256(jar),
        "native_source": str(source),
        "native_source_sha256": sha256(source),
        "current_source_sha256": sha256(current_source),
        "oracle_source_sha256": sha256(oracle),
        "class_sha256": class_checksum,
        "canonical_class_sha256": canonical_checksum,
        "fresh_jvms": len(cases),
        "untouched_seeded_jvms": len(SEEDS),
        "unchanged_native_class_jvms": len(cases),
        "rng_holder_only_boundary_jvms": len(BOUNDARIES),
        "boundary_seam": "Unsafe replaces Math$RandomNumberGeneratorHolder.randomNumberGenerator only in fresh -Xint JVMs",
        "entries_per_case": 131072,
        "compared_native_entries": len(cases) * 131072,
        "byte_order": "little-endian u32: all hslToRgb, then all unsignedHslToRgb",
        "hsv": "independently randomized HSV LUT is out of scope",
        "pixel_equivalence": "not claimed; only HSL RGB dependency tested",
    }
    (work / "rgb-oracle.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(f"Native RGB oracle matched {len(cases) * 131072} entries from {len(cases)} fresh JVMs; unchanged jar SHA-256 {checksum}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
