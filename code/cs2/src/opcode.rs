#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct OpcodeMetadata {
    pub opcode: Opcode,
    pub name: &'static str,
    pub ordinal: u16,
    pub has_int_constant: bool,
    pub family: u8,
}

include!(concat!(env!("OUT_DIR"), "/opcodes.rs"));
