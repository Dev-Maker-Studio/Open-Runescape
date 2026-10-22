use openrs_login::world_list::{
    Error, MAX_DESCRIPTOR_SLOTS, MAX_WORLD_LIST_BYTES, WorldList, WorldListDecoder,
};
use openrs_protocol::payload::{PayloadError, PayloadWriter};

const BASIC: &[u8] = include_bytes!("fixtures/world-list-basic.payload");
const BASIC_VALUES: &[u8] = include_bytes!("fixtures/world-list-basic.native-values");
const COUNTS: &[u8] = include_bytes!("fixtures/world-list-counts.payload");

fn encoded(build: impl FnOnce(&mut PayloadWriter<'_>)) -> Vec<u8> {
    let mut bytes = vec![0; MAX_WORLD_LIST_BYTES + 128];
    let mut writer = PayloadWriter::new(&mut bytes);
    build(&mut writer);
    let length = writer.position();
    bytes.truncate(length);
    bytes
}

fn native_values(snapshot: &WorldList) -> Vec<u8> {
    encoded(|writer| {
        writer
            .write_u8(u8::from(snapshot.version.is_some()))
            .unwrap();
        if let Some(version) = snapshot.version {
            writer.write_i32_be(version).unwrap();
        }
        writer
            .write_i32_be(snapshot.locations.len() as i32)
            .unwrap();
        for location in &snapshot.locations {
            writer.write_i32_be(i32::from(location.country_id)).unwrap();
            writer.write_jag_cp1252(&location.activity).unwrap();
        }
        writer.write_i32_be(i32::from(snapshot.start)).unwrap();
        writer
            .write_i32_be(i32::from(snapshot.size_plus_one))
            .unwrap();
        writer
            .write_i32_be(snapshot.descriptor_slots as i32)
            .unwrap();
        writer.write_i32_be(snapshot.worlds.len() as i32).unwrap();
        for world in &snapshot.worlds {
            writer.write_i32_be(i32::from(world.raw_id)).unwrap();
            writer.write_i32_be(i32::from(world.world_number)).unwrap();
            writer
                .write_i32_be(i32::from(world.location_index))
                .unwrap();
            writer.write_i32_be(world.flags).unwrap();
            writer.write_jag_cp1252(&world.activity).unwrap();
            writer.write_jag_cp1252(&world.host).unwrap();
            writer.write_u8(u8::from(world.port.is_some())).unwrap();
            if let Some(port) = world.port {
                writer.write_i32_be(port).unwrap();
            }
            writer.write_i32_be(world.player_count).unwrap();
        }
    })
}

fn packet(refresh: u8, chunk: &[u8]) -> Vec<u8> {
    let mut bytes = vec![refresh];
    bytes.extend_from_slice(chunk);
    bytes
}

fn full_payload(start: u16, upper: u16, locations: u16, ids: &[u16], host: &str) -> Vec<u8> {
    encoded(|writer| {
        writer.write_u8(1).unwrap();
        writer.write_u8(2).unwrap();
        writer.write_u8(1).unwrap();
        writer.write_u_smart(locations).unwrap();
        for index in 0..locations {
            writer.write_u_smart(index).unwrap();
            writer.write_jag_cp1252("").unwrap();
        }
        writer.write_u_smart(start).unwrap();
        writer.write_u_smart(upper).unwrap();
        writer.write_u_smart(ids.len() as u16).unwrap();
        for &id in ids {
            writer.write_u_smart(id).unwrap();
            writer.write_u8(0).unwrap();
            writer.write_i32_be(0).unwrap();
            writer.write_jag_cp1252("").unwrap();
            writer.write_jag_cp1252(host).unwrap();
        }
        writer.write_i32_be(0).unwrap();
        for &id in ids {
            writer.write_u_smart(id).unwrap();
            writer.write_u16_be(1).unwrap();
        }
    })
}

fn minimal_payload() -> Vec<u8> {
    full_payload(0, 1, 1, &[0], "")
}

fn assert_poisoned(decoder: &mut WorldListDecoder) {
    assert!(decoder.is_failed());
    assert!(decoder.snapshot().is_none());
    assert_eq!(decoder.pending_bytes(), 0);
    assert_eq!(decoder.feed(BASIC), Err(Error::DecoderFailed));
    assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
}

fn reject(payload: &[u8], expected: Error) {
    let mut decoder = WorldListDecoder::new();
    decoder.feed(BASIC).unwrap().unwrap();
    assert_eq!(decoder.feed(payload), Err(expected));
    assert_poisoned(&mut decoder);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_snapshot_matches_isolated_native_oracle() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder.feed(BASIC).unwrap().unwrap();
    assert_eq!(native_values(snapshot), BASIC_VALUES);
    assert_eq!(snapshot.version, Some(0x89abcdefu32 as i32));
    assert_eq!(snapshot.locations[0].country_id, 200);
    assert_eq!(snapshot.locations[0].activity, "OFFLINE_€_Œ");
    assert_eq!(snapshot.worlds.len(), 2);
    let first = snapshot.world_by_raw_id(2).unwrap();
    assert_eq!(first.port, Some(43594));
    assert_eq!(first.player_count, 321);
    assert_eq!(first.host, "first.invalid");
    let second = snapshot.world_by_number(128).unwrap();
    assert_eq!(second.flags, 0x80000002u32 as i32);
    assert_eq!(second.activity, "FAKE_™_ÿ");
    assert_eq!(second.player_count, -1);
    assert_eq!(second.port, None);
    assert!(snapshot.world_by_raw_id(1).is_none());
    assert!(snapshot.world_by_number(131).is_none());
    assert_eq!(decoder.pending_bytes(), 0);
    assert_eq!(decoder.finish(), Ok(()));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_two_and_three_chunk_fixtures_match() {
    let fixtures: &[(&[&[u8]], &[u8])] = &[
        (
            &[
                include_bytes!("fixtures/world-list-split2-0.payload"),
                include_bytes!("fixtures/world-list-split2-1.payload"),
            ],
            include_bytes!("fixtures/world-list-split2.native-values"),
        ),
        (
            &[
                include_bytes!("fixtures/world-list-split3-0.payload"),
                include_bytes!("fixtures/world-list-split3-1.payload"),
                include_bytes!("fixtures/world-list-split3-2.payload"),
            ],
            include_bytes!("fixtures/world-list-split3.native-values"),
        ),
    ];
    for &(chunks, expected) in fixtures {
        let mut decoder = WorldListDecoder::new();
        for chunk in &chunks[..chunks.len() - 1] {
            assert!(decoder.feed(chunk).unwrap().is_none());
            assert!(decoder.snapshot().is_none());
        }
        let snapshot = decoder.feed(chunks.last().unwrap()).unwrap().unwrap();
        assert_eq!(native_values(snapshot), expected);
        assert_eq!(decoder.finish(), Ok(()));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_two_and_three_chunk_boundary_preserves_snapshot() {
    let data = &BASIC[1..];
    for first in 0..=data.len() {
        for refresh in [0, 2, 255] {
            let mut decoder = WorldListDecoder::new();
            assert!(
                decoder
                    .feed(&packet(refresh, &data[..first]))
                    .unwrap()
                    .is_none()
            );
            assert_eq!(decoder.pending_bytes(), first);
            let snapshot = decoder.feed(&packet(1, &data[first..])).unwrap().unwrap();
            assert_eq!(native_values(snapshot), BASIC_VALUES);
        }
        for second in first..=data.len() {
            let mut decoder = WorldListDecoder::new();
            assert!(decoder.feed(&packet(0, &data[..first])).unwrap().is_none());
            assert!(
                decoder
                    .feed(&packet(255, &data[first..second]))
                    .unwrap()
                    .is_none()
            );
            let snapshot = decoder.feed(&packet(1, &data[second..])).unwrap().unwrap();
            assert_eq!(native_values(snapshot), BASIC_VALUES);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn counts_only_preserves_previous_metadata_and_native_values() {
    let mut decoder = WorldListDecoder::new();
    let previous = decoder.feed(BASIC).unwrap().unwrap().clone();
    let snapshot = decoder.feed(COUNTS).unwrap().unwrap();
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-counts.native-values")
    );
    assert_eq!(snapshot.version, previous.version);
    assert_eq!(snapshot.locations, previous.locations);
    assert_eq!(snapshot.start, previous.start);
    assert_eq!(snapshot.size_plus_one, previous.size_plus_one);
    assert_eq!(snapshot.worlds[0].player_count, 65534);
    assert_eq!(snapshot.worlds[1].player_count, 42);
    let snapshot = decoder
        .feed(include_bytes!("fixtures/world-list-noncanonical.payload"))
        .unwrap()
        .unwrap();
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-noncanonical.native-values")
    );
    assert_eq!(snapshot.worlds[0].player_count, -1);
    assert_eq!(snapshot.worlds[1].player_count, 17);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn incomplete_update_does_not_publish_partial_counts() {
    let mut decoder = WorldListDecoder::new();
    let previous = decoder.feed(BASIC).unwrap().unwrap().clone();
    for &byte in &COUNTS[1..COUNTS.len() - 1] {
        assert!(decoder.feed(&[0, byte]).unwrap().is_none());
        assert_eq!(decoder.snapshot(), Some(&previous));
    }
    let updated = decoder
        .feed(&[1, *COUNTS.last().unwrap()])
        .unwrap()
        .unwrap();
    assert_eq!(
        native_values(updated),
        include_bytes!("fixtures/world-list-counts.native-values")
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn no_prior_counts_can_only_be_explicitly_empty() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder
        .feed(include_bytes!("fixtures/world-list-empty.payload"))
        .unwrap()
        .unwrap();
    assert_eq!(snapshot, &WorldList::default());
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-empty.native-values")
    );
    for has_worlds in [0, 2, 127, 255] {
        let mut decoder = WorldListDecoder::new();
        assert_eq!(
            decoder.feed(&[1, 2, has_worlds]).unwrap(),
            Some(&WorldList::default())
        );
    }
    let mut decoder = WorldListDecoder::new();
    assert_eq!(decoder.feed(COUNTS), Err(Error::TrailingData));
    assert_poisoned(&mut decoder);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn nonzero_start_uses_native_raw_id_indexing() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder
        .feed(include_bytes!("fixtures/world-list-nonzero.payload"))
        .unwrap()
        .unwrap();
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-nonzero.native-values")
    );
    assert_eq!(snapshot.start, 100);
    assert_eq!(snapshot.descriptor_slots, 131);
    assert_eq!(snapshot.world_by_number(102).unwrap().raw_id, 2);
    assert_eq!(snapshot.world_by_raw_id(128).unwrap().world_number, 228);
    assert_eq!(snapshot.world_by_raw_id(2).unwrap().port, Some(i32::MIN));
    assert!(snapshot.world_by_number(2).is_none());
    assert_eq!(
        decoder
            .feed(COUNTS)
            .unwrap()
            .unwrap()
            .world_by_number(102)
            .unwrap()
            .player_count,
        65534
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_sparse_count_entries_ignore_in_range_empty_slots() {
    let mut decoder = WorldListDecoder::new();
    decoder.feed(BASIC).unwrap();
    let snapshot = decoder
        .feed(include_bytes!("fixtures/world-list-sparse-counts.payload"))
        .unwrap()
        .unwrap();
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-sparse-counts.native-values")
    );
    assert_eq!(snapshot.world_by_raw_id(2).unwrap().player_count, 321);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn undefined_cp1252_uses_native_question_mark_replacement() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder
        .feed(include_bytes!(
            "fixtures/world-list-undefined-cp1252.payload"
        ))
        .unwrap()
        .unwrap();
    assert_eq!(snapshot.locations[0].activity, "OFFLINE_?_Œ");
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-undefined-cp1252.native-values")
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_truncated_full_prefix_is_rejected_without_native_padding() {
    for complete in [
        BASIC,
        include_bytes!("fixtures/world-list-nonzero.payload").as_slice(),
    ] {
        for end in 0..complete.len() {
            let mut decoder = WorldListDecoder::new();
            decoder.feed(BASIC).unwrap();
            assert!(
                decoder.feed(&complete[..end]).is_err(),
                "accepted prefix {end}"
            );
            assert_poisoned(&mut decoder);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_truncated_counts_prefix_poison_clears_previous_snapshot() {
    for end in 0..COUNTS.len() {
        let mut decoder = WorldListDecoder::new();
        decoder.feed(BASIC).unwrap();
        assert!(
            decoder.feed(&COUNTS[..end]).is_err(),
            "accepted counts prefix {end}"
        );
        assert_poisoned(&mut decoder);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_jag_prefix_in_each_string_is_rejected() {
    for index in [5, 16, 18] {
        let mut bytes = minimal_payload();
        bytes[index] = 1;
        reject(&bytes, Error::Payload(PayloadError::InvalidStringPrefix(1)));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn missing_jag_terminator_in_each_string_is_rejected() {
    for index in [5, 16, 18] {
        let mut bytes = minimal_payload();
        bytes[index + 1..].fill(65);
        reject(&bytes, Error::Payload(PayloadError::UnexpectedEof));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_mode_trailing_and_zero_length_packet_are_terminal() {
    reject(&[], Error::Payload(PayloadError::UnexpectedEof));
    reject(&[1], Error::Payload(PayloadError::UnexpectedEof));
    for mode in [0, 1, 3, 255] {
        reject(&[1, mode, 0], Error::InvalidMode(mode));
    }
    let mut trailing = minimal_payload();
    trailing.push(0);
    reject(&trailing, Error::TrailingData);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn descriptor_and_count_ids_are_bounded_and_duplicates_rejected() {
    let mut bytes = minimal_payload();
    bytes[10] = 2;
    reject(
        &bytes,
        Error::InvalidWorldId {
            raw_id: 2,
            slots: 2,
        },
    );
    let mut bytes = minimal_payload();
    bytes[24] = 2;
    reject(
        &bytes,
        Error::InvalidWorldId {
            raw_id: 2,
            slots: 2,
        },
    );
    reject(
        &full_payload(0, 1, 1, &[0, 0], ""),
        Error::DuplicateDescriptor(0),
    );
    let mut bytes = full_payload(0, 1, 1, &[0, 1], "");
    let second_count_id = bytes.len() - 3;
    bytes[second_count_id] = 0;
    reject(&bytes, Error::DuplicateCount(0));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_location_and_missing_locations_are_rejected() {
    let mut bytes = minimal_payload();
    bytes[11] = 1;
    reject(&bytes, Error::InvalidLocation { index: 1, count: 1 });
    reject(
        &full_payload(0, 1, 0, &[0], ""),
        Error::InvalidLocation { index: 0, count: 0 },
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn zero_length_native_descriptor_array_is_valid() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder
        .feed(include_bytes!("fixtures/world-list-zero-slots.payload"))
        .unwrap()
        .unwrap();
    assert_eq!(snapshot.descriptor_slots, 0);
    assert_eq!(snapshot.start, 1);
    assert_eq!(snapshot.size_plus_one, 0);
    assert_eq!(snapshot.version, Some(i32::MIN));
    assert!(snapshot.worlds.is_empty());
    assert_eq!(
        native_values(snapshot),
        include_bytes!("fixtures/world-list-zero-slots.native-values")
    );
    assert_eq!(
        decoder.feed(&[1, 2, 0]).unwrap().unwrap().version,
        Some(i32::MIN)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn negative_array_range_and_excess_world_count_are_rejected() {
    reject(
        &full_payload(3, 1, 1, &[0], ""),
        Error::InvalidDescriptorRange {
            start: 3,
            size_plus_one: 1,
        },
    );
    reject(
        &full_payload(0, 0, 1, &[0, 1], ""),
        Error::InvalidWorldCount { count: 2, slots: 1 },
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn numeric_counts_are_checked_against_wire_bytes_before_allocation() {
    reject(
        &[1, 2, 1, 255, 255],
        Error::Payload(PayloadError::UnexpectedEof),
    );
    let mut bytes = full_payload(0, 32767, 0, &[], "");
    let count_index = 7;
    bytes.splice(count_index..count_index + 1, [255, 255]);
    reject(&bytes, Error::Payload(PayloadError::UnexpectedEof));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn maximum_smart_raw_id_and_native_sparse_array_bound_are_supported() {
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder
        .feed(&full_payload(0, 32767, 1, &[32767], ""))
        .unwrap()
        .unwrap();
    assert_eq!(snapshot.descriptor_slots, MAX_DESCRIPTOR_SLOTS);
    assert_eq!(snapshot.world_by_raw_id(32767).unwrap().world_number, 32767);
    let snapshot = decoder
        .feed(&full_payload(32767, 32767, 1, &[0], ""))
        .unwrap()
        .unwrap();
    assert_eq!(snapshot.descriptor_slots, 1);
    assert_eq!(snapshot.world_by_number(32767).unwrap().raw_id, 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn location_count_smart_can_exceed_byte_addressable_locations() {
    let mut bytes = full_payload(0, 0, 257, &[0], "");
    let location_index = bytes.len() - 16;
    bytes[location_index] = 255;
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder.feed(&bytes).unwrap().unwrap();
    assert_eq!(snapshot.locations.len(), 257);
    assert_eq!(snapshot.locations[256].country_id, 256);
    assert_eq!(snapshot.worlds[0].location_index, 255);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn cumulative_chunk_limit_is_checked_before_copying() {
    let mut decoder = WorldListDecoder::new();
    decoder.feed(BASIC).unwrap();
    let bytes = vec![0; MAX_WORLD_LIST_BYTES + 1];
    assert!(decoder.feed(&bytes).unwrap().is_none());
    assert_eq!(decoder.pending_bytes(), MAX_WORLD_LIST_BYTES);
    assert_eq!(
        decoder.feed(&[0, 0]),
        Err(Error::LengthLimitExceeded {
            length: MAX_WORLD_LIST_BYTES + 1,
            limit: MAX_WORLD_LIST_BYTES
        })
    );
    assert_poisoned(&mut decoder);
    let oversized = vec![0; MAX_WORLD_LIST_BYTES + 2];
    reject(
        &oversized,
        Error::LengthLimitExceeded {
            length: MAX_WORLD_LIST_BYTES + 1,
            limit: MAX_WORLD_LIST_BYTES,
        },
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exact_byte_limit_and_long_strings_decode_without_truncation() {
    let overhead = minimal_payload().len() - 1;
    let host = "x".repeat(MAX_WORLD_LIST_BYTES - overhead);
    let bytes = full_payload(0, 1, 1, &[0], &host);
    assert_eq!(bytes.len(), MAX_WORLD_LIST_BYTES + 1);
    let mut decoder = WorldListDecoder::new();
    let snapshot = decoder.feed(&bytes).unwrap().unwrap();
    assert_eq!(snapshot.worlds[0].host, host);
    assert_eq!(decoder.pending_bytes(), 0);
    decoder.feed(BASIC).unwrap();
    assert_eq!(native_values(decoder.snapshot().unwrap()), BASIC_VALUES);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn finish_poison_clears_partial_and_prior_state_then_reset_recovers() {
    let mut decoder = WorldListDecoder::new();
    assert_eq!(decoder.finish(), Ok(()));
    decoder.feed(BASIC).unwrap();
    assert!(decoder.feed(&[0]).unwrap().is_none());
    assert_eq!(
        decoder.finish(),
        Err(Error::Payload(PayloadError::UnexpectedEof))
    );
    assert_poisoned(&mut decoder);
    decoder.reset();
    assert!(!decoder.is_failed());
    assert!(decoder.snapshot().is_none());
    assert_eq!(
        native_values(decoder.feed(BASIC).unwrap().unwrap()),
        BASIC_VALUES
    );
    decoder.feed(&[0, 2]).unwrap();
    decoder.reset();
    assert_eq!(decoder.pending_bytes(), 0);
    assert_eq!(decoder.finish(), Ok(()));
    assert_eq!(
        native_values(decoder.feed(BASIC).unwrap().unwrap()),
        BASIC_VALUES
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_final_chunk_drops_accumulated_bytes_and_previous_metadata() {
    let mut decoder = WorldListDecoder::new();
    decoder.feed(BASIC).unwrap();
    assert!(decoder.feed(&[0, 2, 0]).unwrap().is_none());
    assert_eq!(
        decoder.feed(&[1, 0]),
        Err(Error::Payload(PayloadError::UnexpectedEof))
    );
    assert_poisoned(&mut decoder);
}
