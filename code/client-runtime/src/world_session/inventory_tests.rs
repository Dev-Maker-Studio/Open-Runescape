use super::*;
use openrs_protocol::payload::PayloadWriter;

fn key(negative_key: bool) -> ContainerKey {
    ContainerKey {
        id: 93,
        negative_key,
    }
}

fn full() -> Vec<u8> {
    let mut storage = [0; 32];
    let mut writer = PayloadWriter::new(&mut storage);
    writer.write_u16_be(93).unwrap();
    writer.write_u8(0).unwrap();
    writer.write_u16_be(2).unwrap();
    writer.write_u8_s(5).unwrap();
    writer.write_u16_le_a(101).unwrap();
    writer.write_u8_s(255).unwrap();
    writer.write_i32_inverse_middle(i32::MIN).unwrap();
    writer.write_u16_le_a(65535).unwrap();
    writer.written().to_vec()
}

fn inventory_awaiting(limits: InventoryLimits) -> WorldSession {
    let mut session = session();
    session.enable_inventory(limits).unwrap();
    assert_eq!(session.take_outgoing().unwrap().as_slice(), [14]);
    session.feed_at(&[0], 0).unwrap();
    assert!(!session.take_outgoing().unwrap().is_empty());
    session
}

fn inventory_ready(limits: InventoryLimits) -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut session = inventory_awaiting(limits);
    session.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    session.feed_at(&first, 0).unwrap();
    (session, encoder)
}

#[test]
fn inventory_is_explicit_early_bounded_and_default_packets_remain_opaque() {
    let mut session = session();
    let invalid = InventoryLimits {
        containers: 0,
        ..InventoryLimits::default()
    };
    assert!(session.enable_inventory(invalid).is_err());
    assert!(session.report().inventory.is_none());
    session
        .enable_inventory(InventoryLimits::default())
        .unwrap();
    assert!(
        session
            .enable_inventory(InventoryLimits::default())
            .is_err()
    );
    assert_eq!(session.phase(), "sendHandshake");
    session.close();
    clean(&mut session, "closed");
    let mut late = awaiting();
    assert!(late.enable_inventory(InventoryLimits::default()).is_err());
    let mut legacy = ready();
    let (_, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    for (opcode, payload) in [
        (ServerPacket::UPDATE_INV_FULL, full()),
        (ServerPacket::UPDATE_INV_PARTIAL, vec![0, 93, 0]),
        (ServerPacket::UPDATE_INV_STOP_TRANSMIT, vec![0, 93, 0]),
    ] {
        assert!(matches!(
            legacy
                .feed_at(&encoder.encode(opcode, &payload).unwrap(), 0)
                .unwrap()
                .event,
            Some(WorldEvent::OpaquePacket { .. })
        ));
    }
    assert!(legacy.container(key(false)).is_err());
    assert!(legacy.item(key(false), 0).is_err());
    assert!(legacy.report().inventory.is_none());
}

#[test]
fn inventory_packets_share_cipher_and_preserve_fragmentation_and_state() {
    let (_, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    let wire = [
        response(1),
        initial(ServerPacket::REGION, STANDARD).0,
        encoder
            .encode(ServerPacket::UPDATE_INV_FULL, &full())
            .unwrap(),
        encoder
            .encode(ServerPacket::UPDATE_INV_PARTIAL, &[0, 93, 0, 4, 0, 43, 7])
            .unwrap(),
        encoder.encode(ServerPacket::PING, &[]).unwrap(),
    ]
    .concat();
    for size in [1, 2, 3, 7, 31, wire.len()] {
        let mut session = inventory_awaiting(InventoryLimits::default());
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
        assert_eq!(events.len(), 5);
        assert_eq!(
            serde_json::to_value(&events[2]).unwrap(),
            json!({"kind":"inventoryUpdated","opcode":5,"name":"UPDATE_INV_FULL","id":93,"negativeKey":false,"update":"full","recordCount":2,"slotCount":2,"containerPresent":true})
        );
        assert!(matches!(
            events[3],
            WorldEvent::InventoryUpdated {
                record_count: 1,
                slot_count: 5,
                ..
            }
        ));
        assert!(matches!(events[4], WorldEvent::OpaquePacket { .. }));
        assert_eq!(
            session.container(key(false)).unwrap(),
            Some(WorldContainerReport {
                id: 93,
                negative_key: false,
                slots: 5
            })
        );
        assert_eq!(
            session.item(key(false), 0).unwrap(),
            Some(WorldItemReport {
                item_id: 100,
                amount: 5,
                tooltip: None
            })
        );
        assert_eq!(
            session.item(key(false), 1).unwrap().unwrap().amount,
            i32::MIN
        );
        assert_eq!(session.item(key(false), 3).unwrap().unwrap().item_id, -1);
        assert_eq!(session.item(key(false), 4).unwrap().unwrap().item_id, 42);
        assert!(session.item(key(false), 5).unwrap().is_none());
        assert!(session.item(key(true), 0).unwrap().is_none());
        assert!(session.item(key(false), usize::MAX).is_err());
        assert_eq!(
            session.report().inventory.unwrap(),
            WorldInventoryReport {
                containers: 1,
                slots: 5,
                tooltip_bytes: 0,
                updates: 2,
                full_updates: 1,
                partial_updates: 1,
                stops: 0
            }
        );
        session.close();
        clean(&mut session, "closed");
        assert!(session.container(key(false)).is_err());
    }
}

#[test]
fn inventory_stop_removes_only_its_namespace_and_finish_clears_the_bank() {
    let (mut session, mut encoder) = inventory_ready(InventoryLimits::default());
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_FULL, &full())
                .unwrap(),
            0,
        )
        .unwrap();
    let mut alternate = full();
    alternate[2] = 1;
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_FULL, &alternate)
                .unwrap(),
            0,
        )
        .unwrap();
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_STOP_TRANSMIT, &[0, 93, 0])
                .unwrap(),
            0,
        )
        .unwrap();
    assert!(session.container(key(false)).unwrap().is_none());
    assert!(session.container(key(true)).unwrap().is_some());
    let report = session.report().inventory.unwrap();
    assert_eq!(
        (
            report.containers,
            report.slots,
            report.stops,
            report.updates
        ),
        (1, 2, 1, 3)
    );
    session.finish().unwrap();
    clean(&mut session, "closed");
}

