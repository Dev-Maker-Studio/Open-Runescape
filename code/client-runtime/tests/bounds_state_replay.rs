#[allow(dead_code)]
#[path = "../../cs2/tests/bounds_cases/mod.rs"]
mod bounds_cases;
#[allow(dead_code)]
#[path = "../../cs2/tests/common/mod.rs"]
mod common;

use openrs_client_runtime::vars::{WorldVarHost, WorldVariables};
use openrs_cs2::*;

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_bounds_case_matches_the_actual_kotlin_dispatch() {
    bounds_cases::replay(|vm, repository, world| {
        let mut variables = WorldVariables::new(4).unwrap();
        let mut host = WorldVarHost::new(&mut variables, 0).with_world_state(world);
        vm.run_with_host(1, repository, Arguments::default(), &mut host)
    });
}
