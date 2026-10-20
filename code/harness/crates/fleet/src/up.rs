use crate::config::{Cfg, Slot};
use crate::{creds, local, proc};
use control::Conn;
use std::collections::VecDeque;
use std::path::PathBuf;
use std::time::{Duration, Instant};

const JVM: &str = "-XX:+UseSerialGC -Xms64m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:-UsePerfData -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:ReservedCodeCacheSize=48m -Xss512k -Drender.allowsoftgl=true";

pub fn sock(n: usize) -> PathBuf {
    display::dir().join(format!("s{n}/c.sock"))
}

pub fn ingame(n: usize) -> bool {
    Conn::open_at(&sock(n).display().to_string()).and_then(|mut c| c.send("state", &[])).is_ok_and(|s| s["gameState"] == "LOGGED_INGAME" && s["tick"].as_i64().unwrap_or(0) >= 1)
}

fn refused(n: usize) -> Option<String> {
    let log = std::fs::read_to_string(display::dir().join(format!("slot{n}.log"))).ok()?;
    log.lines().find_map(|l| l.strip_prefix("Login response: ").filter(|c| *c != "2").map(String::from))
}

fn launch(cfg: &Cfg, slot: &Slot, pass: &str) -> Result<u32, String> {
    let (host, lobby, world) = cfg.endpoint(slot)?;
    let dir = display::dir().join(format!("s{}", slot.n));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(dir.join("home")).map_err(|e| e.to_string())?;
    let mut jvm: Vec<String> = cfg.get("defaults.jvm", JVM).split_whitespace().map(String::from).collect();
    jvm.push(format!("-Xmx{}", slot.heap));
    let mut args = vec![
        format!("-Dbot.control={}", sock(slot.n).display()),
        format!("-Dbot.footprints={}", dir.join("fp").display()),
        format!("-Dautologin={}:{pass}", slot.account),
        format!("-Duser.home={}", dir.join("home").display()),
        format!("-Dwindow.size={}", cfg.get("defaults.window", "800x600")),
        format!("-Dfps={}", cfg.get("defaults.fps", "20")),
        "-Dbot.scripts.sync=false".into(),
    ];
    if slot.server == "local" {
        args.push(format!("-Dautologin.world={}", playerstore::LOCAL_WORLD));
    }
    args.extend([host, lobby, world]);
    display::launch(&format!("slot{}", slot.n), &jvm, &args, &[("MALLOC_ARENA_MAX".to_string(), "2".to_string())])
}

pub fn up(args: &[String]) -> Result<(), String> {
    let count: usize = args.first().map_or(Ok(9), |a| a.parse().map_err(|_| format!("bad slot count {a}")))?;
    if !(1..=9).contains(&count) {
        return Err("slot count must be 1 to 9".into());
    }
    let begin = Instant::now();
    let cfg = Cfg::load()?;
    let slots = (1..=count).map(|n| cfg.slot(n)).collect::<Result<Vec<_>, _>>()?;
    local::ensure(&cfg, &slots)?;
    let mut pending: VecDeque<(&Slot, String)> = VecDeque::new();
    for s in &slots {
        if proc::pid(&format!("slot{}", s.n), "darkan-client").is_some() {
            println!("slot {} already up", s.n);
            continue;
        }
        let pass = creds::password(s)?;
        if s.server == "local" {
            local::prepare(s, &pass)?;
        }
        pending.push_back((s, pass));
    }
    let parallel: usize = cfg.get("defaults.parallel", "3").parse().unwrap_or(3);
    let mut loading: Vec<(usize, u32, Instant)> = vec![];
    let mut failed = 0;
    while !pending.is_empty() || !loading.is_empty() {
        loading.retain(|&(n, pid, t)| {
            let done = ingame(n);
            let refusal = refused(n);
            let dead = !proc::alive(pid) || t.elapsed() > Duration::from_secs(240);
            if done {
                println!("slot {n} ready in {:.1}s, pid {pid}", t.elapsed().as_secs_f32());
            } else if dead || refusal.is_some() {
                eprintln!("slot {n} failed (login response {}), see {}", refusal.as_deref().unwrap_or("none"), display::dir().join(format!("slot{n}.log")).display());
                failed += 1;
                proc::kill(pid);
            }
            !(done || dead || refusal.is_some())
        });
        while loading.len() < parallel {
            let Some((s, pass)) = pending.pop_front() else { break };
            loading.push((s.n, launch(&cfg, s, &pass)?, Instant::now()));
        }
        std::thread::sleep(Duration::from_millis(250));
    }
    println!("fleet of {count} up in {:.1}s, {failed} failed", begin.elapsed().as_secs_f32());
    if failed > 0 { Err("some slots failed".into()) } else { Ok(()) }
}
