use crate::ParamTable;
use std::collections::BTreeMap;
use std::sync::Arc;

pub const GAME_STATE_RENDERING_SCENE: [i32; 3] = [12, 13, 18];

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct MiniMenuEntryState {
    pub action: Arc<str>,
    pub colored_name: Arc<str>,
    pub target_name: Option<Arc<str>>,
    pub click_type: i32,
    pub quest_icons: Arc<str>,
    pub param0: i32,
    pub param1: i32,
    pub item_id: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct MecDefinition {
    pub area_name: Option<Arc<str>>,
    pub default_icon_archive: i32,
    pub category_id: i32,
    pub base_text_zoom: i32,
    pub params: ParamTable,
}

impl Default for MecDefinition {
    fn default() -> Self {
        Self {
            area_name: None,
            default_icon_archive: -1,
            category_id: -1,
            base_text_zoom: 0,
            params: BTreeMap::new(),
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct DirectoryPickerState {
    pub finished: bool,
    pub picked: Option<Arc<str>>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct MessageBoxState {
    pub anchor_x: i32,
    pub anchor_y: i32,
    pub padding_left: i32,
    pub padding_top: i32,
    pub minimal_text_width: i32,
    pub minimal_text_height: i32,
    pub outline_corner_sprite: i32,
    pub outline_straight_sprite: i32,
    pub filled_background_sprite: i32,
    pub text_color: i32,
    pub box_id: i32,
}

#[derive(Clone, Debug, PartialEq)]
pub struct PlatformState {
    pub language: Option<i32>,
    pub fullscreen_supported: bool,
    pub fullscreen_frame: bool,
    pub fullscreen_window_mode: bool,
    pub fixed_screen: i32,
    pub hostname_identifier: bool,
    pub hostname: Option<Arc<str>>,
    pub clipboard: Option<Arc<str>>,
    pub game_state: i32,
    pub preference_file: Option<Arc<str>>,
    pub directory_picker: Option<DirectoryPickerState>,
    pub jagtheora_loaded: bool,
    pub mini_menu_open: bool,
    pub active_entry: Option<MiniMenuEntryState>,
    pub secondary_entry: Option<MiniMenuEntryState>,
    pub entries: Vec<MiniMenuEntryState>,
    pub window_mode_requests: Vec<i32>,
    pub full_screen_preference: i32,
    pub preference_saves: u32,
    pub java_build: i32,
    pub java_update: i32,
    pub cpu_profile: i32,
    pub message_box: Option<MessageBoxState>,
    pub mec: BTreeMap<i32, MecDefinition>,
}

impl Default for PlatformState {
    fn default() -> Self {
        Self {
            language: None,
            fullscreen_supported: true,
            fullscreen_frame: false,
            fullscreen_window_mode: false,
            fixed_screen: 0,
            hostname_identifier: false,
            hostname: None,
            clipboard: None,
            game_state: 0,
            preference_file: None,
            directory_picker: None,
            jagtheora_loaded: false,
            mini_menu_open: false,
            active_entry: None,
            secondary_entry: None,
            entries: Vec::new(),
            window_mode_requests: Vec::new(),
            full_screen_preference: 0,
            preference_saves: 0,
            java_build: 17,
            java_update: 0,
            cpu_profile: 0,
            message_box: None,
            mec: BTreeMap::new(),
        }
    }
}

impl PlatformState {
    pub fn window_mode(&self) -> i32 {
        if self.fullscreen_supported && self.fullscreen_frame {
            3
        } else if self.fullscreen_window_mode {
            2
        } else {
            1
        }
    }

    pub fn rendering_scene(&self) -> bool {
        GAME_STATE_RENDERING_SCENE.contains(&self.game_state)
    }
}

pub fn mini_menu_click_type(open: bool, entry: Option<i32>) -> i32 {
    if open {
        return 6;
    }
    let Some(click_type) = entry else {
        return 0;
    };
    match click_type {
        57 | 58 | 1007 | 25 | 30 => 1,
        18..=22 | 1004 | 17 => 2,
        3..=6 | 1001 | 1002 | 2 => 3,
        9..=13 | 1003 | 8 => 4,
        44..=53 | 15 => 7,
        16 => 8,
        _ => 5,
    }
}
