use crate::{assets, transfer::Transfer};
use openrs_assets::{Integrity, ReferenceTable, parse_reference_table, split_archive};
use openrs_protocol::js5::{self, Reply, VersionEntry};
use openrs_scene::floor::{MAX_DEFINITION_BYTES, MAX_TAGS_PER_DEFINITION, Overlay, Underlay};
use serde::Serialize;

const CONFIG_INDEX: u8 = 2;
const UNDERLAY_ARCHIVE: u32 = 1;
const OVERLAY_ARCHIVE: u32 = 4;
const MAX_FLOOR_ID: u32 = u16::MAX as u32;
const MAX_DEFINITIONS_PER_ARCHIVE: usize = 65_536;
const MAX_TOTAL_DEFINITIONS: usize = 131_072;
const MAX_CONTAINER_BYTES: usize = 4 * 1024 * 1024;
const MAX_TOTAL_WIRE_BYTES: usize = 20 * 1024 * 1024;
const MAX_TOTAL_CONTAINER_BYTES: usize = 16 * 1024 * 1024;
const MAX_TOTAL_EXPANDED_BYTES: usize = 24 * 1024 * 1024;
const MAX_TOTAL_OPERATIONS: usize = 128 * 1024 * 1024;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldFloorLimitsReport {
    pub max_reference_archives: usize,
    pub max_reference_files: usize,
    pub max_definitions_per_archive: usize,
    pub max_total_definitions: usize,
    pub max_floor_id: u32,
    pub max_definition_bytes: usize,
    pub max_tags_per_definition: usize,
    pub max_container_bytes: usize,
    pub max_expanded_container_bytes: usize,
    pub max_total_wire_bytes: usize,
    pub max_total_container_bytes: usize,
    pub max_total_expanded_bytes: usize,
    pub max_total_operations: usize,
    pub operation_unit: &'static str,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FloorArchiveReport {
    pub archive: u32,
    pub archive_revision: i32,
    pub container_bytes: usize,
    pub expanded_bytes: usize,
    pub file_slots: u32,
    pub populated_definitions: usize,
    pub definition_bytes: usize,
    pub crc_verified: bool,
    pub whirlpool_verified: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldFloorReport {
    pub revision: u32,
    pub subrevision: u32,
    pub prefetch_count: usize,
    pub index: u8,
    pub reference_revision: i32,
    pub reference_archives: usize,
    pub reference_files: usize,
    pub reference_crc_verified: bool,
    pub reference_whirlpool_verified: bool,
    pub authenticated: bool,
    pub rendered_world: bool,
    pub opcode8_side_effects_applied: bool,
    pub total_wire_bytes: usize,
    pub total_container_bytes: usize,
    pub total_expanded_bytes: usize,
    pub total_operations: usize,
    pub underlays: FloorArchiveReport,
    pub overlays: FloorArchiveReport,
    pub limits: WorldFloorLimitsReport,
}

#[derive(Debug)]
pub enum FloorLookup<'a, T> {
    Present(&'a T),
    Default(T),
}

impl<T> FloorLookup<'_, T> {
    pub fn present(&self) -> bool {
        matches!(self, Self::Present(_))
    }

    pub fn definition(&self) -> &T {
        match self {
            Self::Present(definition) => definition,
            Self::Default(definition) => definition,
        }
    }
}

#[derive(Debug)]
pub struct FloorRepository {
    underlays: Vec<(u32, Underlay)>,
    overlays: Vec<(u32, Overlay)>,
    underlay_file_slots: u32,
    overlay_file_slots: u32,
}

