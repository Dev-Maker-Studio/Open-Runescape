mod files;
mod mime;

use std::path::PathBuf;
use std::sync::Arc;
use std::thread;
use tiny_http::Server;

const WORKERS: usize = 4;

fn main() {
    let port = std::env::var("PORT").unwrap_or_else(|_| "8080".into());
    let host = std::env::var("HOST").unwrap_or_else(|_| "127.0.0.1".into());
    let root = PathBuf::from(std::env::var("STATIC_DIR").unwrap_or_else(|_| "static".into()));
    let server = Arc::new(Server::http(format!("{host}:{port}")).expect("bind failed"));
    println!("serving {} on http://{host}:{port}", root.display());

    let stopper = server.clone();
    ctrlc::set_handler(move || (0..WORKERS).for_each(|_| stopper.unblock())).expect("signal handler");

    let workers: Vec<_> = (0..WORKERS)
        .map(|_| {
            let (server, root) = (server.clone(), root.clone());
            thread::spawn(move || {
                while let Ok(request) = server.recv() {
                    files::serve(&root, request);
                }
            })
        })
        .collect();
    for worker in workers {
        let _ = worker.join();
    }
}
