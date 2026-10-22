use openrs_login::world_packets::chat::{ChatHuffman, ChatLine, ChatSession, IgnoredName};
use openrs_login::world_packets::pulse::PulseQueue;
use openrs_login::world_packets::scene::{KnownPlayer, SceneError, SceneEvent, SceneSession};
use openrs_login::world_packets::{self, Error, WorldPacket, requests_var_verify};
use openrs_protocol::payload::PayloadError;
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeMap;
use std::fmt::Write;

const CASES: &str = include_str!("fixtures/world-packets-cases.tsv");

const INNER_NATIVE_EXCEPTION: [&str; 1] = ["OPEN_URL"];
const EARLY_NATIVE_EXCEPTION: [&str; 5] = [
    "SET_CURSOR",
    "CREATE_OBJECT",
    "DESTROY_OBJECT",
    "OBJ_ANIM",
    "OBJECT_PREFETCH",
];

fn safe(unit: u16) -> bool {
    matches!(unit, 0x61..=0x7a | 0x41..=0x5a | 0x30..=0x39 | 0x20 | 0x5f | 0x2d | 0x23 | 0x2e | 0x21)
}

fn enc(value: Option<&str>) -> String {
    let Some(value) = value else {
        return "~".to_string();
    };
    let mut output = String::from("'");
    for unit in value.encode_utf16() {
        if safe(unit) {
            output.push(char::from(unit as u8));
        } else {
            write!(output, "%{unit:04x}").unwrap();
        }
    }
    output
}

fn dec(value: &str) -> Option<String> {
    if value == "~" {
        return None;
    }
    let body = value.strip_prefix('\'').expect("quoted text");
    let mut units = Vec::new();
    let bytes = body.as_bytes();
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'%' {
            units.push(u16::from_str_radix(&body[index + 1..index + 5], 16).unwrap());
            index += 5;
        } else {
            units.push(u16::from(bytes[index]));
            index += 1;
        }
    }
    Some(String::from_utf16(&units).unwrap())
}

fn render_line(line: &ChatLine) -> String {
    format!(
        "{},0,{},{},{},{},{},{}",
        line.kind.native_name(),
        enc(Some(&line.crowned_name)),
        enc(Some(&line.name)),
        enc(Some(&line.simple_name)),
        enc(line.clan.as_deref()),
        line.quickchat_id,
        enc(Some(&line.message))
    )
}

fn session_for(fields: &[&str], tables: &BTreeMap<String, Vec<i8>>) -> ChatSession {
    let table = tables.get(fields[5]).expect("table row");
    let mut session = ChatSession::new(Some(ChatHuffman::new(table)));
    let flags: u8 = fields[6].parse().unwrap();
    session.quickchat_account = flags & 4 != 0;
    session.verified_email = flags & 2 != 0;
    session.quickchat_only = flags & 1 != 0;
    let seed_index: usize = fields[8].parse().unwrap();
    if fields[7] != "-" {
        let seed: Vec<i64> = fields[7].split(',').map(|v| v.parse().unwrap()).collect();
        session.seed_hashes(&seed, seed_index);
    } else {
        session.seed_hashes(&[], seed_index);
    }
    if fields[9] != "-" {
        session.ignored = fields[9]
            .split(',')
            .map(|entry| {
                let (username, display_name) = entry.split_once('|').unwrap();
                IgnoredName {
                    username: dec(username),
                    display_name: dec(display_name),
                }
            })
            .collect();
    }
    session.member_channel = dec(fields[10]);
    session.guest_channel = dec(fields[11]);
    session
}

fn hex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn packets() -> BTreeMap<&'static str, ServerPacket> {
    (0..2048_u16)
        .filter_map(ServerPacket::from_wire)
        .map(|packet| (packet.metadata().name, packet))
        .collect()
}

fn drain(queue: &mut PulseQueue) -> String {
    let mut events = Vec::new();
    while let Some(event) = queue.next_event() {
        events.push(format!(
            "{},{},{},{},{},{}",
            event.pointer >> 56,
            event.pointer & 0x00ff_ffff_ffff_ffff,
            event.primary,
            event.secondary,
            event.tertiary,
            enc(event.text.as_deref())
        ));
    }
    if events.is_empty() {
        "-".to_string()
    } else {
        events.join(";")
    }
}

