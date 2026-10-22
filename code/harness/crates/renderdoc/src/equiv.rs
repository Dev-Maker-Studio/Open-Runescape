use crate::{analyze, capture, env::{self, Res}};
use std::path::{Path, PathBuf};
use std::sync::mpsc;
use std::time::{Duration, Instant};

pub const SCENES: [(&str, &str, &str); 6] = [
    ("login", "", ""),
    ("lumbridge", "3222", "3218"),
    ("varrock", "3213", "3428"),
    ("draynor", "3093", "3244"),
    ("falador", "2965", "3380"),
    ("edgeville", "3093", "3493"),
];

#[derive(Clone)]
struct Cfg {
    scene: String,
    live: bool,
    top: String,
    min_ssim: String,
    max_mismatch: String,
    reuse: bool,
    cold: bool,
    builtin: bool,
    settle: String,
    out: PathBuf,
    exe: Option<PathBuf>,
    size: Option<String>,
    pos: Option<String>,
    browser: bool,
    frames: Option<u32>,
}

fn parse(args: &[String]) -> Res<Cfg> {
    let mut c = Cfg { scene: String::new(), live: false, top: "5".into(), min_ssim: "0.99".into(), max_mismatch: "0".into(), reuse: false, cold: false, builtin: false, settle: "4".into(), out: PathBuf::new(), exe: None, size: None, pos: None, browser: false, frames: None };
    let mut it = args.iter();
    while let Some(a) = it.next() {
        let mut val = || it.next().cloned().ok_or(format!("{a} needs a value"));
        match a.as_str() {
            "--frames" => c.frames = Some(val()?.parse::<u32>().ok().filter(|n| (1..=5).contains(n)).ok_or("--frames needs a number from 1 to 5")?),
            "--live" => c.live = true,
            "--reuse" => c.reuse = true,
            "--cold" => c.cold = true,
            "--builtin" => c.builtin = true,
            "--top" => c.top = val()?,
            "--min-ssim" => c.min_ssim = val()?,
            "--max-mismatch" => c.max_mismatch = val()?,
            "--settle" => c.settle = val()?,
            "--out" => c.out = env::absolute(val()?),
            "--exe" => c.exe = Some(env::absolute(val()?)),
            "--size" => c.size = Some(val()?),
            "--target" => c.browser = match val()?.as_str() {
                "native" => false,
                "browser" => true,
                t => return Err(format!("unknown target {t}, targets: native browser")),
            },
            s if !s.starts_with("--") && c.scene.is_empty() => c.scene = s.into(),
            _ => return Err(format!("unknown option {a}")),
        }
    }
    if c.live {
        c.scene = "live".into();
        c.pos = control::send("pos", &[]).ok().map(|v| format!("{} {} {}", v["x"], v["y"], v["z"]));
    }
    isolate();
    if c.scene.is_empty() {
        return Err("equiv needs a scene or --live".into());
    }
    if c.scene != "live" && !SCENES.iter().any(|s| s.0 == c.scene) {
        return Err(format!("unknown scene {}, scenes: {}", c.scene, SCENES.map(|s| s.0).join(" ")));
    }
    if c.frames.is_some() && (c.builtin || c.reuse || c.scene == "login") {
        return Err("--frames needs a world scene with live dumps; not --builtin or --reuse".into());
    }
    if c.out.as_os_str().is_empty() {
        c.out = base().join(label(&c));
    }
    Ok(c)
}

fn label(c: &Cfg) -> String {
    if c.browser { format!("{}-browser", c.scene) } else { c.scene.clone() }
}

fn isolate() {
    std::env::set_var("OPENRS_CONTROL", std::env::var("OPENRS_EQUIV_CONTROL").unwrap_or_else(|_| format!("{}/openrs-equiv.sock", std::env::var("XDG_RUNTIME_DIR").unwrap_or_else(|_| "/tmp".into()))));
}

fn base() -> PathBuf {
    env::root().join("code/harness/target/equiv")
}

fn history_file() -> PathBuf {
    base().join("history.jsonl")
}

fn state_file() -> PathBuf {
    std::env::var("XDG_RUNTIME_DIR").map_or_else(|_| "/tmp".into(), PathBuf::from).join("openrs/equiv-client")
}

fn alive(pid: &str) -> bool {
    Path::new(&format!("/proc/{pid}")).exists()
}

