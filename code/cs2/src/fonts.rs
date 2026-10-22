//! The client's `FontMetrics` text measuring, ported over UTF-16 units like the Kotlin `String`.

use crate::{HostError, VarHost};
use std::collections::HashMap;
use std::sync::Arc;

const LINE_BUFFER: usize = 100;
const OBJ_WIDTH: i32 = 36;
const OBJ_HEIGHT: i32 = 32;
const EURO_GLYPH: usize = 128;

/// A decoded font: glyph advances, the optional kerning table and the line paddings.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct FontMetrics {
    pub glyph_widths: Vec<u8>,
    /// 256 by 256, row is the previous glyph, column the current one.
    pub kerning: Option<Vec<i8>>,
    pub vertical_spacing: i32,
    pub top_padding: i32,
    pub bottom_padding: i32,
}

struct Reader<'a> {
    bytes: &'a [u8],
    at: usize,
}

impl Reader<'_> {
    fn byte(&mut self) -> Option<u8> {
        let value = *self.bytes.get(self.at)?;
        self.at += 1;
        Some(value)
    }
}

fn calculate_kerning(
    horizontal: &[Vec<i8>],
    vertical: &[Vec<i8>],
    heights: &[i32],
    glyph_widths: &[u8],
    widths: &[i32],
    start: usize,
    end: usize,
) -> Option<i32> {
    let x1 = heights[start];
    let x2 = x1 + widths[start];
    let x3 = heights[end];
    let x4 = x3 + widths[end];
    let min_x = x1.max(x3);
    let max_x = x2.min(x4);
    let mut minimum = i32::from(glyph_widths[start]).min(i32::from(glyph_widths[end]));
    let off_x = (min_x - x1) as usize;
    let off_y = (min_x - x3) as usize;
    for step in 0..(max_x - min_x).max(0) as usize {
        let gap = i32::from(*vertical[start].get(off_x + step)?)
            + i32::from(*horizontal[end].get(off_y + step)?);
        minimum = minimum.min(gap);
    }
    Some(-minimum)
}

impl FontMetrics {
    /// The `FontMetrics` constructor; `None` where the Kotlin one throws.
    pub fn decode(bytes: &[u8]) -> Option<Self> {
        let mut reader = Reader { bytes, at: 0 };
        if reader.byte()? != 0 {
            return None;
        }
        let variable = reader.byte()? == 1;
        let mut glyph_widths = Vec::with_capacity(256);
        for _ in 0..256 {
            glyph_widths.push(reader.byte()?);
        }
        let mut kerning = None;
        let vertical_spacing;
        if variable {
            let mut widths = [0i32; 256];
            let mut heights = [0i32; 256];
            for width in &mut widths {
                *width = i32::from(reader.byte()?);
            }
            for height in &mut heights {
                *height = i32::from(reader.byte()?);
            }
            let offsets = |reader: &mut Reader| -> Option<Vec<Vec<i8>>> {
                let mut rows = Vec::with_capacity(256);
                for width in widths {
                    let mut offset = 0i8;
                    let mut row = Vec::with_capacity(width as usize);
                    for _ in 0..width {
                        offset = offset.wrapping_add(reader.byte()? as i8);
                        row.push(offset);
                    }
                    rows.push(row);
                }
                Some(rows)
            };
            let horizontal = offsets(&mut reader)?;
            let vertical = offsets(&mut reader)?;
            let mut table = vec![0i8; 256 * 256];
            for i in 0..256 {
                if i == 32 || i == 160 {
                    continue;
                }
                for j in 0..256 {
                    if j != 32 && j != 160 {
                        table[i * 256 + j] = calculate_kerning(
                            &horizontal,
                            &vertical,
                            &heights,
                            &glyph_widths,
                            &widths,
                            i,
                            j,
                        )? as i8;
                    }
                }
            }
            kerning = Some(table);
            vertical_spacing = heights[32] + widths[32];
        } else {
            vertical_spacing = i32::from(reader.byte()?);
        }
        reader.byte()?;
        reader.byte()?;
        let top_padding = i32::from(reader.byte()?);
        let bottom_padding = i32::from(reader.byte()?);
        Some(Self {
            glyph_widths,
            kerning,
            vertical_spacing,
            top_padding,
            bottom_padding,
        })
    }

