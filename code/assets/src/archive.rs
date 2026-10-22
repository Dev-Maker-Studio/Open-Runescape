use crate::{check_limit, copy_bytes, reserved, Error, FileMetadata, Limits, Reader};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ArchiveFile {
    pub id: u32,
    pub data: Vec<u8>,
}

pub fn split_archive(
    payload: &[u8],
    files: &[FileMetadata],
    limits: &Limits,
) -> Result<Vec<ArchiveFile>, Error> {
    check_limit(
        "archive bytes",
        payload.len(),
        limits.max_uncompressed_bytes,
    )?;
    check_limit(
        "files per archive",
        files.len(),
        limits.max_files_per_archive,
    )?;
    check_limit("total files", files.len(), limits.max_total_files)?;
    for (index, file) in files.iter().enumerate() {
        if file.id >= i32::MAX as u32 || index != 0 && file.id <= files[index - 1].id {
            return Err(Error::InvalidIds);
        }
        check_limit("file ID", file.id as usize, limits.max_file_id as usize)?;
    }
    if files.is_empty() {
        return if payload.is_empty() {
            Ok(Vec::new())
        } else {
            Err(Error::InvalidArchive)
        };
    }
    let mut output = reserved(files.len())?;
    if files.len() == 1 {
        output.push(ArchiveFile {
            id: files[0].id,
            data: copy_bytes(payload)?,
        });
        return Ok(output);
    }
    let chunks = usize::from(*payload.last().ok_or(Error::Truncated)?);
    if chunks == 0 {
        return Err(Error::InvalidArchive);
    }
    let table_size = files
        .len()
        .checked_mul(chunks)
        .and_then(|n| n.checked_mul(4))
        .ok_or(Error::InvalidArchive)?;
    let data_size = payload
        .len()
        .checked_sub(1)
        .and_then(|n| n.checked_sub(table_size))
        .ok_or(Error::InvalidArchive)?;
    let table = &payload[data_size..payload.len() - 1];
    let mut input = Reader::new(table);
    let mut lengths = reserved(files.len())?;
    lengths.resize(files.len(), 0usize);
    let mut total = 0usize;
    for _ in 0..chunks {
        let mut segment = 0i32;
        for length in &mut lengths {
            segment = segment
                .checked_add(input.i32()?)
                .filter(|n| *n >= 0)
                .ok_or(Error::InvalidArchive)?;
            *length = length
                .checked_add(segment as usize)
                .ok_or(Error::InvalidArchive)?;
            total = total
                .checked_add(segment as usize)
                .ok_or(Error::InvalidArchive)?;
            if total > data_size {
                return Err(Error::InvalidArchive);
            }
        }
    }
    if total != data_size {
        return Err(Error::InvalidArchive);
    }
    for (file, length) in files.iter().zip(lengths) {
        output.push(ArchiveFile {
            id: file.id,
            data: reserved(length)?,
        });
    }
    let mut source = Reader::new(&payload[..data_size]);
    let mut input = Reader::new(table);
    for _ in 0..chunks {
        let mut segment = 0i32;
        for file in &mut output {
            segment = segment
                .checked_add(input.i32()?)
                .filter(|n| *n >= 0)
                .ok_or(Error::InvalidArchive)?;
            file.data.extend_from_slice(source.take(segment as usize)?);
        }
    }
    source.finish()?;
    input.finish()?;
    Ok(output)
}
