use crate::{HostError, Varbit};
use std::collections::BTreeMap;

pub const SKILL_COUNT: usize = 25;
pub const NPC_STAT_COUNT: usize = 6;
const INTEGER_VARN_TYPE: i32 = 105;

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct MyPlayer {
    pub combat_level: i32,
    pub female: Option<bool>,
    pub yaw_rotation: i32,
    pub username: Option<String>,
    pub display_name: Option<String>,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
}

impl MyPlayer {
    pub fn username_with_title(&self) -> String {
        let mut full = self.prefix_title.clone().unwrap_or_default();
        full.push_str(self.username.as_deref().unwrap_or("null"));
        if let Some(postfix) = &self.postfix_title {
            full.push_str(postfix);
        }
        full
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct PlayerState {
    pub skill_level_visible: [i32; SKILL_COUNT],
    pub skill_level_actual: [i32; SKILL_COUNT],
    pub skill_xp: [i32; SKILL_COUNT],
    pub reboot_timer: i32,
    pub run_energy: i32,
    pub run_weight: i32,
    pub player_rights: i32,
    pub player_mod_level: i32,
    pub is_member: bool,
    pub member_world: bool,
    pub world_id: Option<i32>,
    pub my_player: Option<MyPlayer>,
    pub my_player_index: i32,
}

impl Default for PlayerState {
    fn default() -> Self {
        Self {
            skill_level_visible: [0; SKILL_COUNT],
            skill_level_actual: [0; SKILL_COUNT],
            skill_xp: [0; SKILL_COUNT],
            reboot_timer: 0,
            run_energy: 0,
            run_weight: 0,
            player_rights: 0,
            player_mod_level: 0,
            is_member: false,
            member_world: false,
            world_id: None,
            my_player: None,
            my_player_index: 0,
        }
    }
}

fn index_error(index: i32) -> HostError {
    HostError::State(format!("array index {index} out of bounds"))
}

fn skill(values: &[i32; SKILL_COUNT], id: i32) -> Result<i32, HostError> {
    usize::try_from(id)
        .ok()
        .and_then(|index| values.get(index))
        .copied()
        .ok_or_else(|| index_error(id))
}

impl PlayerState {
    pub fn stat(&self, skill_id: i32) -> Result<i32, HostError> {
        skill(&self.skill_level_visible, skill_id)
    }

    pub fn stat_base(&self, skill_id: i32) -> Result<i32, HostError> {
        skill(&self.skill_level_actual, skill_id)
    }

    pub fn stat_xp(&self, skill_id: i32) -> Result<i32, HostError> {
        skill(&self.skill_xp, skill_id)
    }

    pub fn set_skill(&mut self, skill_id: usize, experience: i32, visible: i32, actual: i32) {
        if skill_id < SKILL_COUNT {
            self.skill_xp[skill_id] = experience;
            self.skill_level_visible[skill_id] = visible;
            self.skill_level_actual[skill_id] = actual;
        }
    }

    pub fn staff_mod_level(&self) -> i32 {
        if self.player_rights >= 2 {
            self.player_rights
        } else {
            0
        }
    }

    pub fn is_player_mod(&self) -> bool {
        (5..=9).contains(&self.player_mod_level)
    }

    pub fn player_mod_level(&self) -> i32 {
        if self.is_player_mod() {
            self.player_mod_level
        } else {
            0
        }
    }

    pub fn world_id(&self) -> Result<i32, HostError> {
        self.world_id.ok_or(HostError::NoEntity)
    }

    pub fn my_player(&self) -> Result<&MyPlayer, HostError> {
        self.my_player.as_ref().ok_or(HostError::NoEntity)
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct VarnBit {
    pub varn: u16,
    pub start: u8,
    pub end: u8,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct NpcState {
    pub type_id: Option<i32>,
    pub max_stats: [i32; NPC_STAT_COUNT],
    pub current_stats: [i32; NPC_STAT_COUNT],
    pub varns: BTreeMap<i32, i32>,
    pub varn_types: BTreeMap<i32, i32>,
    pub varn_bits: BTreeMap<i32, VarnBit>,
}

impl NpcState {
    pub fn npc_type(&self) -> Result<i32, HostError> {
        self.type_id.ok_or(HostError::NoEntity)
    }

    pub fn stat_pair(&self, index: i32) -> Result<(i32, i32), HostError> {
        let slot = usize::try_from(index)
            .ok()
            .filter(|slot| *slot < NPC_STAT_COUNT)
            .ok_or_else(|| index_error(index))?;
        Ok((self.max_stats[slot], self.current_stats[slot]))
    }

    pub fn varn(&self, id: i32) -> i32 {
        if let Some(value) = self.varns.get(&id) {
            return *value;
        }
        if self.varn_types.get(&id).copied().unwrap_or(0) == INTEGER_VARN_TYPE {
            0
        } else {
            -1
        }
    }

    pub fn varn_bit(&self, id: i32) -> i32 {
        let definition = self.varn_bits.get(&id).copied().unwrap_or(VarnBit {
            varn: 0,
            start: 0,
            end: 0,
        });
        Varbit {
            varp: definition.varn,
            start: definition.start,
            end: definition.end,
        }
        .entity_read(self.varn(i32::from(definition.varn)))
    }
}
