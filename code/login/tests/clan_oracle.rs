use openrs_login::clan::{ClanChannel, ClanModel, ClanSettings, ClanVar, Context, Error, VarValue};
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeMap;
use std::fmt::Write;

const CASES: &str = include_str!("fixtures/clan-cases.tsv");

const USER_PROBES: [&str; 13] = [
    "Alice",
    "ALICE",
    "alice",
    "ÃNDRÉ",
    "ÿo",
    "Ÿo",
    "straße",
    "STRASSE",
    "µ",
    "Μ",
    "Zed",
    "bob smith",
    "",
];
const MEMBER_PROBES: [Option<&str>; 7] = [
    None,
    Some(""),
    Some("Alice"),
    Some("alice"),
    Some("Bob Smith"),
    Some("Zed"),
    Some("a"),
];
const FLAG_PROBES: [[i32; 3]; 7] = [
    [0, 0, 31],
    [0, 4, 15],
    [1, 3, 7],
    [2, 31, 31],
    [0, 33, 40],
    [-1, 0, 0],
    [70000, 0, 0],
];
const VAR_PROBE_KEYS: [i32; 4] = [0, 1, 2, 16];
const VAR_BIT_PROBES: [[i32; 3]; 4] = [[1, 0, 31], [2, 4, 15], [16, 3, 7], [0, 33, 40]];

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

fn hex(value: &str) -> Vec<u8> {
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn exception(error: &Error) -> &'static str {
    match error {
        Error::NullPointer(_) => "NullPointerException",
        Error::IndexOutOfBounds(_) => "ArrayIndexOutOfBoundsException",
        Error::NegativeArraySize(_) => "NegativeArraySizeException",
        Error::Runtime(_) => "RuntimeException",
        _ => "other",
    }
}

fn guard(result: Result<String, Error>) -> String {
    match result {
        Ok(text) => text,
        Err(error) => format!("!{}", exception(&error)),
    }
}

fn ints(values: Option<&[i32]>) -> String {
    match values {
        None => "~".to_string(),
        Some(values) => values
            .iter()
            .map(|value| value.to_string())
            .collect::<Vec<_>>()
            .join(" "),
    }
}

fn listed<T>(values: Option<&[T]>, show: impl Fn(&T) -> String) -> String {
    match values {
        None => "~".to_string(),
        Some(values) => format!(
            "{}:{}",
            values.len(),
            values.iter().map(show).collect::<Vec<_>>().join(",")
        ),
    }
}

fn channel(slot: &mut Option<ClanChannel>) -> String {
    let Some(c) = slot else {
        return "~".to_string();
    };
    let mut out = String::new();
    write!(
        out,
        "n={}|k={}|g={}|c={}|p={}|u={}|S=",
        enc(c.clan_name.as_deref()),
        c.min_rank_to_kick,
        c.guests_talk,
        c.num_players,
        c.pointer,
        c.next_update_number
    )
    .unwrap();
    out.push_str(&listed(c.players.as_deref(), |entry| match entry {
        None => "~".to_string(),
        Some(index) => {
            let p = &c.arena[*index];
            format!("{}/{}/{}", enc(p.name.as_deref()), p.rank, p.world)
        }
    }));
    write!(out, "|X={}", ints(c.sorted_name_indices.as_deref())).unwrap();
    let sorted = guard(
        c.get_players_sorted_by_name()
            .map(|values| ints(Some(&values))),
    );
    write!(out, "|H={sorted}").unwrap();
    for probe in USER_PROBES {
        write!(
            out,
            ",{}",
            guard(c.get_user_slot(probe).map(|value| value.to_string()))
        )
        .unwrap();
    }
    out
}

fn boxed<T: ToString>(value: Option<T>) -> String {
    value.map_or_else(|| "~".to_string(), |inner| inner.to_string())
}

