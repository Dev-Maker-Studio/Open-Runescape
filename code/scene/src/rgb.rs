use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

pub const HSL_RGB_TABLE_LEN: usize = 65_536;
const MIN_GAMMA: f64 = native_gamma(0.0);
const MAX_GAMMA: f64 = native_gamma(f64::from_bits(1.0_f64.to_bits() - 1));

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum HslRgbError {
    InvalidRandomUnit,
    InvalidGamma,
}

impl Display for HslRgbError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::InvalidRandomUnit => {
                write!(f, "HSL RGB random unit must be finite and in [0, 1)")
            }
            Self::InvalidGamma => write!(
                f,
                "HSL RGB gamma must be finite and in [{MIN_GAMMA}, {MAX_GAMMA}]"
            ),
        }
    }
}

impl Error for HslRgbError {}

#[derive(Clone, Debug, PartialEq)]
pub struct HslRgbLut {
    gamma: f64,
    hsl_to_rgb: Box<[u32]>,
    unsigned_hsl_to_rgb: Box<[u32]>,
}

impl HslRgbLut {
    pub fn sample(mut random_unit: impl FnMut() -> f64) -> Result<Self, HslRgbError> {
        Self::from_random_unit(random_unit())
    }

    pub fn from_random_unit(random_unit: f64) -> Result<Self, HslRgbError> {
        if !random_unit.is_finite() || !(0.0..1.0).contains(&random_unit) {
            return Err(HslRgbError::InvalidRandomUnit);
        }
        Self::from_gamma(native_gamma(random_unit))
    }

    pub fn from_gamma(gamma: f64) -> Result<Self, HslRgbError> {
        if !gamma.is_finite() || !(MIN_GAMMA..=MAX_GAMMA).contains(&gamma) {
            return Err(HslRgbError::InvalidGamma);
        }
        let mut hsl_to_rgb = Vec::with_capacity(HSL_RGB_TABLE_LEN);
        let mut unsigned_hsl_to_rgb = Vec::with_capacity(HSL_RGB_TABLE_LEN);
        for hsl_index in 0..HSL_RGB_TABLE_LEN {
            let hue = ((hsl_index >> 10) & 0x3f) as f64 / 64.0 + 0.0078125;
            let saturation = 0.0625 + ((hsl_index >> 7) & 0x7) as f64 / 8.0;
            let lightness = (hsl_index & 0x7f) as f64 / 128.0;
            let mut red = lightness;
            let mut green = lightness;
            let mut blue = lightness;
            if saturation != 0.0 {
                let chroma = if lightness < 0.5 {
                    lightness * (saturation + 1.0)
                } else {
                    saturation + lightness - lightness * saturation
                };
                let min_component = lightness * 2.0 - chroma;
                let mut hue_red = 0.3333333333333333 + hue;
                if hue_red > 1.0 {
                    hue_red -= 1.0;
                }
                let mut hue_blue = hue - 0.3333333333333333;
                if hue_blue < 0.0 {
                    hue_blue += 1.0;
                }
                red = if hue_red * 6.0 < 1.0 {
                    min_component + (chroma - min_component) * 6.0 * hue_red
                } else if 2.0 * hue_red < 1.0 {
                    chroma
                } else if 3.0 * hue_red < 2.0 {
                    min_component + 6.0 * (0.6666666666666666 - hue_red) * (chroma - min_component)
                } else {
                    min_component
                };
                green = if 6.0 * hue < 1.0 {
                    min_component + 6.0 * (chroma - min_component) * hue
                } else if hue * 2.0 < 1.0 {
                    chroma
                } else if 3.0 * hue < 2.0 {
                    6.0 * (0.6666666666666666 - hue) * (chroma - min_component) + min_component
                } else {
                    min_component
                };
                blue = if hue_blue * 6.0 < 1.0 {
                    hue_blue * 6.0 * (chroma - min_component) + min_component
                } else if hue_blue * 2.0 < 1.0 {
                    chroma
                } else if hue_blue * 3.0 < 2.0 {
                    6.0 * (0.6666666666666666 - hue_blue) * (chroma - min_component) + min_component
                } else {
                    min_component
                };
            }
            red = red.powf(gamma);
            green = green.powf(gamma);
            blue = blue.powf(gamma);
            let red_int = (red * 256.0) as u32;
            let green_int = (green * 256.0) as u32;
            let blue_int = (256.0 * blue) as u32;
            hsl_to_rgb.push((blue_int + (green_int << 8) + (red_int << 16)) & 0xffffff);
            unsigned_hsl_to_rgb.push(red_int + (green_int << 8) + (blue_int << 16));
        }
        Ok(Self {
            gamma,
            hsl_to_rgb: hsl_to_rgb.into_boxed_slice(),
            unsigned_hsl_to_rgb: unsigned_hsl_to_rgb.into_boxed_slice(),
        })
    }

    pub fn gamma(&self) -> f64 {
        self.gamma
    }

    pub fn rgb(&self, hsl: i32) -> Option<u32> {
        let index = usize::try_from(hsl).ok()?;
        self.hsl_to_rgb.get(index).copied()
    }

    pub fn unsigned_rgb(&self, hsl: i32) -> Option<u32> {
        let index = usize::try_from(hsl).ok()?;
        self.unsigned_hsl_to_rgb.get(index).copied()
    }

    pub fn hsl_to_rgb(&self) -> &[u32] {
        &self.hsl_to_rgb
    }

    pub fn unsigned_hsl_to_rgb(&self) -> &[u32] {
        &self.unsigned_hsl_to_rgb
    }
}

const fn native_gamma(random_unit: f64) -> f64 {
    0.7 + (random_unit * 0.03 - 0.015)
}
