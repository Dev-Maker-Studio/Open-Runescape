use crate::{crc, mismatch, pack, parse_revision};
use std::path::{Path, PathBuf};

fn tmp(name: &str) -> PathBuf {
    let d = std::env::temp_dir().join(format!("openrs-cache-{}-{name}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::fs::create_dir_all(&d).unwrap();
    d
}

fn write(p: &Path, data: &[u8]) {
    std::fs::create_dir_all(p.parent().unwrap()).unwrap();
    std::fs::write(p, data).unwrap();
}

#[test]
fn crc32_matches_the_standard_check_value() {
    assert_eq!(crc::crc32(b"123456789") as u32, 0xCBF4_3926);
    assert_eq!(crc::crc32(b""), 0);
}

#[test]
fn crc_table_reads_chained_sectors() {
    let dir = tmp("table");
    let a: Vec<u8> = (0..10).collect();
    let b: Vec<u8> = (0..600u32).map(|i| (i * 7) as u8).collect();
    let mut dat = vec![0u8; 520 * 4];
    let mut sector = |n: usize, next: u8, data: &[u8]| {
        dat[n * 520 + 6] = next;
        dat[n * 520 + 8..n * 520 + 8 + data.len()].copy_from_slice(data);
    };
    sector(1, 0, &a);
    sector(2, 3, &b[..512]);
    sector(3, 0, &b[512..]);
    dat.truncate(3 * 520 + 8 + 88);
    let entry = |size: u32, sector: u32| [&size.to_be_bytes()[1..], &sector.to_be_bytes()[1..]].concat();
    let idx = [entry(10, 1), entry(0, 0), entry(600, 2), entry(5, 9), entry(600, 3)].concat();
    write(&dir.join("main_file_cache.dat2"), &dat);
    write(&dir.join("main_file_cache.idx255"), &idx);
    assert_eq!(crc::table(&dir).unwrap(), vec![crc::crc32(&a), 0, crc::crc32(&b), 0, 0]);
    assert!(crc::table(&tmp("empty")).unwrap_err().contains("empty"));
}

#[test]
fn mismatch_lists_differing_indices() {
    assert_eq!(mismatch(&[1, 2, 3], &[1, 2, 3]), None);
    assert_eq!(mismatch(&[1, 2, 3, 4], &[1, 2, 3]), None);
    let m = mismatch(&[1, 9, 3], &[1, 2, 3, 4]).unwrap();
    assert!(m.starts_with("cache mismatch: local "), "{m}");
    assert!(m.ends_with(" (indices 1,3), run openrs cache build"), "{m}");
    let digest = |t: &[i32]| format!("{:08x}", crc::crc32(&t.iter().flat_map(|c| c.to_be_bytes()).collect::<Vec<_>>()));
    assert!(m.contains(&format!("local {} vs lobby {}", digest(&[1, 9, 3]), digest(&[1, 2, 3, 4]))), "{m}");
}

#[test]
fn revision_from_cache_json() {
    assert_eq!(parse_revision(r#"{"name":"x","revision": 727, "a":1}"#), Some(727));
    assert_eq!(parse_revision(r#"{"revision":730}"#), Some(730));
    assert_eq!(parse_revision(r#"{"name":"x"}"#), None);
}

#[test]
fn manifest_tracks_files_and_ignores_git() {
    let tree = tmp("manifest");
    write(&tree.join("a.dat"), b"one");
    write(&tree.join("sub/b.dat"), b"two");
    let first = pack::manifest(&tree).unwrap();
    assert!(first.starts_with("2-"), "{first}");
    assert_eq!(pack::manifest(&tree).unwrap(), first);
    write(&tree.join(".git/HEAD"), b"ref");
    write(&tree.join(".packed/main_file_cache.dat2"), b"generated packed cache");
    write(&tree.join(".packed.stamp"), b"generated manifest");
    assert_eq!(pack::manifest(&tree).unwrap(), first);
    write(&tree.join(".packed/main_file_cache.idx255"), b"rebuilt reference table");
    assert_eq!(pack::manifest(&tree).unwrap(), first);
    write(&tree.join("sub/b.dat"), b"three");
    let second = pack::manifest(&tree).unwrap();
    assert_ne!(second, first);
    write(&tree.join("c.dat"), b"");
    assert!(pack::manifest(&tree).unwrap().starts_with("3-"));
    assert!(pack::manifest(&tree.join("missing")).is_err());
}

#[test]
fn freshness_needs_packed_cache_and_matching_stamp() {
    std::env::remove_var("OPENRS_CACHE");
    let root = tmp("fresh");
    write(&root.join("assets/cache/idx/0.dat"), b"data");
    let stamp = root.join("assets/cache/.packed.stamp");
    assert!(!pack::fresh(&root).unwrap());
    write(&stamp, format!("{}\n", pack::manifest(&root.join("assets/cache")).unwrap()).as_bytes());
    assert!(!pack::fresh(&root).unwrap());
    write(&root.join("assets/cache/.packed/main_file_cache.dat2"), b"");
    assert!(pack::fresh(&root).unwrap());
    assert_eq!(crate::check(&root, false), Ok(()));
    write(&root.join("assets/cache/idx/1.dat"), b"more");
    assert!(!pack::fresh(&root).unwrap());
    assert_eq!(crate::check(&root, true).unwrap_err(), "cache mismatch: assets/cache/.packed is stale or missing, run openrs cache build");
}

#[test]
fn packed_path_accepts_a_source_root_or_a_sector_directory() {
    let root = tmp("resolve");
    write(&root.join("cache.json"), b"{}");
    assert_eq!(pack::resolve_packed(root.clone()), root.join(".packed"));
    let packed = root.join(".packed");
    write(&packed.join("main_file_cache.dat2"), b"cache");
    assert_eq!(pack::resolve_packed(packed.clone()), packed);
}
