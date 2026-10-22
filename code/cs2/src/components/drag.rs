use super::{ComponentError, ComponentFailure, ComponentId, ComponentModel};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum DragTarget {
    None,
    Frame,
    Component(ComponentId),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct DragState {
    pub dragging: Option<ComponentId>,
    pub selected: DragTarget,
    pub offset_x: i32,
    pub offset_y: i32,
    pub threshold_met: bool,
    pub ticks: i32,
    pub mini_menu_open: bool,
}

impl Default for DragState {
    fn default() -> Self {
        Self {
            dragging: None,
            selected: DragTarget::None,
            offset_x: 0,
            offset_y: 0,
            threshold_met: false,
            ticks: 0,
            mini_menu_open: false,
        }
    }
}

fn failure(error: ComponentFailure) -> ComponentError {
    ComponentError::Failure(error)
}

impl ComponentModel {
    pub fn drag_state(&self) -> DragState {
        self.drag
    }

    pub fn set_drag_state(&mut self, state: DragState) {
        self.drag = state;
    }

    pub fn set_mini_menu_open(&mut self, open: bool) {
        self.drag.mini_menu_open = open;
    }

    pub(super) fn sub_of(&self, id: ComponentId) -> Result<Option<ComponentId>, ComponentError> {
        let hash = self.arena[id.0].id_hash;
        let interface = hash >> 16;
        if interface < 0 || interface >= self.capacity {
            return Err(failure(ComponentFailure::Index));
        }
        let state = self
            .interfaces
            .get(&interface)
            .ok_or(failure(ComponentFailure::NoComponent))?;
        let parent = self.arena[id.0].parent_id;
        if parent != -1 {
            return self.lookup(state, parent).map_err(failure);
        }
        if !state.prevent_interaction {
            let wanted = ((hash as u32) >> 16) as i32;
            for open in self.open_sequence() {
                if self.open_interfaces.get(&open).copied() == Some(wanted) {
                    return self.resolve(open);
                }
            }
        }
        Ok(None)
    }

    fn drag_target(&self, id: ComponentId) -> Result<DragTarget, ComponentError> {
        let settings = self
            .settings_of(id)
            .ok_or(failure(ComponentFailure::NoComponent))?;
        if settings.ignores_depth_flags() {
            return Ok(DragTarget::Frame);
        }
        let flags = settings.depth_flags();
        if flags == 0 {
            return Ok(DragTarget::None);
        }
        let mut current = id;
        for _ in 0..flags {
            match self.sub_of(current)? {
                Some(next) => current = next,
                None => return Ok(DragTarget::Frame),
            }
        }
        Ok(DragTarget::Component(current))
    }

    pub(crate) fn drag_pickup(
        &mut self,
        id: ComponentId,
        offset_x: i32,
        offset_y: i32,
    ) -> Result<(), ComponentError> {
        if self.drag.dragging.is_some() || self.drag.mini_menu_open {
            return Ok(());
        }
        let settings = self
            .settings_of(id)
            .ok_or(failure(ComponentFailure::NoComponent))?;
        let draggable = settings.depth_flags() > 0
            || settings.ignores_depth_flags()
            || self.arena[id.0].depth_child.is_some();
        if !draggable {
            return Ok(());
        }
        self.drag.dragging = Some(id);
        let target = match self.drag_target(id)? {
            DragTarget::None => self.arena[id.0]
                .depth_child
                .map_or(DragTarget::None, DragTarget::Component),
            target => target,
        };
        self.drag.selected = target;
        self.drag.offset_x = offset_x;
        self.drag.offset_y = offset_y;
        self.drag.ticks = 0;
        self.drag.threshold_met = false;
        Ok(())
    }
}
