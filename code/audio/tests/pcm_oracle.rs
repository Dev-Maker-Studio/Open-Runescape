mod common;

use common::{gunzip, short_digest};
use openrs_audio::{MixedAudioBuffer, PcmPlayer, VoiceId, R};

const CASES: &[u8] = include_bytes!("fixtures/pcm-cases.tsv.gz");
const CASES_SHA256: &str = include_str!("fixtures/pcm-cases.sha256");
const ORACLE: &[u8] = include_bytes!("fixtures/pcm-oracle.tsv.gz");
const ORACLE_SHA256: &str = include_str!("fixtures/pcm-oracle.sha256");

fn int(text: &str) -> i32 {
    text.parse().unwrap()
}

fn add_voice(player: &mut PcmPlayer, text: &str) -> VoiceId {
    let p: Vec<&str> = text.split(',').collect();
    let samples: Vec<i8> = (0..p[0].len() / 2)
        .map(|i| u8::from_str_radix(&p[0][i * 2..i * 2 + 2], 16).unwrap() as i8)
        .collect();
    let mut buffer = MixedAudioBuffer::new(samples, int(p[1]), int(p[2]));
    buffer.enable_loop = p[3] == "1";
    player.add_buffer(&buffer, int(p[4]), int(p[5]), int(p[6]), int(p[7]))
}

fn flags(player: &PcmPlayer, all: &[VoiceId]) -> String {
    all.iter()
        .map(|id| match player.fill_of(*id) {
            Some(true) => '1',
            Some(false) => '0',
            None => '-',
        })
        .collect()
}

fn run(f: &[&str]) -> (String, usize) {
    let id = f[1];
    let mut audible = 0usize;
    let mut player = PcmPlayer::new(f[2] == "1");
    let mut all: Vec<VoiceId> = Vec::new();
    let mut log = String::new();
    let mut block = vec![0i32; player.block_len()];
    let ops: Vec<&str> = if f[4].is_empty() {
        Vec::new()
    } else {
        f[4].split(';').collect()
    };
    for text in f[3].split('/').filter(|t| !t.is_empty()) {
        let voice = add_voice(&mut player, text);
        all.push(voice);
    }
    let mut index = 0usize;
    let result: R<()> = (|| {
        while index < ops.len() {
            let op = ops[index];
            let arg = &op[1..];
            let pair: Vec<&str> = arg.split(',').collect();
            match op.as_bytes()[0] {
                b'R' => {
                    for _ in 0..int(arg) {
                        player.fill_block(&mut block)?;
                        let bytes: Vec<u8> = block
                            .iter()
                            .flat_map(|v| openrs_audio::clip_sample(*v).to_le_bytes())
                            .collect();
                        audible += usize::from(block.iter().any(|v| *v != 0));
                        log.push_str(&short_digest(&bytes));
                        log.push(':');
                        log.push_str(&flags(&player, &all));
                        log.push('|');
                    }
                }
                b'K' => {
                    player.skip_block()?;
                    log.push_str("K:");
                    log.push_str(&flags(&player, &all));
                    log.push('|');
                }
                b'V' => {
                    let voice = *all.get(int(pair[0]) as usize).ok_or(openrs_audio::AudioError::Bounds)?;
                    if let Some(stream) = player.voice_mut(voice) {
                        stream.set_volume(int(pair[1]));
                    }
                }
                b'N' => {
                    let voice = *all.get(int(pair[0]) as usize).ok_or(openrs_audio::AudioError::Bounds)?;
                    if let Some(stream) = player.voice_mut(voice) {
                        stream.set_pan_position(int(pair[1]));
                    }
                }
                b'O' => {
                    let voice = *all.get(int(pair[0]) as usize).ok_or(openrs_audio::AudioError::Bounds)?;
                    if let Some(stream) = player.voice_mut(voice) {
                        stream.fade_out_and_stop(int(pair[1]))?;
                    }
                }
                b'X' => {
                    let voice = *all.get(int(arg) as usize).ok_or(openrs_audio::AudioError::Bounds)?;
                    player.remove_voice(voice);
                }
                b'A' => {
                    let voice = add_voice(&mut player, arg);
                    all.push(voice);
                }
                _ => unreachable!(),
            }
            index += 1;
        }
        Ok(())
    })();
    match result {
        Ok(()) => (
            format!(
                "P\t{id}\t{}\t{}",
                short_digest(log.as_bytes()),
                flags(&player, &all)
            ),
            audible,
        ),
        Err(_) => (format!("P\t{id}\terror\t{index}"), audible),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn pcm_player_replays_the_oracle_byte_for_byte() {
    let cases = String::from_utf8(gunzip(CASES, CASES_SHA256)).unwrap();
    let oracle = String::from_utf8(gunzip(ORACLE, ORACLE_SHA256)).unwrap();
    let mut expected = oracle.lines();
    let mut rows = 0usize;
    let mut errors = 0usize;
    let mut audible = 0usize;
    for line in cases.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let (got, count) = run(&f);
        audible += count;
        let want = expected.next().expect("oracle row");
        assert_eq!(got, want, "case {line}");
        errors += usize::from(got.contains("\terror"));
        rows += 1;
    }
    assert!(expected.next().is_none());
    assert!(rows >= 1800);
    assert!(errors > 0);
    assert!(audible > 1000);
}
