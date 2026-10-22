#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{cell::RefCell, collections::BTreeMap, fmt::Write};

#[derive(Clone, Debug, Eq, PartialEq)]
enum Call {
    Read(i32),
    Write(i32, i32),
    Lookup(i32),
}

struct Host {
    active: Vec<i32>,
    pending: Vec<i32>,
    definitions: BTreeMap<i32, Varbit>,
    available: bool,
    read_error: Option<HostError>,
    write_error: Option<HostError>,
    lookup_error: Option<HostError>,
    calls: RefCell<Vec<Call>>,
}

impl Host {
    fn new() -> Self {
        Self {
            active: vec![0; 65536],
            pending: vec![-1; 65536],
            definitions: BTreeMap::new(),
            available: true,
            read_error: None,
            write_error: None,
            lookup_error: None,
            calls: RefCell::default(),
        }
    }

    fn check_varp(&self, id: i32) -> Result<usize, HostError> {
        if !self.available {
            return Err(HostError::Unavailable);
        }
        usize::try_from(id)
            .ok()
            .filter(|id| *id < self.active.len())
            .ok_or(HostError::InvalidVarp(id))
    }
}

impl VarHost for Host {
    fn varp(&self, id: i32) -> Result<i32, HostError> {
        self.calls.borrow_mut().push(Call::Read(id));
        let id = self.check_varp(id)?;
        if let Some(error) = &self.read_error {
            return Err(error.clone());
        }
        Ok(self.active[id])
    }

    fn set_varp(&mut self, id: i32, value: i32) -> Result<(), HostError> {
        self.calls.borrow_mut().push(Call::Write(id, value));
        let id = self.check_varp(id)?;
        if let Some(error) = &self.write_error {
            return Err(error.clone());
        }
        self.active[id] = value;
        Ok(())
    }

    fn varbit(&self, id: i32) -> Result<VarbitLookup, HostError> {
        self.calls.borrow_mut().push(Call::Lookup(id));
        if !self.available {
            return Err(HostError::Unavailable);
        }
        if !(0..=0x03ff_ffff).contains(&id) {
            return Err(HostError::InvalidVarbit(id));
        }
        if let Some(error) = &self.lookup_error {
            return Err(error.clone());
        }
        Ok(self
            .definitions
            .get(&id)
            .copied()
            .map_or(VarbitLookup::Absent, VarbitLookup::Defined))
    }
}

