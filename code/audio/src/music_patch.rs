use std::sync::Arc;

use crate::audio_file::AudioFile;
use crate::reader::{at, put, AudioError, Buf, R};

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct PatchInfo {
    pub release_envelope: Option<Vec<i8>>,
    pub volume_envelope_key_scaling: i32,
    pub volume_envelope_decay: i32,
    pub volume_envelope: Option<Vec<i8>>,
    pub release_envelope_key_scaling: i32,
    pub decay_key_scaling: i32,
    pub vibrato_lfo_pitch: i32,
    pub volume_envelope_release: i32,
    pub vibrato_delay: i32,
}

pub trait SoundBank {
    fn list_effect(&mut self, id: i32, rates: &mut [i32]) -> Option<Arc<AudioFile>>;
    fn list_music(&mut self, id: i32, rates: &mut [i32]) -> Option<Arc<AudioFile>>;
}

#[derive(Debug, Clone)]
pub struct MusicPatch {
    pub sample_offsets: Vec<i32>,
    pub audio_files: Vec<Option<Arc<AudioFile>>>,
    pub pitch_offset: [i16; 128],
    pub volume_offset: [i8; 128],
    pub pan_offset: [i8; 128],
    pub infos: Vec<PatchInfo>,
    pub info_of_key: [Option<usize>; 128],
    pub loop_mode: [i8; 128],
    pub base_velocity: i32,
}

pub fn divide_rounded(numerator: i32, denominator: i32) -> R<i32> {
    let sign = ((numerator as u32) >> 31) as i32;
    if denominator == 0 {
        return Err(AudioError::Invalid);
    }
    Ok(numerator.wrapping_add(sign).wrapping_div(denominator).wrapping_sub(sign))
}

fn scan(buf: &Buf) -> R<usize> {
    let mut count = 0usize;
    while at(buf.data(), buf.pos + count as i64)? != 0 {
        count += 1;
    }
    Ok(count)
}

fn signed_run(buf: &mut Buf, count: usize) -> R<Vec<i8>> {
    let mut runs = Vec::with_capacity(count);
    for _ in 0..count {
        runs.push(buf.i8()? as i8);
    }
    Ok(runs)
}

fn next_run(runs: &[i8], index: &mut usize) -> i32 {
    if *index < runs.len() {
        let value = i32::from(runs[*index]);
        *index += 1;
        value
    } else {
        -1
    }
}

fn direct(buf: &Buf, offset: &mut i64) -> R<i32> {
    let value = at(buf.data(), *offset)?;
    *offset += 1;
    Ok(i32::from(value as i8))
}

fn key_at<T: Copy>(values: &[T; 128], key: i32) -> R<T> {
    at(values, i64::from(key))
}

fn key_put<T>(values: &mut [T; 128], key: i32, value: T) -> R<()> {
    put(values, i64::from(key), value)
}

