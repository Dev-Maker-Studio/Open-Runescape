use super::{ComponentError, ComponentFailure, ComponentId, ComponentModel, TriggerSlot};

const CONTAINER_TYPE: u8 = 0;

fn component_click_type(click_type: i32) -> bool {
    matches!(click_type, 25 | 30 | 57 | 58 | 1007)
}

impl ComponentModel {
    pub(crate) fn has_active_menu_entry(
        &self,
        hash: i32,
        slot: i32,
        entries: &[(i32, i32, i32)],
    ) -> Result<bool, ComponentError> {
        let interface = hash >> 16;
        if interface < 0 || interface >= self.capacity {
            return Err(ComponentError::Failure(ComponentFailure::Index));
        }
        let Some(state) = self.interfaces.get(&interface) else {
            return Ok(false);
        };
        let Some(entry) = state.components.get((hash & 0xffff) as usize) else {
            return Err(ComponentError::Failure(ComponentFailure::Index));
        };
        let Some(component) = *entry else {
            return Ok(false);
        };
        let id_hash = self.arena[component.0].id_hash;
        if slot == -1 && self.arena[component.0].component_type == Some(CONTAINER_TYPE) {
            for (_, param1, click_type) in entries {
                if !component_click_type(*click_type) {
                    continue;
                }
                let mut current = self.resolve(*param1)?;
                while let Some(id) = current {
                    if self.arena[id.0].id_hash == id_hash {
                        return Ok(true);
                    }
                    current = self.sub_of(id)?;
                }
            }
        } else {
            for (param0, param1, click_type) in entries {
                if *param0 == slot && id_hash == *param1 && component_click_type(*click_type) {
                    return Ok(true);
                }
            }
        }
        Ok(false)
    }

    pub(crate) fn mouse_leave_groups(
        &self,
        param1: i32,
    ) -> Result<(Vec<Vec<i32>>, Option<ComponentError>), ComponentError> {
        let mut groups = Vec::new();
        let mut current: Option<ComponentId> = self.resolve(param1)?;
        while let Some(id) = current {
            let state = &self.arena[id.0];
            groups.push(
                state
                    .hooks
                    .triggers(TriggerSlot::MouseLeave)
                    .cloned()
                    .unwrap_or_default(),
            );
            current = if state.parent_id == -1 {
                None
            } else {
                match self.resolve(state.parent_id) {
                    Ok(next) => next,
                    Err(error) => return Ok((groups, Some(error))),
                }
            };
        }
        Ok((groups, None))
    }
}
