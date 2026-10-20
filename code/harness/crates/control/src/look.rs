use crate::{conn::Conn, trail};
use serde_json::Value;

pub fn run(args: &[String]) -> i32 {
    let mut conn = match Conn::open() {
        Ok(c) => c,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    let path = args.first().cloned();
    let mut shot = vec!["screenshot".to_string()];
    shot.extend(path.clone());
    let forced = std::env::var_os("OPENRS_LOOK_VIA").is_some_and(|v| v == "display");
    let shot_path = match if forced { Err(String::new()) } else { conn.send(&shot[0], &shot[1..]) } {
        Ok(Value::String(p)) => p,
        _ => {
            let out = path.unwrap_or_else(|| trail::dir().join("shots").join(format!("{}-display.png", std::process::id())).display().to_string());
            let _ = std::fs::create_dir_all(std::path::Path::new(&out).parent().unwrap_or(std::path::Path::new(".")));
            match display::shot(&out) {
                Ok(()) => {
                    trail::append("shot", &format!("\"path\":\"{out}\",\"via\":\"display\""));
                    out
                }
                Err(e) => format!("unavailable ({e})"),
            }
        }
    };
    println!("screenshot: {shot_path}");
    match conn.send("describe", &[]) {
        Ok(Value::String(t)) => println!("{t}"),
        Ok(v) => println!("{v}"),
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    }
    0
}
