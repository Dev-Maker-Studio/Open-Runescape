use openrs_login::world_packets::pulse::{IF_SETHIDE, IF_SETTEXT, PulseQueue, PulseUpdate};
use openrs_login::world_packets::{Error, WorldPacket, decode};
use openrs_protocol::payload::PayloadError;
use openrs_protocol::registry::ServerPacket;

#[test]
fn requeue_moves_to_tail_and_last_write_wins() {
    let mut queue = PulseQueue::new();
    queue.apply(&PulseUpdate::new(IF_SETHIDE, 1).with(1, 0, 0));
    queue.apply(&PulseUpdate::new(IF_SETHIDE, 2).with(1, 0, 0));
    queue.apply(&PulseUpdate::new(IF_SETHIDE, 1).with(0, 0, 0));
    assert_eq!(queue.len(), 2);
    let first = queue.next_event().unwrap();
    assert_eq!(first.pointer & 0xffff_ffff, 2);
    let second = queue.next_event().unwrap();
    assert_eq!((second.pointer & 0xffff_ffff, second.primary), (1, 0));
    assert!(queue.is_empty());
    assert_eq!(queue.var_verify_id(), 3);
}

#[test]
fn negative_uid_corrupts_type_bits() {
    let update = PulseUpdate::new(IF_SETTEXT, -1);
    assert_eq!(update.pointer() >> 56, 0xff);
}

#[test]
fn drained_event_is_removed_from_cache() {
    let mut queue = PulseQueue::new();
    queue.apply(&PulseUpdate::new(IF_SETTEXT, 5).with_text("a".to_string()));
    queue.next_event().unwrap();
    queue.apply(&PulseUpdate::new(IF_SETTEXT, 5));
    assert_eq!(queue.next_event().unwrap().text, None);
}

#[test]
fn varbit_prefix_and_trailing_are_rejected() {
    let payload = [0x01, 0x02, 0x03];
    assert!(matches!(
        decode(ServerPacket::VARBIT_SMALL, &payload[..2]),
        Err(Error::Payload(PayloadError::UnexpectedEof))
    ));
    assert!(matches!(
        decode(ServerPacket::VARBIT_SMALL, &[1, 2, 3, 4]),
        Err(Error::TrailingBytes {
            consumed: 3,
            length: 4
        })
    ));
    assert!(matches!(
        decode(ServerPacket::VARBIT_SMALL, &payload),
        Ok(WorldPacket::VarBit { .. })
    ));
}
