#[allow(dead_code)]
#[path = "../../cs2/tests/common/mod.rs"]
mod common;

use common::*;
use openrs_client_runtime::vars::config::ConfigStore;
use openrs_client_runtime::vars::{WorldVarHost, WorldVariables};
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const CONFIG_OPERATIONS: &[Opcode] = &[
    Opcode::OC_NAME,
    Opcode::OC_OP,
    Opcode::OC_IOP,
    Opcode::OC_COST,
    Opcode::OC_STACKABLE,
    Opcode::OC_CERT,
    Opcode::OC_UNCERT,
    Opcode::OC_WEARPOS,
    Opcode::OC_WEARPOS2,
    Opcode::OC_WEARPOS3,
    Opcode::OC_MEMBERS,
    Opcode::OC_PARAM,
    Opcode::OC_ICURSOR,
    Opcode::OC_MULTISTACKSIZE,
    Opcode::OC_MINIMENU_COLOUR_OVERRIDDEN,
    Opcode::OC_MINIMENU_COLOUR,
    Opcode::NC_PARAM,
    Opcode::STRUCT_PARAM,
    Opcode::SEQ_PARAM,
];

const INVENTORY_OPERATIONS: &[Opcode] = &[
    Opcode::INV_GETOBJ,
    Opcode::INV_GETNUM,
    Opcode::INV_TOTAL,
    Opcode::INV_SIZE,
    Opcode::INV_STOCKBASE,
    Opcode::INVOTHER_GETOBJ,
    Opcode::INVOTHER_GETNUM,
    Opcode::INVOTHER_TOTAL,
    Opcode::INV_FREESPACE,
    Opcode::INV_TOTALPARAM,
    Opcode::INV_TOTALPARAM_STACK,
    Opcode::INV_GETTOOLTIP,
    Opcode::INV_OTHER_GETTOOLTIP,
    Opcode::OC_FIND,
    Opcode::OC_FINDNEXT,
    Opcode::OC_FINDRESTART,
    Opcode::ITEM_FIND_PARAMINT,
    Opcode::ITEM_FIND_PARAMSTR,
];

