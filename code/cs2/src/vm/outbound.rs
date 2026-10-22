use super::{pop, push, Vm};
use crate::outbox::{
    friend_add, friend_del, friends_chat_display_name, friends_chat_join, friends_chat_kick,
    friends_chat_leave, friends_chat_set_rank, ignore_add, ignore_del, Effects,
};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

impl Vm {
    pub(super) fn is_outbound_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            FRIEND_ADD
                | FRIEND_DEL
                | FRIEND_SETRANK
                | IGNORE_ADD
                | IGNORE_DEL
                | IGNORE_ADD_TEMP
                | CLAN_KICKUSER
                | CLAN_JOINCHAT
                | CLAN_LEAVECHAT
                | CLAN_GETCHATDISPLAYNAME
        )
    }

    fn pop_optional_text(&mut self) -> Result<Option<Arc<str>>, VmErrorKind> {
        pop(&mut self.strings, StackKind::String)
    }

    pub(super) fn execute_outbound(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        if opcode == CLAN_GETCHATDISPLAYNAME {
            let name = friends_chat_display_name(host.social_state().map_err(&unavailable)?);
            return push(&mut self.strings, Some(Arc::from(name.as_str())), StackKind::String);
        }
        host.outbox().map_err(&unavailable)?;
        let mut effects = Effects::default();
        let outcome = self.outbound_effects(opcode, host, &mut effects, &unavailable);
        if let Ok(outbox) = host.outbox() {
            effects.apply(outbox);
        }
        outcome
    }

    fn outbound_effects(
        &mut self,
        opcode: Opcode,
        host: &mut dyn VarHost,
        effects: &mut Effects,
        unavailable: &dyn Fn(HostError) -> VmErrorKind,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host_failure = |error| unavailable(error);
        match opcode {
            FRIEND_ADD | FRIEND_DEL | IGNORE_ADD | IGNORE_DEL | CLAN_KICKUSER | CLAN_JOINCHAT => {
                let name = self.pop_optional_text()?;
                if opcode == CLAN_KICKUSER && name.is_none() {
                    return Err(VmErrorKind::Host(HostError::NoEntity));
                }
                let Some(name) = name else {
                    return Ok(());
                };
                let social = host.social_state_mut().map_err(host_failure)?;
                match opcode {
                    FRIEND_ADD => friend_add(social, &name, effects),
                    FRIEND_DEL => friend_del(social, &name, effects),
                    IGNORE_ADD => ignore_add(social, &name, false, effects),
                    IGNORE_DEL => ignore_del(social, &name, effects),
                    CLAN_KICKUSER => friends_chat_kick(social, &name, effects),
                    _ => friends_chat_join(&name, effects),
                }
                .map_err(host_failure)
            }
            IGNORE_ADD_TEMP => {
                let name = self.pop_optional_text()?;
                let Some(name) = name else {
                    return Ok(());
                };
                let social = host.social_state_mut().map_err(host_failure)?;
                ignore_add(social, &name, true, effects).map_err(host_failure)
            }
            FRIEND_SETRANK => {
                let name = self.pop_optional_text()?;
                let rank = pop(&mut self.ints, Int)?;
                let name = name.ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                effects
                    .packets
                    .push(friends_chat_set_rank(&name, rank).map_err(host_failure)?);
                Ok(())
            }
            _ => {
                effects
                    .packets
                    .push(friends_chat_leave().map_err(host_failure)?);
                Ok(())
            }
        }
    }
}
