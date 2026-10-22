use crate::inventory::quicksort;
use std::{collections::BTreeMap, sync::Arc};

pub const MENU_BASE: i32 = 32768;
pub const SEARCH_LIMIT: usize = 50;

const WINDOWS_1252: [u16; 32] = [
    0x20ac, 0, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021, 0x02c6, 0x2030, 0x0160, 0x2039,
    0x0152, 0, 0x017d, 0, 0, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014, 0x02dc,
    0x2122, 0x0161, 0x203a, 0x0153, 0, 0x017e, 0x0178,
];

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct QuickChatDecodeError;

impl std::fmt::Display for QuickChatDecodeError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str("quick chat definition ended early")
    }
}

impl std::error::Error for QuickChatDecodeError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct QuickChatValueType {
    pub id: i32,
    pub client_transmit_size: i32,
    pub server_transmit_size: i32,
    pub parameter_count: usize,
}

const fn kind(id: i32, client: i32, server: i32, parameters: usize) -> QuickChatValueType {
    QuickChatValueType {
        id,
        client_transmit_size: client,
        server_transmit_size: server,
        parameter_count: parameters,
    }
}

pub const VALUE_TYPES: [QuickChatValueType; 14] = [
    kind(0, 2, 2, 1),
    kind(1, 2, 2, 0),
    kind(2, 4, 4, 0),
    kind(4, 1, 1, 1),
    kind(6, 0, 4, 2),
    kind(7, 0, 1, 1),
    kind(8, 0, 4, 1),
    kind(9, 0, 4, 1),
    kind(10, 2, 2, 0),
    kind(11, 0, 1, 2),
    kind(12, 0, 1, 0),
    kind(13, 0, 1, 0),
    kind(14, 0, 4, 1),
    kind(15, 0, 1, 0),
];

