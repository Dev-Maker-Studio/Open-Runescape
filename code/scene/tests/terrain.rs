use openrs_scene::terrain::{
    ByteField, DecodeError, DecodeErrorKind, DecodeLimits, HeightInstruction, Terrain, Tile,
    TilePosition, MAP_SIZE, PLANES, TILE_COUNT,
};

fn limits(size: usize, tags: usize) -> DecodeLimits {
    DecodeLimits {
        max_input_bytes: size,
        max_tags_per_tile: tags,
    }
}

fn index(plane: usize, x: usize, y: usize) -> usize {
    (plane * MAP_SIZE + x) * MAP_SIZE + y
}

fn archive_with(overrides: &[(usize, Vec<u8>)]) -> Vec<u8> {
    let mut bytes = Vec::new();
    for i in 0..TILE_COUNT {
        if let Some((_, payload)) = overrides.iter().find(|(position, _)| *position == i) {
            bytes.extend_from_slice(payload);
        } else {
            bytes.push(0);
        }
    }
    bytes
}

fn decode(bytes: &[u8]) -> Terrain {
    Terrain::decode(bytes, limits(bytes.len(), 16)).unwrap()
}

fn position(plane: usize, x: usize, y: usize) -> Option<TilePosition> {
    Some(TilePosition { plane, x, y })
}

#[test]
fn four_planes_have_exact_native_order_and_no_extra_border_tiles() {
    let samples = [
        (0, 0, 0),
        (0, 0, 1),
        (0, 1, 0),
        (0, 63, 63),
        (1, 0, 0),
        (1, 0, 1),
        (2, 63, 63),
        (3, 0, 0),
        (3, 63, 63),
    ];
    let overrides: Vec<_> = samples
        .iter()
        .enumerate()
        .map(|(i, &(p, x, y))| (index(p, x, y), vec![82 + i as u8, 1, 10 + i as u8]))
        .collect();
    let terrain = decode(&archive_with(&overrides));
    assert_eq!(terrain.tiles().len(), TILE_COUNT);
    for (i, &(p, x, y)) in samples.iter().enumerate() {
        let tile = terrain.tile(p, x, y).unwrap();
        assert_eq!(tile.underlay_id, 1 + i as u8);
        assert_eq!(tile.height, HeightInstruction::Explicit(10 + i as u8));
        assert_eq!(*tile, terrain.tiles()[index(p, x, y)]);
    }
    assert_eq!(*terrain.tile(0, 1, 1).unwrap(), Tile::default());
}

#[test]
fn all_tag_ranges_and_payload_byte_patterns_are_preserved() {
    let mut overrides = Vec::new();
    for tag in 2..=49_u8 {
        for raw in [0, 127, 128, 255] {
            overrides.push((overrides.len(), vec![tag, raw, 0]));
        }
    }
    for tag in 50..=255_u8 {
        overrides.push((overrides.len(), vec![tag, 0]));
    }
    let terrain = decode(&archive_with(&overrides));
    let mut i = 0;
    for tag in 2..=49_u8 {
        for raw in [0, 127, 128, 255] {
            assert_eq!(
                terrain.tiles()[i],
                Tile {
                    overlay_id: raw,
                    overlay_shape: (tag - 2) / 4,
                    overlay_rotation: (tag - 2) & 3,
                    ..Tile::default()
                }
            );
            i += 1;
        }
    }
    for tag in 50..=255_u8 {
        let expected = if tag <= 81 {
            Tile {
                render_flags: tag - 49,
                ..Tile::default()
            }
        } else {
            Tile {
                underlay_id: tag - 81,
                ..Tile::default()
            }
        };
        assert_eq!(terrain.tiles()[i], expected);
        i += 1;
    }
}

#[test]
fn repeated_attributes_replace_instead_of_accumulating() {
    let bytes = archive_with(&[(0, vec![81, 50, 255, 82, 49, 255, 2, 128, 0])]);
    assert_eq!(
        *decode(&bytes).tile(0, 0, 0).unwrap(),
        Tile {
            overlay_id: 128,
            underlay_id: 1,
            render_flags: 1,
            ..Tile::default()
        }
    );
    let bytes = archive_with(&[(0, vec![49, 255, 2, 0, 0])]);
    assert_eq!(*decode(&bytes).tile(0, 0, 0).unwrap(), Tile::default());
}

