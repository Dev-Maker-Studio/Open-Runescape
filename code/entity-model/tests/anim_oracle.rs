#![cfg(not(target_arch = "wasm32"))]

mod common;

use common::{fixture, short_digest, Cache, Canon};
use openrs_entity::anim::{
    AnimBase, AnimFrame, BaseStore, FrameSet, FrameSets, Param, SeqType, Sequence, SequenceMask,
};
use openrs_entity::reader::R;
use std::collections::HashMap;

const FIXTURE: &[u8] = include_bytes!("fixtures/anim-oracle.tsv.gz");
const FIXTURE_SHA256: &str = include_str!("fixtures/anim-oracle.sha256");
const FRAME_INDEX: u32 = 0;
const BASE_INDEX: u32 = 1;
const SEQ_INDEX: u32 = 20;

struct Sets(HashMap<i32, FrameSet>);

impl FrameSets for Sets {
    fn frameset(&self, group: i32) -> Option<&FrameSet> {
        self.0.get(&group)
    }
}

fn iv(c: &mut Canon, values: &[i32]) {
    c.int(values.len() as i32);
    for value in values {
        c.int(*value);
    }
}

fn bools(c: &mut Canon, values: &Option<Vec<bool>>) {
    match values {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for value in list {
                c.byte(u8::from(*value));
            }
        }
    }
}

fn canonical_base(b: &AnimBase) -> Vec<u8> {
    let mut c = Canon::default();
    c.int(b.id);
    c.int(b.num_transformation);
    iv(&mut c, &b.transformation_type);
    c.int(b.labels.len() as i32);
    for label in &b.labels {
        iv(&mut c, label);
    }
    c.byte(1);
    c.int(b.shadowed.len() as i32);
    for flag in &b.shadowed {
        c.byte(u8::from(*flag));
    }
    iv(&mut c, &b.submeshes);
    c.bytes
}

fn canonical_frame(c: &mut Canon, base_id: i32, f: &AnimFrame) {
    c.int(base_id);
    c.int(f.num_transformations);
    c.byte(u8::from(f.modifies_alpha));
    c.byte(u8::from(f.modifies_color));
    c.byte(u8::from(f.modifies_billboard));
    c.shorts(&f.base_indices);
    c.shorts(&f.transformation_x);
    c.shorts(&f.transformation_y);
    c.shorts(&f.transformation_z);
    c.shorts(&f.labels);
    c.i8s(&f.tweening_properties);
}

fn canonical_seq(s: &SeqType) -> Vec<u8> {
    let mut c = Canon::default();
    c.int(s.id);
    c.ints(&s.durations);
    c.int(s.replay_interval);
    bools(&mut c, &s.interleave_order);
    c.ints(&s.frames);
    c.int(s.priority);
    c.int(s.replacement_shield);
    c.int(s.replacement_weapon);
    c.int(s.replay_count);
    c.int(s.interrupt_type);
    c.int(s.move_type);
    c.int(s.loop_type);
    c.ints(&s.interface_frames);
    match &s.frame_sounds {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for sound in list {
                c.ints(sound);
            }
        }
    }
    c.byte(u8::from(s.lights));
    c.byte(u8::from(s.tweened));
    c.byte(u8::from(s.vorbis));
    c.ints(&s.frame_sound_volume);
    c.ints(&s.sound_max_delay);
    c.ints(&s.sound_min_delay);
    let mut keys: Vec<i32> = s.parameters.iter().map(|(key, _)| *key).collect();
    keys.sort_unstable();
    keys.dedup();
    c.int(keys.len() as i32);
    for key in keys {
        c.int(key);
        let (_, value) = s.parameters.iter().find(|(k, _)| *k == key).unwrap();
        match value {
            Param::Int(v) => {
                c.byte(0);
                c.int(*v);
                c.int(s.cs2_integer(key, -77).unwrap_or(-78));
            }
            Param::Text(text) => {
                c.byte(1);
                c.int(text.len() as i32);
                for byte in text {
                    c.byte(*byte);
                }
                c.int(-78);
            }
        }
    }
    c.bytes
}

