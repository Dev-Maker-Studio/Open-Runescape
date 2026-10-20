use renderer::{capture, frame, scene};

#[test]
fn every_scene_renders_on_a_gpu() {
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    for name in scene::NAMES {
        let mut source = scene::Source::replay(frame::parse(&frame::write(&scene::build(name, 0, 160, 120).unwrap())).unwrap());
        let mut rgba = vec![];
        for i in 0..2 {
            rgba = capture::render_rgba(&mut renderer, &source.next(i, 160, 120));
        }
        let background = &rgba[..4];
        let painted = rgba.chunks(4).filter(|p| *p != background).count();
        assert!(painted > 0, "{name}: nothing drawn");
    }
}
