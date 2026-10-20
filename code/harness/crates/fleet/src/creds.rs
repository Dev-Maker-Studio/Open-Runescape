use crate::config::Slot;
use serde_json::{json, Value};
use std::io::Read;

fn path() -> std::path::PathBuf {
    std::path::PathBuf::from(std::env::var("HOME").unwrap_or_default()).join(".config/openrs/fleet-local.json")
}

fn random() -> String {
    let mut b = [0u8; 12];
    let _ = std::fs::File::open("/dev/urandom").and_then(|mut f| f.read_exact(&mut b));
    b.iter().map(|x| format!("{x:02x}")).collect()
}

fn local(account: &str) -> Result<String, String> {
    let p = path();
    let mut v: Value = std::fs::read_to_string(&p).ok().and_then(|t| serde_json::from_str(&t).ok()).unwrap_or(json!({}));
    if let Some(s) = v[account].as_str() {
        return Ok(s.into());
    }
    let pass = random();
    v[account] = json!(pass);
    std::fs::create_dir_all(p.parent().unwrap()).map_err(|e| e.to_string())?;
    std::fs::write(&p, v.to_string()).map_err(|e| e.to_string())?;
    std::fs::set_permissions(&p, std::os::unix::fs::PermissionsExt::from_mode(0o600)).map_err(|e| e.to_string())?;
    Ok(pass)
}

pub fn password(slot: &Slot) -> Result<String, String> {
    match slot.creds.split_once(':') {
        None if slot.creds == "local" => local(&slot.account),
        Some(("env", var)) => std::env::var(var).map_err(|_| format!("slot {}: env {var} is not set", slot.n)),
        Some(("file", f)) => {
            let f = f.strip_prefix('~').map_or_else(|| f.to_string(), |r| format!("{}{r}", std::env::var("HOME").unwrap_or_default()));
            std::fs::read_to_string(&f).map(|s| s.trim().to_string()).map_err(|e| format!("slot {}: {f}: {e}", slot.n))
        }
        _ => Err(format!("slot {}: credentials must be local, env:NAME or file:PATH", slot.n)),
    }
}
