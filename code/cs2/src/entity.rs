use crate::{HostError, VarHost, Varbit};
use std::collections::BTreeMap;

pub const MODEL_MIN_Y_UNSET: i32 = -32768;
pub const GROUND_ITEM_MIN_Y: i32 = -10;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct NpcTypeInfo {
    pub name: String,
    pub visible: bool,
    pub icon_height: i32,
    pub bas_id: i32,
    pub alternative_ids: Option<Vec<i32>>,
    pub varp: i32,
    pub varpbit: i32,
}

impl Default for NpcTypeInfo {
    fn default() -> Self {
        Self {
            name: "null".to_string(),
            visible: true,
            icon_height: -1,
            bas_id: -1,
            alternative_ids: None,
            varp: -1,
            varpbit: -1,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct EntityCommon {
    pub player_index: i32,
    pub loaded_interface_id: i32,
    pub interface_id_to_load: i32,
    pub model_min_y: i32,
    pub hint_text: Option<String>,
    pub ground_decoration_offset: Option<i32>,
}

impl Default for EntityCommon {
    fn default() -> Self {
        Self {
            player_index: 0,
            loaded_interface_id: 0,
            interface_id_to_load: 0,
            model_min_y: MODEL_MIN_Y_UNSET,
            hint_text: None,
            ground_decoration_offset: None,
        }
    }
}

impl EntityCommon {
    pub fn set_overhead_interface_id(&mut self, interface_id: i32) {
        if self.interface_id_to_load != self.loaded_interface_id {
            self.clear_overhead_interface();
        }
        self.interface_id_to_load = interface_id;
    }

    fn clear_overhead_interface(&mut self) {
        if self.loaded_interface_id != -1 {
            self.loaded_interface_id = -1;
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct PlayerEntityState {
    pub common: EntityCommon,
    pub username: Option<String>,
    pub display_name: Option<String>,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
    pub render_animation: i32,
}

impl PlayerEntityState {
    pub fn username_with_title(&self) -> String {
        let mut full = String::new();
        if let Some(prefix) = &self.prefix_title {
            full.push_str(prefix);
        }
        full.push_str(self.username.as_deref().unwrap_or("null"));
        if let Some(postfix) = &self.postfix_title {
            full.push_str(postfix);
        }
        full
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct NpcEntityState {
    pub common: EntityCommon,
    pub type_id: Option<i32>,
    pub modified_name: Option<String>,
    pub current_render_animation: i32,
}

impl Default for NpcEntityState {
    fn default() -> Self {
        Self {
            common: EntityCommon::default(),
            type_id: None,
            modified_name: None,
            current_render_animation: -1,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum CurrentEntity {
    Player(PlayerEntityState),
    Npc(NpcEntityState),
}

impl CurrentEntity {
    pub fn common(&self) -> &EntityCommon {
        match self {
            Self::Player(player) => &player.common,
            Self::Npc(npc) => &npc.common,
        }
    }

    pub fn common_mut(&mut self) -> &mut EntityCommon {
        match self {
            Self::Player(player) => &mut player.common,
            Self::Npc(npc) => &mut npc.common,
        }
    }

    pub fn cs2_index(&self) -> i32 {
        match self {
            Self::Player(player) => player.common.player_index.wrapping_neg().wrapping_sub(1),
            Self::Npc(npc) => npc.common.player_index.wrapping_add(1),
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct GroundItemState {
    pub tile_height_offset: i32,
}

impl GroundItemState {
    pub fn icon_height(&self) -> i32 {
        self.tile_height_offset.wrapping_sub(GROUND_ITEM_MIN_Y)
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct NearbyPlayer {
    pub index: i32,
    pub present: bool,
    pub is_self: bool,
    pub display_name: Option<String>,
    pub movement_x: i32,
    pub movement_y: i32,
    pub size: i32,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MyCoord {
    pub plane: i32,
    pub fine_x: i32,
    pub fine_z: i32,
    pub base_x: i32,
    pub base_y: i32,
}

impl MyCoord {
    pub fn packed(&self) -> i32 {
        let x = (self.fine_x >> 9).wrapping_add(self.base_x);
        let y = (self.fine_z >> 9).wrapping_add(self.base_y);
        (self.plane << 28)
            .wrapping_add(x << 14)
            .wrapping_add(y)
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct EntityState {
    pub my_coord: Option<MyCoord>,
    pub nearby_players: Vec<NearbyPlayer>,
    pub has_my_player: bool,
    pub current: Option<CurrentEntity>,
    pub ground_item: Option<GroundItemState>,
    pub entity_target: i32,
    pub public_filter: i32,
    pub friend_names: Vec<String>,
    pub my_display_name: Option<String>,
    pub npc_types: BTreeMap<i32, NpcTypeInfo>,
    pub bas_icon_heights: BTreeMap<i32, i32>,
}

fn index_error(index: i32) -> HostError {
    HostError::State(format!("array index {index} out of bounds"))
}

fn same_name(left: &str, right: &str) -> bool {
    left == right || left.to_lowercase() == right.to_lowercase()
}

impl EntityState {
    pub fn current(&self) -> Result<&CurrentEntity, HostError> {
        self.current.as_ref().ok_or(HostError::NoEntity)
    }

    pub fn current_mut(&mut self) -> Result<&mut CurrentEntity, HostError> {
        self.current.as_mut().ok_or(HostError::NoEntity)
    }

    pub fn player(&self) -> Result<&PlayerEntityState, HostError> {
        match self.current()? {
            CurrentEntity::Player(player) => Ok(player),
            CurrentEntity::Npc(_) => Err(HostError::EntityType),
        }
    }

    pub fn npc(&self) -> Result<&NpcEntityState, HostError> {
        match self.current()? {
            CurrentEntity::Npc(npc) => Ok(npc),
            CurrentEntity::Player(_) => Err(HostError::EntityType),
        }
    }

    pub fn ground_item(&self) -> Result<&GroundItemState, HostError> {
        self.ground_item.as_ref().ok_or(HostError::NoEntity)
    }

    pub fn npc_type(&self, id: i32) -> Result<&NpcTypeInfo, HostError> {
        self.npc_types
            .get(&id)
            .ok_or(HostError::NoEntity)
    }

    pub fn npc_definitions(&self, npc: &NpcEntityState) -> Result<&NpcTypeInfo, HostError> {
        self.npc_type(npc.type_id.ok_or(HostError::NoEntity)?)
    }

    pub fn friend_or_self(&self, display_name: Option<&str>) -> bool {
        let Some(name) = display_name else {
            return false;
        };
        self.friend_names.iter().any(|friend| same_name(name, friend))
            || self
                .my_display_name
                .as_deref()
                .is_some_and(|mine| same_name(name, mine))
    }

    pub fn hint_text(&self, entity: &CurrentEntity) -> Option<String> {
        let text = entity.common().hint_text.as_ref()?;
        match entity {
            CurrentEntity::Npc(_) => Some(text.clone()),
            CurrentEntity::Player(player) => {
                let filter = self.public_filter;
                if filter == 0
                    || filter == 3
                    || (filter == 1 && self.friend_or_self(player.display_name.as_deref()))
                {
                    Some(text.clone())
                } else {
                    None
                }
            }
        }
    }

    pub fn bas_icon_height_override(&self, render_animation: i32) -> Result<i32, HostError> {
        if render_animation == -1 {
            Ok(-1)
        } else {
            self.bas_icon_heights
                .get(&render_animation)
                .copied()
                .ok_or(HostError::NoEntity)
        }
    }
}

pub fn multi_npc_index(host: &dyn VarHost, definitions: &NpcTypeInfo) -> Result<i32, HostError> {
    if definitions.varpbit != -1 {
        let varbit = Varbit::lookup(host, definitions.varpbit)?;
        let value = host.varp(i32::from(varbit.varp))?;
        Ok(varbit.read(value))
    } else if definitions.varp != -1 {
        host.varp(definitions.varp)
    } else {
        Ok(-1)
    }
}

pub fn multi_npc<'a>(
    host: &dyn VarHost,
    state: &'a EntityState,
    definitions: &NpcTypeInfo,
) -> Result<Option<&'a NpcTypeInfo>, HostError> {
    let alternatives = definitions
        .alternative_ids
        .as_ref()
        .ok_or(HostError::NoEntity)?;
    let index = multi_npc_index(host, definitions)?;
    let last = alternatives.len() as i32 - 1;
    if index >= 0 && index < last {
        let target = alternatives[index as usize];
        if target != -1 {
            return state.npc_type(target).map(Some);
        }
    }
    let morphed = *usize::try_from(last)
        .ok()
        .and_then(|slot| alternatives.get(slot))
        .ok_or_else(|| index_error(last))?;
    if morphed != -1 {
        state.npc_type(morphed).map(Some)
    } else {
        Ok(None)
    }
}

pub fn npc_render_animation(
    host: &dyn VarHost,
    state: &EntityState,
    npc: &NpcEntityState,
) -> Result<i32, HostError> {
    if npc.current_render_animation != -1 {
        return Ok(npc.current_render_animation);
    }
    let Some(type_id) = npc.type_id else {
        return Ok(-1);
    };
    let definitions = state.npc_type(type_id)?;
    if definitions.alternative_ids.is_some() {
        if let Some(resolved) = multi_npc(host, state, definitions)? {
            if resolved.bas_id != -1 {
                return Ok(resolved.bas_id);
            }
        }
    }
    Ok(definitions.bas_id)
}

pub fn pathing_icon_height(
    state: &EntityState,
    common: &EntityCommon,
    render_animation: i32,
) -> Result<i32, HostError> {
    let override_height = state.bas_icon_height_override(render_animation)?;
    let height = if override_height != -1 {
        override_height
    } else if common.model_min_y == MODEL_MIN_Y_UNSET {
        200
    } else {
        common.model_min_y.wrapping_neg()
    };
    Ok(match common.ground_decoration_offset {
        Some(offset) => offset.wrapping_add(height),
        None => height,
    })
}

pub fn entity_icon_height(host: &dyn VarHost, state: &EntityState) -> Result<i32, HostError> {
    match state.current()? {
        CurrentEntity::Player(player) => {
            pathing_icon_height(state, &player.common, player.render_animation)
        }
        CurrentEntity::Npc(npc) => {
            let definitions = state.npc_definitions(npc)?;
            if definitions.alternative_ids.is_some() {
                if let Some(resolved) = multi_npc(host, state, definitions)? {
                    if resolved.icon_height != -1 {
                        return Ok(resolved.icon_height);
                    }
                }
            }
            if definitions.icon_height != -1 {
                return Ok(definitions.icon_height);
            }
            let render = npc_render_animation(host, state, npc)?;
            pathing_icon_height(state, &npc.common, render)
        }
    }
}

pub fn npc_name(host: &dyn VarHost, state: &EntityState) -> Result<String, HostError> {
    let npc = state.npc()?;
    let definitions = state.npc_definitions(npc)?;
    let name = if definitions.alternative_ids.is_some() {
        match multi_npc(host, state, definitions)? {
            Some(resolved) => Some(resolved.name.clone()),
            None => Some(String::new()),
        }
    } else {
        npc.modified_name.clone()
    };
    Ok(name.unwrap_or_default())
}

pub fn npc_resolved<'a>(
    host: &dyn VarHost,
    state: &'a EntityState,
) -> Result<Option<&'a NpcTypeInfo>, HostError> {
    let npc = state.npc()?;
    let definitions = state.npc_definitions(npc)?;
    if definitions.alternative_ids.is_some() {
        multi_npc(host, state, definitions)
    } else {
        Ok(Some(definitions))
    }
}