impl MusicPatch {
    pub fn new(data: &[u8]) -> R<Self> {
        let mut buf = Buf::new(data);
        let loop_mode_count = scan(&buf)?;
        let loop_mode_runs = signed_run(&mut buf, loop_mode_count)?;
        buf.pos += 1;
        let mut loop_mode_value = buf.pos;
        buf.pos += loop_mode_count as i64 + 1;

        let pan_count = scan(&buf)?;
        let pan_runs = signed_run(&mut buf, pan_count)?;
        buf.pos += 1;
        let mut pan_value = buf.pos;
        buf.pos += pan_count as i64 + 1;

        let info_run_count = scan(&buf)?;
        let info_runs = signed_run(&mut buf, info_run_count)?;
        buf.pos += 1;
        let run_total = info_run_count + 1;
        let mut run_info_indices = vec![0i8; run_total];
        let info_count;
        if run_total > 1 {
            run_info_indices[1] = 1;
            let mut info_index = 1i32;
            let mut count = 2i32;
            for slot in run_info_indices.iter_mut().take(run_total).skip(2) {
                let reference = buf.u8()?;
                if reference == 0 {
                    info_index = count;
                    count += 1;
                } else {
                    info_index = if reference <= info_index {
                        reference - 1
                    } else {
                        reference
                    };
                }
                *slot = info_index as i8;
            }
            info_count = count as usize;
        } else {
            info_count = run_total;
        }

        let mut infos = vec![PatchInfo::default(); info_count];
        for info in infos.iter_mut() {
            let mut segments = buf.u8()?;
            if segments > 0 {
                info.volume_envelope = Some(vec![0i8; (segments * 2) as usize]);
            }
            segments = buf.u8()?;
            if segments > 0 {
                let mut release = vec![0i8; (segments * 2 + 2) as usize];
                release[1] = 64;
                info.release_envelope = Some(release);
            }
        }

        let mut curve_points = buf.u8()?;
        let mut volume_curve = if curve_points > 0 {
            Some(vec![0i8; (curve_points * 2) as usize])
        } else {
            None
        };
        curve_points = buf.u8()?;
        let mut pan_curve = if curve_points > 0 {
            Some(vec![0i8; (curve_points * 2) as usize])
        } else {
            None
        };

        let key_count = scan(&buf)?;
        let key_runs = signed_run(&mut buf, key_count)?;
        buf.pos += 1;

        let mut pitch_offset = [0i16; 128];
        let mut accum = 0i32;
        for slot in pitch_offset.iter_mut() {
            accum = accum.wrapping_add(buf.u8()?);
            *slot = accum as i16;
        }
        accum = 0;
        for slot in pitch_offset.iter_mut() {
            accum = accum.wrapping_add(buf.u8()?);
            *slot = i32::from(*slot).wrapping_add(accum.wrapping_shl(8)) as i16;
        }

        let mut sample_offsets = vec![0i32; 128];
        let mut n = 0i32;
        let mut run_index = 0usize;
        let mut sample_offset = 0i32;
        for key in 0..128 {
            if n == 0 {
                n = next_run(&key_runs, &mut run_index);
                sample_offset = buf.var_int()?;
            }
            pitch_offset[key] = i32::from(pitch_offset[key])
                .wrapping_add(((sample_offset.wrapping_sub(1)) & 2).wrapping_shl(14))
                as i16;
            sample_offsets[key] = sample_offset;
            n -= 1;
        }

        let mut loop_mode = [0i8; 128];
        n = 0;
        run_index = 0;
        let mut m = 0i32;
        for key in 0..128 {
            if sample_offsets[key] != 0 {
                if n == 0 {
                    n = next_run(&loop_mode_runs, &mut run_index);
                    m = direct(&buf, &mut loop_mode_value)?.wrapping_sub(1);
                }
                loop_mode[key] = m as i8;
                n -= 1;
            }
        }

        let mut pan_offset = [0i8; 128];
        n = 0;
        run_index = 0;
        let mut p = 0i32;
        for key in 0..128 {
            if sample_offsets[key] != 0 {
                if n == 0 {
                    n = next_run(&pan_runs, &mut run_index);
                    p = direct(&buf, &mut pan_value)?.wrapping_add(16).wrapping_shl(2);
                }
                pan_offset[key] = p as i8;
                n -= 1;
            }
        }

        let mut info_of_key = [None; 128];
        n = 0;
        run_index = 0;
        let mut run_info: Option<usize> = None;
        for key in 0..128 {
            if sample_offsets[key] != 0 {
                if n == 0 {
                    let index = i32::from(at(&run_info_indices, run_index as i64)?);
                    if index < 0 || index as usize >= infos.len() {
                        return Err(AudioError::Bounds);
                    }
                    run_info = Some(index as usize);
                    n = next_run(&info_runs, &mut run_index);
                }
                info_of_key[key] = run_info;
                n -= 1;
            }
        }

        let mut volume_offset = [0i8; 128];
        n = 0;
        run_index = 0;
        let mut q = 0i32;
        for key in 0..128 {
            if n == 0 {
                n = next_run(&key_runs, &mut run_index);
                if sample_offsets[key] > 0 {
                    q = buf.u8()?.wrapping_add(1);
                }
            }
            volume_offset[key] = q as i8;
            n -= 1;
        }

        let base_velocity = buf.u8()?.wrapping_add(1);

        for info in infos.iter_mut() {
            if let Some(volume) = info.volume_envelope.as_mut() {
                let mut index = 1;
                while index < volume.len() {
                    volume[index] = buf.i8()? as i8;
                    index += 2;
                }
            }
            if let Some(release) = info.release_envelope.as_mut() {
                let mut index = 3;
                while index + 2 < release.len() {
                    release[index] = buf.i8()? as i8;
                    index += 2;
                }
            }
        }

        if let Some(curve) = volume_curve.as_mut() {
            let mut index = 1;
            while index < curve.len() {
                curve[index] = buf.i8()? as i8;
                index += 2;
            }
        }
        if let Some(curve) = pan_curve.as_mut() {
            let mut index = 1;
            while index < curve.len() {
                curve[index] = buf.i8()? as i8;
                index += 2;
            }
        }

        for info in infos.iter_mut() {
            if let Some(release) = info.release_envelope.as_mut() {
                accum = 0;
                let mut index = 2;
                while index < release.len() {
                    accum = 1i32.wrapping_add(accum).wrapping_add(buf.u8()?);
                    release[index] = accum as i8;
                    index += 2;
                }
            }
        }
        for info in infos.iter_mut() {
            if let Some(volume) = info.volume_envelope.as_mut() {
                accum = 0;
                let mut index = 2;
                while index < volume.len() {
                    accum = accum.wrapping_add(1).wrapping_add(buf.u8()?);
                    volume[index] = accum as i8;
                    index += 2;
                }
            }
        }

        if let Some(curve) = volume_curve.as_mut() {
            accum = buf.u8()?;
            curve[0] = accum as i8;
            let mut index = 2;
            while index < curve.len() {
                accum = 1i32.wrapping_add(accum).wrapping_add(buf.u8()?);
                curve[index] = accum as i8;
                index += 2;
            }
            let mut start_key = i32::from(curve[0]);
            let mut start_volume = i32::from(curve[1]);
            for key in 0..start_key {
                let current = i32::from(key_at(&volume_offset, key)?);
                key_put(
                    &mut volume_offset,
                    key,
                    (start_volume.wrapping_mul(current).wrapping_add(32) >> 6) as i8,
                )?;
            }
            let mut index = 2;
            while index < curve.len() {
                let end_key = i32::from(curve[index]);
                let end_volume = i32::from(curve[index + 1]);
                let span = end_key.wrapping_sub(start_key);
                let mut interp = start_volume
                    .wrapping_mul(span)
                    .wrapping_add(span.wrapping_div(2));
                for key in start_key..end_key {
                    let value = divide_rounded(interp, span)?;
                    let current = i32::from(key_at(&volume_offset, key)?);
                    key_put(
                        &mut volume_offset,
                        key,
                        (value.wrapping_mul(current).wrapping_add(32) >> 6) as i8,
                    )?;
                    interp = interp.wrapping_add(end_volume.wrapping_sub(start_volume));
                }
                start_key = end_key;
                start_volume = end_volume;
                index += 2;
            }
            for key in start_key..128 {
                let current = i32::from(key_at(&volume_offset, key)?);
                key_put(
                    &mut volume_offset,
                    key,
                    (start_volume.wrapping_mul(current).wrapping_add(32) >> 6) as i8,
                )?;
            }
        }

        if let Some(curve) = pan_curve.as_mut() {
            accum = buf.u8()?;
            curve[0] = accum as i8;
            let mut index = 2;
            while index < curve.len() {
                accum = 1i32.wrapping_add(accum).wrapping_add(buf.u8()?);
                curve[index] = accum as i8;
                index += 2;
            }
            let mut start_key = i32::from(curve[0]);
            let mut start_pan = i32::from(curve[1]).wrapping_shl(1);
            for key in 0..start_key {
                let current = i32::from(key_at(&pan_offset, key)? as u8);
                key_put(
                    &mut pan_offset,
                    key,
                    start_pan.wrapping_add(current).clamp(0, 128) as i8,
                )?;
            }
            let mut index = 2;
            while index < curve.len() {
                let end_key = i32::from(curve[index]);
                let end_pan = i32::from(curve[index + 1]).wrapping_shl(1);
                let span = end_key.wrapping_sub(start_key);
                let mut interp = span
                    .wrapping_mul(start_pan)
                    .wrapping_add(span.wrapping_div(2));
                for key in start_key..end_key {
                    let value = divide_rounded(interp, span)?;
                    let current = i32::from(key_at(&pan_offset, key)? as u8);
                    key_put(
                        &mut pan_offset,
                        key,
                        current.wrapping_add(value).clamp(0, 128) as i8,
                    )?;
                    interp = interp.wrapping_add(end_pan.wrapping_sub(start_pan));
                }
                start_key = end_key;
                start_pan = end_pan;
                index += 2;
            }
            for key in start_key..128 {
                let current = i32::from(key_at(&pan_offset, key)? as u8);
                key_put(
                    &mut pan_offset,
                    key,
                    current.wrapping_add(start_pan).clamp(0, 128) as i8,
                )?;
            }
        }

        for info in infos.iter_mut() {
            info.volume_envelope_decay = buf.u8()?;
        }
        for info in infos.iter_mut() {
            if info.volume_envelope.is_some() {
                info.volume_envelope_key_scaling = buf.u8()?;
            }
            if info.release_envelope.is_some() {
                info.release_envelope_key_scaling = buf.u8()?;
            }
            if info.volume_envelope_decay > 0 {
                info.decay_key_scaling = buf.u8()?;
            }
        }
        for info in infos.iter_mut() {
            info.volume_envelope_release = buf.u8()?;
        }
        for info in infos.iter_mut() {
            if info.volume_envelope_release > 0 {
                info.vibrato_lfo_pitch = buf.u8()?;
            }
        }
        for info in infos.iter_mut() {
            if info.vibrato_lfo_pitch > 0 {
                info.vibrato_delay = buf.u8()?;
            }
        }

        Ok(Self {
            sample_offsets,
            audio_files: vec![None; 128],
            pitch_offset,
            volume_offset,
            pan_offset,
            infos,
            info_of_key,
            loop_mode,
            base_velocity,
        })
    }

    pub fn ready(
        &mut self,
        bank: &mut dyn SoundBank,
        data: Option<&[u8; 128]>,
        rates: &mut [i32],
    ) -> bool {
        let mut ready = true;
        let mut file = 0i32;
        let mut audio: Option<Arc<AudioFile>> = None;
        for index in 0..128usize {
            if data.is_none_or(|d| d[index] != 0) {
                let Some(&offset) = self.sample_offsets.get(index) else {
                    continue;
                };
                if offset != 0 {
                    if offset != file {
                        let var = offset - 1;
                        file = offset;
                        audio = if var & 1 == 0 {
                            bank.list_effect(var >> 2, rates)
                        } else {
                            bank.list_music(var >> 2, rates)
                        };
                        if audio.is_none() {
                            ready = false;
                        }
                    }
                    if let Some(audio) = &audio {
                        self.audio_files[index] = Some(audio.clone());
                        self.sample_offsets[index] = 0;
                    }
                }
            }
        }
        ready
    }

    pub fn reset(&mut self) {
        self.sample_offsets = Vec::new();
    }
}
