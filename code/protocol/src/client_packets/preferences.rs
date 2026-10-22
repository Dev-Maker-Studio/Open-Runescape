pub const PREFERENCE_VERSION: u8 = 28;
pub const PREFERENCE_VALUES: usize = 32;
pub const PREFERENCE_BYTES: usize = 35;
pub const TOOLKIT_MODERN_OPENGL: u8 = 3;

pub const FIELD_NAMES: [&str; PREFERENCE_VALUES] = [
    "antiAliasing",
    "bloom",
    "brightness",
    "mapSize",
    "buildArea",
    "flickeringEffects",
    "fog",
    "goundBlending",
    "groundDecoration",
    "idleAnimations",
    "lightDetail",
    "sceneryShadows",
    "defaultToolkit",
    "particles",
    "removeRoofs",
    "maxScreenSize",
    "skyboxes",
    "characterShadows",
    "textures",
    "water",
    "fixedScreen",
    "customCursor",
    "graphics",
    "cpu",
    "gametipStartStagePreference",
    "safeMode",
    "soundEffectVolume",
    "ambientSoundVolume",
    "voiceOverVolume",
    "musicVolume",
    "soundLevel",
    "monoStereo",
];

const TOOLKIT_SLOT: usize = 19;
const RESERVED_SLOT: usize = 20;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Preferences {
    pub values: [i32; PREFERENCE_VALUES],
}

impl Preferences {
    pub fn new(values: [i32; PREFERENCE_VALUES]) -> Self {
        Self { values }
    }

    pub fn encode(&self) -> [u8; PREFERENCE_BYTES] {
        let mut out = [0u8; PREFERENCE_BYTES];
        out[0] = PREFERENCE_VERSION;
        let mut next = 0;
        for slot in 0..PREFERENCE_BYTES - 1 {
            out[1 + slot] = match slot {
                TOOLKIT_SLOT => TOOLKIT_MODERN_OPENGL,
                RESERVED_SLOT => 0,
                _ => {
                    let value = self.values[next];
                    next += 1;
                    value as u8
                }
            };
        }
        debug_assert_eq!(next, PREFERENCE_VALUES);
        out
    }
}

pub fn send_preferences_body(preferences: &Preferences) -> Vec<u8> {
    preferences.encode().to_vec()
}
