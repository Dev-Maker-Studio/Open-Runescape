use super::{pop, push, Vm};
use crate::outbox::{chat_set_filter, chat_set_mode, report_abuse, Effects};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn truncate_utf16(text: &str, limit: usize) -> String {
    let units: Vec<u16> = text.encode_utf16().collect();
    if units.len() > limit {
        String::from_utf16_lossy(&units[..limit])
    } else {
        text.to_string()
    }
}

impl Vm {
    pub(super) fn is_chat_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CHAT_GETFILTER_PUBLIC
                | CHAT_GETFILTER_PRIVATE
                | CHAT_GETFILTER_TRADE
                | CHAT_SETFILTER
                | CHAT_SETMODE
                | CHAT_SENDABUSEREPORT
                | CHAT_GETHISTORYMESSAGE
                | CHAT_GETHISTORYTYPE
                | CHAT_GETHISTORYNAME
                | CHAT_GETHISTORYCLAN
                | CHAT_GETHISTORYPHRASE
                | CHAT_GETHISTORYLENGTH
                | CHATLINE_GETEFFECTFLAGS
                | CHATLINE_GETUID
                | CHATLINE_GETSIMPLENAME
                | CHAT_GETPREVUID
                | UNKNOWN_COMMAND_5019
        )
    }

    fn push_chat_int(&mut self, value: i32) -> Result<(), VmErrorKind> {
        push(&mut self.ints, value, StackKind::Int)
    }

    fn push_chat_text(&mut self, value: &str) -> Result<(), VmErrorKind> {
        push(&mut self.strings, Some(Arc::from(value)), StackKind::String)
    }

    pub(super) fn execute_chat(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        match opcode {
            CHAT_GETFILTER_PUBLIC => {
                let value = host.chat_state().map_err(&unavailable)?.public_filter;
                self.push_chat_int(value)
            }
            CHAT_GETFILTER_PRIVATE => {
                let value = host
                    .chat_state()
                    .map_err(&unavailable)?
                    .private_filter
                    .unwrap_or(-1);
                self.push_chat_int(value)
            }
            CHAT_GETFILTER_TRADE => {
                let value = host.chat_state().map_err(&unavailable)?.trade_filter;
                self.push_chat_int(value)
            }
            CHAT_GETHISTORYLENGTH => {
                let value = host.chat_state().map_err(&unavailable)?.line_count;
                self.push_chat_int(value)
            }
            CHAT_SETFILTER => {
                let trade = pop(&mut self.ints, StackKind::Int)?;
                let private = pop(&mut self.ints, StackKind::Int)?;
                let public = pop(&mut self.ints, StackKind::Int)?;
                let state = host.chat_state_mut().map_err(&unavailable)?;
                let private = state.set_filters(public, private, trade);
                let packet =
                    chat_set_filter(public, private, trade).map_err(VmErrorKind::Host)?;
                let mut effects = Effects::default();
                effects.packets.push(packet);
                effects.apply(host.outbox().map_err(&unavailable)?);
                Ok(())
            }
            CHAT_SETMODE => {
                let mode = pop(&mut self.ints, StackKind::Int)?;
                let packet = chat_set_mode(mode).map_err(VmErrorKind::Host)?;
                let mut effects = Effects::default();
                effects.packets.push(packet);
                effects.apply(host.outbox().map_err(&unavailable)?);
                Ok(())
            }
            CHAT_SENDABUSEREPORT => {
                let comment = pop(&mut self.strings, StackKind::String)?;
                let reported = pop(&mut self.strings, StackKind::String)?;
                let mute = pop(&mut self.ints, StackKind::Int)?;
                let rule = pop(&mut self.ints, StackKind::Int)?;
                let comment = truncate_utf16(comment.as_deref().unwrap_or(""), 80);
                let reported = reported.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let packet =
                    report_abuse(&reported, &comment, rule, mute).map_err(VmErrorKind::Host)?;
                let mut effects = Effects::default();
                effects.packets.push(packet);
                effects.apply(host.outbox().map_err(&unavailable)?);
                Ok(())
            }
            _ => {
                let index = pop(&mut self.ints, StackKind::Int)?;
                let chat = host.chat_state().map_err(&unavailable)?;
                let line = chat.line(index);
                match opcode {
                    CHAT_GETHISTORYTYPE => {
                        let value = match line {
                            Some(line) => line
                                .kind
                                .ok_or(VmErrorKind::Host(HostError::NoEntity))?,
                            None => -1,
                        };
                        self.push_chat_int(value)
                    }
                    CHAT_GETHISTORYPHRASE => {
                        self.push_chat_int(line.map_or(-1, |line| line.quickchat_message_id))
                    }
                    CHAT_GETPREVUID => self.push_chat_int(line.map_or(-1, |line| line.time)),
                    CHATLINE_GETUID => self.push_chat_int(line.map_or(-1, |line| line.identifier)),
                    CHATLINE_GETEFFECTFLAGS => {
                        self.push_chat_int(line.map_or(0, |line| line.effect_flags))
                    }
                    CHATLINE_GETSIMPLENAME => {
                        let text = line.and_then(|line| line.simple_name.clone());
                        self.push_chat_text(text.as_deref().unwrap_or(""))
                    }
                    CHAT_GETHISTORYMESSAGE => {
                        let text = line.and_then(|line| line.message.clone());
                        self.push_chat_text(text.as_deref().unwrap_or(""))
                    }
                    CHAT_GETHISTORYCLAN => {
                        let text = line.and_then(|line| line.clan.clone());
                        self.push_chat_text(text.as_deref().unwrap_or(""))
                    }
                    CHAT_GETHISTORYNAME => {
                        let text = line.and_then(|line| line.crowned_name.clone());
                        self.push_chat_text(text.as_deref().unwrap_or(""))
                    }
                    UNKNOWN_COMMAND_5019 => {
                        let text = line.and_then(|line| line.name.clone());
                        self.push_chat_text(text.as_deref().unwrap_or(""))
                    }
                    _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
                }
            }
        }
    }
}
