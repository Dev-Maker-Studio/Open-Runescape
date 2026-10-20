use renderer::{capture, frame, pass::Pass, scene};

fn render(f: &frame::Frame) -> Vec<u8> {
    capture::render_rgba(&mut pollster::block_on(capture::headless()), f)
}

#[test]
fn scene_round_trip() {
    let original = scene::build("all", 0, 320, 240).unwrap();
    let parsed = frame::parse(&frame::write(&original)).unwrap();
    assert_eq!(parsed.draws.len(), original.draws.len());
    assert_eq!(parsed.meshes.len(), original.meshes.len());
    assert_eq!(parsed.instances.len(), original.instances.len());
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter, parse round trip checked only");
        return;
    };
    assert_eq!(capture::render_rgba(&mut renderer, &parsed), capture::render_rgba(&mut renderer, &original));
}

#[test]
fn client_fixture_parses() {
    let f = frame::parse(include_bytes!("fixtures/synthetic.orf")).unwrap();
    assert_eq!((f.width, f.height, f.clear), (320, 240, [0.1, 0.2, 0.3, 1.]));
    assert_eq!((f.meshes.len(), f.textures.len(), f.uniforms.len(), f.instances.len(), f.draws.len()), (2, 2, 2, 1, 5));
    let pattern = |size: usize, seed: usize| (0..size).map(|i| ((i * 31 + seed) & 0xff) as u8).collect::<Vec<_>>();
    let model = &f.meshes[0];
    assert_eq!((model.pass, model.vertices.len() as u64 / model.pass.stride(), &model.indices[..]), (Pass::Model, 3, &[0, 1, 2][..]));
    assert_eq!(model.vertices, pattern(3 * 36, 1));
    assert_eq!((f.meshes[1].pass, f.meshes[1].indices.len()), (Pass::Sprite, 6));
    assert_eq!((f.textures[0].width, f.textures[0].layers, &f.textures[0].rgba), (2, 1, &pattern(16, 3)));
    assert_eq!((f.textures[1].width, f.textures[1].height, f.textures[1].layers, &f.textures[1].rgba), (4, 4, 2, &pattern(128, 4)));
    assert_eq!((f.uniforms[0].bytes.len(), f.uniforms[1].bytes.len()), (272, 64));
    assert_eq!(f.uniforms[0].bytes[228..232], 1u32.to_le_bytes());
    assert_eq!((f.instances[0].count, &f.instances[0].bytes), (2, &pattern(160, 5)));
    let drawn: Vec<_> = f.draws.iter().filter(|d| d.skip.is_none()).map(|d| (d.pass, d.mesh, d.uniform, d.textures.clone(), d.instances)).collect();
    let none = frame::NO_TEXTURE;
    assert_eq!(
        drawn,
        [(Pass::Model, 0, 0, vec![0], frame::NO_INSTANCES), (Pass::Model, 0, 0, vec![0], frame::NO_INSTANCES), (Pass::Instanced, 0, 0, vec![none], 0), (Pass::Sprite, 1, 1, vec![1], frame::NO_INSTANCES)]
    );
    assert_eq!(f.draws[4].scissor, [8, 8, 100, 50]);
    assert_eq!(f.skipped().get("groundTextured.water"), Some(&1));
    assert!(f.summary().ends_with("drawn 4 [instanced=1,model=2,sprite=1], skipped 1 [groundTextured.water=1]"));
}

#[test]
fn skipped_draws_are_counted() {
    let mut f = scene::build("model", 0, 64, 64).unwrap();
    let mut skip = f.draws[0].clone();
    skip.skip = Some("water".into());
    f.draws.extend([skip.clone(), skip]);
    let parsed = frame::parse(&frame::write(&f)).unwrap();
    assert_eq!(parsed.skipped().get("water"), Some(&2));
    assert!(parsed.summary().contains("skipped 2 [water=2]"));
}

#[test]
fn client_dump_replays() {
    let Ok(path) = std::env::var("ORF_CLIENT_DUMP") else { return };
    let f = frame::parse(&std::fs::read(path).unwrap()).unwrap();
    let drawn = f.draws.iter().filter(|d| d.skip.is_none()).count();
    assert!(drawn > 0);
    let rgba = render(&f);
    assert_eq!(rgba.len(), (f.width * f.height * 4) as usize);
    let painted = rgba.chunks(4).filter(|p| p[..3] != rgba[..3]).count();
    assert!(painted * 4 > rgba.len() / 4, "frame is mostly one color");
}
