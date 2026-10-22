#![forbid(unsafe_code)]

pub mod clan;
pub mod item_containers;
pub mod lobby_stream;
pub mod player_stats;
pub mod response;
pub mod rsa;
pub mod session;
pub mod social;
pub mod strings;
pub mod world;
pub mod world_entry;
pub mod world_list;
pub mod world_packets;
pub mod world_state;
pub mod xtea;

pub use world::{EncodedWorldRequest, MachineInformation, WorldRequest, encode_world_request};

use std::fmt;
use zeroize::Zeroizing;

pub const REVISION: u32 = 727;
pub const SUB_REVISION: u32 = 1;
pub const HANDSHAKE_OPCODE: u8 = 14;
pub const LOBBY_OPCODE: u8 = 19;
pub const MAX_PAYLOAD_BYTES: usize = u16::MAX as usize;
pub const RANDOM_BYTES: usize = 24;
pub const PREFERENCE_BYTES: usize = 35;
pub const INDEX_CRC_COUNT: usize = 36;
pub const MAX_USERNAME_BYTES: usize = 320;
pub const MAX_PASSWORD_BYTES: usize = rsa::MODULUS_BYTES - 42;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Error {
    InvalidLimit,
    LengthLimitExceeded,
    InvalidRange,
    UnsupportedCharacter,
    EmbeddedNul,
    InvalidCp1252Byte,
    UnterminatedString,
    InvalidJagPrefix,
    EmptyRsaPlaintext,
    InvalidPublicKey,
    InvalidIdentityToken,
    AllocationFailed,
    UnexpectedHandshakeResponse,
    InvalidSeed,
    MissingWorldToken,
    InvalidMachineInformation,
    UnexpectedEof,
    TrailingData,
    DecoderFinished,
    DecoderFailed,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::InvalidLimit => "invalid login codec limit",
            Self::LengthLimitExceeded => "login codec field or payload exceeds its bound",
            Self::InvalidRange => "invalid XTEA byte range",
            Self::UnsupportedCharacter => "character is not representable in native CP1252",
            Self::EmbeddedNul => "native string contains a null character",
            Self::InvalidCp1252Byte => "undefined native CP1252 byte",
            Self::UnterminatedString => "unterminated native string",
            Self::InvalidJagPrefix => "invalid native jag string prefix",
            Self::EmptyRsaPlaintext => "RSA plaintext must not be empty",
            Self::InvalidPublicKey => "invalid public RSA key",
            Self::InvalidIdentityToken => "identity token is reserved by the native username flag",
            Self::AllocationFailed => "login codec buffer allocation failed",
            Self::UnexpectedHandshakeResponse => "lobby handshake was not accepted",
            Self::InvalidSeed => "invalid ISAAC seed",
            Self::MissingWorldToken => "world login requires a caller-supplied lobby-issued token",
            Self::InvalidMachineInformation => "invalid version 6 machine information",
            Self::UnexpectedEof => "truncated login response",
            Self::TrailingData => "unexpected trailing login success data",
            Self::DecoderFinished => "login response decoder has completed",
            Self::DecoderFailed => "login response decoder cannot resume after an error",
        })
    }
}

impl std::error::Error for Error {}

pub struct SensitiveBytes(Zeroizing<Vec<u8>>);

impl SensitiveBytes {
    pub fn as_bytes(&self) -> &[u8] {
        &self.0
    }
}

pub struct SensitiveString(Zeroizing<String>);

impl SensitiveString {
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

pub enum LobbyIdentity<'a> {
    Username(&'a str),
    Token(u64),
}

pub struct LobbyRequest<'a> {
    pub identity: LobbyIdentity<'a>,
    pub password: &'a str,
    pub seed: &'a [u32; 4],
    pub social_nonce: u64,
    pub client_random_seed: u64,
    pub game_id: u8,
    pub language: u8,
    pub random24: &'a [u8; RANDOM_BYTES],
    pub affiliate_p: &'a str,
    pub preferences: &'a [u8; PREFERENCE_BYTES],
    pub login_token: &'a str,
    pub affiliate: i32,
    pub session_token: i32,
    pub js5_auth_token: &'a str,
    pub index_crcs: &'a [u32; INDEX_CRC_COUNT],
}

pub struct EncodedLobbyRequest {
    bytes: SensitiveBytes,
}

impl EncodedLobbyRequest {
    pub fn wire_bytes(&self) -> &[u8] {
        self.bytes.as_bytes()
    }

    pub fn payload_bytes(&self) -> &[u8] {
        &self.bytes.as_bytes()[3..]
    }
}

pub fn handshake_bytes() -> [u8; 1] {
    [HANDSHAKE_OPCODE]
}

