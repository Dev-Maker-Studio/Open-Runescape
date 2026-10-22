#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
const STALE_INT: i32 = 0x5a5a5a5a;
const STALE_TEXT: &str = "stale";

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

fn hex(value: &str) -> String {
    value.bytes().map(|byte| format!("{byte:02x}")).collect()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value
            .split(',')
            .map(|item| {
                if item == "null" {
                    None
                } else {
                    Some(String::from_utf8(unhex(item.strip_prefix("hex:").unwrap())).unwrap())
                }
            })
            .collect()
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
    strings: Vec<Option<String>>,
    result: String,
    out_ints: Vec<i32>,
    out_strings: Vec<Option<String>>,
    events: String,
    hooks: [String; 2],
    deltas: BTreeMap<String, String>,
    hook_changes: Vec<(bool, Option<(i32, usize)>)>,
}

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    hooks: Vec<(bool, i32, usize)>,
    params: Vec<(i32, i32, bool, Option<String>)>,
    settings: Vec<(i32, usize, i32)>,
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
    let mut pending: Vec<(bool, Option<(i32, usize)>)> = Vec::new();
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
                params: Vec::new(),
                settings: Vec::new(),
                steps: Vec::new(),
            }),
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
            "K" => worlds.last_mut().unwrap().hooks.push((
                columns[1] == "1",
                number(columns[2]),
                columns[3].parse().unwrap(),
            )),
            "P" => worlds.last_mut().unwrap().params.push((
                number(columns[1]),
                number(columns[2]),
                columns[3] == "s",
                strings(columns[4]).pop().unwrap(),
            )),
            "S" => worlds.last_mut().unwrap().settings.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                number(columns[3]),
            )),
            "H" => {
                let id = number(columns[2]);
                pending.push((
                    columns[1] == "1",
                    if id < 0 {
                        None
                    } else {
                        Some((id, columns[3].parse().unwrap()))
                    },
                ));
            }
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                assert_eq!(result.len(), 7, "{result:?}");
                index += 1;
                let mut deltas = BTreeMap::new();
                while index + 1 < lines.len() && lines[index + 1][0] == "D" {
                    index += 1;
                    deltas.insert(lines[index][1].to_string(), lines[index][2..].join("\t"));
                }
                worlds.last_mut().unwrap().steps.push(Step {
                    name: columns[2].to_string(),
                    operand: number(columns[3]),
                    ints: integers(columns[4]),
                    strings: strings(columns[5]),
                    result: result[1].to_string(),
                    out_ints: integers(result[2]),
                    out_strings: strings(result[3]),
                    events: result[4].to_string(),
                    hooks: [result[5].to_string(), result[6].to_string()],
                    deltas,
                    hook_changes: std::mem::take(&mut pending),
                });
            }
            "Z" => total = columns[1].parse().unwrap(),
            other => panic!("{other}"),
        }
        index += 1;
    }
    (worlds, total)
}

fn describe(model: &ComponentModel, id: ComponentId) -> String {
    let c = &model.component(id).options;
    let optional = |value: &Option<Arc<str>>| match value {
        None => "null".to_string(),
        Some(value) => format!("hex:{}", hex(value)),
    };
    let bytes = |value: &Option<Vec<i8>>| match value {
        None => "null".to_string(),
        Some(value) if value.is_empty() => "empty".to_string(),
        Some(value) => value
            .iter()
            .map(|byte| byte.to_string())
            .collect::<Vec<_>>()
            .join("/"),
    };
    let mut fields = Vec::new();
    fields.push(match &c.options {
        None => "null".to_string(),
        Some(options) => format!(
            "{}[{}]",
            options.len(),
            options.iter().map(&optional).collect::<Vec<_>>().join(",")
        ),
    });
    fields.push(optional(&c.op_base));
    fields.push(match &c.op_cursors {
        None => "null".to_string(),
        Some(cursors) => cursors
            .iter()
            .map(|cursor| cursor.to_string())
            .collect::<Vec<_>>()
            .join(","),
    });
    fields.push(c.target_over_cursor.to_string());
    fields.push(c.target_leave_cursor.to_string());
    fields.push(c.mouse_over_cursor.to_string());
    fields.push(c.target_op_cursor.to_string());
    match &c.keys {
        None => fields.extend(["null".to_string(), "null".to_string(), "null".to_string()]),
        Some(keys) => {
            fields.push(
                keys.char_codes
                    .iter()
                    .map(|code| code.to_string())
                    .collect::<Vec<_>>()
                    .join(","),
            );
            fields.push(
                keys.actions
                    .iter()
                    .map(&bytes)
                    .collect::<Vec<_>>()
                    .join(","),
            );
            fields.push(
                keys.modifiers
                    .iter()
                    .map(&bytes)
                    .collect::<Vec<_>>()
                    .join(","),
            );
        }
    }
    fields.push(i32::from(c.has_interaction).to_string());
    fields.push(c.params.len().to_string());
    let default: Arc<str> = Arc::from("d");
    fields.push(
        (1..=9)
            .map(|param| {
                format!(
                    "{}/hex:{}",
                    c.int_param(param, 12345),
                    hex(&c.string_param(param, &default))
                )
            })
            .collect::<Vec<_>>()
            .join(","),
    );
    fields.push(c.target_settings.to_string());
    fields.join("\t")
}

