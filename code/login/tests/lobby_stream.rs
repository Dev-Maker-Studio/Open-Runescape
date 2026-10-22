use openrs_login::lobby_stream::{KEEPALIVE_IDLE_TICKS, LobbyStream, StreamError, StreamState};
use openrs_login::session::SessionCiphers;
use openrs_protocol::Error as ProtocolError;
use openrs_protocol::framing::{PacketEncoder, PacketSize};
use openrs_protocol::isaac::Isaac;
use openrs_protocol::registry::{ClientPacket, GamePacket, RegistryEncoder, ServerPacket};

fn fake_ciphers() -> SessionCiphers {
    let mut outgoing = Isaac::new(&[3, 5, 7, 11]).unwrap();
    let mut incoming = Isaac::new(&[53, 55, 57, 61]).unwrap();
    for _ in 0..521 {
        outgoing.next_u32();
    }
    for _ in 0..517 {
        incoming.next_u32();
    }
    SessionCiphers { outgoing, incoming }
}

fn stream(limit: usize) -> LobbyStream {
    LobbyStream::new(fake_ciphers(), limit).unwrap()
}

fn expect_error<T>(result: Result<T, StreamError>, expected: StreamError) {
    match result {
        Err(error) => assert_eq!(error, expected),
        Ok(_) => panic!("invalid stream operation succeeded"),
    }
}

fn queue_ping(stream: &mut LobbyStream) -> Vec<u8> {
    for _ in 0..KEEPALIVE_IDLE_TICKS {
        assert!(stream.tick().unwrap().is_none());
    }
    stream.tick().unwrap().unwrap()
}

fn assert_unusable(stream: &mut LobbyStream, error: StreamError) {
    assert!(!stream.ping_pending());
    expect_error(stream.encode(ClientPacket::PING, &[]), error.clone());
    expect_error(stream.request_world_list(0), error.clone());
    expect_error(stream.feed(&[]), error.clone());
    expect_error(stream.tick(), error.clone());
    expect_error(stream.mark_flushed(), error.clone());
    expect_error(stream.finish(), error);
}

fn server_packets() -> Vec<GamePacket<ServerPacket>> {
    vec![
        GamePacket {
            opcode: ServerPacket::WORLD_LIST,
            payload: vec![1, 0, 0, 7, 0xff, 0x80, 0],
        },
        GamePacket {
            opcode: ServerPacket::PING,
            payload: vec![],
        },
        GamePacket {
            opcode: ServerPacket::MIDI_SONG_LOCATION,
            payload: vec![0xfe, 0xa5, 0, 1, 2, 3],
        },
        GamePacket {
            opcode: ServerPacket::PROCESS_DEV_CONSOLE_COMMAND,
            payload: vec![0, 0x80, 0xff, 5, 9],
        },
    ]
}

fn server_frames(packets: &[GamePacket<ServerPacket>]) -> Vec<Vec<u8>> {
    let mut encoder =
        RegistryEncoder::<ServerPacket>::new(Some(fake_ciphers().incoming), u16::MAX as usize)
            .unwrap();
    packets
        .iter()
        .map(|packet| encoder.encode(packet.opcode, &packet.payload).unwrap())
        .collect()
}

