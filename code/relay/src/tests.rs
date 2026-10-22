use super::*;
use tungstenite::{
    client::IntoClientRequest,
    http::HeaderValue,
    protocol::{
        Role,
        frame::{
            Frame,
            coding::{Data, OpCode},
        },
    },
};

const ORIGIN: &str = "http://127.0.0.1:8091";

struct Relay {
    address: SocketAddr,
    shutdown: Arc<AtomicBool>,
    worker: Option<thread::JoinHandle<Result<(), String>>>,
}

impl Relay {
    fn start(upstream: SocketAddr) -> Self {
        Self::configured(Config::new(upstream, vec![ORIGIN.into()], 8110).unwrap())
    }

    fn configured(mut config: Config) -> Self {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).unwrap();
        let address = listener.local_addr().unwrap();
        config.port = address.port();
        let shutdown = Arc::new(AtomicBool::new(false));
        let stop = shutdown.clone();
        let worker = thread::spawn(move || serve(listener, config, stop));
        Self {
            address,
            shutdown,
            worker: Some(worker),
        }
    }

    fn connect(&self, path: &str, origin: Option<&str>) -> Result<WebSocket<TcpStream>, Error> {
        let mut request = format!("ws://{}{path}", self.address)
            .into_client_request()
            .unwrap();
        if let Some(origin) = origin {
            request
                .headers_mut()
                .insert("origin", HeaderValue::from_str(origin).unwrap());
        }
        let socket = TcpStream::connect(self.address).unwrap();
        socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
        socket.set_write_timeout(Some(IO_TIMEOUT)).unwrap();
        tungstenite::client(request, socket)
            .map(|(ws, _)| ws)
            .map_err(|error| match error {
                tungstenite::HandshakeError::Failure(error) => error,
                _ => panic!("blocking client interrupted"),
            })
    }
}

impl Drop for Relay {
    fn drop(&mut self) {
        self.shutdown.store(true, Ordering::Relaxed);
        if let Some(worker) = self.worker.take() {
            assert!(worker.join().unwrap().is_ok());
        }
    }
}

fn upstream() -> TcpListener {
    TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).unwrap()
}

fn status(error: Error) -> u16 {
    match error {
        Error::Http(response) => response.status().as_u16(),
        other => panic!("unexpected error {other}"),
    }
}

#[test]
fn requires_explicit_fixed_target_and_exact_loopback_origin() {
    assert!(Config::parse(&[]).is_err());
    let address = "127.0.0.1:43594".parse().unwrap();
    for origin in [
        "null",
        "http://example.com",
        "http://127.0.0.1.evil:8091",
        "http://127.0.0.1:8091/",
        "http://localhost:8091/path",
        "ftp://localhost:8091",
        "http://localhost:8091?x=1",
    ] {
        assert!(
            Config::new(address, vec![origin.into()], 8110).is_err(),
            "{origin}"
        );
    }
    assert!(Config::new(address, vec![ORIGIN.into()], 8110).is_ok());
    assert!(Config::new(address, vec!["https://[::1]:8091".into()], 8110).is_ok());
    assert!(Config::new(address, vec![], 8110).is_err());
}

#[test]
fn rejects_missing_wrong_or_suffix_origins_without_upstream_connection() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    for origin in [
        None,
        Some("null"),
        Some("http://127.0.0.1:8092"),
        Some("http://127.0.0.1:8091.evil"),
    ] {
        assert_eq!(status(relay.connect("/js5", origin).unwrap_err()), 403);
    }
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

#[test]
fn browser_cannot_choose_an_upstream_or_different_route() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    for path in ["/", "/world", "/js5?host=example.com:80", "/js5/"] {
        assert_eq!(status(relay.connect(path, Some(ORIGIN)).unwrap_err()), 404);
    }
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

#[test]
fn passes_binary_bytes_in_order_across_message_and_tcp_boundaries() {
    let upstream = upstream();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let payload: Vec<u8> = (0..100_000).map(|i| (i % 251) as u8).collect();
    let expected = payload.clone();
    let worker = thread::spawn(move || {
        let (mut socket, _) = upstream.accept().unwrap();
        socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
        let mut header = [0; 4];
        socket.read_exact(&mut header).unwrap();
        assert_eq!(header, [15, 1, 2, 3]);
        let mut bytes = vec![0; expected.len()];
        socket.read_exact(&mut bytes).unwrap();
        assert_eq!(bytes, expected);
        for chunk in bytes.chunks(777) {
            socket.write_all(chunk).unwrap();
        }
    });
    let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    ws.send(Message::binary(vec![15, 1, 2, 3])).unwrap();
    for chunk in payload.chunks(20_003) {
        ws.send(Message::binary(chunk.to_vec())).unwrap();
    }
    let mut received = Vec::new();
    while received.len() < payload.len() {
        match ws.read().unwrap() {
            Message::Binary(data) => received.extend_from_slice(&data),
            other => panic!("unexpected message {other:?}"),
        }
    }
    assert_eq!(received, payload);
    worker.join().unwrap();
}

#[test]
fn non_js5_and_text_handshakes_never_dial_upstream() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    for message in [
        Message::binary(vec![14]),
        Message::binary(Vec::new()),
        Message::text("15"),
    ] {
        let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
        ws.send(message).unwrap();
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
    }
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

#[test]
fn oversized_initial_message_never_dials_upstream() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    let mut data = vec![0; MAX_MESSAGE + 1];
    data[0] = 15;
    let _ = ws.send(Message::binary(data));
    assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

