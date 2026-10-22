use openrs_assets::{
    decode_container, parse_reference_table, split_archive, Error, FileMetadata, Integrity, Limits,
    ReferenceTable,
};
use std::io::Write;

const RAW: &[u8] = include_bytes!("fixtures/raw.container");
const GZIP: &[u8] = include_bytes!("fixtures/gzip.container");
const BZIP: &[u8] = include_bytes!("fixtures/bzip2-multiblock.container");
const ARCHIVE: &[u8] = include_bytes!("fixtures/gzip.native");
const TABLE: &[u8] = include_bytes!("fixtures/table-7-3.payload");

fn fixture_containers() -> Vec<(&'static [u8], &'static [u8], &'static [u8])> {
    vec![
        (
            RAW,
            include_bytes!("fixtures/raw.native"),
            include_bytes!("fixtures/raw.whirlpool"),
        ),
        (GZIP, ARCHIVE, include_bytes!("fixtures/gzip.whirlpool")),
        (
            BZIP,
            include_bytes!("fixtures/bzip2-multiblock.native"),
            include_bytes!("fixtures/bzip2-multiblock.whirlpool"),
        ),
        (
            include_bytes!("fixtures/empty-0.container"),
            include_bytes!("fixtures/empty-0.native"),
            include_bytes!("fixtures/empty-0.whirlpool"),
        ),
        (
            include_bytes!("fixtures/empty-1.container"),
            include_bytes!("fixtures/empty-1.native"),
            include_bytes!("fixtures/empty-1.whirlpool"),
        ),
        (
            include_bytes!("fixtures/empty-2.container"),
            include_bytes!("fixtures/empty-2.native"),
            include_bytes!("fixtures/empty-2.whirlpool"),
        ),
    ]
}

fn fixture_tables() -> Vec<(u8, u8, &'static [u8], &'static [u8])> {
    macro_rules! entry {
        ($protocol:literal, $flags:literal, $prefix:literal) => {
            (
                $protocol,
                $flags,
                &include_bytes!(concat!("fixtures/", $prefix, ".payload"))[..],
                &include_bytes!(concat!("fixtures/", $prefix, ".native"))[..],
            )
        };
    }
    vec![
        entry!(5, 0, "table-5-0"),
        entry!(5, 1, "table-5-1"),
        entry!(5, 2, "table-5-2"),
        entry!(5, 3, "table-5-3"),
        entry!(6, 0, "table-6-0"),
        entry!(6, 1, "table-6-1"),
        entry!(6, 2, "table-6-2"),
        entry!(6, 3, "table-6-3"),
        entry!(7, 0, "table-7-0"),
        entry!(7, 1, "table-7-1"),
        entry!(7, 2, "table-7-2"),
        entry!(7, 3, "table-7-3"),
    ]
}

fn snapshot(table: &ReferenceTable) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(&table.revision.to_be_bytes());
    bytes.extend_from_slice(&(table.archives.len() as u32).to_be_bytes());
    for archive in &table.archives {
        bytes.extend_from_slice(&archive.id.to_be_bytes());
        bytes.extend_from_slice(&archive.crc32.to_be_bytes());
        bytes.extend_from_slice(&archive.revision.to_be_bytes());
        bytes.push(u8::from(archive.name_hash.is_some()));
        if let Some(hash) = archive.name_hash {
            bytes.extend_from_slice(&hash.to_be_bytes());
        }
        bytes.push(u8::from(archive.whirlpool.is_some()));
        if let Some(hash) = archive.whirlpool {
            bytes.extend_from_slice(&hash);
        }
        bytes.extend_from_slice(&(archive.files.len() as u32).to_be_bytes());
        for file in &archive.files {
            bytes.extend_from_slice(&file.id.to_be_bytes());
            bytes.push(u8::from(file.name_hash.is_some()));
            if let Some(hash) = file.name_hash {
                bytes.extend_from_slice(&hash.to_be_bytes());
            }
        }
    }
    bytes
}

fn decode(container: &[u8]) -> Result<Vec<u8>, Error> {
    decode_container(container, Integrity::default(), &Limits::default())
}

fn files(ids: &[u32]) -> Vec<FileMetadata> {
    ids.iter()
        .map(|id| FileMetadata {
            id: *id,
            name_hash: None,
        })
        .collect()
}

