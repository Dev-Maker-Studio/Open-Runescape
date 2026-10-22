use super::{pop, push, Vm};
use crate::social::strip_crown;
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

fn text(value: Option<&str>) -> Option<Arc<str>> {
    value.map(Arc::from)
}

impl Vm {
    fn pop_text(&mut self) -> Result<Arc<str>, VmErrorKind> {
        pop(&mut self.strings, StackKind::String)?.ok_or(VmErrorKind::Host(HostError::NoEntity))
    }

    fn push_text(&mut self, value: Option<&str>) -> Result<(), VmErrorKind> {
        push(&mut self.strings, text(value), StackKind::String)
    }

    pub(super) fn execute_social(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let social = host.social_state().map_err(&unavailable)?;
        match opcode {
            FRIEND_COUNT => push(&mut self.ints, social.friend_count(), Int),
            IGNORE_COUNT => push(&mut self.ints, social.ignore_count(), Int),
            CLAN_GETCHATCOUNT => push(&mut self.ints, social.fc_count(), Int),
            CLAN_GETCHATMINKICK => push(&mut self.ints, i32::from(social.fc_min_rank_can_kick), Int),
            CLAN_GETCHATRANK => push(&mut self.ints, i32::from(social.my_fc_rank), Int),
            PLAYERCOUNTRY => push(&mut self.ints, social.country, Int),
            CLAN_GETCHATOWNERNAME => {
                self.push_text(Some(social.fc_owner_name.as_deref().unwrap_or("")))
            }
            FRIEND_GETWORLD | FRIEND_GETRANK | FRIEND_GETWORLDFLAGS | FRIEND_IS_REFERRER => {
                let index = pop(&mut self.ints, Int)?;
                let friend = social.friend(index).map_err(&unavailable)?;
                let value = friend.map_or(0, |friend| match opcode {
                    FRIEND_GETWORLD => friend.world_id,
                    FRIEND_GETRANK => friend.friends_chat_rank,
                    FRIEND_GETWORLDFLAGS => friend.world_flags,
                    _ => i32::from(friend.referrer),
                });
                push(&mut self.ints, value, Int)
            }
            FRIEND_PLATFORM => {
                let index = pop(&mut self.ints, Int)?;
                let value = social.friend_non_negative(index).map_or(0, |friend| friend.platform);
                push(&mut self.ints, value, Int)
            }
            FRIEND_GETWORLDNAME => {
                let index = pop(&mut self.ints, Int)?;
                let friend = social.friend(index).map_err(&unavailable)?;
                self.push_text(Some(friend.map_or("", |friend| friend.world_name.as_str())))
            }
            FRIEND_GETNAME => {
                let index = pop(&mut self.ints, Int)?;
                let friend = social.friend(index).map_err(&unavailable)?;
                self.push_text(Some(friend.map_or("", |friend| friend.display_name.as_str())))?;
                self.push_text(Some(
                    friend.map_or("", |friend| friend.previous_display_name.as_str()),
                ))
            }
            FRIEND_TEST | FRIEND_GETSLOTFROMNAME | IGNORE_TEST => {
                let name = self.pop_text()?;
                let name = strip_crown(&name);
                let value = match opcode {
                    FRIEND_TEST => i32::from(social.friend_contains(name).map_err(&unavailable)?),
                    FRIEND_GETSLOTFROMNAME => social.friend_slot(name),
                    _ => i32::from(social.ignore_contains(name)),
                };
                push(&mut self.ints, value, Int)
            }
            IGNORE_GETNAME => {
                let index = pop(&mut self.ints, Int)?;
                let ignore = social.ignore(index).map_err(&unavailable)?;
                match ignore {
                    Some(ignore) => self.push_text(ignore.unfiltered_username.as_deref())?,
                    None => self.push_text(Some(""))?,
                }
                self.push_text(Some(
                    ignore.map_or("", |ignore| ignore.display_name.as_deref().unwrap_or("")),
                ))
            }
            IGNORE_GETNAME_UNFILTERED => {
                let index = pop(&mut self.ints, Int)?;
                let ignore = social.ignore(index).map_err(&unavailable)?;
                match ignore {
                    Some(ignore) => self.push_text(ignore.unfiltered_username.as_deref()),
                    None => self.push_text(Some("")),
                }
            }
            IGNORE_IS_TEMP => {
                let index = pop(&mut self.ints, Int)?;
                let value = social.ignore_is_temporary(index).map_err(&unavailable)?;
                push(&mut self.ints, i32::from(value), Int)
            }
            CLAN_GETCHATUSERWORLD | CLAN_GETCHATUSERRANK => {
                let index = pop(&mut self.ints, Int)?;
                let player = social.fc_player(index).map_err(&unavailable)?;
                let value = player.map_or(0, |player| {
                    if opcode == CLAN_GETCHATUSERWORLD {
                        player.world_id
                    } else {
                        i32::from(player.rank)
                    }
                });
                push(&mut self.ints, value, Int)
            }
            CLAN_ISSELF => {
                let index = pop(&mut self.ints, Int)?;
                let value = social.fc_is_self(index).map_err(&unavailable)?;
                push(&mut self.ints, i32::from(value), Int)
            }
            CLAN_GETCHATUSERNAME | CLAN_GETCHATUSERNAME_UNFILTERED | CLAN_GETCHATUSERWORLDNAME => {
                let index = pop(&mut self.ints, Int)?;
                let player = social.fc_player(index).map_err(&unavailable)?;
                match player {
                    Some(player) => {
                        let value = match opcode {
                            CLAN_GETCHATUSERNAME => &player.display_name,
                            CLAN_GETCHATUSERNAME_UNFILTERED => &player.username,
                            _ => &player.world_name,
                        };
                        self.push_text(value.as_deref())
                    }
                    None => self.push_text(Some("")),
                }
            }
            _ => Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
    }

    pub(super) fn is_social_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            FRIEND_COUNT
                | FRIEND_GETNAME
                | FRIEND_GETWORLD
                | FRIEND_GETRANK
                | FRIEND_GETWORLDFLAGS
                | FRIEND_GETWORLDNAME
                | FRIEND_TEST
                | FRIEND_GETSLOTFROMNAME
                | FRIEND_PLATFORM
                | FRIEND_IS_REFERRER
                | IGNORE_COUNT
                | IGNORE_GETNAME
                | IGNORE_GETNAME_UNFILTERED
                | IGNORE_TEST
                | IGNORE_IS_TEMP
                | CLAN_GETCHATCOUNT
                | CLAN_GETCHATUSERNAME
                | CLAN_GETCHATUSERNAME_UNFILTERED
                | CLAN_GETCHATUSERWORLD
                | CLAN_GETCHATUSERRANK
                | CLAN_GETCHATUSERWORLDNAME
                | CLAN_GETCHATMINKICK
                | CLAN_GETCHATRANK
                | CLAN_GETCHATOWNERNAME
                | CLAN_ISSELF
                | PLAYERCOUNTRY
        )
    }
}
