use std::collections::VecDeque;

pub const BASE_KEY_REMAP: [u8; 521] = [
    0, 0, 0, 0, 0, 0, 0, 0, 85, 80, 84, 0, 91, 0, 0, 0, 81, 82, 86, 0, 0, 0, 0, 0,
    0, 0, 0, 13, 0, 0, 0, 0, 83, 104, 105, 103, 102, 96, 98, 97, 99, 0, 0, 0, 0, 0, 0, 0,
    25, 16, 17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 48, 68, 66, 50, 34, 51, 52,
    53, 39, 54, 55, 56, 70, 69, 40, 41, 32, 35, 49, 36, 38, 67, 33, 65, 37, 64, 0, 0, 0, 0, 0,
    228, 231, 227, 233, 224, 219, 225, 230, 226, 232, 89, 87, 0, 88, 229, 90, 1, 2, 3, 4, 5, 6, 7, 8,
    9, 10, 11, 12, 0, 0, 0, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
];

const REMAP_OVERRIDES: [(usize, u8); 12] = [
    (44, 71),
    (45, 26),
    (46, 72),
    (47, 73),
    (59, 57),
    (61, 27),
    (91, 42),
    (92, 74),
    (93, 43),
    (192, 28),
    (222, 58),
    (520, 59),
];

pub fn remapped_key(index: usize) -> Option<i32> {
    let value = REMAP_OVERRIDES
        .iter()
        .find(|(i, _)| *i == index)
        .map(|(_, v)| *v)
        .or_else(|| BASE_KEY_REMAP.get(index).copied())?;
    Some(i32::from(value))
}

pub const UNICODE_TABLE: [u16; 32] = [
    0x20ac, 0, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021, 0x02c6, 0x2030, 0x0160, 0x2039,
    0x0152, 0, 0x017d, 0, 0, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014, 0x02dc,
    0x2122, 0x0161, 0x203a, 0x0153, 0, 0x017e, 0x0178,
];

pub fn is_valid_key_char(unit: u16) -> bool {
    if (1..128).contains(&unit) || (160..=255).contains(&unit) {
        return true;
    }
    unit != 0 && UNICODE_TABLE.contains(&unit)
}

pub fn translate_key_code(key_code: i32, entry_type: i32) -> i32 {
    if key_code == 0 {
        return 0;
    }
    let Some(index) = usize::try_from(key_code).ok() else {
        return 0;
    };
    let Some(mapped) = remapped_key(index) else {
        return 0;
    };
    if entry_type == 0 && mapped & 0x80 != 0 {
        0
    } else {
        mapped & -129
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct KeyEntry {
    pub entry_type: i32,
    pub character: u16,
    pub code: i32,
    pub time: i64,
    pub flags: i32,
}

#[derive(Clone, Debug)]
pub struct KeyRecorder {
    entries: VecDeque<KeyEntry>,
    awaiting: VecDeque<KeyEntry>,
    held: [bool; 112],
}

impl Default for KeyRecorder {
    fn default() -> Self {
        Self::new()
    }
}

impl KeyRecorder {
    pub fn new() -> Self {
        Self {
            entries: VecDeque::new(),
            awaiting: VecDeque::new(),
            held: [false; 112],
        }
    }

    pub fn held(&self, index: i32) -> bool {
        usize::try_from(index)
            .ok()
            .and_then(|i| self.held.get(i).copied())
            .unwrap_or(false)
    }

    fn add(&mut self, entry_type: i32, character: u16, code: i32, time: i64) {
        self.entries.push_back(KeyEntry {
            entry_type,
            character,
            code,
            time,
            flags: 0,
        });
    }

    pub fn on_key_press(&mut self, key_code: i32, entry_type: i32, time: i64) -> bool {
        let code = translate_key_code(key_code, entry_type);
        if code != 0 {
            self.add(entry_type, 0xffff, code, time);
            true
        } else {
            false
        }
    }

    pub fn key_pressed(&mut self, key_code: i32, time: i64) -> bool {
        self.on_key_press(key_code, 0, time)
    }

    pub fn key_released(&mut self, key_code: i32, time: i64) -> bool {
        self.on_key_press(key_code, 1, time)
    }

    pub fn key_typed(&mut self, unit: u16, time: i64) -> bool {
        if unit != 0xffff && is_valid_key_char(unit) {
            self.add(3, unit, -1, time);
            true
        } else {
            false
        }
    }

    pub fn focus_lost(&mut self, time: i64) {
        self.add(-1, 0, 0, time);
    }

    pub fn flags(&self) -> i32 {
        let mut flags = 0;
        if self.held[81] {
            flags |= 1;
        }
        if self.held[82] {
            flags |= 4;
        }
        if self.held[86] {
            flags |= 2;
        }
        flags
    }

    pub fn prepare_awaiting_keys(&mut self) {
        self.awaiting.clear();
        while let Some(mut entry) = self.entries.pop_front() {
            entry.flags = self.flags();
            match entry.entry_type {
                0 => {
                    let index = entry.code as usize;
                    if !self.held.get(index).copied().unwrap_or(false) {
                        self.awaiting.push_back(KeyEntry {
                            entry_type: 0,
                            character: 0xffff,
                            ..entry
                        });
                        if let Some(slot) = self.held.get_mut(index) {
                            *slot = true;
                        }
                    }
                    entry.entry_type = 2;
                    self.awaiting.push_back(entry);
                }
                1 => {
                    let index = entry.code as usize;
                    if self.held.get(index).copied().unwrap_or(false) {
                        self.awaiting.push_back(entry);
                        self.held[index] = false;
                    }
                }
                -1 => {
                    for index in 0..112 {
                        if self.held[index] {
                            self.awaiting.push_back(KeyEntry {
                                entry_type: 1,
                                character: 0xffff,
                                code: index as i32,
                                time: entry.time,
                                flags: entry.flags,
                            });
                            self.held[index] = false;
                        }
                    }
                }
                3 => self.awaiting.push_back(entry),
                _ => {}
            }
        }
    }

    pub fn pop_head(&mut self) -> Option<KeyEntry> {
        self.awaiting.pop_front()
    }
}
