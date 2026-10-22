mod control;
mod resources;
mod source;

use renderer::stream::{self, ToHost};
use serde::Serialize;
use std::{
    collections::VecDeque,
    io::{ErrorKind, Write},
    net::{TcpListener, TcpStream},
    path::PathBuf,
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc,
    },
    time::{Duration, Instant},
};
use tungstenite::{protocol::WebSocketConfig, Error, Message, WebSocket};

#[derive(Clone)]
pub struct Config {
    pub control: PathBuf,
    pub spool: PathBuf,
    pub port: u16,
    pub fps: u32,
}

impl Config {
    pub fn parse(args: &[String]) -> Result<Self, String> {
        let mut c = Self {
            control: PathBuf::new(),
            spool: PathBuf::new(),
            port: 8101,
            fps: 5,
        };
        let mut a = args.iter();
        while let Some(k) = a.next() {
            let v = a.next().ok_or("missing option value")?;
            match k.as_str() {
                "--control" => c.control = v.into(),
                "--spool" => c.spool = v.into(),
                "--port" => c.port = v.parse().map_err(|_| "invalid port")?,
                "--fps" => c.fps = v.parse().map_err(|_| "invalid fps")?,
                _ => return Err("unknown option".into()),
            }
        }
        if c.control.as_os_str().is_empty()
            || c.spool.as_os_str().is_empty()
            || c.port == 0
            || !(1..=30).contains(&c.fps)
        {
            return Err(
                "usage: openrs-native-host --control SOCKET --spool DIR [--port 8101] [--fps 5]"
                    .into(),
            );
        }
        Ok(c)
    }
}

#[derive(Default, Serialize)]
#[serde(rename_all = "camelCase")]
struct Status {
    connected: bool,
    frame: u32,
    frames: u64,
    sent: u64,
    dropped: u64,
    acks: u64,
    inputs: u64,
    input_seq: u32,
    resources: u64,
    epoch: u32,
    errors: u64,
    error: Option<String>,
}

impl Status {
    fn save(&self, c: &Config) {
        let mut value = serde_json::to_value(self).unwrap_or_default();
        value["pid"] = std::process::id().into();
        if let Ok(bytes) = serde_json::to_vec(&value) {
            let temp = c.spool.join("host.json.tmp");
            if std::fs::write(&temp, bytes).is_ok() {
                let _ = std::fs::rename(temp, c.spool.join("host.json"));
            }
        }
    }

    fn error(&mut self, e: String) {
        self.errors += 1;
        self.error = Some(e);
    }
}

fn idle(e: &Error) -> bool {
    matches!(e, Error::Io(e) if matches!(e.kind(), ErrorKind::WouldBlock | ErrorKind::TimedOut))
}

fn send(ws: &mut WebSocket<TcpStream>, b: Vec<u8>) -> Result<(), String> {
    ws.send(Message::binary(b))
        .map_err(|_| "browser send failed or timed out".into())
}

fn diagnostic(ws: &mut WebSocket<TcpStream>, e: &str) {
    let _ = ws.send(Message::text(serde_json::json!({"error":e}).to_string()));
}

fn input(c: &Config, i: stream::Input) -> Result<(), String> {
    if i.kind > stream::FOCUS
        || !i.a.is_finite()
        || !i.b.is_finite()
        || i.a.abs() > 100000.
        || i.b.abs() > 100000.
        || !(i.code.strip_prefix("Text:").is_some_and(|t| {
            i.kind == stream::KEY_DOWN
                && !t.is_empty()
                && t.len() <= 128
                && !t.chars().any(char::is_control)
        }) || (i.code.len() <= 64 && i.code.bytes().all(|b| b.is_ascii_alphanumeric())))
    {
        return Err("invalid native input".into());
    }
    let mut args = vec![
        i.seq.to_string(),
        i.kind.to_string(),
        i.button.to_string(),
        i.a.to_string(),
        i.b.to_string(),
    ];
    if !i.code.is_empty() {
        args.push(i.code);
    }
    control::command(&c.control, "remote-input", &args).map(drop)
}

