use crate::reader::{at, put, AudioError, Buf, R};
use std::sync::OnceLock;

pub const OUTPUT_LEN: usize = 220_500;
pub const SAMPLE_RATE: i32 = 22_050;

struct Tables {
    noise: Vec<i32>,
    sine: Vec<i32>,
}

fn tables() -> &'static Tables {
    static TABLES: OnceLock<Tables> = OnceLock::new();
    TABLES.get_or_init(|| {
        let mut seed: u64 = 0x5DEE_CE66D & ((1u64 << 48) - 1);
        let mut noise = Vec::with_capacity(32768);
        for _ in 0..32768 {
            seed = (seed.wrapping_mul(0x5DEE_CE66D).wrapping_add(0xB)) & ((1u64 << 48) - 1);
            let next = (seed >> 16) as u32 as i32;
            noise.push((next & 2) - 1);
        }
        let sine = (0..32768)
            .map(|i| (libm::sin(f64::from(i) / 5215.1903) * 16384.0) as i32)
            .collect();
        Tables { noise, sine }
    })
}

#[derive(Debug, Clone)]
pub struct Envelope {
    pub form: i32,
    pub start: i32,
    pub end: i32,
    critical: i32,
    phase_index: i32,
    step: i32,
    amplitude: i32,
    ticks: i32,
    pub phase_duration: Vec<i32>,
    pub phase_peak: Vec<i32>,
}

impl Default for Envelope {
    fn default() -> Self {
        Self {
            form: 0,
            start: 0,
            end: 0,
            critical: 0,
            phase_index: 0,
            step: 0,
            amplitude: 0,
            ticks: 0,
            phase_duration: vec![0, 65535],
            phase_peak: vec![0, 65535],
        }
    }
}

impl Envelope {
    pub fn decode(buf: &mut Buf) -> R<Self> {
        let mut envelope = Self {
            form: buf.u8()?,
            start: buf.i32()?,
            end: buf.i32()?,
            ..Self::default()
        };
        envelope.decode_shape(buf)?;
        Ok(envelope)
    }

    pub fn decode_shape(&mut self, buf: &mut Buf) -> R<()> {
        let count = buf.u8()?;
        self.phase_duration = vec![0; count as usize];
        self.phase_peak = vec![0; count as usize];
        for i in 0..count as usize {
            self.phase_duration[i] = buf.u16()?;
            self.phase_peak[i] = buf.u16()?;
        }
        Ok(())
    }

    pub fn reset(&mut self) {
        self.critical = 0;
        self.phase_index = 0;
        self.step = 0;
        self.amplitude = 0;
        self.ticks = 0;
    }

    pub fn step(&mut self, period: i32) -> R<i32> {
        if self.ticks >= self.critical {
            let peak = at(&self.phase_peak, i64::from(self.phase_index))?;
            self.phase_index += 1;
            self.amplitude = peak.wrapping_shl(15);
            let count = self.phase_peak.len() as i32;
            if self.phase_index >= count {
                self.phase_index = count - 1;
            }
            let duration = at(&self.phase_duration, i64::from(self.phase_index))?;
            self.critical = (f64::from(duration) / 65536.0 * f64::from(period)) as i32;
            if self.critical > self.ticks {
                let target = at(&self.phase_peak, i64::from(self.phase_index))?.wrapping_shl(15);
                self.step = target.wrapping_sub(self.amplitude) / (self.critical - self.ticks);
            }
        }
        self.amplitude = self.amplitude.wrapping_add(self.step);
        self.ticks = self.ticks.wrapping_add(1);
        Ok(self.amplitude.wrapping_sub(self.step) >> 15)
    }
}

#[derive(Debug, Clone, Default)]
pub struct Filter {
    pub num_pairs: [i32; 2],
    pair_phase: [[[i32; 4]; 2]; 2],
    pair_magnitude: [[[i32; 4]; 2]; 2],
    pub unity: [i32; 2],
}

#[derive(Default)]
struct FilterState {
    coefficient_float: [[f32; 8]; 2],
    coefficient_int: [[i32; 8]; 2],
    inv_unity_float: f32,
    inv_unity_int: i32,
}

fn normalize(octaves: f32) -> f32 {
    let frequency = 32.703197_f32 * (libm::pow(2.0, f64::from(octaves)) as f32);
    frequency * std::f32::consts::PI / 11025.0_f32
}

