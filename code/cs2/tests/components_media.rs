#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{
    collections::{BTreeMap, BTreeSet},
    sync::Arc,
};

const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
const STALE_INT: i32 = 0x5a5a5a5a;
const IF_CONTINUE: u8 = 49;
const CLOSE_INTERFACE: u8 = 60;

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap_or_else(|| panic!("{name}"))
        .opcode
}

struct ModelHost {
    model: ComponentModel,
    outbox: Outbox,
    player: PlayerState,
    fonts: BTreeMap<i32, Arc<FontMetrics>>,
    items: BTreeMap<i32, ItemModelView>,
    npcs: BTreeMap<i32, Arc<NpcModelDefinition>>,
    sequences: BTreeSet<i32>,
    icons: Arc<[i32]>,
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

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }

    fn player_state(&self) -> Result<&PlayerState, HostError> {
        Ok(&self.player)
    }

    fn font_metrics(&self, id: i32) -> Result<Option<Arc<FontMetrics>>, HostError> {
        Ok(self.fonts.get(&id).cloned())
    }

    fn item_model_view(&self, id: i32) -> Result<Option<ItemModelView>, HostError> {
        Ok(self.items.get(&id).copied())
    }

    fn npc_model_definition(&self, id: i32) -> Result<Option<Arc<NpcModelDefinition>>, HostError> {
        Ok(self.npcs.get(&id).cloned())
    }

    fn sequence_playable(&self, id: i32) -> Result<bool, HostError> {
        Ok(self.sequences.contains(&id))
    }

    fn name_icon_widths(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        Ok(Some(self.icons.clone()))
    }

    fn inline_graphic_size(&self, _: i32, _: i32) -> Result<Option<(i32, i32)>, HostError> {
        Ok(None)
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
    console: String,
    events: String,
    hooks: [String; 2],
    packet: Option<(u8, String)>,
    requests: String,
    count: i32,
    forget: bool,
    deltas: BTreeMap<String, String>,
}

struct Settings {
    interface: i32,
    index: usize,
    target: Option<ComponentSettings>,
}

#[derive(Default)]
struct Statics {
    fonts: BTreeMap<i32, Vec<u8>>,
    icons: Vec<i32>,
    items: BTreeMap<i32, ItemModelView>,
    npcs: BTreeMap<i32, NpcModelDefinition>,
    sequences: BTreeSet<i32>,
    player: i32,
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
    resizes: Vec<(i32, usize, HookScript)>,
    media: Vec<(i32, usize, [i32; 8], Arc<str>)>,
    steps: Vec<Step>,
}

fn list(value: &str) -> Option<Vec<i32>> {
    match value {
        "-" => None,
        "e" => Some(Vec::new()),
        value => Some(value.split(',').map(number).collect()),
    }
}

fn short_list(value: &str) -> Option<Vec<i16>> {
    list(value).map(|values| values.into_iter().map(|n| n as i16).collect())
}

fn script(value: &str) -> HookScript {
    value
        .split(',')
        .map(|token| match token.split_at(1) {
            ("i", rest) => HookValue::Int(number(rest)),
            ("s", rest) => HookValue::Text(Some(Arc::from(String::from_utf8(unhex(rest.strip_prefix("hex:").unwrap_or(rest))).unwrap()))),
            other => panic!("{other:?}"),
        })
        .collect()
}

fn script_text(script: &HookScript) -> String {
    if script.is_empty() {
        return "-".to_string();
    }
    script
        .iter()
        .map(|value| match value {
            HookValue::Int(n) => format!("i{n}"),
            HookValue::Text(Some(text)) => format!("shex:{}", hex(text)),
            other => panic!("{other:?}"),
        })
        .collect::<Vec<_>>()
        .join(",")
}

