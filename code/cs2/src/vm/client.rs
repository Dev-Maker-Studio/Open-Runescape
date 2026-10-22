use super::{pop, push, Vm};
use crate::{
    equals_ignore_case, AreaSound, AreaSoundKind, ClientState, HostError, Opcode, StackKind,
    VarHost, VmErrorKind,
};
use std::sync::Arc;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn null_pointer() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

fn flag(value: bool) -> i32 {
    i32::from(value)
}

fn from_menu_level(level: i32) -> i32 {
    match level {
        2 => 3,
        1 => 2,
        _ => 0,
    }
}

fn menu_level(water: i32) -> i32 {
    if water == 3 {
        2
    } else if water >= 2 {
        1
    } else {
        0
    }
}

fn adjusted_volume(volume: i32, music_volume: i32) -> i32 {
    volume.wrapping_mul(music_volume) >> 8
}

fn play_song(client: &mut ClientState, archive: i32, volume: i32) -> Result<(), VmErrorKind> {
    let adjusted = adjusted_volume(volume, client.preferences.music_volume);
    let jingle = client.sound_jingle_playing;
    if archive == -1 && !jingle {
        return Err(null_pointer());
    }
    if archive != -1 && adjusted != 0 && !jingle {
        let transition = archive != client.current_music_archive || client.song_load_state == 0;
        if transition {
            return Err(null_pointer());
        }
    }
    client.current_music_archive = archive;
    Ok(())
}

fn play_jingle(client: &mut ClientState, id: i32, volume: i32) -> Result<(), VmErrorKind> {
    let adjusted = adjusted_volume(volume, client.preferences.music_volume);
    if adjusted != 0 && id != -1 {
        return Err(null_pointer());
    }
    Ok(())
}

fn music_volume_change(
    client: &mut ClientState,
    volume: i32,
    current: i32,
    play: bool,
    discard: bool,
) -> Result<(), VmErrorKind> {
    if play {
        return Err(null_pointer());
    }
    if discard {
        return Err(null_pointer());
    }
    let _ = (volume, current);
    if client.song_load_state != 0 {
        client.sound_level_static = volume;
        Ok(())
    } else {
        Err(null_pointer())
    }
}

impl Vm {
    pub(super) fn execute_client(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let client = host.client().map_err(&unavailable)?;
        if Self::is_client_sound_opcode(opcode) {
            return self.execute_client_sound(opcode, client);
        }
        if Self::is_client_detail_opcode(opcode) {
            return self.execute_client_detail(opcode, client);
        }
        self.execute_client_misc(opcode, client)
    }

    pub(super) fn is_client_opcode(opcode: Opcode) -> bool {
        Self::is_client_sound_opcode(opcode)
            || Self::is_client_detail_opcode(opcode)
            || Self::is_client_misc_opcode(opcode)
    }

