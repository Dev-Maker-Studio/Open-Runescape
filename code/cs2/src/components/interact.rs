use super::{
    ComponentError, ComponentFailure, ComponentHook, ComponentId, ComponentModel, ComponentState,
    HookSlot,
};
use std::collections::BTreeMap;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum EntityInterface {
    Absent,
    Unbound,
    Bound(i32),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ComponentSettings {
    pub hash: i32,
    pub interface_id: i32,
}

impl ComponentSettings {
    pub fn click_option_enabled(self, index: i32) -> bool {
        self.hash.wrapping_shr(index.wrapping_add(1) as u32) & 1 != 0
    }

    pub fn continue_option_enabled(self) -> bool {
        self.hash & 1 != 0
    }

    pub fn depth_flags(self) -> i32 {
        self.hash.wrapping_shr(18) & 0x7
    }

    pub fn ignores_depth_flags(self) -> bool {
        self.hash.wrapping_shr(23) & 1 != 0
    }

    pub fn use_option_flags(self) -> i32 {
        self.hash.wrapping_shr(11) & 0x7f
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct InterfaceSelection {
    pub active: bool,
    pub hash: i32,
    pub slot: i32,
    pub flags: i32,
    pub interface: i32,
    pub item: i32,
    pub use_cursor: i32,
    pub default_cursor: i32,
}

impl Default for InterfaceSelection {
    fn default() -> Self {
        Self {
            active: false,
            hash: 0,
            slot: -1,
            flags: 0,
            interface: 0,
            item: -1,
            use_cursor: -1,
            default_cursor: -1,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct OptionInteraction {
    pub option: i32,
    pub hash: i32,
    pub slot: i32,
    pub item_id: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
struct Props {
    target: Option<ComponentSettings>,
    over_cursor: i32,
    leave_cursor: i32,
}

impl Default for Props {
    fn default() -> Self {
        Self {
            target: Some(ComponentSettings {
                hash: 0,
                interface_id: -1,
            }),
            over_cursor: -1,
            leave_cursor: -1,
        }
    }
}

#[derive(Clone, Debug)]
pub(super) struct InteractState {
    entity: EntityInterface,
    pub(super) slot_settings: BTreeMap<(i32, i32), ComponentSettings>,
    props: BTreeMap<ComponentId, Props>,
    selection: InterfaceSelection,
}

impl Default for InteractState {
    fn default() -> Self {
        Self {
            entity: EntityInterface::Absent,
            slot_settings: BTreeMap::new(),
            props: BTreeMap::new(),
            selection: InterfaceSelection::default(),
        }
    }
}

impl ComponentModel {
    pub fn set_entity_interface(&mut self, entity: EntityInterface) {
        self.interact.entity = entity;
    }

    pub fn set_slot_settings(&mut self, hash: i32, slot: i32, settings: Option<ComponentSettings>) {
        match settings {
            Some(settings) => {
                self.interact.slot_settings.insert((hash, slot), settings);
            }
            None => {
                self.interact.slot_settings.remove(&(hash, slot));
            }
        }
    }

    pub fn set_target_settings(&mut self, id: ComponentId, settings: Option<ComponentSettings>) {
        self.interact.props.entry(id).or_default().target = settings;
    }

    pub fn set_container_item(&mut self, id: ComponentId, item_id: i32) {
        self.arena[id.0].container_item_id = item_id;
    }

    pub fn set_target_cursors(&mut self, id: ComponentId, over: i32, leave: i32) {
        let props = self.interact.props.entry(id).or_default();
        props.over_cursor = over;
        props.leave_cursor = leave;
    }

    pub fn set_selection(&mut self, selection: InterfaceSelection) {
        self.interact.selection = selection;
    }

    pub fn selection(&self) -> InterfaceSelection {
        self.interact.selection
    }

    fn props(&self, id: ComponentId) -> Props {
        self.interact.props.get(&id).copied().unwrap_or_default()
    }

    pub(crate) fn settings_of(&self, id: ComponentId) -> Option<ComponentSettings> {
        let state = &self.arena[id.0];
        self.interact
            .slot_settings
            .get(&(state.id_hash, state.slot_id))
            .copied()
            .or(self.props(id).target)
    }

    fn has_script(&self, id: ComponentId, slot: HookSlot) -> bool {
        self.arena[id.0].hooks.script(slot).is_some()
    }

    pub(crate) fn entity_is_bound(&self) -> bool {
        self.entity_bound().is_ok()
    }

    fn entity_bound(&self) -> Result<i32, ComponentFailure> {
        match self.interact.entity {
            EntityInterface::Bound(id) => Ok(id),
            _ => Err(ComponentFailure::NoComponent),
        }
    }

    fn entity_state(&self) -> Result<(i32, &super::InterfaceState), ComponentError> {
        let id = self.entity_bound()?;
        match self.interfaces.get(&id) {
            Some(state) => Ok((id, state)),
            None => Err(ComponentError::Unavailable),
        }
    }

    pub(crate) fn list_component(
        &self,
        hash: i32,
        slot: i32,
    ) -> Result<Option<ComponentId>, ComponentError> {
        let found = self.resolve(hash)?;
        if slot == -1 {
            return Ok(found);
        }
        let Some(parent) = found else {
            return Ok(None);
        };
        match self.arena[parent.0].slot_children.as_ref() {
            Some(children) if slot < children.len() as i32 => {
                let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
                Ok(children[index])
            }
            _ => Ok(None),
        }
    }

    pub(crate) fn entity_create(
        &mut self,
        secondary: bool,
        hash: i32,
        type_id: i32,
        slot: i32,
    ) -> Result<(), ComponentError> {
        let entity = self.entity_bound()?;
        if type_id == 0 {
            return Err(ComponentFailure::Rejected.into());
        }
        let Some(interface) = self.interfaces.get(&entity) else {
            return Err(ComponentError::Unavailable);
        };
        let parent = interface
            .components
            .get((hash & 0xffff) as usize)
            .ok_or(ComponentFailure::Index)?
            .ok_or(ComponentFailure::NoComponent)?;
        let size = slot.wrapping_add(1);
        if self.arena[parent.0].slot_children.is_none() {
            if size < 0 {
                return Err(ComponentFailure::Index.into());
            }
            if size as usize > self.max_slots {
                return Err(ComponentFailure::SlotLimit.into());
            }
            self.arena[parent.0].slot_children = Some(vec![None; size as usize]);
            self.arena[parent.0].item_slots = None;
        }
        let length = self.arena[parent.0]
            .slot_children
            .as_ref()
            .map_or(0, Vec::len);
        if (length as i64) <= i64::from(slot) {
            if size < 0 {
                return Err(ComponentFailure::Index.into());
            }
            if size as usize > self.max_slots {
                return Err(ComponentFailure::SlotLimit.into());
            }
            if let Some(children) = self.arena[parent.0].slot_children.as_mut() {
                children.resize(size as usize, None);
            }
            if let Some(items) = self.arena[parent.0].item_slots.as_mut() {
                items.resize(size as usize, None);
            }
        }
        if slot > 0 && self.slot_child(parent, (slot - 1) as usize).is_none() {
            return Err(ComponentFailure::Rejected.into());
        }
        let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
        let parent_hash = self.arena[parent.0].id_hash;
        self.arena.push(ComponentState {
            component_type: u8::try_from(type_id).ok().filter(|id| *id <= 9),
            id_hash: parent_hash,
            parent_id: parent_hash,
            slot_id: slot,
            ..ComponentState::default()
        });
        let id = ComponentId(self.arena.len() - 1);
        if let Some(children) = self.arena[parent.0].slot_children.as_mut() {
            children[index] = Some(id);
        }
        if let Some(items) = self.arena[parent.0].item_slots.as_mut() {
            items[index] = Some(id);
        }
        self.hooks[usize::from(secondary)] = Some(ComponentHook {
            interface: entity,
            component: id,
        });
        self.redraw(parent);
        Ok(())
    }

    pub(crate) fn entity_delete_all(&mut self, hash: i32) -> Result<(), ComponentError> {
        let (_, state) = self.entity_state()?;
        let found = self.lookup(state, hash)?;
        let id = found.ok_or(ComponentFailure::NoComponent)?;
        self.delete_all(id);
        Ok(())
    }

    pub(crate) fn entity_load(
        &mut self,
        secondary: bool,
        hash: i32,
    ) -> Result<bool, ComponentError> {
        if self.interact.entity == EntityInterface::Absent {
            return Err(ComponentFailure::NoComponent.into());
        }
        if self.interact.entity == EntityInterface::Unbound {
            self.hooks[usize::from(secondary)] = None;
            return Ok(false);
        }
        let (entity, state) = self.entity_state()?;
        let found = self.lookup(state, hash)?;
        self.hooks[usize::from(secondary)] = found.map(|component| ComponentHook {
            interface: entity,
            component,
        });
        Ok(found.is_some())
    }

    pub(crate) fn option_interact(
        &mut self,
        option: i32,
        hash: i32,
        slot: i32,
    ) -> Result<Option<OptionInteraction>, ComponentError> {
        let Some(id) = self.list_component(hash, slot)? else {
            return Ok(None);
        };
        if self.has_script(id, HookSlot::Params) {
            return Err(ComponentError::Unavailable);
        }
        let settings = self
            .settings_of(id)
            .ok_or(ComponentFailure::NoComponent)?;
        if !settings.click_option_enabled(option.wrapping_sub(1)) {
            return Ok(None);
        }
        Ok(Some(OptionInteraction {
            option,
            hash,
            slot,
            item_id: self.arena[id.0].container_item_id,
        }))
    }

    pub(crate) fn cancel_selection(&mut self) -> Result<(), ComponentError> {
        let selection = self.interact.selection;
        if !selection.active {
            return Ok(());
        }
        let selected = self.list_component(selection.hash, selection.slot)?;
        if let Some(selected) = selected {
            if self.has_script(selected, HookSlot::HookParams) {
                return Err(ComponentError::Unavailable);
            }
        }
        self.interact.selection.default_cursor = -1;
        self.interact.selection.item = -1;
        self.interact.selection.active = false;
        if let Some(selected) = selected {
            self.redraw(selected);
        }
        Ok(())
    }

    pub(crate) fn debug_target(&mut self, hash: i32, slot: i32) -> Result<(), ComponentError> {
        let target = self.list_component(hash, slot)?;
        let selection = self.interact.selection;
        let cancelled = if selection.active {
            let selected = self.list_component(selection.hash, selection.slot)?;
            if let Some(selected) = selected {
                if self.has_script(selected, HookSlot::HookParams) {
                    return Err(ComponentError::Unavailable);
                }
            }
            Some(selected)
        } else {
            None
        };
        let ready = target.and_then(|id| self.settings_of(id).map(|settings| (id, settings)));
        if let Some((id, _)) = ready {
            if self.has_script(id, HookSlot::OnTargetEnter) {
                return Err(ComponentError::Unavailable);
            }
        }
        if let Some(selected) = cancelled {
            self.interact.selection.default_cursor = -1;
            self.interact.selection.item = -1;
            self.interact.selection.active = false;
            if let Some(selected) = selected {
                self.redraw(selected);
            }
        }
        let Some((id, settings)) = ready else {
            return Err(ComponentFailure::NoComponent.into());
        };
        let props = self.props(id);
        let state = &self.arena[id.0];
        self.interact.selection = InterfaceSelection {
            active: true,
            hash: state.id_hash,
            slot: state.slot_id,
            flags: settings.use_option_flags(),
            interface: settings.interface_id,
            item: self.arena[id.0].container_item_id,
            use_cursor: props.over_cursor,
            default_cursor: props.leave_cursor,
        };
        self.redraw(id);
        Ok(())
    }
}
