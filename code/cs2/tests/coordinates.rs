#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn repository(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(1, script(code))])
}

fn sample(opcode: Opcode) -> (&'static [i32], i32) {
    match opcode {
        Opcode::COORDX => (&[0x1234_5678], 0x08d1),
        Opcode::COORDY => (&[-1], -1),
        Opcode::COORDZ => (&[0x1234_5678], 0x1678),
        Opcode::MOVECOORD => (&[7, 2, 1, 3], 0x1000_800a),
        _ => unreachable!(),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn coordinate_metadata_preserves_native_names_ids_order_families_and_flags() {
    for ((opcode, arity), (ordinal, wire)) in
        COORDINATE_OPERATIONS
            .iter()
            .zip([(401, 93), (402, 298), (403, 536), (423, 48)])
    {
        assert_eq!(opcode.wire(), wire);
        assert_eq!(opcode.metadata().ordinal, ordinal);
        assert_eq!(opcode.metadata().family, 7);
        assert!(!opcode.metadata().has_int_constant);
        assert_eq!(*arity, if *opcode == Opcode::MOVECOORD { 4 } else { 1 });
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn coordinate_operations_match_actual_native_dispatch_results_and_record_underflow_safety() {
    let mut counts = [0; 4];
    let mut underflows = [0; 4];
    let mut full = [0; 4];
    let mut random = [0; 4];
    let mut bytes = [[false; 256]; 4];
    for line in include_str!("fixtures/coordinate.native.tsv").lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 7);
        let name = fields[0];
        let index = COORDINATE_OPERATIONS
            .iter()
            .position(|(opcode, _)| opcode.metadata().name == fields[1])
            .unwrap();
        let (opcode, arity) = COORDINATE_OPERATIONS[index];
        let operand: i32 = fields[2].parse().unwrap();
        assert!((0..=255).contains(&operand));
        let input = integers(fields[3]);
        let pointer: i32 = fields[5].parse().unwrap();
        let expected = integers(fields[6]);
        let mut code = vec![text("preserved"), long(i64::MIN + 727)];
        code.extend(input.iter().map(|n| constant(*n)));
        code.extend([int(opcode, operand), ret()]);
        let mut vm = Vm::default();
        let result = vm.run(1, &repository(&code), Arguments::default());
        if input.len() < arity {
            assert_eq!(pointer, input.len() as i32 - arity as i32, "{name}");
            assert_eq!(
                fields[4],
                format!("error:ArrayIndexOutOfBoundsException:Index {pointer} out of bounds for length 1000"),
                "{name}"
            );
            assert!(expected.is_empty(), "{name}");
            assert_eq!(
                result.unwrap_err(),
                VmError {
                    script_id: 1,
                    instruction: input.len() + 2,
                    kind: VmErrorKind::StackUnderflow(StackKind::Int),
                },
                "{name}"
            );
            assert_eq!(vm.int_stack(), input, "{name}");
            underflows[index] += 1;
        } else {
            assert_eq!(fields[4], "ok", "{name}");
            assert_eq!(pointer as usize, input.len() - arity + 1, "{name}");
            assert_eq!(expected.len(), pointer as usize, "{name}");
            let execution = result.unwrap();
            assert_eq!(
                execution.instructions_executed,
                input.len() as u64 + 4,
                "{name}"
            );
            assert_eq!(execution.joined_string_bytes, 0, "{name}");
            assert_eq!(vm.int_stack(), expected, "{name}");
        }
        assert_eq!(
            vm.string_stack(),
            &[Some(Arc::<str>::from("preserved"))],
            "{name}"
        );
        assert_eq!(vm.long_stack(), &[i64::MIN + 727], "{name}");
        for slot in 0..5 {
            assert_eq!(vm.array(slot), Some(&[][..]), "{name}");
        }
        full[index] += usize::from(input.len() == 1000);
        random[index] += usize::from(name.starts_with("random-"));
        bytes[index][operand as usize] = true;
        counts[index] += 1;
    }
    assert_eq!(counts, [2126, 2126, 2126, 25232]);
    assert_eq!(underflows, [2, 2, 2, 8]);
    assert_eq!(full, [16; 4]);
    assert_eq!(random, [1024; 4]);
    assert!(bytes.iter().flatten().all(|present| *present));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn coordinate_extraction_uses_signed_plane_and_fourteen_bit_horizontal_fields() {
    for (opcode, value, expected) in [
        (Opcode::COORDX, -1, 16383),
        (Opcode::COORDX, i32::MIN, 0),
        (Opcode::COORDX, i32::MAX, 16383),
        (Opcode::COORDX, 0x0fff_c000, 16383),
        (Opcode::COORDY, -1, -1),
        (Opcode::COORDY, i32::MIN, -8),
        (Opcode::COORDY, i32::MAX, 7),
        (Opcode::COORDY, 0x3000_0000, 3),
        (Opcode::COORDZ, -1, 16383),
        (Opcode::COORDZ, i32::MIN, 0),
        (Opcode::COORDZ, i32::MAX, 16383),
        (Opcode::COORDZ, 0x0fff_c000, 0),
    ] {
        let mut vm = Vm::default();
        vm.run(
            1,
            &repository(&[constant(727), constant(value), int(opcode, 255), ret()]),
            Arguments::default(),
        )
        .unwrap();
        assert_eq!(vm.int_stack(), &[727, expected], "{opcode:?} {value}");
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn move_coord_wraps_and_carries_without_clamping_or_masking_any_operand() {
    for (values, expected) in [
        ([0, 0, 0, -1], -1),
        ([0, 0, 8, 0], i32::MIN),
        ([0, 0, 16, 0], 0),
        ([0, 1 << 18, 0, 0], 0),
        ([0x3fff, 0, 0, 1], 0x4000),
        ([0x0fff_c000, 1, 0, 0], 0x1000_0000),
        ([i32::MIN, 0, 8, 0], 0),
        ([i32::MAX, 0, 0, 1], i32::MIN),
        ([i32::MIN, 0, 0, -1], i32::MAX),
        (
            [(2 << 28) | (3200 << 14) | 6400, 7, 1, -9],
            (3 << 28) | (3207 << 14) | 6391,
        ),
    ] {
        let mut code: Vec<_> = values.into_iter().map(constant).collect();
        code.extend([int(Opcode::MOVECOORD, 0), ret()]);
        let mut vm = Vm::default();
        vm.run(1, &repository(&code), Arguments::default()).unwrap();
        assert_eq!(vm.int_stack(), &[expected], "{values:?}");
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_coordinate_underflow_preserves_all_available_operands_and_other_stack_types() {
    for &(opcode, arity) in COORDINATE_OPERATIONS {
        for depth in 0..arity {
            let input: Vec<_> = (0..depth).map(|n| n as i32 + 727).collect();
            let mut code = vec![text("untouched"), long(i64::MAX)];
            code.extend(input.iter().map(|n| constant(*n)));
            code.extend([int(opcode, 255), ret()]);
            let mut vm = Vm::default();
            assert_eq!(
                vm.run(1, &repository(&code), Arguments::default())
                    .unwrap_err(),
                VmError {
                    script_id: 1,
                    instruction: depth + 2,
                    kind: VmErrorKind::StackUnderflow(StackKind::Int),
                }
            );
            assert_eq!(vm.int_stack(), input);
            assert_eq!(vm.string_stack(), &[Some(Arc::<str>::from("untouched"))]);
            assert_eq!(vm.long_stack(), &[i64::MAX]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_stack_replacement_obeys_exact_fuel_boundaries_without_string_allocations() {
    for &(opcode, arity) in COORDINATE_OPERATIONS {
        let (values, expected) = sample(opcode);
        assert_eq!(values.len(), arity);
        let mut input = vec![727; 1000 - arity];
        input.extend_from_slice(values);
        let mut code: Vec<_> = input.iter().map(|n| constant(*n)).collect();
        code.extend([int(opcode, 255), ret()]);
        let scripts = repository(&code);
        let mut output = input[..1000 - arity].to_vec();
        output.push(expected);
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
                        kind: VmErrorKind::InstructionLimit,
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
fn parameterized_children_preserve_operand_order_caller_locals_and_error_locations() {
    for &(opcode, arity) in COORDINATE_OPERATIONS {
        let (values, expected) = sample(opcode);
        let mut child: Vec<_> = (0..arity)
            .map(|n| int(Opcode::PUSH_INT_LOCAL, n as i32))
            .collect();
        child.extend([
            int(opcode, 255),
            int(Opcode::POP_INT_LOCAL, arity as i32),
            int(Opcode::PUSH_INT_LOCAL, arity as i32),
            ret(),
        ]);
        let child = Script::decode(&binary(
            &child,
            counts(arity as u16 + 1, 0, 0),
            counts(arity as u16, 0, 0),
            &[],
        ))
        .unwrap();
        let mut root = vec![
            constant(123),
            int(Opcode::POP_INT_LOCAL, 0),
            text("caller"),
            long(48),
            constant(727),
        ];
        root.extend(values.iter().map(|n| constant(*n)));
        root.extend([
            int(Opcode::GOSUB_WITH_PARAMS, 900048),
            int(Opcode::PUSH_INT_LOCAL, 0),
            ret(),
        ]);
        let root = Script::decode(&binary(&root, counts(1, 0, 0), zero(), &[])).unwrap();
        let scripts = BTreeMap::from([(1, root), (900048, child)]);
        let mut vm = Vm::default();
        let execution = vm.run(1, &scripts, Arguments::default()).unwrap();
        assert_eq!(execution.instructions_executed, 12 + 2 * arity as u64);
        assert_eq!(execution.locals.ints, &[123]);
        assert_eq!(vm.int_stack(), &[727, expected, 123]);
        assert_eq!(vm.string_stack(), &[Some(Arc::<str>::from("caller"))]);
        assert_eq!(vm.long_stack(), &[48]);

        let mut root: Vec<_> = values.iter().map(|n| constant(*n)).collect();
        root.extend([int(Opcode::GOSUB_WITH_PARAMS, 900048), ret()]);
        for missing in [false, true] {
            let mut child: Vec<_> = (0..arity - usize::from(missing))
                .map(|n| int(Opcode::PUSH_INT_LOCAL, n as i32))
                .collect();
            child.extend([int(opcode, 0), ret()]);
            let child = Script::decode(&binary(
                &child,
                counts(arity as u16, 0, 0),
                counts(arity as u16, 0, 0),
                &[],
            ))
            .unwrap();
            let scripts = BTreeMap::from([(1, script(&root)), (900048, child)]);
            let mut vm = Vm::new(VmLimits {
                max_instructions: if missing {
                    200_000
                } else {
                    2 * arity as u64 + 1
                },
                ..VmLimits::default()
            });
            assert_eq!(
                vm.run(1, &scripts, Arguments::default()).unwrap_err(),
                VmError {
                    script_id: 900048,
                    instruction: arity - usize::from(missing),
                    kind: if missing {
                        VmErrorKind::StackUnderflow(StackKind::Int)
                    } else {
                        VmErrorKind::InstructionLimit
                    },
                }
            );
            assert_eq!(vm.int_stack(), &values[..arity - usize::from(missing)]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_coordinate_byte_operand_is_ignored_without_changing_instruction_cost() {
    for &(opcode, arity) in COORDINATE_OPERATIONS {
        let (values, expected) = sample(opcode);
        for operand in 0..=255 {
            let mut code: Vec<_> = values.iter().map(|n| constant(*n)).collect();
            code.extend([int(opcode, operand), ret()]);
            let mut vm = Vm::new(VmLimits {
                max_string_bytes: 0,
                max_join_bytes: 0,
                ..VmLimits::default()
            });
            let execution = vm.run(1, &repository(&code), Arguments::default()).unwrap();
            assert_eq!(vm.int_stack(), &[expected]);
            assert_eq!(execution.instructions_executed, arity as u64 + 2);
            assert_eq!(execution.joined_string_bytes, 0);
        }
    }
}

struct RejectHost;

impl VarHost for RejectHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        panic!("pure operation read variable host")
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        panic!("pure operation wrote variable host")
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        panic!("pure operation looked up variable host")
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn coordinate_ops_never_touch_host_and_related_world_ops_remain_explicitly_unsupported() {
    for &(opcode, arity) in COORDINATE_OPERATIONS {
        let (values, _) = sample(opcode);
        for (depth, max_instructions) in [
            (arity, 200_000),
            (arity - 1, 200_000),
            (arity, arity as u64),
        ] {
            let mut code: Vec<_> = values[..depth].iter().map(|n| constant(*n)).collect();
            code.extend([int(opcode, 255), ret()]);
            let scripts = repository(&code);
            let limits = VmLimits {
                max_instructions,
                ..VmLimits::default()
            };
            let mut pure = Vm::new(limits);
            let mut hosted = Vm::new(limits);
            let result = pure.run(1, &scripts, Arguments::default());
            let hosted_result =
                hosted.run_with_host(1, &scripts, Arguments::default(), &mut RejectHost);
            assert_eq!(result, hosted_result);
            assert_eq!(pure.int_stack(), hosted.int_stack());
            assert_eq!(pure.string_stack(), hosted.string_stack());
            assert_eq!(pure.long_stack(), hosted.long_stack());
        }
    }
    for opcode in [
        Opcode::COORD,
        Opcode::COORD_CLAMPTOSCENE_ORDEFAULT,
        Opcode::MAP_WORLD,
        Opcode::MAP_LANG,
        Opcode::TOSTRING_LOCALISED,
    ] {
        let code = [
            constant(727),
            constant(1),
            constant(2),
            constant(3),
            int(opcode, 0),
            ret(),
        ];
        let mut vm = Vm::default();
        assert_eq!(
            vm.run_with_host(1, &repository(&code), Arguments::default(), &mut RejectHost)
                .unwrap_err(),
            VmError {
                script_id: 1,
                instruction: 4,
                kind: VmErrorKind::UnsupportedOpcode(opcode)
            }
        );
        assert_eq!(vm.int_stack(), &[727, 1, 2, 3]);
    }
}
