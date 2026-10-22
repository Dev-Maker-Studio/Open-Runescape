use std::sync::Arc;

use crate::reader::{at, AudioError, R};
use crate::synth::MixedAudioBuffer;

pub fn calculate_sound_attenuation(volume: i32, pan: i32) -> i32 {
    if pan < 0 {
        volume
    } else {
        (volume as f64 * libm::sqrt(1.220703125E-4 * 16384i32.wrapping_sub(pan) as f64) + 0.5) as i32
    }
}

pub fn calculate_sound_pan(volume: i32, pan: i32) -> i32 {
    if pan < 0 {
        volume.wrapping_neg()
    } else {
        (volume as f64 * libm::sqrt(1.220703125E-4 * pan as f64) + 0.5) as i32
    }
}

fn flip(rate: i32) -> i32 {
    rate.wrapping_mul(1927994969)
        .wrapping_neg()
        .wrapping_mul(-177305111)
}

fn ld(samples: &[i8], index: i32) -> R<i32> {
    at(samples, index as i64).map(|v| v as i32)
}

fn add(mix: &mut [i32], index: i32, value: i32) -> R<()> {
    if index < 0 {
        return Err(AudioError::Bounds);
    }
    let slot = mix.get_mut(index as usize).ok_or(AudioError::Bounds)?;
    *slot = slot.wrapping_add(value);
    Ok(())
}

fn div(a: i32, b: i32) -> R<i32> {
    if b == 0 {
        Err(AudioError::Invalid)
    } else {
        Ok(a.wrapping_div(b))
    }
}

fn rem(a: i32, b: i32) -> R<i32> {
    if b == 0 {
        Err(AudioError::Invalid)
    } else {
        Ok(a.wrapping_rem(b))
    }
}

fn sh(value: i32) -> i32 {
    value.wrapping_shl(8)
}

