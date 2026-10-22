#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::{BTreeMap, BTreeSet};

const POOL: [i32; 12] = [
    0,
    1,
    2,
    3,
    5,
    7,
    8,
    100,
    -1,
    65535,
    i32::MAX,
    i32::MIN,
];

struct WorldHost {
    world: WorldState,
}

impl VarHost for WorldHost {
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

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn float(bits: i32) -> f32 {
    f32::from_bits(bits as u32)
}

fn set_of(value: &str) -> BTreeSet<i32> {
    if value == "-" {
        BTreeSet::new()
    } else {
        value.split(';').map(number).collect()
    }
}

fn spline_of(value: &str) -> Option<SplinePath> {
    if value == "n" {
        return None;
    }
    let body = &value[1..];
    if body.is_empty() {
        return Some(Vec::new());
    }
    Some(body.split(';').map(|row| row.split(':').map(number).collect()).collect())
}

#[allow(clippy::field_reassign_with_default)]
fn parse(digest: &str) -> WorldState {
    let sections: Vec<&str> = digest.split('|').collect();
    assert_eq!(sections.len(), 6);
    let c = integers(sections[0]);
    let mut map_part: Vec<&str> = sections[1].split(',').collect();
    let element = map_part.pop().unwrap();
    let m: Vec<i32> = map_part.iter().map(|value| number(value)).collect();
    let mut camera = CameraState::default();
    camera.calculation_type = c[0];
    camera.yaw = c[1];
    camera.roll = c[2];
    camera.pitch = c[3];
    camera.move_speed = c[4];
    camera.move_angle = c[5];
    camera.movement_x = c[6];
    camera.movement_y = c[7];
    camera.movement_z = c[8];
    camera.constant_speed = c[9];
    camera.variable_speed = c[10];
    camera.position_x = c[11];
    camera.position_y = c[12];
    camera.position_z = c[13];
    camera.transition_x = c[14];
    camera.transition_y = c[15];
    camera.transition_z = c[16];
    camera.transition_yaw = c[17];
    camera.transition_roll = c[18];
    camera.angle_x = float(c[19]);
    camera.angle_y = float(c[20]);
    camera.target_x = c[21];
    camera.target_y = c[22];
    camera.ping_target_x = c[23];
    camera.static_target_y = c[24];
    camera.max_zoom = c[25];
    camera.base_zoom = c[26];
    camera.position_path_index = c[27];
    camera.look_at_path_index = c[28];
    camera.position_keyframe = c[29];
    camera.look_at_keyframe = c[30];
    camera.spline_progress = c[31];
    camera.spline_speed_start = c[32];
    camera.spline_speed_end = c[33];
    camera.pitch_velocity = float(c[34]);
    camera.yaw_velocity = float(c[35]);
    camera.pitch_velocity_applied = c[36] == 1;
    camera.yaw_velocity_applied = c[37] == 1;
    camera.send_angle_to_server = c[38] == 1;
    camera.last_smooth_cycle = c[39];
    camera.cycles = c[40];
    for slot in 0..CAMERA_SHAKE_SLOTS {
        camera.shaking[slot] = c[41 + slot] == 1;
    }
    camera.viewport_fov_min = c[46] as i16;
    camera.viewport_fov_max = c[47] as i16;
    camera.zoom_min = c[48] as i16;
    camera.zoom_max = c[49] as i16;
    camera.fov_scale_min = c[50] as i16;
    camera.fov_scale_max = c[51] as i16;
    camera.aspect_fov_min = c[52] as i16;
    camera.aspect_fov_max = c[53] as i16;
    camera.scene_base_x = c[54];
    camera.scene_base_y = c[55];
    camera.scene_size_x = c[56];
    camera.scene_size_y = c[57];
    camera.splines = [spline_of(sections[4]), spline_of(sections[5])];
    let world_map = WorldMapState {
        hide_icons: m[0] == 1,
        requested_zoom: float(m[1]),
        square_x: m[2],
        square_y: m[3],
        center_x: m[4],
        center_y: m[5],
        static_element: (element != "n").then(|| number(element)),
        loading_percentage: m[6],
        view_width: m[7],
        viewport_scale: m[8],
        hidden_elements: set_of(sections[2]),
        hidden_categories: set_of(sections[3]),
        ..WorldMapState::default()
    };
    WorldState { camera, world_map, ..Default::default() }
}

fn join(values: &[i32]) -> String {
    values
        .iter()
        .map(i32::to_string)
        .collect::<Vec<_>>()
        .join(",")
}

fn set_text(set: &BTreeSet<i32>) -> String {
    let listed: Vec<String> = POOL
        .iter()
        .filter(|id| set.contains(id))
        .map(i32::to_string)
        .collect();
    if listed.is_empty() {
        "-".to_string()
    } else {
        listed.join(";")
    }
}

fn spline_text(path: &Option<SplinePath>) -> String {
    match path {
        None => "n".to_string(),
        Some(rows) => format!(
            "a{}",
            rows.iter()
                .map(|row| row.iter().map(i32::to_string).collect::<Vec<_>>().join(":"))
                .collect::<Vec<_>>()
                .join(";")
        ),
    }
}

fn digest(world: &WorldState) -> String {
    let camera = &world.camera;
    let map = &world.world_map;
    let mut c = vec![
        camera.calculation_type,
        camera.yaw,
        camera.roll,
        camera.pitch,
        camera.move_speed,
        camera.move_angle,
        camera.movement_x,
        camera.movement_y,
        camera.movement_z,
        camera.constant_speed,
        camera.variable_speed,
        camera.position_x,
        camera.position_y,
        camera.position_z,
        camera.transition_x,
        camera.transition_y,
        camera.transition_z,
        camera.transition_yaw,
        camera.transition_roll,
        camera.angle_x.to_bits() as i32,
        camera.angle_y.to_bits() as i32,
        camera.target_x,
        camera.target_y,
        camera.ping_target_x,
        camera.static_target_y,
        camera.max_zoom,
        camera.base_zoom,
        camera.position_path_index,
        camera.look_at_path_index,
        camera.position_keyframe,
        camera.look_at_keyframe,
        camera.spline_progress,
        camera.spline_speed_start,
        camera.spline_speed_end,
        camera.pitch_velocity.to_bits() as i32,
        camera.yaw_velocity.to_bits() as i32,
        i32::from(camera.pitch_velocity_applied),
        i32::from(camera.yaw_velocity_applied),
        i32::from(camera.send_angle_to_server),
        camera.last_smooth_cycle,
        camera.cycles,
    ];
    c.extend(camera.shaking.iter().map(|flag| i32::from(*flag)));
    c.extend([
        i32::from(camera.viewport_fov_min),
        i32::from(camera.viewport_fov_max),
        i32::from(camera.zoom_min),
        i32::from(camera.zoom_max),
        i32::from(camera.fov_scale_min),
        i32::from(camera.fov_scale_max),
        i32::from(camera.aspect_fov_min),
        i32::from(camera.aspect_fov_max),
        camera.scene_base_x,
        camera.scene_base_y,
        camera.scene_size_x,
        camera.scene_size_y,
    ]);
    let m = [
        i32::from(map.hide_icons),
        map.requested_zoom.to_bits() as i32,
        map.square_x,
        map.square_y,
        map.center_x,
        map.center_y,
        map.loading_percentage,
        map.view_width,
        map.viewport_scale,
    ];
    format!(
        "{}|{},{}|{}|{}|{}|{}",
        join(&c),
        join(&m),
        map.static_element
            .map_or("n".to_string(), |element| element.to_string()),
        set_text(&map.hidden_elements),
        set_text(&map.hidden_categories),
        spline_text(&camera.splines[0]),
        spline_text(&camera.splines[1]),
    )
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
fn every_native_world_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/world.native.tsv");
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "C" => cases.push(columns),
            "Z" => expected_total = number(columns[1]) as usize,
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
        let mut host = WorldHost {
            world: parse(columns[3]),
        };
        if digest(&host.world) != columns[3] {
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
                    mismatches.push(format!(
                        "{label}: ints {:?} vs {expected:?}",
                        vm.int_stack()
                    ));
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
        let after = digest(&host.world);
        if after != columns[6] {
            mismatches.push(format!(
                "{label}: state\n  {after}\n  {}\n  from {}",
                columns[6], columns[3]
            ));
        }
    }
    println!(
        "world cases={} mismatches={}",
        cases.len(),
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(8) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), expected_total);
    assert_eq!(per_opcode.len(), 34);
}

#[test]
fn world_opcodes_are_unsupported_without_a_host_state() {
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
        Opcode::CAM_RESET,
        Opcode::WORLDMAP_GETZOOM,
        Opcode::VIEWPORT_GETFOV,
    ] {
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(
                &[int(opcode, 0), ret()],
                counts(0, 1, 0),
                zero(),
                &[],
            ))
            .unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}
