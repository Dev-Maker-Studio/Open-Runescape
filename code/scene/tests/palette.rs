use openrs_scene::floor::{pack_hsl, Underlay};
use openrs_scene::palette::{
    calculate_underlay_palette, PaletteCell, PaletteError, UnderlayLookup, UnderlayPalette,
};
use openrs_scene::surface::NativeMapSize;
use openrs_scene::terrain::PLANES;

const NATIVE: &str = include_str!("fixtures/palette.tsv");
const SIZE: NativeMapSize = NativeMapSize::Tiles72;

fn index(size: usize, plane: usize, x: usize, y: usize) -> usize {
    (plane * size + x) * size + y
}

fn ids(size: NativeMapSize) -> Vec<Option<u8>> {
    vec![Some(0); PLANES * size.tiles().pow(2)]
}

fn example() -> Underlay {
    Underlay {
        hue: 10,
        saturation: 100,
        lightness: 80,
        hue_multiplier: 20,
        ..Underlay::default()
    }
}

fn fixture_definitions() -> Vec<Option<Underlay>> {
    let mut result = vec![None; 255];
    for line in NATIVE
        .lines()
        .filter(|line| line.starts_with("definition\t"))
    {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 8);
        let id: usize = fields[1].parse().unwrap();
        let expected = [
            fields[4].parse::<i32>().unwrap(),
            fields[5].parse().unwrap(),
            fields[6].parse().unwrap(),
            fields[7].parse().unwrap(),
        ];
        let underlay = match fields[2] {
            "rgb" => {
                let rgb = u32::from_str_radix(fields[3], 16).unwrap();
                Underlay::decode(&[1, (rgb >> 16) as u8, (rgb >> 8) as u8, rgb as u8, 0]).unwrap()
            }
            "custom" => Underlay {
                hue: expected[0],
                saturation: expected[1],
                lightness: expected[2],
                hue_multiplier: expected[3],
                ..Underlay::default()
            },
            "absent" => Underlay::default(),
            other => panic!("unexpected native definition {other}"),
        };
        assert_eq!(
            [
                underlay.hue,
                underlay.saturation,
                underlay.lightness,
                underlay.hue_multiplier,
            ],
            expected,
            "{line}",
        );
        if fields[2] != "absent" {
            result[id] = Some(underlay);
        }
    }
    result
}

fn tile_id(pattern: &str, plane: usize, x: usize, y: usize, size: usize) -> u8 {
    match pattern {
        "empty" => 0,
        "uniform" => 1,
        "checker" => {
            let value = (plane * 53 + x * 17 + y * 31) % 19;
            if value == 18 {
                255
            } else {
                value as u8
            }
        }
        "sparse" => {
            let point = x == y && [0, 4, 5, 9, 10, 31, 63, 64, size - 1].contains(&x)
                || x == 0 && y == size - 1
                || x == size - 1 && y == 0;
            if point && plane < 3 {
                (1 + (plane * 3 + x + y) % 16) as u8
            } else {
                0
            }
        }
        "default" => {
            if (x + y + plane).is_multiple_of(2) {
                1
            } else {
                17
            }
        }
        "all-default" => 17,
        "carry" => match plane {
            0 => (1 + (x * 7 + y * 11) % 16) as u8,
            1 => 0,
            2 => 17,
            _ => {
                if x < size / 2 && y < size / 2 {
                    5
                } else {
                    0
                }
            }
        },
        "overflow" => {
            if (plane + x + y).is_multiple_of(13) {
                0
            } else {
                (21 + (plane * 7 + x * 11 + y * 3) % 6) as u8
            }
        }
        "divide" => {
            if (x + y + plane).is_multiple_of(3) {
                0
            } else if (x + 2 * y).is_multiple_of(2) {
                41
            } else {
                1
            }
        }
        "edge" => {
            if x == 20 + plane * 5 && y == 20 {
                2
            } else if x == 20 && y == 20 + plane * 5 {
                5
            } else {
                0
            }
        }
        "bytexff" => 255,
        "thresholds" => (31 + (plane + x / 12 + y / 12) % 8) as u8,
        other => panic!("unexpected native pattern {other}"),
    }
}

fn plane_hash(palette: &UnderlayPalette, plane: usize) -> u64 {
    let size = palette.map_size().tiles();
    let mut hash = 0xcbf29ce484222325_u64;
    for cell in &palette.cells()[plane * size * size..(plane + 1) * size * size] {
        let PaletteCell::Known(value) = cell else {
            panic!("complete native oracle has an unavailable palette cell");
        };
        for byte in value.to_le_bytes() {
            hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
        }
    }
    hash
}

