#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
    Opcode::HSVTORGB,
    Opcode::RANDOM,
    Opcode::RANDOMINC,
    Opcode::RANDOM_SOUND_PITCH,
    Opcode::DATE_MINUTES,
    Opcode::DATE_RUNEDAY,
    Opcode::DATE_YEAR,
    Opcode::DATE_RUNEDAY_TODATE,
    Opcode::FROMDATE,
    Opcode::FORMAT_DATETIME_FROM_MINUTES,
];

struct ClockHost {
    millis: i64,
    offset: i64,
    language: Option<i32>,
    draw: f64,
    draws: u32,
    table: Option<Arc<[i32]>>,
}

impl VarHost for ClockHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn clock_millis(&self) -> Result<i64, HostError> {
        Ok(self.millis)
    }

    fn random_unit(&mut self) -> Result<f64, HostError> {
        self.draws += 1;
        Ok(self.draw)
    }

    fn local_offset_millis(&self, _: i64) -> Result<i64, HostError> {
        Ok(self.offset)
    }

    fn language(&self) -> Result<Option<i32>, HostError> {
        Ok(self.language)
    }

    fn hsv_to_rgb_table(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        Ok(self.table.clone())
    }
}

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

fn utf16(value: &str) -> Option<String> {
    if value == "null" {
        return None;
    }
    let units: Vec<u16> = (0..(value.len() - 1) / 4)
        .map(|index| u16::from_str_radix(&value[1 + index * 4..5 + index * 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn run(host: &mut dyn VarHost, code: &[(Opcode, Operand)]) -> (Vm, Result<(), VmError>) {
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), host)
        .map(|_| ());
    (vm, result)
}

fn hsv_table() -> Arc<[i32]> {
    let bytes = include_bytes!("fixtures/hsv.native.bin");
    assert_eq!(bytes.len(), 65536 * 4);
    bytes
        .chunks_exact(4)
        .map(|chunk| i32::from_be_bytes(chunk.try_into().unwrap()))
        .collect()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_clock_random_and_date_case_matches_the_actual_kotlin_dispatch() {
    let table = hsv_table();
    let mut total = 0;
    let mut failing = 0;
    for line in include_str!("fixtures/clock.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 12, "{line}");
        let name = columns[0];
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[1])
            .unwrap();
        let language: i32 = columns[5].parse().unwrap();
        let mut host = ClockHost {
            millis: columns[3].parse().unwrap(),
            offset: columns[4].parse().unwrap(),
            language: (language >= 0).then_some(language),
            draw: f64::from_bits(columns[6].parse::<i64>().unwrap() as u64),
            draws: 0,
            table: (columns[7] == "1").then(|| Arc::clone(&table)),
        };
        let mut code: Vec<_> = integers(columns[2]).into_iter().map(constant).collect();
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(&mut host, &code);
        match columns[9] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
            }
            error if error.starts_with("error:") => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::InvalidOperand, "{name}");
                failing += 1;
            }
            other => panic!("{name}: unexpected {other}"),
        }
        assert_eq!(vm.int_stack(), integers(columns[10]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = if columns[11] == "-" {
            Vec::new()
        } else {
            columns[11]
                .split(',')
                .map(|value| utf16(value).map(Arc::from))
                .collect()
        };
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        assert_eq!(host.draws, columns[8].parse::<u32>().unwrap(), "{name} draws");
        total += 1;
    }
    assert_eq!(total, 11466);
    assert!(failing > 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn clock_operations_without_a_clock_host_stay_unsupported() {
    for opcode in OPERATIONS {
        let (_, result) = run(&mut Bare, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
        let repository = BTreeMap::from([(1, script(&[int(*opcode, 0), ret()]))]);
        let error = Vm::default().run(1, &repository, Arguments::default());
        assert_eq!(
            error.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn clock_operations_report_underflow_when_the_host_is_available() {
    for opcode in OPERATIONS {
        let mut host = ClockHost {
            millis: 0,
            offset: 0,
            language: Some(0),
            draw: 0.5,
            draws: 0,
            table: Some(hsv_table()),
        };
        let (_, result) = run(&mut host, &[int(*opcode, 0), ret()]);
        let clock_only = matches!(
            opcode,
            Opcode::DATE_MINUTES | Opcode::DATE_RUNEDAY | Opcode::DATE_YEAR
        );
        if clock_only {
            result.unwrap();
        } else {
            assert_eq!(
                result.unwrap_err().kind,
                VmErrorKind::StackUnderflow(StackKind::Int)
            );
        }
    }
}