fn warm() -> Option<(String, String, String)> {
    let s = std::fs::read_to_string(state_file()).ok()?;
    let mut p = s.split_whitespace().map(String::from);
    let (ident, pid, mode) = (p.next()?, p.next()?, p.next()?);
    (alive(&pid) && control::send("state", &[]).is_ok()).then_some((ident, pid, mode))
}

fn stop() {
    if let Some((_, pid, _)) = warm() {
        capture::stop(&pid);
    }
    let _ = std::fs::remove_file(state_file());
}

fn wait(what: &str, limit: u64, mut ready: impl FnMut() -> bool) -> Res {
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(limit) {
        if ready() {
            return Ok(());
        }
        std::thread::sleep(Duration::from_millis(100));
    }
    Err(format!("timed out after {limit}s waiting for {what}"))
}

fn snapshot() -> Res<(PathBuf, bool)> {
    let libs = env::root().join("code/client/build/libs");
    let src = std::fs::read_dir(&libs).map_err(|e| e.to_string())?.flatten().map(|e| e.path()).filter(|p| p.to_string_lossy().ends_with("-all.jar")).max_by_key(|p| p.metadata().and_then(|m| m.modified()).ok()).ok_or("no shaded jar, run ./gradlew shadowJar")?;
    let dst = base().join("client.jar");
    let same = |a: &Path, b: &Path| a.metadata().ok().zip(b.metadata().ok()).is_some_and(|(x, y)| x.len() == y.len() && x.modified().ok() <= y.modified().ok());
    let stale = !same(&src, &dst);
    if stale {
        std::fs::create_dir_all(base()).map_err(|e| e.to_string())?;
        env::run(std::process::Command::new("cp").arg("--reflink=auto").arg(&src).arg(&dst))?;
    }
    Ok((dst, stale))
}

fn account() -> String {
    let exe = std::env::current_exe().unwrap_or_default();
    for a in [&["account", "equivqa", "equivbot1"][..], &["create", "equivqa"], &["set", "equivqa", "chosenAccountType", "true"]] {
        let _ = std::process::Command::new(&exe).arg("player").args(a).stdout(std::process::Stdio::null()).stderr(std::process::Stdio::null()).status();
    }
    "-Dautologin=equivqa:equivbot1".into()
}

fn ensure_client(c: &Cfg, display: &str, out: &Path) -> Res<(String, String)> {
    let mode = if c.scene == "login" { "login" } else { "world" };
    let (jar, stale) = snapshot()?;
    if let Some((i, p, _)) = warm().filter(|w| w.2 == mode && !stale) {
        return Ok((i, p));
    }
    stop();
    let args = if mode == "login" {
        control::client_args(true, vec![]).into_iter().filter(|a| a.starts_with("-Dbot.")).collect()
    } else {
        control::client_args(false, vec![account()])
    };
    let live = capture::launch("client", args, Some(jar), out, display)?;
    let ready = if mode == "login" { wait("the login window", 120, || alive(&live.pid) && std::fs::read_to_string(out.join("app.log")).is_ok_and(|l| l.contains("stage=display_login_window"))) } else { wait("the world", 180, || alive(&live.pid) && control::ingame().is_some()) };
    if let Err(e) = ready {
        capture::stop(&live.pid);
        return Err(e);
    }
    let _ = std::fs::create_dir_all(state_file().parent().unwrap());
    let _ = std::fs::write(state_file(), format!("{} {} {mode}", live.ident, live.pid));
    Ok((live.ident, live.pid))
}

fn place(c: &Cfg) -> Res {
    let s = |cmd: &str, a: &[&str]| control::send(cmd, &a.iter().map(|x| x.to_string()).collect::<Vec<_>>());
    let settle = [c.settle.as_str()];
    match c.scene.as_str() {
        "login" => std::thread::sleep(Duration::from_secs(3)),
        "live" => {
            if let Some(p) = &c.pos {
                let p: Vec<&str> = p.split(' ').collect();
                s("command", &["tele", p[0], p[1], p[2]])?;
                s("wait-until", &["pos", p[0], p[1], "3", "timeout=30000"])?;
            }
            s("wait-tick", &settle)?;
        }
        name => {
            let (_, x, y) = *SCENES.iter().find(|s| s.0 == name).ok_or("unknown scene")?;
            s("command", &["tele", x, y, "0"])?;
            s("wait-until", &["pos", x, y, "3", "timeout=30000"])?;
            s("wait-idle", &["2", "timeout=30000"])?;
            s("wait-tick", &settle)?;
        }
    }
    Ok(())
}

