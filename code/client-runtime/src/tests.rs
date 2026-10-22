use super::*;

fn accepted() -> Vec<u8> {
    vec![0; 1 + 27 * 4]
}

fn version_reply(index: u8, archive: u32, count: u8) -> Vec<u8> {
    let mut body = vec![count];
    for i in 0..count {
        body.extend_from_slice(&(-(i as i32) - 1).to_be_bytes());
        body.extend_from_slice(&(1000 + i as i32).to_be_bytes());
        body.extend_from_slice(&[i; 64]);
    }
    body.extend_from_slice(&[0xa5; 128]);
    let mut wire = vec![index];
    wire.extend_from_slice(&archive.to_be_bytes());
    wire.push(0);
    wire.extend_from_slice(&(body.len() as u32).to_be_bytes());
    let mut position = 10;
    for byte in body {
        if position == 512 {
            wire.push(255);
            position = 1;
        }
        wire.push(byte);
        position += 1;
    }
    wire
}

#[test]
fn starts_with_native_revision_handshake_and_waits_for_full_prefetch() {
    let mut probe = Probe::new(b"test").unwrap();
    assert_eq!(
        probe.take_outgoing(),
        [15, 13, 0, 0, 2, 215, 0, 0, 0, 1, b't', b'e', b's', b't', 0]
    );
    probe.feed(&accepted()[..108]).unwrap();
    assert!(probe.take_outgoing().is_empty());
    probe.feed(&[0]).unwrap();
    assert_eq!(
        probe.take_outgoing(),
        [6, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 1, 255, 0, 0, 0, 255]
    );
    assert!(!probe.complete());
}

#[test]
fn decodes_fragmented_block_marked_reply_with_no_jvm_or_network() {
    let wire = [accepted(), version_reply(255, 255, 8)].concat();
    for chunk_size in [1, 2, 3, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = Probe::new(b"test").unwrap();
        for chunk in wire.chunks(chunk_size) {
            probe.feed(chunk).unwrap();
        }
        assert!(probe.complete());
        let report = probe.report().unwrap();
        assert_eq!(report.indices.len(), 8);
        assert_eq!(
            report.indices[7],
            IndexVersion {
                index: 7,
                crc: -8,
                revision: 1007
            }
        );
        assert_eq!(report.prefetch_count, 27);
        assert_eq!(report.signature_bytes, 128);
        assert!(!report.authenticated);
        probe.finish().unwrap();
    }
}

#[test]
fn rejection_is_terminal_and_does_not_emit_requests() {
    let mut probe = Probe::new(b"test").unwrap();
    probe.take_outgoing();
    assert!(probe.feed(&[6]).unwrap_err().contains("rejected"));
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.feed(&accepted()).is_err());
    assert!(probe.report().is_none());
}

#[test]
fn incomplete_close_cannot_be_reported_as_success() {
    let mut probe = Probe::new(b"test").unwrap();
    probe.feed(&accepted()).unwrap();
    let reply = version_reply(255, 255, 1);
    probe.feed(&reply[..reply.len() - 1]).unwrap();
    assert!(probe.finish().is_err());
    assert!(!probe.complete());
    assert!(probe.report().is_none());
    assert!(probe.take_outgoing().is_empty());
}

#[test]
fn rejects_unrequested_reply_identity() {
    for (index, archive) in [(0, 255), (255, 0)] {
        let mut probe = Probe::new(b"test").unwrap();
        probe.feed(&accepted()).unwrap();
        assert!(probe.feed(&version_reply(index, archive, 1)).is_err());
        assert!(!probe.complete());
    }
}

#[test]
fn oversized_reply_fails_before_payload_allocation() {
    let mut probe = Probe::new(b"test").unwrap();
    probe.feed(&accepted()).unwrap();
    assert!(probe.feed(&[255, 0, 0, 0, 255, 0, 0, 1, 0, 0]).is_err());
    assert!(probe.report().is_none());
}

#[test]
fn trailing_bytes_invalidate_even_previously_complete_report() {
    let mut probe = Probe::new(b"test").unwrap();
    probe.feed(&accepted()).unwrap();
    probe.feed(&version_reply(255, 255, 1)).unwrap();
    assert!(probe.complete());
    assert!(probe.feed(&[0]).is_err());
    assert!(!probe.complete());
    assert!(probe.report().is_none());
}

#[test]
fn token_validation_and_report_serialization_are_explicit() {
    assert!(Probe::new(b"bad\0token").is_err());
    let mut probe = Probe::new(b"test").unwrap();
    probe.feed(&accepted()).unwrap();
    probe.feed(&version_reply(255, 255, 1)).unwrap();
    let report = serde_json::to_value(probe.report().unwrap()).unwrap();
    assert_eq!(report["authenticated"], false);
    assert_eq!(report["indices"][0]["crc"], -1);
}
