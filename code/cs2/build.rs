mod opcode_source;

use std::{env, fmt::Write, fs, path::PathBuf};

fn main() {
    let source = PathBuf::from(env::var_os("CARGO_MANIFEST_DIR").unwrap()).join(
        "../client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
    );
    println!("cargo:rerun-if-changed={}", source.display());
    println!("cargo:rerun-if-changed=opcode_source.rs");
    let entries =
        opcode_source::parse(&fs::read_to_string(&source).expect("read Kotlin opcode source"))
            .expect("parse Kotlin opcode source without drift");
    let mut out = String::from("#[allow(non_camel_case_types)]\n#[derive(Clone, Copy, Debug, Eq, PartialEq, Hash)]\n#[repr(u16)]\npub enum Opcode {\n");
    for entry in &entries {
        writeln!(out, "{} = {},", entry.name, entry.wire).unwrap();
    }
    writeln!(
        out,
        "}}\npub static OPCODES: [OpcodeMetadata; {}] = [",
        entries.len()
    )
    .unwrap();
    for (ordinal, entry) in entries.iter().enumerate() {
        writeln!(out, "OpcodeMetadata {{ opcode: Opcode::{}, name: {:?}, ordinal: {}, has_int_constant: {}, family: {} }},", entry.name, entry.name, ordinal, entry.wide, entry.family).unwrap();
    }
    out.push_str(
        "];\nimpl Opcode {\npub fn from_wire(wire: u16) -> Option<Self> {\nSome(match wire {\n",
    );
    for entry in &entries {
        writeln!(out, "{} => Self::{},", entry.wire, entry.name).unwrap();
    }
    out.push_str("_ => return None,\n})\n}\npub const fn wire(self) -> u16 { self as u16 }\npub fn metadata(self) -> &'static OpcodeMetadata {\n&OPCODES[match self {\n");
    for (ordinal, entry) in entries.iter().enumerate() {
        writeln!(out, "Self::{} => {},", entry.name, ordinal).unwrap();
    }
    out.push_str("}]\n}\n}\n");
    fs::write(
        PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("opcodes.rs"),
        out,
    )
    .expect("write generated Rust opcode metadata");
}
