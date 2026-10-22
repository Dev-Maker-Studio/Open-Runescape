use crate::{
    Error, INDEX_CRC_COUNT, LobbyIdentity, MAX_PASSWORD_BYTES, MAX_PAYLOAD_BYTES,
    MAX_USERNAME_BYTES, PREFERENCE_BYTES, RANDOM_BYTES, REVISION, SUB_REVISION, SensitiveBytes,
    buffer, rsa, strings, xtea,
};

pub const WORLD_OPCODE: u8 = 16;
pub const MACHINE_INFO_VERSION: u8 = 6;

pub struct MachineInformation<'a> {
    pub operating_system: u8,
    pub x64_os: bool,
    pub os_vendor: u8,
    pub java_vendor: u8,
    pub java_build: u8,
    pub java_sub_build: u8,
    pub java_update: u8,
    pub unknown_flag: bool,
    pub max_memory: u16,
    pub processors: u8,
    pub ram: u32,
    pub cpu_clock: u16,
    pub gpu_model: &'a str,
    pub gpu_driver_version: &'a str,
    pub directx_version: &'a str,
    pub directx_letter: &'a str,
    pub dx_driver_month: u8,
    pub dx_driver_year: u16,
    pub cpu_type: &'a str,
    pub cpu_data: &'a str,
    pub cpu_cores: u8,
    pub cpu_family_id: u8,
    pub cpu_feature_flags: &'a [u32; 3],
    pub cpu_model_info: u32,
}

impl MachineInformation<'_> {
    pub fn encode(&self) -> Result<SensitiveBytes, Error> {
        let length = self.encoded_length()?;
        let mut bytes = buffer(length)?;
        self.append(&mut bytes)?;
        Ok(SensitiveBytes(bytes))
    }

    fn encoded_length(&self) -> Result<usize, Error> {
        if self.ram > 0x00ffffff {
            return Err(Error::InvalidMachineInformation);
        }
        let mut length = 50;
        for (value, limit) in [
            (self.gpu_model, 40),
            (self.gpu_driver_version, 40),
            (self.directx_version, 10),
            (self.directx_letter, 10),
            (self.cpu_type, MAX_PAYLOAD_BYTES),
            (self.cpu_data, MAX_PAYLOAD_BYTES),
        ] {
            length += strings::validate(value, limit)?;
        }
        if length > MAX_PAYLOAD_BYTES {
            return Err(Error::LengthLimitExceeded);
        }
        Ok(length)
    }

    fn append(&self, bytes: &mut Vec<u8>) -> Result<(), Error> {
        bytes.extend_from_slice(&[
            MACHINE_INFO_VERSION,
            self.operating_system,
            u8::from(self.x64_os),
            self.os_vendor,
            self.java_vendor,
            self.java_build,
            self.java_sub_build,
            self.java_update,
            u8::from(self.unknown_flag),
        ]);
        bytes.extend_from_slice(&self.max_memory.to_be_bytes());
        bytes.push(self.processors);
        bytes.extend_from_slice(&self.ram.to_be_bytes()[1..]);
        bytes.extend_from_slice(&self.cpu_clock.to_be_bytes());
        for value in [
            self.gpu_model,
            self.gpu_driver_version,
            self.directx_version,
            self.directx_letter,
        ] {
            bytes.push(0);
            strings::append(bytes, value)?;
        }
        bytes.push(self.dx_driver_month);
        bytes.extend_from_slice(&self.dx_driver_year.to_be_bytes());
        for value in [self.cpu_type, self.cpu_data] {
            bytes.push(0);
            strings::append(bytes, value)?;
        }
        bytes.extend_from_slice(&[self.cpu_cores, self.cpu_family_id]);
        for word in self.cpu_feature_flags {
            bytes.extend_from_slice(&word.to_be_bytes());
        }
        bytes.extend_from_slice(&self.cpu_model_info.to_be_bytes());
        Ok(())
    }
}

pub struct WorldRequest<'a> {
    pub identity: LobbyIdentity<'a>,
    pub lobby_auth_token: &'a str,
    pub seed: &'a [u32; 4],
    pub social_nonce: u64,
    pub client_random_seed: u64,
    pub reconnecting: bool,
    pub display_mode: u8,
    pub screen_width: u16,
    pub screen_height: u16,
    pub anti_aliasing: u8,
    pub random24: &'a [u8; RANDOM_BYTES],
    pub affiliate_p: &'a str,
    pub affiliate: i32,
    pub preferences: &'a [u8; PREFERENCE_BYTES],
    pub machine_info: &'a MachineInformation<'a>,
    pub var_verify_id: i32,
    pub machine_uid: u64,
    pub login_token: &'a str,
    pub supplementary_login_data: Option<&'a str>,
    pub jagtheora_loaded: bool,
    pub is_applet: bool,
    pub has_signed_terms_agreement: bool,
    pub login_type: u8,
    pub session_token: i32,
    pub js5_auth_token: &'a str,
    pub different_world: bool,
    pub world_id: i32,
    pub index_crcs: &'a [u32; INDEX_CRC_COUNT],
}

