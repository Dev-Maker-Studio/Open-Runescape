use openrs_protocol::js5::{
    self, BoundaryMode, HandshakeDecoder, HandshakeReply, Priority, Reply, ReplyDecoder,
};
use openrs_protocol::Error;

const PLAIN: &[u8] = include_bytes!("fixtures/plain1024.wire");
const PLAIN_CONTAINER: &[u8] = include_bytes!("fixtures/plain1024.container");
const COMPRESSED: &[u8] = include_bytes!("fixtures/compressed500.wire");
const COMPRESSED_CONTAINER: &[u8] = include_bytes!("fixtures/compressed500.container");
const EMPTY: &[u8] = include_bytes!("fixtures/empty.wire");
const VERSION: &[u8] = include_bytes!("fixtures/version2.wire");
const VERSION_CONTAINER: &[u8] = include_bytes!("fixtures/version2.container");

fn feed_all(decoder: &mut ReplyDecoder, mut bytes: &[u8], replies: &mut Vec<Reply>) {
    while !bytes.is_empty() {
        let result = decoder.feed(bytes).unwrap();
        assert!(result.consumed > 0);
        bytes = &bytes[result.consumed..];
        if let Some(reply) = result.item {
            replies.push(reply);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_handshake_and_six_byte_requests() {
    assert_eq!(
        js5::encode_handshake(b"abc").unwrap(),
        include_bytes!("fixtures/handshake.wire")
    );
    assert_eq!(js5::acknowledge(), [6, 0, 0, 3, 0, 0]);
    assert_eq!(js5::encode_status(false), [3, 0, 0, 0, 0, 0]);
    assert_eq!(js5::encode_status(true), [2, 0, 0, 0, 0, 0]);
    assert_eq!(
        js5::encode_request(255, 255, Priority::High),
        [1, 255, 0, 0, 0, 255]
    );
    assert_eq!(
        js5::encode_request(8, 0x89abcdef, Priority::Prefetch),
        [0, 8, 137, 171, 205, 239]
    );
    assert_eq!(
        js5::encode_request(0, u32::MAX, Priority::High),
        [1, 0, 255, 255, 255, 255]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn handshake_length_is_checked_without_truncation() {
    assert_eq!(js5::encode_handshake(&[1; 246]).unwrap()[1], 255);
    assert_eq!(
        js5::encode_handshake(&[1; 247]),
        Err(Error::HandshakeTooLong)
    );
    assert_eq!(js5::encode_handshake(b"a\0b"), Err(Error::InvalidToken));
    assert_eq!(
        js5::encode_handshake(b"").unwrap(),
        [15, 9, 0, 0, 2, 215, 0, 0, 0, 1, 0]
    );
    assert_eq!(
        js5::encode_handshake(&[128, 255]).unwrap()[10..],
        [128, 255, 0]
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn prefetch_count_is_explicit_not_a_version_table_index_count() {
    let bytes = [
        0, 0x12, 0x34, 0x56, 0x78, 0x80, 0, 0, 0, 0xff, 0xff, 0xff, 0xff,
    ];
    for split in 0..=bytes.len() {
        let mut decoder = HandshakeDecoder::new(3).unwrap();
        let first = decoder.feed(&bytes[..split]).unwrap();
        assert_eq!(first.consumed, split);
        let item = if split == bytes.len() {
            first.item.unwrap()
        } else {
            assert_eq!(first.item, None);
            decoder.feed(&bytes[split..]).unwrap().item.unwrap()
        };
        assert_eq!(
            item,
            HandshakeReply::Accepted {
                prefetch_keys: vec![0x12345678, i32::MIN, -1]
            }
        );
        decoder.finish().unwrap();
        assert_eq!(decoder.feed(&[]), Err(Error::DecoderFinished));
    }
    assert_eq!(js5::REVISION727_PREFETCH_COUNT, 27);
    assert!(HandshakeDecoder::new(256).is_ok());
    assert!(matches!(
        HandshakeDecoder::new(257),
        Err(Error::InvalidLimit)
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn handshake_stops_before_following_transport_bytes() {
    let mut decoder = HandshakeDecoder::new(0).unwrap();
    let result = decoder.feed(&[0, 99, 100]).unwrap();
    assert_eq!(result.consumed, 1);
    assert_eq!(
        result.item,
        Some(HandshakeReply::Accepted {
            prefetch_keys: vec![]
        })
    );
    for status in (1..=255).filter(|&status| status != 25) {
        let mut decoder = HandshakeDecoder::new(27).unwrap();
        let result = decoder.feed(&[status, 42]).unwrap();
        assert_eq!(result.consumed, 1);
        assert_eq!(result.item, Some(HandshakeReply::Rejected { status }));
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_interim_status_supports_fragmented_and_coalesced_acceptance() {
    let bytes = [25, 25, 0, 0, 0, 0, 25, 0xde, 0xad, 0xbe, 0xef, 99];
    let handshake_length = bytes.len() - 1;
    for split in 0..=handshake_length {
        let mut decoder = HandshakeDecoder::new(2).unwrap();
        let first = decoder.feed(&bytes[..split]).unwrap();
        assert_eq!(first.consumed, split);
        let item = if let Some(item) = first.item {
            assert_eq!(split, handshake_length);
            item
        } else {
            let second = decoder.feed(&bytes[split..]).unwrap();
            assert_eq!(second.consumed, handshake_length - split);
            second.item.unwrap()
        };
        assert_eq!(
            item,
            HandshakeReply::Accepted {
                prefetch_keys: vec![25, 0xdeadbeefu32 as i32],
            }
        );
        decoder.finish().unwrap();
    }
    let mut decoder = HandshakeDecoder::new(2).unwrap();
    let mut accepted = None;
    for &byte in &bytes[..handshake_length] {
        let result = decoder.feed(&[byte]).unwrap();
        assert_eq!(result.consumed, 1);
        if result.item.is_some() {
            accepted = result.item;
        }
    }
    assert!(matches!(accepted, Some(HandshakeReply::Accepted { .. })));
    decoder.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn repeated_native_interim_statuses_do_not_finish_or_change_prefetch_phase() {
    let mut decoder = HandshakeDecoder::new(0).unwrap();
    let result = decoder.feed(&[25; 4096]).unwrap();
    assert_eq!(result.consumed, 4096);
    assert_eq!(result.item, None);
    assert_eq!(decoder.feed(&[]).unwrap().item, None);
    let result = decoder.feed(&[0, 99]).unwrap();
    assert_eq!(result.consumed, 1);
    assert_eq!(
        result.item,
        Some(HandshakeReply::Accepted {
            prefetch_keys: vec![]
        })
    );
    decoder.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn interim_statuses_can_be_followed_by_a_rejection_or_truncation() {
    let mut decoder = HandshakeDecoder::new(27).unwrap();
    let result = decoder.feed(&[25, 25, 6, 0]).unwrap();
    assert_eq!(result.consumed, 3);
    assert_eq!(result.item, Some(HandshakeReply::Rejected { status: 6 }));
    decoder.finish().unwrap();
    let mut decoder = HandshakeDecoder::new(27).unwrap();
    assert_eq!(decoder.feed(&[25, 25]).unwrap().item, None);
    assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
    assert_eq!(decoder.feed(&[0]), Err(Error::DecoderFailed));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn handshake_rejects_every_truncated_prefetch_prefix() {
    let bytes = [0; 109];
    for length in 0..bytes.len() {
        let mut decoder = HandshakeDecoder::new(27).unwrap();
        assert_eq!(decoder.feed(&bytes[..length]).unwrap().item, None);
        assert_eq!(decoder.finish(), Err(Error::UnexpectedEof));
        assert_eq!(decoder.feed(&bytes), Err(Error::DecoderFailed));
    }
    let mut decoder = HandshakeDecoder::new(27).unwrap();
    assert!(decoder.feed(&bytes).unwrap().item.is_some());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_plain_reply_every_possible_two_chunk_split() {
    assert_eq!(PLAIN.len(), 1036);
    assert_eq!(PLAIN[512], 255);
    assert_eq!(PLAIN[1024], 255);
    for split in 0..=PLAIN.len() {
        let mut decoder = ReplyDecoder::new(2048).unwrap();
        let mut replies = vec![];
        feed_all(&mut decoder, &PLAIN[..split], &mut replies);
        feed_all(&mut decoder, &PLAIN[split..], &mut replies);
        assert_eq!(replies.len(), 1, "split {split}");
        assert_eq!(replies[0].index, 8);
        assert_eq!(replies[0].archive, 0x89abcdef);
        assert_eq!(replies[0].priority, Priority::Prefetch);
        assert_eq!(replies[0].container, PLAIN_CONTAINER);
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_compressed_reply_includes_the_decompressed_length() {
    assert_eq!(COMPRESSED.len(), 515);
    assert_eq!(COMPRESSED[512], 255);
    for chunk_size in 1..=COMPRESSED.len() {
        let mut decoder = ReplyDecoder::new(509).unwrap();
        let mut replies = vec![];
        for chunk in COMPRESSED.chunks(chunk_size) {
            feed_all(&mut decoder, chunk, &mut replies);
        }
        assert_eq!(replies.len(), 1);
        assert_eq!(replies[0].index, 30);
        assert_eq!(replies[0].archive, 70000);
        assert_eq!(replies[0].priority, Priority::High);
        assert_eq!(replies[0].container, COMPRESSED_CONTAINER);
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reply_stops_at_one_item_even_when_the_input_contains_many() {
    let stream = [EMPTY, PLAIN, COMPRESSED, VERSION, EMPTY].concat();
    for chunk_size in [
        1,
        2,
        3,
        7,
        9,
        10,
        11,
        127,
        502,
        511,
        512,
        513,
        1024,
        stream.len(),
    ] {
        let mut decoder = ReplyDecoder::new(2048).unwrap();
        let mut replies = vec![];
        for chunk in stream.chunks(chunk_size) {
            feed_all(&mut decoder, chunk, &mut replies);
        }
        assert_eq!(replies.len(), 5);
        assert_eq!(replies[0].container, [0, 0, 0, 0, 0]);
        assert_eq!(replies[1].container, PLAIN_CONTAINER);
        assert_eq!(replies[2].container, COMPRESSED_CONTAINER);
        assert_eq!(replies[3].container, VERSION_CONTAINER);
        assert_eq!(replies[4].container, [0, 0, 0, 0, 0]);
        assert_eq!(decoder.feed(&[]).unwrap().item, None);
        decoder.finish().unwrap();
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reply_reports_every_truncated_header_payload_and_marker() {
    for bytes in [PLAIN, COMPRESSED, EMPTY] {
        for length in 1..bytes.len() {
            let mut decoder = ReplyDecoder::new(2048).unwrap();
            assert_eq!(decoder.feed(&bytes[..length]).unwrap().item, None);
            assert_eq!(
                decoder.finish(),
                Err(Error::UnexpectedEof),
                "length {length}"
            );
            assert_eq!(decoder.feed(bytes), Err(Error::DecoderFailed));
        }
    }
    ReplyDecoder::new(5).unwrap().finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reply_lengths_are_checked_before_allocation_and_failure_is_sticky() {
    assert!(matches!(ReplyDecoder::new(4), Err(Error::InvalidLimit)));
    assert!(matches!(
        ReplyDecoder::new(usize::MAX),
        Err(Error::InvalidLimit)
    ));
    let mut decoder = ReplyDecoder::new(1028).unwrap();
    assert_eq!(
        decoder.feed(PLAIN),
        Err(Error::LengthLimitExceeded {
            length: 1029,
            limit: 1028
        })
    );
    assert_eq!(decoder.feed(EMPTY), Err(Error::DecoderFailed));
    assert_eq!(decoder.finish(), Err(Error::DecoderFailed));
    for length in [0x80000000u32, u32::MAX] {
        let mut header = EMPTY.to_vec();
        header[6..10].copy_from_slice(&length.to_be_bytes());
        assert_eq!(
            ReplyDecoder::new(2048).unwrap().feed(&header),
            Err(Error::InvalidLength)
        );
    }
    let mut header = EMPTY.to_vec();
    header[6..10].copy_from_slice(&i32::MAX.to_be_bytes());
    assert!(matches!(
        ReplyDecoder::new(2048).unwrap().feed(&header),
        Err(Error::LengthLimitExceeded { .. })
    ));
    let mut decoder = ReplyDecoder::new(508).unwrap();
    assert_eq!(
        decoder.feed(COMPRESSED),
        Err(Error::LengthLimitExceeded {
            length: 509,
            limit: 508
        })
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn strict_mode_validates_continuation_bytes() {
    for marker_offset in [512, 1024] {
        for marker in [0, 1, 127, 254] {
            let mut bytes = PLAIN.to_vec();
            bytes[marker_offset] = marker;
            let mut decoder = ReplyDecoder::new(2048).unwrap();
            assert_eq!(
                decoder.feed(&bytes),
                Err(Error::InvalidContinuation(marker))
            );
            assert_eq!(decoder.feed(EMPTY), Err(Error::DecoderFailed));
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_restart_treats_non_separator_as_the_next_header_first_byte() {
    let bytes = [&PLAIN[..512], COMPRESSED].concat();
    for split in 510..=524 {
        let mut decoder =
            ReplyDecoder::with_boundary_mode(2048, BoundaryMode::NativeRestart).unwrap();
        let mut replies = vec![];
        feed_all(&mut decoder, &bytes[..split], &mut replies);
        feed_all(&mut decoder, &bytes[split..], &mut replies);
        assert_eq!(replies.len(), 1);
        assert_eq!(replies[0].container, COMPRESSED_CONTAINER);
        assert_eq!(decoder.interrupted_replies(), 1);
        decoder.finish().unwrap();
    }
    let mut decoder = ReplyDecoder::with_boundary_mode(2048, BoundaryMode::NativeRestart).unwrap();
    let mut replies = vec![];
    feed_all(&mut decoder, PLAIN, &mut replies);
    assert_eq!(decoder.interrupted_replies(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn completion_on_a_block_boundary_needs_no_trailing_separator() {
    let mut bytes = PLAIN[..512].to_vec();
    bytes[6..10].copy_from_slice(&502i32.to_be_bytes());
    bytes.extend_from_slice(EMPTY);
    let mut decoder = ReplyDecoder::new(2048).unwrap();
    let first = decoder.feed(&bytes).unwrap();
    assert_eq!(first.consumed, 512);
    assert_eq!(first.item.unwrap().container.len(), 507);
    let second = decoder.feed(&bytes[first.consumed..]).unwrap();
    assert_eq!(second.item.unwrap().container, [0, 0, 0, 0, 0]);
    decoder.finish().unwrap();
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn nonzero_compression_metadata_is_preserved_without_decompression() {
    for compression in [1, 2, 3, 127] {
        let mut bytes = [EMPTY, &[0, 0, 0, 42]].concat();
        bytes[5] = compression | 128;
        let reply = ReplyDecoder::new(9)
            .unwrap()
            .feed(&bytes)
            .unwrap()
            .item
            .unwrap();
        assert_eq!(reply.priority, Priority::Prefetch);
        assert_eq!(reply.container, [compression, 0, 0, 0, 0, 0, 0, 0, 42]);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_version_table_preserves_crc_revision_whirlpool_and_opaque_signature() {
    let table = js5::decode_version_table(VERSION_CONTAINER).unwrap();
    assert_eq!(table.entries.len(), 2);
    assert_eq!(table.entries[0].crc, 0x89abcdefu32 as i32);
    assert_eq!(table.entries[0].revision, 727);
    assert_eq!(table.entries[1].crc, 0x12345678);
    assert_eq!(table.entries[1].revision, -1);
    assert_eq!(table.entries[0].whirlpool, std::array::from_fn(|i| i as u8));
    assert_eq!(
        table.entries[1].whirlpool,
        std::array::from_fn(|i| (64 + i) as u8)
    );
    assert_eq!(table.signature.len(), 65);
    assert_eq!(table.signature[0], 1);
    assert_eq!(table.signature[1], 254);
    assert_eq!(table.signature[64], 191);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn version_table_index_count_is_variable_and_unsigned() {
    for count in [0u8, 1, 27, 41, 128, 255] {
        let length = 1 + count as usize * 72;
        let mut container = vec![0; 5 + length];
        container[1..5].copy_from_slice(&(length as i32).to_be_bytes());
        container[5] = count;
        let table = js5::decode_version_table(&container).unwrap();
        assert_eq!(table.entries.len(), count as usize);
        assert_eq!(table.signature.len(), 0);
        if count > 0 {
            container.pop();
            let length = container.len() - 5;
            container[1..5].copy_from_slice(&(length as i32).to_be_bytes());
            assert_eq!(
                js5::decode_version_table(&container),
                Err(Error::InvalidVersionTable)
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn version_table_rejects_bad_headers_counts_lengths_and_oversized_signatures() {
    for length in 0..6 {
        assert_eq!(
            js5::decode_version_table(&VERSION_CONTAINER[..length]),
            Err(Error::InvalidVersionTable)
        );
    }
    for length in [0u32, u32::MAX, 0x80000000, 1] {
        let mut bytes = VERSION_CONTAINER.to_vec();
        bytes[1..5].copy_from_slice(&length.to_be_bytes());
        assert_eq!(
            js5::decode_version_table(&bytes),
            Err(Error::InvalidVersionTable)
        );
    }
    let mut bytes = VERSION_CONTAINER.to_vec();
    bytes[5] = 3;
    assert_eq!(
        js5::decode_version_table(&bytes),
        Err(Error::InvalidVersionTable)
    );
    let mut bytes = VERSION_CONTAINER.to_vec();
    bytes[0] = 2;
    assert_eq!(
        js5::decode_version_table(&bytes),
        Err(Error::InvalidCompression(2))
    );
    for signature_length in [256, 257] {
        let mut bytes = vec![0; 6 + signature_length];
        let length = (bytes.len() - 5) as i32;
        bytes[1..5].copy_from_slice(&length.to_be_bytes());
        if signature_length == 256 {
            assert_eq!(
                js5::decode_version_table(&bytes).unwrap().signature.len(),
                256
            );
        } else {
            assert_eq!(
                js5::decode_version_table(&bytes),
                Err(Error::InvalidVersionTable)
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn arbitrary_small_input_never_panics_or_spins() {
    let mut state = 0xcafebabeu32;
    for length in 0..300 {
        let mut bytes = vec![0; length];
        for byte in &mut bytes {
            state ^= state << 13;
            state ^= state >> 17;
            state ^= state << 5;
            *byte = state as u8;
        }
        let mut decoder = ReplyDecoder::new(4096).unwrap();
        let _ = decoder.feed(&bytes);
        let _ = decoder.finish();
        let _ = js5::decode_version_table(&bytes);
        let mut handshake = HandshakeDecoder::new(length % 28).unwrap();
        let _ = handshake.feed(&bytes);
        let _ = handshake.finish();
    }
}
