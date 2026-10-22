import json
import struct
import sys
import tempfile
import unittest
import zlib
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(HERE / "py"))
import classify
import convention
import diff
import orf

REAL = json.loads((HERE / "golden" / "matrix.json").read_text())


def tex(size, h, flip=None, fmt="RGBA8"):
    return {"size": size, "hash": h, "flip_hash": flip, "format": fmt}


class ClipMatrix(unittest.TestCase):
    def test_real_view_projection_equals_the_web_words_after_depth_range(self):
        client = REAL["client"]
        web = {"vs.uniforms10._words": REAL["web_words"]}
        self.assertTrue(diff.uniform_diffs(client, web))
        summary = {"meta": {"api": "OpenGL"}, "draws": [{"uniforms": dict(client)}]}
        norm = convention.to_web(summary)["draws"][0]["uniforms"]
        self.assertEqual(diff.uniform_diffs(norm, web), [])

    def test_z_row_is_the_mean_of_z_and_w(self):
        m = REAL["client"]["vs.uViewProjection"]
        out = convention.depth_range(m)
        self.assertEqual(out[:8], m[:8])
        self.assertEqual(out[12:], m[12:])
        self.assertAlmostEqual(out[11], (m[11] + m[15]) / 2)

    def test_inverse_is_left_alone_because_the_web_keeps_it(self):
        inv = REAL["inverse"]
        self.assertFalse(convention.is_clip_matrix("fs.uInvViewProjection", inv["client"]))
        column_major = [inv["client"][c + 4 * r] for c in range(4) for r in range(4)]
        self.assertEqual(column_major, inv["web_words"])

    def test_recognized_by_name_and_shape(self):
        sixteen = [0.0] * 16
        for name in ("vs.uViewProjection", "vs.uProjection", "vs.uShadowMatrix", "vs.uModelViewProjection"):
            self.assertTrue(convention.is_clip_matrix(name, sixteen), name)
        for name in ("vs.uViewMatrix", "fs.uInvViewProjection", "vs.uFogStart"):
            self.assertFalse(convention.is_clip_matrix(name, sixteen), name)
        self.assertFalse(convention.is_clip_matrix("vs.uViewProjection", [0.0] * 4))

    def test_vulkan_summaries_are_untouched(self):
        s = {"meta": {"api": "Vulkan"}, "draws": [{"uniforms": {"vs.uViewProjection": list(range(16))}}]}
        self.assertEqual(convention.to_web(s)["draws"][0]["uniforms"]["vs.uViewProjection"], list(range(16)))

    def test_tolerance_absorbs_rounding_only(self):
        a, b = [1.0, 19560.0625], [1.0004, 19560.06]
        self.assertEqual(diff.missing(a, b), [])
        self.assertEqual(diff.missing([0.5], [0.52]), [0.5])


class TimeDrift(unittest.TestCase):
    client = {"fs.uTextureTime": [3.245], "vs.uPointLightColor.uPointLightColor[0]": [0.0674, 0.048, 0.0055], "vs.uFogStart": [42140.0586]}
    web = {"vs.U0._words": [42140.0586, 10.331, 0.0, 0.083, 0.0591, 0.0068, 0.0, 16.0]}

    def test_moved_time_explains_light_flicker(self):
        out = diff.uniform_diffs(self.client, self.web)
        self.assertEqual([k for k, _ in out], ["drift"])
        self.assertIn("uPointLightColor", out[0][1])

    def test_same_time_leaves_light_difference_real(self):
        client = {**self.client, "fs.uTextureTime": [10.331]}
        out = diff.uniform_diffs(client, self.web)
        self.assertEqual([k for k, _ in out], ["uniform"])
        self.assertIn("uPointLightColor", out[0][1])

    def test_unrelated_value_stays_real_under_drift(self):
        client = {**self.client, "vs.uFogStart": [41000.0]}
        out = diff.uniform_diffs(client, self.web)
        self.assertEqual([k for k, _ in out], ["uniform"])
        self.assertIn("client=[41000.0]", out[0][1])


class WebExtras(unittest.TestCase):
    def test_extra_web_values_are_a_note(self):
        out = diff.uniform_diffs({"vs.uFog": [3.5]}, {"vs.U0._words": [3.5, 0.0, 16.0]})
        self.assertEqual([k for k, _ in out], ["web extra"])

    def test_missing_client_value_is_still_real(self):
        out = diff.uniform_diffs({"vs.uFog": [3.5, 7.0]}, {"vs.U0._words": [3.5, 16.0]})
        self.assertEqual([k for k, _ in out], ["uniform"])


