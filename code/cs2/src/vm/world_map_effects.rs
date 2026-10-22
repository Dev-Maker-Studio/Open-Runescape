use super::world::host_error;
use super::{push, Vm};
use crate::{
    HostError, MecType, Opcode, StackKind, VarHost, Varbit, VmErrorKind, WorldMapEffect,
};

fn read_variable(host: &dyn VarHost, varp: i32, varbit: i32) -> Result<Option<i32>, HostError> {
    if varp != -1 {
        return host.varp(varp).map(Some);
    }
    if varbit == -1 {
        return Ok(None);
    }
    let definition = Varbit::lookup(host, varbit)?;
    let value = host.varp(i32::from(definition.varp))?;
    Ok(Some(definition.read(value)))
}

fn should_draw(host: &dyn VarHost, mec: &MecType) -> Result<bool, HostError> {
    let Some(primary) = read_variable(host, mec.primary_varp, mec.primary_varbit)? else {
        return Ok(true);
    };
    if primary < mec.primary_min || primary > mec.primary_max {
        return Ok(false);
    }
    let Some(secondary) = read_variable(host, mec.secondary_varp, mec.secondary_varbit)? else {
        return Ok(true);
    };
    Ok(secondary >= mec.secondary_min && secondary <= mec.secondary_max)
}

impl Vm {
    pub(super) fn execute_world_map_effect(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        if matches!(opcode, WORLDMAP_LISTELEMENT_START | WORLDMAP_LISTELEMENT_NEXT) {
            return self.list_world_map_element(opcode, host);
        }
        let world = host.world_state().map_err(&unavailable)?;
        let map = &mut world.world_map;
        let data = &world.map_data;
        let unpack = |coord: i32| ((coord >> 14) & 0x3fff, coord & 0x3fff);
        match opcode {
            WORLDMAP_SETZOOM => {
                let [percent] = self.pop_arguments()?;
                map.request_zoom(percent);
            }
            WORLDMAP_SETMAP | WORLDMAP_SETMAP_COORD | WORLDMAP_SETMAP_COORD_OVERRIDE => {
                let (id, x, y, use_target) = if opcode == WORLDMAP_SETMAP {
                    let [id] = self.pop_arguments()?;
                    (id, -1, -1, false)
                } else {
                    let [id, coord] = self.pop_arguments()?;
                    let (x, y) = unpack(coord);
                    (id, x, y, opcode == WORLDMAP_SETMAP_COORD_OVERRIDE)
                };
                map.effects.push(WorldMapEffect::ReloadAssets);
                map.requested_scroll_x = x;
                map.requested_scroll_y = y;
                map.use_target_position = use_target;
                map.static_element = data.defs.poll(id).map(|element| element.element_id);
            }
            WORLDMAP_JUMPTODISPLAYCOORD => {
                let [coord] = self.pop_arguments()?;
                let (x, y) = unpack(coord);
                map.set_scroll_target(x, y);
            }
            WORLDMAP_JUMPTOSOURCECOORD => {
                let [coord] = self.pop_arguments()?;
                let (x, y) = unpack(coord);
                let element = map.static_element.and_then(|id| data.defs.poll(id));
                if let Some((x, y)) =
                    element.and_then(|element| element.game_to_map_for_plane((coord >> 28) & 3, x, y))
                {
                    map.set_scroll_target(x, y);
                }
            }
            WORLDMAP_FLASHELEMENT => {
                let [id] = self.pop_arguments()?;
                map.flash_element(id);
            }
            WORLDMAP_FLASHELEMENTCATEGORY => {
                let [id] = self.pop_arguments()?;
                map.flash_category(id);
            }
            WORLDMAP_CLOSEMAP => map.effects.push(WorldMapEffect::LoadingScreen),
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    fn list_world_map_element(
        &mut self,
        opcode: Opcode,
        host: &mut dyn VarHost,
    ) -> Result<(), VmErrorKind> {
        let unavailable = host_error(opcode);
        let world = host.world_state().map_err(&unavailable)?;
        let restart = opcode == Opcode::WORLDMAP_LISTELEMENT_START;
        let start = if restart {
            Some(0)
        } else {
            world.world_map.list_cursor
        };
        let categories = world.map_data.categories.clone();
        let mec_types = world.map_data.mec_types.clone();
        let (square_x, square_y) = (world.world_map.square_x, world.world_map.square_y);
        let mut cursor = start;
        let mut found = None;
        while let Some(index) = cursor {
            let Some(category) = categories.get(index) else {
                cursor = None;
                break;
            };
            cursor = Some(index + 1);
            let mec = mec_types.get(&category.mec_id).copied().unwrap_or_default();
            let drawn = mec.visible && should_draw(&*host, &mec).map_err(&unavailable)?;
            if drawn {
                found = Some(*category);
                break;
            }
        }
        host.world_state().map_err(&unavailable)?.world_map.list_cursor = cursor;
        let (id, coord) = found.map_or((-1, -1), |category| {
            (
                category.mec_id,
                (category.abs_x.wrapping_add(square_x) << 14)
                    | category.abs_y.wrapping_add(square_y),
            )
        });
        push(&mut self.ints, id, StackKind::Int)?;
        push(&mut self.ints, coord, StackKind::Int)?;
        Ok(())
    }

    pub(super) fn is_world_map_effect_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            WORLDMAP_SETZOOM
                | WORLDMAP_SETMAP
                | WORLDMAP_SETMAP_COORD
                | WORLDMAP_SETMAP_COORD_OVERRIDE
                | WORLDMAP_JUMPTODISPLAYCOORD
                | WORLDMAP_JUMPTOSOURCECOORD
                | WORLDMAP_FLASHELEMENT
                | WORLDMAP_FLASHELEMENTCATEGORY
                | WORLDMAP_CLOSEMAP
                | WORLDMAP_LISTELEMENT_START
                | WORLDMAP_LISTELEMENT_NEXT
        )
    }
}
