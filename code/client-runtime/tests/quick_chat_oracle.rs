#![cfg(not(target_arch = "wasm32"))]

use flate2::read::GzDecoder;
use openrs_assets::Integrity;
use openrs_client_runtime::vars::quick_chat::{
    CATEGORY_ARCHIVE, MENU_INDEX, MESSAGE_ARCHIVE, MESSAGE_INDEX, QuickChatStore,
};
use openrs_cs2::{QuickChatDefs, QuickChatSearch};
use std::{
    fmt::Write as _,
    io::Read,
    path::{Path, PathBuf},
};

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/quick-chat-oracle.tsv.gz"
);
const FIXTURE_SHA256: &str = include_str!("fixtures/quick-chat-oracle.sha256");

fn container(root: &Path, dat: &[u8], index: u32, archive: u32) -> Vec<u8> {
    let idx = std::fs::read(root.join(format!("main_file_cache.idx{index}"))).unwrap();
    let entry = &idx[archive as usize * 6..archive as usize * 6 + 6];
    let size = u32::from_be_bytes([0, entry[0], entry[1], entry[2]]) as usize;
    let mut sector = u32::from_be_bytes([0, entry[3], entry[4], entry[5]]) as usize;
    let header = if archive > 0xFFFF { 10 } else { 8 };
    let mut out = Vec::with_capacity(size);
    while out.len() < size {
        let block = &dat[sector * 520..sector * 520 + 520];
        let take = (size - out.len()).min(520 - header);
        out.extend_from_slice(&block[header..header + take]);
        sector = u32::from_be_bytes([0, block[header - 4], block[header - 3], block[header - 2]])
            as usize;
    }
    out
}

fn trim(container: &[u8]) -> &[u8] {
    let length = u32::from_be_bytes(container[1..5].try_into().unwrap()) as usize;
    let body = if container[0] == 0 {
        5 + length
    } else {
        9 + length
    };
    assert!(container.len() == body || container.len() == body + 2);
    &container[..body]
}

fn fixture() -> String {
    let compressed = std::fs::read(FIXTURE).unwrap();
    let mut text = Vec::new();
    GzDecoder::new(compressed.as_slice())
        .read_to_end(&mut text)
        .unwrap();
    let digest = {
        use sha2::{Digest as _, Sha256};
        let mut hasher = Sha256::new();
        hasher.update(&text);
        hasher
            .finalize()
            .iter()
            .fold(String::new(), |mut out, byte| {
                write!(out, "{byte:02x}").unwrap();
                out
            })
    };
    assert_eq!(digest, FIXTURE_SHA256.trim());
    String::from_utf8(text).unwrap()
}

fn encode(value: &str) -> String {
    let mut out = String::from("u");
    for unit in value.encode_utf16() {
        write!(out, "{unit:04x}").unwrap();
    }
    out
}

fn decode(value: &str) -> Option<String> {
    if value == "null" {
        return None;
    }
    let units: Vec<u16> = (0..(value.len() - 1) / 4)
        .map(|index| u16::from_str_radix(&value[1 + index * 4..5 + index * 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn join<T: ToString>(values: impl IntoIterator<Item = T>) -> String {
    let parts: Vec<String> = values.into_iter().map(|value| value.to_string()).collect();
    if parts.is_empty() {
        "-".to_string()
    } else {
        parts.join(",")
    }
}

fn category_line(defs: &QuickChatDefs, id: i32) -> String {
    let category = defs.category(id);
    let name = category.name.as_deref().map_or("null".to_string(), encode);
    format!(
        "C\t{id}\t{name}\t{}\t{}\t{}\t{}",
        join(category.sub_categories.iter().flatten()),
        join(category.sub_category_hotkeys.iter().flatten()),
        join(category.messages.iter().flatten()),
        join(category.message_hotkeys.iter().flatten()),
    )
}

fn message_line(defs: &QuickChatDefs, id: i32) -> String {
    let message = defs.message(id);
    let text = encode(&message.placeholder_text());
    let mut commands = Vec::new();
    for index in 0..message.value_type_count() {
        let kind = match message.value_type(index) {
            Ok(Some(kind)) => kind.id.to_string(),
            _ => "E".to_string(),
        };
        let mut entry = format!("{index}:{kind}");
        if kind == "0" {
            for config in -1..=4 {
                match message.value_config(index, config) {
                    Ok(value) => write!(entry, ":{value}").unwrap(),
                    Err(_) => entry.push_str(":E"),
                }
            }
        }
        commands.push(entry);
    }
    format!(
        "M\t{id}\t{text}\t{}\t{}",
        join(message.responses.iter().flatten()),
        join(commands),
    )
}

fn search_line(defs: &QuickChatDefs, query: &str, menu: &str) -> String {
    let text = decode(query).unwrap();
    let outcome = defs.search(&text, menu == "1");
    let (count, ids) = match outcome {
        QuickChatSearch::Overflow => ("-1".to_string(), "-".to_string()),
        QuickChatSearch::Found(ids) => (
            ids.len().to_string(),
            join(ids.iter().map(|id| i32::from(*id))),
        ),
    };
    format!("S\t{query}\t{menu}\t{count}\t{ids}")
}

#[test]
fn quick_chat_decoders_match_the_kotlin_loaders_on_every_real_definition() {
    let root = match std::env::var_os("OPENRS_CACHE") {
        Some(path) => PathBuf::from(path),
        None => match std::env::var_os("HOME") {
            Some(home) => PathBuf::from(home).join(".darkanrs/caches/darkan/LIVE"),
            None => return,
        },
    };
    let Ok(dat) = std::fs::read(root.join("main_file_cache.dat2")) else {
        return;
    };
    let mut store = QuickChatStore::new();
    for index in [MESSAGE_INDEX, MENU_INDEX] {
        let reference = trim(&container(&root, &dat, 255, u32::from(index))).to_vec();
        store
            .load_reference(index, &reference, Integrity::default())
            .unwrap();
        for archive in [CATEGORY_ARCHIVE, MESSAGE_ARCHIVE] {
            let bytes = trim(&container(&root, &dat, u32::from(index), archive)).to_vec();
            store.load_archive(index, archive, &bytes).unwrap();
        }
    }
    let defs = store.build().unwrap();
    let expected = fixture();
    let (mut categories, mut messages, mut searches) = (0, 0, 0);
    let mut total = 0;
    for line in expected.lines() {
        let columns: Vec<_> = line.split('\t').collect();
        let actual = match columns[0] {
            "C" => {
                categories += 1;
                category_line(&defs, columns[1].parse().unwrap())
            }
            "M" => {
                messages += 1;
                message_line(&defs, columns[1].parse().unwrap())
            }
            "S" => {
                searches += 1;
                search_line(&defs, columns[1], columns[2])
            }
            other => panic!("{other}"),
        };
        assert_eq!(actual, line);
        total += 1;
    }
    assert_eq!(total, 1524);
    assert_eq!((categories, messages, searches), (245, 1219, 60));
}
