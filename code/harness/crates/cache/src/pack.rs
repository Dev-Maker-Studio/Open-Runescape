use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Mutex;

pub fn resolve_packed(path: PathBuf) -> PathBuf {
    if path.join("cache.json").is_file() || path.join(".packed").is_dir() { path.join(".packed") } else { path }
}

pub fn packed(root: &Path) -> PathBuf {
    resolve_packed(std::env::var_os("OPENRS_CACHE").map(PathBuf::from).unwrap_or_else(|| root.join("assets/cache/.packed")))
}

fn stamp_file(root: &Path) -> PathBuf {
    root.join("assets/cache/.packed.stamp")
}

fn mix(bytes: &[u8]) -> u64 {
    bytes.iter().fold(0xcbf2_9ce4_8422_2325u64, |h, &b| (h ^ b as u64).wrapping_mul(0x100_0000_01b3))
}

pub fn manifest(tree: &Path) -> Result<String, String> {
    let work = Mutex::new((vec![tree.to_path_buf()], 0usize));
    let (sum, files, err) = (AtomicU64::new(0), AtomicU64::new(0), Mutex::new(None::<String>));
    let threads = std::thread::available_parallelism().map_or(4, |n| n.get().min(8));
    std::thread::scope(|s| {
        for _ in 0..threads {
            s.spawn(|| loop {
                let dir = {
                    let mut w = work.lock().unwrap();
                    match w.0.pop() {
                        Some(d) => {
                            w.1 += 1;
                            d
                        }
                        None if w.1 == 0 => return,
                        None => {
                            drop(w);
                            std::thread::yield_now();
                            continue;
                        }
                    }
                };
                let (mut dirs, mut local, mut count) = (vec![], 0u64, 0u64);
                let entries = fs::read_dir(&dir);
                if let Err(x) = &entries {
                    err.lock().unwrap().get_or_insert(format!("{}: {x}", dir.display()));
                }
                for e in entries.into_iter().flatten().flatten() {
                    let path = e.path();
                    if path.file_name().is_some_and(|n| n == ".git" || n == ".packed" || n == ".packed.stamp") {
                        continue;
                    }
                    match e.metadata() {
                        Ok(meta) if meta.is_dir() => dirs.push(path),
                        Ok(meta) => {
                            let ns = meta.modified().ok().and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok()).map_or(0, |d| d.as_nanos());
                            local = local.wrapping_add(mix(format!("{}\0{}\0{ns}", path.strip_prefix(tree).unwrap_or(&path).display(), meta.len()).as_bytes()));
                            count += 1;
                        }
                        Err(x) => {
                            err.lock().unwrap().get_or_insert(x.to_string());
                        }
                    }
                }
                sum.fetch_add(local, Ordering::Relaxed);
                files.fetch_add(count, Ordering::Relaxed);
                let mut w = work.lock().unwrap();
                w.0.extend(dirs);
                w.1 -= 1;
            });
        }
    });
    err.into_inner().unwrap().map_or(Ok(format!("{}-{:016x}", files.into_inner(), sum.into_inner())), Err)
}

pub fn fresh(root: &Path) -> Result<bool, String> {
    let built = packed(root).join("main_file_cache.dat2").exists();
    Ok(built && fs::read_to_string(stamp_file(root)).ok().as_deref().map(str::trim) == Some(&manifest(&root.join("assets/cache"))?))
}

fn java_home() -> Option<PathBuf> {
    if let Some(h) = std::env::var_os("OPENRS_JAVA_HOME") {
        return Some(h.into());
    }
    fs::read_dir(PathBuf::from(std::env::var_os("HOME")?).join(".jdks")).ok()?.flatten().map(|e| e.path()).find(|p| p.file_name().is_some_and(|n| n.to_string_lossy().starts_with("jdk-26")))
}

pub fn build(root: &Path, force: bool) -> Result<bool, String> {
    if !force && fresh(root)? {
        return Ok(false);
    }
    let tree = root.join("assets/cache");
    let stamp = manifest(&tree)?;
    let out = packed(root);
    let mut cmd = Command::new("./gradlew");
    cmd.current_dir(root.join("code/server")).args([":tools:run", "-q", "--console=plain"]).arg(format!("--args=cache build {} {}", tree.display(), out.display())).env("DARKAN_CACHE_SOURCE", &tree).env("CACHE_SOURCE_PATH", &tree).env("CACHE_BUILD_PATH", &out);
    if let Some(h) = java_home() {
        cmd.env("JAVA_HOME", h);
    }
    if !cmd.status().map_err(|e| e.to_string())?.success() {
        return Err("gradle cache build failed".into());
    }
    fs::write(stamp_file(root), stamp).map_err(|e| e.to_string())?;
    Ok(true)
}
