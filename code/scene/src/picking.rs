#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Matrix44(pub [f32; 16]);

impl Matrix44 {
    pub fn identity() -> Self {
        let mut e = [0.0f32; 16];
        e[0] = 1.0;
        e[5] = 1.0;
        e[10] = 1.0;
        e[15] = 1.0;
        Matrix44(e)
    }

    pub fn from_matrix43(m: &[f32; 12]) -> Self {
        Matrix44([
            m[0], m[1], m[2], 0.0, m[3], m[4], m[5], 0.0, m[6], m[7], m[8], 0.0, m[9], m[10],
            m[11], 1.0,
        ])
    }

    pub fn scale(&self, x: f32, y: f32, z: f32) -> [f32; 4] {
        let e = &self.0;
        [
            e[0] * x + e[4] * y + e[8] * z + e[12],
            e[1] * x + e[5] * y + e[9] * z + e[13],
            e[2] * x + e[6] * y + e[10] * z + e[14],
            e[3] * x + e[7] * y + e[11] * z + e[15],
        ]
    }

    pub fn determinant(&self) -> f32 {
        let e = &self.0;
        e[0] * e[5] * e[10] * e[15] - e[0] * e[5] * e[11] * e[14] - e[0] * e[6] * e[9] * e[15]
            + e[0] * e[6] * e[11] * e[13]
            + e[0] * e[7] * e[9] * e[14]
            - e[0] * e[7] * e[10] * e[13]
            - e[1] * e[4] * e[10] * e[15]
            + e[1] * e[4] * e[11] * e[14]
            + e[1] * e[6] * e[8] * e[15]
            - e[1] * e[6] * e[11] * e[12]
            - e[1] * e[7] * e[8] * e[14]
            + e[1] * e[7] * e[10] * e[12]
            + e[2] * e[4] * e[9] * e[15]
            - e[2] * e[4] * e[11] * e[13]
            - e[2] * e[5] * e[8] * e[15]
            + e[2] * e[5] * e[11] * e[12]
            + e[2] * e[7] * e[8] * e[13]
            - e[2] * e[7] * e[9] * e[12]
            - e[3] * e[4] * e[9] * e[14]
            + e[3] * e[4] * e[10] * e[13]
            + e[3] * e[5] * e[8] * e[14]
            - e[3] * e[5] * e[10] * e[12]
            - e[3] * e[6] * e[8] * e[13]
            + e[3] * e[6] * e[9] * e[12]
    }

