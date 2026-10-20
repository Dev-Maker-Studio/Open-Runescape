type Run = fn(&[String]) -> i32;

const COMMANDS: &[(&str, &str, Run)] = &[
    ("up", "[shared] [--no-build] [-D.. -X.. client args]  one client in the world, rebuilding a stale jar and cache first", up),
    ("down", "[all]  stop the client, with all also the display, audio sink and local server", |a| done("down", control::down(a))),
    ("do", "<cmd> [args] [; cmd ...] | - | @file  control commands and batches for the client", control::run),
    ("look", "[path]  screenshot of the client frame and the world description", control::look),
    ("trail", "[N | since T | path]  footprints of commands and observed changes", control::trail),
    ("run", "[-D.. args]  start the client on the private display without autologin setup", |a| done("run", display::run(a))),
    ("show", "move the private display to the active workspace", |_| done("show", display::show())),
    ("hide", "move the private display back to its hidden workspace", |_| done("hide", display::hide())),
    ("toggle", "show or hide the private display", |_| done("toggle", display::toggle())),
    ("shot", "[path]  screenshot of the private display", |a| done("shot", display::shot(a.first().map_or("shot.png", |s| s)))),
    ("stop", "stop the client, the private display and the null audio sink", |_| done("stop", display::stop())),
    ("cache", "build [force] | status | verify [shared]  packed cache and the lobby CRC check", cache::run),
    ("player", "<command>  player files, interfaces, accounts and the local lobby and world", playerstore::run),
    ("fleet", "up [N] [--no-build] | down | status | place | do | look | trail | compare  up to nine clients", fleet),
    ("resume", "restart what a crash left behind: the fleet slots and the single client, with their local servers", resume),
    ("capture", "setup | verify | client | web | exec | analyze | summary | diff | selftest  RenderDoc captures", renderdoc::run),
    ("equiv", "<scene> | --live | history | scenes | test | down  client and renderer equivalence", renderdoc::equiv),
    ("doctor", "check everything the harness needs, PASS WARN FAIL with fixes", doctor::run),
    ("verify", "live end to end pass of the whole harness, JSON summary in code/harness/target/verify", verify::run),
    ("help", "this list", help),
];

fn done(name: &str, r: Result<(), String>) -> i32 {
    r.map_or_else(|e| { eprintln!("{name}: {e}"); 1 }, |()| 0)
}

fn build() -> Result<(), String> {
    fresh::ensure(&display::root()?)
}

fn up(args: &[String]) -> i32 {
    let (rest, skip) = fresh::strip(args);
    if !skip && control::ingame().is_none() {
        if let Err(e) = build() {
            eprintln!("up: {e}");
            return 1;
        }
    }
    done("up", control::up(&rest))
}

fn fleet(args: &[String]) -> i32 {
    let (rest, skip) = fresh::strip(args);
    if !skip && rest.first().is_some_and(|a| a == "up") {
        if let Err(e) = build() {
            eprintln!("fleet: {e}");
            return 1;
        }
    }
    fleet::run(&rest)
}

fn runtime(name: &str) -> std::path::PathBuf {
    std::path::PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join(name)
}

fn dead(pid_file: &std::path::Path) -> bool {
    std::fs::read_to_string(pid_file).ok().and_then(|s| s.trim().parse::<u32>().ok()).is_some_and(|p| !std::fs::read(format!("/proc/{p}/cmdline")).is_ok_and(|c| String::from_utf8_lossy(&c).contains("darkan-client")))
}

fn resume(_: &[String]) -> i32 {
    let fleet_dir = runtime("openrs-fleet");
    let slots: Vec<usize> = std::fs::read_dir(&fleet_dir).into_iter().flatten().flatten().filter_map(|e| e.file_name().to_str()?.strip_prefix("slot")?.parse().ok()).collect();
    let mut code = 0;
    let mut did = false;
    if let Some(n) = slots.iter().copied().filter(|n| dead(&fleet_dir.join(format!("slot{n}")))).max().map(|_| *slots.iter().max().unwrap_or(&0)) {
        println!("resuming fleet of {n}");
        code |= fleet(&["up".into(), n.to_string()]);
        did = true;
    }
    if dead(&runtime("openrs").join("client")) {
        println!("resuming the single client");
        code |= up(&[]);
        did = true;
    }
    if !did {
        println!("nothing to resume");
    }
    code
}

fn help(_: &[String]) -> i32 {
    println!("usage: openrs <command> [args]");
    COMMANDS.iter().for_each(|(name, line, _)| println!("  {name:<8} {line}"));
    0
}

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let Some(cmd) = args.first() else { std::process::exit(help(&[])) };
    match COMMANDS.iter().find(|c| c.0 == cmd) {
        Some(c) => std::process::exit((c.2)(&args[1..])),
        None => {
            eprintln!("unknown command: {cmd}, see openrs help");
            std::process::exit(2);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::COMMANDS;

    #[test]
    fn table_is_unique_and_documented() {
        let mut names: Vec<&str> = COMMANDS.iter().map(|c| c.0).collect();
        names.sort();
        names.dedup();
        assert_eq!(names.len(), COMMANDS.len());
        assert!(COMMANDS.iter().all(|c| !c.1.is_empty()));
        assert!(["up", "doctor", "verify", "help"].iter().all(|n| names.contains(n)));
    }
}
