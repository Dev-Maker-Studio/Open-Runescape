use crate::{check_limit, reserved, Error, Limits, Reader};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FileMetadata {
    pub id: u32,
    pub name_hash: Option<i32>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Archive {
    pub id: u32,
    pub crc32: u32,
    pub revision: i32,
    pub name_hash: Option<i32>,
    pub whirlpool: Option<[u8; 64]>,
    pub files: Vec<FileMetadata>,
}

impl Archive {
    pub fn file(&self, id: u32) -> Option<&FileMetadata> {
        self.files
            .binary_search_by_key(&id, |file| file.id)
            .ok()
            .map(|i| &self.files[i])
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReferenceTable {
    pub protocol: u8,
    pub revision: i32,
    pub flags: u8,
    pub archives: Vec<Archive>,
}

impl ReferenceTable {
    pub fn archive(&self, id: u32) -> Option<&Archive> {
        self.archives
            .binary_search_by_key(&id, |archive| archive.id)
            .ok()
            .map(|i| &self.archives[i])
    }
}

pub fn parse_reference_table(payload: &[u8], limits: &Limits) -> Result<ReferenceTable, Error> {
    check_limit(
        "reference-table bytes",
        payload.len(),
        limits.max_uncompressed_bytes,
    )?;
    let mut input = Reader::new(payload);
    let protocol = input.u8()?;
    if !(5..=7).contains(&protocol) {
        return Err(Error::UnsupportedProtocol(protocol));
    }
    let revision = if protocol >= 6 { input.i32()? } else { 0 };
    let flags = input.u8()?;
    if flags & !3 != 0 {
        return Err(Error::UnsupportedFlags(flags));
    }
    let count = input.smart(protocol)? as usize;
    check_limit("archives", count, limits.max_archives)?;
    let named = flags & 1 != 0;
    let hashed = flags & 2 != 0;
    let minimum_per_archive = 12 + if named { 4 } else { 0 } + if hashed { 64 } else { 0 };
    if count > input.remaining() / minimum_per_archive {
        return Err(Error::Truncated);
    }
    let mut archives = reserved(count)?;
    let mut id = 0u32;
    for index in 0..count {
        id = next_id(
            &mut input,
            protocol,
            id,
            index,
            limits.max_archive_id,
            "archive ID",
        )?;
        archives.push(Archive {
            id,
            crc32: 0,
            revision: 0,
            name_hash: None,
            whirlpool: None,
            files: Vec::new(),
        });
    }
    if named {
        for archive in &mut archives {
            archive.name_hash = Some(input.i32()?);
        }
    }
    for archive in &mut archives {
        archive.crc32 = input.u32()?;
    }
    if hashed {
        for archive in &mut archives {
            let mut digest = [0; 64];
            digest.copy_from_slice(input.take(64)?);
            archive.whirlpool = Some(digest);
        }
    }
    for archive in &mut archives {
        archive.revision = input.i32()?;
    }
    let mut counts = reserved(count)?;
    let mut total_files = 0usize;
    for _ in 0..count {
        let files = input.smart(protocol)? as usize;
        check_limit("files per archive", files, limits.max_files_per_archive)?;
        total_files = total_files.checked_add(files).ok_or(Error::InvalidLength)?;
        check_limit("total files", total_files, limits.max_total_files)?;
        counts.push(files);
    }
    let minimum_per_file = if named { 6 } else { 2 };
    if total_files > input.remaining() / minimum_per_file {
        return Err(Error::Truncated);
    }
    for (archive, count) in archives.iter_mut().zip(counts) {
        archive.files = reserved(count)?;
        let mut id = 0u32;
        for index in 0..count {
            id = next_id(
                &mut input,
                protocol,
                id,
                index,
                limits.max_file_id,
                "file ID",
            )?;
            archive.files.push(FileMetadata {
                id,
                name_hash: None,
            });
        }
    }
    if named {
        for archive in &mut archives {
            for file in &mut archive.files {
                file.name_hash = Some(input.i32()?);
            }
        }
    }
    input.finish()?;
    Ok(ReferenceTable {
        protocol,
        revision,
        flags,
        archives,
    })
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReferenceProjectionLimits {
    pub table: Limits,
    pub max_selected_archives: usize,
    pub max_selected_files: usize,
    pub max_total_records: usize,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReferenceTableProjection {
    pub table: ReferenceTable,
    pub archive_count: usize,
    pub total_files: usize,
    pub scan_operations: usize,
}

struct SelectedArchive {
    position: usize,
    file_count: usize,
    archive: Archive,
}

pub fn parse_reference_table_by_names(
    payload: &[u8],
    name_hashes: &[i32],
    limits: &ReferenceProjectionLimits,
) -> Result<ReferenceTableProjection, Error> {
    check_limit(
        "selected archive names",
        name_hashes.len(),
        limits.max_selected_archives,
    )?;
    for (index, hash) in name_hashes.iter().enumerate() {
        if name_hashes[..index].contains(hash) {
            return Err(Error::AmbiguousNameHash);
        }
    }
    check_limit(
        "reference-table bytes",
        payload.len(),
        limits.table.max_uncompressed_bytes,
    )?;
    let mut input = Reader::new(payload);
    let protocol = input.u8()?;
    if !(5..=7).contains(&protocol) {
        return Err(Error::UnsupportedProtocol(protocol));
    }
    let revision = if protocol >= 6 { input.i32()? } else { 0 };
    let flags = input.u8()?;
    if flags & !3 != 0 {
        return Err(Error::UnsupportedFlags(flags));
    }
    let count = input.smart(protocol)? as usize;
    check_limit("archives", count, limits.table.max_archives)?;
    check_limit("metadata records", count, limits.max_total_records)?;
    let named = flags & 1 != 0;
    let hashed = flags & 2 != 0;
    let minimum_per_archive = 12 + if named { 4 } else { 0 } + if hashed { 64 } else { 0 };
    if count > input.remaining() / minimum_per_archive {
        return Err(Error::Truncated);
    }
    let mut ids = input;
    let mut id = 0;
    for index in 0..count {
        id = next_id(
            &mut input,
            protocol,
            id,
            index,
            limits.table.max_archive_id,
            "archive ID",
        )?;
    }
    let mut selected = reserved(name_hashes.len().min(count))?;
    if named {
        id = 0;
        for position in 0..count {
            id = next_id(
                &mut ids,
                protocol,
                id,
                position,
                limits.table.max_archive_id,
                "archive ID",
            )?;
            let hash = input.i32()?;
            if name_hashes.contains(&hash) {
                if selected
                    .iter()
                    .any(|entry: &SelectedArchive| entry.archive.name_hash == Some(hash))
                {
                    return Err(Error::AmbiguousNameHash);
                }
                check_limit(
                    "selected archives",
                    selected.len() + 1,
                    limits.max_selected_archives,
                )?;
                selected.push(SelectedArchive {
                    position,
                    file_count: 0,
                    archive: Archive {
                        id,
                        crc32: 0,
                        revision: 0,
                        name_hash: Some(hash),
                        whirlpool: None,
                        files: Vec::new(),
                    },
                });
            }
        }
    }
    let mut next = 0;
    for position in 0..count {
        let crc = input.u32()?;
        if let Some(entry) = selected.get_mut(next) {
            if entry.position == position {
                entry.archive.crc32 = crc;
                next += 1;
            }
        }
    }
    if hashed {
        next = 0;
        for position in 0..count {
            let digest = input.take(64)?;
            if let Some(entry) = selected.get_mut(next) {
                if entry.position == position {
                    let mut bytes = [0; 64];
                    bytes.copy_from_slice(digest);
                    entry.archive.whirlpool = Some(bytes);
                    next += 1;
                }
            }
        }
    }
    next = 0;
    for position in 0..count {
        let revision = input.i32()?;
        if let Some(entry) = selected.get_mut(next) {
            if entry.position == position {
                entry.archive.revision = revision;
                next += 1;
            }
        }
    }
    let counts = input;
    let mut total_files = 0usize;
    let mut selected_files = 0usize;
    next = 0;
    for position in 0..count {
        let files = input.smart(protocol)? as usize;
        check_limit(
            "files per archive",
            files,
            limits.table.max_files_per_archive,
        )?;
        total_files = total_files.checked_add(files).ok_or(Error::InvalidLength)?;
        check_limit("total files", total_files, limits.table.max_total_files)?;
        check_limit(
            "metadata records",
            count.checked_add(total_files).ok_or(Error::InvalidLength)?,
            limits.max_total_records,
        )?;
        if let Some(entry) = selected.get_mut(next) {
            if entry.position == position {
                selected_files = selected_files
                    .checked_add(files)
                    .ok_or(Error::InvalidLength)?;
                check_limit("selected files", selected_files, limits.max_selected_files)?;
                entry.file_count = files;
                next += 1;
            }
        }
    }
    let minimum_per_file = if named { 6 } else { 2 };
    if total_files > input.remaining() / minimum_per_file {
        return Err(Error::Truncated);
    }
    for entry in &mut selected {
        entry.archive.files = reserved(entry.file_count)?;
    }
    let mut file_counts = counts;
    next = 0;
    for position in 0..count {
        let files = file_counts.smart(protocol)? as usize;
        let entry = selected
            .get_mut(next)
            .filter(|entry| entry.position == position);
        let mut id = 0;
        if let Some(entry) = entry {
            for index in 0..files {
                id = next_id(
                    &mut input,
                    protocol,
                    id,
                    index,
                    limits.table.max_file_id,
                    "file ID",
                )?;
                entry.archive.files.push(FileMetadata {
                    id,
                    name_hash: None,
                });
            }
            next += 1;
        } else {
            for index in 0..files {
                id = next_id(
                    &mut input,
                    protocol,
                    id,
                    index,
                    limits.table.max_file_id,
                    "file ID",
                )?;
            }
        }
    }
    if named {
        file_counts = counts;
        next = 0;
        for position in 0..count {
            let files = file_counts.smart(protocol)? as usize;
            if let Some(entry) = selected
                .get_mut(next)
                .filter(|entry| entry.position == position)
            {
                for file in &mut entry.archive.files {
                    file.name_hash = Some(input.i32()?);
                }
                next += 1;
            } else {
                input.take(files.checked_mul(4).ok_or(Error::InvalidLength)?)?;
            }
        }
    }
    input.finish()?;
    let scan_operations = count
        .checked_mul(
            name_hashes
                .len()
                .checked_add(3)
                .ok_or(Error::InvalidLength)?,
        )
        .and_then(|work| work.checked_add(total_files))
        .and_then(|work| work.checked_add(payload.len()))
        .ok_or(Error::InvalidLength)?;
    let mut archives = reserved(selected.len())?;
    archives.extend(selected.into_iter().map(|entry| entry.archive));
    Ok(ReferenceTableProjection {
        table: ReferenceTable {
            protocol,
            revision,
            flags,
            archives,
        },
        archive_count: count,
        total_files,
        scan_operations,
    })
}

fn next_id(
    input: &mut Reader<'_>,
    protocol: u8,
    previous: u32,
    index: usize,
    limit: u32,
    resource: &'static str,
) -> Result<u32, Error> {
    let delta = input.smart(protocol)?;
    if index != 0 && delta == 0 {
        return Err(Error::InvalidIds);
    }
    let id = previous.checked_add(delta).ok_or(Error::InvalidIds)?;
    if id >= i32::MAX as u32 {
        return Err(Error::InvalidIds);
    }
    check_limit(resource, id as usize, limit as usize)?;
    Ok(id)
}
