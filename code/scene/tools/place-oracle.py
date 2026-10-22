import argparse
import importlib.util
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIXTURES = HERE.parent / "tests" / "fixtures"
LIBRARY = Path(__file__).resolve().parents[2] / "harness/target/loop/mesh-oracle/libjaclib.so"
LIBRARY_SHA = "92f32235d2a67e7debedd89fbccd2ac0e937302eaa7e0732fe0ba93072a08a74"
SCENES = {"a": (39 * 64 + 24, 50 * 64 + 24, 104), "b": (40 * 64, 48 * 64, 128), "c": (53 * 64 + 20, 53 * 64 + 20, 104)}
SCENE_NAMES = ["a", "b", "c"]


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


mesh = load("mesh_oracle", "mesh-oracle.py")
base = mesh
cache_tools = mesh.load_cache_tools()


def run_java(jar, work, mode, library):
    command = [
        "java",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-Djava.awt.headless=true",
        "-cp",
        f"{work}:{jar}",
        "PlaceOracle",
        str(jar),
        str(work),
        mode,
        "-",
        str(library),
    ]
    return subprocess.run(command, check=True, capture_output=True, text=True)


def regions_of(base_x, base_y, size):
    return [(x << 8) | y for x in range(base_x // 64, (base_x + size - 1) // 64 + 1) for y in range(base_y // 64, (base_y + size - 1) // 64 + 1)]


def scene_files():
    lines = []
    regions = set()
    for name, (bx, by, size) in SCENES.items():
        parts = []
        for region in regions_of(bx, by, size):
            if not (FIXTURES / f"shadow-l{region}.bin").exists():
                continue
            regions.add(region)
            parts.append(f"{region}:{region}:{(region >> 8) * 64 - bx}:{(region & 255) * 64 - by}")
        lines.append(f"{name} {size} {size} {bx} {by} {','.join(parts)}")
    return lines, sorted(regions)


def generate(jar_source, cache_path, work, library):
    work.mkdir(parents=True, exist_ok=True)
    if mesh.digest(library) != LIBRARY_SHA:
        raise ValueError("unexpected native library")
    jar = work / "oracle-client.jar"
    base.private_copy(jar_source, jar, base.PINNED)
    source = HERE / "PlaceOracle.java"
    subprocess.run(["javac", "-nowarn", "-cp", str(jar), "-d", str(work), str(source)], check=True, capture_output=True, text=True)
    lines, regions = scene_files()
    (work / "scenes.txt").write_text("\n".join(lines) + "\n")
    cache = cache_tools.Cache(cache_path)
    names = cache_tools.named_archives(cache)
    maps = []
    for region in regions:
        shutil.copyfile(FIXTURES / f"shadow-l{region}.bin", work / f"shadow-l{region}.bin")
        archive = names.get(cache_tools.name_hash(f"m{region >> 8}_{region & 255}"))
        if archive is not None:
            (work / f"place-m{region}.bin").write_bytes(cache_tools.decode(cache.container(5, archive)))
            maps.append(region)
    shutil.copyfile(FIXTURES / "shadow-defs.bundle", work / "defs.bundle")
    first = run_java(jar, work, "ids", library)
    ids = [int(v) for v in (work / "used-model-ids.txt").read_text().split()]
    models = {}
    undecodable = []
    for model_id in ids:
        try:
            models[model_id] = cache_tools.decode(cache.container(7, model_id))
        except Exception:
            undecodable.append(model_id)
    mesh.write_bundle(models, work / "models.bundle")
    (work / "materials.bin").write_bytes(cache_tools.decode(cache.container(26, 0)))
    second = run_java(jar, work, "full", library)
    if base.digest(jar) != base.PINNED or base.digest(jar_source) != base.PINNED or mesh.digest(library) != LIBRARY_SHA:
        raise ValueError("pinned inputs changed")
    files = {
        "place-scenes.txt": work / "scenes.txt",
        "place-models.bundle": work / "models.bundle",
        "place-materials.bin": work / "materials.bin",
        "place-entities.tsv": work / "native-place.tsv",
    }
    for region in maps:
        files[f"place-m{region}.bin"] = work / f"place-m{region}.bin"
    for name in SCENE_NAMES:
        files[f"place-heights-{name}.bin"] = work / f"place-heights-{name}.bin"
    evidence = {
        "jarSha256": base.PINNED,
        "libraryJaclibSha256": LIBRARY_SHA,
        "javaSourceSha256": base.digest(source),
        "scenes": SCENE_NAMES,
        "regionsWithTerrain": maps,
        "regionsWithLocs": regions,
        "modelsRequested": len(ids),
        "modelsUndecodable": undecodable,
        "idsRun": first.stdout.strip(),
        "fullRun": second.stdout.strip(),
        "fullStderr": second.stderr.strip()[:2000],
        "injected": [
            "function mask 2048 for every getPair call",
            "hasShadow false",
            "ObjectTypeList and OpenGlToolkit built through Unsafe",
            "GroundGL built through Unsafe over MapLoader.tileHeights",
            "preferenceMask 0",
            "SceneGraph.addObject glue transcribed, not executed",
        ],
        "originalJarUnchanged": True,
    }
    (work / "place-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    files["place-evidence.json"] = work / "place-evidence.json"
    return files, evidence


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--cache", type=Path, default=mesh.CACHE_ROOT)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--library", type=Path, default=LIBRARY)
    parser.add_argument("--fixtures", type=Path, default=FIXTURES)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--no-install", action="store_true")
    args = parser.parse_args()
    if args.check:
        with tempfile.TemporaryDirectory(dir=args.work.parent if args.work.parent.exists() else None) as temp:
            files, evidence = generate(args.jar, args.cache, Path(temp), args.library)
            bad = [n for n, p in sorted(files.items()) if not (args.fixtures / n).exists() or (args.fixtures / n).read_bytes() != p.read_bytes()]
            print(json.dumps({"checked": len(files), "mismatched": bad}))
            raise SystemExit(1 if bad else 0)
    files, evidence = generate(args.jar, args.cache, args.work, args.library)
    if not args.no_install:
        for name, path in files.items():
            (args.fixtures / name).write_bytes(path.read_bytes())
    print(json.dumps(evidence))


if __name__ == "__main__":
    main()
