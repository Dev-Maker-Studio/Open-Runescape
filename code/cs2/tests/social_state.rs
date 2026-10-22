#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct SocialHost {
    social: SocialState,
}

impl VarHost for SocialHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn social_state(&self) -> Result<&SocialState, HostError> {
        Ok(&self.social)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn nullable(value: &str) -> Option<String> {
    (value != "~").then(|| value.to_string())
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value
            .split('|')
            .map(|item| {
                (item != "~").then(|| item.trim_start_matches('"').trim_end_matches('"').to_string())
            })
            .collect()
    }
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

fn run(opcode: Opcode, code: Vec<(Opcode, Operand)>, social: SocialState) -> (Vm, Result<Execution, VmError>) {
    let _ = opcode;
    let mut host = SocialHost { social };
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, zero(), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
    (vm, result)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_social_case_matches_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/social-state.native.tsv");
    let mut states: BTreeMap<i32, SocialState> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    let mut player_slots: BTreeMap<i32, Vec<Option<FriendsChatPlayer>>> = BTreeMap::new();
    let mut players_null: BTreeMap<i32, bool> = BTreeMap::new();
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "F" => {
                let state = states.entry(number(c[1])).or_default();
                state.friends_loaded = number(c[2]);
            }
            "f" => states.entry(number(c[1])).or_default().friends.push(Friend {
                display_name: c[2].to_string(),
                referrer: c[3] == "1",
                world_id: number(c[4]),
                world_name: c[5].to_string(),
                friends_chat_rank: number(c[6]),
                platform: number(c[7]),
                previous_display_name: c[8].to_string(),
                world_flags: number(c[9]),
            }),
            "I" => {}
            "i" => states.entry(number(c[1])).or_default().ignores.push(Ignore {
                unfiltered_username: nullable(c[2]),
                display_name: nullable(c[3]),
                temporary: c[4] == "1",
            }),
            "C" => {
                let id = number(c[1]);
                let state = states.entry(id).or_default();
                state.fc_name = nullable(c[2]);
                state.fc_owner_name = nullable(c[3]);
                players_null.insert(id, c[4] == "1");
                player_slots.insert(id, vec![None; number(c[5]) as usize]);
                state.fc_player_count = number(c[6]);
                state.fc_min_rank_can_kick = number(c[7]) as i8;
                state.my_fc_rank = number(c[8]) as i8;
                state.my_display_name = (c[9] == "1").then(|| nullable(c[10]));
                state.country = number(c[11]);
            }
            "p" => {
                let id = number(c[1]);
                player_slots.get_mut(&id).unwrap()[number(c[2]) as usize] =
                    Some(FriendsChatPlayer {
                        rank: number(c[3]) as i8,
                        username: nullable(c[4]),
                        display_name: nullable(c[5]),
                        world_name: nullable(c[6]),
                        world_id: number(c[7]),
                    });
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    for (id, state) in states.iter_mut() {
        if !players_null[id] {
            state.fc_players = Some(player_slots.remove(id).unwrap());
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut skipped = 0;
    for c in &cases {
        let name = c[1];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let argument = c[3];
        let mut code = Vec::new();
        let initial = match argument {
            "-" | "u" => 0,
            "n" => {
                skipped += 1;
                continue;
            }
            argument if argument.starts_with('i') => {
                code.push(constant(number(&argument[1..])));
                1
            }
            argument if cp1252(&argument[1..]).is_none() => {
                skipped += 1;
                continue;
            }
            argument => {
                code.push(text(&argument[1..]));
                1
            }
        };
        code.push(int(opcode, 0));
        code.push(ret());
        let (vm, result) = run(opcode, code, states[&number(c[2])].clone());
        let label = format!("{name} state {} {argument}", c[2]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let ints = integers(c[5]);
                let texts = strings(c[6]);
                let actual: Vec<Option<String>> = vm
                    .string_stack()
                    .iter()
                    .map(|item| item.as_ref().map(|item| item.to_string()))
                    .collect();
                if vm.int_stack() != ints.as_slice() || actual != texts {
                    mismatches.push(format!(
                        "{label}: {:?} {actual:?} vs {ints:?} {texts:?}",
                        vm.int_stack()
                    ));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                let position = if argument == "u" { 0 } else { initial };
                if error.instruction != position
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
    }
    println!(
        "social cases={} skipped={skipped} mismatches={}",
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
    assert_eq!(skipped, 54 + 108);
    assert_eq!(cases.len(), 12564);
    assert_eq!(per_opcode.len(), 26);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn a_null_string_on_the_stack_raises_a_null_pointer_like_the_kotlin_text_not_null_assertion() {
    let social = SocialState {
        friends_loaded: 2,
        ignores: vec![Ignore {
            unfiltered_username: None,
            display_name: None,
            temporary: false,
        }],
        ..SocialState::default()
    };
    for opcode in [Opcode::FRIEND_TEST, Opcode::FRIEND_GETSLOTFROMNAME, Opcode::IGNORE_TEST] {
        let (_, result) = run(
            opcode,
            vec![
                constant(0),
                int(Opcode::IGNORE_GETNAME_UNFILTERED, 0),
                int(opcode, 0),
                ret(),
            ],
            social.clone(),
        );
        let error = result.unwrap_err();
        assert_eq!(error.kind, VmErrorKind::Host(HostError::NoEntity));
        assert_eq!(error.instruction, 2);
    }
}

#[test]
fn social_opcodes_are_unsupported_without_a_host_state() {
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
    for opcode in [Opcode::FRIEND_COUNT, Opcode::CLAN_ISSELF, Opcode::PLAYERCOUNTRY] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(
                &[constant(1), int(opcode, 0), ret()],
                zero(),
                zero(),
                &[],
            ))
            .unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
