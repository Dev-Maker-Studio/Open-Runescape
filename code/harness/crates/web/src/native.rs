use crate::{page::Page, state};
use serde_json::{json, Value};
use std::{path::{Path, PathBuf}, process::{Command, Stdio}, time::{Duration, Instant}};

const NAME: &str = "openrs-native-host";

pub struct Options {
    control: PathBuf,
    spool: PathBuf,
    port: u16,
    fps: u32,
}

pub fn take(args: &[String]) -> Result<(Vec<String>, Option<Options>), String> {
    let mut rest = Vec::new();
    let (mut enabled, mut configured, mut control, mut spool, mut port, mut fps) = (false, false, None, None, 8101u16, 5u32);
    let mut a = args.iter();
    while let Some(k) = a.next() {
        match k.as_str() {
            "--native" => enabled = true,
            "--native-control" | "--native-spool" | "--native-port" | "--native-fps" => {
                configured = true;
                let v = a.next().ok_or_else(|| format!("{k} requires a value"))?;
                match k.as_str() {
                    "--native-control" => control = Some(PathBuf::from(v)),
                    "--native-spool" => spool = Some(PathBuf::from(v)),
                    "--native-fps" => fps = v.parse().map_err(|_| "invalid native fps")?,
                    _ => port = v.parse().map_err(|_| "invalid native port")?,
                }
            }
            _ => rest.push(k.clone()),
        }
    }
    if !enabled {
        if configured { return Err("native options require --native".into()); }
        return Ok((rest, None));
    }
    if rest.iter().any(|a| a == "--game" || !a.starts_with("--")) { return Err("--native cannot be combined with a scene or --game".into()); }
    if port == 0 { return Err("invalid native port".into()); }
    if !(1..=30).contains(&fps) { return Err("native fps must be from 1 to 30".into()); }
    Ok((rest, Some(Options { control: control.ok_or("--native requires --native-control SOCKET")?, spool: spool.unwrap_or_else(|| state::dir().join("native-spool")), port, fps })))
}

fn record() -> Option<Value> {
    serde_json::from_slice(&std::fs::read(state::dir().join("native.json")).ok()?).ok()
}

pub fn host() -> Option<(u32, u16)> {
    let v = record()?;
    let pid = u32::try_from(v["pid"].as_u64()?).ok()?;
    let port = u16::try_from(v["port"].as_u64()?).ok()?;
    state::alive(pid, NAME).then_some((pid, port))
}

pub fn crashed() -> bool { record().is_some() && host().is_none() }

fn ensure(root: &Path) -> Result<PathBuf, String> {
    let bin = root.join("code/native-host/target/release").join(NAME);
    let source = ["code/native-host/src", "code/native-host/Cargo.toml", "code/renderer/src"].iter().filter_map(|s| fresh::newest(&root.join(s))).max();
    if fresh::stale(std::fs::metadata(&bin).and_then(|m| m.modified()).ok(), source).is_some()
        && !Command::new("cargo").current_dir(root.join("code/native-host")).args(["build", "--release", "-q"]).status().map_err(|e| e.to_string())?.success()
    {
        return Err("cargo build of code/native-host failed".into());
    }
    Ok(bin)
}

