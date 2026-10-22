use crate::{keys, page::Page};
use serde_json::{json, Value};
use std::time::Duration;

pub const COMMANDS: [&str; 8] = ["key", "type", "click", "dblclick", "move", "drag", "scroll", "sleep"];

pub struct Rect {
    left: f64,
    top: f64,
    width: f64,
    height: f64,
    cw: f64,
    ch: f64,
}

impl Rect {
    pub fn from(v: &Value) -> Result<Rect, String> {
        let f = |k: &str| v[k].as_f64().filter(|n| *n > 0.0 || k == "left" || k == "top").ok_or(format!("bad canvas rectangle {v}"));
        Ok(Rect { left: f("left")?, top: f("top")?, width: f("width")?, height: f("height")?, cw: f("cw")?, ch: f("ch")? })
    }

    pub fn css(&self, x: f64, y: f64) -> (f64, f64) {
        (self.left + x * self.width / self.cw, self.top + y * self.height / self.ch)
    }
}

#[derive(Debug, PartialEq)]
pub enum Step {
    Send(&'static str, Value),
    Pause(u64),
}

fn num(args: &[String], i: usize, usage: &str) -> Result<f64, String> {
    args.get(i).and_then(|a| a.parse().ok()).ok_or(format!("usage: {usage}"))
}

fn button(args: &[String], i: usize) -> Result<(&'static str, u32), String> {
    match args.get(i).map(String::as_str) {
        None | Some("left") => Ok(("left", 1)),
        Some("right") => Ok(("right", 2)),
        Some("middle") => Ok(("middle", 4)),
        Some(b) => Err(format!("unknown button {b}, buttons: left middle right")),
    }
}

fn mouse(kind: &str, (x, y): (f64, f64), b: (&str, u32), held: u32, clicks: u32) -> Step {
    Step::Send("Input.dispatchMouseEvent", json!({ "type": kind, "x": x, "y": y, "button": b.0, "buttons": held, "clickCount": clicks }))
}

fn hover(p: (f64, f64)) -> Step {
    mouse("mouseMoved", p, ("none", 0), 0, 0)
}

fn key(kind: &str, code: &str, mods: u32) -> Result<Step, String> {
    let k = keys::lookup(code, mods).ok_or(format!("unknown key code {code}"))?;
    let mut p = json!({ "type": if kind == "keyDown" && k.text.is_none() { "rawKeyDown" } else { kind }, "key": k.key, "code": code, "windowsVirtualKeyCode": k.vk, "nativeVirtualKeyCode": k.vk, "modifiers": mods });
    if let (Some(t), "keyDown") = (&k.text, kind) {
        p["text"] = json!(t);
        p["unmodifiedText"] = json!(t);
    }
    Ok(Step::Send("Input.dispatchKeyEvent", p))
}

fn keys_plan(args: &[String]) -> Result<Vec<Step>, String> {
    let usage = "key <down|up|press> <code>";
    let (action, spec) = (args.first().ok_or(usage)?, args.get(1).ok_or(usage)?);
    let (code, mods) = keys::parse(spec)?;
    match action.as_str() {
        "down" => Ok(vec![key("keyDown", &code, mods)?]),
        "up" => Ok(vec![key("keyUp", &code, mods)?]),
        "press" => Ok(vec![key("keyDown", &code, mods)?, key("keyUp", &code, mods)?]),
        a => Err(format!("unknown key action {a}, {usage}")),
    }
}

fn type_plan(args: &[String]) -> Result<Vec<Step>, String> {
    if args.is_empty() {
        return Err("usage: type <text>".into());
    }
    let mut out = vec![];
    for c in args.join(" ").chars() {
        match keys::code_of(c) {
            Some((code, mods)) => {
                out.push(key("keyDown", &code, mods)?);
                out.push(key("keyUp", &code, mods)?);
            }
            None => out.push(Step::Send("Input.insertText", json!({ "text": c.to_string() }))),
        }
    }
    Ok(out)
}

pub fn plan(cmd: &str, args: &[String], rect: &Rect) -> Result<Vec<Step>, String> {
    let at = |i: usize, usage: &str| Ok::<_, String>(rect.css(num(args, i, usage)?, num(args, i + 1, usage)?));
    match cmd {
        "key" => keys_plan(args),
        "type" => type_plan(args),
        "sleep" => Ok(vec![Step::Pause(num(args, 0, "sleep <ms>")? as u64)]),
        "move" => Ok(vec![hover(at(0, "move <x> <y>")?)]),
        "click" | "dblclick" => {
            let p = at(0, &format!("{cmd} <x> <y> [left|middle|right]"))?;
            let b = button(args, 2)?;
            let rounds = if cmd == "click" { 1 } else { 2 };
            let mut out = vec![hover(p)];
            for n in 1..=rounds {
                out.push(mouse("mousePressed", p, b, b.1, n));
                out.push(mouse("mouseReleased", p, b, 0, n));
            }
            Ok(out)
        }
        "drag" => {
            let usage = "drag <x1> <y1> <x2> <y2> [left|middle|right]";
            let (from, to, b) = (at(0, usage)?, at(2, usage)?, button(args, 4)?);
            let mut out = vec![hover(from), mouse("mousePressed", from, b, b.1, 1)];
            for i in 1..=8 {
                let t = f64::from(i) / 8.0;
                out.push(mouse("mouseMoved", (from.0 + (to.0 - from.0) * t, from.1 + (to.1 - from.1) * t), ("none", 0), b.1, 0));
                out.push(Step::Pause(16));
            }
            out.push(mouse("mouseReleased", to, b, 0, 1));
            Ok(out)
        }
        "scroll" => {
            let usage = "scroll <dx> <dy> [x y]";
            let (dx, dy) = (num(args, 0, usage)?, num(args, 1, usage)?);
            let p = if args.len() > 2 { at(2, usage)? } else { rect.css(rect.cw / 2.0, rect.ch / 2.0) };
            Ok(vec![hover(p), Step::Send("Input.dispatchMouseEvent", json!({ "type": "mouseWheel", "x": p.0, "y": p.1, "deltaX": dx, "deltaY": dy }))])
        }
        _ => Err(format!("unknown input command {cmd}")),
    }
}

fn delivered(v: &Value) -> u64 {
    v["counts"].as_object().map_or(0, |m| m.values().filter_map(Value::as_u64).sum())
}

pub fn exec(page: &mut Page, cmd: &str, args: &[String]) -> Result<Value, String> {
    let rect = Rect::from(&page.run("rect", &[])?)?;
    let steps = plan(cmd, args, &rect)?;
    let before = delivered(&page.run("input", &[])?);
    for s in &steps {
        match s {
            Step::Send(method, params) => drop(page.call(method, params.clone())?),
            Step::Pause(ms) => std::thread::sleep(Duration::from_millis(*ms)),
        }
    }
    let seen = delivered(&page.run("input", &[])?).saturating_sub(before);
    if !matches!(cmd, "sleep" | "move") && seen == 0 {
        return Err(format!("the page received no input events from {cmd}"));
    }
    Ok(json!(format!("{cmd} {} sent {} delivered {seen} frame {}", args.join(" "), steps.iter().filter(|s| matches!(s, Step::Send(..))).count(), page.frames)))
}
