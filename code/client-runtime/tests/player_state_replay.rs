#[allow(dead_code)]
#[path = "../../cs2/tests/common/mod.rs"]
mod common;

use common::*;
use openrs_client_runtime::vars::player::CurrentNpc;
use openrs_client_runtime::vars::{WorldVarHost, WorldVariables};
use openrs_cs2::*;
use std::collections::BTreeMap;

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::EntityType) => expected == "ClassCastException",
        VmErrorKind::Host(HostError::State(_)) | VmErrorKind::StackUnderflow(_) => {
            expected == "ArrayIndexOutOfBoundsException"
        }
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_var_host_replays_every_native_player_state_case() {
    let text = include_str!("../../cs2/tests/fixtures/player-state.native.tsv");
    let mut players: BTreeMap<i32, PlayerState> = BTreeMap::new();
    let mut npcs: BTreeMap<i32, NpcState> = BTreeMap::new();
    let mut types = BTreeMap::new();
    let mut bits = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "K" => {
                let player = players.entry(number(columns[1])).or_default();
                player.set_skill(
                    number(columns[2]) as usize,
                    number(columns[5]),
                    number(columns[3]),
                    number(columns[4]),
                );
            }
            "S" => {
                let player = players.entry(number(columns[1])).or_default();
                player.player_rights = number(columns[2]);
                player.player_mod_level = number(columns[3]);
                player.is_member = columns[4] == "1";
                player.member_world = columns[5] == "1";
                player.reboot_timer = number(columns[6]);
                player.run_energy = number(columns[7]);
                player.run_weight = number(columns[8]);
                player.world_id = (columns[9] != "null").then(|| number(columns[9]));
                player.my_player = (columns[10] != "null").then(|| {
                    let parts: Vec<&str> = columns[10].split(',').collect();
                    MyPlayer {
                        combat_level: number(parts[0]),
                        female: match parts[1] {
                            "n" => None,
                            value => Some(value == "1"),
                        },
                        yaw_rotation: number(parts[2]),
                        ..MyPlayer::default()
                    }
                });
                player.my_player_index = number(columns[11]);
            }
            "T" => {
                types.insert(number(columns[1]), number(columns[2]));
            }
            "B" => {
                bits.insert(
                    number(columns[1]),
                    VarnBit {
                        varn: number(columns[2]) as u16,
                        start: number(columns[3]) as u8,
                        end: number(columns[4]) as u8,
                    },
                );
            }
            "N" => {
                let npc = npcs.entry(number(columns[1])).or_default();
                npc.type_id = (columns[2] != "null").then(|| number(columns[2]));
                npc.max_stats.copy_from_slice(&integers(columns[3]));
                npc.current_stats.copy_from_slice(&integers(columns[4]));
            }
            "V" => {
                npcs.entry(number(columns[1]))
                    .or_default()
                    .varns
                    .insert(number(columns[2]), number(columns[3]));
            }
            "C" => cases.push(columns),
            "Z" => expected_total = number(columns[1]) as usize,
            other => panic!("{other}"),
        }
    }
    for npc in npcs.values_mut() {
        npc.varn_types = types.clone();
        npc.varn_bits = bits.clone();
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for columns in &cases {
        let name = columns[1];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let entity = match columns[4] {
            "npc" => "npc",
            "player" => "player",
            _ => "null",
        };
        let player = if columns[2] == "-" {
            PlayerState::default()
        } else {
            players[&number(columns[2])].clone()
        };
        let npc = if columns[3] == "-" {
            NpcState::default()
        } else {
            npcs[&number(columns[3])].clone()
        };
        let current = match entity {
            "npc" => CurrentNpc::Npc(&npc),
            "player" => CurrentNpc::Player,
            _ => CurrentNpc::None,
        };
        let mut variables = WorldVariables::new(4).unwrap();
        let mut host = WorldVarHost::new(&mut variables, 0)
            .with_player_state(&player)
            .with_current_npc(current);
        let argument = number(columns[5]);
        let initial = number(columns[6]);
        let mut code = Vec::new();
        if initial == 1 {
            code.push(constant(argument));
        }
        code.push(int(opcode, argument));
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} {} {} {entity} {argument}", columns[2], columns[3]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[7]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(columns[8]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!(
                        "{label}: ints {:?} vs {expected:?}",
                        vm.int_stack()
                    ));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial as usize
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
    }
    println!(
        "player state cases={} mismatches={}",
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
    assert_eq!(per_opcode.len(), 21);
}
