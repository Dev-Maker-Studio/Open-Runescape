use crate::Step;
use serde_json::{json, Map, Value};
use std::path::{Path, PathBuf};
use std::process::Command;

mod web;
pub use web::{EQUIV, WEB};

const ACCOUNT: &str = "claudeqa";
const LOGS: &str = "1511";

pub const ALL: &[Step<Live>] = &[
    ("doctor", doctor),
    ("down-before", down_before),
    ("reset-player", reset_player),
    ("up", up),
    ("time-to-world", time_to_world),
    ("reads", reads),
    ("teleport", teleport),
    ("walk", walk),
    ("waits", waits),
    ("chat", chat),
    ("give-item", give_item),
    ("widget-drop", widget_drop),
    ("take", take),
    ("npc", npc),
    ("object", object),
    ("scripts", scripts),
    ("look", look),
    ("look-display", look_display),
    ("iface-open", iface_open),
    ("iface-text-look", iface_text),
    ("iface-item-model", iface_item_model),
    ("iface-close", iface_close),
    ("dump-frame", dump_frame),
    ("renderer-replay", replay),
    ("twin", twin),
    ("twin-multiframe", twin_multiframe),
    ("equiv", equiv),
    ("renderdoc-retry", renderdoc_retry),
    ("down", down),
    ("fleet-up", fleet_up),
    ("fleet-time-to-world", fleet_time_to_world),
    ("fleet-do", fleet_do),
    ("fleet-compare", fleet_compare),
    ("fleet-status", fleet_status),
    ("fleet-down", fleet_down),
    ("down-all", down_all),
];

pub struct Live {
    exe: PathBuf,
    root: PathBuf,
    pub out: PathBuf,
    pub metrics: Map<String, Value>,
    dropped: (i64, i64),
}

impl Live {
    pub fn new() -> Result<Live, String> {
        let root = display::root()?;
        let out = root.join("code/harness/target/verify");
        std::fs::create_dir_all(&out).map_err(|e| e.to_string())?;
        Ok(Live { exe: std::env::current_exe().map_err(|e| e.to_string())?, root, out, metrics: Map::new(), dropped: (0, 0) })
    }

    fn sh(&self, args: &[&str], envs: &[(&str, &str)]) -> (Option<i32>, String) {
        match Command::new(&self.exe).args(args).envs(envs.iter().copied()).current_dir(&self.root).output() {
            Ok(o) => (o.status.code(), format!("{}{}", String::from_utf8_lossy(&o.stdout), String::from_utf8_lossy(&o.stderr))),
            Err(e) => (None, e.to_string()),
        }
    }

    fn args_with(&self, args: &[&str], envs: &[(&str, &str)]) -> Result<String, String> {
        match self.sh(args, envs) {
            (Some(0), text) => Ok(text),
            (code, text) => Err(format!("openrs {} exited {}\n{}", args.join(" "), code.map_or("on a signal".into(), |c| c.to_string()), tail(&text, 20))),
        }
    }

    fn args(&self, args: &[&str]) -> Result<String, String> {
        self.args_with(args, &[])
    }

    fn openrs(&self, line: &str) -> Result<String, String> {
        self.args(&line.split_whitespace().collect::<Vec<_>>())
    }

    fn data(&self, line: &str) -> Result<Value, String> {
        let t = self.openrs(&format!("do {line}"))?;
        Ok(serde_json::from_str(t.trim()).unwrap_or_else(|_| Value::String(t.trim().into())))
    }

    fn path(&self, name: &str) -> String {
        self.out.join(name).display().to_string()
    }
}

pub fn tail(text: &str, n: usize) -> String {
    let lines: Vec<&str> = text.lines().collect();
    lines[lines.len().saturating_sub(n)..].join("\n")
}

fn last(text: &str) -> String {
    text.lines().rev().find(|l| !l.trim().is_empty()).unwrap_or("").to_string()
}

pub fn milestones(log: &str) -> Vec<(String, u64)> {
    log.lines()
        .filter_map(|l| {
            let (name, ms) = l.split_once("[Milestone] ")?.1.rsplit_once(' ')?;
            Some((name.to_string(), ms.strip_suffix("ms")?.parse().ok()?))
        })
        .collect()
}

pub fn world_ms(log: &str) -> Option<u64> {
    milestones(log).into_iter().rev().find(|m| m.0 == "first world frame").map(|m| m.1)
}

