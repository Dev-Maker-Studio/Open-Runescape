#![cfg(not(target_arch = "wasm32"))]

use flate2::read::GzDecoder;
use openrs_assets::{
    ArchiveFile, Integrity, Limits, ReferenceTable, decode_container, parse_reference_table,
    split_archive,
};
use openrs_client_runtime::vars::config::{
    CONFIG_INDEX, ConfigStore, DEFAULTS_INDEX, INTERACTIONS_FILE, INVENTORY_GROUP, ITEM_INDEX,
    NPC_INDEX, PARAM_GROUP, SEQ_INDEX, STRUCT_GROUP, decode, sync_containers,
};
use openrs_cs2::{ConfigKind, EnumValue, ItemModel, ParamTable};
use openrs_login::item_containers::{ItemContainers, Limits as ContainerLimits};
use openrs_protocol::js5::VersionEntry;
use openrs_protocol::registry::ServerPacket;
use std::{
    collections::{BTreeMap, HashMap},
    fmt::Write as _,
    io::Read,
    path::PathBuf,
    sync::OnceLock,
};
use whirlpool::{Digest, Whirlpool};

const PARAM: u32 = 1;
const ITEM: u32 = 2;
const NPC: u32 = 3;
const SEQ: u32 = 4;
const STRUCT: u32 = 5;
const CONTAINER: u32 = 6;
const INTERACTION: u32 = 7;

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/config-oracle.tsv.gz"
);
const FIXTURE_SHA256: &str = include_str!("fixtures/config-oracle.sha256");

struct Cache {
    root: PathBuf,
    dat: Vec<u8>,
}

impl Cache {
    fn open() -> Option<Self> {
        let root = match std::env::var_os("OPENRS_CACHE") {
            Some(path) => PathBuf::from(path),
            None => PathBuf::from(std::env::var_os("HOME")?).join(".darkanrs/caches/darkan/LIVE"),
        };
        let dat = std::fs::read(root.join("main_file_cache.dat2")).ok()?;
        Some(Self { root, dat })
    }

