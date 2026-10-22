use openrs_scene::surface::{
    NativeMapSize, RegionAvailability, StandardMapLayout, SurfaceError, SurfaceHeights,
};
use openrs_scene::terrain::{DecodeLimits, Terrain, MAP_SIZE, PLANES, TILE_COUNT};

const NATIVE: &str = include_str!("fixtures/surface.tsv");
const REGION_IDS: [u16; 4] = [12850, 12851, 13106, 13107];

fn decode(bytes: &[u8]) -> Terrain {
    Terrain::decode(
        bytes,
        DecodeLimits {
            max_input_bytes: bytes.len(),
            max_tags_per_tile: 1,
        },
    )
    .unwrap()
}

fn constant_terrain(raw: [u8; PLANES]) -> Terrain {
    let mut bytes = Vec::new();
    for value in raw {
        for _ in 0..MAP_SIZE * MAP_SIZE {
            bytes.extend_from_slice(&[1, value]);
        }
    }
    decode(&bytes)
}

fn archive_bytes(region_id: u16, patterned: bool) -> Vec<u8> {
    let mut bytes = Vec::new();
    for i in 0..TILE_COUNT {
        if !patterned || (i + usize::from(region_id)) % 3 == 0 {
            bytes.push(0);
        } else {
            bytes.extend_from_slice(&[1, ((usize::from(region_id) * 17 + i * 73) & 255) as u8]);
        }
    }
    bytes
}

fn layout() -> StandardMapLayout {
    StandardMapLayout::new([3200, 3200], NativeMapSize::Tiles104, &REGION_IDS).unwrap()
}

fn height_hash(surface: &SurfaceHeights) -> u64 {
    let mut hash = 0xcbf29ce484222325_u64;
    for height in surface.heights_native_units() {
        for byte in height
            .expect("complete native oracle must have no unavailable heights")
            .to_le_bytes()
        {
            hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
        }
    }
    hash
}

#[test]
fn copied_native_jar_matches_all_scene_cells_on_five_sizes_clipping_absence_and_planes() {
    let mut cases = 0;
    let mut samples = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("case\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 8);
        let case_id = fields[1];
        let size = NativeMapSize::from_tiles(fields[2].parse().unwrap()).unwrap();
        let base = [fields[3].parse().unwrap(), fields[4].parse().unwrap()];
        let patterned = match fields[6] {
            "zero" => false,
            "patterned" => true,
            other => panic!("unexpected payload {other}"),
        };
        let mut ids = Vec::new();
        let mut present = Vec::new();
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
                "present" => present.push((id, decode(&archive_bytes(id, patterned)))),
                "absent" => absent.push(id),
                other => panic!("unexpected availability {other}"),
            }
        }
        let layout = StandardMapLayout::new(base, size, &ids).unwrap();
        let present: Vec<_> = present.iter().map(|(id, terrain)| (*id, terrain)).collect();
        let surface = SurfaceHeights::compose(&layout, &present, &absent).unwrap();
        assert_eq!(
            surface.heights_native_units().len(),
            PLANES * (size.tiles() + 1).pow(2)
        );
        assert_eq!(
            height_hash(&surface),
            u64::from_str_radix(fields[7], 16).unwrap(),
            "{line}"
        );
        for row in NATIVE.lines().filter(|row| row.starts_with("sample\t")) {
            let sample: Vec<_> = row.split('\t').collect();
            if sample[1] != case_id {
                continue;
            }
            assert_eq!(sample.len(), 6);
            assert_eq!(
                surface.height_native_units(
                    sample[2].parse().unwrap(),
                    sample[3].parse().unwrap(),
                    sample[4].parse().unwrap(),
                ),
                Some(sample[5].parse().unwrap()),
                "{row} in {line}",
            );
            samples += 1;
        }
        cases += 1;
    }
    assert_eq!(cases, 80);
    assert_eq!(samples, 6400);
}

#[test]
fn native_fixture_pins_unchanged_jar_sizes_and_record_types() {
    let mut digests = 0;
    let mut sizes = Vec::new();
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "jar-sha256" => {
                assert_eq!(fields.len(), 2);
                assert_eq!(
                    fields[1],
                    "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
                );
                digests += 1;
            }
            "map-size" => {
                assert_eq!(fields.len(), 3);
                assert_eq!(fields[1].parse::<usize>().unwrap(), sizes.len());
                sizes.push(fields[2].parse::<usize>().unwrap());
            }
            "case" | "region" | "sample" => {}
            other => panic!("unexpected native fixture record {other}"),
        }
    }
    assert_eq!(digests, 1);
    assert_eq!(sizes, [104, 120, 136, 168, 72]);
}

