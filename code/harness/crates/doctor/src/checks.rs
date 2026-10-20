use crate::{fail, pass, warn, Check};
use std::net::{TcpStream, ToSocketAddrs};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const ALL: [fn(&Path) -> Vec<Check>; 10] = [jar, cache, renderdoc, gpu, display, writable, java, ports, fleet, disk];

const NO_GPU: &str = "no GPU is visible to this process, run from an unrestricted shell on a machine with the driver loaded";

fn home() -> PathBuf {
    PathBuf::from(std::env::var("HOME").unwrap_or_default())
}

fn runtime() -> PathBuf {
    PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into()))
}

pub fn which(p: &str) -> Option<PathBuf> {
    std::env::split_paths(&std::env::var_os("PATH")?).map(|d| d.join(p)).find(|f| f.is_file())
}

fn output(cmd: &mut Command, secs: u64) -> Option<String> {
    let mut child = cmd.stdin(Stdio::null()).stdout(Stdio::piped()).stderr(Stdio::piped()).spawn().ok()?;
    let t = Instant::now();
    while child.try_wait().ok()?.is_none() {
        if t.elapsed() > Duration::from_secs(secs) {
            let _ = child.kill();
            let _ = child.wait();
            return None;
        }
        std::thread::sleep(Duration::from_millis(5));
    }
    let o = child.wait_with_output().ok()?;
    Some(format!("{}{}", String::from_utf8_lossy(&o.stdout), String::from_utf8_lossy(&o.stderr)))
}

fn jar(root: &Path) -> Vec<Check> {
    let fix = "openrs up rebuilds it, or cd code/client && ./gradlew shadowJar";
    vec![match fresh::jar_stale(root) {
        None => pass("jar", "client shaded jar is newer than every client source"),
        Some("missing") => fail("jar", format!("{} missing", fresh::jar(root).display()), fix),
        Some(why) => warn("jar", format!("client jar is {why}"), fix),
    }]
}

fn cache(root: &Path) -> Vec<Check> {
    if !root.join("assets/cache").is_dir() {
        return vec![fail("cache", "assets/cache missing", "clone darkanrs/cache-unpacked into assets/cache")];
    }
    vec![match cache::fresh(root) {
        Ok(true) => pass("cache", "packed cache matches assets/cache"),
        Ok(false) => warn("cache", "packed cache is stale or missing", "openrs cache build, openrs up runs it when stale"),
        Err(e) => fail("cache", e, "check assets/cache is readable"),
    }]
}

fn renderdoc(_: &Path) -> Vec<Check> {
    let bad = renderdoc::problems();
    vec![if bad.is_empty() { pass("renderdoc", "RenderDoc with OpenGL and Vulkan, rdc-cli, preload shim and capture layer") } else { fail("renderdoc", bad.join("; "), "openrs capture setup") }]
}

fn gpu(_: &Path) -> Vec<Check> {
    let mut nodes: Vec<String> = ["/dev/nvidia0", "/dev/nvidiactl"].into_iter().filter(|p| Path::new(p).exists()).map(String::from).collect();
    nodes.extend(std::fs::read_dir("/dev/dri").into_iter().flatten().flatten().map(|e| e.path().display().to_string()).filter(|p| p.contains("renderD")));
    if nodes.is_empty() {
        return vec![fail("gpu", "no /dev/nvidia* or /dev/dri/renderD* visible", NO_GPU)];
    }
    let Some(info) = which("vulkaninfo").and_then(|_| output(Command::new("vulkaninfo").arg("--summary"), 3)) else {
        return vec![pass("gpu", format!("{}, Vulkan not probed (no vulkaninfo)", nodes.join(" ")))];
    };
    let real: Vec<&str> = info.lines().filter_map(|l| l.split_once("deviceName")).map(|(_, v)| v.trim_start_matches([' ', '=']).trim()).filter(|n| !n.contains("llvmpipe")).collect();
    vec![if real.is_empty() { warn("gpu", format!("{} visible but Vulkan lists no hardware device", nodes.join(" ")), "install the Vulkan driver and ICD loader") } else { pass("gpu", format!("Vulkan on {}", real.join(", "))) }]
}

