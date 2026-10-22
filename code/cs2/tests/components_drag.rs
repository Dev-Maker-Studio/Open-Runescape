#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
const STALE_INT: i32 = 0x5a5a5a5a;

struct ModelHost {
    model: ComponentModel,
}

impl VarHost for ModelHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn components(&mut self) -> Result<&mut ComponentModel, HostError> {
        Ok(&mut self.model)
    }
}

fn unhex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn state(columns: &[&str]) -> ComponentState {
    let mut p = 2;
    let mut next = || {
        p += 1;
        columns[p - 1]
    };
    let type_id = number(next());
    let mut state = ComponentState::default();
    state.component_type = if type_id < 0 {
        None
    } else {
        Some(type_id as u8)
    };
    state.id_hash = number(next());
    state.parent_id = number(next());
    state.x = number(next());
    state.y = number(next());
    state.width = number(next());
    state.height = number(next());
    state.base_position_x = number(next());
    state.base_position_y = number(next());
    state.base_width = number(next());
    state.base_height = number(next());
    state.aspect_x_type = number(next()) as i8;
    state.aspect_y_type = number(next()) as i8;
    state.aspect_width_type = number(next()) as i8;
    state.aspect_height_type = number(next()) as i8;
    state.aspect_width = number(next());
    state.aspect_height = number(next());
    state.aspect_x = number(next());
    state.aspect_y = number(next());
    state.hidden = number(next()) != 0;
    state.scroll_x = number(next());
    state.scroll_y = number(next());
    state.scroll_width = number(next());
    state.scroll_height = number(next());
    state.sprite_id = number(next());
    state.text = Arc::from(String::from_utf8(unhex(next())).unwrap());
    state.color = number(next());
    state.transparency = number(next());
    state.rendered = next() == "1";
    state
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    let VmErrorKind::Component(failure) = kind else {
        return false;
    };
    match failure {
        ComponentFailure::NoComponent => expected == "NullPointerException",
        ComponentFailure::Index => {
            expected == "ArrayIndexOutOfBoundsException" || expected == "NegativeArraySizeException"
        }
        ComponentFailure::Interface => expected == "IllegalArgumentException",
        ComponentFailure::Rejected => expected == "RuntimeException",
        ComponentFailure::DivideByZero => expected == "ArithmeticException",
        ComponentFailure::SlotLimit | ComponentFailure::Depth => false,
    }
}

fn push_all(ints: &[i32], strings: &[Option<String>]) -> Vec<(Opcode, Operand)> {
    let mut code = Vec::new();
    for value in strings {
        match value {
            Some(value) => code.push(text(value)),
            None => code.push(int(Opcode::PUSH_STRING_LOCAL, 0)),
        }
    }
    code.extend(ints.iter().map(|n| constant(*n)));
    code
}

fn build(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )])
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap_or_else(|| panic!("{name}"))
        .opcode
}

struct Step {
    name: String,
    operand: i32,
    ints: Vec<i32>,
    before: Vec<String>,
    result: String,
    out_ints: Vec<i32>,
    after: Vec<String>,
}

type SettingRow = (i32, usize, Option<i32>, Option<(i32, i32)>);

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    hooks: Vec<(bool, i32, usize)>,
    settings: Vec<SettingRow>,
    overrides: Vec<(i32, i32, i32)>,
    opens: Vec<(i32, i32)>,
    steps: Vec<Step>,
}

