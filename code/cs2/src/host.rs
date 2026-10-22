use std::{collections::BTreeMap, fmt, sync::Arc};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Varbit {
    pub varp: u16,
    pub start: u8,
    pub end: u8,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum VarbitLookup {
    Defined(Varbit),
    Absent,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum HostError {
    Unavailable,
    InvalidVarp(i32),
    InvalidVarbit(i32),
    InvalidVarbitSpan { id: i32, start: u8, end: u8 },
    State(String),
    Varc(crate::VarcError),
    NoEntity,
    EntityType,
}

impl fmt::Display for HostError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "CS2 variable host: {self:?}")
    }
}

impl std::error::Error for HostError {}

pub trait VarHost {
    fn varp(&self, id: i32) -> Result<i32, HostError>;
    fn set_varp(&mut self, id: i32, value: i32) -> Result<(), HostError>;
    fn varbit(&self, id: i32) -> Result<VarbitLookup, HostError>;
    fn entity_varp(&self, _id: i32) -> Result<i32, HostError> {
        Err(HostError::Unavailable)
    }
    fn entity_varbit(&self, _id: i32) -> Result<VarbitLookup, HostError> {
        Err(HostError::Unavailable)
    }
    fn varcs(&mut self) -> Result<&mut crate::VarcState, HostError> {
        Err(HostError::Unavailable)
    }
    fn enum_definition(&self, _id: i32) -> Result<Option<Arc<EnumDefinition>>, HostError> {
        Err(HostError::Unavailable)
    }
    fn components(&mut self) -> Result<&mut crate::ComponentModel, HostError> {
        Err(HostError::Unavailable)
    }
    fn items(&mut self) -> Result<&mut crate::ItemModel, HostError> {
        Err(HostError::Unavailable)
    }
    fn player_state(&self) -> Result<&crate::PlayerState, HostError> {
        Err(HostError::Unavailable)
    }
    fn npc_state(&self) -> Result<&crate::NpcState, HostError> {
        Err(HostError::Unavailable)
    }
    fn entity_state(&self) -> Result<&crate::EntityState, HostError> {
        Err(HostError::Unavailable)
    }
    fn entity_state_mut(&mut self) -> Result<&mut crate::EntityState, HostError> {
        Err(HostError::Unavailable)
    }
    fn appearance_state_mut(&mut self) -> Result<&mut crate::AppearanceState, HostError> {
        Err(HostError::Unavailable)
    }
    fn social_state(&self) -> Result<&crate::SocialState, HostError> {
        Err(HostError::Unavailable)
    }
    fn social_state_mut(&mut self) -> Result<&mut crate::SocialState, HostError> {
        Err(HostError::Unavailable)
    }
    fn clan_state(&self) -> Result<&crate::ClanState, HostError> {
        Err(HostError::Unavailable)
    }
    fn clan_var_defs(&self) -> Result<&crate::ClanVarDefs, HostError> {
        Err(HostError::Unavailable)
    }
    fn chat_state(&self) -> Result<&crate::ChatState, HostError> {
        Err(HostError::Unavailable)
    }
    fn chat_state_mut(&mut self) -> Result<&mut crate::ChatState, HostError> {
        Err(HostError::Unavailable)
    }
    fn outbox(&mut self) -> Result<&mut crate::Outbox, HostError> {
        Err(HostError::Unavailable)
    }
    fn bas_definition(&self, _id: i32) -> Result<Option<Arc<crate::BasDefinition>>, HostError> {
        Err(HostError::Unavailable)
    }
    fn item_definition(
        &self,
        _id: i32,
    ) -> Result<Option<Arc<crate::ItemDefinition>>, HostError> {
        Err(HostError::Unavailable)
    }
    fn param_definition(
        &self,
        _id: i32,
    ) -> Result<Option<Arc<crate::ParamDefinition>>, HostError> {
        Err(HostError::Unavailable)
    }
    fn config_params(
        &self,
        _kind: crate::ConfigKind,
        _id: i32,
    ) -> Result<Option<Arc<crate::ParamTable>>, HostError> {
        Err(HostError::Unavailable)
    }
    fn interaction_colours(&self) -> Result<crate::InteractionColours, HostError> {
        Err(HostError::Unavailable)
    }
    fn interface_option(
        &mut self,
        _interaction: crate::OptionInteraction,
        _name: &str,
    ) -> Result<(), HostError> {
        Err(HostError::Unavailable)
    }
    fn client(&mut self) -> Result<&mut crate::ClientState, HostError> {
        Err(HostError::Unavailable)
    }

    fn world_state(&mut self) -> Result<&mut crate::WorldState, HostError> {
        Err(HostError::Unavailable)
    }

    /// Milliseconds since the Unix epoch as the client's `Utils.time()` reports them.
    fn clock_millis(&self) -> Result<i64, HostError> {
        Err(HostError::Unavailable)
    }

    /// The next `Math.random()` draw, in `[0, 1)`.
    fn random_unit(&mut self) -> Result<f64, HostError> {
        Err(HostError::Unavailable)
    }

    /// The local zone's offset from UTC at the given UTC instant.
    fn local_offset_millis(&self, _utc_millis: i64) -> Result<i64, HostError> {
        Err(HostError::Unavailable)
    }

