#[path = "../packet_source.rs"]
mod packet_source;

use openrs_protocol::framing::{PacketDecoder, PacketEncoder, PacketSize};
use openrs_protocol::isaac::Isaac;
use openrs_protocol::registry::{
    ClientPacket, Direction, GameOpcode, GamePacket, PacketIdentity, PacketMetadata,
    RegistryDecoder, RegistryEncoder, ServerPacket, CLIENT_PACKETS, CLIENT_PACKET_SIZES,
    SERVER_PACKETS, SERVER_PACKET_SIZES,
};
use openrs_protocol::Error;
use packet_source::{NativeDirection, SourcePacket};
use std::{collections::BTreeSet, fmt::Write};

const CLIENT_SOURCE: &str = include_str!(
    "../../client/src/main/kotlin/com/jagex/game/runetek5/network/packet/ClientProt.kt"
);
const SERVER_SOURCE: &str = include_str!(
    "../../client/src/main/kotlin/com/jagex/game/runetek5/network/packet/ServerProt.kt"
);
const CLIENT_WIRE: &[u8] = include_bytes!("fixtures/game-client.wire");
const SERVER_WIRE: &[u8] = include_bytes!("fixtures/game-server.wire");
const CLIENT_FRAMES: &str = include_str!("fixtures/game-client.frames.tsv");
const SERVER_FRAMES: &str = include_str!("fixtures/game-server.frames.tsv");

fn cipher() -> Option<Isaac> {
    Some(Isaac::new(&[1, 0xffff_fffe, 0x1234_5678, 0x8000_0000]).unwrap())
}

