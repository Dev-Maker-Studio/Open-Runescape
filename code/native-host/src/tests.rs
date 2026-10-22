use super::*;
use renderer::{frame, scene};
use serde_json::{json, Value};
use sha2::{Digest, Sha256};
use std::{
    io::{BufRead, BufReader},
    os::unix::net::UnixListener,
    sync::{atomic::AtomicU32, Mutex},
    thread::JoinHandle,
};

static NEXT: AtomicU32 = AtomicU32::new(0);

struct Fixture {
    c: Config,
    stop: Arc<AtomicBool>,
    commands: Arc<Mutex<Vec<Value>>>,
    threads: Vec<JoinHandle<()>>,
}

impl Fixture {
    fn new() -> Self {
        Self::with_limits(resources::Limits::default())
    }

    fn with_limits(limits: resources::Limits) -> Self {
        let dir = std::env::temp_dir().join(format!(
            "native-host-{}-{}",
            std::process::id(),
            NEXT.fetch_add(1, Ordering::Relaxed)
        ));
        std::fs::create_dir_all(&dir).unwrap();
        let control = dir.join("control.sock");
        let unix = UnixListener::bind(&control).unwrap();
        unix.set_nonblocking(true).unwrap();
        let listener = TcpListener::bind(("127.0.0.1", 0)).unwrap();
        let c = Config {
            control,
            spool: dir,
            port: listener.local_addr().unwrap().port(),
            fps: 5,
        };
        let stop = Arc::new(AtomicBool::new(false));
        let commands = Arc::new(Mutex::new(Vec::new()));
        let (end, seen) = (stop.clone(), commands.clone());
        let native = std::thread::spawn(move || {
            while !end.load(Ordering::Relaxed) {
                if let Ok((mut s, _)) = unix.accept() {
                    s.set_read_timeout(Some(Duration::from_secs(1))).unwrap();
                    let mut line = String::new();
                    BufReader::new(s.try_clone().unwrap())
                        .read_line(&mut line)
                        .unwrap();
                    let v: Value = serde_json::from_str(&line).unwrap();
                    seen.lock().unwrap().push(v);
                    writeln!(s, "{}", json!({"ok":true,"data":{"running":false}})).unwrap();
                } else {
                    std::thread::sleep(Duration::from_millis(2));
                }
            }
        });
        let (config, end) = (c.clone(), stop.clone());
        let host =
            std::thread::spawn(move || serve_with_limits(listener, config, end, limits).unwrap());
        Self {
            c,
            stop,
            commands,
            threads: vec![host, native],
        }
    }

    fn connect(&self) -> WebSocket<TcpStream> {
        let (ws, _) =
            tungstenite::connect(format!("ws://127.0.0.1:{}/?game=native", self.c.port)).unwrap();
        if let tungstenite::stream::MaybeTlsStream::Plain(s) = ws.get_ref() {
            s.set_read_timeout(Some(Duration::from_secs(3))).unwrap();
        }
        let socket = match ws.into_inner() {
            tungstenite::stream::MaybeTlsStream::Plain(s) => s,
            _ => panic!(),
        };
        WebSocket::from_raw_socket(socket, tungstenite::protocol::Role::Client, None)
    }

    fn wait_cmd(&self, cmd: &str) {
        let start = Instant::now();
        while !self
            .commands
            .lock()
            .unwrap()
            .iter()
            .any(|v| v["cmd"] == cmd)
        {
            assert!(start.elapsed() < Duration::from_secs(4), "missing {cmd}");
            std::thread::sleep(Duration::from_millis(5));
        }
    }

    fn publish(&self, n: u32, bytes: &[u8]) {
        let slot = n % 2;
        let file = format!("frame-{slot}.orf");
        std::fs::write(self.c.spool.join("frame.tmp"), bytes).unwrap();
        std::fs::rename(self.c.spool.join("frame.tmp"), self.c.spool.join(&file)).unwrap();
        let m = json!({"frame":n,"slot":slot,"file":file,"width":64,"height":64,"tick":n,"inputSeq":7,"nanos":u64::from(n),"sha256":format!("{:x}", Sha256::digest(bytes)),"summary":"synthetic"});
        std::fs::write(self.c.spool.join("latest.tmp"), m.to_string()).unwrap();
        std::fs::rename(
            self.c.spool.join("latest.tmp"),
            self.c.spool.join("latest.json"),
        )
        .unwrap();
    }

