use crate::HostError;
use std::collections::BTreeSet;

pub const SPLINE_PATH_COUNT: usize = 2;
pub const CAMERA_SHAKE_SLOTS: usize = 5;
const SPLINE_LIMIT: i32 = 1 << 20;

pub type SplinePath = Vec<Vec<i32>>;

#[derive(Clone, Debug, PartialEq)]
pub struct CameraState {
    pub calculation_type: i32,
    pub yaw: i32,
    pub roll: i32,
    pub pitch: i32,
    pub move_speed: i32,
    pub move_angle: i32,
    pub movement_x: i32,
    pub movement_y: i32,
    pub movement_z: i32,
    pub constant_speed: i32,
    pub variable_speed: i32,
    pub position_x: i32,
    pub position_y: i32,
    pub position_z: i32,
    pub transition_x: i32,
    pub transition_y: i32,
    pub transition_z: i32,
    pub transition_yaw: i32,
    pub transition_roll: i32,
    pub angle_x: f32,
    pub angle_y: f32,
    pub target_x: i32,
    pub target_y: i32,
    pub ping_target_x: i32,
    pub static_target_y: i32,
    pub max_zoom: i32,
    pub base_zoom: i32,
    pub position_path_index: i32,
    pub look_at_path_index: i32,
    pub position_keyframe: i32,
    pub look_at_keyframe: i32,
    pub spline_progress: i32,
    pub spline_speed_start: i32,
    pub spline_speed_end: i32,
    pub splines: [Option<SplinePath>; SPLINE_PATH_COUNT],
    pub pitch_velocity: f32,
    pub yaw_velocity: f32,
    pub pitch_velocity_applied: bool,
    pub yaw_velocity_applied: bool,
    pub send_angle_to_server: bool,
    pub last_smooth_cycle: i32,
    pub cycles: i32,
    pub shaking: [bool; CAMERA_SHAKE_SLOTS],
    pub viewport_fov_min: i16,
    pub viewport_fov_max: i16,
    pub zoom_min: i16,
    pub zoom_max: i16,
    pub fov_scale_min: i16,
    pub fov_scale_max: i16,
    pub aspect_fov_min: i16,
    pub aspect_fov_max: i16,
    pub scene_base_x: i32,
    pub scene_base_y: i32,
    pub scene_size_x: i32,
    pub scene_size_y: i32,
}

impl Default for CameraState {
    fn default() -> Self {
        Self {
            calculation_type: 0,
            yaw: 0,
            roll: 0,
            pitch: 0,
            move_speed: 0,
            move_angle: 0,
            movement_x: 0,
            movement_y: 0,
            movement_z: 0,
            constant_speed: 0,
            variable_speed: 0,
            position_x: 0,
            position_y: 0,
            position_z: 0,
            transition_x: 0,
            transition_y: 0,
            transition_z: 0,
            transition_yaw: 0,
            transition_roll: 0,
            angle_x: 0.0,
            angle_y: 0.0,
            target_x: -1,
            target_y: -1,
            ping_target_x: -1,
            static_target_y: -1,
            max_zoom: 0,
            base_zoom: 0,
            position_path_index: -1,
            look_at_path_index: -1,
            position_keyframe: 0,
            look_at_keyframe: 0,
            spline_progress: 0,
            spline_speed_start: 0,
            spline_speed_end: 0,
            splines: [None, None],
            pitch_velocity: 0.0,
            yaw_velocity: 0.0,
            pitch_velocity_applied: false,
            yaw_velocity_applied: false,
            send_angle_to_server: false,
            last_smooth_cycle: 0,
            cycles: 0,
            shaking: [false; CAMERA_SHAKE_SLOTS],
            viewport_fov_min: 256,
            viewport_fov_max: 205,
            zoom_min: 256,
            zoom_max: 320,
            fov_scale_min: 1,
            fov_scale_max: 32767,
            aspect_fov_min: 1,
            aspect_fov_max: 32767,
            scene_base_x: 0,
            scene_base_y: 0,
            scene_size_x: 0,
            scene_size_y: 0,
        }
    }
}

fn index_error(index: i32) -> HostError {
    HostError::State(format!("array index {index} out of bounds"))
}

pub const RUNTIME_FAILURE: &str = "runtime exception";
pub const NEGATIVE_ARRAY_SIZE: &str = "negative array size";

impl CameraState {
    pub fn angle_x_units(&self) -> i32 {
        (self.angle_x as i32) >> 3
    }

