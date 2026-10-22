use openrs_assets::{
    Integrity, Limits, ReferenceTable, decode_container, parse_reference_table, split_archive,
};
use openrs_cs2::{HostError, Varbit, VarbitLookup};
use openrs_protocol::js5::VersionEntry;
use std::fmt;

pub const VARBIT_INDEX: u8 = 22;
pub const VARBIT_ARCHIVE_BITS: u32 = 10;
pub const MAX_VARBIT_ID: i32 = 0x03ff_ffff;
pub const MAX_DEFINITION_BYTES: usize = 321;
pub const MAX_REFERENCE_BYTES: usize = 2 * 1024 * 1024;
pub const MAX_ARCHIVE_BYTES: usize = 512 * 1024;
pub const MAX_REPOSITORY_BYTES: usize = 4 * 1024 * 1024;
pub const MAX_REFERENCE_ARCHIVES: usize = 256;
pub const MAX_DEFINITIONS: usize = 65_536;
pub const MAX_LOADED_ARCHIVES: usize = 64;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DefinitionError {
    Truncated,
    TrailingData,
    UnknownOpcode(u8),
    InvalidSpan { start: u8, end: u8 },
    ByteLimit,
}

impl fmt::Display for DefinitionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "varbit definition: {self:?}")
    }
}

impl std::error::Error for DefinitionError {}

pub fn decode_definition(bytes: &[u8]) -> Result<Varbit, DefinitionError> {
    if bytes.len() > MAX_DEFINITION_BYTES {
        return Err(DefinitionError::ByteLimit);
    }
    let mut remaining = bytes;
    let mut definition = Varbit {
        varp: 0,
        start: 0,
        end: 0,
    };
    loop {
        let (&opcode, rest) = remaining.split_first().ok_or(DefinitionError::Truncated)?;
        remaining = rest;
        match opcode {
            0 => {
                return if remaining.is_empty() {
                    Ok(definition)
                } else {
                    Err(DefinitionError::TrailingData)
                };
            }
            1 => {
                let fields = remaining.get(..4).ok_or(DefinitionError::Truncated)?;
                let start = fields[2];
                let end = fields[3];
                if start > end || end - start > 31 {
                    return Err(DefinitionError::InvalidSpan { start, end });
                }
                definition = Varbit {
                    varp: u16::from_be_bytes([fields[0], fields[1]]),
                    start,
                    end,
                };
                remaining = &remaining[4..];
            }
            unknown => return Err(DefinitionError::UnknownOpcode(unknown)),
        }
    }
}

pub fn bit_mask(definition: &Varbit) -> i32 {
    2i32.wrapping_shl(u32::from(definition.end.wrapping_sub(definition.start)))
        .wrapping_sub(1)
}

pub fn read_bits(definition: &Varbit, varp_value: i32) -> i32 {
    varp_value.wrapping_shr(u32::from(definition.start)) & bit_mask(definition)
}

pub fn entity_read_bits(definition: &Varbit, varp_value: i32) -> i32 {
    let end = u32::from(definition.end);
    let mask = if end == 31 {
        -1
    } else {
        1i32.wrapping_shl(end + 1).wrapping_sub(1)
    };
    ((varp_value & mask) as u32).wrapping_shr(u32::from(definition.start)) as i32
}

pub fn write_bits(definition: &Varbit, previous: i32, value: i32) -> i32 {
    let mask = bit_mask(definition);
    let value = if value < 0 || value > mask { 0 } else { value };
    let shifted = mask.wrapping_shl(u32::from(definition.start));
    (previous & !shifted) | (value.wrapping_shl(u32::from(definition.start)) & shifted)
}

pub fn definition_location(id: i32) -> Result<(u32, u32), HostError> {
    if !(0..=MAX_VARBIT_ID).contains(&id) {
        return Err(HostError::InvalidVarbit(id));
    }
    let id = id as u32;
    Ok((id >> VARBIT_ARCHIVE_BITS, id & 1023))
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RepositoryError {
    Asset(openrs_assets::Error),
    Definition { id: u32, error: DefinitionError },
    ReferenceUnavailable,
    ReferenceAlreadyLoaded,
    RevisionMismatch { expected: i32, actual: i32 },
    ArchiveAbsent(u32),
    ArchiveAlreadyLoaded(u32),
    InvalidArchiveId(u32),
    LoadedArchiveLimit,
    ExpandedByteLimit,
    AllocationFailed,
}

impl fmt::Display for RepositoryError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "varbit repository: {self:?}")
    }
}

impl std::error::Error for RepositoryError {}

impl From<openrs_assets::Error> for RepositoryError {
    fn from(error: openrs_assets::Error) -> Self {
        Self::Asset(error)
    }
}

#[derive(Debug)]
struct LoadedArchive {
    id: u32,
    definitions: Vec<(u32, Varbit)>,
}