    fn line_height(&self) -> i32 {
        self.top_padding + self.bottom_padding
    }

    fn kern(&self, previous: i32, glyph: usize) -> i32 {
        match (&self.kerning, previous) {
            (Some(table), previous) if previous != -1 => {
                i32::from(table[previous as usize * 256 + glyph])
            }
            _ => 0,
        }
    }
}

/// `Utils.byteForChar` as the unsigned glyph index.
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

/// `Utils.parseInt(text, 10)`.
fn parse_int(text: &str) -> Option<i32> {
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
        let digit = ch.to_digit(10)? as i32;
        let signed = if negative { -digit } else { digit };
        let accumulated = signed.wrapping_add(result.wrapping_mul(10));
        if accumulated.wrapping_div(10) != result {
            return None;
        }
        result = accumulated;
        digits = true;
    }
    digits.then_some(result)
}

fn is(tag: &[u16], name: &str) -> bool {
    tag.len() == name.len() && tag.iter().zip(name.bytes()).all(|(a, b)| *a == u16::from(b))
}

fn starts(tag: &[u16], prefix: &str) -> bool {
    tag.len() >= prefix.len()
        && tag.iter().zip(prefix.bytes()).all(|(a, b)| *a == u16::from(b))
}

fn entity(tag: &[u16]) -> Option<u16> {
    [
        ("lt", 60u16),
        ("gt", 62),
        ("nbsp", 160),
        ("shy", 173),
        ("times", 215),
        ("euro", 8364),
        ("copy", 169),
        ("reg", 174),
    ]
    .into_iter()
    .find(|(name, _)| is(tag, name))
    .map(|(_, unit)| unit)
}

fn is_inline_tag(tag: &[u16]) -> bool {
    starts(tag, "sprite=") || starts(tag, "obj=")
}

/// Host lookups behind the measuring: the name icons (`<img=n>`) and the inline graphics.
/// The first host error is kept and the lookup answers as if the thing were missing.
pub(crate) struct Surroundings<'a> {
    host: &'a dyn VarHost,
    use_icons: bool,
    icons: Option<Option<Arc<[i32]>>>,
    sizes: HashMap<Vec<u16>, Option<(i32, i32)>>,
    pub(crate) error: Option<HostError>,
}

impl<'a> Surroundings<'a> {
    pub(crate) fn new(host: &'a dyn VarHost, use_icons: bool) -> Self {
        Self {
            host,
            use_icons,
            icons: None,
            sizes: HashMap::new(),
            error: None,
        }
    }

    fn note<T>(&mut self, result: Result<T, HostError>) -> Option<T> {
        match result {
            Ok(value) => Some(value),
            Err(error) => {
                self.error.get_or_insert(error);
                None
            }
        }
    }

    /// `sprites[id].getSpriteWidth()` of an `img=` tag; `None` where the Kotlin tag is ignored.
    fn icon_width(&mut self, tag: &[u16]) -> Option<i32> {
        if !self.use_icons || !starts(tag, "img=") {
            return None;
        }
        if self.icons.is_none() {
            let loaded = self.host.name_icon_widths();
            self.icons = Some(self.note(loaded).flatten());
        }
        let icons = self.icons.clone()??;
        let id = parse_int(&String::from_utf16_lossy(&tag[4..]))?;
        icons.get(usize::try_from(id).ok()?).copied()
    }

    /// `InlineSprites.size` of a `sprite=` or `obj=` tag.
    fn inline_size(&mut self, tag: &[u16]) -> Option<(i32, i32)> {
        if let Some(size) = self.sizes.get(tag) {
            return *size;
        }
        let size = self.measure_inline(tag);
        self.sizes.insert(tag.to_vec(), size);
        size
    }

    fn measure_inline(&mut self, tag: &[u16]) -> Option<(i32, i32)> {
        let object = starts(tag, "obj=");
        let text = String::from_utf16_lossy(&tag[if object { 4 } else { 7 }..]);
        let mut arguments = text.split(',');
        let id = parse_int(arguments.next()?)?;
        let argument = match arguments.next() {
            Some(second) => parse_int(second)?,
            None if object => 1,
            None => 0,
        };
        if object {
            return Some((OBJ_WIDTH, OBJ_HEIGHT));
        }
        let size = self.host.inline_graphic_size(id, argument);
        self.note(size).flatten()
    }

