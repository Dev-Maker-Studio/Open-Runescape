use super::*;
use std::collections::BTreeMap;
use whirlpool::{Digest, Whirlpool};

fn raw(bytes: &[u8]) -> Vec<u8> {
    let mut result = vec![0];
    result.extend_from_slice(&u32::try_from(bytes.len()).unwrap().to_be_bytes());
    result.extend_from_slice(bytes);
    result
}

fn reply(index: u8, archive: u32, container: &[u8]) -> Vec<u8> {
    let mut result = vec![index];
    result.extend_from_slice(&archive.to_be_bytes());
    result.extend_from_slice(&container[..5]);
    let mut offset = 10;
    for &byte in &container[5..] {
        if offset == 512 {
            result.push(255);
            offset = 1;
        }
        result.push(byte);
        offset += 1;
    }
    result
}

fn smart(bytes: &mut Vec<u8>, number: u32) {
    if number >= 32768 {
        bytes.extend_from_slice(&(number | 0x80000000).to_be_bytes());
    } else {
        bytes.extend_from_slice(&u16::try_from(number).unwrap().to_be_bytes());
    }
}

struct Entry {
    id: u32,
    files: Vec<(u32, Vec<u8>)>,
    crc: Option<u32>,
    whirlpool: Option<[u8; 64]>,
    payload: Option<Vec<u8>>,
}

impl Entry {
    fn new(id: u32, files: &[(u32, &[u8])]) -> Self {
        Self {
            id,
            files: files
                .iter()
                .map(|(id, bytes)| (*id, bytes.to_vec()))
                .collect(),
            crc: None,
            whirlpool: None,
            payload: None,
        }
    }

    fn container(&self) -> Vec<u8> {
        let mut payload = self
            .files
            .iter()
            .flat_map(|(_, bytes)| bytes.iter().copied())
            .collect::<Vec<_>>();
        if self.files.len() > 1 {
            let mut prior = 0i32;
            for (_, bytes) in &self.files {
                let length = i32::try_from(bytes.len()).unwrap();
                payload.extend_from_slice(&(length - prior).to_be_bytes());
                prior = length;
            }
            payload.push(1);
        }
        raw(self.payload.as_deref().unwrap_or(&payload))
    }
}

struct Chain {
    version: Vec<u8>,
    reference: Vec<u8>,
    archives: BTreeMap<u32, Vec<u8>>,
}

impl Chain {
    fn new(entries: Vec<Entry>) -> Self {
        Self::with_flags(entries, true, true)
    }

    fn with_flags(mut entries: Vec<Entry>, named: bool, hashed: bool) -> Self {
        entries.sort_by_key(|entry| entry.id);
        let mut table = vec![7];
        table.extend_from_slice(&71i32.to_be_bytes());
        table.push(u8::from(named) | (u8::from(hashed) << 1));
        smart(&mut table, u32::try_from(entries.len()).unwrap());
        let mut prior = 0;
        for entry in &entries {
            smart(&mut table, entry.id - prior);
            prior = entry.id;
        }
        if named {
            for _ in &entries {
                table.extend_from_slice(&0i32.to_be_bytes());
            }
        }
        let archives = entries
            .iter()
            .map(|entry| (entry.id, entry.container()))
            .collect::<BTreeMap<_, _>>();
        for entry in &entries {
            table.extend_from_slice(
                &entry
                    .crc
                    .unwrap_or_else(|| crc32fast::hash(&archives[&entry.id]))
                    .to_be_bytes(),
            );
        }
        if hashed {
            for entry in &entries {
                table.extend_from_slice(
                    &entry
                        .whirlpool
                        .unwrap_or_else(|| Whirlpool::digest(&archives[&entry.id]).into()),
                );
            }
        }
        for _ in &entries {
            table.extend_from_slice(&42i32.to_be_bytes());
        }
        for entry in &entries {
            smart(&mut table, u32::try_from(entry.files.len()).unwrap());
        }
        for entry in &entries {
            let mut prior = 0;
            for (id, _) in &entry.files {
                smart(&mut table, id - prior);
                prior = *id;
            }
        }
        if named {
            for entry in &entries {
                for _ in &entry.files {
                    table.extend_from_slice(&0i32.to_be_bytes());
                }
            }
        }
        let reference = raw(&table);
        Self {
            version: version(&reference, 71),
            reference,
            archives,
        }
    }

