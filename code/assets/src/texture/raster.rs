use super::ops::divide;
use crate::Error;

#[derive(Debug, Clone)]
pub(crate) enum Shape {
    Line {
        from: [i32; 2],
        to: [i32; 2],
        stroke: i32,
    },
    Bezier {
        points: [i32; 8],
        stroke: i32,
    },
    Rectangle {
        left: i32,
        top: i32,
        right: i32,
        bottom: i32,
        fill: i32,
        stroke: i32,
        width: i32,
    },
    Ellipse {
        center: [i32; 2],
        radius: [i32; 2],
        fill: i32,
        stroke: i32,
        width: i32,
    },
}

pub(crate) struct Canvas {
    pub(crate) rows: Vec<Vec<i32>>,
    max_x: i32,
    max_y: i32,
    inner_radius: Vec<i32>,
}

fn bad() -> Error {
    Error::InvalidTexture("rasterizer out of bounds")
}

fn clamp(value: i32, min: i32, max: i32) -> i32 {
    if value < min {
        min
    } else {
        value.min(max)
    }
}

impl Canvas {
    pub(crate) fn new(width: usize, height: usize, max_x: i32, max_y: i32) -> Self {
        Self {
            rows: vec![vec![0; width]; height],
            max_x,
            max_y,
            inner_radius: Vec::new(),
        }
    }

    fn row(&mut self, y: i32) -> Result<&mut Vec<i32>, Error> {
        usize::try_from(y)
            .ok()
            .and_then(|index| self.rows.get_mut(index))
            .ok_or_else(bad)
    }

    fn set(&mut self, y: i32, x: i32, color: i32) -> Result<(), Error> {
        let row = self.row(y)?;
        let slot = usize::try_from(x)
            .ok()
            .and_then(|index| row.get_mut(index))
            .ok_or_else(bad)?;
        *slot = color;
        Ok(())
    }

    fn span(&mut self, y: i32, from: i32, to: i32, color: i32) -> Result<(), Error> {
        let row = self.row(y)?;
        if from > to {
            return Ok(());
        }
        if from < 0 || to as usize >= row.len() {
            return Err(bad());
        }
        for slot in &mut row[from as usize..=to as usize] {
            *slot = color;
        }
        Ok(())
    }

    fn vertical(&mut self, x: i32, y1: i32, y2: i32, color: i32) -> Result<(), Error> {
        let (start, end) = if y1 > y2 { (y2, y1) } else { (y1, y2) };
        for y in start..end {
            self.set(y, x, color)?;
        }
        Ok(())
    }

    fn clipped_vertical(&mut self, x: i32, y1: i32, y2: i32, color: i32) -> Result<(), Error> {
        if x >= 0 && x <= self.max_x {
            let a = clamp(y1, 0, self.max_y);
            let b = clamp(y2, 0, self.max_y);
            self.vertical(x, a, b, color)?;
        }
        Ok(())
    }

    fn horizontal(&mut self, x1: i32, x2: i32, y: i32, color: i32) -> Result<(), Error> {
        if x1 > x2 {
            self.span(y, x2, x1, color)
        } else {
            self.span(y, x1, x2, color)
        }
    }

    fn clipped_horizontal(&mut self, x1: i32, x2: i32, y: i32, color: i32) -> Result<(), Error> {
        if y >= 0 && y <= self.max_y {
            let a = clamp(x1, 0, self.max_x);
            let b = clamp(x2, 0, self.max_x);
            self.horizontal(a, b, y, color)?;
        }
        Ok(())
    }

