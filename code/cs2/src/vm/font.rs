use super::{pop, push, Vm};
use crate::fonts::Surroundings;
use crate::{FontMetrics, HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

fn units(text: &str) -> Vec<u16> {
    text.encode_utf16().collect()
}

impl Vm {
    /// STRINGWIDTH, PARAWIDTH, PARAHEIGHT and PARAHEIGHT_EXTRA. The Kotlin dispatch bumps the int
    /// stack pointer before it evaluates a result that can throw, so a missing font or text leaves
    /// the slot it was about to overwrite (the font id for STRINGWIDTH, the width otherwise).
    pub(super) fn execute_font(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host: &dyn VarHost = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let needed = if opcode == STRINGWIDTH { 1 } else { 2 };
        if self.ints.len() < needed || self.strings.is_empty() {
            return Err(match host.font_metrics(0) {
                Err(HostError::Unavailable) => VmErrorKind::UnsupportedOpcode(opcode),
                _ if self.ints.len() < needed => VmErrorKind::StackUnderflow(Int),
                _ => VmErrorKind::StackUnderflow(Text),
            });
        }
        let text = pop(&mut self.strings, Text)?;
        if opcode == PARAHEIGHT_EXTRA && text.is_none() {
            return Err(VmErrorKind::InvalidOperand);
        }
        let (width, font_id) = if opcode == STRINGWIDTH {
            (0, pop(&mut self.ints, Int)?)
        } else {
            let font_id = pop(&mut self.ints, Int)?;
            (pop(&mut self.ints, Int)?, font_id)
        };
        let font: Option<Arc<FontMetrics>> = lookup(opcode, host.font_metrics(font_id))?;
        let stale = if opcode == STRINGWIDTH { font_id } else { width };
        let missing = font.is_none() || (opcode != STRINGWIDTH && text.is_none());
        let font = match font {
            Some(font) if !missing => font,
            _ => {
                if opcode != PARAHEIGHT_EXTRA {
                    push(&mut self.ints, stale, Int)?;
                }
                return Err(VmErrorKind::InvalidOperand);
            }
        };
        let text = text.as_deref().map(units).unwrap_or_default();
        let mut around = Surroundings::new(host, opcode != PARAHEIGHT_EXTRA);
        let value = match opcode {
            STRINGWIDTH => font.text_width(&text, &mut around),
            PARAWIDTH => font.text_width_multiline(&text, width, &mut around),
            PARAHEIGHT => font.line_count(&text, width, &mut around),
            _ => font.inline_extra_height(&text, width, &mut around),
        };
        if let Some(error) = around.error {
            return Err(match error {
                HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
                error => VmErrorKind::Host(error),
            });
        }
        push(&mut self.ints, value, Int)
    }
}
