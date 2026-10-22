mod crc;
mod js5;
mod pack;

pub use pack::{build, fresh, packed};

use std::path::Path;

pub fn lobby(shared: bool) -> String {
    if shared {
        std::env::var("OPENRS_SHARED_LOBBY").unwrap_or("lobby.devmakerstudio.com:43594".into())
    } else {
        format!("127.0.0.1:{}", std::env::var("OPENRS_LOBBY_PORT").unwrap_or("24594".into()))
    }
}

fn revision(root: &Path) -> u32 {
    std::fs::read_to_string(root.join("assets/cache/cache.json")).ok().and_then(|s| parse_revision(&s)).unwrap_or(727)
}

fn parse_revision(s: &str) -> Option<u32> {
    s.split("\"revision\"").nth(1)?.trim_start_matches([':', ' ']).split(|c: char| !c.is_ascii_digit()).next()?.parse().ok()
}

fn digest(t: &[i32]) -> i32 {
    crc::crc32(&t.iter().flat_map(|c| c.to_be_bytes()).collect::<Vec<_>>())
}

pub fn diff(root: &Path, addr: &str) -> Result<Option<String>, String> {
    let local = crc::table(&pack::packed(root))?;
    let remote = js5::table(addr, revision(root))?;
    Ok(mismatch(&local, &remote))
}

fn mismatch(local: &[i32], remote: &[i32]) -> Option<String> {
    let ids: Vec<String> = (0..remote.len()).filter(|&i| local.get(i) != remote.get(i)).map(|i| i.to_string()).collect();
    (!ids.is_empty()).then(|| format!("cache mismatch: local {:08x} vs lobby {:08x} (indices {}), run openrs cache build", digest(local), digest(remote), ids.join(",")))
}

pub fn check(root: &Path, shared: bool) -> Result<(), String> {
    if !pack::fresh(root)? {
        return Err("cache mismatch: assets/cache/.packed is stale or missing, run openrs cache build".into());
    }
    let addr = lobby(shared);
    match diff(root, &addr) {
        Ok(None) => Ok(()),
        Ok(Some(m)) if shared => {
            println!("warning: {m}; the shared lobby serves a different cache build, the client logs in with the lobby CRC table");
            Ok(())
        }
        Ok(Some(m)) => Err(m),
        Err(e) if shared => {
            println!("warning: lobby version check skipped: {e}");
            Ok(())
        }
        Err(_) => Ok(()),
    }
}

pub fn run(args: &[String]) -> i32 {
    let root = match display::root() {
        Ok(r) => r,
        Err(e) => return fail(&e),
    };
    let shared = args.iter().any(|a| a == "shared");
    let result = match args.first().map(String::as_str) {
        Some("build") => pack::build(&root, args.iter().any(|a| a == "force")).map(|b| println!("{}", if b { "built assets/cache/.packed" } else { "up to date" })),
        Some("status") => pack::fresh(&root).and_then(|f| {
            let table = crc::table(&pack::packed(&root));
            println!("manifest {}", pack::manifest(&root.join("assets/cache"))?);
            println!("packed {} ({})", pack::packed(&root).display(), if f { "fresh" } else { "stale or missing" });
            println!("indices {}", table.as_ref().map_or(0, Vec::len));
            Ok(())
        }),
        Some("verify") => diff(&root, &lobby(shared)).and_then(|d| d.map_or_else(|| {
            println!("cache match with {}", lobby(shared));
            Ok(())
        }, Err)),
        _ => Err("usage: openrs cache build [force] | status | verify [shared]".into()),
    };
    result.map_or_else(|e| fail(&e), |_| 0)
}

fn fail(e: &str) -> i32 {
    eprintln!("cache: {e}");
    1
}

#[cfg(test)]
mod tests;
