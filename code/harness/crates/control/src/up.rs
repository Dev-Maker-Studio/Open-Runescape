use crate::{conn, trail};
use std::net::TcpStream;
use std::time::{Duration, Instant};

fn port(var: &str, default: &str) -> String {
    std::env::var(var).unwrap_or_else(|_| default.into())
}

pub fn ingame() -> Option<serde_json::Value> {
    conn::send("state", &[]).ok().filter(|s| s["gameState"] == "LOGGED_INGAME" && s["tick"].as_i64().unwrap_or(0) >= 1)
}

fn local_server_up() -> bool {
    TcpStream::connect_timeout(&format!("127.0.0.1:{}", port("OPENRS_LOBBY_PORT", "24594")).parse().unwrap(), Duration::from_millis(300)).is_ok()
}

fn ensure_cache(root: &std::path::Path) -> String {
    let packed = cache::packed(root);
    if packed.join("main_file_cache.dat2").exists() {
        packed.display().to_string()
    } else {
        "none packed, run openrs cache build".into()
    }
}

pub fn milestones(log: &str) -> String {
    log.lines()
        .filter_map(|l| {
            l.split_once("[Milestone] ").map(|x| x.1.to_string()).or_else(|| l.split("[load] done total=").nth(1).map(|t| format!("load done {}", t.split(' ').next().unwrap_or(""))))
        })
        .collect::<Vec<_>>()
        .join(", ")
}

fn player(args: &[&str]) {
    if let Ok(exe) = std::env::current_exe() {
        let _ = std::process::Command::new(exe).arg("player").args(args).stdout(std::process::Stdio::null()).stderr(std::process::Stdio::null()).status();
    }
}

pub fn client_args(shared: bool, rest: Vec<String>) -> Vec<String> {
    if let Ok(root) = display::root() {
        ensure_cache(&root);
    }
    let mut client_args = vec![format!("-Dbot.control={}", conn::endpoint()), format!("-Dbot.footprints={}", trail::dir().display())];
    if shared {
        client_args.push("-Dautologin=true".into());
    } else {
        let (name, pass) = ("claudeqa", "claudebot1");
        if !local_server_up() {
            playerstore::run(&["serve".to_string()]);
        }
        player(&["account", name, pass]);
        player(&["create", name]);
        player(&["set", name, "chosenAccountType", "true"]);
        client_args.extend([format!("-Dautologin={name}:{pass}"), format!("-Dautologin.world={}", playerstore::LOCAL_WORLD), "localhost".into(), port("OPENRS_LOBBY_PORT", "24594"), port("OPENRS_WORLD_PORT", "24595")]);
    }
    client_args.extend(rest);
    client_args
}

pub fn up(args: &[String]) -> Result<(), String> {
    let begin = Instant::now();
    let shared = args.first().is_some_and(|a| a == "shared");
    let rest: Vec<String> = args.iter().filter(|a| *a != "shared" && *a != "local").cloned().collect();
    if let Some(s) = ingame() {
        println!("ready (already up) tick {}", s["tick"]);
        return Ok(());
    }
    let root = display::root()?;
    println!("cache: {}", ensure_cache(&root));
    let client_args = client_args(shared, rest);
    cache::check(&root, shared)?;
    let started = Instant::now();
    display::run(&client_args)?;
    trail::append("up", &format!("\"mode\":\"{}\"", if shared { "shared" } else { "local" }));
    let log = display::dir().join("client.log");
    let pid = display::client_pid();
    while started.elapsed() < Duration::from_secs(180) {
        if let Some(s) = ingame() {
            let _ = conn::send("wait-tick", &["1".to_string()]);
            let pos = conn::send("pos", &[]).map(|p| p.to_string()).unwrap_or_default();
            println!("ready tick {} pos {pos}", s["tick"]);
            println!("timings: {}, world entry {:.1}s, total {:.1}s", milestones(&std::fs::read_to_string(&log).unwrap_or_default()), started.elapsed().as_secs_f32(), begin.elapsed().as_secs_f32());
            return Ok(());
        }
        if pid.is_some() && display::client_pid().is_none() {
            return Err(format!("client exited, see {}", log.display()));
        }
        std::thread::sleep(Duration::from_millis(100));
    }
    Err(format!("not in world after 180s, see {}", log.display()))
}

pub fn down(args: &[String]) -> Result<(), String> {
    display::stop_client();
    if args.first().is_some_and(|a| a == "all") {
        display::stop()?;
        playerstore::run(&["serve".to_string(), "stop".to_string()]);
    }
    Ok(())
}
