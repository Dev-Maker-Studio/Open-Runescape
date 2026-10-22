#![cfg(not(target_arch = "wasm32"))]

use flate2::read::GzDecoder;
use openrs_client_runtime::minimenu::{EntrySpec, Font, MenuConfig, MenuInput, MiniMenu};
use openrs_protocol::client_packets::{MenuContext, menu::process_menuclick};
use sha2::{Digest, Sha256};
use std::{fmt::Write as _, io::Read};

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/minimenu-oracle.tsv.gz"
);
const FIXTURE_SHA256: &str = include_str!("fixtures/minimenu-oracle.sha256");

fn int(field: &str) -> i32 {
    field.parse().unwrap()
}

fn flag(field: &str) -> bool {
    field == "1"
}

struct Header {
    base_x: i32,
    base_y: i32,
    selected_hash: i32,
    selected_item: i32,
    selected_slot: i32,
    own_player_index: i32,
}

fn dump_state(menu: &MiniMenu) -> Vec<String> {
    let id = |e: Option<&std::rc::Rc<openrs_client_runtime::minimenu::Entry>>| {
        e.map_or(-1, |e| i64::from(e.id))
    };
    let mut lines = Vec::new();
    lines.push(format!(
        "o\tS\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
        u8::from(menu.is_open),
        u8::from(menu.is_collapsed),
        menu.entry_count,
        menu.colored_entry_count,
        id(menu.active()),
        id(menu.top()),
        id(menu.secondary()),
        menu.menu_open_x,
        menu.menu_open_y,
        menu.menu_width,
        menu.menu_height,
        menu.submenu_x,
        menu.submenu_y,
        menu.submenu_width,
        menu.submenu_height,
        menu.active_submenu_index(),
        menu.deferred_click_type,
        id(menu.deferred_entry()),
    ));
    let mut list = String::from("o\tL");
    for entry in menu.entries() {
        write!(list, "\t{}:{}", entry.id, entry.click_type()).unwrap();
    }
    lines.push(list);
    for group in menu.groups() {
        let mut line = format!("o\tG\t{}\t{}", group.queue_size(), group.tag);
        for entry in group.entries() {
            write!(line, "\t{}", entry.id).unwrap();
        }
        lines.push(line);
    }
    lines
}

fn hex(bytes: &[u8]) -> String {
    let mut out = String::new();
    for b in bytes {
        write!(out, "{b:02x}").unwrap();
    }
    out
}

