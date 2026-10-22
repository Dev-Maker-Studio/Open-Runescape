use renderer::{capture, frame, pass::Pass};

#[test]
fn jvm_clear_fixture_preserves_operation_order_and_gl_depth_conversion() {
    let f = frame::parse(include_bytes!("fixtures/color-clears.orf")).unwrap();
    assert_eq!((f.width, f.height, f.clear), (12, 10, [1., 0., 1., 1.]));
    assert_eq!(f.draws.len(), 4);
    let slots: Vec<_> = f
        .draws
        .iter()
        .map(|d| {
            let mesh = f.meshes.iter().find(|m| m.id == d.mesh).unwrap();
            f32::from_le_bytes(mesh.vertices[20..24].try_into().unwrap())
        })
        .collect();
    assert_eq!(slots, [-2., -1., -2., -1.]);
    for i in [0, 2] {
        let d = &f.draws[i];
        assert_eq!(d.pass, Pass::Sprite);
        assert_eq!(d.state, 7 << 7);
        assert_eq!(d.viewport, [0, 0, 12, 10]);
        assert_eq!(d.textures, [frame::NO_TEXTURE]);
        let uniform = f.uniforms.iter().find(|u| u.id == d.uniform).unwrap();
        assert_eq!(
            f32::from_le_bytes(uniform.bytes[56..60].try_into().unwrap()),
            0.5
        );
    }
    assert_eq!(f.draws[2].scissor, [2, 0, 8, 6]);
}

#[test]
fn identity_alias_resolve_preserves_history_without_repairing_an_unported_mesh() {
    let f = frame::parse(include_bytes!("fixtures/untouched-history.orf")).unwrap();
    assert_eq!(f.skipped().get("unported.mesh"), Some(&1));
    let copies: Vec<_> = f.draws.iter().filter(|d| d.skip.is_none()).collect();
    assert_eq!(copies.len(), 2);
    assert_eq!(
        copies
            .iter()
            .map(|d| d.scissor[2] * d.scissor[3])
            .sum::<u32>(),
        20
    );
    assert!(
        copies
            .iter()
            .all(|d| d.scissor[0] == 0 || d.scissor[0] == 19)
    );
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    let pixels = capture::render_rgba(&mut renderer, &f);
    let mut without_history = f.clone();
    without_history.draws.retain(|d| d.skip.is_some());
    let baseline = capture::render_rgba(&mut renderer, &without_history);
    let mut missing_mesh_pixels = 0;
    for y in 0..10 {
        for x in 0..20 {
            let offset = (y * 20 + x) * 4;
            if x == 0 || x == 19 {
                assert_eq!(
                    pixels[offset..offset + 4],
                    [0, 0, (y + 1) as u8, if y % 2 == 0 { 0 } else { 127 }],
                    "border {x},{y}"
                );
                assert_eq!(baseline[offset..offset + 4], [141, 164, 194, 255]);
            } else {
                assert_eq!(
                    pixels[offset..offset + 4],
                    baseline[offset..offset + 4],
                    "history overlaid unknown viewport {x},{y}"
                );
                assert_eq!(pixels[offset..offset + 4], [141, 164, 194, 255]);
                if pixels[offset..offset + 4] != [255, 0, 0, 255] {
                    missing_mesh_pixels += 1;
                }
            }
        }
    }
    assert_eq!(missing_mesh_pixels, 180);
}

#[test]
fn full_base_then_scissored_sky_keeps_border_and_interleaved_sprites() {
    let f = frame::parse(include_bytes!("fixtures/color-clears.orf")).unwrap();
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    let pixels = capture::render_rgba(&mut renderer, &f);
    for y in 0..10 {
        for x in 0..12 {
            let expected = if (7..9).contains(&x) && (2..4).contains(&y) {
                [200, 30, 50, 255]
            } else if (2..10).contains(&x) && y < 6 {
                [141, 164, 194, 128]
            } else if (4..8).contains(&x) && (3..9).contains(&y) {
                [41, 38, 33, 255]
            } else {
                [0, 0, 1, 0]
            };
            let offset = (y * 12 + x) * 4;
            assert_eq!(pixels[offset..offset + 4], expected, "pixel {x},{y}");
        }
    }
}
