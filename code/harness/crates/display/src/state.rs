use std::path::PathBuf;

pub fn dir() -> PathBuf {
    let d = std::env::var_os("OPENRS_STATE").map(PathBuf::from).unwrap_or_else(|| PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join("openrs"));
    let _ = std::fs::create_dir_all(&d);
    d
}

pub fn read(name: &str) -> Option<String> {
    std::fs::read_to_string(dir().join(name)).ok().map(|s| s.trim().to_string())
}

pub fn write(name: &str, v: &str) {
    let _ = std::fs::write(dir().join(name), v);
}

pub fn alive(pid: u32) -> bool {
    std::fs::read(format!("/proc/{pid}/cmdline")).is_ok_and(|c| {
        let c = String::from_utf8_lossy(&c);
        c.contains("darkan-client") || c.contains("Xwayland")
    })
}

pub fn root() -> Result<PathBuf, String> {
    if let Ok(r) = std::env::var("OPENRS_ROOT") {
        return Ok(r.into());
    }
    let mut p = std::env::current_dir().map_err(|e| e.to_string())?;
    loop {
        if p.join("_APP-SPECIFICATION.md").exists() {
            return Ok(p);
        }
        if !p.pop() {
            return Err("project root not found, set OPENRS_ROOT".into());
        }
    }
}
