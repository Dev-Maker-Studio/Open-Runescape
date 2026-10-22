use super::{pair, pop, push, Budget, Vm, STACK_CAPACITY};
use crate::{Opcode, StackKind, VmErrorKind};
use std::sync::Arc;

impl Vm {
    pub(super) fn execute_text(
        &mut self,
        opcode: Opcode,
        budget: &mut Budget,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, Long, String as Text};
        match opcode {
            APPEND => {
                let (left, right) = pair(&mut self.strings, Text)?;
                self.concatenate(
                    &[
                        left.as_deref().unwrap_or("null"),
                        right.as_deref().unwrap_or("null"),
                    ],
                    budget,
                )?;
            }
            APPEND_NUM | APPEND_SIGNNUM => {
                let text = pop(&mut self.strings, Text)?;
                let value = pop(&mut self.ints, Int)?;
                let mut buffer = [0; 20];
                let formatted = number(
                    i64::from(value),
                    10,
                    opcode == APPEND_SIGNNUM,
                    false,
                    &mut buffer,
                );
                self.concatenate(&[text.as_deref().unwrap_or("null"), formatted], budget)?;
            }
            TOSTRING | GET_COL_TAG => {
                let value = pop(&mut self.ints, Int)?;
                let mut buffer = [0; 20];
                if opcode == GET_COL_TAG {
                    let formatted = number(i64::from(value as u32), 16, false, false, &mut buffer);
                    self.concatenate(&["<col=", formatted, ">"], budget)?;
                } else {
                    let formatted = number(i64::from(value), 10, false, false, &mut buffer);
                    self.concatenate(&[formatted], budget)?;
                }
            }
            TEXT_SWITCH => {
                let (yes, no) = pair(&mut self.strings, Text)?;
                let flag = pop(&mut self.ints, Int)?;
                push(&mut self.strings, if flag == 1 { yes } else { no }, Text)?;
            }
            ESCAPE => {
                let text = pop(&mut self.strings, Text)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let extra = text
                    .bytes()
                    .filter(|byte| matches!(byte, b'<' | b'>'))
                    .count();
                let length = extra
                    .checked_mul(3)
                    .and_then(|extra| text.len().checked_add(extra))
                    .ok_or(VmErrorKind::StringLimit)?;
                let mut result = self.text_buffer(length, budget)?;
                for character in text.chars() {
                    match character {
                        '<' => result.push_str("<lt>"),
                        '>' => result.push_str("<gt>"),
                        other => result.push(other),
                    }
                }
                self.finish_text(result, budget);
            }
            APPEND_CHAR => {
                let text = pop(&mut self.strings, Text)?;
                let value = pop(&mut self.ints, Int)?;
                if value == -1 {
                    return Err(VmErrorKind::InvalidOperand);
                }
                let character = char::from_u32(u32::from(value as u16))
                    .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
                let mut buffer = [0; 4];
                self.concatenate(
                    &[
                        text.as_deref().unwrap_or("null"),
                        character.encode_utf8(&mut buffer),
                    ],
                    budget,
                )?;
            }
            CHAR_ISPRINTABLE | CHAR_ISALPHANUMERIC | CHAR_ISALPHA | CHAR_ISNUMERIC
            | CHAR_TOLOWERCASE | CHAR_TOUPPERCASE => {
                let unit = pop(&mut self.ints, Int)? as u16;
                let alphabetic = matches!(unit, 0x41..=0x5a | 0x61..=0x7a);
                let numeric = matches!(unit, 0x30..=0x39);
                let result = match opcode {
                    CHAR_ISPRINTABLE => i32::from(
                        matches!(unit, 0x20..=0x7e | 0xa0..=0xff | 8364 | 338 | 8212 | 339 | 376),
                    ),
                    CHAR_ISALPHANUMERIC => i32::from(alphabetic || numeric),
                    CHAR_ISALPHA => i32::from(alphabetic),
                    CHAR_ISNUMERIC => i32::from(numeric),
                    CHAR_TOLOWERCASE => i32::from(case(unit, include_bytes!("char-lower.bin"))),
                    CHAR_TOUPPERCASE => i32::from(case(unit, include_bytes!("char-upper.bin"))),
                    _ => unreachable!(),
                };
                push(&mut self.ints, result, Int)?;
            }
            STRING_LENGTH => {
                let text = pop(&mut self.strings, Text)?;
                let length = text
                    .as_deref()
                    .map_or(0, |text| text.encode_utf16().count());
                let result = i32::try_from(length).map_err(|_| VmErrorKind::StringLimit)?;
                push(&mut self.ints, result, Int)?;
            }
            SUBSTRING => {
                let text = pop(&mut self.strings, Text)?;
                let (start, end) = pair(&mut self.ints, Int)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let start = usize::try_from(start).map_err(|_| VmErrorKind::InvalidOperand)?;
                let end = usize::try_from(end).map_err(|_| VmErrorKind::InvalidOperand)?;
                if start > end || end > text.encode_utf16().count() {
                    return Err(VmErrorKind::InvalidOperand);
                }
                if start == end {
                    self.concatenate(&[""], budget)?;
                } else {
                    let start = utf16_boundary(text, start)
                        .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
                    let end =
                        utf16_boundary(text, end).ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
                    self.concatenate(&[&text[start..end]], budget)?;
                }
            }
            LOWERCASE => {
                let text = pop(&mut self.strings, Text)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                if text.contains('\u{3a3}') {
                    return Err(VmErrorKind::UnsupportedOpcode(opcode));
                }
                let lowered = lowercase(text);
                let mut result = self.text_buffer(lowered.len(), budget)?;
                result.push_str(&lowered);
                self.finish_text(result, budget);
            }
            DATE_ISLEAPYEAR => {
                let year = pop(&mut self.ints, Int)?;
                push(
                    &mut self.ints,
                    i32::from(crate::calendar::is_leap_year(year)),
                    Int,
                )?;
            }
            DATE_MINUTES_FROMRUNEDAY => {
                let days = pop(&mut self.ints, Int)?;
                push(
                    &mut self.ints,
                    crate::calendar::minutes_from_runeday(days),
                    Int,
                )?;
            }
            DATE_RUNEDAY_FROMDATE => {
                if self.ints.len() < 3 {
                    return Err(VmErrorKind::StackUnderflow(Int));
                }
                let year = pop(&mut self.ints, Int)?;
                let month = pop(&mut self.ints, Int)?;
                let day = pop(&mut self.ints, Int)?;
                push(
                    &mut self.ints,
                    crate::calendar::runeday_from_date(day, month, year),
                    Int,
                )?;
            }
            REMOVETAGS => {
                let text = pop(&mut self.strings, Text)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let length = visible(text).map(char::len_utf8).sum();
                let mut result = self.text_buffer(length, budget)?;
                result.extend(visible(text));
                self.finish_text(result, budget);
            }
            STRING_INDEXOF_CHAR => {
                let text = pop(&mut self.strings, Text)?;
                let (unit, from) = pair(&mut self.ints, Int)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let position = text
                    .encode_utf16()
                    .enumerate()
                    .skip(from.max(0) as usize)
                    .find_map(|(index, character)| (character == unit as u16).then_some(index));
                push(&mut self.ints, index(position)?, Int)?;
            }
            STRING_INDEXOF_STRING => {
                let (text, search) = pair(&mut self.strings, Text)?;
                let from = pop(&mut self.ints, Int)?;
                let text = text.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let search = search.as_deref().ok_or(VmErrorKind::InvalidOperand)?;
                let result = index(find(text, search, from.max(0) as usize))?;
                push(&mut self.ints, result, Int)?;
            }
            CLANFORUMQFC_TOSTRING => {
                let value = pop(&mut self.longs, Long)?;
                let mut buffer = [0; 20];
                let formatted = if value == -1 {
                    ""
                } else {
                    number(value, 36, false, true, &mut buffer)
                };
                self.concatenate(&[formatted], budget)?;
            }
            _ => crate::numeric::execute(opcode, &mut self.ints)?,
        }
        Ok(())
    }

    fn text_buffer(&self, length: usize, budget: &Budget) -> Result<String, VmErrorKind> {
        if self.strings.len() >= STACK_CAPACITY {
            return Err(VmErrorKind::StackOverflow(StackKind::String));
        }
        if length > self.limits.max_string_bytes
            || budget
                .join_bytes
                .checked_add(length)
                .is_none_or(|total| total > self.limits.max_join_bytes)
        {
            return Err(VmErrorKind::StringLimit);
        }
        let mut result = String::new();
        result
            .try_reserve_exact(length)
            .map_err(|_| VmErrorKind::StringLimit)?;
        Ok(result)
    }

    fn finish_text(&mut self, value: String, budget: &mut Budget) {
        budget.join_bytes += value.len();
        self.strings.push(Some(Arc::from(value)));
    }

    fn concatenate(&mut self, parts: &[&str], budget: &mut Budget) -> Result<(), VmErrorKind> {
        let length = parts
            .iter()
            .try_fold(0usize, |length, part| length.checked_add(part.len()))
            .ok_or(VmErrorKind::StringLimit)?;
        let mut result = self.text_buffer(length, budget)?;
        for part in parts {
            result.push_str(part);
        }
        self.finish_text(result, budget);
        Ok(())
    }
}

