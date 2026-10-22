use games::{game, host};
use renderer::{
    frame,
    stream::{self, Input, ToBrowser},
};
use std::{net::TcpStream, time::Duration};
use tungstenite::{Message, WebSocket};

fn start() -> u16 {
    let l = host::bind(0).unwrap();
    let port = l.local_addr().unwrap().port();
    std::thread::spawn(move || host::serve(l));
    port
}

fn connect(port: u16, game: &str) -> WebSocket<TcpStream> {
    let stream = TcpStream::connect(("127.0.0.1", port)).unwrap();
    stream.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
    tungstenite::client::client(format!("ws://127.0.0.1:{port}/?game={game}"), stream).unwrap().0
}

fn next(ws: &mut WebSocket<TcpStream>) -> Vec<u8> {
    loop {
        if let Message::Binary(b) = ws.read().unwrap() {
            return b.to_vec();
        }
    }
}

fn frame_of(bytes: &[u8]) -> (u32, u32, u32, frame::Frame) {
    match stream::decode_to_browser(bytes) {
        Some(ToBrowser::Frame { number, seq, dropped, orf }) => (number, seq, dropped, frame::parse(orf).unwrap()),
        other => panic!("expected a frame, got {other:?}"),
    }
}

fn send(ws: &mut WebSocket<TcpStream>, bytes: Vec<u8>) {
    ws.send(Message::binary(bytes)).unwrap();
}

#[test]
fn every_game_streams_and_echoes_input() {
    let port = start();
    for name in game::NAMES {
        let mut ws = connect(port, name);
        let hello = next(&mut ws);
        assert_eq!(stream::decode_to_browser(&hello), Some(ToBrowser::Hello(&format!("{{\"game\":\"{name}\",\"width\":800,\"height\":600,\"tick\":60}}"))));
        let res = next(&mut ws);
        let Some(ToBrowser::Resource(orf)) = stream::decode_to_browser(&res) else { panic!("expected resources first") };
        let r = frame::parse(orf).unwrap();
        assert!(!r.textures.is_empty() && r.draws.is_empty());
        let (n1, seq, dropped, f1) = frame_of(&next(&mut ws));
        assert_eq!((n1, seq, dropped), (1, 0, 0));
        assert!(!f1.draws.is_empty() && f1.textures.is_empty() && (f1.width, f1.height) == (800, 600));
        send(&mut ws, stream::ack(n1));
        send(&mut ws, stream::input(&Input { seq: 5, kind: stream::RESIZE, a: 320., b: 200., ..Default::default() }));
        send(&mut ws, stream::input(&Input { seq: 6, kind: stream::KEY_DOWN, code: "KeyW".into(), ..Default::default() }));
        let mut last = (0, 0, frame::Frame::default());
        for _ in 0..8 {
            let b = next(&mut ws);
            if let Some(ToBrowser::Frame { .. }) = stream::decode_to_browser(&b) {
                let (n, s, _, f) = frame_of(&b);
                send(&mut ws, stream::ack(n));
                last = (n, s, f);
                if s == 6 {
                    break;
                }
            }
        }
        assert_eq!(last.1, 6, "{name} never echoed the input sequence");
        assert_eq!((last.2.width, last.2.height), (320, 200));
        ws.close(None).unwrap();
    }
}

#[test]
fn host_drops_frames_until_acked() {
    let port = start();
    let mut ws = connect(port, "buttons");
    let mut frames = 0;
    let mut pings = 0;
    let mut newest = 0;
    while pings == 0 {
        match stream::decode_to_browser(&next(&mut ws)) {
            Some(ToBrowser::Frame { number, .. }) => {
                frames += 1;
                newest = number;
            }
            Some(ToBrowser::Ping(_)) => pings += 1,
            _ => {}
        }
    }
    assert_eq!(frames, stream::WINDOW);
    send(&mut ws, stream::ack(newest));
    let (n, _, dropped, _) = frame_of(&next(&mut ws));
    assert!(n > newest + 50 && dropped >= 50, "frame {n} after {newest}, dropped {dropped}");
    ws.close(None).unwrap();
}

#[test]
fn unknown_game_is_refused() {
    let port = start();
    let mut ws = connect(port, "nope");
    let Message::Text(t) = ws.read().unwrap() else { panic!() };
    assert!(t.as_str().starts_with("unknown game nope"));
}
