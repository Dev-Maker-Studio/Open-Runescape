#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::{BTreeMap, BTreeSet};

fn program(opcode: Opcode, values: &[i32]) -> Script {
    let mut code: Vec<_> = values.iter().map(|value| constant(*value)).collect();
    code.extend([int(opcode, 255), ret()]);
    script(&code)
}

fn evaluate(opcode: Opcode, values: &[i32]) -> i32 {
    let repository = BTreeMap::from([(1, program(opcode, values))]);
    let mut vm = Vm::default();
    vm.run(1, &repository, Arguments::default()).unwrap();
    assert_eq!(vm.int_stack().len(), 1);
    vm.int_stack()[0]
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn numeric_results_and_exceptional_inputs_match_the_existing_native_jar() {
    let operations: BTreeMap<_, _> = NUMERIC_OPERATIONS
        .iter()
        .map(|(opcode, count)| (opcode.metadata().name, (*opcode, *count)))
        .collect();
    let mut covered = BTreeSet::new();
    let mut errors = BTreeSet::new();
    let mut vm = Vm::default();
    let fixture = include_str!("fixtures/numeric.native.tsv");
    for line in fixture.lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 3);
        let (opcode, count) = operations[fields[0]];
        let values: Vec<i32> = fields[1]
            .split(',')
            .map(|value| value.parse().unwrap())
            .collect();
        assert_eq!(values.len(), count);
        let mut code = vec![constant(0x12345678)];
        code.extend(values.iter().map(|value| constant(*value)));
        code.extend([int(opcode, 255), ret()]);
        let repository = BTreeMap::from([(1, script(&code))]);
        let result = vm.run(1, &repository, Arguments::default());
        covered.insert(fields[0]);
        if fields[2].starts_with("error:") {
            assert_eq!(fields[2], "error:ArithmeticException");
            assert_eq!(
                result.unwrap_err().kind,
                VmErrorKind::InvalidOperand,
                "{line}"
            );
            assert_eq!(vm.int_stack(), &[0x12345678], "{line}");
            errors.insert(fields[0]);
        } else {
            let expected: i32 = fields[2].parse().unwrap();
            let execution = result.unwrap_or_else(|error| panic!("{line}: {error}"));
            assert_eq!(execution.instructions_executed, values.len() as u64 + 3);
            assert_eq!(vm.int_stack(), &[0x12345678, expected], "{line}");
        }
    }
    assert_eq!(fixture.lines().count(), 5255);
    assert_eq!(covered, operations.keys().copied().collect());
    assert_eq!(
        errors,
        BTreeSet::from(["DIVIDE", "MODULO", "SCALE", "INTERPOLATE"])
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn wrapping_division_remainders_and_intermediate_widths_are_explicit() {
    use Opcode::*;
    for (opcode, values, expected) in [
        (ADD, [i32::MAX, 1], i32::MIN),
        (SUB, [i32::MIN, 1], i32::MAX),
        (MULTIPLY, [i32::MAX, 2], -2),
        (DIVIDE, [i32::MIN, -1], i32::MIN),
        (MODULO, [i32::MIN, -1], 0),
        (DIVIDE, [-7, 3], -2),
        (DIVIDE, [7, -3], -2),
        (MODULO, [-7, 3], -1),
        (MODULO, [7, -3], 1),
        (MIN, [i32::MIN, i32::MAX], i32::MIN),
        (MAX, [i32::MIN, i32::MAX], i32::MAX),
        (ADDPERCENT, [2_000_000_000, 100], -294_967_296),
        (ADDPERCENT, [-7, 50], -10),
    ] {
        assert_eq!(evaluate(opcode, &values), expected, "{opcode:?}");
    }
    assert_eq!(evaluate(SCALE, &[65536, 65536, 65536]), 65536);
    assert_eq!(evaluate(SCALE, &[-7, 3, 1]), -2);
    assert_eq!(evaluate(INTERPOLATE, &[0, i32::MIN, 0, -1, 1]), i32::MIN);
    assert_eq!(evaluate(INTERPOLATE, &[0, 100, 0, 10, 5]), 50);
    assert_eq!(evaluate(INTERPOLATE, &[-100, 100, 0, 10, -5]), -200);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_bit_indices_mask_negative_and_large_shift_counts() {
    for (index, bit) in [
        (i32::MIN, 0),
        (-65, 31),
        (-64, 0),
        (-33, 31),
        (-32, 0),
        (-31, 1),
        (-1, 31),
        (0, 0),
        (31, 31),
        (32, 0),
        (33, 1),
        (63, 31),
        (64, 0),
        (i32::MAX, 31),
    ] {
        let mask = (1u32 << bit) as i32;
        assert_eq!(evaluate(Opcode::SETBIT, &[0, index]), mask);
        assert_eq!(evaluate(Opcode::CLEARBIT, &[-1, index]), !mask);
        assert_eq!(evaluate(Opcode::TESTBIT, &[mask, index]), 1);
        assert_eq!(evaluate(Opcode::TESTBIT, &[!mask, index]), 0);
    }
    assert_eq!(evaluate(Opcode::AND, &[0x55aa55aa, -1]), 0x55aa55aa);
    assert_eq!(evaluate(Opcode::OR, &[i32::MIN, i32::MAX]), -1);
    assert_eq!(evaluate(Opcode::NOT, &[i32::MIN]), i32::MAX);
    let mut flags = 0;
    for bit in 3..=5 {
        flags = evaluate(Opcode::SETBIT, &[flags, bit]);
    }
    assert_eq!(flags, 0b111000);
    for bit in 3..=5 {
        flags = evaluate(Opcode::CLEARBIT, &[flags, bit]);
    }
    assert_eq!(flags, 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_numeric_operation_checks_underflow_and_preserves_other_stack_types() {
    for &(opcode, count) in NUMERIC_OPERATIONS {
        for available in 0..count {
            let mut code = vec![text("unchanged"), long(17)];
            code.extend(std::iter::repeat_n(constant(1), available));
            code.extend([int(opcode, 0), ret()]);
            let repository = BTreeMap::from([(1, script(&code))]);
            let mut vm = Vm::default();
            let error = vm.run(1, &repository, Arguments::default()).unwrap_err();
            assert_eq!(error.kind, VmErrorKind::StackUnderflow(StackKind::Int));
            assert_eq!(error.instruction, available + 2);
            assert_eq!(vm.int_stack(), vec![1; available]);
            assert_eq!(vm.string_stack()[0].as_deref(), Some("unchanged"));
            assert_eq!(vm.long_stack(), &[17]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn numeric_results_do_not_grow_a_full_stack_and_fuel_still_applies() {
    for &(opcode, count) in NUMERIC_OPERATIONS {
        let arguments = match opcode {
            Opcode::INTERPOLATE => vec![1, 2, 1, 2, 2],
            _ => vec![1; count],
        };
        let mut code = vec![constant(42); 1000 - count];
        code.extend(arguments.iter().map(|value| constant(*value)));
        code.extend([int(opcode, 0), ret()]);
        let repository = BTreeMap::from([(1, script(&code))]);
        let mut vm = Vm::default();
        vm.run(1, &repository, Arguments::default()).unwrap();
        assert_eq!(vm.int_stack().len(), 1001 - count);
        assert!(vm.int_stack()[..1000 - count].iter().all(|n| *n == 42));
        let mut vm = Vm::new(VmLimits {
            max_instructions: 1000,
            ..VmLimits::default()
        });
        assert_eq!(
            vm.run(1, &repository, Arguments::default())
                .unwrap_err()
                .kind,
            VmErrorKind::InstructionLimit
        );
        assert_eq!(vm.int_stack().len(), 1000);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn integer_power_is_bounded_and_preserves_jvm_special_cases_and_saturation() {
    for (base, exponent, expected) in [
        (0, i32::MIN, 0),
        (0, 0, 0),
        (0, i32::MAX, 0),
        (1, i32::MIN, 1),
        (-1, i32::MIN, 1),
        (-1, i32::MAX, -1),
        (2, i32::MAX, i32::MAX),
        (-2, i32::MAX, i32::MIN),
        (-2, i32::MIN, 0),
        (i32::MIN, 1, i32::MIN),
        (i32::MIN, 2, i32::MAX),
        (i32::MIN, -1, 0),
        (2, 0, 1),
        (-2, 31, i32::MIN),
        (-2, 30, 1_073_741_824),
        (2, 31, i32::MAX),
        (46340, 2, 2_147_395_600),
        (46341, 2, i32::MAX),
        (-1290, 3, -2_146_689_000),
        (-1291, 3, i32::MIN),
    ] {
        let repository = BTreeMap::from([(1, program(Opcode::POW, &[base, exponent]))]);
        let mut vm = Vm::new(VmLimits {
            max_instructions: 4,
            ..VmLimits::default()
        });
        assert_eq!(
            vm.run(1, &repository, Arguments::default())
                .unwrap()
                .instructions_executed,
            4
        );
        assert_eq!(vm.int_stack(), &[expected]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inverse_power_matches_all_preexisting_native_vectors_including_rounding_boundaries() {
    let mut covered = BTreeMap::new();
    let mut vm = Vm::default();
    for line in include_str!("fixtures/inverse-power.native.tsv").lines() {
        let values: Vec<i32> = line
            .split('\t')
            .map(|value| value.parse().unwrap())
            .collect();
        assert_eq!(values.len(), 3);
        covered.insert((values[0], values[1]), values[2]);
        let repository = BTreeMap::from([(1, program(Opcode::INVPOW, &values[..2]))]);
        let execution = vm.run(1, &repository, Arguments::default()).unwrap();
        assert_eq!(execution.instructions_executed, 4);
        assert_eq!(vm.int_stack(), &[values[2]], "{line}");
    }
    assert_eq!(covered.len(), 420);
    assert_eq!(covered[&(0, 0)], 0);
    assert_eq!(covered[&(1, 0)], i32::MAX);
    assert_eq!(covered[&(-8, 3)], 0);
    assert_eq!(covered[&(64, 3)], 3);
    assert_eq!(covered[&(125, 3)], 4);
    assert_eq!(covered[&(1000, 3)], 9);
}
