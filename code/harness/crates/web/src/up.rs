use crate::launch::{self, Browser, Launch};
use crate::{page::Page, server, state, trail};
use serde_json::{json, Value};
use std::process::Command;
use std::time::{Duration, Instant};

pub fn start(rest: &[String]) -> Result<(Browser, String), String> {
    if let Some(b) = state::browser() {
        return Err(format!("web client already running, pid {}", b.pid));
    }
    let root = display::root()?;
    let (spid, port) = match state::server() {
        Some(s) => s,
        None => {
            let s = server::start(&root, &state::files())?;
            state::write("server", &format!("{} {}", s.0, s.1));
            s
        }
    };
    let display = display::display_name()?;
    let scene = rest.iter().find(|a| !a.starts_with("--")).map_or(String::new(), |s| format!("?scene={s}"));
    let extra: Vec<String> = rest.iter().filter(|a| a.starts_with("--")).cloned().collect();
    let url = format!("http://127.0.0.1:{port}/{scene}");
    let profile = state::profile();
    let _ = std::fs::remove_dir_all(&profile);
    let b = launch::launch(&Launch { display: &display, url: &url, profile: &profile, args: &extra, envs: &[], wrapper: &[] })?;
    state::write("browser", &format!("{} {} {}", b.pid, b.port, b.ws));
    println!("web client pid {} on {display} server pid {spid} at {url} devtools {} log {}", b.pid, b.port, profile.join("chromium.log").display());
    Ok((b, url))
}

fn page_state() -> Result<Value, String> {
    Page::open()?.run("state", &[])
}

pub fn up(args: &[String]) -> Result<(), String> {
    let begin = Instant::now();
    let (rest, skip) = fresh::strip(args);
    if let Some(s) = state::browser().and_then(|_| page_state().ok()).filter(|s| s["ready"] == true) {
        println!("ready (already up) frame {}", s["frames"]);
        return Ok(());
    }
    if !skip {
        fresh::web(&display::root()?)?;
    }
    let started = Instant::now();
    let (b, url) = start(&rest)?;
    while started.elapsed() < Duration::from_secs(60) {
        match page_state() {
            Ok(s) if s["lastError"].is_string() => return Err(format!("page error: {}", s["lastError"].as_str().unwrap_or(""))),
            Ok(s) if s["ready"] == true && s["frames"].as_i64().unwrap_or(0) > 0 => {
                let g = &s["gpu"];
                println!("ready frame {} {} {} adapter {} gpu {} {} fallback {}", s["frames"], s["backend"].as_str().unwrap_or("?"), s["wgpu"].as_str().unwrap_or("?"), s["adapter"]["name"], g["vendor"], g["architecture"], g["fallback"]);
                println!("timings: page ready {:.1}s, total {:.1}s", started.elapsed().as_secs_f32(), begin.elapsed().as_secs_f32());
                trail::append(s["frames"].as_i64().unwrap_or(-1), "up", &[("url", json!(url)), ("pid", json!(b.pid))]);
                return Ok(());
            }
            _ if state::browser().is_none() => return Err(format!("chromium exited, see {}", state::profile().join("chromium.log").display())),
            _ => std::thread::sleep(Duration::from_millis(100)),
        }
    }
    Err("page not ready after 60s, see openrs web do console".into())
}

pub fn down() -> Result<(), String> {
    let pids = [state::browser().map(|b| b.pid), state::server().map(|s| s.0)];
    for pid in pids.into_iter().flatten() {
        let _ = Command::new("kill").arg(pid.to_string()).status();
    }
    state::remove("browser");
    state::remove("server");
    Ok(())
}
