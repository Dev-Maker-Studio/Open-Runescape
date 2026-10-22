use openrs_scene::terrain::{DecodeLimits, Terrain, MAP_SIZE, PLANES, TILE_COUNT};
use std::str::from_utf8;

const NATIVE: &str = include_str!("fixtures/native.tsv");

fn decode(bytes: &[u8]) -> Terrain {
    Terrain::decode(
        bytes,
        DecodeLimits {
            max_input_bytes: bytes.len(),
            max_tags_per_tile: 16,
        },
    )
    .unwrap()
}

fn hex(payload: &str) -> Vec<u8> {
    assert_eq!(payload.len() % 2, 0);
    payload
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

fn archive_bytes(patterned: bool) -> Vec<u8> {
    let mut bytes = Vec::new();
    for i in 0..TILE_COUNT {
        if !patterned {
            bytes.push(0);
            continue;
        }
        bytes.extend_from_slice(&[
            2 + (i % 48) as u8,
            ((i * 73 + 128) & 255) as u8,
            50 + (i % 32) as u8,
            82 + (i % 174) as u8,
        ]);
        if i % 7 == 0 {
            bytes.extend_from_slice(&[2 + ((i + 17) % 48) as u8, (i & 255) as u8]);
        }
        if i % 11 == 0 {
            bytes.extend_from_slice(&[81, 255]);
        }
        if i % 3 == 0 {
            bytes.push(0);
        } else {
            bytes.extend_from_slice(&[1, (i & 255) as u8]);
        }
    }
    bytes
}

fn archive_hash(terrain: &Terrain, origin: [i32; 2]) -> u64 {
    let mut hash = 0xcbf29ce484222325_u64;
    for p in 0..PLANES {
        for x in 0..MAP_SIZE {
            for y in 0..MAP_SIZE {
                let tile = terrain.tile(p, x, y).unwrap();
                let height = terrain
                    .surface_height_native_units(origin, p, x, y)
                    .unwrap();
                let bytes = height.to_le_bytes().into_iter().chain([
                    tile.overlay_id,
                    tile.underlay_id,
                    tile.render_flags,
                    tile.overlay_shape,
                    tile.overlay_rotation,
                ]);
                for byte in bytes {
                    hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
                }
            }
        }
    }
    hash
}

#[test]
fn native_height_vectors_match_negative_coordinates_fractions_and_i32_wrapping() {
    let terrain = decode(&vec![0; TILE_COUNT]);
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("height\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 4);
        let x = fields[1].parse().unwrap();
        let y = fields[2].parse().unwrap();
        let expected = fields[3].parse().unwrap();
        assert_eq!(
            terrain.surface_height_native_units([x, y], 0, 0, 0),
            Some(expected),
            "{line}"
        );
        count += 1;
    }
    assert_eq!(count, 349);
}

#[test]
fn native_tile_vectors_match_attributes_and_height_on_all_four_planes() {
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("tile\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 10);
        let plane: usize = fields[1].parse().unwrap();
        let payload = hex(fields[2]);
        assert_eq!(payload.len(), fields[3].parse::<usize>().unwrap());
        let mut bytes = Vec::new();
        for p in 0..PLANES {
            for x in 0..MAP_SIZE {
                for y in 0..MAP_SIZE {
                    if x == 0 && y == 0 && p == plane {
                        bytes.extend_from_slice(&payload);
                    } else if x == 0 && y == 0 && p < plane {
                        bytes.extend_from_slice(&[1, 7 + p as u8]);
                    } else {
                        bytes.push(0);
                    }
                }
            }
        }
        let terrain = decode(&bytes);
        let tile = terrain.tile(plane, 0, 0).unwrap();
        assert_eq!(
            terrain.surface_height_native_units([3200, 3200], plane, 0, 0),
            Some(fields[4].parse().unwrap()),
            "{line}"
        );
        let expected: Vec<u8> = fields[5..]
            .iter()
            .map(|field| field.parse().unwrap())
            .collect();
        assert_eq!(
            [
                tile.overlay_id,
                tile.underlay_id,
                tile.render_flags,
                tile.overlay_shape,
                tile.overlay_rotation
            ]
            .as_slice(),
            expected,
            "{line}"
        );
        count += 1;
    }
    assert_eq!(count, 60);
}

#[test]
fn native_full_archive_vectors_match_order_consumption_and_resolved_contents() {
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("archive\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 6);
        let patterned = match fields[1] {
            "zero" => false,
            "patterned" => true,
            other => panic!("unexpected native archive {other}"),
        };
        let bytes = archive_bytes(patterned);
        assert_eq!(bytes.len(), fields[4].parse::<usize>().unwrap());
        let origin = [fields[2].parse().unwrap(), fields[3].parse().unwrap()];
        let expected = u64::from_str_radix(fields[5], 16).unwrap();
        assert_eq!(archive_hash(&decode(&bytes), origin), expected, "{line}");
        count += 1;
    }
    assert_eq!(count, 6);
}

#[test]
fn native_fixture_has_only_known_records_and_a_pinned_jar_digest() {
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
            "cosine" | "height" | "tile" | "archive" => {}
            other => panic!("unexpected native fixture record {other}"),
        }
    }
    assert_eq!(digests, 1);
}
