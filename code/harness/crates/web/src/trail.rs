use serde_json::Value;
use std::io::Write;
use std::path::PathBuf;

pub fn dir() -> PathBuf {
    std::env::var_os("OPENRS_WEB_FOOTPRINTS").map(PathBuf::from).unwrap_or_else(|| control::trail::dir().join("web"))
}

pub fn line(t: u128, tick: i64, kind: &str, extra: &[(&str, Value)]) -> String {
    let fields: String = extra.iter().map(|(k, v)| format!(",\"{k}\":{v}")).collect();
    format!("{{\"t\":{t},\"tick\":{tick},\"kind\":\"{kind}\"{fields}}}")
}

pub fn append(tick: i64, kind: &str, extra: &[(&str, Value)]) {
    let _ = std::fs::create_dir_all(dir());
    if let Ok(mut f) = std::fs::OpenOptions::new().create(true).append(true).open(dir().join("trail.jsonl")) {
        let t = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_or(0, |d| d.as_millis());
        let _ = writeln!(f, "{}", line(t, tick, kind, extra));
    }
}

pub fn run(args: &[String]) -> i32 {
    if matches!(args, [p] if p == "path") {
        println!("{}", dir().join("trail.jsonl").display());
        return 0;
    }
    let text = std::fs::read_to_string(dir().join("trail.jsonl")).unwrap_or_default();
    match control::trail::select(&text.lines().collect::<Vec<_>>(), args) {
        Some(shown) => {
            shown.iter().for_each(|l| println!("{l}"));
            0
        }
        None => {
            eprintln!("usage: openrs web trail [N | since <frame> | path]");
            2
        }
    }
}
