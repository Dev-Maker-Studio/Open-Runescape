#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
const STALE_INT: i32 = 0x5a5a5a5a;

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap_or_else(|| panic!("{name}"))
        .opcode
}

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

struct Step {
    name: String,
    operand: i32,
    ints: Vec<i32>,
    result: String,
    out_ints: Vec<i32>,
    hooks: [String; 2],
    out_strings: Vec<String>,
    changes: Vec<(bool, i32, usize)>,
}

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    hooks: Vec<(bool, i32, usize)>,
    subs: Vec<(i32, i32)>,
    names: Vec<(i32, usize, Option<String>, i32)>,
    base: i32,
    screen: Option<(i32, usize)>,
    steps: Vec<Step>,
}

fn parse(text: &str) -> (Vec<World>, usize) {
    let lines: Vec<Vec<&str>> = text
        .lines()
        .map(|line| line.split('\t').collect())
        .collect();
    let mut worlds: Vec<World> = Vec::new();
    let mut total = 0;
    let mut index = 0;
    let mut pending: Vec<(bool, i32, usize)> = Vec::new();
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
                subs: Vec::new(),
                names: Vec::new(),
                base: -1,
                screen: None,
                steps: Vec::new(),
            }),
            "B" => worlds.last_mut().unwrap().base = number(columns[1]),
            "Q" => worlds.last_mut().unwrap().names.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                columns[3]
                    .strip_prefix("hex:")
                    .map(|hex| String::from_utf8(unhex(hex)).unwrap()),
                number(columns[4]),
            )),
            "G" => {
                worlds.last_mut().unwrap().screen =
                    Some((number(columns[1]), columns[2].parse().unwrap()))
            }
            "H" => pending.push((
                columns[1] == "1",
                number(columns[2]),
                columns[3].parse().unwrap(),
            )),
            "I" => {
                let count: usize = columns[3].parse().unwrap();
                let mut rows = Vec::new();
                for offset in 1..=count {
                    let row = &lines[index + offset];
                    rows.push(match row[0] {
                        "C" => {
                            assert_eq!(row.len(), 32, "{row:?}");
                            assert_eq!(row[1].parse::<usize>().unwrap(), offset - 1);
                            Some(state(row))
                        }
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
            "O" => worlds
                .last_mut()
                .unwrap()
                .subs
                .push((number(columns[1]), number(columns[2]))),
            "K" => worlds.last_mut().unwrap().hooks.push((
                columns[1] == "1",
                number(columns[2]),
                columns[3].parse().unwrap(),
            )),
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                assert_eq!(result.len(), 7, "{result:?}");
                index += 1;
                worlds.last_mut().unwrap().steps.push(Step {
                    name: columns[2].to_string(),
                    operand: number(columns[3]),
                    ints: integers(columns[4]),
                    result: result[1].to_string(),
                    out_ints: integers(result[2]),
                    hooks: [result[5].to_string(), result[6].to_string()],
                    out_strings: if result[3] == "-" {
                        Vec::new()
                    } else {
                        result[3]
                            .split(',')
                            .map(|item| {
                                String::from_utf8(unhex(item.strip_prefix("hex:").unwrap())).unwrap()
                            })
                            .collect()
                    },
                    changes: std::mem::take(&mut pending),
                });
            }
            "Z" => total = columns[1].parse().unwrap(),
            other => panic!("{other}"),
        }
        index += 1;
    }
    (worlds, total)
}

