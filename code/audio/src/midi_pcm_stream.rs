use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use crate::audio_file::AudioFile;
use crate::music_patch::{MusicPatch, PatchInfo, SoundBank};
use crate::music_track::{MidiFileReader, MusicTrack};
use crate::pcm_stream_mixer::{PcmStreamMixer, Voice};
use crate::reader::{at, AudioError, R};

const LOOP_GUARD: u32 = 1 << 22;

pub type SharedPatches = Arc<Mutex<HashMap<i64, MusicPatch>>>;

pub trait PatchSource {
    fn get_file(&mut self, id: i32) -> Option<Vec<u8>>;
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct MidiConfig {
    pub sample_rate: i32,
    pub stereo: bool,
}

fn div(numerator: i32, denominator: i32) -> R<i32> {
    if denominator == 0 {
        return Err(AudioError::Invalid);
    }
    Ok(numerator.wrapping_div(denominator))
}

fn div_long(numerator: i64, denominator: i64) -> R<i64> {
    if denominator == 0 {
        return Err(AudioError::Invalid);
    }
    Ok(numerator.wrapping_div(denominator))
}

fn byte(values: &[i8], index: i32) -> R<i32> {
    Ok(i32::from(at(values, i64::from(index))?))
}

fn ubyte(values: &[i8], index: i32) -> R<i32> {
    Ok(byte(values, index)? & 0xff)
}

fn pow(base: f64, exponent: f64) -> f64 {
    libm::pow(base, exponent)
}

struct PatchNode {
    loop_variable: i32,
    current_note_pitch: i32,
    max_volume_level: i32,
    vibrato_phase: i32,
    current_pan_value: i32,
    note_pitch_adjustment: i32,
    portamento_pitch_offset: i32,
    portamento_factor: i32,
    volume_envelope_time: i32,
    volume_envelope_position: i32,
    volume_envelope_index: i32,
    release_state: i32,
    release_envelope_index: i32,
    tick_counter: i32,
    current_track: i32,
    retrigger_phase: i32,
    tick_sample_length: i32,
    pitch_offset: Option<[i16; 128]>,
    audio_file: Option<Arc<AudioFile>>,
    info: Option<PatchInfo>,
    stream: Option<Voice>,
}

impl PatchNode {
    fn clear(&mut self) {
        self.pitch_offset = None;
        self.audio_file = None;
        self.info = None;
        self.stream = None;
    }

    fn loops_forever(&self) -> R<bool> {
        let offsets = self.pitch_offset.as_ref().ok_or(AudioError::Invalid)?;
        Ok(at(offsets, i64::from(self.current_note_pitch))? < 0)
    }
}

pub struct MidiPcmStream {
    pub music_volume: i32,
    pub volume_multiplier: i32,
    pub ticks: i32,
    pub channel_volume: [i32; 16],
    pub volume: [i32; 16],
    pub pan: [i32; 16],
    pub expression: [i32; 16],
    pub default_program: [i32; 16],
    pub program_change: [i32; 16],
    pub bank_select: [i32; 16],
    pub pitch_bend: [i32; 16],
    pub modulation: [i32; 16],
    pub portamento: [i32; 16],
    pub sustain: [i32; 16],
    pub non_registered_parameter: [i32; 16],
    pub data_entry: [i32; 16],
    pub general_purpose1: [i32; 16],
    pub effect_speed_raw: [i32; 16],
    pub effect_speed_scaled: [i32; 16],
    pub reader: MidiFileReader,
    pub current_tick_time: i64,
    pub prioritized_track: i32,
    pub prioritized_track_length: i32,
    pub next_event_tick_time: i64,
    pub is_looping: bool,
    config: MidiConfig,
    patches: SharedPatches,
    nodes: Vec<Option<PatchNode>>,
    queue: Vec<usize>,
    active_notes: [[Option<usize>; 128]; 16],
    loop_group_notes: [[Option<usize>; 128]; 16],
    fade_out: PcmStreamMixer,
}

impl MidiPcmStream {
    pub fn new(config: MidiConfig) -> R<Self> {
        Self::with_patches(Arc::new(Mutex::new(HashMap::new())), config)
    }

    pub fn with_patches(patches: SharedPatches, config: MidiConfig) -> R<Self> {
        if config.sample_rate < 100 {
            return Err(AudioError::Invalid);
        }
        let mut stream = Self {
            music_volume: 256,
            volume_multiplier: 256,
            ticks: 1_000_000,
            channel_volume: [0; 16],
            volume: [0; 16],
            pan: [0; 16],
            expression: [0; 16],
            default_program: [0; 16],
            program_change: [0; 16],
            bank_select: [0; 16],
            pitch_bend: [0; 16],
            modulation: [0; 16],
            portamento: [0; 16],
            sustain: [0; 16],
            non_registered_parameter: [0; 16],
            data_entry: [0; 16],
            general_purpose1: [0; 16],
            effect_speed_raw: [0; 16],
            effect_speed_scaled: [0; 16],
            reader: MidiFileReader::empty(),
            current_tick_time: 0,
            prioritized_track: 0,
            prioritized_track_length: 0,
            next_event_tick_time: 0,
            is_looping: false,
            config,
            patches,
            nodes: Vec::new(),
            queue: Vec::new(),
            active_notes: [[None; 128]; 16],
            loop_group_notes: [[None; 128]; 16],
            fade_out: PcmStreamMixer::new(),
        };
        stream.set_channel_volume(-1, 256);
        stream.send_system_reset_message(true)?;
        Ok(stream)
    }

    pub fn patches(&self) -> SharedPatches {
        self.patches.clone()
    }

    pub fn config(&self) -> MidiConfig {
        self.config
    }

    pub fn set_pitch_bend(&mut self, index: usize, value: i32) {
        if let Some(slot) = self.pitch_bend.get_mut(index) {
            *slot = value;
        }
    }

    pub fn set_volume_multiplier(&mut self, volume_multiplier: i32) {
        self.volume_multiplier = volume_multiplier;
    }

    pub fn set_channel_volume(&mut self, channel: i32, volume: i32) {
        if channel < 0 {
            self.channel_volume = [volume; 16];
        } else if let Some(slot) = self.channel_volume.get_mut(channel as usize) {
            *slot = volume;
        }
    }

    pub fn set_music_volume(&mut self, volume: i32) {
        self.music_volume = volume;
    }

    pub fn get_music_volume(&self) -> i32 {
        self.music_volume
    }

    pub fn load(&mut self) -> R<()> {
        self.init_drum_channel();
        Ok(())
    }