pub fn start(o: Options) -> Result<String, String> {
    use std::os::unix::{fs::FileTypeExt, process::CommandExt};
    if !std::fs::metadata(&o.control).is_ok_and(|m| m.file_type().is_socket()) { return Err("native control socket is missing; connect to an already running native client".into()); }
    if let (Some((_, port)), Some(v)) = (host(), record()) {
        if port == o.port && v["control"] == o.control.display().to_string() && v["spool"] == o.spool.display().to_string() && v["fps"].as_u64().unwrap_or(5) == u64::from(o.fps) { return Ok(format!("?game=native&host=127.0.0.1:{port}")); }
    }
    stop();
    let bin = ensure(&display::root()?)?;
    let log = std::fs::File::create(state::dir().join("native-host.log")).map_err(|e| e.to_string())?;
    let mut child = Command::new(bin).arg("--control").arg(&o.control).arg("--spool").arg(&o.spool).args(["--port", &o.port.to_string(), "--fps", &o.fps.to_string()]).stdin(Stdio::null()).stdout(log.try_clone().map_err(|e| e.to_string())?).stderr(log).process_group(0).spawn().map_err(|e| e.to_string())?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(5) {
        if child.try_wait().map_err(|e| e.to_string())?.is_some() { return Err("native host exited; see native-host.log".into()); }
        let ready = std::fs::read(o.spool.join("host.json")).ok().and_then(|b| serde_json::from_slice::<Value>(&b).ok()).is_some_and(|v| v["pid"] == child.id());
        if ready {
            let v = json!({"pid":child.id(),"port":o.port,"control":o.control,"spool":o.spool,"fps":o.fps});
            state::write("native.json", &v.to_string());
            println!("native host pid {} on 127.0.0.1:{}", child.id(), o.port);
            return Ok(format!("?game=native&host=127.0.0.1:{}", o.port));
        }
        std::thread::sleep(Duration::from_millis(20));
    }
    let _ = child.kill();
    let _ = child.wait();
    Err("native host did not listen within 5s".into())
}

pub fn resume() -> Result<(), String> {
    if !crashed() { return Ok(()); }
    let v = record().ok_or("missing native host state")?;
    let options = Options { control: v["control"].as_str().ok_or("missing native socket")?.into(), spool: v["spool"].as_str().ok_or("missing native spool")?.into(), port: v["port"].as_u64().and_then(|n| u16::try_from(n).ok()).ok_or("missing native port")?, fps: v["fps"].as_u64().and_then(|v| u32::try_from(v).ok()).filter(|v| (1..=30).contains(v)).unwrap_or(5) };
    state::remove("native.json");
    let query = start(options)?;
    let host = query.split("host=").nth(1).ok_or("missing native host")?;
    if state::browser().is_some() { Page::open()?.run("game", &["native".into(), host.into()])?; }
    Ok(())
}

pub fn stop() {
    if let Some((pid, _)) = host() { crate::proc::kill(pid); }
    state::remove("native.json");
}

#[cfg(test)]
mod tests {
    use super::*;
    fn args(a: &[&str]) -> Vec<String> { a.iter().map(|s| s.to_string()).collect() }
    #[test]
    fn native_options_are_separate_and_strict() {
        let (rest, o) = take(&args(&["--native", "--native-control", "/tmp/a b.sock", "--native-spool", "/tmp/spool", "--native-port", "8104", "--no-sandbox"])).unwrap();
        let o = o.unwrap();
        assert_eq!(rest, args(&["--no-sandbox"]));
        assert_eq!((o.control, o.spool, o.port), (PathBuf::from("/tmp/a b.sock"), PathBuf::from("/tmp/spool"), 8104));
        for a in [args(&["--native"]), args(&["--native-control", "/a"]), args(&["--native", "--native-control", "/a", "--game", "walker"]), args(&["--native", "--native-control", "/a", "model"])] { assert!(take(&a).is_err()); }
        assert!(take(&args(&["--game", "walker"])).unwrap().1.is_none());
    }
    #[test]
    fn native_rate_is_bounded_and_defaults_to_five() {
        assert_eq!(take(&args(&["--native", "--native-control", "/a"])).unwrap().1.unwrap().fps, 5);
        assert_eq!(take(&args(&["--native", "--native-control", "/a", "--native-fps", "10"])).unwrap().1.unwrap().fps, 10);
        for rate in ["0", "31", "bad"] {
            assert!(take(&args(&["--native", "--native-control", "/a", "--native-fps", rate])).is_err());
        }
        assert!(take(&args(&["--native-fps", "10"])).is_err());
    }

}
