#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
    Opcode::USERDETAIL_LOBBY_MEMBERSHIP,
    Opcode::USERDETAIL_LOBBY_RECOVERYDAY,
    Opcode::USERDETAIL_LOBBY_UNREADMESSAGES,
    Opcode::USERDETAIL_LOBBY_LASTLOGINDAY,
    Opcode::USERDETAIL_LOBBY_LASTLOGINADDRESS,
    Opcode::USERDETAIL_LOBBY_EMAILSTATUS,
    Opcode::USERDETAIL_LOBBY_CCEXPIRY,
    Opcode::USERDETAIL_LOBBY_GRACEEXPIRY,
    Opcode::USERDETAIL_LOBBY_DOBREQUESTED,
    Opcode::USERDETAIL_DOB,
    Opcode::USERDETAIL_LOBBY_MEMBERSSTATS,
    Opcode::USERDETAIL_LOBBY_PLAYAGE,
    Opcode::USERDETAIL_LOBBY_JCOINS_BALANCE,
    Opcode::USERDETAIL_LOBBY_LOYALTY_ENABLED,
    Opcode::USERDETAIL_LOBBY_LOYALTY_BALANCE,
    Opcode::BOOST_ADVERT_EXISTS,
    Opcode::BOOST_ADVERT_AVAILABLE,
    Opcode::BOOST_ADVERT_LAUNCH,
];

#[derive(Default)]
struct LobbyHost {
    lobby: LobbyState,
    now: i64,
}

impl VarHost for LobbyHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn lobby_state(&self) -> Result<&LobbyState, HostError> {
        Ok(&self.lobby)
    }

    fn clock_millis(&self) -> Result<i64, HostError> {
        Ok(self.now)
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

fn lobby(columns: &[&str]) -> (i64, LobbyState) {
    let flag = |index: usize| columns[index] == "1";
    let number = |index: usize| columns[index].parse::<i32>().unwrap();
    (
        columns[1].parse().unwrap(),
        LobbyState {
            membership_end: columns[2].parse().unwrap(),
            membership_time_correction: columns[3].parse().unwrap(),
            is_member_subscription: flag(4),
            recovery_day: number(5),
            messages: number(6),
            last_login_day: number(7),
            last_login_host: utf16(columns[8]).map(|host| Arc::from(host.as_str())),
            email_status: number(9),
            credit_card_expiry: number(10),
            grace_expiry: number(11),
            dob_requested: flag(12),
            dob: number(13),
            members_stats: number(14),
            play_age: number(15),
            jcoins: number(16),
            loyalty_enabled: flag(17),
            loyalty_balance: number(18),
        },
    )
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_userdetail_case_matches_the_actual_kotlin_dispatch() {
    let mut host = LobbyHost::default();
    let mut total = 0;
    let mut declared = 0;
    for line in include_str!("fixtures/userdetail.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        match columns[0] {
            "S" => (host.now, host.lobby) = lobby(&columns),
            "C" => {
                let opcode = OPERATIONS
                    .iter()
                    .copied()
                    .find(|opcode| opcode.metadata().name == columns[1])
                    .unwrap();
                let name = format!("{} {} {:?}", columns[1], columns[2], host.lobby);
                let mut code = Vec::new();
                if columns[2] != "-" {
                    let argument = utf16(columns[2]).filter(|value| value.chars().all(|c| c.is_ascii() && c != '\0'));
                    code.push(text(argument.as_deref().unwrap_or("null")));
                }
                code.extend([int(opcode, 0), ret()]);
                let (vm, result) = run(&mut host, &code);
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
                assert_eq!(vm.int_stack(), integers(columns[3]).as_slice(), "{name}");
                let expected: Vec<Option<Arc<str>>> = if columns[4] == "-" {
                    Vec::new()
                } else {
                    columns[4]
                        .split(',')
                        .map(|value| utf16(value).map(|value| Arc::from(value.as_str())))
                        .collect()
                };
                assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
                total += 1;
            }
            "Z" => declared = columns[1].parse::<usize>().unwrap(),
            other => panic!("unexpected row {other}"),
        }
    }
    assert_eq!(total, declared);
    assert_eq!(total, 7218);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn userdetail_operations_without_lobby_state_stay_unsupported() {
    for opcode in OPERATIONS {
        if matches!(
            opcode,
            Opcode::BOOST_ADVERT_EXISTS | Opcode::BOOST_ADVERT_AVAILABLE | Opcode::BOOST_ADVERT_LAUNCH
        ) {
            continue;
        }
        let (_, result) = run(&mut Bare, &[int(*opcode, 0), ret()]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}
