use super::*;
use openrs_login::player_stats::{SKILL_COUNT, SKILL_UPDATE_RING_SIZE};

fn stat(skill_id: u8, experience: i32, visible_level: u8) -> [u8; 6] {
    let xp = experience.to_be_bytes();
    [xp[0], xp[1], xp[2], xp[3], skill_id, visible_level]
}

fn stats_session(varps: bool, inventory: bool) -> WorldSession {
    let mut world = if varps {
        lobby()
            .start_world_state(WorldConfig::from_json(&config().to_string()).unwrap(), 4)
            .unwrap()
    } else {
        session()
    };
    if inventory {
        world.enable_inventory(InventoryLimits::default()).unwrap();
    }
    world
        .enable_player_stats(PlayerStatsLimits::default())
        .unwrap();
    world
}

fn handshake(world: &mut WorldSession) {
    assert_eq!(world.take_outgoing().unwrap().as_slice(), [14]);
    world.feed_at(&[0], 0).unwrap();
    assert!(!world.take_outgoing().unwrap().is_empty());
}

fn stats_ready(varps: bool, inventory: bool) -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut world = stats_session(varps, inventory);
    handshake(&mut world);
    world.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    world.feed_at(&first, 0).unwrap();
    (world, encoder)
}

fn empty_report(count: usize) -> WorldPlayerStatsReport {
    WorldPlayerStatsReport {
        skill_count: count,
        updates: 0,
        stat_updates: 0,
        run_energy_updates: 0,
        player_weight_updates: 0,
        recent_skill_updates: [0; SKILL_UPDATE_RING_SIZE],
        skill_update_cycle: 0,
        run_energy: 0,
        player_weight: 0,
    }
}

fn assert_cleared(world: &mut WorldSession, phase: &'static str) {
    clean(world, phase);
    assert!(world.player_stats.is_none());
    assert!(world.report().player_stats.is_none());
    assert!(world.player_stats().is_err());
    assert!(world.skill(0).is_err());
    assert!(
        !serde_json::to_value(world.report())
            .unwrap()
            .as_object()
            .unwrap()
            .contains_key("playerStats")
    );
    assert!(
        world
            .enable_player_stats(PlayerStatsLimits::default())
            .is_err()
    );
}

#[test]
fn player_stats_enable_is_explicit_early_once_bounded_and_nonterminal_on_invalid_arguments() {
    let mut world = session();
    assert!(world.player_stats().is_err());
    assert!(world.skill(0).is_err());
    assert!(
        !serde_json::to_value(world.report())
            .unwrap()
            .as_object()
            .unwrap()
            .contains_key("playerStats")
    );
    for skill_count in [0, SKILL_COUNT + 1, usize::MAX] {
        assert_eq!(
            world.enable_player_stats(PlayerStatsLimits { skill_count }),
            Err("invalid world player stats limits")
        );
        assert_eq!(world.phase(), "sendHandshake");
        assert!(world.report().player_stats.is_none());
    }
    world
        .enable_player_stats(PlayerStatsLimits { skill_count: 1 })
        .unwrap();
    assert_eq!(world.player_stats(), Ok(empty_report(1)));
    assert_eq!(
        world.skill(0),
        Ok(WorldSkillReport {
            experience: 0,
            visible_level: 0,
            base_level: 0
        })
    );
    for id in 1..=u8::MAX {
        assert_eq!(world.skill(id), Err("invalid world skill id"));
    }
    assert_eq!(
        world.enable_player_stats(PlayerStatsLimits::default()),
        Err("world player stats must be enabled before handshake")
    );
    handshake(&mut world);
    assert_eq!(
        world.enable_player_stats(PlayerStatsLimits::default()),
        Err("world player stats must be enabled before handshake")
    );
    assert_eq!(world.player_stats(), Ok(empty_report(1)));
    world.close();
    assert_cleared(&mut world, "closed");
    for mut late in [awaiting(), ready()] {
        assert_eq!(
            late.enable_player_stats(PlayerStatsLimits::default()),
            Err("world player stats must be enabled before handshake")
        );
        assert!(late.report().player_stats.is_none());
    }
}

