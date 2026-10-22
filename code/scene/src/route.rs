#![allow(clippy::too_many_arguments)]

use crate::collision::{
    can_reach, CollisionMap, ALT_OBJ, BLOCKED, BLOCKED_DECO, E_ALT_OBJ, NE_ALT_OBJ, NW_ALT_OBJ,
    N_ALT_OBJ, SE_ALT_OBJ, SW_ALT_OBJ, S_ALT_OBJ, W_ALT_OBJ,
};

pub const GRAPH_SIZE: i32 = 128;
pub const QUEUE_MASK: usize = 4095;
pub const UNREACHED_DISTANCE: i32 = 99999999;
pub const SOURCE_DIRECTION: i32 = 99;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct RouteFault;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Strategy {
    FixedTile {
        x: i32,
        y: i32,
    },
    GroundItem {
        x: i32,
        y: i32,
    },
    Player {
        x: i32,
        y: i32,
        size_x: i32,
        size_y: i32,
        clipping_flag: i32,
    },
    Npc {
        x: i32,
        y: i32,
        size_x: i32,
        size_y: i32,
        clipping_flag: i32,
    },
    Wall {
        x: i32,
        y: i32,
        size_x: i32,
        size_y: i32,
        shape: i32,
        rotation: i32,
    },
    Normal {
        x: i32,
        y: i32,
        shape: i32,
        rotation: i32,
    },
}

impl Strategy {
    pub fn destination(&self) -> (i32, i32, i32, i32) {
        match *self {
            Strategy::FixedTile { x, y } | Strategy::GroundItem { x, y } => (x, y, 1, 1),
            Strategy::Player {
                x,
                y,
                size_x,
                size_y,
                ..
            }
            | Strategy::Npc {
                x,
                y,
                size_x,
                size_y,
                ..
            }
            | Strategy::Wall {
                x,
                y,
                size_x,
                size_y,
                ..
            } => (x, y, size_x, size_y),
            Strategy::Normal { x, y, .. } => (x, y, 0, 0),
        }
    }

    pub fn can_exit(&self, size: i32, x: i32, y: i32, clip: &CollisionMap) -> bool {
        match *self {
            Strategy::FixedTile { x: dx, y: dy } => x == dx && y == dy,
            Strategy::GroundItem { x: dx, y: dy } => can_reach(x, y, size, size, dx, dy, 1, 1),
            Strategy::Player {
                x: dx,
                y: dy,
                size_x,
                size_y,
                clipping_flag,
            } => clip.walkable(x, y, size, size, dx, dy, size_x, size_y, clipping_flag),
            Strategy::Npc {
                x: dx,
                y: dy,
                size_x,
                size_y,
                clipping_flag,
            } => clip.can_reach_npc(x, y, size, dx, dy, size_x, size_y, clipping_flag),
            Strategy::Wall {
                x: dx,
                y: dy,
                shape,
                rotation,
                ..
            }
            | Strategy::Normal {
                x: dx,
                y: dy,
                shape,
                rotation,
            } => clip.can_reach_loc(x, y, size, dx, dy, shape, rotation),
        }
    }
}

const A: i32 = BLOCKED_DECO | BLOCKED | ALT_OBJ;
const NW: i32 = NW_ALT_OBJ;
const N: i32 = N_ALT_OBJ;
const NE: i32 = NE_ALT_OBJ;
const E: i32 = E_ALT_OBJ;
const SE: i32 = SE_ALT_OBJ;
const S: i32 = S_ALT_OBJ;
const SW: i32 = SW_ALT_OBJ;
const W: i32 = W_ALT_OBJ;

struct Step {
    dx: i32,
    dy: i32,
    direction: i32,
    checks: &'static [(i32, i32, i32)],
}

