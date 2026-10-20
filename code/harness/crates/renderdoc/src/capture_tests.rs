use super::*;

fn s(a: &[&str]) -> Vec<String> {
    a.iter().map(|x| x.to_string()).collect()
}

fn tmp(name: &str) -> PathBuf {
    let d = std::env::temp_dir().join(format!("openrs-renderdoc-{}-{name}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::fs::create_dir_all(&d).unwrap();
    d
}

#[test]
fn field_reads_rdc_json() {
    let j = r#"{"success": true, "pid": 4242, "ident": 38920, "path": "/tmp/a, b.rdc", "error": ""}"#;
    assert_eq!(field(j, "success").as_deref(), Some("true"));
    assert_eq!(field(j, "pid").as_deref(), Some("4242"));
    assert_eq!(field(j, "ident").as_deref(), Some("38920"));
    assert_eq!(field(j, "path").as_deref(), Some("/tmp/a, b.rdc"));
    assert_eq!(field(j, "error").as_deref(), Some(""));
    assert_eq!(field(r#"{"success": false, "error": "no GL, giving up"}"#, "error").as_deref(), Some("no GL, giving up"));
    assert_eq!(field(j, "missing"), None);
}

#[test]
fn options_parse_with_defaults() {
    let o = parse("client", &[]).unwrap();
    assert_eq!((o.frame, o.seconds, o.retries, o.api.as_str(), o.display, o.exe, o.rest.len()), (None, None, 1, "vulkan", None, None, 0));
    assert!(o.out.ends_with("code/harness/target/capture/client"));
    let o = parse("exec", &s(&["--frame", "5", "--seconds", "9", "--out", "/x", "--display", ":3", "--retries", "2", "--api", "gl", "--", "app", "--frame", "1"])).unwrap();
    assert_eq!((o.frame.as_deref(), o.seconds, o.out, o.display.as_deref(), o.retries, o.api.as_str()), (Some("5"), Some(9), PathBuf::from("/x"), Some(":3"), 2, "gl"));
    assert_eq!(o.rest, s(&["app", "--frame", "1"]));
    assert_eq!(parse("web", &s(&["--exe", "/bin/r"])).unwrap().exe, Some(PathBuf::from("/bin/r")));
    assert_eq!(parse("web", &s(&["--seconds", "x"])).err().unwrap(), "bad --seconds");
    assert_eq!(parse("web", &s(&["--frame"])).err().unwrap(), "--frame needs a value");
    assert_eq!(parse("web", &s(&["--bogus"])).err().unwrap(), "unknown option --bogus");
}

#[test]
fn targets_build_commands() {
    let o = parse("client", &s(&["--jar", "/c.jar", "--", "-Dx=1", "host", "-Xmx2g", "443"])).unwrap();
    let (cmd, env, jvm) = target(&o).unwrap();
    assert!(jvm);
    assert_eq!(cmd[0], "java");
    assert_eq!(cmd[cmd.len() - 6..], s(&["-Dx=1", "-Xmx2g", "-jar", "/c.jar", "host", "443"]));
    assert_eq!(env, vec![("PULSE_SERVER", "unix:/nonexistent".to_string())]);
    let (cmd, env, jvm) = target(&parse("web", &s(&["--exe", "/r", "--", "--scene", "all"])).unwrap()).unwrap();
    assert_eq!((cmd, env.len(), jvm), (s(&["/r", "--scene", "all"]), 2, false));
    let (cmd, env, _) = target(&parse("exec", &s(&["--api", "gl", "--", "glxgears"])).unwrap()).unwrap();
    assert_eq!((cmd, env.len()), (s(&["glxgears"]), 0));
    assert_eq!(target(&parse("exec", &s(&["--", "vk"])).unwrap()).unwrap().1[0], ("WGPU_BACKEND", "vulkan".to_string()));
    assert_eq!(target(&parse("exec", &[]).unwrap()).err().unwrap(), "exec needs -- command args");
}

#[test]
fn diff_needs_both_summaries() {
    let (c, w) = (tmp("client"), tmp("web"));
    std::fs::write(c.join("summary.json"), "{}").unwrap();
    assert_eq!(analyze::diff(&c, &w, &[]).unwrap_err(), format!("{} has no summary.json, capture or analyze first", w.display()));
    assert!(analyze::diff(&w, &c, &[]).unwrap_err().starts_with(&w.display().to_string()));
}

#[test]
fn analysis_is_cached_when_summary_is_newer() {
    let d = tmp("cached");
    let (rdc, summary) = (d.join("frame.rdc"), d.join("summary.json"));
    std::fs::write(&rdc, "rdc").unwrap();
    std::fs::write(&summary, "{}").unwrap();
    let set = |p: &Path, t: std::time::SystemTime| std::fs::File::options().write(true).open(p).unwrap().set_modified(t).unwrap();
    set(&rdc, std::time::UNIX_EPOCH + Duration::from_secs(1000));
    set(&summary, std::time::SystemTime::now() + Duration::from_secs(86400));
    assert_eq!(analyze::run_to(&rdc, d.clone(), false), Ok(()));
    assert_eq!(std::fs::read_to_string(&summary).unwrap(), "{}");
    assert!(analyze::run_to(&d.join("absent.rdc"), tmp("absent"), false).unwrap_err().contains("absent.rdc"));
}

#[test]
fn gpu_lock_excludes_threads_and_processes() {
    let path = std::env::temp_dir().join(format!("openrs-gpu-lock-test-{}", std::process::id()));
    let held = crate::analyze::gpu_lock(&path).unwrap();
    let other = std::process::Command::new("flock").args(["-n"]).arg(&path).arg("true").status().unwrap();
    assert!(!other.success());
    let start = std::time::Instant::now();
    let p = path.clone();
    let waiter = std::thread::spawn(move || {
        let _g = crate::analyze::gpu_lock(&p).unwrap();
        start.elapsed()
    });
    std::thread::sleep(std::time::Duration::from_millis(300));
    drop(held);
    assert!(waiter.join().unwrap() >= std::time::Duration::from_millis(300));
    assert!(std::process::Command::new("flock").args(["-n"]).arg(&path).arg("true").status().unwrap().success());
    let _ = std::fs::remove_file(path);
}

#[test]
fn relative_paths_are_resolved_against_the_caller() {
    let here = std::env::current_dir().unwrap();
    let o = crate::capture::parse("client", &["--out".into(), "rel/dir".into(), "--jar".into(), "a.jar".into()]).unwrap();
    assert_eq!(o.out, here.join("rel/dir"));
    assert_eq!(o.exe, Some(here.join("a.jar")));
    let o = crate::capture::parse("client", &["--out".into(), "/abs/dir".into()]).unwrap();
    assert_eq!(o.out, std::path::PathBuf::from("/abs/dir"));
}

