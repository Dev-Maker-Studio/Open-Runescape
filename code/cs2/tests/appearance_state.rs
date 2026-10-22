#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;

struct AppearanceHost {
    state: AppearanceState,
    model: ComponentModel,
}

impl VarHost for AppearanceHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn appearance_state_mut(&mut self) -> Result<&mut AppearanceState, HostError> {
        Ok(&mut self.state)
    }

    fn components(&mut self) -> Result<&mut ComponentModel, HostError> {
        Ok(&mut self.model)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn list(value: &str) -> Option<Vec<i32>> {
    match value {
        "-" => None,
        "e" => Some(Vec::new()),
        _ => Some(value.split(',').map(number).collect()),
    }
}

fn shorts(value: &str) -> Option<Vec<i16>> {
    list(value).map(|values| values.into_iter().map(|value| value as i16).collect())
}

fn fixed<const N: usize>(value: &str) -> [i32; N] {
    list(value).unwrap().try_into().unwrap()
}

fn modifiers(value: &str) -> Option<Vec<Option<MeshModifier>>> {
    match value {
        "-" => None,
        "e" => Some(Vec::new()),
        _ => Some(
            value
                .split(';')
                .map(|entry| {
                    if entry == "-" {
                        return None;
                    }
                    let fields: Vec<&str> = entry.split('/').collect();
                    Some(MeshModifier {
                        male_body: fixed(fields[0]),
                        female_body: fixed(fields[1]),
                        male_head: fixed(fields[2]),
                        female_head: fixed(fields[3]),
                        recol_dst: shorts(fields[4]),
                        retex_dst: shorts(fields[5]),
                    })
                })
                .collect(),
        ),
    }
}

fn appearance(columns: &[&str], mods: Option<Vec<Option<MeshModifier>>>) -> Option<PlayerAppearance> {
    if columns[2] == "-" {
        return None;
    }
    Some(PlayerAppearance {
        bas_id: number(columns[4]),
        appearance: list(columns[0]),
        mesh_modifiers: mods,
        recolor_dst_indices: list(columns[1]),
        female: columns[2] == "1",
        hash: columns[3].parse().unwrap(),
    })
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::EntityType) => expected == "ClassCastException",
        VmErrorKind::Host(HostError::State(_)) | VmErrorKind::StackUnderflow(_) => {
            expected == "ArrayIndexOutOfBoundsException"
        }
        VmErrorKind::Component(failure) => match failure {
            ComponentFailure::NoComponent => expected == "NullPointerException",
            ComponentFailure::Index => expected == "ArrayIndexOutOfBoundsException",
            ComponentFailure::Interface => expected == "IllegalArgumentException",
            _ => false,
        },
        _ => false,
    }
}

fn opcode_named(name: &str) -> Opcode {
    OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap_or_else(|| panic!("{name}"))
        .opcode
}

fn run(
    opcode: Opcode,
    stack: &[i32],
    operand: i32,
    host: &mut AppearanceHost,
) -> (Result<(), VmError>, Vec<i32>) {
    let mut code: Vec<_> = stack.iter().map(|value| constant(*value)).collect();
    code.push(int(opcode, operand));
    code.push(ret());
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm
        .run_with_host(1, &repository, Arguments::default(), host)
        .map(|_| ());
    (result, vm.int_stack().to_vec())
}

fn hook_text(model: &ComponentModel, secondary: bool) -> String {
    match model.hook(secondary) {
        None => "-".to_string(),
        Some(hook) => format!("{}:{}", hook.interface, model.component(hook.component).id_hash),
    }
}

