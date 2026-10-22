use super::world::host_error;
use super::{push, Vm};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn execute_projection(
        &mut self,
        opcode: Opcode,
        host: &mut Option<&mut dyn VarHost>,
    ) -> Result<(), VmErrorKind> {
        use Opcode::*;
        let host = host
            .as_deref_mut()
            .ok_or(VmErrorKind::UnsupportedOpcode(opcode))?;
        let unavailable = host_error(opcode);
        let world = host.world_state().map_err(&unavailable)?;
        let projection = &mut world.projection;
        match opcode {
            VIEWPORT_GETEFFECTIVESIZE => {
                let (width, height) = projection
                    .game_screen
                    .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                projection
                    .calculate_game_area(&world.camera, 0, 0, width, height)
                    .map_err(&unavailable)?;
                push(&mut self.ints, projection.game_area_width, StackKind::Int)?;
                push(&mut self.ints, projection.game_area_height, StackKind::Int)?;
            }
            GET_ENTITY_SCREEN_POSITION | GET_LOC_SCREEN_POSITION | GET_OBJ_SCREEN_POSITION => {
                let anchor = match opcode {
                    GET_ENTITY_SCREEN_POSITION => projection.entity_anchor,
                    GET_LOC_SCREEN_POSITION => projection.loc_anchor,
                    _ => projection.obj_anchor,
                }
                .ok_or(VmErrorKind::Host(HostError::NoEntity))?;
                let [floor_offset] = self.pop_arguments()?;
                projection
                    .hint_placement(&world.scene, &world.camera, anchor, floor_offset)
                    .map_err(&unavailable)?;
                for value in projection.hint_position {
                    push(&mut self.ints, value as i32, StackKind::Int)?;
                }
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_projection_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            VIEWPORT_GETEFFECTIVESIZE
                | GET_ENTITY_SCREEN_POSITION
                | GET_LOC_SCREEN_POSITION
                | GET_OBJ_SCREEN_POSITION
        )
    }
}