    fn bresenham(&mut self, x1: i32, y1: i32, x2: i32, y2: i32, color: i32) -> Result<(), Error> {
        let (mut start_x, mut start_y, mut end_x, mut end_y) = (x1, y1, x2, y2);
        let dy = end_y.wrapping_sub(start_y);
        let dx = end_x.wrapping_sub(start_x);
        if dx == 0 {
            if dy != 0 {
                self.vertical(start_x, start_y, end_y, color)?;
            }
            return Ok(());
        }
        if dy == 0 {
            return self.horizontal(start_x, end_x, start_y, color);
        }
        let steep = dy.wrapping_abs() > dx.wrapping_abs();
        if steep {
            std::mem::swap(&mut start_x, &mut start_y);
            std::mem::swap(&mut end_x, &mut end_y);
        }
        if start_x > end_x {
            std::mem::swap(&mut start_x, &mut end_x);
            std::mem::swap(&mut start_y, &mut end_y);
        }
        let mut current = start_y;
        let run = end_x.wrapping_sub(start_x);
        let mut delta_y = end_y.wrapping_sub(start_y);
        let mut error = -(run >> 1);
        let y_step = if start_y < end_y { 1 } else { -1 };
        if delta_y < 0 {
            delta_y = -delta_y;
        }
        for i in start_x..=end_x {
            if steep {
                self.set(i, current, color)?;
            } else {
                self.set(current, i, color)?;
            }
            error += delta_y;
            if error > 0 {
                current += y_step;
                error -= run;
            }
        }
        Ok(())
    }

    fn line(&mut self, x1: i32, y1: i32, x2: i32, y2: i32, color: i32) -> Result<(), Error> {
        let delta_x = x2.wrapping_sub(x1);
        let delta_y = y2.wrapping_sub(y1);
        if delta_x == 0 {
            if delta_y != 0 {
                self.clipped_vertical(x1, y1, y2, color)?;
            }
            return Ok(());
        }
        if delta_y == 0 {
            return self.clipped_horizontal(x1, x2, y1, color);
        }
        let slope = divide(delta_y << 12, delta_x)?;
        let intercept = y1.wrapping_sub(x1.wrapping_mul(slope) >> 12);
        let (max_x, max_y) = (self.max_x, self.max_y);
        let project = |x: i32| (slope.wrapping_mul(x) >> 12).wrapping_add(intercept);
        let (start_x, start_y) = if x1 < 0 {
            (0, project(0))
        } else if x1 > max_x {
            (max_x, project(max_x))
        } else {
            (x1, y1)
        };
        let (end_x, end_y) = if x2 < 0 {
            (0, project(0))
        } else if x2 > max_x {
            (max_x, project(max_x))
        } else {
            (x2, y2)
        };
        let mut a = (start_x, start_y);
        let mut b = (end_x, end_y);
        for point in [&mut a, &mut b] {
            if point.1 < 0 {
                point.1 = 0;
                point.0 = divide(0i32.wrapping_sub(intercept) << 12, slope)?;
            } else if point.1 > max_y {
                point.1 = max_y;
                point.0 = divide(max_y.wrapping_sub(intercept) << 12, slope)?;
            }
        }
        self.bresenham(a.0, a.1, b.0, b.1, color)
    }

