#![cfg(not(target_arch = "wasm32"))]

use super::*;
use crate::lobby::tests::{accepted as lobby, accepted_with_default_world, config as lobby_config};
use openrs_login::isaac_ciphers;
use openrs_protocol::registry::RegistryEncoder;
use serde_json::{Value, json};

#[path = "inventory_tests.rs"]
mod inventory_tests;

#[path = "player_stats_tests.rs"]
mod player_stats_tests;

#[path = "cs2_run_tests.rs"]
mod cs2_run_tests;

#[path = "scene_tests.rs"]
mod scene_tests;

const SEED: [u32; 4] = [1, 4294967294, 0x12345678, 0x80000000];
const STANDARD: &[u8] =
    include_bytes!("../../../login/tests/fixtures/world-state-standard-104.payload");
const DYNAMIC: &[u8] =
    include_bytes!("../../../login/tests/fixtures/world-state-dynamic-size-0.payload");
const NATIVE_OUTGOING: &[u8] = include_bytes!("../../../login/tests/fixtures/isaac-outgoing.words");

fn config() -> Value {
    let mut value = lobby_config();
    let object = value.as_object_mut().unwrap();
    object.remove("password");
    object.remove("gameId");
    object.remove("language");
    let additions = json!({
        "reconnecting":false,
        "displayMode":2,
        "screenWidth":1280,
        "screenHeight":720,
        "antiAliasing":4,
        "machineInfo":{
            "operatingSystem":3,"x64Os":true,"osVendor":9,"javaVendor":4,
            "javaBuild":17,"javaSubBuild":2,"javaUpdate":3,"unknownFlag":false,
            "maxMemory":4096,"processors":8,"ram":0xabcdef,"cpuClock":4200,
            "gpuModel":"FAKE_GPU_€","gpuDriverVersion":"FAKE_DRIVER_™",
            "directxVersion":"12.1","directxLetter":"A","dxDriverMonth":10,
            "dxDriverYear":2026,"cpuType":"FAKE_CPU","cpuData":"OFFLINE_CPU_Œ",
            "cpuCores":16,"cpuFamilyId":254,
            "cpuFeatureFlags":[0x89abcdefu32,0x12345678u32,0x80000000u32],
            "cpuModelInfo":0xfedcba98u32,
        },
        "varVerifyId":0xfedcba98u32 as i32,
        "machineUid":[0x88776655u32,0x44332211u32],
        "loginToken":"FAKE_CLIENT_LOGIN_€",
        "supplementaryLoginData":"FAKE_EXTRA_ÿ",
        "jagtheoraLoaded":true,"isApplet":false,"hasSignedTermsAgreement":true,
        "loginType":7,"js5AuthToken":"FAKE_WORLD_JS5_™","differentWorld":true,
        "worldId":42,
    });
    object.extend(additions.as_object().unwrap().clone());
    value
}

fn session() -> WorldSession {
    lobby()
        .start_world(WorldConfig::from_json(&config().to_string()).unwrap())
        .unwrap()
}

fn awaiting() -> WorldSession {
    let mut session = session();
    assert!(session.take_outgoing().unwrap().as_slice() == [14]);
    assert_eq!(session.feed(&[0]).unwrap().consumed, 1);
    assert!(!session.take_outgoing().unwrap().is_empty());
    session
}

fn ready() -> WorldSession {
    let mut session = awaiting();
    session.feed(&response(1)).unwrap();
    session
        .feed(&initial(ServerPacket::REGION, STANDARD).0)
        .unwrap();
    session
}

fn queue_keepalive(session: &mut WorldSession) {
    for _ in 0..=KEEPALIVE_IDLE_TICKS {
        session.game_tick().unwrap();
    }
}

fn state_awaiting(count: usize) -> WorldSession {
    let mut session = lobby()
        .start_world_state(
            WorldConfig::from_json(&config().to_string()).unwrap(),
            count,
        )
        .unwrap();
    assert_eq!(session.take_outgoing().unwrap().as_slice(), [14]);
    session.feed_at(&[0], 0).unwrap();
    assert!(!session.take_outgoing().unwrap().is_empty());
    session
}

fn state_ready(count: usize) -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut session = state_awaiting(count);
    session.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    assert_eq!(
        session.feed_at(&first, 0).unwrap().event,
        Some(WorldEvent::WorldReady)
    );
    (session, encoder)
}

fn small_payload(id: u16, value: i8) -> [u8; 3] {
    [value as u8, (id as u8).wrapping_add(128), (id >> 8) as u8]
}

fn large_payload(id: u16, value: i32) -> [u8; 6] {
    let bytes = value.to_be_bytes();
    [
        bytes[1],
        bytes[0],
        bytes[3],
        bytes[2],
        (id as u8).wrapping_add(128),
        (id >> 8) as u8,
    ]
}

fn response(index: u16) -> Vec<u8> {
    let mut success = include_bytes!("../../../login/tests/fixtures/world-success.wire").to_vec();
    success[8..10].copy_from_slice(&index.to_be_bytes());
    success
}

fn initial(opcode: ServerPacket, payload: &[u8]) -> (Vec<u8>, RegistryEncoder<ServerPacket>) {
    let (_, mut incoming) = isaac_ciphers(&SEED).unwrap();
    let mut wire = vec![(opcode.wire() as u8).wrapping_add(incoming.next_u32() as u8)];
    wire.extend_from_slice(&(payload.len() as u16).to_be_bytes());
    wire.extend_from_slice(payload);
    (wire, RegistryEncoder::new(Some(incoming), 65535).unwrap())
}

