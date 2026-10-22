use std::{
    io::{self, ErrorKind, Read, Write},
    net::{Ipv4Addr, SocketAddr, TcpListener, TcpStream, ToSocketAddrs},
    sync::{
        Arc,
        atomic::{AtomicBool, AtomicUsize, Ordering},
    },
    thread,
    time::{Duration, Instant},
};
use tungstenite::{Error, HandshakeError, Message, WebSocket, protocol::WebSocketConfig};

const MAX_CONNECTIONS: usize = 8;
const MAX_MESSAGE: usize = 64 * 1024;
const MAX_BYTES: usize = 64 * 1024 * 1024;
const IO_TIMEOUT: Duration = Duration::from_secs(2);
const POLL_TIMEOUT: Duration = Duration::from_millis(5);
const IDLE_TIMEOUT: Duration = Duration::from_secs(30);
const SESSION_TIMEOUT: Duration = Duration::from_secs(120);
const MAX_GAME_LIFETIME: Duration = Duration::from_secs(24 * 60 * 60);
const MAX_GAME_BYTES: usize = 1024 * 1024 * 1024;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct GameLimits {
    pub max_lifetime: Duration,
    pub max_bytes: usize,
}

impl Default for GameLimits {
    fn default() -> Self {
        Self {
            max_lifetime: SESSION_TIMEOUT,
            max_bytes: MAX_BYTES,
        }
    }
}

