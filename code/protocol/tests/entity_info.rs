use openrs_protocol::entity_info::{PlayerBlock, PlayerEvent, PlayerFrame, PlayerView};
use std::collections::BTreeMap;

const PRIME: u64 = 0x100000001b3;
const BASIS: u64 = 0xcbf29ce484222325;

fn fold(mut hash: u64, value: i64) -> u64 {
    let bits = value as u64;
    for shift in 0..8 {
        hash ^= (bits >> (8 * shift)) & 0xff;
        hash = hash.wrapping_mul(PRIME);
    }
    hash
}

fn unhex(text: &str) -> Vec<u8> {
    (0..text.len() / 2)
        .map(|i| u8::from_str_radix(&text[i * 2..i * 2 + 2], 16).unwrap())
        .collect()
}

fn dump(view: &PlayerView) -> String {
    let mut count = 0;
    let mut players = BASIS;
    for index in 1..2048usize {
        if let Some(player) = &view.players[index] {
            count += 1;
            players = fold(players, index as i64);
            players = fold(players, i64::from(player.x));
            players = fold(players, i64::from(player.y));
            players = fold(players, i64::from(player.plane));
            players = fold(players, i64::from(player.face_entity));
            players = fold(players, i64::from(player.pmapdot));
            players = fold(players, i64::from(player.reduce_attack_priority));
        }
    }
    let mut saved = BASIS;
    for index in 0..2048usize {
        if let Some(entry) = &view.saved[index] {
            saved = fold(saved, index as i64);
            saved = fold(saved, i64::from(entry.packed));
            saved = fold(saved, i64::from(entry.face_entity));
            saved = fold(saved, i64::from(entry.pmapdot));
            saved = fold(saved, i64::from(entry.reduce_attack_priority));
        }
    }
    let mut state = BASIS;
    let mut movement = BASIS;
    for index in 1..2048usize {
        state = fold(state, i64::from(view.state[index] as i8));
        movement = fold(movement, i64::from(view.movement_types[index]));
    }
    let mut active = BASIS;
    for index in &view.active {
        active = fold(active, i64::from(*index));
    }
    let mut inactive = BASIS;
    for index in &view.inactive {
        inactive = fold(inactive, i64::from(*index));
    }
    format!(
        "{} {} {:x} {:x} {:x} {:x} {:x} {:x}",
        view.my_plane, count, players, saved, state, movement, active, inactive
    )
}

fn tokens(
    index: u16,
    blocks: &[PlayerBlock],
    view: &PlayerView,
    configs: &mut BTreeMap<u16, BTreeMap<u16, i32>>,
) -> String {
    let entry = configs.entry(index).or_default();
    let mut config_touched = false;
    let mut tint = None;
    let mut reduce = None;
    let mut pmapdot = None;
    let mut face = None;
    let mut movement = false;
    let mut force = None;
    for block in blocks {
        match block {
            PlayerBlock::Tint(value) => tint = Some(value.clone()),
            PlayerBlock::ConfigSet(values) => {
                config_touched = true;
                for value in values {
                    entry.insert(value.key, value.value);
                }
            }
            PlayerBlock::ConfigReset(values) => {
                config_touched = true;
                entry.clear();
                for value in values {
                    entry.insert(value.key, value.value);
                }
            }
            PlayerBlock::ReduceAttackPriority(value) => reduce = Some(*value),
            PlayerBlock::Pmapdot(value) => pmapdot = Some(*value),
            PlayerBlock::FaceEntity(value) => face = Some(*value),
            PlayerBlock::MovementType(_) => movement = true,
            PlayerBlock::ForceMove(value) => force = Some(value.clone()),
            _ => {}
        }
    }
    let mut text = String::new();
    if let Some(tint) = tint {
        text += &format!(
            " T {} {} {} {} {} {}",
            tint.hue, tint.saturation, tint.lightness, tint.opacity, tint.start, tint.end
        );
    }
    if config_touched {
        let list: Vec<String> = (0..16u16)
            .map(|key| entry.get(&key).map_or("_".to_string(), |v| v.to_string()))
            .collect();
        text += &format!(" C {}", list.join(","));
    }
    if let Some(value) = reduce {
        text += &format!(" R {}", i32::from(value));
    }
    if let Some(value) = pmapdot {
        text += &format!(" D {}", i32::from(value));
    }
    if let Some(value) = face {
        text += &format!(" F {value}");
    }
    if movement {
        text += &format!(" Y {}", view.movement_types[usize::from(index)]);
    }
    if let Some(v) = force {
        text += &format!(
            " V {} {} {} {} {} {} {}",
            v.first_x, v.first_y, v.second_x, v.second_y, v.first_delay, v.second_delay, v.direction
        );
    }
    text
}

