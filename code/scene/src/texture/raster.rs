use std::sync::OnceLock;

pub struct Raster {
    pub width: i32,
    pub height: i32,
    pub row_end: i32,
    pub column_end: i32,
    pub scaled_width: i32,
    pub horizontal: Vec<i32>,
    pub vertical: Vec<i32>,
}

impl Raster {
    pub fn new(width: i32, height: i32) -> Self {
        let horizontal = (0..width).map(|i| (i << 12) / width).collect();
        let vertical = (0..height).map(|i| (i << 12) / height).collect();
        Raster {
            width,
            height,
            row_end: width - 1,
            column_end: height - 1,
            scaled_width: width * 32,
            horizontal,
            vertical,
        }
    }
}

pub struct Palettes {
    pub sin: [i32; 256],
    pub cos: [i32; 256],
}

pub fn palettes() -> &'static Palettes {
    static TABLE: OnceLock<Palettes> = OnceLock::new();
    TABLE.get_or_init(|| {
        let mut sin = [0i32; 256];
        let mut cos = [0i32; 256];
        for i in 0..256 {
            let angle = 6.283185307179586 * (i as f64 / 255.0);
            sin[i] = (angle.sin() * 4096.0) as i32;
            cos[i] = (angle.cos() * 4096.0) as i32;
        }
        Palettes { sin, cos }
    })
}

pub fn brightness_table(brightness: f64) -> [i32; 256] {
    let mut table = [0i32; 256];
    for (grade, slot) in table.iter_mut().enumerate() {
        let value = ((grade as f64 / 255.0).powf(brightness) * 255.0) as i32;
        *slot = value.min(255);
    }
    table
}

fn s_curve(t: i32) -> i32 {
    let t3 = t.wrapping_mul(t.wrapping_mul(t) >> 12) >> 12;
    let six = t * 6 - 61440;
    let poly = (t.wrapping_mul(six) >> 12) + 40960;
    poly.wrapping_mul(t3) >> 12
}

pub fn sine_wave() -> &'static [i32; 4096] {
    static TABLE: OnceLock<[i32; 4096]> = OnceLock::new();
    TABLE.get_or_init(|| {
        let mut table = [0i32; 4096];
        for (i, slot) in table.iter_mut().enumerate() {
            *slot = s_curve(i as i32);
        }
        table
    })
}

pub fn clamp(value: i32, min: i32, max: i32) -> i32 {
    if value < min {
        min
    } else {
        value.min(max)
    }
}
