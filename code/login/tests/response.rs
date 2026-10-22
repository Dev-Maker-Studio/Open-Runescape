use openrs_login::response::{
    LobbySuccess, LoginKind, LoginResponse, LoginResponseDecoder, MAX_SUCCESS_BYTES,
    MIN_LOBBY_SUCCESS_BYTES, MIN_WORLD_SUCCESS_BYTES, WorldSuccess, decode_lobby_success,
    decode_world_success,
};
use openrs_login::{Error, SensitiveString, strings};

fn expect_error<T>(result: Result<T, Error>, expected: Error) {
    match result {
        Err(error) => assert_eq!(error, expected),
        Ok(_) => panic!("invalid fake login response was accepted"),
    }
}

fn append_string(output: &mut Vec<u8>, value: &SensitiveString, jag: bool) {
    let bytes = if jag {
        strings::encode_jag(value.as_str(), MAX_SUCCESS_BYTES)
    } else {
        strings::encode(value.as_str(), MAX_SUCCESS_BYTES)
    }
    .unwrap();
    output.extend_from_slice(bytes.as_bytes());
}

fn lobby_native_values(value: &LobbySuccess) -> Vec<u8> {
    let mut output = vec![value.rights, value.mod_level, u8::from(value.quick_chat)];
    output.extend_from_slice(&value.date_of_birth.to_be_bytes());
    output.extend_from_slice(&i32::from(value.gender).to_be_bytes());
    output.extend_from_slice(&[
        u8::from(value.verified_email),
        u8::from(value.account_security),
    ]);
    output.extend_from_slice(&value.membership_end.to_be_bytes());
    output.extend_from_slice(&value.membership_time_offset.to_be_bytes());
    output.push(value.membership_flags);
    output.extend_from_slice(&value.jcoins.to_be_bytes());
    output.push(u8::from(value.loyalty_enabled));
    output.extend_from_slice(&value.loyalty_balance.to_be_bytes());
    output.extend_from_slice(&value.recovery_questions_set_date.to_be_bytes());
    output.extend_from_slice(&value.messages.to_be_bytes());
    output.extend_from_slice(&value.last_login_day.to_be_bytes());
    output.extend_from_slice(&value.last_ip_address.to_be_bytes());
    append_string(&mut output, &value.world_login_token, false);
    output.push(value.email_status);
    output.extend_from_slice(&value.credit_card_expiry.to_be_bytes());
    output.extend_from_slice(&value.grace_expiry.to_be_bytes());
    output.push(u8::from(value.dob_requested));
    append_string(&mut output, &value.display_name, true);
    output.push(value.membership_status);
    output.extend_from_slice(&value.play_age.to_be_bytes());
    output.push(u8::from(value.display_name_pending));
    output.extend_from_slice(&value.default_world_id.map_or(-1, i32::from).to_be_bytes());
    append_string(&mut output, &value.default_world_host, true);
    output
}

fn world_native_values(value: &WorldSuccess) -> Vec<u8> {
    let mut output = vec![
        value.rights,
        value.mod_level,
        u8::from(value.quick_chat),
        u8::from(value.verified_email),
        u8::from(value.account_security),
        u8::from(value.quick_chat_only),
    ];
    output.extend_from_slice(&value.player_index.to_be_bytes());
    output.push(u8::from(value.members));
    output.extend_from_slice(&value.date_of_birth.to_be_bytes());
    output.push(u8::from(value.member_world));
    append_string(&mut output, &value.map_owner_identifier, false);
    output
}

fn native_values(event: &LoginResponse) -> Vec<u8> {
    match event {
        LoginResponse::LobbySuccess(value) => lobby_native_values(value),
        LoginResponse::WorldSuccess(value) => world_native_values(value),
        _ => panic!("fake success fixture was not decoded as success"),
    }
}

fn split_response(kind: LoginKind, wire: &[u8], split: usize) -> LoginResponse {
    let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
    let first = decoder.feed(&wire[..split]).unwrap();
    assert_eq!(first.consumed, split);
    let event = if let Some(event) = first.event {
        assert_eq!(split, wire.len());
        event
    } else {
        let second = decoder.feed(&wire[split..]).unwrap();
        assert_eq!(second.consumed, wire.len() - split);
        second.event.unwrap()
    };
    assert_eq!(decoder.finish(), Ok(()));
    expect_error(decoder.feed(&[]), Error::DecoderFinished);
    event
}

