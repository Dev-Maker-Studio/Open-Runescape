use crate::{page::Page, state, trail};
use serde_json::{json, Value};
use std::path::{Path, PathBuf};

pub fn exec(page: &mut Page, cmd: &str, args: &[String]) -> Result<Value, String> {
    match cmd {
        "screenshot" => screenshot(page, args.first()).map(Value::String),
        "dump-frame" => dump(page, args.first()).map(Value::String),
        "replay" => {
            let file = args.first().ok_or("usage: replay <file.orf>")?;
            page.run("replay", &[link(Path::new(file))?, file.clone()])
        }
        _ => page.run(cmd, args),
    }
}

fn target(path: Option<&String>, frames: i64, ext: &str) -> PathBuf {
    path.map(PathBuf::from).unwrap_or_else(|| {
        let t = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_or(0, |d| d.as_millis());
        trail::dir().join("shots").join(format!("{t}-f{frames}.{ext}"))
    })
}

fn save(path: &Path, b64: &Value) -> Result<usize, String> {
    let bytes = data_encoding::BASE64.decode(b64.as_str().ok_or("no data")?.as_bytes()).map_err(|e| e.to_string())?;
    if let Some(dir) = path.parent().filter(|d| !d.as_os_str().is_empty()) {
        std::fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    }
    std::fs::write(path, &bytes).map_err(|e| format!("{}: {e}", path.display()))?;
    Ok(bytes.len())
}

fn screenshot(page: &mut Page, path: Option<&String>) -> Result<String, String> {
    let v = page.run("png", &[])?;
    let out = target(path, page.frames, "png");
    save(&out, &v["data"])?;
    let shown = out.display().to_string();
    trail::append(page.frames, "shot", &[("path", json!(shown)), ("width", v["width"].clone()), ("height", v["height"].clone())]);
    Ok(shown)
}

fn dump(page: &mut Page, path: Option<&String>) -> Result<String, String> {
    let v = page.run("dump", &[])?;
    let out = target(path, page.frames, "orf");
    let n = save(&out, &v["data"])?;
    let summary = v["summary"].as_str().unwrap_or("");
    trail::append(page.frames, "dump", &[("path", json!(out.display().to_string())), ("bytes", json!(n))]);
    Ok(format!("{} {n} bytes {summary}", out.display()))
}

pub fn safe_name(name: &str) -> String {
    let s: String = name.chars().map(|c| if c.is_ascii_alphanumeric() || matches!(c, '.' | '-' | '_') { c } else { '_' }).collect();
    s.trim_start_matches('.').to_string()
}

pub fn link(file: &Path) -> Result<String, String> {
    let abs = file.canonicalize().map_err(|e| format!("{}: {e}", file.display()))?;
    if !abs.is_file() {
        return Err(format!("{} is not a file", abs.display()));
    }
    let name = safe_name(&abs.file_name().unwrap_or_default().to_string_lossy());
    let dir = state::files();
    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    let at = dir.join(&name);
    let _ = std::fs::remove_file(&at);
    std::os::unix::fs::symlink(&abs, &at).map_err(|e| e.to_string())?;
    Ok(format!("/files/{name}"))
}
