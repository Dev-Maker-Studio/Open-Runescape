use openrs_login::item_containers::{
    ContainerKey, Error, ItemContainers, ItemSlot, Limits, MAX_CONTAINER_SLOTS, MAX_CONTAINERS,
    MAX_PACKET_BYTES, MAX_TOOLTIP_BYTES, MAX_TOTAL_SLOTS, RECENT_CONTAINER_COUNT, UpdateKind,
};
use openrs_protocol::payload::PayloadError;
use openrs_protocol::registry::ServerPacket;

const FULL: ServerPacket = ServerPacket::UPDATE_INV_FULL;
const PARTIAL: ServerPacket = ServerPacket::UPDATE_INV_PARTIAL;
const STOP: ServerPacket = ServerPacket::UPDATE_INV_STOP_TRANSMIT;

fn key(id: u16, negative_key: bool) -> ContainerKey {
    ContainerKey { id, negative_key }
}

fn bank() -> ItemContainers {
    ItemContainers::new(Limits::default()).unwrap()
}

fn smart(bytes: &mut Vec<u8>, value: u16) {
    assert!(value <= 32767);
    if value < 128 {
        bytes.push(value as u8);
    } else {
        bytes.extend_from_slice(&(value + 32768).to_be_bytes());
    }
}

fn full(id: u16, flags: u8, slots: &[(u16, i32)]) -> Vec<u8> {
    let mut bytes = id.to_be_bytes().to_vec();
    bytes.push(flags);
    bytes.extend_from_slice(&(slots.len() as u16).to_be_bytes());
    for &(encoded_id, amount) in slots {
        if (0..255).contains(&amount) {
            bytes.push(128u8.wrapping_sub(amount as u8));
        } else {
            bytes.push(129);
            let [a, b, c, d] = amount.to_be_bytes();
            bytes.extend_from_slice(&[b, a, d, c]);
        }
        let [high, low] = encoded_id.to_be_bytes();
        bytes.extend_from_slice(&[low.wrapping_add(128), high]);
    }
    bytes
}

fn partial(id: u16, flags: u8) -> Vec<u8> {
    let mut bytes = id.to_be_bytes().to_vec();
    bytes.push(flags);
    bytes
}

fn record(bytes: &mut Vec<u8>, header: u16, encoded_id: u16, amount: i32) {
    smart(bytes, header);
    bytes.extend_from_slice(&encoded_id.to_be_bytes());
    if encoded_id != 0 {
        if (0..255).contains(&amount) {
            bytes.push(amount as u8);
        } else {
            bytes.push(255);
            bytes.extend_from_slice(&amount.to_be_bytes());
        }
    }
}

fn stop(id: u16, flags: u8) -> Vec<u8> {
    let mut bytes = vec![flags.wrapping_neg()];
    bytes.extend_from_slice(&id.to_le_bytes());
    bytes
}

fn masked(id: u16, flags: u8, size: usize, tooltips: &[(usize, &[u8])]) -> Vec<u8> {
    let mut bytes = full(id, flags | 2, &vec![(2, 1); size]);
    let start = bytes.len();
    bytes.resize(start + size.div_ceil(8), 0);
    for &(slot, _) in tooltips {
        bytes[start + (slot >> 3)] |= 1 << (slot & 7);
    }
    for &(_, text) in tooltips {
        bytes.extend_from_slice(text);
        bytes.push(0);
    }
    bytes
}

