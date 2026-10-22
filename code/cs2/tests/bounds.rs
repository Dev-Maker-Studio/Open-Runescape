#[allow(dead_code)]
mod common;
#[allow(dead_code)]
mod bounds_cases;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct BoundsHost {
    world: WorldState,
}

impl VarHost for BoundsHost {
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

fn run_script(opcode: Opcode, world: WorldState) -> (Vm, Result<Execution, VmError>, WorldState) {
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&[int(opcode, 0), ret()], counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut host = BoundsHost { world };
    let mut vm = Vm::default();
    let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
    (vm, result, host.world)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_bounds_case_matches_the_actual_kotlin_dispatch() {
    bounds_cases::replay(|vm, repository, world| {
        let mut host = BoundsHost {
            world: std::mem::take(world),
        };
        let result = vm.run_with_host(1, repository, Arguments::default(), &mut host);
        *world = host.world;
        result
    });
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounding_box_pushes_found_flag_then_the_extent() {
    let mut world = WorldState::default();
    world.projection.bounds.entity = Some(BoxSet {
        polygons: Some(vec![
            Some(BoundingBox {
                calculated: true,
                left: 10,
                right: 4,
                top: 1,
                bottom: 9,
                size: 2,
            }),
            Some(BoundingBox {
                calculated: false,
                left: -500,
                right: 500,
                top: -500,
                bottom: 500,
                size: 0,
            }),
        ]),
    });
    let (vm, result, _) = run_script(Opcode::GET_ENTITY_BOUNDING_BOX, world);
    result.unwrap();
    assert_eq!(vm.int_stack(), &[1, 2, -1, 12, 11]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounding_box_without_calculated_polygons_pushes_zeros() {
    let mut world = WorldState::default();
    world.projection.bounds.obj = Some(BoxSet { polygons: None });
    let (vm, result, _) = run_script(Opcode::GET_OBJ_BOUNDING_BOX, world);
    result.unwrap();
    assert_eq!(vm.int_stack(), &[0, 0, 0, 0, 0]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn loc_overlay_height_caches_the_model_bounds() {
    let mut world = WorldState::default();
    world.projection.bounds.loc_height = Some(LocHeight::Model(Some(ModelBounds {
        bounds_updated: false,
        min_y: 0,
        vertex_count: 3,
        vertices_x: Some(vec![0; 3]),
        vertices_y: Some(vec![40, -90, 12]),
        vertices_z: Some(vec![0; 3]),
    })));
    let (vm, result, world) = run_script(Opcode::GET_LOC_OVERLAY_HEIGHT, world);
    result.unwrap();
    assert_eq!(vm.int_stack(), &[90]);
    let Some(LocHeight::Model(Some(model))) = world.projection.bounds.loc_height else {
        panic!("model state lost");
    };
    assert!(model.bounds_updated);
    assert_eq!(model.min_y, -90);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bounds_opcodes_are_unsupported_without_a_host_state() {
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
        Opcode::GET_ENTITY_BOUNDING_BOX,
        Opcode::GET_LOC_BOUNDING_BOX,
        Opcode::GET_OBJ_BOUNDING_BOX,
        Opcode::GET_LOC_OVERLAY_HEIGHT,
    ] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&[int(opcode, 0), ret()], counts(0, 1, 0), zero(), &[]))
                .unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
