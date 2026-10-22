use super::writer::{ClientPacketError, Writer};
use crate::registry::ClientPacket;

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MenuContext {
    pub run: bool,
    pub base_x: i32,
    pub base_y: i32,
    pub selected_hash: i32,
    pub selected_item: i32,
    pub selected_slot: i32,
    pub own_player_index: i32,
    pub camera_yaw: i32,
    pub minimap_rotation: i32,
    pub minimap_zoom: i32,
    pub absolute_x: i32,
    pub absolute_z: i32,
    pub container_item: i32,
    pub target_hash: i32,
    pub target_slot: i32,
    pub target_item: i32,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MenuClick {
    pub click_type: i32,
    pub entity_uid: i64,
    pub param0: i32,
    pub param1: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ClientBody {
    pub packet: ClientPacket,
    pub body: Vec<u8>,
}

const PLAYER_OPS: [ClientPacket; 10] = [
    ClientPacket::PLAYER_OP1,
    ClientPacket::PLAYER_OP2,
    ClientPacket::PLAYER_OP3,
    ClientPacket::PLAYER_OP4,
    ClientPacket::PLAYER_OP5,
    ClientPacket::PLAYER_OP6,
    ClientPacket::PLAYER_OP7,
    ClientPacket::PLAYER_OP8,
    ClientPacket::PLAYER_OP9,
    ClientPacket::PLAYER_OP10,
];

const NPC_OPS: [ClientPacket; 5] = [
    ClientPacket::NPC_OP1,
    ClientPacket::NPC_OP2,
    ClientPacket::NPC_OP3,
    ClientPacket::NPC_OP4,
    ClientPacket::NPC_OP5,
];

const GROUND_ITEM_OPS: [ClientPacket; 5] = [
    ClientPacket::GROUND_ITEM_OP1,
    ClientPacket::GROUND_ITEM_OP2,
    ClientPacket::GROUND_ITEM_OP3,
    ClientPacket::GROUND_ITEM_OP4,
    ClientPacket::GROUND_ITEM_OP5,
];

const OBJECT_OPS: [ClientPacket; 5] = [
    ClientPacket::OBJECT_OP1,
    ClientPacket::OBJECT_OP2,
    ClientPacket::OBJECT_OP3,
    ClientPacket::OBJECT_OP4,
    ClientPacket::OBJECT_OP5,
];

const IF_OPS: [ClientPacket; 10] = [
    ClientPacket::IF_OP1,
    ClientPacket::IF_OP2,
    ClientPacket::IF_OP3,
    ClientPacket::IF_OP4,
    ClientPacket::IF_OP5,
    ClientPacket::IF_OP6,
    ClientPacket::IF_OP7,
    ClientPacket::IF_OP8,
    ClientPacket::IF_OP9,
    ClientPacket::IF_OP10,
];

fn done(packet: ClientPacket, writer: Writer) -> Result<Option<ClientBody>, ClientPacketError> {
    Ok(Some(ClientBody {
        packet,
        body: writer.into_bytes(),
    }))
}

pub fn walk_body(run: bool, x: i32, y: i32, ctx: &MenuContext) -> Vec<u8> {
    let mut w = Writer::new();
    w.byte(i32::from(run))
        .short(x.wrapping_add(ctx.base_x))
        .short_little(y.wrapping_add(ctx.base_y));
    w.into_bytes()
}

pub fn walk_to_tile(x: i32, y: i32, movement_type: i32, ctx: &MenuContext) -> Option<ClientBody> {
    let packet = match movement_type {
        0 => ClientPacket::WALK,
        1 => ClientPacket::MINI_WALK,
        _ => return None,
    };
    let mut body = walk_body(ctx.run, x, y, ctx);
    if movement_type == 1 {
        let mut w = Writer(body);
        w.byte(-1)
            .byte(-1)
            .short(ctx.camera_yaw)
            .byte(57)
            .byte(ctx.minimap_rotation)
            .byte(ctx.minimap_zoom)
            .byte(89)
            .short(ctx.absolute_x)
            .short(ctx.absolute_z)
            .byte(63);
        body = w.into_bytes();
    }
    Some(ClientBody { packet, body })
}

pub fn interface_component_data(hash: i32, slot: i32, item: i32) -> Writer {
    let mut w = Writer::new();
    w.int_little(hash).short(item).short_add(slot);
    w
}

pub fn if_option(option: i32, hash: i32, slot: i32, item: i32) -> Result<ClientBody, ClientPacketError> {
    let packet = usize::try_from(option - 1)
        .ok()
        .and_then(|i| IF_OPS.get(i).copied())
        .ok_or(ClientPacketError::Option(option))?;
    Ok(ClientBody {
        packet,
        body: interface_component_data(hash, slot, item).into_bytes(),
    })
}

pub fn if_continue(hash: i32, slot: i32) -> ClientBody {
    let mut w = Writer::new();
    w.int_middle(hash).short_add_little(slot);
    ClientBody {
        packet: ClientPacket::IF_CONTINUE,
        body: w.into_bytes(),
    }
}

pub fn if_on_if(ctx: &MenuContext) -> ClientBody {
    let mut w = Writer::new();
    w.short_add_little(ctx.target_slot)
        .short_little(ctx.selected_slot)
        .short_add_little(ctx.target_item)
        .int_little(ctx.target_hash)
        .int_inverse_middle(ctx.selected_hash)
        .short_little(ctx.selected_item);
    ClientBody {
        packet: ClientPacket::IF_ON_IF,
        body: w.into_bytes(),
    }
}

pub fn if_drag_onto_if(
    source_slot: i32,
    source_item: i32,
    source_hash: i32,
    ctx: &MenuContext,
) -> ClientBody {
    let mut w = Writer::new();
    w.short_add_little(ctx.target_slot)
        .short_little(source_slot)
        .short(ctx.target_item)
        .short_add_little(source_item)
        .int_middle(source_hash)
        .int_little(ctx.target_hash);
    ClientBody {
        packet: ClientPacket::IF_DRAG_ONTO_IF,
        body: w.into_bytes(),
    }
}

pub fn if_on_player_direct(player_index: i32, ctx: &MenuContext) -> ClientBody {
    let mut w = Writer::new();
    w.short(ctx.selected_slot)
        .short_little(player_index)
        .byte_subtract(0)
        .int_inverse_middle(ctx.selected_hash)
        .short_little(ctx.selected_item);
    ClientBody {
        packet: ClientPacket::IF_ON_PLAYER,
        body: w.into_bytes(),
    }
}

pub fn player_operation(op: i32, player_index: i32) -> Option<ClientBody> {
    let packet = match op {
        1 => ClientPacket::PLAYER_OP1,
        4 => ClientPacket::PLAYER_OP4,
        5 => ClientPacket::PLAYER_OP5,
        6 => ClientPacket::PLAYER_OP6,
        7 => ClientPacket::PLAYER_OP7,
        9 => ClientPacket::PLAYER_OP9,
        _ => return None,
    };
    let mut w = Writer::new();
    w.short(player_index).byte_subtract(0);
    Some(ClientBody {
        packet,
        body: w.into_bytes(),
    })
}

pub fn process_menuclick(
    click: &MenuClick,
    ctx: &MenuContext,
) -> Result<Option<ClientBody>, ClientPacketError> {
    let click_type = if click.click_type >= 2000 {
        click.click_type - 2000
    } else {
        click.click_type
    };
    let uid = click.entity_uid as i32;
    let attr1 = click.param0;
    let attr2 = click.param1;
    let bitpacked = click.entity_uid;
    let run = i32::from(ctx.run);
    let object_hash = ((bitpacked as u64 >> 32) as u32 as i32) & 0x7fff_ffff;
    let (bx, by) = (ctx.base_x, ctx.base_y);
    let mut w = Writer::new();
    match click_type {
        44..=53 => {
            w.short(uid).byte_subtract(run);
            done(PLAYER_OPS[(click_type - 44) as usize], w)
        }
        16 => {
            w.short(ctx.selected_slot)
                .short_little(ctx.own_player_index)
                .byte_subtract(run)
                .int_inverse_middle(ctx.selected_hash)
                .short_little(ctx.selected_item);
            done(ClientPacket::IF_ON_PLAYER, w)
        }
        15 => {
            w.short(ctx.selected_slot)
                .short_little(uid)
                .byte_subtract(run)
                .int_inverse_middle(ctx.selected_hash)
                .short_little(ctx.selected_item);
            done(ClientPacket::IF_ON_PLAYER, w)
        }
        2 => {
            w.short_add_little(attr1.wrapping_add(bx))
                .byte_subtract(run)
                .int_middle(object_hash)
                .int(ctx.selected_hash)
                .short_little(ctx.selected_item)
                .short_add(ctx.selected_slot)
                .short_little(attr2.wrapping_add(by));
            done(ClientPacket::IF_ON_OBJECT, w)
        }
        9..=13 => {
            w.short(uid).byte(run);
            done(NPC_OPS[(click_type - 9) as usize], w)
        }
        1003 => {
            w.short(uid).byte(run);
            done(ClientPacket::NPC_EXAMINE, w)
        }
        60 => {
            w.short_add(attr1.wrapping_add(bx))
                .short(attr2.wrapping_add(by));
            done(ClientPacket::UNK_63, w)
        }
        23 => Ok(walk_to_tile(attr1, attr2, uid, ctx)),
        17 => {
            w.short_add(ctx.selected_item)
                .int_inverse_middle(ctx.selected_hash)
                .short(uid)
                .byte_subtract(run)
                .short_add_little(ctx.selected_slot)
                .short_add_little(attr2.wrapping_add(by))
                .short_little(attr1.wrapping_add(bx));
            done(ClientPacket::IF_ON_GROUND_ITEM, w)
        }
        59 => {
            w.short_little(ctx.selected_item)
                .short_little(attr2.wrapping_add(by))
                .int_middle(ctx.selected_hash)
                .short_add(ctx.selected_slot)
                .short_little(attr1.wrapping_add(bx));
            done(ClientPacket::IF_ON_TILE, w)
        }
        30 => Ok(Some(if_continue(attr2, attr1))),
        58 => Ok(Some(if_on_if(ctx))),
        18..=22 | 1004 => {
            w.short_add_little(uid)
                .byte_inverse(run)
                .short(attr2.wrapping_add(by))
                .short_add(attr1.wrapping_add(bx));
            let packet = if click_type == 1004 {
                ClientPacket::GROUND_ITEM_EXAMINE
            } else {
                GROUND_ITEM_OPS[(click_type - 18) as usize]
            };
            done(packet, w)
        }
        8 => {
            w.int_inverse_middle(ctx.selected_hash)
                .short_add_little(uid)
                .byte_subtract(run)
                .short_add_little(ctx.selected_item)
                .short_add(ctx.selected_slot);
            done(ClientPacket::IF_ON_NPC, w)
        }
        3..=6 | 1001 | 1002 => {
            w.short(attr2.wrapping_add(by))
                .short(attr1.wrapping_add(bx))
                .int(object_hash)
                .byte_add(run);
            let packet = match click_type {
                1001 => ClientPacket::OBJECT_OP5,
                1002 => ClientPacket::OBJECT_EXAMINE,
                _ => OBJECT_OPS[(click_type - 3) as usize],
            };
            done(packet, w)
        }
        57 | 1007 => Ok(if_option(uid, attr2, attr1, ctx.container_item).ok()),
        _ => Ok(None),
    }
}
