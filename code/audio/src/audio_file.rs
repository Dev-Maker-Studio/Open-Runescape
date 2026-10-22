use crate::synth::MixedAudioBuffer;

#[derive(Debug, Clone, PartialEq)]
pub struct AudioBuffer {
    pub sample_rate: i32,
    pub samples: Vec<f32>,
    pub start: i32,
    pub end: i32,
    pub enable_loop: bool,
}

impl AudioBuffer {
    pub fn new(sample_rate: i32, samples: Vec<f32>, start: i32, end: i32, enable_loop: bool) -> Self {
        Self {
            sample_rate,
            samples,
            start,
            end,
            enable_loop,
        }
    }
}

#[derive(Debug, Clone)]
pub enum AudioFile {
    Effect(MixedAudioBuffer),
    Music(AudioBuffer),
}

impl AudioFile {
    pub fn sample_rate(&self) -> i32 {
        match self {
            Self::Effect(buffer) => buffer.sample_rate,
            Self::Music(buffer) => buffer.sample_rate,
        }
    }

    pub fn start(&self) -> i32 {
        match self {
            Self::Effect(buffer) => buffer.start,
            Self::Music(buffer) => buffer.start,
        }
    }

    pub fn end(&self) -> i32 {
        match self {
            Self::Effect(buffer) => buffer.end,
            Self::Music(buffer) => buffer.end,
        }
    }

    pub fn enable_loop(&self) -> bool {
        match self {
            Self::Effect(buffer) => buffer.enable_loop,
            Self::Music(buffer) => buffer.enable_loop,
        }
    }
}
