use openrs_protocol::framing::{Packet, PacketDecoder, PacketEncoder, PacketSize};
use openrs_protocol::isaac::Isaac;
use openrs_protocol::Error;

const OPCODES: [u16; 7] = [0, 127, 128, 255, 256, 32767, 5];
const KOTLIN_PACKETS: &[u8] = include_bytes!("fixtures/packets.wire");

fn sizes() -> Vec<Option<PacketSize>> {
    let mut sizes = vec![None; 32768];
    for (index, opcode) in OPCODES.iter().enumerate() {
        sizes[*opcode as usize] = Some(match index % 3 {
            0 => PacketSize::Fixed(2),
            1 => PacketSize::VarByte,
            _ => PacketSize::VarShort,
        });
    }
    sizes
}

fn packets() -> Vec<Packet> {
    OPCODES
        .iter()
        .enumerate()
        .map(|(index, opcode)| Packet {
            opcode: *opcode,
            payload: match index % 3 {
                0 => vec![0x11, 0x22],
                1 => vec![0x80, 0, 0x7f],
                _ => vec![0xff],
            },
        })
        .collect()
}

fn decode_chunks(decoder: &mut PacketDecoder<'_>, bytes: &[u8], chunk_size: usize) -> Vec<Packet> {
    let mut packets = vec![];
    for chunk in bytes.chunks(chunk_size) {
        let mut remaining = chunk;
        while !remaining.is_empty() {
            let result = decoder.feed(remaining).unwrap();
            assert!(result.consumed > 0);
            remaining = &remaining[result.consumed..];
            if let Some(packet) = result.item {
                packets.push(packet);
            }
        }
    }
    packets
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_packet_fixture_decodes_at_every_chunk_size() {
    let sizes = sizes();
    for chunk_size in 1..=KOTLIN_PACKETS.len() {
        let mut decoder =
            PacketDecoder::new(Some(Isaac::new(&[1, 2, 3, 4]).unwrap()), &sizes, 65535).unwrap();
        assert_eq!(
            decode_chunks(&mut decoder, KOTLIN_PACKETS, chunk_size),
            packets()
        );
        assert_eq!(decoder.feed(&[]).unwrap().item, None);
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn packet_encoder_matches_kotlin_smart_opcodes_and_unencrypted_length_prefixes() {
    let sizes = sizes();
    let mut encoder = PacketEncoder::new(Some(Isaac::new(&[1, 2, 3, 4]).unwrap()), 65535).unwrap();
    let mut bytes = vec![];
    for packet in packets() {
        bytes.extend(
            encoder
                .encode(
                    packet.opcode,
                    sizes[packet.opcode as usize].unwrap(),
                    &packet.payload,
                )
                .unwrap(),
        );
    }
    assert_eq!(bytes, KOTLIN_PACKETS);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn plaintext_opcode_boundaries_and_variable_sizes_match_fixed_bytes() {
    let mut encoder = PacketEncoder::new(None, 65535).unwrap();
    for (opcode, expected) in [
        (0, vec![0]),
        (127, vec![127]),
        (128, vec![128, 128]),
        (255, vec![128, 255]),
        (256, vec![129, 0]),
        (32767, vec![255, 255]),
    ] {
        assert_eq!(
            encoder.encode(opcode, PacketSize::Fixed(0), &[]).unwrap(),
            expected
        );
    }
    assert_eq!(
        encoder.encode(128, PacketSize::VarByte, &[255, 0]).unwrap(),
        [128, 128, 2, 255, 0]
    );
    assert_eq!(
        encoder
            .encode(127, PacketSize::VarShort, &[255, 0])
            .unwrap(),
        [127, 0, 2, 255, 0]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_encoding_does_not_advance_the_cipher() {
    let mut encoder = PacketEncoder::new(Some(Isaac::new(&[1, 2, 3, 4]).unwrap()), 65535).unwrap();
    assert_eq!(
        encoder.encode(32768, PacketSize::Fixed(0), &[]),
        Err(Error::InvalidOpcode(32768))
    );
    assert_eq!(
        encoder.encode(0, PacketSize::Fixed(2), &[1]),
        Err(Error::PayloadLengthMismatch {
            expected: 2,
            actual: 1
        })
    );
    assert_eq!(
        encoder.encode(0, PacketSize::VarByte, &[0; 256]),
        Err(Error::LengthLimitExceeded {
            length: 256,
            limit: 255
        })
    );
    assert_eq!(
        encoder.encode(0, PacketSize::VarShort, &vec![0; 65536]),
        Err(Error::LengthLimitExceeded {
            length: 65536,
            limit: 65535
        })
    );
    assert_eq!(
        encoder
            .encode(0, PacketSize::Fixed(2), &[0x11, 0x22])
            .unwrap(),
        &KOTLIN_PACKETS[..3]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn unknown_opcode_is_terminal_without_attempting_to_guess_a_length() {
    let mut decoder = PacketDecoder::new(None, &[None; 10], 100).unwrap();
    assert_eq!(decoder.feed(&[5]), Err(Error::UnknownOpcode(5)));
    assert_eq!(decoder.feed(&[0]), Err(Error::DecoderFailed));
    assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
    let mut decoder = PacketDecoder::new(None, &[], 100).unwrap();
    assert_eq!(decoder.feed(&[255]).unwrap().item, None);
    assert_eq!(decoder.feed(&[255]), Err(Error::UnknownOpcode(32767)));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn payload_limits_are_checked_for_fixed_and_variable_packets() {
    for (size, bytes) in [
        (PacketSize::Fixed(11), vec![0]),
        (PacketSize::VarByte, vec![0, 11]),
        (PacketSize::VarShort, vec![0, 0, 11]),
    ] {
        let sizes = [Some(size)];
        let mut decoder = PacketDecoder::new(None, &sizes, 10).unwrap();
        assert_eq!(
            decoder.feed(&bytes),
            Err(Error::LengthLimitExceeded {
                length: 11,
                limit: 10
            })
        );
        assert_eq!(decoder.feed(&[]), Err(Error::DecoderFailed));
    }
    let mut encoder = PacketEncoder::new(None, 0).unwrap();
    assert_eq!(
        encoder.encode(0, PacketSize::Fixed(1), &[0]),
        Err(Error::LengthLimitExceeded {
            length: 1,
            limit: 0
        })
    );
    assert!(matches!(
        PacketEncoder::new(None, usize::MAX),
        Err(Error::InvalidLimit)
    ));
    assert!(matches!(
        PacketDecoder::new(None, &[], usize::MAX),
        Err(Error::InvalidLimit)
    ));
    assert!(matches!(
        PacketDecoder::new(None, &vec![None; 32769], 10),
        Err(Error::InvalidLimit)
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn zero_length_packets_complete_without_reading_the_next_opcode() {
    let sizes = [
        Some(PacketSize::Fixed(0)),
        Some(PacketSize::VarByte),
        Some(PacketSize::VarShort),
    ];
    let bytes = [0, 1, 0, 2, 0, 0, 0];
    for chunk_size in 1..=bytes.len() {
        let mut decoder = PacketDecoder::new(None, &sizes, 0).unwrap();
        let packets = decode_chunks(&mut decoder, &bytes, chunk_size);
        assert_eq!(
            packets.iter().map(|p| p.opcode).collect::<Vec<_>>(),
            [0, 1, 2, 0]
        );
        assert!(packets.iter().all(|p| p.payload.is_empty()));
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn truncated_opcode_prefix_and_payload_are_distinguished_from_a_clean_boundary() {
    let sizes = sizes();
    let mut decoder = PacketDecoder::new(None, &sizes, 65535).unwrap();
    decoder.finish().unwrap();
    for bytes in [
        vec![128],
        vec![128, 128],
        vec![128, 128, 0],
        vec![128, 128, 0, 1],
        vec![127],
        vec![127, 2],
        vec![127, 2, 9],
        vec![0, 9],
    ] {
        let mut decoder = PacketDecoder::new(None, &sizes, 65535).unwrap();
        assert_eq!(decoder.feed(&bytes).unwrap().item, None);
        assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
        assert_eq!(decoder.feed(&[0]), Err(Error::DecoderFailed));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn length_prefixes_support_their_full_unsigned_ranges() {
    for (size, length) in [(PacketSize::VarByte, 255), (PacketSize::VarShort, 65535)] {
        let sizes = [Some(size)];
        let payload = (0..length).map(|i| i as u8).collect::<Vec<_>>();
        let bytes = PacketEncoder::new(None, length)
            .unwrap()
            .encode(0, size, &payload)
            .unwrap();
        let mut decoder = PacketDecoder::new(None, &sizes, length).unwrap();
        assert_eq!(
            decode_chunks(&mut decoder, &bytes, 7),
            [Packet { opcode: 0, payload }]
        );
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn opcode_cipher_remains_aligned_over_multiple_isaac_refills() {
    let sizes = [Some(PacketSize::VarByte)];
    let mut encoder =
        PacketEncoder::new(Some(Isaac::new(&[u32::MAX, 2, 3, 4]).unwrap()), 255).unwrap();
    let mut bytes = vec![];
    let expected = (0..1000)
        .map(|i| Packet {
            opcode: 0,
            payload: vec![i as u8; i % 11],
        })
        .collect::<Vec<_>>();
    for packet in &expected {
        bytes.extend(
            encoder
                .encode(0, PacketSize::VarByte, &packet.payload)
                .unwrap(),
        );
    }
    let mut decoder =
        PacketDecoder::new(Some(Isaac::new(&[u32::MAX, 2, 3, 4]).unwrap()), &sizes, 255).unwrap();
    assert_eq!(decode_chunks(&mut decoder, &bytes, 3), expected);
    decoder.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn arbitrary_small_frames_do_not_panic() {
    let sizes = (0..256)
        .map(|i| {
            Some(match i % 3 {
                0 => PacketSize::Fixed(i % 17),
                1 => PacketSize::VarByte,
                _ => PacketSize::VarShort,
            })
        })
        .collect::<Vec<_>>();
    for length in 0..300 {
        let bytes = (0..length)
            .map(|i| (i * 29 + length * 13) as u8)
            .collect::<Vec<_>>();
        let mut decoder = PacketDecoder::new(None, &sizes, 1024).unwrap();
        let _ = decoder.feed(&bytes);
        let _ = decoder.finish();
    }
}
