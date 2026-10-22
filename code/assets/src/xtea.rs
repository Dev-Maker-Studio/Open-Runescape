const DELTA: u32 = 0x9e37_79b9;
const ROUNDS: u32 = 32;
const START: usize = 5;

pub fn is_zero_key(key: &[i32; 4]) -> bool {
    key.iter().all(|word| *word == 0)
}

pub fn decrypt_container(container: &[u8], key: &[i32; 4]) -> Vec<u8> {
    let mut data = container.to_vec();
    if is_zero_key(key) || data.len() <= START {
        return data;
    }
    let words = key.map(|word| word as u32);
    let blocks = (data.len() - START) / 8;
    for block in 0..blocks {
        let at = START + block * 8;
        let mut left = u32::from_be_bytes([data[at], data[at + 1], data[at + 2], data[at + 3]]);
        let mut right =
            u32::from_be_bytes([data[at + 4], data[at + 5], data[at + 6], data[at + 7]]);
        let mut sum = DELTA.wrapping_mul(ROUNDS);
        for _ in 0..ROUNDS {
            right = right.wrapping_sub(
                (((left << 4) ^ (left >> 5)).wrapping_add(left))
                    ^ sum.wrapping_add(words[((sum >> 11) & 3) as usize]),
            );
            sum = sum.wrapping_sub(DELTA);
            left = left.wrapping_sub(
                (((right << 4) ^ (right >> 5)).wrapping_add(right))
                    ^ sum.wrapping_add(words[(sum & 3) as usize]),
            );
        }
        data[at..at + 4].copy_from_slice(&left.to_be_bytes());
        data[at + 4..at + 8].copy_from_slice(&right.to_be_bytes());
    }
    data
}

pub fn trim_revision(container: &[u8]) -> Option<&[u8]> {
    let mode = *container.first()?;
    let length = u32::from_be_bytes(container.get(1..5)?.try_into().ok()?) as usize;
    let body = match mode {
        0 => 5usize.checked_add(length)?,
        1 | 2 => 9usize.checked_add(length)?,
        _ => return None,
    };
    if container.len() == body || container.len() == body + 2 {
        container.get(..body)
    } else {
        None
    }
}