    pub fn data_loaded(&self) -> bool {
        self.reader.data_loaded()
    }

    pub fn active_node_count(&self) -> usize {
        self.queue.len()
    }

    pub fn fading_count(&self) -> usize {
        self.fade_out.len()
    }

    pub fn reset_music_patches(&mut self) {
        let mut patches = self.patches.lock().unwrap_or_else(|e| e.into_inner());
        for patch in patches.values_mut() {
            patch.reset();
        }
    }

    pub fn unlink_music_patches(&mut self) {
        self.patches
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .clear();
    }

    pub fn set_music_track(&mut self, track: &MusicTrack, looping: bool) -> R<()> {
        self.load_and_play_track(track, looping, true)
    }

    pub fn set_music_track_at_position(
        &mut self,
        track: &MusicTrack,
        looping: bool,
        tick_position: i64,
    ) -> R<()> {
        self.load_and_play_track(track, looping, true)?;
        self.advance_to_tick(i64::from(self.reader.division).wrapping_mul(tick_position))
    }

    pub fn turn_off_sound(&mut self) -> R<()> {
        self.clear_track(true)
    }

    pub fn clear_track(&mut self, all_sound_off: bool) -> R<()> {
        self.reader.clear();
        self.send_system_reset_message(all_sound_off)
    }

    pub fn load_and_play_track(
        &mut self,
        track: &MusicTrack,
        looping: bool,
        all_sound_off: bool,
    ) -> R<()> {
        self.clear_track(all_sound_off)?;
        self.reader = match MidiFileReader::new(&track.midi) {
            Ok(reader) => reader,
            Err(error) => {
                self.reader = MidiFileReader::empty();
                return Err(error);
            }
        };
        self.is_looping = looping;
        self.current_tick_time = 0;
        let count = self.reader.track_count();
        for index in 0..count {
            self.reader.goto_track(index)?;
            self.reader.track_length(index)?;
            self.reader.mark_position(index)?;
        }
        self.prioritized_track = self.reader.prioritized_track();
        self.prioritized_track_length = self.track_length_of(self.prioritized_track)?;
        self.next_event_tick_time = self
            .reader
            .tick_timestamp(self.prioritized_track_length);
        Ok(())
    }

    fn track_length_of(&self, track: i32) -> R<i32> {
        if self.reader.track_lengths.is_empty() {
            Ok(0)
        } else {
            at(&self.reader.track_lengths, i64::from(track))
        }
    }

    fn track_has_length(&self, track: i32, length: i32) -> R<bool> {
        if self.reader.track_lengths.is_empty() {
            Ok(false)
        } else {
            Ok(length == at(&self.reader.track_lengths, i64::from(track))?)
        }
    }

    pub fn ready(
        &mut self,
        track: &mut MusicTrack,
        source: &mut dyn PatchSource,
        bank: &mut dyn SoundBank,
    ) -> R<bool> {
        track.build_patch_table()?;
        let mut is_ready = true;
        let mut rates = [22050i32];
        let entries: Vec<(i64, [u8; 128])> = match &track.table {
            Some(table) => table.entries().map(|(key, flags)| (key, *flags)).collect(),
            None => Vec::new(),
        };
        for (pointer, flags) in entries {
            let key = i64::from(pointer as i32);
            let mut patches = self.patches.lock().unwrap_or_else(|e| e.into_inner());
            if let std::collections::hash_map::Entry::Vacant(slot) = patches.entry(key) {
                let Some(data) = source.get_file(key as i32) else {
                    is_ready = false;
                    continue;
                };
                slot.insert(MusicPatch::new(&data)?);
            }
            if let Some(patch) = patches.get_mut(&key) {
                if !patch.ready(bank, Some(&flags), &mut rates) {
                    is_ready = false;
                }
            }
        }
        if is_ready {
            track.clear();
        }
        Ok(is_ready)
    }

    pub fn fill(&mut self, mix: &mut [i32], offset: i32, length: i32) -> R<()> {
        let mut output_offset = offset;
        let mut remaining = length;
        if self.reader.data_loaded() {
            let ticks_per_sample = div(
                self.reader.division.wrapping_mul(self.ticks),
                self.config.sample_rate,
            )?;
            let per = i64::from(ticks_per_sample);
            let mut guard = 0u32;
            loop {
                guard += 1;
                if guard > LOOP_GUARD {
                    return Err(AudioError::Invalid);
                }
                let projected = per
                    .wrapping_mul(i64::from(remaining))
                    .wrapping_add(self.current_tick_time);
                if self.next_event_tick_time.wrapping_sub(projected) >= 0 {
                    self.current_tick_time = projected;
                    break;
                }
                let samples = div_long(
                    self.next_event_tick_time
                        .wrapping_sub(self.current_tick_time)
                        .wrapping_add(per)
                        .wrapping_sub(1),
                    per,
                )? as i32;
                self.current_tick_time = self
                    .current_tick_time
                    .wrapping_add(i64::from(samples).wrapping_mul(per));
                self.patch_fill(mix, output_offset, samples)?;
                output_offset = output_offset.wrapping_add(samples);
                remaining = remaining.wrapping_sub(samples);
                self.process_next_midi_events()?;
                if !self.reader.data_loaded() {
                    break;
                }
            }
        }
        self.patch_fill(mix, output_offset, remaining)
    }

    pub fn skip(&mut self, sample_count: i32) -> R<()> {
        let mut remaining = sample_count;
        if self.reader.data_loaded() {
            let ticks_per_sample = div(
                self.reader.division.wrapping_mul(self.ticks),
                self.config.sample_rate,
            )?;
            let per = i64::from(ticks_per_sample);
            let mut guard = 0u32;
            loop {
                guard += 1;
                if guard > LOOP_GUARD {
                    return Err(AudioError::Invalid);
                }
                let projected = i64::from(remaining)
                    .wrapping_mul(per)
                    .wrapping_add(self.current_tick_time);
                if self.next_event_tick_time.wrapping_sub(projected) >= 0 {
                    self.current_tick_time = projected;
                    break;
                }
                let samples = div_long(
                    self.next_event_tick_time
                        .wrapping_sub(self.current_tick_time)
                        .wrapping_add(per)
                        .wrapping_sub(1),
                    per,
                )? as i32;
                self.current_tick_time = self
                    .current_tick_time
                    .wrapping_add(i64::from(samples).wrapping_mul(per));
                self.patch_skip(samples)?;
                remaining = remaining.wrapping_sub(samples);
                self.process_next_midi_events()?;
                if !self.reader.data_loaded() {
                    break;
                }
            }
        }
        self.patch_skip(remaining)
    }

