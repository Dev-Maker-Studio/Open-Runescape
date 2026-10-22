#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::{collections::BTreeMap, sync::Arc};

const OPERATIONS: &[Opcode] = &[
    Opcode::STRINGWIDTH,
    Opcode::PARAWIDTH,
    Opcode::PARAHEIGHT,
    Opcode::PARAHEIGHT_EXTRA,
];

#[derive(Default)]
struct FontHost {
    fonts: BTreeMap<i32, Arc<FontMetrics>>,
    icons: Option<Arc<[i32]>>,
    // id -> (frames, width, height)
    sprites: BTreeMap<i32, (i32, i32, i32)>,
}

impl VarHost for FontHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn font_metrics(&self, id: i32) -> Result<Option<Arc<FontMetrics>>, HostError> {
        Ok(self.fonts.get(&id).cloned())
    }

    fn name_icon_widths(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        Ok(self.icons.clone())
    }

    fn inline_graphic_size(&self, id: i32, frame: i32) -> Result<Option<(i32, i32)>, HostError> {
        Ok(self
            .sprites
            .get(&id)
            .filter(|(frames, _, _)| (0..*frames).contains(&frame))
            .map(|(_, width, height)| (*width, *height)))
    }
}

struct Bare;

impl VarHost for Bare {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }
}

fn utf16(value: &str) -> Option<String> {
    if value == "null" {
        return None;
    }
    let units: Vec<u16> = (0..(value.len() - 1) / 4)
        .map(|index| u16::from_str_radix(&value[1 + index * 4..5 + index * 4], 16).unwrap())
        .collect();
    Some(String::from_utf16(&units).unwrap())
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(|n| n.parse().unwrap()).collect()
    }
}

fn bytes(value: &str) -> Vec<u8> {
    if value == "-" {
        return Vec::new();
    }
    (0..value.len() / 2)
        .map(|index| u8::from_str_radix(&value[index * 2..index * 2 + 2], 16).unwrap())
        .collect()
}

fn host() -> FontHost {
    let mut host = FontHost::default();
    let mut icons = Vec::new();
    for line in include_str!("fixtures/fonts.table.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        match columns[0] {
            "font" => {
                assert_eq!(columns.len(), 8, "{line}");
                let decoded = FontMetrics::decode(&bytes(columns[2])).expect(columns[1]);
                assert_eq!(decoded.glyph_widths, bytes(columns[6]), "font {}", columns[1]);
                assert_eq!(decoded.vertical_spacing, columns[3].parse::<i32>().unwrap());
                assert_eq!(decoded.top_padding, columns[4].parse::<i32>().unwrap());
                assert_eq!(decoded.bottom_padding, columns[5].parse::<i32>().unwrap());
                let kerning: Option<Vec<i8>> = (columns[7] != "-")
                    .then(|| bytes(columns[7]).into_iter().map(|b| b as i8).collect());
                assert_eq!(decoded.kerning, kerning, "font {} kerning", columns[1]);
                host.fonts.insert(columns[1].parse().unwrap(), Arc::new(decoded));
            }
            "icon" => icons.push(columns[2].parse().unwrap()),
            "sprite" => {
                host.sprites.insert(
                    columns[1].parse().unwrap(),
                    (
                        columns[2].parse().unwrap(),
                        columns[3].parse().unwrap(),
                        columns[4].parse().unwrap(),
                    ),
                );
            }
            other => panic!("unexpected table row {other}"),
        }
    }
    host.icons = Some(icons.into());
    host
}

fn run(
    host: &mut dyn VarHost,
    code: &[(Opcode, Operand)],
    strings: &[Option<Arc<str>>],
) -> (Vm, Result<(), VmError>) {
    let arguments = counts(0, strings.len() as u16, 0);
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(code, arguments, arguments, &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(
            1,
            &repository,
            Arguments {
                ints: &[],
                strings,
                longs: &[],
            },
            host,
        )
        .map(|_| ());
    (vm, result)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_font_measuring_case_matches_the_actual_kotlin_dispatch() {
    let mut host = host();
    assert_eq!(host.fonts.len(), 30);
    let mut total = 0;
    let mut failing = 0;
    for line in include_str!("fixtures/fonts.native.tsv").lines() {
        let columns: Vec<_> = line.split('\t').collect();
        assert_eq!(columns.len(), 7, "{line}");
        let name = columns[0];
        let opcode = OPERATIONS
            .iter()
            .copied()
            .find(|opcode| opcode.metadata().name == columns[1])
            .unwrap();
        let input: Option<Arc<str>> = utf16(columns[3]).map(Arc::from);
        let mut code: Vec<_> = integers(columns[2]).into_iter().map(constant).collect();
        code.push(int(Opcode::PUSH_STRING_LOCAL, 0));
        let pushed = code.len();
        code.extend([int(opcode, 0), ret()]);
        let (vm, result) = run(&mut host, &code, &[input]);
        match columns[4] {
            "ok" => {
                result.unwrap_or_else(|error| panic!("{name}: {error:?}"));
            }
            error if error.starts_with("error:") => {
                let error = result.unwrap_err();
                assert_eq!(error.instruction, pushed, "{name}");
                assert_eq!(error.kind, VmErrorKind::InvalidOperand, "{name}");
                failing += 1;
            }
            other => panic!("{name}: unexpected {other}"),
        }
        assert_eq!(vm.int_stack(), integers(columns[5]).as_slice(), "{name}");
        let expected: Vec<Option<Arc<str>>> = if columns[6] == "-" {
            Vec::new()
        } else {
            columns[6]
                .split(',')
                .map(|value| utf16(value).map(Arc::from))
                .collect()
        };
        assert_eq!(vm.string_stack(), expected.as_slice(), "{name}");
        total += 1;
    }
    assert_eq!(total, include_str!("fixtures/fonts.native.tsv").lines().count());
    assert!(total > 17000);
    assert_eq!(failing, 141);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn font_operations_without_a_font_host_stay_unsupported() {
    let strings = [Some(Arc::<str>::from("text"))];
    for opcode in OPERATIONS {
        let (_, result) = run(
            &mut Bare,
            &[
                constant(1),
                constant(2),
                int(Opcode::PUSH_STRING_LOCAL, 0),
                int(*opcode, 0),
                ret(),
            ],
            &strings,
        );
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
        let repository = BTreeMap::from([(1, script(&[int(*opcode, 0), ret()]))]);
        let error = Vm::default().run(1, &repository, Arguments::default());
        assert_eq!(
            error.unwrap_err().kind,
            VmErrorKind::UnsupportedOpcode(*opcode)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn font_operations_report_underflow_when_the_host_is_available() {
    let strings = [Some(Arc::<str>::from("text"))];
    for opcode in OPERATIONS {
        let (_, result) = run(&mut host(), &[int(*opcode, 0), ret()], &strings);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::Int)
        );
        let (_, result) = run(&mut host(), &[constant(1), constant(494), int(*opcode, 0), ret()], &[]);
        assert_eq!(
            result.unwrap_err().kind,
            VmErrorKind::StackUnderflow(StackKind::String)
        );
    }
}
