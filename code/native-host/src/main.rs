use std::{
    net::TcpListener,
    sync::{atomic::AtomicBool, Arc},
};

fn run() -> Result<(), String> {
    let c = openrs_native_host::Config::parse(&std::env::args().skip(1).collect::<Vec<_>>())?;
    let listener = TcpListener::bind(("127.0.0.1", c.port)).map_err(|e| e.to_string())?;
    let stop = Arc::new(AtomicBool::new(false));
    for signal in [signal_hook::consts::SIGTERM, signal_hook::consts::SIGINT] {
        signal_hook::flag::register(signal, stop.clone()).map_err(|e| e.to_string())?;
    }
    println!("native host ws://127.0.0.1:{}/?game=native", c.port);
    openrs_native_host::serve(listener, c, stop)
}

fn main() {
    if let Err(e) = run() {
        eprintln!("native host: {e}");
        std::process::exit(1);
    }
}
