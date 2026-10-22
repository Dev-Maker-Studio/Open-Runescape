use openrs_assets::{
    decode_container, decrypt_container, is_zero_key, parse_reference_table, split_archive,
    trim_revision, Integrity, Limits,
};
use openrs_scene::loc_type::{canonical, decode_loc_type, ParamValue};
use openrs_scene::locs::{decode_region_locs, decode_scene_locs};
use openrs_scene::packet::PacketError;
#[cfg(target_arch = "wasm32")]
use wasm_bindgen_test::wasm_bindgen_test;

const PLACEMENTS: &str = include_str!("fixtures/loc-placements.tsv");
const DEFINITIONS: &str = include_str!("fixtures/loc-defs.tsv");
const XTEA: &str = include_str!("fixtures/loc-xtea.tsv");
const REFERENCE: &[u8] = include_bytes!("fixtures/loc-reference.container");
const REGIONS: [(i32, &[u8]); 6] = [
    (32650, include_bytes!("fixtures/loc-l32650.container")),
    (32651, include_bytes!("fixtures/loc-l32651.container")),
    (32906, include_bytes!("fixtures/loc-l32906.container")),
    (32907, include_bytes!("fixtures/loc-l32907.container")),
    (33162, include_bytes!("fixtures/loc-l33162.container")),
    (33163, include_bytes!("fixtures/loc-l33163.container")),
];
const ENCRYPTED: [(i32, usize, &[u8]); 18] = [
    (32650, 0, include_bytes!("fixtures/loc-xtea-l32650-0.bin")),
    (32650, 1, include_bytes!("fixtures/loc-xtea-l32650-1.bin")),
    (32650, 2, include_bytes!("fixtures/loc-xtea-l32650-2.bin")),
    (32651, 0, include_bytes!("fixtures/loc-xtea-l32651-0.bin")),
    (32651, 1, include_bytes!("fixtures/loc-xtea-l32651-1.bin")),
    (32651, 2, include_bytes!("fixtures/loc-xtea-l32651-2.bin")),
    (32906, 0, include_bytes!("fixtures/loc-xtea-l32906-0.bin")),
    (32906, 1, include_bytes!("fixtures/loc-xtea-l32906-1.bin")),
    (32906, 2, include_bytes!("fixtures/loc-xtea-l32906-2.bin")),
    (32907, 0, include_bytes!("fixtures/loc-xtea-l32907-0.bin")),
    (32907, 1, include_bytes!("fixtures/loc-xtea-l32907-1.bin")),
    (32907, 2, include_bytes!("fixtures/loc-xtea-l32907-2.bin")),
    (33162, 0, include_bytes!("fixtures/loc-xtea-l33162-0.bin")),
    (33162, 1, include_bytes!("fixtures/loc-xtea-l33162-1.bin")),
    (33162, 2, include_bytes!("fixtures/loc-xtea-l33162-2.bin")),
    (33163, 0, include_bytes!("fixtures/loc-xtea-l33163-0.bin")),
    (33163, 1, include_bytes!("fixtures/loc-xtea-l33163-1.bin")),
    (33163, 2, include_bytes!("fixtures/loc-xtea-l33163-2.bin")),
];
const DEFINITION_ARCHIVES: [(u32, &[u8]); 6] = [
    (76, include_bytes!("fixtures/loc-defs-76.container")),
    (81, include_bytes!("fixtures/loc-defs-81.container")),
    (196, include_bytes!("fixtures/loc-defs-196.container")),
    (241, include_bytes!("fixtures/loc-defs-241.container")),
    (252, include_bytes!("fixtures/loc-defs-252.container")),
    (288, include_bytes!("fixtures/loc-defs-288.container")),
];

fn payload(container: &[u8]) -> Vec<u8> {
    let trimmed = trim_revision(container).expect("trailer");
    decode_container(trimmed, Integrity::default(), &Limits::default()).expect("container")
}