#[derive(Clone)]
pub struct Config {
    upstream: SocketAddr,
    lobby_upstream: Option<SocketAddr>,
    world_upstream: Option<SocketAddr>,
    game_limits: GameLimits,
    origins: Vec<String>,
    pub port: u16,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Protocol {
    Js5,
    Login,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
struct Target {
    upstream: SocketAddr,
    protocol: Protocol,
    limits: GameLimits,
}

enum Direction {
    Browser,
    Upstream,
}

enum ByteBudget {
    Js5 { browser: usize, upstream: usize },
    Game { remaining: usize },
}

impl ByteBudget {
    fn new(protocol: Protocol, limit: usize) -> Self {
        match protocol {
            Protocol::Js5 => Self::Js5 {
                browser: limit,
                upstream: limit,
            },
            Protocol::Login => Self::Game { remaining: limit },
        }
    }

    fn consume(&mut self, direction: Direction, bytes: usize) -> Result<(), String> {
        let remaining = match (self, direction) {
            (Self::Js5 { browser, .. }, Direction::Browser) => browser,
            (Self::Js5 { upstream, .. }, Direction::Upstream) => upstream,
            (Self::Game { remaining }, _) => remaining,
        };
        *remaining = remaining.checked_sub(bytes).ok_or("relay byte limit")?;
        Ok(())
    }
}

impl Config {
    pub fn new(upstream: SocketAddr, origins: Vec<String>, port: u16) -> Result<Self, String> {
        if upstream.port() == 0 || origins.is_empty() || origins.len() > 8 || port == 0 {
            return Err("upstream, origin and nonzero ports required".into());
        }
        for origin in &origins {
            let uri = origin
                .parse::<tungstenite::http::Uri>()
                .map_err(|_| "invalid origin")?;
            if !matches!(uri.scheme_str(), Some("http" | "https"))
                || !matches!(uri.host(), Some("localhost" | "127.0.0.1" | "[::1]"))
                || uri.query().is_some()
                || uri.path() != "/"
                || origin.ends_with('/')
            {
                return Err("exact loopback HTTP origin required without trailing slash".into());
            }
        }
        Ok(Self {
            upstream,
            lobby_upstream: None,
            world_upstream: None,
            game_limits: GameLimits::default(),
            origins,
            port,
        })
    }

    pub fn with_login_upstreams(
        mut self,
        lobby: Option<SocketAddr>,
        world: Option<SocketAddr>,
    ) -> Result<Self, String> {
        if [lobby, world]
            .into_iter()
            .flatten()
            .any(|addr| addr.port() == 0)
        {
            return Err("nonzero login upstream ports required".into());
        }
        self.lobby_upstream = lobby;
        self.world_upstream = world;
        Ok(self)
    }

    pub fn with_game_limits(mut self, limits: GameLimits) -> Result<Self, String> {
        if limits.max_lifetime < Duration::from_secs(1)
            || limits.max_lifetime > MAX_GAME_LIFETIME
            || limits.max_lifetime.subsec_nanos() != 0
        {
            return Err("game max lifetime must be whole seconds in 1..=86400".into());
        }
        if !(MAX_BYTES..=MAX_GAME_BYTES).contains(&limits.max_bytes) {
            return Err("game max bytes must be in 67108864..=1073741824".into());
        }
        self.game_limits = limits;
        Ok(self)
    }

    fn target(&self, uri: &tungstenite::http::Uri) -> Option<Target> {
        if uri.scheme().is_some() || uri.authority().is_some() || uri.query().is_some() {
            return None;
        }
        let (upstream, protocol) = match uri.path() {
            "/js5" => (self.upstream, Protocol::Js5),
            "/lobby" => (self.lobby_upstream?, Protocol::Login),
            "/world" => (self.world_upstream?, Protocol::Login),
            _ => return None,
        };
        let limits = match protocol {
            Protocol::Js5 => GameLimits::default(),
            Protocol::Login => self.game_limits,
        };
        Some(Target {
            upstream,
            protocol,
            limits,
        })
    }

    pub fn parse(args: &[String]) -> Result<Self, String> {
        let mut upstream = None;
        let mut lobby_upstream = None;
        let mut world_upstream = None;
        let mut origins = Vec::new();
        let mut port = 8110;
        let mut game_max_lifetime = None;
        let mut game_max_bytes = None;
        let mut it = args.iter();
        while let Some(key) = it.next() {
            let value = it.next().ok_or("missing option value")?;
            match key.as_str() {
                "--upstream" | "--lobby-upstream" | "--world-upstream" => {
                    let target = match key.as_str() {
                        "--lobby-upstream" => &mut lobby_upstream,
                        "--world-upstream" => &mut world_upstream,
                        _ => &mut upstream,
                    };
                    if target.is_some() {
                        return Err("duplicate upstream".into());
                    }
                    *target = Some(
                        value
                            .to_socket_addrs()
                            .map_err(|_| "upstream resolution failed")?
                            .next()
                            .ok_or("upstream resolution failed")?,
                    );
                }
                "--origin" => origins.push(value.clone()),
                "--port" => port = value.parse().map_err(|_| "invalid port")?,
                "--game-max-lifetime-secs" => {
                    if game_max_lifetime.is_some() {
                        return Err("duplicate game max lifetime".into());
                    }
                    game_max_lifetime = Some(Duration::from_secs(
                        value.parse().map_err(|_| "invalid game max lifetime")?,
                    ));
                }
                "--game-max-bytes" => {
                    if game_max_bytes.is_some() {
                        return Err("duplicate game max bytes".into());
                    }
                    game_max_bytes = Some(value.parse().map_err(|_| "invalid game max bytes")?);
                }
                _ => return Err("unknown option".into()),
            }
        }
        Self::new(upstream.ok_or("fixed upstream required")?, origins, port)?
            .with_login_upstreams(lobby_upstream, world_upstream)?
            .with_game_limits(GameLimits {
                max_lifetime: game_max_lifetime.unwrap_or(SESSION_TIMEOUT),
                max_bytes: game_max_bytes.unwrap_or(MAX_BYTES),
            })
    }
}

fn timeout(error: &io::Error) -> bool {
    matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::TimedOut)
}

fn idle(error: &Error) -> bool {
    matches!(error, Error::Io(error) if timeout(error))
}

fn rejected(status: u16) -> tungstenite::handshake::server::ErrorResponse {
    tungstenite::http::Response::builder()
        .status(status)
        .body(Some("relay request rejected".into()))
        .unwrap()
}

struct BrowserStream {
    socket: TcpStream,
    shutdown: Arc<AtomicBool>,
    deadline: Instant,
    read_budget: usize,
}

impl BrowserStream {
    fn check(&self) -> io::Result<()> {
        if self.shutdown.load(Ordering::Relaxed) {
            return Err(io::Error::from(ErrorKind::ConnectionAborted));
        }
        if Instant::now() >= self.deadline {
            return Err(io::Error::from(ErrorKind::TimedOut));
        }
        Ok(())
    }

