#[allow(dead_code)]
#[path = "../../cs2/tests/common/mod.rs"]
mod common;

use common::*;
use openrs_client_runtime::vars::{WorldVarHost, WorldVariables};
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::sync::Arc;

fn repository(code: &[(Opcode, Operand)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(
        1,
        Script::decode(&binary(code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )])
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_var_host_serves_the_platform_opcodes_from_the_bound_world_state() {
    let mut world = WorldState::default();
    world.platform.language = Some(2);
    world.platform.fullscreen_frame = true;
    world.platform.clipboard = Some(Arc::from("pasted"));
    world.platform.mec.insert(
        4,
        MecDefinition {
            area_name: Some(Arc::from("Lumbridge")),
            category_id: 9,
            ..Default::default()
        },
    );
    let code = [
        int(Opcode::MAP_LANG, 0),
        int(Opcode::GETWINDOWMODE, 0),
        constant(4),
        int(Opcode::MEC_CATEGORY, 0),
        constant(4),
        int(Opcode::MEC_TEXT, 0),
        int(Opcode::GETCLIPBOARD, 0),
        int(Opcode::VIDEO_ADVERT_HAS_FINISHED, 0),
        ret(),
    ];
    let mut vm = Vm::default();
    let mut variables = WorldVariables::new(4).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0).with_world_state(&mut world);
    vm.run_with_host(1, &repository(&code), Arguments::default(), &mut host)
        .unwrap();
    assert_eq!(vm.int_stack(), &[2, 3, 9, 1]);
    let strings: Vec<&str> = vm
        .string_stack()
        .iter()
        .map(|text| text.as_deref().unwrap())
        .collect();
    assert_eq!(strings, ["Lumbridge", "pasted"]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_var_host_without_a_bound_world_state_reports_platform_opcodes_unsupported() {
    let mut vm = Vm::default();
    let mut variables = WorldVariables::new(4).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0);
    let error = vm
        .run_with_host(
            1,
            &repository(&[int(Opcode::MAP_LANG, 0), ret()]),
            Arguments::default(),
            &mut host,
        )
        .unwrap_err();
    assert!(matches!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(Opcode::MAP_LANG)
    ));
}
