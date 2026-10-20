use std::process::Command;
use std::time::{Duration, Instant};

pub fn state(name: &str) -> Option<String> {
    std::fs::read_to_string(display::dir().join(name)).ok().map(|s| s.trim().to_string())
}

pub fn pid(name: &str, needle: &str) -> Option<u32> {
    let pid: u32 = state(name)?.split_whitespace().last()?.parse().ok()?;
    std::fs::read(format!("/proc/{pid}/cmdline")).ok().filter(|c| String::from_utf8_lossy(c).contains(needle)).map(|_| pid)
}

pub fn mem(pid: u32) -> (u64, u64) {
    let t = std::fs::read_to_string(format!("/proc/{pid}/smaps_rollup")).unwrap_or_default();
    let f = |k: &str| t.lines().find_map(|l| l.strip_prefix(k)?.split_whitespace().next()?.parse::<u64>().ok()).unwrap_or(0) / 1024;
    (f("Rss:"), f("Pss:"))
}

pub fn ticks(pid: u32) -> u64 {
    let s = std::fs::read_to_string(format!("/proc/{pid}/stat")).unwrap_or_default();
    let f: Vec<&str> = s.rsplit_once(')').map_or("", |x| x.1).split_whitespace().collect();
    f.get(11).and_then(|v| v.parse::<u64>().ok()).unwrap_or(0) + f.get(12).and_then(|v| v.parse::<u64>().ok()).unwrap_or(0)
}

pub fn alive(pid: u32) -> bool {
    std::path::Path::new(&format!("/proc/{pid}")).exists()
}

pub fn kill(pid: u32) {
    let _ = Command::new("kill").arg(pid.to_string()).status();
    let t = Instant::now();
    while alive(pid) && t.elapsed() < Duration::from_secs(10) {
        std::thread::sleep(Duration::from_millis(100));
    }
    if alive(pid) {
        let _ = Command::new("kill").args(["-9", &pid.to_string()]).status();
    }
}
