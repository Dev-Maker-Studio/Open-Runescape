mod checks;

use std::path::Path;
use std::time::Instant;

#[derive(Clone, Copy, PartialEq, Debug)]
pub enum Level {
    Pass,
    Warn,
    Fail,
}

pub struct Check {
    pub level: Level,
    pub name: String,
    pub msg: String,
    pub fix: String,
}

fn check(level: Level, name: impl Into<String>, msg: impl Into<String>, fix: &str) -> Check {
    Check { level, name: name.into(), msg: msg.into(), fix: fix.into() }
}

pub fn pass(name: impl Into<String>, msg: impl Into<String>) -> Check {
    check(Level::Pass, name, msg, "")
}

pub fn warn(name: impl Into<String>, msg: impl Into<String>, fix: &str) -> Check {
    check(Level::Warn, name, msg, fix)
}

pub fn fail(name: impl Into<String>, msg: impl Into<String>, fix: &str) -> Check {
    check(Level::Fail, name, msg, fix)
}

pub fn line(c: &Check) -> String {
    let tag = match c.level {
        Level::Pass => "PASS",
        Level::Warn => "WARN",
        Level::Fail => "FAIL",
    };
    let fix = if c.fix.is_empty() || c.level == Level::Pass { String::new() } else { format!("  fix: {}", c.fix) };
    format!("{tag} {:<10} {}{fix}", c.name, c.msg)
}

pub fn code(checks: &[Check]) -> i32 {
    checks.iter().any(|c| c.level == Level::Fail).into()
}

pub fn collect(root: &Path) -> Vec<Check> {
    std::thread::scope(|s| {
        let running: Vec<_> = checks::ALL.iter().map(|f| s.spawn(move || f(root))).collect();
        running.into_iter().flat_map(|h| h.join().unwrap_or_else(|_| vec![fail("doctor", "a check panicked", "")])).collect()
    })
}

pub fn run(_: &[String]) -> i32 {
    let t = Instant::now();
    let checks = display::root().map_or_else(|e| vec![fail("root", e, "run inside the project or set OPENRS_ROOT")], |r| collect(&r));
    checks.iter().for_each(|c| println!("{}", line(c)));
    let n = |l| checks.iter().filter(|c| c.level == l).count();
    println!("{} pass, {} warn, {} fail in {} ms", n(Level::Pass), n(Level::Warn), n(Level::Fail), t.elapsed().as_millis());
    code(&checks)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lines_show_fix_only_when_not_passing() {
        assert_eq!(line(&pass("jar", "fresh")), "PASS jar        fresh");
        assert_eq!(line(&warn("cache", "stale", "openrs cache build")), "WARN cache      stale  fix: openrs cache build");
        assert_eq!(line(&fail("gpu", "none", "")), "FAIL gpu        none");
    }

    #[test]
    fn exit_code_fails_only_on_fail() {
        assert_eq!(code(&[pass("a", ""), warn("b", "", "")]), 0);
        assert_eq!(code(&[pass("a", ""), fail("b", "", "")]), 1);
        assert_eq!(code(&[]), 0);
    }
}
