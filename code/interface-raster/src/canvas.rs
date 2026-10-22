#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Bitmap {
    pub width: i32,
    pub height: i32,
    pub pixels: Vec<u32>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct Rect {
    left: i32,
    top: i32,
    right: i32,
    bottom: i32,
}

#[derive(Debug, Clone)]
pub struct Canvas {
    pub width: i32,
    pub height: i32,
    pub pixels: Vec<u32>,
    clip: Rect,
}

fn mul(a: u32, b: u32) -> u32 {
    (a * b + 127) / 255
}

pub(crate) fn tint(src: u32, color: u32) -> u32 {
    let a = mul(src >> 24, color >> 24);
    let r = mul((src >> 16) & 0xff, (color >> 16) & 0xff);
    let g = mul((src >> 8) & 0xff, (color >> 8) & 0xff);
    let b = mul(src & 0xff, color & 0xff);
    (a << 24) | (r << 16) | (g << 8) | b
}

fn over(dst: u32, src: u32) -> u32 {
    let sa = src >> 24;
    if sa == 255 {
        return src;
    }
    if sa == 0 {
        return dst;
    }
    let da = dst >> 24;
    let weight = da * (255 - sa);
    let out_a = sa * 255 + weight;
    let channel = |shift: u32| {
        let s = (src >> shift) & 0xff;
        let d = (dst >> shift) & 0xff;
        (s * sa * 255 + d * weight + out_a / 2) / out_a
    };
    let alpha = (out_a + 127) / 255;
    (alpha << 24) | (channel(16) << 16) | (channel(8) << 8) | channel(0)
}

impl Canvas {
    pub fn new(width: i32, height: i32) -> Self {
        let width = width.max(0);
        let height = height.max(0);
        Self {
            width,
            height,
            pixels: vec![0; (width * height) as usize],
            clip: Rect { left: 0, top: 0, right: width, bottom: height },
        }
    }

    pub fn clear(&mut self) {
        self.pixels.fill(0);
        self.reset_clip();
    }

    pub fn clear_rect(&mut self, x: i32, y: i32, width: i32, height: i32) {
        let left = x.max(self.clip.left);
        let top = y.max(self.clip.top);
        let right = x.saturating_add(width).min(self.clip.right);
        let bottom = y.saturating_add(height).min(self.clip.bottom);
        for row in top..bottom {
            for col in left..right {
                self.pixels[(row * self.width + col) as usize] = 0;
            }
        }
    }

    pub fn reset_clip(&mut self) {
        self.clip = Rect { left: 0, top: 0, right: self.width, bottom: self.height };
    }

    pub fn set_clip(&mut self, left: i32, top: i32, right: i32, bottom: i32) {
        self.clip = Rect {
            left: left.max(0),
            top: top.max(0),
            right: right.min(self.width),
            bottom: bottom.min(self.height),
        };
    }

    pub fn expand_clip(&mut self, left: i32, top: i32, right: i32, bottom: i32) {
        self.clip = Rect {
            left: self.clip.left.max(left),
            top: self.clip.top.max(top),
            right: self.clip.right.min(right),
            bottom: self.clip.bottom.min(bottom),
        };
    }

    pub fn plot(&mut self, x: i32, y: i32, argb: u32) {
        if x < self.clip.left || x >= self.clip.right || y < self.clip.top || y >= self.clip.bottom {
            return;
        }
        let at = (y * self.width + x) as usize;
        self.pixels[at] = over(self.pixels[at], argb);
    }

    pub fn fill_rect(&mut self, x: i32, y: i32, width: i32, height: i32, argb: u32) {
        let left = x.max(self.clip.left);
        let top = y.max(self.clip.top);
        let right = x.saturating_add(width).min(self.clip.right);
        let bottom = y.saturating_add(height).min(self.clip.bottom);
        for row in top..bottom {
            for col in left..right {
                let at = (row * self.width + col) as usize;
                self.pixels[at] = over(self.pixels[at], argb);
            }
        }
    }

    pub fn outline_rect(&mut self, x: i32, y: i32, width: i32, height: i32, argb: u32) {
        if width <= 0 || height <= 0 {
            return;
        }
        self.fill_rect(x, y, width, 1, argb);
        if height > 1 {
            self.fill_rect(x, y + height - 1, width, 1, argb);
        }
        if height > 2 {
            self.fill_rect(x, y + 1, 1, height - 2, argb);
            if width > 1 {
                self.fill_rect(x + width - 1, y + 1, 1, height - 2, argb);
            }
        }
    }