fn number(value: i64, radix: u64, plus: bool, uppercase: bool, buffer: &mut [u8; 20]) -> &str {
    let mut magnitude = value.unsigned_abs();
    let mut start = buffer.len();
    loop {
        start -= 1;
        let digit = (magnitude % radix) as u8;
        buffer[start] = if digit < 10 {
            b'0' + digit
        } else {
            (if uppercase { b'A' } else { b'a' }) + digit - 10
        };
        magnitude /= radix;
        if magnitude == 0 {
            break;
        }
    }
    if value < 0 || plus {
        start -= 1;
        buffer[start] = if value < 0 { b'-' } else { b'+' };
    }
    std::str::from_utf8(&buffer[start..]).unwrap()
}

fn utf16_boundary(text: &str, index: usize) -> Option<usize> {
    let mut units = 0;
    for (offset, character) in text.char_indices() {
        if units == index {
            return Some(offset);
        }
        units += character.len_utf16();
        if units > index {
            return None;
        }
    }
    (units == index).then_some(text.len())
}

fn visible(text: &str) -> impl Iterator<Item = char> + '_ {
    let mut inside = false;
    text.chars().filter(move |character| match character {
        '<' => {
            inside = true;
            false
        }
        '>' => {
            inside = false;
            false
        }
        _ => !inside,
    })
}