fn parse(text: &str) -> (Vec<World>, usize) {
    let lines: Vec<Vec<&str>> = text.lines().map(|line| line.split('\t').collect()).collect();
    let mut worlds: Vec<World> = Vec::new();
    let mut total = 0;
    let mut index = 0;
    while index < lines.len() {
        let columns = &lines[index];
        match columns[0] {
            "W" => worlds.push(World {
                name: columns[1].to_string(),
                capacity: number(columns[2]),
                width: number(columns[3]),
                height: number(columns[4]),
                interfaces: Vec::new(),
                hooks: Vec::new(),
                settings: Vec::new(),
                overrides: Vec::new(),
                opens: Vec::new(),
                steps: Vec::new(),
            }),
            "I" => {
                let count: usize = columns[3].parse().unwrap();
                let mut rows = Vec::new();
                for offset in 1..=count {
                    let row = &lines[index + offset];
                    rows.push(match row[0] {
                        "C" => Some(state(row)),
                        "N" => None,
                        other => panic!("{other}"),
                    });
                }
                index += count;
                worlds.last_mut().unwrap().interfaces.push((
                    number(columns[1]),
                    columns[2] == "1",
                    rows,
                ));
            }
            "K" => worlds.last_mut().unwrap().hooks.push((
                columns[1] == "1",
                number(columns[2]),
                columns[3].parse().unwrap(),
            )),
            "S" => worlds.last_mut().unwrap().settings.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                (columns[3] != "-").then(|| number(columns[3])),
                (columns[5] != "-").then(|| {
                    let parts: Vec<&str> = columns[5].split(':').collect();
                    (number(parts[0]), number(parts[1]))
                }),
            )),
            "O" => worlds.last_mut().unwrap().overrides.push((
                number(columns[1]),
                number(columns[2]),
                number(columns[3]),
            )),
            "V" => worlds
                .last_mut()
                .unwrap()
                .opens
                .push((number(columns[1]), number(columns[2]))),
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                index += 1;
                worlds.last_mut().unwrap().steps.push(Step {
                    name: columns[2].to_string(),
                    operand: number(columns[3]),
                    ints: integers(columns[4]),
                    before: columns[5..].iter().map(|v| v.to_string()).collect(),
                    result: result[1].to_string(),
                    out_ints: integers(result[2]),
                    after: result[3..].iter().map(|v| v.to_string()).collect(),
                });
            }
            "Z" => total = columns[1].parse().unwrap(),
            other => panic!("{other}"),
        }
        index += 1;
    }
    (worlds, total)
}

fn model_for(world: &World) -> ComponentModel {
    let mut inverse: i32 = CANVAS_HEIGHT_FACTOR;
    for _ in 0..6 {
        inverse =
            inverse.wrapping_mul(2i32.wrapping_sub(CANVAS_HEIGHT_FACTOR.wrapping_mul(inverse)));
    }
    let mut model = ComponentModel::new(
        world.capacity,
        world.width,
        world.height.wrapping_mul(inverse),
    );
    for (id, prevent, components) in &world.interfaces {
        model
            .add_interface(*id, *prevent, components.clone())
            .unwrap();
    }
    for (secondary, id, index) in &world.hooks {
        let component = model.interface_component(*id, *index).unwrap();
        model.set_hook(
            *secondary,
            Some(ComponentHook {
                interface: *id,
                component,
            }),
        );
    }
    let mut targets = BTreeMap::new();
    for (id, index, settings, over) in &world.settings {
        let component = model.interface_component(*id, *index).unwrap();
        model.set_target_settings(
            component,
            settings.map(|hash| ComponentSettings {
                hash,
                interface_id: 0,
            }),
        );
        if let Some((hash, slot)) = over {
            targets.insert(component, (*hash, *slot));
        }
    }
    let index = identities(&model, world);
    for (component, key) in targets {
        let target = index[&format!("{}:{}", key.0, key.1)];
        model.component_mut(component).depth_child = Some(target);
    }
    let mut seen = std::collections::BTreeSet::new();
    for (hash, slot, settings) in &world.overrides {
        if seen.insert((*hash, *slot)) {
            model.set_slot_settings(
                *hash,
                *slot,
                Some(ComponentSettings {
                    hash: *settings,
                    interface_id: 0,
                }),
            );
        }
    }
    for (parent, interface) in &world.opens {
        model.set_open_interface_with_overlay(*parent, *interface, 3);
    }
    model
}

fn identities(model: &ComponentModel, world: &World) -> BTreeMap<String, ComponentId> {
    let mut map = BTreeMap::new();
    for (id, _, components) in &world.interfaces {
        for index in 0..components.len() {
            let Some(component) = model.interface_component(*id, index) else {
                continue;
            };
            let state = model.component(component);
            map.insert(format!("{}:{}", state.id_hash, state.slot_id), component);
            if let Some(children) = model.slot_children(component) {
                for child in children.iter().flatten() {
                    let state = model.component(*child);
                    map.insert(format!("{}:{}", state.id_hash, state.slot_id), *child);
                }
            }
        }
    }
    map
}

