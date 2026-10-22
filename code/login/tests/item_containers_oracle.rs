use openrs_login::item_containers::{ContainerKey, ItemContainers, Limits};
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeSet;

struct Fixture {
    name: &'static str,
    payload: &'static [u8],
    values: &'static [u8],
}

macro_rules! fixture {
    ($name:literal) => {
        Fixture {
            name: $name,
            payload: include_bytes!(concat!("fixtures/item-containers-", $name, ".payload")),
            values: include_bytes!(concat!(
                "fixtures/item-containers-",
                $name,
                ".native-values"
            )),
        }
    };
}

const FIXTURES: &[Fixture] = &[
    fixture!("zero-full-absent"),
    fixture!("header-only-partial-absent"),
    fixture!("delete-absent"),
    fixture!("full-quantities-literal"),
    fixture!("full-mask-cp1252"),
    fixture!("full-shrink-sparse-tail"),
    fixture!("partial-packed-keep"),
    fixture!("partial-packed-clear"),
    fixture!("partial-packed-set-empty-item"),
    fixture!("partial-bare-clears"),
    fixture!("partial-smart-127-128"),
    fixture!("full-shrink-resets-tail"),
    fixture!("full-sparse-tail-duplicate"),
    fixture!("header-only-partial-existing"),
    fixture!("partial-packed-keep-three"),
    fixture!("negative-full-same-id"),
    fixture!("negative-partial"),
    fixture!("delete-positive-opposite"),
    fixture!("zero-full-positive-opposite"),
    fixture!("header-only-positive-opposite"),
    fixture!("positive-recreate"),
    fixture!("delete-negative-existing"),
    fixture!("delete-negative-absent"),
    fixture!("zero-full-existing"),
    fixture!("full-both-flags-precedence"),
    fixture!("partial-high-flags-packed"),
    fixture!("partial-high-flags-bare"),
    fixture!("negative-zero-full-absent"),
    fixture!("negative-header-only-absent"),
    fixture!("full-mask-zero-absent"),
    fixture!("full-sparse-zero-absent"),
    fixture!("full-sparse-count-127"),
    fixture!("full-sparse-count-128"),
    fixture!("partial-bare-duplicates"),
    fixture!("full-extended-small-amounts"),
    fixture!("full-mask-empty-items"),
    fixture!("full-zero-existing-mask"),
    fixture!("full-sparse-zero-existing"),
    fixture!("partial-packed-modes-all"),
    fixture!("max-bare-slot"),
    fixture!("max-bare-header-only"),
    fixture!("delete-max-bare"),
    fixture!("max-packed-slot"),
    fixture!("max-packed-set-empty-item"),
    fixture!("zero-full-max-packed-existing"),
    fixture!("delete-max-packed"),
    fixture!("delete-zero-id-absent"),
    fixture!("partial-zero-id-create"),
    fixture!("delete-zero-id"),
    fixture!("full-max-id-positive"),
    fixture!("full-max-id-negative"),
    fixture!("delete-max-id-positive"),
    fixture!("delete-max-id-negative"),
    fixture!("zero-full-zero-id-negative"),
    fixture!("header-only-max-id-absent"),
    fixture!("partial-extended-small-amounts"),
];

fn int(output: &mut Vec<u8>, value: i32) {
    output.extend_from_slice(&value.to_be_bytes());
}

