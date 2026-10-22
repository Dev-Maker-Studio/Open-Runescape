use crc32fast::hash as crc32;
use openrs_client_runtime::WorldTerrainProbe;
use openrs_client_runtime::floors::WorldFloorProbe;
use openrs_client_runtime::materials::WorldMaterialProbe;
use openrs_client_runtime::surface_ground::{
    SurfaceGroundInputs, compose_surface_ground, overlay_render_color,
};
use openrs_client_runtime::surface_palette::{compose_surface_tiles, compose_underlay_palette};
use openrs_scene::floor::Overlay;
use openrs_scene::ground_geometry::{
    GroundGeometry, GroundGeometryInput, GroundGeometryOptions, MAX_OUTPUT_VERTICES,
    build_blended_ground,
};
use openrs_scene::material::MaterialTable;
use openrs_scene::palette::{UnderlayLookup, UnderlayPalette, calculate_underlay_palette};
use openrs_scene::surface::{NativeMapSize, StandardMapLayout, SurfaceHeights};
use openrs_scene::surface_tiles::SurfaceTiles;
use openrs_scene::terrain::{DecodeLimits, MAP_SIZE, PLANES, Terrain};
use serde_json::{Value, from_str};
use whirlpool::{Digest, Whirlpool};

const REGIONS: [u16; 4] = [0, 1, 256, 257];
const SIZE: usize = 72;

fn raw(data: &[u8]) -> Vec<u8> {
    let mut bytes = vec![0];
    bytes.extend_from_slice(&u32::try_from(data.len()).unwrap().to_be_bytes());
    bytes.extend_from_slice(data);
    bytes
}

fn reply(index: u8, archive: u32, container: &[u8]) -> Vec<u8> {
    let mut bytes = vec![index];
    bytes.extend_from_slice(&archive.to_be_bytes());
    bytes.extend_from_slice(&container[..5]);
    let mut offset = 10;
    for &byte in &container[5..] {
        if offset == 512 {
            bytes.push(255);
            offset = 1;
        }
        bytes.push(byte);
        offset += 1;
    }
    bytes
}

struct Archive {
    id: u16,
    name: Option<i32>,
    files: Vec<(u16, Vec<u8>)>,
}

fn wire(index: u8, archives: &[Archive]) -> Vec<u8> {
    let containers = archives
        .iter()
        .map(|archive| {
            let mut bytes = archive
                .files
                .iter()
                .flat_map(|(_, file)| file.iter().copied())
                .collect::<Vec<_>>();
            if archive.files.len() > 1 {
                let mut prior = 0i32;
                for (_, file) in &archive.files {
                    let length = i32::try_from(file.len()).unwrap();
                    bytes.extend_from_slice(&(length - prior).to_be_bytes());
                    prior = length;
                }
                bytes.push(1);
            }
            raw(&bytes)
        })
        .collect::<Vec<_>>();
    let named = archives.iter().all(|archive| archive.name.is_some());
    let mut reference = vec![6];
    reference.extend_from_slice(&727i32.to_be_bytes());
    reference.push(if named { 3 } else { 2 });
    reference.extend_from_slice(&u16::try_from(archives.len()).unwrap().to_be_bytes());
    let mut prior = 0;
    for archive in archives {
        reference.extend_from_slice(&(archive.id - prior).to_be_bytes());
        prior = archive.id;
    }
    if named {
        for archive in archives {
            reference.extend_from_slice(&archive.name.unwrap().to_be_bytes());
        }
    }
    for container in &containers {
        reference.extend_from_slice(&crc32(container).to_be_bytes());
    }
    for container in &containers {
        reference.extend_from_slice(&Whirlpool::digest(container));
    }
    for _ in archives {
        reference.extend_from_slice(&44i32.to_be_bytes());
    }
    for archive in archives {
        reference.extend_from_slice(&u16::try_from(archive.files.len()).unwrap().to_be_bytes());
    }
    for archive in archives {
        let mut prior = 0;
        for &(id, _) in &archive.files {
            reference.extend_from_slice(&(id - prior).to_be_bytes());
            prior = id;
        }
    }
    if named {
        for archive in archives {
            for _ in &archive.files {
                reference.extend_from_slice(&0i32.to_be_bytes());
            }
        }
    }
    let reference = raw(&reference);
    let mut version = vec![index + 1];
    version.resize(1 + usize::from(index) * 72, 0);
    version.extend_from_slice(&crc32(&reference).to_be_bytes());
    version.extend_from_slice(&727i32.to_be_bytes());
    version.extend_from_slice(&Whirlpool::digest(&reference));
    version.extend_from_slice(&[0x5a; 128]);
    let mut bytes = vec![0; 109];
    bytes.extend_from_slice(&reply(255, 255, &raw(&version)));
    bytes.extend_from_slice(&reply(255, u32::from(index), &reference));
    for (archive, container) in archives.iter().zip(containers) {
        bytes.extend_from_slice(&reply(index, u32::from(archive.id), &container));
    }
    bytes
}

