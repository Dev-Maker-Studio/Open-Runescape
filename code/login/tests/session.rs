use openrs_login::response::LoginResponse;
use openrs_login::session::{LoginAttempt, Phase, SessionError};
use openrs_login::{
    Error, LobbyIdentity, LobbyRequest, MachineInformation, WorldRequest, encode_lobby_request,
    encode_world_request, isaac_ciphers,
};
use openrs_protocol::registry::{ClientPacket, RegistryDecoder, RegistryEncoder, ServerPacket};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const RANDOM: [u8; 24] = [7; 24];
const PREFERENCES: [u8; 35] = [11; 35];
const CRCS: [u32; 36] = [0x80000000; 36];
const FEATURES: [u32; 3] = [0; 3];

fn lobby_request() -> LobbyRequest<'static> {
    LobbyRequest {
        identity: LobbyIdentity::Username("OFFLINE_ONLY_727"),
        password: "OFFLINE_ONLY_PASSWORD",
        seed: &SEED,
        social_nonce: 9,
        client_random_seed: 17,
        game_id: 0,
        language: 0,
        random24: &RANDOM,
        affiliate_p: "",
        preferences: &PREFERENCES,
        login_token: "",
        affiliate: 0,
        session_token: 0,
        js5_auth_token: "OFFLINE_JS5",
        index_crcs: &CRCS,
    }
}

fn machine() -> MachineInformation<'static> {
    MachineInformation {
        operating_system: 3,
        x64_os: true,
        os_vendor: 0,
        java_vendor: 4,
        java_build: 17,
        java_sub_build: 0,
        java_update: 0,
        unknown_flag: false,
        max_memory: 4096,
        processors: 8,
        ram: 0,
        cpu_clock: 0,
        gpu_model: "OFFLINE_GPU",
        gpu_driver_version: "OFFLINE_DRIVER",
        directx_version: "",
        directx_letter: "",
        dx_driver_month: 0,
        dx_driver_year: 0,
        cpu_type: "OFFLINE_CPU",
        cpu_data: "",
        cpu_cores: 8,
        cpu_family_id: 0,
        cpu_feature_flags: &FEATURES,
        cpu_model_info: 0,
    }
}

fn world_request<'a>(machine: &'a MachineInformation<'a>) -> WorldRequest<'a> {
    WorldRequest {
        identity: LobbyIdentity::Username("OFFLINE_ONLY_727"),
        lobby_auth_token: "FAKE_LOBBY_ISSUED_TOKEN_727",
        seed: &SEED,
        social_nonce: 9,
        client_random_seed: 17,
        reconnecting: false,
        display_mode: 2,
        screen_width: 765,
        screen_height: 503,
        anti_aliasing: 0,
        random24: &RANDOM,
        affiliate_p: "",
        affiliate: 0,
        preferences: &PREFERENCES,
        machine_info: machine,
        var_verify_id: 0,
        machine_uid: 0,
        login_token: "",
        supplementary_login_data: None,
        jagtheora_loaded: false,
        is_applet: false,
        has_signed_terms_agreement: false,
        login_type: 0,
        session_token: 0,
        js5_auth_token: "OFFLINE_JS5",
        different_world: false,
        world_id: 42,
        index_crcs: &CRCS,
    }
}

fn response_stage(attempt: &mut LoginAttempt) {
    assert_eq!(attempt.phase(), Phase::SendHandshake);
    assert_eq!(attempt.take_outgoing().unwrap().unwrap().as_bytes(), &[14]);
    assert_eq!(attempt.phase(), Phase::AwaitHandshake);
    assert!(attempt.take_outgoing().unwrap().is_none());
    assert_eq!(attempt.feed(&[]).unwrap().consumed, 0);
    assert_eq!(attempt.feed(&[0]).unwrap().consumed, 1);
    assert_eq!(attempt.phase(), Phase::SendRequest);
    assert!(attempt.take_outgoing().unwrap().is_some());
    assert_eq!(attempt.phase(), Phase::AwaitResponse);
}

