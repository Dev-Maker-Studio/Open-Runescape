import argparse
import ast
import bz2
import gzip
from hashlib import sha256
import importlib.util
import json
import lzma
from pathlib import Path
from random import Random
import shutil
import struct
import subprocess
import sys

PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
LIBRARY = "92f32235d2a67e7debedd89fbccd2ac0e937302eaa7e0732fe0ba93072a08a74"
ROOT = Path(__file__).resolve().parents[2]
CACHE_ROOT = ROOT.parent / "assets/cache/.packed"
LOC_DEFS = ROOT / "scene/tests/fixtures/loc-defs.tsv"
MUTANT_BASE = 1_000_000
SYNTHETIC_BASE = 2_000_000
PARAMS = [
    (0, 0, 768, 0, 1),
    (1, 40, 768, 1, 1),
    (0, 127, 0, 2, 1),
    (1, 64, 1000, 0, 1),
    (0, 10, 768, 1, 0),
    (1, 90, 500, 2, 1),
]
EXTREME = [(0, -5, 768, 0, 1), (0, 200, 768, 0, 1), (0, 0, -768, 0, 1)]


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


def load_cache_tools():
    path = ROOT / "scene/tools/ground-lighting-world-oracle.py"
    spec = importlib.util.spec_from_file_location("ground_lighting_world_oracle", path)
    module = importlib.util.module_from_spec(spec)
    saved = sys.argv
    sys.argv = ["x"]
    spec.loader.exec_module(module)
    sys.argv = saved
    return module


def loc_model_lists():
    lists = []
    seen = set()
    for line in LOC_DEFS.read_text().splitlines():
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        values = dict(item.split("=", 1) for item in parts[2:] if "=" in item)
        if "modelIds" not in values:
            continue
        slots = ast.literal_eval(values["modelIds"].replace("null", "None")) or []
        for slot in slots:
            if slot:
                key = tuple(slot)
                if key not in seen:
                    seen.add(key)
                    lists.append(key)
    return lists


def smart(value):
    if -64 <= value < 64:
        return bytes([value + 64])
    return struct.pack(">H", value + 49152)


def ushort(value):
    return struct.pack(">H", value & 0xFFFF)


def synthetic_old_model(rng):
    vertices = rng.randint(3, 40)
    faces = rng.randint(1, 40)
    textures = rng.choice([0, 0, 1, 3])
    has_type = rng.random() < 0.6
    priority = rng.choice([255, rng.randrange(0, 12)])
    has_alpha = rng.random() < 0.4
    has_face_skins = rng.random() < 0.3
    has_vertex_skins = rng.random() < 0.3
    flags = bytearray()
    xs = bytearray()
    ys = bytearray()
    zs = bytearray()
    for _ in range(vertices):
        flag = 0
        if rng.random() < 0.8:
            flag |= 1
            xs += smart(rng.randint(-300, 300) if rng.random() < 0.9 else rng.randint(-9000, 9000))
        if rng.random() < 0.8:
            flag |= 2
            ys += smart(rng.randint(-300, 300) if rng.random() < 0.9 else rng.randint(-9000, 9000))
        if rng.random() < 0.8:
            flag |= 4
            zs += smart(rng.randint(-300, 300) if rng.random() < 0.9 else rng.randint(-9000, 9000))
        flags.append(flag)
    kinds = bytearray()
    indices = bytearray()
    last = 0
    for _ in range(faces):
        kind = rng.choice([1, 1, 2, 3, 4])
        kinds.append(kind)

        def target():
            if rng.random() < 0.03:
                return vertices + rng.randint(0, 5)
            return rng.randrange(vertices)

        if kind == 1:
            a, b, c = target(), target(), target()
            indices += smart(a - last) + smart(b - a) + smart(c - b)
            last = c
        else:
            c = target()
            indices += smart(c - last)
            last = c
    priorities = bytes(rng.randrange(0, 256) for _ in range(faces)) if priority == 255 else b""
    face_skins = bytes(rng.randrange(0, 256) for _ in range(faces)) if has_face_skins else b""
    face_types = bytearray()
    colors = bytearray()
    for _ in range(faces):
        color = rng.randrange(65536)
        flag = 0
        if has_type:
            if rng.random() < 0.4:
                flag |= 1
            if textures and rng.random() < 0.5:
                flag |= 2 | (rng.randrange(textures) << 2)
                color = rng.choice([65535, rng.randrange(2594), rng.randrange(2594)])
            face_types.append(flag)
        colors += ushort(color)
    vertex_skins = bytes(rng.randrange(0, 256) for _ in range(vertices)) if has_vertex_skins else b""
    alphas = bytes(rng.choice([0, 255, 128, rng.randrange(256)]) for _ in range(faces)) if has_alpha else b""
    texture_tris = b"".join(ushort(rng.randrange(vertices)) for _ in range(textures * 3))
    header = struct.pack(">HHBBBBBBHHHH", vertices, faces, textures, int(has_type), priority, int(has_alpha), int(has_face_skins),
        int(has_vertex_skins), len(xs), len(ys), len(zs), len(indices))
    data = bytes(flags) + bytes(kinds) + priorities + face_skins + bytes(face_types) + vertex_skins + alphas + bytes(indices) + bytes(colors) + \
        texture_tris + bytes(xs) + bytes(ys) + bytes(zs) + header
    assert not (data[-1] == 255 and data[-2] == 255)
    return data


