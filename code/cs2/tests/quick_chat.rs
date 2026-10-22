#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
    Opcode::CHATCAT_GETDESC,
    Opcode::CHATCAT_GETSUBCATCOUNT,
    Opcode::CHATCAT_GETSUBCAT,
    Opcode::CHATCAT_GETPHRASECOUNT,
    Opcode::CHATCAT_GETPHRASE,
    Opcode::CHATCAT_GETSUBCATSHORTCUT,
    Opcode::CHATCAT_GETPHRASESHORTCUT,
    Opcode::CHATCAT_FINDSUBCATBYSHORTCUT,
    Opcode::CHATCAT_FINDPHRASEBYSHORTCUT,
    Opcode::CHATPHRASE_GETTEXT,
    Opcode::CHATPHRASE_GETAUTORESPONSECOUNT,
    Opcode::CHATPHRASE_GETAUTORESPONSE,
    Opcode::CHATPHRASE_GETDYNAMICCOMMANDCOUNT,
    Opcode::CHATPHRASE_GETDYNAMICCOMMAND,
    Opcode::CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM,
    Opcode::CHATPHRASE_FIND,
    Opcode::CHATPHRASE_FINDNEXT,
    Opcode::CHATPHRASE_FINDRESTART,
];

struct QuickChatHost {
    defs: QuickChatDefs,
    items: ItemModel,
}

impl VarHost for QuickChatHost {
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
        Ok(&mut self.items)
    }

    fn quick_chat(&self) -> Result<&QuickChatDefs, HostError> {
        Ok(&self.defs)
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

fn unhex_bytes(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
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

fn host() -> QuickChatHost {
    let mut defs = None;
    for line in include_str!("fixtures/quick-chat.defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        match columns[0] {
            "counts" => {
                defs = Some(QuickChatDefs::new(
                    columns[1].parse().unwrap(),
                    columns[2].parse().unwrap(),
                ))
            }
            "def" => {
                let menu = columns[1] == "1";
                let file: i32 = columns[3].parse().unwrap();
                let bytes = unhex_bytes(columns[4]);
                let defs = defs.as_mut().unwrap();
                if columns[2] == "c" {
                    defs.insert_category(menu, file, &bytes).unwrap();
                } else {
                    defs.insert_message(menu, file, &bytes).unwrap();
                }
            }
            other => panic!("{other}"),
        }
    }
    QuickChatHost {
        defs: defs.unwrap(),
        items: ItemModel::default(),
    }
}

fn run(host: &mut dyn VarHost, code: &[(Opcode, Operand)]) -> (Vm, Result<(), VmError>) {
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 2, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), host)
        .map(|_| ());
    (vm, result)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_quick_chat_case_matches_the_actual_kotlin_dispatch() {
    let mut host = host();
    let mut total = 0;
    let mut failing = 0;
    let mut trailer = None;
    for line in include_str!("fixtures/quick-chat.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        if columns[0] == "N" {
            trailer = Some(columns[1].parse::<usize>().unwrap());
            continue;
        }
        assert_eq!(columns.len(), 8, "{line}");
        let name = columns[1];
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[2])
            .unwrap();
        let mut code = Vec::new();
        for value in strings(columns[4]) {
            code.push(text(&value.unwrap()));
        }
        code.extend(integers(columns[3]).into_iter().map(constant));
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(&mut host, &code);
        match columns[5] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
            }
            "error:NullPointerException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::Host(HostError::NoEntity), "{name}");
                failing += 1;
            }
            "error:ArrayIndexOutOfBoundsException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(
                    error.kind,
                    VmErrorKind::Host(HostError::State("index out of bounds".to_string())),
                    "{name}"
                );
                failing += 1;
            }
            "error:RuntimeException" => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::InvalidOperand, "{name}");
                failing += 1;
            }
            other => panic!("{name}: unexpected {other}"),
        }
        assert_eq!(vm.int_stack(), integers(columns[6]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = strings(columns[7])
            .into_iter()
            .map(|value| value.map(Arc::from))
            .collect();
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        total += 1;
    }
    assert_eq!(trailer, Some(total));
    assert_eq!(total, 3521);
    assert_eq!(failing, 1993);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn quick_chat_operations_without_a_definition_host_stay_unsupported() {
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
fn quick_chat_operations_report_underflow_when_definitions_are_available() {
    let mut host = host();
    for opcode in OPERATIONS {
        if matches!(
            opcode,
            Opcode::CHATPHRASE_FINDNEXT | Opcode::CHATPHRASE_FINDRESTART
        ) {
            continue;
        }
        let (_, result) = run(&mut host, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int),
            "{opcode:?}"
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn quick_chat_definitions_decode_the_documented_layout() {
    let category = QuickChatCategory::decode(b"\x01Root\x00\x02\x01\x00\x00a\x03\x01\x00\x07b\x00")
        .unwrap();
    assert_eq!(category.name.as_deref(), Some("Root"));
    assert_eq!(category.sub_category_hotkeys, Some(vec![u16::from(b'a')]));
    assert_eq!(category.messages, Some(vec![7]));
    let message = QuickChatMessage::decode(b"\x01a<b\x00\x04\x00").unwrap();
    assert!(!message.searchable);
    assert_eq!(&*message.placeholder_text(), "a...b");
    assert!(QuickChatMessage::decode(b"\x02").is_err());
}
