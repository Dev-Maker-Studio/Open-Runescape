use openrs_scene::material::MaterialTable;
use openrs_scene::mesh::{Mesh, MeshError};
use openrs_scene::model_build::{build, BuildInput};
use openrs_scene::rgb::HslRgbLut;
use serde_json::Value;
use std::collections::{HashMap, HashSet};

const FIXTURE: &str = include_str!("fixtures/mesh-oracle.json");
const MODELS: &[u8] = include_bytes!("fixtures/mesh-oracle-models.bin");
const MATERIALS: &[u8] = include_bytes!("fixtures/mesh-oracle-materials.bin");
const BASIS: u64 = 0xcbf2_9ce4_8422_2325;
const PRIME: u64 = 0x0000_0100_0000_01b3;

fn hash_int(mut hash: u64, value: i32) -> u64 {
    for byte in value.to_le_bytes() {
        hash = (hash ^ u64::from(byte)).wrapping_mul(PRIME);
    }
    hash
}

fn hash_bytes(mut hash: u64, bytes: &[u8]) -> u64 {
    for byte in bytes {
        hash = (hash ^ u64::from(*byte)).wrapping_mul(PRIME);
    }
    hash
}

fn hash_array<T: Copy + Into<i32>>(hash: u64, array: &Option<Vec<T>>) -> u64 {
    match array {
        None => hash_int(hash, -1),
        Some(values) => {
            let mut hash = hash_int(hash, values.len() as i32);
            for value in values {
                hash = hash_int(hash, (*value).into());
            }
            hash
        }
    }
}

fn hash_slice(hash: u64, values: &[i32]) -> u64 {
    let mut hash = hash_int(hash, values.len() as i32);
    for value in values {
        hash = hash_int(hash, *value);
    }
    hash
}

fn mesh_hash(mesh: &Mesh) -> u64 {
    let mut h = BASIS;
    for value in [
        mesh.version,
        mesh.num_vertices,
        mesh.max_depth,
        mesh.num_faces,
        i32::from(mesh.priority),
        mesh.num_textures,
    ] {
        h = hash_int(h, value);
    }
    h = hash_array(h, &mesh.textures_mapping);
    h = hash_array(h, &mesh.vertices_x);
    h = hash_array(h, &mesh.vertices_y);
    h = hash_array(h, &mesh.vertices_z);
    h = hash_array(h, &mesh.faces_a);
    h = hash_array(h, &mesh.faces_b);
    h = hash_array(h, &mesh.faces_c);
    h = hash_array(h, &mesh.vertex_groups);
    h = hash_array(h, &mesh.face_render_type);
    h = hash_array(h, &mesh.faces_layer);
    h = hash_array(h, &mesh.faces_alpha);
    h = hash_array(h, &mesh.face_groups);
    h = hash_array(h, &mesh.faces_material);
    h = hash_array(h, &mesh.faces_color);
    h = hash_array(h, &mesh.faces_texture);
    h = hash_array(h, &mesh.texture_face_x);
    h = hash_array(h, &mesh.texture_face_y);
    h = hash_array(h, &mesh.texture_face_z);
    h = hash_array(h, &mesh.textures_scale_x);
    h = hash_array(h, &mesh.textures_scale_y);
    h = hash_array(h, &mesh.textures_scale_z);
    h = hash_array(h, &mesh.textures_rotation);
    h = hash_array(h, &mesh.textures_direction);
    h = hash_array(h, &mesh.textures_speed);
    h = hash_array(h, &mesh.textures_utrans);
    h = hash_array(h, &mesh.textures_vtrans);
    match &mesh.billboards {
        None => h = hash_int(h, -1),
        Some(list) => {
            h = hash_int(h, list.len() as i32);
            for b in list {
                for value in [b.billboard_id, b.applies_on_face, b.depth, b.distance] {
                    h = hash_int(h, value);
                }
            }
        }
    }
    h = hash_int(
        h,
        mesh.particle_emitters
            .as_ref()
            .map_or(-1, |list| list.len() as i32),
    );
    hash_int(
        h,
        mesh.particle_effectors
            .as_ref()
            .map_or(-1, |list| list.len() as i32),
    )
}

fn crc32(bytes: &[u8]) -> u32 {
    let mut crc = 0xFFFF_FFFFu32;
    for byte in bytes {
        crc ^= u32::from(*byte);
        for _ in 0..8 {
            crc = if crc & 1 != 0 {
                (crc >> 1) ^ 0xEDB8_8320
            } else {
                crc >> 1
            };
        }
    }
    !crc
}

fn models() -> HashMap<i32, &'static [u8]> {
    let count = i32::from_be_bytes(MODELS[0..4].try_into().unwrap());
    let mut at = 4;
    let mut out = HashMap::new();
    for _ in 0..count {
        let id = i32::from_be_bytes(MODELS[at..at + 4].try_into().unwrap());
        let len = i32::from_be_bytes(MODELS[at + 4..at + 8].try_into().unwrap()) as usize;
        out.insert(id, &MODELS[at + 8..at + 8 + len]);
        at += 8 + len;
    }
    assert_eq!(at, MODELS.len());
    out
}

fn assemble(ids: &[i32], models: &HashMap<i32, &[u8]>) -> Result<Mesh, MeshError> {
    let mut parts = Vec::new();
    for id in ids {
        let mut mesh = Mesh::from_bytes(models[id])?;
        if mesh.version < 13 {
            mesh.upscale()?;
        }
        parts.push(mesh);
    }
    if parts.len() == 1 {
        Ok(parts.remove(0))
    } else {
        let refs: Vec<Option<&Mesh>> = parts.iter().map(Some).collect();
        Mesh::combine(&refs)
    }
}