def mutate(rng, data):
    data = bytearray(data)
    mode = rng.choice(["flip", "flip", "truncate", "zero", "append"])
    if mode == "flip":
        for _ in range(rng.randint(1, 4)):
            data[rng.randrange(max(1, len(data) - 2))] ^= 1 << rng.randrange(8)
    elif mode == "truncate":
        del data[len(data) - rng.randint(1, min(30, len(data) - 1)):]
    elif mode == "zero":
        start = rng.randrange(max(1, len(data) - 2))
        for i in range(start, min(len(data) - 2, start + rng.randint(1, 12))):
            data[i] = 0
    else:
        data += bytes(rng.randrange(256) for _ in range(rng.randint(1, 8)))
    return bytes(data)


def build_inputs(cache_tools):
    cache = cache_tools.Cache(CACHE_ROOT)
    rng = Random(727727)
    lists = loc_model_lists()
    ids = sorted({i for entry in lists for i in entry})
    models = {}
    undecodable = []
    for model_id in ids:
        try:
            models[model_id] = cache_tools.decode(cache.container(7, model_id))
        except Exception:
            undecodable.append(model_id)
    lists = [entry for entry in lists if all(i in models for i in entry)]
    material_payload = cache_tools.decode(cache.container(26, 0))
    cases = []

    def add(name, id_list, params):
        strip, ambient, contrast, uid, with_materials = params
        cases.append({"name": name, "ids": list(id_list), "strip": strip, "ambient": ambient, "contrast": contrast, "uidMode": uid,
            "materials": with_materials})

    for index, entry in enumerate(lists):
        label = "-".join(map(str, entry))
        for k in range(3):
            add(f"loc-{label}-{k}", entry, PARAMS[(index + k * 2) % len(PARAMS)])
    for index, model_id in enumerate(ids):
        if model_id in models and index % 11 == 0:
            for k, params in enumerate(EXTREME):
                add(f"extreme-{model_id}-{k}", [model_id], params)
    small = [i for i in models if len(models[i]) < 12000 and models[i][-1] == 255 and models[i][-2] == 255]
    for n in range(240):
        base = rng.choice(small)
        model_id = MUTANT_BASE + n
        models[model_id] = mutate(rng, models[base])
        add(f"mutant-{n}-of-{base}", [model_id], PARAMS[n % len(PARAMS)])
    synthetic = []
    for n in range(160):
        model_id = SYNTHETIC_BASE + n
        models[model_id] = synthetic_old_model(rng)
        synthetic.append(model_id)
        add(f"old-{n}", [model_id], PARAMS[n % len(PARAMS)])
        add(f"old-{n}-alt", [model_id], PARAMS[(n + 3) % len(PARAMS)])
    for n in range(60):
        pair = [rng.choice(synthetic), rng.choice(synthetic)]
        if n % 2:
            pair[1] = rng.choice(small)
        add(f"old-pair-{n}", pair, PARAMS[n % len(PARAMS)])
    for n in range(80):
        add(f"garbage-{n}", [SYNTHETIC_BASE + 1000 + n], PARAMS[0])
        models[SYNTHETIC_BASE + 1000 + n] = bytes(rng.randrange(256) for _ in range(rng.randint(1, 200))) + (b"\x00" if n % 2 else b"")
    return models, material_payload, cases, undecodable


