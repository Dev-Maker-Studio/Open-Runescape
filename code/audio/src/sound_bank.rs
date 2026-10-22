use std::collections::HashMap;
use std::sync::Arc;

use crate::audio_file::AudioFile;
use crate::midi_pcm_stream::{MidiPcmStream, PatchSource};
use crate::music_patch::SoundBank;
use crate::music_sample::{MusicSample, SampleDecoder};
use crate::music_track::MusicTrack;
use crate::song::MidiHost;
use crate::synth::{SoundEffect, Synth};

const DECODE_CAP: usize = 1 << 20;
const GAME_CYCLES_PER_SECOND: i32 = 50;

pub trait Js5Files {
    fn archive_count(&self) -> i32;
    fn archive_file_count(&self, archive: i32) -> i32;
    fn file(&mut self, archive: i32, file: i32) -> Option<Vec<u8>>;

    fn file_by_id(&mut self, id: i32) -> Option<Vec<u8>> {
        if self.archive_count() == 1 {
            return self.file(0, id);
        }
        if self.archive_file_count(id) == 1 {
            return self.file(id, 0);
        }
        None
    }
}

fn hash(archive: i32, file: i32) -> i64 {
    let mixed = file ^ (((archive << 4) & 0xffff) | ((archive as u32) >> 12) as i32);
    i64::from(mixed | (archive << 16))
}

pub struct SoundBankCache<E: Js5Files, M: Js5Files> {
    effects: E,
    samples: M,
    decoder: Option<SampleDecoder>,
    mixed: HashMap<i64, Arc<AudioFile>>,
    music: HashMap<i64, Arc<AudioFile>>,
    synth: Synth,
}

impl<E: Js5Files, M: Js5Files> SoundBankCache<E, M> {
    pub fn new(effects: E, samples: M) -> Self {
        Self {
            effects,
            samples,
            decoder: None,
            mixed: HashMap::new(),
            music: HashMap::new(),
            synth: Synth::new(),
        }
    }

    pub fn get_sound_effect(
        &mut self,
        archive: i32,
        file: i32,
        rates: Option<&mut [i32]>,
    ) -> Option<Arc<AudioFile>> {
        let key = hash(archive, file);
        if let Some(found) = self.mixed.get(&key) {
            return Some(found.clone());
        }
        if let Some(rates) = rates.as_ref() {
            if rates[0] <= 0 {
                return None;
            }
        }
        let data = self.effects.file(archive, file)?;
        let mut effect = SoundEffect::decode(&data).ok()?;
        let buffer = effect.mixed_audio(&mut self.synth).ok()?;
        let size = buffer.samples.len() as i32;
        let file = Arc::new(AudioFile::Effect(buffer));
        self.mixed.insert(key, file.clone());
        if let Some(rates) = rates {
            rates[0] = rates[0].wrapping_sub(size);
        }
        Some(file)
    }

    fn decode_sample(&mut self, data: &[u8]) -> Option<AudioFile> {
        if self.decoder.is_none() {
            let setup = self.samples.file(0, 0)?;
            self.decoder = SampleDecoder::new(&setup).ok();
        }
        let decoder = self.decoder.as_mut()?;
        let mut sample = MusicSample::new(data).ok()?;
        let mut guard = 0usize;
        while sample.has_more_samples() && (sample.sound_indices as usize) < sample.packets.len() {
            if guard >= DECODE_CAP {
                break;
            }
            guard += 1;
            let len = sample.sample_length.wrapping_add(sample.sample_rate / 8);
            sample.calculate_audio_buffer_length(decoder, len).ok()?;
        }
        sample
            .audio_buffer(GAME_CYCLES_PER_SECOND)
            .ok()?
            .map(AudioFile::Music)
    }

    pub fn get_music_sample(
        &mut self,
        archive: i32,
        file: i32,
        rates: Option<&mut [i32]>,
    ) -> Option<Arc<AudioFile>> {
        let key = hash(archive, file) ^ 0x1_0000_0000;
        if let Some(found) = self.music.get(&key) {
            return Some(found.clone());
        }
        if let Some(rates) = rates {
            if rates[0] <= 0 {
                return None;
            }
        }
        let data = self.samples.file(archive, file)?;
        let decoded = Arc::new(self.decode_sample(&data)?);
        self.music.insert(key, decoded.clone());
        Some(decoded)
    }

