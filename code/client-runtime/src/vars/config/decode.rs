use openrs_cs2::{
    ContainerType, EnumValue, InteractionColours, ItemDefinition, ParamDefinition, ParamTable,
};
use openrs_scene::packet::{Packet, PacketError};
use std::sync::Arc;

pub const DEFAULT_GROUND_ACTIONS: [Option<&str>; 6] =
    [None, None, Some("Take"), None, None, Some("Examine")];
pub const DEFAULT_INVENTORY_ACTIONS: [Option<&str>; 5] = [None, None, None, None, Some("Drop")];
pub const DISCARD: &str = "Discard";

pub type DecodeResult<T> = Result<T, PacketError>;

fn text(value: Option<&str>) -> Option<Arc<str>> {
    value.map(Arc::from)
}

fn parameters(packet: &mut Packet, table: &mut Option<ParamTable>) -> DecodeResult<()> {
    let count = packet.u8()?;
    let table = table.get_or_insert_with(ParamTable::new);
    for _ in 0..count {
        let is_string = packet.u8()? == 1;
        let key = packet.u24()?;
        let value = if is_string {
            EnumValue::Text(Arc::from(packet.string()?))
        } else {
            EnumValue::Int(packet.i32()?)
        };
        table.entry(key).or_insert(value);
    }
    Ok(())
}

fn skip(packet: &mut Packet, bytes: usize) -> DecodeResult<()> {
    for _ in 0..bytes {
        packet.u8()?;
    }
    Ok(())
}

pub fn decode_param(bytes: &[u8]) -> DecodeResult<ParamDefinition> {
    let mut packet = Packet::new(bytes);
    let mut definition = ParamDefinition::default();
    loop {
        match packet.u8()? {
            0 => return Ok(definition),
            1 => definition.is_string = packet.u8()? == i32::from(b's'),
            2 => definition.default_int = packet.i32()?,
            5 => definition.type_name = Arc::from(packet.string()?),
            _ => {}
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct RawItem {
    pub definition: ItemDefinition,
    pub tradeable: bool,
    pub unknown_int6: i32,
    pub lend_id: i32,
    pub lend_template_id: i32,
    pub bind_id: i32,
    pub bind_template_id: i32,
}

impl Default for RawItem {
    fn default() -> Self {
        Self {
            definition: ItemDefinition {
                ground_actions: DEFAULT_GROUND_ACTIONS
                    .iter()
                    .map(|action| text(*action))
                    .collect(),
                inventory_actions: DEFAULT_INVENTORY_ACTIONS
                    .iter()
                    .map(|action| text(*action))
                    .collect(),
                ..ItemDefinition::default()
            },
            tradeable: false,
            unknown_int6: 0,
            lend_id: -1,
            lend_template_id: -1,
            bind_id: -1,
            bind_template_id: -1,
        }
    }
}

pub fn decode_item(bytes: &[u8]) -> DecodeResult<RawItem> {
    let mut packet = Packet::new(bytes);
    let mut raw = RawItem::default();
    loop {
        let opcode = packet.u8()?;
        let item = &mut raw.definition;
        match opcode {
            0 => return Ok(raw),
            1 | 23..=26 | 78 | 79 | 90..=93 => {
                packet.big_smart()?;
            }
            2 => item.name = Arc::from(packet.string()?),
            4..=8 | 95 | 110..=112 => {
                packet.u16()?;
            }
            11 => item.stackable = 1,
            12 => item.value = packet.i32()?,
            13 => item.wear_pos = packet.u8()?,
            14 => item.wear_pos2 = packet.u8()?,
            16 => item.members_only = true,
            18 => item.multi_stack_size = packet.u16()?,
            27 => item.wear_pos3 = packet.u8()?,
            30..=34 => {
                item.ground_actions[(opcode - 30) as usize] = Some(Arc::from(packet.string()?))
            }
            35..=39 => {
                item.inventory_actions[(opcode - 35) as usize] = Some(Arc::from(packet.string()?))
            }
            40 | 41 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 4)?;
            }
            42 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize)?;
            }
            43 => {
                item.tooltip_color = packet.i32()?;
                item.has_tooltip_color = true;
            }
            65 => raw.tradeable = true,
            96 => raw.unknown_int6 = packet.u8()?,
            97 => item.cert_id = packet.u16()?,
            98 => item.cert_template_id = packet.u16()?,
            100..=109 => skip(&mut packet, 4)?,
            113..=115 => {
                packet.u8()?;
            }
            121 => raw.lend_id = packet.u16()?,
            122 => raw.lend_template_id = packet.u16()?,
            125 | 126 => skip(&mut packet, 3)?,
            127 | 128 => skip(&mut packet, 3)?,
            129 => {
                item.cursor_op1 = packet.u8()?;
                item.cursor_id1 = packet.u16()?;
            }
            130 => {
                item.cursor_op2 = packet.u8()?;
                item.cursor_id2 = packet.u16()?;
            }
            132 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 2)?;
            }
            134 => {
                packet.u8()?;
            }
            139 => raw.bind_id = packet.u16()?,
            140 => raw.bind_template_id = packet.u16()?,
            249 => parameters(&mut packet, &mut item.params)?,
            _ => {}
        }
    }
}

pub fn derive_note(item: &mut ItemDefinition, original: &ItemDefinition) {
    item.name = Arc::clone(&original.name);
    item.members_only = original.members_only;
    item.value = original.value;
    item.stackable = 1;
}

