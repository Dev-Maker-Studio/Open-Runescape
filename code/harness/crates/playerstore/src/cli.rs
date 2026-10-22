use crate::{edit, gameval, iface, store};
use serde_json::{json, Value};
use std::fs;

const USAGE: &str = "usage: openrs player <command>
  list
  show <name> [path]
  create <name> [x y plane]
  delete <name>
  set <name> <path> <json>
  pos <name> <x> <y> [plane]
  skill <name> <skill|all> <level> [xp]
  item <name> <inv|equip> <slot> <item> [amount]
  give <name> <inv|equip> <item> [amount]
  clear <name> <inv|equip> [slot]
  varp <name> <varp> <value>
  iface <name> <list|show|open|close|text|hide|set> ...
  import <name> <file>
  export <name> <file>
  account <name> <password>   (needs serve running)
  serve [N] | serve stop [I] | serve status";

fn num<T: std::str::FromStr>(a: Option<&String>, what: &str) -> Result<T, String> {
    a.ok_or_else(|| format!("missing {what}"))?.parse().map_err(|_| format!("bad {what}"))
}

fn arg<'a>(a: &'a [String], i: usize, what: &str) -> Result<&'a str, String> {
    a.get(i).map(String::as_str).ok_or_else(|| format!("missing {what}"))
}

fn edit_file(name: &str, f: impl FnOnce(&mut Value) -> Result<(), String>) -> Result<(), String> {
    let mut v = store::read(name)?;
    f(&mut v)?;
    store::write(name, &v)
}

pub fn summary(v: &Value) -> String {
    let get = |p| edit::get_at(v, p);
    let mut out = format!("name: {}\n", v["username"].as_str().unwrap_or("?"));
    if let Some(t) = get("tile") {
        out += &format!("position: {} {} {}\n", t["x"], t["y"], t["plane"]);
    }
    let lv = get("skills.level").and_then(Value::as_array);
    let xp = get("skills.xp").and_then(Value::as_array);
    for (i, s) in edit::SKILLS.iter().enumerate() {
        let l = lv.and_then(|a| a.get(i)).filter(|x| !x.is_null());
        if let Some(l) = l {
            out += &format!("skill {s}: {l} ({} xp)\n", xp.and_then(|a| a.get(i)).unwrap_or(&Value::Null));
        }
    }
    for (label, p) in [("inv", "inventory.items.data"), ("equip", "equipment.items.data")] {
        for (i, it) in get(p).and_then(Value::as_array).into_iter().flatten().enumerate() {
            if !it.is_null() {
                out += &format!("{label}[{i}]: {} x{}\n", it["id"], it["amount"]);
            }
        }
    }
    if let Some(m) = get("varManager.vars").and_then(Value::as_object) {
        out += &format!("varps: {}\n", m.len());
    }
    out
}

fn dispatch(a: &[String]) -> Result<(), String> {
    let name = || arg(a, 1, "name");
    match arg(a, 0, "command")? {
        "list" => store::list().iter().for_each(|n| println!("{n}")),
        "show" => {
            let v = store::read(name()?)?;
            match a.get(2) {
                Some(p) => println!("{}", serde_json::to_string_pretty(edit::get_at(&v, p).unwrap_or(&Value::Null)).unwrap()),
                None => print!("{}", summary(&v)),
            }
        }
        "create" => {
            let n = name()?;
            if store::path(n).exists() {
                return Err("player already exists".into());
            }
            let mut v = json!({"username": store::key(n)});
            let (x, y, p) = (num(a.get(2), "x").unwrap_or(3222), num(a.get(3), "y").unwrap_or(3218), num(a.get(4), "plane").unwrap_or(0));
            edit::set_position(&mut v, x, y, p);
            store::write(n, &v)?;
            if store::key(n).ends_with("_bot") {
                store::write_account(n)?;
            }
        }
        "delete" => fs::remove_file(store::path(name()?)).map_err(|e| e.to_string())?,
        "set" => edit_file(name()?, |v| {
            edit::set_at(v, arg(a, 2, "path")?, edit::parse_value(arg(a, 3, "json")?));
            Ok(())
        })?,
        "pos" => edit_file(name()?, |v| {
            edit::set_position(v, num(a.get(2), "x")?, num(a.get(3), "y")?, num(a.get(4), "plane").unwrap_or(0));
            Ok(())
        })?,
        "skill" => edit_file(name()?, |v| {
            let level: u32 = num(a.get(3), "level")?;
            let xp = a.get(4).and_then(|s| s.parse().ok());
            let which = arg(a, 2, "skill")?;
            let all: Vec<usize> = if which == "all" { (0..25).collect() } else { vec![edit::skill_index(which)?] };
            all.into_iter().for_each(|s| edit::set_skill(v, s, level, xp));
            Ok(())
        })?,
        "item" => edit_file(name()?, |v| {
            let (path, size) = edit::container(arg(a, 2, "container")?)?;
            let slot: usize = num(a.get(3), "slot")?;
            if slot >= size {
                return Err("slot out of range".into());
            }
            let id = gameval::resolve("obj", arg(a, 4, "item")?)?;
            edit::set_item(v, path, slot, id, num(a.get(5), "amount").unwrap_or(1));
            Ok(())
        })?,
        "give" => edit_file(name()?, |v| {
            let (path, size) = edit::container(arg(a, 2, "container")?)?;
            let id = gameval::resolve("obj", arg(a, 3, "item")?)?;
            let slot = edit::free_slot(v, path, size).ok_or("container full")?;
            edit::set_item(v, path, slot, id, num(a.get(4), "amount").unwrap_or(1));
            Ok(())
        })?,
        "clear" => edit_file(name()?, |v| {
            let (path, size) = edit::container(arg(a, 2, "container")?)?;
            let slots: Vec<usize> = match a.get(3) {
                Some(s) => vec![s.parse().map_err(|_| "bad slot")?],
                None => (0..size).collect(),
            };
            slots.into_iter().for_each(|s| edit::clear_slot(v, path, s));
            Ok(())
        })?,
        "varp" => edit_file(name()?, |v| {
            let id = gameval::resolve("varp", arg(a, 2, "varp")?)?;
            edit::set_varp(v, id, num(a.get(3), "value")?);
            Ok(())
        })?,
        "iface" => iface::run(name()?, &a[2..])?,
        "import" => {
            let text = fs::read_to_string(arg(a, 2, "file")?).map_err(|e| e.to_string())?;
            let v: Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
            store::write(name()?, &v)?;
            if store::key(name()?).ends_with("_bot") {
                store::write_account(name()?)?;
            }
        }
        "export" => store::write_to(std::path::Path::new(arg(a, 2, "file")?), &store::read(name()?)?)?,
        "account" => crate::launch::account(name()?, arg(a, 2, "password")?)?,
        "serve" => return crate::launch::serve(&a[1..]),
        other => return Err(format!("unknown command {other}\n{USAGE}")),
    }
    Ok(())
}

pub fn run(args: &[String]) -> i32 {
    match dispatch(args) {
        Ok(()) => 0,
        Err(e) => {
            eprintln!("player: {e}");
            2
        }
    }
}
