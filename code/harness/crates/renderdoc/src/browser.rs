use crate::capture::{self, Opts};
use crate::{analyze, env::{self, Res}};
use std::io::{BufRead, BufReader};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::time::{Duration, Instant};

pub const FLAGS: [&str; 5] = ["--no-sandbox", "--disable-gpu-sandbox", "--disable-gpu-watchdog", "--enable-logging=stderr", "--v=0"];

const MODULE: &str = "const m = await import(new URL('./renderer.js', location.href));";

pub fn js(body: &str) -> String {
    format!("(async () => {{ try {{ {MODULE} {body} }} catch (e) {{ throw new Error(String(e)); }} }})()")
}

pub fn frame_of(o: &Opts) -> Res<Option<u32>> {
    match (&o.frame, o.seconds) {
        (Some(f), _) => f.parse().map(Some).map_err(|_| format!("bad --frame {f}")),
        (None, Some(_)) => Ok(None),
        (None, None) => Ok(Some(3)),
    }
}

pub fn page_url(port: u16, scene: Option<&str>) -> String {
    format!("http://127.0.0.1:{port}/?scene={}", scene.unwrap_or("all"))
}

struct Session {
    browser: Option<u32>,
    children: Vec<Child>,
    profile: PathBuf,
    log: PathBuf,
}

impl Drop for Session {
    fn drop(&mut self) {
        if let Some(pid) = self.browser {
            capture::stop(&pid.to_string());
            let t = Instant::now();
            while Path::new(&format!("/proc/{pid}")).exists() && t.elapsed() < Duration::from_secs(5) {
                std::thread::sleep(Duration::from_millis(50));
            }
        }
        for c in &mut self.children {
            let _ = c.kill();
            let _ = c.wait();
        }
        let _ = std::fs::copy(self.profile.join("chromium.log"), &self.log);
        let _ = std::fs::remove_dir_all(&self.profile);
    }
}

fn serve(s: &mut Session, files: &Path) -> Res<u16> {
    let root = env::root();
    fresh::web(&root)?;
    let port = web::free_port(18600).ok_or("no free port from 18600")?;
    let child = Command::new(fresh::server(&root))
        .env("PORT", port.to_string())
        .env("HOST", "127.0.0.1")
        .env("STATIC_DIR", root.join("code/web/static"))
        .env("FILES_DIR", files)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|e| format!("openrs-web: {e}"))?;
    s.children.push(child);
    let t = Instant::now();
    while TcpStream::connect(("127.0.0.1", port)).is_err() {
        if t.elapsed() > Duration::from_secs(5) {
            return Err(format!("openrs-web did not listen on {port}"));
        }
        std::thread::sleep(Duration::from_millis(20));
    }
    Ok(port)
}

fn until<T>(what: &str, secs: u64, mut f: impl FnMut() -> Res<Option<T>>) -> Res<T> {
    let t = Instant::now();
    let mut last = String::new();
    while t.elapsed() < Duration::from_secs(secs) {
        match f() {
            Ok(Some(v)) => return Ok(v),
            Ok(None) => {}
            Err(e) => last = e,
        }
        std::thread::sleep(Duration::from_millis(200));
    }
    Err(format!("timed out after {secs}s waiting for {what} {last}"))
}