fn key_of(model: &ComponentModel, id: Option<ComponentId>) -> String {
    match id {
        None => "-".to_string(),
        Some(id) => {
            let state = model.component(id);
            format!("{}:{}", state.id_hash, state.slot_id)
        }
    }
}

fn state_text(model: &ComponentModel) -> Vec<String> {
    let drag = model.drag_state();
    vec![
        key_of(model, drag.dragging),
        match drag.selected {
            DragTarget::None => "-".to_string(),
            DragTarget::Frame => "F".to_string(),
            DragTarget::Component(id) => key_of(model, Some(id)),
        },
        drag.offset_x.to_string(),
        drag.offset_y.to_string(),
        i32::from(drag.threshold_met).to_string(),
        drag.ticks.to_string(),
        i32::from(drag.mini_menu_open).to_string(),
    ]
}

fn seed_state(
    model: &mut ComponentModel,
    index: &BTreeMap<String, ComponentId>,
    values: &[String],
) {
    let find = |value: &str| (value != "-").then(|| index[value]);
    model.set_drag_state(DragState {
        dragging: find(&values[0]),
        selected: match values[1].as_str() {
            "-" => DragTarget::None,
            "F" => DragTarget::Frame,
            other => DragTarget::Component(index[other]),
        },
        offset_x: number(&values[2]),
        offset_y: number(&values[3]),
        threshold_met: values[4] == "1",
        ticks: number(&values[5]),
        mini_menu_open: values[6] == "1",
    });
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_drag_pickup_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/components-drag.native.tsv"));
    assert_eq!(worlds.len(), 4);
    let mut total = 0;
    let mut successes = 0;
    let mut declined = 0;
    let mut kinds = BTreeMap::new();
    let mut mismatches = Vec::new();
    for world in &worlds {
        let mut host = ModelHost {
            model: model_for(world),
        };
        let index = identities(&host.model, world);
        for step in &world.steps {
            let opcode = opcode_named(&step.name);
            let mut code = push_all(&step.ints, &[]);
            let pushed = code.len();
            code.extend([int(opcode, step.operand), ret()]);
            let repository = build(&code);
            seed_state(&mut host.model, &index, &step.before);
            let mut vm = Vm::default();
            let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
            let label = format!("{}#{total} {} {}", world.name, step.name, step.operand);
            let mut problems = Vec::new();
            if matches!(
                &result,
                Err(VmError {
                    kind: VmErrorKind::UnsupportedOpcode(_),
                    ..
                })
            ) {
                declined += 1;
                if step.result != "error:NullPointerException"
                    || state_text(&host.model) != step.after
                {
                    mismatches.push(format!("{label}: declined {} vs {result:?}", step.result));
                }
                total += 1;
                continue;
            }
            match (&result, step.result.as_str()) {
                (Ok(_), "ok") => successes += 1,
                (Err(error), expected) if expected.starts_with("error:") => {
                    if error.instruction != pushed
                        || !class_matches(&expected["error:".len()..], &error.kind)
                    {
                        problems.push(format!("error {expected} vs {error:?}"));
                    }
                }
                (actual, expected) => problems.push(format!("result {expected} vs {actual:?}")),
            }
            let mut expected_ints = step.out_ints.clone();
            if result.is_err() {
                while expected_ints.last() == Some(&STALE_INT) {
                    expected_ints.pop();
                }
            }
            if vm.int_stack() != expected_ints.as_slice() {
                problems.push(format!("ints {:?} vs {:?}", vm.int_stack(), step.out_ints));
            }
            let actual = state_text(&host.model);
            if actual != step.after {
                problems.push(format!("state {actual:?} vs {:?}", step.after));
            }
            if step.result == "ok" {
                let changed = actual != step.before;
                *kinds
                    .entry((changed, step.after[1].clone() == "F", step.after[1] == "-"))
                    .or_insert(0) += 1;
            }
            if !problems.is_empty() {
                mismatches.push(format!("{label}: {}", problems.join("; ")));
            }
            total += 1;
        }
    }
    println!("drag cases={total} ok={successes} declined={declined} kinds={kinds:?} mismatches={}", mismatches.len());
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 12000);
    assert!(kinds.keys().any(|(_, frame, _)| *frame));
    assert!(kinds.keys().any(|(changed, _, _)| *changed));
}
