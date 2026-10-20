import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import diff


def rec(eid, marker, outputs=("T0",), kind="draw"):
    return {"eid": eid, "kind": kind, "marker": marker, "outputs": list(outputs), "topology": "TriangleList", "count": 3, "instances": 1}


class BrowserFilter(unittest.TestCase):
    def test_tag_marks_draws_outside_the_renderer_group(self):
        recs = diff.tag([rec(1, ""), rec(2, "openrs"), rec(3, "openrs/water"), rec(4, "chromium/openrsx")])
        self.assertEqual([r["origin"] for r in recs], ["browser", "renderer", "renderer", "browser"])

    def test_tag_leaves_unmarked_captures_alone(self):
        recs = diff.tag([rec(1, ""), rec(2, "pass")])
        self.assertTrue(all("origin" not in r for r in recs))

    def test_load_keeps_renderer_draws_and_targets(self):
        recs = diff.tag([rec(1, "", ["T1"]), rec(2, "openrs", ["T0"]), rec(3, "openrs/water", ["T0"]), rec(4, "", ["T1"]), rec(5, "", ["T1"], "clear")])
        draws, clears = recs[:4], recs[4:]
        targets = [{"id": "T0", "final": True}, {"id": "T1", "final": False}]
        d = Path(tempfile.mkdtemp())
        (d / "summary.json").write_text(json.dumps({"meta": {}, "targets": targets, "shaders": {}, "clears": clears, "draws": draws}))
        s = diff.load(d)
        self.assertEqual([x["eid"] for x in s["draws"]], [2, 3])
        self.assertEqual([x["marker"] for x in s["draws"]], ["", "water"])
        self.assertEqual(s["clears"], [])
        self.assertEqual([t["id"] for t in s["targets"]], ["T0"])

    def test_load_without_tags_is_unchanged(self):
        d = Path(tempfile.mkdtemp())
        body = {"meta": {}, "targets": [{"id": "T0", "final": True}, {"id": "T1", "final": False}], "shaders": {}, "clears": [], "draws": [rec(1, "")]}
        (d / "summary.json").write_text(json.dumps(body))
        self.assertEqual(diff.load(d), body)

    def test_raw_uniform_words_compare_as_value_sets(self):
        client = {"vs.uMatrix": [1.0, 0.0, 0.5, 2.0], "fs.uFlag": [1.0]}
        same = {"vs.P0._words": [1.0, 0.0, 0.5, 0.0, 2.0, 0.0, 1.0, 0.0], "fs.P0._words": [1.0, 0.0, 0.5, 2.0]}
        self.assertEqual(diff.uniform_diffs(client, same), [])
        other = {"vs.P0._words": [1.0, 0.25, 2.0]}
        out = diff.uniform_diffs(client, other)
        self.assertEqual(len(out), 1)
        self.assertIn("client=[0.5] web=[0.25]", out[0][1])


if __name__ == "__main__":
    unittest.main()
