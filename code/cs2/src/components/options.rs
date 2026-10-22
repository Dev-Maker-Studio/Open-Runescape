use super::ComponentFailure;
use std::sync::Arc;

const KEY_SLOTS: usize = 11;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ParamType {
    pub default_int: i32,
    pub type_char: char,
    pub type_name: Option<Arc<str>>,
}

impl ParamType {
    pub fn int(default_int: i32) -> Self {
        Self {
            default_int,
            type_char: '\0',
            type_name: None,
        }
    }

    pub fn string(type_name: Option<Arc<str>>) -> Self {
        Self {
            default_int: 0,
            type_char: 's',
            type_name,
        }
    }

    pub fn is_string(&self) -> bool {
        self.type_char == 's'
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum ParamNode {
    Int(i32),
    Text(Arc<str>),
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct KeyTriggers {
    pub char_codes: [i32; KEY_SLOTS],
    pub actions: [Option<Vec<i8>>; KEY_SLOTS],
    pub modifiers: [Option<Vec<i8>>; KEY_SLOTS],
}

impl KeyTriggers {
    fn new() -> Self {
        Self {
            char_codes: [0; KEY_SLOTS],
            actions: Default::default(),
            modifiers: Default::default(),
        }
    }

    fn any_trigger(&self) -> bool {
        (0..KEY_SLOTS).any(|slot| self.actions[slot].is_some() || self.char_codes[slot] > 0)
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ComponentOptions {
    pub options: Option<Vec<Option<Arc<str>>>>,
    pub op_base: Option<Arc<str>>,
    pub op_cursors: Option<Vec<i32>>,
    pub target_over_cursor: i32,
    pub target_leave_cursor: i32,
    pub mouse_over_cursor: i32,
    pub target_op_cursor: i32,
    pub keys: Option<KeyTriggers>,
    pub has_interaction: bool,
    pub target_settings: i32,
    pub params: Vec<(i32, ParamNode)>,
}

impl Default for ComponentOptions {
    fn default() -> Self {
        Self {
            options: None,
            op_base: Some(Arc::from("")),
            op_cursors: None,
            target_over_cursor: -1,
            target_leave_cursor: -1,
            mouse_over_cursor: -1,
            target_op_cursor: -1,
            keys: None,
            has_interaction: false,
            target_settings: 0,
            params: Vec::new(),
        }
    }
}

impl ComponentOptions {
    pub fn option(&self, index: i32) -> Result<Option<&Arc<str>>, ComponentFailure> {
        let Some(options) = &self.options else {
            return Ok(None);
        };
        if index < 0 {
            return Err(ComponentFailure::Index);
        }
        Ok(options.get(index as usize).and_then(Option::as_ref))
    }

    pub fn add_option(&mut self, index: usize, name: Option<Arc<str>>) {
        let options = self.options.get_or_insert_with(Vec::new);
        if options.len() <= index {
            options.resize(index + 1, None);
        }
        options[index] = name;
    }

    pub fn add_cursor(&mut self, index: usize, cursor: i32) {
        let grow = self
            .op_cursors
            .as_ref()
            .is_none_or(|cursors| cursors.len() <= index);
        if grow {
            let mut replacement = vec![0; index + 1];
            if let Some(old) = &self.op_cursors {
                replacement[..old.len()].copy_from_slice(old);
                for slot in replacement.iter_mut().take(index).skip(old.len()) {
                    *slot = -1;
                }
            }
            self.op_cursors = Some(replacement);
        }
        if let Some(cursors) = self.op_cursors.as_mut() {
            cursors[index] = cursor;
        }
    }

    pub fn set_char_slot(&mut self, slot: usize, char_code: i32) {
        if self.keys.is_none() {
            if char_code <= 0 {
                return;
            }
            self.keys = Some(KeyTriggers::new());
        }
        let Some(keys) = self.keys.as_mut() else {
            return;
        };
        keys.char_codes[slot] = char_code;
        if char_code > 0 {
            self.has_interaction = true;
        } else {
            self.has_interaction = keys.any_trigger();
        }
    }

    pub fn set_trigger_key(
        &mut self,
        slot: usize,
        codes: Option<Vec<i8>>,
        modifiers: Option<Vec<i8>>,
    ) {
        if self.keys.is_none() {
            if codes.is_none() {
                return;
            }
            self.keys = Some(KeyTriggers::new());
        }
        let Some(keys) = self.keys.as_mut() else {
            return;
        };
        let present = codes.is_some();
        keys.actions[slot] = codes;
        if present {
            self.has_interaction = true;
        } else {
            self.has_interaction = keys.any_trigger();
        }
        keys.modifiers[slot] = modifiers;
    }

    fn param_index(&self, key: i32) -> Option<usize> {
        self.params
            .iter()
            .position(|(existing, _)| *existing == key)
    }

    pub fn remove_param(&mut self, key: i32) {
        if let Some(index) = self.param_index(key) {
            self.params.remove(index);
        }
    }

    pub fn int_param(&self, key: i32, default: i32) -> i32 {
        match self.param_index(key).map(|index| &self.params[index].1) {
            Some(ParamNode::Int(value)) => *value,
            _ => default,
        }
    }

    pub fn string_param(&self, key: i32, default: &Arc<str>) -> Arc<str> {
        match self.param_index(key).map(|index| &self.params[index].1) {
            Some(ParamNode::Text(value)) => value.clone(),
            _ => default.clone(),
        }
    }

    pub fn set_int_param(&mut self, key: i32, value: i32) {
        match self.param_index(key) {
            Some(index) if matches!(self.params[index].1, ParamNode::Int(_)) => {
                self.params[index].1 = ParamNode::Int(value);
            }
            _ => self.params.push((key, ParamNode::Int(value))),
        }
    }

    pub fn set_string_param(&mut self, key: i32, value: Arc<str>) {
        if let Some(index) = self.param_index(key) {
            if matches!(self.params[index].1, ParamNode::Text(_)) {
                self.params.remove(index);
            }
        }
        self.params.push((key, ParamNode::Text(value)));
    }
}