#[test]
fn malformed_and_over_budget_inventory_packets_fail_closed() {
    for (limits, opcode, payload) in [
        (
            InventoryLimits::default(),
            ServerPacket::UPDATE_INV_FULL,
            vec![0, 93, 0, 0, 1],
        ),
        (
            InventoryLimits::default(),
            ServerPacket::UPDATE_INV_PARTIAL,
            vec![0, 93, 0, 4],
        ),
        (
            InventoryLimits {
                slots_per_container: 1,
                ..InventoryLimits::default()
            },
            ServerPacket::UPDATE_INV_FULL,
            full(),
        ),
    ] {
        let (mut session, mut encoder) = inventory_ready(limits);
        assert_eq!(
            session.feed_at(&encoder.encode(opcode, &payload).unwrap(), 0),
            Err("invalid world inventory packet")
        );
        clean(&mut session, "failed");
        assert!(session.item(key(false), 0).is_err());
    }
    let (mut session, mut encoder) = inventory_ready(InventoryLimits::default());
    session
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_FULL, &full())
                .unwrap(),
            0,
        )
        .unwrap();
    let truncated = encoder
        .encode(ServerPacket::UPDATE_INV_PARTIAL, &[0, 93, 0, 0, 0, 1, 1])
        .unwrap();
    session
        .feed_at(&truncated[..truncated.len() - 1], 0)
        .unwrap();
    assert!(session.finish().is_err());
    clean(&mut session, "failed");
}
