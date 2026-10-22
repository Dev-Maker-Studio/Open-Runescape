use openrs_scene::collision::{can_reach, CollisionMap};
use openrs_scene::collision_region::{LocCollisionProps, SceneCollision};
use openrs_scene::loc_type::decode_loc_type;
use openrs_scene::locs::decode_region_locs;
use openrs_scene::route::{RouteFinder, Strategy};
use openrs_scene::terrain::{DecodeLimits, Terrain};
use std::collections::HashMap;
#[cfg(target_arch = "wasm32")]
use wasm_bindgen_test::wasm_bindgen_test;

const SCENES: &str = include_str!("fixtures/coll-scenes.txt");
const BUNDLE: &[u8] = include_bytes!("fixtures/coll-defs.bundle");
const SYNTH: &str = include_str!("fixtures/coll-synth.txt");
const SYNTH_FLAGS: &[u8] = include_bytes!("fixtures/coll-synth.flags");
const ROUTES: &str = include_str!("fixtures/coll-routes.tsv");
const REACH: &str = include_str!("fixtures/coll-reach.tsv");

fn payload(name: &str) -> &'static [u8] {
    match name {
        "m22111" => include_bytes!("fixtures/coll-m22111.bin"),
        "m22112" => include_bytes!("fixtures/coll-m22112.bin"),
        "m22367" => include_bytes!("fixtures/coll-m22367.bin"),
        "m22368" => include_bytes!("fixtures/coll-m22368.bin"),
        "m33906" => include_bytes!("fixtures/coll-m33906.bin"),
        "m33907" => include_bytes!("fixtures/coll-m33907.bin"),
        "m34162" => include_bytes!("fixtures/coll-m34162.bin"),
        "m34163" => include_bytes!("fixtures/coll-m34163.bin"),
        "m35944" => include_bytes!("fixtures/coll-m35944.bin"),
        "l22111" => include_bytes!("fixtures/coll-l22111.bin"),
        "l22112" => include_bytes!("fixtures/coll-l22112.bin"),
        "l22367" => include_bytes!("fixtures/coll-l22367.bin"),
        "l22368" => include_bytes!("fixtures/coll-l22368.bin"),
        "l33906" => include_bytes!("fixtures/coll-l33906.bin"),
        "l33907" => include_bytes!("fixtures/coll-l33907.bin"),
        "l34162" => include_bytes!("fixtures/coll-l34162.bin"),
        "l34163" => include_bytes!("fixtures/coll-l34163.bin"),
        "l35944" => include_bytes!("fixtures/coll-l35944.bin"),
        "l101907" => include_bytes!("fixtures/coll-l101907.bin"),
        "l102111" => include_bytes!("fixtures/coll-l102111.bin"),
        "l103367" => include_bytes!("fixtures/coll-l103367.bin"),
        "l104112" => include_bytes!("fixtures/coll-l104112.bin"),
        "l105368" => include_bytes!("fixtures/coll-l105368.bin"),
        "l106162" => include_bytes!("fixtures/coll-l106162.bin"),
        other => panic!("unknown payload {other}"),
    }
}

fn flags_fixture(name: &str) -> &'static [u8] {
    match name {
        "S1" => include_bytes!("fixtures/coll-flags-S1.bin"),
        "S2" => include_bytes!("fixtures/coll-flags-S2.bin"),
        "S3" => include_bytes!("fixtures/coll-flags-S3.bin"),
        "S4" => include_bytes!("fixtures/coll-flags-S4.bin"),
        "S5" => include_bytes!("fixtures/coll-flags-S5.bin"),
        "S6" => include_bytes!("fixtures/coll-flags-S6.bin"),
        "S7" => include_bytes!("fixtures/coll-flags-S7.bin"),
        other => panic!("unknown scene {other}"),
    }
}

fn bundle() -> HashMap<i32, Option<LocCollisionProps>> {
    let be = |at: usize| i32::from_be_bytes(BUNDLE[at..at + 4].try_into().unwrap());
    let count = be(0);
    let mut at = 4;
    let mut out = HashMap::new();
    for _ in 0..count {
        let id = be(at);
        let length = be(at + 4);
        at += 8;
        let data = if length < 0 {
            None
        } else {
            let slice = &BUNDLE[at..at + length as usize];
            at += length as usize;
            Some(slice)
        };
        if let Ok(loc) = decode_loc_type(id, data, true) {
            out.insert(id, Some(LocCollisionProps::from(&loc)));
        }
    }
    assert_eq!(at, BUNDLE.len());
    out
}

struct Scene {
    name: String,
    collision: SceneCollision,
    placements: usize,
}

