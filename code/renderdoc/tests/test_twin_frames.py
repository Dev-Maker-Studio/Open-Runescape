import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import numpy as np
from PIL import Image

import twin_frames


def grey(v, size=(48, 40)):
    return np.full((size[1], size[0], 3), v, np.uint8)


def save(arr, path):
    Image.fromarray(arr).save(path)


class Motion(unittest.TestCase):
    def test_a_moving_block_marks_its_neighbourhood_only(self):
        frames = []
        for i in range(3):
            f = grey(0)
            f[10:14, 4 + 8 * i : 8 + 8 * i] = 255
            frames.append(f)
        mask = twin_frames.motion((frames[1], frames[1]), ((frames[0], frames[0]), (frames[2], frames[2])))
        self.assertTrue(mask[12, 14])
        self.assertFalse(mask[35, 40])

    def test_still_frames_have_no_motion(self):
        self.assertFalse(twin_frames.motion((grey(5), grey(5)), ((grey(5), grey(5)), None)).any())

    def test_single_frame_has_no_motion(self):
        self.assertFalse(twin_frames.motion((grey(5), grey(5)), (None, None)).any())


class Summary(unittest.TestCase):
    def test_sheet_picks_evenly_spaced_frames_up_to_twelve(self):
        self.assertEqual(twin_frames.sheet_indices(5), [0, 1, 2, 3, 4])
        picked = twin_frames.sheet_indices(30)
        self.assertEqual(len(picked), 12)
        self.assertEqual((picked[0], picked[-1]), (0, 29))

    def test_outliers_are_frames_far_under_the_median(self):
        self.assertEqual(twin_frames.outliers([0.97, 0.97, 0.42, 0.97, 0.96]), [2])
        self.assertEqual(twin_frames.outliers([0.9, 0.91, 0.92]), [])

    def test_trend_directions(self):
        self.assertEqual(twin_frames.trend([0.99] * 6)["direction"], "flat")
        self.assertEqual(twin_frames.trend([0.99, 0.97, 0.95, 0.9, 0.85, 0.8])["direction"], "degrading")
        self.assertEqual(twin_frames.trend([0.8, 0.85, 0.9, 0.95, 0.97, 0.99])["direction"], "improving")
        self.assertEqual(twin_frames.trend([0.9])["direction"], "flat")

    def test_verdict_needs_identical_frames_mean_and_minimum(self):
        good = [{"ssim": 0.995, "identical": True}] * 4
        self.assertEqual(twin_frames.verdict(good)[0], "PASS")
        self.assertEqual(twin_frames.verdict(good + [{"ssim": 0.5, "identical": True}])[0], "FAIL")
        self.assertEqual(twin_frames.verdict(good[:3] + [{"ssim": 0.995, "identical": False}])[0], "FAIL")
        self.assertEqual(twin_frames.verdict([{"ssim": 0.985, "identical": True}] * 3)[0], "FAIL")

    def test_history_compares_only_the_same_frame_count(self):
        with tempfile.TemporaryDirectory() as d:
            h = f"{d}/h.jsonl"
            rec = {"scene": "varrock", "frames": 20, "ssim": 0.9}
            self.assertIsNone(twin_frames.history(h, rec))
            self.assertIsNone(twin_frames.history(h, dict(rec, frames=30)))
            self.assertEqual(twin_frames.history(h, dict(rec, ssim=0.95)), 0.05)


class Run(unittest.TestCase):
    def build(self, d, count):
        client, web = Path(d) / "client", Path(d) / "web"
        client.mkdir()
        web.mkdir()
        entries = []
        for i in range(count):
            base = grey(40 + i)
            moving = base.copy()
            moving[5:15, 2 + 4 * i : 12 + 4 * i] = 220
            other = moving.copy()
            other[5:15, 2 + 4 * i : 12 + 4 * i] = 180
            save(moving, client / f"frame-{i:04d}.png")
            save(other, web / f"frame-{i:04d}.png")
            (client / f"frame-{i:04d}.orf").write_bytes(b"orf%d" % i)
            (web / f"frame-{i:04d}.orf").write_bytes(b"orf%d" % i)
            entries.append({"index": i, "frame": 10 + i, "tick": 3, "nanos": 1000 * i})
        (client / "frames.json").write_text(json.dumps({"count": count, "frames": entries}))

    def test_outputs_and_records(self):
        with tempfile.TemporaryDirectory() as d:
            self.build(d, 4)
            r = twin_frames.run(d, "synthetic", 1)
            self.assertEqual(r["frames"], 4)
            self.assertTrue(r["summary"]["all_serialized_identical"])
            self.assertEqual(len(r["frame_records"]), 4)
            self.assertTrue(r["frame_records"][1]["dynamic_only"])
            for name in ("client.png", "web.png", "diff.png", "twin.png"):
                self.assertTrue((Path(d) / "frames" / "0003" / name).is_file())
            self.assertTrue((Path(d) / "twin-frames.png").is_file())
            with Image.open(Path(d) / "twin.gif") as gif:
                self.assertEqual(gif.n_frames, 4)
            self.assertEqual(json.loads((Path(d) / "twin.json").read_text())["mode"], "frames")
            self.assertTrue((Path(d) / "web" / "frame-0000.orf").exists())
            self.assertIn("TWIN scene=synthetic frames=4", twin_frames.text(r))

    def test_incomplete_capture_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            self.build(d, 2)
            with self.assertRaisesRegex(ValueError, "incomplete capture"):
                twin_frames.run(d, "test", 1, expected=3)
            (Path(d) / "client" / "frames.json").write_text(json.dumps({"frames": [], "error": "disk full"}))
            with self.assertRaisesRegex(ValueError, "disk full"):
                twin_frames.run(d, "test", 1)

    def test_whole_screen_change_is_not_dynamic_only(self):
        with tempfile.TemporaryDirectory() as d:
            self.build(d, 2)
            for i in range(2):
                save(grey(i * 255), Path(d) / "client" / f"frame-{i:04d}.png")
                save(grey(100), Path(d) / "web" / f"frame-{i:04d}.png")
            r = twin_frames.run(d, "test", 1)
            self.assertFalse(any(f["dynamic_only"] for f in r["frame_records"]))

    def test_a_different_serialized_frame_fails_and_is_kept(self):
        with tempfile.TemporaryDirectory() as d:
            self.build(d, 2)
            (Path(d) / "web" / "frame-0001.orf").write_bytes(b"other")
            r = twin_frames.run(d, "synthetic", 1)
            self.assertEqual(r["verdict"], "FAIL")
            self.assertEqual(r["summary"]["serialized_identical"], 1)
            self.assertEqual(r["frame_records"][1]["first_difference"], 1)
            self.assertTrue((Path(d) / "web" / "frame-0001.orf").exists())


if __name__ == "__main__":
    unittest.main()
