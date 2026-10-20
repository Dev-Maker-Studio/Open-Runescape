use std::path::{Path, PathBuf};
use std::process::Command;

fn renderer(root: &Path) -> Result<PathBuf, String> {
    let exe = std::env::var_os("OPENRS_RENDERER_EXE").map(PathBuf::from).unwrap_or_else(|| root.join("code/renderer/target/release/renderer-app"));
    exe.is_file().then_some(exe).ok_or("missing code/renderer/target/release/renderer-app, run cargo build --release in code/renderer".into())
}

fn python(root: &Path) -> PathBuf {
    root.join("code/renderdoc/.env/v/bin/python")
}

pub fn line(slot: usize, m: &serde_json::Value, replay: &str) -> String {
    format!(
        "slot {slot} ssim {} psnr {} differing {}% replay: {} diff {}",
        m["ssim"], m["psnr_db"], m["differing_pixels_pct"], replay.trim(), m["diff_png"].as_str().unwrap_or("")
    )
}

pub fn compare(slot: usize, out: Option<&str>) -> Result<(), String> {
    let root = display::root()?;
    let dir = out.map(PathBuf::from).unwrap_or_else(|| root.join(format!("code/harness/target/compare/s{slot}")));
    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    let (orf, client, web, diff) = (dir.join("frame.orf"), dir.join("client.png"), dir.join("web.png"), dir.join("diff.png"));
    let me = std::env::current_exe().map_err(|e| e.to_string())?;
    let s = slot.to_string();
    let grab = Command::new(&me).args(["fleet", "do", &s, "dump-frame"]).arg(&orf).args([";", "screenshot"]).arg(&client).output().map_err(|e| e.to_string())?;
    if !grab.status.success() || !orf.is_file() || !client.is_file() {
        return Err(format!("slot {slot} did not dump a frame and screenshot: {}", String::from_utf8_lossy(&grab.stderr).trim()));
    }
    let rep = Command::new(renderer(&root)?).arg("--replay").arg(&orf).arg("--out").arg(&web).output().map_err(|e| e.to_string())?;
    let replay = String::from_utf8_lossy(&rep.stdout).lines().chain(String::from_utf8_lossy(&rep.stderr).lines()).find(|l| l.starts_with("replay:")).unwrap_or("").trim_start_matches("replay:").to_string();
    if !rep.status.success() || !web.is_file() {
        return Err(format!("renderer replay failed: {}", String::from_utf8_lossy(&rep.stderr).lines().last().unwrap_or("")));
    }
    let code = "import sys,json;sys.path.insert(0,sys.argv[1]);import diff;print(json.dumps(diff.pixels(sys.argv[2],sys.argv[3],sys.argv[4])))";
    let px = Command::new(python(&root)).args(["-c", code]).arg(root.join("code/renderdoc/py")).arg(&client).arg(&web).arg(&diff).output().map_err(|e| e.to_string())?;
    let m: serde_json::Value = serde_json::from_slice(&px.stdout).map_err(|_| format!("pixel diff failed: {}", String::from_utf8_lossy(&px.stderr).lines().last().unwrap_or("")))?;
    let rec = serde_json::json!({"slot": slot, "time": std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_or(0, |d| d.as_secs()), "exe": renderer(&root)?.display().to_string(), "replay": replay.trim(), "pixels": m});
    std::fs::write(dir.join("compare.json"), rec.to_string()).map_err(|e| e.to_string())?;
    let hist = dir.parent().unwrap_or(&dir).join(format!("history-s{slot}.jsonl"));
    let mut f = std::fs::OpenOptions::new().create(true).append(true).open(&hist).map_err(|e| e.to_string())?;
    std::io::Write::write_all(&mut f, format!("{rec}\n").as_bytes()).map_err(|e| e.to_string())?;
    println!("{}", line(slot, &m, &replay));
    Ok(())
}
