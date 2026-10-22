use super::*;
use openrs_assets::{Limits, decode_container};
use std::collections::BTreeMap;
use whirlpool::{Digest, Whirlpool};

const REFERENCE: &[u8] = include_bytes!("../../../scene/tests/fixtures/loc-reference.container");
const XTEA: &str = include_str!("../../../scene/tests/fixtures/loc-xtea.tsv");
const REGIONS: [(u16, &[u8]); 4] = [
    (
        32906,
        include_bytes!("../../../scene/tests/fixtures/loc-l32906.container"),
    ),
    (
        32907,
        include_bytes!("../../../scene/tests/fixtures/loc-l32907.container"),
    ),
    (
        33162,
        include_bytes!("../../../scene/tests/fixtures/loc-l33162.container"),
    ),
    (
        33163,
        include_bytes!("../../../scene/tests/fixtures/loc-l33163.container"),
    ),
];
const ENCRYPTED_32906: [&[u8]; 3] = [
    include_bytes!("../../../scene/tests/fixtures/loc-xtea-l32906-0.bin"),
    include_bytes!("../../../scene/tests/fixtures/loc-xtea-l32906-1.bin"),
    include_bytes!("../../../scene/tests/fixtures/loc-xtea-l32906-2.bin"),
];
const DEFINITIONS: [(u32, &[u8]); 6] = [
    (
        76,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-76.container"),
    ),
    (
        81,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-81.container"),
    ),
    (
        196,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-196.container"),
    ),
    (
        241,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-241.container"),
    ),
    (
        252,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-252.container"),
    ),
    (
        288,
        include_bytes!("../../../scene/tests/fixtures/loc-defs-288.container"),
    ),
];

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

fn trimmed(container: &[u8]) -> Vec<u8> {
    trim_revision(container).unwrap().to_vec()
}

fn payload(container: &[u8]) -> Vec<u8> {
    decode_container(
        &trimmed(container),
        Integrity::default(),
        &Limits::default(),
    )
    .unwrap()
}

fn keys() -> Vec<[i32; 4]> {
    XTEA.lines()
        .map(|line| line.split('\t').collect::<Vec<_>>())
        .filter(|f| f[0] == "KEY")
        .map(|f| {
            let mut key = [0i32; 4];
            for (slot, value) in key.iter_mut().zip(&f[2..6]) {
                *slot = value.parse().unwrap();
            }
            key
        })
        .collect()
}

struct Entry {
    id: u32,
    hash: i32,
    container: Vec<u8>,
}

fn named_reference(entries: &[Entry]) -> Vec<u8> {
    let mut sorted: Vec<&Entry> = entries.iter().collect();
    sorted.sort_by_key(|entry| entry.id);
    let mut table = vec![7];
    table.extend_from_slice(&71i32.to_be_bytes());
    table.push(3);
    smart(&mut table, sorted.len() as u32);
    let mut prior = 0;
    for entry in &sorted {
        smart(&mut table, entry.id - prior);
        prior = entry.id;
    }
    for entry in &sorted {
        table.extend_from_slice(&entry.hash.to_be_bytes());
    }
    for entry in &sorted {
        table.extend_from_slice(&crc32fast::hash(&entry.container).to_be_bytes());
    }
    for entry in &sorted {
        table.extend_from_slice(&Whirlpool::digest(&entry.container));
    }
    for _ in &sorted {
        table.extend_from_slice(&42i32.to_be_bytes());
    }
    for _ in &sorted {
        smart(&mut table, 1);
    }
    for _ in &sorted {
        smart(&mut table, 0);
    }
    for _ in &sorted {
        table.extend_from_slice(&0i32.to_be_bytes());
    }
    raw(&table)
}

