#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

struct PhraseHost {
    defs: QuickChatDefs,
    outbox: Outbox,
}

impl VarHost for PhraseHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn quick_chat(&self) -> Result<&QuickChatDefs, HostError> {
        Ok(&self.defs)
    }

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

struct NoOutbox(QuickChatDefs);

impl VarHost for NoOutbox {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn quick_chat(&self) -> Result<&QuickChatDefs, HostError> {
        Ok(&self.0)
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

const OPERATIONS: &[Opcode] = &[
    Opcode::ACTIVECHATPHRASE_PREPARE,
    Opcode::ACTIVECHATPHRASE_SETDYNAMICINT,
    Opcode::ACTIVECHATPHRASE_SETDYNAMICOBJ,
    Opcode::ACTIVECHATPHRASE_SENDPUBLIC,
    Opcode::ACTIVECHATPHRASE_SENDCLAN,
    Opcode::ACTIVECHATPHRASE_SENDCLANCHANNEL_AFFINED,
    Opcode::ACTIVECHATPHRASE_SENDCLANCHANNEL_LISTENED,
    Opcode::ACTIVECHATPHRASE_SENDPRIVATE,
];

fn unhex_bytes(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
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

fn strings(value: &str) -> Vec<Option<String>> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(utf16).collect()
    }
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn defs() -> QuickChatDefs {
    let mut defs = None;
    for line in include_str!("fixtures/quick-chat.defs.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        match columns[0] {
            "counts" => {
                defs = Some(QuickChatDefs::new(
                    columns[1].parse().unwrap(),
                    columns[2].parse().unwrap(),
                ))
            }
            "def" => {
                let menu = columns[1] == "1";
                let file: i32 = columns[3].parse().unwrap();
                let bytes = unhex_bytes(columns[4]);
                let defs = defs.as_mut().unwrap();
                if columns[2] == "c" {
                    defs.insert_category(menu, file, &bytes).unwrap();
                } else {
                    defs.insert_message(menu, file, &bytes).unwrap();
                }
            }
            other => panic!("{other}"),
        }
    }
    defs.unwrap()
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

fn state(vm: &Vm) -> String {
    match vm.chat_phrase() {
        None => "~".to_string(),
        Some((id, message, values)) => {
            let values = match values {
                None => "~".to_string(),
                Some([]) => "-".to_string(),
                Some(values) => values
                    .iter()
                    .map(|value| value.to_string())
                    .collect::<Vec<_>>()
                    .join(","),
            };
            format!("{id}:{}:{values}", if message { "m" } else { "~" })
        }
    }
}

fn repository(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 2, 0), zero(), &[])).unwrap(),
    )])
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_chat_phrase_case_matches_the_actual_kotlin_dispatch_and_packet_bytes() {
    let mut host = PhraseHost {
        defs: defs(),
        outbox: Outbox::default(),
    };
    let mut vm = Vm::default();
    let mut expected_total = None;
    let mut cases = 0;
    let mut skipped = 0;
    let mut packets_checked = 0;
    let mut stacks_skipped = 0;
    let mut native: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for line in include_str!("fixtures/chat-phrase.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        if columns[0] == "Z" {
            expected_total = Some(columns[1].parse::<usize>().unwrap());
            continue;
        }
        assert_eq!(columns.len(), 10, "{line}");
        let name = columns[1];
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[2])
            .unwrap();
        let entry = native.entry(columns[2].to_string()).or_default();
        if columns[5] == "ok" {
            entry.0 += 1;
        } else {
            entry.1 += 1;
        }
        cases += 1;
        let texts = strings(columns[4]);
        let ints = integers(columns[3]);
        let mut code = Vec::new();
        let mut unusable = false;
        for value in &texts {
            match value {
                Some(value) if !value.contains('\0') && cp1252(value).is_some() => {
                    code.push(text(value))
                }
                _ => unusable = true,
            }
        }
        if unusable {
            skipped += 1;
            continue;
        }
        code.extend(ints.iter().copied().map(constant));
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        host.outbox = Outbox::default();
        let result = vm
            .run_with_host(1, &repository(&code), Arguments::default(), &mut host)
            .map(|_| ());
        let mut problems = Vec::new();
        let mut underflow = false;
        match (&result, columns[5]) {
            (Ok(()), "ok") => {}
            (Err(error), expected) if expected.starts_with("error:") => {
                underflow = matches!(error.kind, VmErrorKind::StackUnderflow(_));
                let expected = &expected["error:".len()..];
                let matches = if expected == "IllegalArgumentException" {
                    false
                } else {
                    class_matches(expected, &error.kind)
                };
                if error.instruction != pushed || !matches {
                    problems.push(format!("error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => problems.push(format!("{expected} vs {actual:?}")),
        }
        if underflow {
            stacks_skipped += 1;
        } else {
            let want_ints = integers(columns[6]);
            if vm.int_stack() != want_ints.as_slice() {
                problems.push(format!("ints {:?} vs {want_ints:?}", vm.int_stack()));
            }
            let want: Vec<Option<std::sync::Arc<str>>> = strings(columns[7])
                .into_iter()
                .map(|value| value.map(std::sync::Arc::from))
                .collect();
            if vm.string_stack() != want.as_slice() {
                problems.push(format!("strings {:?} vs {want:?}", vm.string_stack()));
            }
        }
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox
                .packets
                .iter()
                .map(hex)
                .collect::<Vec<_>>()
                .join("|")
        };
        if packets != columns[8] {
            problems.push(format!("packets {packets} vs {}", columns[8]));
        }
        if packets != "-" {
            packets_checked += 1;
        }
        let phrase = state(&vm);
        if phrase != columns[9] {
            problems.push(format!("state {phrase} vs {}", columns[9]));
        }
        if !problems.is_empty() {
            mismatches.push(format!("{name}: {}", problems.join("; ")));
        }
    }
    println!(
        "chat phrase cases={cases} skipped={skipped} packets={packets_checked} stacks_skipped={stacks_skipped} mismatches={}",
        mismatches.len()
    );
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(expected_total, Some(cases));
    assert_eq!(cases, 2557);
    assert!(packets_checked > 1000);
    let mut stats = BTreeMap::new();
    for line in include_str!("fixtures/chat-phrase.stats.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        stats.insert(
            columns[0].to_string(),
            (
                columns[1].parse::<usize>().unwrap(),
                columns[2].parse::<usize>().unwrap(),
            ),
        );
    }
    assert_eq!(native, stats);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn chat_phrase_operations_are_unsupported_without_a_host_or_outbox() {
    for opcode in OPERATIONS {
        let plain = BTreeMap::from([(1, script(&[int(*opcode, 0), ret()]))]);
        assert_eq!(
            Vm::default()
                .run(1, &plain, Arguments::default())
                .unwrap_err()
                .kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
    for opcode in [OPERATIONS[0]].iter().chain(&OPERATIONS[3..]) {
        let code = [constant(1), text("a"), int(*opcode, 0), ret()];
        let error =
            Vm::default().run_with_host(1, &repository(&code), Arguments::default(), &mut Bare);
        assert_eq!(
            error.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
    let mut host = NoOutbox(defs());
    for opcode in &OPERATIONS[3..] {
        let code = [text("a"), int(*opcode, 0), ret()];
        let error =
            Vm::default().run_with_host(1, &repository(&code), Arguments::default(), &mut host);
        assert_eq!(
            error.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}
