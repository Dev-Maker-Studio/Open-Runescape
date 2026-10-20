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


if __name__ == "__main__":
    unittest.main()
