use std::collections::HashMap;
use std::process::Command;
use std::time::{Duration, Instant};

fn stat(pid: u32) -> Vec<String> {
    let s = std::fs::read_to_string(format!("/proc/{pid}/stat")).unwrap_or_default();
    s.rsplit_once(')').map_or("", |x| x.1).split_whitespace().map(String::from).collect()
}

pub fn alive(pid: u32) -> bool {
    stat(pid).first().is_some_and(|s| s != "Z")
}

pub fn group(leader: u32) -> Vec<u32> {
    std::fs::read_dir("/proc")
        .into_iter()
        .flatten()
        .flatten()
        .filter_map(|e| e.file_name().to_str()?.parse::<u32>().ok())
        .filter(|p| stat(*p).get(2).is_some_and(|g| g.parse() == Ok(leader)))
        .collect()
}

pub fn mem(pid: u32) -> (u64, u64) {
    let t = std::fs::read_to_string(format!("/proc/{pid}/smaps_rollup")).unwrap_or_default();
    let f = |k: &str| t.lines().find_map(|l| l.strip_prefix(k)?.split_whitespace().next()?.parse::<u64>().ok()).unwrap_or(0) / 1024;
    (f("Rss:"), f("Pss:"))
}

pub fn ticks(pid: u32) -> u64 {
    let s = stat(pid);
    (11..13).filter_map(|i| s.get(i)?.parse::<u64>().ok()).sum()
}

pub fn gpu() -> HashMap<u32, u64> {
    let out = Command::new("nvidia-smi").args(["--query-compute-apps=pid,used_memory", "--format=csv,noheader,nounits"]).output().map(|o| String::from_utf8_lossy(&o.stdout).into_owned()).unwrap_or_default();
    parse_gpu(&out)
}

pub fn parse_gpu(text: &str) -> HashMap<u32, u64> {
    text.lines().filter_map(|l| {
        let (pid, mib) = l.split_once(',')?;
        Some((pid.trim().parse().ok()?, mib.trim().parse().ok()?))
    }).collect()
}

pub fn gpu_used() -> u64 {
    Command::new("nvidia-smi").args(["--query-gpu=memory.used", "--format=csv,noheader,nounits"]).output().ok().and_then(|o| String::from_utf8_lossy(&o.stdout).trim().parse().ok()).unwrap_or(0)
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
