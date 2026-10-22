use super::{
    DrawBuilder, DrawKind, DrawMesh, HeightMap, Local, TILE_UNITS, WorldDraw, emit, entity_faces,
    rotate_y,
};
use crate::world_pose::TrackStart;
use crate::world_resources::Lookup;
use crate::world_scene::{Projectile, WorldScene};
use openrs_entity::config::SpotAnimType;
use openrs_entity::mesh::Mesh as EntityMesh;
use openrs_entity::pose::Pose;

const UNITS_PER_TURN: f64 = 16384.0;
const ANGLE_FACTOR: f64 = 0.02454369;
const NO_SPOT: i32 = 65535;

pub(super) struct SpotRequest {
    pub kind: DrawKind,
    pub id: i32,
    pub plane: u8,
    pub tile: (i32, i32),
    pub origin: (i32, i32, i32),
    pub yaw: i32,
    pub pitch: f64,
    pub key: (i32, i32),
    pub speed: i32,
    pub cycle: u64,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub(super) struct Flight {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: i32,
    pub pitch: f64,
}

pub(super) struct FlightPlan {
    pub start: (f64, f64, f64),
    pub target: (f64, f64, f64),
    pub left: f64,
    pub angle: i32,
    pub floor_height: bool,
    pub start_height: f64,
}

pub(super) fn fly(plan: &FlightPlan, elapsed: f64, floor: &dyn Fn(f64, f64) -> f64) -> Flight {
    let (ax, ay, az) = plan.start;
    let (tx, ty, tz) = plan.target;
    let (dx, dz) = (tx - ax, tz - az);
    let step_x = dx / plan.left;
    let step_z = dz / plan.left;
    let horizontal = (step_x * step_x + step_z * step_z).sqrt();
    let x = ax + step_x * elapsed;
    let z = az + step_z * elapsed;
    let vertical;
    let y;
    if plan.floor_height {
        vertical = (ty - ay) / plan.left;
        y = floor(x, z) - plan.start_height;
    } else if plan.angle != -1 {
        let initial = -horizontal * (ANGLE_FACTOR * f64::from(plan.angle)).tan();
        let acceleration = ((ty - ay) - initial * plan.left) * 2.0 / (plan.left * plan.left);
        vertical = initial + elapsed * acceleration;
        y = ay + initial * elapsed + elapsed * elapsed * 0.5 * acceleration;
    } else {
        vertical = (ty - ay) / plan.left;
        y = ay + vertical * elapsed;
    }
    let yaw = (step_x.atan2(step_z) - std::f64::consts::PI) * UNITS_PER_TURN
        / (2.0 * std::f64::consts::PI);
    Flight {
        x,
        y,
        z,
        yaw: yaw.round() as i32,
        pitch: vertical.atan2(horizontal),
    }
}

fn pitched(local: &mut Local, pitch: f64) {
    if pitch == 0.0 {
        return;
    }
    let (sin, cos) = pitch.sin_cos();
    for (y, z) in local.ys.iter_mut().zip(local.zs.iter_mut()) {
        let (fy, fz) = (f64::from(*y), f64::from(*z));
        *y = (fy * cos - fz * sin) as i32;
        *z = (fy * sin + fz * cos) as i32;
    }
}

impl DrawBuilder {
    fn spot_type(&mut self, id: i32) -> Option<SpotAnimType> {
        if let Some(cached) = self.spot_types.get(&id) {
            return cached.clone();
        }
        match self.store.spot_anim_type(id) {
            Lookup::Unknown => None,
            Lookup::Absent => {
                self.spot_types.insert(id, None);
                None
            }
            Lookup::Present(bytes) => {
                let decoded = SpotAnimType::decode(&bytes).ok();
                self.spot_types.insert(id, decoded.clone());
                decoded
            }
        }
    }

