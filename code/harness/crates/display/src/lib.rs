mod audio;
mod hypr;
mod state;
mod xserver;

use std::path::PathBuf;
use std::process::{Command, Stdio};

pub type Res<T = ()> = Result<T, String>;

const JVM: [&str; 4] = ["-XX:+UseZGC", "-Xmx4g", "-Xms1g", "-Drender.allowsoftgl=true"];

pub const UNSUPPORTED: &str = "the private display needs a running Hyprland session";

pub fn run(extra: &[String]) -> Res {
    let pid = launch("client", &JVM.map(String::from), extra, &[])?;
    let log = state::dir().join("client.log");
    let st = private()?;
    println!("client pid {pid} on {} audio {} log {}", st.display, audio::SINK, log.display());
    Ok(())
}

fn detach(cmd: &mut Command) {
    use std::os::unix::process::CommandExt;
    cmd.process_group(0);
}

pub fn launch(tag: &str, jvm: &[String], extra: &[String], envs: &[(String, String)]) -> Res<u32> {
    let st = private()?;
    let sink = audio::null_sink().ok();
    let jar = state::root()?.join("code/client/build/libs/darkan-client-shaded-2.0.0-all.jar");
    if !jar.exists() {
        return Err(format!("missing {} (run ./gradlew shadowJar)", jar.display()));
    }
    if let Some(pid) = state::read(tag).and_then(|s| s.parse().ok()) {
        if state::alive(pid) {
            return Err(format!("{tag} already running, pid {pid}"));
        }
    }
    let log = std::fs::File::create(state::dir().join(format!("{tag}.log"))).map_err(|e| e.to_string())?;
    let mut cmd = Command::new("java");
    cmd.args(jvm)
        .args(extra.iter().filter(|a| a.starts_with("-D") || a.starts_with("-X")))
        .arg("-jar")
        .arg(&jar)
        .args(extra.iter().filter(|a| !a.starts_with("-D") && !a.starts_with("-X")))
        .envs(envs.iter().map(|(k, v)| (k, v)))
        .env("OPENRS_BIN", std::env::current_exe().map_err(|e| e.to_string())?)
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log);
    cmd.env("DISPLAY", &st.display).env_remove("WAYLAND_DISPLAY");
    if let Some(sink) = &sink {
        cmd.env("PULSE_SINK", sink).env("PIPEWIRE_NODE", sink);
    }
    detach(&mut cmd);
    let child = cmd.spawn().map_err(|e| e.to_string())?;
    state::write(tag, &child.id().to_string());
    Ok(child.id())
}

pub fn display_name() -> Res<String> {
    private().map(|s| s.display)
}

pub fn xserver_pid() -> Option<u32> {
    state::read("xserver").and_then(|s| s.split(' ').nth(1).and_then(|p| p.parse().ok())).filter(|p| state::alive(*p))
}

pub fn show() -> Res {
    let st = private()?;
    hypr::show(st.pid)
}

pub fn hide() -> Res {
    hypr::hide(private()?.pid)
}

fn private() -> Res<xserver::Xs> {
    if !hypr::available() {
        return Err(UNSUPPORTED.into());
    }
    xserver::ensure()
}

pub fn toggle() -> Res {
    let st = private()?;
    if hypr::shown(st.pid) {
        hypr::hide(st.pid)
    } else {
        hypr::show(st.pid)
    }
}

pub fn shot(out: &str) -> Res {
    let st = private()?;
    let ok = Command::new("import")
        .args(["-display", &st.display, "-window", "root", out])
        .status()
        .map_err(|e| e.to_string())?
        .success();
    if ok { Ok(()) } else { Err("screenshot failed".into()) }
}

pub fn stop() -> Res {
    for name in ["client", "xserver"] {
        let pid = if name == "xserver" { state::read("xserver").and_then(|s| s.split(' ').nth(1).and_then(|p| p.parse().ok())) } else { state::read(name).and_then(|s| s.parse().ok()) };
        if let Some(pid) = pid {
            if state::alive(pid) {
                let _ = Command::new("kill").arg(pid.to_string()).status();
            }
        }
    }
    audio::unload();
    let _ = std::fs::remove_dir_all(state::dir());
    Ok(())
}

pub fn root() -> Res<PathBuf> {
    state::root()
}

pub fn client_pid() -> Option<u32> {
    state::read("client").and_then(|s| s.parse().ok()).filter(|p| state::alive(*p))
}

pub fn stop_client() {
    if let Some(pid) = client_pid() {
        let _ = Command::new("kill").arg(pid.to_string()).status();
    }
    let _ = std::fs::remove_file(dir().join("client"));
}

pub fn dir() -> PathBuf {
    state::dir()
}

#[cfg(test)]
mod tests;
