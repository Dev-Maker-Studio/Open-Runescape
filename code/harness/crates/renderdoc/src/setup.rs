use crate::env::{self, Res};
use std::path::Path;
use std::process::Command;

fn missing() -> Vec<&'static str> {
    ["python3", "cmake", "g++", "gcc", "git", "patch", "Xvfb"].into_iter().filter(|p| Command::new("which").arg(p).output().is_ok_and(|o| !o.status.success())).collect()
}

fn site() -> Res<String> {
    env::out(env::tool(env::python()).args(["-c", "import rdc,os;print(os.path.dirname(os.path.dirname(rdc.__file__)))"])).map(|s| s.trim().into())
}

fn patched(site: &str, patch: &Path) -> bool {
    !Command::new("sh").arg("-c").arg(format!("patch -p1 -N -s --dry-run -d '{site}' < '{}'", patch.display())).output().is_ok_and(|o| o.status.success())
}

fn layer() -> Res {
    let (h, dst) = (env::home(), env::home().join("layer/renderdoc_capture.json"));
    let src = h.join("build/renderdoc/build/lib/renderdoc_capture.json");
    if dst.exists() && std::fs::read_to_string(&dst).is_ok_and(|t| t.contains(&h.join("mod/librenderdoc.so").to_string_lossy().into_owned())) {
        return Ok(());
    }
    let text = std::fs::read_to_string(&src).map_err(|e| format!("{}: {e}", src.display()))?;
    std::fs::create_dir_all(dst.parent().unwrap()).map_err(|e| e.to_string())?;
    std::fs::write(&dst, text.replace("/usr/local/lib/librenderdoc.so", &h.join("mod/librenderdoc.so").to_string_lossy())).map_err(|e| e.to_string())
}

fn shim() -> Res {
    let (src, dst) = (env::tools().join("native/deepbind.c"), env::deepbind());
    let fresh = |a: &Path, b: &Path| a.metadata().and_then(|m| m.modified()).ok() >= b.metadata().and_then(|m| m.modified()).ok();
    if dst.exists() && fresh(&dst, &src) {
        return Ok(());
    }
    env::run(Command::new("gcc").args(["-shared", "-fPIC", "-O1", "-o"]).arg(&dst).arg(src).arg("-ldl"))
}

pub fn install() -> Res {
    let m = missing();
    if !m.is_empty() {
        return Err(format!("missing system tools: {}", m.join(" ")));
    }
    let (h, t) = (env::home(), env::tools());
    std::fs::create_dir_all(&h).map_err(|e| e.to_string())?;
    if !env::python().exists() {
        env::run(Command::new("python3").args(["-m", "venv"]).arg(h.join("v")))?;
    }
    let have = env::tool(env::python()).args(["-c", "import rdc,PIL,numpy,mcp"]).output().is_ok_and(|o| o.status.success());
    if !have {
        env::run(env::tool("pip").args(["install", "-q", "rdc-cli", "ninja", "pillow", "numpy", "mcp>=1.2,<2"]))?;
    }
    let site = site()?;
    let patch = t.join("patches/rdc-cli-fixes.patch");
    if !patched(&site, &patch) {
        let _ = Command::new("sh").arg("-c").arg(format!("patch -p1 -N -s -d '{site}' < '{}'", patch.display())).status();
    }
    let build = format!("{site}/rdc/_build_renderdoc.py");
    if let Ok(src) = std::fs::read_to_string(&build) {
        if src.contains("-DENABLE_GL=OFF") {
            let _ = std::fs::write(&build, src.replace("-DENABLE_GL=OFF", "-DENABLE_GL=ON"));
        }
    }
    if !h.join("mod/renderdoc.so").exists() || !env::has_gl() {
        let jobs = std::thread::available_parallelism().map_or(2, |n| n.get().min(6)).to_string();
        let _ = std::fs::remove_dir_all(h.join("build/renderdoc/build"));
        env::run(env::rdc(&["setup-renderdoc"]).arg(h.join("mod")).arg("--build-dir").arg(h.join("build")).args(["--jobs", &jobs]))?;
    }
    layer()?;
    shim()?;
    verify(false)
}

pub fn problems() -> Vec<String> {
    let mut bad = missing().iter().map(|p| format!("missing {p}")).collect::<Vec<_>>();
    let h = env::home();
    if !h.join("mod/renderdoc.so").exists() {
        bad.push("renderdoc python module not built".into());
    } else if !env::has_gl() {
        bad.push("renderdoc was built without OpenGL".into());
    }
    for (p, what) in [("mod/deepbind.so", "jvm preload shim"), ("layer/renderdoc_capture.json", "vulkan capture layer"), ("v/bin/rdc", "rdc-cli")] {
        if !h.join(p).exists() {
            bad.push(format!("{what} missing"));
        }
    }
    bad
}

pub fn verify(full: bool) -> Res {
    let bad = problems();
    if !bad.is_empty() {
        return Err(format!("{}; run: openrs capture setup", bad.join("; ")));
    }
    if full {
        print!("{}", env::out(&mut env::rdc(&["doctor"])).unwrap_or_default());
    }
    println!("renderdoc ready: OpenGL and Vulkan capture, normalized analysis");
    Ok(())
}
