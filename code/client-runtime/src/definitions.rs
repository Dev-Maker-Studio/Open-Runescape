use crate::world_resources::{Lookup, ResourceStore};
use openrs_entity::config::{ItemType, NpcType};
use openrs_scene::loc_type::decode_loc_type;
use serde_json::{Value, json};

pub const KIND_LOC: u8 = 0;
pub const KIND_ITEM: u8 = 1;
pub const KIND_NPC: u8 = 2;

#[derive(Debug, Clone, PartialEq)]
pub enum Definition {
    Pending,
    Missing,
    Ready(Value),
}

impl Definition {
    pub fn to_json(&self) -> String {
        match self {
            Definition::Pending => String::new(),
            Definition::Missing => "null".to_string(),
            Definition::Ready(value) => value.to_string(),
        }
    }
}

fn text(bytes: &[u8]) -> String {
    bytes.iter().map(|byte| char::from(*byte)).collect()
}

fn options(list: &[Option<Vec<u8>>; 5]) -> Vec<Option<String>> {
    list.iter()
        .map(|entry| entry.as_ref().map(|bytes| text(bytes)))
        .collect()
}

pub fn loc_definition(store: &ResourceStore, id: i32) -> Definition {
    match store.loc_type(id) {
        Lookup::Unknown => Definition::Pending,
        Lookup::Absent => Definition::Missing,
        Lookup::Present(bytes) => match decode_loc_type(id, Some(&bytes), true) {
            Err(_) => Definition::Missing,
            Ok(loc) => Definition::Ready(json!({
                "name": loc.name,
                "actions": loc.actions.unwrap_or_default(),
                "primaryCursorIndex": loc.primary_cursor_action_index,
                "primaryCursor": loc.primary_cursor,
                "secondaryCursorIndex": loc.secondary_cursor_action_index,
                "secondaryCursor": loc.secondary_cursor,
                "alternativeIds": loc.alternative_ids,
            })),
        },
    }
}

pub fn npc_definition(store: &ResourceStore, id: i32) -> Definition {
    match store.npc_type(id) {
        Lookup::Unknown => Definition::Pending,
        Lookup::Absent => Definition::Missing,
        Lookup::Present(bytes) => match NpcType::decode(id, &bytes) {
            Err(_) => Definition::Missing,
            Ok(npc) => Definition::Ready(json!({
                "name": text(&npc.name),
                "combatLevel": npc.combat_level,
                "visible": npc.visible,
                "gameType": npc.game_type,
                "actions": options(&npc.options),
                "primaryCursorIndex": -1,
                "primaryCursor": -1,
                "secondaryCursorIndex": -1,
                "secondaryCursor": -1,
                "alternativeIds": npc.alternative_ids,
            })),
        },
    }
}

pub fn item_definition(store: &ResourceStore, id: i32) -> Definition {
    match store.item_type(id) {
        Lookup::Unknown => Definition::Pending,
        Lookup::Absent => Definition::Missing,
        Lookup::Present(bytes) => match ItemType::decode(id, &bytes) {
            Err(_) => Definition::Missing,
            Ok(item) => Definition::Ready(json!({
                "name": text(&item.name),
                "actions": options(&item.ground_options),
                "primaryCursorIndex": -1,
                "primaryCursor": -1,
                "secondaryCursorIndex": -1,
                "secondaryCursor": -1,
            })),
        },
    }
}

pub fn definition(store: &ResourceStore, kind: u8, id: i32) -> Option<Definition> {
    match kind {
        KIND_LOC => Some(loc_definition(store, id)),
        KIND_ITEM => Some(item_definition(store, id)),
        KIND_NPC => Some(npc_definition(store, id)),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::world_resources::{INDEX_ITEM, INDEX_NPC};

    fn npc_bytes() -> Vec<u8> {
        let mut bytes = vec![2];
        bytes.extend_from_slice(b"Guard\0");
        bytes.push(30);
        bytes.extend_from_slice(b"Talk-to\0");
        bytes.push(152);
        bytes.extend_from_slice(b"Attack\0");
        bytes.push(0);
        bytes
    }

    fn item_bytes() -> Vec<u8> {
        let mut bytes = vec![2];
        bytes.extend_from_slice(b"Coins\0");
        bytes.push(32);
        bytes.extend_from_slice(b"Take\0");
        bytes.push(35);
        bytes.extend_from_slice(b"Drop\0");
        bytes.push(0);
        bytes
    }

    #[test]
    fn unknown_records_a_need_and_stays_pending() {
        let store = ResourceStore::new();
        assert_eq!(npc_definition(&store, 130), Definition::Pending);
        assert_eq!(store.needs(), vec![(INDEX_NPC, 1)]);
        assert_eq!(Definition::Pending.to_json(), "");
    }

    #[test]
    fn absent_archive_is_missing() {
        let store = ResourceStore::new();
        store.provide_absent(INDEX_ITEM, 0);
        assert_eq!(item_definition(&store, 5), Definition::Missing);
        assert_eq!(Definition::Missing.to_json(), "null");
    }

    #[test]
    fn npc_options_come_from_both_opcode_ranges() {
        let store = ResourceStore::new();
        store.provide_file(INDEX_NPC, 1, 2, npc_bytes());
        let Definition::Ready(value) = npc_definition(&store, 130) else {
            panic!("npc definition not ready");
        };
        assert_eq!(value["name"], "Guard");
        assert_eq!(value["actions"][0], "Talk-to");
        assert_eq!(value["actions"][2], "Attack");
        assert!(value["actions"][1].is_null());
    }

    #[test]
    fn ground_item_options_exclude_inventory_options() {
        let store = ResourceStore::new();
        let id = 995;
        let archive = (id >> 8) as u32;
        let file = (id & 0xff) as u32;
        store.provide_file(INDEX_ITEM, archive, file, item_bytes());
        let Definition::Ready(value) = item_definition(&store, id) else {
            panic!("item definition not ready");
        };
        assert_eq!(value["name"], "Coins");
        assert_eq!(value["actions"][2], "Take");
        assert_eq!(value["actions"].as_array().unwrap().len(), 5);
        assert!(!value["actions"].to_string().contains("Drop"));
    }

    #[test]
    fn unsupported_kind_is_none() {
        assert!(definition(&ResourceStore::new(), 9, 1).is_none());
    }
}
