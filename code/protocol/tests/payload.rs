use openrs_protocol::payload::{PayloadError, PayloadReader, PayloadWriter};

type Result<T> = std::result::Result<T, PayloadError>;

const VECTORS: &str = include_str!("fixtures/payload-vectors.tsv");
const READS: &[(&str, usize)] = &[
    ("readUByte", 1),
    ("readByte", 1),
    ("readUByteAdd", 1),
    ("readByteAdd", 1),
    ("readUByteInverse", 1),
    ("readByteInverse", 1),
    ("readUByteSubtract", 1),
    ("readByteSubtract", 1),
    ("readUShort", 2),
    ("readShort", 2),
    ("readUShortLittle", 2),
    ("readShortLittle", 2),
    ("readUShortAdd", 2),
    ("readShortAdd", 2),
    ("readUShortAddLittle", 2),
    ("readShortAddLittle", 2),
    ("readUMedium", 3),
    ("readMedium", 3),
    ("readInt", 4),
    ("readUIntLittle", 4),
    ("readUIntMiddle", 4),
    ("readUIntInverseMiddle", 4),
    ("readLong", 8),
    ("readSmart", 2),
    ("readUSmart", 2),
    ("readBigSmart", 4),
    ("readUBigSmart", 4),
];
const WRITES: &[(&str, usize, i64)] = &[
    ("writeByte", 1, 0x12),
    ("writeByteAdd", 1, 0x12),
    ("writeByteInverse", 1, 0x12),
    ("writeByteSubtract", 1, 0x12),
    ("writeShort", 2, 0x1234),
    ("writeShortLittle", 2, 0x1234),
    ("writeShortAdd", 2, 0x1234),
    ("writeShortAddLittle", 2, 0x1234),
    ("writeMedium", 3, 0x123456),
    ("writeInt", 4, 0x12345678),
    ("writeIntLittle", 4, 0x12345678),
    ("writeIntMiddle", 4, 0x12345678),
    ("writeIntInverseMiddle", 4, 0x12345678),
    ("writeLong", 8, 0x123456789abcdef),
    ("writeSmart", 2, 128),
    ("writeSmart", 1, 127),
];

fn hex(text: &str) -> Vec<u8> {
    assert_eq!(text.len() % 2, 0);
    (0..text.len())
        .step_by(2)
        .map(|i| u8::from_str_radix(&text[i..i + 2], 16).unwrap())
        .collect()
}

