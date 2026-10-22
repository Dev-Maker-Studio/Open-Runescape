use crate::orbit::{Ground, Jitter, Orbit};
use crate::orbit_oracle::{OracleGround, SIZE, mix, scene_ground};
use crate::scripted_camera::{Placement, ScriptedCamera, target_tile};

const KEYS: usize = 6;

fn spline_value(seed: i32, path: i32, point: i32, axis: i32, base: (i32, i32)) -> i32 {
    let v = mix(
        seed.wrapping_add(path * 31).wrapping_add(axis * 7),
        point,
        path * 5 + axis,
    );
    let span = ((v as u32) >> 4) & 0xffff;
    let span = span as i32;
    match axis {
        0 => base.0 * 512 + span % (SIZE * 512),
        2 => base.1 * 512 + span % (SIZE * 512),
        1 => span % 3000 - 1500,
        _ => span % 2200 - 400,
    }
}

fn splines(seed: i32, base: (i32, i32)) -> [Option<Vec<[i32; 4]>>; 2] {
    let mut out: [Option<Vec<[i32; 4]>>; 2] = [None, None];
    for (path, slot) in out.iter_mut().enumerate() {
        let mut points = Vec::new();
        for point in 0..KEYS * 2 {
            let mut p = [0; 4];
            for (axis, value) in p.iter_mut().enumerate() {
                *value = spline_value(seed, path as i32, point as i32, axis as i32, base);
            }
            points.push(p);
        }
        *slot = Some(points);
    }
    out
}

fn bits(value: i64) -> f32 {
    f32::from_bits(value as i32 as u32)
}

fn float_bits(value: f32) -> i64 {
    i64::from(value.to_bits() as i32)
}

#[test]
fn scripted_camera_replays_the_kotlin_fixture() {
    replay(&OracleGround);
}

#[test]
fn scripted_camera_replays_the_kotlin_fixture_on_the_scene_ground() {
    replay(&scene_ground());
}

fn replay(ground: &dyn Ground) {
    let text = include_str!("../tests/fixtures/camera-modes.native.tsv");
    let mut orbit = Orbit::default();
    let mut camera = ScriptedCamera::default();
    let mut trailer = None;
    let mut rows = 0usize;
    let mut modes = [0usize; 6];
    let mut completed = 0usize;
    for line in text.lines() {
        let fields: Vec<&str> = line.split('\t').collect();
        if fields[0] == "T" {
            trailer = Some(fields[1].parse::<usize>().unwrap());
            continue;
        }
        let bar = fields.iter().position(|f| *f == "|").unwrap();
        let input: Vec<i64> = fields[2..bar].iter().map(|f| f.parse().unwrap()).collect();
        let output: Vec<i64> = fields[bar + 1..]
            .iter()
            .map(|f| f.parse().unwrap())
            .collect();
        let case = input[0];
        let frame = input[1];
        let plane = input[2] as i32;
        let area = input[3] as i32;
        let player_x = input[4] as i32;
        let player_y = input[5] as i32;
        let base = (input[6] as i32, input[7] as i32);
        let elapsed = input[8];
        let cycles = input[9];
        let op = input[10];
        let a: Vec<i32> = input[11..17].iter().map(|v| *v as i32).collect();
        let draws: Vec<f64> = input[17..22]
            .iter()
            .map(|v| f64::from_bits(*v as u64))
            .collect();
        if frame == 0 {
            orbit = Orbit {
                angle_x: input[26] as f32,
                angle_y: input[25] as f32,
                base_x: input[29] as i32,
                base_y: input[30] as i32,
                lowest_yaw: input[28] as i32,
                zoom: input[27] as i32,
                jitter: Jitter {
                    h: input[22] as i32,
                    v: input[23] as i32,
                    a: input[24] as i32,
                    ..Jitter::default()
                },
                ..Orbit::default()
            };
            camera = ScriptedCamera {
                cycles: input[31] as i32,
                splines: splines(input[32] as i32, base),
                ..ScriptedCamera::default()
            };
        }
        match op {
            1 => camera.move_camera(a[0], a[1], a[2], a[3], a[4], true, ground, plane),
            2 => camera.set_angle(&mut orbit, a[0], a[1], a[2], ground, plane),
            3 => camera.look_at(a[0], a[1], a[2], a[3], a[4], ground, plane),
            4 => camera.smooth_reset(),
            5 => camera.start_smooth_transition(),
            6 => camera.shake(a[0] as usize, a[1], a[2], a[3], a[4]),
            7 => match target_tile(a[0], base, (SIZE, SIZE)) {
                Some((x, y)) => camera.set_target(x, y),
                None => camera.set_target(-1, -1),
            },
            8 => {
                let (x, y) = target_tile(a[0], base, (SIZE, SIZE)).unwrap();
                camera.follow_coordinate(x, y);
            }
            9 => camera.start_cutscene(a[0], a[1], a[2], a[3], a[4], a[5]),
            _ => {}
        }
        for _ in 0..cycles {
            camera.tick_cycle(base.0, base.1, ground, plane);
            camera.advance_cycle();
        }
        let at = Placement {
            player_x,
            player_y,
            plane,
            area_height: area,
        };
        let mut next = draws.into_iter();
        let mut random = || next.next().unwrap_or(0.0);
        let pose = camera.frame(&mut orbit, elapsed, ground, &at, &mut random);
        modes[camera.mode as usize] += 1;
        completed += usize::from(camera.spline_completed);
        let actual = [
            i64::from(pose.x),
            i64::from(pose.z),
            i64::from(pose.y),
            i64::from(pose.yaw),
            i64::from(pose.roll),
            i64::from(pose.pitch),
            i64::from(camera.mode),
            i64::from(orbit.base_x),
            i64::from(orbit.base_y),
            i64::from(orbit.lowest_yaw),
            float_bits(orbit.angle_x),
            float_bits(orbit.angle_y),
            i64::from(camera.x),
            i64::from(camera.z),
            i64::from(camera.y),
            i64::from(camera.yaw),
            i64::from(camera.roll),
            i64::from(camera.pitch),
            i64::from(camera.ping_target_x),
            i64::from(camera.static_target_y),
            i64::from(camera.target_x),
            i64::from(camera.target_y),
            i64::from(camera.spline_progress),
            i64::from(camera.spline_completed),
            i64::from(camera.spline_previously_completed),
            i64::from(camera.position_path),
            i64::from(camera.look_path),
            i64::from(camera.move_speed),
            i64::from(camera.move_angle),
            i64::from(camera.start_x),
            i64::from(camera.start_yaw),
            i64::from(camera.cycles.wrapping_sub(camera.last_smooth_cycle)),
            i64::from(camera.send_angle),
        ];
        assert_eq!(
            actual.as_slice(),
            output.as_slice(),
            "case {case} frame {frame} op {op}: angle_x {} expected {}",
            orbit.angle_x,
            bits(output[10])
        );
        rows += 1;
    }
    assert_eq!(trailer, Some(rows));
    assert!(rows > 3000);
    assert!(modes[1..].iter().all(|n| *n > 100));
    assert!(completed > 0);
}
