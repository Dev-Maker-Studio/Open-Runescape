mod packet_source;

use packet_source::{NativeDirection, SourcePacket};
use std::{env, fmt::Write, fs, path::PathBuf};

fn packet_size(size: i32) -> String {
    match size {
        -1 => "PacketSize::VarByte".into(),
        -2 => "PacketSize::VarShort".into(),
        value => format!("PacketSize::Fixed({value})"),
    }
}

fn generate(out: &mut String, entries: &[SourcePacket], direction: NativeDirection) {
    let (kind, table, sizes, direction) = match direction {
        NativeDirection::Client => (
            "ClientPacket",
            "CLIENT_PACKETS",
            "CLIENT_PACKET_SIZES",
            "ClientToServer",
        ),
        NativeDirection::Server => (
            "ServerPacket",
            "SERVER_PACKETS",
            "SERVER_PACKET_SIZES",
            "ServerToClient",
        ),
    };
    writeln!(out, "#[allow(non_camel_case_types)]\n#[derive(Clone, Copy, Debug, Eq, PartialEq, Hash)]\n#[repr(u16)]\npub enum {kind} {{").unwrap();
    for entry in entries {
        writeln!(out, "{} = {},", entry.name, entry.wire).unwrap();
    }
    writeln!(
        out,
        "}}\npub static {table}: [PacketMetadata<{kind}>; {}] = [",
        entries.len()
    )
    .unwrap();
    for entry in entries {
        let identity = if entry.name.starts_with("UNK_") {
            "UnsupportedUnknown"
        } else {
            "Named"
        };
        writeln!(out, "PacketMetadata {{ packet: {kind}::{}, name: {:?}, native_size: {}, size: {}, identity: PacketIdentity::{identity}, tick_processed: {:?} }},", entry.name, entry.name, entry.size, packet_size(entry.size), entry.tick_processed).unwrap();
    }
    let max_wire = entries.iter().map(|entry| entry.wire).max().unwrap();
    let mut lengths = vec!["None".to_owned(); usize::from(max_wire) + 1];
    for entry in entries {
        lengths[usize::from(entry.wire)] = format!("Some({})", packet_size(entry.size));
    }
    writeln!(
        out,
        "];\npub static {sizes}: [Option<PacketSize>; {}] = [{}];",
        lengths.len(),
        lengths.join(",")
    )
    .unwrap();
    writeln!(
        out,
        "impl {kind} {{\npub fn from_wire(wire: u16) -> Option<Self> {{\nSome(match wire {{"
    )
    .unwrap();
    for entry in entries {
        writeln!(out, "{} => Self::{},", entry.wire, entry.name).unwrap();
    }
    writeln!(out, "_ => return None,\n}})\n}}\npub const fn wire(self) -> u16 {{ self as u16 }}\npub fn metadata(self) -> &'static PacketMetadata<Self> {{\n&{table}[match self {{").unwrap();
    for (index, entry) in entries.iter().enumerate() {
        writeln!(out, "Self::{} => {index},", entry.name).unwrap();
    }
    writeln!(out, "}}]\n}}\n}}\nimpl sealed::Sealed for {kind} {{}}\nimpl GameOpcode for {kind} {{\nconst DIRECTION: Direction = Direction::{direction};\nfn from_wire(wire: u16) -> Option<Self> {{ Self::from_wire(wire) }}\nfn wire(self) -> u16 {{ self.wire() }}\nfn metadata(self) -> &'static PacketMetadata<Self> {{ self.metadata() }}\nfn sizes() -> &'static [Option<PacketSize>] {{ &{sizes} }}\n}}").unwrap();
}

fn main() {
    println!("cargo:rerun-if-changed=packet_source.rs");
    let base = PathBuf::from(env::var_os("CARGO_MANIFEST_DIR").unwrap())
        .join("../client/src/main/kotlin/com/jagex/game/runetek5/network/packet");
    let mut out = String::new();
    for (file, direction) in [
        ("ClientProt.kt", NativeDirection::Client),
        ("ServerProt.kt", NativeDirection::Server),
    ] {
        let source = base.join(file);
        println!("cargo:rerun-if-changed={}", source.display());
        let entries = packet_source::parse(
            &fs::read_to_string(&source).expect("read native Kotlin packet enum"),
            direction,
        )
        .expect("parse native Kotlin packet enum without drift");
        generate(&mut out, &entries, direction);
    }
    fs::write(
        PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("game_packets.rs"),
        out,
    )
    .expect("write generated game packet metadata");
}
