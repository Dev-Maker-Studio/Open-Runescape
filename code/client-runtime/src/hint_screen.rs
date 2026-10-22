use crate::model_hover::ProjectionViewport;

pub const OVERHEAD_COLORS: [i32; 6] = [16_776_960, 16_711_680, 65_280, 65_535, 16_711_935, 16_777_215];

pub fn world_to_screen(
    mvp: &[f32; 16],
    viewport: &ProjectionViewport,
    x: f32,
    y: f32,
    z: f32,
) -> Option<(f32, f32)> {
    let clip_z = mvp[14] + mvp[2] * x + mvp[6] * y + mvp[10] * z;
    let clip_w = mvp[15] + mvp[3] * x + mvp[7] * y + mvp[11] * z;
    if !(clip_z >= -clip_w && clip_z <= clip_w) {
        return None;
    }
    let clip_x = mvp[12] + mvp[0] * x + mvp[4] * y + mvp[8] * z;
    if !(clip_x >= -clip_w && clip_x <= clip_w) {
        return None;
    }
    let clip_y = mvp[13] + mvp[1] * x + mvp[5] * y + mvp[9] * z;
    if !(clip_y >= -clip_w && clip_y <= clip_w) {
        return None;
    }
    Some((
        viewport.origin_x + viewport.center_x * clip_x / clip_w,
        viewport.origin_y + viewport.center_y * clip_y / clip_w,
    ))
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct HintOffset {
    pub x: f32,
    pub y: f32,
}

pub fn hint_arrow_placement(
    mvp: &[f32; 16],
    viewport: &ProjectionViewport,
    scene_size: (i32, i32),
    position: (i32, i32, i32),
    offset: HintOffset,
) -> Option<(f32, f32)> {
    let (abs_x, ground_minus_floor, abs_y) = position;
    if abs_x < 512
        || abs_y < 512
        || abs_x > (scene_size.0 - 2) * 512
        || abs_y > (scene_size.1 - 2) * 512
    {
        return None;
    }
    let (x, y) = world_to_screen(
        mvp,
        viewport,
        abs_x as f32,
        ground_minus_floor as f32,
        abs_y as f32,
    )?;
    Some((x - offset.x, y - offset.y))
}

pub fn tile_arrow_icon(position: (f32, f32), cycles: u64) -> Option<(i32, i32)> {
    (cycles % 20 < 10).then_some(((position.0 - 12.) as i32, (position.1 - 28.) as i32))
}

pub fn overhead_text_color(
    color_type: i32,
    cycles: u64,
    ticks_left: i32,
    ticks_visible: i32,
) -> i32 {
    let mut color = 16_776_960;
    let phase = cycles % 20 < 10;
    let progress = || {
        if ticks_visible == 0 {
            0
        } else {
            150 - ticks_left.wrapping_mul(150) / ticks_visible
        }
    };
    if (0..6).contains(&color_type) {
        color = OVERHEAD_COLORS[color_type as usize];
    }
    match color_type {
        6 => color = if phase { 16_711_680 } else { 16_776_960 },
        7 => color = if phase { 255 } else { 65_535 },
        8 => color = if phase { 45_056 } else { 8_454_016 },
        9 => {
            let p = progress();
            if p < 50 {
                color = p * 1280 + 16_711_680;
            } else if p < 100 {
                color = 16_776_960 - (p - 50) * 327_680;
            } else if p < 150 {
                color = (p - 100) * 5 + 65_280;
            }
        }
        10 => {
            let p = progress();
            if p < 50 {
                color = p * 5 + 16_711_680;
            } else if p < 100 {
                color = 16_711_935 - (p - 50) * 327_680;
            } else if p < 150 {
                color = (p - 100) * 327_680 + 255 - (p - 100) * 5;
            }
        }
        11 => {
            let p = progress();
            if p < 50 {
                color = 16_777_215 - p * 327_685;
            } else if p < 100 {
                color = (p - 50) * 327_685 + 65_280;
            } else if p < 150 {
                color = 16_777_215 - (p - 100) * 327_680;
            }
        }
        _ => {}
    }
    color
}

pub fn effect_progress(ticks_left: i32, ticks_visible: i32) -> i32 {
    if ticks_visible == 0 {
        0
    } else {
        150 - ticks_left.wrapping_mul(150) / ticks_visible
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct LabelBox {
    pub x: i32,
    pub y: i32,
    pub half_width: i32,
}

pub fn stack_labels(labels: &[LabelBox], padding: i32) -> Vec<i32> {
    let mut placed: Vec<LabelBox> = Vec::with_capacity(labels.len());
    for label in labels {
        let mut y = label.y;
        let mut again = true;
        while again {
            again = false;
            for other in &placed {
                if y + 2 > other.y - padding
                    && y - padding < other.y + 2
                    && label.x - label.half_width < other.half_width + other.x
                    && label.x + label.half_width > other.x - other.half_width
                    && other.y - padding < y
                {
                    y = other.y - padding;
                    again = true;
                }
            }
        }
        placed.push(LabelBox { y, ..*label });
    }
    placed.into_iter().map(|label| label.y).collect()
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MinimapHint {
    Marker { x: i32, y: i32 },
    Edge { x: f32, y: f32, rotation: i32 },
}

pub struct MinimapGeometry {
    pub component_width: i32,
    pub component_height: i32,
    pub sprite_width: i32,
    pub sprite_height: i32,
    pub locked_camera: bool,
    pub minimap_zoom: i32,
    pub sine: i32,
    pub cosine: i32,
}

pub fn minimap_hint(
    geometry: &MinimapGeometry,
    x: i32,
    y: i32,
    max_distance: i64,
    within_bounds: &dyn Fn(i32, i32) -> bool,
) -> Option<MinimapHint> {
    let distance_squared = i64::from(y) * i64::from(y) + i64::from(x) * i64::from(x);
    if distance_squared > max_distance {
        return None;
    }
    let mut sine = geometry.sine;
    let mut cosine = geometry.cosine;
    if !geometry.locked_camera {
        sine = sine.wrapping_mul(256) / (geometry.minimap_zoom + 256);
        cosine = cosine.wrapping_mul(256) / (geometry.minimap_zoom + 256);
    }
    let dx = cosine.wrapping_mul(x).wrapping_add(y.wrapping_mul(sine)) >> 14;
    let dy = y.wrapping_mul(cosine).wrapping_sub(sine.wrapping_mul(x)) >> 14;
    let x1 = dx + geometry.component_width / 2 - geometry.sprite_width / 2;
    let x2 = geometry.sprite_width + x1;
    let y1 = -dy + geometry.component_height / 2 - geometry.sprite_height;
    let y2 = geometry.sprite_height + y1;
    if within_bounds(x1, y1) && within_bounds(x2, y1) && within_bounds(x1, y2) && within_bounds(x2, y2)
    {
        return Some(MinimapHint::Marker { x: x1, y: y1 });
    }
    let radians = f64::from(dx).atan2(f64::from(dy));
    let reach = (geometry.component_width / 2).min(geometry.component_height / 2) - 6;
    let edge_x = (radians.sin() * f64::from(reach)) as i32;
    let edge_y = (radians.cos() * f64::from(reach)) as i32;
    Some(MinimapHint::Edge {
        x: geometry.component_width as f32 / 2. + edge_x as f32,
        y: geometry.component_height as f32 / 2. - edge_y as f32,
        rotation: (65535.0 * (-radians / std::f64::consts::TAU)) as i32,
    })
}

#[cfg(target_arch = "wasm32")]
mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = minimapHint)]
    pub fn minimap_hint_of(
        geometry: &[i32],
        x: i32,
        y: i32,
        max_distance: f64,
        row_start: &[i32],
        row_width: &[i32],
    ) -> Vec<f64> {
        let Ok(g): Result<&[i32; 8], _> = geometry.try_into() else {
            return Vec::new();
        };
        let locked = g[4] != 0;
        let angle = if locked {
            (g[6] & 0x3fff) as usize
        } else {
            (g[6].wrapping_add(g[7]) & 0x3fff) as usize
        };
        let geometry = MinimapGeometry {
            component_width: g[0],
            component_height: g[1],
            sprite_width: g[2],
            sprite_height: g[3],
            locked_camera: locked,
            minimap_zoom: g[5],
            sine: openrs_entity::trig::sine()[angle],
            cosine: openrs_entity::trig::cosine()[angle],
        };
        let within = |px: i32, py: i32| {
            usize::try_from(py)
                .ok()
                .and_then(|row| Some((*row_start.get(row)?, *row_width.get(row)?)))
                .is_some_and(|(start, width)| px >= start && px <= start + width)
        };
        match minimap_hint(&geometry, x, y, max_distance as i64, &within) {
            None => Vec::new(),
            Some(MinimapHint::Marker { x, y }) => vec![0., f64::from(x), f64::from(y), 0.],
            Some(MinimapHint::Edge { x, y, rotation }) => {
                vec![1., f64::from(x), f64::from(y), f64::from(rotation)]
            }
        }
    }
}

#[cfg(test)]
mod tests;
