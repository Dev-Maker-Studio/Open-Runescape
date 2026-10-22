use super::world::host_error;
use super::{push, Vm};
use crate::{Opcode, StackKind, VarHost, VmErrorKind};
use std::sync::Arc;

impl Vm {
    pub(super) fn execute_world_map(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        use StackKind::Int;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let world = host.world_state().map_err(&unavailable)?;
        let map = &world.world_map;
        let data = &world.map_data;
        let unpack = |coord: i32| ((coord >> 14) & 0x3fff, coord & 0x3fff);
        match opcode {
            WORLDMAP_GETMAP => {
                let [coord] = self.pop_arguments()?;
                let (x, y) = unpack(coord);
                let id = data.defs.get_map_def(x, y).map_or(-1, |element| element.element_id);
                push(&mut self.ints, id, Int)?;
            }
            WORLDMAP_GETMAPNAME => {
                let [id] = self.pop_arguments()?;
                let name = data
                    .defs
                    .poll(id)
                    .map_or_else(|| Arc::from(""), |element| Arc::from(element.display_name.as_str()));
                push(&mut self.strings, Some(name), StackKind::String)?;
            }
            WORLDMAP_GETCONFIGORIGIN => {
                let [id] = self.pop_arguments()?;
                let (x, y) = data
                    .defs
                    .poll(id)
                    .map_or((0, 0), |element| unpack(element.bitpacked_placement));
                push(&mut self.ints, x, Int)?;
                push(&mut self.ints, y, Int)?;
            }
            WORLDMAP_GETCONFIGSIZE => {
                let [id] = self.pop_arguments()?;
                let (width, height) = data.defs.poll(id).map_or((0, 0), |element| {
                    (
                        element.bounds_max_x.wrapping_sub(element.bounds_min_x),
                        element.bounds_max_y.wrapping_sub(element.bounds_min_y),
                    )
                });
                push(&mut self.ints, width, Int)?;
                push(&mut self.ints, height, Int)?;
            }
            WORLDMAP_GETCONFIGZOOM => {
                let [id] = self.pop_arguments()?;
                let zoom = data.defs.poll(id).map_or(-1, |element| element.default_zoom_level);
                push(&mut self.ints, zoom, Int)?;
            }
            WORLDMAP_COORDINMAP => {
                let [coord, id] = self.pop_arguments()?;
                let (x, y) = unpack(coord);
                let found = data
                    .defs
                    .rendered_elements(x, y)
                    .any(|element| element.element_id == id);
                push(&mut self.ints, i32::from(found), Int)?;
            }
            WORLDMAP_GETSOURCECOORD | WORLDMAP_GETDISPLAYCOORD | WORLDMAP_GETSOURCEPOSITION => {
                let element = map.static_element.and_then(|id| data.defs.poll(id));
                let converted = match opcode {
                    WORLDMAP_GETSOURCECOORD => {
                        let [coord] = self.pop_arguments()?;
                        let (x, y) = unpack(coord);
                        element.and_then(|element| element.map_to_game(x, y))
                    }
                    WORLDMAP_GETDISPLAYCOORD => {
                        let [coord] = self.pop_arguments()?;
                        let (x, y) = unpack(coord);
                        element.and_then(|element| {
                            element.game_to_map_for_plane((coord >> 28) & 3, x, y)
                        })
                    }
                    _ => element.and_then(|element| {
                        element.map_to_game(
                            map.square_x.wrapping_add(map.center_x),
                            map.square_y.wrapping_add(map.center_y),
                        )
                    }),
                };
                let (x, y) = converted.unwrap_or((-1, -1));
                push(&mut self.ints, x, Int)?;
                push(&mut self.ints, y, Int)?;
            }
            WORLDMAP_FINDNEARESTELEMENT => {
                let [category, coord] = self.pop_arguments()?;
                let (x, y) = unpack(coord);
                let nearest = data.find_nearest_element(
                    category,
                    x,
                    y,
                    map.square_x,
                    map.square_y,
                    map.loading_percentage,
                );
                push(&mut self.ints, nearest.max(-1), Int)?;
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_world_map_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            WORLDMAP_GETMAP
                | WORLDMAP_GETMAPNAME
                | WORLDMAP_GETCONFIGORIGIN
                | WORLDMAP_GETCONFIGSIZE
                | WORLDMAP_GETCONFIGZOOM
                | WORLDMAP_COORDINMAP
                | WORLDMAP_GETSOURCECOORD
                | WORLDMAP_GETDISPLAYCOORD
                | WORLDMAP_GETSOURCEPOSITION
                | WORLDMAP_FINDNEARESTELEMENT
        )
    }
}
