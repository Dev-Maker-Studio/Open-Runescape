use openrs_scene::loc_place::{place_scene, SceneHeights, VertexBuilder};
use openrs_scene::loc_type::{decode_loc_type, LocType};
use openrs_scene::locs::{decode_region_locs, place_in_scene, ScenePlacement};
use std::collections::HashMap;

const DEFINITIONS: &[u8] = include_bytes!("fixtures/shadow-defs.bundle");
const MODELS: &[u8] = include_bytes!("fixtures/place-models.bundle");
const ENTITIES: &str = include_str!("fixtures/place-entities.tsv");
const REGIONS: [(i32, &[u8]); 12] = [
    (10034, include_bytes!("fixtures/shadow-l10034.bin")),
    (10035, include_bytes!("fixtures/shadow-l10035.bin")),
    (10288, include_bytes!("fixtures/shadow-l10288.bin")),
    (10289, include_bytes!("fixtures/shadow-l10289.bin")),
    (10290, include_bytes!("fixtures/shadow-l10290.bin")),
    (10291, include_bytes!("fixtures/shadow-l10291.bin")),
    (10544, include_bytes!("fixtures/shadow-l10544.bin")),
    (10545, include_bytes!("fixtures/shadow-l10545.bin")),
    (13621, include_bytes!("fixtures/shadow-l13621.bin")),
    (13622, include_bytes!("fixtures/shadow-l13622.bin")),
    (13877, include_bytes!("fixtures/shadow-l13877.bin")),
    (13878, include_bytes!("fixtures/shadow-l13878.bin")),
];

struct Scene {
    name: &'static str,
    base_x: i32,
    base_y: i32,
    size: i32,
    heights: &'static [u8],
}

const SCENES: [Scene; 3] = [
    Scene {
        name: "a",
        base_x: 39 * 64 + 24,
        base_y: 50 * 64 + 24,
        size: 104,
        heights: include_bytes!("fixtures/place-heights-a.bin"),
    },
    Scene {
        name: "b",
        base_x: 40 * 64,
        base_y: 48 * 64,
        size: 128,
        heights: include_bytes!("fixtures/place-heights-b.bin"),
    },
    Scene {
        name: "c",
        base_x: 53 * 64 + 20,
        base_y: 53 * 64 + 20,
        size: 104,
        heights: include_bytes!("fixtures/place-heights-c.bin"),
    },
];

fn entries(bundle: &'static [u8]) -> Vec<(i32, Option<&'static [u8]>)> {
    let mut at = 0usize;
    let mut take = |count: usize| {
        let slice = &bundle[at..at + count];
        at += count;
        slice
    };
    let total = i32::from_be_bytes(take(4).try_into().unwrap());
    let mut out = Vec::new();
    for _ in 0..total {
        let id = i32::from_be_bytes(take(4).try_into().unwrap());
        let length = i32::from_be_bytes(take(4).try_into().unwrap());
        out.push((id, (length >= 0).then(|| take(length as usize))));
    }
    out
}

fn definitions() -> HashMap<i32, LocType> {
    entries(DEFINITIONS)
        .into_iter()
        .filter_map(|(id, data)| {
            data.map(|data| (id, decode_loc_type(id, Some(data), true).expect("loc type")))
        })
        .collect()
}

fn models() -> HashMap<i32, &'static [u8]> {
    entries(MODELS)
        .into_iter()
        .filter_map(|(id, data)| data.map(|data| (id, data)))
        .collect()
}

fn scene_placements(scene: &Scene) -> Vec<ScenePlacement> {
    let mut placements = Vec::new();
    for (region, data) in REGIONS {
        let (region_x, region_y) = (region >> 8, region & 255);
        let overlaps = region_x * 64 < scene.base_x + scene.size
            && region_x * 64 + 64 > scene.base_x
            && region_y * 64 < scene.base_y + scene.size
            && region_y * 64 + 64 > scene.base_y;
        if overlaps {
            let decoded = decode_region_locs(data).expect("locs");
            placements.extend(place_in_scene(
                region,
                &decoded,
                scene.base_x,
                scene.base_y,
                scene.size,
            ));
        }
    }
    placements
}

