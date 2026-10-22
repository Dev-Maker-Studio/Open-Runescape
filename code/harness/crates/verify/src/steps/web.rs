use super::{last, shot, size, tail, Live};
use crate::Step;
use serde_json::{json, Value};
use std::path::PathBuf;
use std::process::Command;
use std::time::{Duration, Instant};

pub const WEB: &[Step<Live>] = &[
    ("web-up", up),
    ("web-state", state),
    ("web-replay", replay),
    ("web-look", look),
    ("web-input", input),
    ("web-trail", trail),
    ("web-toggle", toggle),
    ("web-bench", bench),
    ("web-fleet-up", fleet_up),
    ("web-fleet-do", fleet_do),
    ("web-fleet-look", fleet_look),
    ("web-fleet-down", fleet_down),
    ("capture-browser", capture_browser),
    ("web-resume", resume),
    ("web-game-orbit", game_orbit),
    ("web-game-walker", game_walker),
    ("web-game-buttons", game_buttons),
    ("web-down", down),
];

pub const EQUIV: &[Step<Live>] = &[("equiv-browser", equiv_browser)];

const FLEET: usize = 3;

fn web(l: &Live, line: &str) -> Result<String, String> {
    l.openrs(&format!("web {line}"))
}

fn page(l: &Live) -> Result<Value, String> {
    let t = web(l, "do state")?;
    serde_json::from_str(t.trim()).map_err(|e| format!("web do state is not JSON ({e})\n{}", tail(&t, 5)))
}

pub fn hardware(s: &Value) -> Result<String, String> {
    let g = &s["gpu"];
    let arch = g["architecture"].as_str().unwrap_or("none");
    if s["ready"] != true || g["fallback"] != false || arch.contains("swiftshader") || arch == "none" {
        return Err(format!("not a ready hardware WebGPU adapter: ready {}, gpu {g}", s["ready"]));
    }
    Ok(format!("{} {arch}, frame {}", g["vendor"].as_str().unwrap_or("?"), s["frames"]))
}

pub fn replay_draws(summary: &str) -> Result<(u64, u64, u64), String> {
    let w: Vec<&str> = summary.split_whitespace().collect();
    let at = |word: &str| w.iter().position(|x| *x == word).and_then(|i| w.get(i + 1)?.parse::<u64>().ok());
    match (w.get(1).and_then(|n| n.parse::<u64>().ok()), at("drawn"), at("skipped")) {
        (Some(d), Some(n), Some(k)) if n > 0 && n + k == d => Ok((d, n, k)),
        _ => Err(format!("replay drew nothing or lost draws: {summary}")),
    }
}

pub fn last_shot(trail: &str) -> Option<Value> {
    trail.lines().rev().filter_map(|l| serde_json::from_str::<Value>(l).ok()).find(|v| v["kind"] == "shot" && v["colors"].is_number())
}

pub fn cmd_tick(trail: &str, cmd: &str) -> Option<i64> {
    trail.lines().rev().filter_map(|l| serde_json::from_str::<Value>(l).ok()).find(|v| v["kind"] == "cmd" && v["cmd"] == cmd && v["ok"] == true).and_then(|v| v["tick"].as_i64())
}

pub fn input_ok(s: &Value) -> Result<String, String> {
    let c = &s["input"]["counts"];
    let n = |k: &str| c[k].as_u64().unwrap_or(0);
    let need = [("mousedown", 3), ("mouseup", 3), ("click", 2), ("dblclick", 1), ("mousemove", 1), ("wheel", 1), ("keydown", 4), ("keyup", 4), ("keypress", 3)];
    let missing: Vec<String> = need.iter().filter(|(k, min)| n(k) < *min).map(|(k, min)| format!("{k} {} of at least {min}", n(k))).collect();
    if missing.is_empty() { Ok(format!("page saw {c}")) } else { Err(format!("the page missed input: {}", missing.join(", "))) }
}

fn colors(l: &Live, trail: &str) -> Result<(String, u64), String> {
    let v = last_shot(&l.openrs(trail)?).ok_or("the trail has no shot line with colors")?;
    Ok((v["path"].as_str().unwrap_or("").to_string(), v["colors"].as_u64().unwrap_or(0)))
}

fn up(l: &mut Live) -> Result<String, String> {
    let t = web(l, "up")?;
    if !t.contains("ready") {
        return Err(format!("web up did not report ready\n{}", tail(&t, 10)));
    }
    Ok(t.lines().filter(|x| x.starts_with("rebuilt") || x.starts_with("ready") || x.starts_with("timings")).collect::<Vec<_>>().join("\n"))
}

