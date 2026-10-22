#![allow(clippy::excessive_precision, clippy::approx_constant)]

const ATAN_HI: [f64; 4] = [
    4.63647609000806093515e-01,
    7.85398163397448278999e-01,
    9.82793723247329054082e-01,
    1.57079632679489655800e+00,
];
const ATAN_LO: [f64; 4] = [
    2.26987774529616870924e-17,
    3.06161699786838301793e-17,
    1.39033110312309984516e-17,
    6.12323399573676603587e-17,
];
const A_T: [f64; 11] = [
    3.33333333333329318027e-01,
    -1.99999999998764832476e-01,
    1.42857142725034663711e-01,
    -1.11111104054623557880e-01,
    9.09088713343650656196e-02,
    -7.69187620504482999495e-02,
    6.66107313738753120669e-02,
    -5.83357013379057348645e-02,
    4.97687799461593236017e-02,
    -3.65315727442169155270e-02,
    1.62858201153657823623e-02,
];
const TINY: f64 = 1.0e-300;
const PI_O_4: f64 = 7.8539816339744827900e-01;
const PI_O_2: f64 = 1.5707963267948965580e+00;
const PI: f64 = 3.1415926535897931160e+00;
const PI_LO: f64 = 1.2246467991473531772e-16;

fn high(value: f64) -> i32 {
    (value.to_bits() >> 32) as u32 as i32
}

fn low(value: f64) -> u32 {
    value.to_bits() as u32
}

fn atan(input: f64) -> f64 {
    let mut x = input;
    let hx = high(x);
    let ix = hx & 0x7fff_ffff;
    if ix >= 0x4410_0000 {
        if ix > 0x7ff0_0000 || (ix == 0x7ff0_0000 && low(x) != 0) {
            return x + x;
        }
        return if hx > 0 {
            ATAN_HI[3] + ATAN_LO[3]
        } else {
            -ATAN_HI[3] - ATAN_LO[3]
        };
    }
    let id: i32;
    if ix < 0x3fdc_0000 {
        if ix < 0x3e20_0000 && 1.0e300 + x > 1.0 {
            return x;
        }
        id = -1;
    } else {
        x = x.abs();
        if ix < 0x3ff3_0000 {
            if ix < 0x3fe6_0000 {
                id = 0;
                x = (2.0 * x - 1.0) / (2.0 + x);
            } else {
                id = 1;
                x = (x - 1.0) / (x + 1.0);
            }
        } else if ix < 0x4003_8000 {
            id = 2;
            x = (x - 1.5) / (1.0 + 1.5 * x);
        } else {
            id = 3;
            x = -1.0 / x;
        }
    }
    let z = x * x;
    let w = z * z;
    let s1 = z * (A_T[0] + w * (A_T[2] + w * (A_T[4] + w * (A_T[6] + w * (A_T[8] + w * A_T[10])))));
    let s2 = w * (A_T[1] + w * (A_T[3] + w * (A_T[5] + w * (A_T[7] + w * A_T[9]))));
    if id < 0 {
        x - x * (s1 + s2)
    } else {
        let id = id as usize;
        let z = ATAN_HI[id] - ((x * (s1 + s2) - ATAN_LO[id]) - x);
        if hx < 0 {
            -z
        } else {
            z
        }
    }
}

fn negate(value: f64) -> f64 {
    f64::from_bits(value.to_bits() ^ 0x8000_0000_0000_0000)
}

pub fn atan2(y: f64, x: f64) -> f64 {
    let hx = high(x);
    let lx = low(x);
    let hy = high(y);
    let ly = low(y);
    let ix = hx & 0x7fff_ffff;
    let iy = hy & 0x7fff_ffff;
    let nan_x = (ix as u32 | ((lx | lx.wrapping_neg()) >> 31)) > 0x7ff0_0000;
    let nan_y = (iy as u32 | ((ly | ly.wrapping_neg()) >> 31)) > 0x7ff0_0000;
    if nan_x || nan_y {
        return x + y;
    }
    if (hx.wrapping_sub(0x3ff0_0000) as u32 | lx) == 0 {
        return atan(y);
    }
    let m = ((hy >> 31) & 1) | ((hx >> 30) & 2);
    if (iy as u32 | ly) == 0 {
        return match m {
            0 | 1 => y,
            2 => PI + TINY,
            _ => -PI - TINY,
        };
    }
    if (ix as u32 | lx) == 0 {
        return if hy < 0 { -PI_O_2 - TINY } else { PI_O_2 + TINY };
    }
    if ix == 0x7ff0_0000 {
        if iy == 0x7ff0_0000 {
            return match m {
                0 => PI_O_4 + TINY,
                1 => -PI_O_4 - TINY,
                2 => 3.0 * PI_O_4 + TINY,
                _ => -3.0 * PI_O_4 - TINY,
            };
        }
        return match m {
            0 => 0.0,
            1 => -0.0,
            2 => PI + TINY,
            _ => -PI - TINY,
        };
    }
    if iy == 0x7ff0_0000 {
        return if hy < 0 { -PI_O_2 - TINY } else { PI_O_2 + TINY };
    }
    let k = (iy - ix) >> 20;
    let z = if k > 60 {
        PI_O_2 + 0.5 * PI_LO
    } else if hx < 0 && k < -60 {
        0.0
    } else {
        atan((y / x).abs())
    };
    match m {
        0 => z,
        1 => negate(z),
        2 => PI - (z - PI_LO),
        _ => (z - PI_LO) - PI,
    }
}