fn utf16(text: &str) -> Option<String> {
    if text == "-" {
        return None;
    }
    let units: Vec<_> = (0..text.len())
        .step_by(4)
        .map(|i| u16::from_str_radix(&text[i..i + 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn read(reader: &mut PayloadReader<'_>, method: &str) -> Result<i64> {
    Ok(match method {
        "readUByte" => i64::from(reader.read_u8()?),
        "readByte" => i64::from(reader.read_i8()?),
        "readUByteAdd" => i64::from(reader.read_u8_a()?),
        "readByteAdd" => i64::from(reader.read_i8_a()?),
        "readUByteInverse" => i64::from(reader.read_u8_c()?),
        "readByteInverse" => i64::from(reader.read_i8_c()?),
        "readUByteSubtract" => i64::from(reader.read_u8_s()?),
        "readByteSubtract" => i64::from(reader.read_i8_s()?),
        "readUShort" => i64::from(reader.read_u16_be()?),
        "readShort" => i64::from(reader.read_i16_be()?),
        "readUShortLittle" => i64::from(reader.read_u16_le()?),
        "readShortLittle" => i64::from(reader.read_i16_le()?),
        "readUShortAdd" => i64::from(reader.read_u16_be_a()?),
        "readShortAdd" => i64::from(reader.read_i16_be_a()?),
        "readUShortAddLittle" => i64::from(reader.read_u16_le_a()?),
        "readShortAddLittle" => i64::from(reader.read_i16_le_a()?),
        "readUMedium" => i64::from(reader.read_u24_be()?),
        "readMedium" => i64::from(reader.read_i24_be()?),
        "readInt" => i64::from(reader.read_i32_be()?),
        "readUIntLittle" => i64::from(reader.read_i32_le()?),
        "readUIntMiddle" => i64::from(reader.read_i32_middle()?),
        "readUIntInverseMiddle" => i64::from(reader.read_i32_inverse_middle()?),
        "readLong" => reader.read_i64_be()?,
        "readSmart" => i64::from(reader.read_smart()?),
        "readUSmart" => i64::from(reader.read_u_smart()?),
        "readBigSmart" => i64::from(reader.read_big_smart()?),
        "readUBigSmart" => i64::from(reader.read_u_big_smart()?),
        _ => panic!("unknown native reader {method}"),
    })
}

fn write(writer: &mut PayloadWriter<'_>, method: &str, value: i64) -> Result<()> {
    if value < 0 {
        return match method {
            "writeByte" => writer.write_i8(value as i8),
            "writeByteAdd" => writer.write_i8_a(value as i8),
            "writeByteInverse" => writer.write_i8_c(value as i8),
            "writeByteSubtract" => writer.write_i8_s(value as i8),
            "writeShort" => writer.write_i16_be(value as i16),
            "writeShortLittle" => writer.write_i16_le(value as i16),
            "writeShortAdd" => writer.write_i16_be_a(value as i16),
            "writeShortAddLittle" => writer.write_i16_le_a(value as i16),
            "writeMedium" => writer.write_i24_be(value as i32),
            "writeInt" => writer.write_i32_be(value as i32),
            "writeIntLittle" => writer.write_i32_le(value as i32),
            "writeIntMiddle" => writer.write_i32_middle(value as i32),
            "writeIntInverseMiddle" => writer.write_i32_inverse_middle(value as i32),
            "writeLong" => writer.write_i64_be(value),
            _ => panic!("unknown native signed writer {method}"),
        };
    }
    match method {
        "writeByte" => writer.write_u8(value as u8),
        "writeByteAdd" => writer.write_u8_a(value as u8),
        "writeByteInverse" => writer.write_u8_c(value as u8),
        "writeByteSubtract" => writer.write_u8_s(value as u8),
        "writeShort" => writer.write_u16_be(value as u16),
        "writeShortLittle" => writer.write_u16_le(value as u16),
        "writeShortAdd" => writer.write_u16_be_a(value as u16),
        "writeShortAddLittle" => writer.write_u16_le_a(value as u16),
        "writeMedium" => writer.write_u24_be(value as u32),
        "writeInt" => writer.write_u32_be(value as u32),
        "writeIntLittle" => writer.write_u32_le(value as u32),
        "writeIntMiddle" => writer.write_u32_middle(value as u32),
        "writeIntInverseMiddle" => writer.write_u32_inverse_middle(value as u32),
        "writeLong" => writer.write_u64_be(value as u64),
        "writeSmart" => writer.write_u_smart(value as u16),
        _ => panic!("unknown native writer {method}"),
    }
}

fn hash_byte(hash: u64, byte: u8) -> u64 {
    (hash ^ u64::from(byte)).wrapping_mul(0x100000001b3)
}
fn hash_read(mut hash: u64, value: i64, position: usize) -> u64 {
    for byte in value.to_le_bytes() {
        hash = hash_byte(hash, byte);
    }
    hash_byte(hash, position as u8)
}
fn hash_write(mut hash: u64, bytes: &[u8]) -> u64 {
    for &byte in bytes {
        hash = hash_byte(hash, byte);
    }
    hash_byte(hash, bytes.len() as u8)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_integer_read_vectors_include_signed_and_noncanonical_smart_encodings() {
    let mut count = 0;
    for line in VECTORS.lines().filter(|line| line.starts_with("R\t")) {
        let row: Vec<_> = line.split('\t').collect();
        let bytes = hex(row[2]);
        let mut reader = PayloadReader::new(&bytes);
        assert_eq!(
            read(&mut reader, row[1]).unwrap(),
            row[3].parse::<i64>().unwrap(),
            "{line}"
        );
        assert_eq!(
            reader.position(),
            row[4].parse::<usize>().unwrap(),
            "{line}"
        );
        assert_eq!(reader.remaining(), bytes.len() - reader.position());
        count += 1;
    }
    assert_eq!(count, 233);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_integer_write_vectors_fix_middle_endian_order_and_signed_edges() {
    let mut count = 0;
    for line in VECTORS.lines().filter(|line| line.starts_with("W\t")) {
        let row: Vec<_> = line.split('\t').collect();
        let expected = hex(row[3]);
        let mut output = [0x55; 16];
        let mut writer = PayloadWriter::new(&mut output);
        write(&mut writer, row[1], row[2].parse().unwrap()).unwrap();
        assert_eq!(writer.written(), expected, "{line}");
        assert_eq!(writer.position(), row[4].parse::<usize>().unwrap());
        assert_eq!(writer.remaining(), 16 - expected.len());
        assert!(output[expected.len()..].iter().all(|&b| b == 0x55));
        count += 1;
    }
    assert!(count > 200);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_exhaustive_byte_short_and_smart_hashes_match_without_roundtrip_assumptions() {
    let mut count = 0;
    for line in VECTORS
        .lines()
        .filter(|line| line.starts_with("H\t") && !line.starts_with("H\tS\t"))
    {
        let row: Vec<_> = line.split('\t').collect();
        let method = row[2];
        let width = if row[1] == "R" {
            READS.iter().find(|entry| entry.0 == method).unwrap().1
        } else {
            WRITES.iter().find(|entry| entry.0 == method).unwrap().1
        };
        let limit = if method == "writeSmart" {
            32768
        } else {
            1 << (width * 8)
        };
        let mut hash = 0xcbf29ce484222325;
        for value in 0..limit {
            if row[1] == "R" {
                let bytes = (value as u16).to_be_bytes();
                let bytes = &bytes[2 - width..];
                let mut reader = PayloadReader::new(bytes);
                let actual = read(&mut reader, method).unwrap();
                hash = hash_read(hash, actual, reader.position());
            } else {
                let mut bytes = [0; 2];
                let mut writer = PayloadWriter::new(&mut bytes);
                write(&mut writer, method, value as i64).unwrap();
                hash = hash_write(hash, writer.written());
            }
        }
        assert_eq!(hash, u64::from_str_radix(row[3], 16).unwrap(), "{line}");
        count += 1;
    }
    assert_eq!(count, 27);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_bit_vectors_cover_every_alignment_and_width_including_32() {
    let mut count = 0;
    for line in VECTORS.lines().filter(|line| line.starts_with("B\t")) {
        let row: Vec<_> = line.split('\t').collect();
        let bytes = hex(row[1]);
        let mut reader = PayloadReader::new(&bytes);
        let initial = row[2].parse::<usize>().unwrap();
        reader.read_bytes(initial).unwrap();
        reader.start_bit_access().unwrap();
        let skip = row[3].parse::<u8>().unwrap();
        if skip != 0 {
            reader.read_bits(skip).unwrap();
        }
        let actual = reader.read_bits(row[4].parse().unwrap()).unwrap();
        assert_eq!(
            i64::from(actual as i32),
            row[5].parse::<i64>().unwrap(),
            "{line}"
        );
        assert_eq!(reader.position(), initial);
        reader.finish_bit_access().unwrap();
        assert_eq!(
            reader.position(),
            row[6].parse::<usize>().unwrap(),
            "{line}"
        );
        assert!(!reader.is_bit_access());
        count += 1;
    }
    assert_eq!(count, 7 * 8 * 32);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_cp1252_vectors_include_all_bytes_jag_nullable_and_utf16_fallback() {
    let mut count = 0;
    for line in VECTORS
        .lines()
        .filter(|line| line.starts_with("S\t") || line.starts_with("T\t"))
    {
        let row: Vec<_> = line.split('\t').collect();
        if row[0] == "S" {
            let text = utf16(row[2]).unwrap();
            let expected = hex(row[3]);
            let mut bytes = [0x55; 256];
            let mut writer = PayloadWriter::new(&mut bytes);
            if row[1] == "writeString" {
                writer.write_cp1252(&text).unwrap();
            } else {
                writer.write_jag_cp1252(&text).unwrap();
            }
            assert_eq!(writer.written(), expected, "{line}");
            assert!(bytes[expected.len()..].iter().all(|&b| b == 0x55));
        } else {
            let bytes = hex(row[2]);
            let mut reader = PayloadReader::new(&bytes);
            let actual = match row[1] {
                "readString" => Some(reader.read_cp1252().unwrap()),
                "readNullString" => reader.read_nullable_cp1252().unwrap(),
                "readJagString" => Some(reader.read_jag_cp1252().unwrap()),
                _ => panic!("unknown string reader"),
            };
            assert_eq!(actual, utf16(row[3]), "{line}");
            assert_eq!(reader.position(), row[4].parse::<usize>().unwrap());
        }
        count += 1;
    }
    assert_eq!(count, 12 + 256 * 3);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_exhaustive_bmp_scalar_encoding_hash_matches() {
    let row: Vec<_> = VECTORS
        .lines()
        .find(|line| line.starts_with("H\tS\t"))
        .unwrap()
        .split('\t')
        .collect();
    let mut hash = 0xcbf29ce484222325;
    for unit in 1..=65535 {
        if let Some(ch) = char::from_u32(unit) {
            let text = ch.to_string();
            let mut bytes = [0; 2];
            let mut writer = PayloadWriter::new(&mut bytes);
            writer.write_cp1252(&text).unwrap();
            hash = hash_write(hash, writer.written());
        }
    }
    assert_eq!(hash, u64::from_str_radix(row[3], 16).unwrap());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_truncated_integer_read_is_atomic_including_smart_branches() {
    for &(method, width) in READS {
        for length in 0..width {
            let mut bytes = vec![0x55];
            bytes.extend(vec![0xff; length]);
            let mut reader = PayloadReader::new(&bytes);
            assert_eq!(reader.read_u8().unwrap(), 0x55);
            assert_eq!(
                read(&mut reader, method),
                Err(PayloadError::UnexpectedEof),
                "{method} {length}"
            );
            assert_eq!(reader.position(), 1);
            assert_eq!(reader.remaining(), length);
            assert_eq!(reader.read_bytes(length).unwrap(), &bytes[1..]);
        }
    }
    for method in ["readBigSmart", "readUBigSmart"] {
        let mut reader = PayloadReader::new(&[0x7f]);
        assert_eq!(read(&mut reader, method), Err(PayloadError::UnexpectedEof));
        assert_eq!(reader.position(), 0);
    }
    let mut reader = PayloadReader::new(&[1, 2]);
    assert_eq!(
        reader.read_bytes(usize::MAX),
        Err(PayloadError::UnexpectedEof)
    );
    assert_eq!(reader.position(), 0);
    assert_eq!(reader.read_bytes(2).unwrap(), [1, 2]);
    assert!(reader.read_bytes(0).unwrap().is_empty());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_short_integer_write_preserves_cursor_prefix_and_output() {
    for &(method, width, value) in WRITES {
        for value in [value, -1] {
            if method == "writeSmart" && value == -1 {
                continue;
            }
            for remaining in 0..width {
                let mut bytes = vec![0x66; remaining + 1];
                let mut writer = PayloadWriter::new(&mut bytes);
                writer.write_u8(0x55).unwrap();
                assert_eq!(
                    write(&mut writer, method, value),
                    Err(PayloadError::OutputFull {
                        needed: width,
                        remaining
                    }),
                    "{method} {value} {remaining}"
                );
                assert_eq!(writer.position(), 1);
                assert_eq!(writer.written(), [0x55]);
                assert_eq!(writer.remaining(), remaining);
                assert_eq!(bytes[0], 0x55);
                assert!(bytes[1..].iter().all(|&b| b == 0x66));
            }
        }
    }
    let mut empty = [];
    let mut writer = PayloadWriter::new(&mut empty);
    writer.write_bytes(&[]).unwrap();
    assert_eq!(
        writer.write_bytes(&[1]),
        Err(PayloadError::OutputFull {
            needed: 1,
            remaining: 0
        })
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn narrow_writes_reject_out_of_range_values_without_native_silent_truncation() {
    let mut bytes = [0x55; 8];
    let mut writer = PayloadWriter::new(&mut bytes);
    for value in [0x1000000, u32::MAX] {
        assert_eq!(
            writer.write_u24_be(value),
            Err(PayloadError::OutOfRange {
                value: i64::from(value),
                min: 0,
                max: 0xffffff
            })
        );
    }
    for value in [i32::MIN, -8388609, 8388608, i32::MAX] {
        assert_eq!(
            writer.write_i24_be(value),
            Err(PayloadError::OutOfRange {
                value: i64::from(value),
                min: -8388608,
                max: 8388607
            })
        );
    }
    for value in [32768, u16::MAX] {
        assert_eq!(
            writer.write_u_smart(value),
            Err(PayloadError::OutOfRange {
                value: i64::from(value),
                min: 0,
                max: 32767
            })
        );
    }
    assert_eq!(writer.position(), 0);
    assert_eq!(bytes, [0x55; 8]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bit_mode_checks_and_failed_reads_do_not_advance_either_cursor() {
    let mut reader = PayloadReader::new(&[0x55, 0xaa, 0xff, 0, 0x7f]);
    assert_eq!(reader.read_bits(1), Err(PayloadError::NotInBitMode));
    assert_eq!(reader.finish_bit_access(), Err(PayloadError::NotInBitMode));
    reader.read_u8().unwrap();
    reader.start_bit_access().unwrap();
    assert_eq!(reader.start_bit_access(), Err(PayloadError::InBitMode));
    for &(method, _) in READS {
        assert_eq!(read(&mut reader, method), Err(PayloadError::InBitMode));
    }
    assert_eq!(reader.read_bytes(0), Err(PayloadError::InBitMode));
    assert_eq!(reader.read_cp1252(), Err(PayloadError::InBitMode));
    assert_eq!(reader.read_nullable_cp1252(), Err(PayloadError::InBitMode));
    assert_eq!(reader.read_jag_cp1252(), Err(PayloadError::InBitMode));
    for count in [0, 33, 255] {
        assert_eq!(
            reader.read_bits(count),
            Err(PayloadError::InvalidBitCount(count))
        );
    }
    assert_eq!(reader.read_bits(1).unwrap(), 1);
    let mut reference = reader.clone();
    assert_eq!(reader.read_bits(32), Err(PayloadError::UnexpectedEof));
    assert_eq!(reader.read_bits(31), reference.read_bits(31));
    assert_eq!(reader.position(), 1);
    assert_eq!(reader.read_bits(1), Err(PayloadError::UnexpectedEof));
    reader.finish_bit_access().unwrap();
    assert_eq!(reader.position(), 5);
    reader.start_bit_access().unwrap();
    assert_eq!(reader.read_bits(1), Err(PayloadError::UnexpectedEof));
    reader.finish_bit_access().unwrap();
    assert_eq!(reader.position(), 5);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bit_access_rounds_up_and_can_restart_between_byte_fields() {
    for bits in 1..=32 {
        let mut reader = PayloadReader::new(&[0xff; 9]);
        reader.read_u8().unwrap();
        reader.start_bit_access().unwrap();
        assert_eq!(reader.read_bits(bits).unwrap(), u32::MAX >> (32 - bits));
        reader.finish_bit_access().unwrap();
        assert_eq!(reader.position(), 1 + usize::from(bits).div_ceil(8));
        assert_eq!(reader.read_u8().unwrap(), 255);
        reader.start_bit_access().unwrap();
        reader.finish_bit_access().unwrap();
        assert_eq!(reader.position(), 2 + usize::from(bits).div_ceil(8));
        reader.start_bit_access().unwrap();
        assert_eq!(reader.read_bits(1).unwrap(), 1);
        reader.finish_bit_access().unwrap();
        assert_eq!(reader.position(), 3 + usize::from(bits).div_ceil(8));
    }
    let mut empty = PayloadReader::new(&[]);
    empty.start_bit_access().unwrap();
    assert_eq!(empty.read_bits(32), Err(PayloadError::UnexpectedEof));
    empty.finish_bit_access().unwrap();
    assert_eq!(empty.position(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn strings_enforce_terminators_prefixes_nul_rules_and_atomic_bounds() {
    for bytes in [&[][..], &[1][..], &[0, 1][..], &[0, 0xff, 0xff][..]] {
        let mut reader = PayloadReader::new(bytes);
        assert_eq!(
            reader.read_jag_cp1252(),
            Err(if bytes.first().is_some_and(|&b| b != 0) {
                PayloadError::InvalidStringPrefix(bytes[0])
            } else {
                PayloadError::UnexpectedEof
            })
        );
        assert_eq!(reader.position(), 0);
    }
    for bytes in [&[][..], &[1][..], &[0xff, 0x80][..]] {
        let mut reader = PayloadReader::new(bytes);
        assert_eq!(reader.read_cp1252(), Err(PayloadError::UnexpectedEof));
        assert_eq!(
            reader.read_nullable_cp1252(),
            Err(PayloadError::UnexpectedEof)
        );
        assert_eq!(reader.position(), 0);
    }
    for jag in [false, true] {
        for text in ["", "x", "€", "😀"] {
            let needed = text.encode_utf16().count() + 1 + usize::from(jag);
            for remaining in 0..needed {
                let mut bytes = vec![0x55; remaining];
                let mut writer = PayloadWriter::new(&mut bytes);
                let result = if jag {
                    writer.write_jag_cp1252(text)
                } else {
                    writer.write_cp1252(text)
                };
                assert_eq!(result, Err(PayloadError::OutputFull { needed, remaining }));
                assert_eq!(writer.position(), 0);
                assert!(bytes.iter().all(|&b| b == 0x55));
            }
        }
        let mut bytes = [0x55; 16];
        let mut writer = PayloadWriter::new(&mut bytes);
        for text in ["\0", "before\0after", "😀\0"] {
            let result = if jag {
                writer.write_jag_cp1252(text)
            } else {
                writer.write_cp1252(text)
            };
            assert_eq!(result, Err(PayloadError::EmbeddedNul));
            assert_eq!(writer.position(), 0);
        }
        assert_eq!(bytes, [0x55; 16]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unsigned_wide_aliases_preserve_every_bit() {
    let mut bytes = [0; 24];
    let mut writer = PayloadWriter::new(&mut bytes);
    writer.write_u32_be(0x89abcdef).unwrap();
    writer.write_u32_le(0x89abcdef).unwrap();
    writer.write_u32_middle(0x89abcdef).unwrap();
    writer.write_u32_inverse_middle(0x89abcdef).unwrap();
    writer.write_u64_be(u64::MAX).unwrap();
    assert_eq!(
        writer.written(),
        [
            0x89, 0xab, 0xcd, 0xef, 0xef, 0xcd, 0xab, 0x89, 0xcd, 0xef, 0x89, 0xab, 0xab, 0x89,
            0xef, 0xcd, 255, 255, 255, 255, 255, 255, 255, 255
        ]
    );
    let mut reader = PayloadReader::new(&bytes);
    assert_eq!(reader.read_u32_be().unwrap(), 0x89abcdef);
    assert_eq!(reader.read_u32_le().unwrap(), 0x89abcdef);
    assert_eq!(reader.read_u32_middle().unwrap(), 0x89abcdef);
    assert_eq!(reader.read_u32_inverse_middle().unwrap(), 0x89abcdef);
    assert_eq!(reader.read_u64_be().unwrap(), u64::MAX);
    assert_eq!(reader.remaining(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn arbitrary_payload_operations_are_bounded_and_recoverable() {
    let mut state = 0x12345678u32;
    for length in 0..32 {
        for _ in 0..32 {
            let mut bytes = vec![0; length];
            for byte in &mut bytes {
                state ^= state << 13;
                state ^= state >> 17;
                state ^= state << 5;
                *byte = state as u8;
            }
            for &(method, _) in READS {
                let mut reader = PayloadReader::new(&bytes);
                let result = read(&mut reader, method);
                assert!(reader.position() <= bytes.len());
                if result.is_err() {
                    assert_eq!(reader.position(), 0);
                }
            }
            let mut reader = PayloadReader::new(&bytes);
            reader.start_bit_access().unwrap();
            for width in [1, 2, 5, 8, 11, 26, 30, 18, 32] {
                let mut reference = reader.clone();
                if reader.read_bits(width).is_err() {
                    assert_eq!(reader.read_bits(1), reference.read_bits(1));
                }
            }
            reader.finish_bit_access().unwrap();
            assert!(reader.position() <= length);
            for method in [PayloadReader::read_cp1252, PayloadReader::read_jag_cp1252] {
                let mut reader = PayloadReader::new(&bytes);
                let result = method(&mut reader);
                assert!(reader.position() <= length);
                if result.is_err() {
                    assert_eq!(reader.position(), 0);
                }
            }
        }
    }
}