    pub fn advance_to_tick(&mut self, target_tick: i64) -> R<()> {
        let mut guard = 0u32;
        while self.next_event_tick_time <= target_tick {
            let mut track = self.prioritized_track;
            let mut length = self.prioritized_track_length;
            let mut tick_time = self.next_event_tick_time;
            loop {
                guard += 1;
                if guard > LOOP_GUARD {
                    return Err(AudioError::Invalid);
                }
                if length != self.prioritized_track_length {
                    break;
                }
                while self.track_has_length(track, length)? {
                    self.reader.goto_track(track)?;
                    let flags = self.reader.read_message(track)?;
                    if flags == 1 {
                        self.reader.set_track_done();
                        self.reader.mark_position(track)?;
                        if self.reader.is_done() {
                            if !self.is_looping || length == 0 {
                                self.send_system_reset_message(true)?;
                                self.reader.clear();
                                return Ok(());
                            }
                            self.reader.reset_from_timestamp(tick_time)?;
                        }
                        break;
                    }
                    if flags & 0x80 != 0 && flags & 0xf0 != 144 {
                        self.parse_message(flags)?;
                    }
                    self.reader.track_length(track)?;
                    self.reader.mark_position(track)?;
                }
                self.current_tick_time = tick_time;
                track = self.reader.prioritized_track();
                length = self.track_length_of(track)?;
                tick_time = self.reader.tick_timestamp(length);
            }
            self.prioritized_track = track;
            self.prioritized_track_length = length;
            self.next_event_tick_time = tick_time;
        }
        Ok(())
    }

    pub fn process_next_midi_events(&mut self) -> R<()> {
        let mut track = self.prioritized_track;
        let mut length = self.prioritized_track_length;
        let mut tick_time = self.next_event_tick_time;
        let mut guard = 0u32;
        loop {
            guard += 1;
            if guard > LOOP_GUARD {
                return Err(AudioError::Invalid);
            }
            if length != self.prioritized_track_length {
                self.prioritized_track = track;
                self.prioritized_track_length = length;
                self.next_event_tick_time = tick_time;
                return Ok(());
            }
            while self.track_has_length(track, length)? {
                self.reader.goto_track(track)?;
                let message = self.reader.read_message(track)?;
                if message == 1 {
                    self.reader.set_track_done();
                    self.reader.mark_position(track)?;
                    if self.reader.is_done() {
                        if !self.is_looping || length == 0 {
                            self.send_system_reset_message(true)?;
                            self.reader.clear();
                            return Ok(());
                        }
                        self.reader.reset_from_timestamp(tick_time)?;
                    }
                    break;
                }
                if message & 0x80 != 0 {
                    self.parse_message(message)?;
                }
                self.reader.track_length(track)?;
                self.reader.mark_position(track)?;
            }
            track = self.reader.prioritized_track();
            length = self.track_length_of(track)?;
            tick_time = self.reader.tick_timestamp(length);
        }
    }

    pub fn set_effect_speed(&mut self, channel: usize, raw_speed: i32) {
        self.effect_speed_raw[channel] = raw_speed;
        self.effect_speed_scaled[channel] =
            (2097152.0 * pow(2.0, f64::from(raw_speed) * 5.4931640625E-4) + 0.5) as i32;
    }

    pub fn init_drum_channel(&mut self) {
        self.default_program[9] = 128;
        self.bank_select[9] = 128;
        self.set_program_change(9, 128);
    }

    fn set_program_change(&mut self, channel: usize, program: i32) {
        if program != self.program_change[channel] {
            self.program_change[channel] = program;
            self.loop_group_notes[channel] = [None; 128];
        }
    }

    fn unlink(&mut self, id: usize) {
        self.queue.retain(|queued| *queued != id);
    }

    fn collect_garbage(&mut self) {
        if self.nodes.len() < 1024 || self.nodes.len() < 4 * (self.queue.len() + 1) {
            return;
        }
        let mut keep = vec![false; self.nodes.len()];
        for id in &self.queue {
            keep[*id] = true;
        }
        for row in self.active_notes.iter().chain(self.loop_group_notes.iter()) {
            for id in row.iter().flatten() {
                keep[*id] = true;
            }
        }
        let mut remap = vec![usize::MAX; self.nodes.len()];
        let mut compact: Vec<Option<PatchNode>> = Vec::new();
        for (old, slot) in std::mem::take(&mut self.nodes).into_iter().enumerate() {
            if keep[old] {
                remap[old] = compact.len();
                compact.push(slot);
            }
        }
        self.nodes = compact;
        for id in &mut self.queue {
            *id = remap[*id];
        }
        for row in self
            .active_notes
            .iter_mut()
            .chain(self.loop_group_notes.iter_mut())
        {
            for id in row.iter_mut().flatten() {
                *id = remap[*id];
            }
        }
    }

    fn node(&self, id: usize) -> R<&PatchNode> {
        self.nodes
            .get(id)
            .and_then(|slot| slot.as_ref())
            .ok_or(AudioError::Invalid)
    }

    fn node_mut(&mut self, id: usize) -> R<&mut PatchNode> {
        self.nodes
            .get_mut(id)
            .and_then(|slot| slot.as_mut())
            .ok_or(AudioError::Invalid)
    }

    fn apply_initial_sample_offset(&self, node: &mut PatchNode, reverse_loop: bool) {
        let Some(file) = node.audio_file.clone() else {
            return;
        };
        let Some(stream) = node.stream.as_mut() else {
            return;
        };
        let channel = node.current_track as usize;
        let mut samples = total_samples(&file);
        let mut sample_offset: i32;
        if reverse_loop && file.enable_loop() {
            let bounce = samples.wrapping_add(samples).wrapping_sub(file.start());
            sample_offset =
                ((i64::from(self.general_purpose1[channel]).wrapping_mul(i64::from(bounce))) >> 6)
                    as i32;
            samples = samples.wrapping_shl(8);
            if sample_offset >= samples {
                sample_offset = samples
                    .wrapping_add(samples)
                    .wrapping_sub(1)
                    .wrapping_sub(sample_offset);
                stream.reverse_playback_direction();
            }
        } else {
            sample_offset =
                ((i64::from(self.general_purpose1[channel]).wrapping_mul(i64::from(samples))) >> 6)
                    as i32;
        }
        stream.set_playback_position(sample_offset);
    }

