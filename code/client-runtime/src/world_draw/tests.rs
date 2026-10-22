use super::*;
use crate::world_resources::{INDEX_ITEM, INDEX_LOC, INDEX_MODELS, INDEX_NPC};
use openrs_login::world_state::{LoadingType, MapBase, MapLayout, MapSize};
use openrs_protocol::entity_info::OwnPlayer;
use openrs_protocol::registry::ServerPacket;

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

fn multi_loc_bytes(varp: u16, alternatives: [u16; 2]) -> Vec<u8> {
    let mut data = vec![77];
    data.extend_from_slice(&65535u16.to_be_bytes());
    data.extend_from_slice(&varp.to_be_bytes());
    data.push(1);
    for id in alternatives {
        data.extend_from_slice(&id.to_be_bytes());
    }
    data.push(0);
    data
}

fn item_bytes(model: u16) -> Vec<u8> {
    let mut data = vec![1];
    data.extend_from_slice(&model.to_be_bytes());
    data.push(0);
    data
}

fn provide_loc(store: &ResourceStore, id: u32, bytes: Vec<u8>) {
    store.provide_group(INDEX_LOC, id >> 8, vec![(id & 0xff, bytes)]);
}

fn provide_model(store: &ResourceStore, id: u32, bytes: Vec<u8>) {
    store.provide_group(INDEX_MODELS, id, vec![(0, bytes)]);
}

fn layout() -> MapLayout {
    MapLayout {
        map_size: MapSize::Default104,
        center_x: 0,
        center_y: 0,
        force_position_update: false,
        loading_type: LoadingType::Standard,
        base: MapBase { x: 3200, y: 3200 },
        chunks: None,
        region_ids: Vec::new(),
        xtea_keys: Vec::new(),
    }
}

fn scene(store: &ResourceStore) -> WorldScene {
    let mut scene = WorldScene::new(
        1,
        &layout(),
        Some(OwnPlayer {
            x: 3210,
            y: 3210,
            plane: 0,
        }),
    );
    scene.apply(
        ServerPacket::UPDATE_ZONE_PARTIAL_FOLLOWS,
        &[(24 >> 3) as u8 + 128, 128, (16 >> 3) as u8 + 128],
        store,
    );
    scene
}

fn tile_byte(x: u8, y: u8) -> u8 {
    (x << 4) | y
}