#[test]
fn default_stats_are_opaque_and_do_not_change_existing_inventory_or_variable_schema() {
    let (mut world, mut encoder) = state_ready(4);
    for (opcode, payload) in [
        (ServerPacket::UPDATE_STAT, stat(255, i32::MIN, 255).to_vec()),
        (ServerPacket::RUN_ENERGY, vec![255]),
        (ServerPacket::PLAYER_WEIGHT, i16::MIN.to_be_bytes().to_vec()),
    ] {
        assert_eq!(
            world
                .feed_at(&encoder.encode(opcode, &payload).unwrap(), 0)
                .unwrap()
                .event,
            Some(WorldEvent::OpaquePacket {
                opcode: opcode.wire(),
                name: opcode.metadata().name,
                payload_bytes: payload.len()
            })
        );
    }
    let value = serde_json::to_value(world.report()).unwrap();
    assert_eq!(
        value
            .as_object()
            .unwrap()
            .keys()
            .map(String::as_str)
            .collect::<Vec<_>>(),
        [
            "accepted",
            "initialWorld",
            "phase",
            "variables",
            "worldReady"
        ]
    );
    assert_eq!(
        value["variables"],
        json!({"count":4,"scheduled":0,"serverAssignments":0,"published":0,"clears":0,"nonZero":0})
    );
    assert_eq!(world.varp(0), Ok(0));
    assert!(world.player_stats().is_err());
    assert_eq!(world.phase(), "worldReady");
}

