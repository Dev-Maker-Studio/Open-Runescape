use super::*;
use std::collections::BTreeMap;
use whirlpool::{Digest, Whirlpool};

fn raw(data: &[u8]) -> Vec<u8> {
    [
        vec![0],
        (data.len() as u32).to_be_bytes().to_vec(),
        data.to_vec(),
    ]
    .concat()
}

fn reply(index: u8, id: u32, container: &[u8]) -> Vec<u8> {
    let mut wire = vec![index];
    wire.extend_from_slice(&id.to_be_bytes());
    wire.extend_from_slice(&container[..5]);
    let mut offset = 10;
    for &byte in &container[5..] {
        if offset == 512 {
            wire.push(255);
            offset = 1;
        }
        wire.push(byte);
        offset += 1;
    }
    wire
}

fn smart(out: &mut Vec<u8>, value: u32) {
    if value >= 32768 {
        out.extend_from_slice(&(value | 0x80000000).to_be_bytes());
    } else {
        out.extend_from_slice(&(value as u16).to_be_bytes());
    }
}

fn tiles(height: u8, suffix: &[u8]) -> Vec<u8> {
    let mut bytes = vec![1, height];
    bytes.resize(TILE_COUNT + 1, 0);
    bytes.extend_from_slice(suffix);
    bytes
}

struct Entry {
    id: u32,
    hash: i32,
    container: Vec<u8>,
    files: Vec<u32>,
    crc: Option<u32>,
    whirlpool: Option<[u8; 64]>,
}

impl Entry {
    fn surface(region_id: u16, id: u32, bytes: &[u8]) -> Self {
        Self {
            id,
            hash: surface_name_hash(region_id, false),
            container: raw(bytes),
            files: vec![0],
            crc: None,
            whirlpool: None,
        }
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
        smart(&mut table, entries.len() as u32);
        let mut prior = 0;
        for entry in &entries {
            smart(&mut table, entry.id - prior);
            prior = entry.id;
        }
        if named {
            for entry in &entries {
                table.extend_from_slice(&entry.hash.to_be_bytes());
            }
        }
        for entry in &entries {
            table.extend_from_slice(
                &entry
                    .crc
                    .unwrap_or_else(|| crc32fast::hash(&entry.container))
                    .to_be_bytes(),
            );
        }
        if hashed {
            for entry in &entries {
                table.extend_from_slice(
                    &entry
                        .whirlpool
                        .unwrap_or_else(|| Whirlpool::digest(&entry.container).into()),
                );
            }
        }
        for _ in &entries {
            table.extend_from_slice(&42i32.to_be_bytes());
        }
        for entry in &entries {
            smart(&mut table, entry.files.len() as u32);
        }
        for entry in &entries {
            let mut prior = 0;
            for &id in &entry.files {
                smart(&mut table, id - prior);
                prior = id;
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
            version: version(&reference),
            reference,
            archives: entries
                .into_iter()
                .map(|entry| (entry.id, entry.container))
                .collect(),
        }
    }

    fn before_reference(&self, ids: &[u16]) -> WorldTerrainProbe {
        let mut probe = WorldTerrainProbe::new(b"synthetic", ids).unwrap();
        assert!(!probe.take_outgoing().is_empty());
        probe.feed(&[0; 109]).unwrap();
        assert_eq!(probe.take_outgoing().len(), 18);
        probe.feed(&reply(255, 255, &self.version)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(255, 5, js5::Priority::High)
        );
        probe
    }

    fn begin(&self, ids: &[u16]) -> WorldTerrainProbe {
        let mut probe = self.before_reference(ids);
        probe.feed(&reply(255, 5, &self.reference)).unwrap();
        probe
    }

    fn wire(&self, archive_order: &[u32]) -> Vec<u8> {
        let mut wire = vec![0; 109];
        wire.extend_from_slice(&reply(255, 255, &self.version));
        wire.extend_from_slice(&reply(255, 5, &self.reference));
        for id in archive_order {
            wire.extend_from_slice(&reply(5, *id, &self.archives[id]));
        }
        wire
    }
}

fn version(reference: &[u8]) -> Vec<u8> {
    let mut bytes = vec![6];
    bytes.extend_from_slice(&[0; 5 * 72]);
    bytes.extend_from_slice(&crc32fast::hash(reference).to_be_bytes());
    bytes.extend_from_slice(&71i32.to_be_bytes());
    bytes.extend_from_slice(&Whirlpool::digest(reference));
    bytes.extend_from_slice(&[0x5a; 128]);
    raw(&bytes)
}

fn terminal(probe: &mut WorldTerrainProbe) {
    assert!(!probe.complete());
    assert!(probe.report().is_none());
    assert!(probe.terrain(0x1234).is_err());
    assert!(probe.height(0x1234, 0, 0, 0).is_err());
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.terrains.is_empty());
    assert!(probe.regions.is_empty());
    assert!(probe.region_ids.is_empty());
    assert!(probe.transfer.is_none());
    assert_eq!((probe.reference_archives, probe.reference_files), (0, 0));
    assert_eq!(
        (
            probe.received,
            probe.containers,
            probe.expanded,
            probe.operations
        ),
        (0, 0, 0, 0)
    );
    assert!(probe.feed(&[]).is_err());
    assert!(probe.finish().is_err());
}