#[test]
fn region_boundary_64_is_next_archive_tile_zero_not_repeated_tile_63() {
    let west = constant_terrain([7; PLANES]);
    let east = constant_terrain([12; PLANES]);
    let scene = SurfaceHeights::compose(
        &layout(),
        &[(REGION_IDS[2], &east), (REGION_IDS[0], &west)],
        &[],
    )
    .unwrap();
    for plane in 0..PLANES {
        assert_eq!(
            scene.height_native_units(plane, 63, 0),
            Some(-224 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 64, 0),
            Some(-384 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 65, 0),
            Some(-384 * (plane as i32 + 1))
        );
        for coordinate in 0..=104 {
            assert_eq!(scene.height_native_units(plane, 104, coordinate), Some(0));
            assert_eq!(scene.height_native_units(plane, coordinate, 104), Some(0));
        }
    }
    assert_eq!(scene.height_native_units(0, 0, 64), None);
    assert_eq!(scene.source_tile(63, 0).unwrap().region_tile, [63, 0]);
    assert_eq!(scene.source_tile(64, 0).unwrap().region_tile, [0, 0]);
    assert_eq!(scene.source_tile(65, 0).unwrap().region_tile, [1, 0]);
    assert_eq!(scene.source_tile(64, 0).unwrap().world_tile, [3264, 3200]);
    assert_eq!(scene.source_tile(64, 0).unwrap().region_id, REGION_IDS[2]);
    assert_eq!(scene.source_tile(104, 0), None);
    assert_eq!(scene.source_tile(0, 104), None);
}

#[test]
fn scene_origin_is_not_used_as_procedural_archive_origin() {
    let terrain = decode(&vec![0; TILE_COUNT]);
    let layout =
        StandardMapLayout::new([3208, 3224], NativeMapSize::Tiles104, &REGION_IDS).unwrap();
    let scene = SurfaceHeights::compose(&layout, &[(REGION_IDS[0], &terrain)], &[]).unwrap();
    for plane in 0..PLANES {
        for [scene_x, scene_y] in [[0, 0], [1, 1], [55, 39]] {
            assert_eq!(
                scene.height_native_units(plane, scene_x, scene_y),
                terrain.surface_height_native_units([3200, 3200], plane, scene_x + 8, scene_y + 24),
            );
        }
    }
    assert_eq!(scene.height_native_units(0, 56, 0), None);
    assert_eq!(scene.source_tile(0, 0).unwrap().region_tile, [8, 24]);
    assert_eq!(scene.source_tile(0, 0).unwrap().world_tile, [3208, 3224]);
    assert_eq!(scene.source_tile(56, 40).unwrap().region_id, REGION_IDS[3]);
}

#[test]
fn explicit_zero_and_one_keep_lower_plane_and_all_four_planes_remain_distinct() {
    let terrain = constant_terrain([7, 1, 0, 255]);
    let scene = SurfaceHeights::compose(&layout(), &[(REGION_IDS[0], &terrain)], &[]).unwrap();
    for (plane, expected) in [-224, -224, -224, -8384].into_iter().enumerate() {
        assert_eq!(scene.height_native_units(plane, 0, 0), Some(expected));
    }
    assert_eq!(scene.height_native_units(4, 0, 0), None);
    assert_eq!(scene.height_native_units(usize::MAX, 0, 0), None);
    assert_eq!(scene.height_native_units(0, usize::MAX, 0), None);
    assert_eq!(scene.height_native_units(0, 0, usize::MAX), None);
}