#[test]
fn connection_limit_is_bounded_and_closed_slots_are_reusable() {
    let upstream = upstream();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let mut clients: Vec<_> = (0..MAX_CONNECTIONS)
        .map(|_| relay.connect("/js5", Some(ORIGIN)).unwrap())
        .collect();
    assert_eq!(
        status(relay.connect("/js5", Some(ORIGIN)).unwrap_err()),
        503
    );
    clients.pop();
    let start = Instant::now();
    loop {
        match relay.connect("/js5", Some(ORIGIN)) {
            Ok(_) => break,
            Err(error) => assert_eq!(status(error), 503),
        }
        assert!(start.elapsed() < Duration::from_secs(1));
        thread::sleep(Duration::from_millis(10));
    }
}

fn fragment(ws: &mut WebSocket<TcpStream>, data: Vec<u8>, kind: Data, final_frame: bool) {
    ws.send(Message::Frame(Frame::message(
        data,
        OpCode::Data(kind),
        final_frame,
    )))
    .unwrap();
}

fn drip(mut socket: TcpStream, bytes: Vec<u8>) -> (Arc<AtomicBool>, thread::JoinHandle<usize>) {
    let stop = Arc::new(AtomicBool::new(false));
    let stopped = stop.clone();
    let worker = thread::spawn(move || {
        socket.set_write_timeout(Some(IO_TIMEOUT)).unwrap();
        let mut count = 0;
        while !stopped.load(Ordering::Relaxed) {
            if socket.write_all(&bytes).is_err() {
                break;
            }
            count += 1;
            thread::sleep(Duration::from_millis(25));
        }
        count
    });
    (stop, worker)
}

fn wait_closed(socket: &mut TcpStream, limit: Duration) {
    socket
        .set_read_timeout(Some(Duration::from_millis(50)))
        .unwrap();
    let start = Instant::now();
    let mut buffer = [0; 256];
    loop {
        match socket.read(&mut buffer) {
            Ok(0) => return,
            Ok(_) => {}
            Err(error) if timeout(&error) => {}
            Err(error)
                if matches!(
                    error.kind(),
                    ErrorKind::ConnectionReset | ErrorKind::ConnectionAborted
                ) =>
            {
                return;
            }
            Err(error) => panic!("unexpected read failure: {error}"),
        }
        assert!(start.elapsed() < limit, "relay did not close connection");
    }
}

#[test]
fn http_handshake_deadline_is_absolute_despite_drip_reads() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let mut socket = TcpStream::connect(relay.address).unwrap();
    socket
        .write_all(b"GET /js5 HTTP/1.1\r\nHost: localhost\r\nX-Drip: ")
        .unwrap();
    let (stop, worker) = drip(socket.try_clone().unwrap(), vec![b'x']);
    let start = Instant::now();
    wait_closed(&mut socket, IO_TIMEOUT + Duration::from_secs(1));
    stop.store(true, Ordering::Relaxed);
    assert!(worker.join().unwrap() >= 10);
    assert!(start.elapsed() < IO_TIMEOUT + Duration::from_secs(1));
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
    relay.connect("/js5", Some(ORIGIN)).unwrap();
}

#[test]
fn fragmented_initial_deadlines_release_all_slots_despite_drip_reads() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let mut clients = Vec::new();
    let mut writers = Vec::new();
    for _ in 0..MAX_CONNECTIONS {
        let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![15], Data::Binary, false);
        writers.push(drip(
            ws.get_ref().try_clone().unwrap(),
            vec![0, 128, 0, 0, 0, 0],
        ));
        clients.push(ws);
    }
    assert_eq!(
        status(relay.connect("/js5", Some(ORIGIN)).unwrap_err()),
        503
    );
    let start = Instant::now();
    for ws in &mut clients {
        wait_closed(ws.get_mut(), IO_TIMEOUT + Duration::from_secs(1));
    }
    for (stop, worker) in writers {
        stop.store(true, Ordering::Relaxed);
        assert!(worker.join().unwrap() >= 10);
    }
    assert!(start.elapsed() < IO_TIMEOUT + Duration::from_secs(1));
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
    let replacements: Vec<_> = (0..MAX_CONNECTIONS)
        .map(|_| relay.connect("/js5", Some(ORIGIN)).unwrap())
        .collect();
    assert_eq!(replacements.len(), MAX_CONNECTIONS);
}

#[test]
fn initial_fragmentation_handles_control_frames_without_early_dial() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    fragment(&mut ws, vec![15], Data::Binary, false);
    ws.send(Message::Ping(vec![7, 8, 9].into())).unwrap();
    assert_eq!(ws.read().unwrap(), Message::Pong(vec![7, 8, 9].into()));
    ws.send(Message::Pong(vec![6].into())).unwrap();
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
    upstream.set_nonblocking(false).unwrap();
    let worker = thread::spawn(move || {
        let (mut socket, _) = upstream.accept().unwrap();
        socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
        let mut bytes = [0; 4];
        socket.read_exact(&mut bytes).unwrap();
        assert_eq!(bytes, [15, 1, 2, 3]);
        socket.write_all(&[42]).unwrap();
    });
    fragment(&mut ws, vec![1, 2, 3], Data::Continue, true);
    assert_eq!(ws.read().unwrap(), Message::binary(vec![42]));
    worker.join().unwrap();
}

