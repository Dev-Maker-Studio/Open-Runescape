#![cfg(target_os = "linux")]

use std::fs::{self, OpenOptions};
use std::io::{Read, Write};
use std::net::{Shutdown, SocketAddr, TcpListener, TcpStream};
use std::os::unix::fs::{symlink, DirBuilderExt, OpenOptionsExt, PermissionsExt};
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Barrier};
use std::thread;
use std::time::{Duration, Instant};

const ROUTE: &str = "/__openrs/dev-login";
const FAKE_CONFIG: &str = r#"{"username":"fake-local-test-user","password":"not-a-real-account","lobby":{"host":"127.0.0.1","port":12345},"identity":{"complete":true}}"#;
static NEXT: AtomicUsize = AtomicUsize::new(0);

struct Fixture {
    directory: PathBuf,
    root: PathBuf,
    shared: PathBuf,
    config: PathBuf,
    address: SocketAddr,
}

impl Fixture {
    fn new() -> Self {
        let directory = std::env::temp_dir().join(format!(
            "openrs-web-login-test-{}-{}",
            std::process::id(),
            NEXT.fetch_add(1, Ordering::Relaxed)
        ));
        fs::DirBuilder::new()
            .mode(0o700)
            .create(&directory)
            .unwrap();
        let root = directory.join("static");
        let shared = directory.join("shared");
        fs::create_dir(&root).unwrap();
        fs::create_dir(&shared).unwrap();
        fs::write(root.join("index.html"), b"fake test page").unwrap();
        let config = directory.join("private.json");
        OpenOptions::new()
            .create_new(true)
            .write(true)
            .mode(0o600)
            .open(&config)
            .unwrap()
            .write_all(FAKE_CONFIG.as_bytes())
            .unwrap();
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let address = listener.local_addr().unwrap();
        drop(listener);
        Self {
            directory,
            root,
            shared,
            config,
            address,
        }
    }

    fn origin(&self) -> String {
        format!("http://{}", self.address)
    }

    fn command(&self) -> Command {
        let mut command = Command::new(env!("CARGO_BIN_EXE_openrs-web"));
        command
            .env_clear()
            .env("HOST", self.address.ip().to_string())
            .env("PORT", self.address.port().to_string())
            .env("STATIC_DIR", &self.root)
            .env("FILES_DIR", &self.shared)
            .env("OPENRS_DEV_LOGIN_CONFIG", &self.config)
            .env("OPENRS_DEV_ORIGIN", self.origin())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());
        command
    }

    fn start(&self) -> Running {
        self.start_command(self.command())
    }

    fn start_command(&self, mut command: Command) -> Running {
        let mut child = command.spawn().unwrap();
        let deadline = Instant::now() + Duration::from_secs(5);
        loop {
            if TcpStream::connect(self.address).is_ok() {
                return Running { child };
            }
            if child.try_wait().unwrap().is_some() || Instant::now() > deadline {
                let _ = child.kill();
                let _ = child.wait();
                panic!("fake development server did not start");
            }
            thread::sleep(Duration::from_millis(10));
        }
    }

    fn reject(&self, mut command: Command) {
        let mut child = command.spawn().unwrap();
        let deadline = Instant::now() + Duration::from_secs(5);
        loop {
            if let Some(status) = child.try_wait().unwrap() {
                assert!(!status.success());
                let mut stdout = String::new();
                let mut stderr = String::new();
                child
                    .stdout
                    .take()
                    .unwrap()
                    .read_to_string(&mut stdout)
                    .unwrap();
                child
                    .stderr
                    .take()
                    .unwrap()
                    .read_to_string(&mut stderr)
                    .unwrap();
                assert!(stdout.is_empty());
                assert_eq!(stderr, "invalid local development login configuration\n");
                return;
            }
            if Instant::now() > deadline {
                let _ = child.kill();
                let _ = child.wait();
                panic!("invalid development configuration did not fail startup");
            }
            thread::sleep(Duration::from_millis(10));
        }
    }

    fn request(&self) -> WireRequest {
        WireRequest {
            method: "POST".into(),
            path: ROUTE.into(),
            headers: vec![
                ("Host".into(), self.address.to_string()),
                ("Origin".into(), self.origin()),
                ("Sec-Fetch-Site".into(), "same-origin".into()),
                ("Content-Type".into(), "application/json".into()),
                ("X-OpenRS-Intent".into(), "original-login".into()),
                ("Content-Length".into(), "2".into()),
                ("Connection".into(), "close".into()),
            ],
            body: "{}".into(),
        }
    }
}

