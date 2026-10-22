use super::{check_string, pop, push, Vm};
use crate::entity::{entity_icon_height, npc_name, npc_resolved};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

impl Vm {
    pub(super) fn execute_entity(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        if opcode == SETENTITYOVERHEADIF {
            host.entity_state_mut()
                .map_err(&unavailable)?
                .current_mut()
                .map_err(&unavailable)?;
            let interface = pop(&mut self.ints, Int)?;
            host.entity_state_mut()
                .map_err(&unavailable)?
                .current_mut()
                .map_err(&unavailable)?
                .common_mut()
                .set_overhead_interface_id(interface);
            return Ok(());
        }
        let host: &dyn VarHost = host;
        let state = host.entity_state().map_err(&unavailable)?;
        match opcode {
            GET_ENTITY_OVERLAY_HEIGHT => {
                let value = entity_icon_height(host, state).map_err(&unavailable)?;
                push(&mut self.ints, value, Int)
            }
            GET_OBJ_OVERLAY_HEIGHT => {
                let value = state.ground_item().map_err(&unavailable)?.icon_height();
                push(&mut self.ints, value, Int)
            }
            GETENTITYOVERHEADIF => {
                let value = state.current().map_err(&unavailable)?.common().loaded_interface_id;
                push(&mut self.ints, value, Int)
            }
            IS_TARGETED_ENTITY => {
                let index = state.current().map_err(&unavailable)?.cs2_index();
                push(&mut self.ints, i32::from(state.entity_target == index), Int)
            }
            IS_NPC_ACTIVE => {
                let resolved = npc_resolved(host, state).map_err(&unavailable)?;
                push(
                    &mut self.ints,
                    i32::from(resolved.is_some_and(|npc| npc.visible)),
                    Int,
                )
            }
            IS_NPC_VISIBLE => {
                let resolved = npc_resolved(host, state).map_err(&unavailable)?;
                push(&mut self.ints, i32::from(resolved.is_some()), Int)
            }
            GET_ENTITY_SAY => {
                let entity = state.current().map_err(&unavailable)?;
                let text = state.hint_text(entity).unwrap_or_default();
                self.entity_text(text)
            }
            GET_DISPLAYNAME_WITHEXTRAS => {
                let text = state
                    .player()
                    .map_err(&unavailable)?
                    .username_with_title();
                self.entity_text(text)
            }
            GET_NPC_NAME => {
                let text = npc_name(host, state).map_err(&unavailable)?;
                self.entity_text(text)
            }
            _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
    }

    fn entity_text(&mut self, text: String) -> Result<(), VmErrorKind> {
        check_string(Some(&text), self.limits.max_string_bytes)?;
        push(
            &mut self.strings,
            Some(Arc::from(text)),
            StackKind::String,
        )
    }

    pub(super) fn is_entity_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            GET_ENTITY_OVERLAY_HEIGHT
                | GET_OBJ_OVERLAY_HEIGHT
                | GET_ENTITY_SAY
                | SETENTITYOVERHEADIF
                | GETENTITYOVERHEADIF
                | GET_DISPLAYNAME_WITHEXTRAS
                | GET_NPC_NAME
                | IS_NPC_ACTIVE
                | IS_NPC_VISIBLE
                | IS_TARGETED_ENTITY
        )
    }
}
