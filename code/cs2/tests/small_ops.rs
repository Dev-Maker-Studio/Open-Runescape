#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const STALE_INT: i32 = 0x5a5a5a5a;

#[derive(Default)]
struct SmallHost {
    world: WorldState,
    bas: BTreeMap<i32, Arc<BasDefinition>>,
}

impl VarHost for SmallHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        Ok(&mut self.world)
    }

    fn bas_definition(&self, id: i32) -> Result<Option<Arc<BasDefinition>>, HostError> {
        Ok(self.bas.get(&id).cloned())
    }
}

fn number(text: &str) -> i32 {
    text.parse().unwrap()
}

fn numbers(text: &str) -> Vec<i32> {
    if text == "-" {
        return Vec::new();
    }
    text.split(',').map(|v| v.parse().unwrap()).collect()
}

fn run(
    opcode: Opcode,
    ints: &[i32],
    host: &mut dyn VarHost,
) -> (Vm, Result<(), VmError>) {
    let mut code: Vec<_> = ints.iter().map(|v| constant(*v)).collect();
    code.push(int(opcode, 0));
    code.push(ret());
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), host)
        .map(|_| ());
    (vm, result)
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::ArrayIndex(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

fn expected_stack(result: &str, text: &str) -> Vec<i32> {
    let mut stack = numbers(text);
    if result != "ok" {
        while stack.last() == Some(&STALE_INT) {
            stack.pop();
        }
    }
    stack
}

fn parse_optional(text: &str) -> Option<Vec<i32>> {
    match text {
        "~" => None,
        "-" => Some(Vec::new()),
        other => Some(numbers(other)),
    }
}

fn box_text(state: Option<MessageBoxState>) -> String {
    let Some(b) = state else {
        return "-,-,-7,-7,-7,-7,-7,-7,-7,-7,-7".to_string();
    };
    [
        b.anchor_x,
        b.anchor_y,
        b.padding_left,
        b.padding_top,
        b.minimal_text_width,
        b.minimal_text_height,
        b.outline_corner_sprite,
        b.outline_straight_sprite,
        b.filled_background_sprite,
        b.text_color,
        b.box_id,
    ]
    .iter()
    .map(i32::to_string)
    .collect::<Vec<_>>()
    .join(",")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn small_ops_match_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/small-ops.native.tsv");
    let mut host = SmallHost::default();
    let mut total = 0;
    let mut expected_total = 0;
    let mut counts_by_op: BTreeMap<&str, (usize, usize)> = BTreeMap::new();
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "D" => {
                let parts: Vec<&str> = c[2].split(';').collect();
                host.bas.insert(
                    number(c[1]),
                    Arc::new(BasDefinition {
                        stand_sequence: number(parts[0]),
                        random_stand_sequences: parse_optional(parts[1]),
                        random_stand_chances: parse_optional(parts[2]),
                    }),
                );
            }
            "J" => {
                host.world.platform.java_build = number(c[1]);
                host.world.platform.java_update = number(c[2]);
                let (vm, result) = run(opcode_named("JAVA_VERSION_SUPPORTED"), &[], &mut host);
                assert!(result.is_ok() && c[3] == "ok", "{line}");
                assert_eq!(vm.int_stack(), numbers(c[4]).as_slice(), "{line}");
                counts_by_op.entry("J").or_default().0 += 1;
                total += 1;
            }
            "M" => {
                let given = numbers(c[1]);
                host.world.platform.message_box = None;
                let (vm, result) = run(opcode_named("SETUP_MESSAGEBOX"), &given, &mut host);
                match (&result, c[2]) {
                    (Ok(_), "ok") => counts_by_op.entry("M").or_default().0 += 1,
                    (Err(error), expected) if class_matches(&expected["error:".len().min(expected.len())..], &error.kind) => {
                        counts_by_op.entry("M").or_default().1 += 1
                    }
                    other => panic!("{line}: {other:?}"),
                }
                assert_eq!(vm.int_stack(), expected_stack(c[2], c[3]).as_slice(), "{line}");
                assert_eq!(box_text(host.world.platform.message_box), c[4], "{line}");
                total += 1;
            }
            "B" => {
                let (vm, result) = run(opcode_named("BAS_GETANIM_READY"), &[number(c[1])], &mut host);
                match (&result, c[2]) {
                    (Ok(_), "ok") => counts_by_op.entry("B").or_default().0 += 1,
                    (Err(error), expected) if expected.starts_with("error:") && class_matches(&expected["error:".len()..], &error.kind) => {
                        counts_by_op.entry("B").or_default().1 += 1
                    }
                    other => panic!("{line}: {other:?}"),
                }
                assert_eq!(vm.int_stack(), expected_stack(c[2], c[3]).as_slice(), "{line}");
                total += 1;
            }
            "N" => {
                let (vm, result) = run(opcode_named("NOTIFY_ACCOUNTCREATESTARTED"), &[9, 8], &mut host);
                assert!(result.is_ok(), "{line}");
                assert_eq!(vm.int_stack(), numbers(c[1]).as_slice(), "{line}");
                total += 1;
            }
            "Z" => expected_total = c[1].parse::<usize>().unwrap(),
            other => panic!("{other}"),
        }
    }
    assert_eq!(total, expected_total);
    assert!(counts_by_op["B"].0 > 100 && counts_by_op["B"].1 > 40);
    assert!(counts_by_op["M"].0 > 100 && counts_by_op["M"].1 > 100);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn small_ops_without_a_host_state() {
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
    for (name, ints) in [
        ("JAVA_VERSION_SUPPORTED", vec![]),
        ("PROFILE_CPU", vec![]),
        ("BAS_GETANIM_READY", vec![1]),
    ] {
        let (_, result) = run(opcode_named(name), &ints, &mut Bare);
        assert!(matches!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(_)
        ));
    }
    let (_, result) = run(opcode_named("NOTIFY_ACCOUNTCREATESTARTED"), &[], &mut Bare);
    assert!(result.is_ok());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn window_mode_ops_follow_the_kotlin_range_guard() {
    let mut host = SmallHost::default();
    for mode in [-1, 0, 1, 2, 3, 99] {
        let (vm, result) = run(opcode_named("SETWINDOWMODE"), &[mode], &mut host);
        assert!(result.is_ok() && vm.int_stack().is_empty());
        let (_, result) = run(opcode_named("SETDEFAULTWINDOWMODE"), &[mode], &mut host);
        assert!(result.is_ok());
    }
    assert_eq!(host.world.platform.window_mode_requests, vec![1, 2]);
    assert_eq!(host.world.platform.fixed_screen, 2);
    assert_eq!(host.world.platform.full_screen_preference, 2);
    assert_eq!(host.world.platform.preference_saves, 2);
}
