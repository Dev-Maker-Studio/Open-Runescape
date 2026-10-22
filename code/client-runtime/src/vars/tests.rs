use super::*;
use serde_json::Value;

fn large(id: u16, value: i32) -> VariablePacket {
    VariablePacket::VarpLarge { id, value }
}

fn change(id: u16, previous: i32, value: i32) -> VariableChange {
    VariableChange {
        id,
        previous,
        value,
    }
}

fn hex(input: &str) -> Vec<u8> {
    input
        .as_bytes()
        .chunks_exact(2)
        .map(|pair| u8::from_str_radix(std::str::from_utf8(pair).unwrap(), 16).unwrap())
        .collect()
}

#[test]
fn small_payload_uses_signed_byte_before_add_little_unsigned_id() {
    for (bytes, id, value) in [
        ([0x80, 0x80, 0], 0, -128),
        ([0xff, 0x7f, 0xff], 65535, -1),
        ([0x7f, 0xb4, 0x12], 0x1234, 127),
        ([0, 0x00, 0x80], 0x8080, 0),
    ] {
        assert_eq!(
            decode_variable_packet(ServerPacket::VARP_SMALL, &bytes),
            Ok(VariablePacket::VarpSmall { id, value })
        );
    }
}

#[test]
fn large_payload_uses_inverse_middle_signed_int_before_add_little_unsigned_id() {
    for (bytes, id, value) in [
        ([0x34, 0x12, 0x78, 0x56, 0xb4, 0x12], 0x1234, 0x12345678),
        ([0, 0x80, 0, 0, 0x80, 0], 0, i32::MIN),
        ([0xff, 0x7f, 0xff, 0xff, 0x7f, 0xff], 65535, i32::MAX),
        ([0xff, 0xff, 0xff, 0xff, 0, 0x80], 0x8080, -1),
    ] {
        assert_eq!(
            decode_variable_packet(ServerPacket::VARP_LARGE, &bytes),
            Ok(large(id, value))
        );
    }
}

#[test]
fn exact_native_packet_names_sizes_and_empty_clear_are_preserved() {
    assert_eq!(ServerPacket::VARP_SMALL.wire(), 115);
    assert_eq!(ServerPacket::VARP_LARGE.wire(), 8);
    assert_eq!(ServerPacket::CLEAR_VARPS.wire(), 100);
    assert_eq!(ServerPacket::VARP_SMALL.metadata().native_size, 3);
    assert_eq!(ServerPacket::VARP_LARGE.metadata().native_size, 6);
    assert_eq!(ServerPacket::CLEAR_VARPS.metadata().native_size, 0);
    assert_eq!(
        decode_variable_packet(ServerPacket::CLEAR_VARPS, &[]),
        Ok(VariablePacket::ClearVarps)
    );
}

#[test]
fn every_truncation_and_extra_payload_byte_is_rejected_without_mutation() {
    let mut state = WorldVariables::new(300).unwrap();
    state.set_local(1, 20, 100).unwrap();
    state.apply(large(129, 30), 100).unwrap();
    for (opcode, valid) in [
        (ServerPacket::VARP_SMALL, vec![0xff, 0x81, 0]),
        (
            ServerPacket::VARP_LARGE,
            vec![0xff, 0xff, 0xff, 0xff, 0x81, 0],
        ),
        (ServerPacket::CLEAR_VARPS, vec![]),
    ] {
        for len in 0..valid.len() {
            let before = state.clone();
            assert!(matches!(
                state.apply_payload(opcode, &valid[..len], 200),
                Err(VariableError::PayloadLength { .. })
            ));
            assert_eq!(state, before);
        }
        let mut extra = valid.clone();
        extra.push(0);
        let before = state.clone();
        assert!(matches!(
            state.apply_payload(opcode, &extra, 200),
            Err(VariableError::PayloadLength { .. })
        ));
        assert_eq!(state, before);
    }
}

#[test]
fn all_other_registry_packets_are_explicitly_unsupported() {
    let mut state = WorldVariables::new(1).unwrap();
    for wire in 0..=160 {
        let opcode = ServerPacket::from_wire(wire).unwrap();
        if matches!(
            opcode,
            ServerPacket::VARP_SMALL | ServerPacket::VARP_LARGE | ServerPacket::CLEAR_VARPS
        ) {
            continue;
        }
        let before = state.clone();
        assert_eq!(
            state.apply_payload(opcode, &[], 0),
            Err(VariableError::UnsupportedPacket(opcode))
        );
        assert_eq!(state, before);
    }
}