impl Drop for Fixture {
    fn drop(&mut self) {
        let _ = fs::remove_dir_all(&self.directory);
    }
}

struct Running {
    child: Child,
}

impl Drop for Running {
    fn drop(&mut self) {
        let _ = self.child.kill();
        let _ = self.child.wait();
        let mut stdout = String::new();
        let mut stderr = String::new();
        self.child
            .stdout
            .take()
            .unwrap()
            .read_to_string(&mut stdout)
            .unwrap();
        self.child
            .stderr
            .take()
            .unwrap()
            .read_to_string(&mut stderr)
            .unwrap();
        assert_eq!(stdout, "serving web content\n");
        assert!(stderr.is_empty());
    }
}

#[derive(Clone)]
struct WireRequest {
    method: String,
    path: String,
    headers: Vec<(String, String)>,
    body: String,
}

impl WireRequest {
    fn set(&mut self, name: &str, value: &str) {
        self.remove(name);
        self.headers.push((name.into(), value.into()));
    }

    fn remove(&mut self, name: &str) {
        self.headers
            .retain(|(field, _)| !field.eq_ignore_ascii_case(name));
    }

    fn wire(&self) -> String {
        let mut wire = format!("{} {} HTTP/1.1\r\n", self.method, self.path);
        for (field, value) in &self.headers {
            wire.push_str(&format!("{field}: {value}\r\n"));
        }
        wire.push_str("\r\n");
        wire.push_str(&self.body);
        wire
    }

    fn send(&self, address: SocketAddr) -> WireResponse {
        let mut stream = TcpStream::connect(address).unwrap();
        stream
            .set_read_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        stream
            .set_write_timeout(Some(Duration::from_secs(5)))
            .unwrap();
        stream.write_all(self.wire().as_bytes()).unwrap();
        stream.shutdown(Shutdown::Write).unwrap();
        let mut response = String::new();
        stream.read_to_string(&mut response).unwrap();
        let (headers, body) = response.split_once("\r\n\r\n").unwrap();
        let status = headers.split_whitespace().nth(1).unwrap().parse().unwrap();
        WireResponse {
            status,
            headers: headers.to_ascii_lowercase(),
            body: body.into(),
        }
    }
}

struct WireResponse {
    status: u16,
    headers: String,
    body: String,
}

impl WireResponse {
    fn protected(&self) {
        for line in [
            "cache-control: no-store, private, max-age=0",
            "pragma: no-cache",
            "expires: 0",
            "x-content-type-options: nosniff",
            "cross-origin-resource-policy: same-origin",
            "cross-origin-opener-policy: same-origin",
            "cross-origin-embedder-policy: require-corp",
        ] {
            assert!(self.headers.lines().any(|actual| actual == line));
        }
        assert!(!self.headers.contains("access-control-"));
    }
}

#[test]
fn one_time_delivery_preserves_the_full_json_and_private_file() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    let first = fixture.request().send(fixture.address);
    assert_eq!(first.status, 200);
    assert_eq!(first.body, FAKE_CONFIG);
    assert!(first.headers.contains("content-type: application/json"));
    first.protected();
    let next = fixture.request().send(fixture.address);
    assert_eq!(next.status, 410);
    assert!(next.body.is_empty());
    next.protected();
    assert_eq!(fs::read_to_string(&fixture.config).unwrap(), FAKE_CONFIG);
}

