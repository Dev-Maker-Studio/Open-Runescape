use crate::inventory::{
    LiveInventory, WorldContainerReport, WorldInventoryReport, WorldItemReport,
};
use crate::lobby::{
    ConfigIdentity, Lifetime, LobbyPhase, MAX_CONFIG_BYTES, MAX_EVENTS, MAX_FEED_BYTES,
    MAX_INCOMING_BYTES, MAX_OPERATIONS, SecretArray,
};
use crate::player_stats::{LivePlayerStats, WorldPlayerStatsReport, WorldSkillReport};
use crate::vars::{VariableEffect, WorldVariables};
use crate::world_draw::{HeightMap, WorldDraw};
use crate::world_live::{LiveScene, WorldSceneReport};
use crate::world_resources::{ArchiveKey, ResourceStore};
use crate::world_scene::{SceneEffect, WorldScene};
use openrs_login::item_containers::{
    ContainerKey, ItemContainers, Limits as InventoryLimits, UpdateKind,
};
use openrs_login::lobby_stream::KEEPALIVE_IDLE_TICKS;
use openrs_login::player_stats::{Limits as PlayerStatsLimits, Update as PlayerStatsUpdate};
use openrs_login::response::LoginResponse;
use openrs_login::session::LoginAttempt;
use openrs_login::world_entry::WorldEntryDecoder;
use openrs_login::world_state::{WorldState, decode_fresh_world};
use openrs_login::{LobbyIdentity, MachineInformation, SensitiveString, WorldRequest};
use openrs_protocol::isaac::Isaac;
use openrs_protocol::registry::{ClientPacket, RegistryDecoder, ServerPacket};
use serde::{Deserialize, Deserializer, Serialize};
use zeroize::{Zeroize, Zeroizing};

#[cfg(target_arch = "wasm32")]
pub(crate) type LocRegions = (Vec<u16>, Zeroizing<Vec<[i32; 4]>>);

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct MachineConfig {
    operating_system: u8,
    x64_os: bool,
    os_vendor: u8,
    java_vendor: u8,
    java_build: u8,
    java_sub_build: u8,
    java_update: u8,
    unknown_flag: bool,
    max_memory: u16,
    processors: u8,
    ram: u32,
    cpu_clock: u16,
    gpu_model: Zeroizing<String>,
    gpu_driver_version: Zeroizing<String>,
    directx_version: Zeroizing<String>,
    directx_letter: Zeroizing<String>,
    dx_driver_month: u8,
    dx_driver_year: u16,
    cpu_type: Zeroizing<String>,
    cpu_data: Zeroizing<String>,
    cpu_cores: u8,
    cpu_family_id: u8,
    cpu_feature_flags: SecretArray<u32, 3>,
    cpu_model_info: u32,
}

impl MachineConfig {
    fn borrowed(&self) -> MachineInformation<'_> {
        MachineInformation {
            operating_system: self.operating_system,
            x64_os: self.x64_os,
            os_vendor: self.os_vendor,
            java_vendor: self.java_vendor,
            java_build: self.java_build,
            java_sub_build: self.java_sub_build,
            java_update: self.java_update,
            unknown_flag: self.unknown_flag,
            max_memory: self.max_memory,
            processors: self.processors,
            ram: self.ram,
            cpu_clock: self.cpu_clock,
            gpu_model: &self.gpu_model,
            gpu_driver_version: &self.gpu_driver_version,
            directx_version: &self.directx_version,
            directx_letter: &self.directx_letter,
            dx_driver_month: self.dx_driver_month,
            dx_driver_year: self.dx_driver_year,
            cpu_type: &self.cpu_type,
            cpu_data: &self.cpu_data,
            cpu_cores: self.cpu_cores,
            cpu_family_id: self.cpu_family_id,
            cpu_feature_flags: &self.cpu_feature_flags.0,
            cpu_model_info: self.cpu_model_info,
        }
    }
}

