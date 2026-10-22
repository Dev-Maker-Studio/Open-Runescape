use super::writer::{char_to_byte, Text};

pub const GET_FIELD_VALUE: u8 = 0;
pub const SET_FIELD_VALUE: u8 = 1;
pub const GET_FIELD_MODIFIERS: u8 = 2;
pub const GET_METHOD_RESULT: u8 = 3;
pub const GET_METHOD_MODIFIERS: u8 = 4;

pub const CLASS_NOT_FOUND: i8 = -1;
pub const SECURITY: i8 = -2;
pub const NULL_POINTER: i8 = -3;
pub const EXCEPTION: i8 = -4;
pub const THROWABLE: i8 = -5;

const PRIMITIVE_TYPES: [&[u8]; 9] = [b"B", b"I", b"S", b"J", b"Z", b"F", b"D", b"C", b"void"];

pub fn crc32(bytes: &[u8]) -> u32 {
    let mut crc = 0xffff_ffffu32;
    for &byte in bytes {
        crc ^= u32::from(byte);
        for _ in 0..8 {
            crc = if crc & 1 != 0 {
                (crc >> 1) ^ 0xedb8_8320
            } else {
                crc >> 1
            };
        }
    }
    !crc
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum OpResult {
    Failed(i8),
    FieldValue(i32),
    FieldSet,
    Modifiers(i32),
    MethodNull,
    MethodNumber(i64),
    MethodString(Text),
    MethodOther,
}

pub fn encode_results(header: i32, results: &[OpResult]) -> Vec<u8> {
    let mut out = Vec::new();
    out.extend_from_slice(&header.to_be_bytes());
    for result in results {
        match result {
            OpResult::Failed(code) => out.push(*code as u8),
            OpResult::FieldValue(value) => {
                out.push(0);
                out.extend_from_slice(&value.to_be_bytes());
            }
            OpResult::FieldSet => out.push(0),
            OpResult::Modifiers(value) => {
                out.push(0);
                out.extend_from_slice(&value.to_be_bytes());
            }
            OpResult::MethodNull => out.push(0),
            OpResult::MethodNumber(value) => {
                out.push(1);
                out.extend_from_slice(&value.to_be_bytes());
            }
            OpResult::MethodString(text) => {
                out.push(2);
                if text.0.contains(&0) {
                    out.push(-15i8 as u8);
                } else {
                    out.extend(text.0.iter().map(|unit| char_to_byte(*unit)));
                    out.push(0);
                }
            }
            OpResult::MethodOther => out.push(4),
        }
    }
    let crc = crc32(&out);
    out.extend_from_slice(&crc.to_be_bytes());
    out
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct RequestOp {
    pub operation: u8,
    pub failure: i8,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Request {
    pub header: i32,
    pub operations: Vec<RequestOp>,
}

struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> Reader<'a> {
    fn byte(&mut self) -> Option<u8> {
        let value = self.bytes.get(self.position).copied();
        self.position = self.position.saturating_add(1);
        value
    }

    fn int(&mut self) -> Option<i32> {
        let mut value = 0u32;
        let mut ok = true;
        for _ in 0..4 {
            match self.byte() {
                Some(byte) => value = (value << 8) | u32::from(byte),
                None => ok = false,
            }
        }
        ok.then_some(value as i32)
    }

    fn string(&mut self) -> Option<&'a [u8]> {
        let start = self.position;
        loop {
            if self.byte()? == 0 {
                return Some(&self.bytes[start..self.position - 1]);
            }
        }
    }

    fn skip(&mut self, length: i32) -> Option<()> {
        if length < 0 {
            return None;
        }
        let length = length as usize;
        let end = self.position.saturating_add(length);
        let ok = end <= self.bytes.len();
        self.position = end;
        ok.then_some(())
    }
}

fn is_primitive(name: &[u8]) -> bool {
    PRIMITIVE_TYPES.contains(&name)
}

fn resolution_failure(names: &[&[u8]], class: &[u8]) -> i8 {
    if names.iter().chain([&class]).any(|name| !is_primitive(name)) {
        CLASS_NOT_FOUND
    } else {
        SECURITY
    }
}

fn parse_operation(reader: &mut Reader<'_>) -> Option<RequestOp> {
    let operation = reader.byte()?;
    match operation {
        GET_FIELD_VALUE | SET_FIELD_VALUE | GET_FIELD_MODIFIERS => {
            let class = reader.string()?;
            reader.string()?;
            if operation == SET_FIELD_VALUE {
                reader.int()?;
            }
            Some(RequestOp {
                operation,
                failure: resolution_failure(&[], class),
            })
        }
        GET_METHOD_RESULT | GET_METHOD_MODIFIERS => {
            let class = reader.string()?;
            reader.string()?;
            let count = reader.byte()?;
            let mut types = Vec::with_capacity(usize::from(count));
            for _ in 0..count {
                types.push(reader.string()?);
            }
            let returns = reader.string()?;
            if operation == GET_METHOD_RESULT {
                for _ in 0..count {
                    let length = reader.int()?;
                    reader.skip(length)?;
                }
            }
            types.push(returns);
            Some(RequestOp {
                operation,
                failure: resolution_failure(&types, class),
            })
        }
        _ => Some(RequestOp {
            operation: GET_FIELD_VALUE,
            failure: 0,
        }),
    }
}

pub fn parse_request(payload: &[u8]) -> Option<Request> {
    let mut reader = Reader {
        bytes: payload,
        position: 0,
    };
    let count = reader.byte()?;
    let header = reader.int()?;
    let mut operations = Vec::with_capacity(usize::from(count));
    for _ in 0..count {
        operations.push(parse_operation(&mut reader).unwrap_or(RequestOp {
            operation: GET_FIELD_VALUE,
            failure: EXCEPTION,
        }));
    }
    Some(Request { header, operations })
}

pub fn answer_without_runtime(request: &Request) -> Vec<u8> {
    let results: Vec<OpResult> = request
        .operations
        .iter()
        .map(|op| {
            OpResult::Failed(if op.failure != 0 {
                op.failure
            } else {
                -19
            })
        })
        .collect();
    encode_results(request.header, &results)
}
