use crate::reader::{at_slice, Buf, ModelError, R};
use std::collections::HashMap;

pub const DISPLACE_X: i32 = 0x1;
pub const DISPLACE_Y: i32 = 0x2;
pub const DISPLACE_Z: i32 = 0x4;
pub const ALTERS_VERTICES: i32 = 0x20;
pub const ALTERS_COLOR: i32 = 0x80;
pub const ALTERS_ALPHA: i32 = 0x100;
pub const ALTERS_LIGHT: i32 = 0x200;
pub const ALTERS_BILLBOARDS: i32 = 0x400;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AnimBase {
    pub id: i32,
    pub num_transformation: i32,
    pub transformation_type: Vec<i32>,
    pub labels: Vec<Vec<i32>>,
    pub shadowed: Vec<bool>,
    pub submeshes: Vec<i32>,
}

impl AnimBase {
    pub fn decode(id: i32, data: &[u8]) -> R<Self> {
        let mut stream = Buf::new(data);
        let count = stream.u8()?;
        let n = count as usize;
        let mut transformation_type = vec![0; n];
        let mut labels = vec![Vec::new(); n];
        let mut shadowed = vec![false; n];
        let mut submeshes = vec![0; n];
        for kind in transformation_type.iter_mut() {
            *kind = stream.u8()?;
            if *kind == 6 {
                *kind = 2;
            }
        }
        for flag in shadowed.iter_mut() {
            *flag = stream.u8()? == 1;
        }
        for submesh in submeshes.iter_mut() {
            *submesh = stream.u16()?;
        }
        for label in labels.iter_mut() {
            *label = vec![0; stream.u8()? as usize];
        }
        for label in labels.iter_mut() {
            for slot in label.iter_mut() {
                *slot = stream.u8()?;
            }
        }
        Ok(Self {
            id,
            num_transformation: count,
            transformation_type,
            labels,
            shadowed,
            submeshes,
        })
    }
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct AnimFrame {
    pub num_transformations: i32,
    pub modifies_alpha: bool,
    pub modifies_color: bool,
    pub modifies_billboard: bool,
    pub base_indices: Option<Vec<i16>>,
    pub transformation_x: Option<Vec<i16>>,
    pub transformation_y: Option<Vec<i16>>,
    pub transformation_z: Option<Vec<i16>>,
    pub labels: Option<Vec<i16>>,
    pub tweening_properties: Option<Vec<i8>>,
}

const SCRATCH: usize = 500;

impl AnimFrame {
    pub fn decode(data: &[u8], base: &AnimBase) -> Self {
        let mut frame = Self::default();
        if frame.try_decode(data, base).is_err() {
            frame.num_transformations = 0;
            frame.modifies_alpha = false;
            frame.modifies_color = false;
        }
        frame
    }

