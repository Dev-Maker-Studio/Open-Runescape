use std::{fmt, sync::Arc};

pub const VARC_SLOT_CAPACITY: usize = 65_536;
pub const VARC_DEBOUNCE_MS: u64 = 500;
pub const MAX_VARC_TIME_MS: u64 = i64::MAX as u64;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct VarcLimits {
    pub max_string_bytes: usize,
    pub max_total_string_bytes: usize,
}

impl Default for VarcLimits {
    fn default() -> Self {
        Self {
            max_string_bytes: 1024 * 1024,
            max_total_string_bytes: 16 * 1024 * 1024,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum VarcError {
    SlotLimit,
    PersistenceCount,
    InvalidIntId(i32),
    InvalidStringId(i32),
    StringLimit,
    StorageLimit,
    ClockOutOfRange(u64),
    ClockRewind { previous: u64, supplied: u64 },
    SequenceExhausted,
}

impl fmt::Display for VarcError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "CS2 client variables: {self:?}")
    }
}

impl std::error::Error for VarcError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum VarcPulseKind {
    Int,
    String,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct VarcPulse {
    pub kind: VarcPulseKind,
    pub id: u16,
    pub due_ms: u64,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
struct ScheduledPulse {
    due_ms: u64,
    sequence: u64,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct VarcState {
    ints: Vec<i32>,
    strings: Vec<Option<Arc<str>>>,
    persistent: Vec<bool>,
    int_pulses: Vec<Option<ScheduledPulse>>,
    string_pulses: Vec<Option<ScheduledPulse>>,
    limits: VarcLimits,
    string_bytes: usize,
    dirty: bool,
    now_ms: u64,
    sequence: u64,
}

impl VarcState {
    pub fn new(
        persistent: &[bool],
        string_count: usize,
        limits: VarcLimits,
    ) -> Result<Self, VarcError> {
        if persistent.len() > VARC_SLOT_CAPACITY || string_count > VARC_SLOT_CAPACITY {
            return Err(VarcError::SlotLimit);
        }
        Ok(Self {
            ints: vec![-1; persistent.len()],
            strings: vec![None; string_count],
            persistent: persistent.to_vec(),
            int_pulses: vec![None; persistent.len()],
            string_pulses: vec![None; string_count],
            limits,
            string_bytes: 0,
            dirty: false,
            now_ms: 0,
            sequence: 0,
        })
    }

    pub fn from_values(
        persistent: &[bool],
        ints: &[i32],
        strings: &[Option<Arc<str>>],
        limits: VarcLimits,
    ) -> Result<Self, VarcError> {
        if persistent.len() != ints.len() {
            return Err(VarcError::PersistenceCount);
        }
        if persistent.len() > VARC_SLOT_CAPACITY || strings.len() > VARC_SLOT_CAPACITY {
            return Err(VarcError::SlotLimit);
        }
        let mut string_bytes = 0usize;
        for value in strings {
            let bytes = value.as_ref().map_or(0, |s| s.len());
            if bytes > limits.max_string_bytes {
                return Err(VarcError::StringLimit);
            }
            string_bytes = string_bytes
                .checked_add(bytes)
                .filter(|total| *total <= limits.max_total_string_bytes)
                .ok_or(VarcError::StorageLimit)?;
        }
        let mut state = Self::new(persistent, strings.len(), limits)?;
        state.ints.copy_from_slice(ints);
        state.strings.clone_from_slice(strings);
        state.string_bytes = string_bytes;
        Ok(state)
    }

    pub fn int_count(&self) -> usize {
        self.ints.len()
    }

    pub fn string_count(&self) -> usize {
        self.strings.len()
    }

    pub fn string_bytes(&self) -> usize {
        self.string_bytes
    }

    pub fn dirty(&self) -> bool {
        self.dirty
    }

    pub fn clear_dirty(&mut self) {
        self.dirty = false;
    }

    pub fn now_ms(&self) -> u64 {
        self.now_ms
    }

    pub fn set_time(&mut self, now_ms: u64) -> Result<(), VarcError> {
        if now_ms > MAX_VARC_TIME_MS {
            return Err(VarcError::ClockOutOfRange(now_ms));
        }
        if now_ms < self.now_ms {
            return Err(VarcError::ClockRewind {
                previous: self.now_ms,
                supplied: now_ms,
            });
        }
        self.now_ms = now_ms;
        Ok(())
    }

    pub fn int(&self, id: i32) -> Result<i32, VarcError> {
        Ok(self.ints[self.int_id(id)?])
    }

    pub fn string(&self, id: i32) -> Result<Option<Arc<str>>, VarcError> {
        Ok(self.strings[self.string_id(id)?].clone())
    }

    pub fn set_int(&mut self, id: i32, value: i32) -> Result<(), VarcError> {
        let slot = self.int_id(id)?;
        let pulse = self.next_pulse()?;
        self.ints[slot] = value;
        self.dirty |= self.persistent[slot];
        self.int_pulses[slot] = Some(pulse);
        self.sequence = pulse.sequence;
        Ok(())
    }

    pub fn set_string(&mut self, id: i32, value: Option<Arc<str>>) -> Result<(), VarcError> {
        let slot = self.string_id(id)?;
        let bytes = value.as_ref().map_or(0, |s| s.len());
        if bytes > self.limits.max_string_bytes {
            return Err(VarcError::StringLimit);
        }
        let total = self
            .string_bytes
            .checked_sub(self.strings[slot].as_ref().map_or(0, |s| s.len()))
            .and_then(|previous| previous.checked_add(bytes))
            .filter(|total| *total <= self.limits.max_total_string_bytes)
            .ok_or(VarcError::StorageLimit)?;
        let pulse = self.next_pulse()?;
        self.strings[slot] = value;
        self.string_bytes = total;
        self.string_pulses[slot] = Some(pulse);
        self.sequence = pulse.sequence;
        Ok(())
    }

    pub fn pending_pulses(&self) -> Vec<VarcPulse> {
        self.ordered_pulses(false)
    }

    pub fn take_due_pulses(&mut self) -> Vec<VarcPulse> {
        let pulses = self.ordered_pulses(true);
        for pulse in &pulses {
            let slots = match pulse.kind {
                VarcPulseKind::Int => &mut self.int_pulses,
                VarcPulseKind::String => &mut self.string_pulses,
            };
            slots[usize::from(pulse.id)] = None;
        }
        pulses
    }

    fn ordered_pulses(&self, due_only: bool) -> Vec<VarcPulse> {
        let mut pulses = Vec::new();
        for (kind, slots) in [
            (VarcPulseKind::Int, &self.int_pulses),
            (VarcPulseKind::String, &self.string_pulses),
        ] {
            for (id, pulse) in slots.iter().enumerate() {
                if let Some(pulse) = pulse {
                    if !due_only || pulse.due_ms <= self.now_ms {
                        pulses.push((
                            pulse.sequence,
                            VarcPulse {
                                kind,
                                id: id as u16,
                                due_ms: pulse.due_ms,
                            },
                        ));
                    }
                }
            }
        }
        pulses.sort_unstable_by_key(|(sequence, _)| *sequence);
        pulses.into_iter().map(|(_, pulse)| pulse).collect()
    }

    fn next_pulse(&self) -> Result<ScheduledPulse, VarcError> {
        Ok(ScheduledPulse {
            due_ms: self
                .now_ms
                .checked_add(VARC_DEBOUNCE_MS)
                .filter(|due| *due <= MAX_VARC_TIME_MS)
                .ok_or(VarcError::ClockOutOfRange(self.now_ms))?,
            sequence: self
                .sequence
                .checked_add(1)
                .ok_or(VarcError::SequenceExhausted)?,
        })
    }

    fn int_id(&self, id: i32) -> Result<usize, VarcError> {
        usize::try_from(id)
            .ok()
            .filter(|slot| *slot < self.ints.len())
            .ok_or(VarcError::InvalidIntId(id))
    }

    fn string_id(&self, id: i32) -> Result<usize, VarcError> {
        usize::try_from(id)
            .ok()
            .filter(|slot| *slot < self.strings.len())
            .ok_or(VarcError::InvalidStringId(id))
    }
}

impl crate::VarHost for VarcState {
    fn varp(&self, _id: i32) -> Result<i32, crate::HostError> {
        Err(crate::HostError::Unavailable)
    }

    fn set_varp(&mut self, _id: i32, _value: i32) -> Result<(), crate::HostError> {
        Err(crate::HostError::Unavailable)
    }

    fn varbit(&self, _id: i32) -> Result<crate::VarbitLookup, crate::HostError> {
        Err(crate::HostError::Unavailable)
    }

    fn varcs(&mut self) -> Result<&mut VarcState, crate::HostError> {
        Ok(self)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn sequence_exhaustion_is_transactional() {
        let mut state = VarcState::new(&[true], 1, VarcLimits::default()).unwrap();
        state.sequence = u64::MAX;
        let previous = state.clone();
        assert_eq!(state.set_int(0, 1), Err(VarcError::SequenceExhausted));
        assert_eq!(state, previous);
        assert_eq!(state.set_string(0, None), Err(VarcError::SequenceExhausted));
        assert_eq!(state, previous);
    }
}