    pub fn invert(&mut self) {
        let inverse_det = 1.0f32 / self.determinant();
        let e = &self.0;
        let c00 = (e[5] * e[10] * e[15] - e[5] * e[11] * e[14] - e[6] * e[9] * e[15]
            + e[6] * e[11] * e[13]
            + e[7] * e[9] * e[14]
            - e[7] * e[10] * e[13])
            * inverse_det;
        let c01 = (-e[1] * e[10] * e[15] + e[1] * e[11] * e[14] + e[2] * e[9] * e[15]
            - e[2] * e[11] * e[13]
            - e[3] * e[9] * e[14]
            + e[3] * e[10] * e[13])
            * inverse_det;
        let c02 = (e[1] * e[6] * e[15] - e[1] * e[7] * e[14] - e[2] * e[5] * e[15]
            + e[2] * e[7] * e[13]
            + e[3] * e[5] * e[14]
            - e[3] * e[6] * e[13])
            * inverse_det;
        let c03 = (-e[1] * e[6] * e[11] + e[1] * e[7] * e[10] + e[2] * e[5] * e[11]
            - e[2] * e[7] * e[9]
            - e[3] * e[5] * e[10]
            + e[3] * e[6] * e[9])
            * inverse_det;
        let c10 = (-e[4] * e[10] * e[15] + e[4] * e[11] * e[14] + e[6] * e[8] * e[15]
            - e[6] * e[11] * e[12]
            - e[7] * e[8] * e[14]
            + e[7] * e[10] * e[12])
            * inverse_det;
        let c11 = (e[0] * e[10] * e[15] - e[0] * e[11] * e[14] - e[2] * e[8] * e[15]
            + e[2] * e[11] * e[12]
            + e[3] * e[8] * e[14]
            - e[3] * e[10] * e[12])
            * inverse_det;
        let c12 = (-e[0] * e[6] * e[15] + e[0] * e[7] * e[14] + e[2] * e[4] * e[15]
            - e[2] * e[7] * e[12]
            - e[3] * e[4] * e[14]
            + e[3] * e[6] * e[12])
            * inverse_det;
        let c13 = (e[0] * e[6] * e[11] - e[0] * e[7] * e[10] - e[2] * e[4] * e[11]
            + e[2] * e[7] * e[8]
            + e[3] * e[4] * e[10]
            - e[3] * e[6] * e[8])
            * inverse_det;
        let c20 = (e[4] * e[9] * e[15] - e[4] * e[11] * e[13] - e[5] * e[8] * e[15]
            + e[5] * e[11] * e[12]
            + e[7] * e[8] * e[13]
            - e[7] * e[9] * e[12])
            * inverse_det;
        let c21 = (-e[0] * e[9] * e[15] + e[0] * e[11] * e[13] + e[1] * e[8] * e[15]
            - e[1] * e[11] * e[12]
            - e[3] * e[8] * e[13]
            + e[3] * e[9] * e[12])
            * inverse_det;
        let c22 = (e[0] * e[5] * e[15] - e[0] * e[7] * e[13] - e[1] * e[4] * e[15]
            + e[1] * e[7] * e[12]
            + e[3] * e[4] * e[13]
            - e[3] * e[5] * e[12])
            * inverse_det;
        let c23 = (-e[0] * e[5] * e[11] + e[0] * e[7] * e[9] + e[1] * e[4] * e[11]
            - e[1] * e[7] * e[8]
            - e[3] * e[4] * e[9]
            + e[3] * e[5] * e[8])
            * inverse_det;
        let c30 = (-e[4] * e[9] * e[14] + e[4] * e[10] * e[13] + e[5] * e[8] * e[14]
            - e[5] * e[10] * e[12]
            - e[6] * e[8] * e[13]
            + e[6] * e[9] * e[12])
            * inverse_det;
        let c31 = (e[0] * e[9] * e[14] - e[0] * e[10] * e[13] - e[1] * e[8] * e[14]
            + e[1] * e[10] * e[12]
            + e[2] * e[8] * e[13]
            - e[2] * e[9] * e[12])
            * inverse_det;
        let c32 = (-e[0] * e[5] * e[14] + e[0] * e[6] * e[13] + e[1] * e[4] * e[14]
            - e[1] * e[6] * e[12]
            - e[2] * e[4] * e[13]
            + e[2] * e[5] * e[12])
            * inverse_det;
        let c33 = (e[0] * e[5] * e[10] - e[0] * e[6] * e[9] - e[1] * e[4] * e[10]
            + e[1] * e[6] * e[8]
            + e[2] * e[4] * e[9]
            - e[2] * e[5] * e[8])
            * inverse_det;
        self.0 = [
            c00, c01, c02, c03, c10, c11, c12, c13, c20, c21, c22, c23, c30, c31, c32, c33,
        ];
    }

