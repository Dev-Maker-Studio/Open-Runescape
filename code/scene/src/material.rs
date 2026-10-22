use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

pub const MAX_MATERIALS: usize = u16::MAX as usize;
pub const BYTES_PER_MATERIAL: usize = 23;
pub const MAX_DEFINITION_BYTES: usize = 2 + MAX_MATERIALS * (1 + BYTES_PER_MATERIAL);

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MaterialErrorKind {
    InputTooLarge { size: usize, limit: usize },
    UnexpectedEnd { needed: usize, remaining: usize },
    TrailingData { remaining: usize },
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct MaterialError {
    pub offset: usize,
    pub kind: MaterialErrorKind,
}

impl Display for MaterialError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        write!(f, "material byte {}: ", self.offset)?;
        match self.kind {
            MaterialErrorKind::InputTooLarge { size, limit } => {
                write!(f, "input size {size} exceeds byte limit {limit}")
            }
            MaterialErrorKind::UnexpectedEnd { needed, remaining } => {
                write!(f, "expected {needed} bytes, only {remaining} remain")
            }
            MaterialErrorKind::TrailingData { remaining } => {
                write!(f, "{remaining} trailing bytes")
            }
        }
    }
}

impl Error for MaterialError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MaterialLookupError {
    OutOfRange { id: i32, size: usize },
    Missing { id: i32 },
}

impl Display for MaterialLookupError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::OutOfRange { id, size } => {
                write!(f, "material id {id} outside table of size {size}")
            }
            Self::Missing { id } => write!(f, "material not found: {id}"),
        }
    }
}

impl Error for MaterialLookupError {}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct Material {
    pub details_only: bool,
    pub is_small: bool,
    pub skip_triangles: bool,
    pub brightness: i8,
    pub alpha: i8,
    pub effect_id: i8,
    pub effect_param1: i8,
    pub color_hsl: i16,
    pub texture_speed_u: i8,
    pub texture_speed_v: i8,
    pub a_bool2087: bool,
    pub is_brick_tile: bool,
    pub mipmapping: i8,
    pub repeat_s: bool,
    pub repeat_t: bool,
    pub hdr: bool,
    pub combine_mode: u8,
    pub effect_param2: i32,
    pub effect_combiner: u8,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct MaterialTable {
    materials: Vec<Option<Material>>,
}

impl MaterialTable {
    pub fn decode(input: &[u8]) -> Result<Self, MaterialError> {
        let mut reader = Reader::new(input)?;
        let count = usize::from(reader.u16()?);
        let mut materials = Vec::with_capacity(count);
        for _ in 0..count {
            materials.push((reader.u8()? == 1).then(Material::default));
        }
        for material in materials.iter_mut().flatten() {
            material.details_only = reader.u8()? == 0;
        }
        for material in materials.iter_mut().flatten() {
            material.is_small = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.skip_triangles = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.brightness = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.alpha = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.effect_id = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.effect_param1 = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.color_hsl = reader.u16()? as i16;
        }
        for material in materials.iter_mut().flatten() {
            material.texture_speed_u = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.texture_speed_v = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.a_bool2087 = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.is_brick_tile = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.mipmapping = reader.u8()? as i8;
        }
        for material in materials.iter_mut().flatten() {
            material.repeat_s = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.repeat_t = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.hdr = reader.u8()? == 1;
        }
        for material in materials.iter_mut().flatten() {
            material.combine_mode = reader.u8()?;
        }
        for material in materials.iter_mut().flatten() {
            material.effect_param2 = i32::from_be_bytes(reader.bytes()?);
        }
        for material in materials.iter_mut().flatten() {
            material.effect_combiner = reader.u8()?;
        }
        reader.finish()?;
        Ok(Self { materials })
    }

    pub fn len(&self) -> usize {
        self.materials.len()
    }

    pub fn is_empty(&self) -> bool {
        self.materials.is_empty()
    }

    pub fn slots(&self) -> &[Option<Material>] {
        &self.materials
    }

    pub fn get_material(&self, id: i32) -> Result<&Material, MaterialLookupError> {
        let slot = usize::try_from(id)
            .ok()
            .and_then(|index| self.materials.get(index))
            .ok_or(MaterialLookupError::OutOfRange {
                id,
                size: self.len(),
            })?;
        slot.as_ref().ok_or(MaterialLookupError::Missing { id })
    }
}

pub fn is_opaque_effect(effect_id: i32) -> bool {
    effect_id != 1 && effect_id != 7
}

struct Reader<'a> {
    input: &'a [u8],
    offset: usize,
}

impl<'a> Reader<'a> {
    fn new(input: &'a [u8]) -> Result<Self, MaterialError> {
        if input.len() > MAX_DEFINITION_BYTES {
            return Err(MaterialError {
                offset: 0,
                kind: MaterialErrorKind::InputTooLarge {
                    size: input.len(),
                    limit: MAX_DEFINITION_BYTES,
                },
            });
        }
        Ok(Self { input, offset: 0 })
    }

    fn bytes<const N: usize>(&mut self) -> Result<[u8; N], MaterialError> {
        let remaining = self.input.len() - self.offset;
        if remaining < N {
            return Err(MaterialError {
                offset: self.offset,
                kind: MaterialErrorKind::UnexpectedEnd {
                    needed: N,
                    remaining,
                },
            });
        }
        let mut result = [0; N];
        result.copy_from_slice(&self.input[self.offset..self.offset + N]);
        self.offset += N;
        Ok(result)
    }

    fn u8(&mut self) -> Result<u8, MaterialError> {
        Ok(self.bytes::<1>()?[0])
    }

    fn u16(&mut self) -> Result<u16, MaterialError> {
        Ok(u16::from_be_bytes(self.bytes()?))
    }

    fn finish(&self) -> Result<(), MaterialError> {
        if self.offset != self.input.len() {
            return Err(MaterialError {
                offset: self.offset,
                kind: MaterialErrorKind::TrailingData {
                    remaining: self.input.len() - self.offset,
                },
            });
        }
        Ok(())
    }
}
