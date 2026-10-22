use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

mod height;

use height::procedural_surface_height;

pub const PLANES: usize = 4;
pub const MAP_SIZE: usize = 64;
pub const TILE_COUNT: usize = PLANES * MAP_SIZE * MAP_SIZE;

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum HeightInstruction {
    #[default]
    Procedural,
    Explicit(u8),
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct Tile {
    pub height: HeightInstruction,
    pub overlay_id: u8,
    pub underlay_id: u8,
    pub render_flags: u8,
    pub overlay_shape: u8,
    pub overlay_rotation: u8,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct DecodeLimits {
    pub max_input_bytes: usize,
    pub max_tags_per_tile: usize,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct TilePosition {
    pub plane: usize,
    pub x: usize,
    pub y: usize,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ByteField {
    Tag,
    ExplicitHeight,
    OverlayId,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum DecodeErrorKind {
    InputTooLarge { size: usize, limit: usize },
    UnexpectedEnd { field: ByteField },
    TileTagLimit { limit: usize },
    TrailingData { remaining: usize },
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct DecodeError {
    pub offset: usize,
    pub tile: Option<TilePosition>,
    pub kind: DecodeErrorKind,
}

impl Display for DecodeError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        write!(f, "terrain byte {}", self.offset)?;
        if let Some(tile) = self.tile {
            write!(f, " at plane {} tile ({}, {})", tile.plane, tile.x, tile.y)?;
        }
        match self.kind {
            DecodeErrorKind::InputTooLarge { size, limit } => {
                write!(f, ": input size {size} exceeds byte limit {limit}")
            }
            DecodeErrorKind::UnexpectedEnd { field } => {
                write!(f, ": missing {field:?}")
            }
            DecodeErrorKind::TileTagLimit { limit } => {
                write!(f, ": tile exceeds tag limit {limit}")
            }
            DecodeErrorKind::TrailingData { remaining } => {
                write!(f, ": {remaining} trailing bytes")
            }
        }
    }
}

impl Error for DecodeError {}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Terrain {
    tiles: Box<[Tile]>,
}

impl Terrain {
    pub fn decode(input: &[u8], limits: DecodeLimits) -> Result<Self, DecodeError> {
        let (terrain, remaining) = Self::decode_prefix(input, limits)?;
        if !remaining.is_empty() {
            return Err(DecodeError {
                offset: input.len() - remaining.len(),
                tile: None,
                kind: DecodeErrorKind::TrailingData {
                    remaining: remaining.len(),
                },
            });
        }
        Ok(terrain)
    }

    pub fn decode_prefix(input: &[u8], limits: DecodeLimits) -> Result<(Self, &[u8]), DecodeError> {
        Self::decode_planes(input, PLANES, limits)
    }

    pub fn decode_planes(
        input: &[u8],
        planes: usize,
        limits: DecodeLimits,
    ) -> Result<(Self, &[u8]), DecodeError> {
        let planes = planes.min(PLANES);
        if input.len() > limits.max_input_bytes {
            return Err(DecodeError {
                offset: 0,
                tile: None,
                kind: DecodeErrorKind::InputTooLarge {
                    size: input.len(),
                    limit: limits.max_input_bytes,
                },
            });
        }
        let mut reader = Reader { input, offset: 0 };
        let mut tiles = vec![Tile::default(); TILE_COUNT].into_boxed_slice();
        for plane in 0..planes {
            for x in 0..MAP_SIZE {
                for y in 0..MAP_SIZE {
                    let position = TilePosition { plane, x, y };
                    let tile = &mut tiles[(plane * MAP_SIZE + x) * MAP_SIZE + y];
                    let mut terminated = false;
                    for _ in 0..limits.max_tags_per_tile {
                        let tag = reader.byte(position, ByteField::Tag)?;
                        match tag {
                            0 => {
                                terminated = true;
                                break;
                            }
                            1 => {
                                tile.height = HeightInstruction::Explicit(
                                    reader.byte(position, ByteField::ExplicitHeight)?,
                                );
                                terminated = true;
                                break;
                            }
                            2..=49 => {
                                tile.overlay_id = reader.byte(position, ByteField::OverlayId)?;
                                tile.overlay_shape = (tag - 2) / 4;
                                tile.overlay_rotation = (tag - 2) & 3;
                            }
                            50..=81 => tile.render_flags = tag - 49,
                            82..=255 => tile.underlay_id = tag - 81,
                        }
                    }
                    if !terminated {
                        return Err(DecodeError {
                            offset: reader.offset,
                            tile: Some(position),
                            kind: DecodeErrorKind::TileTagLimit {
                                limit: limits.max_tags_per_tile,
                            },
                        });
                    }
                }
            }
        }
        Ok((Self { tiles }, &input[reader.offset..]))
    }

    pub fn tiles(&self) -> &[Tile] {
        &self.tiles
    }

    pub fn tile(&self, plane: usize, x: usize, y: usize) -> Option<&Tile> {
        Self::index(plane, x, y).map(|index| &self.tiles[index])
    }

    pub fn surface_height_native_units(
        &self,
        absolute_tile_origin: [i32; 2],
        plane: usize,
        x: usize,
        y: usize,
    ) -> Option<i32> {
        Self::index(plane, x, y)?;
        let mut height = 0;
        for level in 0..=plane {
            match self.tiles[(level * MAP_SIZE + x) * MAP_SIZE + y].height {
                HeightInstruction::Procedural if level == 0 => {
                    let absolute_x = absolute_tile_origin[0].wrapping_add(x as i32);
                    let absolute_y = absolute_tile_origin[1].wrapping_add(y as i32);
                    height = procedural_surface_height(absolute_x, absolute_y);
                }
                HeightInstruction::Procedural => height -= 960,
                HeightInstruction::Explicit(raw) => {
                    let delta = if raw == 1 { 0 } else { i32::from(raw) };
                    height -= delta * 32;
                }
            }
        }
        Some(height)
    }

    fn index(plane: usize, x: usize, y: usize) -> Option<usize> {
        if plane < PLANES && x < MAP_SIZE && y < MAP_SIZE {
            Some((plane * MAP_SIZE + x) * MAP_SIZE + y)
        } else {
            None
        }
    }
}

struct Reader<'a> {
    input: &'a [u8],
    offset: usize,
}

impl Reader<'_> {
    fn byte(&mut self, tile: TilePosition, field: ByteField) -> Result<u8, DecodeError> {
        let value = self.input.get(self.offset).copied().ok_or(DecodeError {
            offset: self.offset,
            tile: Some(tile),
            kind: DecodeErrorKind::UnexpectedEnd { field },
        })?;
        self.offset += 1;
        Ok(value)
    }
}
