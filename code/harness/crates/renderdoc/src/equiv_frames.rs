use super::{analyze, capture, env, ensure_client, place, report, stop, web_side, Cfg, Res};
use std::time::Instant;

pub fn run(c: &Cfg, count: u32) -> Res<i32> {
    super::super::setup::verify(false)?;
    eprintln!("equiv: {count} RenderDoc capture/dump pairs; substantially slower than twin --frames");
    let display = display::display_name()?;
    std::fs::create_dir_all(&c.out).map_err(|e| e.to_string())?;
    let result = (|| {
        let (ident, _) = ensure_client(c, &display, &c.out.join("client-launch"))?;
        place(c)?;
        for i in 0..count {
            let dir = c.out.join("frames").join(format!("{i:04}"));
            std::fs::create_dir_all(dir.join("client")).map_err(|e| e.to_string())?;
            let orf = dir.join("frame.orf");
            let _ = std::fs::remove_file(&orf);
            capture::attach(&ident)?;
            capture::fire()?;
            control::send("dump-frame", &[orf.to_string_lossy().into_owned()])?;
            capture::collect(&dir.join("client/frame.rdc"))?;
        }
        for i in 0..count {
            let t = Instant::now();
            let mut frame = c.clone();
            frame.frames = None;
            frame.out = c.out.join("frames").join(format!("{i:04}"));
            let client = frame.out.join("client");
            analyze::run_to(&client.join("frame.rdc"), client.clone(), false)?;
            web_side(&frame, &display, &frame.out.join("web"), Some(frame.out.join("frame.orf")))?;
            let status = report(&frame, true, &format!("{{\"total\":{:.1}}}", t.elapsed().as_secs_f32()));
            if status > 1 {
                return Err(format!("frame {i}: report failed with {status}"));
            }
        }
        let mut cmd = env::script("equiv_frames.py", &[]);
        cmd.arg(&c.out).args(["--frames", &count.to_string(), "--scene", &c.scene]);
        cmd.status().map_err(|e| e.to_string()).map(|s| s.code().unwrap_or(2))
    })();
    if c.cold || result.is_err() {
        stop();
    }
    result
}
