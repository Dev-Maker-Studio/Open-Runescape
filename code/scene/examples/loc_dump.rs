use openrs_assets::{decode_container, decrypt_container, Integrity, Limits};
use openrs_scene::loc_type::{canonical, decode_loc_type};
use openrs_scene::locs::decode_region_locs;
use std::fmt::Write as _;
use std::fs;
use std::path::Path;

fn payload(container: &[u8]) -> Vec<u8> {
    decode_container(container, Integrity::default(), &Limits::default()).expect("container")
}

fn placements(work: &Path, regions: &[&str]) -> String {
    let mut out = String::new();
    for region in regions {
        for kind in ["l", "ul"] {
            let path = work.join(format!("{kind}{region}.container"));
            if !path.exists() {
                continue;
            }
            let data = payload(&fs::read(&path).expect("read"));
            writeln!(out, "{region}\t{kind}\tPAYLOAD\t{}", data.len()).unwrap();
            let decoded = decode_region_locs(&data);
            let (rows, status) = match decoded {
                Ok(rows) => (rows, "ok"),
                Err(_) => (Vec::new(), "error"),
            };
            for row in &rows {
                writeln!(
                    out,
                    "{region}\t{kind}\t{}\t{}\t{}\t{}\t{}\t{}",
                    row.id, row.plane, row.local_x, row.local_y, row.kind, row.rotation
                )
                .unwrap();
            }
            writeln!(out, "{region}\t{kind}\tEND\t{}\t{status}", rows.len()).unwrap();
        }
    }
    out
}

fn keys(native: &str) -> Vec<[i32; 4]> {
    let mut keys = Vec::new();
    for line in native.lines() {
        let fields: Vec<&str> = line.split('\t').collect();
        if fields[0] == "KEY" {
            let mut key = [0i32; 4];
            for (slot, value) in key.iter_mut().zip(&fields[2..6]) {
                *slot = value.parse().expect("key");
            }
            keys.push(key);
        }
    }
    keys
}

fn xtea(work: &Path, regions: &[&str]) -> String {
    let mut out = String::new();
    let native = fs::read_to_string(work.join("native-xtea.tsv")).expect("native xtea");
    let keys = keys(&native);
    for region in regions {
        let original = work.join(format!("l{region}.container"));
        if !original.exists() {
            continue;
        }
        let original = fs::read(original).expect("read");
        let expected = payload(&original);
        for (index, key) in keys.iter().enumerate() {
            let encrypted =
                fs::read(work.join(format!("xtea-l{region}-{index}.bin"))).expect("read");
            let restored = decrypt_container(&encrypted, key);
            let decoded = payload(&restored) == expected;
            writeln!(
                out,
                "XTEA\t{region}\t{index}\t{}\trestored={}\tdecoded={decoded}",
                encrypted.len(),
                restored == original
            )
            .unwrap();
        }
    }
    out
}

fn definitions(work: &Path) -> Option<String> {
    let bundle = fs::read(work.join("defs.bundle")).ok()?;
    let mut at = 0usize;
    let mut read = |count: usize| {
        let slice = &bundle[at..at + count];
        at += count;
        slice
    };
    let total = i32::from_be_bytes(read(4).try_into().unwrap());
    let mut out = String::new();
    for _ in 0..total {
        let id = i32::from_be_bytes(read(4).try_into().unwrap());
        let length = i32::from_be_bytes(read(4).try_into().unwrap());
        let data = (length >= 0).then(|| read(length as usize).to_vec());
        for member in [true, false] {
            let line = match decode_loc_type(id, data.as_deref(), member) {
                Ok(loc) => canonical(&loc),
                Err(_) => "error".to_string(),
            };
            writeln!(out, "{id}\t{}\t{line}", i32::from(member)).unwrap();
        }
    }
    Some(out)
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let work = Path::new(&args[1]);
    let regions: Vec<&str> = args[2].split(',').filter(|v| !v.is_empty()).collect();
    fs::write(work.join("rust-locs.tsv"), placements(work, &regions)).expect("write");
    fs::write(work.join("rust-xtea.tsv"), xtea(work, &regions)).expect("write");
    if let Some(text) = definitions(work) {
        fs::write(work.join("rust-defs.tsv"), text).expect("write");
    }
}