fn state(l: &mut Live) -> Result<String, String> {
    let s = page(l)?;
    let m = hardware(&s)?;
    l.metrics.insert("web".into(), json!({"gpu": s["gpu"], "frame_ms": s["frameMs"], "cpu_ms": s["cpuMs"]}));
    Ok(m)
}

fn streamed_game(l: &mut Live, name: &str, action: &str) -> Result<String, String> {
    web(l, "down")?;
    web(l, &format!("up --game {name}"))?;
    web(l, "do wait-frames 4")?;
    if !action.is_empty() {
        web(l, &format!("do {action} ; wait-frames 6"))?;
    }
    let state = page(l)?;
    hardware(&state)?;
    let stream = &state["stream"];
    if stream["game"] != name || stream["received"].as_u64().unwrap_or(0) == 0 || stream["shown"].as_u64().unwrap_or(0) == 0 {
        return Err(format!("{name} stream did not render: {stream}"));
    }
    if !action.is_empty() && stream["inputSeq"].as_u64().unwrap_or(0) == 0 {
        return Err(format!("{name} host acknowledged no input: {stream}"));
    }
    let path = l.path(&format!("game-{name}.png"));
    shot(&web(l, &format!("look {path}"))?)?;
    let (_, distinct) = colors(l, "web trail 5")?;
    if distinct < 2 { return Err(format!("{name} stream is blank")); }
    l.metrics.insert(format!("game_{name}"), stream.clone());
    Ok(format!("{name} host frame {}, received {}, dropped {}, rtt {} ms", stream["shown"], stream["received"], stream["dropped"], stream["rttMs"]))
}

fn game_orbit(l: &mut Live) -> Result<String, String> { streamed_game(l, "orbit", "") }
fn game_walker(l: &mut Live) -> Result<String, String> { streamed_game(l, "walker", "key press KeyW") }
fn game_buttons(l: &mut Live) -> Result<String, String> { streamed_game(l, "buttons", "click 100 100") }

fn replay(l: &mut Live) -> Result<String, String> {
    let f = PathBuf::from(l.path("web-replay-scene.orf"));
    let renderer = l.root.join("code/renderer/target/release/renderer-app");
    let made = Command::new(&renderer).args(["--scene", "all", "--size", "765x503", "--dump"]).arg(&f).output().map_err(|e| e.to_string())?;
    if !made.status.success() {
        return Err(format!("could not generate replay scene: {}", String::from_utf8_lossy(&made.stderr)));
    }
    let t = web(l, &format!("do replay {} ; wait-frames 3", f.display()))?;
    let (draws, drawn, skipped) = replay_draws(t.lines().find(|x| x.starts_with("replay:")).ok_or_else(|| format!("replay printed no summary\n{}", tail(&t, 5)))?)?;
    let out = l.path("web-frame.orf");
    web(l, &format!("do dump-frame {out}"))?;
    if std::fs::read(&out).ok() != std::fs::read(&f).ok() {
        return Err(format!("dump-frame of the replay differs from {}", f.display()));
    }
    let path = l.path("web-replay.png");
    shot(&web(l, &format!("look {path}"))?)?;
    let (_, distinct) = colors(l, "web trail 5")?;
    if skipped != 0 || distinct < 8 {
        return Err(format!("all-pass replay skipped {skipped} draws or was blank ({distinct} colors)"));
    }
    Ok(format!("{draws} draws, {drawn} drawn, {skipped} skipped, dump byte identical, {distinct} sampled colors"))
}

fn look(l: &mut Live) -> Result<String, String> {
    let p = l.path("web-look.png");
    let _ = std::fs::remove_file(&p);
    web(l, "do scene model ; wait-frames 2")?;
    let shown = shot(&web(l, &format!("look {p}"))?)?;
    let (_, n) = colors(l, "web trail 5")?;
    if n < 2 {
        return Err(format!("{shown} is blank, {n} distinct sampled color"));
    }
    Ok(format!("{shown}, {n} distinct sampled colors, {} bytes", size(&shown)))
}

fn input(l: &mut Live) -> Result<String, String> {
    web(l, "do input-reset ; click 100 100")?;
    let first = page(l)?;
    let hit = first["input"]["last"].as_array().into_iter().flatten().any(|e| e["type"] == "click" && e["x"] == 100 && e["y"] == 100);
    if !hit {
        return Err(format!("the click at 100 100 did not reach the page at those canvas pixels: {}", first["input"]["last"]));
    }
    web(l, "do dblclick 120 100 ; move 50 50 ; drag 10 10 80 80 ; scroll 0 120 ; key press KeyA ; type hi ; key down ShiftLeft ; key up ShiftLeft")?;
    input_ok(&page(l)?)
}

