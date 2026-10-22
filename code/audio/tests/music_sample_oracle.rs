mod common;

use std::collections::HashMap;
use std::fmt::Write;

use common::{gunzip, short_digest};
use openrs_audio::music_sample::{MusicSample, SampleDecoder};
use openrs_audio::R;

const INPUT: &[u8] = include_bytes!("fixtures/msample-input.bin.gz");
const INPUT_SHA256: &str = include_str!("fixtures/msample-input.sha256");
const CASES: &[u8] = include_bytes!("fixtures/msample-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/msample-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/msample-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/msample-oracle.sha256");

const ITERATION_CAP: usize = 100_000;
const PROBE_MIN_BYTES: usize = 2000;

fn be(data: &[u8], at: &mut usize) -> i32 {
    let value = i32::from_be_bytes(data[*at..*at + 4].try_into().unwrap());
    *at += 4;
    value
}

pub fn blobs() -> Vec<(i32, Vec<u8>)> {
    let data = gunzip(INPUT, INPUT_SHA256);
    let mut at = 0;
    let count = be(&data, &mut at);
    (0..count)
        .map(|_| {
            let id = be(&data, &mut at);
            let len = be(&data, &mut at) as usize;
            let blob = data[at..at + len].to_vec();
            at += len;
            (id, blob)
        })
        .collect()
}

pub fn mutate(data: &[u8], kind: i32, pos: usize, value: i32) -> Vec<u8> {
    match kind {
        1 => data[..pos.min(data.len())].to_vec(),
        2 => {
            let mut copy = data.to_vec();
            if pos < copy.len() {
                copy[pos] = value as u8;
            }
            copy
        }
        _ => data.to_vec(),
    }
}

fn canonical(value: f32) -> u32 {
    if value.is_nan() {
        0x7fc0_0000
    } else {
        value.to_bits()
    }
}

fn float_digest(values: &[f32]) -> String {
    let mut bytes = Vec::with_capacity(values.len() * 4);
    for value in values {
        bytes.extend_from_slice(&canonical(*value).to_be_bytes());
    }
    short_digest(&bytes)
}

fn ints(out: &mut String, values: &[i32]) {
    out.push('[');
    for value in values {
        write!(out, "{value},").unwrap();
    }
    out.push(']');
}

fn floats(out: &mut String, values: &[f32]) {
    out.push('[');
    for value in values {
        write!(out, "{},", canonical(*value) as i32).unwrap();
    }
    out.push(']');
}

fn summary(dec: &SampleDecoder) -> String {
    let mut out = String::new();
    write!(out, "{},{}|", dec.block_size0, dec.block_size1).unwrap();
    for book in &dec.codebooks {
        write!(out, "cb:{},{}", book.dimensions, book.entries).unwrap();
        ints(&mut out, &book.lengths);
        match &book.multiplicands {
            None => out.push('-'),
            Some(table) => {
                let dims = book.dimensions as usize;
                for entry in 0..book.entries as usize {
                    floats(&mut out, &table[entry * dims..entry * dims + dims]);
                }
            }
        }
        ints(&mut out, &book.huffman_tree);
        out.push(';');
    }
    for floor in &dec.floors {
        out.push_str("fl:");
        ints(&mut out, &floor.partition_class_list);
        ints(&mut out, &floor.class_dimensions);
        ints(&mut out, &floor.class_subclasses);
        ints(&mut out, &floor.class_masterbooks);
        for books in &floor.subclass_books {
            ints(&mut out, books);
        }
        write!(out, "{}", floor.multiplier).unwrap();
        ints(&mut out, &floor.x_list);
        out.push(';');
    }
    for residue in &dec.residues {
        write!(
            out,
            "rs:{},{},{},{},{},{}",
            residue.residue_type,
            residue.begin,
            residue.end,
            residue.partition_size,
            residue.classifications,
            residue.classbook
        )
        .unwrap();
        ints(&mut out, &residue.books);
        out.push(';');
    }
    for mapping in &dec.mappings {
        write!(out, "mp:{},{}", mapping.submaps, mapping.mux).unwrap();
        ints(&mut out, &mapping.submap_floors);
        ints(&mut out, &mapping.submap_residues);
        out.push(';');
    }
    out.push_str("md:");
    for flag in &dec.no_residues {
        out.push(if *flag { '1' } else { '0' });
    }
    ints(&mut out, &dec.modes);
    floats(&mut out, &dec.trig[0].t1);
    floats(&mut out, &dec.trig[0].t2);
    floats(&mut out, &dec.trig[0].t3);
    ints(&mut out, &dec.trig[0].reversed);
    floats(&mut out, &dec.trig[1].t1);
    floats(&mut out, &dec.trig[1].t2);
    floats(&mut out, &dec.trig[1].t3);
    ints(&mut out, &dec.trig[1].reversed);
    short_digest(out.as_bytes())
}

