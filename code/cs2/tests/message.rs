#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::sync::Arc;

#[derive(Default)]
struct MessageHost {
    language: Option<i32>,
    entity: EntityState,
    outbox: Outbox,
}

impl VarHost for MessageHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn language(&self) -> Result<Option<i32>, HostError> {
        Ok(self.language)
    }

    fn entity_state(&self) -> Result<&EntityState, HostError> {
        Ok(&self.entity)
    }

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn decode(value: &str) -> Option<Arc<str>> {
    if value == "~" {
        return None;
    }
    let units: Vec<u16> = (1..value.len())
        .step_by(4)
        .map(|start| u16::from_str_radix(&value[start..start + 4], 16).unwrap())
        .collect();
    Some(Arc::from(String::from_utf16(&units).unwrap().as_str()))
}

fn language_id(name: &str) -> Option<i32> {
    ["EN", "DE", "FR", "PT", "NL", "ES"]
        .iter()
        .position(|candidate| *candidate == name)
        .map(|index| index as i32)
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

fn run(
    opcode: Opcode,
    host: &mut MessageHost,
    strings: &[Option<Arc<str>>],
    ints: &[i32],
) -> (Vm, Result<(), VmError>) {
    let mut code = Vec::new();
    for index in 0..strings.len() {
        code.push(int(Opcode::PUSH_STRING_LOCAL, index as i32));
    }
    for index in 0..ints.len() {
        code.push(int(Opcode::PUSH_INT_LOCAL, index as i32));
    }
    let initial = code.len();
    code.push(int(opcode, 0));
    code.push(ret());
    let counts = counts(ints.len() as u16, strings.len() as u16, 0);
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts, counts, &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(
            1,
            &repository,
            Arguments {
                ints,
                strings,
                longs: &[],
            },
            host,
        )
        .map(|_| ());
    let result = result.map_err(|mut error| {
        assert_eq!(error.instruction, initial);
        error.instruction = 0;
        error
    });
    (vm, result)
}

fn error_matches(error: &VmError, class: &str) -> bool {
    match &error.kind {
        VmErrorKind::Host(HostError::NoEntity) => class == "NullPointerException",
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn message_compare_and_coord_ops_match_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/message.native.tsv");
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for c in &cases {
        let name = c[1];
        let opcode = opcode_named(name);
        let mut host = MessageHost {
            language: language_id(c[2]),
            ..MessageHost::default()
        };
        let mut strings: Vec<Option<Arc<str>>> = Vec::new();
        let mut ints: Vec<i32> = Vec::new();
        match name {
            "COMPARE" => strings.extend(c[3].split('|').map(decode)),
            "MES" => strings.push(decode(c[3])),
            "MES_TYPED" => {
                let parts: Vec<&str> = c[3].split('|').collect();
                strings.push(decode(parts[0]));
                ints.push(number(parts[1]));
                ints.push(number(parts[2]));
            }
            _ => {
                let parts: Vec<&str> = c[3].split(':').collect();
                host.entity.my_coord = (parts[0] == "P").then(|| MyCoord {
                    plane: number(parts[1]),
                    fine_x: number(parts[2]),
                    fine_z: number(parts[3]),
                    base_x: number(parts[4]),
                    base_y: number(parts[5]),
                });
            }
        }
        let (vm, result) = run(opcode, &mut host, &strings, &ints);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let mut problems = Vec::new();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let stack = vm.int_stack();
                let texts = if stack.is_empty() {
                    "-".to_string()
                } else {
                    stack
                        .iter()
                        .map(|value| value.to_string())
                        .collect::<Vec<_>>()
                        .join(",")
                };
                if texts != c[5] {
                    problems.push(format!("ints {texts} vs {}", c[5]));
                }
                let expected: Vec<&str> = c[6].split(':').collect();
                match (host.outbox.typed_messages.as_slice(), expected[0]) {
                    ([], "-") => {}
                    ([sent], "1") => {
                        let text = decode(expected[3]).unwrap();
                        if sent.message_type != number(expected[1])
                            || sent.effect_flags != number(expected[2])
                            || sent.text != *text
                        {
                            problems.push(format!("typed {sent:?} vs {}", c[6]));
                        }
                    }
                    (sent, _) => problems.push(format!("typed {sent:?} vs {}", c[6])),
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if !error_matches(error, &expected["error:".len()..])
                    || !host.outbox.typed_messages.is_empty()
                {
                    problems.push(format!("error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => problems.push(format!("{expected} vs {actual:?}")),
        }
        if !problems.is_empty() {
            mismatches.push(format!("{name} {} {}: {}", c[2], c[3], problems.join("; ")));
        }
    }
    println!("message cases={} mismatches={}", cases.len(), mismatches.len());
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), expected_total);
    assert_eq!(per_opcode.len(), 4);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn console_typed_messages_are_routed_to_the_console_outputs() {
    let mut host = MessageHost::default();
    let text: Option<Arc<str>> = Some(Arc::from("line"));
    run(Opcode::MES_TYPED, &mut host, std::slice::from_ref(&text), &[99, 0]).1.unwrap();
    run(Opcode::MES_TYPED, &mut host, std::slice::from_ref(&text), &[98, 0]).1.unwrap();
    assert_eq!(host.outbox.console_lines, vec!["line".to_string()]);
    assert_eq!(host.outbox.console_clears, vec!["line".to_string()]);
    assert!(host.outbox.typed_messages.is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn message_ops_are_unsupported_without_a_host() {
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
    for (opcode, strings, ints) in [
        (Opcode::COMPARE, 2, 0),
        (Opcode::MES, 1, 0),
        (Opcode::MES_TYPED, 1, 2),
        (Opcode::COORD, 0, 0),
    ] {
        let mut code: Vec<_> = (0..strings).map(|_| text("a")).collect();
        code.extend((0..ints).map(|_| constant(0)));
        code.extend([int(opcode, 0), ret()]);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