fn terrain_bytes(region: u16, changed_tiles: bool, changed_heights: bool) -> Vec<u8> {
    let mut bytes = Vec::new();
    for plane in 0..PLANES {
        for x in 0..MAP_SIZE {
            for y in 0..MAP_SIZE {
                let pattern = (usize::from(region) + plane * 3 + x * 5 + y * 7) % 7;
                let (underlay, overlay) = match pattern {
                    0 => (0, 0),
                    1 => (1, 0),
                    2 => (1, 1),
                    3 => (4, 2),
                    4 => (1, 3),
                    5 => (8, 4),
                    _ => (4, 5),
                };
                let shape = if pattern == 1 {
                    0
                } else {
                    (plane + x + y) % 12
                };
                let rotation = (usize::from(region) + x * 3 + y) % 4;
                bytes.extend_from_slice(&[
                    2 + shape as u8 * 4 + rotation as u8,
                    if changed_tiles { 0 } else { overlay },
                    49 + ((x + y + plane) % 32 + 1) as u8,
                ]);
                if underlay != 0 {
                    bytes.push(81 + underlay);
                }
                bytes.extend_from_slice(&[1, if changed_heights { 91 } else { 3 + plane as u8 }]);
            }
        }
    }
    bytes
}

fn terrain_probe() -> WorldTerrainProbe {
    let archives = REGIONS
        .iter()
        .enumerate()
        .map(|(at, &region)| Archive {
            id: 100 + at as u16,
            name: Some(
                format!("m{}_{}", region >> 8, region & 255)
                    .bytes()
                    .fold(0i32, |hash, byte| {
                        hash.wrapping_mul(31).wrapping_add(i32::from(byte))
                    }),
            ),
            files: vec![(0, terrain_bytes(region, false, false))],
        })
        .collect::<Vec<_>>();
    let mut probe = WorldTerrainProbe::new(b"synthetic", &REGIONS).unwrap();
    for chunk in wire(5, &archives).chunks(511) {
        probe.feed(chunk).unwrap();
    }
    probe.finish().unwrap();
    assert!(probe.complete());
    probe
}

fn floor_probe(missing_material: bool) -> WorldFloorProbe {
    let mut material_overlay = vec![1, 0x12, 0x34, 0x56, 3];
    material_overlay
        .extend_from_slice(&(if missing_material { 65534u16 } else { 0 }).to_be_bytes());
    material_overlay.extend_from_slice(&[12, 0]);
    let archives = [
        Archive {
            id: 1,
            name: None,
            files: vec![(0, vec![1, 0x80, 0x20, 0x10, 3, 0, 7, 4, 0]), (3, vec![0])],
        },
        Archive {
            id: 4,
            name: None,
            files: vec![
                (0, vec![1, 0xff, 0, 0xff, 3, 0xff, 0xfe, 0]),
                (1, material_overlay),
                (2, vec![1, 0xff, 0x80, 0, 2, 1, 12, 10, 0]),
                (
                    3,
                    vec![
                        1, 0x20, 0x30, 0x40, 3, 0xff, 0xfe, 7, 0xab, 0xcd, 0xef, 12, 0,
                    ],
                ),
                (4, vec![1, 0x10, 0x20, 0x30, 9, 0, 3, 10, 0]),
            ],
        },
    ];
    let mut probe = WorldFloorProbe::new(b"synthetic").unwrap();
    for chunk in wire(2, &archives).chunks(7) {
        probe.feed(chunk).unwrap();
    }
    probe.finish().unwrap();
    assert!(probe.complete());
    probe
}

