import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import zlib


def raw(data):
    return b"\x00" + struct.pack(">I", len(data)) + data


def whirlpool(data):
    result = subprocess.run(
        ["openssl", "dgst", "-provider", "legacy", "-whirlpool", "-binary"],
        input=data, capture_output=True, check=True,
    )
    if len(result.stdout) != 64:
        raise RuntimeError("invalid Whirlpool digest length")
    return result.stdout


def reply(index, archive, container):
    wire = bytearray(bytes([index]) + struct.pack(">I", archive) + container[:5])
    position = 10
    for byte in container[5:]:
        if position == 512:
            wire.append(255)
            position = 1
        wire.append(byte)
        position += 1
    return bytes(wire)


def chain(files, digest=True):
    payload = b"".join(data for _, data in files)
    if len(files) > 1:
        previous = 0
        for _, data in files:
            payload += struct.pack(">i", len(data) - previous)
            previous = len(data)
        payload += b"\x01"
    archive = raw(payload)
    reference = bytearray(b"\x06" + struct.pack(">i", 727) + bytes([2 if digest else 0]) + b"\x00\x01\x00\x00")
    reference += struct.pack(">I", zlib.crc32(archive))
    if digest:
        reference += whirlpool(archive)
    reference += struct.pack(">iH", 44, len(files))
    previous = 0
    for file_id, _ in files:
        reference += struct.pack(">H", file_id - previous)
        previous = file_id
    reference = raw(reference)
    version = b"\x1b" + bytes(26 * 72)
    version += struct.pack(">Ii", zlib.crc32(reference), 9001) + whirlpool(reference)
    version += bytes([0x5a]) * 128
    wire = bytes(109) + reply(255, 255, raw(version)) + reply(255, 26, reference)
    return wire + reply(26, 0, archive)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--update", action="store_true")
    parser.add_argument("--evidence-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    native = root.parent / "scene/tests/fixtures/material.tsv"
    lines = [line.split("\t") for line in native.read_text().splitlines()]
    source_hash = next(fields[1] for fields in lines if fields[0] == "jar-sha256")
    source_case = "bytes-128"
    payload = bytes.fromhex(next(fields[2] for fields in lines if fields[:2] == ["table", source_case]))
    valid = chain([(0, payload)])
    corrupt = bytearray(valid)
    corrupt[-1] ^= 1
    fixtures = {
        "material-native.wire": valid,
        "material-truncated.wire": valid[:-1],
        "material-corrupt.wire": bytes(corrupt),
        "material-missing0.wire": chain([(1, payload)]),
        "material-extra-files.wire": chain([(0, payload), (1, b"\x00\x00")]),
        "material-metadata-truncated.wire": chain([(0, payload[:-1])]),
        "material-trailing.wire": chain([(0, payload + b"\xff")]),
    }
    additional_cases = {
        "material-param2-min.wire": "param2--2147483648",
        "material-param2-max.wire": "param2-2147483647",
        "material-id-addressing.wire": "id-addressing",
    }
    for name, case in additional_cases.items():
        data = bytes.fromhex(next(fields[2] for fields in lines if fields[:2] == ["table", case]))
        fixtures[name] = chain([(0, data)])
    fixtures["material-without-archive-digest.wire"] = chain([(0, payload)], digest=False)
    metadata = {
        "origin": "Synthetic JS5 integrity chain carrying a material payload decoded by the unchanged native jar; not captured live cache or actual material IDs128/154",
        "sourceJarSha256": source_hash,
        "sourceFixtureSha256": hashlib.sha256(native.read_bytes()).hexdigest(),
        "nativeCase": source_case,
        "additionalNativeCases": additional_cases,
        "materialPayloadHex": payload.hex(),
        "materialFields": [
            {"id": int(fields[2]), "values": [int(value) for value in fields[3:]]}
            for fields in lines if fields[:2] == ["material", source_case]
        ],
        "index": 26,
        "archive": 0,
        "file": 0,
        "referenceRevision": 727,
        "archiveRevision": 44,
        "authenticated": False,
        "referenceCrcAndWhirlpool": True,
        "archiveCrcAndWhirlpool": True,
        "extraFilesPolicy": "Reject any archive except exactly one file with id0; stricter than native getFile(0,0), which could ignore other files",
        "trailingPolicy": "Reject trailing material bytes although native decoder ignores them",
        "fixtures": {
            name: {"bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
            for name, data in fixtures.items()
        },
    }
    fixtures["material-native.json"] = (json.dumps(metadata, indent=2) + "\n").encode()
    for name, data in fixtures.items():
        path = root / "tests/fixtures" / name
        if args.update:
            path.write_bytes(data)
        elif path.read_bytes() != data:
            raise RuntimeError(f"material fixture differs: {name}")
    summary = {
        "status": "matched",
        "wireFixtures": len(fixtures) - 1,
        "nativeCase": source_case,
        "sourceJarSha256": source_hash,
        "authenticated": False,
        "syntheticTransport": True,
    }
    if args.evidence_dir is not None:
        args.evidence_dir.mkdir(parents=True, exist_ok=True)
        (args.evidence_dir / "material-runtime-fixtures.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    main()