fn hook_text(model: &ComponentModel, secondary: bool) -> String {
    match model.hook(secondary) {
        None => "-".to_string(),
        Some(hook) => {
            let component = model.component(hook.component);
            format!(
                "{}:{}:{}",
                hook.interface, component.id_hash, component.slot_id
            )
        }
    }
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    let VmErrorKind::Component(failure) = kind else {
        return false;
    };
    match failure {
        ComponentFailure::NoComponent => {
            expected == "NullPointerException" || expected == "ClassCastException"
        }
        ComponentFailure::Index => {
            expected == "ArrayIndexOutOfBoundsException"
                || expected == "NegativeArraySizeException"
                || expected == "StringIndexOutOfBoundsException"
        }
        ComponentFailure::Interface => expected == "IllegalArgumentException",
        ComponentFailure::Rejected => expected == "RuntimeException",
        ComponentFailure::DivideByZero => expected == "ArithmeticException",
        ComponentFailure::SlotLimit | ComponentFailure::Depth => false,
    }
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
    model.set_base_window(world.base);
    for (id, index, name, triggers) in &world.names {
        model.set_component_name(*id, *index, name.as_deref());
        model.set_server_triggers(*id, *index, *triggers);
    }
    if let Some((id, index)) = world.screen {
        model.set_game_screen(model.interface_component(id, index));
    }
    for (hash, interface) in &world.subs {
        model.set_open_interface(*hash, *interface);
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
    model
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_query_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/components-query.native.tsv"));
    assert_eq!(worlds.len(), 4);
    let mut total = 0;
    let mut successes = 0;
    let mut failures = 0;
    let mut declined = 0;
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for world in &worlds {
        let mut host = ModelHost {
            model: model_for(world),
        };
        for step in &world.steps {
            for (secondary, id, index) in &step.changes {
                let component = host.model.interface_component(*id, *index).unwrap();
                host.model.set_hook(
                    *secondary,
                    Some(ComponentHook {
                        interface: *id,
                        component,
                    }),
                );
            }
            let opcode = opcode_named(&step.name);
            let mut code: Vec<_> = step.ints.iter().map(|n| constant(*n)).collect();
            let pushed = code.len();
            code.extend([int(opcode, step.operand), ret()]);
            let repository = BTreeMap::from([(
                1,
                Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
            )]);
            let mut vm = Vm::default();
            let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
            let label = format!("{}#{total} {} {} {:?}", world.name, step.name, step.operand, step.ints);
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
                    || step.hooks[0] != hook_text(&host.model, false)
                    || step.hooks[1] != hook_text(&host.model, true)
                    || !host.model.take_events().is_empty()
                {
                    mismatches.push(format!("{label}: declined {} vs {result:?}", step.result));
                }
                total += 1;
                continue;
            }
            match (&result, step.result.as_str()) {
                (Ok(_), "ok") => {
                    successes += 1;
                    per_opcode.entry(step.name.clone()).or_default().0 += 1;
                }
                (Err(error), expected) if expected.starts_with("error:") => {
                    failures += 1;
                    per_opcode.entry(step.name.clone()).or_default().1 += 1;
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
            let strings: Vec<String> = vm
                .string_stack()
                .iter()
                .map(|value| value.as_deref().unwrap_or("<null>").to_string())
                .collect();
            let mut expected_strings = step.out_strings.clone();
            if result.is_err() {
                while expected_strings.last().map(String::as_str) == Some("stale") {
                    expected_strings.pop();
                }
            }
            if strings != expected_strings {
                problems.push(format!("strings {strings:?} vs {:?}", step.out_strings));
            }
            if !host.model.take_events().is_empty() {
                problems.push("unexpected events".to_string());
            }
            for (slot, expected) in step.hooks.iter().enumerate() {
                let actual = hook_text(&host.model, slot == 1);
                if &actual != expected {
                    problems.push(format!("hook{slot} {actual} vs {expected}"));
                }
            }
            if !problems.is_empty() {
                mismatches.push(format!("{label}: {}", problems.join("; ")));
            }
            total += 1;
        }
    }
    println!(
        "component query cases={total} ok={successes} failing={failures} declined={declined} mismatches={}",
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(400) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 14000);
    assert_eq!(successes + failures + declined, total);
    assert_eq!(per_opcode.len(), 9);
    for (name, (ok, _)) in &per_opcode {
        assert!(*ok >= 16, "{name}");
    }
}
