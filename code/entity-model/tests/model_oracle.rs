#![cfg(not(target_arch = "wasm32"))]

mod common;

use common::{fixture, short_digest, Cache, Canon};
use openrs_entity::mesh::Mesh;
use openrs_entity::model::{BillboardType, Built, Env, Model};
use openrs_entity::reader::{ModelError, R};
use openrs_scene::material::MaterialTable;
use openrs_scene::rgb::HslRgbLut;
use std::collections::{HashMap, HashSet};

const FIXTURE: &[u8] = include_bytes!("fixtures/model-oracle.tsv.gz");
const FIXTURE_SHA256: &str = include_str!("fixtures/model-oracle.sha256");
const SHADE_FIXTURE: &[u8] = include_bytes!("fixtures/shade-oracle.tsv.gz");
const SHADE_FIXTURE_SHA256: &str = include_str!("fixtures/shade-oracle.sha256");
const MATERIALS: &[u8] = include_bytes!("fixtures/model-oracle-materials.bin");
const MODEL_INDEX: u32 = 7;
const RANDOM_UNIT_SEED_ZERO: f64 = 0.730967787376657;

fn int(text: &str) -> i32 {
    text.parse().unwrap()
}

fn mesh(spec: &str, models: &HashMap<i32, Vec<u8>>) -> R<Mesh> {
    let ids: Vec<i32> = spec.split(',').map(int).collect();
    let decode = |id: i32| Mesh::decode(models.get(&id).ok_or(ModelError::Null)?);
    if ids.len() == 1 {
        return decode(ids[0]);
    }
    let parts = ids.iter().map(|id| decode(*id)).collect::<R<Vec<_>>>()?;
    let refs: Vec<Option<&Mesh>> = parts.iter().map(Some).collect();
    Mesh::merge(&refs, refs.len())
}

fn groups(c: &mut Canon, values: &Option<Vec<Vec<i32>>>) {
    match values {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for group in list {
                c.ints(&Some(group.clone()));
            }
        }
    }
}

fn float_bits(values: &[f32]) -> Option<Vec<i32>> {
    Some(
        values
            .iter()
            .map(|v| {
                if v.is_nan() {
                    0x7fc0_0000
                } else {
                    v.to_bits() as i32
                }
            })
            .collect(),
    )
}

fn state(m: &Model) -> String {
    let r = m.released;
    let mut c = Canon::default();
    c.int(m.num_vertices);
    c.int(m.max_depth);
    c.int(m.num_normals as i32);
    c.int(m.num_faces as i32);
    c.int(m.rendered_face_count as i32);
    c.ints(&Some(m.mesh_face_indices.clone()));
    c.ints(&(!r.vertex_index).then(|| m.triangles.clone()));
    c.ints(&m.vertices_x);
    c.ints(&m.vertices_y);
    c.ints(&m.vertices_z);
    c.shorts(&(!r.normals).then(|| m.normals_x.clone()));
    c.shorts(&(!r.normals).then(|| m.normals_y.clone()));
    c.shorts(&(!r.normals).then(|| m.normals_z.clone()));
    c.i8s(&(!r.normals).then(|| m.magnitudes.clone()));
    c.ints(&(!r.uv).then(|| float_bits(&m.texture_u).unwrap()));
    c.ints(&(!r.uv).then(|| float_bits(&m.texture_v).unwrap()));
    c.shorts(&(!r.color).then(|| m.faces_color.clone()));
    c.i8s(&(!r.alpha).then(|| m.faces_alpha.clone()));
    c.shorts(&(!r.faces).then(|| m.faces_a.clone()));
    c.shorts(&(!r.faces).then(|| m.faces_b.clone()));
    c.shorts(&(!r.faces).then(|| m.faces_c.clone()));
    c.shorts(&(!r.material).then(|| m.faces_material.clone()));
    c.shorts(&m.faces_submeshes);
    c.shorts(&(!r.vertex_index).then(|| m.vertex_normal_indices.clone()));
    c.ints(&(!r.batch_offsets).then(|| m.material_batch_offsets.clone()));
    c.int(i32::from(m.ambient));
    c.int(i32::from(m.contrast));
    c.byte(u8::from(m.has_alpha_transition));
    c.byte(u8::from(m.texture_animating));
    groups(&mut c, &m.vertex_groups);
    groups(&mut c, &m.billboard_groups);
    groups(&mut c, &m.face_groups);
    short_digest(&c.bytes)
}

