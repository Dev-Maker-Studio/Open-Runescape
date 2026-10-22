mod archive;
mod material;
mod reference;
mod texture;
mod xtea;

pub use xtea::{decrypt_container, is_zero_key, trim_revision};

pub use archive::{split_archive, ArchiveFile};
pub use material::{is_opaque_effect, parse_material_table, MaterialTable, MaterialType};
pub use reference::{
    parse_reference_table, parse_reference_table_by_names, Archive, FileMetadata,
    ReferenceProjectionLimits, ReferenceTable, ReferenceTableProjection,
};
pub use texture::{render_material, MaterialSource, TextureGraph, TextureSources};

use std::fmt;
use std::io::Read;
use whirlpool::{Digest, Whirlpool};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Limits {
    pub max_container_bytes: usize,
    pub max_uncompressed_bytes: usize,
    pub max_archives: usize,
    pub max_files_per_archive: usize,
    pub max_total_files: usize,
    pub max_archive_id: u32,
    pub max_file_id: u32,
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            max_container_bytes: 16 * 1024 * 1024,
            max_uncompressed_bytes: 64 * 1024 * 1024,
            max_archives: 65_536,
            max_files_per_archive: 65_536,
            max_total_files: 1_000_000,
            max_archive_id: 1_000_000,
            max_file_id: 1_000_000,
        }
    }
}

#[derive(Debug, Clone, Copy, Default)]
pub struct Integrity<'a> {
    pub crc32: Option<u32>,
    pub whirlpool: Option<&'a [u8; 64]>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    Truncated,
    TrailingData,
    InvalidLength,
    LimitExceeded {
        resource: &'static str,
        value: usize,
        limit: usize,
    },
    AllocationFailed,
    UnsupportedCompression(u8),
    InvalidCompression,
    CrcMismatch {
        expected: u32,
        actual: u32,
    },
    WhirlpoolMismatch,
    UnsupportedProtocol(u8),
    UnsupportedFlags(u8),
    InvalidIds,
    AmbiguousNameHash,
    InvalidArchive,
    UnsupportedTextureOperation(u8),
    InvalidTexture(&'static str),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Truncated => f.write_str("truncated asset data"),
            Self::TrailingData => f.write_str("trailing asset data"),
            Self::InvalidLength => f.write_str("invalid asset length"),
            Self::LimitExceeded {
                resource,
                value,
                limit,
            } => {
                write!(f, "asset {resource} limit exceeded: {value} > {limit}")
            }
            Self::AllocationFailed => f.write_str("asset allocation failed"),
            Self::UnsupportedCompression(mode) => write!(f, "unsupported JS5 compression {mode}"),
            Self::InvalidCompression => f.write_str("invalid compressed asset"),
            Self::CrcMismatch { expected, actual } => {
                write!(f, "asset CRC mismatch: {actual:08x} != {expected:08x}")
            }
            Self::WhirlpoolMismatch => f.write_str("asset Whirlpool mismatch"),
            Self::UnsupportedProtocol(protocol) => {
                write!(f, "unsupported reference-table protocol {protocol}")
            }
            Self::UnsupportedFlags(flags) => {
                write!(f, "unsupported reference-table flags {flags:#04x}")
            }
            Self::InvalidIds => f.write_str("invalid or duplicate asset IDs"),
            Self::AmbiguousNameHash => f.write_str("ambiguous archive name hash"),
            Self::InvalidArchive => f.write_str("inconsistent archive chunk table"),
            Self::UnsupportedTextureOperation(id) => {
                write!(f, "unsupported texture operation {id}")
            }
            Self::InvalidTexture(reason) => write!(f, "invalid texture definition: {reason}"),
        }
    }
}

impl std::error::Error for Error {}

pub fn decode_container(
    container: &[u8],
    integrity: Integrity<'_>,
    limits: &Limits,
) -> Result<Vec<u8>, Error> {
    check_limit(
        "container bytes",
        container.len(),
        limits.max_container_bytes,
    )?;
    let mut input = Reader::new(container);
    let mode = input.u8()?;
    let compressed_len = input.length()?;
    let expected = match mode {
        0 => compressed_len,
        1 | 2 => input.length()?,
        other => return Err(Error::UnsupportedCompression(other)),
    };
    check_limit(
        "uncompressed bytes",
        expected,
        limits.max_uncompressed_bytes,
    )?;
    let compressed = input.take(compressed_len)?;
    input.finish()?;
    if let Some(expected) = integrity.crc32 {
        let actual = crc32fast::hash(container);
        if actual != expected {
            return Err(Error::CrcMismatch { expected, actual });
        }
    }
    if let Some(expected) = integrity.whirlpool {
        if Whirlpool::digest(container).as_slice() != expected {
            return Err(Error::WhirlpoolMismatch);
        }
    }
    match mode {
        0 => copy_bytes(compressed),
        1 => decode_bzip2(compressed, expected),
        2 => decode_gzip(compressed, expected),
        other => Err(Error::UnsupportedCompression(other)),
    }
}

