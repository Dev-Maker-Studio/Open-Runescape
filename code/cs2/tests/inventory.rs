#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
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

#[derive(Default)]
struct InventoryHost {
    params: BTreeMap<i32, Arc<ParamDefinition>>,
    model: ItemModel,
}

impl VarHost for InventoryHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn items(&mut self) -> Result<&mut ItemModel, HostError> {
        Ok(&mut self.model)
    }

    fn param_definition(&self, id: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        Ok(self.params.get(&id).cloned())
    }
}

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

fn tooltips(value: &str) -> Vec<Option<Arc<str>>> {
    value
        .split(',')
        .map(|entry| match entry {
            "null" => None,
            hex => Some(Arc::from(unhex(&hex[1..]))),
        })
        .collect()
}

fn host() -> InventoryHost {
    let mut host = InventoryHost::default();
    for line in include_str!("fixtures/inventory-defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        let number = |index: usize| -> i32 { columns[index].parse().unwrap() };
        match columns[0] {
            "param" => {
                host.params.insert(
                    number(1),
                    Arc::new(ParamDefinition {
                        is_string: number(2) == i32::from(b's'),
                        default_int: number(3),
                        type_name: Arc::from(unhex(columns[4])),
                    }),
                );
            }
            "item" => {
                assert_eq!(host.model.items.len(), number(1) as usize);
                host.model.items.push(ItemRecord {
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
                let list = |index: usize| {
                    if columns[index] == "-" {
                        Vec::new()
                    } else {
                        integers(columns[index])
                    }
                };
                host.model.container_types.insert(
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
                let key =
                    i64::from(number(1)) | if number(2) == 1 { i64::from(i32::MIN) } else { 0 };
                host.model.containers.insert(
                    key,
                    ItemContainer {
                        item_ids: integers(columns[3]),
                        amounts: integers(columns[4]),
                        tooltips: tooltips(columns[5]),
                    },
                );
            }
            _ => unreachable!(),
        }
    }
    host
}

fn run(host: &mut dyn VarHost, code: &[(Opcode, Operand)]) -> (Vm, Result<(), VmError>) {
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), host)
        .map(|_| ());
    (vm, result)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_inventory_case_matches_the_actual_kotlin_dispatch() {
    let mut host = host();
    let mut total = 0;
    let mut failing = 0;
    let mut scenarios = 0;
    for (number, line) in include_str!("fixtures/inventory.native.tsv")
        .lines()
        .enumerate()
    {
        let columns: Vec<_> = line.split('\t').collect();
        if columns[0] == "S" {
            host.model.query = QueryResults::default();
            scenarios += 1;
            continue;
        }
        assert_eq!(columns.len(), 7, "{line}");
        let name = format!("{number}:{}", columns[1]);
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[1])
            .unwrap();
        let mut code = Vec::new();
        for value in strings(columns[3]) {
            match value {
                Some(value) => code.push(text(&value)),
                None => code.push(int(Opcode::PUSH_STRING_LOCAL, 0)),
            }
        }
        code.extend(integers(columns[2]).into_iter().map(constant));
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(&mut host, &code);
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
        assert_eq!(vm.int_stack(), integers(columns[5]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = strings(columns[6])
            .into_iter()
            .map(|value| value.map(Arc::from))
            .collect();
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        total += 1;
    }
    assert_eq!(scenarios, 5);
    assert_eq!(total, 8854);
    assert_eq!(failing, 96);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inventory_operations_without_an_item_host_stay_unsupported() {
    for opcode in OPERATIONS {
        let (_, result) = run(&mut Bare, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
        let repository = BTreeMap::from([(1, script(&[int(*opcode, 0), ret()]))]);
        let error = Vm::default().run(1, &repository, Arguments::default());
        assert_eq!(
            error.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inventory_operations_report_underflow_when_the_host_is_available() {
    let mut host = host();
    for opcode in OPERATIONS {
        if matches!(opcode, Opcode::OC_FINDNEXT | Opcode::OC_FINDRESTART) {
            continue;
        }
        let (_, result) = run(&mut host, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
    }
}
