const MULTIPLIER: i64 = 0x5DEECE66D;
const ADDEND: i64 = 0xB;
const MASK: i64 = (1 << 48) - 1;

pub struct JavaRandom {
    seed: i64,
}

impl JavaRandom {
    pub fn new(seed: i64) -> Self {
        JavaRandom { seed: (seed ^ MULTIPLIER) & MASK }
    }

    fn next(&mut self, bits: u32) -> i32 {
        self.seed = (self.seed.wrapping_mul(MULTIPLIER).wrapping_add(ADDEND)) & MASK;
        (self.seed >> (48 - bits)) as i32
    }

    pub fn next_int(&mut self) -> i32 {
        self.next(32)
    }
}

pub fn is_power_of_two(value: i32) -> bool {
    value == (value & value.wrapping_neg())
}

pub fn floored_mod(value: i32, limit: i32) -> i32 {
    let offset = (value >> 31) & (limit - 1);
    (value.wrapping_add(((value as u32) >> 31) as i32)) % limit + offset
}

pub fn bounded_random(random: &mut JavaRandom, limit: i32) -> i32 {
    assert!(limit > 0);
    if is_power_of_two(limit) {
        return (((random.next_int() as i64) & 0xffff_ffff) * limit as i64 >> 32) as i32;
    }
    let rejection_threshold = i32::MIN.wrapping_sub((4294967296i64 % limit as i64) as i32);
    loop {
        let value = random.next_int();
        if value < rejection_threshold {
            return floored_mod(value, limit);
        }
    }
}

pub fn randomly_seeded_permutation(seed: i32) -> [u8; 512] {
    let mut block = [0u8; 512];
    let mut random = JavaRandom::new(seed as i64);
    for (index, slot) in block.iter_mut().enumerate().take(255) {
        *slot = index as u8;
    }
    for index in 0..255 {
        let remaining = 255 - index;
        let swap = bounded_random(&mut random, remaining as i32) as usize;
        let temp = block[swap];
        block[swap] = block[remaining];
        block[remaining] = temp;
        block[511 - index] = temp;
    }
    block
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn next_int_matches_java_sequence_for_seed_zero() {
        let mut random = JavaRandom::new(0);
        assert_eq!(random.next_int(), -1155484576);
        assert_eq!(random.next_int(), -723955400);
        assert_eq!(random.next_int(), 1033096058);
    }

    #[test]
    fn next_int_matches_java_sequence_for_seed_42() {
        let mut random = JavaRandom::new(42);
        assert_eq!(random.next_int(), -1170105035);
        assert_eq!(random.next_int(), 234785527);
    }
}