#[test]
fn explicit_definition_span_is_bounded_and_zeroed_without_fabricating_definitions() {
    assert!(matches!(
        WorldVariables::new(MAX_VARPS + 1),
        Err(VariableError::CountOutOfRange(_))
    ));
    assert!(matches!(
        WorldVariables::new(usize::MAX),
        Err(VariableError::CountOutOfRange(_))
    ));
    let mut empty = WorldVariables::new(0).unwrap();
    assert_eq!(empty.count(), 0);
    assert!(empty.active(0).is_err());
    assert!(empty.pending(0).is_err());
    assert!(empty.apply(large(0, 1), 100).is_err());
    assert_eq!(
        empty.apply(VariablePacket::ClearVarps, 100),
        Ok(VariableEffect::Cleared)
    );
    assert!(empty.service(100).unwrap().is_empty());
    let mut state = WorldVariables::new(MAX_VARPS).unwrap();
    assert_eq!(state.active(u16::MAX), Ok(0));
    assert_eq!(state.pending(u16::MAX), Ok(0));
    state.apply(large(u16::MAX, i32::MIN), 10).unwrap();
    assert_eq!(state.service(10).unwrap(), [change(u16::MAX, 0, i32::MIN)]);
}

#[test]
fn out_of_range_access_and_assignments_are_transactional() {
    let mut state = WorldVariables::new(2).unwrap();
    state.apply(large(1, 4), 100).unwrap();
    let before = state.clone();
    for id in [2, 100, u16::MAX] {
        let error = VariableError::IdOutOfRange { id, count: 2 };
        assert_eq!(state.active(id), Err(error.clone()));
        assert_eq!(state.pending(id), Err(error.clone()));
        assert_eq!(state.apply(large(id, 12), 200), Err(error.clone()));
        assert_eq!(state.set_local(id, 14, 200), Err(error));
        assert_eq!(state, before);
    }
    assert!(
        state
            .apply_payload(ServerPacket::VARP_SMALL, &[1, 0x82, 0], 200)
            .is_err()
    );
    assert_eq!(state, before);
}

#[test]
fn server_assignment_changes_pending_only_until_service_and_coalesces() {
    let mut state = WorldVariables::new(5).unwrap();
    assert_eq!(
        state.apply(large(4, 5), 100),
        Ok(VariableEffect::Pending { id: 4, value: 5 })
    );
    state.apply(large(4, -8), 200).unwrap();
    state.apply(large(4, 9), 250).unwrap();
    assert_eq!(state.active(4), Ok(0));
    assert_eq!(state.pending(4), Ok(9));
    assert_eq!(state.scheduled_count(), 1);
    assert_eq!(state.service(250).unwrap(), [change(4, 0, 9)]);
    assert_eq!(state.active(4), Ok(9));
    assert_eq!(state.pending(4), Ok(9));
    assert_eq!(state.scheduled_count(), 0);
    assert!(state.service(251).unwrap().is_empty());
}

#[test]
fn immediate_server_sentinel_has_native_strict_deadline_one() {
    let mut state = WorldVariables::new(1).unwrap();
    state.apply(large(0, 7), 0).unwrap();
    assert!(state.service(0).unwrap().is_empty());
    assert!(state.service(1).unwrap().is_empty());
    assert_eq!(state.service(2).unwrap(), [change(0, 0, 7)]);
}

#[test]
fn unchanged_assignments_still_publish_a_native_change() {
    let mut state = WorldVariables::new(1).unwrap();
    state.apply(large(0, 0), 100).unwrap();
    assert_eq!(state.service(100).unwrap(), [change(0, 0, 0)]);
    state.apply(large(0, 0), 101).unwrap();
    assert_eq!(state.service(101).unwrap(), [change(0, 0, 0)]);
}

#[test]
fn service_matches_hash_bucket_order_and_insertion_order_within_bucket() {
    let mut state = WorldVariables::new(400).unwrap();
    for id in [257, 2, 129, 128, 0, 1, 256] {
        state.apply(large(id, i32::from(id) + 1), 100).unwrap();
    }
    state.apply(large(257, -1), 101).unwrap();
    let changes = state.service(101).unwrap();
    assert_eq!(
        changes.iter().map(|change| change.id).collect::<Vec<_>>(),
        [128, 0, 256, 257, 129, 1, 2]
    );
    assert_eq!(changes[3].value, -1);
    for id in [129, 1, 257] {
        state.apply(large(id, -2), 102).unwrap();
    }
    assert_eq!(
        state
            .service(102)
            .unwrap()
            .iter()
            .map(|change| change.id)
            .collect::<Vec<_>>(),
        [129, 1, 257]
    );
}

