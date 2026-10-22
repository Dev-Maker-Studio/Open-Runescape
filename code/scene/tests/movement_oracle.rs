use openrs_scene::movement::{BasParams, MovingEntity, SeqStall};
#[cfg(target_arch = "wasm32")]
use wasm_bindgen_test::wasm_bindgen_test;

const MOVEMENT: &str = include_str!("fixtures/coll-movement.tsv");

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn movement_stepping_matches_the_kotlin_entity_updating_per_iteration() {
    let mut cases = 0;
    let mut snapshots = 0;
    let mut types = [0usize; 5];
    let mut snaps_with_far_snap = 0;
    for line in MOVEMENT.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let n = |i: usize| -> i32 { f[i].parse().unwrap() };
        let bits = |i: usize| f32::from_bits(n(i) as u32);
        let h = |k: usize| n(4 + k);
        let mut entity = MovingEntity::new(bits(1), bits(2), bits(3), h(0));
        entity.delay_movement = h(1) != 0;
        entity.steps_left = h(2);
        entity.previous_steps_left = h(3);
        entity.stalled = h(4);
        entity.walk_speed_accumulator = h(5);
        entity.last_yaw_rotation = h(6);
        entity.turn_direction = h(7);
        entity.face_entity = h(8);
        entity.yaw.rotation = h(9);
        entity.yaw.velocity = h(10);
        entity.roll.rotation = h(11);
        entity.roll.velocity = h(12);
        entity.pitch.rotation = h(13);
        entity.pitch.velocity = h(14);
        entity.turning_ticks = h(15);
        let cutscene = h(16) != 0;
        let bas = BasParams {
            yaw_acceleration: h(17),
            yaw_max_velocity: h(18),
            roll_acceleration: h(19),
            roll_max_velocity: h(20),
            roll_target_angle: h(21),
            pitch_acceleration: h(22),
            pitch_max_velocity: h(23),
            pitch_target_angle: h(24),
            unused: h(25),
        };
        let has_seq = h(26) != 0;
        let speed = h(29);
        if has_seq && speed == 0 {
            entity.seq_stall = Some(SeqStall {
                interrupt_type: h(27),
                move_type: h(28),
            });
        }
        for k in 0..25 {
            entity.movement_base_x[k] = n(34 + k * 3);
            entity.movement_base_y[k] = n(35 + k * 3);
            entity.movement_types[k] = n(36 + k * 3) as i8;
        }
        let tail = f[f.len() - 1];
        let start = bits(1);
        for (index, snap) in tail.split(';').enumerate() {
            let expected: Vec<i32> = snap.split(',').map(|v| v.parse().unwrap()).collect();
            let out = entity.update_position(cutscene, &bas);
            let turned = if entity.face_entity == -1 {
                entity.turn_to(&bas)
            } else {
                -999
            };
            let actual = [
                out.movement_type,
                out.turn_direction_index,
                turned,
                entity.x.to_bits() as i32,
                entity.y.to_bits() as i32,
                entity.z.to_bits() as i32,
                entity.steps_left,
                entity.previous_steps_left,
                entity.stalled,
                entity.walk_speed_accumulator,
                entity.last_yaw_rotation,
                entity.yaw.rotation,
                entity.yaw.velocity,
                entity.roll.rotation,
                entity.roll.velocity,
                entity.pitch.rotation,
                entity.pitch.velocity,
                entity.turning_ticks,
            ];
            assert_eq!(
                expected.as_slice(),
                actual.as_slice(),
                "case {} iteration {index}",
                f[0]
            );
            types[(out.movement_type + 1) as usize] += 1;
            if (entity.x - start).abs() > 1000.0 {
                snaps_with_far_snap += 1;
            }
            snapshots += 1;
        }
        cases += 1;
    }
    assert_eq!(cases, 1000);
    assert_eq!(snapshots, 8000);
    assert!(types.iter().filter(|c| **c > 100).count() >= 4);
    assert!(snaps_with_far_snap > 100);
}
