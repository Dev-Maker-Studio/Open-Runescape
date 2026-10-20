mod cdp;
mod cli;
mod cmd;
mod launch;
mod look;
mod page;
mod server;
mod state;
mod trail;
mod up;

pub use cdp::Cdp;
pub use launch::{launch, Browser, Launch, CHROMIUM, GPU};
pub use server::free_port;
pub use state::crashed;
pub use up::{down, up};

const USAGE: &str = "usage: openrs web up [scene] [--no-build] [--chromium-flags] | down | do <cmd> [args] [; cmd ...] | - | @file | look [path] | trail [N | since F | path] | run [scene] [--flags] | show | hide | toggle";

fn done(name: &str, r: Result<(), String>) -> i32 {
    r.map_or_else(|e| { eprintln!("web {name}: {e}"); 1 }, |()| 0)
}

pub fn run(args: &[String]) -> i32 {
    let rest = args.get(1..).unwrap_or(&[]);
    match args.first().map(String::as_str) {
        Some("up") => done("up", up::up(rest)),
        Some("down") => done("down", up::down()),
        Some("do") => cli::run(rest),
        Some("look") => look::run(rest),
        Some("trail") => trail::run(rest),
        Some("run") => done("run", up::start(rest).map(drop)),
        Some("show") => done("show", display::show()),
        Some("hide") => done("hide", display::hide()),
        Some("toggle") => done("toggle", display::toggle()),
        _ => {
            eprintln!("{USAGE}");
            2
        }
    }
}

#[cfg(test)]
mod tests;