fn expected_stack(value: &str) -> Vec<i32> {
    list(value).unwrap_or_default()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_appearance_case_matches_the_actual_kotlin_dispatch() {
    let text = include_str!("fixtures/appearance.native.tsv");
    let mut mismatches = Vec::new();
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut layouts: BTreeMap<i32, Vec<i32>> = BTreeMap::new();
    let (mut total, mut visibility) = (0usize, 0usize);
    let mut trailer = (0usize, 0usize);
    for line in text.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "I" => {
                layouts.insert(
                    number(columns[1]),
                    list(columns[2]).unwrap_or_default(),
                );
            }
            "Z" => {
                trailer = (
                    columns[1].parse().unwrap(),
                    columns[2].parse().unwrap(),
                );
            }
            "C" => {
                total += 1;
                let name = columns[1];
                let opcode = opcode_named(name);
                let mods = modifiers(columns[8]);
                let mut host = AppearanceHost {
                    state: AppearanceState {
                        my_player: columns[2] == "1",
                        appearance: appearance(&columns[3..8], mods),
                    },
                    model: ComponentModel::new(8, 765, 503),
                };
                let stack = expected_stack(columns[9]);
                let (result, ints) = run(opcode, &stack, 0, &mut host);
                let label = format!("#{total} {name} {stack:?} state={}", columns[3..8].join("|"));
                let entry = per_opcode.entry(name.to_string()).or_default();
                match (&result, columns[10]) {
                    (Ok(_), "ok") => {
                        entry.0 += 1;
                        if ints != expected_stack(columns[11]) {
                            mismatches.push(format!("{label}: ints {ints:?} vs {}", columns[11]));
                        }
                    }
                    (Err(error), expected) if expected.starts_with("error:") => {
                        entry.1 += 1;
                        if error.instruction != stack.len()
                            || !class_matches(&expected["error:".len()..], &error.kind)
                        {
                            mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                        }
                    }
                    (actual, expected) => {
                        mismatches.push(format!("{label}: {expected} vs {actual:?}"));
                    }
                }
                let before = host.state.appearance.as_ref().and_then(|value| value.mesh_modifiers.clone());
                let after = appearance(&columns[12..17], before);
                if host.state.appearance != after {
                    mismatches.push(format!(
                        "{label}: state {:?} vs {}",
                        host.state.appearance,
                        columns[12..17].join("|")
                    ));
                }
            }
            "V" => {
                visibility += 1;
                let secondary = columns[1] == "1";
                let mut model = ComponentModel::new(8, 765, 503);
                let mut handles = BTreeMap::new();
                for (id, hashes) in &layouts {
                    let states = hashes
                        .iter()
                        .map(|hash| {
                            (*hash != -1).then(|| {
                                let mut state = ComponentState::default();
                                state.id_hash = *hash;
                                state
                            })
                        })
                        .collect();
                    handles.insert(*id, model.add_interface(*id, false, states).unwrap());
                }
                model.set_entity_interface(match columns[2] {
                    "-" => EntityInterface::Absent,
                    "u" => EntityInterface::Unbound,
                    id => EntityInterface::Bound(number(id)),
                });
                if columns[4] != "-" {
                    let parts: Vec<&str> = columns[4].split(':').collect();
                    let id = number(parts[1]);
                    let index = number(parts[2]) as usize;
                    model.set_hook(
                        parts[0] == "2",
                        Some(ComponentHook {
                            interface: id,
                            component: handles[&id][index].unwrap(),
                        }),
                    );
                }
                let mut host = AppearanceHost {
                    state: AppearanceState::default(),
                    model,
                };
                let supplied: Vec<i32> = list(columns[3]).unwrap_or_default();
                let (result, ints) = run(
                    Opcode::GET_NPC_VISLEVEL,
                    &supplied,
                    i32::from(secondary),
                    &mut host,
                );
                let label = format!(
                    "#{visibility} GET_NPC_VISLEVEL secondary={secondary} entity={} component={} seeded={}",
                    columns[2], columns[3], columns[4]
                );
                let entry = per_opcode.entry("GET_NPC_VISLEVEL".to_string()).or_default();
                match (&result, columns[5]) {
                    (Ok(_), "ok") => {
                        entry.0 += 1;
                        if ints != expected_stack(columns[6]) {
                            mismatches.push(format!("{label}: ints {ints:?} vs {}", columns[6]));
                        }
                    }
                    (Err(error), expected) if expected.starts_with("error:") => {
                        entry.1 += 1;
                        if error.instruction != supplied.len()
                            || !class_matches(&expected["error:".len()..], &error.kind)
                        {
                            mismatches.push(format!("{label}: error {expected} vs {error:?}"));
                        }
                    }
                    (actual, expected) => {
                        mismatches.push(format!("{label}: {expected} vs {actual:?}"));
                    }
                }
                let hooks = (
                    hook_text(&host.model, false),
                    hook_text(&host.model, true),
                );
                if hooks.0 != columns[7] || hooks.1 != columns[8] {
                    mismatches.push(format!(
                        "{label}: hooks {hooks:?} vs {} {}",
                        columns[7], columns[8]
                    ));
                }
            }
            _ => panic!("{line}"),
        }
    }
    println!(
        "appearance cases={total} visibility={visibility} mismatches={}",
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(12) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!((total, visibility), trailer);
    assert_eq!(per_opcode.len(), 5);
    assert!(per_opcode.values().all(|(ok, error)| *ok > 0 && *error > 0));
}

#[test]
fn appearance_opcodes_are_unsupported_without_a_host_state() {
    struct Bare;
    impl VarHost for Bare {
        fn varp(&self, _: i32) -> Result<i32, HostError> {
            Ok(0)
        }
        fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
            Ok(())
        }
        fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
            Ok(VarbitLookup::Absent)
        }
    }
    for (opcode, arguments) in [
        (Opcode::SETOBJ, 2),
        (Opcode::BASEIDKIT, 2),
        (Opcode::BASECOLOUR, 2),
        (Opcode::SETGENDER, 1),
        (Opcode::GET_NPC_VISLEVEL, 1),
    ] {
        let mut code: Vec<_> = (0..arguments).map(|_| constant(0)).collect();
        code.push(int(opcode, 0));
        code.push(ret());
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
        )]);
        let mut vm = Vm::default();
        let error = vm
            .run_with_host(1, &repository, Arguments::default(), &mut Bare)
            .unwrap_err();
        assert!(matches!(error.kind, VmErrorKind::UnsupportedOpcode(found) if found == opcode));
    }
}

#[test]
fn crc64_table_and_slot_tables_match_the_client_constants() {
    assert_eq!(CRC64_TABLE[0], 0);
    assert_eq!(CRC64_TABLE[128], -3_932_672_073_523_589_310);
    assert_eq!(MALE_IDKIT_SLOTS, [0, 1, 2, 3, 4, 5, 6, 14]);
    assert_eq!(FEMALE_IDKIT_SLOTS, [7, 8, 9, 10, 11, 12, 13, 15]);
    assert_eq!(IDK_PART_TABLE, [8, 11, 4, 6, 9, 7, 10, 0]);
}