    fn reset_budget(&mut self) {
        self.read_budget = MAX_MESSAGE;
    }
}

impl Read for BrowserStream {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        self.check()?;
        if self.read_budget == 0 {
            return Err(io::Error::from(ErrorKind::WouldBlock));
        }
        let size = buffer.len().min(self.read_budget);
        let read = self.socket.read(&mut buffer[..size])?;
        self.read_budget -= read;
        Ok(read)
    }
}

impl Write for BrowserStream {
    fn write(&mut self, buffer: &[u8]) -> io::Result<usize> {
        self.check()?;
        self.socket.write(buffer)
    }

    fn flush(&mut self) -> io::Result<()> {
        self.check()?;
        self.socket.flush()
    }
}

fn accept(
    socket: TcpStream,
    config: &Config,
    shutdown: Arc<AtomicBool>,
) -> Result<(WebSocket<BrowserStream>, Target), String> {
    socket
        .set_nonblocking(true)
        .map_err(|_| "socket setup failed")?;
    socket
        .set_nodelay(true)
        .map_err(|_| "socket setup failed")?;
    let socket = BrowserStream {
        socket,
        shutdown,
        deadline: Instant::now() + IO_TIMEOUT,
        read_budget: MAX_MESSAGE,
    };
    let mut target = None;
    #[allow(clippy::result_large_err)]
    let validate = |request: &tungstenite::handshake::server::Request, response| {
        let selected = config.target(request.uri()).ok_or_else(|| rejected(404))?;
        let mut origins = request.headers().get_all("origin").iter();
        let origin = origins.next().and_then(|v| v.to_str().ok());
        if origins.next().is_some()
            || !origin.is_some_and(|origin| config.origins.iter().any(|allowed| allowed == origin))
        {
            return Err(rejected(403));
        }
        target = Some(selected);
        Ok(response)
    };
    let mut result = tungstenite::accept_hdr_with_config(
        socket,
        validate,
        Some(
            WebSocketConfig::default()
                .max_message_size(Some(MAX_MESSAGE))
                .max_frame_size(Some(MAX_MESSAGE))
                .write_buffer_size(0)
                .max_write_buffer_size(MAX_MESSAGE * 2),
        ),
    );
    loop {
        match result {
            Ok(ws) => return Ok((ws, target.ok_or("relay route required")?)),
            Err(HandshakeError::Interrupted(mut handshake)) => {
                let stream = handshake.get_mut().get_mut();
                stream.check().map_err(|_| "websocket handshake stopped")?;
                stream.reset_budget();
                thread::sleep(POLL_TIMEOUT);
                result = handshake.handshake();
            }
            Err(HandshakeError::Failure(_)) => return Err("websocket handshake rejected".into()),
        }
    }
}

fn flush_browser(
    ws: &mut WebSocket<BrowserStream>,
    pending: &mut Option<Instant>,
) -> Result<bool, String> {
    if pending.is_some_and(|start| start.elapsed() >= IO_TIMEOUT) {
        return Err("browser write timeout".into());
    }
    match ws.flush() {
        Ok(()) => Ok(pending.take().is_some()),
        Err(error) if idle(&error) => {
            pending.get_or_insert_with(Instant::now);
            Ok(false)
        }
        Err(_) => Err("browser send failed".into()),
    }
}

