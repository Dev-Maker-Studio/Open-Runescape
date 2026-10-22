use super::{InfoError, InfoResult};
use crate::payload::PayloadReader;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct ItemShape {
    pub team_id: i32,
    pub has_body2: bool,
    pub has_body3: bool,
    pub has_head2: bool,
    pub recolors: Option<usize>,
    pub retextures: Option<usize>,
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct ItemEffects {
    pub body: Vec<(i32, i32)>,
    pub head: Vec<(i32, i32)>,
    pub recolors: Vec<(u8, u16)>,
    pub retextures: Vec<(u8, u16)>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WornSlot {
    Empty,
    Kit(i32),
    Item(i32),
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Appearance {
    pub male: bool,
    pub instrument_sound: bool,
    pub show_elo: bool,
    pub size: u8,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
    pub skull: i8,
    pub prayer_icon: i8,
    pub hidden: bool,
    pub morph: i32,
    pub team: i32,
    pub slots: Vec<WornSlot>,
    pub effects: Vec<Option<ItemEffects>>,
    pub static_recolors: [u8; 10],
    pub render_emote: u16,
    pub username: String,
    pub combat_level: u8,
    pub elo: i32,
    pub summoning_combat_level: u8,
    pub related_level: i32,
    pub is_npc: u8,
    pub sound_ids: [u16; 4],
    pub ambient_volume: u8,
}

pub fn decode_item_effects(
    shape: &ItemShape,
    reader: &mut PayloadReader<'_>,
) -> InfoResult<ItemEffects> {
    let flags = reader.read_u8()?;
    let mut effects = ItemEffects::default();
    if flags & 0x1 != 0 {
        let male = reader.read_big_smart()?;
        let female = reader.read_big_smart()?;
        effects.body.push((male, female));
        if shape.has_body2 {
            let male = reader.read_big_smart()?;
            let female = reader.read_big_smart()?;
            effects.body.push((male, female));
        }
        if shape.has_body3 {
            let male = reader.read_big_smart()?;
            let female = reader.read_big_smart()?;
            effects.body.push((male, female));
        }
    }
    if flags & 0x2 != 0 {
        let male = reader.read_big_smart()?;
        let female = reader.read_big_smart()?;
        effects.head.push((male, female));
        if shape.has_head2 {
            let male = reader.read_big_smart()?;
            let female = reader.read_big_smart()?;
            effects.head.push((male, female));
        }
    }
    if flags & 0x4 != 0 {
        let packed = reader.read_u16_be()?;
        for shift in [0u16, 4, 8, 12] {
            let target = ((packed >> shift) & 0xf) as u8;
            if target != 15 {
                let len = shape.recolors.ok_or(InfoError::ItemEffectTarget(0x4))?;
                let value = reader.read_u16_be()?;
                if usize::from(target) >= len {
                    return Err(InfoError::ItemEffectSlot { slot: target, len });
                }
                effects.recolors.push((target, value));
            }
        }
    }
    if flags & 0x8 != 0 {
        let packed = reader.read_u8()?;
        for shift in [0u8, 4] {
            let target = (packed >> shift) & 0xf;
            if target != 15 {
                let len = shape.retextures.ok_or(InfoError::ItemEffectTarget(0x8))?;
                let value = reader.read_u16_be()?;
                if usize::from(target) >= len {
                    return Err(InfoError::ItemEffectSlot { slot: target, len });
                }
                effects.retextures.push((target, value));
            }
        }
    }
    Ok(effects)
}

pub fn decode_appearance(
    bytes: &[u8],
    customizable_slots: &[i32],
    items: &dyn Fn(i32) -> Option<ItemShape>,
) -> InfoResult<Appearance> {
    let mut reader = PayloadReader::new(bytes);
    let flags = reader.read_u8()?;
    let mut appearance = Appearance {
        male: flags & 0x1 != 0,
        instrument_sound: flags & 0x2 != 0,
        show_elo: flags & 0x4 != 0,
        size: ((flags >> 3) & 0x7) + 1,
        morph: -1,
        ..Appearance::default()
    };
    if flags & 0x40 != 0 {
        appearance.prefix_title = Some(reader.read_jag_cp1252()?);
    }
    if flags & 0x80 != 0 {
        appearance.postfix_title = Some(reader.read_jag_cp1252()?);
    }
    appearance.skull = reader.read_i8()?;
    appearance.prayer_icon = reader.read_i8()?;
    appearance.hidden = reader.read_i8()? == 1;

    let count = customizable_slots.len();
    let mut worn_shapes: Vec<Option<ItemShape>> = vec![None; count];
    appearance.slots = vec![WornSlot::Empty; count];
    appearance.effects = vec![None; count];

    for slot in 0..count {
        if customizable_slots[slot] == 1 {
            continue;
        }
        let high = i32::from(reader.read_u8()?);
        if high == 0 {
            continue;
        }
        let low = i32::from(reader.read_u8()?);
        let mut item_id = low + (high << 8);
        if slot == 0 && item_id == 65535 {
            appearance.morph = reader.read_big_smart()?;
            appearance.team = i32::from(reader.read_u8()?);
            break;
        }
        if item_id >= 16384 {
            item_id -= 16384;
            let shape = items(item_id).ok_or(InfoError::UnknownItem(item_id))?;
            if shape.team_id != 0 {
                appearance.team = shape.team_id;
            }
            worn_shapes[slot] = Some(shape);
            appearance.slots[slot] = WornSlot::Item(item_id);
        } else {
            appearance.slots[slot] = WornSlot::Kit(item_id - 256);
        }
    }

    if appearance.morph == -1 {
        let mask = u32::from(reader.read_u16_be()?);
        let mut bit = 0u32;
        for slot in 0..count {
            if customizable_slots[slot] == 0 {
                if mask & (1 << (bit % 32)) != 0 {
                    let shape = worn_shapes[slot].ok_or(InfoError::ItemNotWorn(slot))?;
                    appearance.effects[slot] = Some(decode_item_effects(&shape, &mut reader)?);
                }
                bit += 1;
            }
        }
    }

    for slot in 0..10 {
        appearance.static_recolors[slot] = reader.read_u8()?;
    }
    appearance.render_emote = reader.read_u16_be()?;
    appearance.username = reader.read_cp1252()?;
    appearance.combat_level = reader.read_u8()?;
    if appearance.show_elo {
        let elo = reader.read_u16_be()?;
        appearance.elo = if elo == 65535 { -1 } else { i32::from(elo) };
        appearance.summoning_combat_level = appearance.combat_level;
        appearance.related_level = -1;
    } else {
        appearance.elo = 0;
        appearance.summoning_combat_level = reader.read_u8()?;
        let related = reader.read_u8()?;
        appearance.related_level = if related == 255 { -1 } else { i32::from(related) };
    }
    appearance.is_npc = reader.read_u8()?;
    if appearance.is_npc != 0 {
        for id in &mut appearance.sound_ids {
            *id = reader.read_u16_be()?;
        }
        appearance.ambient_volume = reader.read_u8()?;
    }
    Ok(appearance)
}
