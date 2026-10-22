pub const MOVE_STATIONARY: i32 = -1;
pub const MOVE_HALF_WALK: i32 = 0;
pub const MOVE_WALKING: i32 = 1;
pub const MOVE_RUNNING: i32 = 2;

pub const MAX_STEPS: usize = 25;

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct EntityTurnDirection {
    pub rotation: i32,
    pub velocity: i32,
}

impl EntityTurnDirection {
    pub fn calculate_rotation(
        &mut self,
        target: i32,
        acceleration: i32,
        max_velocity: i32,
    ) -> bool {
        let previous = self.velocity;
        if self.rotation == target && self.velocity == 0 {
            return false;
        }
        let should_accelerate = if self.velocity != 0 {
            if self.velocity > 0 && target > self.rotation {
                let stopping =
                    self.velocity.wrapping_mul(self.velocity) / acceleration.wrapping_mul(2);
                let projected = stopping.wrapping_add(self.rotation);
                projected < target && projected >= self.rotation
            } else if self.velocity < 0 && target < self.rotation {
                let stopping =
                    self.velocity.wrapping_mul(self.velocity) / acceleration.wrapping_mul(2);
                let projected = self.rotation.wrapping_sub(stopping);
                projected > target && projected <= self.rotation
            } else {
                false
            }
        } else {
            if (target > self.rotation && target <= acceleration.wrapping_add(self.rotation))
                || (target < self.rotation && target >= self.rotation.wrapping_sub(acceleration))
            {
                self.rotation = target;
                return false;
            }
            true
        };

        if should_accelerate {
            if target > self.rotation {
                self.velocity = self.velocity.wrapping_add(acceleration);
                if max_velocity != 0 && self.velocity > max_velocity {
                    self.velocity = max_velocity;
                }
            } else {
                self.velocity = self.velocity.wrapping_sub(acceleration);
                if max_velocity != 0 && self.velocity < max_velocity.wrapping_neg() {
                    self.velocity = max_velocity.wrapping_neg();
                }
            }
            if previous != self.velocity {
                let stopping =
                    self.velocity.wrapping_mul(self.velocity) / acceleration.wrapping_mul(2);
                if target > self.rotation {
                    if stopping.wrapping_add(self.rotation) > target {
                        self.velocity = previous;
                    }
                } else if self.rotation.wrapping_sub(stopping) < target {
                    self.velocity = previous;
                }
            }
        } else if self.velocity > 0 {
            self.velocity = self.velocity.wrapping_sub(acceleration);
            if self.velocity < 0 {
                self.velocity = 0;
            }
        } else {
            self.velocity = self.velocity.wrapping_add(acceleration);
            if self.velocity > 0 {
                self.velocity = 0;
            }
        }

        self.rotation = self
            .rotation
            .wrapping_add(previous.wrapping_add(self.velocity) >> 1);
        should_accelerate
    }

    pub fn set_rotation(&mut self, rotation: i32) {
        self.rotation = rotation;
        self.velocity = 0;
    }

    pub fn get_rotation(&self) -> i32 {
        self.rotation & 0x3fff
    }