fn material_bytes(materials: &[(u16, bool, i16)]) -> Vec<u8> {
    let count = materials
        .iter()
        .map(|&(id, _, _)| id)
        .max()
        .map_or(0, |id| id + 1);
    let mut bytes = count.to_be_bytes().to_vec();
    for id in 0..count {
        bytes.push(u8::from(materials.iter().any(|&(at, _, _)| at == id)));
    }
    for &(_, details_only, _) in materials {
        bytes.push(u8::from(!details_only));
    }
    bytes.resize(bytes.len() + 6 * materials.len(), 0);
    for &(_, _, color) in materials {
        bytes.extend_from_slice(&color.to_be_bytes());
    }
    bytes.resize(bytes.len() + 14 * materials.len(), 0);
    bytes
}

fn material_probe() -> WorldMaterialProbe {
    let archives = [Archive {
        id: 0,
        name: None,
        files: vec![(0, material_bytes(&[(0, false, -32477), (1, true, -1234)]))],
    }];
    let mut probe = WorldMaterialProbe::new(b"synthetic").unwrap();
    for chunk in wire(26, &archives).chunks(13) {
        probe.feed(chunk).unwrap();
    }
    probe.finish().unwrap();
    assert!(probe.complete());
    probe
}

fn layout(base: [i32; 2]) -> StandardMapLayout {
    StandardMapLayout::new(base, NativeMapSize::Tiles72, &REGIONS).unwrap()
}

fn surface_from(terrain: &WorldTerrainProbe, base: [i32; 2]) -> SurfaceHeights {
    let present = REGIONS
        .iter()
        .map(|&id| (id, terrain.terrain(id).unwrap()))
        .collect::<Vec<_>>();
    SurfaceHeights::compose(&layout(base), &present, &[]).unwrap()
}

struct Ready {
    terrain: WorldTerrainProbe,
    surface: SurfaceHeights,
    tiles: SurfaceTiles,
    palette: UnderlayPalette,
    floors: WorldFloorProbe,
    materials: WorldMaterialProbe,
}

impl Ready {
    fn new() -> Self {
        let terrain = terrain_probe();
        let floors = floor_probe(false);
        let materials = material_probe();
        let surface = surface_from(&terrain, [56, 56]);
        let tiles = compose_surface_tiles(&terrain, &surface).unwrap();
        let palette =
            compose_underlay_palette(&terrain, &surface, floors.repository().unwrap()).unwrap();
        Self {
            terrain,
            surface,
            tiles,
            palette,
            floors,
            materials,
        }
    }

    fn inputs(&self) -> SurfaceGroundInputs<'_> {
        SurfaceGroundInputs {
            terrain: &self.terrain,
            surface: &self.surface,
            tiles: &self.tiles,
            palette: &self.palette,
            floors: &self.floors,
            materials: &self.materials,
        }
    }

    fn direct(&self, plane: usize) -> GroundGeometry {
        let mut underlays = vec![None; 255];
        let mut overlays = vec![None; 255];
        let mut rendered = vec![None; 255];
        let start = plane * SIZE * SIZE;
        let end = start + SIZE * SIZE;
        let tiles = &self.tiles.tiles()[start..end];
        for tile in tiles.iter().flatten() {
            if let Some(id) = tile.underlay_id.checked_sub(1) {
                underlays[usize::from(id)] =
                    Some(*self.floors.underlay(u32::from(id)).unwrap().definition());
            }
            if let Some(id) = tile.overlay_id.checked_sub(1) {
                let overlay = *self.floors.overlay(u32::from(id)).unwrap().definition();
                let color = if overlay.tile_color_hsl == -1 && overlay.minimap_color_hsl == -1 {
                    -1
                } else if overlay.minimap_color_hsl != -1 {
                    overlay.minimap_color_hsl
                } else if overlay.texture != -1 {
                    let material = self.materials.material(overlay.texture).unwrap();
                    if material.details_only {
                        overlay.tile_color_hsl
                    } else {
                        i32::from(material.color_hsl)
                    }
                } else {
                    overlay.tile_color_hsl
                };
                overlays[usize::from(id)] = Some(overlay);
                rendered[usize::from(id)] = Some(color);
            }
        }
        let points = (SIZE + 1) * (SIZE + 1);
        build_blended_ground(
            GroundGeometryInput {
                width: SIZE,
                height: SIZE,
                tiles,
                ground_heights: &self.surface.heights_native_units()
                    [plane * points..(plane + 1) * points],
                palette: &self.palette.cells()[start..end],
                underlays: &underlays,
                overlays: &overlays,
                overlay_render_colors: &rendered,
            },
            GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
        )
        .unwrap()
    }
}

