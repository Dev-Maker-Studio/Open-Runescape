#![cfg(not(target_arch = "wasm32"))]

mod common;

use common::{fixture, short_digest, Cache, Canon};
use openrs_entity::anim::{AnimBase, AnimFrame, BaseStore, FrameSet};
use openrs_entity::mesh::Mesh;
use openrs_entity::pose::Pose;
use openrs_entity::reader::{ModelError, R};
use std::collections::{HashMap, HashSet};

const FIXTURE: &[u8] = include_bytes!("fixtures/pose-oracle.tsv.gz");
const FIXTURE_SHA256: &str = include_str!("fixtures/pose-oracle.sha256");
const MODEL_INDEX: u32 = 7;
const FRAME_INDEX: u32 = 0;
const BASE_INDEX: u32 = 1;

struct Sets {
    sets: HashMap<i32, FrameSet>,
    bases: HashMap<(i32, i32), i32>,
}

struct Frame<'a> {
    base: i32,
    frame: &'a AnimFrame,
}

impl Sets {
    fn frame(&self, group: i32, file: i32) -> R<Frame<'_>> {
        let set = self.sets.get(&group).ok_or(ModelError::Null)?;
        let frame = set.frame(file)?;
        let base = *self.bases.get(&(group, file)).ok_or(ModelError::Null)?;
        Ok(Frame { base, frame })
    }

    fn next(&self, group: i32, file: i32, animating_base: i32) -> R<Option<Frame<'_>>> {
        if group == -1 {
            return Ok(None);
        }
        let found = self.frame(group, file)?;
        Ok(if found.base == animating_base {
            Some(found)
        } else {
            None
        })
    }
}

fn mesh(spec: &str, models: &HashMap<i32, Vec<u8>>) -> R<Mesh> {
    let ids: Vec<i32> = spec.split(',').map(|v| v.parse().unwrap()).collect();
    let decode = |id: i32| Mesh::decode(models.get(&id).ok_or(ModelError::Null)?);
    if ids.len() == 1 {
        return decode(ids[0]);
    }
    let parts = ids.iter().map(|id| decode(*id)).collect::<R<Vec<_>>>()?;
    let refs: Vec<Option<&Mesh>> = parts.iter().map(Some).collect();
    Mesh::merge(&refs, refs.len())
}

fn mask(hex: &str) -> Option<Vec<bool>> {
    if hex == "-" {
        return None;
    }
    let raw: Vec<u8> = (0..hex.len() / 2)
        .map(|i| u8::from_str_radix(&hex[i * 2..i * 2 + 2], 16).unwrap())
        .collect();
    Some(
        (0..raw.len() * 8)
            .map(|i| (raw[i >> 3] >> (i & 7)) & 1 != 0)
            .collect(),
    )
}

fn digest(pose: &Pose) -> String {
    let mut c = Canon::default();
    c.int(pose.num_vertices);
    c.ints(&Some(pose.vertices_x.clone()));
    c.ints(&Some(pose.vertices_y.clone()));
    c.ints(&Some(pose.vertices_z.clone()));
    c.i8s(&pose.faces_alpha);
    c.shorts(&pose.faces_color);
    short_digest(&c.bytes)
}

fn int(text: &str) -> i32 {
    text.parse().unwrap()
}

fn compute(
    sets: &Sets,
    bases: &HashMap<i32, AnimBase>,
    models: &HashMap<i32, Vec<u8>>,
    f: &[&str],
) -> Option<String> {
    let mut pose = Pose::from_mesh(&mesh(f[1], models).ok()?).ok()?;
    let animatable = pose.vertex_groups.is_some();
    let (cycle, length) = (int(f[6]), int(f[7]));
    if animatable {
        let first = sets.frame(int(f[2]), int(f[3])).ok()?;
        let first_base = bases.get(&first.base)?;
        let next = sets.next(int(f[4]), int(f[5]), first.base).ok()?;
        let next_frame = next.as_ref().map(|n| n.frame);
        if f[0] == "O" {
            pose.animate_model_frame(
                first_base,
                first.frame,
                next_frame,
                cycle,
                length,
                int(f[8]),
            )
            .ok()?;
        } else {
            let labels = mask(f[14]);
            match labels {
                None => {
                    pose.animate_model_frame(first_base, first.frame, next_frame, cycle, length, 0)
                        .ok()?;
                }
                Some(labels) => {
                    let idle = sets.frame(int(f[8]), int(f[9])).ok()?;
                    let idle_base = bases.get(&idle.base)?;
                    let idle_next = sets.next(int(f[10]), int(f[11]), first.base).ok()?;
                    pose.double_animation(
                        first_base,
                        first.frame,
                        next_frame,
                        cycle,
                        length,
                        Some((
                            idle_base,
                            idle.frame,
                            idle_next.as_ref().map(|n| n.frame),
                            int(f[12]),
                            int(f[13]),
                        )),
                        Some(&labels),
                    )
                    .ok()?;
                }
            }
        }
    }
    Some(digest(&pose))
}

