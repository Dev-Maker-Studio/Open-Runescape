use super::*;

const SEEDS: [i64; 8] = [1, 2, 3, 42, 727, 123456789, -5, 0x7fffffffffff];

fn fixture_table() -> Vec<i32> {
    include_bytes!("../../../cs2/tests/fixtures/hsv.native.bin")
        .chunks_exact(4)
        .map(|chunk| i32::from_be_bytes(chunk.try_into().unwrap()))
        .collect()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn java_random_reproduces_every_seeded_draw_recorded_from_the_kotlin_client() {
    let fixture = include_str!("../../../cs2/tests/fixtures/clock.native.tsv");
    let mut checked = 0;
    for (index, seed) in SEEDS.iter().enumerate() {
        let line = fixture
            .lines()
            .find(|line| line.starts_with(&format!("random-{index}\t")))
            .unwrap();
        let columns: Vec<_> = line.split('\t').collect();
        let recorded = f64::from_bits(columns[6].parse::<i64>().unwrap() as u64);
        assert_eq!(
            JavaRandom::new(*seed).next_double().to_bits(),
            recorded.to_bits()
        );
        checked += 1;
    }
    assert_eq!(checked, 8);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn hsv_table_built_from_the_seeded_gamma_matches_the_kotlin_table_entry_for_entry() {
    let mut environment = Environment::new(Some(0));
    environment.seed(20120727);
    let table = environment.hsv_table();
    let expected = fixture_table();
    assert_eq!(table.len(), expected.len());
    let differing = table
        .iter()
        .zip(&expected)
        .filter(|(left, right)| left != right)
        .count();
    assert_eq!(differing, 0);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn wall_clock_is_the_injected_base_plus_the_monotonic_session_time() {
    let mut environment = Environment::new(None);
    assert_eq!(environment.epoch_ms(5), None);
    environment.set_epoch_base(1_700_000_000_500, 500);
    assert_eq!(environment.epoch_ms(0), Some(1_700_000_000_000));
    assert_eq!(environment.epoch_ms(2_500), Some(1_700_000_002_500));
    environment.set_utc_offset(3_600_000);
    assert_eq!(environment.offset_ms(), 3_600_000);
    assert_eq!(environment.language(), Some(0));
}
