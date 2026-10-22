use crate::{page::Page, server, state};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const PORT: u16 = 8100;
const NEEDLE: &str = "openrs-games";

pub fn binary(root: &Path) -> PathBuf {
    root.join("code/games/target/release/openrs-games")
}

fn ensure(root: &Path) -> Result<PathBuf, String> {
    let bin = binary(root);
    let source = ["code/games/src", "code/games/Cargo.toml", "code/renderer/src"].iter().filter_map(|s| fresh::newest(&root.join(s))).max();
    if let Some(why) = fresh::stale(std::fs::metadata(&bin).and_then(|m| m.modified()).ok(), source) {
        let t = Instant::now();
        let ok = Command::new("cargo").current_dir(root.join("code/games")).args(["build", "--release", "-q"]).status().map_err(|e| format!("cargo: {e}"))?.success();
        if !ok {
            return Err("cargo build of code/games failed".into());
        }
        println!("rebuilt games host ({why}) in {:.1}s", t.elapsed().as_secs_f32());
    }
    Ok(bin)
}

pub fn host() -> Option<(u32, u16, String)> {
    let f = state::read("games")?;
    let (pid, port) = (f.first()?.parse().ok()?, f.get(1)?.parse().ok()?);
    state::alive(pid, NEEDLE).then(|| (pid, port, f.get(2).cloned().unwrap_or_default()))
}

pub fn crashed() -> bool {
    state::read("games").is_some() && host().is_none()
}

pub fn take(rest: &[String]) -> (Vec<String>, Option<String>) {
    match rest.iter().position(|a| a == "--game") {
        Some(i) => ([&rest[..i], rest.get(i + 2..).unwrap_or(&[])].concat(), rest.get(i + 1).cloned()),
        None => (rest.to_vec(), None),
    }
}

fn spawn(root: &Path, name: &str) -> Result<u16, String> {
    use std::os::unix::process::CommandExt;
    let bin = ensure(root)?;
    let port = server::free_port(PORT).ok_or("no free port from 8100")?;
    let log = std::fs::File::create(state::dir().join("games.log")).map_err(|e| e.to_string())?;
    let child = Command::new(&bin)
        .args(["serve", "--port", &port.to_string()])
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log)
        .process_group(0)
        .spawn()
        .map_err(|e| format!("{}: {e}", bin.display()))?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(5) {
        if TcpStream::connect(("127.0.0.1", port)).is_ok() {
            state::write("games", &format!("{} {port} {name}", child.id()));
            println!("games host pid {} at ws://127.0.0.1:{port}/?game={name} log {}", child.id(), state::dir().join("games.log").display());
            return Ok(port);
        }
        std::thread::sleep(Duration::from_millis(20));
    }
    Err(format!("openrs-games did not listen on {port}"))
}

pub fn start(rest: &[String]) -> Result<(Vec<String>, String), String> {
    let (rest, game) = take(rest);
    let remembered = || state::read("games").and_then(|f| f.get(2).cloned()).filter(|_| rest.iter().all(|a| a.starts_with("--")));
    let Some(name) = game.or_else(remembered) else { return Ok((rest, String::new())) };
    let port = match host() {
        Some((_, port, running)) if running == name => port,
        _ => {
            stop();
            spawn(&display::root()?, &name)?
        }
    };
    Ok((rest, format!("?game={name}&host=127.0.0.1:{port}")))
}

pub fn resume() -> Result<(), String> {
    if !crashed() {
        return Ok(());
    }
    let name = state::read("games").and_then(|f| f.get(2).cloned()).unwrap_or_default();
    state::remove("games");
    let port = spawn(&display::root()?, &name)?;
    if state::browser().is_some() {
        Page::open()?.run("game", &[name, format!("127.0.0.1:{port}")])?;
    }
    Ok(())
}

pub fn stop() {
    if let Some((pid, ..)) = host() {
        let _ = Command::new("kill").arg(pid.to_string()).status();
    }
    state::remove("games");
}

pub fn games(args: &[String]) -> i32 {
    if args.first().map(String::as_str) != Some("list") {
        eprintln!("usage: openrs games list");
        return 2;
    }
    match display::root().and_then(|r| ensure(&r)).and_then(|bin| Command::new(bin).arg("list").status().map_err(|e| e.to_string())) {
        Ok(s) if s.success() => 0,
        Ok(_) => 1,
        Err(e) => {
            eprintln!("games: {e}");
            1
        }
    }
}

#[cfg(test)]
mod tests {
    use super::take;

    #[test]
    fn game_flag_is_taken_out() {
        let a: Vec<String> = ["--no-sandbox", "--game", "walker", "model"].map(String::from).to_vec();
        assert_eq!(take(&a), (vec!["--no-sandbox".to_string(), "model".to_string()], Some("walker".to_string())));
        assert_eq!(take(&a[..1]), (vec!["--no-sandbox".to_string()], None));
        assert_eq!(take(&a[1..2]), (vec![], None));
    }
}
