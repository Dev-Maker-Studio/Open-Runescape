use std::collections::HashMap;
use std::collections::VecDeque;

pub const CLIENT_SETVARC: u8 = 1;
pub const CLIENT_SETVARCSTR: u8 = 2;
pub const IF_SETTEXT: u8 = 3;
pub const IF_SETCONTENT: u8 = 4;
pub const IF_SETANIM: u8 = 5;
pub const IF_SETCOLOR: u8 = 6;
pub const IF_SETHIDE: u8 = 7;
pub const IF_SETANGLE: u8 = 8;
pub const IF_SETITEM: u8 = 9;
pub const IF_SETOFFSET: u8 = 10;
pub const IF_SETPOSITION: u8 = 11;
pub const IF_SETSCROLLPOS: u8 = 12;
pub const T13: u8 = 13;
pub const IF_SETGRAPHIC: u8 = 14;
pub const MINIMAP_FLAG: u8 = 15;
pub const IF_SETFONT: u8 = 16;
pub const T17: u8 = 17;
pub const IF_SETRECOL: u8 = 18;
pub const IF_SETRETEX: u8 = 19;
pub const IF_SETMONOSPACED: u8 = 20;
pub const IF_SETCLICKMASK: u8 = 21;
pub const IF_SETTEXTANTIMACRO: u8 = 22;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PulseUpdate {
    pub pulse_type: u8,
    pub data: i64,
    pub text: Option<String>,
    pub primary: i32,
    pub secondary: i32,
    pub tertiary: i32,
    pub requests_var_verify: bool,
}

impl PulseUpdate {
    pub fn new(pulse_type: u8, uid: i32) -> Self {
        Self {
            pulse_type,
            data: i64::from(uid),
            text: None,
            primary: 0,
            secondary: 0,
            tertiary: 0,
            requests_var_verify: true,
        }
    }

    pub fn keyed(pulse_type: u8, index: i32, hash: i32) -> Self {
        let mut update = Self::new(pulse_type, hash);
        update.data = (i64::from(index) << 32) | i64::from(hash);
        update
    }

    pub fn pointer(&self) -> u64 {
        (u64::from(self.pulse_type) << 56) | self.data as u64
    }

    pub fn event_type(&self) -> u8 {
        (self.pointer() >> 56) as u8
    }

    pub fn value(&self) -> u64 {
        self.pointer() & 0x00ff_ffff_ffff_ffff
    }

    pub fn with(mut self, primary: i32, secondary: i32, tertiary: i32) -> Self {
        self.primary = primary;
        self.secondary = secondary;
        self.tertiary = tertiary;
        self
    }

    pub fn with_text(mut self, text: String) -> Self {
        self.text = Some(text);
        self
    }

    pub fn without_var_verify(mut self) -> Self {
        self.requests_var_verify = false;
        self
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PulseEvent {
    pub pointer: u64,
    pub text: Option<String>,
    pub primary: i32,
    pub secondary: i32,
    pub tertiary: i32,
}

#[derive(Debug, Default)]
pub struct PulseQueue {
    cache: HashMap<u64, PulseEvent>,
    high: VecDeque<u64>,
    var_verify_id: i32,
    send_var_verify: bool,
}

impl PulseQueue {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn var_verify_id(&self) -> i32 {
        self.var_verify_id
    }

    pub fn send_var_verify(&self) -> bool {
        self.send_var_verify
    }

    pub fn apply(&mut self, update: &PulseUpdate) {
        if update.requests_var_verify {
            self.var_verify_id = self.var_verify_id.wrapping_add(1);
            self.send_var_verify = true;
        }
        let pointer = update.pointer();
        let event = self.cache.entry(pointer).or_insert_with(|| PulseEvent {
            pointer,
            text: None,
            primary: 0,
            secondary: 0,
            tertiary: 0,
        });
        if let Some(position) = self.high.iter().position(|queued| *queued == pointer) {
            self.high.remove(position);
        }
        self.high.push_back(pointer);
        event.text = update.text.clone();
        event.primary = update.primary;
        event.secondary = update.secondary;
        event.tertiary = update.tertiary;
    }

    pub fn len(&self) -> usize {
        self.high.len()
    }

    pub fn is_empty(&self) -> bool {
        self.high.is_empty()
    }

    pub fn next_event(&mut self) -> Option<PulseEvent> {
        let pointer = self.high.pop_front()?;
        self.cache.remove(&pointer)
    }

    pub fn clear(&mut self) {
        self.cache.clear();
        self.high.clear();
    }
}
