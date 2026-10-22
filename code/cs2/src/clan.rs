use crate::{ClanVarValue, HostError};
use std::collections::BTreeMap;

pub const STOCK_SLOT_COUNT: usize = 6;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ClanChannelPlayer {
    pub name: Option<String>,
    pub rank: i8,
    pub world: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ClanChannel {
    pub num_players: i32,
    pub clan_name: Option<String>,
    pub players: Option<Vec<Option<ClanChannelPlayer>>>,
    pub min_rank_to_kick: i8,
    pub guests_talk: i8,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ClanSettings {
    pub clan_name: Option<String>,
    pub allow_guests: bool,
    pub talk_rank: i8,
    pub kick_rank: i8,
    pub lootshare_rank: i8,
    pub coin_share: i8,
    pub member_count: i32,
    pub member_names: Option<Vec<String>>,
    pub member_ranks: Option<Vec<i8>>,
    pub member_join_days: Option<Vec<i32>>,
    pub ban_count: i32,
    pub banned_user_names: Option<Vec<String>>,
    pub current_owner: i32,
    pub replacement_owner: i32,
    pub clan_flags: Option<Vec<i32>>,
    pub variables: Option<BTreeMap<i32, ClanVarValue>>,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct StockSlot {
    pub progress: i8,
    pub item_id: i32,
    pub price: i32,
    pub amount: i32,
    pub current_amount: i32,
    pub total_price: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ClanState {
    pub channel: Option<ClanChannel>,
    pub listened_channel: Option<ClanChannel>,
    pub listened_is_channel: bool,
    pub settings: Option<ClanSettings>,
    pub guest_settings: Option<ClanSettings>,
    pub clan_vars: Option<Vec<Option<ClanVarValue>>>,
    pub game_id: Option<i32>,
    pub stock_slots: Vec<Option<StockSlot>>,
}

impl Default for ClanState {
    fn default() -> Self {
        Self {
            channel: None,
            listened_channel: None,
            listened_is_channel: false,
            settings: None,
            guest_settings: None,
            clan_vars: None,
            game_id: Some(0),
            stock_slots: vec![None; STOCK_SLOT_COUNT],
        }
    }
}

fn no_entity() -> HostError {
    HostError::NoEntity
}

fn out_of_bounds() -> HostError {
    HostError::State("array index out of bounds".to_string())
}

fn index(array_length: usize, slot: i32) -> Result<usize, HostError> {
    usize::try_from(slot)
        .ok()
        .filter(|&slot| slot < array_length)
        .ok_or_else(out_of_bounds)
}

pub(crate) fn element<T>(array: &Option<Vec<T>>, slot: i32) -> Result<&T, HostError> {
    let array = array.as_ref().ok_or_else(no_entity)?;
    Ok(&array[index(array.len(), slot)?])
}

fn compare_utf16(left: &str, right: &str) -> std::cmp::Ordering {
    left.encode_utf16().cmp(right.encode_utf16())
}

fn quicksort(names: &mut [String], ids: &mut [i32], from: i32, to: i32) {
    if from >= to {
        return;
    }
    let (from_slot, to_slot) = (from as usize, to as usize);
    let middle = ((to + from) / 2) as usize;
    let mut partition = from_slot;
    let pivot = names[middle].clone();
    names.swap(middle, to_slot);
    ids.swap(middle, to_slot);
    for i in from_slot..to_slot {
        let limit = i32::from(i & 1 == 1);
        let order = compare_utf16(&names[i], &pivot);
        let difference = match order {
            std::cmp::Ordering::Less => -1,
            std::cmp::Ordering::Equal => 0,
            std::cmp::Ordering::Greater => 1,
        };
        if difference < limit {
            names.swap(i, partition);
            ids.swap(i, partition);
            partition += 1;
        }
    }
    names.swap(to_slot, partition);
    ids.swap(to_slot, partition);
    quicksort(names, ids, from, partition as i32 - 1);
    quicksort(names, ids, partition as i32 + 1, to);
}

fn sorted_indices(mut names: Vec<String>) -> Vec<i32> {
    let mut ids: Vec<i32> = (0..names.len() as i32).collect();
    let last = names.len() as i32 - 1;
    quicksort(&mut names, &mut ids, 0, last);
    ids
}

impl ClanChannel {
    pub fn player(&self, slot: i32) -> Result<&ClanChannelPlayer, HostError> {
        element(&self.players, slot)?.as_ref().ok_or_else(no_entity)
    }

    pub fn user_slot(&self, name: &str) -> Result<i32, HostError> {
        for slot in 0..self.num_players {
            let player = self.player(slot)?;
            if player
                .name
                .as_deref()
                .is_some_and(|candidate| crate::equals_ignore_case(candidate, name))
            {
                return Ok(slot);
            }
        }
        Ok(-1)
    }

    pub fn players_sorted_by_name(&self) -> Result<Vec<i32>, HostError> {
        let mut names = Vec::new();
        for slot in 0..self.num_players.max(0) {
            names.push(self.player(slot)?.name.clone().ok_or_else(no_entity)?);
        }
        Ok(sorted_indices(names))
    }
}

impl ClanSettings {
    pub fn member_index(&self, name: Option<&str>) -> Result<i32, HostError> {
        if let Some(name) = name.filter(|name| !name.is_empty()) {
            for slot in 0..self.member_count {
                if element(&self.member_names, slot)? == name {
                    return Ok(slot);
                }
            }
        }
        Ok(-1)
    }

    pub fn clan_flag_bitfield(&self, slot: i32, start: i32, end: i32) -> Result<i32, HostError> {
        let mask = if end == 31 {
            -1
        } else {
            (1_i32.wrapping_shl((end.wrapping_add(1)) as u32 & 31)).wrapping_sub(1)
        };
        let flags = *element(&self.clan_flags, slot)?;
        Ok(((flags & mask) as u32 >> (start as u32 & 31)) as i32)
    }

    pub fn sorted_affined_slots(&self) -> Result<Vec<i32>, HostError> {
        let mut names = Vec::new();
        for slot in 0..self.member_count.max(0) {
            names.push(element(&self.member_names, slot)?.clone());
        }
        Ok(sorted_indices(names))
    }
}

impl StockSlot {
    pub fn state(&self) -> i32 {
        i32::from(self.progress) & 7
    }

    pub fn is_buy_offer(&self) -> bool {
        i32::from(self.progress) & 8 == 8
    }
}

impl ClanState {
    pub fn stock_slot(&self, slot: i32) -> Result<&StockSlot, HostError> {
        let index = index(self.stock_slots.len(), slot)?;
        self.stock_slots[index].as_ref().ok_or_else(no_entity)
    }
}