fn execute(
    vm: &mut Vm,
    host: &mut dyn VarHost,
    code: &[(Opcode, Operand)],
) -> Result<Execution, VmError> {
    vm.run_with_host(
        1,
        &BTreeMap::from([(1, script(code))]),
        Arguments::default(),
        host,
    )
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exactly_four_host_operations_are_added_without_changing_pure_support_or_metadata() {
    let expected = [
        (Opcode::PUSH_VAR, 1, 154),
        (Opcode::POP_VAR, 2, 929),
        (Opcode::PUSH_VARBIT, 14, 408),
        (Opcode::POP_VARBIT, 15, 371),
    ];
    for (opcode, ordinal, wire) in expected {
        assert_eq!(opcode.wire(), wire);
        assert_eq!(opcode.metadata().ordinal, ordinal);
        assert_eq!(opcode.metadata().family, 0);
        assert!(opcode.metadata().has_int_constant);
    }
    let mut additional = Vec::new();
    let mut pure = 0;
    let mut vm = Vm::default();
    let mut host = Host::new();
    for entry in &OPCODES {
        let instruction = match entry.opcode {
            Opcode::PUSH_CONSTANT_STRING => text(""),
            Opcode::PUSH_LONG_CONSTANT => long(0),
            opcode => int(opcode, 0),
        };
        let repository = BTreeMap::from([(1, script(&[instruction, ret()]))]);
        let unhosted = vm.run(1, &repository, Arguments::default());
        if !matches!(
            unhosted,
            Err(VmError {
                kind: VmErrorKind::UnsupportedOpcode(_),
                ..
            })
        ) {
            pure += 1;
            continue;
        }
        let hosted = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        if !matches!(
            hosted,
            Err(VmError {
                kind: VmErrorKind::UnsupportedOpcode(_),
                ..
            })
        ) {
            additional.push(entry.opcode);
        }
    }
    assert_eq!(
        pure,
        79 + COORDINATE_OPERATIONS.len() + INVERSE_POWER_OPERATIONS.len()
    );
    assert_eq!(
        additional[..expected.len()],
        expected.map(|(opcode, _, _)| opcode)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unhosted_operations_remain_unsupported_without_consuming_store_values() {
    for opcode in [
        Opcode::PUSH_VAR,
        Opcode::POP_VAR,
        Opcode::PUSH_VARBIT,
        Opcode::POP_VARBIT,
    ] {
        let repository = BTreeMap::from([(1, script(&[constant(727), int(opcode, -1), ret()]))]);
        let mut vm = Vm::default();
        let error = vm.run(1, &repository, Arguments::default()).unwrap_err();
        assert_eq!(
            error,
            VmError {
                script_id: 1,
                instruction: 1,
                kind: VmErrorKind::UnsupportedOpcode(opcode)
            }
        );
        assert_eq!(vm.int_stack(), &[727]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn installed_unavailable_host_is_not_a_proven_absent_definition() {
    for opcode in [
        Opcode::PUSH_VAR,
        Opcode::POP_VAR,
        Opcode::PUSH_VARBIT,
        Opcode::POP_VARBIT,
    ] {
        let mut vm = Vm::default();
        let mut host = Host::new();
        host.available = false;
        let store = matches!(opcode, Opcode::POP_VAR | Opcode::POP_VARBIT);
        let mut code = vec![constant(727)];
        if store {
            code.push(constant(9));
        }
        code.extend([int(opcode, 123), ret()]);
        let error = execute(&mut vm, &mut host, &code).unwrap_err();
        assert_eq!(error.kind, VmErrorKind::Host(HostError::Unavailable));
        assert_eq!(vm.int_stack(), &[727]);
        assert_eq!(
            &*host.calls.borrow(),
            &[match opcode {
                Opcode::PUSH_VAR => Call::Read(123),
                Opcode::POP_VAR => Call::Write(123, 9),
                _ => Call::Lookup(123),
            }]
        );
        assert_eq!(host.active[0], 0);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn varp_and_varbit_reads_and_writes_use_active_values_not_pending() {
    let mut vm = Vm::default();
    let mut host = Host::new();
    host.active[2] = 0x1234;
    host.pending[2] = 0x5678;
    host.definitions.insert(
        123,
        Varbit {
            varp: 2,
            start: 4,
            end: 7,
        },
    );
    execute(
        &mut vm,
        &mut host,
        &[
            int(Opcode::PUSH_VAR, 2),
            int(Opcode::PUSH_VARBIT, 123),
            constant(10),
            int(Opcode::POP_VARBIT, 123),
            int(Opcode::PUSH_VAR, 2),
            constant(-727),
            int(Opcode::POP_VAR, 2),
            int(Opcode::PUSH_VARBIT, 123),
            ret(),
        ],
    )
    .unwrap();
    assert_eq!(vm.int_stack(), &[0x1234, 3, 0x12a4, 2]);
    assert_eq!(host.active[2], -727);
    assert_eq!(host.pending[2], 0x5678);
    assert_eq!(
        &*host.calls.borrow(),
        &[
            Call::Read(2),
            Call::Lookup(123),
            Call::Read(2),
            Call::Lookup(123),
            Call::Read(2),
            Call::Write(2, 0x12a4),
            Call::Read(2),
            Call::Write(2, -727),
            Call::Lookup(123),
            Call::Read(2),
        ]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn only_proven_absence_uses_native_varp_zero_bit_zero_defaults() {
    let mut vm = Vm::default();
    let mut host = Host::new();
    host.active[0] = 0x726;
    execute(
        &mut vm,
        &mut host,
        &[
            int(Opcode::PUSH_VARBIT, 123),
            constant(1),
            int(Opcode::POP_VARBIT, 123),
            int(Opcode::PUSH_VARBIT, 123),
            constant(2),
            int(Opcode::POP_VARBIT, 123),
            int(Opcode::PUSH_VAR, 0),
            ret(),
        ],
    )
    .unwrap();
    assert_eq!(vm.int_stack(), &[0, 1, 0x726]);
    assert_eq!(host.active[0], 0x726);
    host.lookup_error = Some(HostError::State("invalid repository".into()));
    let error = execute(&mut vm, &mut host, &[int(Opcode::PUSH_VARBIT, 123), ret()]).unwrap_err();
    assert_eq!(
        error.kind,
        VmErrorKind::Host(HostError::State("invalid repository".into()))
    );
    assert!(vm.int_stack().is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_identifiers_and_spans_return_typed_installed_host_errors() {
    for (opcode, id, expected) in [
        (Opcode::PUSH_VAR, -1, HostError::InvalidVarp(-1)),
        (Opcode::POP_VAR, 65536, HostError::InvalidVarp(65536)),
        (Opcode::PUSH_VAR, i32::MAX, HostError::InvalidVarp(i32::MAX)),
        (Opcode::PUSH_VARBIT, -1, HostError::InvalidVarbit(-1)),
        (
            Opcode::POP_VARBIT,
            i32::MAX,
            HostError::InvalidVarbit(i32::MAX),
        ),
    ] {
        let mut vm = Vm::default();
        let mut host = Host::new();
        let code = [constant(727), constant(9), int(opcode, id), ret()];
        let error = execute(&mut vm, &mut host, &code).unwrap_err();
        assert_eq!(error.kind, VmErrorKind::Host(expected));
        assert_eq!(
            vm.int_stack(),
            if matches!(opcode, Opcode::POP_VAR | Opcode::POP_VARBIT) {
                &[727][..]
            } else {
                &[727, 9][..]
            }
        );
    }
    for (start, end) in [(1, 0), (0, 32), (0, 255), (222, 254), (255, 0)] {
        for opcode in [Opcode::PUSH_VARBIT, Opcode::POP_VARBIT] {
            let mut vm = Vm::default();
            let mut host = Host::new();
            host.definitions.insert(
                123,
                Varbit {
                    varp: 2,
                    start,
                    end,
                },
            );
            let error =
                execute(&mut vm, &mut host, &[constant(9), int(opcode, 123), ret()]).unwrap_err();
            assert_eq!(
                error.kind,
                VmErrorKind::Host(HostError::InvalidVarbitSpan {
                    id: 123,
                    start,
                    end
                })
            );
            assert_eq!(&*host.calls.borrow(), &[Call::Lookup(123)]);
            assert_eq!(host.active[2], 0);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn stores_pop_before_lookup_read_and_write_errors_and_underflow_touches_no_host() {
    for opcode in [Opcode::POP_VAR, Opcode::POP_VARBIT] {
        for stage in 0..3 {
            let mut vm = Vm::default();
            let mut host = Host::new();
            host.definitions.insert(
                123,
                Varbit {
                    varp: 2,
                    start: 4,
                    end: 7,
                },
            );
            let error = HostError::State(format!("stage {stage}"));
            if stage == 0 && opcode == Opcode::POP_VARBIT {
                host.lookup_error = Some(error.clone());
            } else if stage == 1 && opcode == Opcode::POP_VARBIT {
                host.read_error = Some(error.clone());
            } else {
                host.write_error = Some(error.clone());
            }
            let code = [
                constant(727),
                text("preserved"),
                long(99),
                constant(9),
                int(opcode, 123),
                ret(),
            ];
            let result = execute(&mut vm, &mut host, &code).unwrap_err();
            assert_eq!(result.script_id, 1);
            assert_eq!(result.instruction, 4);
            assert_eq!(result.kind, VmErrorKind::Host(error));
            assert_eq!(vm.int_stack(), &[727]);
            assert_eq!(vm.string_stack()[0].as_deref(), Some("preserved"));
            assert_eq!(vm.long_stack(), &[99]);
            assert_eq!(host.active[2], 0);
        }
        let mut vm = Vm::default();
        let mut host = Host::new();
        host.available = false;
        let error = execute(&mut vm, &mut host, &[int(opcode, -1), ret()]).unwrap_err();
        assert_eq!(error.kind, VmErrorKind::StackUnderflow(StackKind::Int));
        assert!(host.calls.borrow().is_empty());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn hosted_reads_obey_stack_capacity_and_instruction_budget_without_writing() {
    for opcode in [Opcode::PUSH_VAR, Opcode::PUSH_VARBIT] {
        let mut vm = Vm::default();
        let mut host = Host::new();
        let mut code = vec![constant(727); 1000];
        code.extend([int(opcode, 0), ret()]);
        let error = execute(&mut vm, &mut host, &code).unwrap_err();
        assert_eq!(error.kind, VmErrorKind::StackOverflow(StackKind::Int));
        assert_eq!(vm.int_stack().len(), 1000);
        assert!(host
            .calls
            .borrow()
            .iter()
            .all(|call| !matches!(call, Call::Write(..))));
    }
    let mut host = Host::new();
    let mut vm = Vm::new(VmLimits {
        max_instructions: 1,
        ..VmLimits::default()
    });
    let error = execute(
        &mut vm,
        &mut host,
        &[constant(9), int(Opcode::POP_VAR, 2), ret()],
    )
    .unwrap_err();
    assert_eq!(error.kind, VmErrorKind::InstructionLimit);
    assert_eq!(vm.int_stack(), &[9]);
    assert!(host.calls.borrow().is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn nested_calls_share_only_the_supplied_host_and_report_callee_error_locations() {
    let child = script(&[
        constant(99),
        int(Opcode::POP_VAR, 2),
        int(Opcode::PUSH_VARBIT, 123),
        ret(),
    ]);
    let repository = BTreeMap::from([
        (
            1,
            script(&[
                int(Opcode::GOSUB_WITH_PARAMS, 2),
                int(Opcode::PUSH_VAR, 2),
                ret(),
            ]),
        ),
        (2, child),
    ]);
    let mut vm = Vm::default();
    let mut first = Host::new();
    first.definitions.insert(
        123,
        Varbit {
            varp: 2,
            start: 0,
            end: 7,
        },
    );
    let execution = vm
        .run_with_host(1, &repository, Arguments::default(), &mut first)
        .unwrap();
    assert_eq!(execution.instructions_executed, 7);
    assert_eq!(vm.int_stack(), &[99, 99]);
    let mut second = Host::new();
    second.write_error = Some(HostError::State("read only".into()));
    let error = vm
        .run_with_host(1, &repository, Arguments::default(), &mut second)
        .unwrap_err();
    assert_eq!(
        error,
        VmError {
            script_id: 2,
            instruction: 1,
            kind: VmErrorKind::Host(HostError::State("read only".into()))
        }
    );
    assert!(vm.int_stack().is_empty());
    assert_eq!(first.active[2], 99);
    assert_eq!(second.active[2], 0);
    vm.reset();
    assert_eq!(first.active[2], 99);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn independent_real_native_executor_vectors_match_all_masks_signed_values_and_masked_shifts() {
    let mut vm = Vm::default();
    let mut host = Host::new();
    let mut count = 0;
    for line in include_str!("fixtures/variables.native.tsv").lines() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 12);
        let number = |index: usize| fields[index].parse::<i32>().unwrap();
        let opcode = OPCODES
            .iter()
            .find(|entry| entry.name == fields[0])
            .unwrap()
            .opcode;
        let id = number(1);
        let varp = number(2) as usize;
        host.active[varp] = number(5);
        host.pending[varp] = number(6);
        host.definitions.clear();
        host.calls.borrow_mut().clear();
        if number(3) >= 0 {
            host.definitions.insert(
                id,
                Varbit {
                    varp: varp as u16,
                    start: number(3) as u8,
                    end: number(4) as u8,
                },
            );
        }
        let mut code = vec![constant(727)];
        if fields[7] != "-" {
            code.push(constant(number(7)));
        }
        code.extend([int(opcode, id), ret()]);
        let execution = execute(&mut vm, &mut host, &code).unwrap();
        assert_eq!(
            execution.instructions_executed,
            fields[11].parse::<u64>().unwrap(),
            "{line}"
        );
        let mut stack = vec![727];
        if fields[8] != "-" {
            stack.push(number(8));
        }
        assert_eq!(vm.int_stack(), stack, "{line}");
        assert_eq!(host.active[varp], number(9), "{line}");
        assert_eq!(host.pending[varp], number(10), "{line}");
        count += 1;
    }
    assert_eq!(count, 5863);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_script_fixture_matches_stack_and_active_vars_without_wall_clock_assumptions() {
    let repository = BTreeMap::from([(
        1,
        Script::decode(include_bytes!("fixtures/vm-vars.bin")).unwrap(),
    )]);
    let mut vm = Vm::default();
    let mut host = Host::new();
    for (id, start, end) in [(1031, 4, 11), (1032, 34, 41), (1033, 0, 31)] {
        host.definitions.insert(
            id,
            Varbit {
                varp: 2,
                start,
                end,
            },
        );
    }
    let execution = vm
        .run_with_host(1, &repository, Arguments::default(), &mut host)
        .unwrap();
    let mut actual = format!("instructions\t{}\n", execution.instructions_executed);
    for value in vm.int_stack() {
        writeln!(actual, "int\t{value}").unwrap();
    }
    for id in [0, 2, 65535] {
        writeln!(actual, "active\t{id}\t{}", host.active[id]).unwrap();
    }
    assert_eq!(actual, include_str!("fixtures/vm-vars.vm.tsv"));
    assert!(host.pending.iter().all(|value| *value == -1));
}
