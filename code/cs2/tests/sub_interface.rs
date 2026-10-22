#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct SubHost {
    model: ComponentModel,
}

impl VarHost for SubHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn components(&mut self) -> Result<&mut ComponentModel, HostError> {
        Ok(&mut self.model)
    }
}

struct BareHost;

impl VarHost for BareHost {
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

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

fn run(opcode: Opcode, host: &mut dyn VarHost, ints: &[i32]) -> Result<Vm, VmError> {
    let mut code = Vec::new();
    for index in 0..ints.len() {
        code.push(int(Opcode::PUSH_INT_LOCAL, index as i32));
    }
    code.push(int(opcode, 0));
    code.push(ret());
    let counts = counts(ints.len() as u16, 0, 0);
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts, counts, &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    vm.run_with_host(
        1,
        &repository,
        Arguments {
            ints,
            strings: &[],
            longs: &[],
        },
        host,
    )
    .map(|_| vm)
}

fn entries_text(model: &ComponentModel) -> String {
    let parts: Vec<String> = model
        .open_entries()
        .iter()
        .map(|(hash, interface, overlay)| format!("{hash}:{interface}:{overlay}"))
        .collect();
    if parts.is_empty() {
        "-".into()
    } else {
        parts.join(",")
    }
}

fn settings_text(model: &ComponentModel) -> String {
    let mut parts: Vec<String> = model
        .slot_settings_entries()
        .iter()
        .map(|(hash, slot, settings, interface)| format!("{hash}:{slot}:{settings}:{interface}"))
        .collect();
    parts.sort();
    if parts.is_empty() {
        "-".into()
    } else {
        parts.join(",")
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sub_client_ops_match_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/sub-interface.native.tsv");
    let mut states: BTreeMap<i32, (String, String)> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "H" => {
                states.insert(number(c[1]), (c[2].to_string(), c[3].to_string()));
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    assert_eq!(cases.len(), expected_total);
    let mut mismatches = Vec::new();
    for c in &cases {
        let (open, settings) = &states[&number(c[2])];
        let mut model = ComponentModel::new(0, 765, 503);
        if open != "-" {
            for entry in open.split(',') {
                let p: Vec<i32> = entry.split(':').map(number).collect();
                model.set_open_interface_with_overlay(p[0], p[1], p[2]);
            }
        }
        if settings != "-" {
            for entry in settings.split(',') {
                let p: Vec<i32> = entry.split(':').map(number).collect();
                model.set_slot_settings(
                    p[0],
                    p[1],
                    Some(ComponentSettings {
                        hash: p[2],
                        interface_id: p[3],
                    }),
                );
            }
        }
        let ints: Vec<i32> = c[3].split(':').map(number).collect();
        let mut host = SubHost { model };
        let result = run(opcode_named(c[1]), &mut host, &ints);
        assert_eq!(c[4], "ok", "{c:?}");
        match result {
            Ok(vm) => {
                let stack = if vm.int_stack().is_empty() {
                    "-".to_string()
                } else {
                    String::new()
                };
                let actual = (
                    stack,
                    entries_text(&host.model),
                    settings_text(&host.model),
                );
                if actual != ("-".to_string(), c[6].to_string(), c[7].to_string()) || c[5] != "-" {
                    mismatches.push(format!("{c:?} vs {actual:?}"));
                }
            }
            Err(error) => mismatches.push(format!("{c:?} error {error:?}")),
        }
    }
    assert!(
        mismatches.is_empty(),
        "{} mismatches: {:?}",
        mismatches.len(),
        &mismatches[..mismatches.len().min(5)]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sub_client_ops_are_unsupported_without_a_component_host() {
    for name in ["IF_OPENSUBCLIENT", "IF_CLOSESUBCLIENT"] {
        let ints = if name == "IF_OPENSUBCLIENT" { vec![1, 2] } else { vec![1] };
        let error = run(opcode_named(name), &mut BareHost, &ints).err().unwrap();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(_)), "{name}");
    }
}
