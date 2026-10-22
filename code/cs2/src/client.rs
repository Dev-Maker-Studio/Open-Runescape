use std::sync::Arc;

macro_rules! preferences {
    ($($field:ident),* $(,)?) => {
        #[derive(Clone, Debug, Default, Eq, PartialEq)]
        pub struct Preferences {
            $(pub $field: i32,)*
        }

        impl Preferences {
            pub const NAMES: &'static [&'static str] = &[$(stringify!($field)),*];

            pub fn to_array(&self) -> Vec<i32> {
                vec![$(self.$field),*]
            }

            pub fn from_slice(values: &[i32]) -> Option<Self> {
                if values.len() != Self::NAMES.len() {
                    return None;
                }
                let mut iter = values.iter().copied();
                Some(Self {
                    $($field: iter.next()?,)*
                })
            }
        }
    };
}

preferences!(
    sound_effect_volume,
    ambient_sound_volume,
    voice_over_volume,
    music_volume,
    sound_level,
    mono_stereo,
    brightness,
    water,
    custom_cursor,
    remove_roofs,
    remove_roofs_option_override,
    scenery_shadows,
    skyboxes,
    particles,
    anti_aliasing,
    anti_aliasing_preview,
    bloom,
    character_shadows,
    cpu,
    fog,
    graphics,
    ground_blending,
    ground_decoration,
    idle_animations,
    light_detail,
    map_size,
    max_screen_size,
    textures,
    default_toolkit,
    flickering_effects,
    gametip_start_stage,
    toolkit,
);

pub const AREA_SOUND_CAPACITY: usize = 50;

pub const TOOLKIT_MODERN: i32 = 3;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum AreaSoundKind {
    Synth,
    Vorbis,
    Speech,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct AreaSound {
    pub kind: AreaSoundKind,
    pub id: i32,
    pub repeats: i32,
    pub delay: i32,
    pub volume: i32,
    pub position: i32,
    pub rate: i32,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MouseState {
    pub x: i32,
    pub y: i32,
    pub left: bool,
    pub right: bool,
    pub middle: bool,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct KeyState {
    pub alt: bool,
    pub ctrl: bool,
    pub shift: bool,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ClientState {
    pub preferences: Preferences,
    pub game_runescape: bool,
    pub max_memory: i32,
    pub rendering_scene: bool,
    pub mouse: Option<MouseState>,
    pub keys: Option<KeyState>,
    pub cycles: i32,
    pub affiliate: i32,
    pub billing_flag: i32,
    pub sign_on_key: Option<Arc<[u8]>>,
    pub machine_uid: i64,
    pub forced_safemode: bool,
    pub safemode_forced_during_startup: bool,
    pub custom_string_param: Option<Arc<str>>,
    pub just_created_account: bool,
    pub applet: bool,
    pub map_owner_identifier: Option<Arc<str>>,
    pub quickchat_only: bool,
    pub current_cursor: i32,
    pub interface_selection_active: bool,
    pub use_on_type: Option<Arc<str>>,
    pub use_on_target: Option<Arc<str>>,
    pub userdetail_quickchat: bool,
    pub verified_email_address: bool,
    pub applet_has_focus: bool,
    pub login_queue_position: i32,
    pub login_response_detail: i32,
    pub login_response_extra_info: i32,
    pub lobby_login_response: i32,
    pub world_login_response: i32,
    pub login_handshake_code: i32,
    pub minimenu_entry_count: i32,
    pub minimenu_coloured_entry_count: i32,
    pub default_attack_cursor: i32,
    pub default_cursor_id: i32,
    pub instruction_count: i32,
    pub preferences_saved_on_server: bool,
    pub needs_resize: bool,
    pub particle_details: i32,
    pub area_sound_count: i32,
    pub area_sounds: Vec<Option<AreaSound>>,
    pub current_music_archive: i32,
    pub titlescreen_music_archive: i32,
    pub sound_level_static: i32,
    pub song_load_state: i32,
    pub sound_jingle_playing: bool,
    pub login: crate::LoginState,
}

impl ClientState {
    pub fn new() -> Self {
        Self {
            area_sounds: vec![None; AREA_SOUND_CAPACITY],
            current_music_archive: -1,
            titlescreen_music_archive: -1,
            default_attack_cursor: -1,
            default_cursor_id: -1,
            current_cursor: -1,
            preferences: Preferences {
                toolkit: TOOLKIT_MODERN,
                ..Preferences::default()
            },
            ..Self::default()
        }
    }

    pub fn queue_area_sound(&mut self, sound: AreaSound) -> Result<(), crate::HostError> {
        let volume = if sound.kind == AreaSoundKind::Speech {
            self.preferences.voice_over_volume
        } else {
            self.preferences.sound_effect_volume
        };
        if volume != 0
            && sound.repeats != 0
            && self.area_sound_count < AREA_SOUND_CAPACITY as i32
            && sound.id != -1
        {
            self.area_sound_count = self.area_sound_count.wrapping_add(1);
            let index = self.area_sound_count.wrapping_sub(1);
            let slot = usize::try_from(index)
                .ok()
                .and_then(|index| self.area_sounds.get_mut(index))
                .ok_or_else(|| crate::HostError::State("area sound cache index".into()))?;
            *slot = Some(sound);
        }
        Ok(())
    }
}
