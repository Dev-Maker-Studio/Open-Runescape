#![cfg(not(target_arch = "wasm32"))]

use super::*;
use openrs_login::isaac_ciphers;
use openrs_protocol::registry::{ClientPacket, RegistryDecoder, RegistryEncoder};
use serde_json::{Value, json};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const SUCCESS: &[u8] = include_bytes!("../../../login/tests/fixtures/lobby-success.wire");
const BASIC: &[u8] = include_bytes!("../../../login/tests/fixtures/world-list-basic.payload");
const COUNTS: &[u8] = include_bytes!("../../../login/tests/fixtures/world-list-counts.payload");

pub(crate) fn config() -> Value {
    json!({
        "identity": { "kind": "username", "username": "offline_Œ_727" },
        "password": "OFFLINE_ONLY_€",
        "seed": SEED,
        "socialNonce": [0xfedcba98u32, 0x76543210u32],
        "clientRandomSeed": [0x01234567u32, 0x89abcdefu32],
        "gameId": 0,
        "language": 2,
        "random24": (0u8..24).map(|i| i.wrapping_mul(7).wrapping_add(3)).collect::<Vec<_>>(),
        "affiliateP": "FAKE_AFFILIATE_™",
        "preferences": (0u8..35).map(|i| i.wrapping_mul(11).wrapping_add(27)).collect::<Vec<_>>(),
        "loginToken": "FAKE_LOGIN_€",
        "affiliate": 0x89abcdefu32 as i32,
        "sessionToken": 0x76543210,
        "js5AuthToken": "FAKE_JS5_ÿ",
        "indexCrcs": (0u32..36).map(|i| i.wrapping_mul(0x1234567).wrapping_add(0x80000000)).collect::<Vec<_>>(),
    })
}

fn session() -> LobbySession {
    LobbySession::from_json(&config().to_string()).unwrap()
}

fn awaiting() -> LobbySession {
    let mut session = session();
    assert!(session.take_outgoing().unwrap().as_slice() == [14]);
    assert_eq!(session.feed(&[0]).unwrap().consumed, 1);
    assert!(!session.take_outgoing().unwrap().is_empty());
    assert_eq!(session.phase(), LobbyPhase::AwaitResponse);
    session
}

pub(crate) fn accepted() -> LobbySession {
    accepted_with_default_world(Some(42))
}

pub(crate) fn accepted_with_default_world(default_world_id: Option<u16>) -> LobbySession {
    let mut session = awaiting();
    let mut success = SUCCESS.to_vec();
    let world_at = success.len() - b"\0offline.invalid\0".len() - 2;
    assert_eq!(&success[world_at..world_at + 2], &42u16.to_be_bytes());
    success[world_at..world_at + 2]
        .copy_from_slice(&default_world_id.unwrap_or(u16::MAX).to_be_bytes());
    assert_eq!(
        session.feed(&success).unwrap().event,
        Some(LobbyEvent::LoginAccepted)
    );
    assert!(session.world_token.is_some());
    assert_eq!(session.default_world_id, default_world_id);
    session
}

fn server() -> RegistryEncoder<ServerPacket> {
    RegistryEncoder::new(Some(isaac_ciphers(&SEED).unwrap().1), 65535).unwrap()
}

fn clean(session: &mut LobbySession, expected: LobbyPhase) {
    assert_eq!(session.phase(), expected);
    assert!(session.attempt.is_none());
    assert!(session.stream.is_none());
    assert!(session.world_token.is_none());
    assert!(session.default_world_id.is_none());
    assert!(session.pending.is_empty());
    assert!(!session.report().accepted);
    assert!(session.report().world_list.is_none());
}

