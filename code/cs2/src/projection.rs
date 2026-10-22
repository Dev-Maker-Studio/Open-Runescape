use crate::world::CameraState;
use crate::{HostError, SceneState};

pub const DIVISION_BY_ZERO: &str = "division by zero";

const IDENTITY_44: [f32; 16] = [
    1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0,
];

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Matrix43 {
    pub xa: f32,
    pub xb: f32,
    pub xc: f32,
    pub xd: f32,
    pub ya: f32,
    pub yb: f32,
    pub yc: f32,
    pub yd: f32,
    pub za: f32,
    pub zb: f32,
    pub zc: f32,
    pub zd: f32,
}

impl Matrix43 {
    pub const IDENTITY: Self = Self {
        xa: 1.0,
        xb: 0.0,
        xc: 0.0,
        xd: 0.0,
        ya: 0.0,
        yb: 1.0,
        yc: 0.0,
        yd: 0.0,
        za: 0.0,
        zb: 0.0,
        zc: 1.0,
        zd: 0.0,
    };

    pub fn translate(&mut self, x: f32, y: f32, z: f32) {
        self.xd += x;
        self.yd += y;
        self.zd += z;
    }

    pub fn to_matrix44(self) -> [f32; 16] {
        [
            self.xa, self.ya, self.za, 0.0, self.xb, self.yb, self.zb, 0.0, self.xc, self.yc,
            self.zc, 0.0, self.xd, self.yd, self.zd, 1.0,
        ]
    }
}

impl Default for Matrix43 {
    fn default() -> Self {
        Self::IDENTITY
    }
}

