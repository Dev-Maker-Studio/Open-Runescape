use super::{ComponentFailure, ComponentId, ComponentModel};
use std::{collections::BTreeMap, sync::Arc};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Cursor {
    Head(usize),
    Element(usize, usize),
}

pub(crate) enum OpenIdMiss {
    Null,
    Cast,
}

enum Walk {
    Missing,
    Head,
    Element(i32),
}

#[derive(Clone, Debug)]
pub(super) struct QueryState {
    names: BTreeMap<(i32, usize), Arc<str>>,
    triggers: BTreeMap<(i32, usize), i32>,
    pub(super) order: Vec<i32>,
    base_window: i32,
    game_screen: Option<ComponentId>,
}

impl Default for QueryState {
    fn default() -> Self {
        Self {
            names: BTreeMap::new(),
            triggers: BTreeMap::new(),
            order: Vec::new(),
            base_window: -1,
            game_screen: None,
        }
    }
}

impl QueryState {
    pub(super) fn opened(&mut self, hash: i32) {
        self.order.retain(|&known| known != hash);
        self.order.push(hash);
    }

    pub(super) fn closed(&mut self, hash: i32) {
        self.order.retain(|&known| known != hash);
    }
}

impl ComponentModel {
    pub fn set_component_name(&mut self, interface: i32, index: usize, name: Option<&str>) {
        match name {
            Some(name) => {
                self.query.names.insert((interface, index), Arc::from(name));
            }
            None => {
                self.query.names.remove(&(interface, index));
            }
        }
    }

    pub fn set_server_triggers(&mut self, interface: i32, index: usize, triggers: i32) {
        self.query.triggers.insert((interface, index), triggers);
    }

    pub fn set_base_window(&mut self, interface: i32) {
        self.query.base_window = interface;
    }

    pub fn set_game_screen(&mut self, component: Option<ComponentId>) {
        self.query.game_screen = component;
    }

    pub(crate) fn game_screen_hash(&self) -> i32 {
        self.query
            .game_screen
            .map_or(-1, |id| self.arena[id.0].id_hash)
    }

    pub(super) fn open_sequence(&self) -> Vec<i32> {
        let mut sequence = Vec::with_capacity(self.query.order.len());
        for bucket in 0..8 {
            sequence.extend(
                self.query
                    .order
                    .iter()
                    .copied()
                    .filter(|hash| hash & 7 == bucket),
            );
        }
        sequence
    }

    pub(crate) fn open_count(&self) -> i32 {
        self.open_interfaces.len() as i32 + i32::from(self.query.base_window != -1)
    }

    pub(crate) fn open_id_at(&self, index: i32) -> Result<i32, OpenIdMiss> {
        let mut index = index;
        if self.query.base_window != -1 {
            if index == 0 {
                return Ok(self.query.base_window);
            }
            index = index.wrapping_sub(1);
        }
        let steps = if index > 0 { index as u64 } else { 0 };
        match self.walk_open(steps) {
            Walk::Element(hash) => self
                .open_interfaces
                .get(&hash)
                .copied()
                .ok_or(OpenIdMiss::Null),
            Walk::Missing => Err(OpenIdMiss::Null),
            Walk::Head => Err(OpenIdMiss::Cast),
        }
    }

