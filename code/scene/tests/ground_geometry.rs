use openrs_scene::floor::{Overlay, Underlay};
use openrs_scene::ground_geometry::{
    build_blended_ground, GroundGeometry, GroundGeometryError, GroundGeometryInput,
    GroundGeometryOptions, MAX_DIMENSION, MAX_OUTPUT_VERTICES, MAX_VERTICES_PER_TILE,
};
use openrs_scene::palette::PaletteCell;
use openrs_scene::surface_tiles::SurfaceTile;

#[derive(Clone)]
struct Inputs {
    width: usize,
    height: usize,
    tiles: Vec<Option<SurfaceTile>>,
    heights: Vec<Option<i32>>,
    palette: Vec<PaletteCell>,
    underlays: Vec<Option<Underlay>>,
    overlays: Vec<Option<Overlay>>,
    rendered: Vec<Option<i32>>,
}

impl Inputs {
    fn new(width: usize, height: usize) -> Self {
        Self {
            width,
            height,
            tiles: vec![Some(SurfaceTile::default()); width * height],
            heights: vec![Some(0); (width + 1) * (height + 1)],
            palette: vec![PaletteCell::Known(12_345); width * height],
            underlays: vec![Some(Underlay::default()); 255],
            overlays: (0..255)
                .map(|id| Some(Overlay::default_for_id(id)))
                .collect(),
            rendered: vec![Some(0); 255],
        }
    }