type SuccessFixture = (LoginKind, &'static [u8], &'static [u8], &'static [u8]);

fn success_fixtures() -> [SuccessFixture; 5] {
    [
        (
            LoginKind::Lobby,
            include_bytes!("fixtures/lobby-success.wire"),
            include_bytes!("fixtures/lobby-success.payload"),
            include_bytes!("fixtures/lobby-success.native-values"),
        ),
        (
            LoginKind::Lobby,
            include_bytes!("fixtures/lobby-success-sentinel.wire"),
            include_bytes!("fixtures/lobby-success-sentinel.payload"),
            include_bytes!("fixtures/lobby-success-sentinel.native-values"),
        ),
        (
            LoginKind::World,
            include_bytes!("fixtures/world-success.wire"),
            include_bytes!("fixtures/world-success.payload"),
            include_bytes!("fixtures/world-success.native-values"),
        ),
        (
            LoginKind::World,
            include_bytes!("fixtures/world-success-noncanonical.wire"),
            include_bytes!("fixtures/world-success-noncanonical.payload"),
            include_bytes!("fixtures/world-success-noncanonical.native-values"),
        ),
        (
            LoginKind::World,
            include_bytes!("fixtures/world-success-maximum.wire"),
            include_bytes!("fixtures/world-success-maximum.payload"),
            include_bytes!("fixtures/world-success-maximum.native-values"),
        ),
    ]
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn success_payload_fields_match_native_packet_readers() {
    for (kind, _, payload, expected) in success_fixtures() {
        let actual = match kind {
            LoginKind::Lobby => lobby_native_values(&decode_lobby_success(payload).unwrap()),
            LoginKind::World => world_native_values(&decode_world_success(payload).unwrap()),
        };
        assert_eq!(actual, expected);
    }
    let lobby = decode_lobby_success(include_bytes!("fixtures/lobby-success.payload")).unwrap();
    assert_eq!(
        lobby.world_login_token.as_str(),
        "FAKE_LOBBY_ISSUED_TOKEN_727"
    );
    assert_eq!(lobby.default_world_host.as_str(), "offline.invalid");
    assert_eq!(lobby.default_world_id, Some(42));
    assert!(lobby.is_member());
    assert!(lobby.has_member_subscription());
    assert_eq!(lobby.membership_time_offset, 0xfedcba9876);
    assert_eq!(lobby.date_of_birth, -74566);
    assert_eq!(lobby.gender, -2);
    let world = decode_world_success(include_bytes!("fixtures/world-success.payload")).unwrap();
    assert_eq!(world.player_index, 0xfedc);
    assert_eq!(world.date_of_birth, -8344627);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn noncanonical_booleans_and_world_sentinel_follow_native_semantics() {
    let lobby =
        decode_lobby_success(include_bytes!("fixtures/lobby-success-sentinel.payload")).unwrap();
    assert_eq!(lobby.default_world_id, None);
    assert!(!lobby.quick_chat);
    assert!(!lobby.loyalty_enabled);
    let world = decode_world_success(include_bytes!(
        "fixtures/world-success-noncanonical.payload"
    ))
    .unwrap();
    assert!(!world.quick_chat);
    assert!(!world.members);
    assert!(world.account_security);
    assert_eq!(world.map_owner_identifier.as_str(), "");
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_split_of_native_success_frames_decodes_exactly() {
    for (kind, wire, _, expected) in success_fixtures() {
        for split in 0..=wire.len() {
            assert_eq!(native_values(&split_response(kind, wire, split)), expected);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn success_frames_decode_in_every_fixed_chunk_size() {
    for (kind, wire, _, expected) in success_fixtures() {
        for size in 1..=wire.len() {
            let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
            let mut event = None;
            for chunk in wire.chunks(size) {
                let result = decoder.feed(chunk).unwrap();
                assert_eq!(result.consumed, chunk.len());
                if result.event.is_some() {
                    assert!(event.is_none());
                    event = result.event;
                }
            }
            assert_eq!(native_values(&event.unwrap()), expected);
            assert_eq!(decoder.finish(), Ok(()));
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn success_decoder_leaves_first_game_packet_and_map_bytes_unconsumed() {
    for (kind, wire, _, expected) in success_fixtures() {
        let mut bytes = wire.to_vec();
        bytes.extend_from_slice(&[0xee, 0x80, 0x00, 0xff]);
        bytes.extend_from_slice(&vec![0xa5; 4096]);
        let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
        let result = decoder.feed(&bytes).unwrap();
        assert_eq!(result.consumed, wire.len());
        assert_eq!(native_values(&result.event.unwrap()), expected);
        assert_eq!(
            &bytes[result.consumed..result.consumed + 4],
            &[0xee, 0x80, 0x00, 0xff]
        );
        assert_eq!(decoder.finish(), Ok(()));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn eof_at_every_partial_success_frame_is_terminal_failure() {
    for (kind, wire, _, _) in success_fixtures() {
        for end in 0..wire.len() {
            let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
            let result = decoder.feed(&wire[..end]).unwrap();
            assert_eq!(result.consumed, end);
            assert!(result.event.is_none());
            assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
            expect_error(decoder.feed(wire), Error::DecoderFailed);
            assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn success_lengths_limits_and_direct_payload_bounds_are_checked() {
    for (kind, minimum) in [
        (LoginKind::Lobby, MIN_LOBBY_SUCCESS_BYTES),
        (LoginKind::World, MIN_WORLD_SUCCESS_BYTES),
    ] {
        for invalid in [0, minimum - 1, MAX_SUCCESS_BYTES + 1, usize::MAX] {
            expect_error(
                LoginResponseDecoder::new(kind, invalid),
                Error::InvalidLimit,
            );
        }
        for length in 0..minimum {
            let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
            expect_error(decoder.feed(&[2, length as u8]), Error::LengthLimitExceeded);
            expect_error(decoder.feed(&[]), Error::DecoderFailed);
        }
    }
    expect_error(
        decode_lobby_success(&[0; MIN_LOBBY_SUCCESS_BYTES - 1]),
        Error::UnexpectedEof,
    );
    expect_error(
        decode_world_success(&[0; MIN_WORLD_SUCCESS_BYTES - 1]),
        Error::UnexpectedEof,
    );
    expect_error(
        decode_lobby_success(&[0; MAX_SUCCESS_BYTES + 1]),
        Error::LengthLimitExceeded,
    );
    expect_error(
        decode_world_success(&[0; MAX_SUCCESS_BYTES + 1]),
        Error::LengthLimitExceeded,
    );
    assert!(decode_lobby_success(&[0; MIN_LOBBY_SUCCESS_BYTES]).is_ok());
    assert!(decode_world_success(&[0; MIN_WORLD_SUCCESS_BYTES]).is_ok());
    let maximum = include_bytes!("fixtures/world-success-maximum.payload");
    assert_eq!(maximum.len(), MAX_SUCCESS_BYTES);
    let world = decode_world_success(maximum).unwrap();
    assert_eq!(world.map_owner_identifier.as_str().len(), 241);
    let mut decoder = LoginResponseDecoder::new(LoginKind::World, MAX_SUCCESS_BYTES - 1).unwrap();
    expect_error(
        decoder.feed(include_bytes!("fixtures/world-success-maximum.wire")),
        Error::LengthLimitExceeded,
    );
    let mut lobby_maximum = vec![0; MIN_LOBBY_SUCCESS_BYTES];
    lobby_maximum.splice(
        42..42,
        vec![b'T'; MAX_SUCCESS_BYTES - MIN_LOBBY_SUCCESS_BYTES],
    );
    let lobby = decode_lobby_success(&lobby_maximum).unwrap();
    assert_eq!(
        lobby.world_login_token.as_str().len(),
        MAX_SUCCESS_BYTES - MIN_LOBBY_SUCCESS_BYTES
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_success_strings_and_trailing_payload_are_rejected() {
    let original = include_bytes!("fixtures/lobby-success.payload");
    let mut invalid = original.to_vec();
    invalid[42] = 0x81;
    expect_error(decode_lobby_success(&invalid), Error::InvalidCp1252Byte);
    invalid = original.to_vec();
    let display_prefix = 49 + "FAKE_LOBBY_ISSUED_TOKEN_727".len();
    invalid[display_prefix] = 1;
    expect_error(decode_lobby_success(&invalid), Error::InvalidJagPrefix);
    let world = include_bytes!("fixtures/world-success.payload");
    let mut invalid_world = world.to_vec();
    invalid_world[13] = 0x81;
    expect_error(
        decode_world_success(&invalid_world),
        Error::InvalidCp1252Byte,
    );
    for (kind, wire, payload, _) in success_fixtures() {
        let truncated = &payload[..payload.len() - 1];
        assert!(match kind {
            LoginKind::Lobby => decode_lobby_success(truncated).is_err(),
            LoginKind::World => decode_world_success(truncated).is_err(),
        });
        if payload.len() < MAX_SUCCESS_BYTES {
            let mut extra = payload.to_vec();
            extra.push(0);
            match kind {
                LoginKind::Lobby => expect_error(decode_lobby_success(&extra), Error::TrailingData),
                LoginKind::World => expect_error(decode_world_success(&extra), Error::TrailingData),
            }
            let mut framed = wire.to_vec();
            framed[1] += 1;
            framed.push(0);
            let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
            expect_error(decoder.feed(&framed), Error::TrailingData);
            expect_error(decoder.feed(wire), Error::DecoderFailed);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn evidenced_status_details_and_terminal_events_are_incremental() {
    for kind in [LoginKind::Lobby, LoginKind::World] {
        for bytes in [
            vec![1],
            vec![15],
            vec![23],
            vec![3],
            vec![35],
            vec![21, 255],
            vec![29, 7],
            vec![45, 9, 0xab, 0xcd],
        ] {
            let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
            for prefix in &bytes[..bytes.len() - 1] {
                let result = decoder.feed(&[*prefix]).unwrap();
                assert_eq!(result.consumed, 1);
                assert!(result.event.is_none());
            }
            let mut suffix = vec![bytes[bytes.len() - 1]];
            suffix.extend_from_slice(&[0xde, 0xad, 0xbe, 0xef]);
            let result = decoder.feed(&suffix).unwrap();
            assert_eq!(result.consumed, 1);
            match result.event.unwrap() {
                LoginResponse::ConfirmationRequired => assert_eq!(bytes, [1]),
                LoginResponse::ReconnectRequired => assert_eq!(bytes, [15]),
                LoginResponse::RetryRequested => assert_eq!(bytes, [23]),
                LoginResponse::Rejected { response_code } => assert_eq!(response_code, bytes[0]),
                LoginResponse::Queue {
                    position,
                    delay_ticks,
                } => {
                    assert_eq!(position, 255);
                    assert_eq!(delay_ticks, 12750);
                }
                LoginResponse::RejectedDetail {
                    response_code,
                    detail,
                    extra_info,
                } => {
                    assert_eq!(response_code, bytes[0]);
                    assert_eq!(detail, bytes[1]);
                    assert_eq!(extra_info, if bytes[0] == 45 { Some(0xabcd) } else { None });
                }
                _ => panic!("unexpected fake status event"),
            }
            assert_eq!(decoder.finish(), Ok(()));
            expect_error(decoder.feed(&suffix[1..]), Error::DecoderFinished);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn time_sync_events_resume_reading_response_codes_without_consuming_following_success() {
    for (kind, wire, _, expected) in success_fixtures() {
        let mut bytes = vec![42, 0xab, 0xcd, 42, 0x12, 0x34];
        bytes.extend_from_slice(wire);
        let mut decoder = LoginResponseDecoder::new(kind, MAX_SUCCESS_BYTES).unwrap();
        for (offset, expected_code) in [(0, 0xabcd), (3, 0x1234)] {
            let result = decoder.feed(&bytes[offset..]).unwrap();
            assert_eq!(result.consumed, 3);
            match result.event.unwrap() {
                LoginResponse::TimeSync { code } => assert_eq!(code, expected_code),
                _ => panic!("missing fake time-sync event"),
            }
        }
        let result = decoder.feed(&bytes[6..]).unwrap();
        assert_eq!(result.consumed, wire.len());
        assert_eq!(native_values(&result.event.unwrap()), expected);
        assert_eq!(decoder.finish(), Ok(()));
    }
    for prefix in [
        vec![42],
        vec![42, 1],
        vec![21],
        vec![29],
        vec![45],
        vec![45, 1],
        vec![45, 1, 2],
    ] {
        let mut decoder = LoginResponseDecoder::new(LoginKind::World, MAX_SUCCESS_BYTES).unwrap();
        assert!(decoder.feed(&prefix).unwrap().event.is_none());
        assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
    }
}
