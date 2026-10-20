use std::io::{Read, Write};
use std::net::{TcpStream, ToSocketAddrs};
use std::time::Duration;

const TOKEN: &str = "ev9+VAp5/tMKeNR/7MOuH6lKWS+rGkHK";
const PREFETCH: usize = 27 * 4;

pub fn table(addr: &str, revision: u32) -> Result<Vec<i32>, String> {
    let sock = addr.to_socket_addrs().map_err(|e| format!("{addr}: {e}"))?.next().ok_or(format!("{addr}: no address"))?;
    let mut s = TcpStream::connect_timeout(&sock, Duration::from_secs(8)).map_err(|e| format!("{addr}: {e}"))?;
    s.set_read_timeout(Some(Duration::from_secs(15))).map_err(|e| e.to_string())?;
    let token = std::env::var("OPENRS_JS5_TOKEN").unwrap_or(TOKEN.into());
    let mut body = [&revision.to_be_bytes()[..], &1u32.to_be_bytes(), token.as_bytes(), &[0]].concat();
    body.splice(0..0, [15, body.len() as u8]);
    s.write_all(&body).map_err(|e| e.to_string())?;
    let mut head = [0u8; 1];
    s.read_exact(&mut head).map_err(|e| e.to_string())?;
    if head[0] != 0 {
        return Err(format!("{addr}: js5 refused with {}", head[0]));
    }
    s.read_exact(&mut [0u8; PREFETCH]).map_err(|e| e.to_string())?;
    s.write_all(&[6, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 1, 255, 0, 0, 0, 255]).map_err(|e| e.to_string())?;
    let mut h = [0u8; 10];
    s.read_exact(&mut h).map_err(|e| e.to_string())?;
    let len = u32::from_be_bytes([h[6], h[7], h[8], h[9]]) as usize;
    let mut data = Vec::with_capacity(len);
    let mut pos = 10;
    while data.len() < len {
        if pos % 512 == 0 {
            s.read_exact(&mut [0u8; 1]).map_err(|e| e.to_string())?;
            pos += 1;
        }
        let n = (len - data.len()).min(512 - pos % 512);
        let at = data.len();
        data.resize(at + n, 0);
        s.read_exact(&mut data[at..]).map_err(|e| e.to_string())?;
        pos += n;
    }
    Ok((0..data[0] as usize).map(|i| i32::from_be_bytes(data[1 + i * 72..5 + i * 72].try_into().unwrap())).collect())
}