fn index(position: Option<usize>) -> Result<i32, VmErrorKind> {
    position.map_or(Ok(-1), |position| {
        i32::try_from(position).map_err(|_| VmErrorKind::StringLimit)
    })
}

fn find(text: &str, search: &str, from: usize) -> Option<usize> {
    if search.is_empty() {
        return Some(from.min(text.encode_utf16().count()));
    }
    let mut units = 0;
    let mut byte = text.len();
    for (offset, character) in text.char_indices() {
        if units >= from {
            byte = offset;
            break;
        }
        units += character.len_utf16();
    }
    let tail = &text[byte..];
    tail.find(search)
        .map(|offset| units + tail[..offset].encode_utf16().count())
}

pub(super) fn case(unit: u16, table: &[u8]) -> u16 {
    let mut lower = 0;
    let mut upper = table.len() / 4;
    while lower < upper {
        let middle = lower + (upper - lower) / 2;
        let start = middle * 4;
        let key = u16::from_be_bytes([table[start], table[start + 1]]);
        if key < unit {
            lower = middle + 1;
        } else {
            upper = middle;
        }
    }
    let start = lower * 4;
    if let Some(entry) = table.get(start..start + 4) {
        if u16::from_be_bytes([entry[0], entry[1]]) == unit {
            return u16::from_be_bytes([entry[2], entry[3]]);
        }
    }
    unit
}

fn lowercase(text: &str) -> String {
    let mut result = String::with_capacity(text.len());
    for character in text.chars() {
        let point = u32::from(character);
        if point == 0x130 {
            result.push_str("i\u{307}");
        } else if point < 0x10000 {
            let mapped = case(point as u16, include_bytes!("char-lower.bin"));
            result.push(char::from_u32(u32::from(mapped)).unwrap_or(character));
        } else {
            result.push(supplementary_lower(point).map_or(character, |mapped| mapped));
        }
    }
    result
}

fn supplementary_lower(point: u32) -> Option<char> {
    let table = include_bytes!("lower-supplementary.bin");
    let mut lower = 0;
    let mut upper = table.len() / 8;
    while lower < upper {
        let middle = lower + (upper - lower) / 2;
        let start = middle * 8;
        let key = u32::from_be_bytes(table[start..start + 4].try_into().unwrap());
        if key < point {
            lower = middle + 1;
        } else {
            upper = middle;
        }
    }
    let start = lower * 8;
    let entry = table.get(start..start + 8)?;
    if u32::from_be_bytes(entry[..4].try_into().unwrap()) == point {
        char::from_u32(u32::from_be_bytes(entry[4..].try_into().unwrap()))
    } else {
        None
    }
}
