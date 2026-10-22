use crate::transfer::Transfer;
use openrs_assets::{
    Archive, ArchiveFile, Integrity, Limits, decode_container, parse_reference_table, split_archive,
};
use openrs_cs2::{DecodeLimits, Script};
use openrs_protocol::js5::{self, Reply, VersionEntry};
use serde::Serialize;
use std::collections::BTreeMap;

const MAX_CONTAINER_BYTES: usize = 4 * 1024 * 1024;
const MAX_EXPANDED_BYTES: usize = 8 * 1024 * 1024;
const MAX_RECEIVED_BYTES: usize = 12 * 1024 * 1024;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AssetScriptReport {
    pub instructions: usize,
    pub local_ints: u16,
    pub local_strings: u16,
    pub local_longs: u16,
    pub argument_ints: u16,
    pub argument_strings: u16,
    pub argument_longs: u16,
    pub switch_tables: usize,
    pub opcodes: BTreeMap<String, usize>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AssetFileReport {
    pub id: u32,
    pub bytes: usize,
    pub script: Option<AssetScriptReport>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AssetReport {
    pub revision: u32,
    pub subrevision: u32,
    pub prefetch_count: usize,
    pub index: u8,
    pub archive: u32,
    pub reference_revision: i32,
    pub archive_revision: i32,
    pub reference_crc_verified: bool,
    pub reference_whirlpool_verified: bool,
    pub archive_crc_verified: bool,
    pub archive_whirlpool_verified: bool,
    pub authenticated: bool,
    pub container_bytes: usize,
    pub expanded_bytes: usize,
    pub file_slots: u32,
    pub files: Vec<AssetFileReport>,
}

enum Stage {
    VersionTable,
    ReferenceTable(VersionEntry),
    Archive {
        reference_revision: i32,
        archive: Archive,
    },
    Complete,
}

pub struct AssetProbe {
    transfer: Transfer,
    index: u8,
    archive: u32,
    stage: Stage,
    received: usize,
    failed: bool,
    report: Option<AssetReport>,
    files: Vec<ArchiveFile>,
}

impl AssetProbe {
    pub fn new(token: &[u8], index: u32, archive: u32) -> Result<Self, String> {
        if index >= 255 || archive > i32::MAX as u32 {
            return Err("invalid JS5 asset identity".into());
        }
        Ok(Self {
            transfer: Transfer::new(token, MAX_CONTAINER_BYTES)?,
            index: index as u8,
            archive,
            stage: Stage::VersionTable,
            received: 0,
            failed: false,
            report: None,
            files: Vec::new(),
        })
    }

    pub fn take_outgoing(&mut self) -> Vec<u8> {
        if self.failed {
            Vec::new()
        } else {
            self.transfer.outgoing()
        }
    }

    pub fn complete(&self) -> bool {
        self.report.is_some() && !self.failed
    }

    pub fn report(&self) -> Option<&AssetReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn file(&self, id: u32) -> Result<&[u8], String> {
        if !self.complete() {
            return Err("asset archive not complete".into());
        }
        self.files
            .iter()
            .find(|file| file.id == id)
            .map(|file| file.data.as_slice())
            .ok_or_else(|| "asset file not found".into())
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("asset probe has failed".into());
        }
        let result = self.feed_inner(bytes);
        if result.is_err() {
            self.fail();
        }
        result
    }

    fn feed_inner(&mut self, mut bytes: &[u8]) -> Result<(), String> {
        self.received = self
            .received
            .checked_add(bytes.len())
            .filter(|count| *count <= MAX_RECEIVED_BYTES)
            .ok_or_else(|| "asset probe byte limit".to_string())?;
        while !bytes.is_empty() {
            if self.complete() {
                return Err("unexpected bytes after asset archive".into());
            }
            let feed = self.transfer.feed(bytes)?;
            if feed.consumed == 0 && feed.item.is_none() {
                return Err("asset decoder made no progress".into());
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
                let table = js5::decode_version_table(&reply.container)
                    .map_err(|error| error.to_string())?;
                let version = table
                    .entries
                    .into_iter()
                    .nth(usize::from(self.index))
                    .ok_or_else(|| "asset index absent from version table".to_string())?;
                self.stage = Stage::ReferenceTable(version);
                self.transfer.request(255, u32::from(self.index))?;
            }
            Stage::ReferenceTable(version) => {
                let data = decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(version.crc as u32),
                        whirlpool: Some(&version.whirlpool),
                    },
                )?;
                let table = parse_reference_table(&data, &table_limits())
                    .map_err(|error| error.to_string())?;
                let archive = table
                    .archives
                    .into_iter()
                    .find(|entry| entry.id == self.archive)
                    .ok_or_else(|| "asset archive absent from reference table".to_string())?;
                self.stage = Stage::Archive {
                    reference_revision: table.revision,
                    archive,
                };
                self.transfer.request(self.index, self.archive)?;
            }
            Stage::Archive {
                reference_revision,
                archive,
            } => {
                let data = decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(archive.crc32),
                        whirlpool: archive.whirlpool.as_ref(),
                    },
                )?;
                let files = split_archive(&data, &archive.files, &limits())
                    .map_err(|error| error.to_string())?;
                let reports = files
                    .iter()
                    .map(|file| {
                        Ok(AssetFileReport {
                            id: file.id,
                            bytes: file.data.len(),
                            script: if self.index == 12 {
                                Some(script_report(&file.data)?)
                            } else {
                                None
                            },
                        })
                    })
                    .collect::<Result<Vec<_>, String>>()?;
                self.report = Some(AssetReport {
                    revision: js5::REVISION,
                    subrevision: js5::SUBREVISION,
                    prefetch_count: self.transfer.prefetch_count(),
                    index: self.index,
                    archive: self.archive,
                    reference_revision,
                    archive_revision: archive.revision,
                    reference_crc_verified: true,
                    reference_whirlpool_verified: true,
                    archive_crc_verified: true,
                    archive_whirlpool_verified: archive.whirlpool.is_some(),
                    authenticated: false,
                    container_bytes: reply.container.len(),
                    expanded_bytes: data.len(),
                    file_slots: archive.files.last().map_or(0, |file| file.id + 1),
                    files: reports,
                });
                self.files = files;
            }
            Stage::Complete => return Err("unexpected JS5 asset reply".into()),
        }
        Ok(())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.report = None;
        self.files.clear();
        self.stage = Stage::Complete;
        self.transfer.outgoing();
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.fail();
        Err("connection ended before a complete asset archive".into())
    }
}