fn initial(ws: &mut WebSocket<BrowserStream>, protocol: Protocol) -> Result<Vec<u8>, String> {
    ws.get_mut().deadline = Instant::now() + IO_TIMEOUT;
    let mut pending = None;
    loop {
        ws.get_ref()
            .check()
            .map_err(|_| "initial handshake stopped")?;
        flush_browser(ws, &mut pending)?;
        ws.get_mut().reset_budget();
        match ws.read() {
            Ok(Message::Binary(data)) => {
                let valid = match protocol {
                    Protocol::Js5 => data.first() == Some(&15),
                    Protocol::Login => data.as_ref() == [14],
                };
                if !valid {
                    return Err("initial handshake required".into());
                }
                return Ok(data.to_vec());
            }
            Ok(Message::Ping(_) | Message::Pong(_)) => {}
            Err(error) if idle(&error) => thread::sleep(POLL_TIMEOUT),
            _ => return Err("initial handshake required".into()),
        }
    }
}

fn finish(ws: &mut WebSocket<BrowserStream>) {
    let deadline = Instant::now() + IO_TIMEOUT;
    let mut result = ws.close(None);
    while result.as_ref().is_err_and(idle) {
        if Instant::now() >= deadline || ws.get_ref().check().is_err() {
            break;
        }
        thread::sleep(POLL_TIMEOUT);
        result = ws.flush();
    }
}

fn forward(
    ws: &mut WebSocket<BrowserStream>,
    mut upstream: TcpStream,
    first: Vec<u8>,
    target: Target,
) -> Result<(), String> {
    upstream
        .set_nonblocking(true)
        .map_err(|_| "upstream setup failed")?;
    upstream
        .set_nodelay(true)
        .map_err(|_| "upstream setup failed")?;
    let start = Instant::now();
    let deadline = start + target.limits.max_lifetime;
    let mut active = start;
    let mut budget = ByteBudget::new(target.protocol, target.limits.max_bytes);
    budget.consume(Direction::Browser, first.len())?;
    let mut outgoing = first;
    let mut offset = 0;
    let mut upstream_pending = Instant::now();
    let mut browser_pending = None;
    let mut buffer = [0u8; 16 * 1024];
    loop {
        ws.get_mut().deadline = deadline.min(active + IDLE_TIMEOUT);
        ws.get_ref().check().map_err(|_| "relay session stopped")?;
        let mut progress = flush_browser(ws, &mut browser_pending)?;
        if !outgoing.is_empty() {
            if upstream_pending.elapsed() >= IO_TIMEOUT {
                return Err("upstream write timeout".into());
            }
            match upstream.write(&outgoing[offset..]) {
                Ok(0) => return Err("upstream send failed".into()),
                Ok(n) => {
                    offset += n;
                    active = Instant::now();
                    progress = true;
                    if offset == outgoing.len() {
                        outgoing.clear();
                        offset = 0;
                    }
                }
                Err(error) if timeout(&error) => {}
                Err(_) => return Err("upstream send failed".into()),
            }
        }
        if outgoing.is_empty() {
            ws.get_mut().reset_budget();
            match ws.read() {
                Ok(Message::Binary(data)) => {
                    budget.consume(Direction::Browser, data.len())?;
                    outgoing = data.to_vec();
                    upstream_pending = Instant::now();
                    if !outgoing.is_empty() {
                        active = Instant::now();
                    }
                    progress = true;
                }
                Ok(Message::Close(_)) | Err(Error::ConnectionClosed | Error::AlreadyClosed) => {
                    return Ok(());
                }
                Ok(Message::Ping(_) | Message::Pong(_)) => progress = true,
                Ok(_) => return Err("binary messages required".into()),
                Err(error) if idle(&error) => {}
                Err(_) => return Err("browser receive failed".into()),
            }
        }
        progress |= flush_browser(ws, &mut browser_pending)?;
        if browser_pending.is_none() {
            match upstream.read(&mut buffer) {
                Ok(0) => return Ok(()),
                Ok(n) => {
                    budget.consume(Direction::Upstream, n)?;
                    match ws.write(Message::binary(buffer[..n].to_vec())) {
                        Ok(()) => {}
                        Err(error) if idle(&error) => {
                            browser_pending = Some(Instant::now());
                        }
                        Err(_) => return Err("browser send failed".into()),
                    }
                    active = Instant::now();
                    progress = true;
                }
                Err(error) if timeout(&error) => {}
                Err(_) => return Err("upstream receive failed".into()),
            }
        }
        if !progress {
            thread::sleep(POLL_TIMEOUT);
        }
    }
}

