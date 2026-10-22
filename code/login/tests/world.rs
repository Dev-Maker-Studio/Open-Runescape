use openrs_login::{
    Error, INDEX_CRC_COUNT, LobbyIdentity, MAX_PASSWORD_BYTES, MAX_PAYLOAD_BYTES,
    MAX_USERNAME_BYTES, MachineInformation, PREFERENCE_BYTES, RANDOM_BYTES, WorldRequest,
    encode_world_request, rsa, xtea,
};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const FEATURES: [u32; 3] = [0x89abcdef, 0x12345678, 0x80000000];
const RANDOM24: [u8; RANDOM_BYTES] = patterned(7, 3);
const PREFERENCES: [u8; PREFERENCE_BYTES] = patterned(11, 27);
const CRCS: [u32; INDEX_CRC_COUNT] = crc_words();

const fn patterned<const N: usize>(multiplier: u8, addend: u8) -> [u8; N] {
    let mut bytes = [0; N];
    let mut index = 0;
    while index < N {
        bytes[index] = (index as u8).wrapping_mul(multiplier).wrapping_add(addend);
        index += 1;
    }
    bytes
}

const fn crc_words() -> [u32; INDEX_CRC_COUNT] {
    let mut words = [0; INDEX_CRC_COUNT];
    let mut index = 0;
    while index < words.len() {
        words[index] = (index as u32)
            .wrapping_mul(0x1234567)
            .wrapping_add(0x80000000);
        index += 1;
    }
    words
}

fn machine() -> MachineInformation<'static> {
    MachineInformation {
        operating_system: 3,
        x64_os: true,
        os_vendor: 9,
        java_vendor: 4,
        java_build: 17,
        java_sub_build: 2,
        java_update: 3,
        unknown_flag: false,
        max_memory: 4096,
        processors: 8,
        ram: 0xabcdef,
        cpu_clock: 4200,
        gpu_model: "FAKE_GPU_€",
        gpu_driver_version: "FAKE_DRIVER_™",
        directx_version: "12.1",
        directx_letter: "A",
        dx_driver_month: 10,
        dx_driver_year: 2026,
        cpu_type: "FAKE_CPU",
        cpu_data: "OFFLINE_CPU_Œ",
        cpu_cores: 16,
        cpu_family_id: 254,
        cpu_feature_flags: &FEATURES,
        cpu_model_info: 0xfedcba98,
    }
}

fn request<'a>(machine: &'a MachineInformation<'a>) -> WorldRequest<'a> {
    WorldRequest {
        identity: LobbyIdentity::Username("offline_Œ_727"),
        lobby_auth_token: "FAKE_LOBBY_ISSUED_TOKEN_727",
        seed: &SEED,
        social_nonce: 0xfedcba9876543210,
        client_random_seed: 0x0123456789abcdef,
        reconnecting: false,
        display_mode: 2,
        screen_width: 1280,
        screen_height: 720,
        anti_aliasing: 4,
        random24: &RANDOM24,
        affiliate_p: "FAKE_AFFILIATE_™",
        affiliate: 0x89abcdef_u32 as i32,
        preferences: &PREFERENCES,
        machine_info: machine,
        var_verify_id: 0xfedcba98_u32 as i32,
        machine_uid: 0x8877665544332211,
        login_token: "FAKE_CLIENT_LOGIN_€",
        supplementary_login_data: Some("FAKE_EXTRA_ÿ"),
        jagtheora_loaded: true,
        is_applet: false,
        has_signed_terms_agreement: true,
        login_type: 7,
        session_token: 0x76543210,
        js5_auth_token: "FAKE_WORLD_JS5_™",
        different_world: true,
        world_id: 42,
        index_crcs: &CRCS,
    }
}

fn expect_error<T>(result: Result<T, Error>, expected: Error) {
    match result {
        Err(error) => assert_eq!(error, expected),
        Ok(_) => panic!("invalid fake world input was accepted"),
    }
}

