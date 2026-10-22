#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

struct DialogHost {
    outbox: Outbox,
}

impl VarHost for DialogHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(_)) | VmErrorKind::StackUnderflow(_) => {
            expected == "ArrayIndexOutOfBoundsException"
        }
        _ => false,
    }
}

fn hex(packet: &ClientPacket) -> String {
    let mut out = format!("{:02x}", packet.opcode);
    for byte in &packet.payload {
        write!(out, "{byte:02x}").unwrap();
    }
    out
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_dialog_opcode_matches_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/dialog.native.tsv");
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut skipped = 0;
    let mut packets_checked = 0;
    for c in &cases {
        let name = c[1];
        let opcode = opcode_named(name);
        let mut instructions = Vec::new();
        if c[2] != "-" {
            for value in c[2].split(',') {
                instructions.push(constant(number(value)));
            }
        }
        if c[3] != "<none>" {
            if c[3] == "~" || cp1252(c[3]).is_none() {
                skipped += 1;
                continue;
            }
            instructions.push(text(c[3]));
        }
        let initial = instructions.len();
        instructions.push(int(opcode, 0));
        instructions.push(ret());
        let mut host = DialogHost {
            outbox: Outbox::default(),
        };
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} {} {}", c[2], c[3]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox.packets.iter().map(hex).collect::<Vec<_>>().join("|")
        };
        let mut problems = Vec::new();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let ints: Vec<String> = vm.int_stack().iter().map(|v| v.to_string()).collect();
                let ints = if ints.is_empty() { "-".to_string() } else { ints.join(",") };
                let texts = if vm.string_stack().is_empty() { "-" } else { "?" };
                if ints != c[5] || texts != c[6] {
                    problems.push(format!("stacks {ints} / {texts} vs {} / {}", c[5], c[6]));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    problems.push(format!("error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => problems.push(format!("{expected} vs {actual:?}")),
        }
        if packets != c[7] {
            problems.push(format!("packets {packets} vs {}", c[7]));
        }
        if packets != "-" {
            packets_checked += 1;
        }
        if !problems.is_empty() {
            mismatches.push(format!("{label}: {}", problems.join("; ")));
        }
    }
    println!(
        "dialog cases={} skipped={skipped} packets={packets_checked} mismatches={}",
        cases.len(),
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), expected_total);
    assert_eq!(per_opcode.len(), 6);
    assert!(packets_checked > 150);
}

#[test]
fn dialog_opcodes_are_unsupported_without_an_outbox() {
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
    for opcode in [Opcode::RESUME_OBJDIALOG, Opcode::RESUME_HSLDIALOG] {
        let instructions = [constant(5), int(opcode, 0), ret()];
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
