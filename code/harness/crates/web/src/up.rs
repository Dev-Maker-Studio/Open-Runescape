use crate::launch::{self, Browser, Launch};
use crate::{game, page::Page, server, state, trail};
use serde_json::{json, Value};
use std::time::{Duration, Instant};

pub fn start(rest: &[String]) -> Result<(Browser, String), String> {
    let given = rest.join(" ");
    let given_args = rest.to_vec();
    if let Some(b) = state::browser() {
        return Err(format!("web client already running, pid {}", b.pid));
    }
    let (rest, native) = crate::native::take(rest)?;
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
    let (rest, query) = match native {
        Some(o) => { game::stop(); (rest, crate::native::start(o)?) }
        None => { crate::native::stop(); game::start(&rest)? }
    };
    let scene = rest.iter().find(|a| !a.starts_with("--")).map_or(String::new(), |s| format!("?scene={s}"));
    let extra: Vec<String> = rest.iter().filter(|a| a.starts_with("--")).cloned().collect();
    let url = format!("http://127.0.0.1:{port}/{}", if query.is_empty() { scene } else { query });
    let profile = state::profile();
    let _ = std::fs::remove_dir_all(&profile);
    let b = launch::launch(&Launch { display: &display, url: &url, profile: &profile, args: &extra, envs: &[], wrapper: &[] }).inspect_err(|_| crate::native::stop())?;
    state::write("browser", &format!("{} {} {}", b.pid, b.port, b.ws));
    state::write("args", &given);
    state::write("args.json", &serde_json::to_string(&given_args).unwrap());
    println!("web client pid {} on {display} server pid {spid} at {url} devtools {} log {}", b.pid, b.port, profile.join("chromium.log").display());
    Ok((b, url))
}

fn page_state() -> Result<Value, String> {
    Page::open()?.run("state", &[])
}

pub fn up(args: &[String]) -> Result<(), String> {
    let begin = Instant::now();
    let (rest, skip) = fresh::strip(args);
    let native = rest.iter().any(|a| a == "--native");
    crate::native::take(&rest)?;
    crate::native::resume()?;
    game::resume()?;
    if let Some(s) = state::browser().and_then(|_| page_state().ok()).filter(|s| s["ready"] == true) {
        if native && (s["stream"]["game"] != "native" || s["stream"]["received"].as_u64().unwrap_or(0) == 0) {
            return Err("existing browser is not receiving native frames; use a distinct OPENRS_WEB_STATE or web down first".into());
        }
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
            Ok(s) if native && s["stream"]["error"].is_string() => return Err(format!("native stream: {}", s["stream"]["error"].as_str().unwrap_or("failed"))),
            Ok(s) if s["ready"] == true && s["frames"].as_i64().unwrap_or(0) > 0 && (!native || s["stream"]["received"].as_u64().unwrap_or(0) > 0) => {
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
        crate::proc::kill(pid);
    }
    game::stop();
    crate::native::stop();
    state::remove("browser");
    state::remove("server");
    Ok(())
}
