#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct MapHost {
    world: WorldState,
}

impl VarHost for MapHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        Ok(&mut self.world)
    }
}

fn number<T: std::str::FromStr>(value: &str) -> T
where
    T::Err: std::fmt::Debug,
{
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn unhex(value: &str) -> Vec<u8> {
    if value == "-" {
        return Vec::new();
    }
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn units(text: &str) -> String {
    if text.is_empty() {
        return "-".to_string();
    }
    text.encode_utf16()
        .map(|unit| format!("{unit:x}"))
        .collect::<Vec<_>>()
        .join(".")
}

fn element_digest(element: &WorldMapElement) -> String {
    let placements: Vec<String> = element
        .placements
        .iter()
        .map(|p| {
            format!(
                "{},{},{},{},{},{},{},{},{}",
                p.plane,
                p.start_x,
                p.start_y,
                p.end_x,
                p.end_y,
                p.map_min_x,
                p.map_min_y,
                p.map_max_x,
                p.map_max_y
            )
        })
        .collect();
    format!(
        "{}|{}|{}|{}|{}|{}|{}|{},{},{},{}|{}",
        element.element_id,
        units(&element.filename_prefix),
        units(&element.display_name),
        element.bitpacked_placement,
        element.color,
        i32::from(element.should_render),
        element.default_zoom_level,
        element.bounds_min_x,
        element.bounds_max_x,
        element.bounds_min_y,
        element.bounds_max_y,
        placements.join(";")
    )
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(message)) => match message.as_str() {
            RUNTIME_FAILURE => expected == "RuntimeException",
            NEGATIVE_ARRAY_SIZE => expected == "NegativeArraySizeException",
            _ => expected == "ArrayIndexOutOfBoundsException",
        },
        VmErrorKind::StackUnderflow(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

struct Source {
    id: i32,
    bytes: Vec<u8>,
    expected: String,
}

fn build_defs(members: &[usize], sources: &[Source]) -> WorldMapDefs {
    let mut defs = WorldMapDefs::default();
    for index in members {
        let source = &sources[*index];
        defs.put(WorldMapElement::decode(source.id, &source.bytes).unwrap());
    }
    defs
}

fn categories(text: &str) -> Vec<WorldMapCategory> {
    if text == "-" {
        return Vec::new();
    }
    text.split(';')
        .map(|entry| {
            let values: Vec<i32> = entry.split(':').map(number).collect();
            WorldMapCategory {
                mec_id: values[0],
                abs_x: values[1],
                abs_y: values[2],
            }
        })
        .collect()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_world_map_case_matches_the_actual_kotlin_classes() {
    let text = include_str!("fixtures/world-map.native.tsv");
    let mut sources: Vec<Source> = Vec::new();
    let mut sets: BTreeMap<usize, Vec<usize>> = BTreeMap::new();
    let mut orders: BTreeMap<usize, Vec<usize>> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut totals = (0, 0);
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "E" => {
                assert_eq!(number::<usize>(columns[1]), sources.len());
                sources.push(Source {
                    id: number(columns[2]),
                    bytes: unhex(columns[3]),
                    expected: columns[4].to_string(),
                });
            }
            "S" => {
                let members = if columns[2] == "-" {
                    Vec::new()
                } else {
                    columns[2].split(',').map(number).collect()
                };
                sets.insert(number(columns[1]), members);
            }
            "O" => {
                let order = if columns[2] == "-" {
                    Vec::new()
                } else {
                    columns[2].split(',').map(number).collect()
                };
                orders.insert(number(columns[1]), order);
            }
            "C" => cases.push(columns),
            "Z" => totals = (number::<usize>(columns[1]), number::<usize>(columns[2])),
            other => panic!("{other}"),
        }
    }
    let mut mismatches = Vec::new();
    let mut decoded = (0, 0);
    for (index, source) in sources.iter().enumerate() {
        match (WorldMapElement::decode(source.id, &source.bytes), &source.expected) {
            (Ok(element), expected) if !expected.starts_with("error:") => {
                decoded.0 += 1;
                let actual = element_digest(&element);
                if &actual != expected {
                    mismatches.push(format!("element {index}\n  {actual}\n  {expected}"));
                }
            }
            (Err(_), expected) if expected.starts_with("error:") => decoded.1 += 1,
            (actual, expected) => mismatches.push(format!("element {index}: {expected} vs {actual:?}")),
        }
    }
    for (set, members) in &sets {
        let defs = build_defs(members, &sources);
        let actual: Vec<String> = defs.iter().map(element_digest).collect();
        let expected: Vec<String> = orders[set]
            .iter()
            .map(|index| sources[*index].expected.clone())
            .collect();
        if actual != expected {
            mismatches.push(format!("set {set}: iteration order differs"));
        }
    }
    let defs_by_set: BTreeMap<usize, WorldMapDefs> = sets
        .iter()
        .map(|(set, members)| (*set, build_defs(members, &sources)))
        .collect();
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    for columns in &cases {
        let set: usize = number(columns[1]);
        let static_id: i32 = number(columns[2]);
        let name = columns[9];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let arguments = integers(columns[10]);
        let initial = arguments.len();
        let map_data = WorldMapData {
            defs: defs_by_set[&set].clone(),
            categories: categories(columns[8]),
            ..WorldMapData::default()
        };
        let world_map = WorldMapState {
            static_element: (static_id >= 0).then_some(static_id),
            square_x: number(columns[3]),
            square_y: number(columns[4]),
            center_x: number(columns[5]),
            center_y: number(columns[6]),
            loading_percentage: number(columns[7]),
            ..WorldMapState::default()
        };
        let mut host = MapHost {
            world: WorldState {
                world_map,
                map_data: map_data.clone(),
                ..Default::default()
            },
        };
        let mut code: Vec<_> = arguments.iter().map(|value| constant(*value)).collect();
        code.push(int(opcode, 0));
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("set {set} {name} {} static {static_id}", columns[10]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[11]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(columns[12]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!("{label}: ints {:?} vs {expected:?}", vm.int_stack()));
                }
                let strings: Vec<String> = vm
                    .string_stack()
                    .iter()
                    .map(|value| units(value.as_deref().unwrap_or("")))
                    .collect();
                let actual = if strings.is_empty() {
                    "-".to_string()
                } else {
                    strings.join("|")
                };
                if actual != columns[13] {
                    mismatches.push(format!("{label}: strings {actual} vs {}", columns[13]));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
        if host.world.map_data != map_data {
            mismatches.push(format!("{label}: map data changed"));
        }
    }
    println!(
        "world map elements={} decoded={} rejected={} cases={} mismatches={}",
        sources.len(),
        decoded.0,
        decoded.1,
        cases.len(),
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(8) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), totals.0);
    assert_eq!(sources.len(), totals.1);
    assert_eq!(per_opcode.len(), 10);
    assert!(decoded.0 > 100 && decoded.1 > 20);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_map_opcodes_are_unsupported_without_a_host_state() {
    struct Bare;
    impl VarHost for Bare {
        fn varp(&self, _: i32) -> Result<i32, HostError> {
            Ok(0)
        }
        fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
            Ok(())
        }
        fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
            Ok(VarbitLookup::Absent)
        }
    }
    for opcode in [
        Opcode::WORLDMAP_GETMAP,
        Opcode::WORLDMAP_GETSOURCEPOSITION,
        Opcode::WORLDMAP_FINDNEARESTELEMENT,
    ] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&[int(opcode, 0), ret()], counts(0, 1, 0), zero(), &[]))
                .unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn the_decoder_rejects_every_prefix_of_a_valid_element() {
    let text = include_str!("fixtures/world-map.native.tsv");
    let mut checked = 0;
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        if columns[0] != "E" || columns[4].starts_with("error:") {
            continue;
        }
        let bytes = unhex(columns[3]);
        let id: i32 = number(columns[2]);
        let full = WorldMapElement::decode(id, &bytes).unwrap();
        let used = {
            let mut length = 0;
            for _ in 0..2 {
                while bytes[length] != 0 {
                    length += 1;
                }
                length += 1;
            }
            length + 4 + 4 + 4 + full.placements.len() * 17
        };
        for cut in 0..used.min(bytes.len()) {
            assert!(WorldMapElement::decode(id, &bytes[..cut]).is_err());
            checked += 1;
        }
        assert!(WorldMapElement::decode(id, &bytes[..used.min(bytes.len())]).is_ok());
    }
    assert!(checked > 1000);
}