pub fn totals(text: &str) -> Option<Map<String, Value>> {
    let words: Vec<&str> = text.lines().rev().find(|l| l.starts_with("clients "))?.split_whitespace().collect();
    Some(words.chunks(2).filter_map(|c| Some((c[0].to_string(), json!(c.get(1)?.parse::<u64>().ok()?)))).collect())
}

pub fn shot(text: &str) -> Result<String, String> {
    let p = text.lines().find_map(|l| l.strip_prefix("screenshot: ")).ok_or("look printed no screenshot line")?;
    if std::fs::metadata(p).is_ok_and(|m| m.len() > 0) { Ok(p.to_string()) } else { Err(format!("screenshot not written: {p}")) }
}

fn find(list: &Value, f: impl Fn(&Value) -> bool) -> Option<&Value> {
    list.as_array()?.iter().find(|v| f(v))
}

fn fleet_dir() -> PathBuf {
    std::env::var_os("OPENRS_STATE").map(PathBuf::from).unwrap_or_else(|| PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join("openrs-fleet"))
}

fn doctor(l: &mut Live) -> Result<String, String> {
    match l.sh(&["doctor"], &[]) {
        (Some(0), t) => Ok(last(&t)),
        (_, t) => Err(t.lines().filter(|x| x.starts_with("FAIL")).collect::<Vec<_>>().join("\n")),
    }
}

fn down_before(l: &mut Live) -> Result<String, String> {
    l.openrs("fleet down")?;
    l.openrs("down").map(|_| "no client or fleet left running".into())
}

fn reset_player(l: &mut Live) -> Result<String, String> {
    if l.openrs(&format!("player show {ACCOUNT}")).is_err() {
        return Ok("new player, created by up".into());
    }
    l.openrs(&format!("player clear {ACCOUNT} inv"))?;
    l.openrs(&format!("player pos {ACCOUNT} 3222 3218 0")).map(|_| "inventory cleared, placed at 3222 3218".into())
}

fn up(l: &mut Live) -> Result<String, String> {
    let t = l.openrs("up -Dbot.enabled=true -Dbot.scripts.sync=false")?;
    if !t.contains("ready") {
        return Err(format!("up did not report ready\n{}", tail(&t, 10)));
    }
    Ok(t.lines().filter(|l| l.starts_with("rebuilt") || l.starts_with("ready") || l.starts_with("timings")).collect::<Vec<_>>().join("\n"))
}

fn time_to_world(l: &mut Live) -> Result<String, String> {
    let log = std::fs::read_to_string(display::dir().join("client.log")).map_err(|e| format!("client.log: {e}"))?;
    let ms = world_ms(&log).ok_or("no [Milestone] first world frame line in client.log")?;
    l.metrics.insert("milestones".into(), Value::Object(milestones(&log).into_iter().map(|(k, v)| (k, json!(v))).collect()));
    l.metrics.insert("time_to_world_ms".into(), json!(ms));
    Ok(format!("first world frame after {ms} ms"))
}

fn reads(l: &mut Live) -> Result<String, String> {
    l.openrs("do tick ; state ; pos ; skills ; inventory ; bank ; varp 0 ; varbit 0 ; interface ; chat 5 ; npcs ; objects ; ground ; describe ; help").map(|t| format!("{} results", t.lines().count()))
}

fn teleport(l: &mut Live) -> Result<String, String> {
    l.openrs("do command tele 3222 3218 0 ; wait-until pos 3222 3218 2 timeout=30000 ; wait-tick 2").map(|t| last(&t))
}

fn walk(l: &mut Live) -> Result<String, String> {
    l.openrs("do walk 3225 3219 ; wait-until pos 3225 3219 1 timeout=20000").map(|t| last(&t))
}

fn waits(l: &mut Live) -> Result<String, String> {
    l.openrs("do wait-tick 1 ; wait-idle 1 timeout=20000 ; wait-until tick 1 ; wait-until idle 1 timeout=20000 ; wait-until state LOGGED_INGAME timeout=5000 ; wait-until pos 3225 3219 3 timeout=5000").map(|t| format!("{} waits", t.lines().count()))
}

fn chat(l: &mut Live) -> Result<String, String> {
    l.openrs("do chat openrs-verify ; wait-until chat openrs-verify timeout=10000").map(|t| last(&t))
}

