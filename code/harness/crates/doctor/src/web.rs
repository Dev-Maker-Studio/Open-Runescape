use crate::checks::output;
use crate::{fail, pass, warn, Check};
use serde_json::Value;
use std::path::Path;
use std::process::Command;

pub fn lock_version(lock: &str, package: &str) -> Option<String> {
    let mut lines = lock.lines();
    lines.find(|l| l.trim() == format!("name = \"{package}\""))?;
    lines.next()?.strip_prefix("version = \"")?.strip_suffix('"').map(String::from)
}

pub fn tool_version(text: &str) -> Option<String> {
    text.split_whitespace().find(|w| w.chars().next().is_some_and(|c| c.is_ascii_digit())).map(String::from)
}

pub fn adapter(state: &Value) -> Result<String, String> {
    let g = &state["gpu"];
    let (vendor, arch) = (g["vendor"].as_str().unwrap_or("?"), g["architecture"].as_str().unwrap_or("?"));
    if g.is_null() {
        return Err("the page found no WebGPU adapter".into());
    }
    if g["fallback"] == true || arch.contains("swiftshader") || vendor.contains("google") {
        return Err(format!("WebGPU runs on a software adapter ({vendor} {arch}, fallback {})", g["fallback"]));
    }
    Ok(format!("WebGPU on hardware, {vendor} {arch}, frame {}", state["frames"]))
}

fn chromium(_: &Path) -> Vec<Check> {
    if !Path::new(web::CHROMIUM).is_file() {
        return vec![fail("chromium", format!("{} missing", web::CHROMIUM), "install chromium, the web harness never uses another browser")];
    }
    let v = output(Command::new(web::CHROMIUM).arg("--version"), 5).unwrap_or_default();
    vec![pass("chromium", format!("{} {}", web::CHROMIUM, v.lines().next().unwrap_or("version unknown").trim()))]
}

fn wasm_target(_: &Path) -> Vec<Check> {
    let sysroot = output(Command::new("rustc").args(["--print", "sysroot"]), 5).map(|s| s.trim().to_string()).unwrap_or_default();
    let fix = "rustup target add wasm32-unknown-unknown";
    vec![if !sysroot.is_empty() && Path::new(&sysroot).join("lib/rustlib/wasm32-unknown-unknown").is_dir() { pass("wasm-target", "wasm32-unknown-unknown installed") } else { fail("wasm-target", "rust target wasm32-unknown-unknown missing", fix) }]
}

fn bindgen(root: &Path) -> Vec<Check> {
    let want = std::fs::read_to_string(root.join("code/renderer/Cargo.lock")).ok().and_then(|l| lock_version(&l, "wasm-bindgen"));
    let have = output(Command::new("wasm-bindgen").arg("--version"), 5).as_deref().and_then(tool_version);
    vec![match (have, want) {
        (None, _) => fail("wasm-bindgen", "wasm-bindgen CLI not found on PATH", "cargo install wasm-bindgen-cli --locked"),
        (Some(h), Some(w)) if h != w => fail("wasm-bindgen", format!("CLI {h} but code/renderer/Cargo.lock has the crate at {w}"), &format!("cargo install wasm-bindgen-cli --version {w} --locked")),
        (Some(h), Some(_)) => pass("wasm-bindgen", format!("CLI {h} matches code/renderer/Cargo.lock")),
        (Some(h), None) => warn("wasm-bindgen", format!("CLI {h}, no wasm-bindgen crate in code/renderer/Cargo.lock to compare"), "cd code/renderer && cargo generate-lockfile"),
    }]
}

fn artifacts(root: &Path) -> Vec<Check> {
    let row = |name: &str, what: &str, path: std::path::PathBuf, stale: Option<&str>| match stale {
        None => pass(name, format!("{what} is newer than its sources")),
        Some("missing") => warn(name, format!("{} missing", path.display()), "openrs web up builds it"),
        Some(why) => warn(name, format!("{what} is {why}"), "openrs web up rebuilds it"),
    };
    vec![row("web-wasm", "renderer wasm", fresh::wasm(root), fresh::wasm_stale(root)), row("web-server", "openrs-web release binary", fresh::server(root), fresh::server_stale(root))]
}

