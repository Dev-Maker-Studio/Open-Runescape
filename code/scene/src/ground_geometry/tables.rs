pub(super) const OVERLAY_FACE_COUNT: [usize; 15] = [2, 1, 1, 1, 2, 2, 2, 1, 3, 3, 3, 2, 0, 4, 0];

pub(super) const UNDERLAY_FACE_COUNT: [usize; 15] = [0, 1, 2, 2, 1, 1, 2, 3, 1, 3, 3, 4, 2, 0, 4];

pub(super) const OVERLAY_FACE_COUNTS: [usize; 13] = [4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0];

pub(super) const UNDERLAY_FACE_COUNTS: [usize; 13] = [0, 2, 2, 2, 1, 1, 3, 3, 1, 3, 3, 4, 4];

pub(super) const OVERLAY_FACE_COUNT_BLENDED: [usize; 13] = [4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0];

pub(super) const UNDERLAY_FACE_COUNT_BLENDED: [usize; 13] = [0, 4, 3, 3, 1, 1, 3, 5, 1, 5, 3, 6, 4];

pub(super) const TILE_SIZE_DELTAS_X: [usize; 13] =
    [0, 256, 512, 512, 512, 256, 0, 0, 128, 256, 128, 384, 256];

pub(super) const TILE_SIZE_DELTAS_Y: [usize; 13] =
    [0, 0, 0, 256, 512, 512, 512, 256, 256, 384, 128, 128, 256];

pub(super) const OVERLAY_SHAPE_SUPPORTS_HEIGHT: [[bool; 13]; 15] = [
    [
        true, true, true, true, true, true, true, true, true, true, true, true, true,
    ],
    [
        true, true, true, false, false, false, true, true, false, false, false, false, true,
    ],
    [
        true, false, false, false, false, true, true, true, false, false, false, false, false,
    ],
    [
        false, false, true, true, true, true, false, false, false, false, false, false, false,
    ],
    [
        true, true, true, true, true, true, false, false, false, false, false, false, false,
    ],
    [
        true, true, true, false, false, true, true, true, false, false, false, false, false,
    ],
    [
        true, true, false, false, false, true, true, true, false, false, false, false, true,
    ],
    [
        true, true, false, false, false, false, false, true, false, false, false, false, false,
    ],
    [
        false, true, true, true, true, true, true, true, false, false, false, false, false,
    ],
    [
        true, false, false, false, true, true, true, true, true, true, false, false, false,
    ],
    [
        true, true, true, true, true, false, false, false, true, true, false, false, false,
    ],
    [
        true, true, true, false, false, false, false, false, false, false, true, true, false,
    ],
    [false; 13],
    [
        true, true, true, true, true, true, true, true, true, true, true, true, true,
    ],
    [false; 13],
];

pub(super) const SHAPE_VERTEX_A: &[&[usize]] = &[
    &[0, 2],
    &[0, 2],
    &[0, 0, 2],
    &[2, 0, 0],
    &[0, 2, 0],
    &[0, 0, 2],
    &[0, 5, 1, 4],
    &[0, 4, 4, 4],
    &[4, 4, 4, 0],
    &[6, 6, 6, 2, 2, 2],
    &[2, 2, 2, 6, 6, 6],
    &[0, 11, 6, 6, 6, 4],
    &[0, 2],
    &[0, 4, 4, 4],
    &[0, 4, 4, 4],
];

pub(super) const SHAPE_VERTEX_B: &[&[usize]] = &[
    &[2, 4],
    &[2, 4],
    &[5, 2, 4],
    &[4, 5, 2],
    &[2, 4, 5],
    &[5, 2, 4],
    &[1, 6, 2, 5],
    &[1, 6, 7, 1],
    &[6, 7, 1, 1],
    &[0, 8, 9, 8, 9, 4],
    &[8, 9, 4, 0, 8, 9],
    &[2, 10, 0, 10, 11, 11],
    &[2, 4],
    &[1, 6, 7, 1],
    &[1, 6, 7, 1],
];

pub(super) const SHAPE_VERTEX_C: &[&[usize]] = &[
    &[6, 6],
    &[6, 6],
    &[6, 5, 5],
    &[5, 6, 5],
    &[5, 5, 6],
    &[6, 5, 5],
    &[5, 0, 4, 1],
    &[7, 7, 1, 2],
    &[7, 1, 2, 7],
    &[8, 9, 4, 0, 8, 9],
    &[0, 8, 9, 8, 9, 4],
    &[11, 0, 10, 11, 4, 2],
    &[6, 6],
    &[7, 7, 1, 2],
    &[7, 7, 1, 2],
];

pub(super) const PATH_SQUARES: [[bool; 4]; 13] = [
    [false; 4],
    [false; 4],
    [false, false, true, false],
    [false, false, true, false],
    [false, false, true, false],
    [false, false, true, false],
    [true, false, true, false],
    [true, false, false, true],
    [true, false, false, true],
    [false; 4],
    [false; 4],
    [false; 4],
    [false; 4],
];

