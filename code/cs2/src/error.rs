use crate::{HostError, Opcode};
use std::fmt;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct DecodeError {
    pub offset: usize,
    pub kind: DecodeErrorKind,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum DecodeErrorKind {
    InputLimit,
    Truncated,
    Footer,
    InstructionCount,
    UnknownOpcode(u16),
    StringLimit,
    ArgumentsExceedLocals,
    Allocation,
    TrailingData,
}

impl fmt::Display for DecodeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "CS2 decode at byte {}: {:?}", self.offset, self.kind)
    }
}

impl std::error::Error for DecodeError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum StackKind {
    Int,
    String,
    Long,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct VmError {
    pub script_id: i32,
    pub instruction: usize,
    pub kind: VmErrorKind,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum VmErrorKind {
    MissingScript(i32),
    UnsupportedOpcode(Opcode),
    Host(HostError),
    InstructionLimit,
    InstructionPointer,
    StackUnderflow(StackKind),
    StackOverflow(StackKind),
    LocalIndex(StackKind, i32),
    ArgumentCount(StackKind),
    CallDepth,
    LocalLimit,
    ArraySlot(i32),
    ArrayLength(i32),
    ArrayAlias(i32),
    ArrayIndex(i32),
    SwitchIndex(i32),
    EmptySwitchTable,
    StringLimit,
    InvalidOperand,
    Enum(EnumFailure),
    Component(crate::ComponentFailure),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum EnumFailure {
    NoEnum,
    KeyType,
    ValueType,
    Lookup { enum_id: i32, key: i32 },
    ReverseIndex,
    ValueClass,
    Inconsistent,
}

impl fmt::Display for VmError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "CS2 script {} instruction {}: {:?}",
            self.script_id, self.instruction, self.kind
        )
    }
}

impl std::error::Error for VmError {}