fn settings(slot: &mut Option<ClanSettings>) -> String {
    let Some(s) = slot else {
        return "~".to_string();
    };
    let mut out = String::new();
    write!(
        out,
        "co={}|ro={}|bc={}|mc={}|n={}|ag={}|t={}|k={}|l={}|cs={}|dv={}|uc={}|cd={}|uh={}|un={}",
        s.current_owner,
        s.replacement_owner,
        s.ban_count,
        s.member_count,
        enc(s.clan_name.as_deref()),
        u8::from(s.allow_guests),
        s.talk_rank,
        s.kick_rank,
        s.lootshare_rank,
        s.coin_share,
        s.delta_version,
        s.update_count,
        s.creation_day,
        u8::from(s.use_hashes),
        u8::from(s.use_names)
    )
    .unwrap();
    let names = |values: &Option<Vec<Option<String>>>| {
        listed(values.as_deref(), |value| enc(value.as_deref()))
    };
    write!(
        out,
        "|BH={}|BN={}|MN={}|MR={}|MJ={}|MH={}|MF={}|V=",
        listed(s.banned_user_hashes.as_deref(), |v| v.to_string()),
        names(&s.banned_user_names),
        names(&s.member_names),
        listed(s.member_ranks.as_deref(), |v| v.to_string()),
        listed(s.member_join_days.as_deref(), |v| v.to_string()),
        listed(s.member_hashes.as_deref(), |v| v.to_string()),
        listed(s.clan_flags.as_deref(), |v| v.to_string())
    )
    .unwrap();
    match &s.variables {
        None => out.push('~'),
        Some(map) => {
            write!(out, "{}:", map.buckets.len()).unwrap();
            for (index, bucket) in map.buckets.iter().enumerate() {
                if index > 0 {
                    out.push(',');
                }
                for (position, (key, value)) in bucket.iter().enumerate() {
                    if position > 0 {
                        out.push('+');
                    }
                    match value {
                        VarValue::Int(v) => write!(out, "{key}/i/{v}").unwrap(),
                        VarValue::Long(v) => write!(out, "{key}/l/{v}").unwrap(),
                        VarValue::Str(v) => write!(out, "{key}/s/{}", enc(Some(v))).unwrap(),
                    }
                }
            }
        }
    }
    write!(out, "|SA={}", ints(s.sorted_affined_slots.as_deref())).unwrap();
    let sorted = guard(
        s.get_sorted_affined_slots()
            .map(|values| ints(Some(&values))),
    );
    write!(out, "|H={sorted}").unwrap();
    for probe in MEMBER_PROBES {
        write!(
            out,
            ",{}",
            guard(s.get_member_index(probe).map(|value| value.to_string()))
        )
        .unwrap();
    }
    for [slot_index, start, end] in FLAG_PROBES {
        write!(
            out,
            ",{}",
            guard(
                s.get_clan_flag_bitfield(slot_index, start, end)
                    .map(|value| value.to_string())
            )
        )
        .unwrap();
    }
    for key in VAR_PROBE_KEYS {
        write!(
            out,
            ",{}/{}/{}",
            boxed(s.get_int_var(key)),
            boxed(s.get_long_var(key)),
            enc(s.get_string_var(key))
        )
        .unwrap();
    }
    for [key, start, end] in VAR_BIT_PROBES {
        write!(out, ",{}", boxed(s.get_variable_bitfield(key, start, end))).unwrap();
    }
    out
}

fn ticks(model: &ClanModel) -> String {
    format!(
        "lc={};ls={}",
        model.last_clan_channel_update_tick, model.last_clan_settings_update_tick
    )
}

fn clan_vars(model: &ClanModel) -> String {
    listed(model.clan_vars.as_deref(), |value| match value {
        None => "~".to_string(),
        Some(ClanVar::Int(v)) => format!("i{v}"),
        Some(ClanVar::Long(v)) => format!("l{v}"),
        Some(ClanVar::Str(v)) => format!("s{}", enc(Some(v))),
    })
}

