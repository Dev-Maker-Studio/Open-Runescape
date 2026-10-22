use openrs_scene::ground_lowering::lighting_intensity;
use openrs_scene::loc_type::{decode_loc_type, LocType};
use openrs_scene::locs::{decode_region_locs, place_in_scene, ScenePlacement};
use openrs_scene::static_shadows::{
    bake_static_shadows, wall_corners, ShadowGrid, MAX_INTERACT_SHADOW,
};
use std::collections::HashMap;
#[cfg(target_arch = "wasm32")]
use wasm_bindgen_test::wasm_bindgen_test;

const DEFINITIONS: &[u8] = include_bytes!("fixtures/shadow-defs.bundle");
const REGIONS: [(i32, &[u8]); 12] = [
    (10034, include_bytes!("fixtures/shadow-l10034.bin")),
    (10035, include_bytes!("fixtures/shadow-l10035.bin")),
    (10288, include_bytes!("fixtures/shadow-l10288.bin")),
    (10289, include_bytes!("fixtures/shadow-l10289.bin")),
    (10290, include_bytes!("fixtures/shadow-l10290.bin")),
    (10291, include_bytes!("fixtures/shadow-l10291.bin")),
    (10544, include_bytes!("fixtures/shadow-l10544.bin")),
    (10545, include_bytes!("fixtures/shadow-l10545.bin")),
    (13621, include_bytes!("fixtures/shadow-l13621.bin")),
    (13622, include_bytes!("fixtures/shadow-l13622.bin")),
    (13877, include_bytes!("fixtures/shadow-l13877.bin")),
    (13878, include_bytes!("fixtures/shadow-l13878.bin")),
];
struct Scene {
    name: &'static str,
    base_x: i32,
    base_y: i32,
    size: i32,
    native: &'static [u8],
    stats: &'static str,
}

const SCENES: [Scene; 3] = [
    Scene {
        name: "a",
        base_x: 39 * 64 + 24,
        base_y: 50 * 64 + 24,
        size: 104,
        native: include_bytes!("fixtures/native-shadow-a.bin"),
        stats: include_str!("fixtures/native-shadow-a.tsv"),
    },
    Scene {
        name: "b",
        base_x: 40 * 64,
        base_y: 48 * 64,
        size: 128,
        native: include_bytes!("fixtures/native-shadow-b.bin"),
        stats: include_str!("fixtures/native-shadow-b.tsv"),
    },
    Scene {
        name: "c",
        base_x: 53 * 64 + 20,
        base_y: 53 * 64 + 20,
        size: 104,
        native: include_bytes!("fixtures/native-shadow-c.bin"),
        stats: include_str!("fixtures/native-shadow-c.tsv"),
    },
];

fn injected_size2d(placement: &ScenePlacement) -> Option<i32> {
    let mixed = (placement.id as u64)
        .wrapping_mul(2654435761)
        .wrapping_add(placement.rotation as u64 * 40503)
        .wrapping_add(placement.kind as u64 * 977)
        & 0xFFFF_FFFF;
    let value = mixed >> 8;
    if value.is_multiple_of(7) {
        None
    } else {
        Some((value % 160) as i32)
    }
}

fn definitions() -> HashMap<i32, LocType> {
    let mut at = 0usize;
    let mut take = |count: usize| {
        let slice = &DEFINITIONS[at..at + count];
        at += count;
        slice
    };
    let total = i32::from_be_bytes(take(4).try_into().unwrap());
    let mut types = HashMap::new();
    for _ in 0..total {
        let id = i32::from_be_bytes(take(4).try_into().unwrap());
        let length = i32::from_be_bytes(take(4).try_into().unwrap());
        if length < 0 {
            continue;
        }
        let data = take(length as usize);
        types.insert(id, decode_loc_type(id, Some(data), true).expect("loc type"));
    }
    types
}