    fn before_reference(&self) -> WorldFloorProbe {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_handshake(b"synthetic").unwrap()
        );
        probe.feed(&[0; 109]).unwrap();
        assert_eq!(probe.take_outgoing().len(), 18);
        probe.feed(&reply(255, 255, &self.version)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(255, 2, js5::Priority::High)
        );
        probe
    }

    fn begin(&self) -> WorldFloorProbe {
        let mut probe = self.before_reference();
        probe.feed(&reply(255, 2, &self.reference)).unwrap();
        probe
    }

    fn wire(&self) -> Vec<u8> {
        let mut bytes = vec![0; 109];
        bytes.extend_from_slice(&reply(255, 255, &self.version));
        bytes.extend_from_slice(&reply(255, 2, &self.reference));
        for id in [1, 4] {
            bytes.extend_from_slice(&reply(2, id, &self.archives[&id]));
        }
        bytes
    }
}

fn version(reference: &[u8], revision: i32) -> Vec<u8> {
    let mut bytes = vec![3];
    bytes.extend_from_slice(&[0; 2 * 72]);
    bytes.extend_from_slice(&crc32fast::hash(reference).to_be_bytes());
    bytes.extend_from_slice(&revision.to_be_bytes());
    bytes.extend_from_slice(&Whirlpool::digest(reference));
    bytes.extend_from_slice(&[0x5a; 128]);
    raw(&bytes)
}

fn fixture() -> Chain {
    Chain::new(vec![
        Entry::new(0, &[(0, &[0])]),
        Entry::new(
            1,
            &[
                (0, &[1, 0x80, 0x20, 0x10, 2, 0xff, 0xff, 3, 0, 7, 4, 5, 0]),
                (3, &[0]),
                (65535, &[1, 0, 0, 0, 0]),
            ],
        ),
        Entry::new(
            4,
            &[
                (1, &[1, 0xff, 0, 0xff, 8, 0]),
                (5, &[8, 11, 255, 9, 0xff, 0xff, 0]),
            ],
        ),
        Entry::new(9, &[(0, &[0]), (7, &[0])]),
    ])
}

fn terminal(probe: &mut WorldFloorProbe) {
    assert!(!probe.complete());
    assert!(probe.report().is_none());
    assert!(probe.repository().is_err());
    assert!(probe.underlay(0).is_err());
    assert!(probe.overlay(0).is_err());
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.transfer.is_none());
    assert!(probe.underlays.is_empty());
    assert!(probe.underlay_report.is_none());
    assert!(probe.repository.is_none());
    assert_eq!(
        (
            probe.reference_files,
            probe.received,
            probe.containers,
            probe.expanded,
            probe.operations
        ),
        (0, 0, 0, 0, 0)
    );
    assert!(probe.feed(&[]).is_err());
    assert!(probe.finish().is_err());
}

