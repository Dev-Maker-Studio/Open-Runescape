mod common;

use common::{gunzip, short_digest};
use openrs_audio::audio_buffer_stream::AudioBufferStream;
use openrs_audio::audio_file::AudioBuffer;
use openrs_audio::R;

const CASES: &[u8] = include_bytes!("fixtures/abuf-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/abuf-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/abuf-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/abuf-oracle.sha256");
const MIX_SIZE: usize = 128;

fn hex_floats(text: &str) -> Vec<f32> {
    (0..text.len() / 8)
        .map(|i| f32::from_bits(u32::from_str_radix(&text[i * 8..i * 8 + 8], 16).unwrap()))
        .collect()
}

fn int(text: &str) -> i32 {
    text.parse().unwrap()
}

fn args(text: &str) -> Vec<i32> {
    if text.is_empty() {
        Vec::new()
    } else {
        text.split(',').map(int).collect()
    }
}

fn ints_digest(values: &[i32]) -> String {
    let bytes: Vec<u8> = values.iter().flat_map(|v| v.to_be_bytes()).collect();
    short_digest(&bytes)
}

fn state(s: &AudioBufferStream, mix: &[i32], filled: bool) -> String {
    let priority = match s.priority() {
        Ok(value) => value.to_string(),
        Err(_) => "E".to_string(),
    };
    let mut text = format!(
        "{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",
        s.playback_position,
        s.sample_rate,
        s.current_volume,
        s.left_channel_volume,
        s.right_channel_volume,
        s.fade_samples_remaining,
        s.loops_remaining,
        s.target_volume,
        s.pan_position,
        s.volume_fade_delta,
        s.left_volume_fade_delta,
        s.right_volume_fade_delta,
        s.start,
        s.end,
        i32::from(s.linked),
        priority,
        i32::from(s.is_finished()),
        i32::from(s.is_fading()),
        s.active_voice_count(),
        s.absolute_playback_rate(),
        s.target_volume(),
        s.pan_position(),
    );
    if filled {
        text.push(',');
        text.push_str(&ints_digest(mix));
    }
    text
}

fn apply(
    stream: &mut AudioBufferStream,
    mix: &mut [i32],
    stereo: bool,
    op: &str,
) -> R<bool> {
    let a = args(&op[1..]);
    let mut filled = false;
    match op.as_bytes()[0] {
        b'F' => {
            mix.fill(0);
            stream.fill(mix, a[0], a[1], stereo)?;
            filled = true;
        }
        b'K' => stream.skip(a[0])?,
        b'D' => stream.fade_to_volume(a[0], a[1])?,
        b'P' => stream.fade_to_volume_and_pan(a[0], a[1], a[2])?,
        b'O' => stream.fade_out_and_stop(a[0])?,
        b'V' => stream.set_volume(a[0]),
        b'N' => stream.set_pan_position(a[0]),
        b'L' => stream.set_num_loops(a[0]),
        b'T' => stream.set_playback_rate(a[0]),
        b'B' => stream.reverse_playback_direction(),
        b'S' => stream.set_playback_position(a[0]),
        b'U' => stream.mute_volume(),
        b'C' => stream.cancel_fade(),
        b'W' => stream.set_volume_and_pan(a[0], a[1]),
        b'X' => {
            stream.update_fade_state();
        }
        b'Y' => stream.recalculate_channel_volumes(),
        _ => unreachable!(),
    }
    Ok(filled)
}

fn mixer_row(f: &[&str]) -> (String, usize) {
    let mut audible = 0usize;
    let id = f[1];
    let stereo = f[2] == "1";
    let buffer = AudioBuffer::new(22050, hex_floats(f[6]), int(f[4]), int(f[5]), f[3] == "1");
    let mut stream = AudioBufferStream::from_buffer(&buffer, int(f[7]), int(f[8]), int(f[9]));
    let mut mix = vec![0i32; MIX_SIZE];
    let mut all = String::new();
    let mut last = state(&stream, &mix, false);
    all.push_str(&last);
    all.push('|');
    let ops: Vec<&str> = if f[10].is_empty() {
        Vec::new()
    } else {
        f[10].split(';').collect()
    };
    for (index, op) in ops.iter().enumerate() {
        match apply(&mut stream, &mut mix, stereo, op) {
            Ok(filled) => {
                audible += usize::from(filled && mix.iter().any(|v| *v != 0));
                last = state(&stream, &mix, filled);
                all.push_str(&last);
                all.push('|');
            }
            Err(_) => return (format!("M\t{id}\terror\t{index}"), audible),
        }
    }
    (format!("M\t{id}\t{}\t{last}", short_digest(all.as_bytes())), audible)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn audio_buffer_stream_replays_the_oracle_byte_for_byte() {
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut expected = oracle.lines();
    let mut rows = 0usize;
    let mut errors = 0usize;
    let mut audible = 0usize;
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let (got, count) = mixer_row(&f);
        audible += count;
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        rows += 1;
        errors += usize::from(got.contains("\terror"));
    }
    assert!(expected.next().is_none());
    assert!(rows >= 15_000);
    assert!(errors > 100);
    assert!(audible > 5_000);
}
