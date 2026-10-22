use crate::{Error, Reader};

pub(crate) fn decode_first(data: &[u8]) -> Result<(Vec<i32>, usize, usize), Error> {
    let bad = Error::InvalidTexture("sprite");
    if data.len() < 2 {
        return Err(bad);
    }
    let mut reader = Reader::new(data);
    reader.position = data.len() - 2;
    let count = usize::from(reader.u16()?);
    if count == 0 {
        return Err(bad);
    }
    let header = data
        .len()
        .checked_sub(7 + count * 8)
        .ok_or(Error::InvalidTexture("sprite"))?;
    reader.position = header;
    let biggest_width = i32::from(reader.u16()?);
    let biggest_height = i32::from(reader.u16()?);
    let palette_length = usize::from(reader.u8()?) + 1;
    let mut min_x = Vec::new();
    let mut min_y = Vec::new();
    let mut widths = Vec::new();
    let mut heights = Vec::new();
    for _ in 0..count {
        min_x.push(i32::from(reader.u16()?));
    }
    for _ in 0..count {
        min_y.push(i32::from(reader.u16()?));
    }
    for _ in 0..count {
        widths.push(i32::from(reader.u16()?));
    }
    for _ in 0..count {
        heights.push(i32::from(reader.u16()?));
    }
    let palette_start = header
        .checked_sub((palette_length - 1) * 3)
        .ok_or(Error::InvalidTexture("sprite"))?;
    reader.position = palette_start;
    let mut palette = vec![0i32; palette_length];
    for slot in palette.iter_mut().skip(1) {
        *slot = reader.medium()?;
        if *slot == 0 {
            *slot = 1;
        }
    }
    reader.position = 0;
    let width = widths[0];
    let height = heights[0];
    let pixel_count = (width * height) as usize;
    let flags = reader.u8()?;
    let mut indices = vec![0u8; pixel_count];
    if flags & 1 == 0 {
        for slot in indices.iter_mut() {
            *slot = reader.u8()?;
        }
    } else {
        for x in 0..width as usize {
            for y in 0..height as usize {
                indices[x + y * width as usize] = reader.u8()?;
            }
        }
    }
    let full_width = width + min_x[0] + (biggest_width - width - min_x[0]);
    let full_height = height + min_y[0] + (biggest_height - height - min_y[0]);
    if full_width <= 0
        || full_height <= 0
        || min_x[0] + width > full_width
        || min_y[0] + height > full_height
    {
        return Err(Error::InvalidTexture("sprite"));
    }
    let (fw, fh) = (full_width as usize, full_height as usize);
    let mut out = vec![0i32; fw * fh];
    for y in 0..height as usize {
        let mut dest = fw * (y + min_y[0] as usize) + min_x[0] as usize;
        for x in 0..width as usize {
            let index = usize::from(indices[y * width as usize + x]);
            out[dest] = *palette
                .get(index)
                .ok_or(Error::InvalidTexture("sprite palette"))?;
            dest += 1;
        }
    }
    Ok((out, fw, fh))
}
