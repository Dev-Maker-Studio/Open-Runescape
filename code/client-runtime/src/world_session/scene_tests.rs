use super::*;
use crate::world_draw::HeightMap;
use crate::world_resources::{INDEX_LOC, INDEX_MODELS};

fn triangle_model() -> Vec<u8> {
    let mut data = vec![0, 1, 4, 1, 64, 65, 65, 0x12, 0x34, 74, 74];
    data.extend_from_slice(&3u16.to_be_bytes());
    data.extend_from_slice(&1u16.to_be_bytes());
    data.extend_from_slice(&[0, 0, 0, 0, 0, 0]);
    data.extend_from_slice(&1u16.to_be_bytes());
    data.extend_from_slice(&0u16.to_be_bytes());
    data.extend_from_slice(&1u16.to_be_bytes());
    data.extend_from_slice(&3u16.to_be_bytes());
    data
}

fn loc_bytes(model: u16) -> Vec<u8> {
    let mut data = vec![1, 1, 10, 1];
    data.extend_from_slice(&model.to_be_bytes());
    data.push(0);
    data
}

fn partial_follows(x: i32, y: i32, plane: u8) -> Vec<u8> {
    vec![
        ((y >> 3) as u8).wrapping_add(128),
        plane.wrapping_add(128),
        ((x >> 3) as u8).wrapping_add(128),
    ]
}

fn create_object(id: i32, shape: u8, rotation: u8, x: u8, y: u8) -> Vec<u8> {
    let mut out = id.to_be_bytes().to_vec();
    out.push(128u8.wrapping_sub((x << 4) | y));
    out.push(128u8.wrapping_sub((shape << 2) | rotation));
    out
}

fn scene_ready() -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut session = session();
    session.enable_scene().unwrap();
    assert!(session.enable_scene().is_err());
    assert_eq!(session.take_outgoing().unwrap().as_slice(), [14]);
    session.feed_at(&[0], 0).unwrap();
    assert!(!session.take_outgoing().unwrap().is_empty());
    session.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    session.feed_at(&first, 0).unwrap();
    (session, encoder)
}

fn feed_all(session: &mut WorldSession, wire: &[u8]) -> Vec<WorldEvent> {
    let mut events = Vec::new();
    let mut rest = wire;
    while !rest.is_empty() {
        let feed = session.feed_at(rest, 0).unwrap();
        rest = &rest[feed.consumed..];
        events.extend(feed.event);
    }
    events
}

