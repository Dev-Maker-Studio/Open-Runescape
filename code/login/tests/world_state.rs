use openrs_login::world_state::{
    Error, LoadingType, MAX_CHUNK_DESCRIPTORS, MAX_WORLD_STATE_BYTES, MapSize,
    OWN_PLAYER_BLOCK_BYTES, PLAYER_SLOTS, WorldState, decode_dynamic_map, decode_fresh_world,
    decode_own_player, decode_standard_map,
};
use openrs_protocol::payload::PayloadError;
use openrs_protocol::registry::ServerPacket;

struct Fixture {
    name: &'static str,
    index: u16,
    opcode: ServerPacket,
    payload: &'static [u8],
    values: &'static [u8],
}

macro_rules! fixture {
    ($name:literal, $index:literal, $opcode:ident) => {
        Fixture {
            name: $name,
            index: $index,
            opcode: ServerPacket::$opcode,
            payload: include_bytes!(concat!("fixtures/world-state-", $name, ".payload")),
            values: include_bytes!(concat!("fixtures/world-state-", $name, ".native-values")),
        }
    };
}

const FIXTURES: &[Fixture] = &[
    fixture!("standard-104", 1, REGION),
    fixture!("standard-120", 1024, REGION),
    fixture!("standard-136", 2047, REGION),
    fixture!("standard-168", 31, REGION),
    fixture!("standard-72", 512, REGION),
    fixture!("standard-zero-truncate", 2, REGION),
    fixture!("standard-168-truncate", 2046, REGION),
    fixture!("standard-boundary", 127, REGION),
    fixture!("dynamic-size-0", 1, DYNAMIC_MAP_REGION),
    fixture!("dynamic-size-1", 501, DYNAMIC_MAP_REGION),
    fixture!("dynamic-size-2", 1001, DYNAMIC_MAP_REGION),
    fixture!("dynamic-size-3", 1501, DYNAMIC_MAP_REGION),
    fixture!("dynamic-size-4", 2001, DYNAMIC_MAP_REGION),
    fixture!("dynamic-empty", 2047, DYNAMIC_MAP_REGION),
    fixture!("dynamic-all", 1023, DYNAMIC_MAP_REGION),
    fixture!("dynamic-unknown-five", 9, DYNAMIC_MAP_REGION),
    fixture!("dynamic-unknown-zero", 2040, DYNAMIC_MAP_REGION),
];

fn case(name: &str) -> &'static Fixture {
    FIXTURES
        .iter()
        .find(|fixture| fixture.name == name)
        .unwrap()
}

fn state(fixture: &Fixture) -> WorldState {
    decode_fresh_world(fixture.index, fixture.opcode, fixture.payload).unwrap()
}

fn values(state: &WorldState, consumed: usize) -> Vec<u8> {
    let mut output = Vec::new();
    let mut int = |value: i32| output.extend_from_slice(&value.to_be_bytes());
    let players = &state.players;
    int(i32::from(players.player_index));
    int(i32::from(players.tile.plane));
    int(i32::from(players.tile.x));
    int(i32::from(players.tile.y));
    int(players.tile.tile_hash() as i32);
    int(players.tile.region_hash() as i32);
    int(players.other_players.len() as i32);
    for player in &players.other_players {
        int(i32::from(player.index));
        int(i32::from(player.plane));
        int(i32::from(player.region_x));
        int(i32::from(player.region_y));
        int(player.region_hash() as i32);
    }
    int(OWN_PLAYER_BLOCK_BYTES as i32);
    let map = &state.map;
    int(i32::from(map.map_size.id()));
    int(i32::from(map.map_size.size()));
    int(i32::from(map.center_x));
    int(i32::from(map.center_y));
    int(i32::from(map.force_position_update));
    int(match map.loading_type {
        LoadingType::Standard => 0,
        LoadingType::Instance => 1,
        LoadingType::InstanceNoEntityReset => 2,
        LoadingType::InstanceLarge => 3,
        LoadingType::InstanceLargeNoReloadUpdateZones => 4,
    });
    int(map.base.x);
    int(map.base.y);
    int(map.chunks.as_ref().map_or(0, |chunks| chunks.span as i32));
    int(map
        .chunks
        .as_ref()
        .map_or(0, |chunks| chunks.descriptors.len() as i32));
    if let Some(chunks) = &map.chunks {
        for descriptor in &chunks.descriptors {
            int(descriptor.map_or(-1, |descriptor| descriptor as i32));
        }
    }
    int(map.region_ids.len() as i32);
    for (&region, keys) in map.region_ids.iter().zip(&map.xtea_keys) {
        int(i32::from(region));
        for &key in keys {
            int(key);
        }
    }
    int(i32::from(state.local_tile.plane));
    int(state.local_tile.x);
    int(state.local_tile.y);
    int(consumed as i32);
    output
}