    fn container(&self, index: u32, archive: u32) -> Vec<u8> {
        let idx = std::fs::read(self.root.join(format!("main_file_cache.idx{index}"))).unwrap();
        let entry = &idx[archive as usize * 6..archive as usize * 6 + 6];
        let size = u32::from_be_bytes([0, entry[0], entry[1], entry[2]]) as usize;
        let mut sector = u32::from_be_bytes([0, entry[3], entry[4], entry[5]]) as usize;
        let header = if archive > 0xFFFF { 10 } else { 8 };
        let mut out = Vec::with_capacity(size);
        while out.len() < size {
            let block = &self.dat[sector * 520..sector * 520 + 520];
            let take = (size - out.len()).min(520 - header);
            out.extend_from_slice(&block[header..header + take]);
            sector =
                u32::from_be_bytes([0, block[header - 4], block[header - 3], block[header - 2]])
                    as usize;
        }
        out
    }
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

fn version_entry(reference_container: &[u8]) -> VersionEntry {
    let digest = Whirlpool::digest(reference_container);
    let mut whirlpool = [0u8; 64];
    whirlpool.copy_from_slice(digest.as_slice());
    let bytes = decode_container(reference_container, Integrity::default(), &limits()).unwrap();
    let table = parse_reference_table(&bytes, &limits()).unwrap();
    VersionEntry {
        crc: crc32fast::hash(reference_container) as i32,
        revision: table.revision,
        whirlpool,
    }
}

struct Loaded {
    store: ConfigStore,
    reference: BTreeMap<u8, ReferenceTable>,
    files: BTreeMap<(u8, u32), Vec<ArchiveFile>>,
    reference_containers: BTreeMap<u8, Vec<u8>>,
}

fn load(cache: &Cache) -> Loaded {
    let mut store = ConfigStore::new();
    let mut reference = BTreeMap::new();
    let mut files = BTreeMap::new();
    let mut reference_containers = BTreeMap::new();
    for index in [
        CONFIG_INDEX,
        NPC_INDEX,
        ITEM_INDEX,
        SEQ_INDEX,
        DEFAULTS_INDEX,
    ] {
        let container = trim(&cache.container(255, u32::from(index))).to_vec();
        let version = version_entry(&container);
        store.load_reference(index, &container, &version).unwrap();
        let bytes = decode_container(&container, Integrity::default(), &limits()).unwrap();
        let table = parse_reference_table(&bytes, &limits()).unwrap();
        for archive in &table.archives {
            let wanted = match index {
                CONFIG_INDEX => [INVENTORY_GROUP, PARAM_GROUP, STRUCT_GROUP].contains(&archive.id),
                DEFAULTS_INDEX => archive.id == INTERACTIONS_FILE,
                _ => true,
            };
            if !wanted {
                continue;
            }
            let archive_container = trim(&cache.container(u32::from(index), archive.id)).to_vec();
            store
                .load_archive(index, archive.id, &archive_container)
                .unwrap();
            let bytes =
                decode_container(&archive_container, Integrity::default(), &limits()).unwrap();
            files.insert(
                (index, archive.id),
                split_archive(&bytes, &archive.files, &limits()).unwrap(),
            );
        }
        reference.insert(index, table);
        reference_containers.insert(index, container);
    }
    Loaded {
        store,
        reference,
        files,
        reference_containers,
    }
}

fn utf16(value: &str) -> String {
    let mut out = String::from("u");
    for (position, unit) in value.encode_utf16().enumerate() {
        if position > 0 {
            out.push('.');
        }
        write!(out, "{unit:x}").unwrap();
    }
    out
}

fn text(value: Option<&str>) -> String {
    value.map_or_else(|| "null".to_string(), utf16)
}

fn texts<S: AsRef<str>>(values: &[Option<S>]) -> String {
    let parts: Vec<String> = values
        .iter()
        .map(|value| text(value.as_ref().map(AsRef::as_ref)))
        .collect();
    format!("[{}]", parts.join(","))
}

fn ints(values: &[i32]) -> String {
    let parts: Vec<String> = values.iter().map(i32::to_string).collect();
    format!("[{}]", parts.join(","))
}

fn params(table: Option<&ParamTable>) -> String {
    let Some(table) = table else {
        return "null".into();
    };
    let parts: Vec<String> = table
        .iter()
        .map(|(key, value)| match value {
            EnumValue::Int(number) => format!("{key}:i{number}"),
            EnumValue::Text(string) => format!("{key}:{}", utf16(string)),
        })
        .collect();
    format!("{{{}}}", parts.join(","))
}

fn flag(value: bool) -> u8 {
    u8::from(value)
}

fn file_of(loaded: &Loaded, index: u8, archive: u32, file: u32) -> Option<&[u8]> {
    loaded
        .files
        .get(&(index, archive))?
        .iter()
        .find(|candidate| candidate.id == file)
        .map(|candidate| candidate.data.as_slice())
}

fn ids(loaded: &Loaded, index: u8, archive: u32) -> Vec<u32> {
    loaded.reference[&index]
        .archive(archive)
        .map(|entry| entry.files.iter().map(|file| file.id).collect())
        .unwrap_or_default()
}

fn item_line(loaded: &Loaded, id: u32) -> String {
    let definition = loaded.store.item_definition(id as i32).unwrap().unwrap();
    let item = &*definition;
    let raw = decode::decode_item(file_of(loaded, ITEM_INDEX, id >> 8, id & 255).unwrap()).unwrap();
    format!(
        "name={}\tvalue={}\tstackable={}\tmembers={}\twear={},{},{}\tmulti={}\ttooltip={},{}\tcert={},{}\tlend={},{}\tbind={},{}\tcursor={},{},{},{}\ttradeable={}\tunk6={}\tground={}\tinventory={}\tparams={}",
        utf16(&item.name),
        item.value,
        item.stackable,
        flag(item.members_only),
        item.wear_pos,
        item.wear_pos2,
        item.wear_pos3,
        item.multi_stack_size,
        flag(item.has_tooltip_color),
        item.tooltip_color,
        item.cert_id,
        item.cert_template_id,
        raw.lend_id,
        raw.lend_template_id,
        raw.bind_id,
        raw.bind_template_id,
        item.cursor_op1,
        item.cursor_id1,
        item.cursor_op2,
        item.cursor_id2,
        flag(raw.tradeable),
        raw.unknown_int6,
        texts(&item.ground_actions),
        texts(&item.inventory_actions),
        params(item.params.as_ref()),
    )
}

fn rust_lines(loaded: &Loaded) -> Vec<(u32, u32, String)> {
    let mut out = Vec::new();
    for file in ids(loaded, CONFIG_INDEX, PARAM_GROUP) {
        let definition = loaded.store.param_definition(file as i32).unwrap().unwrap();
        let name = if &*definition.type_name == "null" {
            "null".to_string()
        } else {
            utf16(&definition.type_name)
        };
        out.push((
            PARAM,
            file,
            format!(
                "string={}\tdefault={}\tname={name}",
                flag(definition.is_string),
                definition.default_int
            ),
        ));
    }
    let item_ids: Vec<u32> = loaded.reference[&ITEM_INDEX]
        .archives
        .iter()
        .flat_map(|archive| {
            archive
                .files
                .iter()
                .map(move |file| archive.id << 8 | file.id)
        })
        .collect();
    for id in item_ids {
        out.push((ITEM, id, item_line(loaded, id)));
    }
    for (kind, index, shift, mask) in [(NPC, NPC_INDEX, 7, 127), (SEQ, SEQ_INDEX, 7, 127)] {
        let config_kind = if kind == NPC {
            ConfigKind::Npc
        } else {
            ConfigKind::Seq
        };
        let combined: Vec<u32> = loaded.reference[&index]
            .archives
            .iter()
            .flat_map(|archive| {
                archive
                    .files
                    .iter()
                    .map(move |file| archive.id << shift | file.id)
            })
            .collect();
        for id in combined {
            assert!(file_of(loaded, index, id >> shift, id & mask).is_some());
            let table = loaded.store.config_params(config_kind, id as i32).unwrap();
            out.push((kind, id, format!("params={}", params(table.as_deref()))));
        }
    }
    for file in ids(loaded, CONFIG_INDEX, STRUCT_GROUP) {
        let table = loaded
            .store
            .config_params(ConfigKind::Struct, file as i32)
            .unwrap();
        out.push((STRUCT, file, format!("params={}", params(table.as_deref()))));
    }
    let mut model = ItemModel::default();
    loaded.store.populate_container_types(&mut model).unwrap();
    for file in ids(loaded, CONFIG_INDEX, INVENTORY_GROUP) {
        let container = &model.container_types[&(file as i32)];
        out.push((
            CONTAINER,
            file,
            format!(
                "max={}\tsize={}\tids={}\tamounts={}",
                container.max_item_size,
                container.default_item_size,
                ints(&container.item_ids),
                ints(&container.amounts)
            ),
        ));
    }
    let colours = loaded.store.interaction_colours().unwrap();
    out.push((
        INTERACTION,
        0,
        format!("members={}\tfree={}", colours.members, colours.free),
    ));
    out
}

fn fixture() -> Vec<u8> {
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
    text
}

fn oracle() -> HashMap<(u32, u32), String> {
    let text = String::from_utf8(fixture()).unwrap();
    text.lines()
        .map(|line| {
            let mut parts = line.splitn(3, '\t');
            let kind = parts.next().unwrap().parse().unwrap();
            let id = parts.next().unwrap().parse().unwrap();
            let mut fields = parts.next().unwrap().to_string();
            if kind == PARAM {
                let cut = fields.rfind("\tautoDisable=").unwrap();
                fields.truncate(cut);
                fields = fields.replace("name=u6e.75.6c.6c", "name=null");
            }
            ((kind, id), fields)
        })
        .collect()
}

fn loaded() -> Option<&'static Loaded> {
    static LOADED: OnceLock<Option<Loaded>> = OnceLock::new();
    LOADED
        .get_or_init(|| Cache::open().map(|cache| load(&cache)))
        .as_ref()
}