#[test]
fn fragments_preserve_supplied_order_verified_tiles_suffix_and_native_origins() {
    let first = tiles(7, &[0xfe, 0x81, 0x22]);
    let second = tiles(1, &[]);
    let chain = Chain::new(vec![
        Entry::surface(0xffff, 90001, &first),
        Entry::surface(0x1234, 21, &second),
    ]);
    let wire = chain.wire(&[90001, 21]);
    for size in [1, 2, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = WorldTerrainProbe::new(b"synthetic", &[0xffff, 0, 0x1234]).unwrap();
        assert!(probe.terrain(0xffff).is_err());
        for chunk in wire.chunks(size) {
            probe.feed(chunk).unwrap();
        }
        let report = probe.report().unwrap();
        assert_eq!(report.index, 5);
        assert_eq!(report.reference_revision, 71);
        assert_eq!(
            (
                report.reference_archives,
                report.reference_files,
                report.retained_archives
            ),
            (2, 2, 2)
        );
        assert!(report.reference_crc_verified && report.reference_whirlpool_verified);
        assert!(!report.authenticated);
        assert_eq!(report.total_wire_bytes, wire.len());
        assert_eq!(
            report.total_container_bytes,
            chain.version.len()
                + chain.reference.len()
                + chain.archives.values().map(Vec::len).sum::<usize>()
        );
        assert_eq!(
            report.total_expanded_bytes,
            report.total_container_bytes - 4 * 5
        );
        assert_eq!(
            report
                .regions
                .iter()
                .map(|region| region.region_id)
                .collect::<Vec<_>>(),
            [0xffff, 0, 0x1234]
        );
        assert_eq!(report.regions[0].origin, [16320, 16320]);
        assert_eq!(report.regions[2].origin, [1152, 3328]);
        assert_eq!(report.regions[0].archive_id, Some(90001));
        assert_eq!(report.regions[0].terrain_bytes, TILE_COUNT + 1);
        assert_eq!(report.regions[0].suffix_bytes, 3);
        assert_eq!(report.regions[0].tile_count, TILE_COUNT);
        assert!(
            report.regions[0].present
                && report.regions[0].crc_verified
                && report.regions[0].whirlpool_verified
        );
        assert!(!report.regions[0].effects_decoded);
        assert!(!report.regions[1].present);
        assert!(report.regions[1].archive_id.is_none());
        assert_eq!(report.regions[1].tile_count, 0);
        assert!(!report.regions[1].crc_verified);
        assert!(probe.terrain(0).is_err());
        assert!(probe.terrain(1).is_err());
        assert_eq!(probe.height(0xffff, 0, 0, 0).unwrap(), -224);
        assert_eq!(probe.height(0xffff, 1, 0, 0).unwrap(), -1184);
        assert_eq!(probe.height(0x1234, 0, 0, 0).unwrap(), 0);
        assert!(probe.height(0xffff, 4, 0, 0).is_err());
        assert!(probe.height(0xffff, 0, 64, 0).is_err());
        assert_eq!(probe.terrain(0xffff).unwrap().tiles().len(), TILE_COUNT);
        assert_eq!(report.limits.max_regions, 36);
        assert_eq!(report.limits.max_reference_archives, 262_144);
        assert_eq!(report.limits.max_reference_files, 262_144);
        assert_eq!(report.limits.max_reference_records, 524_288);
        assert_eq!(report.limits.max_retained_archives, 36);
        assert_eq!(report.limits.max_retained_files, 4096);
        assert_eq!(report.limits.max_tags_per_tile, 256);
        assert!(report.limits.tile_tag_limit_is_diagnostic);
        assert!(report.total_operations <= report.limits.max_total_operations);
        let json = serde_json::to_string(report).unwrap();
        assert!(!json.contains("synthetic"));
        assert!(!json.contains("m255_255"));
        assert!(!json.contains("whirlpool\":"));
        probe.finish().unwrap();
        probe.finish().unwrap();
        probe.feed(&[]).unwrap();
    }
}

