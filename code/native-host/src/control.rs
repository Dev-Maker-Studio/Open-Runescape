use serde_json::{json, Value};
use std::{
    io::{BufRead, BufReader, Read, Write},
    os::unix::net::UnixStream,
    path::Path,
    time::Duration,
};

pub fn command(socket: &Path, cmd: &str, args: &[String]) -> Result<Value, String> {
    let mut s =
        UnixStream::connect(socket).map_err(|_| "native control unavailable".to_string())?;
    s.set_read_timeout(Some(Duration::from_secs(2)))
        .map_err(|e| e.to_string())?;
    s.set_write_timeout(Some(Duration::from_secs(2)))
        .map_err(|e| e.to_string())?;
    writeln!(s, "{}", json!({"cmd":cmd,"args":args}))
        .map_err(|_| "native control write failed".to_string())?;
    let mut line = String::new();
    BufReader::new(s)
        .take(65537)
        .read_line(&mut line)
        .map_err(|_| "native control reply timed out".to_string())?;
    if line.len() > 65536 || !line.ends_with('\n') {
        return Err("native control reply exceeds limit".into());
    }
    let v: Value =
        serde_json::from_str(&line).map_err(|_| "invalid native control reply".to_string())?;
    if v["ok"] != true {
        return Err(format!(
            "native {cmd} rejected; inspect stream-state on native control"
        ));
    }
    Ok(v["data"].clone())
}
