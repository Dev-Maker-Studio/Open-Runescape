use super::packet::{Packet, PacketError};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Material {
    pub details_only: bool,
    pub is_small: bool,
    pub skip_triangles: bool,
    pub brightness: i32,
    pub alpha: i32,
    pub effect_id: i32,
    pub effect_param1: i32,
    pub color_hsl: i32,
    pub texture_speed_u: i32,
    pub texture_speed_v: i32,
    pub a_bool2087: bool,
    pub is_brick_tile: bool,
    pub mipmapping: i32,
    pub repeat_s: bool,
    pub repeat_t: bool,
    pub hdr: bool,
    pub combine_mode: i32,
    pub effect_param2: i32,
    pub effect_combiner: i32,
}

impl Material {
    pub fn is_opaque_effect(&self) -> bool {
        self.effect_id != 1 && self.effect_id != 7
    }

    pub fn size(&self) -> i32 {
        if self.is_small {
            64
        } else {
            128
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MaterialList {
    pub materials: Vec<Option<Material>>,
}

impl MaterialList {
    pub fn decode(data: &[u8]) -> Result<MaterialList, PacketError> {
        let mut packet = Packet::new(data);
        let count = packet.read_u16()? as usize;
        let mut materials: Vec<Option<Material>> = Vec::with_capacity(count);
        for _ in 0..count {
            let present = packet.read_u8()? == 1;
            materials.push(if present { Some(Material::default()) } else { None });
        }
        fn field<F: FnMut(&mut Material, &mut Packet) -> Result<(), PacketError>>(
            materials: &mut [Option<Material>],
            packet: &mut Packet,
            mut apply: F,
        ) -> Result<(), PacketError> {
            for material in materials.iter_mut().flatten() {
                apply(material, packet)?;
            }
            Ok(())
        }
        field(&mut materials, &mut packet, |m, p| Ok(m.details_only = p.read_u8()? == 0))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.is_small = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.skip_triangles = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.brightness = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.alpha = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.effect_id = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.effect_param1 = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.color_hsl = p.read_i16()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.texture_speed_u = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.texture_speed_v = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.a_bool2087 = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.is_brick_tile = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.mipmapping = p.read_i8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.repeat_s = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.repeat_t = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.hdr = p.read_u8()? == 1))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.combine_mode = p.read_u8()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.effect_param2 = p.read_i32()?))?;
        field(&mut materials, &mut packet, |m, p| Ok(m.effect_combiner = p.read_u8()?))?;
        Ok(MaterialList { materials })
    }

    pub fn get(&self, id: i32) -> Option<&Material> {
        if id < 0 {
            return None;
        }
        self.materials.get(id as usize).and_then(|m| m.as_ref())
    }
}
