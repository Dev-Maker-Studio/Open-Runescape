use openrs_login::isaac_ciphers;
use openrs_login::session::SessionCiphers;
use openrs_login::world_entry::{WorldEntryDecoder, WorldEntryError};
use openrs_protocol::Error as ProtocolError;
use openrs_protocol::framing::PacketSize;
use openrs_protocol::isaac::Isaac;
use openrs_protocol::registry::{
    ClientPacket, GamePacket, RegistryDecoder, RegistryEncoder, ServerPacket,
};

const SEED: [u32; 4] = [1, u32::MAX - 1, 0x12345678, 0x80000000];
const INCOMING: &[u8] = include_bytes!("fixtures/isaac-incoming.words");
const OUTGOING: &[u8] = include_bytes!("fixtures/isaac-outgoing.words");

fn native_word(fixture: &[u8], index: usize) -> u32 {
    u32::from_be_bytes(fixture[index * 4..index * 4 + 4].try_into().unwrap())
}

fn ciphers() -> SessionCiphers {
    let (outgoing, incoming) = isaac_ciphers(&SEED).unwrap();
    SessionCiphers { outgoing, incoming }
}

fn decoder(limit: usize) -> WorldEntryDecoder {
    WorldEntryDecoder::new(ciphers(), limit).unwrap()
}

fn expect_error<T>(result: Result<T, WorldEntryError>, error: WorldEntryError) {
    match result {
        Err(actual) => assert_eq!(actual, error),
        Ok(_) => panic!("invalid world entry operation succeeded"),
    }
}

fn native_opcode(wire: &mut Vec<u8>, opcode: u16, key: &mut usize) {
    assert!(opcode <= 32767);
    if opcode >= 128 {
        wire.push(((opcode >> 8) as u8 + 128).wrapping_add(native_word(INCOMING, *key) as u8));
        *key += 1;
    }
    wire.push((opcode as u8).wrapping_add(native_word(INCOMING, *key) as u8));
    *key += 1;
}

fn first_frame(opcode: u16, payload: &[u8]) -> Vec<u8> {
    let mut wire = Vec::new();
    native_opcode(&mut wire, opcode, &mut 0);
    wire.extend_from_slice(&u16::try_from(payload.len()).unwrap().to_be_bytes());
    wire.extend_from_slice(payload);
    wire
}

fn regular_frames(packets: &[GamePacket<ServerPacket>], mut key: usize) -> Vec<u8> {
    let mut wire = Vec::new();
    for packet in packets {
        native_opcode(&mut wire, packet.opcode.wire(), &mut key);
        match packet.opcode.metadata().size {
            PacketSize::Fixed(length) => assert_eq!(packet.payload.len(), length),
            PacketSize::VarByte => wire.push(u8::try_from(packet.payload.len()).unwrap()),
            PacketSize::VarShort => {
                wire.extend_from_slice(&u16::try_from(packet.payload.len()).unwrap().to_be_bytes());
            }
        }
        wire.extend_from_slice(&packet.payload);
    }
    wire
}

fn following_packets() -> Vec<GamePacket<ServerPacket>> {
    vec![
        GamePacket {
            opcode: ServerPacket::PING,
            payload: vec![],
        },
        GamePacket {
            opcode: ServerPacket::MIDI_SONG_LOCATION,
            payload: vec![0x80, 0, 0xff, 1, 2, 3],
        },
        GamePacket {
            opcode: ServerPacket::PROCESS_DEV_CONSOLE_COMMAND,
            payload: vec![0xff, 0x80, 0, 1],
        },
        GamePacket {
            opcode: ServerPacket::WORLD_LIST,
            payload: vec![0x55; 9],
        },
    ]
}

fn feed_regular(
    decoder: &mut RegistryDecoder<ServerPacket>,
    input: &[u8],
    packets: &mut Vec<GamePacket<ServerPacket>>,
) {
    let mut consumed = 0;
    while consumed < input.len() {
        let result = decoder.feed(&input[consumed..]).unwrap();
        assert!(result.consumed > 0);
        consumed += result.consumed;
        assert!(consumed <= input.len());
        if let Some(packet) = result.item {
            packets.push(packet);
        } else {
            assert_eq!(consumed, input.len());
        }
    }
}