pub fn accept_handshake(response: u8) -> Result<(), Error> {
    if response == 0 {
        Ok(())
    } else {
        Err(Error::UnexpectedHandshakeResponse)
    }
}

pub fn isaac_ciphers(
    seed: &[u32; 4],
) -> Result<(openrs_protocol::isaac::Isaac, openrs_protocol::isaac::Isaac), Error> {
    let incoming_seed = Zeroizing::new(seed.map(|word| word.wrapping_add(50)));
    Ok((
        openrs_protocol::isaac::Isaac::new(seed).map_err(|_| Error::InvalidSeed)?,
        openrs_protocol::isaac::Isaac::new(&*incoming_seed).map_err(|_| Error::InvalidSeed)?,
    ))
}

pub fn encode_lobby_request(request: &LobbyRequest<'_>) -> Result<EncodedLobbyRequest, Error> {
    let password_len = strings::validate(request.password, MAX_PASSWORD_BYTES)?;
    let identity_len = match request.identity {
        LobbyIdentity::Username(name) => 1 + strings::validate(name, MAX_USERNAME_BYTES)? + 1,
        LobbyIdentity::Token(u64::MAX) => return Err(Error::InvalidIdentityToken),
        LobbyIdentity::Token(_) => 9,
    };
    let affiliate_len = strings::validate(request.affiliate_p, MAX_PAYLOAD_BYTES)?;
    let login_token_len = strings::validate(request.login_token, MAX_PAYLOAD_BYTES)?;
    let js5_token_len = strings::validate(request.js5_auth_token, MAX_PAYLOAD_BYTES)?;
    let xtea_len = identity_len
        + 2
        + RANDOM_BYTES
        + affiliate_len
        + 1
        + 1
        + PREFERENCE_BYTES
        + login_token_len
        + 1
        + 8
        + js5_token_len
        + 1
        + 4 * INDEX_CRC_COUNT;
    if xtea_len + 11 > MAX_PAYLOAD_BYTES {
        return Err(Error::LengthLimitExceeded);
    }

    let mut plain = buffer(password_len + 42)?;
    plain.push(10);
    for word in request.seed {
        plain.extend_from_slice(&word.to_be_bytes());
    }
    plain.extend_from_slice(&0_u64.to_be_bytes());
    strings::append(&mut plain, request.password)?;
    plain.extend_from_slice(&request.social_nonce.to_be_bytes());
    plain.extend_from_slice(&request.client_random_seed.to_be_bytes());
    let rsa = rsa::encrypt_public(&plain)?;
    let payload_len = 8 + rsa.as_bytes().len() + xtea_len;
    if payload_len > MAX_PAYLOAD_BYTES {
        return Err(Error::LengthLimitExceeded);
    }

    let mut encrypted = buffer(xtea_len)?;
    match request.identity {
        LobbyIdentity::Username(name) => {
            encrypted.push(1);
            strings::append(&mut encrypted, name)?;
        }
        LobbyIdentity::Token(token) => {
            encrypted.push(0);
            encrypted.extend_from_slice(&token.to_be_bytes());
        }
    }
    encrypted.extend_from_slice(&[request.game_id, request.language]);
    encrypted.extend_from_slice(request.random24);
    strings::append(&mut encrypted, request.affiliate_p)?;
    encrypted.push(PREFERENCE_BYTES as u8);
    encrypted.extend_from_slice(request.preferences);
    strings::append(&mut encrypted, request.login_token)?;
    encrypted.extend_from_slice(&request.affiliate.to_be_bytes());
    encrypted.extend_from_slice(&request.session_token.to_be_bytes());
    strings::append(&mut encrypted, request.js5_auth_token)?;
    for crc in request.index_crcs {
        encrypted.extend_from_slice(&crc.to_be_bytes());
    }
    xtea::encrypt(&mut encrypted, request.seed, 0..xtea_len)?;

    let mut bytes = buffer(payload_len + 3)?;
    bytes.push(LOBBY_OPCODE);
    bytes.extend_from_slice(&(payload_len as u16).to_be_bytes());
    bytes.extend_from_slice(&REVISION.to_be_bytes());
    bytes.extend_from_slice(&SUB_REVISION.to_be_bytes());
    bytes.extend_from_slice(rsa.as_bytes());
    bytes.extend_from_slice(&encrypted);
    Ok(EncodedLobbyRequest {
        bytes: SensitiveBytes(bytes),
    })
}

fn buffer(capacity: usize) -> Result<Zeroizing<Vec<u8>>, Error> {
    let mut bytes = Vec::new();
    bytes
        .try_reserve_exact(capacity)
        .map_err(|_| Error::AllocationFailed)?;
    Ok(Zeroizing::new(bytes))
}
