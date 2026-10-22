use crate::{DecodeError, DecodeErrorKind, Opcode};
use std::sync::Arc;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Counts {
    pub ints: u16,
    pub strings: u16,
    pub longs: u16,
}

impl Counts {
    pub fn total(self) -> usize {
        usize::from(self.ints) + usize::from(self.strings) + usize::from(self.longs)
    }
}

#[derive(Clone, Copy, Debug)]
pub struct DecodeLimits {
    pub max_script_bytes: usize,
    pub max_instructions: usize,
    pub max_string_bytes: usize,
}

impl Default for DecodeLimits {
    fn default() -> Self {
        Self {
            max_script_bytes: 4 * 1024 * 1024,
            max_instructions: 200_000,
            max_string_bytes: 1024 * 1024,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum Operand {
    Int(i32),
    String(Arc<str>),
    Long(i64),
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Instruction {
    pub opcode: Opcode,
    pub operand: Operand,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct SwitchCase {
    pub key: i32,
    pub offset: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct SwitchTable {
    cases: Vec<SwitchCase>,
}

impl SwitchTable {
    pub fn cases(&self) -> &[SwitchCase] {
        &self.cases
    }

    pub fn lookup(&self, key: i32) -> Option<i32> {
        let index = self.cases.partition_point(|case| case.key < key);
        self.cases
            .get(index)
            .filter(|case| case.key == key)
            .map(|case| case.offset)
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Script {
    name: Option<Arc<str>>,
    locals: Counts,
    arguments: Counts,
    instructions: Vec<Instruction>,
    switches: Vec<SwitchTable>,
}

impl Script {
    pub fn decode(bytes: &[u8]) -> Result<Self, DecodeError> {
        Self::decode_with_limits(bytes, DecodeLimits::default())
    }

    pub fn decode_with_limits(bytes: &[u8], limits: DecodeLimits) -> Result<Self, DecodeError> {
        if bytes.len() > limits.max_script_bytes {
            return Err(error(0, DecodeErrorKind::InputLimit));
        }
        if bytes.len() < 20 {
            return Err(error(bytes.len(), DecodeErrorKind::Truncated));
        }
        let footer_end = bytes.len() - 2;
        let switch_size = usize::from(u16::from_be_bytes(bytes[footer_end..].try_into().unwrap()));
        let header = footer_end
            .checked_sub(switch_size)
            .and_then(|n| n.checked_sub(16))
            .filter(|n| *n > 0 && switch_size > 0)
            .ok_or_else(|| error(footer_end, DecodeErrorKind::Footer))?;
        let mut footer = Reader::new(bytes, header, footer_end);
        let count_offset = footer.at;
        let count = usize::try_from(footer.i32()?)
            .map_err(|_| error(count_offset, DecodeErrorKind::InstructionCount))?;
        if count > limits.max_instructions || count > (header - 1) / 3 {
            return Err(error(count_offset, DecodeErrorKind::InstructionCount));
        }
        let locals = footer.counts()?;
        let arguments = footer.counts()?;
        if arguments.ints > locals.ints
            || arguments.strings > locals.strings
            || arguments.longs > locals.longs
        {
            return Err(error(
                count_offset + 10,
                DecodeErrorKind::ArgumentsExceedLocals,
            ));
        }
        let table_count = usize::from(footer.u8()?);
        let mut switches = reserved(table_count, footer.at)?;
        for _ in 0..table_count {
            let case_count = usize::from(footer.u16()?);
            if case_count > (footer.end - footer.at) / 8 {
                return Err(error(footer.at, DecodeErrorKind::Truncated));
            }
            let mut cases = reserved(case_count, footer.at)?;
            for _ in 0..case_count {
                cases.push(SwitchCase {
                    key: footer.i32()?,
                    offset: footer.i32()?,
                });
            }
            cases.sort_by_key(|case| case.key);
            switches.push(SwitchTable { cases });
        }
        if footer.at != footer.end {
            return Err(error(footer.at, DecodeErrorKind::TrailingData));
        }
        let mut code = Reader::new(bytes, 0, header);
        let name = if bytes[0] == 0 {
            code.u8()?;
            None
        } else {
            Some(code.string(limits.max_string_bytes, false)?)
        };
        let mut instructions = reserved(count, code.at)?;
        for _ in 0..count {
            let offset = code.at;
            let wire = code.u16()?;
            let opcode = Opcode::from_wire(wire)
                .ok_or_else(|| error(offset, DecodeErrorKind::UnknownOpcode(wire)))?;
            let operand = match opcode {
                Opcode::PUSH_CONSTANT_STRING => {
                    Operand::String(code.string(limits.max_string_bytes, true)?)
                }
                Opcode::PUSH_LONG_CONSTANT => {
                    Operand::Long(i64::from_be_bytes(code.take(8)?.try_into().unwrap()))
                }
                _ if opcode.metadata().has_int_constant => Operand::Int(code.i32()?),
                _ => Operand::Int(i32::from(code.u8()?)),
            };
            instructions.push(Instruction { opcode, operand });
        }
        if code.at != code.end {
            return Err(error(code.at, DecodeErrorKind::InstructionCount));
        }
        Ok(Self {
            name,
            locals,
            arguments,
            instructions,
            switches,
        })
    }

    pub fn name(&self) -> Option<&str> {
        self.name.as_deref()
    }
    pub fn locals(&self) -> Counts {
        self.locals
    }
    pub fn arguments(&self) -> Counts {
        self.arguments
    }
    pub fn instructions(&self) -> &[Instruction] {
        &self.instructions
    }
    pub fn switches(&self) -> &[SwitchTable] {
        &self.switches
    }
}

fn error(offset: usize, kind: DecodeErrorKind) -> DecodeError {
    DecodeError { offset, kind }
}

fn reserved<T>(count: usize, offset: usize) -> Result<Vec<T>, DecodeError> {
    let mut values = Vec::new();
    values
        .try_reserve_exact(count)
        .map_err(|_| error(offset, DecodeErrorKind::Allocation))?;
    Ok(values)
}

struct Reader<'a> {
    bytes: &'a [u8],
    at: usize,
    end: usize,
}

impl<'a> Reader<'a> {
    fn new(bytes: &'a [u8], at: usize, end: usize) -> Self {
        Self { bytes, at, end }
    }

    fn take(&mut self, count: usize) -> Result<&'a [u8], DecodeError> {
        let end = self
            .at
            .checked_add(count)
            .filter(|end| *end <= self.end)
            .ok_or_else(|| error(self.at, DecodeErrorKind::Truncated))?;
        let result = &self.bytes[self.at..end];
        self.at = end;
        Ok(result)
    }

    fn u8(&mut self) -> Result<u8, DecodeError> {
        Ok(self.take(1)?[0])
    }
    fn u16(&mut self) -> Result<u16, DecodeError> {
        Ok(u16::from_be_bytes(self.take(2)?.try_into().unwrap()))
    }
    fn i32(&mut self) -> Result<i32, DecodeError> {
        Ok(i32::from_be_bytes(self.take(4)?.try_into().unwrap()))
    }
    fn counts(&mut self) -> Result<Counts, DecodeError> {
        Ok(Counts {
            ints: self.u16()?,
            strings: self.u16()?,
            longs: self.u16()?,
        })
    }

    fn string(&mut self, limit: usize, branding: bool) -> Result<Arc<str>, DecodeError> {
        let start = self.at;
        let count = self.bytes[start..self.end]
            .iter()
            .position(|b| *b == 0)
            .ok_or_else(|| error(start, DecodeErrorKind::Truncated))?;
        let raw = self.take(count + 1)?;
        let length = raw[..count]
            .iter()
            .try_fold(0usize, |len, b| len.checked_add(cp1252(*b).len_utf8()))
            .filter(|len| *len <= limit)
            .ok_or_else(|| error(start, DecodeErrorKind::StringLimit))?;
        let mut text = String::new();
        text.try_reserve_exact(length)
            .map_err(|_| error(start, DecodeErrorKind::Allocation))?;
        for byte in &raw[..count] {
            text.push(cp1252(*byte));
        }
        if branding
            && (text.contains("runescape")
                || text.contains("RuneScape")
                || text.contains("Runescape"))
        {
            text = text
                .replace("runescape", "darkan")
                .replace("RuneScape", "Darkan")
                .replace("Runescape", "Darkan");
        }
        Ok(Arc::from(text))
    }
}

fn cp1252(byte: u8) -> char {
    const TABLE: [char; 32] = [
        '\u{20ac}', '?', '\u{201a}', '\u{0192}', '\u{201e}', '\u{2026}', '\u{2020}', '\u{2021}',
        '\u{02c6}', '\u{2030}', '\u{0160}', '\u{2039}', '\u{0152}', '?', '\u{017d}', '?', '?',
        '\u{2018}', '\u{2019}', '\u{201c}', '\u{201d}', '\u{2022}', '\u{2013}', '\u{2014}',
        '\u{02dc}', '\u{2122}', '\u{0161}', '\u{203a}', '\u{0153}', '?', '\u{017e}', '\u{0178}',
    ];
    if (128..160).contains(&byte) {
        TABLE[usize::from(byte - 128)]
    } else {
        char::from(byte)
    }
}