    fn status(&self, test: impl Fn(&Value) -> bool) -> Value {
        let t = Instant::now();
        loop {
            if let Some(v) = std::fs::read(self.c.spool.join("host.json"))
                .ok()
                .and_then(|b| serde_json::from_slice::<Value>(&b).ok())
                .filter(|v| test(v))
            {
                return v;
            }
            assert!(t.elapsed() < Duration::from_secs(4), "host status timeout");
            std::thread::sleep(Duration::from_millis(10));
        }
    }
}

impl Drop for Fixture {
    fn drop(&mut self) {
        self.stop.store(true, Ordering::Relaxed);
        while let Some(t) = self.threads.pop() {
            let _ = t.join();
        }
        let _ = std::fs::remove_dir_all(&self.c.spool);
    }
}

fn next_frame(ws: &mut WebSocket<TcpStream>, received: &mut stream::Stream) -> (u32, frame::Frame) {
    next_frame_inner(ws, received, true)
}

fn next_frame_inner(
    ws: &mut WebSocket<TcpStream>,
    received: &mut stream::Stream,
    epochs: bool,
) -> (u32, frame::Frame) {
    loop {
        match ws.read().unwrap() {
            Message::Binary(b) => {
                if let Some(reply) = received.push(&b).unwrap() {
                    if epochs || stream::decode_to_host(&reply) != Some(ToHost::Epochs) {
                        ws.send(Message::binary(reply)).unwrap();
                    }
                }
                if let Some(f) = received.take() {
                    return (received.stats.host_frame, f);
                }
            }
            Message::Text(e) => panic!("unexpected diagnostic {e}"),
            _ => {}
        }
    }
}

#[test]
fn websocket_frames_deduplicate_remap_forward_inputs_and_stop() {
    let fixture = Fixture::new();
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let mut received = stream::Stream::default();
    let mut f = scene::build("model", 0, 64, 64).unwrap();
    fixture.publish(1, &frame::write(&f));
    let (_, first) = next_frame(&mut ws, &mut received);
    assert!(received
        .hello
        .as_ref()
        .unwrap()
        .contains("\"game\":\"native\""));
    assert_eq!((first.meshes.len(), first.textures.len()), (1, 1));
    let id = first.draws[0].mesh;
    ws.send(Message::binary(stream::ack(1))).unwrap();
    let mesh = f.meshes[0].id;
    f.meshes[0].id = 999;
    let texture = f.textures[0].id;
    f.textures[0].id = 888;
    for d in &mut f.draws {
        if d.mesh == mesh {
            d.mesh = 999;
        }
        for t in &mut d.textures {
            if *t == texture {
                *t = 888;
            }
        }
    }
    fixture.publish(2, &frame::write(&f));
    let (_, second) = next_frame(&mut ws, &mut received);
    assert_eq!(
        (
            second.meshes.len(),
            second.textures.len(),
            second.draws[0].mesh
        ),
        (0, 0, id)
    );
    let input = stream::Input {
        seq: 17,
        kind: stream::KEY_DOWN,
        code: "Text:hello".into(),
        ..Default::default()
    };
    ws.send(Message::binary(stream::input(&input))).unwrap();
    fixture.wait_cmd("remote-input");
    let commands = fixture.commands.lock().unwrap();
    let v = commands
        .iter()
        .find(|v| v["cmd"] == "remote-input")
        .unwrap();
    assert_eq!(v["args"], json!(["17", "0", "0", "0", "0", "Text:hello"]));
    drop(commands);
    let rejected = tungstenite::connect(format!("ws://127.0.0.1:{}/", fixture.c.port)).unwrap_err();
    assert!(matches!(rejected, Error::Http(r) if r.status() == 503));
    ws.close(None).unwrap();
    fixture.wait_cmd("stream-stop");
}

#[test]
fn full_window_recovers_with_latest_frame_after_number_gap() {
    let fixture = Fixture::new();
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let bytes = frame::write(&scene::build("model", 0, 64, 64).unwrap());
    let mut received = stream::Stream::default();
    fixture.publish(10, &bytes);
    assert_eq!(next_frame(&mut ws, &mut received).0, 10);
    fixture.publish(20, &bytes);
    assert_eq!(next_frame(&mut ws, &mut received).0, 20);
    fixture.publish(30, &bytes);
    fixture.status(|v| v["frame"] == 30);
    fixture.publish(40, &bytes);
    let s = fixture.status(|v| v["frame"] == 40);
    assert_eq!(s["sent"], 2);
    ws.send(Message::binary(stream::ack(20))).unwrap();
    assert_eq!(next_frame(&mut ws, &mut received).0, 40);
    assert!(received.stats.host_dropped > 0);
    fixture.publish(50, &bytes);
    assert_eq!(next_frame(&mut ws, &mut received).0, 50);
    ws.close(None).unwrap();
    fixture.wait_cmd("stream-stop");
}