fn compare(kind: u32, name: &str) {
    let Some(loaded) = loaded() else {
        eprintln!("skipping {name}: no cache");
        return;
    };
    let expected = oracle();
    let lines = rust_lines(loaded);
    let mut compared = 0usize;
    let mut mismatches = Vec::new();
    for (line_kind, id, line) in &lines {
        if *line_kind != kind {
            continue;
        }
        compared += 1;
        let want = expected
            .get(&(kind, *id))
            .unwrap_or_else(|| panic!("oracle has no {name} {id}"));
        if want != line {
            mismatches.push(format!("{name} {id}\n  kotlin {want}\n  rust   {line}"));
        }
    }
    let oracle_count = expected.keys().filter(|(k, _)| *k == kind).count();
    assert_eq!(compared, oracle_count, "{name} id sets differ");
    eprintln!(
        "{name}: compared {compared}, mismatched {}",
        mismatches.len()
    );
    assert!(
        mismatches.is_empty(),
        "{} {name} mismatches, first:\n{}",
        mismatches.len(),
        mismatches
            .iter()
            .take(5)
            .cloned()
            .collect::<Vec<_>>()
            .join("\n")
    );
}

#[test]
fn param_definitions_match_the_kotlin_loader() {
    compare(PARAM, "param");
}