#[test]
fn explicit_height_tags_remain_raw_but_resolve_with_surface_rules() {
    let mut bytes = Vec::new();
    for _ in 0..PLANES {
        for i in 0..MAP_SIZE * MAP_SIZE {
            if i < 256 {
                bytes.extend_from_slice(&[1, i as u8]);
            } else {
                bytes.push(0);
            }
        }
    }
    let terrain = decode(&bytes);
    for raw in 0..=255_u8 {
        let x = usize::from(raw) / MAP_SIZE;
        let y = usize::from(raw) % MAP_SIZE;
        let delta = if raw == 1 { 0 } else { i32::from(raw) * 32 };
        for p in 0..PLANES {
            assert_eq!(
                terrain.tile(p, x, y).unwrap().height,
                HeightInstruction::Explicit(raw)
            );
            assert_eq!(
                terrain.surface_height_native_units([0, 0], p, x, y),
                Some(-delta * (p as i32 + 1))
            );
        }
    }
}

#[test]
fn procedural_upper_planes_use_previous_height_minus_960() {
    let terrain = decode(&archive_with(&[(0, vec![1, 10])]));
    for p in 0..PLANES {
        assert_eq!(
            terrain.surface_height_native_units([3200, 3200], p, 0, 0),
            Some(-320 - p as i32 * 960)
        );
        if p > 0 {
            assert_eq!(
                terrain.tile(p, 0, 0).unwrap().height,
                HeightInstruction::Procedural
            );
        }
    }
}

#[test]
fn prefix_returns_the_unchanged_effects_suffix_and_strict_decode_rejects_it() {
    let mut bytes = vec![0; TILE_COUNT];
    bytes.extend_from_slice(&[0, 128, 1, 255]);
    let (terrain, suffix) = Terrain::decode_prefix(&bytes, limits(bytes.len(), 1)).unwrap();
    assert_eq!(terrain.tiles().len(), TILE_COUNT);
    assert_eq!(suffix, &[0, 128, 1, 255]);
    assert_eq!(suffix.as_ptr(), bytes[TILE_COUNT..].as_ptr());
    assert_eq!(
        Terrain::decode(&bytes, limits(bytes.len(), 1)).unwrap_err(),
        DecodeError {
            offset: TILE_COUNT,
            tile: None,
            kind: DecodeErrorKind::TrailingData { remaining: 4 },
        }
    );
}

#[test]
fn input_byte_limit_applies_to_the_whole_slice_even_in_prefix_mode() {
    let bytes = vec![0; TILE_COUNT];
    assert!(Terrain::decode(&bytes, limits(TILE_COUNT, 1)).is_ok());
    for prefix in [false, true] {
        let result = if prefix {
            Terrain::decode_prefix(&bytes, limits(TILE_COUNT - 1, 1)).map(|(terrain, _)| terrain)
        } else {
            Terrain::decode(&bytes, limits(TILE_COUNT - 1, 1))
        };
        assert_eq!(
            result.unwrap_err(),
            DecodeError {
                offset: 0,
                tile: None,
                kind: DecodeErrorKind::InputTooLarge {
                    size: TILE_COUNT,
                    limit: TILE_COUNT - 1,
                },
            }
        );
    }
}

#[test]
fn prefix_input_limit_includes_unparsed_effects_suffix() {
    let mut bytes = vec![0; TILE_COUNT];
    bytes.extend_from_slice(&[0, 128, 1, 255]);
    assert_eq!(
        Terrain::decode_prefix(&bytes, limits(TILE_COUNT, 1)).unwrap_err(),
        DecodeError {
            offset: 0,
            tile: None,
            kind: DecodeErrorKind::InputTooLarge {
                size: TILE_COUNT + 4,
                limit: TILE_COUNT,
            },
        }
    );
}

#[test]
fn tag_budget_counts_tags_including_terminators_but_not_payload_bytes() {
    let bytes = archive_with(&[(0, vec![2, 255, 2, 128, 0])]);
    assert!(Terrain::decode(&bytes, limits(bytes.len(), 3)).is_ok());
    assert_eq!(
        Terrain::decode(&bytes, limits(bytes.len(), 2)).unwrap_err(),
        DecodeError {
            offset: 4,
            tile: position(0, 0, 0),
            kind: DecodeErrorKind::TileTagLimit { limit: 2 },
        }
    );
    assert_eq!(
        Terrain::decode(&[0], limits(1, 0)).unwrap_err(),
        DecodeError {
            offset: 0,
            tile: position(0, 0, 0),
            kind: DecodeErrorKind::TileTagLimit { limit: 0 },
        }
    );
    let bytes = archive_with(&[(0, vec![1, 255])]);
    assert!(Terrain::decode(&bytes, limits(bytes.len(), 1)).is_ok());
}

