use crate::mixer::MixedAudioStream;
use crate::reader::{AudioError, R};
use crate::synth::MixedAudioBuffer;

pub const SAMPLE_RATE: i32 = 22050;
pub const BLOCK_FRAMES: usize = 256;
pub const MAX_ACTIVE_VOICES: i32 = 32;

pub fn clip_sample(value: i32) -> i16 {
    let mut data = value;
    if (data.wrapping_add(8_388_608) & !0xff_ffff) != 0 {
        data = 0x7f_ffff ^ (data >> 31);
    }
    (data >> 8) as i16
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct VoiceId(pub u32);

struct Voice {
    id: VoiceId,
    stream: MixedAudioStream,
    fill: bool,
}

pub struct PcmPlayer {
    voices: Vec<Voice>,
    next_id: u32,
    countdown: i32,
    pub stereo: bool,
    pub sample_rate: i32,
    pub filled_voices: u32,
}

impl PcmPlayer {
    pub fn new(stereo: bool) -> Self {
        Self {
            voices: Vec::new(),
            next_id: 0,
            countdown: 0,
            stereo,
            sample_rate: SAMPLE_RATE,
            filled_voices: 0,
        }
    }

    pub fn block_len(&self) -> usize {
        if self.stereo {
            BLOCK_FRAMES * 2
        } else {
            BLOCK_FRAMES
        }
    }

    pub fn add_voice(&mut self, stream: MixedAudioStream) -> VoiceId {
        let id = VoiceId(self.next_id);
        self.next_id = self.next_id.wrapping_add(1);
        self.voices.push(Voice {
            id,
            stream,
            fill: true,
        });
        id
    }

    pub fn add_buffer(
        &mut self,
        buffer: &MixedAudioBuffer,
        rate: i32,
        volume: i32,
        pan: i32,
        loops: i32,
    ) -> VoiceId {
        let mut stream = MixedAudioStream::new(buffer, rate, volume, pan);
        stream.set_num_loops(loops);
        self.add_voice(stream)
    }

    pub fn remove_voice(&mut self, id: VoiceId) {
        self.voices.retain(|voice| voice.id != id);
    }

    pub fn is_linked(&self, id: VoiceId) -> bool {
        self.voices
            .iter()
            .any(|voice| voice.id == id && voice.stream.linked)
    }

    pub fn voice_mut(&mut self, id: VoiceId) -> Option<&mut MixedAudioStream> {
        self.voices
            .iter_mut()
            .find(|voice| voice.id == id)
            .map(|voice| &mut voice.stream)
    }

    pub fn fill_of(&self, id: VoiceId) -> Option<bool> {
        self.voices
            .iter()
            .find(|voice| voice.id == id && voice.stream.linked)
            .map(|voice| voice.fill)
    }

    pub fn voice_count(&self) -> usize {
        self.voices.len()
    }

    pub fn fill_flags(&self) -> Vec<bool> {
        self.voices.iter().map(|voice| voice.fill).collect()
    }

    fn schedule(&mut self) -> R<()> {
        for voice in &mut self.voices {
            voice.fill = false;
        }
        let mut buckets: [Vec<usize>; 8] = Default::default();
        for (index, voice) in self.voices.iter().enumerate().rev() {
            let priority = 255i32.wrapping_mul(voice.stream.priority()?) >> 8;
            buckets
                .get_mut((priority >> 5) as usize)
                .ok_or(AudioError::Bounds)?
                .push(index);
        }
        let mut active = 0i32;
        'scan: for bucket in buckets.iter().rev() {
            for &index in bucket {
                self.voices[index].fill = true;
                active = active.wrapping_add(self.voices[index].stream.active_voice_count());
                if active >= MAX_ACTIVE_VOICES {
                    break 'scan;
                }
            }
        }
        Ok(())
    }

    pub fn fill_block(&mut self, out: &mut [i32]) -> R<()> {
        let stereo = self.stereo;
        let len = self.block_len();
        out[..len].fill(0);
        self.countdown -= BLOCK_FRAMES as i32;
        if self.countdown <= 0 {
            self.countdown += self.sample_rate >> 4;
            self.schedule()?;
        }
        if self.countdown < 0 {
            self.countdown = 0;
        }
        let mut filled = 0u32;
        for voice in &mut self.voices {
            if voice.fill {
                voice
                    .stream
                    .fill(out, 0, BLOCK_FRAMES as i32, stereo)?;
                filled += 1;
            } else {
                voice.stream.skip(BLOCK_FRAMES as i32)?;
            }
        }
        self.filled_voices = filled;
        self.voices.retain(|voice| voice.stream.linked);
        Ok(())
    }

    pub fn render(&mut self, blocks: usize) -> R<Vec<i16>> {
        let len = self.block_len();
        let mut mix = vec![0i32; len];
        let mut pcm = Vec::with_capacity(len * blocks);
        for _ in 0..blocks {
            self.fill_block(&mut mix)?;
            pcm.extend(mix.iter().map(|v| clip_sample(*v)));
        }
        Ok(pcm)
    }

    pub fn skip_block(&mut self) -> R<()> {
        self.countdown -= BLOCK_FRAMES as i32;
        if self.countdown < 0 {
            self.countdown = 0;
        }
        for voice in &mut self.voices {
            voice.stream.skip(BLOCK_FRAMES as i32)?;
        }
        self.voices.retain(|voice| voice.stream.linked);
        Ok(())
    }
}
