use crate::reader::{at, put, AudioError, Buf, Writer, R};

const MESSAGE_DATA_BYTE_COUNT: [i8; 128] = {
    let mut table = [0i8; 128];
    let mut index = 0;
    while index < 128 {
        table[index] = if index < 64 {
            2
        } else if index < 96 {
            1
        } else if index < 112 {
            2
        } else {
            match index - 112 {
                1 => 1,
                2 => 2,
                3 => 1,
                _ => 0,
            }
        };
        index += 1;
    }
    table
};

fn next(buffer: &[u8], offset: &mut i64) -> R<i32> {
    let value = at(buffer, *offset)?;
    *offset += 1;
    Ok(i32::from(value))
}

fn next_signed(buffer: &[u8], offset: &mut i64) -> R<i32> {
    Ok(i32::from(next(buffer, offset)? as u8 as i8))
}

#[derive(Debug, Clone)]
pub struct MusicTrack {
    pub midi: Vec<u8>,
    pub table: Option<PatchTable>,
}

#[derive(Debug, Clone, Default)]
pub struct PatchTable {
    buckets: [Vec<(i64, Box<[u8; 128]>)>; 16],
}

impl PatchTable {
    pub fn new() -> Self {
        Self::default()
    }

    fn bucket(key: i64) -> usize {
        (key & 15) as usize
    }

    pub fn get(&self, key: i64) -> Option<&[u8; 128]> {
        self.buckets[Self::bucket(key)]
            .iter()
            .find(|(k, _)| *k == key)
            .map(|(_, v)| &**v)
    }

    fn get_or_insert(&mut self, key: i64) -> &mut [u8; 128] {
        let bucket = &mut self.buckets[Self::bucket(key)];
        let index = match bucket.iter().position(|(k, _)| *k == key) {
            Some(index) => index,
            None => {
                bucket.push((key, Box::new([0u8; 128])));
                bucket.len() - 1
            }
        };
        &mut bucket[index].1
    }

    pub fn entries(&self) -> impl Iterator<Item = (i64, &[u8; 128])> {
        self.buckets
            .iter()
            .flat_map(|bucket| bucket.iter().map(|(k, v)| (*k, &**v)))
    }

    pub fn len(&self) -> usize {
        self.buckets.iter().map(Vec::len).sum()
    }

    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }
}

