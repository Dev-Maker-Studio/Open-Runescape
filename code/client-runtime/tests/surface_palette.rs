use openrs_client_runtime::WorldTerrainProbe;
use openrs_client_runtime::floors::WorldFloorProbe;
use openrs_client_runtime::surface_palette::{
    compose_surface_tiles, compose_underlay_palette, scene_underlay_ids,
};
use openrs_scene::floor::Underlay;
use openrs_scene::palette::{PaletteCell, UnderlayLookup, calculate_underlay_palette};
use openrs_scene::surface::{NativeMapSize, StandardMapLayout, SurfaceHeights};
use openrs_scene::surface_tiles::SurfaceTile;
use openrs_scene::terrain::{DecodeLimits, MAP_SIZE, PLANES, TILE_COUNT, Terrain};
use std::collections::BTreeMap;
use whirlpool::{Digest, Whirlpool};

const REGIONS: [u16; 4] = [0, 1, 256, 257];

fn raw(bytes: &[u8]) -> Vec<u8> {
    let mut result = vec![0];
    result.extend_from_slice(&u32::try_from(bytes.len()).unwrap().to_be_bytes());
    result.extend_from_slice(bytes);
    result
}

fn reply(index: u8, id: u32, container: &[u8]) -> Vec<u8> {
    let mut result = vec![index];
    result.extend_from_slice(&id.to_be_bytes());
    result.extend_from_slice(&container[..5]);
    let mut offset = 10;
    for &byte in &container[5..] {
        if offset == 512 {
            result.push(255);
            offset = 1;
        }
        result.push(byte);
        offset += 1;
    }
    result
}

fn tile_id(region: u16, plane: usize, x: usize, y: usize) -> u8 {
    u8::try_from(1 + (usize::from(region) + plane * 3 + x / 7 + y / 9) % 8).unwrap()
}

fn attributes(region: u16, plane: usize, x: usize, y: usize) -> SurfaceTile {
    SurfaceTile {
        underlay_id: tile_id(region, plane, x, y),
        overlay_id: ((usize::from(region) + plane * 13 + x * 5 + y * 7) % 256) as u8,
        render_flags: ((usize::from(region) + plane + x + y) % 32 + 1) as u8,
        overlay_shape: ((plane + x + y) % 12) as u8,
        overlay_rotation: ((usize::from(region) + x + 3 * y) % 4) as u8,
    }
}

fn tiles(region: u16) -> Vec<u8> {
    let mut result = Vec::with_capacity(TILE_COUNT * 5);
    for plane in 0..PLANES {
        for x in 0..MAP_SIZE {
            for y in 0..MAP_SIZE {
                let tile = attributes(region, plane, x, y);
                result.extend_from_slice(&[
                    2 + tile.overlay_shape * 4 + tile.overlay_rotation,
                    tile.overlay_id,
                    49 + tile.render_flags,
                    81 + tile.underlay_id,
                    0,
                ]);
            }
        }
    }
    result
}

fn name_hash(id: u16) -> i32 {
    format!("m{}_{}", id >> 8, id & 255)
        .bytes()
        .fold(0i32, |hash, byte| {
            hash.wrapping_mul(31).wrapping_add(i32::from(byte))
        })
}

