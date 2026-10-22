use openrs_client_runtime::materials::{
    MATERIAL_ARCHIVE, MATERIAL_FILE, MATERIAL_INDEX, WorldMaterialProbe,
};
use openrs_protocol::js5;
use openrs_scene::material::{MAX_DEFINITION_BYTES, Material, MaterialTable};
use serde_json::Value;
use std::str::from_utf8;
use whirlpool::{Digest, Whirlpool};

const NATIVE: &str = include_str!("../../scene/tests/fixtures/material.tsv");
const WIRE: &[u8] = include_bytes!("fixtures/material-native.wire");

fn hex(payload: &str) -> Vec<u8> {
    assert_eq!(payload.len() % 2, 0);
    payload
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

fn fields(value: &Material) -> [i32; 19] {
    [
        i32::from(value.details_only),
        i32::from(value.is_small),
        i32::from(value.skip_triangles),
        i32::from(value.brightness),
        i32::from(value.alpha),
        i32::from(value.effect_id),
        i32::from(value.effect_param1),
        i32::from(value.color_hsl),
        i32::from(value.texture_speed_u),
        i32::from(value.texture_speed_v),
        i32::from(value.a_bool2087),
        i32::from(value.is_brick_tile),
        i32::from(value.mipmapping),
        i32::from(value.repeat_s),
        i32::from(value.repeat_t),
        i32::from(value.hdr),
        i32::from(value.combine_mode),
        value.effect_param2,
        i32::from(value.effect_combiner),
    ]
}

fn raw(data: &[u8]) -> Vec<u8> {
    let mut result = vec![0];
    result.extend_from_slice(&u32::try_from(data.len()).unwrap().to_be_bytes());
    result.extend_from_slice(data);
    result
}

fn reply(index: u8, archive: u32, container: &[u8]) -> Vec<u8> {
    let mut wire = vec![index];
    wire.extend_from_slice(&archive.to_be_bytes());
    wire.extend_from_slice(&container[..5]);
    let mut position = 10;
    for &byte in &container[5..] {
        if position == 512 {
            wire.push(255);
            position = 1;
        }
        wire.push(byte);
        position += 1;
    }
    wire
}

struct Chain {
    version: Vec<u8>,
    reference: Vec<u8>,
    archive: Vec<u8>,
}

impl Chain {
    fn new(files: &[(u16, &[u8])], digest: bool) -> Self {
        let mut data = Vec::new();
        for (_, bytes) in files {
            data.extend_from_slice(bytes);
        }
        if files.len() > 1 {
            let mut previous = 0;
            for (_, bytes) in files {
                let size = i32::try_from(bytes.len()).unwrap();
                data.extend_from_slice(&(size - previous).to_be_bytes());
                previous = size;
            }
            data.push(1);
        }
        let archive = raw(&data);
        let mut reference = vec![6];
        reference.extend_from_slice(&727_i32.to_be_bytes());
        reference.push(if digest { 2 } else { 0 });
        reference.extend_from_slice(&1_u16.to_be_bytes());
        reference.extend_from_slice(&0_u16.to_be_bytes());
        reference.extend_from_slice(&crc32fast::hash(&archive).to_be_bytes());
        if digest {
            reference.extend_from_slice(&Whirlpool::digest(&archive));
        }
        reference.extend_from_slice(&44_i32.to_be_bytes());
        reference.extend_from_slice(&u16::try_from(files.len()).unwrap().to_be_bytes());
        let mut previous = 0;
        for &(id, _) in files {
            reference.extend_from_slice(&(id - previous).to_be_bytes());
            previous = id;
        }
        let reference = raw(&reference);
        let version = Self::version(&reference);
        Self {
            version,
            reference,
            archive,
        }
    }

    fn version(reference: &[u8]) -> Vec<u8> {
        let mut version = vec![27];
        version.extend_from_slice(&[0; 26 * 72]);
        version.extend_from_slice(&crc32fast::hash(reference).to_be_bytes());
        version.extend_from_slice(&9001_i32.to_be_bytes());
        version.extend_from_slice(&Whirlpool::digest(reference));
        version.extend_from_slice(&[0x5a; 128]);
        raw(&version)
    }

    fn before_reference(&self) -> WorldMaterialProbe {
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        assert!(!probe.take_outgoing().is_empty());
        assert_unpublished(&probe);
        probe.feed(&[0; 109]).unwrap();
        assert_eq!(probe.take_outgoing().len(), 18);
        probe.feed(&reply(255, 255, &self.version)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(255, 26, js5::Priority::High)
        );
        assert_unpublished(&probe);
        probe
    }

    fn before_archive(&self) -> WorldMaterialProbe {
        let mut probe = self.before_reference();
        probe.feed(&reply(255, 26, &self.reference)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(26, 0, js5::Priority::High)
        );
        assert_unpublished(&probe);
        probe
    }

    fn wire(&self) -> Vec<u8> {
        [
            vec![0; 109],
            reply(255, 255, &self.version),
            reply(255, 26, &self.reference),
            reply(26, 0, &self.archive),
        ]
        .concat()
    }
}

fn assert_unpublished(probe: &WorldMaterialProbe) {
    assert!(!probe.complete());
    assert!(probe.report().is_none());
    assert!(probe.table().is_err());
    assert!(probe.material(0).is_err());
    assert!(probe.material(128).is_err());
    assert!(probe.material(154).is_err());
}

fn assert_terminal(probe: &mut WorldMaterialProbe) {
    assert_unpublished(probe);
    assert!(probe.take_outgoing().is_empty());
    assert_eq!(probe.feed(&[]).unwrap_err(), "material probe has failed");
    assert_eq!(probe.feed(WIRE).unwrap_err(), "material probe has failed");
    assert_eq!(probe.finish().unwrap_err(), "material probe has failed");
    assert_unpublished(probe);
    assert!(probe.take_outgoing().is_empty());
}

fn native_payload(name: &str) -> Vec<u8> {
    let row: Vec<_> = NATIVE
        .lines()
        .find(|line| line.starts_with(&format!("table\t{name}\t")))
        .unwrap()
        .split('\t')
        .collect();
    hex(row[2])
}

#[test]
fn verified_native_payload_has_exact_chain_reports_and_only_final_publication() {
    assert_eq!(MATERIAL_INDEX, 26);
    assert_eq!(MATERIAL_ARCHIVE, 0);
    assert_eq!(MATERIAL_FILE, 0);
    let metadata: Value =
        serde_json::from_str(include_str!("fixtures/material-native.json")).unwrap();
    let payload = native_payload("bytes-128");
    let chain = Chain::new(&[(0, &payload)], true);
    assert_eq!(chain.wire(), WIRE);
    assert_eq!(metadata["materialPayloadHex"], hex_string(&payload));
    for chunk_size in [1, 2, 7, 109, 511, 512, 513, WIRE.len()] {
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        for (index, chunk) in WIRE.chunks(chunk_size).enumerate() {
            probe.feed(chunk).unwrap();
            if (index + 1) * chunk_size < WIRE.len() {
                assert_unpublished(&probe);
            }
        }
        assert!(probe.complete());
        let report = probe.report().unwrap();
        assert_eq!(report.index, 26);
        assert_eq!(report.archive, 0);
        assert_eq!(report.reference_revision, 727);
        assert_eq!(report.archive_revision, 44);
        assert_eq!(report.prefetch_count, 27);
        assert_eq!(report.container_bytes, payload.len() + 5);
        assert_eq!(report.expanded_bytes, payload.len());
        assert_eq!(report.file_slots, 1);
        assert_eq!(report.files.len(), 1);
        assert_eq!(report.files[0].id, 0);
        assert_eq!(report.files[0].bytes, payload.len());
        assert!(report.files[0].script.is_none());
        assert!(report.reference_crc_verified);
        assert!(report.reference_whirlpool_verified);
        assert!(report.archive_crc_verified);
        assert!(report.archive_whirlpool_verified);
        assert!(!report.authenticated);
        let json = serde_json::to_value(report).unwrap();
        assert_eq!(json["authenticated"], false);
        assert_eq!(json["archiveWhirlpoolVerified"], true);
        for expected in metadata["materialFields"].as_array().unwrap() {
            let id = i32::try_from(expected["id"].as_i64().unwrap()).unwrap();
            let expected: Vec<i32> = expected["values"]
                .as_array()
                .unwrap()
                .iter()
                .map(|value| i32::try_from(value.as_i64().unwrap()).unwrap())
                .collect();
            assert_eq!(fields(probe.material(id).unwrap()).as_slice(), expected);
        }
        let table = probe.table().unwrap() as *const MaterialTable;
        probe.feed(&[]).unwrap();
        probe.finish().unwrap();
        probe.finish().unwrap();
        assert_eq!(probe.table().unwrap() as *const MaterialTable, table);
    }
}

fn hex_string(input: &[u8]) -> String {
    input.iter().map(|byte| format!("{byte:02x}")).collect()
}

#[test]
fn all_native_oracle_tables_survive_verified_transport_without_field_changes() {
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("table\t")) {
        let row: Vec<_> = line.split('\t').collect();
        let payload = hex(row[2]);
        let chain = Chain::new(&[(0, &payload)], count % 2 == 0);
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        for chunk in chain.wire().chunks(511) {
            probe.feed(chunk).unwrap();
        }
        assert_eq!(
            probe.table().unwrap(),
            &MaterialTable::decode(&payload).unwrap(),
            "{}",
            row[1]
        );
        assert_eq!(
            probe.report().unwrap().archive_whirlpool_verified,
            count % 2 == 0
        );
        assert!(!probe.report().unwrap().authenticated);
        count += 1;
    }
    assert_eq!(count, 280);
}

