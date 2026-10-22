use openrs_protocol::isaac::Isaac;
use openrs_protocol::Error;

const ZERO: &[u8] = include_bytes!("fixtures/isaac-zero.words");
const MIXED: &[u8] = include_bytes!("fixtures/isaac-mixed.words");
const FULL: &[u8] = include_bytes!("fixtures/isaac-full.words");

fn check(seed: &[u32], vector: &[u8]) {
    assert_eq!(vector.len(), 520 * 4);
    let mut cipher = Isaac::new(seed).unwrap();
    for (index, bytes) in vector.chunks_exact(4).enumerate() {
        let expected = u32::from_be_bytes(bytes.try_into().unwrap());
        let peek = cipher.peek_u32();
        assert_eq!(cipher.peek_u32(), peek);
        let word = cipher.next_u32();
        assert_eq!(word, peek);
        assert_eq!(word, expected, "word {index}");
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_zero_seed_vectors_include_reverse_order_and_two_refills() {
    assert_eq!(&ZERO[..4], &0x182600f3u32.to_be_bytes());
    assert_eq!(&ZERO[256 * 4..257 * 4], &0x7a68710fu32.to_be_bytes());
    assert_eq!(&ZERO[512 * 4..513 * 4], &0x4bb5af29u32.to_be_bytes());
    check(&[], ZERO);
    check(&[0; 256], ZERO);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_overflowing_seed_vectors_include_logical_shifts_and_wrapping_addition() {
    check(&[1, 0xfffffffe, 0x12345678, 0x80000000], MIXED);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn kotlin_full_seed_exercises_every_seed_word() {
    let seed: [u32; 256] = std::array::from_fn(|index| {
        (index as u32)
            .wrapping_mul(0x1234567)
            .wrapping_add(0x80000000)
    });
    check(&seed, FULL);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn seed_length_is_checked_instead_of_indexing_past_state() {
    assert!(matches!(
        Isaac::new(&[0; 257]),
        Err(Error::InvalidSeedLength)
    ));
}