    pub fn angle_y_units(&self) -> i32 {
        (self.angle_y as i32) >> 3
    }

    pub fn follow_height(&self) -> i32 {
        self.max_zoom.wrapping_sub(self.base_zoom)
    }

    pub fn set_follow_height(&mut self, height: i32) {
        self.max_zoom = height.max(0).wrapping_add(self.base_zoom);
    }

    pub fn clear_scripted_targets(&mut self) {
        self.static_target_y = -1;
        self.ping_target_x = -1;
    }

    pub fn move_to(&mut self, coord: i32, height: i32, constant_speed: i32, variable_speed: i32) {
        self.movement_x = ((coord >> 14) & 0x3fff).wrapping_sub(self.scene_base_x);
        self.movement_y = (coord & 0x3fff).wrapping_sub(self.scene_base_y);
        self.movement_z = height << 2;
        self.constant_speed = constant_speed;
        self.variable_speed = variable_speed;
        self.calculation_type = 5;
        self.clear_scripted_targets();
    }

    fn clamp_to_scene(&self, tile: i32, size: i32) -> i32 {
        if tile < 0 {
            0
        } else if tile >= size {
            size
        } else {
            tile
        }
    }

    pub fn follow_coord(&mut self, coord: i32) {
        let tile_x = self.clamp_to_scene(
            ((coord >> 14) & 0x3fff).wrapping_sub(self.scene_base_x),
            self.scene_size_x,
        );
        let tile_y = self.clamp_to_scene(
            (coord & 0x3fff).wrapping_sub(self.scene_base_y),
            self.scene_size_y,
        );
        self.target_x = (tile_x << 9).wrapping_add(256);
        self.target_y = (tile_y << 9).wrapping_add(256);
        self.calculation_type = 4;
        self.clear_scripted_targets();
    }

    pub fn clamp_coord_or_default(&mut self, coord: i32) {
        if coord == -1 {
            let tile_x = self.clamp_to_scene(16383i32.wrapping_sub(self.scene_base_x), self.scene_size_x);
            let tile_y = self.clamp_to_scene(16383i32.wrapping_sub(self.scene_base_y), self.scene_size_y);
            self.ping_target_x = (tile_x << 9).wrapping_add(256);
            self.static_target_y = (tile_y << 9).wrapping_add(256);
        } else {
            self.clear_scripted_targets();
        }
    }

    pub fn nudge_pitch(&mut self, target: f32) {
        if !self.pitch_velocity_applied {
            self.pitch_velocity += (target - self.pitch_velocity) / 2.0;
            self.send_angle_to_server = true;
            self.pitch_velocity_applied = true;
        }
    }

    pub fn nudge_yaw(&mut self, target: f32) {
        if !self.yaw_velocity_applied {
            self.yaw_velocity += (target - self.yaw_velocity) / 2.0;
            self.send_angle_to_server = true;
            self.yaw_velocity_applied = true;
        }
    }

    fn stop_scripted_motion(&mut self) {
        self.shaking = [false; CAMERA_SHAKE_SLOTS];
        self.position_path_index = -1;
        self.look_at_path_index = -1;
        self.move_speed = 0;
        self.move_angle = 0;
    }

    pub fn smooth_reset(&mut self) {
        self.stop_scripted_motion();
        self.calculation_type = 2;
        self.clear_scripted_targets();
    }

    pub fn start_smooth_transition(&mut self) {
        self.stop_scripted_motion();
        self.calculation_type = 1;
        self.clear_scripted_targets();
        self.last_smooth_cycle = self.cycles;
        self.transition_x = self.position_x;
        self.transition_z = self.position_z;
        self.transition_y = self.position_y;
        self.transition_yaw = self.yaw;
        self.transition_roll = self.roll;
    }

    pub fn spline_new(&mut self, path: i32, keyframes: i32) -> Result<(), HostError> {
        if !(0..SPLINE_PATH_COUNT as i32).contains(&path) {
            return Ok(());
        }
        let length = keyframes << 1;
        if length < 0 {
            return Err(HostError::State(NEGATIVE_ARRAY_SIZE.to_string()));
        }
        if length > SPLINE_LIMIT {
            return Err(HostError::State("array too large".to_string()));
        }
        self.splines[path as usize] = Some(vec![vec![0; 4]; length as usize]);
        Ok(())
    }