fn terrain_probe(requested: &[u16], present: &[u16]) -> WorldTerrainProbe {
    let archives = present
        .iter()
        .enumerate()
        .map(|(at, &region)| {
            (
                100 + u32::try_from(at).unwrap(),
                (region, raw(&tiles(region))),
            )
        })
        .collect::<BTreeMap<_, _>>();
    let mut table = vec![6];
    table.extend_from_slice(&727i32.to_be_bytes());
    table.push(3);
    table.extend_from_slice(&u16::try_from(archives.len()).unwrap().to_be_bytes());
    let mut prior = 0;
    for &id in archives.keys() {
        table.extend_from_slice(&u16::try_from(id - prior).unwrap().to_be_bytes());
        prior = id;
    }
    for (region, _) in archives.values() {
        table.extend_from_slice(&name_hash(*region).to_be_bytes());
    }
    for (_, container) in archives.values() {
        table.extend_from_slice(&crc32fast::hash(container).to_be_bytes());
    }
    for (_, container) in archives.values() {
        table.extend_from_slice(&Whirlpool::digest(container));
    }
    for _ in archives.values() {
        table.extend_from_slice(&44i32.to_be_bytes());
    }
    for _ in archives.values() {
        table.extend_from_slice(&1u16.to_be_bytes());
    }
    for _ in archives.values() {
        table.extend_from_slice(&0u16.to_be_bytes());
    }
    for _ in archives.values() {
        table.extend_from_slice(&0i32.to_be_bytes());
    }
    let reference = raw(&table);
    let mut version = vec![6];
    version.extend_from_slice(&[0; 5 * 72]);
    version.extend_from_slice(&crc32fast::hash(&reference).to_be_bytes());
    version.extend_from_slice(&727i32.to_be_bytes());
    version.extend_from_slice(&Whirlpool::digest(&reference));
    version.extend_from_slice(&[0x5a; 128]);
    let mut wire = vec![0; 109];
    wire.extend_from_slice(&reply(255, 255, &raw(&version)));
    wire.extend_from_slice(&reply(255, 5, &reference));
    for region in requested {
        if let Some((&id, (_, container))) = archives.iter().find(|(_, (id, _))| id == region) {
            wire.extend_from_slice(&reply(5, id, container));
        }
    }
    let mut probe = WorldTerrainProbe::new(b"synthetic", requested).unwrap();
    for chunk in wire.chunks(511) {
        probe.feed(chunk).unwrap();
    }
    probe.finish().unwrap();
    probe
}

fn floor_probe() -> WorldFloorProbe {
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    for chunk in include_bytes!("fixtures/floors-native.wire").chunks(7) {
        probe.feed(chunk).unwrap();
    }
    probe.finish().unwrap();
    probe
}

fn layout() -> StandardMapLayout {
    StandardMapLayout::new([56, 56], NativeMapSize::Tiles72, &REGIONS).unwrap()
}

fn surface(terrain: &WorldTerrainProbe, unavailable: &[u16]) -> SurfaceHeights {
    let report = terrain.report().unwrap();
    let present = report
        .regions
        .iter()
        .filter(|region| region.present)
        .map(|region| (region.region_id, terrain.terrain(region.region_id).unwrap()))
        .collect::<Vec<_>>();
    let absent = report
        .regions
        .iter()
        .filter(|region| !region.present && !unavailable.contains(&region.region_id))
        .map(|region| region.region_id)
        .collect::<Vec<_>>();
    SurfaceHeights::compose(&layout(), &present, &absent).unwrap()
}

fn expected_ids(present: &[u16], unavailable: &[u16]) -> Vec<Option<u8>> {
    let size = 72;
    let mut result = Vec::with_capacity(PLANES * size * size);
    for plane in 0..PLANES {
        for x in 0..size {
            for y in 0..size {
                let world_x = 56 + x;
                let world_y = 56 + y;
                let region = u16::try_from((world_x / 64) * 256 + world_y / 64).unwrap();
                result.push(if present.contains(&region) {
                    Some(tile_id(region, plane, world_x % 64, world_y % 64))
                } else if unavailable.contains(&region) {
                    None
                } else {
                    Some(0)
                });
            }
        }
    }
    result
}

fn expected_palette(ids: &[Option<u8>]) -> openrs_scene::palette::UnderlayPalette {
    let defined =
        Underlay::decode(&[1, 0x80, 0x20, 0x10, 2, 0xff, 0xff, 3, 0, 7, 4, 5, 0]).unwrap();
    let native_default_file = Underlay::default();
    calculate_underlay_palette(NativeMapSize::Tiles72, ids, |id| match id {
        0 => UnderlayLookup::Present(&defined),
        3 => UnderlayLookup::Present(&native_default_file),
        _ => UnderlayLookup::ProvenAbsent,
    })
    .unwrap()
}

