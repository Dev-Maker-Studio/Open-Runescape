use super::world::host_error;
use super::{push, Vm};
use crate::platform::{mini_menu_click_type, MecDefinition, MiniMenuEntryState};
use crate::{MessageBoxState, EnumValue, HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn null_pointer() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

fn flag(value: bool) -> i32 {
    i32::from(value)
}

impl Vm {
    fn push_platform_text(&mut self, text: Arc<str>) -> Result<(), VmErrorKind> {
        push(&mut self.strings, Some(text), StackKind::String)
    }

    fn push_platform_menu_entry(
        &mut self,
        open: bool,
        entry: Option<&MiniMenuEntryState>,
    ) -> Result<(), VmErrorKind> {
        push(
            &mut self.ints,
            mini_menu_click_type(open, entry.map(|entry| entry.click_type)),
            StackKind::Int,
        )?;
        let visible = if open { None } else { entry };
        let action = visible.map_or_else(|| Arc::from(""), |entry| Arc::clone(&entry.action));
        let name = visible.map_or_else(
            || Arc::from(""),
            |entry| match &entry.target_name {
                Some(target) if entry.colored_name.is_empty() && !target.is_empty() => {
                    Arc::clone(target)
                }
                _ => Arc::clone(&entry.colored_name),
            },
        );
        let quests = visible.map_or_else(|| Arc::from(""), |entry| Arc::clone(&entry.quest_icons));
        self.push_platform_text(action)?;
        self.push_platform_text(name)?;
        self.push_platform_text(quests)
    }

    pub(super) fn execute_platform(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let definition = if opcode == MEC_PARAM {
            match self.ints.last().copied() {
                Some(param) => Some(host.param_definition(param).map_err(&unavailable)?),
                None => None,
            }
        } else {
            None
        };
        let platform = &mut host.world_state().map_err(&unavailable)?.platform;
        match opcode {
            MAP_LANG => {
                let language = platform.language.ok_or_else(null_pointer)?;
                push(&mut self.ints, language, Int)?;
            }
            GET_ACTIVE_MINIMENU_ENTRY => {
                let entry = platform.active_entry.clone();
                self.push_platform_menu_entry(platform.mini_menu_open, entry.as_ref())?;
            }
            GET_SECOND_MINIMENU_ENTRY => {
                let entry = platform.secondary_entry.clone();
                self.push_platform_menu_entry(platform.mini_menu_open, entry.as_ref())?;
            }
            GETWINDOWMODE => push(&mut self.ints, platform.window_mode(), Int)?,
            GETDEFAULTWINDOWMODE => push(&mut self.ints, platform.fixed_screen, Int)?,
            GETHOSTNAME => {
                if !platform.hostname_identifier {
                    return Err(null_pointer());
                }
                let name = platform.hostname.clone().unwrap_or_else(|| Arc::from(""));
                self.push_platform_text(name)?;
            }
            GETCLIPBOARD => {
                let text = platform.clipboard.clone().unwrap_or_else(|| Arc::from(""));
                self.push_platform_text(text)?;
            }
            IS_GAMESCREEN_STATE => {
                push(&mut self.ints, flag(platform.rendering_scene()), Int)?;
            }
            GETPREFERENCEFILE => {
                let text = platform
                    .preference_file
                    .clone()
                    .unwrap_or_else(|| Arc::from("null"));
                self.push_platform_text(text)?;
            }
            GETDIRECTORYPICKERRESULT => {
                let finished = platform
                    .directory_picker
                    .as_ref()
                    .is_some_and(|picker| picker.finished);
                let mut path: Arc<str> = Arc::from("");
                if finished {
                    if let Some(picker) = platform.directory_picker.take() {
                        if let Some(picked) = picker.picked {
                            path = picked;
                        }
                    }
                }
                push(&mut self.ints, flag(finished), Int)?;
                self.push_platform_text(path)?;
            }
            MEC_TEXT | MEC_GRAPHIC | MEC_TEXTSIZE | MEC_CATEGORY => {
                let [id] = self.pop_arguments()?;
                let definition = platform.mec.get(&id).cloned().unwrap_or_default();
                match opcode {
                    MEC_TEXT => {
                        let name = definition.area_name.unwrap_or_else(|| Arc::from(""));
                        self.push_platform_text(name)?;
                    }
                    MEC_GRAPHIC => push(&mut self.ints, definition.default_icon_archive, Int)?,
                    MEC_TEXTSIZE => push(&mut self.ints, definition.base_text_zoom, Int)?,
                    _ => push(&mut self.ints, definition.category_id, Int)?,
                }
            }
            MEC_PARAM => {
                let [id, param] = self.pop_arguments()?;
                let parameter = definition.flatten().unwrap_or_default();
                let mec: MecDefinition = platform.mec.get(&id).cloned().unwrap_or_default();
                let entry = mec.params.get(&param);
                if parameter.is_string {
                    let text = match entry {
                        Some(EnumValue::Text(text)) => Arc::clone(text),
                        _ => Arc::clone(&parameter.type_name),
                    };
                    self.push_platform_text(text)?;
                } else {
                    let number = match entry {
                        Some(EnumValue::Int(number)) => *number,
                        _ => parameter.default_int,
                    };
                    push(&mut self.ints, number, Int)?;
                }
            }
            SETWINDOWMODE => {
                let [mode] = self.pop_arguments()?;
                if (1..=2).contains(&mode) {
                    platform.window_mode_requests.push(mode);
                }
            }
            SETDEFAULTWINDOWMODE => {
                let [mode] = self.pop_arguments()?;
                if (1..=2).contains(&mode) {
                    platform.fixed_screen = mode;
                    platform.full_screen_preference = mode;
                    platform.preference_saves = platform.preference_saves.wrapping_add(1);
                }
            }
            JAVA_VERSION_SUPPORTED => {
                let supported = platform.java_build >= 6
                    && !(platform.java_build == 6 && platform.java_update < 10);
                push(&mut self.ints, flag(supported), Int)?;
            }
            PROFILE_CPU => push(&mut self.ints, platform.cpu_profile, Int)?,
            SETUP_MESSAGEBOX => {
                let v: [i32; 11] = self.pop_arguments()?;
                for index in [v[0], v[1]] {
                    if !(0..3).contains(&index) {
                        return Err(VmErrorKind::ArrayIndex(index));
                    }
                }
                platform.message_box = Some(MessageBoxState {
                    anchor_x: v[0],
                    anchor_y: v[1],
                    padding_left: v[2],
                    padding_top: v[3],
                    minimal_text_width: v[4],
                    minimal_text_height: v[5],
                    outline_corner_sprite: v[6],
                    outline_straight_sprite: v[7],
                    filled_background_sprite: v[8],
                    text_color: v[9],
                    box_id: v[10],
                });
            }
            VIDEOADVERT_ISSUPPORTED => {
                push(&mut self.ints, flag(platform.jagtheora_loaded), Int)?;
            }
            VIDEO_ADVERT_PLAY => {
                let [_age] = self.pop_arguments()?;
                push(&mut self.ints, 0, Int)?;
            }
            VIDEO_ADVERT_FORCE_REMOVE | VIDEO_ADVERT_ALLOW_SKIP => {}
            VIDEO_ADVERT_HAS_FINISHED => push(&mut self.ints, 1, Int)?,
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_platform_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            MAP_LANG
                | GET_ACTIVE_MINIMENU_ENTRY
                | GET_SECOND_MINIMENU_ENTRY
                | GETWINDOWMODE
                | GETDEFAULTWINDOWMODE
                | GETHOSTNAME
                | GETCLIPBOARD
                | IS_GAMESCREEN_STATE
                | GETPREFERENCEFILE
                | GETDIRECTORYPICKERRESULT
                | MEC_TEXT
                | MEC_GRAPHIC
                | MEC_TEXTSIZE
                | MEC_CATEGORY
                | MEC_PARAM
                | SETWINDOWMODE
                | SETDEFAULTWINDOWMODE
                | JAVA_VERSION_SUPPORTED
                | PROFILE_CPU
                | SETUP_MESSAGEBOX
                | VIDEOADVERT_ISSUPPORTED
                | VIDEO_ADVERT_PLAY
                | VIDEO_ADVERT_FORCE_REMOVE
                | VIDEO_ADVERT_ALLOW_SKIP
                | VIDEO_ADVERT_HAS_FINISHED
        )
    }
}