#[test]
fn local_assignment_changes_active_only_and_expires_without_publishing_pending() {
    let mut state = WorldVariables::new(1).unwrap();
    assert_eq!(state.set_local(0, 55, 100), Ok(change(0, 0, 55)));
    assert_eq!(state.active(0), Ok(55));
    assert_eq!(state.pending(0), Ok(0));
    assert!(state.service(600).unwrap().is_empty());
    assert_eq!(state.scheduled_count(), 1);
    assert!(state.service(601).unwrap().is_empty());
    assert_eq!(state.scheduled_count(), 0);
    assert_eq!(state.active(0), Ok(55));
    assert_eq!(state.pending(0), Ok(0));
}

#[test]
fn server_after_local_rearms_publication_for_500_ms_with_strict_expiry() {
    let mut state = WorldVariables::new(1).unwrap();
    state.set_local(0, 1, 100).unwrap();
    state.apply(large(0, 2), 200).unwrap();
    assert_eq!(state.active(0), Ok(1));
    assert_eq!(state.pending(0), Ok(2));
    assert!(state.service(700).unwrap().is_empty());
    assert_eq!(state.service(701).unwrap(), [change(0, 1, 2)]);
}

#[test]
fn repeated_protected_server_assignment_moves_deadline_without_reordering() {
    let mut state = WorldVariables::new(130).unwrap();
    state.set_local(129, 5, 100).unwrap();
    state.apply(large(1, 9), 100).unwrap();
    state.apply(large(129, 6), 200).unwrap();
    state.apply(large(129, 7), 300).unwrap();
    assert_eq!(state.service(700).unwrap(), [change(1, 0, 9)]);
    assert!(state.service(800).unwrap().is_empty());
    assert_eq!(state.service(801).unwrap(), [change(129, 5, 7)]);
}

#[test]
fn local_after_server_cancels_publication_but_retains_pending_until_another_server_write() {
    let mut state = WorldVariables::new(1).unwrap();
    state.apply(large(0, 7), 100).unwrap();
    state.set_local(0, 9, 200).unwrap();
    assert!(state.service(701).unwrap().is_empty());
    assert_eq!(state.active(0), Ok(9));
    assert_eq!(state.pending(0), Ok(7));
    state.apply(large(0, 8), 702).unwrap();
    assert_eq!(state.service(702).unwrap(), [change(0, 9, 8)]);
}

#[test]
fn local_rewrite_cancels_protected_server_publication() {
    let mut state = WorldVariables::new(1).unwrap();
    state.set_local(0, 1, 100).unwrap();
    state.apply(large(0, 2), 200).unwrap();
    state.set_local(0, 3, 300).unwrap();
    assert!(state.service(801).unwrap().is_empty());
    assert_eq!(state.active(0), Ok(3));
    assert_eq!(state.pending(0), Ok(2));
}

#[test]
fn service_removes_expired_middle_and_tail_without_losing_protected_head() {
    let mut state = WorldVariables::new(400).unwrap();
    state.set_local(0, 5, 100).unwrap();
    state.apply(large(128, 6), 100).unwrap();
    state.set_local(256, 7, 100).unwrap();
    state.apply(large(384, 8), 100).unwrap();
    assert_eq!(
        state.service(200).unwrap(),
        [change(128, 0, 6), change(384, 0, 8)]
    );
    state.apply(large(128, 9), 200).unwrap();
    assert_eq!(state.service(201).unwrap(), [change(128, 6, 9)]);
    state.apply(large(384, 10), 201).unwrap();
    assert!(
        state
            .service(600)
            .unwrap()
            .iter()
            .all(|change| change.id == 384)
    );
    assert!(state.service(601).unwrap().is_empty());
    assert_eq!(state.scheduled_count(), 0);
    state.apply(large(256, 11), 601).unwrap();
    assert_eq!(state.service(601).unwrap(), [change(256, 7, 11)]);
}