#[test]
fn stats_dispatch_preserves_cipher_initial_boundaries_fragmentation_and_distinct_event_schema() {
    let (first, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    let wire = [
        response(1),
        first,
        encoder
            .encode(ServerPacket::UPDATE_STAT, &stat(24, i32::MAX, 255))
            .unwrap(),
        encoder.encode(ServerPacket::RUN_ENERGY, &[255]).unwrap(),
        encoder
            .encode(ServerPacket::PLAYER_WEIGHT, &i16::MIN.to_be_bytes())
            .unwrap(),
        encoder.encode(ServerPacket::PING, &[]).unwrap(),
    ]
    .concat();
    for size in [1, 2, 3, 7, 31, 511, wire.len()] {
        let mut world = stats_session(false, false);
        handshake(&mut world);
        let mut events = Vec::new();
        for chunk in wire.chunks(size) {
            let mut rest = chunk;
            while !rest.is_empty() {
                let feed = world.feed_at(rest, 0).unwrap();
                assert!((1..=rest.len()).contains(&feed.consumed));
                rest = &rest[feed.consumed..];
                if let Some(event) = feed.event {
                    events.push(event);
                }
            }
        }
        assert_eq!(events.len(), 6);
        assert_eq!(events[0], WorldEvent::LoginAccepted);
        assert_eq!(events[1], WorldEvent::WorldReady);
        assert_eq!(
            serde_json::to_value(&events[2]).unwrap(),
            json!({"kind":"statUpdated","opcode":ServerPacket::UPDATE_STAT.wire(),"name":"UPDATE_STAT","skillId":24,"experience":i32::MAX,"visibleLevel":255,"baseLevel":120})
        );
        assert_eq!(
            serde_json::to_value(&events[3]).unwrap(),
            json!({"kind":"runEnergyUpdated","opcode":ServerPacket::RUN_ENERGY.wire(),"name":"RUN_ENERGY","runEnergy":255})
        );
        assert_eq!(
            serde_json::to_value(&events[4]).unwrap(),
            json!({"kind":"playerWeightUpdated","opcode":ServerPacket::PLAYER_WEIGHT.wire(),"name":"PLAYER_WEIGHT","playerWeight":i16::MIN})
        );
        assert_eq!(
            events[5],
            WorldEvent::OpaquePacket {
                opcode: ServerPacket::PING.wire(),
                name: "PING",
                payload_bytes: 0
            }
        );
        assert_eq!(
            world.skill(24),
            Ok(WorldSkillReport {
                experience: i32::MAX,
                visible_level: 255,
                base_level: 120
            })
        );
        assert_eq!(
            world.skill(0),
            Ok(WorldSkillReport {
                experience: 0,
                visible_level: 0,
                base_level: 0
            })
        );
        let mut expected = empty_report(SKILL_COUNT);
        expected.updates = 3;
        expected.stat_updates = 1;
        expected.run_energy_updates = 1;
        expected.player_weight_updates = 1;
        expected.recent_skill_updates[0] = 24;
        expected.skill_update_cycle = 1;
        expected.run_energy = 255;
        expected.player_weight = i16::MIN;
        assert_eq!(world.player_stats(), Ok(expected.clone()));
        assert_eq!(world.report().player_stats, Some(expected));
        assert_eq!(
            world.report().initial_world,
            Some(WorldInitialReport::from(
                &decode_fresh_world(1, ServerPacket::REGION, STANDARD).unwrap()
            ))
        );
        queue_keepalive(&mut world);
        assert_eq!(
            world.take_outgoing().unwrap().as_slice(),
            [NATIVE_OUTGOING[3]]
        );
        world.mark_flushed().unwrap();
        world.finish().unwrap();
        assert_cleared(&mut world, "closed");
    }
}

#[test]
fn skill_queries_preserve_signed_experience_raw_levels_and_native_ring_wrap_in_fresh_reports() {
    let (mut world, mut encoder) = stats_ready(false, false);
    let before = world.player_stats().unwrap();
    let mut ring = [0; SKILL_UPDATE_RING_SIZE];
    for index in 0..70 {
        let skill = (index % SKILL_COUNT) as u8;
        ring[index & 31] = skill;
        let experience = if index % 2 == 0 { i32::MIN } else { i32::MAX };
        world
            .feed_at(
                &encoder
                    .encode(ServerPacket::UPDATE_STAT, &stat(skill, experience, 255))
                    .unwrap(),
                0,
            )
            .unwrap();
        assert_eq!(
            world.skill(skill),
            Ok(WorldSkillReport {
                experience,
                visible_level: 255,
                base_level: if experience < 0 {
                    1
                } else if skill == 24 {
                    120
                } else {
                    99
                }
            })
        );
    }
    for id in SKILL_COUNT as u8..=u8::MAX {
        assert_eq!(world.skill(id), Err("invalid world skill id"));
    }
    for energy in [0, 100, 101, 255] {
        world
            .feed_at(
                &encoder.encode(ServerPacket::RUN_ENERGY, &[energy]).unwrap(),
                0,
            )
            .unwrap();
    }
    for weight in [i16::MIN, -1, 0, i16::MAX] {
        world
            .feed_at(
                &encoder
                    .encode(ServerPacket::PLAYER_WEIGHT, &weight.to_be_bytes())
                    .unwrap(),
                0,
            )
            .unwrap();
    }
    let mut report = world.player_stats().unwrap();
    assert_eq!(before, empty_report(SKILL_COUNT));
    assert_eq!(report.recent_skill_updates, ring);
    assert_eq!(report.skill_update_cycle, 70);
    assert_eq!(report.updates, 78);
    assert_eq!(report.stat_updates, 70);
    assert_eq!(report.run_energy_updates, 4);
    assert_eq!(report.player_weight_updates, 4);
    assert_eq!(report.run_energy, 255);
    assert_eq!(report.player_weight, i16::MAX);
    report.recent_skill_updates.fill(255);
    report.run_energy = 0;
    assert_eq!(report.run_energy, 0);
    assert_eq!(report.recent_skill_updates, [255; SKILL_UPDATE_RING_SIZE]);
    assert_eq!(world.player_stats().unwrap().recent_skill_updates, ring);
    assert_eq!(world.player_stats().unwrap().run_energy, 255);
    let value = serde_json::to_value(world.player_stats().unwrap()).unwrap();
    assert_eq!(
        value
            .as_object()
            .unwrap()
            .keys()
            .map(String::as_str)
            .collect::<Vec<_>>(),
        [
            "playerWeight",
            "playerWeightUpdates",
            "recentSkillUpdates",
            "runEnergy",
            "runEnergyUpdates",
            "skillCount",
            "skillUpdateCycle",
            "statUpdates",
            "updates"
        ]
    );
}

#[test]
fn stats_inventory_and_variable_banks_are_independent_and_confirmation_keeps_outgoing_cipher() {
    let mut world = stats_session(true, true);
    handshake(&mut world);
    assert_eq!(
        world.feed_at(&[1], 0).unwrap().event,
        Some(WorldEvent::ConfirmationRequired)
    );
    world.confirm().unwrap();
    assert_eq!(
        world.take_outgoing().unwrap().as_slice(),
        [26u8.wrapping_add(NATIVE_OUTGOING[3])]
    );
    world.feed_at(&response(1), 0).unwrap();
    let (first, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    world.feed_at(&first, 0).unwrap();
    let inventory = [0, 93, 0, 0, 1, 123, 229, 0];
    world
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_FULL, &inventory)
                .unwrap(),
            0,
        )
        .unwrap();
    world
        .feed_at(
            &encoder
                .encode(ServerPacket::VARP_SMALL, &small_payload(1, -128))
                .unwrap(),
            0,
        )
        .unwrap();
    world
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_STAT, &stat(1, -1, 255))
                .unwrap(),
            0,
        )
        .unwrap();
    assert_eq!(world.varp(1), Ok(0));
    assert_eq!(
        world.tick_at(2).unwrap().events,
        [WorldEvent::VariablePublished {
            id: 1,
            previous: 0,
            value: -128
        }]
    );
    assert_eq!(world.varp(1), Ok(-128));
    assert_eq!(
        world
            .item(
                ContainerKey {
                    id: 93,
                    negative_key: false
                },
                0
            )
            .unwrap()
            .unwrap()
            .item_id,
        100
    );
    assert_eq!(world.skill(1).unwrap().experience, -1);
    assert_eq!(world.report().inventory.unwrap().updates, 1);
    assert_eq!(world.report().variables.unwrap().published, 1);
    assert_eq!(world.player_stats().unwrap().updates, 1);
    queue_keepalive(&mut world);
    assert_eq!(
        world.take_outgoing().unwrap().as_slice(),
        [NATIVE_OUTGOING[7]]
    );
    world.mark_flushed().unwrap();
    world.finish().unwrap();
    assert_cleared(&mut world, "closed");
}

