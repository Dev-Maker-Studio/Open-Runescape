use super::*;
use crate::cs2_host::{RunInputs, run_in_world};
use openrs_cs2::{Opcode, Script};
use std::collections::BTreeMap;

fn script(code: &[(Opcode, i32)]) -> BTreeMap<i32, Script> {
    let code: Vec<_> = code.iter().copied().chain([(Opcode::RETURN, 0)]).collect();
    let mut bytes = vec![0];
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
    BTreeMap::from([(0, Script::decode(&bytes).unwrap())])
}

fn ready() -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut world = lobby()
        .start_world_state(WorldConfig::from_json(&config().to_string()).unwrap(), 4)
        .unwrap();
    world.enable_inventory(InventoryLimits::default()).unwrap();
    world
        .enable_player_stats(PlayerStatsLimits::default())
        .unwrap();
    assert_eq!(world.take_outgoing().unwrap().as_slice(), [14]);
    world.feed_at(&[0], 0).unwrap();
    assert!(!world.take_outgoing().unwrap().is_empty());
    world.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    world.feed_at(&first, 0).unwrap();
    (world, encoder)
}

fn run(world: &mut WorldSession, code: &[(Opcode, i32)]) -> crate::cs2_host::HostRunReport {
    run_in_world(0, &script(code), &RunInputs::default(), world).unwrap()
}

#[test]
fn world_script_sees_live_stats_inventory_and_varps_from_decoded_packets() {
    let (mut world, mut encoder) = ready();
    let mut stat = 54_321i32.to_be_bytes().to_vec();
    stat.extend_from_slice(&[7, 40]);
    world
        .feed_at(
            &encoder.encode(ServerPacket::UPDATE_STAT, &stat).unwrap(),
            0,
        )
        .unwrap();
    let stats = run(
        &mut world,
        &[(Opcode::PUSH_CONSTANT_INT, 7), (Opcode::STAT, 0)],
    );
    assert_eq!(stats.status, "ok", "{:?}", stats.error);
    assert_eq!(stats.ints, [40]);
    assert_eq!(
        stats.hosts,
        [
            "varps",
            "clock",
            "lobby",
            "appearance",
            "outbox",
            "entity",
            "playerStats",
            "inventories"
        ]
    );
    let varp = run(&mut world, &[(Opcode::PUSH_VAR, 0)]);
    assert_eq!(varp.ints, [0]);
    assert_eq!(varp.instructions_executed, Some(2));
}

#[test]
fn world_script_reads_the_live_inventory_container() {
    use openrs_protocol::payload::PayloadWriter;
    let (mut world, mut encoder) = ready();
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
    let payload = writer.written().to_vec();
    world
        .feed_at(
            &encoder
                .encode(ServerPacket::UPDATE_INV_FULL, &payload)
                .unwrap(),
            0,
        )
        .unwrap();
    let key = ContainerKey {
        id: 93,
        negative_key: false,
    };
    let live = world.item(key, 0).unwrap().unwrap();
    assert_eq!(live.amount, 5);
    for (opcode, expected) in [
        (Opcode::INV_GETOBJ, live.item_id),
        (Opcode::INV_GETNUM, live.amount),
    ] {
        let report = run(
            &mut world,
            &[
                (Opcode::PUSH_CONSTANT_INT, 93),
                (Opcode::PUSH_CONSTANT_INT, 0),
                (opcode, 0),
            ],
        );
        assert_eq!(report.status, "ok", "{:?}", report.error);
        assert_eq!(report.ints, [expected]);
    }
}

#[test]
fn world_script_without_enabled_state_reports_unbound_hosts_or_refuses() {
    let mut world = session();
    assert!(run_in_world(0, &script(&[]), &RunInputs::default(), &mut world).is_err());
}

#[test]
fn membership_time_comes_from_the_injected_wall_clock_and_the_lobby_record() {
    let (mut world, _) = ready();
    let epoch = 1_700_000_000_000i64;
    world.set_wall_clock(epoch).unwrap();
    world.set_lobby_state(openrs_cs2::LobbyState {
        membership_end: epoch + 120 * 60_000 + 30_000,
        is_member_subscription: true,
        ..openrs_cs2::LobbyState::default()
    });
    let report = run(&mut world, &[(Opcode::USERDETAIL_LOBBY_MEMBERSHIP, 0)]);
    assert_eq!(report.status, "ok", "{:?}", report.error);
    assert_eq!(
        report.ints,
        [((epoch + 120 * 60_000 + 30_000) / 60_000) as i32, 120, 1]
    );
    assert!(report.hosts.contains(&"clock"));
    assert!(report.hosts.contains(&"lobby"));
}

#[test]
fn appearance_ops_edit_the_decoded_local_player_block_and_persist_until_the_server_resends() {
    use crate::world_resources::INDEX_DEFAULTS;
    let mut world = lobby()
        .start_world_state(WorldConfig::from_json(&config().to_string()).unwrap(), 4)
        .unwrap();
    world.enable_scene().unwrap();
    assert_eq!(world.take_outgoing().unwrap().as_slice(), [14]);
    world.feed_at(&[0], 0).unwrap();
    world.take_outgoing().unwrap();
    world.feed_at(&response(1), 0).unwrap();
    let (first, _) = initial(ServerPacket::REGION, STANDARD);
    world.feed_at(&first, 0).unwrap();
    world
        .scene_store()
        .provide_group(INDEX_DEFAULTS, 6, vec![(0, vec![1, 2, 0, 0, 0])]);

    let missing = run(
        &mut world,
        &[
            (Opcode::PUSH_CONSTANT_INT, 1),
            (Opcode::PUSH_CONSTANT_INT, 4151),
            (Opcode::SETOBJ, 0),
        ],
    );
    assert_eq!(missing.status, "error");

    let mut block = vec![1, 0, 0, 0, 1, 5, 0, 0, 0];
    block.extend_from_slice(&[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    block.extend_from_slice(&1426u16.to_be_bytes());
    block.extend_from_slice(b"Me\0");
    block.extend_from_slice(&[3, 3, 255, 0]);
    let own = world.scene().unwrap().own_index();
    world
        .scene
        .as_mut()
        .unwrap()
        .scene
        .player_states
        .get_mut(&own)
        .unwrap()
        .appearance = Some(block);

    let report = run(
        &mut world,
        &[
            (Opcode::PUSH_CONSTANT_INT, 1),
            (Opcode::PUSH_CONSTANT_INT, 4151),
            (Opcode::SETOBJ, 0),
        ],
    );
    assert_eq!(report.status, "ok", "{:?}", report.error);
    assert!(report.hosts.contains(&"appearance"));
    let mine = world.appearance.appearance.clone().unwrap();
    assert_eq!(mine.bas_id, 1426);
    assert!(!mine.female);
    assert_eq!(
        mine.appearance,
        Some(vec![0x8000_0000u32 as i32 | 5, 4151 | 0x4000_0000])
    );
    assert_eq!(mine.recolor_dst_indices, Some((1..=10).collect()));

    let again = run(
        &mut world,
        &[(Opcode::PUSH_CONSTANT_INT, 1), (Opcode::SETGENDER, 0)],
    );
    assert_eq!(again.status, "ok", "{:?}", again.error);
    let mine = world.appearance.appearance.clone().unwrap();
    assert!(mine.female);
    assert_eq!(mine.appearance.unwrap()[1], 4151 | 0x4000_0000);
}
