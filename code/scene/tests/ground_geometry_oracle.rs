use openrs_scene::floor::{Overlay, Underlay};
use openrs_scene::ground_geometry::{
    build_blended_ground, GroundGeometryInput, GroundGeometryOptions, MAX_OUTPUT_VERTICES,
};
use openrs_scene::palette::PaletteCell;
use openrs_scene::surface_tiles::SurfaceTile;
use serde_json::{from_str, Value};

const FIXTURE: &str = include_str!("fixtures/ground-geometry.json");
const JAR_SHA256: &str = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

fn number(value: &Value) -> i32 {
    i32::try_from(value.as_i64().unwrap()).unwrap()
}

fn size(value: &Value) -> usize {
    usize::try_from(value.as_u64().unwrap()).unwrap()
}

fn numbers(value: &Value) -> Vec<i32> {
    value.as_array().unwrap().iter().map(number).collect()
}

fn matrix(value: &Value) -> Vec<i32> {
    value.as_array().unwrap().iter().flat_map(numbers).collect()
}

struct FixtureInput {
    width: usize,
    height: usize,
    tiles: Vec<Option<SurfaceTile>>,
    heights: Vec<Option<i32>>,
    palette: Vec<PaletteCell>,
    underlays: Vec<Option<Underlay>>,
    overlays: Vec<Option<Overlay>>,
    rendered: Vec<Option<i32>>,
}

impl FixtureInput {
    fn new(case: &Value) -> Self {
        let input = &case["input"];
        let underlay_ids = matrix(&input["underlay_ids"]);
        let overlay_ids = matrix(&input["overlay_ids"]);
        let shapes = matrix(&input["shapes"]);
        let rotations = matrix(&input["rotations"]);
        let tiles = (0..underlay_ids.len())
            .map(|index| {
                Some(SurfaceTile {
                    underlay_id: u8::try_from(underlay_ids[index]).unwrap(),
                    overlay_id: u8::try_from(overlay_ids[index]).unwrap(),
                    overlay_shape: u8::try_from(shapes[index]).unwrap(),
                    overlay_rotation: u8::try_from(rotations[index]).unwrap(),
                    render_flags: 0,
                })
            })
            .collect();
        let mut underlays = vec![None; 255];
        for value in input["underlays"].as_array().unwrap() {
            underlays[size(&value["id"]) - 1] = Some(Underlay {
                texture: number(&value["texture"]),
                scale: u32::try_from(number(&value["scale"])).unwrap(),
                shadowed: value["shadowed"].as_bool().unwrap(),
                occlude: value["occlude"].as_bool().unwrap(),
                ..Underlay::default()
            });
        }
        let mut overlays = vec![None; 255];
        let mut rendered = vec![None; 255];
        for value in input["overlays"].as_array().unwrap() {
            let id = size(&value["id"]) - 1;
            overlays[id] = Some(Overlay {
                slot: number(&value["slot"]),
                tile_color_hsl: number(&value["tile_color"]),
                minimap_color_hsl: number(&value["minimap_color"]),
                texture: number(&value["texture"]),
                texture_scale: u32::try_from(number(&value["scale"])).unwrap(),
                blends_with_underlay: value["blend"].as_bool().unwrap(),
                shadowed: value["shadowed"].as_bool().unwrap(),
                occlude: value["occlude"].as_bool().unwrap(),
                ..Overlay::default()
            });
            rendered[id] = Some(number(&value["rendered_color"]));
        }
        Self {
            width: size(&case["width"]),
            height: size(&case["height"]),
            tiles,
            heights: matrix(&input["heights"]).into_iter().map(Some).collect(),
            palette: matrix(&input["palette_indices"])
                .into_iter()
                .map(PaletteCell::Known)
                .collect(),
            underlays,
            overlays,
            rendered,
        }
    }