fn give_item(l: &mut Live) -> Result<String, String> {
    l.openrs(&format!("do command item {LOGS} 1 ; wait-until inv {LOGS} 1 timeout=10000")).map(|t| last(&t))
}

fn widget_drop(l: &mut Live) -> Result<String, String> {
    let inv = l.data("inventory")?;
    let slot = find(&inv, |i| i["id"] == 1511).and_then(|i| i["slot"].as_i64()).ok_or("logs not in the inventory")?;
    l.openrs(&format!("do widget 679 0 8 {slot} {LOGS}"))?;
    for _ in 0..10 {
        l.openrs("do wait-tick 1")?;
        if let Some(g) = find(&l.data("ground")?, |i| i["id"] == 1511) {
            l.dropped = (g["x"].as_i64().unwrap_or(0), g["y"].as_i64().unwrap_or(0));
            return Ok(format!("widget 679 0 op 8 dropped the logs at {} {}", l.dropped.0, l.dropped.1));
        }
    }
    Err(format!("widget 679 0 op 8 slot {slot}: the logs never showed on the ground"))
}

fn take(l: &mut Live) -> Result<String, String> {
    let (x, y) = l.dropped;
    l.openrs(&format!("do take {LOGS} {x} {y} ; wait-until inv {LOGS} 1 timeout=15000")).map(|t| last(&t))
}

fn click(l: &mut Live, list: &str, cmd: &str) -> Result<String, String> {
    let me = l.data("pos")?;
    let (px, py) = (me["x"].as_i64().unwrap_or(0), me["y"].as_i64().unwrap_or(0));
    let all = l.data(list)?;
    let mut named: Vec<Value> = all.as_array().into_iter().flatten().filter(|v| v["name"].as_str().is_some_and(|n| !n.is_empty() && n != "null")).cloned().collect();
    if named.is_empty() {
        return Err(format!("{list} is empty"));
    }
    named.sort_by_key(|v| (v["x"].as_i64().unwrap_or(0) - px).abs().max((v["y"].as_i64().unwrap_or(0) - py).abs()));
    let mut tried = Vec::new();
    for target in named.iter().take(8) {
        if l.data(&format!("{cmd} {} 1", target["id"]))? == Value::Bool(true) {
            l.openrs("do wait-idle 2 timeout=20000")?;
            return Ok(format!("{cmd} option 1 on {}#{} at {} {}", target["name"].as_str().unwrap_or(""), target["id"], target["x"], target["y"]));
        }
        tried.push(format!("{}#{}", target["name"].as_str().unwrap_or(""), target["id"]));
    }
    Err(format!("{cmd} answered false for the nearest {}", tried.join(", ")))
}

fn npc(l: &mut Live) -> Result<String, String> {
    click(l, "npcs", "npc")
}

fn object(l: &mut Live) -> Result<String, String> {
    click(l, "objects", "object")
}

fn running(l: &Live) -> Result<usize, String> {
    Ok(l.data("state")?["scripts"].as_array().map_or(0, Vec::len))
}

fn scripts(l: &mut Live) -> Result<String, String> {
    let list = l.data("scripts")?;
    let id = find(&list, |s| s["id"].is_string()).and_then(|s| s["id"].as_str()).ok_or("no scripts listed, the client needs -Dbot.enabled=true and scripts in ~/.darkan/scripts")?.to_string();
    l.args(&["do", "run", &id])?;
    l.openrs("do wait-tick 2")?;
    let during = running(l)?;
    l.openrs("do stop all ; wait-tick 1")?;
    match (during, running(l)?) {
        (0, _) => Err(format!("script {id} never showed as running")),
        (_, 0) => Ok(format!("ran and stopped {id}")),
        (_, n) => Err(format!("{n} scripts still running after stop all")),
    }
}

fn look_at(l: &Live, name: &str, envs: &[(&str, &str)]) -> Result<(String, String), String> {
    let t = l.args_with(&["look", &l.path(name)], envs)?;
    Ok((shot(&t)?, t))
}

fn look(l: &mut Live) -> Result<String, String> {
    look_at(l, "look.png", &[]).map(|r| r.0)
}

