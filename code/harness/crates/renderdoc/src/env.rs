use std::path::{Path, PathBuf};
use std::process::Command;

pub type Res<T = ()> = Result<T, String>;

pub fn root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).ancestors().nth(4).unwrap().into()
}

pub fn tools() -> PathBuf {
    root().join("code/renderdoc")
}

pub fn home() -> PathBuf {
    std::env::var("OPENRS_RDOC_ENV").map_or_else(|_| tools().join(".env"), Into::into)
}

pub fn deepbind() -> PathBuf {
    home().join("mod/deepbind.so")
}

pub fn python() -> PathBuf {
    home().join("v/bin/python")
}

pub fn tool(prog: impl AsRef<std::ffi::OsStr>) -> Command {
    let h = home();
    let mut c = Command::new(prog);
    let path = std::env::var("PATH").unwrap_or_default();
    c.env("PATH", format!("{}:{path}", h.join("v/bin").display()))
        .env("RENDERDOC_PYTHON_PATH", h.join("mod"))
        .env("VK_ADD_IMPLICIT_LAYER_PATH", h.join("layer"))
        .env("ENABLE_VULKAN_RENDERDOC_CAPTURE", "1")
        .env("PYTHONWARNINGS", "ignore");
    c
}

pub fn script(name: &str, args: &[&Path]) -> Command {
    let mut c = tool(python());
    c.arg(tools().join("py").join(name)).args(args);
    c
}

pub fn rdc(args: &[&str]) -> Command {
    let mut c = tool("rdc");
    c.args(args);
    c
}

pub fn out(c: &mut Command) -> Res<String> {
    let o = c.output().map_err(|e| format!("{:?}: {e}", c.get_program()))?;
    if o.status.success() {
        Ok(String::from_utf8_lossy(&o.stdout).into_owned())
    } else {
        Err(format!("{:?} {}", c.get_program(), String::from_utf8_lossy(&o.stderr).trim()))
    }
}

pub fn run(c: &mut Command) -> Res {
    c.status().map_err(|e| e.to_string()).and_then(|s| s.success().then_some(()).ok_or(format!("{:?} failed", c.get_program())))
}

pub fn has_gl() -> bool {
    std::fs::read(home().join("mod/librenderdoc.so")).is_ok_and(|b| b.windows(16).any(|w| w == b"glXCreateContext"))
}
