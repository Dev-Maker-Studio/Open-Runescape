use crate::analyze;
use crate::env::{self, Res};
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::time::Duration;

struct Opts {
    kind: String,
    frame: Option<String>,
    seconds: u64,
    out: PathBuf,
    display: Option<String>,
    retries: u32,
    exe: Option<PathBuf>,
    api: String,
    rest: Vec<String>,
}

fn field(json: &str, key: &str) -> Option<String> {
    let rest = json.split(&format!("\"{key}\": ")).nth(1)?;
    Some(match rest.strip_prefix('"') {
        Some(s) => s.split('"').next()?,
        None => rest.split([',', '}']).next()?.trim(),
    }
    .to_string())
}

fn parse(kind: &str, args: &[String]) -> Res<Opts> {
    let mut o = Opts { kind: kind.into(), frame: None, seconds: 60, out: env::root().join("code/harness/target/capture").join(kind), display: None, retries: 1, exe: None, api: "vulkan".into(), rest: vec![] };
    let mut it = args.iter();
    while let Some(a) = it.next() {
        let mut val = || it.next().cloned().ok_or(format!("{a} needs a value"));
        match a.as_str() {
            "--frame" => o.frame = Some(val()?),
            "--seconds" => o.seconds = val()?.parse().map_err(|_| "bad --seconds")?,
            "--out" => o.out = val()?.into(),
            "--display" => o.display = Some(val()?),
            "--retries" => o.retries = val()?.parse().map_err(|_| "bad --retries")?,
            "--jar" | "--exe" => o.exe = Some(val()?.into()),
            "--api" => o.api = val()?,
            "--" => {
                o.rest = it.cloned().collect();
                break;
            }
            _ => return Err(format!("unknown option {a}")),
        }
    }
    Ok(o)
}

pub fn display(given: Option<String>) -> Res<(String, Option<Child>)> {
    if let Some(d) = given {
        return Ok((d, None));
    }
    let state = std::env::var("XDG_RUNTIME_DIR").map(|d| Path::new(&d).join("openrs/xserver")).ok().and_then(|p| std::fs::read_to_string(p).ok());
    if let Some(s) = state {
        let mut p = s.split_whitespace();
        if let (Some(d), Some(pid)) = (p.next(), p.next()) {
            if Path::new(&format!("/proc/{pid}")).exists() {
                return Ok((d.into(), None));
            }
        }
    }
    let mut x = Command::new("Xvfb").args(["-displayfd", "1", "-screen", "0", "1280x900x24", "-nolisten", "tcp", "+extension", "GLX"]).stdout(Stdio::piped()).stderr(Stdio::null()).spawn().map_err(|e| format!("Xvfb: {e}"))?;
    let mut n = String::new();
    BufReader::new(x.stdout.take().unwrap()).read_line(&mut n).map_err(|e| e.to_string())?;
    Ok((format!(":{}", n.trim()), Some(x)))
}

fn newest(dir: PathBuf, accept: impl Fn(&Path) -> bool) -> Option<PathBuf> {
    std::fs::read_dir(dir).ok()?.flatten().map(|e| e.path()).filter(|p| accept(p)).max_by_key(|p| p.metadata().and_then(|m| m.modified()).ok())
}

