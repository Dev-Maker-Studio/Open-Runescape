use crate::{conn::Conn, script};
use serde_json::Value;

fn show(value: &Value, pretty: bool) {
    match value {
        Value::String(text) => println!("{text}"),
        v if pretty => println!("{}", serde_json::to_string_pretty(v).unwrap_or_default()),
        v => println!("{v}"),
    }
}

pub fn run(args: &[String]) -> i32 {
    let commands = match script::load(args) {
        Ok(c) if c.is_empty() => vec![vec!["help".to_string()]],
        Ok(c) => c,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    let mut conn = match Conn::open() {
        Ok(c) => c,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    if commands.len() == 1 {
        return match conn.send(&commands[0][0], &commands[0][1..]) {
            Ok(v) => {
                show(&v, true);
                0
            }
            Err(e) => {
                eprintln!("{e}");
                1
            }
        };
    }
    match conn.batch(&commands) {
        Ok(results) => {
            let mut code = 0;
            for r in &results {
                if r["ok"] == true {
                    show(&r["data"], false);
                } else {
                    eprintln!("error: {}", r["error"].as_str().unwrap_or("unknown"));
                    code = 1;
                }
            }
            if results.len() < commands.len() && code == 0 {
                code = 1;
            }
            code
        }
        Err(e) => {
            eprintln!("{e}");
            1
        }
    }
}