#[test]
fn fragmented_verified_single_chain_preserves_floor_ids_and_native_defaults() {
    let chain = fixture();
    let wire = chain.wire();
    for size in [1, 2, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        assert!(probe.repository().is_err());
        for chunk in wire.chunks(size) {
            probe.feed(chunk).unwrap();
        }
        assert!(probe.complete());
        let report = probe.report().unwrap();
        assert_eq!(
            (
                report.revision,
                report.subrevision,
                report.index,
                report.prefetch_count
            ),
            (727, 1, 2, 27)
        );
        assert_eq!(
            (
                report.reference_revision,
                report.reference_archives,
                report.reference_files
            ),
            (71, 4, 8)
        );
        assert!(report.reference_crc_verified && report.reference_whirlpool_verified);
        assert!(
            !report.authenticated && !report.rendered_world && !report.opcode8_side_effects_applied
        );
        assert_eq!(report.total_wire_bytes, wire.len());
        assert_eq!(
            report.total_container_bytes,
            chain.version.len()
                + chain.reference.len()
                + chain.archives[&1].len()
                + chain.archives[&4].len()
        );
        assert_eq!(
            report.total_expanded_bytes,
            report.total_container_bytes - 4 * 5
        );
        assert_eq!(
            (
                report.underlays.archive,
                report.underlays.file_slots,
                report.underlays.populated_definitions
            ),
            (1, 65536, 3)
        );
        assert_eq!(
            (
                report.overlays.archive,
                report.overlays.file_slots,
                report.overlays.populated_definitions
            ),
            (4, 6, 2)
        );
        assert!(
            report.underlays.crc_verified
                && report.underlays.whirlpool_verified
                && report.overlays.crc_verified
                && report.overlays.whirlpool_verified
        );
        assert!(report.total_operations <= report.limits.max_total_operations);
        assert_eq!(report.limits.max_definition_bytes, MAX_DEFINITION_BYTES);
        assert_eq!(
            report.limits.max_tags_per_definition,
            MAX_TAGS_PER_DEFINITION
        );
        let repository = probe.repository().unwrap();
        assert_eq!(repository.underlay_file_slots(), 65536);
        assert_eq!(repository.overlay_file_slots(), 6);
        assert_eq!(repository.underlays().len(), 3);
        assert_eq!(repository.overlays().len(), 2);
        assert!(repository.underlay(3).unwrap().present());
        assert!(!repository.underlay(2).unwrap().present());
        assert_eq!(
            *repository.underlay(2).unwrap().definition(),
            Underlay::default()
        );
        assert_eq!(
            *repository.underlay(3).unwrap().definition(),
            Underlay::default()
        );
        assert_eq!(repository.underlay(0).unwrap().definition().scale, 28);
        assert_eq!(
            repository
                .underlay(65535)
                .unwrap()
                .definition()
                .hue_multiplier,
            1
        );
        assert!(repository.overlay(1).unwrap().definition().saw_opcode8);
        assert!(repository.overlay(5).unwrap().definition().saw_opcode8);
        assert_eq!(repository.overlay(5).unwrap().definition().slot, 65285);
        for id in [0, 2, 6, 65535] {
            let lookup = repository.overlay(id).unwrap();
            assert!(!lookup.present());
            assert_eq!(*lookup.definition(), Overlay::default_for_id(id));
            assert_eq!(lookup.definition().overlay_id, id);
        }
        let json = serde_json::to_string(report).unwrap();
        assert!(!json.contains("synthetic") && !json.contains("whirlpool\":"));
        probe.finish().unwrap();
        probe.finish().unwrap();
        probe.feed(&[]).unwrap();
    }
}

#[test]
fn requests_both_original_config_archives_sequentially_regardless_lookup_demand() {
    let chain = fixture();
    let mut probe = chain.begin();
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(2, 1, js5::Priority::High)
    );
    assert!(probe.take_outgoing().is_empty());
    probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap();
    assert_eq!(probe.underlays.len(), 3);
    assert!(probe.repository().is_err());
    assert!(probe.underlay(0).is_err());
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(2, 4, js5::Priority::High)
    );
    probe.feed(&reply(2, 4, &chain.archives[&4])).unwrap();
    assert!(probe.complete());
    assert!(probe.take_outgoing().is_empty());
}

#[test]
fn invalid_queries_do_not_destroy_valid_repository_or_completion() {
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    probe.feed(&fixture().wire()).unwrap();
    for id in [65536, u32::MAX] {
        assert!(probe.underlay(id).is_err());
        assert!(probe.overlay(id).is_err());
        assert!(probe.repository().unwrap().underlay(id).is_err());
        assert!(probe.complete());
        assert_eq!(probe.underlay(0).unwrap().definition().scale, 28);
    }
    probe.finish().unwrap();
}

#[test]
fn unavailable_archives_never_become_empty_defaults_but_verified_empty_archives_do() {
    for entries in [vec![], vec![Entry::new(1, &[])], vec![Entry::new(4, &[])]] {
        let chain = Chain::new(entries);
        let mut probe = chain.before_reference();
        assert_eq!(
            probe.feed(&reply(255, 2, &chain.reference)).unwrap_err(),
            "floor archive unavailable in reference table"
        );
        terminal(&mut probe);
    }
    let chain = Chain::new(vec![Entry::new(1, &[]), Entry::new(4, &[])]);
    let mut probe = chain.begin();
    assert!(!probe.complete());
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(2, 1, js5::Priority::High)
    );
    probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap();
    assert!(!probe.complete());
    probe.feed(&reply(2, 4, &chain.archives[&4])).unwrap();
    let report = probe.report().unwrap();
    assert_eq!(report.underlays.file_slots, 0);
    assert_eq!(report.overlays.populated_definitions, 0);
    assert!(!probe.underlay(0).unwrap().present());
    assert_eq!(
        *probe.overlay(65535).unwrap().definition(),
        Overlay::default_for_id(65535)
    );
}