impl FloorRepository {
    pub fn underlay(&self, id: u32) -> Result<FloorLookup<'_, Underlay>, String> {
        validate_id(id)?;
        Ok(
            match self.underlays.binary_search_by_key(&id, |entry| entry.0) {
                Ok(at) => FloorLookup::Present(&self.underlays[at].1),
                Err(_) => FloorLookup::Default(Underlay::default()),
            },
        )
    }

    pub fn overlay(&self, id: u32) -> Result<FloorLookup<'_, Overlay>, String> {
        validate_id(id)?;
        Ok(
            match self.overlays.binary_search_by_key(&id, |entry| entry.0) {
                Ok(at) => FloorLookup::Present(&self.overlays[at].1),
                Err(_) => FloorLookup::Default(Overlay::default_for_id(id)),
            },
        )
    }

    pub fn underlays(&self) -> &[(u32, Underlay)] {
        &self.underlays
    }

    pub fn overlays(&self) -> &[(u32, Overlay)] {
        &self.overlays
    }

    pub fn underlay_file_slots(&self) -> u32 {
        self.underlay_file_slots
    }

    pub fn overlay_file_slots(&self) -> u32 {
        self.overlay_file_slots
    }
}

enum Stage {
    VersionTable,
    ReferenceTable(VersionEntry),
    Archive { table: ReferenceTable, id: u32 },
    Complete,
}

pub struct WorldFloorProbe {
    transfer: Option<Transfer>,
    stage: Stage,
    underlays: Vec<(u32, Underlay)>,
    underlay_report: Option<FloorArchiveReport>,
    repository: Option<FloorRepository>,
    reference_files: usize,
    received: usize,
    containers: usize,
    expanded: usize,
    operations: usize,
    failed: bool,
    report: Option<WorldFloorReport>,
}