#[test]
fn copied_native_palette_matches_every_cell_on_five_sizes_and_twelve_patterns() {
    let definitions = fixture_definitions();
    let mut cases = 0;
    let mut hashes = 0;
    let mut samples = 0;
    let mut compared_cells = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("case\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 4);
        let case_id = fields[1];
        let size = NativeMapSize::from_tiles(fields[2].parse().unwrap()).unwrap();
        let mut input = ids(size);
        for plane in 0..PLANES {
            for x in 0..size.tiles() {
                for y in 0..size.tiles() {
                    input[index(size.tiles(), plane, x, y)] =
                        Some(tile_id(fields[3], plane, x, y, size.tiles()));
                }
            }
        }
        let palette = calculate_underlay_palette(size, &input, |id| {
            match definitions[usize::from(id)].as_ref() {
                Some(underlay) => UnderlayLookup::Present(underlay),
                None if id == 16 => UnderlayLookup::ProvenAbsent,
                None => panic!("fixture omitted definition {id}"),
            }
        })
        .unwrap();
        assert_eq!(palette.map_size(), size);
        assert_eq!(palette.cells().len(), input.len());
        assert!(palette.unavailable_definition_ids().is_empty());
        for row in NATIVE.lines() {
            let fields: Vec<_> = row.split('\t').collect();
            if !matches!(fields[0], "plane-hash" | "sample") || fields[1] != case_id {
                continue;
            }
            if fields[0] == "plane-hash" {
                assert_eq!(fields.len(), 4);
                let plane = fields[2].parse().unwrap();
                assert_eq!(
                    plane_hash(&palette, plane),
                    u64::from_str_radix(fields[3], 16).unwrap(),
                    "{row} in {line}",
                );
                hashes += 1;
                compared_cells += size.tiles().pow(2);
            } else {
                assert_eq!(fields.len(), 6);
                assert_eq!(
                    palette.cell(
                        fields[2].parse().unwrap(),
                        fields[3].parse().unwrap(),
                        fields[4].parse().unwrap(),
                    ),
                    Some(PaletteCell::Known(fields[5].parse().unwrap())),
                    "{row} in {line}",
                );
                samples += 1;
            }
        }
        cases += 1;
    }
    assert_eq!(cases, 60);
    assert_eq!(hashes, 240);
    assert_eq!(samples, 23040);
    assert_eq!(compared_cells, 3_701_760);
}

#[test]
fn fixture_pins_native_method_dependencies_downstream_capture_seam_and_jar() {
    let mut sizes = Vec::new();
    let mut classes = Vec::new();
    let mut jar = 0;
    let mut method = 0;
    let mut seam = 0;
    let mut definitions = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "jar-sha256" => {
                assert_eq!(
                    fields[1],
                    "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
                );
                jar += 1;
            }
            "palette-method-sha256" => {
                assert_eq!(
                    fields[1],
                    "3543825198d091b2dabff021da1b107b57b327f8769d7377a5a9bf019dfc51f3"
                );
                method += 1;
            }
            "class-sha256" => classes.push((fields[1], fields[2])),
            "capture-seam" => {
                assert_eq!(fields.len(), 3);
                assert_eq!(fields[1], "addRegularTile");
                assert_eq!(fields[2], "(Lcom/jagex/game/runetek5/scene/ground/Ground;I[[ILcom/jagex/game/runetek5/scene/ground/Ground;Lcom/jagex/game/runetek5/scene/ground/Ground;)V");
                seam += 1;
            }
            "map-size" => {
                assert_eq!(fields[1].parse::<usize>().unwrap(), sizes.len());
                sizes.push(fields[2].parse::<usize>().unwrap());
            }
            "definition" => definitions += 1,
            "case" | "plane-hash" | "sample" => {}
            other => panic!("unexpected native palette fixture row {other}"),
        }
    }
    assert_eq!((jar, method, seam, definitions), (1, 1, 1, 33));
    assert_eq!(sizes, [104, 120, 136, 168, 72]);
    assert_eq!(
        classes,
        [
            (
                "com.jagex.game.runetek5.scene.MapLoader",
                "528908706fefda8369f28f7ea4b729e89a83f36ad1d7ebadff2050b17e01c161"
            ),
            (
                "com.jagex.game.runetek5.config.flu.FluType",
                "3cb76b0d69d8f432c0eddad7b46f123cc36cc368f4df13953f91213d679d68e0"
            ),
            (
                "com.jagex.game.runetek5.config.flu.FluTypeList",
                "23954dfe970bc1f8adf6916e4793d01343912e4d3562480c3b475d3e497cdac6"
            ),
            (
                "com.jagex.game.runetek5.utils.ColorUtil",
                "89cd0840b5db7b3bbfa309eea868631b2a948be5ae2883aee786c7c4ba6e27f1"
            ),
        ]
    );
}

