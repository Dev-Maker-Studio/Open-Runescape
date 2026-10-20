use crate::config::{Cfg, Slot};
use std::net::TcpStream;
use std::path::PathBuf;
use std::time::Duration;

fn open(port: &str) -> bool {
    port.parse().is_ok_and(|p| TcpStream::connect_timeout(&std::net::SocketAddr::from(([127, 0, 0, 1], p)), Duration::from_millis(300)).is_ok())
}

fn files() -> PathBuf {
    std::env::var_os("OPENRS_PLAYER_DIR").map(PathBuf::from).unwrap_or_else(|| display::root().unwrap_or_default().join("code/harness/target/local/files"))
}

fn ps(args: &[&str]) -> Result<(), String> {
    let a: Vec<String> = args.iter().map(|s| s.to_string()).collect();
    if playerstore::run(&a) == 0 { Ok(()) } else { Err(format!("player {} failed", args[0])) }
}

fn owned() -> Vec<usize> {
    std::fs::read_to_string(display::dir().join("servers")).unwrap_or_default().split_whitespace().filter_map(|i| i.parse().ok()).collect()
}

pub fn ensure(cfg: &Cfg, slots: &[Slot]) -> Result<(), String> {
    let mut done = vec![];
    for s in slots {
        let Some(i) = s.instance().filter(|i| !done.contains(i)) else { continue };
        done.push(i);
        let (_, lobby, world) = cfg.endpoint(s)?;
        if !open(&lobby) && playerstore::start_instance(i)? {
            let mut mine = owned();
            mine.push(i);
            std::fs::create_dir_all(display::dir()).map_err(|e| e.to_string())?;
            std::fs::write(display::dir().join("servers"), mine.iter().map(|i| format!("{i} ")).collect::<String>()).map_err(|e| e.to_string())?;
        }
        if !open(&lobby) || !open(&world) {
            return Err(format!("local instance {i}: lobby {lobby} or world {world} is not accepting connections"));
        }
    }
    Ok(())
}

pub fn release() {
    for i in owned() {
        if let Err(e) = playerstore::stop_instance(i) {
            eprintln!("local instance {i}: {e}");
        }
    }
}

pub fn prepare(slot: &Slot, pass: &str) -> Result<(), String> {
    let (key, dir) = (slot.account.to_lowercase().replace([' ', '/'], "_"), files());
    let [x, y, z] = [0, 1, 2].map(|i| slot.start.get(i).copied().unwrap_or(0).to_string());
    if !dir.join("accounts").join(format!("{key}.json")).exists() {
        ps(&["account", &slot.account, pass])?;
    }
    if !dir.join("players").join(format!("{key}.json")).exists() {
        ps(&["create", &slot.account, &x, &y, &z])?;
    }
    ps(&["pos", &slot.account, &x, &y, &z])?;
    ps(&["set", &slot.account, "chosenAccountType", "true"])
}
