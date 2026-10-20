use crate::store;
use std::fs::{self, File};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const STRIDE: u16 = 10;
pub const MAX: usize = 16;

pub const LOCAL_WORLD: &str = "100";

fn base(var: &str, default: u16) -> u16 {
    std::env::var(var).ok().and_then(|v| v.parse().ok()).unwrap_or(default)
}

fn shift(bases: [u16; 3], i: usize) -> [u16; 3] {
    bases.map(|p| p + STRIDE * i as u16)
}

pub fn ports(i: usize) -> [u16; 3] {
    shift([base("OPENRS_LOBBY_PORT", 24594), base("OPENRS_WORLD_PORT", 24595), base("OPENRS_API_PORT", 4140)], i)
}

fn tag(role: &str, i: usize) -> String {
    if i == 0 { role.into() } else { format!("{role}{i}") }
}

fn local() -> Result<PathBuf, String> {
    let local = store::root().parent().map(PathBuf::from).unwrap_or_default();
    fs::create_dir_all(&local).map_err(|e| e.to_string())?;
    Ok(local)
}

fn java_home() -> Option<PathBuf> {
    if let Some(h) = std::env::var_os("OPENRS_JAVA_HOME") {
        return Some(h.into());
    }
    let jdks = PathBuf::from(std::env::var_os("HOME")?).join(".jdks");
    fs::read_dir(jdks).ok()?.flatten().map(|e| e.path()).find(|p| p.file_name().is_some_and(|n| n.to_string_lossy().starts_with("jdk-26")))
}

fn alive(pid: &str) -> bool {
    PathBuf::from("/proc").join(pid).exists()
}

fn ours(role: &str, pid: &str) -> bool {
    fs::read(PathBuf::from("/proc").join(pid).join("cmdline")).is_ok_and(|c| String::from_utf8_lossy(&c).contains(&format!("code/server/{role}/build/install")))
}

fn pid_file(local: &Path, role: &str, i: usize) -> PathBuf {
    local.join(format!("{}.pid", tag(role, i)))
}

fn start(role: &str, ready: &str, local: &Path, i: usize) -> Result<(), String> {
    let repo = store::repo();
    let server = repo.join("code/server");
    let bin = server.join(format!("{role}/build/install/{role}/bin/{role}"));
    if !bin.exists() {
        return Err(format!("{} missing: run ./gradlew :{role}:installDist in code/server with DARKAN_CACHE_SOURCE={}", bin.display(), repo.join("assets/cache").display()));
    }
    let [lobby_port, world_port, api_port] = ports(i).map(|p| p.to_string());
    let log_path = local.join(format!("{}.log", tag(role, i)));
    let log = File::create(&log_path).map_err(|e| e.to_string())?;
    let mut cmd = Command::new(&bin);
    cmd.current_dir(&server)
        .env("PLAYER_FILE_DIR", store::root())
        .env("CACHE_SOURCE_PATH", repo.join("assets/cache"))
        .env("CACHE_BUILD_PATH", repo.join("assets/cache-packed"))
        .env("LOBBY_PORT", &lobby_port)
        .env("WORLD_PORT", &world_port)
        .env("LOBBY_API_PORT", &api_port)
        .env("SOCIAL_GATEWAY_URL", format!("ws://localhost:{api_port}/social/ws"))
        .env("DEBUG", "true")
        .env("AUTO_UPDATE", "false")
        .env("BOT_IP_WHITELIST", "127.0.0.1,::1")
        .env("MULTILOG_WHITELIST", "localhost,127.0.0.1,::1")
        .env("WORLD_ID", LOCAL_WORLD)
        .env("WORLD_BOTS_ALLOWED", "true")
        .env("MEM_CACHE", if role == "lobby" { "true" } else { "false" })
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log);
    if let Some(h) = java_home() {
        cmd.env("JAVA_HOME", h);
    }
    let child = cmd.spawn().map_err(|e| e.to_string())?;
    fs::write(pid_file(local, role, i), child.id().to_string()).map_err(|e| e.to_string())?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(900) {
        if !alive(&child.id().to_string()) {
            return Err(format!("{role} exited, see {}", log_path.display()));
        }
        if fs::read_to_string(&log_path).is_ok_and(|s| s.contains(ready)) {
            println!("instance {i} {role} ready in {:.1}s (pid {})", t.elapsed().as_secs_f32(), child.id());
            return Ok(());
        }
        std::thread::sleep(Duration::from_millis(500));
    }
    Err(format!("{role} not ready, see {}", log_path.display()))
}

