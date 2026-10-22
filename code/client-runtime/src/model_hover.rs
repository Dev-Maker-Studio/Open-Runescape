use crate::world_draw::{DrawKind, DrawMesh, WorldDraw};

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct ProjectionViewport {
    pub origin_x: f32,
    pub origin_y: f32,
    pub center_x: f32,
    pub center_y: f32,
}

pub fn project_vertex(
    mvp: &[f32; 16],
    viewport: &ProjectionViewport,
    vertex: [i32; 3],
) -> Option<(i32, i32)> {
    let (x, y, z) = (vertex[0] as f32, vertex[1] as f32, vertex[2] as f32);
    let clip_z = mvp[2] * x + mvp[6] * y + mvp[10] * z + mvp[14];
    let clip_w = mvp[3] * x + mvp[7] * y + mvp[11] * z + mvp[15];
    if clip_z >= -clip_w {
        let clip_x = mvp[0] * x + mvp[4] * y + mvp[8] * z + mvp[12];
        let clip_y = mvp[1] * x + mvp[5] * y + mvp[9] * z + mvp[13];
        Some((
            (viewport.origin_x + viewport.center_x * clip_x / clip_w) as i32,
            (viewport.origin_y + viewport.center_y * clip_y / clip_w) as i32,
        ))
    } else {
        None
    }
}

pub fn is_hovering(
    mouse: (i32, i32),
    y: [i32; 3],
    x: [i32; 3],
) -> bool {
    let (mouse_x, mouse_y) = mouse;
    (mouse_y >= y[0] || mouse_y >= y[1] || mouse_y >= y[2])
        && (mouse_y <= y[0] || mouse_y <= y[1] || mouse_y <= y[2])
        && (mouse_x >= x[0] || mouse_x >= x[1] || mouse_x >= x[2])
        && (mouse_x <= x[0] || mouse_x <= x[1] || mouse_x <= x[2])
}

pub fn triangles_hit(
    mvp: &[f32; 16],
    viewport: &ProjectionViewport,
    positions: &[i32],
    mouse: (i32, i32),
) -> bool {
    positions.chunks_exact(9).any(|triangle| {
        let mut xs = [0; 3];
        let mut ys = [0; 3];
        for corner in 0..3 {
            let vertex = [
                triangle[corner * 3],
                triangle[corner * 3 + 1],
                triangle[corner * 3 + 2],
            ];
            let Some((sx, sy)) = project_vertex(mvp, viewport, vertex) else {
                return false;
            };
            xs[corner] = sx;
            ys[corner] = sy;
        }
        is_hovering(mouse, ys, xs)
    })
}

pub fn entity_hits(
    draw: &WorldDraw,
    mvp: &[f32; 16],
    viewport: &ProjectionViewport,
    mouse: (i32, i32),
) -> Vec<(u8, i32)> {
    draw.meshes
        .iter()
        .filter(|mesh: &&DrawMesh| matches!(mesh.kind, DrawKind::Player | DrawKind::Npc))
        .filter(|mesh| triangles_hit(mvp, viewport, &mesh.positions, mouse))
        .map(|mesh| (mesh.kind.code(), mesh.id))
        .collect()
}

#[cfg(test)]
mod tests;
