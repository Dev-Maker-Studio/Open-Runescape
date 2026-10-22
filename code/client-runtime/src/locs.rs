use crate::{assets, transfer::Transfer};
use openrs_assets::{
    Archive, Integrity, ReferenceProjectionLimits, ReferenceTable, decrypt_container, is_zero_key,
    parse_reference_table, parse_reference_table_by_names, split_archive, trim_revision,
};
use openrs_protocol::js5::{self, Reply, VersionEntry};
use openrs_scene::loc_type::decode_loc_type;
use openrs_scene::locs::decode_region_locs;
use serde::Serialize;
use std::collections::{BTreeMap, BTreeSet, VecDeque};
use zeroize::Zeroizing;

const MAPS_INDEX: u8 = 5;
const MODELS_INDEX: u8 = 7;
const LOC_TYPES_INDEX: u8 = 16;
const MAX_REGIONS: usize = 64;
const MAX_TYPES: usize = 32_768;
const MAX_MODELS: usize = 16_384;
const MAX_CONTAINER_BYTES: usize = 4 * 1024 * 1024;
const MAX_TOTAL_WIRE_BYTES: usize = 192 * 1024 * 1024;
const MAX_TOTAL_EXPANDED_BYTES: usize = 192 * 1024 * 1024;
const MAX_REFERENCE_ARCHIVES: usize = 262_144;
const MAX_REFERENCE_FILES: usize = 262_144;
const MAX_REFERENCE_RECORDS: usize = 524_288;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldLocRegionReport {
    pub region_id: u16,
    pub present: bool,
    pub keyed: bool,
    pub decrypted: bool,
    pub container_bytes: usize,
    pub expanded_bytes: usize,
    pub placements: usize,
    pub type_ids: usize,
    pub crc_verified: bool,
    pub whirlpool_verified: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldLocReport {
    pub authenticated: bool,
    pub regions: Vec<WorldLocRegionReport>,
    pub placements: usize,
    pub type_ids: usize,
    pub types_found: usize,
    pub types_decoded: usize,
    pub types_undecodable: usize,
    pub model_ids: usize,
    pub models_found: usize,
    pub total_wire_bytes: usize,
    pub total_expanded_bytes: usize,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RegionLocs {
    pub data: Vec<u8>,
    pub type_ids: Vec<i32>,
}

enum Stage {
    VersionTable,
    Reference(u8, VersionEntry),
    Archive {
        index: u8,
        table: ReferenceTable,
        current: u32,
    },
    Complete,
}

pub struct WorldLocProbe {
    transfer: Option<Transfer>,
    stage: Stage,
    versions: Vec<VersionEntry>,
    region_ids: Vec<u16>,
    keys: Zeroizing<Vec<[i32; 4]>>,
    regions: Vec<WorldLocRegionReport>,
    region_archives: Vec<Option<u32>>,
    region_data: BTreeMap<u16, RegionLocs>,
    queue: VecDeque<u32>,
    wanted_types: BTreeSet<i32>,
    types: BTreeMap<i32, Vec<u8>>,
    wanted_models: BTreeSet<i32>,
    models: BTreeMap<i32, Vec<u8>>,
    types_decoded: usize,
    types_undecodable: usize,
    received: usize,
    expanded: usize,
    failed: bool,
    report: Option<WorldLocReport>,
}

impl WorldLocProbe {
    pub fn new(token: &[u8], region_ids: &[u16], keys: Vec<[i32; 4]>) -> Result<Self, String> {
        if region_ids.is_empty() || region_ids.len() > MAX_REGIONS {
            return Err("loc region count limit".into());
        }
        if keys.len() != region_ids.len() {
            return Err("loc region key count mismatch".into());
        }
        for (at, id) in region_ids.iter().enumerate() {
            if region_ids[..at].contains(id) {
                return Err("duplicate loc region identity".into());
            }
        }
        Ok(Self {
            transfer: Some(Transfer::new(token, MAX_CONTAINER_BYTES)?),
            stage: Stage::VersionTable,
            versions: Vec::new(),
            region_ids: region_ids.to_vec(),
            keys: Zeroizing::new(keys),
            regions: Vec::new(),
            region_archives: Vec::new(),
            region_data: BTreeMap::new(),
            queue: VecDeque::new(),
            wanted_types: BTreeSet::new(),
            types: BTreeMap::new(),
            wanted_models: BTreeSet::new(),
            models: BTreeMap::new(),
            types_decoded: 0,
            types_undecodable: 0,
            received: 0,
            expanded: 0,
            failed: false,
            report: None,
        })
    }

    pub fn take_outgoing(&mut self) -> Vec<u8> {
        self.transfer
            .as_mut()
            .map_or_else(Vec::new, Transfer::outgoing)
    }

    pub fn complete(&self) -> bool {
        !self.failed && self.report.is_some()
    }

    pub fn report(&self) -> Option<&WorldLocReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn region(&self, region_id: u16) -> Option<&RegionLocs> {
        if !self.complete() {
            return None;
        }
        self.region_data.get(&region_id)
    }

    pub fn loc_type(&self, id: i32) -> Option<&[u8]> {
        if !self.complete() {
            return None;
        }
        self.types.get(&id).map(Vec::as_slice)
    }

    pub fn model(&self, id: i32) -> Option<&[u8]> {
        if !self.complete() {
            return None;
        }
        self.models.get(&id).map(Vec::as_slice)
    }

    pub fn model_ids(&self, type_id: i32) -> Vec<i32> {
        self.loc_type(type_id)
            .map_or_else(Vec::new, |bytes| type_model_ids(type_id, bytes))
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("loc probe has failed".into());
        }
        let result = self.feed_inner(bytes);
        if result.is_err() {
            self.fail();
        }
        result
    }

    fn feed_inner(&mut self, mut bytes: &[u8]) -> Result<(), String> {
        self.received = bounded_add(
            self.received,
            bytes.len(),
            MAX_TOTAL_WIRE_BYTES,
            "loc cumulative wire byte limit",
        )?;
        while !bytes.is_empty() {
            if self.complete() {
                return Err("unexpected bytes after loc probe".into());
            }
            let feed = self
                .transfer
                .as_mut()
                .ok_or_else(|| "loc transfer unavailable".to_string())?
                .feed(bytes)
                .map_err(|_| "invalid loc JS5 transfer".to_string())?;
            if feed.consumed == 0 && feed.item.is_none() {
                return Err("loc decoder made no progress".into());
            }
            bytes = &bytes[feed.consumed..];
            if let Some(reply) = feed.item {
                self.reply(reply)?;
            }
        }
        Ok(())
    }

    fn reply(&mut self, reply: Reply) -> Result<(), String> {
        match std::mem::replace(&mut self.stage, Stage::Complete) {
            Stage::VersionTable => {
                self.decode(&reply.container, Integrity::default())?;
                let table = js5::decode_version_table(&reply.container)
                    .map_err(|_| "invalid loc version table".to_string())?;
                self.versions = table.entries;
                self.begin_phase(MAPS_INDEX)
            }
            Stage::Reference(index, version) => {
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(version.crc as u32),
                        whirlpool: Some(&version.whirlpool),
                    },
                )?;
                let table = if index == MAPS_INDEX {
                    let hashes: Vec<i32> = self
                        .region_ids
                        .iter()
                        .map(|id| loc_name_hash(*id))
                        .collect();
                    parse_reference_table_by_names(&data, &hashes, &projection_limits())
                        .map_err(|_| "invalid loc reference table".to_string())?
                        .table
                } else {
                    parse_reference_table(&data, &table_limits())
                        .map_err(|_| "invalid loc reference table".to_string())?
                };
                self.plan(index, &table)?;
                self.next_archive(index, table)
            }
            Stage::Archive {
                index,
                table,
                current,
            } => {
                let archive = table
                    .archive(current)
                    .ok_or_else(|| "loc archive metadata unavailable".to_string())?
                    .clone();
                self.archive(index, &archive, &reply)?;
                self.next_archive(index, table)
            }
            Stage::Complete => Err("unexpected loc reply".into()),
        }
    }

    fn begin_phase(&mut self, index: u8) -> Result<(), String> {
        let version = self
            .versions
            .get(usize::from(index))
            .cloned()
            .ok_or_else(|| "loc index unavailable in version table".to_string())?;
        self.stage = Stage::Reference(index, version);
        self.request(255, u32::from(index))
    }

    fn plan(&mut self, index: u8, table: &ReferenceTable) -> Result<(), String> {
        self.queue.clear();
        match index {
            MAPS_INDEX => {
                if table.flags & 1 == 0 {
                    return Err("loc named reference table unavailable".into());
                }
                for at in 0..self.region_ids.len() {
                    let region_id = self.region_ids[at];
                    let keyed = !is_zero_key(&self.keys[at]);
                    let hash = loc_name_hash(region_id);
                    let archive = table
                        .archives
                        .iter()
                        .find(|archive| archive.name_hash == Some(hash));
                    if archive.is_some_and(|archive| archive.file(0).is_none()) {
                        return Err("loc archive file zero unavailable".into());
                    }
                    self.region_archives.push(archive.map(|archive| archive.id));
                    self.regions.push(WorldLocRegionReport {
                        region_id,
                        present: archive.is_some(),
                        keyed,
                        decrypted: false,
                        container_bytes: 0,
                        expanded_bytes: 0,
                        placements: 0,
                        type_ids: 0,
                        crc_verified: false,
                        whirlpool_verified: false,
                    });
                }
                self.queue
                    .extend(self.region_archives.iter().flatten().copied());
            }
            LOC_TYPES_INDEX => {
                let archives: BTreeSet<u32> = self
                    .wanted_types
                    .iter()
                    .map(|id| (*id as u32) >> 8)
                    .filter(|id| table.archive(*id).is_some())
                    .collect();
                self.queue.extend(archives);
            }
            _ => {
                let ids: Vec<u32> = self
                    .wanted_models
                    .iter()
                    .filter(|id| **id >= 0 && table.archive(**id as u32).is_some())
                    .map(|id| *id as u32)
                    .collect();
                self.queue.extend(ids);
            }
        }
        Ok(())
    }

    fn next_archive(&mut self, index: u8, table: ReferenceTable) -> Result<(), String> {
        if let Some(current) = self.queue.pop_front() {
            self.stage = Stage::Archive {
                index,
                table,
                current,
            };
            return self.request(index, current);
        }
        match index {
            MAPS_INDEX => {
                if self.wanted_types.is_empty() {
                    return self.finish_report();
                }
                self.begin_phase(LOC_TYPES_INDEX)
            }
            LOC_TYPES_INDEX => {
                self.collect_models();
                if self.wanted_models.is_empty() {
                    return self.finish_report();
                }
                self.begin_phase(MODELS_INDEX)
            }
            _ => self.finish_report(),
        }
    }

    fn archive(&mut self, index: u8, archive: &Archive, reply: &Reply) -> Result<(), String> {
        match index {
            MAPS_INDEX => self.region_archive(archive, reply),
            LOC_TYPES_INDEX => {
                let (data, _) = self.open(&reply.container, archive, None)?;
                let files = split_archive(&data, &archive.files, &assets::limits())
                    .map_err(|_| "invalid loc type archive".to_string())?;
                for file in files {
                    let id = ((archive.id << 8) | file.id) as i32;
                    if self.wanted_types.contains(&id) {
                        self.types.insert(id, file.data);
                    }
                }
                Ok(())
            }
            _ => {
                let (data, _) = self.open(&reply.container, archive, None)?;
                let files = split_archive(&data, &archive.files, &assets::limits())
                    .map_err(|_| "invalid loc model archive".to_string())?;
                if let Some(file) = files.into_iter().next() {
                    self.models.insert(archive.id as i32, file.data);
                }
                Ok(())
            }
        }
    }

    fn region_archive(&mut self, archive: &Archive, reply: &Reply) -> Result<(), String> {
        let at = self
            .region_archives
            .iter()
            .position(|id| *id == Some(archive.id))
            .ok_or_else(|| "unexpected loc region archive".to_string())?;
        let key = self.keys[at];
        let (data, outcome) = self.open(&reply.container, archive, Some(&key))?;
        let files = split_archive(&data, &archive.files, &assets::limits())
            .map_err(|_| "invalid loc region archive".to_string())?;
        let file = files
            .into_iter()
            .find(|file| file.id == 0)
            .ok_or_else(|| "loc archive file zero unavailable".to_string())?;
        let placements = decode_region_locs(&file.data)
            .map_err(|_| "invalid region loc placements".to_string())?;
        let type_ids: Vec<i32> = placements
            .iter()
            .map(|placement| placement.id)
            .collect::<BTreeSet<_>>()
            .into_iter()
            .collect();
        if self.wanted_types.len() + type_ids.len() > MAX_TYPES {
            return Err("loc type limit".into());
        }
        self.wanted_types.extend(type_ids.iter().copied());
        let report = &mut self.regions[at];
        report.container_bytes = reply.container.len();
        report.expanded_bytes = data.len();
        report.placements = placements.len();
        report.type_ids = type_ids.len();
        report.decrypted = outcome.decrypted;
        report.crc_verified = outcome.crc;
        report.whirlpool_verified = outcome.whirlpool;
        let region_id = report.region_id;
        self.region_data.insert(
            region_id,
            RegionLocs {
                data: file.data,
                type_ids,
            },
        );
        Ok(())
    }

    fn open(
        &mut self,
        container: &[u8],
        archive: &Archive,
        key: Option<&[i32; 4]>,
    ) -> Result<(Vec<u8>, Opened), String> {
        let trimmed =
            trim_revision(container).ok_or_else(|| "invalid loc container header".to_string())?;
        let integrity = Integrity {
            crc32: Some(archive.crc32),
            whirlpool: archive.whirlpool.as_ref(),
        };
        let keyed = key.filter(|key| !is_zero_key(key));
        let (data, outcome) = match keyed {
            None => (
                self.decode(trimmed, integrity)?,
                Opened {
                    decrypted: false,
                    crc: true,
                    whirlpool: archive.whirlpool.is_some(),
                },
            ),
            Some(key) => {
                let plain = decrypt_container(trimmed, key);
                let crc = crc32fast::hash(trimmed) == archive.crc32
                    || crc32fast::hash(&plain) == archive.crc32;
                if !crc {
                    return Err("loc encrypted container integrity check".into());
                }
                match self.decode(&plain, Integrity::default()) {
                    Ok(data) => (
                        data,
                        Opened {
                            decrypted: true,
                            crc: true,
                            whirlpool: false,
                        },
                    ),
                    Err(_) => (
                        self.decode(trimmed, integrity)?,
                        Opened {
                            decrypted: false,
                            crc: true,
                            whirlpool: archive.whirlpool.is_some(),
                        },
                    ),
                }
            }
        };
        Ok((data, outcome))
    }

    fn decode(&mut self, container: &[u8], integrity: Integrity<'_>) -> Result<Vec<u8>, String> {
        let data = assets::decode_with_budget(
            container,
            integrity,
            MAX_TOTAL_EXPANDED_BYTES.saturating_sub(self.expanded),
        )
        .map_err(|_| "invalid loc container or integrity check".to_string())?;
        self.expanded = bounded_add(
            self.expanded,
            data.len(),
            MAX_TOTAL_EXPANDED_BYTES,
            "loc cumulative expanded byte limit",
        )?;
        Ok(data)
    }

    fn collect_models(&mut self) {
        let mut ids = BTreeSet::new();
        for (id, bytes) in &self.types {
            match decode_loc_type(*id, Some(bytes), true) {
                Ok(loc) => {
                    self.types_decoded += 1;
                    for group in loc.model_ids.iter().flatten() {
                        ids.extend(group.iter().copied().filter(|model| *model >= 0));
                    }
                }
                Err(_) => self.types_undecodable += 1,
            }
        }
        if ids.len() > MAX_MODELS {
            ids = ids.into_iter().take(MAX_MODELS).collect();
        }
        self.wanted_models = ids;
    }

    fn finish_report(&mut self) -> Result<(), String> {
        let placements = self.regions.iter().map(|region| region.placements).sum();
        self.report = Some(WorldLocReport {
            authenticated: false,
            regions: std::mem::take(&mut self.regions),
            placements,
            type_ids: self.wanted_types.len(),
            types_found: self.types.len(),
            types_decoded: self.types_decoded,
            types_undecodable: self.types_undecodable,
            model_ids: self.wanted_models.len(),
            models_found: self.models.len(),
            total_wire_bytes: self.received,
            total_expanded_bytes: self.expanded,
        });
        self.keys = Zeroizing::new(Vec::new());
        self.stage = Stage::Complete;
        Ok(())
    }

    fn request(&mut self, index: u8, archive: u32) -> Result<(), String> {
        self.transfer
            .as_mut()
            .ok_or_else(|| "loc transfer unavailable".to_string())?
            .request(index, archive)
            .map_err(|_| "invalid loc request sequence".to_string())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.report = None;
        self.transfer = None;
        self.stage = Stage::Complete;
        self.keys = Zeroizing::new(Vec::new());
        self.versions = Vec::new();
        self.region_ids = Vec::new();
        self.regions = Vec::new();
        self.region_archives = Vec::new();
        self.region_data = BTreeMap::new();
        self.queue = VecDeque::new();
        self.wanted_types = BTreeSet::new();
        self.types = BTreeMap::new();
        self.wanted_models = BTreeSet::new();
        self.models = BTreeMap::new();
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.fail();
        Err("connection ended before complete loc probe".into())
    }
}