#[test]
fn composed_verified_tiles_preserve_all_raw_attributes_and_conservative_availability() {
    for (present, unavailable) in [
        (REGIONS.as_slice(), [].as_slice()),
        ([0, 1, 256].as_slice(), [].as_slice()),
        ([0, 1, 256].as_slice(), [257].as_slice()),
    ] {
        let terrain = terrain_probe(&REGIONS, present);
        let surface = surface(&terrain, unavailable);
        let tiles = compose_surface_tiles(&terrain, &surface).unwrap();
        assert_eq!(tiles.tiles().len(), PLANES * 72 * 72);
        assert_eq!(tiles.layout(), surface.layout());
        for plane in 0..PLANES {
            for x in 0..72 {
                for y in 0..72 {
                    let source = surface.source_tile(x, y).unwrap();
                    let expected = if present.contains(&source.region_id) {
                        Some(attributes(
                            source.region_id,
                            plane,
                            source.region_tile[0],
                            source.region_tile[1],
                        ))
                    } else if unavailable.contains(&source.region_id) {
                        None
                    } else {
                        Some(SurfaceTile::default())
                    };
                    assert_eq!(tiles.tile(plane, x, y).copied(), expected);
                    assert_eq!(tiles.tiles()[(plane * 72 + x) * 72 + y], expected);
                }
            }
        }
        for [plane, x, y] in [[4, 0, 0], [0, 72, 0], [0, 0, 72], [usize::MAX; 3]] {
            assert_eq!(tiles.tile(plane, x, y), None);
        }
        assert!(terrain.complete());
    }
}

#[test]
fn verified_region_clipping_and_four_region_seams_preserve_every_plane_source_id() {
    let terrain = terrain_probe(&REGIONS, &REGIONS);
    let floors = floor_probe();
    let surface = surface(&terrain, &[]);
    let expected = expected_ids(&REGIONS, &[]);
    let ids = scene_underlay_ids(&terrain, &surface).unwrap();
    assert_eq!(ids, expected);
    for plane in 0..PLANES {
        for [x, y] in [[0, 0], [7, 7], [8, 7], [7, 8], [8, 8], [71, 71]] {
            let source = surface.source_tile(x, y).unwrap();
            assert_eq!(
                ids[(plane * 72 + x) * 72 + y],
                Some(tile_id(
                    source.region_id,
                    plane,
                    source.region_tile[0],
                    source.region_tile[1]
                ))
            );
        }
    }
    let palette =
        compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).unwrap();
    assert_eq!(palette, expected_palette(&expected));
    assert!(palette.unavailable_definition_ids().is_empty());
    assert!(
        palette
            .cells()
            .iter()
            .all(|cell| matches!(cell, PaletteCell::Known(_)))
    );
    assert!(
        palette
            .cells()
            .iter()
            .any(|cell| matches!(cell, PaletteCell::Known(color) if *color != 0))
    );
    let incorrectly_dropped_absent = ids
        .iter()
        .map(|id| id.map(|id| if id == 1 || id == 4 { id } else { 0 }))
        .collect::<Vec<_>>();
    assert_ne!(palette, expected_palette(&incorrectly_dropped_absent));
    assert!(terrain.complete() && floors.complete());
}

#[test]
fn verified_reference_absent_regions_become_zero_tile_ids_not_unavailable_definitions() {
    let present = [0, 1, 256];
    let terrain = terrain_probe(&REGIONS, &present);
    let floors = floor_probe();
    let surface = surface(&terrain, &[]);
    let expected = expected_ids(&present, &[]);
    assert_eq!(scene_underlay_ids(&terrain, &surface).unwrap(), expected);
    for plane in 0..PLANES {
        for x in 8..72 {
            for y in 8..72 {
                assert_eq!(expected[(plane * 72 + x) * 72 + y], Some(0));
            }
        }
    }
    let palette =
        compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).unwrap();
    assert_eq!(palette, expected_palette(&expected));
    assert!(palette.unavailable_definition_ids().is_empty());
    assert!(
        palette
            .cells()
            .iter()
            .all(|cell| matches!(cell, PaletteCell::Known(_)))
    );
    assert!(!terrain.report().unwrap().authenticated);
}