fn clean(session: &mut WorldSession, phase: &'static str) {
    assert_eq!(session.phase(), phase);
    assert_eq!(
        session.report(),
        WorldReport {
            phase,
            accepted: false,
            world_ready: false,
            initial_world: None,
            variables: None,
            inventory: None,
            player_stats: None,
            scene: None,
        }
    );
    assert!(session.attempt.is_none());
    assert!(session.entry.is_none());
    assert!(session.packets.is_none());
    assert!(session.outgoing.is_none());
    assert!(session.pending_ping.is_none());
    assert!(!session.flush_pending);
    assert_eq!(session.idle_ticks, 0);
    assert!(session.initial.is_none());
    assert!(session.player_index.is_none());
    assert!(session.variables.is_none());
    assert!(session.inventory.is_none());
    assert!(session.varp(0).is_err());
}

#[test]
fn original_world_request_uses_only_the_rust_retained_lobby_issued_token() {
    let mut lobby = accepted_with_default_world(Some(43));
    let mut world = lobby
        .start_world(WorldConfig::from_json(&config().to_string()).unwrap())
        .unwrap();
    assert_eq!(lobby.phase(), LobbyPhase::Lobby);
    assert!(lobby.report().accepted);
    assert_eq!(world.phase(), "sendHandshake");
    assert!(world.take_outgoing().unwrap().as_slice() == [14]);
    assert_eq!(
        world.feed(&[0, 99]).unwrap(),
        WorldFeed {
            consumed: 1,
            event: None
        }
    );
    assert_eq!(world.phase(), "sendRequest");
    assert!(
        world.take_outgoing().unwrap().as_slice()
            == include_bytes!("../../../login/tests/fixtures/world-normal.wire")
    );
    lobby.close();
    assert_eq!(world.phase(), "awaitResponse");
    world.close();
    clean(&mut world, "closed");
    let mut unaccepted = crate::LobbySession::from_json(&lobby_config().to_string()).unwrap();
    assert!(
        unaccepted
            .start_world(WorldConfig::from_json(&config().to_string()).unwrap())
            .is_err()
    );
}

#[test]
fn world_request_derives_native_different_world_from_the_retained_lobby_response() {
    let native_wire = include_bytes!("../../../login/tests/fixtures/world-normal.wire");
    let native_plain = include_bytes!("../../../login/tests/fixtures/world-normal.xtea");
    let flag_at = native_plain.len() - 36 * 4 - 5;
    assert_eq!(native_plain[flag_at], 1);
    assert_eq!(
        &native_plain[flag_at + 1..flag_at + 5],
        &42i32.to_be_bytes()
    );
    for (default_world_id, selected_world, expected) in [
        (Some(42), 42i32, false),
        (Some(43), 42, true),
        (None, 42, true),
        (None, -1, false),
    ] {
        let mut lobby = accepted_with_default_world(default_world_id);
        for caller_flag in [false, true] {
            let mut supplied = config();
            supplied["differentWorld"] = json!(caller_flag);
            supplied["worldId"] = json!(selected_world);
            let mut world = lobby
                .start_world(WorldConfig::from_json(&supplied.to_string()).unwrap())
                .unwrap();
            assert_eq!(world.take_outgoing().unwrap().as_slice(), [14]);
            world.feed(&[0]).unwrap();
            let wire = world.take_outgoing().unwrap();
            let encrypted_at = 14 + usize::from(u16::from_be_bytes([wire[12], wire[13]]));
            assert_eq!(&wire[..encrypted_at], &native_wire[..encrypted_at]);
            let mut plain = wire[encrypted_at..].to_vec();
            let length = plain.len();
            openrs_login::xtea::decrypt(&mut plain, &SEED, 0..length).unwrap();
            let mut expected_plain = native_plain.to_vec();
            expected_plain[flag_at] = u8::from(expected);
            expected_plain[flag_at + 1..flag_at + 5].copy_from_slice(&selected_world.to_be_bytes());
            assert_eq!(plain, expected_plain);
            world.close();
        }
        lobby.close();
        assert_eq!(lobby.phase(), LobbyPhase::Closed);
        assert!(
            lobby
                .start_world(WorldConfig::from_json(&config().to_string()).unwrap())
                .is_err()
        );
    }
}

#[test]
fn world_config_rejects_omissions_secret_overrides_coercion_and_reconnection() {
    for field in config().as_object().unwrap().keys() {
        let mut invalid = config();
        invalid.as_object_mut().unwrap().remove(field);
        assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
    }
    for field in config()["machineInfo"].as_object().unwrap().keys() {
        let mut invalid = config();
        invalid["machineInfo"]
            .as_object_mut()
            .unwrap()
            .remove(field);
        assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
    }
    for field in [
        "password",
        "lobbyAuthToken",
        "worldToken",
        "worldLoginToken",
    ] {
        let mut invalid = config();
        invalid[field] = json!("secret-never-returned");
        assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
    }
    for (field, values) in [
        (
            "seed",
            vec![
                json!(-1),
                json!(4294967296u64),
                json!(1.5),
                json!("1"),
                json!(null),
            ],
        ),
        ("machineUid", vec![json!(-1), json!(4294967296u64)]),
    ] {
        for value in values {
            let mut invalid = config();
            invalid[field][0] = value;
            assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
        }
    }
    let mut invalid = config();
    invalid["machineInfo"]["cpuFeatureFlags"][0] = json!(-1);
    assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
    let mut invalid = config();
    invalid["machineInfo"]["cpuFeatureFlags"] = json!([0, 1]);
    assert!(WorldConfig::from_json(&invalid.to_string()).is_err());
    let mut invalid = config();
    invalid["reconnecting"] = json!(true);
    assert_eq!(
        lobby()
            .start_world(WorldConfig::from_json(&invalid.to_string()).unwrap())
            .err()
            .unwrap(),
        "world reconnect is unsupported"
    );
    let mut invalid = config();
    invalid["machineInfo"]["ram"] = json!(0x1000000u32);
    assert_eq!(
        lobby()
            .start_world(WorldConfig::from_json(&invalid.to_string()).unwrap())
            .err()
            .unwrap(),
        "invalid world request configuration"
    );
    assert!(WorldConfig::from_json(&" ".repeat(MAX_CONFIG_BYTES + 1)).is_err());
    let mut oversized = config();
    oversized["machineInfo"]["gpuModel"] = json!("x".repeat(MAX_CONFIG_BYTES + 1));
    let typed = serde_json::from_value::<WorldConfig>(oversized).unwrap();
    assert_eq!(
        lobby().start_world(typed).err(),
        Some("world configuration byte limit")
    );
}

