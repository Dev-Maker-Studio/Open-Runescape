use std::cell::RefCell;
use std::collections::{HashMap, HashSet};
use std::rc::Rc;

use openrs_login::world_packets::zone::{Tile, ZoneUpdate};
use openrs_login::world_packets::{decode, WorldPacket};
use openrs_protocol::registry::ServerPacket;

use crate::area::{spatial_music_multiplier, EffectSource, LoadedEffect};
use crate::midi_pcm_stream::{MidiConfig, MidiPcmStream};
use crate::pcm::{clip_sample, BLOCK_FRAMES};
use crate::reader::R;
use crate::song::MidiHost;
use crate::sound_bank::{Js5Files, SoundBankCache, StreamHost};
use crate::synth::{SoundEffect, Synth};
use crate::world::{AudioWorld, ZoneBase};

pub const TRACK_INDEX: i32 = 6;
pub const PATCH_INDEX: i32 = 15;
pub const SAMPLE_INDEX: i32 = 14;
pub const EFFECT_INDEX: i32 = 4;

const MISC: i32 = 1;
const SOUND_AREA: i32 = 3;
const MIDI_SONG_LOCATION: i32 = 4;
const DEFAULT_VOLUME: i32 = 127;
const MAX_BLOCKS: usize = 256;

#[derive(Default)]
struct Store {
    files: HashMap<(i32, i32, i32), Vec<u8>>,
    done: HashSet<(i32, i32)>,
    asked: HashSet<(i32, i32)>,
    wanted: Vec<(i32, i32)>,
}

type Shared = Rc<RefCell<Store>>;

#[derive(Clone)]
struct Index {
    store: Shared,
    index: i32,
}

impl Js5Files for Index {
    fn archive_count(&self) -> i32 {
        2
    }

    fn archive_file_count(&self, _archive: i32) -> i32 {
        1
    }

    fn file(&mut self, archive: i32, file: i32) -> Option<Vec<u8>> {
        let mut store = self.store.borrow_mut();
        if let Some(found) = store.files.get(&(self.index, archive, file)) {
            return Some(found.clone());
        }
        let group = (self.index, archive);
        if !store.done.contains(&group) && store.asked.insert(group) {
            store.wanted.push(group);
        }
        None
    }
}

struct Effects {
    index: Index,
    synth: Synth,
    cache: HashMap<i32, Option<(i32, crate::synth::MixedAudioBuffer)>>,
}

impl EffectSource for Effects {
    fn effect(&mut self, id: i32) -> Option<LoadedEffect> {
        if let Some(Some((delay, buffer))) = self.cache.get(&id) {
            return Some(LoadedEffect {
                delay: *delay,
                buffer: buffer.clone(),
            });
        }
        let data = self.index.file(id, 0)?;
        let mut effect = match SoundEffect::decode(&data) {
            Ok(effect) => effect,
            Err(_) => {
                self.cache.insert(id, None);
                return None;
            }
        };
        let delay = effect.delay();
        let buffer = effect.mixed_audio(&mut self.synth).ok()?;
        self.cache.insert(id, Some((delay, buffer.clone())));
        Some(LoadedEffect { delay, buffer })
    }
}

type Host = StreamHost<Index, Index, Index, Index>;

pub struct Engine {
    world: AudioWorld,
    host: Host,
    effects: Effects,
    store: Shared,
    stereo: bool,
    mix: Vec<i32>,
    base: (i32, i32),
}

impl Engine {
    pub fn new(stereo: bool) -> R<Self> {
        let store: Shared = Rc::new(RefCell::new(Store::default()));
        let index = |number: i32| Index {
            store: store.clone(),
            index: number,
        };
        let config = MidiConfig {
            sample_rate: 22050,
            stereo,
        };
        let mut stream = MidiPcmStream::new(config)?;
        stream.load()?;
        let bank = SoundBankCache::new(index(EFFECT_INDEX), index(SAMPLE_INDEX));
        let host = StreamHost::new(stream, index(TRACK_INDEX), index(PATCH_INDEX), bank);
        let mut world = AudioWorld::new(stereo)?;
        world.prefs.ambient = DEFAULT_VOLUME;
        world.prefs.sound_effect = DEFAULT_VOLUME;
        world.prefs.voice_over = DEFAULT_VOLUME;
        world.prefs.music = DEFAULT_VOLUME;
        let len = world.player.block_len();
        Ok(Self {
            world,
            host,
            effects: Effects {
                index: index(EFFECT_INDEX),
                synth: Synth::new(),
                cache: HashMap::new(),
            },
            store,
            stereo,
            mix: vec![0; len],
            base: (0, 0),
        })
    }