#[test]
fn lookups_are_signed_bounded_and_missing_slots_never_become_defaults() {
    let chain = Chain::new(&[(0, &native_payload("id-addressing"))], true);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    probe.feed(&chain.wire()).unwrap();
    assert_eq!(probe.table().unwrap().len(), 155);
    assert!(probe.material(128).is_ok());
    assert!(probe.material(154).is_ok());
    for id in [0, 127, 129, 153] {
        assert_eq!(
            probe.material(id).unwrap_err(),
            format!("material not found: {id}")
        );
    }
    for id in [i32::MIN, -1, 155, 65_535, i32::MAX] {
        assert_eq!(
            probe.material(id).unwrap_err(),
            format!("material id {id} outside table of size 155")
        );
    }
    assert!(probe.complete());
    probe.finish().unwrap();
}

#[test]
fn valid_empty_table_is_distinct_from_missing_or_malformed_file() {
    let chain = Chain::new(&[(0, &[0, 0])], false);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    probe.feed(&chain.wire()).unwrap();
    assert!(probe.complete());
    assert!(probe.table().unwrap().is_empty());
    assert!(probe.material(0).is_err());
    assert!(!probe.report().unwrap().archive_whirlpool_verified);
    assert!(probe.report().unwrap().reference_whirlpool_verified);
    assert!(!probe.report().unwrap().authenticated);
    let chain = Chain::new(&[(0, &[])], false);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    assert!(
        probe
            .feed(&chain.wire())
            .unwrap_err()
            .contains("material byte 0")
    );
    assert_terminal(&mut probe);
}