#[test]
fn owned_configuration_matches_native_username_and_token_requests_without_defaults() {
    for (identity, fixture) in [
        (
            json!({"kind":"username", "username":"offline_Œ_727"}),
            include_bytes!("../../../login/tests/fixtures/lobby-username.wire").as_slice(),
        ),
        (
            json!({"kind":"token", "token":[0x88776655u32,0x44332211u32]}),
            include_bytes!("../../../login/tests/fixtures/lobby-token.wire").as_slice(),
        ),
    ] {
        let mut config = config();
        config["identity"] = identity;
        let mut session =
            LobbySession::new(LobbyConfig::from_json(&config.to_string()).unwrap()).unwrap();
        assert_eq!(session.phase(), LobbyPhase::SendHandshake);
        assert!(session.take_outgoing().unwrap().as_slice() == [14]);
        assert!(session.take_outgoing().unwrap().is_empty());
        let feed = session.feed(&[0, 2, 99]).unwrap();
        assert_eq!(
            feed,
            LobbyFeed {
                consumed: 1,
                event: None
            }
        );
        assert_eq!(session.phase(), LobbyPhase::SendRequest);
        assert!(session.take_outgoing().unwrap().as_slice() == fixture);
        assert!(session.take_outgoing().unwrap().is_empty());
        session.close();
        clean(&mut session, LobbyPhase::Closed);
    }
}

#[test]
fn strict_config_rejects_missing_unknown_duplicate_unsigned_and_wrong_length_fields() {
    for field in config().as_object().unwrap().keys() {
        let mut invalid = config();
        invalid.as_object_mut().unwrap().remove(field);
        assert!(LobbySession::from_json(&invalid.to_string()).is_err());
    }
    for (field, count, invalid_values) in [
        (
            "seed",
            4,
            vec![
                json!(-1),
                json!(4294967296u64),
                json!(1.5),
                json!("1"),
                Value::Null,
                json!(true),
            ],
        ),
        (
            "socialNonce",
            2,
            vec![json!(-1), json!(4294967296u64), json!(0.1)],
        ),
        ("clientRandomSeed", 2, vec![json!(-1), json!(4294967296u64)]),
        ("random24", 24, vec![json!(-1), json!(256), json!(1.5)]),
        ("preferences", 35, vec![json!(-1), json!(256), json!("1")]),
        (
            "indexCrcs",
            36,
            vec![json!(-1), json!(4294967296u64), json!(1.5)],
        ),
    ] {
        for length in [count - 1, count + 1] {
            let mut invalid = config();
            invalid[field] = json!(vec![0; length]);
            assert!(LobbySession::from_json(&invalid.to_string()).is_err());
        }
        for value in invalid_values {
            let mut invalid = config();
            invalid[field][0] = value;
            assert!(LobbySession::from_json(&invalid.to_string()).is_err());
        }
    }
    for invalid_identity in [
        json!({"kind":"username","username":"x","token":[0,1]}),
        json!({"kind":"token","token":[0,1],"username":"x"}),
        json!({"kind":"token","token":[-1,1]}),
        json!({"kind":"token","token":[4294967295u32,4294967295u32]}),
    ] {
        let mut invalid = config();
        invalid["identity"] = invalid_identity;
        assert!(LobbySession::from_json(&invalid.to_string()).is_err());
    }
    let mut invalid = config();
    invalid["unexpected"] = json!("never returned");
    assert!(LobbySession::from_json(&invalid.to_string()).is_err());
    let duplicated = config()
        .to_string()
        .replacen('{', "{\"password\":\"never returned\",", 1);
    assert!(LobbySession::from_json(&duplicated).is_err());
    let float_seed = config()
        .to_string()
        .replace("\"seed\":[1,", "\"seed\":[1.0,");
    assert!(LobbySession::from_json(&float_seed).is_err());
    assert!(LobbySession::from_json(&" ".repeat(MAX_CONFIG_BYTES + 1)).is_err());
    for password in ["never returned\u{0}", "never returned😀"] {
        let mut invalid = config();
        invalid["password"] = json!(password);
        let error = LobbySession::from_json(&invalid.to_string()).err().unwrap();
        assert_eq!(error, "invalid lobby request configuration");
        assert!(!error.contains("never returned"));
    }
}

