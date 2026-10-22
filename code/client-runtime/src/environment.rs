use std::sync::Arc;

const MULTIPLIER: i64 = 0x5DEECE66D;
const ADDEND: i64 = 0xB;
const MASK: i64 = (1 << 48) - 1;
const HSV_TABLE_SIZE: usize = 65536;
const DEFAULT_LANGUAGE: i32 = 0;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct JavaRandom {
    seed: i64,
}

impl JavaRandom {
    pub fn new(seed: i64) -> Self {
        Self {
            seed: (seed ^ MULTIPLIER) & MASK,
        }
    }

    fn next(&mut self, bits: u32) -> i32 {
        self.seed = self.seed.wrapping_mul(MULTIPLIER).wrapping_add(ADDEND) & MASK;
        (self.seed >> (48 - bits)) as i32
    }

    pub fn next_double(&mut self) -> f64 {
        let high = i64::from(self.next(26)) << 27;
        let low = i64::from(self.next(27));
        (high + low) as f64 * (1.0 / (1u64 << 53) as f64)
    }
}

pub fn hsv_to_rgb_table(gamma: f64) -> Arc<[i32]> {
    let mut table = Vec::with_capacity(HSV_TABLE_SIZE);
    for hue_saturation in 0..512i32 {
        let hue_angle = 360.0f32 * ((hue_saturation >> 3) as f32 / 64.0f32 + 0.0078125f32);
        let saturation = (hue_saturation & 7) as f32 / 8.0f32 + 0.0625f32;
        for value_index in 0..128i32 {
            let value = value_index as f32 / 128.0f32;
            let segment = hue_angle / 60.0f32;
            let segment_int = segment as i32;
            let region = segment_int % 6;
            let fraction = segment - segment_int as f32;
            let minimum = value * (1.0f32 - saturation);
            let falling = value * (1.0f32 - saturation * fraction);
            let rising = (1.0f32 - saturation * (1.0f32 - fraction)) * value;
            let (red, green, blue) = match region {
                0 => (value, rising, minimum),
                1 => (falling, value, minimum),
                2 => (minimum, value, rising),
                3 => (minimum, falling, value),
                4 => (rising, minimum, value),
                5 => (value, minimum, falling),
                _ => (0.0, 0.0, 0.0),
            };
            let correct = |component: f32| f64::from(component).powf(gamma) as f32;
            let red = (correct(red) * 256.0f32) as i32;
            let green = (256.0f32 * correct(green)) as i32;
            let blue = (256.0f32 * correct(blue)) as i32;
            table.push(
                blue.wrapping_add(green << 8)
                    .wrapping_add(red << 16)
                    .wrapping_add(!0xffffff),
            );
        }
    }
    table.into()
}

pub fn gamma_from_draw(draw: f64) -> f64 {
    0.7 + (draw * 0.03 - 0.015)
}

#[derive(Debug)]
pub struct Environment {
    epoch_base_ms: Option<i64>,
    offset_ms: i64,
    language: Option<i32>,
    random: JavaRandom,
    hsv: Option<Arc<[i32]>>,
}

impl Environment {
    pub fn new(epoch_ms: Option<i64>) -> Self {
        let seed = epoch_ms.unwrap_or(20120727) ^ 0x5851F42D4C957F2D;
        Self {
            epoch_base_ms: epoch_ms,
            offset_ms: 0,
            language: Some(DEFAULT_LANGUAGE),
            random: JavaRandom::new(seed),
            hsv: None,
        }
    }

    pub fn native() -> Self {
        #[cfg(not(target_arch = "wasm32"))]
        {
            let now = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .ok()
                .and_then(|elapsed| i64::try_from(elapsed.as_millis()).ok());
            Self::new(now)
        }
        #[cfg(target_arch = "wasm32")]
        {
            Self::new(None)
        }
    }

    pub fn set_epoch_base(&mut self, epoch_ms: i64, elapsed_ms: u64) {
        self.epoch_base_ms = Some(epoch_ms.saturating_sub(elapsed_ms as i64));
    }

    pub fn set_utc_offset(&mut self, offset_ms: i64) {
        self.offset_ms = offset_ms;
    }

    pub fn set_language(&mut self, language: Option<i32>) {
        self.language = language;
    }

    pub fn seed(&mut self, seed: i64) {
        self.random = JavaRandom::new(seed);
    }

    pub fn epoch_ms(&self, elapsed_ms: u64) -> Option<i64> {
        self.epoch_base_ms
            .map(|base| base.saturating_add(elapsed_ms as i64))
    }

    pub fn offset_ms(&self) -> i64 {
        self.offset_ms
    }

    pub fn language(&self) -> Option<i32> {
        self.language
    }

    pub fn draw(&mut self) -> f64 {
        self.random.next_double()
    }

    pub fn hsv_table(&mut self) -> Arc<[i32]> {
        if self.hsv.is_none() {
            let gamma = gamma_from_draw(self.random.next_double());
            self.hsv = Some(hsv_to_rgb_table(gamma));
        }
        Arc::clone(self.hsv.as_ref().expect("table built above"))
    }
}

#[cfg(test)]
mod tests;