impl WorldFloorProbe {
    pub fn new(token: &[u8]) -> Result<Self, String> {
        Ok(Self {
            transfer: Some(Transfer::new(token, MAX_CONTAINER_BYTES)?),
            stage: Stage::VersionTable,
            underlays: Vec::new(),
            underlay_report: None,
            repository: None,
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

    pub fn report(&self) -> Option<&WorldFloorReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn repository(&self) -> Result<&FloorRepository, String> {
        if !self.complete() {
            return Err("floor probe not complete".into());
        }
        self.repository
            .as_ref()
            .ok_or_else(|| "floor repository unavailable".into())
    }

    pub fn underlay(&self, id: u32) -> Result<FloorLookup<'_, Underlay>, String> {
        self.repository()?.underlay(id)
    }

    pub fn overlay(&self, id: u32) -> Result<FloorLookup<'_, Overlay>, String> {
        self.repository()?.overlay(id)
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("floor probe has failed".into());
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
            "floor cumulative wire byte limit",
        )?;
        self.charge(bytes.len())?;
        while !bytes.is_empty() {
            if self.complete() {
                return Err("unexpected bytes after floor probe".into());
            }
            let feed = self
                .transfer
                .as_mut()
                .ok_or_else(|| "floor transfer unavailable".to_string())?
                .feed(bytes)
                .map_err(|_| "invalid floor JS5 transfer".to_string())?;
            if feed.consumed == 0 && feed.item.is_none() {
                return Err("floor decoder made no progress".into());
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
            "floor cumulative container byte limit",
        )?;
        self.charge(reply.container.len())?;
        match std::mem::replace(&mut self.stage, Stage::Complete) {
            Stage::VersionTable => {
                let data = self.decode(&reply.container, Integrity::default())?;
                drop(data);
                let version = js5::decode_version_table(&reply.container)
                    .map_err(|_| "invalid floor version table".to_string())?
                    .entries
                    .into_iter()
                    .nth(usize::from(CONFIG_INDEX))
                    .ok_or_else(|| "floor index unavailable in version table".to_string())?;
                self.stage = Stage::ReferenceTable(version);
                self.request(255, u32::from(CONFIG_INDEX))?;
            }
            Stage::ReferenceTable(version) => {
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(u32::from_be_bytes(version.crc.to_be_bytes())),
                        whirlpool: Some(&version.whirlpool),
                    },
                )?;
                self.charge(data.len())?;
                let table = parse_reference_table(&data, &assets::limits())
                    .map_err(|_| "invalid floor reference table".to_string())?;
                if table.revision != version.revision {
                    return Err("floor reference revision mismatch".into());
                }
                let files = table.archives.iter().try_fold(0usize, |count, archive| {
                    bounded_add(
                        count,
                        archive.files.len(),
                        assets::limits().max_total_files,
                        "floor reference file count limit",
                    )
                })?;
                self.reference_files = files;
                let records = table
                    .archives
                    .len()
                    .checked_mul(6)
                    .and_then(|count| {
                        files
                            .checked_mul(if table.flags & 1 != 0 { 2 } else { 1 })
                            .and_then(|files| count.checked_add(files))
                    })
                    .ok_or_else(|| "floor reference operation overflow".to_string())?;
                self.charge(records)?;
                for id in [UNDERLAY_ARCHIVE, OVERLAY_ARCHIVE] {
                    let archive = table.archive(id).ok_or_else(|| {
                        "floor archive unavailable in reference table".to_string()
                    })?;
                    file_slots(archive)?;
                }
                self.stage = Stage::Archive {
                    table,
                    id: UNDERLAY_ARCHIVE,
                };
                self.request(CONFIG_INDEX, UNDERLAY_ARCHIVE)?;
            }
            Stage::Archive { table, id } => {
                let archive = table
                    .archive(id)
                    .ok_or_else(|| "floor archive metadata unavailable".to_string())?;
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(archive.crc32),
                        whirlpool: archive.whirlpool.as_ref(),
                    },
                )?;
                self.charge(data.len())?;
                let files = split_archive(&data, &archive.files, &assets::limits())
                    .map_err(|_| "invalid floor archive".to_string())?;
                let definition_bytes = files.iter().try_fold(0usize, |count, file| {
                    bounded_add(
                        count,
                        file.data.len(),
                        assets::limits().max_uncompressed_bytes,
                        "floor definition byte limit",
                    )
                })?;
                self.charge(definition_bytes)?;
                self.charge(files.len())?;
                let archive_report = FloorArchiveReport {
                    archive: id,
                    archive_revision: archive.revision,
                    container_bytes: reply.container.len(),
                    expanded_bytes: data.len(),
                    file_slots: file_slots(archive)?,
                    populated_definitions: files.len(),
                    definition_bytes,
                    crc_verified: true,
                    whirlpool_verified: archive.whirlpool.is_some(),
                };
                if id == UNDERLAY_ARCHIVE {
                    let mut definitions = reserved(files.len())?;
                    for file in files {
                        let definition = Underlay::decode(&file.data).map_err(|_| {
                            "invalid floor underlay definition or diagnostic bound exceeded"
                                .to_string()
                        })?;
                        definitions.push((file.id, definition));
                    }
                    self.underlays = definitions;
                    self.underlay_report = Some(archive_report);
                    self.stage = Stage::Archive {
                        table,
                        id: OVERLAY_ARCHIVE,
                    };
                    self.request(CONFIG_INDEX, OVERLAY_ARCHIVE)?;
                } else if id == OVERLAY_ARCHIVE {
                    let mut definitions = reserved(files.len())?;
                    for file in files {
                        let definition = Overlay::decode(file.id, &file.data).map_err(|_| {
                            "invalid floor overlay definition or diagnostic bound exceeded"
                                .to_string()
                        })?;
                        definitions.push((file.id, definition));
                    }
                    let underlays = self
                        .underlay_report
                        .take()
                        .ok_or_else(|| "floor underlay metadata unavailable".to_string())?;
                    self.repository = Some(FloorRepository {
                        underlays: std::mem::take(&mut self.underlays),
                        overlays: definitions,
                        underlay_file_slots: underlays.file_slots,
                        overlay_file_slots: archive_report.file_slots,
                    });
                    self.report = Some(WorldFloorReport {
                        revision: js5::REVISION,
                        subrevision: js5::SUBREVISION,
                        prefetch_count: self.transfer.as_ref().map_or(0, Transfer::prefetch_count),
                        index: CONFIG_INDEX,
                        reference_revision: table.revision,
                        reference_archives: table.archives.len(),
                        reference_files: self.reference_files,
                        reference_crc_verified: true,
                        reference_whirlpool_verified: true,
                        authenticated: false,
                        rendered_world: false,
                        opcode8_side_effects_applied: false,
                        total_wire_bytes: self.received,
                        total_container_bytes: self.containers,
                        total_expanded_bytes: self.expanded,
                        total_operations: self.operations,
                        underlays,
                        overlays: archive_report,
                        limits: limits_report(),
                    });
                } else {
                    return Err("invalid floor archive stage".into());
                }
            }
            Stage::Complete => return Err("unexpected floor reply".into()),
        }
        Ok(())
    }

    fn decode(&mut self, container: &[u8], integrity: Integrity<'_>) -> Result<Vec<u8>, String> {
        let start = if container.first() == Some(&0) { 1 } else { 5 };
        let length = container
            .get(start..start + 4)
            .ok_or_else(|| "invalid floor container header".to_string())?;
        let length = usize::try_from(u32::from_be_bytes(length.try_into().unwrap()))
            .map_err(|_| "floor expanded length overflow".to_string())?;
        let expanded = bounded_add(
            self.expanded,
            length,
            MAX_TOTAL_EXPANDED_BYTES,
            "floor cumulative expanded byte limit",
        )?;
        self.charge(length)?;
        let data = assets::decode_with_budget(
            container,
            integrity,
            MAX_TOTAL_EXPANDED_BYTES - self.expanded,
        )
        .map_err(|_| "invalid floor container or integrity check".to_string())?;
        self.expanded = expanded;
        Ok(data)
    }

    fn request(&mut self, index: u8, archive: u32) -> Result<(), String> {
        self.transfer
            .as_mut()
            .ok_or_else(|| "floor transfer unavailable".to_string())?
            .request(index, archive)
            .map_err(|_| "invalid floor request sequence".to_string())
    }

    fn charge(&mut self, count: usize) -> Result<(), String> {
        self.operations = bounded_add(
            self.operations,
            count,
            MAX_TOTAL_OPERATIONS,
            "floor cumulative operation limit",
        )?;
        Ok(())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.transfer = None;
        self.stage = Stage::Complete;
        self.underlays = Vec::new();
        self.underlay_report = None;
        self.repository = None;
        self.report = None;
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
        Err("connection ended before complete floor probe".into())
    }
}

