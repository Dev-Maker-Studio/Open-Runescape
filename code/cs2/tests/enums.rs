#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{cell::Cell, collections::BTreeMap, fmt::Write, sync::Arc};

const ENUMS: &[Opcode] = &[
    Opcode::ENUM,
    Opcode::ENUM_STRING,
    Opcode::ENUM_HASOUTPUT,
    Opcode::ENUM_HASOUTPUT_STRING,
    Opcode::ENUM_GETOUTPUTCOUNT,
    Opcode::ENUM_GETREVERSECOUNT,
    Opcode::ENUM_GETREVERSECOUNT_STRING,
    Opcode::ENUM_GETREVERSEINDEX,
    Opcode::ENUM_GETREVERSEINDEX_STRING,
];

const ARITY: &[(Opcode, usize, usize)] = &[
    (Opcode::ENUM, 4, 0),
    (Opcode::ENUM_STRING, 2, 0),
    (Opcode::ENUM_HASOUTPUT, 3, 0),
    (Opcode::ENUM_HASOUTPUT_STRING, 1, 1),
    (Opcode::ENUM_GETOUTPUTCOUNT, 1, 0),
    (Opcode::ENUM_GETREVERSECOUNT, 3, 0),
    (Opcode::ENUM_GETREVERSECOUNT_STRING, 1, 1),
    (Opcode::ENUM_GETREVERSEINDEX, 5, 0),
    (Opcode::ENUM_GETREVERSEINDEX_STRING, 3, 1),
];

#[derive(Default)]
struct EnumHost {
    varc: Option<VarcState>,
    components: Option<ComponentModel>,
    definitions: BTreeMap<i32, Arc<EnumDefinition>>,
    lookups: Cell<usize>,
    failure: Option<HostError>,
    client: Option<openrs_cs2::ClientState>,
}

impl VarHost for EnumHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn entity_varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn entity_varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn item_definition(&self, _: i32) -> Result<Option<Arc<ItemDefinition>>, HostError> {
        Ok(None)
    }

    fn param_definition(&self, _: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        Ok(None)
    }

    fn config_params(&self, _: ConfigKind, _: i32) -> Result<Option<Arc<ParamTable>>, HostError> {
        Ok(None)
    }

    fn interaction_colours(&self) -> Result<InteractionColours, HostError> {
        Ok(InteractionColours::default())
    }

    fn varcs(&mut self) -> Result<&mut VarcState, HostError> {
        self.varc.as_mut().ok_or(HostError::Unavailable)
    }

    fn components(&mut self) -> Result<&mut ComponentModel, HostError> {
        self.components.as_mut().ok_or(HostError::Unavailable)
    }

    fn client(&mut self) -> Result<&mut openrs_cs2::ClientState, HostError> {
        self.client.as_mut().ok_or(HostError::Unavailable)
    }

    fn enum_definition(&self, id: i32) -> Result<Option<Arc<EnumDefinition>>, HostError> {
        self.lookups.set(self.lookups.get() + 1);
        if let Some(error) = &self.failure {
            return Err(error.clone());
        }
        Ok(self.definitions.get(&id).cloned())
    }
}

struct Unavailable;