impl QuickChatValueType {
    pub fn get(id: i32) -> Option<Self> {
        VALUE_TYPES.iter().copied().find(|kind| kind.id == id)
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum QuickChatFault {
    Index,
}

struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl Reader<'_> {
    fn byte(&mut self) -> Result<u8, QuickChatDecodeError> {
        let value = *self.bytes.get(self.position).ok_or(QuickChatDecodeError)?;
        self.position += 1;
        Ok(value)
    }

    fn unsigned_short(&mut self) -> Result<i32, QuickChatDecodeError> {
        let high = i32::from(self.byte()?);
        Ok((high << 8) | i32::from(self.byte()?))
    }

    fn string(&mut self) -> Result<String, QuickChatDecodeError> {
        let mut units: Vec<u16> = Vec::new();
        loop {
            let value = self.byte()?;
            if value == 0 {
                break;
            }
            units.push(cp1252_unit(value));
        }
        Ok(String::from_utf16_lossy(&units))
    }

    fn hotkey(&mut self) -> Result<u16, QuickChatDecodeError> {
        let value = self.byte()?;
        Ok(if value == 0 { 0 } else { cp1252_unit(value) })
    }
}

fn cp1252_unit(value: u8) -> u16 {
    if (128..160).contains(&value) {
        match WINDOWS_1252[usize::from(value) - 128] {
            0 => 63,
            mapped => mapped,
        }
    } else {
        u16::from(value)
    }
}

fn split_lt(input: &str) -> Vec<Arc<str>> {
    input.split('<').map(Arc::from).collect()
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct QuickChatCategory {
    pub name: Option<Arc<str>>,
    pub sub_categories: Option<Vec<i32>>,
    pub sub_category_hotkeys: Option<Vec<u16>>,
    pub messages: Option<Vec<i32>>,
    pub message_hotkeys: Option<Vec<u16>>,
}

impl QuickChatCategory {
    pub fn decode(bytes: &[u8]) -> Result<Self, QuickChatDecodeError> {
        let mut reader = Reader { bytes, position: 0 };
        let mut category = Self::default();
        loop {
            match reader.byte()? {
                0 => return Ok(category),
                1 => category.name = Some(Arc::from(reader.string()?)),
                opcode @ (2 | 3) => {
                    let count = usize::from(reader.byte()?);
                    let mut ids = Vec::with_capacity(count);
                    let mut hotkeys = Vec::with_capacity(count);
                    for _ in 0..count {
                        ids.push(reader.unsigned_short()?);
                        hotkeys.push(reader.hotkey()?);
                    }
                    if opcode == 2 {
                        category.sub_categories = Some(ids);
                        category.sub_category_hotkeys = Some(hotkeys);
                    } else {
                        category.messages = Some(ids);
                        category.message_hotkeys = Some(hotkeys);
                    }
                }
                _ => {}
            }
        }
    }

    fn post_decode(&mut self) {
        for list in [&mut self.messages, &mut self.sub_categories]
            .into_iter()
            .flatten()
        {
            for id in list {
                *id |= 0x8000;
            }
        }
    }

    pub fn sub_category_for_hotkey(&self, hotkey: u16) -> i32 {
        hotkey_lookup(&self.sub_categories, &self.sub_category_hotkeys, hotkey)
    }

    pub fn message_for_hotkey(&self, hotkey: u16) -> i32 {
        hotkey_lookup(&self.messages, &self.message_hotkeys, hotkey)
    }
}

fn hotkey_lookup(ids: &Option<Vec<i32>>, hotkeys: &Option<Vec<u16>>, hotkey: u16) -> i32 {
    if let (Some(ids), Some(hotkeys)) = (ids, hotkeys) {
        for (index, id) in ids.iter().enumerate() {
            if hotkeys[index] == hotkey {
                return *id;
            }
        }
    }
    -1
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct QuickChatMessage {
    pub responses: Option<Vec<i32>>,
    pub searchable: bool,
    pub message: Option<Vec<Arc<str>>>,
    pub value_types: Option<Vec<i32>>,
    pub configs: Option<Vec<Option<Vec<i32>>>>,
}

impl Default for QuickChatMessage {
    fn default() -> Self {
        Self {
            responses: None,
            searchable: true,
            message: None,
            value_types: None,
            configs: None,
        }
    }
}

impl QuickChatMessage {
    pub fn decode(bytes: &[u8]) -> Result<Self, QuickChatDecodeError> {
        let mut reader = Reader { bytes, position: 0 };
        let mut message = Self::default();
        loop {
            match reader.byte()? {
                0 => return Ok(message),
                1 => message.message = Some(split_lt(&reader.string()?)),
                2 => {
                    let count = usize::from(reader.byte()?);
                    let mut responses = Vec::with_capacity(count);
                    for _ in 0..count {
                        responses.push(reader.unsigned_short()?);
                    }
                    message.responses = Some(responses);
                }
                3 => {
                    let count = usize::from(reader.byte()?);
                    let mut types = vec![0; count];
                    let mut configs: Vec<Option<Vec<i32>>> = vec![None; count];
                    for index in 0..count {
                        let type_id = reader.unsigned_short()?;
                        if let Some(kind) = QuickChatValueType::get(type_id) {
                            types[index] = type_id;
                            let mut values = Vec::with_capacity(kind.parameter_count);
                            for _ in 0..kind.parameter_count {
                                values.push(reader.unsigned_short()?);
                            }
                            configs[index] = Some(values);
                        }
                    }
                    message.value_types = Some(types);
                    message.configs = Some(configs);
                }
                4 => message.searchable = false,
                _ => {}
            }
        }
    }

    fn post_decode(&mut self) {
        if let Some(responses) = &mut self.responses {
            for id in responses {
                *id |= 0x8000;
            }
        }
    }

    pub fn placeholder_text(&self) -> Arc<str> {
        let Some(pieces) = &self.message else {
            return Arc::from("");
        };
        let mut text = String::new();
        text.push_str(&pieces[0]);
        for piece in &pieces[1..] {
            text.push_str("...");
            text.push_str(piece);
        }
        Arc::from(text)
    }

    pub fn value_type_count(&self) -> i32 {
        self.value_types.as_ref().map_or(0, |types| types.len() as i32)
    }

    pub fn value_type(&self, index: i32) -> Result<Option<QuickChatValueType>, QuickChatFault> {
        match &self.value_types {
            Some(types) if index >= 0 && index as usize <= types.len() => types
                .get(index as usize)
                .map(|id| QuickChatValueType::get(*id))
                .ok_or(QuickChatFault::Index),
            _ => Ok(None),
        }
    }

    pub fn value_config(&self, index: i32, config: i32) -> Result<i32, QuickChatFault> {
        let (Some(types), Some(configs)) = (&self.value_types, &self.configs) else {
            return Ok(-1);
        };
        if index < 0 || index as usize > types.len() {
            return Ok(-1);
        }
        let entry = configs.get(index as usize).ok_or(QuickChatFault::Index)?;
        match entry {
            Some(values) if config >= 0 && config as usize <= values.len() => values
                .get(config as usize)
                .copied()
                .ok_or(QuickChatFault::Index),
            _ => Ok(-1),
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum QuickChatSearch {
    Overflow,
    Found(Vec<u16>),
}

static EMPTY_CATEGORY: QuickChatCategory = QuickChatCategory {
    name: None,
    sub_categories: None,
    sub_category_hotkeys: None,
    messages: None,
    message_hotkeys: None,
};

static EMPTY_MESSAGE: QuickChatMessage = QuickChatMessage {
    responses: None,
    searchable: true,
    message: None,
    value_types: None,
    configs: None,
};

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct QuickChatDefs {
    message_count: i32,
    menu_count: i32,
    categories: BTreeMap<(bool, i32), QuickChatCategory>,
    messages: BTreeMap<(bool, i32), QuickChatMessage>,
}

fn slot(id: i32) -> Option<(bool, i32)> {
    if id >= MENU_BASE {
        Some((true, id & 0x7fff))
    } else if id >= 0 {
        Some((false, id))
    } else {
        None
    }
}

impl QuickChatDefs {
    pub fn new(message_count: i32, menu_count: i32) -> Self {
        Self {
            message_count,
            menu_count,
            ..Self::default()
        }
    }

    pub fn message_count(&self) -> i32 {
        self.message_count
    }

    pub fn menu_count(&self) -> i32 {
        self.menu_count
    }

    pub fn insert_category(
        &mut self,
        menu: bool,
        file: i32,
        bytes: &[u8],
    ) -> Result<(), QuickChatDecodeError> {
        let mut category = QuickChatCategory::decode(bytes)?;
        if menu {
            category.post_decode();
        }
        self.categories.insert((menu, file), category);
        Ok(())
    }

    pub fn insert_message(
        &mut self,
        menu: bool,
        file: i32,
        bytes: &[u8],
    ) -> Result<(), QuickChatDecodeError> {
        let mut message = QuickChatMessage::decode(bytes)?;
        if menu {
            message.post_decode();
        }
        self.messages.insert((menu, file), message);
        Ok(())
    }

    pub fn category(&self, id: i32) -> &QuickChatCategory {
        slot(id)
            .and_then(|key| self.categories.get(&key))
            .unwrap_or(&EMPTY_CATEGORY)
    }

    pub fn message(&self, id: i32) -> &QuickChatMessage {
        slot(id)
            .and_then(|key| self.messages.get(&key))
            .unwrap_or(&EMPTY_MESSAGE)
    }

    pub fn search(&self, query: &str, menu: bool) -> QuickChatSearch {
        let needle = query.to_lowercase();
        let start = if menu { MENU_BASE } else { 0 };
        let end = start.wrapping_add(if menu {
            self.menu_count
        } else {
            self.message_count
        });
        let mut ids: Vec<u16> = Vec::new();
        for id in start..end {
            let message = self.message(id);
            if message.searchable
                && message
                    .placeholder_text()
                    .to_lowercase()
                    .contains(needle.as_str())
            {
                if ids.len() >= SEARCH_LIMIT {
                    return QuickChatSearch::Overflow;
                }
                ids.push(id as u16);
            }
        }
        let mut names: Vec<Arc<str>> = ids
            .iter()
            .map(|id| self.message(i32::from(*id as i16)).placeholder_text())
            .collect();
        let last = ids.len() as isize - 1;
        quicksort(&mut names, &mut ids, 0, last);
        QuickChatSearch::Found(ids)
    }
}