impl MusicTrack {
    pub fn new(input: &[u8]) -> R<Self> {
        let mut head = Buf::new(input);
        head.pos = input.len() as i64 - 3;
        let track_count = head.u8()?;
        let division = head.u16()?;
        let mut size = track_count.wrapping_mul(10).wrapping_add(14);
        let mut reader = Buf::new(input);
        let mut tempo = 0i32;
        let mut controller = 0i32;
        let mut note_on = 0i32;
        let mut note_off = 0i32;
        let mut pitch_bend = 0i32;
        let mut channel_pressure = 0i32;
        let mut key_pressure = 0i32;
        let mut program_change = 0i32;
        for _ in 0..track_count {
            let mut last = -1;
            loop {
                let event = reader.u8()?;
                if event != last {
                    size = size.wrapping_add(1);
                }
                last = event & 0xf;
                if event == 7 {
                    break;
                }
                if event == 23 {
                    tempo += 1;
                } else {
                    match last {
                        0 => note_on += 1,
                        1 => note_off += 1,
                        2 => controller += 1,
                        3 => pitch_bend += 1,
                        4 => channel_pressure += 1,
                        5 => key_pressure += 1,
                        6 => program_change += 1,
                        _ => return Err(AudioError::Invalid),
                    }
                }
            }
        }
        size = size.wrapping_add(tempo.wrapping_mul(5));
        size = size.wrapping_add(
            (note_on + note_off + controller + pitch_bend + key_pressure).wrapping_mul(2),
        );
        size = size.wrapping_add(channel_pressure + program_change);
        let delta_start = reader.pos;
        let var_ints = track_count
            + tempo
            + controller
            + note_on
            + note_off
            + pitch_bend
            + channel_pressure
            + key_pressure
            + program_change;
        for _ in 0..var_ints {
            reader.var_int()?;
        }
        size = size.wrapping_add((reader.pos - delta_start) as i32);
        let mut controller_offset = reader.pos;
        let mut mod_wheel_msb = 0i64;
        let mut mod_wheel_lsb = 0i64;
        let mut volume_msb = 0i64;
        let mut volume_lsb = 0i64;
        let mut pan_msb = 0i64;
        let mut pan_lsb = 0i64;
        let mut nrpn_msb = 0i64;
        let mut nrpn_lsb = 0i64;
        let mut rpn_msb = 0i64;
        let mut rpn_lsb = 0i64;
        let mut switch_controller = 0i64;
        let mut other_controller = 0i64;
        let mut program_count = i64::from(program_change);
        let mut number = 0i32;
        for _ in 0..controller {
            number = (number + reader.u8()?) & 0x7f;
            if number != 0 && number != 32 {
                match number {
                    1 => mod_wheel_msb += 1,
                    33 => mod_wheel_lsb += 1,
                    7 => volume_msb += 1,
                    39 => volume_lsb += 1,
                    10 => pan_msb += 1,
                    42 => pan_lsb += 1,
                    99 => nrpn_msb += 1,
                    98 => nrpn_lsb += 1,
                    101 => rpn_msb += 1,
                    100 => rpn_lsb += 1,
                    64 | 65 | 120 | 121 | 123 => switch_controller += 1,
                    _ => other_controller += 1,
                }
            } else {
                program_count += 1;
            }
        }
        let mut cursor = reader.pos;
        let mut take = |count: i64| {
            let start = cursor;
            cursor += count;
            start
        };
        let mut switch_offset = take(switch_controller);
        let mut key_pressure_offset = take(i64::from(key_pressure));
        let mut channel_pressure_offset = take(i64::from(channel_pressure));
        let mut pitch_high_offset = take(i64::from(pitch_bend));
        let mut mod_wheel_msb_offset = take(mod_wheel_msb);
        let mut volume_msb_offset = take(volume_msb);
        let mut pan_msb_offset = take(pan_msb);
        let mut note_key_offset = take(i64::from(note_on + note_off + key_pressure));
        let mut note_on_velocity_offset = take(i64::from(note_on));
        let mut other_offset = take(other_controller);
        let mut note_off_velocity_offset = take(i64::from(note_off));
        let mut mod_wheel_lsb_offset = take(mod_wheel_lsb);
        let mut volume_lsb_offset = take(volume_lsb);
        let mut pan_lsb_offset = take(pan_lsb);
        let mut program_offset = take(program_count);
        let mut pitch_low_offset = take(i64::from(pitch_bend));
        let mut nrpn_msb_offset = take(nrpn_msb);
        let mut nrpn_lsb_offset = take(nrpn_lsb);
        let mut rpn_msb_offset = take(rpn_msb);
        let mut rpn_lsb_offset = take(rpn_lsb);
        let mut tempo_offset = take(i64::from(tempo) * 3);

        let mut out = Writer::new(size)?;
        out.int(1_297_377_380)?;
        out.int(6)?;
        out.short(if track_count > 1 { 1 } else { 0 })?;
        out.short(track_count)?;
        out.short(division)?;
        reader.pos = delta_start;
        let mut channel_xor = 0i32;
        let mut note_key = 0i32;
        let mut note_on_velocity = 0i32;
        let mut note_off_velocity = 0i32;
        let mut pitch_bend_accum = 0i32;
        let mut channel_pressure_accum = 0i32;
        let mut key_pressure_accum = 0i32;
        let mut controller_values = [0i32; 128];
        number = 0;
        let mut opcode_index = 0i64;
        for _ in 0..track_count {
            out.int(1_297_379_947)?;
            out.pos += 4;
            let track_start = out.pos;
            let mut previous = -1i32;
            loop {
                let delta = reader.var_int()?;
                out.flags(delta)?;
                let encoded = next(input, &mut opcode_index)?;
                let is_new = encoded != previous;
                previous = encoded & 0xf;
                if encoded == 7 {
                    if is_new {
                        out.byte(255)?;
                    }
                    out.byte(47)?;
                    out.byte(0)?;
                    out.int_length((out.pos - track_start) as i32)?;
                    break;
                }
                if encoded == 23 {
                    if is_new {
                        out.byte(255)?;
                    }
                    out.byte(81)?;
                    out.byte(3)?;
                    let a = next(input, &mut tempo_offset)?;
                    out.byte(a)?;
                    let b = next(input, &mut tempo_offset)?;
                    out.byte(b)?;
                    let c = next(input, &mut tempo_offset)?;
                    out.byte(c)?;
                    continue;
                }
                channel_xor ^= encoded >> 4;
                match previous {
                    0 => {
                        if is_new {
                            out.byte(channel_xor + 144)?;
                        }
                        note_key = note_key.wrapping_add(next(input, &mut note_key_offset)?);
                        note_on_velocity =
                            note_on_velocity.wrapping_add(next(input, &mut note_on_velocity_offset)?);
                        out.byte(note_key & 0x7f)?;
                        out.byte(note_on_velocity & 0x7f)?;
                    }
                    1 => {
                        if is_new {
                            out.byte(channel_xor + 128)?;
                        }
                        note_key = note_key.wrapping_add(next(input, &mut note_key_offset)?);
                        note_off_velocity = note_off_velocity
                            .wrapping_add(next(input, &mut note_off_velocity_offset)?);
                        out.byte(note_key & 0x7f)?;
                        out.byte(note_off_velocity & 0x7f)?;
                    }
                    2 => {
                        if is_new {
                            out.byte(channel_xor + 176)?;
                        }
                        number = (number + next(input, &mut controller_offset)?) & 0x7f;
                        out.byte(number)?;
                        let value = if number != 0 && number != 32 {
                            match number {
                                1 => next_signed(input, &mut mod_wheel_msb_offset)?,
                                33 => next_signed(input, &mut mod_wheel_lsb_offset)?,
                                7 => next_signed(input, &mut volume_msb_offset)?,
                                39 => next_signed(input, &mut volume_lsb_offset)?,
                                10 => next_signed(input, &mut pan_msb_offset)?,
                                42 => next_signed(input, &mut pan_lsb_offset)?,
                                99 => next_signed(input, &mut nrpn_msb_offset)?,
                                98 => next_signed(input, &mut nrpn_lsb_offset)?,
                                101 => next_signed(input, &mut rpn_msb_offset)?,
                                100 => next_signed(input, &mut rpn_lsb_offset)?,
                                64 | 65 | 120 | 121 | 123 => {
                                    next_signed(input, &mut switch_offset)?
                                }
                                _ => next_signed(input, &mut other_offset)?,
                            }
                        } else {
                            next_signed(input, &mut program_offset)?
                        };
                        let slot = &mut controller_values[number as usize];
                        let accumulated = value.wrapping_add(*slot);
                        *slot = accumulated;
                        out.byte(accumulated & 0x7f)?;
                    }
                    3 => {
                        if is_new {
                            out.byte(channel_xor + 224)?;
                        }
                        pitch_bend_accum =
                            pitch_bend_accum.wrapping_add(next_signed(input, &mut pitch_low_offset)?);
                        pitch_bend_accum = pitch_bend_accum
                            .wrapping_add(next(input, &mut pitch_high_offset)?.wrapping_shl(7));
                        out.byte(pitch_bend_accum & 0x7f)?;
                        out.byte((pitch_bend_accum >> 7) & 0x7f)?;
                    }
                    4 => {
                        if is_new {
                            out.byte(channel_xor + 208)?;
                        }
                        channel_pressure_accum = channel_pressure_accum
                            .wrapping_add(next(input, &mut channel_pressure_offset)?);
                        out.byte(channel_pressure_accum & 0x7f)?;
                    }
                    5 => {
                        if is_new {
                            out.byte(channel_xor + 160)?;
                        }
                        note_key = note_key.wrapping_add(next(input, &mut note_key_offset)?);
                        key_pressure_accum = key_pressure_accum
                            .wrapping_add(next(input, &mut key_pressure_offset)?);
                        out.byte(note_key & 0x7f)?;
                        out.byte(key_pressure_accum & 0x7f)?;
                    }
                    6 => {
                        if is_new {
                            out.byte(channel_xor + 192)?;
                        }
                        let program = next(input, &mut program_offset)?;
                        out.byte(program)?;
                    }
                    _ => return Err(AudioError::Invalid),
                }
            }
        }
        Ok(Self {
            midi: out.into_data(),
            table: None,
        })
    }