fn feed_chunk(stream: &mut LobbyStream, bytes: &[u8], packets: &mut Vec<GamePacket<ServerPacket>>) {
    let mut consumed = 0;
    while consumed < bytes.len() {
        let result = stream.feed(&bytes[consumed..]).unwrap();
        assert!(result.consumed > 0);
        consumed += result.consumed;
        assert!(consumed <= bytes.len());
        if let Some(packet) = result.item {
            packets.push(packet);
        } else {
            assert_eq!(consumed, bytes.len());
        }
    }
    let empty = stream.feed(&[]).unwrap();
    assert_eq!(empty.consumed, 0);
    assert!(empty.item.is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn payload_limits_are_exactly_the_nonzero_native_u16_range() {
    for limit in [0, 65536, usize::MAX] {
        expect_error(
            LobbyStream::new(fake_ciphers(), limit),
            StreamError::Protocol(ProtocolError::InvalidLimit),
        );
    }
    for limit in [1, 65535] {
        let mut stream = stream(limit);
        assert_eq!(stream.state(), StreamState::Open);
        assert!(!stream.ping_pending());
        assert_eq!(stream.encode(ClientPacket::PING, &[]).unwrap().len(), 1);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn pings_use_exactly_one_next_transferred_outgoing_word_without_reinitialization() {
    let mut stream = stream(65535);
    let mut expected = fake_ciphers().outgoing;
    assert_eq!(ClientPacket::PING.wire(), 0);
    assert_eq!(ClientPacket::PING.metadata().size, PacketSize::Fixed(0));
    for _ in 0..520 {
        assert_eq!(queue_ping(&mut stream), [expected.next_u32() as u8]);
        assert!(stream.ping_pending());
        for _ in 0..100 {
            assert!(stream.tick().unwrap().is_none());
        }
        stream.mark_flushed().unwrap();
        assert!(!stream.ping_pending());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn tick_fifty_one_queues_once_and_only_explicit_flush_resets_idle_and_pending() {
    let mut stream = stream(65535);
    let mut expected = fake_ciphers().outgoing;
    for _ in 0..49 {
        assert!(stream.tick().unwrap().is_none());
    }
    let world_list = stream.request_world_list(-1).unwrap();
    assert_eq!(world_list[0], 47_u8.wrapping_add(expected.next_u32() as u8));
    assert_eq!(&world_list[1..], &[0xff; 4]);
    assert!(stream.tick().unwrap().is_none());
    let frames = server_frames(&[GamePacket {
        opcode: ServerPacket::PING,
        payload: vec![],
    }]);
    assert_eq!(
        stream.feed(&frames[0]).unwrap().item.unwrap().opcode,
        ServerPacket::PING
    );
    assert_eq!(stream.tick().unwrap().unwrap(), [expected.next_u32() as u8]);
    assert!(stream.ping_pending());
    let world_list = stream.request_world_list(7).unwrap();
    assert_eq!(world_list[0], 47_u8.wrapping_add(expected.next_u32() as u8));
    assert_eq!(&world_list[1..], &7_i32.to_be_bytes());
    for _ in 0..1000 {
        assert!(stream.tick().unwrap().is_none());
    }
    assert!(stream.ping_pending());
    stream.mark_flushed().unwrap();
    assert!(!stream.ping_pending());
    assert_eq!(queue_ping(&mut stream), [expected.next_u32() as u8]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn marking_a_regular_flush_resets_idle_before_any_ping_is_pending() {
    let mut stream = stream(65535);
    for _ in 0..KEEPALIVE_IDLE_TICKS {
        assert!(stream.tick().unwrap().is_none());
    }
    stream
        .encode(ClientPacket::REQUEST_WORLD_LIST, &[0; 4])
        .unwrap();
    stream.mark_flushed().unwrap();
    assert!(!stream.ping_pending());
    assert_eq!(queue_ping(&mut stream).len(), 1);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn world_list_requests_preserve_signed_i32_big_endian_and_registry_framing() {
    let mut stream = stream(65535);
    let mut encoder =
        RegistryEncoder::<ClientPacket>::new(Some(fake_ciphers().outgoing), 65535).unwrap();
    assert_eq!(ClientPacket::REQUEST_WORLD_LIST.wire(), 47);
    for version in [0, 1, -1, i32::MIN, i32::MAX, 0x1234_5678] {
        assert_eq!(
            stream.request_world_list(version).unwrap(),
            encoder
                .encode(ClientPacket::REQUEST_WORLD_LIST, &version.to_be_bytes())
                .unwrap(),
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn coalesced_and_every_split_world_list_pong_and_opaque_packets_are_lossless() {
    let expected = server_packets();
    let wire: Vec<u8> = server_frames(&expected).concat();
    for split in 0..=wire.len() {
        let mut stream = stream(65535);
        let mut packets = Vec::new();
        feed_chunk(&mut stream, &wire[..split], &mut packets);
        feed_chunk(&mut stream, &wire[split..], &mut packets);
        assert_eq!(packets, expected);
        stream.finish().unwrap();
        assert_eq!(stream.state(), StreamState::Closed);
    }
    let mut stream = stream(65535);
    let mut packets = Vec::new();
    for byte in wire.chunks(1) {
        feed_chunk(&mut stream, byte, &mut packets);
    }
    assert_eq!(packets, expected);
    stream.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn feed_yields_one_packet_and_consumes_exactly_its_boundary() {
    let expected = server_packets();
    let frames = server_frames(&expected);
    let wire = frames.concat();
    let mut stream = stream(65535);
    let mut consumed = 0;
    for (packet, frame) in expected.into_iter().zip(frames) {
        let result = stream.feed(&wire[consumed..]).unwrap();
        assert_eq!(result.consumed, frame.len());
        assert_eq!(result.item, Some(packet));
        consumed += result.consumed;
    }
    assert_eq!(consumed, wire.len());
    stream.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn eof_rejects_every_partial_smart_opcode_length_prefix_and_payload() {
    for packet in server_packets() {
        let wire = server_frames(&[packet])[0].clone();
        for cut in 1..wire.len() {
            let mut stream = stream(65535);
            queue_ping(&mut stream);
            let result = stream.feed(&wire[..cut]).unwrap();
            assert_eq!(result.consumed, cut);
            assert!(result.item.is_none());
            expect_error(
                stream.finish(),
                StreamError::Protocol(ProtocolError::UnexpectedEof),
            );
            assert_eq!(stream.state(), StreamState::Failed);
            assert_unusable(&mut stream, StreamError::Failed);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn clean_eof_is_terminal_and_clears_the_pending_ping() {
    for with_packet in [false, true] {
        let mut stream = stream(65535);
        queue_ping(&mut stream);
        if with_packet {
            let frames = server_frames(&[GamePacket {
                opcode: ServerPacket::PING,
                payload: vec![],
            }]);
            assert!(stream.feed(&frames[0]).unwrap().item.is_some());
        }
        stream.finish().unwrap();
        assert_eq!(stream.state(), StreamState::Closed);
        assert_unusable(&mut stream, StreamError::Closed);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unknown_incoming_opcode_is_fatal_and_clears_all_stream_state() {
    let mut encoder = PacketEncoder::new(Some(fake_ciphers().incoming), 65535).unwrap();
    let wire = encoder.encode(32767, PacketSize::Fixed(0), &[]).unwrap();
    assert!(ServerPacket::from_wire(32767).is_none());
    let mut stream = stream(65535);
    queue_ping(&mut stream);
    expect_error(
        stream.feed(&wire),
        StreamError::Protocol(ProtocolError::UnknownOpcode(32767)),
    );
    assert_eq!(stream.state(), StreamState::Failed);
    assert_unusable(&mut stream, StreamError::Failed);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn oversized_incoming_length_is_rejected_before_payload_and_is_fatal() {
    let packets = [GamePacket {
        opcode: ServerPacket::WORLD_LIST,
        payload: vec![0x55; 5],
    }];
    let frames = server_frames(&packets);
    let mut stream = stream(4);
    queue_ping(&mut stream);
    expect_error(
        stream.feed(&frames[0][..3]),
        StreamError::Protocol(ProtocolError::LengthLimitExceeded {
            length: 5,
            limit: 4,
        }),
    );
    assert_eq!(stream.state(), StreamState::Failed);
    assert_unusable(&mut stream, StreamError::Failed);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_outgoing_payloads_do_not_advance_isaac_or_poison_the_stream() {
    let mut stream = stream(65535);
    let mut expected = fake_ciphers().outgoing;
    expect_error(
        stream.encode(ClientPacket::PING, &[0x55]),
        StreamError::Protocol(ProtocolError::PayloadLengthMismatch {
            expected: 0,
            actual: 1,
        }),
    );
    expect_error(
        stream.encode(ClientPacket::REQUEST_WORLD_LIST, &[0x55; 3]),
        StreamError::Protocol(ProtocolError::PayloadLengthMismatch {
            expected: 4,
            actual: 3,
        }),
    );
    expect_error(
        stream.encode(ClientPacket::REQUEST_WORLD_LIST, &vec![0x55; 65536]),
        StreamError::Protocol(ProtocolError::LengthLimitExceeded {
            length: 65536,
            limit: 65535,
        }),
    );
    assert_eq!(stream.state(), StreamState::Open);
    assert_eq!(queue_ping(&mut stream), [expected.next_u32() as u8]);
    stream.mark_flushed().unwrap();
    assert_eq!(
        stream.encode(ClientPacket::PING, &[]).unwrap(),
        [expected.next_u32() as u8]
    );
    let mut limited = LobbyStream::new(fake_ciphers(), 3).unwrap();
    expect_error(
        limited.request_world_list(0),
        StreamError::Protocol(ProtocolError::LengthLimitExceeded {
            length: 4,
            limit: 3,
        }),
    );
    assert_eq!(limited.state(), StreamState::Open);
    assert_eq!(
        queue_ping(&mut limited),
        [fake_ciphers().outgoing.next_u32() as u8]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn explicit_close_drops_pending_and_partial_packets_and_is_idempotent() {
    let frames = server_frames(&[GamePacket {
        opcode: ServerPacket::WORLD_LIST,
        payload: vec![0x55; 7],
    }]);
    let mut stream = stream(65535);
    queue_ping(&mut stream);
    let result = stream.feed(&frames[0][..4]).unwrap();
    assert_eq!(result.consumed, 4);
    assert!(result.item.is_none());
    stream.close();
    assert_eq!(stream.state(), StreamState::Closed);
    assert_unusable(&mut stream, StreamError::Closed);
    stream.close();
    assert_eq!(stream.state(), StreamState::Closed);
    assert_unusable(&mut stream, StreamError::Closed);
}
