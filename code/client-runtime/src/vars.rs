use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::fmt;
use zeroize::Zeroize;

pub mod clock;
pub mod config;
pub mod player;
pub mod quick_chat;
pub mod social;
pub mod varbit;
pub mod world;

pub const MAX_VARPS: usize = u16::MAX as usize + 1;
pub const MAX_VARIABLE_TIME_MS: u64 = ((1u64 << 62) - 1) - LOCAL_OVERRIDE_MS;
const LOCAL_OVERRIDE_MS: u64 = 500;
const UPDATE_BUCKETS: usize = 128;
const IMMEDIATE_DEADLINE_MS: u64 = 1;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VariablePacket {
    VarpSmall { id: u16, value: i8 },
    VarpLarge { id: u16, value: i32 },
    ClearVarps,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VariableEffect {
    Pending { id: u16, value: i32 },
    Cleared,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct VariableChange {
    pub id: u16,
    pub previous: i32,
    pub value: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum VariableError {
    UnsupportedPacket(ServerPacket),
    PayloadLength {
        opcode: ServerPacket,
        expected: usize,
        actual: usize,
    },
    Payload(PayloadError),
    CountOutOfRange(usize),
    IdOutOfRange {
        id: u16,
        count: usize,
    },
    ClockOutOfRange(u64),
    ClockRewind {
        previous: u64,
        supplied: u64,
    },
    AllocationFailed,
}

impl fmt::Display for VariableError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnsupportedPacket(opcode) => write!(f, "unsupported variable packet {opcode:?}"),
            Self::PayloadLength {
                opcode,
                expected,
                actual,
            } => write!(f, "{opcode:?} payload length {actual}, expected {expected}"),
            Self::Payload(error) => error.fmt(f),
            Self::CountOutOfRange(count) => write!(f, "variable count {count} exceeds {MAX_VARPS}"),
            Self::IdOutOfRange { id, count } => {
                write!(f, "variable id {id} is outside definition span {count}")
            }
            Self::ClockOutOfRange(now) => {
                write!(f, "variable time {now} exceeds {MAX_VARIABLE_TIME_MS}")
            }
            Self::ClockRewind { previous, supplied } => {
                write!(f, "variable time rewound from {previous} to {supplied}")
            }
            Self::AllocationFailed => f.write_str("variable state allocation failed"),
        }
    }
}

impl std::error::Error for VariableError {}