#[test]
fn native_window_is_asymmetric_ten_by_ten_and_empty_planes_retain_prior_palette() {
    let mut input = ids(SIZE);
    input[index(SIZE.tiles(), 0, 20, 20)] = Some(1);
    let definition = example();
    let palette =
        calculate_underlay_palette(SIZE, &input, |_| UnderlayLookup::Present(&definition)).unwrap();
    let color = pack_hsl(128, 100, 80);
    for plane in 0..PLANES {
        for x in 0..SIZE.tiles() {
            for y in 0..SIZE.tiles() {
                let expected = if (15..=24).contains(&x) && (15..=24).contains(&y) {
                    color
                } else {
                    0
                };
                assert_eq!(
                    palette.cell(plane, x, y),
                    Some(PaletteCell::Known(expected))
                );
            }
        }
    }
}

#[test]
fn proven_missing_definition_counts_in_average_but_has_zero_hue_multiplier() {
    let mut input = ids(SIZE);
    input[index(SIZE.tiles(), 0, 20, 20)] = Some(1);
    input[index(SIZE.tiles(), 0, 21, 20)] = Some(2);
    let definition = example();
    let palette = calculate_underlay_palette(SIZE, &input, |id| match id {
        0 => UnderlayLookup::Present(&definition),
        1 => UnderlayLookup::ProvenAbsent,
        _ => panic!("unexpected definition"),
    })
    .unwrap();
    assert_eq!(
        palette.cell(0, 20, 20),
        Some(PaletteCell::Known(pack_hsl(128, 50, 40)))
    );
    assert_eq!(
        palette.cell(0, 15, 20),
        Some(PaletteCell::Known(pack_hsl(128, 100, 80)))
    );
    assert_eq!(palette.cell(0, 25, 20), Some(PaletteCell::Known(0)));
    assert!(palette.unavailable_definition_ids().is_empty());
}

#[test]
fn unavailable_definition_has_exact_window_footprint_and_unknown_plane_carry() {
    let mut input = ids(SIZE);
    input[index(SIZE.tiles(), 0, 20, 20)] = Some(255);
    let mut requested = Vec::new();
    let palette = calculate_underlay_palette(SIZE, &input, |id| {
        requested.push(id);
        UnderlayLookup::Unavailable
    })
    .unwrap();
    assert_eq!(requested, [254]);
    assert_eq!(palette.unavailable_definition_ids(), [254]);
    for plane in 0..PLANES {
        for x in 0..SIZE.tiles() {
            for y in 0..SIZE.tiles() {
                let expected = if (15..=24).contains(&x) && (15..=24).contains(&y) {
                    PaletteCell::Unavailable
                } else {
                    PaletteCell::Known(0)
                };
                assert_eq!(palette.cell(plane, x, y), Some(expected));
            }
        }
    }
}

#[test]
fn unavailable_map_tile_is_not_treated_as_known_zero_and_can_be_overwritten() {
    let mut input = ids(SIZE);
    input[index(SIZE.tiles(), 0, 20, 20)] = None;
    let plane_len = SIZE.tiles().pow(2);
    input[2 * plane_len..3 * plane_len].fill(Some(1));
    let definition = example();
    let palette =
        calculate_underlay_palette(SIZE, &input, |_| UnderlayLookup::Present(&definition)).unwrap();
    assert!(palette.unavailable_definition_ids().is_empty());
    for plane in 0..PLANES {
        for x in 0..SIZE.tiles() {
            for y in 0..SIZE.tiles() {
                let expected = if plane >= 2 {
                    PaletteCell::Known(pack_hsl(128, 100, 80))
                } else if (15..=24).contains(&x) && (15..=24).contains(&y) {
                    PaletteCell::Unavailable
                } else {
                    PaletteCell::Known(0)
                };
                assert_eq!(palette.cell(plane, x, y), Some(expected));
            }
        }
    }
}

