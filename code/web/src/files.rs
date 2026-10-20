use crate::mime;
use std::fs::File;
use std::path::{Component, Path, PathBuf};
use tiny_http::{Header, Request, Response};

fn header(name: &str, value: &str) -> Header {
    Header::from_bytes(name, value).unwrap()
}

fn resolve(root: &Path, url: &str) -> Option<PathBuf> {
    let clean = url.split(['?', '#']).next()?.trim_start_matches('/');
    let mut path = root.to_path_buf();
    for part in Path::new(clean).components() {
        match part {
            Component::Normal(p) => path.push(p),
            _ => return None,
        }
    }
    if path.is_dir() || clean.is_empty() {
        path.push("index.html");
    }
    Some(path)
}

fn accepted(request: &Request) -> String {
    request
        .headers()
        .iter()
        .find(|h| h.field.equiv("Accept-Encoding"))
        .map(|h| h.value.as_str().to_string())
        .unwrap_or_default()
}

fn open(path: &Path, accept: &str) -> Option<(File, &'static str)> {
    for (token, ext) in [("br", "br"), ("gzip", "gz")] {
        if accept.contains(token) {
            let mut name = path.as_os_str().to_owned();
            name.push(format!(".{ext}"));
            if let Ok(file) = File::open(&name) {
                return Some((file, token));
            }
        }
    }
    File::open(path).ok().map(|file| (file, ""))
}

pub fn serve(root: &Path, files: Option<&Path>, request: Request) {
    let shared = files.zip(request.url().strip_prefix("/files/"));
    let path = shared.map_or_else(|| resolve(root, request.url()), |(dir, rest)| resolve(dir, rest));
    let found = path.as_ref().and_then(|p| open(p, &accepted(&request)).map(|f| (p, f)));
    let Some((path, (file, encoding))) = found else {
        let _ = request.respond(Response::from_string("not found").with_status_code(404));
        return;
    };
    let name = path.to_string_lossy();
    let cache = if shared.is_some() { "no-store" } else if name.ends_with(".html") { "no-cache" } else { "public, max-age=3600" };
    let mut response = Response::from_file(file)
        .with_header(header("Content-Type", mime::of(&name)))
        .with_header(header("Cache-Control", cache))
        .with_header(header("Vary", "Accept-Encoding"))
        .with_header(header("Cross-Origin-Opener-Policy", "same-origin"))
        .with_header(header("Cross-Origin-Embedder-Policy", "require-corp"))
        .with_header(header("Cross-Origin-Resource-Policy", "same-origin"));
    if !encoding.is_empty() {
        response = response.with_header(header("Content-Encoding", encoding));
    }
    let _ = request.respond(response);
}
