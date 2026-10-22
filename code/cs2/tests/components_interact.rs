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
    calls: Vec<(OptionInteraction, String)>,
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

    fn interface_option(
        &mut self,
        interaction: OptionInteraction,
        name: &str,
    ) -> Result<(), HostError> {
        self.calls.push((interaction, name.to_string()));
        Ok(())
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
    deltas: BTreeMap<String, String>,
    entity: EntityInterface,
    before: Option<InterfaceSelection>,
    packet: Option<(i32, i32, i32, i32)>,
    after: InterfaceSelection,
}

struct Settings {
    interface: i32,
    index: usize,
    target: Option<ComponentSettings>,
    item: i32,
    over: i32,
    leave: i32,
}

fn selection_of(columns: &[&str]) -> InterfaceSelection {
    InterfaceSelection {
        active: columns[1] == "1",
        hash: number(columns[2]),
        slot: number(columns[3]),
        flags: number(columns[4]),
        interface: number(columns[5]),
        item: number(columns[6]),
        use_cursor: number(columns[7]),
        default_cursor: number(columns[8]),
    }
}

struct World {
    name: String,
    capacity: i32,
    width: i32,
    height: i32,
    interfaces: Vec<(i32, bool, Vec<Option<ComponentState>>)>,
    hooks: Vec<(bool, i32, usize)>,
    settings: Vec<Settings>,
    overrides: Vec<(i32, i32, ComponentSettings)>,
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
    let mut pending: Option<InterfaceSelection> = None;
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
            "S" => worlds.last_mut().unwrap().settings.push(Settings {
                interface: number(columns[1]),
                index: columns[2].parse().unwrap(),
                target: if columns[3] == "-" {
                    None
                } else {
                    Some(ComponentSettings {
                        hash: number(columns[3]),
                        interface_id: number(columns[4]),
                    })
                },
                item: number(columns[5]),
                over: number(columns[6]),
                leave: number(columns[7]),
            }),
            "O" => worlds.last_mut().unwrap().overrides.push((
                number(columns[1]),
                number(columns[2]),
                ComponentSettings {
                    hash: number(columns[3]),
                    interface_id: number(columns[4]),
                },
            )),
            "M" => pending = Some(selection_of(columns)),
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                assert_eq!(result.len(), 7, "{result:?}");
                let packet = &lines[index + 2];
                assert_eq!(packet[0], "P");
                let after = &lines[index + 3];
                assert_eq!(after[0], "X");
                index += 3;
                let mut deltas = BTreeMap::new();
                while index + 1 < lines.len() && lines[index + 1][0] == "D" {
                    index += 1;
                    deltas.insert(lines[index][1].to_string(), lines[index][2..].join("\t"));
                }
                worlds.last_mut().unwrap().steps.push(Step {
                    name: columns[2].to_string(),
                    operand: number(columns[3]),
                    ints: integers(columns[4]),
                    result: result[1].to_string(),
                    out_ints: integers(result[2]),
                    hooks: [result[5].to_string(), result[6].to_string()],
                    deltas,
                    entity: match columns[5] {
                        "-" => EntityInterface::Absent,
                        "u" => EntityInterface::Unbound,
                        id => EntityInterface::Bound(number(id)),
                    },
                    before: pending.take(),
                    packet: if packet[1] == "-" {
                        None
                    } else {
                        Some((
                            number(packet[1]),
                            number(packet[2]),
                            number(packet[3]),
                            number(packet[4]),
                        ))
                    },
                    after: selection_of(after),
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
        c.drag_dead_time.to_string(),
        c.drag_type.to_string(),
    ]);
    fields.push(match c.depth_child {
        None => "-".to_string(),
        Some(child) => {
            let child = model.component(child);
            format!("{}:{}", child.id_hash, child.slot_id)
        }
    });
    let shorts = |value: &Option<([i16; 5], [i16; 5])>| match value {
        None => "-/-".to_string(),
        Some((source, destination)) => {
            let join = |values: &[i16; 5]| {
                values
                    .iter()
                    .map(|n| n.to_string())
                    .collect::<Vec<_>>()
                    .join(",")
            };
            format!("{}/{}", join(source), join(destination))
        }
    };
    fields.push(shorts(&c.recolor));
    fields.push(shorts(&c.retexture));
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
    for row in &world.settings {
        let id = model.interface_component(row.interface, row.index).unwrap();
        model.set_target_settings(id, row.target);
        model.set_container_item(id, row.item);
        model.set_target_cursors(id, row.over, row.leave);
    }
    let mut seen = std::collections::BTreeSet::new();
    for (hash, slot, settings) in &world.overrides {
        if seen.insert((*hash, *slot)) {
            model.set_slot_settings(*hash, *slot, Some(*settings));
        }
    }
    model
}