fn session(socket: TcpStream, config: &Config, shutdown: Arc<AtomicBool>) -> Result<(), String> {
    let (mut ws, target) = accept(socket, config, shutdown)?;
    let result = (|| {
        let first = initial(&mut ws, target.protocol)?;
        ws.get_ref()
            .check()
            .map_err(|_| "initial handshake stopped")?;
        let upstream = TcpStream::connect_timeout(&target.upstream, IO_TIMEOUT)
            .map_err(|_| "upstream connection failed")?;
        forward(&mut ws, upstream, first, target)
    })();
    finish(&mut ws);
    result
}

struct Slot(Arc<AtomicUsize>);

impl Drop for Slot {
    fn drop(&mut self) {
        self.0.fetch_sub(1, Ordering::AcqRel);
    }
}

pub fn serve(
    listener: TcpListener,
    config: Config,
    shutdown: Arc<AtomicBool>,
) -> Result<(), String> {
    if !listener
        .local_addr()
        .map_err(|_| "listener address unavailable")?
        .ip()
        .is_loopback()
    {
        return Err("loopback listener required".into());
    }
    listener
        .set_nonblocking(true)
        .map_err(|_| "listener setup failed")?;
    let active = Arc::new(AtomicUsize::new(0));
    let mut workers = Vec::new();
    let result = loop {
        if shutdown.load(Ordering::Relaxed) {
            break Ok(());
        }
        let mut pending = Vec::new();
        for worker in workers.drain(..) {
            let worker: thread::JoinHandle<()> = worker;
            if worker.is_finished() {
                let _ = worker.join();
            } else {
                pending.push(worker);
            }
        }
        workers = pending;
        match listener.accept() {
            Ok((mut socket, _)) => {
                if active
                    .fetch_update(Ordering::AcqRel, Ordering::Acquire, |n| {
                        (n < MAX_CONNECTIONS).then_some(n + 1)
                    })
                    .is_err()
                {
                    let _ = socket.set_nonblocking(true);
                    let _ = socket.write_all(b"HTTP/1.1 503 Service Unavailable\r\nConnection: close\r\nContent-Length: 0\r\n\r\n");
                    continue;
                }
                let slot = Slot(active.clone());
                let config = config.clone();
                let shutdown = shutdown.clone();
                workers.push(thread::spawn(move || {
                    let _slot = slot;
                    if let Err(error) = session(socket, &config, shutdown) {
                        eprintln!("{error}");
                    }
                }));
            }
            Err(error) if error.kind() == ErrorKind::WouldBlock => {
                thread::sleep(Duration::from_millis(5))
            }
            Err(_) => break Err("listener failed".into()),
        }
    };
    shutdown.store(true, Ordering::Relaxed);
    for worker in workers {
        let _ = worker.join();
    }
    result
}

pub fn run(config: Config, shutdown: Arc<AtomicBool>) -> Result<(), String> {
    let listener =
        TcpListener::bind((Ipv4Addr::LOCALHOST, config.port)).map_err(|_| "relay bind failed")?;
    println!("JS5 relay on ws://127.0.0.1:{}/js5", config.port);
    for (path, upstream) in [
        ("lobby", config.lobby_upstream),
        ("world", config.world_upstream),
    ] {
        if upstream.is_some() {
            println!("Login relay on ws://127.0.0.1:{}/{path}", config.port);
        }
    }
    serve(listener, config, shutdown)
}

#[cfg(test)]
mod tests;
