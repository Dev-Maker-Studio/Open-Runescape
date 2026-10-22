use crate::collision::CollisionMap;
use crate::route::{RouteFinder, Strategy};

pub const WALKING_STEPS: usize = 50;

#[derive(Clone, Copy)]
pub struct RouteSource<'a> {
    pub scene_size_x: i32,
    pub scene_size_y: i32,
    pub movement_base_x: i32,
    pub movement_base_y: i32,
    pub player_size: i32,
    pub clip: &'a CollisionMap,
}

pub fn calculate_route(
    finder: &mut RouteFinder,
    source: &RouteSource,
    destination_x: i32,
    destination_y: i32,
    find_alternative: bool,
    strategy: &Strategy,
) -> Option<(i32, i32)> {
    let (base_x, base_y) = (source.movement_base_x, source.movement_base_y);
    if !(base_x >= 0 && base_x < source.scene_size_x && base_y >= 0 && base_y < source.scene_size_y)
    {
        return None;
    }
    if !(destination_x >= 0
        && destination_x < source.scene_size_x
        && destination_y >= 0
        && destination_y < source.scene_size_y)
    {
        return None;
    }
    let mut steps_x = vec![0; WALKING_STEPS];
    let mut steps_y = vec![0; WALKING_STEPS];
    let executed = finder
        .find_route(
            base_x,
            base_y,
            source.player_size,
            strategy,
            source.clip,
            find_alternative,
            &mut steps_x,
            &mut steps_y,
        )
        .ok()?;
    if executed < 1 {
        return None;
    }
    let last = (executed - 1) as usize;
    Some((steps_x[last], steps_y[last]))
}

pub fn route_to_ground_item(
    finder: &mut RouteFinder,
    source: &RouteSource,
    x: i32,
    y: i32,
) -> Option<(i32, i32)> {
    calculate_route(finder, source, x, y, false, &Strategy::GroundItem { x, y }).or_else(|| {
        calculate_route(
            finder,
            source,
            x,
            y,
            false,
            &Strategy::Player {
                x,
                y,
                size_x: 1,
                size_y: 1,
                clipping_flag: 0,
            },
        )
    })
}
