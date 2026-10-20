mod analyze;
mod browser;
mod capture;
mod env;
mod equiv;
pub use equiv::SCENES;
mod setup;

pub use setup::problems;

use std::path::Path;

pub fn equiv(args: &[String]) -> i32 {
    equiv::run(args)
}

pub fn run(args: &[String]) -> i32 {
    let rest = args.get(1..).unwrap_or_default();
    let r = match args.first().map(String::as_str) {
        Some("setup") => setup::install(),
        Some("verify") => setup::verify(rest.first().is_some_and(|a| a == "--full")),
        Some("analyze") if !rest.is_empty() => analyze::run(Path::new(&rest[0]), rest.get(1).map_or_else(|| Path::new(&rest[0]).with_extension(""), Into::into)),
        Some("diff") if rest.len() >= 2 => analyze::diff(Path::new(&rest[0]), Path::new(&rest[1]), &rest[2..]),
        Some("summary") if !rest.is_empty() => analyze::overview(Path::new(&rest[0]), &rest[1..]),
        Some("selftest") => capture::selftest(),
        Some("browser") => browser::run(rest),
        Some(k @ ("client" | "web" | "exec")) => capture::run(k, rest),
        _ => Err("usage: openrs capture setup | verify [--full] | client [opts] [-- java args] | web [opts] [-- app args] | browser [opts] [--url URL] [--scene NAME] [--orf FILE] [-- chromium args] | exec [opts] -- cmd args | analyze <rdc> [dir] | summary <dir> [max-draws] | diff <client-dir> <web-dir> [out-dir] [max-lines] | selftest\n  opts: --frame N | --seconds S, --out DIR, --display :N, --retries N, --jar PATH (client), --exe PATH (web), --api gl|vulkan (exec)".into()),
    };
    r.map_or_else(|e| { eprintln!("capture: {e}"); 1 }, |()| 0)
}
