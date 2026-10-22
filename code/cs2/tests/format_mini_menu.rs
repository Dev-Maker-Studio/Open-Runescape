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

fn numbers(text: &str) -> Vec<i32> {
    text.split(',').map(|v| v.parse().unwrap()).collect()
}

fn style_of(values: &[i32]) -> MiniMenuStyle {
    MiniMenuStyle {
        background_color: values[0],
        background_transparency: values[1],
        hover_highlight_color: values[2],
        background_alpha: values[3],
        loading_bar_background_sprite: values[4],
        loading_bar_tile_sprite: values[5],
        top_border_sprite: values[6],
        side_border_sprite: values[7],
        corner_sprite: values[8],
        text_color: values[9],
        hover_text_color: values[10],
        font_index: values[11],
    }
}

fn state_text(world: &WorldState) -> String {
    let s = world.mini_menu.style;
    let flags = [
        i32::from(world.platform.mini_menu_open),
        i32::from(world.platform.active_entry.is_some()),
        i32::from(world.platform.secondary_entry.is_some()),
        i32::from(world.mini_menu.bold_font),
    ];
    let mut all: Vec<i32> = flags.to_vec();
    all.extend([
        s.background_color,
        s.background_transparency,
        s.hover_highlight_color,
        s.background_alpha,
        s.loading_bar_background_sprite,
        s.loading_bar_tile_sprite,
        s.top_border_sprite,
        s.side_border_sprite,
        s.corner_sprite,
        s.text_color,
        s.hover_text_color,
        s.font_index,
    ]);
    all.iter().map(i32::to_string).collect::<Vec<_>>().join(",")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn format_mini_menu_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/format-mini-menu.native.tsv");
    let mut cases = 0;
    let mut total = 0;
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "Z" => total = c[1].parse::<usize>().unwrap(),
            "C" => {
                cases += 1;
                let given = numbers(c[1]);
                let pre = numbers(c[2]);
                let mut host = MenuHost {
                    world: WorldState::default(),
                };
                host.world.platform.mini_menu_open = pre[0] == 1;
                if pre[1] == 1 {
                    host.world.platform.active_entry = Some(MiniMenuEntryState::default());
                }
                if pre[2] == 1 {
                    host.world.platform.secondary_entry = Some(MiniMenuEntryState::default());
                }
                host.world.mini_menu.bold_font = pre[3] == 1;
                host.world.mini_menu.style = style_of(&pre[4..]);
                let mut code: Vec<_> = given.iter().map(|v| constant(*v)).collect();
                code.push(int(Opcode::FORMATMINIMENU, 0));
                code.push(ret());
                let repository = BTreeMap::from([(
                    1,
                    Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
                )]);
                let mut vm = Vm::default();
                let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
                assert_eq!(c[3], "ok", "{line}");
                assert!(result.is_ok(), "{line}: {result:?}");
                assert!(vm.int_stack().is_empty(), "{line}");
                assert_eq!(state_text(&host.world), c[5], "{line}");
            }
            other => panic!("{other}"),
        }
    }
    assert_eq!(cases, total);
    assert_eq!(cases, 400);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn format_mini_menu_is_unsupported_without_a_host_state() {
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
    let mut code: Vec<_> = (0..12).map(constant).collect();
    code.push(int(Opcode::FORMATMINIMENU, 0));
    code.push(ret());
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let error = Vm::default()
        .run_with_host(1, &repository, Arguments::default(), &mut Bare)
        .unwrap_err();
    assert!(matches!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(Opcode::FORMATMINIMENU)
    ));
}
