use std::path::Path;
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const CHROMIUM: &str = "/usr/bin/chromium";

pub const GPU: [&str; 5] = ["--enable-unsafe-webgpu", "--enable-features=Vulkan,VulkanFromANGLE,DefaultANGLEVulkan", "--use-angle=vulkan", "--ignore-gpu-blocklist", "--ozone-platform=x11"];

const QUIET: [&str; 15] = [
    "--disable-gpu-vsync",
    "--disable-frame-rate-limit",
    "--no-first-run",
    "--no-default-browser-check",
    "--disable-sync",
    "--disable-extensions",
    "--disable-component-update",
    "--password-store=basic",
    "--disable-background-timer-throttling",
    "--disable-renderer-backgrounding",
    "--disable-backgrounding-occluded-windows",
    "--force-device-scale-factor=1",
    "--window-position=0,0",
    "--window-size=1280,900",
    "--remote-debugging-port=0",
];

pub struct Browser {
    pub pid: u32,
    pub port: u16,
    pub ws: String,
}

pub struct Launch<'a> {
    pub display: &'a str,
    pub url: &'a str,
    pub profile: &'a Path,
    pub args: &'a [String],
    pub envs: &'a [(String, String)],
    pub wrapper: &'a [String],
}

pub fn args(l: &Launch) -> Vec<String> {
    let mut out = vec![format!("--user-data-dir={}", l.profile.display())];
    out.extend(GPU.iter().chain(&QUIET).map(|s| s.to_string()));
    out.push(format!("--app={}", l.url));
    out.extend(l.args.iter().cloned());
    out
}

pub fn audio(sink: Option<String>) -> (Vec<(String, String)>, Option<&'static str>) {
    match sink {
        Some(s) => (vec![("PULSE_SINK".into(), s.clone()), ("PIPEWIRE_NODE".into(), s)], None),
        None => (vec![], Some("--mute-audio")),
    }
}

pub fn active_port(text: &str) -> Option<(u16, String)> {
    let mut lines = text.lines();
    let port = lines.next()?.trim().parse().ok()?;
    let path = lines.next()?.trim();
    path.starts_with("/devtools/").then(|| (port, format!("ws://127.0.0.1:{port}{path}")))
}

pub fn launch(l: &Launch) -> Result<Browser, String> {
    use std::os::unix::process::CommandExt;
    std::fs::create_dir_all(l.profile).map_err(|e| format!("{}: {e}", l.profile.display()))?;
    let port_file = l.profile.join("DevToolsActivePort");
    let _ = std::fs::remove_file(&port_file);
    let log = std::fs::File::create(l.profile.join("chromium.log")).map_err(|e| e.to_string())?;
    let mut cmd = match l.wrapper.split_first() {
        Some((w, rest)) => {
            let mut c = Command::new(w);
            c.args(rest).arg(CHROMIUM);
            c
        }
        None => Command::new(CHROMIUM),
    };
    let (sink_envs, mute) = audio(display::sink());
    cmd.args(args(l))
        .args(mute)
        .env("DISPLAY", l.display)
        .env_remove("WAYLAND_DISPLAY")
        .env("XDG_CONFIG_HOME", l.profile.join("xdg"))
        .envs(sink_envs)
        .envs(l.envs.iter().map(|(k, v)| (k, v)))
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log)
        .process_group(0);
    let mut child = cmd.spawn().map_err(|e| format!("{CHROMIUM}: {e}"))?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(30) {
        if let Some((port, ws)) = std::fs::read_to_string(&port_file).ok().as_deref().and_then(active_port) {
            return Ok(Browser { pid: child.id(), port, ws });
        }
        if let Ok(Some(status)) = child.try_wait() {
            return Err(format!("chromium exited with {status}, see {}", l.profile.join("chromium.log").display()));
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    Err("chromium did not open its devtools port within 30s".into())
}
