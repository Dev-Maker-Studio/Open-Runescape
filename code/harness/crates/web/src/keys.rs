pub struct Key {
    pub key: String,
    pub vk: u32,
    pub text: Option<String>,
}

const NAMED: [(&str, &str, u32); 27] = [
    ("Enter", "Enter", 13),
    ("Tab", "Tab", 9),
    ("Backspace", "Backspace", 8),
    ("Escape", "Escape", 27),
    ("Delete", "Delete", 46),
    ("Insert", "Insert", 45),
    ("Home", "Home", 36),
    ("End", "End", 35),
    ("PageUp", "PageUp", 33),
    ("PageDown", "PageDown", 34),
    ("ArrowLeft", "ArrowLeft", 37),
    ("ArrowUp", "ArrowUp", 38),
    ("ArrowRight", "ArrowRight", 39),
    ("ArrowDown", "ArrowDown", 40),
    ("ShiftLeft", "Shift", 16),
    ("ShiftRight", "Shift", 16),
    ("ControlLeft", "Control", 17),
    ("ControlRight", "Control", 17),
    ("AltLeft", "Alt", 18),
    ("AltRight", "Alt", 18),
    ("MetaLeft", "Meta", 91),
    ("MetaRight", "Meta", 92),
    ("CapsLock", "CapsLock", 20),
    ("ContextMenu", "ContextMenu", 93),
    ("NumLock", "NumLock", 144),
    ("PrintScreen", "PrintScreen", 44),
    ("Pause", "Pause", 19),
];

const PUNCT: [(&str, char, u32); 12] = [
    ("Space", ' ', 32),
    ("Minus", '-', 189),
    ("Equal", '=', 187),
    ("Comma", ',', 188),
    ("Period", '.', 190),
    ("Slash", '/', 191),
    ("Semicolon", ';', 186),
    ("Quote", '\'', 222),
    ("BracketLeft", '[', 219),
    ("BracketRight", ']', 221),
    ("Backslash", '\\', 220),
    ("Backquote", '`', 192),
];

pub const SHIFT: u32 = 8;

pub fn modifier(name: &str) -> Option<u32> {
    match name.to_ascii_lowercase().as_str() {
        "alt" => Some(1),
        "ctrl" | "control" => Some(2),
        "meta" | "cmd" => Some(4),
        "shift" => Some(SHIFT),
        _ => None,
    }
}

pub fn parse(spec: &str) -> Result<(String, u32), String> {
    let mut parts: Vec<&str> = spec.split('+').collect();
    let code = parts.pop().filter(|c| !c.is_empty()).ok_or("empty key code")?;
    let mods = parts.iter().try_fold(0, |m, p| modifier(p).map(|b| m | b).ok_or(format!("unknown modifier {p}")))?;
    Ok((code.to_string(), mods))
}

pub fn lookup(code: &str, mods: u32) -> Option<Key> {
    let shift = mods & SHIFT != 0;
    let printable = |c: char, vk: u32| {
        let c = if shift { c.to_ascii_uppercase() } else { c };
        Some(Key { key: c.to_string(), vk, text: (mods & 6 == 0).then(|| c.to_string()) })
    };
    if let Some(l) = code.strip_prefix("Key").and_then(|k| k.chars().next()).filter(|c| code.len() == 4 && c.is_ascii_uppercase()) {
        return printable(l.to_ascii_lowercase(), l as u32);
    }
    if let Some(d) = code.strip_prefix("Digit").and_then(|k| k.chars().next()).filter(|c| code.len() == 6 && c.is_ascii_digit()) {
        return Some(Key { key: d.to_string(), vk: d as u32, text: (mods & 6 == 0).then(|| d.to_string()) });
    }
    if let Some(n) = code.strip_prefix('F').and_then(|n| n.parse::<u32>().ok()).filter(|n| (1..=12).contains(n)) {
        return Some(Key { key: code.to_string(), vk: 111 + n, text: None });
    }
    if let Some(&(_, c, vk)) = PUNCT.iter().find(|p| p.0 == code) {
        return Some(Key { key: c.to_string(), vk, text: (mods & 6 == 0).then(|| c.to_string()) });
    }
    NAMED.iter().find(|n| n.0 == code).map(|&(_, key, vk)| Key { key: key.into(), vk, text: (code == "Enter").then(|| "\r".to_string()) })
}

pub fn code_of(c: char) -> Option<(String, u32)> {
    match c {
        'a'..='z' => Some((format!("Key{}", c.to_ascii_uppercase()), 0)),
        'A'..='Z' => Some((format!("Key{c}"), SHIFT)),
        '0'..='9' => Some((format!("Digit{c}"), 0)),
        c => PUNCT.iter().find(|p| p.1 == c).map(|p| (p.0.to_string(), 0)),
    }
}
