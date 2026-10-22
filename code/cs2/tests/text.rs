#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

fn arity(opcode: Opcode) -> Counts {
    use Opcode::*;
    match opcode {
        APPEND => counts(0, 2, 0),
        APPEND_NUM | APPEND_SIGNNUM | APPEND_CHAR => counts(1, 1, 0),
        GET_COL_TAG | TOSTRING | CHAR_ISPRINTABLE | CHAR_ISALPHANUMERIC | CHAR_ISALPHA
        | CHAR_ISNUMERIC | CHAR_TOLOWERCASE | CHAR_TOUPPERCASE => counts(1, 0, 0),
        TEXT_SWITCH | STRING_INDEXOF_STRING => counts(1, 2, 0),
        ESCAPE | STRING_LENGTH | REMOVETAGS => counts(0, 1, 0),
        SUBSTRING | STRING_INDEXOF_CHAR => counts(2, 1, 0),
        CLANFORUMQFC_TOSTRING => counts(0, 0, 1),
        _ => panic!("{opcode:?}"),
    }
}

fn returns_text(opcode: Opcode) -> bool {
    use Opcode::*;
    matches!(
        opcode,
        APPEND_NUM
            | APPEND
            | APPEND_SIGNNUM
            | GET_COL_TAG
            | TOSTRING
            | TEXT_SWITCH
            | ESCAPE
            | APPEND_CHAR
            | SUBSTRING
            | REMOVETAGS
            | CLANFORUMQFC_TOSTRING
    )
}

fn loads(arguments: Counts) -> Vec<(Opcode, Operand)> {
    let mut code = Vec::new();
    for (opcode, count) in [
        (Opcode::PUSH_INT_LOCAL, arguments.ints),
        (Opcode::PUSH_STRING_LOCAL, arguments.strings),
        (Opcode::PUSH_LONG_LOCAL, arguments.longs),
    ] {
        for index in 0..count {
            code.push(int(opcode, i32::from(index)));
        }
    }
    code
}

fn invocation(opcode: Opcode) -> Script {
    let counts = arity(opcode);
    let mut code = loads(counts);
    code.extend([int(opcode, 0), ret()]);
    Script::decode(&binary(&code, counts, counts, &[])).unwrap()
}

fn invoke(
    opcode: Opcode,
    arguments: Arguments<'_>,
    limits: VmLimits,
) -> (Vm, Result<Execution, VmError>) {
    let mut vm = Vm::new(limits);
    let result = vm.run(1, &BTreeMap::from([(1, invocation(opcode))]), arguments);
    (vm, result)
}

fn units(value: &str) -> Option<Vec<u16>> {
    if value == "null" {
        return None;
    }
    let hex = value.strip_prefix('u').unwrap();
    assert_eq!(hex.len() % 4, 0);
    Some(
        hex.as_bytes()
            .chunks_exact(4)
            .map(|chunk| u16::from_str_radix(std::str::from_utf8(chunk).unwrap(), 16).unwrap())
            .collect(),
    )
}

fn strings(value: &str) -> Vec<Option<Arc<str>>> {
    if value == "-" {
        return Vec::new();
    }
    value
        .split(',')
        .map(|value| units(value).map(|units| Arc::from(String::from_utf16(&units).unwrap())))
        .collect()
}

