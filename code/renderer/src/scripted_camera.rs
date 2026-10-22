use crate::camera::CameraPose;
use crate::orbit::{self, EYE_RAISE, Ground, Orbit, TURN_MASK};

pub const SHAKE_SLOTS: usize = 5;
pub const SPLINE_PATHS: usize = 2;
pub const ANGLE_UNITS: f64 = 2607.5945876176133;
pub const SMOOTH_CYCLES: i32 = 100;
pub const SPLINE_END: i32 = 65535;
pub const SMOOTH_ZOOM: i32 = 600;
pub const SHAKE_YAW_SLOT: usize = 4;
pub const SHAKE_YAW_FLOOR: i32 = 128;
pub const MIN_PITCH_UNITS: i32 = 1024;
pub const MAX_PITCH_UNITS: i32 = 3072;

pub const MODE_SMOOTH: i32 = 1;
pub const MODE_FOLLOW: i32 = 2;
pub const MODE_SPLINE: i32 = 3;
pub const MODE_TARGET: i32 = 4;
pub const MODE_SCRIPTED: i32 = 5;

pub const COMMAND_FORCE_ANGLE: i32 = 1;
pub const COMMAND_SMOOTH_RESET: i32 = 2;
pub const COMMAND_RESET: i32 = 3;
pub const COMMAND_LOOK_AT: i32 = 4;
pub const COMMAND_MOVE_TO: i32 = 5;
pub const COMMAND_SHAKE: i32 = 6;
pub const COMMAND_TARGET: i32 = 7;

pub type Spline = Vec<[i32; 4]>;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Shake {
    pub active: bool,
    pub jitter: i32,
    pub amplitude: i32,
    pub frequency: i32,
    pub phase: i32,
    pub cycle: i32,
    pub offset: i32,
}

