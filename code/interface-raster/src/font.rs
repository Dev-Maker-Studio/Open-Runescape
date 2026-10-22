use crate::canvas::{Bitmap, Canvas};
use crate::sprite::{Frame, SpriteGroup};
use openrs_cs2::FontMetrics;

const EURO_GLYPH: usize = 128;
const LINE_BUFFER: usize = 100;

pub struct Font {
    pub metrics: FontMetrics,
    glyphs: Vec<Frame>,
}

#[derive(Debug, Clone, Copy)]
pub struct TextStyle {
    pub argb: u32,
    pub shadow_argb: i32,
    pub horizontal: i32,
    pub vertical: i32,
    pub line_spacing: i32,
    pub max_lines: i32,
    pub plain: bool,
}

struct Effects {
    color: u32,
    effect_color: u32,
    shadow_original: u32,
    shadow: u32,
    strike: i64,
    underline: i64,
    justify_width: i32,
    justify_acc: i32,
}

impl Effects {
    fn calibrate(color: u32, shadow: i32) -> Self {
        let shadow = if shadow == -1 { 0 } else { shadow as u32 };
        Self {
            color,
            effect_color: color,
            shadow_original: shadow,
            shadow,
            strike: -1,
            underline: -1,
            justify_width: 0,
            justify_acc: 0,
        }
    }

    fn reset(&mut self) {
        let fresh = Self::calibrate(self.color, self.shadow_original as i32);
        *self = fresh;
    }

    fn apply(&mut self, tag: &str) {
        let hex = |s: &str| parse_hex(s);
        let keep = 0xff00_0000u32;
        if let Some(v) = tag.strip_prefix("col=") {
            match hex(v) {
                Some(v) => self.effect_color = (self.effect_color & keep) | (v & 0xff_ffff),
                None => return,
            }
        } else if tag == "/col" {
            self.effect_color = (self.effect_color & keep) | (self.color & 0xff_ffff);
        }
        if let Some(v) = tag.strip_prefix("argb=") {
            if let Some(v) = hex(v) {
                self.effect_color = v;
            }
        } else if tag == "/argb" {
            self.effect_color = self.color;
        } else if let Some(v) = tag.strip_prefix("str=") {
            if let Some(v) = hex(v) {
                self.strike = i64::from((self.effect_color & keep) | v);
            }
        } else if tag == "str" {
            self.strike = i64::from((self.effect_color & keep) | 0x80_0000);
        } else if tag == "/str" {
            self.strike = -1;
        } else if let Some(v) = tag.strip_prefix("u=") {
            if let Some(v) = hex(v) {
                self.underline = i64::from((self.effect_color & keep) | v);
            }
        } else if tag == "u" {
            self.underline = i64::from(self.effect_color & keep);
        } else if tag == "/u" {
            self.underline = -1;
        } else if tag.eq_ignore_ascii_case("shad=-1") {
            self.shadow = 0;
        } else if let Some(v) = tag.strip_prefix("shad=") {
            if let Some(v) = hex(v) {
                self.shadow = (self.effect_color & keep) | v;
            }
        } else if tag == "shad" {
            self.shadow = self.effect_color & keep;
        } else if tag == "/shad" {
            self.shadow = self.shadow_original;
        } else if tag == "br" {
            self.reset();
        }
    }
}

fn parse_hex(text: &str) -> Option<u32> {
    let mut negative = false;
    let mut digits = false;
    let mut result = 0i32;
    for (index, ch) in text.chars().enumerate() {
        if index == 0 {
            if ch == '-' {
                negative = true;
                continue;
            }
            if ch == '+' {
                continue;
            }
        }
        let digit = ch.to_digit(16)? as i32;
        let signed = if negative { -digit } else { digit };
        let accumulated = signed.wrapping_add(result.wrapping_mul(16));
        if accumulated.wrapping_div(16) != result {
            return None;
        }
        result = accumulated;
        digits = true;
    }
    digits.then_some(result as u32)
}

fn parse_dec(text: &str) -> Option<i32> {
    text.parse::<i32>().ok()
}

