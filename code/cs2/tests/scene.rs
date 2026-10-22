#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct SceneHost {
    world: WorldState,
}

impl VarHost for SceneHost {
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

#[derive(Clone)]
struct Template {
    scene: SceneState,
    size_x: i32,
    size_y: i32,
}

fn number<T: std::str::FromStr>(value: &str) -> T
where
    T::Err: std::fmt::Debug,
{
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn int_rows(value: &str) -> Vec<Vec<i32>> {
    value
        .split(';')
        .map(|row| row.split(',').map(number).collect())
        .collect()
}

fn byte_rows(value: &str) -> Vec<Vec<i8>> {
    if value.is_empty() {
        return Vec::new();
    }
    value
        .split(';')
        .map(|row| row.split(',').map(number).collect())
        .collect()
}

fn template(columns: &[&str]) -> Template {
    let planes: Vec<Option<HeightGrid>> = columns[5]
        .split('|')
        .map(|plane| {
            if plane == "n" {
                return None;
            }
            let (sizes, rows) = plane.split_once('/').unwrap();
            let (width, length) = sizes.split_once(':').unwrap();
            Some(HeightGrid {
                width: number(width),
                length: number(length),
                heights: int_rows(rows),
            })
        })
        .collect();
    let lighting = if columns[7] == "n" {
        None
    } else {
        Some(
            columns[7]
                .split('|')
                .map(|plane| {
                    if plane == "n" {
                        None
                    } else {
                        Some(byte_rows(&plane[1..]))
                    }
                })
                .collect(),
        )
    };
    let mut scene = SceneState {
        loaded: columns[2] == "1",
        roof_masks: byte_rows(columns[6]),
        lighting,
        ..Default::default()
    };
    for (slot, plane) in planes.into_iter().enumerate() {
        scene.planes[slot] = plane;
    }
    Template {
        scene,
        size_x: number(columns[3]),
        size_y: number(columns[4]),
    }
}

fn parse(digest: &str, templates: &[Template]) -> (usize, WorldState) {
    let (id, values) = digest.split_once('|').unwrap();
    let id: usize = number(id);
    let v = integers(values);
    let template = &templates[id];
    let mut camera = CameraState {
        calculation_type: v[0],
        yaw: v[1],
        roll: v[2],
        pitch: v[3],
        move_speed: v[4],
        move_angle: v[5],
        position_x: v[6],
        position_y: v[7],
        position_z: v[8],
        angle_x: f32::from_bits(v[9] as u32),
        angle_y: f32::from_bits(v[10] as u32),
        send_angle_to_server: v[11] == 1,
        ping_target_x: v[12],
        static_target_y: v[13],
        scene_base_x: v[21],
        scene_base_y: v[22],
        scene_size_x: template.size_x,
        scene_size_y: template.size_y,
        ..CameraState::default()
    };
    camera.max_zoom = 0;
    let mut scene = template.scene.clone();
    scene.look_x = v[14];
    scene.look_y = v[15];
    scene.look_height_offset = v[16];
    scene.camera_base_x = v[17];
    scene.camera_base_y = v[18];
    scene.regular_lowest_yaw = v[19];
    scene.my_plane = v[20];
    (
        id,
        WorldState {
            camera,
            scene,
            ..Default::default()
        },
    )
}

fn digest(id: usize, world: &WorldState) -> String {
    let c = &world.camera;
    let s = &world.scene;
    let values = [
        c.calculation_type,
        c.yaw,
        c.roll,
        c.pitch,
        c.move_speed,
        c.move_angle,
        c.position_x,
        c.position_y,
        c.position_z,
        c.angle_x.to_bits() as i32,
        c.angle_y.to_bits() as i32,
        i32::from(c.send_angle_to_server),
        c.ping_target_x,
        c.static_target_y,
        s.look_x,
        s.look_y,
        s.look_height_offset,
        s.camera_base_x,
        s.camera_base_y,
        s.regular_lowest_yaw,
        s.my_plane,
        c.scene_base_x,
        c.scene_base_y,
    ];
    let text: Vec<String> = values.iter().map(i32::to_string).collect();
    format!("{id}|{}", text.join(","))
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(message)) => match message.as_str() {
            RUNTIME_FAILURE => expected == "RuntimeException",
            NEGATIVE_ARRAY_SIZE => expected == "NegativeArraySizeException",
            _ => expected == "ArrayIndexOutOfBoundsException",
        },
        VmErrorKind::StackUnderflow(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_scene_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/scene.native.tsv");
    let mut templates = Vec::new();
    let mut cases = Vec::new();
    let mut atans = Vec::new();
    let mut totals = (0, 0);
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "S" => templates.push(template(&columns)),
            "C" => cases.push(columns),
            "A" => atans.push(columns),
            "Z" => totals = (number::<usize>(columns[1]), number::<usize>(columns[2])),
            other => panic!("{other}"),
        }
    }
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    for columns in &cases {
        let name = columns[1];
        let opcode = OPCODES
            .iter()
            .find(|metadata| metadata.name == name)
            .unwrap()
            .opcode;
        let arguments = integers(columns[2]);
        let initial = arguments.len();
        let (id, world) = parse(columns[3], &templates);
        let mut host = SceneHost { world };
        if digest(id, &host.world) != columns[3] {
            mismatches.push(format!("{name}: state round trip differs"));
            continue;
        }
        let mut code: Vec<_> = arguments.iter().map(|value| constant(*value)).collect();
        code.push(int(opcode, 0));
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("{name} {}", columns[2]);
        let entry = per_opcode.entry(name.to_string()).or_default();
        match (&result, columns[4]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(columns[5]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!("{label}: ints {:?}", vm.int_stack()));
                }
            }
            (Err(error), expected) if expected.starts_with("error:") => {
                entry.1 += 1;
                if error.instruction != initial
                    || !class_matches(&expected["error:".len()..], &error.kind)
                {
                    mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                }
            }
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
        let after = digest(id, &host.world);
        if after != columns[6] {
            mismatches.push(format!(
                "{label}: state\n  {after}\n  {}\n  from {}",
                columns[6], columns[3]
            ));
        }
    }
    let mut atan_mismatches = 0;
    for columns in &atans {
        let y = f64::from_bits(u64::from_str_radix(columns[1], 16).unwrap());
        let x = f64::from_bits(u64::from_str_radix(columns[2], 16).unwrap());
        let expected = f64::from_bits(u64::from_str_radix(columns[3], 16).unwrap());
        let actual = fdlibm_atan2(y, x);
        if actual.to_bits() != expected.to_bits() && !(actual.is_nan() && expected.is_nan()) {
            atan_mismatches += 1;
            if atan_mismatches <= 8 {
                println!("atan2({y:e}, {x:e}) = {actual:e} vs {expected:e}");
            }
        }
    }
    println!(
        "scene cases={} mismatches={} atan2 pairs={} atan2 mismatches={atan_mismatches}",
        cases.len(),
        mismatches.len(),
        atans.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(8) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(atan_mismatches, 0);
    assert_eq!(cases.len(), totals.0);
    assert_eq!(atans.len(), totals.1);
    assert_eq!(per_opcode.len(), 2);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn scene_opcodes_are_unsupported_without_a_host_state() {
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
    for opcode in [Opcode::CAM_LOOKAT, Opcode::CAM_FORCEANGLE] {
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