    fn set_note_on(&mut self, channel: usize, note: usize, velocity: i32) -> R<()> {
        self.set_note_off(channel, note)?;
        if self.sustain[channel] & 0x2 != 0 {
            let snapshot: Vec<usize> = self.queue.iter().rev().copied().collect();
            for id in snapshot {
                let (track, release) = {
                    let node = self.node(id)?;
                    (node.current_track, node.release_state)
                };
                if track == channel as i32 && release < 0 {
                    let old_pitch = self.node(id)?.current_note_pitch as usize;
                    self.active_notes[channel][old_pitch] = None;
                    self.active_notes[channel][note] = Some(id);
                    let node = self.node_mut(id)?;
                    let portamento_pitch = (node
                        .portamento_factor
                        .wrapping_mul(node.portamento_pitch_offset)
                        >> 12)
                        .wrapping_add(node.note_pitch_adjustment);
                    node.note_pitch_adjustment = node
                        .note_pitch_adjustment
                        .wrapping_add((note as i32 - node.current_note_pitch).wrapping_shl(8));
                    node.portamento_pitch_offset =
                        portamento_pitch.wrapping_sub(node.note_pitch_adjustment);
                    node.portamento_factor = 4096;
                    node.current_note_pitch = note as i32;
                    return Ok(());
                }
            }
        }
        self.collect_garbage();

        let program = i64::from(self.program_change[channel]);
        let prepared = {
            let patches = self.patches.lock().unwrap_or_else(|e| e.into_inner());
            match patches.get(&program) {
                None => None,
                Some(patch) => patch
                    .audio_files
                    .get(note)
                    .and_then(|f| f.clone())
                    .map(|file| {
                        (
                            file,
                            patch.info_of_key[note].and_then(|i| patch.infos.get(i).cloned()),
                            i32::from(patch.loop_mode[note]),
                            i32::from(patch.volume_offset[note]),
                            i32::from(patch.pan_offset[note]),
                            patch.pitch_offset,
                            patch.base_velocity,
                        )
                    }),
            }
        };
        let Some((file, info, loop_mode, volume_offset, pan_offset, pitch_offset, base_velocity)) =
            prepared
        else {
            return Ok(());
        };
        let pitch_here = i32::from(pitch_offset[note]);
        let mut node = PatchNode {
            loop_variable: loop_mode,
            current_note_pitch: note as i32,
            max_volume_level: velocity
                .wrapping_mul(velocity)
                .wrapping_mul(volume_offset)
                .wrapping_mul(base_velocity)
                .wrapping_add(1024)
                >> 11,
            vibrato_phase: 0,
            current_pan_value: pan_offset & 0xff,
            note_pitch_adjustment: ((note as i32) << 8).wrapping_sub(pitch_here & 0x7fff),
            portamento_pitch_offset: 0,
            portamento_factor: 0,
            volume_envelope_time: 0,
            volume_envelope_position: 0,
            volume_envelope_index: 0,
            release_state: -1,
            release_envelope_index: 0,
            tick_counter: 0,
            current_track: channel as i32,
            retrigger_phase: 0,
            tick_sample_length: 0,
            pitch_offset: Some(pitch_offset),
            audio_file: Some(file.clone()),
            info,
            stream: None,
        };
        if self.general_purpose1[channel] == 0 {
            let pitch = self.calculate_pitch(&node)?;
            let volume = self.calculate_volume(&node)?;
            let pan = self.calculate_pan(&node);
            node.stream = Some(Voice::from_file(&file, pitch, volume, pan));
        } else {
            let pitch = self.calculate_pitch(&node)?;
            let pan = self.calculate_pan(&node);
            node.stream = Some(Voice::from_file(&file, pitch, 0, pan));
            self.apply_initial_sample_offset(&mut node, pitch_here < 0);
        }
        if pitch_here < 0 {
            if let Some(stream) = node.stream.as_mut() {
                stream.set_num_loops(-1);
            }
        }
        let id = self.nodes.len();
        if node.loop_variable >= 0 {
            let group = node.loop_variable as usize;
            if let Some(existing) = self.loop_group_notes[channel][group] {
                let (release, pitch) = {
                    let other = self.node(existing)?;
                    (other.release_state, other.current_note_pitch as usize)
                };
                if release < 0 {
                    self.active_notes[channel][pitch] = None;
                    self.node_mut(existing)?.release_state = 0;
                }
            }
            self.loop_group_notes[channel][group] = Some(id);
        }
        self.nodes.push(Some(node));
        self.queue.push(id);
        self.active_notes[channel][note] = Some(id);
        Ok(())
    }

    fn calculate_volume(&self, node: &PatchNode) -> R<i32> {
        let channel = node.current_track as usize;
        if self.channel_volume[channel] == 0 {
            return Ok(0);
        }
        let Some(info) = node.info.as_ref() else {
            return Ok(0);
        };
        let mut vol = (self.expression[channel]
            .wrapping_mul(self.volume[channel])
            .wrapping_add(4096))
            >> 13;
        vol = (vol.wrapping_mul(vol).wrapping_add(16384)) >> 15;
        vol = (vol.wrapping_mul(node.max_volume_level).wrapping_add(16384)) >> 15;
        vol = (vol.wrapping_mul(self.music_volume).wrapping_add(128)) >> 8;
        vol = vol.wrapping_mul(self.volume_multiplier) >> 8;
        vol = (vol.wrapping_mul(self.channel_volume[channel]).wrapping_add(128)) >> 8;
        if info.volume_envelope_decay > 0 {
            vol = (f64::from(vol)
                * pow(
                    0.5,
                    f64::from(node.volume_envelope_time)
                        * 1.953125E-5
                        * f64::from(info.volume_envelope_decay),
                )
                + 0.5) as i32;
        }
        if let Some(data) = info.volume_envelope.as_deref() {
            let index = node.volume_envelope_index;
            let position = node.volume_envelope_position;
            let mut value = ubyte(data, index.wrapping_add(1))?;
            if index < (data.len() as i32).wrapping_sub(2) {
                let start = ubyte(data, index)?.wrapping_shl(8);
                let end = ubyte(data, index.wrapping_add(2))?.wrapping_shl(8);
                let next = byte(data, index.wrapping_add(3))?;
                value = value.wrapping_add(div(
                    position
                        .wrapping_sub(start)
                        .wrapping_mul(next.wrapping_sub(value)),
                    end.wrapping_sub(start),
                )?);
            }
            vol = (vol.wrapping_mul(value).wrapping_add(32)) >> 6;
        }
        if let Some(data) = info.release_envelope.as_deref() {
            if node.release_state > 0 {
                let index = node.release_envelope_index;
                let position = node.release_state;
                let mut value = ubyte(data, index.wrapping_add(1))?;
                if index < (data.len() as i32).wrapping_sub(2) {
                    let start = ubyte(data, index)?.wrapping_shl(8);
                    let end = ubyte(data, index.wrapping_add(2))?.wrapping_shl(8);
                    let next = byte(data, index.wrapping_add(3))?;
                    value = value.wrapping_add(div(
                        next.wrapping_sub(value)
                            .wrapping_mul(position.wrapping_sub(start)),
                        end.wrapping_sub(start),
                    )?);
                }
                vol = (value.wrapping_mul(vol).wrapping_add(32)) >> 6;
            }
        }
        Ok(vol)
    }

