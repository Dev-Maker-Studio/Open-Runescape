mod dev_login;
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
    let files = std::env::var_os("FILES_DIR").map(PathBuf::from);
    let login = match dev_login::DevLogin::from_env(&host, &port, &root, files.as_deref()) {
        Ok(login) => login.map(Arc::new),
        Err(message) => {
            eprintln!("{message}");
            std::process::exit(1);
        }
    };
    let address = login.as_ref().map_or_else(
        || format!("{host}:{port}"),
        |login| login.bind().to_string(),
    );
    let server = match Server::http(address) {
        Ok(server) => Arc::new(server),
        Err(_) => {
            eprintln!("web server bind failed");
            std::process::exit(1);
        }
    };
    println!("serving web content");

    let stopper = server.clone();
    ctrlc::set_handler(move || (0..WORKERS).for_each(|_| stopper.unblock()))
        .expect("signal handler");

    let workers: Vec<_> = (0..WORKERS)
        .map(|_| {
            let (server, root, shared, login) =
                (server.clone(), root.clone(), files.clone(), login.clone());
            thread::spawn(move || {
                while let Ok(request) = server.recv() {
                    if let Some(request) = dev_login::dispatch(login.as_deref(), request) {
                        files::serve(&root, shared.as_deref(), request);
                    }
                }
            })
        })
        .collect();
    for worker in workers {
        let _ = worker.join();
    }
}
