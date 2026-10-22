use crate::canvas::Bitmap;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Frame {
    pub min_x: i32,
    pub min_y: i32,
    pub width: i32,
    pub height: i32,
    pub pixels: Vec<u32>,
    pub has_alpha: bool,
    pub mask: Vec<u8>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpriteGroup {
    pub biggest_width: i32,
    pub biggest_height: i32,
    pub frames: Vec<Frame>,
}

impl SpriteGroup {
    pub fn full(&self, index: usize) -> Option<Bitmap> {
        let frame = self.frames.get(index)?;
        let mut pixels = vec![0u32; (self.biggest_width * self.biggest_height) as usize];
        for y in 0..frame.height {
            for x in 0..frame.width {
                let dx = x + frame.min_x;
                let dy = y + frame.min_y;
                if dx < self.biggest_width && dy < self.biggest_height {
                    pixels[(dy * self.biggest_width + dx) as usize] =
                        frame.pixels[(y * frame.width + x) as usize];
                }
            }
        }
        Some(Bitmap { width: self.biggest_width, height: self.biggest_height, pixels })
    }
}

fn u16_at(data: &[u8], at: usize) -> Option<i32> {
    Some((i32::from(*data.get(at)?) << 8) | i32::from(*data.get(at + 1)?))
}

pub fn decode_sprites(data: &[u8]) -> Option<SpriteGroup> {
    let size = data.len();
    let count = u16_at(data, size.checked_sub(2)?)? as usize;
    if count == 0 {
        return None;
    }
    let header = size.checked_sub(7 + count * 8)?;
    let biggest_width = u16_at(data, header)?;
    let biggest_height = u16_at(data, header + 2)?;
    let palette_length = usize::from(*data.get(header + 4)?) + 1;
    let column = |n: usize, i: usize| u16_at(data, header + 5 + n * count * 2 + i * 2);
    let palette_start = header.checked_sub((palette_length - 1) * 3)?;
    let mut palette = vec![0u32; palette_length];
    for (i, slot) in palette.iter_mut().enumerate().skip(1) {
        let at = palette_start + (i - 1) * 3;
        let color = (u32::from(*data.get(at)?) << 16)
            | (u32::from(*data.get(at + 1)?) << 8)
            | u32::from(*data.get(at + 2)?);
        *slot = if color == 0 { 1 } else { color };
    }
    let mut position = 0usize;
    let mut frames = Vec::with_capacity(count);
    for i in 0..count {
        let min_x = column(0, i)?;
        let min_y = column(1, i)?;
        let width = column(2, i)?;
        let height = column(3, i)?;
        let pixel_count = (width * height) as usize;
        let flags = *data.get(position)?;
        position += 1;
        let column_major = flags & 1 != 0;
        let has_plane = flags & 2 != 0;
        let read = |position: &mut usize| -> Option<Vec<u8>> {
            let mut plane = vec![0u8; pixel_count];
            if column_major {
                for x in 0..width {
                    for y in 0..height {
                        plane[(x + y * width) as usize] = *data.get(*position)?;
                        *position += 1;
                    }
                }
            } else {
                for slot in plane.iter_mut() {
                    *slot = *data.get(*position)?;
                    *position += 1;
                }
            }
            Some(plane)
        };
        let indices = read(&mut position)?;
        let mut alpha = None;
        if has_plane {
            let plane = read(&mut position)?;
            if plane.iter().any(|a| *a != 0xff) {
                alpha = Some(plane);
            }
        }
        let mut pixels = vec![0u32; pixel_count];
        for (k, slot) in pixels.iter_mut().enumerate() {
            let color = palette[indices[k] as usize];
            *slot = match &alpha {
                Some(plane) => (u32::from(plane[k]) << 24) | color,
                None if indices[k] != 0 => 0xff00_0000 | color,
                None => 0,
            };
        }
        let mask = match &alpha {
            Some(plane) => plane.clone(),
            None => indices.iter().map(|v| if *v == 0 { 0 } else { 255 }).collect(),
        };
        frames.push(Frame { min_x, min_y, width, height, pixels, has_alpha: alpha.is_some(), mask });
    }
    Some(SpriteGroup { biggest_width, biggest_height, frames })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn encode(frames: &[(i32, i32, i32, i32, Vec<u8>)], palette: &[u32], bw: i32, bh: i32) -> Vec<u8> {
        let mut out = Vec::new();
        for f in frames {
            out.push(0);
            out.extend(&f.4);
        }
        for c in palette {
            out.extend([(c >> 16) as u8, (c >> 8) as u8, *c as u8]);
        }
        out.extend((bw as u16).to_be_bytes());
        out.extend((bh as u16).to_be_bytes());
        out.push(palette.len() as u8);
        for n in 0..4 {
            for f in frames {
                let v = [f.0, f.1, f.2, f.3][n];
                out.extend((v as u16).to_be_bytes());
            }
        }
        out.extend((frames.len() as u16).to_be_bytes());
        out
    }

    #[test]
    fn decodes_two_frames_with_offsets_and_transparent_index_zero() {
        let data = encode(
            &[(1, 0, 2, 1, vec![1, 0]), (0, 1, 1, 1, vec![2])],
            &[0xff0000, 0x00ff00],
            3,
            2,
        );
        let g = decode_sprites(&data).unwrap();
        assert_eq!(g.frames.len(), 2);
        assert_eq!(g.frames[0].pixels, vec![0xffff0000, 0]);
        let full = g.full(0).unwrap();
        assert_eq!((full.width, full.height), (3, 2));
        assert_eq!(full.pixels[1], 0xffff0000);
        assert_eq!(g.full(1).unwrap().pixels[3], 0xff00ff00);
        assert_eq!(g.frames[1].mask, vec![255]);
    }

    #[test]
    fn rejects_truncated_data() {
        assert!(decode_sprites(&[0, 1]).is_none());
        assert!(decode_sprites(&[]).is_none());
    }
}
