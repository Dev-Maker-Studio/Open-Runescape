use openrs_assets::{
    parse_reference_table, parse_reference_table_by_names, Error, Limits, ReferenceProjectionLimits,
};

#[cfg(not(target_arch = "wasm32"))]
mod allocation {
    use std::alloc::{GlobalAlloc, Layout, System};
    use std::cell::Cell;

    thread_local! {
        static ACTIVE: Cell<bool> = const { Cell::new(false) };
        static BYTES: Cell<usize> = const { Cell::new(0) };
    }

    pub struct Tracked;

    unsafe impl GlobalAlloc for Tracked {
        unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
            if ACTIVE.try_with(Cell::get).unwrap_or(false) {
                let _ =
                    BYTES.try_with(|bytes| bytes.set(bytes.get().saturating_add(layout.size())));
            }
            unsafe { System.alloc(layout) }
        }

        unsafe fn dealloc(&self, pointer: *mut u8, layout: Layout) {
            unsafe { System.dealloc(pointer, layout) }
        }

        unsafe fn realloc(&self, pointer: *mut u8, layout: Layout, size: usize) -> *mut u8 {
            if ACTIVE.try_with(Cell::get).unwrap_or(false) {
                let _ = BYTES.try_with(|bytes| bytes.set(bytes.get().saturating_add(size)));
            }
            unsafe { System.realloc(pointer, layout, size) }
        }
    }

    pub fn measure<T>(operation: impl FnOnce() -> T) -> (T, usize) {
        BYTES.with(|bytes| bytes.set(0));
        ACTIVE.with(|active| active.set(true));
        let result = operation();
        ACTIVE.with(|active| active.set(false));
        (result, BYTES.with(Cell::get))
    }
}

#[cfg(not(target_arch = "wasm32"))]
#[global_allocator]
static ALLOCATOR: allocation::Tracked = allocation::Tracked;

fn limits() -> ReferenceProjectionLimits {
    ReferenceProjectionLimits {
        table: Limits {
            max_uncompressed_bytes: 8 * 1024 * 1024,
            max_archives: 262_144,
            max_total_files: 262_144,
            ..Limits::default()
        },
        max_selected_archives: 36,
        max_selected_files: 4096,
        max_total_records: 524_288,
    }
}

fn smart(output: &mut Vec<u8>, value: u32) {
    if value >= 32768 {
        output.extend_from_slice(&(value | 0x8000_0000).to_be_bytes());
    } else {
        output.extend_from_slice(&(value as u16).to_be_bytes());
    }
}

struct Encoded {
    payload: Vec<u8>,
    ids: usize,
    names: usize,
    counts: usize,
    files: usize,
    file_names: usize,
}

