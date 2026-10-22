use openrs_scene::floor::{
    blend_hsl, pack_hsl, rgb_to_hsl24, FloorError, FloorErrorKind, Overlay, Underlay,
    MAX_DEFINITION_BYTES, MAX_TAGS_PER_DEFINITION,
};
use std::str::from_utf8;

const NATIVE: &str = include_str!("fixtures/floor.tsv");

fn hex(payload: &str) -> Vec<u8> {
    assert_eq!(payload.len() % 2, 0);
    payload
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

fn underlay_fields(value: Underlay) -> [i64; 9] {
    [
        i64::from(value.rgb),
        i64::from(value.texture),
        i64::from(value.scale),
        i64::from(value.shadowed),
        i64::from(value.occlude),
        i64::from(value.hue),
        i64::from(value.saturation),
        i64::from(value.lightness),
        i64::from(value.hue_multiplier),
    ]
}

fn overlay_fields(value: Overlay) -> [i64; 14] {
    [
        i64::from(value.slot),
        i64::from(value.tile_color_hsl),
        i64::from(value.texture),
        i64::from(value.occlude),
        i64::from(value.minimap_color_hsl),
        i64::from(value.texture_scale),
        i64::from(value.shadowed),
        i64::from(value.blends_with_underlay),
        i64::from(value.water_color),
        i64::from(value.water_fog_depth),
        i64::from(value.water_intensity),
        i64::from(value.opcode20),
        i64::from(value.unused_opcode21),
        i64::from(value.unused_opcode22),
    ]
}

#[test]
fn underlay_initializers_do_not_apply_a_missing_rgb_opcode() {
    assert_eq!(
        Underlay::default(),
        Underlay {
            rgb: 0,
            texture: -1,
            scale: 512,
            shadowed: true,
            occlude: true,
            hue: 0,
            saturation: 0,
            lightness: 0,
            hue_multiplier: 0,
        }
    );
    assert_eq!(Underlay::decode(&[0]).unwrap(), Underlay::default());
    let black = Underlay::decode(&[1, 0, 0, 0, 0]).unwrap();
    assert_eq!(black.hue_multiplier, 1);
    assert_eq!(black.hue, 0);
    assert_eq!(black.saturation, 0);
    assert_eq!(black.lightness, 0);
}

#[test]
fn overlay_initializers_and_id_aware_missing_definition_match_post_decode() {
    assert_eq!(
        Overlay::default(),
        Overlay {
            slot: 8,
            tile_color_hsl: 0,
            texture: -1,
            occlude: true,
            minimap_color_hsl: -1,
            texture_scale: 512,
            shadowed: true,
            blends_with_underlay: false,
            water_color: 1_190_717,
            water_fog_depth: 512,
            water_intensity: 255,
            opcode20: 63,
            unused_opcode21: 0,
            unused_opcode22: 64,
            overlay_id: 0,
            saw_opcode8: false,
        }
    );
    for id in [0, 1, 255, 256, 65_535, i32::MAX as u32, 1 << 31, u32::MAX] {
        let value = Overlay::decode(id, &[0]).unwrap();
        assert_eq!(value, Overlay::default_for_id(id));
        assert_eq!(value.overlay_id, id);
        assert_eq!(value.slot, (8 << 8) | id as i32);
        assert!(!value.saw_opcode8);
    }
}

#[test]
fn native_underlay_vectors_cover_all_fields_opcodes_and_color_boundaries() {
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("underlay\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 12, "{line}");
        let bytes = hex(fields[1]);
        assert_eq!(bytes.len(), fields[2].parse::<usize>().unwrap(), "{line}");
        let actual = underlay_fields(Underlay::decode(&bytes).unwrap());
        let expected: Vec<i64> = fields[3..]
            .iter()
            .map(|number| number.parse().unwrap())
            .collect();
        assert_eq!(actual.as_slice(), expected, "{line}");
        count += 1;
    }
    assert_eq!(count, 5134);
}

#[test]
fn native_overlay_vectors_cover_all_recognized_opcodes_ordering_and_list_side_effect() {
    let mut count = 0;
    let mut saw_opcode8_count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("overlay\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 19, "{line}");
        let id = fields[1].parse().unwrap();
        let bytes = hex(fields[2]);
        assert_eq!(bytes.len(), fields[3].parse::<usize>().unwrap(), "{line}");
        let actual = Overlay::decode(id, &bytes).unwrap();
        let expected: Vec<i64> = fields[4..18]
            .iter()
            .map(|number| number.parse().unwrap())
            .collect();
        assert_eq!(overlay_fields(actual).as_slice(), expected, "{line}");
        assert_eq!(actual.overlay_id, id, "{line}");
        let native_list_value: i32 = fields[18].parse().unwrap();
        assert_eq!(
            actual.saw_opcode8,
            native_list_value != -1_200_000_007,
            "{line}"
        );
        let mut last_loaded_overlay_id = -1_200_000_007;
        if actual.saw_opcode8 {
            last_loaded_overlay_id = actual.overlay_id as i32;
            saw_opcode8_count += 1;
        }
        assert_eq!(last_loaded_overlay_id, native_list_value, "{line}");
        count += 1;
    }
    assert_eq!(count, 304);
    assert_eq!(saw_opcode8_count, 24);
}

#[test]
fn magenta_is_only_an_overlay_sentinel_not_a_general_rgb_conversion_sentinel() {
    assert_ne!(rgb_to_hsl24(0xff00ff), -1);
    let underlay = Underlay::decode(&[1, 255, 0, 255, 0]).unwrap();
    assert_eq!(underlay.rgb, 0xff00ff);
    assert_ne!(underlay.hue_multiplier, 0);
    let overlay = Overlay::decode(727, &[1, 255, 0, 255, 7, 255, 0, 255, 0]).unwrap();
    assert_eq!(overlay.tile_color_hsl, -1);
    assert_eq!(overlay.minimap_color_hsl, -1);
}

#[test]
fn native_integer_color_vectors_match_lightness_thresholds_masks_and_i32_wrapping() {
    let mut rgb_count = 0;
    let mut pack_count = 0;
    let mut blend_count = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "rgb" => {
                assert_eq!(fields.len(), 3);
                assert_eq!(
                    rgb_to_hsl24(fields[1].parse().unwrap()),
                    fields[2].parse::<i32>().unwrap(),
                    "{line}"
                );
                rgb_count += 1;
            }
            "pack" => {
                assert_eq!(fields.len(), 5);
                assert_eq!(
                    pack_hsl(
                        fields[1].parse().unwrap(),
                        fields[2].parse().unwrap(),
                        fields[3].parse().unwrap()
                    ),
                    fields[4].parse::<i32>().unwrap(),
                    "{line}"
                );
                pack_count += 1;
            }
            "blend" => {
                assert_eq!(fields.len(), 5);
                assert_eq!(
                    blend_hsl(
                        fields[1].parse().unwrap(),
                        fields[2].parse().unwrap(),
                        fields[3].parse().unwrap()
                    ),
                    fields[4].parse::<i32>().unwrap(),
                    "{line}"
                );
                blend_count += 1;
            }
            _ => {}
        }
    }
    assert_eq!(rgb_count, 5124);
    assert_eq!(pack_count, 2100);
    assert_eq!(blend_count, 1440);
}