fn mask(c: &mut Canon, m: &SequenceMask) {
    c.int(m.function_mask);
    c.byte(u8::from(m.function_mask_calculated));
    c.int(m.current_frame);
    c.int(m.current_frameset.unwrap_or(-1));
    c.int(m.next_frame);
    c.int(m.next_frameset.unwrap_or(-1));
}

fn state(c: &mut Canon, s: &Sequence) {
    c.int(s.seq_id);
    c.byte(u8::from(s.reset_animation));
    c.byte(u8::from(s.tweening));
    c.int(s.speed);
    c.int(s.current_loop_count);
    c.int(s.current_frame_id);
    c.int(s.next_frame_id);
    c.int(s.current_frame_duration);
    c.int(s.replay_mode);
    c.byte(u8::from(s.hard_reset));
}

fn usable(seqs: &HashMap<i32, SeqType>, sets: &Sets, id: i32) -> bool {
    let Some(ty) = seqs.get(&id) else {
        return false;
    };
    [&ty.frames, &ty.interface_frames]
        .into_iter()
        .flatten()
        .all(|list| list.iter().all(|frame| sets.0.contains_key(&(frame >> 16))))
}

fn trace(tween: bool, steps: &str, seqs: &HashMap<i32, SeqType>, sets: &Sets) -> Option<String> {
    let parts: Vec<Vec<&str>> = steps.split(';').map(|s| s.split(',').collect()).collect();
    for p in &parts {
        if p[0] == "a" && p[1] != "-1" && !usable(seqs, sets, p[1].parse().unwrap()) {
            return None;
        }
    }
    let lookup = |id: i32| seqs.get(&id).cloned();
    let mut c = Canon::default();
    let mut seq = Sequence::new(tween);
    let mut current: Option<SeqType> = None;
    for p in &parts {
        match p[0] {
            "a" => {
                let id: i32 = p[1].parse().unwrap();
                let old = seq.seq_id;
                let result = seq.animate(
                    id,
                    p[2].parse().unwrap(),
                    p[3].parse().unwrap(),
                    p[4] == "1",
                    &lookup,
                );
                if seq.seq_id == -1 {
                    current = None;
                } else if seq.seq_id != old {
                    current = lookup(seq.seq_id);
                }
                c.byte(if result.is_ok() { 1 } else { 9 });
            }
            "p" => {
                let duration: i32 = p[1].parse().unwrap();
                let result: R<bool> = match &current {
                    None => Ok(false),
                    Some(ty) => seq.progress_cycle(ty, duration),
                };
                c.byte(match result {
                    Ok(true) => 2,
                    Ok(false) => 3,
                    Err(_) => 9,
                });
            }
            _ => {
                let calculated: R<bool> = match &current {
                    None => Ok(false),
                    Some(ty) => seq.is_mask_calculated(sets, ty),
                };
                match calculated {
                    Err(_) => c.byte(9),
                    Ok(flag) => {
                        c.byte(if flag { 4 } else { 5 });
                        let function: R<i32> = match &current {
                            None => Ok(0),
                            Some(ty) => seq.function_mask(sets, ty),
                        };
                        match function {
                            Err(_) => c.byte(9),
                            Ok(value) => {
                                c.int(value);
                                mask(&mut c, &seq.current_mask);
                                if let Some(next) = &seq.next_mask {
                                    mask(&mut c, next);
                                }
                            }
                        }
                    }
                }
            }
        }
        state(&mut c, &seq);
        if p[0] != "m" {
            let calculated: R<bool> = match &current {
                None => Ok(false),
                Some(ty) => seq.is_mask_calculated(sets, ty),
            };
            c.byte(match calculated {
                Ok(true) => 1,
                Ok(false) => 0,
                Err(_) => 9,
            });
        }
    }
    Some(short_digest(&c.bytes))
}

fn file_base_id(data: &[u8]) -> i32 {
    i32::from(data[1]) << 8 | i32::from(data[2])
}

