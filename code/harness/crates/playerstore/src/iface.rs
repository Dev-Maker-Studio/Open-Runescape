use crate::{edit, gameval, store};
use serde_json::{json, Value};

const USAGE: &str = "usage: openrs player iface <name> <command>
  list
  show <iface>
  open <iface> [slot|iface:comp]
  close <iface> [slot|iface:comp]
  text <iface> <comp> <text>
  hide <iface> <comp> <true|false>
  set <iface> <comp> item <obj> [amount]
  set <iface> <comp> model|graphic <id>
  set <iface> <comp> events <from> <to> <settings>";

fn arg<'a>(a: &'a [String], i: usize, what: &str) -> Result<&'a str, String> {
    a.get(i).map(String::as_str).ok_or_else(|| format!("missing {what}\n{USAGE}"))
}

fn int(a: &[String], i: usize, what: &str) -> Result<i64, String> {
    arg(a, i, what)?.parse().map_err(|_| format!("bad {what}"))
}

fn entries(v: &mut Value) -> &mut Vec<Value> {
    if !v["interfaces"]["open"].is_array() {
        edit::set_at(v, "interfaces.open", json!([]));
    }
    v["interfaces"]["open"].as_array_mut().unwrap()
}

fn find(v: &mut Value, id: i64) -> Option<&mut Value> {
    entries(v).iter_mut().find(|e| e["id"] == id)
}

fn parent(slot: Option<&String>) -> Result<Value, String> {
    match slot.map(String::as_str) {
        None => Ok(json!({"slot": "CENTRAL"})),
        Some(s) => match s.split_once(':') {
            Some((i, c)) => {
                let i = gameval::resolve("interface", i)?;
                Ok(json!({"parent": {"interface": i, "component": gameval::component(i, c)?}}))
            }
            None => Ok(json!({"slot": s.to_uppercase()})),
        },
    }
}

fn open(v: &mut Value, id: i64, slot: Option<&String>, shown: bool) -> Result<(), String> {
    let old = find(v, id).cloned();
    let mut e = old.clone().unwrap_or_else(|| json!({}));
    e["id"] = json!(id);
    e["name"] = json!(gameval::name("interface", &id.to_string()));
    if shown || old.is_none() {
        let o = e.as_object_mut().unwrap();
        for k in ["slot", "parent", "open"] {
            o.remove(k);
        }
        o.extend(parent(slot)?.as_object().unwrap().clone());
    }
    if !shown {
        e["open"] = json!(false);
    }
    let list = entries(v);
    list.retain(|x| x["id"] != id);
    list.push(e);
    Ok(())
}

fn component(v: &mut Value, a: &[String], f: impl FnOnce(&mut Value) -> Result<(), String>) -> Result<(), String> {
    let id = gameval::resolve("interface", arg(a, 0, "iface")?)?;
    let comp = gameval::component(id, arg(a, 1, "comp")?)?;
    let e = find(v, id).ok_or("interface is not in the file, run iface open first")?;
    let slot = &mut e["components"][comp.to_string()];
    if !slot.is_object() {
        *slot = json!({});
    }
    slot["name"] = json!(gameval::name("component", &format!("{id}:{comp}")));
    slot["pin"] = json!(true);
    f(slot)
}

fn show(v: &Value, id: i64) {
    let e = v["interfaces"]["open"].as_array().and_then(|l| l.iter().find(|e| e["id"] == id));
    println!("{}", e.map_or("not open".into(), |e| serde_json::to_string_pretty(e).unwrap()));
    for (c, n) in gameval::components(id) {
        println!("component {c} {n}");
    }
}

pub fn run(name: &str, a: &[String]) -> Result<(), String> {
    let cmd = arg(a, 0, "command")?;
    if matches!(cmd, "list" | "show") {
        let v = store::read(name)?;
        if cmd == "show" {
            show(&v, gameval::resolve("interface", arg(a, 1, "iface")?)?);
        } else {
            for e in v["interfaces"]["open"].as_array().into_iter().flatten() {
                let at = e["slot"].as_str().map(String::from).unwrap_or_else(|| format!("{}:{}", e["parent"]["interface"], e["parent"]["component"]));
                println!("{} {} {at}{}", e["id"], e["name"].as_str().unwrap_or(""), if e["open"] == false { " (closing)" } else { "" });
            }
        }
        return Ok(());
    }
    let mut v = store::read(name)?;
    let rest = &a[1..];
    match cmd {
        "open" | "close" => open(&mut v, gameval::resolve("interface", arg(a, 1, "iface")?)?, a.get(2), cmd == "open")?,
        "text" => component(&mut v, rest, |s| {
            s["text"] = json!(arg(a, 3, "text")?);
            Ok(())
        })?,
        "hide" => component(&mut v, rest, |s| {
            s["hidden"] = json!(arg(a, 3, "bool")? == "true");
            Ok(())
        })?,
        "set" => component(&mut v, rest, |s| {
            match arg(a, 3, "field")? {
                "item" => s["item"] = json!({"id": gameval::resolve("obj", arg(a, 4, "obj")?)?, "amount": int(a, 5, "amount").unwrap_or(1)}),
                f @ ("model" | "graphic") => s[f] = json!(int(a, 4, "id")?),
                "events" => s["events"] = json!({"from": int(a, 4, "from")?, "to": int(a, 5, "to")?, "settings": int(a, 6, "settings")?}),
                other => return Err(format!("unknown field {other}\n{USAGE}")),
            }
            Ok(())
        })?,
        other => return Err(format!("unknown iface command {other}\n{USAGE}")),
    }
    store::write(name, &v)
}
