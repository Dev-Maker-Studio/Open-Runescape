use crate::canvas::{Bitmap, Canvas};
use crate::font::{Font, TextStyle};
use openrs_interface::draw::{DrawCmd, RectMode, SpriteOp};
use openrs_interface::def::ContentType;

pub trait Assets {
    fn sprite(&self, id: i32) -> Option<&Bitmap>;
    fn font(&self, id: i32) -> Option<&Font>;
    fn icons(&self) -> &[Bitmap] {
        &[]
    }
}

#[derive(Debug, Default, Clone, PartialEq)]
pub struct PaintReport {
    pub game_area: Option<(i32, i32, i32, i32, bool)>,
    pub specials: Vec<(ContentType, i32, i32, i32, i32)>,
    pub missing_sprites: Vec<i32>,
    pub missing_fonts: Vec<i32>,
    pub skipped_models: usize,
    pub skipped_items: usize,
}

pub struct Painter;

fn rgb(color: i32) -> u32 {
    0xff00_0000 | (color as u32 & 0xff_ffff)
}

fn note(list: &mut Vec<i32>, id: i32) {
    if !list.contains(&id) {
        list.push(id);
    }
}

impl Painter {
    pub fn paint(canvas: &mut Canvas, cmds: &[DrawCmd], assets: &dyn Assets) -> PaintReport {
        let mut report = PaintReport::default();
        for cmd in cmds {
            match cmd {
                DrawCmd::SetClip(c) => canvas.set_clip(c.left, c.top, c.right, c.bottom),
                DrawCmd::ExpandClip { left, top, right, bottom } => {
                    canvas.expand_clip(*left, *top, *right, *bottom)
                }
                DrawCmd::Rect { x, y, width, height, argb, mode, filled } => {
                    let color = match mode {
                        RectMode::Opaque => rgb(*argb),
                        RectMode::Blend => *argb as u32,
                    };
                    if *filled {
                        canvas.fill_rect(*x, *y, *width, *height, color);
                    } else {
                        canvas.outline_rect(*x, *y, *width, *height, color);
                    }
                }
                DrawCmd::Line { x1, y1, x2, y2, color, width } => {
                    canvas.thick_line(*x1, *y1, *x2, *y2, *width, rgb(*color));
                }
                DrawCmd::Text {
                    text, x, y, width, height, argb, shadow_argb, horizontal, vertical, line_spacing,
                    max_lines, font_id, monospaced, ..
                } => match assets.font(*font_id) {
                    Some(font) => {
                        let style = TextStyle {
                            argb: *argb as u32,
                            shadow_argb: *shadow_argb,
                            horizontal: *horizontal,
                            vertical: *vertical,
                            line_spacing: *line_spacing,
                            max_lines: *max_lines,
                            plain: *monospaced,
                        };
                        font.render_text(canvas, text, *x, *y, *width, *height, &style, assets.icons());
                    }
                    None => note(&mut report.missing_fonts, *font_id),
                },
                DrawCmd::Sprite { sprite_id, item_id, op, .. } => {
                    if *item_id != -1 {
                        report.skipped_items += 1;
                        continue;
                    }
                    let Some(bitmap) = assets.sprite(*sprite_id) else {
                        note(&mut report.missing_sprites, *sprite_id);
                        continue;
                    };
                    match op {
                        SpriteOp::Plain { x, y } => canvas.draw_bitmap(bitmap, *x, *y, None),
                        SpriteOp::Scaled { x, y, width, height } => {
                            canvas.draw_scaled(bitmap, *x, *y, *width, *height, None)
                        }
                        SpriteOp::Colored { x, y, argb } => {
                            canvas.draw_bitmap(bitmap, *x, *y, Some(*argb as u32))
                        }
                        SpriteOp::ScaledColored { x, y, width, height, argb } => {
                            canvas.draw_scaled(bitmap, *x, *y, *width, *height, Some(*argb as u32))
                        }
                        SpriteOp::Tiled { x, y, width, height } => {
                            canvas.draw_tiled(bitmap, *x, *y, *width, *height, None)
                        }
                        SpriteOp::Rotated { center_x, center_y, scale, angle } => {
                            canvas.draw_rotated(bitmap, *center_x, *center_y, *scale, *angle, None)
                        }
                        SpriteOp::RotatedColored { center_x, center_y, scale, angle, argb } => {
                            canvas.draw_rotated(bitmap, *center_x, *center_y, *scale, *angle, Some(*argb as u32))
                        }
                    }
                }
                DrawCmd::Model { .. } => report.skipped_models += 1,
                DrawCmd::GameArea { x, y, width, height, variant_1403 } => {
                    canvas.clear_rect(*x, *y, *width, *height);
                    report.game_area = Some((*x, *y, *width, *height, *variant_1403));
                }
                DrawCmd::Special { content_type, x, y, width, height } => {
                    report.specials.push((*content_type, *x, *y, *width, *height));
                }
                DrawCmd::FadeOverlay { .. } => {}
            }
        }
        report
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_interface::draw::Clip;

    struct Sprites(Vec<Bitmap>);

    impl Assets for Sprites {
        fn sprite(&self, id: i32) -> Option<&Bitmap> {
            self.0.get(usize::try_from(id).ok()?)
        }
        fn font(&self, _: i32) -> Option<&Font> {
            None
        }
    }

    #[test]
    fn paints_rects_sprites_and_reports_missing_assets() {
        let mut canvas = Canvas::new(8, 8);
        let assets = Sprites(vec![Bitmap { width: 2, height: 2, pixels: vec![0xff0000ff; 4] }]);
        let cmds = vec![
            DrawCmd::SetClip(Clip { left: 0, top: 0, right: 8, bottom: 8 }),
            DrawCmd::Rect { x: 0, y: 0, width: 8, height: 8, argb: 0x123456, mode: RectMode::Opaque, filled: true },
            DrawCmd::Sprite { id_hash: 1, sprite_id: 0, item_id: -1, op: SpriteOp::Plain { x: 3, y: 3 } },
            DrawCmd::Sprite { id_hash: 1, sprite_id: 9, item_id: -1, op: SpriteOp::Plain { x: 0, y: 0 } },
            DrawCmd::Text {
                id_hash: 1, text: "a".into(), x: 0, y: 0, width: 4, height: 4, argb: -1, shadow_argb: -1,
                horizontal: 0, vertical: 0, line_spacing: 0, max_lines: 0, font_id: 5, monospaced: true,
            },
            DrawCmd::GameArea { x: 6, y: 6, width: 2, height: 2, variant_1403: false },
        ];
        let report = Painter::paint(&mut canvas, &cmds, &assets);
        assert_eq!(canvas.pixel(0, 0), 0xff123456);
        assert_eq!(canvas.pixel(3, 3), 0xff0000ff);
        assert_eq!(canvas.pixel(7, 7), 0);
        assert_eq!(report.missing_sprites, vec![9]);
        assert_eq!(report.missing_fonts, vec![5]);
        assert_eq!(report.game_area, Some((6, 6, 2, 2, false)));
    }
}
