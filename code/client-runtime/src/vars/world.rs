use super::WorldVarHost;
use openrs_cs2::{AppearanceState, HostError, WorldState};

#[derive(Default)]
pub(super) struct WorldBinding<'a> {
    world: Option<&'a mut WorldState>,
    appearance: Option<&'a mut AppearanceState>,
}

impl<'a> WorldBinding<'a> {
    pub(super) fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        self.world.as_deref_mut().ok_or(HostError::Unavailable)
    }

    pub(super) fn appearance_state_mut(&mut self) -> Result<&mut AppearanceState, HostError> {
        self.appearance.as_deref_mut().ok_or(HostError::Unavailable)
    }
}

impl<'a> WorldVarHost<'a> {
    pub fn with_world_state(mut self, world: &'a mut WorldState) -> Self {
        self.world.world = Some(world);
        self
    }

    pub fn with_appearance_state(mut self, appearance: &'a mut AppearanceState) -> Self {
        self.world.appearance = Some(appearance);
        self
    }
}
