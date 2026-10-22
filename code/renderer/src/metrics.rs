pub struct Rgba<'a> {
    pub width: usize,
    pub height: usize,
    pub data: &'a [u8],
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Diff {
    pub pixels: usize,
    pub exact: usize,
    pub over_8: usize,
    pub max: u8,
    pub ssim: f64,
}

const WINDOW: usize = 7;

fn gray(image: &Rgba) -> Vec<f64> {
    image
        .data
        .chunks_exact(4)
        .map(|p| (f64::from(p[0]) + f64::from(p[1]) + f64::from(p[2])) / 3.)
        .collect()
}

fn integral(values: impl Iterator<Item = f64>, width: usize, height: usize) -> Vec<f64> {
    let stride = width + 1;
    let mut out = vec![0.; stride * (height + 1)];
    let mut values = values;
    for y in 0..height {
        let mut row = 0.;
        for x in 0..width {
            row += values.next().unwrap();
            out[(y + 1) * stride + x + 1] = out[y * stride + x + 1] + row;
        }
    }
    out
}

fn window(table: &[f64], stride: usize, x: usize, y: usize) -> f64 {
    let k = WINDOW;
    (table[(y + k) * stride + x + k] - table[y * stride + x + k] - table[(y + k) * stride + x]
        + table[y * stride + x])
        / (k * k) as f64
}

pub fn ssim(a: &Rgba, b: &Rgba, mask: Option<&[bool]>) -> f64 {
    assert_eq!((a.width, a.height), (b.width, b.height));
    let (w, h) = (a.width, a.height);
    if w < WINDOW || h < WINDOW {
        return if a.data == b.data { 1. } else { 0. };
    }
    let (ga, gb) = (gray(a), gray(b));
    let ia = integral(ga.iter().copied(), w, h);
    let ib = integral(gb.iter().copied(), w, h);
    let iaa = integral(ga.iter().map(|v| v * v), w, h);
    let ibb = integral(gb.iter().map(|v| v * v), w, h);
    let iab = integral(ga.iter().zip(&gb).map(|(x, y)| x * y), w, h);
    let stride = w + 1;
    let (c1, c2) = ((0.01f64 * 255.).powi(2), (0.03f64 * 255.).powi(2));
    let (mut sum, mut count) = (0., 0usize);
    for y in 0..=h - WINDOW {
        for x in 0..=w - WINDOW {
            if let Some(m) = mask {
                let centre = (y + WINDOW / 2) * w + x + WINDOW / 2;
                if !m[centre] {
                    continue;
                }
            }
            let (ma, mb) = (window(&ia, stride, x, y), window(&ib, stride, x, y));
            let va = window(&iaa, stride, x, y) - ma * ma;
            let vb = window(&ibb, stride, x, y) - mb * mb;
            let cov = window(&iab, stride, x, y) - ma * mb;
            sum += ((2. * ma * mb + c1) * (2. * cov + c2))
                / ((ma * ma + mb * mb + c1) * (va + vb + c2));
            count += 1;
        }
    }
    if count == 0 { 1. } else { sum / count as f64 }
}

pub fn diff(a: &Rgba, b: &Rgba, mask: Option<&[bool]>) -> Diff {
    assert_eq!((a.width, a.height), (b.width, b.height));
    let mut out = Diff {
        pixels: 0,
        exact: 0,
        over_8: 0,
        max: 0,
        ssim: ssim(a, b, mask),
    };
    for (i, (p, q)) in a
        .data
        .chunks_exact(4)
        .zip(b.data.chunks_exact(4))
        .enumerate()
    {
        if mask.is_some_and(|m| !m[i]) {
            continue;
        }
        let d = (0..3).map(|c| p[c].abs_diff(q[c])).max().unwrap();
        out.pixels += 1;
        out.exact += usize::from(d == 0);
        out.over_8 += usize::from(d > 8);
        out.max = out.max.max(d);
    }
    out
}

pub fn changed(a: &[u8], b: &[u8]) -> Vec<bool> {
    a.chunks_exact(4)
        .zip(b.chunks_exact(4))
        .map(|(p, q)| p[..3] != q[..3])
        .collect()
}

pub fn dilate(mask: &[bool], width: usize, height: usize, radius: usize) -> Vec<bool> {
    let mut out = vec![false; mask.len()];
    for y in 0..height {
        for x in 0..width {
            if mask[y * width + x] {
                for yy in y.saturating_sub(radius)..(y + radius + 1).min(height) {
                    for xx in x.saturating_sub(radius)..(x + radius + 1).min(width) {
                        out[yy * width + xx] = true;
                    }
                }
            }
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn flat(value: u8, n: usize) -> Vec<u8> {
        (0..n * n)
            .flat_map(|_| [value, value, value, 255])
            .collect()
    }

    #[test]
    fn identical_images_have_unit_ssim_and_no_difference() {
        let data = flat(90, 16);
        let a = Rgba {
            width: 16,
            height: 16,
            data: &data,
        };
        let d = diff(&a, &a, None);
        assert_eq!((d.exact, d.pixels, d.max), (256, 256, 0));
        assert!((d.ssim - 1.).abs() < 1e-12);
    }

    #[test]
    fn offset_images_lose_ssim_and_report_counts() {
        let (x, y) = (flat(90, 16), flat(140, 16));
        let a = Rgba {
            width: 16,
            height: 16,
            data: &x,
        };
        let b = Rgba {
            width: 16,
            height: 16,
            data: &y,
        };
        let d = diff(&a, &b, None);
        assert_eq!((d.exact, d.over_8, d.max), (0, 256, 50));
        assert!(d.ssim < 0.95 && d.ssim > 0.8);
    }

    #[test]
    fn mask_restricts_counts_and_dilation_grows_it() {
        let (x, mut y) = (flat(90, 16), flat(90, 16));
        y[4 * (5 * 16 + 5)] = 200;
        let m = changed(&x, &y);
        assert_eq!(m.iter().filter(|v| **v).count(), 1);
        let grown = dilate(&m, 16, 16, 2);
        assert_eq!(grown.iter().filter(|v| **v).count(), 25);
        let a = Rgba {
            width: 16,
            height: 16,
            data: &x,
        };
        let b = Rgba {
            width: 16,
            height: 16,
            data: &y,
        };
        let d = diff(&a, &b, Some(&grown));
        assert_eq!((d.pixels, d.exact, d.over_8), (25, 24, 1));
    }
}
