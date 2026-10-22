#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn arrays(value: &str) -> Vec<Vec<i32>> {
    value.split('|').map(integers).collect()
}

fn define(code: &mut Vec<(Opcode, Operand)>, slot: usize, values: &[i32]) {
    code.extend([
        constant(values.len() as i32),
        int(Opcode::DEFINE_ARRAY, (slot as i32) << 16 | 105),
    ]);
    for (index, value) in values.iter().enumerate() {
        code.extend([
            constant(index as i32),
            constant(*value),
            int(Opcode::POP_ARRAY_INT, slot as i32),
        ]);
    }
}

fn native_error(error: &VmErrorKind, stack_length: usize) -> String {
    let (index, length) = match error {
        VmErrorKind::StackUnderflow(StackKind::Int) => (stack_length as i32 - 3, 1000),
        VmErrorKind::ArraySlot(slot) => (*slot, 5),
        VmErrorKind::ArrayIndex(index) => (*index, 5000),
        VmErrorKind::ArrayLength(_) | VmErrorKind::ArrayAlias(_) => {
            return "error:RuntimeException:-".into();
        }
        other => panic!("unexpected ARRAY_SORT error {other:?}"),
    };
    format!("error:ArrayIndexOutOfBoundsException:Index {index} out of bounds for length {length}")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn paired_sort_matches_every_native_result_error_and_mutation() {
    let mut cases = 0;
    let mut stress = 0;
    for line in include_str!("fixtures/array-sort.native.tsv").lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 10);
        let name = fields[0];
        let lengths = integers(fields[4]);
        let initial = arrays(fields[5]);
        let stack = integers(fields[6]);
        let expected_stack = integers(fields[8]);
        let expected_arrays = arrays(fields[9]);
        assert_eq!(initial.len(), 5);
        assert_eq!(expected_arrays.len(), 5);
        let mut init = Vec::new();
        for (slot, values) in initial.iter().enumerate() {
            assert_eq!(lengths[slot] as usize, values.len());
            define(&mut init, slot, values);
        }
        init.push(ret());
        let mut code: Vec<_> = stack.iter().map(|value| constant(*value)).collect();
        code.extend([int(Opcode::ARRAY_SORT, 255), ret()]);
        let repository = BTreeMap::from([(1, script(&init)), (2, script(&code))]);
        let mut vm = Vm::default();
        vm.run(1, &repository, Arguments::default()).unwrap();
        let result = vm.run(2, &repository, Arguments::default());
        let actual = match &result {
            Ok(execution) => {
                assert_eq!(execution.instructions_executed, stack.len() as u64 + 2);
                assert_eq!(execution.joined_string_bytes, 0);
                "ok".to_owned()
            }
            Err(error) => {
                assert_eq!(error.script_id, 2);
                assert_eq!(error.instruction, stack.len());
                native_error(&error.kind, stack.len())
            }
        };
        assert_eq!(actual, fields[7], "{name}");
        assert_eq!(vm.int_stack(), expected_stack, "{name}");
        for (slot, expected) in expected_arrays.iter().enumerate() {
            assert_eq!(
                vm.array(slot),
                Some(expected.as_slice()),
                "{name} slot {slot}"
            );
        }
        assert!(vm.string_stack().is_empty());
        assert!(vm.long_stack().is_empty());
        cases += 1;
        if name.starts_with("stress-") {
            assert_eq!(lengths[0], 5000);
            assert_eq!(lengths[4], 5000);
            stress += 1;
        }
    }
    assert_eq!(cases, 349);
    assert_eq!(stress, 8);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sort_validation_order_is_typed_and_does_not_mutate_arrays() {
    let mut init = Vec::new();
    define(&mut init, 0, &[3, 2, 1]);
    define(&mut init, 4, &[30, 20]);
    init.push(ret());
    for (count, key, value, expected) in [
        (4, -1, 5, VmErrorKind::ArraySlot(-1)),
        (4, 0, -1, VmErrorKind::ArrayLength(4)),
        (3, 0, -1, VmErrorKind::ArraySlot(-1)),
        (3, 0, 4, VmErrorKind::ArrayLength(3)),
        (4, 0, 0, VmErrorKind::ArrayLength(4)),
        (3, 0, 0, VmErrorKind::ArrayAlias(0)),
        (0, 0, 0, VmErrorKind::ArrayAlias(0)),
        (-1, 0, 0, VmErrorKind::ArrayAlias(0)),
        (i32::MIN, 0, 0, VmErrorKind::ArrayAlias(0)),
        (i32::MIN, -1, 4, VmErrorKind::ArraySlot(-1)),
        (i32::MIN, 0, 5, VmErrorKind::ArraySlot(5)),
        (i32::MIN, 0, 4, VmErrorKind::ArrayIndex(1_073_741_823)),
        (i32::MAX, 0, 4, VmErrorKind::ArrayLength(i32::MAX)),
    ] {
        let code = [
            constant(727),
            constant(count),
            constant(key),
            constant(value),
            int(Opcode::ARRAY_SORT, 0),
            ret(),
        ];
        let repository = BTreeMap::from([(1, script(&init)), (2, script(&code))]);
        let mut vm = Vm::default();
        vm.run(1, &repository, Arguments::default()).unwrap();
        assert_eq!(
            vm.run(2, &repository, Arguments::default())
                .unwrap_err()
                .kind,
            expected,
            "{count} {key} {value}"
        );
        assert_eq!(vm.int_stack(), &[727]);
        assert_eq!(vm.array(0), Some(&[3, 2, 1][..]));
        assert_eq!(vm.array(4), Some(&[30, 20][..]));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn array_sort_underflow_retains_every_available_operand() {
    for length in 0..3 {
        let mut code = vec![constant(5); length];
        code.extend([int(Opcode::ARRAY_SORT, 0), ret()]);
        let repository = BTreeMap::from([(1, script(&code))]);
        let mut vm = Vm::default();
        assert_eq!(
            vm.run(1, &repository, Arguments::default())
                .unwrap_err()
                .kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
        assert_eq!(vm.int_stack(), vec![5; length]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sorted_arrays_persist_between_scripts_and_reset_only_explicitly() {
    let mut init = Vec::new();
    define(&mut init, 3, &[3, 1, 2, -999]);
    define(&mut init, 1, &[30, 10, 20, 888, 777]);
    init.push(ret());
    let sort = script(&[
        constant(3),
        constant(3),
        constant(1),
        int(Opcode::ARRAY_SORT, 255),
        ret(),
    ]);
    let read = script(&[
        constant(0),
        int(Opcode::PUSH_ARRAY_INT, 3),
        constant(0),
        int(Opcode::PUSH_ARRAY_INT, 1),
        ret(),
    ]);
    let repository = BTreeMap::from([(1, script(&init)), (2, sort), (3, read)]);
    let mut vm = Vm::default();
    vm.run(1, &repository, Arguments::default()).unwrap();
    assert_eq!(
        vm.run(2, &repository, Arguments::default())
            .unwrap()
            .instructions_executed,
        5
    );
    assert_eq!(vm.array(3), Some(&[1, 2, 3, -999][..]));
    assert_eq!(vm.array(1), Some(&[10, 20, 30, 888, 777][..]));
    vm.run(3, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.int_stack(), &[1, 10]);
    assert_eq!(vm.array(3), Some(&[1, 2, 3, -999][..]));
    vm.reset();
    for slot in 0..5 {
        assert_eq!(vm.array(slot), Some(&[][..]));
    }
}
