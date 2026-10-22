use openrs_cs2::{Counts, Opcode, Operand, Script};
use std::sync::Arc;

pub fn int(opcode: Opcode, value: i32) -> (Opcode, Operand) {
    (opcode, Operand::Int(value))
}
pub fn text(value: &str) -> (Opcode, Operand) {
    (
        Opcode::PUSH_CONSTANT_STRING,
        Operand::String(Arc::from(value)),
    )
}
pub fn long(value: i64) -> (Opcode, Operand) {
    (Opcode::PUSH_LONG_CONSTANT, Operand::Long(value))
}
pub fn constant(value: i32) -> (Opcode, Operand) {
    int(Opcode::PUSH_CONSTANT_INT, value)
}
pub fn ret() -> (Opcode, Operand) {
    int(Opcode::RETURN, 0)
}
pub fn counts(ints: u16, strings: u16, longs: u16) -> Counts {
    Counts {
        ints,
        strings,
        longs,
    }
}
pub fn zero() -> Counts {
    counts(0, 0, 0)
}

pub fn cp1252(value: &str) -> Option<Vec<u8>> {
    const EXTENDED: &str = "€\0‚ƒ„…†‡ˆ‰Š‹Œ\0Ž\0\0‘’“”•–—˜™š›œ\0žŸ";
    value
        .chars()
        .map(|character| {
            let number = u32::from(character);
            if (1..128).contains(&number) || (160..256).contains(&number) {
                Some(number as u8)
            } else if number == 0 {
                None
            } else {
                EXTENDED
                    .chars()
                    .position(|candidate| candidate == character)
                    .map(|index| 128 + index as u8)
            }
        })
        .collect()
}

pub fn binary(
    code: &[(Opcode, Operand)],
    locals: Counts,
    args: Counts,
    switches: &[Vec<(i32, i32)>],
) -> Vec<u8> {
    let mut bytes = vec![0];
    for (opcode, operand) in code {
        bytes.extend_from_slice(&opcode.wire().to_be_bytes());
        match operand {
            Operand::String(value) => {
                bytes.extend_from_slice(
                    &cp1252(value).expect("test string must be CP1252 without NUL"),
                );
                bytes.push(0);
            }
            Operand::Long(value) => bytes.extend_from_slice(&value.to_be_bytes()),
            Operand::Int(value) if opcode.metadata().has_int_constant => {
                bytes.extend_from_slice(&value.to_be_bytes())
            }
            Operand::Int(value) => bytes.push(*value as u8),
        }
    }
    bytes.extend_from_slice(&(code.len() as i32).to_be_bytes());
    for value in [
        locals.ints,
        locals.strings,
        locals.longs,
        args.ints,
        args.strings,
        args.longs,
    ] {
        bytes.extend_from_slice(&value.to_be_bytes());
    }
    let switch_start = bytes.len();
    bytes.push(switches.len() as u8);
    for cases in switches {
        bytes.extend_from_slice(&(cases.len() as u16).to_be_bytes());
        for (key, offset) in cases {
            bytes.extend_from_slice(&key.to_be_bytes());
            bytes.extend_from_slice(&offset.to_be_bytes());
        }
    }
    let size = (bytes.len() - switch_start) as u16;
    bytes.extend_from_slice(&size.to_be_bytes());
    bytes
}

pub fn script(code: &[(Opcode, Operand)]) -> Script {
    Script::decode(&binary(code, zero(), zero(), &[])).unwrap()
}

pub fn footer(bytes: &[u8]) -> usize {
    bytes.len()
        - 18
        - usize::from(u16::from_be_bytes(
            bytes[bytes.len() - 2..].try_into().unwrap(),
        ))
}

pub fn hex(value: &str) -> String {
    value
        .as_bytes()
        .iter()
        .map(|b| format!("{b:02x}"))
        .collect()
}
pub fn string(value: Option<&str>) -> String {
    value
        .map(|value| format!("hex:{}", hex(value)))
        .unwrap_or_else(|| "null".into())
}

pub const COORDINATE_OPERATIONS: &[(Opcode, usize)] = &[
    (Opcode::COORDX, 1),
    (Opcode::COORDY, 1),
    (Opcode::COORDZ, 1),
    (Opcode::MOVECOORD, 4),
];

pub const INVERSE_POWER_OPERATIONS: &[(Opcode, usize)] = &[(Opcode::INVPOW, 2)];

pub const NUMERIC_OPERATIONS: &[(Opcode, usize)] = &[
    (Opcode::ADD, 2),
    (Opcode::SUB, 2),
    (Opcode::MULTIPLY, 2),
    (Opcode::DIVIDE, 2),
    (Opcode::INTERPOLATE, 5),
    (Opcode::ADDPERCENT, 2),
    (Opcode::SETBIT, 2),
    (Opcode::CLEARBIT, 2),
    (Opcode::TESTBIT, 2),
    (Opcode::MODULO, 2),
    (Opcode::POW, 2),
    (Opcode::AND, 2),
    (Opcode::OR, 2),
    (Opcode::MIN, 2),
    (Opcode::MAX, 2),
    (Opcode::SCALE, 3),
    (Opcode::NOT, 1),
];