def write_bundle(models, path):
    out = bytearray(struct.pack(">i", len(models)))
    for model_id in sorted(models):
        out += struct.pack(">ii", model_id, len(models[model_id])) + models[model_id]
    path.write_bytes(bytes(out))


def write_cases(cases, path):
    lines = []
    for case in cases:
        lines.append("\t".join(["case", case["name"], ",".join(map(str, case["ids"])), str(case["strip"]), str(case["ambient"]),
            str(case["contrast"]), str(case["uidMode"]), str(case["materials"])]))
    path.write_text("\n".join(lines) + "\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native-library", type=Path, required=True)
    parser.add_argument("--work", type=Path, default=ROOT / "harness/target/loop/mesh-oracle")
    parser.add_argument("--fixtures", type=Path, default=ROOT / "scene/tests/fixtures")
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    jar = args.work / "oracle-client.jar"
    native = args.work / "libjaclib.so"
    private_copy(args.jar, jar, PINNED)
    private_copy(args.native_library, native, LIBRARY)
    models, material_payload, cases, undecodable = build_inputs(load_cache_tools())
    bundle = args.work / "mesh-oracle-models.bin"
    materials = args.work / "mesh-oracle-materials.bin"
    case_file = args.work / "mesh-oracle-input.tsv"
    output = args.work / "mesh-oracle-native.json"
    write_bundle(models, bundle)
    materials.write_bytes(material_payload)
    write_cases(cases, case_file)
    source = Path(__file__).with_name("MeshOracle.java")
    subprocess.run(["javac", "-cp", str(jar), "-d", str(args.work), str(source)], check=True)
    subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-Djava.awt.headless=true",
        "-cp", f"{args.work}:{jar}", "MeshOracle", str(jar), str(native), str(bundle), str(materials), str(case_file), str(output)],
        check=True, timeout=900)
    native_output = json.loads(output.read_text())
    assert len(native_output["cases"]) == len(cases)
    for case, result in zip(cases, native_output["cases"]):
        assert case["name"] == result["name"]
        result["ids"] = case["ids"]
        result["strip"] = case["strip"]
        result["ambient"] = case["ambient"]
        result["contrast"] = case["contrast"]
        result["uidMode"] = case["uidMode"]
        result["materials"] = case["materials"]
    native_output["javaSourceSha256"] = digest(source)
    native_output["inputSha256"] = digest(case_file)
    native_output["undecodableModels"] = undecodable
    native_output["bundleModels"] = len(models)
    native_output["mutantBase"] = MUTANT_BASE
    native_output["syntheticBase"] = SYNTHETIC_BASE
    native_output["billboardUidSource"] = "injected into BillboardTypeList.BILLBOARD_CACHE per case; not read from the cache"
    for original, copy, expected in [(args.jar, jar, PINNED), (args.native_library, native, LIBRARY)]:
        assert digest(original) == digest(copy) == expected
    args.fixtures.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bundle, args.fixtures / "mesh-oracle-models.bin")
    shutil.copyfile(materials, args.fixtures / "mesh-oracle-materials.bin")
    fixture = args.fixtures / "mesh-oracle.json"
    fixture.write_text(json.dumps(native_output, separators=(",", ":")) + "\n")
    evidence = {"jarSha256": PINNED, "nativeLibrarySha256": LIBRARY, "sourceSha256": digest(source), "fixtureSha256": digest(fixture),
        "cases": len(cases), "bundleModels": len(models), "tally": native_output["tally"], "originalsUnchanged": True}
    (args.work / "oracle-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(f"WROTE {fixture} {len(cases)} cases {len(models)} models tally {native_output['tally']}")


if __name__ == "__main__":
    main()
