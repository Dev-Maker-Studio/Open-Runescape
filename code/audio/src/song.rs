#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct SongRange {
    pub x: i32,
    pub z: i32,
    pub near: i32,
    pub far: i32,
    pub range: i32,
}

impl SongRange {
    pub fn new(x: i32, z: i32, near: i32, far: i32) -> Self {
        Self {
            x,
            z,
            near,
            far,
            range: far.wrapping_sub(near),
        }
    }
}

pub trait MidiHost {
    fn turn_off_sound(&mut self);
    fn music_volume(&self) -> i32;
    fn set_music_volume(&mut self, volume: i32);
    fn set_channel_volume(&mut self, channel: i32, volume: i32);
    fn ready(&mut self, archive: i32, file: i32) -> bool;
    fn start_track(&mut self, archive: i32, looping: bool, start_position: i64);

    fn data_loaded(&self) -> bool {
        true
    }

    fn unlink_music_patches(&mut self) {}
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum SongEvent {
    SongLoaded(i32),
    TransitionApplied,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Preloaded {
    pub archive: i32,
    pub loaded: bool,
}

pub struct SongController {
    pub load_state: i32,
    pub music_archive: i32,
    pub music_file: i32,
    pub sound_level: i32,
    pub looping: bool,
    pub fade_step: i32,
    pub fade_in_rate: i32,
    pub start_position: i64,
    pub request_pending: bool,
    pub transition_pending: bool,
    pub current_archive: i32,
    pub jingle_playing: bool,
    pub primary: Option<SongRange>,
    pub secondary: Option<SongRange>,
    pub preloaded: Option<Preloaded>,
    pub channel_volumes: [i32; 16],
    pub song_loading: bool,
    pub song_transitioning: bool,
    pub jingle_captured: bool,
    pub music_from_jingle: bool,
}

impl Default for SongController {
    fn default() -> Self {
        Self::new()
    }
}

impl SongController {
    pub fn new() -> Self {
        Self {
            load_state: 0,
            music_archive: 0,
            music_file: 0,
            sound_level: 0,
            looping: false,
            fade_step: 0,
            fade_in_rate: 0,
            start_position: 0,
            request_pending: false,
            transition_pending: false,
            current_archive: -1,
            jingle_playing: false,
            primary: None,
            secondary: None,
            preloaded: None,
            channel_volumes: [255; 16],
            song_loading: false,
            song_transitioning: false,
            jingle_captured: false,
            music_from_jingle: false,
        }
    }

    pub fn discard_song_data(&mut self, host: &mut dyn MidiHost) {
        host.turn_off_sound();
        self.load_state = 1;
        self.request_pending = false;
    }

    pub fn load_and_play(&mut self, archive: i32, file: i32, sound_level: i32, looping: bool) {
        self.load_state = 1;
        self.music_archive = archive;
        self.music_file = file;
        self.sound_level = sound_level;
        self.looping = looping;
        self.request_pending = true;
        self.fade_step = 10000;
        self.start_position = 0;
        self.fade_in_rate = 0;
    }

    pub fn transition_to_next_song(
        &mut self,
        fade_steps: i32,
        archive: i32,
        sound_level: i32,
        host: &mut dyn MidiHost,
        events: &mut Vec<SongEvent>,
    ) {
        if fade_steps > 0 {
            self.load_state = 1;
            self.music_archive = archive;
            self.music_file = 0;
            self.sound_level = sound_level;
            self.looping = false;
            self.request_pending = true;
            self.fade_step = (host.music_volume() / fade_steps).max(1);
            self.transition_pending = true;
        } else {
            events.push(SongEvent::TransitionApplied);
            self.load_and_play(archive, 0, sound_level, false);
        }
    }

    pub fn clear_primary(&mut self) -> Option<i32> {
        if self.primary.is_some() {
            self.primary = None;
            Some(1)
        } else {
            None
        }
    }

    pub fn reset_channel_volumes(&mut self, host: &mut dyn MidiHost) {
        self.set_channel_volume(-1, 255, host);
    }

    pub fn set_channel_volume(&mut self, channel: i32, volume: i32, host: &mut dyn MidiHost) {
        if self.load_state != 0 {
            if channel < 0 {
                self.channel_volumes = [volume; 16];
            } else if let Some(slot) = self.channel_volumes.get_mut(channel as usize) {
                *slot = volume;
            }
        }
        host.set_channel_volume(channel, volume);
    }

    pub fn play_song(
        &mut self,
        archive: i32,
        volume: i32,
        fade: i32,
        music_pref: i32,
        host: &mut dyn MidiHost,
    ) -> Vec<SongEvent> {
        let mut events = Vec::new();
        match self.preloaded {
            Some(pre) if pre.loaded && pre.archive == archive => {
                let adjusted = (volume.wrapping_mul(music_pref)) >> 8;
                host.turn_off_sound();
                host.set_music_volume(adjusted);
                self.clear_primary();
                self.current_archive = archive;
                self.preloaded = None;
                events.push(SongEvent::SongLoaded(-1));
            }
            _ => {
                let adjusted = (volume.wrapping_mul(music_pref)) >> 8;
                if archive == -1 && !self.jingle_playing {
                    self.discard_song_data(host);
                } else if archive != -1
                    && (archive != self.current_archive || !self.song_loading)
                    && adjusted != 0
                    && !self.jingle_playing
                {
                    self.transition_to_next_song(fade, archive, adjusted, host, &mut events);
                    self.reset_channel_volumes(host);
                }
                self.current_archive = archive;
                self.secondary = None;
            }
        }
        events
    }

    #[allow(clippy::too_many_arguments)]
    pub fn play_song_at_position(
        &mut self,
        x: i32,
        z: i32,
        near: i32,
        far: i32,
        archive: i32,
        volume: i32,
        music_pref: i32,
        host: &mut dyn MidiHost,
    ) -> Vec<SongEvent> {
        let events = self.play_song(archive, volume, 100, music_pref, host);
        self.secondary = Some(SongRange::new(x << 9, z << 9, near << 8, far.wrapping_mul(8)));
        events
    }

    pub fn preload_song(&mut self, archive: i32) {
        self.preloaded = if archive == -1 {
            None
        } else {
            Some(Preloaded {
                archive,
                loaded: false,
            })
        };
    }

    pub fn play_jingle(&mut self, jingle: i32, volume: i32, music_pref: i32, host: &mut dyn MidiHost) {
        let adjusted = volume.wrapping_mul(music_pref) >> 8;
        if adjusted != 0 && jingle != -1 {
            if !self.jingle_playing
                && self.current_archive != -1
                && self.song_loading
                && !self.song_transitioning
            {
                self.jingle_captured = true;
                self.clear_primary();
            }
            self.load_and_play(jingle, 0, adjusted, false);
            self.music_from_jingle = true;
            self.set_channel_volume(-1, 255, host);
            self.jingle_playing = true;
        }
    }

    pub fn update_fade(&mut self, host: &mut dyn MidiHost) {
        if self.load_state == 1 {
            let volume = host.music_volume();
            if volume > 0 && host.data_loaded() {
                host.set_music_volume((volume - self.fade_step).max(0));
                return;
            }
            host.turn_off_sound();
            host.unlink_music_patches();
            self.load_state = if self.request_pending { 2 } else { 0 };
        }
        if self.load_state == 3 {
            let volume = host.music_volume();
            if volume < self.sound_level && host.data_loaded() {
                host.set_music_volume((volume + self.fade_in_rate).min(self.sound_level));
            } else {
                self.fade_in_rate = 0;
                self.load_state = 0;
            }
        }
    }

    pub fn load_pending(&mut self, host: &mut dyn MidiHost) -> bool {
        if self.load_state != 2 {
            return false;
        }
        if !host.ready(self.music_archive, self.music_file) {
            return false;
        }
        if self.fade_in_rate > 0 {
            self.load_state = 3;
            host.set_music_volume(self.fade_in_rate.min(self.sound_level));
        } else {
            self.load_state = 0;
            host.set_music_volume(self.sound_level);
        }
        for channel in 0..16 {
            host.set_channel_volume(channel as i32, self.channel_volumes[channel]);
            self.channel_volumes[channel] = 255;
        }
        host.start_track(self.music_archive, self.looping, self.start_position);
        self.request_pending = false;
        self.transition_pending = false;
        self.start_position = 0;
        true
    }
}

pub fn spatial_music_volume(
    player_x: f32,
    player_y: f32,
    player_z: f32,
    range: &SongRange,
) -> i32 {
    let dx = player_x - range.x as f32;
    let dy = player_y;
    let dz = player_z - range.z as f32;
    let magnitude = (dx * dx + dy * dy + dz * dz).sqrt();
    let multiplier: f32 = if magnitude < range.near as f32 {
        1.0
    } else if magnitude > range.far as f32 {
        0.0
    } else {
        1.0 - (magnitude - range.near as f32) / range.range as f32
    };
    ((multiplier * 256.0) as i32) >> 8
}

#[cfg(test)]
mod tests {
    use super::*;

    #[derive(Default)]
    struct Recorder {
        log: Vec<String>,
        volume: i32,
        ready: bool,
    }

    impl MidiHost for Recorder {
        fn turn_off_sound(&mut self) {
            self.log.push("off".into());
        }
        fn music_volume(&self) -> i32 {
            self.volume
        }
        fn set_music_volume(&mut self, volume: i32) {
            self.volume = volume;
            self.log.push(format!("vol{volume}"));
        }
        fn set_channel_volume(&mut self, channel: i32, volume: i32) {
            self.log.push(format!("ch{channel}={volume}"));
        }
        fn ready(&mut self, _archive: i32, _file: i32) -> bool {
            self.ready
        }
        fn start_track(&mut self, archive: i32, looping: bool, start: i64) {
            self.log.push(format!("track{archive},{looping},{start}"));
        }
    }

    #[test]
    fn fading_transition_uses_a_volume_proportional_step() {
        let mut host = Recorder {
            volume: 200,
            ..Recorder::default()
        };
        let mut song = SongController::new();
        song.play_song(7, 255, 40, 255, &mut host);
        assert_eq!(song.load_state, 1);
        assert_eq!(song.fade_step, 5);
        assert_eq!(song.music_archive, 7);
        assert_eq!(song.sound_level, (255 * 255) >> 8);
        assert!(song.transition_pending);
        assert_eq!(song.current_archive, 7);
        assert_eq!(song.channel_volumes, [255; 16]);
    }

    #[test]
    fn zero_volume_or_jingle_blocks_a_new_song() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.play_song(7, 255, 40, 0, &mut host);
        assert_eq!(song.load_state, 0);
        assert_eq!(song.current_archive, 7);
        song.jingle_playing = true;
        song.play_song(8, 255, 40, 255, &mut host);
        assert_eq!(song.load_state, 0);
        song.jingle_playing = false;
        song.play_song(-1, 255, 40, 255, &mut host);
        assert_eq!(song.load_state, 1);
        assert_eq!(host.log, vec!["off".to_string()]);
    }

    #[test]
    fn preloaded_song_starts_at_once_and_reports_loaded() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.preloaded = Some(Preloaded {
            archive: 9,
            loaded: true,
        });
        song.primary = Some(SongRange::new(0, 0, 1, 2));
        let events = song.play_song(9, 128, 10, 255, &mut host);
        assert_eq!(events, vec![SongEvent::SongLoaded(-1)]);
        assert_eq!(song.current_archive, 9);
        assert!(song.preloaded.is_none());
        assert!(song.primary.is_none());
        assert_eq!(host.volume, (128 * 255) >> 8);
    }