#[test]
fn invalid_browser_selectors_and_methods_never_consume() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    for field in [
        "Host",
        "Origin",
        "Sec-Fetch-Site",
        "Content-Type",
        "X-OpenRS-Intent",
    ] {
        let mut missing = fixture.request();
        missing.remove(field);
        assert_eq!(missing.send(fixture.address).status, 403);
        let mut wrong = fixture.request();
        wrong.set(field, "wrong");
        assert_eq!(wrong.send(fixture.address).status, 403);
        let mut duplicate = fixture.request();
        let value = duplicate
            .headers
            .iter()
            .find(|(name, _)| name == field)
            .unwrap()
            .1
            .clone();
        duplicate.headers.push((field.to_ascii_lowercase(), value));
        assert_eq!(duplicate.send(fixture.address).status, 403);
    }
    for origin in [
        "null",
        "https://127.0.0.1",
        "http://evil.invalid",
        "http://127.0.0.1:1",
    ] {
        let mut request = fixture.request();
        request.set("Origin", origin);
        assert_eq!(request.send(fixture.address).status, 403);
    }
    for site in [
        "none",
        "cross-site",
        "same-site",
        "same-origin, same-origin",
    ] {
        let mut request = fixture.request();
        request.set("Sec-Fetch-Site", site);
        assert_eq!(request.send(fixture.address).status, 403);
    }
    for method in ["GET", "OPTIONS", "HEAD", "PUT"] {
        let mut request = fixture.request();
        request.method = method.into();
        let response = request.send(fixture.address);
        assert_eq!(response.status, 405);
        assert!(response.body.is_empty());
        response.protected();
    }
    for suffix in ["?", "?anything=1", "/", "#fragment", "-alias"] {
        let mut request = fixture.request();
        request.path.push_str(suffix);
        assert_eq!(request.send(fixture.address).status, 404);
    }
    assert_eq!(fixture.request().send(fixture.address).status, 200);
}

#[test]
fn ambiguous_framing_and_nonempty_payloads_never_consume() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    for body in ["[]", "  ", "{", "{} ", "{\"unexpected\":true}"] {
        let mut request = fixture.request();
        request.body = body.into();
        request.set("Content-Length", &body.len().to_string());
        assert_eq!(request.send(fixture.address).status, 400);
    }
    for (field, value) in [
        ("Content-Length", "2"),
        ("Connection", "close"),
        ("Content-Encoding", "identity"),
        ("Upgrade", "websocket"),
        ("Trailer", "X-Anything"),
        ("Forwarded", "host=evil.invalid"),
        ("X-Forwarded-Host", "evil.invalid"),
        ("X-Forwarded-Proto", "https"),
        ("TE", "chunked"),
    ] {
        let mut request = fixture.request();
        request.headers.push((field.into(), value.into()));
        assert_eq!(request.send(fixture.address).status, 400);
    }
    let mut transfer = fixture.request();
    transfer.set("Transfer-Encoding", "chunked");
    transfer.body = "2\r\n{}\r\n0\r\n\r\n".into();
    assert_eq!(transfer.send(fixture.address).status, 400);
    let mut upgrade = fixture.request();
    upgrade.set("Connection", "upgrade");
    assert_eq!(upgrade.send(fixture.address).status, 400);
    let mut expect = fixture.request();
    expect.set("Expect", "100-continue");
    assert_eq!(expect.send(fixture.address).status, 400);
    let mut request = fixture.request();
    request.set("Content-Length", "0");
    request.body.clear();
    assert_eq!(request.send(fixture.address).status, 200);
}

#[test]
fn parallel_requests_have_exactly_one_winner() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    let barrier = Arc::new(Barrier::new(16));
    let workers: Vec<_> = (0..16)
        .map(|_| {
            let barrier = barrier.clone();
            let request = fixture.request();
            let address = fixture.address;
            thread::spawn(move || {
                barrier.wait();
                request.send(address)
            })
        })
        .collect();
    let responses: Vec<_> = workers.into_iter().map(|w| w.join().unwrap()).collect();
    assert_eq!(responses.iter().filter(|r| r.status == 200).count(), 1);
    assert_eq!(responses.iter().filter(|r| r.status == 410).count(), 15);
    assert_eq!(
        responses.iter().filter(|r| r.body == FAKE_CONFIG).count(),
        1
    );
    for response in responses {
        response.protected();
    }
}

