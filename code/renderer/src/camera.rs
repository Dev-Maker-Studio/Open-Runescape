use crate::math::{self, Mat4};

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

pub fn theta(rotation: i32) -> f32 {
    let masked = rotation & 0x3fff;
    (masked as f32 / 16384.0) * std::f32::consts::TAU
}

impl Matrix43 {
    pub const IDENTITY: Matrix43 = Matrix43 {
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

    pub fn set_view_position(&mut self, x: f32, y: f32, z: f32) {
        *self = Matrix43::IDENTITY;
        self.xd = x;
        self.yd = y;
        self.zd = z;
    }

    pub fn rotate(&mut self, ax: f32, ay: f32, az: f32, angle: f32) {
        let cos = f64::from(angle).cos() as f32;
        let sin = f64::from(angle).sin() as f32;
        let m00 = cos + ax * ax * (1.0 - cos);
        let m01 = az * sin + ay * ax * (1.0 - cos);
        let m02 = -ay * sin + az * ax * (1.0 - cos);
        let m10 = -az * sin + ax * ay * (1.0 - cos);
        let m11 = cos + ay * ay * (1.0 - cos);
        let m12 = ax * sin + az * ay * (1.0 - cos);
        let m20 = ay * sin + ax * az * (1.0 - cos);
        let m21 = -ax * sin + ay * az * (1.0 - cos);
        let m22 = cos + az * az * (1.0 - cos);

        let nxa = self.xa * m00 + self.ya * m10 + self.za * m20;
        let nya = self.xa * m01 + self.ya * m11 + self.za * m21;
        let nza = self.xa * m02 + self.ya * m12 + self.za * m22;
        self.xa = nxa;
        self.ya = nya;
        self.za = nza;

        let nxb = self.xb * m00 + self.yb * m10 + self.zb * m20;
        let nyb = self.xb * m01 + self.yb * m11 + self.zb * m21;
        let nzb = self.xb * m02 + self.yb * m12 + self.zb * m22;
        self.xb = nxb;
        self.yb = nyb;
        self.zb = nzb;

        let nxc = self.xc * m00 + self.yc * m10 + self.zc * m20;
        let nyc = self.xc * m01 + self.yc * m11 + self.zc * m21;
        let nzc = self.xc * m02 + self.yc * m12 + self.zc * m22;
        self.xc = nxc;
        self.yc = nyc;
        self.zc = nzc;

        let nxd = self.xd * m00 + self.yd * m10 + self.zd * m20;
        let nyd = self.xd * m01 + self.yd * m11 + self.zd * m21;
        let nzd = self.xd * m02 + self.yd * m12 + self.zd * m22;
        self.xd = nxd;
        self.yd = nyd;
        self.zd = nzd;
    }

    pub fn to_matrix44(&self) -> Mat4 {
        [
            self.xa, self.ya, self.za, 0.0, self.xb, self.yb, self.zb, 0.0, self.xc, self.yc,
            self.zc, 0.0, self.xd, self.yd, self.zd, 1.0,
        ]
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct CameraPose {
    pub x: i32,
    pub y: i32,
    pub z: i32,
    pub yaw: i32,
    pub roll: i32,
    pub pitch: i32,
}

pub fn view_matrix(pose: &CameraPose) -> Matrix43 {
    let mut m = Matrix43::IDENTITY;
    m.set_view_position(-(pose.x as f32), -(pose.z as f32), -(pose.y as f32));
    m.rotate(0.0, -1.0, 0.0, theta(pose.roll.wrapping_neg() & 0x3fff));
    m.rotate(-1.0, 0.0, 0.0, theta(pose.yaw.wrapping_neg() & 0x3fff));
    m.rotate(0.0, 0.0, -1.0, theta(pose.pitch.wrapping_neg() & 0x3fff));
    m
}

pub fn multiply_by(a: &Mat4, b: &Mat4) -> Mat4 {
    let mut r = [0.0f32; 16];
    for row in 0..4 {
        for col in 0..4 {
            let i = row * 4;
            r[i + col] = a[i] * b[col]
                + a[i + 1] * b[4 + col]
                + a[i + 2] * b[8 + col]
                + a[i + 3] * b[12 + col];
        }
    }
    r
}

#[allow(clippy::too_many_arguments)]
pub fn set_perspective(
    center_x: f32,
    center_y: f32,
    focal_x: f32,
    focal_y: f32,
    view_range_start: f32,
    view_range_end: f32,
    width: f32,
    height: f32,
) -> Mat4 {
    let mut e = [0.0f32; 16];
    e[0] = focal_x * 2.0 / width;
    e[5] = focal_y * 2.0 / height;
    e[8] = 2.0 * center_x / width - 1.0;
    e[9] = 2.0 * center_y / height - 1.0;
    e[10] = (view_range_end + view_range_start) / (view_range_end - view_range_start);
    e[11] = 1.0;
    e[14] = 2.0 * view_range_end * view_range_start / (view_range_start - view_range_end);
    e
}

pub fn near_clip_distance(m: &Mat4) -> f32 {
    -(m[15] + m[14]) / (m[11] + m[10])
}

pub fn depth_perspective_cd(m: &Mat4) -> f32 {
    (m[14] - m[15]) / (m[11] - m[10])
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Viewport {
    pub width: i32,
    pub height: i32,
    pub scaled_fov: i32,
    pub view_range_start: i32,
    pub view_range_end: i32,
}

pub const FOG_DISTANCE_MULTIPLIER: i32 = 10;
pub const VIEW_RANGE_START: i32 = 200;

pub fn view_range(scene_size_x: i32, extend_build_area: bool, fog_multiplier: i32) -> (i32, i32) {
    let mut end = ((34.46f64 * f64::from(scene_size_x)) as i32) << 2;
    if extend_build_area {
        end = end.wrapping_add(512);
    }
    end = end.wrapping_add(3072i32.wrapping_mul(fog_multiplier));
    (VIEW_RANGE_START, end)
}

pub fn projection(v: &Viewport) -> Mat4 {
    let focal = (v.scaled_fov << 1) as f32;
    set_perspective(
        (v.width / 2) as f32,
        (v.height / 2) as f32,
        focal,
        focal,
        v.view_range_start as f32,
        v.view_range_end as f32,
        v.width as f32,
        v.height as f32,
    )
}

pub fn view_projection(view: &Matrix43, projection: &Mat4) -> Mat4 {
    multiply_by(&view.to_matrix44(), projection)
}

pub fn batcher_view_projection(vp: &Mat4) -> Mat4 {
    let mut f = *vp;
    f[1] = -f[1];
    f[5] = -f[5];
    f[9] = -f[9];
    f[13] = -f[13];
    f
}

pub fn wgpu_view_projection(vp: &Mat4) -> Mat4 {
    let mut m = batcher_view_projection(vp);
    math::gl_to_wgpu_depth(&mut m);
    m
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Fog {
    pub enabled: bool,
    pub start: f32,
    pub end: f32,
    pub color: [f32; 3],
}

pub fn color_channels(rgb: i32) -> [f32; 3] {
    [
        (rgb & 0xff0000) as f32 / 1.671_168E7_f32,
        (rgb & 0xff00) as f32 / 65280.0,
        (rgb & 0xff) as f32 / 255.0,
    ]
}

pub fn batcher_color(rgb: i32) -> [f32; 3] {
    [
        ((rgb >> 16) & 0xff) as f32 / 255.0,
        ((rgb >> 8) & 0xff) as f32 / 255.0,
        (rgb & 0xff) as f32 / 255.0,
    ]
}

pub fn fog_range(model: &Mat4, density: i32, offset: i32) -> (f32, f32) {
    let far = depth_perspective_cd(model);
    let min_start = near_clip_distance(model);
    let end = far - offset as f32 - 0.0;
    let mut start = end - density as f32 * 1.0;
    if start < min_start {
        start = min_start;
    }
    (start, end)
}

pub fn fog(model: &Mat4, enabled: bool, color: i32, density: i32, offset: i32) -> Fog {
    let (start, end) = fog_range(model, density, offset);
    Fog {
        enabled: enabled && density >= 0,
        start,
        end,
        color: batcher_color(color),
    }
}

pub fn environment_density(fog_depth: i32, fog_preference_on: bool) -> i32 {
    if fog_preference_on {
        fog_depth.wrapping_add(256) << 2
    } else {
        -1
    }
}

pub fn clear_color(argb: i32) -> [f32; 4] {
    let c = color_channels(argb);
    [c[0], c[1], c[2], ((argb as u32) >> 24) as f32 / 255.0]
}

pub fn clear_color_opaque(argb: i32) -> [f32; 4] {
    let c = clear_color(argb);
    [c[0], c[1], c[2], 1.0]
}

pub const DEFAULT_FOG_COLOR: i32 = 13_156_520;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Frame {
    pub vp: Mat4,
    pub view: Mat4,
    pub fog: Fog,
    pub clear: [f32; 4],
}

pub fn frame(
    pose: &CameraPose,
    viewport: &Viewport,
    fog_enabled: bool,
    fog_color: i32,
    fog_density: i32,
    fog_offset: i32,
    clear_argb: i32,
) -> Frame {
    let view = view_matrix(pose);
    let proj = projection(viewport);
    let native_vp = view_projection(&view, &proj);
    Frame {
        vp: wgpu_view_projection(&native_vp),
        view: view.to_matrix44(),
        fog: fog(&proj, fog_enabled, fog_color, fog_density, fog_offset),
        clear: clear_color_opaque(clear_argb),
    }
}

pub fn default_pose(tile_x: i32, tile_z: i32, height: i32) -> CameraPose {
    CameraPose {
        x: tile_x * 512 + 256,
        y: tile_z * 512 + 256 - 1536,
        z: height,
        yaw: 1664,
        roll: 0,
        pitch: 0,
    }
}
