#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, fmt::Write, str::FromStr, sync::Arc};

fn numbers<T: FromStr>(value: &str) -> Vec<T>
where
    T::Err: std::fmt::Debug,
{
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn strings(value: &str) -> Vec<Option<Arc<str>>> {
    if value == "-" {
        return Vec::new();
    }
    value
        .split(',')
        .map(|value| {
            if value == "null" {
                None
            } else {
                let units = value.strip_prefix('u').unwrap();
                assert_eq!(units.len() % 4, 0);
                let units: Vec<u16> = (0..units.len())
                    .step_by(4)
                    .map(|i| u16::from_str_radix(&units[i..i + 4], 16).unwrap())
                    .collect();
                Some(Arc::from(String::from_utf16(&units).expect(
                    "VARC fixture must be representable without isolated surrogates",
                )))
            }
        })
        .collect()
}

fn events(value: &str) -> Vec<(VarcPulseKind, u16)> {
    if value == "-" {
        return Vec::new();
    }
    value
        .split(',')
        .map(|value| {
            let fields: Vec<_> = value.split(':').collect();
            assert_eq!(fields.len(), 4);
            assert_eq!(fields[3], "500");
            let kind = match fields[1] {
                "1" => VarcPulseKind::Int,
                "2" => VarcPulseKind::String,
                _ => panic!("unknown native pulse type"),
            };
            (kind, fields[2].parse().unwrap())
        })
        .collect()
}

fn assert_state(state: &VarcState, ints: &str, text: &str, dirty: &str, queue: &str) {
    let expected_ints = numbers::<i32>(ints);
    assert_eq!(state.int_count(), expected_ints.len());
    for (id, value) in expected_ints.iter().enumerate() {
        assert_eq!(state.int(id as i32).unwrap(), *value);
    }
    let expected_strings = strings(text);
    assert_eq!(state.string_count(), expected_strings.len());
    for (id, value) in expected_strings.iter().enumerate() {
        assert_eq!(&state.string(id as i32).unwrap(), value);
    }
    assert_eq!(
        state.string_bytes(),
        expected_strings
            .iter()
            .flatten()
            .map(|s| s.len())
            .sum::<usize>()
    );
    assert_eq!(state.dirty(), dirty == "1");
    assert_eq!(
        state
            .pending_pulses()
            .iter()
            .map(|pulse| (pulse.kind, pulse.id))
            .collect::<Vec<_>>(),
        events(queue)
    );
    assert!(state
        .pending_pulses()
        .iter()
        .all(|pulse| pulse.due_ms == 500));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_varc_step_matches_or_is_rejected_transactionally() {
    let mut current_case = "";
    let mut state = VarcState::new(&[], 0, VarcLimits::default()).unwrap();
    let mut successes = [0usize; 4];
    let mut failures = 0;
    let mut malformed_states = 0;
    for line in include_str!("fixtures/varc.native.tsv").lines() {
        let row: Vec<_> = line.split('\t').collect();
        assert_eq!(row.len(), 25);
        let opcode = OPCODES
            .iter()
            .find(|entry| entry.name == row[2])
            .unwrap()
            .opcode;
        let operation = [
            Opcode::PUSH_VARC_INT,
            Opcode::POP_VARC_INT,
            Opcode::PUSH_VARC_STRING,
            Opcode::POP_VARC_STRING,
        ]
        .iter()
        .position(|value| *value == opcode)
        .unwrap();
        let initial_ints = numbers::<i32>(row[7]);
        let flags: Vec<_> = numbers::<u8>(row[9]).into_iter().map(|n| n != 0).collect();
        if initial_ints.len() != flags.len() {
            assert_eq!(
                VarcState::from_values(
                    &flags,
                    &initial_ints,
                    &strings(row[8]),
                    VarcLimits::default()
                )
                .unwrap_err(),
                VarcError::PersistenceCount
            );
            malformed_states += 1;
            continue;
        }
        if current_case != row[0] {
            assert_eq!(row[1], "0");
            assert_eq!(row[10], "0");
            assert_eq!(row[12], "-");
            state = VarcState::from_values(
                &flags,
                &initial_ints,
                &strings(row[8]),
                VarcLimits::default(),
            )
            .unwrap();
            current_case = row[0];
        }
        assert_state(&state, row[7], row[8], row[10], row[12]);
        let previous = state.clone();
        let ints = numbers::<i32>(row[4]);
        let text = strings(row[5]);
        let longs = numbers::<i64>(row[6]);
        let mut code: Vec<_> = ints.iter().map(|value| constant(*value)).collect();
        code.extend((0..text.len()).map(|id| int(Opcode::PUSH_STRING_LOCAL, id as i32)));
        code.extend(longs.iter().map(|value| long(*value)));
        code.push(int(opcode, row[3].parse().unwrap()));
        code.push(ret());
        let args = counts(0, text.len().try_into().unwrap(), 0);
        let script = Script::decode(&binary(&code, args, args, &[])).unwrap();
        let repository = BTreeMap::from([(1, script)]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(
            1,
            &repository,
            Arguments {
                strings: &text,
                ..Arguments::default()
            },
            &mut state,
        );
        if row[13] == "ok" {
            result.unwrap_or_else(|error| panic!("{} step{}: {error:?}", row[0], row[1]));
            assert_eq!(vm.int_stack(), numbers::<i32>(row[15]));
            assert_eq!(vm.string_stack(), strings(row[17]));
            assert_eq!(vm.long_stack(), numbers::<i64>(row[19]));
            assert_eq!(vm.int_stack().len(), row[14].parse::<usize>().unwrap());
            assert_eq!(vm.string_stack().len(), row[16].parse::<usize>().unwrap());
            assert_eq!(vm.long_stack().len(), row[18].parse::<usize>().unwrap());
            assert_state(&state, row[20], row[21], row[22], row[24]);
            let mut cache = events(row[23]);
            let mut queue = events(row[24]);
            let key = |(kind, id): &(VarcPulseKind, u16)| {
                (if *kind == VarcPulseKind::Int { 1 } else { 2 }, *id)
            };
            cache.sort_by_key(key);
            queue.sort_by_key(key);
            assert_eq!(cache, queue);
            successes[operation] += 1;
        } else {
            assert!(row[13].starts_with("error:"));
            let error = result.unwrap_err();
            assert_eq!(error.instruction, code.len() - 2);
            assert!(matches!(
                error.kind,
                VmErrorKind::Host(HostError::Varc(_))
                    | VmErrorKind::StackUnderflow(_)
                    | VmErrorKind::StackOverflow(_)
            ));
            assert_eq!(state, previous);
            assert_eq!(vm.int_stack(), ints);
            assert_eq!(vm.string_stack(), text);
            assert_eq!(vm.long_stack(), longs);
            failures += 1;
        }
    }
    assert_eq!(successes, [130, 181, 137, 194]);
    assert_eq!(failures, 34);
    assert_eq!(malformed_states, 3);
}

fn state_rows(phase: &str) -> BTreeMap<&'static str, &'static str> {
    include_str!("fixtures/vm-varcs.state.tsv")
        .lines()
        .filter_map(|line| {
            let row: Vec<_> = line.split('\t').collect();
            assert_eq!(row.len(), 3);
            (row[0] == phase).then_some((row[1], row[2]))
        })
        .collect()
}

fn vm_snapshot(vm: &Vm, execution: &Execution) -> String {
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
    actual
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_varc_root_and_child_preserve_decode_stacks_locals_and_shared_host() {
    let fixtures: &[(&[u8], &str)] = &[
        (
            include_bytes!("fixtures/vm-varcs.bin"),
            include_str!("fixtures/vm-varcs.decode.tsv"),
        ),
        (
            include_bytes!("fixtures/vm-varcs-child.bin"),
            include_str!("fixtures/vm-varcs-child.decode.tsv"),
        ),
    ];
    for (bytes, expected) in fixtures {
        let script = Script::decode(bytes).unwrap();
        let locals = script.locals();
        let args = script.arguments();
        let mut actual = format!(
            "name\t{}\ncounts\t{}\t{}\t{}\t{}\t{}\t{}\n",
            string(script.name()),
            locals.ints,
            locals.strings,
            locals.longs,
            args.ints,
            args.strings,
            args.longs
        );
        for (index, instruction) in script.instructions().iter().enumerate() {
            write!(actual, "{index}\t{}\t", instruction.opcode.metadata().name).unwrap();
            match &instruction.operand {
                Operand::Int(value) => writeln!(actual, "int\t{value}"),
                Operand::Long(value) => writeln!(actual, "long\t{value}"),
                Operand::String(value) => writeln!(actual, "string\t{}", string(Some(value))),
            }
            .unwrap();
        }
        assert!(script.switches().is_empty());
        assert_eq!(&actual, expected);
    }
    let before = state_rows("before");
    let flags: Vec<_> = numbers::<u8>(before["persistent"])
        .into_iter()
        .map(|n| n != 0)
        .collect();
    let mut state = VarcState::from_values(
        &flags,
        &numbers::<i32>(before["ints"]),
        &strings(before["strings"]),
        VarcLimits::default(),
    )
    .unwrap();
    assert_state(
        &state,
        before["ints"],
        before["strings"],
        before["dirty"],
        before["low-priority"],
    );
    let repository = BTreeMap::from([
        (1, Script::decode(fixtures[0].0).unwrap()),
        (900026, Script::decode(fixtures[1].0).unwrap()),
    ]);
    let mut vm = Vm::default();
    let execution = vm
        .run_with_host(1, &repository, Arguments::default(), &mut state)
        .unwrap();
    assert_eq!(
        vm_snapshot(&vm, &execution),
        include_str!("fixtures/vm-varcs.vm.tsv")
    );
    let after = state_rows("after");
    assert_state(
        &state,
        after["ints"],
        after["strings"],
        after["dirty"],
        after["low-priority"],
    );
    let error = Vm::default()
        .run(1, &repository, Arguments::default())
        .unwrap_err();
    assert!(matches!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(
            Opcode::PUSH_VARC_INT
                | Opcode::POP_VARC_INT
                | Opcode::PUSH_VARC_STRING
                | Opcode::POP_VARC_STRING
        )
    ));
}
