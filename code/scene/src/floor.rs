use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

pub const MAX_DEFINITION_BYTES: usize = 4096;
pub const MAX_TAGS_PER_DEFINITION: usize = 256;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum FloorErrorKind {
    InputTooLarge { size: usize, limit: usize },
    UnexpectedEnd { needed: usize, remaining: usize },
    UnknownOpcode { opcode: u8 },
    TagLimit { limit: usize },
    TrailingData { remaining: usize },
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct FloorError {
    pub offset: usize,
    pub kind: FloorErrorKind,
}

impl Display for FloorError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        write!(f, "floor byte {}: ", self.offset)?;
        match self.kind {
            FloorErrorKind::InputTooLarge { size, limit } => {
                write!(f, "input size {size} exceeds byte limit {limit}")
            }
            FloorErrorKind::UnexpectedEnd { needed, remaining } => {
                write!(f, "expected {needed} bytes, only {remaining} remain")
            }
            FloorErrorKind::UnknownOpcode { opcode } => write!(f, "unknown opcode {opcode}"),
            FloorErrorKind::TagLimit { limit } => write!(f, "definition exceeds tag limit {limit}"),
            FloorErrorKind::TrailingData { remaining } => write!(f, "{remaining} trailing bytes"),
        }
    }
}

impl Error for FloorError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Underlay {
    pub rgb: u32,
    pub texture: i32,
    pub scale: u32,
    pub shadowed: bool,
    pub occlude: bool,
    pub hue: i32,
    pub saturation: i32,
    pub lightness: i32,
    pub hue_multiplier: i32,
}

impl Default for Underlay {
    fn default() -> Self {
        Self {
            rgb: 0,
            texture: -1,
            scale: 512,
            shadowed: true,
            occlude: true,
            hue: 0,
            saturation: 0,
            lightness: 0,
            hue_multiplier: 0,
        }
    }
}

impl Underlay {
    pub fn decode(input: &[u8]) -> Result<Self, FloorError> {
        let mut reader = Reader::new(input)?;
        let mut result = Self::default();
        loop {
            match reader.opcode()? {
                0 => {
                    reader.finish()?;
                    return Ok(result);
                }
                1 => {
                    result.rgb = reader.number(3)?;
                    result.calculate_hsl();
                }
                2 => result.texture = texture_id(reader.number(2)?),
                3 => result.scale = reader.number(2)? << 2,
                4 => result.shadowed = false,
                5 => result.occlude = false,
                opcode => return Err(reader.unknown_opcode(opcode)),
            }
        }
    }