#[test]
fn fresh_standard_and_dynamic_state_only_become_ready_after_exact_initial_frame_boundary() {
    for (opcode, payload) in [
        (ServerPacket::REGION, STANDARD),
        (ServerPacket::DYNAMIC_MAP_REGION, DYNAMIC),
    ] {
        let expected = decode_fresh_world(1, opcode, payload).unwrap();
        let expected_report = WorldInitialReport::from(&expected);
        let (first, mut suffix) = initial(opcode, payload);
        let mut wire = response(1);
        wire.extend_from_slice(&first);
        wire.extend(suffix.encode(ServerPacket::PING, &[]).unwrap());
        wire.extend(
            suffix
                .encode(ServerPacket::MIDI_SONG_LOCATION, &[0xfe, 0xa5, 0, 1, 2, 3])
                .unwrap(),
        );
        for size in [1, 2, 3, 7, 31, 511, 1024, wire.len()] {
            let mut session = awaiting();
            let mut events = Vec::new();
            for chunk in wire.chunks(size) {
                let mut rest = chunk;
                while !rest.is_empty() {
                    let feed = session.feed(rest).unwrap();
                    assert!((1..=rest.len()).contains(&feed.consumed));
                    rest = &rest[feed.consumed..];
                    if let Some(event) = feed.event {
                        if event == WorldEvent::LoginAccepted {
                            assert_eq!(session.phase(), "awaitInitialWorld");
                            assert!(session.report().accepted);
                            assert!(!session.report().world_ready);
                        }
                        events.push(event);
                    }
                }
            }
            assert_eq!(events[0], WorldEvent::LoginAccepted);
            assert_eq!(events[1], WorldEvent::WorldReady);
            assert_eq!(events.len(), 4);
            assert!(matches!(
                events[2],
                WorldEvent::OpaquePacket {
                    opcode: 29,
                    payload_bytes: 0,
                    ..
                }
            ));
            assert!(matches!(
                events[3],
                WorldEvent::OpaquePacket {
                    opcode: 128,
                    payload_bytes: 6,
                    ..
                }
            ));
            assert_eq!(
                session.report().initial_world,
                Some(expected_report.clone())
            );
            assert_eq!(session.phase(), "worldReady");
            assert!(session.report().world_ready);
            assert!(session.take_outgoing().unwrap().is_empty());
            let report = serde_json::to_value(session.report()).unwrap();
            assert_eq!(
                report
                    .as_object()
                    .unwrap()
                    .keys()
                    .cloned()
                    .collect::<Vec<_>>(),
                ["accepted", "initialWorld", "phase", "worldReady"]
            );
            assert_eq!(
                report["initialWorld"]
                    .as_object()
                    .unwrap()
                    .keys()
                    .cloned()
                    .collect::<Vec<_>>(),
                [
                    "base",
                    "chunkCount",
                    "mapKind",
                    "mapSize",
                    "playerIndex",
                    "regionIds",
                    "tile"
                ]
            );
            let text = report.to_string().to_lowercase();
            for forbidden in [
                "token", "password", "seed", "key", "owner", "username", "offline_", "fake_",
            ] {
                assert!(!text.contains(forbidden));
            }
            session.finish().unwrap();
            clean(&mut session, "closed");
        }
    }
}

#[test]
fn initial_map_kind_uses_decoded_grid_presence_even_when_no_dynamic_chunks_exist() {
    for (opcode, payload, map_kind) in [
        (ServerPacket::REGION, STANDARD, "standard"),
        (ServerPacket::DYNAMIC_MAP_REGION, DYNAMIC, "dynamic"),
        (
            ServerPacket::DYNAMIC_MAP_REGION,
            include_bytes!("../../../login/tests/fixtures/world-state-dynamic-empty.payload")
                .as_slice(),
            "dynamic",
        ),
    ] {
        let mut session = awaiting();
        session.feed(&response(1)).unwrap();
        session.feed(&initial(opcode, payload).0).unwrap();
        let report = session.report().initial_world.unwrap();
        assert_eq!(report.map_kind, map_kind);
        assert_eq!(serde_json::to_value(&report).unwrap()["mapKind"], map_kind);
        if payload != DYNAMIC {
            assert_eq!(report.chunk_count, 0);
        }
        session.close();
        clean(&mut session, "closed");
    }
}

