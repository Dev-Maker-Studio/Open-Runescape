use super::*;
use openrs_login::world_state::{MapBase, MapSize};

struct Source {
    npcs: BTreeMap<i32, NpcTypeShape>,
    locs: BTreeMap<i32, CustomizeCounts>,
}

impl Source {
    fn empty() -> Self {
        Self {
            npcs: BTreeMap::new(),
            locs: BTreeMap::new(),
        }
    }
}

impl ShapeSource for Source {
    fn npc_shape(&self, type_id: i32) -> Option<NpcTypeShape> {
        self.npcs.get(&type_id).copied()
    }

    fn customize_counts(&self, loc_id: i32, _shape: u8) -> Option<CustomizeCounts> {
        self.locs.get(&loc_id).copied()
    }
}

fn layout(base: (i32, i32), loading_type: LoadingType) -> MapLayout {
    MapLayout {
        map_size: MapSize::Default104,
        center_x: 0,
        center_y: 0,
        force_position_update: false,
        loading_type,
        base: MapBase {
            x: base.0,
            y: base.1,
        },
        chunks: None,
        region_ids: Vec::new(),
        xtea_keys: Vec::new(),
    }
}

fn scene() -> WorldScene {
    WorldScene::new(
        1,
        &layout((3200, 3200), LoadingType::Standard),
        Some(OwnPlayer {
            x: 3210,
            y: 3210,
            plane: 0,
        }),
    )
}

fn tile_byte(x: u8, y: u8) -> u8 {
    (x << 4) | y
}

fn partial_follows(x: i32, y: i32, plane: u8) -> Vec<u8> {
    vec![
        ((y >> 3) as u8).wrapping_add(128),
        plane.wrapping_add(128),
        ((x >> 3) as u8).wrapping_add(128),
    ]
}

fn full_follows(x: i32, y: i32, plane: u8) -> Vec<u8> {
    vec![128u8.wrapping_sub((x >> 3) as u8), (y >> 3) as u8, plane]
}

fn create_item(item: u16, amount: u16, x: u8, y: u8) -> Vec<u8> {
    vec![
        (amount as u8).wrapping_add(128),
        (amount >> 8) as u8,
        (item >> 8) as u8,
        (item as u8).wrapping_add(128),
        tile_byte(x, y).wrapping_neg(),
    ]
}

fn remove_item(item: u16, x: u8, y: u8) -> Vec<u8> {
    vec![
        tile_byte(x, y).wrapping_neg(),
        (item as u8).wrapping_add(128),
        (item >> 8) as u8,
    ]
}

fn create_object(id: i32, shape: u8, rotation: u8, x: u8, y: u8) -> Vec<u8> {
    let mut out = id.to_be_bytes().to_vec();
    out.push(128u8.wrapping_sub(tile_byte(x, y)));
    out.push(128u8.wrapping_sub((shape << 2) | rotation));
    out
}

fn destroy_object(shape: u8, rotation: u8, x: u8, y: u8) -> Vec<u8> {
    vec![((shape << 2) | rotation).wrapping_add(128), tile_byte(x, y)]
}

fn spot_anim(id: u16, x: u8, y: u8) -> Vec<u8> {
    let mut out = vec![tile_byte(x, y)];
    out.extend_from_slice(&id.to_be_bytes());
    out.extend_from_slice(&100u16.to_be_bytes());
    out.extend_from_slice(&5u16.to_be_bytes());
    out.push(0);
    out
}

fn projanim(x: u8, y: u8, end_time: u16) -> Vec<u8> {
    let mut out = vec![(x << 3) | y, 0, 0];
    out.extend_from_slice(&(-1i16).to_be_bytes());
    out.extend_from_slice(&1u16.to_be_bytes());
    out.push(10);
    out.push(10);
    out.extend_from_slice(&0u16.to_be_bytes());
    out.extend_from_slice(&end_time.to_be_bytes());
    out.push(0);
    out.extend_from_slice(&0u16.to_be_bytes());
    out
}

fn enclosed(x: i32, y: i32, plane: u8, subs: &[(u8, Vec<u8>)]) -> Vec<u8> {
    let mut out = vec![
        128u8.wrapping_sub((y >> 3) as u8),
        plane.wrapping_add(128),
        (x >> 3) as u8,
    ];
    for (index, body) in subs {
        out.push(*index);
        out.extend_from_slice(body);
    }
    out
}

fn apply(
    scene: &mut WorldScene,
    packet: ServerPacket,
    payload: &[u8],
    src: &Source,
) -> Vec<SceneEffect> {
    scene.apply(packet, payload, src)
}