    fn walk_open(&self, steps: u64) -> Walk {
        let mut buckets: Vec<Vec<i32>> = vec![Vec::new(); 8];
        for &hash in &self.query.order {
            buckets[(hash & 7) as usize].push(hash);
        }
        let follow = |node: Cursor| -> Cursor {
            match node {
                Cursor::Head(bucket) => match buckets[bucket].first() {
                    Some(_) => Cursor::Element(bucket, 0),
                    None => Cursor::Head(bucket),
                },
                Cursor::Element(bucket, index) => {
                    if index + 1 < buckets[bucket].len() {
                        Cursor::Element(bucket, index + 1)
                    } else {
                        Cursor::Head(bucket)
                    }
                }
            }
        };
        let mut position = 0usize;
        let mut current: Option<Cursor> = None;
        let mut outputs: Vec<Walk> = Vec::new();
        let mut before: Vec<(usize, Option<Cursor>)> = Vec::new();
        let mut call = 0u64;
        loop {
            if call == 0 {
                position = 0;
            } else if let Some(seen) = before
                .iter()
                .position(|&state| state == (position, current))
            {
                let period = (call as usize - 1) - seen;
                let offset = (steps - call) % period as u64;
                return outputs.remove(seen + offset as usize + 1);
            }
            if call > 0 {
                before.push((position, current));
            }
            let output = 'call: {
                if position > 0 && current != Some(Cursor::Head(position - 1)) {
                    let Some(value) = current else {
                        break 'call Walk::Missing;
                    };
                    current = Some(follow(value));
                    break 'call match value {
                        Cursor::Head(_) => Walk::Head,
                        Cursor::Element(bucket, index) => Walk::Element(buckets[bucket][index]),
                    };
                }
                while position < 8 {
                    position += 1;
                    let head = Cursor::Head(position - 1);
                    let first = follow(head);
                    if first != head {
                        current = Some(follow(first));
                        let Cursor::Element(bucket, index) = first else {
                            unreachable!()
                        };
                        break 'call Walk::Element(buckets[bucket][index]);
                    }
                }
                Walk::Missing
            };
            if matches!(output, Walk::Head) || call == steps {
                return output;
            }
            outputs.push(output);
            call += 1;
        }
    }

    pub(crate) fn parent_layer(
        &self,
        interface: i32,
        id: ComponentId,
    ) -> Result<i32, ComponentFailure> {
        let component = &self.arena[id.0];
        let state = self.interface(interface)?;
        if component.parent_id != -1 {
            let found = self.lookup(state, component.parent_id)?;
            return Ok(found.map_or(-1, |found| self.arena[found.0].id_hash));
        }
        if !state.prevent_interaction {
            let wanted = ((component.id_hash as u32) >> 16) as i32;
            for hash in self.open_sequence() {
                if self.open_interfaces.get(&hash) == Some(&wanted) {
                    let target_interface = hash >> 16;
                    if target_interface < 0 || target_interface >= self.capacity {
                        return Ok(-1);
                    }
                    let Some(target) = self.interfaces.get(&target_interface) else {
                        return Err(ComponentFailure::NoComponent);
                    };
                    let found = self.lookup(target, hash)?;
                    return Ok(found.map_or(-1, |found| self.arena[found.0].id_hash));
                }
            }
        }
        Ok(-1)
    }

    fn debug_interface(
        &self,
        interface: i32,
    ) -> Result<Option<&super::InterfaceState>, ComponentFailure> {
        if interface < 0 || interface >= self.capacity {
            return Err(ComponentFailure::Index);
        }
        Ok(self.interfaces.get(&interface))
    }

    pub(crate) fn debug_interface_loaded(&self, interface: i32) -> bool {
        interface >= 0 && interface < self.capacity && self.interfaces.contains_key(&interface)
    }

    pub(crate) fn debug_component_count(&self, interface: i32) -> Result<i32, ComponentFailure> {
        Ok(self
            .debug_interface(interface)?
            .map_or(0, |state| state.components.len() as i32))
    }

    pub(crate) fn debug_interface_name(&self, interface: i32) -> Result<Arc<str>, ComponentFailure> {
        let Some(state) = self.debug_interface(interface)? else {
            return Ok(Arc::from(""));
        };
        state
            .components
            .first()
            .ok_or(ComponentFailure::Index)?
            .ok_or(ComponentFailure::NoComponent)?;
        match self.query.names.get(&(interface, 0)) {
            None => Ok(Arc::from("")),
            Some(name) => match name.find(':') {
                Some(colon) => Ok(Arc::from(&name[..colon])),
                None => Err(ComponentFailure::Index),
            },
        }
    }

    fn debug_slot(
        &self,
        interface: i32,
        index: i32,
    ) -> Result<Option<usize>, ComponentFailure> {
        let Some(state) = self.debug_interface(interface)? else {
            return Ok(None);
        };
        let slot = state
            .components
            .get(usize::try_from(index).map_err(|_| ComponentFailure::Index)?)
            .ok_or(ComponentFailure::Index)?;
        slot.ok_or(ComponentFailure::NoComponent)?;
        Ok(Some(index as usize))
    }

    pub(crate) fn debug_component_name(
        &self,
        interface: i32,
        index: i32,
    ) -> Result<Arc<str>, ComponentFailure> {
        Ok(match self.debug_slot(interface, index)? {
            None => Arc::from(""),
            Some(index) => self
                .query
                .names
                .get(&(interface, index))
                .cloned()
                .unwrap_or_else(|| Arc::from("")),
        })
    }

    pub(crate) fn debug_server_triggers(
        &self,
        interface: i32,
        index: i32,
    ) -> Result<i32, ComponentFailure> {
        Ok(match self.debug_slot(interface, index)? {
            None => 0,
            Some(index) => self
                .query
                .triggers
                .get(&(interface, index))
                .copied()
                .unwrap_or(0),
        })
    }
}