#[test]
fn world_response_and_initial_frame_each_stop_before_coalesced_suffix() {
    let success = response(1);
    let (first, mut suffix) = initial(ServerPacket::REGION, STANDARD);
    let ping = suffix.encode(ServerPacket::PING, &[]).unwrap();
    let mut session = awaiting();
    let wire = [success.as_slice(), first.as_slice(), ping.as_slice()].concat();
    let feed = session.feed(&wire).unwrap();
    assert_eq!(feed.consumed, success.len());
    assert_eq!(feed.event, Some(WorldEvent::LoginAccepted));
    let feed = session.feed(&wire[success.len()..]).unwrap();
    assert_eq!(feed.consumed, first.len());
    assert_eq!(feed.event, Some(WorldEvent::WorldReady));
    let feed = session.feed(&wire[success.len() + first.len()..]).unwrap();
    assert_eq!(feed.consumed, ping.len());
    assert!(matches!(
        feed.event,
        Some(WorldEvent::OpaquePacket { opcode: 29, .. })
    ));
}

#[test]
fn malformed_truncated_or_nonmap_initial_packets_never_report_world_ready() {
    for (opcode, payload) in [
        (ServerPacket::PING, vec![]),
        (ServerPacket::REGION, vec![0; 7]),
        (
            ServerPacket::REGION,
            STANDARD[..STANDARD.len() - 1].to_vec(),
        ),
        (ServerPacket::DYNAMIC_MAP_REGION, vec![0xff; 64]),
    ] {
        let mut session = awaiting();
        session.feed(&response(1)).unwrap();
        assert!(session.feed(&initial(opcode, &payload).0).is_err());
        clean(&mut session, "failed");
    }
    for index in [0, 2048, u16::MAX] {
        let mut session = awaiting();
        assert!(session.feed(&response(index)).is_err());
        clean(&mut session, "failed");
    }
    let (first, _) = initial(ServerPacket::REGION, STANDARD);
    for cut in [0, 1, 2, 3, 4, first.len() - 1] {
        let mut session = awaiting();
        session.feed(&response(1)).unwrap();
        session.feed(&first[..cut]).unwrap();
        assert!(session.finish().is_err());
        clean(&mut session, "failed");
        assert!(session.feed(&first[cut..]).is_err());
    }
    let mut session = awaiting();
    session.feed(&response(1)).unwrap();
    session.close();
    clean(&mut session, "closed");
}

