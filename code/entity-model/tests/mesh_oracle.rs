#![cfg(not(target_arch = "wasm32"))]

mod common;

use common::{fixture, short_digest, Cache, Canon};
use openrs_entity::mesh::Mesh;
use openrs_entity::reader::R;
use openrs_entity::trig;
use std::collections::HashMap;

const FIXTURE: &[u8] = include_bytes!("fixtures/mesh-oracle.tsv.gz");
const FIXTURE_SHA256: &str = include_str!("fixtures/mesh-oracle.sha256");
const MODEL_INDEX: u32 = 7;

fn canonical(m: &Mesh) -> Vec<u8> {
    let mut c = Canon::default();
    c.int(m.version);
    c.int(m.num_vertices);
    c.int(m.max_depth);
    c.int(m.num_faces);
    c.byte(m.priority as u8);
    c.int(m.num_textures);
    c.i8s(&m.textures_mapping);
    c.ints(&m.vertices_x);
    c.ints(&m.vertices_y);
    c.ints(&m.vertices_z);
    c.shorts(&m.faces_a);
    c.shorts(&m.faces_b);
    c.shorts(&m.faces_c);
    c.ints(&m.vertex_groups);
    c.i8s(&m.face_render_type);
    c.i8s(&m.faces_layer);
    c.i8s(&m.faces_alpha);
    c.ints(&m.face_groups);
    c.shorts(&m.faces_material);
    c.shorts(&m.faces_color);
    c.i8s(&m.faces_texture);
    c.shorts(&m.texture_face_x);
    c.shorts(&m.texture_face_y);
    c.shorts(&m.texture_face_z);
    c.ints(&m.textures_scale_x);
    c.ints(&m.textures_scale_y);
    c.ints(&m.textures_scale_z);
    c.i8s(&m.textures_rotation);
    c.i8s(&m.textures_direction);
    c.ints(&m.textures_speed);
    c.ints(&m.textures_utrans);
    c.ints(&m.textures_vtrans);
    match &m.particle_emitters {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for e in list {
                c.int(e.producer_id);
                c.int(e.face_a);
                c.int(e.face_b);
                c.int(e.face_c);
                c.byte(e.priority as u8);
            }
        }
    }
    match &m.particle_effectors {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for e in list {
                c.int(e.effect_id);
                c.int(e.applies_on_vertex);
            }
        }
    }
    match &m.billboards {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for b in list {
                c.int(b.id);
                c.int(b.applies_on_face);
                c.int(b.depth);
                c.int(b.distance);
            }
        }
    }
    c.shorts(&m.vertices_submeshes);
    c.shorts(&m.faces_submeshes);
    c.groups(&m.create_vertex_groups(true));
    c.groups(&m.create_vertex_groups(false));
    c.groups(&m.create_face_groups());
    c.groups(&m.create_billboard_groups());
    c.bytes
}

fn number(text: &str) -> i32 {
    text.parse().unwrap()
}

fn apply(mesh: &mut Mesh, ops: &str) -> R<()> {
    if ops == "-" {
        return Ok(());
    }
    for op in ops.split('|') {
        let p: Vec<&str> = op.split(':').collect();
        match p[0] {
            "R" => mesh.recolor(number(p[1]) as i16, number(p[2]) as i16)?,
            "RF" => {
                let src = match &mesh.faces_color {
                    Some(colors) if mesh.num_faces > 0 => colors[0],
                    _ => 0,
                };
                mesh.recolor(src, number(p[1]) as i16)?;
            }
            "T" => mesh.retexture(number(p[1]) as i16, number(p[2]) as i16)?,
            "TF" => {
                let src = match &mesh.faces_material {
                    Some(materials) if mesh.num_faces > 0 => materials[0],
                    _ => 0,
                };
                mesh.retexture(src, number(p[1]) as i16)?;
            }
            "ROT" => mesh.rotate_without_normals(number(p[1]), number(p[2]), number(p[3]))?,
            "UP" => mesh.upscale()?,
            "TR" => mesh.translate(number(p[1]), number(p[2]), number(p[3]))?,
            other => panic!("unknown op {other}"),
        }
    }
    Ok(())
}

fn run_case(kind: &str, ids: &str, ops: &str, models: &mut ModelStore) -> Option<String> {
    let ids: Vec<u32> = ids.split(',').map(|id| id.parse().unwrap()).collect();
    let result = (|| -> R<Mesh> {
        let mut mesh = if kind == "M" {
            let mut parts = Vec::new();
            for id in &ids {
                parts.push(Mesh::decode(models.get(*id))?);
            }
            let refs: Vec<Option<&Mesh>> = parts.iter().map(Some).collect();
            Mesh::merge(&refs, refs.len())?
        } else {
            Mesh::decode(models.get(ids[0]))?
        };
        apply(&mut mesh, ops)?;
        Ok(mesh)
    })();
    result.ok().map(|mesh| short_digest(&canonical(&mesh)))
}

struct ModelStore {
    cache: Cache,
    loaded: HashMap<u32, Vec<u8>>,
}

impl ModelStore {
    fn get(&mut self, id: u32) -> &[u8] {
        if !self.loaded.contains_key(&id) {
            let data = self.cache.file(MODEL_INDEX, id, 0).expect("model");
            self.cache.drop_archive(MODEL_INDEX, id);
            self.loaded.insert(id, data);
        }
        &self.loaded[&id]
    }
}

#[test]
fn trig_tables_match_the_kotlin_math_utils() {
    let mut c = Canon::default();
    for value in trig::sine() {
        c.int(*value);
    }
    for value in trig::cosine() {
        c.int(*value);
    }
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let line = text.lines().find(|l| l.starts_with("TRIG\t")).unwrap();
    let expected = line.rsplit('\t').next().unwrap();
    assert_eq!(short_digest(&c.bytes), expected);
}

#[test]
fn mesh_decode_merge_and_transforms_match_the_kotlin_oracle() {
    let Some(cache) = Cache::open() else {
        return;
    };
    let mut models = ModelStore {
        cache,
        loaded: HashMap::new(),
    };
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let mut compared = [0usize; 3];
    let mut errors = 0usize;
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        if f[0] == "TRIG" {
            continue;
        }
        let actual = run_case(f[0], f[1], f[2], &mut models);
        let expected = if f[3] == "error" {
            None
        } else {
            Some(f[3].to_string())
        };
        assert_eq!(actual, expected, "case {line}");
        if expected.is_none() {
            errors += 1;
        }
        compared[match f[0] {
            "D" => 0,
            "M" => 1,
            _ => 2,
        }] += 1;
        if f[0] != "D" {
            models.loaded.retain(|_, data| data.len() < 1 << 20);
        }
    }
    eprintln!(
        "mesh oracle: decode {} merge {} transform {} (error cases {errors})",
        compared[0], compared[1], compared[2]
    );
    assert!(compared[0] > 20_000 && compared[1] >= 4000 && compared[2] >= 4000);
}

#[test]
fn mesh_decode_rejects_truncated_and_empty_data() {
    assert!(Mesh::decode(&[]).is_err());
    assert!(Mesh::decode(&[0xff]).is_err());
    assert!(Mesh::decode(&[0xff, 0xff]).is_err());
    assert!(Mesh::decode(&[0u8; 10]).is_err());
}