    pub fn multiply_by(&mut self, other: &Matrix44) {
        let e = &self.0;
        let o = &other.0;
        let r00 = e[0] * o[0] + e[1] * o[4] + e[2] * o[8] + e[3] * o[12];
        let r01 = e[0] * o[1] + e[1] * o[5] + e[2] * o[9] + e[3] * o[13];
        let r02 = e[0] * o[2] + e[1] * o[6] + e[2] * o[10] + e[3] * o[14];
        let r03 = e[0] * o[3] + e[1] * o[7] + e[2] * o[11] + e[3] * o[15];
        let r10 = e[4] * o[0] + e[5] * o[4] + e[6] * o[8] + e[7] * o[12];
        let r11 = e[4] * o[1] + e[5] * o[5] + e[6] * o[9] + e[7] * o[13];
        let r12 = e[4] * o[2] + e[5] * o[6] + e[6] * o[10] + e[7] * o[14];
        let r13 = e[4] * o[3] + e[5] * o[7] + e[6] * o[11] + e[7] * o[15];
        let r20 = e[8] * o[0] + e[9] * o[4] + e[10] * o[8] + e[11] * o[12];
        let r21 = e[8] * o[1] + e[9] * o[5] + e[10] * o[9] + e[11] * o[13];
        let r22 = e[8] * o[2] + e[9] * o[6] + e[10] * o[10] + e[11] * o[14];
        let r23 = e[8] * o[3] + e[9] * o[7] + e[10] * o[11] + e[11] * o[15];
        let r30 = e[12] * o[0] + e[13] * o[4] + e[14] * o[8] + e[15] * o[12];
        let r31 = e[12] * o[1] + e[13] * o[5] + e[14] * o[9] + e[15] * o[13];
        let r32 = e[12] * o[2] + e[13] * o[6] + e[14] * o[10] + e[15] * o[14];
        let r33 = e[12] * o[3] + e[13] * o[7] + e[14] * o[11] + e[15] * o[15];
        self.0 = [
            r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33,
        ];
    }
}

pub fn inverse_view_projection(sun: &Matrix44, camera: &Matrix44) -> Matrix44 {
    let mut inverse = *sun;
    inverse.multiply_by(camera);
    inverse.invert();
    inverse
}

#[derive(Clone, Debug)]
pub struct HeightField {
    pub width: i32,
    pub length: i32,
    pub tile_scale: i32,
    pub tile_units: i32,
    pub heights: Vec<i32>,
}

impl HeightField {
    pub fn new(width: i32, length: i32, tile_scale: i32, heights: Vec<i32>) -> Self {
        Self {
            width,
            length,
            tile_scale,
            tile_units: 1 << tile_scale,
            heights,
        }
    }

    fn at(&self, x: i32, y: i32) -> i32 {
        self.heights[(x * (self.length + 1) + y) as usize]
    }

    pub fn average_height(&self, x: i32, y: i32) -> i32 {
        let scaled_x = x >> self.tile_scale;
        let scaled_y = y >> self.tile_scale;
        if scaled_x >= 0
            && scaled_y >= 0
            && scaled_x < self.width
            && scaled_y < self.length
        {
            let units = self.tile_units;
            let offset_x = x & (units - 1);
            let offset_y = y & (units - 1);
            let height1 = self
                .at(scaled_x, scaled_y)
                .wrapping_mul(units - offset_x)
                .wrapping_add(offset_x.wrapping_mul(self.at(scaled_x + 1, scaled_y)))
                >> self.tile_scale;
            let height2 = self
                .at(scaled_x, scaled_y + 1)
                .wrapping_mul(units - offset_x)
                .wrapping_add(self.at(scaled_x + 1, scaled_y + 1).wrapping_mul(offset_x))
                >> self.tile_scale;
            return height2
                .wrapping_mul(offset_y)
                .wrapping_add(height1.wrapping_mul(units - offset_y))
                >> self.tile_scale;
        }
        0
    }
}

pub trait RaySurface {
    fn scene_size(&self) -> (i32, i32);
    fn player_plane(&self) -> i32;
    fn bridged(&self, x: i32, y: i32) -> bool;
    fn average_height(&self, plane: i32, x: i32, y: i32) -> i32;
}