#[test]
fn shutdown_interrupts_http_and_fragmented_initial_reads_and_joins_workers() {
    let upstream = upstream();
    upstream.set_nonblocking(true).unwrap();
    let mut relay = Relay::start(upstream.local_addr().unwrap());
    let mut sockets = Vec::new();
    for _ in 0..MAX_CONNECTIONS / 2 {
        let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![15], Data::Binary, false);
        sockets.push(ws.get_ref().try_clone().unwrap());
    }
    for _ in 0..MAX_CONNECTIONS / 2 {
        let mut socket = TcpStream::connect(relay.address).unwrap();
        socket
            .write_all(b"GET /js5 HTTP/1.1\r\nHost: localhost\r\nX: ")
            .unwrap();
        sockets.push(socket);
    }
    assert_eq!(
        status(relay.connect("/js5", Some(ORIGIN)).unwrap_err()),
        503
    );
    let start = Instant::now();
    relay.shutdown.store(true, Ordering::Relaxed);
    assert!(relay.worker.take().unwrap().join().unwrap().is_ok());
    assert!(start.elapsed() < Duration::from_millis(500));
    for socket in &mut sockets {
        wait_closed(socket, Duration::from_millis(100));
    }
    assert_eq!(upstream.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

#[test]
fn shutdown_closes_established_upstream_and_fragmented_browser_connections() {
    let upstream = upstream();
    let mut relay = Relay::start(upstream.local_addr().unwrap());
    let worker = thread::spawn(move || {
        let (mut socket, _) = upstream.accept().unwrap();
        socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
        let mut first = [0];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [15]);
        socket.write_all(&[99]).unwrap();
        assert_eq!(socket.read(&mut first).unwrap(), 0);
    });
    let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    ws.send(Message::binary(vec![15])).unwrap();
    assert_eq!(ws.read().unwrap(), Message::binary(vec![99]));
    fragment(&mut ws, vec![1], Data::Binary, false);
    let start = Instant::now();
    relay.shutdown.store(true, Ordering::Relaxed);
    assert!(relay.worker.take().unwrap().join().unwrap().is_ok());
    worker.join().unwrap();
    assert!(start.elapsed() < Duration::from_millis(500));
    wait_closed(ws.get_mut(), Duration::from_millis(100));
}

#[test]
fn both_directions_progress_when_upstream_replies_before_reading_more_requests() {
    const REQUEST: usize = 8 * 1024 * 1024;
    const RESPONSE: usize = 16 * 1024 * 1024;
    let upstream = upstream();
    let relay = Relay::start(upstream.local_addr().unwrap());
    let worker = thread::spawn(move || {
        let (mut socket, _) = upstream.accept().unwrap();
        let options = socket2::SockRef::from(&socket);
        options.set_send_buffer_size(64 * 1024).unwrap();
        options.set_recv_buffer_size(64 * 1024).unwrap();
        socket
            .set_read_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        socket
            .set_write_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        let mut first = [0; 4];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [15, 1, 2, 3]);
        socket.write_all(&vec![0x5a; RESPONSE]).unwrap();
        let mut requests = vec![0; REQUEST];
        socket.read_exact(&mut requests).unwrap();
        assert!(requests.iter().all(|byte| *byte == 0xa5));
        socket.write_all(b"done").unwrap();
    });
    let mut ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    ws.send(Message::binary(vec![15, 1, 2, 3])).unwrap();
    let writer_socket = ws.get_ref().try_clone().unwrap();
    let writer = thread::spawn(move || {
        writer_socket
            .set_write_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        let mut sender = WebSocket::from_raw_socket(writer_socket, Role::Client, None);
        for _ in 0..REQUEST / MAX_MESSAGE {
            sender
                .send(Message::binary(vec![0xa5; MAX_MESSAGE]))
                .unwrap();
        }
    });
    let mut response = Vec::new();
    while response.len() < RESPONSE + 4 {
        match ws.read().unwrap() {
            Message::Binary(data) => response.extend_from_slice(&data),
            other => panic!("unexpected message: {other:?}"),
        }
    }
    assert_eq!(response.len(), RESPONSE + 4);
    assert!(response[..RESPONSE].iter().all(|byte| *byte == 0x5a));
    assert_eq!(&response[RESPONSE..], b"done");
    writer.join().unwrap();
    worker.join().unwrap();
}

fn login_relay(address: SocketAddr) -> Relay {
    game_relay(address, GameLimits::default())
}

fn game_relay(address: SocketAddr, limits: GameLimits) -> Relay {
    Relay::configured(
        Config::new(address, vec![ORIGIN.into()], 8110)
            .unwrap()
            .with_login_upstreams(Some(address), Some(address))
            .unwrap()
            .with_game_limits(limits)
            .unwrap(),
    )
}

fn extended_game_relay(address: SocketAddr) -> Relay {
    game_relay(
        address,
        GameLimits {
            max_lifetime: MAX_GAME_LIFETIME,
            max_bytes: MAX_GAME_BYTES,
        },
    )
}

fn no_dial(listener: &TcpListener) {
    listener.set_nonblocking(true).unwrap();
    assert_eq!(listener.accept().unwrap_err().kind(), ErrorKind::WouldBlock);
}

fn accepted(listener: &TcpListener) -> TcpStream {
    listener.set_nonblocking(true).unwrap();
    let start = Instant::now();
    loop {
        match listener.accept() {
            Ok((socket, _)) => {
                socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
                socket.set_write_timeout(Some(IO_TIMEOUT)).unwrap();
                return socket;
            }
            Err(error) if error.kind() == ErrorKind::WouldBlock => {}
            Err(error) => panic!("unexpected accept failure: {error}"),
        }
        assert!(start.elapsed() < IO_TIMEOUT, "upstream was not connected");
        thread::sleep(POLL_TIMEOUT);
    }
}

fn raw_status(relay: &Relay, target: &str, origins: &[&str]) -> u16 {
    use std::io::{BufRead, BufReader};

    let mut socket = TcpStream::connect(relay.address).unwrap();
    socket.set_read_timeout(Some(IO_TIMEOUT)).unwrap();
    socket.set_write_timeout(Some(IO_TIMEOUT)).unwrap();
    write!(socket, "GET {target} HTTP/1.1\r\nHost: unconfigured.invalid\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n").unwrap();
    for origin in origins {
        write!(socket, "Origin: {origin}\r\n").unwrap();
    }
    socket.write_all(b"\r\n").unwrap();
    let mut line = String::new();
    BufReader::new(socket).read_line(&mut line).unwrap();
    line.split_whitespace().nth(1).unwrap().parse().unwrap()
}

