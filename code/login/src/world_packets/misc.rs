use super::{Error, none_if_ffff};
use openrs_protocol::payload::PayloadReader;
use openrs_protocol::registry::ServerPacket;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Misc {
    DevConsoleCommand(String),
    DrawOrder(bool),
    Cursor {
        text: Option<String>,
        id: Option<i32>,
    },
    CamForceAngle {
        angle_y: u16,
        angle_x: u16,
    },
    CamLookAt {
        view_x: u8,
        view_y: u32,
        view_z: u8,
        constant_speed: u8,
        variable_speed: u8,
    },
    CamMoveTo {
        local_y: u8,
        local_x: u8,
        constant_speed: u8,
        move_z: u32,
        variable_speed: u8,
    },
    CamShake {
        key: u8,
        flags: u16,
        shake_type: u8,
        jitter: u8,
        frequency: u8,
    },
    CameraTarget {
        clear: bool,
        x: i32,
        y: i32,
    },
    SetTarget(i16),
    ReduceAttackPriority(bool),
    ShowFaceHere(bool),
    VorbisSound {
        id: i32,
        repeat: u8,
        delay: u16,
        volume: u8,
        sample_rate: u16,
    },
    VorbisPreloadSound {
        key: u8,
        flags: u16,
    },
    MusicTrack {
        key: u8,
        flags: u8,
        song: i32,
    },
    MusicEffect {
        key: u32,
        flags: u8,
        jingle: i32,
    },
    SongVolume {
        volume: i8,
        flags: u8,
    },
    SetVorbisSound(u16),
    StopVorbisStream(u16),
    PreloadSong(i32),
    SoundSynth {
        id: i32,
        flags: u8,
        delay: u16,
        volume: u8,
        sample_rate: u16,
    },
    VorbisSpeech {
        id: i32,
        kind: u8,
        delay: u16,
        volume: u8,
    },
    PlaySongAtPosition {
        near: u8,
        volume: u8,
        far: u8,
        archive: i32,
        plane: u32,
        x: u32,
        y: u32,
    },
    SpotAnimSpecific {
        index: u8,
        graphic: i32,
        settings: u8,
        speed: u16,
        target: u32,
        height: u16,
    },
    GameMessage {
        kind: u16,
        effect_flags: i32,
        flags: u8,
        name: Option<String>,
        name_formatted: Option<String>,
        message: String,
    },
    OpenUrl {
        url: String,
        fallback: Option<String>,
    },
    QuickHopWorlds {
        port: u16,
        host: String,
        reconnecting: bool,
    },
    RequestFps {
        key: i32,
        flags: i32,
    },
    RebootTimer(u16),
    UpdateDob {
        value: i32,
        flag: bool,
    },
    JcoinsUpdate(u32),
    LoyaltyUpdate {
        balance: i32,
        flag: bool,
    },
    IdentifyHostName(i32),
    ApplyDebug(u16),
    Uid192([u8; 28]),
    ByteReply(u8),
    SignedByte(i8),
    ChatFilter {
        trade: u8,
        public: u8,
    },
    RawString(String),
    WindowCall(Vec<u8>),
    AnimateNpc {
        npc: u16,
        values: [u32; 4],
        speed: u8,
    },
    MinimapState(u8),
    ObjAnimSpecific {
        tile: u32,
        animation: u32,
        shape: u8,
        rotation: u8,
    },
    ProjAnimSpecific {
        end_height: u8,
        source: i16,
        end_time: u16,
        lock_on: i16,
        flags: u8,
        y: u16,
        slope: u16,
        start_time: u16,
        spot_anim: u16,
        angle: i32,
        x_dist: i8,
        y_dist: i8,
        x: u16,
        start_height: u8,
    },
}

