#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct MenuHost {
    world: WorldState,
}

impl VarHost for MenuHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        Ok(&mut self.world)
    }
}

fn pair(text: &str) -> (i32, bool) {
    let (maximum, bold) = text.split_once(',').unwrap();
    (maximum.parse().unwrap(), bold == "1")
}

fn script(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )])
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_mini_menu_case_matches_the_actual_kotlin_classes() {
    let text = include_str!("fixtures/mini-menu.native.tsv");
    let mut cases = 0;
    let mut total = 0;
    let mut errors = 0;
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "Z" => total = c[1].parse::<usize>().unwrap(),
            "C" => {
                cases += 1;
                let opcode = OPCODES
                    .iter()
                    .find(|metadata| metadata.name == c[1])
                    .unwrap()
                    .opcode;
                let mut code = Vec::new();
                if c[2] != "-" {
                    code.push(constant(c[2].parse().unwrap()));
                }
                code.push(int(opcode, 0));
                code.push(ret());
                let (maximum, bold) = pair(c[3]);
                let mut host = MenuHost {
                    world: WorldState {
                        mini_menu: MiniMenuState {
                            max_visible_entries: maximum,
                            bold_font: bold,
                            ..Default::default()
                        },
                        ..Default::default()
                    },
                };
                let mut vm = Vm::default();
                let result = vm.run_with_host(1, &script(&code), Arguments::default(), &mut host);
                match (result, c[4]) {
                    (Ok(_), "ok") => {
                        let expected: Vec<i32> = if c[5] == "-" {
                            Vec::new()
                        } else {
                            c[5].split(',').map(|v| v.parse().unwrap()).collect()
                        };
                        assert_eq!(vm.int_stack(), expected.as_slice(), "{line}");
                        let (maximum, bold) = pair(c[6]);
                        assert_eq!(host.world.mini_menu.max_visible_entries, maximum, "{line}");
                        assert_eq!(host.world.mini_menu.bold_font, bold, "{line}");
                    }
                    (Err(_), "error") => errors += 1,
                    (actual, expected) => panic!("{line}: {expected} vs {actual:?}"),
                }
            }
            other => panic!("{other}"),
        }
    }
    assert_eq!(cases, total);
    assert!(errors > 0 && cases > 200);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn mini_menu_opcodes_are_unsupported_without_a_host_state() {
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
    let repository = script(&[int(Opcode::DEFAULTMINIMENU, 0), ret()]);
    let error = Vm::default()
        .run_with_host(1, &repository, Arguments::default(), &mut Bare)
        .unwrap_err();
    assert!(matches!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(Opcode::DEFAULTMINIMENU)
    ));
}
