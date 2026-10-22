use openrs_scene::ground_geometry::{BlendedGroundTile, GroundGeometry, GroundVertex};
use openrs_scene::ground_lowering::{
    ground_material_uid, lower_ordinary_untextured_ground, lower_textured_ground,
    select_ordinary_ground, select_textured_ground, shade_ground_color, CompiledGround,
    CompiledGroundMaterial, CompiledTexturedGround, GroundLoweringError, GroundLoweringLimits,
    GroundMaterialShading, OrdinaryGroundInput, TexturedGroundInput, DEFAULT_SURFACE_WATER,
};
use openrs_scene::rgb::HslRgbLut;

const QUAD: [(i32, i32); 6] = [(0, 0), (512, 0), (512, 512), (0, 0), (512, 512), (0, 512)];

fn textured_tile(
    x: usize,
    z: usize,
    vertices: &[(i32, i32, i32, i32, i32, i32)],
) -> BlendedGroundTile {
    BlendedGroundTile {
        x,
        y: z,
        shape: 0,
        rotation: 0,
        split_edges: [false; 4],
        vertices: vertices
            .iter()
            .map(
                |&(offset_x, offset_z, main_color, _, texture, texture_scale)| GroundVertex {
                    offset_x,
                    offset_z,
                    main_color,
                    texture,
                    texture_scale,
                },
            )
            .collect(),
        blending_colors: Some(vertices.iter().map(|v| v.3).collect()),
        emitted_vertices: vertices.len(),
        has_shadows: true,
    }
}

fn quad(main: [i32; 2], blend: [i32; 2], texture: [i32; 2], scale: [i32; 2]) -> BlendedGroundTile {
    let vertices: Vec<_> = QUAD
        .iter()
        .enumerate()
        .map(|(i, &(ox, oz))| {
            (
                ox,
                oz,
                main[i / 3],
                blend[i / 3],
                texture[i / 3],
                scale[i / 3],
            )
        })
        .collect();
    textured_tile(0, 0, &vertices)
}

fn rgb_bytes(color: i32) -> [u8; 3] {
    [(color >> 16) as u8, (color >> 8) as u8, color as u8]
}

fn shading(material_id: i32, alpha: u8, brightness: u8, effect_id: i8) -> GroundMaterialShading {
    GroundMaterialShading {
        material_id,
        alpha,
        brightness,
        effect_id,
    }
}

struct TexturedInputs {
    inputs: Inputs,
    materials: Vec<GroundMaterialShading>,
}

impl TexturedInputs {
    fn new(width: usize, height: usize, tiles: Vec<BlendedGroundTile>) -> Self {
        Self {
            inputs: Inputs::new(width, height, tiles),
            materials: vec![
                shading(5, 255, 0, 0),
                shading(16383, 128, 64, 4),
                shading(0, 0, 255, 1),
            ],
        }
    }
    fn input(&self) -> TexturedGroundInput<'_> {
        let base = self.inputs.input();
        TexturedGroundInput {
            width: base.width,
            height: base.height,
            geometry: base.geometry,
            position_heights: base.position_heights,
            normal_heights: base.normal_heights,
            baked_shadows: base.baked_shadows,
            rgb: base.rgb,
            materials: &self.materials,
            tile_units: 512,
            surface_water: DEFAULT_SURFACE_WATER,
            underwater: false,
            secondary_ground_heights: false,
            hd_lighting: true,
        }
    }
    fn lower(&self) -> Result<CompiledTexturedGround, GroundLoweringError> {
        lower_textured_ground(self.input(), GroundLoweringLimits::default())
    }
}

fn tile(x: usize, z: usize, points: &[(i32, i32)]) -> BlendedGroundTile {
    BlendedGroundTile {
        x,
        y: z,
        shape: 0,
        rotation: 0,
        split_edges: [false; 4],
        vertices: points
            .iter()
            .map(|&(offset_x, offset_z)| GroundVertex {
                offset_x,
                offset_z,
                main_color: 12000,
                texture: -1,
                texture_scale: 128,
            })
            .collect(),
        blending_colors: None,
        emitted_vertices: points.len(),
        has_shadows: false,
    }
}

fn geometry(tiles: Vec<BlendedGroundTile>) -> GroundGeometry {
    GroundGeometry {
        vertex_count: tiles.iter().map(|t| t.vertices.len()).sum(),
        tiles: tiles.into_boxed_slice(),
    }
}

