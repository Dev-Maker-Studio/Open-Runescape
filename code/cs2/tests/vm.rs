#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, fmt::Write, sync::Arc};

fn repository(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(1, script(code))])
}
fn run(code: &[(Opcode, Operand)]) -> (Vm, Execution) {
    let repository = repository(code);
    let mut vm = Vm::default();
    let execution = vm.run(1, &repository, Arguments::default()).unwrap();
    (vm, execution)
}
fn fail(code: &[(Opcode, Operand)]) -> VmErrorKind {
    Vm::default()
        .run(1, &repository(code), Arguments::default())
        .unwrap_err()
        .kind
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn pure_execution_matches_the_actual_kotlin_vm() {
    for (name, bytes, expected) in EXECUTION {
        let repository = BTreeMap::from([
            (1, Script::decode(bytes).unwrap()),
            (
                900002,
                Script::decode(include_bytes!("fixtures/vm-child.bin")).unwrap(),
            ),
            (
                900087,
                Script::decode(include_bytes!("fixtures/vm-sort-child.bin")).unwrap(),
            ),
            (
                900048,
                Script::decode(include_bytes!("fixtures/vm-coordinates-child.bin")).unwrap(),
            ),
            (
                900776,
                Script::decode(include_bytes!("fixtures/vm-inverse-power-child.bin")).unwrap(),
            ),
        ]);
        let mut vm = Vm::default();
        let execution = vm.run(1, &repository, Arguments::default()).unwrap();
        let mut actual = format!("instructions\t{}\n", execution.instructions_executed);
        for value in vm.int_stack() {
            writeln!(actual, "int\t{value}").unwrap();
        }
        for value in vm.string_stack() {
            writeln!(actual, "string\t{}", string(value.as_deref())).unwrap();
        }
        for value in vm.long_stack() {
            writeln!(actual, "long\t{value}").unwrap();
        }
        for value in &execution.locals.ints {
            writeln!(actual, "local-int\t{value}").unwrap();
        }
        for value in &execution.locals.strings {
            writeln!(actual, "local-string\t{}", string(value.as_deref())).unwrap();
        }
        for value in &execution.locals.longs {
            writeln!(actual, "local-long\t{value}").unwrap();
        }
        for slot in 0..5 {
            let values = vm.array(slot).unwrap();
            write!(actual, "array\t{slot}\t{}", values.len()).unwrap();
            for value in values {
                write!(actual, "\t{value}").unwrap();
            }
            actual.push('\n');
        }
        assert_eq!(&actual, expected, "{name}");
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exactly_81_pure_operations_are_supported_without_silent_fallbacks() {
    use Opcode::*;
    let supported = [
        PUSH_CONSTANT_INT,
        PUSH_CONSTANT_STRING,
        PUSH_LONG_CONSTANT,
        PUSH_INT_LOCAL,
        POP_INT_LOCAL,
        PUSH_STRING_LOCAL,
        POP_STRING_LOCAL,
        PUSH_LONG_LOCAL,
        POP_LONG_LOCAL,
        POP_INT_DISCARD,
        POP_STRING_DISCARD,
        POP_LONG_DISCARD,
        JOIN_STRING,
        BRANCH,
        BRANCH_NOT,
        BRANCH_EQUALS,
        BRANCH_LESS_THAN,
        BRANCH_GREATER_THAN,
        BRANCH_LESS_THAN_OR_EQUALS,
        BRANCH_GREATER_THAN_OR_EQUALS,
        LONG_BRANCH_NOT,
        LONG_BRANCH_EQUALS,
        LONG_BRANCH_LESS_THAN,
        LONG_BRANCH_GREATER_THAN,
        LONG_BRANCH_LESS_THAN_OR_EQUALS,
        LONG_BRANCH_GREATER_THAN_OR_EQUALS,
        BRANCH_IF_TRUE,
        BRANCH_IF_FALSE,
        SWITCH,
        GOSUB_WITH_PARAMS,
        RETURN,
        DEFINE_ARRAY,
        PUSH_ARRAY_INT,
        POP_ARRAY_INT,
    ];
    assert_eq!(supported.len(), 34);
    assert!(supported.iter().all(|opcode| opcode.metadata().family == 0));
    assert_eq!(NUMERIC_OPERATIONS.len(), 17);
    assert!(NUMERIC_OPERATIONS
        .iter()
        .all(|(opcode, _)| opcode.metadata().family == 10));
    assert_eq!(TEXT_OPERATIONS.len(), 20);
    assert!(TEXT_OPERATIONS
        .iter()
        .all(|opcode| opcode.metadata().family == 10));
    assert_eq!(ARRAY_SORT.wire(), 87);
    assert_eq!(ARRAY_SORT.metadata().family, 15);
    assert!(!ARRAY_SORT.metadata().has_int_constant);
    let supported: Vec<_> = supported
        .into_iter()
        .chain(NUMERIC_OPERATIONS.iter().map(|(opcode, _)| *opcode))
        .chain(TEXT_OPERATIONS.iter().copied())
        .chain([ARRAY_SORT])
        .collect();
    assert_eq!(supported.len(), 72);
    assert_eq!(COORDINATE_OPERATIONS.len(), 4);
    assert!(COORDINATE_OPERATIONS
        .iter()
        .all(|(opcode, _)| opcode.metadata().family == 7));
    let supported: Vec<_> = supported
        .into_iter()
        .chain(COORDINATE_OPERATIONS.iter().map(|(opcode, _)| *opcode))
        .collect();
    assert_eq!(supported.len(), 76);
    assert_eq!(INVERSE_POWER_OPERATIONS.len(), 1);
    assert!(INVERSE_POWER_OPERATIONS
        .iter()
        .all(|(opcode, _)| opcode.metadata().family == 10));
    let supported: Vec<_> = supported
        .into_iter()
        .chain(INVERSE_POWER_OPERATIONS.iter().map(|(opcode, _)| *opcode))
        .chain([
            LOWERCASE,
            DATE_ISLEAPYEAR,
            DATE_MINUTES_FROMRUNEDAY,
            DATE_RUNEDAY_FROMDATE,
        ])
        .collect();
    assert_eq!(supported.len(), 81);
    assert_eq!(
        supported
            .iter()
            .map(|opcode| opcode.wire())
            .collect::<std::collections::BTreeSet<_>>()
            .len(),
        supported.len()
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_signed_int_and_long_comparison_preserves_operand_order() {
    use Opcode::*;
    let ops = [
        (BRANCH_NOT, LONG_BRANCH_NOT),
        (BRANCH_EQUALS, LONG_BRANCH_EQUALS),
        (BRANCH_LESS_THAN, LONG_BRANCH_LESS_THAN),
        (BRANCH_GREATER_THAN, LONG_BRANCH_GREATER_THAN),
        (BRANCH_LESS_THAN_OR_EQUALS, LONG_BRANCH_LESS_THAN_OR_EQUALS),
        (
            BRANCH_GREATER_THAN_OR_EQUALS,
            LONG_BRANCH_GREATER_THAN_OR_EQUALS,
        ),
    ];
    for (index, (integer, wide)) in ops.into_iter().enumerate() {
        for (left, right) in [
            (i64::MIN, i64::MIN),
            (i64::MIN, i64::MAX),
            (i64::MAX, i64::MIN),
        ] {
            let expected = [
                left != right,
                left == right,
                left < right,
                left > right,
                left <= right,
                left >= right,
            ][index];
            let code = [
                long(left),
                long(right),
                int(wide, 2),
                constant(0),
                int(BRANCH, 1),
                constant(1),
                ret(),
            ];
            assert_eq!(run(&code).0.int_stack(), &[i32::from(expected)]);
        }
        for (left, right) in [
            (i32::MIN, i32::MIN),
            (i32::MIN, i32::MAX),
            (i32::MAX, i32::MIN),
        ] {
            let expected = [
                left != right,
                left == right,
                left < right,
                left > right,
                left <= right,
                left >= right,
            ][index];
            let code = [
                constant(left),
                constant(right),
                int(integer, 2),
                constant(0),
                int(BRANCH, 1),
                constant(1),
                ret(),
            ];
            assert_eq!(run(&code).0.int_stack(), &[i32::from(expected)]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn boolean_branches_test_exact_zero_or_one() {
    for (opcode, expected) in [(Opcode::BRANCH_IF_TRUE, 1), (Opcode::BRANCH_IF_FALSE, 0)] {
        for value in [i32::MIN, -1, 0, 1, 2, i32::MAX] {
            let code = [
                constant(value),
                int(opcode, 2),
                constant(0),
                int(Opcode::BRANCH, 1),
                constant(1),
                ret(),
            ];
            assert_eq!(run(&code).0.int_stack(), &[i32::from(value == expected)]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn arrays_use_logical_bounds_defaults_and_persistent_executor_storage() {
    let good = script(&[
        constant(5000),
        int(Opcode::DEFINE_ARRAY, (4 << 16) | 105),
        constant(4999),
        constant(77),
        int(Opcode::POP_ARRAY_INT, 4),
        constant(4999),
        int(Opcode::PUSH_ARRAY_INT, 4),
        ret(),
    ]);
    let minus_one = script(&[
        constant(2),
        int(Opcode::DEFINE_ARRAY, (4 << 16) | 115),
        ret(),
    ]);
    let empty = script(&[
        constant(0),
        int(Opcode::DEFINE_ARRAY, (4 << 16) | 105),
        ret(),
    ]);
    let repository = BTreeMap::from([(1, good), (2, minus_one), (3, script(&[ret()])), (4, empty)]);
    let mut vm = Vm::default();
    vm.run(1, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.int_stack(), &[77]);
    assert_eq!(vm.array(4).unwrap().len(), 5000);
    assert_eq!(vm.array(4).unwrap()[0], 0);
    vm.run(2, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.array(4), Some(&[-1, -1][..]));
    vm.run(3, &repository, Arguments::default()).unwrap();
    assert!(vm.int_stack().is_empty());
    assert_eq!(vm.array(4), Some(&[-1, -1][..]));
    vm.run(4, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.array(4), Some(&[][..]));
    assert_eq!(vm.array(5), None);
    vm.reset();
    assert_eq!(vm.array(4), Some(&[][..]));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_array_lengths_consume_the_value_without_redefining_the_array() {
    let mut vm = Vm::default();
    let init = script(&[constant(2), int(Opcode::DEFINE_ARRAY, 105), ret()]);
    for length in [-1, 5001] {
        let bad = script(&[constant(length), int(Opcode::DEFINE_ARRAY, 105), ret()]);
        let repository = BTreeMap::from([(1, init.clone()), (2, bad)]);
        vm.run(1, &repository, Arguments::default()).unwrap();
        assert_eq!(
            vm.run(2, &repository, Arguments::default())
                .unwrap_err()
                .kind,
            VmErrorKind::ArrayLength(length)
        );
        assert!(vm.int_stack().is_empty());
        assert_eq!(vm.array(0), Some(&[0, 0][..]));
    }
    for slot in [-1, 5] {
        assert_eq!(
            fail(&[
                constant(0),
                int(Opcode::DEFINE_ARRAY, slot << 16 | 105),
                ret()
            ]),
            VmErrorKind::ArraySlot(slot)
        );
        assert_eq!(
            fail(&[constant(0), int(Opcode::PUSH_ARRAY_INT, slot), ret()]),
            VmErrorKind::ArraySlot(slot)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_array_indices_pop_the_index_and_store_value_but_keep_lower_values() {
    for index in [-1, 2, 4999] {
        for store in [false, true] {
            let mut code = vec![
                constant(2),
                int(Opcode::DEFINE_ARRAY, 105),
                constant(777),
                constant(index),
            ];
            if store {
                code.push(constant(42));
            }
            code.push(int(
                if store {
                    Opcode::POP_ARRAY_INT
                } else {
                    Opcode::PUSH_ARRAY_INT
                },
                0,
            ));
            code.push(ret());
            let mut vm = Vm::default();
            assert_eq!(
                vm.run(1, &repository(&code), Arguments::default())
                    .unwrap_err()
                    .kind,
                VmErrorKind::ArrayIndex(index)
            );
            assert_eq!(vm.int_stack(), &[777]);
            assert_eq!(vm.array(0), Some(&[0, 0][..]));
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn typed_entry_arguments_populate_independent_locals_and_null_joins() {
    let code = [
        int(Opcode::PUSH_INT_LOCAL, 1),
        int(Opcode::PUSH_INT_LOCAL, 0),
        int(Opcode::PUSH_STRING_LOCAL, 0),
        int(Opcode::PUSH_STRING_LOCAL, 1),
        int(Opcode::JOIN_STRING, 2),
        int(Opcode::PUSH_LONG_LOCAL, 0),
        ret(),
    ];
    let root = Script::decode(&binary(&code, counts(3, 3, 2), counts(2, 2, 1), &[])).unwrap();
    let repository = BTreeMap::from([(1, root)]);
    let strings = [None, Some(Arc::from("€"))];
    let mut vm = Vm::default();
    let execution = vm
        .run(
            1,
            &repository,
            Arguments {
                ints: &[11, 22],
                strings: &strings,
                longs: &[i64::MIN],
            },
        )
        .unwrap();
    assert_eq!(vm.int_stack(), &[22, 11]);
    assert_eq!(vm.string_stack()[0].as_deref(), Some("null€"));
    assert_eq!(vm.long_stack(), &[i64::MIN]);
    assert_eq!(execution.locals.ints, [11, 22, 0]);
    assert_eq!(execution.locals.strings, [None, Some(Arc::from("€")), None]);
    assert_eq!(execution.locals.longs, [i64::MIN, 0]);
    assert_eq!(
        vm.run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::ArgumentCount(StackKind::Int)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn missing_scripts_and_callee_argument_underflow_are_structured_errors() {
    let repository = BTreeMap::from([(1, script(&[int(Opcode::GOSUB_WITH_PARAMS, 99), ret()]))]);
    let mut vm = Vm::default();
    assert_eq!(
        vm.run(99, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::MissingScript(99)
    );
    assert_eq!(
        vm.run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::MissingScript(99)
    );
    for (kind, counts) in [
        (StackKind::Int, counts(1, 0, 0)),
        (StackKind::String, counts(0, 1, 0)),
        (StackKind::Long, counts(0, 0, 1)),
    ] {
        let child = Script::decode(&binary(&[ret()], counts, counts, &[])).unwrap();
        let mut repository = repository.clone();
        repository.insert(99, child);
        assert_eq!(
            vm.run(1, &repository, Arguments::default())
                .unwrap_err()
                .kind,
            VmErrorKind::StackUnderflow(kind)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn call_depth_is_fifty_and_local_budget_is_reclaimed_after_return() {
    let mut repository = BTreeMap::new();
    for id in 0..50 {
        repository.insert(id, script(&[int(Opcode::GOSUB_WITH_PARAMS, id + 1), ret()]));
    }
    repository.insert(50, script(&[ret()]));
    let mut vm = Vm::default();
    let execution = vm.run(0, &repository, Arguments::default()).unwrap();
    assert_eq!(execution.instructions_executed, 101);
    repository.insert(50, script(&[int(Opcode::GOSUB_WITH_PARAMS, 51), ret()]));
    repository.insert(51, script(&[ret()]));
    let error = vm.run(0, &repository, Arguments::default()).unwrap_err();
    assert_eq!(error.kind, VmErrorKind::CallDepth);
    assert_eq!(error.script_id, 50);
    let code = [
        int(Opcode::GOSUB_WITH_PARAMS, 2),
        int(Opcode::GOSUB_WITH_PARAMS, 2),
        ret(),
    ];
    let repository = BTreeMap::from([
        (
            1,
            Script::decode(&binary(&code, counts(3, 0, 0), zero(), &[])).unwrap(),
        ),
        (
            2,
            Script::decode(&binary(&[ret()], counts(3, 0, 0), zero(), &[])).unwrap(),
        ),
    ]);
    let mut vm = Vm::new(VmLimits {
        max_local_values: 6,
        ..VmLimits::default()
    });
    vm.run(1, &repository, Arguments::default()).unwrap();
    let mut vm = Vm::new(VmLimits {
        max_local_values: 5,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::LocalLimit
    );
    let root = Script::decode(&binary(
        &[ret()],
        counts(u16::MAX, u16::MAX, u16::MAX),
        zero(),
        &[],
    ))
    .unwrap();
    assert_eq!(
        Vm::default()
            .run(1, &BTreeMap::from([(1, root)]), Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::LocalLimit
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_three_operand_stacks_enforce_exact_capacity_and_underflow() {
    for (value, kind, discard) in [
        (constant(1), StackKind::Int, Opcode::POP_INT_DISCARD),
        (text("x"), StackKind::String, Opcode::POP_STRING_DISCARD),
        (long(1), StackKind::Long, Opcode::POP_LONG_DISCARD),
    ] {
        let mut code = vec![value.clone(); 1000];
        code.push(ret());
        run(&code);
        code.insert(0, value);
        assert_eq!(fail(&code), VmErrorKind::StackOverflow(kind));
        assert_eq!(
            fail(&[int(discard, 0), ret()]),
            VmErrorKind::StackUnderflow(kind)
        );
    }
    assert_eq!(
        fail(&[constant(1), int(Opcode::BRANCH_EQUALS, 0), ret()]),
        VmErrorKind::StackUnderflow(StackKind::Int)
    );
    assert_eq!(
        fail(&[long(1), int(Opcode::LONG_BRANCH_EQUALS, 0), ret()]),
        VmErrorKind::StackUnderflow(StackKind::Long)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn instruction_budget_counts_return_and_nested_calls() {
    let mut vm = Vm::new(VmLimits {
        max_instructions: 1,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository(&[ret()]), Arguments::default())
            .unwrap()
            .instructions_executed,
        1
    );
    assert_eq!(
        vm.run(1, &repository(&[constant(1), ret()]), Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::InstructionLimit
    );
    let mut vm = Vm::new(VmLimits {
        max_instructions: 0,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository(&[ret()]), Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::InstructionLimit
    );
    let mut vm = Vm::new(VmLimits {
        max_instructions: 3,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(
            1,
            &repository(&[int(Opcode::BRANCH, -1)]),
            Arguments::default()
        )
        .unwrap_err()
        .kind,
        VmErrorKind::InstructionLimit
    );
    let repository = BTreeMap::from([
        (1, script(&[int(Opcode::GOSUB_WITH_PARAMS, 2), ret()])),
        (2, script(&[ret()])),
    ]);
    let mut vm = Vm::new(VmLimits {
        max_instructions: 2,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::InstructionLimit
    );
    assert_eq!(
        Vm::default()
            .run(1, &repository, Arguments::default())
            .unwrap()
            .instructions_executed,
        3
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bad_instruction_local_and_switch_indices_are_not_panics() {
    for offset in [i32::MIN, -2, 1, i32::MAX] {
        assert_eq!(
            fail(&[int(Opcode::BRANCH, offset), ret()]),
            VmErrorKind::InstructionPointer
        );
    }
    assert_eq!(fail(&[constant(1)]), VmErrorKind::InstructionPointer);
    assert_eq!(fail(&[]), VmErrorKind::InstructionPointer);
    for (load, kind) in [
        (Opcode::PUSH_INT_LOCAL, StackKind::Int),
        (Opcode::PUSH_STRING_LOCAL, StackKind::String),
        (Opcode::PUSH_LONG_LOCAL, StackKind::Long),
    ] {
        for index in [-1, 0, i32::MAX] {
            assert_eq!(
                fail(&[int(load, index), ret()]),
                VmErrorKind::LocalIndex(kind, index)
            );
        }
    }
    for index in [-1, 0, i32::MAX] {
        assert_eq!(
            fail(&[constant(0), int(Opcode::SWITCH, index), ret()]),
            VmErrorKind::SwitchIndex(index)
        );
    }
    let bytes = binary(
        &[constant(0), int(Opcode::SWITCH, 0), ret()],
        zero(),
        zero(),
        &[vec![]],
    );
    let repository = BTreeMap::from([(1, Script::decode(&bytes).unwrap())]);
    assert_eq!(
        Vm::default()
            .run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::EmptySwitchTable
    );
    let bytes = binary(
        &[constant(0), int(Opcode::SWITCH, 0), ret()],
        zero(),
        zero(),
        &[vec![(0, i32::MAX)]],
    );
    let repository = BTreeMap::from([(1, Script::decode(&bytes).unwrap())]);
    assert_eq!(
        Vm::default()
            .run(1, &repository, Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::InstructionPointer
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn join_string_enforces_per_value_and_cumulative_allocation_budgets() {
    let code = [text("ab"), text("cd"), int(Opcode::JOIN_STRING, 2), ret()];
    let mut vm = Vm::new(VmLimits {
        max_string_bytes: 3,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository(&code), Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::StringLimit
    );
    let code = [
        text("a"),
        text("b"),
        int(Opcode::JOIN_STRING, 2),
        int(Opcode::POP_STRING_DISCARD, 0),
        text("c"),
        text("d"),
        int(Opcode::JOIN_STRING, 2),
        ret(),
    ];
    let mut vm = Vm::new(VmLimits {
        max_join_bytes: 3,
        ..VmLimits::default()
    });
    assert_eq!(
        vm.run(1, &repository(&code), Arguments::default())
            .unwrap_err()
            .kind,
        VmErrorKind::StringLimit
    );
    assert_eq!(
        fail(&[int(Opcode::JOIN_STRING, -1), ret()]),
        VmErrorKind::InvalidOperand
    );
    assert_eq!(
        fail(&[int(Opcode::JOIN_STRING, 1), ret()]),
        VmErrorKind::StackUnderflow(StackKind::String)
    );
    let mut code = vec![text(""); 1000];
    code.push(int(Opcode::JOIN_STRING, 0));
    code.push(ret());
    assert_eq!(fail(&code), VmErrorKind::StackOverflow(StackKind::String));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn constants_and_entry_arguments_obey_the_utf8_string_limit() {
    for value in ["abcd", "é", "€"] {
        let repository = repository(&[text(value), ret()]);
        let mut vm = Vm::new(VmLimits {
            max_string_bytes: value.len() - 1,
            ..VmLimits::default()
        });
        let error = vm.run(1, &repository, Arguments::default()).unwrap_err();
        assert_eq!(error.kind, VmErrorKind::StringLimit);
        assert_eq!(error.instruction, 0);
        assert!(vm.string_stack().is_empty());
        let mut vm = Vm::new(VmLimits {
            max_string_bytes: value.len(),
            ..VmLimits::default()
        });
        vm.run(1, &repository, Arguments::default()).unwrap();
        assert_eq!(vm.string_stack()[0].as_deref(), Some(value));
    }
    let repository = BTreeMap::from([
        (
            1,
            Script::decode(&binary(&[ret()], counts(0, 1, 0), counts(0, 1, 0), &[])).unwrap(),
        ),
        (
            2,
            Script::decode(&binary(
                &[int(Opcode::PUSH_STRING_LOCAL, 0), ret()],
                counts(0, 1, 0),
                counts(0, 1, 0),
                &[],
            ))
            .unwrap(),
        ),
        (3, script(&[text(""), ret()])),
    ]);
    for value in ["abcd", "é", "€", "😀"] {
        let strings = [Some(Arc::from(value))];
        let arguments = Arguments {
            strings: &strings,
            ..Arguments::default()
        };
        let mut vm = Vm::new(VmLimits {
            max_string_bytes: value.len() - 1,
            ..VmLimits::default()
        });
        assert_eq!(
            vm.run(1, &repository, arguments).unwrap_err().kind,
            VmErrorKind::StringLimit
        );
        let mut vm = Vm::new(VmLimits {
            max_string_bytes: value.len(),
            ..VmLimits::default()
        });
        vm.run(2, &repository, arguments).unwrap();
        assert_eq!(vm.string_stack(), &strings);
    }
    let mut vm = Vm::new(VmLimits {
        max_string_bytes: 0,
        ..VmLimits::default()
    });
    for value in [None, Some(Arc::from(""))] {
        let strings = [value];
        vm.run(
            2,
            &repository,
            Arguments {
                strings: &strings,
                ..Arguments::default()
            },
        )
        .unwrap();
        assert_eq!(vm.string_stack(), &strings);
    }
    vm.run(3, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.string_stack()[0].as_deref(), Some(""));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reused_executor_keeps_long_stack_like_kotlin_until_explicit_reset() {
    let repository = BTreeMap::from([
        (1, script(&[constant(7), text("first"), long(1), ret()])),
        (2, script(&[long(2), ret()])),
    ]);
    let mut vm = Vm::default();
    vm.run(1, &repository, Arguments::default()).unwrap();
    vm.run(2, &repository, Arguments::default()).unwrap();
    assert!(vm.int_stack().is_empty());
    assert!(vm.string_stack().is_empty());
    assert_eq!(vm.long_stack(), &[1, 2]);
    vm.reset();
    vm.run(2, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.long_stack(), &[2]);
}