#[test]
fn login_configuration_is_optional_fixed_and_resolved_at_parse_time() {
    let base = ["--upstream", "127.0.0.1:43594", "--origin", ORIGIN];
    let parse = |extra: &[&str]| {
        Config::parse(
            &base
                .iter()
                .chain(extra)
                .map(|value| (*value).to_string())
                .collect::<Vec<_>>(),
        )
    };
    let default = parse(&[]).unwrap();
    assert_eq!(default.port, 8110);
    assert_eq!(default.upstream, "127.0.0.1:43594".parse().unwrap());
    assert_eq!(default.lobby_upstream, None);
    assert_eq!(default.world_upstream, None);
    let lobby = parse(&["--lobby-upstream", "localhost:43595"]).unwrap();
    assert!(lobby.lobby_upstream.unwrap().ip().is_loopback());
    assert_eq!(lobby.lobby_upstream.unwrap().port(), 43595);
    assert_eq!(lobby.clone().lobby_upstream, lobby.lobby_upstream);
    assert_eq!(lobby.world_upstream, None);
    let world = parse(&["--world-upstream", "127.0.0.1:43596"]).unwrap();
    assert_eq!(world.lobby_upstream, None);
    assert_eq!(
        world.world_upstream,
        Some("127.0.0.1:43596".parse().unwrap())
    );
    for flag in ["--upstream", "--lobby-upstream", "--world-upstream"] {
        assert!(parse(&[flag]).is_err());
        assert!(parse(&[flag, "127.0.0.1:0"]).is_err());
        assert!(parse(&[flag, "not-an-endpoint"]).is_err());
        assert!(parse(&[flag, "127.0.0.1:43595", flag, "127.0.0.1:43596"]).is_err());
    }
    assert!(parse(&["--host", "127.0.0.1:43595"]).is_err());
    assert!(
        Config::parse(
            &["--lobby-upstream", "127.0.0.1:43595", "--origin", ORIGIN].map(String::from)
        )
        .is_err()
    );
    let zero = Some("127.0.0.1:0".parse().unwrap());
    assert!(default.clone().with_login_upstreams(zero, None).is_err());
    assert!(default.with_login_upstreams(None, zero).is_err());
}

#[test]
fn game_limits_are_bounded_and_apply_only_to_enabled_login_routes() {
    let address = "127.0.0.1:43594".parse().unwrap();
    let base = Config::new(address, vec![ORIGIN.into()], 8110).unwrap();
    let default = GameLimits {
        max_lifetime: Duration::from_secs(120),
        max_bytes: 64 * 1024 * 1024,
    };
    assert_eq!(GameLimits::default(), default);
    assert_eq!(base.game_limits, default);
    for limits in [
        default,
        GameLimits {
            max_lifetime: Duration::from_secs(1),
            max_bytes: MAX_BYTES,
        },
        GameLimits {
            max_lifetime: Duration::from_secs(3600),
            max_bytes: 128 * 1024 * 1024,
        },
        GameLimits {
            max_lifetime: Duration::from_secs(86400),
            max_bytes: 1024 * 1024 * 1024,
        },
    ] {
        let disabled = base.clone().with_game_limits(limits).unwrap();
        for path in ["/lobby", "/world"] {
            assert!(disabled.target(&path.parse().unwrap()).is_none());
        }
        for configured in [
            disabled
                .with_login_upstreams(Some(address), Some(address))
                .unwrap(),
            base.clone()
                .with_login_upstreams(Some(address), Some(address))
                .unwrap()
                .with_game_limits(limits)
                .unwrap(),
        ] {
            for config in [configured.clone(), configured] {
                for (path, protocol, expected) in [
                    ("/js5", Protocol::Js5, default),
                    ("/lobby", Protocol::Login, limits),
                    ("/world", Protocol::Login, limits),
                ] {
                    assert_eq!(
                        config.target(&path.parse().unwrap()),
                        Some(Target {
                            upstream: address,
                            protocol,
                            limits: expected,
                        })
                    );
                }
            }
        }
    }
    for max_lifetime in [
        Duration::ZERO,
        Duration::from_millis(999),
        Duration::from_millis(1500),
        Duration::from_secs(86401),
        Duration::MAX,
    ] {
        assert!(
            base.clone()
                .with_game_limits(GameLimits {
                    max_lifetime,
                    ..default
                })
                .is_err()
        );
    }
    for max_bytes in [0, MAX_BYTES - 1, MAX_GAME_BYTES + 1, usize::MAX] {
        assert!(
            base.clone()
                .with_game_limits(GameLimits {
                    max_bytes,
                    ..default
                })
                .is_err()
        );
    }
}

