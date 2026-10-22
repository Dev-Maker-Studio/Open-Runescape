#![cfg(not(target_arch = "wasm32"))]

use flate2::read::GzDecoder;
use openrs_assets::{Integrity, Limits, decode_container, parse_reference_table, split_archive};
use openrs_client_runtime::vars::config::{
    CONFIG_INDEX,
    idk::{IDK_GROUP, IdkDefinition, decode_idk},
};
use std::{
    collections::BTreeMap,
    fmt::Write as _,
    io::Read,
    path::{Path, PathBuf},
};

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/idk-oracle.tsv.gz"
);
const FIXTURE_SHA256: &str = include_str!("fixtures/idk-oracle.sha256");

fn limits() -> Limits {
    Limits {
        max_container_bytes: 64 << 20,
        max_uncompressed_bytes: 64 << 20,
        max_archives: 65_536,
        max_files_per_archive: 65_536,
        max_total_files: 1 << 21,
        max_archive_id: u16::MAX as u32,
        max_file_id: 65_535,
    }
}

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

fn ints(values: &[i32]) -> String {
    let parts: Vec<String> = values.iter().map(i32::to_string).collect();
    format!("[{}]", parts.join(","))
}

fn half(pairs: &Option<Vec<(i16, i16)>>, second: bool) -> String {
    match pairs {
        None => "null".into(),
        Some(list) => {
            let parts: Vec<String> = list
                .iter()
                .map(|pair| if second { pair.1 } else { pair.0 }.to_string())
                .collect();
            format!("[{}]", parts.join(","))
        }
    }
}

fn line(definition: &IdkDefinition) -> String {
    let body = definition
        .body_models
        .as_ref()
        .map_or_else(|| "null".to_string(), |models| ints(models));
    format!(
        "body={}\trecolorSrc={}\trecolorDst={}\tretexSrc={}\tretexDst={}\thead={}",
        body,
        half(&definition.recolor, false),
        half(&definition.recolor, true),
        half(&definition.retexture, false),
        half(&definition.retexture, true),
        ints(&definition.head_models),
    )
}

#[test]
fn identity_kit_decoder_matches_the_kotlin_loader_on_every_real_kit() {
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
    let reference = trim(&container(&root, &dat, 255, u32::from(CONFIG_INDEX))).to_vec();
    let bytes = decode_container(&reference, Integrity::default(), &limits()).unwrap();
    let table = parse_reference_table(&bytes, &limits()).unwrap();
    let archive = table.archive(IDK_GROUP).unwrap();
    let archive_container =
        trim(&container(&root, &dat, u32::from(CONFIG_INDEX), IDK_GROUP)).to_vec();
    let archive_bytes =
        decode_container(&archive_container, Integrity::default(), &limits()).unwrap();
    let files = split_archive(&archive_bytes, &archive.files, &limits()).unwrap();
    let mut actual = BTreeMap::new();
    for file in &files {
        let definition = decode_idk(&file.data).unwrap();
        actual.insert(file.id, line(&definition));
    }
    let expected = fixture();
    let mut compared = 0usize;
    for text in expected.lines() {
        let (id, rest) = text.split_once('\t').unwrap();
        let id: u32 = id.parse().unwrap();
        assert_eq!(actual.get(&id), Some(&rest.to_string()), "kit {id}");
        compared += 1;
    }
    assert_eq!(compared, actual.len());
    assert!(compared > 600);
}

#[test]
fn identity_kit_decoder_defaults_and_rejects_truncation() {
    let kit = decode_idk(&[0]).unwrap();
    assert_eq!(kit.head_models, [-1; 5]);
    assert!(kit.body_models.is_none());
    let kit = decode_idk(&[
        1, 9, 2, 1, 0x80, 0, 0, 5, 40, 1, 0xff, 0xfe, 0, 3, 61, 0x7f, 0xff, 7, 0,
    ])
    .unwrap();
    assert_eq!(kit.body_models, Some(vec![5]));
    assert_eq!(kit.recolor, Some(vec![(-2, 3)]));
    assert_eq!(kit.head_models[1], -1);
    assert!(decode_idk(&[2, 1]).is_err());
    assert!(decode_idk(&[]).is_err());
}