pub fn capture(o: &Opts) -> Res {
    let rdc = o.out.join("frame.rdc");
    std::fs::create_dir_all(&o.out).map_err(|e| e.to_string())?;
    let _ = std::fs::remove_file(&rdc);
    let (disp, xvfb) = capture::display(o.display.clone())?;
    let base = std::env::var("XDG_RUNTIME_DIR").map_or_else(|_| std::env::temp_dir(), PathBuf::from);
    let mut s = Session { browser: None, children: xvfb.into_iter().collect(), profile: base.join(format!("openrs-capture-browser-{}", std::process::id())), log: o.out.join("app.log") };
    if let Some(orf) = &o.orf {
        let at = o.out.join("frame.orf");
        if std::fs::canonicalize(orf).ok() != std::fs::canonicalize(&at).ok() {
            std::fs::copy(orf, &at).map_err(|e| format!("{}: {e}", orf.display()))?;
        }
    }
    let url = match &o.url {
        Some(u) => u.clone(),
        None => page_url(serve(&mut s, &o.out)?, o.scene.as_deref()),
    };
    let layer = env::home().join("layer").to_string_lossy().into_owned();
    let envs = [("VK_ADD_IMPLICIT_LAYER_PATH".to_string(), layer), ("ENABLE_VULKAN_RENDERDOC_CAPTURE".into(), "1".into())];
    let args: Vec<String> = FLAGS.iter().map(|f| f.to_string()).chain(o.rest.iter().cloned()).collect();
    let b = web::launch(&web::Launch { display: &disp, url: &url, profile: &s.profile, args: &args, envs: &envs, wrapper: &[] })?;
    s.browser = Some(b.pid);
    let mut cdp = until("the page", 30, || web::Cdp::attach(&b.ws).map(Some))?;
    let mut eval = |body: &str| cdp.eval(&js(body));
    until("the renderer", 90, || Ok(eval("return m.frames();")?.as_u64().filter(|f| *f > 0)))?;
    let adapter = eval("const a = await navigator.gpu.requestAdapter(); return a ? `${a.info.vendor} ${a.info.architecture} ${a.info.description}`.trim() : 'none';")?;
    let adapter = adapter.as_str().unwrap_or("none").to_string();
    println!("browser adapter {adapter} url {url}");
    if adapter == "none" || adapter.contains("swiftshader") || adapter.starts_with("google") {
        return Err(format!("WebGPU runs on {adapter}, not the hardware adapter"));
    }
    if o.orf.is_some() {
        eval("const r = await fetch(new URL('/files/frame.orf', location.href)); if (!r.ok) throw new Error(`frame.orf ${r.status}`); m.replay_next(new Uint8Array(await r.arrayBuffer())); return m.frames();")?;
    }
    let mut watch = env::script("target.py", &[]);
    watch.arg(b.pid.to_string()).arg(&rdc).args(["60", "120"]).stdout(Stdio::piped());
    let mut child = watch.spawn().map_err(|e| e.to_string())?;
    let mut lines = BufReader::new(child.stdout.take().unwrap()).lines();
    let ready = lines.next().and_then(Result::ok).unwrap_or_default();
    let started = ready.starts_with("ready ");
    s.children.push(child);
    if !started {
        return Err("no RenderDoc target in the chromium GPU process".into());
    }
    let frame = match frame_of(o)? {
        Some(n) => eval(&format!("return m.capture_at(m.frames() + {n});"))?,
        None => {
            std::thread::sleep(Duration::from_secs(o.seconds.unwrap_or(0)));
            eval("return m.capture_at(0);")?
        }
    };
    let done = lines.next().and_then(Result::ok).unwrap_or_default();
    if !done.contains("\"success\": true") || !rdc.exists() {
        return Err(format!("browser capture of frame {frame} failed {done}, see {}", o.out.join("app.log").display()));
    }
    println!("captured page frame {frame} {ready}");
    Ok(())
}

pub fn run(args: &[String]) -> Res {
    let mut o = capture::parse("browser", args)?;
    if o.orf.is_some() && o.url.is_none() && o.scene.is_some() {
        return Err("--orf and --scene exclude each other".into());
    }
    o.orf = o.orf.map(|p| std::fs::canonicalize(&p).unwrap_or(p));
    crate::setup::verify(false)?;
    capture(&o)?;
    analyze::run(&o.out.join("frame.rdc"), o.out)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn s(a: &[&str]) -> Vec<String> {
        a.iter().map(|x| x.to_string()).collect()
    }

    #[test]
    fn browser_options_pick_the_frame() {
        let o = capture::parse("browser", &s(&["--orf", "/f.orf", "--frame", "7", "--url", "http://h/"])).unwrap();
        assert_eq!((frame_of(&o).unwrap(), o.orf, o.url.as_deref()), (Some(7), Some(PathBuf::from("/f.orf")), Some("http://h/")));
        assert_eq!(frame_of(&capture::parse("browser", &s(&["--seconds", "2"])).unwrap()).unwrap(), None);
        assert_eq!(frame_of(&capture::parse("browser", &[]).unwrap()).unwrap(), Some(3));
        assert!(frame_of(&capture::parse("browser", &s(&["--frame", "x"])).unwrap()).is_err());
        assert!(capture::parse("browser", &[]).unwrap().out.ends_with("capture/browser"));
    }

    #[test]
    fn page_helpers() {
        assert_eq!(page_url(18600, Some("water")), "http://127.0.0.1:18600/?scene=water");
        assert_eq!(page_url(1, None), "http://127.0.0.1:1/?scene=all");
        assert!(js("return m.frames();").starts_with("(async () => { try { const m = await import("));
        assert!(js("x").ends_with("x } catch (e) { throw new Error(String(e)); } })()"));
    }
}
