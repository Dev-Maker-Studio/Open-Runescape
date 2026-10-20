use std::io::Write;
use std::path::PathBuf;

pub fn dir() -> PathBuf {
    std::env::var_os("OPENRS_FOOTPRINTS").map(PathBuf::from).unwrap_or_else(|| display::root().unwrap_or_default().join("code/harness/target/footprints"))
}

pub fn append(kind: &str, extra: &str) {
    let _ = std::fs::create_dir_all(dir());
    if let Ok(mut f) = std::fs::OpenOptions::new().create(true).append(true).open(dir().join("trail.jsonl")) {
        let t = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_or(0, |d| d.as_millis());
        let _ = writeln!(f, "{{\"t\":{t},\"tick\":-1,\"kind\":\"{kind}\",{extra}}}");
    }
}

fn tick(line: &str) -> i64 {
    serde_json::from_str::<serde_json::Value>(line).ok().and_then(|v| v["tick"].as_i64()).unwrap_or(-1)
}

pub fn select<'a>(lines: &[&'a str], args: &[String]) -> Option<Vec<&'a str>> {
    match args {
        [] => Some(lines[lines.len().saturating_sub(20)..].to_vec()),
        [w, n] if w == "since" => {
            let n: i64 = n.parse().unwrap_or(0);
            Some(lines.iter().copied().filter(|l| tick(l) >= n).collect())
        }
        [n] => Some(lines[lines.len().saturating_sub(n.parse().unwrap_or(20))..].to_vec()),
        _ => None,
    }
}

pub fn run(args: &[String]) -> i32 {
    if matches!(args, [p] if p == "path") {
        println!("{}", dir().join("trail.jsonl").display());
        return 0;
    }
    let text = std::fs::read_to_string(dir().join("trail.jsonl")).unwrap_or_default();
    match select(&text.lines().collect::<Vec<_>>(), args) {
        Some(shown) => {
            shown.iter().for_each(|l| println!("{l}"));
            0
        }
        None => {
            eprintln!("usage: openrs trail [N | since <tick> | path]");
            2
        }
    }
}
