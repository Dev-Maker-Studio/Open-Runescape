use std::collections::BTreeSet;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum NativeDirection {
    Client,
    Server,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct SourcePacket {
    pub name: String,
    pub wire: u16,
    pub size: i32,
    pub tick_processed: Option<bool>,
}

pub fn parse(source: &str, direction: NativeDirection) -> Result<Vec<SourcePacket>, String> {
    let (signature, body) = source
        .split_once(") {")
        .ok_or("missing packet enum declaration")?;
    let signature: String = signature.chars().filter(|c| !c.is_whitespace()).collect();
    let expected = match direction {
        NativeDirection::Client => "packagecom.jagex.game.runetek5.network.packetenumclassClientProt(@JvmFieldvalid:Int,@JvmFieldvalsize:Int,@JvmFieldvaltickProcessed:Boolean=false",
        NativeDirection::Server => "packagecom.jagex.game.runetek5.network.packetenumclassServerProt(valopcode:Int,valsize:Int",
    };
    if signature != expected {
        return Err("packet enum signature or defaults changed".into());
    }
    let mut entries = Vec::new();
    let mut names = BTreeSet::new();
    let mut wires = BTreeSet::new();
    for line in body.lines().map(str::trim).filter(|s| !s.is_empty()) {
        let terminal = line.ends_with(';');
        let line = line
            .strip_suffix(if terminal { ';' } else { ',' })
            .ok_or_else(|| format!("unexpected packet declaration: {line}"))?;
        let (name, args) = line.split_once('(').ok_or("missing packet arguments")?;
        if name.is_empty()
            || !name.as_bytes()[0].is_ascii_uppercase()
            || !name
                .bytes()
                .all(|c| c.is_ascii_uppercase() || c.is_ascii_digit() || c == b'_')
        {
            return Err("invalid packet name".into());
        }
        let args = args.strip_suffix(')').ok_or("unclosed packet arguments")?;
        let args: Vec<_> = args.split(',').map(str::trim).collect();
        let allowed_count = match direction {
            NativeDirection::Client => 2..=3,
            NativeDirection::Server => 2..=2,
        };
        if !allowed_count.contains(&args.len()) {
            return Err("unexpected packet argument count".into());
        }
        let wire: u16 = args[0].parse().map_err(|_| "invalid wire opcode")?;
        let max_wire = match direction {
            NativeDirection::Client => u8::MAX.into(),
            NativeDirection::Server => 32767,
        };
        if wire > max_wire {
            return Err("packet opcode exceeds native direction's opcode range".into());
        }
        let size: i32 = args[1].parse().map_err(|_| "invalid packet size")?;
        if size < -2 {
            return Err("unsupported native packet size".into());
        }
        let tick_processed = match direction {
            NativeDirection::Client => Some(
                args.get(2)
                    .copied()
                    .unwrap_or("false")
                    .parse()
                    .map_err(|_| "invalid tick processing flag")?,
            ),
            NativeDirection::Server => None,
        };
        if !names.insert(name.to_owned()) || !wires.insert(wire) {
            return Err("duplicate packet name or wire opcode".into());
        }
        entries.push(SourcePacket {
            name: name.to_owned(),
            wire,
            size,
            tick_processed,
        });
        if terminal {
            return Ok(entries);
        }
    }
    Err("missing packet enum terminator".into())
}
