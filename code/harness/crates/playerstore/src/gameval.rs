use serde_json::Value;
use std::fs;

pub fn load(kind: &str) -> Result<Value, String> {
    let dir = std::env::var_os("OPENRS_GAMEVAL_DIR").map_or_else(|| crate::store::repo().join("assets/cache/gamevals"), Into::into);
    let file = dir.join(format!("{kind}.json"));
    let text = fs::read_to_string(&file).map_err(|e| format!("{}: {e}", file.display()))?;
    serde_json::from_str(&text).map_err(|e| e.to_string())
}

pub fn name(kind: &str, key: &str) -> Option<String> {
    load(kind).ok()?["entries"][key].as_str().map(String::from)
}

pub fn resolve(kind: &str, token: &str) -> Result<i64, String> {
    if let Ok(n) = token.parse() {
        return Ok(n);
    }
    load(kind)?["entries"]
        .as_object()
        .and_then(|m| m.iter().find(|(_, v)| v.as_str() == Some(token)))
        .and_then(|(k, _)| k.parse().ok())
        .ok_or_else(|| format!("no {kind} named {token}"))
}

pub fn component(iface: i64, token: &str) -> Result<i64, String> {
    if let Ok(n) = token.parse() {
        return Ok(n);
    }
    let full = name("interface", &iface.to_string()).map(|i| format!("{i}:{token}"));
    load("component")?["entries"]
        .as_object()
        .and_then(|m| {
            let prefix = format!("{iface}:");
            m.iter().filter(|(k, _)| k.starts_with(&prefix)).find(|(_, v)| v.as_str() == Some(token) || Some(v.as_str().unwrap_or("")) == full.as_deref())
        })
        .and_then(|(k, _)| k.split(':').nth(1)?.parse().ok())
        .ok_or_else(|| format!("no component {token} in interface {iface}"))
}

pub fn components(iface: i64) -> Vec<(i64, String)> {
    let prefix = format!("{iface}:");
    let mut out: Vec<(i64, String)> = load("component")
        .ok()
        .and_then(|d| d["entries"].as_object().cloned())
        .into_iter()
        .flatten()
        .filter_map(|(k, v)| Some((k.strip_prefix(&prefix)?.parse().ok()?, v.as_str()?.to_string())))
        .collect();
    out.sort();
    out
}
