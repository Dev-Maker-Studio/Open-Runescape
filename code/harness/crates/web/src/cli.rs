use crate::{cmd, page::Page, trail};
use serde_json::{json, Value};

fn show(value: &Value, pretty: bool) {
    match value {
        Value::String(text) => println!("{text}"),
        v if pretty => println!("{}", serde_json::to_string_pretty(v).unwrap_or_default()),
        v => println!("{v}"),
    }
}

pub fn run(args: &[String]) -> i32 {
    let commands = match control::script::load(args) {
        Ok(c) if c.is_empty() => vec![vec!["help".to_string()]],
        Ok(c) => c,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    let mut page = match Page::open() {
        Ok(p) => p,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    let single = commands.len() == 1;
    for c in &commands {
        let r = cmd::exec(&mut page, &c[0], &c[1..]);
        let mut extra = vec![("cmd", json!(c[0])), ("args", json!(&c[1..])), ("ok", json!(r.is_ok()))];
        if let Err(e) = &r {
            extra.push(("error", json!(e)));
        }
        trail::append(page.frames, "cmd", &extra);
        match r {
            Ok(v) => show(&v, single),
            Err(e) => {
                eprintln!("{}{e}", if single { "" } else { "error: " });
                return 1;
            }
        }
    }
    0
}
