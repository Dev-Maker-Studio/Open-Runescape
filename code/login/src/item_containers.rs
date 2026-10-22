use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::fmt;

pub const MAX_PACKET_BYTES: usize = u16::MAX as usize;
pub const MAX_CONTAINER_SLOTS: usize = u16::MAX as usize;
pub const MAX_CONTAINERS: usize = 1024;
pub const MAX_TOTAL_SLOTS: usize = 1 << 20;
pub const MAX_TOOLTIP_BYTES: usize = 8 << 20;
pub const RECENT_CONTAINER_COUNT: usize = 32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Limits {
    pub containers: usize,
    pub slots_per_container: usize,
    pub total_slots: usize,
    pub tooltip_bytes: usize,
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            containers: 128,
            slots_per_container: MAX_CONTAINER_SLOTS,
            total_slots: 1 << 18,
            tooltip_bytes: 1 << 20,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    Payload(PayloadError),
    UnsupportedOpcode(ServerPacket),
    InvalidLimits,
    LengthLimitExceeded { length: usize, limit: usize },
    SlotLimitExceeded { length: usize, limit: usize },
    ContainerLimitExceeded { limit: usize },
    TotalSlotLimitExceeded { length: usize, limit: usize },
    TooltipLimitExceeded { length: usize, limit: usize },
    TrailingData(usize),
    AllocationFailed,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Payload(error) => error.fmt(f),
            Self::UnsupportedOpcode(opcode) => {
                write!(f, "unsupported item-container opcode {opcode:?}")
            }
            Self::InvalidLimits => f.write_str("invalid item-container limits"),
            Self::LengthLimitExceeded { length, limit } => write!(
                f,
                "item-container payload has {length} bytes, limit is {limit}"
            ),
            Self::SlotLimitExceeded { length, limit } => {
                write!(f, "item container needs {length} slots, limit is {limit}")
            }
            Self::ContainerLimitExceeded { limit } => {
                write!(f, "item-container count exceeds {limit}")
            }
            Self::TotalSlotLimitExceeded { length, limit } => {
                write!(f, "item containers need {length} slots, limit is {limit}")
            }
            Self::TooltipLimitExceeded { length, limit } => write!(
                f,
                "item-container tooltips need {length} UTF-8 bytes, limit is {limit}"
            ),
            Self::TrailingData(remaining) => {
                write!(f, "item-container payload has {remaining} trailing bytes")
            }
            Self::AllocationFailed => f.write_str("item-container allocation failed"),
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

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct ContainerKey {
    pub id: u16,
    pub negative_key: bool,
}

impl ContainerKey {
    pub fn native_key(self) -> i64 {
        i64::from(self.id)
            | if self.negative_key {
                i64::from(i32::MIN)
            } else {
                0
            }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ItemSlot {
    pub item_id: i32,
    pub amount: i32,
    pub tooltip: Option<String>,
}

impl Default for ItemSlot {
    fn default() -> Self {
        Self {
            item_id: -1,
            amount: 0,
            tooltip: None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Container {
    key: ContainerKey,
    slots: Vec<ItemSlot>,
    tooltip_bytes: usize,
}

impl Container {
    pub fn key(&self) -> ContainerKey {
        self.key
    }

    pub fn slots(&self) -> &[ItemSlot] {
        &self.slots
    }

    pub fn len(&self) -> usize {
        self.slots.len()
    }

    pub fn is_empty(&self) -> bool {
        self.slots.is_empty()
    }

    pub fn item_id(&self, slot: usize) -> i32 {
        self.slots.get(slot).map_or(-1, |slot| slot.item_id)
    }

    pub fn amount(&self, slot: usize) -> i32 {
        self.slots.get(slot).map_or(0, |slot| slot.amount)
    }

    pub fn tooltip(&self, slot: usize) -> Option<&str> {
        self.slots
            .get(slot)
            .and_then(|slot| slot.tooltip.as_deref())
    }

    fn empty(key: ContainerKey) -> Self {
        Self {
            key,
            slots: Vec::new(),
            tooltip_bytes: 0,
        }
    }

    fn grow(&mut self, length: usize) -> Result<(), Error> {
        if length > self.len() {
            if length > self.slots.capacity() {
                let capacity = length
                    .max(self.slots.capacity().saturating_mul(2))
                    .min(MAX_CONTAINER_SLOTS);
                self.slots
                    .try_reserve_exact(capacity - self.len())
                    .map_err(|_| Error::AllocationFailed)?;
            }
            self.slots.resize_with(length, ItemSlot::default);
        }
        Ok(())
    }

    fn copy_existing(&mut self, existing: &Self) -> Result<(), Error> {
        self.grow(existing.len())?;
        for (target, source) in self.slots.iter_mut().zip(&existing.slots) {
            target.item_id = source.item_id;
            target.amount = source.amount;
            if let Some(tooltip) = &source.tooltip {
                let mut text = String::new();
                text.try_reserve_exact(tooltip.len())
                    .map_err(|_| Error::AllocationFailed)?;
                text.push_str(tooltip);
                target.tooltip = Some(text);
            }
        }
        self.tooltip_bytes = existing.tooltip_bytes;
        Ok(())
    }

    fn set_tooltip(&mut self, slot: usize, tooltip: Option<String>) {
        if let Some(target) = self.slots.get_mut(slot) {
            self.tooltip_bytes = self.tooltip_bytes
                - target.tooltip.as_ref().map_or(0, String::len)
                + tooltip.as_ref().map_or(0, String::len);
            target.tooltip = tooltip;
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum UpdateKind {
    Full,
    Partial,
    StopTransmit,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Update {
    pub key: ContainerKey,
    pub kind: UpdateKind,
    pub record_count: usize,
    pub slot_count: usize,
    pub container_present: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ItemContainers {
    limits: Limits,
    containers: Vec<Container>,
    total_slots: usize,
    tooltip_bytes: usize,
    recent: [u16; RECENT_CONTAINER_COUNT],
    recent_cursor: i32,
}

impl ItemContainers {
    pub fn new(limits: Limits) -> Result<Self, Error> {
        if !(1..=MAX_CONTAINERS).contains(&limits.containers)
            || !(1..=MAX_CONTAINER_SLOTS).contains(&limits.slots_per_container)
            || !(1..=MAX_TOTAL_SLOTS).contains(&limits.total_slots)
            || limits.tooltip_bytes > MAX_TOOLTIP_BYTES
        {
            return Err(Error::InvalidLimits);
        }
        Ok(Self {
            limits,
            containers: Vec::new(),
            total_slots: 0,
            tooltip_bytes: 0,
            recent: [0; RECENT_CONTAINER_COUNT],
            recent_cursor: 0,
        })
    }

    pub fn supports(opcode: ServerPacket) -> bool {
        matches!(
            opcode,
            ServerPacket::UPDATE_INV_FULL
                | ServerPacket::UPDATE_INV_PARTIAL
                | ServerPacket::UPDATE_INV_STOP_TRANSMIT
        )
    }

    pub fn limits(&self) -> Limits {
        self.limits
    }

    pub fn len(&self) -> usize {
        self.containers.len()
    }

    pub fn is_empty(&self) -> bool {
        self.containers.is_empty()
    }

    pub fn total_slots(&self) -> usize {
        self.total_slots
    }

    pub fn tooltip_bytes(&self) -> usize {
        self.tooltip_bytes
    }

    pub fn recently_used(&self) -> &[u16; RECENT_CONTAINER_COUNT] {
        &self.recent
    }

    pub fn recent_cursor(&self) -> i32 {
        self.recent_cursor
    }

    pub fn containers(&self) -> &[Container] {
        &self.containers
    }

    pub fn get(&self, key: ContainerKey) -> Option<&Container> {
        self.index(key).ok().map(|index| &self.containers[index])
    }

    fn index(&self, key: ContainerKey) -> Result<usize, usize> {
        self.containers.binary_search_by_key(&key, Container::key)
    }

    fn check_size(&self, length: usize, previous: usize, existed: bool) -> Result<(), Error> {
        if length > self.limits.slots_per_container {
            return Err(Error::SlotLimitExceeded {
                length,
                limit: self.limits.slots_per_container,
            });
        }
        if length != 0 && !existed && self.len() == self.limits.containers {
            return Err(Error::ContainerLimitExceeded {
                limit: self.limits.containers,
            });
        }
        let total = self.total_slots - previous + length;
        if total > self.limits.total_slots {
            return Err(Error::TotalSlotLimitExceeded {
                length: total,
                limit: self.limits.total_slots,
            });
        }
        Ok(())
    }

    fn record(&mut self, id: u16) {
        self.recent[self.recent_cursor as u32 as usize & (RECENT_CONTAINER_COUNT - 1)] = id;
        self.recent_cursor = self.recent_cursor.wrapping_add(1);
    }

    pub fn apply(&mut self, opcode: ServerPacket, payload: &[u8]) -> Result<Update, Error> {
        if !Self::supports(opcode) {
            return Err(Error::UnsupportedOpcode(opcode));
        }
        if payload.len() > MAX_PACKET_BYTES {
            return Err(Error::LengthLimitExceeded {
                length: payload.len(),
                limit: MAX_PACKET_BYTES,
            });
        }
        let mut reader = PayloadReader::new(payload);
        if opcode == ServerPacket::UPDATE_INV_STOP_TRANSMIT {
            let negative_key = reader.read_u8_c()? & 1 != 0;
            let key = ContainerKey {
                id: reader.read_u16_le()?,
                negative_key,
            };
            finish(&reader)?;
            if let Ok(index) = self.index(key) {
                let removed = self.containers.remove(index);
                self.total_slots -= removed.len();
                self.tooltip_bytes -= removed.tooltip_bytes;
            }
            self.record(key.id);
            return Ok(Update {
                key,
                kind: UpdateKind::StopTransmit,
                record_count: 0,
                slot_count: 0,
                container_present: false,
            });
        }
        let id = reader.read_u16_be()?;
        let flags = reader.read_u8()?;
        let key = ContainerKey {
            id,
            negative_key: flags & 1 != 0,
        };
        let existing = self.get(key);
        let previous_slots = existing.map_or(0, Container::len);
        let previous_tooltips = existing.map_or(0, |container| container.tooltip_bytes);
        let mut candidate = Container::empty(key);
        let (kind, record_count) = if opcode == ServerPacket::UPDATE_INV_FULL {
            let size = usize::from(reader.read_u16_be()?);
            self.check_size(size.max(previous_slots), previous_slots, existing.is_some())?;
            if size * 3 > reader.remaining() {
                return Err(PayloadError::UnexpectedEof.into());
            }
            candidate.grow(size.max(previous_slots))?;
            for slot in 0..size {
                let amount = match reader.read_u8_s()? {
                    255 => reader.read_i32_inverse_middle()?,
                    amount => i32::from(amount),
                };
                candidate.slots[slot].item_id = i32::from(reader.read_u16_le_a()?) - 1;
                candidate.slots[slot].amount = amount;
            }
            full_tooltips(&mut reader, &mut candidate, size, flags)?;
            (UpdateKind::Full, size)
        } else {
            if let Some(existing) = existing {
                candidate.copy_existing(existing)?;
            }
            let mut records = 0;
            while reader.remaining() != 0 {
                let header = reader.read_u_smart()?;
                let slot = usize::from(if flags & 2 != 0 { header >> 2 } else { header });
                let encoded_id = reader.read_u16_be()?;
                let amount = if encoded_id == 0 {
                    0
                } else {
                    match reader.read_u8()? {
                        255 => reader.read_i32_be()?,
                        amount => i32::from(amount),
                    }
                };
                let length = candidate.len().max(slot + 1);
                self.check_size(length, previous_slots, existing.is_some())?;
                candidate.grow(length)?;
                candidate.slots[slot].item_id = i32::from(encoded_id) - 1;
                candidate.slots[slot].amount = amount;
                match if flags & 2 != 0 { header & 3 } else { 1 } {
                    1 => candidate.set_tooltip(slot, None),
                    2 => candidate.set_tooltip(slot, Some(reader.read_cp1252()?)),
                    _ => {}
                }
                records += 1;
            }
            (UpdateKind::Partial, records)
        };
        finish(&reader)?;
        let final_tooltip_bytes = self.tooltip_bytes - previous_tooltips + candidate.tooltip_bytes;
        if final_tooltip_bytes > self.limits.tooltip_bytes {
            return Err(Error::TooltipLimitExceeded {
                length: final_tooltip_bytes,
                limit: self.limits.tooltip_bytes,
            });
        }
        let update = Update {
            key,
            kind,
            record_count,
            slot_count: candidate.len(),
            container_present: !candidate.is_empty(),
        };
        if !candidate.is_empty() {
            let candidate_tooltip_bytes = candidate.tooltip_bytes;
            match self.index(key) {
                Ok(index) => self.containers[index] = candidate,
                Err(index) => {
                    self.containers
                        .try_reserve_exact(1)
                        .map_err(|_| Error::AllocationFailed)?;
                    self.containers.insert(index, candidate);
                }
            }
            self.tooltip_bytes = self.tooltip_bytes - previous_tooltips + candidate_tooltip_bytes;
            self.total_slots = self.total_slots - previous_slots + update.slot_count;
        }
        self.record(key.id);
        Ok(update)
    }
}

fn finish(reader: &PayloadReader<'_>) -> Result<(), Error> {
    if reader.remaining() != 0 {
        return Err(Error::TrailingData(reader.remaining()));
    }
    Ok(())
}

fn full_tooltips(
    reader: &mut PayloadReader<'_>,
    candidate: &mut Container,
    size: usize,
    flags: u8,
) -> Result<(), Error> {
    if flags & 2 != 0 {
        let mask = reader.read_bytes(size.div_ceil(8))?;
        for slot in 0..size {
            if mask[slot >> 3] & (1 << (slot & 7)) != 0 {
                candidate.set_tooltip(slot, Some(reader.read_cp1252()?));
            }
        }
    } else if flags & 4 != 0 {
        for _ in 0..reader.read_u_smart()? {
            let slot = usize::from(reader.read_u_smart()?);
            candidate.set_tooltip(slot, Some(reader.read_cp1252()?));
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn native_recent_cursor_wraps_at_signed_int_boundary() {
        let mut state = ItemContainers::new(Limits::default()).unwrap();
        state.recent_cursor = i32::MAX;
        state
            .apply(ServerPacket::UPDATE_INV_PARTIAL, &[0x12, 0x34, 0])
            .unwrap();
        assert_eq!(state.recent_cursor(), i32::MIN);
        assert_eq!(state.recently_used()[31], 0x1234);
        state
            .apply(ServerPacket::UPDATE_INV_STOP_TRANSMIT, &[255, 0x78, 0x56])
            .unwrap();
        assert_eq!(state.recent_cursor(), i32::MIN + 1);
        assert_eq!(state.recently_used()[0], 0x5678);
    }
}