fn look_display(l: &mut Live) -> Result<String, String> {
    let (p, _) = look_at(l, "look-display.png", &[("OPENRS_LOOK_VIA", "display")])?;
    if !l.openrs("trail 5")?.contains("\"via\":\"display\"") {
        return Err("the trail has no shot via display".into());
    }
    Ok(format!("display fallback wrote {p}"))
}

fn iface(l: &Live, rest: &str) -> Result<String, String> {
    l.openrs(&format!("player iface {ACCOUNT} {rest}"))
}

fn iface_open(l: &mut Live) -> Result<String, String> {
    iface(l, "open 275 CENTRAL")?;
    l.openrs("do wait-until interface 275 timeout=15000").map(|t| format!("275 opened live {}", last(&t)))
}

fn iface_text(l: &mut Live) -> Result<String, String> {
    iface(l, "text 275 2 openrs-verify")?;
    l.openrs("do wait-tick 3")?;
    let (p, t) = look_at(l, "iface-text.png", &[])?;
    match t.lines().find(|x| x.starts_with("interfaces")) {
        Some(x) if x.contains("275") => Ok(format!("275 on screen in {p}")),
        x => Err(format!("look does not list 275: {}", x.unwrap_or("no interfaces line"))),
    }
}

fn iface_item_model(l: &mut Live) -> Result<String, String> {
    iface(l, &format!("set 275 3 item {LOGS} 5"))?;
    iface(l, "set 275 4 model 2")?;
    l.openrs("do wait-tick 3")?;
    if l.data("state")?["gameState"] != "LOGGED_INGAME" {
        return Err("client left the world after the item and model packets".into());
    }
    let entry = iface(l, "show 275")?;
    if !(entry.contains("\"item\"") && entry.contains("\"model\"")) {
        return Err(format!("player file lacks the item or model state\n{}", tail(&entry, 10)));
    }
    look_at(l, "iface-item-model.png", &[]).map(|(p, _)| format!("item and model applied, {p}"))
}

fn iface_close(l: &mut Live) -> Result<String, String> {
    iface(l, "close 275")?;
    l.openrs("do wait-until closed 275 timeout=15000").map(|t| last(&t))
}

fn size(p: &str) -> u64 {
    std::fs::metadata(p).map_or(0, |m| m.len())
}

fn dump_frame(l: &mut Live) -> Result<String, String> {
    let file = l.path("frame.orf");
    let _ = std::fs::remove_file(&file);
    l.args(&["do", "dump-frame", &file])?;
    match size(&file) {
        0 => Err(format!("dump-frame wrote nothing to {file}")),
        n => Ok(format!("{n} bytes in {file}")),
    }
}

fn renderer(root: &Path) -> Option<PathBuf> {
    std::env::var_os("OPENRS_WEB_EXE").map(PathBuf::from).or_else(|| ["release", "debug"].iter().map(|p| root.join("code/renderer/target").join(p).join("renderer-app")).find(|p| p.is_file()))
}

fn replay(l: &mut Live) -> Result<String, String> {
    let exe = renderer(&l.root).ok_or("no renderer-app: cd code/renderer && cargo build --release")?;
    let png = l.path("replay.png");
    let _ = std::fs::remove_file(&png);
    let o = Command::new(&exe).args(["--replay", &l.path("frame.orf"), "--out", &png]).output().map_err(|e| format!("{}: {e}", exe.display()))?;
    let text = format!("{}{}", String::from_utf8_lossy(&o.stdout), String::from_utf8_lossy(&o.stderr));
    if !o.status.success() || size(&png) == 0 {
        return Err(format!("renderer replay failed\n{}", tail(&text, 10)));
    }
    Ok(format!("{} -> {png}", text.lines().next().unwrap_or("")))
}

fn twin(l: &mut Live) -> Result<String, String> {
    let out = l.path("twin");
    let (code, text) = l.sh(&["twin", "lumbridge", "--out", &out], &[]);
    if !matches!(code, Some(0 | 1)) {
        return Err(format!("twin errored {code:?}\n{}", tail(&text, 20)));
    }
    let res: Value = std::fs::read_to_string(format!("{out}/twin.json")).ok().and_then(|t| serde_json::from_str(&t).ok()).ok_or("twin wrote no twin.json")?;
    let ssim = res["pixels"]["ssim"].as_f64().ok_or("twin.json has no ssim")?;
    if res["serialized"]["identical"] != true {
        return Err(format!("the browser's frame dump differs from the client's frame\n{}", tail(&text, 12)));
    }
    if !std::fs::metadata(format!("{out}/twin.png")).is_ok_and(|m| m.len() > 0) {
        return Err("twin wrote no side by side image".into());
    }
    l.metrics.insert("twin".into(), json!({"ssim": ssim, "psnr": res["pixels"]["psnr_db"], "verdict": res["verdict"]}));
    Ok(format!("{out}/twin.png, ssim {ssim}, serialized frames identical"))
}