pub trait Pcm: Copy + Send + Sync + std::fmt::Debug + 'static {
    type Wrap: Copy;

    fn load(samples: &[Self], index: i32) -> R<Self::Wrap>;

    fn zero() -> Self::Wrap;

    #[allow(clippy::too_many_arguments)]
    fn constant(
        stream: &mut MixedAudioStream<Self>,
        samples: &[Self],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32>;

    #[allow(clippy::too_many_arguments)]
    fn interpolated(
        stream: &mut MixedAudioStream<Self>,
        samples: &[Self],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        wrap: Self::Wrap,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32>;
}

impl Pcm for i8 {
    type Wrap = i32;

    fn load(samples: &[i8], index: i32) -> R<i32> {
        ld(samples, index)
    }

    fn zero() -> i32 {
        0
    }

    fn constant(
        stream: &mut MixedAudioStream<i8>,
        samples: &[i8],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        stream.mix_constant(samples, mix, out0, max_out, end_position, stereo, backward, fading)
    }

    fn interpolated(
        stream: &mut MixedAudioStream<i8>,
        samples: &[i8],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        wrap: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        stream.mix_interpolated(samples, mix, out0, max_out, end_position, wrap, stereo, backward, fading)
    }
}

#[derive(Debug, Clone)]
pub struct MixedAudioStream<S: Pcm = i8> {
    pub samples: Arc<[S]>,
    pub start: i32,
    pub end: i32,
    pub enable_loop: bool,
    pub sample_rate: i32,
    pub current_volume: i32,
    pub left_channel_volume: i32,
    pub right_channel_volume: i32,
    pub fade_samples_remaining: i32,
    pub loops_remaining: i32,
    pub target_volume: i32,
    pub playback_position: i32,
    pub pan_position: i32,
    pub volume_fade_delta: i32,
    pub left_volume_fade_delta: i32,
    pub right_volume_fade_delta: i32,
    pub linked: bool,
}

impl MixedAudioStream<i8> {
    pub fn new(buffer: &MixedAudioBuffer, sample_rate: i32, volume: i32, pan: i32) -> Self {
        Self::with_samples(
            Arc::from(buffer.samples.as_slice()),
            buffer.start,
            buffer.end,
            buffer.enable_loop,
            sample_rate,
            volume,
            pan,
        )
    }
}

impl<S: Pcm> MixedAudioStream<S> {
    pub fn with_samples(
        samples: Arc<[S]>,
        start: i32,
        end: i32,
        enable_loop: bool,
        sample_rate: i32,
        volume: i32,
        pan: i32,
    ) -> Self {
        let mut stream = Self {
            samples,
            start,
            end,
            enable_loop,
            sample_rate,
            current_volume: 0,
            left_channel_volume: 0,
            right_channel_volume: 0,
            fade_samples_remaining: 0,
            loops_remaining: 0,
            target_volume: volume,
            playback_position: 0,
            pan_position: pan,
            volume_fade_delta: 0,
            left_volume_fade_delta: 0,
            right_volume_fade_delta: 0,
            linked: true,
        };
        stream.recalculate_channel_volumes();
        stream
    }

    fn unlink(&mut self) {
        self.linked = false;
    }

    fn total_shifted(&self) -> i32 {
        sh(self.samples.len() as i32)
    }

    pub fn set_playback_rate(&mut self, rate: i32) {
        self.sample_rate = if self.sample_rate < 0 {
            rate.wrapping_neg()
        } else {
            rate
        };
    }

    pub fn is_fading(&self) -> bool {
        self.fade_samples_remaining != 0
    }

    pub fn set_volume(&mut self, volume: i32) {
        let pan = self.pan_position();
        self.set_volume_and_pan(volume.wrapping_shl(6), pan);
    }

    pub fn reverse_playback_direction(&mut self) {
        let rate = self.sample_rate;
        self.sample_rate = (rate ^ (rate >> 31)).wrapping_add(((rate as u32) >> 31) as i32);
        self.sample_rate = flip(self.sample_rate);
    }

    pub fn target_volume(&self) -> i32 {
        if self.target_volume == i32::MIN {
            0
        } else {
            self.target_volume
        }
    }

    pub fn pan_position(&self) -> i32 {
        if self.pan_position < 0 {
            -1
        } else {
            self.pan_position
        }
    }

    pub fn set_playback_position(&mut self, position: i32) {
        let total = self.total_shifted();
        let mut pos = position;
        if pos < -1 {
            pos = -1;
        }
        if pos > total {
            pos = total;
        }
        self.playback_position = pos;
    }

    pub fn is_finished(&self) -> bool {
        self.playback_position < 0 || self.playback_position >= self.total_shifted()
    }

    pub fn fade_to_volume(&mut self, duration: i32, volume: i32) -> R<()> {
        let pan = self.pan_position();
        self.fade_to_volume_and_pan(duration, volume, pan)
    }

    pub fn fade_to_volume_and_pan(&mut self, fade_duration: i32, volume: i32, pan: i32) -> R<()> {
        let mut duration = fade_duration;
        if duration == 0 {
            self.set_volume_and_pan(volume, pan);
            return Ok(());
        }
        let target_left = calculate_sound_attenuation(volume, pan);
        let target_right = calculate_sound_pan(volume, pan);
        if target_left == self.left_channel_volume && target_right == self.right_channel_volume {
            self.fade_samples_remaining = 0;
            return Ok(());
        }
        let mut max_delta = volume.wrapping_sub(self.current_volume);
        for candidate in [
            self.current_volume.wrapping_sub(volume),
            target_left.wrapping_sub(self.left_channel_volume),
            self.left_channel_volume.wrapping_sub(target_left),
            target_right.wrapping_sub(self.right_channel_volume),
            self.right_channel_volume.wrapping_sub(target_right),
        ] {
            if candidate > max_delta {
                max_delta = candidate;
            }
        }
        if duration > max_delta {
            duration = max_delta;
        }
        self.fade_samples_remaining = duration;
        self.target_volume = volume;
        self.pan_position = pan;
        self.volume_fade_delta = div(volume.wrapping_sub(self.current_volume), duration)?;
        self.left_volume_fade_delta = div(target_left.wrapping_sub(self.left_channel_volume), duration)?;
        self.right_volume_fade_delta = div(target_right.wrapping_sub(self.right_channel_volume), duration)?;
        Ok(())
    }

    pub fn fade_out_and_stop(&mut self, fade_duration: i32) -> R<()> {
        let mut duration = fade_duration;
        if duration == 0 {
            self.mute_volume();
            self.unlink();
        } else if self.left_channel_volume == 0 && self.right_channel_volume == 0 {
            self.fade_samples_remaining = 0;
            self.target_volume = 0;
            self.current_volume = 0;
            self.unlink();
        } else {
            let mut max_delta = self.current_volume.wrapping_neg();
            for candidate in [
                self.current_volume,
                self.left_channel_volume.wrapping_neg(),
                self.left_channel_volume,
                self.right_channel_volume.wrapping_neg(),
                self.right_channel_volume,
            ] {
                if candidate > max_delta {
                    max_delta = candidate;
                }
            }
            if duration > max_delta {
                duration = max_delta;
            }
            self.fade_samples_remaining = duration;
            self.target_volume = i32::MIN;
            self.volume_fade_delta = div(self.current_volume.wrapping_neg(), duration)?;
            self.left_volume_fade_delta = div(self.left_channel_volume.wrapping_neg(), duration)?;
            self.right_volume_fade_delta = div(self.right_channel_volume.wrapping_neg(), duration)?;
        }
        Ok(())
    }

    pub fn absolute_playback_rate(&self) -> i32 {
        if self.sample_rate < 0 {
            self.sample_rate.wrapping_neg()
        } else {
            self.sample_rate
        }
    }

    pub fn active_voice_count(&self) -> i32 {
        if self.target_volume == 0 && self.fade_samples_remaining == 0 {
            0
        } else {
            1
        }
    }

    pub fn update_fade_state(&mut self) -> bool {
        let mut volume = self.target_volume;
        let target_left;
        let target_right;
        if volume == i32::MIN {
            target_right = 0;
            target_left = 0;
            volume = 0;
        } else {
            target_left = calculate_sound_attenuation(volume, self.pan_position);
            target_right = calculate_sound_pan(volume, self.pan_position);
        }
        if volume == self.current_volume
            && target_left == self.left_channel_volume
            && target_right == self.right_channel_volume
        {
            if self.target_volume == i32::MIN {
                self.target_volume = 0;
                self.right_channel_volume = 0;
                self.left_channel_volume = 0;
                self.current_volume = 0;
                self.unlink();
                return true;
            }
            self.recalculate_channel_volumes();
            return false;
        }
        if self.current_volume < volume {
            self.volume_fade_delta = 1;
            self.fade_samples_remaining = volume.wrapping_sub(self.current_volume);
        } else if self.current_volume > volume {
            self.volume_fade_delta = -1;
            self.fade_samples_remaining = self.current_volume.wrapping_sub(volume);
        } else {
            self.volume_fade_delta = 0;
        }
        if self.left_channel_volume < target_left {
            self.left_volume_fade_delta = 1;
            let diff = target_left.wrapping_sub(self.left_channel_volume);
            if self.fade_samples_remaining == 0 || self.fade_samples_remaining > diff {
                self.fade_samples_remaining = diff;
            }
        } else if self.left_channel_volume > target_left {
            self.left_volume_fade_delta = -1;
            let diff = self.left_channel_volume.wrapping_sub(target_left);
            if self.fade_samples_remaining == 0 || self.fade_samples_remaining > diff {
                self.fade_samples_remaining = diff;
            }
        } else {
            self.left_volume_fade_delta = 0;
        }
        if self.right_channel_volume < target_right {
            self.right_volume_fade_delta = 1;
            let diff = target_right.wrapping_sub(self.right_channel_volume);
            if self.fade_samples_remaining == 0 || self.fade_samples_remaining > diff {
                self.fade_samples_remaining = diff;
            }
        } else if self.right_channel_volume > target_right {
            self.right_volume_fade_delta = -1;
            let diff = self.right_channel_volume.wrapping_sub(target_right);
            if self.fade_samples_remaining == 0 || self.fade_samples_remaining > diff {
                self.fade_samples_remaining = diff;
            }
        } else {
            self.right_volume_fade_delta = 0;
        }
        false
    }

    pub fn skip(&mut self, sample_count: i32) -> R<()> {
        let mut remaining = sample_count;
        if self.fade_samples_remaining > 0 {
            if remaining >= self.fade_samples_remaining {
                if self.target_volume == i32::MIN {
                    self.target_volume = 0;
                    self.right_channel_volume = 0;
                    self.left_channel_volume = 0;
                    self.current_volume = 0;
                    self.unlink();
                    remaining = self.fade_samples_remaining;
                }
                self.fade_samples_remaining = 0;
                self.recalculate_channel_volumes();
            } else {
                self.current_volume = self
                    .current_volume
                    .wrapping_add(remaining.wrapping_mul(self.volume_fade_delta));
                self.left_channel_volume = self
                    .left_channel_volume
                    .wrapping_add(self.left_volume_fade_delta.wrapping_mul(remaining));
                self.right_channel_volume = self
                    .right_channel_volume
                    .wrapping_add(self.right_volume_fade_delta.wrapping_mul(remaining));
                self.fade_samples_remaining -= remaining;
            }
        }
        let loop_start = sh(self.start);
        let loop_end = sh(self.end);
        let total = self.total_shifted();
        let loop_length = loop_end.wrapping_sub(loop_start);
        if loop_length <= 0 {
            self.loops_remaining = 0;
        }
        if self.playback_position < 0 {
            if self.sample_rate <= 0 {
                self.cancel_fade();
                self.unlink();
                return Ok(());
            }
            self.playback_position = 0;
        }
        if self.playback_position >= total {
            if self.sample_rate >= 0 {
                self.cancel_fade();
                self.unlink();
                return Ok(());
            }
            self.playback_position = total.wrapping_sub(1);
        }
        self.playback_position = self
            .playback_position
            .wrapping_add(self.sample_rate.wrapping_mul(remaining));
        if self.loops_remaining < 0 {
            if self.enable_loop {
                if self.sample_rate < 0 {
                    if self.playback_position >= loop_start {
                        return Ok(());
                    }
                    self.playback_position = loop_start
                        .wrapping_add(loop_start)
                        .wrapping_sub(1)
                        .wrapping_sub(self.playback_position);
                    self.sample_rate = flip(self.sample_rate);
                }
                while self.playback_position >= loop_end {
                    self.playback_position = loop_end
                        .wrapping_add(loop_end)
                        .wrapping_sub(1)
                        .wrapping_sub(self.playback_position);
                    self.sample_rate = flip(self.sample_rate);
                    if self.playback_position >= loop_start {
                        break;
                    }
                    self.playback_position = loop_start
                        .wrapping_add(loop_start)
                        .wrapping_sub(1)
                        .wrapping_sub(self.playback_position);
                    self.sample_rate = flip(self.sample_rate);
                }
            } else if self.sample_rate < 0 {
                if self.playback_position < loop_start {
                    let inner = loop_end
                        .wrapping_sub(1)
                        .wrapping_sub(self.playback_position);
                    self.playback_position = loop_end
                        .wrapping_sub(1)
                        .wrapping_sub(rem(inner, loop_length)?);
                }
            } else if self.playback_position >= loop_end {
                self.playback_position = loop_start
                    .wrapping_add(rem(self.playback_position.wrapping_sub(loop_start), loop_length)?);
            }
            return Ok(());
        }
        if self.loops_remaining > 0 {
            if self.enable_loop {
                'ping: {
                    if self.sample_rate < 0 {
                        if self.playback_position >= loop_start {
                            break 'ping;
                        }
                        self.playback_position = loop_start
                            .wrapping_add(loop_start)
                            .wrapping_sub(1)
                            .wrapping_sub(self.playback_position);
                        self.sample_rate = flip(self.sample_rate);
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break 'ping;
                        }
                    }
                    loop {
                        if self.playback_position < loop_end {
                            break 'ping;
                        }
                        self.playback_position = loop_end
                            .wrapping_add(loop_end)
                            .wrapping_sub(1)
                            .wrapping_sub(self.playback_position);
                        self.sample_rate = flip(self.sample_rate);
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break;
                        }
                        if self.playback_position >= loop_start {
                            break 'ping;
                        }
                        self.playback_position = loop_start
                            .wrapping_add(loop_start)
                            .wrapping_sub(1)
                            .wrapping_sub(self.playback_position);
                        self.sample_rate = flip(self.sample_rate);
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break;
                        }
                    }
                }
            } else {
                if self.sample_rate < 0 {
                    if self.playback_position >= loop_start {
                        return Ok(());
                    }
                    let skips = div(
                        loop_end.wrapping_sub(1).wrapping_sub(self.playback_position),
                        loop_length,
                    )?;
                    if skips < self.loops_remaining {
                        self.playback_position = self
                            .playback_position
                            .wrapping_add(loop_length.wrapping_mul(skips));
                        self.loops_remaining -= skips;
                        return Ok(());
                    }
                    self.playback_position = self
                        .playback_position
                        .wrapping_add(self.loops_remaining.wrapping_mul(loop_length));
                } else {
                    if self.playback_position < loop_end {
                        return Ok(());
                    }
                    let skips = div(self.playback_position.wrapping_sub(loop_start), loop_length)?;
                    if skips < self.loops_remaining {
                        self.playback_position = self
                            .playback_position
                            .wrapping_sub(loop_length.wrapping_mul(skips));
                        self.loops_remaining -= skips;
                        return Ok(());
                    }
                    self.playback_position = self
                        .playback_position
                        .wrapping_sub(loop_length.wrapping_mul(self.loops_remaining));
                }
                self.loops_remaining = 0;
            }
        }
        if self.sample_rate < 0 {
            if self.playback_position < 0 {
                self.playback_position = -1;
                self.cancel_fade();
                self.unlink();
            }
        } else if self.playback_position >= total {
            self.playback_position = total;
            self.cancel_fade();
            self.unlink();
        }
        Ok(())
    }

    pub fn set_num_loops(&mut self, loops: i32) {
        self.loops_remaining = loops;
    }

    pub fn mute_volume(&mut self) {
        let pan = self.pan_position();
        self.set_volume_and_pan(0, pan);
    }

    pub fn recalculate_channel_volumes(&mut self) {
        self.current_volume = self.target_volume;
        self.left_channel_volume = calculate_sound_attenuation(self.target_volume, self.pan_position);
        self.right_channel_volume = calculate_sound_pan(self.target_volume, self.pan_position);
    }

    pub fn priority(&self) -> R<i32> {
        let mut priority = self.current_volume.wrapping_mul(3) >> 6;
        priority = (priority ^ (priority >> 31)).wrapping_add(((priority as u32) >> 31) as i32);
        if self.loops_remaining == 0 {
            priority = priority.wrapping_sub(div(
                priority.wrapping_mul(self.playback_position),
                self.total_shifted(),
            )?);
        } else if self.loops_remaining >= 0 {
            priority = priority.wrapping_sub(div(
                priority.wrapping_mul(self.start),
                self.samples.len() as i32,
            )?);
        }
        Ok(priority.min(255))
    }

    pub fn set_pan_position(&mut self, pan: i32) {
        let volume = self.target_volume();
        self.set_volume_and_pan(volume, pan);
    }

    pub fn cancel_fade(&mut self) {
        if self.fade_samples_remaining != 0 {
            if self.target_volume == i32::MIN {
                self.target_volume = 0;
            }
            self.fade_samples_remaining = 0;
            self.recalculate_channel_volumes();
        }
    }

    pub fn set_volume_and_pan(&mut self, volume: i32, pan: i32) {
        self.target_volume = volume;
        self.pan_position = pan;
        self.fade_samples_remaining = 0;
        self.recalculate_channel_volumes();
    }


    #[allow(clippy::too_many_arguments)]
    fn mix_samples(
        &mut self,
        samples: &[S],
        mix: &mut [i32],
        output_offset: i32,
        end_position: i32,
        max_out: i32,
        wrap: S::Wrap,
        stereo: bool,
        backward: bool,
    ) -> R<i32> {
        let unit: i32 = if backward { -256 } else { 256 };
        let mut out = output_offset;
        loop {
            let constant = self.sample_rate == unit && (self.playback_position & 0xff) == 0;
            if self.fade_samples_remaining <= 0 {
                return if constant {
                    S::constant(self, samples, mix, out, max_out, end_position, stereo, backward, false)
                } else {
                    S::interpolated(self, samples, mix, out, max_out, end_position, wrap, stereo, backward, false)
                };
            }
            let mut fade_end = out.wrapping_add(self.fade_samples_remaining);
            if fade_end > max_out {
                fade_end = max_out;
            }
            self.fade_samples_remaining = self.fade_samples_remaining.wrapping_add(out);
            out = if constant {
                S::constant(self, samples, mix, out, fade_end, end_position, stereo, backward, true)?
            } else {
                S::interpolated(self, samples, mix, out, fade_end, end_position, wrap, stereo, backward, true)?
            };
            self.fade_samples_remaining = self.fade_samples_remaining.wrapping_sub(out);
            if self.fade_samples_remaining != 0 {
                return Ok(out);
            }
            if self.update_fade_state() {
                break;
            }
        }
        Ok(max_out)
    }

    pub fn fill(&mut self, mix: &mut [i32], offset: i32, length: i32, stereo: bool) -> R<()> {
        if self.target_volume == 0 && self.fade_samples_remaining == 0 {
            return self.skip(length);
        }
        let samples = self.samples.clone();
        let s: &[S] = &samples;
        let start_pos = sh(self.start);
        let end_pos = sh(self.end);
        let total = sh(s.len() as i32);
        let loop_length = end_pos.wrapping_sub(start_pos);
        if loop_length <= 0 {
            self.loops_remaining = 0;
        }
        let mut cur = offset;
        let end_off = length.wrapping_add(offset);
        if self.playback_position < 0 {
            if self.sample_rate <= 0 {
                self.cancel_fade();
                self.unlink();
                return Ok(());
            }
            self.playback_position = 0;
        }
        if self.playback_position >= total {
            if self.sample_rate >= 0 {
                self.cancel_fade();
                self.unlink();
                return Ok(());
            }
            self.playback_position = total.wrapping_sub(1);
        }
        macro_rules! forward {
            ($from:expr, $end:expr, $wrap:expr) => {{
                let wrap = $wrap;
                self.mix_samples(s, mix, $from, $end, end_off, wrap, stereo, false)?
            }};
        }
        macro_rules! backward {
            ($from:expr, $end:expr, $wrap:expr) => {{
                let wrap = $wrap;
                self.mix_samples(s, mix, $from, $end, end_off, wrap, stereo, true)?
            }};
        }
        macro_rules! reflect_low {
            () => {
                self.playback_position = start_pos
                    .wrapping_add(start_pos)
                    .wrapping_sub(1)
                    .wrapping_sub(self.playback_position);
                self.sample_rate = flip(self.sample_rate);
            };
        }
        macro_rules! reflect_high {
            () => {
                self.playback_position = end_pos
                    .wrapping_add(end_pos)
                    .wrapping_sub(1)
                    .wrapping_sub(self.playback_position);
                self.sample_rate = flip(self.sample_rate);
            };
        }
        if self.loops_remaining < 0 {
            if self.enable_loop {
                if self.sample_rate < 0 {
                    cur = backward!(offset, start_pos, S::load(s, self.start)?);
                    if self.playback_position >= start_pos {
                        return Ok(());
                    }
                    reflect_low!();
                }
                loop {
                    cur = forward!(cur, end_pos, S::load(s, self.end.wrapping_sub(1))?);
                    if self.playback_position < end_pos {
                        break;
                    }
                    reflect_high!();
                    cur = backward!(cur, start_pos, S::load(s, self.start)?);
                    if self.playback_position >= start_pos {
                        break;
                    }
                    reflect_low!();
                }
            } else if self.sample_rate < 0 {
                loop {
                    cur = backward!(cur, start_pos, S::load(s, self.end.wrapping_sub(1))?);
                    if self.playback_position >= start_pos {
                        break;
                    }
                    let inner = end_pos.wrapping_sub(1).wrapping_sub(self.playback_position);
                    self.playback_position = end_pos.wrapping_sub(1).wrapping_sub(rem(inner, loop_length)?);
                }
            } else {
                loop {
                    cur = forward!(cur, end_pos, S::load(s, self.start)?);
                    if self.playback_position < end_pos {
                        break;
                    }
                    self.playback_position = start_pos
                        .wrapping_add(rem(self.playback_position.wrapping_sub(start_pos), loop_length)?);
                }
            }
            return Ok(());
        }
        if self.loops_remaining > 0 {
            if self.enable_loop {
                'looping: {
                    if self.sample_rate < 0 {
                        cur = backward!(offset, start_pos, S::load(s, self.start)?);
                        if self.playback_position >= start_pos {
                            return Ok(());
                        }
                        reflect_low!();
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break 'looping;
                        }
                    }
                    loop {
                        cur = forward!(cur, end_pos, S::load(s, self.end.wrapping_sub(1))?);
                        if self.playback_position < end_pos {
                            return Ok(());
                        }
                        reflect_high!();
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break;
                        }
                        cur = backward!(cur, start_pos, S::load(s, self.start)?);
                        if self.playback_position >= start_pos {
                            return Ok(());
                        }
                        reflect_low!();
                        self.loops_remaining -= 1;
                        if self.loops_remaining == 0 {
                            break;
                        }
                    }
                }
            } else if self.sample_rate < 0 {
                loop {
                    cur = backward!(cur, start_pos, S::load(s, self.end.wrapping_sub(1))?);
                    if self.playback_position >= start_pos {
                        return Ok(());
                    }
                    let skips = div(
                        end_pos.wrapping_sub(1).wrapping_sub(self.playback_position),
                        loop_length,
                    )?;
                    if skips >= self.loops_remaining {
                        self.playback_position = self
                            .playback_position
                            .wrapping_add(loop_length.wrapping_mul(self.loops_remaining));
                        self.loops_remaining = 0;
                        break;
                    }
                    self.playback_position = self
                        .playback_position
                        .wrapping_add(loop_length.wrapping_mul(skips));
                    self.loops_remaining -= skips;
                }
            } else {
                loop {
                    cur = forward!(cur, end_pos, S::load(s, self.start)?);
                    if self.playback_position < end_pos {
                        return Ok(());
                    }
                    let skips = div(self.playback_position.wrapping_sub(start_pos), loop_length)?;
                    if skips >= self.loops_remaining {
                        self.playback_position = self
                            .playback_position
                            .wrapping_sub(loop_length.wrapping_mul(self.loops_remaining));
                        self.loops_remaining = 0;
                        break;
                    }
                    self.playback_position = self
                        .playback_position
                        .wrapping_sub(loop_length.wrapping_mul(skips));
                    self.loops_remaining -= skips;
                }
            }
        }
        if self.sample_rate < 0 {
            backward!(cur, 0, S::zero());
            if self.playback_position < 0 {
                self.playback_position = -1;
                self.cancel_fade();
                self.unlink();
            }
        } else {
            forward!(cur, total, S::zero());
            if self.playback_position >= total {
                self.playback_position = total;
                self.cancel_fade();
                self.unlink();
            }
        }
        Ok(())
    }
}