fn scene_session(fields: &[&str]) -> SceneSession {
    let mut session = SceneSession {
        map_base_x: fields[9].parse().unwrap(),
        map_base_y: fields[10].parse().unwrap(),
        hint_sprite_count: fields[11].parse().unwrap(),
        message_duration: fields[12].parse().unwrap(),
        cycles_per_second: fields[13].parse().unwrap(),
        ..SceneSession::default()
    };
    if fields[14] != "-" {
        for entry in fields[14].split(',') {
            let parts: Vec<&str> = entry.split('|').collect();
            session.players.insert(
                parts[0].parse().unwrap(),
                KnownPlayer {
                    username: dec(parts[1]).unwrap(),
                    display_name: dec(parts[2]),
                    has_appearance: parts[3] == "1",
                    prefix_title: dec(parts[4]),
                    postfix_title: dec(parts[5]),
                },
            );
        }
    }
    session
}

fn scene_chat(fields: &[&str], tables: &BTreeMap<String, Vec<i8>>) -> ChatSession {
    let table = tables.get(fields[5]).expect("table row");
    let mut chat = ChatSession::new(Some(ChatHuffman::new(table)));
    let flags: u8 = fields[6].parse().unwrap();
    chat.quickchat_account = flags & 4 != 0;
    chat.verified_email = flags & 2 != 0;
    chat.quickchat_only = flags & 1 != 0;
    if fields[7] != "-" {
        chat.ignored = fields[7]
            .split(',')
            .map(|entry| {
                let (username, display_name) = entry.split_once('|').unwrap();
                IgnoredName {
                    username: dec(username),
                    display_name: dec(display_name),
                }
            })
            .collect();
    }
    chat
}

fn render_scene(event: &Option<SceneEvent>, session: &SceneSession) -> String {
    match event {
        None => "-".to_string(),
        Some(SceneEvent::PublicChat(chat)) => {
            let overhead = &chat.overhead;
            format!(
                "{}:{},{},{},{},{}|{}",
                chat.player_index,
                enc(Some(&overhead.text)),
                overhead.colour,
                overhead.effect,
                overhead.ticks,
                overhead.ticks,
                render_line(&chat.line)
            )
        }
        Some(SceneEvent::PublicQuickchat(_)) => unreachable!("quickchat is classified only"),
        Some(SceneEvent::HintArrow(arrow)) => format!(
            "{}:{},{},{},{},{},{},{},{},{},{}",
            arrow.slot,
            arrow.target_type,
            arrow.sprite,
            arrow.x,
            arrow.y,
            arrow.plane,
            arrow.height,
            arrow.distance,
            arrow.target_index,
            arrow.denominator,
            arrow.model_id
        ),
        Some(SceneEvent::HintArrowClear { slot }) | Some(SceneEvent::HintTrailClear { slot }) => {
            let _ = session;
            format!("{slot}:null")
        }
        Some(SceneEvent::HintTrail(trail)) => {
            let mut text = format!("{}:{},{}", trail.slot, trail.model_id, trail.placements.len());
            for (x, y) in &trail.placements {
                write!(text, ",{x}:{y}").unwrap();
            }
            text
        }
        Some(SceneEvent::Cutscene(cutscene)) => {
            let mut keys = String::new();
            for xtea in &cutscene.xteas {
                write!(keys, "{},{},{},{};", xtea[0], xtea[1], xtea[2], xtea[3]).unwrap();
            }
            let buffer: String = cutscene
                .buffer
                .iter()
                .map(|byte| format!("{byte:02x}"))
                .collect();
            format!("{}|{}|{}", cutscene.key, keys, buffer)
        }
    }
}