#[test]
fn disabled_endpoint_is_not_a_static_file_route() {
    let fixture = Fixture::new();
    fs::create_dir(fixture.root.join("__openrs")).unwrap();
    fs::write(fixture.root.join("__openrs/dev-login"), b"not an endpoint").unwrap();
    let mut command = fixture.command();
    command
        .env_remove("OPENRS_DEV_LOGIN_CONFIG")
        .env_remove("OPENRS_DEV_ORIGIN");
    let _server = fixture.start_command(command);
    for method in ["GET", "POST", "OPTIONS"] {
        let mut request = fixture.request();
        request.method = method.into();
        assert_eq!(request.send(fixture.address).status, 404);
    }
    let mut request = fixture.request();
    request.method = "GET".into();
    request.path = "/".into();
    assert_eq!(request.send(fixture.address).body, "fake test page");
}

#[test]
fn startup_rejects_invalid_environment_without_echoing_values() {
    let fixture = Fixture::new();
    for (name, value) in [
        ("HOST", "0.0.0.0"),
        ("HOST", "192.0.2.1"),
        ("HOST", "example.invalid"),
        ("PORT", "0"),
        ("PORT", "08080"),
        ("OPENRS_DEV_ORIGIN", "https://127.0.0.1:8080"),
        ("OPENRS_DEV_ORIGIN", "http://127.0.0.1:1"),
        ("OPENRS_DEV_ORIGIN", "http://localhost:8080"),
        ("OPENRS_DEV_ORIGIN", "http://127.0.0.1:8080/"),
        ("OPENRS_DEV_ORIGIN", "http://127.0.0.1:8080?query=1"),
        (
            "OPENRS_DEV_ORIGIN",
            "http://user:fake-password@127.0.0.1:8080",
        ),
        ("OPENRS_DEV_LOGIN_CONFIG", "relative-private.json"),
        ("OPENRS_DEV_LOGIN_CONFIG", ""),
        (
            "OPENRS_DEV_LOGIN_CONFIG",
            "/missing/fake-private-config.json",
        ),
    ] {
        let mut command = fixture.command();
        command.env(name, value);
        fixture.reject(command);
    }
    for name in ["OPENRS_DEV_LOGIN_CONFIG", "OPENRS_DEV_ORIGIN"] {
        let mut command = fixture.command();
        command.env_remove(name);
        fixture.reject(command);
    }
    let mut command = fixture.command();
    command.env("OPENRS_DEV_ORIGIN", format!("{}/", fixture.origin()));
    fixture.reject(command);
}

#[test]
fn startup_rejects_unsafe_files_and_public_roots() {
    let fixture = Fixture::new();
    for mode in [0o000, 0o200, 0o644, 0o660, 0o700, 0o1600, 0o4600] {
        fs::set_permissions(&fixture.config, fs::Permissions::from_mode(mode)).unwrap();
        fixture.reject(fixture.command());
    }
    fs::set_permissions(&fixture.config, fs::Permissions::from_mode(0o600)).unwrap();
    let link = fixture.directory.join("private-link.json");
    symlink(&fixture.config, &link).unwrap();
    let mut command = fixture.command();
    command.env("OPENRS_DEV_LOGIN_CONFIG", &link);
    fixture.reject(command);
    fs::remove_file(&link).unwrap();
    fs::hard_link(&fixture.config, &link).unwrap();
    fixture.reject(fixture.command());
    fs::remove_file(&link).unwrap();
    for root in [&fixture.root, &fixture.shared] {
        let destination = root.join("fake-private.json");
        fs::rename(&fixture.config, &destination).unwrap();
        let mut command = fixture.command();
        command.env("OPENRS_DEV_LOGIN_CONFIG", &destination);
        fixture.reject(command);
        fs::rename(destination, &fixture.config).unwrap();
    }
    let public_alias = fixture.directory.join("public-alias");
    symlink(&fixture.directory, &public_alias).unwrap();
    let mut command = fixture.command();
    command.env("STATIC_DIR", &public_alias);
    fixture.reject(command);
    let mut command = fixture.command();
    command.env("OPENRS_DEV_LOGIN_CONFIG", &fixture.directory);
    fixture.reject(command);
}