impl Filter {
    fn adapt_magnitude(&self, dir: usize, pair: usize, t: f32) -> f32 {
        let low = self.pair_magnitude[dir][0][pair];
        let high = self.pair_magnitude[dir][1][pair];
        let mut attenuation_db = low as f32 + t * (high.wrapping_sub(low) as f32);
        attenuation_db *= 0.0015258789_f32;
        1.0_f32 - (libm::pow(10.0, f64::from(-attenuation_db) / 20.0) as f32)
    }

    fn adapt_phase(&self, dir: usize, pair: usize, t: f32) -> f32 {
        let low = self.pair_phase[dir][0][pair];
        let high = self.pair_phase[dir][1][pair];
        let mut octaves = low as f32 + t * (high.wrapping_sub(low) as f32);
        octaves *= 1.2207031E-4_f32;
        normalize(octaves)
    }

    pub fn decode(&mut self, buf: &mut Buf, envelope: &mut Envelope) -> R<()> {
        let count = buf.u8()?;
        self.num_pairs[0] = count >> 4;
        self.num_pairs[1] = count & 0xf;
        if count != 0 {
            self.unity[0] = buf.u16()?;
            self.unity[1] = buf.u16()?;
            let varying_mask = buf.u8()?;
            for dir in 0..2usize {
                for pair in 0..self.num_pairs[dir] {
                    if pair >= 4 {
                        return Err(AudioError::Bounds);
                    }
                    self.pair_phase[dir][0][pair as usize] = buf.u16()?;
                    self.pair_magnitude[dir][0][pair as usize] = buf.u16()?;
                }
            }
            for dir in 0..2usize {
                for pair in 0..self.num_pairs[dir] {
                    let p = pair as usize;
                    let bit = (1i32.wrapping_shl((dir * 4) as u32)).wrapping_shl(pair as u32);
                    if varying_mask & bit != 0 {
                        self.pair_phase[dir][1][p] = buf.u16()?;
                        self.pair_magnitude[dir][1][p] = buf.u16()?;
                    } else {
                        self.pair_phase[dir][1][p] = self.pair_phase[dir][0][p];
                        self.pair_magnitude[dir][1][p] = self.pair_magnitude[dir][0][p];
                    }
                }
            }
            if varying_mask != 0 || self.unity[1] != self.unity[0] {
                envelope.decode_shape(buf)?;
            }
        } else {
            self.unity = [0, 0];
        }
        Ok(())
    }

    fn compute(&self, state: &mut FilterState, dir: usize, t: f32) -> i32 {
        if dir == 0 {
            let mut unity_db = self.unity[0] as f32 + (self.unity[1].wrapping_sub(self.unity[0]) as f32) * t;
            unity_db *= 0.0030517578_f32;
            state.inv_unity_float = libm::pow(0.1, f64::from(unity_db) / 20.0) as f32;
            state.inv_unity_int = (state.inv_unity_float * 65536.0_f32) as i32;
        }
        let pairs = self.num_pairs[dir] as usize;
        if pairs == 0 {
            return 0;
        }
        let coef = &mut state.coefficient_float[dir];
        let mut magnitude = self.adapt_magnitude(dir, 0, t);
        coef[0] = -2.0_f32 * magnitude * (libm::cos(f64::from(self.adapt_phase(dir, 0, t))) as f32);
        coef[1] = magnitude * magnitude;
        for pair in 1..pairs {
            magnitude = self.adapt_magnitude(dir, pair, t);
            let coeff1 =
                -2.0_f32 * magnitude * (libm::cos(f64::from(self.adapt_phase(dir, pair, t))) as f32);
            let coeff2 = magnitude * magnitude;
            coef[pair * 2 + 1] = coef[pair * 2 - 1] * coeff2;
            coef[pair * 2] = coef[pair * 2 - 1] * coeff1 + coef[pair * 2 - 2] * coeff2;
            let mut i = pair * 2 - 1;
            while i >= 2 {
                coef[i] += coef[i - 1] * coeff1 + coef[i - 2] * coeff2;
                i -= 1;
            }
            coef[1] += coef[0] * coeff1 + coeff2;
            coef[0] += coeff1;
        }
        if dir == 0 {
            let inv = state.inv_unity_float;
            for value in state.coefficient_float[0].iter_mut().take(pairs * 2) {
                *value *= inv;
            }
        }
        for i in 0..pairs * 2 {
            state.coefficient_int[dir][i] = (state.coefficient_float[dir][i] * 65536.0_f32) as i32;
        }
        (pairs * 2) as i32
    }
}