#[test]
fn malformed_and_racing_spool_are_bounded_and_recover() {
    let fixture = Fixture::new();
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let mut bomb = vec![0; 32];
    bomb[..4].copy_from_slice(b"ORF3");
    bomb.extend([1u32, u32::MAX, 1].into_iter().flat_map(u32::to_le_bytes));
    fixture.publish(1, &bomb);
    fixture.status(|v| v["errors"].as_u64().unwrap_or(0) > 0);
    let mut received = stream::Stream::default();
    loop {
        if matches!(ws.read().unwrap(), Message::Text(_)) {
            break;
        }
    }
    let bytes = frame::write(&scene::build("model", 0, 64, 64).unwrap());
    fixture.publish(2, &bytes);
    assert_eq!(next_frame(&mut ws, &mut received).0, 2);
    let m = source::latest(&fixture.c.spool).unwrap();
    std::fs::write(fixture.c.spool.join(&m.file), b"ORF3").unwrap();
    assert!(source::load(&fixture.c.spool, &m).unwrap().is_none());
    ws.close(None).unwrap();
    fixture.wait_cmd("stream-stop");
}

#[test]
fn resource_identity_is_content_not_original_id() {
    let mut sent = resources::Resources::default();
    let mut f = scene::build("model", 0, 64, 64).unwrap();
    let first = sent.split(&mut f).unwrap();
    let mut changed = scene::build("model", 0, 64, 64).unwrap();
    changed.meshes[0].vertices[0] ^= 1;
    let next = sent.split(&mut changed).unwrap();
    assert_ne!(first.meshes[0].id, next.meshes[0].id);
    assert_eq!(next.textures.len(), 0);
}

#[test]
fn text_payload_limit_is_utf8_bytes_and_diagnostics_do_not_leak() {
    let fixture = Fixture::new();
    let i = |text: &str| stream::Input {
        kind: stream::KEY_DOWN,
        code: format!("Text:{text}"),
        ..Default::default()
    };
    assert!(input(&fixture.c, i(&"é".repeat(64))).is_ok());
    assert!(input(&fixture.c, i(&format!("{}x", "é".repeat(64)))).is_err());
    assert!(input(&fixture.c, i("")).is_err());
    assert!(input(&fixture.c, i("a\nb")).is_err());
    assert!(Config::parse(
        &["--control", "/tmp/x", "--spool", "/tmp/y", "--fps", "31"].map(String::from)
    )
    .is_err());
}

#[test]
fn spool_is_exclusive_and_external_browser_origins_are_rejected() {
    use tungstenite::client::IntoClientRequest;
    let fixture = Fixture::new();
    fixture.status(|v| v["pid"].as_u64().is_some());
    let listener = TcpListener::bind(("127.0.0.1", 0)).unwrap();
    assert_eq!(
        serve(
            listener,
            fixture.c.clone(),
            Arc::new(AtomicBool::new(false))
        )
        .unwrap_err(),
        "native spool already hosted"
    );
    let mut request = format!("ws://127.0.0.1:{}/?game=native", fixture.c.port)
        .into_client_request()
        .unwrap();
    request
        .headers_mut()
        .insert("Origin", "https://example.org".parse().unwrap());
    let rejected = tungstenite::connect(request).unwrap_err();
    assert!(matches!(rejected, Error::Http(r) if r.status() == 403));
    assert!(!fixture
        .commands
        .lock()
        .unwrap()
        .iter()
        .any(|v| v["cmd"] == "stream-start"));
}

#[test]
#[ignore]
fn recorded_orf_from_environment() {
    let path = std::env::var("OPENRS_NATIVE_TEST_ORF")
        .expect("set OPENRS_NATIVE_TEST_ORF to an existing ORF3 recording");
    let bytes = std::fs::read(path).unwrap();
    let mut f = source::parse(&bytes).unwrap();
    let resources = resources::Resources::default().split(&mut f).unwrap();
    println!(
        "validated {}x{}, {} draws, {} meshes, {} textures",
        f.width,
        f.height,
        f.draws.len(),
        resources.meshes.len(),
        resources.textures.len()
    );
}