    pub fn build_patch_table(&mut self) -> R<()> {
        if self.table.is_some() {
            return Ok(());
        }
        let mut table = PatchTable::new();
        let mut bank_selects = [0i32; 16];
        let mut patch_programs = [0i32; 16];
        patch_programs[9] = 128;
        bank_selects[9] = 128;
        let mut reader = MidiFileReader::new(&self.midi)?;
        let track_count = reader.track_count();
        for track in 0..track_count {
            reader.goto_track(track)?;
            reader.track_length(track)?;
            reader.mark_position(track)?;
        }
        loop {
            'inner: loop {
                let track = reader.prioritized_track();
                let length = at(&reader.track_lengths, i64::from(track))?;
                while length == at(&reader.track_lengths, i64::from(track))? {
                    reader.goto_track(track)?;
                    let message = reader.read_message(track)?;
                    if message == 1 {
                        reader.set_track_done();
                        reader.mark_position(track)?;
                        break 'inner;
                    }
                    let status = message & 0xf0;
                    let channel = (message & 0xf) as usize;
                    let data1 = (message >> 8) & 0x7f;
                    let data2 = (message >> 16) & 0x7f;
                    if status == 176 {
                        if data1 == 0 {
                            bank_selects[channel] = (data2.wrapping_shl(14))
                                .wrapping_add(bank_selects[channel] & -2_080_769);
                        }
                        if data1 == 32 {
                            bank_selects[channel] = (bank_selects[channel] & -16257)
                                .wrapping_add(data2.wrapping_shl(7));
                        }
                    }
                    if status == 192 {
                        patch_programs[channel] = data1.wrapping_add(bank_selects[channel]);
                    }
                    if status == 144 && data2 > 0 {
                        let patch = i64::from(patch_programs[channel]);
                        table.get_or_insert(patch)[data1 as usize] = 1;
                    }
                    reader.track_length(track)?;
                    reader.mark_position(track)?;
                }
            }
            if reader.is_done() {
                break;
            }
        }
        self.table = Some(table);
        Ok(())
    }

    pub fn clear(&mut self) {
        self.table = None;
    }
}

