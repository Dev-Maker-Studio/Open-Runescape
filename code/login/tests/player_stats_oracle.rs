use openrs_login::player_stats::{
    Limits, PlayerStats, SKILL_COUNT, SKILL_MAX_LEVEL, SKILL_UPDATE_RING_SIZE, SKILL_XP_FOR_LEVEL,
    Update,
};
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeSet;

const MANIFEST: &str = include_str!("fixtures/player-stats-cases.tsv");
const PAYLOADS: &[u8] = include_bytes!("fixtures/player-stats-payloads.bin");
const VALUES: &[u8] = include_bytes!("fixtures/player-stats-native-values.bin");
const SNAPSHOT_BYTES: usize = (1 + SKILL_COUNT * 3 + 3 + SKILL_UPDATE_RING_SIZE) * 4;

fn snapshot(state: &PlayerStats, consumed: usize) -> Vec<u8> {
    let mut output = Vec::with_capacity(SNAPSHOT_BYTES);
    let mut int = |value: i32| output.extend_from_slice(&value.to_be_bytes());
    int(consumed as i32);
    for skill in state.skills() {
        int(skill.experience);
    }
    for skill in state.skills() {
        int(i32::from(skill.visible_level));
    }
    for skill in state.skills() {
        int(i32::from(skill.base_level));
    }
    int(i32::from(state.run_energy()));
    int(i32::from(state.player_weight()));
    int(state.skill_update_cycle());
    for &skill_id in state.recent_skill_updates() {
        int(i32::from(skill_id));
    }
    assert_eq!(output.len(), SNAPSHOT_BYTES);
    output
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn actual_native_initial_state_and_all_table_entries_are_frozen_exactly() {
    assert_eq!(
        include_str!("fixtures/player-stats-oracle.sha256").trim(),
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    let state = PlayerStats::new(Limits::default()).unwrap();
    assert_eq!(
        snapshot(&state, 0),
        include_bytes!("fixtures/player-stats-initial.native-values")
    );
    let mut tables = Vec::new();
    for cap in SKILL_MAX_LEVEL {
        tables.extend_from_slice(&i32::from(cap).to_be_bytes());
    }
    for threshold in SKILL_XP_FOR_LEVEL {
        tables.extend_from_slice(&threshold.to_be_bytes());
    }
    assert_eq!(
        tables,
        include_bytes!("fixtures/player-stats-tables.native-values")
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn actual_packet_decoder_snapshots_match_after_every_sequential_update() {
    let mut lines = MANIFEST.lines();
    assert_eq!(lines.next(), Some("order\tname\topcode\toffset\tlength"));
    let count = lines.clone().count();
    assert!(count > 1000);
    assert_eq!(VALUES.len(), count * SNAPSHOT_BYTES);
    let mut state = PlayerStats::new(Limits::default()).unwrap();
    let mut offset = 0;
    let mut names = BTreeSet::new();
    let mut skill_ids = BTreeSet::new();
    let mut visible_levels = BTreeSet::new();
    let mut xp_boundaries = BTreeSet::new();
    let mut energies = BTreeSet::new();
    let mut weights = BTreeSet::new();
    let mut stat_count = 0;
    for (index, line) in lines.enumerate() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 5);
        assert_eq!(fields[0].parse::<usize>().unwrap(), index);
        assert!(names.insert(fields[1]));
        let opcode = ServerPacket::from_wire(fields[2].parse().unwrap()).unwrap();
        let start = fields[3].parse::<usize>().unwrap();
        let length = fields[4].parse::<usize>().unwrap();
        assert_eq!(start, offset);
        let payload = &PAYLOADS[start..start + length];
        assert_eq!(length as i32, opcode.metadata().native_size);
        let update = state.apply(opcode, payload).unwrap();
        assert_eq!(
            snapshot(&state, length),
            &VALUES[index * SNAPSHOT_BYTES..(index + 1) * SNAPSHOT_BYTES],
            "native case {} ({})",
            index + 1,
            fields[1]
        );
        match update {
            Update::Stat {
                skill_id,
                state: skill,
            } => {
                skill_ids.insert(skill_id);
                visible_levels.insert(skill.visible_level);
                xp_boundaries.insert((skill_id, skill.experience));
                stat_count += 1;
            }
            Update::RunEnergy(energy) => {
                energies.insert(energy);
            }
            Update::PlayerWeight(weight) => {
                weights.insert(weight);
            }
        }
        assert_eq!(state.skill_update_cycle(), stat_count);
        offset += length;
    }
    assert_eq!(offset, PAYLOADS.len());
    assert_eq!(skill_ids.len(), SKILL_COUNT);
    assert_eq!(visible_levels.len(), 256);
    assert_eq!(energies.len(), 256);
    for skill_id in [0, 24] {
        for threshold in SKILL_XP_FOR_LEVEL {
            for xp in [threshold - 1, threshold, threshold + 1] {
                assert!(
                    xp_boundaries.contains(&(skill_id, xp)),
                    "missing native XP boundary {skill_id}/{xp}"
                );
            }
        }
        assert!(xp_boundaries.contains(&(skill_id, i32::MIN)));
        assert!(xp_boundaries.contains(&(skill_id, i32::MAX)));
    }
    for weight in [i16::MIN, -1, 0, 1, i16::MAX] {
        assert!(weights.contains(&weight));
    }
    assert!(stat_count > 32);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_case_rejects_all_prefixes_and_trailing_bytes_without_mutation() {
    let mut state = PlayerStats::new(Limits::default()).unwrap();
    for line in MANIFEST.lines().skip(1) {
        let fields: Vec<_> = line.split('\t').collect();
        let opcode = ServerPacket::from_wire(fields[2].parse().unwrap()).unwrap();
        let offset = fields[3].parse::<usize>().unwrap();
        let length = fields[4].parse::<usize>().unwrap();
        let payload = &PAYLOADS[offset..offset + length];
        let before = state.clone();
        for cut in 0..length {
            assert!(
                state.apply(opcode, &payload[..cut]).is_err(),
                "{} prefix {cut}",
                fields[1]
            );
            assert_eq!(state, before);
        }
        let mut trailing = payload.to_vec();
        trailing.push(0);
        assert!(state.apply(opcode, &trailing).is_err());
        assert_eq!(state, before);
        state.apply(opcode, payload).unwrap();
    }
}