#[test]
fn unnamed_tables_and_optional_archive_whirlpool_are_supported_without_authentication() {
    let chain = Chain::with_flags(
        vec![Entry::new(1, &[(0, &[0])]), Entry::new(4, &[(0, &[0])])],
        false,
        false,
    );
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    probe.feed(&chain.wire()).unwrap();
    let report = probe.report().unwrap();
    assert!(report.reference_whirlpool_verified);
    assert!(report.underlays.crc_verified && report.overlays.crc_verified);
    assert!(!report.underlays.whirlpool_verified && !report.overlays.whirlpool_verified);
    assert!(!report.authenticated && !report.rendered_world);
}

#[test]
fn corrupted_crc_whirlpool_and_bytes_drop_every_previously_decoded_definition() {
    for kind in 0..3 {
        let mut overlay = Entry::new(4, &[(0, &[0])]);
        if kind == 0 {
            overlay.crc = Some(0xdeadbeef);
        }
        if kind == 1 {
            overlay.whirlpool = Some([0x6a; 64]);
        }
        let chain = Chain::new(vec![Entry::new(1, &[(0, &[0])]), overlay]);
        let mut probe = chain.begin();
        probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap();
        assert_eq!(probe.underlays.len(), 1);
        let mut container = chain.archives[&4].clone();
        if kind == 2 {
            *container.last_mut().unwrap() ^= 1;
        }
        assert_eq!(
            probe.feed(&reply(2, 4, &container)).unwrap_err(),
            "invalid floor container or integrity check"
        );
        terminal(&mut probe);
    }
}

#[test]
fn strict_reference_integrity_revision_unknown_flags_trailing_and_unrelated_records() {
    for kind in 0..7 {
        let mut chain = fixture();
        match kind {
            0 => *chain.reference.last_mut().unwrap() ^= 1,
            1 => chain.version[5 + 1 + 2 * 72 + 8] ^= 1,
            2 => chain.version = version(&chain.reference, 72),
            3 => {
                chain.reference[5] = 249;
                chain.version = version(&chain.reference, 71);
            }
            4 => {
                chain.reference[10] = 0xff;
                chain.version = version(&chain.reference, 71);
            }
            5 => {
                chain.reference.push(0);
                let length = u32::try_from(chain.reference.len() - 5).unwrap();
                chain.reference[1..5].copy_from_slice(&length.to_be_bytes());
                chain.version = version(&chain.reference, 71);
            }
            _ => {
                chain.reference[19..21].fill(0);
                chain.version = version(&chain.reference, 71);
            }
        }
        let mut probe = chain.before_reference();
        let error = probe.feed(&reply(255, 2, &chain.reference)).unwrap_err();
        assert!(matches!(
            error.as_str(),
            "invalid floor container or integrity check"
                | "invalid floor reference table"
                | "floor reference revision mismatch"
        ));
        assert!(!error.contains("249") && !error.contains("deadbeef"));
        terminal(&mut probe);
    }
}

#[test]
fn strict_definition_decode_is_transactional_unknown_trailing_truncated_and_bounded() {
    let mut too_many_tags = vec![4; MAX_TAGS_PER_DEFINITION + 1];
    too_many_tags.push(0);
    for (id, invalid) in [
        (1, vec![]),
        (1, vec![1]),
        (1, vec![2, 0]),
        (1, vec![255, 0]),
        (1, vec![0, 0]),
        (1, too_many_tags),
        (1, vec![4; MAX_DEFINITION_BYTES + 1]),
        (4, vec![]),
        (4, vec![7, 0]),
        (4, vec![255, 0]),
        (4, vec![0, 0]),
        (4, vec![8; MAX_TAGS_PER_DEFINITION + 1]),
    ] {
        let mut entries = vec![Entry::new(1, &[(0, &[0])]), Entry::new(4, &[(0, &[0])])];
        let entry = entries.iter_mut().find(|entry| entry.id == id).unwrap();
        entry.files.push((1, invalid));
        let chain = Chain::new(entries);
        let mut probe = chain.begin();
        if id == 4 {
            probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap();
        }
        let error = probe.feed(&reply(2, id, &chain.archives[&id])).unwrap_err();
        assert!(error.contains("definition or diagnostic bound"));
        terminal(&mut probe);
    }
}

