use std::sync::{Arc, atomic::AtomicBool};

fn main() {
    let result = (|| {
        let config =
            openrs_js5_relay::Config::parse(&std::env::args().skip(1).collect::<Vec<_>>())?;
        let shutdown = Arc::new(AtomicBool::new(false));
        for signal in [signal_hook::consts::SIGINT, signal_hook::consts::SIGTERM] {
            signal_hook::flag::register(signal, shutdown.clone())
                .map_err(|_| "signal setup failed")?;
        }
        openrs_js5_relay::run(config, shutdown)
    })();
    if let Err(error) = result {
        eprintln!("{error}");
        std::process::exit(1);
    }
}