fn expect_dump(line: &str) -> Option<String> {
    let rest = line.strip_prefix("DUMP ok ")?;
    Some(rest.to_string())
}

fn apply_events(frame: &PlayerFrame, configs: &mut BTreeMap<u16, BTreeMap<u16, i32>>) {
    for event in &frame.events {
        match event {
            PlayerEvent::Entered { index, .. } | PlayerEvent::Left { index } => {
                configs.remove(index);
            }
            PlayerEvent::Moved { .. } => {}
        }
    }
}

#[test]
fn player_info_matches_the_real_client() {
    let path = format!("{}/tests/fixtures/entity-player.tsv", env!("CARGO_MANIFEST_DIR"));
    let text = std::fs::read_to_string(path).unwrap();
    let mut view = PlayerView::new(0);
    let mut configs: BTreeMap<u16, BTreeMap<u16, i32>> = BTreeMap::new();
    let mut scenarios = 0;
    let mut steps = 0;
    let mut terminal = 0;
    let mut errors = 0;
    let mut mask_lines = 0;
    let mut pending: Vec<(u16, Vec<PlayerBlock>)> = Vec::new();
    let mut last_step: Option<Vec<u8>> = None;
    let mut current = 0;
    for line in text.lines() {
        let (head, rest) = line.split_once(' ').unwrap_or((line, ""));
        match head {
            "BEGIN" => {
                let mut parts = rest.split(' ');
                current = parts.next().unwrap().parse::<u32>().unwrap();
                let my: u16 = parts.next().unwrap().parse().unwrap();
                view = PlayerView::new(my);
                view.reset();
                configs.clear();
                pending.clear();
                scenarios += 1;
            }
            "OWN" => {
                view.decode_own_player(&unhex(rest)).unwrap();
            }
            "STEP" => {
                last_step = Some(unhex(rest));
            }
            "TERM" => {
                let bytes = last_step.take().unwrap();
                let mut probe = view.clone();
                let frame = probe
                    .decode(&bytes)
                    .unwrap_or_else(|e| panic!("scenario {current} terminal {rest}: {e}"));
                assert!(!frame.updates.is_empty());
                terminal += 1;
            }
            "DUMP" => {
                if rest == "err" {
                    let bytes = last_step.take().unwrap();
                    let before = view.clone();
                    assert!(view.decode(&bytes).is_err(), "scenario {current} should fail");
                    assert_eq!(view, before, "scenario {current} state changed on error");
                    errors += 1;
                } else if let Some(bytes) = last_step.take() {
                    let frame = view
                        .decode(&bytes)
                        .unwrap_or_else(|e| panic!("scenario {current} step {steps}: {e}"));
                    apply_events(&frame, &mut configs);
                    pending = frame.updates;
                    steps += 1;
                    assert_eq!(
                        expect_dump(line).unwrap(),
                        dump(&view),
                        "scenario {current} step {steps}"
                    );
                } else {
                    assert_eq!(expect_dump(line).unwrap(), dump(&view), "scenario {current} own");
                }
            }
            "BLK" => {
                let mut parts = rest.splitn(2, ' ');
                let index: u16 = parts.next().unwrap().parse().unwrap();
                let expected = parts.next().map(|t| format!(" {t}")).unwrap_or_default();
                let position = pending
                    .iter()
                    .position(|(i, _)| *i == index)
                    .unwrap_or_else(|| panic!("scenario {current}: no update for {index}"));
                let (_, blocks) = pending.remove(position);
                let actual = tokens(index, &blocks, &view, &mut configs);
                assert_eq!(expected, actual, "scenario {current} player {index}");
                mask_lines += 1;
            }
            "END" => {}
            other => panic!("unknown record {other}"),
        }
    }
    assert!(scenarios >= 50);
    assert!(steps > 100);
    assert!(terminal >= 10);
    assert!(errors >= 8);
    assert!(mask_lines > 50);
}
