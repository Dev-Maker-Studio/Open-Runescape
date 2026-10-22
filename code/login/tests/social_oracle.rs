use openrs_login::social::{Context, Error, SocialModel, format_displayname, name_for_base37};
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeMap;
use std::fmt::Write;

const CASES: &str = include_str!("fixtures/social-cases.tsv");

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
    let body = value.strip_prefix('\'').unwrap();
    let mut units: Vec<u16> = Vec::new();
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

fn hex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn snapshot(model: &SocialModel) -> String {
    let mut out = String::new();
    write!(
        out,
        "lf={};lc={};fl={};fn={};F=",
        model.last_friend_ignore_update_tick,
        model.last_friends_chat_update_tick,
        model.friends_loaded,
        model.friends.len()
    )
    .unwrap();
    for (index, friend) in model.friends.iter().enumerate() {
        if index > 0 {
            out.push(',');
        }
        write!(
            out,
            "{}|{}|{}|{}|{}|{}|{}|{}",
            enc(Some(&friend.display_name)),
            enc(Some(&friend.previous_display_name)),
            enc(Some(&friend.world_name)),
            friend.friends_chat_rank,
            friend.platform,
            u8::from(friend.referrer),
            friend.world_id,
            friend.world_flags
        )
        .unwrap();
    }
    write!(out, ";ic={};I=", model.ignore_count).unwrap();
    let mut first = true;
    for (index, slot) in model.ignore_slots.iter().enumerate() {
        if let Some(ignore) = slot {
            if !first {
                out.push(',');
            }
            first = false;
            write!(
                out,
                "{index}:{}|{}|{}",
                enc(Some(&ignore.unfiltered_username)),
                enc(Some(&ignore.display_name)),
                u8::from(ignore.temporary)
            )
            .unwrap();
        }
    }
    write!(
        out,
        ";fcn={};fco={};fcc={};fck={};fcr={};P=",
        enc(model.fc_name.as_deref()),
        enc(model.fc_owner_name.as_deref()),
        model.fc_player_count,
        model.fc_min_rank_can_kick,
        model.my_fc_rank
    )
    .unwrap();
    match &model.fc_players {
        None => out.push('~'),
        Some(players) => {
            let mut first = true;
            for (index, slot) in players.iter().enumerate() {
                if let Some(player) = slot {
                    if !first {
                        out.push(',');
                    }
                    first = false;
                    write!(
                        out,
                        "{index}:{}|{}|{}|{}|{}|{}",
                        player.rank,
                        enc(Some(&player.username)),
                        enc(Some(&player.display_name)),
                        enc(Some(&player.world_name)),
                        player.world_id,
                        enc(player.text_name.as_deref())
                    )
                    .unwrap();
                }
            }
        }
    }
    out.push_str(";Q=");
    for (index, update) in model.login_state_updates.iter().enumerate() {
        if index > 0 {
            out.push(',');
        }
        write!(
            out,
            "{}|{}",
            enc(Some(&update.username)),
            update.action_type
        )
        .unwrap();
    }
    out
}

fn packet(name: &str) -> ServerPacket {
    match name {
        "FRIEND_STATUS" => ServerPacket::FRIEND_STATUS,
        "FRIENDLIST_LOADED" => ServerPacket::FRIENDLIST_LOADED,
        "UPDATE_IGNORE_LIST" => ServerPacket::UPDATE_IGNORE_LIST,
        "ADD_IGNORE" => ServerPacket::ADD_IGNORE,
        "FRIENDS_CHAT_CHANNEL" => ServerPacket::FRIENDS_CHAT_CHANNEL,
        "UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER" => {
            ServerPacket::UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER
        }
        other => panic!("{other}"),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_social_packet_step_matches_the_actual_kotlin_decoder() {
    let mut model = SocialModel::default();
    let mut mismatches: Vec<String> = Vec::new();
    let mut steps = 0usize;
    let mut expected_total = 0usize;
    let mut texts = 0usize;
    let mut names = 0usize;
    let mut sessions = 0usize;
    let mut per_packet: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    for line in CASES.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "T" => {
                texts += 1;
                let input = dec(c[1]).unwrap();
                let actual = enc(format_displayname(&input).as_deref());
                if actual != c[2] {
                    mismatches.push(format!("format_displayname {} {actual} vs {}", c[1], c[2]));
                }
            }
            "N" => {
                names += 1;
                let value: i64 = c[1].parse().unwrap();
                let actual = enc(name_for_base37(value).as_deref());
                if actual != c[2] {
                    mismatches.push(format!("name_for_base37 {value} {actual} vs {}", c[2]));
                }
            }
            "R" => {
                sessions += 1;
                model = SocialModel::default();
                let blank = snapshot(&model);
                assert!(
                    blank.starts_with("lf=0;lc=0;fl=0;fn=0;F=;ic=0;I=;"),
                    "{blank}"
                );
            }
            "S" => {
                steps += 1;
                let name = c[2];
                let tick: i32 = c[3].parse().unwrap();
                let world = (c[4] != "~").then(|| c[4].parse::<i32>().unwrap());
                let me = match c[5] {
                    "!" => None,
                    "~" => Some(None),
                    value => Some(dec(value)),
                };
                let context = Context {
                    server_tick: tick,
                    world_id: world,
                    my_player: me.as_ref().map(|inner| inner.as_deref()),
                };
                let result = model.apply(packet(name), &hex(c[6]), &context);
                let actual_result = match &result {
                    Ok(()) => "ok",
                    Err(Error::NullPointer(_)) => "NullPointerException",
                    Err(Error::IndexOutOfBounds(_)) => "ArrayIndexOutOfBoundsException",
                    Err(_) => "other",
                };
                let entry = per_packet.entry(name.to_string()).or_default();
                if actual_result == "ok" {
                    entry.0 += 1;
                } else {
                    entry.1 += 1;
                }
                let actual_snapshot = snapshot(&model);
                if actual_result != c[7] || actual_snapshot != c[8] {
                    mismatches.push(format!(
                        "step {} {name} payload={} result {actual_result} ({result:?}) vs {}\n  rust {actual_snapshot}\n  java {}",
                        c[1], c[6], c[7], c[8]
                    ));
                }
            }
            "Z" => expected_total = c[1].parse().unwrap(),
            other => panic!("{other}"),
        }
    }
    println!(
        "social steps={steps} sessions={sessions} texts={texts} names={names} mismatches={}",
        mismatches.len()
    );
    for (name, (ok, error)) in &per_packet {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(6) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(steps, expected_total);
    assert_eq!(per_packet.len(), 6);
    assert!(steps >= 3000 && sessions >= 300);
}
