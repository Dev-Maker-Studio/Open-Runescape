use openrs_scene::surface::{
    NativeMapSize, RegionAvailability, StandardMapLayout, SurfaceError, SurfaceHeights,
};
use openrs_scene::surface_tiles::{SurfaceTile, SurfaceTiles, SurfaceTilesError};
use openrs_scene::terrain::{DecodeLimits, Terrain, Tile, MAP_SIZE, PLANES};

const NATIVE: &str = include_str!("fixtures/surface-tiles.tsv");
const JAR_SHA256: &str = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
const SIZES: [usize; 5] = [104, 120, 136, 168, 72];

fn archive_bytes(region_id: u16) -> Vec<u8> {
    let region = usize::from(region_id);
    let mut bytes = Vec::new();
    for plane in 0..PLANES {
        for x in 0..MAP_SIZE {
            for y in 0..MAP_SIZE {
                let index = (plane * MAP_SIZE + x) * MAP_SIZE + y;
                let seed = region * 37 + plane * 101 + x * 13 + y * 29;
                if !(index + region).is_multiple_of(11) {
                    bytes.extend_from_slice(&[2, 255, 82, 50]);
                    let zero_overlay = (index + region).is_multiple_of(13);
                    let tag = if zero_overlay {
                        49
                    } else {
                        2 + (region + plane * 17 + x * 7 + y * 13) % 48
                    };
                    let overlay = if zero_overlay {
                        0
                    } else {
                        (region * 19 + plane * 61 + x * 37 + y * 73) & 255
                    };
                    let underlay = 1 + (region * 11 + plane * 43 + x * 17 + y * 23) % 174;
                    let flags = 1 + (region * 7 + plane * 11 + x * 5 + y * 3) % 32;
                    bytes.extend_from_slice(&[
                        tag as u8,
                        overlay as u8,
                        (81 + underlay) as u8,
                        (49 + flags) as u8,
                    ]);
                }
                bytes.extend_from_slice(&[1, (seed & 255) as u8]);
            }
        }
    }
    bytes
}

fn terrain(region_id: u16) -> Terrain {
    let bytes = archive_bytes(region_id);
    Terrain::decode(
        &bytes,
        DecodeLimits {
            max_input_bytes: bytes.len(),
            max_tags_per_tile: 7,
        },
    )
    .unwrap()
}

fn layout(base: [i32; 2], size: usize) -> StandardMapLayout {
    let half = size as i32 >> 4;
    let center = base.map(|coordinate| coordinate / 8 + half);
    let mut ids = Vec::new();
    for x in (center[0] - half) / 8..=(center[0] + half) / 8 {
        for y in (center[1] - half) / 8..=(center[1] + half) / 8 {
            ids.push(((x << 8) + y) as u16);
        }
    }
    StandardMapLayout::new(base, NativeMapSize::from_tiles(size).unwrap(), &ids).unwrap()
}

fn attributes(tile: &SurfaceTile) -> [u8; 5] {
    [
        tile.underlay_id,
        tile.overlay_id,
        tile.render_flags,
        tile.overlay_shape,
        tile.overlay_rotation,
    ]
}

fn from_terrain(tile: &Tile) -> SurfaceTile {
    SurfaceTile {
        underlay_id: tile.underlay_id,
        overlay_id: tile.overlay_id,
        render_flags: tile.render_flags,
        overlay_shape: tile.overlay_shape,
        overlay_rotation: tile.overlay_rotation,
    }
}