fn parse(text: &str) -> (Statics, Vec<World>, usize) {
    let lines: Vec<Vec<&str>> = text
        .lines()
        .map(|line| line.split('\t').collect())
        .collect();
    let mut statics = Statics::default();
    let mut worlds: Vec<World> = Vec::new();
    let mut total = 0;
    let mut index = 0;
    let mut count = 0;
    let mut forget = false;
    while index < lines.len() {
        let columns = &lines[index];
        match columns[0] {
            "G" => {
                statics.fonts.insert(number(columns[1]), unhex(columns[2]));
            }
            "V" => statics.icons = integers(columns[1]),
            "J" => {
                statics.items.insert(
                    number(columns[1]),
                    ItemModelView {
                        rotation_x: number(columns[2]),
                        rotation_y: number(columns[3]),
                        rotation_z: number(columns[4]),
                        offset_x: number(columns[5]),
                        offset_y: number(columns[6]),
                        zoom: number(columns[7]),
                    },
                );
            }
            "L" => {
                statics.npcs.insert(
                    number(columns[1]),
                    NpcModelDefinition {
                        model_ids: list(columns[2]),
                        head_models: list(columns[3]),
                        recolor_dst: short_list(columns[4]),
                        retexture_dst: short_list(columns[5]),
                    },
                );
            }
            "E" => {
                statics.sequences.insert(number(columns[1]));
            }
            "Y" => statics.player = number(columns[1]),
            "W" => worlds.push(World {
                name: columns[1].to_string(),
                capacity: number(columns[2]),
                width: number(columns[3]),
                height: number(columns[4]),
                interfaces: Vec::new(),
                hooks: Vec::new(),
                settings: Vec::new(),
                overrides: Vec::new(),
                resizes: Vec::new(),
                media: Vec::new(),
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
            }),
            "H" => worlds.last_mut().unwrap().resizes.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                script(columns[3]),
            )),
            "F" => worlds.last_mut().unwrap().media.push((
                number(columns[1]),
                columns[2].parse().unwrap(),
                [
                    number(columns[3]),
                    number(columns[4]),
                    number(columns[6]),
                    number(columns[7]),
                    number(columns[8]),
                    number(columns[9]),
                    number(columns[10]),
                    0,
                ],
                Arc::from(String::from_utf8(unhex(columns[5])).unwrap()),
            )),
            "O" => worlds.last_mut().unwrap().overrides.push((
                number(columns[1]),
                number(columns[2]),
                ComponentSettings {
                    hash: number(columns[3]),
                    interface_id: number(columns[4]),
                },
            )),
            "B" => count = number(columns[1]),
            "A" => forget = true,
            "T" => {
                let result = &lines[index + 1];
                assert_eq!(result[0], "R");
                assert_eq!(result.len(), 7, "{result:?}");
                let packet = &lines[index + 2];
                assert_eq!(packet[0], "P");
                let requests = &lines[index + 3];
                assert_eq!(requests[0], "Q");
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
                    console: result[3].to_string(),
                    events: result[4].to_string(),
                    hooks: [result[5].to_string(), result[6].to_string()],
                    packet: if packet[1] == "-" {
                        None
                    } else {
                        Some((
                            match packet[1] {
                                "IF_CONTINUE" => IF_CONTINUE,
                                "CLOSE_INTERFACE" => CLOSE_INTERFACE,
                                other => panic!("{other}"),
                            },
                            if packet[2] == "-" {
                                String::new()
                            } else {
                                packet[2].to_string()
                            },
                        ))
                    },
                    requests: requests[1].to_string(),
                    count,
                    forget,
                    deltas,
                });
                forget = false;
            }
            "Z" => total = columns[1].parse().unwrap(),
            other => panic!("{other}"),
        }
        index += 1;
    }
    (statics, worlds, total)
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

