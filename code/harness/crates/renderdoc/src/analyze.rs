use crate::env::{self, Res};
use std::path::{Path, PathBuf};

pub fn gpu_lock(path: &Path) -> Res<std::fs::File> {
    let f = std::fs::File::create(path).map_err(|e| e.to_string())?;
    f.lock().map_err(|e| e.to_string())?;
    Ok(f)
}

pub fn run(rdc: &Path, out: PathBuf) -> Res {
    run_to(rdc, out, true)
}

pub fn run_to(rdc: &Path, out: PathBuf, verbose: bool) -> Res {
    let stamp = |p: &Path| p.metadata().and_then(|m| m.modified()).ok();
    if stamp(&out.join("summary.json")) > stamp(rdc).max(stamp(&env::tools().join("py/frame.py"))) {
        if verbose {
            println!("analysis cached {}", out.display());
        }
        return Ok(());
    }
    let rdc = std::fs::canonicalize(rdc).map_err(|e| format!("{}: {e}", rdc.display()))?;
    std::fs::create_dir_all(&out).map_err(|e| e.to_string())?;
    let (mut frame, mut audit) = (env::script("frame.py", &[&rdc, &out]), env::script("audit.py", &[&rdc]));
    if !verbose {
        frame.stdout(std::process::Stdio::null());
    }
    let a = {
        let _gpu = gpu_lock(&std::env::temp_dir().join("openrs-gpu-replay.lock"))?;
        let r = env::run(&mut frame);
        (r, audit.output().ok())
    };
    a.0?;
    if let Some(o) = a.1 {
        let text = String::from_utf8_lossy(&o.stdout).into_owned() + &String::from_utf8_lossy(&o.stderr);
        let _ = std::fs::write(out.join("audit.txt"), &text);
        if verbose {
            print!("{text}");
        }
    }
    if verbose {
        println!("capture folder {}", out.display());
    }
    Ok(())
}

pub fn diff(client: &Path, web: &Path, rest: &[String]) -> Res {
    let out = rest.first().map_or_else(|| web.parent().unwrap_or(web).join("diff"), PathBuf::from);
    for d in [client, web] {
        if !d.join("summary.json").exists() {
            return Err(format!("{} has no summary.json, capture or analyze first", d.display()));
        }
    }
    let limit = rest.get(1).cloned().unwrap_or_else(|| "40".into());
    let mut c = env::script("diff.py", &[client, web, &out]);
    c.arg(limit);
    env::run(&mut c)
}

pub fn overview(dir: &Path, rest: &[String]) -> Res {
    let mut c = env::script("overview.py", &[dir]);
    c.args(rest);
    env::run(&mut c)
}
