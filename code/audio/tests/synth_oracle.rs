mod common;

use common::{gunzip, short_digest};
use openrs_audio::{Synth, SoundEffect, R};

const INPUT: &[u8] = include_bytes!("fixtures/synth-input.bin.gz");
const INPUT_SHA256: &str = include_str!("fixtures/synth-input.sha256");
const CASES: &[u8] = include_bytes!("fixtures/synth-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/synth-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/synth-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/synth-oracle.sha256");

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

pub fn row(synth: &mut Synth, id: &str, data: &[u8]) -> String {
    let result: R<String> = (|| {
        let mut first = SoundEffect::decode(data)?;
        let a = first.mixed_audio(synth)?;
        let mut second = SoundEffect::decode(data)?;
        let delay = second.delay();
        let b = second.mixed_audio(synth)?;
        let bytes_a: Vec<u8> = a.samples.iter().map(|v| *v as u8).collect();
        let bytes_b: Vec<u8> = b.samples.iter().map(|v| *v as u8).collect();
        Ok(format!(
            "S\t{id}\t{}\t{}\t{}\t{}\t{}\t{}\t{delay}\t{}\t{}\t{}\t{}",
            short_digest(&bytes_a),
            a.samples.len(),
            first.loop_begin,
            first.loop_end,
            a.start,
            a.end,
            second.loop_begin,
            second.loop_end,
            short_digest(&bytes_b),
            b.samples.len(),
        ))
    })();
    result.unwrap_or_else(|_| format!("S\t{id}\terror"))
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn synth_replays_the_oracle_byte_for_byte() {
    let blobs: std::collections::HashMap<i32, Vec<u8>> = blobs().into_iter().collect();
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut synth = Synth::new();
    let mut compared = 0usize;
    let mut errors = 0usize;
    let mut injected = 0usize;
    let mut expected = oracle.lines();
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let base: i32 = f[2].parse().unwrap();
        let kind: i32 = f[3].parse().unwrap();
        let data = mutate(
            &blobs[&base],
            kind,
            f[4].parse().unwrap(),
            f[5].parse().unwrap(),
        );
        let got = row(&mut synth, f[1], &data);
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        compared += 1;
        errors += usize::from(got.ends_with("\terror"));
        injected += usize::from(kind != 0);
    }
    assert!(expected.next().is_none());
    assert!(compared > 10_000);
    assert!(injected > 1000);
    assert!(errors > 0);
}

#[cfg(not(target_arch = "wasm32"))]
#[test]
fn bundle_matches_the_live_cache() {
    let Some(mut cache) = common::Cache::open() else {
        return;
    };
    let mut checked = 0;
    for (id, blob) in blobs() {
        let live = cache.file(4, id as u32, 0).expect("sound effect file");
        assert_eq!(live, blob, "archive {id}");
        cache.drop_archive(4, id as u32);
        checked += 1;
    }
    assert_eq!(checked, cache.archive_ids(4).len());
}