fn open_zone(scene: &mut WorldScene, x: i32, y: i32, plane: u8, src: &Source) {
    apply(
        scene,
        ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
        &partial_follows(x, y, plane),
        src,
    );
}

#[test]
fn orientation_cardinals() {
    assert_eq!(step_orientation(0, -1), Some(0));
    assert_eq!(step_orientation(-1, 0), Some(512));
    assert_eq!(step_orientation(0, 1), Some(1024));
    assert_eq!(step_orientation(1, 0), Some(1536));
    assert_eq!(step_orientation(0, 0), None);
}

#[test]
fn loc_layers() {
    assert_eq!(loc_layer(0), 0);
    assert_eq!(loc_layer(3), 0);
    assert_eq!(loc_layer(4), 1);
    assert_eq!(loc_layer(10), 2);
    assert_eq!(loc_layer(22), 3);
    assert_eq!(loc_layer(99), 2);
}

#[test]
fn ground_item_lifecycle() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 16, 24, 0, &src);
    assert_eq!(scene.zone_origin(), Some((3216, 3224, 0)));
    let effects = apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(995, 40, 3, 5),
        &src,
    );
    assert_eq!(
        effects,
        vec![SceneEffect::ZoneUpdates {
            count: 1,
            applied: 1
        }]
    );
    let key = (0, 3219, 3229);
    assert_eq!(
        scene.ground_items()[&key],
        vec![GroundItem {
            id: 995,
            amount: 40
        }]
    );
    apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(995 | 0x8000, 7, 3, 5),
        &src,
    );
    assert_eq!(scene.ground_items()[&key].len(), 2);
    apply(
        &mut scene,
        ServerPacket::REMOVE_GROUND_ITEM,
        &remove_item(995, 3, 5),
        &src,
    );
    assert_eq!(scene.ground_items()[&key].len(), 1);
    apply(
        &mut scene,
        ServerPacket::REMOVE_GROUND_ITEM,
        &remove_item(995, 3, 5),
        &src,
    );
    assert!(scene.ground_items().is_empty());
    let effects = apply(
        &mut scene,
        ServerPacket::REMOVE_GROUND_ITEM,
        &remove_item(995, 3, 5),
        &src,
    );
    assert_eq!(
        effects,
        vec![SceneEffect::ZoneUpdates {
            count: 1,
            applied: 0
        }]
    );
}

#[test]
fn objects_add_and_remove_by_layer() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 8, 8, 1, &src);
    apply(
        &mut scene,
        ServerPacket::CREATE_OBJECT,
        &create_object(1276, 10, 2, 1, 2),
        &src,
    );
    let key = (1, 3209, 3210, 2);
    assert_eq!(
        scene.locs()[&key].change,
        LocChange::Added {
            id: 1276,
            shape: 10,
            rotation: 2
        }
    );
    apply(
        &mut scene,
        ServerPacket::DESTROY_OBJECT,
        &destroy_object(10, 2, 1, 2),
        &src,
    );
    assert_eq!(scene.locs()[&key].change, LocChange::Removed);
    apply(
        &mut scene,
        ServerPacket::CREATE_OBJECT,
        &create_object(-1, 0, 0, 4, 4),
        &src,
    );
    assert_eq!(scene.locs()[&(1, 3212, 3212, 0)].change, LocChange::Removed);
}

#[test]
fn full_follows_clears_the_zone() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 8, 8, 0, &src);
    apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(1, 1, 0, 0),
        &src,
    );
    apply(
        &mut scene,
        ServerPacket::CREATE_OBJECT,
        &create_object(5, 10, 0, 1, 1),
        &src,
    );
    open_zone(&mut scene, 16, 8, 0, &src);
    apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(2, 1, 0, 0),
        &src,
    );
    let effects = apply(
        &mut scene,
        ServerPacket::UPDATE_ZONE_FULL_FOLLOWS,
        &full_follows(8, 8, 0),
        &src,
    );
    assert_eq!(
        effects,
        vec![SceneEffect::ZoneOrigin {
            x: 3208,
            y: 3208,
            plane: 0,
            full: true
        }]
    );
    assert!(scene.locs().is_empty());
    assert_eq!(scene.ground_items().len(), 1);
    assert!(scene.ground_items().contains_key(&(0, 3216, 3208)));
}