#[test]
fn exact_event_boundaries_survive_fragmentation_confirmation_and_cipher_handoff() {
    for confirmation in [false, true] {
        let mut encoder = server();
        let chunks: &[&[u8]] = &[
            include_bytes!("../../../login/tests/fixtures/world-list-split3-0.payload"),
            include_bytes!("../../../login/tests/fixtures/world-list-split3-1.payload"),
            include_bytes!("../../../login/tests/fixtures/world-list-split3-2.payload"),
        ];
        let mut wire = vec![42, 0x12, 0x34];
        wire.extend_from_slice(SUCCESS);
        for payload in chunks {
            wire.extend(encoder.encode(ServerPacket::WORLD_LIST, payload).unwrap());
            wire.extend(encoder.encode(ServerPacket::PING, &[]).unwrap());
        }
        wire.extend(encoder.encode(ServerPacket::WORLD_LIST, COUNTS).unwrap());
        wire.extend(
            encoder
                .encode(ServerPacket::MIDI_SONG_LOCATION, &[0xfe, 0xa5, 0, 1, 2, 3])
                .unwrap(),
        );
        for size in [1, 2, 3, 7, 31, 63, 127, wire.len()] {
            let mut session = awaiting();
            let (mut outgoing, _) = isaac_ciphers(&SEED).unwrap();
            if confirmation {
                assert_eq!(
                    session.feed(&[1, 99]).unwrap(),
                    LobbyFeed {
                        consumed: 1,
                        event: Some(LobbyEvent::ConfirmationRequired)
                    }
                );
                session.confirm().unwrap();
                assert_eq!(session.phase(), LobbyPhase::SendConfirmation);
                assert!(
                    session.take_outgoing().unwrap().as_slice()
                        == [26u8.wrapping_add(outgoing.next_u32() as u8)]
                );
            }
            let mut events = Vec::new();
            for chunk in wire.chunks(size) {
                let mut rest = chunk;
                while !rest.is_empty() {
                    let feed = session.feed(rest).unwrap();
                    assert!((1..=rest.len()).contains(&feed.consumed));
                    rest = &rest[feed.consumed..];
                    if let Some(event) = feed.event {
                        events.push(event);
                    }
                }
            }
            assert_eq!(
                events
                    .iter()
                    .filter(|event| matches!(event, LobbyEvent::LoginAccepted))
                    .count(),
                1
            );
            assert_eq!(
                events
                    .iter()
                    .filter(|event| matches!(event, LobbyEvent::TimeSync))
                    .count(),
                1
            );
            assert_eq!(
                events
                    .iter()
                    .filter(|event| matches!(event, LobbyEvent::WorldListUpdated))
                    .count(),
                2
            );
            assert_eq!(
                events
                    .iter()
                    .filter(|event| matches!(event, LobbyEvent::WorldListPending))
                    .count(),
                2
            );
            assert_eq!(
                events
                    .iter()
                    .filter(|event| matches!(event, LobbyEvent::OpaquePacket { .. }))
                    .count(),
                4
            );
            let mut expected = WorldListDecoder::new();
            expected.feed(BASIC).unwrap();
            expected.feed(COUNTS).unwrap();
            assert_eq!(
                session.report().world_list,
                expected.snapshot().map(LobbyWorldListReport::from)
            );
            assert!(
                session
                    .world_token
                    .as_ref()
                    .is_some_and(|token| token.as_str() == "FAKE_LOBBY_ISSUED_TOKEN_727")
            );
            assert_eq!(session.phase(), LobbyPhase::Lobby);
            session.request_world_list(i32::MIN).unwrap();
            let request = session.take_outgoing().unwrap();
            let mut decoder = RegistryDecoder::<ClientPacket>::new(Some(outgoing), 65535).unwrap();
            let packet = decoder.feed(&request).unwrap().item.unwrap();
            assert_eq!(packet.opcode, ClientPacket::REQUEST_WORLD_LIST);
            assert_eq!(packet.payload, i32::MIN.to_be_bytes());
            session.mark_flushed().unwrap();
            for _ in 0..50 {
                session.tick().unwrap();
                assert!(session.take_outgoing().unwrap().is_empty());
            }
            session.tick().unwrap();
            assert_eq!(
                decoder
                    .feed(&session.take_outgoing().unwrap())
                    .unwrap()
                    .item
                    .unwrap()
                    .opcode,
                ClientPacket::PING
            );
            session.mark_flushed().unwrap();
            session.finish().unwrap();
            session.finish().unwrap();
            clean(&mut session, LobbyPhase::Closed);
        }
    }
}