#[test]
fn game_limit_arguments_validate_units_ranges_duplicates_and_defaults() {
    let base = ["--upstream", "127.0.0.1:43594", "--origin", ORIGIN];
    let parse = |extra: &[&str]| {
        Config::parse(
            &base
                .iter()
                .chain(extra)
                .map(|value| (*value).to_string())
                .collect::<Vec<_>>(),
        )
    };
    assert_eq!(parse(&[]).unwrap().game_limits, GameLimits::default());
    for (extra, expected) in [
        (
            vec!["--game-max-lifetime-secs", "1"],
            GameLimits {
                max_lifetime: Duration::from_secs(1),
                ..GameLimits::default()
            },
        ),
        (
            vec!["--game-max-bytes", "1073741824"],
            GameLimits {
                max_bytes: MAX_GAME_BYTES,
                ..GameLimits::default()
            },
        ),
        (
            vec![
                "--game-max-lifetime-secs",
                "86400",
                "--game-max-bytes",
                "67108864",
                "--world-upstream",
                "127.0.0.1:43596",
            ],
            GameLimits {
                max_lifetime: MAX_GAME_LIFETIME,
                max_bytes: MAX_BYTES,
            },
        ),
    ] {
        let config = parse(&extra).unwrap();
        assert_eq!(config.game_limits, expected);
        assert_eq!(
            config.target(&"/js5".parse().unwrap()).unwrap().limits,
            GameLimits::default()
        );
        assert!(config.target(&"/lobby".parse().unwrap()).is_none());
        if let Some(target) = config.target(&"/world".parse().unwrap()) {
            assert_eq!(target.protocol, Protocol::Login);
            assert_eq!(target.limits, expected);
        }
    }
    for flag in ["--game-max-lifetime-secs", "--game-max-bytes"] {
        assert!(parse(&[flag]).is_err());
        for value in [
            "",
            "-1",
            "0",
            "1.5",
            "infinite",
            "none",
            "18446744073709551616",
        ] {
            assert!(parse(&[flag, value]).is_err(), "{flag} {value}");
        }
        let value = if flag == "--game-max-bytes" {
            "67108864"
        } else {
            "120"
        };
        assert!(parse(&[flag, value, flag, value]).is_err());
    }
    for (flag, value) in [
        ("--game-max-lifetime-secs", "86401"),
        ("--game-max-lifetime-secs", "18446744073709551615"),
        ("--game-max-bytes", "67108863"),
        ("--game-max-bytes", "1073741825"),
        ("--game-max-bytes", "18446744073709551615"),
        ("--game-unlimited", "true"),
        ("--game-idle-timeout", "0"),
    ] {
        assert!(parse(&[flag, value]).is_err(), "{flag} {value}");
    }
}

#[test]
fn game_budgets_are_cumulative_while_js5_budgets_remain_independent() {
    for limit in [MAX_BYTES, MAX_GAME_BYTES] {
        let mut game = ByteBudget::new(Protocol::Login, limit);
        assert!(game.consume(Direction::Browser, usize::MAX).is_err());
        game.consume(Direction::Browser, 1).unwrap();
        game.consume(Direction::Upstream, limit - 2).unwrap();
        assert!(game.consume(Direction::Upstream, 2).is_err());
        game.consume(Direction::Browser, 1).unwrap();
        game.consume(Direction::Browser, 0).unwrap();
        game.consume(Direction::Upstream, 0).unwrap();
        assert!(game.consume(Direction::Browser, 1).is_err());
        assert!(game.consume(Direction::Upstream, 1).is_err());
    }
    let mut js5 = ByteBudget::new(Protocol::Js5, MAX_BYTES);
    js5.consume(Direction::Browser, MAX_BYTES).unwrap();
    assert!(js5.consume(Direction::Browser, 1).is_err());
    js5.consume(Direction::Upstream, MAX_BYTES).unwrap();
    assert!(js5.consume(Direction::Upstream, 1).is_err());
}

#[test]
fn login_routes_are_enabled_individually_and_never_fall_back_to_js5() {
    let upstream = upstream();
    let address = upstream.local_addr().unwrap();
    for (lobby, world) in [(None, None), (Some(address), None), (None, Some(address))] {
        let relay = Relay::configured(
            Config::new(address, vec![ORIGIN.into()], 8110)
                .unwrap()
                .with_login_upstreams(lobby, world)
                .unwrap(),
        );
        for (path, enabled) in [("/lobby", lobby.is_some()), ("/world", world.is_some())] {
            if enabled {
                let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
                ws.send(Message::binary(vec![15])).unwrap();
                assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
            } else {
                assert_eq!(status(relay.connect(path, Some(ORIGIN)).unwrap_err()), 404);
            }
        }
        no_dial(&upstream);
    }
}

#[test]
fn exact_routes_stream_only_to_their_distinct_configured_tcp_endpoints() {
    let js5 = upstream();
    let lobby = upstream();
    let world = upstream();
    let decoy = upstream();
    let args = [
        "--upstream".into(),
        js5.local_addr().unwrap().to_string(),
        "--lobby-upstream".into(),
        lobby.local_addr().unwrap().to_string(),
        "--world-upstream".into(),
        world.local_addr().unwrap().to_string(),
        "--origin".into(),
        ORIGIN.into(),
    ];
    let relay = Relay::configured(Config::parse(&args).unwrap());
    for (path, target, first) in [
        ("/js5", &js5, vec![15, 1, 2, 3]),
        ("/lobby", &lobby, vec![14]),
        ("/world", &world, vec![14]),
    ] {
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(first.clone())).unwrap();
        let mut socket = accepted(target);
        let mut header = vec![0; first.len()];
        socket.read_exact(&mut header).unwrap();
        assert_eq!(header, first);
        for listener in [&js5, &lobby, &world, &decoy] {
            no_dial(listener);
        }
        let mut payload = format!(
            "tcp://{}/world?upstream={}",
            decoy.local_addr().unwrap(),
            decoy.local_addr().unwrap()
        )
        .into_bytes();
        payload.extend((0..100_000).map(|i| (i % 251) as u8));
        let expected = payload.clone();
        let worker = thread::spawn(move || {
            let mut received = vec![0; expected.len()];
            socket.read_exact(&mut received).unwrap();
            assert_eq!(received, expected);
            for chunk in received.chunks(777) {
                socket.write_all(chunk).unwrap();
            }
        });
        for chunk in payload.chunks(20_003) {
            ws.send(Message::binary(chunk.to_vec())).unwrap();
        }
        let mut received = Vec::new();
        while received.len() < payload.len() {
            match ws.read().unwrap() {
                Message::Binary(data) => received.extend_from_slice(&data),
                other => panic!("unexpected message: {other:?}"),
            }
        }
        assert_eq!(received, payload);
        worker.join().unwrap();
        no_dial(&decoy);
    }
}