fn plain_reference(ids: &[u32], containers: &BTreeMap<u32, Vec<u8>>) -> Vec<u8> {
    let mut table = vec![7];
    table.extend_from_slice(&71i32.to_be_bytes());
    table.push(2);
    smart(&mut table, ids.len() as u32);
    let mut prior = 0;
    for id in ids {
        smart(&mut table, id - prior);
        prior = *id;
    }
    for id in ids {
        table.extend_from_slice(&crc32fast::hash(&containers[id]).to_be_bytes());
    }
    for id in ids {
        table.extend_from_slice(&Whirlpool::digest(&containers[id]));
    }
    for _ in ids {
        table.extend_from_slice(&42i32.to_be_bytes());
    }
    for _ in ids {
        smart(&mut table, 1);
    }
    for _ in ids {
        smart(&mut table, 0);
    }
    raw(&table)
}

fn version(references: &BTreeMap<u8, Vec<u8>>) -> Vec<u8> {
    let mut bytes = vec![17];
    for index in 0..17u8 {
        match references.get(&index) {
            Some(reference) => {
                bytes.extend_from_slice(&crc32fast::hash(reference).to_be_bytes());
                bytes.extend_from_slice(&71i32.to_be_bytes());
                bytes.extend_from_slice(&Whirlpool::digest(reference));
            }
            None => bytes.extend_from_slice(&[0; 72]),
        }
    }
    bytes.extend_from_slice(&[0x5a; 128]);
    raw(&bytes)
}

fn expected_types(payloads: &[Vec<u8>]) -> BTreeSet<i32> {
    payloads
        .iter()
        .flat_map(|data| decode_region_locs(data).unwrap())
        .map(|placement| placement.id)
        .collect()
}

fn usmart(out: &mut Vec<u8>, value: u32) {
    if value < 128 {
        out.push(value as u8);
    } else {
        out.extend_from_slice(&((value + 32768) as u16).to_be_bytes());
    }
}

fn modeled_types(count: usize) -> Vec<i32> {
    let reference = parse_reference_table(&payload(REFERENCE), &Limits::default()).unwrap();
    let mut found = Vec::new();
    for (archive_id, container) in DEFINITIONS {
        let archive = reference.archive(archive_id).unwrap();
        for file in split_archive(&payload(container), &archive.files, &Limits::default()).unwrap()
        {
            let id = ((archive_id << 8) | file.id) as i32;
            if !type_model_ids(id, &file.data).is_empty() {
                found.push(id);
            }
        }
    }
    found.sort_unstable();
    found.into_iter().step_by(37).take(count).collect()
}

fn synthetic_payload(ids: &[i32]) -> Vec<u8> {
    let mut out = Vec::new();
    let mut previous = -1i32;
    for id in ids {
        let mut offset = (id - previous) as u32;
        while offset >= 32767 {
            usmart(&mut out, 32767);
            offset -= 32767;
        }
        assert!(offset > 0);
        usmart(&mut out, offset);
        previous = *id;
        usmart(&mut out, 66);
        out.push((10 << 2) | 1);
        usmart(&mut out, 65);
        out.push((22 << 2) | 3);
        usmart(&mut out, 0);
    }
    out.push(0);
    out
}

fn fixture_types(wanted: &BTreeSet<i32>) -> BTreeMap<i32, Vec<u8>> {
    let reference = parse_reference_table(&payload(REFERENCE), &Limits::default()).unwrap();
    let mut found = BTreeMap::new();
    for (archive_id, container) in DEFINITIONS {
        let archive = reference.archive(archive_id).unwrap();
        for file in split_archive(&payload(container), &archive.files, &Limits::default()).unwrap()
        {
            let id = ((archive_id << 8) | file.id) as i32;
            if wanted.contains(&id) {
                found.insert(id, file.data);
            }
        }
    }
    found
}

fn expected_models(types: &BTreeMap<i32, Vec<u8>>) -> BTreeSet<i32> {
    types
        .iter()
        .flat_map(|(id, bytes)| type_model_ids(*id, bytes))
        .collect()
}

struct Server {
    wire: Vec<u8>,
    region_ids: Vec<u16>,
    models: BTreeSet<i32>,
}

