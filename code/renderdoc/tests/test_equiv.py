import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
GOLDEN = HERE / "golden" / "selftest"
EXPECTED = json.loads((HERE / "golden" / "expected.json").read_text())


def equiv(client, web, *extra):
    out = Path(tempfile.mkdtemp())
    r = subprocess.run([sys.executable, str(HERE / "py" / "equiv.py"), "report", str(client), str(web), str(out), "--scene", "golden", "--history", str(out / "h.jsonl"), "--no-history", *extra], capture_output=True, text=True)
    return r.returncode, json.loads((out / "equiv.json").read_text()), r.stdout


def copy(name):
    dst = Path(tempfile.mkdtemp()) / name
    shutil.copytree(GOLDEN / name, dst)
    return dst


class Golden(unittest.TestCase):
    def test_baseline_matches_stored_report(self):
        code, r, text = equiv(GOLDEN / "client", GOLDEN / "web", "--min-ssim", "0.995", "--max-mismatch", "1")
        self.assertEqual(code, 0)
        for k, v in EXPECTED["baseline"].items():
            self.assertEqual(r[k] if k != "ssim" else r["pixels"]["ssim"], v, k)
        self.assertIn("blend", text)

    def test_strict_threshold_fails_on_blend_mismatch(self):
        code, r, _ = equiv(GOLDEN / "client", GOLDEN / "web")
        self.assertEqual(code, 1)
        self.assertEqual(r["verdict"], "FAIL")
        self.assertEqual(r["draws"]["mismatched"], 1)

    def test_missing_draw_is_unmatched(self):
        web = copy("web")
        s = json.loads((web / "summary.json").read_text())
        s["draws"].pop()
        (web / "summary.json").write_text(json.dumps(s))
        code, r, _ = equiv(GOLDEN / "client", web, "--max-mismatch", "5")
        self.assertEqual(code, 1)
        self.assertEqual(len(r["draws"]["only_client"]), 1)

    def test_texture_and_uniform_differences_are_listed(self):
        web = copy("web")
        s = json.loads((web / "summary.json").read_text())
        s["draws"][0]["uniforms"] = {k: [v + 1 for v in vals] for k, vals in s["draws"][0]["uniforms"].items()} or {"vs.x": [1.0]}
        s["draws"][0]["textures"] = [{"stage": "fs", "slot": 0, "id": "X0", "size": [4, 4], "mips": 1, "format": "RGBA8", "hash": "deadbeef", "flip_hash": "feedface"}]
        (web / "summary.json").write_text(json.dumps(s))
        _, r, _ = equiv(GOLDEN / "client", web, "--max-mismatch", "5")
        self.assertTrue(r["texture_diffs"])
        self.assertEqual(r["texture_sets"]["web_only"], ["deadbeef"])

    def test_pixel_regions_find_changed_area(self):
        from PIL import Image

        web = copy("web")
        img = Image.open(web / "targets" / "T0.png").convert("RGB")
        for x in range(200, 256):
            for y in range(200, 256):
                img.putpixel((x, y), (255, 0, 255))
        img.save(web / "targets" / "T0.png")
        _, r, _ = equiv(GOLDEN / "client", web, "--top", "3")
        box = r["regions"][0]["box"]
        self.assertGreaterEqual(box[0], 192)
        self.assertGreaterEqual(box[1], 192)
        self.assertLess(r["pixels"]["ssim"], 1.0)


if __name__ == "__main__":
    unittest.main()
