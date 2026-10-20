use serde_json::Value;
use std::fs;
use std::path::{Path, PathBuf};

pub fn repo() -> PathBuf {
    let mut dir = std::env::current_dir().unwrap_or_default();
    while !dir.join("_APP-SPECIFICATION.md").exists() {
        if !dir.pop() {
            return std::env::current_dir().unwrap_or_default();
        }
    }
    dir
}

pub fn root() -> PathBuf {
    std::env::var_os("OPENRS_PLAYER_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|| repo().join("code/harness/target/local/files"))
}

pub fn key(name: &str) -> String {
    name.to_lowercase().replace(' ', "_").replace('/', "_")
}

fn players() -> PathBuf {
    root().join("players")
}

pub fn path(name: &str) -> PathBuf {
    players().join(format!("{}.json", key(name)))
}

pub fn list() -> Vec<String> {
    let mut names: Vec<String> = fs::read_dir(players())
        .into_iter()
        .flatten()
        .flatten()
        .map(|e| e.path())
        .filter(|p| p.extension().is_some_and(|x| x == "json"))
        .filter_map(|p| p.file_stem().map(|s| s.to_string_lossy().into_owned()))
        .collect();
    names.sort();
    names
}

pub fn read(name: &str) -> Result<Value, String> {
    let p = path(name);
    let text = fs::read_to_string(&p).map_err(|e| format!("{}: {e}", p.display()))?;
    serde_json::from_str(&text).map_err(|e| format!("{}: {e}", p.display()))
}

pub fn write_to(p: &Path, v: &Value) -> Result<(), String> {
    if let Some(dir) = p.parent() {
        fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    }
    let tmp = p.with_extension("json.tmp");
    let text = serde_json::to_string_pretty(v).map_err(|e| e.to_string())?;
    fs::write(&tmp, text).and_then(|_| fs::rename(&tmp, p)).map_err(|e| e.to_string())
}

pub fn write(name: &str, v: &Value) -> Result<(), String> {
    write_to(&path(name), v)
}

pub fn account(name: &str) -> Value {
    let k = key(name);
    serde_json::json!({
        "username": k,
        "email": format!("{k}@bots.darkan.org"),
        "recoveryEmail": format!("{k}@bots.darkan.org"),
        "displayName": name,
        "passwordHash": "",
        "bot": true
    })
}

pub fn write_account(name: &str) -> Result<(), String> {
    write_to(&root().join("accounts").join(format!("{}.json", key(name))), &account(name))
}