fn built(result: R<Option<Built>>) -> String {
    match result {
        Err(_) => "error".to_string(),
        Ok(None) => "null".to_string(),
        Ok(Some(b)) => {
            let mut c = Canon::default();
            c.int(b.vertex_count as i32);
            c.int(b.face_count as i32);
            c.int(b.vertex_words.len() as i32);
            for word in &b.vertex_words {
                c.int(if f32::from_bits(*word as u32).is_nan() {
                    0x7fc0_0000
                } else {
                    *word
                });
            }
            c.int(b.batches.len() as i32);
            for batch in &b.batches {
                c.int(batch.material_id);
                c.int(batch.indices.len() as i32);
                for index in &batch.indices {
                    c.int(*index);
                }
            }
            short_digest(&c.bytes)
        }
    }
}

fn apply(model: &mut Model, env: &Env, op: &str) -> R<()> {
    let p: Vec<&str> = op.split(':').collect();
    match p[0] {
        "r" => model.recolor(int(p[1]) as i16, int(p[2]) as i16)?,
        "m" => model.rematerial(env, int(p[1]) as i16, int(p[2]) as i16)?,
        "t" => model.tint(int(p[1]), int(p[2]), int(p[3]), int(p[4]))?,
        "a" => model.set_ambient(int(p[1])),
        "c" => model.set_contrast(int(p[1])),
        "e" => model.apply_environment_alpha(int(p[1]) as i8, None)?,
        "f" => {
            let k = int(p[2]);
            let transparency: Vec<i8> = (0..model.num_faces as i32)
                .map(|i| i.wrapping_mul(k).wrapping_add(7) as i8)
                .collect();
            model.apply_environment_alpha(int(p[1]) as i8, Some(&transparency))?;
        }
        "s" => model.apply_shading(int(p[1])),
        other => panic!("unknown op {other}"),
    }
    Ok(())
}

fn compute(
    table: &MaterialTable,
    rgb: &[u32],
    models: &HashMap<i32, Vec<u8>>,
    f: &[&str],
) -> Option<String> {
    let mesh = mesh(f[1], models).ok()?;
    let mode = int(f[6]);
    let material = int(f[7]);
    let mut billboards = HashMap::new();
    if mode != 0 {
        if let Some(list) = &mesh.billboards {
            for b in list {
                let has_uid = mode == 1 || (mode == 2 && b.id % 2 == 0);
                billboards.insert(
                    b.id,
                    BillboardType {
                        has_uid,
                        material_id: material,
                    },
                );
            }
        }
    }
    let env = Env {
        materials: table,
        hsl_to_rgb: rgb,
        billboards: &billboards,
    };
    let mut model = Model::new(&env, &mesh, int(f[2]), int(f[3]), int(f[4]), int(f[5])).ok()?;
    if f[8] != "-" {
        for op in f[8].split(';') {
            apply(&mut model, &env, op).ok()?;
        }
    }
    let s = state(&model);
    let b = built(model.build_model_cache(&env));
    Some(format!("{s}/{b}"))
}

#[test]
fn model_state_and_vertex_cache_match_the_kotlin_oracle() {
    let Some(mut cache) = Cache::open() else {
        return;
    };
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let mut ids = HashSet::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        for id in f[1].split(',') {
            ids.insert(int(id));
        }
    }
    let mut models = HashMap::new();
    for id in ids {
        let data = cache.file(MODEL_INDEX, id as u32, 0).expect("model");
        cache.drop_archive(MODEL_INDEX, id as u32);
        models.insert(id, data);
    }
    let table = MaterialTable::decode(MATERIALS).unwrap();
    let lut = HslRgbLut::from_random_unit(RANDOM_UNIT_SEED_ZERO).unwrap();
    let rgb = lut.hsl_to_rgb();

    let mut total = 0usize;
    let mut errors = 0usize;
    let mut nulls = 0usize;
    let mut full = 0usize;
    let mut mismatches: Vec<String> = Vec::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let expected = match f[f.len() - 1] {
            "error" => None,
            other => Some(other.to_string()),
        };
        let actual = compute(&table, rgb, &models, &f[..f.len() - 1]);
        if actual != expected {
            mismatches.push(format!("{line} -> {actual:?}"));
        }
        total += 1;
        match &expected {
            None => errors += 1,
            Some(e) if e.ends_with("/null") => nulls += 1,
            Some(_) => full += 1,
        }
    }
    eprintln!(
        "model oracle: {total} cases, {errors} errors, {nulls} null builds, {full} full builds"
    );
    assert!(
        mismatches.is_empty(),
        "{} mismatches, first {:#?}",
        mismatches.len(),
        &mismatches[..mismatches.len().min(8)]
    );
    assert!(total >= 9000 && full >= 5000);
}