impl MixedAudioStream<i8> {
    #[allow(clippy::too_many_arguments)]
    fn mix_constant(
        &mut self,
        samples: &[i8],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        let mut sample_pos = self.playback_position >> 8;
        let end_pos = end_position >> 8;
        let mut limit = if backward {
            out0.wrapping_add(sample_pos).wrapping_sub(end_pos.wrapping_sub(1))
        } else {
            out0.wrapping_add(end_pos).wrapping_sub(sample_pos)
        };
        if limit > max_out {
            limit = max_out;
        }
        let delta_step: i32 = if backward { -1 } else { 1 };
        if !stereo {
            let mut volume = self.current_volume.wrapping_shl(2);
            let delta = if fading { self.volume_fade_delta.wrapping_shl(2) } else { 0 };
            if fading {
                let count = limit.wrapping_sub(out0);
                self.left_channel_volume = self
                    .left_channel_volume
                    .wrapping_add(self.left_volume_fade_delta.wrapping_mul(count));
                self.right_channel_volume = self
                    .right_channel_volume
                    .wrapping_add(self.right_volume_fade_delta.wrapping_mul(count));
            }
            let mut out = out0;
            while out < limit {
                add(mix, out, ld(samples, sample_pos)?.wrapping_mul(volume))?;
                volume = volume.wrapping_add(delta);
                out += 1;
                sample_pos = sample_pos.wrapping_add(delta_step);
            }
            if fading {
                self.current_volume = volume >> 2;
            }
            self.playback_position = sh(sample_pos);
            return Ok(out);
        }
        let mut left = self.left_channel_volume.wrapping_shl(2);
        let mut right = self.right_channel_volume.wrapping_shl(2);
        let (left_delta, right_delta) = if fading {
            (
                self.left_volume_fade_delta.wrapping_shl(2),
                self.right_volume_fade_delta.wrapping_shl(2),
            )
        } else {
            (0, 0)
        };
        if fading {
            self.current_volume = self
                .current_volume
                .wrapping_add(self.volume_fade_delta.wrapping_mul(limit.wrapping_sub(out0)));
        }
        let mut out = out0.wrapping_shl(1);
        let out_limit = limit.wrapping_shl(1);
        while out < out_limit {
            let sample = ld(samples, sample_pos)?;
            add(mix, out, sample.wrapping_mul(left))?;
            left = left.wrapping_add(left_delta);
            add(mix, out + 1, sample.wrapping_mul(right))?;
            right = right.wrapping_add(right_delta);
            out += 2;
            sample_pos = sample_pos.wrapping_add(delta_step);
        }
        if fading {
            self.left_channel_volume = left >> 2;
            self.right_channel_volume = right >> 2;
        }
        self.playback_position = sh(sample_pos);
        Ok(out >> 1)
    }