fn decode(dec: &mut SampleDecoder, data: &[u8]) -> R<String> {
    let mut sample = MusicSample::new(data)?;
    let mut guard = 0usize;
    while sample.has_more_samples() && (sample.sound_indices as usize) < sample.packets.len() {
        if guard >= ITERATION_CAP {
            break;
        }
        guard += 1;
        let len = sample.sample_length.wrapping_add(sample.sample_rate / 8);
        sample.calculate_audio_buffer_length(dec, len)?;
    }
    let header = format!(
        "{},{},{},{},{},{}",
        sample.sample_rate,
        sample.sample_count,
        sample.start,
        sample.end,
        i32::from(sample.loop_consistency),
        sample.packets.len()
    );
    let state = format!(
        "{},{},{},{}",
        sample.sample_length,
        sample.sound_indices,
        i32::from(sample.has_more_samples()),
        sample.audio_buffer_length
    );
    let samples = match &sample.samples {
        None => "-".to_string(),
        Some(samples) => float_digest(samples),
    };
    Ok(format!(
        "{header}\t{state}\t{samples}\t{}",
        float_digest(&dec.window)
    ))
}

fn sample_row(dec: &mut SampleDecoder, id: &str, data: &[u8]) -> String {
    match decode(dec, data) {
        Ok(rest) => format!("S\t{id}\t{rest}"),
        Err(_) => format!("S\t{id}\terror"),
    }
}

fn setup_row(id: &str, data: &[u8], probes: &[&[u8]]) -> String {
    let Ok(mut dec) = SampleDecoder::new(data) else {
        return format!("U\t{id}\terror");
    };
    let mut sub = String::new();
    for probe in probes {
        sub.push_str(&sample_row(&mut dec, "p", probe));
        sub.push('\n');
    }
    format!("U\t{id}\t{}\t{}", summary(&dec), short_digest(sub.as_bytes()))
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn music_samples_replay_the_oracle_byte_for_byte() {
    let list = blobs();
    let map: HashMap<i32, Vec<u8>> = list.iter().cloned().collect();
    let mut order: Vec<i32> = map
        .iter()
        .filter(|(id, blob)| **id >= 1 && blob.len() >= PROBE_MIN_BYTES)
        .map(|(id, _)| *id)
        .collect();
    order.sort_by_key(|id| (map[id].len(), *id));
    let probes: Vec<&[u8]> = order[..3].iter().map(|id| map[id].as_slice()).collect();
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut expected = oracle.lines();
    let mut dec: Option<SampleDecoder> = None;
    let mut compared = 0usize;
    let mut injected = 0usize;
    let mut errors = 0usize;
    let mut sample_ok = 0usize;
    let mut setup_ok = 0usize;
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let base: i32 = f[2].parse().unwrap();
        let kind: i32 = f[3].parse().unwrap();
        let data = mutate(&map[&base], kind, f[4].parse().unwrap(), f[5].parse().unwrap());
        let got = if base == 0 && kind == 0 {
            match SampleDecoder::new(&data) {
                Ok(fresh) => {
                    let row = format!("I\t{}\t{}", f[1], summary(&fresh));
                    dec = Some(fresh);
                    row
                }
                Err(_) => format!("I\t{}\terror", f[1]),
            }
        } else if base == 0 {
            setup_row(f[1], &data, &probes)
        } else {
            sample_row(dec.as_mut().expect("clean setup"), f[1], &data)
        };
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        compared += 1;
        injected += usize::from(kind != 0);
        let failed = got.ends_with("\terror");
        errors += usize::from(failed);
        if !failed {
            sample_ok += usize::from(got.starts_with("S\t"));
            setup_ok += usize::from(got.starts_with("U\t"));
        }
    }
    assert!(expected.next().is_none());
    assert!(compared > 800);
    assert!(injected > 700);
    assert!(errors > 0);
    assert!(sample_ok > 300);
    assert!(setup_ok > 20);
}

#[cfg(not(target_arch = "wasm32"))]
#[test]
fn bundle_matches_the_live_cache() {
    let Some(mut cache) = common::Cache::open() else {
        return;
    };
    for (id, blob) in blobs() {
        let live = cache.file(14, id as u32, 0).expect("sample file");
        assert_eq!(live, blob, "index 14 archive {id}");
        cache.drop_archive(14, id as u32);
    }
}