fn validate_id(id: u32) -> Result<(), String> {
    if id > MAX_FLOOR_ID {
        Err("floor definition identity out of bounds".into())
    } else {
        Ok(())
    }
}

fn file_slots(archive: &openrs_assets::Archive) -> Result<u32, String> {
    match archive.files.last() {
        Some(file) => {
            validate_id(file.id)?;
            file.id
                .checked_add(1)
                .ok_or_else(|| "floor file slot overflow".into())
        }
        None => Ok(0),
    }
}

fn bounded_add(current: usize, count: usize, limit: usize, error: &str) -> Result<usize, String> {
    current
        .checked_add(count)
        .filter(|count| *count <= limit)
        .ok_or_else(|| error.to_string())
}

fn reserved<T>(count: usize) -> Result<Vec<T>, String> {
    if count > MAX_DEFINITIONS_PER_ARCHIVE {
        return Err("floor definition count limit".into());
    }
    let mut result = Vec::new();
    result
        .try_reserve_exact(count)
        .map_err(|_| "floor allocation failed".to_string())?;
    Ok(result)
}

fn limits_report() -> WorldFloorLimitsReport {
    let limits = assets::limits();
    WorldFloorLimitsReport {
        max_reference_archives: limits.max_archives,
        max_reference_files: limits.max_total_files,
        max_definitions_per_archive: MAX_DEFINITIONS_PER_ARCHIVE,
        max_total_definitions: MAX_TOTAL_DEFINITIONS,
        max_floor_id: MAX_FLOOR_ID,
        max_definition_bytes: MAX_DEFINITION_BYTES,
        max_tags_per_definition: MAX_TAGS_PER_DEFINITION,
        max_container_bytes: MAX_CONTAINER_BYTES,
        max_expanded_container_bytes: limits.max_uncompressed_bytes,
        max_total_wire_bytes: MAX_TOTAL_WIRE_BYTES,
        max_total_container_bytes: MAX_TOTAL_CONTAINER_BYTES,
        max_total_expanded_bytes: MAX_TOTAL_EXPANDED_BYTES,
        max_total_operations: MAX_TOTAL_OPERATIONS,
        operation_unit: "wire/container/expanded/split/definition bytes and bounded metadata records",
    }
}

#[cfg(test)]
mod tests;
