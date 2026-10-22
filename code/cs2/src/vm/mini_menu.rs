use super::world::host_error;
use super::{push, Vm};
use crate::{ComponentError, MiniMenuStyle, Opcode, StackKind, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn execute_mini_menu(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        if matches!(
            opcode,
            Opcode::MINIMENUOPEN | Opcode::GET_ACTIVE_MINIMENU_INVSLOT
        ) {
            return self.execute_menu_lookup(opcode, host);
        }
        let world = host.world_state().map_err(host_error(opcode))?;
        if opcode == Opcode::FORMATMINIMENU {
            let v: [i32; 12] = self.pop_arguments()?;
            world.platform.mini_menu_open = false;
            world.platform.active_entry = None;
            world.platform.secondary_entry = None;
            world.mini_menu.style = MiniMenuStyle {
                background_color: v[0],
                background_transparency: v[1],
                hover_highlight_color: v[2],
                background_alpha: v[3],
                loading_bar_background_sprite: v[4],
                loading_bar_tile_sprite: v[5],
                top_border_sprite: v[6],
                side_border_sprite: v[7],
                corner_sprite: v[8],
                text_color: v[9],
                hover_text_color: v[10],
                font_index: v[11],
            };
            world.mini_menu.bold_font = true;
            return Ok(());
        }
        let menu = &mut world.mini_menu;
        match opcode {
            Opcode::MINIMENU_SETMAXENTRIES => {
                let [maximum] = self.pop_arguments()?;
                menu.max_visible_entries = maximum;
            }
            Opcode::DEFAULTMINIMENU => menu.bold_font = false,
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_mini_menu_opcode(opcode: Opcode) -> bool {
        matches!(
            opcode,
            Opcode::MINIMENU_SETMAXENTRIES
                | Opcode::DEFAULTMINIMENU
                | Opcode::FORMATMINIMENU
                | Opcode::MINIMENUOPEN
                | Opcode::GET_ACTIVE_MINIMENU_INVSLOT
        )
    }
}

fn component_failure(error: ComponentError, opcode: Opcode) -> VmErrorKind {
    match error {
        ComponentError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        ComponentError::Failure(failure) => VmErrorKind::Component(failure),
    }
}

fn component_click_type(click_type: i32) -> bool {
    matches!(click_type, 25 | 30 | 57 | 58 | 1007)
}

impl Vm {
    fn execute_menu_lookup(
        &mut self,
        opcode: Opcode,
        host: &mut dyn VarHost,
    ) -> Result<(), VmErrorKind> {
        let arguments: Option<[i32; 2]> = if opcode == Opcode::MINIMENUOPEN {
            Some(self.pop_arguments()?)
        } else {
            None
        };
        let platform = &host.world_state().map_err(host_error(opcode))?.platform;
        let open = platform.mini_menu_open;
        if let Some([hash, slot]) = arguments {
            let entries: Vec<(i32, i32, i32)> = platform
                .entries
                .iter()
                .map(|entry| (entry.param0, entry.param1, entry.click_type))
                .collect();
            let found = if open {
                let outcome = host
                    .components()
                    .map_err(host_error(opcode))?
                    .has_active_menu_entry(hash, slot, &entries);
                match outcome {
                    Ok(found) => found,
                    Err(error) => {
                        push(&mut self.ints, hash, StackKind::Int)?;
                        return Err(component_failure(error, opcode));
                    }
                }
            } else {
                false
            };
            return push(&mut self.ints, i32::from(found), StackKind::Int);
        }
        let entry = if open {
            None
        } else {
            platform
                .active_entry
                .clone()
                .filter(|entry| entry.item_id != -1 && component_click_type(entry.click_type))
        };
        let mut found = (-1, -1, false);
        let mut matched = false;
        if let Some(entry) = entry {
            let (groups, pending) = host
                .components()
                .map_err(host_error(opcode))?
                .mouse_leave_groups(entry.param1)
                .map_err(|error| component_failure(error, opcode))?;
            'search: for group in groups {
                for candidate in group {
                    for other in [false, true] {
                        let slot = host
                            .items()
                            .map_err(host_error(opcode))?
                            .find_tooltip_slot(candidate, entry.param0, entry.item_id, other);
                        if slot != -1 {
                            found = (candidate, slot, other);
                            matched = true;
                            break 'search;
                        }
                    }
                }
            }
            if !matched {
                if let Some(error) = pending {
                    return Err(component_failure(error, opcode));
                }
            }
        }
        push(&mut self.ints, found.0, StackKind::Int)?;
        push(&mut self.ints, found.1, StackKind::Int)?;
        push(&mut self.ints, i32::from(found.2), StackKind::Int)
    }
}
