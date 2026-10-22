#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

fn bytes(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn text(value: &str) -> Option<Arc<str>> {
    if value == "null" {
        return None;
    }
    let digits = value.strip_prefix("hex:").unwrap();
    Some(Arc::from(String::from_utf8(bytes(digits)).unwrap()))
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        return Vec::new();
    }
    value.split(',').map(|n| n.parse().unwrap()).collect()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn pure_operations_match_actual_native_dispatch_results() {
    let mut counts: BTreeMap<String, usize> = BTreeMap::new();
    let mut vm = Vm::default();
    let mut sigma = 0usize;
    for line in include_str!("fixtures/pure.native.tsv").lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 9);
        let name = fields[0];
        let opcode = OPCODES
            .iter()
            .find(|entry| entry.name == fields[1])
            .unwrap()
            .opcode;
        let operand: i32 = fields[2].parse().unwrap();
        let ints = integers(fields[3]);
        let input = if fields[4] == "-" {
            None
        } else {
            Some(text(fields[4]))
        };
        let strings = input.iter().cloned().collect::<Vec<_>>();
        let arguments = Counts {
            ints: 0,
            strings: strings.len() as u16,
            ..zero()
        };
        let mut code = Vec::new();
        for index in 0..strings.len() {
            code.push(int(Opcode::PUSH_STRING_LOCAL, index as i32));
        }
        code.extend(ints.iter().map(|n| constant(*n)));
        code.extend([int(opcode, operand), ret()]);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, arguments, arguments, &[])).unwrap(),
        )]);
        if opcode == Opcode::LOWERCASE
            && fields[4] != "null"
            && input
                .as_ref()
                .is_some_and(|t| t.as_deref().is_some_and(|t| t.contains('\u{3a3}')))
        {
            let error = vm
                .run(
                    1,
                    &repository,
                    Arguments {
                        ints: &[],
                        strings: &strings,
                        longs: &[],
                    },
                )
                .unwrap_err();
            assert_eq!(error.kind, VmErrorKind::UnsupportedOpcode(opcode), "{name}");
            sigma += 1;
            continue;
        }
        let result = vm.run(
            1,
            &repository,
            Arguments {
                ints: &[],
                strings: &strings,
                longs: &[],
            },
        );
        *counts.entry(fields[1].to_string()).or_default() += 1;
        match fields[5] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
                assert_eq!(vm.int_stack(), integers(fields[7]), "{name}");
                let expected: Vec<Option<Arc<str>>> = if fields[8] == "-" {
                    Vec::new()
                } else {
                    vec![text(fields[8])]
                };
                assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
            }
            "error:NullPointerException" => {
                assert_eq!(
                    result.unwrap_err().kind,
                    VmErrorKind::InvalidOperand,
                    "{name}"
                );
            }
            "error:ArrayIndexOutOfBoundsException" => {
                assert!(fields[6].parse::<i32>().unwrap() < 0, "{name}");
                assert_eq!(
                    result.unwrap_err().kind,
                    VmErrorKind::StackUnderflow(StackKind::Int),
                    "{name}"
                );
                assert_eq!(vm.int_stack(), ints, "{name}");
            }
            other => panic!("{name}: {other}"),
        }
    }
    assert_eq!(sigma, 2105);
    assert_eq!(
        counts,
        BTreeMap::from([
            ("DATE_ISLEAPYEAR".to_string(), 15061),
            ("DATE_MINUTES_FROMRUNEDAY".to_string(), 11498),
            ("DATE_RUNEDAY_FROMDATE".to_string(), 46206),
            ("LOWERCASE".to_string(), 6649),
        ])
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn locale_dependent_operations_stay_unsupported() {
    use Opcode::*;
    for opcode in [
        COMPARE,
        TOSTRING_LOCALISED,
        TEXT_GENDER,
    ] {
        let error = Vm::default()
            .run(
                1,
                &BTreeMap::from([(1, script(&[int(opcode, 0), ret()]))]),
                Arguments::default(),
            )
            .unwrap_err();
        assert_eq!(error.kind, VmErrorKind::UnsupportedOpcode(opcode));
    }
}
