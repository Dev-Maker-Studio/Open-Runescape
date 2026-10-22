use openrs_scene::packet::{Packet, PacketError};

pub const IDK_GROUP: u32 = 3;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct IdkDefinition {
    pub body_models: Option<Vec<i32>>,
    pub recolor: Option<Vec<(i16, i16)>>,
    pub retexture: Option<Vec<(i16, i16)>>,
    pub head_models: [i32; 5],
}

fn pairs(packet: &mut Packet) -> Result<Vec<(i16, i16)>, PacketError> {
    let count = packet.u8()?;
    let mut out = Vec::with_capacity(count as usize);
    for _ in 0..count {
        let source = packet.u16()? as i16;
        let destination = packet.u16()? as i16;
        out.push((source, destination));
    }
    Ok(out)
}

pub fn decode_idk(bytes: &[u8]) -> Result<IdkDefinition, PacketError> {
    let mut packet = Packet::new(bytes);
    let mut definition = IdkDefinition {
        head_models: [-1; 5],
        ..IdkDefinition::default()
    };
    loop {
        let opcode = packet.u8()?;
        match opcode {
            0 => return Ok(definition),
            1 => {
                packet.u8()?;
            }
            2 => {
                let count = packet.u8()?;
                let mut models = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    models.push(packet.big_smart()?);
                }
                definition.body_models = Some(models);
            }
            40 => definition.recolor = Some(pairs(&mut packet)?),
            41 => definition.retexture = Some(pairs(&mut packet)?),
            60..=69 => definition.head_models[(opcode - 60) as usize] = packet.big_smart()?,
            _ => {}
        }
    }
}
