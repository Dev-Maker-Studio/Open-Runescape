use crate::{proc, up};
use control::Conn;

pub fn down() -> Result<(), String> {
    let pids: Vec<(usize, u32)> = (1..=9).filter_map(|n| proc::pid(&format!("slot{n}"), "darkan-client").map(|p| (n, p))).collect();
    let loggers: Vec<_> = pids.iter().map(|&(n, _)| std::thread::spawn(move || Conn::open_at(&up::sock(n).display().to_string()).and_then(|mut c| c.send("logout", &[])))).collect();
    std::thread::sleep(std::time::Duration::from_secs(if loggers.is_empty() { 0 } else { 3 }));
    let killers: Vec<_> = pids.iter().map(|&(n, p)| std::thread::spawn(move || { proc::kill(p); println!("slot {n} stopped, pid {p}"); })).collect();
    killers.into_iter().for_each(|k| { let _ = k.join(); });
    if let Some(x) = proc::pid("xserver", "Xwayland") {
        proc::kill(x);
        println!("display stopped, pid {x}");
    }
    crate::local::release();
    let _ = std::fs::remove_dir_all(display::dir());
    Ok(())
}
