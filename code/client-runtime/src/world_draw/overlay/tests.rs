use super::*;

#[test]
fn tile_arrows_use_their_distance_and_entities_the_fixed_limit() {
    assert_eq!(arrow_dimension(ARROW_TILE, 10), (10_i64 << 9) * (10_i64 << 9));
    assert_eq!(arrow_dimension(ARROW_TILE, 65535), (65535_i64 << 9).pow(2));
    assert_eq!(arrow_dimension(ARROW_NPC, 10), ENTITY_LIMIT);
    assert_eq!(arrow_dimension(ARROW_PLAYER, 0), ENTITY_LIMIT);
}

#[test]
fn arrows_hide_when_too_close_or_too_far() {
    assert_eq!(arrow_rotation(511, 0, ENTITY_LIMIT, 0), None);
    assert_eq!(arrow_rotation(512, 0, ENTITY_LIMIT, 0), Some(((std::f64::consts::FRAC_PI_2 * UNITS_PER_RADIAN) as i32) & ANGLE_MASK));
    assert_eq!(arrow_rotation(9600, 0, ENTITY_LIMIT, 0), Some(((std::f64::consts::FRAC_PI_2 * UNITS_PER_RADIAN) as i32) & ANGLE_MASK));
    assert_eq!(arrow_rotation(9601, 0, ENTITY_LIMIT, 0), None);
}

#[test]
fn arrow_rotation_wraps_into_the_angle_mask() {
    assert_eq!(arrow_rotation(0, 1000, ENTITY_LIMIT, 0), Some(0));
    assert_eq!(arrow_rotation(0, 1000, ENTITY_LIMIT, 100), Some(-100 & ANGLE_MASK));
    assert_eq!(arrow_rotation(0, -1000, ENTITY_LIMIT, 0), Some(8192 & ANGLE_MASK));
    assert_eq!(arrow_rotation(-1000, 0, ENTITY_LIMIT, 0), Some(-4096 & ANGLE_MASK));
}

#[test]
fn trail_nodes_step_diagonally_then_straight() {
    let nodes = trail_nodes(&[(100, 100), (103, 101)], (100, 100), (104, 104));
    assert_eq!(nodes, [(1, 1), (2, 1), (3, 1)]);
}

#[test]
fn trail_nodes_outside_the_scene_are_dropped() {
    let nodes = trail_nodes(&[(98, 100), (102, 100)], (100, 100), (104, 104));
    assert_eq!(nodes, [(0, 0), (1, 0), (2, 0)]);
    assert!(trail_nodes(&[(5, 5)], (0, 0), (104, 104)).is_empty());
    assert!(trail_nodes(&[], (0, 0), (104, 104)).is_empty());
}

#[test]
fn trail_node_origin_is_the_tile_centre() {
    assert_eq!(node_origin((3, 4)), (3 * 512 + 256, 4 * 512 + 256));
}

#[test]
fn hint_sets_report_emptiness() {
    let mut set = HintSet::default();
    assert!(set.is_empty());
    set.trails.push(HintTrailSpec {
        slot: 0,
        model_id: 1,
        plane: 0,
        placements: Vec::new(),
    });
    assert!(!set.is_empty());
}
