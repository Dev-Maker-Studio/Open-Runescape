use openrs_login::lobby_stream::{LobbyStream, StreamState};
use openrs_login::response::LoginResponse;
use openrs_login::session::{LoginAttempt, Phase};
use openrs_login::world_list::WorldListDecoder;
use openrs_login::{LobbyIdentity, LobbyRequest, isaac_ciphers};
use openrs_protocol::registry::{ClientPacket, RegistryDecoder, RegistryEncoder, ServerPacket};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const RANDOM: [u8; 24] = [7; 24];
const PREFERENCES: [u8; 35] = [11; 35];
const CRCS: [u32; 36] = [0x80000000; 36];

fn request() -> LobbyRequest<'static> {
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

fn response_stage() -> LoginAttempt {
    let mut attempt = LoginAttempt::lobby(&request()).unwrap();
    assert_eq!(attempt.take_outgoing().unwrap().unwrap().as_bytes(), [14]);
    assert_eq!(attempt.feed(&[0]).unwrap().consumed, 1);
    let request = attempt.take_outgoing().unwrap().unwrap();
    assert_eq!(request.as_bytes()[0], 19);
    assert_eq!(attempt.phase(), Phase::AwaitResponse);
    attempt
}

fn lobby_flow(chunk_size: usize, confirmation: bool) {
    let mut attempt = response_stage();
    let (mut expected_outgoing, incoming) = isaac_ciphers(&SEED).unwrap();
    if confirmation {
        let response = attempt.feed(&[1]).unwrap();
        assert!(matches!(
            response.event,
            Some(LoginResponse::ConfirmationRequired)
        ));
        attempt.confirm().unwrap();
        assert_eq!(
            attempt.take_outgoing().unwrap().unwrap().as_bytes(),
            [26u8.wrapping_add(expected_outgoing.next_u32() as u8)]
        );
    }
    let mut server = RegistryEncoder::<ServerPacket>::new(Some(incoming), 65535).unwrap();
    let mut wire = vec![42, 0x12, 0x34];
    wire.extend_from_slice(include_bytes!("fixtures/lobby-success.wire"));
    let chunks: &[&[u8]] = &[
        include_bytes!("fixtures/world-list-split3-0.payload"),
        include_bytes!("fixtures/world-list-split3-1.payload"),
        include_bytes!("fixtures/world-list-split3-2.payload"),
    ];
    for payload in chunks {
        wire.extend(server.encode(ServerPacket::WORLD_LIST, payload).unwrap());
        wire.extend(server.encode(ServerPacket::PING, &[]).unwrap());
    }
    wire.extend(
        server
            .encode(
                ServerPacket::WORLD_LIST,
                include_bytes!("fixtures/world-list-counts.payload"),
            )
            .unwrap(),
    );
    let mut stream = None;
    let mut world_list = WorldListDecoder::new();
    let mut successes = 0;
    let mut times = 0;
    let mut lists = 0;
    let mut pongs = 0;
    for chunk in wire.chunks(chunk_size) {
        let mut remaining = chunk;
        while !remaining.is_empty() {
            let consumed = if let Some(stream) = stream.as_mut() {
                let stream: &mut LobbyStream = stream;
                let result = stream.feed(remaining).unwrap();
                if let Some(packet) = result.item {
                    match packet.opcode {
                        ServerPacket::WORLD_LIST => {
                            if world_list.feed(&packet.payload).unwrap().is_some() {
                                lists += 1;
                            }
                        }
                        ServerPacket::PING => {
                            assert!(packet.payload.is_empty());
                            pongs += 1;
                        }
                        _ => panic!("unexpected synthetic packet"),
                    }
                }
                result.consumed
            } else {
                let result = attempt.feed(remaining).unwrap();
                match result.event {
                    Some(LoginResponse::LobbySuccess(lobby)) => {
                        assert_eq!(
                            lobby.world_login_token.as_str(),
                            "FAKE_LOBBY_ISSUED_TOKEN_727"
                        );
                        successes += 1;
                        stream =
                            Some(LobbyStream::new(attempt.take_ciphers().unwrap(), 65535).unwrap());
                    }
                    Some(LoginResponse::TimeSync { code }) => {
                        assert_eq!(code, 0x1234);
                        times += 1;
                    }
                    None => {}
                    _ => panic!("unexpected synthetic login event"),
                }
                result.consumed
            };
            assert!(consumed > 0 && consumed <= remaining.len());
            remaining = &remaining[consumed..];
        }
    }
    assert_eq!((successes, times, lists, pongs), (1, 1, 2, 3));
    let snapshot = world_list.snapshot().unwrap();
    let mut expected = WorldListDecoder::new();
    expected
        .feed(include_bytes!("fixtures/world-list-basic.payload"))
        .unwrap();
    expected
        .feed(include_bytes!("fixtures/world-list-counts.payload"))
        .unwrap();
    assert_eq!(Some(snapshot), expected.snapshot());
    let mut stream = stream.unwrap();
    let mut client = RegistryDecoder::<ClientPacket>::new(Some(expected_outgoing), 65535).unwrap();
    let request = stream
        .request_world_list(snapshot.version.unwrap())
        .unwrap();
    let result = client.feed(&request).unwrap();
    assert_eq!(result.consumed, request.len());
    let packet = result.item.unwrap();
    assert_eq!(packet.opcode, ClientPacket::REQUEST_WORLD_LIST);
    assert_eq!(packet.payload, snapshot.version.unwrap().to_be_bytes());
    stream.mark_flushed().unwrap();
    for _ in 0..50 {
        assert!(stream.tick().unwrap().is_none());
    }
    let ping = stream.tick().unwrap().unwrap();
    assert_eq!(
        client.feed(&ping).unwrap().item.unwrap().opcode,
        ClientPacket::PING
    );
    assert!(stream.tick().unwrap().is_none());
    stream.mark_flushed().unwrap();
    world_list.finish().unwrap();
    stream.finish().unwrap();
    assert_eq!(stream.state(), StreamState::Closed);
    client.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn fragmented_and_coalesced_login_packets_share_the_original_ciphers() {
    for size in (1..=17).chain([31, 63, 127, 65535]) {
        lobby_flow(size, false);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn confirmation_cipher_progress_survives_the_session_to_lobby_handoff() {
    for size in [1, 2, 3, 7, 63, 65535] {
        lobby_flow(size, true);
    }
}