pub fn derive_lend(item: &mut ItemDefinition, original: &ItemDefinition) {
    derive_exchange(item, original);
}

pub fn derive_bind(item: &mut ItemDefinition, original: &ItemDefinition) {
    derive_exchange(item, original);
    item.stackable = original.stackable;
}

fn derive_exchange(item: &mut ItemDefinition, original: &ItemDefinition) {
    item.value = 0;
    item.name = Arc::clone(&original.name);
    item.members_only = original.members_only;
    item.wear_pos = original.wear_pos;
    item.wear_pos2 = original.wear_pos2;
    item.wear_pos3 = original.wear_pos3;
    item.ground_actions = original.ground_actions.clone();
    item.params = original.params.clone();
    let mut actions: Vec<Option<Arc<str>>> = vec![None; 5];
    for (index, action) in original.inventory_actions.iter().take(4).enumerate() {
        actions[index] = action.clone();
    }
    actions[4] = Some(Arc::from(DISCARD));
    item.inventory_actions = actions;
}

pub fn decode_params_only(bytes: &[u8]) -> DecodeResult<Option<ParamTable>> {
    let mut packet = Packet::new(bytes);
    let mut table = None;
    loop {
        match packet.u8()? {
            0 => return Ok(table),
            249 => parameters(&mut packet, &mut table)?,
            _ => {}
        }
    }
}

pub fn decode_npc_params(bytes: &[u8]) -> DecodeResult<Option<ParamTable>> {
    let mut packet = Packet::new(bytes);
    let mut table = None;
    loop {
        let opcode = packet.u8()?;
        match opcode {
            0 => return Ok(table),
            1 | 60 => {
                let count = packet.u8()?;
                for _ in 0..count {
                    packet.big_smart()?;
                }
            }
            2 | 30..=34 | 150..=154 => {
                packet.string()?;
            }
            12 | 128 | 140 | 163 | 165 | 168 => {
                packet.u8()?;
            }
            40 | 41 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 4)?;
            }
            42 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize)?;
            }
            95 | 97 | 98 | 102 | 103 | 123 | 127 | 137 | 142 => {
                packet.u16()?;
            }
            100 | 101 | 119 | 125 => {
                packet.u8()?;
            }
            106 | 118 => {
                skip(&mut packet, 4)?;
                if opcode == 118 {
                    skip(&mut packet, 2)?;
                }
                let length = packet.u8()?;
                skip(&mut packet, (length as usize + 1) * 2)?;
            }
            113 | 164 => skip(&mut packet, 4)?,
            114 => skip(&mut packet, 2)?,
            121 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 4)?;
            }
            134 => skip(&mut packet, 9)?,
            135 | 136 => skip(&mut packet, 3)?,
            138 => {
                packet.big_smart()?;
            }
            155 => skip(&mut packet, 4)?,
            160 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 2)?;
            }
            249 => parameters(&mut packet, &mut table)?,
            _ => {}
        }
    }
}

pub fn decode_seq_params(bytes: &[u8]) -> DecodeResult<Option<ParamTable>> {
    let mut packet = Packet::new(bytes);
    let mut table = None;
    loop {
        match packet.u8()? {
            0 => return Ok(table),
            1 => {
                let count = packet.u16()?;
                skip(&mut packet, count as usize * 6)?;
            }
            2 | 6 | 7 => {
                packet.u16()?;
            }
            3 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize)?;
            }
            5 | 8 | 9 | 10 | 11 => {
                packet.u8()?;
            }
            12 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize * 4)?;
            }
            13 => {
                let frames = packet.u16()?;
                for _ in 0..frames {
                    let sounds = packet.u8()?;
                    if sounds > 0 {
                        packet.u24()?;
                        skip(&mut packet, (sounds as usize - 1) * 2)?;
                    }
                }
            }
            19 => skip(&mut packet, 2)?,
            20 => skip(&mut packet, 5)?,
            249 => parameters(&mut packet, &mut table)?,
            _ => {}
        }
    }
}

pub fn decode_container_type(bytes: &[u8]) -> DecodeResult<ContainerType> {
    let mut packet = Packet::new(bytes);
    let mut container = ContainerType::default();
    loop {
        match packet.u8()? {
            0 => return Ok(container),
            2 => container.max_item_size = packet.u16()?,
            4 => {
                let size = packet.u8()?;
                container.default_item_size = size;
                container.item_ids.clear();
                container.amounts.clear();
                for _ in 0..size {
                    container.item_ids.push(packet.u16()?);
                    container.amounts.push(packet.u16()?);
                }
            }
            _ => {}
        }
    }
}

fn input_subscriber(packet: &mut Packet) -> DecodeResult<()> {
    match packet.u8()? {
        0 => {
            skip(packet, 2)?;
            let count = packet.u8()?;
            skip(packet, count as usize)
        }
        1 => skip(packet, 2),
        2 => {
            let count = packet.u8()?;
            skip(packet, count as usize)
        }
        _ => Ok(()),
    }
}

pub fn decode_interaction_colours(bytes: &[u8]) -> DecodeResult<InteractionColours> {
    let mut packet = Packet::new(bytes);
    let mut colours = InteractionColours::default();
    loop {
        match packet.u8()? {
            0 => return Ok(colours),
            1..=4 | 8..=10 => input_subscriber(&mut packet)?,
            5..=7 => {
                let count = packet.u8()?;
                skip(&mut packet, count as usize)?;
            }
            12 => colours.members = packet.i32()?,
            13 => colours.free = packet.i32()?,
            _ => {}
        }
    }
}
