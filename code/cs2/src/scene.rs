use crate::fdlibm::atan2;
use crate::world::CameraState;
use crate::HostError;

const TILE_SCALE: u32 = 9;
const TILE_UNITS: i32 = 1 << TILE_SCALE;
const PLANES: usize = 4;
const YAW_TO_UNITS: f64 = 2607.5945876176133;

#[derive(Clone, Debug, Default, PartialEq)]
pub struct HeightGrid {
    pub width: i32,
    pub length: i32,
    pub heights: Vec<Vec<i32>>,
}

pub type LightingPlane = Option<Vec<Vec<i8>>>;

#[derive(Clone, Debug, Default, PartialEq)]
pub struct SceneState {
    pub loaded: bool,
    pub my_plane: i32,
    pub planes: [Option<HeightGrid>; PLANES],
    pub roof_masks: Vec<Vec<i8>>,
    pub lighting: Option<Vec<LightingPlane>>,
    pub look_x: i32,
    pub look_y: i32,
    pub look_height_offset: i32,
    pub camera_base_x: i32,
    pub camera_base_y: i32,
    pub regular_lowest_yaw: i32,
}

fn index_error() -> HostError {
    HostError::State("array index out of bounds".to_string())
}

fn cell<T: Copy>(rows: &[Vec<T>], x: i32, y: i32) -> Result<T, HostError> {
    usize::try_from(x)
        .ok()
        .and_then(|x| rows.get(x))
        .and_then(|row| usize::try_from(y).ok().and_then(|y| row.get(y)))
        .copied()
        .ok_or_else(index_error)
}

impl HeightGrid {
    pub fn height(&self, x: i32, y: i32) -> Result<i32, HostError> {
        cell(&self.heights, x, y)
    }

    pub fn average_height(&self, x: i32, y: i32) -> Result<i32, HostError> {
        let scaled_x = x >> TILE_SCALE;
        let scaled_y = y >> TILE_SCALE;
        if scaled_x >= 0
            && scaled_y >= 0
            && scaled_x <= self.width.wrapping_sub(1)
            && scaled_y <= self.length.wrapping_sub(1)
        {
            let offset_x = x & (TILE_UNITS - 1);
            let offset_y = y & (TILE_UNITS - 1);
            let blend = |near: i32, far: i32| {
                near.wrapping_mul(TILE_UNITS - offset_x)
                    .wrapping_add(offset_x.wrapping_mul(far))
                    >> TILE_SCALE
            };
            let height1 = blend(
                self.height(scaled_x, scaled_y)?,
                self.height(scaled_x + 1, scaled_y)?,
            );
            let height2 = blend(
                self.height(scaled_x, scaled_y + 1)?,
                self.height(scaled_x + 1, scaled_y + 1)?,
            );
            return Ok(height2
                .wrapping_mul(offset_y)
                .wrapping_add(height1.wrapping_mul(TILE_UNITS - offset_y))
                >> TILE_SCALE);
        }
        Ok(0)
    }
}

impl SceneState {
    fn plane_grid(&self, plane: i32) -> Result<Option<&HeightGrid>, HostError> {
        usize::try_from(plane)
            .ok()
            .and_then(|plane| self.planes.get(plane))
            .map(Option::as_ref)
            .ok_or_else(index_error)
    }

    pub fn tile_height(
        &self,
        camera: &CameraState,
        absolute_x: i32,
        absolute_y: i32,
        plane: i32,
    ) -> Result<i32, HostError> {
        if !self.loaded {
            return Ok(0);
        }
        let scene_x = absolute_x >> TILE_SCALE;
        let scene_y = absolute_y >> TILE_SCALE;
        if scene_x >= 0
            && scene_y >= 0
            && scene_x <= camera.scene_size_x.wrapping_sub(1)
            && scene_y <= camera.scene_size_y.wrapping_sub(1)
        {
            let mut collision_plane = plane;
            if plane < 3 && (cell(&self.roof_masks, scene_x, scene_y)? & 0x2) != 0 {
                collision_plane = plane + 1;
            }
            return self
                .plane_grid(collision_plane)?
                .ok_or(HostError::NoEntity)?
                .average_height(absolute_x, absolute_y);
        }
        Ok(0)
    }

    fn roofs_hidden(&self, x: i32, y: i32) -> bool {
        let Some(column) = self.roof_masks.get(usize::try_from(x).unwrap_or(usize::MAX)) else {
            return false;
        };
        x >= 0
            && y >= 0
            && column
                .get(usize::try_from(y).unwrap_or(usize::MAX))
                .is_some_and(|mask| (mask & 0x2) != 0)
    }

