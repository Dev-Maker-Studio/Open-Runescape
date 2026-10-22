use crate::{Error, SensitiveBytes, buffer};
use num_bigint::BigInt;
use zeroize::Zeroizing;

pub const MAX_PLAINTEXT_BYTES: usize = 518;
pub const MODULUS_BYTES: usize = 128;
pub const MAX_ENCODED_BYTES: usize = MODULUS_BYTES + 3;
pub const PUBLIC_EXPONENT: u32 = 65537;
pub const PUBLIC_MODULUS_DECIMAL: &str = "117525752735533423040644219776209926525585489242340044375332234679786347045466594509203355398209678968096551043842518449703703964361320462967286756268851663407950384008240524570966471744081769815157355561961607944067477858512067883877129283799853947605780903005188603658779539811385137666347647991072028080201";

pub fn encrypt_public(plaintext: &[u8]) -> Result<SensitiveBytes, Error> {
    if plaintext.is_empty() {
        return Err(Error::EmptyRsaPlaintext);
    }
    if plaintext.len() > MAX_PLAINTEXT_BYTES {
        return Err(Error::LengthLimitExceeded);
    }
    let modulus = BigInt::parse_bytes(PUBLIC_MODULUS_DECIMAL.as_bytes(), 10)
        .ok_or(Error::InvalidPublicKey)?;
    let exponent = BigInt::from(PUBLIC_EXPONENT);
    let message = BigInt::from_signed_bytes_be(plaintext);
    let ciphertext = Zeroizing::new(message.modpow(&exponent, &modulus).to_signed_bytes_be());
    if ciphertext.len() + 2 > MAX_ENCODED_BYTES {
        return Err(Error::LengthLimitExceeded);
    }
    let mut output = buffer(ciphertext.len() + 2)?;
    output.extend_from_slice(&(ciphertext.len() as u16).to_be_bytes());
    output.extend_from_slice(&ciphertext);
    Ok(SensitiveBytes(output))
}