    pub fn thick_line(&mut self, x1: i32, y1: i32, x2: i32, y2: i32, width: i32, argb: u32) {
        let mut dx = (x2 - x1) as f32;
        let mut dy = (y2 - y1) as f32;
        if dx == 0.0 && dy == 0.0 {
            dx = 1.0;
        } else {
            let inv = (1.0 / ((dx * dx + dy * dy) as f64).sqrt()) as f32;
            dx *= inv;
            dy *= inv;
        }
        let half = width.max(1) as f32 * 0.5;
        let px = -dy * half;
        let py = dx * half;
        let sx = x1 as f32 + 0.35;
        let sy = y1 as f32 + 0.35;
        let ex = x2 as f32 + dx + 0.35;
        let ey = y2 as f32 + dy + 0.35;
        let quad = [(sx + px, sy + py), (sx - px, sy - py), (ex - px, ey - py), (ex + px, ey + py)];
        self.fill_quad(&quad, argb);
    }

    fn fill_quad(&mut self, q: &[(f32, f32); 4], argb: u32) {
        let min_x = q.iter().map(|p| p.0).fold(f32::MAX, f32::min).floor() as i32;
        let max_x = q.iter().map(|p| p.0).fold(f32::MIN, f32::max).ceil() as i32;
        let min_y = q.iter().map(|p| p.1).fold(f32::MAX, f32::min).floor() as i32;
        let max_y = q.iter().map(|p| p.1).fold(f32::MIN, f32::max).ceil() as i32;
        for y in min_y.max(self.clip.top)..max_y.min(self.clip.bottom) {
            for x in min_x.max(self.clip.left)..max_x.min(self.clip.right) {
                let p = (x as f32 + 0.5, y as f32 + 0.5);
                let mut sign = 0i32;
                let mut inside = true;
                for i in 0..4 {
                    let a = q[i];
                    let b = q[(i + 1) % 4];
                    let cross = (b.0 - a.0) * (p.1 - a.1) - (b.1 - a.1) * (p.0 - a.0);
                    let s = if cross > 0.0 { 1 } else if cross < 0.0 { -1 } else { 0 };
                    if s != 0 {
                        if sign == 0 {
                            sign = s;
                        } else if sign != s {
                            inside = false;
                            break;
                        }
                    }
                }
                if inside {
                    self.plot(x, y, argb);
                }
            }
        }
    }

    pub fn draw_bitmap(&mut self, bitmap: &Bitmap, x: i32, y: i32, color: Option<u32>) {
        for row in 0..bitmap.height {
            for col in 0..bitmap.width {
                let src = bitmap.pixels[(row * bitmap.width + col) as usize];
                if src >> 24 == 0 {
                    continue;
                }
                self.plot(x + col, y + row, color.map_or(src, |c| tint(src, c)));
            }
        }
    }

    pub fn draw_scaled(&mut self, bitmap: &Bitmap, x: i32, y: i32, width: i32, height: i32, color: Option<u32>) {
        if width <= 0 || height <= 0 || bitmap.width <= 0 || bitmap.height <= 0 {
            return;
        }
        if width == bitmap.width && height == bitmap.height {
            return self.draw_bitmap(bitmap, x, y, color);
        }
        for row in 0..height {
            let sy = ((row as i64 * bitmap.height as i64) / height as i64) as i32;
            for col in 0..width {
                let sx = ((col as i64 * bitmap.width as i64) / width as i64) as i32;
                let src = bitmap.pixels[(sy * bitmap.width + sx) as usize];
                if src >> 24 != 0 {
                    self.plot(x + col, y + row, color.map_or(src, |c| tint(src, c)));
                }
            }
        }
    }

    pub fn draw_tiled(&mut self, bitmap: &Bitmap, x: i32, y: i32, width: i32, height: i32, color: Option<u32>) {
        if bitmap.width <= 0 || bitmap.height <= 0 {
            return;
        }
        let saved = self.clip;
        self.expand_clip(x, y, x.saturating_add(width), y.saturating_add(height));
        let mut row = 0;
        while row < height {
            let mut col = 0;
            while col < width {
                self.draw_bitmap(bitmap, x + col, y + row, color);
                col += bitmap.width;
            }
            row += bitmap.height;
        }
        self.clip = saved;
    }