fn sink(_: &Path) -> Vec<Check> {
    vec![match display::sink() {
        Some(s) => pass("web-audio", format!("null sink {s} available, browsers never reach the speakers")),
        None => warn("web-audio", "the null audio sink cannot be created, browsers start with --mute-audio instead", "start pipewire-pulse or pulseaudio"),
    }]
}

pub fn quick(root: &Path) -> Vec<Check> {
    std::thread::scope(|s| {
        let all: Vec<_> = [chromium, wasm_target, bindgen, artifacts, sink].map(|f| s.spawn(move || f(root))).into();
        all.into_iter().flat_map(|h| h.join().unwrap_or_default()).collect()
    })
}

pub fn deep(_: &Path) -> Vec<Check> {
    let Ok(exe) = std::env::current_exe() else { return vec![fail("web-gpu", "cannot find the openrs binary", "")] };
    let dir = std::path::PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join(format!("openrs-doctor-web-{}", std::process::id()));
    let run = |args: &[&str], secs| output(Command::new(&exe).args(args).env("OPENRS_WEB_STATE", dir.join("state")).env("OPENRS_WEB_FOOTPRINTS", dir.join("fp")), secs).unwrap_or_default();
    let up = run(&["web", "up", "--no-build"], 90);
    let state = serde_json::from_str::<Value>(&run(&["web", "do", "state"], 10)).unwrap_or(Value::Null);
    run(&["web", "down"], 20);
    let _ = std::fs::remove_dir_all(&dir);
    vec![match adapter(&state) {
        Ok(m) => pass("web-gpu", m),
        Err(_) if state.is_null() => fail("web-gpu", format!("the page did not start: {}", up.lines().last().unwrap_or("no output")), "openrs web up shows the error, openrs web do console the page log"),
        Err(e) => fail("web-gpu", e, "install the Vulkan driver, check vulkaninfo --summary and the Chromium GPU flags in WEB-HARNESS.md"),
    }]
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn lock_and_tool_versions() {
        let lock = "[[package]]\nname = \"wasm-bindgen-macro\"\nversion = \"0.2.1\"\n\n[[package]]\nname = \"wasm-bindgen\"\nversion = \"0.2.118\"\n";
        assert_eq!(lock_version(lock, "wasm-bindgen").as_deref(), Some("0.2.118"));
        assert_eq!(lock_version(lock, "nothing"), None);
        assert_eq!(tool_version("wasm-bindgen 0.2.118\n").as_deref(), Some("0.2.118"));
        assert_eq!(tool_version("Chromium 141.0.1 Arch Linux").as_deref(), Some("141.0.1"));
        assert_eq!(tool_version("no digits here"), None);
    }

    #[test]
    fn hardware_adapters_pass_and_software_fails() {
        let real = json!({ "gpu": { "vendor": "nvidia", "architecture": "turing", "fallback": false }, "frames": 12 });
        assert!(adapter(&real).unwrap().contains("nvidia turing, frame 12"));
        let soft = json!({ "gpu": { "vendor": "google", "architecture": "swiftshader", "fallback": true } });
        assert!(adapter(&soft).unwrap_err().contains("software adapter"));
        assert!(adapter(&json!({ "gpu": { "vendor": "nvidia", "architecture": "turing", "fallback": true } })).is_err());
        assert!(adapter(&json!({ "gpu": null })).unwrap_err().contains("no WebGPU adapter"));
        assert!(adapter(&Value::Null).is_err());
    }

    #[test]
    fn quick_checks_name_every_prerequisite() {
        let root = Path::new(env!("CARGO_MANIFEST_DIR")).ancestors().nth(4).unwrap();
        let names: Vec<String> = quick(root).into_iter().map(|c| c.name).collect();
        for n in ["chromium", "wasm-target", "wasm-bindgen", "web-wasm", "web-server", "web-audio"] {
            assert!(names.contains(&n.to_string()), "{n} in {names:?}");
        }
    }
}
