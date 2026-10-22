mod common;

use std::collections::HashMap;

use common::gunzip;
use openrs_audio::midi_pcm_stream::{MidiConfig, MidiPcmStream};
use openrs_audio::song::MidiHost;
use openrs_audio::sound_bank::{Js5Files, SoundBankCache, StreamHost};
use openrs_audio::world::render_music;

const INPUT: &[u8] = include_bytes!("fixtures/music-input.bin.gz");
const INPUT_SHA256: &str = include_str!("fixtures/music-input.sha256");
const PATCH_BASE: i32 = 1_000_000;

struct Files(HashMap<i32, Vec<u8>>, i32);

impl Js5Files for Files {
    fn archive_count(&self) -> i32 {
        self.1
    }

    fn archive_file_count(&self, archive: i32) -> i32 {
        i32::from(self.0.contains_key(&archive))
    }

    fn file(&mut self, archive: i32, file: i32) -> Option<Vec<u8>> {
        if self.1 == 1 {
            self.0.get(&file).cloned()
        } else if file == 0 {
            self.0.get(&archive).cloned()
        } else {
            None
        }
    }
}

fn be(data: &[u8], at: &mut usize) -> i32 {
    let value = i32::from_be_bytes(data[*at..*at + 4].try_into().unwrap());
    *at += 4;
    value
}

fn bundle() -> (HashMap<i32, Vec<u8>>, HashMap<i32, Vec<u8>>) {
    let data = gunzip(INPUT, INPUT_SHA256);
    let mut at = 0;
    let count = be(&data, &mut at);
    let mut tracks = HashMap::new();
    let mut patches = HashMap::new();
    for _ in 0..count {
        let id = be(&data, &mut at);
        let len = be(&data, &mut at) as usize;
        let blob = data[at..at + len].to_vec();
        at += len;
        if id >= PATCH_BASE {
            patches.insert(id - PATCH_BASE, blob);
        } else {
            tracks.insert(id, blob);
        }
    }
    (tracks, patches)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn host_without_samples_never_starts_and_idle_stream_is_silent() {
    let (tracks, patches) = bundle();
    let id = *tracks.keys().min().unwrap();
    let config = MidiConfig {
        sample_rate: 22050,
        stereo: true,
    };
    let stream = MidiPcmStream::new(config).unwrap();
    let bank = SoundBankCache::new(Files(HashMap::new(), 1), Files(HashMap::new(), 1));
    let mut host = StreamHost::new(stream, Files(tracks, 2), Files(patches, 1), bank);
    host.set_music_volume(200);
    assert_eq!(host.music_volume(), 200);
    let first = host.ready(id, 0);
    assert!(host.has_pending() || !first);
    if !first {
        host.start_track(id, false, 0);
        assert!(!host.has_pending());
    }
    let mut idle = MidiPcmStream::new(config).unwrap();
    let pcm = render_music(&mut idle, true, 4).unwrap();
    assert_eq!(pcm.len(), 4 * 256 * 2);
    assert!(pcm.iter().all(|v| *v == 0));
}
