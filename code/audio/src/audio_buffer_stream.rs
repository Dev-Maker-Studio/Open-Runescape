use std::sync::Arc;

use crate::audio_file::AudioBuffer;
use crate::mixer::{MixedAudioStream, Pcm};
use crate::reader::{at, AudioError, R};

pub type AudioBufferStream = MixedAudioStream<f32>;

impl MixedAudioStream<f32> {
    pub fn from_buffer(buffer: &AudioBuffer, sample_rate: i32, volume: i32, pan: i32) -> Self {
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

fn add(mix: &mut [i32], index: i32, value: i32) -> R<()> {
    if index < 0 {
        return Err(AudioError::Bounds);
    }
    let slot = mix.get_mut(index as usize).ok_or(AudioError::Bounds)?;
    *slot = slot.wrapping_add(value);
    Ok(())
}

fn add_float(mix: &mut [i32], index: i32, value: f32) -> R<()> {
    if index < 0 {
        return Err(AudioError::Bounds);
    }
    let slot = mix.get_mut(index as usize).ok_or(AudioError::Bounds)?;
    *slot = ((*slot as f32) + value) as i32;
    Ok(())
}

fn scaled(value: i32) -> i32 {
    value.wrapping_mul(128)
}

fn interpolate(base: f32, next: f32, position: i32) -> f32 {
    base * 256.0 + (next - base) * ((position & 0xff) as f32)
}

impl Pcm for f32 {
    type Wrap = f32;

    fn load(samples: &[f32], index: i32) -> R<f32> {
        at(samples, index as i64)
    }

    fn zero() -> f32 {
        0.0
    }

    fn constant(
        stream: &mut MixedAudioStream<f32>,
        samples: &[f32],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        let mut sample_pos = stream.playback_position >> 8;
        let end_pos = end_position >> 8;
        let mut limit = if backward {
            out0.wrapping_add(sample_pos).wrapping_sub(end_pos.wrapping_sub(1))
        } else {
            out0.wrapping_add(end_pos).wrapping_sub(sample_pos)
        };
        if limit > max_out {
            limit = max_out;
        }
        let step: i32 = if backward { -1 } else { 1 };
        if !stereo {
            let mut volume = scaled(stream.current_volume.wrapping_shl(2));
            let delta = if fading {
                scaled(stream.volume_fade_delta.wrapping_shl(2))
            } else {
                0
            };
            if fading {
                let count = limit.wrapping_sub(out0);
                stream.left_channel_volume = stream
                    .left_channel_volume
                    .wrapping_add(stream.left_volume_fade_delta.wrapping_mul(count));
                stream.right_channel_volume = stream
                    .right_channel_volume
                    .wrapping_add(stream.right_volume_fade_delta.wrapping_mul(count));
            }
            let mut out = out0;
            while out < limit {
                let sample = at(samples, sample_pos as i64)?;
                add_float(mix, out, sample * (volume as f32))?;
                volume = volume.wrapping_add(delta);
                out += 1;
                sample_pos = sample_pos.wrapping_add(step);
            }
            if fading {
                stream.current_volume = volume.wrapping_div(128) >> 2;
            }
            stream.playback_position = sample_pos.wrapping_shl(8);
            return Ok(out);
        }
        let mut left = scaled(stream.left_channel_volume.wrapping_shl(2));
        let mut right = scaled(stream.right_channel_volume.wrapping_shl(2));
        let (left_delta, right_delta) = if fading {
            (
                scaled(stream.left_volume_fade_delta.wrapping_shl(2)),
                scaled(stream.right_volume_fade_delta.wrapping_shl(2)),
            )
        } else {
            (0, 0)
        };
        if fading {
            stream.current_volume = stream
                .current_volume
                .wrapping_add(stream.volume_fade_delta.wrapping_mul(limit.wrapping_sub(out0)));
        }
        let mut out = out0.wrapping_shl(1);
        let out_limit = limit.wrapping_shl(1);
        while out < out_limit {
            let sample = at(samples, sample_pos as i64)?;
            add_float(mix, out, sample * (left as f32))?;
            left = left.wrapping_add(left_delta);
            add_float(mix, out + 1, sample * (right as f32))?;
            right = right.wrapping_add(right_delta);
            out += 2;
            sample_pos = sample_pos.wrapping_add(step);
        }
        if fading {
            stream.left_channel_volume = left.wrapping_div(128) >> 2;
            stream.right_channel_volume = right.wrapping_div(128) >> 2;
        }
        stream.playback_position = sample_pos.wrapping_shl(8);
        Ok(out >> 1)
    }

    fn interpolated(
        stream: &mut MixedAudioStream<f32>,
        samples: &[f32],
        mix: &mut [i32],
        out0: i32,
        max_out: i32,
        end_position: i32,
        wrap: f32,
        stereo: bool,
        backward: bool,
        fading: bool,
    ) -> R<i32> {
        let step = stream.sample_rate;
        let mut pos = stream.playback_position;
        let mut volume = scaled(stream.current_volume);
        let volume_delta = if fading { scaled(stream.volume_fade_delta) } else { 0 };
        let mut left = scaled(stream.left_channel_volume);
        let mut right = scaled(stream.right_channel_volume);
        let (left_delta, right_delta) = if fading {
            (
                scaled(stream.left_volume_fade_delta),
                scaled(stream.right_volume_fade_delta),
            )
        } else {
            (0, 0)
        };
        if fading {
            if stereo {
                stream.current_volume = stream
                    .current_volume
                    .wrapping_sub(out0.wrapping_mul(stream.volume_fade_delta));
            } else {
                stream.left_channel_volume = stream
                    .left_channel_volume
                    .wrapping_sub(stream.left_volume_fade_delta.wrapping_mul(out0));
                stream.right_channel_volume = stream
                    .right_channel_volume
                    .wrapping_sub(stream.right_volume_fade_delta.wrapping_mul(out0));
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
                let base = at(samples, index.wrapping_sub(1) as i64)?;
                (base, at(samples, index as i64)?)
            } else {
                let base = at(samples, index as i64)?;
                (base, at(samples, index.wrapping_add(1) as i64)?)
            };
            let value = interpolate(base, next, pos);
            if stereo {
                add(mix, out, ((value * (left as f32)) as i32) >> 6)?;
                left = left.wrapping_add(left_delta);
                add(mix, out + 1, ((value * (right as f32)) as i32) >> 6)?;
                right = right.wrapping_add(right_delta);
            } else {
                add(mix, out, ((value * (volume as f32)) as i32) >> 6)?;
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
            let current = at(samples, (pos >> 8) as i64)?;
            let (base, next) = if backward { (wrap, current) } else { (current, wrap) };
            let value = interpolate(base, next, pos);
            if stereo {
                add(mix, out, ((value * (left as f32)) as i32) >> 6)?;
                left = left.wrapping_add(left_delta);
                add(mix, out + 1, ((value * (right as f32)) as i32) >> 6)?;
                right = right.wrapping_add(right_delta);
            } else {
                add(mix, out, ((value * (volume as f32)) as i32) >> 6)?;
                volume = volume.wrapping_add(volume_delta);
            }
            out += channels;
            pos = pos.wrapping_add(step);
        }
        let result = if stereo { out >> 1 } else { out };
        if fading {
            if stereo {
                stream.current_volume = stream
                    .current_volume
                    .wrapping_add(stream.volume_fade_delta.wrapping_mul(result));
                stream.left_channel_volume = left.wrapping_div(128);
                stream.right_channel_volume = right.wrapping_div(128);
            } else {
                stream.left_channel_volume = stream
                    .left_channel_volume
                    .wrapping_add(stream.left_volume_fade_delta.wrapping_mul(result));
                stream.right_channel_volume = stream
                    .right_channel_volume
                    .wrapping_add(stream.right_volume_fade_delta.wrapping_mul(result));
                stream.current_volume = volume.wrapping_div(128);
            }
        }
        stream.playback_position = pos;
        Ok(result)
    }
}