    fn input(&self) -> GroundGeometryInput<'_> {
        GroundGeometryInput {
            width: self.width,
            height: self.height,
            tiles: &self.tiles,
            ground_heights: &self.heights,
            palette: &self.palette,
            underlays: &self.underlays,
            overlays: &self.overlays,
            overlay_render_colors: &self.rendered,
        }
    }

    fn build(&self) -> GroundGeometry {
        build_blended_ground(
            self.input(),
            GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
        )
        .unwrap()
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn empty_and_underlay_only_keep_exact_order_and_boundary_clamping() {
    let mut input = Inputs::new(2, 2);
    assert!(input.build().tiles.is_empty());
    for tile in input.tiles.iter_mut().flatten() {
        tile.underlay_id = 1;
    }
    input.palette = (0..4)
        .map(|value| PaletteCell::Known(value * 100))
        .collect();
    let result = input.build();
    assert_eq!(result.vertex_count, 24);
    assert_eq!(
        result
            .tiles
            .iter()
            .map(|tile| (tile.x, tile.y))
            .collect::<Vec<_>>(),
        [(0, 0), (0, 1), (1, 0), (1, 1)]
    );
    let first = &result.tiles[0];
    assert_eq!(first.shape, 12);
    assert_eq!(first.rotation, 0);
    assert!(first.blending_colors.is_none());
    assert!(first.has_shadows);
    assert_eq!(
        first
            .vertices
            .iter()
            .map(|v| (v.offset_x, v.offset_z, v.main_color))
            .collect::<Vec<_>>(),
        [
            (0, 0, 0),
            (512, 0, 200),
            (0, 512, 100),
            (512, 0, 200),
            (512, 512, 300),
            (0, 512, 100)
        ]
    );
    assert!(result.tiles[3]
        .vertices
        .iter()
        .all(|vertex| vertex.main_color == 300));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn overlay_and_underlay_colors_are_separate_and_shadow_flags_are_not_forced() {
    let mut input = Inputs::new(1, 1);
    input.tiles[0] = Some(SurfaceTile {
        underlay_id: 1,
        overlay_id: 1,
        overlay_shape: 1,
        ..SurfaceTile::default()
    });
    let overlay = input.overlays[0].as_mut().unwrap();
    overlay.tile_color_hsl = 400;
    overlay.minimap_color_hsl = 500;
    overlay.texture = 19;
    overlay.texture_scale = 2048;
    overlay.shadowed = false;
    input.rendered[0] = Some(500);
    input.underlays[0].as_mut().unwrap().shadowed = false;
    let result = input.build();
    let tile = &result.tiles[0];
    assert_eq!(tile.vertices.len(), 6);
    assert!(!tile.has_shadows);
    assert!(tile.vertices[..3]
        .iter()
        .all(|v| v.main_color == 400 && v.texture == 19 && v.texture_scale == 2048));
    assert!(tile.vertices[3..]
        .iter()
        .all(|v| v.main_color == 12_345 && v.texture == -1 && v.texture_scale == 512));
    assert_eq!(
        tile.blending_colors.as_deref(),
        Some([500, 500, 500, 12_345, 12_345, 12_345].as_slice())
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_raw_shapes_rotations_and_presence_combinations_are_bounded_and_deterministic() {
    let mut input = Inputs::new(3, 3);
    for shape in 0..=12 {
        for rotation in 0..4 {
            for presence in 1..=3 {
                for blending in [false, true] {
                    for tile in input.tiles.iter_mut().flatten() {
                        *tile = SurfaceTile {
                            underlay_id: u8::from(presence & 1 != 0),
                            overlay_id: u8::from(presence & 2 != 0),
                            overlay_shape: shape,
                            overlay_rotation: rotation,
                            render_flags: 255,
                        };
                    }
                    input.overlays[0].as_mut().unwrap().blends_with_underlay = blending;
                    let a = input.build();
                    let b = input.build();
                    assert_eq!(a, b);
                    for tile in &a.tiles {
                        assert!(tile.shape <= 14);
                        assert!(tile.rotation < 4);
                        assert_eq!(tile.vertices.len() % 3, 0);
                        assert!(tile.vertices.len() <= MAX_VERTICES_PER_TILE);
                        assert!(tile.emitted_vertices <= tile.vertices.len());
                        assert!(tile
                            .vertices
                            .iter()
                            .all(|v| (0..=512).contains(&v.offset_x)
                                && (0..=512).contains(&v.offset_z)));
                    }
                }
            }
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn deterministic_mixed_neighbors_preserve_allocation_and_render_flags_do_not_change_topology() {
    let mut input = Inputs::new(5, 4);
    let mut seed = 0x6543_210f_u32;
    for sample in 0..160 {
        for (id, overlay) in input.overlays.iter_mut().take(5).enumerate() {
            let overlay = overlay.as_mut().unwrap();
            overlay.blends_with_underlay = id % 2 == 0;
            overlay.tile_color_hsl = (id as i32 + 1) * 1200;
            overlay.minimap_color_hsl = overlay.tile_color_hsl;
            overlay.slot = (id / 2) as i32;
            input.rendered[id] = Some(overlay.tile_color_hsl);
        }
        for (index, tile) in input.tiles.iter_mut().flatten().enumerate() {
            seed = seed.wrapping_mul(1_664_525).wrapping_add(1_013_904_223);
            *tile = SurfaceTile {
                underlay_id: ((seed >> 5) % 6) as u8,
                overlay_id: ((seed >> 13) % 6) as u8,
                overlay_shape: ((seed >> 20) % 13) as u8,
                overlay_rotation: ((seed >> 28) & 3) as u8,
                render_flags: (sample + index) as u8,
            };
        }
        let expected = input.build();
        assert!(expected
            .tiles
            .iter()
            .all(|tile| tile.vertices.len() <= MAX_VERTICES_PER_TILE));
        for tile in input.tiles.iter_mut().flatten() {
            tile.render_flags ^= 255;
        }
        assert_eq!(input.build(), expected);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exact_vertex_budget_failure_does_not_mutate_inputs_or_previous_result() {
    let mut input = Inputs::new(3, 3);
    for tile in input.tiles.iter_mut().flatten() {
        tile.underlay_id = 1;
    }
    let previous = input.build();
    let tiles = input.tiles.clone();
    let vertices = previous.vertex_count;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(vertices - 1)),
        Err(GroundGeometryError::VertexBudget {
            required: vertices,
            limit: vertices - 1
        })
    );
    assert_eq!(input.tiles, tiles);
    assert_eq!(input.build(), previous);
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(vertices)).unwrap(),
        previous
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unsupported_layers_and_malformed_or_unavailable_inputs_fail_closed() {
    let mut input = Inputs::new(1, 1);
    let mut options = GroundGeometryOptions::surface(0);
    options.overlay_hidden = true;
    assert_eq!(
        build_blended_ground(input.input(), options),
        Err(GroundGeometryError::UnsupportedUnderwater)
    );
    options.overlay_hidden = false;
    options.secondary_ground_heights = true;
    assert_eq!(
        build_blended_ground(input.input(), options),
        Err(GroundGeometryError::UnsupportedSecondaryGround)
    );
    input.tiles[0] = None;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailableTile { x: 0, y: 0 })
    );
    input.tiles[0] = Some(SurfaceTile {
        overlay_shape: 13,
        ..SurfaceTile::default()
    });
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::InvalidShape {
            x: 0,
            y: 0,
            shape: 13
        })
    );
    input.tiles[0] = Some(SurfaceTile {
        overlay_rotation: 4,
        ..SurfaceTile::default()
    });
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::InvalidRotation {
            x: 0,
            y: 0,
            rotation: 4
        })
    );
    input.tiles[0] = Some(SurfaceTile::default());
    input.heights[3] = None;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailableHeight { x: 1, y: 1 })
    );
    input.heights[3] = Some(0);
    input.palette[0] = PaletteCell::Unavailable;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailablePalette { x: 0, y: 0 })
    );
    input.palette[0] = PaletteCell::Known(0);
    input.tiles[0].as_mut().unwrap().underlay_id = 255;
    input.underlays[254] = None;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailableUnderlay { definition_id: 254 })
    );
    input.tiles[0].as_mut().unwrap().underlay_id = 0;
    input.tiles[0].as_mut().unwrap().overlay_id = 255;
    input.overlays[254] = None;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailableOverlay { definition_id: 254 })
    );
    input.overlays[254] = Some(Overlay::default());
    input.rendered[254] = None;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::UnavailableOverlayRenderColor { definition_id: 254 })
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dimensions_lengths_scales_and_maximum_extent_are_checked_before_allocation() {
    for (width, height) in [(0, 1), (1, 0), (MAX_DIMENSION + 1, 1), (1, usize::MAX)] {
        let input = GroundGeometryInput {
            width,
            height,
            tiles: &[],
            ground_heights: &[],
            palette: &[],
            underlays: &[],
            overlays: &[],
            overlay_render_colors: &[],
        };
        assert_eq!(
            build_blended_ground(input, GroundGeometryOptions::surface(0)),
            Err(GroundGeometryError::Dimensions { width, height })
        );
    }
    let mut input = Inputs::new(1, 1);
    input.tiles.clear();
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::InputLength {
            input: "tiles",
            expected: 1,
            actual: 0
        })
    );
    input.tiles.push(Some(SurfaceTile {
        underlay_id: 1,
        ..SurfaceTile::default()
    }));
    input.underlays[0].as_mut().unwrap().scale = u32::MAX;
    assert_eq!(
        build_blended_ground(input.input(), GroundGeometryOptions::surface(0)),
        Err(GroundGeometryError::InvalidScale {
            overlay: false,
            definition_id: 0,
            scale: u32::MAX
        })
    );
    let largest = Inputs::new(MAX_DIMENSION, MAX_DIMENSION);
    assert!(largest.build().tiles.is_empty());
}
