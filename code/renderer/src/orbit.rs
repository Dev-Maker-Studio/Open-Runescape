use crate::camera::CameraPose;
use std::sync::OnceLock;

pub const TURN: i32 = 16384;
pub const TURN_MASK: i32 = 0x3fff;
pub const MIN_ANGLE_X: f32 = 1081.0;
pub const MAX_ANGLE_X: f32 = 2980.0;
pub const DEFAULT_ANGLE_X: f32 = 1052.0;
pub const DEFAULT_ZOOM: i32 = 600;
pub const ZOOM_FLOOR: i32 = -50;
pub const ZOOM_CEILING: i32 = 1700;
pub const ZOOM_STEP: i32 = 30;
pub const ZOOM_MIN: i32 = 256;
pub const ZOOM_MAX: i32 = 320;
pub const YAW_VELOCITY_TARGET: f32 = 24.0;
pub const PITCH_VELOCITY_TARGET: f32 = 12.0;
pub const SNAP_DISTANCE: i32 = 2000;
pub const FOLLOW_DIVISOR: i64 = 320;
pub const JITTER_PERIOD: i32 = 508;
pub const EYE_RAISE: i32 = 200;
pub const LOWEST_YAW_MIN: i32 = 262_144;
pub const LOWEST_YAW_MAX: i32 = 786_432;
pub const CYCLE_MILLIS: i64 = 20;
const TRIG_STEP: f64 = 3.834951969714103E-4;

struct Trig {
    sine: Vec<i32>,
    cosine: Vec<i32>,
}

fn trig() -> &'static Trig {
    static TABLES: OnceLock<Trig> = OnceLock::new();
    TABLES.get_or_init(|| Trig {
        sine: (0..TURN)
            .map(|i| (16384.0 * (f64::from(i) * TRIG_STEP).sin()) as i32)
            .collect(),
        cosine: (0..TURN)
            .map(|i| (16384.0 * (f64::from(i) * TRIG_STEP).cos()) as i32)
            .collect(),
    })
}

pub fn sine(index: i32) -> i32 {
    trig().sine[(index & TURN_MASK) as usize]
}

pub fn cosine(index: i32) -> i32 {
    trig().cosine[(index & TURN_MASK) as usize]
}

pub trait Ground {
    fn size(&self) -> (i32, i32);
    fn dimension(&self) -> (i32, i32);
    fn height(&self, plane: i32, x: i32, y: i32) -> i32;
    fn average_height(&self, plane: i32, abs_x: i32, abs_y: i32) -> i32;
    fn roofs_hidden(&self, x: i32, y: i32) -> bool;
    fn bridged(&self, x: i32, y: i32) -> bool;
    fn lighting(&self, plane: i32, x: i32, y: i32) -> Option<u8>;