fn trail(l: &mut Live) -> Result<String, String> {
    let t = web(l, "trail 80")?;
    let tick = cmd_tick(&t, "click").filter(|n| *n > 0).ok_or("the trail has no click with a page frame number")?;
    if last_shot(&t).is_none() {
        return Err("the trail has no shot line".into());
    }
    let since = web(l, &format!("trail since {tick}"))?;
    if !since.contains("\"cmd\":\"click\"") || since.lines().filter_map(|x| serde_json::from_str::<Value>(x).ok()).any(|v| v["tick"].as_i64().unwrap_or(0) < tick) {
        return Err(format!("trail since {tick} is not the lines from that frame on\n{}", tail(&since, 5)));
    }
    Ok(format!("{} lines, click logged at frame {tick}", t.lines().count()))
}

fn toggle(l: &mut Live) -> Result<String, String> {
    web(l, "show")?;
    web(l, "hide").map(|_| "private display shown then hidden".into())
}

fn bench(l: &mut Live) -> Result<String, String> {
    if !web(l, "do help")?.contains("\"bench\"") {
        return Ok("skipped, the page has no bench command".into());
    }
    let t = web(l, "do bench 20")?;
    t.lines().find(|x| x.contains("p50")).map(|x| x.trim().to_string()).ok_or_else(|| format!("bench printed no p50 line\n{}", tail(&t, 5)))
}

fn each(l: &Live, f: impl Fn(&Live, usize) -> Result<String, String> + Sync) -> Result<Vec<String>, String> {
    let f = &f;
    let all: Vec<Result<String, String>> = std::thread::scope(|s| {
        let h: Vec<_> = (1..=FLEET).map(|n| s.spawn(move || f(l, n))).collect();
        h.into_iter().map(|h| h.join().unwrap_or(Err("panicked".into()))).collect()
    });
    let errs: Vec<String> = all.iter().filter_map(|r| r.as_ref().err().cloned()).collect();
    if errs.is_empty() { Ok(all.into_iter().flatten().collect()) } else { Err(errs.join("\n")) }
}

fn fleet_up(l: &mut Live) -> Result<String, String> {
    let t = web(l, &format!("fleet up {FLEET} --scene model --no-build"))?;
    if !t.contains(&format!("web fleet of {FLEET} up")) || !t.contains(" 0 failed") {
        return Err(format!("fleet up did not report {FLEET} slots\n{}", tail(&t, 8)));
    }
    Ok(last(&t))
}

fn fleet_do(l: &mut Live) -> Result<String, String> {
    let t = Instant::now();
    let seen = each(l, |l, n| {
        let out = web(l, &format!("fleet do {n} click 50 50 ; state"))?;
        let s: Value = serde_json::from_str(last(&out).trim()).map_err(|e| format!("slot {n}: state is not JSON ({e})"))?;
        match s["input"]["counts"]["click"].as_u64() {
            Some(1) => Ok(format!("slot {n}")),
            c => Err(format!("slot {n} saw {c:?} clicks instead of its own 1")),
        }
    })?;
    let ms = t.elapsed().as_millis();
    let trail = web(l, "fleet trail 1 5")?;
    if cmd_tick(&trail, "click").is_none() {
        return Err("slot 1 trail has no click line".into());
    }
    Ok(format!("{} slots answered in parallel in {ms} ms, each saw only its own click", seen.len()))
}

fn fleet_look(l: &mut Live) -> Result<String, String> {
    let l2 = &*l;
    let dir = l2.path("web-fleet");
    let _ = std::fs::create_dir_all(&dir);
    let shots = each(l2, |l, n| {
        let p = format!("{dir}/slot{n}.png");
        let _ = std::fs::remove_file(&p);
        let shown = shot(&web(l, &format!("fleet look {n} {p}"))?)?;
        let (_, c) = colors(l, &format!("web fleet trail {n} 5"))?;
        if c < 2 { Err(format!("slot {n} screenshot is blank")) } else { Ok(shown) }
    })?;
    Ok(format!("{} screenshots, none blank, in {dir}", shots.len()))
}

