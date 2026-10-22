use crate::EnumValue;
use std::{collections::BTreeMap, sync::Arc};

pub type ParamTable = BTreeMap<i32, EnumValue>;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ConfigKind {
    Npc,
    Seq,
    Struct,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ParamDefinition {
    pub is_string: bool,
    pub default_int: i32,
    pub type_name: Arc<str>,
}

impl Default for ParamDefinition {
    fn default() -> Self {
        Self {
            is_string: false,
            default_int: 0,
            type_name: Arc::from("null"),
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct BasDefinition {
    pub stand_sequence: i32,
    pub random_stand_sequences: Option<Vec<i32>>,
    pub random_stand_chances: Option<Vec<i32>>,
}

impl Default for BasDefinition {
    fn default() -> Self {
        Self {
            stand_sequence: -1,
            random_stand_sequences: None,
            random_stand_chances: None,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ItemDefinition {
    pub name: Arc<str>,
    pub value: i32,
    pub stackable: i32,
    pub members_only: bool,
    pub cert_id: i32,
    pub cert_template_id: i32,
    pub wear_pos: i32,
    pub wear_pos2: i32,
    pub wear_pos3: i32,
    pub multi_stack_size: i32,
    pub ground_actions: Vec<Option<Arc<str>>>,
    pub inventory_actions: Vec<Option<Arc<str>>>,
    pub tooltip_color: i32,
    pub has_tooltip_color: bool,
    pub cursor_op1: i32,
    pub cursor_id1: i32,
    pub cursor_op2: i32,
    pub cursor_id2: i32,
    pub params: Option<ParamTable>,
}

impl Default for ItemDefinition {
    fn default() -> Self {
        Self {
            name: Arc::from("null"),
            value: 1,
            stackable: 0,
            members_only: false,
            cert_id: -1,
            cert_template_id: -1,
            wear_pos: -1,
            wear_pos2: -1,
            wear_pos3: -1,
            multi_stack_size: -1,
            ground_actions: vec![None; 6],
            inventory_actions: vec![None; 5],
            tooltip_color: 0,
            has_tooltip_color: false,
            cursor_op1: -1,
            cursor_id1: -1,
            cursor_op2: -1,
            cursor_id2: -1,
            params: None,
        }
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct InteractionColours {
    pub members: i32,
    pub free: i32,
}
