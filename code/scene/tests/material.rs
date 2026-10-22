use openrs_scene::material::{
    is_opaque_effect, Material, MaterialError, MaterialErrorKind, MaterialLookupError,
    MaterialTable, BYTES_PER_MATERIAL, MAX_DEFINITION_BYTES, MAX_MATERIALS,
};
use std::collections::BTreeMap;
use std::str::from_utf8;

const NATIVE: &str = include_str!("fixtures/material.tsv");
const WIDTHS: [usize; 19] = [1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1];

fn hex(payload: &str) -> Vec<u8> {
    assert_eq!(payload.len() % 2, 0);
    payload
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

fn material_fields(value: &Material) -> [i32; 19] {
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

fn native_tables() -> BTreeMap<&'static str, MaterialTable> {
    NATIVE
        .lines()
        .filter(|line| line.starts_with("table\t"))
        .map(|line| {
            let fields: Vec<_> = line.split('\t').collect();
            (fields[1], MaterialTable::decode(&hex(fields[2])).unwrap())
        })
        .collect()
}

fn dense_input() -> Vec<u8> {
    let mut input = Vec::with_capacity(MAX_DEFINITION_BYTES);
    input.extend_from_slice(&u16::MAX.to_be_bytes());
    input.resize(2 + MAX_MATERIALS, 1);
    for (column, width) in WIDTHS.into_iter().enumerate() {
        for id in 0..MAX_MATERIALS {
            let value = match column {
                7 => id as u32,
                17 => (id as u32)
                    .wrapping_mul(1_664_525)
                    .wrapping_add(1_013_904_223),
                _ => ((id + column * 17) & 255) as u32,
            };
            input.extend_from_slice(&value.to_be_bytes()[4 - width..]);
        }
    }
    input
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_constructor_defaults_and_zero_columns_are_distinct() {
    let defaults = NATIVE
        .lines()
        .find(|line| line.starts_with("defaults\t"))
        .unwrap();
    let expected: Vec<i32> = defaults
        .split('\t')
        .skip(1)
        .map(|field| field.parse().unwrap())
        .collect();
    assert_eq!(material_fields(&Material::default()).as_slice(), expected);
    assert_eq!(material_fields(&Material::default()), [0; 19]);
    let tables = native_tables();
    let zero = tables["zero"].get_material(0).unwrap();
    assert!(zero.details_only);
    assert_eq!(
        *zero,
        Material {
            details_only: true,
            ..Material::default()
        }
    );
    assert!(tables["empty"].is_empty());
    assert_eq!(tables["empty"].len(), 0);
    assert!(tables["empty"].slots().is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_column_major_sparse_tables_match_every_material_field() {
    let tables = native_tables();
    assert_eq!(tables.len(), 280);
    let mut expected = BTreeMap::new();
    let mut rows = 0;
    for line in NATIVE.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        match fields[0] {
            "table" => {
                assert_eq!(fields.len(), 4, "{line}");
                let table = &tables[fields[1]];
                assert_eq!(table.len(), fields[3].parse::<usize>().unwrap(), "{line}");
                assert_eq!(table.len(), table.slots().len(), "{line}");
                assert_eq!(table.is_empty(), table.slots().is_empty(), "{line}");
            }
            "material" => {
                assert_eq!(fields.len(), 22, "{line}");
                let id: i32 = fields[2].parse().unwrap();
                let actual = material_fields(tables[fields[1]].get_material(id).unwrap());
                let values: Vec<i32> = fields[3..]
                    .iter()
                    .map(|value| value.parse().unwrap())
                    .collect();
                assert_eq!(actual.as_slice(), values, "{line}");
                assert!(expected.insert((fields[1], id as usize), actual).is_none());
                rows += 1;
            }
            _ => {}
        }
    }
    for (name, table) in tables {
        for (id, material) in table.slots().iter().enumerate() {
            assert_eq!(
                material.as_ref().map(material_fields),
                expected.get(&(name, id)).copied(),
                "{name} slot {id}"
            );
        }
    }
    assert_eq!(rows, 538);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_presence_and_boolean_columns_use_exact_equality_not_nonzero() {
    let tables = native_tables();
    let presence = &tables["presence-bytes"];
    assert_eq!(presence.len(), 256);
    assert_eq!(presence.slots().iter().flatten().count(), 1);
    assert!(presence.slots()[1].is_some());
    let mut coverage = [[false; 256]; 19];
    for raw in 0..=255 {
        let name = format!("bytes-{raw}");
        let table = &tables[name.as_str()];
        let value = table.get_material(0).unwrap();
        let fields = material_fields(value);
        for column in [3, 4, 5, 6, 8, 9, 12, 16, 18] {
            coverage[column][(fields[column] & 255) as usize] = true;
        }
        let raw_column = |column: usize| ((raw + column * 17) & 255) as u8;
        assert_eq!(value.details_only, raw_column(0) == 0);
        assert_eq!(value.is_small, raw_column(1) == 1);
        assert_eq!(value.skip_triangles, raw_column(2) == 1);
        assert_eq!(value.a_bool2087, raw_column(10) == 1);
        assert_eq!(value.is_brick_tile, raw_column(11) == 1);
        assert_eq!(value.repeat_s, raw_column(13) == 1);
        assert_eq!(value.repeat_t, raw_column(14) == 1);
        assert_eq!(value.hdr, raw_column(15) == 1);
        assert_eq!(table.slots()[1].is_some(), raw == 1);
        assert!(table.slots()[2].is_none());
        assert!(table.slots()[3].is_some());
    }
    for column in [3, 4, 5, 6, 8, 9, 12, 16, 18] {
        assert!(coverage[column].into_iter().all(|seen| seen));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_lookup_has_no_missing_material_or_out_of_range_fallback() {
    let mut tables = native_tables();
    tables.insert(
        "dense-maximum",
        MaterialTable::decode(&dense_input()).unwrap(),
    );
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("lookup\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 4, "{line}");
        let id = fields[2].parse().unwrap();
        let table = &tables[fields[1]];
        let actual = table.get_material(id);
        match fields[3] {
            "present" => assert!(actual.is_ok(), "{line}"),
            "RuntimeException" => {
                assert_eq!(actual, Err(MaterialLookupError::Missing { id }), "{line}");
            }
            "ArrayIndexOutOfBoundsException" => {
                assert_eq!(
                    actual,
                    Err(MaterialLookupError::OutOfRange {
                        id,
                        size: table.len()
                    }),
                    "{line}"
                );
            }
            other => panic!("unexpected lookup result {other}"),
        }
        count += 1;
    }
    assert_eq!(count, 20);
    for id in [i32::MIN, -1, 0, 1, i32::MAX] {
        assert_eq!(
            tables["empty"].get_material(id),
            Err(MaterialLookupError::OutOfRange { id, size: 0 })
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_opaque_effect_helper_uses_full_integer_comparison() {
    let mut count = 0;
    for line in NATIVE.lines().filter(|line| line.starts_with("opaque\t")) {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 3, "{line}");
        assert_eq!(
            is_opaque_effect(fields[1].parse().unwrap()),
            fields[2] == "1",
            "{line}"
        );
        count += 1;
    }
    assert_eq!(count, 770);
    assert!(is_opaque_effect(257));
    assert!(is_opaque_effect(263));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_truncated_aggregates_fail_explicitly_without_partial_materials() {
    let payloads: BTreeMap<_, _> = NATIVE
        .lines()
        .filter(|line| line.starts_with("table\t"))
        .map(|line| {
            let fields: Vec<_> = line.split('\t').collect();
            (fields[1], hex(fields[2]))
        })
        .collect();
    let mut count = 0;
    for line in NATIVE
        .lines()
        .filter(|line| line.starts_with("truncated\t"))
    {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 4, "{line}");
        assert_eq!(fields[3], "ArrayIndexOutOfBoundsException", "{line}");
        let length: usize = fields[2].parse().unwrap();
        let error = MaterialTable::decode(&payloads[fields[1]][..length]).unwrap_err();
        match error.kind {
            MaterialErrorKind::UnexpectedEnd { needed, remaining } => {
                assert!(remaining < needed, "{line}");
                assert_eq!(error.offset + remaining, length, "{line}");
                assert!([1, 2, 4].contains(&needed), "{line}");
            }
            other => panic!("unexpected truncation error {other:?}: {line}"),
        }
        count += 1;
    }
    assert_eq!(count, 81);
    assert_eq!(
        MaterialTable::decode(&[]),
        Err(MaterialError {
            offset: 0,
            kind: MaterialErrorKind::UnexpectedEnd {
                needed: 2,
                remaining: 0
            }
        })
    );
    assert_eq!(
        MaterialTable::decode(&[0, 1]),
        Err(MaterialError {
            offset: 2,
            kind: MaterialErrorKind::UnexpectedEnd {
                needed: 1,
                remaining: 0
            }
        })
    );
    for (column, width) in WIDTHS.into_iter().enumerate() {
        let mut payload = vec![0, 1, 1];
        payload.resize(3 + WIDTHS[..column].iter().sum::<usize>(), 0);
        for remaining in 0..width {
            let mut truncated = payload.clone();
            truncated.resize(payload.len() + remaining, 0);
            assert_eq!(
                MaterialTable::decode(&truncated).unwrap_err(),
                MaterialError {
                    offset: payload.len(),
                    kind: MaterialErrorKind::UnexpectedEnd {
                        needed: width,
                        remaining
                    }
                }
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn strict_aggregate_boundary_rejects_native_ignored_extensions_and_tagged_data() {
    let mut count = 0;
    for line in NATIVE
        .lines()
        .filter(|line| line.starts_with("native-trailing\t"))
    {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 4, "{line}");
        let input = hex(fields[2]);
        assert_eq!(
            MaterialTable::decode(&input),
            Err(MaterialError {
                offset: input.len() - 2,
                kind: MaterialErrorKind::TrailingData { remaining: 2 }
            }),
            "{line}"
        );
        count += 1;
    }
    assert_eq!(count, 3);
    for byte in 0..=255 {
        assert_eq!(
            MaterialTable::decode(&[0, 0, byte]),
            Err(MaterialError {
                offset: 2,
                kind: MaterialErrorKind::TrailingData { remaining: 1 }
            })
        );
    }
}

fn hash_int(mut hash: u64, value: i32) -> u64 {
    for byte in value.to_le_bytes() {
        hash = (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3);
    }
    hash
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_dense_maximum_covers_all_short_values_and_bounded_full_table() {
    assert_eq!(MAX_MATERIALS, 65_535);
    assert_eq!(BYTES_PER_MATERIAL, WIDTHS.iter().sum::<usize>());
    assert_eq!(MAX_DEFINITION_BYTES, 1_572_842);
    let input = dense_input();
    assert_eq!(input.len(), MAX_DEFINITION_BYTES);
    let table = MaterialTable::decode(&input).unwrap();
    assert_eq!(table.len(), MAX_MATERIALS);
    let row: Vec<_> = NATIVE
        .lines()
        .find(|line| line.starts_with("dense-maximum\t"))
        .unwrap()
        .split('\t')
        .collect();
    assert_eq!(row.len(), 4);
    assert_eq!(row[1].parse::<usize>().unwrap(), input.len());
    assert_eq!(row[2].parse::<usize>().unwrap(), table.len());
    let mut hash = hash_int(0xcbf29ce484222325, table.len() as i32);
    for (id, value) in table.slots().iter().enumerate() {
        let value = value.as_ref().unwrap();
        assert_eq!(value.color_hsl as u16, id as u16);
        for field in material_fields(value) {
            hash = hash_int(hash, field);
        }
    }
    assert_eq!(hash, u64::from_str_radix(row[3], 16).unwrap());
    let mut too_large = input;
    too_large.push(0);
    assert_eq!(
        MaterialTable::decode(&too_large),
        Err(MaterialError {
            offset: 0,
            kind: MaterialErrorKind::InputTooLarge {
                size: MAX_DEFINITION_BYTES + 1,
                limit: MAX_DEFINITION_BYTES
            }
        })
    );
    let mut absent = vec![0xff, 0xff];
    absent.resize(2 + MAX_MATERIALS, 255);
    let table = MaterialTable::decode(&absent).unwrap();
    assert_eq!(table.len(), MAX_MATERIALS);
    assert!(table.slots().iter().all(Option::is_none));
    assert_eq!(
        table.get_material(65_534),
        Err(MaterialLookupError::Missing { id: 65_534 })
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_input_is_deterministic_and_error_messages_are_stable() {
    let mut seed = 727_u32;
    for length in 0..512 {
        let input: Vec<u8> = (0..length)
            .map(|_| {
                seed = seed.wrapping_mul(1_664_525).wrapping_add(1_013_904_223);
                (seed >> 24) as u8
            })
            .collect();
        let first = MaterialTable::decode(&input);
        assert_eq!(first, MaterialTable::decode(&input));
        if let Err(error) = first {
            assert!(error.offset <= input.len());
            assert!(error.to_string().starts_with("material byte "));
        }
    }
    assert_eq!(
        MaterialTable::decode(&[]).unwrap_err().to_string(),
        "material byte 0: expected 2 bytes, only 0 remain"
    );
    assert_eq!(
        MaterialTable::decode(&[0, 0, 7]).unwrap_err().to_string(),
        "material byte 2: 1 trailing bytes"
    );
    assert_eq!(
        MaterialLookupError::Missing { id: 154 }.to_string(),
        "material not found: 154"
    );
    assert_eq!(
        MaterialLookupError::OutOfRange { id: -1, size: 155 }.to_string(),
        "material id -1 outside table of size 155"
    );
}
