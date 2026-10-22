#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
const STALE_INT: i32 = 0x5a5a5a5a;

struct MenuHost {
    model: ComponentModel,
    world: WorldState,
    items: ItemModel,
}

impl VarHost for MenuHost {
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

    fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        Ok(&mut self.world)
    }

    fn items(&mut self) -> Result<&mut ItemModel, HostError> {
        Ok(&mut self.items)
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

#[derive(Default)]
struct Block {
    entries: Vec<[i32; 4]>,
    active: Option<[i32; 4]>,
    containers: Vec<(i32, bool, Vec<i32>)>,
}

struct Step {
    name: String,
    open: bool,
    ints: Vec<i32>,
    block: usize,
    result: String,
    out_ints: Vec<i32>,
}

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    lists: Vec<(i32, usize, Vec<i32>)>,
    blocks: Vec<Block>,
    steps: Vec<Step>,
}

fn four(columns: &[&str]) -> [i32; 4] {
    [
        number(columns[1]),
        number(columns[2]),
        number(columns[3]),
        number(columns[4]),
    ]
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
                lists: Vec::new(),
                blocks: Vec::new(),
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
            "K" => {}
            "L" => worlds.last_mut().unwrap().lists.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                integers(columns[3]),
            )),
            "B" => worlds.last_mut().unwrap().blocks.push(Block::default()),
            "E" => {
                let block = worlds.last_mut().unwrap().blocks.last_mut().unwrap();
                block.entries.push(four(columns));
            }
            "A" => {
                let block = worlds.last_mut().unwrap().blocks.last_mut().unwrap();
                block.active = (columns[1] != "-").then(|| four(columns));
            }
            "G" => {
                let block = worlds.last_mut().unwrap().blocks.last_mut().unwrap();
                block
                    .containers
                    .push((number(columns[1]), columns[2] == "1", integers(columns[3])));
            }
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                index += 1;
                let world = worlds.last_mut().unwrap();
                let block = world.blocks.len() - 1;
                world.steps.push(Step {
                    name: columns[2].to_string(),
                    open: columns[3] == "1",
                    ints: integers(columns[4]),
                    block,
                    result: result[1].to_string(),
                    out_ints: integers(result[2]),
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
    for (id, index, list) in &world.lists {
        let component = model.interface_component(*id, *index).unwrap();
        model.component_mut(component).hooks.set(
            HookSlot::MouseLeaveScript,
            None,
            Some((TriggerSlot::MouseLeave, Some(list.clone()))),
        );
    }
    model
}

fn entry_state(values: &[i32; 4]) -> MiniMenuEntryState {
    MiniMenuEntryState {
        click_type: values[0],
        param0: values[1],
        param1: values[2],
        item_id: values[3],
        ..Default::default()
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_menu_entry_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/menu-entry.native.tsv"));
    assert_eq!(worlds.len(), 4);
    let mut total = 0;
    let mut successes = 0;
    let mut declined = 0;
    let mut positives = 0;
    let mut mismatches = Vec::new();
    for world in &worlds {
        let mut host = MenuHost {
            model: model_for(world),
            world: WorldState::default(),
            items: ItemModel::default(),
        };
        for step in &world.steps {
            let block = &world.blocks[step.block];
            host.world.platform.entries = block.entries.iter().map(entry_state).collect();
            host.world.platform.active_entry = block.active.as_ref().map(entry_state);
            host.world.platform.mini_menu_open = step.open;
            host.items = ItemModel::default();
            for (id, other, items) in &block.containers {
                host.items.containers.insert(
                    i64::from(*id) | if *other { i64::from(i32::MIN) } else { 0 },
                    ItemContainer {
                        amounts: vec![1; items.len()],
                        tooltips: vec![None; items.len()],
                        item_ids: items.clone(),
                    },
                );
            }
            let opcode = opcode_named(&step.name);
            let mut code = push_all(&step.ints, &[]);
            let pushed = code.len();
            code.extend([int(opcode, 0), ret()]);
            let repository = build(&code);
            let mut vm = Vm::default();
            let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
            let label = format!("{}#{total} {}", world.name, step.name);
            let mut problems = Vec::new();
            if matches!(
                &result,
                Err(VmError {
                    kind: VmErrorKind::UnsupportedOpcode(_),
                    ..
                })
            ) {
                declined += 1;
                if step.result != "error:NullPointerException" {
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
            if step.result == "ok" && step.out_ints.iter().any(|v| *v == 1 || *v > 0) {
                positives += 1;
            }
            if !problems.is_empty() {
                mismatches.push(format!("{label}: {}", problems.join("; ")));
            }
            total += 1;
        }
    }
    println!(
        "menu entry cases={total} ok={successes} declined={declined} positives={positives} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 12000);
    assert!(positives > 500);
}