fn serve(regions: &[(u16, Vec<u8>)], payloads: &[Vec<u8>]) -> Server {
    let mut maps = Vec::new();
    for (at, (id, container)) in regions.iter().enumerate() {
        maps.push(Entry {
            id: 1000 + at as u32 * 17,
            hash: loc_name_hash(*id),
            container: container.clone(),
        });
    }
    let map_reference = named_reference(&maps);
    let mut references = BTreeMap::new();
    references.insert(5u8, map_reference.clone());
    let region_ids: Vec<u16> = regions.iter().map(|region| region.0).collect();
    let wanted = expected_types(payloads);
    let types = fixture_types(&wanted);
    let models = expected_models(&types);
    let mut model_containers: BTreeMap<u32, Vec<u8>> = BTreeMap::new();
    for id in &models {
        model_containers.insert(*id as u32, raw(&id.to_be_bytes()));
    }
    let model_ids: Vec<u32> = model_containers.keys().copied().collect();
    let model_reference = plain_reference(&model_ids, &model_containers);
    let def_reference = trimmed(REFERENCE);
    if !wanted.is_empty() {
        references.insert(16u8, def_reference.clone());
        if !models.is_empty() {
            references.insert(7u8, model_reference.clone());
        }
    }
    let mut wire = vec![0; 109];
    wire.extend_from_slice(&reply(255, 255, &version(&references)));
    wire.extend_from_slice(&reply(255, 5, &map_reference));
    for entry in &maps {
        wire.extend_from_slice(&reply(5, entry.id, &entry.container));
    }
    if !wanted.is_empty() {
        wire.extend_from_slice(&reply(255, 16, &def_reference));
        let archives: BTreeSet<u32> = wanted.iter().map(|id| (*id as u32) >> 8).collect();
        for (archive_id, container) in DEFINITIONS {
            if archives.contains(&archive_id) {
                wire.extend_from_slice(&reply(16, archive_id, &trimmed(container)));
            }
        }
        assert!(
            archives
                .iter()
                .all(|id| DEFINITIONS.iter().any(|d| d.0 == *id))
        );
        if !models.is_empty() {
            wire.extend_from_slice(&reply(255, 7, &model_reference));
            for (id, container) in &model_containers {
                wire.extend_from_slice(&reply(7, *id, container));
            }
        }
    }
    Server {
        wire,
        region_ids,
        models,
    }
}

type PlainRegions = (Vec<(u16, Vec<u8>)>, Vec<Vec<u8>>);

fn plain_regions(ids: &[u16]) -> PlainRegions {
    let mut regions = Vec::new();
    let mut payloads = Vec::new();
    for id in ids {
        let container = REGIONS.iter().find(|region| region.0 == *id).unwrap().1;
        regions.push((*id, trimmed(container)));
        payloads.push(payload(container));
    }
    (regions, payloads)
}

fn run(server: &Server, keys: Vec<[i32; 4]>, size: usize) -> Result<WorldLocProbe, String> {
    let mut probe = WorldLocProbe::new(b"synthetic", &server.region_ids, keys)?;
    assert!(!probe.take_outgoing().is_empty());
    for chunk in server.wire.chunks(size) {
        probe.feed(chunk)?;
    }
    Ok(probe)
}

