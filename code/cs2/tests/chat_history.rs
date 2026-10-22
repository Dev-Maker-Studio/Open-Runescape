#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

struct ChatHost {
    chat: ChatState,
    outbox: Outbox,
}

impl VarHost for ChatHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn chat_state(&self) -> Result<&ChatState, HostError> {
        Ok(&self.chat)
    }

    fn chat_state_mut(&mut self) -> Result<&mut ChatState, HostError> {
        Ok(&mut self.chat)
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

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_chat_opcode_matches_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/chat-history.native.tsv");
    let mut states: BTreeMap<i32, ChatState> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "L" => {
                let state = states.entry(number(c[1])).or_default();
                state.lines[number(c[2]) as usize] = Some(ChatLine {
                    kind: (c[3] != "~").then(|| number(c[3])),
                    identifier: number(c[4]),
                    time: number(c[5]),
                    effect_flags: number(c[6]),
                    crowned_name: nullable(c[7]),
                    name: nullable(c[8]),
                    simple_name: nullable(c[9]),
                    clan: nullable(c[10]),
                    quickchat_message_id: number(c[11]),
                    message: nullable(c[12]),
                });
            }
            "H" => {
                let state = states.entry(number(c[1])).or_default();
                state.line_count = number(c[2]);
                state.public_filter = number(c[3]);
                state.private_filter = (c[4] != "-1").then(|| number(c[4]));
                state.trade_filter = number(c[5]);
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
        let mut instructions = Vec::new();
        if c[3] != "-" {
            for value in c[3].split(',') {
                instructions.push(constant(number(value)));
            }
        }
        if c[4] != "-" {
            let mut unsupported = false;
            for argument in c[4].split('|') {
                if argument == "~" || cp1252(argument).is_none() {
                    unsupported = true;
                    break;
                }
            }
            if unsupported {
                skipped += 1;
                continue;
            }
            for argument in c[4].split('|') {
                instructions.push(text(argument));
            }
        }
        let initial = instructions.len();
        instructions.push(int(opcode, 0));
        instructions.push(ret());
        let mut host = ChatHost {
            chat: states[&number(c[2])].clone(),
            outbox: Outbox::default(),
        };
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} state {} {} {}", c[2], c[3], c[4]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox.packets.iter().map(hex).collect::<Vec<_>>().join("|")
        };
        let mut problems = Vec::new();
        match (&result, c[5]) {
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
                if ints != c[6] || texts != c[7] {
                    problems.push(format!("stacks {ints} / {texts} vs {} / {}", c[6], c[7]));
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
        if packets != c[8] {
            problems.push(format!("packets {packets} vs {}", c[8]));
        }
        if packets != "-" {
            packets_checked += 1;
        }
        let post = format!(
            "{},{},{}",
            host.chat.public_filter,
            host.chat.private_filter.unwrap_or(-1),
            host.chat.trade_filter
        );
        if post != c[9] {
            problems.push(format!("filters {post} vs {}", c[9]));
        }
        if !problems.is_empty() {
            mismatches.push(format!("{label}: {}", problems.join("; ")));
        }
    }
    println!(
        "chat history cases={} skipped={skipped} packets={packets_checked} mismatches={}",
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
    assert_eq!(per_opcode.len(), 17);
    assert!(packets_checked > 300);
}

#[test]
fn chat_opcodes_are_unsupported_without_a_host_chat_state() {
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
        Opcode::CHAT_GETFILTER_PUBLIC,
        Opcode::CHAT_GETHISTORYLENGTH,
        Opcode::CHAT_SETFILTER,
    ] {
        let mut instructions = Vec::new();
        if opcode == Opcode::CHAT_SETFILTER {
            instructions.extend([constant(0), constant(1), constant(2)]);
        }
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