    pub fn set_volumes(&mut self, ambient: i32, effects: i32, voice: i32, music: i32) {
        let prefs = &mut self.world.prefs;
        prefs.ambient = ambient.clamp(0, 255);
        prefs.sound_effect = effects.clamp(0, 255);
        prefs.voice_over = voice.clamp(0, 255);
        prefs.music = music.clamp(0, 255);
    }

    #[allow(clippy::too_many_arguments)]
    pub fn set_listener(
        &mut self,
        plane: i32,
        x: i32,
        z: i32,
        size: i32,
        camera_roll: i32,
        base_x: i32,
        base_y: i32,
    ) {
        self.base = (base_x, base_y);
        let listener = &mut self.world.listener;
        listener.plane = plane;
        listener.x = x;
        listener.z = z;
        listener.size = size;
        listener.camera_roll = camera_roll;
    }

    pub fn needs(&mut self) -> Vec<i32> {
        self.store
            .borrow_mut()
            .wanted
            .drain(..)
            .flat_map(|(index, archive)| [index, archive])
            .collect()
    }

    pub fn add_file(&mut self, index: i32, archive: i32, file: i32, data: &[u8]) {
        self.store
            .borrow_mut()
            .files
            .insert((index, archive, file), data.to_vec());
    }

    pub fn finish_group(&mut self, index: i32, archive: i32) {
        let mut store = self.store.borrow_mut();
        store.done.insert((index, archive));
        store.asked.remove(&(index, archive));
    }

    pub fn feed(&mut self, events: &[i32]) -> usize {
        let mut at = 0usize;
        let mut applied = 0usize;
        while at < events.len() {
            let kind = events[at];
            match kind {
                MISC => {
                    let Some(len) = events.get(at + 2).copied().filter(|len| *len >= 0) else {
                        break;
                    };
                    let end = at + 3 + len as usize;
                    if end > events.len() {
                        break;
                    }
                    let payload: Vec<u8> = events[at + 3..end].iter().map(|v| *v as u8).collect();
                    if self.misc(events[at + 1], &payload) {
                        applied += 1;
                    }
                    at = end;
                }
                SOUND_AREA if at + 10 <= events.len() => {
                    let e = &events[at..at + 10];
                    let update = ZoneUpdate::SoundArea {
                        tile: Tile { x: 0, y: 0 },
                        sound: e[4],
                        flags: e[5] as u8,
                        delay: e[6] as u8,
                        volume: e[7] as u8,
                        sample_rate: e[8] as u16,
                        voice_effect: e[9] != 0,
                    };
                    applied += usize::from(self.zone(e[1], e[2], e[3], &update));
                    at += 10;
                }
                MIDI_SONG_LOCATION if at + 9 <= events.len() => {
                    let e = &events[at..at + 9];
                    let update = ZoneUpdate::MidiSongLocation {
                        tile: Tile { x: 0, y: 0 },
                        sound: e[4],
                        flags: e[5] as u8,
                        delay: e[6] as u8,
                        volume: e[7] as u8,
                        rate: e[8] as u16,
                    };
                    applied += usize::from(self.zone(e[1], e[2], e[3], &update));
                    at += 9;
                }
                _ => break,
            }
        }
        applied
    }

    fn zone(&mut self, x: i32, y: i32, plane: i32, update: &ZoneUpdate) -> bool {
        if let ZoneUpdate::MidiSongLocation { sound, .. } = update {
            self.effects.index.file(*sound, 0);
        }
        let base = ZoneBase { x, y, plane };
        self.world
            .apply_zone(update, &base, &|_, _, _| true)
            == crate::world::Handled::Applied
    }

    fn misc(&mut self, opcode: i32, payload: &[u8]) -> bool {
        let Some(packet) = u16::try_from(opcode)
            .ok()
            .and_then(<ServerPacket as openrs_protocol::registry::GameOpcode>::from_wire)
        else {
            return false;
        };
        let Ok(WorldPacket::Misc(misc)) = decode(packet, payload) else {
            return false;
        };
        if let openrs_login::world_packets::misc::Misc::SoundSynth { id, .. } = &misc {
            self.effects.index.file(*id, 0);
        }
        let host: &mut dyn MidiHost = &mut self.host;
        let (handled, _) = self.world.apply_misc(&misc, host);
        handled == crate::world::Handled::Applied
    }

    pub fn cycle(&mut self) -> R<usize> {
        let host: &mut dyn MidiHost = &mut self.host;
        self.world.song.update_fade(host);
        self.world.song.load_pending(host);
        if let Some(range) = self.world.song.primary {
            let dx = self.world.listener.x.wrapping_add(self.base.0 << 9) as f32 - range.x as f32;
            let dz = self.world.listener.z.wrapping_add(self.base.1 << 9) as f32 - range.z as f32;
            let multiplier = spatial_music_multiplier((dx * dx + dz * dz).sqrt(), range.near, range.far);
            self.host.stream.set_volume_multiplier(multiplier);
        }
        Ok(self.world.tick(&mut self.effects)?.len())
    }

