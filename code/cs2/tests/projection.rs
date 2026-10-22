#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct ProjectionHost {
    world: WorldState,
}

impl VarHost for ProjectionHost {
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

fn matrix43(v: &[i32]) -> Matrix43 {
    let f = |index: usize| f32::from_bits(v[index] as u32);
    Matrix43 {
        xa: f(0),
        xb: f(1),
        xc: f(2),
        xd: f(3),
        ya: f(4),
        yb: f(5),
        yc: f(6),
        yd: f(7),
        za: f(8),
        zb: f(9),
        zc: f(10),
        zd: f(11),
    }
}

fn matrix44(v: &[i32]) -> [f32; 16] {
    let mut out = [0.0f32; 16];
    for (slot, value) in out.iter_mut().zip(v) {
        *slot = f32::from_bits(*value as u32);
    }
    out
}

fn canonical(value: f32) -> i32 {
    if value.is_nan() {
        0x7fc00000
    } else {
        value.to_bits() as i32
    }
}

fn parse(digest: &str, templates: &[Template], name: &str) -> (usize, WorldState) {
    let (id, values) = digest.split_once('|').unwrap();
    let id: usize = number(id);
    let v = integers(values);
    let template = &templates[id];
    let camera = CameraState {
        scene_size_x: template.size_x,
        scene_size_y: template.size_y,
        viewport_fov_min: v[0] as i16,
        viewport_fov_max: v[1] as i16,
        fov_scale_min: v[2] as i16,
        fov_scale_max: v[3] as i16,
        aspect_fov_min: v[4] as i16,
        aspect_fov_max: v[5] as i16,
        ..CameraState::default()
    };
    let anchor = match v[93] {
        1 => Some(HintAnchor::Point {
            x: f32::from_bits(v[94] as u32),
            z: f32::from_bits(v[95] as u32),
            plane: v[96] as i8,
        }),
        2 => Some(HintAnchor::Footprint {
            start_x: v[94] as i16,
            end_x: v[95] as i16,
            start_z: v[96] as i16,
            end_z: v[97] as i16,
            plane: v[98] as i8,
        }),
        _ => None,
    };
    let mut projection = ProjectionState {
        hint_offset_x: v[6],
        hint_offset_y: v[7],
        game_area_width: v[8],
        game_area_height: v[9],
        scaled_fov: v[10],
        game_screen: (v[11] == 1).then_some((v[12], v[13])),
        origin_x: f32::from_bits(v[14] as u32),
        origin_y: f32::from_bits(v[15] as u32),
        center_x: f32::from_bits(v[16] as u32),
        center_y: f32::from_bits(v[17] as u32),
        hint_position: [
            f32::from_bits(v[18] as u32),
            f32::from_bits(v[19] as u32),
            f32::from_bits(v[20] as u32),
        ],
        view: matrix43(&v[21..33]),
        view44: matrix44(&v[33..49]),
        model: matrix44(&v[49..65]),
        view_projection: matrix44(&v[65..81]),
        sun: matrix43(&v[81..93]),
        ..ProjectionState::default()
    };
    match name {
        "GET_ENTITY_SCREEN_POSITION" => projection.entity_anchor = anchor,
        "GET_LOC_SCREEN_POSITION" => projection.loc_anchor = anchor,
        "GET_OBJ_SCREEN_POSITION" => projection.obj_anchor = anchor,
        _ => {}
    }
    (
        id,
        WorldState {
            camera,
            scene: template.scene.clone(),
            projection,
            ..Default::default()
        },
    )
}

fn digest(id: usize, world: &WorldState, name: &str) -> String {
    let c = &world.camera;
    let p = &world.projection;
    let mut values = vec![
        i32::from(c.viewport_fov_min),
        i32::from(c.viewport_fov_max),
        i32::from(c.fov_scale_min),
        i32::from(c.fov_scale_max),
        i32::from(c.aspect_fov_min),
        i32::from(c.aspect_fov_max),
        p.hint_offset_x,
        p.hint_offset_y,
        p.game_area_width,
        p.game_area_height,
        p.scaled_fov,
    ];
    match p.game_screen {
        Some((width, height)) => values.extend([1, width, height]),
        None => values.extend([0, 0, 0]),
    }
    values.extend([p.origin_x, p.origin_y, p.center_x, p.center_y].map(canonical));
    values.extend(p.hint_position.map(canonical));
    let matrix43_values = |matrix: &Matrix43| {
        [
            matrix.xa, matrix.xb, matrix.xc, matrix.xd, matrix.ya, matrix.yb, matrix.yc, matrix.yd,
            matrix.za, matrix.zb, matrix.zc, matrix.zd,
        ]
        .map(canonical)
    };
    values.extend(matrix43_values(&p.view));
    values.extend(p.view44.map(canonical));
    values.extend(p.model.map(canonical));
    values.extend(p.view_projection.map(canonical));
    values.extend(matrix43_values(&p.sun));
    let anchor = match name {
        "GET_ENTITY_SCREEN_POSITION" => p.entity_anchor,
        "GET_LOC_SCREEN_POSITION" => p.loc_anchor,
        "GET_OBJ_SCREEN_POSITION" => p.obj_anchor,
        _ => None,
    };
    match anchor {
        Some(HintAnchor::Point { x, z, plane }) => {
            values.extend([1, canonical(x), canonical(z), i32::from(plane), 0, 0]);
        }
        Some(HintAnchor::Footprint {
            start_x,
            end_x,
            start_z,
            end_z,
            plane,
        }) => values.extend([
            2,
            i32::from(start_x),
            i32::from(end_x),
            i32::from(start_z),
            i32::from(end_z),
            i32::from(plane),
        ]),
        None => values.extend([0; 6]),
    }
    let text: Vec<String> = values.iter().map(i32::to_string).collect();
    format!("{id}|{}", text.join(","))
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(message)) => match message.as_str() {
            DIVISION_BY_ZERO => expected == "ArithmeticException",
            _ => expected == "ArrayIndexOutOfBoundsException",
        },
        VmErrorKind::StackUnderflow(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_projection_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/projection.native.tsv");
    let mut templates = Vec::new();
    let mut cases = Vec::new();
    let mut totals = (0, 0);
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "S" => templates.push(template(&columns)),
            "C" => cases.push(columns),
            "Z" => totals = (number::<usize>(columns[1]), 0),
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
        let (id, world) = parse(columns[3], &templates, name);
        let mut host = ProjectionHost { world };
        if digest(id, &host.world, name) != columns[3] {
            mismatches.push(format!("{name}: state round trip differs\n  {}\n  {}", digest(id, &host.world, name), columns[3]));
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
        let after = digest(id, &host.world, name);
        if after != columns[6] {
            mismatches.push(format!(
                "{label}: state\n  {after}\n  {}\n  from {}",
                columns[6], columns[3]
            ));
        }
    }
    println!(
        "projection cases={} mismatches={}",
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
    assert_eq!(cases.len(), totals.0);
    assert_eq!(per_opcode.len(), 4);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn projection_opcodes_are_unsupported_without_a_host_state() {
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
        Opcode::VIEWPORT_GETEFFECTIVESIZE,
        Opcode::GET_ENTITY_SCREEN_POSITION,
        Opcode::GET_LOC_SCREEN_POSITION,
        Opcode::GET_OBJ_SCREEN_POSITION,
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