#[derive(Debug, Clone)]
pub struct MidiFileReader {
    pub division: i32,
    pub microseconds_per_beat: i32,
    pub track_starts: Vec<i32>,
    pub tempo_time_offset: i64,
    pub start_positions: Vec<i32>,
    pub track_lengths: Vec<i32>,
    pub running_status: Vec<i32>,
    data: Vec<u8>,
    pos: i64,
}

impl MidiFileReader {
    pub fn empty() -> Self {
        Self {
            division: 0,
            microseconds_per_beat: 0,
            track_starts: Vec::new(),
            tempo_time_offset: 0,
            start_positions: Vec::new(),
            track_lengths: Vec::new(),
            running_status: Vec::new(),
            data: Vec::new(),
            pos: 0,
        }
    }

    pub fn new(bytes: &[u8]) -> R<Self> {
        let mut buf = Buf::new(bytes);
        buf.pos = 10;
        let count = buf.u16()?;
        let division = buf.u16()?;
        let mut starts = vec![0i32; count as usize];
        let mut index = 0usize;
        while index < count as usize {
            let kind = buf.i32()?;
            let length = buf.i32()?;
            if kind == 1_297_379_947 {
                starts[index] = buf.pos as i32;
                index += 1;
            }
            buf.pos = i64::from((buf.pos as i32).wrapping_add(length));
        }
        Ok(Self {
            division,
            microseconds_per_beat: 500_000,
            track_starts: starts.clone(),
            tempo_time_offset: 0,
            start_positions: starts,
            track_lengths: vec![0; count as usize],
            running_status: vec![0; count as usize],
            data: bytes.to_vec(),
            pos: buf.pos,
        })
    }

    fn advance(&mut self, count: i32) {
        self.pos = i64::from((self.pos as i32).wrapping_add(count));
    }