pub const TEXT_OPERATIONS: &[Opcode] = &[
    Opcode::APPEND_NUM,
    Opcode::APPEND,
    Opcode::APPEND_SIGNNUM,
    Opcode::GET_COL_TAG,
    Opcode::TOSTRING,
    Opcode::TEXT_SWITCH,
    Opcode::ESCAPE,
    Opcode::APPEND_CHAR,
    Opcode::CHAR_ISPRINTABLE,
    Opcode::CHAR_ISALPHANUMERIC,
    Opcode::CHAR_ISALPHA,
    Opcode::CHAR_ISNUMERIC,
    Opcode::STRING_LENGTH,
    Opcode::SUBSTRING,
    Opcode::REMOVETAGS,
    Opcode::STRING_INDEXOF_CHAR,
    Opcode::STRING_INDEXOF_STRING,
    Opcode::CHAR_TOLOWERCASE,
    Opcode::CHAR_TOUPPERCASE,
    Opcode::CLANFORUMQFC_TOSTRING,
];

pub const DECODING: &[(&str, &[u8], &str)] = &[
    (
        "all-opcodes",
        include_bytes!("../fixtures/all-opcodes.bin"),
        include_str!("../fixtures/all-opcodes.decode.tsv"),
    ),
    (
        "vm-root",
        include_bytes!("../fixtures/vm-root.bin"),
        include_str!("../fixtures/vm-root.decode.tsv"),
    ),
    (
        "vm-child",
        include_bytes!("../fixtures/vm-child.bin"),
        include_str!("../fixtures/vm-child.decode.tsv"),
    ),
    (
        "vm-flow",
        include_bytes!("../fixtures/vm-flow.bin"),
        include_str!("../fixtures/vm-flow.decode.tsv"),
    ),
    (
        "vm-strings",
        include_bytes!("../fixtures/vm-strings.bin"),
        include_str!("../fixtures/vm-strings.decode.tsv"),
    ),
    (
        "vm-longs",
        include_bytes!("../fixtures/vm-longs.bin"),
        include_str!("../fixtures/vm-longs.decode.tsv"),
    ),
    (
        "vm-numeric",
        include_bytes!("../fixtures/vm-numeric.bin"),
        include_str!("../fixtures/vm-numeric.decode.tsv"),
    ),
    (
        "vm-text",
        include_bytes!("../fixtures/vm-text.bin"),
        include_str!("../fixtures/vm-text.decode.tsv"),
    ),
    (
        "vm-vars",
        include_bytes!("../fixtures/vm-vars.bin"),
        include_str!("../fixtures/vm-vars.decode.tsv"),
    ),
    (
        "vm-sort-child",
        include_bytes!("../fixtures/vm-sort-child.bin"),
        include_str!("../fixtures/vm-sort-child.decode.tsv"),
    ),
    (
        "vm-sort",
        include_bytes!("../fixtures/vm-sort.bin"),
        include_str!("../fixtures/vm-sort.decode.tsv"),
    ),
    (
        "vm-coordinates-child",
        include_bytes!("../fixtures/vm-coordinates-child.bin"),
        include_str!("../fixtures/vm-coordinates-child.decode.tsv"),
    ),
    (
        "vm-coordinates",
        include_bytes!("../fixtures/vm-coordinates.bin"),
        include_str!("../fixtures/vm-coordinates.decode.tsv"),
    ),
    (
        "vm-inverse-power-child",
        include_bytes!("../fixtures/vm-inverse-power-child.bin"),
        include_str!("../fixtures/vm-inverse-power-child.decode.tsv"),
    ),
    (
        "vm-inverse-power",
        include_bytes!("../fixtures/vm-inverse-power.bin"),
        include_str!("../fixtures/vm-inverse-power.decode.tsv"),
    ),
];

pub const EXECUTION: &[(&str, &[u8], &str)] = &[
    (
        "vm-root",
        include_bytes!("../fixtures/vm-root.bin"),
        include_str!("../fixtures/vm-root.vm.tsv"),
    ),
    (
        "vm-flow",
        include_bytes!("../fixtures/vm-flow.bin"),
        include_str!("../fixtures/vm-flow.vm.tsv"),
    ),
    (
        "vm-strings",
        include_bytes!("../fixtures/vm-strings.bin"),
        include_str!("../fixtures/vm-strings.vm.tsv"),
    ),
    (
        "vm-longs",
        include_bytes!("../fixtures/vm-longs.bin"),
        include_str!("../fixtures/vm-longs.vm.tsv"),
    ),
    (
        "vm-numeric",
        include_bytes!("../fixtures/vm-numeric.bin"),
        include_str!("../fixtures/vm-numeric.vm.tsv"),
    ),
    (
        "vm-text",
        include_bytes!("../fixtures/vm-text.bin"),
        include_str!("../fixtures/vm-text.vm.tsv"),
    ),
    (
        "vm-sort",
        include_bytes!("../fixtures/vm-sort.bin"),
        include_str!("../fixtures/vm-sort.vm.tsv"),
    ),
    (
        "vm-coordinates",
        include_bytes!("../fixtures/vm-coordinates.bin"),
        include_str!("../fixtures/vm-coordinates.vm.tsv"),
    ),
    (
        "vm-inverse-power",
        include_bytes!("../fixtures/vm-inverse-power.bin"),
        include_str!("../fixtures/vm-inverse-power.vm.tsv"),
    ),
];
