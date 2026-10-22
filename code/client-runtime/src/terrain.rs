use crate::{assets, transfer::Transfer};
use openrs_assets::{
    Integrity, ReferenceProjectionLimits, ReferenceTable, parse_reference_table_by_names,
    split_archive,
};
use openrs_protocol::js5::{self, Reply, VersionEntry};
use openrs_scene::terrain::{DecodeLimits, PLANES, TILE_COUNT, Terrain};
use serde::Serialize;

const MAPS_INDEX: u8 = 5;
const MAX_REGIONS: usize = 36;
const MAX_REFERENCE_ARCHIVES: usize = 262_144;
const MAX_REFERENCE_FILES: usize = 262_144;
const MAX_REFERENCE_RECORDS: usize = 524_288;
const MAX_RETAINED_FILES: usize = 4096;
const MAX_CONTAINER_BYTES: usize = 4 * 1024 * 1024;
const MAX_TERRAIN_FILE_BYTES: usize = 1024 * 1024;
const MAX_TAGS_PER_TILE: usize = 256;
const MAX_TOTAL_WIRE_BYTES: usize = 32 * 1024 * 1024;
const MAX_TOTAL_CONTAINER_BYTES: usize = 24 * 1024 * 1024;
const MAX_TOTAL_EXPANDED_BYTES: usize = 32 * 1024 * 1024;
const MAX_TOTAL_OPERATIONS: usize = 128 * 1024 * 1024;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldTerrainLimitsReport {
    pub max_regions: usize,
    pub max_reference_archives: usize,
    pub max_reference_files: usize,
    pub max_reference_records: usize,
    pub max_retained_archives: usize,
    pub max_retained_files: usize,
    pub max_container_bytes: usize,
    pub max_expanded_container_bytes: usize,
    pub max_terrain_file_bytes: usize,
    pub max_tags_per_tile: usize,
    pub tile_tag_limit_is_diagnostic: bool,
    pub max_total_wire_bytes: usize,
    pub max_total_container_bytes: usize,
    pub max_total_expanded_bytes: usize,
    pub max_total_operations: usize,
    pub operation_unit: &'static str,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldTerrainRegionReport {
    pub region_id: u16,
    pub origin: [i32; 2],
    pub present: bool,
    pub archive_id: Option<u32>,
    pub archive_revision: Option<i32>,
    pub container_bytes: usize,
    pub expanded_bytes: usize,
    pub terrain_bytes: usize,
    pub suffix_bytes: usize,
    pub tile_count: usize,
    pub operations: usize,
    pub crc_verified: bool,
    pub whirlpool_verified: bool,
    pub effects_decoded: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldTerrainReport {
    pub index: u8,
    pub reference_revision: i32,
    pub reference_archives: usize,
    pub reference_files: usize,
    pub retained_archives: usize,
    pub reference_crc_verified: bool,
    pub reference_whirlpool_verified: bool,
    pub authenticated: bool,
    pub total_wire_bytes: usize,
    pub total_container_bytes: usize,
    pub total_expanded_bytes: usize,
    pub total_operations: usize,
    pub limits: WorldTerrainLimitsReport,
    pub regions: Vec<WorldTerrainRegionReport>,
}

enum Stage {
    VersionTable,
    ReferenceTable(VersionEntry),
    Archive {
        table: ReferenceTable,
        region: usize,
    },
    Complete,
}

pub struct WorldTerrainProbe {
    transfer: Option<Transfer>,
    stage: Stage,
    underwater: bool,
    region_ids: Vec<u16>,
    regions: Vec<WorldTerrainRegionReport>,
    terrains: Vec<(u16, Terrain)>,
    next_region: usize,
    reference_archives: usize,
    reference_files: usize,
    received: usize,
    containers: usize,
    expanded: usize,
    operations: usize,
    failed: bool,
    report: Option<WorldTerrainReport>,
}

impl WorldTerrainProbe {
    pub fn new(token: &[u8], region_ids: &[u16]) -> Result<Self, String> {
        Self::build(token, region_ids, false)
    }

    pub fn new_underwater(token: &[u8], region_ids: &[u16]) -> Result<Self, String> {
        Self::build(token, region_ids, true)
    }

    fn build(token: &[u8], region_ids: &[u16], underwater: bool) -> Result<Self, String> {
        if region_ids.is_empty() || region_ids.len() > MAX_REGIONS {
            return Err("terrain region count limit".into());
        }
        for (at, id) in region_ids.iter().enumerate() {
            if region_ids[..at].contains(id) {
                return Err("duplicate terrain region identity".into());
            }
        }
        Ok(Self {
            transfer: Some(Transfer::new(token, MAX_CONTAINER_BYTES)?),
            stage: Stage::VersionTable,
            underwater,
            region_ids: region_ids.to_vec(),
            regions: Vec::new(),
            terrains: Vec::new(),
            next_region: 0,
            reference_archives: 0,
            reference_files: 0,
            received: 0,
            containers: 0,
            expanded: 0,
            operations: 0,
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

    pub fn report(&self) -> Option<&WorldTerrainReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn terrain(&self, region_id: u16) -> Result<&Terrain, String> {
        if !self.complete() {
            return Err("terrain probe not complete".into());
        }
        self.terrains
            .iter()
            .find(|(id, _)| *id == region_id)
            .map(|(_, terrain)| terrain)
            .ok_or_else(|| "terrain region unavailable".into())
    }

    pub fn height(&self, region_id: u16, plane: usize, x: usize, y: usize) -> Result<i32, String> {
        self.terrain(region_id)?
            .surface_height_native_units(origin(region_id), plane, x, y)
            .ok_or_else(|| "terrain tile coordinates out of bounds".into())
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("terrain probe has failed".into());
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
            "terrain cumulative wire byte limit",
        )?;
        self.charge(bytes.len())?;
        while !bytes.is_empty() {
            if self.complete() {
                return Err("unexpected bytes after terrain probe".into());
            }
            let feed = self
                .transfer
                .as_mut()
                .ok_or_else(|| "terrain transfer unavailable".to_string())?
                .feed(bytes)
                .map_err(|_| "invalid terrain JS5 transfer".to_string())?;
            if feed.consumed == 0 && feed.item.is_none() {
                return Err("terrain decoder made no progress".into());
            }
            bytes = &bytes[feed.consumed..];
            if let Some(reply) = feed.item {
                self.reply(reply)?;
            }
        }
        Ok(())
    }

    fn reply(&mut self, reply: Reply) -> Result<(), String> {
        self.containers = bounded_add(
            self.containers,
            reply.container.len(),
            MAX_TOTAL_CONTAINER_BYTES,
            "terrain cumulative container byte limit",
        )?;
        self.charge(reply.container.len())?;
        match std::mem::replace(&mut self.stage, Stage::Complete) {
            Stage::VersionTable => {
                let data = self.decode(&reply.container, Integrity::default())?;
                drop(data);
                let table = js5::decode_version_table(&reply.container)
                    .map_err(|_| "invalid terrain version table".to_string())?;
                let version = table
                    .entries
                    .into_iter()
                    .nth(usize::from(MAPS_INDEX))
                    .ok_or_else(|| "terrain index unavailable in version table".to_string())?;
                self.stage = Stage::ReferenceTable(version);
                self.request(255, u32::from(MAPS_INDEX))?;
            }
            Stage::ReferenceTable(version) => {
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(version.crc as u32),
                        whirlpool: Some(&version.whirlpool),
                    },
                )?;
                let hashes: Vec<_> = self
                    .region_ids
                    .iter()
                    .copied()
                    .map(|id| surface_name_hash(id, self.underwater))
                    .collect();
                let projection =
                    parse_reference_table_by_names(&data, &hashes, &reference_limits())
                        .map_err(|_| "invalid terrain reference table".to_string())?;
                self.charge(projection.scan_operations)?;
                self.reference_archives = projection.archive_count;
                self.reference_files = projection.total_files;
                self.plan(&projection.table)?;
                self.next(projection.table)?;
            }
            Stage::Archive { table, region } => {
                let archive = self.regions[region]
                    .archive_id
                    .and_then(|id| table.archive(id))
                    .ok_or_else(|| "terrain archive metadata unavailable".to_string())?;
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(archive.crc32),
                        whirlpool: archive.whirlpool.as_ref(),
                    },
                )?;
                self.charge(data.len())?;
                let files = split_archive(&data, &archive.files, &assets::limits())
                    .map_err(|_| "invalid terrain archive".to_string())?;
                let file = files
                    .into_iter()
                    .find(|file| file.id == 0)
                    .ok_or_else(|| "terrain archive file zero unavailable".to_string())?;
                self.charge(file.data.len() + TILE_COUNT)?;
                let planes = if self.underwater { 1 } else { PLANES };
                let (terrain, suffix) = Terrain::decode_planes(
                    &file.data,
                    planes,
                    DecodeLimits {
                        max_input_bytes: MAX_TERRAIN_FILE_BYTES,
                        max_tags_per_tile: MAX_TAGS_PER_TILE,
                    },
                )
                .map_err(|_| {
                    "invalid terrain tile stream or diagnostic bound exceeded".to_string()
                })?;
                let report = &mut self.regions[region];
                report.container_bytes = reply.container.len();
                report.expanded_bytes = data.len();
                report.terrain_bytes = file.data.len() - suffix.len();
                report.suffix_bytes = suffix.len();
                report.tile_count = TILE_COUNT;
                report.operations = file.data.len() + TILE_COUNT;
                report.crc_verified = true;
                report.whirlpool_verified = archive.whirlpool.is_some();
                self.terrains.push((report.region_id, terrain));
                self.next(table)?;
            }
            Stage::Complete => return Err("unexpected terrain reply".into()),
        }
        Ok(())
    }

    fn decode(&mut self, container: &[u8], integrity: Integrity<'_>) -> Result<Vec<u8>, String> {
        let start = if container.first() == Some(&0) { 1 } else { 5 };
        let length = container
            .get(start..start + 4)
            .ok_or_else(|| "invalid terrain container header".to_string())?;
        let length = u32::from_be_bytes(length.try_into().unwrap()) as usize;
        let expanded = bounded_add(
            self.expanded,
            length,
            MAX_TOTAL_EXPANDED_BYTES,
            "terrain cumulative expanded byte limit",
        )?;
        self.charge(length)?;
        let data = assets::decode_with_budget(
            container,
            integrity,
            MAX_TOTAL_EXPANDED_BYTES - self.expanded,
        )
        .map_err(|_| "invalid terrain container or integrity check".to_string())?;
        self.expanded = expanded;
        Ok(data)
    }

    fn plan(&mut self, table: &ReferenceTable) -> Result<(), String> {
        if table.flags & 1 == 0 {
            return Err("terrain named reference table unavailable".into());
        }
        self.charge(self.region_ids.len() * table.archives.len())?;
        for &region_id in &self.region_ids {
            let hash = surface_name_hash(region_id, self.underwater);
            let archive = table
                .archives
                .iter()
                .find(|archive| archive.name_hash == Some(hash));
            let archive_id = archive.map(|archive| archive.id);
            if archive.is_some_and(|archive| archive.file(0).is_none()) {
                return Err("terrain archive file zero unavailable".into());
            }
            self.regions.push(WorldTerrainRegionReport {
                region_id,
                origin: origin(region_id),
                present: archive.is_some(),
                archive_id,
                archive_revision: archive.map(|archive| archive.revision),
                container_bytes: 0,
                expanded_bytes: 0,
                terrain_bytes: 0,
                suffix_bytes: 0,
                tile_count: 0,
                operations: 0,
                crc_verified: false,
                whirlpool_verified: false,
                effects_decoded: false,
            });
        }
        Ok(())
    }

    fn next(&mut self, table: ReferenceTable) -> Result<(), String> {
        while let Some(region) = self.regions.get(self.next_region) {
            let at = self.next_region;
            self.next_region += 1;
            if let Some(id) = region.archive_id {
                self.stage = Stage::Archive { table, region: at };
                return self.request(MAPS_INDEX, id);
            }
        }
        self.region_ids.clear();
        self.report = Some(WorldTerrainReport {
            index: MAPS_INDEX,
            reference_revision: table.revision,
            reference_archives: self.reference_archives,
            reference_files: self.reference_files,
            retained_archives: table.archives.len(),
            reference_crc_verified: true,
            reference_whirlpool_verified: true,
            authenticated: false,
            total_wire_bytes: self.received,
            total_container_bytes: self.containers,
            total_expanded_bytes: self.expanded,
            total_operations: self.operations,
            limits: WorldTerrainLimitsReport {
                max_regions: MAX_REGIONS,
                max_reference_archives: MAX_REFERENCE_ARCHIVES,
                max_reference_files: MAX_REFERENCE_FILES,
                max_reference_records: MAX_REFERENCE_RECORDS,
                max_retained_archives: MAX_REGIONS,
                max_retained_files: MAX_RETAINED_FILES,
                max_container_bytes: MAX_CONTAINER_BYTES,
                max_expanded_container_bytes: assets::limits().max_uncompressed_bytes,
                max_terrain_file_bytes: MAX_TERRAIN_FILE_BYTES,
                max_tags_per_tile: MAX_TAGS_PER_TILE,
                tile_tag_limit_is_diagnostic: true,
                max_total_wire_bytes: MAX_TOTAL_WIRE_BYTES,
                max_total_container_bytes: MAX_TOTAL_CONTAINER_BYTES,
                max_total_expanded_bytes: MAX_TOTAL_EXPANDED_BYTES,
                max_total_operations: MAX_TOTAL_OPERATIONS,
                operation_unit: "wire/container/expanded/split/tile bytes and bounded metadata records",
            },
            regions: std::mem::take(&mut self.regions),
        });
        Ok(())
    }

    fn request(&mut self, index: u8, archive: u32) -> Result<(), String> {
        self.transfer
            .as_mut()
            .ok_or_else(|| "terrain transfer unavailable".to_string())?
            .request(index, archive)
            .map_err(|_| "invalid terrain request sequence".to_string())
    }

    fn charge(&mut self, count: usize) -> Result<(), String> {
        self.operations = bounded_add(
            self.operations,
            count,
            MAX_TOTAL_OPERATIONS,
            "terrain cumulative operation limit",
        )?;
        Ok(())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.report = None;
        self.transfer = None;
        self.stage = Stage::Complete;
        self.region_ids = Vec::new();
        self.regions = Vec::new();
        self.terrains = Vec::new();
        self.next_region = 0;
        self.reference_archives = 0;
        self.reference_files = 0;
        self.received = 0;
        self.containers = 0;
        self.expanded = 0;
        self.operations = 0;
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.fail();
        Err("connection ended before complete terrain probe".into())
    }
}

fn bounded_add(current: usize, count: usize, limit: usize, error: &str) -> Result<usize, String> {
    current
        .checked_add(count)
        .filter(|count| *count <= limit)
        .ok_or_else(|| error.to_string())
}

fn reference_limits() -> ReferenceProjectionLimits {
    let mut table = assets::limits();
    table.max_archives = MAX_REFERENCE_ARCHIVES;
    table.max_total_files = MAX_REFERENCE_FILES;
    ReferenceProjectionLimits {
        table,
        max_selected_archives: MAX_REGIONS,
        max_selected_files: MAX_RETAINED_FILES,
        max_total_records: MAX_REFERENCE_RECORDS,
    }
}

fn origin(region_id: u16) -> [i32; 2] {
    [
        i32::from(region_id >> 8) * 64,
        i32::from(region_id & 255) * 64,
    ]
}

fn surface_name_hash(region_id: u16, underwater: bool) -> i32 {
    let prefix = if underwater { "um" } else { "m" };
    format!("{prefix}{}_{}", region_id >> 8, region_id & 255)
        .bytes()
        .fold(0i32, |hash, byte| {
            hash.wrapping_mul(31).wrapping_add(i32::from(byte))
        })
}

#[cfg(test)]
mod tests;