#[test]
fn items_with_note_lend_and_bind_derivation_match_the_kotlin_loader() {
    compare(ITEM, "item");
}

#[test]
fn npc_parameters_match_the_kotlin_loader() {
    compare(NPC, "npc");
}

#[test]
fn seq_parameters_match_the_kotlin_loader() {
    compare(SEQ, "seq");
}

#[test]
fn struct_parameters_match_the_kotlin_loader() {
    compare(STRUCT, "struct");
}

#[test]
fn inventory_types_match_the_kotlin_loader() {
    compare(CONTAINER, "inventory");
}

#[test]
fn interaction_colours_match_the_kotlin_loader() {
    compare(INTERACTION, "interaction");
}

#[test]
fn populate_items_agrees_with_the_derived_definitions() {
    let Some(loaded) = loaded() else {
        return;
    };
    let mut model = ItemModel::default();
    loaded.store.populate_items(&mut model).unwrap();
    let expected = oracle();
    let mut checked = 0usize;
    let mut max_id = 0usize;
    for (&(kind, id), line) in &expected {
        if kind != ITEM {
            continue;
        }
        max_id = max_id.max(id as usize);
        let record = &model.items[id as usize];
        let fields: HashMap<&str, &str> = line
            .split('\t')
            .filter_map(|field| field.split_once('='))
            .collect();
        assert_eq!(utf16(&record.name), fields["name"], "name {id}");
        assert_eq!(
            format!("{}", flag(record.tradeable)),
            fields["tradeable"],
            "tradeable {id}"
        );
        assert_eq!(record.unknown_int6.to_string(), fields["unk6"], "unk6 {id}");
        let cert: Vec<&str> = fields["cert"].split(',').collect();
        let lend: Vec<&str> = fields["lend"].split(',').collect();
        let bind: Vec<&str> = fields["bind"].split(',').collect();
        assert_eq!(record.cert_template_id.to_string(), cert[1], "cert {id}");
        assert_eq!(record.lend_template_id.to_string(), lend[1], "lend {id}");
        assert_eq!(record.bind_template_id.to_string(), bind[1], "bind {id}");
        assert_eq!(
            params(record.params.as_ref()),
            fields["params"],
            "params {id}"
        );
        checked += 1;
    }
    assert_eq!(model.items.len(), max_id + 1);
    eprintln!("populate_items: checked {checked} records");
    assert!(checked > 20_000);
}