pub(crate) fn glyph_for(unit: u16) -> usize {
    let signed: i32 = match unit {
        1..=127 | 160..=255 => i32::from(unit),
        8364 => -128,
        8218 => -126,
        402 => -125,
        8222 => -124,
        8230 => -123,
        8224 => -122,
        8225 => -121,
        710 => -120,
        8240 => -119,
        352 => -118,
        8249 => -117,
        338 => -116,
        381 => -114,
        8216 => -111,
        8217 => -110,
        8220 => -109,
        8221 => -108,
        8226 => -107,
        8211 => -106,
        8212 => -105,
        732 => -104,
        8482 => -103,
        353 => -102,
        8250 => -101,
        339 => -100,
        382 => -98,
        376 => -97,
        _ => 63,
    };
    (signed & 0xff) as usize
}

fn entity(tag: &str) -> Option<usize> {
    Some(match tag {
        "lt" => 60,
        "gt" => 62,
        "nbsp" => 160,
        "shy" => 173,
        "times" => 215,
        "euro" => EURO_GLYPH,
        "copy" => 169,
        "reg" => 174,
        _ => return None,
    })
}

fn units(text: &str) -> Vec<u16> {
    text.encode_utf16().collect()
}

fn tag_text(slice: &[u16]) -> String {
    String::from_utf16_lossy(slice)
}

impl Font {
    pub fn new(metrics: FontMetrics, group: &SpriteGroup) -> Option<Self> {
        if group.frames.len() < 256 {
            return None;
        }
        Some(Self { metrics, glyphs: group.frames[..256].to_vec() })
    }

    fn kern(&self, previous: i32, glyph: usize) -> i32 {
        match (&self.metrics.kerning, previous) {
            (Some(table), previous) if previous != -1 => i32::from(table[previous as usize * 256 + glyph]),
            _ => 0,
        }
    }

    fn width_of(&self, glyph: usize) -> i32 {
        i32::from(self.metrics.glyph_widths[glyph])
    }

    pub fn text_width(&self, text: &[u16], icons: &[Bitmap]) -> i32 {
        let mut tag_start: Option<usize> = None;
        let mut previous = -1i32;
        let mut width = 0i32;
        for (index, &unit) in text.iter().enumerate() {
            let mut glyph_unit = None;
            if unit == u16::from(b'<') {
                tag_start = Some(index);
                continue;
            }
            if unit == u16::from(b'>') {
                if let Some(start) = tag_start {
                    let tag = tag_text(&text[start + 1..index]);
                    tag_start = None;
                    match entity(&tag) {
                        Some(g) => glyph_unit = Some(g),
                        None => {
                            if let Some(icon) = icon_width(&tag, icons) {
                                width = width.wrapping_add(icon);
                                previous = -1;
                            }
                            continue;
                        }
                    }
                }
            }
            if tag_start.is_none() {
                let glyph = glyph_unit.unwrap_or_else(|| glyph_for(unit));
                width = width.wrapping_add(self.width_of(glyph)).wrapping_add(self.kern(previous, glyph));
                previous = glyph as i32;
            }
        }
        width
    }

    pub fn split_lines(&self, text: &[u16], max_width: Option<i32>, icons: &[Bitmap]) -> Vec<Vec<u16>> {
        let mut lines: Vec<Vec<u16>> = Vec::new();
        let mut current = 0i32;
        let mut line_start = 0usize;
        let mut last_space: Option<usize> = None;
        let mut last_space_width = 0i32;
        let mut space_trailing = 0usize;
        let mut previous = -1i32;
        let mut tag_start: Option<usize> = None;
        for (index, &unit) in text.iter().enumerate() {
            let code = glyph_for(unit);
            let mut char_width = 0i32;
            let mut break_pos = index;
            if code == 60 {
                tag_start = Some(index);
                continue;
            }
            if let Some(start) = tag_start {
                if code != 62 {
                    continue;
                }
                break_pos = start;
                let tag = tag_text(&text[start + 1..index]);
                tag_start = None;
                if tag == "br" {
                    lines.push(text[line_start..=index].to_vec());
                    if lines.len() >= LINE_BUFFER {
                        return Vec::new();
                    }
                    line_start = index + 1;
                    current = 0;
                    last_space = None;
                    previous = -1;
                    continue;
                }
                match entity(&tag) {
                    Some(glyph) => {
                        char_width = char_width
                            .wrapping_add(self.width_of(glyph))
                            .wrapping_add(self.kern(previous, glyph));
                        previous = glyph as i32;
                    }
                    None => {
                        if let Some(icon) = icon_width(&tag, icons) {
                            char_width = char_width.wrapping_add(icon);
                            previous = -1;
                        }
                    }
                }
            } else {
                char_width = char_width
                    .wrapping_add(self.width_of(code))
                    .wrapping_add(self.kern(previous, code));
                previous = code as i32;
            }
            if char_width > 0 {
                current = current.wrapping_add(char_width);
                let Some(limit) = max_width else { continue };
                if code == 32 {
                    last_space = Some(index);
                    last_space_width = current;
                    space_trailing = 1;
                }
                if current > limit {
                    if let Some(space) = last_space {
                        lines.push(text[line_start..space + 1 - space_trailing].to_vec());
                        if lines.len() >= LINE_BUFFER {
                            return Vec::new();
                        }
                        line_start = space + 1;
                        last_space = None;
                        current = current.wrapping_sub(last_space_width);
                    } else {
                        lines.push(text[line_start..break_pos].to_vec());
                        if lines.len() >= LINE_BUFFER {
                            return Vec::new();
                        }
                        line_start = break_pos;
                        last_space = None;
                        current = char_width;
                    }
                    previous = -1;
                }
                if code == 45 {
                    last_space = Some(index);
                    last_space_width = current;
                    space_trailing = 0;
                }
            }
        }
        if text.len() > line_start {
            lines.push(text[line_start..].to_vec());
        }
        lines
    }