#[test]
fn requests_are_sequential_even_with_missing_regions_and_sparse_archive_ids() {
    let chain = Chain::new(vec![
        Entry::surface(0x0102, 7, &tiles(2, &[])),
        Entry::surface(0x0304, 80000, &tiles(3, &[])),
    ]);
    let mut probe = chain.begin(&[0x0304, 0x9999, 0x0102]);
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(5, 80000, js5::Priority::High)
    );
    assert!(probe.take_outgoing().is_empty());
    probe
        .feed(&reply(5, 80000, &chain.archives[&80000]))
        .unwrap();
    assert!(!probe.complete());
    assert!(probe.terrain(0x0304).is_err());
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(5, 7, js5::Priority::High)
    );
    probe.feed(&reply(5, 7, &chain.archives[&7])).unwrap();
    assert!(probe.complete());
    assert!(probe.take_outgoing().is_empty());
}

#[test]
fn verified_named_absence_is_not_nameless_unavailability_or_synthetic_geometry() {
    let empty = Chain::new(vec![]);
    let mut probe = empty.begin(&[0, u16::MAX]);
    assert!(probe.complete());
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.terrains.is_empty());
    assert!(
        probe
            .report()
            .unwrap()
            .regions
            .iter()
            .all(|region| !region.present && region.archive_id.is_none() && region.tile_count == 0)
    );
    for entries in [vec![], vec![Entry::surface(0, 1, &tiles(2, &[]))]] {
        let chain = Chain::with_flags(entries, false, true);
        let mut probe = chain.before_reference(&[0]);
        assert_eq!(
            probe.feed(&reply(255, 5, &chain.reference)).unwrap_err(),
            "terrain named reference table unavailable"
        );
        terminal(&mut probe);
    }
}

#[test]
fn absent_archive_whirlpool_is_reported_without_claiming_authentication() {
    let chain = Chain::with_flags(vec![Entry::surface(0, 1, &tiles(2, &[]))], true, false);
    let mut probe = chain.begin(&[0]);
    probe.feed(&reply(5, 1, &chain.archives[&1])).unwrap();
    let report = probe.report().unwrap();
    assert!(report.reference_whirlpool_verified);
    assert!(report.regions[0].crc_verified);
    assert!(!report.regions[0].whirlpool_verified);
    assert!(!report.authenticated);
}

#[test]
fn duplicate_requested_hashes_fail_but_unrelated_collisions_allow_verified_lookup_and_absence() {
    let mut other = Entry::surface(0x1234, 2, &tiles(2, &[]));
    other.hash = surface_name_hash(0x5555, false);
    let chain = Chain::new(vec![
        Entry::surface(0x5555, 1, &tiles(3, &[])),
        other,
        Entry::surface(0, 3, &tiles(4, &[])),
    ]);
    let mut probe = chain.before_reference(&[0x5555]);
    assert_eq!(
        probe.feed(&reply(255, 5, &chain.reference)).unwrap_err(),
        "invalid terrain reference table"
    );
    terminal(&mut probe);
    let mut probe = chain.begin(&[0x9999, 0]);
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(5, 3, js5::Priority::High)
    );
    probe.feed(&reply(5, 3, &chain.archives[&3])).unwrap();
    let report = probe.report().unwrap();
    assert_eq!(
        (
            report.reference_archives,
            report.reference_files,
            report.retained_archives
        ),
        (3, 3, 1)
    );
    assert!(!report.regions[0].present);
    assert_eq!(probe.height(0, 0, 0, 0).unwrap(), -128);
}