    fn bezier(&mut self, p: [i32; 8], color: i32) -> Result<(), Error> {
        let inside = (0..4).all(|i| {
            p[i * 2] >= 0
                && p[i * 2] <= self.max_x
                && p[i * 2 + 1] >= 0
                && p[i * 2 + 1] <= self.max_y
        });
        let [p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y] = p;
        let straight = p0x == p1x && p1y == p0y && p2x == p3x && p2y == p3y;
        if straight {
            return if inside {
                self.bresenham(p0x, p0y, p3x, p3y, color)
            } else {
                self.line(p0x, p0y, p3x, p3y, color)
            };
        }
        let (mut prev_x, mut prev_y) = (p0x, p0y);
        let (p0x3, p0y3) = (p0x.wrapping_mul(3), p0y.wrapping_mul(3));
        let (p1x3, p1y3) = (p1x.wrapping_mul(3), p1y.wrapping_mul(3));
        let (p2x3, p2y3) = (p2x.wrapping_mul(3), p2y.wrapping_mul(3));
        let ax = p3x.wrapping_sub(p2x3).wrapping_add(p1x3).wrapping_sub(p0x);
        let ay = p1y3.wrapping_add(p3y.wrapping_sub(p2y3)).wrapping_sub(p0y);
        let bx = p0x3.wrapping_add(p2x3.wrapping_sub(p1x3).wrapping_sub(p1x3));
        let by = p0y3.wrapping_add(p2y3.wrapping_sub(p1y3).wrapping_sub(p1y3));
        let cx = p1x3.wrapping_sub(p0x3);
        let cy = p1y3.wrapping_sub(p0y3);
        let mut t = 128i32;
        while t <= 4096 {
            let squared = t.wrapping_mul(t) >> 12;
            let cubed = squared.wrapping_mul(t) >> 12;
            let x_sum = bx
                .wrapping_mul(squared)
                .wrapping_add(ax.wrapping_mul(cubed))
                .wrapping_add(t.wrapping_mul(cx));
            let y_sum = by
                .wrapping_mul(squared)
                .wrapping_add(ay.wrapping_mul(cubed))
                .wrapping_add(t.wrapping_mul(cy));
            let current_x = p0x.wrapping_add(x_sum >> 12);
            let current_y = (y_sum >> 12).wrapping_add(p0y);
            if inside {
                self.bresenham(prev_x, prev_y, current_x, current_y, color)?;
            } else {
                self.line(prev_x, prev_y, current_x, current_y, color)?;
            }
            prev_x = current_x;
            prev_y = current_y;
            t += 128;
        }
        Ok(())
    }

    fn inside(&self, left: i32, right: i32, top: i32, bottom: i32) -> bool {
        left >= 0 && right <= self.max_x && top >= 0 && bottom <= self.max_y
    }

    fn fill_rectangle(
        &mut self,
        left: i32,
        right: i32,
        top: i32,
        bottom: i32,
        color: i32,
    ) -> Result<(), Error> {
        if self.inside(left, right, top, bottom) {
            for y in top..=bottom {
                self.span(y, left, right, color)?;
            }
        } else {
            let top = clamp(top, 0, self.max_y);
            let bottom = clamp(bottom, 0, self.max_y);
            let left = clamp(left, 0, self.max_x);
            let right = clamp(right, 0, self.max_x);
            for y in top..=bottom {
                self.span(y, left, right, color)?;
            }
        }
        Ok(())
    }

    fn stroke_rectangle(
        &mut self,
        left: i32,
        right: i32,
        top: i32,
        bottom: i32,
        color: i32,
        thickness: i32,
    ) -> Result<(), Error> {
        if self.inside(left, right, top, bottom) {
            if thickness == 1 {
                self.span(top, left, right, color)?;
                self.span(bottom, left, right, color)?;
                for y in top + 1..=bottom - 1 {
                    self.set(y, left, color)?;
                    self.set(y, right, color)?;
                }
            } else {
                let top_border = thickness.wrapping_add(top);
                let bottom_border = bottom.wrapping_sub(thickness);
                let mut row = top;
                while row < top_border {
                    self.span(row, left, right, color)?;
                    row += 1;
                }
                row = bottom;
                while row > bottom_border {
                    self.span(row, left, right, color)?;
                    row -= 1;
                }
                let inner_left = thickness.wrapping_add(left);
                let right_border = right.wrapping_sub(thickness);
                for y in top_border..=bottom_border {
                    self.span(y, left, inner_left, color)?;
                    self.span(y, right_border, right, color)?;
                }
            }
            return Ok(());
        }
        if thickness == 1 {
            return self.clipped_stroke_thin(left, right, top, bottom, color);
        }
        let clipped_top = clamp(top, 0, self.max_y);
        let clipped_bottom = clamp(bottom, 0, self.max_y);
        let clipped_left = clamp(left, 0, self.max_x);
        let clipped_right = clamp(right, 0, self.max_x);
        let top_end = clamp(thickness.wrapping_add(top), 0, self.max_y);
        let bottom_start = clamp(bottom.wrapping_sub(thickness), 0, self.max_y);
        let mut y = clipped_top;
        while y < top_end {
            self.span(y, clipped_left, clipped_right, color)?;
            y += 1;
        }
        y = clipped_bottom;
        while y > bottom_start {
            self.span(y, clipped_left, clipped_right, color)?;
            y -= 1;
        }
        let inner_left = clamp(thickness.wrapping_add(left), 0, self.max_x);
        let right_start = clamp(right.wrapping_sub(thickness), 0, self.max_x);
        for row in top_end..=bottom_start {
            self.span(row, clipped_left, inner_left, color)?;
            self.span(row, right_start, clipped_right, color)?;
        }
        Ok(())
    }