#[test]
fn long_unterminated_tile_stops_at_the_caller_budget() {
    let bytes = vec![255; 1024];
    assert_eq!(
        Terrain::decode(&bytes, limits(bytes.len(), 8)).unwrap_err(),
        DecodeError {
            offset: 8,
            tile: position(0, 0, 0),
            kind: DecodeErrorKind::TileTagLimit { limit: 8 },
        }
    );
}

#[test]
fn truncated_tag_height_and_overlay_reads_report_exact_positions() {
    let header = [2, 255, 49, 128, 50, 81, 82, 255, 1, 255];
    let fields = [
        ByteField::Tag,
        ByteField::OverlayId,
        ByteField::Tag,
        ByteField::OverlayId,
        ByteField::Tag,
        ByteField::Tag,
        ByteField::Tag,
        ByteField::Tag,
        ByteField::Tag,
        ByteField::ExplicitHeight,
    ];
    for cut in 0..header.len() {
        assert_eq!(
            Terrain::decode(&header[..cut], limits(usize::MAX, usize::MAX)).unwrap_err(),
            DecodeError {
                offset: cut,
                tile: position(0, 0, 0),
                kind: DecodeErrorKind::UnexpectedEnd { field: fields[cut] },
            }
        );
    }
    let bytes = vec![0; TILE_COUNT];
    for cut in [1, 63, 64, 4095, 4096, 8192, 12288, TILE_COUNT - 1] {
        assert_eq!(
            Terrain::decode(&bytes[..cut], limits(TILE_COUNT, 1)).unwrap_err(),
            DecodeError {
                offset: cut,
                tile: position(cut / 4096, cut / 64 % 64, cut % 64),
                kind: DecodeErrorKind::UnexpectedEnd {
                    field: ByteField::Tag
                },
            }
        );
    }
    for tag in [1, 2, 49] {
        let mut bytes = vec![0; TILE_COUNT];
        bytes[TILE_COUNT - 1] = tag;
        assert_eq!(
            Terrain::decode(&bytes, limits(TILE_COUNT, 1)).unwrap_err(),
            DecodeError {
                offset: TILE_COUNT,
                tile: position(3, 63, 63),
                kind: DecodeErrorKind::UnexpectedEnd {
                    field: if tag == 1 {
                        ByteField::ExplicitHeight
                    } else {
                        ByteField::OverlayId
                    },
                },
            }
        );
    }
}

#[test]
fn every_invalid_tile_axis_including_usize_max_is_rejected_before_index_math() {
    let terrain = decode(&vec![0; TILE_COUNT]);
    for (p, x, y) in [
        (PLANES, 0, 0),
        (0, MAP_SIZE, 0),
        (0, 0, MAP_SIZE),
        (usize::MAX, 0, 0),
        (0, usize::MAX, 0),
        (0, 0, usize::MAX),
        (usize::MAX, usize::MAX, usize::MAX),
    ] {
        assert!(terrain.tile(p, x, y).is_none());
        assert!(terrain
            .surface_height_native_units([i32::MAX, i32::MIN], p, x, y)
            .is_none());
    }
}

#[test]
fn bounded_arbitrary_truncations_never_panic_or_read_past_input() {
    let mut seed = 727_u32;
    for size in 0..512 {
        let bytes: Vec<_> = (0..size)
            .map(|_| {
                seed = seed.wrapping_mul(1664525).wrapping_add(1013904223);
                (seed >> 24) as u8
            })
            .collect();
        let error = Terrain::decode(&bytes, limits(512, 8)).unwrap_err();
        assert!(error.offset <= bytes.len());
        assert!(matches!(
            error.kind,
            DecodeErrorKind::UnexpectedEnd { .. } | DecodeErrorKind::TileTagLimit { limit: 8 }
        ));
        let tile = error.tile.unwrap();
        assert!(tile.plane < PLANES && tile.x < MAP_SIZE && tile.y < MAP_SIZE);
    }
}

#[test]
fn decode_planes_reads_a_single_underwater_plane_and_leaves_the_rest_default() {
    let bytes = vec![0u8; TILE_COUNT / PLANES];
    let (terrain, rest) = Terrain::decode_planes(&bytes, 1, limits(bytes.len(), 16)).unwrap();
    assert!(rest.is_empty());
    assert_eq!(terrain.tiles().len(), TILE_COUNT);
    assert!(Terrain::decode(&bytes, limits(bytes.len(), 16)).is_err());
}

#[test]
fn decode_planes_clamps_to_four_planes() {
    let bytes = archive_with(&[]);
    let (terrain, rest) = Terrain::decode_planes(&bytes, 9, limits(bytes.len(), 16)).unwrap();
    assert!(rest.is_empty());
    assert_eq!(terrain.tiles().len(), TILE_COUNT);
}
