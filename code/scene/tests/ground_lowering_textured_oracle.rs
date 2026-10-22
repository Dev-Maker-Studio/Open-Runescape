use openrs_scene::ground_geometry::{BlendedGroundTile, GroundGeometry, GroundVertex};
use openrs_scene::ground_lowering::{
    ground_material_uid, lower_textured_ground, select_textured_ground, GroundLoweringLimits,
    GroundMaterialShading, OrdinaryGroundVertex, TexturedGroundInput, DEFAULT_SURFACE_WATER,
};
use openrs_scene::rgb::HslRgbLut;
use serde_json::Value;

fn numbers(value: &Value) -> Vec<i32> {
    value
        .as_array()
        .unwrap()
        .iter()
        .map(|v| v.as_i64().unwrap() as i32)
        .collect()
}

fn hex(value: &Value) -> Vec<u8> {
    value
        .as_str()
        .unwrap()
        .as_bytes()
        .chunks_exact(2)
        .map(|b| {
            let digit = |c: u8| match c {
                b'0'..=b'9' => c - b'0',
                b'a'..=b'f' => c - b'a' + 10,
                _ => panic!("invalid hex"),
            };
            digit(b[0]) * 16 + digit(b[1])
        })
        .collect()
}

fn encode(vertices: &[OrdinaryGroundVertex]) -> Vec<u8> {
    let mut bytes = Vec::new();
    for v in vertices {
        for f in v.position {
            bytes.extend(f.to_le_bytes());
        }
        bytes.extend(v.rgba);
        for f in v.texcoord {
            bytes.extend(f.to_le_bytes());
        }
        for f in v.normal {
            bytes.extend(f.to_le_bytes());
        }
    }
    bytes
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unchanged_groundgl_textured_materials_colors_masks_and_order() {
    let fixture: Value =
        serde_json::from_str(include_str!("fixtures/ground-lowering-textured.json")).unwrap();
    assert_eq!(
        fixture["jarSha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    assert_eq!(
        fixture["nativeLibrarySha256"],
        "92f32235d2a67e7debedd89fbccd2ac0e937302eaa7e0732fe0ba93072a08a74"
    );
    assert_eq!(fixture["jarUnchanged"], true);
    assert_eq!(fixture["nativeLibraryUnchanged"], true);
    assert_eq!(fixture["actualGroundConstructor"], true);
    assert_eq!(fixture["actualResetLight"], true);
    assert_eq!(fixture["actualGroundMaterialShading"], true);
    assert_eq!(fixture["glDrawExecuted"], false);
    let rgb = HslRgbLut::from_random_unit(fixture["randomUnit"].as_f64().unwrap()).unwrap();
    let cases = fixture["cases"].as_array().unwrap();
    assert_eq!(cases.len(), 381);
    let mut allocated_total = 0;
    let mut unique_total = 0;
    let mut material_total = 0;
    let mut selection_total = 0;
    let mut textured_vertices = 0;
    let mut colorless_vertices = 0;
    for case in cases {
        let name = case["name"].as_str().unwrap();
        let materials: Vec<GroundMaterialShading> = case["materials"]
            .as_array()
            .unwrap()
            .iter()
            .map(|m| GroundMaterialShading {
                material_id: m["id"].as_i64().unwrap() as i32,
                alpha: m["alpha"].as_i64().unwrap() as u8,
                brightness: m["brightness"].as_i64().unwrap() as u8,
                effect_id: m["effect"].as_i64().unwrap() as i8,
            })
            .collect();
        let mut tiles = Vec::new();
        for tile in case["tiles"].as_array().unwrap() {
            let mut vertices = Vec::new();
            let mut blend = Vec::new();
            for v in tile["vertices"].as_array().unwrap() {
                let v = numbers(v);
                textured_vertices += usize::from(v[4] >= 0);
                colorless_vertices += usize::from(v[2] == -1);
                vertices.push(GroundVertex {
                    offset_x: v[0],
                    offset_z: v[1],
                    main_color: v[2],
                    texture: v[4],
                    texture_scale: v[5],
                });
                blend.push(v[3]);
            }
            tiles.push(BlendedGroundTile {
                x: tile["x"].as_u64().unwrap() as usize,
                y: tile["z"].as_u64().unwrap() as usize,
                shape: tile["shape"].as_u64().unwrap_or(0) as u8,
                rotation: tile["rotation"].as_u64().unwrap_or(0) as u8,
                split_edges: [false; 4],
                emitted_vertices: tile["emittedVertices"].as_u64().unwrap() as usize,
                has_shadows: tile["hasShadows"].as_bool().unwrap(),
                vertices: vertices.into_boxed_slice(),
                blending_colors: if tile["hasBlendingColors"].as_bool().unwrap_or(true) {
                    Some(blend.into_boxed_slice())
                } else {
                    None
                },
            });
        }
        let vertex_count = tiles.iter().map(|t| t.vertices.len()).sum();
        let geometry = GroundGeometry {
            tiles: tiles.into_boxed_slice(),
            vertex_count,
        };
        let position = numbers(&case["positionHeights"]);
        let normal = numbers(&case["normalHeights"]);
        let shadow: Vec<u8> = numbers(&case["bakedShadows"])
            .into_iter()
            .map(|v| v as u8)
            .collect();
        let compiled = lower_textured_ground(
            TexturedGroundInput {
                width: case["width"].as_u64().unwrap() as usize,
                height: case["height"].as_u64().unwrap() as usize,
                geometry: &geometry,
                position_heights: &position,
                normal_heights: &normal,
                baked_shadows: &shadow,
                rgb: &rgb,
                materials: &materials,
                tile_units: 512,
                surface_water: DEFAULT_SURFACE_WATER,
                underwater: false,
                secondary_ground_heights: false,
                hd_lighting: true,
            },
            GroundLoweringLimits::default(),
        )
        .unwrap_or_else(|e| panic!("{name}: {e}"));
        let expected = &case["compiled"];
        assert_eq!(
            vertex_count,
            expected["allocatedVertices"].as_u64().unwrap() as usize,
            "{name}"
        );
        assert_eq!(
            compiled.vertices().len(),
            expected["vertexCount"].as_u64().unwrap() as usize,
            "{name}"
        );
        let mut native = Vec::new();
        for v in compiled.vertices() {
            for f in v.position {
                native.extend(f.to_le_bytes());
            }
            native.extend(v.rgba);
            for f in &v.texcoord[..2] {
                native.extend(f.to_le_bytes());
            }
            assert_eq!(v.texcoord[2], 0.0, "{name}");
            for f in v.normal {
                native.extend(f.to_le_bytes());
            }
        }
        assert_eq!(
            native,
            hex(&expected["vertexBytes"]),
            "shared stream {name}"
        );
        let expected_materials = expected["materials"].as_array().unwrap();
        assert_eq!(
            compiled.materials().len(),
            expected["materialCount"].as_u64().unwrap() as usize,
            "{name}"
        );
        assert_eq!(
            compiled.materials().len(),
            expected_materials.len(),
            "{name}"
        );
        for (position, (material, expected)) in compiled
            .materials()
            .iter()
            .zip(expected_materials)
            .enumerate()
        {
            let material_id = expected["materialId"].as_i64().unwrap() as i32;
            let scale = expected["textureScale"].as_i64().unwrap() as i32;
            assert_eq!(
                material.material_id(),
                material_id,
                "material order {name} {position}"
            );
            assert_eq!(material.texture_scale(), scale, "{name} {position}");
            assert_eq!(numbers(&expected["water"]), DEFAULT_SURFACE_WATER, "{name}");
            assert_eq!(
                material.uid(),
                expected["pointer"].as_i64().unwrap(),
                "uid {name} {position}"
            );
            assert_eq!(
                material.uid(),
                ground_material_uid(material_id, scale, DEFAULT_SURFACE_WATER),
                "{name}"
            );
            let colors: Vec<u8> = material.colors().iter().flatten().copied().collect();
            assert_eq!(
                colors,
                hex(&expected["materialBytes"]),
                "material color {name} {material_id} {scale}"
            );
            assert_eq!(
                material.tile_masks(),
                numbers(&expected["masks"])
                    .iter()
                    .map(|&v| v as u32)
                    .collect::<Vec<_>>(),
                "masks {name} {material_id} {scale}"
            );
        }
        for (tile, expected) in compiled
            .tile_indices()
            .iter()
            .zip(expected["indices"].as_array().unwrap())
        {
            if expected.is_null() {
                assert!(tile.is_none(), "{name}");
            } else {
                assert_eq!(
                    tile.as_ref().unwrap().as_ref(),
                    numbers(expected)
                        .iter()
                        .map(|&v| v as u16)
                        .collect::<Vec<_>>(),
                    "tile indices {name}"
                );
            }
        }
        for selection in case["selections"].as_array().unwrap() {
            let material = selection["material"].as_u64().unwrap() as usize;
            let visible: Vec<bool> = selection["visibleTiles"]
                .as_array()
                .unwrap()
                .iter()
                .map(|v| v.as_bool().unwrap())
                .collect();
            let selected =
                select_textured_ground(&compiled, material, &visible, vertex_count).unwrap();
            assert_eq!(
                selected.material_id,
                compiled.materials()[material].material_id(),
                "{name}"
            );
            assert_eq!(
                selected.texture_scale,
                compiled.materials()[material].texture_scale(),
                "{name}"
            );
            assert_eq!(
                selected.source_first_vertex,
                selection["sourceFirstVertex"].as_u64().unwrap() as u32,
                "{name}"
            );
            assert_eq!(
                selected.indices.as_ref(),
                numbers(&selection["indices"])
                    .iter()
                    .map(|&v| v as u32)
                    .collect::<Vec<_>>(),
                "selected order {name} {material}"
            );
            assert_eq!(
                encode(&selected.vertices),
                hex(&selection["vertexBytes"]),
                "canonical span {name} {material}"
            );
            selection_total += 1;
        }
        allocated_total += vertex_count;
        unique_total += compiled.vertices().len();
        material_total += compiled.materials().len();
    }
    assert_eq!(allocated_total, 23499);
    assert_eq!(unique_total, 10821);
    assert_eq!(material_total, 875);
    assert_eq!(selection_total, 2422);
    assert!(textured_vertices > 15000);
    assert!(colorless_vertices > 500);
}
