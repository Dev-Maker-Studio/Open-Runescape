use crate::HostError;

pub const MALE_IDKIT_SLOTS: [i32; 8] = [0, 1, 2, 3, 4, 5, 6, 14];
pub const FEMALE_IDKIT_SLOTS: [i32; 8] = [7, 8, 9, 10, 11, 12, 13, 15];
pub const IDK_PART_TABLE: [usize; 8] = [8, 11, 4, 6, 9, 7, 10, 0];
pub const ITEM_FLAG: i32 = 0x4000_0000;
pub const IDK_FLAG: i32 = i32::MIN;
const CRC64_POLYNOMIAL: i64 = -3_932_672_073_523_589_310;

const fn crc64_table() -> [i64; 256] {
    let mut table = [0i64; 256];
    let mut index = 0;
    while index < 256 {
        let mut hash = index as i64;
        let mut step = 0;
        while step < 8 {
            let shifted = ((hash as u64) >> 1) as i64;
            hash = if hash & 1 == 1 {
                shifted ^ CRC64_POLYNOMIAL
            } else {
                shifted
            };
            step += 1;
        }
        table[index] = hash;
        index += 1;
    }
    table
}

pub const CRC64_TABLE: [i64; 256] = crc64_table();

fn null_pointer() -> HostError {
    HostError::NoEntity
}

fn out_of_bounds(index: i64, length: usize) -> HostError {
    HostError::State(format!("index {index} out of bounds for length {length}"))
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct MeshModifier {
    pub male_body: [i32; 3],
    pub female_body: [i32; 3],
    pub male_head: [i32; 2],
    pub female_head: [i32; 2],
    pub recol_dst: Option<Vec<i16>>,
    pub retex_dst: Option<Vec<i16>>,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct PlayerAppearance {
    pub bas_id: i32,
    pub appearance: Option<Vec<i32>>,
    pub mesh_modifiers: Option<Vec<Option<MeshModifier>>>,
    pub recolor_dst_indices: Option<Vec<i32>>,
    pub female: bool,
    pub hash: i64,
}

impl PlayerAppearance {
    fn feed(&mut self, value: i32) {
        let index = ((self.hash ^ i64::from(value)) & 0xff) as usize;
        self.hash = (((self.hash as u64) >> 8) as i64) ^ CRC64_TABLE[index];
    }

    fn feed_wide(&mut self, value: i32) {
        self.feed(value.wrapping_shr(8));
        self.feed(value);
    }

    pub fn calculate_hash(&mut self) -> Result<(), HostError> {
        self.hash = -1;
        self.feed(self.bas_id.wrapping_shr(8));
        self.feed(self.bas_id);
        let parts = self.appearance.clone().ok_or_else(null_pointer)?;
        for part in parts {
            self.feed(part.wrapping_shr(24));
            self.feed(part.wrapping_shr(16));
            self.feed(part.wrapping_shr(8));
            self.feed(part);
        }
        if let Some(modifiers) = self.mesh_modifiers.clone() {
            for modifier in modifiers.iter().flatten() {
                let (body, head): (&[i32], &[i32]) = if self.female {
                    (&modifier.female_body, &modifier.female_head)
                } else {
                    (&modifier.male_body, &modifier.male_head)
                };
                for value in body {
                    self.feed_wide(*value);
                }
                for value in head {
                    self.feed_wide(*value);
                }
                for value in [&modifier.recol_dst, &modifier.retex_dst]
                    .into_iter()
                    .flatten()
                    .flatten()
                {
                    self.feed_wide(i32::from(*value));
                }
            }
        }
        for part in 0..10usize {
            let indices = self.recolor_dst_indices.as_ref().ok_or_else(null_pointer)?;
            let value = *indices
                .get(part)
                .ok_or_else(|| out_of_bounds(part as i64, indices.len()))?;
            self.feed(value);
        }
        self.feed(i32::from(self.female));
        Ok(())
    }

    pub fn add_identity(&mut self, slot: usize, id: i32) -> Result<(), HostError> {
        let part = IDK_PART_TABLE[slot];
        let parts = self.appearance.as_mut().ok_or_else(null_pointer)?;
        let length = parts.len();
        *parts
            .get_mut(part)
            .ok_or_else(|| out_of_bounds(part as i64, length))? = id | IDK_FLAG;
        self.calculate_hash()
    }

    pub fn add_item(&mut self, index: i32, item_id: i32) -> Result<(), HostError> {
        let parts = self.appearance.as_mut().ok_or_else(null_pointer)?;
        let length = parts.len();
        let cell = usize::try_from(index)
            .ok()
            .and_then(|index| parts.get_mut(index))
            .ok_or_else(|| out_of_bounds(i64::from(index), length))?;
        if item_id == -1 {
            *cell = 0;
            return Ok(());
        }
        *cell = item_id | ITEM_FLAG;
        self.calculate_hash()
    }

    pub fn change_gender(&mut self, female: bool) -> Result<(), HostError> {
        self.female = female;
        self.calculate_hash()
    }

    pub fn recolor(&mut self, slot: i32, color: i32) -> Result<(), HostError> {
        let indices = self
            .recolor_dst_indices
            .as_mut()
            .ok_or_else(null_pointer)?;
        let length = indices.len();
        *usize::try_from(slot)
            .ok()
            .and_then(|slot| indices.get_mut(slot))
            .ok_or_else(|| out_of_bounds(i64::from(slot), length))? = color;
        self.calculate_hash()
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct AppearanceState {
    pub my_player: bool,
    pub appearance: Option<PlayerAppearance>,
}

impl AppearanceState {
    pub fn mine(&mut self) -> Result<Option<&mut PlayerAppearance>, HostError> {
        if !self.my_player {
            return Err(null_pointer());
        }
        Ok(self.appearance.as_mut())
    }

    pub fn set_item(&mut self, slot: i32, item_id: i32) -> Result<(), HostError> {
        match self.mine()? {
            Some(appearance) => appearance.add_item(slot, item_id),
            None => Ok(()),
        }
    }

    pub fn set_base_identikit(&mut self, body_part: i32, id: i32) -> Result<(), HostError> {
        let Some(appearance) = self.mine()? else {
            return Ok(());
        };
        let slot = MALE_IDKIT_SLOTS
            .iter()
            .position(|part| *part == body_part)
            .or_else(|| {
                FEMALE_IDKIT_SLOTS
                    .iter()
                    .position(|part| *part == body_part)
            });
        match slot {
            Some(slot) => appearance.add_identity(slot, id),
            None => Ok(()),
        }
    }

    pub fn set_gender(&mut self, female: bool) -> Result<(), HostError> {
        match self.mine()? {
            Some(appearance) => appearance.change_gender(female),
            None => Ok(()),
        }
    }

    pub fn set_base_colour(&mut self, slot: i32, color: i32) -> Result<(), HostError> {
        match self.mine()? {
            Some(appearance) => appearance.recolor(slot, color),
            None => Ok(()),
        }
    }
}