#[test]
fn fixture_regions_fetch_decode_types_and_models_in_any_fragmentation() {
    let ids = [32906, 32907, 33162, 33163];
    let (mut regions, mut payloads) = plain_regions(&ids);
    let modeled = modeled_types(3);
    assert_eq!(modeled.len(), 3);
    let synthetic = synthetic_payload(&modeled);
    regions.push((40000, raw(&synthetic)));
    payloads.push(synthetic);
    let server = serve(&regions, &payloads);
    let wanted = expected_types(&payloads);
    let types = fixture_types(&wanted);
    assert!(modeled.iter().all(|id| types.contains_key(id)));
    assert_eq!(types.len(), 3);
    assert!(wanted.len() > types.len());
    assert!(server.models.len() >= 3);
    for size in [1, 7, 511, 512, 513, server.wire.len()] {
        let probe = run(&server, vec![[0; 4]; 5], size).unwrap();
        assert!(probe.complete());
        let report = probe.report().unwrap();
        assert_eq!(report.placements, 596);
        assert_eq!(report.type_ids, wanted.len());
        assert_eq!(report.types_found, types.len());
        assert_eq!(report.types_decoded, types.len());
        assert_eq!(report.types_undecodable, 0);
        assert_eq!(report.model_ids, server.models.len());
        assert_eq!(report.models_found, server.models.len());
        assert!(!report.authenticated);
        assert!(
            report
                .regions
                .iter()
                .all(|region| region.present && region.crc_verified && region.whirlpool_verified)
        );
        assert!(report.regions.iter().all(|region| !region.decrypted));
        for (id, container) in REGIONS {
            let region = probe.region(id).unwrap();
            assert_eq!(region.data, payload(container));
            let placed: BTreeSet<i32> = decode_region_locs(&region.data)
                .unwrap()
                .iter()
                .map(|placement| placement.id)
                .collect();
            assert_eq!(region.type_ids, placed.into_iter().collect::<Vec<_>>());
        }
        for (id, bytes) in &types {
            assert_eq!(probe.loc_type(*id).unwrap(), bytes.as_slice());
            for model in probe.model_ids(*id) {
                assert_eq!(probe.model(model).unwrap(), model.to_be_bytes());
            }
        }
        assert!(probe.region(1).is_none());
        assert!(probe.loc_type(-1).is_none());
        assert!(probe.model(-1).is_none());
        assert!(probe.model_ids(-1).is_empty());
    }
}

#[test]
fn encrypted_regions_are_decrypted_with_their_world_key_and_raw_regions_still_load() {
    let keys = keys();
    let container = REGIONS[0].1;
    let expected = payload(container);
    let regions = vec![
        (32906u16, ENCRYPTED_32906[0].to_vec()),
        (32907, trimmed(REGIONS[1].1)),
    ];
    let payloads = vec![payload(REGIONS[0].1), payload(REGIONS[1].1)];
    let server = serve(&regions, &payloads);
    let probe = run(&server, vec![keys[0], [0; 4]], 512).unwrap();
    assert_eq!(probe.region(32906).unwrap().data, expected);
    assert_eq!(probe.region(32907).unwrap().data, payload(REGIONS[1].1));
    let report = probe.report().unwrap();
    assert!(report.regions[0].keyed && report.regions[0].decrypted);
    assert!(report.regions[0].crc_verified && !report.regions[0].whirlpool_verified);
    assert!(!report.regions[1].keyed && !report.regions[1].decrypted);
    assert!(report.regions[1].whirlpool_verified);
    assert_eq!(report.types_found, 0);
    let json = serde_json::to_string(report).unwrap();
    assert!(!json.contains("19088743"));
}

#[test]
fn a_wrong_key_fails_and_clears_everything() {
    let keys = keys();
    let regions = vec![(32906u16, ENCRYPTED_32906[0].to_vec())];
    let server = serve(&regions, &[payload(REGIONS[0].1)]);
    let error = run(&server, vec![keys[1]], 4096).err().unwrap();
    assert!(error.contains("loc"));
    let mut probe = WorldLocProbe::new(b"synthetic", &[32906], vec![keys[1]]).unwrap();
    let mut failed = false;
    for chunk in server.wire.chunks(1024) {
        if probe.feed(chunk).is_err() {
            failed = true;
            break;
        }
    }
    assert!(failed);
    assert!(!probe.complete());
    assert!(probe.report().is_none());
    assert!(probe.region(32906).is_none());
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.keys.is_empty());
    assert!(probe.feed(&[]).is_err());
    assert!(probe.finish().is_err());
}

#[test]
fn encrypted_containers_need_the_stored_crc() {
    let keys = keys();
    let mut tampered = ENCRYPTED_32906[0].to_vec();
    let last = tampered.len() - 1;
    tampered[last] ^= 1;
    let maps = vec![Entry {
        id: 9,
        hash: loc_name_hash(32906),
        container: ENCRYPTED_32906[0].to_vec(),
    }];
    let reference = named_reference(&maps);
    let mut references = BTreeMap::new();
    references.insert(5u8, reference.clone());
    let mut wire = vec![0; 109];
    wire.extend_from_slice(&reply(255, 255, &version(&references)));
    wire.extend_from_slice(&reply(255, 5, &reference));
    wire.extend_from_slice(&reply(5, 9, &tampered));
    let mut probe = WorldLocProbe::new(b"synthetic", &[32906], vec![keys[0]]).unwrap();
    assert!(probe.feed(&wire).is_err());
    assert!(!probe.complete());
}