#[test]
fn verified_adapter_matches_direct_geometry_for_all_planes_and_material_branches() {
    let ready = Ready::new();
    for plane in 0..PLANES {
        let result = compose_surface_ground(
            ready.inputs(),
            plane,
            GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
        )
        .unwrap();
        assert_eq!(result, ready.direct(plane));
        assert!(result.vertex_count > 0);
        assert_eq!(
            result.vertex_count,
            result
                .tiles
                .iter()
                .map(|tile| tile.vertices.len())
                .sum::<usize>()
        );
        assert!(
            result
                .tiles
                .iter()
                .flat_map(|tile| &*tile.vertices)
                .any(|vertex| vertex.texture == 0)
        );
        assert!(
            result
                .tiles
                .iter()
                .flat_map(|tile| tile.blending_colors.iter().flat_map(|colors| &**colors))
                .any(|&color| color == -32477)
        );
        assert!(
            result
                .tiles
                .windows(2)
                .all(|pair| (pair[0].x, pair[0].y) < (pair[1].x, pair[1].y))
        );
    }
    assert!(ready.floors.overlay(0).unwrap().definition().texture > 65000);
    assert!(ready.materials.material(65534).is_err());
    assert!(ready.terrain.complete() && ready.floors.complete() && ready.materials.complete());
}

