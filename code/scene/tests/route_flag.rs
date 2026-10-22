use openrs_scene::collision::{CollisionMap, BLOCKED};
use openrs_scene::route::{RouteFinder, Strategy};
use openrs_scene::route_flag::{calculate_route, route_to_ground_item, RouteSource, WALKING_STEPS};

fn open_map() -> CollisionMap {
    let mut clip = CollisionMap::create_clip_map(104, 104);
    for x in 0..104 {
        for y in 0..104 {
            clip.remove_flag(x, y, BLOCKED);
        }
    }
    clip
}

fn source(clip: &CollisionMap, base: (i32, i32)) -> RouteSource<'_> {
    RouteSource {
        scene_size_x: 104,
        scene_size_y: 104,
        movement_base_x: base.0,
        movement_base_y: base.1,
        player_size: 1,
        clip,
    }
}

#[test]
fn flag_is_the_last_step_of_the_route() {
    let clip = open_map();
    let mut finder = RouteFinder::new();
    let src = source(&clip, (10, 10));
    let strategy = Strategy::FixedTile { x: 14, y: 10 };
    let flag = calculate_route(&mut finder, &src, 14, 10, true, &strategy);
    assert_eq!(flag, Some((14, 10)));
    let mut xs = vec![0; WALKING_STEPS];
    let mut ys = vec![0; WALKING_STEPS];
    let steps = finder
        .find_route(10, 10, 1, &strategy, &clip, true, &mut xs, &mut ys)
        .unwrap();
    assert!(steps >= 1);
    assert_eq!(flag, Some((xs[steps as usize - 1], ys[steps as usize - 1])));
}

#[test]
fn out_of_scene_destinations_and_sources_have_no_flag() {
    let clip = open_map();
    let mut finder = RouteFinder::new();
    let strategy = Strategy::FixedTile { x: 200, y: 10 };
    assert_eq!(
        calculate_route(&mut finder, &source(&clip, (10, 10)), 200, 10, true, &strategy),
        None
    );
    let strategy = Strategy::FixedTile { x: 5, y: 5 };
    assert_eq!(
        calculate_route(&mut finder, &source(&clip, (-1, 10)), 5, 5, true, &strategy),
        None
    );
    assert_eq!(
        calculate_route(&mut finder, &source(&clip, (10, 104)), 5, 5, true, &strategy),
        None
    );
}

#[test]
fn standing_on_the_destination_has_no_flag() {
    let clip = open_map();
    let mut finder = RouteFinder::new();
    let strategy = Strategy::FixedTile { x: 10, y: 10 };
    assert_eq!(
        calculate_route(&mut finder, &source(&clip, (10, 10)), 10, 10, false, &strategy),
        None
    );
}

#[test]
fn blocked_destination_falls_back_to_the_nearest_tile_when_alternatives_are_allowed() {
    let mut clip = open_map();
    for x in 19..=21 {
        for y in 9..=11 {
            if (x, y) != (20, 10) {
                clip.add_flag(x, y, BLOCKED);
            }
        }
    }
    let mut finder = RouteFinder::new();
    let strategy = Strategy::FixedTile { x: 20, y: 10 };
    let strict = calculate_route(&mut finder, &source(&clip, (10, 10)), 20, 10, false, &strategy);
    let relaxed = calculate_route(&mut finder, &source(&clip, (10, 10)), 20, 10, true, &strategy);
    assert_eq!(strict, None);
    let (x, y) = relaxed.unwrap();
    assert!((x - 20).abs() <= 2 && (y - 10).abs() <= 2);
}

#[test]
fn ground_item_route_ends_on_the_item_tile() {
    let clip = open_map();
    let mut finder = RouteFinder::new();
    let flag = route_to_ground_item(&mut finder, &source(&clip, (10, 10)), 16, 13);
    assert_eq!(flag, Some((16, 13)));
}