fn dense(archives: usize, files_per_archive: usize) -> Encoded {
    let mut payload = vec![7, 0, 0, 1, 48, 1];
    smart(&mut payload, archives as u32);
    let ids = payload.len();
    for id in 0..archives {
        smart(&mut payload, u32::from(id != 0));
    }
    let names = payload.len();
    for id in 0..archives {
        payload.extend_from_slice(&(id as i32).to_be_bytes());
    }
    for id in 0..archives {
        payload.extend_from_slice(&((id as u32) ^ 0x5aa5).to_be_bytes());
    }
    for id in 0..archives {
        payload.extend_from_slice(&(id as i32 + 17).to_be_bytes());
    }
    let counts = payload.len();
    for _ in 0..archives {
        smart(&mut payload, files_per_archive as u32);
    }
    let files = payload.len();
    for _ in 0..archives {
        for id in 0..files_per_archive {
            smart(&mut payload, u32::from(id != 0));
        }
    }
    let file_names = payload.len();
    for _ in 0..archives {
        for id in 0..files_per_archive {
            payload.extend_from_slice(&(id as i32 + 101).to_be_bytes());
        }
    }
    Encoded {
        payload,
        ids,
        names,
        counts,
        files,
        file_names,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn projected_metadata_agrees_with_every_pinned_native_protocol_and_flags_oracle() {
    macro_rules! fixture {
        ($protocol:literal, $flags:literal) => {
            &include_bytes!(concat!(
                "fixtures/table-",
                $protocol,
                "-",
                $flags,
                ".payload"
            ))[..]
        };
    }
    let fixtures = [
        fixture!(5, 0),
        fixture!(5, 1),
        fixture!(5, 2),
        fixture!(5, 3),
        fixture!(6, 0),
        fixture!(6, 1),
        fixture!(6, 2),
        fixture!(6, 3),
        fixture!(7, 0),
        fixture!(7, 1),
        fixture!(7, 2),
        fixture!(7, 3),
    ];
    for payload in fixtures {
        let original = parse_reference_table(payload, &Limits::default()).unwrap();
        let names = original
            .archives
            .iter()
            .filter_map(|archive| archive.name_hash)
            .collect::<Vec<_>>();
        let projection = parse_reference_table_by_names(payload, &names, &limits()).unwrap();
        assert_eq!(projection.archive_count, original.archives.len());
        assert_eq!(
            projection.total_files,
            original
                .archives
                .iter()
                .map(|archive| archive.files.len())
                .sum::<usize>()
        );
        assert_eq!(
            projection.scan_operations,
            payload.len() + projection.archive_count * (names.len() + 3) + projection.total_files
        );
        assert_eq!(projection.table.protocol, original.protocol);
        assert_eq!(projection.table.revision, original.revision);
        assert_eq!(projection.table.flags, original.flags);
        if original.flags & 1 != 0 {
            assert_eq!(projection.table, original);
            let one = parse_reference_table_by_names(payload, &names[1..], &limits()).unwrap();
            assert_eq!(one.table.archives, original.archives[1..]);
        } else {
            assert!(projection.table.archives.is_empty());
        }
        let none = parse_reference_table_by_names(payload, &[], &limits()).unwrap();
        assert!(none.table.archives.is_empty());
        assert_eq!(none.archive_count, original.archives.len());
        for end in 0..payload.len() {
            assert!(parse_reference_table_by_names(&payload[..end], &[], &limits()).is_err());
        }
        let mut trailing = payload.to_vec();
        trailing.push(0);
        assert_eq!(
            parse_reference_table_by_names(&trailing, &names, &limits()),
            Err(Error::TrailingData)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dense_native_sized_table_retains_only_requested_names_and_accounts_all_records() {
    let encoded = dense(262_144, 1);
    assert_eq!(encoded.payload.len(), 5_767_178);
    let names: Vec<i32> = (0..36).map(|index| index * 7480).collect();
    let result = parse_reference_table_by_names(&encoded.payload, &names, &limits()).unwrap();
    assert_eq!(result.archive_count, 262_144);
    assert_eq!(result.total_files, 262_144);
    assert_eq!(result.table.archives.len(), 36);
    assert_eq!(result.table.archives.capacity(), 36);
    assert_eq!(result.scan_operations, encoded.payload.len() + 262_144 * 40);
    for (archive, &name) in result.table.archives.iter().zip(&names) {
        assert_eq!(archive.id, name as u32);
        assert_eq!(archive.name_hash, Some(name));
        assert_eq!(archive.crc32, (name as u32) ^ 0x5aa5);
        assert_eq!(archive.revision, name + 17);
        assert_eq!(archive.files.len(), 1);
        assert_eq!(archive.files.capacity(), 1);
        assert_eq!(archive.files[0].id, 0);
        assert_eq!(archive.files[0].name_hash, Some(101));
    }
    assert_eq!(Limits::default().max_archives, 65_536);
    assert!(matches!(
        parse_reference_table(&encoded.payload, &Limits::default()),
        Err(Error::LimitExceeded {
            resource: "archives",
            value: 262_144,
            limit: 65_536
        })
    ));
    let none = parse_reference_table_by_names(&encoded.payload, &[-1], &limits()).unwrap();
    assert!(none.table.archives.is_empty());
    assert_eq!((none.archive_count, none.total_files), (262_144, 262_144));
}

#[cfg(not(target_arch = "wasm32"))]
#[test]
fn full_dense_scan_allocation_depends_on_selected_records_not_reference_size_or_ids() {
    let encoded = dense(262_144, 1);
    let names: Vec<i32> = (0..36).map(|index| index * 7480).collect();
    let limits = limits();
    let (result, allocated) =
        allocation::measure(|| parse_reference_table_by_names(&encoded.payload, &names, &limits));
    assert_eq!(result.unwrap().table.archives.len(), 36);
    assert!(
        allocated < 32 * 1024,
        "projection allocated {allocated} bytes"
    );
    let (result, allocated) =
        allocation::measure(|| parse_reference_table_by_names(&encoded.payload, &[], &limits));
    assert!(result.unwrap().table.archives.is_empty());
    assert_eq!(allocated, 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn limits_are_inclusive_and_archive_file_record_and_retained_plus_one_are_rejected() {
    let encoded = dense(2, 2);
    let mut exact = limits();
    exact.table.max_uncompressed_bytes = encoded.payload.len();
    exact.table.max_archives = 2;
    exact.table.max_files_per_archive = 2;
    exact.table.max_total_files = 4;
    exact.table.max_archive_id = 1;
    exact.table.max_file_id = 1;
    exact.max_selected_archives = 2;
    exact.max_selected_files = 4;
    exact.max_total_records = 6;
    assert!(parse_reference_table_by_names(&encoded.payload, &[0, 1], &exact).is_ok());
    let reductions: &[fn(&mut ReferenceProjectionLimits)] = &[
        |limits| limits.table.max_uncompressed_bytes -= 1,
        |limits| limits.table.max_archives -= 1,
        |limits| limits.table.max_files_per_archive -= 1,
        |limits| limits.table.max_total_files -= 1,
        |limits| limits.table.max_archive_id -= 1,
        |limits| limits.table.max_file_id -= 1,
        |limits| limits.max_selected_archives -= 1,
        |limits| limits.max_selected_files -= 1,
        |limits| limits.max_total_records -= 1,
    ];
    for reduce in reductions {
        let mut below = exact.clone();
        reduce(&mut below);
        assert!(matches!(
            parse_reference_table_by_names(&encoded.payload, &[0, 1], &below),
            Err(Error::LimitExceeded { .. })
        ));
    }
    let cases = [
        (dense(262_145, 0), "archives"),
        (dense(5, 52_429), "total files"),
        (dense(1, 65_537), "files per archive"),
        (dense(1, 4097), "selected files"),
    ];
    for (encoded, resource) in cases {
        let requested = if resource == "selected files" { 0 } else { -1 };
        assert!(
            matches!(parse_reference_table_by_names(&encoded.payload, &[requested], &limits()), Err(Error::LimitExceeded { resource: found, .. }) if found == resource)
        );
    }
    let encoded = dense(262_144, 1);
    let mut below = limits();
    below.max_total_records -= 1;
    assert!(matches!(
        parse_reference_table_by_names(&encoded.payload, &[0], &below),
        Err(Error::LimitExceeded {
            resource: "metadata records",
            value: 524_288,
            limit: 524_287
        })
    ));
    let too_many_names = [0; 37];
    assert!(matches!(
        parse_reference_table_by_names(&[], &too_many_names, &limits()),
        Err(Error::LimitExceeded {
            resource: "selected archive names",
            ..
        })
    ));
    let encoded = dense(1, 4096);
    assert_eq!(
        parse_reference_table_by_names(&encoded.payload, &[0], &limits())
            .unwrap()
            .table
            .archives[0]
            .files
            .len(),
        4096
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn only_ambiguity_in_a_requested_hash_is_rejected() {
    let mut encoded = dense(3, 1);
    let duplicate = encoded.payload[encoded.names..encoded.names + 4].to_vec();
    encoded.payload[encoded.names + 4..encoded.names + 8].copy_from_slice(&duplicate);
    let projection = parse_reference_table_by_names(&encoded.payload, &[2], &limits()).unwrap();
    assert_eq!(projection.table.archives[0].id, 2);
    assert_eq!(
        parse_reference_table_by_names(&encoded.payload, &[0], &limits()),
        Err(Error::AmbiguousNameHash)
    );
    assert_eq!(
        parse_reference_table_by_names(&encoded.payload, &[2, 2], &limits()),
        Err(Error::AmbiguousNameHash)
    );
    assert_eq!(
        Error::AmbiguousNameHash.to_string(),
        "ambiguous archive name hash"
    );
    let absent = parse_reference_table_by_names(&encoded.payload, &[-1], &limits()).unwrap();
    assert!(absent.table.archives.is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unselected_ids_counts_names_and_trailing_data_are_still_fully_validated() {
    for selected in [&[][..], &[0][..]] {
        let encoded = dense(3, 2);
        let mut invalid = encoded.payload.clone();
        invalid[encoded.ids + 2..encoded.ids + 4].fill(0);
        assert_eq!(
            parse_reference_table_by_names(&invalid, selected, &limits()),
            Err(Error::InvalidIds)
        );
        let mut invalid = encoded.payload.clone();
        invalid[encoded.files + 6..encoded.files + 8].fill(0);
        assert_eq!(
            parse_reference_table_by_names(&invalid, selected, &limits()),
            Err(Error::InvalidIds)
        );
        for offset in [encoded.ids + 2, encoded.files + 4] {
            let mut invalid = encoded.payload.clone();
            invalid.splice(
                offset..offset + 2,
                (1_000_001u32 | 0x8000_0000).to_be_bytes(),
            );
            assert!(matches!(
                parse_reference_table_by_names(&invalid, selected, &limits()),
                Err(Error::LimitExceeded {
                    resource: "archive ID" | "file ID",
                    ..
                })
            ));
        }
        let mut invalid = encoded.payload.clone();
        invalid[encoded.counts + 2..encoded.counts + 4].copy_from_slice(&3u16.to_be_bytes());
        assert!(parse_reference_table_by_names(&invalid, selected, &limits()).is_err());
        for end in [
            encoded.names + 5,
            encoded.file_names + 5,
            encoded.payload.len() - 1,
        ] {
            assert!(
                parse_reference_table_by_names(&encoded.payload[..end], selected, &limits())
                    .is_err()
            );
        }
        for protocol in [0, 4, 8, 255] {
            let mut invalid = encoded.payload.clone();
            invalid[0] = protocol;
            assert_eq!(
                parse_reference_table_by_names(&invalid, selected, &limits()),
                Err(Error::UnsupportedProtocol(protocol))
            );
        }
        for flags in [4, 128, 255] {
            let mut invalid = encoded.payload.clone();
            invalid[5] = flags;
            assert_eq!(
                parse_reference_table_by_names(&invalid, selected, &limits()),
                Err(Error::UnsupportedFlags(flags))
            );
        }
    }
}
