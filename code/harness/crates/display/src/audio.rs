use std::process::Command;

pub const SINK: &str = "openrs_null";

fn pactl(args: &[&str]) -> Option<String> {
    let o = Command::new("pactl").args(args).output().ok()?;
    o.status.success().then(|| String::from_utf8_lossy(&o.stdout).into_owned())
}

pub fn null_sink() -> Result<String, String> {
    let have = pactl(&["list", "short", "sinks"]).ok_or("pactl unavailable")?;
    if !have.lines().any(|l| l.split('\t').nth(1) == Some(SINK)) {
        let id = pactl(&["load-module", "module-null-sink", &format!("sink_name={SINK}"), "sink_properties=device.description=OpenRS-Null"])
            .ok_or("null sink load failed")?;
        crate::state::write("sink", id.trim());
    }
    Ok(SINK.into())
}

pub fn unload() {
    if let Some(id) = crate::state::read("sink") {
        pactl(&["unload-module", &id]);
    }
}
