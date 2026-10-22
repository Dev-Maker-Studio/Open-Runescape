use crate::common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

pub const FIXTURE: &str = include_str!("../fixtures/bounds.native.tsv");

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

fn column(value: &str) -> Option<Vec<i32>> {
    match value {
        "n" => None,
        "e" => Some(Vec::new()),
        other => Some(other.split(',').map(number).collect()),
    }
}

fn column_text(value: &Option<Vec<i32>>) -> String {
    match value {
        None => "n".to_string(),
        Some(values) if values.is_empty() => "e".to_string(),
        Some(values) => values
            .iter()
            .map(i32::to_string)
            .collect::<Vec<_>>()
            .join(","),
    }
}

fn parse_boxes(text: &str) -> Option<BoxSet> {
    let rest = text.strip_prefix("B|").unwrap();
    if rest == "n" {
        return None;
    }
    let rest = rest.strip_prefix("y|").unwrap();
    let polygons = match rest {
        "n" => None,
        "e" => Some(Vec::new()),
        list => Some(
            list.split(';')
                .map(|item| {
                    if item == "x" {
                        return None;
                    }
                    let v = integers(item);
                    Some(BoundingBox {
                        calculated: v[0] == 1,
                        left: v[1],
                        right: v[2],
                        top: v[3],
                        bottom: v[4],
                        size: v[5],
                    })
                })
                .collect(),
        ),
    };
    Some(BoxSet { polygons })
}

fn boxes_text(set: &Option<BoxSet>) -> String {
    let Some(set) = set else {
        return "B|n".to_string();
    };
    match &set.polygons {
        None => "B|y|n".to_string(),
        Some(list) if list.is_empty() => "B|y|e".to_string(),
        Some(list) => {
            let items: Vec<String> = list
                .iter()
                .map(|item| match item {
                    None => "x".to_string(),
                    Some(b) => format!(
                        "{},{},{},{},{},{}",
                        i32::from(b.calculated),
                        b.left,
                        b.right,
                        b.top,
                        b.bottom,
                        b.size
                    ),
                })
                .collect();
            format!("B|y|{}", items.join(";"))
        }
    }
}

fn parse_height(text: &str) -> Option<LocHeight> {
    let rest = text.strip_prefix("H|").unwrap();
    if rest == "n" {
        return None;
    }
    if let Some(value) = rest.strip_prefix("a|") {
        return Some(LocHeight::Animated(number(value)));
    }
    let model = rest.strip_prefix("m|").unwrap();
    if model == "n" {
        return Some(LocHeight::Model(None));
    }
    let parts: Vec<&str> = model.split(':').collect();
    Some(LocHeight::Model(Some(ModelBounds {
        bounds_updated: parts[0] == "1",
        min_y: number(parts[1]),
        vertex_count: number(parts[2]),
        vertices_x: column(parts[3]),
        vertices_y: column(parts[4]),
        vertices_z: column(parts[5]),
    })))
}

fn height_text(height: &Option<LocHeight>) -> String {
    match height {
        None => "H|n".to_string(),
        Some(LocHeight::Animated(value)) => format!("H|a|{value}"),
        Some(LocHeight::Model(None)) => "H|m|n".to_string(),
        Some(LocHeight::Model(Some(model))) => format!(
            "H|m|{}:{}:{}:{}:{}:{}",
            i32::from(model.bounds_updated),
            model.min_y,
            model.vertex_count,
            column_text(&model.vertices_x),
            column_text(&model.vertices_y),
            column_text(&model.vertices_z)
        ),
    }
}

fn parse(name: &str, text: &str) -> WorldState {
    let mut world = WorldState::default();
    let bounds = &mut world.projection.bounds;
    match name {
        "GET_ENTITY_BOUNDING_BOX" => bounds.entity = parse_boxes(text),
        "GET_LOC_BOUNDING_BOX" => bounds.loc = parse_boxes(text),
        "GET_OBJ_BOUNDING_BOX" => bounds.obj = parse_boxes(text),
        _ => bounds.loc_height = parse_height(text),
    }
    world
}

fn digest(name: &str, world: &WorldState) -> String {
    let bounds = &world.projection.bounds;
    match name {
        "GET_ENTITY_BOUNDING_BOX" => boxes_text(&bounds.entity),
        "GET_LOC_BOUNDING_BOX" => boxes_text(&bounds.loc),
        "GET_OBJ_BOUNDING_BOX" => boxes_text(&bounds.obj),
        _ => height_text(&bounds.loc_height),
    }
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(_)) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

pub fn replay(
    mut run: impl FnMut(&mut Vm, &BTreeMap<i32, Script>, &mut WorldState) -> Result<Execution, VmError>,
) {
    let mut cases = Vec::new();
    let mut total = 0;
    for line in FIXTURE.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "C" => cases.push(columns),
            "Z" => total = number::<usize>(columns[1]),
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for columns in &cases {
        let name = columns[1];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let mut world = parse(name, columns[3]);
        if digest(name, &world) != columns[3] {
            mismatches.push(format!(
                "{name}: state round trip differs from {}",
                columns[3]
            ));
            continue;
        }
        let code = vec![int(opcode, 0), ret()];
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = run(&mut vm, &repository, &mut world);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(columns[5]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!(
                        "{name} from {}: ints {:?}",
                        columns[3],
                        vm.int_stack()
                    ));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != 0
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!(
                        "{name} from {}: error {expected} vs {error:?}",
                        columns[3]
                    ));
                }
            }
            (actual, expected) => mismatches.push(format!(
                "{name} from {}: {expected} vs {actual:?}",
                columns[3]
            )),
        }
        let after = digest(name, &world);
        if after != columns[6] {
            mismatches.push(format!("{name}: state\n  {after}\n  {}", columns[6]));
        }
    }
    println!(
        "bounds cases={} mismatches={}",
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
    assert_eq!(cases.len(), total);
    assert_eq!(per_opcode.len(), 4);
}