fn hash_int(mut hash: u64, value: i32) -> u64 {
    for byte in value.to_le_bytes() {
        hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
    }
    hash
}

fn expected_hash(kind: &str) -> u64 {
    let matching: Vec<_> = NATIVE
        .lines()
        .filter(|line| line.split('\t').next() == Some(kind))
        .collect();
    assert_eq!(matching.len(), 1);
    let fields: Vec<_> = matching[0].split('\t').collect();
    assert_eq!(fields.len(), 3);
    assert_eq!(fields[1], "16777216");
    u64::from_str_radix(fields[2], 16).unwrap()
}

#[test]
fn every_24_bit_rgb_and_underlay_hsl_matches_original_native_hashes() {
    let mut rgb_hash = 0xcbf29ce484222325_u64;
    let mut underlay_hash = 0xcbf29ce484222325_u64;
    for rgb in 0_u32..=0xffffff {
        rgb_hash = hash_int(rgb_hash, rgb_to_hsl24(rgb));
        let value =
            Underlay::decode(&[1, (rgb >> 16) as u8, (rgb >> 8) as u8, rgb as u8, 0]).unwrap();
        for number in [
            value.hue,
            value.saturation,
            value.lightness,
            value.hue_multiplier,
        ] {
            underlay_hash = hash_int(underlay_hash, number);
        }
    }
    assert_eq!(rgb_hash, expected_hash("rgb-exhaustive"));
    assert_eq!(underlay_hash, expected_hash("underlay-exhaustive"));
}