struct Opened {
    decrypted: bool,
    crc: bool,
    whirlpool: bool,
}

pub fn type_model_ids(type_id: i32, bytes: &[u8]) -> Vec<i32> {
    let Ok(loc) = decode_loc_type(type_id, Some(bytes), true) else {
        return Vec::new();
    };
    loc.model_ids
        .iter()
        .flatten()
        .flatten()
        .copied()
        .filter(|model| *model >= 0)
        .collect::<BTreeSet<_>>()
        .into_iter()
        .collect()
}

fn bounded_add(current: usize, count: usize, limit: usize, error: &str) -> Result<usize, String> {
    current
        .checked_add(count)
        .filter(|count| *count <= limit)
        .ok_or_else(|| error.to_string())
}

fn table_limits() -> openrs_assets::Limits {
    let mut limits = assets::limits();
    limits.max_archives = MAX_REFERENCE_ARCHIVES;
    limits.max_total_files = MAX_REFERENCE_FILES;
    limits
}

fn projection_limits() -> ReferenceProjectionLimits {
    ReferenceProjectionLimits {
        table: table_limits(),
        max_selected_archives: MAX_REGIONS,
        max_selected_files: 4096,
        max_total_records: MAX_REFERENCE_RECORDS,
    }
}

fn loc_name_hash(region_id: u16) -> i32 {
    format!("l{}_{}", region_id >> 8, region_id & 255)
        .bytes()
        .fold(0i32, |hash, byte| {
            hash.wrapping_mul(31).wrapping_add(i32::from(byte))
        })
}