    fn spot_draw(
        &mut self,
        request: SpotRequest,
        ty: &SpotAnimType,
        mesh: &EntityMesh,
        draw: &mut WorldDraw,
    ) {
        let Some(faces) = entity_faces(mesh) else {
            draw.stats.skipped += 1;
            return;
        };
        let Ok(mut pose) = Pose::from_mesh(mesh) else {
            draw.stats.skipped += 1;
            return;
        };
        if ty.sequence_id >= 0 {
            let start = TrackStart {
                seq_id: ty.sequence_id,
                started: 0,
                speed: request.speed,
                replay: if ty.replay { 0 } else { 2 },
            };
            if self
                .pose
                .animate_tracked(&mut pose, request.key, start, request.cycle, true)
            {
                draw.stats.posed += 1;
            }
        }
        let count = usize::try_from(pose.num_vertices).unwrap_or(0);
        let mut local = Local {
            xs: pose.vertices_x.clone(),
            ys: pose.vertices_y.clone(),
            zs: pose.vertices_z.clone(),
            mirrored: false,
        };
        local.xs.resize(count, 0);
        local.ys.resize(count, 0);
        local.zs.resize(count, 0);
        if ty.scale_xz != 128 {
            for value in local.xs.iter_mut().chain(local.zs.iter_mut()) {
                *value = value.wrapping_mul(ty.scale_xz) >> 7;
            }
        }
        if ty.scale_y != 128 {
            for value in &mut local.ys {
                *value = value.wrapping_mul(ty.scale_y) >> 7;
            }
        }
        if request.yaw != 0 {
            rotate_y(&mut local.xs, &mut local.zs, request.yaw);
        }
        pitched(&mut local, request.pitch);
        let mut out = DrawMesh::new(
            request.kind,
            request.id,
            request.plane,
            request.tile,
            request.origin,
        );
        let colors = pose.faces_color.clone();
        let alphas = pose.faces_alpha.clone();
        let posed = super::Faces {
            color: colors.as_deref().or(faces.color),
            alpha: alphas.as_deref().or(faces.alpha),
            ..faces
        };
        emit(&mut out, &local, &posed);
        match request.kind {
            DrawKind::Projectile => draw.stats.projectiles += 1,
            _ => draw.stats.spots += 1,
        }
        draw.meshes.push(out);
    }

    fn spot_mesh(&mut self, id: i32) -> Option<(SpotAnimType, EntityMesh)> {
        let ty = self.spot_type(id)?;
        let mesh = ty.mesh(&self.store).ok()??;
        Some((ty, mesh))
    }

    pub(super) fn build_spots(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let ground: Vec<_> = scene.spots().iter().map(|(k, v)| (*k, *v)).collect();
        for ((plane, abs_x, abs_y), entry) in ground {
            if entry.id < 0 || entry.id == NO_SPOT {
                continue;
            }
            let Some((ty, mesh)) = self.spot_mesh(entry.id) else {
                continue;
            };
            let (tile_x, tile_y) = (abs_x - base.0, abs_y - base.1);
            let x = tile_x * TILE_UNITS + 256;
            let z = tile_y * TILE_UNITS + 256;
            let y = self.ground(heights, draw, plane, x, z) - i32::from(entry.height);
            let request = SpotRequest {
                kind: DrawKind::SpotAnim,
                id: entry.id,
                plane,
                tile: (abs_x, abs_y),
                origin: (x, y, z),
                yaw: i32::from(entry.rotation) * 2048 + ty.fixed_rotation(),
                pitch: 0.0,
                key: (
                    16 + i32::from(plane),
                    (abs_x & 0x3fff) << 16 | (abs_y & 0x3fff),
                ),
                speed: i32::from(entry.speed),
                cycle: scene.cycle(),
            };
            self.spot_draw(request, &ty, &mesh, draw);
        }
        self.build_entity_spots(scene, heights, draw);
    }

    fn build_entity_spots(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let mut jobs = Vec::new();
        for (index, state) in &scene.player_states {
            if let Some(position) = scene.player_position(*index) {
                for spot in &state.spots {
                    jobs.push((32, *index, position, spot.clone()));
                }
            }
        }
        for (index, state) in &scene.npc_states {
            if let Some(position) = scene.npc_position(*index) {
                for spot in &state.spots {
                    jobs.push((33, *index, position, spot.clone()));
                }
            }
        }
        for (code, index, (abs_x, abs_y, plane), spot) in jobs {
            if spot.id < 0 || spot.id == NO_SPOT {
                continue;
            }
            let Some((ty, mesh)) = self.spot_mesh(spot.id) else {
                continue;
            };
            let x = (abs_x - base.0) * TILE_UNITS + 256;
            let z = (abs_y - base.1) * TILE_UNITS + 256;
            let lift = (spot.packed >> 16) << 2;
            let y = self.ground(heights, draw, plane, x, z) - lift;
            let request = SpotRequest {
                kind: DrawKind::SpotAnim,
                id: spot.id,
                plane,
                tile: (abs_x, abs_y),
                origin: (x, y, z),
                yaw: i32::from(spot.rotation) * 2048 + ty.fixed_rotation(),
                pitch: 0.0,
                key: (code, i32::from(index) * 256 + i32::from(spot.index)),
                speed: spot.packed & 0xffff,
                cycle: scene.cycle(),
            };
            self.spot_draw(request, &ty, &mesh, draw);
        }
    }

