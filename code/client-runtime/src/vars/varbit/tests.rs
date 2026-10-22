use super::*;
use crate::vars::{VariablePacket, WorldVarHost, WorldVariables};
use openrs_assets::{Archive, FileMetadata};
use openrs_cs2::{Arguments, Opcode, Script, VarHost, Vm, VmErrorKind};
use serde_json::Value;
use std::collections::BTreeMap;
use whirlpool::{Digest, Whirlpool};

fn fixture() -> Value {
    serde_json::from_str(include_str!("../../../tests/fixtures/varbits-native.json")).unwrap()
}

fn hex(value: &str) -> Vec<u8> {
    assert_eq!(value.len() % 2, 0);
    value
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(std::str::from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

fn native_version(fixture: &Value) -> VersionEntry {
    VersionEntry {
        crc: fixture["version"]["crc"].as_i64().unwrap() as i32,
        revision: fixture["version"]["revision"].as_i64().unwrap() as i32,
        whirlpool: hex(fixture["version"]["whirlpoolHex"].as_str().unwrap())
            .try_into()
            .unwrap(),
    }
}

fn native_repository(load_archives: bool) -> VarbitRepository {
    let fixture = fixture();
    let mut repository = VarbitRepository::new();
    repository
        .load_reference(
            &hex(fixture["referenceHex"].as_str().unwrap()),
            &native_version(&fixture),
        )
        .unwrap();
    if load_archives {
        for archive in fixture["archives"].as_array().unwrap() {
            repository
                .load_archive(
                    archive["id"].as_u64().unwrap() as u32,
                    &hex(archive["containerHex"].as_str().unwrap()),
                )
                .unwrap();
        }
    }
    repository
}

fn native_definition(value: &Value) -> Varbit {
    Varbit {
        varp: value["varp"].as_u64().unwrap() as u16,
        start: value["start"].as_u64().unwrap() as u8,
        end: value["end"].as_u64().unwrap() as u8,
    }
}

fn container(bytes: &[u8]) -> Vec<u8> {
    let mut out = vec![0];
    out.extend_from_slice(&(bytes.len() as i32).to_be_bytes());
    out.extend_from_slice(bytes);
    out
}

fn version(container: &[u8]) -> VersionEntry {
    VersionEntry {
        crc: crc32fast::hash(container) as i32,
        revision: 17,
        whirlpool: Whirlpool::digest(container).into(),
    }
}

fn archive(id: u32, file_ids: &[u32], container: &[u8], hashed: bool) -> Archive {
    Archive {
        id,
        crc32: crc32fast::hash(container),
        revision: 20,
        name_hash: None,
        whirlpool: hashed.then(|| Whirlpool::digest(container).into()),
        files: file_ids
            .iter()
            .map(|id| FileMetadata {
                id: *id,
                name_hash: None,
            })
            .collect(),
    }
}

fn reference(archives: &[Archive], hashed: bool) -> Vec<u8> {
    let mut out = vec![6];
    out.extend_from_slice(&17i32.to_be_bytes());
    out.push(if hashed { 2 } else { 0 });
    out.extend_from_slice(&(archives.len() as u16).to_be_bytes());
    let mut previous = 0;
    for archive in archives {
        out.extend_from_slice(&((archive.id - previous) as u16).to_be_bytes());
        previous = archive.id;
    }
    for archive in archives {
        out.extend_from_slice(&archive.crc32.to_be_bytes());
    }
    if hashed {
        for archive in archives {
            out.extend_from_slice(archive.whirlpool.as_ref().unwrap());
        }
    }
    for archive in archives {
        out.extend_from_slice(&archive.revision.to_be_bytes());
    }
    for archive in archives {
        out.extend_from_slice(&(archive.files.len() as u16).to_be_bytes());
    }
    for archive in archives {
        let mut previous = 0;
        for file in &archive.files {
            out.extend_from_slice(&((file.id - previous) as u16).to_be_bytes());
            previous = file.id;
        }
    }
    container(&out)
}

fn repository(archives: &[Archive], hashed: bool) -> VarbitRepository {
    let reference = reference(archives, hashed);
    let mut repository = VarbitRepository::new();
    repository
        .load_reference(&reference, &version(&reference))
        .unwrap();
    repository
}

fn packed(definitions: &[Vec<u8>]) -> Vec<u8> {
    if definitions.len() == 1 {
        return definitions[0].clone();
    }
    let mut out = Vec::new();
    for definition in definitions {
        out.extend_from_slice(definition);
    }
    let mut previous = 0;
    for definition in definitions {
        out.extend_from_slice(&(definition.len() as i32 - previous).to_be_bytes());
        previous = definition.len() as i32;
    }
    out.push(1);
    out
}

fn host_script(code: &[(Opcode, i32)]) -> Script {
    let mut bytes = vec![0];
    for (opcode, operand) in code {
        bytes.extend_from_slice(&opcode.wire().to_be_bytes());
        if opcode.metadata().has_int_constant {
            bytes.extend_from_slice(&operand.to_be_bytes());
        } else {
            bytes.push(*operand as u8);
        }
    }
    bytes.extend_from_slice(&(code.len() as i32).to_be_bytes());
    bytes.extend_from_slice(&[0; 12]);
    bytes.extend_from_slice(&[0, 0, 1]);
    Script::decode(&bytes).unwrap()
}

#[test]
fn copied_native_oracle_proves_index22_ten_bit_mapping_and_decoder_fields() {
    let fixture = fixture();
    assert_eq!(
        fixture["provenance"]["jarSha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    assert_eq!(fixture["index"], VARBIT_INDEX);
    assert_eq!(fixture["configTypeId"], 14);
    assert_eq!(fixture["archiveBits"], VARBIT_ARCHIVE_BITS);
    for sample in fixture["mapping"].as_array().unwrap() {
        assert_eq!(
            definition_location(sample["id"].as_i64().unwrap() as i32),
            Ok((
                sample["archive"].as_u64().unwrap() as u32,
                sample["file"].as_u64().unwrap() as u32
            ))
        );
    }
    let definitions = fixture["definitions"].as_array().unwrap();
    assert_eq!(definitions.len(), 293);
    for sample in definitions {
        let bytes = hex(sample["hex"].as_str().unwrap());
        assert_eq!(
            decode_definition(&bytes),
            Ok(native_definition(&sample["definition"]))
        );
        assert_eq!(sample["consumed"].as_u64().unwrap() as usize, bytes.len());
    }
}

#[test]
fn malformed_definitions_fail_closed_instead_of_native_permissive_fallbacks() {
    for sample in fixture()["nonCanonical"].as_array().unwrap() {
        let bytes = hex(sample["hex"].as_str().unwrap());
        assert!(decode_definition(&bytes).is_err(), "{sample}");
    }
    for bytes in [
        vec![],
        vec![1],
        vec![1, 0],
        vec![1, 0, 0],
        vec![1, 0, 0, 0],
        vec![1, 0, 0, 0, 0],
    ] {
        assert_eq!(decode_definition(&bytes), Err(DefinitionError::Truncated));
    }
    for (start, end) in [(1, 0), (0, 32), (0, 255), (222, 254), (255, 0)] {
        assert_eq!(
            decode_definition(&[1, 0, 2, start, end, 0]),
            Err(DefinitionError::InvalidSpan { start, end })
        );
    }
    for opcode in 2..=255 {
        assert_eq!(
            decode_definition(&[opcode, 0]),
            Err(DefinitionError::UnknownOpcode(opcode))
        );
    }
    assert_eq!(
        decode_definition(&[0, 0]),
        Err(DefinitionError::TrailingData)
    );
    let mut repeated = [1, 0, 2, 34, 41].repeat(64);
    repeated.push(0);
    assert_eq!(repeated.len(), MAX_DEFINITION_BYTES);
    assert_eq!(
        decode_definition(&repeated),
        Ok(Varbit {
            varp: 2,
            start: 34,
            end: 41
        })
    );
    repeated.push(0);
    assert_eq!(
        decode_definition(&repeated),
        Err(DefinitionError::ByteLimit)
    );
}

#[test]
fn sparse_reference_absence_never_conflates_declared_but_unloaded_definitions() {
    let fixture = fixture();
    let unknown = VarbitRepository::new();
    assert!(!unknown.reference_loaded());
    for id in [0, 1, 1023, 1031, MAX_VARBIT_ID] {
        assert_eq!(unknown.lookup(id), Err(HostError::Unavailable));
    }
    for id in [-1, MAX_VARBIT_ID + 1, i32::MAX] {
        assert_eq!(unknown.lookup(id), Err(HostError::InvalidVarbit(id)));
        assert_eq!(definition_location(id), Err(HostError::InvalidVarbit(id)));
    }
    let mut repository = native_repository(false);
    assert!(repository.reference_loaded());
    assert_eq!(repository.definition_count(), 0);
    assert_eq!(repository.loaded_archive_count(), 0);
    for id in [1, 1025, 2048, MAX_VARBIT_ID] {
        assert_eq!(repository.lookup(id), Ok(VarbitLookup::Absent));
    }
    for archive in fixture["archives"].as_array().unwrap() {
        let archive_id = archive["id"].as_u64().unwrap() as u32;
        for file in archive["files"].as_array().unwrap() {
            assert_eq!(
                repository
                    .lookup(((archive_id << 10) | file["id"].as_u64().unwrap() as u32) as i32),
                Err(HostError::Unavailable)
            );
        }
        repository
            .load_archive(archive_id, &hex(archive["containerHex"].as_str().unwrap()))
            .unwrap();
        for file in archive["files"].as_array().unwrap() {
            let id = ((archive_id << 10) | file["id"].as_u64().unwrap() as u32) as i32;
            assert_eq!(
                repository.lookup(id),
                Ok(VarbitLookup::Defined(native_definition(
                    &file["definition"]
                )))
            );
        }
    }
    assert_eq!(repository.loaded_archive_count(), 3);
    assert_eq!(repository.definition_count(), 6);
    assert_eq!(repository.lookup(1), Ok(VarbitLookup::Absent));
    assert_eq!(
        repository.lookup(1024),
        Ok(VarbitLookup::Defined(Varbit {
            varp: 0,
            start: 0,
            end: 0
        }))
    );
    assert!(repository.expanded_bytes() < 1024);
    for sample in fixture["lookups"].as_array().unwrap() {
        let id = sample["id"].as_i64().unwrap() as i32;
        if sample["available"] == true {
            match repository.lookup(id).unwrap() {
                VarbitLookup::Defined(definition) => {
                    assert_eq!(sample["fileReturned"], true);
                    assert_eq!(definition, native_definition(&sample["definition"]));
                }
                VarbitLookup::Absent => {
                    assert_eq!(sample["fileReturned"], false);
                    assert_eq!(
                        native_definition(&sample["definition"]),
                        Varbit {
                            varp: 0,
                            start: 0,
                            end: 0
                        }
                    );
                }
            }
        } else {
            assert_eq!(sample["fileReturned"], false);
        }
    }
}

#[test]
fn integrity_revision_and_reference_parse_failures_cannot_invent_absence() {
    let fixture = fixture();
    let original = hex(fixture["referenceHex"].as_str().unwrap());
    let expected = native_version(&fixture);
    let mut repository = VarbitRepository::new();
    let mut damaged = original.clone();
    *damaged.last_mut().unwrap() ^= 1;
    assert!(matches!(
        repository.load_reference(&damaged, &expected),
        Err(RepositoryError::Asset(
            openrs_assets::Error::CrcMismatch { .. }
        ))
    ));
    let mut bad_digest = expected.clone();
    bad_digest.whirlpool[0] ^= 1;
    assert_eq!(
        repository.load_reference(&original, &bad_digest),
        Err(RepositoryError::Asset(
            openrs_assets::Error::WhirlpoolMismatch
        ))
    );
    let mut bad_revision = expected.clone();
    bad_revision.revision += 1;
    assert_eq!(
        repository.load_reference(&original, &bad_revision),
        Err(RepositoryError::RevisionMismatch {
            expected: 18,
            actual: 17
        })
    );
    for malformed in [vec![8, 0], original[5..original.len() - 1].to_vec()] {
        let malformed = container(&malformed);
        assert!(
            repository
                .load_reference(&malformed, &version(&malformed))
                .is_err()
        );
        assert_eq!(repository.lookup(1), Err(HostError::Unavailable));
        assert_eq!(repository.expanded_bytes(), 0);
        assert!(!repository.reference_loaded());
    }
    repository.load_reference(&original, &expected).unwrap();
    assert_eq!(
        repository.load_reference(&original, &expected),
        Err(RepositoryError::ReferenceAlreadyLoaded)
    );
    assert_eq!(repository.lookup(0), Err(HostError::Unavailable));
    assert_eq!(repository.lookup(1), Ok(VarbitLookup::Absent));
}

#[test]
fn archive_decode_is_atomic_and_corruption_never_creates_a_default_definition() {
    let valid = vec![1, 0, 2, 4, 7, 0];
    for invalid in [
        vec![],
        vec![1],
        vec![2, 0],
        vec![0, 0],
        vec![1, 0, 2, 0, 32, 0],
        vec![0; MAX_DEFINITION_BYTES + 1],
    ] {
        let bytes = container(&packed(&[valid.clone(), invalid]));
        let mut repository = repository(&[archive(1, &[0, 7], &bytes, true)], true);
        let expanded = repository.expanded_bytes();
        assert!(matches!(
            repository.load_archive(1, &bytes),
            Err(RepositoryError::Definition { id: 1031, .. })
        ));
        assert_eq!(repository.lookup(1024), Err(HostError::Unavailable));
        assert_eq!(repository.lookup(1031), Err(HostError::Unavailable));
        assert_eq!(repository.lookup(1025), Ok(VarbitLookup::Absent));
        assert_eq!(repository.definition_count(), 0);
        assert_eq!(repository.loaded_archive_count(), 0);
        assert_eq!(repository.expanded_bytes(), expanded);
    }
    let fixture = fixture();
    let mut repository = native_repository(false);
    let bytes = hex(fixture["archives"][0]["containerHex"].as_str().unwrap());
    let mut corrupted = bytes.clone();
    *corrupted.last_mut().unwrap() ^= 1;
    assert!(matches!(
        repository.load_archive(0, &corrupted),
        Err(RepositoryError::Asset(
            openrs_assets::Error::CrcMismatch { .. }
        ))
    ));
    assert_eq!(repository.lookup(0), Err(HostError::Unavailable));
    repository.load_archive(0, &bytes).unwrap();
    let expanded = repository.expanded_bytes();
    assert_eq!(
        repository.load_archive(0, &bytes),
        Err(RepositoryError::ArchiveAlreadyLoaded(0))
    );
    assert_eq!(
        repository.load_archive(2, &bytes),
        Err(RepositoryError::ArchiveAbsent(2))
    );
    assert_eq!(
        repository.load_archive(65536, &bytes),
        Err(RepositoryError::InvalidArchiveId(65536))
    );
    assert_eq!(repository.expanded_bytes(), expanded);
    assert_eq!(
        VarbitRepository::new().load_archive(0, &bytes),
        Err(RepositoryError::ReferenceUnavailable)
    );
}

#[test]
fn archive_whirlpool_is_verified_when_present_and_crc_only_tables_are_supported() {
    let bytes = container(&[0]);
    let mut metadata = archive(0, &[0], &bytes, true);
    metadata.whirlpool.as_mut().unwrap()[0] ^= 1;
    let mut invalid = repository(&[metadata], true);
    assert_eq!(
        invalid.load_archive(0, &bytes),
        Err(RepositoryError::Asset(
            openrs_assets::Error::WhirlpoolMismatch
        ))
    );
    assert_eq!(invalid.lookup(0), Err(HostError::Unavailable));
    let mut crc_only = repository(&[archive(0, &[0], &bytes, false)], false);
    crc_only.load_archive(0, &bytes).unwrap();
    assert_eq!(
        crc_only.lookup(0),
        Ok(VarbitLookup::Defined(Varbit {
            varp: 0,
            start: 0,
            end: 0
        }))
    );
    let invalid_bytes = container(&[0, 0, 0]);
    let mut invalid_split = repository(&[archive(0, &[0, 1], &invalid_bytes, true)], true);
    assert!(matches!(
        invalid_split.load_archive(0, &invalid_bytes),
        Err(RepositoryError::Asset(openrs_assets::Error::InvalidArchive))
    ));
    assert_eq!(invalid_split.lookup(0), Err(HostError::Unavailable));
}

#[test]
fn sparse_maximum_identity_is_bounded_without_allocating_the_identity_span() {
    let bytes = container(&[1, 255, 255, 255, 255, 0]);
    let mut repository = repository(&[archive(65535, &[1023], &bytes, false)], false);
    assert_eq!(
        repository.lookup(MAX_VARBIT_ID),
        Err(HostError::Unavailable)
    );
    assert_eq!(
        repository.lookup(MAX_VARBIT_ID - 1),
        Ok(VarbitLookup::Absent)
    );
    repository.load_archive(65535, &bytes).unwrap();
    assert_eq!(
        repository.lookup(MAX_VARBIT_ID),
        Ok(VarbitLookup::Defined(Varbit {
            varp: 65535,
            start: 255,
            end: 255
        }))
    );
    assert_eq!(repository.definition_count(), 1);
    assert_eq!(repository.loaded_archive_count(), 1);
    assert!(repository.expanded_bytes() < 128);
}

#[test]
fn reference_and_archive_resource_limits_reject_before_publication() {
    let bytes = container(&[0]);
    for metadata in [
        vec![archive(0, &[1024], &bytes, false)],
        vec![archive(0, &(0..1025).collect::<Vec<_>>(), &bytes, false)],
        (0..=MAX_REFERENCE_ARCHIVES as u32)
            .map(|id| archive(id, &[0], &bytes, false))
            .collect(),
        (0..65)
            .map(|id| archive(id, &(0..1024).collect::<Vec<_>>(), &bytes, false))
            .collect(),
    ] {
        let reference = reference(&metadata, false);
        let mut repository = VarbitRepository::new();
        assert!(matches!(
            repository.load_reference(&reference, &version(&reference)),
            Err(RepositoryError::Asset(
                openrs_assets::Error::LimitExceeded { .. }
            ))
        ));
        assert!(!repository.reference_loaded());
    }
    let mut oversized = vec![0; MAX_REFERENCE_BYTES + 1];
    assert!(matches!(
        VarbitRepository::new().load_reference(&oversized, &version(&oversized)),
        Err(RepositoryError::Asset(
            openrs_assets::Error::LimitExceeded { .. }
        ))
    ));
    oversized.truncate(MAX_ARCHIVE_BYTES + 1);
    let mut repository = repository(&[archive(0, &[0], &oversized, false)], false);
    assert!(matches!(
        repository.load_archive(0, &oversized),
        Err(RepositoryError::Asset(
            openrs_assets::Error::LimitExceeded { .. }
        ))
    ));
    assert_eq!(repository.lookup(0), Err(HostError::Unavailable));
}

#[test]
fn loaded_archive_and_total_expansion_budgets_are_global_and_atomic() {
    let bytes = container(&[0]);
    let archives: Vec<_> = (0..=MAX_LOADED_ARCHIVES as u32)
        .map(|id| archive(id, &[0], &bytes, false))
        .collect();
    let mut repository = repository(&archives, false);
    for id in 0..MAX_LOADED_ARCHIVES as u32 {
        repository.load_archive(id, &bytes).unwrap();
    }
    let expanded = repository.expanded_bytes();
    assert_eq!(
        repository.load_archive(MAX_LOADED_ARCHIVES as u32, &bytes),
        Err(RepositoryError::LoadedArchiveLimit)
    );
    assert_eq!(repository.expanded_bytes(), expanded);
    assert_eq!(
        repository.lookup((MAX_LOADED_ARCHIVES as i32) << 10),
        Err(HostError::Unavailable)
    );
    let mut largest = [1, 0, 0, 0, 0].repeat(64);
    largest.push(0);
    let bytes = container(&packed(&vec![largest; 1024]));
    let archives: Vec<_> = (0..20)
        .map(|id| archive(id, &(0..1024).collect::<Vec<_>>(), &bytes, false))
        .collect();
    let reference_bytes = reference(&archives, false);
    let mut repository = VarbitRepository::new();
    repository
        .load_reference(&reference_bytes, &version(&reference_bytes))
        .unwrap();
    let mut accepted = 0;
    loop {
        let expanded = repository.expanded_bytes();
        if expanded + bytes.len() - 5 > MAX_REPOSITORY_BYTES {
            assert!(repository.load_archive(accepted, &bytes).is_err());
            assert_eq!(repository.expanded_bytes(), expanded);
            assert_eq!(
                repository.lookup((accepted as i32) << 10),
                Err(HostError::Unavailable)
            );
            break;
        }
        repository.load_archive(accepted, &bytes).unwrap();
        accepted += 1;
    }
    assert!((10..20).contains(&accepted));
    assert_eq!(repository.loaded_archive_count(), accepted as usize);
    assert_eq!(repository.definition_count(), accepted as usize * 1024);
}

#[test]
fn real_decoded_definition_host_reads_writes_and_native_masks_match_copied_provider_oracle() {
    let fixture = fixture();
    let definitions = native_repository(true);
    let cases = fixture["hostCases"].as_array().unwrap();
    assert_eq!(cases.len(), 56);
    for case in cases {
        let id = case["id"].as_i64().unwrap() as i32;
        let value = case["value"].as_i64().unwrap() as i32;
        let varp = case["varp"].as_u64().unwrap() as u16;
        let mut variables = WorldVariables::new(65536).unwrap();
        variables.set_local(varp, 0x12345678, 100).unwrap();
        variables
            .apply(
                VariablePacket::VarpLarge {
                    id: varp,
                    value: -727,
                },
                100,
            )
            .unwrap();
        let script = host_script(&[
            (Opcode::PUSH_VARBIT, id),
            (Opcode::PUSH_CONSTANT_INT, value),
            (Opcode::POP_VARBIT, id),
            (Opcode::PUSH_VARBIT, id),
            (Opcode::RETURN, 0),
        ]);
        let mut vm = Vm::default();
        vm.run_with_host(
            0,
            &BTreeMap::from([(0, script)]),
            Arguments::default(),
            &mut WorldVarHost::with_varbits(&mut variables, 200, &definitions),
        )
        .unwrap();
        assert_eq!(
            vm.int_stack(),
            [
                case["before"].as_i64().unwrap() as i32,
                case["after"].as_i64().unwrap() as i32
            ],
            "{case}"
        );
        assert_eq!(
            variables.active(varp).unwrap(),
            case["active"].as_i64().unwrap() as i32,
            "{case}"
        );
        assert_eq!(
            variables.pending(varp).unwrap(),
            case["pending"].as_i64().unwrap() as i32,
            "{case}"
        );
        assert_eq!(variables.scheduled_count(), 1);
        assert!(variables.service(700).unwrap().is_empty());
        assert_eq!(variables.scheduled_count(), 1);
        assert!(variables.service(701).unwrap().is_empty());
        assert_eq!(variables.scheduled_count(), 0);
    }
}

#[test]
fn host_repository_errors_and_invalid_parent_varps_never_mutate_world_variables() {
    let unknown = VarbitRepository::new();
    let reference_only = native_repository(false);
    let loaded = native_repository(true);
    for (definitions, id, expected) in [
        (&unknown, 1, HostError::Unavailable),
        (&reference_only, 1031, HostError::Unavailable),
        (&loaded, -1, HostError::InvalidVarbit(-1)),
        (
            &loaded,
            MAX_VARBIT_ID + 1,
            HostError::InvalidVarbit(MAX_VARBIT_ID + 1),
        ),
        (&loaded, 1031, HostError::InvalidVarp(568)),
    ] {
        for opcode in [Opcode::PUSH_VARBIT, Opcode::POP_VARBIT] {
            let mut variables = WorldVariables::new(3).unwrap();
            variables.set_local(2, 9, 100).unwrap();
            let before = variables.clone();
            let script = host_script(&[
                (Opcode::PUSH_CONSTANT_INT, 727),
                (Opcode::PUSH_CONSTANT_INT, 5),
                (opcode, id),
                (Opcode::RETURN, 0),
            ]);
            let mut vm = Vm::default();
            let error = vm
                .run_with_host(
                    0,
                    &BTreeMap::from([(0, script)]),
                    Arguments::default(),
                    &mut WorldVarHost::with_varbits(&mut variables, 100, definitions),
                )
                .unwrap_err();
            assert_eq!(error.kind, VmErrorKind::Host(expected.clone()));
            assert_eq!(
                vm.int_stack(),
                if opcode == Opcode::POP_VARBIT {
                    &[727][..]
                } else {
                    &[727, 5][..]
                }
            );
            assert_eq!(variables, before);
        }
    }
    let mut variables = WorldVariables::new(3).unwrap();
    variables.set_local(2, 9, 100).unwrap();
    let before = variables.clone();
    let mut host = WorldVarHost::with_varbits(&mut variables, 99, &loaded);
    assert_eq!(host.varp(2), Ok(9));
    assert_eq!(
        host.set_varp(2, 8),
        Err(HostError::State("invalid world variable clock".into()))
    );
    assert_eq!(variables, before);
    assert_eq!(
        WorldVarHost::new(&mut variables, 100).varbit(1031),
        Err(HostError::Unavailable)
    );
}

#[test]
fn bit_helpers_match_native_player_var_domain_cases() {
    let fixture = fixture();
    let definitions = native_repository(true);
    let (mut checked, mut absent) = (0, 0);
    for case in fixture["hostCases"].as_array().unwrap() {
        let id = case["id"].as_i64().unwrap() as i32;
        let value = case["value"].as_i64().unwrap() as i32;
        let openrs_cs2::VarbitLookup::Defined(definition) = definitions.lookup(id).unwrap() else {
            absent += 1;
            continue;
        };
        checked += 1;
        let active = case["active"].as_i64().unwrap() as i32;
        let initial = 0x1234_5678;
        assert_eq!(read_bits(&definition, initial), case["before"].as_i64().unwrap() as i32, "{case}");
        let written = write_bits(&definition, initial, value);
        assert_eq!(read_bits(&definition, written), case["after"].as_i64().unwrap() as i32, "{case}");
        assert_eq!(written, active, "{case}");
    }
    assert!(checked > 0);
    assert_eq!(checked + absent, 56);
}
