#[allow(dead_code)]
#[path = "../../cs2/tests/common/mod.rs"]
mod common;

use common::*;
use openrs_client_runtime::chat_send::to_body;
use openrs_client_runtime::environment::Environment;
use openrs_client_runtime::vars::{WorldVarHost, WorldVariables};
use openrs_cs2::*;
use openrs_protocol::client_packets::Huffman;
use openrs_protocol::isaac::Isaac;
use std::collections::BTreeMap;

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

fn table_a() -> Huffman {
    let lengths: Vec<i8> = (0..256)
        .map(|i| {
            if i < 64 {
                7
            } else if i < 128 {
                8
            } else {
                9
            }
        })
        .collect();
    Huffman::new(&lengths)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_var_host_and_the_protocol_framer_replay_every_native_chat_send() {
    let fixture = include_str!("../../cs2/tests/fixtures/chat-send.native.tsv");
    let mut states: BTreeMap<i32, Vec<&str>> = BTreeMap::new();
    let mut cases = Vec::new();
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "H" => {
                states.insert(number(c[1]), c);
            }
            "T" => cases.push(c),
            _ => {}
        }
    }
    let huffman = table_a();
    let mut mismatches = Vec::new();
    let (mut framed, mut oversize, mut skipped) = (0, 0, 0);
    let mut max_framed = 0;
    let mut max_private = 0;
    for c in &cases {
        if !matches!(c[1], "CHAT_SENDPUBLIC" | "CHAT_SENDPRIVATE")
            || c[4] != "ok" && c[4] != "error:ArrayIndexOutOfBoundsException"
        {
            continue;
        }
        let arguments: Vec<&str> = c[3].split('|').collect();
        if arguments
            .iter()
            .any(|argument| *argument == "~" || cp1252(argument).is_none())
        {
            skipped += 1;
            continue;
        }
        let state = &states[&number(c[2])];
        let mut player = PlayerState {
            player_rights: number(state[2]),
            my_player: (state[7] == "P").then(|| MyPlayer {
                username: nullable(state[8]),
                display_name: nullable(state[9]),
                prefix_title: nullable(state[10]),
                postfix_title: nullable(state[11]),
                ..MyPlayer::default()
            }),
            ..PlayerState::default()
        };
        player.player_rights = number(state[2]);
        let mut client = ClientState {
            userdetail_quickchat: number(state[3]) != 0,
            verified_email_address: number(state[4]) != 0,
            quickchat_only: number(state[5]) != 0,
            ..ClientState::default()
        };
        let mut environment = Environment::new(Some(0));
        environment.set_language(language_id(state[6]));
        let mut variables = WorldVariables::new(8).unwrap();
        let mut outbox = Outbox::default();
        let mut instructions: Vec<_> = arguments.iter().map(|argument| text(argument)).collect();
        instructions.push(int(opcode_named(c[1]), 0));
        instructions.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let result = {
            let mut host = WorldVarHost::new(&mut variables, 0)
                .with_environment(&mut environment, 0)
                .with_player_state(&player)
                .with_client_state(&mut client)
                .with_outbox(&mut outbox);
            Vm::default().run_with_host(1, &repository, Arguments::default(), &mut host)
        };
        let sends = outbox.drain_chat_sends();
        let expected = if c[6] == "-" {
            None
        } else {
            Some(hex_bytes(c[6]))
        };
        let label = format!("{} state {} {}", c[1], c[2], c[3]);
        if result.is_err() {
            mismatches.push(format!("{label}: {result:?}"));
            continue;
        }
        if sends.is_empty() {
            if expected.is_some() {
                mismatches.push(format!("{label}: no send but packet expected"));
            }
            continue;
        }
        let body = to_body(&huffman, &sends[0]);
        match (body, &expected) {
            (Ok(body), Some(bytes)) => {
                let framed_bytes = openrs_protocol::client_packets::frame::frame_body(
                    &mut Isaac::new(&[1, 2, 3, 4]).unwrap(),
                    &body,
                );
                let Ok(framed_bytes) = framed_bytes else {
                    mismatches.push(format!("{label}: unframeable {framed_bytes:?}"));
                    continue;
                };
                if framed_bytes[1..] != bytes[1..] {
                    mismatches.push(format!("{label}: body differs"));
                } else {
                    framed += 1;
                    max_framed = max_framed.max(if c[1] == "CHAT_SENDPUBLIC" {
                        body.body.len()
                    } else {
                        0
                    });
                    max_private = max_private.max(if c[1] == "CHAT_SENDPUBLIC" {
                        0
                    } else {
                        body.body.len()
                    });
                }
            }
            (Err(_), None) => oversize += 1,
            (body, expected) => mismatches.push(format!(
                "{}: {} body bytes vs {} expected",
                &label[..label.len().min(40)],
                body.map(|body| body.body.len()).unwrap_or(0),
                expected.as_ref().map(Vec::len).unwrap_or(0)
            )),
        }
    }
    println!("largest public body {max_framed} largest private body {max_private}");
    println!(
        "chat replay framed={framed} oversize={oversize} skipped={skipped} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty());
    assert!(framed > 500);
    assert!(oversize > 50);
}