    fn try_decode(&mut self, data: &[u8], base: &AnimBase) -> R<()> {
        let mut attrib = Buf::new(data);
        let mut trans = Buf::new(data);
        attrib.u8()?;
        attrib.pos += 2;
        let count = attrib.u8()?;
        let mut used = 0usize;
        let mut last: i32 = -1;
        let mut last_used: i32 = -1;
        trans.pos = i64::from(count) + attrib.pos;
        let mut indices = Vec::new();
        let mut xs: Vec<i16> = Vec::new();
        let mut ys: Vec<i16> = Vec::new();
        let mut zs: Vec<i16> = Vec::new();
        let mut skipped: Vec<i16> = Vec::new();
        let mut flags: Vec<i8> = Vec::new();
        for i in 0..count {
            let kind = at_slice(&base.transformation_type, i64::from(i))?;
            if kind == 0 {
                last = i;
            }
            let attribute = attrib.u8()?;
            if attribute > 0 {
                if kind == 0 {
                    last_used = i;
                }
                if used >= SCRATCH {
                    return Err(ModelError::Bounds);
                }
                indices.push(i as i16);
                let value: i16 = if kind == 3 || kind == 10 { 128 } else { 0 };
                let mut x = if attribute & DISPLACE_X != 0 {
                    trans.smart()? as i16
                } else {
                    value
                };
                let mut y = if attribute & DISPLACE_Y != 0 {
                    trans.smart()? as i16
                } else {
                    value
                };
                let mut z = if attribute & DISPLACE_Z != 0 {
                    trans.smart()? as i16
                } else {
                    value
                };
                flags.push(((attribute >> 3) & 0x3) as i8);
                if kind == 2 || kind == 9 {
                    x = ((i32::from(x) << 2) & 0x3fff) as i16;
                    y = ((i32::from(y) << 2) & 0x3fff) as i16;
                    z = ((i32::from(z) << 2) & 0x3fff) as i16;
                }
                xs.push(x);
                ys.push(y);
                zs.push(z);
                let mut skip: i16 = -1;
                if kind != 1 && kind != 2 && kind != 3 {
                    if kind == 5 {
                        self.modifies_alpha = true;
                    } else if kind == 7 {
                        self.modifies_color = true;
                    } else if kind == 9 || kind == 10 || kind == 8 {
                        self.modifies_billboard = true;
                    }
                } else if last > last_used {
                    skip = last as i16;
                    last_used = last;
                }
                skipped.push(skip);
                used += 1;
            }
        }
        if data.len() as i64 != trans.pos {
            return Err(ModelError::Invalid);
        }
        self.num_transformations = used as i32;
        self.base_indices = Some(indices);
        self.transformation_x = Some(xs);
        self.transformation_y = Some(ys);
        self.transformation_z = Some(zs);
        self.labels = Some(skipped);
        self.tweening_properties = Some(flags);
        Ok(())
    }
}

#[derive(Debug, Clone, Default)]
pub struct FrameSet {
    pub group_id: i32,
    pub frames: Vec<Option<AnimFrame>>,
}

impl FrameSet {
    pub fn frame(&self, index: i32) -> R<&AnimFrame> {
        if index < 0 {
            return Err(ModelError::Bounds);
        }
        match self.frames.get(index as usize) {
            Some(Some(frame)) => Ok(frame),
            Some(None) => Err(ModelError::Null),
            None => Err(ModelError::Bounds),
        }
    }

    pub fn modifies_alpha(&self, index: i32) -> R<bool> {
        Ok(self.frame(index)?.modifies_alpha)
    }

    pub fn modifies_colors(&self, index: i32) -> R<bool> {
        Ok(self.frame(index)?.modifies_color)
    }

    pub fn modifies_billboards(&self, index: i32) -> R<bool> {
        Ok(self.frame(index)?.modifies_billboard)
    }

    pub fn decode(group_id: i32, files: &[(i32, Vec<u8>)], bases: &mut BaseStore) -> R<Self> {
        let capacity = files
            .iter()
            .map(|(id, _)| *id + 1)
            .max()
            .unwrap_or(0)
            .max(0) as usize;
        let mut frames: Vec<Option<AnimFrame>> = vec![None; capacity];
        for (file_id, data) in files {
            let mut buffer = Buf::new(data);
            buffer.pos = 1;
            let base_id = buffer.u16()?;
            let base = bases.get(base_id)?.clone();
            let slot = frames
                .get_mut(*file_id as usize)
                .ok_or(ModelError::Bounds)?;
            *slot = Some(AnimFrame::decode(data, &base));
        }
        Ok(Self { group_id, frames })
    }
}

pub struct BaseStore {
    pub source: Box<dyn FnMut(i32) -> Option<Vec<u8>>>,
    pub loaded: HashMap<i32, AnimBase>,
}

impl BaseStore {
    pub fn new(source: Box<dyn FnMut(i32) -> Option<Vec<u8>>>) -> Self {
        Self {
            source,
            loaded: HashMap::new(),
        }
    }

