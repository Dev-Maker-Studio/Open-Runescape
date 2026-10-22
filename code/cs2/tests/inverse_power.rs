#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

fn repository(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(1, script(code))])
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn operation(base: i32, root: i32, operand: i32) -> [(Opcode, Operand); 4] {
    [
        constant(base),
        constant(root),
        int(Opcode::INVPOW, operand),
        ret(),
    ]
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inverse_power_metadata_keeps_native_name_order_wire_family_and_operand_flag() {
    assert_eq!(INVERSE_POWER_OPERATIONS, &[(Opcode::INVPOW, 2)]);
    let entry = Opcode::INVPOW.metadata();
    assert_eq!(entry.name, "INVPOW");
    assert_eq!(entry.ordinal, 547);
    assert_eq!(entry.opcode.wire(), 776);
    assert_eq!(entry.family, 10);
    assert!(!entry.has_int_constant);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_positive_int_perfect_power_boundaries_and_neighbors_match_actual_native_floating_results() {
    let payload = include_bytes!("fixtures/inverse-power-boundaries.native.bin");
    let word = |bytes: &[u8]| i32::from_be_bytes(bytes.try_into().unwrap());
    assert_eq!(word(&payload[..4]), 0x727c5215);
    assert_eq!(word(&payload[4..8]), 48036);
    assert_eq!(payload.len(), 8 + 48036 * 24);
    let mut counts = [0usize; 31];
    let mut last = (2, 1);
    let mut corrections = Vec::new();
    let mut vm = Vm::new(VmLimits {
        max_instructions: 5,
        max_string_bytes: 0,
        max_join_bytes: 0,
        ..VmLimits::default()
    });
    for row in payload[8..].chunks_exact(24) {
        let fields: Vec<_> = row.chunks_exact(4).map(word).collect();
        let (root, exact_root, base) = (fields[0], fields[1], fields[2]);
        assert!((2..=30).contains(&root));
        assert_eq!(i64::from(exact_root).pow(root as u32), i64::from(base));
        if last.0 == root {
            assert_eq!(exact_root, last.1 + 1);
        } else {
            assert_eq!(root, last.0 + 1);
            assert_eq!(exact_root, 2);
            assert!(i64::from(last.1 + 1).pow(last.0 as u32) > i64::from(i32::MAX));
        }
        last = (root, exact_root);
        counts[root as usize] += 1;
        assert_eq!(fields[3], exact_root - 1);
        assert!(fields[4] == exact_root || fields[4] == exact_root - 1);
        assert_eq!(fields[5], exact_root);
        if fields[4] == exact_root - 1 {
            corrections.push((root, base));
        }
        for (offset, expected) in [-1, 0, 1].into_iter().zip(&fields[3..]) {
            let mut code = vec![constant(727)];
            code.extend(operation(base + offset, root, 255));
            let execution = vm.run(1, &repository(&code), Arguments::default()).unwrap();
            assert_eq!(execution.instructions_executed, 5);
            assert_eq!(execution.joined_string_bytes, 0);
            assert_eq!(
                vm.int_stack(),
                &[727, *expected],
                "base={} root={root}",
                base + offset
            );
            assert!(vm.string_stack().is_empty());
            assert!(vm.long_stack().is_empty());
        }
    }
    assert!(i64::from(last.1 + 1).pow(last.0 as u32) > i64::from(i32::MAX));
    assert_eq!(
        &counts[2..],
        &[
            46339, 1289, 214, 72, 34, 20, 13, 9, 7, 6, 4, 4, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1
        ]
    );
    let table: &[(i32, i32)] = include!("../src/inverse_power_round_down.rs");
    assert_eq!(corrections.len(), 1347);
    assert_eq!(corrections, table);
    assert!(table.windows(2).all(|pair| pair[0] < pair[1]));
    for slot in 0..5 {
        assert_eq!(vm.array(slot), Some(&[][..]));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_domain_vectors_preserve_stack_replacement_sentinels_and_safe_underflow() {
    let mut counts = [0usize; 5];
    let mut bytes = [false; 256];
    let mut total = 0;
    for line in include_str!("fixtures/inverse-power.native-cases.tsv").lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 6);
        let name = fields[0];
        let operand: i32 = fields[1].parse().unwrap();
        let input = integers(fields[2]);
        let pointer: i32 = fields[4].parse().unwrap();
        let expected = integers(fields[5]);
        bytes[operand as usize] = true;
        let mut code = vec![text("preserved"), long(i64::MIN + 727)];
        code.extend(input.iter().map(|n| constant(*n)));
        code.extend([int(Opcode::INVPOW, operand), ret()]);
        let mut vm = Vm::default();
        let result = vm.run(1, &repository(&code), Arguments::default());
        if input.len() < 2 {
            assert_eq!(pointer, input.len() as i32 - 2, "{name}");
            assert_eq!(fields[3], format!("error:ArrayIndexOutOfBoundsException:Index {pointer} out of bounds for length 1000"));
            assert!(expected.is_empty());
            assert_eq!(
                result.unwrap_err(),
                VmError {
                    script_id: 1,
                    instruction: input.len() + 2,
                    kind: VmErrorKind::StackUnderflow(StackKind::Int),
                }
            );
            assert_eq!(vm.int_stack(), input, "{name}");
            counts[0] += 1;
        } else {
            assert_eq!(fields[3], "ok", "{name}");
            assert_eq!(pointer as usize, input.len() - 1);
            assert_eq!(expected.len(), pointer as usize);
            assert_eq!(&expected[..expected.len() - 1], &input[..input.len() - 2]);
            let execution = result.unwrap();
            assert_eq!(execution.instructions_executed, input.len() as u64 + 4);
            assert_eq!(execution.joined_string_bytes, 0);
            assert_eq!(vm.int_stack(), expected, "{name}");
        }
        assert_eq!(vm.string_stack(), &[Some(Arc::<str>::from("preserved"))]);
        assert_eq!(vm.long_stack(), &[i64::MIN + 727]);
        for slot in 0..5 {
            assert_eq!(vm.array(slot), Some(&[][..]));
        }
        counts[1] += usize::from(input.len() == 1000);
        counts[2] += usize::from(name.starts_with("random-wide-"));
        counts[3] += usize::from(name.starts_with("random-positive-"));
        counts[4] += usize::from(name.starts_with("byte-"));
        total += 1;
    }
    assert_eq!(total, 22362);
    assert_eq!(counts, [4, 32, 4096, 4096, 256]);
    assert!(bytes.into_iter().all(|present| present));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inverse_power_preserves_zero_precedence_nan_identity_reciprocals_and_native_round_down() {
    for (base, root, expected) in [
        (0, 0, 0),
        (0, i32::MIN, 0),
        (0, i32::MAX, 0),
        (i32::MIN, 0, i32::MAX),
        (i32::MAX, 0, i32::MAX),
        (-1, 0, i32::MAX),
        (i32::MIN, 1, i32::MIN),
        (i32::MAX, 1, i32::MAX),
        (-64, 1, -64),
        (-1, -1, -1),
        (-2, -1, 0),
        (i32::MIN, -1, 0),
        (-1, 2, 0),
        (-8, 3, 0),
        (-64, 3, 0),
        (-8, -3, 0),
        (1, i32::MIN, 1),
        (1, i32::MAX, 1),
        (2, i32::MIN, 0),
        (2, i32::MAX, 1),
        (i32::MAX, 31, 1),
        (i32::MAX, 30, 2),
        (i32::MAX, 2, 46340),
        (2147395600, 2, 46340),
        (63, 3, 3),
        (64, 3, 3),
        (65, 3, 4),
        (125, 3, 4),
        (1000, 3, 9),
        (64, 2, 8),
        (64, 6, 2),
        (65, 6, 2),
    ] {
        let mut vm = Vm::new(VmLimits {
            max_instructions: 4,
            ..VmLimits::default()
        });
        let result = vm
            .run(
                1,
                &repository(&operation(base, root, 255)),
                Arguments::default(),
            )
            .unwrap();
        assert_eq!(result.instructions_executed, 4);
        assert_eq!(vm.int_stack(), &[expected], "{base} {root}");
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inverse_power_obeys_exact_fuel_on_full_stack_without_string_allocation() {
    for (base, root, expected) in [
        (64, 3, 3),
        (0, 0, 0),
        (i32::MIN, 1, i32::MIN),
        (i32::MAX, 0, i32::MAX),
        (i32::MAX, 31, 1),
        (-1, -1, -1),
    ] {
        let mut input = vec![727; 998];
        input.extend([base, root]);
        let mut output = vec![727; 998];
        output.push(expected);
        let mut code: Vec<_> = input.iter().map(|n| constant(*n)).collect();
        code.extend([int(Opcode::INVPOW, 255), ret()]);
        let scripts = repository(&code);
        for max_instructions in [1000, 1001, 1002] {
            let mut vm = Vm::new(VmLimits {
                max_instructions,
                max_string_bytes: 0,
                max_join_bytes: 0,
                ..VmLimits::default()
            });
            let result = vm.run(1, &scripts, Arguments::default());
            if max_instructions < 1002 {
                assert_eq!(
                    result.unwrap_err(),
                    VmError {
                        script_id: 1,
                        instruction: max_instructions as usize,
                        kind: VmErrorKind::InstructionLimit
                    }
                );
            } else {
                let execution = result.unwrap();
                assert_eq!(execution.instructions_executed, 1002);
                assert_eq!(execution.joined_string_bytes, 0);
            }
            assert_eq!(
                vm.int_stack(),
                if max_instructions == 1000 {
                    &input
                } else {
                    &output
                }
            );
            assert!(vm.string_stack().is_empty());
            assert!(vm.long_stack().is_empty());
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_inverse_power_byte_operand_is_ignored_in_actual_decoded_bytecode() {
    for operand in 0..=255 {
        for (base, root, expected) in [
            (64, 3, 3),
            (i32::MIN, 1, i32::MIN),
            (0, 0, 0),
            (1, 0, i32::MAX),
        ] {
            let script = Script::decode(&binary(
                &operation(base, root, operand),
                zero(),
                zero(),
                &[],
            ))
            .unwrap();
            assert_eq!(script.instructions()[2].operand, Operand::Int(operand));
            let mut vm = Vm::default();
            let result = vm
                .run(1, &BTreeMap::from([(1, script)]), Arguments::default())
                .unwrap();
            assert_eq!(result.instructions_executed, 4);
            assert_eq!(vm.int_stack(), &[expected]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn decoded_parameterized_child_preserves_all_caller_types_and_exact_callee_fuel_locations() {
    let child = Script::decode(include_bytes!("fixtures/vm-inverse-power-child.bin")).unwrap();
    assert_eq!(child.locals(), counts(3, 1, 1));
    assert_eq!(child.arguments(), counts(2, 1, 1));
    let root = [
        constant(123),
        int(Opcode::POP_INT_LOCAL, 0),
        text("caller"),
        long(17),
        constant(727),
        constant(64),
        constant(3),
        text("child"),
        long(19),
        int(Opcode::GOSUB_WITH_PARAMS, 900776),
        int(Opcode::PUSH_INT_LOCAL, 0),
        ret(),
    ];
    let root = Script::decode(&binary(&root, counts(1, 0, 0), zero(), &[])).unwrap();
    let scripts = BTreeMap::from([(1, root), (900776, child)]);
    for (fuel, expected, location) in [
        (12, vec![727, 64, 3], Some(2)),
        (13, vec![727, 3], Some(3)),
        (20, vec![727, 3, 123], None),
    ] {
        let mut vm = Vm::new(VmLimits {
            max_instructions: fuel,
            ..VmLimits::default()
        });
        let result = vm.run(1, &scripts, Arguments::default());
        if let Some(instruction) = location {
            assert_eq!(
                result.unwrap_err(),
                VmError {
                    script_id: 900776,
                    instruction,
                    kind: VmErrorKind::InstructionLimit
                }
            );
            assert_eq!(vm.string_stack(), &[Some(Arc::<str>::from("caller"))]);
            assert_eq!(vm.long_stack(), &[17]);
        } else {
            let execution = result.unwrap();
            assert_eq!(execution.instructions_executed, 20);
            assert_eq!(execution.locals.ints, &[123]);
            assert_eq!(
                vm.string_stack(),
                &[
                    Some(Arc::<str>::from("caller")),
                    Some(Arc::<str>::from("child"))
                ]
            );
            assert_eq!(vm.long_stack(), &[17, 19]);
        }
        assert_eq!(vm.int_stack(), expected);
    }
    let child = Script::decode(&binary(
        &[
            int(Opcode::PUSH_INT_LOCAL, 0),
            int(Opcode::INVPOW, 0),
            ret(),
        ],
        counts(2, 0, 0),
        counts(2, 0, 0),
        &[],
    ))
    .unwrap();
    let scripts = BTreeMap::from([
        (
            1,
            script(&[
                constant(64),
                constant(3),
                int(Opcode::GOSUB_WITH_PARAMS, 900776),
                ret(),
            ]),
        ),
        (900776, child),
    ]);
    let mut vm = Vm::default();
    assert_eq!(
        vm.run(1, &scripts, Arguments::default()).unwrap_err(),
        VmError {
            script_id: 900776,
            instruction: 1,
            kind: VmErrorKind::StackUnderflow(StackKind::Int)
        }
    );
    assert_eq!(vm.int_stack(), &[64]);
}

struct RejectHost;

impl VarHost for RejectHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        panic!("pure inverse power read host")
    }
    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        panic!("pure inverse power wrote host")
    }
    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        panic!("pure inverse power looked up host")
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inverse_power_is_host_independent() {
    for (values, fuel) in [(&[64, 3][..], 4), (&[64][..], 4), (&[64, 3][..], 2)] {
        let mut code: Vec<_> = values.iter().map(|n| constant(*n)).collect();
        code.extend([int(Opcode::INVPOW, 255), ret()]);
        let scripts = repository(&code);
        let limits = VmLimits {
            max_instructions: fuel,
            ..VmLimits::default()
        };
        let mut pure = Vm::new(limits);
        let mut hosted = Vm::new(limits);
        assert_eq!(
            pure.run(1, &scripts, Arguments::default()),
            hosted.run_with_host(1, &scripts, Arguments::default(), &mut RejectHost)
        );
        assert_eq!(pure.int_stack(), hosted.int_stack());
    }
}
