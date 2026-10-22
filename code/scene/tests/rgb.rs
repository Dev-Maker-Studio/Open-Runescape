use openrs_scene::rgb::{HslRgbError, HslRgbLut, HSL_RGB_TABLE_LEN};
#[cfg(target_arch = "wasm32")]
use wasm_bindgen_test::wasm_bindgen_test;

const MANIFEST: &str = include_str!("fixtures/rgb.tsv");
const TABLES: [(&str, &[u8]); 8] = [
    ("seed-0", include_bytes!("fixtures/rgb-seed-0.bin")),
    ("seed-1", include_bytes!("fixtures/rgb-seed-1.bin")),
    ("seed-2", include_bytes!("fixtures/rgb-seed-2.bin")),
    ("seed-3", include_bytes!("fixtures/rgb-seed-3.bin")),
    ("seed-4", include_bytes!("fixtures/rgb-seed-4.bin")),
    ("seed-5", include_bytes!("fixtures/rgb-seed-5.bin")),
    ("boundary-0", include_bytes!("fixtures/rgb-boundary-0.bin")),
    ("boundary-1", include_bytes!("fixtures/rgb-boundary-1.bin")),
];

fn bits(value: &str) -> u64 {
    u64::from_str_radix(value, 16).unwrap()
}

fn native_entry(bytes: &[u8], index: usize) -> u32 {
    u32::from_le_bytes(bytes[index * 4..index * 4 + 4].try_into().unwrap())
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn all_entries_match_six_seeded_native_jvms_and_two_holder_only_boundaries() {
    let mut cases = 0;
    let mut compared = 0;
    for row in MANIFEST.lines().filter(|line| line.starts_with("case\t")) {
        let fields: Vec<_> = row.split('\t').collect();
        assert_eq!(fields.len(), 8);
        let case_id = fields[1];
        let (_, bytes) = TABLES.iter().find(|(id, _)| *id == case_id).unwrap();
        assert_eq!(bytes.len(), 2 * HSL_RGB_TABLE_LEN * 4);
        assert_eq!(fields[6], format!("rgb-{case_id}.bin"));
        let unit = f64::from_bits(bits(fields[4]));
        let gamma = f64::from_bits(bits(fields[5]));
        let sampled = HslRgbLut::from_random_unit(unit).unwrap();
        let explicit = HslRgbLut::from_gamma(gamma).unwrap();
        assert_eq!(sampled.gamma().to_bits(), gamma.to_bits(), "{case_id}");
        assert_eq!(explicit.gamma().to_bits(), gamma.to_bits(), "{case_id}");
        for lut in [&sampled, &explicit] {
            assert_eq!(lut.hsl_to_rgb().len(), HSL_RGB_TABLE_LEN);
            assert_eq!(lut.unsigned_hsl_to_rgb().len(), HSL_RGB_TABLE_LEN);
            for index in 0..HSL_RGB_TABLE_LEN {
                assert_eq!(
                    lut.hsl_to_rgb()[index],
                    native_entry(bytes, index),
                    "{case_id} hslToRgb[{index}] gamma={:016x}",
                    gamma.to_bits(),
                );
                assert_eq!(
                    lut.unsigned_hsl_to_rgb()[index],
                    native_entry(bytes, HSL_RGB_TABLE_LEN + index),
                    "{case_id} unsignedHslToRgb[{index}] gamma={:016x}",
                    gamma.to_bits(),
                );
                assert_eq!(lut.rgb(index as i32), Some(lut.hsl_to_rgb()[index]));
                assert_eq!(
                    lut.unsigned_rgb(index as i32),
                    Some(lut.unsigned_hsl_to_rgb()[index])
                );
                compared += 2;
            }
        }
        cases += 1;
    }
    assert_eq!(cases, 8);
    assert_eq!(compared, 2_097_152);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn fixture_pins_untouched_native_source_class_jar_and_holder_only_boundary_seam() {
    let expected = [
        ("format", "rgb-native-v1"),
        (
            "jar-sha256",
            "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048",
        ),
        (
            "native-source-sha256",
            "5b78ad7ca7f173848e328f0a4204e5bceff8a4163112b460d8fd2ce91b6a320c",
        ),
        (
            "current-source-sha256",
            "5b78ad7ca7f173848e328f0a4204e5bceff8a4163112b460d8fd2ce91b6a320c",
        ),
        (
            "oracle-source-sha256",
            "8a4464b363a198ab84d0129da8c69fc84304625fccea8ce91117b9d362228800",
        ),
        (
            "class-sha256",
            "89cd0840b5db7b3bbfa309eea868631b2a948be5ae2883aee786c7c4ba6e27f1",
        ),
        ("all-native-classes-unchanged", "true"),
        (
            "boundary-random-holder-seam",
            "Unsafe replaces Math$RandomNumberGeneratorHolder.randomNumberGenerator only",
        ),
        (
            "canonical-class-sha256",
            "9c2f05a8521b72ce298fbd625bc7a0902cbe89772fb6452ab1727894be1d4c70",
        ),
    ];
    let headers: Vec<_> = MANIFEST
        .lines()
        .filter(|line| !line.starts_with("case\t"))
        .map(|line| line.split_once('\t').unwrap())
        .collect();
    assert_eq!(headers, expected);
    let seeds: Vec<_> = MANIFEST
        .lines()
        .filter(|line| line.starts_with("case\tseed-"))
        .map(|line| {
            let fields: Vec<_> = line.split('\t').collect();
            assert_eq!(fields[2], "seed");
            fields[3].parse::<i64>().unwrap()
        })
        .collect();
    assert_eq!(
        seeds,
        [0, 42, 123456789, 987654321, 25214903917, 142379987759401]
    );
    let boundaries: Vec<_> = MANIFEST
        .lines()
        .filter(|line| line.starts_with("case\tboundary-"))
        .map(|line| {
            let fields: Vec<_> = line.split('\t').collect();
            assert_eq!(fields[2], "boundary");
            assert_eq!(fields[3], fields[4]);
            bits(fields[3])
        })
        .collect();
    assert_eq!(boundaries, [0, 1.0_f64.to_bits() - 1]);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn native_gamma_expression_preserves_binary64_endpoints_and_midpoint() {
    for (unit, expected_bits) in [
        (0.0, 0x3fe5eb851eb851eb),
        (f64::from_bits(1), 0x3fe5eb851eb851eb),
        (0.5, 0.7_f64.to_bits()),
        (f64::from_bits(1.0_f64.to_bits() - 1), 0x3fe6e147ae147ae1),
    ] {
        let lut = HslRgbLut::from_random_unit(unit).unwrap();
        assert_eq!(lut.gamma().to_bits(), expected_bits);
        assert_eq!(
            HslRgbLut::from_gamma(f64::from_bits(expected_bits))
                .unwrap()
                .gamma()
                .to_bits(),
            expected_bits
        );
    }
    assert_ne!(
        HslRgbLut::from_random_unit(0.0).unwrap().gamma().to_bits(),
        0.685_f64.to_bits()
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn invalid_random_samples_and_gamma_are_rejected_without_clamping() {
    for unit in [
        -f64::MIN_POSITIVE,
        -1.0,
        1.0,
        f64::from_bits(1.0_f64.to_bits() + 1),
        f64::NAN,
        f64::NEG_INFINITY,
        f64::INFINITY,
    ] {
        let error = HslRgbLut::from_random_unit(unit).unwrap_err();
        assert_eq!(error, HslRgbError::InvalidRandomUnit);
        assert!(error.to_string().contains("finite and in [0, 1)"));
    }
    for gamma in [
        f64::from_bits(0x3fe5eb851eb851ea),
        f64::from_bits(0x3fe6e147ae147ae2),
        0.0,
        -0.7,
        1.0,
        f64::NAN,
        f64::NEG_INFINITY,
        f64::INFINITY,
    ] {
        let error = HslRgbLut::from_gamma(gamma).unwrap_err();
        assert_eq!(error, HslRgbError::InvalidGamma);
        assert!(error.to_string().contains("finite and in"));
    }
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn each_explicit_owned_allocation_samples_once_and_lookup_never_resamples() {
    let mut calls = 0;
    let mut source = || {
        calls += 1;
        if calls == 1 {
            0.0
        } else {
            f64::from_bits(1.0_f64.to_bits() - 1)
        }
    };
    let first = HslRgbLut::sample(&mut source).unwrap();
    let second = HslRgbLut::sample(&mut source).unwrap();
    assert_eq!(calls, 2);
    assert_ne!(first.gamma().to_bits(), second.gamma().to_bits());
    assert_ne!(first.hsl_to_rgb().as_ptr(), second.hsl_to_rgb().as_ptr());
    assert_ne!(first.rgb(63), second.rgb(63));
    for _ in 0..32 {
        assert_eq!(first.rgb(0), Some(0));
        assert_eq!(second.rgb(0), Some(0));
    }
    assert_eq!(calls, 2);
    let mut invalid_calls = 0;
    let result = HslRgbLut::sample(|| {
        invalid_calls += 1;
        1.0
    });
    assert_eq!(result.unwrap_err(), HslRgbError::InvalidRandomUnit);
    assert_eq!(invalid_calls, 1);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn lookup_rejects_sentinels_and_out_of_range_without_masking_or_panicking() {
    let lut = HslRgbLut::from_random_unit(0.5).unwrap();
    for hsl in [i32::MIN, -12345678, -2, -1, 65536, 12345678, i32::MAX] {
        assert_eq!(lut.rgb(hsl), None);
        assert_eq!(lut.unsigned_rgb(hsl), None);
    }
    assert_eq!(lut.rgb(0), Some(0));
    assert_eq!(lut.unsigned_rgb(0), Some(0));
    assert_eq!(lut.rgb(65535), Some(lut.hsl_to_rgb()[65535]));
    assert_eq!(
        lut.unsigned_rgb(65535),
        Some(lut.unsigned_hsl_to_rgb()[65535])
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn packed_channels_are_swapped_without_alpha_and_native_zero_is_not_replaced() {
    let lut = HslRgbLut::from_random_unit(0.5).unwrap();
    for (index, (&rgb, &unsigned)) in lut
        .hsl_to_rgb()
        .iter()
        .zip(lut.unsigned_hsl_to_rgb())
        .enumerate()
    {
        assert_eq!(rgb >> 24, 0);
        assert_eq!(unsigned >> 24, 0);
        assert_eq!(
            unsigned,
            ((rgb & 0xff) << 16) | (rgb & 0xff00) | ((rgb >> 16) & 0xff)
        );
        if index & 0x7f == 0 {
            assert_eq!(rgb, 0);
            assert_eq!(unsigned, 0);
        }
    }
    assert_ne!(lut.rgb(1023), lut.unsigned_rgb(1023));
}
