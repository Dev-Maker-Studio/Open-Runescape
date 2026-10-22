use super::{pop, push, Vm};
use crate::{HostError, Opcode, StackKind, VarHost, VmErrorKind};

pub(super) fn host_error(opcode: Opcode) -> impl Fn(HostError) -> VmErrorKind {
    move |error| match error {
        HostError::Unavailable => VmErrorKind::UnsupportedOpcode(opcode),
        error => VmErrorKind::Host(error),
    }
}

impl Vm {
    pub(super) fn pop_arguments<const N: usize>(&mut self) -> Result<[i32; N], VmErrorKind> {
        let mut values = [0; N];
        for slot in values.iter_mut().rev() {
            *slot = pop(&mut self.ints, StackKind::Int)?;
        }
        Ok(values)
    }

    pub(super) fn execute_world(
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
        let camera = &mut world.camera;
        let map = &mut world.world_map;
        match opcode {
            CAM_MOVETO => {
                let [coord, height, constant, variable] = self.pop_arguments()?;
                camera.move_to(coord, height, constant, variable);
            }
            CAM_MOVEALONG => {
                let arguments = self.pop_arguments()?;
                camera.move_along(arguments).map_err(&unavailable)?;
            }
            CAM_RESET => camera.smooth_reset(),
            CAM_SMOOTHRESET => camera.start_smooth_transition(),
            CAM_GETANGLE_XA => push(&mut self.ints, camera.angle_x_units(), Int)?,
            CAM_GETANGLE_YA => push(&mut self.ints, camera.angle_y_units(), Int)?,
            CAM_INC_X => camera.nudge_pitch(12.0),
            CAM_DEC_X => camera.nudge_pitch(-12.0),
            CAM_INC_Y => camera.nudge_yaw(24.0),
            CAM_DEC_Y => camera.nudge_yaw(-24.0),
            CAM_SETFOLLOWHEIGHT => {
                let [height] = self.pop_arguments()?;
                camera.set_follow_height(height);
            }
            CAM_GETFOLLOWHEIGHT => push(&mut self.ints, camera.follow_height(), Int)?,
            CAM_FOLLOWCOORD => {
                let [coord] = self.pop_arguments()?;
                camera.follow_coord(coord);
            }
            COORD_CLAMPTOSCENE_ORDEFAULT => {
                let [coord] = self.pop_arguments()?;
                camera.clamp_coord_or_default(coord);
            }
            SPLINE_NEW => {
                let [path, keyframes] = self.pop_arguments()?;
                camera.spline_new(path, keyframes).map_err(&unavailable)?;
            }
            SPLINE_ADDPOINT => {
                let [path, keyframe, position, position_height, look_at, look_at_height, pitch] =
                    self.pop_arguments()?;
                camera.spline_add_point(
                    path,
                    keyframe,
                    (position, position_height),
                    (look_at, look_at_height),
                    pitch,
                );
            }
            SPLINE_LENGTH => {
                let [path] = self.pop_arguments()?;
                let length = camera.spline_length(path).map_err(&unavailable)?;
                push(&mut self.ints, length, Int)?;
            }
            CAM2_ISENABLED => push(&mut self.ints, i32::from(camera.calculation_type == 2), Int)?,
            VIEWPORT_SETFOV => {
                let [minimum, maximum] = self.pop_arguments()?;
                camera.set_viewport_fov(minimum, maximum);
            }
            VIEWPORT_SETZOOM => {
                let [minimum, maximum] = self.pop_arguments()?;
                camera.set_viewport_zoom(minimum, maximum);
            }
            VIEWPORT_CLAMPFOV => {
                let values = self.pop_arguments()?;
                camera.clamp_viewport_fov(values);
            }
            VIEWPORT_GETFOV => {
                push(&mut self.ints, i32::from(camera.viewport_fov_min), Int)?;
                push(&mut self.ints, i32::from(camera.viewport_fov_max), Int)?;
            }
            VIEWPORT_GETZOOM => {
                push(&mut self.ints, i32::from(camera.zoom_min), Int)?;
                push(&mut self.ints, i32::from(camera.zoom_max), Int)?;
            }
            WORLDMAP_GETDISPLAYPOSITION => {
                push(&mut self.ints, map.center_x.wrapping_add(map.square_x), Int)?;
                push(&mut self.ints, map.center_y.wrapping_add(map.square_y), Int)?;
            }
            WORLDMAP_GETSIZE => {
                push(&mut self.ints, map.view_width, Int)?;
                push(&mut self.ints, map.viewport_scale, Int)?;
            }
            WORLDMAP_ISLOADED => push(&mut self.ints, i32::from(map.loading_percentage == 100), Int)?,
            WORLDMAP_GETCURRENTMAP => push(&mut self.ints, map.static_element.unwrap_or(-1), Int)?,
            WORLDMAP_GETZOOM => push(&mut self.ints, map.zoom_level(), Int)?,
            WORLDMAP_DISABLEELEMENTS => {
                let [value] = self.pop_arguments()?;
                map.hide_icons = value == 1;
            }
            WORLDMAP_GETDISABLEELEMENTS => push(&mut self.ints, i32::from(map.hide_icons), Int)?,
            WORLDMAP_DISABLEELEMENT => {
                let [id, disabled] = self.pop_arguments()?;
                map.set_element_hidden(id, disabled == 1);
            }
            WORLDMAP_GETDISABLEELEMENT => {
                let [id] = self.pop_arguments()?;
                push(&mut self.ints, i32::from(map.hidden_elements.contains(&id)), Int)?;
            }
            WORLDMAP_DISABLEELEMENTCATEGORY => {
                let [id, disabled] = self.pop_arguments()?;
                map.set_category_hidden(id, disabled == 1);
            }
            WORLDMAP_GETDISABLEELEMENTCATEGORY => {
                let [id] = self.pop_arguments()?;
                push(&mut self.ints, i32::from(map.hidden_categories.contains(&id)), Int)?;
            }
            _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
        }
        Ok(())
    }

    pub(super) fn is_world_opcode(opcode: Opcode) -> bool {
        use Opcode::*;
        matches!(
            opcode,
            CAM_MOVETO
                | CAM_MOVEALONG
                | CAM_RESET
                | CAM_SMOOTHRESET
                | CAM_GETANGLE_XA
                | CAM_GETANGLE_YA
                | CAM_INC_X
                | CAM_DEC_X
                | CAM_INC_Y
                | CAM_DEC_Y
                | CAM_SETFOLLOWHEIGHT
                | CAM_GETFOLLOWHEIGHT
                | CAM_FOLLOWCOORD
                | COORD_CLAMPTOSCENE_ORDEFAULT
                | SPLINE_NEW
                | SPLINE_ADDPOINT
                | SPLINE_LENGTH
                | CAM2_ISENABLED
                | VIEWPORT_SETFOV
                | VIEWPORT_SETZOOM
                | VIEWPORT_CLAMPFOV
                | VIEWPORT_GETFOV
                | VIEWPORT_GETZOOM
                | WORLDMAP_GETDISPLAYPOSITION
                | WORLDMAP_GETSIZE
                | WORLDMAP_ISLOADED
                | WORLDMAP_GETCURRENTMAP
                | WORLDMAP_GETZOOM
                | WORLDMAP_DISABLEELEMENTS
                | WORLDMAP_GETDISABLEELEMENTS
                | WORLDMAP_DISABLEELEMENT
                | WORLDMAP_GETDISABLEELEMENT
                | WORLDMAP_DISABLEELEMENTCATEGORY
                | WORLDMAP_GETDISABLEELEMENTCATEGORY
        )
    }
}