fn twin_multiframe(l: &mut Live) -> Result<String, String> {
    let out = l.path("twin-multiframe");
    let (code, text) = l.sh(&["twin", "lumbridge", "--frames", "3", "--out", &out], &[]);
    if !matches!(code, Some(0 | 1)) { return Err(format!("multiframe twin errored {code:?}\n{}", tail(&text, 12))); }
    let report: Value = std::fs::read_to_string(format!("{out}/twin.json")).ok().and_then(|t| serde_json::from_str(&t).ok()).ok_or("multiframe twin wrote no report")?;
    if report["frames"] != 3 || report["summary"]["all_serialized_identical"] != true || report["frame_records"].as_array().map_or(0, |a| a.len()) != 3 {
        return Err(format!("multiframe serialization check failed: {}", report["summary"]));
    }
    l.metrics.insert("twin_multiframe".into(), report.clone());
    if !std::path::Path::new(&format!("{out}/twin-frames.png")).is_file() { return Err("multiframe contact sheet missing".into()); }
    Ok(format!("three recorded frames, report and contact sheet at {out}"))
}

fn equiv(l: &mut Live) -> Result<String, String> {
    let out = l.path("equiv");
    let (code, text) = l.sh(&["equiv", "--live", "--cold", "--out", &out], &[]);
    if !matches!(code, Some(0 | 1)) {
        return Err(format!("equiv errored {code:?}\n{}", tail(&text, 20)));
    }
    let res: Value = std::fs::read_to_string(l.out.join("equiv/equiv.json")).ok().and_then(|t| serde_json::from_str(&t).ok()).ok_or("equiv wrote no equiv.json")?;
    if res["mode"] != "replay" {
        return Err(format!("equiv ran in mode {} instead of replay on the dumped frame", res["mode"]));
    }
    l.metrics.insert("equiv".into(), json!({"verdict": res["verdict"], "score": res["score"]}));
    Ok(format!("{} score {} on a dumped frame", res["verdict"].as_str().unwrap_or("?"), res["score"]))
}

fn renderdoc_retry(l: &mut Live) -> Result<String, String> {
    let (code, text) = l.sh(&["capture", "exec", "--api", "vulkan", "--frame", "1", "--retries", "2", "--out", &l.path("retry-fail"), "--", "false"], &[]);
    if code == Some(0) || !text.contains("capture attempt 1 failed") {
        return Err(format!("a failing capture did not take the retry path\n{}", tail(&text, 10)));
    }
    l.args(&["capture", "web", "--frame", "3", "--retries", "2", "--out", &l.path("retry-web"), "--", "--present", "--frames", "8"]).map(|_| "failing capture retried, renderer capture succeeded with --retries".into())
}

fn down(l: &mut Live) -> Result<String, String> {
    l.openrs("down").map(|_| "client stopped".into())
}

fn fleet_up(l: &mut Live) -> Result<String, String> {
    l.openrs("fleet up 9 --no-build").map(|t| last(&t))
}

fn fleet_time_to_world(l: &mut Live) -> Result<String, String> {
    let mut each = Map::new();
    for n in 1..=9 {
        let log = std::fs::read_to_string(fleet_dir().join(format!("slot{n}.log"))).unwrap_or_default();
        each.insert(n.to_string(), json!(world_ms(&log).ok_or(format!("slot {n} has no first world frame milestone"))?));
    }
    let max = each.values().filter_map(Value::as_u64).max().unwrap_or(0);
    l.metrics.insert("fleet_time_to_world_ms".into(), Value::Object(each));
    Ok(format!("slowest slot reached the world after {max} ms"))
}