    fn calculate_hsl(&mut self) {
        let (hue, saturation, lightness) = rgb_components(self.rgb);
        self.saturation = ((saturation * 256.0) as i32).clamp(0, 255);
        self.lightness = ((256.0 * lightness) as i32).clamp(0, 255);
        self.hue_multiplier = if lightness > 0.5 {
            (saturation * (1.0 - lightness) * 512.0) as i32
        } else {
            (saturation * lightness * 512.0) as i32
        }
        .max(1);
        self.hue = (hue * f64::from(self.hue_multiplier)) as i32;
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Overlay {
    pub slot: i32,
    pub tile_color_hsl: i32,
    pub texture: i32,
    pub occlude: bool,
    pub minimap_color_hsl: i32,
    pub texture_scale: u32,
    pub shadowed: bool,
    pub blends_with_underlay: bool,
    pub water_color: u32,
    pub water_fog_depth: u32,
    pub water_intensity: u32,
    pub opcode20: u32,
    pub unused_opcode21: u32,
    pub unused_opcode22: u32,
    pub overlay_id: u32,
    pub saw_opcode8: bool,
}

impl Default for Overlay {
    fn default() -> Self {
        Self {
            slot: 8,
            tile_color_hsl: 0,
            texture: -1,
            occlude: true,
            minimap_color_hsl: -1,
            texture_scale: 512,
            shadowed: true,
            blends_with_underlay: false,
            water_color: 1_190_717,
            water_fog_depth: 512,
            water_intensity: 255,
            opcode20: 63,
            unused_opcode21: 0,
            unused_opcode22: 64,
            overlay_id: 0,
            saw_opcode8: false,
        }
    }
}

impl Overlay {
    pub fn default_for_id(id: u32) -> Self {
        Self {
            slot: (8 << 8) | id as i32,
            overlay_id: id,
            ..Self::default()
        }
    }

    pub fn decode(id: u32, input: &[u8]) -> Result<Self, FloorError> {
        let mut reader = Reader::new(input)?;
        let mut result = Self {
            overlay_id: id,
            ..Self::default()
        };
        loop {
            match reader.opcode()? {
                0 => {
                    reader.finish()?;
                    result.slot = (result.slot << 8) | id as i32;
                    return Ok(result);
                }
                1 => result.tile_color_hsl = overlay_color(reader.number(3)?),
                2 => result.texture = reader.number(1)? as i32,
                3 => result.texture = texture_id(reader.number(2)?),
                5 => result.occlude = false,
                7 => result.minimap_color_hsl = overlay_color(reader.number(3)?),
                8 => result.saw_opcode8 = true,
                9 => result.texture_scale = reader.number(2)? << 2,
                10 => result.shadowed = false,
                11 => result.slot = reader.number(1)? as i32,
                12 => result.blends_with_underlay = true,
                13 => result.water_color = reader.number(3)?,
                14 => result.water_fog_depth = reader.number(1)? << 5,
                16 => result.water_intensity = reader.number(1)?,
                20 => result.opcode20 = reader.number(2)?,
                21 => result.unused_opcode21 = reader.number(1)?,
                22 => result.unused_opcode22 = reader.number(2)?,
                opcode => return Err(reader.unknown_opcode(opcode)),
            }
        }
    }
}

pub fn pack_hsl(hue: i32, saturation: i32, lightness: i32) -> i32 {
    let saturation = if lightness > 243 {
        saturation >> 4
    } else if lightness > 217 {
        saturation >> 3
    } else if lightness > 192 {
        saturation >> 2
    } else if lightness > 179 {
        saturation >> 1
    } else {
        saturation
    };
    (((hue & 0xff) >> 2) << 10)
        .wrapping_add(lightness >> 1)
        .wrapping_add((saturation >> 5).wrapping_shl(7))
}

pub fn rgb_to_hsl24(rgb: u32) -> i32 {
    let (hue, saturation, lightness) = rgb_components(rgb);
    pack_hsl(
        (256.0 * hue) as i32,
        ((saturation * 256.0) as i32).clamp(0, 255),
        ((lightness * 256.0) as i32).clamp(0, 255),
    )
}

pub fn blend_hsl(color_a: i32, color_b: i32, blend_factor: i32) -> i32 {
    if color_a == color_b {
        return color_a;
    }
    let inverse_factor = 128_i32.wrapping_sub(blend_factor);
    let lightness = inverse_factor
        .wrapping_mul(color_a & 0x7f)
        .wrapping_add((color_b & 0x7f).wrapping_mul(blend_factor))
        >> 7;
    let saturation = inverse_factor
        .wrapping_mul(color_a & 0x380)
        .wrapping_add(blend_factor.wrapping_mul(color_b & 0x380))
        >> 7;
    let hue = (color_b & 0xfc00)
        .wrapping_mul(blend_factor)
        .wrapping_add(inverse_factor.wrapping_mul(color_a & 0xfc00))
        >> 7;
    (hue & 0xfc00) | (saturation & 0x380) | (lightness & 0x7f)
}

fn rgb_components(rgb: u32) -> (f64, f64, f64) {
    let red = f64::from((rgb >> 16) & 0xff) / 256.0;
    let green = f64::from((rgb >> 8) & 0xff) / 256.0;
    let blue = f64::from(rgb & 0xff) / 256.0;
    let min = red.min(green).min(blue);
    let max = red.max(green).max(blue);
    let mut hue = 0.0;
    let mut saturation = 0.0;
    let lightness = (max + min) / 2.0;
    if max != min {
        saturation = if lightness < 0.5 {
            (max - min) / (max + min)
        } else {
            (max - min) / (2.0 - max - min)
        };
        hue = if red == max {
            (green - blue) / (max - min)
        } else if green == max {
            2.0 + (blue - red) / (max - min)
        } else {
            4.0 + (red - green) / (max - min)
        };
    }
    (hue / 6.0, saturation, lightness)
}

fn texture_id(raw: u32) -> i32 {
    if raw == 65_535 {
        -1
    } else {
        raw as i32
    }
}

fn overlay_color(rgb: u32) -> i32 {
    if rgb == 0xff00ff {
        -1
    } else {
        rgb_to_hsl24(rgb)
    }
}

struct Reader<'a> {
    input: &'a [u8],
    offset: usize,
    tags: usize,
}

impl<'a> Reader<'a> {
    fn new(input: &'a [u8]) -> Result<Self, FloorError> {
        if input.len() > MAX_DEFINITION_BYTES {
            return Err(FloorError {
                offset: 0,
                kind: FloorErrorKind::InputTooLarge {
                    size: input.len(),
                    limit: MAX_DEFINITION_BYTES,
                },
            });
        }
        Ok(Self {
            input,
            offset: 0,
            tags: 0,
        })
    }

    fn opcode(&mut self) -> Result<u8, FloorError> {
        if self.tags == MAX_TAGS_PER_DEFINITION {
            return Err(FloorError {
                offset: self.offset,
                kind: FloorErrorKind::TagLimit {
                    limit: MAX_TAGS_PER_DEFINITION,
                },
            });
        }
        let opcode = self.number(1)? as u8;
        self.tags += 1;
        Ok(opcode)
    }

    fn number(&mut self, count: usize) -> Result<u32, FloorError> {
        let remaining = self.input.len() - self.offset;
        if remaining < count {
            return Err(FloorError {
                offset: self.offset,
                kind: FloorErrorKind::UnexpectedEnd {
                    needed: count,
                    remaining,
                },
            });
        }
        let mut value = 0;
        for byte in &self.input[self.offset..self.offset + count] {
            value = (value << 8) | u32::from(*byte);
        }
        self.offset += count;
        Ok(value)
    }

    fn unknown_opcode(&self, opcode: u8) -> FloorError {
        FloorError {
            offset: self.offset - 1,
            kind: FloorErrorKind::UnknownOpcode { opcode },
        }
    }

    fn finish(&self) -> Result<(), FloorError> {
        if self.offset != self.input.len() {
            return Err(FloorError {
                offset: self.offset,
                kind: FloorErrorKind::TrailingData {
                    remaining: self.input.len() - self.offset,
                },
            });
        }
        Ok(())
    }
}