#[derive(Debug, Clone)]
pub struct Instrument {
    pub pitch: Envelope,
    pub volume: Envelope,
    pub pitch_modifier: Option<(Envelope, Envelope)>,
    pub volume_multiplier: Option<(Envelope, Envelope)>,
    pub release_attack: Option<(Envelope, Envelope)>,
    pub delay_time: i32,
    pub delay_decay: i32,
    pub duration: i32,
    pub offset: i32,
    pub filter: Filter,
    pub filter_envelope: Envelope,
    pub oscillator_volume: [i32; 5],
    pub oscillator_pitch: [i32; 5],
    pub oscillator_delays: [i32; 5],
}

fn optional_pair(buf: &mut Buf) -> R<Option<(Envelope, Envelope)>> {
    let option = buf.u8()?;
    if option == 0 {
        return Ok(None);
    }
    buf.pos -= 1;
    let first = Envelope::decode(buf)?;
    let second = Envelope::decode(buf)?;
    Ok(Some((first, second)))
}

impl Instrument {
    pub fn decode(buf: &mut Buf) -> R<Self> {
        let pitch = Envelope::decode(buf)?;
        let volume = Envelope::decode(buf)?;
        let pitch_modifier = optional_pair(buf)?;
        let volume_multiplier = optional_pair(buf)?;
        let release_attack = optional_pair(buf)?;
        let mut oscillator_volume = [0; 5];
        let mut oscillator_pitch = [0; 5];
        let mut oscillator_delays = [0; 5];
        for i in 0..10usize {
            let volume = buf.usmart()?;
            if volume == 0 {
                break;
            }
            put(&mut oscillator_volume, i as i64, volume)?;
            put(&mut oscillator_pitch, i as i64, buf.smart()?)?;
            put(&mut oscillator_delays, i as i64, buf.usmart()?)?;
        }
        let delay_time = buf.usmart()?;
        let delay_decay = buf.usmart()?;
        let duration = buf.u16()?;
        let offset = buf.u16()?;
        let mut filter = Filter::default();
        let mut filter_envelope = Envelope::default();
        filter.decode(buf, &mut filter_envelope)?;
        Ok(Self {
            pitch,
            volume,
            pitch_modifier,
            volume_multiplier,
            release_attack,
            delay_time,
            delay_decay,
            duration,
            offset,
            filter,
            filter_envelope,
            oscillator_volume,
            oscillator_pitch,
            oscillator_delays,
        })
    }
}

fn evaluate_wave(phase: i32, amplitude: i32, form: i32) -> i32 {
    match form {
        1 => {
            if (phase & 0x7fff) < 16384 {
                amplitude
            } else {
                amplitude.wrapping_neg()
            }
        }
        2 => tables().sine[(phase & 0x7fff) as usize]
            .wrapping_mul(amplitude)
            >> 14,
        3 => (amplitude.wrapping_mul(phase & 0x7fff) >> 14).wrapping_sub(amplitude),
        4 => amplitude.wrapping_mul(tables().noise[((phase / 2607) & 0x7fff) as usize]),
        _ => 0,
    }
}

pub struct Synth {
    output: Vec<i32>,
    filter_state: FilterState,
}

impl Default for Synth {
    fn default() -> Self {
        Self::new()
    }
}

impl Synth {
    pub fn new() -> Self {
        Self {
            output: vec![0; OUTPUT_LEN],
            filter_state: FilterState::default(),
        }
    }

    pub fn output(&self) -> &[i32] {
        &self.output
    }

    fn out_at(&self, index: i64) -> R<i32> {
        at(&self.output, index)
    }

