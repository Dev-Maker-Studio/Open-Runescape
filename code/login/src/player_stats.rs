use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::fmt;

pub const SKILL_COUNT: usize = 25;
pub const SKILL_UPDATE_RING_SIZE: usize = 32;
pub const UPDATE_STAT_BYTES: usize = 6;
pub const RUN_ENERGY_BYTES: usize = 1;
pub const PLAYER_WEIGHT_BYTES: usize = 2;
pub const SKILL_MAX_LEVEL: [u8; SKILL_COUNT] = [
    99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
    120,
];
pub const SKILL_XP_FOR_LEVEL: [i32; 120] = [
    83, 174, 276, 388, 512, 650, 801, 969, 1154, 1358, 1584, 1833, 2107, 2411, 2746, 3115, 3523,
    3973, 4470, 5018, 5624, 6291, 7028, 7842, 8740, 9730, 10824, 12031, 13363, 14833, 16456, 18247,
    20224, 22406, 24815, 27473, 30408, 33648, 37224, 41171, 45529, 50339, 55649, 61512, 67983,
    75127, 83014, 91721, 101333, 111945, 123660, 136594, 150872, 166636, 184040, 203254, 224466,
    247886, 273742, 302288, 333804, 368599, 407015, 449428, 496254, 547953, 605032, 668051, 737627,
    814445, 899257, 992895, 1096278, 1210421, 1336443, 1475581, 1629200, 1798808, 1986068, 2192818,
    2421087, 2673114, 2951373, 3258594, 3597792, 3972294, 4385776, 4842295, 5346332, 5902831,
    6517253, 7195629, 7944614, 8771558, 9684577, 10692629, 11805606, 13034431, 14391160, 15889109,
    17542976, 19368992, 21385073, 23611006, 26068632, 28782069, 31777943, 35085654, 38737661,
    42769801, 47221641, 52136869, 57563718, 63555443, 70170840, 77474828, 85539082, 94442737,
    104273167, 115126838,
];

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Limits {
    pub skill_count: usize,
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            skill_count: SKILL_COUNT,
        }
    }
}

