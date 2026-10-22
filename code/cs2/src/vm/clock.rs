use super::{pop, push, Vm};
use crate::{calendar, HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

/// `ColorUtil.adjustHsv`, the packed 16 bit colour the table is indexed with.
fn adjust_hsv(value: i32) -> i32 {
    let hue = (value >> 10) & 0x3f;
    let saturation = (value >> 3) & 0x70;
    let brightness = value & 0x7f;
    let chroma = if brightness <= 64 {
        (brightness * saturation) >> 7
    } else {
        (saturation * (127 - brightness)) >> 7
    };
    let total = chroma + brightness;
    let new_saturation = if total == 0 { chroma << 1 } else { (chroma << 8) / total };
    i32::from(((hue << 10) | ((new_saturation >> 4) << 7) | total) as i16)
}

impl Vm {
    pub(super) fn execute_clock(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let needed = match opcode {
            RANDOM_SOUND_PITCH => 2,
            DATE_MINUTES | DATE_RUNEDAY | DATE_YEAR => 0,
            _ => 1,
        };
        if self.ints.len() < needed {
            return Err(match host.clock_millis() {
                Err(HostError::Unavailable) => VmErrorKind::UnsupportedOpcode(opcode),
                _ => VmErrorKind::StackUnderflow(Int),
            });
        }
        match opcode {
            RANDOM | RANDOMINC => {
                let value = pop(&mut self.ints, Int)?;
                let bound = if opcode == RANDOM { value } else { value.wrapping_add(1) };
                let draw = lookup(opcode, host.random_unit())?;
                push(&mut self.ints, (draw * f64::from(bound)) as i32, Int)
            }
            RANDOM_SOUND_PITCH => {
                let up = pop(&mut self.ints, Int)?;
                let down = pop(&mut self.ints, Int)?;
                if down > 700 || up > 700 {
                    return push(&mut self.ints, 256, Int);
                }
                let draw = lookup(opcode, host.random_unit())?;
                let octaves =
                    (draw * f64::from(up.wrapping_add(down)) - f64::from(down) + 800.0) / 100.0;
                push(&mut self.ints, (octaves.exp2() + 0.5) as i32, Int)
            }
            HSVTORGB => {
                let value = *self.ints.last().expect("length checked");
                let table = lookup(opcode, host.hsv_to_rgb_table())?;
                let table = table.ok_or(VmErrorKind::InvalidOperand)?;
                let index = (adjust_hsv(value) & 0xffff) as usize;
                let colour = *table.get(index).ok_or(VmErrorKind::InvalidOperand)?;
                self.ints.pop();
                push(&mut self.ints, colour, Int)
            }
            DATE_MINUTES => {
                let time = lookup(opcode, host.clock_millis())?;
                push(&mut self.ints, (time / 60_000) as i32, Int)
            }
            DATE_RUNEDAY => {
                let time = lookup(opcode, host.clock_millis())?;
                push(&mut self.ints, (time / 86_400_000 - 11_745) as i32, Int)
            }
            DATE_YEAR => {
                let time = lookup(opcode, host.clock_millis())?;
                let offset = lookup(opcode, host.local_offset_millis(time))?;
                let fields = calendar::fields(time.saturating_add(offset));
                push(&mut self.ints, fields.year, Int)
            }
            DATE_RUNEDAY_TODATE => {
                let days = pop(&mut self.ints, Int)?;
                let utc = i64::from(days.wrapping_add(11_745)) * 86_400_000;
                let offset = lookup(opcode, host.local_offset_millis(utc))?;
                let fields = calendar::fields(utc.saturating_add(offset));
                push(&mut self.ints, fields.day, Int)?;
                push(&mut self.ints, fields.month, Int)?;
                push(&mut self.ints, fields.year, Int)
            }
            FROMDATE => {
                let days = pop(&mut self.ints, Int)?;
                let language = lookup(opcode, host.language())?;
                let utc = i64::from(days.wrapping_add(11_745)) * 86_400_000;
                let offset = lookup(opcode, host.local_offset_millis(utc))?;
                let fields = calendar::fields(utc.saturating_add(offset));
                let text = match language {
                    Some(3) => Some(calendar::slash_date(&fields)),
                    Some(language) => calendar::dash_date(&fields, language),
                    None => None,
                };
                match text {
                    Some(text) => push(&mut self.strings, Some(Arc::from(text)), Text),
                    None => {
                        self.strings.push(None);
                        Err(VmErrorKind::InvalidOperand)
                    }
                }
            }
            FORMAT_DATETIME_FROM_MINUTES => {
                let minutes = pop(&mut self.ints, Int)?;
                let language = lookup(opcode, host.language())?;
                let fields = calendar::fields(i64::from(minutes) * 60_000);
                let date = match language {
                    Some(3) => Some(calendar::slash_date(&fields)),
                    Some(language) => calendar::dash_date(&fields, language),
                    None => None,
                };
                match date {
                    Some(date) => {
                        let text = format!("{date} {} UTC", calendar::time_of(&fields));
                        push(&mut self.strings, Some(Arc::from(text)), Text)
                    }
                    None => {
                        self.strings.push(None);
                        Err(VmErrorKind::InvalidOperand)
                    }
                }
            }
            _ => unreachable!(),
        }
    }
}