fn with_standard_header(size: u8, x: u16, y: u16) -> Vec<u8> {
    let mut payload = vec![size];
    payload.extend_from_slice(&x.to_be_bytes());
    payload.extend_from_slice(&y.to_be_bytes());
    payload.push(0);
    payload
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_seventeen_fixtures_match_native_primitive_and_geometry_oracle() {
    assert_eq!(FIXTURES.len(), 17);
    let manifest = include_str!("fixtures/world-state-cases.tsv");
    assert_eq!(manifest.lines().count(), FIXTURES.len());
    for fixture in FIXTURES {
        let snapshot = state(fixture);
        assert_eq!(
            values(&snapshot, fixture.payload.len()),
            fixture.values,
            "{}",
            fixture.name
        );
        assert!(manifest.lines().any(|line| line
            == format!(
                "{}\t{}\t{}",
                fixture.name,
                fixture.index,
                fixture.opcode.wire()
            )));
        assert_eq!(snapshot.map.region_ids.len(), snapshot.map.xtea_keys.len());
        assert_eq!(snapshot.players.other_players.len(), PLAYER_SLOTS - 2);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn explicit_map_helpers_and_own_player_prefix_match_whole_payload() {
    for fixture in FIXTURES {
        let snapshot = state(fixture);
        let (players, consumed) = decode_own_player(fixture.index, fixture.payload).unwrap();
        assert_eq!(consumed, OWN_PLAYER_BLOCK_BYTES);
        assert_eq!(players, snapshot.players);
        let payload = &fixture.payload[consumed..];
        let map = if fixture.opcode == ServerPacket::REGION {
            decode_standard_map(payload)
        } else {
            decode_dynamic_map(payload)
        }
        .unwrap();
        assert_eq!(map, snapshot.map);
        assert_eq!(
            decode_own_player(fixture.index, &fixture.payload[..consumed]).unwrap(),
            (players, consumed)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn own_player_indices_skip_exactly_the_explicit_slot() {
    let prefix = &FIXTURES[0].payload[..OWN_PLAYER_BLOCK_BYTES];
    for index in 1..PLAYER_SLOTS as u16 {
        let (players, consumed) = decode_own_player(index, prefix).unwrap();
        assert_eq!(consumed, 4608);
        assert_eq!(players.player_index, index);
        assert_eq!(players.other_players.len(), 2046);
        assert!(
            !players
                .other_players
                .iter()
                .any(|player| player.index == index || player.index == 0)
        );
        assert!(
            players
                .other_players
                .windows(2)
                .all(|pair| pair[0].index < pair[1].index)
        );
    }
    for index in [0, 2048, u16::MAX] {
        assert_eq!(
            decode_own_player(index, prefix),
            Err(Error::InvalidPlayerIndex(index))
        );
        assert_eq!(
            decode_fresh_world(index, ServerPacket::REGION, FIXTURES[0].payload),
            Err(Error::InvalidPlayerIndex(index))
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn own_bits_preserve_planes_full_tile_ranges_and_region_hashes() {
    let ordinary = state(case("standard-104"));
    assert_eq!(
        ordinary.players.tile.tile_hash(),
        (2 << 28) | (3201 << 14) | 3210
    );
    assert_eq!(
        ordinary.players.tile.region_hash(),
        (2 << 28) | (50 << 14) | 50
    );
    assert_eq!((ordinary.local_tile.x, ordinary.local_tile.y), (49, 50));
    let maximum = state(case("standard-120"));
    assert_eq!(maximum.players.tile.tile_hash(), 0x3fff_ffff);
    assert_eq!(
        maximum.players.tile.region_hash(),
        (3 << 28) | (255 << 14) | 255
    );
    let origin = state(case("standard-136"));
    assert_eq!(origin.players.tile.tile_hash(), 0);
    assert!(origin.local_tile.x < 0 && origin.local_tile.y < 0);
    let players = ordinary.players.other_players;
    assert!(players.iter().any(|player| player.region_x == 0));
    assert!(players.iter().any(|player| player.region_x == 255));
    assert!(players.iter().any(|player| player.region_y == 0));
    assert!(players.iter().any(|player| player.region_y == 255));
    for player in players {
        assert_eq!(player.plane, (player.index & 3) as u8);
        assert_eq!(player.region_x, (u32::from(player.index) * 37) as u8);
        assert_eq!(player.region_y, (u32::from(player.index) * 73 + 255) as u8);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_map_size_and_x_major_standard_region_order() {
    for (id, size) in [(0, 104), (1, 120), (2, 136), (3, 168), (4, 72)] {
        let map_size = MapSize::from_id(id).unwrap();
        assert_eq!(map_size.id(), id);
        assert_eq!(map_size.size(), size);
        assert_eq!(map_size.chunk_span(), usize::from(size / 8));
    }
    let snapshot = state(case("standard-104"));
    assert_eq!(
        snapshot.map.region_ids,
        vec![49 * 256 + 49, 49 * 256 + 50, 50 * 256 + 49, 50 * 256 + 50]
    );
    assert_eq!(
        snapshot.map.xtea_keys[0],
        [i32::MIN, i32::MAX, 0x89ab_cdefu32 as i32, 0]
    );
    assert_eq!(
        snapshot.map.xtea_keys[1],
        [i32::MIN + 1, i32::MAX - 1, (0x89ab_cdefu32 ^ 1) as i32, -1]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn standard_signed_division_truncates_toward_zero_and_base_stays_signed() {
    for (name, expected) in [
        ("standard-zero-truncate", -48),
        ("standard-168-truncate", -56),
        ("standard-72", -32),
    ] {
        let snapshot = state(case(name));
        assert_eq!(
            (snapshot.map.base.x, snapshot.map.base.y),
            (expected, expected)
        );
        assert_eq!(snapshot.map.region_ids.first(), Some(&0));
        assert_eq!(
            snapshot.local_tile.x,
            i32::from(snapshot.players.tile.x) - expected
        );
    }
    let snapshot = state(case("standard-boundary"));
    assert_eq!(snapshot.map.region_ids.last(), Some(&u16::MAX));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn standard_rejects_derived_archive_coordinates_outside_unsigned_bytes() {
    for (size, x, y) in [
        (3, 2, 3),
        (3, 3, 2),
        (0, 2042, 400),
        (0, 400, 2042),
        (4, u16::MAX, 0),
    ] {
        assert!(matches!(
            decode_standard_map(&with_standard_header(size, x, y)),
            Err(Error::InvalidArchiveCoordinates { .. })
        ));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn standard_boolean_is_exactly_one_dynamic_boolean_is_low_bit() {
    let mut standard = case("standard-104").payload[OWN_PLAYER_BLOCK_BYTES..].to_vec();
    let mut dynamic = case("dynamic-size-0").payload[OWN_PLAYER_BLOCK_BYTES..].to_vec();
    for value in 0..=255u8 {
        standard[5] = value;
        assert_eq!(
            decode_standard_map(&standard)
                .unwrap()
                .force_position_update,
            value == 1
        );
        dynamic[0] = value.wrapping_neg();
        assert_eq!(
            decode_dynamic_map(&dynamic).unwrap().force_position_update,
            value & 1 != 0
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dynamic_unknown_load_types_default_to_instance_without_length_guessing() {
    let mut payload = case("dynamic-size-0").payload[OWN_PLAYER_BLOCK_BYTES..].to_vec();
    for load_type in 0..=255u8 {
        payload[1] = 128u8.wrapping_sub(load_type);
        let expected = match load_type {
            2 => LoadingType::InstanceNoEntityReset,
            3 => LoadingType::InstanceLarge,
            4 => LoadingType::InstanceLargeNoReloadUpdateZones,
            _ => LoadingType::Instance,
        };
        assert_eq!(decode_dynamic_map(&payload).unwrap().loading_type, expected);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dynamic_transforms_chunk_order_first_seen_regions_and_full_descriptors() {
    let snapshot = state(case("dynamic-size-0"));
    assert_eq!((snapshot.map.center_x, snapshot.map.center_y), (257, 511));
    assert_eq!(snapshot.map.region_ids, [0, 32767, 258, 1032]);
    let grid = snapshot.map.chunks.unwrap();
    assert_eq!(grid.span, 13);
    assert_eq!(grid.get(0, 0, 0), Some(Some(0)));
    assert_eq!(grid.get(0, 0, 1), Some(Some(1)));
    assert_eq!(grid.get(0, 0, 2), Some(None));
    assert_eq!(grid.get(0, 1, 2), Some(Some((1 << 26) - 1)));
    assert_eq!(
        grid.get(1, 0, 0),
        Some(Some((1 << 24) | (10 << 14) | (19 << 3) | (2 << 1) | 1))
    );
    assert_eq!(grid.get(4, 0, 0), None);
    assert_eq!(grid.get(0, 13, 0), None);
    assert_eq!(grid.get(0, 0, 13), None);
    assert_eq!(grid.get(usize::MAX, usize::MAX, usize::MAX), None);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dynamic_empty_and_maximum_grid_are_bounded_without_invented_regions() {
    let empty = state(case("dynamic-empty"));
    assert!(empty.map.region_ids.is_empty());
    assert!(empty.map.xtea_keys.is_empty());
    assert!(
        empty
            .map
            .chunks
            .unwrap()
            .descriptors
            .iter()
            .all(Option::is_none)
    );
    assert_eq!((empty.map.base.x, empty.map.base.y), (524248, -32));
    let maximum = state(case("dynamic-all"));
    let grid = maximum.map.chunks.unwrap();
    assert_eq!(grid.span, 21);
    assert_eq!(grid.descriptors.len(), MAX_CHUNK_DESCRIPTORS);
    assert!(grid.descriptors.iter().all(Option::is_some));
    assert_eq!(
        maximum.map.region_ids,
        (0..MAX_CHUNK_DESCRIPTORS as u16).collect::<Vec<_>>()
    );
    assert_eq!(maximum.map.xtea_keys.len(), MAX_CHUNK_DESCRIPTORS);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn byte_aligned_finish_ignores_nonzero_own_and_dynamic_padding_bits() {
    assert_eq!(OWN_PLAYER_BLOCK_BYTES, 4608);
    for fixture in FIXTURES {
        let original = state(fixture);
        let mut payload = fixture.payload.to_vec();
        assert_eq!(payload[OWN_PLAYER_BLOCK_BYTES - 1] & 0x3f, 0x3f);
        payload[OWN_PLAYER_BLOCK_BYTES - 1] &= 0xc0;
        assert_eq!(
            decode_fresh_world(fixture.index, fixture.opcode, &payload).unwrap(),
            original
        );
        if let Some(chunks) = &original.map.chunks {
            let bits = chunks.descriptors.len()
                + chunks
                    .descriptors
                    .iter()
                    .filter(|chunk| chunk.is_some())
                    .count()
                    * 26;
            let padding = (8 - bits % 8) % 8;
            if padding != 0 {
                let last = OWN_PLAYER_BLOCK_BYTES + 7 + bits.div_ceil(8) - 1;
                payload[last] &= !((1u8 << padding) - 1);
                assert_eq!(
                    decode_fresh_world(fixture.index, fixture.opcode, &payload).unwrap(),
                    original
                );
            }
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_prefix_of_standard_and_dynamic_fresh_payload_is_rejected() {
    for name in ["standard-104", "dynamic-size-0", "dynamic-empty"] {
        let fixture = case(name);
        for end in 0..fixture.payload.len() {
            assert!(
                decode_fresh_world(fixture.index, fixture.opcode, &fixture.payload[..end]).is_err(),
                "{name} prefix {end}"
            );
        }
    }
    let fixture = case("dynamic-all");
    for end in [
        0,
        3,
        OWN_PLAYER_BLOCK_BYTES - 1,
        OWN_PLAYER_BLOCK_BYTES,
        OWN_PLAYER_BLOCK_BYTES + 6,
        OWN_PLAYER_BLOCK_BYTES + 7,
        OWN_PLAYER_BLOCK_BYTES + 8,
        fixture.payload.len() - 17,
        fixture.payload.len() - 16,
        fixture.payload.len() - 1,
    ] {
        assert!(
            decode_fresh_world(fixture.index, fixture.opcode, &fixture.payload[..end]).is_err()
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_permissive_key_remainders_and_surplus_groups_are_strictly_rejected() {
    for name in ["standard-104", "dynamic-size-0", "dynamic-empty"] {
        let fixture = case(name);
        for trailing in 1..=15 {
            let mut payload = fixture.payload.to_vec();
            payload.resize(payload.len() + trailing, 0);
            assert!(matches!(
                decode_fresh_world(fixture.index, fixture.opcode, &payload),
                Err(Error::InvalidKeyRemainder(_))
            ));
        }
        let mut payload = fixture.payload.to_vec();
        payload.extend_from_slice(&[0; 16]);
        let count = state(fixture).map.region_ids.len();
        assert_eq!(
            decode_fresh_world(fixture.index, fixture.opcode, &payload),
            Err(Error::KeyCountMismatch {
                expected: count,
                actual: count + 1
            })
        );
        if count > 0 {
            assert_eq!(
                decode_fresh_world(
                    fixture.index,
                    fixture.opcode,
                    &fixture.payload[..fixture.payload.len() - 16]
                ),
                Err(Error::KeyCountMismatch {
                    expected: count,
                    actual: count - 1
                })
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn only_explicit_region_opcodes_are_accepted_without_payload_guessing() {
    for wire in 0..=255 {
        if let Some(opcode) = ServerPacket::from_wire(wire)
            && !matches!(
                opcode,
                ServerPacket::REGION | ServerPacket::DYNAMIC_MAP_REGION
            )
        {
            assert_eq!(
                decode_fresh_world(1, opcode, FIXTURES[0].payload),
                Err(Error::UnsupportedOpcode(opcode))
            );
        }
    }
    for fixture in FIXTURES {
        let body = &fixture.payload[OWN_PLAYER_BLOCK_BYTES..];
        assert!(decode_fresh_world(fixture.index, fixture.opcode, body).is_err());
    }
    assert!(
        decode_fresh_world(
            1,
            ServerPacket::REGION,
            &FIXTURES[0].payload[..OWN_PLAYER_BLOCK_BYTES]
        )
        .is_err()
    );
    assert!(
        decode_fresh_world(
            1,
            ServerPacket::DYNAMIC_MAP_REGION,
            &FIXTURES[0].payload[..OWN_PLAYER_BLOCK_BYTES]
        )
        .is_err()
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_map_size_is_rejected_after_native_byte_transform() {
    let mut standard = case("standard-104").payload[OWN_PLAYER_BLOCK_BYTES..].to_vec();
    let mut dynamic = case("dynamic-size-0").payload[OWN_PLAYER_BLOCK_BYTES..].to_vec();
    for size in 5..=255u8 {
        standard[0] = size;
        dynamic[2] = size.wrapping_add(128);
        assert_eq!(
            decode_standard_map(&standard),
            Err(Error::InvalidMapSize(size))
        );
        assert_eq!(
            decode_dynamic_map(&dynamic),
            Err(Error::InvalidMapSize(size))
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn each_public_payload_api_enforces_u16_payload_bound() {
    let oversized = vec![0; MAX_WORLD_STATE_BYTES + 1];
    let expected = Error::LengthLimitExceeded {
        length: oversized.len(),
        limit: MAX_WORLD_STATE_BYTES,
    };
    assert_eq!(decode_own_player(1, &oversized), Err(expected.clone()));
    assert_eq!(decode_standard_map(&oversized), Err(expected.clone()));
    assert_eq!(decode_dynamic_map(&oversized), Err(expected.clone()));
    assert_eq!(
        decode_fresh_world(1, ServerPacket::REGION, &oversized),
        Err(expected.clone())
    );
    assert_eq!(
        decode_fresh_world(1, ServerPacket::DYNAMIC_MAP_REGION, &oversized),
        Err(expected)
    );
    let maximum = vec![0; MAX_WORLD_STATE_BYTES];
    assert!(decode_own_player(1, &maximum).is_ok());
    assert!(!matches!(
        decode_standard_map(&maximum),
        Err(Error::LengthLimitExceeded { .. })
    ));
    assert!(!matches!(
        decode_dynamic_map(&maximum),
        Err(Error::LengthLimitExceeded { .. })
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn empty_bodies_and_truncated_own_blocks_return_bounded_reader_errors() {
    assert_eq!(
        decode_standard_map(&[]),
        Err(Error::Payload(PayloadError::UnexpectedEof))
    );
    assert_eq!(
        decode_dynamic_map(&[]),
        Err(Error::Payload(PayloadError::UnexpectedEof))
    );
    for end in [0, 1, 2, 3, 4, OWN_PLAYER_BLOCK_BYTES - 1] {
        assert_eq!(
            decode_own_player(1, &FIXTURES[0].payload[..end]),
            Err(Error::Payload(PayloadError::UnexpectedEof))
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn debug_output_omits_asset_key_arrays() {
    let snapshot = state(case("standard-104"));
    let text = format!("{snapshot:?}");
    assert!(text.contains("key_count: 4"));
    assert!(!text.contains("xtea_keys"));
    assert!(!text.contains("-2147483648"));
    assert!(!text.contains("-1985229329"));
}