fn client_side(c: &Cfg, display: &str, dir: &Path, orf: &mpsc::Sender<Option<PathBuf>>) -> Res<(f32, f32)> {
    let t = Instant::now();
    let r = (|| -> Res<f32> {
        let (ident, _) = ensure_client(c, display, &c.out.join("client-launch"))?;
        let ready = t.elapsed().as_secs_f32();
        place(c)?;
        std::fs::create_dir_all(dir).map_err(|e| e.to_string())?;
        let file = c.out.join("frame.orf");
        let _ = std::fs::remove_file(&file);
        capture::attach(&ident)?;
        capture::fire()?;
        let dumped = c.scene != "login" && !c.builtin && control::send("dump-frame", &[file.to_string_lossy().into_owned()]).is_ok() && file.metadata().is_ok_and(|m| m.len() > 0);
        capture::collect(&dir.join("frame.rdc"))?;
        let _ = orf.send(dumped.then_some(file));
        Ok(ready)
    })();
    let _ = orf.send(None);
    if c.cold || r.is_err() {
        stop();
    }
    let ready = r?;
    analyze::run_to(&dir.join("frame.rdc"), dir.into(), false)?;
    Ok((ready, t.elapsed().as_secs_f32()))
}

fn web_side(c: &Cfg, display: &str, dir: &Path, orf: Option<PathBuf>) -> Res<(bool, f32)> {
    let t = Instant::now();
    if c.browser {
        let mut o = capture::parse("browser", &["--out".into(), dir.to_string_lossy().into_owned(), "--display".into(), display.into()])?;
        o.orf = orf.clone();
        crate::browser::capture(&o)?;
        analyze::run_to(&dir.join("frame.rdc"), dir.into(), false)?;
        return Ok((orf.is_some(), t.elapsed().as_secs_f32()));
    }
    let mut args = vec!["--present".to_string(), "--frames".into(), "8".into()];
    if let Some(f) = &orf {
        args.extend(["--replay".into(), f.to_string_lossy().into_owned()]);
    } else {
        args.extend(["--scene".into(), "all".into()]);
        args.extend(c.size.iter().flat_map(|s| ["--size".to_string(), s.clone()]));
    }
    capture::web(c.exe.clone(), args, "3", dir, display)?;
    analyze::run_to(&dir.join("frame.rdc"), dir.into(), false)?;
    Ok((orf.is_some(), t.elapsed().as_secs_f32()))
}

fn report(c: &Cfg, replay: bool, timings: &str) -> i32 {
    let out = c.out.to_string_lossy().into_owned();
    let mut p = env::script("equiv.py", &[]);
    p.arg("report").arg(c.out.join("client")).arg(c.out.join("web")).arg(&out);
    p.args(["--scene", &label(c), "--mode", if replay { "replay" } else { "builtin" }, "--top", &c.top, "--min-ssim", &c.min_ssim, "--max-mismatch", &c.max_mismatch, "--timings", timings, "--history", &history_file().to_string_lossy()]);
    p.status().map_or(2, |s| s.code().unwrap_or(2))
}

#[path = "equiv_frames.rs"]
mod frames;

fn run_cfg(c: &Cfg) -> Res<i32> {
    if let Some(count) = c.frames {
        return frames::run(c, count);
    }
    super::setup::verify(false)?;
    let total = Instant::now();
    std::fs::create_dir_all(&c.out).map_err(|e| e.to_string())?;
    let (cdir, wdir) = (c.out.join("client"), c.out.join("web"));
    if c.reuse && cdir.join("summary.json").exists() && wdir.join("summary.json").exists() {
        let replay = c.out.join("frame.orf").exists();
        return Ok(report(c, replay, "{}"));
    }
    let display = display::display_name()?;
    let (tx, rx) = mpsc::channel();
    let (client, web) = std::thread::scope(|s| {
        let h = s.spawn(|| client_side(c, &display, &cdir, &tx));
        let orf = if c.builtin { None } else { rx.recv().ok().flatten() };
        let web = web_side(c, &display, &wdir, orf);
        (h.join().unwrap_or_else(|_| Err("client thread panicked".into())), web)
    });
    let (ready, ctotal) = client.map_err(|e| format!("client side: {e}"))?;
    let (replay, wtotal) = web.map_err(|e| format!("web side: {e}"))?;
    let timings = format!("{{\"client_ready\":{ready:.1},\"client\":{ctotal:.1},\"web\":{wtotal:.1},\"total\":{:.1}}}", total.elapsed().as_secs_f32());
    Ok(report(c, replay, &timings))
}