class Textures(unittest.TestCase):
    def test_placeholders_do_not_count(self):
        client = [tex([128, 128, 1], "b")]
        web = [tex([1, 1, 1], "p"), tex([128, 128, 1], "b"), tex([1, 1, 1], "q")]
        self.assertEqual(convention.textures(client, web), ([], []))

    def test_missing_web_texture_is_not_assumed_unused(self):
        client = [tex([16, 16, 1], "a"), tex([128, 128, 1], "b")]
        web = [tex([1, 1, 1], "p"), tex([128, 128, 1], "b")]
        found, notes = convention.textures(client, web)
        self.assertEqual(found, ["hash only client=a [16, 16, 1] RGBA8"])
        self.assertEqual(notes, [])

    def test_bound_count_and_slot_order_do_not_matter(self):
        self.assertEqual(convention.textures([tex([4, 4, 1], "a"), tex([8, 8, 1], "b")], [tex([8, 8, 1], "b"), tex([4, 4, 1], "a")]), ([], []))

    def test_flipped_pair_is_one_difference(self):
        out, _ = convention.textures([tex([4, 4, 1], "a", "f")], [tex([4, 4, 1], "f", "a")])
        self.assertEqual(len(out), 1)
        self.assertIn("vertically flipped", out[0])

    def test_content_difference_is_reported_by_hash(self):
        out, _ = convention.textures([tex([4, 4, 1], "a")], [tex([4, 4, 1], "z")])
        self.assertEqual(out, ["hash only client=a [4, 4, 1] RGBA8", "hash only web=z [4, 4, 1] RGBA8"])

    def test_equal_hash_with_clamped_float_values_is_a_difference(self):
        client = {**tex([64, 64, 1], "f", fmt="R16G16B16A16_FLOAT"), "range": [3.924, 3.924]}
        out, _ = convention.textures([client], [tex([64, 64, 1], "f", fmt="R8G8B8A8_UNORM")])
        self.assertEqual(len(out), 1)
        self.assertIn("clamps it to 0..1", out[0])
        self.assertEqual(convention.textures([{**client, "range": [0.0, 1.0]}], [tex([64, 64, 1], "f")]), ([], []))
        self.assertEqual(convention.textures([client], [tex([64, 64, 1], "f", fmt="R16G16B16A16_FLOAT")]), ([], []))

    def test_extra_web_texture_is_a_difference(self):
        out, _ = convention.textures([tex([4, 4, 1], "a")], [tex([4, 4, 1], "a"), tex([765, 503, 1], "d")])
        self.assertEqual(out, ["hash only web=d [765, 503, 1] RGBA8"])


class Blend(unittest.TestCase):
    def test_source_alpha_factor_swap_is_a_convention(self):
        b = {"src": "SrcAlpha", "dst": "InvSrcAlpha", "op": "Add", "asrc": "SrcAlpha", "adst": "InvSrcAlpha", "aop": "Add", "mask": 15}
        self.assertTrue(convention.alpha_only([b], [{**b, "asrc": "One"}]))
        self.assertFalse(convention.alpha_only([b], [{**b, "src": "One"}]))
        self.assertFalse(convention.alpha_only([b], [b]))
        c = {"state": {"blend": [b]}}
        w = {"state": {"blend": [{**b, "asrc": "One"}]}}
        differences, _ = diff.draw_diffs(c, w)
        self.assertTrue(any(kind == "state" and "blend" in msg for kind, msg in differences))


def sh(inputs):
    return {"inputs": [f"{i}:a{i}:Floatx{n}" for i, n in enumerate(inputs)]}


def draw(eid, comps, topology="TriangleList"):
    return {"eid": eid, "topology": topology, "shaders": {"vs": "S" + "".join(map(str, comps))}, "count": 3}


def summary(*draws):
    return {"draws": list(draws), "shaders": {d["shaders"]["vs"]: sh([int(ch) for ch in d["shaders"]["vs"][1:]]) for d in draws}}


GROUND, SPRITE, PARTICLE = (3, 4, 3, 3), (2, 2, 4, 1, 4), (3, 2, 4)