impl Limits {
    fn validate(self) -> Result<Self, Error> {
        if self.skill_count == 0 || self.skill_count > SKILL_COUNT {
            return Err(Error::InvalidLimits);
        }
        Ok(self)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    InvalidLimits,
    InvalidSkill { skill_id: u8, skill_count: usize },
    UnexpectedLength { expected: usize, actual: usize },
    UnsupportedOpcode(ServerPacket),
    Payload(PayloadError),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidLimits => {
                write!(f, "player-stat skill count must be in 1..={SKILL_COUNT}")
            }
            Self::InvalidSkill {
                skill_id,
                skill_count,
            } => {
                write!(
                    f,
                    "skill {skill_id} is outside configured count {skill_count}"
                )
            }
            Self::UnexpectedLength { expected, actual } => {
                write!(
                    f,
                    "player-stat payload has {actual} bytes, expected {expected}"
                )
            }
            Self::UnsupportedOpcode(opcode) => {
                write!(f, "unsupported player-stat opcode {opcode:?}")
            }
            Self::Payload(error) => error.fmt(f),
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

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct SkillState {
    pub experience: i32,
    pub visible_level: u8,
    pub base_level: u8,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Update {
    Stat { skill_id: u8, state: SkillState },
    RunEnergy(u8),
    PlayerWeight(i16),
}

pub fn base_level_for_experience(skill_id: u8, experience: i32) -> Result<u8, Error> {
    let cap = *SKILL_MAX_LEVEL
        .get(usize::from(skill_id))
        .ok_or(Error::InvalidSkill {
            skill_id,
            skill_count: SKILL_COUNT,
        })?;
    let mut level = 1;
    for (index, &threshold) in SKILL_XP_FOR_LEVEL[..usize::from(cap - 1)]
        .iter()
        .enumerate()
    {
        if experience >= threshold {
            level = (index + 2) as u8;
        }
    }
    Ok(level)
}

pub fn decode(opcode: ServerPacket, payload: &[u8], limits: Limits) -> Result<Update, Error> {
    let limits = limits.validate()?;
    let expected = match opcode {
        ServerPacket::UPDATE_STAT => UPDATE_STAT_BYTES,
        ServerPacket::RUN_ENERGY => RUN_ENERGY_BYTES,
        ServerPacket::PLAYER_WEIGHT => PLAYER_WEIGHT_BYTES,
        _ => return Err(Error::UnsupportedOpcode(opcode)),
    };
    if payload.len() != expected {
        return Err(Error::UnexpectedLength {
            expected,
            actual: payload.len(),
        });
    }
    let mut reader = PayloadReader::new(payload);
    match opcode {
        ServerPacket::UPDATE_STAT => {
            let experience = reader.read_i32_be()?;
            let skill_id = reader.read_u8()?;
            let visible_level = reader.read_u8()?;
            if usize::from(skill_id) >= limits.skill_count {
                return Err(Error::InvalidSkill {
                    skill_id,
                    skill_count: limits.skill_count,
                });
            }
            Ok(Update::Stat {
                skill_id,
                state: SkillState {
                    experience,
                    visible_level,
                    base_level: base_level_for_experience(skill_id, experience)?,
                },
            })
        }
        ServerPacket::RUN_ENERGY => Ok(Update::RunEnergy(reader.read_u8()?)),
        ServerPacket::PLAYER_WEIGHT => Ok(Update::PlayerWeight(reader.read_i16_be()?)),
        _ => Err(Error::UnsupportedOpcode(opcode)),
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlayerStats {
    limits: Limits,
    skills: [SkillState; SKILL_COUNT],
    run_energy: u8,
    player_weight: i16,
    recent_skill_updates: [u8; SKILL_UPDATE_RING_SIZE],
    skill_update_cycle: i32,
}

impl PlayerStats {
    pub fn new(limits: Limits) -> Result<Self, Error> {
        Ok(Self {
            limits: limits.validate()?,
            skills: [SkillState::default(); SKILL_COUNT],
            run_energy: 0,
            player_weight: 0,
            recent_skill_updates: [0; SKILL_UPDATE_RING_SIZE],
            skill_update_cycle: 0,
        })
    }

    pub fn limits(&self) -> Limits {
        self.limits
    }

    pub fn skills(&self) -> &[SkillState] {
        &self.skills[..self.limits.skill_count]
    }

    pub fn skill(&self, skill_id: u8) -> Option<&SkillState> {
        self.skills().get(usize::from(skill_id))
    }

    pub fn run_energy(&self) -> u8 {
        self.run_energy
    }

    pub fn player_weight(&self) -> i16 {
        self.player_weight
    }

    pub fn recent_skill_updates(&self) -> &[u8; SKILL_UPDATE_RING_SIZE] {
        &self.recent_skill_updates
    }

    pub fn skill_update_cycle(&self) -> i32 {
        self.skill_update_cycle
    }

    pub fn apply(&mut self, opcode: ServerPacket, payload: &[u8]) -> Result<Update, Error> {
        let update = decode(opcode, payload, self.limits)?;
        match update {
            Update::Stat { skill_id, state } => {
                self.skills[usize::from(skill_id)] = state;
                let slot =
                    (self.skill_update_cycle as u32 & (SKILL_UPDATE_RING_SIZE as u32 - 1)) as usize;
                self.recent_skill_updates[slot] = skill_id;
                self.skill_update_cycle = self.skill_update_cycle.wrapping_add(1);
            }
            Update::RunEnergy(energy) => self.run_energy = energy,
            Update::PlayerWeight(weight) => self.player_weight = weight,
        }
        Ok(update)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn skill_cursor_wraps_like_native_int_arithmetic() {
        let mut state = PlayerStats::new(Limits::default()).unwrap();
        state.skill_update_cycle = i32::MAX;
        state
            .apply(ServerPacket::UPDATE_STAT, &[0, 0, 0, 0, 24, 255])
            .unwrap();
        assert_eq!(state.skill_update_cycle(), i32::MIN);
        assert_eq!(state.recent_skill_updates()[31], 24);
        state
            .apply(ServerPacket::UPDATE_STAT, &[0, 0, 0, 0, 7, 0])
            .unwrap();
        assert_eq!(state.skill_update_cycle(), i32::MIN + 1);
        assert_eq!(state.recent_skill_updates()[0], 7);
        state.skill_update_cycle = -1;
        state
            .apply(ServerPacket::UPDATE_STAT, &[0, 0, 0, 0, 12, 0])
            .unwrap();
        assert_eq!(state.skill_update_cycle(), 0);
        assert_eq!(state.recent_skill_updates()[31], 12);
    }
}
