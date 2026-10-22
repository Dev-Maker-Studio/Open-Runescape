use games::{buttons::Buttons, game::{DT, Game}, walker::Walker};
use renderer::stream::{self, Input};

fn input(kind: u8, button: u8, a: f32, b: f32, code: &str) -> Input {
    Input { seq: 0, kind, button, a, b, code: code.into() }
}

#[test]
fn walker_moves_with_keys_and_clicks() {
    let mut w = Walker::default();
    w.frame(800, 600);
    w.step(DT, &[input(stream::KEY_DOWN, 0, 0., 0., "KeyW")]);
    for _ in 0..59 {
        w.step(DT, &[]);
    }
    assert!((w.pos[1] + 4.).abs() < 0.01 && w.pos[0].abs() < 1e-6);
    w.step(DT, &[input(stream::KEY_UP, 0, 0., 0., "KeyW"), input(stream::KEY_DOWN, 0, 0., 0., "ArrowRight")]);
    for _ in 0..29 {
        w.step(DT, &[]);
    }
    assert!((w.pos[0] - 2.).abs() < 0.01);
    w.step(DT, &[input(stream::FOCUS, 0, 0., 0., "")]);
    assert!(w.keys.is_empty());
    let before = w.pos;
    w.step(DT, &[input(stream::DOWN, 0, 400., 300., "")]);
    let target = w.target.expect("click sets a target");
    assert_eq!(w.clicks, 1);
    assert!((target[0] - before[0]).abs() < 0.5 && (target[1] - before[1]).abs() < 1.5, "{target:?} near {before:?}");
    w.step(DT, &[input(stream::DOWN, 0, 790., 590., "")]);
    let far = w.target.unwrap();
    assert!(far[0] > before[0] && far[1] > before[1]);
    for _ in 0..600 {
        w.step(DT, &[]);
    }
    assert!(w.target.is_none() && (w.pos[0] - far[0]).abs() < 1e-4 && (w.pos[1] - far[1]).abs() < 1e-4);
    let f = w.frame(800, 600);
    assert_eq!(f.draws.len(), 4);
}

#[test]
fn buttons_hover_click_and_drag() {
    let mut b = Buttons::default();
    b.step(DT, &[input(stream::MOVE, 0, 100., 80., "")]);
    assert_eq!(b.hover, Some(0));
    b.step(DT, &[input(stream::DOWN, 0, 100., 80., "")]);
    assert_eq!(b.pressed, Some(0));
    b.step(DT, &[input(stream::UP, 0, 100., 80., "")]);
    assert_eq!((b.clicks, b.pressed), ([1, 0, 0], None));
    b.step(DT, &[input(stream::DOWN, 0, 240., 80., ""), input(stream::MOVE, 0, 500., 80., ""), input(stream::UP, 0, 500., 80., "")]);
    assert_eq!((b.clicks, b.hover), ([1, 0, 0], None));
    b.step(DT, &[input(stream::DOWN, 0, 240., 184., "")]);
    assert!(b.dragging && (b.slider - 0.5).abs() < 1e-6);
    b.step(DT, &[input(stream::MOVE, 0, 340., 10., "")]);
    assert!((b.slider - 0.75).abs() < 1e-6);
    b.step(DT, &[input(stream::UP, 0, 340., 10., "")]);
    assert!(!b.dragging);
    b.step(DT, &[input(stream::MOVE, 0, 900., 10., "")]);
    assert!((b.slider - 0.75).abs() < 1e-6);
    b.step(DT, &[input(stream::WHEEL, 0, 0., 100., "")]);
    assert!((b.slider - 0.7).abs() < 1e-6);
    let f = b.frame(640, 480);
    assert_eq!((f.draws.len(), f.textures.len(), f.meshes.len()), (1, 1, 1));
    assert!(b.frame(640, 480).textures.is_empty());
}