class Unmatched(unittest.TestCase):
    def test_offscreen_skip_and_tessellation_are_known(self):
        client = summary(*[draw(i, GROUND) for i in range(3)], draw(9, (3, 4, 3), "PatchList_3CPs"))
        web = summary(draw(100, GROUND))
        frame = (Counter({"groundTextured": 1}), Counter({"groundTextured@offscreen": 2, "hdWater@mode14": 1}))
        rows = classify.classify(client, web, client["draws"][1:], [], frame)
        self.assertEqual([r["known"] for r in rows], [True, True, True])
        self.assertIn("offscreen", rows[0]["reason"])
        self.assertIn("tessellation", rows[2]["reason"])

    def test_skip_budget_is_not_exceeded(self):
        client = summary(*[draw(i, SPRITE) for i in range(3)])
        web = summary(draw(50, SPRITE))
        frame = (Counter({"sprite": 3}), Counter({"sprite@offscreen": 1}))
        rows = classify.classify(client, web, client["draws"][1:], [], frame)
        self.assertEqual([r["known"] for r in rows], [True, False])
        self.assertEqual(rows[1]["reason"], "genuinely missing on the web")

    def test_a_mostly_empty_dump_is_a_real_failure(self):
        client = summary(*[draw(i, GROUND) for i in range(10)])
        rows = classify.classify(client, summary(), client["draws"], [], (Counter({"sprite": 2}), Counter()))
        self.assertEqual([r["known"] for r in rows], [False] * 10)
        self.assertIn("frame dump incomplete", rows[0]["reason"])

    def test_pass_absent_from_the_dump_is_not_ported(self):
        client = summary(draw(1, GROUND))
        rows = classify.classify(client, summary(), client["draws"], [], (Counter({"sprite": 1}), Counter()))
        self.assertEqual((rows[0]["known"], rows[0]["reason"]), (True, "shader/pass not ported"))

    def test_drift_when_the_web_drew_what_the_dump_holds(self):
        client = summary(draw(1, SPRITE))
        web = summary(draw(5, SPRITE), draw(6, SPRITE))
        rows = classify.classify(client, web, [], [web["draws"][1]], (Counter({"sprite": 2}), Counter()))
        self.assertTrue(rows[0]["known"])
        self.assertIn("frame drift", rows[0]["reason"])

    def test_missing_without_a_dump_is_real(self):
        client = summary(draw(1, SPRITE))
        rows = classify.classify(client, summary(), client["draws"], [], None)
        self.assertFalse(rows[0]["known"])
        self.assertEqual(rows[0]["owner"], "sprite/UI/minimap")

    def test_same_pass_different_geometry_is_paired_and_real(self):
        client = summary(draw(1, PARTICLE))
        web = summary(draw(2, PARTICLE))
        frame = (Counter({"particle": 1}), Counter())
        rows = classify.classify(client, web, client["draws"], web["draws"], frame)
        self.assertEqual([r["known"] for r in rows], [False, False])
        self.assertIn("same pass, different geometry", rows[0]["reason"])
        self.assertEqual(rows[0]["owner"], "particles")


class FrameFile(unittest.TestCase):
    def test_counts_read_drawn_and_skipped_names(self):
        def record(pas, name):
            n = name.encode()
            return struct.pack("<4I", pas, 0, 0, 0) + struct.pack("<I", 0) + struct.pack("<4I", 0, 0, 0, 0) * 2 + struct.pack("<I", 0) + struct.pack("<I", len(n)) + n

        def chunk(kind, payload):
            z = zlib.compress(payload)
            return struct.pack("<3I", kind, len(payload), len(z)) + z

        body = b"ORF3" + struct.pack("<3I", 3, 10, 10) + struct.pack("<4f", 0, 0, 0, 0)
        body += chunk(4, record(3, "groundTextured")) + chunk(4, record(255, "groundTextured@offscreen")) + chunk(4, record(2, "sprite")) + struct.pack("<3I", 5, 0, 0)
        f = Path(tempfile.mkdtemp()) / "frame.orf"
        f.write_bytes(body)
        drawn, skipped = orf.counts(f)
        self.assertEqual(drawn, Counter({"groundTextured": 1, "sprite": 1}))
        self.assertEqual(skipped, Counter({"groundTextured@offscreen": 1}))


class Reorder(unittest.TestCase):
    def test_leftovers_with_equal_keys_are_paired(self):
        def d(eid, count):
            return {"eid": eid, "topology": "TriangleList", "count": count, "instances": 1}

        a = [d(1, 3), d(2, 6), d(3, 9)]
        b = [d(11, 9), d(12, 3), d(13, 6)]
        pairs, oa, ob, moved = diff.align(a, b)
        self.assertEqual((len(pairs), oa, ob), (3, [], []))
        self.assertTrue(moved)


if __name__ == "__main__":
    unittest.main()