fn display(_: &Path) -> Vec<Check> {
    let rt = runtime();
    let wayland = std::env::var("WAYLAND_DISPLAY").ok().filter(|w| rt.join(w).exists());
    let disp = match (wayland, which("Xwayland")) {
        (Some(w), Some(_)) => pass("display", format!("Wayland socket {w}, Xwayland installed")),
        (Some(_), None) => fail("display", "Xwayland is not installed", "install xorg-xwayland"),
        (None, _) => fail("display", "no live Wayland socket in XDG_RUNTIME_DIR", "run from the desktop session, the private display is an Xwayland under the compositor"),
    };
    let sig = std::env::var("HYPRLAND_INSTANCE_SIGNATURE").ok().filter(|s| rt.join("hypr").join(s).join(".socket.sock").exists());
    let hypr = match (sig, which("hyprctl")) {
        (Some(_), Some(_)) => pass("hyprland", "hyprctl reaches the running Hyprland"),
        _ => fail("hyprland", "Hyprland is not reachable", "run inside a Hyprland session, the private Xwayland and the toggle go through hyprctl"),
    };
    let shot = if which("import").is_some() { pass("import", "ImageMagick import present for the look fallback") } else { warn("import", "ImageMagick import missing, look has no display fallback", "install imagemagick") };
    vec![disp, hypr, shot]
}

fn probe(dir: &Path) -> Result<(), String> {
    let f = dir.join(format!(".openrs-doctor-{}", std::process::id()));
    std::fs::create_dir_all(dir).and_then(|_| std::fs::write(&f, b"")).and_then(|_| std::fs::remove_file(&f)).map_err(|e| e.to_string())
}

fn writable(root: &Path) -> Vec<Check> {
    let gradle = std::env::var_os("GRADLE_USER_HOME").map_or_else(|| home().join(".gradle"), PathBuf::from);
    let mut rows: Vec<Check> = [("home", home()), ("gradle", gradle), ("runtime", runtime()), ("target", root.join("code/harness/target"))]
        .into_iter()
        .map(|(n, d)| match probe(&d) {
            Ok(()) => pass(n, format!("{} writable", d.display())),
            Err(e) => fail(n, format!("{} not writable ({e})", d.display()), if n == "target" { "make code/harness/target writable" } else { "" }),
        })
        .collect();
    let blocked: Vec<String> = rows.iter().filter(|c| c.level == crate::Level::Fail && c.name != "target").map(|c| c.name.clone()).collect();
    if !blocked.is_empty() {
        rows.push(fail("sandbox", format!("restricted sandbox detected, {} not writable: gradle, the client and the local server cannot run in this session", blocked.join(", ")), "run openrs from an unrestricted shell"));
    }
    rows
}

pub fn major(line: &str) -> Option<u32> {
    line.split('"').nth(1)?.split(['.', '-', '+']).next()?.parse().ok()
}

fn wrapper(dir: &Path) -> Check {
    let name = format!("gradlew-{}", dir.file_name().map_or(String::new(), |n| n.to_string_lossy().into_owned()));
    let Ok(props) = std::fs::read_to_string(dir.join("gradle/wrapper/gradle-wrapper.properties")) else {
        return fail(name, format!("{} has no gradle wrapper", dir.display()), "restore gradlew and gradle/wrapper");
    };
    let dist = props.lines().find_map(|l| l.strip_prefix("distributionUrl=")).and_then(|u| u.rsplit('/').next()).map(|z| z.trim_end_matches(".zip").to_string()).unwrap_or_default();
    let base = std::env::var_os("GRADLE_USER_HOME").map_or_else(|| home().join(".gradle"), PathBuf::from);
    if base.join("wrapper/dists").join(&dist).is_dir() { pass(name, format!("{dist} installed")) } else { warn(name, format!("{dist} not downloaded yet"), "the first ./gradlew run downloads it, needs network") }
}

fn java(root: &Path) -> Vec<Check> {
    let first = output(Command::new("java").arg("-version"), 5).and_then(|o| o.lines().next().map(str::to_string));
    let java = match first.as_deref().map(|l| (l, major(l))) {
        Some((l, Some(m))) if m >= 17 => pass("java", l.trim()),
        Some((l, _)) => fail("java", format!("{} is older than 17 or unknown", l.trim()), "install JDK 17 or newer"),
        None => fail("java", "java not found on PATH", "install JDK 17 or newer"),
    };
    let jdk = std::env::var_os("OPENRS_JAVA_HOME").map(PathBuf::from).or_else(|| std::fs::read_dir(home().join(".jdks")).ok()?.flatten().map(|e| e.path()).find(|p| p.file_name().is_some_and(|n| n.to_string_lossy().starts_with("jdk-26"))));
    let server = match jdk {
        Some(j) => pass("jdk", format!("{} for the cache build and the local server", j.display())),
        None => warn("jdk", "no OPENRS_JAVA_HOME or ~/.jdks/jdk-26*, the server tools use the default java", "install JDK 26 into ~/.jdks or set OPENRS_JAVA_HOME"),
    };
    vec![java, server, wrapper(&root.join("code/client")), wrapper(&root.join("code/server"))]
}