    pub fn fit_text(&self, text: &[u16], max_width: i32, icons: &[Bitmap]) -> Vec<u16> {
        if self.text_width(text, icons) <= max_width {
            return text.to_vec();
        }
        let adjusted = max_width - self.text_width(&units("..."), &[]);
        let mut escape: Option<usize> = None;
        let mut previous = -1i32;
        let mut width = 0i32;
        let mut prefix = 0usize;
        let dots = |end: usize| {
            let mut out = text[..end].to_vec();
            out.extend(units("..."));
            out
        };
        for (i, &unit) in text.iter().enumerate() {
            if unit == u16::from(b'<') {
                escape = Some(i);
                continue;
            }
            let mut glyph = None;
            if unit == u16::from(b'>') {
                if let Some(start) = escape {
                    let tag = tag_text(&text[start + 1..i]);
                    escape = None;
                    match entity(&tag) {
                        Some(g) => glyph = Some(g),
                        None => {
                            if let Some(icon) = icon_width(&tag, icons) {
                                width += icon;
                                previous = -1;
                                if width > adjusted {
                                    return dots(prefix);
                                }
                                prefix = i + 1;
                            }
                            continue;
                        }
                    }
                }
            }
            if escape.is_none() {
                let glyph = glyph.unwrap_or_else(|| glyph_for(unit));
                width += self.width_of(glyph);
                if previous != -1 {
                    width += self.kern(previous, glyph);
                }
                previous = glyph as i32;
                let total = width + self.kern(glyph as i32, 46);
                if total > adjusted {
                    return dots(prefix);
                }
                prefix = i + 1;
            }
        }
        text.to_vec()
    }