#[test]
fn scene_is_off_unless_enabled_and_late_enable_is_refused() {
    let mut late = awaiting();
    assert!(late.enable_scene().is_err());
    let mut plain = ready();
    assert!(plain.report().scene.is_none());
    assert!(plain.scene().is_none());
    assert!(plain.scene_draw().is_err());
    let (_, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    let wire = encoder
        .encode(
            ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
            &partial_follows(16, 24, 0),
        )
        .unwrap();
    let events = feed_all(&mut plain, &wire);
    assert!(matches!(events[0], WorldEvent::OpaquePacket { .. }));
}

#[test]
fn zone_packets_replay_into_draw_data_once_cache_data_arrives() {
    let (mut session, mut encoder) = scene_ready();
    assert!(session.report().scene.is_some());
    let wire = [
        encoder
            .encode(
                ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
                &partial_follows(16, 24, 0),
            )
            .unwrap(),
        encoder
            .encode(
                ServerPacket::CREATE_OBJECT,
                &create_object(1000, 10, 0, 3, 5),
            )
            .unwrap(),
    ]
    .concat();
    let events = feed_all(&mut session, &wire);
    assert_eq!(events.len(), 2);
    assert!(
        events
            .iter()
            .all(|event| matches!(event, WorldEvent::SceneUpdated { .. }))
    );
    let scene = session.report().scene.unwrap();
    assert_eq!(scene.locs, 1);
    assert_eq!(scene.errors, 0);

    let base = session.scene().unwrap().base();
    let heights = HeightMap::new(104, 1, vec![Some(40); 105 * 105]).unwrap();
    session.set_scene_heights(heights).unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(draw.incomplete());
    assert_eq!(draw.triangles(), 0);
    assert!(draw.pending.contains(&(INDEX_LOC, 1000 >> 8)));
    assert!(session.scene_needs().contains(&(INDEX_LOC, 1000 >> 8)));

    session
        .scene_store()
        .provide_group(INDEX_LOC, 1000 >> 8, vec![(1000 & 0xff, loc_bytes(50))]);
    session.scene_provided().unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(draw.pending.contains(&(INDEX_MODELS, 50)));

    session
        .scene_store()
        .provide_group(INDEX_MODELS, 50, vec![(0, triangle_model())]);
    session.scene_provided().unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(!draw.incomplete());
    assert_eq!(draw.stats.locs, 1);
    assert_eq!(draw.triangles(), 1);
    let mesh = &draw.meshes[0];
    assert_eq!(mesh.tile, (base.0 + 16 + 3, base.1 + 24 + 5));
    assert_eq!(mesh.origin.1, 40);
}

#[test]
fn replay_is_independent_of_how_the_stream_is_fragmented() {
    let (_, mut encoder) = initial(ServerPacket::REGION, STANDARD);
    let wire = [
        encoder
            .encode(
                ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
                &partial_follows(16, 24, 0),
            )
            .unwrap(),
        encoder
            .encode(
                ServerPacket::CREATE_OBJECT,
                &create_object(1000, 10, 1, 3, 5),
            )
            .unwrap(),
        encoder
            .encode(
                ServerPacket::CREATE_OBJECT,
                &create_object(1000, 10, 0, 4, 5),
            )
            .unwrap(),
    ]
    .concat();
    let mut reports = Vec::new();
    for size in [1, 3, wire.len()] {
        let (mut session, _) = scene_ready();
        for chunk in wire.chunks(size) {
            feed_all(&mut session, chunk);
        }
        reports.push(session.report().scene.unwrap().locs);
    }
    assert_eq!(reports, vec![2, 2, 2]);
}

fn transform_loc(varbit: u16, alternatives: &[u16]) -> Vec<u8> {
    let mut data = vec![77];
    data.extend_from_slice(&varbit.to_be_bytes());
    data.extend_from_slice(&65535u16.to_be_bytes());
    data.push((alternatives.len() - 1) as u8);
    for id in alternatives {
        data.extend_from_slice(&id.to_be_bytes());
    }
    data.push(0);
    data
}

fn scene_ready_with_variables() -> (WorldSession, RegistryEncoder<ServerPacket>) {
    let mut session = lobby()
        .start_world_state(WorldConfig::from_json(&config().to_string()).unwrap(), 4096)
        .unwrap();
    session.enable_scene().unwrap();
    session.take_outgoing().unwrap();
    session.feed_at(&[0], 0).unwrap();
    session.take_outgoing().unwrap();
    session.feed_at(&response(1), 0).unwrap();
    let (first, encoder) = initial(ServerPacket::REGION, STANDARD);
    session.feed_at(&first, 0).unwrap();
    (session, encoder)
}

#[test]
fn a_transform_loc_flips_when_a_varbit_packet_arrives() {
    use crate::world_resources::INDEX_MODELS as MODELS;
    let (mut session, mut encoder) = scene_ready_with_variables();
    let wire = [
        encoder
            .encode(
                ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
                &partial_follows(16, 24, 0),
            )
            .unwrap(),
        encoder
            .encode(
                ServerPacket::CREATE_OBJECT,
                &create_object(1000, 10, 0, 3, 5),
            )
            .unwrap(),
    ]
    .concat();
    feed_all(&mut session, &wire);
    session
        .set_scene_heights(HeightMap::new(104, 1, vec![Some(40); 105 * 105]).unwrap())
        .unwrap();
    session.scene_store().provide_group(
        INDEX_LOC,
        1000 >> 8,
        vec![
            (1000 & 0xff, transform_loc(5, &[1001, 1002])),
            (1001 & 0xff, loc_bytes(50)),
            (1002 & 0xff, loc_bytes(51)),
        ],
    );
    session.scene_provided().unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(draw.pending.contains(&(22, 0)));
    assert_eq!(draw.triangles(), 0);
    session
        .scene_store()
        .provide_group(22, 0, vec![(5, vec![1, 0, 10, 2, 3, 0])]);
    session
        .scene_store()
        .provide_group(MODELS, 50, vec![(0, triangle_model())]);
    session.scene_provided().unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(!draw.incomplete());
    assert_eq!(draw.triangles(), 1);

    let value = 1u8;
    let wire = encoder
        .encode(
            ServerPacket::VARBIT_SMALL,
            &[0, 5, value.wrapping_add(128)],
        )
        .unwrap();
    feed_all(&mut session, &wire);
    session.tick_at(100_000).unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(draw.pending.contains(&(MODELS, 51)));
    assert_eq!(draw.triangles(), 0);
    session
        .scene_store()
        .provide_group(MODELS, 51, vec![(0, triangle_model())]);
    session.scene_provided().unwrap();
    let draw = session.scene_draw().unwrap();
    assert!(!draw.incomplete());
    assert_eq!(draw.triangles(), 1);
    assert_eq!(session.variables.as_ref().unwrap().bank.active(10).unwrap(), 4);
}

#[test]
fn advance_scene_moves_the_cycle_clock_and_reports_it() {
    let (mut session, _) = scene_ready();
    assert_eq!(session.advance_scene(0), 0);
    assert_eq!(session.advance_scene(7), 7);
    assert_eq!(session.advance_scene(3), 10);
    assert_eq!(session.report().scene.unwrap().cycle, 10);
}
