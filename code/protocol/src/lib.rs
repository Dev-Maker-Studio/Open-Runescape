#![forbid(unsafe_code)]

pub mod client_packets;
pub mod entity_info;
pub mod framing;
pub mod isaac;
pub mod js5;
pub mod payload;
pub mod registry;

use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Feed<T> {
    pub consumed: usize,
    pub item: Option<T>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    InvalidToken,
    HandshakeTooLong,
    InvalidLimit,
    InvalidSeedLength,
    AllocationFailed,
    DecoderFailed,
    DecoderFinished,
    UnexpectedEof,
    InvalidCompression(u8),
    InvalidLength,
    LengthLimitExceeded { length: usize, limit: usize },
    InvalidContinuation(u8),
    InvalidVersionTable,
    InvalidOpcode(u16),
    UnknownOpcode(u16),
    PayloadLengthMismatch { expected: usize, actual: usize },
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidToken => f.write_str("JS5 token contains a null byte"),
            Self::HandshakeTooLong => f.write_str("JS5 handshake exceeds its one-byte length"),
            Self::InvalidLimit => f.write_str("invalid decoder limit"),
            Self::InvalidSeedLength => f.write_str("ISAAC seed exceeds 256 words"),
            Self::AllocationFailed => f.write_str("protocol buffer allocation failed"),
            Self::DecoderFailed => f.write_str("decoder cannot resume after a protocol error"),
            Self::DecoderFinished => f.write_str("handshake decoder has already completed"),
            Self::UnexpectedEof => f.write_str("truncated protocol input"),
            Self::InvalidCompression(value) => write!(f, "unsupported JS5 compression {value}"),
            Self::InvalidLength => f.write_str("invalid protocol length"),
            Self::LengthLimitExceeded { length, limit } => {
                write!(f, "protocol length {length} exceeds limit {limit}")
            }
            Self::InvalidContinuation(value) => {
                write!(f, "invalid JS5 continuation marker {value}")
            }
            Self::InvalidVersionTable => f.write_str("invalid JS5 version table"),
            Self::InvalidOpcode(value) => {
                write!(f, "opcode {value} exceeds the smart-opcode range")
            }
            Self::UnknownOpcode(value) => write!(f, "no packet size registered for opcode {value}"),
            Self::PayloadLengthMismatch { expected, actual } => {
                write!(
                    f,
                    "packet payload length {actual} does not match {expected}"
                )
            }
        }
    }
}

impl std::error::Error for Error {}