#[test]
fn crc_and_whirlpool_failures_drop_previously_decoded_terrain_and_transfer_buffers() {
    for kind in 0..3 {
        let mut entry = Entry::surface(0x1234, 2, &tiles(2, &[]));
        match kind {
            0 => entry.crc = Some(0xdeadbeef),
            1 => entry.whirlpool = Some([0xa5; 64]),
            _ => {}
        }
        let chain = Chain::new(vec![Entry::surface(0, 1, &tiles(3, &[])), entry]);
        let mut probe = chain.begin(&[0, 0x1234]);
        probe.feed(&reply(5, 1, &chain.archives[&1])).unwrap();
        assert_eq!(probe.terrains.len(), 1);
        let mut archive = chain.archives[&2].clone();
        if kind == 2 {
            *archive.last_mut().unwrap() ^= 1;
        }
        assert_eq!(
            probe.feed(&reply(5, 2, &archive)).unwrap_err(),
            "invalid terrain container or integrity check"
        );
        terminal(&mut probe);
    }
}

#[test]
fn reference_integrity_and_metadata_errors_never_echo_field_contents() {
    for kind in 0..4 {
        let mut chain = Chain::new(vec![Entry::surface(0, 1, &tiles(2, &[]))]);
        match kind {
            0 => *chain.reference.last_mut().unwrap() ^= 1,
            1 => chain.version[5 + 1 + 5 * 72 + 8] ^= 1,
            2 => {
                chain.reference[5] = 249;
                chain.version = version(&chain.reference);
            }
            _ => {
                chain.reference[10] = 255;
                chain.version = version(&chain.reference);
            }
        }
        let mut probe = chain.before_reference(&[0]);
        let error = probe.feed(&reply(255, 5, &chain.reference)).unwrap_err();
        assert!(matches!(
            error.as_str(),
            "invalid terrain container or integrity check" | "invalid terrain reference table"
        ));
        assert!(!error.contains("249") && !error.contains("255") && !error.contains("deadbeef"));
        terminal(&mut probe);
    }
}

#[test]
fn missing_file_zero_empty_archives_and_invalid_chunk_tables_are_terminal() {
    for files in [vec![], vec![3], vec![0, 1]] {
        let mut entry = Entry::surface(0x1234, 1, &[0, 0]);
        entry.files = files.clone();
        let chain = Chain::new(vec![entry]);
        let mut probe = chain.before_reference(&[0x1234]);
        let reference = probe.feed(&reply(255, 5, &chain.reference));
        if files.contains(&0) {
            reference.unwrap();
            assert_eq!(
                probe.feed(&reply(5, 1, &chain.archives[&1])).unwrap_err(),
                "invalid terrain archive"
            );
        } else {
            assert_eq!(
                reference.unwrap_err(),
                "terrain archive file zero unavailable"
            );
        }
        terminal(&mut probe);
    }
}

#[test]
fn malformed_tiles_and_diagnostic_tag_and_file_bounds_fail_closed() {
    let mut too_many_tags = vec![82; MAX_TAGS_PER_TILE];
    too_many_tags.extend_from_slice(&vec![0; TILE_COUNT]);
    for data in [
        vec![],
        vec![0; TILE_COUNT - 1],
        vec![1],
        vec![2],
        too_many_tags,
        vec![0; MAX_TERRAIN_FILE_BYTES + 1],
    ] {
        let chain = Chain::new(vec![Entry::surface(0x1234, 1, &data)]);
        let mut probe = chain.begin(&[0x1234]);
        assert_eq!(
            probe.feed(&reply(5, 1, &chain.archives[&1])).unwrap_err(),
            "invalid terrain tile stream or diagnostic bound exceeded"
        );
        terminal(&mut probe);
    }
    let mut data = vec![82; MAX_TAGS_PER_TILE - 1];
    data.resize(data.len() + TILE_COUNT, 0);
    data.resize(MAX_TERRAIN_FILE_BYTES, 0xfb);
    let chain = Chain::new(vec![Entry::surface(0, 1, &data)]);
    let mut probe = chain.begin(&[0]);
    probe.feed(&reply(5, 1, &chain.archives[&1])).unwrap();
    assert_eq!(
        probe.terrain(0).unwrap().tile(0, 0, 0).unwrap().underlay_id,
        1
    );
    assert_eq!(
        probe.report().unwrap().regions[0].terrain_bytes,
        TILE_COUNT + MAX_TAGS_PER_TILE - 1
    );
    assert_eq!(
        probe.report().unwrap().regions[0].expanded_bytes,
        MAX_TERRAIN_FILE_BYTES
    );
}