#[test]
fn overlay_render_colors_match_the_pinned_native_ground_fixture() {
    let native: Value = from_str(include_str!(
        "../../scene/tests/fixtures/ground-geometry.json"
    ))
    .unwrap();
    assert_eq!(
        native["jar_sha256"],
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    let input = &native["cases"][0]["input"];
    let materials = input["materials"]
        .as_array()
        .unwrap()
        .iter()
        .map(|material| {
            (
                material["id"].as_u64().unwrap() as u16,
                material["details_only"].as_bool().unwrap(),
                material["color_hsl"].as_u64().unwrap() as u16 as i16,
            )
        })
        .collect::<Vec<_>>();
    let table = MaterialTable::decode(&material_bytes(&materials)).unwrap();
    let overlays = input["overlays"].as_array().unwrap();
    assert_eq!(overlays.len(), 8);
    for definition in overlays {
        let overlay = Overlay {
            tile_color_hsl: definition["tile_color"].as_i64().unwrap() as i32,
            minimap_color_hsl: definition["minimap_color"].as_i64().unwrap() as i32,
            texture: definition["texture"].as_i64().unwrap() as i32,
            ..Overlay::default()
        };
        assert_eq!(
            overlay_render_color(&overlay, &table).unwrap(),
            definition["rendered_color"].as_i64().unwrap() as i32,
            "overlay {}",
            definition["id"]
        );
    }
}

#[test]
fn overlay_render_color_short_circuits_minimap_and_untextured_colors() {
    let table =
        MaterialTable::decode(&material_bytes(&[(0, false, i16::MIN), (1, true, -1)])).unwrap();
    let overlay = Overlay {
        tile_color_hsl: 0x7fff,
        minimap_color_hsl: 0x1234,
        texture: i32::MAX,
        ..Overlay::default()
    };
    assert_eq!(overlay_render_color(&overlay, &table).unwrap(), 0x1234);
    let overlay = Overlay {
        minimap_color_hsl: -1,
        texture: -1,
        ..overlay
    };
    assert_eq!(overlay_render_color(&overlay, &table).unwrap(), 0x7fff);
    assert_eq!(
        overlay_render_color(
            &Overlay {
                texture: 0,
                ..overlay
            },
            &table
        )
        .unwrap(),
        -32768
    );
    assert_eq!(
        overlay_render_color(
            &Overlay {
                texture: 1,
                ..overlay
            },
            &table
        )
        .unwrap(),
        0x7fff
    );
    assert_eq!(
        overlay_render_color(
            &Overlay {
                tile_color_hsl: -1,
                ..overlay
            },
            &table
        )
        .unwrap(),
        -1
    );
    for texture in [-2, 2, i32::MAX] {
        assert!(overlay_render_color(&Overlay { texture, ..overlay }, &table).is_err());
    }
    let sparse = MaterialTable::decode(&material_bytes(&[(1, false, 0)])).unwrap();
    assert!(
        overlay_render_color(
            &Overlay {
                texture: 0,
                ..overlay
            },
            &sparse
        )
        .is_err()
    );
}

#[test]
fn incomplete_and_failed_probes_never_publish_surface_ground() {
    let ready = Ready::new();
    let terrain = WorldTerrainProbe::new(b"synthetic", &REGIONS).unwrap();
    let floors = WorldFloorProbe::new(b"synthetic").unwrap();
    let materials = WorldMaterialProbe::new(b"synthetic").unwrap();
    let options = GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES);
    assert_eq!(
        compose_surface_ground(
            SurfaceGroundInputs {
                terrain: &terrain,
                ..ready.inputs()
            },
            0,
            options
        )
        .unwrap_err(),
        "world terrain not complete"
    );
    assert_eq!(
        compose_surface_ground(
            SurfaceGroundInputs {
                floors: &floors,
                ..ready.inputs()
            },
            0,
            options
        )
        .unwrap_err(),
        "floor probe not complete"
    );
    assert_eq!(
        compose_surface_ground(
            SurfaceGroundInputs {
                materials: &materials,
                ..ready.inputs()
            },
            0,
            options
        )
        .unwrap_err(),
        "material probe not complete"
    );
    let mut materials = material_probe();
    assert!(materials.feed(&[1]).is_err());
    assert!(!materials.complete());
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                materials: &materials,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
}

#[test]
fn invalid_planes_and_unsupported_options_fail_before_probe_work() {
    let ready = Ready::new();
    let terrain = WorldTerrainProbe::new(b"synthetic", &REGIONS).unwrap();
    let input = SurfaceGroundInputs {
        terrain: &terrain,
        ..ready.inputs()
    };
    for plane in [PLANES, usize::MAX] {
        assert_eq!(
            compose_surface_ground(
                input,
                plane,
                GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES)
            )
            .unwrap_err(),
            "ground geometry plane out of bounds"
        );
    }
    let mut options = GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES);
    options.overlay_hidden = true;
    assert_eq!(
        compose_surface_ground(input, 0, options).unwrap_err(),
        "underwater ground geometry is unsupported"
    );
    options.overlay_hidden = false;
    options.secondary_ground_heights = true;
    assert_eq!(
        compose_surface_ground(input, 0, options).unwrap_err(),
        "secondary ground height adjustments are unsupported"
    );
}

#[test]
fn shifted_tile_layout_and_wrong_palette_dimensions_are_rejected() {
    let ready = Ready::new();
    let shifted = surface_from(&ready.terrain, [48, 56]);
    let tiles = compose_surface_tiles(&ready.terrain, &shifted).unwrap();
    let options = GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES);
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                tiles: &tiles,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
    let palette = calculate_underlay_palette(
        NativeMapSize::Tiles104,
        &vec![Some(0); PLANES * 104 * 104],
        |_| UnderlayLookup::ProvenAbsent,
    )
    .unwrap();
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                palette: &palette,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
}

