use super::world::host_error;
use super::{check_string, pop, push, Vm};
use crate::{Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

impl Vm {
    pub(super) fn execute_userdetail(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::{Int, String as Text};
        match opcode {
            BOOST_ADVERT_EXISTS | BOOST_ADVERT_AVAILABLE => {
                pop(&mut self.strings, Text)?;
                return push(&mut self.ints, i32::from(opcode == BOOST_ADVERT_EXISTS), Int);
            }
            BOOST_ADVERT_LAUNCH => {
                pop(&mut self.strings, Text)?;
                return Ok(());
            }
            _ => {}
        }
        let host: &dyn VarHost = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let lobby = host.lobby_state().map_err(&unavailable)?;
        if opcode == USERDETAIL_LOBBY_LASTLOGINADDRESS {
            let name = lobby
                .last_login_host
                .clone()
                .unwrap_or_else(|| Arc::from(""));
            check_string(Some(&name), self.limits.max_string_bytes)?;
            return push(&mut self.strings, Some(name), Text);
        }
        if opcode == USERDETAIL_LOBBY_MEMBERSHIP {
            let end = lobby.membership_end;
            let now = host.clock_millis().map_err(&unavailable)?;
            let remaining = end
                .wrapping_sub(now)
                .wrapping_sub(lobby.membership_time_correction);
            push(&mut self.ints, (end / 60000) as i32, Int)?;
            push(&mut self.ints, (remaining / 60000) as i32, Int)?;
            return push(&mut self.ints, i32::from(lobby.is_member_subscription), Int);
        }
        let value = match opcode {
            USERDETAIL_LOBBY_RECOVERYDAY => lobby.recovery_day,
            USERDETAIL_LOBBY_UNREADMESSAGES => lobby.messages,
            USERDETAIL_LOBBY_LASTLOGINDAY => lobby.last_login_day,
            USERDETAIL_LOBBY_EMAILSTATUS => lobby.email_status,
            USERDETAIL_LOBBY_CCEXPIRY => lobby.credit_card_expiry,
            USERDETAIL_LOBBY_GRACEEXPIRY => lobby.grace_expiry,
            USERDETAIL_LOBBY_DOBREQUESTED => i32::from(lobby.dob_requested),
            USERDETAIL_DOB => lobby.dob,
            USERDETAIL_LOBBY_MEMBERSSTATS => lobby.members_stats,
            USERDETAIL_LOBBY_PLAYAGE => lobby.play_age,
            USERDETAIL_LOBBY_JCOINS_BALANCE => lobby.jcoins,
            USERDETAIL_LOBBY_LOYALTY_ENABLED => i32::from(lobby.loyalty_enabled),
            USERDETAIL_LOBBY_LOYALTY_BALANCE => lobby.loyalty_balance,
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        };
        push(&mut self.ints, value, Int)
    }

    pub(super) fn is_userdetail_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            USERDETAIL_LOBBY_MEMBERSHIP
                | USERDETAIL_LOBBY_RECOVERYDAY
                | USERDETAIL_LOBBY_UNREADMESSAGES
                | USERDETAIL_LOBBY_LASTLOGINDAY
                | USERDETAIL_LOBBY_LASTLOGINADDRESS
                | USERDETAIL_LOBBY_EMAILSTATUS
                | USERDETAIL_LOBBY_CCEXPIRY
                | USERDETAIL_LOBBY_GRACEEXPIRY
                | USERDETAIL_LOBBY_DOBREQUESTED
                | USERDETAIL_DOB
                | USERDETAIL_LOBBY_MEMBERSSTATS
                | USERDETAIL_LOBBY_PLAYAGE
                | USERDETAIL_LOBBY_JCOINS_BALANCE
                | USERDETAIL_LOBBY_LOYALTY_ENABLED
                | USERDETAIL_LOBBY_LOYALTY_BALANCE
                | BOOST_ADVERT_EXISTS
                | BOOST_ADVERT_AVAILABLE
                | BOOST_ADVERT_LAUNCH
        )
    }
}
