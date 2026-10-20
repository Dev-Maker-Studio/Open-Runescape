use renderer::{
    frame::{self, Draw, Frame, NO_TEXTURE},
    pass::{self, Pass},
    scene, water,
};

const CLIENT_SHADERS: [&str; 16] = [
    "model", "ground", "sprite", "groundTextured", "instanced", "shadow", "particle", "hdWater", "materialCubeSpec", "water", "waterHD",
    "waterLapping", "waterReflecting", "waterRippling", "waterfall", "surfaceMask",
];

#[test]
fn every_client_shader_routes_to_one_pass() {
    for name in CLIENT_SHADERS {
        let p = Pass::from_shader(name).unwrap_or_else(|| panic!("{name} has no pass"));
        assert_eq!(p.shader(), name);
        assert_eq!(Pass::from_u32(p as u32), Some(p));
    }
    for name in ["batched", "optimized", "fullscreen", "hdWaterTess"] {
        assert_eq!(Pass::from_shader(name), None);
    }
    assert_eq!(pass::ALL.len(), CLIENT_SHADERS.len());
}

#[test]
fn replay_dispatches_every_pass() {
    let mut f = Frame { width: 8, height: 8, ..Default::default() };
    for p in pass::ALL {
        f.draws.push(Draw::new(p, 0, 0, vec![NO_TEXTURE; p.bindings().len()]));
    }
    let parsed = frame::parse(&frame::write(&f)).unwrap();
    assert_eq!(parsed.draws.iter().map(|d| d.pass).collect::<Vec<_>>(), pass::ALL);
    let summary = parsed.summary();
    for p in pass::ALL {
        assert!(summary.contains(&format!("{}=1", p.shader())), "{summary}");
    }
    assert!(summary.ends_with("skipped 0 []"));
}

#[test]
fn offscreen_passes_are_only_the_shadow_pass() {
    assert_eq!(pass::ALL.iter().filter(|p| p.offscreen()).collect::<Vec<_>>(), [&Pass::Shadow]);
}

#[test]
fn scenes_cover_every_new_pass() {
    let all = scene::build("all", 0, 320, 240).unwrap();
    for p in [Pass::Shadow, Pass::Particle, Pass::HdWater, Pass::Material, Pass::SurfaceMask] {
        assert!(all.draws.iter().any(|d| d.pass == p), "{:?} missing from all", p);
    }
    let parsed = frame::parse(&frame::write(&all)).unwrap();
    for (a, b) in parsed.draws.iter().zip(&all.draws) {
        assert_eq!((a.pass, a.mesh, a.uniform, &a.textures, a.state), (b.pass, b.mesh, b.uniform, &b.textures, b.state));
    }
    for name in scene::NAMES {
        assert!(scene::build(name, 1, 64, 64).is_some(), "{name}");
    }
}

#[test]
fn water_tessellation_follows_screen_size() {
    let f = scene::build("water", 0, 320, 240).unwrap();
    let mesh = f.meshes.iter().find(|m| m.pass == Pass::HdWater).unwrap();
    let vertices: Vec<_> = bytemuck::pod_collect_to_vec(&mesh.vertices);
    let u = &f.uniforms.iter().find(|u| u.id == 40).unwrap().bytes;
    assert!(water::waving(u));
    let vp: &[f32; 16] = bytemuck::from_bytes(&u[..64]);
    let (near, far) = (water::tessellate(&vertices, &mesh.indices, vp, [0, 0, 320, 240]), water::tessellate(&vertices, &mesh.indices, vp, [0, 0, 32, 24]));
    assert!(near.1.len() > far.1.len() && far.1.len() > mesh.indices.len());
    assert_eq!(near.1.len() % 3, 0);
    assert!(near.1.iter().all(|i| (*i as usize) < near.0.len()));
    let max = near.0.iter().map(|v| v.p[0].abs().max(v.p[2].abs())).fold(0f32, f32::max);
    assert!((max - 10.).abs() < 1e-4);
    let flat = water::tessellate(&vertices, &mesh.indices, &renderer::math::IDENTITY, [0, 0, 1, 1]);
    assert_eq!(flat.1.len(), mesh.indices.len());
}
