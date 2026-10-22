import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import numpy as np
from PIL import Image

import twin


def write(path, data):
    Path(path).write_bytes(data)
    return path


def solid(path, colour, size=(64, 48)):
    Image.new("RGB", size, colour).save(path)
    return path


class RegionSsim(unittest.TestCase):
    def frame(self):
        rng = np.random.default_rng(3)
        return rng.integers(0, 255, (503, 765, 3), dtype=np.uint8)

    def test_identical_frames_score_one_everywhere(self):
        a = self.frame()
        r = twin.region_ssim(a, a.copy())
        self.assertEqual(list(r), ["terrain", "models", "minimap", "chat"])
        for v in r.values():
            self.assertEqual(v["ssim"], 1.0)
        self.assertIsNone(r["models"]["box"])

    def test_change_in_viewport_hits_models_not_minimap_or_chat(self):
        a = self.frame()
        b = a.copy()
        b[100:140, 200:240] = 255 - b[100:140, 200:240]
        r = twin.region_ssim(a, b)
        self.assertLess(r["models"]["ssim"], 0.5)
        self.assertEqual(r["terrain"]["ssim"], 1.0)
        self.assertEqual(r["minimap"]["ssim"], 1.0)
        self.assertEqual(r["chat"]["ssim"], 1.0)
        x0, y0, x1, y1 = r["models"]["box"]
        self.assertTrue(x0 <= 200 and y0 <= 100 and x1 >= 240 and y1 >= 140)

    def test_minimap_and_chat_changes_are_isolated(self):
        a = self.frame()
        b = a.copy()
        b[50:120, 600:700] = 0
        b[400:440, 20:300] = 0
        r = twin.region_ssim(a, b)
        self.assertLess(r["minimap"]["ssim"], 0.9)
        self.assertLess(r["chat"]["ssim"], 0.9)
        self.assertEqual(r["terrain"]["ssim"], 1.0)
        self.assertEqual(r["models"]["ssim"], 1.0)

    def test_small_global_noise_lowers_terrain(self):
        a = self.frame()
        b = a.astype(np.int16)
        b[:338, :516] += 10
        r = twin.region_ssim(a, np.clip(b, 0, 255).astype(np.uint8))
        self.assertLess(r["terrain"]["ssim"], 1.0)
        self.assertEqual(r["models"]["ssim"], 1.0)


class Serialized(unittest.TestCase):
    def test_identical_files(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = write(f"{d}/a", b"frame"), write(f"{d}/b", b"frame")
            r = twin.serialized(a, b)
            self.assertTrue(r["identical"])
            self.assertIsNone(r["first_difference"])

    def test_first_difference_is_reported(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = write(f"{d}/a", b"frame one"), write(f"{d}/b", b"frame two")
            r = twin.serialized(a, b)
            self.assertFalse(r["identical"])
            self.assertEqual(r["first_difference"], 6)

    def test_truncated_file_differs_at_the_shorter_length(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = write(f"{d}/a", b"abcd"), write(f"{d}/b", b"ab")
            self.assertEqual(twin.serialized(a, b)["first_difference"], 2)

    def test_missing_file_is_not_identical(self):
        with tempfile.TemporaryDirectory() as d:
            a = write(f"{d}/a", b"x")
            r = twin.serialized(a, f"{d}/missing")
            self.assertFalse(r["identical"])
            self.assertIsNone(r["web"]["sha256"])


class Regions(unittest.TestCase):
    def test_a_changed_block_is_the_worst_region(self):
        a = np.zeros((64, 64, 3), np.uint8)
        b = a.copy()
        b[16:32, 32:48] = 200
        top = twin.regions(a, b, 3)
        self.assertEqual(top[0]["box"][:2], [32, 16])
        self.assertEqual(top[0]["differing_pct"], 100.0)

    def test_identical_images_have_no_regions(self):
        a = np.zeros((32, 32, 3), np.uint8)
        self.assertEqual(twin.regions(a, a.copy(), 5), [])


class Compose(unittest.TestCase):
    def test_sheet_holds_three_panels(self):
        with tempfile.TemporaryDirectory() as d:
            c, w, df = solid(f"{d}/c.png", (10, 10, 10)), solid(f"{d}/w.png", (20, 20, 20)), solid(f"{d}/d.png", (0, 0, 0))
            size = twin.compose(c, w, df, f"{d}/t.png", "title", ["line one", "line two"])
            self.assertGreater(size[0], 3 * 64)
            self.assertGreater(size[1], 48)
            self.assertTrue(Path(f"{d}/t.png").is_file())


class Trend(unittest.TestCase):
    def test_trend_compares_with_the_previous_run_of_the_same_scene(self):
        with tempfile.TemporaryDirectory() as d:
            h = f"{d}/sub/history.jsonl"
            r = {"scene": "varrock", "verdict": "FAIL", "pixels": {"ssim": 0.9, "psnr_db": 20}, "serialized": {"identical": True}}
            self.assertIsNone(twin.trend(h, r))
            r["pixels"] = {"ssim": 0.95, "psnr_db": 25}
            self.assertEqual(twin.trend(h, r), 0.05)
            other = dict(r, scene="falador")
            self.assertIsNone(twin.trend(h, other))
            self.assertEqual(len(Path(h).read_text().splitlines()), 3)


if __name__ == "__main__":
    unittest.main()