    #[allow(clippy::too_many_arguments)]
    fn draw_glyph(&self, canvas: &mut Canvas, glyph: usize, x: i32, y: i32, argb: u32, shadow: bool, plain: bool) {
        let frame = &self.glyphs[glyph];
        if frame.width <= 0 || frame.height <= 0 {
            return;
        }
        let tint_color = if plain || shadow { Some(argb) } else { None };
        for row in 0..frame.height {
            for col in 0..frame.width {
                let at = (row * frame.width + col) as usize;
                let src = if plain {
                    (u32::from(frame.mask[at]) << 24) | 0xff_ffff
                } else {
                    frame.pixels[at]
                };
                if src >> 24 == 0 {
                    continue;
                }
                let out = tint_color.map_or(src, |c| crate::canvas::tint(src, c));
                canvas.plot(x + frame.min_x + col, y + frame.min_y + row, out);
            }
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn render_line(&self, canvas: &mut Canvas, line: &[u16], x: i32, y: i32, fx: &mut Effects, plain: bool, icons: &[Bitmap]) {
        let mut cur_x = x;
        let cur_y = y - self.metrics.vertical_spacing;
        let mut tag_start: Option<usize> = None;
        let mut previous = -1i32;
        for (i, &unit) in line.iter().enumerate() {
            let mut glyph = glyph_for(unit);
            if glyph == 60 {
                tag_start = Some(i);
                continue;
            }
            if glyph == 62 {
                if let Some(start) = tag_start {
                    let tag = tag_text(&line[start + 1..i]);
                    tag_start = None;
                    match entity(&tag) {
                        Some(g) => glyph = g,
                        None => {
                            if let Some(id) = tag.strip_prefix("img=").and_then(parse_dec) {
                                if let Some(icon) = usize::try_from(id).ok().and_then(|id| icons.get(id)) {
                                    let top = cur_y + self.metrics.vertical_spacing - icon.height;
                                    let color = if fx.effect_color & 0xff00_0000 == 0xff00_0000 {
                                        None
                                    } else {
                                        Some((fx.effect_color & 0xff00_0000) | 0xff_ffff)
                                    };
                                    canvas.draw_bitmap(icon, cur_x, top, color);
                                    cur_x += icon.width;
                                    previous = -1;
                                }
                            } else if !tag.starts_with("sprite=") && !tag.starts_with("obj=") {
                                fx.apply(&tag);
                            }
                            continue;
                        }
                    }
                }
            }
            if tag_start.is_some() {
                continue;
            }
            if previous != -1 {
                cur_x += self.kern(previous, glyph);
            }
            if glyph != 32 {
                if fx.shadow & 0xff00_0000 != 0 {
                    self.draw_glyph(canvas, glyph, cur_x + 1, cur_y + 1, fx.shadow, true, plain);
                }
                self.draw_glyph(canvas, glyph, cur_x, cur_y, fx.effect_color, false, plain);
            } else if fx.justify_width > 0 {
                fx.justify_acc += fx.justify_width;
                cur_x += fx.justify_acc >> 8;
                fx.justify_acc &= 0xff;
            }
            let glyph_width = self.width_of(glyph);
            if fx.strike != -1 {
                let at = cur_y + (0.7 * f64::from(self.metrics.vertical_spacing)) as i32;
                canvas.fill_rect(cur_x, at, glyph_width, 1, fx.strike as u32);
            }
            if fx.underline != -1 {
                canvas.fill_rect(cur_x, cur_y + self.metrics.vertical_spacing + 1, glyph_width, 1, fx.underline as u32);
            }
            cur_x += glyph_width;
            previous = glyph as i32;
        }
    }

    fn justify(&self, fx: &mut Effects, line: &[u16], width: i32) {
        let mut spaces = 0;
        let mut in_tag = false;
        for &unit in line {
            if unit == u16::from(b'<') {
                in_tag = true;
            } else if unit == u16::from(b'>') {
                in_tag = false;
            } else if !in_tag && unit == u16::from(b' ') {
                spaces += 1;
            }
        }
        if spaces > 0 {
            fx.justify_width = ((width - self.text_width(line, &[])) << 8) / spaces;
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn render_text(
        &self,
        canvas: &mut Canvas,
        text: &str,
        x: i32,
        y: i32,
        width: i32,
        height: i32,
        style: &TextStyle,
        icons: &[Bitmap],
    ) -> i32 {
        let m = &self.metrics;
        let mut fx = Effects::calibrate(style.argb, style.shadow_argb);
        let mut spacing = if style.line_spacing == 0 { m.vertical_spacing } else { style.line_spacing };
        let wrap = if height < spacing + m.bottom_padding + m.top_padding && height < spacing + spacing {
            None
        } else {
            Some(width)
        };
        let all = units(text);
        let mut lines = self.split_lines(&all, wrap, icons);
        let mut max_lines = style.max_lines;
        if max_lines == -1 {
            max_lines = height / spacing.max(1);
            if max_lines <= 0 {
                max_lines = 1;
            }
        }
        let mut count = lines.len() as i32;
        if max_lines > 0 && count >= max_lines {
            let at = (max_lines - 1) as usize;
            lines[at] = self.fit_text(&lines[at], width, icons);
            count = max_lines;
        }
        if count <= 0 {
            return 0;
        }
        let count = count as usize;
        let mut vertical = style.vertical;
        if vertical == 3 && count == 1 {
            vertical = 1;
        }
        let pad = m.top_padding + m.bottom_padding;
        let n = count as i32;
        let start_y = match vertical {
            0 => y + m.top_padding,
            1 => (height - pad - spacing * (n - 1)) / 2 + y + m.top_padding,
            2 => y + height - m.bottom_padding - spacing * (n - 1),
            _ => {
                let add = ((height - pad - spacing * (n - 1)) / (n + 1)).max(0);
                spacing += add;
                y + m.top_padding + add
            }
        };
        let mut render_y = start_y;
        for (i, line) in lines.iter().take(count).enumerate() {
            let line_x = match style.horizontal {
                0 => x,
                1 => x + (width - self.text_width(line, &[])) / 2,
                2 => x + width - self.text_width(line, &[]),
                _ if i == count - 1 => x,
                _ => {
                    self.justify(&mut fx, line, width);
                    x
                }
            };
            self.render_line(canvas, line, line_x, render_y, &mut fx, style.plain, icons);
            fx.justify_width = 0;
            render_y += spacing;
        }
        n
    }
}

fn icon_width(tag: &str, icons: &[Bitmap]) -> Option<i32> {
    let id = parse_dec(tag.strip_prefix("img=")?)?;
    icons.get(usize::try_from(id).ok()?).map(|b| b.width)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn font(width: u8) -> Font {
        let metrics = FontMetrics {
            glyph_widths: vec![width; 256],
            kerning: None,
            vertical_spacing: 8,
            top_padding: 9,
            bottom_padding: 3,
        };
        let frame = Frame { min_x: 0, min_y: 0, width: 1, height: 1, pixels: vec![0xffffffff], has_alpha: false, mask: vec![255] };
        let group = SpriteGroup { biggest_width: 1, biggest_height: 1, frames: vec![frame; 256] };
        Font::new(metrics, &group).unwrap()
    }

    fn lines(f: &Font, text: &str, width: Option<i32>) -> Vec<String> {
        f.split_lines(&units(text), width, &[]).iter().map(|l| String::from_utf16_lossy(l)).collect()
    }

    #[test]
    fn splits_on_spaces_and_breaks() {
        let f = font(4);
        assert_eq!(lines(&f, "aa bb cc", Some(12)), vec!["aa", "bb", "cc"]);
        assert_eq!(lines(&f, "a<br>b", None), vec!["a<br>", "b"]);
        assert_eq!(lines(&f, "abcdef", Some(8)), vec!["ab", "cd", "ef"]);
    }

    #[test]
    fn width_skips_tags_and_resolves_entities() {
        let f = font(5);
        assert_eq!(f.text_width(&units("a<col=ff0000>b</col>"), &[]), 10);
        assert_eq!(f.text_width(&units("a<lt>"), &[]), 10);
    }

    #[test]
    fn fit_text_adds_ellipsis_within_the_width() {
        let f = font(4);
        assert_eq!(String::from_utf16_lossy(&f.fit_text(&units("abcdefgh"), 20, &[])), "ab...");
        assert_eq!(String::from_utf16_lossy(&f.fit_text(&units("abc"), 20, &[])), "abc");
    }

    #[test]
    fn hex_parsing_follows_the_kotlin_overflow_rule() {
        assert_eq!(parse_hex("ff0000"), Some(0xff0000));
        assert_eq!(parse_hex("ffffffff"), None);
        assert_eq!(parse_hex("-1"), Some(u32::MAX));
        assert_eq!(parse_hex("zz"), None);
    }

    #[test]
    fn renders_coloured_centered_text_with_shadow() {
        let f = font(4);
        let mut canvas = Canvas::new(40, 20);
        let style = TextStyle { argb: 0xffffffff, shadow_argb: 0xff000000u32 as i32, horizontal: 1, vertical: 0, line_spacing: 0, max_lines: 0, plain: true };
        let n = f.render_text(&mut canvas, "<col=ff0000>ab", 0, 0, 40, 20, &style, &[]);
        assert_eq!(n, 1);
        let y = 1;
        let x = (40 - 8) / 2;
        assert_eq!(canvas.pixel(x, y), 0xffff0000);
        assert_eq!(canvas.pixel(x + 1, y + 1), 0xff000000);
    }

    #[test]
    fn br_resets_the_colour_effects() {
        let f = font(4);
        let mut canvas = Canvas::new(40, 40);
        let style = TextStyle { argb: 0xff00ff00, shadow_argb: -1, horizontal: 0, vertical: 0, line_spacing: 0, max_lines: 0, plain: true };
        let n = f.render_text(&mut canvas, "<col=ff0000>a<br>b", 0, 0, 40, 40, &style, &[]);
        assert_eq!(n, 2);
        assert_eq!(canvas.pixel(0, 1), 0xffff0000);
        assert_eq!(canvas.pixel(0, 9), 0xff00ff00);
    }
}
