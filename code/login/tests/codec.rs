use openrs_login::{
    Error, INDEX_CRC_COUNT, LobbyIdentity, LobbyRequest, MAX_PASSWORD_BYTES, MAX_PAYLOAD_BYTES,
    MAX_USERNAME_BYTES, PREFERENCE_BYTES, RANDOM_BYTES, accept_handshake, encode_lobby_request,
    handshake_bytes, isaac_ciphers, rsa, strings, xtea,
};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const RANDOM24: [u8; RANDOM_BYTES] = pattern();
const PREFERENCES: [u8; PREFERENCE_BYTES] = preferences();
const CRCS: [u32; INDEX_CRC_COUNT] = crcs();

const fn pattern() -> [u8; RANDOM_BYTES] {
    let mut bytes = [0; RANDOM_BYTES];
    let mut index = 0;
    while index < bytes.len() {
        bytes[index] = (index as u8).wrapping_mul(7).wrapping_add(3);
        index += 1;
    }
    bytes
}

const fn preferences() -> [u8; PREFERENCE_BYTES] {
    let mut bytes = [0; PREFERENCE_BYTES];
    let mut index = 0;
    while index < bytes.len() {
        bytes[index] = (index as u8).wrapping_mul(11).wrapping_add(27);
        index += 1;
    }
    bytes
}

const fn crcs() -> [u32; INDEX_CRC_COUNT] {
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

fn request() -> LobbyRequest<'static> {
    LobbyRequest {
        identity: LobbyIdentity::Username("offline_Œ_727"),
        password: "OFFLINE_ONLY_€",
        seed: &SEED,
        social_nonce: 0xfedcba9876543210,
        client_random_seed: 0x0123456789abcdef,
        game_id: 0,
        language: 2,
        random24: &RANDOM24,
        affiliate_p: "FAKE_AFFILIATE_™",
        preferences: &PREFERENCES,
        login_token: "FAKE_LOGIN_€",
        affiliate: 0x89abcdef_u32 as i32,
        session_token: 0x76543210,
        js5_auth_token: "FAKE_JS5_ÿ",
        index_crcs: &CRCS,
    }
}

fn expect_error<T>(result: Result<T, Error>, expected: Error) {
    match result {
        Err(error) => assert_eq!(error, expected),
        Ok(_) => panic!("invalid fake input was accepted"),
    }
}

