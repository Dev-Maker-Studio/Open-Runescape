use super::{check_string, push, Vm};
use crate::{
    HostError, Opcode, QuickChatFault, QuickChatSearch, StackKind, VarHost, VmErrorKind,
};
use std::sync::Arc;

fn lookup<T>(opcode: Opcode, result: Result<T, HostError>) -> Result<T, VmErrorKind> {
    result.map_err(|error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    })
}

fn null() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

fn bounds(_: QuickChatFault) -> VmErrorKind {
    VmErrorKind::Host(HostError::State("index out of bounds".to_string()))
}

fn element<T: Copy>(list: &Option<Vec<T>>, index: i32) -> Result<T, VmErrorKind> {
    let list = list.as_ref().ok_or_else(null)?;
    usize::try_from(index)
        .ok()
        .and_then(|index| list.get(index))
        .copied()
        .ok_or_else(|| bounds(QuickChatFault::Index))
}

impl Vm {
    pub(super) fn is_quick_chat_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CHATCAT_GETDESC
                | CHATCAT_GETSUBCATCOUNT
                | CHATCAT_GETSUBCAT
                | CHATCAT_GETPHRASECOUNT
                | CHATCAT_GETPHRASE
                | CHATCAT_GETSUBCATSHORTCUT
                | CHATCAT_GETPHRASESHORTCUT
                | CHATCAT_FINDSUBCATBYSHORTCUT
                | CHATCAT_FINDPHRASEBYSHORTCUT
                | CHATPHRASE_GETTEXT
                | CHATPHRASE_GETAUTORESPONSECOUNT
                | CHATPHRASE_GETAUTORESPONSE
                | CHATPHRASE_GETDYNAMICCOMMANDCOUNT
                | CHATPHRASE_GETDYNAMICCOMMAND
                | CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM
                | CHATPHRASE_FIND
                | CHATPHRASE_FINDNEXT
                | CHATPHRASE_FINDRESTART
        )
    }

    pub(super) fn execute_quick_chat(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        lookup(opcode, host.quick_chat().map(|_| ()))?;
        let (ints, strings) = match opcode {
            CHATCAT_GETSUBCAT
            | CHATCAT_GETPHRASE
            | CHATCAT_GETSUBCATSHORTCUT
            | CHATCAT_GETPHRASESHORTCUT
            | CHATCAT_FINDSUBCATBYSHORTCUT
            | CHATCAT_FINDPHRASEBYSHORTCUT
            | CHATPHRASE_GETAUTORESPONSE
            | CHATPHRASE_GETDYNAMICCOMMAND => (2, 0),
            CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM => (3, 0),
            CHATPHRASE_FIND => (1, 1),
            CHATPHRASE_FINDNEXT | CHATPHRASE_FINDRESTART => (0, 0),
            _ => (1, 0),
        };
        if self.ints.len() < ints {
            return Err(VmErrorKind::StackUnderflow(Int));
        }
        if self.strings.len() < strings {
            return Err(VmErrorKind::StackUnderflow(Text));
        }
        let int_base = self.ints.len() - ints;
        let string_base = self.strings.len() - strings;
        let a: Vec<i32> = self.ints[int_base..].to_vec();
        let s: Vec<Option<Arc<str>>> = self.strings[string_base..].to_vec();
        self.ints.truncate(int_base);
        self.strings.truncate(string_base);
        if opcode == CHATCAT_GETDESC {
            let name = lookup(opcode, host.quick_chat())?.category(a[0]).name.clone();
            check_string(name.as_deref(), self.limits.max_string_bytes)?;
            return push(&mut self.strings, name, Text);
        }
        if opcode == CHATPHRASE_GETTEXT {
            let text = lookup(opcode, host.quick_chat())?.message(a[0]).placeholder_text();
            check_string(Some(&text), self.limits.max_string_bytes)?;
            return push(&mut self.strings, Some(text), Text);
        }
        if opcode == CHATPHRASE_FIND {
            let query = s[0].clone().ok_or_else(null)?;
            let outcome = lookup(opcode, host.quick_chat())?.search(&query, a[0] == 1);
            let results = &mut lookup(opcode, host.items())?.query;
            match outcome {
                QuickChatSearch::Overflow => {
                    results.count = -1;
                    results.ids = None;
                }
                QuickChatSearch::Found(ids) => {
                    results.count = ids.len() as i32;
                    results.ids = Some(ids);
                    results.cursor = 0;
                }
            }
            let count = results.count;
            return push(&mut self.ints, count, Int);
        }
        if opcode == CHATPHRASE_FINDRESTART {
            lookup(opcode, host.items())?.find_restart();
            return Ok(());
        }
        if opcode == CHATPHRASE_FINDNEXT {
            let value = lookup(opcode, host.items())?.find_next();
            return push(&mut self.ints, value, Int);
        }
        let defs = lookup(opcode, host.quick_chat())?;
        let outcome: Result<i32, VmErrorKind> = match opcode {
            CHATCAT_GETSUBCATCOUNT => Ok(defs
                .category(a[0])
                .sub_categories
                .as_ref()
                .map_or(0, |list| list.len() as i32)),
            CHATCAT_GETPHRASECOUNT => Ok(defs
                .category(a[0])
                .messages
                .as_ref()
                .map_or(0, |list| list.len() as i32)),
            CHATCAT_GETSUBCAT => element(&defs.category(a[0]).sub_categories, a[1]),
            CHATCAT_GETPHRASE => element(&defs.category(a[0]).messages, a[1]),
            CHATCAT_GETSUBCATSHORTCUT => {
                element(&defs.category(a[0]).sub_category_hotkeys, a[1]).map(i32::from)
            }
            CHATCAT_GETPHRASESHORTCUT => {
                element(&defs.category(a[0]).message_hotkeys, a[1]).map(i32::from)
            }
            CHATCAT_FINDSUBCATBYSHORTCUT => Ok(if a[1] == -1 {
                -1
            } else {
                defs.category(a[0]).sub_category_for_hotkey(a[1] as u16)
            }),
            CHATCAT_FINDPHRASEBYSHORTCUT => Ok(if a[1] == -1 {
                -1
            } else {
                defs.category(a[0]).message_for_hotkey(a[1] as u16)
            }),
            CHATPHRASE_GETAUTORESPONSECOUNT => Ok(defs
                .message(a[0])
                .responses
                .as_ref()
                .map_or(0, |list| list.len() as i32)),
            CHATPHRASE_GETAUTORESPONSE => element(&defs.message(a[0]).responses, a[1]),
            CHATPHRASE_GETDYNAMICCOMMANDCOUNT => Ok(defs.message(a[0]).value_type_count()),
            CHATPHRASE_GETDYNAMICCOMMAND => defs
                .message(a[0])
                .value_type(a[1])
                .map_err(bounds)
                .and_then(|kind| kind.map(|kind| kind.id).ok_or_else(null)),
            CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM => {
                match defs.message(a[0]).value_type(a[1]).map_err(bounds) {
                    Err(error) => Err(error),
                    Ok(None) => Err(null()),
                    Ok(Some(kind)) if kind.id != 0 => Err(VmErrorKind::InvalidOperand),
                    Ok(Some(_)) => defs
                        .message(a[0])
                        .value_config(a[1], a[2])
                        .map_err(bounds),
                }
            }
            _ => unreachable!(),
        };
        let push_first = matches!(
            opcode,
            CHATCAT_GETPHRASE
                | CHATCAT_GETSUBCATSHORTCUT
                | CHATCAT_GETPHRASESHORTCUT
                | CHATPHRASE_GETAUTORESPONSE
        );
        match outcome {
            Ok(value) => push(&mut self.ints, value, Int),
            Err(error) => {
                let config_fault = opcode == CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM
                    && matches!(&error, VmErrorKind::Host(HostError::State(_)))
                    && defs
                        .message(a[0])
                        .value_type(a[1])
                        .is_ok_and(|kind| kind.is_some_and(|kind| kind.id == 0));
                if push_first || config_fault {
                    self.ints.push(a[0]);
                }
                Err(error)
            }
        }
    }
}