#[cfg(target_arch = "wasm32")]
mod browser {
    use super::WorldLocProbe;
    use crate::world_session::browser::BrowserWorldSession;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = Js5WorldLocProbe)]
    pub struct BrowserWorldLocProbe(WorldLocProbe);

    #[wasm_bindgen(js_class = Js5WorldLocProbe)]
    impl BrowserWorldLocProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(
            token: &str,
            session: &BrowserWorldSession,
        ) -> Result<BrowserWorldLocProbe, JsValue> {
            let (regions, keys) = session
                .0
                .loc_regions()
                .ok_or_else(|| JsValue::from_str("world map unavailable"))?;
            let keys = keys.to_vec();
            WorldLocProbe::new(token.as_bytes(), &regions, keys)
                .map(Self)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }

        pub fn complete(&self) -> bool {
            self.0.complete()
        }

        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }

        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("loc probe not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("loc report serialization failed"))
        }

        #[wasm_bindgen(js_name = regionLocs)]
        pub fn region_locs(&self, region_id: u32) -> Option<Vec<u8>> {
            let id = u16::try_from(region_id).ok()?;
            self.0.region(id).map(|region| region.data.clone())
        }

        #[wasm_bindgen(js_name = regionTypeIds)]
        pub fn region_type_ids(&self, region_id: u32) -> Option<Vec<i32>> {
            let id = u16::try_from(region_id).ok()?;
            self.0.region(id).map(|region| region.type_ids.clone())
        }

        #[wasm_bindgen(js_name = locType)]
        pub fn loc_type(&self, id: i32) -> Option<Vec<u8>> {
            self.0.loc_type(id).map(Vec::from)
        }

        #[wasm_bindgen(js_name = locModel)]
        pub fn loc_model(&self, id: i32) -> Option<Vec<u8>> {
            self.0.model(id).map(Vec::from)
        }

        #[wasm_bindgen(js_name = modelIds)]
        pub fn model_ids(&self, type_id: i32) -> Vec<i32> {
            self.0.model_ids(type_id)
        }
    }
}

#[cfg(test)]
mod tests;