impl From<PayloadError> for VariableError {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

pub fn decode_variable_packet(
    opcode: ServerPacket,
    payload: &[u8],
) -> Result<VariablePacket, VariableError> {
    let expected = match opcode {
        ServerPacket::VARP_SMALL => 3,
        ServerPacket::VARP_LARGE => 6,
        ServerPacket::CLEAR_VARPS => 0,
        _ => return Err(VariableError::UnsupportedPacket(opcode)),
    };
    if payload.len() != expected {
        return Err(VariableError::PayloadLength {
            opcode,
            expected,
            actual: payload.len(),
        });
    }
    let mut reader = PayloadReader::new(payload);
    Ok(match opcode {
        ServerPacket::VARP_SMALL => {
            let value = reader.read_i8()?;
            let id = reader.read_u16_le_a()?;
            VariablePacket::VarpSmall { id, value }
        }
        ServerPacket::VARP_LARGE => {
            let value = reader.read_i32_inverse_middle()?;
            let id = reader.read_u16_le_a()?;
            VariablePacket::VarpLarge { id, value }
        }
        ServerPacket::CLEAR_VARPS => VariablePacket::ClearVarps,
        _ => return Err(VariableError::UnsupportedPacket(opcode)),
    })
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Schedule {
    Immediate,
    Protected { deadline: u64, publish: bool },
}

impl Schedule {
    fn expired(self, now_ms: u64) -> bool {
        match self {
            Self::Immediate => IMMEDIATE_DEADLINE_MS < now_ms,
            Self::Protected { deadline, .. } => deadline < now_ms,
        }
    }

    fn publishes(self) -> bool {
        match self {
            Self::Immediate => true,
            Self::Protected { publish, .. } => publish,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct ScheduledUpdate {
    schedule: Schedule,
    next: Option<u16>,
}

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
struct VariableSlot {
    active: i32,
    pending: i32,
    update: Option<ScheduledUpdate>,
}

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
struct UpdateBucket {
    head: Option<u16>,
    tail: Option<u16>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct WorldVariables {
    slots: Vec<VariableSlot>,
    buckets: [UpdateBucket; UPDATE_BUCKETS],
    scheduled_count: usize,
    last_now_ms: Option<u64>,
}

impl WorldVariables {
    pub fn new(varp_count: usize) -> Result<Self, VariableError> {
        if varp_count > MAX_VARPS {
            return Err(VariableError::CountOutOfRange(varp_count));
        }
        let mut slots = Vec::new();
        slots
            .try_reserve_exact(varp_count)
            .map_err(|_| VariableError::AllocationFailed)?;
        slots.resize(varp_count, VariableSlot::default());
        Ok(Self {
            slots,
            buckets: [UpdateBucket::default(); UPDATE_BUCKETS],
            scheduled_count: 0,
            last_now_ms: None,
        })
    }

    pub fn count(&self) -> usize {
        self.slots.len()
    }

    pub fn scheduled_count(&self) -> usize {
        self.scheduled_count
    }

    pub fn non_zero_count(&self) -> usize {
        self.slots.iter().filter(|slot| slot.active != 0).count()
    }

    pub fn active(&self, id: u16) -> Result<i32, VariableError> {
        self.validate_id(id)?;
        Ok(self.slots[usize::from(id)].active)
    }

    pub fn pending(&self, id: u16) -> Result<i32, VariableError> {
        self.validate_id(id)?;
        Ok(self.slots[usize::from(id)].pending)
    }

    pub fn apply_payload(
        &mut self,
        opcode: ServerPacket,
        payload: &[u8],
        now_ms: u64,
    ) -> Result<VariableEffect, VariableError> {
        self.apply(decode_variable_packet(opcode, payload)?, now_ms)
    }

    pub fn apply(
        &mut self,
        packet: VariablePacket,
        now_ms: u64,
    ) -> Result<VariableEffect, VariableError> {
        let assignment = match packet {
            VariablePacket::VarpSmall { id, value } => Some((id, i32::from(value))),
            VariablePacket::VarpLarge { id, value } => Some((id, value)),
            VariablePacket::ClearVarps => None,
        };
        if let Some((id, _)) = assignment {
            self.validate_id(id)?;
        }
        self.validate_time(now_ms)?;
        self.last_now_ms = Some(now_ms);
        if let Some((id, value)) = assignment {
            let slot = &mut self.slots[usize::from(id)];
            slot.pending = value;
            if let Some(update) = &mut slot.update {
                if update.schedule != Schedule::Immediate {
                    update.schedule = Schedule::Protected {
                        deadline: now_ms + LOCAL_OVERRIDE_MS,
                        publish: true,
                    };
                }
            } else {
                self.schedule(id, Schedule::Immediate);
            }
            Ok(VariableEffect::Pending { id, value })
        } else {
            self.slots.fill(VariableSlot::default());
            self.buckets.fill(UpdateBucket::default());
            self.scheduled_count = 0;
            Ok(VariableEffect::Cleared)
        }
    }

    pub fn set_local(
        &mut self,
        id: u16,
        value: i32,
        now_ms: u64,
    ) -> Result<VariableChange, VariableError> {
        self.validate_id(id)?;
        self.validate_time(now_ms)?;
        self.last_now_ms = Some(now_ms);
        let slot = &mut self.slots[usize::from(id)];
        let previous = slot.active;
        slot.active = value;
        let schedule = Schedule::Protected {
            deadline: now_ms + LOCAL_OVERRIDE_MS,
            publish: false,
        };
        if let Some(update) = &mut slot.update {
            update.schedule = schedule;
        } else {
            self.schedule(id, schedule);
        }
        Ok(VariableChange {
            id,
            previous,
            value,
        })
    }

    pub fn service(&mut self, now_ms: u64) -> Result<Vec<VariableChange>, VariableError> {
        self.validate_time(now_ms)?;
        let mut changes = Vec::new();
        changes
            .try_reserve_exact(self.scheduled_count)
            .map_err(|_| VariableError::AllocationFailed)?;
        self.last_now_ms = Some(now_ms);
        for bucket_index in 0..UPDATE_BUCKETS {
            let mut previous: Option<u16> = None;
            let mut current = self.buckets[bucket_index].head;
            while let Some(id) = current {
                let slot = &mut self.slots[usize::from(id)];
                let update = slot.update.unwrap();
                current = update.next;
                if !update.schedule.expired(now_ms) {
                    previous = Some(id);
                    continue;
                }
                if update.schedule.publishes() {
                    changes.push(VariableChange {
                        id,
                        previous: slot.active,
                        value: slot.pending,
                    });
                    slot.active = slot.pending;
                }
                slot.update = None;
                if let Some(previous) = previous {
                    self.slots[usize::from(previous)]
                        .update
                        .as_mut()
                        .unwrap()
                        .next = current;
                } else {
                    self.buckets[bucket_index].head = current;
                }
                if current.is_none() {
                    self.buckets[bucket_index].tail = previous;
                }
                self.scheduled_count -= 1;
            }
        }
        Ok(changes)
    }

    fn validate_id(&self, id: u16) -> Result<(), VariableError> {
        if usize::from(id) >= self.slots.len() {
            Err(VariableError::IdOutOfRange {
                id,
                count: self.slots.len(),
            })
        } else {
            Ok(())
        }
    }

    fn validate_time(&self, now_ms: u64) -> Result<(), VariableError> {
        if now_ms > MAX_VARIABLE_TIME_MS {
            return Err(VariableError::ClockOutOfRange(now_ms));
        }
        if let Some(previous) = self.last_now_ms
            && now_ms < previous
        {
            return Err(VariableError::ClockRewind {
                previous,
                supplied: now_ms,
            });
        }
        Ok(())
    }

    fn schedule(&mut self, id: u16, schedule: Schedule) {
        let bucket = &mut self.buckets[usize::from(id) & (UPDATE_BUCKETS - 1)];
        self.slots[usize::from(id)].update = Some(ScheduledUpdate {
            schedule,
            next: None,
        });
        if let Some(tail) = bucket.tail {
            self.slots[usize::from(tail)].update.as_mut().unwrap().next = Some(id);
        } else {
            bucket.head = Some(id);
        }
        bucket.tail = Some(id);
        self.scheduled_count += 1;
    }
}

pub struct WorldVarHost<'a> {
    variables: &'a mut WorldVariables,
    now_ms: u64,
    varbits: Option<&'a varbit::VarbitRepository>,
    config: config::ConfigBinding<'a>,
    player: player::PlayerBinding<'a>,
    world: world::WorldBinding<'a>,
    social: social::SocialBinding<'a>,
    clock: clock::ClockBinding<'a>,
    quick_chat: Option<&'a openrs_cs2::QuickChatDefs>,
    quest_defs: Option<&'a openrs_cs2::QuestDefs>,
    lobby_state: Option<&'a openrs_cs2::LobbyState>,
    outbox: Option<&'a mut openrs_cs2::Outbox>,
    client_state: Option<&'a mut openrs_cs2::ClientState>,
    entity_state: Option<&'a mut openrs_cs2::EntityState>,
}

impl<'a> WorldVarHost<'a> {
    pub fn with_quest_defs(mut self, defs: &'a openrs_cs2::QuestDefs) -> Self {
        self.quest_defs = Some(defs);
        self
    }

    pub fn with_lobby_state(mut self, state: &'a openrs_cs2::LobbyState) -> Self {
        self.lobby_state = Some(state);
        self
    }

    pub fn with_outbox(mut self, outbox: &'a mut openrs_cs2::Outbox) -> Self {
        self.outbox = Some(outbox);
        self
    }

    pub fn with_entity_state(mut self, state: &'a mut openrs_cs2::EntityState) -> Self {
        self.entity_state = Some(state);
        self
    }

    pub fn with_client_state(mut self, state: &'a mut openrs_cs2::ClientState) -> Self {
        self.client_state = Some(state);
        self
    }

    pub fn with_quick_chat(mut self, defs: &'a openrs_cs2::QuickChatDefs) -> Self {
        self.quick_chat = Some(defs);
        self
    }

    pub fn new(variables: &'a mut WorldVariables, now_ms: u64) -> Self {
        Self {
            variables,
            now_ms,
            varbits: None,
            config: Default::default(),
            player: Default::default(),
            world: Default::default(),
            social: Default::default(),
            clock: Default::default(),
            quick_chat: None,
            quest_defs: None,
            lobby_state: None,
            outbox: None,
            client_state: None,
            entity_state: None,
        }
    }

    pub fn with_varbits(
        variables: &'a mut WorldVariables,
        now_ms: u64,
        varbits: &'a varbit::VarbitRepository,
    ) -> Self {
        Self {
            variables,
            now_ms,
            varbits: Some(varbits),
            config: Default::default(),
            player: Default::default(),
            world: Default::default(),
            social: Default::default(),
            clock: Default::default(),
            quick_chat: None,
            quest_defs: None,
            lobby_state: None,
            outbox: None,
            client_state: None,
            entity_state: None,
        }
    }

    fn id(&self, id: i32) -> Result<u16, openrs_cs2::HostError> {
        u16::try_from(id)
            .ok()
            .filter(|id| usize::from(*id) < self.variables.count())
            .ok_or(openrs_cs2::HostError::InvalidVarp(id))
    }
}

impl openrs_cs2::VarHost for WorldVarHost<'_> {
    fn varp(&self, id: i32) -> Result<i32, openrs_cs2::HostError> {
        self.variables
            .active(self.id(id)?)
            .map_err(|_| openrs_cs2::HostError::InvalidVarp(id))
    }

    fn set_varp(&mut self, id: i32, value: i32) -> Result<(), openrs_cs2::HostError> {
        self.variables
            .set_local(self.id(id)?, value, self.now_ms)
            .map(|_| ())
            .map_err(|_| openrs_cs2::HostError::State("invalid world variable clock".into()))
    }

    fn varbit(&self, id: i32) -> Result<openrs_cs2::VarbitLookup, openrs_cs2::HostError> {
        if id < 0 {
            Err(openrs_cs2::HostError::InvalidVarbit(id))
        } else if let Some(varbits) = self.varbits {
            varbits.lookup(id)
        } else {
            Err(openrs_cs2::HostError::Unavailable)
        }
    }

    fn items(&mut self) -> Result<&mut openrs_cs2::ItemModel, openrs_cs2::HostError> {
        self.config.items()
    }

    fn item_definition(
        &self,
        id: i32,
    ) -> Result<Option<std::sync::Arc<openrs_cs2::ItemDefinition>>, openrs_cs2::HostError> {
        self.config.item_definition(id)
    }

    fn param_definition(
        &self,
        id: i32,
    ) -> Result<Option<std::sync::Arc<openrs_cs2::ParamDefinition>>, openrs_cs2::HostError> {
        self.config.param_definition(id)
    }

    fn config_params(
        &self,
        kind: openrs_cs2::ConfigKind,
        id: i32,
    ) -> Result<Option<std::sync::Arc<openrs_cs2::ParamTable>>, openrs_cs2::HostError> {
        self.config.config_params(kind, id)
    }

    fn interaction_colours(&self) -> Result<openrs_cs2::InteractionColours, openrs_cs2::HostError> {
        self.config.interaction_colours()
    }

    fn quick_chat(&self) -> Result<&openrs_cs2::QuickChatDefs, openrs_cs2::HostError> {
        self.quick_chat.ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn quest_defs(&self) -> Result<&openrs_cs2::QuestDefs, openrs_cs2::HostError> {
        self.quest_defs.ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn lobby_state(&self) -> Result<&openrs_cs2::LobbyState, openrs_cs2::HostError> {
        self.lobby_state.ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn client(&mut self) -> Result<&mut openrs_cs2::ClientState, openrs_cs2::HostError> {
        self.client_state
            .as_deref_mut()
            .ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn entity_state(&self) -> Result<&openrs_cs2::EntityState, openrs_cs2::HostError> {
        self.entity_state
            .as_deref()
            .ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn entity_state_mut(&mut self) -> Result<&mut openrs_cs2::EntityState, openrs_cs2::HostError> {
        self.entity_state
            .as_deref_mut()
            .ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn outbox(&mut self) -> Result<&mut openrs_cs2::Outbox, openrs_cs2::HostError> {
        self.outbox
            .as_deref_mut()
            .ok_or(openrs_cs2::HostError::Unavailable)
    }

    fn social_state(&self) -> Result<&openrs_cs2::SocialState, openrs_cs2::HostError> {
        self.social.social_state()
    }

    fn social_state_mut(&mut self) -> Result<&mut openrs_cs2::SocialState, openrs_cs2::HostError> {
        self.social.social_state_mut()
    }

    fn clan_state(&self) -> Result<&openrs_cs2::ClanState, openrs_cs2::HostError> {
        self.social.clan_state()
    }

    fn clan_var_defs(&self) -> Result<&openrs_cs2::ClanVarDefs, openrs_cs2::HostError> {
        self.social.clan_var_defs()
    }

    fn chat_state(&self) -> Result<&openrs_cs2::ChatState, openrs_cs2::HostError> {
        self.social.chat_state()
    }

    fn chat_state_mut(&mut self) -> Result<&mut openrs_cs2::ChatState, openrs_cs2::HostError> {
        self.social.chat_state_mut()
    }

    fn player_state(&self) -> Result<&openrs_cs2::PlayerState, openrs_cs2::HostError> {
        self.player.player_state()
    }

    fn npc_state(&self) -> Result<&openrs_cs2::NpcState, openrs_cs2::HostError> {
        self.player.npc_state()
    }

    fn clock_millis(&self) -> Result<i64, openrs_cs2::HostError> {
        self.clock.clock_millis()
    }

    fn random_unit(&mut self) -> Result<f64, openrs_cs2::HostError> {
        self.clock.random_unit()
    }

    fn local_offset_millis(&self, _utc_millis: i64) -> Result<i64, openrs_cs2::HostError> {
        self.clock.local_offset_millis()
    }

    fn language(&self) -> Result<Option<i32>, openrs_cs2::HostError> {
        self.clock.language()
    }

    fn hsv_to_rgb_table(&self) -> Result<Option<std::sync::Arc<[i32]>>, openrs_cs2::HostError> {
        self.clock.hsv_to_rgb_table()
    }

    fn world_state(&mut self) -> Result<&mut openrs_cs2::WorldState, openrs_cs2::HostError> {
        self.world.world_state()
    }

    fn appearance_state_mut(
        &mut self,
    ) -> Result<&mut openrs_cs2::AppearanceState, openrs_cs2::HostError> {
        self.world.appearance_state_mut()
    }
}

impl Drop for WorldVariables {
    fn drop(&mut self) {
        for slot in &mut self.slots {
            slot.active.zeroize();
            slot.pending.zeroize();
            slot.update = None;
        }
        self.buckets.fill(UpdateBucket::default());
        self.scheduled_count = 0;
        self.last_now_ms = None;
    }
}

#[cfg(test)]
mod tests;
