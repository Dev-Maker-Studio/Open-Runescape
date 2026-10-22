use std::ffi::OsString;
use std::io::{self, Read};
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::path::Path;
use std::sync::Mutex;
use tiny_http::{Header, Method, Request, Response, StatusCode};
use zeroize::Zeroizing;

const ROUTE: &str = "/__openrs/dev-login";
const MAX_CONFIG_BYTES: usize = 64 * 1024;
const INVALID: &str = "invalid local development login configuration";

pub struct DevLogin {
    bind: SocketAddr,
    origin: String,
    authority: String,
    config: Mutex<Option<Zeroizing<Vec<u8>>>>,
}

impl DevLogin {
    pub fn from_env(
        host: &str,
        port: &str,
        root: &Path,
        shared: Option<&Path>,
    ) -> Result<Option<Self>, &'static str> {
        Self::configured(
            std::env::var_os("OPENRS_DEV_LOGIN_CONFIG"),
            std::env::var_os("OPENRS_DEV_ORIGIN"),
            host,
            port,
            root,
            shared,
        )
    }

    fn configured(
        path: Option<OsString>,
        origin: Option<OsString>,
        host: &str,
        port: &str,
        root: &Path,
        shared: Option<&Path>,
    ) -> Result<Option<Self>, &'static str> {
        let (path, origin) = match (path, origin) {
            (None, None) => return Ok(None),
            (Some(path), Some(origin)) => (path, origin),
            _ => return Err(INVALID),
        };
        let port_number: u16 = port.parse().map_err(|_| INVALID)?;
        if port_number == 0 || port_number.to_string() != port {
            return Err(INVALID);
        }
        let ip = if host == "localhost" {
            IpAddr::V4(Ipv4Addr::LOCALHOST)
        } else {
            host.strip_prefix('[')
                .and_then(|s| s.strip_suffix(']'))
                .unwrap_or(host)
                .parse::<IpAddr>()
                .map_err(|_| INVALID)?
        };
        if !ip.is_loopback() {
            return Err(INVALID);
        }
        let origin_host = if host == "localhost" {
            host.to_owned()
        } else if ip.is_ipv6() {
            format!("[{ip}]")
        } else {
            ip.to_string()
        };
        let authority = if port_number == 80 {
            origin_host
        } else {
            format!("{origin_host}:{port_number}")
        };
        let expected = format!("http://{authority}");
        if origin.to_str() != Some(expected.as_str()) {
            return Err(INVALID);
        }
        let config = read_config(Path::new(&path), root, shared)?;
        Ok(Some(Self {
            bind: SocketAddr::new(ip, port_number),
            origin: expected,
            authority,
            config: Mutex::new(Some(config)),
        }))
    }

    pub fn bind(&self) -> SocketAddr {
        self.bind
    }

    fn validate(&self, request: &mut Request) -> Result<(), u16> {
        if request.url() != ROUTE {
            return Err(404);
        }
        if request.method() != &Method::Post {
            return Err(405);
        }
        if !request.remote_addr().is_some_and(|a| a.ip().is_loopback()) {
            return Err(403);
        }
        for (name, expected) in [
            ("Host", self.authority.as_str()),
            ("Origin", self.origin.as_str()),
            ("Sec-Fetch-Site", "same-origin"),
            ("Content-Type", "application/json"),
            ("X-OpenRS-Intent", "original-login"),
        ] {
            if single_header(request, name) != Ok(Some(expected)) {
                return Err(403);
            }
        }
        for name in [
            "Transfer-Encoding",
            "TE",
            "Content-Encoding",
            "Expect",
            "Upgrade",
            "Trailer",
            "Forwarded",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
        ] {
            if single_header(request, name) != Ok(None) {
                return Err(400);
            }
        }
        if !matches!(
            single_header(request, "Connection"),
            Ok(None | Some("close" | "keep-alive"))
        ) {
            return Err(400);
        }
        if !matches!(
            single_header(request, "Content-Length"),
            Ok(None | Some("0" | "2"))
        ) {
            return Err(400);
        }
        let mut body = [0; 3];
        let mut length = 0;
        while length < body.len() {
            let count = request
                .as_reader()
                .read(&mut body[length..])
                .map_err(|_| 400_u16)?;
            if count == 0 {
                break;
            }
            length += count;
        }
        if !matches!(&body[..length], b"" | b"{}") {
            return Err(400);
        }
        Ok(())
    }

    fn serve(&self, mut request: Request) {
        if let Err(status) = self.validate(&mut request) {
            deny(request, status);
            return;
        }
        let config = match self.config.lock() {
            Ok(mut slot) => slot.take(),
            Err(_) => {
                deny(request, 500);
                return;
            }
        };
        let Some(config) = config else {
            deny(request, 410);
            return;
        };
        let length = config.len();
        let body = SecretBody { config, offset: 0 };
        let response = Response::new(
            StatusCode(200),
            response_headers(),
            body,
            Some(length),
            None,
        )
        .with_header(header("Content-Type", "application/json"))
        .with_chunked_threshold(MAX_CONFIG_BYTES + 1);
        let _ = request.respond(response);
    }
}

