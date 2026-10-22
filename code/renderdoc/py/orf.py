import struct
from pathlib import Path
import zlib
from collections import Counter

SKIP = 255
SHADERS = ["model", "ground", "sprite", "groundTextured", "instanced", "shadow", "particle", "hdWater", "materialCubeSpec", "water", "waterHD", "waterLapping", "waterReflecting", "waterRippling", "waterfall", "surfaceMask"]
DRAW = 4
END = 5


def draws(path):
    path = Path(path)
    if path.stat().st_size > 128 * 1024 * 1024:
        raise ValueError("ORF file exceeds summary limit")
    data = path.read_bytes()
    if data[:4] not in (b"ORF3", b"ORF4", b"ORF5"):
        return []
    if len(data) < 44:
        raise ValueError("truncated ORF header or end record")
    pos = 32
    out = []
    while pos + 12 <= len(data):
        kind, raw, packed = struct.unpack_from("<3I", data, pos)
        pos += 12
        if kind == END:
            if raw or packed or pos != len(data):
                raise ValueError("invalid ORF end record")
            return out
        n = packed or raw
        if n > len(data) - pos:
            raise ValueError("truncated ORF record")
        body = data[pos : pos + n]
        pos += n
        if kind != DRAW:
            continue
        if raw > 4096 or packed > 4096:
            raise ValueError("ORF draw record exceeds limit")
        if packed:
            decoder = zlib.decompressobj()
            body = decoder.decompress(body, raw + 1)
            if not decoder.eof or decoder.unused_data or decoder.unconsumed_tail:
                raise ValueError("invalid bounded ORF draw compression")
        if len(body) != raw or len(body) < 60:
            raise ValueError("invalid ORF draw size")
        pas, _, _, count = struct.unpack_from("<4I", body, 0)
        if count > 16:
            raise ValueError("ORF draw texture limit")
        at = 56 + 4 * count
        if at + 4 > len(body):
            raise ValueError("truncated ORF draw header")
        (size,) = struct.unpack_from("<I", body, at)
        end = at + 4 + size
        if size > 1024 or end > len(body) or body[end:] not in (b"", b"\0\0\0\0"):
            raise ValueError("invalid ORF draw name or tail")
        out.append((pas, body[at + 4 : end].decode("utf-8", "replace")))
    raise ValueError("missing ORF end record")


def counts(path):
    rows = draws(path)
    drawn = Counter(SHADERS[p] for p, _ in rows if p < len(SHADERS))
    return drawn, Counter(n for p, n in rows if p == SKIP)