    fn clipped_stroke_thin(
        &mut self,
        left: i32,
        right: i32,
        top: i32,
        bottom: i32,
        color: i32,
    ) -> Result<(), Error> {
        let (mut left, mut right, mut top, mut bottom) = (left, right, top, bottom);
        if top <= self.max_y && bottom >= 0 {
            let left_visible = if left < 0 {
                left = 0;
                false
            } else if left > self.max_x {
                left = self.max_x;
                false
            } else {
                true
            };
            let right_visible = if right < 0 {
                right = 0;
                false
            } else if right > self.max_x {
                right = self.max_x;
                false
            } else {
                true
            };
            if top >= 0 {
                self.span(top, left, right, color)?;
                top += 1;
            } else {
                top = 0;
            }
            if bottom <= self.max_y {
                self.span(bottom, left, right, color)?;
                bottom -= 1;
            } else {
                bottom = self.max_y;
            }
            for y in top..=bottom {
                if left_visible {
                    self.set(y, left, color)?;
                }
                if right_visible {
                    self.set(y, right, color)?;
                }
            }
        }
        Ok(())
    }

    fn fill_stroke_rectangle(
        &mut self,
        left: i32,
        right: i32,
        top: i32,
        bottom: i32,
        fill: i32,
        stroke: i32,
        thickness: i32,
    ) -> Result<(), Error> {
        let (
            clipped_top,
            clipped_bottom,
            clipped_left,
            clipped_right,
            top_end,
            bottom_start,
            inner_left,
            right_start,
        );
        if self.inside(left, right, top, bottom) {
            clipped_top = top;
            clipped_bottom = bottom;
            clipped_left = left;
            clipped_right = right;
            top_end = top.wrapping_add(thickness);
            bottom_start = bottom.wrapping_sub(thickness);
            inner_left = left.wrapping_add(thickness);
            right_start = right.wrapping_sub(thickness);
        } else {
            clipped_top = clamp(top, 0, self.max_y);
            clipped_bottom = clamp(bottom, 0, self.max_y);
            clipped_left = clamp(left, 0, self.max_x);
            clipped_right = clamp(right, 0, self.max_x);
            top_end = clamp(top.wrapping_add(thickness), 0, self.max_y);
            bottom_start = clamp(bottom.wrapping_sub(thickness), 0, self.max_y);
            inner_left = clamp(left.wrapping_add(thickness), 0, self.max_x);
            right_start = clamp(right.wrapping_sub(thickness), 0, self.max_x);
        }
        let mut y = clipped_top;
        while y < top_end {
            self.span(y, clipped_left, clipped_right, stroke)?;
            y += 1;
        }
        y = clipped_bottom;
        while y > bottom_start {
            self.span(y, clipped_left, clipped_right, stroke)?;
            y -= 1;
        }
        for row in top_end..=bottom_start {
            self.span(row, clipped_left, inner_left, stroke)?;
            self.span(row, inner_left, right_start, fill)?;
            self.span(row, right_start, clipped_right, stroke)?;
        }
        Ok(())
    }

