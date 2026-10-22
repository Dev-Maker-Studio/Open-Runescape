use crate::def::{Component, ComponentType, ContentType, DragType};

pub const DRAG_LAYER_PARENT: i32 = -1412584499;
pub const NO_REFERENCE: i32 = -1;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Clip {
    pub left: i32,
    pub top: i32,
    pub right: i32,
    pub bottom: i32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RectMode {
    Opaque,
    Blend,
}

#[derive(Debug, Clone, PartialEq)]
pub enum SpriteOp {
    Plain {
        x: i32,
        y: i32,
    },
    Scaled {
        x: i32,
        y: i32,
        width: i32,
        height: i32,
    },
    Colored {
        x: i32,
        y: i32,
        argb: i32,
    },
    ScaledColored {
        x: i32,
        y: i32,
        width: i32,
        height: i32,
        argb: i32,
    },
    Tiled {
        x: i32,
        y: i32,
        width: i32,
        height: i32,
    },
    Rotated {
        center_x: f32,
        center_y: f32,
        scale: i32,
        angle: i32,
    },
    RotatedColored {
        center_x: f32,
        center_y: f32,
        scale: i32,
        angle: i32,
        argb: i32,
    },
}

#[derive(Debug, Clone, PartialEq)]
pub enum DrawCmd {
    SetClip(Clip),
    ExpandClip {
        left: i32,
        top: i32,
        right: i32,
        bottom: i32,
    },
    Rect {
        x: i32,
        y: i32,
        width: i32,
        height: i32,
        argb: i32,
        mode: RectMode,
        filled: bool,
    },
    Text {
        id_hash: i32,
        text: String,
        x: i32,
        y: i32,
        width: i32,
        height: i32,
        argb: i32,
        shadow_argb: i32,
        horizontal: i32,
        vertical: i32,
        line_spacing: i32,
        max_lines: i32,
        font_id: i32,
        monospaced: bool,
    },
    Sprite {
        id_hash: i32,
        sprite_id: i32,
        item_id: i32,
        op: SpriteOp,
    },
    Model {
        id_hash: i32,
        model_x: i32,
        model_y: i32,
        focal_x: i32,
        focal_y: i32,
    },
    Line {
        x1: i32,
        y1: i32,
        x2: i32,
        y2: i32,
        color: i32,
        width: i32,
    },
    GameArea {
        x: i32,
        y: i32,
        width: i32,
        height: i32,
        variant_1403: bool,
    },
    Special {
        content_type: ContentType,
        x: i32,
        y: i32,
        width: i32,
        height: i32,
    },
    FadeOverlay {
        left: i32,
        top: i32,
        width: i32,
        height: i32,
    },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Drag {
    pub id_hash: i32,
    pub ignores_depth: bool,
    pub position: Option<(i32, i32)>,
}

pub trait DrawHost {
    fn is_hidden(&self, component: &Component) -> bool {
        component.hidden
    }

    fn assign_reference(&self, x: i32, y: i32, width: i32, height: i32) -> i32;

    fn prev_on_top(&self, reference: i32) -> bool;

    fn mark_on_top(&self, reference: i32);

    fn item_slots(&self, container_id_hash: i32) -> Option<&[Option<Component>]> {
        let _ = container_id_hash;
        None
    }

    fn sub_interface(&self, container_id_hash: i32) -> Option<&[Option<Component>]> {
        let _ = container_id_hash;
        None
    }

    fn sprite_size(&self, component: &Component) -> Option<(i32, i32)>;

    fn display_text(&self, component: &Component) -> String {
        component.text.clone()
    }

    fn debug_draw_all(&self) -> bool {
        false
    }

    fn clip_interfaces(&self) -> bool {
        false
    }

    fn drag(&self) -> Option<Drag> {
        None
    }
}

#[derive(Debug, Default)]
pub struct DrawOutput {
    pub cmds: Vec<DrawCmd>,
    pub deferred_drag: Option<(i32, i32)>,
}

#[allow(clippy::too_many_arguments)]
pub fn draw_components(
    host: &dyn DrawHost,
    components: &[Option<Component>],
    parent_id: i32,
    clip: Clip,
    offset_x: i32,
    offset_y: i32,
    parent_reference: i32,
    out: &mut DrawOutput,
) {
    out.cmds.push(DrawCmd::SetClip(clip));
    let drag = host.drag();
    for slot in components {
        let Some(c) = slot else { continue };
        let is_dragging = drag.is_some_and(|d| d.id_hash == c.id_hash);
        if !(c.parent_id == parent_id || (parent_id == DRAG_LAYER_PARENT && is_dragging)) {
            continue;
        }
        let reference = if parent_reference == NO_REFERENCE {
            host.assign_reference(
                c.x.wrapping_add(offset_x),
                c.y.wrapping_add(offset_y),
                c.width,
                c.height,
            )
        } else {
            parent_reference
        };
        if host.is_hidden(c) {
            continue;
        }
        let draw_x = c.x.wrapping_add(offset_x);
        let draw_y = c.y.wrapping_add(offset_y);
        let mut transparency = c.transparency;
        let (start_x, start_y);
        if is_dragging {
            let d = drag.unwrap();
            if parent_id != DRAG_LAYER_PARENT
                && (c.drag_type == Some(DragType::Type2)
                    || c.drag_type == Some(DragType::Type0)
                    || d.ignores_depth)
            {
                out.deferred_drag = Some((offset_x, offset_y));
                continue;
            }
            match d.position {
                Some((x, y)) => {
                    start_x = x;
                    start_y = y;
                }
                None => {
                    start_x = draw_x;
                    start_y = draw_y;
                }
            }
            if c.drag_type == Some(DragType::Type0) {
                transparency = 128;
            }
        } else {
            start_x = draw_x;
            start_y = draw_y;
        }
        let kind = c.kind;
        let (end_x, end_y, clip_start_x, clip_start_y);
        if kind == Some(ComponentType::Type2) {
            end_x = clip.right;
            end_y = clip.bottom;
            clip_start_x = clip.left;
            clip_start_y = clip.top;
        } else {
            let mut right = start_x.wrapping_add(c.width);
            let mut bottom = start_y.wrapping_add(c.height);
            if kind == Some(ComponentType::Line) {
                right = right.wrapping_add(1);
                bottom = bottom.wrapping_add(1);
            }
            end_x = right.min(clip.right);
            end_y = bottom.min(clip.bottom);
            clip_start_x = start_x.max(clip.left);
            clip_start_y = start_y.max(clip.top);
        }
        if !(clip_start_x < end_x && clip_start_y < end_y) {
            continue;
        }
        if let Some(content) = c.content_type {
            match content {
                ContentType::GameWindow | ContentType::Type1403 => {
                    out.cmds.push(DrawCmd::GameArea {
                        x: start_x,
                        y: start_y,
                        width: c.width,
                        height: c.height,
                        variant_1403: content == ContentType::Type1403,
                    });
                    out.cmds.push(DrawCmd::SetClip(clip));
                    host.mark_on_top(reference);
                    continue;
                }
                ContentType::Minimap
                | ContentType::ColorPicker
                | ContentType::Type1409
                | ContentType::Compass => {
                    out.cmds.push(DrawCmd::Special {
                        content_type: content,
                        x: start_x,
                        y: start_y,
                        width: c.width,
                        height: c.height,
                    });
                    out.cmds.push(DrawCmd::SetClip(clip));
                    continue;
                }
                ContentType::WorldMap | ContentType::WorldMapOverview => {
                    out.cmds.push(DrawCmd::Special {
                        content_type: content,
                        x: start_x,
                        y: start_y,
                        width: c.width,
                        height: c.height,
                    });
                    host.mark_on_top(reference);
                    out.cmds.push(DrawCmd::SetClip(clip));
                    continue;
                }
                ContentType::DebugInformation => {
                    out.cmds.push(DrawCmd::Special {
                        content_type: content,
                        x: start_x,
                        y: start_y,
                        width: c.width,
                        height: c.height,
                    });
                    host.mark_on_top(reference);
                    continue;
                }
                _ => {}
            }
        }
        if kind == Some(ComponentType::Container) {
            let inner = Clip {
                left: clip_start_x,
                top: clip_start_y,
                right: end_x,
                bottom: end_y,
            };
            let inner_x = start_x.wrapping_sub(c.scroll_x);
            let inner_y = start_y.wrapping_sub(c.scroll_y);
            draw_components(host, components, c.id_hash, inner, inner_x, inner_y, reference, out);
            if let Some(slots) = host.item_slots(c.id_hash) {
                draw_components(host, slots, c.id_hash, inner, inner_x, inner_y, reference, out);
            }
            if let Some(sub) = host.sub_interface(c.id_hash) {
                draw_components(host, sub, -1, inner, start_x, start_y, reference, out);
            }
            if c.content_type == Some(ContentType::CutsceneFade) {
                out.cmds.push(DrawCmd::FadeOverlay {
                    left: clip_start_x,
                    top: clip_start_y,
                    width: end_x - clip_start_x,
                    height: end_y - clip_start_y,
                });
            }
            out.cmds.push(DrawCmd::SetClip(clip));
        }
        if !(host.prev_on_top(reference) || host.debug_draw_all()) {
            continue;
        }
        let blend = 255i32.wrapping_sub(transparency & 0xff).wrapping_shl(24);
        match kind {
            Some(ComponentType::Figure) => {
                let (argb, mode) = if transparency == 0 {
                    (c.color, RectMode::Opaque)
                } else {
                    (blend | (c.color & 0xffffff), RectMode::Blend)
                };
                out.cmds.push(DrawCmd::Rect {
                    x: start_x,
                    y: start_y,
                    width: c.width,
                    height: c.height,
                    argb,
                    mode,
                    filled: c.filled,
                });
            }
            Some(ComponentType::Text) => {
                let text = host.display_text(c);
                if host.clip_interfaces() {
                    out.cmds.push(DrawCmd::ExpandClip {
                        left: start_x,
                        top: start_y,
                        right: start_x.wrapping_add(c.width),
                        bottom: start_y.wrapping_add(c.height),
                    });
                }
                out.cmds.push(DrawCmd::Text {
                    id_hash: c.id_hash,
                    text,
                    x: start_x,
                    y: start_y,
                    width: c.width,
                    height: c.height,
                    argb: blend | c.color,
                    shadow_argb: if c.shadow { blend } else { -1 },
                    horizontal: c.text_horizontal_ali,
                    vertical: c.text_vertical_ali,
                    line_spacing: c.line_spacing,
                    max_lines: c.max_text_lines,
                    font_id: c.font_id,
                    monospaced: c.monospaced,
                });
                if host.clip_interfaces() {
                    out.cmds.push(DrawCmd::SetClip(clip));
                }
            }
            Some(ComponentType::Sprite) => {
                draw_sprite(host, c, start_x, start_y, transparency, clip, out);
            }
            Some(ComponentType::Model) => {
                let focal_x = if c.aspect_width > 0 {
                    c.width.wrapping_shl(9).wrapping_div(c.aspect_width)
                } else {
                    512
                };
                let focal_y = if c.aspect_height > 0 {
                    c.height.wrapping_shl(9).wrapping_div(c.aspect_height)
                } else {
                    512
                };
                let mut model_x = (c.width / 2).wrapping_add(start_x);
                let mut model_y = (c.height / 2).wrapping_add(start_y);
                if !c.has_origin {
                    model_x = model_x.wrapping_add(focal_x.wrapping_mul(c.origin_x) >> 9);
                    model_y = model_y.wrapping_add(focal_y.wrapping_mul(c.origin_y) >> 9);
                }
                out.cmds.push(DrawCmd::Model {
                    id_hash: c.id_hash,
                    model_x,
                    model_y,
                    focal_x,
                    focal_y,
                });
            }
            Some(ComponentType::Line) => {
                let (x1, y1, x2, y2) = if c.line_direction {
                    (
                        start_x,
                        start_y.wrapping_add(c.height),
                        start_x.wrapping_add(c.width),
                        start_y,
                    )
                } else {
                    (
                        start_x,
                        start_y,
                        start_x.wrapping_add(c.width),
                        start_y.wrapping_add(c.height),
                    )
                };
                out.cmds.push(DrawCmd::Line {
                    x1,
                    y1,
                    x2,
                    y2,
                    color: c.color,
                    width: c.line_width,
                });
            }
            _ => {}
        }
    }
}

fn draw_sprite(
    host: &dyn DrawHost,
    c: &Component,
    start_x: i32,
    start_y: i32,
    transparency: i32,
    clip: Clip,
    out: &mut DrawOutput,
) {
    let Some((sprite_w, sprite_h)) = host.sprite_size(c) else {
        return;
    };
    if sprite_w <= 0 || sprite_h <= 0 {
        return;
    }
    let tint = if c.color != 0 { c.color & 0xffffff } else { 0xffffff };
    let argb = 255i32
        .wrapping_sub(transparency & 0xff)
        .wrapping_shl(24)
        | tint;
    let plain = c.color == 0 && transparency == 0;
    fn push(out: &mut DrawOutput, c: &Component, op: SpriteOp) {
        out.cmds.push(DrawCmd::Sprite {
            id_hash: c.id_hash,
            sprite_id: c.sprite_id,
            item_id: c.container_item_id,
            op,
        });
    }
    if c.tiling {
        out.cmds.push(DrawCmd::ExpandClip {
            left: start_x,
            top: start_y,
            right: start_x.wrapping_add(c.width),
            bottom: start_y.wrapping_add(c.height),
        });
        if c.angle_2d != 0 {
            let cols = (sprite_w - 1 + c.width) / sprite_w;
            let rows = (sprite_h - 1 + c.height) / sprite_h;
            for col in 0..cols {
                for row in 0..rows {
                    let center_x = (start_x + col * sprite_w) as f32 + sprite_w as f32 / 2.0;
                    let center_y = sprite_h as f32 / 2.0 + (start_y + sprite_h * row) as f32;
                    if c.color != 0 {
                        push(out, c, SpriteOp::RotatedColored {
                            center_x,
                            center_y,
                            scale: 4096,
                            angle: c.angle_2d,
                            argb,
                        });
                    } else {
                        push(out, c, SpriteOp::Rotated {
                            center_x,
                            center_y,
                            scale: 4096,
                            angle: c.angle_2d,
                        });
                    }
                }
            }
        } else if plain {
            push(out, c, SpriteOp::Tiled {
                x: start_x,
                y: start_y,
                width: c.width,
                height: c.height,
            });
        } else {
            push(out, c, SpriteOp::ScaledColored {
                x: start_x,
                y: start_y,
                width: c.width,
                height: c.height,
                argb,
            });
        }
        out.cmds.push(DrawCmd::SetClip(clip));
        return;
    }
    let center_x = c.width as f32 / 2.0 + start_x as f32;
    let center_y = start_y as f32 + c.height as f32 / 2.0;
    let scale = c.width.wrapping_mul(4096) / sprite_w;
    let same_size = sprite_w == c.width && sprite_h == c.height;
    if plain {
        if c.angle_2d != 0 {
            push(out, c, SpriteOp::Rotated {
                center_x,
                center_y,
                scale,
                angle: c.angle_2d,
            });
        } else if same_size {
            push(out, c, SpriteOp::Plain {
                x: start_x,
                y: start_y,
            });
        } else {
            push(out, c, SpriteOp::Scaled {
                x: start_x,
                y: start_y,
                width: c.width,
                height: c.height,
            });
        }
    } else if c.angle_2d != 0 {
        push(out, c, SpriteOp::RotatedColored {
            center_x,
            center_y,
            scale,
            angle: c.angle_2d,
            argb,
        });
    } else if same_size {
        push(out, c, SpriteOp::Colored {
            x: start_x,
            y: start_y,
            argb,
        });
    } else {
        push(out, c, SpriteOp::ScaledColored {
            x: start_x,
            y: start_y,
            width: c.width,
            height: c.height,
            argb,
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::cell::{Cell, RefCell};

    struct Host {
        next: Cell<i32>,
        on_top: RefCell<Vec<bool>>,
        sprite: Option<(i32, i32)>,
        drag: Option<Drag>,
        sub: Vec<Option<Component>>,
    }

    impl Host {
        fn new() -> Self {
            Self {
                next: Cell::new(0),
                on_top: RefCell::new(vec![true; 8]),
                sprite: Some((10, 10)),
                drag: None,
                sub: Vec::new(),
            }
        }
    }

    impl DrawHost for Host {
        fn assign_reference(&self, _: i32, _: i32, _: i32, _: i32) -> i32 {
            let n = self.next.get();
            self.next.set(n + 1);
            n
        }
        fn prev_on_top(&self, reference: i32) -> bool {
            self.on_top.borrow()[reference as usize]
        }
        fn mark_on_top(&self, reference: i32) {
            self.on_top.borrow_mut()[reference as usize] = true;
        }
        fn sprite_size(&self, _: &Component) -> Option<(i32, i32)> {
            self.sprite
        }
        fn sub_interface(&self, id_hash: i32) -> Option<&[Option<Component>]> {
            (id_hash == 7 && !self.sub.is_empty()).then_some(self.sub.as_slice())
        }
        fn drag(&self) -> Option<Drag> {
            self.drag
        }
    }

    fn comp(id: i32, kind: ComponentType, x: i32, y: i32, w: i32, h: i32) -> Component {
        let mut c = Component::blank(id);
        c.kind = Some(kind);
        c.x = x;
        c.y = y;
        c.width = w;
        c.height = h;
        c
    }

    fn screen() -> Clip {
        Clip {
            left: 0,
            top: 0,
            right: 100,
            bottom: 100,
        }
    }

    fn run(host: &Host, comps: &[Option<Component>]) -> DrawOutput {
        let mut out = DrawOutput::default();
        draw_components(host, comps, -1, screen(), 0, 0, NO_REFERENCE, &mut out);
        out
    }

    #[test]
    fn opaque_figure_uses_raw_color_and_opaque_mode() {
        let mut c = comp(1, ComponentType::Figure, 5, 6, 20, 30);
        c.color = 0x123456;
        c.filled = true;
        let out = run(&Host::new(), &[Some(c)]);
        assert_eq!(
            out.cmds[1],
            DrawCmd::Rect {
                x: 5,
                y: 6,
                width: 20,
                height: 30,
                argb: 0x123456,
                mode: RectMode::Opaque,
                filled: true
            }
        );
    }

    #[test]
    fn translucent_figure_inverts_transparency_into_alpha() {
        let mut c = comp(1, ComponentType::Figure, 0, 0, 10, 10);
        c.color = 0xff00ff;
        c.transparency = 0x40;
        let out = run(&Host::new(), &[Some(c)]);
        let DrawCmd::Rect { argb, mode, filled, .. } = out.cmds[1] else {
            panic!()
        };
        assert_eq!(argb as u32, 0xbfff00ff);
        assert_eq!(mode, RectMode::Blend);
        assert!(!filled);
    }

    #[test]
    fn hidden_and_offscreen_components_emit_nothing() {
        let mut hidden = comp(1, ComponentType::Figure, 0, 0, 10, 10);
        hidden.hidden = true;
        let off = comp(2, ComponentType::Figure, 100, 0, 10, 10);
        let out = run(&Host::new(), &[Some(hidden), Some(off)]);
        assert_eq!(out.cmds.len(), 1);
    }

    #[test]
    fn container_children_are_offset_by_position_minus_scroll_and_clipped() {
        let mut parent = comp(7, ComponentType::Container, 10, 10, 50, 50);
        parent.scroll_x = 3;
        parent.scroll_y = 4;
        let mut child = comp(8, ComponentType::Figure, 20, 20, 100, 100);
        child.parent_id = 7;
        let out = run(&Host::new(), &[Some(parent), Some(child)]);
        assert!(out.cmds.contains(&DrawCmd::SetClip(Clip {
            left: 10,
            top: 10,
            right: 60,
            bottom: 60
        })));
        let rect = out
            .cmds
            .iter()
            .find_map(|c| match c {
                DrawCmd::Rect { x, y, .. } => Some((*x, *y)),
                _ => None,
            })
            .unwrap();
        assert_eq!(rect, (27, 26));
        assert_eq!(out.cmds.last(), Some(&DrawCmd::SetClip(screen())));
    }

    #[test]
    fn sub_interface_draws_at_container_origin_with_parent_minus_one() {
        let parent = comp(7, ComponentType::Container, 10, 10, 50, 50);
        let mut host = Host::new();
        host.sub = vec![Some(comp(9, ComponentType::Figure, 2, 3, 5, 5))];
        let out = run(&host, &[Some(parent)]);
        assert!(out.cmds.iter().any(|c| matches!(c, DrawCmd::Rect { x: 12, y: 13, .. })));
    }

    #[test]
    fn nothing_but_containers_draws_when_not_on_top() {
        let host = Host::new();
        host.on_top.borrow_mut()[0] = false;
        let c = comp(1, ComponentType::Figure, 0, 0, 10, 10);
        let out = run(&host, &[Some(c)]);
        assert_eq!(out.cmds.len(), 1);
    }

    #[test]
    fn line_extends_one_pixel_and_flips_diagonal() {
        let mut c = comp(1, ComponentType::Line, 10, 10, 20, 30);
        c.line_direction = true;
        c.line_width = 1;
        c.color = 5;
        let out = run(&Host::new(), &[Some(c)]);
        assert_eq!(
            out.cmds[1],
            DrawCmd::Line {
                x1: 10,
                y1: 40,
                x2: 30,
                y2: 10,
                color: 5,
                width: 1
            }
        );
    }

    #[test]
    fn plain_sprite_of_equal_size_draws_unscaled() {
        let mut c = comp(1, ComponentType::Sprite, 4, 5, 10, 10);
        c.sprite_id = 3;
        let out = run(&Host::new(), &[Some(c)]);
        assert!(matches!(
            out.cmds[1],
            DrawCmd::Sprite { op: SpriteOp::Plain { x: 4, y: 5 }, .. }
        ));
    }

    #[test]
    fn plain_sprite_of_other_size_is_scaled_and_tinted_one_is_colored() {
        let mut c = comp(1, ComponentType::Sprite, 0, 0, 20, 20);
        let out = run(&Host::new(), &[Some(c.clone())]);
        assert!(matches!(
            out.cmds[1],
            DrawCmd::Sprite { op: SpriteOp::Scaled { width: 20, height: 20, .. }, .. }
        ));
        c.color = 0x00ff00;
        c.transparency = 0x10;
        let out = run(&Host::new(), &[Some(c)]);
        let DrawCmd::Sprite { op: SpriteOp::ScaledColored { argb, .. }, .. } = out.cmds[1] else {
            panic!()
        };
        assert_eq!(argb as u32, 0xef00ff00);
    }

    #[test]
    fn tiled_rotated_sprite_covers_a_grid() {
        let mut c = comp(1, ComponentType::Sprite, 0, 0, 25, 10);
        c.tiling = true;
        c.angle_2d = 100;
        let out = run(&Host::new(), &[Some(c)]);
        let n = out
            .cmds
            .iter()
            .filter(|c| matches!(c, DrawCmd::Sprite { op: SpriteOp::Rotated { scale: 4096, .. }, .. }))
            .count();
        assert_eq!(n, 3);
        assert!(matches!(out.cmds[1], DrawCmd::ExpandClip { right: 25, bottom: 10, .. }));
    }

    #[test]
    fn plain_tiling_uses_tiled_draw() {
        let mut c = comp(1, ComponentType::Sprite, 0, 0, 25, 10);
        c.tiling = true;
        let out = run(&Host::new(), &[Some(c)]);
        assert!(matches!(
            out.cmds[2],
            DrawCmd::Sprite { op: SpriteOp::Tiled { width: 25, height: 10, .. }, .. }
        ));
    }

    #[test]
    fn missing_sprite_draws_nothing() {
        let mut host = Host::new();
        host.sprite = None;
        let c = comp(1, ComponentType::Sprite, 0, 0, 10, 10);
        assert_eq!(run(&host, &[Some(c)]).cmds.len(), 1);
    }

    #[test]
    fn text_carries_alpha_shadow_and_alignment() {
        let mut c = comp(1, ComponentType::Text, 1, 2, 30, 12);
        c.text = "hi".into();
        c.color = 0xffff00;
        c.shadow = true;
        c.text_horizontal_ali = 1;
        c.transparency = 0x80;
        let out = run(&Host::new(), &[Some(c)]);
        let DrawCmd::Text { text, argb, shadow_argb, horizontal, .. } = &out.cmds[1] else {
            panic!()
        };
        assert_eq!(text, "hi");
        assert_eq!(*argb as u32, 0x7fffff00);
        assert_eq!(*shadow_argb as u32, 0x7f000000);
        assert_eq!(*horizontal, 1);
    }

    #[test]
    fn game_window_marks_on_top_and_skips_the_rest() {
        let mut c = comp(1, ComponentType::Container, 0, 0, 50, 50);
        c.content_type = Some(ContentType::GameWindow);
        let host = Host::new();
        host.on_top.borrow_mut()[0] = false;
        let out = run(&host, &[Some(c)]);
        assert!(matches!(out.cmds[1], DrawCmd::GameArea { width: 50, .. }));
        assert!(host.on_top.borrow()[0]);
    }

    #[test]
    fn model_focal_lengths_follow_aspect_and_origin() {
        let mut c = comp(1, ComponentType::Model, 10, 10, 100, 50);
        c.aspect_width = 200;
        c.aspect_height = 0;
        c.origin_x = 512;
        c.origin_y = 100;
        let host = Host::new();
        host.on_top.borrow_mut()[0] = true;
        let out = run(&host, &[Some(c)]);
        assert_eq!(
            out.cmds[1],
            DrawCmd::Model {
                id_hash: 1,
                model_x: 60 + 256,
                model_y: 35 + 100,
                focal_x: 256,
                focal_y: 512
            }
        );
    }

    #[test]
    fn dragged_component_is_deferred_unless_in_the_drag_layer() {
        let mut c = comp(1, ComponentType::Figure, 0, 0, 10, 10);
        c.drag_type = Some(DragType::Type2);
        let mut host = Host::new();
        host.drag = Some(Drag {
            id_hash: 1,
            ignores_depth: false,
            position: Some((40, 41)),
        });
        let comps = [Some(c)];
        let out = run(&host, &comps);
        assert_eq!(out.deferred_drag, Some((0, 0)));
        assert_eq!(out.cmds.len(), 1);
        let mut layer = DrawOutput::default();
        draw_components(&host, &comps, DRAG_LAYER_PARENT, screen(), 0, 0, NO_REFERENCE, &mut layer);
        assert!(layer.cmds.iter().any(|c| matches!(c, DrawCmd::Rect { x: 40, y: 41, .. })));
    }

    #[test]
    fn drag_type_zero_forces_half_transparency_in_the_drag_layer() {
        let mut c = comp(1, ComponentType::Figure, 0, 0, 10, 10);
        c.drag_type = Some(DragType::Type0);
        c.color = 0xff;
        let mut host = Host::new();
        host.drag = Some(Drag {
            id_hash: 1,
            ignores_depth: false,
            position: None,
        });
        let mut out = DrawOutput::default();
        draw_components(&host, &[Some(c)], DRAG_LAYER_PARENT, screen(), 0, 0, NO_REFERENCE, &mut out);
        let DrawCmd::Rect { argb, mode, .. } = out.cmds[1] else {
            panic!()
        };
        assert_eq!(argb as u32, 0x7f0000ff);
        assert_eq!(mode, RectMode::Blend);
    }
}
