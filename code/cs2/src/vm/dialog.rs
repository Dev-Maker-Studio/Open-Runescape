use super::{pop, Vm};
use crate::outbox::{
    resume_count, resume_short, resume_text, Effects, GRAND_EXCHANGE_ITEM_SELECT as PROT_ITEM,
    RESUME_CLANFORUMQFCDIALOG as PROT_CLAN, RESUME_HSLDIALOG as PROT_HSL,
    RESUME_NAMEDIALOG as PROT_NAME, RESUME_TEXTDIALOG as PROT_TEXT,
};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

fn parse_decimal(text: &str) -> Option<i32> {
    let mut negative = false;
    let mut digits = false;
    let mut result: i32 = 0;
    for (index, unit) in text.encode_utf16().enumerate() {
        if index == 0 {
            if unit == u16::from(b'-') {
                negative = true;
                continue;
            }
            if unit == u16::from(b'+') {
                continue;
            }
        }
        let digit = match u8::try_from(unit) {
            Ok(byte @ b'0'..=b'9') => i32::from(byte - b'0'),
            _ => return None,
        };
        let signed = if negative { -digit } else { digit };
        let accumulated = signed.wrapping_add(result.wrapping_mul(10));
        if accumulated.wrapping_div(10) != result {
            return None;
        }
        result = accumulated;
        digits = true;
    }
    digits.then_some(result)
}

impl Vm {
    pub(super) fn is_dialog_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            RESUME_COUNTDIALOG
                | RESUME_NAMEDIALOG
                | RESUME_STRINGDIALOG
                | RESUME_CLANFORUMQFCDIALOG
                | RESUME_OBJDIALOG
                | RESUME_HSLDIALOG
        )
    }

    pub(super) fn execute_dialog(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let packet = match opcode {
            RESUME_COUNTDIALOG => {
                let text = pop(&mut self.strings, StackKind::String)?
                    .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                resume_count(parse_decimal(&text).unwrap_or(0))
            }
            RESUME_OBJDIALOG | RESUME_HSLDIALOG => {
                let value = pop(&mut self.ints, StackKind::Int)?;
                let code = if opcode == RESUME_OBJDIALOG {
                    PROT_ITEM
                } else {
                    PROT_HSL
                };
                resume_short(code, value)
            }
            _ => {
                let text = pop(&mut self.strings, StackKind::String)?
                    .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let code = match opcode {
                    RESUME_NAMEDIALOG => PROT_NAME,
                    RESUME_STRINGDIALOG => PROT_TEXT,
                    _ => PROT_CLAN,
                };
                resume_text(code, &text)
            }
        }
        .map_err(VmErrorKind::Host)?;
        let mut effects = Effects::default();
        effects.packets.push(packet);
        let outbox = host.outbox().map_err(|error| match error {
            HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
            error => VmErrorKind::Host(error),
        })?;
        effects.apply(outbox);
        Ok(())
    }
}