#[test]
fn bases_framesets_seqtypes_and_sequence_traces_match_the_kotlin_oracle() {
    let Some(mut cache) = Cache::open() else {
        return;
    };
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let mut base_data: HashMap<i32, Vec<u8>> = HashMap::new();
    for line in text.lines().filter(|l| l.starts_with("B\t")) {
        let id: u32 = line.split('\t').nth(1).unwrap().parse().unwrap();
        if let Some(data) = cache.file(BASE_INDEX, id, 0) {
            base_data.insert(id as i32, data);
        }
        cache.drop_archive(BASE_INDEX, id);
    }
    let shared = base_data.clone();
    let mut store = BaseStore::new(Box::new(move |id| shared.get(&id).cloned()));
    let mut sets = Sets(HashMap::new());
    let mut seqs: HashMap<i32, SeqType> = HashMap::new();
    let mut compared = [0usize; 4];
    let mut errors = [0usize; 4];
    let mut skipped = 0usize;
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let (kind, actual): (usize, Option<String>) = match f[0] {
            "B" => {
                let id: i32 = f[1].parse().unwrap();
                let actual = base_data
                    .get(&id)
                    .and_then(|data| AnimBase::decode(id, data).ok())
                    .map(|b| short_digest(&canonical_base(&b)));
                (0, actual)
            }
            "F" => {
                let group: u32 = f[1].parse().unwrap();
                let mut files = Vec::new();
                for file in cache.file_ids(FRAME_INDEX, group) {
                    if let Some(data) = cache.file(FRAME_INDEX, group, file) {
                        files.push((file as i32, data));
                    }
                }
                cache.drop_archive(FRAME_INDEX, group);
                let actual = FrameSet::decode(group as i32, &files, &mut store)
                    .ok()
                    .map(|set| {
                        let mut c = Canon::default();
                        c.int(set.frames.len() as i32);
                        for (index, frame) in set.frames.iter().enumerate() {
                            match frame {
                                None => c.byte(0),
                                Some(frame) => {
                                    c.byte(1);
                                    let data = &files
                                        .iter()
                                        .find(|(id, _)| *id as usize == index)
                                        .unwrap()
                                        .1;
                                    canonical_frame(&mut c, file_base_id(data), frame);
                                }
                            }
                        }
                        sets.0.insert(group as i32, set);
                        short_digest(&c.bytes)
                    });
                (1, actual)
            }
            "S" => {
                let id: i32 = f[1].parse().unwrap();
                let data = cache.file(SEQ_INDEX, (id >> 7) as u32, (id & 127) as u32);
                let actual = SeqType::decode_entry(id, data.as_deref()).ok().map(|seq| {
                    let digest = short_digest(&canonical_seq(&seq));
                    seqs.insert(id, seq);
                    digest
                });
                (2, actual)
            }
            _ => {
                let actual = trace(f[1] == "1", f[2], &seqs, &sets);
                if actual.is_none() {
                    skipped += 1;
                }
                (3, actual)
            }
        };
        let expected = match f[f.len() - 1] {
            "error" | "skip" => None,
            digest => Some(digest.to_string()),
        };
        assert_eq!(actual, expected, "case {line}");
        compared[kind] += 1;
        if expected.is_none() {
            errors[kind] += 1;
        }
    }
    eprintln!(
        "anim oracle: bases {} frame sets {} seq types {} traces {} (errors {errors:?}, skipped traces {skipped})",
        compared[0], compared[1], compared[2], compared[3]
    );
    assert!(compared[0] > 3000 && compared[1] > 4000 && compared[2] > 17000 && compared[3] >= 8000);
}

#[test]
fn anim_decoders_reject_truncated_data() {
    assert!(AnimBase::decode(0, &[]).is_err());
    assert!(AnimBase::decode(0, &[3, 1]).is_err());
    let base = AnimBase::decode(0, &[0]).unwrap();
    let frame = AnimFrame::decode(&[0, 0, 0, 0], &base);
    assert_eq!(frame.num_transformations, 0);
    assert!(SeqType::decode_entry(0, Some(&[1, 0])).is_err());
    assert!(SeqType::decode_entry(0, Some(&[19, 0, 0])).is_err());
}
