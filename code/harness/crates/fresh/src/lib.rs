use std::path::{Path, PathBuf};
use std::process::Command;
use std::time::{Instant, SystemTime};

const SOURCES: [&str; 4] = ["src", "build.gradle.kts", "settings.gradle", "gradle.properties"];

pub fn jar(root: &Path) -> PathBuf {
    root.join("code/client/build/libs/darkan-client-shaded-2.0.0-all.jar")
}

pub fn newest(p: &Path) -> Option<SystemTime> {
    let m = std::fs::symlink_metadata(p).ok()?;
    let own = m.modified().ok();
    if !m.is_dir() {
        return own;
    }
    std::fs::read_dir(p).ok()?.flatten().filter_map(|e| newest(&e.path())).chain(own).max()
}

pub fn stale(built: Option<SystemTime>, source: Option<SystemTime>) -> Option<&'static str> {
    match built {
        None => Some("missing"),
        Some(b) if source.is_some_and(|s| s > b) => Some("older than the newest client source"),
        _ => None,
    }
}

pub fn jar_stale(root: &Path) -> Option<&'static str> {
    let client = root.join("code/client");
    stale(std::fs::metadata(jar(root)).and_then(|m| m.modified()).ok(), SOURCES.iter().filter_map(|s| newest(&client.join(s))).max())
}

pub fn clients() -> Vec<u32> {
    std::fs::read_dir("/proc")
        .into_iter()
        .flatten()
        .flatten()
        .filter_map(|e| {
            let pid = e.file_name().to_str()?.parse().ok()?;
            let c = std::fs::read(e.path().join("cmdline")).ok()?;
            let c = String::from_utf8_lossy(&c);
            (c.contains("-jar\0") && (c.contains("darkan-client-shaded") || c.contains("equiv/client.jar"))).then_some(pid)
        })
        .collect()
}

fn timed(what: &str, why: &str, f: impl FnOnce() -> Result<(), String>) -> Result<(), String> {
    let t = Instant::now();
    f()?;
    println!("rebuilt {what} ({why}) in {:.1}s", t.elapsed().as_secs_f32());
    Ok(())
}

fn gradle(root: &Path) -> Result<(), String> {
    let ok = Command::new("./gradlew").current_dir(root.join("code/client")).args(["shadowJar", "-q", "--console=plain"]).status().map_err(|e| format!("gradlew: {e}"))?.success();
    if ok { Ok(()) } else { Err("gradle shadowJar failed".into()) }
}

pub fn renderer(root: &Path) -> PathBuf {
    root.join("code/renderer/target/release/renderer-app")
}

pub fn renderer_stale(root: &Path) -> Option<&'static str> {
    let r = root.join("code/renderer");
    stale(std::fs::metadata(renderer(root)).and_then(|m| m.modified()).ok(), ["src", "shaders", "Cargo.toml"].iter().filter_map(|s| newest(&r.join(s))).max())
}

fn cargo(root: &Path) -> Result<(), String> {
    let ok = Command::new("cargo").current_dir(root.join("code/renderer")).args(["build", "--release", "-q", "--bin", "renderer-app"]).status().map_err(|e| format!("cargo: {e}"))?.success();
    if ok { Ok(()) } else { Err("cargo build of code/renderer failed".into()) }
}

fn modified(p: &Path) -> Option<SystemTime> {
    std::fs::metadata(p).and_then(|m| m.modified()).ok()
}

fn exec(dir: &Path, prog: &str, args: &[&str]) -> Result<(), String> {
    let ok = Command::new(prog).current_dir(dir).args(args).status().map_err(|e| format!("{prog}: {e}"))?.success();
    if ok { Ok(()) } else { Err(format!("{prog} {} failed in {}", args.join(" "), dir.display())) }
}

pub fn wasm(root: &Path) -> PathBuf {
    root.join("code/web/static/renderer_bg.wasm")
}

pub fn wasm_stale(root: &Path) -> Option<&'static str> {
    let r = root.join("code/renderer");
    stale(modified(&wasm(root)), ["src", "shaders", "Cargo.toml"].iter().filter_map(|s| newest(&r.join(s))).max())
}

pub fn server(root: &Path) -> PathBuf {
    root.join("code/web/target/release/openrs-web")
}

pub fn server_stale(root: &Path) -> Option<&'static str> {
    let w = root.join("code/web");
    stale(modified(&server(root)), ["src", "Cargo.toml"].iter().filter_map(|s| newest(&w.join(s))).max())
}

pub fn web(root: &Path) -> Result<(), String> {
    if let Some(why) = wasm_stale(root) {
        timed("renderer wasm", why, || exec(&root.join("code/renderer"), "./build-web.sh", &[]))?;
    }
    if let Some(why) = server_stale(root) {
        timed("web server", why, || exec(&root.join("code/web"), "cargo", &["build", "--release", "-q"]))?;
    }
    Ok(())
}

pub fn ensure(root: &Path) -> Result<(), String> {
    if let Some(why) = renderer_stale(root) {
        timed("renderer", why, || cargo(root))?;
    }
    let busy = clients().len();
    let held = |what: &str, why: &str| println!("{what} {why}, not rebuilt while {busy} client(s) use it");
    if let Some(why) = jar_stale(root) {
        if busy > 0 { held("client jar", why) } else { timed("client jar", why, || gradle(root))? }
    }
    if root.join("assets/cache").is_dir() && !cache::fresh(root)? {
        let why = "stale or missing";
        if busy > 0 { held("packed cache", why) } else { timed("packed cache", why, || cache::build(root, false).map(drop))? }
    }
    Ok(())
}

pub fn strip(args: &[String]) -> (Vec<String>, bool) {
    (args.iter().filter(|a| *a != "--no-build").cloned().collect(), args.iter().any(|a| a == "--no-build"))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Duration;

    #[test]
    fn stale_rules() {
        let t = SystemTime::UNIX_EPOCH + Duration::from_secs(100);
        assert_eq!(stale(None, Some(t)), Some("missing"));
        assert_eq!(stale(Some(t), Some(t + Duration::from_secs(1))), Some("older than the newest client source"));
        assert_eq!(stale(Some(t), Some(t)), None);
        assert_eq!(stale(Some(t), None), None);
    }

    #[test]
    fn newest_walks_tree() {
        let d = std::env::temp_dir().join(format!("fresh-{}", std::process::id()));
        std::fs::create_dir_all(d.join("a/b")).unwrap();
        std::fs::write(d.join("a/b/f"), "x").unwrap();
        let f = std::fs::metadata(d.join("a/b/f")).unwrap().modified().unwrap();
        assert!(newest(&d).unwrap() >= f);
        assert!(newest(&d.join("missing")).is_none());
        std::fs::remove_dir_all(d).unwrap();
    }

    #[test]
    fn strip_no_build() {
        let a: Vec<String> = ["up", "--no-build", "shared"].map(String::from).to_vec();
        assert_eq!(strip(&a), (vec!["up".to_string(), "shared".to_string()], true));
        assert!(!strip(&a[..1]).1);
    }
}