    pub fn render(&mut self, blocks: usize) -> R<Vec<i16>> {
        let blocks = blocks.min(MAX_BLOCKS);
        let mut pcm = Vec::with_capacity(self.mix.len() * blocks);
        for _ in 0..blocks {
            self.world.player.fill_block(&mut self.mix)?;
            self.host
                .stream
                .fill(&mut self.mix, 0, BLOCK_FRAMES as i32)?;
            pcm.extend(self.mix.iter().map(|v| clip_sample(*v)));
        }
        Ok(pcm)
    }

    pub fn channels(&self) -> usize {
        if self.stereo {
            2
        } else {
            1
        }
    }

    pub fn music_volume(&self) -> i32 {
        self.host.music_volume()
    }

    pub fn voices(&self) -> usize {
        self.world.player.voice_count()
    }
}

#[cfg(target_arch = "wasm32")]
mod bindings {
    use super::Engine;
    use wasm_bindgen::prelude::*;

    fn fail<E: std::fmt::Debug>(error: E) -> JsValue {
        JsValue::from_str(&format!("{error:?}"))
    }

    #[wasm_bindgen(js_name = AudioEngine)]
    pub struct AudioEngine(Engine);

    #[wasm_bindgen(js_class = AudioEngine)]
    impl AudioEngine {
        #[wasm_bindgen(constructor)]
        pub fn new(stereo: bool) -> Result<AudioEngine, JsValue> {
            Engine::new(stereo).map(AudioEngine).map_err(fail)
        }

        #[wasm_bindgen(js_name = setVolumes)]
        pub fn set_volumes(&mut self, ambient: i32, effects: i32, voice: i32, music: i32) {
            self.0.set_volumes(ambient, effects, voice, music);
        }

        #[wasm_bindgen(js_name = setListener)]
        #[allow(clippy::too_many_arguments)]
        pub fn set_listener(
            &mut self,
            plane: i32,
            x: i32,
            z: i32,
            size: i32,
            camera_roll: i32,
            base_x: i32,
            base_y: i32,
        ) {
            self.0.set_listener(plane, x, z, size, camera_roll, base_x, base_y);
        }

        pub fn needs(&mut self) -> Vec<i32> {
            self.0.needs()
        }

        #[wasm_bindgen(js_name = addFile)]
        pub fn add_file(&mut self, index: i32, archive: i32, file: i32, data: &[u8]) {
            self.0.add_file(index, archive, file, data);
        }

        #[wasm_bindgen(js_name = finishGroup)]
        pub fn finish_group(&mut self, index: i32, archive: i32) {
            self.0.finish_group(index, archive);
        }

        pub fn feed(&mut self, events: &[i32]) -> usize {
            self.0.feed(events)
        }

        pub fn cycle(&mut self) -> Result<usize, JsValue> {
            self.0.cycle().map_err(fail)
        }

        pub fn render(&mut self, blocks: usize) -> Result<Vec<i16>, JsValue> {
            self.0.render(blocks).map_err(fail)
        }

        pub fn channels(&self) -> usize {
            self.0.channels()
        }

        #[wasm_bindgen(js_name = musicVolume)]
        pub fn music_volume(&self) -> i32 {
            self.0.music_volume()
        }

        pub fn voices(&self) -> usize {
            self.0.voices()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn midi_song_location_requests_the_effect_then_renders_silence_until_loaded() {
        let mut engine = Engine::new(true).unwrap();
        engine.set_listener(0, 3200, 3200, 1, 0, 0, 0);
        let events = [MIDI_SONG_LOCATION, 3200, 3200, 0, 7, 0x11, 0, 100, 22050];
        assert_eq!(engine.feed(&events), 1);
        engine.cycle().unwrap();
        assert_eq!(engine.needs(), vec![EFFECT_INDEX, 7]);
        assert!(engine.needs().is_empty());
        let pcm = engine.render(4).unwrap();
        assert_eq!(pcm.len(), 4 * BLOCK_FRAMES * 2);
        assert!(pcm.iter().all(|v| *v == 0));
        engine.finish_group(EFFECT_INDEX, 7);
        engine.cycle().unwrap();
        assert!(engine.needs().is_empty());
    }

    #[test]
    fn truncated_events_are_ignored() {
        let mut engine = Engine::new(false).unwrap();
        assert_eq!(engine.feed(&[MISC, 1, 5, 0]), 0);
        assert_eq!(engine.feed(&[SOUND_AREA, 1]), 0);
        assert_eq!(engine.feed(&[99]), 0);
        assert_eq!(engine.channels(), 1);
    }
}
