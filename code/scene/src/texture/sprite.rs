use super::packet::PacketError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Sprite {
    pub width: i32,
    pub height: i32,
    pub pixels: Vec<i32>,
}

fn u8_at(data: &[u8], at: usize) -> Result<i32, PacketError> {
    data.get(at).map(|b| *b as i32).ok_or(PacketError::Truncated { position: at, needed: 1, len: data.len() })
}

fn u16_at(data: &[u8], at: usize) -> Result<i32, PacketError> {
    Ok((u8_at(data, at)? << 8) | u8_at(data, at + 1)?)
}

fn u24_at(data: &[u8], at: usize) -> Result<i32, PacketError> {
    Ok((u16_at(data, at)? << 8) | u8_at(data, at + 2)?)
}

fn offset(base: usize, back: usize) -> Result<usize, PacketError> {
    base.checked_sub(back).ok_or(PacketError::Truncated { position: 0, needed: back, len: base })
}

pub fn decode_first_frame(data: &[u8]) -> Result<Sprite, PacketError> {
    let size = data.len();
    let count = u16_at(data, offset(size, 2)?)? as usize;
    if count == 0 {
        return Err(PacketError::Truncated { position: size, needed: 1, len: size });
    }
    let header = offset(size, 7 + count * 8)?;
    let biggest_width = u16_at(data, header)?;
    let biggest_height = u16_at(data, header + 2)?;
    let palette_length = u8_at(data, header + 4)? as usize + 1;
    let min_x = u16_at(data, header + 5)?;
    let min_y = u16_at(data, header + 5 + count * 2)?;
    let width = u16_at(data, header + 5 + count * 4)?;
    let height = u16_at(data, header + 5 + count * 6)?;
    let palette_start = offset(header, (palette_length - 1) * 3)?;
    let mut palette = vec![0i32; palette_length];
    for (i, slot) in palette.iter_mut().enumerate().skip(1) {
        let color = u24_at(data, palette_start + (i - 1) * 3)?;
        *slot = if color == 0 { 1 } else { color };
    }
    let pixel_count = (width * height) as usize;
    let flags = u8_at(data, 0)?;
    let mut position = 1usize;
    let mut indices = vec![0u8; pixel_count];
    let column_major = flags & 0x1 != 0;
    let mut read_plane = |plane: &mut [u8], position: &mut usize| -> Result<(), PacketError> {
        if column_major {
            for x in 0..width {
                for y in 0..height {
                    plane[(x + y * width) as usize] = data[*position];
                    *position += 1;
                }
            }
        } else {
            for slot in plane.iter_mut() {
                *slot = data[*position];
                *position += 1;
            }
        }
        Ok(())
    };
    let needed = if flags & 0x2 != 0 { pixel_count * 2 } else { pixel_count };
    if position + needed > size {
        return Err(PacketError::Truncated { position, needed, len: size });
    }
    read_plane(&mut indices, &mut position)?;
    let mut alpha = None;
    if flags & 0x2 != 0 {
        let mut plane = vec![0u8; pixel_count];
        read_plane(&mut plane, &mut position)?;
        if plane.iter().any(|a| *a != 0xff) {
            alpha = Some(plane);
        }
    }
    let full_width = biggest_width;
    let full_height = biggest_height;
    let mut pixels = vec![0i32; (full_width * full_height) as usize];
    for y in 0..height {
        let src = (y * width) as usize;
        let dest = (full_width * (y + min_y) + min_x) as usize;
        for x in 0..width as usize {
            let color = palette[indices[src + x] as usize];
            pixels[dest + x] = match &alpha {
                Some(plane) => ((plane[src + x] as i32) << 24) | color,
                None if color != 0 => (0xff << 24) | color,
                None => 0,
            };
        }
    }
    Ok(Sprite { width: full_width, height: full_height, pixels })
}