fn expect_error<T>(result: Result<T, SessionError>, expected: SessionError) {
    match result {
        Err(error) => assert_eq!(error, expected),
        Ok(_) => panic!("invalid attempt operation succeeded"),
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn requests_are_gated_by_the_single_raw_handshake_byte() {
    let lobby = lobby_request();
    let machine = machine();
    let world = world_request(&machine);
    for (mut attempt, expected) in [
        (
            LoginAttempt::lobby(&lobby).unwrap(),
            encode_lobby_request(&lobby).unwrap().wire_bytes().to_vec(),
        ),
        (
            LoginAttempt::world(&world).unwrap(),
            encode_world_request(&world).unwrap().wire_bytes().to_vec(),
        ),
    ] {
        assert_eq!(attempt.take_outgoing().unwrap().unwrap().as_bytes(), &[14]);
        let response = attempt.feed(&[0, 2, 14, 99]).unwrap();
        assert_eq!(response.consumed, 1);
        assert!(response.event.is_none());
        assert_eq!(attempt.phase(), Phase::SendRequest);
        assert_eq!(
            attempt.take_outgoing().unwrap().unwrap().as_bytes(),
            expected
        );
        assert!(attempt.take_outgoing().unwrap().is_none());
        assert_eq!(attempt.phase(), Phase::AwaitResponse);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn rejected_handshakes_fail_without_releasing_the_request_or_ciphers() {
    for code in 1..=255 {
        let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
        attempt.take_outgoing().unwrap();
        expect_error(
            attempt.feed(&[code]),
            SessionError::Codec(Error::UnexpectedHandshakeResponse),
        );
        assert_eq!(attempt.phase(), Phase::Failed);
        expect_error(attempt.take_outgoing(), SessionError::InvalidPhase);
        expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
        assert!(
            !SessionError::Codec(Error::UnexpectedHandshakeResponse)
                .to_string()
                .contains("OFFLINE")
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_lobby_success_stops_before_coalesced_isaac_packets() {
    let success = include_bytes!("fixtures/lobby-success.wire");
    let (_, incoming) = isaac_ciphers(&SEED).unwrap();
    let mut server = RegistryEncoder::<ServerPacket>::new(Some(incoming), 65535).unwrap();
    let world_list = ServerPacket::from_wire(103).unwrap();
    let pong = ServerPacket::from_wire(29).unwrap();
    let mut wire = success.to_vec();
    wire.extend(server.encode(world_list, &[1, 2, 0, 0]).unwrap());
    wire.extend(server.encode(pong, &[]).unwrap());
    for split in 0..=success.len() {
        let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
        response_stage(&mut attempt);
        let first = attempt.feed(&wire[..split]).unwrap();
        assert_eq!(first.consumed, split);
        let event = if first.event.is_some() {
            first.event
        } else {
            let second = attempt.feed(&wire[split..]).unwrap();
            assert_eq!(second.consumed, success.len() - split);
            second.event
        };
        let Some(LoginResponse::LobbySuccess(lobby)) = event else {
            panic!("expected lobby success")
        };
        assert_eq!(
            lobby.world_login_token.as_str(),
            "FAKE_LOBBY_ISSUED_TOKEN_727"
        );
        assert_eq!(attempt.phase(), Phase::Accepted);
        let mut ciphers = attempt.take_ciphers().unwrap();
        assert_eq!(attempt.phase(), Phase::Closed);
        let (mut outgoing, _) = isaac_ciphers(&SEED).unwrap();
        assert_eq!(ciphers.outgoing.next_u32(), outgoing.next_u32());
        let mut stream =
            RegistryDecoder::<ServerPacket>::new(Some(ciphers.incoming), 65535).unwrap();
        let first = stream.feed(&wire[success.len()..]).unwrap();
        let packet = first.item.unwrap();
        assert_eq!(packet.opcode, world_list);
        assert_eq!(packet.payload, [1, 2, 0, 0]);
        let second = stream
            .feed(&wire[success.len() + first.consumed..])
            .unwrap();
        assert_eq!(second.item.unwrap().opcode, pong);
        assert_eq!(success.len() + first.consumed + second.consumed, wire.len());
        stream.finish().unwrap();
        expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_world_success_leaves_first_game_header_and_ciphers_untouched() {
    let machine = machine();
    let success = include_bytes!("fixtures/world-success.wire");
    let mut wire = success.to_vec();
    wire.extend_from_slice(&[0x80, 0x55, 0x12, 0x34, 9]);
    for split in 0..=success.len() {
        let mut attempt = LoginAttempt::world(&world_request(&machine)).unwrap();
        response_stage(&mut attempt);
        let first = attempt.feed(&wire[..split]).unwrap();
        let event = if first.event.is_some() {
            first.event
        } else {
            let second = attempt.feed(&wire[split..]).unwrap();
            assert_eq!(second.consumed, success.len() - split);
            second.event
        };
        assert!(matches!(event, Some(LoginResponse::WorldSuccess(_))));
        let mut actual = attempt.take_ciphers().unwrap();
        let (mut outgoing, mut incoming) = isaac_ciphers(&SEED).unwrap();
        for _ in 0..520 {
            assert_eq!(actual.outgoing.next_u32(), outgoing.next_u32());
            assert_eq!(actual.incoming.next_u32(), incoming.next_u32());
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn confirmation_requires_explicit_continue_and_advances_only_outgoing_cipher() {
    let machine = machine();
    let mut attempt = LoginAttempt::world(&world_request(&machine)).unwrap();
    response_stage(&mut attempt);
    let (mut outgoing, mut incoming) = isaac_ciphers(&SEED).unwrap();
    for _ in 0..3 {
        let event = attempt.feed(&[1, 99]).unwrap();
        assert_eq!(event.consumed, 1);
        assert!(matches!(
            event.event,
            Some(LoginResponse::ConfirmationRequired)
        ));
        assert_eq!(attempt.phase(), Phase::AwaitConfirmation);
        assert!(attempt.take_outgoing().unwrap().is_none());
        attempt.confirm().unwrap();
        assert_eq!(attempt.phase(), Phase::SendConfirmation);
        expect_error(attempt.confirm(), SessionError::InvalidPhase);
        assert_eq!(
            attempt.take_outgoing().unwrap().unwrap().as_bytes(),
            &[26_u8.wrapping_add(outgoing.next_u32() as u8)]
        );
        assert_eq!(attempt.phase(), Phase::AwaitResponse);
    }
    assert!(matches!(
        attempt
            .feed(include_bytes!("fixtures/world-success.wire"))
            .unwrap()
            .event,
        Some(LoginResponse::WorldSuccess(_))
    ));
    let mut ciphers = attempt.take_ciphers().unwrap();
    for _ in 0..520 {
        assert_eq!(ciphers.outgoing.next_u32(), outgoing.next_u32());
        assert_eq!(ciphers.incoming.next_u32(), incoming.next_u32());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn time_sync_is_fragmentable_and_does_not_advance_isaac() {
    let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
    response_stage(&mut attempt);
    for byte in [42, 0xab] {
        let event = attempt.feed(&[byte]).unwrap();
        assert_eq!(event.consumed, 1);
        assert!(event.event.is_none());
    }
    let event = attempt.feed(&[0xcd, 2]).unwrap();
    assert_eq!(event.consumed, 1);
    assert!(matches!(
        event.event,
        Some(LoginResponse::TimeSync { code: 0xabcd })
    ));
    assert_eq!(attempt.phase(), Phase::AwaitResponse);
    attempt
        .feed(include_bytes!("fixtures/lobby-success.wire"))
        .unwrap();
    let mut actual = attempt.take_ciphers().unwrap();
    let (mut outgoing, mut incoming) = isaac_ciphers(&SEED).unwrap();
    assert_eq!(actual.outgoing.next_u32(), outgoing.next_u32());
    assert_eq!(actual.incoming.next_u32(), incoming.next_u32());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn response_events_have_a_total_bound_across_confirmation_cycles() {
    let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
    response_stage(&mut attempt);
    for _ in 0..64 {
        assert!(matches!(
            attempt.feed(&[1]).unwrap().event,
            Some(LoginResponse::ConfirmationRequired)
        ));
        attempt.confirm().unwrap();
        attempt.take_outgoing().unwrap();
    }
    expect_error(attempt.feed(&[42, 0, 1]), SessionError::ResponseEventLimit);
    assert_eq!(attempt.phase(), Phase::Failed);
    expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
    expect_error(attempt.take_outgoing(), SessionError::InvalidPhase);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn retry_queue_and_reconnect_are_explicit_terminal_outcomes_not_automatic_logins() {
    for (wire, expected) in [
        (&[23, 99][..], Phase::Retry),
        (&[21, 7, 99][..], Phase::Queued),
        (&[15, 0, 4, 99][..], Phase::Reconnect),
        (&[3, 99][..], Phase::Rejected),
        (&[5, 99][..], Phase::Rejected),
        (&[6, 99][..], Phase::Rejected),
        (&[9, 99][..], Phase::Rejected),
        (&[13, 99][..], Phase::Rejected),
        (&[14, 99][..], Phase::Rejected),
        (&[29, 4, 99][..], Phase::Rejected),
        (&[45, 1, 0, 2, 99][..], Phase::Rejected),
    ] {
        let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
        response_stage(&mut attempt);
        let feed = attempt.feed(wire).unwrap();
        let expected_consumed = match wire[0] {
            21 | 29 => 2,
            45 => 4,
            _ => 1,
        };
        assert_eq!(feed.consumed, expected_consumed);
        if let Some(LoginResponse::Queue {
            position,
            delay_ticks,
        }) = feed.event
        {
            assert_eq!((position, delay_ticks), (7, 350));
        }
        assert_eq!(attempt.phase(), expected);
        assert!(attempt.take_outgoing().unwrap().is_none());
        expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
        attempt.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_partial_success_is_terminal_on_eof() {
    let success = include_bytes!("fixtures/lobby-success.wire");
    for length in 0..success.len() {
        let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
        response_stage(&mut attempt);
        let event = attempt.feed(&success[..length]).unwrap();
        assert!(event.event.is_none());
        expect_error(attempt.finish(), SessionError::Codec(Error::UnexpectedEof));
        assert_eq!(attempt.phase(), Phase::Failed);
        expect_error(attempt.feed(success), SessionError::InvalidPhase);
        expect_error(attempt.take_outgoing(), SessionError::InvalidPhase);
        expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn abort_and_misuse_never_release_pending_login_material() {
    let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
    expect_error(attempt.confirm(), SessionError::InvalidPhase);
    expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
    assert_eq!(attempt.phase(), Phase::SendHandshake);
    expect_error(attempt.feed(&[0]), SessionError::InvalidPhase);
    assert_eq!(attempt.phase(), Phase::Failed);
    attempt.abort();
    assert_eq!(attempt.phase(), Phase::Closed);
    attempt.finish().unwrap();
    expect_error(attempt.take_outgoing(), SessionError::InvalidPhase);
    for prefix in [0, 1, 2, 24] {
        let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
        response_stage(&mut attempt);
        attempt
            .feed(&include_bytes!("fixtures/lobby-success.wire")[..prefix])
            .unwrap();
        attempt.abort();
        attempt.abort();
        expect_error(attempt.take_outgoing(), SessionError::InvalidPhase);
        expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_response_poisoning_is_terminal() {
    let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
    response_stage(&mut attempt);
    expect_error(
        attempt.feed(&[2, 0]),
        SessionError::Codec(Error::LengthLimitExceeded),
    );
    assert_eq!(attempt.phase(), Phase::Failed);
    expect_error(attempt.feed(&[3]), SessionError::InvalidPhase);
    expect_error(attempt.take_ciphers(), SessionError::InvalidPhase);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn outbound_ping_after_lobby_handoff_uses_the_original_seed() {
    let mut attempt = LoginAttempt::lobby(&lobby_request()).unwrap();
    response_stage(&mut attempt);
    attempt
        .feed(include_bytes!("fixtures/lobby-success.wire"))
        .unwrap();
    let ciphers = attempt.take_ciphers().unwrap();
    let mut encoder = RegistryEncoder::<ClientPacket>::new(Some(ciphers.outgoing), 65535).unwrap();
    let (mut expected, _) = isaac_ciphers(&SEED).unwrap();
    let ping = ClientPacket::from_wire(0).unwrap();
    assert_eq!(
        encoder.encode(ping, &[]).unwrap(),
        [expected.next_u32() as u8]
    );
}
