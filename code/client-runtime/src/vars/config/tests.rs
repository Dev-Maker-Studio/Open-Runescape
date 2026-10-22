use super::decode::*;
use openrs_cs2::{EnumValue, ParamTable};
use std::sync::Arc;

fn text(value: &str) -> Vec<u8> {
    let mut bytes = value.as_bytes().to_vec();
    bytes.push(0);
    bytes
}

fn parameters(entries: &[(i32, EnumValue)]) -> Vec<u8> {
    let mut bytes = vec![249, entries.len() as u8];
    for (key, value) in entries {
        match value {
            EnumValue::Int(number) => {
                bytes.push(0);
                bytes.extend_from_slice(&key.to_be_bytes()[1..]);
                bytes.extend_from_slice(&number.to_be_bytes());
            }
            EnumValue::Text(string) => {
                bytes.push(1);
                bytes.extend_from_slice(&key.to_be_bytes()[1..]);
                bytes.extend(text(string));
            }
        }
    }
    bytes
}

#[test]
fn params_decode_type_default_and_name() {
    let mut bytes = vec![1, b's', 2, 0, 0, 0, 9, 4];
    bytes.push(5);
    bytes.extend(text("title"));
    bytes.push(0);
    let definition = decode_param(&bytes).unwrap();
    assert!(definition.is_string);
    assert_eq!(definition.default_int, 9);
    assert_eq!(&*definition.type_name, "title");
    assert_eq!(decode_param(&[0]).unwrap(), Default::default());
    assert!(decode_param(&[2, 0]).is_err());
}

#[test]
fn items_decode_fields_defaults_and_unknown_opcodes() {
    let mut bytes = vec![2];
    bytes.extend(text("Sword"));
    bytes.extend([
        12, 0, 0, 0, 25, 11, 16, 13, 3, 14, 4, 27, 5, 18, 1, 2, 65, 96, 7,
    ]);
    bytes.extend([
        97, 0, 9, 98, 0, 10, 121, 0, 11, 122, 0, 12, 139, 0, 13, 140, 0, 14,
    ]);
    bytes.push(30);
    bytes.extend(text("Grab"));
    bytes.push(37);
    bytes.extend(text("Wield"));
    bytes.extend([43, 0, 18, 183, 86, 129, 6, 1, 2, 130, 7, 3, 4]);
    bytes.extend([1, 0x80, 0, 0, 7, 40, 2, 0, 1, 0, 2, 0, 3, 0, 4]);
    bytes.extend([
        42, 2, 5, 6, 132, 1, 0, 8, 100, 0, 1, 0, 2, 113, 5, 115, 6, 125, 1, 2, 3, 127, 1, 0, 2,
    ]);
    bytes.push(200);
    bytes.extend(parameters(&[
        (3, EnumValue::Int(-4)),
        (3, EnumValue::Int(5)),
        (4, EnumValue::Text(Arc::from("x"))),
    ]));
    bytes.push(0);
    let raw = decode_item(&bytes).unwrap();
    let item = &raw.definition;
    assert_eq!(&*item.name, "Sword");
    assert_eq!(item.value, 25);
    assert_eq!(item.stackable, 1);
    assert!(item.members_only);
    assert_eq!((item.wear_pos, item.wear_pos2, item.wear_pos3), (3, 4, 5));
    assert_eq!(item.multi_stack_size, 258);
    assert!(raw.tradeable);
    assert_eq!(raw.unknown_int6, 7);
    assert_eq!((item.cert_id, item.cert_template_id), (9, 10));
    assert_eq!((raw.lend_id, raw.lend_template_id), (11, 12));
    assert_eq!((raw.bind_id, raw.bind_template_id), (13, 14));
    assert_eq!(item.ground_actions[0].as_deref(), Some("Grab"));
    assert_eq!(item.ground_actions[2].as_deref(), Some("Take"));
    assert_eq!(item.inventory_actions[2].as_deref(), Some("Wield"));
    assert_eq!(item.inventory_actions[4].as_deref(), Some("Drop"));
    assert!(item.has_tooltip_color);
    assert_eq!(item.tooltip_color, 0x0012b756);
    assert_eq!((item.cursor_op1, item.cursor_id1), (6, 258));
    assert_eq!((item.cursor_op2, item.cursor_id2), (7, 772));
    let params = item.params.as_ref().unwrap();
    assert_eq!(params.len(), 2);
    assert_eq!(params[&3], EnumValue::Int(-4));
    assert_eq!(params[&4], EnumValue::Text(Arc::from("x")));
}

#[test]
fn item_defaults_match_the_kotlin_type() {
    let raw = decode_item(&[0]).unwrap();
    let item = raw.definition;
    assert_eq!(&*item.name, "null");
    assert_eq!(item.value, 1);
    assert_eq!(item.stackable, 0);
    assert_eq!(
        (
            item.wear_pos,
            item.wear_pos2,
            item.wear_pos3,
            item.multi_stack_size
        ),
        (-1, -1, -1, -1)
    );
    assert_eq!((item.cert_id, item.cert_template_id), (-1, -1));
    assert_eq!((raw.lend_template_id, raw.bind_template_id), (-1, -1));
    assert_eq!(item.ground_actions.len(), 6);
    assert_eq!(item.inventory_actions.len(), 5);
    assert!(item.params.is_none());
    assert!(decode_item(&[12, 0]).is_err());
}