    pub(super) fn build_projectiles(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        let cycle = scene.cycle();
        let projectiles: Vec<Projectile> = scene.projectiles().to_vec();
        for projectile in projectiles {
            if projectile.half_square {
                draw.stats.skipped += 1;
                continue;
            }
            let spawn = projectile.received_cycle + u64::from(projectile.start_time);
            if cycle < spawn || cycle > projectile.expires_at() {
                continue;
            }
            let id = i32::from(projectile.spot_anim);
            if id == NO_SPOT {
                continue;
            }
            let Some((ty, mesh)) = self.spot_mesh(id) else {
                continue;
            };
            let plane = projectile.plane;
            let base = scene.base();
            let local = |tile_x: i32, tile_y: i32| {
                (
                    f64::from((tile_x - base.0) * TILE_UNITS + 256),
                    f64::from((tile_y - base.1) * TILE_UNITS + 256),
                )
            };
            let (sx, sz) = local(projectile.start_x, projectile.start_y);
            let (mut tx, mut tz) = local(
                projectile.start_x + i32::from(projectile.x_off),
                projectile.start_y + i32::from(projectile.y_off),
            );
            if let Some((x, z)) = lock_target(scene, projectile.lock_on, plane) {
                (tx, tz) = local(x, z);
            }
            let start_height = f64::from(projectile.start_height * 4);
            let end_height = f64::from(i32::from(projectile.end_height) * 4);
            let heights_ref = heights;
            let floor = |x: f64, z: f64| -> f64 {
                f64::from(heights_ref.sample(plane, x as i32, z as i32).unwrap_or(0))
            };
            let (mut ax, mut az) = (sx, sz);
            let (dx, dz) = (tx - sx, tz - sz);
            let size = (dx * dx + dz * dz).sqrt();
            if size != 0.0 {
                let slope = f64::from(i32::from(projectile.slope) * 4);
                ax += dx * slope / size;
                az += dz * slope / size;
            }
            let ay = floor(sx, sz) - start_height;
            let ay = if projectile.floor_height {
                floor(ax, az) - start_height
            } else {
                ay
            };
            let plan = FlightPlan {
                start: (ax, ay, az),
                target: (tx, floor(tx, tz) - end_height, tz),
                left: f64::from(projectile.end_time) + 1.0 - f64::from(projectile.start_time),
                angle: projectile.angle,
                floor_height: projectile.floor_height,
                start_height,
            };
            let flight = fly(&plan, (cycle - spawn) as f64, &floor);
            let request = SpotRequest {
                kind: DrawKind::Projectile,
                id,
                plane,
                tile: (projectile.start_x, projectile.start_y),
                origin: (flight.x as i32, flight.y as i32, flight.z as i32),
                yaw: flight.yaw + ty.fixed_rotation(),
                pitch: flight.pitch,
                key: (
                    34,
                    (projectile.received_cycle as i32)
                        .wrapping_mul(31)
                        .wrapping_add(projectile.start_x * 8191 + projectile.start_y)
                        .wrapping_add(id),
                ),
                speed: 0,
                cycle,
            };
            self.spot_draw(request, &ty, &mesh, draw);
        }
    }
}

fn lock_target(scene: &WorldScene, lock_on: i32, plane: u8) -> Option<(i32, i32)> {
    let position = match lock_on {
        0 => return None,
        value if value > 0 => scene.npc_position(u16::try_from(value - 1).ok()?),
        value => scene.player_position(u16::try_from(-value - 1).ok()?),
    }?;
    (position.2 == plane).then_some((position.0, position.1))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn plan(angle: i32, floor_height: bool) -> FlightPlan {
        FlightPlan {
            start: (0.0, -100.0, 0.0),
            target: (1000.0, -300.0, 0.0),
            left: 10.0,
            angle,
            floor_height,
            start_height: 0.0,
        }
    }

    #[test]
    fn a_flat_flight_is_linear_and_reaches_the_target() {
        let flat = |_: f64, _: f64| 0.0;
        let end = fly(&plan(-1, false), 10.0, &flat);
        assert!((end.x - 1000.0).abs() < 1e-9);
        assert!((end.y + 300.0).abs() < 1e-9);
        let mid = fly(&plan(-1, false), 5.0, &flat);
        assert!((mid.x - 500.0).abs() < 1e-9);
        assert!((mid.y + 200.0).abs() < 1e-9);
    }

    #[test]
    fn an_arc_ends_on_the_target_and_starts_climbing() {
        let flat = |_: f64, _: f64| 0.0;
        let end = fly(&plan(64, false), 10.0, &flat);
        assert!((end.y + 300.0).abs() < 1e-6);
        let start = fly(&plan(64, false), 0.0, &flat);
        assert!(start.pitch < 0.0);
    }

    #[test]
    fn terrain_following_flights_sample_the_ground() {
        let slope = |x: f64, _: f64| x / 10.0;
        let at = fly(&plan(-1, true), 5.0, &slope);
        assert!((at.y - 50.0).abs() < 1e-9);
    }

    #[test]
    fn flights_face_along_the_travel_direction() {
        let flat = |_: f64, _: f64| 0.0;
        let east = fly(&plan(-1, false), 1.0, &flat);
        let expected = ((1000.0f64).atan2(0.0) - std::f64::consts::PI) * 16384.0
            / (2.0 * std::f64::consts::PI);
        assert_eq!(east.yaw, expected.round() as i32);
    }
}
