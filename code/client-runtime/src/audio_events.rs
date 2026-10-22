use openrs_login::world_packets::zone::{Tile, ZoneUpdate};
use openrs_protocol::registry::ServerPacket;

pub const MISC: i32 = 1;
pub const SOUND_AREA: i32 = 3;
pub const MIDI_SONG_LOCATION: i32 = 4;

pub fn is_audio_packet(packet: ServerPacket) -> bool {
    matches!(
        packet,
        ServerPacket::VORBIS_SOUND
            | ServerPacket::VORBIS_PRELOAD_SOUND
            | ServerPacket::MUSIC_TRACK
            | ServerPacket::MUSIC_EFFECT
            | ServerPacket::SET_SONG_VOLUME
            | ServerPacket::SET_VORBIS_SOUND
            | ServerPacket::STOP_VORBIS_STREAM
            | ServerPacket::PRELOAD_SONG
            | ServerPacket::SOUND_SYNTH
            | ServerPacket::VORBIS_SPEECH_SOUND
            | ServerPacket::PLAY_SONG_AT_POSITION
    )
}

pub fn misc_ints(packet: ServerPacket, payload: &[u8]) -> Vec<i32> {
    let mut out = vec![MISC, i32::from(packet.wire()), payload.len() as i32];
    out.extend(payload.iter().map(|byte| i32::from(*byte)));
    out
}

pub fn zone_ints(zone: (i32, i32, u8), update: &ZoneUpdate) -> Option<Vec<i32>> {
    let abs = |tile: &Tile| (zone.0 + i32::from(tile.x), zone.1 + i32::from(tile.y));
    match update {
        ZoneUpdate::SoundArea {
            tile,
            sound,
            flags,
            delay,
            volume,
            sample_rate,
            voice_effect,
        } => {
            let (x, y) = abs(tile);
            Some(vec![
                SOUND_AREA,
                x,
                y,
                i32::from(zone.2),
                *sound,
                i32::from(*flags),
                i32::from(*delay),
                i32::from(*volume),
                i32::from(*sample_rate),
                i32::from(*voice_effect),
            ])
        }
        ZoneUpdate::MidiSongLocation {
            tile,
            sound,
            flags,
            delay,
            volume,
            rate,
        } => {
            let (x, y) = abs(tile);
            Some(vec![
                MIDI_SONG_LOCATION,
                x,
                y,
                i32::from(zone.2),
                *sound,
                i32::from(*flags),
                i32::from(*delay),
                i32::from(*volume),
                i32::from(*rate),
            ])
        }
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sound_area_is_flattened_with_the_tile_offset() {
        let update = ZoneUpdate::SoundArea {
            tile: Tile { x: 3, y: 5 },
            sound: 9,
            flags: 0x21,
            delay: 2,
            volume: 100,
            sample_rate: 256,
            voice_effect: true,
        };
        assert_eq!(
            zone_ints((8, 16, 1), &update),
            Some(vec![SOUND_AREA, 11, 21, 1, 9, 0x21, 2, 100, 256, 1])
        );
        assert_eq!(zone_ints((0, 0, 0), &ZoneUpdate::ObjectPrefetch { object: 0, type_index: 0 }), None);
    }

    #[test]
    fn misc_packets_carry_their_payload_bytes() {
        let ints = misc_ints(ServerPacket::PRELOAD_SONG, &[1, 2]);
        assert_eq!(ints[0], MISC);
        assert_eq!(&ints[2..], &[2, 1, 2]);
    }
}