    fn set_note_off(&mut self, channel: usize, note: usize) -> R<()> {
        let Some(active) = self.active_notes[channel][note] else {
            return Ok(());
        };
        self.active_notes[channel][note] = None;
        if self.sustain[channel] & 0x2 != 0 {
            let snapshot = self.queue.clone();
            let active_track = self.node(active)?.current_track;
            for id in snapshot {
                let streamed = self.node(id)?;
                if active_track == streamed.current_track && streamed.release_state < 0 && id != active
                {
                    self.node_mut(active)?.release_state = 0;
                    break;
                }
            }
        } else {
            self.node_mut(active)?.release_state = 0;
        }
        Ok(())
    }

    fn turn_all_sound_off(&mut self, channel: i32) -> R<()> {
        let snapshot = self.queue.clone();
        for id in snapshot {
            let matches = {
                let node = self.node(id)?;
                channel < 0 || node.current_track == channel
            };
            if !matches {
                continue;
            }
            let (stream, release, track, pitch) = {
                let node = self.node_mut(id)?;
                let stream = node.stream.take();
                node.clear();
                (
                    stream,
                    node.release_state,
                    node.current_track as usize,
                    node.current_note_pitch as usize,
                )
            };
            if let Some(mut stream) = stream {
                stream.fade_out_and_stop(self.config.sample_rate / 100)?;
                if stream.is_fading() {
                    self.fade_out.add_sub_stream(stream);
                }
            }
            if release < 0 {
                self.active_notes[track][pitch] = None;
            }
            self.unlink(id);
        }
        Ok(())
    }

    fn turn_all_notes_off(&mut self, channel: i32) -> R<()> {
        let snapshot = self.queue.clone();
        for id in snapshot {
            let (matches, track, pitch) = {
                let node = self.node(id)?;
                (
                    (channel < 0 || node.current_track == channel) && node.release_state < 0,
                    node.current_track as usize,
                    node.current_note_pitch as usize,
                )
            };
            if matches {
                self.active_notes[track][pitch] = None;
                self.node_mut(id)?.release_state = 0;
            }
        }
        Ok(())
    }

    fn reset_all_controllers(&mut self, channel: i32) -> R<()> {
        if channel < 0 {
            for index in 0..16 {
                self.reset_all_controllers(index)?;
            }
            return Ok(());
        }
        let ch = channel as usize;
        self.volume[ch] = 12800;
        self.pan[ch] = 8192;
        self.expression[ch] = 16383;
        self.pitch_bend[ch] = 8192;
        self.modulation[ch] = 0;
        self.portamento[ch] = 8192;
        self.reset_sustain_pedal(ch)?;
        self.reset_effect_switch(ch, 41)?;
        self.sustain[ch] = 0;
        self.non_registered_parameter[ch] = 32767;
        self.data_entry[ch] = 256;
        self.general_purpose1[ch] = 0;
        self.set_effect_speed(ch, 8192);
        Ok(())
    }

    fn send_system_reset_message(&mut self, all_sound_off: bool) -> R<()> {
        if all_sound_off {
            self.turn_all_sound_off(-1)?;
        } else {
            self.turn_all_notes_off(-1)?;
        }
        self.reset_all_controllers(-1)?;
        self.program_change = self.default_program;
        for index in 0..16 {
            self.bank_select[index] = self.default_program[index] & -128;
        }
        Ok(())
    }

    fn reset_sustain_pedal(&mut self, index: usize) -> R<()> {
        if self.sustain[index] & 0x2 != 0 {
            let snapshot = self.queue.clone();
            for id in snapshot {
                let (matches, pitch) = {
                    let node = self.node(id)?;
                    (
                        node.current_track == index as i32 && node.release_state < 0,
                        node.current_note_pitch as usize,
                    )
                };
                if matches && self.active_notes[index][pitch].is_none() {
                    self.node_mut(id)?.release_state = 0;
                }
            }
        }
        Ok(())
    }

    fn reset_effect_switch(&mut self, index: usize, threshold: i8) -> R<()> {
        if self.sustain[index] & 0x4 != 0 && threshold > 1 {
            let snapshot = self.queue.clone();
            for id in snapshot {
                let node = self.node_mut(id)?;
                if node.current_track == index as i32 {
                    node.retrigger_phase = 0;
                }
            }
        }
        Ok(())
    }

    pub fn parse_message(&mut self, packed: i32) -> R<()> {
        let message = packed & 0xf0;
        if message == 128 {
            let channel = (packed & 0xf) as usize;
            let data1 = ((packed >> 8) & 0x7f) as usize;
            self.set_note_off(channel, data1)?;
        } else if message == 144 {
            let channel = (packed & 0xf) as usize;
            let data1 = ((packed >> 8) & 0x7f) as usize;
            let data2 = (packed >> 16) & 0x7f;
            if data2 > 0 {
                self.set_note_on(channel, data1, data2)?;
            } else {
                self.set_note_off(channel, data1)?;
            }
        } else if message == 160 {
        } else if message == 176 {
            let channel = (packed & 0xf) as usize;
            let data1 = (packed >> 8) & 0x7f;
            let data2 = (packed >> 16) & 0x7f;
            self.control_change(channel, data1, data2)?;
        } else if message == 192 {
            let channel = (packed & 0xf) as usize;
            let data1 = (packed >> 8) & 0x7f;
            let program = data1.wrapping_add(self.bank_select[channel]);
            self.set_program_change(channel, program);
        } else if message == 208 {
        } else if message == 224 {
            let channel = (packed & 0xf) as usize;
            let value = ((packed >> 8) & 0x7f).wrapping_add((packed >> 9) & 0x3f80);
            self.pitch_bend[channel] = value;
        } else if packed & 0xff == 255 {
            self.send_system_reset_message(true)?;
        }
        Ok(())
    }