fn compressed(mode: u8, plain: &[u8]) -> Vec<u8> {
    let encoded = if mode == 1 {
        let mut encoder = bzip2::write::BzEncoder::new(Vec::new(), bzip2::Compression::new(1));
        encoder.write_all(plain).unwrap();
        encoder.finish().unwrap()[4..].to_vec()
    } else {
        let mut encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::default());
        encoder.write_all(plain).unwrap();
        encoder.finish().unwrap()
    };
    let mut result = vec![mode];
    result.extend_from_slice(&(encoded.len() as i32).to_be_bytes());
    result.extend_from_slice(&(plain.len() as i32).to_be_bytes());
    result.extend_from_slice(&encoded);
    result
}

fn table7(archive_ids: &[u32], file_deltas: &[&[u32]]) -> Vec<u8> {
    fn smart(bytes: &mut Vec<u8>, value: u32) {
        if value >= 32768 {
            bytes.extend_from_slice(&(value | 0x8000_0000).to_be_bytes());
        } else {
            bytes.extend_from_slice(&(value as u16).to_be_bytes());
        }
    }
    let mut bytes = vec![7, 0, 0, 0, 42, 0];
    smart(&mut bytes, archive_ids.len() as u32);
    for id in archive_ids {
        smart(&mut bytes, *id);
    }
    bytes.resize(bytes.len() + archive_ids.len() * 8, 0);
    for ids in file_deltas {
        smart(&mut bytes, ids.len() as u32);
    }
    for ids in file_deltas {
        for id in *ids {
            smart(&mut bytes, *id);
        }
    }
    bytes
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_container_and_whirlpool_oracles() {
    for (container, expected, whirlpool) in fixture_containers() {
        let integrity = Integrity {
            crc32: Some(crc32fast::hash(container)),
            whirlpool: Some(whirlpool.try_into().unwrap()),
        };
        assert_eq!(
            decode_container(container, integrity, &Limits::default()).unwrap(),
            expected
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_reference_table_oracles() {
    for (protocol, flags, payload, expected) in fixture_tables() {
        let table = parse_reference_table(payload, &Limits::default()).unwrap();
        assert_eq!((table.protocol, table.flags), (protocol, flags));
        assert_eq!(snapshot(&table), expected);
        assert_eq!(table.archives.len(), 2);
        assert!(table.archive(2).is_none());
        assert_eq!(table.archive(3).unwrap().files.len(), 3);
        assert!(table.archive(3).unwrap().file(1).is_none());
        assert_eq!(table.archive(40000).unwrap().file(2).unwrap().id, 2);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_multifile_archive_oracle_with_negative_deltas() {
    let table = parse_reference_table(TABLE, &Limits::default()).unwrap();
    let output = split_archive(
        ARCHIVE,
        &table.archive(3).unwrap().files,
        &Limits::default(),
    )
    .unwrap();
    let expected: &[(u32, &[u8])] = &[
        (0, include_bytes!("fixtures/archive-file-0.native")),
        (7, include_bytes!("fixtures/archive-file-7.native")),
        (40000, include_bytes!("fixtures/archive-file-40000.native")),
    ];
    assert_eq!(output.len(), expected.len());
    for (file, (id, data)) in output.iter().zip(expected) {
        assert_eq!(file.id, *id);
        assert_eq!(&file.data, data);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn integrity_checks_cover_header_and_compressed_payload() {
    for (container, _, _) in fixture_containers() {
        assert!(matches!(
            decode_container(
                container,
                Integrity {
                    crc32: Some(crc32fast::hash(container) ^ 1),
                    whirlpool: None
                },
                &Limits::default()
            ),
            Err(Error::CrcMismatch { .. })
        ));
        assert_eq!(
            decode_container(
                container,
                Integrity {
                    crc32: None,
                    whirlpool: Some(&[0; 64])
                },
                &Limits::default()
            ),
            Err(Error::WhirlpoolMismatch)
        );
    }
    let mut invalid = GZIP.to_vec();
    invalid[9] ^= 1;
    assert!(matches!(
        decode_container(
            &invalid,
            Integrity {
                crc32: Some(crc32fast::hash(GZIP)),
                whirlpool: None
            },
            &Limits::default()
        ),
        Err(Error::CrcMismatch { .. })
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn container_lengths_and_limits_are_fail_closed() {
    for mode in [0, 1, 2, 3, 4, 255] {
        let mut bytes = vec![mode];
        bytes.extend_from_slice(&(-1i32).to_be_bytes());
        assert_eq!(decode(&bytes), Err(Error::InvalidLength));
    }
    for mode in [3, 4, 255] {
        assert_eq!(
            decode(&[mode, 0, 0, 0, 0]),
            Err(Error::UnsupportedCompression(mode))
        );
    }
    for (container, plain, _) in fixture_containers() {
        let limits = Limits {
            max_container_bytes: container.len() - 1,
            ..Limits::default()
        };
        assert!(matches!(
            decode_container(container, Integrity::default(), &limits),
            Err(Error::LimitExceeded {
                resource: "container bytes",
                ..
            })
        ));
        if !plain.is_empty() {
            let limits = Limits {
                max_uncompressed_bytes: plain.len() - 1,
                ..Limits::default()
            };
            assert!(matches!(
                decode_container(container, Integrity::default(), &limits),
                Err(Error::LimitExceeded {
                    resource: "uncompressed bytes",
                    ..
                })
            ));
        }
        let mut trailing = container.to_vec();
        trailing.extend_from_slice(&[0, 1]);
        assert_eq!(decode(&trailing), Err(Error::TrailingData));
        let mut truncated_length = container.to_vec();
        let length = i32::from_be_bytes(container[1..5].try_into().unwrap());
        truncated_length[1..5].copy_from_slice(&(length + 1).to_be_bytes());
        assert_eq!(decode(&truncated_length), Err(Error::Truncated));
    }
    for mode in [1, 2] {
        let mut container = compressed(mode, b"limit");
        container[5..9].copy_from_slice(&(-1i32).to_be_bytes());
        assert_eq!(decode(&container), Err(Error::InvalidLength));
        container[5..9].copy_from_slice(&i32::MAX.to_be_bytes());
        assert!(matches!(
            decode(&container),
            Err(Error::LimitExceeded { .. })
        ));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn compressed_members_must_end_exactly_and_validate_checksums() {
    for mode in [1, 2] {
        let original = compressed(mode, b"bounded decompression");
        for suffix in [&[0][..], &original[9..]] {
            let mut extra = original.clone();
            extra.extend_from_slice(suffix);
            let count = (extra.len() - 9) as i32;
            extra[1..5].copy_from_slice(&count.to_be_bytes());
            assert!(decode(&extra).is_err());
        }
        for size in [0i32, 1, 20, 22, 100] {
            let mut wrong_size = original.clone();
            wrong_size[5..9].copy_from_slice(&size.to_be_bytes());
            assert!(decode(&wrong_size).is_err());
        }
        for end in 9..original.len() {
            let mut truncated = original[..end].to_vec();
            truncated[1..5].copy_from_slice(&((end - 9) as i32).to_be_bytes());
            assert!(decode(&truncated).is_err(), "mode {mode}, end {end}");
        }
        let mut corrupt = original.clone();
        let pos = if mode == 1 { 15 } else { original.len() - 8 };
        corrupt[pos] ^= 1;
        assert!(decode(&corrupt).is_err());
    }
    let mut flags = GZIP.to_vec();
    flags[12] = 8;
    assert_eq!(decode(&flags), Err(Error::InvalidCompression));
    let mut wrapped = compressed(1, b"headerless");
    wrapped.splice(9..9, *b"BZh1");
    let length = (wrapped.len() - 9) as i32;
    wrapped[1..5].copy_from_slice(&length.to_be_bytes());
    assert!(decode(&wrapped).is_err());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn decompression_bombs_stop_at_declared_output() {
    for mode in [1, 2] {
        let mut bytes = compressed(mode, &vec![42; 1_000_000]);
        bytes[5..9].copy_from_slice(&1i32.to_be_bytes());
        assert_eq!(decode(&bytes), Err(Error::InvalidLength));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reference_tables_reject_all_truncations_and_unknown_flags() {
    for (_, _, payload, _) in fixture_tables() {
        for end in 0..payload.len() {
            assert!(parse_reference_table(&payload[..end], &Limits::default()).is_err());
        }
        let mut extra = payload.to_vec();
        extra.push(0);
        assert_eq!(
            parse_reference_table(&extra, &Limits::default()),
            Err(Error::TrailingData)
        );
    }
    for protocol in [0, 4, 8, 255] {
        assert_eq!(
            parse_reference_table(&[protocol], &Limits::default()),
            Err(Error::UnsupportedProtocol(protocol))
        );
    }
    for flag in 4..=255 {
        let bytes = [5, flag, 0, 0];
        assert_eq!(
            parse_reference_table(&bytes, &Limits::default()),
            Err(Error::UnsupportedFlags(flag))
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reference_table_count_and_sparse_id_limits_are_independent() {
    let cases = [
        (
            Limits {
                max_archives: 1,
                ..Limits::default()
            },
            "archives",
        ),
        (
            Limits {
                max_files_per_archive: 2,
                ..Limits::default()
            },
            "files per archive",
        ),
        (
            Limits {
                max_total_files: 3,
                ..Limits::default()
            },
            "total files",
        ),
        (
            Limits {
                max_archive_id: 39999,
                ..Limits::default()
            },
            "archive ID",
        ),
        (
            Limits {
                max_file_id: 39999,
                ..Limits::default()
            },
            "file ID",
        ),
        (
            Limits {
                max_uncompressed_bytes: TABLE.len() - 1,
                ..Limits::default()
            },
            "reference-table bytes",
        ),
    ];
    for (limits, resource) in cases {
        assert!(
            matches!(parse_reference_table(TABLE, &limits), Err(Error::LimitExceeded { resource: actual, .. }) if actual == resource)
        );
    }
    let empty = parse_reference_table(&[5, 0, 0, 0], &Limits::default()).unwrap();
    assert!(empty.archives.is_empty());
    let mut huge_count = vec![7, 0, 0, 0, 0, 0];
    huge_count.extend_from_slice(&u32::MAX.to_be_bytes());
    assert!(matches!(
        parse_reference_table(&huge_count, &Limits::default()),
        Err(Error::LimitExceeded { .. })
    ));
    let unbounded = Limits {
        max_archives: usize::MAX,
        max_total_files: usize::MAX,
        max_files_per_archive: usize::MAX,
        ..Limits::default()
    };
    assert_eq!(
        parse_reference_table(&huge_count, &unbounded),
        Err(Error::Truncated)
    );
    let mut huge_files = table7(&[0], &[&[]]);
    huge_files.splice(18..20, u32::MAX.to_be_bytes());
    assert_eq!(
        parse_reference_table(&huge_files, &unbounded),
        Err(Error::Truncated)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reference_table_ids_are_monotonic_and_do_not_overflow_native_ints() {
    for bytes in [table7(&[0, 0], &[&[], &[]]), table7(&[0], &[&[0, 0]])] {
        assert_eq!(
            parse_reference_table(&bytes, &Limits::default()),
            Err(Error::InvalidIds)
        );
    }
    let limits = Limits {
        max_archive_id: u32::MAX,
        max_file_id: u32::MAX,
        ..Limits::default()
    };
    for bytes in [
        table7(&[i32::MAX as u32], &[&[]]),
        table7(&[0x7fff_fffe, 2], &[&[], &[]]),
        table7(&[0], &[&[0x7fff_fffe, 2]]),
    ] {
        assert_eq!(
            parse_reference_table(&bytes, &limits),
            Err(Error::InvalidIds)
        );
    }
    let bytes = table7(&[32767, 1], &[&[32767, 1], &[]]);
    let table = parse_reference_table(&bytes, &Limits::default()).unwrap();
    assert_eq!(table.archives[0].id, 32767);
    assert_eq!(table.archives[1].id, 32768);
    assert_eq!(table.archives[0].files[1].id, 32768);
    let mut noncanonical = table7(&[0], &[&[0]]);
    noncanonical.splice(6..8, 0x8000_0001u32.to_be_bytes());
    assert_eq!(
        parse_reference_table(&noncanonical, &Limits::default())
            .unwrap()
            .archives
            .len(),
        1
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn splitting_preserves_single_empty_and_zero_length_files() {
    let limits = Limits::default();
    assert_eq!(
        split_archive(b"single\0\xff", &files(&[70000]), &limits).unwrap()[0].data,
        b"single\0\xff"
    );
    assert_eq!(
        split_archive(&[], &files(&[0]), &limits).unwrap()[0].data,
        b""
    );
    assert!(split_archive(&[], &[], &limits).unwrap().is_empty());
    assert_eq!(
        split_archive(b"orphan", &[], &limits),
        Err(Error::InvalidArchive)
    );
    let mut empty = vec![0; 8];
    empty.push(1);
    let output = split_archive(&empty, &files(&[0, 7]), &limits).unwrap();
    assert!(output.iter().all(|file| file.data.is_empty()));
    let mut many = vec![0; 2 * 255 * 4];
    many.push(255);
    assert!(split_archive(&many, &files(&[0, 1]), &limits)
        .unwrap()
        .iter()
        .all(|file| file.data.is_empty()));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_archive_tables_and_ids_are_rejected() {
    let limits = Limits::default();
    let metadata = files(&[0, 7, 40000]);
    for end in 0..ARCHIVE.len() {
        assert!(split_archive(&ARCHIVE[..end], &metadata, &limits).is_err());
    }
    let mut extra_data = ARCHIVE.to_vec();
    extra_data.insert(0, 0);
    assert_eq!(
        split_archive(&extra_data, &metadata, &limits),
        Err(Error::InvalidArchive)
    );
    for values in [[-1, 0], [i32::MAX, 1], [1, -2], [1, 0]] {
        let mut bytes = Vec::new();
        for value in values {
            bytes.extend_from_slice(&value.to_be_bytes());
        }
        bytes.push(1);
        assert_eq!(
            split_archive(&bytes, &files(&[0, 1]), &limits),
            Err(Error::InvalidArchive)
        );
    }
    assert_eq!(
        split_archive(&[0], &files(&[0, 1]), &limits),
        Err(Error::InvalidArchive)
    );
    for ids in [&[0, 0][..], &[7, 0], &[i32::MAX as u32]] {
        assert_eq!(
            split_archive(ARCHIVE, &files(ids), &limits),
            Err(Error::InvalidIds)
        );
    }
    for limits in [
        Limits {
            max_uncompressed_bytes: ARCHIVE.len() - 1,
            ..Limits::default()
        },
        Limits {
            max_files_per_archive: 2,
            ..Limits::default()
        },
        Limits {
            max_total_files: 2,
            ..Limits::default()
        },
        Limits {
            max_file_id: 39999,
            ..Limits::default()
        },
    ] {
        assert!(matches!(
            split_archive(ARCHIVE, &metadata, &limits),
            Err(Error::LimitExceeded { .. })
        ));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounded_malformed_corpus_never_panics_or_exceeds_limits() {
    let limits = Limits {
        max_container_bytes: 2048,
        max_uncompressed_bytes: 4096,
        max_archives: 32,
        max_files_per_archive: 32,
        max_total_files: 64,
        max_archive_id: 1024,
        max_file_id: 1024,
    };
    let metadata = files(&[0, 7, 10]);
    let mut state = 0x7270_5eedu32;
    for iteration in 0..5000 {
        let length = iteration % 513;
        let mut bytes = Vec::with_capacity(length);
        for _ in 0..length {
            state ^= state << 13;
            state ^= state >> 17;
            state ^= state << 5;
            bytes.push(state as u8);
        }
        if let Ok(output) = decode_container(&bytes, Integrity::default(), &limits) {
            assert!(output.len() <= limits.max_uncompressed_bytes);
        }
        if let Ok(table) = parse_reference_table(&bytes, &limits) {
            assert!(table.archives.len() <= limits.max_archives);
            assert!(
                table
                    .archives
                    .iter()
                    .map(|archive| archive.files.len())
                    .sum::<usize>()
                    <= limits.max_total_files
            );
        }
        if let Ok(output) = split_archive(&bytes, &metadata, &limits) {
            assert!(
                output.iter().map(|file| file.data.len()).sum::<usize>()
                    <= limits.max_uncompressed_bytes
            );
        }
    }
    for seed in [RAW, GZIP, TABLE, ARCHIVE] {
        for position in 0..seed.len() {
            for bit in 0..8 {
                let mut bytes = seed.to_vec();
                bytes[position] ^= 1 << bit;
                let _ = decode_container(&bytes, Integrity::default(), &limits);
                let _ = parse_reference_table(&bytes, &limits);
                let _ = split_archive(&bytes, &metadata, &limits);
            }
        }
    }
}