fn unhex(value: &str) -> String {
    let bytes: Vec<u8> = (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect();
    String::from_utf8(bytes).unwrap()
}

fn utf16(value: &str) -> Option<String> {
    if value == "null" {
        return None;
    }
    let units: Vec<u16> = (0..(value.len() - 1) / 4)
        .map(|index| u16::from_str_radix(&value[1 + index * 4..5 + index * 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(utf16).collect()
    }
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn table(value: &str) -> Option<ParamTable> {
    match value {
        "-" => None,
        "empty" => Some(ParamTable::new()),
        _ => Some(
            value
                .split(';')
                .map(|entry| {
                    let (key, rest) = entry.split_once('=').unwrap();
                    let parsed = match rest.split_at(1) {
                        ("i", number) => EnumValue::Int(number.parse().unwrap()),
                        ("s", hex) => EnumValue::Text(Arc::from(unhex(hex))),
                        _ => unreachable!(),
                    };
                    (key.parse().unwrap(), parsed)
                })
                .collect(),
        ),
    }
}

fn actions(value: &str) -> Vec<Option<Arc<str>>> {
    value
        .split(',')
        .map(|entry| match entry {
            "null" => None,
            hex => Some(Arc::from(unhex(&hex[1..]))),
        })
        .collect()
}

fn param(columns: &[&str]) -> (i32, ParamDefinition) {
    (
        columns[1].parse().unwrap(),
        ParamDefinition {
            is_string: columns[2].parse::<i32>().unwrap() == i32::from(b's'),
            default_int: columns[3].parse().unwrap(),
            type_name: Arc::from(unhex(columns[4])),
        },
    )
}

fn config_store() -> ConfigStore {
    let mut store = ConfigStore::new().closed();
    for line in include_str!("../../cs2/tests/fixtures/config-defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        let number = |index: usize| -> i32 { columns[index].parse().unwrap() };
        match columns[0] {
            "param" => {
                let (id, definition) = param(&columns);
                store.insert_param(id, definition);
            }
            "item" => store.insert_item(
                number(1),
                ItemDefinition {
                    name: Arc::from(unhex(columns[2])),
                    value: number(3),
                    stackable: number(4),
                    members_only: columns[5] == "true",
                    cert_id: number(6),
                    cert_template_id: number(7),
                    wear_pos: number(8),
                    wear_pos2: number(9),
                    wear_pos3: number(10),
                    multi_stack_size: number(11),
                    ground_actions: actions(columns[12]),
                    inventory_actions: actions(columns[13]),
                    tooltip_color: number(14),
                    has_tooltip_color: columns[15] == "true",
                    cursor_op1: number(16),
                    cursor_id1: number(17),
                    cursor_op2: number(18),
                    cursor_id2: number(19),
                    params: table(columns[20]),
                },
            ),
            "npc" | "seq" | "struct" => {
                let kind = match columns[0] {
                    "npc" => ConfigKind::Npc,
                    "seq" => ConfigKind::Seq,
                    _ => ConfigKind::Struct,
                };
                if let Some(parsed) = table(columns[2]) {
                    store.insert_table(kind, number(1), parsed);
                }
            }
            "colours" => store.set_colours(InteractionColours {
                members: number(1),
                free: number(2),
            }),
            _ => unreachable!(),
        }
    }
    store
}

fn inventory_fixtures() -> (ConfigStore, ItemModel) {
    let mut store = ConfigStore::new().closed();
    let mut model = ItemModel::default();
    for line in include_str!("../../cs2/tests/fixtures/inventory-defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        let number = |index: usize| -> i32 { columns[index].parse().unwrap() };
        match columns[0] {
            "param" => {
                let (id, definition) = param(&columns);
                store.insert_param(id, definition);
            }
            "item" => {
                assert_eq!(model.items.len(), number(1) as usize);
                model.items.push(ItemRecord {
                    name: Arc::from(unhex(columns[2])),
                    tradeable: columns[3] == "true",
                    unknown_int6: number(4),
                    cert_template_id: number(5),
                    lend_template_id: number(6),
                    bind_template_id: number(7),
                    params: table(columns[8]),
                });
            }
            "ctype" => {
                let list = |index: usize| integers(columns[index]);
                model.container_types.insert(
                    number(1),
                    ContainerType {
                        default_item_size: number(2),
                        max_item_size: number(3),
                        item_ids: list(4),
                        amounts: list(5),
                    },
                );
            }
            "container" => {
                let key = i64::from(number(1))
                    | if number(2) == 1 {
                        i64::from(i32::MIN)
                    } else {
                        0
                    };
                model.containers.insert(
                    key,
                    ItemContainer {
                        item_ids: integers(columns[3]),
                        amounts: integers(columns[4]),
                        tooltips: actions(columns[5]),
                    },
                );
            }
            _ => unreachable!(),
        }
    }
    (store, model)
}

fn run(
    store: Option<&ConfigStore>,
    model: &mut ItemModel,
    code: &[(Opcode, Operand)],
) -> (Vm, Result<(), VmError>) {
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut variables = WorldVariables::new(4).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0);
    if let Some(store) = store {
        host = host.with_config(store, model);
    }
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), &mut host)
        .map(|_| ());
    (vm, result)
}

fn push_arguments(columns: &[&str]) -> (Vec<(Opcode, Operand)>, usize) {
    let mut code = Vec::new();
    for value in strings(columns[3]) {
        match value {
            Some(value) => code.push(text(&value)),
            None => code.push(int(Opcode::PUSH_STRING_LOCAL, 0)),
        }
    }
    code.extend(integers(columns[2]).into_iter().map(constant));
    let pushed = code.len();
    (code, pushed)
}

fn operation(operations: &[Opcode], name: &str) -> Opcode {
    operations
        .iter()
        .copied()
        .find(|opcode| opcode.metadata().name == name)
        .unwrap()
}

fn assert_stacks(vm: &Vm, columns: &[&str], name: &str) {
    assert_eq!(vm.int_stack(), integers(columns[5]).as_slice(), "{name}");
    let expected: Vec<Option<Arc<str>>> = strings(columns[6])
        .into_iter()
        .map(|value| value.map(Arc::from))
        .collect();
    assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_host_matches_every_native_config_case() {
    let store = config_store();
    let mut model = ItemModel::default();
    let mut total = 0;
    let mut failing = 0;
    for line in include_str!("../../cs2/tests/fixtures/config.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 7, "{line}");
        let name = columns[0];
        let opcode = operation(CONFIG_OPERATIONS, columns[1]);
        let (mut code, pushed) = push_arguments(&columns);
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(Some(&store), &mut model, &code);
        match columns[4] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
            }
            "error:ClassCastException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::InvalidOperand, "{name}");
                failing += 1;
            }
            other => panic!("{name}: unexpected {other}"),
        }
        assert_stacks(&vm, &columns, name);
        total += 1;
    }
    assert_eq!(total, 385);
    assert_eq!(failing, 8);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_host_matches_every_native_inventory_case() {
    let (store, mut model) = inventory_fixtures();
    let mut total = 0;
    let mut failing = 0;
    let mut scenarios = 0;
    for (number, line) in include_str!("../../cs2/tests/fixtures/inventory.native.tsv")
        .lines()
        .enumerate()
    {
        let columns: Vec<_> = line.split('\t').collect();
        if columns[0] == "S" {
            model.query = QueryResults::default();
            scenarios += 1;
            continue;
        }
        assert_eq!(columns.len(), 7, "{line}");
        let name = format!("{number}:{}", columns[1]);
        let opcode = operation(INVENTORY_OPERATIONS, columns[1]);
        let (mut code, pushed) = push_arguments(&columns);
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(Some(&store), &mut model, &code);
        match columns[4] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
            }
            "error:NullPointerException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::Host(HostError::NoEntity), "{name}");
                failing += 1;
            }
            "error:ClassCastException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(
                    error.kind,
                    VmErrorKind::Host(HostError::EntityType),
                    "{name}"
                );
                failing += 1;
            }
            other => panic!("{name}: unexpected {other}"),
        }
        assert_stacks(&vm, &columns, &name);
        total += 1;
    }
    assert_eq!(scenarios, 5);
    assert_eq!(total, 8854);
    assert_eq!(failing, 96);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_host_without_definitions_keeps_the_operations_unsupported() {
    let mut model = ItemModel::default();
    for opcode in CONFIG_OPERATIONS.iter().chain(INVENTORY_OPERATIONS) {
        let (_, result) = run(None, &mut model, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_host_reports_underflow_when_definitions_are_available() {
    let store = config_store();
    let mut model = ItemModel::default();
    for opcode in CONFIG_OPERATIONS {
        let (_, result) = run(Some(&store), &mut model, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
    }
    let (store, mut model) = inventory_fixtures();
    for opcode in INVENTORY_OPERATIONS {
        if matches!(opcode, Opcode::OC_FINDNEXT | Opcode::OC_FINDRESTART) {
            continue;
        }
        let (_, result) = run(Some(&store), &mut model, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
    }
}
