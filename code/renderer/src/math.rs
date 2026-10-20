pub type Mat4 = [f32; 16];

pub const IDENTITY: Mat4 = [1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1.];

pub fn mul(a: &Mat4, b: &Mat4) -> Mat4 {
    let mut r = [0.; 16];
    for c in 0..4 {
        for row in 0..4 {
            r[c * 4 + row] = (0..4).map(|k| a[k * 4 + row] * b[c * 4 + k]).sum();
        }
    }
    r
}

pub fn perspective(fovy: f32, aspect: f32, near: f32, far: f32) -> Mat4 {
    let f = 1. / (fovy * 0.5).tan();
    let mut m = [0.; 16];
    m[0] = f / aspect;
    m[5] = f;
    m[10] = far / (near - far);
    m[11] = -1.;
    m[14] = near * far / (near - far);
    m
}

pub fn ortho_top_left(w: f32, h: f32) -> Mat4 {
    let mut m = IDENTITY;
    m[0] = 2. / w;
    m[5] = -2. / h;
    m[12] = -1.;
    m[13] = 1.;
    m
}

fn norm(v: [f32; 3]) -> [f32; 3] {
    let l = (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).sqrt();
    [v[0] / l, v[1] / l, v[2] / l]
}

fn cross(a: [f32; 3], b: [f32; 3]) -> [f32; 3] {
    [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
}

fn dot(a: [f32; 3], b: [f32; 3]) -> f32 {
    a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
}

pub fn look_at(eye: [f32; 3], at: [f32; 3]) -> Mat4 {
    let f = norm([at[0] - eye[0], at[1] - eye[1], at[2] - eye[2]]);
    let s = norm(cross(f, [0., 1., 0.]));
    let u = cross(s, f);
    [
        s[0], u[0], -f[0], 0., s[1], u[1], -f[1], 0., s[2], u[2], -f[2], 0., -dot(s, eye), -dot(u, eye), dot(f, eye), 1.,
    ]
}

pub fn translate(x: f32, y: f32, z: f32) -> Mat4 {
    let mut m = IDENTITY;
    m[12] = x;
    m[13] = y;
    m[14] = z;
    m
}

pub fn rotate_y(a: f32) -> Mat4 {
    let (s, c) = a.sin_cos();
    let mut m = IDENTITY;
    m[0] = c;
    m[2] = -s;
    m[8] = s;
    m[10] = c;
    m
}

pub fn rotate_vec(m: &Mat4, v: [f32; 3]) -> [f32; 3] {
    let r = |i: usize| m[i] * v[0] + m[4 + i] * v[1] + m[8 + i] * v[2];
    norm([r(0), r(1), r(2)])
}

pub fn normal_matrix(m: &Mat4) -> [[f32; 4]; 3] {
    [[m[0], m[1], m[2], 0.], [m[4], m[5], m[6], 0.], [m[8], m[9], m[10], 0.]]
}

pub fn gl_to_wgpu_depth(m: &mut Mat4) {
    for c in 0..4 {
        m[c * 4 + 2] = 0.5 * m[c * 4 + 2] + 0.5 * m[c * 4 + 3];
    }
}

pub fn scale(s: f32) -> Mat4 {
    let mut m = IDENTITY;
    m[0] = s;
    m[5] = s;
    m[10] = s;
    m
}

pub fn ortho(half: f32, near: f32, far: f32) -> Mat4 {
    let mut m = IDENTITY;
    m[0] = 1. / half;
    m[5] = 1. / half;
    m[10] = -2. / (far - near);
    m[14] = -(far + near) / (far - near);
    m
}

pub fn invert(m: &Mat4) -> Mat4 {
    let a = |r: usize, c: usize| m[c * 4 + r];
    let s = [a(0, 0) * a(1, 1) - a(1, 0) * a(0, 1), a(0, 0) * a(1, 2) - a(1, 0) * a(0, 2), a(0, 0) * a(1, 3) - a(1, 0) * a(0, 3),
        a(0, 1) * a(1, 2) - a(1, 1) * a(0, 2), a(0, 1) * a(1, 3) - a(1, 1) * a(0, 3), a(0, 2) * a(1, 3) - a(1, 2) * a(0, 3)];
    let c = [a(2, 0) * a(3, 1) - a(3, 0) * a(2, 1), a(2, 0) * a(3, 2) - a(3, 0) * a(2, 2), a(2, 0) * a(3, 3) - a(3, 0) * a(2, 3),
        a(2, 1) * a(3, 2) - a(3, 1) * a(2, 2), a(2, 1) * a(3, 3) - a(3, 1) * a(2, 3), a(2, 2) * a(3, 3) - a(3, 2) * a(2, 3)];
    let det = s[0] * c[5] - s[1] * c[4] + s[2] * c[3] + s[3] * c[2] - s[4] * c[1] + s[5] * c[0];
    let inv = [
        [a(1, 1) * c[5] - a(1, 2) * c[4] + a(1, 3) * c[3], -a(0, 1) * c[5] + a(0, 2) * c[4] - a(0, 3) * c[3], a(3, 1) * s[5] - a(3, 2) * s[4] + a(3, 3) * s[3], -a(2, 1) * s[5] + a(2, 2) * s[4] - a(2, 3) * s[3]],
        [-a(1, 0) * c[5] + a(1, 2) * c[2] - a(1, 3) * c[1], a(0, 0) * c[5] - a(0, 2) * c[2] + a(0, 3) * c[1], -a(3, 0) * s[5] + a(3, 2) * s[2] - a(3, 3) * s[1], a(2, 0) * s[5] - a(2, 2) * s[2] + a(2, 3) * s[1]],
        [a(1, 0) * c[4] - a(1, 1) * c[2] + a(1, 3) * c[0], -a(0, 0) * c[4] + a(0, 1) * c[2] - a(0, 3) * c[0], a(3, 0) * s[4] - a(3, 1) * s[2] + a(3, 3) * s[0], -a(2, 0) * s[4] + a(2, 1) * s[2] - a(2, 3) * s[0]],
        [-a(1, 0) * c[3] + a(1, 1) * c[1] - a(1, 2) * c[0], a(0, 0) * c[3] - a(0, 1) * c[1] + a(0, 2) * c[0], -a(3, 0) * s[3] + a(3, 1) * s[1] - a(3, 2) * s[0], a(2, 0) * s[3] - a(2, 1) * s[1] + a(2, 2) * s[0]],
    ];
    let mut out = [0.; 16];
    for r in 0..4 {
        for col in 0..4 {
            out[col * 4 + r] = inv[r][col] / det;
        }
    }
    out
}