    pub fn spline_add_point(
        &mut self,
        path: i32,
        keyframe: i32,
        position: (i32, i32),
        look_at: (i32, i32),
        pitch: i32,
    ) {
        let base = keyframe << 1;
        if !(0..SPLINE_PATH_COUNT as i32).contains(&path) || base < 0 {
            return;
        }
        let Some(points) = self.splines[path as usize].as_mut() else {
            return;
        };
        let base = base as usize;
        if base >= points.len() {
            return;
        }
        let (pos_coord, pos_height) = position;
        let (look_coord, look_height) = look_at;
        points[base] = vec![
            ((pos_coord >> 14) & 0x3fff) << 9,
            pos_height << 2,
            (pos_coord & 0x3fff) << 9,
            pitch,
        ];
        if let Some(slot) = points.get_mut(base + 1) {
            *slot = vec![
                ((look_coord >> 14) & 0x3fff) << 9,
                look_height << 2,
                (look_coord & 0x3fff) << 9,
            ];
        }
    }

    pub fn spline_length(&self, path: i32) -> Result<i32, HostError> {
        let slot = usize::try_from(path)
            .ok()
            .and_then(|index| self.splines.get(index))
            .ok_or_else(|| index_error(path))?;
        let points = slot.as_ref().ok_or(HostError::NoEntity)?;
        Ok((points.len() as i32) >> 1)
    }

    fn path_length(&self, path: i32) -> Result<i32, HostError> {
        self.spline_length(path)
    }

    pub fn move_along(&mut self, arguments: [i32; 6]) -> Result<(), HostError> {
        let [position_path, position_keyframe, speed_start, speed_end, look_path, look_keyframe] =
            arguments;
        if position_path >= SPLINE_PATH_COUNT as i32 {
            return Err(HostError::State(RUNTIME_FAILURE.to_string()));
        }
        self.position_path_index = position_path;
        if position_keyframe.wrapping_add(1) >= self.path_length(position_path)? {
            return Err(HostError::State(RUNTIME_FAILURE.to_string()));
        }
        self.position_keyframe = position_keyframe;
        self.spline_progress = 0;
        self.spline_speed_start = speed_start;
        self.spline_speed_end = speed_end;
        if look_path >= SPLINE_PATH_COUNT as i32 {
            return Err(HostError::State(RUNTIME_FAILURE.to_string()));
        }
        self.look_at_path_index = look_path;
        if look_keyframe.wrapping_add(1) >= self.path_length(look_path)? {
            return Err(HostError::State(RUNTIME_FAILURE.to_string()));
        }
        self.look_at_keyframe = look_keyframe;
        self.calculation_type = 3;
        self.clear_scripted_targets();
        Ok(())
    }

    pub fn set_viewport_fov(&mut self, minimum: i32, maximum: i32) {
        self.viewport_fov_min = minimum as i16;
        if self.viewport_fov_min <= 0 {
            self.viewport_fov_min = 256;
        }
        self.viewport_fov_max = maximum as i16;
        if self.viewport_fov_max <= 0 {
            self.viewport_fov_max = 205;
        }
    }

    pub fn set_viewport_zoom(&mut self, minimum: i32, maximum: i32) {
        self.zoom_min = minimum as i16;
        if self.zoom_min <= 0 {
            self.zoom_min = 256;
        }
        self.zoom_max = maximum as i16;
        if self.zoom_max <= 0 {
            self.zoom_max = 320;
        }
    }