#[test]
fn invalid_split_tables_nonempty_empty_archives_and_out_of_domain_floor_ids_fail_closed() {
    for files in [vec![], vec![(0, vec![0]), (3, vec![0])]] {
        let mut entry = Entry::new(1, &[]);
        entry.files = files;
        entry.payload = Some(vec![0x5a]);
        let chain = Chain::new(vec![entry, Entry::new(4, &[])]);
        let mut probe = chain.begin();
        assert_eq!(
            probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap_err(),
            "invalid floor archive"
        );
        terminal(&mut probe);
    }
    for id in [65536, 1_000_000] {
        let chain = Chain::new(vec![Entry::new(1, &[(id, &[0])]), Entry::new(4, &[])]);
        let mut probe = chain.before_reference();
        assert_eq!(
            probe.feed(&reply(255, 2, &chain.reference)).unwrap_err(),
            "floor definition identity out of bounds"
        );
        terminal(&mut probe);
    }
}

#[test]
fn transport_eof_trailing_identity_priority_handshake_continuation_and_length_errors_reset() {
    let chain = fixture();
    let wire = chain.wire();
    for end in [0, 1, 108, 109, 110, wire.len() / 2, wire.len() - 1] {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        probe.feed(&wire[..end]).unwrap();
        assert!(probe.finish().is_err());
        terminal(&mut probe);
    }
    for kind in 0..7 {
        let mut probe = chain.begin();
        let mut bytes = reply(2, 1, &chain.archives[&1]);
        match kind {
            0 => {
                probe.feed(&bytes).unwrap();
                probe.feed(&reply(2, 4, &chain.archives[&4])).unwrap();
                bytes = vec![0];
            }
            1 => {
                bytes.extend_from_slice(&reply(2, 4, &chain.archives[&4]));
                bytes.push(0);
            }
            2 => bytes[0] = 5,
            3 => bytes[4] = 4,
            4 => bytes[5] |= 0x80,
            5 => {
                bytes = vec![2, 0, 0, 0, 1, 0];
                bytes.extend_from_slice(&u32::try_from(MAX_CONTAINER_BYTES).unwrap().to_be_bytes());
            }
            _ => bytes = reply(255, 2, &chain.reference),
        }
        assert!(probe.feed(&bytes).is_err());
        terminal(&mut probe);
    }
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    assert!(probe.feed(&[6]).is_err());
    terminal(&mut probe);
    assert!(WorldFloorProbe::new(b"bad\0token").is_err());
}

#[test]
fn original_index_two_must_exist_in_version_and_reply_identity() {
    let chain = fixture();
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    probe.feed(&[0; 109]).unwrap();
    let mut bytes = vec![2];
    bytes.extend_from_slice(&[0; 2 * 72]);
    assert_eq!(
        probe.feed(&reply(255, 255, &raw(&bytes))).unwrap_err(),
        "floor index unavailable in version table"
    );
    terminal(&mut probe);
    let mut probe = chain.before_reference();
    assert_eq!(
        probe.feed(&reply(255, 5, &chain.reference)).unwrap_err(),
        "invalid floor JS5 transfer"
    );
    terminal(&mut probe);
}