#[test]
fn checked_synthetic_failure_fixtures_are_terminal_and_never_publish_partial_tables() {
    let fixtures: [(&[u8], &str); 6] = [
        (include_bytes!("fixtures/material-truncated.wire"), "finish"),
        (include_bytes!("fixtures/material-corrupt.wire"), "CRC"),
        (
            include_bytes!("fixtures/material-missing0.wire"),
            "only file 0",
        ),
        (
            include_bytes!("fixtures/material-extra-files.wire"),
            "only file 0",
        ),
        (
            include_bytes!("fixtures/material-metadata-truncated.wire"),
            "expected",
        ),
        (
            include_bytes!("fixtures/material-trailing.wire"),
            "trailing",
        ),
    ];
    for (wire, expected) in fixtures {
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        if expected == "finish" {
            probe.feed(wire).unwrap();
            assert_unpublished(&probe);
            assert!(probe.finish().is_err());
        } else {
            assert!(probe.feed(wire).unwrap_err().contains(expected));
        }
        assert_terminal(&mut probe);
    }
}

#[test]
fn incomplete_close_at_each_transport_stage_clears_requests_and_is_sticky() {
    let chain = Chain::new(&[(0, &[0, 0])], true);
    let root_end = 109 + reply(255, 255, &chain.version).len();
    let reference_end = root_end + reply(255, 26, &chain.reference).len();
    let wire = chain.wire();
    for cut in [
        0,
        1,
        108,
        109,
        110,
        root_end - 1,
        root_end,
        reference_end,
        wire.len() - 1,
    ] {
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        probe.feed(&wire[..cut]).unwrap();
        assert_unpublished(&probe);
        assert!(probe.finish().is_err());
        assert_terminal(&mut probe);
    }
}

#[test]
fn late_bytes_clear_previously_valid_table_and_same_chunk_success_is_transactional() {
    for combined in [false, true] {
        let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
        if combined {
            let mut wire = WIRE.to_vec();
            wire.push(0);
            assert!(probe.feed(&wire).unwrap_err().contains("unexpected bytes"));
        } else {
            probe.feed(WIRE).unwrap();
            assert!(probe.complete());
            assert!(probe.material(0).is_ok());
            assert!(probe.feed(&[0]).unwrap_err().contains("unexpected bytes"));
        }
        assert_terminal(&mut probe);
    }
}

#[test]
fn corrupt_reference_and_archive_crc_fail_before_material_publication() {
    let chain = Chain::new(&[(0, &[0, 0])], true);
    let mut probe = chain.before_reference();
    let mut corrupt = chain.reference.clone();
    *corrupt.last_mut().unwrap() ^= 1;
    assert!(
        probe
            .feed(&reply(255, 26, &corrupt))
            .unwrap_err()
            .contains("CRC")
    );
    assert_terminal(&mut probe);
    let mut probe = chain.before_archive();
    let mut corrupt = chain.archive.clone();
    *corrupt.last_mut().unwrap() ^= 1;
    assert!(
        probe
            .feed(&reply(26, 0, &corrupt))
            .unwrap_err()
            .contains("CRC")
    );
    assert_terminal(&mut probe);
}

