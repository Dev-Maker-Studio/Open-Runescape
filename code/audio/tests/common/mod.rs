#![allow(dead_code)]

use flate2::read::GzDecoder;
use sha2::{Digest as _, Sha256};
use std::fmt::Write as _;
use std::io::Read;

#[cfg(not(target_arch = "wasm32"))]
#[allow(unused_imports)]
pub use native::Cache;

#[cfg(not(target_arch = "wasm32"))]
mod native {
    use openrs_assets::{
        decode_container, parse_reference_table, split_archive, Integrity, Limits, ReferenceTable,
    };
    use std::collections::HashMap;
    use std::fs::File;
    use std::io::{Read, Seek, SeekFrom};
    use std::path::PathBuf;

    pub fn limits() -> Limits {
        Limits {
            max_container_bytes: 64 << 20,
            max_uncompressed_bytes: 64 << 20,
            max_archives: 1 << 20,
            max_files_per_archive: 65_536,
            max_total_files: 1 << 22,
            max_archive_id: 1 << 20,
            max_file_id: 65_535,
        }
    }

    pub struct Cache {
        root: PathBuf,
        dat: File,
        tables: HashMap<u32, ReferenceTable>,
        archives: HashMap<(u32, u32), HashMap<u32, Vec<u8>>>,
    }

    impl Cache {
        pub fn open() -> Option<Self> {
            let root = match std::env::var_os("OPENRS_CACHE") {
                Some(path) => PathBuf::from(path),
                None => PathBuf::from(std::env::var_os("HOME")?).join(".darkanrs/caches/darkan/LIVE"),
            };
            let dat = File::open(root.join("main_file_cache.dat2")).ok()?;
            Some(Self {
                root,
                dat,
                tables: HashMap::new(),
                archives: HashMap::new(),
            })
        }

        fn container(&mut self, index: u32, archive: u32) -> Option<Vec<u8>> {
            let idx = std::fs::read(self.root.join(format!("main_file_cache.idx{index}"))).ok()?;
            let at = archive as usize * 6;
            let entry = idx.get(at..at + 6)?;
            let size = u32::from_be_bytes([0, entry[0], entry[1], entry[2]]) as usize;
            let mut sector = u32::from_be_bytes([0, entry[3], entry[4], entry[5]]) as u64;
            if size == 0 {
                return None;
            }
            let header = if archive > 0xFFFF { 10 } else { 8 };
            let mut out = Vec::with_capacity(size);
            let mut block = [0u8; 520];
            while out.len() < size {
                self.dat.seek(SeekFrom::Start(sector * 520)).ok()?;
                self.dat.read_exact(&mut block).ok()?;
                let take = (size - out.len()).min(520 - header);
                out.extend_from_slice(&block[header..header + take]);
                sector =
                    u32::from_be_bytes([0, block[header - 4], block[header - 3], block[header - 2]])
                        as u64;
            }
            let length = u32::from_be_bytes(out[1..5].try_into().ok()?) as usize;
            let body = if out[0] == 0 { 5 + length } else { 9 + length };
            out.truncate(body);
            Some(out)
        }

        pub fn table(&mut self, index: u32) -> &ReferenceTable {
            if !self.tables.contains_key(&index) {
                let container = self
                    .container(255, index)
                    .expect("reference table container");
                let bytes = decode_container(&container, Integrity::default(), &limits()).unwrap();
                let table = parse_reference_table(&bytes, &limits()).unwrap();
                self.tables.insert(index, table);
            }
            &self.tables[&index]
        }

        pub fn archive_ids(&mut self, index: u32) -> Vec<u32> {
            self.table(index).archives.iter().map(|a| a.id).collect()
        }

        pub fn file_ids(&mut self, index: u32, archive: u32) -> Vec<u32> {
            match self.table(index).archive(archive) {
                Some(entry) => entry.files.iter().map(|f| f.id).collect(),
                None => Vec::new(),
            }
        }

        pub fn file(&mut self, index: u32, archive: u32, file: u32) -> Option<Vec<u8>> {
            if !self.archives.contains_key(&(index, archive)) {
                let files = self.table(index).archive(archive)?.files.clone();
                let container = self.container(index, archive)?;
                let bytes = decode_container(&container, Integrity::default(), &limits()).ok()?;
                let split = split_archive(&bytes, &files, &limits()).ok()?;
                let map = split.into_iter().map(|f| (f.id, f.data)).collect();
                self.archives.insert((index, archive), map);
            }
            self.archives[&(index, archive)].get(&file).cloned()
        }

        pub fn drop_archive(&mut self, index: u32, archive: u32) {
            self.archives.remove(&(index, archive));
        }
    }
}

pub fn hex(bytes: &[u8]) -> String {
    bytes.iter().fold(String::new(), |mut out, byte| {
        write!(out, "{byte:02x}").unwrap();
        out
    })
}

pub fn digest(bytes: &[u8]) -> String {
    let mut hasher = Sha256::new();
    hasher.update(bytes);
    hex(&hasher.finalize())
}

pub fn short_digest(bytes: &[u8]) -> String {
    digest(bytes)[..16].to_string()
}

pub fn gunzip(compressed: &[u8], expected_sha256: &str) -> Vec<u8> {
    let mut data = Vec::new();
    GzDecoder::new(compressed).read_to_end(&mut data).unwrap();
    assert_eq!(digest(&data), expected_sha256.trim());
    data
}