fn native_text() -> String {
    let mut text: String = (1_u8..=127).map(char::from).collect();
    text.push_str("€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ");
    text.extend((160_u8..=255).map(char::from));
    text
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn handshake_is_native_and_rejects_every_nonzero_response() {
    assert_eq!(
        handshake_bytes(),
        *include_bytes!("fixtures/handshake.wire")
    );
    assert_eq!(accept_handshake(0), Ok(()));
    for response in 1..=255 {
        assert_eq!(
            accept_handshake(response),
            Err(Error::UnexpectedHandshakeResponse)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_xtea_encrypt_decrypt_ranges_and_full_blocks() {
    let original = include_bytes!("fixtures/xtea.input");
    for (seed, range_fixture, full_fixture) in [
        (
            SEED,
            include_bytes!("fixtures/xtea-mixed.encrypted").as_slice(),
            include_bytes!("fixtures/xtea-mixed-full.encrypted").as_slice(),
        ),
        (
            [0; 4],
            include_bytes!("fixtures/xtea-zero.encrypted").as_slice(),
            include_bytes!("fixtures/xtea-zero-full.encrypted").as_slice(),
        ),
    ] {
        let mut data = original.to_vec();
        xtea::encrypt(&mut data, &seed, 3..2054).unwrap();
        assert_eq!(data, range_fixture);
        xtea::decrypt(&mut data, &seed, 3..2054).unwrap();
        assert_eq!(data, original);
        let mut full = original.to_vec();
        xtea::encrypt(&mut full, &seed, 0..original.len()).unwrap();
        assert_eq!(full, full_fixture);
        xtea::decrypt(&mut full, &seed, 0..original.len()).unwrap();
        assert_eq!(full, original);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn xtea_known_zero_vector() {
    let mut bytes = [0; 8];
    xtea::encrypt(&mut bytes, &[0; 4], 0..8).unwrap();
    assert_eq!(bytes, [0xde, 0xe9, 0xd4, 0xd8, 0xf7, 0x13, 0x1e, 0xd9]);
    xtea::decrypt(&mut bytes, &[0; 4], 0..8).unwrap();
    assert_eq!(bytes, [0; 8]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn xtea_preserves_prefix_suffix_and_every_short_remainder() {
    for length in 0..=512 {
        let original: Vec<u8> = (0..length + 11)
            .map(|index| (index as u8).wrapping_mul(19))
            .collect();
        let mut bytes = original.clone();
        xtea::encrypt(&mut bytes, &SEED, 3..3 + length).unwrap();
        let whole_end = 3 + length / 8 * 8;
        assert_eq!(&bytes[..3], &original[..3]);
        assert_eq!(&bytes[whole_end..], &original[whole_end..]);
        xtea::decrypt(&mut bytes, &SEED, 3..3 + length).unwrap();
        assert_eq!(bytes, original);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn xtea_checks_ranges_and_bound_before_mutation() {
    let start = 9;
    let end = 8;
    for range in [start..end, 0..17, 17..17, usize::MAX..usize::MAX] {
        let mut bytes = [0x55; 16];
        assert_eq!(
            xtea::encrypt(&mut bytes, &SEED, range.clone()),
            Err(Error::InvalidRange)
        );
        assert_eq!(
            xtea::decrypt(&mut bytes, &SEED, range),
            Err(Error::InvalidRange)
        );
        assert_eq!(bytes, [0x55; 16]);
    }
    let mut bytes = vec![0x55; MAX_PAYLOAD_BYTES + 1];
    assert_eq!(
        xtea::encrypt(&mut bytes, &SEED, 0..MAX_PAYLOAD_BYTES + 1),
        Err(Error::LengthLimitExceeded)
    );
    assert_eq!(
        xtea::decrypt(&mut bytes, &SEED, 0..MAX_PAYLOAD_BYTES + 1),
        Err(Error::LengthLimitExceeded)
    );
    assert!(bytes.iter().all(|&byte| byte == 0x55));
    xtea::encrypt(&mut bytes, &SEED, 0..MAX_PAYLOAD_BYTES).unwrap();
    xtea::decrypt(&mut bytes, &SEED, 0..MAX_PAYLOAD_BYTES).unwrap();
    assert!(bytes.iter().all(|&byte| byte == 0x55));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_cp1252_and_jag_fixtures_round_trip_exactly() {
    let text = native_text();
    let regular = include_bytes!("fixtures/cp1252.string");
    let jag = include_bytes!("fixtures/cp1252.jag");
    assert_eq!(strings::encode(&text, 250).unwrap().as_bytes(), regular);
    assert_eq!(strings::encode_jag(&text, 250).unwrap().as_bytes(), jag);
    let decoded = strings::decode(regular, 250).unwrap();
    assert_eq!(decoded.value.as_str(), text);
    assert_eq!(decoded.consumed, regular.len());
    let decoded = strings::decode_jag(jag, 250).unwrap();
    assert_eq!(decoded.value.as_str(), text);
    assert_eq!(decoded.consumed, jag.len());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn strings_reject_lossy_encoding_nulls_and_undefined_bytes() {
    for text in ["𐀀", "☃", "\u{80}", "\u{81}", "a\u{100}"] {
        expect_error(strings::encode(text, 320), Error::UnsupportedCharacter);
        expect_error(strings::encode_jag(text, 320), Error::UnsupportedCharacter);
    }
    for text in ["\0", "fake\0only"] {
        expect_error(strings::encode(text, 320), Error::EmbeddedNul);
        expect_error(strings::encode_jag(text, 320), Error::EmbeddedNul);
    }
    for byte in [0x81, 0x8d, 0x8f, 0x90, 0x9d] {
        expect_error(strings::decode(&[byte, 0], 1), Error::InvalidCp1252Byte);
        expect_error(
            strings::decode_jag(&[0, byte, 0], 1),
            Error::InvalidCp1252Byte,
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn string_limits_terminators_prefixes_and_consumption_are_strict() {
    assert_eq!(strings::encode("", 0).unwrap().as_bytes(), [0]);
    assert_eq!(strings::encode_jag("", 0).unwrap().as_bytes(), [0, 0]);
    assert_eq!(strings::decode(&[0, 255], 0).unwrap().consumed, 1);
    assert_eq!(strings::decode_jag(&[0, 0, 255], 0).unwrap().consumed, 2);
    expect_error(strings::encode("a", 0), Error::LengthLimitExceeded);
    expect_error(strings::encode("ab", 1), Error::LengthLimitExceeded);
    expect_error(strings::encode("", usize::MAX), Error::InvalidLimit);
    expect_error(strings::decode(&[0], usize::MAX), Error::InvalidLimit);
    expect_error(strings::decode(&[], 0), Error::UnterminatedString);
    expect_error(strings::decode(b"a", 1), Error::UnterminatedString);
    expect_error(strings::decode(b"aa\0", 1), Error::LengthLimitExceeded);
    expect_error(strings::decode_jag(&[], 10), Error::InvalidJagPrefix);
    expect_error(strings::decode_jag(&[1, 0], 10), Error::InvalidJagPrefix);
    expect_error(strings::decode_jag(&[0], 10), Error::UnterminatedString);
    let fixture = include_bytes!("fixtures/cp1252.string");
    for end in 0..fixture.len() {
        expect_error(
            strings::decode(&fixture[..end], 250),
            Error::UnterminatedString,
        );
    }
    let jag = include_bytes!("fixtures/cp1252.jag");
    for end in 1..jag.len() {
        expect_error(
            strings::decode_jag(&jag[..end], 250),
            Error::UnterminatedString,
        );
    }
    let maximum = "€".repeat(MAX_PAYLOAD_BYTES);
    let encoded = strings::encode(&maximum, MAX_PAYLOAD_BYTES).unwrap();
    assert_eq!(encoded.as_bytes().len(), MAX_PAYLOAD_BYTES + 1);
    assert_eq!(
        strings::decode(encoded.as_bytes(), MAX_PAYLOAD_BYTES)
            .unwrap()
            .value
            .as_str(),
        maximum
    );
    expect_error(
        strings::encode(&maximum, MAX_PAYLOAD_BYTES - 1),
        Error::LengthLimitExceeded,
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_rsa_java_signed_bigint_and_length_fixtures_match() {
    for (plain, expected) in [
        (
            include_bytes!("fixtures/rsa-zero.plain").as_slice(),
            include_bytes!("fixtures/rsa-zero.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-one.plain").as_slice(),
            include_bytes!("fixtures/rsa-one.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-negative.plain").as_slice(),
            include_bytes!("fixtures/rsa-negative.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-minus-one.plain").as_slice(),
            include_bytes!("fixtures/rsa-minus-one.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-positive.plain").as_slice(),
            include_bytes!("fixtures/rsa-positive.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-maximum.plain").as_slice(),
            include_bytes!("fixtures/rsa-maximum.rsa").as_slice(),
        ),
        (
            include_bytes!("fixtures/rsa-sign-padding.plain").as_slice(),
            include_bytes!("fixtures/rsa-sign-padding.rsa").as_slice(),
        ),
    ] {
        let encoded = rsa::encrypt_public(plain).unwrap();
        assert_eq!(encoded.as_bytes(), expected);
        assert_eq!(
            u16::from_be_bytes([expected[0], expected[1]]) as usize,
            expected.len() - 2
        );
    }
    assert_eq!(rsa::encrypt_public(&[0]).unwrap().as_bytes(), [0, 1, 0]);
    let padded = include_bytes!("fixtures/rsa-sign-padding.rsa");
    assert_eq!(padded.len(), 131);
    assert_eq!(&padded[..3], [0, 129, 0]);
    assert_ne!(padded[3] & 0x80, 0);
    assert_ne!(
        rsa::encrypt_public(&[0x80]).unwrap().as_bytes(),
        rsa::encrypt_public(&[0, 0x80]).unwrap().as_bytes()
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn rsa_plaintext_is_bounded_and_empty_is_not_zero() {
    expect_error(rsa::encrypt_public(&[]), Error::EmptyRsaPlaintext);
    expect_error(
        rsa::encrypt_public(&vec![0; rsa::MAX_PLAINTEXT_BYTES + 1]),
        Error::LengthLimitExceeded,
    );
    assert_eq!(
        rsa::encrypt_public(&vec![0; rsa::MAX_PLAINTEXT_BYTES])
            .unwrap()
            .as_bytes(),
        [0, 1, 0]
    );
}

fn check_packet(request: &LobbyRequest<'_>, fixture: &[u8], plain_xtea: &[u8], secure: &[u8]) {
    let seed_before = *request.seed;
    let encoded = encode_lobby_request(request).unwrap();
    assert_eq!(encoded.wire_bytes(), fixture);
    let wire = encoded.wire_bytes();
    assert_eq!(wire[0], 19);
    assert_eq!(
        u16::from_be_bytes([wire[1], wire[2]]) as usize,
        wire.len() - 3
    );
    assert_eq!(&encoded.payload_bytes()[..8], &[0, 0, 2, 215, 0, 0, 0, 1]);
    let rsa_len = u16::from_be_bytes([wire[11], wire[12]]) as usize;
    let rsa_end = 13 + rsa_len;
    assert_eq!(
        &wire[11..rsa_end],
        rsa::encrypt_public(secure).unwrap().as_bytes()
    );
    let mut decrypted = wire[rsa_end..].to_vec();
    let length = decrypted.len();
    xtea::decrypt(&mut decrypted, request.seed, 0..length).unwrap();
    assert_eq!(decrypted, plain_xtea);
    let crcs_start = decrypted.len() - 36 * 4;
    for (index, bytes) in decrypted[crcs_start..].chunks_exact(4).enumerate() {
        assert_eq!(bytes, request.index_crcs[index].to_be_bytes());
    }
    let whole_end = length / 8 * 8;
    assert_eq!(&wire[rsa_end + whole_end..], &plain_xtea[whole_end..]);
    assert_eq!(*request.seed, seed_before);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_native_lobby_username_fixture_matches() {
    check_packet(
        &request(),
        include_bytes!("fixtures/lobby-username.wire"),
        include_bytes!("fixtures/lobby-username.xtea"),
        include_bytes!("fixtures/lobby-username.secure"),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_native_lobby_token_identity_fixture_matches() {
    let mut request = request();
    request.identity = LobbyIdentity::Token(0x8877665544332211);
    check_packet(
        &request,
        include_bytes!("fixtures/lobby-token.wire"),
        include_bytes!("fixtures/lobby-token.xtea"),
        include_bytes!("fixtures/lobby-token.secure"),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_native_lobby_empty_tokens_fixture_matches() {
    let mut request = request();
    request.affiliate_p = "";
    request.login_token = "";
    request.js5_auth_token = "";
    check_packet(
        &request,
        include_bytes!("fixtures/lobby-empty-tokens.wire"),
        include_bytes!("fixtures/lobby-empty-tokens.xtea"),
        include_bytes!("fixtures/lobby-empty-tokens.secure"),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_request_strings_reject_loss_and_nulls() {
    for bad in ["fake\0only", "fake☃only"] {
        let expected = if bad.contains('\0') {
            Error::EmbeddedNul
        } else {
            Error::UnsupportedCharacter
        };
        for field in 0..5 {
            let mut request = request();
            match field {
                0 => request.identity = LobbyIdentity::Username(bad),
                1 => request.password = bad,
                2 => request.affiliate_p = bad,
                3 => request.login_token = bad,
                _ => request.js5_auth_token = bad,
            }
            expect_error(encode_lobby_request(&request), expected);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_username_and_secure_block_bounds_are_enforced() {
    let name = "a".repeat(MAX_USERNAME_BYTES);
    let password = "€".repeat(MAX_PASSWORD_BYTES);
    let mut request = request();
    request.identity = LobbyIdentity::Username(&name);
    request.password = &password;
    assert!(encode_lobby_request(&request).is_ok());
    let too_long_name = "a".repeat(MAX_USERNAME_BYTES + 1);
    request.identity = LobbyIdentity::Username(&too_long_name);
    expect_error(encode_lobby_request(&request), Error::LengthLimitExceeded);
    request.identity = LobbyIdentity::Username(&name);
    let too_long_password = "a".repeat(MAX_PASSWORD_BYTES + 1);
    request.password = &too_long_password;
    expect_error(encode_lobby_request(&request), Error::LengthLimitExceeded);
    request.password = "";
    request.identity = LobbyIdentity::Token(u64::MAX);
    expect_error(encode_lobby_request(&request), Error::InvalidIdentityToken);
    request.identity = LobbyIdentity::Token(0);
    assert!(encode_lobby_request(&request).is_ok());
    request.identity = LobbyIdentity::Username("");
    assert!(encode_lobby_request(&request).is_ok());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn lobby_payload_accepts_exact_u16_limit_and_rejects_one_more_byte() {
    let baseline = encode_lobby_request(&request())
        .unwrap()
        .payload_bytes()
        .len();
    let token_length = MAX_PAYLOAD_BYTES - baseline + "FAKE_LOGIN_€".chars().count();
    let token = "a".repeat(token_length);
    let mut request = request();
    request.login_token = &token;
    let encoded = encode_lobby_request(&request).unwrap();
    assert_eq!(encoded.payload_bytes().len(), MAX_PAYLOAD_BYTES);
    assert_eq!(&encoded.wire_bytes()[1..3], &[255, 255]);
    let oversized = "a".repeat(token_length + 1);
    request.login_token = &oversized;
    expect_error(encode_lobby_request(&request), Error::LengthLimitExceeded);
    let enormous = "a".repeat(MAX_PAYLOAD_BYTES + 1);
    request.login_token = &enormous;
    expect_error(encode_lobby_request(&request), Error::LengthLimitExceeded);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_isaac_directions_and_wrapping_seed_plus_50_match() {
    let (mut outgoing, mut incoming) = isaac_ciphers(&SEED).unwrap();
    let outgoing_fixture = include_bytes!("fixtures/isaac-outgoing.words");
    let incoming_fixture = include_bytes!("fixtures/isaac-incoming.words");
    for word in outgoing_fixture.chunks_exact(4) {
        assert_eq!(outgoing.next_u32().to_be_bytes(), word);
    }
    for word in incoming_fixture.chunks_exact(4) {
        assert_eq!(incoming.next_u32().to_be_bytes(), word);
    }
    assert_eq!(SEED, [1, u32::MAX - 1, 0x12345678, 0x80000000]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn lobby_secure_block_is_bounded_by_rsa_recoverability() {
    assert_eq!(MAX_PASSWORD_BYTES, 86);
    let modulus =
        num_bigint::BigInt::parse_bytes(rsa::PUBLIC_MODULUS_DECIMAL.as_bytes(), 10).unwrap();
    let mut message = vec![0; rsa::MODULUS_BYTES];
    message[0] = 10;
    assert!(num_bigint::BigInt::from_signed_bytes_be(&message) < modulus);
    message.push(0);
    assert!(num_bigint::BigInt::from_signed_bytes_be(&message) > modulus);
    let password = "a".repeat(87);
    let mut request = request();
    request.password = &password;
    expect_error(encode_lobby_request(&request), Error::LengthLimitExceeded);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn failures_never_echo_fake_field_values() {
    let mut request = request();
    request.password = "DO_NOT_ECHO_FAKE_☃";
    match encode_lobby_request(&request) {
        Err(error) => {
            assert_eq!(
                format!("{error}"),
                "character is not representable in native CP1252"
            );
            assert_eq!(format!("{error:?}"), "UnsupportedCharacter");
        }
        Ok(_) => panic!("invalid fake input was accepted"),
    }
}