fn tile_hash(surface: &SurfaceTiles) -> u64 {
    let mut hash = 0xcbf29ce484222325_u64;
    for tile in surface.tiles() {
        for byte in attributes(
            tile.as_ref()
                .expect("native fixture has complete availability"),
        ) {
            hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
        }
    }
    hash
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_decode_matches_every_attribute_on_all_five_sizes_clipping_seams_and_planes() {
    let mut cases = 0;
    let mut samples = 0;
    let mut hashed_tiles = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("case\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 7);
        let case_id = fields[1];
        let size: usize = fields[2].parse().unwrap();
        let base = [fields[3].parse().unwrap(), fields[4].parse().unwrap()];
        let mut ids = Vec::new();
        let mut owned = Vec::new();
        let mut absent = Vec::new();
        for row in NATIVE.lines().filter(|row| row.starts_with("region\t")) {
            let region: Vec<_> = row.split('\t').collect();
            if region[1] != case_id {
                continue;
            }
            assert_eq!(region.len(), 5);
            assert_eq!(region[2].parse::<usize>().unwrap(), ids.len());
            let id = region[3].parse().unwrap();
            ids.push(id);
            match region[4] {
                "present" => owned.push((id, terrain(id))),
                "absent" => absent.push(id),
                other => panic!("unexpected native availability {other}"),
            }
        }
        let layout =
            StandardMapLayout::new(base, NativeMapSize::from_tiles(size).unwrap(), &ids).unwrap();
        let present: Vec<_> = owned.iter().map(|(id, terrain)| (*id, terrain)).collect();
        let heights = SurfaceHeights::compose(&layout, &present, &absent).unwrap();
        let heights_before = heights.clone();
        let tiles = SurfaceTiles::compose(&heights, &present).unwrap();
        assert_eq!(heights, heights_before);
        assert_eq!(tiles.layout(), &layout);
        assert_eq!(tiles.tiles().len(), PLANES * size * size);
        assert_eq!(
            tile_hash(&tiles),
            u64::from_str_radix(fields[6], 16).unwrap(),
            "{line}"
        );
        for row in NATIVE.lines().filter(|row| row.starts_with("sample\t")) {
            let sample: Vec<_> = row.split('\t').collect();
            if sample[1] != case_id {
                continue;
            }
            assert_eq!(sample.len(), 10);
            let plane = sample[2].parse().unwrap();
            let x = sample[3].parse().unwrap();
            let y = sample[4].parse().unwrap();
            let expected = std::array::from_fn(|index| sample[index + 5].parse::<u8>().unwrap());
            assert_eq!(
                attributes(tiles.tile(plane, x, y).unwrap()),
                expected,
                "{row}"
            );
            assert_eq!(
                tiles.tiles()[(plane * size + x) * size + y].as_ref(),
                tiles.tile(plane, x, y)
            );
            samples += 1;
        }
        cases += 1;
        hashed_tiles += tiles.tiles().len();
    }
    assert_eq!(cases, 130);
    assert_eq!(samples, 10400);
    assert_eq!(hashed_tiles, 8_020_480);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn fixture_pins_native_provenance_full_size_coverage_and_attribute_edge_values() {
    let mut digests = 0;
    let mut captures = 0;
    let mut sizes = Vec::new();
    let mut cases = Vec::new();
    let mut values = [[false; 256]; 5];
    let mut zero_overlay_with_shape_rotation = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "jar-sha256" => {
                assert_eq!(fields, ["jar-sha256", JAR_SHA256]);
                digests += 1;
            }
            "capture" => {
                assert_eq!(
                    fields,
                    [
                        "capture",
                        "MapLoader.decodeTilesClient",
                        "pre-bridge",
                        "plane/x/y",
                        "underlay/overlay/render_flags/shape/rotation"
                    ]
                );
                captures += 1;
            }
            "map-size" => {
                assert_eq!(fields.len(), 3);
                assert_eq!(fields[1].parse::<usize>().unwrap(), sizes.len());
                sizes.push(fields[2].parse::<usize>().unwrap());
            }
            "case" => {
                assert_eq!(fields.len(), 7);
                assert_eq!(fields[1].parse::<usize>().unwrap(), cases.len());
                cases.push([
                    fields[2].parse::<usize>().unwrap(),
                    fields[3].parse().unwrap(),
                    fields[4].parse().unwrap(),
                    fields[5].parse().unwrap(),
                ]);
                assert_eq!(fields[6].len(), 16);
            }
            "sample" => {
                assert_eq!(fields.len(), 10);
                let attributes: [usize; 5] =
                    std::array::from_fn(|index| fields[index + 5].parse().unwrap());
                for (index, value) in attributes.into_iter().enumerate() {
                    values[index][value] = true;
                }
                if attributes[1] == 0 && attributes[3] != 0 && attributes[4] != 0 {
                    zero_overlay_with_shape_rotation += 1;
                }
            }
            "region" => assert_eq!(fields.len(), 5),
            other => panic!("unexpected fixture record {other}"),
        }
    }
    assert_eq!((digests, captures), (1, 1));
    assert_eq!(sizes, SIZES);
    assert_eq!(cases.len(), 130);
    for size in SIZES {
        assert_eq!(cases.iter().filter(|case| case[0] == size).count(), 26);
        for dx in [0, 8, 24, 56] {
            for dy in [0, 8, 24, 56] {
                assert!(cases.contains(&[size, 3200 + dx, 3200 + dy, 0]));
            }
            for missing in [1, 2] {
                assert!(cases.contains(&[size, 3200 + dx, 3200 + (dx + 24) % 64, missing]));
            }
        }
        assert!(cases.contains(&[size, 0, 0, 0]));
        assert!(cases.contains(&[size, 16384 - size, 16384 - size, 0]));
    }
    for (observed, maximum) in values.iter().zip([174, 255, 32, 11, 3]) {
        assert!(observed[..=maximum].iter().all(|&seen| seen));
        assert!(observed[maximum + 1..].iter().all(|&seen| !seen));
    }
    assert!(zero_overlay_with_shape_rotation > 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn x_and_y_seams_take_next_archive_tile_zero_without_bridge_plane_remapping() {
    let layout = layout([3208, 3224], 168);
    let owned: Vec<_> = layout
        .region_ids()
        .iter()
        .map(|&id| (id, terrain(id)))
        .collect();
    let present: Vec<_> = owned.iter().map(|(id, terrain)| (*id, terrain)).collect();
    let heights = SurfaceHeights::compose(&layout, &present, &[]).unwrap();
    let tiles = SurfaceTiles::compose(&heights, &present).unwrap();
    let mut bridge_flag_samples = 0;
    for plane in 0..PLANES {
        for x in [0, 1, 55, 56, 57, 119, 120, 121, 167] {
            for y in [0, 1, 39, 40, 41, 103, 104, 105, 167] {
                let world_x = 3208 + x;
                let world_y = 3224 + y;
                let id = (((world_x / 64) << 8) + world_y / 64) as u16;
                let source = owned.iter().find(|(region, _)| *region == id).unwrap();
                let expected =
                    from_terrain(source.1.tile(plane, world_x % 64, world_y % 64).unwrap());
                assert_eq!(tiles.tile(plane, x, y), Some(&expected));
                if plane == 1 && expected.render_flags & 2 != 0 {
                    bridge_flag_samples += 1;
                    assert_eq!(
                        attributes(tiles.tile(plane, x, y).unwrap()),
                        attributes(&expected)
                    );
                }
            }
        }
    }
    assert!(bridge_flag_samples > 0);
    assert_eq!(heights.source_tile(55, 39).unwrap().region_tile, [63, 63]);
    assert_eq!(heights.source_tile(56, 40).unwrap().region_tile, [0, 0]);
    assert_eq!(heights.source_tile(57, 41).unwrap().region_tile, [1, 1]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn proven_absent_is_five_zero_attributes_but_unavailable_remains_none_on_every_size() {
    for size in SIZES {
        let layout = layout([3256, 3224], size);
        let absent = SurfaceHeights::compose(&layout, &[], layout.region_ids()).unwrap();
        let unavailable = SurfaceHeights::compose(&layout, &[], &[]).unwrap();
        let known = SurfaceTiles::compose(&absent, &[]).unwrap();
        let unknown = SurfaceTiles::compose(&unavailable, &[]).unwrap();
        assert_eq!(known.tiles().len(), PLANES * size * size);
        assert_eq!(unknown.tiles().len(), PLANES * size * size);
        assert!(known
            .tiles()
            .iter()
            .all(|tile| *tile == Some(SurfaceTile::default())));
        assert!(unknown.tiles().iter().all(Option::is_none));
        for plane in 0..PLANES {
            for x in 0..size {
                for y in 0..size {
                    assert_eq!(known.tile(plane, x, y), Some(&SurfaceTile::default()));
                    assert_eq!(unknown.tile(plane, x, y), None);
                }
            }
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn maximum_size_mixed_availability_is_per_region_and_independent_of_neighbor_heights() {
    let layout = layout([3256, 3256], 168);
    let owned: Vec<_> = layout
        .region_ids()
        .iter()
        .enumerate()
        .filter(|(index, _)| index % 3 == 0)
        .map(|(_, &id)| (id, terrain(id)))
        .collect();
    let present: Vec<_> = owned.iter().map(|(id, terrain)| (*id, terrain)).collect();
    let absent: Vec<_> = layout
        .region_ids()
        .iter()
        .enumerate()
        .filter(|(index, _)| index % 3 == 2)
        .map(|(_, &id)| id)
        .collect();
    let heights = SurfaceHeights::compose(&layout, &present, &absent).unwrap();
    let tiles = SurfaceTiles::compose(&heights, &present).unwrap();
    let mut counts = [0; 3];
    let mut absent_unknown_height = 0;
    for plane in 0..PLANES {
        for x in 0..168 {
            for y in 0..168 {
                let world_x = 3256 + x;
                let world_y = 3256 + y;
                let id = (((world_x / 64) << 8) + world_y / 64) as u16;
                let index = layout
                    .region_ids()
                    .iter()
                    .position(|&region| region == id)
                    .unwrap();
                match index % 3 {
                    0 => {
                        let terrain = owned.iter().find(|(region, _)| *region == id).unwrap();
                        let expected = from_terrain(
                            terrain.1.tile(plane, world_x % 64, world_y % 64).unwrap(),
                        );
                        assert_eq!(tiles.tile(plane, x, y), Some(&expected));
                    }
                    1 => assert_eq!(tiles.tile(plane, x, y), None),
                    2 => {
                        assert_eq!(tiles.tile(plane, x, y), Some(&SurfaceTile::default()));
                        if heights.height_native_units(plane, x, y).is_none() {
                            absent_unknown_height += 1;
                        }
                    }
                    _ => unreachable!(),
                }
                counts[index % 3] += 1;
            }
        }
    }
    assert!(counts.iter().all(|&count| count > 0));
    assert!(absent_unknown_height > 0);
    assert_eq!(counts.iter().sum::<usize>(), PLANES * 168 * 168);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn duplicate_unexpected_and_availability_mismatch_fail_without_mutating_any_input_or_prior_output()
{
    let layout = layout([3200, 3200], 104);
    let ids = layout.region_ids();
    let first = terrain(ids[0]);
    let alternate = terrain(ids[1]);
    let heights = SurfaceHeights::compose(&layout, &[(ids[0], &first)], &[ids[1]]).unwrap();
    let previous = SurfaceTiles::compose(&heights, &[(ids[0], &first)]).unwrap();
    let layout_before = layout.clone();
    let heights_before = heights.clone();
    let first_before = first.clone();
    let alternate_before = alternate.clone();
    let previous_before = previous.clone();
    let cases = [
        (vec![(0, &first)], SurfaceTilesError::UnexpectedRegion(0)),
        (
            vec![(ids[0], &first), (0, &alternate)],
            SurfaceTilesError::UnexpectedRegion(0),
        ),
        (
            vec![(ids[0], &first), (ids[0], &alternate)],
            SurfaceTilesError::DuplicateRegion(ids[0]),
        ),
        (vec![], SurfaceTilesError::AvailabilityMismatch(ids[0])),
        (
            vec![(ids[0], &first), (ids[1], &alternate)],
            SurfaceTilesError::AvailabilityMismatch(ids[1]),
        ),
        (
            vec![(ids[0], &first), (ids[2], &alternate)],
            SurfaceTilesError::AvailabilityMismatch(ids[2]),
        ),
    ];
    for (present, expected) in cases {
        let error = SurfaceTiles::compose(&heights, &present).unwrap_err();
        assert_eq!(error, expected);
        assert!(error.to_string().contains(&match expected {
            SurfaceTilesError::UnexpectedRegion(id)
            | SurfaceTilesError::DuplicateRegion(id)
            | SurfaceTilesError::AvailabilityMismatch(id) => id.to_string(),
        }));
        assert_eq!(layout, layout_before);
        assert_eq!(heights, heights_before);
        assert_eq!(first, first_before);
        assert_eq!(alternate, alternate_before);
        assert_eq!(previous, previous_before);
        assert_eq!(
            SurfaceTiles::compose(&heights, &[(ids[0], &first)]).unwrap(),
            previous
        );
    }
    assert_eq!(
        heights.region_availability()[1].1,
        RegionAvailability::ProvenAbsent
    );
    assert_eq!(
        heights.region_availability()[2].1,
        RegionAvailability::Unavailable
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn complete_maximum_size_composition_is_order_independent_and_preserves_source_snapshots() {
    let layout = layout([3256, 3256], 168);
    let owned: Vec<_> = layout
        .region_ids()
        .iter()
        .map(|&id| (id, terrain(id)))
        .collect();
    let before = owned.clone();
    let mut present: Vec<_> = owned.iter().map(|(id, terrain)| (*id, terrain)).collect();
    let heights = SurfaceHeights::compose(&layout, &present, &[]).unwrap();
    let heights_before = heights.clone();
    let first = SurfaceTiles::compose(&heights, &present).unwrap();
    let first_before = first.clone();
    present.reverse();
    let reversed = SurfaceTiles::compose(&heights, &present).unwrap();
    assert_eq!(first, reversed);
    assert_eq!(first, first_before);
    assert_eq!(heights, heights_before);
    assert_eq!(owned, before);
    assert!(first.tiles().iter().all(Option::is_some));
    assert_eq!(first.tiles().len(), PLANES * 168 * 168);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounds_reject_sentinel_and_extreme_indices_before_any_index_arithmetic() {
    for size in SIZES {
        for base in [[0, 0], [16384 - size as i32, 16384 - size as i32]] {
            let layout = layout(base, size);
            let heights = SurfaceHeights::compose(&layout, &[], layout.region_ids()).unwrap();
            let tiles = SurfaceTiles::compose(&heights, &[]).unwrap();
            assert_eq!(
                tiles.tile(3, size - 1, size - 1),
                Some(&SurfaceTile::default())
            );
            for [plane, x, y] in [
                [4, 0, 0],
                [usize::MAX, 0, 0],
                [0, size, 0],
                [0, 0, size],
                [0, size + 1, 0],
                [0, 0, size + 1],
                [0, usize::MAX, 0],
                [0, 0, usize::MAX],
                [usize::MAX; 3],
            ] {
                assert_eq!(tiles.tile(plane, x, y), None);
            }
        }
    }
    for base_tile in [
        [i32::MAX - 7, 0],
        [0, i32::MAX - 7],
        [i32::MIN, 0],
        [0, i32::MIN],
    ] {
        assert_eq!(
            StandardMapLayout::new(base_tile, NativeMapSize::Tiles168, &[]),
            Err(SurfaceError::UnsupportedWorldBounds { base_tile })
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_partial_availability_contract_preserves_known_tiles_under_both_completions() {
    let layout = layout([3200, 3200], 72);
    let owned: Vec<_> = layout
        .region_ids()
        .iter()
        .map(|&id| (id, terrain(id)))
        .collect();
    assert_eq!(owned.len(), 4);
    for pattern in 0..81 {
        let mut states = pattern;
        let mut present = Vec::new();
        let mut absent = Vec::new();
        let mut unavailable = Vec::new();
        for (id, terrain) in &owned {
            match states % 3 {
                0 => unavailable.push((*id, terrain)),
                1 => present.push((*id, terrain)),
                2 => absent.push(*id),
                _ => unreachable!(),
            }
            states /= 3;
        }
        let heights = SurfaceHeights::compose(&layout, &present, &absent).unwrap();
        let partial = SurfaceTiles::compose(&heights, &present).unwrap();
        for complete_with_present in [false, true] {
            let mut completed_present = present.clone();
            let mut completed_absent = absent.clone();
            for &(id, terrain) in &unavailable {
                if complete_with_present {
                    completed_present.push((id, terrain));
                } else {
                    completed_absent.push(id);
                }
            }
            let completed_heights =
                SurfaceHeights::compose(&layout, &completed_present, &completed_absent).unwrap();
            let completed = SurfaceTiles::compose(&completed_heights, &completed_present).unwrap();
            assert!(completed.tiles().iter().all(Option::is_some));
            for (index, (known, filled)) in
                partial.tiles().iter().zip(completed.tiles()).enumerate()
            {
                if known.is_some() {
                    assert_eq!(
                        known, filled,
                        "pattern {pattern} completion {complete_with_present} tile {index}"
                    );
                }
            }
        }
    }
}