    fn lighting_at(&self, plane: i32, x: i32, y: i32) -> Result<i32, HostError> {
        let Some(planes) = &self.lighting else {
            return Ok(0);
        };
        let slot = usize::try_from(plane)
            .ok()
            .and_then(|plane| planes.get(plane))
            .ok_or_else(index_error)?;
        let Some(rows) = slot else {
            return Ok(0);
        };
        let value = cell(rows, x, y)?;
        Ok(((i32::from(value) & 0xff) * 8) << 2)
    }

    pub fn process_camera(&mut self, camera: &mut CameraState) -> Result<(), HostError> {
        camera.angle_x = camera.angle_x.clamp(1081.0, 2980.0);
        while camera.angle_y >= 16384.0 {
            camera.angle_y -= 16384.0;
        }
        while camera.angle_y < 0.0 {
            camera.angle_y += 16384.0;
        }
        if !self.loaded {
            return Err(HostError::NoEntity);
        }
        let player_x = self.camera_base_x >> TILE_SCALE;
        let player_y = self.camera_base_y >> TILE_SCALE;
        let tile_height = self.tile_height(
            camera,
            self.camera_base_x,
            self.camera_base_y,
            self.my_plane,
        )?;
        let mut highest = 0i32;
        if player_x > 3
            && player_y > 3
            && player_x < camera.scene_size_x.wrapping_sub(4)
            && player_y < camera.scene_size_y.wrapping_sub(4)
        {
            for x in player_x - 4..=player_x + 4 {
                for y in player_y - 4..=player_y + 4 {
                    let mut plane = self.my_plane;
                    if plane < 3 && self.roofs_hidden(x, y) {
                        plane += 1;
                    }
                    let lighting = self.lighting_at(plane, x, y)?;
                    if let Some(grid) = self.plane_grid(plane)? {
                        let offset = tile_height.wrapping_sub(grid.height(x, y)?.wrapping_sub(lighting));
                        if offset > highest {
                            highest = offset;
                        }
                    }
                }
            }
        }
        let mut yaw = (highest >> 2).wrapping_mul(1536);
        yaw = yaw.clamp(262_144, 786_432);
        if yaw > self.regular_lowest_yaw {
            self.regular_lowest_yaw = self
                .regular_lowest_yaw
                .wrapping_add(yaw.wrapping_sub(self.regular_lowest_yaw) / 24);
        } else if yaw < self.regular_lowest_yaw {
            self.regular_lowest_yaw = self
                .regular_lowest_yaw
                .wrapping_add(yaw.wrapping_sub(self.regular_lowest_yaw) / 80);
        }
        Ok(())
    }

    pub fn set_angle(
        &mut self,
        camera: &mut CameraState,
        angle_x: i32,
        angle_y: i32,
        pitch: i32,
    ) -> Result<(), HostError> {
        let shifted_x = angle_x << 3;
        let shifted_y = angle_y << 3;
        let shifted_pitch = pitch << 3;
        camera.angle_x = shifted_x as f32;
        camera.angle_y = shifted_y as f32;
        if camera.calculation_type == 5 {
            camera.yaw = shifted_x;
            camera.roll = shifted_y;
            camera.pitch = shifted_pitch;
        }
        self.process_camera(camera)?;
        camera.send_angle_to_server = true;
        Ok(())
    }

    pub fn look_at(
        &mut self,
        camera: &mut CameraState,
        eye_x: i32,
        eye_y: i32,
        height_offset: i32,
        speed: i32,
        angle: i32,
    ) -> Result<(), HostError> {
        self.look_x = eye_x;
        self.look_y = eye_y;
        self.look_height_offset = height_offset;
        camera.move_speed = speed;
        camera.move_angle = angle;
        if camera.move_angle >= 100 {
            let look_absolute_x = self.look_x.wrapping_mul(512).wrapping_add(256);
            let look_absolute_y = self.look_y.wrapping_mul(512).wrapping_add(256);
            let look_absolute_z = self
                .tile_height(camera, look_absolute_x, look_absolute_y, self.my_plane)?
                .wrapping_sub(self.look_height_offset);
            let delta_x = look_absolute_x.wrapping_sub(camera.position_x);
            let delta_z = look_absolute_z.wrapping_sub(camera.position_z);
            let delta_y = look_absolute_y.wrapping_sub(camera.position_y);
            let horizontal = f64::from(
                delta_x
                    .wrapping_mul(delta_x)
                    .wrapping_add(delta_y.wrapping_mul(delta_y)),
            )
            .sqrt() as i32;
            camera.yaw = ((atan2(f64::from(delta_z), f64::from(horizontal)) * YAW_TO_UNITS) as i32)
                & 0x3fff;
            camera.roll = ((atan2(f64::from(delta_x), f64::from(delta_y)) * -YAW_TO_UNITS) as i32)
                & 0x3fff;
            camera.pitch = 0;
            camera.yaw = camera.yaw.clamp(1024, 3072);
        }
        camera.calculation_type = 5;
        camera.clear_scripted_targets();
        Ok(())
    }
}