fn build_scenes() -> Vec<Scene> {
    let props = bundle();
    let mut out = Vec::new();
    for line in SCENES.lines().filter(|l| !l.is_empty()) {
        let f: Vec<&str> = line.split(' ').collect();
        let size_x: i32 = f[1].parse().unwrap();
        let size_y: i32 = f[2].parse().unwrap();
        let mut collision = SceneCollision::new(size_x, size_y);
        let entries: Vec<Vec<i32>> = f[5]
            .split(',')
            .map(|e| e.split(':').map(|v| v.parse().unwrap()).collect())
            .collect();
        for entry in &entries {
            let data = payload(&format!("m{}", entry[0]));
            let (terrain, _) = Terrain::decode_planes(
                data,
                4,
                DecodeLimits {
                    max_input_bytes: data.len(),
                    max_tags_per_tile: 16,
                },
            )
            .unwrap();
            collision.load_terrain(&terrain, entry[2], entry[3]);
        }
        collision.apply_unwalkable();
        let mut placements = 0;
        for entry in &entries {
            let name = format!("l{}", entry[1]);
            let found = match name.as_str() {
                "l22111" | "l22112" | "l22367" | "l22368" | "l33906" | "l33907" | "l34162"
                | "l34163" | "l35944" | "l101907" | "l102111" | "l103367" | "l104112"
                | "l105368" | "l106162" => Some(payload(&name)),
                _ => None,
            };
            if let Some(data) = found {
                let locs = decode_region_locs(data).unwrap();
                placements += locs.len();
                collision.add_locs(&locs, entry[2], entry[3], |id| {
                    props.get(&id).copied().flatten()
                });
            }
        }
        out.push(Scene {
            name: f[0].to_string(),
            collision,
            placements,
        });
    }
    out
}

fn flag_bytes(maps: &[&CollisionMap]) -> Vec<u8> {
    let mut out = Vec::new();
    for map in maps {
        for value in map.raw() {
            out.extend_from_slice(&value.to_be_bytes());
        }
    }
    out
}

