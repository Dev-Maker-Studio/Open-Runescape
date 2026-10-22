use crate::{fleet, state, up};

pub fn resume() -> Option<i32> {
    let mut code = None;
    if state::crashed() {
        println!("resuming the web client");
        let args = std::fs::read(state::dir().join("args.json")).ok().and_then(|b| serde_json::from_slice::<Vec<String>>(&b).ok()).unwrap_or_else(|| state::read("args").unwrap_or_default());
        code = Some(up::up(&args).map_or_else(|e| { eprintln!("web up: {e}"); 1 }, |()| 0));
    }
    if let Some(n) = fleet::crashed() {
        println!("resuming the web fleet of {n}");
        code = Some(code.unwrap_or(0) | fleet::run(&["up".into(), n.to_string()]));
    }
    code
}