fn optional_secret<'de, D: Deserializer<'de>>(
    deserializer: D,
) -> Result<Option<Zeroizing<String>>, D::Error> {
    Option::<Zeroizing<String>>::deserialize(deserializer)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct WorldConfig {
    identity: ConfigIdentity,
    seed: SecretArray<u32, 4>,
    social_nonce: SecretArray<u32, 2>,
    client_random_seed: SecretArray<u32, 2>,
    reconnecting: bool,
    display_mode: u8,
    screen_width: u16,
    screen_height: u16,
    anti_aliasing: u8,
    random24: SecretArray<u8, 24>,
    affiliate_p: Zeroizing<String>,
    affiliate: i32,
    preferences: SecretArray<u8, 35>,
    machine_info: MachineConfig,
    var_verify_id: i32,
    machine_uid: SecretArray<u32, 2>,
    login_token: Zeroizing<String>,
    #[serde(deserialize_with = "optional_secret")]
    supplementary_login_data: Option<Zeroizing<String>>,
    jagtheora_loaded: bool,
    is_applet: bool,
    has_signed_terms_agreement: bool,
    login_type: u8,
    session_token: Zeroizing<i32>,
    js5_auth_token: Zeroizing<String>,
    different_world: bool,
    world_id: i32,
    index_crcs: SecretArray<u32, 36>,
}

impl WorldConfig {
    pub fn from_json(json: &str) -> Result<Self, &'static str> {
        if json.len() > MAX_CONFIG_BYTES {
            return Err("world configuration byte limit");
        }
        serde_json::from_str(json).map_err(|_| "invalid world configuration")
    }

    pub(crate) fn set_lobby_default_world(&mut self, default_world_id: Option<u16>) {
        self.different_world = default_world_id.map_or(-1, i32::from) != self.world_id;
    }

    fn attempt(&self, token: &SensitiveString) -> Result<LoginAttempt, &'static str> {
        if self.reconnecting {
            return Err("world reconnect is unsupported");
        }
        let identity = match &self.identity {
            ConfigIdentity::Username { username } => LobbyIdentity::Username(username),
            ConfigIdentity::Token { token } => LobbyIdentity::Token(token.word()),
        };
        let machine = self.machine_info.borrowed();
        let strings = [
            match &self.identity {
                ConfigIdentity::Username { username } => username.len(),
                ConfigIdentity::Token { .. } => 0,
            },
            token.as_str().len(),
            self.affiliate_p.len(),
            self.login_token.len(),
            self.supplementary_login_data
                .as_ref()
                .map_or(0, |value| value.len()),
            self.js5_auth_token.len(),
            machine.gpu_model.len(),
            machine.gpu_driver_version.len(),
            machine.directx_version.len(),
            machine.directx_letter.len(),
            machine.cpu_type.len(),
            machine.cpu_data.len(),
        ];
        if strings
            .into_iter()
            .try_fold(0usize, usize::checked_add)
            .is_none_or(|n| n > MAX_CONFIG_BYTES)
        {
            return Err("world configuration byte limit");
        }
        LoginAttempt::world(&WorldRequest {
            identity,
            lobby_auth_token: token.as_str(),
            seed: &self.seed.0,
            social_nonce: self.social_nonce.word(),
            client_random_seed: self.client_random_seed.word(),
            reconnecting: self.reconnecting,
            display_mode: self.display_mode,
            screen_width: self.screen_width,
            screen_height: self.screen_height,
            anti_aliasing: self.anti_aliasing,
            random24: &self.random24.0,
            affiliate_p: &self.affiliate_p,
            affiliate: self.affiliate,
            preferences: &self.preferences.0,
            machine_info: &machine,
            var_verify_id: self.var_verify_id,
            machine_uid: self.machine_uid.word(),
            login_token: &self.login_token,
            supplementary_login_data: self.supplementary_login_data.as_deref().map(String::as_str),
            jagtheora_loaded: self.jagtheora_loaded,
            is_applet: self.is_applet,
            has_signed_terms_agreement: self.has_signed_terms_agreement,
            login_type: self.login_type,
            session_token: *self.session_token,
            js5_auth_token: &self.js5_auth_token,
            different_world: self.different_world,
            world_id: self.world_id,
            index_crcs: &self.index_crcs.0,
        })
        .map_err(|_| "invalid world request configuration")
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum WorldEvent {
    LoginAccepted,
    ConfirmationRequired,
    TimeSync,
    ReconnectRequired,
    RetryRequested,
    Queued,
    Rejected {
        #[serde(rename = "responseCode")]
        response_code: u8,
    },
    WorldReady,
    VariableReceived {
        opcode: u16,
        name: &'static str,
        id: u16,
        value: i32,
    },
    VariablePublished {
        id: u16,
        previous: i32,
        value: i32,
    },
    VariablesCleared {
        opcode: u16,
        name: &'static str,
    },
    InventoryUpdated {
        opcode: u16,
        name: &'static str,
        id: u16,
        #[serde(rename = "negativeKey")]
        negative_key: bool,
        update: &'static str,
        #[serde(rename = "recordCount")]
        record_count: usize,
        #[serde(rename = "slotCount")]
        slot_count: usize,
        #[serde(rename = "containerPresent")]
        container_present: bool,
    },
    StatUpdated {
        opcode: u16,
        name: &'static str,
        #[serde(rename = "skillId")]
        skill_id: u8,
        experience: i32,
        #[serde(rename = "visibleLevel")]
        visible_level: u8,
        #[serde(rename = "baseLevel")]
        base_level: u8,
    },
    RunEnergyUpdated {
        opcode: u16,
        name: &'static str,
        #[serde(rename = "runEnergy")]
        run_energy: u8,
    },
    PlayerWeightUpdated {
        opcode: u16,
        name: &'static str,
        #[serde(rename = "playerWeight")]
        player_weight: i16,
    },
    SceneUpdated {
        opcode: u16,
        name: &'static str,
        applied: usize,
        deferred: usize,
        dropped: usize,
    },
    OverlayUpdated {
        opcode: u16,
        name: &'static str,
        action: &'static str,
        index: Option<u32>,
    },
    OpaquePacket {
        opcode: u16,
        name: &'static str,
        #[serde(rename = "payloadBytes")]
        payload_bytes: usize,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldFeed {
    pub consumed: usize,
    pub event: Option<WorldEvent>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldTick {
    pub events: Vec<WorldEvent>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldVariableReport {
    pub count: usize,
    pub scheduled: usize,
    pub server_assignments: usize,
    pub published: usize,
    pub clears: usize,
    pub non_zero: usize,
}

pub(crate) struct Cs2Parts<'a> {
    pub(crate) variables: &'a mut WorldVariables,
    pub(crate) containers: Option<&'a ItemContainers>,
    pub(crate) now_ms: u64,
    pub(crate) environment: &'a mut crate::environment::Environment,
    pub(crate) lobby: &'a openrs_cs2::LobbyState,
    pub(crate) appearance: &'a mut openrs_cs2::AppearanceState,
    pub(crate) outbox: &'a mut openrs_cs2::Outbox,
    pub(crate) entity: &'a mut openrs_cs2::EntityState,
    pub(crate) chat: &'a mut ChatIdentity,
    pub(crate) chat_state: &'a mut openrs_cs2::ChatState,
}

#[derive(Debug, Clone, Default)]
pub struct ChatIdentityParams {
    pub player_rights: i32,
    pub quickchat_only: bool,
    pub userdetail_quickchat: bool,
    pub verified_email_address: bool,
    pub username: Option<String>,
    pub display_name: Option<String>,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
}

#[derive(Default)]
pub(crate) struct ChatIdentity {
    pub(crate) set: bool,
    pub(crate) player_rights: i32,
    pub(crate) client: openrs_cs2::ClientState,
    pub(crate) player: openrs_cs2::MyPlayer,
    pub(crate) huffman: Option<openrs_protocol::client_packets::Huffman>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OverheadLabel {
    pub index: u16,
    pub text: String,
    pub colour: i32,
    pub effect: i32,
    pub progress: i32,
    pub x: i32,
    pub y: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ArrowIcon {
    pub slot: u8,
    pub sprite: u8,
    pub x: i32,
    pub y: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HintScreen {
    pub overhead: Vec<OverheadLabel>,
    pub arrows: Vec<ArrowIcon>,
}

fn write_varbit(
    variables: &mut LiveVariables,
    scene: &LiveScene,
    id: i32,
    value: i32,
    now_ms: u64,
) -> bool {
    let definition = match scene.varbit_definition(id) {
        crate::world_resources::Lookup::Unknown => return false,
        crate::world_resources::Lookup::Absent => return true,
        crate::world_resources::Lookup::Present(bytes) => {
            match crate::vars::varbit::decode_definition(&bytes) {
                Ok(definition) => definition,
                Err(_) => return true,
            }
        }
    };
    let Ok(previous) = variables.bank.pending(definition.varp) else {
        return true;
    };
    let value = crate::vars::varbit::write_bits(&definition, previous, value);
    let _ = variables.bank.apply(
        crate::vars::VariablePacket::VarpLarge {
            id: definition.varp,
            value,
        },
        now_ms,
    );
    true
}

struct LiveVariables {
    bank: WorldVariables,
    server_assignments: usize,
    published: usize,
    clears: usize,
    waiting_varbits: Vec<(i32, i32)>,
}

impl LiveVariables {
    fn new(count: usize) -> Result<Self, &'static str> {
        Ok(Self {
            bank: WorldVariables::new(count).map_err(|_| "invalid world variable count")?,
            server_assignments: 0,
            published: 0,
            clears: 0,
            waiting_varbits: Vec::new(),
        })
    }

    fn report(&self) -> WorldVariableReport {
        WorldVariableReport {
            count: self.bank.count(),
            scheduled: self.bank.scheduled_count(),
            server_assignments: self.server_assignments,
            published: self.published,
            clears: self.clears,
            non_zero: self.bank.non_zero_count(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldTileReport {
    pub plane: u8,
    pub x: u16,
    pub y: u16,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldBaseReport {
    pub x: i32,
    pub y: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldInitialReport {
    pub player_index: u16,
    pub tile: WorldTileReport,
    pub base: WorldBaseReport,
    pub map_size: u16,
    pub map_kind: &'static str,
    pub region_ids: Vec<u16>,
    pub chunk_count: usize,
}

impl WorldInitialReport {
    pub fn scene_tile(&self) -> Option<(u8, i32, i32)> {
        let size = i32::from(self.map_size);
        let x = i32::from(self.tile.x) - self.base.x;
        let y = i32::from(self.tile.y) - self.base.y;
        if (0..size).contains(&x) && (0..size).contains(&y) {
            Some((self.tile.plane, x, y))
        } else {
            None
        }
    }
}

impl From<&WorldState> for WorldInitialReport {
    fn from(state: &WorldState) -> Self {
        Self {
            player_index: state.players.player_index,
            tile: WorldTileReport {
                plane: state.players.tile.plane,
                x: state.players.tile.x,
                y: state.players.tile.y,
            },
            base: WorldBaseReport {
                x: state.map.base.x,
                y: state.map.base.y,
            },
            map_size: state.map.map_size.size(),
            map_kind: if state.map.chunks.is_some() {
                "dynamic"
            } else {
                "standard"
            },
            region_ids: state.map.region_ids.clone(),
            chunk_count: state
                .map
                .chunks
                .as_ref()
                .map_or(0, |chunks| chunks.descriptors.iter().flatten().count()),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldReport {
    pub phase: &'static str,
    pub accepted: bool,
    pub world_ready: bool,
    pub initial_world: Option<WorldInitialReport>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub variables: Option<WorldVariableReport>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inventory: Option<WorldInventoryReport>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub player_stats: Option<WorldPlayerStatsReport>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scene: Option<WorldSceneReport>,
}

struct StoredWorldState(WorldState);

impl Drop for StoredWorldState {
    fn drop(&mut self) {
        self.0.map.xtea_keys.zeroize();
    }
}

pub struct WorldSession {
    attempt: Option<LoginAttempt>,
    entry: Option<WorldEntryDecoder>,
    packets: Option<RegistryDecoder<ServerPacket>>,
    outgoing: Option<Isaac>,
    pending_ping: Option<Zeroizing<u8>>,
    queued: Vec<u8>,
    flush_pending: bool,
    idle_ticks: u8,
    player_index: Option<u16>,
    initial: Option<StoredWorldState>,
    variables: Option<LiveVariables>,
    inventory: Option<LiveInventory>,
    player_stats: Option<LivePlayerStats>,
    scene_enabled: bool,
    scene: Option<LiveScene>,
    camera_commands: Vec<i32>,
    interfaces: crate::ui::OpenInterfaces,
    audio_events: Vec<i32>,
    scene_store: ResourceStore,
    terminal: Option<&'static str>,
    incoming_bytes: usize,
    events: usize,
    operations: usize,
    lifetime: Lifetime,
    environment: crate::environment::Environment,
    lobby: openrs_cs2::LobbyState,
    appearance: openrs_cs2::AppearanceState,
    appearance_source: Option<Vec<u8>>,
    outbox: openrs_cs2::Outbox,
    entity: openrs_cs2::EntityState,
    chat: ChatIdentity,
    overlay: crate::world_overlay::SceneOverlay,
}

impl WorldSession {
    pub(crate) fn from_lobby(
        config: WorldConfig,
        token: &SensitiveString,
    ) -> Result<Self, &'static str> {
        Self::from_lobby_mode(config, token, None)
    }

    pub(crate) fn from_lobby_with_vars(
        config: WorldConfig,
        token: &SensitiveString,
        varp_count: usize,
    ) -> Result<Self, &'static str> {
        Self::from_lobby_mode(config, token, Some(LiveVariables::new(varp_count)?))
    }

    fn from_lobby_mode(
        config: WorldConfig,
        token: &SensitiveString,
        variables: Option<LiveVariables>,
    ) -> Result<Self, &'static str> {
        let attempt = config.attempt(token)?;
        Ok(Self {
            attempt: Some(attempt),
            entry: None,
            packets: None,
            outgoing: None,
            pending_ping: None,
            queued: Vec::new(),
            flush_pending: false,
            idle_ticks: 0,
            player_index: None,
            initial: None,
            variables,
            inventory: None,
            player_stats: None,
            scene_enabled: false,
            scene: None,
            camera_commands: Vec::new(),
            interfaces: crate::ui::OpenInterfaces::default(),
            audio_events: Vec::new(),
            scene_store: ResourceStore::new(),
            terminal: None,
            incoming_bytes: 0,
            events: 0,
            operations: 0,
            lifetime: Lifetime::new().map_err(|_| "world monotonic clock unavailable")?,
            environment: crate::environment::Environment::native(),
            lobby: openrs_cs2::LobbyState::default(),
            appearance: openrs_cs2::AppearanceState::default(),
            appearance_source: None,
            outbox: openrs_cs2::Outbox::default(),
            entity: openrs_cs2::EntityState::default(),
            chat: ChatIdentity::default(),
            overlay: crate::world_overlay::SceneOverlay::default(),
        })
    }

    pub fn phase(&mut self) -> &'static str {
        self.expire();
        self.current_phase()
    }

    pub fn report(&mut self) -> WorldReport {
        self.expire();
        WorldReport {
            phase: self.current_phase(),
            accepted: self.entry.is_some() || self.packets.is_some(),
            world_ready: self.initial.is_some(),
            initial_world: self
                .initial
                .as_ref()
                .map(|state| WorldInitialReport::from(&state.0)),
            variables: self.variables.as_ref().map(LiveVariables::report),
            inventory: self.inventory.as_ref().map(LiveInventory::report),
            player_stats: self.player_stats.as_ref().map(LivePlayerStats::report),
            scene: self.scene.as_ref().map(LiveScene::report),
        }
    }

    #[cfg(target_arch = "wasm32")]
    pub(crate) fn loc_regions(&self) -> Option<LocRegions> {
        let state = &self.initial.as_ref()?.0;
        if state.map.chunks.is_some() {
            return None;
        }
        Some((
            state.map.region_ids.clone(),
            Zeroizing::new(state.map.xtea_keys.clone()),
        ))
    }

    pub fn enable_inventory(&mut self, limits: InventoryLimits) -> Result<(), &'static str> {
        self.guard()?;
        if self.current_phase() != "sendHandshake" || self.inventory.is_some() {
            return Err("world inventory must be enabled before handshake");
        }
        self.inventory = Some(LiveInventory::new(limits)?);
        Ok(())
    }

    pub fn enable_player_stats(&mut self, limits: PlayerStatsLimits) -> Result<(), &'static str> {
        self.guard()?;
        if self.current_phase() != "sendHandshake" || self.player_stats.is_some() {
            return Err("world player stats must be enabled before handshake");
        }
        self.player_stats = Some(LivePlayerStats::new(limits)?);
        Ok(())
    }

    pub fn enable_scene(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if self.current_phase() != "sendHandshake" || self.scene_enabled {
            return Err("world scene must be enabled before handshake");
        }
        self.scene_enabled = true;
        Ok(())
    }

    pub fn scene_store(&self) -> &ResourceStore {
        &self.scene_store
    }

    pub fn scene_needs(&self) -> Vec<ArchiveKey> {
        self.scene_store.needs()
    }

    pub fn scene_provided(&mut self) -> Result<Vec<SceneEffect>, &'static str> {
        self.guard()?;
        let Some(scene) = self.scene.as_mut() else {
            return Ok(Vec::new());
        };
        scene.invalidate();
        let effects = scene.retry();
        let now_ms = self.now_ms().unwrap_or(0);
        self.drain_varbits(now_ms);
        Ok(effects)
    }

    fn drain_varbits(&mut self, now_ms: u64) {
        let (Some(variables), Some(scene)) = (self.variables.as_mut(), self.scene.as_ref()) else {
            return;
        };
        let waiting = std::mem::take(&mut variables.waiting_varbits);
        for (id, value) in waiting {
            if !write_varbit(variables, scene, id, value, now_ms) {
                variables.waiting_varbits.push((id, value));
            }
        }
    }

    pub fn set_scene_heights(&mut self, heights: HeightMap) -> Result<(), &'static str> {
        self.guard()?;
        self.scene
            .as_mut()
            .ok_or("world scene state unavailable")?
            .set_heights(heights);
        Ok(())
    }

    pub fn scene(&self) -> Option<&WorldScene> {
        self.scene.as_ref().map(|live| &live.scene)
    }

    pub fn scene_draw(&mut self) -> Result<&WorldDraw, &'static str> {
        self.guard()?;
        let variables = self.variables.as_ref().map(|live| &live.bank);
        let scene = self.scene.as_mut().ok_or("world scene state unavailable")?;
        let varp = |id: i32| {
            variables
                .zip(u16::try_from(id).ok())
                .and_then(|(bank, id)| bank.active(id).ok())
                .unwrap_or(0)
        };
        Ok(scene.draw(&varp))
    }

    pub fn skill(&mut self, skill_id: u8) -> Result<WorldSkillReport, &'static str> {
        self.guard()?;
        self.player_stats
            .as_ref()
            .ok_or("world player stats state unavailable")?
            .skill(skill_id)
    }

    pub fn sync_player_state(
        &self,
        state: &mut openrs_cs2::PlayerState,
    ) -> Result<(), &'static str> {
        self.player_stats
            .as_ref()
            .ok_or("world player stats state unavailable")?
            .sync(state);
        if let Some(index) = self.player_index {
            state.my_player_index = i32::from(index);
        }
        Ok(())
    }

    pub fn player_stats(&mut self) -> Result<WorldPlayerStatsReport, &'static str> {
        self.guard()?;
        Ok(self
            .player_stats
            .as_ref()
            .ok_or("world player stats state unavailable")?
            .report())
    }

    pub fn container(
        &mut self,
        key: ContainerKey,
    ) -> Result<Option<WorldContainerReport>, &'static str> {
        self.guard()?;
        Ok(self
            .inventory
            .as_ref()
            .ok_or("world inventory state unavailable")?
            .container(key))
    }

    pub fn item(
        &mut self,
        key: ContainerKey,
        slot: usize,
    ) -> Result<Option<WorldItemReport>, &'static str> {
        self.guard()?;
        if slot >= openrs_login::item_containers::MAX_CONTAINER_SLOTS {
            return Err("invalid world inventory slot");
        }
        Ok(self
            .inventory
            .as_ref()
            .ok_or("world inventory state unavailable")?
            .item(key, slot))
    }

    pub fn varp(&mut self, id: u16) -> Result<i32, &'static str> {
        self.guard()?;
        self.variables
            .as_ref()
            .ok_or("world variable state unavailable")?
            .bank
            .active(id)
            .map_err(|_| "world variable id outside definition span")
    }

    pub(crate) fn cs2_parts(&mut self) -> Result<Cs2Parts<'_>, &'static str> {
        self.guard()?;
        let now_ms = self.now_ms()?;
        self.refresh_appearance();
        self.refresh_entities();
        let variables = self
            .variables
            .as_mut()
            .ok_or("world variable state unavailable")?;
        Ok(Cs2Parts {
            variables: &mut variables.bank,
            containers: self.inventory.as_ref().map(|inventory| &inventory.bank),
            now_ms,
            environment: &mut self.environment,
            lobby: &self.lobby,
            appearance: &mut self.appearance,
            outbox: &mut self.outbox,
            entity: &mut self.entity,
            chat: &mut self.chat,
            chat_state: self.overlay.chat_state(),
        })
    }

    fn apply_overlay(
        &mut self,
        opcode: ServerPacket,
        payload: &[u8],
    ) -> crate::world_overlay::OverlayEffect {
        let Some(live) = self.scene.as_mut() else {
            let session = self.overlay.session_template((0, 0));
            return self.overlay.apply(opcode, payload, &session, 0);
        };
        let plane = live.scene.own_position().map_or(0, |own| own.2);
        let template = self.overlay.session_template(live.scene.base());
        let session = live.scene_session(template);
        let effect = self.overlay.apply(opcode, payload, &session, plane);
        live.set_hints(self.overlay.hint_set());
        effect
    }

    fn refresh_entities(&mut self) {
        let Some(live) = self.scene.as_mut() else {
            self.entity.nearby_players.clear();
            self.entity.has_my_player = false;
            self.entity.my_coord = None;
            return;
        };
        let (base_x, base_y) = live.scene.base();
        let own_index = live.scene.own_index();
        let own = live.scene.own_position();
        let mut seen = Vec::new();
        for (index, record) in live.scene.players.players.iter().enumerate() {
            if let Some(record) = record {
                seen.push((index as u16, record.x - base_x, record.y - base_y));
            }
        }
        let mut players = Vec::with_capacity(seen.len());
        for (index, x, y) in seen {
            let name = live
                .scene
                .player_states
                .get(&index)
                .and_then(|state| state.appearance.clone())
                .and_then(|bytes| live.builder_identity(&bytes));
            players.push(openrs_cs2::NearbyPlayer {
                index: i32::from(index),
                present: true,
                is_self: index == own_index,
                display_name: name,
                movement_x: x,
                movement_y: y,
                size: 1,
            });
        }
        self.entity.nearby_players = players;
        self.entity.has_my_player = own.is_some();
        self.entity.my_coord = own.map(|(x, y, plane)| openrs_cs2::MyCoord {
            plane: i32::from(plane),
            fine_x: (x - base_x) * 512 + 256,
            fine_z: (y - base_y) * 512 + 256,
            base_x,
            base_y,
        });
    }

    pub fn take_cs2_typed_messages(&mut self) -> Vec<openrs_cs2::TypedMessage> {
        std::mem::take(&mut self.outbox.typed_messages)
    }

    pub fn take_cs2_console_lines(&mut self) -> Vec<String> {
        std::mem::take(&mut self.outbox.console_lines)
    }

    pub fn take_cs2_console_clears(&mut self) -> Vec<String> {
        std::mem::take(&mut self.outbox.console_clears)
    }

    pub fn take_cs2_route_requests(&mut self) -> Vec<openrs_cs2::RouteRequest> {
        std::mem::take(&mut self.outbox.route_requests)
    }

    pub fn take_cs2_interface_closes(&mut self) -> u32 {
        std::mem::take(&mut self.outbox.interface_closes)
    }

    fn refresh_appearance(&mut self) {
        let Some(live) = self.scene.as_mut() else {
            return;
        };
        let Some(bytes) = live.own_appearance_bytes() else {
            return;
        };
        if self.appearance_source.as_deref() == Some(bytes) {
            return;
        }
        let bytes = bytes.to_vec();
        let decoded = live.local_appearance(&bytes);
        if let Some(decoded) = decoded {
            self.appearance.my_player = true;
            self.appearance.appearance = Some(decoded);
            self.appearance_source = Some(bytes);
        }
    }

    pub fn set_lobby_state(&mut self, lobby: openrs_cs2::LobbyState) {
        self.lobby = lobby;
    }

    pub fn take_camera_commands(&mut self) -> Vec<i32> {
        std::mem::take(&mut self.camera_commands)
    }

    pub fn open_interfaces(&self) -> &crate::ui::OpenInterfaces {
        &self.interfaces
    }

    pub fn take_audio_events(&mut self) -> Vec<i32> {
        let mut events = std::mem::take(&mut self.audio_events);
        if let Some(live) = self.scene.as_mut() {
            events.extend(live.scene.take_audio());
        }
        events
    }

    pub fn camera_scene_base(&self) -> Vec<i32> {
        self.scene.as_ref().map_or_else(Vec::new, |live| {
            let (x, y) = live.scene.base();
            vec![x, y]
        })
    }

    pub fn take_cs2_packets(&mut self) -> Vec<openrs_cs2::ClientPacket> {
        self.outbox.drain_packets()
    }

    pub fn set_chat_identity(&mut self, identity: ChatIdentityParams) {
        self.chat.set = true;
        self.chat.player_rights = identity.player_rights;
        self.chat.client.quickchat_only = identity.quickchat_only;
        self.chat.client.userdetail_quickchat = identity.userdetail_quickchat;
        self.chat.client.verified_email_address = identity.verified_email_address;
        self.chat.player = openrs_cs2::MyPlayer {
            username: identity.username,
            display_name: identity.display_name,
            prefix_title: identity.prefix_title,
            postfix_title: identity.postfix_title,
            ..openrs_cs2::MyPlayer::default()
        };
    }

    pub fn set_chat_huffman(&mut self, code_lengths: &[i8]) {
        self.chat.huffman = Some(openrs_protocol::client_packets::Huffman::new(code_lengths));
        self.overlay.set_huffman(code_lengths);
    }

    pub fn overlay(&self) -> &crate::world_overlay::SceneOverlay {
        &self.overlay
    }

    pub fn overlay_mut(&mut self) -> &mut crate::world_overlay::SceneOverlay {
        &mut self.overlay
    }

    pub fn advance_overlay(&mut self, cycles: u32) {
        let cycle = self.overlay.cycle() + u64::from(cycles);
        self.overlay.advance_cycles(cycles, cycle);
    }

    pub fn add_static_locs(&mut self, region_id: i32, data: &[u8]) -> Result<usize, &'static str> {
        self.guard()?;
        let live = self.scene.as_mut().ok_or("world scene state unavailable")?;
        let decoded = openrs_scene::locs::decode_region_locs(data)
            .map_err(|_| "invalid region loc placements")?;
        let (base, size) = (live.scene.base(), live.scene.map_size());
        let placed = openrs_scene::locs::place_in_scene(region_id, &decoded, base.0, base.1, size);
        let added = live.scene.add_static_locs(&placed);
        live.invalidate();
        Ok(added)
    }

    pub fn entity_hits(
        &mut self,
        view_projection: &[f32],
        mouse: (i32, i32),
        viewport: crate::model_hover::ProjectionViewport,
    ) -> Result<Vec<(u8, i32)>, &'static str> {
        let matrix: [f32; 16] = view_projection
            .try_into()
            .map_err(|_| "view projection needs sixteen values")?;
        let draw = self.scene_draw()?;
        Ok(crate::model_hover::entity_hits(draw, &matrix, &viewport, mouse))
    }

    pub fn hint_screen(
        &mut self,
        view_projection: &[f32],
        viewport: crate::model_hover::ProjectionViewport,
        offset: crate::hint_screen::HintOffset,
    ) -> Result<HintScreen, &'static str> {
        use crate::hint_screen::{
            effect_progress, hint_arrow_placement, overhead_text_color, tile_arrow_icon,
        };
        let matrix: [f32; 16] = view_projection
            .try_into()
            .map_err(|_| "view projection needs sixteen values")?;
        self.scene_draw()?;
        let live = self.scene.as_ref().ok_or("world scene state unavailable")?;
        let cycles = live.scene.cycle();
        let size = live.scene.map_size();
        let draw = live.draw_ref().ok_or("world draw unavailable")?;
        let mut overhead = Vec::new();
        for entry in self.overlay.overhead() {
            let Some(mesh) = draw.meshes.iter().find(|mesh| {
                mesh.kind == crate::world_draw::DrawKind::Player
                    && mesh.id == i32::from(entry.player_index)
            }) else {
                continue;
            };
            let Some(head) = mesh.positions.chunks_exact(3).map(|v| v[1]).min() else {
                continue;
            };
            let Some((x, y)) = hint_arrow_placement(
                &matrix,
                &viewport,
                (size, size),
                (mesh.origin.0, head, mesh.origin.2),
                offset,
            ) else {
                continue;
            };
            overhead.push(OverheadLabel {
                index: entry.player_index,
                text: entry.text.clone(),
                colour: overhead_text_color(
                    entry.colour,
                    cycles,
                    entry.ticks_left,
                    entry.ticks_visible,
                ),
                effect: entry.effect,
                progress: effect_progress(entry.ticks_left, entry.ticks_visible),
                x: x as i32,
                y: y as i32,
            });
        }
        let mut arrows = Vec::new();
        for slot in 0..crate::world_draw::ARROW_SLOTS {
            let Some(arrow) = self.overlay.arrow(slot) else {
                continue;
            };
            if arrow.target_type != 2 {
                continue;
            }
            let ground = live
                .heights()
                .sample(arrow.plane, arrow.x, arrow.y)
                .unwrap_or(0)
                - arrow.height * 2;
            let Some(position) = hint_arrow_placement(
                &matrix,
                &viewport,
                (size, size),
                (arrow.x, ground, arrow.y),
                offset,
            ) else {
                continue;
            };
            if let Some((x, y)) = tile_arrow_icon(position, cycles) {
                arrows.push(ArrowIcon {
                    slot: arrow.slot,
                    sprite: arrow.sprite,
                    x,
                    y,
                });
            }
        }
        Ok(HintScreen { overhead, arrows })
    }

    pub fn advance_scene(&mut self, cycles: u32) -> u64 {
        match self.scene.as_mut() {
            Some(live) if cycles > 0 => {
                live.advance_cycles(cycles);
                live.report().cycle
            }
            Some(live) => live.report().cycle,
            None => 0,
        }
    }

    pub fn take_cs2_chat_bodies(
        &mut self,
    ) -> Vec<Result<openrs_protocol::client_packets::ClientBody, String>> {
        let sends = self.outbox.drain_chat_sends();
        sends
            .iter()
            .map(|send| match self.chat.huffman.as_ref() {
                Some(huffman) => crate::chat_send::to_body(huffman, send),
                None => Err("chat huffman table unavailable".to_owned()),
            })
            .collect()
    }

    pub fn take_cs2_game_messages(&mut self) -> Vec<String> {
        self.outbox.drain_game_messages()
    }

    pub fn set_wall_clock(&mut self, epoch_ms: i64) -> Result<(), &'static str> {
        let elapsed = self.now_ms()?;
        self.environment.set_epoch_base(epoch_ms, elapsed);
        Ok(())
    }

    pub fn set_utc_offset(&mut self, offset_ms: i64) {
        self.environment.set_utc_offset(offset_ms);
    }

    pub fn tick(&mut self) -> Result<WorldTick, &'static str> {
        let now_ms = self.now_ms()?;
        self.tick_at(now_ms)
    }

    pub fn tick_at(&mut self, now_ms: u64) -> Result<WorldTick, &'static str> {
        self.guard()?;
        let Some(variables) = self.variables.as_mut() else {
            return Ok(WorldTick { events: Vec::new() });
        };
        let changes = match variables.bank.service(now_ms) {
            Ok(changes) => changes,
            Err(_) => return self.fail("invalid world variable service"),
        };
        variables.published += changes.len();
        if !changes.is_empty()
            && let Some(scene) = self.scene.as_mut()
        {
            scene.invalidate();
        }
        let events = changes
            .into_iter()
            .map(|change| WorldEvent::VariablePublished {
                id: change.id,
                previous: change.previous,
                value: change.value,
            })
            .collect::<Vec<_>>();
        self.record_events(events.len())?;
        Ok(WorldTick { events })
    }

    pub fn game_tick(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        let Some(outgoing) = self.outgoing.as_mut() else {
            return self.fail("invalid world keepalive phase");
        };
        self.idle_ticks = self.idle_ticks.saturating_add(1);
        if self.idle_ticks > KEEPALIVE_IDLE_TICKS
            && self.pending_ping.is_none()
            && !self.flush_pending
        {
            self.pending_ping = Some(Zeroizing::new(
                (ClientPacket::PING.wire() as u8).wrapping_add(outgoing.next_u32() as u8),
            ));
        }
        Ok(())
    }

    pub fn mark_flushed(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if self.outgoing.is_none() {
            return self.fail("invalid world flush phase");
        }
        if self.pending_ping.is_some() {
            return self.fail("world outgoing buffer must be drained");
        }
        if !self.flush_pending {
            return self.fail("world flush requires outgoing bytes");
        }
        self.idle_ticks = 0;
        self.flush_pending = false;
        Ok(())
    }

    pub fn send_client_bodies(
        &mut self,
        bodies: &[openrs_protocol::client_packets::ClientBody],
    ) -> Result<(), &'static str> {
        self.guard()?;
        let Some(outgoing) = self.outgoing.as_mut() else {
            return self.fail("client packets need an established world session");
        };
        for body in bodies {
            match openrs_protocol::client_packets::frame::frame_body(outgoing, body) {
                Ok(frame) => self.queued.extend_from_slice(&frame),
                Err(_) => return self.fail("client packet does not fit its frame"),
            }
        }
        Ok(())
    }

    pub fn take_outgoing(&mut self) -> Result<Zeroizing<Vec<u8>>, &'static str> {
        self.guard()?;
        if let Some(byte) = self.pending_ping.take() {
            self.flush_pending = true;
            return Ok(Zeroizing::new(vec![*byte]));
        }
        if !self.queued.is_empty() {
            self.flush_pending = true;
            return Ok(Zeroizing::new(std::mem::take(&mut self.queued)));
        }
        match self.attempt.as_mut() {
            Some(attempt) => match attempt.take_outgoing() {
                Ok(Some(bytes)) => Ok(Zeroizing::new(bytes.as_bytes().to_vec())),
                Ok(None) => Ok(Zeroizing::new(Vec::new())),
                Err(_) => self.fail("invalid world outgoing phase"),
            },
            None => Ok(Zeroizing::new(Vec::new())),
        }
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<WorldFeed, &'static str> {
        let now_ms = self.now_ms()?;
        self.feed_at(bytes, now_ms)
    }

    pub fn feed_at(&mut self, bytes: &[u8], now_ms: u64) -> Result<WorldFeed, &'static str> {
        self.guard()?;
        if bytes.len() > MAX_FEED_BYTES {
            return self.fail("world incoming chunk byte limit");
        }
        if bytes.len() > MAX_INCOMING_BYTES - self.incoming_bytes {
            return self.fail("world incoming total byte limit");
        }
        let feed = match self.feed_inner(bytes, now_ms) {
            Ok(feed) => feed,
            Err(error) => return self.fail(error),
        };
        self.incoming_bytes += feed.consumed;
        self.record_events(usize::from(feed.event.is_some()))?;
        Ok(feed)
    }

    fn feed_inner(&mut self, bytes: &[u8], now_ms: u64) -> Result<WorldFeed, &'static str> {
        if let Some(packets) = self.packets.as_mut() {
            let feed = packets.feed(bytes).map_err(|_| "invalid world packet")?;
            let event = if let Some(packet) = feed.item {
                let payload = Zeroizing::new(packet.payload);
                let opcode = packet.opcode;
                let name = opcode.metadata().name;
                let event = if let Some(variables) = self.variables.as_mut()
                    && matches!(
                        opcode,
                        ServerPacket::VARP_SMALL
                            | ServerPacket::VARP_LARGE
                            | ServerPacket::CLEAR_VARPS
                    ) {
                    match variables
                        .bank
                        .apply_payload(opcode, &payload, now_ms)
                        .map_err(|_| "invalid world variable packet")?
                    {
                        VariableEffect::Pending { id, value } => {
                            variables.server_assignments += 1;
                            WorldEvent::VariableReceived {
                                opcode: opcode.wire(),
                                name,
                                id,
                                value,
                            }
                        }
                        VariableEffect::Cleared => {
                            variables.clears += 1;
                            WorldEvent::VariablesCleared {
                                opcode: opcode.wire(),
                                name,
                            }
                        }
                    }
                } else if let Some(inventory) = self.inventory.as_mut()
                    && ItemContainers::supports(opcode)
                {
                    let update = inventory.apply(opcode, &payload)?;
                    WorldEvent::InventoryUpdated {
                        opcode: opcode.wire(),
                        name,
                        id: update.key.id,
                        negative_key: update.key.negative_key,
                        update: match update.kind {
                            UpdateKind::Full => "full",
                            UpdateKind::Partial => "partial",
                            UpdateKind::StopTransmit => "stopTransmit",
                        },
                        record_count: update.record_count,
                        slot_count: update.slot_count,
                        container_present: update.container_present,
                    }
                } else if let Some(stats) = self.player_stats.as_mut()
                    && matches!(
                        opcode,
                        ServerPacket::UPDATE_STAT
                            | ServerPacket::RUN_ENERGY
                            | ServerPacket::PLAYER_WEIGHT
                    )
                {
                    match stats.apply(opcode, &payload)? {
                        PlayerStatsUpdate::Stat { skill_id, state } => WorldEvent::StatUpdated {
                            opcode: opcode.wire(),
                            name,
                            skill_id,
                            experience: state.experience,
                            visible_level: state.visible_level,
                            base_level: state.base_level,
                        },
                        PlayerStatsUpdate::RunEnergy(run_energy) => WorldEvent::RunEnergyUpdated {
                            opcode: opcode.wire(),
                            name,
                            run_energy,
                        },
                        PlayerStatsUpdate::PlayerWeight(player_weight) => {
                            WorldEvent::PlayerWeightUpdated {
                                opcode: opcode.wire(),
                                name,
                                player_weight,
                            }
                        }
                    }
                } else if crate::world_overlay::SceneOverlay::handles(opcode) {
                    let effect = self.apply_overlay(opcode, &payload);
                    WorldEvent::OverlayUpdated {
                        opcode: opcode.wire(),
                        name,
                        action: effect.action,
                        index: effect.index,
                    }
                } else if let Some(scene) = self.scene.as_mut()
                    && WorldScene::handles(opcode)
                {
                    let effects = scene.apply(opcode, &payload);
                    if let Some(variables) = self.variables.as_mut() {
                        for effect in &effects {
                            if let SceneEffect::VarBit { id, value } = *effect
                                && !write_varbit(variables, scene, i32::from(id), value, now_ms)
                            {
                                variables.waiting_varbits.push((i32::from(id), value));
                            }
                        }
                    }
                    if matches!(
                        opcode,
                        ServerPacket::REGION | ServerPacket::DYNAMIC_MAP_REGION
                    ) {
                        self.overlay.map_rebuilt();
                    }
                    let deferred = effects
                        .iter()
                        .filter(|effect| matches!(effect, SceneEffect::Deferred { .. }))
                        .count();
                    let dropped = effects
                        .iter()
                        .filter(|effect| matches!(effect, SceneEffect::Dropped))
                        .count();
                    WorldEvent::SceneUpdated {
                        opcode: opcode.wire(),
                        name,
                        applied: effects.len() - deferred - dropped,
                        deferred,
                        dropped,
                    }
                } else {
                    if crate::camera_commands::is_camera_packet(opcode) {
                        let (base, size) = self
                            .scene
                            .as_ref()
                            .map_or(((0, 0), 104), |live| {
                                (live.scene.base(), live.scene.map_size())
                            });
                        if let Ok(Some(command)) =
                            crate::camera_commands::camera_command(opcode, &payload, base, size)
                        {
                            self.camera_commands.extend(command.to_ints());
                        }
                    }
                    if crate::audio_events::is_audio_packet(opcode) {
                        self.audio_events
                            .extend(crate::audio_events::misc_ints(opcode, &payload));
                    }
                    if let Ok(decoded) = openrs_login::world_packets::decode(opcode, &payload) {
                        self.interfaces.apply(&decoded);
                    }
                    WorldEvent::OpaquePacket {
                        opcode: opcode.wire(),
                        name,
                        payload_bytes: payload.len(),
                    }
                };
                Some(event)
            } else {
                None
            };
            return Ok(WorldFeed {
                consumed: feed.consumed,
                event,
            });
        }
        if let Some(entry) = self.entry.as_mut() {
            let feed = entry
                .feed(bytes)
                .map_err(|_| "invalid initial world packet")?;
            let event = if let Some(packet) = feed.item {
                let payload = Zeroizing::new(packet.payload);
                let player_index = self.player_index.ok_or("world player index unavailable")?;
                let state = StoredWorldState(
                    decode_fresh_world(player_index, packet.opcode, &payload)
                        .map_err(|_| "invalid initial world state")?,
                );
                let ciphers = entry
                    .take_ciphers()
                    .map_err(|_| "invalid world packet cipher handoff")?;
                self.packets = Some(
                    RegistryDecoder::new(Some(ciphers.incoming), openrs_login::MAX_PAYLOAD_BYTES)
                        .map_err(|_| "invalid world packet decoder configuration")?,
                );
                self.outgoing = Some(ciphers.outgoing);
                self.overlay.reset_world();
                if self.scene_enabled {
                    self.scene_store.clear();
                    self.scene = Some(LiveScene::enter(
                        player_index,
                        &state.0.map,
                        &payload,
                        self.scene_store.clone(),
                    )?);
                }
                self.initial = Some(state);
                self.entry = None;
                Some(WorldEvent::WorldReady)
            } else {
                None
            };
            return Ok(WorldFeed {
                consumed: feed.consumed,
                event,
            });
        }
        let attempt = self.attempt.as_mut().ok_or("invalid world receive phase")?;
        let feed = attempt
            .feed(bytes)
            .map_err(|_| "invalid world login response")?;
        let event = match feed.event {
            Some(LoginResponse::WorldSuccess(success)) => {
                if !(1..2048).contains(&success.player_index) {
                    return Err("invalid world player index");
                }
                let ciphers = attempt
                    .take_ciphers()
                    .map_err(|_| "invalid world login cipher handoff")?;
                self.entry = Some(
                    WorldEntryDecoder::new(ciphers, openrs_login::MAX_PAYLOAD_BYTES)
                        .map_err(|_| "invalid world entry configuration")?,
                );
                self.player_index = Some(success.player_index);
                self.attempt = None;
                Some(WorldEvent::LoginAccepted)
            }
            Some(LoginResponse::ConfirmationRequired) => Some(WorldEvent::ConfirmationRequired),
            Some(LoginResponse::TimeSync { .. }) => Some(WorldEvent::TimeSync),
            Some(LoginResponse::ReconnectRequired) => {
                self.clear("reconnect");
                Some(WorldEvent::ReconnectRequired)
            }
            Some(LoginResponse::RetryRequested) => {
                self.clear("retry");
                Some(WorldEvent::RetryRequested)
            }
            Some(LoginResponse::Queue { .. }) => {
                self.clear("queued");
                Some(WorldEvent::Queued)
            }
            Some(
                LoginResponse::Rejected { response_code }
                | LoginResponse::RejectedDetail { response_code, .. },
            ) => {
                self.clear("rejected");
                Some(WorldEvent::Rejected { response_code })
            }
            Some(LoginResponse::LobbySuccess(_)) => return Err("unexpected lobby login response"),
            None => None,
        };
        Ok(WorldFeed {
            consumed: feed.consumed,
            event,
        })
    }

    pub fn confirm(&mut self) -> Result<(), &'static str> {
        self.guard()?;
        if self
            .attempt
            .as_mut()
            .is_none_or(|attempt| attempt.confirm().is_err())
        {
            return self.fail("invalid world confirmation phase");
        }
        Ok(())
    }

    pub fn finish(&mut self) -> Result<(), &'static str> {
        if self.current_phase() == "closed" {
            return Ok(());
        }
        self.guard()?;
        if self.pending_ping.is_some() || self.flush_pending {
            return self.fail("world connection ended with unflushed bytes");
        }
        if let Some(attempt) = self.attempt.as_mut()
            && attempt.finish().is_err()
        {
            return self.fail("world connection ended during login");
        }
        if let Some(entry) = self.entry.as_mut()
            && entry.finish().is_err()
        {
            return self.fail("world connection ended during initial packet");
        }
        if let Some(packets) = self.packets.as_mut()
            && packets.finish().is_err()
        {
            return self.fail("world connection ended during packet");
        }
        self.clear("closed");
        Ok(())
    }

    pub fn close(&mut self) {
        self.clear("closed");
    }

    fn current_phase(&self) -> &'static str {
        if let Some(terminal) = self.terminal {
            terminal
        } else if self.initial.is_some() {
            "worldReady"
        } else if self.entry.is_some() {
            "awaitInitialWorld"
        } else {
            self.attempt.as_ref().map_or("failed", |attempt| {
                LobbyPhase::from(attempt.phase()).as_str()
            })
        }
    }

    fn expire(&mut self) -> bool {
        if self.terminal.is_none() && self.lifetime.expired() {
            self.clear("failed");
            true
        } else {
            false
        }
    }

    fn guard(&mut self) -> Result<(), &'static str> {
        if self.expire() {
            return Err("world session lifetime limit");
        }
        match self.current_phase() {
            "closed" => return Err("world session is closed"),
            "failed" => return Err("world session has failed"),
            _ => {}
        }
        self.operations += 1;
        if self.operations > MAX_OPERATIONS {
            return self.fail("world session operation limit");
        }
        Ok(())
    }

    fn now_ms(&mut self) -> Result<u64, &'static str> {
        match self.lifetime.elapsed_ms() {
            Some(now_ms) => Ok(now_ms),
            None => self.fail("world monotonic clock unavailable"),
        }
    }

    fn record_events(&mut self, count: usize) -> Result<(), &'static str> {
        if count > MAX_EVENTS - self.events {
            return self.fail("world packet event limit");
        }
        self.events += count;
        Ok(())
    }

    fn fail<T>(&mut self, error: &'static str) -> Result<T, &'static str> {
        self.clear("failed");
        Err(error)
    }

    fn clear(&mut self, phase: &'static str) {
        if let Some(attempt) = self.attempt.as_mut() {
            attempt.abort();
        }
        if let Some(entry) = self.entry.as_mut() {
            let _ = entry.finish();
        }
        self.attempt = None;
        self.entry = None;
        self.packets = None;
        self.outgoing = None;
        self.pending_ping = None;
        self.queued.clear();
        self.flush_pending = false;
        self.idle_ticks = 0;
        self.player_index = None;
        self.initial = None;
        self.variables = None;
        self.inventory = None;
        self.player_stats = None;
        self.scene = None;
        self.overlay.reset_world();
        self.scene_store.clear();
        self.terminal = Some(phase);
    }
}