fn fleet_do(l: &mut Live) -> Result<String, String> {
    let t = std::time::Instant::now();
    let l = &*l;
    let errs: Vec<String> = std::thread::scope(|s| {
        let all: Vec<_> = (1..=9).map(|n| s.spawn(move || l.args(&["fleet", "do", &n.to_string(), "pos", ";", "tick"]).err())).collect();
        all.into_iter().filter_map(|h| h.join().unwrap_or(Some("panicked".into()))).collect()
    });
    let ms = t.elapsed().as_millis();
    if errs.is_empty() { Ok(format!("9 slots answered in parallel in {ms} ms")) } else { Err(errs.join("\n")) }
}

fn fleet_compare(l: &mut Live) -> Result<String, String> {
    let t = std::time::Instant::now();
    let r = &*l;
    let dirs: Vec<String> = (1..=9).map(|n| r.path(&format!("compare/s{n}"))).collect();
    let errs: Vec<String> = std::thread::scope(|s| {
        let all: Vec<_> = dirs.iter().enumerate().map(|(i, d)| s.spawn(move || r.args(&["fleet", "compare", &(i + 1).to_string(), d]).err())).collect();
        all.into_iter().filter_map(|h| h.join().unwrap_or(Some("panicked".into()))).collect()
    });
    if !errs.is_empty() {
        return Err(errs.join("\n"));
    }
    let ssim: Vec<f64> = dirs.iter().map(|d| std::fs::read_to_string(format!("{d}/compare.json")).ok().and_then(|t| serde_json::from_str::<Value>(&t).ok()).and_then(|v| v["pixels"]["ssim"].as_f64())).collect::<Option<_>>().ok_or("a slot wrote no compare.json")?;
    l.metrics.insert("fleet_compare_ssim".into(), json!(ssim));
    Ok(format!("9 slots compared in parallel in {} ms, ssim {}", t.elapsed().as_millis(), ssim.iter().map(|s| format!("{s:.3}")).collect::<Vec<_>>().join(" ")))
}

fn fleet_status(l: &mut Live) -> Result<String, String> {
    let t = l.openrs("fleet status")?;
    let m = totals(&t).ok_or("fleet status printed no totals line")?;
    if m.get("clients") != Some(&json!(9)) || m.get("total_pss_mb").and_then(Value::as_u64).unwrap_or(0) == 0 {
        return Err(format!("expected 9 clients with memory totals\n{}", last(&t)));
    }
    let line = last(&t);
    l.metrics.insert("fleet_memory".into(), Value::Object(m));
    Ok(line)
}

fn fleet_down(l: &mut Live) -> Result<String, String> {
    l.openrs("fleet down").map(|t| format!("{} lines", t.lines().count()))
}

fn down_all(l: &mut Live) -> Result<String, String> {
    l.openrs("down all").map(|_| "client, display and local server stopped".into())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn milestones_and_world_time() {
        let log = "noise\n[2026-10-20 01:29:15.807] [Milestone] cache ready 2530ms\n[load] done total=2719\n[Milestone] first world frame 8259ms\n[Milestone] bad line\n";
        assert_eq!(milestones(log), [("cache ready".to_string(), 2530), ("first world frame".to_string(), 8259)]);
        assert_eq!(world_ms(log), Some(8259));
        assert_eq!(world_ms("[Milestone] cache ready 1ms"), None);
    }

    #[test]
    fn fleet_totals_line() {
        let t = "slot role\n1 a b 2\nclients 9 rss_mb 7400 pss_mb 6800 cpu_pct 300 xwayland_rss_mb 180 total_pss_mb 6950\n";
        let m = totals(t).unwrap();
        assert_eq!(m["clients"], 9);
        assert_eq!(m["total_pss_mb"], 6950);
        assert!(totals("nothing").is_none());
    }

    #[test]
    fn screenshot_line() {
        assert!(shot("describe only").is_err());
        assert!(shot("screenshot: /no/such/file.png").unwrap_err().contains("not written"));
        let f = std::env::temp_dir().join(format!("verify-shot-{}.png", std::process::id()));
        std::fs::write(&f, b"png").unwrap();
        assert_eq!(shot(&format!("screenshot: {}\n0 LOGGED_INGAME", f.display())).unwrap(), f.display().to_string());
        std::fs::remove_file(f).unwrap();
    }

    #[test]
    fn tail_keeps_last_lines() {
        assert_eq!(tail("a\nb\nc", 2), "b\nc");
        assert_eq!(tail("a", 5), "a");
        assert_eq!(last("x\ny\n\n"), "y");
    }
}
