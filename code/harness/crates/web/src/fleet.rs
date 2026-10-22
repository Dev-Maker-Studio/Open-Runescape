use crate::{cli, look, state, trail};
use std::path::PathBuf;

pub const MAX: usize = 9;

const USAGE: &str = "usage: openrs web fleet <up [N] [--scene NAME]... [--no-build] | down | status | do <slot> <cmd...> | look <slot> [path] | trail <slot> [N | since F | path]>";

pub fn dir() -> PathBuf {
    std::env::var_os("OPENRS_WEB_FLEET_STATE").map(PathBuf::from).unwrap_or_else(|| PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join("openrs-web-fleet"))
}

pub fn slot_dir(n: usize) -> PathBuf {
    dir().join(format!("s{n}"))
}

pub fn shared() {
    std::env::set_var("OPENRS_WEB_STATE", dir());
    std::env::remove_var("OPENRS_WEB_FOOTPRINTS");
}

pub fn select(n: usize) {
    std::env::set_var("OPENRS_WEB_STATE", slot_dir(n));
    std::env::set_var("OPENRS_WEB_FOOTPRINTS", slot_dir(n).join("fp"));
}

pub fn slots() -> usize {
    if !dir().is_dir() {
        return 0;
    }
    shared();
    state::read("slots").and_then(|s| s.first()?.parse().ok()).unwrap_or(0)
}

pub fn crashed() -> Option<usize> {
    let n = slots();
    let dead = (1..=n).any(|s| {
        select(s);
        state::crashed()
    });
    shared();
    (dead || (n > 0 && state::read("server").is_some() && state::server().is_none())).then_some(n)
}

fn slot(args: &[String]) -> Result<usize, String> {
    let n = args.first().and_then(|a| a.parse().ok()).filter(|n| (1..=MAX).contains(n)).ok_or("slot must be 1 to 9")?;
    select(n);
    Ok(n)
}

pub fn run(args: &[String]) -> i32 {
    let rest = args.get(1..).unwrap_or(&[]);
    let result = match args.first().map(String::as_str) {
        Some("up") => crate::fleet_up::up(rest),
        Some("down") => crate::fleet_up::down(),
        Some("status") => crate::fleet_status::status(),
        Some(c @ ("do" | "look" | "trail")) => {
            return match slot(rest) {
                Ok(_) => match c {
                    "do" => cli::run(&rest[1..]),
                    "look" => look::run(&rest[1..]),
                    _ => trail::run(&rest[1..]),
                },
                Err(e) => {
                    eprintln!("web fleet {c}: {e}");
                    2
                }
            };
        }
        _ => {
            eprintln!("{USAGE}");
            return 2;
        }
    };
    result.map_or_else(|e| { eprintln!("web fleet: {e}"); 1 }, |()| 0)
}