#[test]
fn confirmation_preserves_request_cipher_state_and_terminal_responses_stay_explicit() {
    let mut session = awaiting();
    assert_eq!(
        session.feed(&[1, 99]).unwrap(),
        WorldFeed {
            consumed: 1,
            event: Some(WorldEvent::ConfirmationRequired)
        }
    );
    assert_eq!(session.phase(), "awaitConfirmation");
    session.confirm().unwrap();
    assert!(
        session.take_outgoing().unwrap().as_slice()
            == [26u8.wrapping_add(isaac_ciphers(&SEED).unwrap().0.next_u32() as u8)]
    );
    session.feed(&response(1)).unwrap();
    session
        .feed(&initial(ServerPacket::REGION, STANDARD).0)
        .unwrap();
    let mut ciphers = isaac_ciphers(&SEED).unwrap().0;
    ciphers.next_u32();
    assert_eq!(
        session.outgoing.as_mut().unwrap().next_u32(),
        ciphers.next_u32()
    );
    for (wire, phase, event) in [
        (
            &[0][..],
            "rejected",
            WorldEvent::Rejected { response_code: 0 },
        ),
        (
            &[3][..],
            "rejected",
            WorldEvent::Rejected { response_code: 3 },
        ),
        (
            &[29, 9][..],
            "rejected",
            WorldEvent::Rejected { response_code: 29 },
        ),
        (
            &[45, 1, 2, 3][..],
            "rejected",
            WorldEvent::Rejected { response_code: 45 },
        ),
        (
            &[255][..],
            "rejected",
            WorldEvent::Rejected { response_code: 255 },
        ),
        (&[21, 2][..], "queued", WorldEvent::Queued),
        (&[23][..], "retry", WorldEvent::RetryRequested),
        (&[15][..], "reconnect", WorldEvent::ReconnectRequired),
    ] {
        let mut session = awaiting();
        let feed = session.feed(wire).unwrap();
        if let WorldEvent::Rejected { response_code } = &event {
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
        clean(&mut session, "closed");
    }
}

#[test]
fn explicit_definition_span_is_required_and_legacy_varps_remain_opaque() {
    let config = || WorldConfig::from_json(&config().to_string()).unwrap();
    let mut accepted = lobby();
    assert!(
        accepted
            .start_world_state(config(), crate::vars::MAX_VARPS + 1)
            .is_err()
    );
    assert!(accepted.start_world_state(config(), usize::MAX).is_err());
    assert_eq!(accepted.phase(), LobbyPhase::Lobby);
    for count in [0, 2, crate::vars::MAX_VARPS] {
        let mut state = accepted.start_world_state(config(), count).unwrap();
        assert_eq!(state.report().variables.unwrap().count, count);
        if count > 0 {
            assert_eq!(state.varp((count - 1) as u16), Ok(0));
        }
        if count < crate::vars::MAX_VARPS {
            assert!(state.varp(count as u16).is_err());
            assert_eq!(state.phase(), "sendHandshake");
        }
        state.close();
        clean(&mut state, "closed");
    }
    let mut unaccepted = crate::LobbySession::from_json(&lobby_config().to_string()).unwrap();
    assert!(unaccepted.start_world_state(config(), 2).is_err());
    let mut legacy = awaiting();
    legacy.feed(&response(1)).unwrap();
    let (first, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    legacy.feed(&first).unwrap();
    for (opcode, payload) in [
        (
            ServerPacket::VARP_SMALL,
            small_payload(u16::MAX, -128).to_vec(),
        ),
        (
            ServerPacket::VARP_LARGE,
            large_payload(u16::MAX, i32::MIN).to_vec(),
        ),
        (ServerPacket::CLEAR_VARPS, vec![]),
    ] {
        assert!(matches!(
            legacy
                .feed(&encoder.encode(opcode, &payload).unwrap())
                .unwrap()
                .event,
            Some(WorldEvent::OpaquePacket { .. })
        ));
    }
    assert!(legacy.report().variables.is_none());
    assert!(legacy.varp(0).is_err());
    assert!(legacy.tick_at(100).unwrap().events.is_empty());
    assert_eq!(legacy.phase(), "worldReady");
}

#[test]
fn live_varp_packets_preserve_boundaries_and_publish_only_when_serviced() {
    let success = response(1);
    let (first, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    let small = encoder
        .encode(ServerPacket::VARP_SMALL, &small_payload(1, -128))
        .unwrap();
    let large = encoder
        .encode(ServerPacket::VARP_LARGE, &large_payload(2, i32::MIN))
        .unwrap();
    let opaque = encoder.encode(ServerPacket::PING, &[]).unwrap();
    let wire = [
        success.as_slice(),
        first.as_slice(),
        small.as_slice(),
        large.as_slice(),
        opaque.as_slice(),
    ]
    .concat();
    for size in [1, 2, 3, 7, 31, wire.len()] {
        let mut session = state_awaiting(3);
        let mut events = Vec::new();
        for chunk in wire.chunks(size) {
            let mut rest = chunk;
            while !rest.is_empty() {
                let feed = session.feed_at(rest, 0).unwrap();
                assert!((1..=rest.len()).contains(&feed.consumed));
                rest = &rest[feed.consumed..];
                if let Some(event) = feed.event {
                    events.push(event);
                }
            }
        }
        assert_eq!(
            events,
            [
                WorldEvent::LoginAccepted,
                WorldEvent::WorldReady,
                WorldEvent::VariableReceived {
                    opcode: 115,
                    name: "VARP_SMALL",
                    id: 1,
                    value: -128
                },
                WorldEvent::VariableReceived {
                    opcode: 8,
                    name: "VARP_LARGE",
                    id: 2,
                    value: i32::MIN
                },
                WorldEvent::OpaquePacket {
                    opcode: 29,
                    name: "PING",
                    payload_bytes: 0
                },
            ]
        );
        assert_eq!(session.varp(1), Ok(0));
        assert_eq!(session.varp(2), Ok(0));
        assert_eq!(
            session.variables.as_ref().unwrap().bank.pending(1),
            Ok(-128)
        );
        assert!(session.tick_at(1).unwrap().events.is_empty());
        assert_eq!(
            session.tick_at(2).unwrap().events,
            [
                WorldEvent::VariablePublished {
                    id: 1,
                    previous: 0,
                    value: -128
                },
                WorldEvent::VariablePublished {
                    id: 2,
                    previous: 0,
                    value: i32::MIN
                },
            ]
        );
        assert!(session.tick_at(2).unwrap().events.is_empty());
        assert_eq!(session.varp(1), Ok(-128));
        assert_eq!(session.varp(2), Ok(i32::MIN));
        assert_eq!(
            session.report().variables,
            Some(WorldVariableReport {
                count: 3,
                scheduled: 0,
                server_assignments: 2,
                published: 2,
                clears: 0,
                non_zero: 2
            })
        );
        let report = serde_json::to_value(session.report()).unwrap();
        assert_eq!(
            report["variables"],
            json!({"count":3,"scheduled":0,"serverAssignments":2,"published":2,"clears":0,"nonZero":2})
        );
        assert_eq!(
            serde_json::to_value(&events[2]).unwrap(),
            json!({"kind":"variableReceived","opcode":115,"name":"VARP_SMALL","id":1,"value":-128})
        );
        session.close();
        clean(&mut session, "closed");
    }
}

#[test]
fn live_local_override_deadlines_coalesce_and_clear_both_banks_and_schedules() {
    let (mut session, mut encoder) = state_ready(3);
    session
        .variables
        .as_mut()
        .unwrap()
        .bank
        .set_local(1, 9, 100)
        .unwrap();
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(1, -1))
                .unwrap(),
            200,
        )
        .unwrap();
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_LARGE, &large_payload(1, i32::MAX))
                .unwrap(),
            300,
        )
        .unwrap();
    assert_eq!(session.varp(1), Ok(9));
    assert!(session.tick_at(800).unwrap().events.is_empty());
    assert_eq!(
        session.tick_at(801).unwrap().events,
        [WorldEvent::VariablePublished {
            id: 1,
            previous: 9,
            value: i32::MAX
        }]
    );
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(2, 7))
                .unwrap(),
            802,
        )
        .unwrap();
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_LARGE, &large_payload(2, 900))
                .unwrap(),
            802,
        )
        .unwrap();
    assert_eq!(
        session.tick_at(802).unwrap().events,
        [WorldEvent::VariablePublished {
            id: 2,
            previous: 0,
            value: 900
        }]
    );
    session
        .variables
        .as_mut()
        .unwrap()
        .bank
        .set_local(1, 77, 803)
        .unwrap();
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(1, 4))
                .unwrap(),
            804,
        )
        .unwrap();
    assert_eq!(
        session
            .feed_at(
                &encoder.encode(ServerPacket::CLEAR_VARPS, &[]).unwrap(),
                805
            )
            .unwrap()
            .event,
        Some(WorldEvent::VariablesCleared {
            opcode: 100,
            name: "CLEAR_VARPS"
        })
    );
    for id in 0..3 {
        assert_eq!(session.varp(id), Ok(0));
        assert_eq!(session.variables.as_ref().unwrap().bank.pending(id), Ok(0));
    }
    assert!(session.tick_at(2000).unwrap().events.is_empty());
    assert_eq!(
        session.report().variables,
        Some(WorldVariableReport {
            count: 3,
            scheduled: 0,
            server_assignments: 5,
            published: 2,
            clears: 1,
            non_zero: 0
        })
    );
}

