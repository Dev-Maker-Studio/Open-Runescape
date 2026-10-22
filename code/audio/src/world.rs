use crate::area::{
    pack_position, AreaEvent, AreaSounds, EffectSource, Listener, MusicState, Prefs,
};
use crate::midi_pcm_stream::MidiPcmStream;
use crate::pcm::PcmPlayer;
use crate::reader::R;
use crate::song::{MidiHost, SongController, SongEvent};
use openrs_login::world_packets::misc::Misc;
use openrs_login::world_packets::zone::ZoneUpdate;
use openrs_login::world_packets::WorldPacket;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct ZoneBase {
    pub x: i32,
    pub y: i32,
    pub plane: i32,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Handled {
    Applied,
    Gated,
    OutOfOracleScope,
    NotAudio,
}

pub struct AudioWorld {
    pub area: AreaSounds,
    pub song: SongController,
    pub prefs: Prefs,
    pub listener: Listener,
    pub player: PcmPlayer,
}

fn uint(value: u16) -> i32 {
    i32::from(value)
}

impl AudioWorld {
    pub fn new(stereo: bool) -> R<Self> {
        Ok(Self {
            area: AreaSounds::new()?,
            song: SongController::new(),
            prefs: Prefs::default(),
            listener: Listener::default(),
            player: PcmPlayer::new(stereo),
        })
    }

    pub fn apply_packet(
        &mut self,
        packet: &WorldPacket,
        host: &mut dyn MidiHost,
    ) -> (Handled, Vec<SongEvent>) {
        match packet {
            WorldPacket::Misc(misc) => self.apply_misc(misc, host),
            _ => (Handled::NotAudio, Vec::new()),
        }
    }

    pub fn apply_misc(&mut self, misc: &Misc, host: &mut dyn MidiHost) -> (Handled, Vec<SongEvent>) {
        let music = self.prefs.music;
        match misc {
            Misc::MusicTrack { key, flags, song } => {
                let events = self.song.play_song(*song, i32::from(*flags), i32::from(*key), music, host);
                (Handled::Applied, events)
            }
            Misc::MusicEffect { flags, jingle, .. } => {
                self.song.play_jingle(*jingle, i32::from(*flags), music, host);
                (Handled::Applied, Vec::new())
            }
            Misc::SongVolume { volume, flags } => {
                self.song
                    .set_channel_volume(i32::from(*volume), i32::from(*flags), host);
                (Handled::Applied, Vec::new())
            }
            Misc::PreloadSong(archive) => {
                self.song.preload_song(*archive);
                (Handled::Applied, Vec::new())
            }
            Misc::PlaySongAtPosition {
                near,
                volume,
                far,
                archive,
                x,
                y,
                ..
            } => {
                let events = self.song.play_song_at_position(
                    *x as i32,
                    *y as i32,
                    i32::from(*near),
                    i32::from(*far),
                    *archive,
                    i32::from(*volume),
                    music,
                    host,
                );
                (Handled::Applied, events)
            }
            Misc::SoundSynth {
                id,
                flags,
                delay,
                volume,
                sample_rate,
            } => {
                let queued = self.area.play_synth(
                    &self.prefs,
                    *id,
                    i32::from(*flags),
                    uint(*delay),
                    i32::from(*volume),
                    0,
                    uint(*sample_rate),
                );
                (gate(queued), Vec::new())
            }
            Misc::VorbisSound {
                id,
                repeat,
                delay,
                volume,
                sample_rate,
            } => {
                let queued = self.area.play_vorbis(
                    &self.prefs,
                    *id,
                    i32::from(*repeat),
                    uint(*delay),
                    i32::from(*volume),
                    0,
                    false,
                    uint(*sample_rate),
                );
                (gate(queued), Vec::new())
            }
            Misc::VorbisSpeech {
                id,
                kind,
                delay,
                volume,
            } => {
                let queued = self.area.play_vorbis(
                    &self.prefs,
                    *id,
                    i32::from(*kind),
                    uint(*delay),
                    i32::from(*volume),
                    0,
                    true,
                    256,
                );
                (gate(queued), Vec::new())
            }
            Misc::VorbisPreloadSound { .. } | Misc::SetVorbisSound(_) | Misc::StopVorbisStream(_) => {
                (Handled::OutOfOracleScope, Vec::new())
            }
            _ => (Handled::NotAudio, Vec::new()),
        }
    }

    pub fn apply_zone(
        &mut self,
        update: &ZoneUpdate,
        zone: &ZoneBase,
        accepts: &dyn Fn(i32, i32, i32) -> bool,
    ) -> Handled {
        match update {
            ZoneUpdate::MidiSongLocation {
                tile,
                sound,
                flags,
                delay,
                volume,
                rate,
            } => {
                let x = i32::from(tile.x) + zone.x;
                let y = i32::from(tile.y) + zone.y;
                let range = i32::from(*flags >> 4);
                let mode = i32::from(*flags & 7);
                if !accepts(x, y, range + 1) {
                    return Handled::Gated;
                }
                let position = range + (zone.plane << 24) + (y << 8) + (x << 16);
                gate(self.area.play_synth(
                    &self.prefs,
                    *sound,
                    mode,
                    i32::from(*delay),
                    i32::from(*volume),
                    position,
                    uint(*rate),
                ))
            }
            ZoneUpdate::SoundArea {
                tile,
                sound,
                flags,
                delay,
                volume,
                sample_rate,
                voice_effect,
            } => {
                let x = i32::from(tile.x) + zone.x;
                let y = i32::from(tile.y) + zone.y;
                let range = i32::from(*flags >> 4);
                let repeats = i32::from(*flags & 7);
                if !accepts(x, y, range + 1) {
                    return Handled::Gated;
                }
                let position = range + (zone.plane << 24) + (y << 8) + (x << 16);
                gate(self.area.play_vorbis(
                    &self.prefs,
                    *sound,
                    repeats,
                    i32::from(*delay),
                    i32::from(*volume),
                    position,
                    *voice_effect,
                    uint(*sample_rate),
                ))
            }
            _ => Handled::NotAudio,
        }
    }

    pub fn reset_sounds(&mut self) {
        self.area.reset_voice_sounds(&mut self.player);
    }

    pub fn tick(&mut self, source: &mut dyn EffectSource) -> R<Vec<AreaEvent>> {
        let mut music = MusicState {
            current_archive: self.song.current_archive,
            song_loading: self.song.song_loading,
            jingle_playing: self.song.jingle_playing,
        };
        let events = self.area.update(
            &mut self.player,
            source,
            &self.listener,
            &self.prefs,
            &mut music,
        )?;
        self.song.current_archive = music.current_archive;
        self.song.jingle_playing = music.jingle_playing;
        Ok(events)
    }
}

fn gate(queued: bool) -> Handled {
    if queued {
        Handled::Applied
    } else {
        Handled::Gated
    }
}

pub fn position_for(plane: i32, x: i32, y: i32, range: i32) -> i32 {
    pack_position(plane, x, y, range)
}

pub fn render_music(stream: &mut MidiPcmStream, stereo: bool, blocks: usize) -> R<Vec<i16>> {
    let frames = crate::pcm::BLOCK_FRAMES;
    let len = frames * if stereo { 2 } else { 1 };
    let mut mix = vec![0i32; len];
    let mut pcm = Vec::with_capacity(len * blocks);
    for _ in 0..blocks {
        mix.fill(0);
        stream.fill(&mut mix, 0, frames as i32)?;
        pcm.extend(mix.iter().map(|v| crate::pcm::clip_sample(*v)));
    }
    Ok(pcm)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::area::LoadedEffect;
    use crate::synth::MixedAudioBuffer;
    use openrs_login::world_packets::zone::Tile;

    struct Host;

    impl MidiHost for Host {
        fn turn_off_sound(&mut self) {}
        fn music_volume(&self) -> i32 {
            100
        }
        fn set_music_volume(&mut self, _volume: i32) {}
        fn set_channel_volume(&mut self, _channel: i32, _volume: i32) {}
        fn ready(&mut self, _archive: i32, _file: i32) -> bool {
            false
        }
        fn start_track(&mut self, _archive: i32, _looping: bool, _start: i64) {}
    }

    struct Tone;

    impl EffectSource for Tone {
        fn effect(&mut self, _id: i32) -> Option<LoadedEffect> {
            Some(LoadedEffect {
                delay: 0,
                buffer: MixedAudioBuffer::new(vec![10; 64], 0, 0),
            })
        }
    }

    fn world() -> AudioWorld {
        let mut world = AudioWorld::new(true).unwrap();
        world.prefs = Prefs {
            ambient: 255,
            sound_effect: 255,
            voice_over: 255,
            music: 255,
        };
        world
    }

    #[test]
    fn music_track_maps_flags_to_volume_and_key_to_fade() {
        let mut world = world();
        let packet = WorldPacket::Misc(Misc::MusicTrack {
            key: 20,
            flags: 255,
            song: 33,
        });
        let (handled, _) = world.apply_packet(&packet, &mut Host);
        assert_eq!(handled, Handled::Applied);
        assert_eq!(world.song.music_archive, 33);
        assert_eq!(world.song.fade_step, 5);
        assert_eq!(world.song.current_archive, 33);
    }

    #[test]
    fn sound_synth_packet_queues_then_plays_on_tick() {
        let mut world = world();
        let packet = Misc::SoundSynth {
            id: 4,
            flags: 1,
            delay: 0,
            volume: 200,
            sample_rate: 256,
        };
        assert_eq!(world.apply_misc(&packet, &mut Host).0, Handled::Applied);
        world.tick(&mut Tone).unwrap();
        assert_eq!(world.player.voice_count(), 1);
        let pcm = world.player.render(2).unwrap();
        assert!(pcm.iter().any(|v| *v != 0));
    }

    #[test]
    fn muted_preference_gates_the_packet() {
        let mut world = world();
        world.prefs.sound_effect = 0;
        let packet = Misc::SoundSynth {
            id: 4,
            flags: 1,
            delay: 0,
            volume: 200,
            sample_rate: 256,
        };
        assert_eq!(world.apply_misc(&packet, &mut Host).0, Handled::Gated);
        assert!(world.area.is_empty());
    }

    #[test]
    fn zone_sound_packs_the_position_and_honours_the_gate() {
        let mut world = world();
        let zone = ZoneBase {
            x: 40,
            y: 48,
            plane: 1,
        };
        let update = ZoneUpdate::MidiSongLocation {
            tile: Tile { x: 3, y: 2 },
            sound: 9,
            flags: 0x52,
            delay: 0,
            volume: 255,
            rate: 256,
        };
        assert_eq!(world.apply_zone(&update, &zone, &|_, _, _| false), Handled::Gated);
        assert!(world.area.is_empty());
        let seen = std::cell::Cell::new((0, 0, 0));
        let handled = world.apply_zone(&update, &zone, &|x, y, r| {
            seen.set((x, y, r));
            true
        });
        assert_eq!(handled, Handled::Applied);
        assert_eq!(seen.get(), (43, 50, 6));
        assert_eq!(world.area.len(), 1);
        assert_eq!(position_for(1, 43, 50, 5), 5 + (1 << 24) + (50 << 8) + (43 << 16));
    }

    #[test]
    fn vorbis_packets_are_reported_out_of_scope_or_queue_unresolved() {
        let mut world = world();
        assert_eq!(
            world.apply_misc(&Misc::SetVorbisSound(3), &mut Host).0,
            Handled::OutOfOracleScope
        );
        let speech = Misc::VorbisSpeech {
            id: 7,
            kind: 1,
            delay: 0,
            volume: 255,
        };
        assert_eq!(world.apply_misc(&speech, &mut Host).0, Handled::Applied);
        world.tick(&mut Tone).unwrap();
        assert_eq!(world.player.voice_count(), 0);
        world.reset_sounds();
    }

    #[test]
    fn song_volume_uses_the_signed_channel_and_flags_as_volume() {
        let mut world = world();
        world.song.load_state = 1;
        world.apply_misc(&Misc::SongVolume { volume: -1, flags: 90 }, &mut Host);
        assert_eq!(world.song.channel_volumes, [90; 16]);
        world.apply_misc(&Misc::SongVolume { volume: 3, flags: 10 }, &mut Host);
        assert_eq!(world.song.channel_volumes[3], 10);
    }
}