    fn inner_get(&self, index: i32) -> Result<i32, Error> {
        usize::try_from(index)
            .ok()
            .and_then(|i| self.inner_radius.get(i).copied())
            .ok_or_else(bad)
    }

    fn inner_set(&mut self, index: i32, value: i32) -> Result<(), Error> {
        let slot = usize::try_from(index)
            .ok()
            .and_then(|i| self.inner_radius.get_mut(i))
            .ok_or_else(bad)?;
        *slot = value;
        Ok(())
    }

    fn triple(
        &mut self,
        y: i32,
        a: i32,
        b: i32,
        c: i32,
        d: i32,
        fill: i32,
        stroke: i32,
    ) -> Result<(), Error> {
        self.span(y, a, b, stroke)?;
        self.span(y, b, c, fill)?;
        self.span(y, c, d, stroke)
    }

    fn ellipse(
        &mut self,
        cx: i32,
        cy: i32,
        orx: i32,
        ory: i32,
        fill: i32,
        stroke: i32,
        thickness: i32,
    ) -> Result<(), Error> {
        if ory == orx {
            return self.circle(cx, cy, orx, fill, stroke, thickness);
        }
        let unclipped =
            cx - orx >= 0 && cx + orx <= self.max_x && cy - ory >= 0 && ory + cy <= self.max_y;
        let (max_x, max_y) = (self.max_x, self.max_y);
        let mut outer_x = 0;
        let mut outer_y = ory;
        let mut inner_x = 0;
        let irx = orx - thickness;
        let iry = ory - thickness;
        let orx_sq = orx.wrapping_mul(orx);
        let ory_sq = ory.wrapping_mul(ory);
        let irx_sq = irx.wrapping_mul(irx);
        let iry_sq = iry.wrapping_mul(iry);
        let ory_sq2 = ory_sq << 1;
        let orx_sq2 = orx_sq << 1;
        let iry_sq2 = iry_sq << 1;
        let irx_sq2 = irx_sq << 1;
        let outer_diameter = ory << 1;
        let inner_diameter = iry << 1;
        let mut outer_a = ory_sq2.wrapping_add((1 - outer_diameter).wrapping_mul(orx_sq));
        let mut outer_b = ory_sq.wrapping_sub(orx_sq2.wrapping_mul(outer_diameter - 1));
        let mut inner_a = iry_sq2.wrapping_add((1 - inner_diameter).wrapping_mul(irx_sq));
        let mut inner_b = iry_sq.wrapping_sub(irx_sq2.wrapping_mul(inner_diameter - 1));
        let orx_sq4 = orx_sq << 2;
        let ory_sq4 = ory_sq << 2;
        let irx_sq4 = irx_sq << 2;
        let iry_sq4 = iry_sq << 2;
        let mut outer_grad_a = ory_sq2.wrapping_mul(3);
        let mut outer_grad_b = orx_sq2.wrapping_mul(outer_diameter - 3);
        let mut inner_grad_a = iry_sq2.wrapping_mul(3);
        let mut inner_grad_b = irx_sq2.wrapping_mul(inner_diameter - 3);
        let mut outer_step_a = ory_sq4;
        let mut outer_step_b = (ory - 1).wrapping_mul(orx_sq4);
        let mut inner_step_a = iry_sq4;
        let mut inner_step_b = irx_sq4.wrapping_mul(iry - 1);
        if unclipped {
            self.triple(cy, cx - orx, cx - irx, cx + irx, cx + orx, fill, stroke)?;
        } else if cy >= 0 && cy <= max_y {
            let left = clamp(cx - orx, 0, max_x);
            let right = clamp(cx + orx, 0, max_x);
            let inner_left = clamp(cx - irx, 0, max_x);
            let inner_right = clamp(cx + irx, 0, max_x);
            self.triple(cy, left, inner_left, inner_right, right, fill, stroke)?;
        }
        while outer_y > 0 {
            let within_inner = outer_y <= iry;
            if within_inner {
                if inner_a < 0 {
                    while inner_a < 0 {
                        inner_a = inner_a.wrapping_add(inner_grad_a);
                        inner_b = inner_b.wrapping_add(inner_step_a);
                        inner_grad_a = inner_grad_a.wrapping_add(iry_sq4);
                        inner_step_a = inner_step_a.wrapping_add(iry_sq4);
                        inner_x += 1;
                    }
                }
                if inner_b < 0 {
                    inner_a = inner_a.wrapping_add(inner_grad_a);
                    inner_b = inner_b.wrapping_add(inner_step_a);
                    inner_grad_a = inner_grad_a.wrapping_add(iry_sq4);
                    inner_step_a = inner_step_a.wrapping_add(iry_sq4);
                    inner_x += 1;
                }
                inner_a = inner_a.wrapping_add(inner_step_b.wrapping_neg());
                inner_b = inner_b.wrapping_add(inner_grad_b.wrapping_neg());
                inner_grad_b = inner_grad_b.wrapping_sub(irx_sq4);
                inner_step_b = inner_step_b.wrapping_sub(irx_sq4);
            }
            if outer_a < 0 {
                while outer_a < 0 {
                    outer_a = outer_a.wrapping_add(outer_grad_a);
                    outer_b = outer_b.wrapping_add(outer_step_a);
                    outer_grad_a = outer_grad_a.wrapping_add(ory_sq4);
                    outer_step_a = outer_step_a.wrapping_add(ory_sq4);
                    outer_x += 1;
                }
            }
            if outer_b < 0 {
                outer_a = outer_a.wrapping_add(outer_grad_a);
                outer_b = outer_b.wrapping_add(outer_step_a);
                outer_grad_a = outer_grad_a.wrapping_add(ory_sq4);
                outer_step_a = outer_step_a.wrapping_add(ory_sq4);
                outer_x += 1;
            }
            outer_a = outer_a.wrapping_add(outer_step_b.wrapping_neg());
            outer_b = outer_b.wrapping_add(outer_grad_b.wrapping_neg());
            outer_grad_b = outer_grad_b.wrapping_sub(orx_sq4);
            outer_step_b = outer_step_b.wrapping_sub(orx_sq4);
            outer_y -= 1;
            let top = cy - outer_y;
            let bottom = outer_y + cy;
            if unclipped {
                let right_edge = cx + outer_x;
                let left_edge = cx - outer_x;
                if within_inner {
                    let inner_right = cx + inner_x;
                    let inner_left = cx - inner_x;
                    self.triple(
                        top,
                        left_edge,
                        inner_left,
                        inner_right,
                        right_edge,
                        fill,
                        stroke,
                    )?;
                    self.triple(
                        bottom,
                        left_edge,
                        inner_left,
                        inner_right,
                        right_edge,
                        fill,
                        stroke,
                    )?;
                } else {
                    self.span(top, left_edge, right_edge, stroke)?;
                    self.span(bottom, left_edge, right_edge, stroke)?;
                }
            } else if bottom >= 0 && top <= max_y {
                let right_edge = clamp(cx + outer_x, 0, max_x);
                let left_edge = clamp(cx - outer_x, 0, max_x);
                if within_inner {
                    let inner_right = clamp(cx + inner_x, 0, max_x);
                    let inner_left = clamp(cx - inner_x, 0, max_x);
                    if top >= 0 {
                        self.triple(
                            top,
                            left_edge,
                            inner_left,
                            inner_right,
                            right_edge,
                            fill,
                            stroke,
                        )?;
                    }
                    if bottom <= max_y {
                        self.triple(
                            bottom,
                            left_edge,
                            inner_left,
                            inner_right,
                            right_edge,
                            fill,
                            stroke,
                        )?;
                    }
                } else {
                    if top >= 0 {
                        self.span(top, left_edge, right_edge, stroke)?;
                    }
                    if bottom <= max_y {
                        self.span(bottom, left_edge, right_edge, stroke)?;
                    }
                }
            }
        }
        Ok(())
    }