#[test]
fn same_layout_tiles_and_palettes_must_match_verified_sources() {
    let ready = Ready::new();
    let decoded = REGIONS
        .iter()
        .map(|&id| {
            (
                id,
                Terrain::decode(
                    &terrain_bytes(id, true, false),
                    DecodeLimits {
                        max_input_bytes: 256 * 1024,
                        max_tags_per_tile: 16,
                    },
                )
                .unwrap(),
            )
        })
        .collect::<Vec<_>>();
    let sources = decoded
        .iter()
        .map(|(id, terrain)| (*id, terrain))
        .collect::<Vec<_>>();
    let tiles = SurfaceTiles::compose(&ready.surface, &sources).unwrap();
    assert_eq!(tiles.layout(), ready.tiles.layout());
    assert_ne!(tiles, ready.tiles);
    let options = GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES);
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                tiles: &tiles,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
    let palette = calculate_underlay_palette(
        NativeMapSize::Tiles72,
        &vec![Some(0); PLANES * SIZE * SIZE],
        |_| UnderlayLookup::ProvenAbsent,
    )
    .unwrap();
    assert_ne!(palette, ready.palette);
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                palette: &palette,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
    let palette = calculate_underlay_palette(
        NativeMapSize::Tiles72,
        &vec![None; PLANES * SIZE * SIZE],
        |_| UnderlayLookup::Unavailable,
    )
    .unwrap();
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                palette: &palette,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
}

#[test]
fn same_layout_height_changes_and_unavailable_surface_regions_are_rejected() {
    let ready = Ready::new();
    let decoded = REGIONS
        .iter()
        .map(|&id| {
            (
                id,
                Terrain::decode(
                    &terrain_bytes(id, false, true),
                    DecodeLimits {
                        max_input_bytes: 256 * 1024,
                        max_tags_per_tile: 16,
                    },
                )
                .unwrap(),
            )
        })
        .collect::<Vec<_>>();
    let sources = decoded
        .iter()
        .map(|(id, terrain)| (*id, terrain))
        .collect::<Vec<_>>();
    let changed = SurfaceHeights::compose(ready.surface.layout(), &sources, &[]).unwrap();
    assert_ne!(changed, ready.surface);
    assert_eq!(
        compose_surface_tiles(&ready.terrain, &changed).unwrap(),
        ready.tiles
    );
    let options = GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES);
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                surface: &changed,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
    let sources = REGIONS[..3]
        .iter()
        .map(|&id| (id, ready.terrain.terrain(id).unwrap()))
        .collect::<Vec<_>>();
    let unavailable = SurfaceHeights::compose(ready.surface.layout(), &sources, &[]).unwrap();
    assert!(
        compose_surface_ground(
            SurfaceGroundInputs {
                surface: &unavailable,
                ..ready.inputs()
            },
            0,
            options
        )
        .is_err()
    );
}

#[test]
fn vertex_budget_is_exact_and_rejection_does_not_change_verified_inputs() {
    let ready = Ready::new();
    let result = compose_surface_ground(
        ready.inputs(),
        0,
        GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
    )
    .unwrap();
    for limit in [0, result.vertex_count - 1] {
        let error =
            compose_surface_ground(ready.inputs(), 0, GroundGeometryOptions::surface(limit))
                .unwrap_err();
        assert!(error.contains("VertexBudget"), "{error}");
    }
    assert_eq!(
        compose_surface_ground(
            ready.inputs(),
            0,
            GroundGeometryOptions::surface(result.vertex_count)
        )
        .unwrap(),
        result
    );
    assert_eq!(
        compose_surface_ground(
            ready.inputs(),
            0,
            GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES)
        )
        .unwrap(),
        result
    );
}

#[test]
fn required_material_misses_fail_instead_of_becoming_default_colors() {
    let ready = Ready::new();
    let floors = floor_probe(true);
    let palette =
        compose_underlay_palette(&ready.terrain, &ready.surface, floors.repository().unwrap())
            .unwrap();
    assert_eq!(palette, ready.palette);
    let error = compose_surface_ground(
        SurfaceGroundInputs {
            floors: &floors,
            ..ready.inputs()
        },
        0,
        GroundGeometryOptions::surface(MAX_OUTPUT_VERTICES),
    )
    .unwrap_err();
    assert!(error.contains("material"), "{error}");
}
