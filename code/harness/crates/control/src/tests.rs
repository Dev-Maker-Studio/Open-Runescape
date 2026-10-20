use crate::{script, trail, Conn};
use serde_json::{json, Value};
use std::io::{BufRead, BufReader, Write};
use std::path::PathBuf;

fn s(a: &[&str]) -> Vec<String> {
    a.iter().map(|x| x.to_string()).collect()
}

fn tmp(name: &str) -> PathBuf {
    let d = std::env::temp_dir().join(format!("openrs-control-{}-{name}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::fs::create_dir_all(&d).unwrap();
    d
}

fn fixture(name: &str) -> String {
    format!("{}/testdata/{name}", env!("CARGO_MANIFEST_DIR"))
}

#[test]
fn words_honour_quotes() {
    assert_eq!(script::words("  walk 3222   3218 "), s(&["walk", "3222", "3218"]));
    assert_eq!(script::words("say \"hello world\" now"), s(&["say", "hello world", "now"]));
    assert_eq!(script::words("a\"b c\"d"), s(&["ab cd"]));
    assert_eq!(script::words("x \"\""), s(&["x", ""]));
    assert!(script::words("   ").is_empty());
}

#[test]
fn split_on_semicolons_drops_empty_commands() {
    assert_eq!(script::split(&s(&["pos", ";", "walk", "1", "2", ";", ";"])), vec![s(&["pos"]), s(&["walk", "1", "2"])]);
    assert_eq!(script::split(&s(&["pos;"])), vec![s(&["pos;"])]);
    assert!(script::split(&[]).is_empty());
}

#[test]
fn batch_from_file_and_stdin() {
    let want = vec![s(&["pos"]), s(&["walk", "3222", "3218"]), s(&["say", "hello world", "now"]), s(&["command", ""])];
    assert_eq!(script::load(&[format!("@{}", fixture("batch.txt"))]).unwrap(), want);
    let text = std::fs::read(fixture("batch.txt")).unwrap();
    assert_eq!(script::load_from(&s(&["-"]), text.as_slice()).unwrap(), want);
    assert_eq!(script::load_from(&s(&["state", ";", "pos"]), &b"ignored"[..]).unwrap(), vec![s(&["state"]), s(&["pos"])]);
    assert!(script::load(&s(&["@/nonexistent/openrs-batch"])).unwrap_err().starts_with("/nonexistent/openrs-batch: "));
}

#[test]
fn trail_selection() {
    let text = std::fs::read_to_string(fixture("trail.jsonl")).unwrap();
    let lines: Vec<&str> = text.lines().collect();
    let kinds = |a: &[&str]| trail::select(&lines, &s(a)).map(|v| v.iter().map(|l| serde_json::from_str::<Value>(l).map_or("bad".to_string(), |v| v["kind"].as_str().unwrap().to_string())).collect::<Vec<_>>());
    assert_eq!(kinds(&[]).unwrap().len(), 6);
    assert_eq!(kinds(&["2"]), Some(s(&["cmd", "shot"])));
    assert_eq!(kinds(&["100"]).unwrap().len(), 6);
    assert_eq!(kinds(&["0"]), Some(vec![]));
    assert_eq!(kinds(&["since", "10"]), Some(s(&["walk", "cmd", "shot"])));
    assert_eq!(kinds(&["since", "-1"]).unwrap(), s(&["up", "cmd", "walk", "bad", "cmd", "shot"]));
    assert_eq!(kinds(&["since", "21"]), Some(vec![]));
    assert_eq!(kinds(&["since", "1", "x"]), None);
    let many: Vec<String> = (0..30).map(|i| format!("{{\"tick\":{i}}}")).collect();
    let many: Vec<&str> = many.iter().map(String::as_str).collect();
    assert_eq!(trail::select(&many, &[]).unwrap()[0], "{\"tick\":10}");
}

#[test]
fn trail_append_writes_json_lines() {
    let d = tmp("trail");
    std::env::set_var("OPENRS_FOOTPRINTS", &d);
    trail::append("shot", "\"path\":\"/a.png\"");
    trail::append("up", "\"mode\":\"local\"");
    let text = std::fs::read_to_string(d.join("trail.jsonl")).unwrap();
    let v: Vec<Value> = text.lines().map(|l| serde_json::from_str(l).unwrap()).collect();
    assert_eq!((v[0]["kind"].clone(), v[0]["tick"].clone(), v[0]["path"].clone()), (json!("shot"), json!(-1), json!("/a.png")));
    assert_eq!(v[1]["mode"], "local");
    assert_eq!(crate::trail::run(&s(&["since", "-1"])), 0);
    assert_eq!(crate::trail::run(&s(&["a", "b", "c"])), 2);
}

trait Rw: std::io::Read + Write {}
impl<T: std::io::Read + Write> Rw for T {}
type Accept = Box<dyn FnOnce() -> Box<dyn Rw + Send> + Send>;

fn listen(name: &str) -> (String, Accept) {
    let sock = tmp(name).join("c.sock");
    match std::os::unix::net::UnixListener::bind(&sock) {
        Ok(l) => (sock.display().to_string(), Box::new(move || Box::new(l.accept().unwrap().0))),
        Err(_) => {
            let l = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
            (l.local_addr().unwrap().port().to_string(), Box::new(move || Box::new(l.accept().unwrap().0)))
        }
    }
}

fn answer(stream: impl std::io::Read + Write, replies: &[&str]) -> Vec<Value> {
    let mut r = BufReader::new(stream);
    let mut seen = vec![];
    for reply in replies {
        let mut line = String::new();
        if r.read_line(&mut line).unwrap() == 0 {
            break;
        }
        assert!(line.ends_with('\n') && !line[..line.len() - 1].contains('\n'));
        seen.push(serde_json::from_str(&line).unwrap());
        if !reply.is_empty() {
            writeln!(r.get_mut(), "{reply}").unwrap();
        }
    }
    seen
}

#[test]
fn request_response_framing() {
    let (at, accept) = listen("framing");
    let server = std::thread::spawn(move || {
        answer(
            accept(),
            &[
                r#"{"ok":true,"data":{"x":3222,"y":3218,"z":0}}"#,
                r#"{"ok":false,"error":"no such command"}"#,
                r#"{"ok":false}"#,
                r#"{"ok":true,"data":[{"ok":true,"data":1},{"ok":false,"error":"e"}]}"#,
                r#"{"ok":true,"data":"text"}"#,
                "not json",
            ],
        )
    });
    let mut c = Conn::open_at(&at).unwrap();
    assert_eq!(c.send("pos", &[]).unwrap(), json!({"x": 3222, "y": 3218, "z": 0}));
    assert_eq!(c.send("frob", &s(&["a b", "\"q\""])).unwrap_err(), "no such command");
    assert_eq!(c.send("state", &[]).unwrap_err(), "unknown error");
    let results = c.batch(&[s(&["wait-tick", "1"]), s(&["pos"])]).unwrap();
    assert_eq!(results, vec![json!({"ok": true, "data": 1}), json!({"ok": false, "error": "e"})]);
    assert_eq!(c.batch(&[s(&["pos"])]).unwrap_err(), "bad batch response: \"text\"");
    assert!(c.send("state", &[]).unwrap_err().starts_with("bad response: "));
    let seen = server.join().unwrap();
    assert_eq!(seen[0], json!({"cmd": "pos", "args": []}));
    assert_eq!(seen[1], json!({"cmd": "frob", "args": ["a b", "\"q\""]}));
    assert_eq!(seen[3], json!({"batch": [{"cmd": "wait-tick", "args": ["1"]}, {"cmd": "pos", "args": []}]}));
}

#[test]
fn closed_connection_and_missing_endpoint() {
    let (at, accept) = listen("closed");
    let server = std::thread::spawn(move || drop(accept()));
    let mut c = Conn::open_at(&at).unwrap();
    server.join().unwrap();
    let e = c.send("state", &[]).unwrap_err();
    assert!(e == "control connection closed" || e.contains("Broken pipe"), "{e}");
    assert!(Conn::open_at(tmp("none").join("c.sock").to_str().unwrap()).err().unwrap().starts_with("control endpoint "));
}

#[test]
fn tcp_port_endpoint() {
    let Ok(l) = std::net::TcpListener::bind("127.0.0.1:0") else { return };
    let port = l.local_addr().unwrap().port().to_string();
    let server = std::thread::spawn(move || answer(l.accept().unwrap().0, &[r#"{"ok":true,"data":7}"#]));
    assert_eq!(Conn::open_at(&port).unwrap().send("tick", &[]).unwrap(), json!(7));
    assert_eq!(server.join().unwrap()[0], json!({"cmd": "tick", "args": []}));
}

#[test]
fn milestones_from_client_log() {
    let log = "boot\n[Milestone] window 0.4s\nnoise [load] done total=3.2s extra\n[Milestone] login 5.0s\n";
    assert_eq!(crate::up::milestones(log), "window 0.4s, load done 3.2s, login 5.0s");
    assert_eq!(crate::up::milestones(""), "");
}