fn replay_scene(
    fields: &[&str],
    table: &BTreeMap<&'static str, ServerPacket>,
    tables: &BTreeMap<String, Vec<i8>>,
    tally: &mut Tally,
) {
    let packet = *table
        .get(fields[1])
        .unwrap_or_else(|| panic!("unknown packet {}", fields[1]));
    let payload = hex(fields[2]);
    let status = fields[3];
    let native_consumed: usize = fields[4].parse().unwrap();
    let context = fields.join("\t");
    let session = scene_session(fields);
    let chat = scene_chat(fields, tables);
    let result = session.decode(&chat, packet, &payload);
    tally.rows += 1;
    tally.scene_rows += 1;
    match status {
        "overrun" => {
            if let Ok(Some(SceneEvent::PublicQuickchat(quickchat))) = &result {
                assert!(quickchat.effects & 0x8000 != 0, "quickchat {context}");
                tally.scene_injected += 1;
            } else {
                assert!(
                    matches!(
                        result,
                        Err(SceneError::Decode(Error::Payload(PayloadError::UnexpectedEof)))
                    ),
                    "overrun {context}: {result:?}"
                );
                tally.scene_overrun += 1;
            }
        }
        "xArrayIndexOutOfBoundsException" => {
            assert!(
                matches!(result, Err(SceneError::ArrayIndexOutOfBounds { .. })),
                "index {context}: {result:?}"
            );
            tally.scene_exception += 1;
        }
        "xNegativeArraySizeException" => {
            assert!(
                matches!(result, Err(SceneError::NegativeArraySize(_))),
                "negative {context}: {result:?}"
            );
            tally.scene_exception += 1;
        }
        "xNullPointerException" => {
            assert_eq!(fields[1], "MESSAGE_PUBLIC", "unexpected NPE {context}");
            match result {
                Ok(Some(SceneEvent::PublicQuickchat(quickchat))) => {
                    assert!(quickchat.effects & 0x8000 != 0, "quickchat {context}");
                    tally.scene_injected += 1;
                }
                other => panic!("quickchat {context}: {other:?}"),
            }
        }
        "ok" => {
            let expected = fields[15];
            if native_consumed == payload.len() {
                let event = result.unwrap_or_else(|error| panic!("exact {context}: {error}"));
                let rendered = match (&event, packet) {
                    (None, ServerPacket::MESSAGE_PUBLIC) => "-|-".to_string(),
                    _ => render_scene(&event, &session),
                };
                assert_eq!(rendered, expected, "scene {context}");
                if event.is_some() {
                    tally.scene_events += 1;
                } else {
                    tally.scene_none += 1;
                }
            } else {
                match result {
                    Err(SceneError::Decode(Error::TrailingBytes { consumed, length })) => {
                        assert_eq!(
                            (consumed, length),
                            (native_consumed, payload.len()),
                            "trailing {context}"
                        );
                        tally.scene_trailing += 1;
                    }
                    other => panic!("trailing {context}: {other:?}"),
                }
            }
        }
        other => panic!("status {other} {context}"),
    }
}

#[derive(Default)]
struct Tally {
    scene_rows: usize,
    scene_events: usize,
    scene_none: usize,
    scene_trailing: usize,
    scene_overrun: usize,
    scene_exception: usize,
    scene_injected: usize,
    rows: usize,
    exact: usize,
    trailing: usize,
    overrun: usize,
    pulse_compared: usize,
    skipped_early: usize,
    chat_rows: usize,
    chat_lines: usize,
    chat_ignored: usize,
    chat_trailing: usize,
    chat_overrun: usize,
    chat_skipped: usize,
}

