use super::text::case;
use super::{pop, push, Vm};
use crate::outbox::TypedMessage;
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

const CHAT_TYPE_COUNT: i32 = 122;
const GAME: i32 = 4;
const DEV_CONSOLE_CLEAR: i32 = 98;
const DEV_CONSOLE: i32 = 99;
const FRENCH: i32 = 2;
const SPANISH: i32 = 5;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn lower(unit: u16) -> u16 {
    case(unit, include_bytes!("char-lower.bin"))
}

fn upper(unit: u16) -> u16 {
    case(unit, include_bytes!("char-upper.bin"))
}

fn is_title(unit: u16) -> bool {
    matches!(unit, 0x1c5 | 0x1c8 | 0x1cb | 0x1f2 | 0x1f88..=0x1f8f | 0x1f98..=0x1f9f | 0x1fa8..=0x1faf | 0x1fbc | 0x1fcc | 0x1ffc)
}

fn is_upper(unit: u16) -> bool {
    char::from_u32(u32::from(unit)).is_some_and(char::is_uppercase)
}

fn collation_key(unit: u16) -> i32 {
    if is_upper(unit) || is_title(unit) {
        (i32::from(lower(unit)) << 4) + 1
    } else {
        i32::from(unit) << 4
    }
}

fn ligature(unit: u16) -> u16 {
    match unit {
        198 | 338 => 69,
        230 | 339 => 101,
        223 => 115,
        _ => 0,
    }
}

fn strip_diacritics(unit: u16, language: i32) -> u16 {
    if (0xc0..=0xff).contains(&unit) {
        let result = match unit {
            0xc0..=0xc6 => Some(b'A'),
            0xc7 => Some(b'C'),
            0xc8..=0xcb => Some(b'E'),
            0xcc..=0xcf => Some(b'I'),
            0xd1 if language != SPANISH => Some(b'N'),
            0xd2..=0xd6 => Some(b'O'),
            0xd9..=0xdc => Some(b'U'),
            0xdd => Some(b'Y'),
            0xdf => Some(b's'),
            0xe0..=0xe6 => Some(b'a'),
            0xe7 => Some(b'c'),
            0xe8..=0xeb => Some(b'e'),
            0xec..=0xef => Some(b'i'),
            0xf1 if language != SPANISH => Some(b'n'),
            0xf2..=0xf6 => Some(b'o'),
            0xf9..=0xfc => Some(b'u'),
            0xfd | 0xff => Some(b'y'),
            _ => None,
        };
        if let Some(letter) = result {
            return u16::from(letter);
        }
    }
    match unit {
        0x152 => 79,
        0x153 => 111,
        0x178 => 89,
        other => other,
    }
}

fn differs(left: u16, right: u16) -> bool {
    left != right && upper(left) != upper(right)
}

pub(super) fn compare_locale_aware(first: &[u16], second: &[u16], language: i32) -> i32 {
    let length1 = first.len() as i32;
    let length2 = second.len() as i32;
    let (mut index1, mut index2) = (0i32, 0i32);
    let (mut pending1, mut pending2) = (0u16, 0u16);
    while index1 - i32::from(pending1) < length1 || index2 - i32::from(pending2) < length2 {
        if index1 - i32::from(pending1) >= length1 {
            return -1;
        }
        if index2 - i32::from(pending2) >= length2 {
            return 1;
        }
        let c1 = if pending1 != 0 {
            pending1
        } else {
            index1 += 1;
            first[(index1 - 1) as usize]
        };
        let c2 = if pending2 != 0 {
            pending2
        } else {
            index2 += 1;
            second[(index2 - 1) as usize]
        };
        pending1 = ligature(c1);
        pending2 = ligature(c2);
        let stripped1 = strip_diacritics(c1, language);
        let stripped2 = strip_diacritics(c2, language);
        if differs(stripped1, stripped2) {
            let lower1 = lower(stripped1);
            let lower2 = lower(stripped2);
            if lower2 != lower1 {
                return collation_key(lower1) - collation_key(lower2);
            }
        }
    }
    let minimum = length1.min(length2);
    for i in 0..minimum {
        let (index1, index2) = if language == FRENCH {
            (length1 - 1 - i, length2 - 1 - i)
        } else {
            (i, i)
        };
        let c1 = first[index1 as usize];
        let c2 = second[index2 as usize];
        if differs(c1, c2) {
            let lower1 = lower(c1);
            let lower2 = lower(c2);
            if lower1 != lower2 {
                return collation_key(lower1) - collation_key(lower2);
            }
        }
    }
    let difference = length1 - length2;
    if difference != 0 {
        return difference;
    }
    for i in 0..minimum as usize {
        if first[i] != second[i] {
            return collation_key(first[i]) - collation_key(second[i]);
        }
    }
    0
}

impl Vm {
    pub(super) fn is_message_opcode(opcode: Opcode) -> bool {
        matches!(
            opcode,
            Opcode::COMPARE | Opcode::MES | Opcode::MES_TYPED | Opcode::COORD
        )
    }

    pub(super) fn execute_message(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        match opcode {
            Opcode::COMPARE => {
                let second = pop(&mut self.strings, StackKind::String)?;
                let first = pop(&mut self.strings, StackKind::String)?;
                let language = host.language().map_err(&unavailable)?;
                let (Some(first), Some(second), Some(language)) = (first, second, language) else {
                    return Err(VmErrorKind::Host(HostError::NoEntity));
                };
                let first: Vec<u16> = first.encode_utf16().collect();
                let second: Vec<u16> = second.encode_utf16().collect();
                push(
                    &mut self.ints,
                    compare_locale_aware(&first, &second, language),
                    StackKind::Int,
                )
            }
            Opcode::MES => {
                let text = pop(&mut self.strings, StackKind::String)?;
                let outbox = host.outbox().map_err(&unavailable)?;
                let text = text.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                outbox.typed_messages.push(TypedMessage {
                    message_type: 0,
                    effect_flags: 0,
                    text: text.to_string(),
                });
                Ok(())
            }
            Opcode::MES_TYPED => {
                let effect_flags = pop(&mut self.ints, StackKind::Int)?;
                let message_type = pop(&mut self.ints, StackKind::Int)?;
                let text = pop(&mut self.strings, StackKind::String)?;
                let outbox = host.outbox().map_err(&unavailable)?;
                let text: Arc<str> = text.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let resolved = if (0..CHAT_TYPE_COUNT).contains(&message_type) {
                    message_type
                } else {
                    GAME
                };
                match resolved {
                    DEV_CONSOLE => outbox.console_lines.push(text.to_string()),
                    DEV_CONSOLE_CLEAR => outbox.console_clears.push(text.to_string()),
                    _ => outbox.typed_messages.push(TypedMessage {
                        message_type: resolved,
                        effect_flags,
                        text: text.to_string(),
                    }),
                }
                Ok(())
            }
            _ => {
                let coord = host
                    .entity_state()
                    .map_err(&unavailable)?
                    .my_coord
                    .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                push(&mut self.ints, coord.packed(), StackKind::Int)
            }
        }
    }
}
