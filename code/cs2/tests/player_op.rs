#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::fmt::Write;

#[derive(Clone)]
struct OpHost {
    entities: EntityState,
    language: Option<i32>,
    outbox: Outbox,
    model: ComponentModel,
}

impl VarHost for OpHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn entity_state(&self) -> Result<&EntityState, HostError> {
        Ok(&self.entities)
    }

    fn language(&self) -> Result<Option<i32>, HostError> {
        Ok(self.language)
    }

    fn components(&mut self) -> Result<&mut ComponentModel, HostError> {
        Ok(&mut self.model)
    }

    fn outbox(&mut self) -> Result<&mut Outbox, HostError> {
        Ok(&mut self.outbox)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn language_id(name: &str) -> Option<i32> {
    ["EN", "DE", "FR", "PT", "NL", "ES"]
        .iter()
        .position(|candidate| *candidate == name)
        .map(|index| index as i32)
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

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(message)) => {
            expected == "IllegalStateException" && message.starts_with("Unknown language")
        }
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn player_op_and_interface_close_match_the_actual_kotlin_dispatch() {
    let fixture = include_str!("fixtures/player-op.native.tsv");
    let mut states: BTreeMap<i32, OpHost> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "H" => {
                let mut entities = EntityState {
                    has_my_player: c[2] == "1",
                    ..EntityState::default()
                };
                for entry in &c[4..] {
                    let mut parts = entry.splitn(3, ':');
                    let index = number(parts.next().unwrap());
                    let kind = parts.next().unwrap();
                    let name = parts.next().unwrap();
                    let is_self = kind == "S" && entities.has_my_player;
                    entities.nearby_players.push(NearbyPlayer {
                        index,
                        present: kind != "N",
                        is_self,
                        display_name: if is_self {
                            Some("Me".to_string())
                        } else {
                            (name != "~").then(|| name.to_string())
                        },
                        movement_x: (index * 7) % 200 - 20,
                        movement_y: (index * 13) % 150 - 10,
                        size: 1 + index % 3,
                    });
                }
                states.insert(
                    number(c[1]),
                    OpHost {
                        entities,
                        language: language_id(c[3]),
                        outbox: Outbox::default(),
                        model: ComponentModel::new(0, 765, 503),
                    },
                );
            }
            "T" => cases.push(c),
            "Z" => expected_total = number(c[1]) as usize,
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut packets_checked = 0;
    let mut messages_checked = 0;
    let mut skipped = 0;
    for c in &cases {
        let name = c[1];
        let opcode = opcode_named(name);
        let mut instructions = Vec::new();
        let mut host = states[&number(c[2])].clone();
        if name == "OPPLAYERT" {
            let (wanted, selection) = c[3].split_once('|').unwrap();
            if wanted == "~" || cp1252(wanted).is_none() {
                skipped += 1;
                continue;
            }
            instructions.push(text(wanted));
            let s: Vec<&str> = selection.split(':').collect();
            host.model.set_selection(InterfaceSelection {
                active: s[0] == "1",
                flags: number(s[1]),
                slot: number(s[2]),
                hash: number(s[3]),
                item: number(s[4]),
                interface: 0,
                use_cursor: -1,
                default_cursor: 5,
            });
        } else if c[3] != "-" {
            let (operation, wanted) = c[3].split_once('|').unwrap();
            if wanted == "~" || cp1252(wanted).is_none() {
                skipped += 1;
                continue;
            }
            instructions.push(text(wanted));
            instructions.push(constant(number(operation)));
        }
        let initial = instructions.len();
        instructions.push(int(opcode, 0));
        instructions.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let entry = per_opcode.entry(name.to_string()).or_default();
        let packets = if host.outbox.packets.is_empty() {
            "-".to_string()
        } else {
            host.outbox.packets.iter().map(hex).collect::<Vec<_>>().join("|")
        };
        let message = if host.outbox.game_messages.is_empty() {
            "-".to_string()
        } else {
            format!(
                "{}:{}",
                host.outbox.game_messages.len(),
                host.outbox.game_messages[0]
            )
        };
        let after = host.model.selection();
        let selection_after = if name == "OPPLAYERT" {
            format!("{}:{}:{}", u8::from(after.active), after.item, after.default_cursor)
        } else {
            "-".to_string()
        };
        let route = match host.outbox.route_requests.as_slice() {
            [] => "-".to_string(),
            [request] => format!("{}:{}:{}:{}", request.x, request.y, request.size, request.size),
            more => format!("{more:?}"),
        };
        let mut problems = Vec::new();
        match (&result, c[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let ints: Vec<String> = vm.int_stack().iter().map(|v| v.to_string()).collect();
                let ints = if ints.is_empty() { "-".to_string() } else { ints.join(",") };
                if ints != c[5] {
                    problems.push(format!("ints {ints} vs {}", c[5]));
                }
                if packets != c[6] {
                    problems.push(format!("packets {packets} vs {}", c[6]));
                }
                if message != c[7] {
                    problems.push(format!("message {message} vs {}", c[7]));
                }
                if selection_after != c[8] {
                    problems.push(format!("selection {selection_after} vs {}", c[8]));
                }
                if route != c[9] {
                    problems.push(format!("route {route} vs {}", c[9]));
                }
                if packets != "-" {
                    packets_checked += 1;
                }
                if message != "-" {
                    messages_checked += 1;
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial
                    || !class_matches(&expected["error:".len()..], &error.kind)
                    || packets != "-"
                    || message != "-"
                    || route != "-"
                {
                    problems.push(format!("error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => problems.push(format!("{expected} vs {actual:?}")),
        }
        if !problems.is_empty() {
            mismatches.push(format!("{name} state {} {}: {}", c[2], c[3], problems.join("; ")));
        }
    }
    println!(
        "player op cases={} packets={packets_checked} messages={messages_checked} skipped={skipped} mismatches={}",
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
    assert_eq!(per_opcode.len(), 3);
    assert!(packets_checked > 500);
    assert!(messages_checked > 500);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn interface_close_counts_one_request_per_run() {
    let instructions = [int(Opcode::IF_CLOSE, 0), int(Opcode::IF_CLOSE, 0), ret()];
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&instructions, zero(), zero(), &[])).unwrap(),
    )]);
    let mut host = OpHost {
        entities: EntityState::default(),
        language: Some(0),
        outbox: Outbox::default(),
        model: ComponentModel::new(0, 765, 503),
    };
    Vm::default()
        .run_with_host(1, &repository, Arguments::default(), &mut host)
        .unwrap();
    assert_eq!(host.outbox.interface_closes, 2);
    assert_eq!(host.outbox.packets.len(), 2);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn player_op_ops_are_unsupported_without_a_host() {
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
    for opcode in [Opcode::OPPLAYER, Opcode::OPPLAYERT, Opcode::IF_CLOSE] {
        let mut instructions = Vec::new();
        if opcode == Opcode::OPPLAYER {
            instructions.extend([text("a"), constant(1)]);
        }
        if opcode == Opcode::OPPLAYERT {
            instructions.push(text("a"));
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