pub struct EncodedWorldRequest {
    bytes: SensitiveBytes,
}

impl EncodedWorldRequest {
    pub fn wire_bytes(&self) -> &[u8] {
        self.bytes.as_bytes()
    }

    pub fn payload_bytes(&self) -> &[u8] {
        &self.bytes.as_bytes()[3..]
    }
}

pub fn encode_world_request(request: &WorldRequest<'_>) -> Result<EncodedWorldRequest, Error> {
    if request.lobby_auth_token.is_empty() {
        return Err(Error::MissingWorldToken);
    }
    let token_len = strings::validate(request.lobby_auth_token, MAX_PASSWORD_BYTES)?;
    let identity_len = match request.identity {
        LobbyIdentity::Username(name) => 2 + strings::validate(name, MAX_USERNAME_BYTES)?,
        LobbyIdentity::Token(u64::MAX) => return Err(Error::InvalidIdentityToken),
        LobbyIdentity::Token(_) => 9,
    };
    let machine_len = request.machine_info.encoded_length()?;
    let affiliate_len = strings::validate(request.affiliate_p, MAX_PAYLOAD_BYTES)?;
    let login_len = strings::validate(request.login_token, MAX_PAYLOAD_BYTES)?;
    let js5_len = strings::validate(request.js5_auth_token, MAX_PAYLOAD_BYTES)?;
    let supplementary_len = match request.supplementary_login_data {
        Some(value) => 1 + strings::validate(value, MAX_PAYLOAD_BYTES)?,
        None => 0,
    };
    let xtea_len = identity_len
        + 6
        + RANDOM_BYTES
        + affiliate_len
        + 1
        + 4
        + 1
        + PREFERENCE_BYTES
        + machine_len
        + 4
        + 8
        + login_len
        + 1
        + 1
        + supplementary_len
        + 4
        + 4
        + js5_len
        + 1
        + 1
        + 4
        + INDEX_CRC_COUNT * 4;
    if xtea_len + 12 > MAX_PAYLOAD_BYTES {
        return Err(Error::LengthLimitExceeded);
    }
    let mut secure = buffer(42 + token_len)?;
    secure.push(10);
    for word in request.seed {
        secure.extend_from_slice(&word.to_be_bytes());
    }
    secure.extend_from_slice(&0_u64.to_be_bytes());
    strings::append(&mut secure, request.lobby_auth_token)?;
    secure.extend_from_slice(&request.social_nonce.to_be_bytes());
    secure.extend_from_slice(&request.client_random_seed.to_be_bytes());
    let encrypted_secure = rsa::encrypt_public(&secure)?;
    let payload_len = 9 + encrypted_secure.as_bytes().len() + xtea_len;
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
    encrypted.push(request.display_mode);
    encrypted.extend_from_slice(&request.screen_width.to_be_bytes());
    encrypted.extend_from_slice(&request.screen_height.to_be_bytes());
    encrypted.push(request.anti_aliasing);
    encrypted.extend_from_slice(request.random24);
    strings::append(&mut encrypted, request.affiliate_p)?;
    encrypted.extend_from_slice(&request.affiliate.to_be_bytes());
    encrypted.push(PREFERENCE_BYTES as u8);
    encrypted.extend_from_slice(request.preferences);
    request.machine_info.append(&mut encrypted)?;
    encrypted.extend_from_slice(&request.var_verify_id.to_be_bytes());
    encrypted.extend_from_slice(&request.machine_uid.to_be_bytes());
    strings::append(&mut encrypted, request.login_token)?;
    encrypted.push(u8::from(request.supplementary_login_data.is_some()));
    if let Some(value) = request.supplementary_login_data {
        strings::append(&mut encrypted, value)?;
    }
    encrypted.extend_from_slice(&[
        u8::from(request.jagtheora_loaded),
        u8::from(request.is_applet),
        u8::from(request.has_signed_terms_agreement),
        request.login_type,
    ]);
    encrypted.extend_from_slice(&request.session_token.to_be_bytes());
    strings::append(&mut encrypted, request.js5_auth_token)?;
    encrypted.push(u8::from(request.different_world));
    encrypted.extend_from_slice(&request.world_id.to_be_bytes());
    for crc in request.index_crcs {
        encrypted.extend_from_slice(&crc.to_be_bytes());
    }
    xtea::encrypt(&mut encrypted, request.seed, 0..xtea_len)?;

    let mut bytes = buffer(payload_len + 3)?;
    bytes.push(WORLD_OPCODE);
    bytes.extend_from_slice(&(payload_len as u16).to_be_bytes());
    bytes.extend_from_slice(&REVISION.to_be_bytes());
    bytes.extend_from_slice(&SUB_REVISION.to_be_bytes());
    bytes.push(u8::from(request.reconnecting));
    bytes.extend_from_slice(encrypted_secure.as_bytes());
    bytes.extend_from_slice(&encrypted);
    Ok(EncodedWorldRequest {
        bytes: SensitiveBytes(bytes),
    })
}
