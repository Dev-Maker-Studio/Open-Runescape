mod common;

use std::collections::HashMap;
use std::fmt::Write;

use common::{gunzip, short_digest};
use openrs_audio::music_patch::MusicPatch;
use openrs_audio::music_track::{MidiFileReader, MusicTrack};
use openrs_audio::R;

const INPUT: &[u8] = include_bytes!("fixtures/music-input.bin.gz");
const INPUT_SHA256: &str = include_str!("fixtures/music-input.sha256");
const CASES: &[u8] = include_bytes!("fixtures/music-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/music-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/music-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/music-oracle.sha256");

const PATCH_BASE: i32 = 1_000_000;
const EVENT_CAP: usize = 300_000;

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

fn ints(values: &[i32]) -> String {
    values.iter().fold(String::new(), |mut out, v| {
        write!(out, "{v},").unwrap();
        out
    })
}

fn bytes(values: Option<&[i8]>) -> String {
    match values {
        None => "-".to_string(),
        Some(values) => values.iter().fold(String::new(), |mut out, v| {
            write!(out, "{v},").unwrap();
            out
        }),
    }
}

fn digest_text(text: &str) -> String {
    short_digest(text.as_bytes())
}

fn track_row(id: &str, data: &[u8]) -> R<String> {
    let mut track = MusicTrack::new(data)?;
    track.build_patch_table()?;
    let mut table = String::new();
    for (key, flags) in track.table.as_ref().unwrap().entries() {
        let signed: Vec<i8> = flags.iter().map(|v| *v as i8).collect();
        write!(table, "{key}:{};", bytes(Some(&signed))).unwrap();
    }
    let mut reader = MidiFileReader::new(&track.midi)?;
    let mut events = String::new();
    let count = reader.track_count();
    for t in 0..count {
        reader.goto_track(t)?;
        reader.track_length(t)?;
        reader.mark_position(t)?;
    }
    let mut emitted = 0usize;
    'outer: loop {
        loop {
            let t = reader.prioritized_track();
            let length = openrs_audio::reader::at(&reader.track_lengths, i64::from(t))?;
            while length == openrs_audio::reader::at(&reader.track_lengths, i64::from(t))? {
                reader.goto_track(t)?;
                let message = reader.read_message(t)?;
                write!(events, "{t},{message};").unwrap();
                emitted += 1;
                if emitted > EVENT_CAP {
                    break 'outer;
                }
                if message == 1 {
                    reader.set_track_done();
                    reader.mark_position(t)?;
                    if reader.is_done() {
                        break 'outer;
                    }
                    continue 'outer;
                }
                reader.track_length(t)?;
                reader.mark_position(t)?;
            }
        }
    }
    write!(
        events,
        "{},{},{};",
        reader.microseconds_per_beat,
        reader.tempo_time_offset,
        reader.tick_timestamp(1000)
    )
    .unwrap();
    reader.reset_from_timestamp(12345)?;
    write!(
        events,
        "{}{}{}",
        ints(&reader.track_lengths),
        ints(&reader.start_positions),
        reader.tempo_time_offset
    )
    .unwrap();
    Ok(format!(
        "T\t{id}\t{}\t{}\t{}\t{count}\t{}\t{}\t{emitted}\t{}",
        short_digest(&track.midi),
        track.midi.len(),
        reader.division,
        digest_text(&ints(&reader.track_starts)),
        digest_text(&table),
        digest_text(&events)
    ))
}

fn patch_row(id: &str, data: &[u8]) -> R<String> {
    let patch = MusicPatch::new(data)?;
    let mut order: HashMap<usize, usize> = HashMap::new();
    let mut keys = String::new();
    let mut infos = String::new();
    for key in 0..128 {
        let Some(info_index) = patch.info_of_key[key] else {
            keys.push_str("-1,");
            continue;
        };
        let next = order.len();
        let index = *order.entry(info_index).or_insert_with(|| {
            let info = &patch.infos[info_index];
            write!(
                infos,
                "{}/{}/{}/{}/{}/{}/{}/{}/{};",
                bytes(info.release_envelope.as_deref()),
                bytes(info.volume_envelope.as_deref()),
                info.volume_envelope_key_scaling,
                info.volume_envelope_decay,
                info.release_envelope_key_scaling,
                info.decay_key_scaling,
                info.vibrato_lfo_pitch,
                info.volume_envelope_release,
                info.vibrato_delay
            )
            .unwrap();
            next
        });
        write!(keys, "{index},").unwrap();
    }
    let shorts: String = patch.pitch_offset.iter().fold(String::new(), |mut out, v| {
        write!(out, "{v},").unwrap();
        out
    });
    let all = format!(
        "{}|{}|{}|{}|{}|{}|{}|{}",
        ints(&patch.sample_offsets),
        shorts,
        bytes(Some(&patch.volume_offset)),
        bytes(Some(&patch.pan_offset)),
        bytes(Some(&patch.loop_mode)),
        patch.base_velocity,
        keys,
        infos
    );
    Ok(format!(
        "P\t{id}\t{}\t{}\t{}\t{}",
        digest_text(&all),
        all.len(),
        patch.base_velocity,
        order.len()
    ))
}

pub fn row(id: &str, base: i32, data: &[u8]) -> String {
    let result: R<String> = if base >= PATCH_BASE {
        patch_row(id, data)
    } else {
        track_row(id, data)
    };
    result.unwrap_or_else(|_| {
        format!("{}\t{id}\terror", if base >= PATCH_BASE { "P" } else { "T" })
    })
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn music_formats_replay_the_oracle_byte_for_byte() {
    let blobs: HashMap<i32, Vec<u8>> = blobs().into_iter().collect();
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
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
        let got = row(f[1], base, &data);
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        compared += 1;
        errors += usize::from(got.ends_with("\terror"));
        injected += usize::from(kind != 0);
    }
    assert!(expected.next().is_none());
    assert!(compared > 4000);
    assert!(injected > 1000);
    assert!(errors > 0);
}

#[cfg(not(target_arch = "wasm32"))]
#[test]
fn bundle_matches_the_live_cache() {
    let Some(mut cache) = common::Cache::open() else {
        return;
    };
    for (id, blob) in blobs() {
        let (index, archive) = if id >= PATCH_BASE {
            (15, (id - PATCH_BASE) as u32)
        } else {
            (6, id as u32)
        };
        let live = cache.file(index, archive, 0).expect("music file");
        assert_eq!(live, blob, "index {index} archive {archive}");
        cache.drop_archive(index, archive);
    }
}
