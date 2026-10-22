use crate::pcm::{PcmPlayer, VoiceId, SAMPLE_RATE};
use crate::reader::{AudioError, R};
use crate::resampler::{resample_buffer, Resampler};
use crate::synth::MixedAudioBuffer;

pub const AREA_SOUND_CACHE_LIMIT: usize = 50;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum SoundKind {
    Synth,
    Vorbis,
    Voice,
}

impl SoundKind {
    pub fn spoken(self) -> bool {
        !matches!(self, SoundKind::Synth)
    }
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct Listener {
    pub plane: i32,
    pub x: i32,
    pub z: i32,
    pub size: i32,
    pub camera_roll: i32,
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct Prefs {
    pub ambient: i32,
    pub sound_effect: i32,
    pub voice_over: i32,
    pub music: i32,
}

#[derive(Clone, Debug)]
pub struct LoadedEffect {
    pub delay: i32,
    pub buffer: MixedAudioBuffer,
}

pub trait EffectSource {
    fn effect(&mut self, id: i32) -> Option<LoadedEffect>;
    fn spoken_buffer(&mut self, _id: i32) -> Option<MixedAudioBuffer> {
        None
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum AreaEvent {
    MusicEnded(i32),
    JingleFinished { archive: i32, volume: i32 },
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct MusicState {
    pub current_archive: i32,
    pub song_loading: bool,
    pub jingle_playing: bool,
}

struct AreaSound {
    kind: SoundKind,
    sound_id: i32,
    loops: i32,
    delay: i32,
    volume: i32,
    position: i32,
    sample_rate: i32,
    effect: Option<LoadedEffect>,
    spoken: Option<MixedAudioBuffer>,
    voice: Option<VoiceId>,
}

pub struct AreaSounds {
    sounds: Vec<AreaSound>,
    resampler: Resampler,
}

pub fn pack_position(plane: i32, tile_x: i32, tile_z: i32, radius: i32) -> i32 {
    ((plane & 3) << 24) | ((tile_x & 0xff) << 16) | ((tile_z & 0xff) << 8) | (radius & 0xff)
}

pub fn angle_to_source(camera_roll: i32, dx: i32, dz: i32) -> i32 {
    let turn = (libm::atan2(f64::from(dx), f64::from(dz)) * 2607.5945876176133) as i32;
    let mut angle = camera_roll
        .wrapping_neg()
        .wrapping_sub(turn)
        .wrapping_sub(4096)
        & 0x3fff;
    if angle > 8192 {
        angle = 16384 - angle;
    }
    angle
}

pub fn stereo_width(distance: i32) -> i32 {
    if distance <= 0 {
        8192
    } else if distance >= 4096 {
        16384
    } else {
        distance.wrapping_mul(8192) / 4096 + 8192
    }
}

pub fn pan_for(camera_roll: i32, dx: i32, dz: i32, distance: i32) -> i32 {
    let angle = angle_to_source(camera_roll, dx, dz);
    let width = stereo_width(distance);
    ((16384 - width) >> 1).wrapping_add(angle.wrapping_mul(width) / 8192)
}

pub fn spatial_music_multiplier(magnitude: f32, near: i32, far: i32) -> i32 {
    let range = far.wrapping_sub(near);
    let multiplier: f32 = if magnitude < near as f32 {
        1.0
    } else if magnitude > far as f32 {
        0.0
    } else {
        1.0 - (magnitude - near as f32) / range as f32
    };
    (multiplier * 256.0) as i32
}

impl AreaSounds {
    pub fn new() -> R<Self> {
        Ok(Self {
            sounds: Vec::new(),
            resampler: Resampler::new(SAMPLE_RATE, SAMPLE_RATE)?,
        })
    }

    pub fn len(&self) -> usize {
        self.sounds.len()
    }

    pub fn is_empty(&self) -> bool {
        self.sounds.is_empty()
    }

    pub fn clear(&mut self) {
        self.sounds.clear();
    }

    pub fn delays(&self) -> Vec<i32> {
        self.sounds.iter().map(|s| s.delay).collect()
    }

    #[allow(clippy::too_many_arguments)]
    fn enqueue(
        &mut self,
        kind: SoundKind,
        pref: i32,
        sound_id: i32,
        loops: i32,
        delay: i32,
        volume: i32,
        position: i32,
        sample_rate: i32,
    ) -> bool {
        if pref != 0 && loops != 0 && self.sounds.len() < AREA_SOUND_CACHE_LIMIT && sound_id != -1 {
            self.sounds.push(AreaSound {
                kind,
                sound_id,
                loops,
                delay,
                volume,
                position,
                sample_rate,
                effect: None,
                spoken: None,
                voice: None,
            });
            true
        } else {
            false
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn play_synth(
        &mut self,
        prefs: &Prefs,
        sound_id: i32,
        loops: i32,
        delay: i32,
        volume: i32,
        position: i32,
        sample_rate: i32,
    ) -> bool {
        self.enqueue(
            SoundKind::Synth,
            prefs.sound_effect,
            sound_id,
            loops,
            delay,
            volume,
            position,
            sample_rate,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn play_vorbis(
        &mut self,
        prefs: &Prefs,
        sound_id: i32,
        loops: i32,
        delay: i32,
        volume: i32,
        position: i32,
        voice_effect: bool,
        sample_rate: i32,
    ) -> bool {
        let (kind, pref) = if voice_effect {
            (SoundKind::Voice, prefs.voice_over)
        } else {
            (SoundKind::Vorbis, prefs.sound_effect)
        };
        self.enqueue(kind, pref, sound_id, loops, delay, volume, position, sample_rate)
    }

    pub fn reset_voice_sounds(&mut self, player: &mut PcmPlayer) {
        for sound in &mut self.sounds {
            if sound.kind == SoundKind::Voice {
                match sound.voice {
                    Some(voice) => player.remove_voice(voice),
                    None => sound.delay = i32::MIN,
                }
            }
        }
    }

    pub fn update(
        &mut self,
        player: &mut PcmPlayer,
        source: &mut dyn EffectSource,
        listener: &Listener,
        prefs: &Prefs,
        music: &mut MusicState,
    ) -> R<Vec<AreaEvent>> {
        let mut events = Vec::new();
        let mut index = 0usize;
        while index < self.sounds.len() {
            let mut remove = false;
            let sound = &mut self.sounds[index];
            match sound.voice {
                None => {
                    sound.delay = sound.delay.wrapping_sub(1);
                    let floor = if sound.kind.spoken() { -1500 } else { -10 };
                    if sound.delay >= floor {
                        if sound.kind == SoundKind::Synth && sound.effect.is_none() {
                            sound.effect = source.effect(sound.sound_id);
                            match &sound.effect {
                                None => {
                                    index += 1;
                                    continue;
                                }
                                Some(effect) => {
                                    sound.delay = sound.delay.wrapping_add(effect.delay);
                                }
                            }
                        } else if sound.kind.spoken() && sound.spoken.is_none() {
                            sound.spoken = source.spoken_buffer(sound.sound_id);
                            if sound.spoken.is_none() {
                                index += 1;
                                continue;
                            }
                        }
                        if sound.delay < 0 {
                            let mut pan = 8192;
                            let volume;
                            if sound.position != 0 {
                                let plane = (sound.position >> 24) & 3;
                                if plane == listener.plane {
                                    let max_hear = (sound.position & 0xff) << 9;
                                    let size = listener.size << 8;
                                    let tile_x = (sound.position >> 16) & 0xff;
                                    let dx = ((tile_x << 9) + 256)
                                        .wrapping_sub(listener.x)
                                        .wrapping_add(size);
                                    let tile_z = (sound.position >> 8) & 0xff;
                                    let dz = ((tile_z << 9) + 256)
                                        .wrapping_sub(listener.z)
                                        .wrapping_add(size);
                                    let mut distance = dx
                                        .wrapping_abs()
                                        .wrapping_add(dz.wrapping_abs())
                                        .wrapping_sub(512);
                                    if distance > max_hear {
                                        sound.delay = -99999;
                                        index += 1;
                                        continue;
                                    }
                                    if distance < 0 {
                                        distance = 0;
                                    }
                                    let product = max_hear
                                        .wrapping_sub(distance)
                                        .wrapping_mul(prefs.ambient)
                                        .wrapping_mul(sound.volume);
                                    volume = product
                                        .checked_div(max_hear)
                                        .ok_or(AudioError::Invalid)?
                                        >> 2;
                                    if dx != 0 || dz != 0 {
                                        pan = pan_for(listener.camera_roll, dx, dz, distance);
                                    }
                                } else {
                                    volume = 0;
                                }
                            } else {
                                let pref = if sound.kind == SoundKind::Voice {
                                    prefs.voice_over
                                } else {
                                    prefs.sound_effect
                                };
                                volume = sound.volume.wrapping_mul(pref) >> 2;
                            }
                            if volume > 0 {
                                let buffer = if sound.kind == SoundKind::Synth {
                                    let mut raw = match &sound.effect {
                                        Some(effect) => effect.buffer.clone(),
                                        None => return Err(AudioError::Invalid),
                                    };
                                    Some(resample_buffer(&mut raw, &self.resampler)?)
                                } else {
                                    sound.spoken.clone()
                                };
                                if let Some(buffer) = buffer {
                                    let id = player.add_buffer(
                                        &buffer,
                                        sound.sample_rate,
                                        volume,
                                        pan,
                                        sound.loops.wrapping_sub(1),
                                    );
                                    sound.voice = Some(id);
                                }
                            }
                        }
                    } else {
                        remove = true;
                    }
                }
                Some(voice) => {
                    if !player.is_linked(voice) {
                        remove = true;
                    }
                }
            }
            if remove {
                self.sounds.remove(index);
            } else {
                index += 1;
            }
        }
        if music.jingle_playing && !music.song_loading {
            if prefs.music != 0 && music.current_archive != -1 {
                events.push(AreaEvent::JingleFinished {
                    archive: music.current_archive,
                    volume: prefs.music,
                });
            }
            music.jingle_playing = false;
        } else if prefs.music != 0 && music.current_archive != -1 && !music.song_loading {
            events.push(AreaEvent::MusicEnded(music.current_archive));
            music.current_archive = -1;
        }
        Ok(events)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    struct Fixed(MixedAudioBuffer, i32);

    impl EffectSource for Fixed {
        fn effect(&mut self, _id: i32) -> Option<LoadedEffect> {
            Some(LoadedEffect {
                delay: self.1,
                buffer: self.0.clone(),
            })
        }
    }

    struct Nothing;

    impl EffectSource for Nothing {
        fn effect(&mut self, _id: i32) -> Option<LoadedEffect> {
            None
        }
    }

    fn prefs() -> Prefs {
        Prefs {
            ambient: 255,
            sound_effect: 255,
            voice_over: 255,
            music: 255,
        }
    }

    fn tone() -> MixedAudioBuffer {
        MixedAudioBuffer::new((0..64).map(|i| (i * 3) as i8).collect(), 0, 0)
    }

    fn quiet() -> MusicState {
        MusicState {
            current_archive: -1,
            ..MusicState::default()
        }
    }

    #[test]
    fn enqueue_gates_on_preference_loops_id_and_capacity() {
        let mut area = AreaSounds::new().unwrap();
        let mut none = prefs();
        none.sound_effect = 0;
        assert!(!area.play_synth(&none, 1, 1, 0, 100, 0, 256));
        assert!(!area.play_synth(&prefs(), 1, 0, 0, 100, 0, 256));
        assert!(!area.play_synth(&prefs(), -1, 1, 0, 100, 0, 256));
        for _ in 0..AREA_SOUND_CACHE_LIMIT {
            assert!(area.play_synth(&prefs(), 1, 1, 0, 100, 0, 256));
        }
        assert!(!area.play_synth(&prefs(), 1, 1, 0, 100, 0, 256));
        assert_eq!(area.len(), 50);
        none.voice_over = 0;
        assert!(!area.play_vorbis(&none, 3, 1, 0, 100, 0, true, 256));
    }

    #[test]
    fn unpositioned_sound_starts_with_preference_scaled_volume() {
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        let mut source = Fixed(tone(), 0);
        assert!(area.play_synth(&prefs(), 5, 1, 0, 400, 0, 256));
        let mut music = quiet();
        area.update(&mut player, &mut source, &Listener::default(), &prefs(), &mut music)
            .unwrap();
        assert_eq!(player.voice_count(), 1);
        let voice = player.voice_mut(crate::pcm::VoiceId(0)).unwrap();
        assert_eq!(voice.target_volume(), (400 * 255) >> 2);
        assert_eq!(voice.pan_position(), 8192);
    }

    #[test]
    fn positioned_sound_attenuates_pans_and_respects_plane() {
        let listener = Listener {
            plane: 0,
            x: 10 * 512 + 256,
            z: 10 * 512 + 256,
            size: 0,
            camera_roll: 0,
        };
        let near = pack_position(0, 12, 10, 10);
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        let mut source = Fixed(tone(), 0);
        assert!(area.play_synth(&prefs(), 5, 1, 0, 256, near, 256));
        area.update(&mut player, &mut source, &listener, &prefs(), &mut quiet())
            .unwrap();
        let voice = player.voice_mut(crate::pcm::VoiceId(0)).unwrap();
        let max_hear = 10 << 9;
        let distance = 1024 - 512;
        let expected = ((max_hear - distance) * 255 * 256 / max_hear) >> 2;
        assert_eq!(voice.target_volume(), expected);
        assert_eq!(voice.pan_position(), pan_for(0, 1024, 0, distance));

        let other_plane = pack_position(1, 12, 10, 10);
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        assert!(area.play_synth(&prefs(), 5, 1, 0, 256, other_plane, 256));
        area.update(&mut player, &mut source, &listener, &prefs(), &mut quiet())
            .unwrap();
        assert_eq!(player.voice_count(), 0);
        assert_eq!(area.len(), 1);
    }

    #[test]
    fn out_of_range_sound_is_parked_then_removed() {
        let listener = Listener::default();
        let far = pack_position(0, 100, 100, 2);
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        let mut source = Fixed(tone(), 0);
        assert!(area.play_synth(&prefs(), 5, 1, 0, 256, far, 256));
        area.update(&mut player, &mut source, &listener, &prefs(), &mut quiet())
            .unwrap();
        assert_eq!(area.delays(), vec![-99999]);
        area.update(&mut player, &mut source, &listener, &prefs(), &mut quiet())
            .unwrap();
        assert!(area.is_empty());
    }

    #[test]
    fn missing_effect_keeps_the_entry_and_stops_the_scan_for_it() {
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        assert!(area.play_synth(&prefs(), 5, 1, 3, 256, 0, 256));
        for _ in 0..8 {
            area.update(&mut player, &mut Nothing, &Listener::default(), &prefs(), &mut quiet())
                .unwrap();
        }
        assert_eq!(area.len(), 1);
        assert_eq!(area.delays(), vec![-5]);
    }

    #[test]
    fn finished_voice_is_removed_and_entries_shift() {
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        let mut source = Fixed(tone(), 0);
        assert!(area.play_synth(&prefs(), 1, 1, 0, 256, 0, 256));
        assert!(area.play_synth(&prefs(), 2, 1, 5, 256, 0, 256));
        area.update(&mut player, &mut source, &Listener::default(), &prefs(), &mut quiet())
            .unwrap();
        assert_eq!(area.len(), 2);
        player.render(4).unwrap();
        area.update(&mut player, &mut source, &Listener::default(), &prefs(), &mut quiet())
            .unwrap();
        assert_eq!(area.len(), 1);
    }

    #[test]
    fn voice_reset_parks_unstarted_and_removes_started() {
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        assert!(area.play_vorbis(&prefs(), 9, 1, 4, 100, 0, true, 256));
        area.reset_voice_sounds(&mut player);
        assert_eq!(area.delays(), vec![i32::MIN]);
    }

    #[test]
    fn music_end_is_reported_once() {
        let mut area = AreaSounds::new().unwrap();
        let mut player = PcmPlayer::new(true);
        let mut music = MusicState {
            current_archive: 77,
            song_loading: false,
            jingle_playing: false,
        };
        let first = area
            .update(&mut player, &mut Nothing, &Listener::default(), &prefs(), &mut music)
            .unwrap();
        assert_eq!(first, vec![AreaEvent::MusicEnded(77)]);
        let second = area
            .update(&mut player, &mut Nothing, &Listener::default(), &prefs(), &mut music)
            .unwrap();
        assert!(second.is_empty());
        music.current_archive = 5;
        music.jingle_playing = true;
        let jingle = area
            .update(&mut player, &mut Nothing, &Listener::default(), &prefs(), &mut music)
            .unwrap();
        assert_eq!(jingle, vec![AreaEvent::JingleFinished { archive: 5, volume: 255 }]);
        assert!(!music.jingle_playing);
    }

    #[test]
    fn geometry_helpers_match_the_kotlin_constants() {
        assert_eq!(stereo_width(0), 8192);
        assert_eq!(stereo_width(4096), 16384);
        assert_eq!(stereo_width(2048), 12288);
        assert_eq!(angle_to_source(0, 0, 1), 4096);
        assert_eq!(spatial_music_multiplier(0.0, 10, 20), 256);
        assert_eq!(spatial_music_multiplier(15.0, 10, 20), 128);
        assert_eq!(spatial_music_multiplier(30.0, 10, 20), 0);
    }
}
