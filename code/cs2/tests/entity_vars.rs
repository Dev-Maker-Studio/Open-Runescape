#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

#[derive(Clone, Copy, PartialEq)]
enum Entity {
    Player,
    Npc,
    Null,
}

struct EntityHost {
    entity: Entity,
    stored: BTreeMap<i32, i32>,
    types: BTreeMap<i32, i32>,
    varbits: BTreeMap<i32, VarbitLookup>,
}

impl EntityHost {
    fn check(&self) -> Result<(), HostError> {
        match self.entity {
            Entity::Player => Ok(()),
            Entity::Npc => Err(HostError::EntityType),
            Entity::Null => Err(HostError::NoEntity),
        }
    }
}

impl VarHost for EntityHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn entity_varp(&self, id: i32) -> Result<i32, HostError> {
        self.check()?;
        if let Some(value) = self.stored.get(&id) {
            return Ok(*value);
        }
        match self.types.get(&id) {
            Some(105) => Ok(0),
            Some(_) => Ok(-1),
            None => Err(HostError::InvalidVarp(id)),
        }
    }

    fn entity_varbit(&self, id: i32) -> Result<VarbitLookup, HostError> {
        self.check()?;
        self.varbits
            .get(&id)
            .copied()
            .ok_or(HostError::InvalidVarbit(id))
    }
}

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
        VmErrorKind::Host(HostError::NoEntity)
        | VmErrorKind::Host(HostError::InvalidVarp(_))
        | VmErrorKind::Host(HostError::InvalidVarbit(_)) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::EntityType) => expected == "ClassCastException",
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_entity_variable_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/entity-vars.native.tsv");
    let mut stored = BTreeMap::new();
    let mut types = BTreeMap::new();
    let mut varbits = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "T" => {
                types.insert(number(columns[1]), number(columns[2]));
            }
            "V" => {
                stored.insert(number(columns[1]), number(columns[2]));
            }
            "B" => {
                varbits.insert(
                    number(columns[1]),
                    VarbitLookup::Defined(Varbit {
                        varp: number(columns[2]) as u16,
                        start: number(columns[3]) as u8,
                        end: number(columns[4]) as u8,
                    }),
                );
            }
            "A" => {
                varbits.insert(number(columns[1]), VarbitLookup::Absent);
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
        let entity = match columns[2] {
            "player" => Entity::Player,
            "npc" => Entity::Npc,
            "null" => Entity::Null,
            other => panic!("{other}"),
        };
        let operand = number(columns[3]);
        let mut host = EntityHost {
            entity,
            stored: stored.clone(),
            types: types.clone(),
            varbits: varbits.clone(),
        };
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(
                &[int(opcode, operand), ret()],
                counts(0, 1, 0),
                zero(),
                &[],
            ))
            .unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} {} {operand}", columns[2]);
        let mut expected_ints = integers(columns[5]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[4]) {
            (Ok(_), "ok") => entry.0 += 1,
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                expected_ints.pop();
                if error.instruction != 0
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
        if vm.int_stack() != expected_ints.as_slice() {
            mismatches.push(format!(
                "{label}: ints {:?} vs {expected_ints:?}",
                vm.int_stack()
            ));
        }
    }
    println!(
        "entity variable cases={} mismatches={}",
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
    assert_eq!(cases.len(), 756);
    for name in ["GET_VARP_OLD", "GET_VARPBIT_OLD"] {
        assert!(per_opcode[name].0 >= 16, "{name}");
    }
}