fn check_fixture(request: &WorldRequest<'_>, wire: &[u8], plain: &[u8], secure: &[u8]) {
    let encoded = encode_world_request(request).unwrap();
    assert_eq!(encoded.wire_bytes(), wire);
    assert_eq!(wire[0], 16);
    assert_eq!(
        u16::from_be_bytes([wire[1], wire[2]]) as usize,
        encoded.payload_bytes().len()
    );
    assert_eq!(&encoded.payload_bytes()[..8], &[0, 0, 2, 215, 0, 0, 0, 1]);
    assert_eq!(wire[11], u8::from(request.reconnecting));
    let rsa_length = u16::from_be_bytes([wire[12], wire[13]]) as usize;
    let end = 14 + rsa_length;
    assert_eq!(
        &wire[12..end],
        rsa::encrypt_public(secure).unwrap().as_bytes()
    );
    let mut decrypted = wire[end..].to_vec();
    let length = decrypted.len();
    xtea::decrypt(&mut decrypted, request.seed, 0..length).unwrap();
    assert_eq!(decrypted, plain);
    let first_crc = length - INDEX_CRC_COUNT * 4;
    for (index, bytes) in decrypted[first_crc..].chunks_exact(4).enumerate() {
        assert_eq!(bytes, request.index_crcs[index].to_be_bytes());
    }
    let whole = length / 8 * 8;
    assert_eq!(&wire[end + whole..], &plain[whole..]);
    assert_eq!(request.seed, &SEED);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_system_info_writer_matches_without_machine_probing() {
    let machine = machine();
    assert_eq!(
        machine.encode().unwrap().as_bytes(),
        include_bytes!("fixtures/machine-v6.bytes")
    );
    assert_eq!(machine.encode().unwrap().as_bytes()[0], 6);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_world_request_full_packet_matches() {
    let machine = machine();
    check_fixture(
        &request(&machine),
        include_bytes!("fixtures/world-normal.wire"),
        include_bytes!("fixtures/world-normal.xtea"),
        include_bytes!("fixtures/world-normal.secure"),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_world_reconnecting_flag_token_identity_and_absent_supplement_match() {
    let machine = machine();
    let mut request = request(&machine);
    request.reconnecting = true;
    request.identity = LobbyIdentity::Token(0x8877665544332211);
    request.supplementary_login_data = None;
    check_fixture(
        &request,
        include_bytes!("fixtures/world-reconnecting.wire"),
        include_bytes!("fixtures/world-reconnecting.xtea"),
        include_bytes!("fixtures/world-reconnecting.secure"),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_world_empty_supplement_is_distinct_from_absent() {
    let machine = machine();
    let mut request = request(&machine);
    request.supplementary_login_data = Some("");
    check_fixture(
        &request,
        include_bytes!("fixtures/world-empty-supplement.wire"),
        include_bytes!("fixtures/world-empty-supplement.xtea"),
        include_bytes!("fixtures/world-empty-supplement.secure"),
    );
    let with_empty = encode_world_request(&request).unwrap();
    request.supplementary_login_data = None;
    let absent = encode_world_request(&request).unwrap();
    assert_eq!(
        with_empty.payload_bytes().len(),
        absent.payload_bytes().len() + 1
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_requires_explicit_recoverable_lobby_token() {
    let machine = machine();
    let mut request = request(&machine);
    request.lobby_auth_token = "";
    expect_error(encode_world_request(&request), Error::MissingWorldToken);
    let maximum = "€".repeat(MAX_PASSWORD_BYTES);
    request.lobby_auth_token = &maximum;
    assert!(encode_world_request(&request).is_ok());
    let oversized = "a".repeat(MAX_PASSWORD_BYTES + 1);
    request.lobby_auth_token = &oversized;
    expect_error(encode_world_request(&request), Error::LengthLimitExceeded);
    for bad in ["FAKE_TOKEN_\0", "FAKE_TOKEN_☃"] {
        request.lobby_auth_token = bad;
        assert!(encode_world_request(&request).is_err());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_strings_and_identity_are_strictly_validated() {
    let machine = machine();
    for bad in ["FAKE_\0", "FAKE_☃"] {
        for field in 0..5 {
            let mut request = request(&machine);
            match field {
                0 => request.identity = LobbyIdentity::Username(bad),
                1 => request.affiliate_p = bad,
                2 => request.login_token = bad,
                3 => request.supplementary_login_data = Some(bad),
                _ => request.js5_auth_token = bad,
            }
            assert!(encode_world_request(&request).is_err());
        }
    }
    let long_name = "a".repeat(MAX_USERNAME_BYTES + 1);
    let mut request = request(&machine);
    request.identity = LobbyIdentity::Username(&long_name);
    expect_error(encode_world_request(&request), Error::LengthLimitExceeded);
    request.identity = LobbyIdentity::Token(u64::MAX);
    expect_error(encode_world_request(&request), Error::InvalidIdentityToken);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn machine_fields_enforce_native_truncation_limits_without_lossy_truncation() {
    let mut machine = machine();
    machine.ram = 0x1000000;
    expect_error(machine.encode(), Error::InvalidMachineInformation);
    machine.ram = 0xffffff;
    assert!(machine.encode().is_ok());
    for (field, limit) in [(0, 40), (1, 40), (2, 10), (3, 10)] {
        let exact = "€".repeat(limit);
        let oversized = "a".repeat(limit + 1);
        for (value, valid) in [(&exact, true), (&oversized, false)] {
            let mut machine = self::machine();
            match field {
                0 => machine.gpu_model = value,
                1 => machine.gpu_driver_version = value,
                2 => machine.directx_version = value,
                _ => machine.directx_letter = value,
            }
            assert_eq!(machine.encode().is_ok(), valid);
        }
    }
    for field in 0..6 {
        let mut machine = self::machine();
        match field {
            0 => machine.gpu_model = "FAKE_☃",
            1 => machine.gpu_driver_version = "FAKE_☃",
            2 => machine.directx_version = "FAKE_☃",
            3 => machine.directx_letter = "FAKE_☃",
            4 => machine.cpu_type = "FAKE_☃",
            _ => machine.cpu_data = "FAKE_☃",
        }
        expect_error(machine.encode(), Error::UnsupportedCharacter);
        expect_error(
            encode_world_request(&request(&machine)),
            Error::UnsupportedCharacter,
        );
    }
    let huge = "a".repeat(MAX_PAYLOAD_BYTES);
    machine.cpu_data = &huge;
    expect_error(machine.encode(), Error::LengthLimitExceeded);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_payload_exact_u16_limit_and_overflow_are_checked() {
    let machine = machine();
    let initial = request(&machine);
    let length = encode_world_request(&initial)
        .unwrap()
        .payload_bytes()
        .len();
    let token_length = MAX_PAYLOAD_BYTES - length + initial.js5_auth_token.chars().count();
    let exact = "a".repeat(token_length);
    let mut request = request(&machine);
    request.js5_auth_token = &exact;
    let encoded = encode_world_request(&request).unwrap();
    assert_eq!(encoded.payload_bytes().len(), MAX_PAYLOAD_BYTES);
    assert_eq!(&encoded.wire_bytes()[1..3], &[255, 255]);
    let oversized = "a".repeat(token_length + 1);
    request.js5_auth_token = &oversized;
    expect_error(encode_world_request(&request), Error::LengthLimitExceeded);
    let huge = "a".repeat(MAX_PAYLOAD_BYTES + 1);
    request.affiliate_p = &huge;
    expect_error(encode_world_request(&request), Error::LengthLimitExceeded);
}
