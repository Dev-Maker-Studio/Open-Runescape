#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::sync::Arc;

struct PlatformHost {
    world: WorldState,
    param: Option<(i32, ParamDefinition)>,
}

impl VarHost for PlatformHost {
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

    fn param_definition(&self, id: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        Ok(match &self.param {
            Some((known, definition)) if *known == id => Some(Arc::new(definition.clone())),
            _ => None,
        })
    }
}

fn decode(text: &str) -> Option<String> {
    if text == "~" {
        return None;
    }
    let digits = &text[1..];
    let bytes: Vec<u8> = (0..digits.len() / 2)
        .map(|i| u8::from_str_radix(&digits[i * 2..i * 2 + 2], 16).unwrap())
        .collect();
    Some(String::from_utf8(bytes).unwrap())
}

fn shared(text: &str) -> Option<Arc<str>> {
    decode(text).map(|value| Arc::from(value.as_str()))
}

fn entry(text: &str) -> Option<MiniMenuEntryState> {
    if text == "~" {
        return None;
    }
    let parts: Vec<&str> = text.split(':').collect();
    Some(MiniMenuEntryState {
        action: shared(parts[0]).unwrap(),
        colored_name: shared(parts[1]).unwrap(),
        target_name: shared(parts[2]),
        click_type: parts[3].parse().unwrap(),
        quest_icons: Arc::from(""),
        ..Default::default()
    })
}

fn parse(text: &str) -> PlatformHost {
    let fields: BTreeMap<&str, &str> = text
        .split('|')
        .map(|field| field.split_once('=').unwrap())
        .collect();
    let flag = |key: &str| fields[key] == "1";
    let mut platform = PlatformState {
        language: (fields["lang"] != "~").then(|| fields["lang"].parse().unwrap()),
        fullscreen_supported: flag("fss"),
        fullscreen_frame: flag("ff"),
        fullscreen_window_mode: flag("fwm"),
        fixed_screen: fields["fixed"].parse().unwrap(),
        hostname_identifier: flag("hid"),
        hostname: shared(fields["host"]),
        clipboard: shared(fields["clip"]),
        game_state: fields["gs"].parse().unwrap(),
        preference_file: shared(fields["pref"]),
        jagtheora_loaded: flag("jt"),
        mini_menu_open: flag("open"),
        active_entry: entry(fields["act"]),
        secondary_entry: entry(fields["sec"]),
        ..Default::default()
    };
    if fields["dp"] != "~" {
        let (finished, path) = fields["dp"].split_once(':').unwrap();
        platform.directory_picker = Some(DirectoryPickerState {
            finished: finished == "1",
            picked: shared(path),
        });
    }
    if fields["mec"] != "~" {
        let parts: Vec<&str> = fields["mec"].split(':').collect();
        let mut params = ParamTable::new();
        if parts[5] != "-" {
            for item in parts[5].split(',') {
                let split = item.find(['i', 's']).unwrap();
                let key = item[..split].parse().unwrap();
                let value = if &item[split..split + 1] == "i" {
                    EnumValue::Int(item[split + 1..].parse().unwrap())
                } else {
                    EnumValue::Text(shared(&item[split + 1..]).unwrap())
                };
                params.insert(key, value);
            }
        }
        platform.mec.insert(
            parts[0].parse().unwrap(),
            MecDefinition {
                area_name: shared(parts[1]),
                default_icon_archive: parts[2].parse().unwrap(),
                category_id: parts[3].parse().unwrap(),
                base_text_zoom: parts[4].parse().unwrap(),
                params,
            },
        );
    }
    let parts: Vec<&str> = fields["pt"].split(':').collect();
    let id: i32 = parts[0].parse().unwrap();
    let param = (id != -1).then(|| {
        (
            id,
            ParamDefinition {
                is_string: parts[1] == "1",
                default_int: parts[2].parse().unwrap(),
                type_name: shared(parts[3]).unwrap(),
            },
        )
    });
    PlatformHost {
        world: WorldState {
            platform,
            ..Default::default()
        },
        param,
    }
}

fn script(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )])
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_platform_case_matches_the_actual_kotlin_classes() {
    let text = include_str!("fixtures/platform.native.tsv");
    let mut cases = 0;
    let mut clicks = 0;
    let mut total = 0;
    let mut errors = 0;
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "Z" => total = c[1].parse::<usize>().unwrap(),
            "K" => {
                clicks += 1;
                let open = c[2] == "1";
                let entry = (c[1] != "-").then(|| c[1].parse().unwrap());
                assert_eq!(
                    mini_menu_click_type(open, entry),
                    c[3].parse::<i32>().unwrap(),
                    "{line}"
                );
            }
            "C" => {
                cases += 1;
                let opcode = OPCODES
                    .iter()
                    .find(|metadata| metadata.name == c[1])
                    .unwrap()
                    .opcode;
                let mut code = Vec::new();
                if c[2] != "-" {
                    for value in c[2].split(',') {
                        code.push(constant(value.parse().unwrap()));
                    }
                }
                code.push(int(opcode, 0));
                code.push(ret());
                let mut host = parse(c[3]);
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
                        let expected: Vec<String> = if c[6] == "-" {
                            Vec::new()
                        } else {
                            c[6].split(',').map(|v| decode(v).unwrap()).collect()
                        };
                        let actual: Vec<String> = vm
                            .string_stack()
                            .iter()
                            .map(|v| v.as_deref().unwrap_or("").to_string())
                            .collect();
                        assert_eq!(actual, expected, "{line}");
                        if c[1] == "GETDIRECTORYPICKERRESULT" {
                            assert_eq!(
                                host.world.platform.directory_picker.is_some(),
                                c[7] == "picker",
                                "{line}"
                            );
                        }
                    }
                    (Err(_), "error") => errors += 1,
                    (actual, expected) => panic!("{line}: {expected} vs {actual:?}"),
                }
            }
            other => panic!("{other}"),
        }
    }
    assert_eq!(cases + clicks, total);
    assert!(errors > 100 && cases > 3000 && clicks > 100);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn injected_quest_icons_follow_the_action_and_name_strings() {
    let entry = MiniMenuEntryState {
        action: Arc::from("Talk-to"),
        colored_name: Arc::from(""),
        target_name: Some(Arc::from("Cook")),
        click_type: 9,
        quest_icons: Arc::from("<img=1>"),
        ..Default::default()
    };
    let mut host = PlatformHost {
        world: WorldState {
            platform: PlatformState {
                active_entry: Some(entry),
                ..Default::default()
            },
            ..Default::default()
        },
        param: None,
    };
    let repository = script(&[int(Opcode::GET_ACTIVE_MINIMENU_ENTRY, 0), ret()]);
    let mut vm = Vm::default();
    vm.run_with_host(1, &repository, Arguments::default(), &mut host)
        .unwrap();
    assert_eq!(vm.int_stack(), &[4]);
    let strings: Vec<&str> = vm
        .string_stack()
        .iter()
        .map(|v| v.as_deref().unwrap())
        .collect();
    assert_eq!(strings, ["Talk-to", "Cook", "<img=1>"]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn platform_opcodes_are_unsupported_without_a_host_state() {
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
    for opcode in [
        Opcode::MAP_LANG,
        Opcode::GETWINDOWMODE,
        Opcode::VIDEO_ADVERT_HAS_FINISHED,
    ] {
        let repository = script(&[int(opcode, 0), ret()]);
        let error = Vm::default()
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