#[test]
fn populate_container_types_fills_every_inventory_with_the_kotlin_values() {
    let Some(loaded) = loaded() else {
        return;
    };
    let mut model = ItemModel::default();
    loaded.store.populate_container_types(&mut model).unwrap();
    let expected = oracle();
    let count = expected
        .keys()
        .filter(|(kind, _)| *kind == CONTAINER)
        .count();
    assert_eq!(model.container_types.len(), count);
    let bank = &model.container_types[&95];
    let line = &expected[&(CONTAINER, 95)];
    assert!(line.starts_with(&format!(
        "max={}\tsize={}",
        bank.max_item_size, bank.default_item_size
    )));
}

#[test]
fn sync_containers_exposes_real_item_slots_to_the_item_model() {
    let Some(loaded) = loaded() else {
        return;
    };
    let mut model = ItemModel::default();
    loaded.store.populate_items(&mut model).unwrap();
    let coins = model
        .items
        .iter()
        .position(|record| &*record.name == "Coins")
        .unwrap();
    let mut containers = ItemContainers::new(ContainerLimits::default()).unwrap();
    let mut payload = vec![0x00, 0x5f, 2];
    payload.push(3 << 2 | 2);
    payload.extend_from_slice(&((coins as u16) + 1).to_be_bytes());
    payload.push(255);
    payload.extend_from_slice(&1_000_000i32.to_be_bytes());
    payload.extend_from_slice(b"stack\0");
    payload.push(5 << 2 | 1);
    payload.extend_from_slice(&((coins as u16) + 1).to_be_bytes());
    payload.push(7);
    containers
        .apply(ServerPacket::UPDATE_INV_PARTIAL, &payload)
        .unwrap();
    sync_containers(&mut model, &containers);
    let synced = &model.containers[&95i64];
    assert_eq!(synced.item_ids.len(), 6);
    assert_eq!(synced.item_ids[3], coins as i32);
    assert_eq!(synced.amounts[3], 1_000_000);
    assert_eq!(synced.item_ids[5], coins as i32);
    assert_eq!(synced.amounts[5], 7);
    assert_eq!(synced.item_ids[0], -1);
    assert_eq!(synced.tooltips[3].as_deref(), Some("stack"));
    assert_eq!(synced.tooltips[5], None);
    assert_eq!(
        model.items[synced.item_ids[3] as usize].name.as_ref(),
        "Coins"
    );
}

#[test]
fn reference_tables_reject_a_wrong_crc_or_whirlpool() {
    let Some(loaded) = loaded() else {
        return;
    };
    let container = &loaded.reference_containers[&ITEM_INDEX];
    let good = version_entry(container);
    let mut bad_crc = good.clone();
    bad_crc.crc ^= 1;
    let mut bad_whirlpool = good.clone();
    bad_whirlpool.whirlpool[17] ^= 1;
    let mut store = ConfigStore::new();
    assert!(
        store
            .load_reference(ITEM_INDEX, container, &bad_crc)
            .is_err()
    );
    assert!(
        store
            .load_reference(ITEM_INDEX, container, &bad_whirlpool)
            .is_err()
    );
    store.load_reference(ITEM_INDEX, container, &good).unwrap();
    let archive = loaded.reference[&ITEM_INDEX].archives[0].id;
    let cache = Cache::open().unwrap();
    let mut corrupt = trim(&cache.container(u32::from(ITEM_INDEX), archive)).to_vec();
    let last = corrupt.len() - 1;
    corrupt[last] ^= 1;
    assert!(store.load_archive(ITEM_INDEX, archive, &corrupt).is_err());
}