    pub fn list_music(&mut self, id: i32, rates: Option<&mut [i32]>) -> Option<Arc<AudioFile>> {
        if self.samples.archive_count() == 1 {
            self.get_music_sample(0, id, rates)
        } else if self.samples.archive_file_count(id) == 1 {
            self.get_music_sample(id, 0, rates)
        } else {
            None
        }
    }

    pub fn list_effect(&mut self, id: i32, rates: Option<&mut [i32]>) -> Option<Arc<AudioFile>> {
        if self.effects.archive_count() == 1 {
            self.get_sound_effect(0, id, rates)
        } else if self.effects.archive_file_count(id) == 1 {
            self.get_sound_effect(id, 0, rates)
        } else {
            None
        }
    }
}

impl<E: Js5Files, M: Js5Files> SoundBank for SoundBankCache<E, M> {
    fn list_effect(&mut self, id: i32, rates: &mut [i32]) -> Option<Arc<AudioFile>> {
        SoundBankCache::list_effect(self, id, Some(rates))
    }

    fn list_music(&mut self, id: i32, rates: &mut [i32]) -> Option<Arc<AudioFile>> {
        SoundBankCache::list_music(self, id, Some(rates))
    }
}

struct Patches<'a, P: Js5Files>(&'a mut P);

impl<P: Js5Files> PatchSource for Patches<'_, P> {
    fn get_file(&mut self, id: i32) -> Option<Vec<u8>> {
        self.0.file_by_id(id)
    }
}

pub struct StreamHost<T: Js5Files, P: Js5Files, E: Js5Files, M: Js5Files> {
    pub stream: MidiPcmStream,
    tracks: T,
    patches: P,
    bank: SoundBankCache<E, M>,
    pending: Option<MusicTrack>,
}

impl<T: Js5Files, P: Js5Files, E: Js5Files, M: Js5Files> StreamHost<T, P, E, M> {
    pub fn new(
        stream: MidiPcmStream,
        tracks: T,
        patches: P,
        bank: SoundBankCache<E, M>,
    ) -> Self {
        Self {
            stream,
            tracks,
            patches,
            bank,
            pending: None,
        }
    }

    pub fn has_pending(&self) -> bool {
        self.pending.is_some()
    }
}

impl<T: Js5Files, P: Js5Files, E: Js5Files, M: Js5Files> MidiHost for StreamHost<T, P, E, M> {
    fn turn_off_sound(&mut self) {
        let _ = self.stream.turn_off_sound();
    }

    fn music_volume(&self) -> i32 {
        self.stream.get_music_volume()
    }

    fn set_music_volume(&mut self, volume: i32) {
        self.stream.set_music_volume(volume);
    }

    fn set_channel_volume(&mut self, channel: i32, volume: i32) {
        self.stream.set_channel_volume(channel, volume);
    }

    fn ready(&mut self, archive: i32, file: i32) -> bool {
        if self.pending.is_none() {
            let Some(data) = self.tracks.file(archive, file) else {
                return false;
            };
            match MusicTrack::new(&data) {
                Ok(track) => self.pending = Some(track),
                Err(_) => return false,
            }
        }
        let Some(track) = self.pending.as_mut() else {
            return false;
        };
        match self
            .stream
            .ready(track, &mut Patches(&mut self.patches), &mut self.bank)
        {
            Ok(true) => {
                self.stream.reset_music_patches();
                true
            }
            _ => false,
        }
    }

    fn data_loaded(&self) -> bool {
        self.stream.data_loaded()
    }

    fn unlink_music_patches(&mut self) {
        self.stream.unlink_music_patches();
        self.pending = None;
    }

    fn start_track(&mut self, _archive: i32, looping: bool, start_position: i64) {
        let Some(track) = self.pending.take() else {
            return;
        };
        let _ = if start_position > 0 {
            self.stream
                .set_music_track_at_position(&track, looping, start_position)
        } else {
            self.stream.set_music_track(&track, looping)
        };
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hash_matches_the_kotlin_expression() {
        assert_eq!(hash(0, 5), 5);
        assert_eq!(hash(1, 2), i64::from((2 ^ 16) | (1 << 16)));
        assert_eq!(hash(0x1234, 7), i64::from((7 ^ ((0x1234 << 4) & 0xffff | (0x1234u32 >> 12) as i32)) | (0x1234 << 16)));
    }
}