#[test]
fn derived_items_follow_note_lend_and_bind_rules() {
    let mut original = decode_item(&[0]).unwrap().definition;
    original.name = Arc::from("Original");
    original.members_only = true;
    original.value = 77;
    original.stackable = 1;
    original.wear_pos = 2;
    original.inventory_actions[0] = Some(Arc::from("Use"));
    original.inventory_actions[4] = Some(Arc::from("Drop"));
    let mut params = ParamTable::new();
    params.insert(1, EnumValue::Int(2));
    original.params = Some(params);

    let mut note = decode_item(&[0]).unwrap().definition;
    derive_note(&mut note, &original);
    assert_eq!(&*note.name, "Original");
    assert_eq!(note.value, 77);
    assert_eq!(note.stackable, 1);
    assert!(note.members_only);
    assert!(note.params.is_none());

    let mut lend = decode_item(&[0]).unwrap().definition;
    derive_lend(&mut lend, &original);
    assert_eq!(lend.value, 0);
    assert_eq!(lend.stackable, 0);
    assert_eq!(lend.wear_pos, 2);
    assert_eq!(lend.params, original.params);
    assert_eq!(lend.inventory_actions[0].as_deref(), Some("Use"));
    assert_eq!(lend.inventory_actions[4].as_deref(), Some("Discard"));

    let mut bind = decode_item(&[0]).unwrap().definition;
    derive_bind(&mut bind, &original);
    assert_eq!(bind.value, 0);
    assert_eq!(bind.stackable, 1);
    assert_eq!(bind.inventory_actions[4].as_deref(), Some("Discard"));
}

#[test]
fn npc_definitions_skip_every_other_field_to_reach_the_parameters() {
    let mut bytes = vec![1, 2, 0x12, 0x34, 0x80, 0, 0x12, 0x34];
    bytes.push(2);
    bytes.extend(text("Guard"));
    bytes.extend([
        12, 1, 40, 1, 0, 1, 0, 2, 42, 2, 9, 9, 60, 1, 0, 5, 93, 95, 0, 3, 100, 4,
    ]);
    bytes.extend([106, 0, 1, 0, 2, 1, 0, 3, 0, 4]);
    bytes.extend([118, 0, 1, 0, 2, 0, 3, 0, 0, 1]);
    bytes.extend([
        121, 1, 0, 1, 2, 3, 134, 0, 1, 0, 2, 0, 3, 0, 4, 5, 138, 0, 1, 150,
    ]);
    bytes.extend(text("a"));
    bytes.extend([155, 1, 2, 3, 4, 160, 2, 0, 1, 0, 2, 164, 0, 1, 0, 2]);
    bytes.extend(parameters(&[(8, EnumValue::Int(3))]));
    bytes.push(0);
    let table = decode_npc_params(&bytes).unwrap().unwrap();
    assert_eq!(table[&8], EnumValue::Int(3));
    assert!(decode_npc_params(&[93, 0]).unwrap().is_none());
}

#[test]
fn seq_definitions_skip_frames_and_sounds_to_reach_the_parameters() {
    let mut bytes = vec![
        1, 0, 1, 0, 1, 0, 2, 0, 3, 2, 0, 1, 3, 2, 4, 5, 5, 1, 6, 0, 1,
    ];
    bytes.extend([12, 1, 0, 1, 0, 2, 13, 0, 2, 2, 0, 0, 7, 0, 9, 0]);
    bytes.extend([19, 1, 2, 20, 1, 0, 2, 0, 3]);
    bytes.extend(parameters(&[(1, EnumValue::Text(Arc::from("s")))]));
    bytes.push(0);
    let table = decode_seq_params(&bytes).unwrap().unwrap();
    assert_eq!(table[&1], EnumValue::Text(Arc::from("s")));
}

#[test]
fn struct_definitions_only_read_parameters() {
    let mut bytes = vec![7];
    bytes.extend(parameters(&[(2, EnumValue::Int(1))]));
    bytes.push(0);
    assert_eq!(
        decode_params_only(&bytes).unwrap().unwrap()[&2],
        EnumValue::Int(1)
    );
    assert!(decode_params_only(&[0]).unwrap().is_none());
}

#[test]
fn container_types_read_size_and_default_contents() {
    let bytes = [2, 0, 40, 4, 2, 0, 1, 0, 5, 0, 2, 0, 6, 0];
    let container = decode_container_type(&bytes).unwrap();
    assert_eq!(container.max_item_size, 40);
    assert_eq!(container.default_item_size, 2);
    assert_eq!(container.item_ids, vec![1, 2]);
    assert_eq!(container.amounts, vec![5, 6]);
}

#[test]
fn interaction_defaults_read_the_two_colours_after_the_subscribers() {
    let mut bytes = vec![
        1, 0, 1, 2, 2, 7, 8, 2, 1, 4, 3, 3, 2, 1, 7, 5, 1, 2, 6, 0, 7, 0, 11,
    ];
    bytes.extend([12, 0, 0x12, 0x34, 0x56]);
    bytes.extend([13, 0, 0x65, 0x43, 0x21, 0]);
    let colours = decode_interaction_colours(&bytes).unwrap();
    assert_eq!((colours.members, colours.free), (1_193_046, 6_636_321));
}