    pub fn clamp(&mut self) {
        self.rotation &= 0x3fff;
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct BasParams {
    pub yaw_acceleration: i32,
    pub yaw_max_velocity: i32,
    pub roll_acceleration: i32,
    pub roll_max_velocity: i32,
    pub roll_target_angle: i32,
    pub pitch_acceleration: i32,
    pub pitch_max_velocity: i32,
    pub pitch_target_angle: i32,
    pub unused: i32,
}

impl Default for BasParams {
    fn default() -> Self {
        Self {
            yaw_acceleration: 0,
            yaw_max_velocity: 0,
            roll_acceleration: 0,
            roll_max_velocity: 0,
            roll_target_angle: 0,
            pitch_acceleration: 0,
            pitch_max_velocity: 0,
            pitch_target_angle: 0,
            unused: -1,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct SeqStall {
    pub interrupt_type: i32,
    pub move_type: i32,
}

#[derive(Clone, Debug)]
pub struct MovingEntity {
    pub x: f32,
    pub y: f32,
    pub z: f32,
    pub movement_base_x: [i32; MAX_STEPS],
    pub movement_base_y: [i32; MAX_STEPS],
    pub movement_types: [i8; MAX_STEPS],
    pub steps_left: i32,
    pub previous_steps_left: i32,
    pub stalled: i32,
    pub walk_speed_accumulator: i32,
    pub last_yaw_rotation: i32,
    pub turn_direction: i32,
    pub face_entity: i32,
    pub yaw: EntityTurnDirection,
    pub roll: EntityTurnDirection,
    pub pitch: EntityTurnDirection,
    pub turning_ticks: i32,
    pub size: i32,
    pub delay_movement: bool,
    pub seq_stall: Option<SeqStall>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct StepOutput {
    pub movement_type: i32,
    pub turn_direction_index: i32,
}

impl MovingEntity {
    pub fn new(x: f32, y: f32, z: f32, size: i32) -> Self {
        Self {
            x,
            y,
            z,
            movement_base_x: [0; MAX_STEPS],
            movement_base_y: [0; MAX_STEPS],
            movement_types: [0; MAX_STEPS],
            steps_left: 0,
            previous_steps_left: 0,
            stalled: 0,
            walk_speed_accumulator: 0,
            last_yaw_rotation: 0,
            turn_direction: 0,
            face_entity: -1,
            yaw: EntityTurnDirection::default(),
            roll: EntityTurnDirection::default(),
            pitch: EntityTurnDirection::default(),
            turning_ticks: 0,
            size,
            delay_movement: true,
            seq_stall: None,
        }
    }

    pub fn turn(&mut self, rotation: i32, bas: &BasParams) {
        if bas.yaw_acceleration != 0 || self.turn_direction != 0 {
            self.yaw.clamp();
            let delta = rotation.wrapping_sub(self.yaw.rotation) & 0x3fff;
            self.last_yaw_rotation = if delta > 8192 {
                self.yaw.rotation.wrapping_sub(16384 - delta)
            } else {
                self.yaw.rotation.wrapping_add(delta)
            };
        }
    }

    pub fn turn_force(&mut self, rotation: i32, force: bool, bas: &BasParams) {
        if force || bas.yaw_acceleration != 0 || self.turn_direction != 0 {
            self.last_yaw_rotation = rotation & 0x3fff;
            self.yaw.set_rotation(self.last_yaw_rotation);
        }
    }

    pub fn set_facing_gradual(&mut self, bas: &BasParams) -> i32 {
        let current = self.yaw.rotation;
        let is_turning = if bas.yaw_acceleration != 0 {
            self.yaw.calculate_rotation(
                self.last_yaw_rotation,
                bas.yaw_acceleration,
                bas.yaw_max_velocity,
            )
        } else {
            self.yaw.calculate_rotation(
                self.last_yaw_rotation,
                self.turn_direction,
                self.turn_direction,
            )
        };
        let rotation = self.yaw.rotation.wrapping_sub(current);
        if rotation != 0 {
            self.turning_ticks = self.turning_ticks.wrapping_add(1);
        } else {
            self.turning_ticks = 0;
            self.yaw.set_rotation(self.last_yaw_rotation);
        }
        if is_turning {
            if bas.roll_acceleration != 0 {
                let target = if rotation > 0 {
                    bas.roll_target_angle
                } else {
                    bas.roll_target_angle.wrapping_neg()
                };
                self.roll
                    .calculate_rotation(target, bas.roll_acceleration, bas.roll_max_velocity);
            }
            if bas.pitch_acceleration != 0 {
                self.pitch.calculate_rotation(
                    bas.pitch_target_angle,
                    bas.pitch_acceleration,
                    bas.pitch_max_velocity,
                );
            }
        } else {
            if bas.roll_acceleration != 0 {
                self.roll
                    .calculate_rotation(0, bas.roll_acceleration, bas.roll_max_velocity);
            } else {
                self.roll.set_rotation(0);
            }
            if bas.pitch_acceleration != 0 {
                self.pitch
                    .calculate_rotation(0, bas.pitch_acceleration, bas.pitch_max_velocity);
            } else {
                self.pitch.set_rotation(0);
            }
        }
        rotation
    }

    pub fn turn_to(&mut self, bas: &BasParams) -> i32 {
        if self.turn_direction == 0 {
            return 0;
        }
        self.set_facing_gradual(bas)
    }

    pub fn update_position(&mut self, cutscene: bool, bas: &BasParams) -> StepOutput {
        let mut out = StepOutput {
            movement_type: MOVE_STATIONARY,
            turn_direction_index: 0,
        };
        if self.steps_left == 0 {
            self.stalled = 0;
            return out;
        }
        if let Some(seq) = self.seq_stall {
            if self.previous_steps_left > 0 && seq.interrupt_type == 0 {
                self.stalled = self.stalled.wrapping_add(1);
                return out;
            }
            if self.previous_steps_left <= 0 && seq.move_type == 0 {
                self.stalled = self.stalled.wrapping_add(1);
                return out;
            }
        }

        let start_x = self.x as i32;
        let start_z = self.z as i32;
        let last = (self.steps_left - 1) as usize;
        let end_x = self.movement_base_x[last]
            .wrapping_mul(512)
            .wrapping_add(self.size.wrapping_mul(256));
        let end_z = self.movement_base_y[last]
            .wrapping_mul(512)
            .wrapping_add(self.size.wrapping_mul(256));

        if start_x < end_x {
            if start_z < end_z {
                self.turn(10240, bas);
            } else if start_z > end_z {
                self.turn(14336, bas);
            } else {
                self.turn(12288, bas);
            }
        } else if start_x > end_x {
            if start_z < end_z {
                self.turn(6144, bas);
            } else if start_z > end_z {
                self.turn(2048, bas);
            } else {
                self.turn(4096, bas);
            }
        } else if start_z < end_z {
            self.turn(8192, bas);
        } else if start_z > end_z {
            self.turn(0, bas);
        }

        let move_type = self.movement_types[last] as i32;
        let dx = end_x.wrapping_sub(start_x);
        let dz = end_z.wrapping_sub(start_z);
        if cutscene || ((-1024..=1024).contains(&dx) && (-1024..=1024).contains(&dz)) {
            let mut delta: i32 = 16;
            if self.delay_movement {
                let yaw_delta = self.last_yaw_rotation.wrapping_sub(self.yaw.rotation);
                if yaw_delta != 0 && self.face_entity == -1 && self.turn_direction != 0 {
                    delta = 8;
                }
                if !cutscene && self.steps_left > 2 {
                    delta = 24;
                }
                if !cutscene && self.steps_left > 3 {
                    delta = 32;
                }
            } else {
                if !cutscene && self.steps_left > 1 {
                    delta = 24;
                }
                if !cutscene && self.steps_left > 2 {
                    delta = 32;
                }
            }
            if self.stalled > 0 && self.steps_left > 1 {
                delta = 32;
                self.stalled -= 1;
            }
            if move_type == MOVE_RUNNING {
                delta <<= 1;
            } else if move_type == MOVE_HALF_WALK {
                delta >>= 1;
            }

            if bas.unused != -1 {
                delta <<= 9;
                if self.steps_left == 1 {
                    let speed_sq = self
                        .walk_speed_accumulator
                        .wrapping_mul(self.walk_speed_accumulator);
                    let size_x = (self.x as i32 - end_x).abs() << 9;
                    let size_z = (self.z as i32 - end_z).abs() << 9;
                    let max_size = size_x.max(size_z);
                    let threshold = max_size.wrapping_mul(bas.unused).wrapping_mul(2);
                    if speed_sq > threshold {
                        self.walk_speed_accumulator /= 2;
                    } else if speed_sq / 2 > max_size {
                        self.walk_speed_accumulator =
                            self.walk_speed_accumulator.wrapping_sub(bas.unused);
                        if self.walk_speed_accumulator < 0 {
                            self.walk_speed_accumulator = 0;
                        }
                    } else if self.walk_speed_accumulator < delta {
                        self.walk_speed_accumulator =
                            self.walk_speed_accumulator.wrapping_add(bas.unused);
                        if self.walk_speed_accumulator > delta {
                            self.walk_speed_accumulator = delta;
                        }
                    }
                } else if self.walk_speed_accumulator < delta {
                    self.walk_speed_accumulator =
                        self.walk_speed_accumulator.wrapping_add(bas.unused);
                    if self.walk_speed_accumulator > delta {
                        self.walk_speed_accumulator = delta;
                    }
                } else {
                    self.walk_speed_accumulator =
                        self.walk_speed_accumulator.wrapping_sub(bas.unused);
                    if self.walk_speed_accumulator < 0 {
                        self.walk_speed_accumulator = 0;
                    }
                }
                delta = self.walk_speed_accumulator >> 9;
                if delta < 1 {
                    delta = 1;
                }
            }

            out.turn_direction_index = 0;
            if end_x == start_x && end_z == start_z {
                out.movement_type = MOVE_STATIONARY;
            } else {
                if start_x < end_x {
                    self.x += delta as f32;
                    out.turn_direction_index |= 0x4;
                    if self.x > end_x as f32 {
                        self.x = end_x as f32;
                    }
                } else if start_x > end_x {
                    self.x -= delta as f32;
                    out.turn_direction_index |= 0x8;
                    if self.x < end_x as f32 {
                        self.x = end_x as f32;
                    }
                }
                if start_z < end_z {
                    self.z += delta as f32;
                    out.turn_direction_index |= 0x1;
                    if self.z > end_z as f32 {
                        self.z = end_z as f32;
                    }
                } else if start_z > end_z {
                    self.z -= delta as f32;
                    out.turn_direction_index |= 0x2;
                    if self.z < end_z as f32 {
                        self.z = end_z as f32;
                    }
                }
                out.movement_type = if delta >= 32 { MOVE_RUNNING } else { move_type };
            }
            if end_x == self.x as i32 && end_z == self.z as i32 {
                self.steps_left -= 1;
                if self.previous_steps_left > 0 {
                    self.previous_steps_left -= 1;
                }
            }
        } else {
            self.x = end_x as f32;
            self.z = end_z as f32;
            let last_yaw = self.last_yaw_rotation;
            self.turn_force(last_yaw, false, bas);
            self.steps_left -= 1;
            if self.previous_steps_left > 0 {
                self.previous_steps_left -= 1;
            }
            out.movement_type = MOVE_STATIONARY;
            out.turn_direction_index = 0;
        }
        out
    }
}
