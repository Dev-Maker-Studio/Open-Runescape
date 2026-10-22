use std::sync::OnceLock;

pub const STEP: f64 = 3.834951969714103E-4;

pub struct Tables {
    pub sine: Vec<i32>,
    pub cosine: Vec<i32>,
}

pub fn tables() -> &'static Tables {
    static TABLES: OnceLock<Tables> = OnceLock::new();
    TABLES.get_or_init(|| {
        let mut sine = Vec::with_capacity(16384);
        let mut cosine = Vec::with_capacity(16384);
        for i in 0..16384 {
            let angle = f64::from(i) * STEP;
            sine.push((16384.0 * libm::sin(angle)) as i32);
            cosine.push((16384.0 * libm::cos(angle)) as i32);
        }
        Tables { sine, cosine }
    })
}

pub fn sine() -> &'static [i32] {
    &tables().sine
}

pub fn cosine() -> &'static [i32] {
    &tables().cosine
}
