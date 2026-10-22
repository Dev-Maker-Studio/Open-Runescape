use super::ops::divide;
use super::random::JavaRandom;
use crate::Error;

#[derive(Debug, Clone)]
pub(crate) struct Params {
    pub(crate) seed: i32,
    pub(crate) min_width: i32,
    pub(crate) max_width: i32,
    pub(crate) min_height: i32,
    pub(crate) max_height: i32,
    pub(crate) offset_variation: i32,
    pub(crate) corner_mode: i32,
    pub(crate) height_variation: i32,
    pub(crate) brightness_variation: i32,
}

struct Painter<'a> {
    rows: Vec<Vec<i32>>,
    width: i32,
    row_end: i32,
    params: &'a Params,
    corner_radius: i32,
}

const OUT_OF_RANGE: Error = Error::InvalidTexture("irregular bricks out of range");

impl Painter<'_> {
    fn row_mut(&mut self, index: i32) -> Result<&mut Vec<i32>, Error> {
        usize::try_from(index)
            .ok()
            .and_then(|index| self.rows.get_mut(index))
            .ok_or(OUT_OF_RANGE)
    }

    fn set(&mut self, row: i32, column: i32, value: i32) -> Result<(), Error> {
        let index = (column & self.row_end) as usize;
        let slot = self.row_mut(row)?.get_mut(index);
        *slot.ok_or(OUT_OF_RANGE)? = value;
        Ok(())
    }

    fn fill(&mut self, row: i32, position: i32, count: i32, value: i32) -> Result<(), Error> {
        if count <= 0 {
            return Ok(());
        }
        let line = self.row_mut(row)?;
        let (start, end) = (
            usize::try_from(position).map_err(|_| OUT_OF_RANGE)?,
            (position + count) as usize,
        );
        line.get_mut(start..end).ok_or(OUT_OF_RANGE)?.fill(value);
        Ok(())
    }

    fn fill_wrapped(&mut self, row: i32, start: i32, count: i32, value: i32) -> Result<(), Error> {
        if count + start > self.width {
            let first = self.width - start;
            self.fill(row, start, first, value)?;
            self.fill(row, 0, count - first, value)
        } else {
            self.fill(row, start, count, value)
        }
    }

    fn draw(
        &mut self,
        random: &mut JavaRandom,
        start_x: i32,
        start_y: i32,
        brick_width: i32,
        brick_height: i32,
    ) -> Result<(), Error> {
        let p = self.params;
        let brightness = if p.brightness_variation > 0 {
            4096 - random.bounded(p.brightness_variation)?
        } else {
            4096
        };
        let corner_variation = p.height_variation.wrapping_mul(self.corner_radius) >> 12;
        let radius = self.corner_radius
            - if corner_variation > 0 {
                random.bounded(corner_variation)?
            } else {
                0
            };
        let mut x0 = start_x;
        if x0 >= self.width {
            x0 -= self.width;
        }
        if radius > 0 {
            if brick_height > 0 && brick_width > 0 {
                let corner_w = (brick_width / 2).min(radius);
                let corner_h = (brick_height / 2).min(radius);
                let inner_x = x0 + corner_w;
                let inner_w = brick_width - corner_w * 2;
                for row in 0..brick_height {
                    let y = row + start_y;
                    let (edge_row, level) = if row < corner_h {
                        (true, divide(brightness * row, corner_h)?)
                    } else {
                        let from_bottom = brick_height - row - 1;
                        if from_bottom < corner_h {
                            (true, divide(brightness * from_bottom, corner_h)?)
                        } else {
                            (false, brightness)
                        }
                    };
                    for column in 0..corner_w {
                        let value = if edge_row {
                            let side = divide(column * brightness, corner_w)?;
                            if p.corner_mode == 0 {
                                side.wrapping_mul(level) >> 12
                            } else {
                                side.min(level)
                            }
                        } else {
                            divide(column * brightness, corner_w)?
                        };
                        self.set(y, x0 + column, value)?;
                        self.set(y, x0 + brick_width - column - 1, value)?;
                    }
                    if inner_w + inner_x > self.width {
                        let first = self.width - inner_x;
                        self.fill(y, inner_x, first, level)?;
                        self.fill(y, 0, inner_w - first, level)?;
                    } else {
                        self.fill(y, inner_x, inner_w, level)?;
                    }
                }
            }
        } else {
            for row in 0..brick_height {
                self.fill_wrapped(row + start_y, x0, brick_width, brightness)?;
            }
        }
        Ok(())
    }
}

fn wrap(value: i32, width: i32) -> i32 {
    let value = if value < 0 { value + width } else { value };
    if value > width {
        value - width
    } else {
        value
    }
}