#[test]
fn configured_routes_reject_queries_absolute_targets_and_path_confusion_before_dial() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    for path in ["js5", "lobby", "world"] {
        for target in [
            format!("/{path}?"),
            format!("/{path}?upstream=127.0.0.1:1"),
            format!("/{path}/"),
            format!("/{path}/../world"),
            format!("/%{path}"),
            format!("//{path}"),
            format!("http://127.0.0.1:1/{path}"),
            format!("ws://unconfigured.invalid/{path}"),
            format!("//127.0.0.1:1/{path}"),
        ] {
            assert_eq!(raw_status(&relay, &target, &[ORIGIN]), 404, "{target}");
        }
    }
    for target in [
        "/",
        "/proxy",
        "/WORLD",
        "/%77orld",
        "/lobby%3fhost=127.0.0.1:1",
    ] {
        assert_eq!(raw_status(&relay, target, &[ORIGIN]), 404, "{target}");
    }
    no_dial(&upstream);
}

#[test]
fn login_routes_enforce_single_exact_origin_before_dial() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    for path in ["/lobby", "/world"] {
        for origin in [
            None,
            Some("null"),
            Some("http://127.0.0.1:8092"),
            Some("http://127.0.0.1:8091.evil"),
            Some("http://127.0.0.1:8091/"),
        ] {
            assert_eq!(status(relay.connect(path, origin).unwrap_err()), 403);
        }
        for origins in [
            &[ORIGIN, ORIGIN][..],
            &[ORIGIN, "null"][..],
            &["null", ORIGIN][..],
        ] {
            assert_eq!(raw_status(&relay, path, origins), 403);
        }
    }
    no_dial(&upstream);
}

#[test]
fn login_initial_message_must_be_binary_and_exactly_fourteen_before_dial() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    for path in ["/lobby", "/world"] {
        for message in [
            Message::binary(Vec::new()),
            Message::binary(vec![15]),
            Message::binary(vec![16]),
            Message::binary(vec![19]),
            Message::binary(vec![29]),
            Message::binary(vec![0, 14]),
            Message::binary(vec![14, 0]),
            Message::binary(vec![14, 14]),
            Message::text("14"),
            Message::Close(None),
        ] {
            let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
            ws.send(message).unwrap();
            assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        }
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![14], Data::Binary, false);
        fragment(&mut ws, vec![0], Data::Continue, true);
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
    }
    no_dial(&upstream);
}

#[test]
fn fragmented_login_initial_waits_for_completion_and_handles_control_frames() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    for path in ["/lobby", "/world"] {
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![14], Data::Binary, false);
        ws.send(Message::Ping(vec![7, 8, 9].into())).unwrap();
        assert_eq!(ws.read().unwrap(), Message::Pong(vec![7, 8, 9].into()));
        ws.send(Message::Pong(vec![6].into())).unwrap();
        no_dial(&upstream);
        fragment(&mut ws, Vec::new(), Data::Continue, true);
        let mut socket = accepted(&upstream);
        let mut first = [0];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [14]);
        socket.write_all(&[0x42]).unwrap();
        assert_eq!(ws.read().unwrap(), Message::binary(vec![0x42]));
        ws.send(Message::text("not binary")).unwrap();
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        assert_eq!(socket.read(&mut first).unwrap(), 0);
    }
}

#[test]
fn login_initial_deadlines_are_absolute_for_silent_and_dripping_clients() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    let mut clients = Vec::new();
    let mut writers = Vec::new();
    for path in ["/lobby", "/world"] {
        clients.push(relay.connect(path, Some(ORIGIN)).unwrap());
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![14], Data::Binary, false);
        writers.push(drip(
            ws.get_ref().try_clone().unwrap(),
            vec![0, 128, 0, 0, 0, 0],
        ));
        clients.push(ws);
    }
    let start = Instant::now();
    for ws in &mut clients {
        wait_closed(ws.get_mut(), IO_TIMEOUT + Duration::from_secs(1));
    }
    for (stop, worker) in writers {
        stop.store(true, Ordering::Relaxed);
        assert!(worker.join().unwrap() >= 10);
    }
    assert!(start.elapsed() < IO_TIMEOUT + Duration::from_secs(1));
    no_dial(&upstream);
    relay.connect("/lobby", Some(ORIGIN)).unwrap();
    relay.connect("/world", Some(ORIGIN)).unwrap();
}

#[test]
fn login_routes_share_session_slots_and_shutdown_with_js5() {
    let upstream = upstream();
    let mut relay = login_relay(upstream.local_addr().unwrap());
    let paths = ["/js5", "/lobby", "/world"];
    let mut clients: Vec<_> = (0..MAX_CONNECTIONS)
        .map(|i| relay.connect(paths[i % paths.len()], Some(ORIGIN)).unwrap())
        .collect();
    for path in paths {
        assert_eq!(status(relay.connect(path, Some(ORIGIN)).unwrap_err()), 503);
    }
    let mut peers = Vec::new();
    for ws in &mut clients[1..3] {
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        let mut first = [0];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [14]);
        socket.write_all(&[0x42]).unwrap();
        assert_eq!(ws.read().unwrap(), Message::binary(vec![0x42]));
        fragment(ws, vec![0x55], Data::Binary, false);
        peers.push(socket);
    }
    let start = Instant::now();
    relay.shutdown.store(true, Ordering::Relaxed);
    assert!(relay.worker.take().unwrap().join().unwrap().is_ok());
    assert!(start.elapsed() < Duration::from_millis(500));
    for ws in &mut clients {
        wait_closed(ws.get_mut(), Duration::from_millis(100));
    }
    for socket in &mut peers {
        assert_eq!(socket.read(&mut [0]).unwrap(), 0);
    }
    no_dial(&upstream);
}