#[test]
fn unclassified_surface_regions_stay_none_and_taint_palette_windows_without_fake_defaults() {
    let present = [0, 1, 256];
    let terrain = terrain_probe(&REGIONS, &present);
    let floors = floor_probe();
    let surface = surface(&terrain, &[257]);
    let expected = expected_ids(&present, &[257]);
    assert_eq!(scene_underlay_ids(&terrain, &surface).unwrap(), expected);
    let palette =
        compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).unwrap();
    assert_eq!(palette, expected_palette(&expected));
    for plane in 0..PLANES {
        assert_eq!(expected[(plane * 72 + 40) * 72 + 40], None);
        assert_eq!(palette.cell(plane, 40, 40), Some(PaletteCell::Unavailable));
        assert!(matches!(
            palette.cell(plane, 0, 0),
            Some(PaletteCell::Known(_))
        ));
    }
    assert!(palette.unavailable_definition_ids().is_empty());
}

#[test]
fn mismatched_surface_regions_order_or_availability_are_rejected_without_mutating_probes() {
    let terrain = terrain_probe(&REGIONS, &REGIONS);
    let floors = floor_probe();
    let other_layout =
        StandardMapLayout::new([120, 56], NativeMapSize::Tiles72, &[256, 257, 512, 513]).unwrap();
    let other_surface = SurfaceHeights::compose(&other_layout, &[], &[]).unwrap();
    assert_eq!(
        scene_underlay_ids(&terrain, &other_surface).unwrap_err(),
        "surface terrain identities do not match"
    );
    let unclassified = SurfaceHeights::compose(&layout(), &[], &[]).unwrap();
    assert_eq!(
        compose_underlay_palette(&terrain, &unclassified, floors.repository().unwrap())
            .unwrap_err(),
        "surface terrain identities do not match"
    );
    let reordered = terrain_probe(&[1, 0, 256, 257], &REGIONS);
    assert_eq!(
        scene_underlay_ids(&reordered, &surface(&terrain, &[])).unwrap_err(),
        "surface terrain identities do not match"
    );
    let absent = terrain_probe(&REGIONS, &[0, 1, 256]);
    let fabricated = Terrain::decode(
        &vec![0; TILE_COUNT],
        DecodeLimits {
            max_input_bytes: TILE_COUNT,
            max_tags_per_tile: 1,
        },
    )
    .unwrap();
    let classified_present =
        SurfaceHeights::compose(&layout(), &[(257, &fabricated)], &[0, 1, 256]).unwrap();
    assert_eq!(
        scene_underlay_ids(&absent, &classified_present).unwrap_err(),
        "surface terrain identities do not match"
    );
    assert!(terrain.complete() && reordered.complete() && absent.complete() && floors.complete());
    let valid = compose_underlay_palette(
        &terrain,
        &surface(&terrain, &[]),
        floors.repository().unwrap(),
    )
    .unwrap();
    assert_eq!(valid.map_size(), NativeMapSize::Tiles72);
}

#[test]
fn incomplete_or_failed_terrain_never_produces_scene_ids_or_palette() {
    let floors = floor_probe();
    let surface = SurfaceHeights::compose(&layout(), &[], &[]).unwrap();
    let mut terrain = WorldTerrainProbe::new(b"synthetic", &REGIONS).unwrap();
    assert_eq!(
        scene_underlay_ids(&terrain, &surface).unwrap_err(),
        "world terrain not complete"
    );
    assert_eq!(
        compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).unwrap_err(),
        "world terrain not complete"
    );
    assert!(terrain.finish().is_err());
    assert!(scene_underlay_ids(&terrain, &surface).is_err());
    assert!(compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).is_err());
    assert!(floors.complete());
}