fn snapshot(model: &mut ClanModel) -> String {
    let mut out = ticks(model);
    write!(out, ";CC={}", channel(&mut model.clan_channel)).unwrap();
    write!(out, ";LC={}", channel(&mut model.listened_clan_channel)).unwrap();
    write!(out, ";CS={}", settings(&mut model.clan_settings)).unwrap();
    write!(out, ";GS={}", settings(&mut model.guest_clan_settings)).unwrap();
    write!(
        out,
        ";V={};K={};N={}",
        clan_vars(model),
        ints(Some(&model.clan_var_keys)),
        model.clan_var_counter
    )
    .unwrap();
    out
}

fn packet(name: &str) -> ServerPacket {
    match name {
        "CLANCHANNEL_FULL" => ServerPacket::CLANCHANNEL_FULL,
        "CLANCHANNEL_DELTA" => ServerPacket::CLANCHANNEL_DELTA,
        "CLANSETTINGS_FULL" => ServerPacket::CLANSETTINGS_FULL,
        "CLANSETTINGS_DELTA" => ServerPacket::CLANSETTINGS_DELTA,
        "VARCLAN_SET_BYTE" => ServerPacket::VARCLAN_SET_BYTE,
        "VARCLAN_SET_INT" => ServerPacket::VARCLAN_SET_INT,
        "VARCLAN_SET_LONG" => ServerPacket::VARCLAN_SET_LONG,
        "VARCLAN_ENABLE" => ServerPacket::VARCLAN_ENABLE,
        "VARCLAN_DISABLE" => ServerPacket::VARCLAN_DISABLE,
        "SET_CLAN_STRING" => ServerPacket::SET_CLAN_STRING,
        other => panic!("{other}"),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_clan_packet_step_matches_the_actual_kotlin_decoder() {
    let mut model = ClanModel::default();
    let mut mismatches: Vec<String> = Vec::new();
    let mut steps = 0usize;
    let mut expected_total = 0usize;
    let mut sessions = 0usize;
    let mut per_packet: BTreeMap<String, (usize, usize, usize)> = BTreeMap::new();
    for line in CASES.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "R" => {
                sessions += 1;
                model = ClanModel::default();
            }
            "S" => {
                steps += 1;
                let name = c[2];
                let context = Context {
                    server_tick: c[3].parse().unwrap(),
                    clan_var_count: (c[4] != "~").then(|| c[4].parse::<usize>().unwrap()),
                };
                let result = model.apply(packet(name), &hex(c[5]), &context);
                let actual_result = match &result {
                    Ok(()) => "ok",
                    Err(Error::Payload(_)) => "overrun",
                    Err(error) => exception(error),
                };
                let entry = per_packet.entry(name.to_string()).or_default();
                match actual_result {
                    "ok" => entry.0 += 1,
                    "overrun" => entry.2 += 1,
                    _ => entry.1 += 1,
                }
                let actual_snapshot = if actual_result == "overrun" {
                    ticks(&model)
                } else {
                    snapshot(&mut model)
                };
                if actual_result != c[6] || actual_snapshot != c[7] {
                    mismatches.push(format!(
                        "step {} {name} payload={} result {actual_result} ({result:?}) vs {}\n  rust {actual_snapshot}\n  java {}",
                        c[1],
                        c[5],
                        c[6],
                        c[7]
                    ));
                }
            }
            "Z" => expected_total = c[1].parse().unwrap(),
            other => panic!("{other}"),
        }
    }
    println!(
        "clan steps={steps} sessions={sessions} mismatches={}",
        mismatches.len()
    );
    for (name, (ok, error, overrun)) in &per_packet {
        println!("{name} ok={ok} error={error} overrun={overrun}");
    }
    for mismatch in mismatches.iter().take(4) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(steps, expected_total);
    assert_eq!(per_packet.len(), 10);
    assert!(steps >= 3000 && sessions >= 300);
}