const S1_STEPS: [Step; 8] = [
    Step {
        dx: -1,
        dy: 0,
        direction: 2,
        checks: &[(-1, 0, A | E)],
    },
    Step {
        dx: 1,
        dy: 0,
        direction: 8,
        checks: &[(1, 0, A | W)],
    },
    Step {
        dx: 0,
        dy: -1,
        direction: 1,
        checks: &[(0, -1, A | N)],
    },
    Step {
        dx: 0,
        dy: 1,
        direction: 4,
        checks: &[(0, 1, A | S)],
    },
    Step {
        dx: -1,
        dy: -1,
        direction: 3,
        checks: &[(-1, -1, A | N | NE | E), (-1, 0, A | E), (0, -1, A | N)],
    },
    Step {
        dx: 1,
        dy: -1,
        direction: 9,
        checks: &[(1, -1, A | NW | N | W), (1, 0, A | W), (0, -1, A | N)],
    },
    Step {
        dx: -1,
        dy: 1,
        direction: 6,
        checks: &[(-1, 1, A | E | SE | S), (-1, 0, A | E), (0, 1, A | S)],
    },
    Step {
        dx: 1,
        dy: 1,
        direction: 12,
        checks: &[(1, 1, A | S | SW | W), (1, 0, A | W), (0, 1, A | S)],
    },
];

const S2_STEPS: [Step; 8] = [
    Step {
        dx: -1,
        dy: 0,
        direction: 2,
        checks: &[(-1, 0, A | N | NE | E), (-1, 1, A | E | SE | S)],
    },
    Step {
        dx: 1,
        dy: 0,
        direction: 8,
        checks: &[(2, 0, A | NW | N | W), (2, 1, A | S | SW | W)],
    },
    Step {
        dx: 0,
        dy: -1,
        direction: 1,
        checks: &[(0, -1, A | N | NE | E), (1, -1, A | NW | N | W)],
    },
    Step {
        dx: 0,
        dy: 1,
        direction: 4,
        checks: &[(0, 2, A | E | SE | S), (1, 2, A | S | SW | W)],
    },
    Step {
        dx: -1,
        dy: -1,
        direction: 3,
        checks: &[
            (-1, 0, A | N | NE | E | SE | S),
            (-1, -1, A | N | NE | E),
            (0, -1, A | NW | N | NE | E | W),
        ],
    },
    Step {
        dx: 1,
        dy: -1,
        direction: 9,
        checks: &[
            (1, -1, A | NW | N | NE | E | W),
            (2, -1, A | NW | N | W),
            (2, 0, A | NW | N | S | SW | W),
        ],
    },
    Step {
        dx: -1,
        dy: 1,
        direction: 6,
        checks: &[
            (-1, 1, A | N | NE | E | SE | S),
            (-1, 2, A | E | SE | S),
            (0, 2, A | E | SE | S | SW | W),
        ],
    },
    Step {
        dx: 1,
        dy: 1,
        direction: 12,
        checks: &[
            (1, 2, A | E | SE | S | SW | W),
            (2, 2, A | S | SW | W),
            (2, 1, A | NW | N | S | SW | W),
        ],
    },
];

fn graph_index(x: i32, y: i32) -> usize {
    x as usize * GRAPH_SIZE as usize + y as usize
}

pub struct RouteFinder {
    pub exit_x: i32,
    pub exit_y: i32,
    directions: Vec<i32>,
    distances: Vec<i32>,
    buffer_x: Vec<i32>,
    buffer_y: Vec<i32>,
}

impl Default for RouteFinder {
    fn default() -> Self {
        Self::new()
    }
}

struct Queue {
    read: usize,
    write: usize,
}

