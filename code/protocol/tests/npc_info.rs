use openrs_protocol::entity_info::{NpcBlock, NpcView};

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

fn dump(view: &NpcView) -> String {
    let mut npcs = BASIS;
    for (id, npc) in &view.npcs {
        npcs = fold(npcs, i64::from(*id));
        npcs = fold(npcs, i64::from(npc.x));
        npcs = fold(npcs, i64::from(npc.y));
        npcs = fold(npcs, i64::from(npc.plane));
    }
    let mut updates = BASIS;
    for index in &view.update_indices {
        updates = fold(updates, i64::from(*index));
    }
    format!(
        "{} {} {:x} {:x}",
        view.npcs.len(),
        view.update_indices.len(),
        npcs,
        updates
    )
}

fn hex_chars(text: &str) -> String {
    if text.is_empty() {
        return "6e.75.6c.6c".to_string();
    }
    text.chars()
        .map(|c| format!("{:x}", c as u32))
        .collect::<Vec<_>>()
        .join(".")
}

fn tokens(blocks: &[NpcBlock]) -> String {
    let mut force = None;
    let mut render = None;
    let mut face = None;
    let mut level = None;
    let mut tile = None;
    let mut tint = None;
    let mut name = None;
    for block in blocks {
        match block {
            NpcBlock::ForceMove(v) => force = Some(v.clone()),
            NpcBlock::RenderAnim(v) => render = Some(*v),
            NpcBlock::FaceEntity(v) => face = Some(*v),
            NpcBlock::CombatLevel(v) => level = Some(*v),
            NpcBlock::FaceTile { x, y } => tile = Some((*x, *y)),
            NpcBlock::Tint(v) => tint = Some(v.clone()),
            NpcBlock::Name(v) => name = Some(v.clone()),
            _ => {}
        }
    }
    let mut text = String::new();
    if let Some(v) = force {
        text += &format!(
            " V {} {} {} {} {} {} {}",
            v.first_x, v.first_y, v.second_x, v.second_y, v.first_delay, v.second_delay, v.direction
        );
    }
    if let Some(v) = render {
        text += &format!(" R {v}");
    }
    if let Some(v) = face {
        text += &format!(" F {v}");
    }
    if let Some(v) = level {
        text += &format!(" L {}", v.map_or(-2, i32::from));
    }
    if let Some((x, y)) = tile {
        text += &format!(" X {x} {y}");
    }
    if let Some(v) = tint {
        text += &format!(
            " T {} {} {} {} {} {}",
            v.hue, v.saturation, v.lightness, v.opacity, v.start, v.end
        );
    }
    if let Some(v) = name {
        text += &format!(" N {}", hex_chars(&v));
    }
    text
}

fn normalise(line: &str) -> String {
    line.to_string()
}

#[test]
fn npc_info_matches_the_real_client() {
    let path = format!("{}/tests/fixtures/entity-npc.tsv", env!("CARGO_MANIFEST_DIR"));
    let text = std::fs::read_to_string(path).unwrap();
    let mut view = NpcView::new();
    let mut base = (0, 0);
    let mut scenarios = 0;
    let mut steps = 0;
    let mut terminal = 0;
    let mut errors = 0;
    let mut mask_lines = 0;
    let mut pending: Vec<(u16, Vec<NpcBlock>)> = Vec::new();
    let mut last_step: Option<(bool, Vec<u8>)> = None;
    let mut current = 0;
    let types = |_: i32| None;
    for line in text.lines() {
        let (head, rest) = line.split_once(' ').unwrap_or((line, ""));
        match head {
            "BEGIN" => {
                let mut parts = rest.split(' ');
                current = parts.next().unwrap().parse::<u32>().unwrap();
                base = (
                    parts.next().unwrap().parse().unwrap(),
                    parts.next().unwrap().parse().unwrap(),
                );
                view = NpcView::new();
                pending.clear();
                scenarios += 1;
            }
            "STEP" => {
                let (large, bytes) = rest.split_once(' ').unwrap_or((rest, ""));
                last_step = Some((large == "1", unhex(bytes)));
            }
            "TERM" => {
                let (large, bytes) = last_step.take().unwrap();
                let mut probe = view.clone();
                let frame = probe
                    .decode(&bytes, large, base, &types)
                    .unwrap_or_else(|e| panic!("scenario {current} terminal {rest}: {e}"));
                assert!(!frame.updates.is_empty());
                terminal += 1;
            }
            "DUMP" => {
                let (large, bytes) = last_step.take().unwrap();
                if rest == "err" {
                    let before = view.clone();
                    assert!(
                        view.decode(&bytes, large, base, &types).is_err(),
                        "scenario {current} should fail"
                    );
                    assert_eq!(view, before, "scenario {current} state changed on error");
                    errors += 1;
                } else {
                    let frame = view
                        .decode(&bytes, large, base, &types)
                        .unwrap_or_else(|e| panic!("scenario {current} step {steps}: {e}"));
                    pending = frame.updates;
                    steps += 1;
                    assert_eq!(
                        normalise(rest.strip_prefix("ok ").unwrap()),
                        dump(&view),
                        "scenario {current} step {steps}"
                    );
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
                assert_eq!(expected, tokens(&blocks), "scenario {current} npc {index}");
                mask_lines += 1;
            }
            "END" => {}
            other => panic!("unknown record {other}"),
        }
    }
    assert!(scenarios >= 150);
    assert!(steps > 500);
    assert!(terminal >= 40);
    assert!(errors >= 20);
    assert!(mask_lines > 500);
}
