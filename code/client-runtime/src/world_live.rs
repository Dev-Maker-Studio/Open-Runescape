use crate::world_draw::{DrawBuilder, HeightMap, HintSet, WorldDraw};
use crate::world_resources::ResourceStore;
use crate::world_scene::{SceneEffect, WorldScene};
use openrs_login::world_packets::scene::{KnownPlayer, SceneSession};
use openrs_login::world_state::MapLayout;
use openrs_protocol::registry::ServerPacket;
use serde::Serialize;

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldSceneReport {
    pub tick: u32,
    pub cycle: u64,
    pub locs: usize,
    pub ground_items: usize,
    pub players: usize,
    pub npcs: usize,
    pub spots: usize,
    pub projectiles: usize,
    pub deferred: usize,
    pub errors: usize,
    pub pending: usize,
    pub version: u64,
}

pub struct LiveScene {
    pub(crate) scene: WorldScene,
    store: ResourceStore,
    builder: DrawBuilder,
    heights: HeightMap,
    draw: Option<WorldDraw>,
    version: u64,
    hints: HintSet,
}

impl LiveScene {
    pub fn new(scene: WorldScene, store: ResourceStore) -> Self {
        Self {
            scene,
            builder: DrawBuilder::new(store.clone()),
            store,
            heights: HeightMap::default(),
            draw: None,
            version: 0,
            hints: HintSet::default(),
        }
    }

    pub fn enter(
        own_index: u16,
        map: &MapLayout,
        payload: &[u8],
        store: ResourceStore,
    ) -> Result<Self, &'static str> {
        let scene = WorldScene::from_initial(own_index, map, payload)
            .map_err(|_| "invalid initial scene state")?;
        Ok(Self::new(scene, store))
    }

    pub fn apply(&mut self, packet: ServerPacket, payload: &[u8]) -> Vec<SceneEffect> {
        let effects = self.scene.apply(packet, payload, &self.store);
        self.version += 1;
        self.draw = None;
        effects
    }

    pub fn retry(&mut self) -> Vec<SceneEffect> {
        let effects = self.scene.retry(&self.store);
        self.version += 1;
        self.draw = None;
        effects
    }

    pub fn varbit_definition(&self, id: i32) -> crate::world_resources::Lookup {
        self.store.varbit_type(id)
    }

    pub fn heights(&self) -> &HeightMap {
        &self.heights
    }

    pub fn set_heights(&mut self, heights: HeightMap) {
        self.heights = heights;
        self.version += 1;
        self.draw = None;
    }

    pub fn set_hints(&mut self, hints: HintSet) {
        if self.hints == hints {
            return;
        }
        self.hints = hints.clone();
        self.builder.set_hints(hints);
        self.invalidate();
    }

    pub fn scene_session(&mut self, template: SceneSession) -> SceneSession {
        let appearances: Vec<(u16, Vec<u8>)> = self
            .scene
            .player_states
            .iter()
            .filter_map(|(index, state)| Some((*index, state.appearance.clone()?)))
            .collect();
        let mut session = template;
        for (index, bytes) in appearances {
            let Some(identity) = self.builder.player_identity(&bytes) else {
                continue;
            };
            session.players.insert(
                index,
                KnownPlayer {
                    display_name: Some(identity.username.clone()),
                    username: identity.username,
                    has_appearance: true,
                    prefix_title: identity.prefix_title,
                    postfix_title: identity.postfix_title,
                },
            );
        }
        session
    }

    pub fn invalidate(&mut self) {
        self.version += 1;
        self.draw = None;
    }

    pub fn advance_cycles(&mut self, cycles: u32) {
        self.scene.advance_cycles(cycles);
        self.invalidate();
    }

    pub fn draw(&mut self, varp: &dyn Fn(i32) -> i32) -> &WorldDraw {
        if self.draw.is_none() {
            let built = self.builder.build(&self.scene, &self.heights, varp);
            self.draw = Some(built);
        }
        self.draw.as_ref().expect("draw built above")
    }

    pub fn draw_ref(&self) -> Option<&WorldDraw> {
        self.draw.as_ref()
    }

    pub fn own_appearance_bytes(&self) -> Option<&[u8]> {
        self.scene
            .player_states
            .get(&self.scene.own_index())
            .and_then(|state| state.appearance.as_deref())
    }

    pub fn builder_identity(&mut self, bytes: &[u8]) -> Option<String> {
        self.builder.player_identity(bytes).map(|identity| identity.username)
    }

    pub fn local_appearance(&mut self, bytes: &[u8]) -> Option<openrs_cs2::PlayerAppearance> {
        self.builder.local_appearance(bytes)
    }

    pub fn report(&self) -> WorldSceneReport {
        WorldSceneReport {
            tick: self.scene.tick(),
            cycle: self.scene.cycle(),
            locs: self.scene.locs().len(),
            ground_items: self.scene.ground_items().values().map(Vec::len).sum(),
            players: self.scene.players.players.iter().flatten().count(),
            npcs: self.scene.npcs.npcs.len(),
            spots: self.scene.spots().len(),
            projectiles: self.scene.projectiles().len(),
            deferred: self.scene.deferred_count(),
            errors: self.scene.error_count(),
            pending: self.store.needs().len(),
            version: self.version,
        }
    }
}