    pub fn clamp_viewport_fov(&mut self, values: [i32; 4]) {
        self.fov_scale_min = values[0] as i16;
        if self.fov_scale_min <= 0 {
            self.fov_scale_min = 1;
        }
        self.fov_scale_max = values[1] as i16;
        if self.fov_scale_max <= 0 {
            self.fov_scale_max = 32767;
        } else if self.fov_scale_max < self.fov_scale_min {
            self.fov_scale_max = self.fov_scale_min;
        }
        self.aspect_fov_min = values[2] as i16;
        if self.aspect_fov_min <= 0 {
            self.aspect_fov_min = 1;
        }
        self.aspect_fov_max = values[3] as i16;
        if self.aspect_fov_max <= 0 {
            self.aspect_fov_max = 32767;
        } else if self.aspect_fov_max < self.aspect_fov_min {
            self.aspect_fov_max = self.aspect_fov_min;
        }
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct WorldMapState {
    pub hide_icons: bool,
    pub requested_zoom: f32,
    pub square_x: i32,
    pub square_y: i32,
    pub center_x: i32,
    pub center_y: i32,
    pub static_element: Option<i32>,
    pub loading_percentage: i32,
    pub view_width: i32,
    pub viewport_scale: i32,
    pub hidden_elements: BTreeSet<i32>,
    pub hidden_categories: BTreeSet<i32>,
    pub scroll_target_x: i32,
    pub scroll_target_y: i32,
    pub requested_scroll_x: i32,
    pub requested_scroll_y: i32,
    pub use_target_position: bool,
    pub flashed_element: i32,
    pub flashed_category: i32,
    pub flash_cycles: i32,
    pub flash_cycle_counter: i32,
    pub list_cursor: Option<usize>,
    pub effects: Vec<WorldMapEffect>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum WorldMapEffect {
    ReloadAssets,
    LoadingScreen,
}

impl Default for WorldMapState {
    fn default() -> Self {
        Self {
            hide_icons: false,
            requested_zoom: 0.0,
            square_x: 0,
            square_y: 0,
            center_x: 0,
            center_y: 0,
            static_element: None,
            loading_percentage: 0,
            view_width: 0,
            viewport_scale: 0,
            hidden_elements: BTreeSet::new(),
            hidden_categories: BTreeSet::new(),
            scroll_target_x: -1,
            scroll_target_y: -1,
            requested_scroll_x: -1,
            requested_scroll_y: -1,
            use_target_position: false,
            flashed_element: -1,
            flashed_category: -1,
            flash_cycles: 0,
            flash_cycle_counter: 0,
            list_cursor: None,
            effects: Vec::new(),
        }
    }
}

impl WorldMapState {
    pub fn zoom_level(&self) -> i32 {
        if self.requested_zoom == 3.0 {
            37
        } else if self.requested_zoom == 4.0 {
            50
        } else if self.requested_zoom == 6.0 {
            75
        } else if self.requested_zoom == 8.0 {
            100
        } else {
            200
        }
    }

    pub fn request_zoom(&mut self, percent: i32) {
        match percent {
            37 => self.requested_zoom = 3.0,
            50 => self.requested_zoom = 4.0,
            75 => self.requested_zoom = 6.0,
            100 => self.requested_zoom = 8.0,
            200 => self.requested_zoom = 16.0,
            _ => {}
        }
        self.scroll_target_y = -1;
    }

    pub fn set_scroll_target(&mut self, x: i32, y: i32) {
        self.scroll_target_x = x.wrapping_sub(self.square_x);
        self.scroll_target_y = y.wrapping_sub(self.square_y);
    }

    pub fn flash_element(&mut self, id: i32) {
        self.flashed_element = id;
        self.flashed_category = -1;
        self.flash_cycles = 3;
        self.flash_cycle_counter = 100;
    }

    pub fn flash_category(&mut self, id: i32) {
        self.flashed_element = -1;
        self.flashed_category = id;
        self.flash_cycles = 3;
        self.flash_cycle_counter = 100;
    }

    pub fn set_element_hidden(&mut self, id: i32, hidden: bool) {
        if hidden {
            self.hidden_elements.insert(id);
        } else {
            self.hidden_elements.remove(&id);
        }
    }

    pub fn set_category_hidden(&mut self, id: i32, hidden: bool) {
        if hidden {
            self.hidden_categories.insert(id);
        } else {
            self.hidden_categories.remove(&id);
        }
    }
}

#[derive(Clone, Debug, Default, PartialEq)]
pub struct WorldState {
    pub camera: CameraState,
    pub world_map: WorldMapState,
    pub scene: crate::SceneState,
    pub map_data: crate::WorldMapData,
    pub projection: crate::ProjectionState,
    pub mini_menu: MiniMenuState,
    pub platform: crate::PlatformState,
}

#[derive(Clone, Debug, PartialEq)]
pub struct MiniMenuState {
    pub max_visible_entries: i32,
    pub bold_font: bool,
    pub style: MiniMenuStyle,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MiniMenuStyle {
    pub background_color: i32,
    pub background_transparency: i32,
    pub hover_highlight_color: i32,
    pub background_alpha: i32,
    pub loading_bar_background_sprite: i32,
    pub loading_bar_tile_sprite: i32,
    pub top_border_sprite: i32,
    pub side_border_sprite: i32,
    pub corner_sprite: i32,
    pub text_color: i32,
    pub hover_text_color: i32,
    pub font_index: i32,
}

impl Default for MiniMenuState {
    fn default() -> Self {
        Self {
            max_visible_entries: -1,
            bold_font: false,
            style: MiniMenuStyle::default(),
        }
    }
}
