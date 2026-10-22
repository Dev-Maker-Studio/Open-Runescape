#![allow(clippy::too_many_arguments, clippy::int_plus_one)]

use std::cell::Cell;

pub const NW_OBJ: i32 = 0x1;
pub const N_OBJ: i32 = 0x2;
pub const NE_OBJ: i32 = 0x4;
pub const E_OBJ: i32 = 0x8;
pub const SE_OBJ: i32 = 0x10;
pub const S_OBJ: i32 = 0x20;
pub const SW_OBJ: i32 = 0x40;
pub const W_OBJ: i32 = 0x80;
pub const OBJ: i32 = 0x100;
pub const NW_BOUND: i32 = 0x200;
pub const N_BOUND: i32 = 0x400;
pub const NE_BOUND: i32 = 0x800;
pub const E_BOUND: i32 = 0x1000;
pub const SE_BOUND: i32 = 0x2000;
pub const S_BOUND: i32 = 0x4000;
pub const SW_BOUND: i32 = 0x8000;
pub const W_BOUND: i32 = 0x10000;
pub const BLOCKED_OBJ: i32 = 0x20000;
pub const BLOCKED_DECO: i32 = 0x40000;
pub const BLOCKED: i32 = 0x200000;
pub const NW_ALT_OBJ: i32 = 0x400000;
pub const N_ALT_OBJ: i32 = 0x800000;
pub const NE_ALT_OBJ: i32 = 0x1000000;
pub const E_ALT_OBJ: i32 = 0x2000000;
pub const SE_ALT_OBJ: i32 = 0x4000000;
pub const S_ALT_OBJ: i32 = 0x8000000;
pub const SW_ALT_OBJ: i32 = 0x10000000;
pub const W_ALT_OBJ: i32 = 0x20000000;
pub const ALT_OBJ: i32 = 0x40000000;
pub const EMPTY: i32 = -1;

const BOUND_SHIFT: u32 = 9;
const ALT_SHIFT: u32 = 22;

type BoundaryEntry = (i32, i32, i32);

fn boundary_entries(kind: i32, rotation: i32) -> &'static [BoundaryEntry] {
    match (kind, rotation) {
        (0, 0) => &[(0, 0, W_OBJ), (-1, 0, E_OBJ)],
        (0, 1) => &[(0, 0, N_OBJ), (0, 1, S_OBJ)],
        (0, 2) => &[(0, 0, E_OBJ), (1, 0, W_OBJ)],
        (0, 3) => &[(0, 0, S_OBJ), (0, -1, N_OBJ)],
        (1 | 3, 0) => &[(0, 0, NW_OBJ), (-1, 1, SE_OBJ)],
        (1 | 3, 1) => &[(0, 0, NE_OBJ), (1, 1, SW_OBJ)],
        (1 | 3, 2) => &[(0, 0, SE_OBJ), (1, -1, NW_OBJ)],
        (1 | 3, 3) => &[(0, 0, SW_OBJ), (-1, -1, NE_OBJ)],
        (2, 0) => &[(0, 0, N_OBJ | W_OBJ), (-1, 0, E_OBJ), (0, 1, S_OBJ)],
        (2, 1) => &[(0, 0, N_OBJ | E_OBJ), (0, 1, S_OBJ), (1, 0, W_OBJ)],
        (2, 2) => &[(0, 0, E_OBJ | S_OBJ), (1, 0, W_OBJ), (0, -1, N_OBJ)],
        (2, 3) => &[(0, 0, S_OBJ | W_OBJ), (0, -1, N_OBJ), (-1, 0, E_OBJ)],
        _ => &[],
    }
}

pub fn not_flagged(value: i32, mask: i32) -> bool {
    value & mask == 0
}