#[test]
fn login_message_limits_apply_before_and_after_the_initial_handshake() {
    let upstream = upstream();
    let relay = extended_game_relay(upstream.local_addr().unwrap());
    for path in ["/lobby", "/world"] {
        let mut oversized = vec![0x55; MAX_MESSAGE + 1];
        oversized[0] = 14;
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        let _ = ws.send(Message::binary(oversized));
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        no_dial(&upstream);
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        fragment(&mut ws, vec![14; MAX_MESSAGE], Data::Binary, false);
        fragment(&mut ws, vec![14], Data::Continue, true);
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        no_dial(&upstream);
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        let worker = thread::spawn(move || {
            let mut data = vec![0; MAX_MESSAGE + 1];
            socket.read_exact(&mut data).unwrap();
            assert_eq!(data[0], 14);
            assert!(data[1..].iter().all(|byte| *byte == 0x55));
            socket.write_all(&[0x42]).unwrap();
            assert_eq!(socket.read(&mut [0]).unwrap(), 0);
        });
        ws.send(Message::binary(vec![0x55; MAX_MESSAGE])).unwrap();
        assert_eq!(ws.read().unwrap(), Message::binary(vec![0x42]));
        let _ = ws.send(Message::binary(vec![0x55; MAX_MESSAGE + 1]));
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        worker.join().unwrap();
    }
}

#[test]
fn configured_game_lifetime_closes_both_sockets_despite_activity_but_not_js5() {
    let js5 = upstream();
    let game = upstream();
    let args = [
        "--upstream".into(),
        js5.local_addr().unwrap().to_string(),
        "--lobby-upstream".into(),
        game.local_addr().unwrap().to_string(),
        "--world-upstream".into(),
        game.local_addr().unwrap().to_string(),
        "--origin".into(),
        ORIGIN.into(),
        "--game-max-lifetime-secs".into(),
        "1".into(),
        "--game-max-bytes".into(),
        "1073741824".into(),
    ];
    let relay = Relay::configured(Config::parse(&args).unwrap());
    let mut js5_ws = relay.connect("/js5", Some(ORIGIN)).unwrap();
    js5_ws.send(Message::binary(vec![15])).unwrap();
    let mut js5_socket = accepted(&js5);
    let mut first = [0];
    js5_socket.read_exact(&mut first).unwrap();
    assert_eq!(first, [15]);
    let mut clients = Vec::new();
    for path in ["/lobby", "/world"] {
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let start = Instant::now();
        let mut socket = accepted(&game);
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [14]);
        socket.write_all(&[0x42]).unwrap();
        assert_eq!(ws.read().unwrap(), Message::binary(vec![0x42]));
        fragment(&mut ws, vec![0x55], Data::Binary, false);
        let browser_writer = drip(ws.get_ref().try_clone().unwrap(), vec![0, 128, 0, 0, 0, 0]);
        let upstream_writer = drip(socket.try_clone().unwrap(), vec![0x5a]);
        clients.push((ws, socket, start, browser_writer, upstream_writer));
    }
    for (mut ws, mut socket, start, browser_writer, upstream_writer) in clients {
        wait_closed(ws.get_mut(), Duration::from_secs(2));
        wait_closed(&mut socket, Duration::from_millis(500));
        for (stop, worker) in [browser_writer, upstream_writer] {
            stop.store(true, Ordering::Relaxed);
            assert!(worker.join().unwrap() >= 10);
        }
        assert!(start.elapsed() < Duration::from_secs(2));
    }
    js5_ws.send(Message::binary(vec![0x66])).unwrap();
    js5_socket.read_exact(&mut first).unwrap();
    assert_eq!(first, [0x66]);
    js5_socket.write_all(&[0x77]).unwrap();
    assert_eq!(js5_ws.read().unwrap(), Message::binary(vec![0x77]));
}

#[test]
fn game_browser_byte_budget_includes_the_initial_handshake_and_honors_opt_in() {
    for (path, max_bytes) in [("/lobby", MAX_BYTES), ("/world", MAX_BYTES + MAX_MESSAGE)] {
        let upstream = upstream();
        let relay = game_relay(
            upstream.local_addr().unwrap(),
            GameLimits {
                max_bytes,
                ..GameLimits::default()
            },
        );
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        let worker = thread::spawn(move || {
            let mut first = [0];
            socket.read_exact(&mut first).unwrap();
            assert_eq!(first, [14]);
            let mut total = 1;
            let mut buffer = [0; MAX_MESSAGE];
            loop {
                let n = socket.read(&mut buffer).unwrap();
                if n == 0 {
                    break;
                }
                assert!(buffer[..n].iter().all(|byte| *byte == 0x55));
                total += n;
                assert!(total <= max_bytes);
            }
            total
        });
        let mut remaining = max_bytes - 1;
        while remaining > 0 {
            let n = remaining.min(MAX_MESSAGE);
            ws.send(Message::binary(vec![0x55; n])).unwrap();
            remaining -= n;
        }
        ws.send(Message::binary(vec![0x55])).unwrap();
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        assert_eq!(worker.join().unwrap(), max_bytes);
    }
}

#[test]
fn game_total_byte_budget_combines_both_directions_and_honors_opt_in() {
    const REQUEST: usize = MAX_MESSAGE + 17;
    for (path, max_bytes) in [("/lobby", MAX_BYTES), ("/world", MAX_BYTES * 2)] {
        let upstream = upstream();
        let relay = game_relay(
            upstream.local_addr().unwrap(),
            GameLimits {
                max_bytes,
                ..GameLimits::default()
            },
        );
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        let response = max_bytes - REQUEST - 1;
        let (sender, receiver) = std::sync::mpsc::sync_channel(0);
        let worker = thread::spawn(move || {
            let mut first = [0];
            socket.read_exact(&mut first).unwrap();
            assert_eq!(first, [14]);
            let mut request = vec![0; REQUEST];
            socket.read_exact(&mut request).unwrap();
            assert!(request.iter().all(|byte| *byte == 0x55));
            let mut remaining = response;
            while remaining > 0 {
                let n = remaining.min(MAX_MESSAGE);
                socket.write_all(&vec![0x5a; n]).unwrap();
                remaining -= n;
            }
            receiver.recv_timeout(IO_TIMEOUT).unwrap();
            socket.write_all(&[0x5a]).unwrap();
            assert_eq!(socket.read(&mut first).unwrap(), 0);
        });
        ws.send(Message::Ping(vec![7, 8, 9].into())).unwrap();
        assert_eq!(ws.read().unwrap(), Message::Pong(vec![7, 8, 9].into()));
        ws.send(Message::binary(vec![0x55; MAX_MESSAGE])).unwrap();
        fragment(&mut ws, vec![0x55; 7], Data::Binary, false);
        fragment(&mut ws, vec![0x55; 10], Data::Continue, true);
        let mut total = 0;
        while total < response {
            match ws.read().unwrap() {
                Message::Binary(data) => {
                    assert!(data.iter().all(|byte| *byte == 0x5a));
                    total += data.len();
                    assert!(total <= response);
                }
                other => panic!("unexpected message: {other:?}"),
            }
        }
        sender.send(()).unwrap();
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        worker.join().unwrap();
    }
}