    pub fn draw_rotated(&mut self, bitmap: &Bitmap, cx: f32, cy: f32, zoom: i32, rotation: i32, color: Option<u32>) {
        if zoom == 0 || bitmap.width <= 0 || bitmap.height <= 0 {
            return;
        }
        let w = bitmap.width as f32;
        let h = bitmap.height as f32;
        let pivot_x = w / 2.0;
        let pivot_y = h / 2.0;
        let radians = f64::from(rotation & 0xffff) * 9.587379924285257E-5;
        let sin = (radians.sin() * f64::from(zoom)) as f32;
        let cos = (radians.cos() * f64::from(zoom)) as f32;
        let tl = ((-pivot_x * cos + -pivot_y * sin) / 4096.0 + cx, (pivot_x * sin + -pivot_y * cos) / 4096.0 + cy);
        let tr = (((w - pivot_x) * cos + -pivot_y * sin) / 4096.0 + cx, (-(w - pivot_x) * sin + -pivot_y * cos) / 4096.0 + cy);
        let bl = ((-pivot_x * cos + (h - pivot_y) * sin) / 4096.0 + cx, (pivot_x * sin + (h - pivot_y) * cos) / 4096.0 + cy);
        let ux = (tr.0 - tl.0, tr.1 - tl.1);
        let vx = (bl.0 - tl.0, bl.1 - tl.1);
        let det = ux.0 * vx.1 - ux.1 * vx.0;
        if det.abs() < 1e-9 {
            return;
        }
        let xs = [tl.0, tr.0, bl.0, tr.0 + bl.0 - tl.0];
        let ys = [tl.1, tr.1, bl.1, tr.1 + bl.1 - tl.1];
        let min_x = xs.iter().cloned().fold(f32::MAX, f32::min).floor() as i32;
        let max_x = xs.iter().cloned().fold(f32::MIN, f32::max).ceil() as i32;
        let min_y = ys.iter().cloned().fold(f32::MAX, f32::min).floor() as i32;
        let max_y = ys.iter().cloned().fold(f32::MIN, f32::max).ceil() as i32;
        for y in min_y.max(self.clip.top)..max_y.min(self.clip.bottom) {
            for x in min_x.max(self.clip.left)..max_x.min(self.clip.right) {
                let px = x as f32 + 0.5 - tl.0;
                let py = y as f32 + 0.5 - tl.1;
                let u = (px * vx.1 - py * vx.0) / det;
                let v = (ux.0 * py - ux.1 * px) / det;
                if !(0.0..1.0).contains(&u) || !(0.0..1.0).contains(&v) {
                    continue;
                }
                let sx = (u * w) as i32;
                let sy = (v * h) as i32;
                let src = bitmap.pixels[(sy * bitmap.width + sx) as usize];
                if src >> 24 != 0 {
                    self.plot(x, y, color.map_or(src, |c| tint(src, c)));
                }
            }
        }
    }

    pub fn to_rgba(&self) -> Vec<u8> {
        let mut out = Vec::with_capacity(self.pixels.len() * 4);
        for p in &self.pixels {
            out.extend([(p >> 16) as u8, (p >> 8) as u8, *p as u8, (p >> 24) as u8]);
        }
        out
    }

    pub fn pixel(&self, x: i32, y: i32) -> u32 {
        self.pixels[(y * self.width + x) as usize]
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fill_respects_clip_and_blends() {
        let mut c = Canvas::new(4, 4);
        c.set_clip(1, 1, 3, 3);
        c.fill_rect(0, 0, 4, 4, 0xffff0000);
        assert_eq!(c.pixel(0, 0), 0);
        assert_eq!(c.pixel(1, 1), 0xffff0000);
        c.fill_rect(1, 1, 1, 1, 0x800000ff);
        let p = c.pixel(1, 1);
        assert_eq!(p >> 24, 255);
        assert!((p & 0xff) >= 127 && (p & 0xff) <= 129);
    }

    #[test]
    fn expand_clip_intersects() {
        let mut c = Canvas::new(8, 8);
        c.set_clip(0, 0, 4, 4);
        c.expand_clip(2, 2, 8, 8);
        c.fill_rect(0, 0, 8, 8, 0xff00ff00);
        assert_eq!(c.pixel(1, 1), 0);
        assert_eq!(c.pixel(2, 2), 0xff00ff00);
        assert_eq!(c.pixel(4, 4), 0);
    }

    #[test]
    fn outline_draws_border_only() {
        let mut c = Canvas::new(5, 5);
        c.outline_rect(0, 0, 5, 5, 0xffffffff);
        assert_eq!(c.pixel(0, 2), 0xffffffff);
        assert_eq!(c.pixel(4, 2), 0xffffffff);
        assert_eq!(c.pixel(2, 2), 0);
    }

    #[test]
    fn tint_modulates_channels() {
        assert_eq!(tint(0xffffffff, 0x80ff0000), 0x80ff0000);
    }

    #[test]
    fn tiled_repeats_inside_the_box() {
        let mut c = Canvas::new(6, 2);
        let b = Bitmap { width: 2, height: 2, pixels: vec![0xffff0000; 4] };
        c.draw_tiled(&b, 0, 0, 5, 2, None);
        assert_eq!(c.pixel(4, 1), 0xffff0000);
        assert_eq!(c.pixel(5, 1), 0);
    }

    #[test]
    fn rotation_by_zero_matches_plain_draw() {
        let mut a = Canvas::new(6, 6);
        let mut b = Canvas::new(6, 6);
        let bmp = Bitmap { width: 2, height: 2, pixels: vec![0xff112233, 0xff445566, 0xff778899, 0xffaabbcc] };
        a.draw_bitmap(&bmp, 2, 2, None);
        b.draw_rotated(&bmp, 3.0, 3.0, 4096, 0, None);
        assert_eq!(a.pixels, b.pixels);
    }

    #[test]
    fn horizontal_thick_line_covers_its_row() {
        let mut c = Canvas::new(8, 4);
        c.thick_line(1, 1, 6, 1, 1, 0xffffffff);
        assert_eq!(c.pixel(3, 1), 0xffffffff);
        assert_eq!(c.pixel(3, 3), 0);
    }
}