fn fleet_down(l: &mut Live) -> Result<String, String> {
    web(l, "fleet down")?;
    let t = web(l, "fleet status")?;
    if !last(&t).starts_with("browsers 0 ") {
        return Err(format!("browsers still recorded after fleet down\n{}", last(&t)));
    }
    Ok("fleet browsers and server stopped".into())
}

fn capture_browser(l: &mut Live) -> Result<String, String> {
    let out = l.path("capture-browser");
    let _ = std::fs::remove_dir_all(&out);
    let t = l.openrs(&format!("capture browser --scene model --frame 3 --out {out}"))?;
    match size(&format!("{out}/frame.rdc")) {
        0 => Err(format!("capture browser wrote no frame.rdc in {out}\n{}", tail(&t, 8))),
        n => Ok(format!("{n} bytes in {out}/frame.rdc")),
    }
}

fn equiv_browser(l: &mut Live) -> Result<String, String> {
    let out = l.path("equiv-browser");
    let (code, text) = l.sh(&["equiv", "--live", "--cold", "--target", "browser", "--out", &out], &[]);
    if !matches!(code, Some(0 | 1)) {
        return Err(format!("equiv errored {code:?}\n{}", tail(&text, 20)));
    }
    let res: Value = std::fs::read_to_string(format!("{out}/equiv.json")).ok().and_then(|t| serde_json::from_str(&t).ok()).ok_or("equiv wrote no equiv.json")?;
    l.metrics.insert("equiv_browser".into(), json!({"verdict": res["verdict"], "score": res["score"], "mode": res["mode"]}));
    Ok(format!("browser target {} score {}", res["verdict"].as_str().unwrap_or("?"), res["score"]))
}

fn runtime(name: &str) -> PathBuf {
    PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join(name)
}

fn web_dir() -> PathBuf {
    std::env::var_os("OPENRS_WEB_STATE").map_or_else(|| runtime("openrs-web"), PathBuf::from)
}

fn fleet_dir() -> PathBuf {
    std::env::var_os("OPENRS_WEB_FLEET_STATE").map_or_else(|| runtime("openrs-web-fleet"), PathBuf::from)
}

pub fn recorded_pid(browser_file: &str) -> Option<u32> {
    browser_file.split_whitespace().next()?.parse().ok()
}

fn pid_in(dir: &std::path::Path) -> Result<u32, String> {
    recorded_pid(&std::fs::read_to_string(dir.join("browser")).map_err(|e| format!("{}/browser: {e}", dir.display()))?).ok_or(format!("{}/browser holds no pid", dir.display()))
}