    fn circle(
        &mut self,
        cx: i32,
        cy: i32,
        outer_radius: i32,
        fill: i32,
        stroke: i32,
        thickness: i32,
    ) -> Result<(), Error> {
        let unclipped = cx - outer_radius >= 0
            && cx + outer_radius <= self.max_x
            && cy - outer_radius >= 0
            && outer_radius + cy <= self.max_y;
        let (max_x, max_y) = (self.max_x, self.max_y);
        let wanted = usize::try_from(outer_radius).unwrap_or(0);
        if self.inner_radius.len() < wanted {
            self.inner_radius = vec![0; wanted];
        }
        let mut x = 0;
        let inner_radius = (outer_radius - thickness).max(0);
        let mut outer_y = outer_radius;
        let mut outer_error = -outer_radius;
        let mut inner_y = inner_radius;
        let mut inner_error = -inner_radius;
        let mut outer_delta = -1;
        let mut inner_delta = -1;
        if unclipped {
            self.triple(
                cy,
                cx - outer_radius,
                cx - inner_radius,
                cx + inner_radius,
                cx + outer_radius,
                fill,
                stroke,
            )?;
        } else if cy >= 0 && cy <= max_y {
            let a = clamp(cx - outer_radius, 0, max_x);
            let b = clamp(cx + outer_radius, 0, max_x);
            let c = clamp(cx - inner_radius, 0, max_x);
            let d = clamp(cx + inner_radius, 0, max_x);
            self.triple(cy, a, c, d, b, fill, stroke)?;
        }
        while outer_y > x {
            outer_delta += 2;
            inner_delta += 2;
            outer_error += outer_delta;
            inner_error += inner_delta;
            if inner_error >= 0 && inner_y >= 1 {
                if unclipped {
                    self.inner_set(inner_y, x)?;
                    inner_y -= 1;
                    inner_error -= inner_y << 1;
                } else {
                    inner_y -= 1;
                    inner_error -= inner_y << 1;
                    self.inner_set(inner_y, x)?;
                }
            }
            x += 1;
            if outer_error >= 0 {
                outer_y -= 1;
                outer_error -= outer_y << 1;
                let below = cy - outer_y;
                let above = outer_y + cy;
                if unclipped {
                    if outer_y >= inner_radius {
                        self.span(above, cx - x, cx + x, stroke)?;
                        self.span(below, cx - x, cx + x, stroke)?;
                    } else {
                        let extent = self.inner_get(outer_y)?;
                        self.triple(
                            above,
                            cx - x,
                            cx - extent,
                            cx + extent,
                            cx + x,
                            fill,
                            stroke,
                        )?;
                        self.triple(
                            below,
                            cx - x,
                            cx - extent,
                            cx + extent,
                            cx + x,
                            fill,
                            stroke,
                        )?;
                    }
                } else if above >= 0 && below <= max_y {
                    if outer_y >= inner_radius {
                        let right = clamp(cx + x, 0, max_x);
                        let left = clamp(cx - x, 0, max_x);
                        if above <= max_y {
                            self.span(above, left, right, stroke)?;
                        }
                        if below >= 0 {
                            self.span(below, left, right, stroke)?;
                        }
                    } else {
                        let extent = self.inner_get(outer_y)?;
                        let right = clamp(cx + x, 0, max_x);
                        let left = clamp(cx - x, 0, max_x);
                        let inner_right = clamp(cx + extent, 0, max_x);
                        let inner_left = clamp(cx - extent, 0, max_x);
                        if above <= max_y {
                            self.triple(above, left, inner_left, inner_right, right, fill, stroke)?;
                        }
                        if below >= 0 {
                            self.triple(below, left, inner_left, inner_right, right, fill, stroke)?;
                        }
                    }
                }
            }
            let below = cy - x;
            let above = x + cy;
            if unclipped {
                let right = cx + outer_y;
                let left = cx - outer_y;
                if x < inner_radius {
                    let extent = if inner_y < x {
                        self.inner_get(x)?
                    } else {
                        inner_y
                    };
                    self.triple(above, left, cx - extent, cx + extent, right, fill, stroke)?;
                    self.triple(below, left, cx - extent, cx + extent, right, fill, stroke)?;
                } else {
                    self.span(above, left, right, stroke)?;
                    self.span(below, left, right, stroke)?;
                }
            } else if above >= 0 && below <= max_y {
                let right = cx + outer_y;
                let left = cx - outer_y;
                if right >= 0 && left <= max_x {
                    let right = clamp(right, 0, max_x);
                    let left = clamp(left, 0, max_x);
                    if x < inner_radius {
                        let extent = if inner_y < x {
                            self.inner_get(x)?
                        } else {
                            inner_y
                        };
                        let inner_right = clamp(cx + extent, 0, max_x);
                        let inner_left = clamp(cx - extent, 0, max_x);
                        if above <= max_y {
                            self.triple(above, left, inner_left, inner_right, right, fill, stroke)?;
                        }
                        if below >= 0 {
                            self.triple(below, left, inner_left, inner_right, right, fill, stroke)?;
                        }
                    } else {
                        if above <= max_y {
                            self.span(above, left, right, stroke)?;
                        }
                        if below >= 0 {
                            self.span(below, left, right, stroke)?;
                        }
                    }
                }
            }
        }
        Ok(())
    }