fn session(
    socket: TcpStream,
    c: &Config,
    shutdown: &AtomicBool,
    status: &mut Status,
    limits: resources::Limits,
) -> Result<(), String> {
    socket
        .set_read_timeout(Some(Duration::from_secs(2)))
        .map_err(|e| e.to_string())?;
    socket
        .set_write_timeout(Some(Duration::from_secs(2)))
        .map_err(|e| e.to_string())?;
    socket.set_nodelay(true).map_err(|e| e.to_string())?;
    let config = WebSocketConfig::default()
        .max_message_size(Some(1024))
        .max_frame_size(Some(1024))
        .max_write_buffer_size(source::MAX_FRAME * 2);
    let mut ws = tungstenite::accept_hdr_with_config(
        socket,
        |req: &tungstenite::handshake::server::Request, resp| {
            if let Some(origin) = req.headers().get("origin") {
                let valid = origin
                    .to_str()
                    .ok()
                    .and_then(|s| s.parse::<tungstenite::http::Uri>().ok())
                    .is_some_and(|u| matches!(u.host(), Some("127.0.0.1" | "localhost" | "[::1]")));
                if !valid {
                    return Err(tungstenite::http::Response::builder()
                        .status(403)
                        .body(Some("loopback origin required".into()))
                        .unwrap());
                }
            }
            Ok(resp)
        },
        Some(config),
    )
    .map_err(|_| "websocket handshake rejected".to_string())?;
    let baseline = source::latest(&c.spool).ok();
    if let Err(e) = control::command(
        &c.control,
        "stream-start",
        &[c.spool.display().to_string(), c.fps.to_string()],
    ) {
        diagnostic(&mut ws, &e);
        return Err(e);
    }
    status.connected = true;
    let result = pump(&mut ws, c, shutdown, status, baseline, limits);
    if let Err(e) = &result {
        diagnostic(&mut ws, e);
    }
    status.connected = false;
    if let Err(e) = control::command(&c.control, "stream-stop", &[]) {
        status.error(e);
    }
    let _ = ws.close(None);
    result
}

fn pump(
    ws: &mut WebSocket<TcpStream>,
    c: &Config,
    shutdown: &AtomicBool,
    s: &mut Status,
    baseline: Option<source::Latest>,
    limits: resources::Limits,
) -> Result<(), String> {
    ws.get_ref()
        .set_read_timeout(Some(Duration::from_millis(5)))
        .map_err(|e| e.to_string())?;
    let mut resources = resources::Resources::new(limits);
    let mut inflight = VecDeque::new();
    let mut pending = None;
    let mut draining = false;
    let mut last_sent = 0;
    let mut observed = baseline;
    let mut hello = false;
    let mut epochs = false;
    let mut last_peer = Instant::now();
    let mut last_frame = Instant::now();
    let mut ping = Instant::now();
    let mut saved = Instant::now();
    let mut rate = (Instant::now(), 0u32);
    while !shutdown.load(Ordering::Relaxed) {
        for _ in 0..64 {
            match ws.read() {
                Ok(Message::Binary(b)) => {
                    last_peer = Instant::now();
                    match stream::decode_to_host(&b).ok_or("invalid browser message")? {
                        ToHost::Input(i) => {
                            if rate.0.elapsed() >= Duration::from_secs(1) {
                                rate = (Instant::now(), 0);
                            }
                            rate.1 += 1;
                            if rate.1 > 1024 {
                                return Err("input rate limit".into());
                            }
                            match input(c, i) {
                                Ok(()) => s.inputs += 1,
                                Err(e) => {
                                    diagnostic(ws, &e);
                                    s.error(e);
                                    break;
                                }
                            }
                        }
                        ToHost::Ack(n) => {
                            if inflight.contains(&n) {
                                while inflight.front().is_some_and(|f| *f <= n) {
                                    inflight.pop_front();
                                }
                                s.acks += 1;
                            }
                        }
                        ToHost::Pong => {}
                        ToHost::Epochs => {
                            if !hello {
                                return Err("resource epochs accepted before hello".into());
                            }
                            epochs = true;
                        }
                    }
                }
                Ok(Message::Close(_)) => return Ok(()),
                Ok(Message::Ping(_)) | Ok(Message::Pong(_)) => last_peer = Instant::now(),
                Ok(_) => return Err("binary protocol required".into()),
                Err(e) if idle(&e) => break,
                Err(Error::ConnectionClosed | Error::AlreadyClosed) => return Ok(()),
                Err(_) => return Err("browser disconnected".into()),
            }
        }
        if last_peer.elapsed() > Duration::from_secs(15) {
            return Err("browser acknowledgement timeout".into());
        }
        match source::latest(&c.spool) {
            Ok(m) if observed.as_ref() != Some(&m) => {
                last_frame = Instant::now();
                if let Some(old) = &observed {
                    s.dropped += u64::from(m.frame.saturating_sub(old.frame).saturating_sub(1));
                }
                s.frames += 1;
                s.frame = m.frame;
                if pending.replace(m.clone()).is_some() {
                    s.dropped += 1;
                }
                observed = Some(m);
            }
            Err(e) if e != "spool not ready" && s.error.as_ref() != Some(&e) => s.error(e),
            _ => {}
        }
        if last_frame.elapsed() > Duration::from_secs(30) {
            return Err("native frame timeout; inspect stream-state".into());
        }
        if inflight.len() < stream::WINDOW as usize && (!draining || inflight.is_empty()) {
            if let Some(m) = pending.take() {
                match source::load(&c.spool, &m) {
                    Ok(Some(mut f)) => {
                        let Some((r, rotated)) =
                            resources.split_epoch(&mut f, inflight.is_empty())?
                        else {
                            if !epochs {
                                return Err("session resource budget exhausted; reconnect with resource epochs".into());
                            }
                            pending = Some(m);
                            draining = true;
                            continue;
                        };
                        if rotated {
                            if !epochs {
                                return Err("session resource budget exhausted; reconnect with resource epochs".into());
                            }
                            s.epoch = s.epoch.checked_add(1).ok_or("resource epoch overflow")?;
                            send(ws, stream::reset(s.epoch, last_sent))?;
                        }
                        draining = false;
                        if !hello {
                            let h = stream::epoch_hello("native", f.width, f.height, c.fps);
                            send(ws, h)?;
                            hello = true;
                        }
                        if !r.meshes.is_empty() || !r.textures.is_empty() {
                            s.resources += (r.meshes.len() + r.textures.len()) as u64;
                            send(ws, stream::resource(&r))?;
                        }
                        send(
                            ws,
                            stream::frame_message(
                                m.frame,
                                m.input_seq,
                                s.dropped.min(u64::from(u32::MAX)) as u32,
                                &f,
                            ),
                        )?;
                        inflight.push_back(m.frame);
                        last_sent = m.frame;
                        s.sent += 1;
                        s.input_seq = m.input_seq;
                    }
                    Ok(None) => {
                        s.dropped += 1;
                    }
                    Err(e) => {
                        s.dropped += 1;
                        diagnostic(ws, &e);
                        s.error(e);
                    }
                }
            }
        }
        if ping.elapsed() >= Duration::from_secs(1) {
            send(ws, stream::ping(s.frame))?;
            ping = Instant::now();
        }
        if saved.elapsed() >= Duration::from_millis(200) {
            s.save(c);
            saved = Instant::now();
        }
        std::thread::sleep(Duration::from_millis(5));
    }
    Ok(())
}