#[test]
fn proven_absence_clears_in_second_pass_with_west_then_south_corner_precedence() {
    let west = constant_terrain([7; PLANES]);
    let south = constant_terrain([12; PLANES]);
    let scene = SurfaceHeights::compose(
        &layout(),
        &[(REGION_IDS[1], &west), (REGION_IDS[2], &south)],
        &[REGION_IDS[3]],
    )
    .unwrap();
    for plane in 0..PLANES {
        assert_eq!(
            scene.height_native_units(plane, 64, 64),
            Some(-224 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 64, 65),
            Some(-224 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 65, 64),
            Some(-384 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 65, 65),
            Some(-960 * plane as i32)
        );
    }
    assert_eq!(
        scene.region_availability(),
        &[
            (REGION_IDS[0], RegionAvailability::Unavailable),
            (REGION_IDS[1], RegionAvailability::Present),
            (REGION_IDS[2], RegionAvailability::Present),
            (REGION_IDS[3], RegionAvailability::ProvenAbsent),
        ]
    );
}

#[test]
fn unknown_neighbor_dependencies_stay_unavailable_instead_of_falling_through_to_known_south() {
    let south = constant_terrain([12; PLANES]);
    let scene =
        SurfaceHeights::compose(&layout(), &[(REGION_IDS[2], &south)], &[REGION_IDS[3]]).unwrap();
    for plane in 0..PLANES {
        assert_eq!(scene.height_native_units(plane, 64, 64), None);
        assert_eq!(scene.height_native_units(plane, 64, 65), None);
        assert_eq!(
            scene.height_native_units(plane, 65, 64),
            Some(-384 * (plane as i32 + 1))
        );
        assert_eq!(
            scene.height_native_units(plane, 65, 65),
            Some(-960 * plane as i32)
        );
        assert_eq!(scene.height_native_units(plane, 0, 0), None);
    }
}

#[test]
fn absent_inputs_are_order_independent_but_clear_execution_is_native_region_order() {
    let layout = layout();
    let forward = SurfaceHeights::compose(&layout, &[], &REGION_IDS).unwrap();
    let mut reverse = REGION_IDS;
    reverse.reverse();
    let backward = SurfaceHeights::compose(&layout, &[], &reverse).unwrap();
    assert_eq!(forward, backward);
    for plane in 0..PLANES {
        assert_eq!(
            forward.height_native_units(plane, 0, 0),
            Some(-960 * plane as i32)
        );
    }
}

#[test]
fn unclassified_regions_are_not_converted_to_native_absent_zeroes() {
    let scene = SurfaceHeights::compose(&layout(), &[], &[]).unwrap();
    assert_eq!(scene.height_native_units(0, 0, 0), None);
    assert_eq!(scene.height_native_units(3, 103, 103), None);
    assert_eq!(
        scene
            .heights_native_units()
            .iter()
            .filter(|height| height.is_none())
            .count(),
        PLANES * 104 * 104
    );
    assert!(scene
        .region_availability()
        .iter()
        .all(|entry| entry.1 == RegionAvailability::Unavailable));
    assert_eq!(scene.source_tile(0, 0).unwrap().region_id, REGION_IDS[0]);
}

#[test]
fn partial_known_values_agree_with_present_and_absent_completions() {
    let terrains = [
        constant_terrain([0, 1, 7, 12]),
        constant_terrain([7, 0, 12, 1]),
        constant_terrain([12, 7, 1, 0]),
        constant_terrain([1, 12, 0, 7]),
    ];
    let alternate = constant_terrain([255, 1, 0, 255]);
    let layout = layout();
    for pattern in 0..81 {
        let mut states = pattern;
        let mut present = Vec::new();
        let mut absent = Vec::new();
        let mut unknown = Vec::new();
        for (index, &id) in REGION_IDS.iter().enumerate() {
            match states % 3 {
                0 => unknown.push(id),
                1 => present.push((id, &terrains[index])),
                2 => absent.push(id),
                _ => unreachable!(),
            }
            states /= 3;
        }
        let partial = SurfaceHeights::compose(&layout, &present, &absent).unwrap();
        for completion in 0..3 {
            let mut completed_present = present.clone();
            let mut completed_absent = absent.clone();
            for &id in &unknown {
                if completion == 0 {
                    completed_absent.push(id);
                } else {
                    let terrain = if completion == 1 {
                        &terrains[0]
                    } else {
                        &alternate
                    };
                    completed_present.push((id, terrain));
                }
            }
            let complete =
                SurfaceHeights::compose(&layout, &completed_present, &completed_absent).unwrap();
            assert!(complete.heights_native_units().iter().all(Option::is_some));
            for (index, (&known, &filled)) in partial
                .heights_native_units()
                .iter()
                .zip(complete.heights_native_units())
                .enumerate()
            {
                if known.is_some() {
                    assert_eq!(
                        known, filled,
                        "pattern {pattern} completion {completion} index {index}"
                    );
                }
            }
        }
    }
}