#[test]
fn clear_zeros_both_banks_discards_all_schedules_and_is_repeatable() {
    let mut state = WorldVariables::new(300).unwrap();
    state.apply(large(1, -1), 100).unwrap();
    state.set_local(129, 2, 100).unwrap();
    state.apply(large(129, 3), 100).unwrap();
    state.service(100).unwrap();
    assert_eq!(
        state.apply_payload(ServerPacket::CLEAR_VARPS, &[], 200),
        Ok(VariableEffect::Cleared)
    );
    for id in 0..300 {
        assert_eq!(state.active(id), Ok(0));
        assert_eq!(state.pending(id), Ok(0));
    }
    assert_eq!(state.scheduled_count(), 0);
    assert!(state.service(1000).unwrap().is_empty());
    assert_eq!(
        state.apply(VariablePacket::ClearVarps, 1000),
        Ok(VariableEffect::Cleared)
    );
    state.apply(large(129, 4), 1001).unwrap();
    assert_eq!(state.service(1001).unwrap(), [change(129, 0, 4)]);
}

#[test]
fn clock_rewind_and_overflow_are_explicit_and_leave_all_state_unchanged() {
    let mut state = WorldVariables::new(1).unwrap();
    state.set_local(0, 4, 100).unwrap();
    let before = state.clone();
    for now in [99, MAX_VARIABLE_TIME_MS + 1, u64::MAX] {
        assert!(state.apply(large(0, 7), now).is_err());
        assert_eq!(state, before);
        assert!(state.apply(VariablePacket::ClearVarps, now).is_err());
        assert_eq!(state, before);
        assert!(state.set_local(0, 9, now).is_err());
        assert_eq!(state, before);
        assert!(state.service(now).is_err());
        assert_eq!(state, before);
    }
    assert_eq!(
        MAX_VARIABLE_TIME_MS.checked_add(500),
        Some((1u64 << 62) - 1)
    );
    state.set_local(0, 8, MAX_VARIABLE_TIME_MS).unwrap();
    state.apply(large(0, 9), MAX_VARIABLE_TIME_MS).unwrap();
    assert!(state.service(MAX_VARIABLE_TIME_MS).unwrap().is_empty());
}

#[test]
fn all_wire_id_slots_can_be_scheduled_without_unbounded_growth() {
    let mut state = WorldVariables::new(MAX_VARPS).unwrap();
    for id in 0..=u16::MAX {
        state.apply(large(id, i32::from(id)), 10).unwrap();
        state.apply(large(id, -i32::from(id)), 10).unwrap();
    }
    assert_eq!(state.scheduled_count(), MAX_VARPS);
    let changes = state.service(10).unwrap();
    assert_eq!(changes.len(), MAX_VARPS);
    assert_eq!(state.scheduled_count(), 0);
    assert!(state.service(10).unwrap().is_empty());
    for id in 0..=u16::MAX {
        assert_eq!(state.active(id), Ok(-i32::from(id)));
    }
}

fn host_script(code: &[(openrs_cs2::Opcode, i32)]) -> openrs_cs2::Script {
    let mut bytes = vec![0];
    for (opcode, operand) in code {
        bytes.extend_from_slice(&opcode.wire().to_be_bytes());
        if opcode.metadata().has_int_constant {
            bytes.extend_from_slice(&operand.to_be_bytes());
        } else {
            bytes.push(*operand as u8);
        }
    }
    bytes.extend_from_slice(&(code.len() as i32).to_be_bytes());
    bytes.extend_from_slice(&[0; 12]);
    bytes.extend_from_slice(&[0, 0, 1]);
    openrs_cs2::Script::decode(&bytes).unwrap()
}

#[test]
fn cs2_host_reads_active_and_local_writes_keep_pending_deadline_semantics() {
    use openrs_cs2::{Arguments, Opcode, Vm};
    use std::collections::BTreeMap;
    let mut state = WorldVariables::new(3).unwrap();
    state.apply(large(2, 11), 100).unwrap();
    state.service(100).unwrap();
    let script = host_script(&[
        (Opcode::PUSH_VAR, 2),
        (Opcode::PUSH_CONSTANT_INT, 77),
        (Opcode::POP_VAR, 2),
        (Opcode::PUSH_VAR, 2),
        (Opcode::RETURN, 0),
    ]);
    let repository = BTreeMap::from([(0, script)]);
    let mut vm = Vm::default();
    vm.run_with_host(
        0,
        &repository,
        Arguments::default(),
        &mut WorldVarHost::new(&mut state, 200),
    )
    .unwrap();
    assert_eq!(vm.int_stack(), [11, 77]);
    assert_eq!(state.active(2), Ok(77));
    assert_eq!(state.pending(2), Ok(11));
    assert_eq!(state.scheduled_count(), 1);
    state.apply(large(2, 99), 250).unwrap();
    assert!(state.service(750).unwrap().is_empty());
    assert_eq!(state.service(751).unwrap(), [change(2, 77, 99)]);
}