fn create_object(id: i32, shape: u8, rotation: u8, x: u8, y: u8) -> Vec<u8> {
    let mut out = id.to_be_bytes().to_vec();
    out.push(128u8.wrapping_sub(tile_byte(x, y)));
    out.push(128u8.wrapping_sub((shape << 2) | rotation));
    out
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

fn flat(height: i32) -> HeightMap {
    HeightMap::new(104, 1, vec![Some(height); 105 * 105]).expect("sized")
}

fn relative(mesh: &DrawMesh) -> Vec<[i32; 3]> {
    mesh.positions
        .chunks(3)
        .map(|p| {
            [
                p[0] - mesh.origin.0,
                p[1] - mesh.origin.1,
                p[2] - mesh.origin.2,
            ]
        })
        .collect()
}

#[test]
fn height_map_samples_bilinearly() {
    let mut heights = vec![Some(0); 105 * 105];
    heights[105] = Some(512);
    heights[106] = Some(512);
    let map = HeightMap::new(104, 1, heights).expect("sized");
    assert_eq!(map.sample(0, 0, 0), Some(0));
    assert_eq!(map.sample(0, 256, 0), Some(256));
    assert_eq!(map.sample(0, 512, 0), Some(512));
    assert_eq!(map.sample(0, 256, 256), Some(256));
    assert_eq!(map.sample(1, 0, 0), None);
    assert_eq!(map.sample(0, -1, 0), None);
    assert_eq!(map.sample(0, 104 * 512, 0), None);
}

#[test]
fn height_map_rejects_wrong_size_and_reports_gaps() {
    assert!(HeightMap::new(104, 1, vec![Some(0); 3]).is_none());
    assert!(HeightMap::default().is_empty());
    let mut heights = vec![Some(7); 105 * 105];
    heights[0] = None;
    let map = HeightMap::new(104, 1, heights).expect("sized");
    assert_eq!(map.sample(0, 0, 0), None);
    assert_eq!(map.sample(0, 600, 600), Some(7));
}

#[test]
fn loc_waits_for_cache_data_then_draws() {
    let store = ResourceStore::new();
    let mut scene = scene(&store);
    scene.apply(
        ServerPacket::CREATE_OBJECT,
        &create_object(1000, 10, 0, 3, 5),
        &store,
    );
    let mut builder = DrawBuilder::new(store.clone());
    let heights = flat(100);
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert!(draw.incomplete());
    assert!(draw.meshes.is_empty());
    assert!(draw.pending.contains(&(INDEX_LOC, 1000 >> 8)));

    provide_loc(&store, 1000, loc_bytes(50));
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert!(draw.pending.contains(&(INDEX_MODELS, 50)));
    assert!(draw.meshes.is_empty());

    provide_model(&store, 50, triangle_model());
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert!(!draw.incomplete());
    assert_eq!(draw.stats.locs, 1);
    assert_eq!(draw.triangles(), 1);
    let mesh = &draw.meshes[0];
    assert_eq!(mesh.kind, DrawKind::Loc);
    assert_eq!(mesh.tile, (3219, 3229));
    assert_eq!(mesh.origin, (19 * 512 + 256, 100, 29 * 512 + 256));
    let rel = relative(mesh);
    assert_eq!(rel[0], [0, 0, 0]);
    assert!(rel[1][0] > 0 && rel[1][2] == 0);
    assert!(rel[2][2] > 0 && rel[2][0] == rel[1][0]);
}

#[test]
fn loc_rotation_turns_a_quarter_per_step() {
    let store = ResourceStore::new();
    provide_loc(&store, 1000, loc_bytes(50));
    provide_model(&store, 50, triangle_model());
    let heights = flat(0);
    let mut results = Vec::new();
    for rotation in 0..4u8 {
        let mut scene = scene(&store);
        scene.apply(
            ServerPacket::CREATE_OBJECT,
            &create_object(1000, 10, rotation, 3, 5),
            &store,
        );
        let mut builder = DrawBuilder::new(store.clone());
        let draw = builder.build(&scene, &heights, &|_| 0);
        results.push(relative(&draw.meshes[0]));
    }
    let [dx, _, _] = results[0][1];
    let [_, _, dz] = results[0][2];
    assert!(dx > 0 && dz > 0);
    assert_eq!(results[1][1], [0, 0, -dx]);
    assert_eq!(results[1][2], [dz, 0, -dx]);
    assert_eq!(results[2][1], [-dx, 0, 0]);
    assert_eq!(results[3][1], [0, 0, dx]);
}

#[test]
fn multi_loc_follows_varp_and_hides_past_the_end() {
    let store = ResourceStore::new();
    provide_loc(&store, 1000, multi_loc_bytes(5, [1001, 1002]));
    provide_loc(&store, 1001, loc_bytes(50));
    provide_loc(&store, 1002, loc_bytes(51));
    provide_model(&store, 50, triangle_model());
    provide_model(&store, 51, triangle_model());
    let mut scene = scene(&store);
    scene.apply(
        ServerPacket::CREATE_OBJECT,
        &create_object(1000, 10, 0, 3, 5),
        &store,
    );
    let heights = flat(0);
    let mut builder = DrawBuilder::new(store.clone());
    let draw = builder.build(&scene, &heights, &|id| if id == 5 { 0 } else { -1 });
    assert_eq!(draw.stats.locs, 1);
    let draw = builder.build(&scene, &heights, &|id| if id == 5 { 1 } else { -1 });
    assert_eq!(draw.stats.locs, 1);
    let draw = builder.build(&scene, &heights, &|id| if id == 5 { 2 } else { -1 });
    assert_eq!(draw.stats.locs, 0);
    assert_eq!(draw.stats.skipped, 1);
    assert!(!draw.incomplete());
}

#[test]
fn ground_item_draws_at_tile_centre_and_counts_missing_heights() {
    let store = ResourceStore::new();
    store.provide_group(INDEX_ITEM, 995 >> 8, vec![(995 & 0xff, item_bytes(60))]);
    provide_model(&store, 60, triangle_model());
    let mut scene = scene(&store);
    scene.apply(
        ServerPacket::CREATE_GROUND_ITEM,
        &create_item(995, 40, 3, 5),
        &store,
    );
    let mut builder = DrawBuilder::new(store.clone());
    let draw = builder.build(&scene, &flat(64), &|_| 0);
    assert_eq!(draw.stats.ground_items, 1);
    assert_eq!(draw.stats.missing_heights, 0);
    let mesh = &draw.meshes[0];
    assert_eq!(mesh.kind, DrawKind::GroundItem);
    assert_eq!(mesh.id, 995);
    assert_eq!(mesh.origin, (19 * 512 + 256, 64, 29 * 512 + 256));

    let draw = builder.build(&scene, &HeightMap::default(), &|_| 0);
    assert_eq!(draw.stats.ground_items, 1);
    assert_eq!(draw.stats.missing_heights, 1);
    assert_eq!(draw.meshes[0].origin.1, 0);
}

fn spot_type_bytes(model: u8) -> Vec<u8> {
    vec![1, 0, model, 0]
}

fn gfx_scene(store: &ResourceStore) -> WorldScene {
    store.provide_group(
        crate::world_resources::INDEX_GFX,
        0,
        vec![(7, spot_type_bytes(60))],
    );
    provide_model(store, 60, triangle_model());
    scene(store)
}

#[test]
fn ground_spot_anim_draws_lifted_at_the_tile_centre() {
    let store = ResourceStore::new();
    let mut scene = gfx_scene(&store);
    let mut payload = vec![tile_byte(3, 5)];
    payload.extend_from_slice(&7u16.to_be_bytes());
    payload.extend_from_slice(&100u16.to_be_bytes());
    payload.extend_from_slice(&0u16.to_be_bytes());
    payload.push(0);
    scene.apply(ServerPacket::SPOT_ANIM, &payload, &store);
    let mut builder = DrawBuilder::new(store.clone());
    let draw = builder.build(&scene, &flat(64), &|_| 0);
    assert_eq!(draw.stats.spots, 1);
    let mesh = &draw.meshes[0];
    assert_eq!(mesh.kind, DrawKind::SpotAnim);
    assert_eq!(mesh.id, 7);
    assert_eq!(mesh.origin, (19 * 512 + 256, 64 - 100, 29 * 512 + 256));
    assert_eq!(mesh.triangles(), 1);
}

#[test]
fn projectile_draws_while_alive_and_stops_after_it_ends() {
    let store = ResourceStore::new();
    let mut scene = gfx_scene(&store);
    let mut payload = vec![(3 << 3) | 5, 0, 0];
    payload.extend_from_slice(&(-1i16).to_be_bytes());
    payload.extend_from_slice(&7u16.to_be_bytes());
    payload.extend_from_slice(&[10, 10]);
    payload.extend_from_slice(&0u16.to_be_bytes());
    payload.extend_from_slice(&30u16.to_be_bytes());
    payload.push(0);
    payload.extend_from_slice(&0u16.to_be_bytes());
    scene.apply(ServerPacket::MAP_PROJANIM, &payload, &store);
    let mut builder = DrawBuilder::new(store.clone());
    let draw = builder.build(&scene, &flat(64), &|_| 0);
    assert_eq!(draw.stats.projectiles, 1);
    let mesh = &draw.meshes[0];
    assert_eq!(mesh.kind, DrawKind::Projectile);
    assert_eq!(
        (mesh.origin.0, mesh.origin.2),
        (19 * 512 + 256, 29 * 512 + 256)
    );
    assert_eq!(mesh.origin.1, 64 - 160);
}

#[test]
fn draw_kind_codes_are_stable() {
    assert_eq!(DrawKind::SpotAnim.code(), 4);
    assert_eq!(DrawKind::Projectile.code(), 5);
    assert_eq!(DrawKind::Loc.code(), 0);
    assert_eq!(DrawKind::GroundItem.code(), 1);
    assert_eq!(DrawKind::Player.code(), 2);
    assert_eq!(DrawKind::Npc.code(), 3);
}

#[test]
fn resource_store_tracks_unknown_absent_and_present() {
    let store = ResourceStore::new();
    assert_eq!(store.lookup(7, 1, 0), Lookup::Unknown);
    assert_eq!(store.needs(), vec![(7, 1)]);
    let version = store.version();
    store.provide_absent(7, 1);
    assert_eq!(store.lookup(7, 1, 0), Lookup::Absent);
    assert!(store.needs().is_empty());
    assert!(store.version() > version);
    store.provide_group(7, 2, vec![(0, vec![9])]);
    assert_eq!(store.lookup(7, 2, 0), Lookup::Present(vec![9]));
    assert_eq!(store.lookup(7, 2, 1), Lookup::Absent);
    store.add_file(7, 3, 0, vec![1]);
    assert!(!store.is_loaded(7, 3));
    store.finish_group(7, 3);
    assert!(store.is_loaded(7, 3));
    store.clear();
    assert_eq!(store.lookup(7, 2, 0), Lookup::Unknown);
}

fn provide_npc(store: &ResourceStore, id: u32, bytes: Vec<u8>) {
    store.provide_group(INDEX_NPC, id >> 7, vec![(id & 0x7f, bytes)]);
}

#[test]
fn npc_morph_follows_varp_and_falls_to_the_last_alternative() {
    let store = ResourceStore::new();
    provide_npc(
        &store,
        1000,
        vec![106, 0xff, 0xff, 0, 7, 1, 0x07, 0xd0, 0x07, 0xd1, 0],
    );
    provide_npc(&store, 2000, vec![0]);
    provide_npc(&store, 2001, vec![0]);
    let scene = scene(&store);
    let mut builder = DrawBuilder::new(store.clone());
    let at = |builder: &mut DrawBuilder, value: i32| {
        builder
            .resolve_npc(1000, &scene, &|id| if id == 7 { value } else { -1 })
            .map(|ty| ty.id)
    };
    assert_eq!(at(&mut builder, 0), Some(2000));
    assert_eq!(at(&mut builder, 1), Some(2001));
    assert_eq!(at(&mut builder, 9), None);
    assert_eq!(at(&mut builder, -1), None);
    assert_eq!(
        builder.resolve_npc(2000, &scene, &|_| 0).map(|ty| ty.id),
        Some(2000)
    );
}

fn item_with(model: u16, value: i32, stackable: bool, extra: &[u8]) -> Vec<u8> {
    let mut data = vec![1];
    data.extend_from_slice(&model.to_be_bytes());
    data.push(12);
    data.extend_from_slice(&value.to_be_bytes());
    if stackable {
        data.push(11);
    }
    data.extend_from_slice(extra);
    data.push(0);
    data
}

fn provide_item(store: &ResourceStore, id: u32, bytes: Vec<u8>) {
    store.provide_group(INDEX_ITEM, id >> 8, vec![(id & 0xff, bytes)]);
}

fn ground(items: &[(u16, u32)]) -> Vec<crate::world_scene::GroundItem> {
    items
        .iter()
        .map(|(id, amount)| crate::world_scene::GroundItem {
            id: *id,
            amount: *amount,
        })
        .collect()
}

#[test]
fn ground_stack_orders_by_value_and_keeps_three_distinct_ids() {
    let store = ResourceStore::new();
    provide_item(&store, 1, item_with(60, 10, false, &[]));
    provide_item(&store, 2, item_with(60, 500, false, &[]));
    provide_item(&store, 3, item_with(60, 30, false, &[]));
    provide_item(&store, 4, item_with(60, 20, false, &[]));
    provide_item(&store, 5, item_with(60, 1, true, &[]));
    let mut builder = DrawBuilder::new(store);
    let stack = builder.ground_stack(&ground(&[(1, 1), (2, 1), (3, 1), (4, 1), (2, 7)]));
    assert_eq!(stack, vec![(1, 1), (3, 1), (2, 1)]);
    let stack = builder.ground_stack(&ground(&[(5, 100), (1, 1)]));
    assert_eq!(stack, vec![(1, 1), (5, 100)]);
    let stack = builder.ground_stack(&ground(&[(2, 1), (2, 9)]));
    assert_eq!(stack, vec![(2, 1)]);
}

#[test]
fn ground_item_resize_and_stack_substitution_use_the_stack_item_type() {
    let store = ResourceStore::new();
    provide_model(&store, 60, triangle_model());
    provide_model(&store, 61, triangle_model());
    let mut resize = vec![110];
    resize.extend_from_slice(&256u16.to_be_bytes());
    provide_item(&store, 10, item_with(60, 1, false, &[]));
    provide_item(&store, 11, item_with(61, 1, false, &resize));
    let mut stack = vec![100];
    stack.extend_from_slice(&11u16.to_be_bytes());
    stack.extend_from_slice(&5u16.to_be_bytes());
    provide_item(&store, 12, item_with(60, 1, true, &stack));
    let mut builder = DrawBuilder::new(store);
    let (_, plain) = builder.item_mesh(12, 4).expect("plain");
    assert_eq!(plain, [128; 3]);
    let (_, big) = builder.item_mesh(12, 5).expect("stacked");
    assert_eq!(big, [256, 128, 128]);
    let mut local = Local {
        xs: vec![3, -3],
        ys: vec![4, 5],
        zs: vec![1, 1],
        mirrored: false,
    };
    scale_local(&mut local, big);
    assert_eq!((local.xs, local.ys), (vec![6, -6], vec![4, 5]));
}

#[test]
fn ground_tint_is_stable_and_in_range() {
    let tint = ground_tint(0, 3200, 3200);
    assert_eq!(tint, ground_tint(0, 3200, 3200));
    assert!((32..36).contains(&tint[0]) && (3..5).contains(&tint[1]));
    assert!((16..19).contains(&tint[2]) && (0..20).contains(&tint[3]));
    let mut colors = [0x1234i16];
    tint_colors(&mut colors, [32, 3, 16, 0]);
    assert_eq!(colors, [0x1234]);
}

#[test]
fn npc_target_follows_the_morph_and_carries_cursors() {
    let store = ResourceStore::new();
    let mut buf = vec![2];
    buf.extend_from_slice(b"Guard\0");
    buf.extend_from_slice(&[
        95, 0, 21, 30, b'A', b't', b't', 0, 135, 2, 0, 9, 137, 0, 4, 0,
    ]);
    provide_npc(&store, 2000, buf);
    provide_npc(
        &store,
        1000,
        vec![106, 0xff, 0xff, 0, 7, 1, 0x07, 0xd0, 0x07, 0xd0, 0],
    );
    let scene = scene(&store);
    let mut builder = DrawBuilder::new(store.clone());
    let ty = builder.resolve_npc(1000, &scene, &|_| 0).expect("type");
    assert_eq!(ty.combat_level, 21);
    assert_eq!(ty.primary_cursor_index, 2);
    assert_eq!(ty.primary_cursor, 9);
    assert_eq!(ty.attack_cursor, 4);
    assert_eq!(ty.secondary_cursor, -1);
}

#[test]
fn ground_target_lists_every_item_by_value_with_tooltip_colours() {
    let store = ResourceStore::new();
    let mut coloured = vec![43, 0x00, 0x12, 0x34, 0x56];
    coloured.extend_from_slice(&[30]);
    coloured.extend_from_slice(b"Take\0");
    coloured.extend_from_slice(&[127, 1, 0, 7]);
    provide_item(&store, 1, item_with(50, 10, false, &coloured));
    provide_item(&store, 2, item_with(50, 99, false, &[16]));
    provide_item(&store, 3, item_with(50, 5, false, &[]));
    let mut scene = scene(&store);
    for item in [1u16, 2, 3] {
        scene.apply(
            ServerPacket::CREATE_GROUND_ITEM,
            &create_item(item, 1, 3, 5),
            &store,
        );
    }
    let mut builder = DrawBuilder::new(store.clone());
    let colours = InteractionColours {
        members: 111,
        free: 222,
    };
    let stack = builder
        .ground_target(&scene, (0, 3219, 3229), colours)
        .expect("stack");
    assert_eq!((stack.start_x, stack.start_z), (19, 29));
    let ids: Vec<i32> = stack.items.iter().map(|item| item.id).collect();
    assert_eq!(ids, vec![2, 1, 3]);
    assert_eq!(stack.items[0].color, 111);
    assert_eq!(stack.items[1].color, 0x0012_3456);
    assert_eq!(stack.items[2].color, 222);
    assert_eq!(stack.items[1].actions[0].as_deref(), Some("Take"));
    assert_eq!(stack.items[1].primary_cursor_index, 1);
    assert_eq!(stack.items[1].primary_cursor, 7);
    assert_eq!(stack.items[0].primary_cursor_index, -1);
}

fn shaped_loc_bytes(shape: u8, model: u16, extra: &[u8]) -> Vec<u8> {
    let mut data = vec![1, 1, shape, 1];
    data.extend_from_slice(&model.to_be_bytes());
    data.extend_from_slice(extra);
    data.push(0);
    data
}

fn wall_decoration_origin(wall_first: bool) -> (i32, i32, i32) {
    let store = ResourceStore::new();
    provide_loc(&store, 2000, shaped_loc_bytes(0, 50, &[28, 8]));
    provide_loc(&store, 2001, shaped_loc_bytes(4, 50, &[]));
    provide_model(&store, 50, triangle_model());
    let mut scene = scene(&store);
    let wall = create_object(2000, 0, 0, 3, 5);
    let decoration = create_object(2001, 5, 0, 3, 5);
    let order = if wall_first {
        [wall, decoration]
    } else {
        [decoration, wall]
    };
    for packet in order {
        scene.apply(ServerPacket::CREATE_OBJECT, &packet, &store);
    }
    let mut builder = DrawBuilder::new(store.clone());
    let draw = builder.build(&scene, &flat(0), &|_| 0);
    draw.meshes
        .iter()
        .find(|mesh| mesh.id == 2001)
        .expect("decoration drawn")
        .origin
}

#[test]
fn wall_decoration_offset_follows_the_stream_order() {
    let behind = wall_decoration_origin(true);
    let before = wall_decoration_origin(false);
    assert_ne!(behind, before);
    assert_eq!(behind.1, before.1);
}

#[test]
fn animated_loc_draws_while_its_frames_are_still_loading() {
    let store = ResourceStore::new();
    provide_loc(&store, 1000, shaped_loc_bytes(10, 50, &[24, 0, 9]));
    provide_model(&store, 50, triangle_model());
    let mut scene = scene(&store);
    scene.apply(
        ServerPacket::CREATE_OBJECT,
        &create_object(1000, 10, 0, 3, 5),
        &store,
    );
    let mut builder = DrawBuilder::new(store.clone());
    let heights = flat(0);
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert!(draw.meshes.is_empty());
    assert!(draw.pending.contains(&(crate::world_resources::INDEX_SEQUENCE, 0)));

    store.provide_absent(crate::world_resources::INDEX_SEQUENCE, 0);
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert_eq!(draw.stats.locs, 1);
    assert_eq!(draw.stats.posed, 0);

    let mut data = vec![1, 0, 2, 0, 2, 0, 0, 0, 1, 0];
    data.push(0);
    store.provide_group(crate::world_resources::INDEX_SEQUENCE, 0, vec![(9, data)]);
    builder.clear();
    let draw = builder.build(&scene, &heights, &|_| 0);
    assert_eq!(draw.stats.locs, 1);
    assert_eq!(draw.triangles(), 1);
}