#[test]
fn minimenu_matches_the_real_kotlin_oracle() {
    let compressed = std::fs::read(FIXTURE).unwrap();
    let mut text = String::new();
    GzDecoder::new(compressed.as_slice())
        .read_to_string(&mut text)
        .unwrap();
    assert_eq!(hex(&Sha256::digest(text.as_bytes())), FIXTURE_SHA256.trim());

    let mut lines = text.lines().peekable();
    let mut menu = MiniMenu::new(MenuConfig::new(765, 503));
    let mut header: Option<Header> = None;
    let mut seeded = false;
    let mut pending_packets: Vec<String> = Vec::new();
    let mut compared = 0usize;
    let mut packets = 0usize;
    let mut cases = 0usize;

    let check_dump = |lines: &mut std::iter::Peekable<std::str::Lines>,
                      menu: &mut MiniMenu,
                      seeded: &mut bool,
                      pending: &mut Vec<String>,
                      context: &str| {
        let mut expected = Vec::new();
        let mut packet_line = String::new();
        while let Some(line) = lines.peek() {
            if line.starts_with("o\tP") {
                packet_line = lines.next().unwrap().to_string();
                break;
            }
            if line.starts_with("o\t") {
                expected.push(lines.next().unwrap().to_string());
            } else {
                break;
            }
        }
        if !*seeded {
            let f: Vec<&str> = expected[0].split('\t').collect();
            menu.menu_open_x = int(f[9]);
            menu.menu_open_y = int(f[10]);
            menu.menu_width = int(f[11]);
            menu.menu_height = int(f[12]);
            menu.submenu_x = int(f[13]);
            menu.submenu_y = int(f[14]);
            menu.submenu_width = int(f[15]);
            menu.submenu_height = int(f[16]);
            *seeded = true;
        }
        assert_eq!(dump_state(menu), expected, "{context}");
        let mut got = String::from("o\tP");
        for p in pending.drain(..) {
            got.push('\t');
            got.push_str(&p);
        }
        assert_eq!(got, packet_line, "{context} packets");
    };

    while let Some(line) = lines.next() {
        let f: Vec<&str> = line.split('\t').collect();
        match f[0] {
            "C" => {
                cases += 1;
                let h = Header {
                    base_x: int(f[16]),
                    base_y: int(f[17]),
                    selected_hash: int(f[12]),
                    selected_item: int(f[13]),
                    selected_slot: int(f[14]),
                    own_player_index: int(f[15]),
                };
                menu.config.font = Font::uniform(6, int(f[9]), int(f[10]), flag(f[2]));
                menu.config.max_visible = int(f[3]);
                menu.config.screen_width = int(f[4]);
                menu.config.screen_height = int(f[5]);
                menu.config.menu_open_mode = int(f[6]);
                menu.config.always_show_context = flag(f[7]);
                menu.config.show_face_here = flag(f[8]);
                menu.config.cutscene = int(f[11]);
                header = Some(h);
            }
            "R" => {
                menu.reset_entries();
                check_dump(
                    &mut lines,
                    &mut menu,
                    &mut seeded,
                    &mut pending_packets,
                    "reset",
                );
                compared += 1;
            }
            "A" => {
                let spec = EntrySpec {
                    action: f[1].to_string(),
                    colored_name: f[2].to_string(),
                    cursor: int(f[3]),
                    click_type: int(f[4]),
                    item_id: int(f[5]),
                    entity_uid: f[6].parse().unwrap(),
                    param0: int(f[7]),
                    param1: int(f[8]),
                    selectable: flag(f[9]),
                    other_level: flag(f[10]),
                    bitpacked: f[11].parse().unwrap(),
                    uncategorized: flag(f[12]),
                };
                menu.add_entry(spec);
            }
            "F" => {
                let run = flag(f[6]);
                let input = MenuInput {
                    primary: flag(f[1]),
                    secondary: flag(f[2]),
                    context: flag(f[3]),
                    open_click: flag(f[4]),
                    minimenu_key: flag(f[5]),
                    dragging: flag(f[7]),
                    item_count: int(f[8]),
                    mouse: (int(f[9]), int(f[10])),
                };
                let outcome = menu.process_input(&input);
                if let Some(click) = outcome.click {
                    let h = header.as_ref().unwrap();
                    let ctx = MenuContext {
                        run,
                        base_x: h.base_x,
                        base_y: h.base_y,
                        selected_hash: h.selected_hash,
                        selected_item: h.selected_item,
                        selected_slot: h.selected_slot,
                        own_player_index: h.own_player_index,
                        container_item: outcome.item_id,
                        ..MenuContext::default()
                    };
                    if let Some(body) = process_menuclick(&click, &ctx).unwrap() {
                        pending_packets.push(format!("{}:{}", body.packet as u16, hex(&body.body)));
                        packets += 1;
                    }
                }
                check_dump(
                    &mut lines,
                    &mut menu,
                    &mut seeded,
                    &mut pending_packets,
                    &format!(
                        "case {cases} step {line} click {:?} item {}",
                        outcome.click, outcome.item_id
                    ),
                );
                compared += 1;
            }
            "E" | "Z" => {}
            other => panic!("unexpected line {other}"),
        }
    }
    assert!(cases >= 1000, "{cases}");
    assert!(compared > 8000, "{compared}");
    assert!(packets > 1000, "{packets}");
}
