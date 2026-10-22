import sys
import unittest

import numpy as np
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "py"))
import frame


class ScissorOrigin(unittest.TestCase):
    def test_gl_bottom_left_becomes_top_left(self):
        self.assertEqual(frame.top_left_y(165, 334, 503), 4)
        self.assertEqual(frame.top_left_y(341, 154, 503), 8)

    def test_full_height_scissor_stays_at_the_top(self):
        self.assertEqual(frame.top_left_y(0, 503, 503), 0)

    def test_flip_is_its_own_inverse(self):
        self.assertEqual(frame.top_left_y(frame.top_left_y(37, 90, 503), 90, 503), 37)


class IndexOrigin(unittest.TestCase):
    def test_same_mesh_at_any_buffer_offset_hashes_equal(self):
        a = frame.relative_indices(np.array([53931, 53932, 53933, 53931]))
        b = frame.relative_indices(np.array([0, 1, 2, 0]))
        self.assertEqual(a.tolist(), b.tolist())

    def test_empty_draw_is_empty(self):
        self.assertEqual(len(frame.relative_indices(np.array([], dtype=np.int64))), 0)


class ScreenSpace(unittest.TestCase):
    def test_real_clip_rectangle_flips_to_top_left(self):
        client = np.array([[4.0, 165.0, 516.0, 499.0], [0.0, 0.0, 765.0, 503.0]])
        self.assertEqual(frame.top_left_clip(client, 503).tolist(), [[4.0, 4.0, 516.0, 338.0], [0.0, 0.0, 765.0, 503.0]])

    def test_clip_flip_is_its_own_inverse(self):
        client = np.array([[4.0, 165.0, 516.0, 499.0]])
        self.assertEqual(frame.top_left_clip(frame.top_left_clip(client, 503), 503).tolist(), client.tolist())

    def test_padded_components_hash_like_the_unpadded_attribute(self):
        two = np.array([[24064.0, 22016.0], [24576.0, 22016.0]])
        three = np.array([[24064.0, 22016.0, 0.0], [24576.0, 22016.0, 0.0]])
        self.assertEqual(frame.trim_zero_components(three).tolist(), two.tolist())
        self.assertEqual(frame.trim_zero_components(two).tolist(), two.tolist())

    def test_all_zero_attribute_keeps_one_component(self):
        self.assertEqual(frame.trim_zero_components(np.zeros((3, 4))).shape, (3, 1))


class Fmt:
    def __init__(self, comps, width, comp_type=None, name="X"):
        self.compCount, self.compByteWidth, self.compType, self.type, self._name = comps, width, comp_type, frame.rd.ResourceFormatType.Regular, name

    def Name(self):
        return self._name


class Tex:
    def __init__(self, width, height):
        self.width, self.height = width, height


class TextureReadback(unittest.TestCase):
    def test_single_channel_reads_back_like_glgetteximage(self):
        px = frame.rgba8(Fmt(1, 1), np.array([10, 200], np.uint8), Tex(2, 1))
        self.assertEqual(px.tolist(), [[10, 0, 0, 255], [200, 0, 0, 255]])

    def test_half_float_is_rounded_to_bytes(self):
        a = np.array([1.0, 0.5, 0.0, 1.0], "<f2").view(np.uint8)
        px = frame.rgba8(Fmt(4, 2, frame.rd.CompType.Float), a, Tex(1, 1))
        self.assertEqual(px.tolist(), [[255, 128, 0, 255]])

    def test_bgra_is_swizzled_and_rgba_is_not(self):
        a = np.array([1, 2, 3, 4], np.uint8)
        self.assertEqual(frame.rgba8(Fmt(4, 1, name="B8G8R8A8_UNORM"), a, Tex(1, 1)).tolist(), [[3, 2, 1, 4]])
        self.assertEqual(frame.rgba8(Fmt(4, 1, name="R8G8B8A8_UNORM"), a, Tex(1, 1)).tolist(), [[1, 2, 3, 4]])

    def test_unknown_layout_is_left_to_the_raw_hash(self):
        self.assertIsNone(frame.rgba8(Fmt(1, 4), np.zeros(4, np.uint8), Tex(1, 1)))


if __name__ == "__main__":
    unittest.main()