    fn inline_width(&mut self, tag: &[u16]) -> i32 {
        self.inline_size(tag).map_or(0, |size| size.0)
    }
}

impl FontMetrics {
    /// `getTextWidth`.
    pub(crate) fn text_width(&self, text: &[u16], around: &mut Surroundings) -> i32 {
        let mut tag_start: Option<usize> = None;
        let mut previous = -1i32;
        let mut width = 0i32;
        for (index, &unit) in text.iter().enumerate() {
            let mut unit = unit;
            if unit == u16::from(b'<') {
                tag_start = Some(index);
                continue;
            }
            if unit == u16::from(b'>') {
                if let Some(start) = tag_start {
                    let tag = &text[start + 1..index];
                    tag_start = None;
                    match entity(tag) {
                        Some(replacement) => unit = replacement,
                        None => {
                            if is_inline_tag(tag) {
                                width = width.wrapping_add(around.inline_width(tag));
                                previous = -1;
                            } else if let Some(icon) = around.icon_width(tag) {
                                width = width.wrapping_add(icon);
                                previous = -1;
                            }
                            continue;
                        }
                    }
                }
            }
            if tag_start.is_none() {
                let glyph = glyph_for(unit);
                width = width.wrapping_add(i32::from(self.glyph_widths[glyph]));
                width = width.wrapping_add(self.kern(previous, glyph));
                previous = glyph as i32;
            }
        }
        width
    }

    /// `splitLinesToBuffer` with a single width and the trailing space kept; each line is a
    /// `(start, end)` range of units. A result of zero lines is also what an overflowing buffer gives.
    pub(crate) fn split_lines(
        &self,
        text: &[u16],
        max_width: i32,
        around: &mut Surroundings,
    ) -> Vec<(usize, usize)> {
        self.split_lines_with(text, max_width, around, true)
    }