#[test]
fn unknown_contributions_leave_both_sliding_axes_and_do_not_poison_other_cells() {
    let mut input = vec![Some(1); PLANES * SIZE.tiles().pow(2)];
    input[index(SIZE.tiles(), 1, 20, 20)] = None;
    let definition = example();
    let palette =
        calculate_underlay_palette(SIZE, &input, |_| UnderlayLookup::Present(&definition)).unwrap();
    let plane_len = SIZE.tiles().pow(2);
    for plane in 0..PLANES {
        let unavailable = palette.cells()[plane * plane_len..(plane + 1) * plane_len]
            .iter()
            .filter(|&&cell| cell == PaletteCell::Unavailable)
            .count();
        assert_eq!(unavailable, if plane == 1 { 100 } else { 0 });
    }
    assert_eq!(
        palette.cell(1, 25, 20),
        Some(PaletteCell::Known(pack_hsl(128, 100, 80)))
    );
    assert_eq!(
        palette.cell(1, 20, 25),
        Some(PaletteCell::Known(pack_hsl(128, 100, 80)))
    );
}

#[test]
fn negative_division_and_hue_multiplication_use_native_signed_i32_arithmetic() {
    for definition in [
        Underlay {
            hue: -7,
            saturation: -13,
            lightness: 27,
            hue_multiplier: 11,
            ..Underlay::default()
        },
        Underlay {
            hue: i32::MAX,
            saturation: i32::MIN,
            lightness: i32::MAX,
            hue_multiplier: 1,
            ..Underlay::default()
        },
    ] {
        let mut input = ids(SIZE);
        input[index(SIZE.tiles(), 0, 20, 20)] = Some(1);
        let palette =
            calculate_underlay_palette(SIZE, &input, |_| UnderlayLookup::Present(&definition))
                .unwrap();
        assert_eq!(
            palette.cell(0, 20, 20),
            Some(PaletteCell::Known(pack_hsl(
                definition.hue.wrapping_mul(256) / definition.hue_multiplier,
                definition.saturation,
                definition.lightness,
            )))
        );
    }
}

#[test]
fn missing_and_known_empty_maps_remain_distinct_without_requesting_definitions() {
    for (input, expected) in [
        (ids(SIZE), PaletteCell::Known(0)),
        (
            vec![None; PLANES * SIZE.tiles().pow(2)],
            PaletteCell::Unavailable,
        ),
    ] {
        let palette =
            calculate_underlay_palette(SIZE, &input, |_| panic!("no known underlay IDs")).unwrap();
        assert!(palette.cells().iter().all(|&cell| cell == expected));
        assert!(palette.unavailable_definition_ids().is_empty());
    }
}

#[test]
fn lookup_is_zero_based_and_called_once_per_used_unsigned_map_id() {
    let mut input = vec![Some(255); PLANES * SIZE.tiles().pow(2)];
    input[index(SIZE.tiles(), 0, 0, 0)] = Some(0);
    input[index(SIZE.tiles(), 0, 0, 1)] = Some(8);
    input[index(SIZE.tiles(), 0, 0, 2)] = Some(3);
    let mut requested = Vec::new();
    let palette = calculate_underlay_palette(SIZE, &input, |id| {
        requested.push(id);
        UnderlayLookup::Unavailable
    })
    .unwrap();
    assert_eq!(requested, [7, 2, 254]);
    assert_eq!(palette.unavailable_definition_ids(), [2, 7, 254]);
}

#[test]
fn exact_bounded_native_layout_is_required_and_accessors_reject_outside_tiles() {
    let expected = PLANES * SIZE.tiles().pow(2);
    for actual in [0, expected - 1, expected + 1] {
        let error = calculate_underlay_palette(SIZE, &vec![Some(0); actual], |_| {
            panic!("invalid layout must not resolve definitions")
        })
        .unwrap_err();
        assert_eq!(error, PaletteError::UnderlayCount { expected, actual });
        assert!(error.to_string().contains("native palette requires"));
    }
    let palette = calculate_underlay_palette(SIZE, &ids(SIZE), |_| panic!("empty map")).unwrap();
    assert_eq!(palette.cell(PLANES, 0, 0), None);
    assert_eq!(palette.cell(0, SIZE.tiles(), 0), None);
    assert_eq!(palette.cell(0, 0, SIZE.tiles()), None);
    assert_eq!(palette.cell(usize::MAX, usize::MAX, usize::MAX), None);
    assert_eq!(palette.cell(3, 71, 71), Some(PaletteCell::Known(0)));
}