struct Inputs {
    width: usize,
    height: usize,
    geometry: GroundGeometry,
    heights: Vec<i32>,
    normals: Vec<i32>,
    shadows: Vec<u8>,
    rgb: HslRgbLut,
}

impl Inputs {
    fn new(width: usize, height: usize, tiles: Vec<BlendedGroundTile>) -> Self {
        Self {
            width,
            height,
            geometry: geometry(tiles),
            heights: vec![0; (width + 1) * (height + 1)],
            normals: vec![0; (width + 1) * (height + 1)],
            shadows: vec![0; (width + 1) * (height + 1)],
            rgb: HslRgbLut::from_random_unit(0.5).unwrap(),
        }
    }
    fn input(&self) -> OrdinaryGroundInput<'_> {
        OrdinaryGroundInput {
            width: self.width,
            height: self.height,
            geometry: &self.geometry,
            position_heights: &self.heights,
            normal_heights: &self.normals,
            baked_shadows: &self.shadows,
            rgb: &self.rgb,
            texture_scale: 128,
            tile_units: 512,
            surface_water: DEFAULT_SURFACE_WATER,
            underwater: false,
            secondary_ground_heights: false,
            hd_lighting: true,
        }
    }
    fn lower(&self) -> Result<CompiledGround, GroundLoweringError> {
        lower_ordinary_untextured_ground(self.input(), GroundLoweringLimits::default())
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn consumes_full_allocated_arrays_and_rejects_textured_suffix() {
    let mut t = tile(
        0,
        0,
        &[
            (0, 0),
            (512, 0),
            (512, 512),
            (128, 128),
            (256, 256),
            (384, 384),
        ],
    );
    t.emitted_vertices = 3;
    t.has_shadows = true;
    let mut data = Inputs::new(1, 1, vec![t]);
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.tile_indices()[0].as_deref().unwrap().len(), 6);
    assert_eq!(compiled.tile_masks(), [3]);
    assert_eq!(compiled.vertices().len(), 6);
    data.geometry.tiles[0].vertices[5].texture = 0;
    assert!(matches!(
        data.lower(),
        Err(GroundLoweringError::InvalidVertex {
            vertex: 5,
            field: "texture",
            ..
        })
    ));
    assert_eq!(compiled.vertices().len(), 6);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn whole_ground_domain_rejects_unsupported_context_and_materials() {
    let data = Inputs::new(1, 1, vec![tile(0, 0, &[(0, 0); 3])]);
    for case in 0..7 {
        let mut input = data.input();
        match case {
            0 => input.underwater = true,
            1 => input.secondary_ground_heights = true,
            2 => input.hd_lighting = false,
            3 => input.tile_units = 256,
            4 => input.surface_water[0] ^= 1,
            5 => input.texture_scale = 0,
            _ => input.texture_scale = -1,
        }
        assert!(matches!(
            lower_ordinary_untextured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::Unsupported(_))
        ));
    }
    for case in 0..5 {
        let mut geometry = data.geometry.clone();
        match case {
            0 => geometry.tiles[0].vertices[1].texture_scale = 256,
            1 => geometry.tiles[0].vertices[1].main_color = -1,
            2 => geometry.tiles[0].vertices[1].main_color = 65536,
            3 => geometry.tiles[0].blending_colors = Some(vec![1, -1, 1].into_boxed_slice()),
            _ => geometry.tiles[0].vertices[1].texture = 12,
        }
        let mut input = data.input();
        input.geometry = &geometry;
        assert!(matches!(
            lower_ordinary_untextured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::InvalidVertex { vertex: 1, .. })
        ));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn strict_lengths_dimensions_coordinates_duplicates_and_counts() {
    let data = Inputs::new(1, 1, vec![tile(0, 0, &[(0, 0); 3])]);
    for field in 0..3 {
        let mut input = data.input();
        match field {
            0 => input.position_heights = &[],
            1 => input.normal_heights = &[],
            _ => input.baked_shadows = &[],
        }
        assert!(matches!(
            lower_ordinary_untextured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::InputLength { .. })
        ));
    }
    for (width, height) in [(0, 1), (1, 0), (169, 1), (1, 169), (usize::MAX, usize::MAX)] {
        let mut input = data.input();
        input.width = width;
        input.height = height;
        assert!(matches!(
            lower_ordinary_untextured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::Dimensions { .. })
        ));
    }
    for case in 0..10 {
        let mut geometry = data.geometry.clone();
        match case {
            0 => geometry.vertex_count += 1,
            1 => geometry.tiles[0].x = 1,
            2 => geometry.tiles[0].y = 1,
            3 => geometry.tiles[0].vertices[0].offset_x = -1,
            4 => geometry.tiles[0].vertices[2].offset_z = 513,
            5 => geometry.tiles[0].emitted_vertices = 4,
            6 => geometry.tiles[0].blending_colors = Some(Box::new([])),
            7 => geometry.tiles[0].shape = 15,
            8 => geometry.tiles[0].rotation = 4,
            _ => {
                let t = geometry.tiles[0].clone();
                geometry.tiles = vec![t.clone(), t].into_boxed_slice();
                geometry.vertex_count = 6;
            }
        }
        let mut input = data.input();
        input.geometry = &geometry;
        assert!(
            lower_ordinary_untextured_ground(input, GroundLoweringLimits::default()).is_err(),
            "case {case}"
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn transactional_budgets_and_selection_bounds() {
    let data = Inputs::new(1, 1, vec![tile(0, 0, &[(0, 0), (256, 256), (512, 512)])]);
    for limits in [
        GroundLoweringLimits {
            max_input_vertices: 2,
            max_unique_vertices: 65536,
        },
        GroundLoweringLimits {
            max_input_vertices: 3,
            max_unique_vertices: 2,
        },
    ] {
        assert!(matches!(
            lower_ordinary_untextured_ground(data.input(), limits),
            Err(GroundLoweringError::Budget { .. })
        ));
    }
    let compiled = data.lower().unwrap();
    let saved = compiled.clone();
    assert!(matches!(
        select_ordinary_ground(&compiled, &[], usize::MAX),
        Err(GroundLoweringError::InputLength { .. })
    ));
    assert!(matches!(
        select_ordinary_ground(&compiled, &[true], 2),
        Err(GroundLoweringError::Budget {
            resource: "selected indices",
            ..
        })
    ));
    assert_eq!(
        select_ordinary_ground(&compiled, &[true], 3)
            .unwrap()
            .indices
            .len(),
        3
    );
    assert!(select_ordinary_ground(&compiled, &[false], 0)
        .unwrap()
        .indices
        .is_empty());
    assert_eq!(compiled, saved);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn canonical_span_keeps_unreferenced_interior_vertices() {
    let data = Inputs::new(
        2,
        1,
        vec![
            tile(1, 0, &[(0, 0), (0, 512), (512, 512)]),
            tile(0, 0, &[(512, 0), (0, 0), (512, 512)]),
        ],
    );
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.tile_indices()[0].as_deref().unwrap(), [0, 1, 2]);
    assert_eq!(compiled.tile_indices()[1].as_deref().unwrap(), [0, 2, 3]);
    let selected = select_ordinary_ground(&compiled, &[false, true], 3).unwrap();
    assert_eq!(selected.source_first_vertex, 0);
    assert_eq!(selected.indices.as_ref(), [0, 2, 3]);
    assert_eq!(selected.vertices.len(), 4);
    assert_eq!(selected.vertices[1].position, [0.0, 0.0, 0.0]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn quarter_alignment_gates_lookup_but_preserves_first_packed_key_insert() {
    let data = Inputs::new(
        1,
        1,
        vec![tile(0, 0, &[(129, 128), (128, 128), (129, 128)])],
    );
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.tile_indices()[0].as_deref().unwrap(), [0, 0, 1]);
    assert_eq!(compiled.vertices().len(), 2);
    assert_eq!(compiled.vertices()[0].position[0], 129.0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn border_height_zero_and_raw_interpolated_normals() {
    let mut data = Inputs::new(
        2,
        2,
        vec![
            tile(0, 0, &[(0, 0), (256, 256), (512, 512)]),
            tile(1, 1, &[(0, 0), (512, 0), (0, 512)]),
        ],
    );
    data.heights.fill(100);
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.vertices()[0].position, [0.0, 100.0, 0.0]);
    assert_eq!(compiled.vertices()[0].normal, [0.0; 3]);
    assert_eq!(compiled.vertices()[1].normal, [0.0, -0.25, 0.0]);
    assert_eq!(compiled.vertices()[2].normal, [0.0, -1.0, 0.0]);
    assert_eq!(compiled.vertices()[3].position[1], 0.0);
    assert_eq!(compiled.vertices()[4].position[1], 0.0);
    data.normals[5] = 60000;
    assert!(matches!(
        data.lower(),
        Err(GroundLoweringError::NonFiniteNormal { x: 1, z: 1 })
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn u16_boundary_accepts_65536_vertices_then_rejects_without_wrap() {
    let mut tiles = Vec::new();
    for n in 0..21846 {
        let mut t = tile(n % 168, n / 168, &[(0, 0); 3]);
        for (i, v) in t.vertices.iter_mut().enumerate() {
            v.main_color = if n == 21845 { 0 } else { i as i32 * 128 };
        }
        tiles.push(t);
    }
    let mut data = Inputs::new(168, 131, tiles);
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.vertices().len(), 65536);
    assert!(compiled
        .tile_indices()
        .iter()
        .flatten()
        .any(|indices| indices.contains(&65535)));
    data.geometry.tiles[21845].vertices[1].main_color = 128;
    assert!(matches!(
        data.lower(),
        Err(GroundLoweringError::Budget {
            resource: "unique vertices",
            required: 65537,
            limit: 65536
        })
    ));
    assert_eq!(compiled.vertices().len(), 65536);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn textured_materials_are_keyed_by_texture_and_scale_and_sorted_by_uid() {
    let mut vertices = Vec::new();
    for (i, &(ox, oz)) in [(0, 0), (512, 0), (512, 512)]
        .iter()
        .cycle()
        .take(18)
        .enumerate()
    {
        let (texture, scale) =
            [(5, 256), (-1, 0), (16383, 4), (5, 4), (0, 65535), (-1, 128)][i / 3];
        vertices.push((ox, oz, 12000, 12000, texture, scale));
    }
    let data = TexturedInputs::new(1, 1, vec![textured_tile(0, 0, &vertices)]);
    let compiled = data.lower().unwrap();
    let keys: Vec<_> = compiled
        .materials()
        .iter()
        .map(|m| (m.material_id(), m.texture_scale()))
        .collect();
    assert_eq!(
        keys,
        [(-1, 128), (-1, 0), (5, 4), (16383, 4), (5, 256), (0, 65535)]
    );
    let uids: Vec<i64> = compiled.materials().iter().map(|m| m.uid()).collect();
    assert_eq!(&uids[..2], [-1, -1]);
    assert!(uids.windows(2).all(|w| w[0] <= w[1]));
    for material in compiled.materials() {
        assert_eq!(
            material.uid(),
            ground_material_uid(
                material.material_id(),
                material.texture_scale(),
                DEFAULT_SURFACE_WATER
            )
        );
        assert_eq!(material.colors().len(), compiled.vertices().len());
        assert_eq!(material.tile_masks().len(), 1);
    }
    assert_eq!(ground_material_uid(-1, 0, DEFAULT_SURFACE_WATER), -1);
    assert_eq!(
        ground_material_uid(16383, 65535, DEFAULT_SURFACE_WATER),
        (127i64 << 48) | (40 << 42) | (1583160 << 28) | (65535 << 14) | 16383
    );
    assert_eq!(compiled.vertices().len(), 3);
    assert_eq!(compiled.tile_indices()[0].as_deref().unwrap().len(), 18);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn shade_ground_color_matches_contrast_effect_and_brightness_arithmetic() {
    assert_eq!(shade_ground_color(None, 0x123456, 64), 0x123456);
    assert_eq!(
        shade_ground_color(Some(shading(1, 0, 0, 0)), 0x123456, 64),
        0x123456
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 255, 0, 4)), 0x123456, 64),
        0x123456
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 255, 0, 0)), 0x123456, 127),
        -131587
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 255, 0, 0)), 0x123456, 200),
        -65794
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 128, 0, 0)), 0x123456, -5),
        0x91a2b
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 128, 0, 0)), 0x123456, 64),
        0x495a6b
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 255, 0, 0)), 0, 64),
        0x7f7f7f
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 0, 255, 0)), 0x123456, 64),
        0x2367ab
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 0, 1, 0)), 0x80ff40, 64),
        0x80ff40
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 128, 64, 0)), 0x123456, 64),
        0x5b7085
    );
    assert_eq!(
        shade_ground_color(Some(shading(1, 255, 255, 5)), 0, 64),
        0xfdfdfd
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn welded_owner_moves_to_lower_uid_and_face_d_takes_the_lowest_owner() {
    let data = TexturedInputs::new(
        1,
        1,
        vec![quad([12000; 2], [12000; 2], [5, 16383], [256, 4])],
    );
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.vertices().len(), 4);
    assert_eq!(
        compiled.tile_indices()[0].as_deref().unwrap(),
        [0, 1, 2, 0, 2, 3]
    );
    let [fine, coarse] = compiled.materials() else {
        panic!("two materials")
    };
    assert_eq!((fine.material_id(), fine.texture_scale()), (16383, 4));
    assert_eq!((coarse.material_id(), coarse.texture_scale()), (5, 256));
    assert_eq!(fine.tile_masks(), [3]);
    assert_eq!(coarse.tile_masks(), [1]);
    let alpha = |m: &CompiledGroundMaterial| m.colors().iter().map(|c| c[3]).collect::<Vec<_>>();
    assert_eq!(alpha(fine), [255, 255, 255, 255]);
    assert_eq!(alpha(coarse), [0, 255, 0, 0]);
    for (index, vertex) in compiled.vertices().iter().enumerate() {
        let [r, g, b, a] = vertex.rgba;
        assert_eq!(a, 255);
        let direct = (i32::from(r) << 16) | (i32::from(g) << 8) | i32::from(b);
        assert_ne!(direct, 0);
        for (material, shading) in [
            (fine, shading(16383, 128, 64, 4)),
            (coarse, shading(5, 255, 0, 0)),
        ] {
            let expected = shade_ground_color(Some(shading), direct, 74);
            assert_eq!(&material.colors()[index][..3], &rgb_bytes(expected));
            assert_ne!(expected, direct);
        }
    }
    let fine_mesh = select_textured_ground(&compiled, 0, &[true], 6).unwrap();
    assert_eq!(fine_mesh.indices.as_ref(), [0, 1, 2, 0, 2, 3]);
    assert_eq!(fine_mesh.material_id, 16383);
    assert_eq!(fine_mesh.texture_scale, 4);
    assert!(fine_mesh
        .vertices
        .iter()
        .zip(fine.colors())
        .all(|(v, c)| v.rgba == *c && v.texcoord[2] == 0.0));
    let coarse_mesh = select_textured_ground(&compiled, 1, &[true], 6).unwrap();
    assert_eq!(coarse_mesh.indices.as_ref(), [0, 1, 2]);
    assert_eq!(coarse_mesh.vertices.len(), 3);
    assert_eq!(coarse_mesh.source_first_vertex, 0);
    assert!(matches!(
        select_textured_ground(&compiled, 2, &[true], 6),
        Err(GroundLoweringError::MaterialIndex { index: 2, count: 2 })
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn colorless_vertices_own_nothing_and_colored_welds_onto_colorless_is_unsupported() {
    let data = TexturedInputs::new(1, 1, vec![quad([-1; 2], [-1; 2], [0, 5], [764; 2])]);
    let compiled = data.lower().unwrap();
    assert_eq!(compiled.vertices().len(), 4);
    for (index, material) in compiled.materials().iter().enumerate() {
        assert_eq!(material.tile_masks(), [0]);
        assert!(material.colors().iter().all(|c| c[3] == 0));
        assert!(select_textured_ground(&compiled, index, &[true], 6)
            .unwrap()
            .indices
            .is_empty());
    }
    assert!(compiled.vertices().iter().all(|v| v.rgba == [0, 0, 0, 255]));
    let brightened = &compiled.materials()[0].colors()[0];
    assert_eq!(compiled.materials()[0].material_id(), 0);
    assert_eq!(&brightened[..3], &[0, 0, 0]);
    let contrasted = &compiled.materials()[1].colors()[0];
    assert_eq!(compiled.materials()[1].material_id(), 5);
    assert_eq!(
        &contrasted[..3],
        &rgb_bytes(shade_ground_color(Some(shading(5, 255, 0, 0)), 0, 74))
    );
    assert_ne!(&contrasted[..3], &[0, 0, 0]);
    let colorless_first =
        TexturedInputs::new(1, 1, vec![quad([-1, 65535], [-1, 65535], [0; 2], [0; 2])]);
    assert_eq!(
        colorless_first.lower(),
        Err(GroundLoweringError::Unsupported(
            "colored vertex welded onto a colorless vertex"
        ))
    );
    let colored_first =
        TexturedInputs::new(1, 1, vec![quad([65535, -1], [65535, 7], [5, -1], [0; 2])]);
    let compiled = colored_first.lower().unwrap();
    assert_eq!(compiled.vertices().len(), 4);
    assert_eq!(
        compiled.tile_indices()[0].as_deref().unwrap(),
        [0, 1, 2, 0, 2, 3]
    );
    assert_eq!(compiled.materials()[1].material_id(), 5);
    assert_eq!(compiled.materials()[1].tile_masks(), [3]);
    let owned: Vec<u8> = compiled.materials()[1]
        .colors()
        .iter()
        .map(|c| c[3])
        .collect();
    assert_eq!(owned, [255, 255, 255, 0]);
    assert_eq!(compiled.materials()[0].material_id(), -1);
    assert_eq!(compiled.materials()[0].tile_masks(), [0]);
    assert!(compiled.materials()[0].colors().iter().all(|c| c[3] == 0));
    let mut separated =
        TexturedInputs::new(1, 1, vec![quad([-1, 65535], [-1, 12000], [0; 2], [0; 2])]);
    assert!(separated.lower().is_ok());
    separated.inputs.geometry.tiles[0].blending_colors = None;
    assert!(separated.lower().is_err());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn textured_validation_rejects_bad_materials_vertices_and_budgets() {
    let mut data = TexturedInputs::new(
        1,
        1,
        vec![quad([12000; 2], [-1, 12000], [5, -1], [65535, 0])],
    );
    assert!(data.lower().is_ok());
    for case in 0..4 {
        let mut materials = data.materials.clone();
        match case {
            0 => materials.push(shading(5, 0, 0, 0)),
            1 => materials.push(shading(16384, 0, 0, 0)),
            2 => materials.push(shading(-1, 0, 0, 0)),
            _ => materials.clear(),
        }
        let materials_input = TexturedInputs {
            inputs: Inputs::new(
                1,
                1,
                vec![quad([12000; 2], [12000; 2], [5, -1], [65535, 0])],
            ),
            materials,
        };
        if case == 3 {
            assert!(matches!(
                materials_input.lower(),
                Err(GroundLoweringError::InvalidVertex {
                    vertex: 0,
                    field: "material shading",
                    ..
                })
            ));
        } else {
            assert!(matches!(
                materials_input.lower(),
                Err(GroundLoweringError::InvalidMaterial { .. })
            ));
        }
    }
    for (case, field) in [
        (0, "texture"),
        (1, "material scale"),
        (2, "material scale"),
        (3, "main color"),
        (4, "blend color"),
        (5, "material shading"),
    ] {
        let mut geometry = data.inputs.geometry.clone();
        let vertex = &mut geometry.tiles[0].vertices[1];
        match case {
            0 => vertex.texture = 16384,
            1 => vertex.texture_scale = 65536,
            2 => vertex.texture_scale = -1,
            3 => vertex.main_color = -2,
            4 => {
                geometry.tiles[0].blending_colors =
                    Some(vec![1, 65536, 1, 1, 1, 1].into_boxed_slice())
            }
            _ => vertex.texture = 7,
        }
        let mut input = data.input();
        input.geometry = &geometry;
        assert_eq!(
            lower_textured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::InvalidVertex {
                x: 0,
                z: 0,
                vertex: 1,
                field
            })
        );
    }
    for case in 0..5 {
        let mut input = data.input();
        match case {
            0 => input.underwater = true,
            1 => input.secondary_ground_heights = true,
            2 => input.hd_lighting = false,
            3 => input.tile_units = 256,
            _ => input.surface_water[2] = 126,
        }
        assert!(matches!(
            lower_textured_ground(input, GroundLoweringLimits::default()),
            Err(GroundLoweringError::Unsupported(_))
        ));
    }
    assert_eq!(
        lower_textured_ground(
            data.input(),
            GroundLoweringLimits {
                max_input_vertices: 65536,
                max_unique_vertices: 3
            }
        ),
        Err(GroundLoweringError::Budget {
            resource: "unique vertices",
            required: 4,
            limit: 3
        })
    );
    data.inputs.geometry.vertex_count = 5;
    assert!(matches!(
        data.lower(),
        Err(GroundLoweringError::VertexCount {
            declared: 5,
            actual: 6
        })
    ));
}