    #[allow(clippy::too_many_arguments)]
    fn mix_interpolated(
        &mut self,
        samples: &[i8],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        wrap: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        let step = self.sample_rate;
        let mut pos = self.playback_position;
        let mut volume = self.current_volume;
        let volume_delta = if fading { self.volume_fade_delta } else { 0 };
        let mut left = self.left_channel_volume;
        let mut right = self.right_channel_volume;
        let (left_delta, right_delta) = if fading {
            (self.left_volume_fade_delta, self.right_volume_fade_delta)
        } else {
            (0, 0)
        };
        if fading {
            if stereo {
                self.current_volume = self
                    .current_volume
                    .wrapping_sub(self.volume_fade_delta.wrapping_mul(out0));
            } else {
                self.left_channel_volume = self
                    .left_channel_volume
                    .wrapping_sub(self.left_volume_fade_delta.wrapping_mul(out0));
                self.right_channel_volume = self
                    .right_channel_volume
                    .wrapping_sub(self.right_volume_fade_delta.wrapping_mul(out0));
            }
        }
        let first = if backward {
            end_position.wrapping_add(256).wrapping_sub(pos).wrapping_add(step)
        } else {
            end_position.wrapping_sub(pos).wrapping_add(step).wrapping_sub(257)
        };
        let mut limit = if step == 0 {
            max_out
        } else {
            out0.wrapping_add(first.wrapping_div(step))
        };
        if limit > max_out {
            limit = max_out;
        }
        let channels = if stereo { 2 } else { 1 };
        let mut out = if stereo { out0.wrapping_shl(1) } else { out0 };
        let mut out_limit = if stereo { limit.wrapping_shl(1) } else { limit };
        while out < out_limit {
            let index = pos >> 8;
            let (base, next) = if backward {
                let base = ld(samples, index.wrapping_sub(1))?;
                (base, ld(samples, index)?)
            } else {
                let base = ld(samples, index)?;
                (base, ld(samples, index.wrapping_add(1))?)
            };
            let interpolated = sh(base).wrapping_add(next.wrapping_sub(base).wrapping_mul(pos & 0xff));
            if stereo {
                add(mix, out, interpolated.wrapping_mul(left) >> 6)?;
                left = left.wrapping_add(left_delta);
                add(mix, out + 1, interpolated.wrapping_mul(right) >> 6)?;
                right = right.wrapping_add(right_delta);
            } else {
                add(mix, out, interpolated.wrapping_mul(volume) >> 6)?;
                volume = volume.wrapping_add(volume_delta);
            }
            out += channels;
            pos = pos.wrapping_add(step);
        }
        let second = if backward {
            end_position.wrapping_sub(pos).wrapping_add(step)
        } else {
            end_position.wrapping_sub(pos).wrapping_add(step).wrapping_sub(1)
        };
        let units = if stereo { out >> 1 } else { out };
        limit = if step == 0 {
            max_out
        } else {
            units.wrapping_add(second.wrapping_div(step))
        };
        if limit > max_out {
            limit = max_out;
        }
        out_limit = if stereo { limit.wrapping_shl(1) } else { limit };
        while out < out_limit {
            let current = ld(samples, pos >> 8)?;
            let (base, next) = if backward { (wrap, current) } else { (current, wrap) };
            let interpolated = sh(base).wrapping_add(next.wrapping_sub(base).wrapping_mul(pos & 0xff));
            if stereo {
                add(mix, out, interpolated.wrapping_mul(left) >> 6)?;
                left = left.wrapping_add(left_delta);
                add(mix, out + 1, interpolated.wrapping_mul(right) >> 6)?;
                right = right.wrapping_add(right_delta);
            } else {
                add(mix, out, interpolated.wrapping_mul(volume) >> 6)?;
                volume = volume.wrapping_add(volume_delta);
            }
            out += channels;
            pos = pos.wrapping_add(step);
        }
        let result = if stereo { out >> 1 } else { out };
        if fading {
            if stereo {
                self.current_volume = self
                    .current_volume
                    .wrapping_add(self.volume_fade_delta.wrapping_mul(result));
                self.left_channel_volume = left;
                self.right_channel_volume = right;
            } else {
                self.left_channel_volume = self
                    .left_channel_volume
                    .wrapping_add(self.left_volume_fade_delta.wrapping_mul(result));
                self.right_channel_volume = self
                    .right_channel_volume
                    .wrapping_add(self.right_volume_fade_delta.wrapping_mul(result));
                self.current_volume = volume;
            }
        }
        self.playback_position = pos;
        Ok(result)
    }
}
