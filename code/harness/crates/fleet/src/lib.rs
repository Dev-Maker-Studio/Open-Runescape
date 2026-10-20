mod compare;
mod config;
mod creds;
mod down;
mod local;
mod proc;
mod status;
mod up;

fn at(args: &[String]) -> Result<usize, String> {
    let n: usize = args.first().and_then(|a| a.parse().ok()).filter(|n| (1..=9).contains(n)).ok_or("slot must be 1 to 9")?;
    std::env::set_var("OPENRS_CONTROL", up::sock(n));
    std::env::set_var("OPENRS_FOOTPRINTS", display::dir().join(format!("s{n}/fp")));
    Ok(n)
}

fn place(args: &[String]) -> Result<(), String> {
    let n = at(args)?;
    let cfg = config::Cfg::load()?;
    let slot = cfg.slot(n)?;
    let to: Vec<String> = if args.len() > 1 { args[1..].to_vec() } else { slot.start.iter().map(|v| v.to_string()).collect() };
    if proc::pid(&format!("slot{n}"), "darkan-client").is_some() {
        let mut cmd = vec!["command".to_string(), "tele".to_string()];
        cmd.extend(to);
        return if control::run(&cmd) == 0 { Ok(()) } else { Err("tele failed".into()) };
    }
    let mut cmd = vec!["pos".to_string(), slot.account];
    cmd.extend(to);
    if playerstore::run(&cmd) == 0 { Ok(()) } else { Err("pos failed".into()) }
}

pub fn run(args: &[String]) -> i32 {
    if std::env::var_os("OPENRS_STATE").is_none() {
        std::env::set_var("OPENRS_STATE", std::path::PathBuf::from(std::env::var("XDG_RUNTIME_DIR").unwrap_or("/tmp".into())).join("openrs-fleet"));
    }
    if let Ok(root) = display::root() {
        let _ = std::env::set_current_dir(root);
    }
    let rest = args.get(1..).unwrap_or(&[]);
    let result = match args.first().map(String::as_str) {
        Some("up") => up::up(rest),
        Some("down") => down::down(),
        Some("status") => status::status(),
        Some("place") => place(rest),
        Some("compare") => at(rest).and_then(|n| compare::compare(n, rest.get(1).map(String::as_str))),
        Some(c @ ("do" | "look" | "trail")) => {
            return match at(rest) {
                Ok(_) => {
                    let tail = &rest[1..];
                    match c {
                        "do" => control::run(tail),
                        "look" => control::look(tail),
                        _ => control::trail(tail),
                    }
                }
                Err(e) => {
                    eprintln!("fleet {c}: {e}");
                    2
                }
            }
        }
        _ => Err("usage: openrs fleet <up [N] | down | status | place <slot> [x y z] | compare <slot> [dir] | do <slot> <cmd...> | look <slot> [path] | trail <slot> [N | since T | path]>".into()),
    };
    match result {
        Ok(()) => 0,
        Err(e) => {
            eprintln!("fleet: {e}");
            1
        }
    }
}

#[cfg(test)]
mod tests;

pub fn validate() -> Result<Vec<(String, String, String, String)>, String> {
    let cfg = config::Cfg::load()?;
    let mut out = vec![];
    for n in 1..=9 {
        let s = cfg.slot(n)?;
        if s.start.len() != 3 {
            return Err(format!("slot {n}: start needs x y z"));
        }
        if s.creds != "local" {
            creds::password(&s)?;
        }
        let (host, lobby, world) = cfg.endpoint(&s)?;
        let e = (s.target(), host, lobby, world);
        if !out.contains(&e) {
            out.push(e);
        }
    }
    Ok(out)
}