fn target(o: &Opts) -> Res<(Vec<String>, Vec<(&'static str, String)>, bool)> {
    let root = env::root();
    match o.kind.as_str() {
        "client" => {
            let jar = o.exe.clone().or_else(|| newest(root.join("code/client/build/libs"), |p| p.to_string_lossy().ends_with("-all.jar"))).ok_or("no shaded jar in code/client/build/libs, run ./gradlew shadowJar")?;
            let mut c = ["java", "-XX:+UseZGC", "-Xmx4g", "-Xms1g", "-Dtoolkit=3", "-Drender.allowsoftgl=true", "-Drender.renderdoc=true"].map(String::from).to_vec();
            c.extend(o.rest.iter().filter(|a| a.starts_with("-D") || a.starts_with("-X")).cloned());
            c.extend(["-jar".into(), jar.to_string_lossy().into_owned()]);
            c.extend(o.rest.iter().filter(|a| !a.starts_with("-D") && !a.starts_with("-X")).cloned());
            Ok((c, vec![("PULSE_SERVER", "unix:/nonexistent".into())], true))
        }
        "web" => {
            let exe = o.exe.clone().or_else(|| std::env::var("OPENRS_WEB_EXE").ok().map(Into::into)).or_else(|| ["release", "debug"].iter().map(|p| root.join("code/renderer/target").join(p).join("renderer-app")).find(|f| f.is_file())).ok_or("no renderer binary: pass --exe PATH or build code/renderer")?;
            let mut c = vec![exe.to_string_lossy().into_owned()];
            c.extend(o.rest.iter().cloned());
            Ok((c, vec![("WGPU_BACKEND", "vulkan".into()), ("WINIT_UNIX_BACKEND", "x11".into())], false))
        }
        _ => {
            let c = o.rest.clone();
            if c.is_empty() {
                return Err("exec needs -- command args".into());
            }
            let env = if o.api == "gl" { vec![] } else { vec![("WGPU_BACKEND", "vulkan".to_string()), ("WINIT_UNIX_BACKEND", "x11".to_string())] };
            Ok((c, env, false))
        }
    }
}

fn kill(pid: &str) {
    let Ok(p) = pid.trim().parse::<u32>() else { return };
    if p <= 1 || p == std::process::id() {
        return;
    }
    let _ = Command::new("kill").arg(p.to_string()).stderr(Stdio::null()).status();
}

pub struct Live {
    pub ident: String,
    pub pid: String,
}

fn spawn(o: &Opts, disp: &str, rdc_file: &Path, log: &Path) -> Res<String> {
    let (cmd, envs, jvm) = target(o)?;
    let logf = std::fs::File::create(log).map_err(|e| e.to_string())?;
    let _ = std::fs::remove_file(rdc_file);
    let mut c = env::rdc(&["capture", "-o"]);
    c.arg(rdc_file).args(["--json", "--timeout", &(o.seconds + 60).to_string()]);
    match &o.frame {
        Some(f) => c.args(["--frame", f]),
        None => c.arg("--trigger"),
    };
    c.arg("--").args(&cmd);
    c.envs(envs).env("DISPLAY", disp).env_remove("WAYLAND_DISPLAY").stdin(Stdio::null()).stdout(logf.try_clone().unwrap()).stderr(logf);
    if jvm {
        c.current_dir(env::root()).env("LD_PRELOAD", env::deepbind());
    }
    let st = c.status().map_err(|e| e.to_string())?;
    let text = std::fs::read_to_string(log).unwrap_or_default();
    let json = text.lines().rev().find(|l| l.starts_with("{\"success\"")).unwrap_or("").to_string();
    if !json.contains("\"success\": true") {
        kill(&field(&json, "pid").unwrap_or_default());
        return Err(format!("capture failed ({st}): {}", field(&json, "error").filter(|e| !e.is_empty()).unwrap_or_else(|| format!("see {}", log.display()))));
    }
    Ok(json)
}

pub fn trigger(ident: &str, rdc_file: &Path) -> Res {
    env::out(&mut env::rdc(&["attach", ident]))?;
    env::out(&mut env::rdc(&["capture-trigger"]))?;
    for _ in 0..60 {
        let l = env::out(&mut env::rdc(&["capture-list", "--json", "--timeout", "1"]))?;
        if let Some(p) = field(&l, "path") {
            return std::fs::rename(&p, rdc_file).or_else(|_| std::fs::copy(&p, rdc_file).map(drop)).map_err(|e| format!("{p}: {e}"));
        }
    }
    Err("no capture received".into())
}

pub fn launch(kind: &str, rest: Vec<String>, exe: Option<PathBuf>, out: &Path, display: &str) -> Res<Live> {
    let mut o = parse(kind, &[])?;
    (o.rest, o.exe) = (rest, exe);
    std::fs::create_dir_all(out).map_err(|e| e.to_string())?;
    let json = spawn(&o, display, &out.join("frame.rdc"), &out.join("app.log"))?;
    Ok(Live { ident: field(&json, "ident").ok_or("no ident")?, pid: field(&json, "pid").unwrap_or_default() })
}

pub fn web(exe: Option<PathBuf>, rest: Vec<String>, frame: &str, out: &Path, display: &str) -> Res {
    let mut o = parse("web", &["--frame".into(), frame.into()])?;
    (o.exe, o.rest) = (exe, rest);
    std::fs::create_dir_all(out).map_err(|e| e.to_string())?;
    once(&o, display, &out.join("frame.rdc"), &out.join("app.log"))
}

pub fn stop(pid: &str) {
    kill(pid);
}

fn once(o: &Opts, disp: &str, rdc_file: &Path, log: &Path) -> Res {
    let json = spawn(o, disp, rdc_file, log)?;
    let pid = field(&json, "pid").unwrap_or_default();
    if o.frame.is_some() {
        let p = field(&json, "path").ok_or("no capture path")?;
        kill(&pid);
        return std::fs::rename(&p, rdc_file).map_err(|e| format!("{p}: {e}"));
    }
    let ident = field(&json, "ident").ok_or("no ident")?;
    std::thread::sleep(Duration::from_secs(o.seconds));
    let r = trigger(&ident, rdc_file);
    kill(&pid);
    r
}

pub fn run(kind: &str, args: &[String]) -> Res {
    let o = parse(kind, args)?;
    super::setup::verify(false)?;
    std::fs::create_dir_all(&o.out).map_err(|e| e.to_string())?;
    let (d, mut xvfb) = display(o.display.clone())?;
    let (rdc_file, log) = (o.out.join("frame.rdc"), o.out.join("app.log"));
    let mut res = Err(String::new());
    for attempt in 1..=o.retries.max(1) {
        res = once(&o, &d, &rdc_file, &log);
        match &res {
            Ok(()) => break,
            Err(e) if attempt < o.retries => {
                eprintln!("capture attempt {attempt} failed: {e}");
                std::thread::sleep(Duration::from_secs(5));
            }
            Err(_) => {}
        }
    }
    if let Some(x) = xvfb.as_mut() {
        let _ = x.kill();
        let _ = x.wait();
    }
    res.map_err(|e| format!("{e}; the client needs a core-profile OpenGL context and the web renderer needs Vulkan, see {}", log.display()))?;
    analyze::run(&rdc_file, o.out)
}

pub fn selftest() -> Res {
    super::setup::verify(false)?;
    let apps = env::tools().join("apps");
    let out = env::root().join("code/harness/target/capture/selftest");
    let gl = apps.join("gl_scene.c");
    let gl_bin = out.join("gl_scene");
    std::fs::create_dir_all(&out).map_err(|e| e.to_string())?;
    env::run(Command::new("gcc").args(["-O1", "-o"]).arg(&gl_bin).arg(&gl).args(["-lGL", "-lX11"]))?;
    env::run(Command::new("cargo").args(["build", "--release", "-q", "--manifest-path"]).arg(apps.join("wgpu_scene/Cargo.toml")))?;
    let web_bin = apps.join("wgpu_scene/target/release/wgpu_scene");
    let args = |bin: &Path, api: &str, dir: &str| ["--api", api, "--frame", "5", "--out", &out.join(dir).to_string_lossy(), "--", &bin.to_string_lossy()].map(String::from).to_vec();
    run("exec", &args(&gl_bin, "gl", "client"))?;
    run("exec", &args(&web_bin, "vulkan", "web"))?;
    analyze::diff(&out.join("client"), &out.join("web"), &[out.join("diff").to_string_lossy().into_owned()])
}

#[cfg(test)]
#[path = "capture_tests.rs"]
mod tests;