#[test]
fn malformed_enabled_stats_fail_the_whole_session_and_clear_all_retained_state() {
    for id in [25, 255] {
        let (mut world, mut encoder) = stats_ready(true, true);
        world
            .feed_at(
                &encoder
                    .encode(ServerPacket::UPDATE_STAT, &stat(0, 83, 2))
                    .unwrap(),
                0,
            )
            .unwrap();
        queue_keepalive(&mut world);
        assert_eq!(
            world
                .feed_at(
                    &encoder
                        .encode(ServerPacket::UPDATE_STAT, &stat(id, 1, 1))
                        .unwrap(),
                    0
                )
                .err(),
            Some("invalid world player stats packet")
        );
        assert_cleared(&mut world, "failed");
    }
    let mut world = session();
    world
        .enable_player_stats(PlayerStatsLimits { skill_count: 1 })
        .unwrap();
    handshake(&mut world);
    world.feed_at(&response(1), 0).unwrap();
    let (first, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    world.feed_at(&first, 0).unwrap();
    assert_eq!(
        world
            .feed_at(
                &encoder
                    .encode(ServerPacket::UPDATE_STAT, &stat(1, 0, 0))
                    .unwrap(),
                0
            )
            .err(),
        Some("invalid world player stats packet")
    );
    assert_cleared(&mut world, "failed");
}

#[test]
fn stats_truncation_for_all_three_fixed_opcodes_fails_on_finish_and_never_publishes_partial_values()
{
    for (opcode, payload) in [
        (ServerPacket::UPDATE_STAT, stat(24, i32::MAX, 255).to_vec()),
        (ServerPacket::RUN_ENERGY, vec![255]),
        (ServerPacket::PLAYER_WEIGHT, i16::MIN.to_be_bytes().to_vec()),
    ] {
        for cut in 1..=payload.len() {
            let (mut world, mut encoder) = stats_ready(true, true);
            let wire = encoder.encode(opcode, &payload).unwrap();
            assert!(world.feed_at(&wire[..cut], 0).unwrap().event.is_none());
            assert_eq!(world.player_stats(), Ok(empty_report(SKILL_COUNT)));
            assert_eq!(world.finish(), Err("world connection ended during packet"));
            assert_cleared(&mut world, "failed");
        }
    }
}

#[test]
fn stats_cannot_replace_initial_map_packet_and_are_cleared_on_all_login_terminals() {
    for (opcode, payload) in [
        (ServerPacket::UPDATE_STAT, stat(0, 83, 2).to_vec()),
        (ServerPacket::RUN_ENERGY, vec![255]),
        (ServerPacket::PLAYER_WEIGHT, i16::MIN.to_be_bytes().to_vec()),
    ] {
        let mut world = stats_session(true, true);
        handshake(&mut world);
        world.feed_at(&response(1), 0).unwrap();
        let (_, incoming) = isaac_ciphers(&SEED).unwrap();
        let mut encoder = RegistryEncoder::new(Some(incoming), 65535).unwrap();
        let packet = encoder.encode(opcode, &payload).unwrap();
        let header_bytes = if opcode.wire() < 128 { 1 } else { 2 };
        let packet = [
            &packet[..header_bytes],
            &(payload.len() as u16).to_be_bytes(),
            payload.as_slice(),
        ]
        .concat();
        assert_eq!(
            world.feed_at(&packet, 0).err(),
            Some("invalid initial world state")
        );
        assert_cleared(&mut world, "failed");
    }
    for (wire, phase) in [
        (&[3][..], "rejected"),
        (&[21, 2][..], "queued"),
        (&[23][..], "retry"),
        (&[15][..], "reconnect"),
    ] {
        let mut world = stats_session(true, true);
        handshake(&mut world);
        world.feed_at(wire, 0).unwrap();
        assert_cleared(&mut world, phase);
    }
}

#[test]
fn stats_queries_enables_and_packet_updates_obey_existing_operation_event_and_byte_budgets() {
    for operation in 0..3 {
        let (mut world, _) = stats_ready(true, true);
        world.operations = MAX_OPERATIONS;
        let result = match operation {
            0 => world.skill(0).map(|_| ()),
            1 => world.player_stats().map(|_| ()),
            _ => world.enable_player_stats(PlayerStatsLimits::default()),
        };
        assert_eq!(result, Err("world session operation limit"));
        assert_cleared(&mut world, "failed");
    }
    for budget in 0..3 {
        let (mut world, mut encoder) = stats_ready(true, true);
        let mut wire = encoder
            .encode(ServerPacket::UPDATE_STAT, &stat(0, 1, 1))
            .unwrap();
        let error = match budget {
            0 => {
                world.events = MAX_EVENTS;
                "world packet event limit"
            }
            1 => {
                world.incoming_bytes = MAX_INCOMING_BYTES;
                "world incoming total byte limit"
            }
            _ => {
                wire = vec![0; MAX_FEED_BYTES + 1];
                "world incoming chunk byte limit"
            }
        };
        assert_eq!(world.feed_at(&wire, 0).err(), Some(error));
        assert_cleared(&mut world, "failed");
    }
    let (mut world, _) = stats_ready(true, true);
    world.close();
    assert_cleared(&mut world, "closed");
    world.finish().unwrap();
    assert_cleared(&mut world, "closed");
}

fn run_ops(state: &openrs_cs2::PlayerState, code: &[(openrs_cs2::Opcode, i32)]) -> Vec<i32> {
    use openrs_cs2::{Arguments, Script, Vm};
    use std::collections::BTreeMap;
    let mut bytes = vec![0];
    let code: Vec<_> = code
        .iter()
        .copied()
        .chain([(openrs_cs2::Opcode::RETURN, 0)])
        .collect();
    for (opcode, operand) in &code {
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
    let repository = BTreeMap::from([(0, Script::decode(&bytes).unwrap())]);
    let mut variables = crate::vars::WorldVariables::new(1).unwrap();
    let mut host = crate::vars::WorldVarHost::new(&mut variables, 0).with_player_state(state);
    let mut vm = Vm::default();
    vm.run_with_host(0, &repository, Arguments::default(), &mut host)
        .unwrap();
    vm.int_stack().to_vec()
}

#[test]
fn decoded_stat_energy_and_weight_packets_drive_the_cs2_player_state_opcodes() {
    use openrs_cs2::Opcode;
    let (mut world, mut encoder) = stats_ready(false, false);
    let mut state = openrs_cs2::PlayerState::default();
    for (opcode, payload) in [
        (ServerPacket::UPDATE_STAT, stat(3, 1_154, 12).to_vec()),
        (ServerPacket::UPDATE_STAT, stat(20, 13_034_431, 99).to_vec()),
        (ServerPacket::RUN_ENERGY, vec![87]),
        (ServerPacket::PLAYER_WEIGHT, (-12i16).to_be_bytes().to_vec()),
    ] {
        world
            .feed_at(&encoder.encode(opcode, &payload).unwrap(), 0)
            .unwrap();
    }
    world.sync_player_state(&mut state).unwrap();
    let ints = |ops: &[(Opcode, i32)]| run_ops(&state, ops);
    let skill = |op: Opcode, id: i32| ints(&[(Opcode::PUSH_CONSTANT_INT, id), (op, 0)]);
    assert_eq!(skill(Opcode::STAT, 3), [12]);
    assert_eq!(skill(Opcode::STAT_VISIBLE_XP, 3), [1_154]);
    assert_eq!(
        skill(Opcode::STAT_BASE, 3),
        [i32::from(world.skill(3).unwrap().base_level)]
    );
    assert_eq!(skill(Opcode::STAT, 20), [99]);
    assert_eq!(skill(Opcode::STAT_VISIBLE_XP, 20), [13_034_431]);
    assert_eq!(skill(Opcode::STAT_BASE, 20), [99]);
    assert_eq!(skill(Opcode::STAT, 0), [0]);
    assert_eq!(ints(&[(Opcode::RUNENERGY_VISIBLE, 0)]), [87]);
    assert_eq!(ints(&[(Opcode::RUNWEIGHT_VISIBLE, 0)]), [-12]);
    assert_eq!(
        ints(&[(Opcode::SELF_PLAYER_UID, 0)]),
        [state.my_player_index]
    );
    let disabled = session();
    assert!(disabled.sync_player_state(&mut state).is_err());
}
