use super::world::host_error;
use super::Vm;
use crate::{Opcode, VarHost, VmErrorKind};

impl Vm {
    pub(super) fn execute_scene(
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
        match opcode {
            CAM_LOOKAT => {
                let [coord, height, speed, angle] = self.pop_arguments()?;
                let base_x = world.camera.scene_base_x;
                let base_y = world.camera.scene_base_y;
                world
                    .scene
                    .look_at(
                        &mut world.camera,
                        ((coord >> 14) & 0x3fff).wrapping_sub(base_x),
                        (coord & 0x3fff).wrapping_sub(base_y),
                        height << 2,
                        speed,
                        angle,
                    )
                    .map_err(&unavailable)?;
            }
            CAM_FORCEANGLE => {
                let [angle_x, angle_y] = self.pop_arguments()?;
                world
                    .scene
                    .set_angle(&mut world.camera, angle_x, angle_y, 0)
                    .map_err(&unavailable)?;
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_scene_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(opcode, CAM_LOOKAT | CAM_FORCEANGLE)
    }
}
