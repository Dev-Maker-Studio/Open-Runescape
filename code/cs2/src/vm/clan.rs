use super::{pop, push, Vm};
use crate::outbox::{clan_channel_kick, clan_operation, Effects};
use crate::{
    ClanChannel, ClanSettings, ClanVarValue, HostError, Opcode, Operand, StackKind, VarHost,
    VmErrorKind,
};
use std::sync::Arc;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub(super) enum Selection {
    Affined,
    Listened,
}

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn no_entity() -> VmErrorKind {
    VmErrorKind::Host(HostError::NoEntity)
}

impl Vm {
    pub(super) fn is_clan_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            ACTIVECLANSETTINGS_FIND_LISTENED
                | ACTIVECLANSETTINGS_FIND_AFFINED
                | ACTIVECLANSETTINGS_GETCLANNAME
                | ACTIVECLANSETTINGS_GETALLOWUNAFFINED
                | ACTIVECLANSETTINGS_GETRANKTALK
                | ACTIVECLANSETTINGS_GETRANKKICK
                | ACTIVECLANSETTINGS_GETRANKLOOTSHARE
                | ACTIVECLANSETTINGS_GETCOINSHARE
                | ACTIVECLANSETTINGS_GETAFFINEDCOUNT
                | ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME
                | ACTIVECLANSETTINGS_GETAFFINEDRANK
                | ACTIVECLANSETTINGS_GETBANNEDCOUNT
                | ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME
                | ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO
                | ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT
                | ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT
                | ACTIVECLANSETTINGS_GETAFFINEDSLOT
                | ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT
                | ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY
                | UNUSED_CLAN_OP
                | ACTIVECLANCHANNEL_FIND_LISTENED
                | ACTIVECLANCHANNEL_FIND_AFFINED
                | ACTIVECLANCHANNEL_GETCLANNAME
                | ACTIVECLANCHANNEL_GETRANKKICK
                | ACTIVECLANCHANNEL_GETRANKTALK
                | ACTIVECLANCHANNEL_GETUSERCOUNT
                | ACTIVECLANCHANNEL_GETUSERDISPLAYNAME
                | ACTIVECLANCHANNEL_GETUSERRANK
                | ACTIVECLANCHANNEL_GETUSERWORLD
                | ACTIVECLANCHANNEL_KICKUSER
                | ACTIVECLANCHANNEL_GETUSERSLOT
                | ACTIVECLANCHANNEL_GETSORTEDUSERSLOT
                | CLANPROFILE_FIND
                | STOCKMARKET_GETOFFERTYPE
                | STOCKMARKET_GETOFFERITEM
                | STOCKMARKET_GETOFFERPRICE
                | STOCKMARKET_GETOFFERCOUNT
                | STOCKMARKET_GETOFFERCOMPLETEDCOUNT
                | STOCKMARKET_GETOFFERCOMPLETEDGOLD
                | STOCKMARKET_ISOFFEREMPTY
                | STOCKMARKET_ISOFFERSTABLE
                | STOCKMARKET_ISOFFERFINISHED
                | STOCKMARKET_ISOFFERADDING
        )
    }

    fn pop_int(&mut self) -> Result<i32, VmErrorKind> {
        pop(&mut self.ints, StackKind::Int)
    }

    fn push_int(&mut self, value: i32) -> Result<(), VmErrorKind> {
        push(&mut self.ints, value, StackKind::Int)
    }

    fn push_optional_text(&mut self, value: Option<&str>) -> Result<(), VmErrorKind> {
        push(&mut self.strings, value.map(Arc::from), StackKind::String)
    }

    fn selected_settings<'a>(
        &self,
        clan: &'a crate::ClanState,
    ) -> Result<&'a ClanSettings, VmErrorKind> {
        match self.clan_settings_selection {
            Some(Selection::Affined) => clan.settings.as_ref(),
            Some(Selection::Listened) => clan.guest_settings.as_ref(),
            None => None,
        }
        .ok_or_else(no_entity)
    }

    fn selected_channel<'a>(
        &self,
        clan: &'a crate::ClanState,
    ) -> Result<&'a ClanChannel, VmErrorKind> {
        match self.clan_channel_selection {
            Some(Selection::Affined) => clan.channel.as_ref(),
            Some(Selection::Listened) => clan.listened_channel.as_ref(),
            None => None,
        }
        .ok_or_else(no_entity)
    }

    pub(super) fn execute_clan(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let clan = host.clan_state().map_err(&unavailable)?;
        let host_failure = VmErrorKind::Host;
        match opcode {
            ACTIVECLANSETTINGS_FIND_LISTENED | ACTIVECLANSETTINGS_FIND_AFFINED => {
                let listened = opcode == ACTIVECLANSETTINGS_FIND_LISTENED;
                let present = if listened {
                    clan.guest_settings.is_some()
                } else {
                    clan.settings.is_some()
                };
                if present {
                    self.clan_settings_selection = Some(if listened {
                        Selection::Listened
                    } else {
                        Selection::Affined
                    });
                }
                self.push_int(i32::from(present))
            }
            ACTIVECLANCHANNEL_FIND_LISTENED | ACTIVECLANCHANNEL_FIND_AFFINED => {
                let listened = opcode == ACTIVECLANCHANNEL_FIND_LISTENED;
                let present = if listened {
                    clan.listened_channel.is_some()
                } else {
                    clan.channel.is_some()
                };
                if present {
                    self.clan_channel_selection = Some(if listened {
                        Selection::Listened
                    } else {
                        Selection::Affined
                    });
                }
                self.push_int(i32::from(present))
            }
            CLANPROFILE_FIND => self.push_int(i32::from(clan.clan_vars.is_some())),
            ACTIVECLANSETTINGS_GETCLANNAME => {
                let settings = self.selected_settings(clan)?;
                self.push_optional_text(settings.clan_name.as_deref())
            }
            ACTIVECLANSETTINGS_GETALLOWUNAFFINED => {
                let settings = self.selected_settings(clan)?;
                self.push_int(i32::from(settings.allow_guests))
            }
            ACTIVECLANSETTINGS_GETRANKTALK => {
                let settings = self.selected_settings(clan)?;
                self.push_int(i32::from(settings.talk_rank))
            }
            ACTIVECLANSETTINGS_GETRANKKICK => {
                let settings = self.selected_settings(clan)?;
                self.push_int(i32::from(settings.kick_rank))
            }
            ACTIVECLANSETTINGS_GETRANKLOOTSHARE => {
                let settings = self.selected_settings(clan)?;
                self.push_int(i32::from(settings.lootshare_rank))
            }
            ACTIVECLANSETTINGS_GETCOINSHARE => {
                let settings = self.selected_settings(clan)?;
                self.push_int(i32::from(settings.coin_share))
            }
            ACTIVECLANSETTINGS_GETAFFINEDCOUNT => {
                let settings = self.selected_settings(clan)?;
                self.push_int(settings.member_count)
            }
            ACTIVECLANSETTINGS_GETBANNEDCOUNT => {
                let settings = self.selected_settings(clan)?;
                self.push_int(settings.ban_count)
            }
            ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT => {
                let settings = self.selected_settings(clan)?;
                self.push_int(settings.current_owner)
            }
            ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT => {
                let settings = self.selected_settings(clan)?;
                self.push_int(settings.replacement_owner)
            }
            ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME => {
                let slot = self.pop_int()?;
                let settings = self.selected_settings(clan)?;
                let name = crate::clan::element(&settings.member_names, slot).map_err(host_failure)?;
                self.push_optional_text(Some(name))
            }
            ACTIVECLANSETTINGS_GETAFFINEDRANK => {
                let slot = self.pop_int()?;
                let settings = self.selected_settings(clan)?;
                let rank = *crate::clan::element(&settings.member_ranks, slot).map_err(host_failure)?;
                self.push_int(i32::from(rank))
            }
            ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY => {
                let slot = self.pop_int()?;
                let settings = self.selected_settings(clan)?;
                let day =
                    *crate::clan::element(&settings.member_join_days, slot).map_err(host_failure)?;
                self.push_int(day)
            }
            ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME => {
                let slot = self.pop_int()?;
                let settings = self.selected_settings(clan)?;
                let name =
                    crate::clan::element(&settings.banned_user_names, slot).map_err(host_failure)?;
                self.push_optional_text(Some(name))
            }
            ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO => {
                let end = self.pop_int()?;
                let start = self.pop_int()?;
                let slot = self.pop_int()?;
                let settings = self.selected_settings(clan)?;
                let value = settings
                    .clan_flag_bitfield(slot, start, end)
                    .map_err(host_failure)?;
                self.push_int(value)
            }
            ACTIVECLANSETTINGS_GETAFFINEDSLOT => {
                let name = pop(&mut self.strings, StackKind::String)?;
                let settings = self.selected_settings(clan)?;
                let slot = settings.member_index(name.as_deref()).map_err(host_failure)?;
                self.push_int(slot)
            }
            ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT => {
                let settings = self.selected_settings(clan)?;
                let sorted = settings.sorted_affined_slots().map_err(host_failure)?;
                let position = pop(&mut self.ints, StackKind::Int)?;
                let value = usize::try_from(position)
                    .ok()
                    .and_then(|position| sorted.get(position))
                    .ok_or_else(|| {
                        VmErrorKind::Host(HostError::State("array index out of bounds".to_string()))
                    })?;
                self.push_int(*value)
            }
            ACTIVECLANCHANNEL_GETCLANNAME => {
                let channel = self.selected_channel(clan)?;
                self.push_optional_text(channel.clan_name.as_deref())
            }
            ACTIVECLANCHANNEL_GETRANKKICK => {
                let channel = self.selected_channel(clan)?;
                self.push_int(i32::from(channel.min_rank_to_kick))
            }
            ACTIVECLANCHANNEL_GETRANKTALK => {
                let channel = self.selected_channel(clan)?;
                self.push_int(i32::from(channel.guests_talk))
            }
            ACTIVECLANCHANNEL_GETUSERCOUNT => {
                let channel = self.selected_channel(clan)?;
                self.push_int(channel.num_players)
            }
            ACTIVECLANCHANNEL_GETUSERDISPLAYNAME => {
                let slot = self.pop_int()?;
                let channel = self.selected_channel(clan)?;
                let player = channel.player(slot).map_err(host_failure)?;
                self.push_optional_text(player.name.as_deref())
            }
            ACTIVECLANCHANNEL_GETUSERRANK => {
                let slot = self.pop_int()?;
                let channel = self.selected_channel(clan)?;
                let player = channel.player(slot).map_err(host_failure)?;
                self.push_int(i32::from(player.rank))
            }
            ACTIVECLANCHANNEL_GETUSERWORLD => {
                let slot = self.pop_int()?;
                let channel = self.selected_channel(clan)?;
                let player = channel.player(slot).map_err(host_failure)?;
                self.push_int(player.world)
            }
            ACTIVECLANCHANNEL_GETUSERSLOT => {
                let channel = self.selected_channel(clan)?;
                let name = pop(&mut self.strings, StackKind::String)?.ok_or_else(no_entity)?;
                let slot = channel.user_slot(&name).map_err(host_failure)?;
                self.push_int(slot)
            }
            ACTIVECLANCHANNEL_GETSORTEDUSERSLOT => {
                let channel = self.selected_channel(clan)?;
                let sorted = channel.players_sorted_by_name().map_err(host_failure)?;
                let position = pop(&mut self.ints, StackKind::Int)?;
                let value = usize::try_from(position)
                    .ok()
                    .and_then(|position| sorted.get(position))
                    .ok_or_else(|| {
                        VmErrorKind::Host(HostError::State("array index out of bounds".to_string()))
                    })?;
                self.push_int(*value)
            }
            ACTIVECLANCHANNEL_KICKUSER | UNUSED_CLAN_OP => {
                let slot = self.pop_int()?;
                let kick = opcode == ACTIVECLANCHANNEL_KICKUSER;
                let main_channel = if kick {
                    match self.clan_channel_selection {
                        Some(Selection::Affined) => true,
                        Some(Selection::Listened) => clan.listened_is_channel,
                        None => clan.channel.is_none(),
                    }
                } else {
                    true
                };
                let channel = if main_channel {
                    clan.channel.as_ref()
                } else {
                    clan.listened_channel.as_ref()
                };
                let Some(channel) = channel.filter(|channel| slot >= 0 && slot < channel.num_players)
                else {
                    return Ok(());
                };
                let Some(players) = &channel.players else {
                    return Ok(());
                };
                let player = players
                    .get(slot as usize)
                    .ok_or_else(|| {
                        VmErrorKind::Host(HostError::State("array index out of bounds".to_string()))
                    })?
                    .as_ref();
                let Some(player) = player.filter(|player| player.rank == -1) else {
                    return Ok(());
                };
                let name = player.name.as_deref().unwrap_or("");
                let packet = if kick {
                    clan_channel_kick(main_channel, slot, name)
                } else {
                    clan_operation(slot, name)
                }
                .map_err(host_failure)?;
                let mut effects = Effects::default();
                effects.packets.push(packet);
                let outbox = host.outbox().map_err(&unavailable)?;
                effects.apply(outbox);
                Ok(())
            }
            STOCKMARKET_GETOFFERTYPE
            | STOCKMARKET_GETOFFERITEM
            | STOCKMARKET_GETOFFERPRICE
            | STOCKMARKET_GETOFFERCOUNT
            | STOCKMARKET_GETOFFERCOMPLETEDCOUNT
            | STOCKMARKET_GETOFFERCOMPLETEDGOLD
            | STOCKMARKET_ISOFFEREMPTY
            | STOCKMARKET_ISOFFERSTABLE
            | STOCKMARKET_ISOFFERFINISHED
            | STOCKMARKET_ISOFFERADDING => {
                let slot = self.pop_int()?;
                let offer = clan.stock_slot(slot).map_err(host_failure)?;
                let value = match opcode {
                    STOCKMARKET_GETOFFERTYPE => i32::from(offer.is_buy_offer()),
                    STOCKMARKET_GETOFFERITEM => offer.item_id,
                    STOCKMARKET_GETOFFERPRICE => offer.price,
                    STOCKMARKET_GETOFFERCOUNT => offer.amount,
                    STOCKMARKET_GETOFFERCOMPLETEDCOUNT => offer.current_amount,
                    STOCKMARKET_GETOFFERCOMPLETEDGOLD => offer.total_price,
                    STOCKMARKET_ISOFFEREMPTY => i32::from(offer.state() == 0),
                    STOCKMARKET_ISOFFERSTABLE => i32::from(offer.state() == 2),
                    STOCKMARKET_ISOFFERFINISHED => i32::from(offer.state() == 5),
                    _ => i32::from(offer.state() == 1),
                };
                self.push_int(value)
            }
            _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
    }
}

