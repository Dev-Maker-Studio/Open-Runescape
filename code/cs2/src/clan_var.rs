use crate::HostError;

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum ClanVarValue {
    Int(i32),
    Long(i64),
    Text(String),
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct ClanVarType {
    pub type_char: u8,
    pub base_var: i32,
    pub start_bit: i32,
    pub end_bit: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ClanVarDefs {
    pub vars: Vec<Option<Vec<u8>>>,
    pub settings: Vec<Option<Vec<u8>>>,
}

struct Reader<'a> {
    data: &'a [u8],
    position: usize,
}

impl Reader<'_> {
    fn byte(&mut self) -> Result<u8, HostError> {
        let value = *self
            .data
            .get(self.position)
            .ok_or_else(|| HostError::State("array index out of bounds".to_string()))?;
        self.position += 1;
        Ok(value)
    }

    fn short(&mut self) -> Result<i32, HostError> {
        let high = i32::from(self.byte()?);
        Ok((high << 8) | i32::from(self.byte()?))
    }

    fn type_char(&mut self) -> Result<u8, HostError> {
        match self.byte()? {
            0 => Err(HostError::State("illegal argument".to_string())),
            value => Ok(value),
        }
    }
}

fn decode(data: &[u8], bits_opcode: u8) -> Result<ClanVarType, HostError> {
    let mut reader = Reader { data, position: 0 };
    let mut found = ClanVarType::default();
    loop {
        let opcode = reader.byte()?;
        if opcode == 0 {
            return Ok(found);
        }
        if opcode == 1 {
            found.type_char = reader.type_char()?;
        } else if opcode == bits_opcode {
            found.base_var = reader.short()?;
            found.start_bit = i32::from(reader.byte()?);
            found.end_bit = i32::from(reader.byte()?);
        }
    }
}

fn lookup(files: &[Option<Vec<u8>>], id: i32, bits_opcode: u8) -> Result<ClanVarType, HostError> {
    let file = usize::try_from(id)
        .ok()
        .and_then(|id| files.get(id))
        .and_then(Option::as_ref);
    match file {
        Some(data) => decode(data, bits_opcode),
        None => Ok(ClanVarType::default()),
    }
}

impl ClanVarDefs {
    pub fn var_type(&self, id: i32) -> Result<ClanVarType, HostError> {
        lookup(&self.vars, id, 3)
    }

    pub fn setting_type(&self, id: i32) -> Result<ClanVarType, HostError> {
        lookup(&self.settings, id, 2)
    }
}

pub(crate) fn bitfield(value: i32, start: i32, end: i32) -> i32 {
    let mask = if end == 31 {
        -1
    } else {
        1_i32.wrapping_shl(end.wrapping_add(1) as u32 & 31).wrapping_sub(1)
    };
    ((value & mask) as u32 >> (start as u32 & 31)) as i32
}