    pub fn synthesize(
        &mut self,
        instrument: &mut Instrument,
        mix_duration: i32,
        instr_duration: i32,
    ) -> R<()> {
        if mix_duration < 0 || mix_duration as usize > OUTPUT_LEN {
            return Err(AudioError::Bounds);
        }
        self.output[..mix_duration as usize].fill(0);
        if instr_duration < 10 {
            return Ok(());
        }
        let fs = f64::from(mix_duration) / (f64::from(instr_duration) + 0.0);
        instrument.pitch.reset();
        instrument.volume.reset();
        let mut pitch_mod_step = 0i32;
        let mut pitch_mod_base_step = 0i32;
        let mut pitch_mod_phase = 0i32;
        if let Some((modifier, amplitude)) = instrument.pitch_modifier.as_mut() {
            modifier.reset();
            amplitude.reset();
            pitch_mod_step =
                (f64::from(modifier.end.wrapping_sub(modifier.start)) * 32.768 / fs) as i32;
            pitch_mod_base_step = (f64::from(modifier.start) * 32.768 / fs) as i32;
        }
        let mut volume_mod_step = 0i32;
        let mut volume_mod_base_step = 0i32;
        let mut volume_mod_phase = 0i32;
        if let Some((multiplier, amplitude)) = instrument.volume_multiplier.as_mut() {
            multiplier.reset();
            amplitude.reset();
            volume_mod_step =
                (f64::from(multiplier.end.wrapping_sub(multiplier.start)) * 32.768 / fs) as i32;
            volume_mod_base_step = (f64::from(multiplier.start) * 32.768 / fs) as i32;
        }
        let mut phases = [0i32; 5];
        let mut delays = [0i32; 5];
        let mut volume_step = [0i32; 5];
        let mut pitch_step = [0i32; 5];
        let mut pitch_base_step = [0i32; 5];
        for i in 0..5 {
            if instrument.oscillator_volume[i] != 0 {
                phases[i] = 0;
                delays[i] = (f64::from(instrument.oscillator_delays[i]) * fs) as i32;
                volume_step[i] = (instrument.oscillator_volume[i].wrapping_shl(14)) / 100;
                pitch_step[i] = (f64::from(instrument.pitch.end.wrapping_sub(instrument.pitch.start))
                    * 32.768
                    * libm::pow(1.0057929410678534, f64::from(instrument.oscillator_pitch[i]))
                    / fs) as i32;
                pitch_base_step[i] = (f64::from(instrument.pitch.start) * 32.768 / fs) as i32;
            }
        }
        for i in 0..mix_duration {
            let mut pitch_change = instrument.pitch.step(mix_duration)?;
            let mut volume_change = instrument.volume.step(mix_duration)?;
            if let Some((modifier, amplitude)) = instrument.pitch_modifier.as_mut() {
                let modulation = modifier.step(mix_duration)?;
                let mod_amplitude = amplitude.step(mix_duration)?;
                pitch_change = pitch_change
                    .wrapping_add(evaluate_wave(pitch_mod_phase, mod_amplitude, modifier.form) >> 1);
                pitch_mod_phase = pitch_mod_phase.wrapping_add(
                    pitch_mod_base_step
                        .wrapping_add(modulation.wrapping_mul(pitch_mod_step) >> 16),
                );
            }
            if let Some((multiplier, amplitude)) = instrument.volume_multiplier.as_mut() {
                let modulation = multiplier.step(mix_duration)?;
                let mod_amplitude = amplitude.step(mix_duration)?;
                volume_change = volume_change.wrapping_mul(
                    (evaluate_wave(volume_mod_phase, mod_amplitude, multiplier.form) >> 1)
                        .wrapping_add(32768),
                ) >> 15;
                volume_mod_phase = volume_mod_phase.wrapping_add(
                    volume_mod_base_step
                        .wrapping_add(modulation.wrapping_mul(volume_mod_step) >> 16),
                );
            }
            for j in 0..5 {
                if instrument.oscillator_volume[j] != 0 {
                    let delayed_index = delays[j].wrapping_add(i);
                    if delayed_index < mix_duration {
                        let wave = evaluate_wave(
                            phases[j],
                            volume_change.wrapping_mul(volume_step[j]) >> 15,
                            instrument.pitch.form,
                        );
                        let slot = delayed_index as i64;
                        let current = self.out_at(slot)?;
                        put(&mut self.output, slot, current.wrapping_add(wave))?;
                        phases[j] = phases[j].wrapping_add(
                            (pitch_change.wrapping_mul(pitch_step[j]) >> 16)
                                .wrapping_add(pitch_base_step[j]),
                        );
                    }
                }
            }
        }
        if let Some((release, attack)) = instrument.release_attack.as_mut() {
            release.reset();
            attack.reset();
            let mut counter = 0i32;
            let mut muted = true;
            for i in 0..mix_duration {
                let release_step = release.step(mix_duration)?;
                let attack_step = attack.step(mix_duration)?;
                let span = release.end.wrapping_sub(release.start);
                let threshold = if muted {
                    (release_step.wrapping_mul(span) >> 8).wrapping_add(release.start)
                } else {
                    (attack_step.wrapping_mul(span) >> 8).wrapping_add(release.start)
                };
                counter = counter.wrapping_add(256);
                if counter >= threshold {
                    counter = 0;
                    muted = !muted;
                }
                if muted {
                    put(&mut self.output, i64::from(i), 0)?;
                }
            }
        }
        if instrument.delay_time > 0 && instrument.delay_decay > 0 {
            let delay = (f64::from(instrument.delay_time) * fs) as i32;
            for i in delay..mix_duration {
                let source = self.out_at(i64::from(i) - i64::from(delay))?;
                let current = self.out_at(i64::from(i))?;
                put(
                    &mut self.output,
                    i64::from(i),
                    current.wrapping_add(source.wrapping_mul(instrument.delay_decay) / 100),
                )?;
            }
        }
        if instrument.filter.num_pairs[0] > 0 || instrument.filter.num_pairs[1] > 0 {
            self.run_filter(instrument, mix_duration)?;
        }
        for i in 0..mix_duration as usize {
            let value = self.output[i];
            self.output[i] = value.clamp(-32768, 32767);
        }
        Ok(())
    }

