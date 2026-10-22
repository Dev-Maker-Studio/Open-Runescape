use super::world::host_error;
use super::{push, Vm};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn execute_bounds(
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
        let bounds = &mut world.projection.bounds;
        let missing = || VmErrorKind::Host(HostError::NoEntity);
        match opcode {
            GET_ENTITY_BOUNDING_BOX | GET_LOC_BOUNDING_BOX | GET_OBJ_BOUNDING_BOX => {
                let set = match opcode {
                    GET_ENTITY_BOUNDING_BOX => &bounds.entity,
                    GET_LOC_BOUNDING_BOX => &bounds.loc,
                    _ => &bounds.obj,
                }
                .as_ref()
                .ok_or_else(missing)?;
                let extent = set.extent().map_err(&unavailable)?;
                let values = match extent {
                    Some([min_x, min_y, max_x, max_y]) => [1, min_x, min_y, max_x, max_y],
                    None => [0; 5],
                };
                for value in values {
                    push(&mut self.ints, value, StackKind::Int)?;
                }
            }
            GET_LOC_OVERLAY_HEIGHT => {
                let height = bounds
                    .loc_height
                    .as_mut()
                    .ok_or_else(missing)?
                    .icon_height()
                    .map_err(&unavailable)?;
                push(&mut self.ints, height, StackKind::Int)?;
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_bounds_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            GET_ENTITY_BOUNDING_BOX
                | GET_LOC_BOUNDING_BOX
                | GET_OBJ_BOUNDING_BOX
                | GET_LOC_OVERLAY_HEIGHT
        )
    }
}