#[test]
fn startup_rejects_nonobject_invalid_and_oversized_json() {
    let fixture = Fixture::new();
    for json in [
        "",
        "[]",
        "null",
        "true",
        "42",
        "\"fake\"",
        "{",
        "{}{}",
        "{\"x\":NaN}",
    ] {
        fs::write(&fixture.config, json).unwrap();
        fixture.reject(fixture.command());
    }
    fs::write(&fixture.config, b"{\"fake\":\"\xff\"}").unwrap();
    fixture.reject(fixture.command());
    let oversized = format!("{{\"fake\":\"{}\"}}", "x".repeat(64 * 1024));
    fs::write(&fixture.config, oversized).unwrap();
    fixture.reject(fixture.command());
}

#[test]
fn read_only_file_and_exact_limit_json_are_delivered_unchanged() {
    let fixture = Fixture::new();
    let maximum = format!("{{\"fake\":\"{}\"}}", "x".repeat(64 * 1024 - 11));
    assert_eq!(maximum.len(), 64 * 1024);
    fs::write(&fixture.config, &maximum).unwrap();
    fs::set_permissions(&fixture.config, fs::Permissions::from_mode(0o400)).unwrap();
    let _server = fixture.start();
    let response = fixture.request().send(fixture.address);
    assert_eq!(response.status, 200);
    assert_eq!(response.body, maximum);
    response.protected();
}

#[test]
fn localhost_origin_is_pinned_to_a_numeric_loopback_listener() {
    let fixture = Fixture::new();
    let origin = format!("http://localhost:{}", fixture.address.port());
    let mut command = fixture.command();
    command
        .env("HOST", "localhost")
        .env("OPENRS_DEV_ORIGIN", &origin);
    let _server = fixture.start_command(command);
    let mut request = fixture.request();
    request.set("Host", &format!("localhost:{}", fixture.address.port()));
    request.set("Origin", &origin);
    assert_eq!(request.send(fixture.address).status, 200);
}

#[test]
fn ipv6_origin_uses_a_loopback_only_listener() {
    let mut fixture = Fixture::new();
    let listener = TcpListener::bind("[::1]:0").unwrap();
    fixture.address = listener.local_addr().unwrap();
    drop(listener);
    let _server = fixture.start();
    assert_eq!(fixture.request().send(fixture.address).status, 200);
}

#[test]
fn loaded_config_is_memory_owned_and_never_reopened() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    fs::write(&fixture.config, b"{\"fake\":\"replaced\"}").unwrap();
    fs::remove_file(&fixture.config).unwrap();
    assert_eq!(fixture.request().send(fixture.address).body, FAKE_CONFIG);
}

#[test]
fn absolute_url_requests_do_not_select_the_endpoint() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    let mut request = fixture.request();
    request.path = format!("{}{ROUTE}", fixture.origin());
    assert_ne!(request.send(fixture.address).status, 200);
    assert_eq!(fixture.request().send(fixture.address).status, 200);
}

#[test]
fn a_disconnected_valid_request_does_not_restore_the_secret() {
    let fixture = Fixture::new();
    let _server = fixture.start();
    let mut stream = TcpStream::connect(fixture.address).unwrap();
    stream
        .set_read_timeout(Some(Duration::from_secs(5)))
        .unwrap();
    stream
        .write_all(fixture.request().wire().as_bytes())
        .unwrap();
    stream.read_exact(&mut [0; 1]).unwrap();
    stream.shutdown(Shutdown::Both).unwrap();
    drop(stream);
    assert_eq!(fixture.request().send(fixture.address).status, 410);
}