#[test]
fn posed_vertices_match_the_kotlin_oracle() {
    let Some(mut cache) = Cache::open() else {
        return;
    };
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let mut model_ids = HashSet::new();
    let mut groups = HashSet::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        for id in f[1].split(',') {
            model_ids.insert(int(id));
        }
        let mut positions = vec![2, 4];
        if f[0] == "D" {
            positions.extend([8, 10]);
        }
        for at in positions {
            if f[at] != "-1" {
                groups.insert(int(f[at]));
            }
        }
    }
    let mut models = HashMap::new();
    for id in model_ids {
        let data = cache.file(MODEL_INDEX, id as u32, 0).expect("model");
        cache.drop_archive(MODEL_INDEX, id as u32);
        models.insert(id, data);
    }
    let mut base_data: HashMap<i32, Vec<u8>> = HashMap::new();
    let mut files: HashMap<i32, Vec<(i32, Vec<u8>)>> = HashMap::new();
    let mut base_of: HashMap<(i32, i32), i32> = HashMap::new();
    for group in &groups {
        let mut list = Vec::new();
        for file in cache.file_ids(FRAME_INDEX, *group as u32) {
            if let Some(data) = cache.file(FRAME_INDEX, *group as u32, file) {
                if data.len() >= 3 {
                    let base = (i32::from(data[1]) << 8) | i32::from(data[2]);
                    base_of.insert((*group, file as i32), base);
                    if let std::collections::hash_map::Entry::Vacant(slot) = base_data.entry(base) {
                        if let Some(bytes) = cache.file(BASE_INDEX, base as u32, 0) {
                            slot.insert(bytes);
                        }
                        cache.drop_archive(BASE_INDEX, base as u32);
                    }
                }
                list.push((file as i32, data));
            }
        }
        cache.drop_archive(FRAME_INDEX, *group as u32);
        files.insert(*group, list);
    }
    let shared = base_data.clone();
    let mut store = BaseStore::new(Box::new(move |id| shared.get(&id).cloned()));
    let mut sets = Sets {
        sets: HashMap::new(),
        bases: base_of,
    };
    for (group, list) in &files {
        sets.sets.insert(
            *group,
            FrameSet::decode(*group, list, &mut store).expect("frame set"),
        );
    }
    let bases: HashMap<i32, AnimBase> = base_data
        .iter()
        .map(|(id, data)| (*id, AnimBase::decode(*id, data).expect("base")))
        .collect();

    let mut compared = [0usize; 2];
    let mut errors = [0usize; 2];
    let mut changed = 0usize;
    let mut mismatches: Vec<String> = Vec::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let expected = match f[f.len() - 1] {
            "error" => None,
            other => Some(other.to_string()),
        };
        let actual = compute(&sets, &bases, &models, &f[..f.len() - 1]);
        if actual != expected {
            mismatches.push(format!("{line} -> {actual:?}"));
        }
        let kind = usize::from(f[0] == "D");
        compared[kind] += 1;
        if expected.is_none() {
            errors[kind] += 1;
        } else if let Some(plain) = mesh(f[1], &models)
            .ok()
            .and_then(|m| Pose::from_mesh(&m).ok())
        {
            if digest(&plain) != actual.clone().unwrap() {
                changed += 1;
            }
        }
    }
    eprintln!("pose oracle: single {} double {} (expected errors {errors:?}), posed differently from rest {changed}", compared[0], compared[1]);
    assert!(
        mismatches.is_empty(),
        "{} mismatches, first {:#?}",
        mismatches.len(),
        &mismatches[..mismatches.len().min(8)]
    );
    assert!(compared[0] >= 6000 && compared[1] >= 3000 && changed > 3000);
}
