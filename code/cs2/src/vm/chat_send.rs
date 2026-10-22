use super::{pop, push, Vm};
use crate::outbox::{ChatSend, Effects};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

const SHOW_COMMAND: &str = "/openrs-show";

const COLORS: [(u8, [&str; 4]); 12] = [
    (0, ["yellow:", "gelb:", "jaune:", "amarelo:"]),
    (1, ["red:", "rot:", "rouge:", "vermelho:"]),
    (2, ["green:", "grün:", "vert:", "verde:"]),
    (3, ["cyan:", "blaugrün:", "cyan:", "cyan:"]),
    (4, ["purple:", "lila:", "violet:", "roxo:"]),
    (5, ["white:", "weiss:", "blanc:", "branco:"]),
    (6, ["flash1:", "blinken1:", "clignotant1:", "flash1:"]),
    (7, ["flash2:", "blinken2:", "clignotant2:", "flash2:"]),
    (8, ["flash3:", "blinken3:", "clignotant3:", "brilho3:"]),
    (9, ["glow1:", "leuchten1:", "brillant1:", "brilho1:"]),
    (10, ["glow2:", "leuchten2:", "brillant2:", "brilho2:"]),
    (11, ["glow3:", "leuchten3:", "brillant3:", "brilho3:"]),
];

const EFFECTS: [(u8, [&str; 4]); 5] = [
    (1, ["wave:", "welle:", "ondulation:", "onda:"]),
    (2, ["wave2:", "welle2:", "ondulation2:", "onda2:"]),
    (3, ["shake:", "schütteln:", "tremblement:", "tremor:"]),
    (4, ["scroll:", "scrollen:", "déroulement:", "rolagem:"]),
    (5, ["slide:", "gleiten:", "glissement:", "deslizamento:"]),
];

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn strip(message: &str, prefix: &str) -> String {
    let units: Vec<u16> = message.encode_utf16().collect();
    String::from_utf16_lossy(&units[prefix.encode_utf16().count().min(units.len())..])
}

fn scan(
    message: &str,
    table: &[(u8, [&str; 4])],
    language: &dyn Fn() -> Result<Option<i32>, VmErrorKind>,
) -> Result<(u8, String), VmErrorKind> {
    let lower = message.to_lowercase();
    for (value, names) in table {
        if lower.starts_with(names[0]) {
            return Ok((*value, strip(message, names[0])));
        }
    }
    match language()? {
        None => Err(VmErrorKind::Host(HostError::NoEntity)),
        Some(0) => Ok((0, message.to_string())),
        Some(id @ 1..=3) => {
            for (value, names) in table {
                let name = names[id as usize];
                if lower.starts_with(name) {
                    return Ok((*value, strip(message, name)));
                }
            }
            Ok((0, message.to_string()))
        }
        Some(_) => Err(VmErrorKind::Host(HostError::State(
            "Unknown language".to_string(),
        ))),
    }
}

impl Vm {
    pub(super) fn is_chat_send_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CHAT_SENDPUBLIC | CHAT_SENDPRIVATE | CHAT_PLAYERNAME | CHAT_PLAYERNAME_UNFILTERED
        )
    }

    pub(super) fn execute_chat_send(
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
            CHAT_PLAYERNAME | CHAT_PLAYERNAME_UNFILTERED => {
                let player = host.player_state().map_err(&unavailable)?;
                let text = match player.my_player.as_ref() {
                    Some(mine) if mine.username.is_some() => {
                        if opcode == CHAT_PLAYERNAME {
                            mine.display_name.clone()
                        } else {
                            Some(mine.username_with_title())
                        }
                    }
                    _ => Some(String::new()),
                };
                push(
                    &mut self.strings,
                    text.map(|text| Arc::from(text.as_str())),
                    StackKind::String,
                )
            }
            CHAT_SENDPUBLIC => {
                let message = pop(&mut self.strings, StackKind::String)?;
                if message
                    .as_deref()
                    .is_some_and(|text| text.trim() == SHOW_COMMAND)
                {
                    return Ok(());
                }
                if !Self::chat_permitted(host, &unavailable)? {
                    return Ok(());
                }
                let message = message.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let language = || host.language().map_err(&unavailable);
                let (color, rest) = scan(&message, &COLORS, &language)?;
                let (effect, rest) = scan(&rest, &EFFECTS, &language)?;
                let mut effects = Effects::default();
                effects.chat_sends.push(ChatSend::Public {
                    color,
                    effect,
                    message: rest,
                });
                effects.apply(host.outbox().map_err(&unavailable)?);
                Ok(())
            }
            _ => {
                let message = pop(&mut self.strings, StackKind::String)?;
                let recipient = pop(&mut self.strings, StackKind::String)?;
                if !Self::chat_permitted(host, &unavailable)? {
                    return Ok(());
                }
                let recipient = recipient.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let mut effects = Effects::default();
                effects.chat_sends.push(ChatSend::Private {
                    recipient: recipient.to_string(),
                    message: message.as_deref().unwrap_or("").to_string(),
                });
                effects.apply(host.outbox().map_err(&unavailable)?);
                Ok(())
            }
        }
    }

    fn chat_permitted(
        host: &mut dyn VarHost,
        unavailable: &dyn Fn(HostError) -> VmErrorKind,
    ) -> Result<bool, VmErrorKind> {
        if host.player_state().map_err(unavailable)?.player_rights != 0 {
            return Ok(true);
        }
        let client = host.client().map_err(unavailable)?;
        Ok((!client.userdetail_quickchat || client.verified_email_address) && !client.quickchat_only)
    }
}