#[test]
fn unknown_opcodes_and_trailing_bytes_are_intentionally_stricter_than_native() {
    let mut underlay_count = 0;
    let mut overlay_count = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "underlay-relaxed" => {
                let bytes = hex(fields[1]);
                let native_consumed: usize = fields[2].parse().unwrap();
                let error = Underlay::decode(&bytes).unwrap_err();
                match fields[1] {
                    "0600" => assert_eq!(
                        error,
                        FloorError {
                            offset: 0,
                            kind: FloorErrorKind::UnknownOpcode { opcode: 6 }
                        }
                    ),
                    "0004" => {
                        assert_eq!(native_consumed, 1);
                        assert_eq!(
                            error,
                            FloorError {
                                offset: 1,
                                kind: FloorErrorKind::TrailingData { remaining: 1 }
                            }
                        );
                    }
                    _ => panic!("unexpected relaxed underlay {line}"),
                }
                underlay_count += 1;
            }
            "overlay-relaxed" => {
                let bytes = hex(fields[2]);
                let error = Overlay::decode(fields[1].parse().unwrap(), &bytes).unwrap_err();
                match fields[2] {
                    "0400" | "0f00" => assert_eq!(
                        error,
                        FloorError {
                            offset: 0,
                            kind: FloorErrorKind::UnknownOpcode { opcode: bytes[0] }
                        }
                    ),
                    "0008" => assert_eq!(
                        error,
                        FloorError {
                            offset: 1,
                            kind: FloorErrorKind::TrailingData { remaining: 1 }
                        }
                    ),
                    _ => panic!("unexpected relaxed overlay {line}"),
                }
                overlay_count += 1;
            }
            _ => {}
        }
    }
    assert_eq!(underlay_count, 2);
    assert_eq!(overlay_count, 3);
    for opcode in 6_u8..=255 {
        assert!(matches!(
            Underlay::decode(&[opcode, 0]),
            Err(FloorError {
                offset: 0,
                kind: FloorErrorKind::UnknownOpcode { .. }
            })
        ));
    }
    for opcode in 1_u8..=255 {
        if [1, 2, 3, 5, 7, 8, 9, 10, 11, 12, 13, 14, 16, 20, 21, 22].contains(&opcode) {
            continue;
        }
        assert!(matches!(
            Overlay::decode(0, &[opcode, 0]),
            Err(FloorError {
                offset: 0,
                kind: FloorErrorKind::UnknownOpcode { .. }
            })
        ));
    }
}

#[test]
fn truncated_native_payloads_return_structured_errors_not_panics() {
    let mut underlay_count = 0;
    let mut overlay_count = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        let result = match fields[0] {
            "underlay-truncated" => {
                underlay_count += 1;
                Underlay::decode(&hex(fields[1])).map(|_| ())
            }
            "overlay-truncated" => {
                overlay_count += 1;
                Overlay::decode(727, &hex(fields[1])).map(|_| ())
            }
            _ => continue,
        };
        assert_eq!(fields[2], "ArrayIndexOutOfBoundsException");
        assert!(matches!(
            result,
            Err(FloorError {
                kind: FloorErrorKind::UnexpectedEnd { .. },
                ..
            })
        ));
    }
    assert_eq!(underlay_count, 10);
    assert_eq!(overlay_count, 24);
    assert_eq!(
        Underlay::decode(&[1, 255, 0]),
        Err(FloorError {
            offset: 1,
            kind: FloorErrorKind::UnexpectedEnd {
                needed: 3,
                remaining: 2,
            },
        })
    );
    assert_eq!(
        Overlay::decode(727, &[22, 255]),
        Err(FloorError {
            offset: 1,
            kind: FloorErrorKind::UnexpectedEnd {
                needed: 2,
                remaining: 1,
            },
        })
    );
}

#[test]
fn bounded_decoding_limits_bytes_and_tags_including_the_terminator() {
    assert_eq!(MAX_DEFINITION_BYTES, 4096);
    assert_eq!(MAX_TAGS_PER_DEFINITION, 256);
    let oversized = vec![0; MAX_DEFINITION_BYTES + 1];
    let expected = FloorError {
        offset: 0,
        kind: FloorErrorKind::InputTooLarge {
            size: oversized.len(),
            limit: MAX_DEFINITION_BYTES,
        },
    };
    assert_eq!(Underlay::decode(&oversized), Err(expected));
    assert_eq!(Overlay::decode(0, &oversized), Err(expected));
    let mut accepted = vec![5; MAX_TAGS_PER_DEFINITION - 1];
    accepted.push(0);
    assert!(Underlay::decode(&accepted).is_ok());
    assert!(Overlay::decode(0, &accepted).is_ok());
    let mut rejected = vec![5; MAX_TAGS_PER_DEFINITION];
    rejected.push(0);
    let expected = FloorError {
        offset: MAX_TAGS_PER_DEFINITION,
        kind: FloorErrorKind::TagLimit {
            limit: MAX_TAGS_PER_DEFINITION,
        },
    };
    assert_eq!(Underlay::decode(&rejected), Err(expected));
    assert_eq!(Overlay::decode(0, &rejected), Err(expected));
    let mut seed = 727_u32;
    for length in 0..=MAX_DEFINITION_BYTES {
        let bytes: Vec<u8> = (0..length)
            .map(|_| {
                seed = seed.wrapping_mul(1_664_525).wrapping_add(1_013_904_223);
                (seed >> 24) as u8
            })
            .collect();
        let _ = Underlay::decode(&bytes);
        let _ = Overlay::decode(seed, &bytes);
    }
}

#[test]
fn native_fixture_is_pinned_and_contains_only_checked_record_kinds() {
    let mut digests = 0;
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
            "underlay"
            | "overlay"
            | "rgb"
            | "pack"
            | "blend"
            | "rgb-exhaustive"
            | "underlay-exhaustive"
            | "underlay-relaxed"
            | "overlay-relaxed"
            | "underlay-truncated"
            | "overlay-truncated" => {}
            other => panic!("unexpected native floor record {other}"),
        }
    }
    assert_eq!(digests, 1);
}