    /// The client language id; `None` is a client with no language selected.
    fn language(&self) -> Result<Option<i32>, HostError> {
        Err(HostError::Unavailable)
    }

    /// The 65536-entry HSV to RGB table; `None` before the client has built it.
    fn hsv_to_rgb_table(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        Err(HostError::Unavailable)
    }

    /// The decoded font metrics file of a font id; `None` where the cache has no such font.
    fn font_metrics(&self, _id: i32) -> Result<Option<Arc<crate::FontMetrics>>, HostError> {
        Err(HostError::Unavailable)
    }

    fn item_model_view(&self, _id: i32) -> Result<Option<crate::ItemModelView>, HostError> {
        Err(HostError::Unavailable)
    }

    fn npc_model_definition(
        &self,
        _id: i32,
    ) -> Result<Option<Arc<crate::NpcModelDefinition>>, HostError> {
        Err(HostError::Unavailable)
    }

    fn sequence_playable(&self, _id: i32) -> Result<bool, HostError> {
        Err(HostError::Unavailable)
    }

    fn sprite_dimensions(&self, _id: i32) -> Result<Option<(i32, i32)>, HostError> {
        Err(HostError::Unavailable)
    }

    fn quick_chat(&self) -> Result<&crate::QuickChatDefs, HostError> {
        Err(HostError::Unavailable)
    }

    fn quest_defs(&self) -> Result<&crate::QuestDefs, HostError> {
        Err(HostError::Unavailable)
    }

    fn lobby_state(&self) -> Result<&crate::LobbyState, HostError> {
        Err(HostError::Unavailable)
    }

    /// `getSpriteWidth` of each name icon sprite (`<img=n>`); `None` before they are loaded.
    fn name_icon_widths(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        Err(HostError::Unavailable)
    }

    /// The width and height of an inline `<sprite=id,frame>` graphic; `None` where it is missing.
    fn inline_graphic_size(&self, _id: i32, _frame: i32) -> Result<Option<(i32, i32)>, HostError> {
        Err(HostError::Unavailable)
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum EnumValue {
    Int(i32),
    Text(Arc<str>),
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct EnumDefinition {
    pub key_type: i32,
    pub value_type: i32,
    pub default_text: Arc<str>,
    pub default_int: i32,
    pub size: i32,
    pub values: Option<Vec<Option<EnumValue>>>,
    pub parameters: Option<BTreeMap<i32, EnumValue>>,
}

impl Default for EnumDefinition {
    fn default() -> Self {
        Self {
            key_type: 0,
            value_type: 0,
            default_text: Arc::from("null"),
            default_int: 0,
            size: 0,
            values: None,
            parameters: None,
        }
    }
}

impl EnumDefinition {
    pub(crate) fn value(&self, key: i32) -> Option<&EnumValue> {
        match &self.values {
            Some(values) => usize::try_from(key)
                .ok()
                .and_then(|index| values.get(index))
                .and_then(Option::as_ref),
            None => self.parameters.as_ref().and_then(|map| map.get(&key)),
        }
    }

    pub(crate) fn references(&self, target: &EnumValue) -> Option<Vec<i32>> {
        if self.size == 0 {
            return Some(Vec::new());
        }
        if let Some(values) = &self.values {
            Some(
                values
                    .iter()
                    .enumerate()
                    .filter(|(_, value)| value.as_ref() == Some(target))
                    .map(|(index, _)| index as i32)
                    .collect(),
            )
        } else {
            self.parameters.as_ref().map(|map| {
                map.iter()
                    .filter(|(_, value)| *value == target)
                    .map(|(key, _)| *key)
                    .collect()
            })
        }
    }
}

impl Varbit {
    pub(crate) fn lookup(host: &dyn VarHost, id: i32) -> Result<Self, HostError> {
        if id < 0 {
            return Err(HostError::InvalidVarbit(id));
        }
        let definition = match host.varbit(id)? {
            VarbitLookup::Defined(definition) => definition,
            VarbitLookup::Absent => Self {
                varp: 0,
                start: 0,
                end: 0,
            },
        };
        if definition.start > definition.end || definition.end - definition.start > 31 {
            return Err(HostError::InvalidVarbitSpan {
                id,
                start: definition.start,
                end: definition.end,
            });
        }
        Ok(definition)
    }

    pub(crate) fn entity_read(self, value: i32) -> i32 {
        let end = u32::from(self.end);
        let mask = if end == 31 {
            -1
        } else {
            1i32.wrapping_shl(end + 1).wrapping_sub(1)
        };
        ((value & mask) as u32).wrapping_shr(u32::from(self.start)) as i32
    }

    pub(crate) fn mask(self) -> i32 {
        2i32.wrapping_shl(u32::from(self.end - self.start))
            .wrapping_sub(1)
    }

    pub(crate) fn read(self, value: i32) -> i32 {
        value.wrapping_shr(u32::from(self.start)) & self.mask()
    }

    pub(crate) fn write(self, previous: i32, value: i32) -> i32 {
        let mask = self.mask();
        let value = if value < 0 || value > mask { 0 } else { value };
        let shifted = mask.wrapping_shl(u32::from(self.start));
        (previous & !shifted) | (value.wrapping_shl(u32::from(self.start)) & shifted)
    }
}