#[test]
fn enclosed_batches_apply_in_order() {
    let src = Source::empty();
    let mut scene = scene();
    let payload = enclosed(
        24,
        32,
        0,
        &[
            (2, create_item(554, 12, 1, 1)),
            (4, create_object(11, 10, 1, 2, 2)),
            (14, spot_anim(100, 3, 3)),
            (1, remove_item(554, 1, 1)),
        ],
    );
    let effects = apply(
        &mut scene,
        ServerPacket::UPDATE_ZONE_PARTIAL_ENCLOSED,
        &payload,
        &src,
    );
    assert_eq!(
        effects,
        vec![SceneEffect::ZoneUpdates {
            count: 4,
            applied: 4
        }]
    );
    assert!(scene.ground_items().is_empty());
    assert_eq!(scene.locs().len(), 1);
    assert_eq!(scene.spots()[&(0, 3227, 3235)].id, 100);
    apply(
        &mut scene,
        ServerPacket::SPOT_ANIM,
        &spot_anim(0xffff, 3, 3),
        &src,
    );
    assert!(scene.spots().is_empty());
}

#[test]
fn updates_outside_the_scene_are_ignored_unless_large_instance() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 104, 0, 0, &src);
    let effects = apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(1, 1, 0, 0),
        &src,
    );
    assert_eq!(
        effects,
        vec![SceneEffect::ZoneUpdates {
            count: 1,
            applied: 0
        }]
    );
    let mut large = WorldScene::new(1, &layout((3200, 3200), LoadingType::InstanceLarge), None);
    open_zone(&mut large, 104, 0, 0, &src);
    apply(
        &mut large,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(1, 1, 0, 0),
        &src,
    );
    assert_eq!(large.ground_items().len(), 1);
}

#[test]
fn projectiles_expire_with_cycles() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 8, 8, 0, &src);
    apply(
        &mut scene,
        ServerPacket::MAP_PROJANIM,
        &projanim(2, 3, 30),
        &src,
    );
    assert_eq!(scene.projectiles().len(), 1);
    let projectile = scene.projectiles()[0];
    assert_eq!((projectile.start_x, projectile.start_y), (3210, 3211));
    assert_eq!(
        projectile.scene_start(scene.base()),
        (10 * 512 + 256, 11 * 512 + 256)
    );
    scene.advance_cycles(29);
    assert_eq!(scene.projectiles().len(), 1);
    scene.advance_cycles(1);
    assert!(scene.projectiles().is_empty());
}

#[test]
fn varbits_are_stored() {
    let src = Source::empty();
    let mut scene = scene();
    let mut payload = 77u16.to_be_bytes().to_vec();
    payload.push(5u8.wrapping_add(128));
    let effects = apply(&mut scene, ServerPacket::VARBIT_SMALL, &payload, &src);
    assert_eq!(effects, vec![SceneEffect::VarBit { id: 77, value: 5 }]);
    assert_eq!(scene.varbit(77), Some(5));
}

#[test]
fn customize_without_counts_defers_then_retries() {
    let mut src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 8, 8, 0, &src);
    let mut payload = 4242i32.to_be_bytes().to_vec();
    payload.push((10u8 << 2).wrapping_add(128));
    payload.push(2 | 4);
    payload.push(tile_byte(1, 1).wrapping_add(128));
    payload.extend_from_slice(&7i32.to_be_bytes());
    payload.extend_from_slice(&9u16.to_be_bytes());
    let effects = apply(&mut scene, ServerPacket::CUSTOMIZE_OBJECT, &payload, &src);
    assert_eq!(effects, vec![SceneEffect::Deferred { queued: 1 }]);
    assert_eq!(scene.misses(), vec![Miss::LocType(4242)]);
    let queued = apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(5, 1, 0, 0),
        &src,
    );
    assert_eq!(queued, vec![SceneEffect::Deferred { queued: 2 }]);
    assert!(scene.ground_items().is_empty());
    assert!(scene.retry(&src).is_empty());
    src.locs.insert(
        4242,
        CustomizeCounts {
            models: 1,
            recolors: 1,
            retextures: 0,
        },
    );
    let effects = scene.retry(&src);
    assert_eq!(effects.len(), 2);
    assert_eq!(scene.deferred_count(), 0);
    let slot = &scene.locs()[&(0, 3209, 3209, 2)];
    assert_eq!(slot.change, LocChange::Retained);
    let custom = slot.custom.as_ref().unwrap();
    assert_eq!(custom.models, Some(vec![7]));
    assert_eq!(custom.recolors, Some(vec![9]));
    assert_eq!(scene.ground_items().len(), 1);
}

