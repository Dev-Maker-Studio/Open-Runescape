use crate::{hypr, state, Res};
use std::path::Path;
use std::time::{Duration, Instant};

pub struct Xs {
    pub display: String,
    pub pid: u32,
}

fn pid_of(n: u32) -> Option<u32> {
    let want = format!("Xwayland\0:{n}\0");
    std::fs::read_dir("/proc").ok()?.flatten().find_map(|e| {
        let pid: u32 = e.file_name().to_str()?.parse().ok()?;
        let c = std::fs::read(e.path().join("cmdline")).ok()?;
        String::from_utf8_lossy(&c).contains(&want).then_some(pid)
    })
}

pub fn ensure() -> Res<Xs> {
    if let Some(s) = state::read("xserver") {
        let mut p = s.split(' ');
        let (d, pid) = (p.next().unwrap_or(""), p.next().and_then(|p| p.parse().ok()).unwrap_or(0));
        if state::alive(pid) {
            return Ok(Xs { display: d.into(), pid });
        }
    }
    let n = (100..140).find(|n| !Path::new(&format!("/tmp/.X11-unix/X{n}")).exists()).ok_or("no free display")?;
    hypr::spawn_hidden(&format!("Xwayland :{n} -geometry 1280x900 -noreset -nolisten tcp -decorate"))?;
    let t = Instant::now();
    while t.elapsed() < Duration::from_secs(10) {
        if Path::new(&format!("/tmp/.X11-unix/X{n}")).exists() {
            if let Some(pid) = pid_of(n) {
                state::write("xserver", &format!(":{n} {pid}"));
                return Ok(Xs { display: format!(":{n}"), pid });
            }
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    Err("Xwayland did not start".into())
}