fn scene_placements(base_x: i32, base_y: i32, size: i32) -> Vec<ScenePlacement> {
    let mut scene = Vec::new();
    for (region, data) in REGIONS {
        let (region_x, region_y) = (region >> 8, region & 255);
        let overlaps = region_x * 64 < base_x + size
            && region_x * 64 + 64 > base_x
            && region_y * 64 < base_y + size
            && region_y * 64 + 64 > base_y;
        if overlaps {
            let decoded = decode_region_locs(data).expect("locs");
            scene.extend(place_in_scene(region, &decoded, base_x, base_y, size));
        }
    }
    scene
}

fn compare(textures: bool) {
    let types = definitions();
    for Scene {
        name,
        base_x,
        base_y,
        size,
        native,
        stats,
    } in SCENES
    {
        let placements = scene_placements(base_x, base_y, size);
        let (grids, rust) = bake_static_shadows(
            4,
            size,
            &placements,
            |id| types.get(&id),
            |placement, _| injected_size2d(placement),
            textures,
        );
        let width = size as usize + 1;
        let cells = width * width;
        let per_texture = 4 * 2 * cells;
        let offset = if textures { 0 } else { per_texture };
        let mut mismatches = 0usize;
        let mut non_zero = 0usize;
        for (plane, grid) in grids.iter().enumerate() {
            let start = offset + plane * 2 * cells;
            let expected = &native[start..start + cells];
            let expected_intensity = &native[start + cells..start + 2 * cells];
            mismatches += grid
                .cells
                .iter()
                .zip(expected)
                .filter(|(a, b)| a != b)
                .count();
            let intensity =
                lighting_intensity(&grid.cells, size as usize, size as usize).expect("intensity");
            assert_eq!(intensity.len(), cells);
            mismatches += intensity
                .iter()
                .zip(expected_intensity)
                .filter(|(a, b)| a != b)
                .count();
            non_zero += grid.non_zero();
        }
        assert_eq!(mismatches, 0, "scene {name} textures {textures}");
        let row = stats
            .lines()
            .map(|line| line.split('\t').collect::<Vec<_>>())
            .find(|f| f[1] == name && f[2] == if textures { "1" } else { "0" })
            .expect("stats row");
        let field = |index: usize| row[index].parse::<usize>().unwrap();
        assert_eq!(rust.placements, field(3));
        assert_eq!(rust.missing_type, field(4));
        assert_eq!(rust.skipped_textures, field(5));
        assert_eq!(rust.walls, field(6));
        assert_eq!(rust.interacts, field(7));
        assert_eq!(rust.interacts_out_of_scene, field(8));
        assert_eq!(rust.animated_interacts, field(9));
        assert_eq!(rust.model_displacements, field(10));
        assert_eq!(non_zero, field(12));
        assert!(rust.walls > 0 && rust.interacts > 0 && non_zero > 0);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn static_shadows_match_native_with_textures() {
    compare(true);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn static_shadows_match_native_without_textures() {
    compare(false);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn texture_gate_skips_required_textures() {
    let types = definitions();
    let placements = scene_placements(53 * 64 + 20, 53 * 64 + 20, 104);
    let (_, on) = bake_static_shadows(
        4,
        104,
        &placements,
        |id| types.get(&id),
        |p, _| injected_size2d(p),
        true,
    );
    let (_, off) = bake_static_shadows(
        4,
        104,
        &placements,
        |id| types.get(&id),
        |p, _| injected_size2d(p),
        false,
    );
    assert_eq!(on.skipped_textures, 0);
    assert!(off.skipped_textures > 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn wall_corner_table_and_max_semantics() {
    assert_eq!(wall_corners(0, 1), &[(0, 1), (1, 1)]);
    assert_eq!(wall_corners(3, 3), &[(0, 0)]);
    assert!(wall_corners(2, 0).is_empty());
    assert!(wall_corners(9, 2).is_empty());
    let mut grid = ShadowGrid::new(8);
    grid.set_tile_shadow(2, 2, 50);
    grid.set_tile_shadow(2, 2, 10);
    assert_eq!(grid.get(2, 2), 50);
    assert_eq!(MAX_INTERACT_SHADOW, 30);
}
