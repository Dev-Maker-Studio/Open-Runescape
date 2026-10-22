use crate::game::{self, DT, Game, TICK_HZ};
use renderer::{
    frame::Frame,
    stream::{self, Input, ToHost, WINDOW},
};
use std::{
    collections::HashMap,
    io::ErrorKind,
    net::{TcpListener, TcpStream},
    time::{Duration, Instant},
};
use tungstenite::{Error, Message, WebSocket};

pub const PORT: u16 = 8100;
const DEFAULT_SIZE: (u32, u32) = (800, 600);
const MESH: u8 = 0;
const TEXTURE: u8 = 1;

#[derive(Default)]
pub struct Flow {
    pub sent: u32,
    pub acked: u32,
    pub dropped: u32,
}

impl Flow {
    pub fn offer(&mut self, number: u32) -> bool {
        if self.sent.saturating_sub(self.acked) >= WINDOW {
            self.dropped += 1;
            return false;
        }
        self.sent = number;
        true
    }

    pub fn ack(&mut self, number: u32) {
        self.acked = self.acked.max(number);
    }
}

pub fn hash(bytes: &[u8]) -> u64 {
    bytes.iter().fold(0xcbf29ce484222325, |h, b| (h ^ u64::from(*b)).wrapping_mul(0x100000001b3))
}

#[derive(Default)]
pub struct Sent(HashMap<(u8, u32), u64>);

impl Sent {
    pub fn split(&mut self, f: &mut Frame) -> Option<Frame> {
        let mut r = Frame::default();
        for m in std::mem::take(&mut f.meshes) {
            let h = hash(&m.vertices) ^ hash(bytemuck::cast_slice(&m.indices));
            if self.0.insert((MESH, m.id), h) != Some(h) {
                r.meshes.push(m);
            }
        }
        for t in std::mem::take(&mut f.textures) {
            let h = hash(&t.rgba) ^ u64::from(t.width) ^ u64::from(t.layers) << 32;
            if self.0.insert((TEXTURE, t.id), h) != Some(h) {
                r.textures.push(t);
            }
        }
        (!r.meshes.is_empty() || !r.textures.is_empty()).then_some(r)
    }
}

pub fn game_name(uri: &str) -> String {
    let (path, query) = uri.split_once('?').unwrap_or((uri, ""));
    query
        .split('&')
        .find_map(|p| p.strip_prefix("game="))
        .map(str::to_string)
        .unwrap_or_else(|| path.trim_matches('/').to_string())
}

struct Session {
    ws: WebSocket<TcpStream>,
    game: Box<dyn Game>,
    sent: Sent,
    flow: Flow,
    inputs: Vec<Input>,
    seq: u32,
    number: u32,
    size: (u32, u32),
}

impl Session {
    fn send(&mut self, bytes: Vec<u8>) -> Result<(), Error> {
        self.ws.write(Message::binary(bytes))?;
        self.flush()
    }

    fn flush(&mut self) -> Result<(), Error> {
        match self.ws.flush() {
            Err(Error::Io(e)) if e.kind() == ErrorKind::WouldBlock => Ok(()),
            r => r,
        }
    }

    fn receive(&mut self) -> Result<(), Error> {
        loop {
            match self.ws.read() {
                Ok(Message::Binary(b)) => match stream::decode_to_host(&b) {
                    Some(ToHost::Input(i)) => {
                        if i.kind == stream::RESIZE && i.a >= 1. && i.b >= 1. {
                            self.size = (i.a as u32, i.b as u32);
                        }
                        self.seq = i.seq;
                        self.inputs.push(i);
                    }
                    Some(ToHost::Ack(n)) => self.flow.ack(n),
                    _ => {}
                },
                Ok(Message::Close(_)) => return Err(Error::ConnectionClosed),
                Ok(_) => {}
                Err(Error::Io(e)) if e.kind() == ErrorKind::WouldBlock => return Ok(()),
                Err(e) => return Err(e),
            }
        }
    }