    pub(crate) fn draw(&mut self, shape: &Shape) -> Result<(), Error> {
        let width = self.rows.first().map_or(0, Vec::len) as i32;
        let height = self.rows.len() as i32;
        match shape {
            Shape::Line { from, to, stroke } => {
                let x1 = from[0].wrapping_mul(width) >> 12;
                let x2 = to[0].wrapping_mul(width) >> 12;
                let y1 = height.wrapping_mul(from[1]) >> 12;
                let y2 = height.wrapping_mul(to[1]) >> 12;
                self.line(x1, y1, x2, y2, *stroke)
            }
            Shape::Bezier { points, stroke } => {
                let mut scaled = [0; 8];
                for i in 0..4 {
                    scaled[i * 2] = points[i * 2].wrapping_mul(width) >> 12;
                    scaled[i * 2 + 1] = height.wrapping_mul(points[i * 2 + 1]) >> 12;
                }
                self.bezier(scaled, *stroke)
            }
            Shape::Rectangle {
                left,
                top,
                right,
                bottom,
                fill,
                stroke,
                width: thickness,
            } => {
                let l = left.wrapping_mul(width) >> 12;
                let r = right.wrapping_mul(width) >> 12;
                let t = height.wrapping_mul(*top) >> 12;
                let b = height.wrapping_mul(*bottom) >> 12;
                if *fill >= 0 {
                    if *stroke >= 0 {
                        self.fill_stroke_rectangle(l, r, t, b, *fill, *stroke, *thickness)
                    } else {
                        self.fill_rectangle(l, r, t, b, *fill)
                    }
                } else if *stroke >= 0 {
                    self.stroke_rectangle(l, r, t, b, *stroke, *thickness)
                } else {
                    Ok(())
                }
            }
            Shape::Ellipse {
                center,
                radius,
                fill,
                stroke,
                width: thickness,
            } => {
                let cx = center[0].wrapping_mul(width) >> 12;
                let rx = radius[0].wrapping_mul(width) >> 12;
                let cy = height.wrapping_mul(center[1]) >> 12;
                let ry = height.wrapping_mul(radius[1]) >> 12;
                if *fill >= 0 {
                    if *stroke >= 0 {
                        self.ellipse(cx, cy, rx, ry, *fill, *stroke, *thickness)
                    } else {
                        Err(Error::UnsupportedTextureOperation(29))
                    }
                } else {
                    Ok(())
                }
            }
        }
    }
}