#[test]
fn root_and_archive_whirlpool_mismatch_cannot_be_hidden_by_valid_crc() {
    let mut chain = Chain::new(&[(0, &[0, 0])], true);
    chain.version[5 + 1 + 26 * 72 + 8] ^= 1;
    let mut probe = chain.before_reference();
    assert!(
        probe
            .feed(&reply(255, 26, &chain.reference))
            .unwrap_err()
            .contains("Whirlpool")
    );
    assert_terminal(&mut probe);
    let mut chain = Chain::new(&[(0, &[0, 0])], true);
    chain.reference[5 + 14] ^= 1;
    chain.version = Chain::version(&chain.reference);
    let mut probe = chain.before_archive();
    assert!(
        probe
            .feed(&reply(26, 0, &chain.archive))
            .unwrap_err()
            .contains("Whirlpool")
    );
    assert_terminal(&mut probe);
}

#[test]
fn absent_root_index_absent_archive_and_unrequested_identity_fail_closed() {
    let mut chain = Chain::new(&[(0, &[0, 0])], true);
    let mut version = vec![26];
    version.extend_from_slice(&[0; 26 * 72 + 128]);
    chain.version = raw(&version);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    probe.feed(&[0; 109]).unwrap();
    assert!(
        probe
            .feed(&reply(255, 255, &chain.version))
            .unwrap_err()
            .contains("index absent")
    );
    assert_terminal(&mut probe);
    let mut chain = Chain::new(&[(0, &[0, 0])], true);
    chain.reference[5 + 9] = 1;
    chain.version = Chain::version(&chain.reference);
    let mut probe = chain.before_reference();
    assert!(
        probe
            .feed(&reply(255, 26, &chain.reference))
            .unwrap_err()
            .contains("archive absent")
    );
    assert_terminal(&mut probe);
    let chain = Chain::new(&[(0, &[0, 0])], true);
    let mut probe = chain.before_reference();
    assert!(
        probe
            .feed(&reply(255, 25, &chain.reference))
            .unwrap_err()
            .contains("identity")
    );
    assert_terminal(&mut probe);
    let mut probe = chain.before_archive();
    assert!(
        probe
            .feed(&reply(26, 1, &chain.archive))
            .unwrap_err()
            .contains("identity")
    );
    assert_terminal(&mut probe);
}

#[test]
fn material_cap_and_generic_transport_caps_are_both_enforced() {
    let payload = vec![0; MAX_DEFINITION_BYTES + 1];
    let chain = Chain::new(&[(0, &payload)], false);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    assert!(
        probe
            .feed(&chain.wire())
            .unwrap_err()
            .contains("exceeds byte limit 1572842")
    );
    assert_terminal(&mut probe);
    let chain = Chain::new(&[(0, &[0, 0])], false);
    let mut probe = chain.before_archive();
    let mut header = vec![26, 0, 0, 0, 0, 0];
    header.extend_from_slice(&(4 * 1024 * 1024_u32).to_be_bytes());
    assert!(probe.feed(&header).is_err());
    assert_terminal(&mut probe);
    let mut probe = chain.before_archive();
    let container = [
        vec![2],
        0_u32.to_be_bytes().to_vec(),
        (8 * 1024 * 1024_u32 + 1).to_be_bytes().to_vec(),
    ]
    .concat();
    assert!(
        probe
            .feed(&reply(26, 0, &container))
            .unwrap_err()
            .contains("expanded byte limit")
    );
    assert_terminal(&mut probe);
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    assert!(
        probe
            .feed(&vec![0; 12 * 1024 * 1024 + 1])
            .unwrap_err()
            .contains("byte limit")
    );
    assert_terminal(&mut probe);
}

#[test]
fn fixture_provenance_explicitly_distinguishes_native_values_from_synthetic_wire() {
    let metadata: Value =
        serde_json::from_str(include_str!("fixtures/material-native.json")).unwrap();
    assert_eq!(
        metadata["sourceJarSha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    assert_eq!(
        metadata["sourceFixtureSha256"],
        "ddad50ed7170177b46d74c123babc33bb205ed7c876c8a22c2daee2860e99a36"
    );
    assert!(
        metadata["origin"]
            .as_str()
            .unwrap()
            .contains("Synthetic JS5")
    );
    assert!(
        metadata["origin"]
            .as_str()
            .unwrap()
            .contains("not captured live cache")
    );
    assert!(
        metadata["extraFilesPolicy"]
            .as_str()
            .unwrap()
            .contains("stricter than native")
    );
    assert_eq!(metadata["authenticated"], false);
    assert_eq!(metadata["fixtures"].as_object().unwrap().len(), 11);
    assert_eq!(
        metadata["fixtures"]["material-native.wire"]["bytes"],
        WIRE.len()
    );
}