pub fn raycast_terrain_intersection<S: RaySurface>(
    surface: &S,
    delta: [f32; 3],
    end: [f32; 3],
    recursion_depth: i32,
) -> f32 {
    let mut interpolation = 0.0f32;
    let direction_x = end[0] - delta[0];
    let direction_y = end[1] - delta[1];
    let direction_z = end[2] - delta[2];
    let mut prev = [0.0f32; 3];
    let (size_x, size_y) = surface.scene_size();
    while interpolation < 1.1f32 {
        let abs_x = interpolation * direction_x + delta[0];
        let abs_y = direction_y * interpolation + delta[1];
        let abs_z = delta[2] + interpolation * direction_z;
        let scene_x = (abs_x as i32) >> 9;
        let scene_z = (abs_z as i32) >> 9;
        if scene_x > 0 && scene_z > 0 && scene_x < size_x && scene_z < size_y {
            let mut plane = surface.player_plane();
            if plane < 3 && surface.bridged(scene_x, scene_z) {
                plane += 1;
            }
            let height = surface.average_height(plane, abs_x as i32, abs_z as i32);
            if (height as f32) < abs_y {
                if recursion_depth >= 2 {
                    return interpolation - 0.1f32
                        + raycast_terrain_intersection(
                            surface,
                            prev,
                            [abs_x, abs_y, abs_z],
                            recursion_depth - 1,
                        ) * 0.1f32;
                }
                return interpolation;
            }
        }
        prev = [abs_x, abs_y, abs_z];
        interpolation += 0.1f32;
    }
    -1.0f32
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Viewport {
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
}

pub fn unproject_ray(
    inverse: &Matrix44,
    mouse_x: i32,
    mouse_y: i32,
    viewport: &Viewport,
) -> ([f32; 3], [f32; 3]) {
    let viewport_mouse_x = mouse_x - viewport.x;
    let viewport_mouse_y = mouse_y - viewport.y;
    let ndc_x = 2.0f32 * viewport_mouse_x as f32 / viewport.width as f32 - 1.0f32;
    let ndc_y = 2.0f32 * viewport_mouse_y as f32 / viewport.height as f32 - 1.0f32;
    let near = inverse.scale(ndc_x, ndc_y, -1.0);
    let far = inverse.scale(ndc_x, ndc_y, 1.0);
    (
        [near[0] / near[3], near[1] / near[3], near[2] / near[3]],
        [far[0] / far[3], far[1] / far[3], far[2] / far[3]],
    )
}

pub fn tile_under_mouse<S: RaySurface>(
    surface: &S,
    inverse: &Matrix44,
    mouse_x: i32,
    mouse_y: i32,
    viewport: &Viewport,
    player_size: i32,
) -> Option<(i32, i32)> {
    if mouse_x < 0 || mouse_y < 0 {
        return None;
    }
    let (near, far) = unproject_ray(inverse, mouse_x, mouse_y, viewport);
    let ray_t = raycast_terrain_intersection(surface, near, far, 4);
    if ray_t > 0.0f32 {
        let ray_dx = far[0] - near[0];
        let ray_dz = far[2] - near[2];
        let pos_x = (ray_dx * ray_t + near[0]) as i32;
        let pos_y = (ray_t * ray_dz + near[2]) as i32;
        let x = pos_x.wrapping_add((player_size - 1) << 8) >> 9;
        let y = pos_y.wrapping_add((player_size - 1) << 8) >> 9;
        return Some((x, y));
    }
    None
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct MinimapView {
    pub component_x: i32,
    pub component_y: i32,
    pub width: i32,
    pub height: i32,
    pub camera_yaw: i32,
    pub minimap_rotation: i32,
    pub minimap_zoom: i32,
    pub locked_camera: bool,
    pub camera_target_x: i32,
    pub camera_target_y: i32,
    pub player_size: i32,
    pub player_x: i32,
    pub player_z: i32,
}

pub fn minimap_angle(view: &MinimapView) -> usize {
    if view.locked_camera {
        (view.camera_yaw & 0x3fff) as usize
    } else {
        (view.camera_yaw.wrapping_add(view.minimap_rotation) & 0x3fff) as usize
    }
}

pub fn minimap_tile(
    view: &MinimapView,
    mouse_x: i32,
    mouse_y: i32,
    sine: i32,
    cosine: i32,
) -> (i32, i32) {
    let relative_x = mouse_x - view.component_x - view.width / 2;
    let relative_y = mouse_y - view.component_y - view.height / 2;
    let mut sin_angle = sine;
    let mut cos_angle = cosine;
    if !view.locked_camera {
        sin_angle = (view.minimap_zoom + 256).wrapping_mul(sin_angle) >> 8;
        cos_angle = cos_angle.wrapping_mul(view.minimap_zoom + 256) >> 8;
    }
    let world_x = sin_angle
        .wrapping_mul(relative_y)
        .wrapping_add(relative_x.wrapping_mul(cos_angle))
        >> 14;
    let world_y = relative_y
        .wrapping_mul(cos_angle)
        .wrapping_sub(sin_angle.wrapping_mul(relative_x))
        >> 14;
    if view.locked_camera {
        (
            (world_x >> 2) + (view.camera_target_x >> 9),
            (view.camera_target_y >> 9) - (world_y >> 2),
        )
    } else {
        let offset = (view.player_size - 1) * 256;
        (
            ((view.player_x - offset) >> 9) + (world_x >> 2),
            ((view.player_z - offset) >> 9) - (world_y >> 2),
        )
    }
}

pub fn minimap_row_hit(mouse_x: i32, component_x: i32, row_start_x: i32, row_width: i32) -> bool {
    let relative_x = mouse_x - component_x;
    !(relative_x < row_start_x || relative_x > row_start_x + row_width)
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct BoundingBoxPolygon {
    pub top: i32,
    pub left: i32,
    pub bottom: i32,
    pub right: i32,
    pub size: i32,
    pub position_calculated: bool,
}

impl BoundingBoxPolygon {
    pub fn hovered(&self, mouse_x: i32, mouse_y: i32) -> bool {
        if !self.position_calculated {
            return false;
        }
        let width = self.right.wrapping_sub(self.left);
        let length = self.top.wrapping_sub(self.bottom);
        let size2d = width
            .wrapping_mul(width)
            .wrapping_add(length.wrapping_mul(length));
        let size_sq = self.size.wrapping_mul(self.size);
        let mut reference = width
            .wrapping_mul(mouse_x)
            .wrapping_add(length.wrapping_mul(mouse_y))
            .wrapping_sub(
                width
                    .wrapping_mul(self.left)
                    .wrapping_add(length.wrapping_mul(self.bottom)),
            );
        if reference <= 0 {
            let offset_left = self.left.wrapping_sub(mouse_x);
            let offset_bottom = self.bottom.wrapping_sub(mouse_y);
            return offset_left
                .wrapping_mul(offset_left)
                .wrapping_add(offset_bottom.wrapping_mul(offset_bottom))
                < size_sq;
        }
        if reference > size2d {
            let relative_x = self.right.wrapping_sub(mouse_x);
            let relative_y = self.top.wrapping_sub(mouse_y);
            return relative_x
                .wrapping_mul(relative_x)
                .wrapping_add(relative_y.wrapping_mul(relative_y))
                < size_sq;
        }
        reference = (reference << 10).wrapping_div(size2d);
        let l = (width.wrapping_mul(reference) >> 10)
            .wrapping_add(self.left)
            .wrapping_sub(mouse_x);
        let t = (length.wrapping_mul(reference) >> 10)
            .wrapping_add(self.bottom)
            .wrapping_sub(mouse_y);
        l.wrapping_mul(l).wrapping_add(t.wrapping_mul(t)) < size_sq
    }
}

pub fn entity_hovered(
    polygons: &[BoundingBoxPolygon],
    size_shift: i32,
    hover_offset_x: i32,
    hover_offset_y: i32,
    mouse_x: i32,
    mouse_y: i32,
    mut projection_hit: impl FnMut() -> bool,
) -> bool {
    for polygon in polygons {
        let mut shifted = *polygon;
        shifted.size <<= size_shift;
        if shifted.hovered(mouse_x + hover_offset_x, mouse_y + hover_offset_y) && projection_hit() {
            return true;
        }
    }
    false
}