fn synthetic() -> (Vec<(String, CollisionMap)>, usize, usize) {
    let mut maps: Vec<(String, CollisionMap)> = Vec::new();
    let mut ops = 0;
    let mut faulted_ops = 0;
    for line in SYNTH.lines() {
        let f: Vec<&str> = line.split(' ').collect();
        if f[0] == "M" {
            let n = |i: usize| -> i32 { f[i].parse().unwrap() };
            let map = if f[2] == "clip" {
                CollisionMap::create_clip_map(n(5), n(6))
            } else {
                CollisionMap::new(n(3), n(4), n(5), n(6))
            };
            maps.push((f[1].to_string(), map));
            continue;
        }
        let (_, map) = maps.iter_mut().find(|(name, _)| name == f[1]).unwrap();
        let bar = f.iter().position(|v| *v == "|").unwrap();
        let expect_exc = f[bar + 1] == "EXC";
        let a: Vec<i32> = f[3..bar].iter().map(|v| v.parse().unwrap()).collect();
        map.clear_fault();
        match f[2] {
            "reset" => map.reset(),
            "ubscene" => {
                for x in 0..a[0] {
                    for y in 0..a[1] {
                        map.unblock(x, y);
                    }
                }
            }
            "fbo" => map.flag_blocked_object(a[0], a[1], a[2], a[3], a[4] != 0, a[5] != 0),
            "ufb" => map.unflag_blocked(a[0], a[1], a[2], a[3], a[4], a[5] != 0, a[6] != 0),
            "fb" => map.flag_boundary(a[0], a[1], a[2], a[3], a[4] != 0, a[5] != 0),
            "ufbd" => map.unflag_boundary(a[0], a[1], a[2], a[3], a[4] != 0, a[5] != 0),
            "fd" => map.flag_blocked_decoration(a[0], a[1]),
            "ufd" => map.unflag_floor_decoration(a[0], a[1]),
            "uw" => map.set_unwalkable(a[0], a[1]),
            "ub" => map.unblock(a[0], a[1]),
            "af" => map.add_flag(a[0], a[1], a[2]),
            "rf" => map.remove_flag(a[0], a[1], a[2]),
            other => panic!("unknown op {other}"),
        }
        assert_eq!(map.faulted(), expect_exc, "{line}");
        ops += 1;
        if expect_exc {
            faulted_ops += 1;
        }
    }
    (maps, ops, faulted_ops)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn scene_collision_flags_match_the_loader_byte_for_byte() {
    let scenes = build_scenes();
    assert_eq!(scenes.len(), 7);
    let mut cells = 0;
    let mut placements = 0;
    for scene in &scenes {
        let maps: Vec<&CollisionMap> = scene.collision.maps.iter().collect();
        let bytes = flag_bytes(&maps);
        let expected = flags_fixture(&scene.name);
        assert_eq!(bytes.len(), expected.len(), "{}", scene.name);
        assert!(bytes == expected, "{} flags differ", scene.name);
        cells += bytes.len() / 4;
        placements += scene.placements;
    }
    assert_eq!(cells, 4 * (5 * 110 * 110 + 2 * 94 * 126));
    assert!(placements > 10000);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn synthetic_boundary_and_object_operations_match_including_faults() {
    let (maps, ops, faulted) = synthetic();
    assert_eq!(maps.len(), 40);
    assert_eq!(ops, 14798);
    assert!(faulted > 1000);
    let refs: Vec<&CollisionMap> = maps.iter().map(|(_, m)| m).collect();
    let bytes = flag_bytes(&refs);
    assert!(bytes == SYNTH_FLAGS);
}

fn strategy(kind: i32, dst: (i32, i32), size: (i32, i32), shape: i32, rotation: i32) -> Strategy {
    match kind {
        0 => Strategy::FixedTile { x: dst.0, y: dst.1 },
        1 => Strategy::GroundItem { x: dst.0, y: dst.1 },
        2 => Strategy::Player {
            x: dst.0,
            y: dst.1,
            size_x: size.0,
            size_y: size.1,
            clipping_flag: 0,
        },
        3 => Strategy::Npc {
            x: dst.0,
            y: dst.1,
            size_x: size.0,
            size_y: size.1,
            clipping_flag: 0,
        },
        4 => Strategy::Wall {
            x: dst.0,
            y: dst.1,
            size_x: size.0,
            size_y: size.1,
            shape,
            rotation,
        },
        _ => Strategy::Normal {
            x: dst.0,
            y: dst.1,
            shape,
            rotation,
        },
    }
}

fn target_map<'a>(
    name: &str,
    scenes: &'a [Scene],
    synth: &'a [(String, CollisionMap)],
) -> &'a CollisionMap {
    if name.starts_with('Y') {
        &synth.iter().find(|(n, _)| n == name).unwrap().1
    } else {
        let (scene, plane) = name.split_once(':').unwrap();
        &scenes
            .iter()
            .find(|s| s.name == scene)
            .unwrap()
            .collision
            .maps[plane.parse::<usize>().unwrap()]
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn route_queries_match_steps_results_exits_and_faults() {
    let scenes = build_scenes();
    let (synth, _, _) = synthetic();
    let mut finder = RouteFinder::new();
    let mut total = 0;
    let mut reached = 0;
    let mut faults = 0;
    let mut real = 0;
    for line in ROUTES.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let n = |i: usize| -> i32 { f[i].parse().unwrap() };
        let map = target_map(f[0], &scenes, &synth);
        let strat = strategy(n(1), (n(5), n(6)), (n(7), n(8)), n(9), n(10));
        let len = n(12) as usize;
        let mut steps_x = vec![0; len];
        let mut steps_y = vec![0; len];
        let result = finder.find_route(
            n(2),
            n(3),
            n(4),
            &strat,
            map,
            n(11) == 1,
            &mut steps_x,
            &mut steps_y,
        );
        match (f[13], result) {
            ("EXC", Err(_)) => faults += 1,
            (text, Ok(value)) => {
                assert_eq!(text, value.to_string(), "{line}");
                if value > 0 {
                    reached += 1;
                }
            }
            _ => panic!("fault mismatch {line}"),
        }
        assert_eq!((finder.exit_x, finder.exit_y), (n(14), n(15)), "{line}");
        let mut expected = Vec::new();
        for (k, (x, y)) in steps_x.iter().zip(&steps_y).enumerate() {
            if *x != 0 || *y != 0 {
                expected.push(format!("{k}:{x}:{y}"));
            }
        }
        let text = if expected.is_empty() {
            "-".to_string()
        } else {
            expected.join(",")
        };
        assert_eq!(text, f[16], "{line}");
        total += 1;
        if !f[0].starts_with('Y') {
            real += 1;
        }
    }
    assert_eq!(total, 6000);
    assert_eq!(real, 3600);
    assert!(reached > 1500);
    assert!(faults > 500);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn reach_and_walkable_queries_match() {
    let scenes = build_scenes();
    let (synth, _, _) = synthetic();
    let mut counts: HashMap<String, usize> = HashMap::new();
    for line in REACH.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let n = |i: usize| -> i32 { f[i].parse().unwrap() };
        let last = f[f.len() - 1];
        let (text, kind) = if f[0] == "rc" {
            let value = can_reach(n(2), n(3), n(4), n(5), n(6), n(7), n(8), n(9));
            (value.to_string(), "rc")
        } else {
            let map = target_map(f[1], &scenes, &synth);
            map.clear_fault();
            let value = match f[0] {
                "rl" => map.can_reach_loc(n(2), n(3), n(4), n(5), n(6), n(7), n(8)),
                "rn" => map.can_reach_npc(n(2), n(3), n(4), n(5), n(6), n(7), n(8), n(9)),
                "rw" => map.walkable(n(2), n(3), n(4), n(5), n(6), n(7), n(8), n(9), n(10)),
                other => panic!("unknown query {other}"),
            };
            let text = if map.faulted() {
                "EXC".to_string()
            } else {
                value.to_string()
            };
            (text, f[0])
        };
        assert_eq!(text, last, "{line}");
        *counts.entry(kind.to_string()).or_default() += 1;
    }
    assert_eq!(counts.values().sum::<usize>(), 4000);
    assert!(counts.values().all(|c| *c > 500));
}
