#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
    Opcode::QUEST_GETNAME,
    Opcode::QUEST_GETSORTNAME,
    Opcode::QUEST_TYPE,
    Opcode::QUEST_GETDIFFICULTY,
    Opcode::QUEST_GETMEMBERS,
    Opcode::QUEST_POINTS,
    Opcode::QUEST_QUESTREQ_COUNT,
    Opcode::QUEST_QUESTREQ,
    Opcode::QUEST_QUESTREQ_MET,
    Opcode::QUEST_POINTSREQ,
    Opcode::QUEST_POINTSREQ_MET,
    Opcode::QUEST_STATREQ_COUNT,
    Opcode::QUEST_STATREQ_STAT,
    Opcode::QUEST_STATREQ_LEVEL,
    Opcode::QUEST_STATREQ_MET,
    Opcode::QUEST_VARPREQ_COUNT,
    Opcode::QUEST_VARPREQ_DESC,
    Opcode::QUEST_VARPREQ_MET,
    Opcode::QUEST_VARBITREQ_COUNT,
    Opcode::QUEST_VARBITREQ_DESC,
    Opcode::QUEST_VARBITREQ_MET,
    Opcode::QUEST_ALLREQMET,
    Opcode::QUEST_STARTED,
    Opcode::QUEST_FINISHED,
    Opcode::QUEST_PARAM,
];

struct QuestHost {
    defs: QuestDefs,
    player: PlayerState,
    varps: BTreeMap<i32, i32>,
    varbits: BTreeMap<i32, Varbit>,
    params: BTreeMap<i32, Arc<ParamDefinition>>,
}

impl VarHost for QuestHost {
    fn varp(&self, id: i32) -> Result<i32, HostError> {
        if !(0..65536).contains(&id) {
            return Err(HostError::InvalidVarp(id));
        }
        Ok(self.varps.get(&id).copied().unwrap_or(0))
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Err(HostError::Unavailable)
    }

    fn varbit(&self, id: i32) -> Result<VarbitLookup, HostError> {
        self.varbits
            .get(&id)
            .map(|varbit| VarbitLookup::Defined(*varbit))
            .ok_or(HostError::InvalidVarbit(id))
    }

    fn player_state(&self) -> Result<&PlayerState, HostError> {
        Ok(&self.player)
    }

    fn quest_defs(&self) -> Result<&QuestDefs, HostError> {
        Ok(&self.defs)
    }

    fn param_definition(&self, id: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        self.params
            .get(&id)
            .cloned()
            .map(Some)
            .ok_or(HostError::State(format!("param {id}")))
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

fn unhex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
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

fn profile(columns: &[&str]) -> QuestHost {
    let files: Vec<Option<Vec<u8>>> = columns[2]
        .split(',')
        .map(|file| Some(unhex(&file[1..])))
        .collect();
    let varbits = columns[3]
        .split(';')
        .map(|entry| {
            let parts: Vec<i32> = entry.split(':').map(|n| n.parse().unwrap()).collect();
            (
                parts[0],
                Varbit {
                    varp: parts[1] as u16,
                    start: parts[2] as u8,
                    end: parts[3] as u8,
                },
            )
        })
        .collect();
    let params = columns[4]
        .split(';')
        .map(|entry| {
            let parts: Vec<&str> = entry.split(':').collect();
            let is_string = parts[1] == "1";
            (
                parts[0].parse().unwrap(),
                Arc::new(ParamDefinition {
                    is_string,
                    default_int: parts[2].parse().unwrap(),
                    type_name: Arc::from(utf16(parts[3]).unwrap().as_str()),
                }),
            )
        })
        .collect();
    QuestHost {
        defs: QuestDefs::from_files(&files).unwrap(),
        player: PlayerState::default(),
        varps: BTreeMap::new(),
        varbits,
        params,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_quest_case_matches_the_actual_kotlin_dispatch() {
    let mut host: Option<QuestHost> = None;
    let mut total = 0;
    let mut failing = 0;
    let mut declared = 0;
    for line in include_str!("fixtures/quest.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        match columns[0] {
            "P" => host = Some(profile(&columns)),
            "S" => {
                let host = host.as_mut().unwrap();
                host.varps = columns[1]
                    .split(',')
                    .map(|entry| {
                        let (id, value) = entry.split_once('=').unwrap();
                        (id.parse().unwrap(), value.parse().unwrap())
                    })
                    .collect();
                let skills = integers(columns[2]);
                host.player.skill_level_actual.copy_from_slice(&skills);
            }
            "C" => {
                let host = host.as_mut().unwrap();
                let opcode = OPERATIONS
                    .iter()
                    .copied()
                    .find(|opcode| opcode.metadata().name == columns[1])
                    .unwrap();
                let name = format!("{} {}", columns[1], columns[2]);
                let mut code: Vec<_> = integers(columns[2]).into_iter().map(constant).collect();
                let pushed = code.len();
                code.extend([int(opcode, 0), ret()]);
                let (vm, result) = run(host, &code);
                match columns[3] {
                    "ok" => {
                        result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
                        assert_eq!(vm.int_stack(), integers(columns[4]).as_slice(), "{name}");
                        let expected: Vec<Option<Arc<str>>> = if columns[5] == "-" {
                            Vec::new()
                        } else {
                            columns[5]
                                .split(',')
                                .map(|value| utf16(value).map(Arc::from))
                                .collect()
                        };
                        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
                    }
                    error if error.starts_with("error:") => {
                        let error = result.err().unwrap_or_else(|| {
                            panic!("{name}: kotlin {} but rust ok {:?}", columns[3], vm.int_stack())
                        });
                        assert_eq!(error.instruction, pushed, "{name}");
                        assert_ne!(error.kind, VmErrorKind::UnsupportedOpcode(opcode), "{name}");
                        failing += 1;
                    }
                    other => panic!("{name}: unexpected {other}"),
                }
                total += 1;
            }
            "Z" => declared = columns[1].parse::<usize>().unwrap(),
            other => panic!("unexpected row {other}"),
        }
    }
    assert_eq!(total, declared);
    assert_eq!(total, 7503);
    assert!(failing > 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn quest_operations_without_quest_definitions_stay_unsupported() {
    for opcode in OPERATIONS {
        let (_, result) = run(&mut Bare, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}