pub fn can_reach(
    src_x: i32,
    src_y: i32,
    size_x: i32,
    size_y: i32,
    dst_x: i32,
    dst_y: i32,
    dst_size_x: i32,
    dst_size_y: i32,
) -> bool {
    (src_x < dst_x + dst_size_x && dst_x < src_x + size_x)
        && (src_y < dst_y + dst_size_y && dst_y < size_y + src_y)
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CollisionMap {
    flags: Vec<i32>,
    pub offset_x: i32,
    pub offset_y: i32,
    pub size_x: i32,
    pub size_y: i32,
    fault: Cell<bool>,
}

impl CollisionMap {
    pub fn new(offset_x: i32, offset_y: i32, size_x: i32, size_y: i32) -> Self {
        let cells = (size_x.max(0) as usize) * (size_y.max(0) as usize);
        Self {
            flags: vec![0; cells],
            offset_x,
            offset_y,
            size_x,
            size_y,
            fault: Cell::new(false),
        }
    }

    pub fn create_clip_map(scene_size_x: i32, scene_size_y: i32) -> Self {
        let mut map = Self::new(-1, -1, scene_size_x + 1 + 5, scene_size_y + 1 + 5);
        map.reset();
        map
    }

    pub fn faulted(&self) -> bool {
        self.fault.get()
    }

    pub fn clear_fault(&self) {
        self.fault.set(false);
    }

    pub fn raw(&self) -> &[i32] {
        &self.flags
    }

    pub fn get(&self, x: i32, y: i32) -> Option<i32> {
        self.index(x, y).map(|i| self.flags[i])
    }

    fn index(&self, x: i32, y: i32) -> Option<usize> {
        if x >= 0 && x < self.size_x && y >= 0 && y < self.size_y {
            Some(x as usize * self.size_y as usize + y as usize)
        } else {
            None
        }
    }

    fn at(&self, x: i32, y: i32) -> i32 {
        match self.index(x, y) {
            Some(i) => self.flags[i],
            None => {
                self.fault.set(true);
                0
            }
        }
    }

    pub fn clear_of(&self, x: i32, y: i32, mask: i32) -> bool {
        not_flagged(self.at(x, y), mask)
    }

    pub fn reset(&mut self) {
        for x in 0..self.size_x {
            for y in 0..self.size_y {
                let value = if x != 0 && y != 0 && x < self.size_x - 5 && y < self.size_y - 5 {
                    BLOCKED
                } else {
                    EMPTY
                };
                let i = x as usize * self.size_y as usize + y as usize;
                self.flags[i] = value;
            }
        }
    }

    pub fn add_flag(&mut self, x: i32, y: i32, flag: i32) {
        if self.fault.get() {
            return;
        }
        match self.index(x, y) {
            Some(i) => self.flags[i] |= flag,
            None => self.fault.set(true),
        }
    }

    pub fn remove_flag(&mut self, x: i32, y: i32, flag: i32) {
        if self.fault.get() {
            return;
        }
        match self.index(x, y) {
            Some(i) => self.flags[i] &= !flag,
            None => self.fault.set(true),
        }
    }

    pub fn flag_blocked_object(
        &mut self,
        x: i32,
        y: i32,
        size_x: i32,
        size_y: i32,
        blocked: bool,
        not_alt: bool,
    ) {
        let mut flag = OBJ;
        if blocked {
            flag |= BLOCKED_OBJ;
        }
        if not_alt {
            flag |= ALT_OBJ;
        }
        let adjusted_x = x - self.offset_x;
        let adjusted_y = y - self.offset_y;
        for tile_x in adjusted_x..adjusted_x + size_x {
            if tile_x >= 0 && tile_x < self.size_x {
                for tile_y in adjusted_y..adjusted_y + size_y {
                    if tile_y >= 0 && tile_y < self.size_y {
                        self.add_flag(tile_x, tile_y, flag);
                    }
                }
            }
        }
    }

    pub fn unflag_blocked(
        &mut self,
        x: i32,
        y: i32,
        size_x: i32,
        size_y: i32,
        rotation: i32,
        blocked: bool,
        not_alt: bool,
    ) {
        let (mut size_x1, mut size_y1) = (size_x, size_y);
        let mut flag = OBJ;
        if blocked {
            flag |= BLOCKED_OBJ;
        }
        if not_alt {
            flag |= ALT_OBJ;
        }
        let adjusted_x = x - self.offset_x;
        let adjusted_y = y - self.offset_y;
        if rotation == 1 || rotation == 3 {
            std::mem::swap(&mut size_x1, &mut size_y1);
        }
        for tile_x in adjusted_x..size_x1 + adjusted_x {
            if tile_x >= 0 && tile_x < self.size_x {
                for tile_y in adjusted_y..adjusted_y + size_y1 {
                    if tile_y >= 0 && tile_y < self.size_y {
                        self.remove_flag(tile_x, tile_y, flag);
                    }
                }
            }
        }
    }

    fn boundary_phase(
        &mut self,
        adjusted_x: i32,
        adjusted_y: i32,
        kind: i32,
        rotation: i32,
        shift: u32,
        add: bool,
    ) {
        for &(dx, dy, mask) in boundary_entries(kind, rotation) {
            let flag = mask << shift;
            if add {
                self.add_flag(adjusted_x + dx, adjusted_y + dy, flag);
            } else {
                self.remove_flag(adjusted_x + dx, adjusted_y + dy, flag);
            }
        }
    }

    pub fn flag_boundary(
        &mut self,
        x: i32,
        y: i32,
        kind: i32,
        rotation: i32,
        blocks: bool,
        alt: bool,
    ) {
        let ax = x - self.offset_x;
        let ay = y - self.offset_y;
        self.boundary_phase(ax, ay, kind, rotation, 0, true);
        if blocks {
            self.boundary_phase(ax, ay, kind, rotation, BOUND_SHIFT, true);
        }
        if alt {
            self.boundary_phase(ax, ay, kind, rotation, ALT_SHIFT, true);
        }
    }

    pub fn unflag_boundary(
        &mut self,
        x: i32,
        y: i32,
        kind: i32,
        rotation: i32,
        blocks: bool,
        not_alternative: bool,
    ) {
        let ax = x - self.offset_x;
        let ay = y - self.offset_y;
        self.boundary_phase(ax, ay, kind, rotation, 0, false);
        if blocks {
            self.boundary_phase(ax, ay, kind, rotation, BOUND_SHIFT, false);
        }
        if not_alternative {
            self.boundary_phase(ax, ay, kind, rotation, ALT_SHIFT, false);
        }
    }

    pub fn flag_blocked_decoration(&mut self, tile_x: i32, tile_y: i32) {
        self.add_flag(tile_x - self.offset_x, tile_y - self.offset_y, BLOCKED_DECO);
    }

    pub fn unflag_floor_decoration(&mut self, tile_x: i32, tile_y: i32) {
        self.remove_flag(tile_x - self.offset_x, tile_y - self.offset_y, BLOCKED_DECO);
    }

    pub fn set_unwalkable(&mut self, tile_x: i32, tile_y: i32) {
        self.add_flag(tile_x - self.offset_x, tile_y - self.offset_y, BLOCKED);
    }

    pub fn unblock(&mut self, scene_x: i32, scene_y: i32) {
        self.remove_flag(scene_x - self.offset_x, scene_y - self.offset_y, BLOCKED);
    }

    #[allow(clippy::too_many_arguments)]
    pub fn walkable(
        &self,
        src_x: i32,
        src_y: i32,
        src_size_x: i32,
        src_size_y: i32,
        dst_x: i32,
        dst_y: i32,
        dst_size_x: i32,
        dst_size_y: i32,
        flag: i32,
    ) -> bool {
        let src_max_x = src_size_x + src_x;
        let src_max_y = src_y + src_size_y;
        let dst_max_x = dst_x + dst_size_x;
        let dst_max_y = dst_size_y + dst_y;
        if dst_max_x == src_x && not_flagged(flag, N_OBJ) {
            for i in src_y.max(dst_y)..src_max_y.min(dst_max_y) {
                if self.clear_of(dst_max_x - 1 - self.offset_x, i - self.offset_y, E_OBJ) {
                    return true;
                }
            }
        } else if dst_x == src_max_x && not_flagged(flag, E_OBJ) {
            for i in src_y.max(dst_y)..src_max_y.min(dst_max_y) {
                if self.clear_of(dst_x - self.offset_x, i - self.offset_y, W_OBJ) {
                    return true;
                }
            }
        } else if dst_max_y == src_y && not_flagged(flag, NW_OBJ) {
            for i in src_x.max(dst_x)..src_max_x.min(dst_max_x) {
                if self.clear_of(i - self.offset_x, dst_max_y - 1 - self.offset_y, N_OBJ) {
                    return true;
                }
            }
        } else if src_max_y == dst_y && not_flagged(flag, NE_OBJ) {
            for i in src_x.max(dst_x)..src_max_x.min(dst_max_x) {
                if self.clear_of(i - self.offset_x, dst_y - self.offset_y, S_OBJ) {
                    return true;
                }
            }
        }
        false
    }

    pub fn can_reach_loc(
        &self,
        src_x: i32,
        src_y: i32,
        size: i32,
        dst_x: i32,
        dst_y: i32,
        shape: i32,
        rotation: i32,
    ) -> bool {
        if size == 1 {
            if dst_x == src_x && dst_y == src_y {
                return true;
            }
        } else if dst_x >= src_x && dst_x <= size + src_x - 1 && dst_y <= size + dst_y - 1 {
            return true;
        }
        let sx = src_x - self.offset_x;
        let sy = src_y - self.offset_y;
        let dx = dst_x - self.offset_x;
        let dy = dst_y - self.offset_y;
        let full = |bit: i32| bit | OBJ | BLOCKED_DECO | BLOCKED;
        if size == 1 {
            let w = dx - 1 == sx && dy == sy;
            let e = dx + 1 == sx && dy == sy;
            let n = dx == sx && dy + 1 == sy;
            let s = dx == sx && sy == dy - 1;
            let here = |bit: i32| self.clear_of(sx, sy, full(bit));
            match shape {
                0 => match rotation {
                    0 => w || (n && here(S_OBJ)) || (s && here(N_OBJ)),
                    1 => n || (w && here(E_OBJ)) || (e && here(W_OBJ)),
                    2 => e || (n && here(S_OBJ)) || (s && here(N_OBJ)),
                    3 => s || (w && here(E_OBJ)) || (e && here(W_OBJ)),
                    _ => false,
                },
                2 => match rotation {
                    0 => w || n || (e && here(W_OBJ)) || (s && here(N_OBJ)),
                    1 => (w && here(E_OBJ)) || n || e || (s && here(N_OBJ)),
                    2 => (w && here(E_OBJ)) || (n && here(S_OBJ)) || e || s,
                    3 => w || (n && here(S_OBJ)) || (e && here(W_OBJ)) || s,
                    _ => false,
                },
                9 => {
                    (n && self.clear_of(sx, sy, S_OBJ))
                        || (s && self.clear_of(sx, sy, N_OBJ))
                        || (w && self.clear_of(sx, sy, E_OBJ))
                        || (e && self.clear_of(sx, sy, W_OBJ))
                }
                _ => false,
            }
        } else {
            let end_x = size + sx - 1;
            let end_y = size + sy - 1;
            let in_x = dx >= sx && dx <= end_x;
            let in_y = dy >= sy && dy <= end_y;
            let wst = dx - size == sx && in_y;
            let est = dx + 1 == sx && in_y;
            let nn = in_x && dy + 1 == sy;
            let ss = in_x && dy - size == sy;
            let wst_f = || self.clear_of(end_x, dy, full(E_OBJ));
            let est_f = || self.clear_of(sx, dy, full(W_OBJ));
            let nn_f = || self.clear_of(dx, sy, full(S_OBJ));
            let ss_f = || self.clear_of(dx, end_y, full(N_OBJ));
            match shape {
                0 => match rotation {
                    0 => wst || (nn && nn_f()) || (ss && ss_f()),
                    1 => nn || (wst && wst_f()) || (est && est_f()),
                    2 => est || (nn && nn_f()) || (ss && ss_f()),
                    3 => ss || (wst && wst_f()) || (est && est_f()),
                    _ => false,
                },
                2 => match rotation {
                    0 => wst || nn || (est && est_f()) || (ss && ss_f()),
                    1 => (wst && wst_f()) || nn || est || (ss && ss_f()),
                    2 => (wst && wst_f()) || (nn && nn_f()) || est || ss,
                    3 => wst || (nn && nn_f()) || (est && est_f()) || ss,
                    _ => false,
                },
                9 => (nn && nn_f()) || (ss && ss_f()) || (wst && wst_f()) || (est && est_f()),
                _ => false,
            }
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn can_reach_npc(
        &self,
        src_x: i32,
        src_y: i32,
        size: i32,
        dst_x: i32,
        dst_y: i32,
        dst_size_x: i32,
        dst_size_y: i32,
        clipping_flag: i32,
    ) -> bool {
        if size > 1 {
            return can_reach(
                src_x, src_y, size, size, dst_x, dst_y, dst_size_x, dst_size_y,
            ) || self.walkable(
                src_x,
                src_y,
                size,
                size,
                dst_x,
                dst_y,
                dst_size_x,
                dst_size_y,
                clipping_flag,
            );
        }
        let end_x = dst_x + dst_size_x - 1;
        let end_y = dst_y + dst_size_y - 1;
        let here = |bit: i32| self.clear_of(src_x - self.offset_x, src_y - self.offset_y, bit);
        (src_x >= dst_x && src_x <= end_x && src_y >= dst_y && src_y <= end_y)
            || (dst_x - 1 == src_x
                && src_y >= dst_y
                && src_y <= end_y
                && here(E_OBJ)
                && not_flagged(clipping_flag, E_OBJ))
            || (end_x + 1 == src_x
                && src_y >= dst_y
                && src_y <= end_y
                && here(W_OBJ)
                && not_flagged(clipping_flag, N_OBJ))
            || (src_y == dst_y - 1
                && src_x >= dst_x
                && src_x <= end_x
                && here(N_OBJ)
                && not_flagged(clipping_flag, NE_OBJ))
            || (src_y == end_y + 1
                && src_x >= dst_x
                && src_x <= end_x
                && here(S_OBJ)
                && not_flagged(clipping_flag, NW_OBJ))
    }
}
