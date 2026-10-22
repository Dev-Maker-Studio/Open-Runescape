use std::collections::BTreeSet;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct SourceOpcode {
    pub name: String,
    pub wire: u16,
    pub wide: bool,
    pub family: u8,
}

pub fn parse(source: &str) -> Result<Vec<SourceOpcode>, String> {
    let (signature, body) = source
        .split_once(") {")
        .ok_or("missing opcode declaration")?;
    let signature: String = signature.chars().filter(|c| !c.is_whitespace()).collect();
    if !signature.ends_with("enumclassClientScriptOpCode(@JvmFieldvalopcode:Int,@JvmFieldvalhasIntConstant:Boolean=false,@JvmFieldvalfamilyId:Int=0,") {
        return Err("opcode signature or defaults changed".into());
    }
    let mut entries = Vec::new();
    let mut names = BTreeSet::new();
    let mut wires = BTreeSet::new();
    for line in body.lines().map(str::trim).filter(|s| !s.is_empty()) {
        let terminal = line.ends_with(';');
        let line = line
            .strip_suffix(if terminal { ';' } else { ',' })
            .ok_or_else(|| format!("unexpected opcode declaration: {line}"))?;
        let (name, args) = line.split_once('(').ok_or("missing opcode arguments")?;
        if name.is_empty()
            || !name.as_bytes()[0].is_ascii_uppercase()
            || !name
                .bytes()
                .all(|c| c.is_ascii_uppercase() || c.is_ascii_digit() || c == b'_')
        {
            return Err("invalid opcode name".into());
        }
        let args = args.strip_suffix(')').ok_or("unclosed opcode arguments")?;
        let args: Vec<_> = args.split(',').map(str::trim).collect();
        let wire: u16 = args[0].parse().map_err(|_| "invalid wire opcode")?;
        let mut wide = None;
        let mut family = None;
        for (index, arg) in args.iter().enumerate().skip(1) {
            if let Some((key, value)) = arg.split_once('=') {
                match key.trim() {
                    "hasIntConstant" if wide.is_none() => {
                        wide = Some(value.trim().parse().map_err(|_| "invalid operand flag")?);
                    }
                    "familyId" if family.is_none() => {
                        family = Some(value.trim().parse().map_err(|_| "invalid family")?);
                    }
                    _ => return Err("unexpected or duplicate opcode argument".into()),
                }
            } else {
                match index {
                    1 if wide.is_none() && family.is_none() => {
                        wide = Some(arg.parse().map_err(|_| "invalid operand flag")?);
                    }
                    2 if wide.is_some() && family.is_none() => {
                        family = Some(arg.parse().map_err(|_| "invalid family")?);
                    }
                    _ => return Err("unexpected positional opcode argument".into()),
                }
            }
        }
        if !names.insert(name.to_owned()) || !wires.insert(wire) {
            return Err("duplicate opcode name or wire value".into());
        }
        if entries.len() >= u16::MAX as usize {
            return Err("too many opcode entries".into());
        }
        entries.push(SourceOpcode {
            name: name.to_owned(),
            wire,
            wide: wide.unwrap_or(false),
            family: family.unwrap_or(0),
        });
        if terminal {
            return Ok(entries);
        }
    }
    Err("missing opcode declaration terminator".into())
}