const BASIS: u64 = 0xcbf2_9ce4_8422_2325;
const PRIME: u64 = 0x100_0000_01b3;

fn hash_int(mut hash: u64, value: i32) -> u64 {
    for byte in value.to_le_bytes() {
        hash = (hash ^ u64::from(byte)).wrapping_mul(PRIME);
    }
    hash
}

fn hash_axis(count: usize, values: &[i32]) -> u64 {
    let mut hash = hash_int(BASIS, count as i32);
    for value in &values[..count] {
        hash = hash_int(hash, *value);
    }
    hash
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn placements_and_vertices_equal_the_native_client() {
    let types = definitions();
    let model_data = models();
    let builder = VertexBuilder::new(|id| model_data.get(&id).copied());
    let mut expected: HashMap<&str, Vec<Vec<&str>>> = HashMap::new();
    let mut height_hashes: HashMap<(&str, usize), u64> = HashMap::new();
    for line in ENTITIES.lines() {
        let fields: Vec<&str> = line.split('\t').collect();
        match fields[0] {
            "E" => expected.entry(fields[1]).or_default().push(fields),
            "H" => {
                height_hashes.insert(
                    (fields[1], fields[2].parse().unwrap()),
                    fields[3].parse().unwrap(),
                );
            }
            _ => panic!("unknown record"),
        }
    }
    let mut compared = 0usize;
    let mut animated = 0usize;
    for scene in &SCENES {
        let heights =
            SceneHeights::from_big_endian(scene.size, scene.size, scene.heights).expect("heights");
        for plane in 0..4usize {
            let mut hash = BASIS;
            for x in 0..=scene.size {
                for y in 0..=scene.size {
                    hash = hash_int(hash, heights.raw(plane, x, y));
                }
            }
            assert_eq!(
                hash,
                height_hashes[&(scene.name, plane)],
                "heights {} {plane}",
                scene.name
            );
        }
        let placements = scene_placements(scene);
        let placed = place_scene(&placements, &heights, |id| types.get(&id));
        let rows = &expected[scene.name];
        assert_eq!(placed.len(), rows.len(), "entity count {}", scene.name);
        for (entity, row) in placed.iter().zip(rows) {
            let ints: Vec<i32> = row[2..10]
                .iter()
                .chain(&row[11..16])
                .map(|value| value.parse().unwrap())
                .collect();
            let context = format!("scene {} seq {}", scene.name, row[2]);
            assert_eq!(
                [
                    entity.seq,
                    entity.role,
                    entity.plane,
                    entity.x,
                    entity.y,
                    entity.loc_id,
                    entity.shape,
                    entity.rotation,
                    entity.pos_x,
                    entity.pos_y,
                    entity.average_height,
                    entity.scene_x,
                    entity.scene_y,
                ]
                .to_vec(),
                ints,
                "{context}"
            );
            assert_eq!(entity.kind.code().to_string(), row[10], "{context}");
            if row[16] == "none" {
                animated += 1;
                continue;
            }
            let loc = types.get(&entity.loc_id).expect("type");
            let vertices = builder
                .build(loc, entity, &heights)
                .unwrap_or_else(|| panic!("{context} no model"));
            let count: usize = row[17].parse().unwrap();
            assert_eq!(vertices.xs.len(), count, "{context}");
            assert_eq!(
                hash_axis(count, &vertices.xs).to_string(),
                row[18],
                "{context}"
            );
            assert_eq!(
                hash_axis(count, &vertices.ys).to_string(),
                row[19],
                "{context}"
            );
            assert_eq!(
                hash_axis(count, &vertices.zs).to_string(),
                row[20],
                "{context}"
            );
            compared += 1;
        }
    }
    assert!(compared > 25000, "compared {compared}");
    assert!(animated > 0);
}
