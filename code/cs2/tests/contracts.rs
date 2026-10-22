#[allow(dead_code)]
mod common;
#[path = "../opcode_source.rs"]
mod opcode_source;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeSet, fmt::Write};

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn metadata_matches_every_native_enum_entry_and_current_kotlin_source() {
    let source = include_str!("../../client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt");
    let parsed = opcode_source::parse(source).unwrap();
    assert_eq!(parsed.len(), OPCODES.len());
    let mut actual = String::new();
    for (index, entry) in OPCODES.iter().enumerate() {
        let native = &parsed[index];
        assert_eq!(entry.ordinal as usize, index);
        assert_eq!(entry.name, native.name);
        assert_eq!(entry.opcode.wire(), native.wire);
        assert_eq!(entry.has_int_constant, native.wide);
        assert_eq!(entry.family, native.family);
        assert_eq!(Opcode::from_wire(native.wire), Some(entry.opcode));
        assert_eq!(entry.opcode.metadata(), entry);
        writeln!(
            actual,
            "{index}\t{}\t{}\t{}\t{}",
            entry.opcode.wire(),
            entry.name,
            entry.has_int_constant,
            entry.family
        )
        .unwrap();
    }
    assert_eq!(actual, include_str!("fixtures/opcodes.tsv"));
    assert_eq!(
        OPCODES
            .iter()
            .map(|entry| entry.opcode.wire())
            .collect::<BTreeSet<_>>()
            .len(),
        OPCODES.len()
    );
    assert_eq!(Opcode::from_wire(u16::MAX), None);
    for (opcode, wire, family) in [
        (Opcode::INV_GETTOOLTIP, 1006, 7),
        (Opcode::INV_OTHER_GETTOOLTIP, 1007, 7),
        (Opcode::GET_ACTIVE_MINIMENU_INVSLOT, 1008, 7),
        (Opcode::PARAHEIGHT_EXTRA, 1009, 10),
    ] {
        assert_eq!(opcode.wire(), wire);
        assert!(!opcode.metadata().has_int_constant);
        assert_eq!(opcode.metadata().family, family);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn generator_rejects_silent_source_drift_and_duplicate_keys() {
    let source = include_str!("../../client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt");
    for changed in [
        source.replace(
            "hasIntConstant: Boolean = false",
            "hasIntConstant: Boolean = true",
        ),
        source.replace("familyId: Int = 0", "familyId: Int = 1"),
        source.replace("PUSH_VAR(154,", "PUSH_VAR(79,"),
        source.replace("PUSH_VAR(154,", "PUSH_CONSTANT_INT(154,"),
        source.replace("PUSH_VAR(154, true, 0)", "PUSH_VAR(154, unknown = true, 0)"),
        source.replace(
            "PUSH_VAR(154, true, 0)",
            "PUSH_VAR(154, hasIntConstant = true, hasIntConstant = false)",
        ),
        source.replace("PUSH_VAR(154, true, 0)", "PUSH_VAR(computed(), true, 0)"),
    ] {
        assert!(opcode_source::parse(&changed).is_err());
    }
    let named = source.replace(
        "PUSH_VAR(154, true, 0)",
        "PUSH_VAR(154, hasIntConstant = true, familyId = 0)",
    );
    assert_eq!(
        opcode_source::parse(&named).unwrap(),
        opcode_source::parse(source).unwrap()
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn decoded_scripts_match_the_actual_kotlin_decoder() {
    for (name, bytes, expected) in DECODING {
        let script = Script::decode(bytes).unwrap();
        let mut actual = format!("name\t{}\n", string(script.name()));
        let locals = script.locals();
        let args = script.arguments();
        writeln!(
            actual,
            "counts\t{}\t{}\t{}\t{}\t{}\t{}",
            locals.ints, locals.strings, locals.longs, args.ints, args.strings, args.longs
        )
        .unwrap();
        for (index, instruction) in script.instructions().iter().enumerate() {
            write!(actual, "{index}\t{}\t", instruction.opcode.metadata().name).unwrap();
            match &instruction.operand {
                Operand::Int(value) => writeln!(actual, "int\t{value}"),
                Operand::Long(value) => writeln!(actual, "long\t{value}"),
                Operand::String(value) => writeln!(actual, "string\t{}", string(Some(value))),
            }
            .unwrap();
        }
        for (index, table) in script.switches().iter().enumerate() {
            let keys: BTreeSet<_> = table.cases().iter().map(|case| case.key).collect();
            for key in keys {
                writeln!(
                    actual,
                    "switch\t{index}\t{key}\t{}",
                    table.lookup(key).unwrap()
                )
                .unwrap();
            }
        }
        assert_eq!(&actual, expected, "{name}");
    }
    let all = Script::decode(DECODING[0].1).unwrap();
    assert_eq!(all.instructions().len(), OPCODES.len());
    assert_eq!(all.name(), Some("RuneScape n€?"));
    assert_eq!(
        all.instructions()
            .iter()
            .find(|instruction| instruction.opcode == Opcode::PUSH_LONG_CONSTANT)
            .unwrap()
            .operand,
        Operand::Long(i64::MIN + 123)
    );
    assert_eq!(
        all.instructions()
            .iter()
            .find(|instruction| instruction.opcode == Opcode::RETURN)
            .unwrap()
            .operand,
        Operand::Int(255)
    );
    assert_eq!(
        all.instructions()
            .iter()
            .find(|instruction| instruction.opcode == Opcode::POP_LONG_DISCARD)
            .unwrap()
            .operand,
        Operand::Int(0x81234567u32 as i32)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn footer_bounds_counts_and_argument_shapes_fail_closed() {
    let good = binary(&[constant(9), ret()], zero(), zero(), &[]);
    for value in [-1, 0, 1, 3, i32::MAX] {
        let mut bad = good.clone();
        let at = footer(&bad);
        bad[at..at + 4].copy_from_slice(&value.to_be_bytes());
        assert!(matches!(
            Script::decode(&bad).unwrap_err().kind,
            DecodeErrorKind::InstructionCount | DecodeErrorKind::Truncated
        ));
    }
    for size in [0u16, u16::MAX, good.len() as u16] {
        let mut bad = good.clone();
        let at = bad.len() - 2;
        bad[at..].copy_from_slice(&size.to_be_bytes());
        assert_eq!(
            Script::decode(&bad).unwrap_err().kind,
            DecodeErrorKind::Footer
        );
    }
    let bad = binary(&[ret()], zero(), counts(1, 0, 0), &[]);
    assert_eq!(
        Script::decode(&bad).unwrap_err().kind,
        DecodeErrorKind::ArgumentsExceedLocals
    );
    let bad = binary(&[ret()], zero(), counts(0, 1, 0), &[]);
    assert_eq!(
        Script::decode(&bad).unwrap_err().kind,
        DecodeErrorKind::ArgumentsExceedLocals
    );
    let bad = binary(&[ret()], zero(), counts(0, 0, 1), &[]);
    assert_eq!(
        Script::decode(&bad).unwrap_err().kind,
        DecodeErrorKind::ArgumentsExceedLocals
    );
    let limits = DecodeLimits {
        max_script_bytes: good.len() - 1,
        ..DecodeLimits::default()
    };
    assert_eq!(
        Script::decode_with_limits(&good, limits).unwrap_err().kind,
        DecodeErrorKind::InputLimit
    );
    let limits = DecodeLimits {
        max_instructions: 1,
        ..DecodeLimits::default()
    };
    assert_eq!(
        Script::decode_with_limits(&good, limits).unwrap_err().kind,
        DecodeErrorKind::InstructionCount
    );
    let mut huge = binary(&[ret()], zero(), zero(), &[]);
    let at = footer(&huge);
    huge[at..at + 4].copy_from_slice(&i32::MAX.to_be_bytes());
    let limits = DecodeLimits {
        max_instructions: usize::MAX,
        ..DecodeLimits::default()
    };
    assert_eq!(
        Script::decode_with_limits(&huge, limits).unwrap_err().kind,
        DecodeErrorKind::InstructionCount
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn truncated_unknown_and_extra_operands_are_rejected() {
    for end in 0..DECODING[0].1.len() {
        assert!(
            Script::decode(&DECODING[0].1[..end]).is_err(),
            "prefix {end}"
        );
    }
    let mut unknown = binary(&[ret()], zero(), zero(), &[]);
    unknown[1..3].copy_from_slice(&u16::MAX.to_be_bytes());
    assert_eq!(
        Script::decode(&unknown).unwrap_err().kind,
        DecodeErrorKind::UnknownOpcode(u16::MAX)
    );
    let mut truncated = binary(&[constant(9)], zero(), zero(), &[]);
    truncated.remove(4);
    assert!(Script::decode(&truncated).is_err());
    let mut truncated = binary(&[long(9)], zero(), zero(), &[]);
    truncated.remove(8);
    assert!(Script::decode(&truncated).is_err());
    let mut unterminated = binary(&[text("x")], zero(), zero(), &[]);
    unterminated[4] = b'x';
    assert_eq!(
        Script::decode(&unterminated).unwrap_err().kind,
        DecodeErrorKind::Truncated
    );
    let mut trailing = binary(&[ret()], zero(), zero(), &[]);
    let end = trailing.len() - 2;
    trailing.insert(end, 0);
    let end = trailing.len() - 2;
    trailing[end..].copy_from_slice(&2u16.to_be_bytes());
    assert_eq!(
        Script::decode(&trailing).unwrap_err().kind,
        DecodeErrorKind::TrailingData
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn cp1252_undefined_bytes_branding_and_utf8_limits_are_explicit() {
    let mut bytes = binary(&[text("x")], zero(), zero(), &[]);
    bytes[3] = 0x80;
    let limits = DecodeLimits {
        max_string_bytes: 3,
        ..DecodeLimits::default()
    };
    assert_eq!(
        Script::decode_with_limits(&bytes, limits)
            .unwrap()
            .instructions()[0]
            .operand,
        Operand::String("€".into())
    );
    let limits = DecodeLimits {
        max_string_bytes: 2,
        ..DecodeLimits::default()
    };
    assert_eq!(
        Script::decode_with_limits(&bytes, limits).unwrap_err().kind,
        DecodeErrorKind::StringLimit
    );
    for byte in [0x81, 0x8d, 0x8f, 0x90, 0x9d] {
        bytes[3] = byte;
        assert_eq!(
            Script::decode(&bytes).unwrap().instructions()[0].operand,
            Operand::String("?".into())
        );
    }
    let script = script(&[text("runescape RuneScape Runescape RUNESCAPE rUnEsCaPe")]);
    assert_eq!(
        script.instructions()[0].operand,
        Operand::String("darkan Darkan Darkan RUNESCAPE rUnEsCaPe".into())
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn synthetic_test_strings_use_cp1252_without_silent_loss() {
    let value = "Aé€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸÿ";
    assert_eq!(
        cp1252(value).unwrap(),
        [
            65, 233, 128, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 142, 145, 146,
            147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 158, 159, 255,
        ]
    );
    assert_eq!(
        script(&[text(value), ret()]).instructions()[0].operand,
        Operand::String(value.into())
    );
    assert_eq!(cp1252("\0"), None);
    assert_eq!(cp1252("\u{80}"), None);
    assert_eq!(cp1252("\u{9f}"), None);
    assert_eq!(cp1252("😀"), None);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn switch_tables_keep_signed_keys_offsets_and_first_duplicate_case() {
    let bytes = binary(
        &[int(Opcode::SWITCH, 0), ret()],
        zero(),
        zero(),
        &[
            vec![
                (i32::MIN, -1),
                (-7, i32::MIN),
                (i32::MIN, 20),
                (i32::MAX, i32::MAX),
            ],
            vec![],
        ],
    );
    let script = Script::decode(&bytes).unwrap();
    let tables = script.switches();
    assert_eq!(tables[0].lookup(i32::MIN), Some(-1));
    assert_eq!(tables[0].lookup(-7), Some(i32::MIN));
    assert_eq!(tables[0].lookup(i32::MAX), Some(i32::MAX));
    assert_eq!(tables[0].lookup(0), None);
    assert!(tables[1].cases().is_empty());
    let mut malformed = bytes.clone();
    let table_start = footer(&malformed) + 17;
    malformed[table_start..table_start + 2].copy_from_slice(&u16::MAX.to_be_bytes());
    assert_eq!(
        Script::decode(&malformed).unwrap_err().kind,
        DecodeErrorKind::Truncated
    );
    let mut malformed = binary(&[ret()], zero(), zero(), &[]);
    let table_count = footer(&malformed) + 16;
    malformed[table_count] = 255;
    assert_eq!(
        Script::decode(&malformed).unwrap_err().kind,
        DecodeErrorKind::Truncated
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounded_malformed_byte_corpus_does_not_panic() {
    let mut random = 0x79a4f319u32;
    let mut next = || {
        random ^= random << 13;
        random ^= random >> 17;
        random ^= random << 5;
        random
    };
    for _ in 0..5000 {
        let length = next() as usize % 256;
        let bytes: Vec<_> = (0..length).map(|_| next() as u8).collect();
        let _ = Script::decode(&bytes);
    }
    for _ in 0..2000 {
        let mut bytes = DECODING[0].1.to_vec();
        let at = next() as usize % bytes.len();
        bytes[at] ^= 1 << (next() % 8);
        let _ = Script::decode(&bytes);
    }
}