fn decode_gzip(compressed: &[u8], expected: usize) -> Result<Vec<u8>, Error> {
    if compressed.len() < 18 {
        return Err(Error::Truncated);
    }
    if compressed[..4] != [0x1f, 0x8b, 8, 0] {
        return Err(Error::InvalidCompression);
    }
    let mut decoder = flate2::bufread::GzDecoder::new(compressed);
    let output = read_bounded(&mut decoder, expected)?;
    if !decoder.into_inner().is_empty() {
        return Err(Error::TrailingData);
    }
    Ok(output)
}

fn decode_bzip2(compressed: &[u8], expected: usize) -> Result<Vec<u8>, Error> {
    let input = (&b"BZh1"[..]).chain(compressed);
    let mut decoder = bzip2::bufread::BzDecoder::new(input);
    let output = read_bounded(&mut decoder, expected)?;
    if decoder.total_in() != compressed.len() as u64 + 4 {
        return Err(Error::TrailingData);
    }
    Ok(output)
}

fn read_bounded(decoder: &mut impl Read, expected: usize) -> Result<Vec<u8>, Error> {
    let capacity = expected.checked_add(1).ok_or(Error::InvalidLength)?;
    let mut output = zeroed(capacity)?;
    let mut used = 0;
    loop {
        let read = decoder
            .read(&mut output[used..])
            .map_err(|_| Error::InvalidCompression)?;
        if read == 0 {
            break;
        }
        used += read;
        if used > expected {
            return Err(Error::InvalidLength);
        }
    }
    if used != expected {
        return Err(Error::InvalidLength);
    }
    output.truncate(expected);
    Ok(output)
}

fn check_limit(resource: &'static str, value: usize, limit: usize) -> Result<(), Error> {
    if value > limit {
        Err(Error::LimitExceeded {
            resource,
            value,
            limit,
        })
    } else {
        Ok(())
    }
}

fn reserved<T>(capacity: usize) -> Result<Vec<T>, Error> {
    let mut result = Vec::new();
    result
        .try_reserve_exact(capacity)
        .map_err(|_| Error::AllocationFailed)?;
    Ok(result)
}

fn zeroed(length: usize) -> Result<Vec<u8>, Error> {
    let mut result = reserved(length)?;
    result.resize(length, 0);
    Ok(result)
}

fn copy_bytes(bytes: &[u8]) -> Result<Vec<u8>, Error> {
    let mut result = reserved(bytes.len())?;
    result.extend_from_slice(bytes);
    Ok(result)
}

#[derive(Clone, Copy)]
struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> Reader<'a> {
    fn new(bytes: &'a [u8]) -> Self {
        Self { bytes, position: 0 }
    }

    fn remaining(&self) -> usize {
        self.bytes.len() - self.position
    }

    fn take(&mut self, count: usize) -> Result<&'a [u8], Error> {
        if count > self.remaining() {
            return Err(Error::Truncated);
        }
        let result = &self.bytes[self.position..self.position + count];
        self.position += count;
        Ok(result)
    }

    fn u8(&mut self) -> Result<u8, Error> {
        Ok(self.take(1)?[0])
    }

    fn u16(&mut self) -> Result<u16, Error> {
        let bytes = self.take(2)?;
        Ok(u16::from_be_bytes([bytes[0], bytes[1]]))
    }

    fn u32(&mut self) -> Result<u32, Error> {
        let bytes = self.take(4)?;
        Ok(u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
    }

    fn i32(&mut self) -> Result<i32, Error> {
        Ok(self.u32()? as i32)
    }

    fn length(&mut self) -> Result<usize, Error> {
        let value = self.i32()?;
        usize::try_from(value).map_err(|_| Error::InvalidLength)
    }

    fn smart(&mut self, protocol: u8) -> Result<u32, Error> {
        if protocol >= 7 && *self.bytes.get(self.position).ok_or(Error::Truncated)? >= 128 {
            Ok(self.u32()? & 0x7fff_ffff)
        } else {
            Ok(u32::from(self.u16()?))
        }
    }

    fn finish(&self) -> Result<(), Error> {
        if self.remaining() == 0 {
            Ok(())
        } else {
            Err(Error::TrailingData)
        }
    }
}
