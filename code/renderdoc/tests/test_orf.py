import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import orf


def header(magic):
    return magic + struct.pack("<III4f", 0, 1, 1, 0, 0, 0, 1)


def chunk(kind, body, compress=False):
    payload = zlib.compress(body) if compress else body
    return struct.pack("<III", kind, len(body), len(payload) if compress else 0) + payload


def draw(pas, name="", tail=b"", compress=False):
    text = name.encode()
    words = [pas, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0xFFFFFFFF, len(text)]
    return chunk(4, struct.pack("<15I", *words) + text + tail, compress)


END = struct.pack("<III", 5, 0, 0)


class OrfSummary(unittest.TestCase):
    def run_file(self, data):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "frame.orf"
            path.write_bytes(data)
            return orf.counts(path)

    def test_orf3_and_orf4_keep_the_same_draw_counts(self):
        for magic in (b"ORF3", b"ORF4"):
            drawn, skipped = self.run_file(header(magic) + draw(4, "instanced") + draw(255, "hdWater@offscreen") + END)
            self.assertEqual(drawn, {"instanced": 1})
            self.assertEqual(skipped, {"hdWater@offscreen": 1})

    def test_typed_texture_chunks_do_not_hide_later_draws(self):
        typed = struct.pack("<7I", 0, 1, 1, 1, 1, 8, 0) + struct.pack("<4e", 4, -0.5, 0, 1)
        drawn, skipped = self.run_file(header(b"ORF4") + chunk(6, typed) + draw(0, "model") + END)
        self.assertEqual(drawn, {"model": 1})
        self.assertEqual(skipped, {})

    def test_orf5_bloom_record_does_not_hide_draws(self):
        bloom = chunk(7, struct.pack("<3f", 8.0, 0.5, 0.25))
        drawn, skipped = self.run_file(header(b"ORF5") + draw(0, "model") + bloom + END)
        self.assertEqual(drawn, {"model": 1})
        self.assertEqual(skipped, {})

    def test_compressed_draw_names_and_exact_legacy_zero_tail(self):
        name = "groundTextured@" + "x" * 180
        drawn, skipped = self.run_file(header(b"ORF4") + draw(255, name, b"\0" * 4, True) + END)
        self.assertEqual(drawn, {})
        self.assertEqual(skipped, {name: 1})

    def test_partial_files_fail_instead_of_reporting_zero_draws(self):
        with self.assertRaisesRegex(ValueError, "end"):
            self.run_file(header(b"ORF4") + draw(0))
        with self.assertRaisesRegex(ValueError, "truncated"):
            self.run_file(header(b"ORF3") + struct.pack("<III", 4, 100, 0) + b"a")

    def test_nonzero_tail_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "tail"):
            self.run_file(header(b"ORF4") + draw(0, "model", b"\0\0\0\1") + END)

    def test_declared_large_draw_is_bounded(self):
        with self.assertRaisesRegex(ValueError, "limit"):
            self.run_file(header(b"ORF4") + struct.pack("<III", 4, 1 << 28, 1) + b"a" + END)

    def test_compressed_bomb_cannot_expand_past_the_declared_draw_size(self):
        packed = zlib.compress(b"x" * (1024 * 1024))
        with self.assertRaises(ValueError):
            self.run_file(header(b"ORF4") + struct.pack("<III", 4, 60, len(packed)) + packed + END)


if __name__ == "__main__":
    unittest.main()