fn script(sub: &str, rest: &[String]) -> i32 {
    let mut p = env::script("equiv.py", &[]);
    p.arg(sub).args(["--history", &history_file().to_string_lossy()]);
    if sub == "history" {
        if let Some(s) = rest.first() {
            p.args(["--scene", s]);
        }
        if let Some(n) = rest.get(1) {
            p.args(["--count", n]);
        }
    }
    p.status().map_or(2, |s| s.code().unwrap_or(2))
}

fn test() -> i32 {
    let unit = env::tool(env::python()).arg("-m").arg("unittest").arg("discover").arg("-s").arg(env::tools().join("tests")).arg("-v").status().is_ok_and(|s| s.success());
    let selftest = capture::selftest().is_ok();
    let dir = env::root().join("code/harness/target/capture/selftest");
    let hist = env::root().join("code/harness/target/equiv/selftest-history.jsonl");
    let mut p = env::script("equiv.py", &[]);
    p.arg("report").arg(dir.join("client")).arg(dir.join("web")).arg(dir.join("equiv")).arg("--scene").arg("selftest").arg("--history").arg(hist).arg("--min-ssim").arg("0.995").arg("--max-mismatch").arg("1");
    let report = p.status().is_ok_and(|s| s.success());
    println!("equiv test: unit={unit} selftest={selftest} report={report}");
    (unit && selftest && report) as i32 ^ 1
}

pub fn run(args: &[String]) -> i32 {
    let rest = args.get(1..).unwrap_or_default();
    match args.first().map(String::as_str) {
        Some("history") => script("history", rest),
        Some("down") => {
            isolate();
            stop();
            0
        }
        Some("test") => test(),
        Some("scenes") => {
            SCENES.iter().for_each(|s| println!("{} {} {}", s.0, s.1, s.2));
            0
        }
        _ => match parse(args).and_then(|c| run_cfg(&c)) {
            Ok(code) => code,
            Err(e) => {
                eprintln!("equiv: {e}");
                2
            }
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn args(a: &[&str]) -> Vec<String> {
        a.iter().map(|s| s.to_string()).collect()
    }

    #[test]
    fn parses_scene_and_options() {
        let c = parse(&args(&["varrock", "--top", "9", "--cold", "--max-mismatch", "2"])).unwrap();
        assert_eq!((c.scene.as_str(), c.top.as_str(), c.cold, c.max_mismatch.as_str()), ("varrock", "9", true, "2"));
        assert!(c.out.ends_with("equiv/varrock"));
        assert!(!c.browser);
        let c = parse(&args(&["varrock", "--target", "browser"])).unwrap();
        assert!(c.browser && c.out.ends_with("equiv/varrock-browser"));
        assert_eq!(label(&c), "varrock-browser");
        assert!(parse(&args(&["varrock", "--target", "gl"])).is_err());
    }

    #[test]
    fn frames_are_opt_in_and_limited_to_five_live_pairs() {
        assert_eq!(parse(&args(&["lumbridge"])).unwrap().frames, None);
        assert_eq!(parse(&args(&["lumbridge", "--frames", "5"])).unwrap().frames, Some(5));
        for value in ["0", "6", "-1", "bad"] {
            assert!(parse(&args(&["lumbridge", "--frames", value])).is_err());
        }
        for extra in ["--builtin", "--reuse"] {
            assert!(parse(&args(&["lumbridge", "--frames", "2", extra])).is_err());
        }
        assert!(parse(&args(&["login", "--frames", "2"])).is_err());
    }

    #[test]
    fn rejects_unknown_scene_and_missing_scene() {
        assert!(parse(&args(&["atlantis"])).is_err());
        assert!(parse(&args(&["--top", "3"])).is_err());
    }
}