impl Drop for WorldSession {
    fn drop(&mut self) {
        self.clear("closed");
    }
}

#[cfg(target_arch = "wasm32")]
pub(crate) mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    pub(crate) fn bounded_varp_number(
        value: JsValue,
        max: usize,
        error: &'static str,
    ) -> Result<usize, JsValue> {
        value
            .as_f64()
            .filter(|value| {
                value.is_finite() && value.fract() == 0.0 && (0.0..=max as f64).contains(value)
            })
            .map(|value| value as usize)
            .ok_or_else(|| JsValue::from_str(error))
    }

    fn inventory_key(id: JsValue, negative_key: JsValue) -> Result<ContainerKey, JsValue> {
        let id = bounded_varp_number(id, u16::MAX as usize, "invalid world container id")?;
        let negative_key = negative_key
            .as_bool()
            .ok_or_else(|| JsValue::from_str("invalid world container namespace"))?;
        Ok(ContainerKey {
            id: id as u16,
            negative_key,
        })
    }

    #[wasm_bindgen(js_name = OriginalWorldSession)]
    pub struct BrowserWorldSession(pub(crate) WorldSession);

    impl BrowserWorldSession {
        fn scene_provided_json(&mut self) -> Result<String, JsValue> {
            let effects = self.0.scene_provided().map_err(JsValue::from_str)?;
            Ok(format!("{{\"retried\":{}}}", effects.len()))
        }
    }

    #[wasm_bindgen(js_class = OriginalWorldSession)]
    impl BrowserWorldSession {
        pub fn outgoing(&mut self) -> Result<js_sys::Uint8Array, JsValue> {
            let bytes = self.0.take_outgoing().map_err(JsValue::from_str)?;
            Ok(js_sys::Uint8Array::from(bytes.as_slice()))
        }

        pub fn feed(&mut self, bytes: JsValue) -> Result<String, JsValue> {
            let bytes = match crate::lobby::browser::bounded_input(
                bytes,
                "invalid world incoming type",
                "world incoming chunk byte limit",
            ) {
                Ok(bytes) => bytes,
                Err(error) => return self.0.fail(error).map_err(JsValue::from_str),
            };
            let report = self.0.feed(&bytes).map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("world feed serialization failed"))
        }

        pub fn tick(&mut self) -> Result<String, JsValue> {
            let tick = self.0.tick().map_err(JsValue::from_str)?;
            serde_json::to_string(&tick)
                .map_err(|_| JsValue::from_str("world tick serialization failed"))
        }

        #[wasm_bindgen(js_name = gameTick)]
        pub fn game_tick(&mut self) -> Result<(), JsValue> {
            self.0.game_tick().map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = markFlushed)]
        pub fn mark_flushed(&mut self) -> Result<(), JsValue> {
            self.0.mark_flushed().map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = enableInventory)]
        pub fn enable_inventory(&mut self) -> Result<(), JsValue> {
            self.0
                .enable_inventory(InventoryLimits::default())
                .map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = enablePlayerStats)]
        pub fn enable_player_stats(&mut self) -> Result<(), JsValue> {
            self.0
                .enable_player_stats(PlayerStatsLimits::default())
                .map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = advanceOverlay)]
        pub fn advance_overlay(&mut self, cycles: u32) {
            self.0.advance_overlay(cycles);
        }

        #[wasm_bindgen(js_name = hintScreen)]
        pub fn hint_screen(
            &mut self,
            view_projection: &[f32],
            origin_x: f32,
            origin_y: f32,
            center_x: f32,
            center_y: f32,
            offset_x: f32,
            offset_y: f32,
        ) -> Result<String, JsValue> {
            let viewport = crate::model_hover::ProjectionViewport {
                origin_x,
                origin_y,
                center_x,
                center_y,
            };
            let offset = crate::hint_screen::HintOffset {
                x: offset_x,
                y: offset_y,
            };
            let report = self
                .0
                .hint_screen(view_projection, viewport, offset)
                .map_err(JsValue::from_str)?;
            serde_json::to_string(&report).map_err(|_| JsValue::from_str("hint screen serialization failed"))
        }

        #[wasm_bindgen(js_name = stackOverheadLabels)]
        pub fn stack_overhead_labels(flat: &[i32], padding: i32) -> Vec<i32> {
            let labels: Vec<crate::hint_screen::LabelBox> = flat
                .chunks_exact(3)
                .map(|c| crate::hint_screen::LabelBox {
                    x: c[0],
                    y: c[1],
                    half_width: c[2],
                })
                .collect();
            crate::hint_screen::stack_labels(&labels, padding)
        }

        #[wasm_bindgen(js_name = sceneEntityHits)]
        pub fn scene_entity_hits(
            &mut self,
            view_projection: &[f32],
            mouse_x: i32,
            mouse_y: i32,
            origin_x: f32,
            origin_y: f32,
            center_x: f32,
            center_y: f32,
        ) -> Result<Vec<i32>, JsValue> {
            let viewport = crate::model_hover::ProjectionViewport {
                origin_x,
                origin_y,
                center_x,
                center_y,
            };
            self.0
                .entity_hits(view_projection, (mouse_x, mouse_y), viewport)
                .map(|hits| {
                    hits.into_iter()
                        .flat_map(|(kind, id)| [i32::from(kind), id])
                        .collect()
                })
                .map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = addStaticLocs)]
        pub fn add_static_locs(&mut self, region_id: u32, data: &[u8]) -> Result<u32, JsValue> {
            let region_id = i32::try_from(region_id)
                .map_err(|_| JsValue::from_str("invalid region id"))?;
            self.0
                .add_static_locs(region_id, data)
                .map(|count| count as u32)
                .map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = advanceScene)]
        pub fn advance_scene(&mut self, cycles: u32) -> f64 {
            self.0.advance_scene(cycles) as f64
        }

        #[wasm_bindgen(js_name = setHintSpriteCount)]
        pub fn set_hint_sprite_count(&mut self, count: u32) {
            self.0.overlay_mut().set_hint_sprite_count(count as usize);
        }

        #[wasm_bindgen(js_name = overlay)]
        pub fn overlay(&self) -> Result<String, JsValue> {
            serde_json::to_string(&self.0.overlay().report())
                .map_err(|_| JsValue::from_str("overlay report unavailable"))
        }

        #[wasm_bindgen(js_name = chatLines)]
        pub fn chat_lines(&mut self, limit: u32) -> Result<String, JsValue> {
            let lines: Vec<_> = self
                .0
                .overlay_mut()
                .chat_state()
                .lines
                .iter()
                .flatten()
                .take(limit as usize)
                .map(|line| {
                    serde_json::json!({
                        "identifier": line.identifier,
                        "time": line.time,
                        "type": line.kind,
                        "name": line.name,
                        "crownedName": line.crowned_name,
                        "clan": line.clan,
                        "message": line.message,
                    })
                })
                .collect();
            serde_json::to_string(&lines).map_err(|_| JsValue::from_str("chat lines unavailable"))
        }

        #[wasm_bindgen(js_name = consoleLines)]
        pub fn console_lines(&self) -> Vec<String> {
            self.0.overlay().console_lines().to_vec()
        }

        #[wasm_bindgen(js_name = takeTypedMessages)]
        pub fn take_typed_messages(&mut self) -> Result<String, JsValue> {
            let messages: Vec<_> = self
                .0
                .take_cs2_typed_messages()
                .into_iter()
                .map(|message| {
                    serde_json::json!({
                        "type": message.message_type,
                        "flags": message.effect_flags,
                        "text": message.text,
                    })
                })
                .collect();
            serde_json::to_string(&messages)
                .map_err(|_| JsValue::from_str("typed messages unavailable"))
        }

        #[wasm_bindgen(js_name = takeScriptConsoleLines)]
        pub fn take_script_console_lines(&mut self) -> Vec<String> {
            self.0.take_cs2_console_lines()
        }

        #[wasm_bindgen(js_name = takeScriptConsoleClears)]
        pub fn take_script_console_clears(&mut self) -> Vec<String> {
            self.0.take_cs2_console_clears()
        }

        #[wasm_bindgen(js_name = takeRouteRequests)]
        pub fn take_route_requests(&mut self) -> Vec<i32> {
            self.0
                .take_cs2_route_requests()
                .into_iter()
                .flat_map(|request| {
                    [
                        request.x,
                        request.y,
                        request.size,
                        i32::from(request.find_alternative),
                    ]
                })
                .collect()
        }

        #[wasm_bindgen(js_name = takeInterfaceCloses)]
        pub fn take_interface_closes(&mut self) -> u32 {
            self.0.take_cs2_interface_closes()
        }

        #[wasm_bindgen(js_name = takeAudioEvents)]
        pub fn take_audio_events(&mut self) -> Vec<i32> {
            self.0.take_audio_events()
        }

        #[wasm_bindgen(js_name = takeCameraCommands)]
        pub fn take_camera_commands(&mut self) -> Vec<i32> {
            self.0.take_camera_commands()
        }

        #[wasm_bindgen(js_name = openInterfaces)]
        pub fn open_interfaces(&self) -> Vec<i32> {
            let open = self.0.open_interfaces();
            let mut out = vec![open.top.unwrap_or(-1), open.revision as i32];
            for (parent, (interface, overlay)) in &open.subs {
                out.extend([*parent, *interface, i32::from(*overlay)]);
            }
            out
        }

        #[wasm_bindgen(js_name = cameraSceneBase)]
        pub fn camera_scene_base(&self) -> Vec<i32> {
            self.0.camera_scene_base()
        }

        #[wasm_bindgen(js_name = enableScene)]
        pub fn enable_scene(&mut self) -> Result<(), JsValue> {
            self.0.enable_scene().map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = sceneNeeds)]
        pub fn scene_needs(&mut self) -> Vec<u32> {
            self.0
                .scene_needs()
                .into_iter()
                .flat_map(|(index, archive)| [u32::from(index), archive])
                .collect()
        }

        #[wasm_bindgen(js_name = sceneAddFile)]
        pub fn scene_add_file(&mut self, index: u8, archive: u32, file: u32, bytes: &[u8]) {
            self.0
                .scene_store()
                .add_file(index, archive, file, bytes.to_vec());
        }

        #[wasm_bindgen(js_name = sceneFinishGroup)]
        pub fn scene_finish_group(&mut self, index: u8, archive: u32) -> Result<String, JsValue> {
            self.0.scene_store().finish_group(index, archive);
            self.scene_provided_json()
        }

        #[wasm_bindgen(js_name = sceneAbsent)]
        pub fn scene_absent(&mut self, index: u8, archive: u32) -> Result<String, JsValue> {
            self.0.scene_store().provide_absent(index, archive);
            self.scene_provided_json()
        }

        #[wasm_bindgen(js_name = sceneHeights)]
        pub fn scene_heights(
            &mut self,
            tiles: u32,
            planes: u32,
            heights: &[i32],
        ) -> Result<(), JsValue> {
            let heights = heights
                .iter()
                .map(|height| (*height != i32::MIN).then_some(*height))
                .collect();
            let map = HeightMap::new(tiles as usize, planes as usize, heights)
                .ok_or_else(|| JsValue::from_str("invalid scene height grid"))?;
            self.0.set_scene_heights(map).map_err(JsValue::from_str)
        }

        #[wasm_bindgen(js_name = sceneNpcs)]
        pub fn scene_npcs(&self) -> Vec<i32> {
            let Some(scene) = self.0.scene() else {
                return Vec::new();
            };
            let mut indices: Vec<u16> = scene.npc_states.keys().copied().collect();
            indices.sort_unstable();
            indices
                .into_iter()
                .filter_map(|index| {
                    let (x, y, plane) = scene.npc_position(index)?;
                    let type_id = scene.npc_type_id(index)?;
                    Some([i32::from(index), type_id, x, y, i32::from(plane)])
                })
                .flatten()
                .collect()
        }

        #[wasm_bindgen(js_name = sceneDefinition)]
        pub fn scene_definition(&mut self, kind: u8, id: i32) -> String {
            crate::definitions::definition(self.0.scene_store(), kind, id)
                .map(|found| found.to_json())
                .unwrap_or_default()
        }

        #[wasm_bindgen(js_name = sceneDraw)]
        pub fn scene_draw(&mut self) -> Result<String, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            serde_json::to_string(&serde_json::json!({
                "meshes": draw.meshes.len(),
                "triangles": draw.triangles(),
                "incomplete": draw.incomplete(),
                "pending": draw.pending,
                "locs": draw.stats.locs,
                "groundItems": draw.stats.ground_items,
                "players": draw.stats.players,
                "npcs": draw.stats.npcs,
                "posed": draw.stats.posed,
                "skipped": draw.stats.skipped,
                "missingHeights": draw.stats.missing_heights,
            }))
            .map_err(|_| JsValue::from_str("world scene draw serialization failed"))
        }

        #[wasm_bindgen(js_name = sceneDrawMeta)]
        pub fn scene_draw_meta(&mut self) -> Result<Vec<i32>, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            Ok(draw
                .meshes
                .iter()
                .flat_map(|mesh| {
                    [
                        i32::from(mesh.kind.code()),
                        mesh.id,
                        i32::from(mesh.plane),
                        mesh.tile.0,
                        mesh.tile.1,
                        mesh.triangles() as i32,
                    ]
                })
                .collect())
        }

        #[wasm_bindgen(js_name = sceneDrawPositions)]
        pub fn scene_draw_positions(&mut self) -> Result<Vec<i32>, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            Ok(draw
                .meshes
                .iter()
                .flat_map(|mesh| mesh.positions.iter().copied())
                .collect())
        }

        #[wasm_bindgen(js_name = sceneDrawColors)]
        pub fn scene_draw_colors(&mut self) -> Result<Vec<i16>, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            Ok(draw
                .meshes
                .iter()
                .flat_map(|mesh| mesh.colors.iter().copied())
                .collect())
        }

        #[wasm_bindgen(js_name = sceneDrawAlphas)]
        pub fn scene_draw_alphas(&mut self) -> Result<Vec<u8>, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            Ok(draw
                .meshes
                .iter()
                .flat_map(|mesh| mesh.alphas.iter().copied())
                .collect())
        }

        #[wasm_bindgen(js_name = sceneDrawMaterials)]
        pub fn scene_draw_materials(&mut self) -> Result<Vec<i16>, JsValue> {
            let draw = self.0.scene_draw().map_err(JsValue::from_str)?;
            Ok(draw
                .meshes
                .iter()
                .flat_map(|mesh| mesh.materials.iter().copied())
                .collect())
        }

        pub fn skill(&mut self, id: JsValue) -> Result<String, JsValue> {
            let id = bounded_varp_number(
                id,
                openrs_login::player_stats::SKILL_COUNT - 1,
                "invalid world skill id",
            )?;
            let report = self.0.skill(id as u8).map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("world skill serialization failed"))
        }

        #[wasm_bindgen(js_name = playerStats)]
        pub fn player_stats(&mut self) -> Result<String, JsValue> {
            let report = self.0.player_stats().map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("world player stats serialization failed"))
        }

        pub fn container(&mut self, id: JsValue, negative_key: JsValue) -> Result<String, JsValue> {
            let key = inventory_key(id, negative_key)?;
            let report = self.0.container(key).map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("world container serialization failed"))
        }

        pub fn item(
            &mut self,
            id: JsValue,
            negative_key: JsValue,
            slot: JsValue,
        ) -> Result<String, JsValue> {
            let key = inventory_key(id, negative_key)?;
            let slot = bounded_varp_number(
                slot,
                openrs_login::item_containers::MAX_CONTAINER_SLOTS - 1,
                "invalid world inventory slot",
            )?;
            let report = self.0.item(key, slot).map_err(JsValue::from_str)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("world inventory item serialization failed"))
        }

        pub fn varp(&mut self, id: JsValue) -> Result<i32, JsValue> {
            let id = bounded_varp_number(id, u16::MAX as usize, "invalid world variable id")?;
            self.0.varp(id as u16).map_err(JsValue::from_str)
        }

        pub fn phase(&mut self) -> String {
            self.0.phase().into()
        }

        pub fn report(&mut self) -> Result<String, JsValue> {
            serde_json::to_string(&self.0.report())
                .map_err(|_| JsValue::from_str("world report serialization failed"))
        }

        #[wasm_bindgen(js_name = sceneTile)]
        pub fn scene_tile(&mut self) -> Option<Vec<i32>> {
            let report = self.0.report();
            let (plane, x, y) = report.initial_world?.scene_tile()?;
            Some(vec![i32::from(plane), x, y])
        }

        pub fn confirm(&mut self) -> Result<(), JsValue> {
            self.0.confirm().map_err(JsValue::from_str)
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(JsValue::from_str)
        }

        pub fn close(&mut self) {
            self.0.close();
        }
    }
}

#[cfg(test)]
mod tests;
