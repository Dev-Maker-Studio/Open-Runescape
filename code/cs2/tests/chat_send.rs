#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct SendHost {
    player: PlayerState,
    client: ClientState,
    language: Option<i32>,
    outbox: Outbox,
}

impl VarHost for SendHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn player_state(&self) -> Result<&PlayerState, HostError> {
        Ok(&self.player)
    }

    fn client(&mut self) -> Result<&mut ClientState, HostError> {
        Ok(&mut self.client)
    }

    fn language(&self) -> Result<Option<i32>, HostError> {
        Ok(self.language)
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

fn language_id(name: &str) -> Option<i32> {
    ["EN", "DE", "FR", "PT", "NL", "ES"]
        .iter()
        .position(|candidate| *candidate == name)
        .map(|index| index as i32)
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

fn hex_bytes(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn build_host(fields: &[&str]) -> SendHost {
    let player = if fields[7] == "P" {
        Some(MyPlayer {
            username: nullable(fields[8]),
            display_name: nullable(fields[9]),
            prefix_title: nullable(fields[10]),
            postfix_title: nullable(fields[11]),
            ..MyPlayer::default()
        })
    } else {
        None
    };
    SendHost {
        player: PlayerState {
            player_rights: number(fields[2]),
            my_player: player,
            ..PlayerState::default()
        },
        client: ClientState {
            userdetail_quickchat: number(fields[3]) != 0,
            verified_email_address: number(fields[4]) != 0,
            quickchat_only: number(fields[5]) != 0,
            ..ClientState::default()
        },
        language: language_id(fields[6]),
        outbox: Outbox::default(),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn chat_send_and_name_ops_match_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/chat-send.native.tsv");
    let mut states: BTreeMap<i32, Vec<&str>> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "H" => {
                states.insert(number(c[1]), c);
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut skipped = 0;
    let mut sends_checked = 0;
    let mut deferred_oversize = 0;
    for c in &cases {
        let name = c[1];
        let opcode = opcode_named(name);
        let mut instructions = Vec::new();
        let arguments: Vec<&str> = if c[3] == "-" {
            Vec::new()
        } else {
            c[3].split('|').collect()
        };
        if arguments
            .iter()
            .any(|argument| *argument == "~" || cp1252(argument).is_none())
        {
            skipped += 1;
            continue;
        }
        for argument in &arguments {
            instructions.push(text(argument));
        }
        let initial = instructions.len();
        instructions.push(int(opcode, 0));
        instructions.push(ret());
        let mut host = build_host(&states[&number(c[2])]);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} state {} {}", c[2], c[3]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let sends = std::mem::take(&mut host.outbox.chat_sends);
        let packet = if c[6] == "-" { None } else { Some(hex_bytes(c[6])) };
        let mut problems = Vec::new();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let texts: Vec<String> = vm
                    .string_stack()
                    .iter()
                    .map(|item| match item {
                        None => "~".to_string(),
                        Some(item) => format!("\"{item}\""),
                    })
                    .collect();
                let texts = if texts.is_empty() { "-".to_string() } else { texts.join("|") };
                if texts != c[5] {
                    problems.push(format!("stack {texts} vs {}", c[5]));
                }
                check_send(&sends, packet.as_deref(), &mut problems);
                if packet.is_some() {
                    sends_checked += 1;
                }
            }
            (Ok(_), "error:ArrayIndexOutOfBoundsException")
                if matches!(name, "CHAT_SENDPUBLIC" | "CHAT_SENDPRIVATE") =>
            {
                entry.1 += 1;
                deferred_oversize += 1;
                if sends.len() != 1 || packet.is_some() {
                    problems.push(format!("oversize send {sends:?}"));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                let class = &expected["error:".len()..];
                let matches = error.instruction == initial
                    && match &error.kind {
                        VmErrorKind::Host(HostError::NoEntity) => class == "NullPointerException",
                        VmErrorKind::Host(HostError::State(message)) => {
                            class == "IllegalStateException" && message.starts_with("Unknown language")
                        }
                        _ => false,
                    };
                if !matches || !sends.is_empty() || packet.is_some() {
                    problems.push(format!("error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => problems.push(format!("{expected} vs {actual:?}")),
        }
        if !problems.is_empty() {
            mismatches.push(format!("{label}: {}", problems.join("; ")));
        }
    }
    println!(
        "chat send cases={} skipped={skipped} sends={sends_checked} oversize_deferred={deferred_oversize} mismatches={}",
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
    assert_eq!(per_opcode.len(), 4);
    assert!(sends_checked > 500);
    assert!(deferred_oversize > 50);
}

fn check_send(sends: &[ChatSend], packet: Option<&[u8]>, problems: &mut Vec<String>) {
    match (sends, packet) {
        ([], None) => {}
        ([ChatSend::Public { color, effect, .. }], Some(bytes)) => {
            if bytes[2] != *color || bytes[3] != *effect {
                problems.push(format!("public {color},{effect} vs {:02x}{:02x}", bytes[2], bytes[3]));
            }
        }
        ([ChatSend::Private { recipient, .. }], Some(bytes)) => {
            let mut expected = cp1252(recipient).unwrap();
            expected.push(0);
            if bytes[3..3 + expected.len()] != expected[..] {
                problems.push(format!("private recipient {recipient}"));
            }
        }
        (sends, packet) => problems.push(format!("sends {sends:?} vs {packet:?}")),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn the_openrs_show_command_sends_nothing() {
    for message in ["/openrs-show", "  /openrs-show\t", "\u{a0}/openrs-show"] {
        let instructions = [text(message), int(Opcode::CHAT_SENDPUBLIC, 0), ret()];
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut host = build_host(&[
            "H", "0", "0", "0", "0", "0", "EN", "-", "", "", "", "",
        ]);
        let mut vm = Vm::default();
        vm.run_with_host(1, &repository, Arguments::default(), &mut host)
            .unwrap();
        assert!(host.outbox.chat_sends.is_empty());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn chat_send_ops_are_unsupported_without_a_host() {
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
    for (opcode, arguments) in [
        (Opcode::CHAT_PLAYERNAME, 0),
        (Opcode::CHAT_PLAYERNAME_UNFILTERED, 0),
        (Opcode::CHAT_SENDPUBLIC, 1),
        (Opcode::CHAT_SENDPRIVATE, 2),
    ] {
        let mut instructions: Vec<_> = (0..arguments).map(|_| text("a")).collect();
        instructions.extend([int(opcode, 0), ret()]);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