fn class_cast() -> VmErrorKind {
    VmErrorKind::Host(HostError::State("class cast".to_string()))
}

fn out_of_bounds() -> VmErrorKind {
    VmErrorKind::Host(HostError::State("array index out of bounds".to_string()))
}

fn int_value(value: Option<&ClanVarValue>) -> Result<Option<i32>, VmErrorKind> {
    match value {
        None => Ok(None),
        Some(ClanVarValue::Int(value)) => Ok(Some(*value)),
        Some(_) => Err(class_cast()),
    }
}

fn setting_key(game_id: Option<i32>, id: i32) -> Result<i32, VmErrorKind> {
    Ok(game_id.ok_or_else(no_entity)?.wrapping_shl(16) | id)
}

impl Vm {
    pub(super) fn is_clan_var_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            PUSH_VARCLAN
                | PUSH_VARCLANBIT
                | LOAD_CLAN_VAR_LONG
                | LOAD_CLAN_VAR_STRING
                | PUSH_VARCLANSETTING
                | PUSH_VARCLANSETTINGBIT
                | PUSH_VARCLANSETTING_LONG
                | PUSH_VARCLANSETTING_STRING
        )
    }

    pub(super) fn execute_clan_var(
        &mut self,
        opcode: Opcode,
        operand: &Operand,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let Operand::Int(id) = operand else {
            return Err(VmErrorKind::InvalidOperand);
        };
        let id = *id;
        let host = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let clan = host.clan_state().map_err(&unavailable)?;
        let global = |slot: i32| -> Result<Option<&ClanVarValue>, VmErrorKind> {
            let vars = clan.clan_vars.as_ref().ok_or_else(no_entity)?;
            let slot = usize::try_from(slot).map_err(|_| out_of_bounds())?;
            Ok(vars.get(slot).ok_or_else(out_of_bounds)?.as_ref())
        };
        let selection = self.clan_settings_selection;
        let setting = |key: i32| -> Result<Option<&ClanVarValue>, VmErrorKind> {
            let settings = match selection {
                Some(Selection::Affined) => clan.settings.as_ref(),
                Some(Selection::Listened) => clan.guest_settings.as_ref(),
                None => None,
            }
            .ok_or_else(no_entity)?;
            Ok(settings
                .variables
                .as_ref()
                .and_then(|variables| variables.get(&key)))
        };
        let integer_default = |type_char: u8| if type_char == 105 || type_char == 49 { 0 } else { -1 };
        match opcode {
            PUSH_VARCLAN => {
                let value = match int_value(global(id)?)? {
                    Some(value) => value,
                    None => {
                        let defs = host.clan_var_defs().map_err(&unavailable)?;
                        integer_default(defs.var_type(id).map_err(VmErrorKind::Host)?.type_char)
                    }
                };
                self.push_int(value)
            }
            PUSH_VARCLANBIT => {
                let defs = host.clan_var_defs().map_err(&unavailable)?;
                let var = defs.var_type(id).map_err(VmErrorKind::Host)?;
                if var.type_char != 1 {
                    return self.push_int(0);
                }
                let value = match int_value(global(var.base_var)?)? {
                    Some(packed) => crate::clan_var::bitfield(packed, var.start_bit, var.end_bit),
                    None => 0,
                };
                self.push_int(value)
            }
            LOAD_CLAN_VAR_LONG => {
                let value = match global(id)? {
                    None => -1,
                    Some(ClanVarValue::Long(value)) => *value,
                    Some(_) => return Err(class_cast()),
                };
                push(&mut self.longs, value, StackKind::Long)
            }
            LOAD_CLAN_VAR_STRING => {
                let value = match global(id)? {
                    None => "",
                    Some(ClanVarValue::Text(value)) => value.as_str(),
                    Some(_) => return Err(class_cast()),
                };
                push(&mut self.strings, Some(Arc::from(value)), StackKind::String)
            }
            PUSH_VARCLANSETTING => {
                let defs = host.clan_var_defs().map_err(&unavailable)?;
                let var = defs.setting_type(id).map_err(VmErrorKind::Host)?;
                let key = setting_key(clan.game_id, id)?;
                let value = match setting(key)? {
                    Some(ClanVarValue::Int(value)) => *value,
                    _ => integer_default(var.type_char),
                };
                self.push_int(value)
            }
            PUSH_VARCLANSETTINGBIT => {
                let defs = host.clan_var_defs().map_err(&unavailable)?;
                let var = defs.setting_type(id).map_err(VmErrorKind::Host)?;
                let key = setting_key(clan.game_id, var.base_var)?;
                let value = match setting(key)? {
                    Some(ClanVarValue::Int(packed)) => {
                        crate::clan_var::bitfield(*packed, var.start_bit, var.end_bit)
                    }
                    _ => 0,
                };
                self.push_int(value)
            }
            PUSH_VARCLANSETTING_LONG => {
                let key = setting_key(clan.game_id, id)?;
                let value = match setting(key)? {
                    Some(ClanVarValue::Long(value)) => *value,
                    _ => -1,
                };
                push(&mut self.longs, value, StackKind::Long)
            }
            _ => {
                let key = setting_key(clan.game_id, id)?;
                let value = match setting(key)? {
                    Some(ClanVarValue::Text(value)) => value.as_str(),
                    _ => "",
                };
                push(&mut self.strings, Some(Arc::from(value)), StackKind::String)
            }
        }
    }
}