pub fn multiply44(left: &[f32; 16], right: &[f32; 16]) -> [f32; 16] {
    let mut out = [0.0f32; 16];
    for row in 0..4 {
        for column in 0..4 {
            out[row * 4 + column] = left[row * 4] * right[column]
                + left[row * 4 + 1] * right[4 + column]
                + left[row * 4 + 2] * right[8 + column]
                + left[row * 4 + 3] * right[12 + column];
        }
    }
    out
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub enum HintAnchor {
    Point {
        x: f32,
        z: f32,
        plane: i8,
    },
    Footprint {
        start_x: i16,
        end_x: i16,
        start_z: i16,
        end_z: i16,
        plane: i8,
    },
}

impl HintAnchor {
    fn resolve(self) -> (i32, i32, i32) {
        match self {
            Self::Point { x, z, plane } => (x as i32, z as i32, i32::from(plane)),
            Self::Footprint {
                start_x,
                end_x,
                start_z,
                end_z,
                plane,
            } => {
                let width = i32::from(end_x)
                    .wrapping_sub(i32::from(start_x))
                    .wrapping_add(1)
                    << 9;
                let length = i32::from(end_z)
                    .wrapping_sub(i32::from(start_z))
                    .wrapping_add(1)
                    << 9;
                (
                    (width / 2).wrapping_add(i32::from(start_x) << 9),
                    (length / 2).wrapping_add(i32::from(start_z) << 9),
                    i32::from(plane),
                )
            }
        }
    }
}

fn divide(numerator: i32, denominator: i32) -> Result<i32, HostError> {
    if denominator == 0 {
        Err(HostError::State(DIVISION_BY_ZERO.to_string()))
    } else {
        Ok(numerator.wrapping_div(denominator))
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct ProjectionState {
    pub view: Matrix43,
    pub view44: [f32; 16],
    pub model: [f32; 16],
    pub view_projection: [f32; 16],
    pub origin_x: f32,
    pub origin_y: f32,
    pub center_x: f32,
    pub center_y: f32,
    pub sun: Matrix43,
    pub hint_position: [f32; 3],
    pub hint_offset_x: i32,
    pub hint_offset_y: i32,
    pub game_area_width: i32,
    pub game_area_height: i32,
    pub scaled_fov: i32,
    pub game_screen: Option<(i32, i32)>,
    pub entity_anchor: Option<HintAnchor>,
    pub loc_anchor: Option<HintAnchor>,
    pub obj_anchor: Option<HintAnchor>,
    pub bounds: crate::BoundsState,
}

impl Default for ProjectionState {
    fn default() -> Self {
        Self {
            view: Matrix43::IDENTITY,
            view44: IDENTITY_44,
            model: IDENTITY_44,
            view_projection: IDENTITY_44,
            origin_x: 0.0,
            origin_y: 0.0,
            center_x: 0.0,
            center_y: 0.0,
            sun: Matrix43::IDENTITY,
            hint_position: [0.0; 3],
            hint_offset_x: 0,
            hint_offset_y: 0,
            game_area_width: 0,
            game_area_height: 0,
            scaled_fov: 0,
            game_screen: None,
            entity_anchor: None,
            loc_anchor: None,
            obj_anchor: None,
            bounds: crate::BoundsState::default(),
        }
    }
}

impl ProjectionState {
    pub fn set_view_matrix(&mut self, view: Matrix43) {
        self.view = view;
        self.view44 = view.to_matrix44();
        self.view_projection = multiply44(&self.view44, &self.model);
    }

    pub fn calculate_projection(&mut self, x: f32, y: f32, z: f32) {
        let m = &self.view_projection;
        let v = &self.view44;
        let clip_w = m[15] + m[3] * x + m[7] * y + m[11] * z;
        let clip_x = m[12] + m[0] * x + m[4] * y + m[8] * z;
        let clip_y = m[13] + m[1] * x + m[5] * y + m[9] * z;
        let view_z = v[14] + v[2] * x + v[6] * y + v[10] * z;
        let inverse_w = 1.0f32 / clip_w;
        self.hint_position = [
            self.origin_x + self.center_x * clip_x * inverse_w,
            self.origin_y + self.center_y * clip_y * inverse_w,
            view_z,
        ];
    }

    pub fn hint_placement(
        &mut self,
        scene: &SceneState,
        camera: &CameraState,
        anchor: HintAnchor,
        floor_offset: i32,
    ) -> Result<(), HostError> {
        let (absolute_x, absolute_y, plane) = anchor.resolve();
        let draw_height = scene
            .tile_height(camera, absolute_x, absolute_y, plane)?
            .wrapping_sub(floor_offset);
        self.sun = self.view;
        self.sun.translate(0.0, 0.0, 0.0);
        self.set_view_matrix(self.sun);
        self.calculate_projection(absolute_x as f32, draw_height as f32, absolute_y as f32);
        self.sun.translate(-0.0, 0.0, 0.0);
        self.set_view_matrix(self.sun);
        self.hint_position[0] -= self.hint_offset_x as f32;
        self.hint_position[1] -= self.hint_offset_y as f32;
        Ok(())
    }

    pub fn calculate_game_area(
        &mut self,
        camera: &CameraState,
        x: i32,
        y: i32,
        width: i32,
        height: i32,
    ) -> Result<(), HostError> {
        let (mut x, mut y, mut w, mut h) = (x, y, width.max(1), height.max(1));
        let scale_factor = h.wrapping_sub(334).clamp(0, 100);
        let fov_min = i32::from(camera.viewport_fov_min);
        let fov_max = i32::from(camera.viewport_fov_max);
        let mut fov_scale = divide(
            fov_max.wrapping_sub(fov_min).wrapping_mul(scale_factor),
            100,
        )?
        .wrapping_add(fov_min);
        let aspect_fov = divide(
            h.wrapping_mul(fov_scale).wrapping_mul(512),
            w.wrapping_mul(334),
        )?;
        let aspect_min = i32::from(camera.aspect_fov_min);
        let aspect_max = i32::from(camera.aspect_fov_max);
        if aspect_fov < aspect_min {
            fov_scale = divide(
                aspect_min.wrapping_mul(w).wrapping_mul(334),
                h.wrapping_mul(512),
            )?;
            if fov_scale > i32::from(camera.fov_scale_max) {
                fov_scale = i32::from(camera.fov_scale_max);
                let adjusted = divide(
                    h.wrapping_mul(fov_scale).wrapping_mul(512),
                    aspect_min.wrapping_mul(334),
                )?;
                let padding = divide(w.wrapping_sub(adjusted), 2)?;
                x = x.wrapping_add(padding);
                w = w.wrapping_sub(padding.wrapping_mul(2));
            }
        } else if aspect_fov > aspect_max {
            fov_scale = divide(
                aspect_max.wrapping_mul(w).wrapping_mul(334),
                h.wrapping_mul(512),
            )?;
            if fov_scale < i32::from(camera.fov_scale_min) {
                fov_scale = i32::from(camera.fov_scale_min);
                let adjusted = divide(
                    aspect_max.wrapping_mul(w).wrapping_mul(334),
                    fov_scale.wrapping_mul(512),
                )?;
                let padding = divide(h.wrapping_sub(adjusted), 2)?;
                y = y.wrapping_add(padding);
                h = h.wrapping_sub(padding.wrapping_mul(2));
            }
        }
        self.scaled_fov = divide(h.wrapping_mul(fov_scale), 334)?;
        self.hint_offset_x = x;
        self.hint_offset_y = y;
        self.game_area_width = w;
        self.game_area_height = h;
        Ok(())
    }
}
