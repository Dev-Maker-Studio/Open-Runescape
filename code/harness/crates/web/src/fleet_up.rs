use crate::fleet::{self, MAX};
use crate::launch::{self, Browser, Launch};
use crate::{page::Page, proc, server, state};
use std::time::{Duration, Instant};

const STAGGER: Duration = Duration::from_millis(250);
const CELL: (usize, usize) = (480, 340);

#[derive(Debug, PartialEq)]
pub struct Plan {
    pub count: usize,
    pub scenes: Vec<String>,
    pub flags: Vec<String>,
}

pub fn parse(args: &[String]) -> Result<Plan, String> {
    let mut p = Plan { count: MAX, scenes: vec![], flags: vec![] };
    let mut it = args.iter();
    while let Some(a) = it.next() {
        match a.as_str() {
            "--scene" => p.scenes.push(it.next().cloned().ok_or("--scene needs a name")?),
            f if f.starts_with("--") => p.flags.push(f.into()),
            n => p.count = n.parse().ok().filter(|c| (1..=MAX).contains(c)).ok_or(format!("slot count must be 1 to 9, got {n}"))?,
        }
    }
    Ok(p)
}

impl Plan {
    pub fn args(&self, n: usize) -> Vec<String> {
        self.scenes.get(n - 1).or(self.scenes.last()).cloned().into_iter().chain(self.flags.iter().cloned()).collect()
    }
}

pub fn window(n: usize) -> [String; 2] {
    let (col, row) = ((n - 1) % 3, (n - 1) / 3);
    [format!("--window-position={},{}", col * CELL.0, row * CELL.1), format!("--window-size={},{}", CELL.0, CELL.1)]
}

fn launch_slot(n: usize, args: &[String], display: &str, port: u16) -> Result<Browser, String> {
    fleet::select(n);
    std::fs::create_dir_all(fleet::slot_dir(n)).map_err(|e| e.to_string())?;
    let _ = std::os::unix::fs::symlink(fleet::dir().join("files"), state::files());
    let scene = args.iter().find(|a| !a.starts_with("--")).map_or(String::new(), |s| format!("?scene={s}"));
    let mut extra = window(n).to_vec();
    extra.extend(args.iter().filter(|a| a.starts_with("--")).cloned());
    let profile = state::profile();
    let _ = std::fs::remove_dir_all(&profile);
    let b = launch::launch(&Launch { display, url: &format!("http://127.0.0.1:{port}/{scene}"), profile: &profile, args: &extra, envs: &[], wrapper: &[] })?;
    state::write("browser", &format!("{} {} {}", b.pid, b.port, b.ws));
    state::write("args", &args.join(" "));
    Ok(b)
}

fn ready(b: &Browser) -> Result<bool, String> {
    let s = Page::at(&b.ws).and_then(|mut p| p.run("state", &[]))?;
    match s["lastError"].as_str() {
        Some(e) => Err(format!("page error: {e}")),
        None => Ok(s["ready"] == true && s["frames"].as_i64().unwrap_or(0) > 0),
    }
}

fn serve() -> Result<(u32, u16), String> {
    fleet::shared();
    if let Some(s) = state::server() {
        return Ok(s);
    }
    let files = fleet::dir().join("files");
    let s = server::start(&display::root()?, &files)?;
    state::write("server", &format!("{} {}", s.0, s.1));
    Ok(s)
}

pub fn up(args: &[String]) -> Result<(), String> {
    let (rest, skip) = fresh::strip(args);
    let plan = parse(&rest)?;
    let root = display::root()?;
    if !skip {
        fresh::web(&root)?;
    }
    let begin = Instant::now();
    let display = display::display_name()?;
    let (_, port) = serve()?;
    let mut loading: Vec<(usize, Browser, Instant)> = vec![];
    for n in 1..=plan.count {
        fleet::select(n);
        if let Some(b) = state::browser() {
            println!("slot {n} already up, pid {}", b.pid);
            continue;
        }
        let recorded = state::read("args").filter(|_| plan.scenes.is_empty() && plan.flags.is_empty());
        let launch_args = recorded.unwrap_or_else(|| plan.args(n));
        loading.push((n, launch_slot(n, &launch_args, &display, port)?, Instant::now()));
        std::thread::sleep(STAGGER);
    }
    fleet::shared();
    state::write("slots", &plan.count.to_string());
    let mut failed = 0;
    while !loading.is_empty() {
        loading.retain(|(n, b, t)| {
            let (done, why) = match ready(b) {
                Ok(true) => (true, None),
                Err(e) if e.starts_with("page error") => (true, Some(e)),
                _ if !proc::alive(b.pid) => (true, Some("chromium exited".to_string())),
                _ if t.elapsed() > Duration::from_secs(90) => (true, Some("not ready after 90s".to_string())),
                _ => (false, None),
            };
            match (done, why) {
                (true, None) => println!("slot {n} ready in {:.1}s, pid {}", t.elapsed().as_secs_f32(), b.pid),
                (true, Some(w)) => {
                    eprintln!("slot {n} failed ({w}), see {}", fleet::slot_dir(*n).join("profile/chromium.log").display());
                    failed += 1;
                }
                _ => {}
            }
            !done
        });
        std::thread::sleep(Duration::from_millis(100));
    }
    println!("web fleet of {} up in {:.1}s, {failed} failed", plan.count, begin.elapsed().as_secs_f32());
    if failed > 0 { Err("some slots failed".into()) } else { Ok(()) }
}

pub fn down() -> Result<(), String> {
    if !fleet::dir().is_dir() {
        return Ok(());
    }
    let pids: Vec<(usize, u32)> = (1..=MAX).filter_map(|n| {
        fleet::select(n);
        state::browser().map(|b| (n, b.pid))
    }).collect();
    fleet::shared();
    let server = state::server();
    let killers: Vec<_> = pids.iter().map(|&(n, p)| std::thread::spawn(move || { proc::kill(p); println!("slot {n} stopped, pid {p}"); })).collect();
    killers.into_iter().for_each(|k| { let _ = k.join(); });
    if let Some((pid, _)) = server {
        proc::kill(pid);
        println!("web server stopped, pid {pid}");
    }
    let _ = std::fs::remove_dir_all(fleet::dir());
    Ok(())
}
