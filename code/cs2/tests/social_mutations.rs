#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

struct MutationHost {
    social: SocialState,
    outbox: Outbox,
}

impl VarHost for MutationHost {
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

    fn social_state_mut(&mut self) -> Result<&mut SocialState, HostError> {
        Ok(&mut self.social)
    }

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn nullable(value: &str) -> Option<String> {
    (value != "~").then(|| value.to_string())
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

fn friends_snapshot(social: &SocialState) -> String {
    let names: Vec<&str> = social.friends.iter().map(|f| f.display_name.as_str()).collect();
    if names.len() > 20 {
        return format!("#{}:{},{}", names.len(), names[0], names[names.len() - 1]);
    }
    let joined = names.join(",");
    if joined.is_empty() {
        "-".to_string()
    } else {
        joined
    }
}

fn ignores_snapshot(social: &SocialState) -> String {
    let names: Vec<&str> = social
        .ignores
        .iter()
        .map(|i| i.unfiltered_username.as_deref().unwrap_or("~"))
        .collect();
    if names.len() > 20 {
        return format!("#{}:{},{}", names.len(), names[0], names[names.len() - 1]);
    }
    let joined = names.join(",");
    if joined.is_empty() {
        "-".to_string()
    } else {
        joined
    }
}

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value
            .split('|')
            .map(|item| {
                (item != "~").then(|| {
                    item.strip_prefix('"')
                        .and_then(|item| item.strip_suffix('"'))
                        .unwrap_or(item)
                        .to_string()
                })
            })
            .collect()
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_social_mutation_matches_the_actual_kotlin_dispatch_and_packet_bytes() {
    let fixture = include_str!("fixtures/social-mutations.native.tsv");
    let mut states: BTreeMap<i32, SocialState> = BTreeMap::new();
    let mut players_null: BTreeMap<i32, bool> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "F" => {
                states.entry(number(c[1])).or_default().friends_loaded = 2;
            }
            "f" => states.entry(number(c[1])).or_default().friends.push(Friend {
                display_name: c[2].to_string(),
                referrer: false,
                world_id: 0,
                world_name: "w".to_string(),
                friends_chat_rank: 0,
                platform: 0,
                previous_display_name: c[3].to_string(),
                world_flags: 0,
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
                players_null.insert(id, c[3] == "1");
                state.my_display_name = (c[4] == "1").then(|| nullable(c[5]));
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    for (id, state) in states.iter_mut() {
        if !players_null[id] {
            state.fc_players = Some(vec![None; 2]);
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut skipped = 0;
    let mut packets_checked = 0;
    let mut messages_checked = 0;
    for c in &cases {
        let name = c[1];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let argument = c[3];
        let mut code = Vec::new();
        let initial;
        if argument == "-" || argument == "u" {
            initial = 0;
        } else if argument == "n" || argument.ends_with(":~") || argument.contains("\\0") {
            skipped += 1;
            continue;
        } else if let Some(rest) = argument.strip_prefix('r') {
            let (rank, body) = rest.split_once(':').unwrap();
            if cp1252(body).is_none() {
                skipped += 1;
                continue;
            }
            code.push(constant(number(rank)));
            code.push(text(body));
            initial = 2;
        } else {
            let body = &argument[1..];
            if cp1252(body).is_none() {
                skipped += 1;
                continue;
            }
            code.push(text(body));
            initial = 1;
        }
        code.push(int(opcode, 0));
        code.push(ret());
        let mut host = MutationHost {
            social: states[&number(c[2])].clone(),
            outbox: Outbox::default(),
        };
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} state {} {argument}", c[2]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox.packets.iter().map(hex).collect::<Vec<_>>().join("|")
        };
        let message = match host.outbox.game_messages.last() {
            None => "-".to_string(),
            Some(last) => format!("{}:{last}", host.outbox.game_messages.len()),
        };
        let tick = if host.outbox.friend_ignore_updates > 0 { "1" } else { "0" };
        let mut problems = Vec::new();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let texts = strings(c[6]);
                let actual: Vec<Option<String>> = vm
                    .string_stack()
                    .iter()
                    .map(|item| item.as_ref().map(|item| item.to_string()))
                    .collect();
                if !vm.int_stack().is_empty() || actual != texts || c[5] != "-" {
                    problems.push(format!("stacks {actual:?} vs {texts:?}"));
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
        if message != c[8] {
            problems.push(format!("message {message} vs {}", c[8]));
        }
        if tick != c[9] {
            problems.push(format!("tick {tick} vs {}", c[9]));
        }
        let friends = friends_snapshot(&host.social);
        let ignores = ignores_snapshot(&host.social);
        if friends != c[10] || ignores != c[11] {
            problems.push(format!(
                "lists {friends} / {ignores} vs {} / {}",
                c[10], c[11]
            ));
        }
        if packets != "-" {
            packets_checked += 1;
        }
        if message != "-" {
            messages_checked += 1;
        }
        if !problems.is_empty() {
            mismatches.push(format!("{label}: {}", problems.join("; ")));
        }
    }
    println!(
        "social mutations cases={} skipped={skipped} packets={packets_checked} messages={messages_checked} mismatches={}",
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
    assert!(packets_checked > 5000 && messages_checked > 1000);
}

#[test]
fn outbound_social_opcodes_are_unsupported_without_a_host_outbox() {
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
    for opcode in [Opcode::CLAN_LEAVECHAT, Opcode::CLAN_GETCHATDISPLAYNAME] {
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