fn event_text(events: &[ComponentEvent]) -> String {
    if events.is_empty() {
        return "-".to_string();
    }
    events
        .iter()
        .map(|event| {
            let kind = match event.kind {
                ComponentEventKind::Recolor(index) => format!("recol{index}"),
                ComponentEventKind::Retexture(index) => format!("retex{index}"),
                other => match other {
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
                    ComponentEventKind::Recolor(_) | ComponentEventKind::Retexture(_) => "",
                }
                .to_string(),
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
    let mut interfaces = world.interfaces.clone();
    for (id, _, components) in &mut interfaces {
        for (index, slot) in components.iter_mut().enumerate() {
            let Some(state) = slot else {
                continue;
            };
            for (media_id, media_index, values, text) in &world.media {
                if media_id == id && *media_index == index {
                    state.font_id = values[0];
                    state.width = values[1];
                    state.line_spacing = values[2];
                    state.media_type = values[3];
                    state.primary_media_id = values[4];
                    state.container_item_id = values[5];
                    state.item_stack_size = values[6];
                    state.text = text.clone();
                }
            }
            for (resize_id, resize_index, script) in &world.resizes {
                if resize_id == id && *resize_index == index {
                    state.hooks.set(HookSlot::OnResize, Some(script.clone()), None);
                }
            }
        }
    }
    for (id, prevent, components) in interfaces {
        model.add_interface(id, prevent, components).unwrap();
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
    }
    let mut seen = BTreeSet::new();
    for (hash, slot, settings) in &world.overrides {
        if seen.insert((*hash, *slot)) {
            model.set_slot_settings(*hash, *slot, Some(*settings));
        }
    }
    model
}

fn ints_text(values: &[i32]) -> String {
    values
        .iter()
        .map(|n| n.to_string())
        .collect::<Vec<_>>()
        .join(",")
}

fn shorts_text(values: &Option<Vec<i16>>) -> String {
    match values {
        None => "-".to_string(),
        Some(values) => values
            .iter()
            .map(|n| n.to_string())
            .collect::<Vec<_>>()
            .join(","),
    }
}

fn extra(model: &ComponentModel, id: ComponentId) -> String {
    let c = model.component(id);
    let custom = match &c.npc_customisation {
        None => "-".to_string(),
        Some(custom) => format!(
            "{}:{}:{}:{}",
            custom.uid,
            ints_text(&custom.model_ids),
            shorts_text(&custom.recolor_dst),
            shorts_text(&custom.retexture_dst)
        ),
    };
    format!(
        "{}\t{}\t{}\t{}\t{}\t{}",
        c.container_item_id, c.item_stack_size, c.render_stack, c.animation, c.video_file_id, custom
    )
}

fn snapshot(world: &World, model: &ComponentModel) -> BTreeMap<String, String> {
    let mut out = dump(world, model);
    for (id, _, components) in &world.interfaces {
        for index in 0..components.len() {
            let Some(component) = model.interface_component(*id, index) else {
                continue;
            };
            out.insert(
                format!("n{id}.{index}"),
                model
                    .item_slots(component)
                    .map_or("-".to_string(), |items| items.len().to_string()),
            );
            out.insert(format!("x{id}.{index}"), extra(model, component));
            if let Some(children) = model.slot_children(component) {
                for (slot, child) in children.iter().enumerate() {
                    if let Some(child) = child {
                        out.insert(format!("x{id}.{index}.s{slot}"), extra(model, *child));
                    }
                }
            }
        }
    }
    out.insert(
        "x.adc".to_string(),
        match model.active_dialogue_continue() {
            None => "-".to_string(),
            Some(id) => {
                let c = model.component(id);
                format!("{}:{}", c.id_hash, c.slot_id)
            }
        },
    );
    out
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_media_case_matches_the_actual_kotlin_dispatch() {
    let (statics, worlds, expected_total) =
        parse(include_str!("fixtures/components-media.native.tsv"));
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
            outbox: Outbox::default(),
            player: PlayerState {
                my_player_index: statics.player,
                ..PlayerState::default()
            },
            fonts: statics
                .fonts
                .iter()
                .map(|(id, bytes)| (*id, Arc::new(FontMetrics::decode(bytes).unwrap())))
                .collect(),
            items: statics.items.clone(),
            npcs: statics
                .npcs
                .iter()
                .map(|(id, definition)| (*id, Arc::new(definition.clone())))
                .collect(),
            sequences: statics.sequences.clone(),
            icons: Arc::from(statics.icons.clone()),
        };
        let mut baseline = snapshot(world, &host.model);
        for step in &world.steps {
            let opcode = opcode_named(&step.name);
            let mut code = push_all(&step.ints, &[]);
            let pushed = code.len();
            code.extend([int(opcode, step.operand), ret()]);
            let repository = build(&code);
            host.model.set_hook_request_count(step.count);
            if step.forget {
                host.model.forget_dialogue_continue();
            }
            host.outbox = Outbox::default();
            host.model.take_hook_requests();
            host.model.take_events();
            let mut vm = Vm::default();
            let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
            let label = format!("{}#{total} {} {}", world.name, step.name, step.operand);
            let mut problems = Vec::new();
            let was_declined = matches!(
                &result,
                Err(VmError {
                    kind: VmErrorKind::UnsupportedOpcode(_),
                    ..
                })
            );
            if was_declined {
                declined += 1;
                if step.result != "error:NullPointerException" {
                    problems.push(format!("declined {} vs {result:?}", step.result));
                }
            } else {
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
                    problems.push(format!("ints {:?} vs {:?} {result:?}", vm.int_stack(), step.out_ints));
                }
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
            let mut lines = std::mem::take(&mut host.outbox.console_lines);
            let console = match lines.pop() {
                None => "-".to_string(),
                Some(line) => hex(&line),
            };
            if !lines.is_empty() || console != step.console {
                problems.push(format!("console {console} vs {}", step.console));
            }
            let actual_packets: Vec<_> = host
                .outbox
                .drain_packets()
                .into_iter()
                .map(|packet| {
                    let payload: String = packet.payload.iter().map(|b| format!("{b:02x}")).collect();
                    (packet.opcode, payload)
                })
                .collect();
            let expected_packets: Vec<_> = step.packet.iter().cloned().collect();
            if actual_packets != expected_packets {
                problems.push(format!("packet {actual_packets:?} vs {expected_packets:?}"));
            }
            packets += expected_packets.len();
            let requests = host.model.take_hook_requests();
            let requests = if requests.is_empty() {
                "-".to_string()
            } else {
                requests
                    .iter()
                    .map(|request| {
                        format!(
                            "{}:{}:{}",
                            request.source_hash,
                            request.request_id,
                            script_text(&request.params)
                        )
                    })
                    .collect::<Vec<_>>()
                    .join("|")
            };
            if requests != step.requests {
                problems.push(format!("requests {requests} vs {}", step.requests));
            }
            let current = snapshot(world, &host.model);
            let mut actual_deltas = BTreeMap::new();
            for key in current.keys().chain(baseline.keys()) {
                let now = current.get(key);
                if now != baseline.get(key) {
                    actual_deltas.insert(key.clone(), now.cloned().unwrap_or("-".to_string()));
                }
            }
            if actual_deltas != step.deltas {
                let differing = (&actual_deltas, &step.deltas);
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
        "media native cases={total} ok={successes} failing={failures} packets={packets} declined={declined} deltas={deltas} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(total, expected_total);
    assert_eq!(total, 18000);
    assert_eq!(successes + failures + declined, total);
    assert!(packets >= 100);
}