#[test]
fn cs2_host_rejects_unverified_varbits_bad_ids_and_invalid_clock_without_mutation() {
    use openrs_cs2::{Arguments, HostError, Opcode, VarHost, Vm, VmErrorKind};
    use std::collections::BTreeMap;
    let mut state = WorldVariables::new(2).unwrap();
    state.set_local(1, 7, 100).unwrap();
    let before = state.clone();
    {
        let mut host = WorldVarHost::new(&mut state, 99);
        for id in [-1, 2, 65536, i32::MAX] {
            assert_eq!(host.varp(id), Err(HostError::InvalidVarp(id)));
            assert_eq!(host.set_varp(id, 4), Err(HostError::InvalidVarp(id)));
        }
        assert_eq!(host.varbit(-1), Err(HostError::InvalidVarbit(-1)));
        assert_eq!(host.varbit(0), Err(HostError::Unavailable));
        assert_eq!(host.varbit(i32::MAX), Err(HostError::Unavailable));
        assert_eq!(
            host.set_varp(1, 4),
            Err(HostError::State("invalid world variable clock".into()))
        );
    }
    assert_eq!(state, before);
    for opcode in [Opcode::PUSH_VARBIT, Opcode::POP_VARBIT] {
        let script = host_script(&[
            (Opcode::PUSH_CONSTANT_INT, 123),
            (opcode, 0),
            (Opcode::RETURN, 0),
        ]);
        let repository = BTreeMap::from([(0, script)]);
        let error = Vm::default()
            .run_with_host(
                0,
                &repository,
                Arguments::default(),
                &mut WorldVarHost::new(&mut state, 100),
            )
            .unwrap_err();
        assert_eq!(error.kind, VmErrorKind::Host(HostError::Unavailable));
        assert_eq!(state, before);
    }
    let mut host = WorldVarHost::new(&mut state, MAX_VARIABLE_TIME_MS + 1);
    assert_eq!(
        host.set_varp(1, 4),
        Err(HostError::State("invalid world variable clock".into()))
    );
    assert_eq!(state, before);
}

#[test]
fn native_existing_jar_payload_and_provider_fixtures_match() {
    let fixture: Value =
        serde_json::from_str(include_str!("../../tests/fixtures/variables-native.json")).unwrap();
    for sample in fixture["packets"].as_array().unwrap() {
        let opcode = ServerPacket::from_wire(sample["opcode"].as_u64().unwrap() as u16).unwrap();
        let packet =
            decode_variable_packet(opcode, &hex(sample["payloadHex"].as_str().unwrap())).unwrap();
        if opcode == ServerPacket::CLEAR_VARPS {
            assert_eq!(packet, VariablePacket::ClearVarps);
        } else {
            let id = sample["id"].as_u64().unwrap() as u16;
            let value = sample["value"].as_i64().unwrap() as i32;
            assert_eq!(
                packet,
                if opcode == ServerPacket::VARP_SMALL {
                    VariablePacket::VarpSmall {
                        id,
                        value: value as i8,
                    }
                } else {
                    large(id, value)
                }
            );
        }
    }
    for scenario in fixture["scenarios"].as_array().unwrap() {
        let mut state = WorldVariables::new(scenario["count"].as_u64().unwrap() as usize).unwrap();
        for step in scenario["steps"].as_array().unwrap() {
            let now = step["now"].as_u64().unwrap();
            match step["operation"].as_str().unwrap() {
                "server" => {
                    state
                        .apply(
                            large(
                                step["id"].as_u64().unwrap() as u16,
                                step["value"].as_i64().unwrap() as i32,
                            ),
                            now,
                        )
                        .unwrap();
                }
                "local" => {
                    state
                        .set_local(
                            step["id"].as_u64().unwrap() as u16,
                            step["value"].as_i64().unwrap() as i32,
                            now,
                        )
                        .unwrap();
                }
                "service" => {
                    let changes = state.service(now).unwrap();
                    let native_ids: Vec<u16> = step["published"]
                        .as_array()
                        .unwrap()
                        .iter()
                        .map(|id| id.as_u64().unwrap() as u16)
                        .collect();
                    assert_eq!(
                        changes.iter().map(|change| change.id).collect::<Vec<_>>(),
                        native_ids,
                        "{}",
                        scenario["name"]
                    );
                }
                "clear" => {
                    state.apply(VariablePacket::ClearVarps, now).unwrap();
                }
                operation => panic!("unknown native fixture operation {operation}"),
            }
            let slots = step["slots"].as_array().unwrap();
            for native in slots {
                let id = native["id"].as_u64().unwrap() as u16;
                assert_eq!(
                    state.active(id).unwrap(),
                    native["active"].as_i64().unwrap() as i32,
                    "{}",
                    scenario["name"]
                );
                assert_eq!(
                    state.pending(id).unwrap(),
                    native["pending"].as_i64().unwrap() as i32,
                    "{}",
                    scenario["name"]
                );
            }
            assert_eq!(
                state.scheduled_count(),
                step["scheduled"].as_u64().unwrap() as usize,
                "{}",
                scenario["name"]
            );
        }
    }
}

