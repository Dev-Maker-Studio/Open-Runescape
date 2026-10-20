use crate::{cmd, page::Page, trail};
use serde_json::{json, Value};

pub fn run(args: &[String]) -> i32 {
    let mut page = match Page::open() {
        Ok(p) => p,
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    };
    let shot = match cmd::exec(&mut page, "screenshot", args) {
        Ok(Value::String(p)) => p,
        r => {
            eprintln!("canvas readback failed ({}), using the display", r.err().unwrap_or_default());
            let out = args.first().cloned().unwrap_or_else(|| trail::dir().join("shots").join(format!("{}-display.png", std::process::id())).display().to_string());
            let _ = std::fs::create_dir_all(std::path::Path::new(&out).parent().unwrap_or(std::path::Path::new(".")));
            match display::shot(&out) {
                Ok(()) => {
                    trail::append(page.frames, "shot", &[("path", json!(out)), ("via", json!("display"))]);
                    out
                }
                Err(e) => format!("unavailable ({e})"),
            }
        }
    };
    println!("screenshot: {shot}");
    match page.run("describe", &[]) {
        Ok(Value::String(t)) => println!("{t}"),
        Ok(v) => println!("{v}"),
        Err(e) => {
            eprintln!("{e}");
            return 1;
        }
    }
    0
}
