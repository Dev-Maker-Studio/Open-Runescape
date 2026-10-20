use std::net::{TcpListener, TcpStream};
use std::path::Path;
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const PORT: u16 = 8091;

pub fn free_port(from: u16) -> Option<u16> {
    (from..from.saturating_add(100)).find(|p| TcpListener::bind(("127.0.0.1", *p)).is_ok())
}

pub fn start(root: &Path, files: &Path) -> Result<(u32, u16), String> {
    use std::os::unix::process::CommandExt;
    let port = match std::env::var("OPENRS_WEB_PORT").ok().and_then(|p| p.parse().ok()) {
        Some(p) => p,
        None => free_port(PORT).ok_or("no free port from 8091")?,
    };
    let exe = fresh::server(root);
    let log = std::fs::File::create(crate::state::dir().join("server.log")).map_err(|e| e.to_string())?;
    std::fs::create_dir_all(files).map_err(|e| e.to_string())?;
    let child = Command::new(&exe)
        .env("PORT", port.to_string())
        .env("HOST", "127.0.0.1")
        .env("STATIC_DIR", root.join("code/web/static"))
        .env("FILES_DIR", files)
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log)
        .process_group(0)
        .spawn()
        .map_err(|e| format!("{}: {e}", exe.display()))?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(5) {
        if TcpStream::connect(("127.0.0.1", port)).is_ok() {
            return Ok((child.id(), port));
        }
        std::thread::sleep(Duration::from_millis(20));
    }
    Err(format!("openrs-web did not listen on {port}"))
}