fn open(host: &str, port: &str) -> bool {
    format!("{host}:{port}").to_socket_addrs().ok().and_then(|mut a| a.next()).is_some_and(|a| TcpStream::connect_timeout(&a, Duration::from_millis(800)).is_ok())
}

fn ports(root: &Path) -> Vec<Check> {
    let env = |k: &str, d: &str| std::env::var(k).unwrap_or(d.into());
    let mut targets = vec![("local0".to_string(), "127.0.0.1".to_string(), env("OPENRS_LOBBY_PORT", "44594"), env("OPENRS_WORLD_PORT", "44595"))];
    for t in fleet::validate().unwrap_or_default() {
        if !targets.iter().any(|o| (&o.1, &o.2, &o.3) == (&t.1, &t.2, &t.3)) {
            targets.push(t);
        }
    }
    if let Some((h, p)) = cache::lobby(true).rsplit_once(':') {
        if !targets.iter().any(|o| o.1 == h && o.2 == p) {
            targets.push(("shared".into(), h.into(), p.into(), String::new()));
        }
    }
    let installed = root.join("code/server/lobby/build/install/lobby/bin/lobby").exists();
    std::thread::scope(|s| {
        let probes: Vec<_> = targets.iter().map(|t| s.spawn(move || (open(&t.1, &t.2), t.3.is_empty() || open(&t.1, &t.3)))).collect();
        targets.iter().zip(probes).map(|(t, h)| {
            let (lobby, world) = h.join().unwrap_or_default();
            let state = |b: bool| if b { "open" } else { "closed" };
            let msg = format!("{} lobby {} {}{}", t.1, t.2, state(lobby), if t.3.is_empty() { String::new() } else { format!(", world {} {}", t.3, state(world)) });
            let local = t.1 == "127.0.0.1" || t.1 == "localhost";
            match (lobby && world, local, installed) {
                (true, _, _) => pass(t.0.clone(), msg),
                (false, true, true) => warn(t.0.clone(), msg, "openrs up and openrs fleet up start the local server"),
                (false, true, false) => fail(t.0.clone(), msg, "cd code/server && ./gradlew :lobby:installDist :world:installDist"),
                (false, false, _) => warn(t.0.clone(), msg, "check the network or OPENRS_SHARED_LOBBY"),
            }
        }).collect()
    })
}

fn fleet(root: &Path) -> Vec<Check> {
    let file = std::env::var("OPENRS_FLEET").unwrap_or_else(|_| if root.join("code/harness/fleet.toml").exists() { "fleet.toml".into() } else { "fleet.toml.example".into() });
    vec![match fleet::validate() {
        Ok(t) => pass("fleet", format!("{file}: 9 slots on {}", t.iter().map(|t| t.0.as_str()).collect::<Vec<_>>().join(" "))),
        Err(e) => fail("fleet", format!("{file}: {e}"), "edit code/harness/fleet.toml, see fleet.toml.example"),
    }]
}

fn disk(root: &Path) -> Vec<Check> {
    let at = Some(root.join("code/harness/target")).filter(|d| d.exists()).unwrap_or_else(|| root.into());
    let free = output(Command::new("df").arg("-Pk").arg(&at), 3).and_then(|o| o.lines().nth(1)?.split_whitespace().nth(3)?.parse::<u64>().ok());
    let fix = "free space or delete code/harness/target/capture and code/harness/target/equiv";
    match free {
        None => vec![warn("disk", format!("df failed on {}", at.display()), "")],
        Some(k) => {
            let msg = format!("{:.1} GB free for captures under {}", k as f64 / 1048576.0, at.display());
            vec![if k < 1 << 20 { fail("disk", msg, fix) } else if k < 5 << 20 { warn("disk", msg, fix) } else { pass("disk", msg) }]
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn java_major_versions() {
        assert_eq!(major("openjdk version \"25.0.4.1\" 2026-08-18"), Some(25));
        assert_eq!(major("java version \"1.8.0_402\""), Some(1));
        assert_eq!(major("openjdk version \"17-ea\""), Some(17));
        assert_eq!(major("garbage"), None);
    }

    #[test]
    fn which_finds_sh() {
        assert!(which("sh").is_some());
        assert!(which("no-such-binary-openrs").is_none());
    }

    #[test]
    fn every_check_reports_and_names_itself() {
        let root = Path::new(env!("CARGO_MANIFEST_DIR")).ancestors().nth(4).unwrap();
        let all = crate::collect(root);
        assert!(all.len() >= ALL.len());
        assert!(all.iter().all(|c| !c.name.is_empty() && !c.msg.is_empty()));
    }
}