pub fn decode(packet: ServerPacket, size: usize, r: &mut PayloadReader<'_>) -> Result<Misc, Error> {
    use ServerPacket as P;
    let decoded = match packet {
        P::PROCESS_DEV_CONSOLE_COMMAND => Misc::DevConsoleCommand(r.read_cp1252()?),
        P::SET_DRAW_ORDER => Misc::DrawOrder(r.read_u8_c()? == 1),
        P::SET_CURSOR => {
            let text = if size > 2 {
                Some(r.read_cp1252()?)
            } else {
                None
            };
            let id = if size > 0 {
                Some(none_if_ffff(r.read_u16_be()?))
            } else {
                None
            };
            Misc::Cursor { text, id }
        }
        P::CAM_FORCEANGLE => {
            let angle_y = r.read_u16_be_a()?;
            let angle_x = r.read_u16_le_a()?;
            Misc::CamForceAngle { angle_y, angle_x }
        }
        P::CAM_LOOKAT => {
            let view_x = r.read_u8()?;
            let view_y = u32::from(r.read_u16_be_a()?) << 2;
            let view_z = r.read_u8_a()?;
            let constant_speed = r.read_u8_c()?;
            let variable_speed = r.read_u8_s()?;
            Misc::CamLookAt {
                view_x,
                view_y,
                view_z,
                constant_speed,
                variable_speed,
            }
        }
        P::CAM_MOVETO => {
            let local_y = r.read_u8()?;
            let local_x = r.read_u8()?;
            let constant_speed = r.read_u8_s()?;
            let move_z = u32::from(r.read_u16_le()?) << 2;
            let variable_speed = r.read_u8_a()?;
            Misc::CamMoveTo {
                local_y,
                local_x,
                constant_speed,
                move_z,
                variable_speed,
            }
        }
        P::CAM_SHAKE => {
            let key = r.read_u8_s()?;
            let flags = r.read_u16_be()?;
            let shake_type = r.read_u8_c()?;
            let jitter = r.read_u8_c()?;
            let frequency = r.read_u8_s()?;
            Misc::CamShake {
                key,
                flags,
                shake_type,
                jitter,
                frequency,
            }
        }
        P::SET_CAMERA_TARGET => {
            let key = r.read_i32_le()?;
            if key == -1 {
                Misc::CameraTarget {
                    clear: true,
                    x: 0,
                    y: 0,
                }
            } else {
                Misc::CameraTarget {
                    clear: false,
                    x: (key >> 14) & 0x3fff,
                    y: key & 0x3fff,
                }
            }
        }
        P::SET_TARGET => Misc::SetTarget(r.read_i16_le()?),
        P::REDUCE_ATTACK_PRIORITY => Misc::ReduceAttackPriority(r.read_u8_a()? == 1),
        P::SHOW_FACE_HERE => Misc::ShowFaceHere(r.read_u8()? == 1),
        P::VORBIS_SOUND => {
            let id = none_if_ffff(r.read_u16_be()?);
            let repeat = r.read_u8()?;
            let delay = r.read_u16_be()?;
            let volume = r.read_u8()?;
            let sample_rate = r.read_u16_be()?;
            Misc::VorbisSound {
                id,
                repeat,
                delay,
                volume,
                sample_rate,
            }
        }
        P::VORBIS_PRELOAD_SOUND => {
            let key = r.read_u8()?;
            let flags = r.read_u16_be_a()?;
            Misc::VorbisPreloadSound { key, flags }
        }
        P::MUSIC_TRACK => {
            let key = r.read_u8_a()?;
            let flags = r.read_u8_a()?;
            let song = none_if_ffff(r.read_u16_be_a()?);
            Misc::MusicTrack { key, flags, song }
        }
        P::MUSIC_EFFECT => {
            let b0 = u32::from(r.read_u8()?);
            let b1 = u32::from(r.read_u8()?);
            let b2 = u32::from(r.read_u8()?);
            let key = (b0 << 16) | b1 | (b2 << 8);
            let flags = r.read_u8_s()?;
            let jingle = none_if_ffff(r.read_u16_le()?);
            Misc::MusicEffect { key, flags, jingle }
        }
        P::SET_SONG_VOLUME => {
            let volume = r.read_i8()?;
            let flags = r.read_u8_c()?;
            Misc::SongVolume { volume, flags }
        }
        P::SET_VORBIS_SOUND => Misc::SetVorbisSound(r.read_u16_be()?),
        P::STOP_VORBIS_STREAM => Misc::StopVorbisStream(r.read_u16_le_a()?),
        P::PRELOAD_SONG => Misc::PreloadSong(none_if_ffff(r.read_u16_le_a()?)),
        P::SOUND_SYNTH => {
            let id = none_if_ffff(r.read_u16_be()?);
            let flags = r.read_u8()?;
            let delay = r.read_u16_be()?;
            let volume = r.read_u8()?;
            let sample_rate = r.read_u16_be()?;
            Misc::SoundSynth {
                id,
                flags,
                delay,
                volume,
                sample_rate,
            }
        }
        P::VORBIS_SPEECH_SOUND => {
            let id = none_if_ffff(r.read_u16_be()?);
            let kind = r.read_u8()?;
            let delay = r.read_u16_be()?;
            let volume = r.read_u8()?;
            Misc::VorbisSpeech {
                id,
                kind,
                delay,
                volume,
            }
        }
        P::PLAY_SONG_AT_POSITION => {
            let near = r.read_u8_s()?;
            let volume = r.read_u8()?;
            let far = r.read_u8_c()?;
            let archive = r.read_i32_be()?;
            let hash = r.read_u32_middle()?;
            Misc::PlaySongAtPosition {
                near,
                volume,
                far,
                archive,
                plane: hash >> 28,
                x: (hash >> 14) & 0x3fff,
                y: hash & 0x3fff,
            }
        }
        P::SPOT_ANIM_SPECIFIC => {
            let index = r.read_u8_c()?;
            let graphic = none_if_ffff(r.read_u16_be_a()?);
            let settings = r.read_u8_c()?;
            let speed = r.read_u16_be_a()?;
            let target = r.read_u32_le()?;
            let height = r.read_u16_le()?;
            Misc::SpotAnimSpecific {
                index,
                graphic,
                settings,
                speed,
                target,
                height,
            }
        }
        P::GAME_MESSAGE => {
            let kind = r.read_u_smart()?;
            let effect_flags = r.read_i32_be()?;
            let flags = r.read_u8()?;
            let mut name = None;
            let mut name_formatted = None;
            if flags & 1 != 0 {
                name = Some(r.read_cp1252()?);
                if flags & 2 != 0 {
                    name_formatted = Some(r.read_cp1252()?);
                }
            }
            let message = r.read_cp1252()?;
            Misc::GameMessage {
                kind,
                effect_flags,
                flags,
                name,
                name_formatted,
                message,
            }
        }
        P::OPEN_URL => {
            let has_fallback = r.read_u8()? == 1;
            let remaining = r.remaining();
            let mut inner = PayloadReader::new(r.read_bytes(remaining)?);
            let url = inner.read_cp1252()?;
            let fallback = if has_fallback {
                Some(inner.read_cp1252()?)
            } else {
                None
            };
            Misc::OpenUrl { url, fallback }
        }
        P::QUICK_HOP_WORLDS => {
            let port = r.read_u16_be()?;
            let host = r.read_cp1252()?;
            let reconnecting = r.read_u8()? == 1;
            Misc::QuickHopWorlds {
                port,
                host,
                reconnecting,
            }
        }
        P::REQUEST_FPS => {
            let key = r.read_i32_be()?;
            let flags = r.read_i32_be()?;
            Misc::RequestFps { key, flags }
        }
        P::UPDATE_REBOOT_TIMER => Misc::RebootTimer(r.read_u16_be()?),
        P::UPDATE_DOB => {
            let value = r.read_i24_be()?;
            let flag = r.read_u8()? == 1;
            Misc::UpdateDob { value, flag }
        }
        P::JCOINS_UPDATE => Misc::JcoinsUpdate(r.read_u32_le()?),
        P::LOYALTY_UPDATE => {
            let balance = r.read_i32_be()?;
            let flag = r.read_u8_c()? == 1;
            Misc::LoyaltyUpdate { balance, flag }
        }
        P::IDENTIFY_HOST_NAME => Misc::IdentifyHostName(r.read_i32_be()?),
        P::APPLY_DEBUG => Misc::ApplyDebug(r.read_u16_be()?),
        P::UPDATE_UID192 => {
            let mut bytes = [0u8; 28];
            bytes.copy_from_slice(r.read_bytes(28)?);
            Misc::Uid192(bytes)
        }
        P::CREATE_ACCOUNT_REPLY
        | P::CREATE_CHECK_EMAIL_REPLY
        | P::CHAT_FILTER_SETTINGS_PRIVATECHAT => Misc::ByteReply(r.read_u8()?),
        P::DEPRECATED_52_CLIENTPACKET_97 => Misc::SignedByte(r.read_i8()?),
        P::CHAT_FILTER_SETTINGS => {
            let trade = r.read_u8_c()?;
            let public = r.read_u8_a()?;
            Misc::ChatFilter { trade, public }
        }
        P::WINDOW_CALL_WITH_ARGS | P::OPEN_SOCIAL_NETWORK_URL => {
            let remaining = r.remaining();
            Misc::WindowCall(r.read_bytes(remaining)?.to_vec())
        }
        P::ANIMATE_NPC => {
            let npc = r.read_u16_le_a()?;
            let mut values = [0u32; 4];
            for value in &mut values {
                *value = r.read_u32_le()?;
            }
            let speed = r.read_u8()?;
            Misc::AnimateNpc { npc, values, speed }
        }
        P::BLOCK_MINIMAP_STATE => Misc::MinimapState(r.read_u8()?),
        P::OBJ_ANIM_SPECIFIC => {
            let tile = r.read_u32_inverse_middle()?;
            let animation = r.read_u32_le()?;
            let flags = r.read_u8_s()?;
            Misc::ObjAnimSpecific {
                tile,
                animation,
                shape: flags >> 2,
                rotation: flags & 3,
            }
        }
        P::PROJANIM_SPECIFIC => {
            let end_height = r.read_u8_a()?;
            let source = r.read_i16_le_a()?;
            let end_time = r.read_u16_le()?;
            let lock_on = r.read_i16_be()?;
            let flags = r.read_u8_a()?;
            let y = r.read_u16_be_a()?;
            let slope = r.read_u16_be()?;
            let start_time = r.read_u16_le_a()?;
            let spot_anim = r.read_u16_le_a()?;
            let raw_angle = r.read_u8_a()?;
            let angle = if raw_angle == 255 {
                -1
            } else {
                i32::from(raw_angle)
            };
            let x_dist = r.read_i8_c()?;
            let y_dist = r.read_i8_c()?;
            let x = r.read_u16_be_a()?;
            let start_height = r.read_u8_c()?;
            Misc::ProjAnimSpecific {
                end_height,
                source,
                end_time,
                lock_on,
                flags,
                y,
                slope,
                start_time,
                spot_anim,
                angle,
                x_dist,
                y_dist,
                x,
                start_height,
            }
        }
        other => return Err(Error::UnsupportedOpcode(other)),
    };
    Ok(decoded)
}