#[test]
fn inclusive_cumulative_limits_are_checked_across_feeds_and_overflow() {
    let chain = fixture();
    let bytes = reply(2, 4, &chain.archives[&4]);
    for resource in 0..4 {
        for excess in [0, 1] {
            let mut probe = chain.begin();
            probe.feed(&reply(2, 1, &chain.archives[&1])).unwrap();
            match resource {
                0 => probe.received = MAX_TOTAL_WIRE_BYTES - bytes.len() + excess,
                1 => {
                    probe.containers = MAX_TOTAL_CONTAINER_BYTES - chain.archives[&4].len() + excess
                }
                2 => {
                    probe.expanded =
                        MAX_TOTAL_EXPANDED_BYTES - (chain.archives[&4].len() - 5) + excess
                }
                _ => {
                    let expanded = chain.archives[&4].len() - 5;
                    let definition_bytes = 6 + 7;
                    let charge = bytes.len()
                        + chain.archives[&4].len()
                        + expanded * 2
                        + definition_bytes
                        + 2;
                    probe.operations = MAX_TOTAL_OPERATIONS - charge + excess;
                }
            }
            let mut result = Ok(());
            for chunk in bytes.chunks(7) {
                result = probe.feed(chunk);
                if result.is_err() {
                    break;
                }
            }
            if excess == 0 {
                result.unwrap();
                let report = probe.report().unwrap();
                let (actual, limit) = match resource {
                    0 => (report.total_wire_bytes, MAX_TOTAL_WIRE_BYTES),
                    1 => (report.total_container_bytes, MAX_TOTAL_CONTAINER_BYTES),
                    2 => (report.total_expanded_bytes, MAX_TOTAL_EXPANDED_BYTES),
                    _ => (report.total_operations, MAX_TOTAL_OPERATIONS),
                };
                assert_eq!(actual, limit);
            } else {
                assert!(result.unwrap_err().contains("cumulative"));
                terminal(&mut probe);
            }
        }
    }
    assert!(bounded_add(usize::MAX, 1, usize::MAX, "overflow").is_err());
}

#[test]
fn compression_expansion_budget_checked_before_payload_decode() {
    for mode in [1, 2] {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        probe.expanded = MAX_TOTAL_EXPANDED_BYTES - 1023;
        let container = [
            vec![mode],
            0u32.to_be_bytes().to_vec(),
            1024u32.to_be_bytes().to_vec(),
        ]
        .concat();
        assert_eq!(
            probe.decode(&container, Integrity::default()).unwrap_err(),
            "floor cumulative expanded byte limit"
        );
        probe.expanded -= 1;
        assert_eq!(
            probe.decode(&container, Integrity::default()).unwrap_err(),
            "invalid floor container or integrity check"
        );
    }
}

#[test]
fn full_reference_and_file_metadata_count_caps_are_checked_without_widening_generic_assets() {
    let mut bytes = vec![7, 0, 0, 0, 71, 0];
    smart(&mut bytes, 65537);
    let reference = raw(&bytes);
    let chain = Chain {
        version: version(&reference, 71),
        reference,
        archives: BTreeMap::new(),
    };
    let mut probe = chain.before_reference();
    assert_eq!(
        probe.feed(&reply(255, 2, &chain.reference)).unwrap_err(),
        "invalid floor reference table"
    );
    terminal(&mut probe);
    assert_eq!(assets::limits().max_archives, 65536);
    assert_eq!(assets::limits().max_file_id, 1_000_000);
}

#[test]
fn maximum_sparse_floor_identity_and_dense_definition_count_supported_boundedly() {
    let mut entry = Entry::new(1, &[]);
    entry.files = (0..=u32::from(u16::MAX)).map(|id| (id, vec![0])).collect();
    let chain = Chain::with_flags(vec![entry, Entry::new(4, &[(65535, &[0])])], false, false);
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    probe.feed(&chain.wire()).unwrap();
    let report = probe.report().unwrap();
    assert_eq!(report.underlays.populated_definitions, 65536);
    assert_eq!(report.underlays.file_slots, 65536);
    assert_eq!(report.overlays.file_slots, 65536);
    assert!(probe.underlay(65535).unwrap().present());
    assert!(probe.overlay(65535).unwrap().present());
    assert_eq!(probe.overlay(65535).unwrap().definition().slot, 65535);
    assert!(!probe.overlay(65534).unwrap().present());
}