#[test]
fn input_regions_are_validated_before_transfer_or_allocation_and_maximum_is_supported() {
    assert!(
        WorldTerrainProbe::new(b"", &[])
            .err()
            .unwrap()
            .contains("count")
    );
    assert!(
        WorldTerrainProbe::new(b"", &[1, 1])
            .err()
            .unwrap()
            .contains("duplicate")
    );
    assert!(
        WorldTerrainProbe::new(b"", &[0; MAX_REGIONS + 1])
            .err()
            .unwrap()
            .contains("count")
    );
    let ids: Vec<u16> = (0..MAX_REGIONS as u16).rev().collect();
    let chain = Chain::new(vec![]);
    let probe = chain.begin(&ids);
    assert_eq!(
        probe
            .report()
            .unwrap()
            .regions
            .iter()
            .map(|region| region.region_id)
            .collect::<Vec<_>>(),
        ids
    );
}

#[test]
fn eof_trailing_identity_priority_handshake_and_continuation_errors_are_terminal() {
    let chain = Chain::new(vec![Entry::surface(0x1234, 1, &tiles(2, &[]))]);
    let wire = chain.wire(&[1]);
    for end in [0, 1, 108, 109, 110, wire.len() / 2, wire.len() - 1] {
        let mut probe = WorldTerrainProbe::new(b"synthetic", &[0x1234]).unwrap();
        probe.feed(&wire[..end]).unwrap();
        assert!(probe.finish().is_err());
        terminal(&mut probe);
    }
    for kind in 0..7 {
        let mut probe = chain.begin(&[0x1234]);
        let mut bytes = reply(5, 1, &chain.archives[&1]);
        match kind {
            0 => {
                probe.feed(&bytes).unwrap();
                bytes = vec![0];
            }
            1 => bytes.push(0),
            2 => bytes[0] = 4,
            3 => bytes[4] = 2,
            4 => bytes[5] |= 0x80,
            5 => bytes[512] = 0,
            _ => {
                bytes = vec![5, 0, 0, 0, 1, 0];
                bytes.extend_from_slice(&(MAX_CONTAINER_BYTES as u32).to_be_bytes());
            }
        }
        assert!(probe.feed(&bytes).is_err());
        terminal(&mut probe);
    }
    let mut probe = WorldTerrainProbe::new(b"synthetic", &[0x1234]).unwrap();
    assert!(probe.feed(&[6]).is_err());
    terminal(&mut probe);
}

