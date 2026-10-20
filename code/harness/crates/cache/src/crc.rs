use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use std::path::Path;

const SECTOR: u64 = 520;

pub fn crc32(data: &[u8]) -> i32 {
    let mut crc = !0u32;
    for &b in data {
        crc ^= b as u32;
        for _ in 0..8 {
            crc = (crc >> 1) ^ (0xEDB8_8320 & (crc & 1).wrapping_neg());
        }
    }
    !crc as i32
}

fn container(idx: &[u8], dat: &mut File, id: usize) -> Option<Vec<u8>> {
    let entry = idx.get(id * 6..id * 6 + 6)?;
    let size = u32::from_be_bytes([0, entry[0], entry[1], entry[2]]) as usize;
    let mut sector = u32::from_be_bytes([0, entry[3], entry[4], entry[5]]) as u64;
    if size == 0 || sector == 0 {
        return None;
    }
    let mut out = Vec::with_capacity(size);
    let mut block = [0u8; SECTOR as usize];
    while out.len() < size {
        dat.seek(SeekFrom::Start(sector * SECTOR)).ok()?;
        let got = dat.read(&mut block).ok()?;
        let count = (size - out.len()).min(SECTOR as usize - 8);
        if got < 8 + count {
            return None;
        }
        out.extend_from_slice(&block[8..8 + count]);
        sector = u32::from_be_bytes([0, block[4], block[5], block[6]]) as u64;
    }
    Some(out)
}

pub fn table(dir: &Path) -> Result<Vec<i32>, String> {
    let idx = std::fs::read(dir.join("main_file_cache.idx255")).map_err(|e| format!("{}: {e}", dir.display()))?;
    let mut dat = File::open(dir.join("main_file_cache.dat2")).map_err(|e| e.to_string())?;
    Ok((0..idx.len() / 6).map(|i| container(&idx, &mut dat, i).map_or(0, |c| crc32(&c))).collect())
}
