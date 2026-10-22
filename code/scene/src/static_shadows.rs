use crate::loc_type::LocType;
use crate::locs::ScenePlacement;

pub const WALL_SHADOW: u8 = 50;
pub const MAX_INTERACT_SHADOW: i32 = 30;
pub const DEFAULT_DISPLACEMENT: i32 = 15;
pub const WALL_STRAIGHT: i32 = 0;
pub const WALL_DIAGONAL_CORNER: i32 = 1;
pub const WALL_STRAIGHT_CORNER: i32 = 3;
pub const SCENERY_INTERACT: i32 = 10;
pub const GROUND_INTERACT: i32 = 11;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ShadowGrid {
    pub width: usize,
    pub cells: Vec<u8>,
}

impl ShadowGrid {
    pub fn new(scene_size: i32) -> Self {
        let width = scene_size as usize + 1;
        Self {
            width,
            cells: vec![0; width * width],
        }
    }

    pub fn get(&self, x: i32, y: i32) -> u8 {
        self.cells[x as usize + y as usize * self.width]
    }

    pub fn set_tile_shadow(&mut self, x: i32, y: i32, shadow: i32) {
        let cell = &mut self.cells[x as usize + y as usize * self.width];
        if i32::from(*cell) < shadow {
            *cell = shadow as u8;
        }
    }

    pub fn non_zero(&self) -> usize {
        self.cells.iter().filter(|value| **value != 0).count()
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct BakeStats {
    pub placements: usize,
    pub missing_type: usize,
    pub skipped_textures: usize,
    pub walls: usize,
    pub interacts: usize,
    pub interacts_out_of_scene: usize,
    pub animated_interacts: usize,
    pub model_displacements: usize,
}

pub fn is_stationary(loc: &LocType) -> bool {
    loc.animations.is_none()
        && loc.alternative_ids.is_none()
        && !loc.has_animation
        && !loc.transforms
}

pub fn interact_displacement(loc: &LocType, size2d: Option<i32>) -> i32 {
    if !is_stationary(loc) {
        return DEFAULT_DISPLACEMENT;
    }
    match size2d {
        Some(size) => size / 4,
        None => DEFAULT_DISPLACEMENT,
    }
}

pub fn wall_corners(shape: i32, rotation: i32) -> &'static [(i32, i32)] {
    match (shape, rotation & 3) {
        (WALL_STRAIGHT, 0) => &[(0, 0), (0, 1)],
        (WALL_STRAIGHT, 1) => &[(0, 1), (1, 1)],
        (WALL_STRAIGHT, 2) => &[(1, 0), (1, 1)],
        (WALL_STRAIGHT, 3) => &[(0, 0), (1, 0)],
        (WALL_DIAGONAL_CORNER | WALL_STRAIGHT_CORNER, 0) => &[(0, 1)],
        (WALL_DIAGONAL_CORNER | WALL_STRAIGHT_CORNER, 1) => &[(1, 1)],
        (WALL_DIAGONAL_CORNER | WALL_STRAIGHT_CORNER, 2) => &[(1, 0)],
        (WALL_DIAGONAL_CORNER | WALL_STRAIGHT_CORNER, 3) => &[(0, 0)],
        _ => &[],
    }
}

pub fn bake_static_shadows<'a>(
    planes: usize,
    scene_size: i32,
    placements: &[ScenePlacement],
    resolve: impl Fn(i32) -> Option<&'a LocType>,
    size2d: impl Fn(&ScenePlacement, &LocType) -> Option<i32>,
    textures_enabled: bool,
) -> (Vec<ShadowGrid>, BakeStats) {
    let mut grids: Vec<ShadowGrid> = (0..planes).map(|_| ShadowGrid::new(scene_size)).collect();
    let mut stats = BakeStats::default();
    for placement in placements {
        stats.placements += 1;
        let Some(loc) = resolve(placement.id) else {
            stats.missing_type += 1;
            continue;
        };
        if !textures_enabled && loc.requires_textures {
            stats.skipped_textures += 1;
            continue;
        }
        let Some(grid) = usize::try_from(placement.plane)
            .ok()
            .and_then(|plane| grids.get_mut(plane))
        else {
            continue;
        };
        let (base_x, base_y) = (placement.scene_x, placement.scene_y);
        if placement.kind == SCENERY_INTERACT || placement.kind == GROUND_INTERACT {
            let (size_x, size_y) = if placement.rotation != 1 && placement.rotation != 3 {
                (loc.size_x, loc.size_y)
            } else {
                (loc.size_y, loc.size_x)
            };
            stats.interacts += 1;
            if !is_stationary(loc) {
                stats.animated_interacts += 1;
            }
            let end_x = base_x + size_x - 1;
            let end_y = base_y + size_y - 1;
            if base_x < 0 || base_y < 0 || end_x >= scene_size || end_y >= scene_size {
                stats.interacts_out_of_scene += 1;
                continue;
            }
            if !loc.static_shadow {
                continue;
            }
            let model = size2d(placement, loc);
            if model.is_some() && is_stationary(loc) {
                stats.model_displacements += 1;
            }
            let displacement = interact_displacement(loc, model).min(MAX_INTERACT_SHADOW);
            for offset_x in 0..=size_x {
                for offset_y in 0..=size_y {
                    grid.set_tile_shadow(base_x + offset_x, base_y + offset_y, displacement);
                }
            }
        } else if loc.static_shadow {
            let corners = wall_corners(placement.kind, placement.rotation);
            if !corners.is_empty() {
                stats.walls += 1;
            }
            for (offset_x, offset_y) in corners {
                grid.set_tile_shadow(base_x + offset_x, base_y + offset_y, i32::from(WALL_SHADOW));
            }
        }
    }
    (grids, stats)
}