fn pids(local: &Path, i: usize) -> Vec<(&'static str, String)> {
    ["world", "lobby"]
        .into_iter()
        .filter_map(|r| fs::read_to_string(pid_file(local, r, i)).ok().map(|p| (r, p.trim().to_string())))
        .collect()
}

pub fn start_instance(i: usize) -> Result<bool, String> {
    let local = local()?;
    if pids(&local, i).iter().any(|(r, p)| ours(r, p)) {
        println!("instance {i} already running");
        return Ok(false);
    }
    start("lobby", "All services started", &local, i)?;
    start("world", "World server started", &local, i)?;
    let [lobby, world, _] = ports(i);
    println!("instance {i}: client localhost {lobby} {world}, world 100");
    Ok(true)
}

pub fn stop_instance(i: usize) -> Result<(), String> {
    let local = local()?;
    for (role, pid) in pids(&local, i) {
        if ours(role, &pid) {
            Command::new("kill").arg(&pid).status().map_err(|e| e.to_string())?;
            println!("stopped instance {i} {role} {pid}");
        }
        let _ = fs::remove_file(pid_file(&local, role, i));
    }
    Ok(())
}

pub fn serve(a: &[String]) -> Result<(), String> {
    let local = local()?;
    let known: Vec<usize> = (0..MAX).filter(|&i| !pids(&local, i).is_empty()).collect();
    match a.first().map(String::as_str) {
        Some("stop") => match a.get(1) {
            Some(i) => stop_instance(i.parse().map_err(|_| format!("bad instance {i}"))?),
            None => known.into_iter().try_for_each(stop_instance),
        },
        Some("status") => {
            for i in known {
                let [lobby, world, http] = ports(i);
                for (role, pid) in pids(&local, i) {
                    println!("instance {i} {role} {pid} {} lobby {lobby} world {world} http {http}", if ours(role, &pid) { "running" } else { "dead" });
                }
            }
            Ok(())
        }
        n => {
            let n: usize = n.map_or(Ok(1), |v| v.parse().map_err(|_| format!("unknown serve action {v}")))?;
            if !(1..=MAX).contains(&n) {
                return Err(format!("instance count must be 1 to {MAX}"));
            }
            (0..n).try_for_each(|i| start_instance(i).map(|_| ()))
        }
    }
}

pub fn account(name: &str, password: &str) -> Result<(), String> {
    use std::io::{Read, Write};
    let body = serde_json::json!({"username": name, "email": format!("{}@local.test", store::key(name)), "password": password}).to_string();
    let key = std::env::var("OPENRS_API_KEY").unwrap_or_else(|_| "TEST_API_KEY".into());
    let mut s = std::net::TcpStream::connect(("127.0.0.1", ports(0)[2])).map_err(|e| e.to_string())?;
    write!(s, "POST /accounts HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\nX-API-Key: {key}\r\nContent-Type: application/json\r\nContent-Length: {}\r\n\r\n{body}", body.len()).map_err(|e| e.to_string())?;
    let mut out = String::new();
    s.read_to_string(&mut out).map_err(|e| e.to_string())?;
    let status = out.lines().next().unwrap_or("");
    if status.contains(" 200 ") { Ok(()) } else { Err(status.to_string()) }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn instance_ports_step_by_stride() {
        assert_eq!(shift([24594, 24595, 4140], 0), [24594, 24595, 4140]);
        assert_eq!(shift([24594, 24595, 4140], 1), [24604, 24605, 4150]);
        assert_eq!(shift([24594, 24595, 4140], 2), [24614, 24615, 4160]);
    }

    #[test]
    fn instance_ports_never_collide() {
        let mut all: Vec<u16> = (0..MAX).flat_map(|i| shift([24594, 24595, 4140], i)).collect();
        all.sort_unstable();
        all.dedup();
        assert_eq!(all.len(), MAX * 3);
    }

    #[test]
    fn default_ports_stay_below_the_ephemeral_range() {
        let ports = ports(0);
        assert!(ports.iter().all(|p| *p < 32768), "{ports:?}");
        let top = (0..MAX).flat_map(|i| shift([24594, 24595, 4140], i)).max().unwrap();
        assert!(top < 32768, "{top}");
    }

    #[test]
    fn instance_zero_keeps_legacy_file_names() {
        assert_eq!(tag("lobby", 0), "lobby");
        assert_eq!(tag("world", 2), "world2");
        assert_eq!(pid_file(Path::new("/l"), "world", 1), PathBuf::from("/l/world1.pid"));
    }
}