#[test]
fn cs2_host_binds_the_appearance_state_for_the_entity_opcodes() {
    use openrs_cs2::{AppearanceState, Arguments, Opcode, PlayerAppearance, Vm, VmErrorKind};
    use std::collections::BTreeMap;
    let script = host_script(&[
        (Opcode::PUSH_CONSTANT_INT, 1),
        (Opcode::SETGENDER, 0),
        (Opcode::RETURN, 0),
    ]);
    let repository = BTreeMap::from([(0, script)]);
    let mut variables = WorldVariables::new(1).unwrap();
    let error = Vm::default()
        .run_with_host(
            0,
            &repository,
            Arguments::default(),
            &mut WorldVarHost::new(&mut variables, 0),
        )
        .unwrap_err();
    assert_eq!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(Opcode::SETGENDER)
    );
    let mut appearance = AppearanceState {
        my_player: true,
        appearance: Some(PlayerAppearance {
            appearance: Some(vec![0; 14]),
            recolor_dst_indices: Some(vec![0; 10]),
            ..PlayerAppearance::default()
        }),
    };
    Vm::default()
        .run_with_host(
            0,
            &repository,
            Arguments::default(),
            &mut WorldVarHost::new(&mut variables, 0).with_appearance_state(&mut appearance),
        )
        .unwrap();
    let mine = appearance.appearance.unwrap();
    assert!(mine.female);
    assert_ne!(mine.hash, 0);
}

#[test]
fn cs2_host_binds_the_outbox_for_the_active_chat_phrase_opcodes() {
    use openrs_cs2::{Arguments, Opcode, Outbox, QuickChatDefs, Vm, VmErrorKind};
    use std::collections::BTreeMap;
    let script = host_script(&[
        (Opcode::PUSH_CONSTANT_INT, 5),
        (Opcode::ACTIVECHATPHRASE_PREPARE, 0),
        (Opcode::ACTIVECHATPHRASE_SENDPUBLIC, 0),
        (Opcode::RETURN, 0),
    ]);
    let repository = BTreeMap::from([(0, script)]);
    let mut variables = WorldVariables::new(1).unwrap();
    let defs = QuickChatDefs::new(0, 0);
    let error = Vm::default()
        .run_with_host(
            0,
            &repository,
            Arguments::default(),
            &mut WorldVarHost::new(&mut variables, 0).with_quick_chat(&defs),
        )
        .unwrap_err();
    assert_eq!(
        error.kind,
        VmErrorKind::UnsupportedOpcode(Opcode::ACTIVECHATPHRASE_SENDPUBLIC)
    );
    let mut outbox = Outbox::default();
    Vm::default()
        .run_with_host(
            0,
            &repository,
            Arguments::default(),
            &mut WorldVarHost::new(&mut variables, 0)
                .with_quick_chat(&defs)
                .with_outbox(&mut outbox),
        )
        .unwrap();
    assert_eq!(outbox.packets.len(), 1);
    assert_eq!(outbox.packets[0].opcode, 64);
    assert_eq!(outbox.packets[0].payload, [3, 0, 0, 5]);
}
