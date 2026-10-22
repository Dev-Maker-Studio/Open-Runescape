use crate::HostError;

pub const FRIEND_CAPACITY: usize = 200;
pub const IGNORE_CAPACITY: usize = 100;
const CROWN_LENGTH: usize = 7;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct Friend {
    pub display_name: String,
    pub referrer: bool,
    pub world_id: i32,
    pub world_name: String,
    pub friends_chat_rank: i32,
    pub platform: i32,
    pub previous_display_name: String,
    pub world_flags: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct Ignore {
    pub unfiltered_username: Option<String>,
    pub display_name: Option<String>,
    pub temporary: bool,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct FriendsChatPlayer {
    pub rank: i8,
    pub username: Option<String>,
    pub display_name: Option<String>,
    pub world_name: Option<String>,
    pub world_id: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct SocialState {
    pub friends_loaded: i32,
    pub friends: Vec<Friend>,
    pub ignores: Vec<Ignore>,
    pub fc_name: Option<String>,
    pub fc_owner_name: Option<String>,
    pub fc_players: Option<Vec<Option<FriendsChatPlayer>>>,
    pub fc_player_count: i32,
    pub fc_min_rank_can_kick: i8,
    pub my_fc_rank: i8,
    pub my_display_name: Option<Option<String>>,
    pub country: i32,
}

fn array_error(index: i32) -> HostError {
    HostError::State(format!("array index {index} out of bounds"))
}

fn fold(unit: u16) -> (u16, u16) {
    let Some(character) = char::from_u32(u32::from(unit)) else {
        return (unit, unit);
    };
    let mut upper = character.to_uppercase();
    let upper = match (upper.next(), upper.next()) {
        (Some(single), None) => single,
        _ => character,
    };
    let mut lower = upper.to_lowercase();
    let lower = match (lower.next(), lower.next()) {
        (Some(single), None) => single,
        _ => upper,
    };
    let narrow = |value: char| u16::try_from(u32::from(value)).unwrap_or(unit);
    (narrow(upper), narrow(lower))
}

pub fn equals_ignore_case(left: &str, right: &str) -> bool {
    let left: Vec<u16> = left.encode_utf16().collect();
    let right: Vec<u16> = right.encode_utf16().collect();
    left.len() == right.len()
        && left.iter().zip(&right).all(|(&a, &b)| {
            a == b || {
                let (au, al) = fold(a);
                let (bu, bl) = fold(b);
                au == bu || al == bl
            }
        })
}

pub fn nullable_equals_ignore_case(left: Option<&str>, right: Option<&str>) -> bool {
    match (left, right) {
        (None, None) => true,
        (Some(left), Some(right)) => equals_ignore_case(left, right),
        _ => false,
    }
}

pub fn strip_crown(text: &str) -> &str {
    if text.starts_with("<img=0>") || text.starts_with("<img=1>") {
        &text[CROWN_LENGTH..]
    } else {
        text
    }
}

impl SocialState {
    pub fn friend_count(&self) -> i32 {
        match self.friends_loaded {
            0 => -2,
            1 => -1,
            _ => self.friends.len() as i32,
        }
    }

    pub fn friend(&self, index: i32) -> Result<Option<&Friend>, HostError> {
        if self.friends_loaded == 2 && i64::from(index) < self.friends.len() as i64 {
            usize::try_from(index)
                .ok()
                .and_then(|slot| self.friends.get(slot))
                .map(Some)
                .ok_or_else(|| array_error(index))
        } else {
            Ok(None)
        }
    }

    pub fn friend_non_negative(&self, index: i32) -> Option<&Friend> {
        if index < 0 {
            None
        } else {
            self.friend(index).ok().flatten()
        }
    }

    pub fn friend_slot(&self, name: &str) -> i32 {
        self.friends
            .iter()
            .position(|friend| equals_ignore_case(name, &friend.display_name))
            .map_or(-1, |index| index as i32)
    }

    fn my_name(&self) -> Result<Option<&str>, HostError> {
        match &self.my_display_name {
            Some(name) => Ok(name.as_deref()),
            None => Err(HostError::NoEntity),
        }
    }

    pub fn friend_contains(&self, name: &str) -> Result<bool, HostError> {
        if self.friend_slot(name) >= 0 {
            return Ok(true);
        }
        Ok(self
            .my_name()?
            .is_some_and(|mine| equals_ignore_case(name, mine)))
    }

    pub fn ignore_count(&self) -> i32 {
        if self.friends_loaded == 0 {
            -1
        } else {
            self.ignores.len() as i32
        }
    }

    pub fn ignore(&self, index: i32) -> Result<Option<&Ignore>, HostError> {
        if self.friends_loaded != 0 && i64::from(index) < self.ignores.len() as i64 {
            usize::try_from(index)
                .ok()
                .and_then(|slot| self.ignores.get(slot))
                .map(Some)
                .ok_or_else(|| array_error(index))
        } else {
            Ok(None)
        }
    }

    pub fn ignore_is_temporary(&self, index: i32) -> Result<bool, HostError> {
        let slot = usize::try_from(index)
            .ok()
            .filter(|&slot| slot < IGNORE_CAPACITY)
            .ok_or_else(|| array_error(index))?;
        self.ignores
            .get(slot)
            .map(|ignore| ignore.temporary)
            .ok_or(HostError::NoEntity)
    }

    pub fn ignore_contains(&self, name: &str) -> bool {
        self.ignores.iter().any(|ignore| {
            ignore
                .unfiltered_username
                .as_deref()
                .is_some_and(|other| equals_ignore_case(name, other))
                || ignore
                    .display_name
                    .as_deref()
                    .is_some_and(|other| equals_ignore_case(name, other))
        })
    }

    pub fn fc_player(&self, index: i32) -> Result<Option<&FriendsChatPlayer>, HostError> {
        if self.fc_name.is_some() && index < self.fc_player_count {
            let players = self.fc_players.as_ref().ok_or(HostError::NoEntity)?;
            let slot = usize::try_from(index)
                .ok()
                .filter(|&slot| slot < players.len())
                .ok_or_else(|| array_error(index))?;
            players[slot].as_ref().map(Some).ok_or(HostError::NoEntity)
        } else {
            Ok(None)
        }
    }

    pub fn fc_count(&self) -> i32 {
        if self.fc_name.is_some() {
            self.fc_player_count
        } else {
            0
        }
    }

    pub fn fc_is_self(&self, index: i32) -> Result<bool, HostError> {
        let Some(players) = &self.fc_players else {
            return Ok(false);
        };
        if index >= self.fc_player_count {
            return Ok(false);
        }
        let slot = usize::try_from(index)
            .ok()
            .filter(|&slot| slot < players.len())
            .ok_or_else(|| array_error(index))?;
        let player = players[slot].as_ref().ok_or(HostError::NoEntity)?;
        let mine = self.my_name()?;
        Ok(nullable_equals_ignore_case(player.username.as_deref(), mine))
    }
}
