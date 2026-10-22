use crate::{cdp, cmd, launch, page, server, state, trail};
use serde_json::{json, Value};
use std::net::TcpListener;
use std::path::Path;
use tungstenite::Message;

fn s(a: &[&str]) -> Vec<String> {
    a.iter().map(|x| x.to_string()).collect()
}

#[test]
fn cdp_request_and_reply_framing() {
    let r: Value = serde_json::from_str(&cdp::request(7, "Runtime.evaluate", json!({ "expression": "1" }), Some("S1"))).unwrap();
    assert_eq!(r, json!({ "id": 7, "method": "Runtime.evaluate", "params": { "expression": "1" }, "sessionId": "S1" }));
    let r: Value = serde_json::from_str(&cdp::request(1, "Target.getTargets", json!({}), None)).unwrap();
    assert!(r.get("sessionId").is_none());
    assert_eq!(cdp::reply(r#"{"method":"Target.attachedToTarget","params":{}}"#, 3), None);
    assert_eq!(cdp::reply(r#"{"id":2,"result":{}}"#, 3), None);
    assert_eq!(cdp::reply("not json", 3), None);
    assert_eq!(cdp::reply(r#"{"id":3,"result":{"a":1}}"#, 3), Some(Ok(json!({ "a": 1 }))));
    assert_eq!(cdp::reply(r#"{"id":3,"error":{"code":-32000,"message":"No target"}}"#, 3), Some(Err("No target".into())));
}

#[test]
fn cdp_evaluate_results() {
    assert_eq!(cdp::evaluated(json!({ "result": { "type": "number", "value": 42 } })), Ok(json!(42)));
    assert_eq!(cdp::evaluated(json!({ "result": { "type": "undefined" } })), Ok(Value::Null));
    let thrown = json!({ "result": {}, "exceptionDetails": { "text": "Uncaught", "exception": { "description": "Error: boom" } } });
    assert_eq!(cdp::evaluated(thrown), Err("Error: boom".into()));
    assert_eq!(cdp::evaluated(json!({ "exceptionDetails": { "text": "Uncaught" } })), Err("Uncaught".into()));
}

#[test]
fn page_target_skips_non_pages() {
    let t = json!({ "targetInfos": [
        { "type": "service_worker", "url": "http://x/sw.js", "targetId": "A" },
        { "type": "page", "url": "chrome://newtab/", "targetId": "B" },
        { "type": "page", "url": "http://127.0.0.1:8091/", "targetId": "C" }
    ] });
    assert_eq!(cdp::page_target(&t), Some("C".into()));
    assert_eq!(cdp::page_target(&json!({})), None);
}

#[test]
fn cdp_over_a_websocket() {
    let listener = TcpListener::bind("127.0.0.1:0").unwrap();
    let port = listener.local_addr().unwrap().port();
    let peer = std::thread::spawn(move || {
        let mut ws = tungstenite::accept(listener.accept().unwrap().0).unwrap();
        let mut seen = vec![];
        while let Ok(Message::Text(t)) = ws.read() {
            let v: Value = serde_json::from_str(t.as_str()).unwrap();
            seen.push(v.clone());
            let id = v["id"].clone();
            let result = match v["method"].as_str().unwrap() {
                "Target.getTargets" => json!({ "targetInfos": [{ "type": "page", "url": "http://127.0.0.1/", "targetId": "T" }] }),
                "Target.attachToTarget" => json!({ "sessionId": "S" }),
                _ => json!({ "result": { "value": { "ok": true, "data": "x".repeat(1 << 20), "frames": 9 } } }),
            };
            ws.send(Message::text(json!({ "method": "Runtime.consoleAPICalled", "params": {} }).to_string())).unwrap();
            ws.send(Message::text(json!({ "id": id, "result": result }).to_string())).unwrap();
        }
        seen
    });
    let mut c = cdp::Cdp::attach(&format!("ws://127.0.0.1:{port}/devtools/browser/abc")).unwrap();
    let v = c.eval(&page::expression("state", &[])).unwrap();
    assert_eq!(page::unwrap(&v).unwrap().as_str().unwrap().len(), 1 << 20);
    drop(c);
    let seen = peer.join().unwrap();
    assert_eq!(seen.iter().map(|v| v["id"].as_u64().unwrap()).collect::<Vec<_>>(), vec![1, 2, 3]);
    assert_eq!(seen[1]["params"]["targetId"], "T");
    assert_eq!(seen[2]["sessionId"], "S");
    assert_eq!(seen[2]["params"]["awaitPromise"], true);
}

#[test]
fn bridge_expression_and_envelope() {
    let e = page::expression("replay", &s(&["/files/a b.orf", "x\"y"]));
    assert!(e.contains(r#"window.openrs.run("replay", ["/files/a b.orf","x\"y"])"#));
    assert_eq!(page::unwrap(&json!({ "ok": true, "data": { "a": 1 }, "frames": 3 })), Ok(json!({ "a": 1 })));
    assert_eq!(page::unwrap(&json!({ "ok": false, "error": "wait timed out" })), Err("wait timed out".into()));
    assert_eq!(page::unwrap(&Value::Null), Err("unknown error".into()));
}

#[test]
fn devtools_port_file_and_launch_args() {
    assert_eq!(launch::active_port("41234\n/devtools/browser/0f-1\n"), Some((41234, "ws://127.0.0.1:41234/devtools/browser/0f-1".into())));
    assert_eq!(launch::active_port("41234\n"), None);
    assert_eq!(launch::active_port("x\n/devtools/browser/a"), None);
    let extra = s(&["--disable-gpu-sandbox"]);
    let l = launch::Launch { display: ":100", url: "http://127.0.0.1:8092/", profile: Path::new("/run/p"), args: &extra, envs: &[], wrapper: &[] };
    let a = launch::args(&l);
    assert_eq!(a[0], "--user-data-dir=/run/p");
    assert!(a.contains(&"--enable-unsafe-webgpu".to_string()) && a.contains(&"--use-angle=vulkan".to_string()) && a.contains(&"--remote-debugging-port=0".to_string()));
    assert!(a.contains(&"--app=http://127.0.0.1:8092/".to_string()));
    assert_eq!(a.last().unwrap(), "--disable-gpu-sandbox");
}

#[test]
fn batch_parsing_for_web_commands() {
    let c = control::script::load(&s(&["replay", "/tmp/a.orf", ";", "wait-frames", "3", ";", "screenshot"])).unwrap();
    assert_eq!(c, vec![s(&["replay", "/tmp/a.orf"]), s(&["wait-frames", "3"]), s(&["screenshot"])]);
    let c = control::script::load_from(&s(&["-"]), &b"# web\nresize 640x480\nscene \"all\"\n"[..]).unwrap();
    assert_eq!(c, vec![s(&["resize", "640x480"]), s(&["scene", "all"])]);
}

#[test]
fn names_ports_files_and_trail() {
    let _guard = crate::tests_input::ENV.lock().unwrap_or_else(|e| e.into_inner());
    assert_eq!(cmd::safe_name("frame 1.orf"), "frame_1.orf");
    assert_eq!(cmd::safe_name("../..x"), "_..x");
    assert_eq!(cmd::safe_name(".hidden"), "hidden");
    let held = TcpListener::bind("127.0.0.1:0").unwrap();
    let p = held.local_addr().unwrap().port();
    assert_ne!(server::free_port(p), Some(p));
    let d = std::env::temp_dir().join(format!("openrs-web-{}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::env::set_var("OPENRS_WEB_STATE", d.join("state"));
    std::env::set_var("OPENRS_WEB_FOOTPRINTS", d.join("fp"));
    std::fs::create_dir_all(&d).unwrap();
    std::fs::write(d.join("my frame.orf"), b"ORF3").unwrap();
    assert_eq!(cmd::link(&d.join("my frame.orf")).unwrap(), "/files/my_frame.orf");
    assert_eq!(std::fs::read(state::files().join("my_frame.orf")).unwrap(), b"ORF3");
    assert!(cmd::link(&d.join("missing.orf")).is_err());
    assert!(cmd::link(&d).is_err());
    assert!(state::browser().is_none() && !state::crashed());
    state::write("browser", "4000000000 9222 ws://127.0.0.1:9222/devtools/browser/x");
    assert!(state::browser().is_none() && state::crashed());
    state::write("server", &format!("{} 8091", std::process::id()));
    assert!(state::server().is_none());
    assert_eq!(trail::line(5, 12, "cmd", &[("cmd", json!("state")), ("ok", json!(true))]), r#"{"t":5,"tick":12,"kind":"cmd","cmd":"state","ok":true}"#);
    trail::append(3, "up", &[("url", json!("http://127.0.0.1:8092/"))]);
    trail::append(9, "shot", &[("path", json!("/a.png"))]);
    let text = std::fs::read_to_string(d.join("fp/trail.jsonl")).unwrap();
    let v: Vec<Value> = text.lines().map(|l| serde_json::from_str(l).unwrap()).collect();
    assert_eq!((v[0]["kind"].clone(), v[0]["tick"].clone(), v[1]["path"].clone()), (json!("up"), json!(3), json!("/a.png")));
    assert_eq!(control::trail::select(&text.lines().collect::<Vec<_>>(), &s(&["since", "5"])).unwrap().len(), 1);
    assert_eq!(trail::run(&s(&["1"])), 0);
    assert_eq!(trail::run(&s(&["a", "b", "c"])), 2);
    std::fs::remove_dir_all(&d).unwrap();
}

#[test]
fn audio_goes_to_the_null_sink_or_is_muted() {
    let (envs, mute) = launch::audio(Some("openrs_null".into()));
    assert!(envs.contains(&("PULSE_SINK".into(), "openrs_null".into())));
    assert!(envs.contains(&("PIPEWIRE_NODE".into(), "openrs_null".into())));
    assert_eq!(mute, None);
    let (envs, mute) = launch::audio(None);
    assert!(envs.is_empty());
    assert_eq!(mute, Some("--mute-audio"));
}

