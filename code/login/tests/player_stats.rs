use openrs_login::player_stats::{
    Error, Limits, PLAYER_WEIGHT_BYTES, PlayerStats, RUN_ENERGY_BYTES, SKILL_COUNT,
    SKILL_MAX_LEVEL, SKILL_UPDATE_RING_SIZE, SKILL_XP_FOR_LEVEL, SkillState, UPDATE_STAT_BYTES,
    Update, base_level_for_experience, decode,
};
use openrs_protocol::registry::ServerPacket;

fn stat(experience: i32, skill_id: u8, visible_level: u8) -> [u8; 6] {
    let mut payload = [0; 6];
    payload[..4].copy_from_slice(&experience.to_be_bytes());
    payload[4] = skill_id;
    payload[5] = visible_level;
    payload
}

fn native() -> PlayerStats {
    PlayerStats::new(Limits::default()).unwrap()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_registry_identities_and_fixed_lengths_are_unchanged() {
    for (packet, id, length) in [
        (ServerPacket::UPDATE_STAT, 140, UPDATE_STAT_BYTES),
        (ServerPacket::RUN_ENERGY, 64, RUN_ENERGY_BYTES),
        (ServerPacket::PLAYER_WEIGHT, 14, PLAYER_WEIGHT_BYTES),
    ] {
        assert_eq!(packet.wire(), id);
        assert_eq!(ServerPacket::from_wire(id), Some(packet));
        assert_eq!(packet.metadata().native_size, length as i32);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn initial_state_is_native_zero_not_an_invented_level_one_or_constitution_ten() {
    let state = native();
    assert_eq!(state.limits(), Limits { skill_count: 25 });
    assert_eq!(state.skills(), &[SkillState::default(); 25]);
    assert_eq!(state.skill(3).unwrap().base_level, 0);
    assert_eq!(state.skill(24).unwrap().base_level, 0);
    assert_eq!(state.skill(25), None);
    assert_eq!(state.skill(255), None);
    assert_eq!(state.run_energy(), 0);
    assert_eq!(state.player_weight(), 0);
    assert_eq!(state.recent_skill_updates(), &[0; 32]);
    assert_eq!(state.skill_update_cycle(), 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn limits_are_explicit_and_never_exceed_native_skill_count() {
    for skill_count in [0, SKILL_COUNT + 1, 256, usize::MAX] {
        let limits = Limits { skill_count };
        assert_eq!(PlayerStats::new(limits), Err(Error::InvalidLimits));
        assert_eq!(
            decode(ServerPacket::RUN_ENERGY, &[0], limits),
            Err(Error::InvalidLimits)
        );
    }
    for skill_count in 1..=SKILL_COUNT {
        let limits = Limits { skill_count };
        let mut state = PlayerStats::new(limits).unwrap();
        assert_eq!(state.skills().len(), skill_count);
        assert_eq!(state.limits(), limits);
        state
            .apply(
                ServerPacket::UPDATE_STAT,
                &stat(0, (skill_count - 1) as u8, 0),
            )
            .unwrap();
        let before = state.clone();
        assert_eq!(
            state.apply(ServerPacket::UPDATE_STAT, &stat(0, skill_count as u8, 0)),
            Err(Error::InvalidSkill {
                skill_id: skill_count as u8,
                skill_count
            })
        );
        assert_eq!(state, before);
        assert_eq!(state.skill(skill_count as u8), None);
        assert_eq!(
            state.apply(ServerPacket::RUN_ENERGY, &[255]),
            Ok(Update::RunEnergy(255))
        );
        assert_eq!(
            state.apply(ServerPacket::PLAYER_WEIGHT, &[128, 0]),
            Ok(Update::PlayerWeight(i16::MIN))
        );
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_wrong_packet_length_is_rejected_before_any_mutation() {
    let mut state = native();
    state
        .apply(ServerPacket::UPDATE_STAT, &stat(13034431, 2, 255))
        .unwrap();
    state.apply(ServerPacket::RUN_ENERGY, &[231]).unwrap();
    state
        .apply(ServerPacket::PLAYER_WEIGHT, &(-12345i16).to_be_bytes())
        .unwrap();
    for (opcode, expected) in [
        (ServerPacket::UPDATE_STAT, 6),
        (ServerPacket::RUN_ENERGY, 1),
        (ServerPacket::PLAYER_WEIGHT, 2),
    ] {
        for actual in (0..=16).chain([255, 65535, 65536]) {
            if actual == expected {
                continue;
            }
            let payload = vec![0; actual];
            let before = state.clone();
            let error = Error::UnexpectedLength { expected, actual };
            assert_eq!(
                decode(opcode, &payload, Limits::default()),
                Err(error.clone())
            );
            assert_eq!(state.apply(opcode, &payload), Err(error));
            assert_eq!(state, before);
        }
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_skill_ids_are_bounded_and_transactional() {
    let mut state = native();
    state
        .apply(ServerPacket::UPDATE_STAT, &stat(i32::MAX, 24, 120))
        .unwrap();
    for skill_id in SKILL_COUNT as u8..=u8::MAX {
        let before = state.clone();
        let error = Error::InvalidSkill {
            skill_id,
            skill_count: SKILL_COUNT,
        };
        assert_eq!(
            state.apply(ServerPacket::UPDATE_STAT, &stat(i32::MIN, skill_id, 255)),
            Err(error.clone())
        );
        assert_eq!(base_level_for_experience(skill_id, i32::MAX), Err(error));
        assert_eq!(state, before);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_other_registry_packets_remain_unsupported_by_this_opt_in_module() {
    let mut state = native();
    let mut rejected = 0;
    for wire in 0..=u16::MAX {
        let Some(opcode) = ServerPacket::from_wire(wire) else {
            continue;
        };
        if matches!(
            opcode,
            ServerPacket::UPDATE_STAT | ServerPacket::RUN_ENERGY | ServerPacket::PLAYER_WEIGHT
        ) {
            continue;
        }
        let before = state.clone();
        for payload in [&[][..], &[0, 0, 0, 0, 0, 0][..]] {
            assert_eq!(
                state.apply(opcode, payload),
                Err(Error::UnsupportedOpcode(opcode))
            );
            assert_eq!(state, before);
        }
        rejected += 1;
    }
    assert!(rejected > 100);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn xp_is_signed_big_endian_and_visible_level_is_an_independent_unsigned_byte() {
    let mut state = native();
    for experience in [
        i32::MIN,
        -123456789,
        -1,
        0,
        1,
        82,
        83,
        0x01234567,
        0x12345678,
        i32::MAX,
    ] {
        for visible_level in 0..=u8::MAX {
            let expected = SkillState {
                experience,
                visible_level,
                base_level: base_level_for_experience(24, experience).unwrap(),
            };
            let update = state
                .apply(
                    ServerPacket::UPDATE_STAT,
                    &stat(experience, 24, visible_level),
                )
                .unwrap();
            assert_eq!(
                update,
                Update::Stat {
                    skill_id: 24,
                    state: expected
                }
            );
            assert_eq!(state.skill(24), Some(&expected));
            assert_eq!(state.skill(0), Some(&SkillState::default()));
            if experience < 83 {
                assert_eq!(expected.base_level, 1);
            }
        }
    }
    let payload = [0x89, 0xab, 0xcd, 0xef, 7, 0x80];
    assert_eq!(
        decode(ServerPacket::UPDATE_STAT, &payload, Limits::default()).unwrap(),
        Update::Stat {
            skill_id: 7,
            state: SkillState {
                experience: 0x89abcdefu32 as i32,
                visible_level: 128,
                base_level: 1
            },
        }
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_energy_bytes_and_signed_weight_values_are_raw_and_not_transformed_or_clamped() {
    let mut state = native();
    state
        .apply(ServerPacket::UPDATE_STAT, &stat(174, 0, 3))
        .unwrap();
    let skills = state.skills().to_vec();
    let ring = *state.recent_skill_updates();
    let cursor = state.skill_update_cycle();
    for energy in 0..=u8::MAX {
        assert_eq!(
            state.apply(ServerPacket::RUN_ENERGY, &[energy]),
            Ok(Update::RunEnergy(energy))
        );
        assert_eq!(state.run_energy(), energy);
    }
    for weight in i16::MIN..=i16::MAX {
        assert_eq!(
            state.apply(ServerPacket::PLAYER_WEIGHT, &weight.to_be_bytes()),
            Ok(Update::PlayerWeight(weight))
        );
        assert_eq!(state.player_weight(), weight);
        assert_eq!(state.run_energy(), 255);
    }
    assert_eq!(state.skills(), skills);
    assert_eq!(state.recent_skill_updates(), &ring);
    assert_eq!(state.skill_update_cycle(), cursor);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn inclusive_thresholds_caps_and_unused_last_table_entry_match_native_rules() {
    assert_eq!(SKILL_COUNT, 25);
    assert_eq!(SKILL_MAX_LEVEL[..24], [99; 24]);
    assert_eq!(SKILL_MAX_LEVEL[24], 120);
    assert_eq!(SKILL_XP_FOR_LEVEL.len(), 120);
    assert_eq!(SKILL_XP_FOR_LEVEL[0], 83);
    assert_eq!(SKILL_XP_FOR_LEVEL[97], 13034431);
    assert_eq!(SKILL_XP_FOR_LEVEL[118], 104273167);
    assert_eq!(SKILL_XP_FOR_LEVEL[119], 115126838);
    assert!(SKILL_XP_FOR_LEVEL.windows(2).all(|pair| pair[0] < pair[1]));
    for skill_id in 0..SKILL_COUNT as u8 {
        let cap = SKILL_MAX_LEVEL[usize::from(skill_id)];
        for (index, threshold) in SKILL_XP_FOR_LEVEL.iter().copied().enumerate() {
            assert_eq!(
                base_level_for_experience(skill_id, threshold - 1).unwrap(),
                ((index + 1) as u8).min(cap)
            );
            assert_eq!(
                base_level_for_experience(skill_id, threshold).unwrap(),
                ((index + 2) as u8).min(cap)
            );
            assert_eq!(
                base_level_for_experience(skill_id, threshold + 1).unwrap(),
                ((index + 2) as u8).min(cap)
            );
        }
        assert_eq!(base_level_for_experience(skill_id, i32::MIN), Ok(1));
        assert_eq!(base_level_for_experience(skill_id, i32::MAX), Ok(cap));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn repeats_decreases_and_cross_skill_updates_append_to_the_native_ring() {
    let mut state = native();
    let mut ring = [0; SKILL_UPDATE_RING_SIZE];
    for index in 0..257 {
        let skill_id = ((index * 7) % SKILL_COUNT) as u8;
        let experience = [i32::MAX, 83, 83, -1, i32::MIN, 0][index % 6];
        state
            .apply(
                ServerPacket::UPDATE_STAT,
                &stat(experience, skill_id, index as u8),
            )
            .unwrap();
        ring[index & 31] = skill_id;
        assert_eq!(state.recent_skill_updates(), &ring);
        assert_eq!(state.skill_update_cycle(), (index + 1) as i32);
        assert_eq!(state.skill(skill_id).unwrap().experience, experience);
    }
    for _ in 0..65 {
        let previous = state.skill_update_cycle();
        state
            .apply(ServerPacket::UPDATE_STAT, &stat(0, 3, 0))
            .unwrap();
        assert_eq!(state.skill_update_cycle(), previous + 1);
    }
    assert_eq!(state.recent_skill_updates(), &[3; 32]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn deterministic_malformed_sequences_never_partially_mutate_state() {
    let mut state = native();
    let mut random = 0xc167a725u32;
    for _ in 0..10000 {
        random ^= random << 13;
        random ^= random >> 17;
        random ^= random << 5;
        let opcode = [
            ServerPacket::UPDATE_STAT,
            ServerPacket::RUN_ENERGY,
            ServerPacket::PLAYER_WEIGHT,
            ServerPacket::UPDATE_INV_FULL,
        ][(random & 3) as usize];
        let mut payload = random.to_be_bytes().repeat(3);
        payload[4] = ((random >> 8) % 30) as u8;
        payload.truncate(((random >> 16) % 13) as usize);
        let before = state.clone();
        let decoded = decode(opcode, &payload, state.limits());
        let applied = state.apply(opcode, &payload);
        assert_eq!(applied, decoded);
        match applied {
            Err(_) => assert_eq!(state, before),
            Ok(Update::Stat {
                skill_id,
                state: skill,
            }) => {
                assert_eq!(state.skill(skill_id), Some(&skill));
                assert_eq!(state.skill_update_cycle(), before.skill_update_cycle() + 1);
                assert_eq!(state.run_energy(), before.run_energy());
                assert_eq!(state.player_weight(), before.player_weight());
                for id in 0..SKILL_COUNT as u8 {
                    if id != skill_id {
                        assert_eq!(state.skill(id), before.skill(id));
                    }
                }
            }
            Ok(Update::RunEnergy(energy)) => {
                assert_eq!(state.run_energy(), energy);
                assert_eq!(state.skills(), before.skills());
                assert_eq!(state.player_weight(), before.player_weight());
                assert_eq!(state.skill_update_cycle(), before.skill_update_cycle());
                assert_eq!(state.recent_skill_updates(), before.recent_skill_updates());
            }
            Ok(Update::PlayerWeight(weight)) => {
                assert_eq!(state.player_weight(), weight);
                assert_eq!(state.skills(), before.skills());
                assert_eq!(state.run_energy(), before.run_energy());
                assert_eq!(state.skill_update_cycle(), before.skill_update_cycle());
                assert_eq!(state.recent_skill_updates(), before.recent_skill_updates());
            }
        }
    }
}
