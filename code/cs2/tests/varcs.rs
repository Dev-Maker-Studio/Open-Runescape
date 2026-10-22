#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: [(Opcode, u16, u16); 4] = [
    (Opcode::PUSH_VARC_INT, 26, 311),
    (Opcode::POP_VARC_INT, 27, 510),
    (Opcode::PUSH_VARC_STRING, 31, 700),
    (Opcode::POP_VARC_STRING, 32, 21),
];

fn state() -> VarcState {
    VarcState::new(&[false, true, false], 3, VarcLimits::default()).unwrap()
}

fn execute(
    vm: &mut Vm,
    state: &mut dyn VarHost,
    code: &[(Opcode, Operand)],
) -> Result<Execution, VmError> {
    vm.run_with_host(
        1,
        &BTreeMap::from([(1, script(code))]),
        Arguments::default(),
        state,
    )
}

struct CombinedHost {
    state: VarcState,
    varp: i32,
}

impl VarHost for CombinedHost {
    fn varp(&self, _id: i32) -> Result<i32, HostError> {
        Ok(self.varp)
    }

    fn set_varp(&mut self, _id: i32, value: i32) -> Result<(), HostError> {
        self.varp = value;
        Ok(())
    }

    fn varbit(&self, _id: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn varcs(&mut self) -> Result<&mut VarcState, HostError> {
        Ok(&mut self.state)
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exactly_four_varcs_preserve_all_metadata_and_existing_support() {
    assert_eq!(OPCODES.len(), 1010);
    for (opcode, ordinal, wire) in OPERATIONS {
        assert_eq!(opcode.metadata().ordinal, ordinal);
        assert_eq!(opcode.wire(), wire);
        assert!(opcode.metadata().has_int_constant);
        assert_eq!(opcode.metadata().family, 0);
    }
    let mut vm = Vm::default();
    let mut host = CombinedHost {
        state: state(),
        varp: 0,
    };
    let mut pure = 0;
    let mut hosted = Vec::new();
    for entry in &OPCODES {
        let instruction = match entry.opcode {
            Opcode::PUSH_CONSTANT_STRING => text(""),
            Opcode::PUSH_LONG_CONSTANT => long(0),
            opcode => int(opcode, 0),
        };
        let repository = BTreeMap::from([(1, script(&[instruction, ret()]))]);
        if !matches!(
            vm.run(1, &repository, Arguments::default()),
            Err(VmError {
                kind: VmErrorKind::UnsupportedOpcode(_),
                ..
            })
        ) {
            pure += 1;
        } else if !matches!(
            vm.run_with_host(1, &repository, Arguments::default(), &mut host),
            Err(VmError {
                kind: VmErrorKind::UnsupportedOpcode(_),
                ..
            })
        ) {
            hosted.push(entry.opcode);
        }
    }
    assert_eq!(pure, 84);
    assert_eq!(
        hosted[..8],
        [
            Opcode::PUSH_VAR,
            Opcode::POP_VAR,
            Opcode::PUSH_VARBIT,
            Opcode::POP_VARBIT,
            Opcode::PUSH_VARC_INT,
            Opcode::POP_VARC_INT,
            Opcode::PUSH_VARC_STRING,
            Opcode::POP_VARC_STRING,
        ]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn initial_values_null_roundtrip_and_typed_stacks() {
    let mut state = state();
    assert_eq!(
        (
            state.int_count(),
            state.string_count(),
            state.string_bytes()
        ),
        (3, 3, 0)
    );
    assert!(!state.dirty());
    assert_eq!(state.int(0).unwrap(), -1);
    assert_eq!(state.string(0).unwrap(), None);
    let code = [
        constant(727),
        text("lower"),
        long(i64::MIN + 727),
        int(Opcode::PUSH_VARC_INT, 0),
        constant(i32::MIN),
        int(Opcode::POP_VARC_INT, 0),
        constant(i32::MAX),
        int(Opcode::POP_VARC_INT, 1),
        int(Opcode::PUSH_VARC_INT, 0),
        int(Opcode::PUSH_VARC_INT, 1),
        int(Opcode::PUSH_VARC_STRING, 0),
        text("€"),
        int(Opcode::POP_VARC_STRING, 0),
        int(Opcode::PUSH_VARC_STRING, 0),
        int(Opcode::PUSH_STRING_LOCAL, 0),
        int(Opcode::POP_VARC_STRING, 0),
        int(Opcode::PUSH_VARC_STRING, 0),
        ret(),
    ];
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    vm.run_with_host(1, &repository, Arguments::default(), &mut state)
        .unwrap();
    assert_eq!(vm.int_stack(), &[727, -1, i32::MIN, i32::MAX]);
    assert_eq!(
        vm.string_stack(),
        &[
            Some(Arc::from("lower")),
            Some(Arc::from("")),
            Some(Arc::from("€")),
            Some(Arc::from(""))
        ]
    );
    assert_eq!(vm.long_stack(), &[i64::MIN + 727]);
    assert_eq!(state.string(0).unwrap(), None);
    assert_eq!(state.string_bytes(), 0);
    assert!(state.dirty());
    assert_eq!(state.pending_pulses().len(), 3);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn no_host_or_varc_capability_does_not_consume_any_stack() {
    struct Legacy;
    impl VarHost for Legacy {
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
    for (opcode, _, _) in OPERATIONS {
        let repository = BTreeMap::from([(
            1,
            script(&[
                constant(727),
                text("lower"),
                long(727),
                int(opcode, -1),
                ret(),
            ]),
        )]);
        for hosted in [false, true] {
            let mut vm = Vm::default();
            let result = if hosted {
                vm.run_with_host(1, &repository, Arguments::default(), &mut Legacy)
            } else {
                vm.run(1, &repository, Arguments::default())
            };
            assert_eq!(
                result.unwrap_err(),
                VmError {
                    script_id: 1,
                    instruction: 3,
                    kind: VmErrorKind::UnsupportedOpcode(opcode)
                }
            );
            assert_eq!(vm.int_stack(), &[727]);
            assert_eq!(vm.string_stack(), &[Some(Arc::from("lower"))]);
            assert_eq!(vm.long_stack(), &[727]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_ids_and_underflow_are_transactional() {
    for (opcode, _, _) in OPERATIONS {
        for id in [i32::MIN, -1, 3, 65536, i32::MAX] {
            let mut state = state();
            let previous = state.clone();
            let mut vm = Vm::default();
            let error = execute(
                &mut vm,
                &mut state,
                &[
                    constant(727),
                    text("lower"),
                    long(727),
                    int(opcode, id),
                    ret(),
                ],
            )
            .unwrap_err();
            let reason = if matches!(opcode, Opcode::PUSH_VARC_INT | Opcode::POP_VARC_INT) {
                VarcError::InvalidIntId(id)
            } else {
                VarcError::InvalidStringId(id)
            };
            assert_eq!(
                error,
                VmError {
                    script_id: 1,
                    instruction: 3,
                    kind: VmErrorKind::Host(HostError::Varc(reason))
                }
            );
            assert_eq!(state, previous);
            assert_eq!(vm.int_stack(), &[727]);
            assert_eq!(vm.string_stack(), &[Some(Arc::from("lower"))]);
            assert_eq!(vm.long_stack(), &[727]);
        }
    }
    for (opcode, kind) in [
        (Opcode::POP_VARC_INT, StackKind::Int),
        (Opcode::POP_VARC_STRING, StackKind::String),
    ] {
        let mut state = state();
        let previous = state.clone();
        assert_eq!(
            execute(&mut Vm::default(), &mut state, &[int(opcode, 0), ret()])
                .unwrap_err()
                .kind,
            VmErrorKind::StackUnderflow(kind)
        );
        assert_eq!(state, previous);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn string_limits_replacement_and_null_are_transactional() {
    let mut state = VarcState::new(
        &[],
        2,
        VarcLimits {
            max_string_bytes: 4,
            max_total_string_bytes: 5,
        },
    )
    .unwrap();
    state.set_string(0, Some(Arc::from("😀"))).unwrap();
    state.set_string(1, Some(Arc::from("x"))).unwrap();
    let previous = state.clone();
    assert_eq!(
        state.set_string(0, Some(Arc::from("12345"))),
        Err(VarcError::StringLimit)
    );
    assert_eq!(state, previous);
    assert_eq!(
        state.set_string(1, Some(Arc::from("xx"))),
        Err(VarcError::StorageLimit)
    );
    assert_eq!(state, previous);
    let mut vm = Vm::default();
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[text("xx"), int(Opcode::POP_VARC_STRING, 1), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::Host(HostError::Varc(VarcError::StorageLimit))
    );
    assert_eq!(vm.string_stack(), &[Some(Arc::from("xx"))]);
    assert_eq!(state, previous);
    state.set_string(0, None).unwrap();
    assert_eq!(state.string_bytes(), 1);
    state.set_string(0, Some(Arc::from(""))).unwrap();
    assert_eq!(state.string(0).unwrap(), Some(Arc::from("")));
    assert_eq!(state.string_bytes(), 1);
    state.set_string(1, Some(Arc::from("€"))).unwrap();
    assert_eq!(state.string_bytes(), 3);
    state.set_string(1, Some(Arc::from("😀"))).unwrap();
    assert_eq!(state.string_bytes(), 4);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn shared_strings_count_per_slot_and_slot_capacity_is_exact() {
    let mut state = VarcState::new(
        &[false; 65536],
        65536,
        VarcLimits {
            max_string_bytes: 4,
            max_total_string_bytes: 4,
        },
    )
    .unwrap();
    let value: Arc<str> = Arc::from("xx");
    state.set_string(0, Some(value.clone())).unwrap();
    state.set_string(65535, Some(value.clone())).unwrap();
    assert_eq!(state.string_bytes(), 4);
    assert_eq!(
        state.set_string(1, Some(value)),
        Err(VarcError::StorageLimit)
    );
    state.set_int(65535, 727).unwrap();
    assert_eq!(state.int(65535).unwrap(), 727);
    assert_eq!(state.pending_pulses().last().unwrap().id, 65535);
    assert_eq!(
        VarcState::new(&[false; 65537], 0, VarcLimits::default()).unwrap_err(),
        VarcError::SlotLimit
    );
    assert_eq!(
        VarcState::new(&[], 65537, VarcLimits::default()).unwrap_err(),
        VarcError::SlotLimit
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn dirty_is_sticky_and_same_value_stores_reschedule_by_type_and_id() {
    let mut state = state();
    state.set_int(0, -1).unwrap();
    assert!(!state.dirty());
    state.set_int(1, -1).unwrap();
    assert!(state.dirty());
    state.clear_dirty();
    state.set_int(1, -1).unwrap();
    assert!(state.dirty());
    state.set_time(100).unwrap();
    state.set_string(0, None).unwrap();
    state.set_time(200).unwrap();
    state.set_int(0, -1).unwrap();
    assert_eq!(
        state.pending_pulses(),
        [
            VarcPulse {
                kind: VarcPulseKind::Int,
                id: 1,
                due_ms: 500
            },
            VarcPulse {
                kind: VarcPulseKind::String,
                id: 0,
                due_ms: 600
            },
            VarcPulse {
                kind: VarcPulseKind::Int,
                id: 0,
                due_ms: 700
            },
        ]
    );
    state.set_time(499).unwrap();
    assert!(state.take_due_pulses().is_empty());
    state.set_time(500).unwrap();
    assert_eq!(
        state.take_due_pulses(),
        [VarcPulse {
            kind: VarcPulseKind::Int,
            id: 1,
            due_ms: 500
        }]
    );
    state.set_time(600).unwrap();
    assert_eq!(
        state.take_due_pulses(),
        [VarcPulse {
            kind: VarcPulseKind::String,
            id: 0,
            due_ms: 600
        }]
    );
    state.set_time(700).unwrap();
    assert_eq!(
        state.take_due_pulses(),
        [VarcPulse {
            kind: VarcPulseKind::Int,
            id: 0,
            due_ms: 700
        }]
    );
    assert!(state.take_due_pulses().is_empty());
    assert_eq!(state.int(0).unwrap(), -1);
    assert_eq!(state.string(0).unwrap(), None);
    assert!(state.dirty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn equal_deadlines_have_native_queue_order_and_clock_is_explicit() {
    let mut state = state();
    state.set_int(0, 0).unwrap();
    state.set_string(0, None).unwrap();
    state.set_int(1, 0).unwrap();
    state.set_int(0, 0).unwrap();
    assert_eq!(
        state
            .pending_pulses()
            .iter()
            .map(|pulse| (pulse.kind, pulse.id))
            .collect::<Vec<_>>(),
        [
            (VarcPulseKind::String, 0),
            (VarcPulseKind::Int, 1),
            (VarcPulseKind::Int, 0),
        ]
    );
    state.set_time(MAX_VARC_TIME_MS - VARC_DEBOUNCE_MS).unwrap();
    state.set_int(2, 0).unwrap();
    assert_eq!(
        state.pending_pulses().last().unwrap().due_ms,
        MAX_VARC_TIME_MS
    );
    state.set_time(MAX_VARC_TIME_MS).unwrap();
    assert_eq!(state.take_due_pulses().len(), 4);
    let previous = state.clone();
    assert_eq!(
        state.set_int(2, 1),
        Err(VarcError::ClockOutOfRange(MAX_VARC_TIME_MS))
    );
    assert_eq!(state, previous);
    assert_eq!(
        state.set_string(0, None),
        Err(VarcError::ClockOutOfRange(MAX_VARC_TIME_MS))
    );
    assert_eq!(state, previous);
    assert!(matches!(
        state.set_time(MAX_VARC_TIME_MS - 1),
        Err(VarcError::ClockRewind { .. })
    ));
    assert_eq!(state, previous);
    assert_eq!(
        state.set_time(MAX_VARC_TIME_MS + 1),
        Err(VarcError::ClockOutOfRange(MAX_VARC_TIME_MS + 1))
    );
    assert_eq!(state, previous);
    assert_eq!(
        state.set_time(u64::MAX),
        Err(VarcError::ClockOutOfRange(u64::MAX))
    );
    assert_eq!(state, previous);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn vm_read_limits_and_stack_overflow_cannot_mutate_host() {
    let mut state = state();
    state.set_string(0, Some(Arc::from("€"))).unwrap();
    let previous = state.clone();
    let mut vm = Vm::new(VmLimits {
        max_string_bytes: 2,
        ..VmLimits::default()
    });
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[int(Opcode::PUSH_VARC_STRING, 0), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::StringLimit
    );
    assert!(vm.string_stack().is_empty());
    assert_eq!(state, previous);
    for (opcode, kind) in [
        (Opcode::PUSH_VARC_INT, StackKind::Int),
        (Opcode::PUSH_VARC_STRING, StackKind::String),
    ] {
        let mut code = vec![int(opcode, 0); 1001];
        code.push(ret());
        let mut vm = Vm::default();
        let error = execute(&mut vm, &mut state, &code).unwrap_err();
        assert_eq!(error.instruction, 1000);
        assert_eq!(error.kind, VmErrorKind::StackOverflow(kind));
        assert_eq!(state, previous);
        assert_eq!(
            if kind == StackKind::Int {
                vm.int_stack().len()
            } else {
                vm.string_stack().len()
            },
            1000
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn initial_snapshot_validation_has_no_store_side_effects() {
    let limits = VarcLimits {
        max_string_bytes: 4,
        max_total_string_bytes: 5,
    };
    let state =
        VarcState::from_values(&[true], &[727], &[None, Some(Arc::from("😀"))], limits).unwrap();
    assert_eq!(state.int(0).unwrap(), 727);
    assert_eq!(state.string(0).unwrap(), None);
    assert_eq!(state.string_bytes(), 4);
    assert!(!state.dirty());
    assert!(state.pending_pulses().is_empty());
    assert_eq!(
        VarcState::from_values(&[], &[0], &[], limits).unwrap_err(),
        VarcError::PersistenceCount
    );
    assert_eq!(
        VarcState::from_values(&[], &[], &[Some(Arc::from("12345"))], limits).unwrap_err(),
        VarcError::StringLimit
    );
    assert_eq!(
        VarcState::from_values(
            &[],
            &[],
            &[Some(Arc::from("€")), Some(Arc::from("€"))],
            limits
        )
        .unwrap_err(),
        VarcError::StorageLimit
    );
    assert_eq!(
        VarcState::from_values(&[], &[], &vec![None; 65537], limits).unwrap_err(),
        VarcError::SlotLimit
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn vm_clock_errors_preserve_store_values_and_instruction_budget_bounds_effects() {
    let mut state = state();
    state.set_time(MAX_VARC_TIME_MS).unwrap();
    let previous = state.clone();
    let mut vm = Vm::default();
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[constant(727), int(Opcode::POP_VARC_INT, 1), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::Host(HostError::Varc(VarcError::ClockOutOfRange(
            MAX_VARC_TIME_MS
        )))
    );
    assert_eq!(vm.int_stack(), &[727]);
    assert_eq!(state, previous);
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[text("x"), int(Opcode::POP_VARC_STRING, 0), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::Host(HostError::Varc(VarcError::ClockOutOfRange(
            MAX_VARC_TIME_MS
        )))
    );
    assert_eq!(vm.string_stack(), &[Some(Arc::from("x"))]);
    assert_eq!(state, previous);
    let mut state = VarcState::new(&[true], 1, VarcLimits::default()).unwrap();
    let previous = state.clone();
    let mut vm = Vm::new(VmLimits {
        max_instructions: 1,
        ..VmLimits::default()
    });
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[constant(727), int(Opcode::POP_VARC_INT, 0), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::InstructionLimit
    );
    assert_eq!(state, previous);
    let mut vm = Vm::new(VmLimits {
        max_instructions: 2,
        ..VmLimits::default()
    });
    assert_eq!(
        execute(
            &mut vm,
            &mut state,
            &[constant(727), int(Opcode::POP_VARC_INT, 0), ret()]
        )
        .unwrap_err()
        .kind,
        VmErrorKind::InstructionLimit
    );
    assert_eq!(state.int(0).unwrap(), 727);
    assert!(state.dirty());
    assert_eq!(state.pending_pulses().len(), 1);
}
