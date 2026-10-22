#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct EntityHost {
    state: EntityState,
    varps: BTreeMap<i32, i32>,
    bits: BTreeMap<i32, Varbit>,
}

impl VarHost for EntityHost {
    fn varp(&self, id: i32) -> Result<i32, HostError> {
        Ok(self.varps.get(&id).copied().unwrap_or(0))
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, id: i32) -> Result<VarbitLookup, HostError> {
        Ok(self
            .bits
            .get(&id)
            .map_or(VarbitLookup::Absent, |bit| VarbitLookup::Defined(*bit)))
    }

    fn entity_state(&self) -> Result<&EntityState, HostError> {
        Ok(&self.state)
    }

    fn entity_state_mut(&mut self) -> Result<&mut EntityState, HostError> {
        Ok(&mut self.state)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn decode(value: &str) -> Option<String> {
    if value == "-" {
        return None;
    }
    let digits = &value[1..];
    let bytes: Vec<u8> = (0..digits.len() / 2)
        .map(|index| u8::from_str_radix(&digits[index * 2..index * 2 + 2], 16).unwrap())
        .collect();
    Some(String::from_utf8(bytes).unwrap())
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn texts(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(decode).collect()
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

fn common(columns: &[&str], start: usize) -> EntityCommon {
    EntityCommon {
        player_index: number(columns[start]),
        loaded_interface_id: number(columns[start + 1]),
        interface_id_to_load: number(columns[start + 2]),
        model_min_y: number(columns[start + 3]),
        hint_text: decode(columns[start + 4]),
        ground_decoration_offset: None,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_entity_state_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/entity-state.native.tsv");
    let mut base = EntityState::default();
    let mut varps = BTreeMap::new();
    let mut bits = BTreeMap::new();
    let mut players: BTreeMap<i32, PlayerEntityState> = BTreeMap::new();
    let mut npcs: BTreeMap<i32, NpcEntityState> = BTreeMap::new();
    let mut grounds: BTreeMap<i32, GroundItemState> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "X" => {
                bits.insert(
                    number(columns[1]),
                    Varbit {
                        varp: number(columns[2]) as u16,
                        start: number(columns[3]) as u8,
                        end: number(columns[4]) as u8,
                    },
                );
            }
            "V" => {
                varps.insert(number(columns[1]), number(columns[2]));
            }
            "A" => {
                base.bas_icon_heights
                    .insert(number(columns[1]), number(columns[2]));
            }
            "T" => {
                base.npc_types.insert(
                    number(columns[1]),
                    NpcTypeInfo {
                        name: decode(columns[2]).unwrap(),
                        visible: columns[3] == "1",
                        icon_height: number(columns[4]),
                        bas_id: number(columns[5]),
                        alternative_ids: match columns[6] {
                            "none" => None,
                            "empty" => Some(Vec::new()),
                            list => Some(integers(list)),
                        },
                        varp: number(columns[7]),
                        varpbit: number(columns[8]),
                    },
                );
            }
            "F" => base.friend_names.push(decode(columns[1]).unwrap()),
            "P" => {
                players.insert(
                    number(columns[1]),
                    PlayerEntityState {
                        common: common(&columns, 7),
                        username: decode(columns[2]),
                        display_name: decode(columns[3]),
                        prefix_title: decode(columns[4]),
                        postfix_title: decode(columns[5]),
                        render_animation: number(columns[6]),
                    },
                );
            }
            "N" => {
                let state = common(&columns, 5);
                npcs.insert(
                    number(columns[1]),
                    NpcEntityState {
                        common: state,
                        type_id: (columns[2] != "-").then(|| number(columns[2])),
                        modified_name: decode(columns[3]),
                        current_render_animation: number(columns[4]),
                    },
                );
            }
            "G" => {
                grounds.insert(
                    number(columns[1]),
                    GroundItemState {
                        tile_height_offset: number(columns[2]),
                    },
                );
            }
            "C" => cases.push(columns),
            "Z" => expected_total = number(columns[1]) as usize,
            other => panic!("{other}"),
        }
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
        let kind = columns[2];
        let index = number(columns[3]);
        let mut state = base.clone();
        state.current = match kind {
            "player" => Some(CurrentEntity::Player(players[&index].clone())),
            "npc" => Some(CurrentEntity::Npc(npcs[&index].clone())),
            _ => None,
        };
        state.ground_item = (number(columns[4]) >= 0).then(|| grounds[&number(columns[4])].clone());
        state.entity_target = number(columns[5]);
        state.public_filter = number(columns[6]);
        state.my_display_name = decode(columns[7]);
        let mut host = EntityHost {
            state,
            varps: varps.clone(),
            bits: bits.clone(),
        };
        let argument = number(columns[8]);
        let initial = number(columns[9]);
        let mut code = Vec::new();
        if initial == 1 {
            code.push(constant(argument));
        }
        code.push(int(opcode, 0));
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} {kind} {index} ground={} {argument}", columns[4]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[10]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(columns[11]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!(
                        "{label}: ints {:?} vs {expected:?}",
                        vm.int_stack()
                    ));
                }
                let strings: Vec<Option<String>> = vm
                    .string_stack()
                    .iter()
                    .map(|value| value.as_ref().map(|text| text.to_string()))
                    .collect();
                if strings != texts(columns[12]) {
                    mismatches.push(format!(
                        "{label}: strings {strings:?} vs {:?}",
                        texts(columns[12])
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
        if let Some(current) = &host.state.current {
            let common = current.common();
            if common.loaded_interface_id != number(columns[13])
                || common.interface_id_to_load != number(columns[14])
            {
                mismatches.push(format!(
                    "{label}: interfaces {} {} vs {} {}",
                    common.loaded_interface_id,
                    common.interface_id_to_load,
                    columns[13],
                    columns[14]
                ));
            }
        }
    }
    println!(
        "entity state cases={} mismatches={}",
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
    assert_eq!(per_opcode.len(), 10);
}

#[test]
fn entity_opcodes_are_unsupported_without_a_host_state() {
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
        Opcode::GET_NPC_NAME,
        Opcode::IS_TARGETED_ENTITY,
        Opcode::GET_OBJ_OVERLAY_HEIGHT,
    ] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&[int(opcode, 0), ret()], counts(0, 1, 0), zero(), &[]))
                .unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
