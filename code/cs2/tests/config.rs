#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
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

#[derive(Default)]
struct ConfigHost {
    params: BTreeMap<i32, Arc<ParamDefinition>>,
    items: BTreeMap<i32, Arc<ItemDefinition>>,
    tables: BTreeMap<(u8, i32), Arc<ParamTable>>,
    colours: Option<InteractionColours>,
}

fn kind_index(kind: ConfigKind) -> u8 {
    match kind {
        ConfigKind::Npc => 0,
        ConfigKind::Seq => 1,
        ConfigKind::Struct => 2,
    }
}

impl VarHost for ConfigHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn item_definition(&self, id: i32) -> Result<Option<Arc<ItemDefinition>>, HostError> {
        Ok(self.items.get(&id).cloned())
    }

    fn param_definition(&self, id: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        Ok(self.params.get(&id).cloned())
    }

    fn config_params(
        &self,
        kind: ConfigKind,
        id: i32,
    ) -> Result<Option<Arc<ParamTable>>, HostError> {
        Ok(self.tables.get(&(kind_index(kind), id)).cloned())
    }

    fn interaction_colours(&self) -> Result<InteractionColours, HostError> {
        self.colours.ok_or(HostError::Unavailable)
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

fn actions(value: &str) -> Vec<Option<Arc<str>>> {
    value
        .split(',')
        .map(|entry| match entry {
            "null" => None,
            hex => Some(Arc::from(unhex(&hex[1..]))),
        })
        .collect()
}

fn host() -> ConfigHost {
    let mut host = ConfigHost::default();
    for line in include_str!("fixtures/config-defs.tsv").lines() {
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
                host.items.insert(
                    number(1),
                    Arc::new(ItemDefinition {
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
                    }),
                );
            }
            "npc" | "seq" | "struct" => {
                let kind = match columns[0] {
                    "npc" => 0,
                    "seq" => 1,
                    _ => 2,
                };
                if let Some(parsed) = table(columns[2]) {
                    host.tables.insert((kind, number(1)), Arc::new(parsed));
                }
            }
            "colours" => {
                host.colours = Some(InteractionColours {
                    members: number(1),
                    free: number(2),
                })
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
fn every_native_config_case_matches_the_actual_kotlin_dispatch() {
    let mut host = host();
    let mut total = 0;
    let mut failing = 0;
    for line in include_str!("fixtures/config.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 7, "{line}");
        let name = columns[0];
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[1])
            .unwrap();
        let mut code = Vec::new();
        for value in strings(columns[3]) {
            code.push(text(&value.unwrap()));
        }
        code.extend(integers(columns[2]).into_iter().map(constant));
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(&mut host, &code);
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
        assert_eq!(vm.int_stack(), integers(columns[5]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = strings(columns[6])
            .into_iter()
            .map(|value| value.map(Arc::from))
            .collect();
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        total += 1;
    }
    assert_eq!(total, 385);
    assert_eq!(failing, 8);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn config_operations_without_a_definition_host_stay_unsupported() {
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
fn config_operations_report_underflow_when_definitions_are_available() {
    let mut host = host();
    for opcode in OPERATIONS {
        let (_, result) = run(&mut host, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
    }
}