fn integers<T: std::str::FromStr>(value: &str) -> Vec<T>
where
    T::Err: std::fmt::Debug,
{
    if value == "-" {
        return Vec::new();
    }
    value
        .split(',')
        .map(|value| value.parse().unwrap())
        .collect()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn string_results_and_exceptional_inputs_match_the_existing_native_jar_losslessly() {
    let repository: BTreeMap<_, _> = TEXT_OPERATIONS
        .iter()
        .map(|opcode| (i32::from(opcode.wire()), invocation(*opcode)))
        .collect();
    let mut vm = Vm::default();
    let mut seen = Vec::new();
    let mut guarded = Vec::new();
    let mut cases = 0;
    for line in include_str!("fixtures/strings.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 5);
        let opcode = OPCODES
            .iter()
            .find(|entry| entry.name == columns[0])
            .unwrap()
            .opcode;
        let id = i32::from(opcode.wire());
        let ints = integers::<i32>(columns[1]);
        let strings = strings(columns[2]);
        let longs = integers::<i64>(columns[3]);
        let arguments = Arguments {
            ints: &ints,
            strings: &strings,
            longs: &longs,
        };
        let result = vm.run(id, &repository, arguments);
        match columns[4].split_once(':').unwrap() {
            ("error", exception) => {
                assert!(matches!(
                    exception,
                    "RuntimeException" | "NullPointerException" | "StringIndexOutOfBoundsException"
                ));
                let error = result.unwrap_err();
                assert_eq!(error.script_id, id, "{line}");
                assert_eq!(
                    error.instruction,
                    usize::from(arity(opcode).ints + arity(opcode).strings + arity(opcode).longs),
                    "{line}"
                );
                assert_eq!(error.kind, VmErrorKind::InvalidOperand, "{line}");
            }
            ("int", expected) => {
                result.unwrap();
                assert_eq!(
                    vm.int_stack(),
                    &[expected.parse::<i32>().unwrap()],
                    "{line}"
                );
                assert!(vm.string_stack().is_empty(), "{line}");
            }
            ("string", expected) => {
                let decoded = units(expected).map(|units| String::from_utf16(&units));
                match decoded {
                    Some(Err(_)) => {
                        assert!(matches!(opcode, Opcode::SUBSTRING | Opcode::APPEND_CHAR));
                        assert_eq!(
                            result.unwrap_err().kind,
                            VmErrorKind::UnsupportedOpcode(opcode),
                            "{line}"
                        );
                        if !guarded.contains(&opcode) {
                            guarded.push(opcode);
                        }
                    }
                    value => {
                        let expected = value.map(Result::unwrap);
                        result.unwrap();
                        assert!(vm.int_stack().is_empty(), "{line}");
                        assert_eq!(vm.string_stack().len(), 1, "{line}");
                        assert_eq!(
                            vm.string_stack()[0].as_deref(),
                            expected.as_deref(),
                            "{line}"
                        );
                    }
                }
            }
            other => panic!("{other:?}"),
        }
        assert!(vm.long_stack().is_empty(), "{line}");
        if !seen.contains(&opcode) {
            seen.push(opcode);
        }
        cases += 1;
    }
    assert_eq!(cases, 8867);
    assert_eq!(seen.len(), 20);
    assert_eq!(guarded.len(), 2);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_65536_utf16_units_match_native_character_checks_and_simple_case_tables() {
    use Opcode::*;
    let operations = [
        CHAR_TOLOWERCASE,
        CHAR_TOUPPERCASE,
        CHAR_ISPRINTABLE,
        CHAR_ISALPHANUMERIC,
        CHAR_ISALPHA,
        CHAR_ISNUMERIC,
    ];
    let mut code = Vec::new();
    for opcode in operations {
        code.extend([int(PUSH_INT_LOCAL, 0), int(opcode, 0)]);
    }
    code.push(ret());
    let counts = counts(1, 0, 0);
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts, counts, &[])).unwrap(),
    )]);
    let oracle = include_bytes!("fixtures/characters.native.bin");
    assert_eq!(oracle.len(), 65536 * 5);
    let mut vm = Vm::default();
    for (unit, expected) in oracle.chunks_exact(5).enumerate() {
        let arguments = [unit as i32];
        let execution = vm
            .run(
                1,
                &repository,
                Arguments {
                    ints: &arguments,
                    ..Arguments::default()
                },
            )
            .unwrap();
        assert_eq!(execution.instructions_executed, 13);
        assert_eq!(execution.joined_string_bytes, 0);
        let lower = i32::from(u16::from_be_bytes([expected[0], expected[1]]));
        let upper = i32::from(u16::from_be_bytes([expected[2], expected[3]]));
        assert_eq!(
            vm.int_stack(),
            &[
                lower,
                upper,
                i32::from(expected[4] & 1),
                i32::from(expected[4] >> 1 & 1),
                i32::from(expected[4] >> 2 & 1),
                i32::from(expected[4] >> 3 & 1)
            ],
            "unit={unit}"
        );
        assert!(vm.string_stack().is_empty());
        assert!(vm.long_stack().is_empty());
    }
    for (field, table) in [
        (0, include_bytes!("../src/vm/char-lower.bin").as_slice()),
        (2, include_bytes!("../src/vm/char-upper.bin").as_slice()),
    ] {
        assert_eq!(table.len() % 4, 0);
        let count = oracle
            .chunks_exact(5)
            .enumerate()
            .filter(|(unit, row)| {
                usize::from(u16::from_be_bytes([row[field], row[field + 1]])) != *unit
            })
            .count();
        assert_eq!(table.len() / 4, count);
        let mut previous = None;
        for entry in table.chunks_exact(4) {
            let unit = u16::from_be_bytes([entry[0], entry[1]]);
            let mapped = u16::from_be_bytes([entry[2], entry[3]]);
            assert!(previous.is_none_or(|previous| previous < unit));
            assert_ne!(unit, mapped);
            let start = usize::from(unit) * 5 + field;
            assert_eq!(
                mapped,
                u16::from_be_bytes([oracle[start], oracle[start + 1]])
            );
            previous = Some(unit);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn utf16_boundaries_empty_substrings_and_isolated_surrogate_guards_are_explicit() {
    use Opcode::*;
    let texts = [Some(Arc::from("a😀b"))];
    for (bounds, expected) in [
        ([0, 4], "a😀b"),
        ([1, 3], "😀"),
        ([3, 4], "b"),
        ([2, 2], ""),
        ([4, 4], ""),
    ] {
        let (vm, result) = invoke(
            SUBSTRING,
            Arguments {
                ints: &bounds,
                strings: &texts,
                longs: &[],
            },
            VmLimits::default(),
        );
        result.unwrap();
        assert_eq!(vm.string_stack()[0].as_deref(), Some(expected));
    }
    for bounds in [[0, 2], [1, 2], [2, 3], [2, 4]] {
        assert_eq!(
            invoke(
                SUBSTRING,
                Arguments {
                    ints: &bounds,
                    strings: &texts,
                    longs: &[]
                },
                VmLimits::default()
            )
            .1
            .unwrap_err()
            .kind,
            VmErrorKind::UnsupportedOpcode(SUBSTRING)
        );
    }
    for bounds in [[-1, 0], [0, 5], [3, 2], [5, 5]] {
        assert_eq!(
            invoke(
                SUBSTRING,
                Arguments {
                    ints: &bounds,
                    strings: &texts,
                    longs: &[]
                },
                VmLimits::default()
            )
            .1
            .unwrap_err()
            .kind,
            VmErrorKind::InvalidOperand
        );
    }
    for unit in [0xd800, 0xdbff, 0xdc00, 0xdfff, 0x1d800, -10240] {
        assert_eq!(
            invoke(
                APPEND_CHAR,
                Arguments {
                    ints: &[unit],
                    strings: &texts,
                    longs: &[]
                },
                VmLimits::default()
            )
            .1
            .unwrap_err()
            .kind,
            VmErrorKind::UnsupportedOpcode(APPEND_CHAR)
        );
    }
    for (unit, suffix) in [
        (0, "\0"),
        (0x10041, "A"),
        (-65536, "\0"),
        (0xffff, "\u{ffff}"),
    ] {
        let (vm, result) = invoke(
            APPEND_CHAR,
            Arguments {
                ints: &[unit],
                strings: &texts,
                longs: &[],
            },
            VmLimits::default(),
        );
        result.unwrap();
        assert_eq!(
            vm.string_stack()[0].as_deref(),
            Some(format!("a😀b{suffix}").as_str())
        );
    }
    for (unit, from, expected) in [
        (0xd83d, 0, 1),
        (0xde00, 0, 2),
        (0xd83d, 2, -1),
        (0xde00, 2, 2),
        (0x1de00, -9, 2),
        (98, 2, 3),
    ] {
        let (vm, result) = invoke(
            STRING_INDEXOF_CHAR,
            Arguments {
                ints: &[unit, from],
                strings: &texts,
                longs: &[],
            },
            VmLimits::default(),
        );
        result.unwrap();
        assert_eq!(vm.int_stack(), &[expected]);
    }
    for (needle, from, expected) in [
        ("😀", 0, 1),
        ("😀", 2, -1),
        ("b", 2, 3),
        ("", 2, 2),
        ("", i32::MAX, 4),
        ("a", i32::MIN, 0),
    ] {
        let strings = [texts[0].clone(), Some(Arc::from(needle))];
        let (vm, result) = invoke(
            STRING_INDEXOF_STRING,
            Arguments {
                ints: &[from],
                strings: &strings,
                longs: &[],
            },
            VmLimits::default(),
        );
        result.unwrap();
        assert_eq!(vm.int_stack(), &[expected]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_allocating_text_operation_obeys_existing_utf8_and_cumulative_budgets() {
    use Opcode::*;
    type AllocationCase<'a> = (Opcode, &'a [i32], &'a [&'a str], &'a [i64], &'a str);
    let cases: &[AllocationCase<'_>] = &[
        (APPEND, &[], &["é", "€"], &[], "é€"),
        (APPEND_NUM, &[123], &["é"], &[], "é123"),
        (APPEND_SIGNNUM, &[0], &["é"], &[], "é+0"),
        (GET_COL_TAG, &[-1], &[], &[], "<col=ffffffff>"),
        (TOSTRING, &[i32::MIN], &[], &[], "-2147483648"),
        (ESCAPE, &[], &["<"], &[], "<lt>"),
        (APPEND_CHAR, &[0x20ac], &["é"], &[], "é€"),
        (SUBSTRING, &[0, 2], &["é€"], &[], "é€"),
        (REMOVETAGS, &[], &["<x>é</x>"], &[], "é"),
        (CLANFORUMQFC_TOSTRING, &[], &[], &[0], "0"),
    ];
    for (opcode, ints, texts, longs, expected) in cases {
        let strings: Vec<_> = texts.iter().map(|text| Some(Arc::from(*text))).collect();
        let args = Arguments {
            ints,
            strings: &strings,
            longs,
        };
        let input_length = texts.iter().map(|text| text.len()).max().unwrap_or(0);
        let limit = input_length.max(expected.len());
        let limits = VmLimits {
            max_string_bytes: limit,
            max_join_bytes: expected.len(),
            ..VmLimits::default()
        };
        let (vm, execution) = invoke(*opcode, args, limits);
        assert_eq!(
            execution.unwrap().joined_string_bytes,
            expected.len(),
            "{opcode:?}"
        );
        assert_eq!(vm.string_stack()[0].as_deref(), Some(*expected));
        assert_eq!(
            invoke(
                *opcode,
                args,
                VmLimits {
                    max_join_bytes: expected.len() - 1,
                    ..limits
                }
            )
            .1
            .unwrap_err()
            .kind,
            VmErrorKind::StringLimit,
            "{opcode:?}"
        );
        assert_eq!(
            invoke(
                *opcode,
                args,
                VmLimits {
                    max_string_bytes: expected.len() - 1,
                    ..limits
                }
            )
            .1
            .unwrap_err()
            .kind,
            VmErrorKind::StringLimit,
            "{opcode:?}"
        );
        let counts = arity(*opcode);
        let mut code = loads(counts);
        code.extend([int(*opcode, 0), int(POP_STRING_DISCARD, 0)]);
        code.extend(loads(counts));
        code.extend([int(*opcode, 0), ret()]);
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts, counts, &[])).unwrap(),
        )]);
        let mut vm = Vm::new(VmLimits {
            max_join_bytes: expected.len() * 2 - 1,
            ..limits
        });
        assert_eq!(
            vm.run(1, &repository, args).unwrap_err().kind,
            VmErrorKind::StringLimit,
            "{opcode:?}"
        );
        let mut vm = Vm::new(VmLimits {
            max_join_bytes: expected.len() * 2,
            ..limits
        });
        assert_eq!(
            vm.run(1, &repository, args).unwrap().joined_string_bytes,
            expected.len() * 2,
            "{opcode:?}"
        );
    }
    let repository = BTreeMap::from([(
        1,
        script(&[
            text("é"),
            constant(0x20ac),
            int(APPEND_CHAR, 0),
            int(JOIN_STRING, 1),
            ret(),
        ]),
    )]);
    assert_eq!(
        Vm::new(VmLimits {
            max_join_bytes: 9,
            ..VmLimits::default()
        })
        .run(1, &repository, Arguments::default())
        .unwrap_err()
        .kind,
        VmErrorKind::StringLimit
    );
    assert_eq!(
        Vm::new(VmLimits {
            max_join_bytes: 10,
            ..VmLimits::default()
        })
        .run(1, &repository, Arguments::default())
        .unwrap()
        .joined_string_bytes,
        10
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn text_selection_preserves_null_and_does_not_consume_allocation_budget() {
    let strings = [Some(Arc::from("😀")), None];
    for (flag, expected) in [(1, Some("😀")), (0, None), (-1, None), (2, None)] {
        let (vm, result) = invoke(
            Opcode::TEXT_SWITCH,
            Arguments {
                ints: &[flag],
                strings: &strings,
                longs: &[],
            },
            VmLimits {
                max_join_bytes: 0,
                ..VmLimits::default()
            },
        );
        assert_eq!(result.unwrap().joined_string_bytes, 0);
        assert_eq!(vm.string_stack()[0].as_deref(), expected);
    }
    for value in [None, Some(Arc::from(""))] {
        let (vm, result) = invoke(
            Opcode::STRING_LENGTH,
            Arguments {
                strings: &[value],
                ..Arguments::default()
            },
            VmLimits {
                max_string_bytes: 0,
                max_join_bytes: 0,
                ..VmLimits::default()
            },
        );
        assert_eq!(result.unwrap().joined_string_bytes, 0);
        assert_eq!(vm.int_stack(), &[0]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_text_operation_checks_arity_and_preserves_lower_values_of_all_types() {
    for opcode in TEXT_OPERATIONS {
        let required = arity(*opcode);
        for (kind, required_count) in [
            (StackKind::Int, required.ints),
            (StackKind::String, required.strings),
            (StackKind::Long, required.longs),
        ] {
            if required_count == 0 {
                continue;
            }
            let mut code = Vec::new();
            for _ in 0..required.ints - u16::from(kind == StackKind::Int) {
                code.push(constant(0));
            }
            for _ in 0..required.strings - u16::from(kind == StackKind::String) {
                code.push(text("abc"));
            }
            for _ in 0..required.longs - u16::from(kind == StackKind::Long) {
                code.push(long(0));
            }
            code.extend([int(*opcode, 0), ret()]);
            let error = Vm::default()
                .run(
                    1,
                    &BTreeMap::from([(1, script(&code))]),
                    Arguments::default(),
                )
                .unwrap_err();
            assert_eq!(error.kind, VmErrorKind::StackUnderflow(kind), "{opcode:?}");
        }
        let mut code = vec![constant(123456), text("sentinel"), long(234567)];
        for _ in 0..required.ints {
            code.push(constant(0));
        }
        for _ in 0..required.strings {
            code.push(text("abc"));
        }
        for _ in 0..required.longs {
            code.push(long(0));
        }
        code.extend([int(*opcode, 0), ret()]);
        let mut vm = Vm::default();
        vm.run(
            1,
            &BTreeMap::from([(1, script(&code))]),
            Arguments::default(),
        )
        .unwrap();
        assert_eq!(vm.int_stack()[0], 123456, "{opcode:?}");
        assert_eq!(
            vm.string_stack()[0].as_deref(),
            Some("sentinel"),
            "{opcode:?}"
        );
        assert_eq!(vm.long_stack(), &[234567], "{opcode:?}");
        assert_eq!(
            vm.int_stack().len(),
            1 + usize::from(!returns_text(*opcode)),
            "{opcode:?}"
        );
        assert_eq!(
            vm.string_stack().len(),
            1 + usize::from(returns_text(*opcode)),
            "{opcode:?}"
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn text_operations_replace_full_stacks_without_growth_and_reject_new_full_stack_results() {
    for opcode in TEXT_OPERATIONS {
        let mut code = vec![constant(0); 1000];
        code.extend(vec![text("abc"); 1000]);
        code.extend(vec![long(0); 1000]);
        code.extend([int(*opcode, 0), ret()]);
        let mut vm = Vm::default();
        let result = vm.run(
            1,
            &BTreeMap::from([(1, script(&code))]),
            Arguments::default(),
        );
        let args = arity(*opcode);
        if returns_text(*opcode) && args.strings == 0 {
            assert_eq!(
                result.unwrap_err().kind,
                VmErrorKind::StackOverflow(StackKind::String),
                "{opcode:?}"
            );
        } else if !returns_text(*opcode) && args.ints == 0 {
            assert_eq!(
                result.unwrap_err().kind,
                VmErrorKind::StackOverflow(StackKind::Int),
                "{opcode:?}"
            );
        } else {
            result.unwrap();
            assert_eq!(
                vm.int_stack().len(),
                1000 - usize::from(args.ints) + usize::from(!returns_text(*opcode)),
                "{opcode:?}"
            );
            assert_eq!(
                vm.string_stack().len(),
                1000 - usize::from(args.strings) + usize::from(returns_text(*opcode)),
                "{opcode:?}"
            );
            assert_eq!(
                vm.long_stack().len(),
                1000 - usize::from(args.longs),
                "{opcode:?}"
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn each_text_opcode_consumes_one_fuel_step_and_deferred_unicode_locale_host_ops_stay_unsupported() {
    use Opcode::*;
    for opcode in TEXT_OPERATIONS {
        let counts = arity(*opcode);
        let ints = vec![0; usize::from(counts.ints)];
        let strings = vec![Some(Arc::from("abc")); usize::from(counts.strings)];
        let longs = vec![0; usize::from(counts.longs)];
        let args = Arguments {
            ints: &ints,
            strings: &strings,
            longs: &longs,
        };
        let steps = u64::from(counts.ints + counts.strings + counts.longs) + 2;
        let (_, result) = invoke(
            *opcode,
            args,
            VmLimits {
                max_instructions: steps,
                ..VmLimits::default()
            },
        );
        assert_eq!(result.unwrap().instructions_executed, steps, "{opcode:?}");
        let (_, result) = invoke(
            *opcode,
            args,
            VmLimits {
                max_instructions: steps - 1,
                ..VmLimits::default()
            },
        );
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::InstructionLimit,
            "{opcode:?}"
        );
    }
    for opcode in [
        COMPARE,
        TOSTRING_LOCALISED,
        TEXT_GENDER,
        PARAHEIGHT,
        PARAWIDTH,
        STRINGWIDTH,
        FORMAT_DATETIME_FROM_MINUTES,
    ] {
        let error = Vm::default()
            .run(
                1,
                &BTreeMap::from([(1, script(&[int(opcode, 0), ret()]))]),
                Arguments::default(),
            )
            .unwrap_err();
        assert_eq!(error.kind, VmErrorKind::UnsupportedOpcode(opcode));
    }
}