fn merged_state(m: &Model) -> String {
    let mut c = Canon::default();
    match &m.merged_normals {
        None => c.byte(0),
        Some(merged) => {
            c.byte(1);
            c.shorts(&Some(merged.normals_x.clone()));
            c.shorts(&Some(merged.normals_y.clone()));
            c.shorts(&Some(merged.normals_z.clone()));
            c.i8s(&Some(merged.magnitudes.clone()));
        }
    }
    short_digest(&c.bytes)
}

fn compute_shaded(
    table: &MaterialTable,
    rgb: &[u32],
    models: &HashMap<i32, Vec<u8>>,
    f: &[&str],
) -> Option<String> {
    let billboards = HashMap::new();
    let env = Env {
        materials: table,
        hsl_to_rgb: rgb,
        billboards: &billboards,
    };
    let mut pair = Vec::new();
    for i in 0..2 {
        let mesh = mesh(f[1 + i], models).ok()?;
        pair.push(Model::new(&env, &mesh, int(f[3 + i]), int(f[5]), int(f[6]), int(f[7])).ok()?);
    }
    let mut b = pair.pop()?;
    let mut a = pair.pop()?;
    if f[8] != "-" {
        for op in f[8].split(';') {
            let p: Vec<&str> = op.split(':').collect();
            match p[0] {
                "x" => a
                    .apply_shading_model(&mut b, int(p[1]), int(p[2]), int(p[3]))
                    .ok()?,
                "y" => b
                    .apply_shading_model(&mut a, int(p[1]), int(p[2]), int(p[3]))
                    .ok()?,
                "s" => a.apply_shading(int(p[1])),
                "u" => b.apply_shading(int(p[1])),
                other => panic!("unknown op {other}"),
            }
        }
    }
    let part = |m: &Model| {
        format!(
            "{}/{}/{}",
            state(m),
            built(m.build_model_cache(&env)),
            merged_state(m)
        )
    };
    Some(format!("{}|{}", part(&a), part(&b)))
}

#[test]
fn cross_model_shading_matches_the_kotlin_oracle() {
    let Some(mut cache) = Cache::open() else {
        return;
    };
    let text = fixture(SHADE_FIXTURE, SHADE_FIXTURE_SHA256);
    let mut ids = HashSet::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        for spec in [f[1], f[2]] {
            for id in spec.split(',') {
                ids.insert(int(id));
            }
        }
    }
    let mut models = HashMap::new();
    for id in ids {
        let data = cache.file(MODEL_INDEX, id as u32, 0).expect("model");
        cache.drop_archive(MODEL_INDEX, id as u32);
        models.insert(id, data);
    }
    let table = MaterialTable::decode(MATERIALS).unwrap();
    let lut = HslRgbLut::from_random_unit(RANDOM_UNIT_SEED_ZERO).unwrap();
    let rgb = lut.hsl_to_rgb();
    let mut mismatches = Vec::new();
    let mut merged = 0usize;
    let mut errors = 0usize;
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let expected = match f[f.len() - 1] {
            "error" => None,
            other => Some(other.to_string()),
        };
        let actual = compute_shaded(&table, rgb, &models, &f[..f.len() - 1]);
        if actual != expected {
            mismatches.push(format!("{line} -> {actual:?}"));
        }
        match &expected {
            None => errors += 1,
            Some(e) if e.split('|').any(|p| !p.ends_with(&merged_state_none())) => merged += 1,
            Some(_) => {}
        }
    }
    eprintln!(
        "shade oracle: {} cases, {errors} errors, {merged} merged",
        text.lines().count()
    );
    assert!(
        mismatches.is_empty(),
        "{} mismatches, first {:#?}",
        mismatches.len(),
        &mismatches[..mismatches.len().min(8)]
    );
    assert!(merged >= 300, "{merged}");
}

fn merged_state_none() -> String {
    let mut c = Canon::default();
    c.byte(0);
    short_digest(&c.bytes)
}