fn rejected_without_changes(
    state: &mut ItemContainers,
    opcode: ServerPacket,
    bytes: &[u8],
) -> Error {
    let before = state.clone();
    let error = state.apply(opcode, bytes).unwrap_err();
    assert_eq!(*state, before);
    error
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn only_native_inventory_catalog_identities_are_supported() {
    for (opcode, wire, size) in [(PARTIAL, 4, -2), (FULL, 5, -2), (STOP, 42, 3)] {
        assert_eq!(opcode.wire(), wire);
        assert_eq!(opcode.metadata().native_size, size);
        assert!(ItemContainers::supports(opcode));
    }
    let mut state = bank();
    for wire in 0..=255 {
        if let Some(opcode) = ServerPacket::from_wire(wire)
            && ![PARTIAL, FULL, STOP].contains(&opcode)
        {
            assert!(!ItemContainers::supports(opcode));
            assert_eq!(
                rejected_without_changes(&mut state, opcode, &[]),
                Error::UnsupportedOpcode(opcode)
            );
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_literal_wire_preserves_subtract_inverse_middle_and_add_little() {
    let bytes = [
        0x12, 0x34, 0, 0, 3, 0x80, 0x80, 0, 0x82, 0x81, 0, 0x81, 0x34, 0x12, 0x78, 0x56, 0x7f, 0xff,
    ];
    let mut state = bank();
    let update = state.apply(FULL, &bytes).unwrap();
    assert_eq!(update.key, key(0x1234, false));
    assert_eq!(update.kind, UpdateKind::Full);
    assert_eq!(update.record_count, 3);
    assert_eq!(update.slot_count, 3);
    assert!(update.container_present);
    let container = state.get(update.key).unwrap();
    assert_eq!(container.item_id(0), -1);
    assert_eq!(container.amount(0), 0);
    assert_eq!(container.item_id(1), 0);
    assert_eq!(container.amount(1), 254);
    assert_eq!(container.item_id(2), 65534);
    assert_eq!(container.amount(2), 0x1234_5678);
    assert_eq!(state.total_slots(), 3);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn quantities_are_signed_ints_and_full_empty_items_still_have_amounts() {
    let amounts = [0, 1, 254, 255, 256, i32::MAX, i32::MIN, -1, 0x1234_5678];
    let slots: Vec<_> = amounts.iter().map(|&amount| (0, amount)).collect();
    let mut state = bank();
    state.apply(FULL, &full(100, 0, &slots)).unwrap();
    let container = state.get(key(100, false)).unwrap();
    for (slot, &amount) in amounts.iter().enumerate() {
        assert_eq!(container.item_id(slot), -1);
        assert_eq!(container.amount(slot), amount);
    }
    let mut bytes = partial(100, 0);
    for (slot, &amount) in amounts.iter().enumerate() {
        record(&mut bytes, slot as u16, u16::MAX, amount);
    }
    state.apply(PARTIAL, &bytes).unwrap();
    let container = state.get(key(100, false)).unwrap();
    for (slot, &amount) in amounts.iter().enumerate() {
        assert_eq!(container.item_id(slot), 65534);
        assert_eq!(container.amount(slot), amount);
    }
    let mut bytes = partial(100, 0);
    record(&mut bytes, 0, 0, i32::MIN);
    record(&mut bytes, 1, 1, 254);
    state.apply(PARTIAL, &bytes).unwrap();
    let container = state.get(key(100, false)).unwrap();
    assert_eq!((container.item_id(0), container.amount(0)), (-1, 0));
    assert_eq!((container.item_id(1), container.amount(1)), (0, 254));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_shorter_and_zero_updates_clear_without_shrinking_or_creating() {
    let mut state = bank();
    state
        .apply(FULL, &masked(101, 0, 9, &[(0, b"old"), (8, b"tail")]))
        .unwrap();
    state
        .apply(FULL, &full(101, 0, &[(65535, 255), (0, 91)]))
        .unwrap();
    let container = state.get(key(101, false)).unwrap();
    assert_eq!(container.len(), 9);
    assert_eq!((container.item_id(0), container.amount(0)), (65534, 255));
    assert_eq!((container.item_id(1), container.amount(1)), (-1, 91));
    assert!(container.slots().iter().all(|slot| slot.tooltip.is_none()));
    assert!(
        container.slots()[2..]
            .iter()
            .all(|slot| slot == &ItemSlot::default())
    );
    state.apply(FULL, &full(101, 0, &[])).unwrap();
    assert_eq!(
        state.get(key(101, false)).unwrap().slots(),
        vec![ItemSlot::default(); 9]
    );
    assert_eq!(state.total_slots(), 9);
    let update = state.apply(FULL, &full(102, 0, &[])).unwrap();
    assert!(!update.container_present);
    assert_eq!(update.slot_count, 0);
    assert!(state.get(key(102, false)).is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sparse_partial_preserves_other_slots_and_duplicates_apply_in_wire_order() {
    let mut state = bank();
    state
        .apply(FULL, &full(103, 0, &[(2, 10), (3, 20), (4, 30)]))
        .unwrap();
    let mut bytes = partial(103, 0);
    record(&mut bytes, 1, 500, 255);
    record(&mut bytes, 1, 0, 0);
    record(&mut bytes, 1, 65535, -1);
    record(&mut bytes, 128, 0, 0);
    let update = state.apply(PARTIAL, &bytes).unwrap();
    assert_eq!(update.record_count, 4);
    assert_eq!(update.slot_count, 129);
    let container = state.get(key(103, false)).unwrap();
    assert_eq!((container.item_id(0), container.amount(0)), (1, 10));
    assert_eq!((container.item_id(1), container.amount(1)), (65534, -1));
    assert_eq!((container.item_id(2), container.amount(2)), (3, 30));
    assert!(
        container.slots()[3..]
            .iter()
            .all(|slot| slot == &ItemSlot::default())
    );
    assert_eq!(container.item_id(usize::MAX), -1);
    assert_eq!(container.amount(usize::MAX), 0);
    assert_eq!(container.tooltip(usize::MAX), None);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_smart_boundaries_and_noncanonical_two_byte_small_slot_are_native_values() {
    let mut state = bank();
    for (index, slot) in [0u16, 1, 127, 128, 8191, 32767].into_iter().enumerate() {
        let mut bytes = partial(104, 0);
        record(&mut bytes, slot, (index + 1) as u16, index as i32);
        state.apply(PARTIAL, &bytes).unwrap();
        assert_eq!(
            state.get(key(104, false)).unwrap().len(),
            usize::from(slot) + 1
        );
    }
    let bytes = [0, 104, 0, 0x80, 0, 0, 2, 17];
    state.apply(PARTIAL, &bytes).unwrap();
    let container = state.get(key(104, false)).unwrap();
    assert_eq!(container.item_id(0), 1);
    assert_eq!(container.amount(0), 17);
    assert_eq!(container.len(), 32768);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn header_only_partial_and_stop_absent_are_successful_noops() {
    let mut state = bank();
    let update = state.apply(PARTIAL, &partial(0, 0)).unwrap();
    assert_eq!(update.record_count, 0);
    assert_eq!(update.slot_count, 0);
    assert!(!update.container_present);
    assert!(state.is_empty());
    state.apply(FULL, &full(105, 0, &[(2, 5)])).unwrap();
    let before = state.get(key(105, false)).unwrap().clone();
    let update = state.apply(PARTIAL, &partial(105, 0)).unwrap();
    assert_eq!(state.get(key(105, false)), Some(&before));
    assert_eq!(update.slot_count, 1);
    assert_eq!(update.record_count, 0);
    let update = state.apply(STOP, &stop(65535, 1)).unwrap();
    assert_eq!(update.kind, UpdateKind::StopTransmit);
    assert!(!update.container_present);
    assert_eq!(state.len(), 1);
    assert_eq!(state.recent_cursor(), 4);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn negative_key_namespaces_are_sign_extended_and_deleted_independently() {
    let mut state = bank();
    for id in [0, 1, 0x1234, 65535] {
        assert_eq!(key(id, false).native_key(), i64::from(id));
        assert_eq!(
            key(id, true).native_key(),
            i64::from(i32::MIN) | i64::from(id)
        );
        state.apply(FULL, &full(id, 0, &[(2, 10)])).unwrap();
        state.apply(FULL, &full(id, 1, &[(3, 20)])).unwrap();
        state.apply(STOP, &stop(id, 1)).unwrap();
        assert!(state.get(key(id, true)).is_none());
        assert_eq!(state.get(key(id, false)).unwrap().amount(0), 10);
        state.apply(STOP, &stop(id, 0)).unwrap();
    }
    assert!(state.is_empty());
    assert_eq!(state.total_slots(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_tooltip_masks_are_lsb_first_and_preserve_empty_and_cp1252_strings() {
    let mut state = bank();
    let mut bytes = masked(
        106,
        0,
        10,
        &[(0, b""), (7, &[0x80, 0x81, 0xff]), (9, b"last")],
    );
    let mask_position = 5 + 10 * 3;
    bytes[mask_position + 1] |= 0xfc;
    state.apply(FULL, &bytes).unwrap();
    let container = state.get(key(106, false)).unwrap();
    assert_eq!(container.tooltip(0), Some(""));
    assert_eq!(container.tooltip(7), Some("€?ÿ"));
    assert_eq!(container.tooltip(9), Some("last"));
    assert_eq!(container.tooltip(8), None);
    assert_eq!(state.tooltip_bytes(), "€?ÿlast".len());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sparse_full_tooltips_allow_duplicates_retained_tail_and_ignore_out_of_range() {
    let mut state = bank();
    state.apply(FULL, &full(107, 0, &[(2, 1); 10])).unwrap();
    let mut bytes = full(107, 4, &[(3, 2)]);
    smart(&mut bytes, 4);
    for (slot, tooltip) in [(9, "tail"), (9, "last"), (32767, "ignored"), (0, "head")] {
        smart(&mut bytes, slot);
        bytes.extend_from_slice(tooltip.as_bytes());
        bytes.push(0);
    }
    state.apply(FULL, &bytes).unwrap();
    let container = state.get(key(107, false)).unwrap();
    assert_eq!(container.len(), 10);
    assert_eq!(container.item_id(9), -1);
    assert_eq!(container.tooltip(9), Some("last"));
    assert_eq!(container.tooltip(0), Some("head"));
    assert_eq!(state.tooltip_bytes(), 8);
    let mut bytes = full(108, 4, &[]);
    smart(&mut bytes, 1);
    smart(&mut bytes, 0);
    bytes.extend_from_slice(b"no creation\0");
    state.apply(FULL, &bytes).unwrap();
    assert!(state.get(key(108, false)).is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn full_mask_takes_precedence_over_sparse_flag() {
    let mut state = bank();
    state
        .apply(FULL, &masked(109, 4, 2, &[(1, b"mask")]))
        .unwrap();
    assert_eq!(state.get(key(109, false)).unwrap().tooltip(1), Some("mask"));
    state.apply(FULL, &full(110, 6, &[])).unwrap();
    assert!(state.get(key(110, false)).is_none());
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn partial_tooltip_modes_keep_clear_set_and_reserved_keep_match_native() {
    let mut state = bank();
    state
        .apply(
            FULL,
            &masked(
                111,
                0,
                4,
                &[(0, b"zero"), (1, b"one"), (2, b"two"), (3, b"three")],
            ),
        )
        .unwrap();
    let mut bytes = partial(111, 2);
    record(&mut bytes, 0, 8, 1);
    record(&mut bytes, (1 << 2) | 1, 8, 2);
    record(&mut bytes, (2 << 2) | 2, 0, 0);
    bytes.extend_from_slice(b"empty item tooltip\0");
    record(&mut bytes, (3 << 2) | 3, 0, 0);
    state.apply(PARTIAL, &bytes).unwrap();
    let container = state.get(key(111, false)).unwrap();
    assert_eq!(container.tooltip(0), Some("zero"));
    assert_eq!(container.tooltip(1), None);
    assert_eq!(container.tooltip(2), Some("empty item tooltip"));
    assert_eq!(container.tooltip(3), Some("three"));
    assert_eq!((container.item_id(2), container.amount(2)), (-1, 0));
    let mut bytes = partial(111, 0);
    record(&mut bytes, 0, 9, 5);
    state.apply(PARTIAL, &bytes).unwrap();
    assert_eq!(state.get(key(111, false)).unwrap().tooltip(0), None);
    let mut bytes = partial(111, 2);
    record(&mut bytes, 32766, 2, 1);
    bytes.extend_from_slice(b"max packed\0");
    state.apply(PARTIAL, &bytes).unwrap();
    let container = state.get(key(111, false)).unwrap();
    assert_eq!(container.len(), 8192);
    assert_eq!(container.tooltip(8191), Some("max packed"));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_flag_byte_uses_only_native_mode_and_namespace_bits() {
    for flags in 0..=255u8 {
        let mut state = bank();
        let mut bytes = full(112, flags, &[(2, 1)]);
        if flags & 6 != 0 {
            bytes.push(0);
        }
        state.apply(FULL, &bytes).unwrap();
        let mut bytes = partial(112, flags);
        record(&mut bytes, 2, 2, 7);
        if flags & 2 != 0 {
            bytes.extend_from_slice(b"set\0");
        }
        state.apply(PARTIAL, &bytes).unwrap();
        let container = state.get(key(112, flags & 1 != 0)).unwrap();
        let slot = usize::from(if flags & 2 != 0 { 0u8 } else { 2 });
        assert_eq!(container.amount(slot), 7);
        assert_eq!(
            container.tooltip(slot),
            if flags & 2 != 0 { Some("set") } else { None }
        );
        state.apply(STOP, &stop(112, flags)).unwrap();
        assert!(state.is_empty());
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_full_prefix_and_trailing_byte_fail_transactionally() {
    let mut state = bank();
    state
        .apply(FULL, &masked(113, 0, 5, &[(0, b"existing")]))
        .unwrap();
    for bytes in [
        full(113, 0, &[(2, 255), (65535, i32::MIN), (0, 17)]),
        masked(113, 0, 9, &[(0, b"first"), (8, b"last")]),
    ] {
        for end in 0..bytes.len() {
            rejected_without_changes(&mut state, FULL, &bytes[..end]);
        }
        let mut trailing = bytes;
        trailing.push(0);
        assert_eq!(
            rejected_without_changes(&mut state, FULL, &trailing),
            Error::TrailingData(1)
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn partial_record_prefixes_only_accept_complete_record_boundaries() {
    let mut bytes = partial(114, 2);
    record(&mut bytes, (128 << 2) | 2, 65535, i32::MIN);
    bytes.extend_from_slice(b"tooltip\0");
    let boundary = bytes.len();
    record(&mut bytes, (127 << 2) | 1, 0, 0);
    for end in 0..=bytes.len() {
        let mut state = bank();
        state.apply(FULL, &full(114, 0, &[(3, 1)])).unwrap();
        if [3, boundary, bytes.len()].contains(&end) {
            state.apply(PARTIAL, &bytes[..end]).unwrap();
        } else {
            rejected_without_changes(&mut state, PARTIAL, &bytes[..end]);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn malformed_sparse_strings_counts_and_stop_sizes_leave_all_state_unchanged() {
    let mut state = bank();
    state
        .apply(FULL, &masked(115, 0, 3, &[(1, b"existing")]))
        .unwrap();
    for trailer in [
        vec![],
        vec![128],
        vec![255, 255],
        vec![1, 0],
        vec![1, 0, b'x'],
    ] {
        let mut bytes = full(115, 4, &[(2, 9)]);
        bytes.extend_from_slice(&trailer);
        rejected_without_changes(&mut state, FULL, &bytes);
    }
    for end in 0..3 {
        assert_eq!(
            rejected_without_changes(&mut state, STOP, &stop(115, 0)[..end]),
            Error::Payload(PayloadError::UnexpectedEof)
        );
    }
    let mut bytes = stop(115, 0);
    bytes.push(0);
    assert_eq!(
        rejected_without_changes(&mut state, STOP, &bytes),
        Error::TrailingData(1)
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn each_recognized_packet_enforces_payload_bound_before_allocation() {
    let oversized = vec![0; MAX_PACKET_BYTES + 1];
    let mut state = bank();
    for opcode in [FULL, PARTIAL, STOP] {
        assert_eq!(
            rejected_without_changes(&mut state, opcode, &oversized),
            Error::LengthLimitExceeded {
                length: oversized.len(),
                limit: MAX_PACKET_BYTES
            }
        );
    }
    let mut bytes = full(116, 0, &[(2, 0); 21843]);
    assert_eq!(bytes.len(), 65534);
    state.apply(FULL, &bytes).unwrap();
    bytes[2] = 2;
    bytes.push(0);
    assert_eq!(bytes.len(), MAX_PACKET_BYTES);
    assert!(!matches!(
        rejected_without_changes(&mut state, FULL, &bytes),
        Error::LengthLimitExceeded { .. }
    ));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn limit_constructor_rejects_unbounded_configuration_and_allows_no_tooltip_storage() {
    for limits in [
        Limits {
            containers: 0,
            ..Limits::default()
        },
        Limits {
            containers: MAX_CONTAINERS + 1,
            ..Limits::default()
        },
        Limits {
            slots_per_container: 0,
            ..Limits::default()
        },
        Limits {
            slots_per_container: MAX_CONTAINER_SLOTS + 1,
            ..Limits::default()
        },
        Limits {
            total_slots: 0,
            ..Limits::default()
        },
        Limits {
            total_slots: MAX_TOTAL_SLOTS + 1,
            ..Limits::default()
        },
        Limits {
            tooltip_bytes: MAX_TOOLTIP_BYTES + 1,
            ..Limits::default()
        },
    ] {
        assert_eq!(ItemContainers::new(limits), Err(Error::InvalidLimits));
    }
    let limits = Limits {
        tooltip_bytes: 0,
        ..Limits::default()
    };
    let mut state = ItemContainers::new(limits).unwrap();
    assert_eq!(state.limits(), limits);
    state.apply(FULL, &masked(117, 0, 1, &[(0, b"")])).unwrap();
    assert_eq!(state.get(key(117, false)).unwrap().tooltip(0), Some(""));
    rejected_without_changes(&mut state, FULL, &masked(117, 0, 1, &[(0, b"x")]));
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn slot_container_and_total_quotas_fail_atomically_and_delete_reclaims_resources() {
    let limits = Limits {
        containers: 2,
        slots_per_container: 5,
        total_slots: 6,
        tooltip_bytes: 10,
    };
    let mut state = ItemContainers::new(limits).unwrap();
    state
        .apply(FULL, &masked(118, 0, 4, &[(0, b"first")]))
        .unwrap();
    state.apply(FULL, &full(119, 0, &[(2, 1); 2])).unwrap();
    assert!(matches!(
        rejected_without_changes(&mut state, FULL, &full(120, 0, &[(2, 1)])),
        Error::ContainerLimitExceeded { .. }
    ));
    let mut bytes = partial(118, 0);
    record(&mut bytes, 5, 2, 1);
    assert!(matches!(
        rejected_without_changes(&mut state, PARTIAL, &bytes),
        Error::SlotLimitExceeded { .. }
    ));
    let mut bytes = partial(118, 0);
    record(&mut bytes, 4, 2, 1);
    assert!(matches!(
        rejected_without_changes(&mut state, PARTIAL, &bytes),
        Error::TotalSlotLimitExceeded { .. }
    ));
    state.apply(FULL, &full(118, 0, &[])).unwrap();
    assert_eq!(state.total_slots(), 6);
    assert_eq!(state.tooltip_bytes(), 0);
    state.apply(STOP, &stop(118, 0)).unwrap();
    assert_eq!(state.total_slots(), 2);
    state.apply(FULL, &full(120, 0, &[(2, 1); 4])).unwrap();
    assert_eq!(state.len(), 2);
    assert_eq!(state.total_slots(), 6);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn tooltip_quota_counts_utf8_across_containers_and_failed_packet_restores_earlier_records() {
    let mut state = ItemContainers::new(Limits {
        tooltip_bytes: 6,
        ..Limits::default()
    })
    .unwrap();
    state
        .apply(FULL, &masked(121, 0, 1, &[(0, &[0x80])]))
        .unwrap();
    state
        .apply(FULL, &masked(122, 0, 1, &[(0, b"abc")]))
        .unwrap();
    assert_eq!(state.tooltip_bytes(), 6);
    let mut bytes = partial(121, 2);
    record(&mut bytes, 1, 2, 7);
    record(&mut bytes, 6, 3, 8);
    bytes.extend_from_slice(b"four\0");
    assert!(matches!(
        rejected_without_changes(&mut state, PARTIAL, &bytes),
        Error::TooltipLimitExceeded { .. }
    ));
    state.apply(STOP, &stop(122, 0)).unwrap();
    assert_eq!(state.tooltip_bytes(), 3);
    state
        .apply(FULL, &masked(121, 0, 2, &[(0, b"abcdef")]))
        .unwrap();
    assert_eq!(state.tooltip_bytes(), 6);
    state.apply(FULL, &full(121, 0, &[])).unwrap();
    assert_eq!(state.tooltip_bytes(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn tooltip_quota_validates_final_state_after_ordered_replacements_and_clears() {
    let mut initial = ItemContainers::new(Limits {
        tooltip_bytes: 6,
        ..Limits::default()
    })
    .unwrap();
    initial
        .apply(FULL, &masked(123, 0, 2, &[(0, b"abcdef")]))
        .unwrap();
    let mut set_then_clear = partial(123, 2);
    record(&mut set_then_clear, 6, 3, 8);
    set_then_clear.extend_from_slice(b"abc\0");
    record(&mut set_then_clear, 1, 2, 7);
    let mut clear_then_set = partial(123, 2);
    record(&mut clear_then_set, 1, 2, 7);
    record(&mut clear_then_set, 6, 3, 8);
    clear_then_set.extend_from_slice(b"abc\0");
    let mut forward = initial.clone();
    let mut reverse = initial;
    forward.apply(PARTIAL, &set_then_clear).unwrap();
    reverse.apply(PARTIAL, &clear_then_set).unwrap();
    assert_eq!(forward, reverse);
    assert_eq!(forward.tooltip_bytes(), 3);
    assert_eq!(forward.get(key(123, false)).unwrap().tooltip(0), None);
    assert_eq!(
        forward.get(key(123, false)).unwrap().tooltip(1),
        Some("abc")
    );
    let mut sparse = full(123, 4, &[(2, 1)]);
    smart(&mut sparse, 2);
    smart(&mut sparse, 0);
    sparse.extend_from_slice(b"oversized\0");
    smart(&mut sparse, 0);
    sparse.extend_from_slice(b"fit\0");
    forward.apply(FULL, &sparse).unwrap();
    assert_eq!(forward.tooltip_bytes(), 3);
    let container = forward.get(key(123, false)).unwrap();
    assert_eq!(container.len(), 2);
    assert_eq!(container.tooltip(0), Some("fit"));
    assert_eq!(container.tooltip(1), None);
    assert_eq!(
        rejected_without_changes(&mut forward, FULL, &masked(123, 0, 1, &[(0, b"toolong")])),
        Error::TooltipLimitExceeded {
            length: 7,
            limit: 6,
        }
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn successful_noops_and_namespaced_updates_append_raw_ids_to_native_recent_ring() {
    let mut state = bank();
    let mut expected = [0; RECENT_CONTAINER_COUNT];
    for index in 0..73u16 {
        let id = 500 + index;
        match index % 3 {
            0 => {
                state.apply(FULL, &full(id, 1, &[])).unwrap();
            }
            1 => {
                state.apply(PARTIAL, &partial(id, 0)).unwrap();
            }
            _ => {
                state.apply(STOP, &stop(id, 1)).unwrap();
            }
        }
        expected[usize::from(index) & 31] = id;
        assert_eq!(state.recent_cursor(), i32::from(index) + 1);
        assert_eq!(state.recently_used(), &expected);
    }
    assert!(state.is_empty());
}