    fn control_change(&mut self, channel: usize, data1: i32, data2: i32) -> R<()> {
        let ch = channel;
        if data1 == 0 {
            self.bank_select[ch] = (data2 << 14).wrapping_add(self.bank_select[ch] & -2080769);
        }
        if data1 == 32 {
            self.bank_select[ch] = (data2 << 7).wrapping_add(self.bank_select[ch] & -16257);
        }
        if data1 == 1 {
            self.modulation[ch] = (data2 << 7).wrapping_add(self.modulation[ch] & -16257);
        }
        if data1 == 33 {
            self.modulation[ch] = data2.wrapping_add(self.modulation[ch] & -128);
        }
        if data1 == 5 {
            self.portamento[ch] = (data2 << 7).wrapping_add(self.portamento[ch] & -16257);
        }
        if data1 == 37 {
            self.portamento[ch] = data2.wrapping_add(self.portamento[ch] & -128);
        }
        if data1 == 7 {
            self.volume[ch] = (data2 << 7).wrapping_add(self.volume[ch] & -16257);
        }
        if data1 == 39 {
            self.volume[ch] = data2.wrapping_add(self.volume[ch] & -128);
        }
        if data1 == 10 {
            self.pan[ch] = (data2 << 7).wrapping_add(self.pan[ch] & -16257);
        }
        if data1 == 42 {
            self.pan[ch] = data2.wrapping_add(self.pan[ch] & -128);
        }
        if data1 == 11 {
            self.expression[ch] = (data2 << 7).wrapping_add(self.expression[ch] & -16257);
        }
        if data1 == 43 {
            self.expression[ch] = data2.wrapping_add(self.expression[ch] & -128);
        }
        if data1 == 64 {
            if data2 >= 64 {
                self.sustain[ch] |= 0x1;
            } else {
                self.sustain[ch] &= -2;
            }
        }
        if data1 == 65 {
            if data2 >= 64 {
                self.sustain[ch] |= 0x2;
            } else {
                self.reset_sustain_pedal(ch)?;
                self.sustain[ch] &= -3;
            }
        }
        if data1 == 99 {
            self.non_registered_parameter[ch] =
                (data2 << 7).wrapping_add(self.non_registered_parameter[ch] & 0x7f);
        }
        if data1 == 98 {
            self.non_registered_parameter[ch] =
                (self.non_registered_parameter[ch] & 0x3f80).wrapping_add(data2);
        }
        if data1 == 101 {
            self.non_registered_parameter[ch] = (data2 << 7)
                .wrapping_add(self.non_registered_parameter[ch] & 0x7f)
                .wrapping_add(16384);
        }
        if data1 == 100 {
            self.non_registered_parameter[ch] = (self.non_registered_parameter[ch] & 0x3f80)
                .wrapping_add(data2)
                .wrapping_add(16384);
        }
        if data1 == 120 {
            self.turn_all_sound_off(channel as i32)?;
        }
        if data1 == 121 {
            self.reset_all_controllers(channel as i32)?;
        }
        if data1 == 123 {
            self.turn_all_notes_off(channel as i32)?;
        }
        if data1 == 6 && self.non_registered_parameter[ch] == 16384 {
            self.data_entry[ch] = (data2 << 7).wrapping_add(self.data_entry[ch] & -16257);
        }
        if data1 == 38 && self.non_registered_parameter[ch] == 16384 {
            self.data_entry[ch] = data2.wrapping_add(self.data_entry[ch] & -128);
        }
        if data1 == 16 {
            self.general_purpose1[ch] = (data2 << 7).wrapping_add(self.general_purpose1[ch] & -16257);
        }
        if data1 == 48 {
            self.general_purpose1[ch] = data2.wrapping_add(self.general_purpose1[ch] & -128);
        }
        if data1 == 81 {
            if data2 >= 64 {
                self.sustain[ch] |= 0x4;
            } else {
                self.reset_effect_switch(ch, 97)?;
                self.sustain[ch] &= -5;
            }
        }
        if data1 == 17 {
            let raw = (data2 << 7).wrapping_add(self.effect_speed_raw[ch] & -16257);
            self.set_effect_speed(ch, raw);
        }
        if data1 == 49 {
            let raw = data2.wrapping_add(self.effect_speed_raw[ch] & -128);
            self.set_effect_speed(ch, raw);
        }
        Ok(())
    }

    fn calculate_pitch(&self, node: &PatchNode) -> R<i32> {
        let channel = node.current_track as usize;
        let mut pitch = (node
            .portamento_pitch_offset
            .wrapping_mul(node.portamento_factor)
            >> 12)
            .wrapping_add(node.note_pitch_adjustment);
        pitch = pitch.wrapping_add(
            (self.pitch_bend[channel].wrapping_sub(8192)).wrapping_mul(self.data_entry[channel])
                >> 12,
        );
        if let Some(info) = node.info.as_ref() {
            if info.volume_envelope_release > 0
                && (info.vibrato_lfo_pitch > 0 || self.modulation[channel] > 0)
            {
                let mut scaled = info.vibrato_lfo_pitch.wrapping_shl(2);
                let delay = info.vibrato_delay.wrapping_shl(1);
                if node.tick_counter < delay {
                    scaled = div(scaled.wrapping_mul(node.tick_counter), delay)?;
                }
                scaled = scaled.wrapping_add(self.modulation[channel] >> 7);
                let wave = libm::sin(0.01227184630308513 * f64::from(node.vibrato_phase & 0x1ff));
                pitch = pitch.wrapping_add((wave * f64::from(scaled)) as i32);
            }
        }
        let scaled = match node.audio_file.as_ref() {
            Some(file) => {
                (f64::from(file.sample_rate().wrapping_mul(256))
                    * pow(2.0, 3.255208333333333E-4 * f64::from(pitch))
                    / f64::from(self.config.sample_rate)
                    + 0.5) as i32
            }
            None => 0,
        };
        Ok(scaled.max(1))
    }

    fn calculate_pan(&self, node: &PatchNode) -> i32 {
        let channel_pan = self.pan[node.current_track as usize];
        if channel_pan < 8192 {
            (channel_pan.wrapping_mul(node.current_pan_value).wrapping_add(32)) >> 6
        } else {
            16384i32.wrapping_sub(
                ((128i32.wrapping_sub(node.current_pan_value))
                    .wrapping_mul(16384i32.wrapping_sub(channel_pan))
                    .wrapping_add(32))
                    >> 6,
            )
        }
    }

    fn is_patch_node_dead(&mut self, id: usize, node: &mut PatchNode) -> bool {
        if node.stream.is_none() {
            if node.release_state >= 0 {
                self.unlink(id);
                self.clear_loop_group(id, node);
            }
            true
        } else {
            false
        }
    }