fn baseline_dump(
    _world: &World,
    baseline: &BTreeMap<String, String>,
) -> BTreeMap<String, String> {
    baseline
        .iter()
        .filter(|(key, _)| !key.starts_with('n'))
        .map(|(key, value)| (key.clone(), value.clone()))
        .collect()
}

fn item_lengths(world: &World, model: &ComponentModel) -> BTreeMap<String, String> {
    let mut out = BTreeMap::new();
    for (id, _, components) in &world.interfaces {
        for index in 0..components.len() {
            if let Some(component) = model.interface_component(*id, index) {
                out.insert(
                    format!("n{id}.{index}"),
                    model
                        .item_slots(component)
                        .map_or("-".to_string(), |items| items.len().to_string()),
                );
            }
        }
    }
    out
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_interface_interaction_case_matches_the_actual_kotlin_dispatch() {
    let (worlds, expected_total) = parse(include_str!("fixtures/components-interact.native.tsv"));
    assert_eq!(worlds.len(), 4);
    let mut total = 0;
    let mut successes = 0;
    let mut failures = 0;
    let mut deltas = 0;
    let mut packets = 0;
    let mut declined = 0;
    let mut mismatches = Vec::new();
    for world in &worlds {
        let mut host = ModelHost {
            model: model_for(world),
            calls: Vec::new(),
        };
        let mut baseline = dump(world, &host.model);
        for step in &world.steps {
            let opcode = opcode_named(&step.name);
            let mut code = push_all(&step.ints, &[]);
            let pushed = code.len();
            code.extend([int(opcode, step.operand), ret()]);
            let repository = build(&code);
            if let Some(selection) = step.before {
                host.model.set_selection(selection);
            }
            host.model.set_entity_interface(step.entity);
            host.calls.clear();
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
                    || step.hooks[0] != hook_text(&host.model, false)
                    || step.hooks[1] != hook_text(&host.model, true)
                    || !host.calls.is_empty()
                    || host.model.selection() != step.after
                    || dump(world, &host.model) != baseline_dump(world, &baseline)
                {
                    mismatches.push(format!("{label}: declined {} vs {result:?}", step.result));
                }
                host.model.take_events();
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
            let mut expected_ints = step.out_ints.clone();
            if result.is_err() {
                while expected_ints.last() == Some(&STALE_INT) {
                    expected_ints.pop();
                }
            }
            if vm.int_stack() != expected_ints.as_slice() {
                problems.push(format!("ints {:?} vs {:?}", vm.int_stack(), step.out_ints));
            }
            host.model.take_events();
            for (slot, expected) in step.hooks.iter().enumerate() {
                let actual = hook_text(&host.model, slot == 1);
                if &actual != expected {
                    problems.push(format!("hook{slot} {actual} vs {expected}"));
                }
            }
            let actual_packet: Vec<_> = host
                .calls
                .iter()
                .map(|(call, name)| {
                    (
                        call.option,
                        call.hash,
                        i32::from(call.item_id as u16),
                        i32::from(call.slot as u16),
                        name.clone(),
                    )
                })
                .collect();
            let expected_packet: Vec<_> = step
                .packet
                .iter()
                .map(|(option, hash, item, slot)| (*option, *hash, *item, *slot, String::new()))
                .collect();
            if actual_packet != expected_packet {
                problems.push(format!("packet {actual_packet:?} vs {expected_packet:?}"));
            }
            packets += expected_packet.len();
            if host.model.selection() != step.after {
                problems.push(format!(
                    "selection {:?} vs {:?}",
                    host.model.selection(),
                    step.after
                ));
            }
            let mut current = dump(world, &host.model);
            current.extend(item_lengths(world, &host.model));
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
        "interact native cases={total} ok={successes} failing={failures} packets={packets} declined={declined} deltas={deltas} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 18000);
    assert_eq!(successes + failures + declined, total);
    assert!(packets >= 1000);
}
