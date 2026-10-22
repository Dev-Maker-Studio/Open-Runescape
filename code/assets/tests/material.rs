use openrs_assets::{is_opaque_effect, parse_material_table, Error, MaterialType};

const PAYLOAD: &[u8] = include_bytes!("fixtures/materials.payload");
const EXPECTED: &str = include_str!("fixtures/materials.expected");

fn flag(value: &str) -> bool {
    value == "1"
}

fn expected_material(fields: &str) -> Option<MaterialType> {
    if fields == "-" {
        return None;
    }
    let v: Vec<&str> = fields.split(' ').collect();
    assert_eq!(v.len(), 19);
    Some(MaterialType {
        details_only: flag(v[0]),
        is_small: flag(v[1]),
        skip_triangles: flag(v[2]),
        brightness: v[3].parse().unwrap(),
        alpha: v[4].parse().unwrap(),
        effect_id: v[5].parse().unwrap(),
        effect_param1: v[6].parse().unwrap(),
        color_hsl: v[7].parse().unwrap(),
        texture_speed_u: v[8].parse().unwrap(),
        texture_speed_v: v[9].parse().unwrap(),
        a_bool2087: flag(v[10]),
        is_brick_tile: flag(v[11]),
        mipmapping: v[12].parse().unwrap(),
        repeat_s: flag(v[13]),
        repeat_t: flag(v[14]),
        hdr: flag(v[15]),
        combine_mode: v[16].parse().unwrap(),
        effect_param2: v[17].parse().unwrap(),
        effect_combiner: v[18].parse().unwrap(),
    })
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_material_matches_the_kotlin_oracle() {
    let table = parse_material_table(PAYLOAD).unwrap();
    let mut lines = EXPECTED.lines();
    let count: usize = lines
        .next()
        .unwrap()
        .strip_prefix("count\t")
        .unwrap()
        .parse()
        .unwrap();
    assert_eq!(table.len(), count);
    assert_eq!(count, 2594);
    let mut present = 0;
    for (index, line) in lines.enumerate() {
        let (id, fields) = line.split_once('\t').unwrap();
        assert_eq!(id.parse::<usize>().unwrap(), index);
        let expected = expected_material(fields);
        assert_eq!(table.materials[index], expected, "material {index}");
        present += usize::from(expected.is_some());
    }
    assert_eq!(table.materials.len(), count);
    assert!(present > 2000);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn lookup_and_effect_helpers() {
    let table = parse_material_table(PAYLOAD).unwrap();
    assert!(table.get(table.len()).is_none());
    assert!(!table.is_empty());
    assert_eq!(table.get(0).unwrap().color_hsl, 6547);
    assert!(is_opaque_effect(0));
    assert!(!is_opaque_effect(1));
    assert!(!is_opaque_effect(7));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn rejects_truncated_and_trailing_payloads() {
    assert_eq!(parse_material_table(&[]), Err(Error::Truncated));
    assert_eq!(
        parse_material_table(&[0xff, 0xff, 1]),
        Err(Error::Truncated)
    );
    for cut in [1, 2, 100, PAYLOAD.len() / 2, PAYLOAD.len() - 1] {
        assert_eq!(
            parse_material_table(&PAYLOAD[..PAYLOAD.len() - cut]),
            Err(Error::Truncated),
            "cut {cut}"
        );
    }
    let mut extended = PAYLOAD.to_vec();
    extended.push(0);
    assert_eq!(parse_material_table(&extended), Err(Error::TrailingData));
    assert_eq!(parse_material_table(&[0, 0]).unwrap().len(), 0);
}