#[derive(Debug, Default)]
pub struct VarbitRepository {
    reference: Option<ReferenceTable>,
    loaded: Vec<LoadedArchive>,
    expanded_bytes: usize,
}

impl VarbitRepository {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn load_reference(
        &mut self,
        container: &[u8],
        version: &VersionEntry,
    ) -> Result<(), RepositoryError> {
        if self.reference.is_some() {
            return Err(RepositoryError::ReferenceAlreadyLoaded);
        }
        let limits = limits(MAX_REFERENCE_BYTES);
        let bytes = decode_container(
            container,
            Integrity {
                crc32: Some(version.crc as u32),
                whirlpool: Some(&version.whirlpool),
            },
            &limits,
        )?;
        let reference = parse_reference_table(&bytes, &limits)?;
        if reference.revision != version.revision {
            return Err(RepositoryError::RevisionMismatch {
                expected: version.revision,
                actual: reference.revision,
            });
        }
        self.expanded_bytes = bytes.len();
        self.reference = Some(reference);
        Ok(())
    }

    pub fn reference_loaded(&self) -> bool {
        self.reference.is_some()
    }

    pub fn loaded_archive_count(&self) -> usize {
        self.loaded.len()
    }

    pub fn definition_count(&self) -> usize {
        self.loaded
            .iter()
            .map(|archive| archive.definitions.len())
            .sum()
    }

    pub fn expanded_bytes(&self) -> usize {
        self.expanded_bytes
    }

    pub fn load_archive(&mut self, id: u32, container: &[u8]) -> Result<(), RepositoryError> {
        if id > u16::MAX as u32 {
            return Err(RepositoryError::InvalidArchiveId(id));
        }
        let reference = self
            .reference
            .as_ref()
            .ok_or(RepositoryError::ReferenceUnavailable)?;
        let archive = reference
            .archive(id)
            .ok_or(RepositoryError::ArchiveAbsent(id))?;
        if self.loaded.iter().any(|loaded| loaded.id == id) {
            return Err(RepositoryError::ArchiveAlreadyLoaded(id));
        }
        if self.loaded.len() >= MAX_LOADED_ARCHIVES {
            return Err(RepositoryError::LoadedArchiveLimit);
        }
        let remaining = MAX_REPOSITORY_BYTES
            .checked_sub(self.expanded_bytes)
            .ok_or(RepositoryError::ExpandedByteLimit)?;
        let limits = limits(MAX_ARCHIVE_BYTES.min(remaining));
        let bytes = decode_container(
            container,
            Integrity {
                crc32: Some(archive.crc32),
                whirlpool: archive.whirlpool.as_ref(),
            },
            &limits,
        )?;
        let files = split_archive(&bytes, &archive.files, &limits)?;
        let mut definitions = Vec::new();
        definitions
            .try_reserve_exact(files.len())
            .map_err(|_| RepositoryError::AllocationFailed)?;
        for file in files {
            let definition_id = (id << VARBIT_ARCHIVE_BITS) | file.id;
            let definition =
                decode_definition(&file.data).map_err(|error| RepositoryError::Definition {
                    id: definition_id,
                    error,
                })?;
            definitions.push((file.id, definition));
        }
        self.loaded
            .try_reserve(1)
            .map_err(|_| RepositoryError::AllocationFailed)?;
        self.loaded.push(LoadedArchive { id, definitions });
        self.expanded_bytes += bytes.len();
        Ok(())
    }

    pub fn lookup(&self, id: i32) -> Result<VarbitLookup, HostError> {
        let (archive_id, file_id) = definition_location(id)?;
        let reference = self.reference.as_ref().ok_or(HostError::Unavailable)?;
        let Some(archive) = reference.archive(archive_id) else {
            return Ok(VarbitLookup::Absent);
        };
        if archive.file(file_id).is_none() {
            return Ok(VarbitLookup::Absent);
        }
        let loaded = self
            .loaded
            .iter()
            .find(|loaded| loaded.id == archive_id)
            .ok_or(HostError::Unavailable)?;
        let index = loaded
            .definitions
            .binary_search_by_key(&file_id, |(id, _)| *id)
            .map_err(|_| HostError::Unavailable)?;
        Ok(VarbitLookup::Defined(loaded.definitions[index].1))
    }
}

fn limits(max_bytes: usize) -> Limits {
    Limits {
        max_container_bytes: max_bytes,
        max_uncompressed_bytes: max_bytes,
        max_archives: MAX_REFERENCE_ARCHIVES,
        max_files_per_archive: 1024,
        max_total_files: MAX_DEFINITIONS,
        max_archive_id: u16::MAX as u32,
        max_file_id: 1023,
    }
}

#[cfg(test)]
mod tests;