    fn tap(&self, index: i64, coefficient: i32) -> R<i32> {
        Ok(((i64::from(self.out_at(index)?)).wrapping_mul(i64::from(coefficient)) >> 16) as i32)
    }

    fn run_filter(&mut self, instrument: &mut Instrument, mix_duration: i32) -> R<()> {
        let filter = instrument.filter.clone();
        let envelope = &mut instrument.filter_envelope;
        envelope.reset();
        let mut t = envelope.step(mix_duration.wrapping_add(1))?;
        let mut m = filter.compute(&mut self.filter_state, 0, t as f32 / 65536.0_f32);
        let mut n_taps = filter.compute(&mut self.filter_state, 1, t as f32 / 65536.0_f32);
        if mix_duration >= m + n_taps {
            let mut n = 0i32;
            let mut delay = n_taps;
            if n_taps > mix_duration - m {
                delay = mix_duration - m;
            }
            while n < delay {
                let mut y = self.tap(i64::from(n) + i64::from(m), self.filter_state.inv_unity_int)?;
                for i in 0..m {
                    y = y.wrapping_add(self.tap(
                        i64::from(n) + i64::from(m) - 1 - i64::from(i),
                        self.filter_state.coefficient_int[0][i as usize],
                    )?);
                }
                for i in 0..n {
                    y = y.wrapping_sub(self.tap(
                        i64::from(n) - 1 - i64::from(i),
                        self.filter_state.coefficient_int[1][i as usize],
                    )?);
                }
                put(&mut self.output, i64::from(n), y)?;
                t = envelope.step(mix_duration.wrapping_add(1))?;
                n += 1;
            }
            delay = 128;
            loop {
                if delay > mix_duration - m {
                    delay = mix_duration - m;
                }
                while n < delay {
                    let mut y =
                        self.tap(i64::from(n) + i64::from(m), self.filter_state.inv_unity_int)?;
                    for i in 0..m {
                        y = y.wrapping_add(self.tap(
                            i64::from(n) + i64::from(m) - 1 - i64::from(i),
                            self.filter_state.coefficient_int[0][i as usize],
                        )?);
                    }
                    for i in 0..n_taps {
                        y = y.wrapping_sub(self.tap(
                            i64::from(n) - 1 - i64::from(i),
                            self.filter_state.coefficient_int[1][i as usize],
                        )?);
                    }
                    put(&mut self.output, i64::from(n), y)?;
                    t = envelope.step(mix_duration.wrapping_add(1))?;
                    n += 1;
                }
                if n >= mix_duration - m {
                    while n < mix_duration {
                        let mut y = 0i32;
                        for i in (n + m - mix_duration)..m {
                            y = y.wrapping_add(self.tap(
                                i64::from(n) + i64::from(m) - 1 - i64::from(i),
                                self.filter_state.coefficient_int[0][i as usize],
                            )?);
                        }
                        for i in 0..n_taps {
                            y = y.wrapping_sub(self.tap(
                                i64::from(n) - 1 - i64::from(i),
                                self.filter_state.coefficient_int[1][i as usize],
                            )?);
                        }
                        put(&mut self.output, i64::from(n), y)?;
                        envelope.step(mix_duration.wrapping_add(1))?;
                        n += 1;
                    }
                    break;
                }
                m = filter.compute(&mut self.filter_state, 0, t as f32 / 65536.0_f32);
                n_taps = filter.compute(&mut self.filter_state, 1, t as f32 / 65536.0_f32);
                delay += 128;
            }
        }
        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct MixedAudioBuffer {
    pub samples: Vec<i8>,
    pub start: i32,
    pub end: i32,
    pub sample_rate: i32,
    pub enable_loop: bool,
}

impl MixedAudioBuffer {
    pub fn new(samples: Vec<i8>, start: i32, end: i32) -> Self {
        Self {
            samples,
            start,
            end,
            sample_rate: SAMPLE_RATE,
            enable_loop: false,
        }
    }

    pub fn total_samples(&self) -> i32 {
        self.samples.len() as i32
    }
}

#[derive(Debug, Clone)]
pub struct SoundEffect {
    pub instruments: [Option<Instrument>; 10],
    pub loop_begin: i32,
    pub loop_end: i32,
}

impl SoundEffect {
    pub fn decode(data: &[u8]) -> R<Self> {
        let mut buf = Buf::new(data);
        let mut instruments: [Option<Instrument>; 10] = Default::default();
        for slot in instruments.iter_mut() {
            let present = buf.u8()?;
            if present != 0 {
                buf.pos -= 1;
                *slot = Some(Instrument::decode(&mut buf)?);
            }
        }
        let loop_begin = buf.u16()?;
        let loop_end = buf.u16()?;
        Ok(Self {
            instruments,
            loop_begin,
            loop_end,
        })
    }

    pub fn delay(&mut self) -> i32 {
        let mut delay = 9_999_999;
        for instrument in self.instruments.iter().flatten() {
            if instrument.offset / 20 < delay {
                delay = instrument.offset / 20;
            }
        }
        if self.loop_begin < self.loop_end && self.loop_begin / 20 < delay {
            delay = self.loop_begin / 20;
        }
        if delay != 9_999_999 && delay != 0 {
            for instrument in self.instruments.iter_mut().flatten() {
                instrument.offset = instrument.offset.wrapping_sub(delay.wrapping_mul(20));
            }
            if self.loop_begin < self.loop_end {
                self.loop_begin = self.loop_begin.wrapping_sub(delay.wrapping_mul(20));
                self.loop_end = self.loop_end.wrapping_sub(delay.wrapping_mul(20));
            }
            return delay;
        }
        0
    }

    pub fn mix(&mut self, synth: &mut Synth) -> R<Vec<i8>> {
        let mut duration = 0i32;
        for instrument in self.instruments.iter().flatten() {
            let end = instrument.duration.wrapping_add(instrument.offset);
            if end > duration {
                duration = end;
            }
        }
        if duration == 0 {
            return Ok(Vec::new());
        }
        let ns = duration.wrapping_mul(SAMPLE_RATE) / 1000;
        if ns < 0 {
            return Err(AudioError::Invalid);
        }
        let mut mixed = vec![0i8; ns as usize];
        for instrument in self.instruments.iter_mut().flatten() {
            let mix_duration = instrument.duration.wrapping_mul(SAMPLE_RATE) / 1000;
            let offset = instrument.offset.wrapping_mul(SAMPLE_RATE) / 1000;
            let instr_duration = instrument.duration;
            synth.synthesize(instrument, mix_duration, instr_duration)?;
            for j in 0..mix_duration {
                let slot = i64::from(j) + i64::from(offset);
                let mut out = (synth.output[j as usize] >> 8).wrapping_add(i32::from(at(&mixed, slot)?));
                if (out.wrapping_add(128) & -256) != 0 {
                    out = (out >> 31) ^ 0x7f;
                }
                put(&mut mixed, slot, out as i8)?;
            }
        }
        Ok(mixed)
    }

    pub fn mixed_audio(&mut self, synth: &mut Synth) -> R<MixedAudioBuffer> {
        let samples = self.mix(synth)?;
        Ok(MixedAudioBuffer::new(
            samples,
            self.loop_begin.wrapping_mul(SAMPLE_RATE) / 1000,
            self.loop_end.wrapping_mul(SAMPLE_RATE) / 1000,
        ))
    }
}