impl RouteFinder {
    pub fn new() -> Self {
        let cells = (GRAPH_SIZE * GRAPH_SIZE) as usize;
        Self {
            exit_x: 0,
            exit_y: 0,
            directions: vec![0; cells],
            distances: vec![0; cells],
            buffer_x: vec![0; QUEUE_MASK + 1],
            buffer_y: vec![0; QUEUE_MASK + 1],
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn find_route(
        &mut self,
        src_x: i32,
        src_y: i32,
        entity_size: i32,
        strategy: &Strategy,
        clip: &CollisionMap,
        find_alternative: bool,
        steps_x: &mut [i32],
        steps_y: &mut [i32],
    ) -> Result<i32, RouteFault> {
        self.directions.fill(0);
        self.distances.fill(UNREACHED_DISTANCE);
        clip.clear_fault();
        let reached = match entity_size {
            1 => self.table_search(src_x, src_y, 1, &S1_STEPS, 127, strategy, clip)?,
            2 => self.table_search(src_x, src_y, 2, &S2_STEPS, 126, strategy, clip)?,
            _ => self.sized_search(src_x, src_y, entity_size, strategy, clip)?,
        };
        let base_x = src_x - 64;
        let base_y = src_y - 64;
        let mut path_x = self.exit_x;
        let mut path_y = self.exit_y;
        if !reached {
            if !find_alternative {
                return Ok(-1);
            }
            let mut nearest = i32::MAX;
            let mut best_steps = i32::MAX;
            let (dest_x, dest_y, dest_size_x, dest_size_y) = strategy.destination();
            for x in dest_x - 10..=dest_x + 10 {
                for y in dest_y - 10..=dest_y + 10 {
                    let gx = x - base_x;
                    let gy = y - base_y;
                    if gx >= 0
                        && gy >= 0
                        && gx < 128
                        && gy < 128
                        && self.distances[graph_index(gx, gy)] < 100
                    {
                        let mut dx = 0;
                        if x < dest_x {
                            dx = dest_x - x;
                        } else if x > dest_size_x + dest_x - 1 {
                            dx = x - (dest_x + dest_size_x - 1);
                        }
                        let mut dy = 0;
                        if y < dest_y {
                            dy = dest_y - y;
                        } else if y > dest_size_y + dest_y - 1 {
                            dy = y - (dest_y + dest_size_y - 1);
                        }
                        let dist_sq = dx * dx + dy * dy;
                        let distance = self.distances[graph_index(gx, gy)];
                        if dist_sq < nearest || (dist_sq == nearest && distance < best_steps) {
                            nearest = dist_sq;
                            best_steps = distance;
                            path_x = x;
                            path_y = y;
                        }
                    }
                }
            }
            if nearest == i32::MAX {
                return Ok(-1);
            }
        }
        if src_x == path_x && path_y == src_y {
            return Ok(0);
        }
        self.buffer_x[0] = path_x;
        self.buffer_y[0] = path_y;
        let mut step_count = 1usize;
        let mut last_direction = self.directions[graph_index(path_x - base_x, path_y - base_y)];
        let mut direction = last_direction;
        while src_x != path_x || path_y != src_y {
            if last_direction != direction {
                last_direction = direction;
                self.buffer_x[step_count] = path_x;
                self.buffer_y[step_count] = path_y;
                step_count += 1;
            }
            if direction & 0x2 != 0 {
                path_x += 1;
            } else if direction & 0x8 != 0 {
                path_x -= 1;
            }
            if direction & 0x1 != 0 {
                path_y += 1;
            } else if direction & 0x4 != 0 {
                path_y -= 1;
            }
            direction = self.directions[graph_index(path_x - base_x, path_y - base_y)];
        }
        let mut step_index = 0usize;
        while step_count > 0 {
            step_count -= 1;
            steps_x[step_index] = self.buffer_x[step_count];
            steps_y[step_index] = self.buffer_y[step_count];
            step_index += 1;
            if step_index >= steps_x.len() {
                break;
            }
        }
        Ok(step_index as i32)
    }

    fn start(&mut self, src_x: i32, src_y: i32) -> Queue {
        self.directions[graph_index(64, 64)] = SOURCE_DIRECTION;
        self.distances[graph_index(64, 64)] = 0;
        self.buffer_x[0] = src_x;
        self.buffer_y[0] = src_y;
        Queue { read: 0, write: 1 }
    }

    #[allow(clippy::too_many_arguments)]
    fn push(
        &mut self,
        queue: &mut Queue,
        x: i32,
        y: i32,
        gx: i32,
        gy: i32,
        direction: i32,
        distance: i32,
    ) {
        self.buffer_x[queue.write] = x;
        self.buffer_y[queue.write] = y;
        queue.write = (queue.write + 1) & QUEUE_MASK;
        self.directions[graph_index(gx, gy)] = direction;
        self.distances[graph_index(gx, gy)] = distance;
    }

    #[allow(clippy::too_many_arguments)]
    fn table_search(
        &mut self,
        src_x: i32,
        src_y: i32,
        size: i32,
        steps: &[Step; 8],
        limit: i32,
        strategy: &Strategy,
        clip: &CollisionMap,
    ) -> Result<bool, RouteFault> {
        let base_x = src_x - 64;
        let base_y = src_y - 64;
        let mut queue = self.start(src_x, src_y);
        let mut cur_x = src_x;
        let mut cur_y = src_y;
        while queue.read != queue.write {
            cur_x = self.buffer_x[queue.read];
            cur_y = self.buffer_y[queue.read];
            queue.read = (queue.read + 1) & QUEUE_MASK;
            let gx = cur_x - base_x;
            let gy = cur_y - base_y;
            let clip_x = cur_x - clip.offset_x;
            let clip_y = cur_y - clip.offset_y;
            let exits = strategy.can_exit(size, cur_x, cur_y, clip);
            if clip.faulted() {
                return Err(RouteFault);
            }
            if exits {
                self.exit_x = cur_x;
                self.exit_y = cur_y;
                return Ok(true);
            }
            let next = self.distances[graph_index(gx, gy)] + 1;
            for step in steps {
                let x_ok = match step.dx {
                    -1 => gx > 0,
                    1 => gx < limit,
                    _ => true,
                };
                let y_ok = match step.dy {
                    -1 => gy > 0,
                    1 => gy < limit,
                    _ => true,
                };
                if !(x_ok && y_ok) {
                    continue;
                }
                let ngx = gx + step.dx;
                let ngy = gy + step.dy;
                if self.directions[graph_index(ngx, ngy)] != 0 {
                    continue;
                }
                let mut open = true;
                for &(ox, oy, mask) in step.checks {
                    if !clip.clear_of(clip_x + ox, clip_y + oy, mask) {
                        open = false;
                        break;
                    }
                }
                if clip.faulted() {
                    return Err(RouteFault);
                }
                if open {
                    self.push(
                        &mut queue,
                        cur_x + step.dx,
                        cur_y + step.dy,
                        ngx,
                        ngy,
                        step.direction,
                        next,
                    );
                }
            }
        }
        self.exit_x = cur_x;
        self.exit_y = cur_y;
        Ok(false)
    }

    fn sized_search(
        &mut self,
        src_x: i32,
        src_y: i32,
        n: i32,
        strategy: &Strategy,
        clip: &CollisionMap,
    ) -> Result<bool, RouteFault> {
        let base_x = src_x - 64;
        let base_y = src_y - 64;
        let mut queue = self.start(src_x, src_y);
        let mut cur_x = src_x;
        let mut cur_y = src_y;
        let edge = 128 - n;
        let side = |count: i32, f: &dyn Fn(i32) -> bool| -> bool {
            let mut i = 1;
            loop {
                if i >= count {
                    return true;
                }
                if !f(i) {
                    return false;
                }
                i += 1;
            }
        };
        while queue.read != queue.write {
            cur_x = self.buffer_x[queue.read];
            cur_y = self.buffer_y[queue.read];
            queue.read = (queue.read + 1) & QUEUE_MASK;
            let gx = cur_x - base_x;
            let gy = cur_y - base_y;
            let cx = cur_x - clip.offset_x;
            let cy = cur_y - clip.offset_y;
            let exits = strategy.can_exit(n, cur_x, cur_y, clip);
            if clip.faulted() {
                return Err(RouteFault);
            }
            if exits {
                self.exit_x = cur_x;
                self.exit_y = cur_y;
                return Ok(true);
            }
            let next = self.distances[graph_index(gx, gy)] + 1;
            let ok = |dx: i32, dy: i32, mask: i32| clip.clear_of(cx + dx, cy + dy, mask);
            let free =
                |this: &Self, dx: i32, dy: i32| this.directions[graph_index(gx + dx, gy + dy)] == 0;

            if gx > 0
                && free(self, -1, 0)
                && ok(-1, 0, A | N | NE | E)
                && ok(-1, n - 1, A | E | SE | S)
                && side(n - 1, &|i| ok(-1, i, A | N | NE | E | SE | S))
            {
                self.push(&mut queue, cur_x - 1, cur_y, gx - 1, gy, 2, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gx < edge
                && free(self, 1, 0)
                && ok(n, 0, A | NW | N | W)
                && ok(n, n - 1, A | S | SW | W)
                && side(n - 1, &|i| ok(n, i, A | NW | N | S | SW | W))
            {
                self.push(&mut queue, cur_x + 1, cur_y, gx + 1, gy, 8, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gy > 0
                && free(self, 0, -1)
                && ok(0, -1, A | N | NE | E)
                && ok(n - 1, -1, A | NW | N | W)
                && side(n - 1, &|i| ok(i, -1, A | NW | N | NE | E | W))
            {
                self.push(&mut queue, cur_x, cur_y - 1, gx, gy - 1, 1, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gy < edge
                && free(self, 0, 1)
                && ok(0, n, A | E | SE | S)
                && ok(n - 1, n, A | S | SW | W)
                && side(n - 1, &|i| ok(i, n, A | E | SE | S | SW | W))
            {
                self.push(&mut queue, cur_x, cur_y + 1, gx, gy + 1, 4, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gx > 0
                && gy > 0
                && free(self, -1, -1)
                && ok(-1, -1, A | N | NE | E)
                && side(n, &|i| {
                    ok(-1, i - 1, A | N | NE | E | SE | S) && ok(i - 1, -1, A | NW | N | NE | E | W)
                })
            {
                self.push(&mut queue, cur_x - 1, cur_y - 1, gx - 1, gy - 1, 3, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gx < edge
                && gy > 0
                && free(self, 1, -1)
                && ok(n, -1, A | NW | N | W)
                && side(n, &|i| {
                    ok(n, i - 1, A | NW | N | S | SW | W) && ok(i, -1, A | NW | N | NE | E | W)
                })
            {
                self.push(&mut queue, cur_x + 1, cur_y - 1, gx + 1, gy - 1, 9, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gx > 0
                && gy < edge
                && free(self, -1, 1)
                && ok(-1, n, A | E | SE | S)
                && side(n, &|i| {
                    ok(-1, i, A | N | NE | E | SE | S) && ok(i - 1, n, A | E | SE | S | SW | W)
                })
            {
                self.push(&mut queue, cur_x - 1, cur_y + 1, gx - 1, gy + 1, 6, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }

            if gx < edge
                && gy < edge
                && free(self, 1, 1)
                && ok(n, n, A | S | SW | W)
                && side(n, &|i| {
                    ok(i, n, A | E | SE | S | SW | W) && ok(n, i, A | NW | N | S | SW | W)
                })
            {
                self.push(&mut queue, cur_x + 1, cur_y + 1, gx + 1, gy + 1, 12, next);
            }
            if clip.faulted() {
                return Err(RouteFault);
            }
        }
        self.exit_x = cur_x;
        self.exit_y = cur_y;
        Ok(false)
    }
}