fn snapshot(state: &ItemContainers, tracked: &BTreeSet<ContainerKey>, consumed: usize) -> Vec<u8> {
    let mut output = Vec::new();
    int(&mut output, consumed as i32);
    int(&mut output, tracked.len() as i32);
    for &key in tracked {
        int(&mut output, i32::from(key.id));
        int(&mut output, i32::from(key.negative_key));
        let container = state.get(key);
        int(&mut output, i32::from(container.is_some()));
        int(
            &mut output,
            container.map_or(0, |container| container.len() as i32),
        );
        if let Some(container) = container {
            for slot in container.slots() {
                int(&mut output, slot.item_id);
                int(&mut output, slot.amount);
                if let Some(tooltip) = &slot.tooltip {
                    int(&mut output, tooltip.len() as i32);
                    output.extend_from_slice(tooltip.as_bytes());
                } else {
                    int(&mut output, -1);
                }
            }
        }
    }
    output
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn fifty_six_native_packet_and_container_snapshots_match_complete_state_after_each_operation() {
    let provenance = include_str!("fixtures/item-containers-oracle.sha256");
    assert_eq!(
        provenance.trim(),
        "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
    );
    let manifest = include_str!("fixtures/item-containers-cases.tsv");
    let mut lines = manifest.lines();
    assert_eq!(
        lines.next(),
        Some("order\tname\topcode\tid\tnegative\tpayload\tsnapshot")
    );
    assert_eq!(lines.clone().count(), FIXTURES.len());
    assert_eq!(FIXTURES.len(), 56);
    let mut state = ItemContainers::new(Limits::default()).unwrap();
    let mut tracked = BTreeSet::new();
    let mut recent = [0u16; 32];
    for (index, (fixture, line)) in FIXTURES.iter().zip(lines).enumerate() {
        let fields: Vec<_> = line.split('\t').collect();
        assert_eq!(fields.len(), 7);
        assert_eq!(fields[0].parse::<usize>().unwrap(), index + 1);
        assert_eq!(fields[1], fixture.name);
        assert_eq!(
            fields[5],
            format!("item-containers-{}.payload", fixture.name)
        );
        assert_eq!(
            fields[6],
            format!("item-containers-{}.native-values", fixture.name)
        );
        let opcode = ServerPacket::from_wire(fields[2].parse().unwrap()).unwrap();
        let key = ContainerKey {
            id: fields[3].parse().unwrap(),
            negative_key: fields[4] == "1",
        };
        tracked.insert(key);
        let update = state.apply(opcode, fixture.payload).unwrap();
        assert_eq!(update.key, key);
        assert_eq!(
            snapshot(&state, &tracked, fixture.payload.len()),
            fixture.values,
            "{}",
            fixture.name
        );
        assert_eq!(
            update.slot_count,
            state.get(key).map_or(0, |container| container.len())
        );
        assert_eq!(update.container_present, state.get(key).is_some());
        let containers: Vec<_> = tracked.iter().filter_map(|&key| state.get(key)).collect();
        assert_eq!(state.len(), containers.len());
        assert_eq!(
            state.total_slots(),
            containers
                .iter()
                .map(|container| container.len())
                .sum::<usize>()
        );
        assert_eq!(
            state.tooltip_bytes(),
            containers
                .iter()
                .flat_map(|container| container.slots())
                .filter_map(|slot| slot.tooltip.as_ref())
                .map(String::len)
                .sum::<usize>()
        );
        recent[index & 31] = key.id;
        assert_eq!(state.recently_used(), &recent);
        assert_eq!(state.recent_cursor(), (index + 1) as i32);
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_full_and_stop_fixture_rejects_one_byte_truncation_without_mutation() {
    let manifest = include_str!("fixtures/item-containers-cases.tsv");
    let mut state = ItemContainers::new(Limits::default()).unwrap();
    for (fixture, line) in FIXTURES.iter().zip(manifest.lines().skip(1)) {
        let fields: Vec<_> = line.split('\t').collect();
        let opcode = ServerPacket::from_wire(fields[2].parse().unwrap()).unwrap();
        if opcode != ServerPacket::UPDATE_INV_PARTIAL {
            let before = state.clone();
            assert!(
                state
                    .apply(opcode, &fixture.payload[..fixture.payload.len() - 1])
                    .is_err(),
                "{}",
                fixture.name
            );
            assert_eq!(state, before);
        }
        state.apply(opcode, fixture.payload).unwrap();
    }
}
