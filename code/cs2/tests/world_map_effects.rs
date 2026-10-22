#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct EffectHost {
    varps: Vec<i32>,
    varbits: BTreeMap<i32, Varbit>,
    world: WorldState,
}

impl VarHost for EffectHost {
    fn varp(&self, id: i32) -> Result<i32, HostError> {
        usize::try_from(id)
            .ok()
            .and_then(|index| self.varps.get(index).copied())
            .ok_or(HostError::InvalidVarp(id))
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, id: i32) -> Result<VarbitLookup, HostError> {
        Ok(self
            .varbits
            .get(&id)
            .map_or(VarbitLookup::Absent, |bit| VarbitLookup::Defined(*bit)))
    }

    fn world_state(&mut self) -> Result<&mut WorldState, HostError> {
        Ok(&mut self.world)
    }
}

fn number<T: std::str::FromStr>(value: &str) -> T
where
    T::Err: std::fmt::Debug,
{
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn categories(text: &str) -> Vec<WorldMapCategory> {
    if text == "-" {
        return Vec::new();
    }
    text.split(';')
        .map(|entry| {
            let values: Vec<i32> = entry.split(':').map(number).collect();
            WorldMapCategory {
                mec_id: values[0],
                abs_x: values[1],
                abs_y: values[2],
            }
        })
        .collect()
}

fn mec_types(text: &str) -> BTreeMap<i32, MecType> {
    text.split(';')
        .map(|row| {
            let v = integers(row);
            (
                v[0],
                MecType {
                    visible: v[1] == 1,
                    primary_varp: v[2],
                    primary_varbit: v[3],
                    primary_min: v[4],
                    primary_max: v[5],
                    secondary_varp: v[6],
                    secondary_varbit: v[7],
                    secondary_min: v[8],
                    secondary_max: v[9],
                },
            )
        })
        .collect()
}

fn state_of(map: &WorldMapState) -> Vec<i32> {
    vec![
        map.scroll_target_x,
        map.scroll_target_y,
        map.requested_scroll_x,
        map.requested_scroll_y,
        i32::from(map.use_target_position),
        map.flashed_element,
        map.flashed_category,
        map.flash_cycles,
        map.flash_cycle_counter,
        map.requested_zoom as i32,
        map.static_element.unwrap_or(-1),
    ]
}

fn apply(map: &mut WorldMapState, state: &[i32]) {
    map.scroll_target_x = state[0];
    map.scroll_target_y = state[1];
    map.requested_scroll_x = state[2];
    map.requested_scroll_y = state[3];
    map.use_target_position = state[4] == 1;
    map.flashed_element = state[5];
    map.flashed_category = state[6];
    map.flash_cycles = state[7];
    map.flash_cycle_counter = state[8];
    map.requested_zoom = state[9] as f32;
    map.static_element = (state[10] >= 0).then_some(state[10]);
}

fn effects_text(effects: &[WorldMapEffect]) -> String {
    if effects.is_empty() {
        return "-".to_string();
    }
    effects
        .iter()
        .map(|effect| match effect {
            WorldMapEffect::ReloadAssets => "reload",
            WorldMapEffect::LoadingScreen => "loading",
        })
        .collect::<Vec<_>>()
        .join(",")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_world_map_effect_case_matches_the_actual_kotlin_classes() {
    let text = include_str!("fixtures/world-map-effects.native.tsv");
    let mut varbits = BTreeMap::new();
    let mut cases = Vec::new();
    let mut real = WorldMapDefs::default();
    let mut total = 0;
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "B" => {
                varbits.insert(
                    number::<i32>(c[1]),
                    Varbit {
                        varp: number(c[2]),
                        start: number(c[3]),
                        end: number(c[4]),
                    },
                );
            }
            "E" => {
                let bytes: Vec<u8> = (0..c[2].len() / 2)
                    .map(|index| u8::from_str_radix(&c[2][index * 2..index * 2 + 2], 16).unwrap())
                    .collect();
                real.put(WorldMapElement::decode(number(c[1]), &bytes).unwrap());
            }
            "C" => cases.push(c),
            "Z" => total = number::<usize>(c[1]),
            other => panic!("{other}"),
        }
    }
    let mut mismatches = Vec::new();
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut effect_cases = 0;
    for c in &cases {
        let set: usize = number(c[1]);
        let defs = if set == 0 {
            real.clone()
        } else {
            WorldMapDefs::default()
        };
        let static_id: i32 = number(c[2]);
        let (cats, types) = match c[5].split_once('@') {
            Some((cats, types)) => (categories(cats), mec_types(types)),
            None => (Vec::new(), BTreeMap::new()),
        };
        let map_data = WorldMapData {
            defs,
            categories: cats,
            mec_types: types,
        };
        let mut world_map = WorldMapState {
            square_x: number(c[3]),
            square_y: number(c[4]),
            ..WorldMapState::default()
        };
        apply(&mut world_map, &integers(c[9]));
        let mut host = EffectHost {
            varps: integers(c[6]),
            varbits: varbits.clone(),
            world: WorldState {
                world_map,
                map_data,
                ..Default::default()
            },
        };
        let names: Vec<&str> = c[7].split(',').collect();
        let arguments = integers(c[8]);
        let mut code: Vec<_> = arguments.iter().map(|value| constant(*value)).collect();
        for name in &names {
            let opcode = OPCODES
                .iter()
                .find(|metadata| metadata.name == *name)
                .unwrap()
                .opcode;
            code.push(int(opcode, 0));
        }
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
        let label = format!("set {set} {} {} static {static_id}", c[7], c[8]);
        let entry = per_opcode.entry(names[0].to_string()).or_default();
        match (&result, c[10]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(c[11]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!("{label}: ints {:?} vs {expected:?}", vm.int_stack()));
                }
                let actual = state_of(&host.world.world_map);
                let wanted = integers(c[12]);
                if actual != wanted {
                    mismatches.push(format!("{label}: state {actual:?} vs {wanted:?}"));
                }
                let effects = effects_text(&host.world.world_map.effects);
                if effects != c[13] {
                    mismatches.push(format!("{label}: effects {effects} vs {}", c[13]));
                }
                if c[13] != "-" {
                    effect_cases += 1;
                }
            }
            (Err(_), "error") => entry.1 += 1,
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
    }
    println!(
        "world map effects cases={} effect_cases={effect_cases} mismatches={}",
        cases.len(),
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(8) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), total);
    assert_eq!(per_opcode.len(), 11);
    assert!(effect_cases > 500);
}