impl Default for Shake {
    fn default() -> Self {
        Self {
            active: false,
            jitter: 0,
            amplitude: 0,
            frequency: 0,
            phase: 0,
            cycle: -1,
            offset: 0,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Placement {
    pub player_x: i32,
    pub player_y: i32,
    pub plane: i32,
    pub area_height: i32,
}

#[derive(Clone, Debug, PartialEq)]
pub struct ScriptedCamera {
    pub mode: i32,
    pub x: i32,
    pub z: i32,
    pub y: i32,
    pub yaw: i32,
    pub roll: i32,
    pub pitch: i32,
    pub start_x: i32,
    pub start_z: i32,
    pub start_y: i32,
    pub start_yaw: i32,
    pub start_roll: i32,
    pub look_x: i32,
    pub look_y: i32,
    pub look_height: i32,
    pub move_x: i32,
    pub move_y: i32,
    pub move_z: i32,
    pub constant_speed: i32,
    pub variable_speed: i32,
    pub move_angle: i32,
    pub move_speed: i32,
    pub target_x: i32,
    pub target_y: i32,
    pub ping_target_x: i32,
    pub static_target_y: i32,
    pub last_smooth_cycle: i32,
    pub cycles: i32,
    pub position_path: i32,
    pub look_path: i32,
    pub position_keyframe: i32,
    pub look_keyframe: i32,
    pub spline_progress: i32,
    pub spline_speed_start: i32,
    pub spline_speed_end: i32,
    pub spline_completed: bool,
    pub spline_previously_completed: bool,
    pub splines: [Option<Spline>; SPLINE_PATHS],
    pub shakes: [Shake; SHAKE_SLOTS],
    pub zoom_min: i32,
    pub zoom_max: i32,
    pub send_angle: bool,
}

impl Default for ScriptedCamera {
    fn default() -> Self {
        Self {
            mode: MODE_FOLLOW,
            x: 0,
            z: 0,
            y: 0,
            yaw: 0,
            roll: 0,
            pitch: 0,
            start_x: 0,
            start_z: 0,
            start_y: 0,
            start_yaw: 0,
            start_roll: 0,
            look_x: 0,
            look_y: 0,
            look_height: 0,
            move_x: 0,
            move_y: 0,
            move_z: 0,
            constant_speed: 0,
            variable_speed: 0,
            move_angle: 0,
            move_speed: 0,
            target_x: 0,
            target_y: 0,
            ping_target_x: -1,
            static_target_y: -1,
            last_smooth_cycle: 0,
            cycles: 0,
            position_path: -1,
            look_path: -1,
            position_keyframe: 0,
            look_keyframe: 0,
            spline_progress: 0,
            spline_speed_start: 0,
            spline_speed_end: 0,
            spline_completed: false,
            spline_previously_completed: false,
            splines: [None, None],
            shakes: [Shake::default(); SHAKE_SLOTS],
            zoom_min: orbit::ZOOM_MIN,
            zoom_max: orbit::ZOOM_MAX,
            send_angle: false,
        }
    }
}

fn units(radians: f64) -> i32 {
    (radians * ANGLE_UNITS) as i32
}

fn catch_up(position: i32, goal: i32, constant: i32, variable: i32) -> i32 {
    let mut value = position;
    if value < goal {
        value = ((goal - value).wrapping_mul(variable) / 1000)
            .wrapping_add(value)
            .wrapping_add(constant);
        if value > goal {
            value = goal;
        }
    }
    if value > goal {
        value =
            value.wrapping_sub((variable.wrapping_mul(value - goal) / 1000).wrapping_add(constant));
        if value < goal {
            value = goal;
        }
    }
    value
}

fn wrap_roll(delta: i32) -> i32 {
    let mut delta = delta;
    if delta > 8192 {
        delta -= 16384;
    }
    if delta < -8192 {
        delta += 16384;
    }
    delta
}

impl ScriptedCamera {
    pub fn is_scripted(&self) -> bool {
        matches!(
            self.mode,
            MODE_SMOOTH | MODE_SPLINE | MODE_TARGET | MODE_SCRIPTED
        )
    }

    pub fn clear_targets(&mut self) {
        self.static_target_y = -1;
        self.ping_target_x = -1;
    }

    fn clear_scripts(&mut self) {
        for shake in self.shakes.iter_mut() {
            shake.active = false;
        }
        self.position_path = -1;
        self.look_path = -1;
        self.move_speed = 0;
        self.move_angle = 0;
    }

    pub fn smooth_reset(&mut self) {
        self.clear_scripts();
        self.mode = MODE_FOLLOW;
        self.clear_targets();
    }

    pub fn start_smooth_transition(&mut self) {
        self.clear_scripts();
        self.mode = MODE_SMOOTH;
        self.clear_targets();
        self.last_smooth_cycle = self.cycles;
        self.start_x = self.x;
        self.start_z = self.z;
        self.start_y = self.y;
        self.start_yaw = self.yaw;
        self.start_roll = self.roll;
    }

    pub fn shake(&mut self, slot: usize, amplitude: i32, phase: i32, jitter: i32, frequency: i32) {
        if let Some(shake) = self.shakes.get_mut(slot) {
            shake.active = true;
            shake.jitter = jitter;
            shake.amplitude = amplitude;
            shake.frequency = frequency;
            shake.phase = phase;
        }
    }

    pub fn start_cutscene(
        &mut self,
        position_path: i32,
        position_keyframe: i32,
        speed_start: i32,
        speed_end: i32,
        look_path: i32,
        look_keyframe: i32,
    ) {
        self.position_path = position_path;
        self.position_keyframe = position_keyframe;
        self.spline_progress = 0;
        self.spline_speed_start = speed_start;
        self.spline_speed_end = speed_end;
        self.look_path = look_path;
        self.look_keyframe = look_keyframe;
        self.mode = MODE_SPLINE;
        self.clear_targets();
    }

    pub fn set_target(&mut self, ping_x: i32, static_y: i32) {
        self.ping_target_x = ping_x;
        self.static_target_y = static_y;
    }

    pub fn follow_coordinate(&mut self, target_x: i32, target_y: i32) {
        self.target_x = target_x;
        self.target_y = target_y;
        self.mode = MODE_TARGET;
        self.clear_targets();
    }

    #[allow(clippy::too_many_arguments)]
    pub fn move_camera(
        &mut self,
        local_x: i32,
        local_y: i32,
        move_z: i32,
        constant_speed: i32,
        variable_speed: i32,
        calculate_position: bool,
        ground: &dyn Ground,
        plane: i32,
    ) {
        self.move_x = local_x;
        self.move_y = local_y;
        self.move_z = move_z;
        self.constant_speed = constant_speed;
        self.variable_speed = variable_speed;
        if calculate_position && self.variable_speed >= 100 {
            self.x = self.move_x.wrapping_mul(512).wrapping_add(256);
            self.y = self.move_y.wrapping_mul(512).wrapping_add(256);
            self.z = ground
                .height_at(self.x, self.y, plane)
                .wrapping_sub(self.move_z);
        }
        self.mode = MODE_SCRIPTED;
        self.clear_targets();
    }

    pub fn set_angle(
        &mut self,
        orbit: &mut Orbit,
        angle_x: i32,
        angle_y: i32,
        pitch: i32,
        ground: &dyn Ground,
        plane: i32,
    ) {
        let shifted_x = angle_x << 3;
        let shifted_y = angle_y << 3;
        let shifted_pitch = pitch << 3;
        orbit.angle_x = shifted_x as f32;
        orbit.angle_y = shifted_y as f32;
        if self.mode == MODE_SCRIPTED {
            self.yaw = shifted_x;
            self.roll = shifted_y;
            self.pitch = shifted_pitch;
        }
        orbit.process(ground, plane);
        self.send_angle = true;
    }

    #[allow(clippy::too_many_arguments)]
    pub fn look_at(
        &mut self,
        eye_x: i32,
        eye_y: i32,
        height_offset: i32,
        speed: i32,
        angle: i32,
        ground: &dyn Ground,
        plane: i32,
    ) {
        self.look_x = eye_x;
        self.look_y = eye_y;
        self.look_height = height_offset;
        self.move_speed = speed;
        self.move_angle = angle;
        if self.move_angle >= 100 {
            let abs_x = self.look_x.wrapping_mul(512).wrapping_add(256);
            let abs_y = self.look_y.wrapping_mul(512).wrapping_add(256);
            let abs_z = ground
                .height_at(abs_x, abs_y, plane)
                .wrapping_sub(self.look_height);
            let delta_x = abs_x.wrapping_sub(self.x);
            let delta_z = abs_z.wrapping_sub(self.z);
            let delta_y = abs_y.wrapping_sub(self.y);
            let horizontal = f64::from(
                delta_x
                    .wrapping_mul(delta_x)
                    .wrapping_add(delta_y.wrapping_mul(delta_y)),
            )
            .sqrt() as i32;
            self.yaw = units(f64::from(delta_z).atan2(f64::from(horizontal))) & TURN_MASK;
            self.roll = units(-f64::from(delta_x).atan2(f64::from(delta_y))) & TURN_MASK;
            self.pitch = 0;
            self.yaw = self.yaw.clamp(MIN_PITCH_UNITS, MAX_PITCH_UNITS);
        }
        self.mode = MODE_SCRIPTED;
        self.clear_targets();
    }

    pub fn calculate_position(&mut self, ground: &dyn Ground, plane: i32) {
        let absolute_x = self.move_x.wrapping_mul(512).wrapping_add(256);
        let absolute_y = self.move_y.wrapping_mul(512).wrapping_add(256);
        let absolute_z = ground
            .height_at(absolute_x, absolute_y, plane)
            .wrapping_sub(self.move_z);
        if self.variable_speed >= 100 {
            self.x = absolute_x;
            self.y = absolute_y;
            self.z = ground
                .height_at(self.x, self.y, plane)
                .wrapping_sub(self.move_z);
        } else {
            self.x = catch_up(self.x, absolute_x, self.constant_speed, self.variable_speed);
            self.z = catch_up(self.z, absolute_z, self.constant_speed, self.variable_speed);
            self.y = catch_up(self.y, absolute_y, self.constant_speed, self.variable_speed);
        }
        let look_abs_x = self.look_x.wrapping_mul(512).wrapping_add(256);
        let look_abs_y = self.look_y.wrapping_mul(512).wrapping_add(256);
        let look_abs_z = ground
            .height_at(look_abs_x, look_abs_y, plane)
            .wrapping_sub(self.look_height);
        let delta_x = look_abs_x.wrapping_sub(self.x);
        let delta_z = look_abs_z.wrapping_sub(self.z);
        let delta_y = look_abs_y.wrapping_sub(self.y);
        let horizontal = f64::from(
            delta_y
                .wrapping_mul(delta_y)
                .wrapping_add(delta_x.wrapping_mul(delta_x)),
        )
        .sqrt() as i32;
        let mut yaw = units(f64::from(delta_z).atan2(f64::from(horizontal))) & TURN_MASK;
        let roll = units(-f64::from(delta_x).atan2(f64::from(delta_y))) & TURN_MASK;
        yaw = yaw.clamp(MIN_PITCH_UNITS, MAX_PITCH_UNITS);
        let speed = self.move_speed;
        let angle = self.move_angle;
        if self.yaw < yaw {
            self.yaw = self
                .yaw
                .wrapping_add((((yaw - self.yaw) >> 3).wrapping_mul(angle) / 1000 + speed) << 3);
            if self.yaw > yaw {
                self.yaw = yaw;
            }
        }
        if self.yaw > yaw {
            self.yaw = self
                .yaw
                .wrapping_sub((((self.yaw - yaw) >> 3).wrapping_mul(angle) / 1000 + speed) << 3);
            if self.yaw < yaw {
                self.yaw = yaw;
            }
        }
        let mut roll_delta = wrap_roll(roll - self.roll);
        roll_delta >>= 3;
        if roll_delta > 0 {
            self.roll = self
                .roll
                .wrapping_add((roll_delta.wrapping_mul(angle) / 1000 + speed) << 3);
            self.roll &= TURN_MASK;
        }
        if roll_delta < 0 {
            self.roll = self
                .roll
                .wrapping_sub((speed - roll_delta.wrapping_mul(angle) / 1000) << 3);
            self.roll &= TURN_MASK;
        }
        let remaining = wrap_roll(roll - self.roll);
        if (remaining < 0 && roll_delta > 0) || (remaining > 0 && roll_delta < 0) {
            self.roll = roll;
        }
        self.pitch = 0;
    }

    fn spline_axis(points: &Spline, index: usize, axis: usize, progress: f32, look: bool) -> f32 {
        let start = points[index][axis];
        let ctrl0 = points[index][axis].wrapping_mul(3);
        let ctrl1 = points[index + 1][axis].wrapping_mul(3);
        let third = points[index + 2][axis];
        let fourth = points[index + 3][axis];
        let ctrl2 = third
            .wrapping_sub(fourth.wrapping_sub(third))
            .wrapping_mul(3);
        let linear = ctrl1.wrapping_sub(ctrl0);
        let quad = ctrl0
            .wrapping_sub(ctrl1.wrapping_mul(2))
            .wrapping_add(ctrl2);
        if look {
            let cubic = ctrl1
                .wrapping_add(third.wrapping_sub(start))
                .wrapping_sub(ctrl2);
            progress * ((quad as f32 + progress * cubic as f32) * progress + linear as f32)
                + start as f32
        } else {
            let cubic = third
                .wrapping_sub(start)
                .wrapping_add(ctrl1)
                .wrapping_sub(ctrl2);
            start as f32
                + ((cubic as f32 * progress + quad as f32) * progress + linear as f32) * progress
        }
    }

    pub fn calculate_spline(&mut self, base_x: i32, base_y: i32) {
        if self.position_path == -1 || self.look_path == -1 {
            return;
        }
        let (Some(position), Some(look)) = (
            self.splines
                .get(self.position_path as usize)
                .and_then(|s| s.as_ref()),
            self.splines
                .get(self.look_path as usize)
                .and_then(|s| s.as_ref()),
        ) else {
            return;
        };
        let position_index = (self.position_keyframe as usize) << 1;
        let look_index = (self.look_keyframe as usize) << 1;
        if position_index + 3 >= position.len() || look_index + 3 >= look.len() {
            return;
        }
        let speed = (((self.spline_speed_end.wrapping_sub(self.spline_speed_start))
            .wrapping_mul(self.spline_progress))
            >> 16)
            .wrapping_add(self.spline_speed_start);
        self.spline_progress = self.spline_progress.wrapping_add(speed);
        if self.spline_progress >= SPLINE_END {
            self.spline_progress = SPLINE_END;
            self.spline_completed = !self.spline_previously_completed;
            self.spline_previously_completed = true;
        } else {
            self.spline_completed = false;
            self.spline_previously_completed = false;
        }
        let progress = self.spline_progress as f32 / 65535.0;
        let mut from = [0.0f32; 3];
        for (axis, slot) in from.iter_mut().enumerate() {
            *slot = Self::spline_axis(position, position_index, axis, progress, false);
        }
        self.x = (from[0] as i32).wrapping_sub(base_x.wrapping_mul(512));
        self.z = (from[1] as i32).wrapping_mul(-1);
        self.y = (from[2] as i32).wrapping_sub(base_y.wrapping_mul(512));
        let mut to = [0.0f32; 3];
        for (axis, slot) in to.iter_mut().enumerate() {
            *slot = Self::spline_axis(look, look_index, axis, progress, true);
        }
        let delta_x = to[0] - from[0];
        let delta_z = -(to[1] - from[1]);
        let delta_y = to[2] - from[2];
        let horizontal = f64::from(delta_y * delta_y + delta_x * delta_x).sqrt();
        self.yaw = units(f64::from(delta_z).atan2(horizontal)) & TURN_MASK;
        self.roll = units(-f64::from(delta_x).atan2(f64::from(delta_y))) & TURN_MASK;
        let first = position[position_index][3];
        let last = position[position_index + 2][3];
        self.pitch =
            (last.wrapping_sub(first).wrapping_mul(self.spline_progress) >> 16).wrapping_add(first);
    }

    pub fn tick_cycle(&mut self, base_x: i32, base_y: i32, ground: &dyn Ground, plane: i32) {
        if self.mode == MODE_SCRIPTED {
            self.calculate_position(ground, plane);
        } else if self.mode == MODE_SPLINE {
            self.calculate_spline(base_x, base_y);
        }
    }

    pub fn advance_cycle(&mut self) {
        self.cycles = self.cycles.wrapping_add(1);
        for shake in self.shakes.iter_mut() {
            shake.phase = shake.phase.wrapping_add(1);
        }
    }

    fn lift(orbit: &Orbit, shakes: &[Shake; SHAKE_SLOTS]) -> i32 {
        let mut yaw = orbit.angle_x as i32;
        if orbit.lowest_yaw >> 8 > yaw {
            yaw = orbit.lowest_yaw >> 8;
        }
        if shakes[SHAKE_YAW_SLOT].active && shakes[SHAKE_YAW_SLOT].amplitude + SHAKE_YAW_FLOOR > yaw
        {
            yaw = shakes[SHAKE_YAW_SLOT].amplitude + SHAKE_YAW_FLOOR;
        }
        yaw
    }

    fn place(&mut self, eye: orbit::Eye) {
        self.x = eye.x;
        self.z = eye.z;
        self.y = eye.y;
        self.yaw = eye.yaw;
        self.roll = eye.roll;
        self.pitch = 0;
    }

    pub fn update_smooth(&mut self, orbit: &mut Orbit, ground: &dyn Ground, at: &Placement) {
        let since = self.cycles.wrapping_sub(self.last_smooth_cycle);
        if since >= SMOOTH_CYCLES {
            self.mode = MODE_FOLLOW;
            self.clear_targets();
            return;
        }
        let yaw = Self::lift(orbit, &self.shakes);
        let roll = (orbit.angle_y as i32).wrapping_add(orbit.jitter.a) & TURN_MASK;
        let eye = orbit::set_camera_position(
            orbit.base_x,
            ground.height_at(at.player_x, at.player_y, at.plane) - EYE_RAISE,
            orbit.base_y,
            yaw,
            roll,
            ((yaw >> 3) * 3 + SMOOTH_ZOOM) << 2,
            at.area_height,
            self.zoom_min,
            self.zoom_max,
        );
        self.place(eye);
        let remaining = 100 - since;
        let progress = 1.0f32 - (remaining * remaining * remaining) as f32 / 1_000_000.0;
        self.x = (self.start_x as f32 + progress * (self.x - self.start_x) as f32) as i32;
        self.z = (self.start_z as f32 + progress * (self.z - self.start_z) as f32) as i32;
        self.y = ((self.y - self.start_y) as f32 * progress + self.start_y as f32) as i32;
        self.yaw = (self.start_yaw as f32 + progress * (self.yaw - self.start_yaw) as f32) as i32;
        let delta = wrap_roll(self.roll - self.start_roll);
        self.roll = (self.start_roll as f32 + progress * delta as f32) as i32;
        self.roll &= TURN_MASK;
    }

    pub fn frame(
        &mut self,
        orbit: &mut Orbit,
        multiplier: i64,
        ground: &dyn Ground,
        at: &Placement,
        random: &mut dyn FnMut() -> f64,
    ) -> CameraPose {
        match self.mode {
            MODE_SMOOTH | MODE_FOLLOW => {
                orbit.follow(multiplier, at.player_x, at.player_y, ground, at.plane)
            }
            MODE_TARGET => {
                orbit.follow_target(multiplier, self.target_x, self.target_y, ground, at.plane)
            }
            _ => {}
        }
        match self.mode {
            MODE_FOLLOW => {
                let yaw = Self::lift(orbit, &self.shakes);
                let roll = (orbit.angle_y as i32).wrapping_add(orbit.jitter.a) & TURN_MASK;
                let eye = orbit::set_camera_position(
                    orbit.base_x,
                    ground.height_at(at.player_x, at.player_y, at.plane) - EYE_RAISE,
                    orbit.base_y,
                    yaw,
                    roll,
                    ((yaw >> 3) * 3 + orbit.zoom) << 2,
                    at.area_height,
                    self.zoom_min,
                    self.zoom_max,
                );
                self.place(eye);
            }
            MODE_TARGET => {
                let yaw = Self::lift(orbit, &self.shakes);
                let roll = (orbit.angle_y as i32) & TURN_MASK;
                let eye = orbit::set_camera_position(
                    orbit.base_x,
                    ground.height_at(self.target_x, self.target_y, at.plane) - EYE_RAISE,
                    orbit.base_y,
                    yaw,
                    roll,
                    ((yaw >> 3) * 3 + orbit.zoom) << 2,
                    at.area_height,
                    self.zoom_min,
                    self.zoom_max,
                );
                self.place(eye);
            }
            MODE_SMOOTH => self.update_smooth(orbit, ground, at),
            _ => {}
        }
        let mut pose = CameraPose {
            x: self.x,
            y: self.y,
            z: self.z,
            yaw: self.yaw,
            roll: self.roll,
            pitch: self.pitch,
        };
        for slot in 0..SHAKE_SLOTS {
            let cycles = self.cycles;
            let shake = &mut self.shakes[slot];
            if !shake.active {
                continue;
            }
            if shake.cycle != cycles {
                shake.cycle = cycles;
                shake.offset = (random() * f64::from(shake.jitter * 2 + 1)
                    - f64::from(shake.jitter)
                    + (f64::from(shake.phase) * (f64::from(shake.frequency) / 100.0)).sin()
                        * f64::from(shake.amplitude)) as i32;
            }
            let transition = shake.offset;
            match slot {
                0 => pose.x = pose.x.wrapping_add(transition << 2),
                1 => pose.z = pose.z.wrapping_add(transition << 2),
                2 => pose.y = pose.y.wrapping_add(transition << 2),
                3 => pose.roll = transition.wrapping_add(pose.roll) & TURN_MASK,
                _ => {
                    pose.yaw = pose.yaw.wrapping_add(transition);
                    pose.yaw = pose.yaw.clamp(MIN_PITCH_UNITS, MAX_PITCH_UNITS);
                }
            }
        }
        let (dimension_x, dimension_y) = ground.dimension();
        pose.x = pose.x.clamp(0, (dimension_x << 9) - 1);
        pose.y = pose.y.clamp(0, (dimension_y << 9) - 1);
        pose
    }
}

pub fn target_tile(key: i32, base: (i32, i32), size: (i32, i32)) -> Option<(i32, i32)> {
    if key == -1 {
        return None;
    }
    let local_x = ((key >> 14) & 0x3fff)
        .wrapping_sub(base.0)
        .clamp(0, size.0.max(0));
    let local_y = (key & 0x3fff).wrapping_sub(base.1).clamp(0, size.1.max(0));
    Some(((local_x << 9) + 256, (local_y << 9) + 256))
}
