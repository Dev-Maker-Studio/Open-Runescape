use super::writer::{ClientPacketError, Text, Writer};

const DELTA: u32 = 0x9e37_79b9;
const ROUNDS: usize = 32;
const PADDING: usize = 7;

pub fn xtea_encrypt(bytes: &mut [u8], keys: &[i32; 4]) {
    for block in bytes.chunks_exact_mut(8) {
        let mut v0 = u32::from_be_bytes([block[0], block[1], block[2], block[3]]);
        let mut v1 = u32::from_be_bytes([block[4], block[5], block[6], block[7]]);
        let mut sum = 0u32;
        for _ in 0..ROUNDS {
            let key = keys[(sum & 3) as usize] as u32;
            v0 = v0.wrapping_add((((v1 << 4) ^ (v1 >> 5)).wrapping_add(v1)) ^ sum.wrapping_add(key));
            sum = sum.wrapping_add(DELTA);
            let key = keys[((sum >> 11) & 3) as usize] as u32;
            v1 = v1.wrapping_add((((v0 << 4) ^ (v0 >> 5)).wrapping_add(v0)) ^ sum.wrapping_add(key));
        }
        block[..4].copy_from_slice(&v0.to_be_bytes());
        block[4..].copy_from_slice(&v1.to_be_bytes());
    }
}

fn sealed(mut w: Writer, keys: &[i32; 4]) -> Vec<u8> {
    w.raw(&[0u8; PADDING]);
    let mut bytes = w.into_bytes();
    xtea_encrypt(&mut bytes, keys);
    bytes
}

pub fn sign_up_form(
    keys: &[i32; 4],
    email: impl Into<Text>,
    password: impl Into<Text>,
    age: i32,
    agreed_to_terms: bool,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(email)?
        .string(password)?
        .byte(age)
        .byte(i32::from(agreed_to_terms));
    Ok(sealed(w, keys))
}

pub fn check_email_validity(
    keys: &[i32; 4],
    email: impl Into<Text>,
) -> Result<Vec<u8>, ClientPacketError> {
    let mut w = Writer::new();
    w.string(email)?;
    Ok(sealed(w, keys))
}
