use super::WorldVarHost;
use openrs_cs2::{HostError, NpcState, PlayerState, SKILL_COUNT};
use openrs_login::player_stats::PlayerStats;

#[derive(Debug, Clone, Copy, Default)]
pub enum CurrentNpc<'a> {
    #[default]
    None,
    Player,
    Npc(&'a NpcState),
}

#[derive(Clone, Copy, Default)]
pub(super) struct PlayerBinding<'a> {
    player: Option<&'a PlayerState>,
    npc: Option<CurrentNpc<'a>>,
}

impl<'a> PlayerBinding<'a> {
    pub(super) fn player_state(&self) -> Result<&'a PlayerState, HostError> {
        self.player.ok_or(HostError::Unavailable)
    }

    pub(super) fn npc_state(&self) -> Result<&'a NpcState, HostError> {
        match self.npc {
            Some(CurrentNpc::Npc(npc)) => Ok(npc),
            Some(CurrentNpc::Player) => Err(HostError::EntityType),
            Some(CurrentNpc::None) => Err(HostError::NoEntity),
            Option::None => Err(HostError::Unavailable),
        }
    }
}

impl<'a> WorldVarHost<'a> {
    pub fn with_player_state(mut self, player: &'a PlayerState) -> Self {
        self.player.player = Some(player);
        self
    }

    pub fn with_current_npc(mut self, npc: CurrentNpc<'a>) -> Self {
        self.player.npc = Some(npc);
        self
    }
}

pub fn sync_player_stats(state: &mut PlayerState, stats: &PlayerStats) {
    for (index, skill) in stats.skills().iter().enumerate().take(SKILL_COUNT) {
        state.set_skill(
            index,
            skill.experience,
            i32::from(skill.visible_level),
            i32::from(skill.base_level),
        );
    }
    state.run_energy = i32::from(stats.run_energy());
    state.run_weight = i32::from(stats.player_weight());
}
