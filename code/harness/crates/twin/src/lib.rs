use std::path::{Path, PathBuf};
use std::process::Command;

type Res<T = ()> = Result<T, String>;

mod frames;

pub struct Opts {
    pub scene: String,
    pub at: Option<[String; 3]>,
    pub out: PathBuf,
    pub renderdoc: bool,
    pub keep: bool,
    pub here: bool,
    pub frames: Option<u32>,
    pub every: u32,
}

fn absolute(p: impl Into<PathBuf>) -> PathBuf {
    let p = p.into();
    if p.is_absolute() { p } else { std::env::current_dir().unwrap_or_default().join(p) }
}

fn count(v: &str, flag: &str, max: u32) -> Res<u32> {
    v.parse::<u32>().ok().filter(|n| (1..=max).contains(n)).ok_or(format!("{flag} needs a number from 1 to {max}"))
}

pub fn parse(args: &[String], root: &Path) -> Res<Opts> {
    let mut o = Opts { scene: String::new(), at: None, out: PathBuf::new(), renderdoc: false, keep: false, here: false, frames: None, every: 1 };
    let mut out = None;
    let mut it = args.iter();
    while let Some(a) = it.next() {
        let mut val = || it.next().cloned().ok_or(format!("{a} needs a value"));
        match a.as_str() {
            "--at" => o.at = Some([val()?, val()?, val()?]),
            "--out" => out = Some(absolute(val()?)),
            "--renderdoc" => o.renderdoc = true,
            "--keep" => o.keep = true,
            "--here" | "--no-teleport" => o.here = true,
            "--frames" => o.frames = Some(count(&val()?, "--frames", 600)?),
            "--every" => o.every = count(&val()?, "--every", 1000)?,
            s if !s.starts_with("--") && o.scene.is_empty() => o.scene = s.into(),
            s => return Err(format!("unknown option {s}")),
        }
    }
    if o.here && (o.at.is_some() || !o.scene.is_empty()) {
        return Err("--here compares the current position, it takes no scene and no --at".into());
    }
    if o.here {
        o.scene = "here".into();
    } else if o.at.is_some() {
        o.scene = "live".into();
    } else if o.scene.is_empty() {
        o.scene = "lumbridge".into();
    }
    if !o.here && o.at.is_none() && !renderdoc::SCENES.iter().any(|s| s.0 == o.scene && s.0 != "login") {
        let names: Vec<&str> = renderdoc::SCENES.iter().map(|s| s.0).filter(|n| *n != "login").collect();
        return Err(format!("unknown scene {}, scenes: {}, or --at X Y Z", o.scene, names.join(" ")));
    }
    if args.iter().any(|a| a == "--every") && o.frames.is_none() {
        return Err("--every needs --frames".into());
    }
    let folder = if o.frames.is_some() { format!("{}-frames", o.scene) } else { o.scene.clone() };
    o.out = out.unwrap_or_else(|| root.join("code/harness/target/twin").join(folder));
    Ok(o)
}

pub fn tile(o: &Opts) -> Option<[String; 3]> {
    if o.here {
        return None;
    }
    o.at.clone().or_else(|| renderdoc::SCENES.iter().find(|s| s.0 == o.scene).map(|s| [s.1.to_string(), s.2.to_string(), "0".into()]))
}

fn tail(text: &str) -> String {
    let lines: Vec<&str> = text.lines().filter(|l| !l.trim().is_empty()).collect();
    lines[lines.len().saturating_sub(6)..].join("\n")
}

fn executable() -> Res<PathBuf> {
    std::env::current_exe().map(|p| PathBuf::from(p.to_string_lossy().trim_end_matches(" (deleted)"))).map_err(|e| e.to_string())
}

fn openrs(args: &[&str]) -> Res<String> {
    let me = executable()?;
    let o = Command::new(me).args(args).output().map_err(|e| e.to_string())?;
    let text = String::from_utf8_lossy(&o.stdout).into_owned();
    if o.status.success() {
        return Ok(text);
    }
    Err(format!("openrs {} failed: {}", args.join(" "), tail(&(text + &String::from_utf8_lossy(&o.stderr)))))
}

fn save(path: &Path, text: &str) -> Res {
    std::fs::write(path, text).map_err(|e| format!("{}: {e}", path.display()))
}

