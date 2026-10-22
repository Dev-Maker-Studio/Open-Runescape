use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::cmp::Ordering;
use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    UnsupportedOpcode(ServerPacket),
    Payload(PayloadError),
    NullPointer(&'static str),
    IndexOutOfBounds(&'static str),
    NegativeArraySize(&'static str),
    Runtime(&'static str),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnsupportedOpcode(opcode) => write!(f, "unsupported clan opcode {opcode:?}"),
            Self::Payload(error) => error.fmt(f),
            Self::NullPointer(site) => write!(f, "native null dereference at {site}"),
            Self::IndexOutOfBounds(site) => write!(f, "native array overflow at {site}"),
            Self::NegativeArraySize(site) => write!(f, "native negative array size at {site}"),
            Self::Runtime(site) => write!(f, "native runtime exception at {site}"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Payload(error) => Some(error),
            _ => None,
        }
    }
}

impl From<PayloadError> for Error {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

fn compare_utf16(l: &str, r: &str) -> Ordering {
    l.encode_utf16().cmp(r.encode_utf16())
}

fn unit_upper(unit: u16) -> u16 {
    match char::from_u32(u32::from(unit)) {
        Some(c) => {
            let mut mapped = c.to_uppercase();
            match (mapped.next(), mapped.next()) {
                (Some(u), None) if (u as u32) <= 0xffff => u as u32 as u16,
                _ => unit,
            }
        }
        None => unit,
    }
}

fn unit_lower(unit: u16) -> u16 {
    match char::from_u32(u32::from(unit)) {
        Some(c) => {
            let mut mapped = c.to_lowercase();
            match (mapped.next(), mapped.next()) {
                (Some(u), None) if (u as u32) <= 0xffff => u as u32 as u16,
                _ => unit,
            }
        }
        None => unit,
    }
}

pub fn equals_ignore_case(l: &str, r: &str) -> bool {
    let left: Vec<u16> = l.encode_utf16().collect();
    let right: Vec<u16> = r.encode_utf16().collect();
    if left.len() != right.len() {
        return false;
    }
    left.iter().zip(right.iter()).all(|(&a, &b)| {
        if a == b {
            return true;
        }
        let (ua, ub) = (unit_upper(a), unit_upper(b));
        ua == ub || unit_lower(ua) == unit_lower(ub)
    })
}

fn slot(index: i64, length: usize, site: &'static str) -> Result<usize, Error> {
    if index < 0 || index >= length as i64 {
        Err(Error::IndexOutOfBounds(site))
    } else {
        Ok(index as usize)
    }
}

fn get<T: Clone>(array: &[T], index: i32, site: &'static str) -> Result<T, Error> {
    Ok(array[slot(i64::from(index), array.len(), site)?].clone())
}

fn put<T>(array: &mut [T], index: i32, value: T, site: &'static str) -> Result<(), Error> {
    let at = slot(i64::from(index), array.len(), site)?;
    array[at] = value;
    Ok(())
}

fn allocation(size: i32, site: &'static str) -> Result<usize, Error> {
    usize::try_from(size).map_err(|_| Error::NegativeArraySize(site))
}

fn array<'a, T>(value: &'a Option<Vec<T>>, site: &'static str) -> Result<&'a Vec<T>, Error> {
    value.as_ref().ok_or(Error::NullPointer(site))
}

fn array_mut<'a, T>(
    value: &'a mut Option<Vec<T>>,
    site: &'static str,
) -> Result<&'a mut Vec<T>, Error> {
    value.as_mut().ok_or(Error::NullPointer(site))
}

pub fn copy_between<T: Clone>(
    source: &[T],
    source_position: i32,
    destination: &mut [T],
    destination_position: i32,
    length: i32,
    site: &'static str,
) -> Result<(), Error> {
    let mut offset = 0i64;
    while offset < i64::from(length) {
        let value = source[slot(i64::from(source_position) + offset, source.len(), site)?].clone();
        let at = slot(
            i64::from(destination_position) + offset,
            destination.len(),
            site,
        )?;
        destination[at] = value;
        offset += 1;
    }
    Ok(())
}

pub fn copy_same<T: Clone>(
    array: &mut [T],
    source_position: i32,
    destination_position: i32,
    length: i32,
    site: &'static str,
) -> Result<(), Error> {
    if source_position == destination_position || length <= 0 {
        return Ok(());
    }
    let (from, to, length) = (
        i64::from(source_position),
        i64::from(destination_position),
        i64::from(length),
    );
    if to > from && to < length + from {
        let mut offset = length - 1;
        while offset >= 0 {
            let value = array[slot(from + offset, array.len(), site)?].clone();
            let at = slot(to + offset, array.len(), site)?;
            array[at] = value;
            offset -= 1;
        }
    } else {
        let mut offset = 0;
        while offset < length {
            let value = array[slot(from + offset, array.len(), site)?].clone();
            let at = slot(to + offset, array.len(), site)?;
            array[at] = value;
            offset += 1;
        }
    }
    Ok(())
}

pub fn quicksort(strings: &mut [Option<String>], ints: &mut [i32], from: i32, to: i32) {
    let mut pending = vec![(from, to)];
    while let Some((from, to)) = pending.pop() {
        if from >= to {
            continue;
        }
        let middle = ((to + from) / 2) as usize;
        let (from_at, to_at) = (from as usize, to as usize);
        let pivot = strings[middle].clone();
        strings.swap(middle, to_at);
        let pivot_int = ints[middle];
        ints.swap(middle, to_at);
        let mut partition = from_at;
        for i in from_at..to_at {
            let take = match (&pivot, &strings[i]) {
                (None, _) => true,
                (Some(_), None) => false,
                (Some(p), Some(s)) => match compare_utf16(s, p) {
                    Ordering::Less => true,
                    Ordering::Equal => i & 1 == 1,
                    Ordering::Greater => false,
                },
            };
            if take {
                strings.swap(i, partition);
                ints.swap(i, partition);
                partition += 1;
            }
        }
        strings[to_at] = strings[partition].clone();
        strings[partition] = pivot;
        ints[to_at] = ints[partition];
        ints[partition] = pivot_int;
        pending.push((from, partition as i32 - 1));
        pending.push((partition as i32 + 1, to));
    }
}

fn read_hash(reader: &mut PayloadReader) -> Result<i64, Error> {
    let first = reader.read_u8()?;
    if first == 255 {
        return Ok(-1);
    }
    let mut value = i64::from(first);
    for &byte in reader.read_bytes(7)? {
        value = (value << 8) | i64::from(byte);
    }
    Ok(value)
}

fn skip_hash(reader: &mut PayloadReader) -> Result<(), Error> {
    if reader.read_u8()? != 255 {
        reader.read_bytes(7)?;
    }
    Ok(())
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct ClanChannelPlayer {
    pub name: Option<String>,
    pub rank: i8,
    pub world: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClanChannel {
    pub pointer: i64,
    pub next_update_number: i64,
    pub clan_name: Option<String>,
    pub min_rank_to_kick: i8,
    pub guests_talk: i8,
    pub num_players: i32,
    pub players: Option<Vec<Option<usize>>>,
    pub arena: Vec<ClanChannelPlayer>,
    pub sorted_name_indices: Option<Vec<i32>>,
}

impl ClanChannel {
    pub fn decode(reader: &mut PayloadReader) -> Result<Self, Error> {
        let flag = reader.read_u8()?;
        let names_as_long = flag & 1 != 0;
        let pointer = reader.read_i64_be()?;
        let next_update_number = reader.read_i64_be()?;
        let clan_name = Some(reader.read_cp1252()?);
        reader.read_u8()?;
        let min_rank_to_kick = reader.read_i8()?;
        let guests_talk = reader.read_i8()?;
        let num_players = i32::from(reader.read_u16_be()?);
        let mut players = None;
        let mut arena = Vec::new();
        if num_players > 0 {
            let mut slots = vec![None; num_players as usize];
            for entry in slots.iter_mut() {
                if names_as_long {
                    reader.read_i64_be()?;
                }
                let name = Some(reader.read_cp1252()?);
                let rank = reader.read_i8()?;
                let world = i32::from(reader.read_u16_be()?);
                arena.push(ClanChannelPlayer { name, rank, world });
                *entry = Some(arena.len() - 1);
            }
            players = Some(slots);
        }
        Ok(Self {
            pointer,
            next_update_number,
            clan_name,
            min_rank_to_kick,
            guests_talk,
            num_players,
            players,
            arena,
            sorted_name_indices: None,
        })
    }

    pub fn player(&self, slot_index: i32) -> Result<&ClanChannelPlayer, Error> {
        let slots = array(&self.players, "ClanChannel.players")?;
        let entry = get(slots, slot_index, "ClanChannel.players[]")?
            .ok_or(Error::NullPointer("ClanChannel.players[]"))?;
        Ok(&self.arena[entry])
    }

    pub fn get_user_slot(&self, name: &str) -> Result<i32, Error> {
        let mut index = 0;
        while index < self.num_players {
            let player = self.player(index)?;
            if let Some(candidate) = &player.name
                && equals_ignore_case(candidate, name)
            {
                return Ok(index);
            }
            index += 1;
        }
        Ok(-1)
    }

    pub fn get_players_sorted_by_name(&mut self) -> Result<Vec<i32>, Error> {
        if self.sorted_name_indices.is_none() {
            let count = allocation(self.num_players, "ClanChannel.sort")?;
            let mut names: Vec<Option<String>> = vec![Some(String::new()); count];
            self.sorted_name_indices = Some(vec![0; count]);
            for i in 0..count {
                let name = self
                    .player(i as i32)?
                    .name
                    .clone()
                    .ok_or(Error::NullPointer("ClanChannel.name"))?;
                names[i] = Some(name);
                if let Some(sorted) = self.sorted_name_indices.as_mut() {
                    sorted[i] = i as i32;
                }
            }
            let sorted = self.sorted_name_indices.as_mut().unwrap();
            quicksort(&mut names, sorted, 0, count as i32 - 1);
        }
        Ok(self.sorted_name_indices.clone().unwrap())
    }

    pub fn remove_player(&mut self, index: i32) -> Result<(), Error> {
        self.num_players = self.num_players.wrapping_sub(1);
        if self.num_players == 0 {
            self.players = None;
        } else {
            let remaining = self.num_players;
            let slots = array_mut(&mut self.players, "ClanChannel.removePlayer")?;
            copy_same(
                slots,
                index.wrapping_add(1),
                index,
                remaining.wrapping_sub(index),
                "ClanChannel.removePlayer",
            )?;
        }
        self.sorted_name_indices = None;
        Ok(())
    }

    pub fn expand(&mut self, new_size: i32) -> Result<(), Error> {
        let size = allocation(new_size, "ClanChannel.expand")?;
        let mut fresh = vec![None; size];
        if let Some(old) = &self.players {
            copy_between(
                old,
                0,
                &mut fresh,
                0,
                self.num_players,
                "ClanChannel.expand",
            )?;
        }
        self.players = Some(fresh);
        Ok(())
    }

    pub fn add_player(&mut self, player: ClanChannelPlayer) -> Result<(), Error> {
        let full = match &self.players {
            None => true,
            Some(slots) => i64::from(self.num_players) >= slots.len() as i64,
        };
        if full {
            self.expand(self.num_players.wrapping_add(5))?;
        }
        let at = self.num_players;
        self.num_players = self.num_players.wrapping_add(1);
        self.arena.push(player);
        let entry = self.arena.len() - 1;
        let slots = array_mut(&mut self.players, "ClanChannel.addPlayer")?;
        put(slots, at, Some(entry), "ClanChannel.addPlayer")?;
        self.sorted_name_indices = None;
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ClanChannelUpdate {
    AddPlayer {
        name: Option<String>,
        world: i32,
        rank: i8,
    },
    UpdatePlayer {
        index: i32,
        rank: i8,
        world: i32,
        name: String,
    },
    KickPlayer {
        index: i32,
    },
    UpdateRights {
        clan_name: Option<String>,
        guests_talk: i8,
        min_rank_to_kick: i8,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClanChannelDelta {
    pub pointer: i64,
    pub update_number: i64,
    pub updates: Vec<ClanChannelUpdate>,
}

impl ClanChannelDelta {
    pub fn decode(reader: &mut PayloadReader) -> Result<Self, Error> {
        let pointer = reader.read_i64_be()?;
        let update_number = reader.read_i64_be()?;
        let mut updates = Vec::new();
        loop {
            match reader.read_u8()? {
                0 => break,
                1 => {
                    skip_hash(reader)?;
                    let name = reader.read_nullable_cp1252()?;
                    let world = i32::from(reader.read_u16_be()?);
                    let rank = reader.read_i8()?;
                    reader.read_i64_be()?;
                    updates.push(ClanChannelUpdate::AddPlayer { name, world, rank });
                }
                2 => {
                    let index = i32::from(reader.read_u16_be()?);
                    let rank = reader.read_i8()?;
                    let world = i32::from(reader.read_u16_be()?);
                    reader.read_i64_be()?;
                    let name = reader.read_cp1252()?;
                    updates.push(ClanChannelUpdate::UpdatePlayer {
                        index,
                        rank,
                        world,
                        name,
                    });
                }
                3 => {
                    let index = i32::from(reader.read_u16_be()?);
                    reader.read_u8()?;
                    skip_hash(reader)?;
                    updates.push(ClanChannelUpdate::KickPlayer { index });
                }
                4 => {
                    let clan_name = reader.read_nullable_cp1252()?;
                    let mut guests_talk = 0;
                    let mut min_rank_to_kick = 0;
                    if clan_name.is_some() {
                        reader.read_u8()?;
                        guests_talk = reader.read_i8()?;
                        min_rank_to_kick = reader.read_i8()?;
                    }
                    updates.push(ClanChannelUpdate::UpdateRights {
                        clan_name,
                        guests_talk,
                        min_rank_to_kick,
                    });
                }
                _ => return Err(Error::Runtime("ClanChannelDelta.opcode")),
            }
        }
        Ok(Self {
            pointer,
            update_number,
            updates,
        })
    }

    pub fn apply(&self, channel: &mut ClanChannel) -> Result<(), Error> {
        if channel.pointer != self.pointer || channel.next_update_number != self.update_number {
            return Err(Error::Runtime("ClanChannelDelta.version"));
        }
        for update in &self.updates {
            match update {
                ClanChannelUpdate::AddPlayer { name, world, rank } => {
                    channel.add_player(ClanChannelPlayer {
                        name: name.clone(),
                        rank: *rank,
                        world: *world,
                    })?;
                }
                ClanChannelUpdate::UpdatePlayer {
                    index,
                    rank,
                    world,
                    name,
                } => {
                    let slots = array(&channel.players, "UpdatePlayer.players")?;
                    let entry = get(slots, *index, "UpdatePlayer.players[]")?
                        .ok_or(Error::NullPointer("UpdatePlayer.players[]"))?;
                    let player = &mut channel.arena[entry];
                    player.rank = *rank;
                    player.world = *world;
                    player.name = Some(name.clone());
                }
                ClanChannelUpdate::KickPlayer { index } => channel.remove_player(*index)?,
                ClanChannelUpdate::UpdateRights {
                    clan_name,
                    guests_talk,
                    min_rank_to_kick,
                } => {
                    channel.clan_name = clan_name.clone();
                    if clan_name.is_some() {
                        channel.guests_talk = *guests_talk;
                        channel.min_rank_to_kick = *min_rank_to_kick;
                    }
                }
            }
        }
        channel.next_update_number = channel.next_update_number.wrapping_add(1);
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum VarValue {
    Int(i32),
    Long(i64),
    Str(String),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VarMap {
    pub buckets: Vec<Vec<(i64, VarValue)>>,
}

impl VarMap {
    pub fn new(size: usize) -> Self {
        Self {
            buckets: vec![Vec::new(); size],
        }
    }

    fn bucket(&self, key: i64) -> usize {
        (key & (self.buckets.len() as i64 - 1)) as usize
    }

    pub fn find(&self, key: i64) -> Option<(usize, usize)> {
        let bucket = self.bucket(key);
        self.buckets[bucket]
            .iter()
            .position(|(pointer, _)| *pointer == key)
            .map(|position| (bucket, position))
    }

    pub fn put(&mut self, value: VarValue, key: i64) {
        let bucket = self.bucket(key);
        self.buckets[bucket].push((key, value));
    }

    pub fn get(&self, key: i64) -> Option<&VarValue> {
        self.find(key)
            .map(|(bucket, position)| &self.buckets[bucket][position].1)
    }
}

fn next_power_of_two(value: i32) -> i32 {
    let mut v = value.wrapping_sub(1);
    v |= ((v as u32) >> 1) as i32;
    v |= ((v as u32) >> 2) as i32;
    v |= ((v as u32) >> 4) as i32;
    v |= ((v as u32) >> 8) as i32;
    v |= ((v as u32) >> 16) as i32;
    v.wrapping_add(1)
}

fn bit_mask(size: i32) -> i32 {
    if size == 31 {
        -1
    } else {
        1i32.wrapping_shl(size.wrapping_add(1) as u32)
            .wrapping_sub(1)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClanSettings {
    pub current_owner: i32,
    pub replacement_owner: i32,
    pub ban_count: i32,
    pub banned_user_names: Option<Vec<Option<String>>>,
    pub member_count: i32,
    pub clan_name: Option<String>,
    pub member_names: Option<Vec<Option<String>>>,
    pub member_ranks: Option<Vec<i8>>,
    pub member_join_days: Option<Vec<i32>>,
    pub allow_guests: bool,
    pub talk_rank: i8,
    pub kick_rank: i8,
    pub lootshare_rank: i8,
    pub coin_share: i8,
    pub delta_version: i64,
    pub banned_user_hashes: Option<Vec<i64>>,
    pub variables: Option<VarMap>,
    pub sorted_affined_slots: Option<Vec<i32>>,
    pub update_count: i32,
    pub creation_day: i32,
    pub member_hashes: Option<Vec<i64>>,
    pub clan_flags: Option<Vec<i32>>,
    pub use_hashes: bool,
    pub use_names: bool,
}

impl ClanSettings {
    fn blank() -> Self {
        Self {
            current_owner: -1,
            replacement_owner: -1,
            ban_count: 0,
            banned_user_names: None,
            member_count: 0,
            clan_name: None,
            member_names: None,
            member_ranks: None,
            member_join_days: None,
            allow_guests: false,
            talk_rank: 0,
            kick_rank: 0,
            lootshare_rank: 0,
            coin_share: 0,
            delta_version: 0,
            banned_user_hashes: None,
            variables: None,
            sorted_affined_slots: None,
            update_count: 0,
            creation_day: 0,
            member_hashes: None,
            clan_flags: None,
            use_hashes: false,
            use_names: false,
        }
    }

    pub fn decode(reader: &mut PayloadReader) -> Result<Self, Error> {
        let mut this = Self::blank();
        let version = reader.read_u8()?;
        if !(1..=5).contains(&version) {
            return Err(Error::Runtime("ClanSettings.version"));
        }
        let attributes = reader.read_u8()?;
        this.use_hashes = attributes & 1 != 0;
        this.use_names = attributes & 2 != 0;
        this.update_count = reader.read_i32_be()?;
        this.creation_day = reader.read_i32_be()?;
        if version <= 3 && this.creation_day != 0 {
            this.creation_day = this.creation_day.wrapping_add(16_912_800);
        }
        this.member_count = i32::from(reader.read_u16_be()?);
        this.ban_count = i32::from(reader.read_u8()?);
        this.clan_name = Some(reader.read_cp1252()?);
        if version >= 4 {
            reader.read_i32_be()?;
        }
        this.allow_guests = reader.read_u8()? == 1;
        this.talk_rank = reader.read_i8()?;
        this.kick_rank = reader.read_i8()?;
        this.lootshare_rank = reader.read_i8()?;
        this.coin_share = reader.read_i8()?;
        if this.member_count > 0 {
            let count = this.member_count as usize;
            if this.use_hashes {
                this.member_hashes = Some(vec![0; count]);
            }
            if this.use_names {
                this.member_names = Some(vec![None; count]);
            }
            this.member_ranks = Some(vec![0; count]);
            this.clan_flags = Some(vec![0; count]);
            this.member_join_days = Some(vec![0; count]);
            for index in 0..count {
                if this.use_hashes {
                    this.member_hashes.as_mut().unwrap()[index] = reader.read_i64_be()?;
                }
                if this.use_names {
                    let name = reader
                        .read_nullable_cp1252()?
                        .ok_or(Error::NullPointer("ClanSettings.member name"))?;
                    this.member_names.as_mut().unwrap()[index] = Some(name);
                }
                this.member_ranks.as_mut().unwrap()[index] = reader.read_i8()?;
                if version >= 2 {
                    this.clan_flags.as_mut().unwrap()[index] = reader.read_i32_be()?;
                }
                this.member_join_days.as_mut().unwrap()[index] = if version >= 5 {
                    i32::from(reader.read_u16_be()?)
                } else {
                    0
                };
            }
            this.update_owners()?;
        }
        if this.ban_count > 0 {
            let count = this.ban_count as usize;
            if this.use_hashes {
                this.banned_user_hashes = Some(vec![0; count]);
            }
            if this.use_names {
                this.banned_user_names = Some(vec![None; count]);
            }
            for index in 0..count {
                if this.use_hashes {
                    this.banned_user_hashes.as_mut().unwrap()[index] = reader.read_i64_be()?;
                }
                if this.use_names {
                    let name = reader
                        .read_nullable_cp1252()?
                        .ok_or(Error::NullPointer("ClanSettings.banned name"))?;
                    this.banned_user_names.as_mut().unwrap()[index] = Some(name);
                }
            }
        }
        if version >= 3 {
            let size = i32::from(reader.read_u16_be()?);
            if size > 0 {
                let buckets = if size < 16 {
                    next_power_of_two(size)
                } else {
                    16
                };
                let mut map = VarMap::new(buckets as usize);
                for _ in 0..size {
                    let packed = reader.read_i32_be()?;
                    let key = i64::from(packed & 0x3fff_ffff);
                    match (packed as u32) >> 30 {
                        0 => map.put(VarValue::Int(reader.read_i32_be()?), key),
                        1 => map.put(VarValue::Long(reader.read_i64_be()?), key),
                        2 => map.put(VarValue::Str(reader.read_cp1252()?), key),
                        _ => {}
                    }
                }
                this.variables = Some(map);
            }
        }
        Ok(this)
    }

    pub fn get_member_index(&self, name: Option<&str>) -> Result<i32, Error> {
        if let Some(name) = name
            && !name.is_empty()
        {
            let mut index = 0;
            while index < self.member_count {
                let names = array(&self.member_names, "getMemberIndex.names")?;
                let candidate = get(names, index, "getMemberIndex.names[]")?;
                if candidate.as_deref() == Some(name) {
                    return Ok(index);
                }
                index += 1;
            }
        }
        Ok(-1)
    }

    pub fn get_clan_flag_bitfield(
        &self,
        slot_index: i32,
        start: i32,
        end: i32,
    ) -> Result<i32, Error> {
        let mask = bit_mask(end);
        let flags = array(&self.clan_flags, "getClanFlagBitfield.flags")?;
        let value = get(flags, slot_index, "getClanFlagBitfield.flags[]")?;
        Ok(((value & mask) as u32).wrapping_shr(start as u32) as i32)
    }

    pub fn get_variable_bitfield(&self, key: i32, start: i32, end: i32) -> Option<i32> {
        match self.variables.as_ref()?.get(i64::from(key))? {
            VarValue::Int(value) => {
                Some(((value & bit_mask(end)) as u32).wrapping_shr(start as u32) as i32)
            }
            _ => None,
        }
    }

    pub fn get_int_var(&self, key: i32) -> Option<i32> {
        match self.variables.as_ref()?.get(i64::from(key))? {
            VarValue::Int(value) => Some(*value),
            _ => None,
        }
    }

    pub fn get_long_var(&self, key: i32) -> Option<i64> {
        match self.variables.as_ref()?.get(i64::from(key))? {
            VarValue::Long(value) => Some(*value),
            _ => None,
        }
    }

    pub fn get_string_var(&self, key: i32) -> Option<&str> {
        match self.variables.as_ref()?.get(i64::from(key))? {
            VarValue::Str(value) => Some(value),
            _ => None,
        }
    }

    pub fn update_owners(&mut self) -> Result<(), Error> {
        if self.member_count == 0 {
            self.current_owner = -1;
            self.replacement_owner = -1;
            return Ok(());
        }
        self.current_owner = -1;
        self.replacement_owner = -1;
        let mut maximum_index = 0;
        let mut maximum = get(
            array(&self.member_ranks, "updateOwners.ranks")?,
            0,
            "updateOwners.ranks[]",
        )?;
        let mut index = 1;
        while index < self.member_count {
            let rank = get(
                array(&self.member_ranks, "updateOwners.ranks")?,
                index,
                "updateOwners.ranks[]",
            )?;
            if rank > maximum {
                if maximum == 125 {
                    self.replacement_owner = maximum_index;
                }
                maximum_index = index;
                maximum = rank;
            } else if self.replacement_owner == -1 && rank == 125 {
                self.replacement_owner = index;
            }
            index += 1;
        }
        self.current_owner = maximum_index;
        put(
            array_mut(&mut self.member_ranks, "updateOwners.ranks")?,
            maximum_index,
            126,
            "updateOwners.ranks[]",
        )
    }

    pub fn get_sorted_affined_slots(&mut self) -> Result<Vec<i32>, Error> {
        if self.sorted_affined_slots.is_none() {
            let count = allocation(self.member_count, "getSortedAffinedSlots")?;
            let mut names: Vec<Option<String>> = Vec::with_capacity(count);
            for index in 0..count {
                let source = array(&self.member_names, "getSortedAffinedSlots.names")?;
                names.push(get(source, index as i32, "getSortedAffinedSlots.names[]")?);
            }
            self.sorted_affined_slots = Some((0..count as i32).collect());
            let sorted = self.sorted_affined_slots.as_mut().unwrap();
            quicksort(&mut names, sorted, 0, count as i32 - 1);
        }
        Ok(self.sorted_affined_slots.clone().unwrap())
    }

    pub fn ban_user(&mut self, hash: i64, name: Option<&str>) -> Result<(), Error> {
        let name = name.filter(|value| !value.is_empty());
        if self.use_hashes != (hash > 0) {
            return Err(Error::Runtime("banUser.hash"));
        }
        if name.is_none() == self.use_names {
            return Err(Error::Runtime("banUser.name"));
        }
        let hashes_full = match &self.banned_user_hashes {
            None => true,
            Some(values) => i64::from(self.ban_count) >= values.len() as i64,
        };
        let names_full = match &self.banned_user_names {
            None => true,
            Some(values) => i64::from(self.ban_count) >= values.len() as i64,
        };
        if (hash > 0 && hashes_full) || (name.is_some() && names_full) {
            self.expand_ban_trackers(self.ban_count.wrapping_add(5))?;
        }
        if let Some(values) = self.banned_user_hashes.as_mut() {
            put(values, self.ban_count, hash, "banUser.hashes[]")?;
        }
        if let Some(values) = self.banned_user_names.as_mut() {
            let name = name.ok_or(Error::NullPointer("banUser.name"))?;
            put(
                values,
                self.ban_count,
                Some(name.to_string()),
                "banUser.names[]",
            )?;
        }
        self.ban_count = self.ban_count.wrapping_add(1);
        Ok(())
    }

    pub fn unban_user(&mut self, index: i32) -> Result<(), Error> {
        self.ban_count = self.ban_count.wrapping_sub(1);
        if self.ban_count == 0 {
            self.banned_user_hashes = None;
            self.banned_user_names = None;
        } else {
            let remaining = self.ban_count.wrapping_sub(index);
            if let Some(values) = self.banned_user_hashes.as_mut() {
                copy_same(
                    values,
                    index.wrapping_add(1),
                    index,
                    remaining,
                    "unbanUser.hashes",
                )?;
            }
            if let Some(values) = self.banned_user_names.as_mut() {
                copy_same(
                    values,
                    index.wrapping_add(1),
                    index,
                    remaining,
                    "unbanUser.names",
                )?;
            }
        }
        Ok(())
    }

    pub fn expand_ban_trackers(&mut self, new_size: i32) -> Result<(), Error> {
        if self.use_hashes {
            let size = allocation(new_size, "expandBanTrackers.hashes")?;
            let mut fresh = vec![0i64; size];
            if let Some(old) = &self.banned_user_hashes {
                copy_between(
                    old,
                    0,
                    &mut fresh,
                    0,
                    self.ban_count,
                    "expandBanTrackers.hashes",
                )?;
            }
            self.banned_user_hashes = Some(fresh);
        }
        if self.use_names {
            let size = allocation(new_size, "expandBanTrackers.names")?;
            let mut fresh: Vec<Option<String>> = vec![None; size];
            if let Some(old) = &self.banned_user_names {
                copy_between(
                    old,
                    0,
                    &mut fresh,
                    0,
                    self.ban_count,
                    "expandBanTrackers.names",
                )?;
            }
            self.banned_user_names = Some(fresh);
        }
        Ok(())
    }

    pub fn update_rank(&mut self, index: i32, rank: i8) -> Result<i32, Error> {
        if rank == 126 || rank == 127 {
            return Ok(-1);
        }
        if self.current_owner == index {
            let blocked = if self.replacement_owner == -1 {
                true
            } else {
                get(
                    array(&self.member_ranks, "updateRank.ranks")?,
                    self.replacement_owner,
                    "updateRank.ranks[]",
                )? < 125
            };
            if blocked {
                return Ok(-1);
            }
        }
        let ranks = array_mut(&mut self.member_ranks, "updateRank.ranks")?;
        if get(ranks, index, "updateRank.ranks[]")? == rank {
            return Ok(-1);
        }
        put(ranks, index, rank, "updateRank.ranks[]")?;
        self.update_owners()?;
        Ok(index)
    }

    pub fn update_clan_flag(
        &mut self,
        index: i32,
        added_flag: i32,
        shift: i32,
        bitmask_size: i32,
    ) -> Result<i32, Error> {
        let mask = 1i32.wrapping_shl(shift as u32).wrapping_sub(1);
        let clear_mask = bit_mask(bitmask_size) ^ mask;
        let flag = added_flag.wrapping_shl(shift as u32) & clear_mask;
        let flags = array_mut(&mut self.clan_flags, "updateClanFlag.flags")?;
        let current = get(flags, index, "updateClanFlag.flags[]")?;
        if current & clear_mask == flag {
            return Ok(-1);
        }
        put(
            flags,
            index,
            (current & !clear_mask) | flag,
            "updateClanFlag.flags[]",
        )?;
        Ok(index)
    }

    pub fn update_bitpacked_variable(
        &mut self,
        key: i32,
        value: i32,
        shift: i32,
        bitmask_size: i32,
    ) -> bool {
        let mask = 1i32.wrapping_shl(shift as u32).wrapping_sub(1);
        let clear_mask = bit_mask(bitmask_size) ^ mask;
        let packed = value.wrapping_shl(shift as u32) & clear_mask;
        let key = i64::from(key);
        let map = self.variables.get_or_insert_with(|| VarMap::new(4));
        if let Some((bucket, position)) = map.find(key) {
            if let VarValue::Int(current) = &mut map.buckets[bucket][position].1 {
                if *current & clear_mask == packed {
                    return false;
                }
                *current = (*current & !clear_mask) | packed;
                return true;
            }
            map.buckets[bucket].remove(position);
        }
        map.put(VarValue::Int(packed), key);
        true
    }

    pub fn set_long_variable(&mut self, key: i32, value: i64) -> bool {
        let key = i64::from(key);
        let map = self.variables.get_or_insert_with(|| VarMap::new(4));
        if let Some((bucket, position)) = map.find(key) {
            if let VarValue::Long(current) = &mut map.buckets[bucket][position].1 {
                if *current == value {
                    return false;
                }
                *current = value;
                return true;
            }
            map.buckets[bucket].remove(position);
        }
        map.put(VarValue::Long(value), key);
        true
    }

    pub fn put_int_variable(&mut self, key: i32, value: i32) -> bool {
        let key = i64::from(key);
        let map = self.variables.get_or_insert_with(|| VarMap::new(4));
        if let Some((bucket, position)) = map.find(key) {
            if let VarValue::Int(current) = &mut map.buckets[bucket][position].1 {
                if *current == value {
                    return false;
                }
                *current = value;
                return true;
            }
            map.buckets[bucket].remove(position);
        }
        map.put(VarValue::Int(value), key);
        true
    }

    pub fn put_string_value(&mut self, key: i32, value: Option<&str>) -> bool {
        let units: Vec<u16> = value.unwrap_or("").encode_utf16().take(80).collect();
        let text = String::from_utf16_lossy(&units);
        let key = i64::from(key);
        let map = self.variables.get_or_insert_with(|| VarMap::new(4));
        if let Some((bucket, position)) = map.find(key) {
            if let VarValue::Str(current) = &map.buckets[bucket][position].1
                && *current == text
            {
                return false;
            }
            map.buckets[bucket].remove(position);
        }
        map.put(VarValue::Str(text), key);
        true
    }

    pub fn add_member(
        &mut self,
        hash: i64,
        name: Option<&str>,
        join_day: i32,
    ) -> Result<(), Error> {
        let name = name.filter(|value| !value.is_empty());
        if (hash > 0) != self.use_hashes {
            return Err(Error::Runtime("addMember.hash"));
        }
        if self.use_names == name.is_none() {
            return Err(Error::Runtime("addMember.name"));
        }
        let count = i64::from(self.member_count);
        let hashes_full = match &self.member_hashes {
            None => true,
            Some(values) => count >= values.len() as i64,
        };
        let names_full = match &self.member_names {
            None => true,
            Some(values) => count >= values.len() as i64,
        };
        if (hash > 0 && hashes_full) || (name.is_some() && names_full) {
            self.expand_member_trackers(self.member_count.wrapping_add(5))?;
        }
        let at = self.member_count;
        if let Some(values) = self.member_hashes.as_mut() {
            put(values, at, hash, "addMember.hashes[]")?;
        }
        if let Some(values) = self.member_names.as_mut() {
            let name = name.ok_or(Error::NullPointer("addMember.name"))?;
            put(values, at, Some(name.to_string()), "addMember.names[]")?;
        }
        if self.current_owner == -1 {
            self.current_owner = at;
            put(
                array_mut(&mut self.member_ranks, "addMember.ranks")?,
                at,
                126,
                "addMember.ranks[]",
            )?;
        } else {
            put(
                array_mut(&mut self.member_ranks, "addMember.ranks")?,
                at,
                0,
                "addMember.ranks[]",
            )?;
        }
        put(
            array_mut(&mut self.clan_flags, "addMember.flags")?,
            at,
            0,
            "addMember.flags[]",
        )?;
        put(
            array_mut(&mut self.member_join_days, "addMember.joinDays")?,
            at,
            join_day,
            "addMember.joinDays[]",
        )?;
        self.member_count = self.member_count.wrapping_add(1);
        self.sorted_affined_slots = None;
        Ok(())
    }

    pub fn expand_member_trackers(&mut self, size: i32) -> Result<(), Error> {
        let count = self.member_count;
        if self.use_hashes {
            let length = allocation(size, "expandMemberTrackers.hashes")?;
            let mut fresh = vec![0i64; length];
            if let Some(old) = &self.member_hashes {
                copy_between(old, 0, &mut fresh, 0, count, "expandMemberTrackers.hashes")?;
            }
            self.member_hashes = Some(fresh);
        }
        if self.use_names {
            let length = allocation(size, "expandMemberTrackers.names")?;
            let mut fresh: Vec<Option<String>> = vec![None; length];
            if let Some(old) = &self.member_names {
                copy_between(old, 0, &mut fresh, 0, count, "expandMemberTrackers.names")?;
            }
            self.member_names = Some(fresh);
        }
        let length = allocation(size, "expandMemberTrackers.ranks")?;
        let mut fresh = vec![0i8; length];
        if let Some(old) = &self.member_ranks {
            copy_between(old, 0, &mut fresh, 0, count, "expandMemberTrackers.ranks")?;
        }
        self.member_ranks = Some(fresh);
        let mut fresh = vec![0i32; length];
        if let Some(old) = &self.clan_flags {
            copy_between(old, 0, &mut fresh, 0, count, "expandMemberTrackers.flags")?;
        }
        self.clan_flags = Some(fresh);
        let mut fresh = vec![0i32; length];
        if let Some(old) = &self.member_join_days {
            copy_between(
                old,
                0,
                &mut fresh,
                0,
                count,
                "expandMemberTrackers.joinDays",
            )?;
        }
        self.member_join_days = Some(fresh);
        Ok(())
    }

    pub fn remove_member(&mut self, index: i32) -> Result<(), Error> {
        if index < 0 || index >= self.member_count {
            return Err(Error::Runtime("removeMember.index"));
        }
        self.sorted_affined_slots = None;
        self.member_count -= 1;
        if self.member_count == 0 {
            self.member_hashes = None;
            self.member_names = None;
            self.member_ranks = None;
            self.clan_flags = None;
            self.member_join_days = None;
            self.current_owner = -1;
            self.replacement_owner = -1;
        } else {
            let remaining = self.member_count - index;
            copy_same(
                array_mut(&mut self.member_ranks, "removeMember.ranks")?,
                index + 1,
                index,
                remaining,
                "removeMember.ranks",
            )?;
            copy_same(
                array_mut(&mut self.clan_flags, "removeMember.flags")?,
                index + 1,
                index,
                remaining,
                "removeMember.flags",
            )?;
            copy_same(
                array_mut(&mut self.member_join_days, "removeMember.joinDays")?,
                index + 1,
                index,
                remaining,
                "removeMember.joinDays",
            )?;
            if let Some(values) = self.member_hashes.as_mut() {
                copy_same(values, index + 1, index, remaining, "removeMember.hashes")?;
            }
            if let Some(values) = self.member_names.as_mut() {
                copy_same(values, index + 1, index, remaining, "removeMember.names")?;
            }
            if self.current_owner == index || self.replacement_owner == index {
                self.update_owners()?;
            }
        }
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ClanSettingsUpdate {
    BanUser {
        hash: i64,
        name: Option<String>,
    },
    AddNewMember {
        hash: i64,
        name: Option<String>,
    },
    AddExistingMember {
        hash: i64,
        name: Option<String>,
        join_runeday: i32,
    },
    UpdatePermission {
        allow_guests: bool,
        talk_rank: i8,
        kick_rank: i8,
        lootshare_rank: i8,
        coin_share: i8,
    },
    UnbanUser {
        index: i32,
    },
    RemoveMember {
        index: i32,
    },
    UpdateRank {
        index: i32,
        rank: i8,
    },
    UpdateClanFlag {
        index: i32,
        flag: i32,
        shift: i32,
        bitmask_size: i32,
    },
    UpdateIntVariable {
        key: i32,
        value: i32,
    },
    UpdateLongVariable {
        key: i32,
        value: i64,
    },
    UpdateStringVariable {
        key: i32,
        value: String,
    },
    UpdateBitpackedVariable {
        key: i32,
        value: i32,
        shift: i32,
        bitmask_size: i32,
    },
    UpdateClanName {
        name: String,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClanSettingsDelta {
    pub delta_version: i64,
    pub update_count: i32,
    pub updates: Vec<ClanSettingsUpdate>,
}

impl ClanSettingsDelta {
    pub fn decode(reader: &mut PayloadReader) -> Result<Self, Error> {
        let delta_version = reader.read_i64_be()?;
        let update_count = reader.read_i32_be()?;
        let mut updates = Vec::new();
        loop {
            let update = match reader.read_u8()? {
                0 => break,
                3 => {
                    let hash = read_hash(reader)?;
                    let name = reader.read_nullable_cp1252()?;
                    ClanSettingsUpdate::BanUser { hash, name }
                }
                1 => {
                    let hash = read_hash(reader)?;
                    let name = reader.read_nullable_cp1252()?;
                    ClanSettingsUpdate::AddNewMember { hash, name }
                }
                13 => {
                    let hash = read_hash(reader)?;
                    let name = reader.read_nullable_cp1252()?;
                    let join_runeday = i32::from(reader.read_u16_be()?);
                    ClanSettingsUpdate::AddExistingMember {
                        hash,
                        name,
                        join_runeday,
                    }
                }
                4 => ClanSettingsUpdate::UpdatePermission {
                    allow_guests: reader.read_u8()? == 1,
                    talk_rank: reader.read_i8()?,
                    kick_rank: reader.read_i8()?,
                    lootshare_rank: reader.read_i8()?,
                    coin_share: reader.read_i8()?,
                },
                6 => ClanSettingsUpdate::UnbanUser {
                    index: i32::from(reader.read_u16_be()?),
                },
                5 => ClanSettingsUpdate::RemoveMember {
                    index: i32::from(reader.read_u16_be()?),
                },
                2 => {
                    let index = i32::from(reader.read_u16_be()?);
                    let rank = reader.read_i8()?;
                    ClanSettingsUpdate::UpdateRank { index, rank }
                }
                7 => {
                    let index = i32::from(reader.read_u16_be()?);
                    let flag = reader.read_i32_be()?;
                    let shift = i32::from(reader.read_u8()?);
                    let bitmask_size = i32::from(reader.read_u8()?);
                    ClanSettingsUpdate::UpdateClanFlag {
                        index,
                        flag,
                        shift,
                        bitmask_size,
                    }
                }
                8 => {
                    let key = reader.read_i32_be()?;
                    let value = reader.read_i32_be()?;
                    ClanSettingsUpdate::UpdateIntVariable { key, value }
                }
                9 => {
                    let key = reader.read_i32_be()?;
                    let value = reader.read_i64_be()?;
                    ClanSettingsUpdate::UpdateLongVariable { key, value }
                }
                10 => {
                    let key = reader.read_i32_be()?;
                    let value = reader.read_cp1252()?;
                    ClanSettingsUpdate::UpdateStringVariable { key, value }
                }
                11 => {
                    let key = reader.read_i32_be()?;
                    let value = reader.read_i32_be()?;
                    let shift = i32::from(reader.read_u8()?);
                    let bitmask_size = i32::from(reader.read_u8()?);
                    ClanSettingsUpdate::UpdateBitpackedVariable {
                        key,
                        value,
                        shift,
                        bitmask_size,
                    }
                }
                12 => {
                    let name = reader.read_cp1252()?;
                    reader.read_i32_be()?;
                    ClanSettingsUpdate::UpdateClanName { name }
                }
                _ => return Err(Error::Runtime("ClanSettingsDelta.opcode")),
            };
            updates.push(update);
        }
        Ok(Self {
            delta_version,
            update_count,
            updates,
        })
    }

    pub fn queue_updates(&self, settings: &mut ClanSettings) -> Result<(), Error> {
        if settings.delta_version != self.delta_version {
            return Err(Error::Runtime("ClanSettingsDelta.deltaVersion"));
        }
        if self.update_count != settings.update_count {
            return Err(Error::Runtime("ClanSettingsDelta.updateCount"));
        }
        for update in &self.updates {
            match update {
                ClanSettingsUpdate::BanUser { hash, name } => {
                    settings.ban_user(*hash, name.as_deref())?
                }
                ClanSettingsUpdate::AddNewMember { hash, name } => {
                    settings.add_member(*hash, name.as_deref(), 0)?
                }
                ClanSettingsUpdate::AddExistingMember {
                    hash,
                    name,
                    join_runeday,
                } => settings.add_member(*hash, name.as_deref(), *join_runeday)?,
                ClanSettingsUpdate::UpdatePermission {
                    allow_guests,
                    talk_rank,
                    kick_rank,
                    lootshare_rank,
                    coin_share,
                } => {
                    settings.allow_guests = *allow_guests;
                    settings.talk_rank = *talk_rank;
                    settings.kick_rank = *kick_rank;
                    settings.lootshare_rank = *lootshare_rank;
                    settings.coin_share = *coin_share;
                }
                ClanSettingsUpdate::UnbanUser { index } => settings.unban_user(*index)?,
                ClanSettingsUpdate::RemoveMember { index } => settings.remove_member(*index)?,
                ClanSettingsUpdate::UpdateRank { index, rank } => {
                    settings.update_rank(*index, *rank)?;
                }
                ClanSettingsUpdate::UpdateClanFlag {
                    index,
                    flag,
                    shift,
                    bitmask_size,
                } => {
                    settings.update_clan_flag(*index, *flag, *shift, *bitmask_size)?;
                }
                ClanSettingsUpdate::UpdateIntVariable { key, value } => {
                    settings.put_int_variable(*key, *value);
                }
                ClanSettingsUpdate::UpdateLongVariable { key, value } => {
                    settings.set_long_variable(*key, *value);
                }
                ClanSettingsUpdate::UpdateStringVariable { key, value } => {
                    settings.put_string_value(*key, Some(value));
                }
                ClanSettingsUpdate::UpdateBitpackedVariable {
                    key,
                    value,
                    shift,
                    bitmask_size,
                } => {
                    settings.update_bitpacked_variable(*key, *value, *shift, *bitmask_size);
                }
                ClanSettingsUpdate::UpdateClanName { name } => {
                    settings.clan_name = Some(name.clone());
                }
            }
        }
        settings.update_count = settings.update_count.wrapping_add(1);
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ClanVar {
    Int(i32),
    Long(i64),
    Str(String),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Context {
    pub server_tick: i32,
    pub clan_var_count: Option<usize>,
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct ClanModel {
    pub last_clan_channel_update_tick: i32,
    pub last_clan_settings_update_tick: i32,
    pub clan_channel: Option<ClanChannel>,
    pub listened_clan_channel: Option<ClanChannel>,
    pub clan_settings: Option<ClanSettings>,
    pub guest_clan_settings: Option<ClanSettings>,
    pub clan_vars: Option<Vec<Option<ClanVar>>>,
    pub clan_var_keys: [i32; 32],
    pub clan_var_counter: i32,
}

impl ClanModel {
    pub fn clan_profile_found(&self) -> bool {
        self.clan_vars.is_some()
    }

    pub fn apply(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
        context: &Context,
    ) -> Result<(), Error> {
        let mut reader = PayloadReader::new(payload);
        match packet {
            ServerPacket::CLANCHANNEL_FULL => {
                self.last_clan_channel_update_tick = context.server_tick;
                let non_guest = reader.read_u8()? == 1;
                let channel = if payload.len() == 1 {
                    None
                } else {
                    Some(ClanChannel::decode(&mut reader)?)
                };
                if non_guest {
                    self.clan_channel = channel;
                } else {
                    self.listened_clan_channel = channel;
                }
                Ok(())
            }
            ServerPacket::CLANCHANNEL_DELTA => {
                self.last_clan_channel_update_tick = context.server_tick;
                let non_guest = reader.read_u8()? == 1;
                let delta = ClanChannelDelta::decode(&mut reader)?;
                let target = if non_guest {
                    self.clan_channel.as_mut()
                } else {
                    self.listened_clan_channel.as_mut()
                };
                match target {
                    Some(channel) => delta.apply(channel),
                    None => Ok(()),
                }
            }
            ServerPacket::CLANSETTINGS_FULL => {
                self.last_clan_settings_update_tick = context.server_tick;
                let non_guest = reader.read_u8()? == 1;
                let settings = if payload.len() == 1 {
                    None
                } else {
                    Some(ClanSettings::decode(&mut reader)?)
                };
                if non_guest {
                    self.clan_settings = settings;
                } else {
                    self.guest_clan_settings = settings;
                }
                Ok(())
            }
            ServerPacket::CLANSETTINGS_DELTA => {
                self.last_clan_settings_update_tick = context.server_tick;
                let non_guest = reader.read_u8()? == 1;
                let delta = ClanSettingsDelta::decode(&mut reader)?;
                let target = if non_guest {
                    self.clan_settings.as_mut()
                } else {
                    self.guest_clan_settings.as_mut()
                };
                match target {
                    Some(settings) => delta.queue_updates(settings),
                    None => Ok(()),
                }
            }
            ServerPacket::VARCLAN_SET_BYTE => {
                let key = i32::from(reader.read_u16_be()?);
                let value = ClanVar::Int(i32::from(reader.read_i8()?));
                self.store_var(key, value, context)
            }
            ServerPacket::VARCLAN_SET_INT => {
                let key = i32::from(reader.read_u16_be()?);
                let value = ClanVar::Int(reader.read_i32_be()?);
                self.store_var(key, value, context)
            }
            ServerPacket::VARCLAN_SET_LONG => {
                let key = i32::from(reader.read_u16_be()?);
                let value = ClanVar::Long(reader.read_i64_be()?);
                self.store_var(key, value, context)
            }
            ServerPacket::SET_CLAN_STRING => {
                let key = i32::from(reader.read_u16_be()?);
                let value = ClanVar::Str(reader.read_cp1252()?);
                self.store_var(key, value, context)
            }
            ServerPacket::VARCLAN_ENABLE => {
                let count = context
                    .clan_var_count
                    .ok_or(Error::NullPointer("VARCLAN_ENABLE.CLAN_VAR"))?;
                self.clan_vars = Some(vec![None; count]);
                Ok(())
            }
            ServerPacket::VARCLAN_DISABLE => {
                self.clan_vars = None;
                Ok(())
            }
            other => Err(Error::UnsupportedOpcode(other)),
        }
    }

    fn store_var(&mut self, key: i32, value: ClanVar, context: &Context) -> Result<(), Error> {
        if self.clan_vars.is_none() {
            let count = context
                .clan_var_count
                .ok_or(Error::NullPointer("CLAN_VAR"))?;
            self.clan_vars = Some(vec![None; count]);
        }
        put(
            array_mut(&mut self.clan_vars, "CLAN_VARS")?,
            key,
            Some(value),
            "CLAN_VARS[]",
        )?;
        self.clan_var_counter = self.clan_var_counter.wrapping_add(1);
        let at = (self.clan_var_counter.wrapping_sub(1) & 0x1f) as usize;
        self.clan_var_keys[at] = key;
        Ok(())
    }
}