#[test]
fn login_transport_progresses_in_both_directions_under_backpressure() {
    const REQUEST: usize = 8 * 1024 * 1024;
    const RESPONSE: usize = 16 * 1024 * 1024;
    for path in ["/lobby", "/world"] {
        let upstream = upstream();
        let relay = extended_game_relay(upstream.local_addr().unwrap());
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        let worker = thread::spawn(move || {
            let options = socket2::SockRef::from(&socket);
            options.set_send_buffer_size(MAX_MESSAGE).unwrap();
            options.set_recv_buffer_size(MAX_MESSAGE).unwrap();
            socket
                .set_read_timeout(Some(Duration::from_secs(5)))
                .unwrap();
            socket
                .set_write_timeout(Some(Duration::from_secs(5)))
                .unwrap();
            let mut first = [0];
            socket.read_exact(&mut first).unwrap();
            assert_eq!(first, [14]);
            socket.write_all(&vec![0x5a; RESPONSE]).unwrap();
            let mut request = vec![0; REQUEST];
            socket.read_exact(&mut request).unwrap();
            assert!(request.iter().all(|byte| *byte == 0x55));
            socket.write_all(b"done").unwrap();
        });
        let writer_socket = ws.get_ref().try_clone().unwrap();
        let writer = thread::spawn(move || {
            writer_socket
                .set_write_timeout(Some(Duration::from_secs(5)))
                .unwrap();
            let mut sender = WebSocket::from_raw_socket(writer_socket, Role::Client, None);
            for _ in 0..REQUEST / MAX_MESSAGE {
                sender
                    .send(Message::binary(vec![0x55; MAX_MESSAGE]))
                    .unwrap();
            }
        });
        let mut response = Vec::new();
        while response.len() < RESPONSE + 4 {
            match ws.read().unwrap() {
                Message::Binary(data) => response.extend_from_slice(&data),
                other => panic!("unexpected message: {other:?}"),
            }
        }
        assert_eq!(response.len(), RESPONSE + 4);
        assert!(response[..RESPONSE].iter().all(|byte| *byte == 0x5a));
        assert_eq!(&response[RESPONSE..], b"done");
        writer.join().unwrap();
        worker.join().unwrap();
    }
}

#[test]
fn login_nonreading_upstream_expires_the_write_deadline() {
    for path in ["/lobby", "/world"] {
        let upstream = upstream();
        let relay = extended_game_relay(upstream.local_addr().unwrap());
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        ws.get_mut()
            .set_read_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        socket2::SockRef::from(&socket)
            .set_recv_buffer_size(4096)
            .unwrap();
        let mut first = [0];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [14]);
        let writer_socket = ws.get_ref().try_clone().unwrap();
        let start = Instant::now();
        let writer = thread::spawn(move || {
            writer_socket
                .set_write_timeout(Some(Duration::from_secs(5)))
                .unwrap();
            let mut sender = WebSocket::from_raw_socket(writer_socket, Role::Client, None);
            for _ in 0..32 * 1024 * 1024 / MAX_MESSAGE {
                if sender
                    .send(Message::binary(vec![0x55; MAX_MESSAGE]))
                    .is_err()
                {
                    return;
                }
            }
            panic!("nonreading upstream did not apply backpressure");
        });
        assert!(matches!(ws.read(), Ok(Message::Close(_)) | Err(_)));
        assert!(start.elapsed() < IO_TIMEOUT + Duration::from_secs(1));
        writer.join().unwrap();
        drop(ws);
        drop(socket);
        let stop = Instant::now();
        drop(relay);
        assert!(stop.elapsed() < Duration::from_millis(500));
    }
}

#[test]
fn login_nonreading_browser_expires_the_write_deadline() {
    for path in ["/lobby", "/world"] {
        let upstream = upstream();
        let relay = extended_game_relay(upstream.local_addr().unwrap());
        let mut ws = relay.connect(path, Some(ORIGIN)).unwrap();
        socket2::SockRef::from(ws.get_ref())
            .set_recv_buffer_size(4096)
            .unwrap();
        ws.send(Message::binary(vec![14])).unwrap();
        let mut socket = accepted(&upstream);
        socket2::SockRef::from(&socket)
            .set_send_buffer_size(MAX_MESSAGE)
            .unwrap();
        socket
            .set_write_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        let mut first = [0];
        socket.read_exact(&mut first).unwrap();
        assert_eq!(first, [14]);
        let start = Instant::now();
        let worker = thread::spawn(move || socket.write_all(&vec![0x5a; 32 * 1024 * 1024]));
        assert!(worker.join().unwrap().is_err());
        assert!(start.elapsed() < IO_TIMEOUT + Duration::from_secs(1));
        drop(ws);
        let stop = Instant::now();
        drop(relay);
        assert!(stop.elapsed() < Duration::from_millis(500));
    }
}
