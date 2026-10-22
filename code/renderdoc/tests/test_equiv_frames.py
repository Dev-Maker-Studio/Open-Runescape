import contextlib
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import equiv_frames


class Sequence(unittest.TestCase):
    def test_reports_per_frame_and_mean_and_needs_all_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            for i, (verdict, score, ssim) in enumerate((("PASS", 98, 0.99), ("FAIL", 94, 0.95))):
                d = out / "frames" / f"{i:04d}"
                d.mkdir(parents=True)
                (d / "equiv.json").write_text(json.dumps({"verdict": verdict, "score": score, "pixels": {"ssim": ssim}}))
            with contextlib.redirect_stdout(io.StringIO()):
                r = equiv_frames.run(out, 2, "test")
            self.assertEqual(r["score"], 96)
            self.assertEqual(r["mean_ssim"], 0.97)
            self.assertEqual(r["verdict"], "FAIL")
            self.assertEqual(r["frames"], 2)
            self.assertEqual([r["index"] for r in r["frame_records"]], [0, 1])
            self.assertIn("frame #1:", (out / "report.txt").read_text())

    def test_missing_pixels_do_not_imply_a_full_sequence_ssim(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            d = out / "frames" / "0000"
            d.mkdir(parents=True)
            (d / "equiv.json").write_text(json.dumps({"verdict": "FAIL", "score": 10, "pixels": None}))
            with contextlib.redirect_stdout(io.StringIO()):
                r = equiv_frames.run(out, 1, "test")
            self.assertIsNone(r["mean_ssim"])
            self.assertEqual(r["verdict"], "FAIL")


if __name__ == "__main__":
    unittest.main()