#[test]
fn live_bad_ids_truncation_clocks_and_event_limits_fail_closed_and_drop_bank() {
    for opcode in [ServerPacket::VARP_SMALL, ServerPacket::VARP_LARGE] {
        for count in [0, 2] {
            let (mut session, mut encoder) = state_ready(count);
            let payload = if opcode == ServerPacket::VARP_SMALL {
                small_payload(count as u16, -1).to_vec()
            } else {
                large_payload(count as u16, 100).to_vec()
            };
            let packet = encoder.encode(opcode, &payload).unwrap();
            assert_eq!(
                session.feed_at(&packet, 10).err(),
                Some("invalid world variable packet")
            );
            clean(&mut session, "failed");
        }
        let payload = if opcode == ServerPacket::VARP_SMALL {
            small_payload(1, -1).to_vec()
        } else {
            large_payload(1, 100).to_vec()
        };
        for cut in 1..=payload.len() {
            let (mut session, mut encoder) = state_ready(2);
            let packet = encoder.encode(opcode, &payload).unwrap();
            assert!(session.feed_at(&packet[..cut], 10).unwrap().event.is_none());
            assert!(session.finish().is_err());
            clean(&mut session, "failed");
        }
    }
    for now in [9, crate::vars::MAX_VARIABLE_TIME_MS + 1] {
        let (mut session, _) = state_ready(2);
        session.tick_at(10).unwrap();
        assert_eq!(
            session.tick_at(now).err(),
            Some("invalid world variable service")
        );
        clean(&mut session, "failed");
        let (mut session, mut encoder) = state_ready(2);
        session.tick_at(10).unwrap();
        assert_eq!(
            session
                .feed_at(
                    &encoder
                        .encode(ServerPacket::VARP_SMALL, &small_payload(1, 4))
                        .unwrap(),
                    now
                )
                .err(),
            Some("invalid world variable packet")
        );
        clean(&mut session, "failed");
    }
    let (mut session, mut encoder) = state_ready(2);
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(1, 4))
                .unwrap(),
            0,
        )
        .unwrap();
    session.events = MAX_EVENTS;
    assert_eq!(session.tick_at(2).err(), Some("world packet event limit"));
    clean(&mut session, "failed");
    let (mut session, _) = state_ready(2);
    session.operations = MAX_OPERATIONS;
    assert_eq!(
        session.tick_at(0).err(),
        Some("world session operation limit")
    );
    clean(&mut session, "failed");
}

#[test]
fn state_mode_never_treats_a_varp_as_initial_world_and_drops_bank_on_rejection() {
    let mut session = state_awaiting(2);
    let success = response(1);
    let (bad_initial, _) = initial(ServerPacket::VARP_SMALL, &small_payload(1, 4));
    let coalesced = [success.as_slice(), bad_initial.as_slice()].concat();
    assert_eq!(
        session.feed_at(&coalesced, 10).unwrap(),
        WorldFeed {
            consumed: success.len(),
            event: Some(WorldEvent::LoginAccepted)
        }
    );
    assert!(!session.report().world_ready);
    assert!(session.feed_at(&coalesced[success.len()..], 10).is_err());
    clean(&mut session, "failed");
    for (wire, phase) in [
        (&[3][..], "rejected"),
        (&[21, 2][..], "queued"),
        (&[23][..], "retry"),
        (&[15][..], "reconnect"),
    ] {
        let mut session = state_awaiting(2);
        let wire_with_suffix = [wire, &[99]].concat();
        assert_eq!(
            session.feed_at(&wire_with_suffix, 10).unwrap().consumed,
            wire.len()
        );
        clean(&mut session, phase);
    }
}

#[test]
fn world_keepalive_matches_every_native_cipher_word_and_preserves_confirmation_order() {
    assert_eq!(ClientPacket::PING.wire(), 0);
    assert_eq!(
        ClientPacket::PING.metadata().size,
        openrs_protocol::framing::PacketSize::Fixed(0)
    );
    for confirmed in [false, true] {
        let mut session = if confirmed {
            let mut session = awaiting();
            session.feed(&[1]).unwrap();
            session.confirm().unwrap();
            assert_eq!(
                session.take_outgoing().unwrap().as_slice(),
                [26u8.wrapping_add(NATIVE_OUTGOING[3])]
            );
            session.feed(&response(1)).unwrap();
            session
                .feed(&initial(ServerPacket::REGION, STANDARD).0)
                .unwrap();
            session
        } else {
            ready()
        };
        let events = session.events;
        for native_word in NATIVE_OUTGOING.chunks_exact(4).skip(usize::from(confirmed)) {
            queue_keepalive(&mut session);
            assert_eq!(session.idle_ticks, 51);
            assert!(session.pending_ping.is_some());
            assert!(!session.flush_pending);
            assert_eq!(
                session.take_outgoing().unwrap().as_slice(),
                [native_word[3]]
            );
            assert!(session.pending_ping.is_none());
            assert!(session.flush_pending);
            assert_eq!(session.idle_ticks, 51);
            session.mark_flushed().unwrap();
            assert_eq!(session.idle_ticks, 0);
            assert!(!session.flush_pending);
        }
        assert_eq!(session.events, events);
        session.finish().unwrap();
        clean(&mut session, "closed");
    }
}

