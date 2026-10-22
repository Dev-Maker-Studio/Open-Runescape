use crate::{Error, Reader};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MaterialType {
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

impl MaterialType {
    fn blank() -> Self {
        Self {
            details_only: false,
            is_small: false,
            skip_triangles: false,
            brightness: 0,
            alpha: 0,
            effect_id: 0,
            effect_param1: 0,
            color_hsl: 0,
            texture_speed_u: 0,
            texture_speed_v: 0,
            a_bool2087: false,
            is_brick_tile: false,
            mipmapping: 0,
            repeat_s: false,
            repeat_t: false,
            hdr: false,
            combine_mode: 0,
            effect_param2: 0,
            effect_combiner: 0,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MaterialTable {
    pub materials: Vec<Option<MaterialType>>,
}

impl MaterialTable {
    pub fn len(&self) -> usize {
        self.materials.len()
    }

    pub fn is_empty(&self) -> bool {
        self.materials.is_empty()
    }

    pub fn get(&self, id: usize) -> Option<&MaterialType> {
        self.materials.get(id).and_then(Option::as_ref)
    }
}

pub fn is_opaque_effect(effect_id: i32) -> bool {
    effect_id != 1 && effect_id != 7
}

fn column<T>(
    reader: &mut Reader<'_>,
    materials: &mut [Option<MaterialType>],
    mut read: impl FnMut(&mut Reader<'_>) -> Result<T, Error>,
    mut set: impl FnMut(&mut MaterialType, T),
) -> Result<(), Error> {
    for slot in materials.iter_mut() {
        let value = read(reader)?;
        if let Some(material) = slot {
            set(material, value);
        }
    }
    Ok(())
}

pub fn parse_material_table(payload: &[u8]) -> Result<MaterialTable, Error> {
    let mut reader = Reader::new(payload);
    let count = reader.u16()? as usize;
    if reader.remaining() < count {
        return Err(Error::Truncated);
    }
    let mut materials: Vec<Option<MaterialType>> = Vec::with_capacity(count);
    for _ in 0..count {
        materials.push(if reader.u8()? == 1 {
            Some(MaterialType::blank())
        } else {
            None
        });
    }
    let m = &mut materials[..];
    let r = &mut reader;
    column(r, m, |r| r.u8(), |x, v| x.details_only = v == 0)?;
    column(r, m, |r| r.u8(), |x, v| x.is_small = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.skip_triangles = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.brightness = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.alpha = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.effect_id = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.effect_param1 = v as i8)?;
    column(r, m, |r| r.u16(), |x, v| x.color_hsl = v as i16)?;
    column(r, m, |r| r.u8(), |x, v| x.texture_speed_u = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.texture_speed_v = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.a_bool2087 = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.is_brick_tile = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.mipmapping = v as i8)?;
    column(r, m, |r| r.u8(), |x, v| x.repeat_s = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.repeat_t = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.hdr = v == 1)?;
    column(r, m, |r| r.u8(), |x, v| x.combine_mode = v)?;
    column(r, m, |r| r.i32(), |x, v| x.effect_param2 = v)?;
    column(r, m, |r| r.u8(), |x, v| x.effect_combiner = v)?;
    if reader.remaining() != 0 {
        return Err(Error::TrailingData);
    }
    Ok(MaterialTable { materials })
}