#[test]
fn login_success_never_consumes_coalesced_lobby_packets() {
    let packet = server().encode(ServerPacket::WORLD_LIST, BASIC).unwrap();
    for cut in 0..=SUCCESS.len() {
        let mut session = awaiting();
        let first = session.feed(&SUCCESS[..cut]).unwrap();
        assert_eq!(first.consumed, cut);
        let mut tail = SUCCESS[cut..].to_vec();
        tail.extend_from_slice(&packet);
        if cut < SUCCESS.len() {
            let feed = session.feed(&tail).unwrap();
            assert_eq!(feed.consumed, SUCCESS.len() - cut);
            assert_eq!(feed.event, Some(LobbyEvent::LoginAccepted));
            tail.drain(..feed.consumed);
        }
        let feed = session.feed(&tail).unwrap();
        assert_eq!(feed.consumed, packet.len());
        assert_eq!(feed.event, Some(LobbyEvent::WorldListUpdated));
    }
}

#[test]
fn reports_and_opaque_events_have_only_allowlisted_nonsecret_fields() {
    let mut session = accepted();
    let report = serde_json::to_value(session.report()).unwrap();
    assert_eq!(
        report,
        json!({"phase":"lobby","accepted":true,"worldList":null})
    );
    let mut encoder = server();
    let opaque = encoder
        .encode(
            ServerPacket::PROCESS_DEV_CONSOLE_COMMAND,
            b"opaque-secret-not-applied",
        )
        .unwrap();
    let feed = session.feed(&opaque).unwrap();
    assert_eq!(
        serde_json::to_value(feed).unwrap()["event"],
        json!({
            "kind":"opaquePacket", "opcode":2, "name":"PROCESS_DEV_CONSOLE_COMMAND", "payloadBytes":25,
        })
    );
    assert!(session.report().world_list.is_none());
    session
        .feed(&encoder.encode(ServerPacket::WORLD_LIST, BASIC).unwrap())
        .unwrap();
    let serialized = serde_json::to_string(&session.report()).unwrap();
    for forbidden in [
        "password",
        "seed",
        "token",
        "OFFLINE_ONLY",
        "FAKE_LOBBY_ISSUED",
        "offline_Œ_727",
        "opaque-secret",
        "FAKE_LOGIN",
        "FAKE_JS5",
    ] {
        assert!(
            !serialized
                .to_lowercase()
                .contains(&forbidden.to_lowercase())
        );
    }
    assert!(session.report().world_list.is_some());
}

#[test]
fn rejected_queued_retry_and_reconnect_release_all_session_secrets() {
    for (wire, phase, event) in [
        (
            &[0][..],
            LobbyPhase::Rejected,
            LobbyEvent::Rejected { response_code: 0 },
        ),
        (
            &[3][..],
            LobbyPhase::Rejected,
            LobbyEvent::Rejected { response_code: 3 },
        ),
        (
            &[29, 9][..],
            LobbyPhase::Rejected,
            LobbyEvent::Rejected { response_code: 29 },
        ),
        (
            &[45, 1, 2, 3][..],
            LobbyPhase::Rejected,
            LobbyEvent::Rejected { response_code: 45 },
        ),
        (
            &[255][..],
            LobbyPhase::Rejected,
            LobbyEvent::Rejected { response_code: 255 },
        ),
        (&[21, 9][..], LobbyPhase::Queued, LobbyEvent::Queued),
        (&[23][..], LobbyPhase::Retry, LobbyEvent::RetryRequested),
        (
            &[15][..],
            LobbyPhase::Reconnect,
            LobbyEvent::ReconnectRequired,
        ),
    ] {
        let mut session = awaiting();
        let mut input = wire.to_vec();
        input.push(99);
        let feed = session.feed(&input).unwrap();
        assert_eq!(feed.consumed, wire.len());
        if let LobbyEvent::Rejected { response_code } = &event {
            assert_eq!(
                serde_json::to_value(&feed.event).unwrap(),
                json!({
                    "kind": "rejected", "responseCode": response_code,
                })
            );
        }
        assert_eq!(feed.event, Some(event));
        clean(&mut session, phase);
        assert!(session.take_outgoing().unwrap().is_empty());
        session.finish().unwrap();
    }
}

