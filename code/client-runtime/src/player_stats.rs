use openrs_login::player_stats::{Limits, PlayerStats, SKILL_UPDATE_RING_SIZE, SkillState, Update};
use openrs_protocol::registry::ServerPacket;
use serde::Serialize;

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldPlayerStatsReport {
    pub skill_count: usize,
    pub updates: usize,
    pub stat_updates: usize,
    pub run_energy_updates: usize,
    pub player_weight_updates: usize,
    pub recent_skill_updates: [u8; SKILL_UPDATE_RING_SIZE],
    pub skill_update_cycle: i32,
    pub run_energy: u8,
    pub player_weight: i16,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldSkillReport {
    pub experience: i32,
    pub visible_level: u8,
    pub base_level: u8,
}

impl From<SkillState> for WorldSkillReport {
    fn from(state: SkillState) -> Self {
        Self {
            experience: state.experience,
            visible_level: state.visible_level,
            base_level: state.base_level,
        }
    }
}

pub(crate) struct LivePlayerStats {
    bank: PlayerStats,
    stat_updates: usize,
    run_energy_updates: usize,
    player_weight_updates: usize,
}

impl LivePlayerStats {
    pub(crate) fn new(limits: Limits) -> Result<Self, &'static str> {
        Ok(Self {
            bank: PlayerStats::new(limits).map_err(|_| "invalid world player stats limits")?,
            stat_updates: 0,
            run_energy_updates: 0,
            player_weight_updates: 0,
        })
    }

    pub(crate) fn report(&self) -> WorldPlayerStatsReport {
        WorldPlayerStatsReport {
            skill_count: self.bank.limits().skill_count,
            updates: self.stat_updates + self.run_energy_updates + self.player_weight_updates,
            stat_updates: self.stat_updates,
            run_energy_updates: self.run_energy_updates,
            player_weight_updates: self.player_weight_updates,
            recent_skill_updates: *self.bank.recent_skill_updates(),
            skill_update_cycle: self.bank.skill_update_cycle(),
            run_energy: self.bank.run_energy(),
            player_weight: self.bank.player_weight(),
        }
    }

    pub(crate) fn sync(&self, state: &mut openrs_cs2::PlayerState) {
        crate::vars::player::sync_player_stats(state, &self.bank);
    }

    pub(crate) fn skill(&self, skill_id: u8) -> Result<WorldSkillReport, &'static str> {
        self.bank
            .skill(skill_id)
            .copied()
            .map(WorldSkillReport::from)
            .ok_or("invalid world skill id")
    }

    pub(crate) fn apply(
        &mut self,
        opcode: ServerPacket,
        payload: &[u8],
    ) -> Result<Update, &'static str> {
        let update = self
            .bank
            .apply(opcode, payload)
            .map_err(|_| "invalid world player stats packet")?;
        match update {
            Update::Stat { .. } => self.stat_updates += 1,
            Update::RunEnergy(_) => self.run_energy_updates += 1,
            Update::PlayerWeight(_) => self.player_weight_updates += 1,
        }
        Ok(update)
    }
}