    pub fn get(&mut self, id: i32) -> R<&AnimBase> {
        if !self.loaded.contains_key(&id) {
            let data = (self.source)(id).ok_or(ModelError::Null)?;
            let base = AnimBase::decode(id, &data)?;
            self.loaded.insert(id, base);
        }
        Ok(&self.loaded[&id])
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Param {
    Int(i32),
    Text(Vec<u8>),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SeqType {
    pub id: i32,
    pub durations: Option<Vec<i32>>,
    pub replay_interval: i32,
    pub interleave_order: Option<Vec<bool>>,
    pub frames: Option<Vec<i32>>,
    pub priority: i32,
    pub replacement_shield: i32,
    pub replacement_weapon: i32,
    pub replay_count: i32,
    pub interrupt_type: i32,
    pub move_type: i32,
    pub loop_type: i32,
    pub interface_frames: Option<Vec<i32>>,
    pub frame_sounds: Option<Vec<Option<Vec<i32>>>>,
    pub lights: bool,
    pub tweened: bool,
    pub vorbis: bool,
    pub frame_sound_volume: Option<Vec<i32>>,
    pub sound_max_delay: Option<Vec<i32>>,
    pub sound_min_delay: Option<Vec<i32>>,
    pub parameters: Vec<(i32, Param)>,
}

impl SeqType {
    pub fn new(id: i32) -> Self {
        Self {
            id,
            durations: None,
            replay_interval: -1,
            interleave_order: None,
            frames: None,
            priority: 5,
            replacement_shield: -1,
            replacement_weapon: -1,
            replay_count: 99,
            interrupt_type: -1,
            move_type: -1,
            loop_type: 2,
            interface_frames: None,
            frame_sounds: None,
            lights: false,
            tweened: false,
            vorbis: false,
            frame_sound_volume: None,
            sound_max_delay: None,
            sound_min_delay: None,
            parameters: Vec::new(),
        }
    }

    pub fn decode_entry(id: i32, data: Option<&[u8]>) -> R<Self> {
        let mut seq = Self::new(id);
        if let Some(data) = data {
            seq.decode(&mut Buf::new(data))?;
        }
        seq.post_decode();
        Ok(seq)
    }

    pub fn decode(&mut self, stream: &mut Buf) -> R<()> {
        loop {
            let opcode = stream.u8()?;
            if opcode == 0 {
                return Ok(());
            }
            self.decode_opcode(stream, opcode)?;
        }
    }

    fn decode_opcode(&mut self, stream: &mut Buf, opcode: i32) -> R<()> {
        match opcode {
            1 => {
                let count = stream.u16()? as usize;
                let mut durations = vec![0; count];
                for slot in durations.iter_mut() {
                    *slot = stream.u16()?;
                }
                let mut frames = vec![0; count];
                for slot in frames.iter_mut() {
                    *slot = stream.u16()?;
                }
                for slot in frames.iter_mut() {
                    *slot = slot.wrapping_add(stream.u16()? << 16);
                }
                self.durations = Some(durations);
                self.frames = Some(frames);
            }
            2 => self.replay_interval = stream.u16()?,
            3 => {
                let mut order = vec![false; 256];
                let count = stream.u8()?;
                for _ in 0..count {
                    order[stream.u8()? as usize] = true;
                }
                self.interleave_order = Some(order);
            }
            5 => self.priority = stream.u8()?,
            6 => self.replacement_shield = stream.u16()?,
            7 => self.replacement_weapon = stream.u16()?,
            8 => self.replay_count = stream.u8()?,
            9 => self.interrupt_type = stream.u8()?,
            10 => self.move_type = stream.u8()?,
            11 => self.loop_type = stream.u8()?,
            12 => {
                let count = stream.u8()? as usize;
                let mut frames = vec![0; count];
                for slot in frames.iter_mut() {
                    *slot = stream.u16()?;
                }
                for slot in frames.iter_mut() {
                    *slot = slot.wrapping_add(stream.u16()? << 16);
                }
                self.interface_frames = Some(frames);
            }
            13 => {
                let frame_count = stream.u16()? as usize;
                let mut sounds: Vec<Option<Vec<i32>>> = vec![None; frame_count];
                self.frame_sounds = Some(Vec::new());
                for slot in sounds.iter_mut() {
                    let count = stream.u8()? as usize;
                    if count > 0 {
                        let mut list = vec![0; count];
                        list[0] = stream.u24()?;
                        for entry in list.iter_mut().skip(1) {
                            *entry = stream.u16()?;
                        }
                        *slot = Some(list);
                    }
                }
                self.frame_sounds = Some(sounds);
            }
            14 => self.lights = true,
            15 => self.tweened = true,
            18 => self.vorbis = true,
            19 => {
                let sounds = self.frame_sounds.as_ref().ok_or(ModelError::Null)?.len();
                if self.frame_sound_volume.is_none() {
                    self.frame_sound_volume = Some(vec![255; sounds]);
                }
                let index = stream.u8()?;
                let value = stream.u8()?;
                let volume = self.frame_sound_volume.as_mut().ok_or(ModelError::Null)?;
                *volume.get_mut(index as usize).ok_or(ModelError::Bounds)? = value;
            }
            20 => {
                if self.sound_max_delay.is_none() || self.sound_min_delay.is_none() {
                    let sounds = self.frame_sounds.as_ref().ok_or(ModelError::Null)?.len();
                    self.sound_max_delay = Some(vec![256; sounds]);
                    self.sound_min_delay = Some(vec![256; sounds]);
                }
                let index = stream.u8()?;
                let max = stream.u16()?;
                let min = stream.u16()?;
                let max_list = self.sound_max_delay.as_mut().ok_or(ModelError::Null)?;
                *max_list.get_mut(index as usize).ok_or(ModelError::Bounds)? = max;
                let min_list = self.sound_min_delay.as_mut().ok_or(ModelError::Null)?;
                *min_list.get_mut(index as usize).ok_or(ModelError::Bounds)? = min;
            }
            249 => {
                let count = stream.u8()?;
                for _ in 0..count {
                    let is_string = stream.u8()? == 1;
                    let key = stream.u24()?;
                    let value = if is_string {
                        Param::Text(stream.string()?)
                    } else {
                        Param::Int(stream.i32()?)
                    };
                    self.parameters.push((key, value));
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub fn post_decode(&mut self) {
        if self.interrupt_type == -1 {
            self.interrupt_type = if self.interleave_order.is_some() {
                2
            } else {
                0
            };
        }
        if self.move_type == -1 {
            self.move_type = if self.interleave_order.is_some() {
                2
            } else {
                0
            };
        }
    }

    pub fn cs2_integer(&self, key: i32, default: i32) -> R<i32> {
        for (k, value) in &self.parameters {
            if *k == key {
                return match value {
                    Param::Int(v) => Ok(*v),
                    Param::Text(_) => Err(ModelError::Invalid),
                };
            }
        }
        Ok(default)
    }
}

pub trait FrameSets {
    fn frameset(&self, group: i32) -> Option<&FrameSet>;
}

#[derive(Debug, Clone, Default)]
pub struct SequenceMask {
    pub current_frame: i32,
    pub current_frameset: Option<i32>,
    pub function_mask: i32,
    pub next_frame: i32,
    pub next_frameset: Option<i32>,
    pub function_mask_calculated: bool,
}

impl SequenceMask {
    pub fn is_calculated(
        &mut self,
        sets: &dyn FrameSets,
        seq: &SeqType,
        current_frame_id: i32,
        next_frame_id: i32,
        frames: &[i32],
    ) -> R<bool> {
        if self.function_mask_calculated {
            return Ok(true);
        }
        if current_frame_id >= frames.len() as i32 {
            return Ok(false);
        }
        self.current_frame = at_slice(frames, i64::from(current_frame_id))?;
        let current_group = self.current_frame >> 16;
        self.current_frameset = sets.frameset(current_group).map(|_| current_group);
        self.current_frame &= 0xffff;
        let Some(current_group) = self.current_frameset else {
            return Ok(false);
        };
        if seq.tweened && next_frame_id != -1 && next_frame_id < frames.len() as i32 {
            self.next_frame = at_slice(frames, i64::from(next_frame_id))?;
            let next_group = self.next_frame >> 16;
            self.next_frameset = sets.frameset(next_group).map(|_| next_group);
            self.next_frame &= 0xffff;
        }
        if seq.lights {
            self.function_mask |= ALTERS_LIGHT;
        }
        let current = sets.frameset(current_group).ok_or(ModelError::Null)?;
        if current.modifies_colors(self.current_frame)? {
            self.function_mask |= ALTERS_COLOR;
        }
        if current.modifies_alpha(self.current_frame)? {
            self.function_mask |= ALTERS_ALPHA;
        }
        if current.modifies_billboards(self.current_frame)? {
            self.function_mask |= ALTERS_BILLBOARDS;
        }
        if let Some(next_group) = self.next_frameset {
            let next = sets.frameset(next_group).ok_or(ModelError::Null)?;
            if next.modifies_colors(self.next_frame)? {
                self.function_mask |= ALTERS_COLOR;
            }
            if next.modifies_alpha(self.next_frame)? {
                self.function_mask |= ALTERS_ALPHA;
            }
            if next.modifies_billboards(self.next_frame)? {
                self.function_mask |= ALTERS_BILLBOARDS;
            }
        }
        self.function_mask |= ALTERS_VERTICES;
        self.function_mask_calculated = true;
        Ok(true)
    }

    pub fn reset(&mut self) {
        self.function_mask_calculated = false;
        self.function_mask = 0;
        self.next_frameset = None;
        self.current_frameset = None;
    }
}

#[derive(Debug, Clone)]
pub struct Sequence {
    pub seq_id: i32,
    pub reset_animation: bool,
    pub tweening: bool,
    pub speed: i32,
    pub current_loop_count: i32,
    pub current_frame_id: i32,
    pub next_frame_id: i32,
    pub current_frame_duration: i32,
    pub replay_mode: i32,
    pub hard_reset: bool,
    pub current_mask: SequenceMask,
    pub next_mask: Option<SequenceMask>,
}

pub fn force_tween() -> bool {
    false
}

fn frames_of(seq: &SeqType) -> R<&Vec<i32>> {
    seq.frames.as_ref().ok_or(ModelError::Null)
}

fn durations_of(seq: &SeqType) -> R<&Vec<i32>> {
    seq.durations.as_ref().ok_or(ModelError::Null)
}

impl Sequence {
    pub fn new(tween: bool) -> Self {
        Self {
            seq_id: -1,
            reset_animation: false,
            tweening: tween,
            speed: 0,
            current_loop_count: 0,
            current_frame_id: 0,
            next_frame_id: 0,
            current_frame_duration: 0,
            replay_mode: 0,
            hard_reset: false,
            current_mask: SequenceMask::default(),
            next_mask: if tween {
                Some(SequenceMask::default())
            } else {
                None
            },
        }
    }

    pub fn reset_masks(&mut self) {
        self.current_mask.reset();
        if self.tweening {
            if let Some(mask) = self.next_mask.as_mut() {
                mask.reset();
            }
        }
    }

    pub fn animate(
        &mut self,
        sequence_id: i32,
        speed: i32,
        replay_mode: i32,
        hard_reset: bool,
        lookup: &dyn Fn(i32) -> Option<SeqType>,
    ) -> R<Option<SeqType>> {
        let mut current: Option<SeqType> = None;
        if sequence_id != self.seq_id {
            if sequence_id == -1 {
                self.seq_id = -1;
            } else {
                current = lookup(sequence_id);
                self.seq_id = sequence_id;
                let ty = current.as_ref().ok_or(ModelError::Null)?;
                self.current_loop_count = 0;
                self.speed = speed;
                self.replay_mode = replay_mode;
                self.hard_reset = hard_reset;
                self.current_frame_id = 0;
                self.current_frame_duration = 0;
                self.next_frame_id = self.current_frame_id + 1;
                if self.next_frame_id >= frames_of(ty)?.len() as i32 {
                    self.next_frame_id = -1;
                }
                self.reset_animation = false;
            }
            self.reset_masks();
        }
        Ok(current)
    }

    pub fn progress_cycle(&mut self, ty: &SeqType, seq_duration: i32) -> R<bool> {
        if seq_duration == 0 {
            return Ok(false);
        }
        let mut duration = seq_duration;
        if self.speed > 0 {
            if self.speed >= duration {
                self.speed -= duration;
                return Ok(false);
            }
            duration -= self.speed;
            self.speed = 0;
        }
        let frames = frames_of(ty)?;
        let durations = durations_of(ty)?;
        duration = duration.wrapping_add(self.current_frame_duration);
        let mut tween = ty.tweened || force_tween();
        if duration > 100 && ty.replay_interval > 0 {
            let from_frame = frames.len() as i32 - ty.replay_interval;
            while self.current_frame_id < from_frame
                && duration > at_slice(durations, i64::from(self.current_frame_id))?
            {
                duration -= at_slice(durations, i64::from(self.current_frame_id))?;
                self.current_frame_id += 1;
            }
            if self.current_frame_id >= from_frame {
                let mut max_duration: i32 = 0;
                for index in from_frame..frames.len() as i32 {
                    max_duration =
                        max_duration.wrapping_add(at_slice(durations, i64::from(index))?);
                }
                if max_duration == 0 {
                    return Err(ModelError::Invalid);
                }
                if self.replay_mode == 0 {
                    self.current_loop_count = self
                        .current_loop_count
                        .wrapping_add(duration.wrapping_div(max_duration));
                }
                duration = duration.wrapping_rem(max_duration);
            }
            self.next_frame_id = self.current_frame_id + 1;
            if self.next_frame_id >= frames.len() as i32 {
                self.next_frame_id -= ty.replay_interval;
                if self.next_frame_id < 0 || self.next_frame_id >= frames.len() as i32 {
                    self.next_frame_id = -1;
                }
            }
            tween = true;
        }
        while duration > at_slice(durations, i64::from(self.current_frame_id))? {
            tween = true;
            self.current_frame_id += 1;
            duration -= at_slice(durations, i64::from(self.current_frame_id) - 1)?;
            if self.current_frame_id >= frames.len() as i32 {
                if ty.replay_interval != -1 && self.replay_mode != 2 {
                    self.current_frame_id -= ty.replay_interval;
                    if self.replay_mode == 0 {
                        self.current_loop_count += 1;
                    }
                }
                if self.current_loop_count >= ty.replay_count
                    || self.current_frame_id < 0
                    || self.current_frame_id >= frames.len() as i32
                {
                    self.reset_animation = true;
                    break;
                }
            }
            self.next_frame_id = self.current_frame_id + 1;
            if self.next_frame_id >= frames.len() as i32 {
                if ty.replay_interval == -1 && self.hard_reset {
                    self.next_frame_id = 0;
                } else {
                    self.next_frame_id -= ty.replay_interval;
                }
                if self.next_frame_id < 0 || self.next_frame_id >= frames.len() as i32 {
                    self.next_frame_id = -1;
                }
            }
        }
        self.current_frame_duration = duration;
        if tween {
            self.reset_masks();
        }
        Ok(tween)
    }

    pub fn is_mask_calculated(&mut self, sets: &dyn FrameSets, ty: &SeqType) -> R<bool> {
        let frames = frames_of(ty)?;
        let calculated = self.current_mask.is_calculated(
            sets,
            ty,
            self.current_frame_id,
            self.next_frame_id,
            frames,
        )?;
        if calculated && self.tweening {
            if let Some(interface) = ty.interface_frames.as_ref() {
                let next = self.next_mask.as_mut().ok_or(ModelError::Null)?;
                next.is_calculated(
                    sets,
                    ty,
                    self.current_frame_id,
                    self.next_frame_id,
                    interface,
                )?;
            }
        }
        Ok(calculated)
    }

    pub fn function_mask(&mut self, sets: &dyn FrameSets, ty: &SeqType) -> R<i32> {
        if self.is_mask_calculated(sets, ty)? {
            let mut flags = self.current_mask.function_mask;
            if self.tweening && ty.interface_frames.is_some() {
                flags |= self
                    .next_mask
                    .as_ref()
                    .ok_or(ModelError::Null)?
                    .function_mask;
            }
            return Ok(flags);
        }
        Ok(0)
    }
}