    fn is_client_sound_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            SOUND_SYNTH
                | SOUND_SONG
                | SOUND_JINGLE
                | SOUND_SYNTH_VOLUME
                | SOUND_SONG_VOLUME
                | SOUND_JINGLE_VOLUME
                | SOUND_VORBIS_VOLUME
                | SOUND_SPEECH_VOLUME
                | SOUND_SYNTH_RATE
                | SOUND_VORBIS_RATE
                | SOUND_VORBIS_PRELOAD
                | SOUND_VORBIS_STOP
                | SOUND_VORBIS_PLAY
        )
    }

    fn is_client_detail_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            DETAILGET_SAFEMODE
                | DETAILGET_CHOSESAFEMODE
                | DETAILGET_CANCHOOSESAFEMODE
                | DETAIL_SOUNDVOL
                | DETAIL_BGSOUNDVOL
                | DETAIL_SPEECHVOL
                | DETAIL_MUSICVOL
                | DETAIL_LOGINVOL
                | DETAIL_IDLEANIMS
                | DETAIL_IDLEANIMS_MANY
                | DETAIL_LOADINGSCREENTYPE
                | DETAIL_SKYDETAIL
                | DETAIL_REMOVEROOFS_OPTION
                | DETAIL_REMOVEROOFS_OPTION_OVERRIDE
                | DETAIL_WATERDETAIL_HIGH
                | DETAIL_TOOLKIT
                | DETAIL_TOOLKIT_DEFAULT
                | DETAIL_BLOOM
                | DETAIL_PARTICLES
                | DETAIL_BRIGHTNESS
                | DETAIL_CUSTOMCURSORS
                | DETAIL_SPOTSHADOWS_ON
                | DETAIL_TEXTURING
                | DETAIL_MAXSCREENSIZE
                | DETAIL_HARDSHADOWS
                | DETAIL_BUILDAREA
                | DETAIL_GROUNDBLENDING
                | DETAIL_CPUUSAGE
                | DETAIL_FLICKERING_ON
                | DETAIL_FOG_ON
                | DETAIL_GROUNDDECOR_ON
                | DETAIL_ANTIALIASING_DEFAULT
                | DETAIL_ANTIALIASING
                | DETAIL_STEREO
                | DETAIL_LIGHTDETAIL_HIGH
                | DETAILGET_BRIGHTNESS
                | DETAILGET_REMOVEROOFS_OPTION
                | DETAILGET_GROUNDDECOR_ON
                | DETAILGET_IDLEANIMS_MANY
                | DETAILGET_FLICKERING_ON
                | DETAILGET_SPOTSHADOWS_ON
                | DETAILGET_HARDSHADOWS
                | DETAILGET_LIGHTDETAIL_HIGH
                | DETAILGET_WATERDETAIL_HIGH
                | DETAILGET_FOG_ON
                | DETAILGET_ANTIALIASING
                | DETAILGET_STEREO
                | DETAILGET_SOUNDVOL
                | DETAILGET_MUSICVOL
                | DETAILGET_BGSOUNDVOL
                | DETAILGET_PARTICLES
                | DETAILGET_ANTIALIASING_DEFAULT
                | DETAILGET_BUILDAREA
                | DETAILGET_BLOOM
                | DETAILGET_CUSTOMCURSORS
                | DETAILGET_IDLEANIMS
                | DETAILGET_GROUNDBLENDING
                | DETAILGET_TOOLKIT
                | DETAILGET_TOOLKIT_DEFAULT
                | DETAILGET_CPUUSAGE
                | DETAILGET_TEXTURING
                | DETAILGET_MAXSCREENSIZE
                | DETAILGET_SPEECHVOL
                | DETAILGET_LOGINVOL
                | DETAILGET_LOADINGSCREENTYPE
                | DETAILGET_DEFAULTTOOLKIT
                | DETAILGET_SKYDETAIL
                | DETAILCANMOD_GROUNDDECOR
                | DETAILCANMOD_CHARSHADOWS
                | DETAILCANMOD_SPOTSHADOWS
                | DETAILCANMOD_WATERDETAIL
                | DETAILCANMOD_ANTIALIASING
                | DETAILCANMOD_PARTICLES
                | DETAILCANMOD_BUILDAREA
                | DETAILCANMOD_BLOOM
                | DETAILCANMOD_GROUNDBLENDING
                | DETAILCANMOD_TEXTURING
                | DETAILCANMOD_MAXSCREENSIZE
                | DETAILCANMOD_FOG
                | DETAILCANMOD_DEFAULTTOOLKIT
                | DETAILCANMOD_TOOLKIT_DEFAULT
                | DETAILCANMOD_SKYDETAIL
                | DETAILCANSET_GROUNDDECOR
                | DETAILCANSET_CHARSHADOWS
                | DETAILCANSET_SPOTSHADOWS
                | DETAILCANSET_WATERDETAIL
                | DETAILCANSET_ANTIALIASING
                | DETAILCANSET_PARTICLES
                | DETAILCANSET_BUILDAREA
                | DETAILCANSET_BLOOM
                | DETAILCANSET_GROUNDBLENDING
                | DETAILCANSET_TEXTURING
                | DETAILCANSET_MAXSCREENSIZE
                | DETAILCANSET_FOG
                | DETAILCANSET_DEFAULTTOOLKIT
                | DETAILCANSET_TOOLKIT_DEFAULT
                | DETAILCANSET_SKYDETAIL
        )
    }

    fn is_client_misc_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CLIENTCLOCK
                | MAP_QUICKCHAT
                | AFFILIATE
                | APPLET_HASFOCUS
                | FROMBILLING
                | GET_CURRENTCURSOR
                | GET_MINIMENU_LENGTH
                | GET_MINIMENU_TARGET
                | GET_MOUSEX
                | GET_MOUSEY
                | GET_MOUSEBUTTONS
                | KEYHELD_ALT
                | KEYHELD_CTRL
                | KEYHELD_SHIFT
                | MAP_ISOWNER
                | HAS_SIGNONKEY
                | USERFLOWFLAGS
                | CREATE_UNDER13
                | CREATE_SETUNDER13
                | CREATE_GET_EMAIL
                | LOGIN_HOPTIME
                | LOGIN_QUEUE_POSITION
                | LOGIN_REPLY_WORLD_ALT
                | LOBBY_ENTERGAMEREPLY
                | LOBBY_ENTERLOBBYREPLY
                | LOGIN_DISALLOWTRIGGER
                | LOGIN_DISALLOWRESULT
                | IS_APPLET
                | NOOP
                | OPCOUNT
                | USERDETAIL_QUICKCHAT
                | NOTIFY_ACCOUNTCREATED
                | SETDEFAULTCURSORS
                | SETHARDCODEDOPCURSORS
                | AUTOSETUP_GETLEVEL
        )
    }

    fn execute_client_sound(
        &mut self,
        opcode: Opcode,
        client: &mut ClientState,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let ints = &mut self.ints;
        let queue = |client: &mut ClientState, sound: AreaSound| {
            client.queue_area_sound(sound).map_err(VmErrorKind::Host)
        };
        match opcode {
            SOUND_SYNTH | SOUND_SYNTH_VOLUME | SOUND_SYNTH_RATE | SOUND_VORBIS_VOLUME
            | SOUND_VORBIS_RATE | SOUND_SPEECH_VOLUME => {
                let with_volume = !matches!(opcode, SOUND_SYNTH);
                let with_rate = matches!(opcode, SOUND_SYNTH_RATE | SOUND_VORBIS_RATE);
                let rate = if with_rate { pop(ints, Int)? } else { 256 };
                let volume = if with_volume { pop(ints, Int)? } else { 255 };
                let delay = pop(ints, Int)?;
                let repeats = pop(ints, Int)?;
                let id = pop(ints, Int)?;
                let kind = match opcode {
                    SOUND_VORBIS_VOLUME | SOUND_VORBIS_RATE => AreaSoundKind::Vorbis,
                    SOUND_SPEECH_VOLUME => AreaSoundKind::Speech,
                    _ => AreaSoundKind::Synth,
                };
                queue(
                    client,
                    AreaSound {
                        kind,
                        id,
                        repeats,
                        delay,
                        volume,
                        position: 0,
                        rate,
                    },
                )
            }
            SOUND_SONG => {
                let archive = pop(ints, Int)?;
                play_song(client, archive, 255)
            }
            SOUND_SONG_VOLUME => {
                let _fade = pop(ints, Int)?;
                let volume = pop(ints, Int)?;
                let archive = pop(ints, Int)?;
                play_song(client, archive, volume)
            }
            SOUND_JINGLE => {
                let _unused = pop(ints, Int)?;
                let id = pop(ints, Int)?;
                play_jingle(client, id, 255)
            }
            SOUND_JINGLE_VOLUME => {
                let volume = pop(ints, Int)?;
                let _unused = pop(ints, Int)?;
                let id = pop(ints, Int)?;
                play_jingle(client, id, volume)
            }
            SOUND_VORBIS_PRELOAD => {
                let _priority = pop(ints, Int)?;
                let file = pop(ints, Int)?;
                if file == -1 {
                    Ok(())
                } else {
                    Err(null_pointer())
                }
            }
            _ => {
                pop(ints, Int)?;
                Ok(())
            }
        }
    }

    #[allow(clippy::collapsible_match)]
    fn execute_client_detail(
        &mut self,
        opcode: Opcode,
        client: &mut ClientState,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let ints = &mut self.ints;
        match opcode {
            DETAIL_SOUNDVOL | DETAIL_BGSOUNDVOL | DETAIL_SPEECHVOL => {
                let value = pop(ints, Int)?;
                match opcode {
                    DETAIL_SOUNDVOL => client.preferences.sound_effect_volume = value,
                    DETAIL_BGSOUNDVOL => client.preferences.ambient_sound_volume = value,
                    _ => client.preferences.voice_over_volume = value,
                }
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_MUSICVOL => {
                let volume = pop(ints, Int)?;
                let current = client.preferences.music_volume;
                if volume != current {
                    if client.rendering_scene {
                        let play = current == 0 && client.current_music_archive != -1;
                        let discard = !play && volume == 0;
                        music_volume_change(client, volume, current, play, discard)?;
                    }
                    client.preferences.music_volume = volume;
                    client.preferences_saved_on_server = false;
                }
                Ok(())
            }
            DETAIL_LOGINVOL => {
                let volume = pop(ints, Int)?;
                let current = client.preferences.sound_level;
                if current != volume && client.titlescreen_music_archive == client.current_music_archive
                {
                    if !client.rendering_scene {
                        let play = current == 0;
                        let discard = !play && volume == 0;
                        music_volume_change(client, volume, current, play, discard)?;
                    }
                    client.preferences.sound_level = volume;
                    client.preferences_saved_on_server = false;
                }
                Ok(())
            }
            DETAIL_IDLEANIMS => {
                client.preferences.idle_animations = pop(ints, Int)?;
                Ok(())
            }
            DETAIL_IDLEANIMS_MANY => {
                client.preferences.idle_animations = pop(ints, Int)?;
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_LOADINGSCREENTYPE => {
                let mut value = pop(ints, Int)?;
                if !(0..=255).contains(&value) {
                    value = 0;
                }
                if value != client.preferences.gametip_start_stage {
                    client.preferences.gametip_start_stage = value;
                    client.preferences_saved_on_server = false;
                }
                Ok(())
            }
            DETAIL_SKYDETAIL => {
                let value = pop(ints, Int)?;
                if value != client.preferences.skyboxes {
                    client.preferences.skyboxes = value;
                    client.preferences_saved_on_server = false;
                }
                Ok(())
            }
            DETAIL_REMOVEROOFS_OPTION => {
                let enabled = pop(ints, Int)? == 1;
                let level = if enabled { 2 } else { 1 };
                client.preferences.remove_roofs = level;
                client.preferences.remove_roofs_option_override = level;
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_REMOVEROOFS_OPTION_OVERRIDE => {
                let value = pop(ints, Int)?;
                client.preferences.remove_roofs_option_override = if value == 1 {
                    0
                } else {
                    client.preferences.remove_roofs
                };
                Ok(())
            }
            DETAIL_WATERDETAIL_HIGH => {
                let value = pop(ints, Int)?;
                let was_high = client.preferences.water >= 2;
                client.preferences.water = from_menu_level(value);
                let high = client.preferences.water >= 2;
                if was_high != high && client.rendering_scene {
                    return Err(null_pointer());
                }
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_TOOLKIT => {
                pop(ints, Int)?;
                Ok(())
            }
            DETAIL_TOOLKIT_DEFAULT => {
                let keep = pop(ints, Int)?;
                let _toolkit = pop(ints, Int)?;
                if keep != 1 {
                    client.preferences.graphics = 0;
                }
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_BLOOM => {
                pop(ints, Int)?;
                Err(null_pointer())
            }
            DETAIL_PARTICLES => {
                let value = pop(ints, Int)?;
                client.preferences.particles = value;
                client.particle_details = if (0..=2).contains(&value) { value } else { 0 };
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_CUSTOMCURSORS => {
                let value = pop(ints, Int)?;
                client.preferences.custom_cursor = flag(value != 0);
                Ok(())
            }
            DETAIL_SPOTSHADOWS_ON => {
                let value = pop(ints, Int)?;
                client.preferences.character_shadows = flag(value == 1);
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_MAXSCREENSIZE => {
                let value = pop(ints, Int)?;
                client.preferences.max_screen_size = value;
                client.needs_resize = true;
                Ok(())
            }
            DETAIL_BUILDAREA => {
                let value = pop(ints, Int)?;
                client.preferences.map_size = value;
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_CPUUSAGE => {
                client.preferences.cpu = pop(ints, Int)?;
                Ok(())
            }
            DETAIL_FLICKERING_ON => {
                let value = pop(ints, Int)?;
                client.preferences.flickering_effects = flag(value == 1);
                client.preferences_saved_on_server = false;
                Ok(())
            }
            DETAIL_ANTIALIASING_DEFAULT => {
                client.preferences.anti_aliasing = pop(ints, Int)?;
                Ok(())
            }
            DETAIL_ANTIALIASING => {
                client.preferences.anti_aliasing_preview = pop(ints, Int)?;
                Err(null_pointer())
            }
            DETAIL_TEXTURING => {
                let value = pop(ints, Int)?;
                client.preferences.textures = flag(value == 1);
                Err(null_pointer())
            }
            DETAIL_STEREO => {
                let value = pop(ints, Int)?;
                client.preferences.mono_stereo = flag(value == 1);
                Err(null_pointer())
            }
            DETAIL_LIGHTDETAIL_HIGH => {
                let value = pop(ints, Int)?;
                client.preferences.light_detail = flag(value == 1);
                Err(null_pointer())
            }
            DETAIL_BRIGHTNESS | DETAIL_HARDSHADOWS | DETAIL_FOG_ON | DETAIL_GROUNDDECOR_ON
            | DETAIL_GROUNDBLENDING => {
                let value = pop(ints, Int)?;
                let preferences = &mut client.preferences;
                match opcode {
                    DETAIL_BRIGHTNESS => preferences.brightness = value,
                    DETAIL_HARDSHADOWS => preferences.scenery_shadows = value,
                    DETAIL_FOG_ON => preferences.fog = flag(value == 1),
                    DETAIL_GROUNDDECOR_ON => preferences.ground_decoration = flag(value == 1),
                    _ => preferences.ground_blending = flag(value != 0),
                }
                if client.rendering_scene {
                    return Err(null_pointer());
                }
                if opcode != DETAIL_GROUNDBLENDING {
                    client.preferences_saved_on_server = false;
                }
                Ok(())
            }
            DETAILGET_SAFEMODE => push(ints, flag(client.forced_safemode), Int),
            DETAILGET_CHOSESAFEMODE => {
                push(ints, flag(client.safemode_forced_during_startup), Int)
            }
            DETAILGET_CANCHOOSESAFEMODE => Err(null_pointer()),
            DETAILGET_TOOLKIT | DETAILGET_TOOLKIT_DEFAULT => {
                push(ints, crate::TOOLKIT_MODERN, Int)
            }
            DETAILGET_PARTICLES => push(ints, client.particle_details, Int),
            DETAILGET_WATERDETAIL_HIGH => push(ints, menu_level(client.preferences.water), Int),
            DETAILGET_REMOVEROOFS_OPTION => {
                push(ints, flag(client.preferences.remove_roofs == 2), Int)
            }
            DETAILGET_BRIGHTNESS => push(ints, client.preferences.brightness, Int),
            DETAILGET_IDLEANIMS | DETAILGET_IDLEANIMS_MANY => {
                push(ints, client.preferences.idle_animations, Int)
            }
            DETAILGET_HARDSHADOWS => push(ints, client.preferences.scenery_shadows, Int),
            DETAILGET_ANTIALIASING => push(ints, client.preferences.anti_aliasing_preview, Int),
            DETAILGET_ANTIALIASING_DEFAULT => push(ints, client.preferences.anti_aliasing, Int),
            DETAILGET_SOUNDVOL => push(ints, client.preferences.sound_effect_volume, Int),
            DETAILGET_MUSICVOL => push(ints, client.preferences.music_volume, Int),
            DETAILGET_BGSOUNDVOL => push(ints, client.preferences.ambient_sound_volume, Int),
            DETAILGET_SPEECHVOL => push(ints, client.preferences.voice_over_volume, Int),
            DETAILGET_LOGINVOL => push(ints, client.preferences.sound_level, Int),
            DETAILGET_BUILDAREA => push(ints, client.preferences.map_size, Int),
            DETAILGET_DEFAULTTOOLKIT => push(ints, client.preferences.default_toolkit, Int),
            DETAILGET_CPUUSAGE => push(ints, client.preferences.cpu, Int),
            DETAILGET_MAXSCREENSIZE => push(ints, client.preferences.max_screen_size, Int),
            DETAILGET_LOADINGSCREENTYPE => {
                push(ints, client.preferences.gametip_start_stage, Int)
            }
            DETAILGET_SKYDETAIL => push(ints, client.preferences.skyboxes, Int),
            DETAILGET_GROUNDDECOR_ON => {
                push(ints, flag(client.preferences.ground_decoration == 1), Int)
            }
            DETAILGET_FLICKERING_ON => {
                push(ints, flag(client.preferences.flickering_effects == 1), Int)
            }
            DETAILGET_SPOTSHADOWS_ON => {
                push(ints, flag(client.preferences.character_shadows == 1), Int)
            }
            DETAILGET_LIGHTDETAIL_HIGH => {
                push(ints, flag(client.preferences.light_detail == 1), Int)
            }
            DETAILGET_FOG_ON => push(ints, flag(client.preferences.fog == 1), Int),
            DETAILGET_STEREO => push(ints, flag(client.preferences.mono_stereo == 1), Int),
            DETAILGET_BLOOM => push(ints, flag(client.preferences.bloom == 1), Int),
            DETAILGET_CUSTOMCURSORS => {
                push(ints, flag(client.preferences.custom_cursor == 1), Int)
            }
            DETAILGET_GROUNDBLENDING => {
                push(ints, flag(client.preferences.ground_blending == 1), Int)
            }
            DETAILGET_TEXTURING => push(ints, flag(client.preferences.textures == 1), Int),
            DETAILCANMOD_GROUNDDECOR | DETAILCANMOD_GROUNDBLENDING | DETAILCANMOD_TEXTURING => {
                push(ints, flag(client.game_runescape), Int)
            }
            DETAILCANMOD_SPOTSHADOWS => push(ints, flag(client.preferences.textures != 0), Int),
            DETAILCANMOD_PARTICLES | DETAILCANMOD_BUILDAREA => {
                push(ints, flag(client.max_memory >= 245), Int)
            }
            DETAILCANMOD_CHARSHADOWS
            | DETAILCANMOD_WATERDETAIL
            | DETAILCANMOD_MAXSCREENSIZE
            | DETAILCANMOD_FOG
            | DETAILCANMOD_DEFAULTTOOLKIT
            | DETAILCANMOD_SKYDETAIL => push(ints, 1, Int),
            DETAILCANMOD_TOOLKIT_DEFAULT => push(ints, 0, Int),
            DETAILCANMOD_ANTIALIASING | DETAILCANMOD_BLOOM => Err(null_pointer()),
            DETAILCANSET_ANTIALIASING | DETAILCANSET_BLOOM => {
                pop(ints, Int)?;
                Err(null_pointer())
            }
            _ => {
                let value = pop(ints, Int)?;
                let preferences = &client.preferences;
                let result = match opcode {
                    DETAILCANSET_GROUNDDECOR => {
                        if client.game_runescape {
                            1
                        } else {
                            3
                        }
                    }
                    DETAILCANSET_SPOTSHADOWS => {
                        if preferences.textures == 0 {
                            3
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_WATERDETAIL => {
                        if from_menu_level(value) != 0 && preferences.ground_blending != 1 {
                            2
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_PARTICLES => {
                        if client.max_memory < 245 {
                            3
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_BUILDAREA => {
                        if client.max_memory < 245 || (value == 3 && client.max_memory < 500) {
                            3
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_GROUNDBLENDING => {
                        if client.game_runescape {
                            if value == 0
                                && (preferences.fog == 1
                                    || preferences.textures == 1
                                    || preferences.water > 0)
                            {
                                2
                            } else {
                                1
                            }
                        } else {
                            3
                        }
                    }
                    DETAILCANSET_TEXTURING => {
                        if client.game_runescape {
                            if value != 0 && preferences.ground_blending != 1 {
                                2
                            } else {
                                1
                            }
                        } else {
                            3
                        }
                    }
                    DETAILCANSET_FOG => {
                        if value != 0 && preferences.default_toolkit == 2 {
                            3
                        } else if value != 0 && preferences.ground_blending != 1 {
                            2
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_DEFAULTTOOLKIT => {
                        if value == 2 {
                            2
                        } else {
                            1
                        }
                    }
                    DETAILCANSET_TOOLKIT_DEFAULT => {
                        if value == crate::TOOLKIT_MODERN {
                            2
                        } else {
                            3
                        }
                    }
                    _ => 1,
                };
                push(ints, result, Int)
            }
        }
    }

    fn execute_client_misc(
        &mut self,
        opcode: Opcode,
        client: &mut ClientState,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let ints = &mut self.ints;
        match opcode {
            CLIENTCLOCK => push(ints, client.cycles, Int),
            AFFILIATE => push(ints, client.affiliate, Int),
            FROMBILLING => push(ints, client.billing_flag, Int),
            APPLET_HASFOCUS => push(ints, flag(client.applet_has_focus), Int),
            IS_APPLET => push(ints, flag(client.applet), Int),
            MAP_QUICKCHAT => push(ints, flag(client.quickchat_only), Int),
            GET_CURRENTCURSOR => push(ints, client.current_cursor, Int),
            GET_MINIMENU_LENGTH => {
                push(ints, client.minimenu_entry_count, Int)?;
                push(ints, client.minimenu_coloured_entry_count, Int)
            }
            GET_MINIMENU_TARGET => {
                push(ints, flag(client.interface_selection_active), Int)?;
                let target = client
                    .use_on_target
                    .clone()
                    .unwrap_or_else(|| Arc::from(""));
                let kind = client.use_on_type.clone().unwrap_or_else(|| Arc::from(""));
                push(&mut self.strings, Some(target), Text)?;
                push(&mut self.strings, Some(kind), Text)
            }
            GET_MOUSEX => push(ints, client.mouse.ok_or_else(null_pointer)?.x, Int),
            GET_MOUSEY => push(ints, client.mouse.ok_or_else(null_pointer)?.y, Int),
            GET_MOUSEBUTTONS => {
                let mouse = client.mouse.ok_or_else(null_pointer)?;
                push(ints, flag(mouse.left), Int)?;
                push(ints, flag(mouse.right), Int)?;
                push(ints, flag(mouse.middle), Int)
            }
            KEYHELD_ALT => push(ints, flag(client.keys.ok_or_else(null_pointer)?.alt), Int),
            KEYHELD_CTRL => push(ints, flag(client.keys.ok_or_else(null_pointer)?.ctrl), Int),
            KEYHELD_SHIFT => push(ints, flag(client.keys.ok_or_else(null_pointer)?.shift), Int),
            MAP_ISOWNER => {
                let name = pop(&mut self.strings, Text)?;
                let owner = client.map_owner_identifier.as_deref();
                let matched = match (owner, name.as_deref()) {
                    (Some(owner), Some(name)) => equals_ignore_case(owner, name),
                    _ => false,
                };
                push(ints, flag(matched), Int)
            }
            HAS_SIGNONKEY => push(ints, flag(client.sign_on_key.is_some()), Int),
            USERFLOWFLAGS => {
                push(ints, (client.machine_uid >> 32) as i32, Int)?;
                push(ints, client.machine_uid as i32, Int)
            }
            CREATE_UNDER13 => push(ints, flag(client.just_created_account), Int),
            CREATE_SETUNDER13 => {
                client.just_created_account = true;
                Ok(())
            }
            CREATE_GET_EMAIL => {
                let email = client
                    .custom_string_param
                    .clone()
                    .unwrap_or_else(|| Arc::from(""));
                push(&mut self.strings, Some(email), Text)
            }
            LOGIN_HOPTIME => push(ints, client.login_queue_position, Int),
            LOGIN_QUEUE_POSITION => push(ints, client.login_handshake_code, Int),
            LOGIN_REPLY_WORLD_ALT | LOBBY_ENTERGAMEREPLY => {
                push(ints, client.world_login_response, Int)
            }
            LOBBY_ENTERLOBBYREPLY => push(ints, client.lobby_login_response, Int),
            LOGIN_DISALLOWTRIGGER => push(ints, client.login_response_extra_info, Int),
            LOGIN_DISALLOWRESULT => push(ints, client.login_response_detail, Int),
            NOOP | NOTIFY_ACCOUNTCREATED => Ok(()),
            OPCOUNT => push(ints, client.instruction_count, Int),
            USERDETAIL_QUICKCHAT => push(
                ints,
                flag(client.userdetail_quickchat && !client.verified_email_address),
                Int,
            ),
            SETDEFAULTCURSORS => {
                let attack = pop(ints, Int)?;
                let default = pop(ints, Int)?;
                client.default_cursor_id = default;
                client.default_attack_cursor = attack;
                Ok(())
            }
            SETHARDCODEDOPCURSORS => {
                pop(ints, Int)?;
                pop(ints, Int)?;
                Ok(())
            }
            AUTOSETUP_GETLEVEL => push(ints, client.preferences.graphics, Int),
            _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
    }
}