#[test]
fn world_keepalive_needs_tick_fifty_one_and_coalesces_until_actual_flush_acknowledgement() {
    let mut session = ready();
    for tick in 1..=KEEPALIVE_IDLE_TICKS {
        session.game_tick().unwrap();
        assert_eq!(session.idle_ticks, tick);
        assert!(session.pending_ping.is_none());
        assert!(session.take_outgoing().unwrap().is_empty());
        assert!(!session.flush_pending);
    }
    session.game_tick().unwrap();
    assert_eq!(session.idle_ticks, 51);
    for _ in 0..1000 {
        session.game_tick().unwrap();
    }
    assert_eq!(session.idle_ticks, u8::MAX);
    assert_eq!(
        session.take_outgoing().unwrap().as_slice(),
        [NATIVE_OUTGOING[3]]
    );
    for _ in 0..1000 {
        session.game_tick().unwrap();
        assert!(session.take_outgoing().unwrap().is_empty());
    }
    assert_eq!(session.idle_ticks, u8::MAX);
    assert!(session.flush_pending);
    session.mark_flushed().unwrap();
    assert_eq!(session.idle_ticks, 0);
    for _ in 0..KEEPALIVE_IDLE_TICKS {
        session.game_tick().unwrap();
    }
    assert!(session.take_outgoing().unwrap().is_empty());
    session.game_tick().unwrap();
    assert_eq!(
        session.take_outgoing().unwrap().as_slice(),
        [NATIVE_OUTGOING[7]]
    );
    session.mark_flushed().unwrap();
    session.finish().unwrap();
    clean(&mut session, "closed");
}

#[test]
fn world_keepalive_feed_and_variable_publication_keep_separate_tick_order_and_state() {
    let (mut session, mut encoder) = state_ready(2);
    for _ in 0..KEEPALIVE_IDLE_TICKS {
        session.game_tick().unwrap();
    }
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(1, 7))
                .unwrap(),
            0,
        )
        .unwrap();
    assert_eq!(session.varp(1), Ok(0));
    assert_eq!(session.idle_ticks, 50);
    assert_eq!(
        session.tick_at(2).unwrap().events,
        [WorldEvent::VariablePublished {
            id: 1,
            previous: 0,
            value: 7
        }]
    );
    assert_eq!(session.idle_ticks, 50);
    assert!(session.take_outgoing().unwrap().is_empty());
    session.game_tick().unwrap();
    assert_eq!(session.idle_ticks, 51);
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_LARGE, &large_payload(1, 8))
                .unwrap(),
            2,
        )
        .unwrap();
    for now in 3..303 {
        let ping = encoder.encode(ServerPacket::PING, &[]).unwrap();
        session.feed_at(&ping, now).unwrap();
        session.game_tick().unwrap();
    }
    assert_eq!(session.varp(1), Ok(7));
    assert_eq!(
        session.take_outgoing().unwrap().as_slice(),
        [NATIVE_OUTGOING[3]]
    );
    assert_eq!(session.idle_ticks, u8::MAX);
    assert_eq!(
        session.tick_at(303).unwrap().events,
        [WorldEvent::VariablePublished {
            id: 1,
            previous: 7,
            value: 8
        }]
    );
    assert_eq!(session.idle_ticks, u8::MAX);
    assert!(session.flush_pending);
    assert!(session.take_outgoing().unwrap().is_empty());
    session.mark_flushed().unwrap();
    assert_eq!(session.idle_ticks, 0);
    assert_eq!(session.varp(1), Ok(8));
    session.finish().unwrap();
    clean(&mut session, "closed");

    let mut legacy = ready();
    for _ in 0..1000 {
        assert!(legacy.tick_at(u64::MAX).unwrap().events.is_empty());
    }
    assert_eq!(legacy.idle_ticks, 0);
    assert!(legacy.take_outgoing().unwrap().is_empty());
    legacy.finish().unwrap();
    clean(&mut legacy, "closed");
}

#[test]
fn world_keepalive_and_flush_reject_invalid_stages_empty_acknowledgements_and_unsent_eof() {
    for operation in [WorldSession::game_tick, WorldSession::mark_flushed] {
        for stage in 0..3 {
            let mut session = match stage {
                0 => session(),
                1 => awaiting(),
                _ => {
                    let mut session = awaiting();
                    session.feed(&response(1)).unwrap();
                    session
                }
            };
            assert!(operation(&mut session).is_err());
            clean(&mut session, "failed");
        }
    }
    let mut session = ready();
    for _ in 0..KEEPALIVE_IDLE_TICKS {
        session.game_tick().unwrap();
        assert!(session.take_outgoing().unwrap().is_empty());
    }
    assert_eq!(
        session.mark_flushed().err(),
        Some("world flush requires outgoing bytes")
    );
    clean(&mut session, "failed");

    let mut session = ready();
    queue_keepalive(&mut session);
    assert_eq!(
        session.mark_flushed().err(),
        Some("world outgoing buffer must be drained")
    );
    clean(&mut session, "failed");

    for drained in [false, true] {
        let (mut session, _) = state_ready(2);
        queue_keepalive(&mut session);
        if drained {
            assert_eq!(session.take_outgoing().unwrap().len(), 1);
        }
        assert_eq!(
            session.finish().err(),
            Some("world connection ended with unflushed bytes")
        );
        clean(&mut session, "failed");
        assert!(session.game_tick().is_err());
        assert!(session.mark_flushed().is_err());
        assert!(session.take_outgoing().is_err());
    }
    let mut session = ready();
    queue_keepalive(&mut session);
    session.take_outgoing().unwrap();
    session.mark_flushed().unwrap();
    assert_eq!(
        session.mark_flushed().err(),
        Some("world flush requires outgoing bytes")
    );
    clean(&mut session, "failed");
}

