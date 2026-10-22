use crate::audio_file::AudioFile;
use crate::mixer::MixedAudioStream;
use crate::reader::R;

pub enum Voice {
    Effect(MixedAudioStream<i8>),
    Music(MixedAudioStream<f32>),
}

macro_rules! each {
    ($self:expr, $s:ident => $body:expr) => {
        match $self {
            Voice::Effect($s) => $body,
            Voice::Music($s) => $body,
        }
    };
}

impl Voice {
    pub fn from_file(file: &AudioFile, sample_rate: i32, volume: i32, pan: i32) -> Self {
        match file {
            AudioFile::Effect(buffer) => {
                Voice::Effect(MixedAudioStream::<i8>::new(buffer, sample_rate, volume, pan))
            }
            AudioFile::Music(buffer) => {
                Voice::Music(MixedAudioStream::<f32>::from_buffer(buffer, sample_rate, volume, pan))
            }
        }
    }

    pub fn fill(&mut self, mix: &mut [i32], offset: i32, length: i32, stereo: bool) -> R<()> {
        each!(self, s => s.fill(mix, offset, length, stereo))
    }

    pub fn skip(&mut self, length: i32) -> R<()> {
        each!(self, s => s.skip(length))
    }

    pub fn linked(&self) -> bool {
        each!(self, s => s.linked)
    }

    pub fn set_linked(&mut self, linked: bool) {
        each!(self, s => s.linked = linked)
    }

    pub fn set_playback_rate(&mut self, rate: i32) {
        each!(self, s => s.set_playback_rate(rate))
    }

    pub fn is_finished(&self) -> bool {
        each!(self, s => s.is_finished())
    }

    pub fn is_fading(&self) -> bool {
        each!(self, s => s.is_fading())
    }

    pub fn fade_out_and_stop(&mut self, duration: i32) -> R<()> {
        each!(self, s => s.fade_out_and_stop(duration))
    }

    pub fn fade_to_volume(&mut self, duration: i32, volume: i32) -> R<()> {
        each!(self, s => s.fade_to_volume(duration, volume))
    }

    pub fn fade_to_volume_and_pan(&mut self, duration: i32, volume: i32, pan: i32) -> R<()> {
        each!(self, s => s.fade_to_volume_and_pan(duration, volume, pan))
    }

    pub fn set_num_loops(&mut self, loops: i32) {
        each!(self, s => s.set_num_loops(loops))
    }

    pub fn set_playback_position(&mut self, position: i32) {
        each!(self, s => s.set_playback_position(position))
    }

    pub fn reverse_playback_direction(&mut self) {
        each!(self, s => s.reverse_playback_direction())
    }

    pub fn absolute_playback_rate(&self) -> i32 {
        each!(self, s => s.absolute_playback_rate())
    }

    pub fn target_volume(&self) -> i32 {
        each!(self, s => s.target_volume())
    }

    pub fn pan_position(&self) -> i32 {
        each!(self, s => s.pan_position())
    }
}

#[derive(Default)]
pub struct PcmStreamMixer {
    streams: Vec<Voice>,
}

impl PcmStreamMixer {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn add_sub_stream(&mut self, mut stream: Voice) {
        stream.set_linked(true);
        self.streams.insert(0, stream);
    }

    pub fn len(&self) -> usize {
        self.streams.len()
    }

    pub fn is_empty(&self) -> bool {
        self.streams.is_empty()
    }

    pub fn clear(&mut self) {
        self.streams.clear();
    }

    pub fn fill(&mut self, mix: &mut [i32], offset: i32, length: i32, stereo: bool) -> R<()> {
        let mut index = 0;
        while index < self.streams.len() {
            let stream = &mut self.streams[index];
            stream.fill(mix, offset, length, stereo)?;
            if stream.linked() {
                index += 1;
            } else {
                self.streams.remove(index);
            }
        }
        Ok(())
    }

    pub fn skip(&mut self, length: i32) -> R<()> {
        let mut index = 0;
        while index < self.streams.len() {
            let stream = &mut self.streams[index];
            stream.skip(length)?;
            if stream.linked() {
                index += 1;
            } else {
                self.streams.remove(index);
            }
        }
        Ok(())
    }
}