#[test]
fn every_standard_chunk_alignment_covers_scene_tiles_without_a_65th_archive_sample() {
    for size in [72, 104, 120, 136, 168] {
        let map_size = NativeMapSize::from_tiles(size).unwrap();
        for dx in (0..64).step_by(8) {
            for dy in (0..64).step_by(8) {
                let base = [3200 + dx, 3200 + dy];
                let half = size as i32 >> 4;
                let center = base.map(|coordinate| coordinate / 8 + half);
                let mut ids = Vec::new();
                for x in (center[0] - half) / 8..=(center[0] + half) / 8 {
                    for y in (center[1] - half) / 8..=(center[1] + half) / 8 {
                        ids.push(((x << 8) + y) as u16);
                    }
                }
                let layout = StandardMapLayout::new(base, map_size, &ids).unwrap();
                assert_eq!(layout.center_chunk(), center);
                assert_eq!(layout.region_ids(), ids);
                assert_eq!(layout.base_tile(), base);
                assert_eq!(layout.map_size(), map_size);
                assert!(ids.len() <= 16);
                let scene = SurfaceHeights::compose(&layout, &[], &[]).unwrap();
                for x in 0..size {
                    for y in 0..size {
                        let source = scene.source_tile(x, y).unwrap();
                        assert!(ids.contains(&source.region_id));
                        assert!(source.region_tile.iter().all(|&coordinate| coordinate < 64));
                        assert_eq!(source.world_tile, [base[0] + x as i32, base[1] + y as i32]);
                    }
                }
            }
        }
    }
}

#[test]
fn invalid_sizes_bases_lists_and_ambiguous_region_classifications_are_rejected() {
    for tiles in [0, 64, 65, 105, usize::MAX] {
        assert_eq!(
            NativeMapSize::from_tiles(tiles),
            Err(SurfaceError::UnsupportedMapSize { tiles })
        );
    }
    assert_eq!(
        StandardMapLayout::new([3201, 3200], NativeMapSize::Tiles104, &REGION_IDS),
        Err(SurfaceError::UnalignedBase {
            base_tile: [3201, 3200]
        })
    );
    for base_tile in [
        [-8, 3200],
        [3200, -8],
        [16384, 3200],
        [i32::MAX - 7, 3200],
        [i32::MIN, 0],
    ] {
        assert_eq!(
            StandardMapLayout::new(base_tile, NativeMapSize::Tiles104, &REGION_IDS),
            Err(SurfaceError::UnsupportedWorldBounds { base_tile })
        );
    }
    assert_eq!(
        StandardMapLayout::new([3200, 3200], NativeMapSize::Tiles104, &[]),
        Err(SurfaceError::RegionCount {
            expected: 4,
            actual: 0
        })
    );
    assert_eq!(
        StandardMapLayout::new(
            [3200, 3200],
            NativeMapSize::Tiles104,
            &[12851, 12850, 13106, 13107]
        ),
        Err(SurfaceError::RegionOrder {
            index: 0,
            expected: 12850,
            actual: 12851
        })
    );
    let terrain = constant_terrain([7; PLANES]);
    assert_eq!(
        SurfaceHeights::compose(&layout(), &[(0, &terrain)], &[]),
        Err(SurfaceError::UnexpectedRegion { region_id: 0 })
    );
    assert_eq!(
        SurfaceHeights::compose(&layout(), &[], &[0]),
        Err(SurfaceError::UnexpectedRegion { region_id: 0 })
    );
    assert_eq!(
        SurfaceHeights::compose(&layout(), &[(REGION_IDS[0], &terrain); 2], &[]),
        Err(SurfaceError::DuplicateRegion {
            region_id: REGION_IDS[0]
        })
    );
    assert_eq!(
        SurfaceHeights::compose(&layout(), &[(REGION_IDS[0], &terrain)], &[REGION_IDS[0]]),
        Err(SurfaceError::DuplicateRegion {
            region_id: REGION_IDS[0]
        })
    );
    assert_eq!(
        SurfaceHeights::compose(&layout(), &[], &[REGION_IDS[0]; 2]),
        Err(SurfaceError::DuplicateRegion {
            region_id: REGION_IDS[0]
        })
    );
}
