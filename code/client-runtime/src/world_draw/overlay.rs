use super::{
    DrawBuilder, DrawKind, DrawMesh, HeightMap, TILE_UNITS, WorldDraw, clone_vertices, emit,
    entity_faces, rotate_y,
};
use crate::world_scene::WorldScene;
use openrs_entity::config::load_mesh;
use openrs_entity::mesh::Mesh as EntityMesh;
use openrs_protocol::entity_info::Appearance;

const NEAR_LIMIT: i64 = 262_144;
const ENTITY_LIMIT: i64 = 92_160_000;
const ARROW_LIFT: i32 = 20;
const UNITS_PER_RADIAN: f64 = 2607.5945876176133;
const ANGLE_MASK: i32 = 0x3fff;
const ARROW_NPC: u8 = 1;
const ARROW_TILE: u8 = 2;
const ARROW_PLAYER: u8 = 10;
pub const ARROW_SLOTS: usize = 8;
pub const TRAIL_SLOTS: usize = 9;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HintArrowSpec {
    pub slot: u8,
    pub target_type: u8,
    pub x: i32,
    pub y: i32,
    pub distance: u16,
    pub target_index: u16,
    pub model_id: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HintTrailSpec {
    pub slot: u8,
    pub model_id: i32,
    pub plane: u8,
    pub placements: Vec<(i32, i32)>,
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct HintSet {
    pub arrows: Vec<HintArrowSpec>,
    pub trails: Vec<HintTrailSpec>,
}

impl HintSet {
    pub fn is_empty(&self) -> bool {
        self.arrows.is_empty() && self.trails.is_empty()
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlayerIdentity {
    pub username: String,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
}

impl From<&Appearance> for PlayerIdentity {
    fn from(appearance: &Appearance) -> Self {
        Self {
            username: appearance.username.clone(),
            prefix_title: appearance.prefix_title.clone(),
            postfix_title: appearance.postfix_title.clone(),
        }
    }
}

pub fn arrow_dimension(target_type: u8, distance: u16) -> i64 {
    if target_type == ARROW_TILE {
        let reach = i64::from(distance) << 9;
        reach * reach
    } else {
        ENTITY_LIMIT
    }
}

pub fn arrow_rotation(dx: i64, dy: i64, limit: i64, yaw: i32) -> Option<i32> {
    let squared = dx * dx + dy * dy;
    if squared < NEAR_LIMIT || squared > limit {
        return None;
    }
    let angle = (dx as f64).atan2(dy as f64) * UNITS_PER_RADIAN - f64::from(yaw);
    Some((angle as i32) & ANGLE_MASK)
}

pub fn trail_nodes(
    placements: &[(i32, i32)],
    base: (i32, i32),
    dimensions: (i32, i32),
) -> Vec<(i32, i32)> {
    let mut nodes = Vec::new();
    let Some(&first) = placements.first() else {
        return nodes;
    };
    let mut previous = first;
    for &current in &placements[1..] {
        while previous != current {
            previous.0 += (current.0 - previous.0).signum();
            previous.1 += (current.1 - previous.1).signum();
            let x = previous.0 - base.0;
            let y = previous.1 - base.1;
            if x >= 0 && x < dimensions.0 && y >= 0 && y < dimensions.1 {
                nodes.push((x, y));
            }
        }
        previous = current;
    }
    nodes
}

fn node_origin(tile: (i32, i32)) -> (i32, i32) {
    ((tile.0 << 9) + 256, (tile.1 << 9) + 256)
}

fn hint_mesh(builder: &DrawBuilder, model_id: i32) -> Option<EntityMesh> {
    let mut mesh = load_mesh(&builder.store, model_id).ok()??;
    if mesh.version < 13 {
        mesh.upscale().ok()?;
    }
    Some(mesh)
}

impl DrawBuilder {
    pub fn set_hints(&mut self, hints: HintSet) {
        self.hints = hints;
    }

    pub fn player_identity(&mut self, bytes: &[u8]) -> Option<PlayerIdentity> {
        let defaults = self.defaults()?;
        let appearance = self.decode_worn(bytes, &defaults)?;
        Some(PlayerIdentity::from(&appearance))
    }

    pub(super) fn build_hints(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        if self.hints.is_empty() {
            return;
        }
        self.build_arrows(scene, heights, draw);
        self.build_trails(scene, heights, draw);
    }

    fn arrow_target(&self, scene: &WorldScene, spec: &HintArrowSpec) -> Option<(i32, i32)> {
        let base = scene.base();
        let place = |x: i32, y: i32, size: i32| {
            (
                (x - base.0) * TILE_UNITS + size * 256,
                (y - base.1) * TILE_UNITS + size * 256,
            )
        };
        match spec.target_type {
            ARROW_NPC => {
                let (x, y, _) = scene.npc_position(spec.target_index)?;
                Some(place(x, y, 1))
            }
            ARROW_TILE => Some((spec.x, spec.y)),
            ARROW_PLAYER => {
                let (x, y, _) = scene.player_position(spec.target_index)?;
                Some(place(x, y, 1))
            }
            _ => None,
        }
    }

    fn build_arrows(&mut self, scene: &WorldScene, heights: &HeightMap, draw: &mut WorldDraw) {
        let Some((own_x, own_y, plane)) = scene.own_position() else {
            return;
        };
        let base = scene.base();
        let own = (
            (own_x - base.0) * TILE_UNITS + 256,
            (own_y - base.1) * TILE_UNITS + 256,
        );
        let yaw = (scene.player_orientation(scene.own_index()) & 2047) * 8;
        let specs: Vec<HintArrowSpec> = self.hints.arrows.iter().rev().cloned().collect();
        for spec in specs {
            if spec.model_id == -1 {
                continue;
            }
            let Some(target) = self.arrow_target(scene, &spec) else {
                continue;
            };
            let dx = i64::from(target.0 - own.0);
            let dy = i64::from(target.1 - own.1);
            let limit = arrow_dimension(spec.target_type, spec.distance);
            let Some(rotation) = arrow_rotation(dx, dy, limit, yaw) else {
                continue;
            };
            let Some(mesh) = hint_mesh(self, spec.model_id) else {
                continue;
            };
            let Some(faces) = entity_faces(&mesh) else {
                draw.stats.skipped += 1;
                continue;
            };
            let mut local = clone_vertices(
                &mesh.vertices_x,
                &mesh.vertices_y,
                &mesh.vertices_z,
                mesh.num_vertices,
            );
            rotate_y(&mut local.xs, &mut local.zs, rotation);
            rotate_y(&mut local.xs, &mut local.zs, yaw);
            let ground = self.ground(heights, draw, plane, own.0, own.1);
            let mut out = DrawMesh::new(
                DrawKind::HintArrow,
                spec.model_id,
                plane,
                (own_x, own_y),
                (own.0, ground - ARROW_LIFT, own.1),
            );
            emit(&mut out, &local, &faces);
            draw.stats.hint_arrows += 1;
            draw.meshes.push(out);
        }
    }

    fn build_trails(&mut self, scene: &WorldScene, heights: &HeightMap, draw: &mut WorldDraw) {
        let base = scene.base();
        let size = scene.map_size();
        let specs = self.hints.trails.clone();
        for spec in specs {
            let Some(mesh) = hint_mesh(self, spec.model_id) else {
                continue;
            };
            let Some(faces) = entity_faces(&mesh) else {
                draw.stats.skipped += 1;
                continue;
            };
            let local = clone_vertices(
                &mesh.vertices_x,
                &mesh.vertices_y,
                &mesh.vertices_z,
                mesh.num_vertices,
            );
            for tile in trail_nodes(&spec.placements, base, (size, size)) {
                let (x, z) = node_origin(tile);
                let y = self.ground(heights, draw, spec.plane, x, z);
                let mut out = DrawMesh::new(
                    DrawKind::HintTrail,
                    spec.model_id,
                    spec.plane,
                    (tile.0 + base.0, tile.1 + base.1),
                    (x, y, z),
                );
                emit(&mut out, &local, &faces);
                draw.stats.hint_trail_nodes += 1;
                draw.meshes.push(out);
            }
        }
    }
}

#[cfg(test)]
mod tests;