    pub(crate) fn split_lines_with(
        &self,
        text: &[u16],
        max_width: i32,
        around: &mut Surroundings,
        trailing_space: bool,
    ) -> Vec<(usize, usize)> {
        let mut lines: Vec<(usize, usize)> = Vec::new();
        let mut current = 0i32;
        let mut line_start = 0usize;
        let mut last_space: Option<usize> = None;
        let mut last_space_width = 0i32;
        let mut space_trailing = 0usize;
        let mut tag_start: Option<usize> = None;
        let mut previous = -1i32;
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
                let tag = &text[start + 1..index];
                tag_start = None;
                if is(tag, "br") {
                    lines.push((line_start, index + 1));
                    if lines.len() >= LINE_BUFFER {
                        return Vec::new();
                    }
                    line_start = index + 1;
                    current = 0;
                    last_space = None;
                    previous = -1;
                    continue;
                }
                match entity(tag) {
                    Some(replacement) => {
                        let glyph = match replacement {
                            8364 => EURO_GLYPH,
                            other => usize::from(other),
                        };
                        char_width = char_width
                            .wrapping_add(i32::from(self.glyph_widths[glyph]))
                            .wrapping_add(self.kern(previous, glyph));
                        previous = glyph as i32;
                    }
                    None => {
                        if is_inline_tag(tag) {
                            char_width = char_width.wrapping_add(around.inline_width(tag));
                            previous = -1;
                        } else if let Some(icon) = around.icon_width(tag) {
                            char_width = char_width.wrapping_add(icon);
                            previous = -1;
                        }
                    }
                }
            } else {
                char_width = char_width
                    .wrapping_add(i32::from(self.glyph_widths[code]))
                    .wrapping_add(self.kern(previous, code));
                previous = code as i32;
            }
            if char_width > 0 {
                current = current.wrapping_add(char_width);
                if code == 32 {
                    last_space = Some(index);
                    last_space_width = current;
                    space_trailing = usize::from(trailing_space);
                }
                if current > max_width {
                    if let Some(space) = last_space {
                        lines.push((line_start, space + 1 - space_trailing));
                        if lines.len() >= LINE_BUFFER {
                            return Vec::new();
                        }
                        line_start = space + 1;
                        last_space = None;
                        current = current.wrapping_sub(last_space_width);
                    } else {
                        lines.push((line_start, break_pos));
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
            lines.push((line_start, text.len()));
        }
        lines
    }

    /// `getLineCount`.
    pub(crate) fn line_count(&self, text: &[u16], width: i32, around: &mut Surroundings) -> i32 {
        self.split_lines(text, width, around).len() as i32
    }

    /// `getTextWidthMultiline`.
    pub(crate) fn text_width_multiline(
        &self,
        text: &[u16],
        width: i32,
        around: &mut Surroundings,
    ) -> i32 {
        let mut widest = 0;
        for (start, end) in self.split_lines(text, width, around) {
            widest = widest.max(self.text_width(&text[start..end], around));
        }
        widest
    }

    /// `inlineExtraHeight`.
    pub(crate) fn inline_extra_height(
        &self,
        text: &[u16],
        width: i32,
        around: &mut Surroundings,
    ) -> i32 {
        let mut extra = 0i32;
        for (start, end) in self.split_lines(text, width, around) {
            let line = &text[start..end];
            let mut tallest = 0;
            let mut cursor = line.iter().position(|unit| *unit == u16::from(b'<'));
            while let Some(open) = cursor {
                let Some(close) = line[open..]
                    .iter()
                    .position(|unit| *unit == u16::from(b'>'))
                    .map(|offset| open + offset)
                else {
                    break;
                };
                let tag = &line[open + 1..close];
                if is_inline_tag(tag) {
                    if let Some(size) = around.inline_size(tag) {
                        tallest = tallest.max(size.1);
                    }
                }
                cursor = line[close..]
                    .iter()
                    .position(|unit| *unit == u16::from(b'<'))
                    .map(|offset| close + offset);
            }
            let overflow = (tallest - self.line_height()).max(0);
            extra = extra.wrapping_add(overflow / 2).wrapping_add(overflow - overflow / 2);
        }
        extra
    }

    pub(crate) fn position_at_index(
        &self,
        text: &[u16],
        width: i32,
        height: i32,
        index: i32,
        around: &mut Surroundings,
    ) -> Option<(i32, i32)> {
        if index <= 0 {
            return Some((0, height.wrapping_add(self.top_padding)));
        }
        let index = usize::try_from(index).map_or(text.len(), |index| index.min(text.len()));
        let height = if height == 0 {
            self.vertical_spacing
        } else {
            height
        };
        let lines = self.split_lines_with(text, width, around, false);
        let mut counted = 0usize;
        for (position, (start, end)) in lines.iter().enumerate() {
            let length = end - start;
            if counted + length > index || (position == lines.len() - 1 && index == text.len()) {
                let mut piece = &text[counted..index];
                if piece.ends_with(&[60, 98, 114, 62]) {
                    piece = &[];
                }
                let line_width = self.text_width(piece, around);
                let y = self
                    .top_padding
                    .wrapping_add(height.wrapping_mul(position as i32));
                return Some((line_width, y));
            }
            counted += length;
        }
        None
    }

    pub(crate) fn index_at_position(
        &self,
        text: &[u16],
        width: i32,
        line_height: i32,
        x: i32,
        y: i32,
        around: &mut Surroundings,
    ) -> Result<i32, crate::ComponentFailure> {
        let line_height = if line_height == 0 {
            self.vertical_spacing
        } else {
            line_height
        };
        let lines = self.split_lines(text, width, around);
        if lines.is_empty() {
            return Ok(0);
        }
        let y = y.max(0);
        if line_height == 0 {
            return Err(crate::ComponentFailure::DivideByZero);
        }
        let mut row = y.wrapping_div(line_height);
        if row >= lines.len() as i32 {
            row = lines.len() as i32 - 1;
        }
        let row = usize::try_from(row).map_err(|_| crate::ComponentFailure::Index)?;
        let (start, end) = lines[row];
        let line = &text[start..end];
        let mut index = 0usize;
        let mut previous = 0i32;
        let mut current = 0i32;
        while index < line.len() {
            previous = current;
            index += 1;
            current = self.text_width(&line[..index], around);
            if current >= x {
                break;
            }
        }
        let mut index = index as i32;
        if index as usize == line.len() && line.ends_with(&[60, 98, 114, 62]) {
            index -= 4;
        }
        if x.wrapping_sub(previous) < current.wrapping_sub(x) {
            index -= 1;
        }
        for (start, end) in &lines[..row] {
            index += (end - start) as i32;
        }
        Ok(index)
    }
}
