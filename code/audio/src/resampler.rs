use crate::reader::{at, put, AudioError, R};
use crate::synth::MixedAudioBuffer;

#[derive(Debug, Clone)]
pub struct Resampler {
    pub input_sample_rate: i32,
    pub output_sample_rate: i32,
    pub table: Option<Vec<[i32; 14]>>,
}

pub fn step(input_sample_rate: i32, output_sample_rate: i32) -> i32 {
    let mut input = input_sample_rate;
    let mut output = output_sample_rate;
    if output > input {
        std::mem::swap(&mut input, &mut output);
    }
    while output != 0 {
        let tmp = input.wrapping_rem(output);
        input = output;
        output = tmp;
    }
    input
}

fn java_div(a: i32, b: i32) -> R<i32> {
    if b == 0 {
        Err(AudioError::Invalid)
    } else {
        Ok(a.wrapping_div(b))
    }
}

fn long_scale(value: i32, numerator: i32, denominator: i32) -> R<i32> {
    if denominator == 0 {
        return Err(AudioError::Invalid);
    }
    Ok((value as i64)
        .wrapping_mul(numerator as i64)
        .wrapping_div(denominator as i64) as i32)
}

impl Resampler {
    pub fn new(input_sample_rate: i32, output_sample_rate: i32) -> R<Self> {
        let mut resampler = Self {
            input_sample_rate: 0,
            output_sample_rate: 0,
            table: None,
        };
        if output_sample_rate == input_sample_rate {
            return Ok(resampler);
        }
        let gcd = step(input_sample_rate, output_sample_rate);
        let input_rate = java_div(input_sample_rate, gcd)?;
        let output_rate = java_div(output_sample_rate, gcd)?;
        resampler.input_sample_rate = input_rate;
        resampler.output_sample_rate = output_rate;
        if input_rate < 0 {
            return Err(AudioError::Invalid);
        }
        let mut table = vec![[0i32; 14]; input_rate as usize];
        for phase in 0..input_rate {
            let coefficients = &mut table[phase as usize];
            let center = phase as f64 / input_rate as f64 + 6.0;
            let window_start = (libm::floor(center - 7.0 + 1.0) as i32).max(0);
            let window_end = (libm::ceil(7.0 + center) as i32).min(14);
            let gain = output_rate as f64 / input_rate as f64;
            for tap in window_start..window_end {
                let arg = (tap as f64 - center) * std::f64::consts::PI;
                let mut value = gain;
                if !(-1.0e-4..=1.0e-4).contains(&arg) {
                    value = gain * (libm::sin(arg) / arg);
                }
                value *= 0.54 + 0.46 * libm::cos(0.2243994752564138 * (tap as f64 - center));
                coefficients[tap as usize] = libm::floor(value * 65536.0 + 0.5) as i32;
            }
        }
        resampler.table = Some(table);
        Ok(resampler)
    }

    fn output_length(&self, size: usize) -> R<usize> {
        if self.input_sample_rate == 0 {
            return Err(AudioError::Invalid);
        }
        let length = (size as i64)
            .wrapping_mul(self.output_sample_rate as i64)
            .wrapping_div(self.input_sample_rate as i64)
            .wrapping_add(14) as i32;
        if length < 0 {
            return Err(AudioError::Invalid);
        }
        Ok(length as usize)
    }

    fn accumulate(
        &self,
        table: &[[i32; 14]],
        input: &[i32],
        weight: impl Fn(i32, i32) -> i32,
        length: usize,
    ) -> R<Vec<i32>> {
        let mut data = vec![0i32; length];
        let mut out = 0i32;
        let mut phase = 0i32;
        for sample in input {
            if phase < 0 {
                return Err(AudioError::Bounds);
            }
            let coefficients = table.get(phase as usize).ok_or(AudioError::Bounds)?;
            for (tap, coefficient) in coefficients.iter().enumerate() {
                let index = out as i64 + tap as i64;
                let current = at(&data, index)?;
                put(&mut data, index, current.wrapping_add(weight(*sample, *coefficient)))?;
            }
            phase = phase.wrapping_add(self.output_sample_rate);
            let advance = java_div(phase, self.input_sample_rate)?;
            out = out.wrapping_add(advance);
            phase = phase.wrapping_sub(advance.wrapping_mul(self.input_sample_rate));
        }
        Ok(data)
    }

    pub fn resample_bytes(&self, input: &[i8]) -> R<Vec<i8>> {
        let Some(table) = &self.table else {
            return Ok(input.to_vec());
        };
        let length = self.output_length(input.len())?;
        let wide: Vec<i32> = input.iter().map(|v| *v as i32).collect();
        let data = self.accumulate(table, &wide, |s, c| s.wrapping_mul(c), length)?;
        Ok(data
            .iter()
            .map(|v| (v.wrapping_add(32768) >> 16).clamp(-128, 127) as i8)
            .collect())
    }

    pub fn resample_shorts(&self, input: &[i16]) -> R<Vec<i16>> {
        let Some(table) = &self.table else {
            return Ok(input.to_vec());
        };
        let length = self.output_length(input.len())?;
        let wide: Vec<i32> = input.iter().map(|v| *v as i32).collect();
        let data = self.accumulate(table, &wide, |s, c| c.wrapping_mul(s) >> 2, length)?;
        Ok(data
            .iter()
            .map(|v| (v.wrapping_add(8192) >> 14).clamp(-32768, 32767) as i16)
            .collect())
    }

    pub fn scale_rate(&self, rate: i32) -> R<i32> {
        if self.table.is_some() {
            return long_scale(rate, self.output_sample_rate, self.input_sample_rate);
        }
        Ok(rate)
    }

    pub fn scale_position(&self, position: i32) -> R<i32> {
        if self.table.is_some() {
            return Ok(
                long_scale(position, self.output_sample_rate, self.input_sample_rate)?
                    .wrapping_add(6),
            );
        }
        Ok(position)
    }
}

pub fn resample_buffer(buffer: &mut MixedAudioBuffer, resampler: &Resampler) -> R<MixedAudioBuffer> {
    let samples = resampler.resample_bytes(&buffer.samples)?;
    buffer.sample_rate = resampler.scale_rate(buffer.sample_rate)?;
    if buffer.start == buffer.end {
        buffer.start = resampler.scale_position(buffer.start)?;
        buffer.end = buffer.start;
    } else {
        buffer.start = resampler.scale_position(buffer.start)?;
        buffer.end = resampler.scale_position(buffer.end)?;
        if buffer.start == buffer.end {
            buffer.start = buffer.start.wrapping_sub(1);
        }
    }
    Ok(MixedAudioBuffer::new(samples, buffer.start, buffer.end))
}
