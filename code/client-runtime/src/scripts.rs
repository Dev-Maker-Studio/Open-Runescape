use crate::{CoreScriptReport, assets, cs2, transfer::Transfer};
use openrs_assets::{Integrity, ReferenceTable, parse_reference_table, split_archive};
use openrs_cs2::{Arguments, DecodeLimits, Opcode, Operand, Script};
use openrs_protocol::js5::{self, Reply, VersionEntry};
use serde::Serialize;
use std::collections::{BTreeMap, BTreeSet};

const MAX_SCRIPTS: usize = 64;
const MAX_SCRIPT_BYTES: usize = 64 * 1024;
const MAX_TOTAL_BYTES: usize = 1024 * 1024;
const MAX_TOTAL_INSTRUCTIONS: usize = 65_536;
const MAX_TOTAL_EXPANDED_BYTES: usize = 8 * 1024 * 1024;
const MAX_RECEIVED_BYTES: usize = 16 * 1024 * 1024;
const MAX_SCRIPT_ID: u32 = 1_000_000;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ScriptEntryReport {
    pub id: u32,
    pub bytes: usize,
    pub instructions: usize,
    pub argument_ints: u16,
    pub argument_strings: u16,
    pub argument_longs: u16,
    pub calls: Vec<u32>,
    pub crc_verified: bool,
    pub whirlpool_verified: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ScriptGraphReport {
    pub root: u32,
    pub reference_revision: i32,
    pub reference_crc_verified: bool,
    pub reference_whirlpool_verified: bool,
    pub authenticated: bool,
    pub total_bytes: usize,
    pub total_expanded_bytes: usize,
    pub total_instructions: usize,
    pub scripts: Vec<ScriptEntryReport>,
}

struct Repository {
    scripts: BTreeMap<i32, Script>,
    bytes: BTreeMap<u32, Vec<u8>>,
    reports: BTreeMap<u32, ScriptEntryReport>,
    total_bytes: usize,
    total_instructions: usize,
}

impl Repository {
    fn new() -> Self {
        Self {
            scripts: BTreeMap::new(),
            bytes: BTreeMap::new(),
            reports: BTreeMap::new(),
            total_bytes: 0,
            total_instructions: 0,
        }
    }

    fn insert(&mut self, id: u32, bytes: Vec<u8>, whirlpool: bool) -> Result<Vec<u32>, String> {
        if id > MAX_SCRIPT_ID || self.scripts.contains_key(&(id as i32)) {
            return Err("invalid or duplicate script identity".into());
        }
        if self.scripts.len() >= MAX_SCRIPTS {
            return Err("script graph count limit".into());
        }
        let total_bytes = self
            .total_bytes
            .checked_add(bytes.len())
            .filter(|count| *count <= MAX_TOTAL_BYTES)
            .ok_or_else(|| "script graph byte limit".to_string())?;
        let script = Script::decode_with_limits(
            &bytes,
            DecodeLimits {
                max_script_bytes: MAX_SCRIPT_BYTES,
                max_instructions: 8192,
                max_string_bytes: 64 * 1024,
            },
        )
        .map_err(|error| error.to_string())?;
        let total_instructions = self
            .total_instructions
            .checked_add(script.instructions().len())
            .filter(|count| *count <= MAX_TOTAL_INSTRUCTIONS)
            .ok_or_else(|| "script graph instruction limit".to_string())?;
        let mut calls = BTreeSet::new();
        for instruction in script.instructions() {
            if instruction.opcode == Opcode::GOSUB_WITH_PARAMS {
                let Operand::Int(callee) = instruction.operand else {
                    return Err("invalid script call operand".into());
                };
                let callee = u32::try_from(callee)
                    .ok()
                    .filter(|id| *id <= MAX_SCRIPT_ID)
                    .ok_or_else(|| "invalid script call identity".to_string())?;
                calls.insert(callee);
            }
        }
        let calls: Vec<u32> = calls.into_iter().collect();
        let args = script.arguments();
        self.reports.insert(
            id,
            ScriptEntryReport {
                id,
                bytes: bytes.len(),
                instructions: script.instructions().len(),
                argument_ints: args.ints,
                argument_strings: args.strings,
                argument_longs: args.longs,
                calls: calls.clone(),
                crc_verified: true,
                whirlpool_verified: whirlpool,
            },
        );
        self.scripts.insert(id as i32, script);
        self.bytes.insert(id, bytes);
        self.total_bytes = total_bytes;
        self.total_instructions = total_instructions;
        Ok(calls)
    }
}

enum Stage {
    VersionTable,
    ReferenceTable(VersionEntry),
    Archive { table: ReferenceTable, id: u32 },
    Complete,
}

pub struct ScriptGraphProbe {
    transfer: Transfer,
    root: u32,
    stage: Stage,
    repository: Repository,
    pending: BTreeSet<u32>,
    received: usize,
    expanded: usize,
    failed: bool,
    report: Option<ScriptGraphReport>,
}

impl ScriptGraphProbe {
    pub fn new(token: &[u8], root: u32) -> Result<Self, String> {
        if root > MAX_SCRIPT_ID {
            return Err("invalid root script identity".into());
        }
        Ok(Self {
            transfer: Transfer::new(token, 4 * 1024 * 1024)?,
            root,
            stage: Stage::VersionTable,
            repository: Repository::new(),
            pending: BTreeSet::from([root]),
            received: 0,
            expanded: 0,
            failed: false,
            report: None,
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
        !self.failed && self.report.is_some()
    }

    pub fn report(&self) -> Option<&ScriptGraphReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn file(&self, id: u32) -> Result<&[u8], String> {
        if !self.complete() {
            return Err("script graph not complete".into());
        }
        self.repository
            .bytes
            .get(&id)
            .map(Vec::as_slice)
            .ok_or_else(|| "script not loaded".into())
    }

    pub(crate) fn repository(&self) -> Result<(i32, &BTreeMap<i32, Script>), String> {
        if !self.complete() {
            return Err("script graph not complete".into());
        }
        Ok((self.root as i32, &self.repository.scripts))
    }

    pub fn run(&self, arguments: Arguments<'_>) -> Result<CoreScriptReport, String> {
        if !self.complete() {
            return Err("script graph not complete".into());
        }
        cs2::run_repository(self.root as i32, &self.repository.scripts, arguments)
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("script graph probe has failed".into());
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
            .ok_or_else(|| "script graph wire byte limit".to_string())?;
        while !bytes.is_empty() {
            if self.complete() {
                return Err("unexpected bytes after script graph".into());
            }
            let feed = self.transfer.feed(bytes)?;
            if feed.consumed == 0 && feed.item.is_none() {
                return Err("script graph decoder made no progress".into());
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
                    .nth(12)
                    .ok_or_else(|| "CS2 index absent from version table".to_string())?;
                self.stage = Stage::ReferenceTable(version);
                self.transfer.request(255, 12)?;
            }
            Stage::ReferenceTable(version) => {
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(version.crc as u32),
                        whirlpool: Some(&version.whirlpool),
                    },
                )?;
                let table = parse_reference_table(&data, &assets::limits())
                    .map_err(|error| error.to_string())?;
                self.next(table)?;
            }
            Stage::Archive { table, id } => {
                let archive = table
                    .archive(id)
                    .ok_or_else(|| "script archive absent from reference table".to_string())?;
                let data = self.decode(
                    &reply.container,
                    Integrity {
                        crc32: Some(archive.crc32),
                        whirlpool: archive.whirlpool.as_ref(),
                    },
                )?;
                let files = split_archive(&data, &archive.files, &assets::limits())
                    .map_err(|error| error.to_string())?;
                let file = files
                    .into_iter()
                    .find(|file| file.id == 0)
                    .ok_or_else(|| "script file zero absent from archive".to_string())?;
                let calls = self
                    .repository
                    .insert(id, file.data, archive.whirlpool.is_some())?;
                for callee in calls {
                    if !self.repository.scripts.contains_key(&(callee as i32)) {
                        self.pending.insert(callee);
                    }
                }
                if self.pending.len() + self.repository.scripts.len() > MAX_SCRIPTS {
                    return Err("script graph count limit".into());
                }
                self.next(table)?;
            }
            Stage::Complete => return Err("unexpected script graph reply".into()),
        }
        Ok(())
    }

    fn decode(&mut self, container: &[u8], integrity: Integrity<'_>) -> Result<Vec<u8>, String> {
        let remaining = MAX_TOTAL_EXPANDED_BYTES
            .checked_sub(self.expanded)
            .ok_or_else(|| "script graph expanded byte limit".to_string())?;
        let data = assets::decode_with_budget(container, integrity, remaining)?;
        self.expanded += data.len();
        Ok(data)
    }

    fn next(&mut self, table: ReferenceTable) -> Result<(), String> {
        if let Some(id) = self.pending.pop_first() {
            let archive = table
                .archive(id)
                .ok_or_else(|| "script archive absent from reference table".to_string())?;
            if archive.file(0).is_none() {
                return Err("script file zero absent from archive".into());
            }
            self.stage = Stage::Archive { table, id };
            self.transfer.request(12, id)?;
        } else {
            self.report = Some(ScriptGraphReport {
                root: self.root,
                reference_revision: table.revision,
                reference_crc_verified: true,
                reference_whirlpool_verified: true,
                authenticated: false,
                total_bytes: self.repository.total_bytes,
                total_expanded_bytes: self.expanded,
                total_instructions: self.repository.total_instructions,
                scripts: std::mem::take(&mut self.repository.reports)
                    .into_values()
                    .collect(),
            });
        }
        Ok(())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.report = None;
        self.stage = Stage::Complete;
        self.repository = Repository::new();
        self.pending.clear();
        self.expanded = 0;
        self.transfer.outgoing();
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.fail();
        Err("connection ended before a complete script graph".into())
    }
}

#[cfg(test)]
mod tests;