#[test]
fn garbage_is_counted_not_deferred() {
    let src = Source::empty();
    let mut scene = scene();
    let effects = apply(&mut scene, ServerPacket::CREATE_OBJECT, &[1, 2], &src);
    assert_eq!(effects, vec![SceneEffect::Dropped]);
    assert_eq!(scene.error_count(), 1);
    assert!(scene.last_error().is_some());
    assert_eq!(scene.deferred_count(), 0);
}

#[test]
fn map_change_resets_zone_state_but_not_entities() {
    let src = Source::empty();
    let mut scene = scene();
    open_zone(&mut scene, 8, 8, 0, &src);
    apply(
        &mut scene,
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(1, 1, 0, 0),
        &src,
    );
    scene.set_map(&layout((3300, 3300), LoadingType::Standard));
    assert!(scene.ground_items().is_empty());
    assert_eq!(scene.base(), (3300, 3300));
    assert_eq!(scene.zone_origin(), None);
    assert_eq!(scene.player_states.len(), 1);
}

#[test]
fn handles_covers_zone_entity_and_varbit_packets() {
    assert!(WorldScene::handles(ServerPacket::CREATE_OBJECT));
    assert!(WorldScene::handles(
        ServerPacket::UPDATE_ZONE_PARTIAL_ENCLOSED
    ));
    assert!(WorldScene::handles(ServerPacket::NPC_UPDATE));
    assert!(WorldScene::handles(ServerPacket::VARBIT_LARGE));
    assert!(!WorldScene::handles(ServerPacket::CLEAR_VARPS));
}

#[test]
fn player_block_updates_state() {
    let mut state = PlayerState::new(Some((10, 10)));
    apply_player_block(&mut state, PlayerBlock::Appearance(vec![1, 2, 3]), 1);
    apply_player_block(
        &mut state,
        PlayerBlock::MovementAnimations {
            ids: [1, 2, 3, 4],
            speed: 2,
        },
        1,
    );
    apply_player_block(&mut state, PlayerBlock::FaceDirection(1536), 1);
    assert_eq!(state.appearance, Some(vec![1, 2, 3]));
    assert_eq!(state.appearance_rev, 1);
    assert_eq!(state.movement_anims, [1, 2, 3, 4]);
    assert_eq!(state.orientation, 1536);
    state.moved_tick = Some(2);
    apply_player_block(&mut state, PlayerBlock::FaceDirection(512), 2);
    assert_eq!(state.orientation, 1536);
}

#[test]
fn npc_block_updates_state() {
    let mut state = NpcState::new(0);
    apply_npc_block(&mut state, NpcBlock::SetType(12));
    assert_eq!(state.type_override, Some(12));
    apply_npc_block(&mut state, NpcBlock::SetType(-1));
    assert_eq!(state.type_override, None);
    apply_npc_block(&mut state, NpcBlock::RenderAnim(77));
    assert_eq!(state.render_anim, Some(77));
    apply_npc_block(&mut state, NpcBlock::FaceTile { x: 5, y: 6 });
    assert_eq!(state.face_tile, Some((5, 6)));
}

#[test]
fn static_locs_seed_the_slots_and_never_overwrite_packet_changes() {
    use openrs_scene::locs::ScenePlacement;
    let mut scene = scene();
    let placement = |id, x, y, kind| ScenePlacement {
        id,
        plane: 0,
        scene_x: x,
        scene_y: y,
        kind,
        rotation: 1,
    };
    let key = |x: i32, y: i32, shape: u8| (0u8, 3200 + x, 3200 + y, loc_layer(shape));
    scene.locs.insert(
        key(5, 5, 10),
        LocSlot {
            change: LocChange::Removed,
            custom: None,
            animation: None,
            order: 1,
        },
    );
    scene.locs.insert(
        key(6, 6, 10),
        LocSlot {
            change: LocChange::Retained,
            custom: None,
            animation: Some(9),
            order: 2,
        },
    );
    let added = scene.add_static_locs(&[
        placement(100, 5, 5, 10),
        placement(101, 6, 6, 10),
        placement(102, 7, 7, 0),
        placement(-1, 8, 8, 10),
    ]);
    assert_eq!(added, 2);
    assert_eq!(scene.locs[&key(5, 5, 10)].change, LocChange::Removed);
    let upgraded = &scene.locs[&key(6, 6, 10)];
    assert_eq!(
        upgraded.change,
        LocChange::Added {
            id: 101,
            shape: 10,
            rotation: 1
        }
    );
    assert_eq!(upgraded.animation, Some(9));
    assert!(matches!(
        scene.locs[&key(7, 7, 0)].change,
        LocChange::Added { id: 102, .. }
    ));
}
