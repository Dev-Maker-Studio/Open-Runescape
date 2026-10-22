use crate::orbit::{Ground, Jitter, Orbit, PLANES, SceneGround};

pub(crate) const SIZE: i32 = 104;

pub(crate) fn mix(plane: i32, x: i32, y: i32) -> i32 {
    let mut v = x.wrapping_mul(73_856_093)
        ^ y.wrapping_mul(19_349_663)
        ^ (plane + 1).wrapping_mul(83_492_791);
    v ^= ((v as u32) >> 13) as i32;
    v = v.wrapping_mul(0x5bd1_e995);
    v ^= ((v as u32) >> 15) as i32;
    v
}

fn height_hash(plane: i32, x: i32, y: i32) -> i32 {
    let v = mix(plane, x, y);
    if ((v as u32) >> 28) == 0 {
        ((v & 0x7fff_ffff) % 3000) - 2400
    } else {
        (v & 0xff) - 128
    }
}

pub(crate) struct OracleGround;

impl Ground for OracleGround {
    fn size(&self) -> (i32, i32) {
        (SIZE, SIZE)
    }

    fn dimension(&self) -> (i32, i32) {
        (SIZE, SIZE)
    }

    fn height(&self, plane: i32, x: i32, y: i32) -> i32 {
        height_hash(plane, x, y)
    }

    fn average_height(&self, plane: i32, abs_x: i32, abs_y: i32) -> i32 {
        let sx = abs_x >> 9;
        let sy = abs_y >> 9;
        if sx >= 0 && sy >= 0 && sx < SIZE && sy < SIZE {
            let ox = abs_x & 511;
            let oy = abs_y & 511;
            let h = |x: i32, y: i32| height_hash(plane, x, y);
            let h1 = (h(sx, sy) * (512 - ox) + ox * h(sx + 1, sy)) >> 9;
            let h2 = (h(sx, sy + 1) * (512 - ox) + h(sx + 1, sy + 1) * ox) >> 9;
            return (h2 * oy + h1 * (512 - oy)) >> 9;
        }
        0
    }

    fn roofs_hidden(&self, x: i32, y: i32) -> bool {
        self.bridged(x, y)
    }

    fn bridged(&self, x: i32, y: i32) -> bool {
        x >= 0 && y >= 0 && x < SIZE && y < SIZE && ((mix(11, x, y) as u32) >> 5).is_multiple_of(9)
    }

    fn lighting(&self, plane: i32, x: i32, y: i32) -> Option<u8> {
        let v = mix(plane + 7, x, y);
        Some(if ((v as u32) >> 20) & 3 == 0 {
            ((v as u32 >> 8) & 0x3f) as u8
        } else {
            0
        })
    }
}

fn bits(value: i64) -> f32 {
    f32::from_bits(value as i32 as u32)
}

fn float_bits(value: f32) -> i64 {
    i64::from(value.to_bits() as i32)
}

pub(crate) fn scene_ground() -> SceneGround {
    let stride = SIZE + 1;
    let mut heights = Vec::new();
    for plane in 0..PLANES as i32 {
        for x in 0..stride {
            for y in 0..stride {
                heights.push(height_hash(plane, x, y));
            }
        }
    }
    let mut bridges = Vec::new();
    let mut lighting = Vec::new();
    for x in 0..SIZE {
        for y in 0..SIZE {
            bridges.push(u8::from(OracleGround.bridged(x, y)));
        }
    }
    for plane in 0..PLANES as i32 {
        for x in 0..SIZE {
            for y in 0..SIZE {
                lighting.push(OracleGround.lighting(plane, x, y).unwrap());
            }
        }
    }
    let mut ground = SceneGround::new(SIZE, &heights, &bridges).unwrap();
    ground.set_lighting(&lighting).unwrap();
    ground
}

#[test]
fn orbit_replays_the_kotlin_camera_fixture() {
    replay(&OracleGround);
}

#[test]
fn scene_ground_replays_the_kotlin_camera_fixture() {
    replay(&scene_ground());
}

fn replay(ground: &dyn Ground) {
    let text = include_str!("../tests/fixtures/camera-orbit.native.tsv");
    let mut orbit = Orbit::default();
    let mut trailer = None;
    let mut rows = 0usize;
    let mut worst_frame = 0;
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
        let elapsed = input[6];
        let cycles = input[7];
        let yaw_dir = input[8] as i32;
        let pitch_dir = input[9] as i32;
        let random_bits = input[10] as i32;
        let wheel = input[11] as i32;
        if frame == 0 {
            orbit = Orbit {
                angle_x: input[16] as f32,
                angle_y: input[15] as f32,
                base_x: input[19] as i32,
                base_y: input[20] as i32,
                lowest_yaw: input[18] as i32,
                zoom: input[17] as i32,
                jitter: Jitter {
                    h: input[12] as i32,
                    v: input[13] as i32,
                    a: input[14] as i32,
                    cycle: input[21] as i32,
                    ..Jitter::default()
                },
                ..Orbit::default()
            };
        }
        for _ in 0..cycles {
            orbit.apply_yaw(yaw_dir);
            orbit.apply_pitch(pitch_dir);
            orbit.settle_velocities();
            orbit.jitter.tick(random_bits);
        }
        orbit.wheel_zoom(wheel);
        orbit.follow(elapsed, player_x, player_y, ground, plane);
        let eye = orbit.eye(player_x, player_y, ground, plane, area);
        let actual = [
            i64::from(eye.x),
            i64::from(eye.z),
            i64::from(eye.y),
            i64::from(eye.yaw),
            i64::from(eye.roll),
            i64::from(orbit.base_x),
            i64::from(orbit.base_y),
            i64::from(orbit.lowest_yaw),
            float_bits(orbit.angle_x),
            float_bits(orbit.angle_y),
            i64::from(orbit.zoom),
            float_bits(orbit.yaw_velocity),
            float_bits(orbit.pitch_velocity),
            i64::from(orbit.jitter.h),
            i64::from(orbit.jitter.v),
            i64::from(orbit.jitter.a),
            i64::from(ground.height_at(player_x, player_y, plane)),
        ];
        assert_eq!(
            actual.as_slice(),
            output.as_slice(),
            "case {case} frame {frame}: angle_x {} expected {}",
            orbit.angle_x,
            bits(output[8])
        );
        worst_frame = worst_frame.max(frame);
        rows += 1;
    }
    assert_eq!(trailer, Some(rows));
    assert!(rows > 1000 && worst_frame > 10);
}
