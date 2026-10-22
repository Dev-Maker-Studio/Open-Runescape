use serde_json::{json, Value};
use std::io::{BufRead, BufReader, Read, Write};
use std::net::TcpStream;
use std::os::unix::net::UnixStream;

pub fn endpoint() -> String {
    std::env::var("OPENRS_CONTROL").unwrap_or_else(|_| {
        format!("{}/openrs-control.sock", std::env::var("XDG_RUNTIME_DIR").unwrap_or_else(|_| "/tmp".into()))
    })
}

trait Stream: Read + Write {}
impl<T: Read + Write> Stream for T {}

pub struct Conn(BufReader<Box<dyn Stream>>);

impl Conn {
    pub fn open() -> Result<Conn, String> {
        Conn::open_at(&endpoint())
    }

    pub fn open_at(endpoint: &str) -> Result<Conn, String> {
        let stream: Box<dyn Stream> = if endpoint.chars().all(|c| c.is_ascii_digit()) {
            let port = endpoint.parse::<u16>().map_err(|e| e.to_string())?;
            let s = TcpStream::connect(("127.0.0.1", port)).map_err(|e| format!("control endpoint {endpoint}: {e}"))?;
            let _ = s.set_nodelay(true);
            Box::new(s)
        } else {
            Box::new(UnixStream::connect(endpoint).map_err(|e| format!("control endpoint {endpoint}: {e}"))?)
        };
        Ok(Conn(BufReader::new(stream)))
    }

    fn exchange(&mut self, request: &Value) -> Result<Value, String> {
        writeln!(self.0.get_mut(), "{request}").map_err(|e| e.to_string())?;
        let mut line = String::new();
        if self.0.read_line(&mut line).map_err(|e| e.to_string())? == 0 {
            return Err("control connection closed".into());
        }
        let response: Value = serde_json::from_str(&line).map_err(|e| format!("bad response: {e}"))?;
        if response["ok"] == true {
            Ok(response["data"].clone())
        } else {
            Err(response["error"].as_str().unwrap_or("unknown error").to_string())
        }
    }

    pub fn send(&mut self, command: &str, args: &[String]) -> Result<Value, String> {
        self.exchange(&json!({ "cmd": command, "args": args }))
    }

    pub fn batch(&mut self, commands: &[Vec<String>]) -> Result<Vec<Value>, String> {
        let items: Vec<Value> = commands.iter().map(|c| json!({ "cmd": c[0], "args": &c[1..] })).collect();
        match self.exchange(&json!({ "batch": items }))? {
            Value::Array(results) => Ok(results),
            other => Err(format!("bad batch response: {other}")),
        }
    }
}

pub fn send(command: &str, args: &[String]) -> Result<Value, String> {
    Conn::open()?.send(command, args)
}