    fn cursor(&mut self) -> Buf<'_> {
        let mut buf = Buf::new(&self.data);
        buf.pos = self.pos;
        buf
    }

    fn var_int(&mut self) -> R<i32> {
        let mut buf = self.cursor();
        let value = buf.var_int();
        let pos = buf.pos;
        self.pos = pos;
        value
    }

    fn u8(&mut self) -> R<i32> {
        let mut buf = self.cursor();
        let value = buf.u8();
        let pos = buf.pos;
        self.pos = pos;
        value
    }

    pub fn track_count(&self) -> i32 {
        self.start_positions.len() as i32
    }

    pub fn track_length(&mut self, track: i32) -> R<()> {
        let delta = self.var_int()?;
        let slot = self
            .track_lengths
            .get_mut(usize::try_from(track).map_err(|_| AudioError::Bounds)?)
            .ok_or(AudioError::Bounds)?;
        *slot = slot.wrapping_add(delta);
        Ok(())
    }

    pub fn prioritized_track(&self) -> i32 {
        let mut best = -1;
        let mut earliest = i32::MAX;
        for track in 0..self.start_positions.len() {
            if self.start_positions[track] >= 0 && self.track_lengths[track] < earliest {
                best = track as i32;
                earliest = self.track_lengths[track];
            }
        }
        best
    }

    pub fn goto_track(&mut self, track: i32) -> R<()> {
        self.pos = i64::from(at(&self.start_positions, i64::from(track))?);
        Ok(())
    }

    pub fn set_track_done(&mut self) {
        self.pos = -1;
    }

    pub fn mark_position(&mut self, track: i32) -> R<()> {
        let pos = self.pos as i32;
        put(&mut self.start_positions, i64::from(track), pos)
    }

    pub fn tick_timestamp(&self, tick_length: i32) -> i64 {
        self.tempo_time_offset
            .wrapping_add(i64::from(tick_length).wrapping_mul(i64::from(self.microseconds_per_beat)))
    }

    pub fn is_done(&self) -> bool {
        self.start_positions.iter().all(|p| *p < 0)
    }

    pub fn reset_from_timestamp(&mut self, timestamp: i64) -> R<()> {
        self.tempo_time_offset = timestamp;
        for track in 0..self.start_positions.len() {
            self.track_lengths[track] = 0;
            self.running_status[track] = 0;
            self.pos = i64::from(self.track_starts[track]);
            self.track_length(track as i32)?;
            self.start_positions[track] = self.pos as i32;
        }
        Ok(())
    }

    pub fn data_loaded(&self) -> bool {
        !self.data.is_empty()
    }

    pub fn clear(&mut self) {
        self.data = Vec::new();
        self.track_starts = Vec::new();
        self.start_positions = Vec::new();
        self.track_lengths = Vec::new();
        self.running_status = Vec::new();
    }

    fn decode_status(&mut self, track: i32, status: i32) -> R<i32> {
        if status == 255 {
            let meta_type = self.u8()?;
            let mut meta_length = self.var_int()?;
            if meta_type == 47 {
                self.advance(meta_length);
                return Ok(1);
            } else if meta_type == 81 {
                let mut buf = self.cursor();
                let tempo = buf.u24()?;
                self.pos = buf.pos;
                meta_length = meta_length.wrapping_sub(3);
                let length = at(&self.track_lengths, i64::from(track))?;
                self.tempo_time_offset = self.tempo_time_offset.wrapping_add(
                    i64::from(length)
                        .wrapping_mul(i64::from(self.microseconds_per_beat.wrapping_sub(tempo))),
                );
                self.microseconds_per_beat = tempo;
                self.advance(meta_length);
                return Ok(2);
            }
            self.advance(meta_length);
            return Ok(3);
        }
        let count = at(&MESSAGE_DATA_BYTE_COUNT, i64::from(status) - 128)?;
        let mut flags = status;
        if count >= 1 {
            flags = status | (self.u8()?.wrapping_shl(8));
        }
        if count >= 2 {
            flags |= self.u8()?.wrapping_shl(16);
        }
        Ok(flags)
    }

    pub fn read_message(&mut self, track: i32) -> R<i32> {
        let peek = i32::from(at(&self.data, self.pos)? as i8);
        let status = if peek < 0 {
            let status = peek & 0xff;
            let slot = self
                .running_status
                .get_mut(usize::try_from(track).map_err(|_| AudioError::Bounds)?)
                .ok_or(AudioError::Bounds)?;
            *slot = status;
            self.pos += 1;
            status
        } else {
            at(&self.running_status, i64::from(track))?
        };
        if status != 240 && status != 247 {
            return self.decode_status(track, status);
        }
        let length = self.var_int()?;
        if status == 247 && length > 0 {
            let flags = i32::from(at(&self.data, self.pos)?);
            if (241..=243).contains(&flags)
                || flags == 246
                || flags == 248
                || (250..=252).contains(&flags)
                || flags == 254
            {
                self.pos += 1;
                put(&mut self.running_status, i64::from(track), flags)?;
                return self.decode_status(track, flags);
            }
        }
        self.advance(length);
        Ok(0)
    }
}