    fn clear_loop_group(&mut self, id: usize, node: &PatchNode) {
        if node.loop_variable > 0 {
            let slot = &mut self.loop_group_notes[node.current_track as usize]
                [node.loop_variable as usize];
            if *slot == Some(id) {
                *slot = None;
            }
        }
    }

    fn update_patch_node(
        &mut self,
        id: usize,
        node: &mut PatchNode,
        mix: Option<&mut [i32]>,
        output_offset: i32,
        sample_length: i32,
    ) -> R<bool> {
        if node.stream.is_none() {
            return Ok(true);
        }
        let rate = self.config.sample_rate;
        node.tick_sample_length = rate / 100;
        let channel = node.current_track as usize;
        let finished = node.stream.as_ref().map(|s| s.is_finished()).unwrap_or(true);
        if node.release_state < 0 || !finished {
            let mut portamento_factor = node.portamento_factor;
            if portamento_factor > 0 {
                portamento_factor = portamento_factor.wrapping_sub(
                    (16.0 * pow(2.0, f64::from(self.portamento[channel]) * 4.921259842519685E-4)
                        + 0.5) as i32,
                );
                if portamento_factor < 0 {
                    portamento_factor = 0;
                }
                node.portamento_factor = portamento_factor;
            }
            let rate_value = self.calculate_pitch(node)?;
            if let Some(stream) = node.stream.as_mut() {
                stream.set_playback_rate(rate_value);
            }
            let Some(info) = node.info.clone() else {
                return Ok(true);
            };
            let mut is_dead = false;
            node.tick_counter = node.tick_counter.wrapping_add(1);
            node.vibrato_phase = node.vibrato_phase.wrapping_add(info.volume_envelope_release);
            let pitch_scalar = 5.086263020833333E-6
                * f64::from(
                    (node.current_note_pitch.wrapping_sub(60).wrapping_shl(8)).wrapping_add(
                        node.portamento_pitch_offset
                            .wrapping_mul(node.portamento_factor)
                            >> 12,
                    ),
                );
            if info.volume_envelope_decay > 0 {
                if info.decay_key_scaling > 0 {
                    node.volume_envelope_time = node.volume_envelope_time.wrapping_add(
                        (128.0 * pow(2.0, pitch_scalar * f64::from(info.decay_key_scaling)) + 0.5)
                            as i32,
                    );
                } else {
                    node.volume_envelope_time = node.volume_envelope_time.wrapping_add(128);
                }
                if info
                    .volume_envelope_decay
                    .wrapping_mul(node.volume_envelope_time)
                    >= 819200
                {
                    is_dead = true;
                }
            }
            if let Some(array) = info.volume_envelope.as_deref() {
                if info.volume_envelope_key_scaling > 0 {
                    node.volume_envelope_position = node.volume_envelope_position.wrapping_add(
                        (128.0
                            * pow(2.0, pitch_scalar * f64::from(info.volume_envelope_key_scaling))
                            + 0.5) as i32,
                    );
                } else {
                    node.volume_envelope_position =
                        node.volume_envelope_position.wrapping_add(128);
                }
                let last = (array.len() as i32).wrapping_sub(2);
                while node.volume_envelope_index < last
                    && node.volume_envelope_position
                        > ubyte(array, node.volume_envelope_index.wrapping_add(2))?.wrapping_shl(8)
                {
                    node.volume_envelope_index = node.volume_envelope_index.wrapping_add(2);
                }
                if last == node.volume_envelope_index
                    && byte(array, node.volume_envelope_index.wrapping_add(1))? == 0
                {
                    is_dead = true;
                }
            }
            if node.release_state >= 0
                && self.sustain[channel] & 0x1 == 0
                && (node.loop_variable < 0
                    || self.loop_group_notes[channel][node.loop_variable as usize] != Some(id))
            {
                if let Some(array) = info.release_envelope.as_deref() {
                    if info.release_envelope_key_scaling > 0 {
                        node.release_state = node.release_state.wrapping_add(
                            (128.0
                                * pow(
                                    2.0,
                                    pitch_scalar * f64::from(info.release_envelope_key_scaling),
                                )
                                + 0.5) as i32,
                        );
                    } else {
                        node.release_state = node.release_state.wrapping_add(128);
                    }
                    let last = (array.len() as i32).wrapping_sub(2);
                    while node.release_envelope_index < last
                        && node.release_state
                            > ubyte(array, node.release_envelope_index.wrapping_add(2))?
                                .wrapping_shl(8)
                    {
                        node.release_envelope_index = node.release_envelope_index.wrapping_add(2);
                    }
                    if last == node.release_envelope_index {
                        is_dead = true;
                    }
                }
            }
            if is_dead {
                let mut stream = node.stream.take().ok_or(AudioError::Invalid)?;
                stream.fade_out_and_stop(node.tick_sample_length)?;
                match mix {
                    Some(buffer) => stream.fill(buffer, output_offset, sample_length, self.config.stereo)?,
                    None => stream.skip(sample_length)?,
                }
                if stream.is_fading() {
                    self.fade_out.add_sub_stream(stream);
                }
                node.clear();
                if node.release_state >= 0 {
                    self.unlink(id);
                    self.clear_loop_group(id, node);
                }
                Ok(true)
            } else {
                let volume = self.calculate_volume(node)?;
                let pan = self.calculate_pan(node);
                let tick = node.tick_sample_length;
                if let Some(stream) = node.stream.as_mut() {
                    stream.fade_to_volume_and_pan(tick, volume, pan)?;
                }
                Ok(false)
            }
        } else {
            node.clear();
            self.unlink(id);
            self.clear_loop_group(id, node);
            Ok(true)
        }
    }

    fn retrigger_stream(
        &self,
        node: &mut PatchNode,
        old: &Voice,
        fade_length: Option<i32>,
    ) -> R<()> {
        let file = node.audio_file.clone().ok_or(AudioError::Invalid)?;
        let channel = node.current_track as usize;
        if self.general_purpose1[channel] == 0 {
            node.stream = Some(Voice::from_file(
                &file,
                old.absolute_playback_rate(),
                old.target_volume(),
                old.pan_position(),
            ));
        } else {
            node.stream = Some(Voice::from_file(
                &file,
                old.absolute_playback_rate(),
                0,
                old.pan_position(),
            ));
            let reverse = node.loops_forever()?;
            self.apply_initial_sample_offset(node, reverse);
            if let (Some(fade), Some(stream)) = (fade_length, node.stream.as_mut()) {
                stream.fade_to_volume(fade, old.target_volume())?;
            }
        }
        if node.loops_forever()? {
            if let Some(stream) = node.stream.as_mut() {
                stream.set_num_loops(-1);
            }
        }
        Ok(())
    }