#[test]
fn every_error_close_and_truncation_wipes_secrets_and_prevents_resume() {
    let mut session = session();
    assert!(session.feed(&[0]).is_err());
    clean(&mut session, LobbyPhase::Failed);
    assert!(session.take_outgoing().is_err());
    assert!(session.feed(&[0]).is_err());
    for code in 1..=255 {
        let mut session = super::tests::session();
        session.take_outgoing().unwrap();
        assert!(session.feed(&[code]).is_err());
        clean(&mut session, LobbyPhase::Failed);
    }
    for cut in 0..SUCCESS.len() {
        let mut session = awaiting();
        session.feed(&SUCCESS[..cut]).unwrap();
        assert!(session.finish().is_err());
        clean(&mut session, LobbyPhase::Failed);
    }
    let packet = server().encode(ServerPacket::WORLD_LIST, BASIC).unwrap();
    for cut in 1..packet.len() {
        let mut session = accepted();
        session.feed(&packet[..cut]).unwrap();
        assert!(session.finish().is_err());
        clean(&mut session, LobbyPhase::Failed);
    }
    let mut session = accepted();
    let payload = include_bytes!("../../../login/tests/fixtures/world-list-split3-0.payload");
    session
        .feed(&server().encode(ServerPacket::WORLD_LIST, payload).unwrap())
        .unwrap();
    assert!(session.finish().is_err());
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.close();
    clean(&mut session, LobbyPhase::Closed);
    assert!(session.take_outgoing().is_err());
    let mut session = awaiting();
    assert!(session.confirm().is_err());
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.request_world_list(0).unwrap();
    assert!(session.request_world_list(0).is_err());
    clean(&mut session, LobbyPhase::Failed);
}

#[test]
fn byte_event_operation_and_lifetime_budgets_are_terminal() {
    let mut session = accepted();
    assert_eq!(
        session.feed(&vec![0; MAX_FEED_BYTES + 1]).err(),
        Some("lobby incoming chunk byte limit")
    );
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.incoming_bytes = MAX_INCOMING_BYTES;
    assert_eq!(
        session.feed(&[0]).err(),
        Some("lobby incoming total byte limit")
    );
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    let mut encoder = server();
    for _ in session.events..MAX_EVENTS {
        let wire = encoder.encode(ServerPacket::PING, &[]).unwrap();
        assert!(session.feed(&wire).unwrap().event.is_some());
    }
    assert_eq!(
        session
            .feed(&encoder.encode(ServerPacket::PING, &[]).unwrap())
            .err(),
        Some("lobby packet event limit")
    );
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.operations = MAX_OPERATIONS;
    assert_eq!(
        session.take_outgoing().err(),
        Some("lobby session operation limit")
    );
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.lifetime.started =
        std::time::Instant::now() - std::time::Duration::from_secs(MAX_LIFETIME_SECONDS);
    assert_eq!(
        session.take_outgoing().err(),
        Some("lobby session lifetime limit")
    );
    clean(&mut session, LobbyPhase::Failed);
    let mut session = accepted();
    session.lifetime.started =
        std::time::Instant::now() - std::time::Duration::from_secs(MAX_LIFETIME_SECONDS);
    clean(&mut session, LobbyPhase::Failed);
}
