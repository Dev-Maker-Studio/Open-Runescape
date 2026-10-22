use openrs_scene::ground_geometry::GroundGeometry;
use openrs_scene::ground_lowering::{
    DEFAULT_SURFACE_WATER, GroundLoweringLimits, GroundMaterialShading, OrdinaryGroundVertex,
    TexturedGroundInput, UNTEXTURED_MATERIAL_ID, lower_textured_ground, select_textured_ground,
};
use openrs_scene::material::MaterialTable;
use openrs_scene::rgb::HslRgbLut;
use openrs_scene::static_shadows::ShadowGrid;
use openrs_scene::surface::SurfaceHeights;
use serde::Serialize;
use sha2::{Digest, Sha256};
use std::collections::BTreeSet;

const MAX_SELECTED_INDICES: usize = 8_000_000;

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LoweredMaterial {
    pub material_id: i32,
    pub texture_scale: i32,
    pub source_first_vertex: u32,
    pub vertex_count: usize,
    pub index_count: usize,
    pub sha256: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LoweredPlane {
    pub scope: &'static str,
    pub plane: usize,
    pub width: usize,
    pub height: usize,
    pub tile_count: usize,
    pub geometry_vertex_count: usize,
    pub lowered_vertex_count: usize,
    pub material_count: usize,
    pub textured_material_count: usize,
    pub untextured_material_count: usize,
    pub vertex_count: usize,
    pub index_count: usize,
    pub triangle_count: usize,
    pub vertex_stride: usize,
    pub sha256: String,
    pub materials: Vec<LoweredMaterial>,
    pub shadows_baked: bool,
    pub normals_from_underwater: bool,
    pub objects_applied: bool,
    pub terrain_lowered: bool,
    pub texture_pixels_decoded: bool,
    pub rendered_world: bool,
}

pub struct LoweredMesh {
    pub report: LoweredPlane,
    pub materials: Vec<LoweredMeshMaterial>,
}

pub struct LoweredMeshMaterial {
    pub material_id: i32,
    pub texture_scale: i32,
    pub vertices: Vec<u8>,
    pub indices: Vec<u32>,
}

pub const VERTEX_STRIDE: usize = 40;

pub fn encode_vertices(vertices: &[OrdinaryGroundVertex]) -> Vec<u8> {
    let mut bytes = Vec::with_capacity(vertices.len() * VERTEX_STRIDE);
    for vertex in vertices {
        for value in vertex.position {
            bytes.extend_from_slice(&value.to_le_bytes());
        }
        bytes.extend_from_slice(&vertex.rgba);
        for value in vertex.texcoord.into_iter().chain(vertex.normal) {
            bytes.extend_from_slice(&value.to_le_bytes());
        }
    }
    bytes
}

fn hex(digest: &[u8]) -> String {
    digest.iter().map(|byte| format!("{byte:02x}")).collect()
}

fn plane_grid(surface: &SurfaceHeights, plane: usize) -> Result<Vec<i32>, String> {
    let size = surface.layout().map_size().tiles();
    let stride = size + 1;
    let mut grid = vec![0i32; stride * stride];
    for y in 0..stride {
        for x in 0..stride {
            grid[x + y * stride] = surface
                .height_native_units(plane, x, y)
                .ok_or_else(|| format!("surface height unavailable at {x},{y}"))?;
        }
    }
    Ok(grid)
}

pub fn lit_normal_heights(
    surface: &SurfaceHeights,
    underwater: Option<&SurfaceHeights>,
    plane: usize,
) -> Result<Vec<i32>, String> {
    let mut grid = plane_grid(surface, plane)?;
    if plane == 0
        && let Some(underwater) = underwater
    {
        let deltas = plane_grid(underwater, 0)?;
        for (height, delta) in grid.iter_mut().zip(deltas) {
            *height = height.wrapping_add(delta);
        }
    }
    Ok(grid)
}

pub fn lower_plane(
    surface: &SurfaceHeights,
    underwater: Option<&SurfaceHeights>,
    geometry: &GroundGeometry,
    plane: usize,
    rgb: &HslRgbLut,
    table: &MaterialTable,
) -> Result<LoweredMesh, String> {
    lower_plane_shadowed(surface, underwater, geometry, plane, rgb, table, None)
}

pub fn lower_plane_shadowed(
    surface: &SurfaceHeights,
    underwater: Option<&SurfaceHeights>,
    geometry: &GroundGeometry,
    plane: usize,
    rgb: &HslRgbLut,
    table: &MaterialTable,
    baked: Option<&ShadowGrid>,
) -> Result<LoweredMesh, String> {
    let size = surface.layout().map_size().tiles();
    let heights = plane_grid(surface, plane)?;
    let normal_heights = lit_normal_heights(surface, underwater, plane)?;
    let shadows = match baked {
        Some(grid) => {
            if grid.width != size + 1 || grid.cells.len() != heights.len() {
                return Err(format!(
                    "shadow grid is {} wide with {} cells, expected {} wide with {}",
                    grid.width,
                    grid.cells.len(),
                    size + 1,
                    heights.len()
                ));
            }
            grid.cells.clone()
        }
        None => vec![0u8; heights.len()],
    };
    let ids: BTreeSet<i32> = geometry
        .tiles
        .iter()
        .flat_map(|tile| tile.vertices.iter().map(|vertex| vertex.texture))
        .filter(|id| *id != UNTEXTURED_MATERIAL_ID)
        .collect();
    let shading = ids
        .iter()
        .map(|id| {
            table
                .get_material(*id)
                .map(|material| GroundMaterialShading::from_material(*id, material))
                .map_err(|error| error.to_string())
        })
        .collect::<Result<Vec<_>, _>>()?;
    let mut normalized = geometry.clone();
    for tile in normalized.tiles.iter_mut() {
        if let Some(colors) = tile.blending_colors.as_mut() {
            for color in colors.iter_mut() {
                if *color < -1 {
                    *color &= 0xFFFF;
                }
            }
        }
    }
    let geometry = &normalized;
    let compiled = lower_textured_ground(
        TexturedGroundInput {
            width: size,
            height: size,
            geometry,
            position_heights: &heights,
            normal_heights: &normal_heights,
            baked_shadows: &shadows,
            rgb,
            materials: &shading,
            tile_units: 512,
            surface_water: DEFAULT_SURFACE_WATER,
            underwater: false,
            secondary_ground_heights: false,
            hd_lighting: true,
        },
        GroundLoweringLimits::default(),
    )
    .map_err(|error| error.to_string())?;
    let visible = vec![true; size * size];
    let mut total = Sha256::new();
    let mut meshes = Vec::new();
    let mut reports = Vec::new();
    let mut vertex_count = 0usize;
    let mut index_count = 0usize;
    let mut untextured = 0usize;
    for index in 0..compiled.materials().len() {
        let selected = select_textured_ground(&compiled, index, &visible, MAX_SELECTED_INDICES)
            .map_err(|error| error.to_string())?;
        let vertices = encode_vertices(&selected.vertices);
        let mut digest = Sha256::new();
        let mut header = Vec::with_capacity(20);
        header.extend_from_slice(&selected.material_id.to_le_bytes());
        header.extend_from_slice(&selected.texture_scale.to_le_bytes());
        header.extend_from_slice(&selected.source_first_vertex.to_le_bytes());
        header.extend_from_slice(&(selected.vertices.len() as u32).to_le_bytes());
        header.extend_from_slice(&(selected.indices.len() as u32).to_le_bytes());
        digest.update(&header);
        digest.update(&vertices);
        for value in selected.indices.iter() {
            digest.update(value.to_le_bytes());
        }
        let digest = digest.finalize();
        total.update(digest);
        if selected.material_id == UNTEXTURED_MATERIAL_ID {
            untextured += 1;
        }
        vertex_count += selected.vertices.len();
        index_count += selected.indices.len();
        reports.push(LoweredMaterial {
            material_id: selected.material_id,
            texture_scale: selected.texture_scale,
            source_first_vertex: selected.source_first_vertex,
            vertex_count: selected.vertices.len(),
            index_count: selected.indices.len(),
            sha256: hex(&digest),
        });
        meshes.push(LoweredMeshMaterial {
            material_id: selected.material_id,
            texture_scale: selected.texture_scale,
            vertices,
            indices: selected.indices.into_vec(),
        });
    }
    let material_count = reports.len();
    Ok(LoweredMesh {
        report: LoweredPlane {
            scope: "textured-ground-lowering",
            plane,
            width: size,
            height: size,
            tile_count: geometry.tiles.len(),
            geometry_vertex_count: geometry.vertex_count,
            lowered_vertex_count: compiled.vertices().len(),
            material_count,
            textured_material_count: material_count - untextured,
            untextured_material_count: untextured,
            vertex_count,
            index_count,
            triangle_count: index_count / 3,
            vertex_stride: VERTEX_STRIDE,
            sha256: hex(&total.finalize()),
            materials: reports,
            shadows_baked: baked.is_some(),
            normals_from_underwater: plane == 0 && underwater.is_some(),
            objects_applied: false,
            terrain_lowered: true,
            texture_pixels_decoded: false,
            rendered_world: false,
        },
        materials: meshes,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_scene::ground_geometry::{BlendedGroundTile, GroundVertex};
    use openrs_scene::surface::{NativeMapSize, StandardMapLayout};

    fn surface() -> SurfaceHeights {
        let ids = [0u16, 1, 256, 257];
        let layout = StandardMapLayout::new([0, 0], NativeMapSize::Tiles72, &ids).unwrap();
        SurfaceHeights::compose(&layout, &[], &ids).unwrap()
    }

    fn geometry(blend: i32) -> GroundGeometry {
        let vertex = |x, z| GroundVertex {
            offset_x: x,
            offset_z: z,
            main_color: 1234,
            texture: UNTEXTURED_MATERIAL_ID,
            texture_scale: 1024,
        };
        GroundGeometry {
            tiles: Box::new([BlendedGroundTile {
                x: 4,
                y: 5,
                shape: 0,
                rotation: 0,
                split_edges: [false; 4],
                vertices: Box::new([vertex(0, 0), vertex(512, 0), vertex(512, 512)]),
                blending_colors: Some(Box::new([blend; 3])),
                emitted_vertices: 3,
                has_shadows: false,
            }]),
            vertex_count: 3,
        }
    }

    fn lower(geometry: &GroundGeometry) -> Result<LoweredMesh, String> {
        let table = MaterialTable::decode(&[0, 0]).unwrap();
        let rgb = HslRgbLut::from_random_unit(0.730967787376657).unwrap();
        lower_plane(&surface(), None, geometry, 0, &rgb, &table)
    }

    #[test]
    fn lowers_one_untextured_tile() {
        let mesh = lower(&geometry(100)).unwrap();
        assert_eq!(mesh.report.material_count, 1);
        assert_eq!(mesh.report.untextured_material_count, 1);
        assert_eq!(mesh.report.vertex_count, 3);
        assert_eq!(mesh.report.index_count, 3);
        assert_eq!(mesh.materials[0].vertices.len(), 3 * VERTEX_STRIDE);
        assert_eq!(mesh.report.sha256.len(), 64);
    }

    #[test]
    fn signed_blend_colours_below_minus_one_are_masked_to_sixteen_bits() {
        let signed = lower(&geometry(-2)).unwrap();
        let masked = lower(&geometry(0xFFFE)).unwrap();
        assert_eq!(signed.report.sha256, masked.report.sha256);
        assert_eq!(signed.materials[0].vertices, masked.materials[0].vertices);
        assert_eq!(signed.materials[0].indices, masked.materials[0].indices);
        assert!(lower(&geometry(-1)).is_ok());
    }

    #[test]
    fn lit_normal_heights_add_underwater_only_on_plane_zero() {
        let surface = surface();
        let plain = lit_normal_heights(&surface, None, 0).unwrap();
        let same = lit_normal_heights(&surface, Some(&surface), 1).unwrap();
        assert_eq!(same, lit_normal_heights(&surface, None, 1).unwrap());
        let lit = lit_normal_heights(&surface, Some(&surface), 0).unwrap();
        assert_eq!(lit.len(), plain.len());
        for (a, b) in lit.iter().zip(&plain) {
            assert_eq!(*a, b.wrapping_add(*b));
        }
    }

    #[test]
    fn baked_shadows_change_the_lowering_and_are_reported() {
        let table = MaterialTable::decode(&[0, 0]).unwrap();
        let rgb = HslRgbLut::from_random_unit(0.730967787376657).unwrap();
        let surface = surface();
        let geometry = geometry(100);
        let size = surface.layout().map_size().tiles();
        let mut grid = ShadowGrid::new(size as i32);
        for x in 3..7 {
            for y in 4..8 {
                grid.set_tile_shadow(x, y, 50);
            }
        }
        let plain = lower_plane(&surface, None, &geometry, 0, &rgb, &table).unwrap();
        let shadowed =
            lower_plane_shadowed(&surface, None, &geometry, 0, &rgb, &table, Some(&grid)).unwrap();
        let empty = ShadowGrid::new(size as i32);
        let same =
            lower_plane_shadowed(&surface, None, &geometry, 0, &rgb, &table, Some(&empty)).unwrap();
        assert!(shadowed.report.shadows_baked);
        assert!(!plain.report.shadows_baked);
        assert_ne!(plain.report.sha256, shadowed.report.sha256);
        assert_eq!(plain.report.sha256, same.report.sha256);
        let wrong = ShadowGrid::new(size as i32 - 1);
        assert!(
            lower_plane_shadowed(&surface, None, &geometry, 0, &rgb, &table, Some(&wrong)).is_err()
        );
    }

    #[test]
    fn underwater_normals_are_reported() {
        let table = MaterialTable::decode(&[0, 0]).unwrap();
        let rgb = HslRgbLut::from_random_unit(0.730967787376657).unwrap();
        let surface = surface();
        let geometry = geometry(100);
        let plain = lower_plane(&surface, None, &geometry, 0, &rgb, &table).unwrap();
        let lit = lower_plane(&surface, Some(&surface), &geometry, 0, &rgb, &table).unwrap();
        assert!(!plain.report.normals_from_underwater);
        assert!(lit.report.normals_from_underwater);
        assert!(!lit.report.shadows_baked);
    }
}