fn check_registry<O: GameOpcode>(
    table: &[PacketMetadata<O>],
    parsed: &[SourcePacket],
    native: &str,
) {
    assert_eq!(table.len(), parsed.len());
    let mut actual = String::new();
    let mut wires = BTreeSet::new();
    for (index, (entry, source)) in table.iter().zip(parsed).enumerate() {
        assert!(wires.insert(entry.packet.wire()));
        assert_eq!(entry.packet.wire(), source.wire);
        assert_eq!(entry.name, source.name);
        assert_eq!(entry.native_size, source.size);
        assert_eq!(entry.tick_processed, source.tick_processed);
        assert_eq!(entry.packet.metadata(), entry);
        assert_eq!(O::from_wire(source.wire), Some(entry.packet));
        let size = match source.size {
            -1 => PacketSize::VarByte,
            -2 => PacketSize::VarShort,
            size => PacketSize::Fixed(usize::try_from(size).unwrap()),
        };
        assert_eq!(entry.size, size);
        let identity = if entry.name.starts_with("UNK_") {
            PacketIdentity::UnsupportedUnknown
        } else {
            PacketIdentity::Named
        };
        assert_eq!(entry.identity, identity);
        writeln!(
            actual,
            "{index}\t{}\t{}\t{}\t{}",
            entry.packet.wire(),
            entry.name,
            entry.native_size,
            entry
                .tick_processed
                .map_or_else(|| "-".into(), |flag| flag.to_string())
        )
        .unwrap();
    }
    assert_eq!(actual, native);
    for wire in 0..=u16::MAX {
        let metadata = table.iter().find(|entry| entry.packet.wire() == wire);
        assert_eq!(O::from_wire(wire), metadata.map(|entry| entry.packet));
        assert_eq!(
            O::sizes().get(usize::from(wire)).copied().flatten(),
            metadata.map(|entry| entry.size)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn generated_registry_matches_all_native_entries_and_current_sources() {
    assert_eq!(CLIENT_PACKETS.len(), 104);
    assert_eq!(SERVER_PACKETS.len(), 161);
    assert_eq!(ClientPacket::DIRECTION, Direction::ClientToServer);
    assert_eq!(ServerPacket::DIRECTION, Direction::ServerToClient);
    check_registry(
        &CLIENT_PACKETS,
        &packet_source::parse(CLIENT_SOURCE, NativeDirection::Client).unwrap(),
        include_str!("fixtures/game-client.tsv"),
    );
    check_registry(
        &SERVER_PACKETS,
        &packet_source::parse(SERVER_SOURCE, NativeDirection::Server).unwrap(),
        include_str!("fixtures/game-server.tsv"),
    );
    assert_eq!(ClientPacket::sizes(), CLIENT_PACKET_SIZES);
    assert_eq!(ServerPacket::sizes(), SERVER_PACKET_SIZES);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn generator_rejects_signature_size_flag_duplicate_and_nonliteral_drift() {
    for source in [
        CLIENT_SOURCE.replace(
            "tickProcessed: Boolean = false",
            "tickProcessed: Boolean = true",
        ),
        CLIENT_SOURCE.replace("@JvmField val id: Int", "@JvmField val id: Long"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, -3)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(256, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(32768, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(-1, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, 2147483648)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(1, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PLAYER_OP6(0, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(computed(), 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, computed())"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, 0, maybe)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, 0, false, true)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "Ping(0, 0)"),
        CLIENT_SOURCE.replace("PING(0, 0)", "PING(0, 0"),
        CLIENT_SOURCE.replace("PLAYER_OP5(103, 3, true);", "PLAYER_OP5(103, 3, true),"),
    ] {
        assert!(packet_source::parse(&source, NativeDirection::Client).is_err());
    }
    for source in [
        SERVER_SOURCE.replace("val opcode: Int", "val opcode: Long"),
        SERVER_SOURCE.replace("IF_SETPLAYERHEAD(0, 4)", "IF_SETPLAYERHEAD(0, -3)"),
        SERVER_SOURCE.replace("IF_SETPLAYERHEAD(0, 4)", "IF_SETPLAYERHEAD(1, 4)"),
        SERVER_SOURCE.replace("IF_SETPLAYERHEAD(0, 4)", "IF_SETPLAYERHEAD(0, 4, false)"),
        SERVER_SOURCE.replace("IF_SETPLAYERHEAD(0, 4)", "IF_SETPLAYERHEAD(0, size = 4)"),
        SERVER_SOURCE.replace("GAME_MESSAGE(160, -1);", "GAME_MESSAGE(160, -1),"),
    ] {
        assert!(packet_source::parse(&source, NativeDirection::Server).is_err());
    }
    assert!(packet_source::parse(CLIENT_SOURCE, NativeDirection::Server).is_err());
    assert!(packet_source::parse(SERVER_SOURCE, NativeDirection::Client).is_err());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn generator_uses_explicit_wire_ids_not_enum_ordinals() {
    let header = SERVER_SOURCE.split_once(") {").unwrap().0;
    let source = format!("{header}) {{\nLAST(32767, -2),\nFIRST(0, 0),\nMIDDLE(128, -1);\n}}");
    let entries = packet_source::parse(&source, NativeDirection::Server).unwrap();
    assert_eq!(
        entries.iter().map(|entry| entry.wire).collect::<Vec<_>>(),
        [32767, 0, 128]
    );
    assert_eq!(
        entries.iter().map(|entry| entry.size).collect::<Vec<_>>(),
        [-2, 0, -1]
    );
    assert!(entries.iter().all(|entry| entry.tick_processed.is_none()));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unresolved_native_identities_are_explicit_but_can_be_framed_opaquely() {
    let unknown: Vec<_> = CLIENT_PACKETS
        .iter()
        .filter(|entry| entry.identity == PacketIdentity::UnsupportedUnknown)
        .map(|entry| entry.packet)
        .collect();
    assert_eq!(unknown, [ClientPacket::UNK_63, ClientPacket::UNK_82]);
    assert!(SERVER_PACKETS
        .iter()
        .all(|entry| entry.identity == PacketIdentity::Named));
    let mut encoder = RegistryEncoder::<ClientPacket>::new(cipher(), 4).unwrap();
    let mut decoder = RegistryDecoder::<ClientPacket>::new(cipher(), 4).unwrap();
    for opcode in unknown {
        assert_eq!(opcode.metadata().native_size, 4);
        assert_eq!(opcode.metadata().size, PacketSize::Fixed(4));
        let payload = vec![0x80, 0, 0xff, 1];
        let bytes = encoder.encode(opcode, &payload).unwrap();
        assert_eq!(
            decoder.feed(&bytes).unwrap().item,
            Some(GamePacket { opcode, payload })
        );
    }
    decoder.finish().unwrap();
    assert_eq!(ClientPacket::from_wire(104), None);
    assert_eq!(ServerPacket::from_wire(161), None);
    assert_eq!(
        ClientPacket::DEPRECATED_97_SERVERPACKET_52.metadata().name,
        "DEPRECATED_97_SERVERPACKET_52"
    );
    assert_eq!(
        ServerPacket::DEPRECATED_52_CLIENTPACKET_97.metadata().name,
        "DEPRECATED_52_CLIENTPACKET_97"
    );
}

fn oracle_packets<O: GameOpcode>(manifest: &str, count: usize) -> Vec<GamePacket<O>> {
    let packets: Vec<_> = manifest
        .lines()
        .enumerate()
        .map(|(index, line)| {
            let fields: Vec<_> = line.split('\t').collect();
            assert_eq!(fields.len(), 3);
            assert_eq!(fields[0].parse::<usize>().unwrap(), index);
            let opcode = O::from_wire(fields[1].parse().unwrap()).unwrap();
            let length = fields[2].parse::<usize>().unwrap();
            let round = index / count;
            let ordinal = index % count;
            GamePacket {
                opcode,
                payload: (0..length)
                    .map(|offset| (round * 37 + ordinal * 13 + offset * 19) as u8)
                    .collect(),
            }
        })
        .collect();
    assert_eq!(packets.len(), 4 * count);
    packets
}

fn decode_chunks<O: GameOpcode>(
    decoder: &mut RegistryDecoder<O>,
    bytes: &[u8],
    chunk_size: usize,
) -> Vec<GamePacket<O>> {
    let mut packets = Vec::new();
    for chunk in bytes.chunks(chunk_size) {
        let mut remaining = chunk;
        while !remaining.is_empty() {
            let empty = decoder.feed(&[]).unwrap();
            assert_eq!(empty.consumed, 0);
            assert_eq!(empty.item, None);
            let result = decoder.feed(remaining).unwrap();
            assert!(result.consumed > 0 && result.consumed <= remaining.len());
            remaining = &remaining[result.consumed..];
            if let Some(packet) = result.item {
                packets.push(packet);
            }
        }
    }
    packets
}

fn check_native_sequence<O: GameOpcode>(manifest: &str, count: usize, wire: &[u8]) {
    let packets = oracle_packets::<O>(manifest, count);
    assert!(packets.iter().any(|packet| packet.payload.len() == 255));
    assert!(packets.iter().any(|packet| packet.payload.len() == 65535));
    let cipher_words: usize = packets
        .iter()
        .map(|packet| if packet.opcode.wire() >= 128 { 2 } else { 1 })
        .sum();
    assert!(cipher_words > 256);
    let mut encoder = RegistryEncoder::<O>::new(cipher(), 65535).unwrap();
    let mut actual = Vec::new();
    for packet in &packets {
        actual.extend(encoder.encode(packet.opcode, &packet.payload).unwrap());
    }
    assert_eq!(actual, wire);
    for chunk_size in [1, 2, 3, 7, 31, 255, 256, 257, 1024, wire.len()] {
        let mut decoder = RegistryDecoder::<O>::new(cipher(), 65535).unwrap();
        assert_eq!(decode_chunks(&mut decoder, wire, chunk_size), packets);
        assert_eq!(decoder.feed(&[]).unwrap().item, None);
        decoder.finish().unwrap();
    }
    let mut raw = PacketDecoder::new(cipher(), O::sizes(), 65535).unwrap();
    let mut remaining = wire;
    for packet in &packets {
        let result = raw.feed(remaining).unwrap();
        remaining = &remaining[result.consumed..];
        let actual = result.item.unwrap();
        assert_eq!(actual.opcode, packet.opcode.wire());
        assert_eq!(actual.payload, packet.payload);
    }
    assert!(remaining.is_empty());
    raw.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn client_registry_frames_match_native_jar_across_refills_and_fragmentation() {
    check_native_sequence::<ClientPacket>(CLIENT_FRAMES, CLIENT_PACKETS.len(), CLIENT_WIRE);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn server_registry_smart_frames_match_native_jar_across_refills_and_fragmentation() {
    assert_eq!(ServerPacket::MIDI_SONG_LOCATION.wire(), 128);
    assert_eq!(ServerPacket::GAME_MESSAGE.wire(), 160);
    check_native_sequence::<ServerPacket>(SERVER_FRAMES, SERVER_PACKETS.len(), SERVER_WIRE);
}

fn check_unknown<O: GameOpcode>(wire: &[u8], suffix: &[u8], unknown: u16, count: usize) {
    let mut decoder = RegistryDecoder::<O>::new(cipher(), 65535).unwrap();
    assert_eq!(decode_chunks(&mut decoder, wire, 13).len(), 4 * count);
    if unknown >= 128 {
        let result = decoder.feed(&suffix[..1]).unwrap();
        assert_eq!(result.consumed, 1);
        assert_eq!(result.item, None);
        assert_eq!(decoder.feed(&[]).unwrap().item, None);
        assert_eq!(
            decoder.feed(&suffix[1..]),
            Err(Error::UnknownOpcode(unknown))
        );
    } else {
        assert_eq!(decoder.feed(suffix), Err(Error::UnknownOpcode(unknown)));
    }
    assert_eq!(decoder.feed(&[]), Err(Error::DecoderFailed));
    assert_eq!(decoder.feed(wire), Err(Error::DecoderFailed));
    assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_unknown_opcode_suffixes_are_terminal_after_isaac_refills() {
    check_unknown::<ClientPacket>(
        CLIENT_WIRE,
        include_bytes!("fixtures/game-client.unknown.wire"),
        104,
        CLIENT_PACKETS.len(),
    );
    check_unknown::<ServerPacket>(
        SERVER_WIRE,
        include_bytes!("fixtures/game-server.unknown.wire"),
        161,
        SERVER_PACKETS.len(),
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn direction_specific_lookup_never_falls_back_to_the_other_registry() {
    assert_eq!(
        ClientPacket::PING.wire(),
        ServerPacket::IF_SETPLAYERHEAD.wire()
    );
    assert_eq!(ClientPacket::PING.metadata().size, PacketSize::Fixed(0));
    assert_eq!(
        ServerPacket::IF_SETPLAYERHEAD.metadata().size,
        PacketSize::Fixed(4)
    );
    for unknown in 104..=255 {
        let mut decoder = RegistryDecoder::<ClientPacket>::new(None, 65535).unwrap();
        assert_eq!(
            decoder.feed(&[unknown as u8, 0]),
            Err(Error::UnknownOpcode(unknown))
        );
    }
    for unknown in [161, 255, 256, 32767] {
        let wire = PacketEncoder::new(None, 0)
            .unwrap()
            .encode(unknown, PacketSize::Fixed(0), &[])
            .unwrap();
        let mut decoder = RegistryDecoder::<ServerPacket>::new(None, 65535).unwrap();
        assert_eq!(decoder.feed(&wire), Err(Error::UnknownOpcode(unknown)));
    }
    let wire = RegistryEncoder::<ServerPacket>::new(None, 6)
        .unwrap()
        .encode(ServerPacket::MIDI_SONG_LOCATION, &[0; 6])
        .unwrap();
    let mut decoder = RegistryDecoder::<ClientPacket>::new(None, 6).unwrap();
    assert_eq!(decoder.feed(&wire[..1]), Err(Error::UnknownOpcode(128)));
    assert_eq!(decoder.feed(&wire[1..]), Err(Error::DecoderFailed));
    let mut decoder = RegistryDecoder::<ServerPacket>::new(None, 6).unwrap();
    assert_eq!(
        decoder.feed(&wire).unwrap().item.unwrap().opcode,
        ServerPacket::MIDI_SONG_LOCATION
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn client_high_opcode_is_one_unknown_byte_not_a_smart_alias_for_ping() {
    let native = include_bytes!("fixtures/game-client.high-unknown.wire");
    for (encrypted, wire) in [(false, &[128, 0][..]), (true, &native[..])] {
        for chunk_size in [1, 2] {
            let mut decoder =
                RegistryDecoder::<ClientPacket>::new(if encrypted { cipher() } else { None }, 0)
                    .unwrap();
            assert_eq!(
                decoder.feed(&wire[..chunk_size]),
                Err(Error::UnknownOpcode(128))
            );
            assert_eq!(decoder.feed(&wire[chunk_size..]), Err(Error::DecoderFailed));
            assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
        }
    }
    let mut generic = PacketDecoder::new(None, &CLIENT_PACKET_SIZES, 0).unwrap();
    assert_eq!(generic.feed(&[128, 0]).unwrap().item.unwrap().opcode, 0);
}

fn check_encoder_failures<O: GameOpcode>(table: &[PacketMetadata<O>]) {
    let fixed = table
        .iter()
        .find(|entry| matches!(entry.size, PacketSize::Fixed(n) if n > 0))
        .unwrap();
    let byte = table
        .iter()
        .find(|entry| entry.size == PacketSize::VarByte)
        .unwrap();
    let short = table
        .iter()
        .find(|entry| entry.size == PacketSize::VarShort)
        .unwrap();
    let mut encoder = RegistryEncoder::<O>::new(cipher(), 65536).unwrap();
    let PacketSize::Fixed(expected) = fixed.size else {
        unreachable!()
    };
    assert_eq!(
        encoder.encode(fixed.packet, &[]),
        Err(Error::PayloadLengthMismatch {
            expected,
            actual: 0
        })
    );
    assert_eq!(
        encoder.encode(byte.packet, &[0; 256]),
        Err(Error::LengthLimitExceeded {
            length: 256,
            limit: 255
        })
    );
    assert_eq!(
        encoder.encode(short.packet, &vec![0; 65536]),
        Err(Error::LengthLimitExceeded {
            length: 65536,
            limit: 65535
        })
    );
    let payload = vec![0xab; expected];
    let mut fresh = RegistryEncoder::<O>::new(cipher(), 65536).unwrap();
    assert_eq!(
        encoder.encode(fixed.packet, &payload).unwrap(),
        fresh.encode(fixed.packet, &payload).unwrap()
    );
    let mut limited = RegistryEncoder::<O>::new(cipher(), expected - 1).unwrap();
    assert_eq!(
        limited.encode(fixed.packet, &payload),
        Err(Error::LengthLimitExceeded {
            length: expected,
            limit: expected - 1
        })
    );
    let zero = table
        .iter()
        .find(|entry| entry.size == PacketSize::Fixed(0))
        .unwrap()
        .packet;
    let mut fresh = RegistryEncoder::<O>::new(cipher(), expected - 1).unwrap();
    assert_eq!(
        limited.encode(zero, &[]).unwrap(),
        fresh.encode(zero, &[]).unwrap()
    );
    assert!(matches!(
        RegistryEncoder::<O>::new(None, usize::MAX),
        Err(Error::InvalidLimit)
    ));
    assert!(matches!(
        RegistryDecoder::<O>::new(None, usize::MAX),
        Err(Error::InvalidLimit)
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn registry_encoding_validation_preserves_cipher_state_and_native_length_limits() {
    check_encoder_failures(&CLIENT_PACKETS);
    check_encoder_failures(&SERVER_PACKETS);
}

fn check_decoder_limits<O: GameOpcode>(table: &[PacketMetadata<O>]) {
    for (size, length) in [(PacketSize::VarByte, 255), (PacketSize::VarShort, 65535)] {
        let opcode = table
            .iter()
            .find(|entry| entry.size == size)
            .unwrap()
            .packet;
        let payload = vec![0; length];
        let wire = RegistryEncoder::<O>::new(cipher(), length)
            .unwrap()
            .encode(opcode, &payload)
            .unwrap();
        let prefix_length = wire.len() - length;
        let mut decoder = RegistryDecoder::<O>::new(cipher(), length - 1).unwrap();
        for byte in &wire[..prefix_length - 1] {
            assert_eq!(decoder.feed(&[*byte]).unwrap().item, None);
        }
        assert_eq!(
            decoder.feed(&wire[prefix_length - 1..prefix_length]),
            Err(Error::LengthLimitExceeded {
                length,
                limit: length - 1
            })
        );
        assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
        assert_eq!(
            decoder.feed(&wire[prefix_length..]),
            Err(Error::DecoderFailed)
        );
    }
    let entry = table
        .iter()
        .find(|entry| matches!(entry.size, PacketSize::Fixed(n) if n > 0))
        .unwrap();
    let PacketSize::Fixed(length) = entry.size else {
        unreachable!()
    };
    let wire = RegistryEncoder::<O>::new(None, length)
        .unwrap()
        .encode(entry.packet, &vec![0; length])
        .unwrap();
    let mut decoder = RegistryDecoder::<O>::new(None, length - 1).unwrap();
    let opcode_length = wire.len() - length;
    assert_eq!(
        decoder.feed(&wire[..opcode_length]),
        Err(Error::LengthLimitExceeded {
            length,
            limit: length - 1
        })
    );
    assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn registered_lengths_are_bounded_before_any_payload_is_fed() {
    check_decoder_limits(&CLIENT_PACKETS);
    check_decoder_limits(&SERVER_PACKETS);
}

fn check_eof<O: GameOpcode>(opcode: O, payload: &[u8]) {
    let wire = RegistryEncoder::<O>::new(cipher(), 65535)
        .unwrap()
        .encode(opcode, payload)
        .unwrap();
    for cut in 1..wire.len() {
        let mut decoder = RegistryDecoder::<O>::new(cipher(), 65535).unwrap();
        assert_eq!(decoder.feed(&wire[..cut]).unwrap().item, None);
        assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
        assert_eq!(decoder.feed(&wire[cut..]), Err(Error::DecoderFailed));
    }
    let mut decoder = RegistryDecoder::<O>::new(cipher(), 65535).unwrap();
    decoder.finish().unwrap();
    assert_eq!(
        decoder.feed(&wire).unwrap().item,
        Some(GamePacket {
            opcode,
            payload: payload.to_vec()
        })
    );
    decoder.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn registry_finish_rejects_truncated_opcodes_lengths_and_opaque_payloads() {
    check_eof(ClientPacket::CLIENT_FOCUS, &[1]);
    check_eof(ClientPacket::CHAT, &[1, 2, 3]);
    check_eof(ClientPacket::KEY_PRESS, &[0; 256]);
    check_eof(ServerPacket::MIDI_SONG_LOCATION, &[0; 6]);
    check_eof(ServerPacket::GAME_MESSAGE, &[1, 2, 3]);
    check_eof(ServerPacket::CLANSETTINGS_FULL, &[0; 256]);
}

fn check_empty<O: GameOpcode>(table: &[PacketMetadata<O>]) {
    let mut encoder = RegistryEncoder::<O>::new(cipher(), 0).unwrap();
    let mut wire = Vec::new();
    let expected: Vec<_> = [
        PacketSize::Fixed(0),
        PacketSize::VarByte,
        PacketSize::VarShort,
    ]
    .map(|size| {
        let opcode = table
            .iter()
            .find(|entry| entry.size == size)
            .unwrap()
            .packet;
        wire.extend(encoder.encode(opcode, &[]).unwrap());
        GamePacket {
            opcode,
            payload: vec![],
        }
    })
    .into();
    for chunk_size in 1..=wire.len() {
        let mut decoder = RegistryDecoder::<O>::new(cipher(), 0).unwrap();
        assert_eq!(decode_chunks(&mut decoder, &wire, chunk_size), expected);
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn empty_registered_frames_complete_without_consuming_the_next_opcode() {
    check_empty(&CLIENT_PACKETS);
    check_empty(&SERVER_PACKETS);
}

fn check_arbitrary<O: GameOpcode>() {
    for sample in 0..1000 {
        let bytes: Vec<_> = (0..sample % 300)
            .map(|index| (index * 29 + sample * 13) as u8)
            .collect();
        let mut decoder = RegistryDecoder::<O>::new(cipher(), 512).unwrap();
        let mut remaining = bytes.as_slice();
        while !remaining.is_empty() {
            match decoder.feed(remaining) {
                Ok(result) => {
                    assert!(result.consumed > 0 && result.consumed <= remaining.len());
                    remaining = &remaining[result.consumed..];
                }
                Err(_) => {
                    assert_eq!(decoder.feed(&[]), Err(Error::DecoderFailed));
                    break;
                }
            }
        }
        let _ = decoder.finish();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn arbitrary_registry_inputs_do_not_panic_or_spin() {
    check_arbitrary::<ClientPacket>();
    check_arbitrary::<ServerPacket>();
}