#[test]
fn world_keepalive_preserves_terminal_cleanup_byte_event_and_operation_limits() {
    for drained in [false, true] {
        for operation in [WorldSession::game_tick, WorldSession::mark_flushed] {
            let (mut session, _) = state_ready(2);
            queue_keepalive(&mut session);
            if drained {
                session.take_outgoing().unwrap();
            }
            session.operations = MAX_OPERATIONS;
            assert_eq!(
                operation(&mut session).err(),
                Some("world session operation limit")
            );
            clean(&mut session, "failed");
        }
        let (mut session, mut encoder) = state_ready(2);
        queue_keepalive(&mut session);
        if drained {
            session.take_outgoing().unwrap();
        }
        let truncated = encoder
            .encode(ServerPacket::VARP_LARGE, &large_payload(1, 7))
            .unwrap();
        session.feed_at(&truncated[..2], 0).unwrap();
        assert!(session.finish().is_err());
        clean(&mut session, "failed");

        for failure in 0..3 {
            let (mut session, mut encoder) = state_ready(2);
            queue_keepalive(&mut session);
            if drained {
                session.take_outgoing().unwrap();
            }
            let (packet, expected) = match failure {
                0 => {
                    session.incoming_bytes = MAX_INCOMING_BYTES;
                    (vec![0], "world incoming total byte limit")
                }
                1 => {
                    session.events = MAX_EVENTS;
                    (
                        encoder.encode(ServerPacket::PING, &[]).unwrap(),
                        "world packet event limit",
                    )
                }
                _ => (
                    encoder
                        .encode(ServerPacket::VARP_SMALL, &small_payload(2, 7))
                        .unwrap(),
                    "invalid world variable packet",
                ),
            };
            assert_eq!(session.feed_at(&packet, 0).err(), Some(expected));
            clean(&mut session, "failed");
        }
        let (mut session, _) = state_ready(2);
        queue_keepalive(&mut session);
        if drained {
            session.take_outgoing().unwrap();
        }
        session.close();
        clean(&mut session, "closed");
        assert!(session.game_tick().is_err());
        assert!(session.mark_flushed().is_err());
        assert!(session.take_outgoing().is_err());
        session.finish().unwrap();
    }
}

#[test]
fn world_byte_event_and_operation_budgets_drop_private_state_and_ciphers() {
    let mut session = awaiting();
    assert_eq!(
        session.feed(&vec![0; MAX_FEED_BYTES + 1]).err(),
        Some("world incoming chunk byte limit")
    );
    clean(&mut session, "failed");
    let mut session = awaiting();
    session.incoming_bytes = MAX_INCOMING_BYTES;
    assert_eq!(
        session.feed(&[0]).err(),
        Some("world incoming total byte limit")
    );
    clean(&mut session, "failed");
    let mut session = awaiting();
    session.operations = MAX_OPERATIONS;
    assert_eq!(
        session.take_outgoing().err(),
        Some("world session operation limit")
    );
    clean(&mut session, "failed");
    let mut session = awaiting();
    session.feed(&response(1)).unwrap();
    let (first, mut suffix) = initial(ServerPacket::REGION, STANDARD);
    session.feed(&first).unwrap();
    session.events = MAX_EVENTS;
    assert_eq!(
        session
            .feed(&suffix.encode(ServerPacket::PING, &[]).unwrap())
            .err(),
        Some("world packet event limit")
    );
    clean(&mut session, "failed");
}

fn report_at(tile_x: u16, tile_y: u16, base: i32, map_size: u16) -> WorldInitialReport {
    WorldInitialReport {
        player_index: 1,
        tile: WorldTileReport {
            plane: 2,
            x: tile_x,
            y: tile_y,
        },
        base: WorldBaseReport { x: base, y: base },
        map_size,
        map_kind: "standard",
        region_ids: Vec::new(),
        chunk_count: 0,
    }
}

#[test]
fn scene_tile_is_the_world_tile_minus_the_base_inside_the_scene() {
    assert_eq!(
        report_at(3222, 3218, 3168, 104).scene_tile(),
        Some((2, 54, 50))
    );
    assert_eq!(
        report_at(3168, 3168, 3168, 104).scene_tile(),
        Some((2, 0, 0))
    );
    assert_eq!(
        report_at(3271, 3271, 3168, 104).scene_tile(),
        Some((2, 103, 103))
    );
    assert_eq!(report_at(3272, 3200, 3168, 104).scene_tile(), None);
    assert_eq!(report_at(3167, 3200, 3168, 104).scene_tile(), None);
    assert_eq!(report_at(3200, 3272, 3168, 104).scene_tile(), None);
}