    fn view(&self) -> GroundGeometryInput<'_> {
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
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unchanged_native_call_arrays_match_exact_bounded_default_geometry() {
    let fixture: Value = from_str(FIXTURE).unwrap();
    assert_eq!(fixture["schema"], 1);
    assert_eq!(fixture["jar_sha256"].as_str().unwrap(), JAR_SHA256);
    assert_eq!(
        fixture["capture"].as_str().unwrap(),
        "original MapLoader.addBlendedTiles -> recording Ground.addBlendedTile"
    );
    let mut cases = 0;
    let mut calls = 0;
    let mut vertices = 0;
    let mut raw_shapes = [[false; 4]; 13];
    let mut effective_shapes = [[false; 4]; 15];
    let mut edge_split_calls = 0;
    let mut blending_calls = 0;
    for case in fixture["cases"].as_array().unwrap() {
        if !case["input"]["heights_current"].is_null() || !case["input"]["heights_next"].is_null() {
            continue;
        }
        let name = case["name"].as_str().unwrap();
        let input = FixtureInput::new(case);
        for tile in input.tiles.iter().flatten() {
            if tile.overlay_id != 0 || tile.underlay_id != 0 {
                raw_shapes[usize::from(tile.overlay_shape)][usize::from(tile.overlay_rotation)] =
                    true;
            }
        }
        let geometry = build_blended_ground(
            input.view(),
            GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
        )
        .unwrap();
        let expected = case["calls"].as_array().unwrap();
        assert_eq!(geometry.tiles.len(), expected.len(), "{name} call count");
        for (actual, expected) in geometry.tiles.iter().zip(expected) {
            assert_eq!(
                (actual.x, actual.y),
                (size(&expected["x"]), size(&expected["z"])),
                "{name} call order"
            );
            assert_eq!(
                i32::from(actual.shape),
                number(&expected["effective_shape"]),
                "{name} shape"
            );
            assert_eq!(
                i32::from(actual.rotation),
                number(&expected["effective_rotation"]),
                "{name} rotation"
            );
            assert_eq!(
                actual
                    .vertices
                    .iter()
                    .map(|v| v.offset_x)
                    .collect::<Vec<_>>(),
                numbers(&expected["offsets_x"]),
                "{name} offsets x"
            );
            assert_eq!(
                actual
                    .vertices
                    .iter()
                    .map(|v| v.offset_z)
                    .collect::<Vec<_>>(),
                numbers(&expected["offsets_z"]),
                "{name} offsets z"
            );
            assert_eq!(
                actual
                    .vertices
                    .iter()
                    .map(|v| v.main_color)
                    .collect::<Vec<_>>(),
                numbers(&expected["main_colors"]),
                "{name} main HSL"
            );
            assert_eq!(
                actual
                    .vertices
                    .iter()
                    .map(|v| v.texture)
                    .collect::<Vec<_>>(),
                numbers(&expected["textures"]),
                "{name} texture IDs"
            );
            assert_eq!(
                actual
                    .vertices
                    .iter()
                    .map(|v| v.texture_scale)
                    .collect::<Vec<_>>(),
                numbers(&expected["texture_scales"]),
                "{name} texture scales"
            );
            let expected_blending = (!expected["blending_colors"].is_null())
                .then(|| numbers(&expected["blending_colors"]));
            assert_eq!(
                actual.blending_colors.as_deref(),
                expected_blending.as_deref(),
                "{name} blending HSL"
            );
            assert_eq!(
                actual.has_shadows,
                expected["shadows"].as_bool().unwrap(),
                "{name} shadows"
            );
            assert!(expected["draw_heights_a"].is_null());
            assert!(expected["draw_heights_b"].is_null());
            effective_shapes[usize::from(actual.shape)][usize::from(actual.rotation)] = true;
            edge_split_calls += usize::from(actual.split_edges.iter().any(|&split| split));
            blending_calls += usize::from(expected["overlay_supports_blending"].as_bool().unwrap());
            vertices += actual.vertices.len();
            calls += 1;
        }
        cases += 1;
    }
    assert_eq!((cases, calls, vertices), (586, 1184, 16_296));
    assert!(raw_shapes.iter().flatten().all(|&seen| seen));
    assert!(effective_shapes.iter().flatten().all(|&seen| seen));
    assert_eq!(edge_split_calls, 516);
    assert_eq!(blending_calls, 359);
    println!("exact bounded native ground parity: cases={cases} calls={calls} vertices={vertices} split_calls={edge_split_calls} blending_calls={blending_calls}");
}