fn s(p: &Path) -> &str {
    p.to_str().unwrap_or_default()
}

fn place(o: &Opts) -> Res {
    if let Some([x, y, z]) = tile(o) {
        openrs(&["do", "command", "tele", &x, &y, &z, ";", "wait-until", "pos", &x, &y, "3", "timeout=30000", ";", "wait-idle", "2", "timeout=30000", ";", "wait-tick", "2"])?;
    }
    Ok(())
}

fn client_side(o: &Opts, dir: &Path) -> Res {
    place(o)?;
    openrs(&["do", "dump-frame", s(&dir.join("frame.orf")), ";", "screenshot", s(&dir.join("shot.png"))])?;
    save(&dir.join("state.json"), &openrs(&["do", "state"])?)?;
    save(&dir.join("describe.txt"), &openrs(&["do", "describe"])?)
}

fn web_side(frame: &Path, dir: &Path) -> Res {
    openrs(&["web", "up"])?;
    openrs(&["web", "do", "replay", s(frame), ";", "wait-frames", "3"])?;
    openrs(&["web", "do", "screenshot", s(&dir.join("shot.png")), ";", "dump-frame", s(&dir.join("frame.orf"))])?;
    save(&dir.join("state.json"), &openrs(&["web", "do", "state"])?)?;
    save(&dir.join("describe.txt"), &openrs(&["web", "do", "describe"])?)
}

fn deep(o: &Opts, dir: &Path) -> Res<Option<PathBuf>> {
    let mut args = vec!["equiv".to_string()];
    match &o.at {
        Some(_) => args.push("--live".into()),
        None => args.push(o.scene.clone()),
    }
    args.extend(["--target".into(), "browser".into(), "--out".into(), s(dir).into()]);
    let me = executable()?;
    let run = Command::new(me).args(&args).output().map_err(|e| e.to_string())?;
    let _ = std::fs::write(dir.with_file_name("renderdoc.log"), [&run.stdout[..], &run.stderr[..]].concat());
    if !matches!(run.status.code(), Some(0 | 1)) {
        return Err(format!("openrs equiv failed with {}: {}", run.status, tail(&String::from_utf8_lossy(&run.stderr))));
    }
    Ok(Some(dir.join("equiv.json")).filter(|p| p.is_file()))
}

pub fn run(args: &[String]) -> i32 {
    let root = match display::root() {
        Ok(r) => r,
        Err(e) => return fail(&e),
    };
    let o = match parse(args, &root) {
        Ok(o) => o,
        Err(e) => return fail(&e),
    };
    let (cdir, wdir) = (o.out.join("client"), o.out.join("web"));
    if let Err(e) = [&cdir, &wdir].iter().try_for_each(std::fs::create_dir_all) {
        return fail(&e.to_string());
    }
    let started = control::ingame().is_none();
    let steps = (|| -> Res<Option<PathBuf>> {
        if started {
            openrs(&["up"])?;
        }
        match o.frames {
            Some(n) => {
                frames::client_frames(&o, &cdir, n, o.every)?;
                frames::web_frames(&cdir, &wdir, n)?;
            }
            None => {
                client_side(&o, &cdir)?;
                web_side(&cdir.join("frame.orf"), &wdir)?;
            }
        }
        let _ = openrs(&["web", "down"]);
        if o.renderdoc { deep(&o, &o.out.join("renderdoc")) } else { Ok(None) }
    })();
    let _ = openrs(&["web", "down"]);
    if started && !o.keep {
        let _ = openrs(&["down"]);
    }
    let deep_json = match steps {
        Ok(j) => j,
        Err(e) => return fail(&e),
    };
    let python = root.join("code/renderdoc/.env/v/bin/python");
    let mut cmd = Command::new(python);
    cmd.arg(root.join("code/renderdoc/py/twin.py")).arg(&o.out).args(["--scene", &o.scene, "--history"]).arg(o.out.parent().unwrap_or(&o.out).join("history.jsonl"));
    if let Some(n) = o.frames {
        cmd.args(frames::python_args(n, o.every));
    }
    if let Some(j) = deep_json {
        cmd.arg("--renderdoc").arg(j);
    }
    cmd.status().ok().and_then(|st| st.code()).unwrap_or(2)
}

fn fail(e: &str) -> i32 {
    eprintln!("twin: {e}");
    2
}

#[cfg(test)]
mod tests;
