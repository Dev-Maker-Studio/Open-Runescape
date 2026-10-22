mod steps;

pub const GROUPS: [&str; 3] = ["all", "native", "web"];

use serde_json::{json, Value};
use std::time::Instant;

pub type Step<C> = (&'static str, fn(&mut C) -> Result<String, String>);

pub struct Outcome {
    pub step: &'static str,
    pub ok: bool,
    pub ms: u128,
    pub detail: String,
}

pub fn execute<C>(steps: &[Step<C>], ctx: &mut C, mut seen: impl FnMut(usize, &Outcome)) -> Vec<Outcome> {
    let mut out = Vec::with_capacity(steps.len());
    for (i, (step, f)) in steps.iter().enumerate() {
        let t = Instant::now();
        let r = f(ctx);
        let o = Outcome { step, ok: r.is_ok(), ms: t.elapsed().as_millis(), detail: r.unwrap_or_else(|e| e) };
        seen(i, &o);
        let stop = !o.ok;
        out.push(o);
        if stop {
            break;
        }
    }
    out
}

pub fn progress(i: usize, total: usize, o: &Outcome) -> String {
    format!("[{}/{total}] {} {:<20} {:>7} ms  {}", i + 1, if o.ok { "ok  " } else { "FAIL" }, o.step, o.ms, o.detail.lines().next().unwrap_or(""))
}

pub fn report(names: &[&str], out: &[Outcome]) -> String {
    let secs = out.iter().map(|o| o.ms).sum::<u128>() as f64 / 1000.0;
    let mut text = match out.iter().position(|o| !o.ok) {
        Some(i) => format!("verify FAIL at step {} of {}: {} after {secs:.1}s\n{}\n", i + 1, names.len(), out[i].step, out[i].detail.lines().map(|l| format!("  {l}")).collect::<Vec<_>>().join("\n")),
        None if out.len() == names.len() => format!("verify PASS, {} steps in {secs:.1}s\n", names.len()),
        None => format!("verify INCOMPLETE, {} of {} steps in {secs:.1}s\n", out.len(), names.len()),
    };
    for (i, name) in names.iter().enumerate() {
        text += &match out.get(i) {
            Some(o) => format!("  {} {name:<20} {:>7} ms\n", if o.ok { "ok  " } else { "FAIL" }, o.ms),
            None => format!("  skip {name}\n"),
        };
    }
    text
}

pub fn summary(names: &[&str], out: &[Outcome], metrics: Value) -> Value {
    let failed = out.iter().find(|o| !o.ok).map(|o| o.step);
    let steps: Vec<Value> = names
        .iter()
        .enumerate()
        .map(|(i, n)| match out.get(i) {
            Some(o) => json!({"step": n, "status": if o.ok { "ok" } else { "fail" }, "ms": o.ms as u64, "detail": o.detail}),
            None => json!({"step": n, "status": "skipped"}),
        })
        .collect();
    json!({"ok": failed.is_none() && out.len() == names.len(), "failed": failed, "total_ms": out.iter().map(|o| o.ms as u64).sum::<u64>(), "metrics": metrics, "steps": steps})
}

pub fn plan<C>(native: &[Step<C>], equiv: &[Step<C>], web: &[Step<C>], group: &str) -> Result<Vec<Step<C>>, String> {
    let before = |name: &str| native.iter().position(|s| s.0 == name).unwrap_or(native.len());
    match group {
        "web" => Ok(web.to_vec()),
        "native" => Ok(native.to_vec()),
        "all" => {
            let (e, d) = (before("equiv") + 1, before("down-all"));
            Ok([&native[..e.min(d)], equiv, &native[e.min(d)..d], web, &native[d..]].concat())
        }
        g => Err(format!("unknown group {g}, groups: {}", GROUPS.join(" "))),
    }
}

fn group_of(args: &[String]) -> Option<&str> {
    args.iter().position(|a| a == "--group").map(|i| args.get(i + 1).map_or("", String::as_str))
}

pub fn run(args: &[String]) -> i32 {
    let all = match plan(steps::ALL, steps::EQUIV, steps::WEB, group_of(args).unwrap_or("all")) {
        Ok(a) => a,
        Err(e) => {
            eprintln!("usage: openrs verify [list] [--group all|native|web] [--from <step>]\n{e}");
            return 2;
        }
    };
    if args.first().is_some_and(|a| a == "list") {
        all.iter().for_each(|s| println!("{}", s.0));
        return 0;
    }
    let start = match args.iter().position(|a| a == "--from").map(|i| args.get(i + 1).and_then(|n| all.iter().position(|s| s.0 == n))) {
        None => 0,
        Some(Some(i)) => i,
        Some(None) => {
            eprintln!("usage: openrs verify [list] [--group all|native|web] [--from <step>]");
            return 2;
        }
    };
    let mut live = match steps::Live::new() {
        Ok(l) => l,
        Err(e) => {
            eprintln!("verify: {e}");
            return 1;
        }
    };
    let names: Vec<&str> = all[start..].iter().map(|s| s.0).collect();
    let out = execute(&all[start..], &mut live, |i, o| println!("{}", progress(i, names.len(), o)));
    print!("{}", report(&names, &out));
    let file = live.out.join("summary.json");
    let doc = summary(&names, &out, Value::Object(std::mem::take(&mut live.metrics)));
    match std::fs::write(&file, serde_json::to_string_pretty(&doc).unwrap_or_default()) {
        Ok(()) => println!("summary: {}", file.display()),
        Err(e) => eprintln!("summary not written: {e}"),
    }
    i32::from(doc["ok"] != true)
}

#[cfg(test)]
mod tests {
    use super::*;

    struct Fake {
        calls: Vec<&'static str>,
    }

    fn good(f: &mut Fake) -> Result<String, String> {
        f.calls.push("good");
        Ok("fine\nmore".into())
    }

    fn bad(f: &mut Fake) -> Result<String, String> {
        f.calls.push("bad");
        Err("broke here\nsecond line".into())
    }

    const STEPS: [Step<Fake>; 4] = [("one", good), ("two", bad), ("three", good), ("four", good)];

    fn names() -> Vec<&'static str> {
        STEPS.iter().map(|s| s.0).collect()
    }

    #[test]
    fn runner_stops_at_first_failure() {
        let mut f = Fake { calls: vec![] };
        let mut seen = vec![];
        let out = execute(&STEPS, &mut f, |i, o| seen.push((i, o.ok)));
        assert_eq!(f.calls, ["good", "bad"]);
        assert_eq!(seen, [(0, true), (1, false)]);
        assert_eq!(out[1].detail, "broke here\nsecond line");
    }

    #[test]
    fn runner_runs_everything_when_green() {
        let mut f = Fake { calls: vec![] };
        let out = execute(&[STEPS[0], STEPS[2]], &mut f, |_, _| {});
        assert_eq!(out.len(), 2);
        assert!(out.iter().all(|o| o.ok));
    }

    #[test]
    fn report_names_the_failed_step_and_skips() {
        let out = execute(&STEPS, &mut Fake { calls: vec![] }, |_, _| {});
        let r = report(&names(), &out);
        assert!(r.starts_with("verify FAIL at step 2 of 4: two after"));
        assert!(r.contains("\n  broke here\n  second line\n"));
        assert!(r.contains("  skip three\n  skip four\n"));
        assert_eq!(r.lines().filter(|l| l.contains("FAIL two")).count(), 1);
    }

    #[test]
    fn report_pass_and_progress_lines() {
        let out = execute(&[STEPS[0]], &mut Fake { calls: vec![] }, |_, _| {});
        assert!(report(&["one"], &out).starts_with("verify PASS, 1 steps in"));
        assert!(progress(0, 4, &out[0]).starts_with("[1/4] ok   one "));
        assert!(progress(0, 4, &out[0]).ends_with("ms  fine"));
    }

    #[test]
    fn summary_json_marks_fail_and_skipped() {
        let out = execute(&STEPS, &mut Fake { calls: vec![] }, |_, _| {});
        let s = summary(&names(), &out, json!({"time_to_world_ms": 9000}));
        assert_eq!(s["ok"], false);
        assert_eq!(s["failed"], "two");
        assert_eq!(s["steps"][1]["status"], "fail");
        assert_eq!(s["steps"][3]["status"], "skipped");
        assert_eq!(s["metrics"]["time_to_world_ms"], 9000);
        let green = execute(&[STEPS[0]], &mut Fake { calls: vec![] }, |_, _| {});
        assert_eq!(summary(&["one"], &green, json!({}))["ok"], true);
    }

    #[test]
    fn groups_select_and_order_steps() {
        let one: [Step<Fake>; 5] = [("up", good), ("equiv", good), ("fleet-up", good), ("down-all", good), ("never", good)];
        let eq: [Step<Fake>; 1] = [("equiv-browser", good)];
        let web: [Step<Fake>; 2] = [("web-up", good), ("web-down", good)];
        let names = |g: &str| plan(&one[..4], &eq, &web, g).unwrap().iter().map(|s| s.0).collect::<Vec<_>>();
        assert_eq!(names("web"), ["web-up", "web-down"]);
        assert_eq!(names("native"), ["up", "equiv", "fleet-up", "down-all"]);
        assert_eq!(names("all"), ["up", "equiv", "equiv-browser", "fleet-up", "web-up", "web-down", "down-all"]);
        assert!(plan(&one[..4], &eq, &web, "mobile").unwrap_err().contains("groups: all native web"));
        let bare: Vec<&str> = plan(&one[..1], &eq, &web, "all").unwrap().iter().map(|s| s.0).collect();
        assert_eq!(bare, ["up", "equiv-browser", "web-up", "web-down"]);
        let args: Vec<String> = ["--group", "web", "--from", "web-down"].iter().map(|s| s.to_string()).collect();
        assert_eq!(group_of(&args), Some("web"));
        assert_eq!(group_of(&[]), None);
    }

    #[test]
    fn the_web_group_touches_no_native_step() {
        let web: Vec<&str> = steps::WEB.iter().map(|s| s.0).collect();
        let native: Vec<&str> = steps::ALL.iter().map(|s| s.0).collect();
        assert!(web.iter().all(|w| !native.contains(w)));
        assert!(!web.iter().any(|w| ["doctor", "down-before", "up", "down", "down-all", "reset-player"].contains(w)));
        let full: Vec<&str> = plan(steps::ALL, steps::EQUIV, steps::WEB, "all").unwrap().iter().map(|s| s.0).collect();
        assert_eq!(full.len(), native.len() + web.len() + steps::EQUIV.len());
        assert!(full.iter().position(|s| *s == "web-down") < full.iter().position(|s| *s == "down-all"));
        assert!(full.iter().position(|s| *s == "equiv") < full.iter().position(|s| *s == "equiv-browser"));
    }

    #[test]
    fn every_live_step_is_unique() {
        let mut n: Vec<&str> = plan(steps::ALL, steps::EQUIV, steps::WEB, "all").unwrap().iter().map(|s| s.0).collect();
        let len = n.len();
        n.sort();
        n.dedup();
        assert_eq!(n.len(), len);
    }
}
