use crate::fleet::{self, MAX};
use crate::{page::Page, proc, state};
use std::collections::HashMap;
use std::time::Duration;

struct Row {
    n: usize,
    pid: u32,
    ws: String,
    procs: Vec<u32>,
}

fn probe(ws: &str) -> (String, String, String) {
    match Page::at(ws).and_then(|mut p| p.run("state", &[])) {
        Ok(s) => (if s["ready"] == true { "ready" } else { "loading" }.into(), s["frames"].to_string(), s["frameMs"].to_string()),
        Err(_) => ("-".into(), "-".into(), "-".into()),
    }
}

fn total(procs: &[u32], f: impl Fn(u32) -> u64) -> u64 {
    procs.iter().map(|p| f(*p)).sum()
}

pub fn status() -> Result<(), String> {
    let rows: Vec<Row> = (1..=MAX).filter(|_| fleet::dir().is_dir()).filter_map(|n| {
        fleet::select(n);
        state::browser().map(|b| Row { n, pid: b.pid, ws: b.ws, procs: proc::group(b.pid) })
    }).collect();
    fleet::shared();
    let probes: Vec<_> = rows.iter().map(|r| { let ws = r.ws.clone(); std::thread::spawn(move || probe(&ws)) }).collect();
    let before: Vec<u64> = rows.iter().map(|r| total(&r.procs, proc::ticks)).collect();
    std::thread::sleep(Duration::from_secs(1));
    let gpu: HashMap<u32, u64> = proc::gpu();
    println!("slot pid state frames frame_ms procs rss_mb pss_mb cpu_pct gpu_mb");
    let (mut rss, mut pss, mut cpu, mut mib) = (0, 0, 0, 0);
    for ((r, p), b) in rows.iter().zip(probes).zip(before) {
        let (state, frames, ms) = p.join().unwrap_or_default();
        let (r_mb, p_mb) = r.procs.iter().map(|p| proc::mem(*p)).fold((0, 0), |a, m| (a.0 + m.0, a.1 + m.1));
        let c = total(&r.procs, proc::ticks).saturating_sub(b);
        let g = total(&r.procs, |p| gpu.get(&p).copied().unwrap_or(0));
        println!("{} {} {state} {frames} {ms} {} {r_mb} {p_mb} {c} {g}", r.n, r.pid, r.procs.len());
        (rss, pss, cpu, mib) = (rss + r_mb, pss + p_mb, cpu + c, mib + g);
    }
    let x = display::xserver_pid().map_or((0, 0), proc::mem);
    println!("browsers {} rss_mb {rss} pss_mb {pss} cpu_pct {cpu} gpu_mb {mib} gpu_used_mb {} xwayland_rss_mb {} total_pss_mb {}", rows.len(), proc::gpu_used(), x.0, pss + x.1);
    Ok(())
}