#[test]
fn continuation_boundaries_and_inclusive_tag_limit_are_validated() {
    let mut definition = [1, 0x80, 0x20, 0x10].repeat(200);
    definition.push(0);
    let chain = Chain::new(vec![Entry::new(1, &[(0, &definition)]), Entry::new(4, &[])]);
    let wire = chain.wire();
    for size in [1, 511, 512, 513] {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        for chunk in wire.chunks(size) {
            probe.feed(chunk).unwrap();
        }
        assert!(probe.complete());
        assert_eq!(probe.underlay(0).unwrap().definition().rgb, 0x802010);
    }
    let mut probe = chain.begin();
    let mut bytes = reply(2, 1, &chain.archives[&1]);
    assert_eq!(bytes[512], 255);
    bytes[512] = 0;
    assert_eq!(
        probe.feed(&bytes).unwrap_err(),
        "invalid floor JS5 transfer"
    );
    terminal(&mut probe);
    let mut definition = vec![4; MAX_TAGS_PER_DEFINITION - 1];
    definition.push(0);
    let chain = Chain::new(vec![Entry::new(1, &[(0, &definition)]), Entry::new(4, &[])]);
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    probe.feed(&chain.wire()).unwrap();
    assert!(!probe.underlay(0).unwrap().definition().shadowed);
}

#[test]
fn pinned_native_loader_sparse_files_defaults_and_lazy_opcode8_trace_match() {
    let fixture: serde_json::Value =
        serde_json::from_str(include_str!("../../tests/fixtures/floors-native.json")).unwrap();
    assert_eq!(
        fixture["provenance"]["jarSha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    assert_eq!(
        fixture["nativeLazyOverlayIds"],
        serde_json::json!([5, 1, 5, 0, 65535])
    );
    assert_eq!(
        fixture["nativeLazyOpcode8Trace"],
        serde_json::json!([5, 1, 1, 1, 1])
    );
    let wire = include_bytes!("../../tests/fixtures/floors-native.wire");
    for size in [1, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
        for chunk in wire.chunks(size) {
            probe.feed(chunk).unwrap();
        }
        let report = probe.report().unwrap();
        assert_eq!(report.reference_revision, 727);
        assert_eq!(report.reference_archives, 4);
        assert_eq!(report.reference_files, 8);
        assert_eq!(
            u64::from(report.underlays.file_slots),
            fixture["underlayFileSlots"].as_u64().unwrap()
        );
        assert_eq!(
            u64::from(report.overlays.file_slots),
            fixture["overlayFileSlots"].as_u64().unwrap()
        );
        assert!(
            !report.authenticated && !report.rendered_world && !report.opcode8_side_effects_applied
        );
        for case in fixture["underlays"].as_array().unwrap() {
            let id = u32::try_from(case["id"].as_u64().unwrap()).unwrap();
            let lookup = probe.underlay(id).unwrap();
            let value = lookup.definition();
            assert_eq!(
                serde_json::json!({
                    "id": id, "present": lookup.present(), "rgb": value.rgb, "texture": value.texture,
                    "scale": value.scale, "shadowed": value.shadowed, "occlude": value.occlude,
                    "hue": value.hue, "saturation": value.saturation, "lightness": value.lightness,
                    "hueMultiplier": value.hue_multiplier,
                }),
                *case
            );
        }
        for case in fixture["overlays"].as_array().unwrap() {
            let id = u32::try_from(case["id"].as_u64().unwrap()).unwrap();
            let lookup = probe.overlay(id).unwrap();
            let value = lookup.definition();
            assert_eq!(
                serde_json::json!({
                    "id": id, "present": lookup.present(), "slot": value.slot,
                    "tileColorHsl": value.tile_color_hsl, "texture": value.texture, "occlude": value.occlude,
                    "minimapColorHsl": value.minimap_color_hsl, "textureScale": value.texture_scale,
                    "shadowed": value.shadowed, "blendsWithUnderlay": value.blends_with_underlay,
                    "waterColor": value.water_color, "waterFogDepth": value.water_fog_depth,
                    "waterIntensity": value.water_intensity, "opcode20": value.opcode20,
                    "unusedOpcode21": value.unused_opcode21, "unusedOpcode22": value.unused_opcode22,
                    "overlayId": value.overlay_id, "sawOpcode8": value.saw_opcode8,
                }),
                *case
            );
        }
        probe.finish().unwrap();
    }
}