#[test]
fn absent_regions_complete_after_the_map_index_without_definition_requests() {
    let server = serve(&[], &[]);
    let mut probe = WorldLocProbe::new(b"synthetic", &[1, 2], vec![[0; 4]; 2]).unwrap();
    probe.take_outgoing();
    probe.feed(&server.wire).unwrap();
    assert!(probe.complete());
    probe.take_outgoing();
    assert!(probe.take_outgoing().is_empty());
    let report = probe.report().unwrap();
    assert_eq!(report.placements, 0);
    assert!(report.regions.iter().all(|region| !region.present));
    assert!(probe.region(1).is_none());
    assert!(probe.feed(&[0]).is_err());
}

#[test]
fn stub_regions_are_present_but_place_nothing() {
    let stub = trimmed(include_bytes!(
        "../../../scene/tests/fixtures/loc-l32650.container"
    ));
    let regions = [(32650u16, stub)];
    let maps = vec![Entry {
        id: 3,
        hash: loc_name_hash(32650),
        container: regions[0].1.clone(),
    }];
    let reference = named_reference(&maps);
    let mut references = BTreeMap::new();
    references.insert(5u8, reference.clone());
    let mut wire = vec![0; 109];
    wire.extend_from_slice(&reply(255, 255, &version(&references)));
    wire.extend_from_slice(&reply(255, 5, &reference));
    wire.extend_from_slice(&reply(5, 3, &regions[0].1));
    let mut probe = WorldLocProbe::new(b"synthetic", &[32650], vec![[0; 4]]).unwrap();
    probe.feed(&wire).unwrap();
    assert!(probe.complete());
    assert_eq!(probe.region(32650).unwrap().data, vec![0]);
    assert!(probe.region(32650).unwrap().type_ids.is_empty());
    assert_eq!(probe.report().unwrap().types_found, 0);
}

#[test]
fn constructor_rejects_bad_regions_and_keys() {
    assert!(WorldLocProbe::new(b"t", &[], vec![]).is_err());
    assert!(WorldLocProbe::new(b"t", &[1, 1], vec![[0; 4]; 2]).is_err());
    assert!(WorldLocProbe::new(b"t", &[1, 2], vec![[0; 4]]).is_err());
    let many: Vec<u16> = (0..65).collect();
    assert!(WorldLocProbe::new(b"t", &many, vec![[0; 4]; 65]).is_err());
}

#[test]
fn truncated_or_unrequested_transfers_fail() {
    let (regions, payloads) = plain_regions(&[32906]);
    let server = serve(&regions, &payloads);
    let mut probe = WorldLocProbe::new(b"synthetic", &server.region_ids, vec![[0; 4]]).unwrap();
    probe.feed(&server.wire[..server.wire.len() / 2]).unwrap();
    assert!(!probe.complete());
    assert!(probe.finish().is_err());
    assert!(probe.region(32906).is_none());
    let mut extra = server.wire.clone();
    extra.push(1);
    let mut probe = WorldLocProbe::new(b"synthetic", &server.region_ids, vec![[0; 4]]).unwrap();
    assert!(probe.feed(&extra).is_err());
}

#[test]
fn name_hash_matches_the_client_archive_name() {
    let expected = "l128_138".bytes().fold(0i32, |hash, byte| {
        hash.wrapping_mul(31).wrapping_add(i32::from(byte))
    });
    assert_eq!(loc_name_hash(32906), expected);
}

#[test]
fn model_ids_of_unknown_or_broken_types_are_empty() {
    assert!(type_model_ids(1, &[14]).is_empty());
    assert!(type_model_ids(1, &[0]).is_empty());
}