fn assert_failed(decoder: &mut WorldEntryDecoder) {
    let error = WorldEntryError::Protocol(ProtocolError::DecoderFailed);
    expect_error(decoder.feed(&[]), error.clone());
    expect_error(decoder.feed(&[0; 8]), error.clone());
    expect_error(decoder.finish(), error);
    expect_error(decoder.take_ciphers(), WorldEntryError::CiphersUnavailable);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn limits_are_exactly_the_nonzero_native_u16_range() {
    for limit in [0, 65536, usize::MAX] {
        expect_error(
            WorldEntryDecoder::new(ciphers(), limit),
            ProtocolError::InvalidLimit.into(),
        );
    }
    for limit in [1, 65535] {
        let mut decoder = decoder(limit);
        let result = decoder.feed(&[]).unwrap();
        assert_eq!(result.consumed, 0);
        assert!(result.item.is_none());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn first_packet_always_has_plain_u16_length_even_for_fixed_and_varbyte_opcodes() {
    assert_eq!(ServerPacket::PING.metadata().size, PacketSize::Fixed(0));
    assert_eq!(
        ServerPacket::MIDI_SONG_LOCATION.metadata().size,
        PacketSize::Fixed(6)
    );
    assert_eq!(
        ServerPacket::PROCESS_DEV_CONSOLE_COMMAND.metadata().size,
        PacketSize::VarByte
    );
    for (opcode, length) in [
        (ServerPacket::PING, 257),
        (ServerPacket::MIDI_SONG_LOCATION, 3),
        (ServerPacket::PROCESS_DEV_CONSOLE_COMMAND, 300),
        (ServerPacket::DYNAMIC_MAP_REGION, 7),
    ] {
        let payload: Vec<_> = (0..length).map(|i| (i as u8).wrapping_mul(37)).collect();
        let first = first_frame(opcode.wire(), &payload);
        let mut wire = first.clone();
        wire.extend_from_slice(&[0x12, 0x34, 0xff, 0]);
        let mut decoder = decoder(65535);
        let result = decoder.feed(&wire).unwrap();
        assert_eq!(result.consumed, first.len());
        assert_eq!(result.item, Some(GamePacket { opcode, payload }));
        decoder.finish().unwrap();
        let mut ciphers = decoder.take_ciphers().unwrap();
        let keys = if opcode.wire() < 128 { 1 } else { 2 };
        assert_eq!(ciphers.incoming.next_u32(), native_word(INCOMING, keys));
        assert_eq!(ciphers.outgoing.next_u32(), native_word(OUTGOING, 0));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn zero_length_completes_at_header_without_consuming_suffix_or_using_registry_size() {
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        let first = first_frame(opcode.wire(), &[]);
        let mut wire = first.clone();
        wire.extend_from_slice(&[0xff; 16]);
        let mut decoder = decoder(1);
        let result = decoder.feed(&wire).unwrap();
        assert_eq!(result.consumed, first.len());
        assert_eq!(
            result.item,
            Some(GamePacket {
                opcode,
                payload: vec![]
            })
        );
        assert_eq!(first.len(), if opcode.wire() < 128 { 3 } else { 4 });
        decoder.finish().unwrap();
        decoder.take_ciphers().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_split_and_coalesced_suffix_resume_regular_framing_on_exact_incoming_keys() {
    let expected = following_packets();
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        for payload in [vec![], vec![0, 0x80, 0xff, 0x55, 0xaa, 0, 0x12]] {
            let first = first_frame(opcode.wire(), &payload);
            let suffix = regular_frames(&expected, if opcode.wire() < 128 { 1 } else { 2 });
            let wire = [first.as_slice(), suffix.as_slice()].concat();
            for split in 0..=wire.len() {
                let mut entry = decoder(64);
                let initial = entry.feed(&wire[..split]).unwrap();
                let (packet, chunks) = if let Some(packet) = initial.item {
                    assert_eq!(initial.consumed, first.len());
                    (packet, [&wire[first.len()..split], &wire[split..]])
                } else {
                    assert!(split < first.len());
                    assert_eq!(initial.consumed, split);
                    let final_feed = entry.feed(&wire[split..]).unwrap();
                    assert_eq!(final_feed.consumed, first.len() - split);
                    (final_feed.item.unwrap(), [&wire[first.len()..], &[][..]])
                };
                assert_eq!(
                    packet,
                    GamePacket {
                        opcode,
                        payload: payload.clone()
                    }
                );
                let transferred = entry.take_ciphers().unwrap();
                let mut regular = RegistryDecoder::new(Some(transferred.incoming), 64).unwrap();
                let mut packets = Vec::new();
                for chunk in chunks {
                    feed_regular(&mut regular, chunk, &mut packets);
                }
                assert_eq!(packets, expected);
                regular.finish().unwrap();
                let mut outgoing =
                    RegistryEncoder::<ClientPacket>::new(Some(transferred.outgoing), 64).unwrap();
                assert_eq!(
                    outgoing.encode(ClientPacket::PING, &[]).unwrap(),
                    [native_word(OUTGOING, 0) as u8]
                );
            }
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn bytewise_smart_header_repeated_empty_feeds_do_not_consume_peek_keys() {
    for advanced in [0, 255, 256, 511] {
        let mut ciphers = ciphers();
        for _ in 0..advanced {
            ciphers.incoming.next_u32();
        }
        let mut entry = WorldEntryDecoder::new(ciphers, 64).unwrap();
        let opcode = ServerPacket::MIDI_SONG_LOCATION;
        let payload = [0, 0xff, 0x80, 7, 9];
        let mut wire = Vec::new();
        let mut key = advanced;
        native_opcode(&mut wire, opcode.wire(), &mut key);
        wire.extend_from_slice(&(payload.len() as u16).to_be_bytes());
        wire.extend_from_slice(&payload);
        for (index, byte) in wire.iter().enumerate() {
            for _ in 0..4 {
                let result = entry.feed(&[]).unwrap();
                assert_eq!(result.consumed, 0);
                assert!(result.item.is_none());
                expect_error(entry.take_ciphers(), WorldEntryError::CiphersUnavailable);
            }
            let result = entry.feed(std::slice::from_ref(byte)).unwrap();
            assert_eq!(result.consumed, 1);
            if index + 1 == wire.len() {
                assert_eq!(
                    result.item,
                    Some(GamePacket {
                        opcode,
                        payload: payload.to_vec()
                    })
                );
            } else {
                assert!(result.item.is_none());
            }
        }
        let mut transferred = entry.take_ciphers().unwrap();
        assert_eq!(
            transferred.incoming.next_u32(),
            native_word(INCOMING, advanced + 2)
        );
        assert_eq!(transferred.outgoing.next_u32(), native_word(OUTGOING, 0));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_unsigned_u16_payload_is_bounded_and_fragmentable() {
    let payload: Vec<_> = (0..65535)
        .map(|i| (i as u8).wrapping_mul(19).wrapping_add(37))
        .collect();
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        let wire = first_frame(opcode.wire(), &payload);
        for chunk_size in [1, 2, 255, 257, 65535] {
            let mut entry = decoder(65535);
            let mut packet = None;
            let mut consumed = 0;
            for chunk in wire.chunks(chunk_size) {
                let result = entry.feed(chunk).unwrap();
                assert_eq!(result.consumed, chunk.len());
                consumed += result.consumed;
                if result.item.is_some() {
                    assert!(packet.is_none());
                    packet = result.item;
                }
            }
            assert_eq!(consumed, wire.len());
            assert_eq!(
                packet,
                Some(GamePacket {
                    opcode,
                    payload: payload.clone()
                })
            );
            entry.finish().unwrap();
            entry.take_ciphers().unwrap();
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn announced_over_limit_lengths_fail_at_header_without_accepting_payload() {
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        for (limit, length) in [(1, 2), (1, 65535), (256, 257), (65534, 65535)] {
            let mut wire = first_frame(opcode.wire(), &[]);
            let prefix = wire.len() - 2;
            wire[prefix..].copy_from_slice(&(length as u16).to_be_bytes());
            let mut entry = decoder(limit);
            expect_error(
                entry.feed(&wire),
                ProtocolError::LengthLimitExceeded { length, limit }.into(),
            );
            assert_failed(&mut entry);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unregistered_smart_opcodes_are_terminal_without_guessing_payload_or_registry_size() {
    for opcode in [161, 255, 256, 32767] {
        assert!(ServerPacket::from_wire(opcode).is_none());
        let mut entry = decoder(1);
        let mut header = first_frame(opcode, &[]);
        let prefix = header.len() - 2;
        header[prefix..].copy_from_slice(&65535u16.to_be_bytes());
        expect_error(
            entry.feed(&header),
            ProtocolError::UnknownOpcode(opcode).into(),
        );
        assert_failed(&mut entry);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn eof_at_every_incomplete_header_and_payload_prefix_is_terminal() {
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        for payload in [&[][..], &[0x80, 0xff, 0x55, 0, 1, 2][..]] {
            let wire = first_frame(opcode.wire(), payload);
            for length in 0..wire.len() {
                let mut entry = decoder(64);
                let result = entry.feed(&wire[..length]).unwrap();
                assert_eq!(result.consumed, length);
                assert!(result.item.is_none());
                expect_error(entry.finish(), ProtocolError::UnexpectedEof.into());
                assert_failed(&mut entry);
            }
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn completion_gates_cipher_transfer_once_and_never_accepts_more_entry_bytes() {
    for opcode in [ServerPacket::PING, ServerPacket::MIDI_SONG_LOCATION] {
        let wire = first_frame(opcode.wire(), &[0xff, 0, 7]);
        let mut entry = decoder(64);
        for byte in &wire {
            expect_error(entry.take_ciphers(), WorldEntryError::CiphersUnavailable);
            entry.feed(std::slice::from_ref(byte)).unwrap();
        }
        entry.finish().unwrap();
        entry.finish().unwrap();
        expect_error(entry.feed(&[]), ProtocolError::DecoderFinished.into());
        expect_error(
            entry.feed(&[0x55; 16]),
            ProtocolError::DecoderFinished.into(),
        );
        let mut transferred = entry.take_ciphers().unwrap();
        expect_error(entry.take_ciphers(), WorldEntryError::CiphersUnavailable);
        let offset = if opcode.wire() < 128 { 1 } else { 2 };
        for index in offset..INCOMING.len() / 4 {
            assert_eq!(
                transferred.incoming.next_u32(),
                native_word(INCOMING, index)
            );
        }
        let mut outgoing =
            RegistryEncoder::<ClientPacket>::new(Some(transferred.outgoing), 64).unwrap();
        for index in 0..OUTGOING.len() / 4 {
            assert_eq!(
                outgoing.encode(ClientPacket::PING, &[]).unwrap(),
                [native_word(OUTGOING, index) as u8]
            );
        }
        entry.finish().unwrap();
        expect_error(entry.feed(&wire), ProtocolError::DecoderFinished.into());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn existing_native_jar_game_stream_resumes_unchanged_after_first_unconditional_length() {
    let native_wire = include_bytes!("../../protocol/tests/fixtures/game-server.wire");
    let frames: Vec<_> = include_str!("../../protocol/tests/fixtures/game-server.frames.tsv")
        .lines()
        .map(|line| {
            let row: Vec<_> = line.split('\t').collect();
            (
                row[1].parse::<u16>().unwrap(),
                row[2].parse::<usize>().unwrap(),
            )
        })
        .collect();
    assert_eq!(frames[0], (0, 4));
    let mut wire = vec![native_wire[0], 0, 4];
    wire.extend_from_slice(&native_wire[1..]);
    for chunk_size in [1, 2, 17, 1024, wire.len()] {
        let mut entry = WorldEntryDecoder::new(
            SessionCiphers {
                outgoing: Isaac::new(&SEED).unwrap(),
                incoming: Isaac::new(&SEED).unwrap(),
            },
            65535,
        )
        .unwrap();
        let result = entry.feed(&wire).unwrap();
        assert_eq!(result.consumed, 7);
        let first = result.item.unwrap();
        assert_eq!(first.opcode.wire(), 0);
        assert_eq!(first.payload, native_wire[1..5]);
        let transferred = entry.take_ciphers().unwrap();
        let mut regular = RegistryDecoder::new(Some(transferred.incoming), 65535).unwrap();
        let mut packets = Vec::new();
        for chunk in wire[7..].chunks(chunk_size) {
            feed_regular(&mut regular, chunk, &mut packets);
        }
        assert_eq!(packets.len(), frames.len() - 1);
        for (index, (packet, &(opcode, length))) in packets.iter().zip(&frames[1..]).enumerate() {
            assert_eq!(packet.opcode.wire(), opcode);
            assert_eq!(packet.payload.len(), length);
            let round = (index + 1) / 161;
            let ordinal = (index + 1) % 161;
            let expected: Vec<_> = (0..length)
                .map(|i| (round * 37 + ordinal * 13 + i * 19) as u8)
                .collect();
            assert_eq!(packet.payload, expected);
        }
        regular.finish().unwrap();
    }
}
