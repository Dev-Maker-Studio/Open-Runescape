use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::fmt;

pub const MAX_WORLD_STATE_BYTES: usize = u16::MAX as usize;
pub const PLAYER_SLOTS: usize = 2048;
pub const OWN_PLAYER_BLOCK_BYTES: usize = (30usize + (PLAYER_SLOTS - 2) * 18).div_ceil(8);
pub const MAX_CHUNK_DESCRIPTORS: usize = 4 * 21 * 21;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    Payload(PayloadError),
    LengthLimitExceeded { length: usize, limit: usize },
    InvalidPlayerIndex(u16),
    UnsupportedOpcode(ServerPacket),
    InvalidMapSize(u8),
    InvalidArchiveCoordinates { x: i32, y: i32 },
    InvalidKeyRemainder(usize),
    KeyCountMismatch { expected: usize, actual: usize },
    AllocationFailed,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Payload(error) => error.fmt(f),
            Self::LengthLimitExceeded { length, limit } => {
                write!(
                    f,
                    "fresh-world payload has {length} bytes, limit is {limit}"
                )
            }
            Self::InvalidPlayerIndex(index) => {
                write!(f, "player index {index} is outside 1..=2047")
            }
            Self::UnsupportedOpcode(opcode) => {
                write!(f, "unsupported fresh-world opcode {opcode:?}")
            }
            Self::InvalidMapSize(id) => write!(f, "unsupported map size {id}"),
            Self::InvalidArchiveCoordinates { x, y } => {
                write!(f, "map archive coordinates ({x}, {y}) are outside 0..=255")
            }
            Self::InvalidKeyRemainder(remaining) => {
                write!(
                    f,
                    "map key section has {remaining} bytes, not a multiple of 16"
                )
            }
            Self::KeyCountMismatch { expected, actual } => {
                write!(f, "map has {actual} key groups for {expected} regions")
            }
            Self::AllocationFailed => f.write_str("fresh-world allocation failed"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Payload(error) => Some(error),
            _ => None,
        }
    }
}

impl From<PayloadError> for Error {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Tile {
    pub plane: u8,
    pub x: u16,
    pub y: u16,
}

impl Tile {
    pub fn tile_hash(self) -> u32 {
        (u32::from(self.plane) << 28) | (u32::from(self.x) << 14) | u32::from(self.y)
    }

