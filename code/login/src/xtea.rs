use crate::{Error, MAX_PAYLOAD_BYTES};
use std::ops::Range;

const DELTA: u32 = 0x9e3779b9;
const ROUNDS: usize = 32;

pub fn encrypt(bytes: &mut [u8], seed: &[u32; 4], range: Range<usize>) -> Result<(), Error> {
    transform(bytes, seed, range, false)
}

pub fn decrypt(bytes: &mut [u8], seed: &[u32; 4], range: Range<usize>) -> Result<(), Error> {
    transform(bytes, seed, range, true)
}

fn transform(
    bytes: &mut [u8],
    seed: &[u32; 4],
    range: Range<usize>,
    decrypt: bool,
) -> Result<(), Error> {
    let selected = bytes.get_mut(range).ok_or(Error::InvalidRange)?;
    if selected.len() > MAX_PAYLOAD_BYTES {
        return Err(Error::LengthLimitExceeded);
    }
    for block in selected.chunks_exact_mut(8) {
        let mut left = u32::from_be_bytes([block[0], block[1], block[2], block[3]]);
        let mut right = u32::from_be_bytes([block[4], block[5], block[6], block[7]]);
        if decrypt {
            let mut sum = DELTA.wrapping_mul(ROUNDS as u32);
            for _ in 0..ROUNDS {
                right = right
                    .wrapping_sub(mix(left) ^ sum.wrapping_add(seed[((sum >> 11) & 3) as usize]));
                sum = sum.wrapping_sub(DELTA);
                left = left.wrapping_sub(mix(right) ^ sum.wrapping_add(seed[(sum & 3) as usize]));
            }
        } else {
            let mut sum = 0_u32;
            for _ in 0..ROUNDS {
                left = left.wrapping_add(mix(right) ^ sum.wrapping_add(seed[(sum & 3) as usize]));
                sum = sum.wrapping_add(DELTA);
                right = right
                    .wrapping_add(mix(left) ^ sum.wrapping_add(seed[((sum >> 11) & 3) as usize]));
            }
        }
        block[..4].copy_from_slice(&left.to_be_bytes());
        block[4..].copy_from_slice(&right.to_be_bytes());
    }
    Ok(())
}

fn mix(value: u32) -> u32 {
    ((value << 4) ^ (value >> 5)).wrapping_add(value)
}