fn dump(world: &World, model: &ComponentModel) -> BTreeMap<String, String> {
    let mut out = BTreeMap::new();
    for (id, _, components) in &world.interfaces {
        for index in 0..components.len() {
            let Some(component) = model.interface_component(*id, index) else {
                continue;
            };
            out.insert(format!("i{id}.{index}"), describe(model, component));
        }
    }
    out
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

fn event_text(events: &[ComponentEvent]) -> String {
    if events.is_empty() {
        return "-".to_string();
    }
    events
        .iter()
        .map(|event| {
            let kind = match event.kind {
                ComponentEventKind::Text => "text",
                ComponentEventKind::Color => "color",
                ComponentEventKind::Hide => "hide",
                ComponentEventKind::Position => "position",
                ComponentEventKind::Graphic => "graphic",
                ComponentEventKind::Angle => "angle",
                ComponentEventKind::ScrollPosition => "scrollpos",
                ComponentEventKind::Font => "font",
                ComponentEventKind::Monospaced => "monospaced",
                ComponentEventKind::ClickMask => "clickmask",
                ComponentEventKind::TextAntiMacro => "antimacro",
                ComponentEventKind::Offset => "offset",
                ComponentEventKind::Content => "content",
                ComponentEventKind::Item => "item",
                ComponentEventKind::Anim => "anim",
                ComponentEventKind::Video => "video",
                ComponentEventKind::Recolor(_) | ComponentEventKind::Retexture(_) => "layer",
            };
            format!("{kind}:{}", event.hash)
        })
        .collect::<Vec<_>>()
        .join(",")
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
    for (id, default_int, text, name) in &world.params {
        model.add_param_type(
            *id,
            if *text {
                ParamType::string(name.as_deref().map(Arc::from))
            } else {
                ParamType::int(*default_int)
            },
        );
    }
    for (id, index, settings) in &world.settings {
        let component = model.interface_component(*id, *index).unwrap();
        model.component_mut(component).options.target_settings = *settings;
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
fn every_option_and_param_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/components-options.native.tsv"));
    assert_eq!(worlds.len(), 2);
    let mut total = 0;
    let mut successes = 0;
    let mut failures = 0;
    let mut deltas = 0;
    let mut declined = 0;
    let mut mismatches = Vec::new();
    for world in &worlds {
        let mut host = ModelHost {
            model: model_for(world),
        };
        let mut baseline = dump(world, &host.model);
        for step in &world.steps {
            for (secondary, change) in &step.hook_changes {
                let hook = change.map(|(id, index)| ComponentHook {
                    interface: id,
                    component: host.model.interface_component(id, index).unwrap(),
                });
                host.model.set_hook(*secondary, hook);
            }
            let opcode = opcode_named(&step.name);
            let mut code = push_all(&step.ints, &step.strings);
            let pushed = code.len();
            code.extend([int(opcode, step.operand), ret()]);
            let repository = build(&code);
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
                    || !step.deltas.is_empty()
                    || step.events != "-"
                    || step.hooks[0] != hook_text(&host.model, false)
                    || step.hooks[1] != hook_text(&host.model, true)
                    || !host.model.take_events().is_empty()
                    || dump(world, &host.model) != baseline
                {
                    mismatches.push(format!("{label}: declined {} vs {result:?}", step.result));
                }
                total += 1;
                continue;
            }
            match (&result, step.result.as_str()) {
                (Ok(_), "ok") => successes += 1,
                (Err(error), expected) if expected.starts_with("error:") => {
                    failures += 1;
                    if error.instruction != pushed
                        || !class_matches(&expected["error:".len()..], &error.kind)
                    {
                        problems.push(format!("error {expected} vs {error:?}"));
                    }
                }
                (actual, expected) => problems.push(format!("result {expected} vs {actual:?}")),
            }
            let actual_strings: Vec<Option<String>> = vm
                .string_stack()
                .iter()
                .map(|value| value.as_ref().map(|value| value.to_string()))
                .collect();
            let mut expected_ints = step.out_ints.clone();
            let mut expected_strings = step.out_strings.clone();
            if result.is_err() {
                while expected_ints.last() == Some(&STALE_INT) {
                    expected_ints.pop();
                }
                while expected_strings.last() == Some(&Some(STALE_TEXT.to_string())) {
                    expected_strings.pop();
                }
            }
            if vm.int_stack() != expected_ints.as_slice() {
                problems.push(format!("ints {:?} vs {:?}", vm.int_stack(), step.out_ints));
            }
            if actual_strings != expected_strings {
                problems.push(format!(
                    "strings {actual_strings:?} vs {:?}",
                    step.out_strings
                ));
            }
            let events = event_text(&host.model.take_events());
            if events != step.events {
                problems.push(format!("events {events} vs {}", step.events));
            }
            for (slot, expected) in step.hooks.iter().enumerate() {
                let actual = hook_text(&host.model, slot == 1);
                if &actual != expected {
                    problems.push(format!("hook{slot} {actual} vs {expected}"));
                }
            }
            let current = dump(world, &host.model);
            let mut actual_deltas = BTreeMap::new();
            for key in current.keys().chain(baseline.keys()) {
                let now = current.get(key);
                if now != baseline.get(key) {
                    actual_deltas.insert(key.clone(), now.cloned().unwrap_or("-".to_string()));
                }
            }
            if actual_deltas != step.deltas {
                let differing: Vec<_> = actual_deltas
                    .iter()
                    .filter(|(key, value)| step.deltas.get(*key) != Some(*value))
                    .chain(
                        step.deltas
                            .iter()
                            .filter(|(key, value)| actual_deltas.get(*key) != Some(*value)),
                    )
                    .collect();
                problems.push(format!("deltas {differing:?}"));
            }
            deltas += step.deltas.len();
            baseline = current;
            if !problems.is_empty() {
                mismatches.push(format!("{label}: {}", problems.join("; ")));
            }
            total += 1;
        }
    }
    println!(
        "component option cases={total} ok={successes} failing={failures} declined={declined} deltas={deltas} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 18000);
    assert_eq!(successes + failures + declined, total);
}
