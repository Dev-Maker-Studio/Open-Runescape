use games::{dump, game, host};
use std::path::Path;

const USAGE: &str = "usage: openrs-games serve [--port P] | dump NAME N DIR [--frame S] [--size WxH] | list";

fn option<'a>(args: &'a [String], key: &str) -> Option<&'a str> {
    args.iter().position(|a| a == key).and_then(|i| args.get(i + 1)).map(String::as_str)
}

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let plain: Vec<&String> = args.iter().enumerate().filter(|(i, a)| !a.starts_with("--") && !args.get(i.wrapping_sub(1)).is_some_and(|p| p.starts_with("--"))).map(|(_, a)| a).collect();
    let code = match plain.first().map(|s| s.as_str()) {
        Some("list") => {
            game::NAMES.iter().for_each(|n| println!("{n}"));
            0
        }
        Some("serve") => {
            let port = option(&args, "--port").and_then(|p| p.parse().ok()).unwrap_or(host::PORT);
            match host::bind(port) {
                Ok(l) => {
                    println!("openrs-games listening on ws://127.0.0.1:{port}/?game=NAME games {}", game::NAMES.join(" "));
                    host::serve(l);
                    0
                }
                Err(e) => {
                    eprintln!("bind {port}: {e}");
                    1
                }
            }
        }
        Some("dump") if plain.len() >= 4 => {
            let start = option(&args, "--frame").and_then(|v| v.parse().ok()).unwrap_or(0);
            let (w, h) = option(&args, "--size").and_then(|v| v.split_once('x')).and_then(|(w, h)| Some((w.parse().ok()?, h.parse().ok()?))).unwrap_or((800, 600));
            match plain[2].parse::<u32>().map_err(|e| e.to_string()).and_then(|n| dump::write(plain[1], start, n, Path::new(plain[3]), w, h)) {
                Ok(paths) => {
                    paths.iter().for_each(|p| println!("{}", p.display()));
                    0
                }
                Err(e) => {
                    eprintln!("dump: {e}");
                    1
                }
            }
        }
        _ => {
            eprintln!("{USAGE}");
            2
        }
    };
    std::process::exit(code);
}