impl VarHost for Unavailable {
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

fn unhex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn utf8(value: &str) -> String {
    String::from_utf8(unhex(value)).unwrap()
}

fn utf16(value: &str) -> Option<String> {
    if value == "null" {
        return None;
    }
    let units: Vec<u16> = (0..(value.len() - 1) / 4)
        .map(|index| u16::from_str_radix(&value[1 + index * 4..5 + index * 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(utf16).collect()
    }
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn entry(value: &str) -> (i32, EnumValue) {
    let (key, rest) = value.split_once('=').unwrap();
    let parsed = match rest.split_at(1) {
        ("i", number) => EnumValue::Int(number.parse().unwrap()),
        ("s", hex) => EnumValue::Text(Arc::from(utf8(hex))),
        _ => unreachable!(),
    };
    (key.parse().unwrap(), parsed)
}

fn host() -> EnumHost {
    let mut host = EnumHost {
        client: Some(openrs_cs2::ClientState::new()),
        ..Default::default()
    };
    for line in include_str!("fixtures/enum-defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 9);
        let entries: Vec<_> = if columns[8] == "-" {
            Vec::new()
        } else {
            columns[8].split(';').map(entry).collect()
        };
        let mut definition = EnumDefinition {
            key_type: columns[1].parse().unwrap(),
            value_type: columns[2].parse().unwrap(),
            default_text: Arc::from(utf8(columns[3])),
            default_int: columns[4].parse().unwrap(),
            size: columns[5].parse().unwrap(),
            values: None,
            parameters: None,
        };
        match columns[6] {
            "values" => {
                let mut values = vec![None; columns[7].parse().unwrap()];
                for (index, value) in entries {
                    values[index as usize] = Some(value);
                }
                definition.values = Some(values);
            }
            "params" => definition.parameters = Some(entries.into_iter().collect()),
            "none" => {}
            _ => unreachable!(),
        }
        host.definitions
            .insert(columns[0].parse().unwrap(), Arc::new(definition));
    }
    host
}

fn push_all(ints: &[i32], strings: &[Option<String>]) -> Vec<(Opcode, Operand)> {
    let mut code = Vec::new();
    for value in strings {
        match value {
            Some(value) => code.push(text(value)),
            None => code.push(int(Opcode::PUSH_STRING_LOCAL, 0)),
        }
    }
    code.extend(ints.iter().map(|n| constant(*n)));
    code
}

fn build(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )])
}

fn failure_matches(expected: &str, kind: &VmErrorKind) -> bool {
    let parts: Vec<_> = expected.splitn(3, ':').collect();
    match (parts[1], kind) {
        ("ClassCastException", VmErrorKind::Enum(EnumFailure::ValueClass)) => true,
        ("IllegalStateException", VmErrorKind::Enum(EnumFailure::Inconsistent)) => true,
        ("NullPointerException", VmErrorKind::InvalidOperand) => true,
        ("RuntimeException", VmErrorKind::Enum(EnumFailure::Lookup { enum_id, key })) => {
            parts[2] == format!("{enum_id} {key}")
        }
        (
            "RuntimeException",
            VmErrorKind::Enum(
                EnumFailure::NoEnum
                | EnumFailure::KeyType
                | EnumFailure::ValueType
                | EnumFailure::ReverseIndex,
            ),
        ) => parts[2] == "-",
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_enum_case_matches_the_actual_kotlin_dispatch() {
    let mut host = host();
    let mut successes = 0;
    let mut failures = 0;
    let mut total = 0;
    for line in include_str!("fixtures/enum.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 7, "{line}");
        let name = columns[0];
        let opcode = ENUMS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[1])
            .unwrap();
        let ints = integers(columns[2]);
        let initial = strings(columns[3]);
        let mut code = push_all(&ints, &initial);
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let repository = build(&code);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        match columns[4] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
                successes += 1;
            }
            expected => {
                let error = result.unwrap_err();
                assert_eq!(error.script_id, 1, "{name}");
                assert_eq!(error.instruction, pushed, "{name}");
                assert!(
                    failure_matches(expected, &error.kind),
                    "{name}: {expected} vs {:?}",
                    error.kind
                );
                failures += 1;
            }
        }
        assert_eq!(vm.int_stack(), integers(columns[5]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = strings(columns[6])
            .into_iter()
            .map(|value| value.map(Arc::from))
            .collect();
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        total += 1;
    }
    println!("enum native cases={total} ok={successes} failing={failures}");
    assert_eq!(total, 23943);
    assert_eq!(successes + failures, total);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn decoded_enum_scripts_match_the_actual_kotlin_vm_including_a_parameterized_child() {
    let fixtures: &[(&[u8], &str)] = &[
        (
            include_bytes!("fixtures/vm-enums.bin"),
            include_str!("fixtures/vm-enums.decode.tsv"),
        ),
        (
            include_bytes!("fixtures/vm-enums-child.bin"),
            include_str!("fixtures/vm-enums-child.decode.tsv"),
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
    let repository = BTreeMap::from([
        (
            1,
            Script::decode(include_bytes!("fixtures/vm-enums.bin")).unwrap(),
        ),
        (
            900049,
            Script::decode(include_bytes!("fixtures/vm-enums-child.bin")).unwrap(),
        ),
    ]);
    let mut host = host();
    let mut vm = Vm::default();
    let execution = vm
        .run_with_host(1, &repository, Arguments::default(), &mut host)
        .unwrap();
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
    assert_eq!(&actual, include_str!("fixtures/vm-enums.vm.tsv"));
}

fn valid_inputs(opcode: Opcode) -> (Vec<i32>, Vec<Option<String>>) {
    match opcode {
        Opcode::ENUM => (vec![105, 105, 1, 2], vec![]),
        Opcode::ENUM_STRING => (vec![2, 0], vec![]),
        Opcode::ENUM_HASOUTPUT => (vec![105, 1, 10], vec![]),
        Opcode::ENUM_HASOUTPUT_STRING | Opcode::ENUM_GETREVERSECOUNT_STRING => {
            (vec![2], vec![Some("a".into())])
        }
        Opcode::ENUM_GETOUTPUTCOUNT => (vec![1], vec![]),
        Opcode::ENUM_GETREVERSECOUNT => (vec![105, 1, 10], vec![]),
        Opcode::ENUM_GETREVERSEINDEX => (vec![105, 105, 1, 10, 1], vec![]),
        Opcode::ENUM_GETREVERSEINDEX_STRING => (vec![105, 2, 1], vec![Some("a".into())]),
        _ => unreachable!(),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn enum_ops_are_unsupported_without_an_enum_host_and_never_reach_a_host_that_lacks_one() {
    for &opcode in ENUMS {
        let (ints, strings) = valid_inputs(opcode);
        let mut code = push_all(&ints, &strings);
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let repository = build(&code);
        let expected = VmError {
            script_id: 1,
            instruction: pushed,
            kind: VmErrorKind::UnsupportedOpcode(opcode),
        };
        assert_eq!(
            Vm::default()
                .run(1, &repository, Arguments::default())
                .unwrap_err(),
            expected
        );
        assert_eq!(
            Vm::default()
                .run_with_host(1, &repository, Arguments::default(), &mut Unavailable)
                .unwrap_err(),
            expected
        );
        let mut hosted = host();
        Vm::default()
            .run_with_host(1, &repository, Arguments::default(), &mut hosted)
            .unwrap();
        assert_eq!(hosted.lookups.get(), 1);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn enum_host_failures_are_reported_and_id_minus_one_skips_the_host_where_native_rejects_it() {
    let mut failing = EnumHost {
        failure: Some(HostError::State("offline".into())),
        ..host()
    };
    let (ints, strings) = valid_inputs(Opcode::ENUM);
    let mut code = push_all(&ints, &strings);
    let pushed = code.len();
    code.extend([int(Opcode::ENUM, 0), ret()]);
    let error = Vm::default()
        .run_with_host(1, &build(&code), Arguments::default(), &mut failing)
        .unwrap_err();
    assert_eq!(error.instruction, pushed);
    assert_eq!(
        error.kind,
        VmErrorKind::Host(HostError::State("offline".into()))
    );
    for &(opcode, _, _) in ARITY {
        let checked = !matches!(
            opcode,
            Opcode::ENUM | Opcode::ENUM_STRING | Opcode::ENUM_GETOUTPUTCOUNT
        );
        let (mut ints, strings) = valid_inputs(opcode);
        let slot = match opcode {
            Opcode::ENUM => 2,
            Opcode::ENUM_STRING | Opcode::ENUM_GETOUTPUTCOUNT => 0,
            Opcode::ENUM_HASOUTPUT | Opcode::ENUM_GETREVERSECOUNT => 1,
            Opcode::ENUM_HASOUTPUT_STRING | Opcode::ENUM_GETREVERSECOUNT_STRING => 0,
            Opcode::ENUM_GETREVERSEINDEX => 2,
            _ => 1,
        };
        ints[slot] = -1;
        let mut code = push_all(&ints, &strings);
        code.extend([int(opcode, 0), ret()]);
        let mut hosted = host();
        let result =
            Vm::default().run_with_host(1, &build(&code), Arguments::default(), &mut hosted);
        if checked {
            assert_eq!(
                result.unwrap_err().kind,
                VmErrorKind::Enum(EnumFailure::NoEnum)
            );
            assert_eq!(hosted.lookups.get(), 0);
        } else {
            assert_eq!(hosted.lookups.get(), 1);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn enum_underflow_reports_the_missing_stack_and_leaves_stacks_untouched() {
    for &(opcode, int_arity, string_arity) in ARITY {
        for have in 0..int_arity {
            let ints: Vec<i32> = (0..have as i32).collect();
            let strings = vec![Some("a".to_string()); string_arity];
            let mut code = push_all(&ints, &strings);
            let pushed = code.len();
            code.extend([int(opcode, 0), ret()]);
            let mut hosted = host();
            let mut vm = Vm::default();
            let error = vm
                .run_with_host(1, &build(&code), Arguments::default(), &mut hosted)
                .unwrap_err();
            assert_eq!(error.instruction, pushed);
            assert_eq!(error.kind, VmErrorKind::StackUnderflow(StackKind::Int));
            assert_eq!(vm.int_stack(), ints.as_slice());
            assert_eq!(vm.string_stack().len(), string_arity);
            assert_eq!(hosted.lookups.get(), 1);
        }
        if string_arity == 1 {
            let ints: Vec<i32> = vec![0; int_arity];
            let mut code = push_all(&ints, &[]);
            code.extend([int(opcode, 0), ret()]);
            let mut vm = Vm::default();
            let error = vm
                .run_with_host(1, &build(&code), Arguments::default(), &mut host())
                .unwrap_err();
            assert_eq!(error.kind, VmErrorKind::StackUnderflow(StackKind::String));
            assert_eq!(vm.int_stack(), ints.as_slice());
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn enum_fuel_boundary_stops_before_the_operation_without_consulting_the_host() {
    for &opcode in ENUMS {
        let (ints, strings) = valid_inputs(opcode);
        let mut code = push_all(&ints, &strings);
        let pushed = code.len() as u64;
        code.extend([int(opcode, 0), ret()]);
        let repository = build(&code);
        let mut hosted = host();
        let mut vm = Vm::new(VmLimits {
            max_instructions: pushed + 2,
            ..VmLimits::default()
        });
        let execution = vm
            .run_with_host(1, &repository, Arguments::default(), &mut hosted)
            .unwrap();
        assert_eq!(execution.instructions_executed, pushed + 2);
        let mut hosted = host();
        let mut vm = Vm::new(VmLimits {
            max_instructions: pushed,
            ..VmLimits::default()
        });
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut hosted)
            .unwrap_err();
        assert_eq!(error.kind, VmErrorKind::InstructionLimit);
        assert_eq!(error.instruction, pushed as usize);
        assert_eq!(hosted.lookups.get(), 0);
        assert_eq!(
            vm.int_stack().len() + vm.string_stack().len(),
            pushed as usize
        );
        let mut hosted = host();
        let mut vm = Vm::new(VmLimits {
            max_instructions: pushed + 1,
            ..VmLimits::default()
        });
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut hosted)
            .unwrap_err();
        assert_eq!(error.kind, VmErrorKind::InstructionLimit);
        assert_eq!(error.instruction, pushed as usize + 1);
        assert_eq!(hosted.lookups.get(), 1);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn enum_string_results_obey_the_string_limit_and_the_string_stack_capacity() {
    let mut hosted = host();
    hosted.definitions.insert(
        500,
        Arc::new(EnumDefinition {
            default_text: Arc::from("x".repeat(64)),
            ..EnumDefinition::default()
        }),
    );
    let code = [
        constant(500),
        constant(0),
        int(Opcode::ENUM_STRING, 0),
        ret(),
    ];
    let repository = build(&code);
    let mut vm = Vm::new(VmLimits {
        max_string_bytes: 64,
        ..VmLimits::default()
    });
    vm.run_with_host(1, &repository, Arguments::default(), &mut hosted)
        .unwrap();
    assert_eq!(
        vm.string_stack()[0].as_deref(),
        Some("x".repeat(64).as_str())
    );
    let mut vm = Vm::new(VmLimits {
        max_string_bytes: 63,
        ..VmLimits::default()
    });
    let error = vm
        .run_with_host(1, &repository, Arguments::default(), &mut hosted)
        .unwrap_err();
    assert_eq!(error.kind, VmErrorKind::StringLimit);
    assert_eq!(error.instruction, 2);
    assert!(vm.int_stack().is_empty());
    let mut code: Vec<_> = (0..1000).map(|_| text("")).collect();
    code.extend([constant(1), constant(0), int(Opcode::ENUM_STRING, 0), ret()]);
    let mut vm = Vm::default();
    let error = vm
        .run_with_host(1, &build(&code), Arguments::default(), &mut host())
        .unwrap_err();
    assert_eq!(error.kind, VmErrorKind::StackOverflow(StackKind::String));
    assert_eq!(vm.string_stack().len(), 1000);
}

fn operand_for(opcode: Opcode) -> Operand {
    match opcode {
        Opcode::PUSH_CONSTANT_STRING => Operand::String(Arc::from("")),
        Opcode::PUSH_LONG_CONSTANT => Operand::Long(0),
        _ => Operand::Int(0),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn exact_supported_and_unsupported_opcode_counts_are_derived_by_execution() {
    let mut supported = Vec::new();
    let mut unsupported = Vec::new();
    let mut undecodable = Vec::new();
    for metadata in OPCODES {
        let opcode = metadata.opcode;
        let bytes = binary(
            &[(opcode, operand_for(opcode))],
            counts(1, 1, 1),
            zero(),
            &[vec![(0, 0)]],
        );
        let Ok(decoded) = Script::decode(&bytes) else {
            undecodable.push(metadata.name);
            continue;
        };
        let repository = BTreeMap::from([(1, decoded)]);
        let mut vm = Vm::default();
        let mut hosted = host();
        hosted.varc = Some(VarcState::new(&[], 0, VarcLimits::default()).unwrap());
        hosted.components = Some(ComponentModel::new(0, 0, 0));
        match vm.run_with_host(1, &repository, Arguments::default(), &mut hosted) {
            Err(VmError {
                instruction: 0,
                kind: VmErrorKind::UnsupportedOpcode(reported),
                ..
            }) => {
                assert_eq!(reported, opcode);
                unsupported.push(metadata);
            }
            _ => supported.push(metadata),
        }
    }
    assert!(undecodable.is_empty(), "{undecodable:?}");
    let mut by_family: BTreeMap<u8, (usize, usize)> = BTreeMap::new();
    for metadata in &supported {
        by_family.entry(metadata.family).or_default().0 += 1;
    }
    for metadata in &unsupported {
        by_family.entry(metadata.family).or_default().1 += 1;
    }
    let mut report = String::new();
    write!(
        report,
        "{{\n  \"total\": {},\n  \"supported\": {},\n  \"unsupported\": {},\n  \"families\": {{",
        OPCODES.len(),
        supported.len(),
        unsupported.len()
    )
    .unwrap();
    for (index, (family, (yes, no))) in by_family.iter().enumerate() {
        write!(
            report,
            "{}\n    \"{family}\": {{\"supported\": {yes}, \"unsupported\": {no}}}",
            if index == 0 { "" } else { "," }
        )
        .unwrap();
    }
    report.push_str("\n  },\n  \"supported_names\": [");
    for (index, metadata) in supported.iter().enumerate() {
        write!(
            report,
            "{}\"{}\"",
            if index == 0 { "" } else { ", " },
            metadata.name
        )
        .unwrap();
    }
    report.push_str("]\n}\n");
    println!("{report}");
    #[cfg(not(target_arch = "wasm32"))]
    if let Ok(path) = std::env::var("CS2_CLASSIFICATION_OUT") {
        std::fs::write(path, &report).unwrap();
    }
    assert_eq!(OPCODES.len(), 1010);
    assert_eq!(supported.len(), 646);
    assert_eq!(unsupported.len(), 364);
    for &opcode in ENUMS {
        assert!(supported.iter().any(|metadata| metadata.opcode == opcode));
    }
}