pub fn dispatch(login: Option<&DevLogin>, request: Request) -> Option<Request> {
    if !request.url().starts_with(ROUTE) {
        return Some(request);
    }
    match login {
        Some(login) => login.serve(request),
        None => deny(request, 404),
    }
    None
}

fn single_header<'a>(request: &'a Request, name: &'static str) -> Result<Option<&'a str>, ()> {
    let mut headers = request.headers().iter().filter(|h| h.field.equiv(name));
    let first = headers.next().map(|h| h.value.as_str());
    if headers.next().is_some() {
        Err(())
    } else {
        Ok(first)
    }
}

fn header(name: &str, value: &str) -> Header {
    Header::from_bytes(name, value).unwrap()
}

fn response_headers() -> Vec<Header> {
    [
        ("Cache-Control", "no-store, private, max-age=0"),
        ("Pragma", "no-cache"),
        ("Expires", "0"),
        ("X-Content-Type-Options", "nosniff"),
        ("Cross-Origin-Resource-Policy", "same-origin"),
        ("Cross-Origin-Opener-Policy", "same-origin"),
        ("Cross-Origin-Embedder-Policy", "require-corp"),
    ]
    .into_iter()
    .map(|(name, value)| header(name, value))
    .collect()
}

fn deny(request: Request, status: u16) {
    let response = Response::new(
        StatusCode(status),
        response_headers(),
        io::empty(),
        Some(0),
        None,
    );
    let _ = request.respond(response);
}

struct SecretBody {
    config: Zeroizing<Vec<u8>>,
    offset: usize,
}

impl Read for SecretBody {
    fn read(&mut self, output: &mut [u8]) -> io::Result<usize> {
        let remaining = &self.config[self.offset..];
        let length = output.len().min(remaining.len());
        output[..length].copy_from_slice(&remaining[..length]);
        self.offset += length;
        Ok(length)
    }
}

#[cfg(target_os = "linux")]
fn read_config(
    path: &Path,
    root: &Path,
    shared: Option<&Path>,
) -> Result<Zeroizing<Vec<u8>>, &'static str> {
    use std::fs::{self, OpenOptions};
    use std::os::fd::AsRawFd;
    use std::os::unix::fs::{MetadataExt, OpenOptionsExt};

    if !path.is_absolute() {
        return Err(INVALID);
    }
    let mut file = OpenOptions::new()
        .read(true)
        .custom_flags(libc::O_NOFOLLOW | libc::O_NONBLOCK)
        .open(path)
        .map_err(|_| INVALID)?;
    let valid_metadata = |metadata: &fs::Metadata| {
        metadata.is_file()
            && metadata.uid() == nix::unistd::geteuid().as_raw()
            && matches!(metadata.mode() & 0o7777, 0o600 | 0o400)
            && metadata.nlink() == 1
            && metadata.len() <= MAX_CONFIG_BYTES as u64
    };
    let metadata = file.metadata().map_err(|_| INVALID)?;
    if !valid_metadata(&metadata) {
        return Err(INVALID);
    }
    let opened_path =
        fs::read_link(format!("/proc/self/fd/{}", file.as_raw_fd())).map_err(|_| INVALID)?;
    for directory in std::iter::once(root).chain(shared) {
        let directory = directory.canonicalize().map_err(|_| INVALID)?;
        if opened_path.starts_with(directory) {
            return Err(INVALID);
        }
    }
    let mut bytes = Zeroizing::new(vec![0; MAX_CONFIG_BYTES + 1]);
    let mut length = 0;
    while length < bytes.len() {
        let count = file.read(&mut bytes[length..]).map_err(|_| INVALID)?;
        if count == 0 {
            break;
        }
        length += count;
    }
    let after = file.metadata().map_err(|_| INVALID)?;
    if length > MAX_CONFIG_BYTES
        || length as u64 != metadata.len()
        || after.len() != metadata.len()
        || !valid_metadata(&after)
    {
        return Err(INVALID);
    }
    bytes.truncate(length);
    let json: &serde_json::value::RawValue = serde_json::from_slice(&bytes).map_err(|_| INVALID)?;
    if !json.get().starts_with('{') {
        return Err(INVALID);
    }
    Ok(bytes)
}

#[cfg(not(target_os = "linux"))]
fn read_config(
    _path: &Path,
    _root: &Path,
    _shared: Option<&Path>,
) -> Result<Zeroizing<Vec<u8>>, &'static str> {
    Err(INVALID)
}