fn int(value: &Value, key: &str) -> i64 {
    value[key].as_i64().unwrap()
}

fn same(case: &Value, key: &str, actual: i64) {
    assert_eq!(int(case, key), actual, "{} {}", case["name"], key);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn mesh_and_model_build_match_the_native_classes() {
    let fixture: Value = serde_json::from_str(FIXTURE).unwrap();
    let rgb = HslRgbLut::from_random_unit(fixture["randomUnit"].as_f64().unwrap()).unwrap();
    let table = MaterialTable::decode(MATERIALS).unwrap();
    let models = models();
    let cases = fixture["cases"].as_array().unwrap();
    let mut tally = [0usize; 3];
    for case in cases {
        let ids: Vec<i32> = case["ids"]
            .as_array()
            .unwrap()
            .iter()
            .map(|v| v.as_i64().unwrap() as i32)
            .collect();
        let outcome = case["outcome"].as_str().unwrap();
        let mesh = match assemble(&ids, &models) {
            Ok(mesh) => mesh,
            Err(_) => {
                assert_eq!(outcome, "error", "{}", case["name"]);
                assert_eq!(case["stage"], "mesh", "{}", case["name"]);
                tally[0] += 1;
                continue;
            }
        };
        assert_ne!(case["stage"], "mesh", "{}", case["name"]);
        assert_eq!(
            int(case, "meshHash"),
            mesh_hash(&mesh) as i64,
            "{} mesh hash",
            case["name"]
        );
        let uid_mode = int(case, "uidMode");
        let known: HashSet<i32> = mesh
            .billboards
            .iter()
            .flatten()
            .map(|b| b.billboard_id)
            .collect();
        let has_uid =
            move |id: i32| known.contains(&id) && (uid_mode == 1 || (uid_mode == 2 && id % 2 == 0));
        let input = BuildInput {
            ambient: int(case, "ambient") as i32,
            materials: if int(case, "materials") == 1 {
                Some(&table)
            } else {
                None
            },
            strip_detail_layers: int(case, "strip") == 1,
            contrast: int(case, "contrast") as i32,
            hsl_to_rgb: rgb.hsl_to_rgb(),
            billboard_has_uid: &has_uid,
        };
        let built = match build(&mesh, &input) {
            Ok(Some(built)) => built,
            Ok(None) => {
                assert_eq!(outcome, "null", "{}", case["name"]);
                tally[1] += 1;
                continue;
            }
            Err(_) => {
                assert_eq!(outcome, "error", "{}", case["name"]);
                assert_eq!(case["stage"], "build", "{}", case["name"]);
                tally[0] += 1;
                continue;
            }
        };
        assert_eq!(outcome, "ok", "{}", case["name"]);
        tally[2] += 1;
        same(case, "id", built.id);
        same(case, "faceCount", i64::from(built.face_count));
        same(case, "vertexCount", i64::from(built.vertex_count));
        same(case, "vertexBytes", built.vertex_data.len() as i64);
        same(
            case,
            "vertexFnv",
            hash_bytes(BASIS, &built.vertex_data) as i64,
        );
        same(case, "vertexCrc", i64::from(crc32(&built.vertex_data)));
        let batches = case["batches"].as_array().unwrap();
        assert_eq!(batches.len(), built.batches.len(), "{}", case["name"]);
        for (expected, batch) in batches.iter().zip(&built.batches) {
            assert_eq!(
                int(expected, "materialId"),
                i64::from(batch.material_id),
                "{}",
                case["name"]
            );
            assert_eq!(
                int(expected, "count"),
                batch.indices.len() as i64,
                "{}",
                case["name"]
            );
            let indices: Vec<i32> = batch.indices.iter().map(|v| *v as i32).collect();
            assert_eq!(
                int(expected, "hash"),
                hash_slice(BASIS, &indices) as i64,
                "{} batch hash",
                case["name"]
            );
        }
        same(
            case,
            "skipBillboardHidden",
            i64::from(built.skip_billboard_hidden),
        );
        same(case, "skipRenderType", i64::from(built.skip_render_type));
        same(
            case,
            "skipInvalidVertex",
            i64::from(built.skip_invalid_vertex),
        );
        same(case, "skipTransparent", i64::from(built.skip_transparent));
        same(case, "sourceFaceCount", i64::from(built.source_face_count));
        same(case, "billboardsSeen", i64::from(built.billboards_seen));
        same(
            case,
            "billboardsWithUid",
            i64::from(built.billboards_with_uid),
        );
        same(
            case,
            "billboardsOutOfRange",
            i64::from(built.billboards_out_of_range),
        );
        same(
            case,
            "skipMaterialFiltered",
            i64::from(built.skip_material_filtered),
        );
        same(
            case,
            "acceptedFacesLength",
            built.accepted_faces.len() as i64,
        );
        same(
            case,
            "acceptedFaces",
            hash_slice(BASIS, &built.accepted_faces) as i64,
        );
        same(
            case,
            "acceptedMaterialIds",
            hash_slice(BASIS, &built.accepted_material_ids) as i64,
        );
    }
    assert_eq!(cases.len(), 3304);
    assert_eq!(tally, [218, 16, 3070]);
}