    fn tick(&mut self) -> Result<(), Error> {
        self.receive()?;
        self.game.step(DT, &self.inputs);
        self.inputs.clear();
        self.number += 1;
        if self.flow.offer(self.number) {
            let mut f = self.game.frame(self.size.0, self.size.1);
            if let Some(r) = self.sent.split(&mut f) {
                self.send(stream::resource(&r))?;
            }
            self.send(stream::frame_message(self.number, self.seq, self.flow.dropped, &f))?;
        } else if self.number % TICK_HZ == 0 {
            self.send(stream::ping(self.number))?;
        }
        self.flush()
    }

    fn run(mut self) -> Result<(), Error> {
        let step = Duration::from_secs(1) / TICK_HZ;
        let mut next = Instant::now();
        loop {
            self.tick()?;
            next += step;
            let now = Instant::now();
            if next > now {
                std::thread::sleep(next - now);
            } else {
                next = now;
            }
        }
    }
}

fn session(stream: TcpStream) -> Result<(), String> {
    let peer = stream.peer_addr().map_err(|e| e.to_string())?;
    let mut name = String::new();
    let mut ws = tungstenite::accept_hdr(stream, |req: &tungstenite::handshake::server::Request, resp| {
        name = game_name(&req.uri().to_string());
        Ok(resp)
    })
    .map_err(|e| match e {
        tungstenite::HandshakeError::Failure(Error::Protocol(_)) => String::new(),
        e => e.to_string(),
    })?;
    let Some(game) = game::create(&name) else {
        let _ = ws.send(Message::text(format!("unknown game {name}, games: {}", game::NAMES.join(" "))));
        let _ = ws.close(None);
        return Err(format!("{peer}: unknown game {name}"));
    };
    ws.get_ref().set_nodelay(true).map_err(|e| e.to_string())?;
    ws.send(Message::binary(stream::hello(&name, DEFAULT_SIZE.0, DEFAULT_SIZE.1, TICK_HZ))).map_err(|e| e.to_string())?;
    ws.get_ref().set_nonblocking(true).map_err(|e| e.to_string())?;
    println!("{peer}: {name} started");
    let s = Session { ws, game, sent: Sent::default(), flow: Flow::default(), inputs: vec![], seq: 0, number: 0, size: DEFAULT_SIZE };
    let r = s.run();
    println!("{peer}: {name} ended: {}", r.err().map_or("ok".into(), |e| e.to_string()));
    Ok(())
}

pub fn bind(port: u16) -> std::io::Result<TcpListener> {
    TcpListener::bind(("127.0.0.1", port))
}

pub fn serve(listener: TcpListener) {
    for stream in listener.incoming().flatten() {
        std::thread::spawn(move || {
            if let Err(e) = session(stream).map_err(|e| e.to_string()) {
                if !e.is_empty() {
                    eprintln!("{e}");
                }
            }
        });
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use renderer::scene;

    #[test]
    fn flow_drops_when_behind() {
        let mut f = Flow::default();
        assert!(f.offer(1) && f.offer(2));
        assert!(!f.offer(3) && !f.offer(4));
        assert_eq!(f.dropped, 2);
        f.ack(1);
        assert!(f.offer(5) && !f.offer(6));
        f.ack(5);
        assert!(f.offer(7));
        assert_eq!((f.sent, f.acked, f.dropped), (7, 5, 3));
    }

    #[test]
    fn resources_go_once_unless_changed() {
        let mut sent = Sent::default();
        let mut f = scene::build("model", 0, 64, 64).unwrap();
        let r = sent.split(&mut f).unwrap();
        assert_eq!((r.meshes.len(), r.textures.len(), f.meshes.len(), f.textures.len()), (1, 1, 0, 0));
        let mut again = scene::build("model", 0, 64, 64).unwrap();
        assert!(sent.split(&mut again).is_none());
        let mut changed = scene::build("model", 0, 64, 64).unwrap();
        changed.meshes[0].vertices[0] ^= 1;
        assert_eq!(sent.split(&mut changed).unwrap().meshes.len(), 1);
    }

    #[test]
    fn game_from_uri() {
        assert_eq!(game_name("/?game=walker"), "walker");
        assert_eq!(game_name("/orbit"), "orbit");
        assert_eq!(game_name("/?x=1&game=buttons"), "buttons");
        assert_eq!(game_name("/"), "");
    }
}
