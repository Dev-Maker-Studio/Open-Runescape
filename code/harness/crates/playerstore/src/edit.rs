use serde_json::{json, Value};

pub const SKILLS: [&str; 25] = [
    "attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking",
    "woodcutting", "fletching", "fishing", "firemaking", "crafting", "smithing", "mining",
    "herblore", "agility", "thieving", "slayer", "farming", "runecrafting", "hunter",
    "construction", "summoning", "dungeoneering",
];

pub fn skill_index(name: &str) -> Result<usize, String> {
    let n = if name == "defense" { "defence" } else if name == "range" { "ranged" } else { name };
    SKILLS.iter().position(|s| *s == n).or_else(|| n.parse().ok().filter(|i| *i < 25)).ok_or_else(|| format!("unknown skill {name}"))
}

pub fn xp_for_level(level: u32) -> f64 {
    let mut points = 0.0;
    let mut out = 0.0;
    for lvl in 1..level {
        points += (lvl as f64 + 300.0 * 2f64.powf(lvl as f64 / 7.0)).floor();
        out = (points / 4.0).floor();
    }
    out
}

pub fn set_at(v: &mut Value, path: &str, val: Value) {
    let mut cur = v;
    let parts: Vec<&str> = path.split('.').collect();
    for (i, p) in parts.iter().enumerate() {
        if let Ok(idx) = p.parse::<usize>() {
            if !cur.is_array() {
                *cur = json!([]);
            }
            let a = cur.as_array_mut().unwrap();
            while a.len() <= idx {
                a.push(Value::Null);
            }
            if i + 1 == parts.len() {
                a[idx] = val;
                return;
            }
            cur = &mut a[idx];
        } else {
            if !cur.is_object() {
                *cur = json!({});
            }
            let o = cur.as_object_mut().unwrap();
            if i + 1 == parts.len() {
                o.insert((*p).into(), val);
                return;
            }
            cur = o.entry(*p).or_insert(Value::Null);
        }
    }
}

pub fn get_at<'a>(v: &'a Value, path: &str) -> Option<&'a Value> {
    path.split('.').try_fold(v, |c, p| match p.parse::<usize>() {
        Ok(i) if c.is_array() => c.get(i),
        _ => c.get(p),
    })
}

pub fn parse_value(s: &str) -> Value {
    serde_json::from_str(s).unwrap_or_else(|_| Value::String(s.into()))
}

pub fn set_position(v: &mut Value, x: i64, y: i64, plane: i64) {
    set_at(v, "tile", json!({"x": x, "y": y, "plane": plane}));
}

pub fn set_skill(v: &mut Value, skill: usize, level: u32, xp: Option<f64>) {
    set_at(v, &format!("skills.level.{skill}"), json!(level));
    set_at(v, &format!("skills.xp.{skill}"), json!(xp.unwrap_or_else(|| xp_for_level(level))));
}

pub fn container(name: &str) -> Result<(&'static str, usize), String> {
    match name {
        "inv" | "inventory" => Ok(("inventory.items.data", 28)),
        "equip" | "equipment" => Ok(("equipment.items.data", 15)),
        _ => Err(format!("unknown container {name} (inv, equip)")),
    }
}

pub fn set_item(v: &mut Value, path: &str, slot: usize, id: i64, amount: i64) {
    set_at(v, &format!("{path}.{slot}"), json!({"id": id, "amount": amount, "slot": slot}));
}

pub fn clear_slot(v: &mut Value, path: &str, slot: usize) {
    set_at(v, &format!("{path}.{slot}"), Value::Null);
}

pub fn free_slot(v: &Value, path: &str, size: usize) -> Option<usize> {
    let a = get_at(v, path).and_then(Value::as_array);
    (0..size).find(|i| a.and_then(|a| a.get(*i)).is_none_or(Value::is_null))
}

pub fn set_varp(v: &mut Value, id: i64, val: i64) {
    set_at(v, "varManager.vars", {
        let mut vars = get_at(v, "varManager.vars").cloned().unwrap_or(json!({}));
        vars[id.to_string()] = json!(val);
        vars
    });
}