    pub fn region_hash(self) -> u32 {
        (u32::from(self.plane) << 28) | (u32::from(self.x >> 6) << 14) | u32::from(self.y >> 6)
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct PlayerRegion {
    pub index: u16,
    pub plane: u8,
    pub region_x: u8,
    pub region_y: u8,
}

impl PlayerRegion {
    pub fn region_hash(self) -> u32 {
        (u32::from(self.plane) << 28) | (u32::from(self.region_x) << 14) | u32::from(self.region_y)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OwnPlayerBlock {
    pub player_index: u16,
    pub tile: Tile,
    pub other_players: Vec<PlayerRegion>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MapSize {
    Default104,
    Extended120,
    Extended136,
    Extended168,
    Mini72,
}

impl MapSize {
    pub fn from_id(id: u8) -> Result<Self, Error> {
        match id {
            0 => Ok(Self::Default104),
            1 => Ok(Self::Extended120),
            2 => Ok(Self::Extended136),
            3 => Ok(Self::Extended168),
            4 => Ok(Self::Mini72),
            _ => Err(Error::InvalidMapSize(id)),
        }
    }

    pub fn id(self) -> u8 {
        match self {
            Self::Default104 => 0,
            Self::Extended120 => 1,
            Self::Extended136 => 2,
            Self::Extended168 => 3,
            Self::Mini72 => 4,
        }
    }

    pub fn size(self) -> u16 {
        match self {
            Self::Default104 => 104,
            Self::Extended120 => 120,
            Self::Extended136 => 136,
            Self::Extended168 => 168,
            Self::Mini72 => 72,
        }
    }

    pub fn chunk_span(self) -> usize {
        usize::from(self.size() >> 3)
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LoadingType {
    Standard,
    Instance,
    InstanceNoEntityReset,
    InstanceLarge,
    InstanceLargeNoReloadUpdateZones,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MapBase {
    pub x: i32,
    pub y: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChunkGrid {
    pub span: usize,
    pub descriptors: Vec<Option<u32>>,
}

impl ChunkGrid {
    pub fn get(&self, plane: usize, x: usize, y: usize) -> Option<Option<u32>> {
        if plane >= 4 || x >= self.span || y >= self.span {
            return None;
        }
        self.descriptors
            .get((plane * self.span + x) * self.span + y)
            .copied()
    }
}

#[derive(Clone, PartialEq, Eq)]
pub struct MapLayout {
    pub map_size: MapSize,
    pub center_x: u16,
    pub center_y: u16,
    pub force_position_update: bool,
    pub loading_type: LoadingType,
    pub base: MapBase,
    pub chunks: Option<ChunkGrid>,
    pub region_ids: Vec<u16>,
    pub xtea_keys: Vec<[i32; 4]>,
}

impl fmt::Debug for MapLayout {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("MapLayout")
            .field("map_size", &self.map_size)
            .field("center_x", &self.center_x)
            .field("center_y", &self.center_y)
            .field("force_position_update", &self.force_position_update)
            .field("loading_type", &self.loading_type)
            .field("base", &self.base)
            .field("chunks", &self.chunks)
            .field("region_ids", &self.region_ids)
            .field("key_count", &self.xtea_keys.len())
            .finish_non_exhaustive()
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct LocalTile {
    pub plane: u8,
    pub x: i32,
    pub y: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct WorldState {
    pub players: OwnPlayerBlock,
    pub map: MapLayout,
    pub local_tile: LocalTile,
}

fn bounded_reader(payload: &[u8]) -> Result<PayloadReader<'_>, Error> {
    if payload.len() > MAX_WORLD_STATE_BYTES {
        return Err(Error::LengthLimitExceeded {
            length: payload.len(),
            limit: MAX_WORLD_STATE_BYTES,
        });
    }
    Ok(PayloadReader::new(payload))
}

fn reserved<T>(length: usize) -> Result<Vec<T>, Error> {
    let mut values = Vec::new();
    values
        .try_reserve_exact(length)
        .map_err(|_| Error::AllocationFailed)?;
    Ok(values)
}

fn own_player(player_index: u16, reader: &mut PayloadReader<'_>) -> Result<OwnPlayerBlock, Error> {
    if !(1..PLAYER_SLOTS as u16).contains(&player_index) {
        return Err(Error::InvalidPlayerIndex(player_index));
    }
    reader.start_bit_access()?;
    let coordinates = reader.read_bits(30)?;
    let tile = Tile {
        plane: (coordinates >> 28) as u8,
        x: ((coordinates >> 14) & 0x3fff) as u16,
        y: (coordinates & 0x3fff) as u16,
    };
    let mut other_players = reserved(PLAYER_SLOTS - 2)?;
    for index in 1..PLAYER_SLOTS as u16 {
        if index == player_index {
            continue;
        }
        let coordinates = reader.read_bits(18)?;
        other_players.push(PlayerRegion {
            index,
            plane: (coordinates >> 16) as u8,
            region_x: ((coordinates >> 8) & 0xff) as u8,
            region_y: (coordinates & 0xff) as u8,
        });
    }
    reader.finish_bit_access()?;
    Ok(OwnPlayerBlock {
        player_index,
        tile,
        other_players,
    })
}

pub fn decode_own_player(
    player_index: u16,
    payload: &[u8],
) -> Result<(OwnPlayerBlock, usize), Error> {
    let mut reader = bounded_reader(payload)?;
    let players = own_player(player_index, &mut reader)?;
    Ok((players, reader.position()))
}

fn base(map_size: MapSize, center_x: u16, center_y: u16) -> MapBase {
    let radius = i32::from(map_size.size() >> 4);
    MapBase {
        x: (i32::from(center_x) - radius) * 8,
        y: (i32::from(center_y) - radius) * 8,
    }
}

fn keys(reader: &mut PayloadReader<'_>, expected: usize) -> Result<Vec<[i32; 4]>, Error> {
    let remaining = reader.remaining();
    if !remaining.is_multiple_of(16) {
        return Err(Error::InvalidKeyRemainder(remaining));
    }
    let actual = remaining / 16;
    if actual != expected {
        return Err(Error::KeyCountMismatch { expected, actual });
    }
    let mut keys = reserved(expected)?;
    for _ in 0..expected {
        keys.push([
            reader.read_i32_be()?,
            reader.read_i32_be()?,
            reader.read_i32_be()?,
            reader.read_i32_be()?,
        ]);
    }
    Ok(keys)
}

fn standard_map(reader: &mut PayloadReader<'_>) -> Result<MapLayout, Error> {
    let map_size = MapSize::from_id(reader.read_u8()?)?;
    let center_x = reader.read_u16_be()?;
    let center_y = reader.read_u16_be()?;
    let force_position_update = reader.read_u8()? == 1;
    let radius = i32::from(map_size.size() >> 4);
    let min_x = (i32::from(center_x) - radius) / 8;
    let max_x = (i32::from(center_x) + radius) / 8;
    let min_y = (i32::from(center_y) - radius) / 8;
    let max_y = (i32::from(center_y) + radius) / 8;
    for (x, y) in [(min_x, min_y), (max_x, max_y)] {
        if !(0..=255).contains(&x) || !(0..=255).contains(&y) {
            return Err(Error::InvalidArchiveCoordinates { x, y });
        }
    }
    let count = ((max_x - min_x + 1) * (max_y - min_y + 1)) as usize;
    let mut region_ids = reserved(count)?;
    for x in min_x..=max_x {
        for y in min_y..=max_y {
            region_ids.push(((x << 8) + y) as u16);
        }
    }
    let xtea_keys = keys(reader, region_ids.len())?;
    Ok(MapLayout {
        map_size,
        center_x,
        center_y,
        force_position_update,
        loading_type: LoadingType::Standard,
        base: base(map_size, center_x, center_y),
        chunks: None,
        region_ids,
        xtea_keys,
    })
}

fn dynamic_map(reader: &mut PayloadReader<'_>) -> Result<MapLayout, Error> {
    let force_position_update = reader.read_u8_c()? & 1 != 0;
    let loading_type = match reader.read_u8_s()? {
        2 => LoadingType::InstanceNoEntityReset,
        3 => LoadingType::InstanceLarge,
        4 => LoadingType::InstanceLargeNoReloadUpdateZones,
        _ => LoadingType::Instance,
    };
    let map_size = MapSize::from_id(reader.read_u8_a()?)?;
    let center_y = reader.read_u16_be_a()?;
    let center_x = reader.read_u16_be()?;
    let span = map_size.chunk_span();
    let count = 4 * span * span;
    let mut descriptors = reserved(count)?;
    let mut region_ids = reserved(count)?;
    let mut seen = [0u64; 512];
    reader.start_bit_access()?;
    for _ in 0..count {
        if reader.read_bits(1)? == 0 {
            descriptors.push(None);
            continue;
        }
        let descriptor = reader.read_bits(26)?;
        descriptors.push(Some(descriptor));
        let chunk_x = (descriptor >> 14) & 0x3ff;
        let chunk_y = (descriptor >> 3) & 0x7ff;
        let region_id = ((chunk_x / 8) << 8) + chunk_y / 8;
        let mask = 1u64 << (region_id % 64);
        let word = &mut seen[region_id as usize / 64];
        if *word & mask == 0 {
            *word |= mask;
            region_ids.push(region_id as u16);
        }
    }
    reader.finish_bit_access()?;
    let xtea_keys = keys(reader, region_ids.len())?;
    Ok(MapLayout {
        map_size,
        center_x,
        center_y,
        force_position_update,
        loading_type,
        base: base(map_size, center_x, center_y),
        chunks: Some(ChunkGrid { span, descriptors }),
        region_ids,
        xtea_keys,
    })
}

pub fn decode_standard_map(payload: &[u8]) -> Result<MapLayout, Error> {
    standard_map(&mut bounded_reader(payload)?)
}

pub fn decode_dynamic_map(payload: &[u8]) -> Result<MapLayout, Error> {
    dynamic_map(&mut bounded_reader(payload)?)
}

pub fn decode_fresh_world(
    player_index: u16,
    opcode: ServerPacket,
    payload: &[u8],
) -> Result<WorldState, Error> {
    if !matches!(
        opcode,
        ServerPacket::REGION | ServerPacket::DYNAMIC_MAP_REGION
    ) {
        return Err(Error::UnsupportedOpcode(opcode));
    }
    let mut reader = bounded_reader(payload)?;
    let players = own_player(player_index, &mut reader)?;
    let map = match opcode {
        ServerPacket::REGION => standard_map(&mut reader)?,
        ServerPacket::DYNAMIC_MAP_REGION => dynamic_map(&mut reader)?,
        _ => unreachable!(),
    };
    let local_tile = LocalTile {
        plane: players.tile.plane,
        x: i32::from(players.tile.x) - map.base.x,
        y: i32::from(players.tile.y) - map.base.y,
    };
    Ok(WorldState {
        players,
        map,
        local_tile,
    })
}