pub(crate) fn limits() -> Limits {
    Limits {
        max_container_bytes: MAX_CONTAINER_BYTES,
        max_uncompressed_bytes: MAX_EXPANDED_BYTES,
        max_archives: 65_536,
        max_files_per_archive: 65_536,
        max_total_files: 262_144,
        max_archive_id: 1_000_000,
        max_file_id: 1_000_000,
    }
}

fn table_limits() -> Limits {
    Limits {
        max_archives: 262_144,
        max_total_files: 1_000_000,
        ..limits()
    }
}

pub(crate) fn decode(container: &[u8], integrity: Integrity<'_>) -> Result<Vec<u8>, String> {
    decode_with_budget(container, integrity, MAX_EXPANDED_BYTES)
}

pub(crate) fn decode_with_budget(
    container: &[u8],
    integrity: Integrity<'_>,
    remaining: usize,
) -> Result<Vec<u8>, String> {
    let start = if container.first() == Some(&0) { 1 } else { 5 };
    let length = container
        .get(start..start + 4)
        .ok_or_else(|| "truncated asset container".to_string())?;
    let length = u32::from_be_bytes(length.try_into().unwrap()) as usize;
    if length > MAX_EXPANDED_BYTES {
        return Err("asset expanded byte limit".into());
    }
    if length > remaining {
        return Err("asset cumulative expanded byte limit".into());
    }
    decode_container(container, integrity, &limits()).map_err(|error| error.to_string())
}

fn script_report(bytes: &[u8]) -> Result<AssetScriptReport, String> {
    let script = Script::decode_with_limits(
        bytes,
        DecodeLimits {
            max_script_bytes: 1024 * 1024,
            max_instructions: 100_000,
            max_string_bytes: 64 * 1024,
        },
    )
    .map_err(|error| error.to_string())?;
    let locals = script.locals();
    let arguments = script.arguments();
    let mut opcodes = BTreeMap::new();
    for instruction in script.instructions() {
        *opcodes
            .entry(instruction.opcode.metadata().name.to_string())
            .or_insert(0) += 1;
    }
    Ok(AssetScriptReport {
        instructions: script.instructions().len(),
        local_ints: locals.ints,
        local_strings: locals.strings,
        local_longs: locals.longs,
        argument_ints: arguments.ints,
        argument_strings: arguments.strings,
        argument_longs: arguments.longs,
        switch_tables: script.switches().len(),
        opcodes,
    })
}

#[cfg(test)]
mod tests;