#[test]
fn cumulative_budgets_are_inclusive_checked_across_feeds_and_drop_partial_state() {
    let chain = Chain::new(vec![Entry::surface(0x1234, 1, &tiles(2, &[]))]);
    let bytes = reply(5, 1, &chain.archives[&1]);
    for resource in 0..4 {
        for excess in [0, 1] {
            let mut probe = chain.begin(&[0x1234]);
            match resource {
                0 => probe.received = MAX_TOTAL_WIRE_BYTES - bytes.len() + excess,
                1 => {
                    probe.containers = MAX_TOTAL_CONTAINER_BYTES - chain.archives[&1].len() + excess
                }
                2 => {
                    probe.expanded =
                        MAX_TOTAL_EXPANDED_BYTES - (chain.archives[&1].len() - 5) + excess
                }
                _ => {
                    let expanded = chain.archives[&1].len() - 5;
                    let charge = bytes.len() + chain.archives[&1].len() + expanded * 3 + TILE_COUNT;
                    probe.operations = MAX_TOTAL_OPERATIONS - charge + excess;
                }
            }
            let mut result = Ok(());
            for chunk in bytes.chunks(511) {
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
fn compressed_expansion_budget_is_checked_before_attempting_payload_decode() {
    for mode in [1, 2] {
        let mut probe = WorldTerrainProbe::new(b"synthetic", &[0x1234]).unwrap();
        probe.expanded = MAX_TOTAL_EXPANDED_BYTES - 1023;
        let container = [
            vec![mode],
            0u32.to_be_bytes().to_vec(),
            1024u32.to_be_bytes().to_vec(),
        ]
        .concat();
        assert_eq!(
            probe.decode(&container, Integrity::default()).unwrap_err(),
            "terrain cumulative expanded byte limit"
        );
        probe.expanded -= 1;
        assert_eq!(
            probe.decode(&container, Integrity::default()).unwrap_err(),
            "invalid terrain container or integrity check"
        );
    }
}

#[test]
fn split_archive_counts_discarded_files_and_chunk_table_against_expansion_budget() {
    let terrain = tiles(7, &[0xfa, 0xfb]);
    let extra_len = 20000;
    let mut data = terrain.clone();
    data.resize(data.len() + extra_len, 0x5a);
    data.extend_from_slice(&(terrain.len() as i32).to_be_bytes());
    data.extend_from_slice(&(extra_len as i32 - terrain.len() as i32).to_be_bytes());
    data.push(1);
    let mut entry = Entry::surface(0x1234, 1, &data);
    entry.files = vec![0, 3];
    let chain = Chain::new(vec![entry]);
    for excess in [0, 1] {
        let mut probe = chain.begin(&[0x1234]);
        probe.expanded = MAX_TOTAL_EXPANDED_BYTES - data.len() + excess;
        let result = probe.feed(&reply(5, 1, &chain.archives[&1]));
        if excess == 0 {
            result.unwrap();
            let report = probe.report().unwrap();
            assert_eq!(report.total_expanded_bytes, MAX_TOTAL_EXPANDED_BYTES);
            assert_eq!(report.regions[0].expanded_bytes, data.len());
            assert_eq!(report.regions[0].terrain_bytes, terrain.len() - 2);
            assert_eq!(report.regions[0].suffix_bytes, 2);
            assert_eq!(probe.height(0x1234, 0, 0, 0).unwrap(), -224);
        } else {
            assert_eq!(
                result.unwrap_err(),
                "terrain cumulative expanded byte limit"
            );
            terminal(&mut probe);
        }
    }
}

#[test]
fn pinned_native_name_lookup_and_gzip_chain_agree_without_native_startup() {
    let fixture: serde_json::Value =
        serde_json::from_str(include_str!("../../tests/fixtures/terrain-native.json")).unwrap();
    assert_eq!(
        fixture["provenance"]["jarSha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    let ids: Vec<u16> = fixture["provenance"]["requestedRegions"]
        .as_array()
        .unwrap()
        .iter()
        .map(|id| id.as_u64().unwrap() as u16)
        .collect();
    let wire = include_bytes!("../../tests/fixtures/terrain-native.wire");
    for size in [1, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = WorldTerrainProbe::new(b"synthetic", &ids).unwrap();
        for chunk in wire.chunks(size) {
            probe.feed(chunk).unwrap();
        }
        let report = probe.report().unwrap();
        assert!(!report.authenticated);
        assert_eq!(report.reference_revision, 727);
        assert_eq!(report.regions[0].suffix_bytes, 3);
        assert!(!report.regions[0].effects_decoded);
        assert!(!report.regions[1].present);
        for case in fixture["cases"].as_array().unwrap() {
            let id = case["regionId"].as_u64().unwrap() as u16;
            assert_eq!(
                i64::from(surface_name_hash(id, false)),
                case["hash"].as_i64().unwrap()
            );
            if ids.contains(&id) {
                let region = report
                    .regions
                    .iter()
                    .find(|region| region.region_id == id)
                    .unwrap();
                assert_eq!(
                    region.archive_id.map_or(-1, i64::from),
                    case["archiveId"].as_i64().unwrap()
                );
            }
        }
        for region in fixture["regions"].as_array().unwrap() {
            let id = region["regionId"].as_u64().unwrap() as u16;
            let raw_height = region["height"].as_i64().unwrap() as i32;
            let expected = if raw_height == 1 { 0 } else { -32 * raw_height };
            assert_eq!(probe.height(id, 0, 0, 0).unwrap(), expected);
            let actual = report
                .regions
                .iter()
                .find(|region| region.region_id == id)
                .unwrap();
            assert_eq!(
                actual.expanded_bytes as u64,
                region["fileBytes"].as_u64().unwrap()
            );
            assert!(actual.crc_verified && actual.whirlpool_verified);
        }
        probe.finish().unwrap();
    }
}

fn dense_chain() -> Chain {
    let reference =
        include_bytes!("../../tests/fixtures/terrain-reference-dense.container").to_vec();
    Chain {
        version: version(&reference),
        reference,
        archives: BTreeMap::from([(262_143, raw(&tiles(2, &[])))]),
    }
}

#[test]
fn dense_reference_is_scanned_fully_but_only_requested_metadata_and_terrain_are_retained() {
    let chain = dense_chain();
    let mut probe = chain.before_reference(&[0, 1]);
    for chunk in reply(255, 5, &chain.reference).chunks(4093) {
        probe.feed(chunk).unwrap();
    }
    assert_eq!(
        (probe.reference_archives, probe.reference_files),
        (262_144, 262_144)
    );
    let Stage::Archive { table, region } = &probe.stage else {
        panic!("archive request missing")
    };
    assert_eq!(*region, 0);
    assert_eq!(table.archives.len(), 1);
    assert_eq!(table.archives[0].id, 262_143);
    assert_eq!(table.archives[0].files.len(), 1);
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(5, 262_143, js5::Priority::High)
    );
    probe
        .feed(&reply(5, 262_143, &chain.archives[&262_143]))
        .unwrap();
    let report = probe.report().unwrap();
    assert_eq!(
        (
            report.reference_archives,
            report.reference_files,
            report.retained_archives
        ),
        (262_144, 262_144, 1)
    );
    assert_eq!(report.limits.max_reference_archives, 262_144);
    assert_eq!(report.limits.max_reference_files, 262_144);
    assert_eq!(report.limits.max_reference_records, 524_288);
    assert_eq!(report.limits.max_retained_archives, 36);
    assert_eq!(report.limits.max_retained_files, 4096);
    assert_eq!(probe.terrains.len(), 1);
    assert_eq!(probe.height(0, 0, 0, 0).unwrap(), -64);
    assert!(probe.terrain(1).is_err());
    assert!(!report.regions[1].present);
    assert!(report.total_operations >= 5_767_178 + 262_144 * 6);
    assert!(report.total_operations < 32 * 1024 * 1024);
    assert_eq!(assets::limits().max_archives, 65_536);
    let json = serde_json::to_value(report).unwrap();
    assert_eq!(json["referenceArchives"], 262_144);
    assert_eq!(json["referenceFiles"], 262_144);
    assert_eq!(json["retainedArchives"], 1);
    assert_eq!(json["limits"]["maxReferenceRecords"], 524_288);
}

#[test]
fn full_unretained_reference_scan_is_included_in_inclusive_operation_budget() {
    let chain = dense_chain();
    let wire = reply(255, 5, &chain.reference);
    let expanded = 5_767_178;
    let charge = wire.len() + chain.reference.len() + expanded * 2 + 262_144 * 5;
    for excess in [0, 1] {
        let mut probe = chain.before_reference(&[1]);
        probe.operations = MAX_TOTAL_OPERATIONS - charge + excess;
        let mut result = Ok(());
        for chunk in wire.chunks(8191) {
            result = probe.feed(chunk);
            if result.is_err() {
                break;
            }
        }
        if excess == 0 {
            result.unwrap();
            let report = probe.report().unwrap();
            assert_eq!(report.total_operations, MAX_TOTAL_OPERATIONS);
            assert_eq!(
                (
                    report.reference_archives,
                    report.reference_files,
                    report.retained_archives
                ),
                (262_144, 262_144, 0)
            );
            assert!(probe.terrains.is_empty());
        } else {
            assert_eq!(result.unwrap_err(), "terrain cumulative operation limit");
            terminal(&mut probe);
        }
    }
}

#[test]
fn projected_reference_caps_and_cumulative_retained_file_bound_fail_closed() {
    let mut header = vec![7, 0, 0, 0, 71, 1];
    smart(&mut header, 262_145);
    let reference = raw(&header);
    let chain = Chain {
        version: version(&reference),
        reference,
        archives: BTreeMap::new(),
    };
    let mut probe = chain.before_reference(&[0]);
    assert_eq!(
        probe.feed(&reply(255, 5, &chain.reference)).unwrap_err(),
        "invalid terrain reference table"
    );
    terminal(&mut probe);
    for counts in [vec![4097], vec![2048, 2049], vec![52_429; 5]] {
        let entries = counts
            .iter()
            .enumerate()
            .map(|(index, &count)| {
                let region = if index == counts.len() - 1 {
                    0
                } else {
                    index as u16 + 1
                };
                let mut entry = Entry::surface(region, index as u32, &tiles(2, &[]));
                entry.files = (0..count as u32).collect();
                entry
            })
            .collect();
        let chain = Chain::with_flags(entries, true, false);
        let ids: Vec<u16> = if counts.len() == 2 {
            vec![0, 1]
        } else {
            vec![0]
        };
        let mut probe = chain.before_reference(&ids);
        assert_eq!(
            probe.feed(&reply(255, 5, &chain.reference)).unwrap_err(),
            "invalid terrain reference table"
        );
        terminal(&mut probe);
    }
    let mut entry = Entry::surface(0, 0, &tiles(2, &[]));
    entry.files = (0..4096).collect();
    let chain = Chain::with_flags(vec![entry], true, false);
    let probe = chain.begin(&[0]);
    let Stage::Archive { table, .. } = &probe.stage else {
        panic!("archive request missing")
    };
    assert_eq!(table.archives[0].files.len(), 4096);
    let mut other = Entry::surface(1, 1, &tiles(2, &[]));
    other.files = (0..4097).collect();
    let chain = Chain::with_flags(
        vec![Entry::surface(0, 0, &tiles(2, &[])), other],
        true,
        false,
    );
    let mut probe = chain.begin(&[0]);
    probe.feed(&reply(5, 0, &chain.archives[&0])).unwrap();
    assert_eq!(
        (
            probe.report().unwrap().reference_archives,
            probe.report().unwrap().reference_files,
            probe.report().unwrap().retained_archives
        ),
        (2, 4098, 1)
    );
}

#[test]
fn malformed_unselected_reference_records_are_terminal_and_never_become_proven_absence() {
    let mut other = Entry::surface(1, 1, &tiles(2, &[]));
    other.files = vec![0, 1];
    let chain = Chain::with_flags(
        vec![Entry::surface(0, 0, &tiles(2, &[])), other],
        true,
        false,
    );
    for kind in 0..4 {
        let mut reference = chain.reference.clone();
        match kind {
            0 => reference[15..17].fill(0),
            1 => reference[49..51].fill(0),
            2 => reference[43..45].copy_from_slice(&3u16.to_be_bytes()),
            _ => {
                reference.pop();
            }
        }
        let length = reference.len() - 5;
        reference[1..5].copy_from_slice(&(length as u32).to_be_bytes());
        let invalid = Chain {
            version: version(&reference),
            reference,
            archives: BTreeMap::new(),
        };
        for ids in [&[0][..], &[0xffff][..]] {
            let mut probe = invalid.before_reference(ids);
            assert_eq!(
                probe.feed(&reply(255, 5, &invalid.reference)).unwrap_err(),
                "invalid terrain reference table"
            );
            terminal(&mut probe);
        }
    }
}

#[test]
fn version_table_requires_original_protocol_maps_index() {
    let chain = Chain::new(vec![]);
    let mut probe = WorldTerrainProbe::new(b"synthetic", &[0x1234]).unwrap();
    probe.feed(&[0; 109]).unwrap();
    let mut bytes = vec![5];
    bytes.extend_from_slice(&[0; 5 * 72]);
    assert_eq!(
        probe.feed(&reply(255, 255, &raw(&bytes))).unwrap_err(),
        "terrain index unavailable in version table"
    );
    terminal(&mut probe);
    let mut probe = WorldTerrainProbe::new(b"synthetic", &[0x1234]).unwrap();
    probe.feed(&[0; 109]).unwrap();
    let mut malformed = chain.version.clone();
    malformed[5] = 255;
    assert_eq!(
        probe.feed(&reply(255, 255, &malformed)).unwrap_err(),
        "invalid terrain version table"
    );
    terminal(&mut probe);
}
