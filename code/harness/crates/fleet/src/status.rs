use crate::config::Cfg;
use crate::{proc, up};
use control::Conn;
use serde_json::Value;
use std::time::Duration;

fn query(n: usize) -> (String, String, String) {
    let mut c = match Conn::open_at(&up::sock(n).display().to_string()) {
        Ok(c) => c,
        Err(_) => return ("-".into(), "-".into(), "-".into()),
    };
    let s = c.send("state", &[]).unwrap_or(Value::Null);
    let p = c.send("pos", &[]).unwrap_or(Value::Null);
    (s["gameState"].as_str().unwrap_or("?").into(), s["tick"].to_string(), format!("{} {} {}", p["x"], p["y"], p["z"]))
}

pub fn status() -> Result<(), String> {
    let cfg = Cfg::load()?;
    let live: Vec<(usize, u32)> = (1..=9).filter_map(|n| proc::pid(&format!("slot{n}"), "darkan-client").map(|p| (n, p))).collect();
    let probes: Vec<_> = live.iter().map(|&(n, _)| std::thread::spawn(move || query(n))).collect();
    let before: Vec<u64> = live.iter().map(|&(_, p)| proc::ticks(p)).collect();
    std::thread::sleep(Duration::from_secs(1));
    println!("slot server role account pid state tick pos rss_mb pss_mb cpu_pct");
    let (mut rss, mut pss, mut cpu) = (0, 0, 0);
    for ((&(n, pid), probe), b) in live.iter().zip(probes).zip(before) {
        let (state, tick, pos) = probe.join().unwrap_or_default();
        let (r, p) = proc::mem(pid);
        let c = proc::ticks(pid).saturating_sub(b);
        let s = cfg.slot(n)?;
        println!("{n} {} {} {} {pid} {state} {tick} {pos} {r} {p} {c}", s.target(), s.role, s.account);
        (rss, pss, cpu) = (rss + r, pss + p, cpu + c);
    }
    let x = proc::pid("xserver", "Xwayland").map_or((0, 0), proc::mem);
    println!("clients {} rss_mb {rss} pss_mb {pss} cpu_pct {cpu} xwayland_rss_mb {} total_pss_mb {}", live.len(), x.0, pss + x.1);
    Ok(())
}
