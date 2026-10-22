mod common;

use std::collections::HashMap;
use std::fmt::Write;
use std::sync::Arc;

use common::{gunzip, short_digest};
use openrs_audio::audio_file::{AudioBuffer, AudioFile};
use openrs_audio::midi_pcm_stream::{MidiConfig, MidiPcmStream, PatchSource};
use openrs_audio::music_patch::SoundBank;
use openrs_audio::music_track::MusicTrack;
use openrs_audio::synth::MixedAudioBuffer;
use openrs_audio::R;

const INPUT: &[u8] = include_bytes!("fixtures/music-input.bin.gz");
const INPUT_SHA256: &str = include_str!("fixtures/music-input.sha256");
const CASES: &[u8] = include_bytes!("fixtures/midi-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/midi-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/midi-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/midi-oracle.sha256");

const PATCH_BASE: i32 = 1_000_000;

struct Lcg(u64);

impl Lcg {
    fn next(&mut self, bound: u64) -> i32 {
        self.0 = self
            .0
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        ((self.0 >> 33) % bound) as i32
    }
}

fn synthetic(id: i32, kind: i32) -> AudioFile {
    let mut rng = Lcg((i64::from(id) * 2 + i64::from(kind) + 1) as u64);
    let length = 200 + rng.next(1800);
    let start = rng.next(u64::from((length / 2) as u32));
    let end = start + 1 + rng.next(u64::from((length - start - 4) as u32));
    let looping = rng.next(2) == 1;
    if kind == 0 {
        let samples = (0..length).map(|_| (rng.next(256) - 128) as i8).collect();
        let mut buffer = MixedAudioBuffer::new(samples, start, end);
        buffer.enable_loop = looping;
        return AudioFile::Effect(buffer);
    }
    let rates = [11025, 22050, 32000, 44100];
    let rate = rates[rng.next(4) as usize];
    let samples = (0..length)
        .map(|_| (rng.next(2001) - 1000) as f32 / 1000.0f32)
        .collect();
    AudioFile::Music(AudioBuffer::new(rate, samples, start, end, looping))
}

struct Bank;

impl SoundBank for Bank {
    fn list_effect(&mut self, id: i32, _rates: &mut [i32]) -> Option<Arc<AudioFile>> {
        Some(Arc::new(synthetic(id, 0)))
    }

    fn list_music(&mut self, id: i32, _rates: &mut [i32]) -> Option<Arc<AudioFile>> {
        Some(Arc::new(synthetic(id, 1)))
    }
}

struct Source<'a>(&'a HashMap<i32, Vec<u8>>);

impl PatchSource for Source<'_> {
    fn get_file(&mut self, id: i32) -> Option<Vec<u8>> {
        self.0.get(&(PATCH_BASE + id)).cloned()
    }
}

fn be(data: &[u8], at: &mut usize) -> i32 {
    let value = i32::from_be_bytes(data[*at..*at + 4].try_into().unwrap());
    *at += 4;
    value
}

fn blobs() -> HashMap<i32, Vec<u8>> {
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

fn mutate(data: &[u8], kind: i32, pos: usize, value: i32) -> Vec<u8> {
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

fn state(s: &MidiPcmStream) -> String {
    let mut out = String::new();
    write!(
        out,
        "{},{},{},{},{},{},{}",
        s.current_tick_time,
        s.next_event_tick_time,
        s.prioritized_track,
        s.prioritized_track_length,
        i32::from(s.data_loaded()),
        s.active_node_count(),
        s.fading_count()
    )
    .unwrap();
    for c in 0..16 {
        write!(
            out,
            ",{},{},{},{}",
            s.program_change[c], s.pitch_bend[c], s.sustain[c], s.effect_speed_scaled[c]
        )
        .unwrap();
    }
    out
}

fn args(text: &str) -> Vec<i32> {
    if text.is_empty() {
        return Vec::new();
    }
    text.split(',').map(|p| p.parse().unwrap()).collect()
}

fn run(f: &[&str], blobs: &HashMap<i32, Vec<u8>>, op_index: &mut i32) -> R<(String, usize)> {
    let base: i32 = f[2].parse().unwrap();
    let data = mutate(
        &blobs[&base],
        f[3].parse().unwrap(),
        f[4].parse().unwrap(),
        f[5].parse().unwrap(),
    );
    let stereo = f[7] == "1";
    let looping = f[8] == "1";
    let channels = if stereo { 2 } else { 1 };
    let mut all = String::new();
    let mut track = MusicTrack::new(&data)?;
    let mut stream = MidiPcmStream::new(MidiConfig {
        sample_rate: f[6].parse().unwrap(),
        stereo,
    })?;
    stream.load()?;
    stream.ready(&mut track, &mut Source(blobs), &mut Bank)?;
    stream.set_music_track(&track, looping)?;
    all.push_str(&state(&stream));
    all.push('|');
    let ops: Vec<&str> = if f[9].is_empty() {
        Vec::new()
    } else {
        f[9].split(';').collect()
    };
    for (index, op) in ops.iter().enumerate() {
        *op_index = index as i32;
        let a = args(&op[1..]);
        match op.as_bytes()[0] {
            b'F' => {
                let mut mix = vec![0i32; a[0] as usize * channels];
                stream.fill(&mut mix, 0, a[0])?;
                let bytes: Vec<u8> = mix.iter().flat_map(|v| v.to_be_bytes()).collect();
                all.push_str(&short_digest(&bytes));
                all.push(',');
            }
            b'K' => stream.skip(a[0])?,
            b'V' => stream.set_music_volume(a[0]),
            b'W' => stream.set_volume_multiplier(a[0]),
            b'C' => stream.set_channel_volume(a[0], a[1]),
            b'B' => stream.set_pitch_bend(a[0] as usize, a[1]),
            b'R' => stream.reset_music_patches(),
            b'T' => stream.set_music_track_at_position(&track, looping, i64::from(a[0]))?,
            b'S' => stream.set_music_track(&track, looping)?,
            b'O' => stream.turn_off_sound()?,
            _ => unreachable!(),
        }
        all.push_str(&state(&stream));
        all.push('|');
    }
    Ok((short_digest(all.as_bytes()), all.len()))
}

fn row(f: &[&str], blobs: &HashMap<i32, Vec<u8>>) -> String {
    let mut op_index = -1;
    match run(f, blobs, &mut op_index) {
        Ok((digest, len)) => format!("M\t{}\t{digest}\t{len}", f[1]),
        Err(_) => format!("M\t{}\terror\t{op_index}", f[1]),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn midi_pcm_stream_replays_the_oracle_byte_for_byte() {
    let blobs = blobs();
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut expected = oracle.lines();
    let mut compared = 0usize;
    let mut errors = 0usize;
    let mut injected = 0usize;
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let got = row(&f, &blobs);
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        compared += 1;
        errors += usize::from(got.contains("\terror\t"));
        injected += usize::from(f[3] != "0");
    }
    assert!(expected.next().is_none());
    assert!(compared > 200);
    assert!(injected > 100);
    assert!(errors > 0);
}
