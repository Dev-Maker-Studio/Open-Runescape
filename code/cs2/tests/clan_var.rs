#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct VarClanHost {
    clan: ClanState,
    defs: ClanVarDefs,
}

impl VarHost for VarClanHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn clan_state(&self) -> Result<&ClanState, HostError> {
        Ok(&self.clan)
    }

    fn clan_var_defs(&self) -> Result<&ClanVarDefs, HostError> {
        Ok(&self.defs)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn unhex(value: &str) -> Vec<u8> {
    (0..value.len())
        .step_by(2)
        .map(|at| u8::from_str_radix(&value[at..at + 2], 16).unwrap())
        .collect()
}

fn value(text: &str) -> ClanVarValue {
    let (kind, rest) = text.split_at(1);
    match kind {
        "i" => ClanVarValue::Int(rest.parse().unwrap()),
        "l" => ClanVarValue::Long(rest.parse().unwrap()),
        _ => ClanVarValue::Text(rest.to_string()),
    }
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(message)) => match message.as_str() {
            "array index out of bounds" => expected == "ArrayIndexOutOfBoundsException",
            "class cast" => expected == "ClassCastException",
            "illegal argument" => expected == "IllegalArgumentException",
            _ => false,
        },
        VmErrorKind::StackUnderflow(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

fn find_opcode(code: &str) -> Opcode {
    opcode_named(if code == "SA" {
        "ACTIVECLANSETTINGS_FIND_AFFINED"
    } else {
        "ACTIVECLANSETTINGS_FIND_LISTENED"
    })
}

#[derive(Default)]
struct State {
    game: i32,
    length: i32,
    vars: BTreeMap<usize, ClanVarValue>,
    settings: BTreeMap<i32, ClanSettings>,
    main: i32,
    guest: i32,
}

impl State {
    fn host(&self, defs: &ClanVarDefs) -> VarClanHost {
        VarClanHost {
            clan: ClanState {
                settings: self.settings.get(&self.main).cloned(),
                guest_settings: self.settings.get(&self.guest).cloned(),
                clan_vars: (self.length >= 0).then(|| {
                    (0..self.length as usize)
                        .map(|slot| self.vars.get(&slot).cloned())
                        .collect()
                }),
                game_id: Some(self.game),
                ..ClanState::default()
            },
            defs: defs.clone(),
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_clan_var_opcode_matches_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/clan-var.native.tsv");
    let mut defs = ClanVarDefs {
        vars: vec![None; 10],
        settings: vec![None; 10],
    };
    let mut states: BTreeMap<i32, State> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "D" => {
                let file = Some(unhex(c[3]));
                let slot = number(c[2]) as usize;
                if c[1] == "v" {
                    defs.vars[slot] = file;
                } else {
                    defs.settings[slot] = file;
                }
            }
            "G" => {
                let state = states.entry(number(c[1])).or_default();
                state.game = number(c[2]);
                state.length = number(c[3]);
            }
            "v" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                state.vars.insert(number(c[2]) as usize, value(c[3]));
            }
            "S" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                state.settings.insert(
                    number(c[2]),
                    ClanSettings {
                        variables: (c[3] != "1").then(BTreeMap::new),
                        ..ClanSettings::default()
                    },
                );
            }
            "w" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                let settings = state.settings.get_mut(&number(c[2])).unwrap();
                settings
                    .variables
                    .as_mut()
                    .unwrap()
                    .insert(number(c[3]), value(c[4]));
            }
            "H" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                state.main = number(c[2]);
                state.guest = number(c[3]);
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for c in &cases {
        let name = c[1];
        let opcode = opcode_named(name);
        let mut instructions: Vec<_> = Vec::new();
        if c[3] != "-" {
            for item in c[3].split(',') {
                instructions.push(int(find_opcode(item), 0));
            }
        }
        let initial = instructions.len();
        instructions.push(int(opcode, number(c[4])));
        instructions.push(ret());
        let mut host = states[&number(c[2])].host(&defs);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} state {} {} id {}", c[2], c[3], c[4]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, c[5]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let ints: Vec<String> = vm.int_stack().iter().map(|v| v.to_string()).collect();
                let ints = if ints.is_empty() { "-".to_string() } else { ints.join(",") };
                let texts: Vec<String> = vm
                    .string_stack()
                    .iter()
                    .map(|item| match item {
                        None => "~".to_string(),
                        Some(item) => format!("\"{item}\""),
                    })
                    .collect();
                let texts = if texts.is_empty() { "-".to_string() } else { texts.join("|") };
                let longs: Vec<String> = vm.long_stack().iter().map(|v| v.to_string()).collect();
                let longs = if longs.is_empty() { "-".to_string() } else { longs.join(",") };
                if ints != c[6] || texts != c[7] || longs != c[8] {
                    mismatches.push(format!(
                        "{label}: stacks {ints} / {texts} / {longs} vs {} / {} / {}",
                        c[6], c[7], c[8]
                    ));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
    }
    println!("clan var cases={} mismatches={}", cases.len(), mismatches.len());
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), expected_total);
    assert_eq!(per_opcode.len(), 8);
}

#[test]
fn clan_var_opcodes_are_unsupported_without_host_clan_state() {
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
    for opcode in [
        Opcode::PUSH_VARCLAN,
        Opcode::PUSH_VARCLANBIT,
        Opcode::LOAD_CLAN_VAR_LONG,
        Opcode::LOAD_CLAN_VAR_STRING,
        Opcode::PUSH_VARCLANSETTING,
        Opcode::PUSH_VARCLANSETTINGBIT,
        Opcode::PUSH_VARCLANSETTING_LONG,
        Opcode::PUSH_VARCLANSETTING_STRING,
    ] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&[int(opcode, 0), ret()], zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