pub(crate) fn render(
    params: &Params,
    width: usize,
    height: usize,
    row_end: i32,
) -> Result<Vec<Vec<i32>>, Error> {
    let (w, h) = (width as i32, height as i32);
    let mut painter = Painter {
        rows: vec![vec![0; width]; height],
        width: w,
        row_end,
        params,
        corner_radius: 0,
    };
    let min_w = params.min_width.wrapping_mul(w) >> 12;
    let max_w = params.max_width.wrapping_mul(w) >> 12;
    let min_h = params.min_height.wrapping_mul(h) >> 12;
    let max_h = params.max_height.wrapping_mul(h) >> 12;
    if max_h <= 1 {
        return Ok(painter.rows);
    }
    painter.corner_radius = (w / 8).wrapping_mul(params.offset_variation) >> 12;
    let slots = (divide(w, min_w)? + 1) as usize;
    let mut current_slots = vec![[0i32; 3]; slots];
    let mut previous_slots = vec![[0i32; 3]; slots];
    let mut random = JavaRandom::new(i64::from(params.seed));
    let (mut previous_offset, mut current_offset, mut before_offset) = (0, 0, 0);
    let (mut end_x, mut previous_index) = (0, 0usize);
    let (mut first_row, mut first_column) = (true, true);
    let (mut previous_count, mut current_count) = (0usize, 0usize);
    for _ in 0..(1 << 22) {
        let mut brick_w = min_w + random.bounded(max_w - min_w)?;
        let mut brick_h = min_h + random.bounded(max_h - min_h)?;
        let mut next_end = brick_w + end_x;
        if next_end > w {
            brick_w = w - end_x;
            next_end = w;
        }
        let start_y;
        if first_column {
            start_y = 0;
        } else {
            let mut index = previous_index;
            let anchor = *previous_slots.get(previous_index).ok_or(OUT_OF_RANGE)?;
            let mut search = 0usize;
            let wrapped_end = wrap(next_end + previous_offset, w);
            let mut found = None;
            for _ in 0..=previous_count.max(1) {
                let slot = previous_slots.get(index).ok_or(OUT_OF_RANGE)?;
                if wrapped_end >= slot[0] && wrapped_end <= slot[1] {
                    found = Some(anchor[2]);
                    break;
                }
                index += 1;
                if index >= previous_count {
                    index = 0;
                }
                search += 1;
            }
            let mut top = found.ok_or(OUT_OF_RANGE)?;
            if index != previous_index {
                let wrapped_start = wrap(end_x + previous_offset, w);
                if previous_count == 0 {
                    return Err(OUT_OF_RANGE);
                }
                for gap in 1..=search {
                    top = top.max(previous_slots[(gap + previous_index) % previous_count][2]);
                }
                for gap in 0..=search {
                    let slot = previous_slots[(gap + previous_index) % previous_count];
                    if slot[2] != top {
                        let (left, right) = if wrapped_start < wrapped_end {
                            (wrapped_start.max(slot[0]), wrapped_end.min(slot[1]))
                        } else if slot[0] == 0 {
                            (0, wrapped_end.min(slot[1]))
                        } else {
                            (wrapped_start.max(slot[0]), w)
                        };
                        painter.draw(
                            &mut random,
                            left + before_offset,
                            slot[2],
                            right - left,
                            top - slot[2],
                        )?;
                    }
                }
            }
            previous_index = index;
            start_y = top;
        }
        if start_y + brick_h > h {
            brick_h = h - start_y;
        } else {
            first_row = false;
        }
        if next_end == w {
            painter.draw(
                &mut random,
                current_offset + end_x,
                start_y,
                brick_w,
                brick_h,
            )?;
            if first_row {
                return Ok(painter.rows);
            }
            first_row = true;
            *current_slots.get_mut(current_count).ok_or(OUT_OF_RANGE)? =
                [end_x, next_end, brick_h + start_y];
            current_count += 1;
            std::mem::swap(&mut previous_slots, &mut current_slots);
            previous_count = current_count;
            current_count = 0;
            before_offset = current_offset;
            current_offset = random.bounded(w)?;
            previous_offset = current_offset - before_offset;
            end_x = 0;
            let target = wrap(previous_offset, w);
            previous_index = 0;
            let mut located = false;
            for _ in 0..=previous_count {
                let slot = previous_slots.get(previous_index).ok_or(OUT_OF_RANGE)?;
                if target >= slot[0] && target <= slot[1] {
                    located = true;
                    break;
                }
                previous_index += 1;
                if previous_index >= previous_count {
                    previous_index = 0;
                }
            }
            if !located {
                return Err(OUT_OF_RANGE);
            }
            first_column = false;
        } else {
            *current_slots.get_mut(current_count).ok_or(OUT_OF_RANGE)? =
                [end_x, next_end, brick_h + start_y];
            current_count += 1;
            painter.draw(
                &mut random,
                current_offset + end_x,
                start_y,
                brick_w,
                brick_h,
            )?;
            end_x = next_end;
        }
    }
    Err(Error::InvalidTexture("irregular bricks did not finish"))
}