fn changing(n: u32) -> frame::Frame {
    let mut f = scene::build("model", 0, 64, 64).unwrap();
    f.meshes[0].vertices[..4].copy_from_slice(&(n as f32).to_le_bytes());
    f
}

#[test]
fn negotiated_websocket_rotates_hundreds_of_frames_without_control_restart() {
    let fixture = Fixture::with_limits(resources::Limits {
        ids: 4,
        bytes: 1024 * 1024,
        frames: 32,
    });
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let mut received = stream::Stream::default();
    for n in 1..=300 {
        fixture.publish(n, &frame::write(&changing(n)));
        let (number, f) = next_frame(&mut ws, &mut received);
        assert_eq!(number, n);
        let stats: Value = serde_json::from_str(&received.json()).unwrap();
        assert!(stats["epochResources"].as_u64().unwrap() <= 4);
        assert!(stats["epochBytes"].as_u64().unwrap() <= 1024 * 1024);
        if n % 3 == 1 {
            assert_eq!((f.meshes.len(), f.textures.len()), (1, 1));
        }
        assert_eq!(received.stats.input_seq, 7);
        ws.send(Message::binary(stream::ack(n))).unwrap();
    }
    assert_eq!(received.epoch, 99);
    assert_eq!(received.stats.received, 300);
    let commands = fixture.commands.lock().unwrap();
    assert_eq!(
        commands
            .iter()
            .filter(|v| v["cmd"] == "stream-start")
            .count(),
        1
    );
    assert!(!commands.iter().any(|v| v["cmd"] == "stream-stop"));
    drop(commands);
    ws.close(None).unwrap();
    fixture.wait_cmd("stream-stop");
}

#[test]
fn rotation_drains_both_frames_ignores_unsent_acks_and_still_forwards_input() {
    let fixture = Fixture::with_limits(resources::Limits {
        ids: 3,
        bytes: 1024 * 1024,
        frames: 4096,
    });
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let mut received = stream::Stream::default();
    for n in [10, 20] {
        fixture.publish(n, &frame::write(&changing(n)));
        assert_eq!(next_frame(&mut ws, &mut received).0, n);
    }
    fixture.publish(30, &frame::write(&changing(30)));
    fixture.status(|v| v["frame"] == 30);
    ws.send(Message::binary(stream::ack(10))).unwrap();
    ws.send(Message::binary(stream::ack(999))).unwrap();
    ws.send(Message::binary(stream::ack(10))).unwrap();
    ws.send(Message::binary(stream::input(&stream::Input {
        seq: 21,
        kind: stream::FOCUS,
        a: 1.,
        ..Default::default()
    })))
    .unwrap();
    fixture.wait_cmd("remote-input");
    fixture.publish(40, &frame::write(&changing(40)));
    let status = fixture.status(|v| v["frame"] == 40);
    assert_eq!(status["sent"], 2);
    assert_eq!(status["epoch"], 0);
    ws.send(Message::binary(stream::ack(20))).unwrap();
    let (number, f) = next_frame(&mut ws, &mut received);
    assert_eq!(number, 40);
    assert_eq!(received.epoch, 1);
    assert_eq!((f.meshes.len(), f.textures.len()), (1, 1));
    ws.send(Message::binary(stream::ack(20))).unwrap();
    ws.send(Message::binary(stream::ack(40))).unwrap();
    fixture.publish(50, &frame::write(&changing(50)));
    assert_eq!(next_frame(&mut ws, &mut received).0, 50);
    ws.close(None).unwrap();
    fixture.wait_cmd("stream-stop");
}

#[test]
fn legacy_connection_never_receives_reused_ids_or_reset() {
    let fixture = Fixture::with_limits(resources::Limits {
        ids: 2,
        bytes: 1024 * 1024,
        frames: 4096,
    });
    let mut ws = fixture.connect();
    fixture.wait_cmd("stream-start");
    let mut received = stream::Stream::default();
    fixture.publish(1, &frame::write(&changing(1)));
    next_frame_inner(&mut ws, &mut received, false);
    ws.send(Message::binary(stream::ack(1))).unwrap();
    fixture.publish(2, &frame::write(&changing(2)));
    loop {
        match ws.read().unwrap() {
            Message::Text(e) => {
                assert!(e.contains("reconnect with resource epochs"));
                break;
            }
            Message::Binary(b) => {
                assert_ne!(b[0], stream::RESET);
            }
            _ => {}
        }
    }
    fixture.wait_cmd("stream-stop");
}
