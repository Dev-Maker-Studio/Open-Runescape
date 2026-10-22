use super::{pop, Vm};
use crate::outbox::{interface_on_player, player_operation, Effects, RouteRequest};
use crate::{ComponentError, HostError, Opcode, StackKind, VarHost, VmErrorKind};

const UNABLE_TO_FIND: [&str; 4] = [
    "Unable to find ",
    "Spieler kann nicht gefunden werden: ",
    "Impossible de trouver ",
    "Não é possível encontrar ",
];

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn simple_upper(unit: u16) -> u16 {
    let Some(character) = char::from_u32(u32::from(unit)) else {
        return unit;
    };
    let mut upper = character.to_uppercase();
    match (upper.next(), upper.next()) {
        (Some(single), None) => u16::try_from(u32::from(single)).unwrap_or(unit),
        _ => unit,
    }
}

fn simple_lower(unit: u16) -> u16 {
    if unit == 0x130 {
        return 0x69;
    }
    let Some(character) = char::from_u32(u32::from(unit)) else {
        return unit;
    };
    let mut lower = character.to_lowercase();
    match (lower.next(), lower.next()) {
        (Some(single), None) => u16::try_from(u32::from(single)).unwrap_or(unit),
        _ => unit,
    }
}

fn equals_ignore_case(left: &str, right: &str) -> bool {
    let left: Vec<u16> = left.encode_utf16().collect();
    let right: Vec<u16> = right.encode_utf16().collect();
    left.len() == right.len()
        && left.iter().zip(&right).all(|(a, b)| {
            if a == b {
                return true;
            }
            let (upper_a, upper_b) = (simple_upper(*a), simple_upper(*b));
            upper_a == upper_b || simple_lower(upper_a) == simple_lower(upper_b)
        })
}

impl Vm {
    pub(super) fn is_player_op_opcode(opcode: Opcode) -> bool {
        matches!(opcode, Opcode::OPPLAYER | Opcode::OPPLAYERT)
    }

    pub(super) fn execute_player_op(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        if opcode == Opcode::OPPLAYERT {
            return self.execute_player_target(host);
        }
        let mut effects = Effects::default();
        let operation = pop(&mut self.ints, StackKind::Int)?;
        let name = pop(&mut self.strings, StackKind::String)?;
        let state = host.entity_state().map_err(&unavailable)?;
        let mut found = false;
        for player in &state.nearby_players {
            if !player.present {
                continue;
            }
            if !state.has_my_player {
                return Err(VmErrorKind::Host(HostError::NoEntity));
            }
            let matches = !player.is_self
                && match (&player.display_name, &name) {
                    (Some(display), Some(wanted)) => equals_ignore_case(display, wanted),
                    _ => false,
                };
            if matches {
                if let Some(packet) = player_operation(operation, player.index)
                    .map_err(VmErrorKind::Host)?
                {
                    effects.packets.push(packet);
                }
                found = true;
                break;
            }
        }
        if !found {
            let prefix = match host.language().map_err(&unavailable)? {
                None => return Err(VmErrorKind::Host(HostError::NoEntity)),
                Some(id @ 0..=3) => UNABLE_TO_FIND[id as usize],
                Some(_) => {
                    return Err(VmErrorKind::Host(HostError::State(
                        "Unknown language".to_string(),
                    )))
                }
            };
            effects
                .game_messages
                .push(format!("{prefix}{}", name.as_deref().unwrap_or("null")));
        }
        let outbox = host.outbox().map_err(unavailable)?;
        effects.apply(outbox);
        Ok(())
    }

    fn execute_player_target(
        &mut self,
        host: &mut dyn VarHost,
    ) -> Result<(), VmErrorKind> {
        let opcode = Opcode::OPPLAYERT;
        let unavailable = host_error(opcode);
        let name = pop(&mut self.strings, StackKind::String)?;
        let selection = host.components().map_err(&unavailable)?.selection();
        if !selection.active || selection.flags & 0x18 == 0 {
            return Ok(());
        }
        let mut effects = Effects::default();
        let state = host.entity_state().map_err(&unavailable)?;
        let mut found = false;
        for player in &state.nearby_players {
            if !player.present {
                continue;
            }
            let named = match (&player.display_name, &name) {
                (Some(display), Some(wanted)) => equals_ignore_case(display, wanted),
                _ => false,
            };
            if !named {
                continue;
            }
            if !state.has_my_player {
                return Err(VmErrorKind::Host(HostError::NoEntity));
            }
            if (player.is_self && selection.flags & 0x10 != 0) || selection.flags & 0x8 != 0 {
                effects.packets.push(
                    interface_on_player(selection.slot, player.index, selection.hash, selection.item)
                        .map_err(VmErrorKind::Host)?,
                );
                effects.route_requests.push(RouteRequest {
                    x: player.movement_x,
                    y: player.movement_y,
                    size: player.size,
                    find_alternative: true,
                });
                found = true;
                break;
            }
        }
        if !found {
            let prefix = match host.language().map_err(&unavailable)? {
                None => return Err(VmErrorKind::Host(HostError::NoEntity)),
                Some(id @ 0..=3) => UNABLE_TO_FIND[id as usize],
                Some(_) => {
                    return Err(VmErrorKind::Host(HostError::State(
                        "Unknown language".to_string(),
                    )))
                }
            };
            effects
                .game_messages
                .push(format!("{prefix}{}", name.as_deref().unwrap_or("null")));
        }
        let model = host.components().map_err(&unavailable)?;
        match model.cancel_selection() {
            Ok(()) => {}
            Err(ComponentError::Unavailable) => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
            Err(ComponentError::Failure(failure)) => return Err(VmErrorKind::Component(failure)),
        }
        let outbox = host.outbox().map_err(unavailable)?;
        effects.apply(outbox);
        Ok(())
    }
}
