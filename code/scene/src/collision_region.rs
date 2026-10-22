use crate::collision::CollisionMap;
use crate::loc_type::LocType;
use crate::locs::LocPlacement;
use crate::terrain::{Terrain, MAP_SIZE, PLANES};

pub const SHAPE_WALL_STRAIGHT: i32 = 0;
pub const SHAPE_WALL_INTERACT: i32 = 9;
pub const SHAPE_SCENERY_INTERACT: i32 = 10;
pub const SHAPE_GROUND_INTERACT: i32 = 11;
pub const SHAPE_GROUND_DECORATION: i32 = 22;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct LocCollisionProps {
    pub size_x: i32,
    pub size_y: i32,
    pub blocks_movement: i32,
    pub blocks_projectiles: bool,
    pub ignore_clip_on_alt_route: bool,
}

impl From<&LocType> for LocCollisionProps {
    fn from(loc: &LocType) -> Self {
        Self {
            size_x: loc.size_x,
            size_y: loc.size_y,
            blocks_movement: loc.blocks_movement,
            blocks_projectiles: loc.blocks_projectiles,
            ignore_clip_on_alt_route: loc.ignore_clip_on_alt_route,
        }
    }
}

pub fn is_wall_shape(shape: i32) -> bool {
    (0..=3).contains(&shape) || shape == SHAPE_WALL_INTERACT
}

pub fn is_roof_shape(shape: i32) -> bool {
    (12..=17).contains(&shape)
}

pub fn is_roof_edge_shape(shape: i32) -> bool {
    (18..=21).contains(&shape)
}

#[derive(Clone, Debug)]
pub struct SceneCollision {
    pub region_size_x: i32,
    pub region_size_y: i32,
    masks: Vec<u8>,
    pub maps: Vec<CollisionMap>,
}

impl SceneCollision {
    pub fn new(region_size_x: i32, region_size_y: i32) -> Self {
        let cells = PLANES * region_size_x as usize * region_size_y as usize;
        Self {
            region_size_x,
            region_size_y,
            masks: vec![0; cells],
            maps: (0..PLANES)
                .map(|_| CollisionMap::create_clip_map(region_size_x, region_size_y))
                .collect(),
        }
    }

    fn mask_index(&self, plane: usize, x: i32, y: i32) -> usize {
        (plane * self.region_size_x as usize + x as usize) * self.region_size_y as usize
            + y as usize
    }

    pub fn tile_function_mask(&self, plane: usize, x: i32, y: i32) -> u8 {
        if x >= 0 && y >= 0 && x < self.region_size_x && y < self.region_size_y && plane < PLANES {
            self.masks[self.mask_index(plane, x, y)]
        } else {
            0
        }
    }

    pub fn load_terrain(&mut self, terrain: &Terrain, chunk_x: i32, chunk_y: i32) {
        for map in &mut self.maps {
            for rx in 0..MAP_SIZE as i32 {
                for ry in 0..MAP_SIZE as i32 {
                    let scene_x = rx + chunk_x;
                    let scene_y = ry + chunk_y;
                    if scene_x >= 0
                        && scene_x < self.region_size_x
                        && scene_y >= 0
                        && scene_y < self.region_size_y
                    {
                        map.unblock(scene_x, scene_y);
                    }
                }
            }
        }
        for plane in 0..PLANES {
            for rx in 0..MAP_SIZE {
                for ry in 0..MAP_SIZE {
                    let scene_x = rx as i32 + chunk_x;
                    let scene_y = ry as i32 + chunk_y;
                    if scene_x >= 0
                        && scene_x < self.region_size_x
                        && scene_y >= 0
                        && scene_y < self.region_size_y
                    {
                        let flags = terrain
                            .tile(plane, rx, ry)
                            .map_or(0, |tile| tile.render_flags);
                        let index = self.mask_index(plane, scene_x, scene_y);
                        self.masks[index] = flags;
                    }
                }
            }
        }
    }

    pub fn apply_unwalkable(&mut self) {
        for plane in 0..PLANES {
            for x in 0..self.region_size_x {
                for y in 0..self.region_size_y {
                    if self.masks[self.mask_index(plane, x, y)] & 0x1 != 0 {
                        let mut height = plane as i32;
                        if self.masks[self.mask_index(1, x, y)] & 0x2 != 0 {
                            height = plane as i32 - 1;
                        }
                        if height >= 0 {
                            self.maps[height as usize].set_unwalkable(x, y);
                        }
                    }
                }
            }
        }
    }

    pub fn add_locs<F>(&mut self, placements: &[LocPlacement], chunk_x: i32, chunk_y: i32, props: F)
    where
        F: Fn(i32) -> Option<LocCollisionProps>,
    {
        for placement in placements {
            let scene_x = placement.local_x + chunk_x;
            let scene_y = placement.local_y + chunk_y;
            if scene_x > 0
                && scene_y > 0
                && scene_x < self.region_size_x - 1
                && scene_y < self.region_size_y - 1
            {
                let mut collision_plane = placement.plane;
                if self.tile_function_mask(1, scene_x, scene_y) & 0x2 == 2 {
                    collision_plane -= 1;
                }
                if collision_plane >= 0 && (collision_plane as usize) < self.maps.len() {
                    if let Some(loc) = props(placement.id) {
                        add_object_collision(
                            &mut self.maps[collision_plane as usize],
                            &loc,
                            scene_x,
                            scene_y,
                            placement.rotation,
                            placement.kind,
                        );
                    }
                }
            }
        }
    }
}

pub fn add_object_collision(
    map: &mut CollisionMap,
    loc: &LocCollisionProps,
    base_x: i32,
    base_y: i32,
    rotation: i32,
    shape: i32,
) {
    let (size_x, size_y) = if rotation != 1 && rotation != 3 {
        (loc.size_x, loc.size_y)
    } else {
        (loc.size_y, loc.size_x)
    };
    let not_alt = !loc.ignore_clip_on_alt_route;
    if shape == SHAPE_GROUND_DECORATION {
        if loc.blocks_movement == 1 {
            map.flag_blocked_decoration(base_x, base_y);
        }
    } else if shape != SHAPE_SCENERY_INTERACT && shape != SHAPE_GROUND_INTERACT {
        if !is_roof_shape(shape) && !is_roof_edge_shape(shape) {
            if loc.blocks_movement != 0 {
                if (0..=3).contains(&shape) {
                    map.flag_boundary(
                        base_x,
                        base_y,
                        shape,
                        rotation,
                        loc.blocks_projectiles,
                        not_alt,
                    );
                } else if shape == SHAPE_WALL_INTERACT {
                    map.flag_blocked_object(
                        base_x,
                        base_y,
                        size_x,
                        size_y,
                        loc.blocks_projectiles,
                        not_alt,
                    );
                }
            }
        } else if loc.blocks_movement != 0 {
            map.flag_blocked_object(
                base_x,
                base_y,
                size_x,
                size_y,
                loc.blocks_projectiles,
                not_alt,
            );
        }
    } else if loc.blocks_movement != 0 {
        map.flag_blocked_object(
            base_x,
            base_y,
            size_x,
            size_y,
            loc.blocks_projectiles,
            not_alt,
        );
    }
}