    #[test]
    fn load_pending_walks_the_state_machine() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.load_and_play(3, 0, 120, true);
        assert!(!song.load_pending(&mut host));
        song.load_state = 2;
        assert!(!song.load_pending(&mut host));
        host.ready = true;
        song.channel_volumes[4] = 77;
        assert!(song.load_pending(&mut host));
        assert_eq!(song.load_state, 0);
        assert_eq!(song.channel_volumes, [255; 16]);
        assert!(host.log.contains(&"ch4=77".to_string()));
        assert!(host.log.contains(&"track3,true,0".to_string()));
        song.load_and_play(3, 0, 120, false);
        song.load_state = 2;
        assert!(song.load_pending(&mut host));
        assert_eq!(song.load_state, 0);
        song.load_and_play(3, 0, 120, false);
        song.fade_in_rate = 30;
        song.load_state = 2;
        assert!(song.load_pending(&mut host));
        assert_eq!(song.load_state, 3);
        assert_eq!(host.volume, 30);
    }

    #[test]
    fn channel_volume_is_recorded_only_while_loading() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.set_channel_volume(2, 10, &mut host);
        assert_eq!(song.channel_volumes[2], 255);
        song.load_state = 1;
        song.set_channel_volume(2, 10, &mut host);
        assert_eq!(song.channel_volumes[2], 10);
        song.set_channel_volume(-1, 33, &mut host);
        assert_eq!(song.channel_volumes, [33; 16]);
    }

    #[test]
    fn positioned_song_records_the_secondary_range() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.play_song_at_position(5, 6, 3, 4, 9, 255, 255, &mut host);
        let range = song.secondary.unwrap();
        assert_eq!((range.x, range.z, range.near, range.far), (5 << 9, 6 << 9, 3 << 8, 32));
        assert_eq!(range.range, 32 - 768);
    }

    #[test]
    fn spatial_volume_matches_the_kotlin_branches() {
        let range = SongRange::new(0, 0, 100, 300);
        assert_eq!(spatial_music_volume(50.0, 0.0, 0.0, &range), 1);
        assert_eq!(spatial_music_volume(400.0, 0.0, 0.0, &range), 0);
        assert_eq!(spatial_music_volume(200.0, 0.0, 0.0, &range), 0);
    }

    #[test]
    fn jingle_captures_the_running_song_once() {
        let mut host = Recorder::default();
        let mut song = SongController::new();
        song.current_archive = 4;
        song.song_loading = true;
        song.play_jingle(12, 255, 255, &mut host);
        assert!(song.jingle_captured);
        assert!(song.jingle_playing);
        assert_eq!(song.music_archive, 12);
        assert_eq!(song.sound_level, (255 * 255) >> 8);
        song.play_jingle(-1, 255, 255, &mut host);
        assert_eq!(song.music_archive, 12);
        song.play_jingle(13, 255, 0, &mut host);
        assert_eq!(song.music_archive, 12);
    }

    #[test]
    fn preload_tracks_the_archive() {
        let mut song = SongController::new();
        song.preload_song(5);
        assert_eq!(song.preloaded, Some(Preloaded { archive: 5, loaded: false }));
        song.preload_song(-1);
        assert!(song.preloaded.is_none());
    }
}
