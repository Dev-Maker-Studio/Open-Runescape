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
}

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    hooks: Vec<(bool, i32, usize)>,
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
    let c = model.component(id);
    let children = model
        .slot_children(id)
        .map_or(-1, |children| children.len() as i64);
    let numbers = [
        c.component_type.map_or(-1, i32::from),
        c.id_hash,
        c.parent_id,
        c.slot_id,
        c.x,
        c.y,
        c.width,
        c.height,
        c.base_position_x,
        c.base_position_y,
        c.base_width,
        c.base_height,
        i32::from(c.aspect_x_type),
        i32::from(c.aspect_y_type),
        i32::from(c.aspect_width_type),
        i32::from(c.aspect_height_type),
        c.aspect_width,
        c.aspect_height,
        c.aspect_x,
        c.aspect_y,
        i32::from(c.hidden),
        c.scroll_x,
        c.scroll_y,
        c.scroll_width,
        c.scroll_height,
        c.sprite_id,
    ];
    let mut fields: Vec<String> = numbers.iter().map(|n| n.to_string()).collect();
    fields.push(hex(&c.text));
    fields.push(c.color.to_string());
    fields.push(c.transparency.to_string());
    fields.push(i32::from(c.redrawn).to_string());
    fields.push(children.to_string());
    let flag = |value: bool| i32::from(value).to_string();
    let optional = |value: &Option<Arc<str>>| match value {
        None => "null".to_string(),
        Some(value) => format!("hex:{}", hex(value)),
    };
    fields.extend([
        flag(c.filled),
        c.line_width.to_string(),
        flag(c.alpha),
        flag(c.shadow),
        c.border_thickness.to_string(),
        c.sprite_shadow.to_string(),
        flag(c.flip_vertical),
        flag(c.flip_horizontal),
        c.angle_2d.to_string(),
        flag(c.tiling),
        flag(c.line_direction),
        c.max_text_lines.to_string(),
        flag(c.prevent_click_through),
        c.sprite_scale.to_string(),
        c.font_id.to_string(),
        flag(c.monospaced),
        flag(c.uses_orthogonal),
        c.origin_x.to_string(),
        c.origin_y.to_string(),
        c.text_horizontal_alignment.to_string(),
        c.text_vertical_alignment.to_string(),
        c.line_spacing.to_string(),
        optional(&c.target_verb),
        optional(&c.resume_text),
        flag(c.click_mask),
        flag(c.text_anti_macro),
        c.drag_deadzone.to_string(),
        c.offset_x.to_string(),
        c.offset_y.to_string(),
        c.sprite_pitch.to_string(),
        c.sprite_roll.to_string(),
        c.sprite_yaw.to_string(),
        c.skybox_id.to_string(),
        c.light_x.to_string(),
        c.light_y.to_string(),
        c.light_z.to_string(),
        c.sun_x.to_string(),
        c.sun_y.to_string(),
        c.tint_red.to_string(),
        c.tint_green.to_string(),
        c.tint_blue.to_string(),
        c.tint_scalar.to_string(),
        c.media_type.to_string(),
        c.primary_media_id.to_string(),
        c.secondary_media_id.to_string(),
        flag(c.wear_col),
    ]);
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
            if let Some(children) = model.slot_children(component) {
                for (slot, child) in children.iter().enumerate() {
                    if let Some(child) = child {
                        out.insert(format!("i{id}.{index}.s{slot}"), describe(model, *child));
                    }
                }
            }
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
fn every_native_component_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/components-extra.native.tsv"));
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
        "component native cases={total} ok={successes} failing={failures} declined={declined} deltas={deltas} mismatches={}",
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
