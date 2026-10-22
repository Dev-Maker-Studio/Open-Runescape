#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

struct ClanHost {
    clan: ClanState,
    outbox: Outbox,
}

impl VarHost for ClanHost {
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

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn rank(value: &str) -> i8 {
    value.parse().unwrap()
}

fn nullable(value: &str) -> Option<String> {
    (value != "~").then(|| value.to_string())
}

fn length(value: &str) -> Option<usize> {
    (value != "-1").then(|| value.parse().unwrap())
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

fn find_opcode(code: &str) -> Opcode {
    let name = match code {
        "SA" => "ACTIVECLANSETTINGS_FIND_AFFINED",
        "SL" => "ACTIVECLANSETTINGS_FIND_LISTENED",
        "CA" => "ACTIVECLANCHANNEL_FIND_AFFINED",
        _ => "ACTIVECLANCHANNEL_FIND_LISTENED",
    };
    opcode_named(name)
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

#[derive(Default)]
struct State {
    vars: bool,
    channels: BTreeMap<i32, ClanChannel>,
    settings: BTreeMap<i32, ClanSettings>,
    stock: Vec<Option<StockSlot>>,
    main_channel: i32,
    listened_channel: i32,
    main_settings: i32,
    guest_settings: i32,
}

impl State {
    fn host(&self) -> ClanHost {
        let pick = |map: &BTreeMap<i32, ClanChannel>, id: i32| map.get(&id).cloned();
        ClanHost {
            clan: ClanState {
                channel: pick(&self.channels, self.main_channel),
                listened_channel: pick(&self.channels, self.listened_channel),
                listened_is_channel: self.main_channel >= 0
                    && self.main_channel == self.listened_channel,
                settings: self.settings.get(&self.main_settings).cloned(),
                guest_settings: self.settings.get(&self.guest_settings).cloned(),
                clan_vars: self.vars.then(Vec::new),
                stock_slots: self.stock.clone(),
                ..ClanState::default()
            },
            outbox: Outbox::default(),
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_clan_and_stock_opcode_matches_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/clan-stock.native.tsv");
    let mut states: BTreeMap<i32, State> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "K" => {
                let state = states.entry(number(c[1])).or_default();
                let id = number(c[2]);
                state.channels.insert(
                    id,
                    ClanChannel {
                        num_players: number(c[6]),
                        clan_name: nullable(c[3]),
                        players: (c[4] != "1").then(|| vec![None; number(c[5]) as usize]),
                        min_rank_to_kick: rank(c[7]),
                        guests_talk: rank(c[8]),
                    },
                );
            }
            "n" => {}
            "k" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                let channel = state.channels.get_mut(&number(c[2])).unwrap();
                channel.players.as_mut().unwrap()[number(c[3]) as usize] =
                    Some(ClanChannelPlayer {
                        name: nullable(c[4]),
                        rank: rank(c[5]),
                        world: number(c[6]),
                    });
            }
            "L" => {
                let state = states.entry(number(c[1])).or_default();
                let id = number(c[2]);
                let names = length(c[13]);
                let ranks = length(c[14]);
                let days = length(c[15]);
                let flags = length(c[16]);
                let bans = length(c[17]);
                state.settings.insert(
                    id,
                    ClanSettings {
                        clan_name: nullable(c[3]),
                        allow_guests: c[4] == "1",
                        talk_rank: rank(c[5]),
                        kick_rank: rank(c[6]),
                        lootshare_rank: rank(c[7]),
                        coin_share: rank(c[8]),
                        member_count: number(c[9]),
                        member_names: names.map(|n| vec![String::new(); n]),
                        member_ranks: ranks.map(|n| vec![0; n]),
                        member_join_days: days.map(|n| vec![0; n]),
                        ban_count: number(c[10]),
                        banned_user_names: bans.map(|n| vec![String::new(); n]),
                        current_owner: number(c[11]),
                        replacement_owner: number(c[12]),
                        clan_flags: flags.map(|n| vec![0; n]),
                        variables: None,
                    },
                );
            }
            "m" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                let settings = state.settings.get_mut(&number(c[2])).unwrap();
                settings.member_names.as_mut().unwrap()[number(c[3]) as usize] = c[4].to_string();
            }
            "r" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                let settings = state.settings.get_mut(&number(c[2])).unwrap();
                let index = number(c[3]) as usize;
                if let Some(ranks) = settings.member_ranks.as_mut() {
                    ranks[index] = rank(c[4]);
                }
                if let Some(days) = settings.member_join_days.as_mut() {
                    days[index] = number(c[5]);
                }
                if let Some(flags) = settings.clan_flags.as_mut() {
                    flags[index] = number(c[6]);
                }
            }
            "b" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                let settings = state.settings.get_mut(&number(c[2])).unwrap();
                settings.banned_user_names.as_mut().unwrap()[number(c[3]) as usize] =
                    c[4].to_string();
            }
            "H" => {
                let state = states.entry(number(c[1])).or_default();
                state.vars = c[2] == "1";
                state.main_channel = number(c[3]);
                state.listened_channel = number(c[4]);
                state.main_settings = number(c[5]);
                state.guest_settings = number(c[6]);
                state.stock = vec![None; STOCK_SLOT_COUNT];
            }
            "g" => {
                let state = states.get_mut(&number(c[1])).unwrap();
                if c[3] != "~" {
                    state.stock[number(c[2]) as usize] = Some(StockSlot {
                        progress: rank(c[3]),
                        item_id: number(c[4]),
                        price: number(c[5]),
                        amount: number(c[6]),
                        current_amount: number(c[7]),
                        total_price: number(c[8]),
                    });
                }
            }
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
        let mut code = Vec::new();
        if c[3] != "-" {
            for item in c[3].split(',') {
                code.push(find_opcode(item));
            }
        }
        let mut instructions: Vec<_> = code.iter().map(|&op| int(op, 0)).collect();
        if c[4] != "-" {
            for value in c[4].split(',') {
                instructions.push(constant(number(value)));
            }
        }
        if let Some(argument) = c[5].strip_prefix('s') {
            if argument == "~" || cp1252(argument).is_none() {
                skipped += 1;
                continue;
            }
            instructions.push(text(argument));
        }
        let initial = instructions.len();
        instructions.push(int(opcode, 0));
        instructions.push(ret());
        let mut host = states[&number(c[2])].host();
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} state {} {} {} {}", c[2], c[3], c[4], c[5]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox.packets.iter().map(hex).collect::<Vec<_>>().join("|")
        };
        let mut problems = Vec::new();
        match (&result, c[6]) {
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
                if ints != c[7] || texts != c[8] {
                    problems.push(format!("stacks {ints} / {texts} vs {} / {}", c[7], c[8]));
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
        if packets != c[9] {
            problems.push(format!("packets {packets} vs {}", c[9]));
        }
        if packets != "-" {
            packets_checked += 1;
        }
        if !problems.is_empty() {
            mismatches.push(format!("{label}: {}", problems.join("; ")));
        }
    }
    println!(
        "clan stock cases={} skipped={skipped} packets={packets_checked} mismatches={}",
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
    assert_eq!(per_opcode.len(), 43);
    assert!(packets_checked > 50);
}

#[test]
fn clan_and_stock_opcodes_are_unsupported_without_a_host_clan_state() {
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
        Opcode::ACTIVECLANSETTINGS_FIND_AFFINED,
        Opcode::ACTIVECLANCHANNEL_FIND_LISTENED,
        Opcode::CLANPROFILE_FIND,
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
