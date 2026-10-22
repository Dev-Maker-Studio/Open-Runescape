mod common;

use common::{gunzip, short_digest};
use openrs_audio::{
    calculate_sound_attenuation, calculate_sound_pan, resample_buffer, MixedAudioBuffer,
    MixedAudioStream, Resampler, R,
};

const CASES: &[u8] = include_bytes!("fixtures/mixer-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/mixer-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/mixer-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/mixer-oracle.sha256");
const MIX_SIZE: usize = 128;

fn hex_bytes(text: &str) -> Vec<i8> {
    (0..text.len() / 2)
        .map(|i| u8::from_str_radix(&text[i * 2..i * 2 + 2], 16).unwrap() as i8)
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

fn state(s: &MixedAudioStream, mix: &[i32], filled: bool) -> String {
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
    stream: &mut MixedAudioStream,
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
    let mut buffer = MixedAudioBuffer::new(hex_bytes(f[6]), int(f[4]), int(f[5]));
    buffer.enable_loop = f[3] == "1";
    let mut stream = MixedAudioStream::new(&buffer, int(f[7]), int(f[8]), int(f[9]));
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

fn bytes(values: &[i8]) -> Vec<u8> {
    values.iter().map(|v| *v as u8).collect()
}

fn resampler_row(f: &[&str]) -> String {
    let id = f[1];
    let result: R<String> = (|| {
        let resampler = Resampler::new(int(f[2]), int(f[3]))?;
        let samples = hex_bytes(f[6]);
        let mut source = MixedAudioBuffer::new(samples.clone(), int(f[4]), int(f[5]));
        source.sample_rate = int(f[7]);
        let wide: Vec<i16> = samples
            .iter()
            .map(|v| (*v as i32).wrapping_mul(257) as i16)
            .collect();
        let narrow = resampler.resample_shorts(&wide)?;
        let rate = resampler.scale_rate(12345)?;
        let position = resampler.scale_position(777)?;
        let result = resample_buffer(&mut source, &resampler)?;
        let narrow_bytes: Vec<u8> = narrow.iter().flat_map(|v| v.to_be_bytes()).collect();
        Ok(format!(
            "R\t{id}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{rate}\t{position}",
            short_digest(&bytes(&result.samples)),
            result.samples.len(),
            result.start,
            result.end,
            result.sample_rate,
            source.sample_rate,
            source.start,
            source.end,
            short_digest(&narrow_bytes),
            narrow.len(),
        ))
    })();
    result.unwrap_or_else(|_| format!("R\t{id}\terror"))
}

fn attenuation_row(f: &[&str]) -> String {
    let volume = int(f[2]);
    let pan = int(f[3]);
    format!(
        "A\t{}\t{}\t{}",
        f[1],
        calculate_sound_attenuation(volume, pan),
        calculate_sound_pan(volume, pan)
    )
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn mixer_replays_the_oracle_byte_for_byte() {
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut expected = oracle.lines();
    let mut mixer = 0usize;
    let mut resampled = 0usize;
    let mut attenuated = 0usize;
    let mut errors = 0usize;
    let mut audible = 0usize;
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let got = match f[0] {
            "M" => {
                mixer += 1;
                let (row, count) = mixer_row(&f);
                audible += count;
                row
            }
            "R" => {
                resampled += 1;
                resampler_row(&f)
            }
            _ => {
                attenuated += 1;
                attenuation_row(&f)
            }
        };
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        errors += usize::from(got.contains("\terror"));
    }
    assert!(expected.next().is_none());
    assert!(mixer >= 15_000);
    assert!(resampled >= 300);
    assert!(attenuated >= 3000);
    assert!(errors > 100);
    assert!(audible > 10_000);
}
