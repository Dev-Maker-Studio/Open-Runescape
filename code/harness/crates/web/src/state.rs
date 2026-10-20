use crate::launch::Browser;
use std::path::PathBuf;

pub fn dir() -> PathBuf {
    let d = std::env::var_os("OPENRS_WEB_STATE").map(PathBuf::from).unwrap_or_else(|| PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join("openrs-web"));
    let _ = std::fs::create_dir_all(&d);
    d
}

pub fn profile() -> PathBuf {
    dir().join("profile")
}

pub fn files() -> PathBuf {
    dir().join("files")
}

pub fn read(name: &str) -> Option<Vec<String>> {
    std::fs::read_to_string(dir().join(name)).ok().map(|s| s.split_whitespace().map(String::from).collect())
}

pub fn write(name: &str, v: &str) {
    let _ = std::fs::write(dir().join(name), v);
}

pub fn remove(name: &str) {
    let _ = std::fs::remove_file(dir().join(name));
}

pub fn alive(pid: u32, needle: &str) -> bool {
    std::fs::read(format!("/proc/{pid}/cmdline")).is_ok_and(|c| String::from_utf8_lossy(&c).contains(needle))
}

pub fn browser() -> Option<Browser> {
    let f = read("browser")?;
    let b = Browser { pid: f.first()?.parse().ok()?, port: f.get(1)?.parse().ok()?, ws: f.get(2)?.clone() };
    alive(b.pid, &profile().display().to_string()).then_some(b)
}

pub fn server() -> Option<(u32, u16)> {
    let f = read("server")?;
    let (pid, port) = (f.first()?.parse().ok()?, f.get(1)?.parse().ok()?);
    alive(pid, "openrs-web").then_some((pid, port))
}

pub fn crashed() -> bool {
    read("browser").is_some() && browser().is_none()
}