pub fn serve(listener: TcpListener, c: Config, shutdown: Arc<AtomicBool>) -> Result<(), String> {
    serve_with_limits(listener, c, shutdown, resources::Limits::default())
}

fn serve_with_limits(
    listener: TcpListener,
    c: Config,
    shutdown: Arc<AtomicBool>,
    limits: resources::Limits,
) -> Result<(), String> {
    use fs2::FileExt;
    std::fs::create_dir_all(&c.spool).map_err(|e| e.to_string())?;
    let lock = std::fs::OpenOptions::new()
        .read(true)
        .write(true)
        .create(true)
        .truncate(false)
        .open(c.spool.join("host.lock"))
        .map_err(|e| e.to_string())?;
    lock.try_lock_exclusive()
        .map_err(|_| "native spool already hosted".to_string())?;
    if let Ok(v) = control::command(&c.control, "stream-state", &[]) {
        if v["running"] == true
            && v["directory"].as_str().is_some_and(|d| {
                std::fs::canonicalize(d).ok() == std::fs::canonicalize(&c.spool).ok()
            })
        {
            control::command(&c.control, "stream-stop", &[])?;
        }
    }
    listener.set_nonblocking(true).map_err(|e| e.to_string())?;
    let active = Arc::new(AtomicBool::new(false));
    let mut worker = None;
    Status::default().save(&c);
    while !shutdown.load(Ordering::Relaxed) {
        match listener.accept() {
            Ok((mut socket, _)) => {
                if active.swap(true, Ordering::AcqRel) {
                    let _ = socket.set_write_timeout(Some(Duration::from_millis(100)));
                    let _ = socket.write_all(b"HTTP/1.1 503 Service Unavailable\r\nContent-Length: 25\r\nConnection: close\r\n\r\nnative controller occupied");
                    continue;
                }
                if let Some(w) = worker.take() {
                    let _: Result<(), _> = std::thread::JoinHandle::join(w);
                }
                let (c, shutdown, active) = (c.clone(), shutdown.clone(), active.clone());
                worker = Some(std::thread::spawn(move || {
                    let mut s = Status::default();
                    if let Err(e) = session(socket, &c, &shutdown, &mut s, limits) {
                        s.error(e);
                    }
                    s.save(&c);
                    active.store(false, Ordering::Release);
                }));
            }
            Err(e) if e.kind() == ErrorKind::WouldBlock => {
                std::thread::sleep(Duration::from_millis(10))
            }
            Err(e) => return Err(e.to_string()),
        }
    }
    if let Some(w) = worker {
        let _ = w.join();
    }
    Ok(())
}

#[cfg(test)]
mod tests;