pub(super) const BLENDED_FACE_MAPPING: &[&[i32]] = &[
    &[0, 1, 2, 3],
    &[1, 2, 3, 0],
    &[1, 2, -1, 0],
    &[2, 0, -1, 1],
    &[0, 1, -1, 2],
    &[1, 2, -1, 0],
    &[-1, 4, -1, 1],
    &[-1, 1, 3, -1],
    &[-1, 0, 2, -1],
    &[3, 5, 2, 0],
    &[0, 2, 5, 3],
    &[0, 2, 3, 5],
    &[0, 1, 2, 3],
];

pub(super) const BLENDED_SHAPE_VERTEX_A: &[&[usize]] = &[
    &[0, 2, 4, 6],
    &[6, 0, 2, 4],
    &[6, 0, 2],
    &[2, 6, 0],
    &[0, 2, 6],
    &[6, 0, 2],
    &[5, 6, 0, 1, 2, 4],
    &[7, 2, 4, 4],
    &[2, 4, 4, 7],
    &[6, 6, 4, 0, 2, 2],
    &[0, 2, 2, 6, 6, 4],
    &[0, 2, 2, 4, 6, 6],
    &[0, 2, 4, 6],
];

pub(super) const BLENDED_SHAPE_VERTEX_B: &[&[usize]] = &[
    &[2, 4, 6, 0],
    &[0, 2, 4, 6],
    &[0, 2, 4],
    &[4, 0, 2],
    &[2, 4, 0],
    &[0, 2, 4],
    &[6, 0, 1, 2, 4, 5],
    &[0, 4, 7, 6],
    &[4, 7, 6, 0],
    &[0, 8, 6, 2, 9, 4],
    &[2, 9, 4, 0, 8, 6],
    &[2, 11, 4, 6, 10, 0],
    &[2, 4, 6, 0],
];

pub(super) const BLENDED_SHAPE_VERTEX_C: &[&[usize]] = &[
    &[12, 12, 12, 12],
    &[12, 12, 12, 12],
    &[5, 5, 5],
    &[5, 5, 5],
    &[5, 5, 5],
    &[5, 5, 5],
    &[12, 12, 12, 12, 12, 12],
    &[1, 1, 1, 7],
    &[1, 1, 7, 1],
    &[8, 9, 9, 8, 8, 9],
    &[8, 8, 9, 8, 9, 9],
    &[10, 10, 11, 11, 11, 10],
    &[12, 12, 12, 12],
];

pub(super) const PATH_BLENDINGS: [[bool; 4]; 13] = [
    [false; 4],
    [false, true, true, false],
    [true, false, true, false],
    [true, false, true, false],
    [false, false, true, false],
    [false, false, true, false],
    [true, false, true, false],
    [true, false, false, true],
    [true, false, false, true],
    [true, true, false, false],
    [false; 4],
    [false, true, false, true],
    [false; 4],
];

pub(super) const BLENDING_FACE_MAPPING: &[&[i32]] = &[
    &[0, 1, 2, 3],
    &[1, -1, -1, 0],
    &[-1, 2, -1, 0],
    &[-1, 0, -1, 2],
    &[0, 1, -1, 2],
    &[1, 2, -1, 0],
    &[-1, 4, -1, 1],
    &[-1, 3, 4, -1],
    &[-1, 0, 2, -1],
    &[-1, -1, 2, 0],
    &[0, 2, 5, 3],
    &[0, -1, 6, -1],
    &[0, 1, 2, 3],
];

pub(super) const BLENDING_SHAPE_VERTEX_A: &[&[usize]] = &[
    &[0, 2, 4, 6],
    &[6, 0, 2, 3, 5, 3],
    &[6, 0, 2, 4],
    &[2, 5, 6, 1],
    &[0, 2, 6],
    &[6, 0, 2],
    &[5, 6, 0, 1, 2, 4],
    &[7, 7, 1, 2, 4, 6],
    &[2, 4, 4, 7],
    &[6, 6, 4, 0, 1, 1, 3, 3],
    &[0, 2, 2, 6, 6, 4],
    &[0, 2, 2, 3, 7, 0, 4, 3],
    &[0, 2, 4, 6],
];

pub(super) const BLENDING_SHAPE_VERTEX_B: &[&[usize]] = &[
    &[2, 4, 6, 0],
    &[0, 2, 3, 5, 6, 4],
    &[0, 1, 4, 5],
    &[4, 6, 0, 2],
    &[2, 4, 0],
    &[0, 2, 4],
    &[6, 0, 1, 2, 4, 5],
    &[0, 1, 2, 4, 6, 7],
    &[4, 7, 6, 0],
    &[0, 8, 6, 1, 9, 2, 9, 4],
    &[2, 9, 4, 0, 8, 6],
    &[2, 11, 3, 7, 10, 10, 6, 6],
    &[2, 4, 6, 0],
];

pub(super) const BLENDING_SHAPE_VERTEX_C: &[&[usize]] = &[
    &[12, 12, 12, 12],
    &[12, 12, 12, 12, 12, 5],
    &[5, 5, 1, 1],
    &[5, 1, 1, 5],
    &[5, 5, 5],
    &[5, 5, 5],
    &[12, 12, 12, 12, 12, 12],
    &[1, 12, 12, 12, 12, 12],
    &[1, 1, 7, 1],
    &[8, 9, 9, 8, 8, 3, 1, 9],
    &[8, 8, 9, 8, 9, 9],
    &[10, 10, 11, 11, 11, 7, 3, 7],
    &[12, 12, 12, 12],
];
