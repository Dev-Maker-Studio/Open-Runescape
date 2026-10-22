const MULTIPLIER: u64 = 0x5DEE_CE66D;
const MASK: u64 = (1 << 48) - 1;

pub(crate) struct JavaRandom {
    seed: u64,
}

impl JavaRandom {
    pub(crate) fn new(seed: i64) -> Self {
        Self {
            seed: (seed as u64 ^ MULTIPLIER) & MASK,
        }
    }

    pub(crate) fn next_int(&mut self) -> i32 {
        self.seed = self.seed.wrapping_mul(MULTIPLIER).wrapping_add(0xB) & MASK;
        (self.seed >> 16) as u32 as i32
    }

    pub(crate) fn bounded(&mut self, limit: i32) -> Result<i32, crate::Error> {
        if limit <= 0 {
            return Err(crate::Error::InvalidTexture("random limit"));
        }
        if limit == (limit & limit.wrapping_neg()) {
            let value = i64::from(self.next_int() as u32) * i64::from(limit);
            return Ok((value >> 32) as i32);
        }
        let threshold = i32::MIN.wrapping_sub((4_294_967_296_i64 % i64::from(limit)) as i32);
        loop {
            let value = self.next_int();
            if value < threshold {
                let offset = (value >> 31) & (limit - 1);
                return Ok(value.wrapping_add(((value as u32) >> 31) as i32) % limit + offset);
            }
        }
    }

    pub(crate) fn shuffled_table(seed: i32) -> Result<Vec<u8>, crate::Error> {
        let mut block = vec![0u8; 512];
        let mut random = Self::new(i64::from(seed));
        for index in 0..=254usize {
            block[index] = index as u8;
        }
        for index in 0..=254i32 {
            let remaining = (255 - index) as usize;
            let swap = random.bounded(remaining as i32)? as usize;
            let temp = block[swap];
            block[swap] = block[remaining];
            block[remaining] = temp;
            block[(511 - index) as usize] = temp;
        }
        Ok(block)
    }
}