fn rows(kind: &str, region: i32) -> Vec<Vec<i32>> {
    PLACEMENTS
        .lines()
        .map(|line| line.split('\t').collect::<Vec<_>>())
        .filter(|f| {
            f[0] == region.to_string() && f[1] == kind && f[2] != "PAYLOAD" && f[2] != "END"
        })
        .map(|f| f[2..].iter().map(|v| v.parse().unwrap()).collect())
        .collect()
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

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn region_placements_match_the_native_loop() {
    let mut total = 0;
    for (region, container) in REGIONS {
        let decoded = decode_region_locs(&payload(container)).unwrap();
        let expected = rows("l", region);
        let actual: Vec<Vec<i32>> = decoded
            .iter()
            .map(|p| vec![p.id, p.plane, p.local_x, p.local_y, p.kind, p.rotation])
            .collect();
        assert_eq!(actual, expected, "region {region}");
        total += actual.len();
    }
    assert_eq!(total, 590);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn stub_regions_decode_to_no_placements() {
    for region in [32650, 32651] {
        let container = REGIONS.iter().find(|r| r.0 == region).unwrap().1;
        assert_eq!(payload(container), vec![0]);
        assert!(decode_region_locs(&payload(container)).unwrap().is_empty());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn truncated_placements_are_rejected() {
    let data = payload(REGIONS[2].1);
    for cut in [1, 2, 3, data.len() / 2, data.len() - 2] {
        assert_eq!(
            decode_region_locs(&data[..cut]),
            Err(PacketError::Truncated)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn scene_placement_filters_and_offsets() {
    let payloads: Vec<(i32, Vec<u8>)> = REGIONS.iter().map(|(r, c)| (*r, payload(c))).collect();
    let borrowed: Vec<(i32, &[u8])> = payloads.iter().map(|(r, d)| (*r, d.as_slice())).collect();
    let scene = decode_scene_locs(&borrowed, 8160, 8840, 104).unwrap();
    let mut expected = Vec::new();
    for (region, _) in &borrowed {
        for row in rows("l", *region) {
            let x = row[2] + (region >> 8) * 64 - 8160;
            let y = row[3] + (region & 255) * 64 - 8840;
            if x > 0 && y > 0 && x < 103 && y < 103 {
                expected.push((row[0], row[1], x, y, row[4], row[5]));
            }
        }
    }
    let actual: Vec<_> = scene
        .iter()
        .map(|p| (p.id, p.plane, p.scene_x, p.scene_y, p.kind, p.rotation))
        .collect();
    assert_eq!(actual, expected);
    assert!(!actual.is_empty());
    assert_eq!(actual.len(), 590);
    let narrow = decode_scene_locs(&borrowed, 8192, 8832, 40).unwrap();
    assert!(!narrow.is_empty() && narrow.len() < 590);
    assert!(narrow
        .iter()
        .all(|p| p.scene_x > 0 && p.scene_y > 0 && p.scene_x < 39 && p.scene_y < 39));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn xtea_matches_the_native_cipher() {
    let keys = keys();
    assert_eq!(keys.len(), 3);
    for (region, index, encrypted) in ENCRYPTED {
        let original = REGIONS.iter().find(|r| r.0 == region).unwrap().1;
        let trimmed = trim_revision(original).unwrap();
        let restored = decrypt_container(encrypted, &keys[index]);
        assert_eq!(restored, trimmed, "region {region} key {index}");
        assert_ne!(encrypted, trimmed);
        assert_ne!(
            decrypt_container(encrypted, &keys[(index + 1) % 3]),
            trimmed
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn zero_key_leaves_the_container_untouched() {
    let original = trim_revision(REGIONS[2].1).unwrap();
    assert!(is_zero_key(&[0; 4]));
    assert!(!is_zero_key(&[0, 0, 0, 1]));
    assert_eq!(decrypt_container(original, &[0; 4]), original);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn revision_trailer_is_trimmed_only_when_it_is_two_bytes() {
    let trimmed = trim_revision(REGIONS[2].1).unwrap();
    assert_eq!(REGIONS[2].1.len(), trimmed.len());
    let mut with_trailer = trimmed.to_vec();
    with_trailer.extend_from_slice(&[0, 7]);
    assert_eq!(trim_revision(&with_trailer).unwrap(), trimmed);
    with_trailer.push(0);
    assert!(trim_revision(&with_trailer).is_none());
    assert!(trim_revision(&[]).is_none());
    assert!(trim_revision(&[9, 0, 0, 0, 0]).is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn definitions_match_the_native_canonical_form() {
    let limits = Limits::default();
    let table = parse_reference_table(&payload(REFERENCE), &limits).unwrap();
    let mut expected = DEFINITIONS.lines();
    let mut checked = 0;
    let mut with_params = 0;
    let mut with_transforms = 0;
    let mut restricted = 0;
    for (archive_id, container) in DEFINITION_ARCHIVES {
        let archive = table.archive(archive_id).unwrap();
        let files = split_archive(&payload(container), &archive.files, &limits).unwrap();
        for file in files {
            let id = ((archive_id << 8) | file.id) as i32;
            for member in [true, false] {
                let loc = decode_loc_type(id, Some(&file.data), member).unwrap();
                let line = format!("{id}\t{}\t{}", i32::from(member), canonical(&loc));
                assert_eq!(
                    Some(line.as_str()),
                    expected.next(),
                    "id {id} member {member}"
                );
                checked += 1;
                if loc.parameters.is_some() {
                    with_params += 1;
                }
                if loc.transforms {
                    with_transforms += 1;
                }
                if loc.actions.is_none() {
                    restricted += 1;
                }
            }
        }
    }
    assert!(expected.next().is_none());
    assert_eq!(checked, DEFINITIONS.lines().count());
    assert!(with_params > 0 && with_transforms > 0 && restricted > 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn missing_definition_data_yields_defaults() {
    let loc = decode_loc_type(5, None, true).unwrap();
    let actions = loc.actions.unwrap();
    assert_eq!(actions.len(), 6);
    assert_eq!(actions[5].as_deref(), Some("Examine"));
    assert!(loc.parameters.is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn parameter_values_keep_their_kind() {
    let limits = Limits::default();
    let table = parse_reference_table(&payload(REFERENCE), &limits).unwrap();
    let mut ints = 0;
    let mut texts = 0;
    for (archive_id, container) in DEFINITION_ARCHIVES {
        let archive = table.archive(archive_id).unwrap();
        for file in split_archive(&payload(container), &archive.files, &limits).unwrap() {
            let loc = decode_loc_type((archive_id << 8 | file.id) as i32, Some(&file.data), true)
                .unwrap();
            if let Some(params) = &loc.parameters {
                for (_, value) in params.iter() {
                    match value {
                        ParamValue::Int(_) => ints += 1,
                        ParamValue::Text(_) => texts += 1,
                    }
                }
            }
        }
    }
    assert!(ints + texts > 0);
}