    fn skip_patch_node(&mut self, node: &mut PatchNode, amount: i32) -> R<()> {
        let mut skip_amount = amount;
        let channel = node.current_track as usize;
        if self.sustain[channel] & 0x4 != 0 && node.release_state < 0 {
            let step = div(self.effect_speed_scaled[channel], self.config.sample_rate)?;
            let loop_count = div(
                step.wrapping_add(1048575).wrapping_sub(node.retrigger_phase),
                step,
            )?;
            node.retrigger_phase = (step
                .wrapping_mul(skip_amount)
                .wrapping_add(node.retrigger_phase))
                & 0xfffff;
            if loop_count <= skip_amount {
                let old = node.stream.take().ok_or(AudioError::Invalid)?;
                self.retrigger_stream(node, &old, None)?;
                skip_amount = div(node.retrigger_phase, step)?;
            }
        }
        node.stream
            .as_mut()
            .ok_or(AudioError::Invalid)?
            .skip(skip_amount)
    }

    fn fill_patch_node(
        &mut self,
        node: &mut PatchNode,
        mix: &mut [i32],
        offset: i32,
        length: i32,
        end_offset: i32,
    ) -> R<()> {
        let mut current_offset = offset;
        let mut remaining = length;
        let channel = node.current_track as usize;
        let stereo = self.config.stereo;
        if self.sustain[channel] & 0x4 != 0 && node.release_state < 0 {
            let step = div(self.effect_speed_scaled[channel], self.config.sample_rate)?;
            let mut guard = 0u32;
            loop {
                guard += 1;
                if guard > LOOP_GUARD {
                    return Err(AudioError::Invalid);
                }
                let loop_count = div(
                    step.wrapping_add(1048575).wrapping_sub(node.retrigger_phase),
                    step,
                )?;
                if loop_count > remaining {
                    node.retrigger_phase = node
                        .retrigger_phase
                        .wrapping_add(remaining.wrapping_mul(step));
                    break;
                }
                node.stream
                    .as_mut()
                    .ok_or(AudioError::Invalid)?
                    .fill(mix, current_offset, loop_count, stereo)?;
                current_offset = current_offset.wrapping_add(loop_count);
                remaining = remaining.wrapping_sub(loop_count);
                node.retrigger_phase = node
                    .retrigger_phase
                    .wrapping_add(step.wrapping_mul(loop_count).wrapping_sub(1048576));
                let fade_length = self.config.sample_rate / 100;
                let max_fade_length = div(262144, step)?;
                let actual = if max_fade_length < fade_length {
                    max_fade_length
                } else {
                    fade_length
                };
                let mut old = node.stream.take().ok_or(AudioError::Invalid)?;
                self.retrigger_stream(node, &old, Some(actual))?;
                old.fade_out_and_stop(actual)?;
                old.fill(mix, current_offset, end_offset.wrapping_sub(current_offset), stereo)?;
                if old.is_fading() {
                    self.fade_out.add_sub_stream(old);
                }
            }
        }
        node.stream
            .as_mut()
            .ok_or(AudioError::Invalid)?
            .fill(mix, current_offset, remaining, stereo)
    }

    fn patch_fill(&mut self, mix: &mut [i32], offset: i32, length: i32) -> R<()> {
        self.fade_out.fill(mix, offset, length, self.config.stereo)?;
        let snapshot = self.queue.clone();
        for id in snapshot {
            let mut node = self
                .nodes
                .get_mut(id)
                .and_then(|slot| slot.take())
                .ok_or(AudioError::Invalid)?;
            let result = self.patch_fill_node(id, &mut node, mix, offset, length);
            self.nodes[id] = Some(node);
            result?;
        }
        Ok(())
    }

    fn patch_fill_node(
        &mut self,
        id: usize,
        node: &mut PatchNode,
        mix: &mut [i32],
        offset: i32,
        length: i32,
    ) -> R<()> {
        if self.is_patch_node_dead(id, node) {
            return Ok(());
        }
        let mut current_offset = offset;
        let mut remaining = length;
        let mut guard = 0u32;
        loop {
            guard += 1;
            if guard > LOOP_GUARD {
                return Err(AudioError::Invalid);
            }
            if remaining <= node.tick_sample_length {
                self.fill_patch_node(node, mix, current_offset, remaining, remaining.wrapping_add(current_offset))?;
                node.tick_sample_length -= remaining;
                return Ok(());
            }
            let tick = node.tick_sample_length;
            self.fill_patch_node(node, mix, current_offset, tick, current_offset.wrapping_add(remaining))?;
            current_offset = current_offset.wrapping_add(tick);
            remaining = remaining.wrapping_sub(tick);
            if self.update_patch_node(id, node, Some(&mut *mix), current_offset, remaining)? {
                return Ok(());
            }
        }
    }

    fn patch_skip(&mut self, amount: i32) -> R<()> {
        self.fade_out.skip(amount)?;
        let snapshot = self.queue.clone();
        for id in snapshot {
            let mut node = self
                .nodes
                .get_mut(id)
                .and_then(|slot| slot.take())
                .ok_or(AudioError::Invalid)?;
            let result = self.patch_skip_node(id, &mut node, amount);
            self.nodes[id] = Some(node);
            result?;
        }
        Ok(())
    }

    fn patch_skip_node(&mut self, id: usize, node: &mut PatchNode, amount: i32) -> R<()> {
        if self.is_patch_node_dead(id, node) {
            return Ok(());
        }
        let mut remaining = amount;
        let mut guard = 0u32;
        loop {
            guard += 1;
            if guard > LOOP_GUARD {
                return Err(AudioError::Invalid);
            }
            if remaining <= node.tick_sample_length {
                self.skip_patch_node(node, remaining)?;
                node.tick_sample_length -= remaining;
                return Ok(());
            }
            let tick = node.tick_sample_length;
            self.skip_patch_node(node, tick)?;
            remaining = remaining.wrapping_sub(tick);
            if self.update_patch_node(id, node, None, 0, remaining)? {
                return Ok(());
            }
        }
    }
}

fn total_samples(file: &AudioFile) -> i32 {
    match file {
        AudioFile::Effect(buffer) => buffer.samples.len() as i32,
        AudioFile::Music(buffer) => buffer.samples.len() as i32,
    }
}