    fn height_at(&self, abs_x: i32, abs_y: i32, plane: i32) -> i32 {
        let (size_x, size_y) = self.size();
        let x = abs_x >> 9;
        let y = abs_y >> 9;
        if x >= 0 && y >= 0 && x < size_x && y < size_y {
            let mut level = plane;
            if plane < 3 && self.bridged(x, y) {
                level = plane + 1;
            }
            return self.average_height(level, abs_x, abs_y);
        }
        0
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Jitter {
    pub h: i32,
    pub v: i32,
    pub a: i32,
    pub step_h: i32,
    pub step_v: i32,
    pub step_a: i32,
    pub cycle: i32,
}

impl Default for Jitter {
    fn default() -> Self {
        Self {
            h: 0,
            v: 0,
            a: 0,
            step_h: 2,
            step_v: 2,
            step_a: 1,
            cycle: 0,
        }
    }
}

impl Jitter {
    pub fn tick(&mut self, random_bits: i32) {
        self.cycle += 1;
        if self.cycle > JITTER_PERIOD {
            self.cycle = 0;
            if random_bits & 1 == 1 {
                self.h += self.step_h;
            }
            if random_bits & 2 == 2 {
                self.v += self.step_v;
            }
            if random_bits & 4 == 4 {
                self.a += self.step_a;
            }
        }
        if self.h < -51 {
            self.step_h = 2;
        }
        if self.h > 56 {
            self.step_h = -2;
        }
        if self.v < -63 {
            self.step_v = 2;
        }
        if self.v > 56 {
            self.step_v = -2;
        }
        if self.a < -44 {
            self.step_a = 1;
        }
        if self.a > 40 {
            self.step_a = -1;
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Orbit {
    pub angle_x: f32,
    pub angle_y: f32,
    pub base_x: i32,
    pub base_y: i32,
    pub lowest_yaw: i32,
    pub zoom: i32,
    pub yaw_velocity: f32,
    pub pitch_velocity: f32,
    pub yaw_applied: bool,
    pub pitch_applied: bool,
    pub jitter: Jitter,
    pub zoom_min: i32,
    pub zoom_max: i32,
}

impl Default for Orbit {
    fn default() -> Self {
        Self {
            angle_x: DEFAULT_ANGLE_X,
            angle_y: 0.0,
            base_x: 0,
            base_y: 0,
            lowest_yaw: 0,
            zoom: DEFAULT_ZOOM,
            yaw_velocity: 0.0,
            pitch_velocity: 0.0,
            yaw_applied: false,
            pitch_applied: false,
            jitter: Jitter::default(),
            zoom_min: ZOOM_MIN,
            zoom_max: ZOOM_MAX,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Eye {
    pub x: i32,
    pub z: i32,
    pub y: i32,
    pub yaw: i32,
    pub roll: i32,
}

impl Eye {
    pub fn pose(&self) -> CameraPose {
        CameraPose {
            x: self.x,
            y: self.y,
            z: self.z,
            yaw: self.yaw,
            roll: self.roll,
            pitch: 0,
        }
    }
}

fn approach(base: i32, target: i32, multiplier: i64) -> i32 {
    if target == base {
        return base;
    }
    let distance = target.wrapping_sub(base);
    let mut offset = (multiplier.wrapping_mul(i64::from(distance)) / FOLLOW_DIVISOR) as i32;
    if distance > 0 {
        if offset == 0 {
            offset = 1;
        } else if offset > distance {
            offset = distance;
        }
    } else if offset == 0 {
        offset = -1;
    } else if offset < distance {
        offset = distance;
    }
    base.wrapping_add(offset)
}

#[allow(clippy::too_many_arguments)]
pub fn set_camera_position(
    x: i32,
    z: i32,
    y: i32,
    yaw: i32,
    roll: i32,
    base_delta_y: i32,
    area_height: i32,
    zoom_min: i32,
    zoom_max: i32,
) -> Eye {
    let delta = (area_height - 334).clamp(0, 100);
    let modifier = (zoom_max - zoom_min) * delta / 100 + zoom_min;
    let base = base_delta_y.wrapping_mul(modifier) >> 8;
    let rotation_x = (16384 - yaw) & TURN_MASK;
    let rotation_z = (16384 - roll) & TURN_MASK;
    let mut delta_x = 0;
    let mut delta_z = 0;
    let mut delta_y = base;
    if rotation_x != 0 {
        delta_z = sine(rotation_x).wrapping_mul(base.wrapping_neg()) >> 14;
        delta_y = cosine(rotation_x).wrapping_mul(base) >> 14;
    }
    if rotation_z != 0 {
        delta_x = sine(rotation_z).wrapping_mul(delta_y) >> 14;
        delta_y = cosine(rotation_z).wrapping_mul(delta_y) >> 14;
    }
    Eye {
        x: x.wrapping_sub(delta_x),
        z: z.wrapping_sub(delta_z),
        y: y.wrapping_sub(delta_y),
        yaw,
        roll,
    }
}

impl Orbit {
    pub fn login(jitter_h: i32, jitter_v: i32, jitter_a: i32, angle_y: i32) -> Self {
        Self {
            angle_y: (angle_y & TURN_MASK) as f32,
            jitter: Jitter {
                h: jitter_h,
                v: jitter_v,
                a: jitter_a,
                ..Jitter::default()
            },
            ..Self::default()
        }
    }

    pub fn apply_yaw(&mut self, direction: i32) {
        if direction == 0 || self.yaw_applied {
            return;
        }
        let target = if direction > 0 {
            YAW_VELOCITY_TARGET
        } else {
            -YAW_VELOCITY_TARGET
        };
        self.yaw_velocity += (target - self.yaw_velocity) / 2.0;
        self.yaw_applied = true;
    }

    pub fn apply_pitch(&mut self, direction: i32) {
        if direction == 0 || self.pitch_applied {
            return;
        }
        let target = if direction > 0 {
            PITCH_VELOCITY_TARGET
        } else {
            -PITCH_VELOCITY_TARGET
        };
        self.pitch_velocity += (target - self.pitch_velocity) / 2.0;
        self.pitch_applied = true;
    }

    pub fn settle_velocities(&mut self) {
        if self.pitch_applied {
            self.pitch_applied = false;
        } else {
            self.pitch_velocity /= 2.0;
        }
        if self.yaw_applied {
            self.yaw_applied = false;
        } else {
            self.yaw_velocity /= 2.0;
        }
    }

    pub fn wheel_zoom(&mut self, rotation: i32) {
        if rotation == -1 && self.zoom >= ZOOM_FLOOR {
            self.zoom -= ZOOM_STEP;
        }
        if rotation == 1 && self.zoom < ZOOM_CEILING {
            self.zoom += ZOOM_STEP;
        }
    }

    pub fn set_angle(&mut self, angle_x: i32, angle_y: i32, ground: &dyn Ground, plane: i32) {
        self.angle_x = (angle_x << 3) as f32;
        self.angle_y = (angle_y << 3) as f32;
        self.process(ground, plane);
    }

    pub fn follow(
        &mut self,
        multiplier: i64,
        player_x: i32,
        player_y: i32,
        ground: &dyn Ground,
        plane: i32,
    ) {
        let x = player_x.wrapping_add(self.jitter.h);
        let y = player_y.wrapping_add(self.jitter.v);
        if self.base_x.wrapping_sub(x) < -SNAP_DISTANCE
            || self.base_x.wrapping_sub(x) > SNAP_DISTANCE
            || self.base_y.wrapping_sub(y) < -SNAP_DISTANCE
            || self.base_y.wrapping_sub(y) > SNAP_DISTANCE
        {
            self.base_x = x;
            self.base_y = y;
        }
        self.base_x = approach(self.base_x, x, multiplier);
        self.base_y = approach(self.base_y, y, multiplier);
        self.angle_y += self.yaw_velocity * multiplier as f32 / 6.0;
        self.angle_x += self.pitch_velocity * multiplier as f32 / 6.0;
        self.process(ground, plane);
    }

    pub fn follow_target(
        &mut self,
        multiplier: i64,
        target_x: i32,
        target_y: i32,
        ground: &dyn Ground,
        plane: i32,
    ) {
        self.base_x = approach(self.base_x, target_x, multiplier);
        self.base_y = approach(self.base_y, target_y, multiplier);
        self.angle_y += 8.0 * (self.yaw_velocity * multiplier as f32 / 40.0);
        self.angle_x += 8.0 * (multiplier as f32 * self.pitch_velocity / 40.0);
        self.process(ground, plane);
    }

    pub fn process(&mut self, ground: &dyn Ground, plane: i32) {
        self.angle_x = self.angle_x.clamp(MIN_ANGLE_X, MAX_ANGLE_X);
        while self.angle_y >= 16384.0 {
            self.angle_y -= 16384.0;
        }
        while self.angle_y < 0.0 {
            self.angle_y += 16384.0;
        }
        let (size_x, size_y) = ground.size();
        let scene_x = self.base_x >> 9;
        let scene_y = self.base_y >> 9;
        let tile_height = ground.height_at(self.base_x, self.base_y, plane);
        let mut highest = 0;
        if scene_x > 3 && scene_y > 3 && scene_x < size_x - 4 && scene_y < size_y - 4 {
            for x in scene_x - 4..=scene_x + 4 {
                for y in scene_y - 4..=scene_y + 4 {
                    let mut level = plane;
                    if level < 3 && ground.roofs_hidden(x, y) {
                        level += 1;
                    }
                    let mut lighting = 0i32;
                    if let Some(byte) = ground.lighting(level, x, y) {
                        lighting = (i32::from(byte) * 8) << 2;
                    }
                    let offset = tile_height - (ground.height(level, x, y) - lighting);
                    if offset > highest {
                        highest = offset;
                    }
                }
            }
        }
        let yaw = ((highest >> 2).wrapping_mul(1536)).clamp(LOWEST_YAW_MIN, LOWEST_YAW_MAX);
        if yaw > self.lowest_yaw {
            self.lowest_yaw += (yaw - self.lowest_yaw) / 24;
        } else if yaw < self.lowest_yaw {
            self.lowest_yaw += (yaw - self.lowest_yaw) / 80;
        }
    }

    pub fn eye(
        &self,
        player_x: i32,
        player_y: i32,
        ground: &dyn Ground,
        plane: i32,
        area_height: i32,
    ) -> Eye {
        let mut yaw = self.angle_x as i32;
        if self.lowest_yaw >> 8 > yaw {
            yaw = self.lowest_yaw >> 8;
        }
        let roll = (self.angle_y as i32).wrapping_add(self.jitter.a) & TURN_MASK;
        let eye = set_camera_position(
            self.base_x,
            ground.height_at(player_x, player_y, plane) - EYE_RAISE,
            self.base_y,
            yaw,
            roll,
            ((yaw >> 3) * 3 + self.zoom) << 2,
            area_height,
            self.zoom_min,
            self.zoom_max,
        );
        let (dimension_x, dimension_y) = ground.dimension();
        Eye {
            x: eye.x.clamp(0, (dimension_x << 9) - 1),
            y: eye.y.clamp(0, (dimension_y << 9) - 1),
            ..eye
        }
    }
}

pub fn cycles_for(elapsed_millis: i64) -> i64 {
    (elapsed_millis / CYCLE_MILLIS).max(0)
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct HeightGrid {
    pub size_x: i32,
    pub size_y: i32,
    pub heights: Vec<i32>,
}

impl HeightGrid {
    pub fn new(size_x: i32, size_y: i32) -> Self {
        Self {
            size_x,
            size_y,
            heights: vec![0; ((size_x + 1) * (size_y + 1)) as usize],
        }
    }

    pub fn set(&mut self, x: i32, y: i32, value: i32) {
        if (0..=self.size_x).contains(&x) && (0..=self.size_y).contains(&y) {
            self.heights[(x * (self.size_y + 1) + y) as usize] = value;
        }
    }

    pub fn get(&self, x: i32, y: i32) -> i32 {
        if (0..=self.size_x).contains(&x) && (0..=self.size_y).contains(&y) {
            self.heights[(x * (self.size_y + 1) + y) as usize]
        } else {
            0
        }
    }
}

impl Ground for HeightGrid {
    fn size(&self) -> (i32, i32) {
        (self.size_x, self.size_y)
    }

    fn dimension(&self) -> (i32, i32) {
        (self.size_x, self.size_y)
    }

    fn height(&self, _plane: i32, x: i32, y: i32) -> i32 {
        self.get(x, y)
    }

    fn average_height(&self, _plane: i32, abs_x: i32, abs_y: i32) -> i32 {
        average_height(self, abs_x, abs_y)
    }

    fn roofs_hidden(&self, _x: i32, _y: i32) -> bool {
        false
    }

    fn bridged(&self, _x: i32, _y: i32) -> bool {
        false
    }

    fn lighting(&self, _plane: i32, _x: i32, _y: i32) -> Option<u8> {
        None
    }
}

pub fn average_height(grid: &HeightGrid, x: i32, y: i32) -> i32 {
    let scaled_x = x >> 9;
    let scaled_y = y >> 9;
    if scaled_x >= 0 && scaled_y >= 0 && scaled_x < grid.size_x && scaled_y < grid.size_y {
        let offset_x = x & 511;
        let offset_y = y & 511;
        let h1 = (grid.get(scaled_x, scaled_y) * (512 - offset_x)
            + offset_x * grid.get(scaled_x + 1, scaled_y))
            >> 9;
        let h2 = (grid.get(scaled_x, scaled_y + 1) * (512 - offset_x)
            + grid.get(scaled_x + 1, scaled_y + 1) * offset_x)
            >> 9;
        return (h2 * offset_y + h1 * (512 - offset_y)) >> 9;
    }
    0
}

pub const NO_HEIGHT: i32 = i32::MIN;
pub const PLANES: usize = 4;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct SceneGround {
    size: i32,
    heights: Vec<i32>,
    bridges: Vec<bool>,
    lighting: Option<Vec<u8>>,
}

impl SceneGround {
    pub fn new(size: i32, heights: &[i32], bridges: &[u8]) -> Result<Self, String> {
        if size <= 0 {
            return Err("scene ground size must be positive".into());
        }
        let stride = (size + 1) as usize;
        if heights.len() != PLANES * stride * stride {
            return Err(format!(
                "scene ground needs {} heights, got {}",
                PLANES * stride * stride,
                heights.len()
            ));
        }
        let tiles = (size * size) as usize;
        if bridges.len() != tiles {
            return Err(format!(
                "scene ground needs {tiles} bridge flags, got {}",
                bridges.len()
            ));
        }
        Ok(Self {
            size,
            heights: heights
                .iter()
                .map(|h| if *h == NO_HEIGHT { 0 } else { *h })
                .collect(),
            bridges: bridges.iter().map(|b| *b != 0).collect(),
            lighting: None,
        })
    }

    pub fn set_lighting(&mut self, bytes: &[u8]) -> Result<(), String> {
        let tiles = (self.size * self.size) as usize;
        if bytes.len() != PLANES * tiles {
            return Err(format!(
                "scene lighting needs {} bytes, got {}",
                PLANES * tiles,
                bytes.len()
            ));
        }
        self.lighting = Some(bytes.to_vec());
        Ok(())
    }

    fn at(&self, plane: i32, x: i32, y: i32) -> i32 {
        let stride = self.size + 1;
        if (0..PLANES as i32).contains(&plane)
            && (0..=self.size).contains(&x)
            && (0..=self.size).contains(&y)
        {
            self.heights[((plane * stride + x) * stride + y) as usize]
        } else {
            0
        }
    }
}

impl Ground for SceneGround {
    fn size(&self) -> (i32, i32) {
        (self.size, self.size)
    }

    fn dimension(&self) -> (i32, i32) {
        (self.size, self.size)
    }

    fn height(&self, plane: i32, x: i32, y: i32) -> i32 {
        self.at(plane, x, y)
    }

    fn average_height(&self, plane: i32, abs_x: i32, abs_y: i32) -> i32 {
        let sx = abs_x >> 9;
        let sy = abs_y >> 9;
        if sx >= 0 && sy >= 0 && sx < self.size && sy < self.size {
            let ox = abs_x & 511;
            let oy = abs_y & 511;
            let h = |x: i32, y: i32| self.at(plane, x, y);
            let h1 = (h(sx, sy) * (512 - ox) + ox * h(sx + 1, sy)) >> 9;
            let h2 = (h(sx, sy + 1) * (512 - ox) + h(sx + 1, sy + 1) * ox) >> 9;
            return (h2 * oy + h1 * (512 - oy)) >> 9;
        }
        0
    }

    fn roofs_hidden(&self, x: i32, y: i32) -> bool {
        self.bridged(x, y)
    }

    fn bridged(&self, x: i32, y: i32) -> bool {
        (0..self.size).contains(&x)
            && (0..self.size).contains(&y)
            && self.bridges[(x * self.size + y) as usize]
    }

    fn lighting(&self, plane: i32, x: i32, y: i32) -> Option<u8> {
        let lighting = self.lighting.as_ref()?;
        if (0..PLANES as i32).contains(&plane)
            && (0..self.size).contains(&x)
            && (0..self.size).contains(&y)
        {
            Some(lighting[((plane * self.size + x) * self.size + y) as usize])
        } else {
            Some(0)
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct FlatGround {
    pub size_x: i32,
    pub size_y: i32,
    pub level: i32,
}

impl Ground for FlatGround {
    fn size(&self) -> (i32, i32) {
        (self.size_x, self.size_y)
    }

    fn dimension(&self) -> (i32, i32) {
        (self.size_x, self.size_y)
    }

    fn height(&self, _plane: i32, _x: i32, _y: i32) -> i32 {
        self.level
    }

    fn average_height(&self, _plane: i32, abs_x: i32, abs_y: i32) -> i32 {
        let x = abs_x >> 9;
        let y = abs_y >> 9;
        if x >= 0 && y >= 0 && x < self.size_x && y < self.size_y {
            self.level
        } else {
            0
        }
    }

    fn roofs_hidden(&self, _x: i32, _y: i32) -> bool {
        false
    }

    fn bridged(&self, _x: i32, _y: i32) -> bool {
        false
    }

    fn lighting(&self, _plane: i32, _x: i32, _y: i32) -> Option<u8> {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn trig_tables_match_the_client() {
        assert_eq!(sine(0), 0);
        assert_eq!(cosine(0), 16384);
        assert_eq!(sine(4096), 16384);
    }

    #[test]
    fn camera_position_at_default_roll_sits_behind_the_target() {
        let eye = set_camera_position(1000, -500, 2000, 1052, 0, 3000, 360, 256, 320);
        assert_eq!(eye.x, 1000);
        assert!(eye.y < 2000);
        assert!(eye.z < -500);
    }

    #[test]
    fn velocity_halves_when_not_applied() {
        let mut orbit = Orbit::default();
        orbit.apply_yaw(1);
        assert_eq!(orbit.yaw_velocity, 12.0);
        orbit.settle_velocities();
        assert_eq!(orbit.yaw_velocity, 12.0);
        orbit.settle_velocities();
        assert_eq!(orbit.yaw_velocity, 6.0);
    }

    #[test]
    fn follow_clamps_pitch_and_wraps_compass() {
        let ground = FlatGround {
            size_x: 104,
            size_y: 104,
            level: 0,
        };
        let mut orbit = Orbit {
            angle_x: 99999.0,
            angle_y: -5.0,
            ..Orbit::default()
        };
        orbit.follow(20, 52 * 512, 52 * 512, &ground, 0);
        assert_eq!(orbit.angle_x, MAX_ANGLE_X);
        assert_eq!(orbit.angle_y, 16379.0);
    }

    #[test]
    fn wheel_zoom_steps_within_bounds() {
        let mut orbit = Orbit::default();
        orbit.wheel_zoom(1);
        assert_eq!(orbit.zoom, DEFAULT_ZOOM + ZOOM_STEP);
        orbit.zoom = ZOOM_CEILING;
        orbit.wheel_zoom(1);
        assert_eq!(orbit.zoom, ZOOM_CEILING);
    }

    fn ramp_ground(bridge_at: Option<(i32, i32)>) -> SceneGround {
        let size = 8;
        let stride = size + 1;
        let mut heights = Vec::new();
        for plane in 0..PLANES as i32 {
            for x in 0..stride {
                for y in 0..stride {
                    heights.push(plane * 1000 + x * 10 + y);
                }
            }
        }
        let mut bridges = vec![0u8; (size * size) as usize];
        if let Some((x, y)) = bridge_at {
            bridges[(x * size + y) as usize] = 1;
        }
        SceneGround::new(size, &heights, &bridges).unwrap()
    }

    #[test]
    fn scene_ground_indexes_planes_and_tiles() {
        let ground = ramp_ground(None);
        assert_eq!(ground.height(0, 3, 4), 34);
        assert_eq!(ground.height(2, 8, 8), 2088);
        assert_eq!(ground.height(4, 1, 1), 0);
        assert_eq!(ground.height(0, 9, 1), 0);
        assert_eq!(ground.size(), (8, 8));
    }

    #[test]
    fn scene_ground_interpolates_average_height() {
        let ground = ramp_ground(None);
        assert_eq!(ground.average_height(0, 3 * 512, 4 * 512), 34);
        assert_eq!(ground.average_height(0, 3 * 512 + 256, 4 * 512), 39);
        assert_eq!(ground.average_height(0, 3 * 512, 4 * 512 + 256), 34);
        assert_eq!(ground.average_height(0, 3 * 512 + 256, 4 * 512 + 256), 39);
        assert_eq!(ground.average_height(0, 8 * 512, 0), 0);
        assert_eq!(ground.average_height(0, -1, 0), 0);
    }

    #[test]
    fn bridge_bumps_the_plane_and_hides_roofs() {
        let ground = ramp_ground(Some((3, 4)));
        assert!(ground.bridged(3, 4));
        assert!(ground.roofs_hidden(3, 4));
        assert!(!ground.bridged(4, 3));
        assert!(!ground.bridged(-1, 0));
        assert_eq!(ground.height_at(3 * 512, 4 * 512, 0), 1034);
        assert_eq!(ground.height_at(4 * 512, 3 * 512, 0), 43);
        assert_eq!(ground.height_at(3 * 512, 4 * 512, 3), 3034);
    }

    #[test]
    fn missing_lighting_is_none_and_present_lighting_is_per_plane() {
        let mut ground = ramp_ground(None);
        assert_eq!(ground.lighting(0, 1, 1), None);
        let mut bytes = vec![0u8; PLANES * 64];
        bytes[(2 * 8 + 1) * 8 + 3] = 5;
        ground.set_lighting(&bytes).unwrap();
        assert_eq!(ground.lighting(0, 1, 3), Some(0));
        assert_eq!(ground.lighting(2, 1, 3), Some(5));
        assert_eq!(ground.lighting(2, 9, 3), Some(0));
        assert!(ground.set_lighting(&[0; 3]).is_err());
    }

    #[test]
    fn scene_ground_rejects_mismatched_buffers_and_maps_no_height() {
        assert!(SceneGround::new(0, &[], &[]).is_err());
        assert!(SceneGround::new(2, &[0; 5], &[0; 4]).is_err());
        assert!(SceneGround::new(2, &[0; 36], &[0; 3]).is_err());
        let mut heights = vec![7; 36];
        heights[0] = NO_HEIGHT;
        let ground = SceneGround::new(2, &heights, &[0; 4]).unwrap();
        assert_eq!(ground.height(0, 0, 0), 0);
        assert_eq!(ground.height(0, 0, 1), 7);
    }

    #[test]
    fn live_ground_lifts_the_eye_with_the_terrain() {
        let low = ramp_ground(None);
        let mut orbit = Orbit::default();
        orbit.follow(20, 4 * 512, 4 * 512, &low, 0);
        let flat = orbit.eye(4 * 512, 4 * 512, &low, 0, 600);
        let mut raised_heights = Vec::new();
        for plane in 0..PLANES as i32 {
            for x in 0..9 {
                for y in 0..9 {
                    raised_heights.push(plane * 1000 + x * 10 + y - 600);
                }
            }
        }
        let high = SceneGround::new(8, &raised_heights, &[0; 64]).unwrap();
        let mut orbit = Orbit::default();
        orbit.follow(20, 4 * 512, 4 * 512, &high, 0);
        let raised = orbit.eye(4 * 512, 4 * 512, &high, 0, 600);
        assert_ne!(flat.z, raised.z);
    }
}
