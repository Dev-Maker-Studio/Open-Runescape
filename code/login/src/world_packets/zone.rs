use super::{Error, none_if_ffff};
use openrs_protocol::payload::PayloadReader;
use openrs_protocol::registry::ServerPacket;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ZoneOrigin {
    pub x: i32,
    pub y: i32,
    pub plane: u8,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Tile {
    pub x: u8,
    pub y: u8,
}

impl Tile {
    fn from_byte(value: u8) -> Self {
        Self {
            x: (value >> 4) & 7,
            y: value & 7,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ZoneUpdate {
    CustomizeObject {
        loc_id: i32,
        shape: u8,
        transform_flags: u8,
        tile: Tile,
        models: Option<Vec<i32>>,
        recolors: Option<Vec<u16>>,
        retextures: Option<Vec<u16>>,
    },
    RemoveGroundItem {
        tile: Tile,
        item: u16,
    },
    CreateGroundItem {
        amount: u16,
        item: u16,
        tile: Tile,
    },
    DestroyObject {
        shape: u8,
        rotation: u8,
        tile: Tile,
    },
    CreateObject {
        object: i32,
        tile: Tile,
        shape: u8,
        rotation: u8,
    },
    MapProjanim {
        flags: u8,
        floor_height: bool,
        tile: Tile,
        x_off: i8,
        y_off: i8,
        lock_on: i16,
        spot_anim: u16,
        start_height: u16,
        end_height: u16,
        start_time: u16,
        end_time: u16,
        angle: i32,
        slope: u16,
    },
    MapProjanimHalfsq {
        coords: u8,
        flags: u8,
        x_off: i8,
        y_off: i8,
        source: i16,
        lock_on: i16,
        spot_anim: u16,
        start_height: i32,
        end_height: u16,
        start_time: u16,
        end_time: u16,
        angle: i32,
        slope: u16,
    },
    ObjectPrefetch {
        object: i32,
        type_index: u8,
    },
    GroundItemReveal {
        player: u16,
        tile: Tile,
        amount: u16,
        item: u16,
    },
    GroundItemCount {
        tile: Tile,
        item: u16,
        old_amount: u16,
        new_amount: u16,
    },
    MidiSongLocation {
        tile: Tile,
        sound: i32,
        flags: u8,
        delay: u8,
        volume: u8,
        rate: u16,
    },
    TileMessage {
        tile: Tile,
        priority: u16,
        duration: u8,
        color: u32,
        message: String,
    },
    ObjAnim {
        shape: u8,
        rotation: u8,
        tile: Tile,
        animation: u32,
    },
    SoundArea {
        tile: Tile,
        sound: i32,
        flags: u8,
        delay: u8,
        volume: u8,
        sample_rate: u16,
        voice_effect: bool,
    },
    SpotAnim {
        tile: Tile,
        spot_anim: i32,
        height: u16,
        speed: u16,
        rotation: u8,
    },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct CustomizeCounts {
    pub models: usize,
    pub recolors: usize,
    pub retextures: usize,
}

pub const SUB_PACKET_COUNT: usize = 15;

pub fn sub_packet_size(index: usize) -> Option<i32> {
    const SIZES: [i32; SUB_PACKET_COUNT] = [-1, 3, 5, 2, 6, 16, 19, 5, 7, 7, 8, -1, 6, 9, 8];
    SIZES.get(index).copied()
}

pub fn decode_sub_packet(
    index: u8,
    r: &mut PayloadReader<'_>,
    counts: &mut impl FnMut(i32, u8) -> CustomizeCounts,
) -> Result<ZoneUpdate, Error> {
    let update = match index {
        0 => {
            let loc_id = r.read_i32_be()?;
            let display = r.read_u8_a()?;
            let transform_flags = r.read_u8()?;
            let tile = Tile::from_byte(r.read_u8_a()?);
            let shape = display >> 2;
            let mut models = None;
            let mut recolors = None;
            let mut retextures = None;
            if transform_flags & 1 != 1 {
                let n = counts(loc_id, shape);
                if transform_flags & 2 == 2 {
                    let mut values = Vec::new();
                    for _ in 0..n.models {
                        values.push(r.read_i32_be()?);
                    }
                    models = Some(values);
                }
                if transform_flags & 4 == 4 {
                    let mut values = Vec::new();
                    for _ in 0..n.recolors {
                        values.push(r.read_u16_be()?);
                    }
                    recolors = Some(values);
                }
                if transform_flags & 8 == 8 {
                    let mut values = Vec::new();
                    for _ in 0..n.retextures {
                        values.push(r.read_u16_be()?);
                    }
                    retextures = Some(values);
                }
            }
            ZoneUpdate::CustomizeObject {
                loc_id,
                shape,
                transform_flags,
                tile,
                models,
                recolors,
                retextures,
            }
        }
        1 => {
            let tile = Tile::from_byte(r.read_u8_c()?);
            let item = r.read_u16_le_a()?;
            ZoneUpdate::RemoveGroundItem { tile, item }
        }
        2 => {
            let amount = r.read_u16_le_a()?;
            let item = r.read_u16_be_a()?;
            let tile = Tile::from_byte(r.read_u8_c()?);
            ZoneUpdate::CreateGroundItem { amount, item, tile }
        }
        3 => {
            let display = r.read_u8_a()?;
            let tile = Tile::from_byte(r.read_u8()?);
            ZoneUpdate::DestroyObject {
                shape: display >> 2,
                rotation: display & 3,
                tile,
            }
        }
        4 => {
            let object = r.read_i32_be()?;
            let tile = Tile::from_byte(r.read_u8_s()?);
            let display = r.read_u8_s()?;
            ZoneUpdate::CreateObject {
                object,
                tile,
                shape: display >> 2,
                rotation: display & 3,
            }
        }
        5 => {
            let flags = r.read_u8()?;
            let tile = Tile {
                x: (flags >> 3) & 7,
                y: flags & 7,
            };
            let x_off = r.read_i8()?;
            let y_off = r.read_i8()?;
            let lock_on = r.read_i16_be()?;
            let spot_anim = r.read_u16_be()?;
            let start_height = u16::from(r.read_u8()?) * 4;
            let end_height = u16::from(r.read_u8()?) * 4;
            let start_time = r.read_u16_be()?;
            let end_time = r.read_u16_be()?;
            let raw_angle = r.read_u8()?;
            let slope = r.read_u16_be()?;
            ZoneUpdate::MapProjanim {
                flags,
                floor_height: flags & 0x80 != 0,
                tile,
                x_off,
                y_off,
                lock_on,
                spot_anim,
                start_height,
                end_height,
                start_time,
                end_time,
                angle: if raw_angle == 255 {
                    -1
                } else {
                    i32::from(raw_angle)
                },
                slope,
            }
        }
        6 => {
            let coords = r.read_u8()?;
            let flags = r.read_u8()?;
            let x_off = r.read_i8()?;
            let y_off = r.read_i8()?;
            let source = r.read_i16_be()?;
            let lock_on = r.read_i16_be()?;
            let spot_anim = r.read_u16_be()?;
            let raw_start = r.read_u8()?;
            let start_height = if flags & 2 != 0 {
                i32::from(raw_start as i8)
            } else {
                i32::from(raw_start) * 4
            };
            let end_height = u16::from(r.read_u8()?) * 4;
            let start_time = r.read_u16_be()?;
            let end_time = r.read_u16_be()?;
            let raw_angle = r.read_u8()?;
            let slope = r.read_u16_be()?;
            ZoneUpdate::MapProjanimHalfsq {
                coords,
                flags,
                x_off,
                y_off,
                source,
                lock_on,
                spot_anim,
                start_height,
                end_height,
                start_time,
                end_time,
                angle: if raw_angle == 255 {
                    -1
                } else {
                    i32::from(raw_angle)
                },
                slope,
            }
        }
        7 => {
            let object = r.read_i32_be()?;
            let type_index = r.read_u8()?;
            ZoneUpdate::ObjectPrefetch { object, type_index }
        }
        8 => {
            let player = r.read_u16_be()?;
            let tile = Tile::from_byte(r.read_u8()?);
            let amount = r.read_u16_le_a()?;
            let item = r.read_u16_le_a()?;
            ZoneUpdate::GroundItemReveal {
                player,
                tile,
                amount,
                item,
            }
        }
        9 => {
            let tile = Tile::from_byte(r.read_u8()?);
            let item = r.read_u16_be()?;
            let old_amount = r.read_u16_be()?;
            let new_amount = r.read_u16_be()?;
            ZoneUpdate::GroundItemCount {
                tile,
                item,
                old_amount,
                new_amount,
            }
        }
        10 => {
            let tile = Tile::from_byte(r.read_u8()?);
            let sound = none_if_ffff(r.read_u16_be()?);
            let flags = r.read_u8()?;
            let delay = r.read_u8()?;
            let volume = r.read_u8()?;
            let rate = r.read_u16_be()?;
            ZoneUpdate::MidiSongLocation {
                tile,
                sound,
                flags,
                delay,
                volume,
                rate,
            }
        }
        11 => {
            r.read_u8()?;
            let tile = Tile::from_byte(r.read_u8()?);
            let priority = r.read_u16_be()?;
            let duration = r.read_u8()?;
            let color = r.read_u24_be()?;
            let message = r.read_cp1252()?;
            ZoneUpdate::TileMessage {
                tile,
                priority,
                duration,
                color,
                message,
            }
        }
        12 => {
            let info = r.read_u8_s()?;
            let tile = Tile::from_byte(r.read_u8_s()?);
            let animation = r.read_u32_le()?;
            ZoneUpdate::ObjAnim {
                shape: info >> 2,
                rotation: info & 3,
                tile,
                animation,
            }
        }
        13 => {
            let tile = Tile::from_byte(r.read_u8()?);
            let sound = none_if_ffff(r.read_u16_be()?);
            let flags = r.read_u8()?;
            let delay = r.read_u8()?;
            let volume = r.read_u8()?;
            let sample_rate = r.read_u16_be()?;
            let voice_effect = r.read_u8()? == 1;
            ZoneUpdate::SoundArea {
                tile,
                sound,
                flags,
                delay,
                volume,
                sample_rate,
                voice_effect,
            }
        }
        14 => {
            let tile = Tile::from_byte(r.read_u8()?);
            let spot_anim = none_if_ffff(r.read_u16_be()?);
            let height = r.read_u16_be()?;
            let speed = r.read_u16_be()?;
            let rotation = r.read_u8()?;
            ZoneUpdate::SpotAnim {
                tile,
                spot_anim,
                height,
                speed,
                rotation,
            }
        }
        _ => return Err(Error::InvalidSubPacket(index)),
    };
    Ok(update)
}

pub fn standalone_index(packet: ServerPacket) -> Option<u8> {
    use ServerPacket as P;
    Some(match packet {
        P::CUSTOMIZE_OBJECT => 0,
        P::REMOVE_GROUND_ITEM => 1,
        P::CREATE_GROUND_ITEM => 2,
        P::DESTROY_OBJECT => 3,
        P::CREATE_OBJECT => 4,
        P::MAP_PROJANIM => 5,
        P::MAP_PROJANIM_HALFSQ => 6,
        P::OBJECT_PREFETCH => 7,
        P::GROUND_ITEM_REVEAL => 8,
        P::GROUND_ITEM_COUNT => 9,
        P::MIDI_SONG_LOCATION => 10,
        P::TILE_MESSAGE => 11,
        P::OBJ_ANIM => 12,
        P::SPOT_ANIM => 14,
        _ => return None,
    })
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Zone {
    Origin(ZoneOrigin),
    Enclosed {
        origin: ZoneOrigin,
        updates: Vec<ZoneUpdate>,
    },
    Update(ZoneUpdate),
}

pub fn decode(
    packet: ServerPacket,
    size: usize,
    r: &mut PayloadReader<'_>,
    counts: &mut impl FnMut(i32, u8) -> CustomizeCounts,
) -> Result<Zone, Error> {
    use ServerPacket as P;
    match packet {
        P::UPDATE_ZONE_PARTIAL_FOLLOWS => {
            let y = i32::from(r.read_i8_a()?) << 3;
            let plane = r.read_u8_a()?;
            let x = i32::from(r.read_i8_a()?) << 3;
            Ok(Zone::Origin(ZoneOrigin { x, y, plane }))
        }
        P::UPDATE_ZONE_FULL_FOLLOWS => {
            let x = i32::from(r.read_i8_s()?) << 3;
            let y = i32::from(r.read_i8()?) << 3;
            let plane = r.read_u8()?;
            Ok(Zone::Origin(ZoneOrigin { x, y, plane }))
        }
        P::UPDATE_ZONE_PARTIAL_ENCLOSED => {
            let y = i32::from(r.read_i8_s()?) << 3;
            let plane = r.read_u8_a()?;
            let x = i32::from(r.read_i8()?) << 3;
            let mut updates = Vec::new();
            while r.position() < size {
                let index = r.read_u8()?;
                updates.push(decode_sub_packet(index, r, counts)?);
            }
            Ok(Zone::Enclosed {
                origin: ZoneOrigin { x, y, plane },
                updates,
            })
        }
        other => match standalone_index(other) {
            Some(index) => Ok(Zone::Update(decode_sub_packet(index, r, counts)?)),
            None => Err(Error::UnsupportedOpcode(other)),
        },
    }
}

pub fn is_zone_packet(packet: ServerPacket) -> bool {
    use ServerPacket as P;
    matches!(
        packet,
        P::UPDATE_ZONE_PARTIAL_FOLLOWS
            | P::UPDATE_ZONE_FULL_FOLLOWS
            | P::UPDATE_ZONE_PARTIAL_ENCLOSED
    ) || standalone_index(packet).is_some()
}