fn kill_exact(pid: u32) -> Result<(), String> {
    Command::new("kill").args(["-9", &pid.to_string()]).status().map_err(|e| e.to_string())?;
    let t = Instant::now();
    while std::path::Path::new(&format!("/proc/{pid}")).exists() && !std::fs::read_to_string(format!("/proc/{pid}/stat")).is_ok_and(|s| s.contains(") Z")) {
        if t.elapsed() > Duration::from_secs(10) {
            return Err(format!("pid {pid} did not die"));
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    Ok(())
}

fn resume(l: &mut Live) -> Result<String, String> {
    let old = pid_in(&web_dir())?;
    kill_exact(old)?;
    if !l.openrs("resume web")?.contains("resuming the web client") {
        return Err("resume did not restart the web client".into());
    }
    let s = page(l)?;
    let new = pid_in(&web_dir())?;
    if s["ready"] != true || new == old {
        return Err(format!("web client not back after resume, pid {old} then {new}, ready {}", s["ready"]));
    }
    web(l, &format!("fleet up {FLEET} --scene model --no-build"))?;
    let before: Vec<u32> = (1..=FLEET).map(|n| pid_in(&fleet_dir().join(format!("s{n}")))).collect::<Result<_, _>>()?;
    kill_exact(before[1])?;
    let t = l.openrs("resume web")?;
    if !t.contains(&format!("resuming the web fleet of {FLEET}")) {
        return Err(format!("resume did not restart the fleet\n{}", tail(&t, 8)));
    }
    let after: Vec<u32> = (1..=FLEET).map(|n| pid_in(&fleet_dir().join(format!("s{n}")))).collect::<Result<_, _>>()?;
    let back = web(l, "fleet do 2 wait-frames 2 ; state").map(|t| last(&t).contains("\"ready\":true") && last(&t).contains("scene=model"));
    web(l, "fleet down")?;
    if after[1] == before[1] || after[0] != before[0] || after[2] != before[2] || back != Ok(true) {
        return Err(format!("fleet resume replaced the wrong slots: pids {before:?} then {after:?}, slot 2 ready on its scene {back:?}"));
    }
    Ok(format!("client pid {old} -> {new}, fleet slot 2 pid {} -> {}, the other slots kept theirs", before[1], after[1]))
}

fn down(l: &mut Live) -> Result<String, String> {
    web(l, "down")?;
    match l.sh(&["web", "do", "state"], &[]).0 {
        Some(1) => Ok("web client and server stopped".into()),
        c => Err(format!("web do state exited {c:?} after web down, expected 1")),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hardware_adapter_verdicts() {
        let ok = json!({"ready": true, "frames": 7, "gpu": {"vendor": "nvidia", "architecture": "turing", "fallback": false}});
        assert_eq!(hardware(&ok).unwrap(), "nvidia turing, frame 7");
        assert!(hardware(&json!({"ready": true, "gpu": {"architecture": "swiftshader", "fallback": true}})).is_err());
        assert!(hardware(&json!({"ready": true, "gpu": {"architecture": "swiftshader", "fallback": false}})).is_err());
        assert!(hardware(&json!({"ready": false, "gpu": {"architecture": "turing", "fallback": false}})).is_err());
        assert!(hardware(&json!({"ready": true, "gpu": null})).is_err());
    }

    #[test]
    fn replay_summary_must_account_for_every_draw() {
        assert_eq!(replay_draws("replay: 3297 draws, drawn 3297 [a=1], skipped 0 []"), Ok((3297, 3297, 0)));
        assert_eq!(replay_draws("replay: 5 draws, drawn 4 [a=4], skipped 1 [water=1]"), Ok((5, 4, 1)));
        assert!(replay_draws("replay: 10 draws, drawn 8 [a=8], skipped 1 [b=1]").is_err());
        assert!(replay_draws("replay: 3 draws, drawn 0 [], skipped 3 [b=3]").is_err());
        assert!(replay_draws("replay: 0 draws, drawn 0 [], skipped 0 []").is_err());
        assert!(replay_draws("no frame loaded").is_err());
    }

    #[test]
    fn trail_lines_for_shots_and_commands() {
        let t = "{\"t\":1,\"tick\":3,\"kind\":\"cmd\",\"cmd\":\"click\",\"args\":[],\"ok\":true}\n{\"t\":2,\"tick\":4,\"kind\":\"shot\",\"path\":\"/a.png\",\"colors\":1}\n{\"t\":3,\"tick\":9,\"kind\":\"shot\",\"path\":\"/b.png\",\"colors\":40}\n{\"t\":4,\"tick\":9,\"kind\":\"cmd\",\"cmd\":\"click\",\"ok\":false}\nnot json\n";
        assert_eq!(last_shot(t).unwrap()["path"], "/b.png");
        assert_eq!(cmd_tick(t, "click"), Some(3));
        assert_eq!(cmd_tick(t, "state"), None);
        assert!(last_shot("{\"kind\":\"shot\",\"colors\":null}").is_none());
    }

    #[test]
    fn input_counts_must_cover_every_device() {
        let good = json!({"input": {"counts": {"mousedown": 4, "mouseup": 4, "click": 3, "dblclick": 1, "mousemove": 9, "wheel": 1, "keydown": 4, "keyup": 4, "keypress": 3}}});
        assert!(input_ok(&good).is_ok());
        let mut bad = good.clone();
        bad["input"]["counts"]["wheel"] = json!(0);
        bad["input"]["counts"]["keyup"] = json!(1);
        let e = input_ok(&bad).unwrap_err();
        assert!(e.contains("wheel 0") && e.contains("keyup 1") && !e.contains("click"));
        assert!(input_ok(&json!({})).is_err());
    }

    #[test]
    fn browser_file_pid_and_step_names() {
        assert_eq!(recorded_pid("1234 9222 ws://127.0.0.1:9222/devtools/browser/x\n"), Some(1234));
        assert_eq!(recorded_pid(""), None);
        let names: Vec<&str> = WEB.iter().chain(EQUIV).map(|s| s.0).collect();
        assert!(names.iter().all(|n| n.starts_with("web-") || n.ends_with("-browser")));
        let pos = |n: &str| names.iter().position(|x| *x == n).unwrap();
        assert!(pos("web-up") == 0 && pos("web-fleet-up") < pos("web-fleet-do") && pos("web-fleet-do") < pos("web-fleet-down") && pos("web-resume") < pos("web-down"));
        assert_eq!(names.last(), Some(&"equiv-browser"));
    }
}
