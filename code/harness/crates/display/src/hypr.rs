use crate::Res;
use std::process::Command;

const SPECIAL: &str = "special:openrs";

pub fn available() -> bool {
    std::env::var_os("HYPRLAND_INSTANCE_SIGNATURE").is_some()
}

fn ctl(args: &[&str]) -> Res<String> {
    let o = Command::new("hyprctl").args(args).output().map_err(|e| e.to_string())?;
    Ok(String::from_utf8_lossy(&o.stdout).into_owned())
}

fn lua(code: &str) -> Res {
    ctl(&["dispatch", code]).map(drop)
}

fn mv(ws: &str, pid: u32) -> Res {
    lua(&format!("hl.dsp.window.move({{workspace=\"{ws}\", follow=false, window=\"pid:{pid}\"}})"))
}

pub fn spawn_hidden(cmd: &str) -> Res {
    lua(&format!("hl.dsp.exec_cmd(\"{cmd}\", {{float=true, size=\"1280 900\", no_initial_focus=true, workspace=\"{SPECIAL} silent\"}})"))
}

pub fn shown(pid: u32) -> bool {
    let all = ctl(&["clients", "-j"]).unwrap_or_default();
    let Some(end) = all.find(&format!("\"pid\": {pid},")) else { return false };
    let head = &all[..end];
    let obj = &head[head.rfind("\"address\"").unwrap_or(0)..];
    !obj.contains("\"name\": \"special:")
}

pub fn hide(pid: u32) -> Res {
    mv(SPECIAL, pid)
}

pub fn show(pid: u32) -> Res {
    let ws = ctl(&["activeworkspace", "-j"])?;
    let id = ws.split("\"id\": ").nth(1).and_then(|s| s.split(',').next()).ok_or("no active workspace")?.trim().to_string();
    mv(&id, pid)?;
    lua(&format!("hl.dsp.focus({{window=\"pid:{pid}\"}})"))
}