#[test]
fn native_world_packets_replay() {
    let table = packets();
    let mut tally = Tally::default();
    let mut per_packet: BTreeMap<String, usize> = BTreeMap::new();
    let mut total = None;
    let mut tables: BTreeMap<String, Vec<i8>> = BTreeMap::new();
    for row in CASES.lines() {
        let fields: Vec<&str> = row.split('\t').collect();
        if fields[0] == "Z" {
            total = Some(fields[1].parse::<usize>().unwrap());
            continue;
        }
        if fields[0] == "H" {
            tables.insert(
                fields[1].to_string(),
                hex(fields[2]).into_iter().map(|byte| byte as i8).collect(),
            );
            continue;
        }
        if fields[0] == "S" {
            replay_scene(&fields, &table, &tables, &mut tally);
            *per_packet.entry(fields[1].to_string()).or_default() += 1;
            continue;
        }
        if fields[0] == "M" {
            let packet = *table
                .get(fields[1])
                .unwrap_or_else(|| panic!("unknown packet {}", fields[1]));
            let payload = hex(fields[2]);
            let status = fields[3];
            let native_consumed: usize = fields[4].parse().unwrap();
            let context = row.to_string();
            let mut session = session_for(&fields, &tables);
            let result = session.decode(packet, &payload);
            tally.rows += 1;
            tally.chat_rows += 1;
            *per_packet.entry(fields[1].to_string()).or_default() += 1;
            if status == "overrun" {
                assert!(
                    matches!(result, Err(Error::Payload(PayloadError::UnexpectedEof))),
                    "overrun {context}: {result:?}"
                );
                tally.chat_overrun += 1;
                continue;
            }
            if status != "ok" {
                tally.chat_skipped += 1;
                continue;
            }
            let (hashes, index) = session.hashes();
            let native_index: usize = fields[13].parse().unwrap();
            let seed_slot: usize = fields[8].parse().unwrap();
            let native_hash: i64 = fields[14].parse().unwrap();
            assert_eq!(index, native_index, "hash index {context}");
            assert_eq!(hashes[seed_slot], native_hash, "hash slot {context}");
            let line = if native_consumed == payload.len() {
                result.unwrap_or_else(|error| panic!("exact {context}: {error}"))
            } else {
                match result {
                    Err(Error::TrailingBytes {
                        consumed,
                        length: found,
                    }) => {
                        assert_eq!(
                            (consumed, found),
                            (native_consumed, payload.len()),
                            "trailing {context}"
                        );
                        tally.chat_trailing += 1;
                        continue;
                    }
                    other => panic!("trailing {context}: {other:?}"),
                }
            };
            match line {
                Some(line) => {
                    assert_eq!(render_line(&line), fields[12], "line {context}");
                    tally.chat_lines += 1;
                }
                None => {
                    assert_eq!(fields[12], "-", "ignored {context}");
                    tally.chat_ignored += 1;
                }
            }
            continue;
        }
        assert_eq!(fields[0], "C");
        let name = fields[1];
        let packet = *table
            .get(name)
            .unwrap_or_else(|| panic!("unknown packet {name}"));
        let payload = hex(fields[2]);
        let status = fields[3];
        let native_consumed: usize = fields[4].parse().unwrap();
        let player_index: i32 = fields[5].parse().unwrap();
        let var_verify: i32 = fields[6].parse().unwrap();
        let native_pulses = fields[7];
        let context = row.to_string();

        tally.rows += 1;
        *per_packet.entry(name.to_string()).or_default() += 1;
        assert_eq!(
            var_verify == 1,
            requests_var_verify(packet),
            "var verify {context}"
        );

        let result = world_packets::decode(packet, &payload);
        let length = payload.len();
        if status == "overrun" {
            assert!(
                matches!(result, Err(Error::Payload(PayloadError::UnexpectedEof))),
                "overrun {context}: {result:?}"
            );
            tally.overrun += 1;
            continue;
        }
        if status != "ok"
            && (EARLY_NATIVE_EXCEPTION.contains(&name) || INNER_NATIVE_EXCEPTION.contains(&name))
        {
            tally.skipped_early += 1;
        } else if native_consumed == length {
            let decoded = result
                .as_ref()
                .unwrap_or_else(|error| panic!("exact {context}: {error}"));
            tally.exact += 1;
            let mut queue = PulseQueue::new();
            let expected = match decoded {
                WorldPacket::Pulse(update) => {
                    queue.apply(update);
                    Some(drain(&mut queue))
                }
                WorldPacket::OwnPlayerContent { hash, kind } => {
                    queue.apply(&WorldPacket::own_player_pulse(*hash, *kind, player_index));
                    Some(drain(&mut queue))
                }
                _ => None,
            };
            match expected {
                Some(text) => {
                    assert_eq!(text, native_pulses, "pulses {context}");
                    tally.pulse_compared += 1;
                }
                None => assert_eq!(native_pulses, "-", "unexpected native pulses {context}"),
            }
        } else {
            match result {
                Err(Error::TrailingBytes {
                    consumed,
                    length: found,
                }) => {
                    assert_eq!(
                        (consumed, found),
                        (native_consumed, length),
                        "trailing {context}"
                    );
                    tally.trailing += 1;
                }
                other => panic!("trailing {context}: {other:?}"),
            }
        }
    }
    assert_eq!(total, Some(tally.rows));
    assert!(tally.rows > 5000);
    assert!(
        tally.chat_lines > 500 && tally.chat_ignored > 20 && tally.chat_trailing > 100,
        "chat coverage lines={} ignored={} trailing={}",
        tally.chat_lines,
        tally.chat_ignored,
        tally.chat_trailing
    );
    assert_eq!(tally.chat_skipped, 0);
    assert!(
        tally.scene_events > 500
            && tally.scene_trailing > 20
            && tally.scene_exception > 100
            && tally.scene_injected > 20,
        "scene coverage events={} trailing={} exceptions={} injected={}",
        tally.scene_events,
        tally.scene_trailing,
        tally.scene_exception,
        tally.scene_injected
    );
    println!(
        "scene_rows={} compared_events={} compared_none={} trailing={} overrun={} exceptions={} quickchat_classified_only={}",
        tally.scene_rows,
        tally.scene_events,
        tally.scene_none,
        tally.scene_trailing,
        tally.scene_overrun,
        tally.scene_exception,
        tally.scene_injected
    );
    println!(
        "rows={} exact={} trailing={} overrun={} pulse_compared={} skipped_early={} chat_rows={} chat_lines={} chat_ignored={} chat_trailing={} chat_overrun={} packets={}",
        tally.rows,
        tally.exact,
        tally.trailing,
        tally.overrun,
        tally.pulse_compared,
        tally.skipped_early,
        tally.chat_rows,
        tally.chat_lines,
        tally.chat_ignored,
        tally.chat_trailing,
        tally.chat_overrun,
        per_packet.len()
    );
}
