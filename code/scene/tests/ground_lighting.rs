use openrs_scene::ground_lowering::{lighting_intensity, lighting_normals};

#[test]
fn flat_grid_points_straight_down() {
    let normals = lighting_normals(&[0; 25], 4, 4).unwrap();
    assert_eq!(normals[6], [0.0, -1.0, 0.0]);
    assert_eq!(normals[0], [0.0; 3]);
    assert_eq!(normals[4 + 4 * 5], [0.0; 3]);
}

#[test]
fn slope_matches_native_formula() {
    let mut heights = vec![0i32; 25];
    heights[7] = 1024;
    heights[5] = 0;
    let normals = lighting_normals(&heights, 4, 4).unwrap();
    let inverse = (1.0 / ((1048576.0f64 + 1024.0 * 1024.0).sqrt())) as f32;
    assert_eq!(normals[6], [1024.0 * inverse, -1024.0 * inverse, 0.0]);
}

#[test]
fn normals_reject_wrong_length() {
    assert!(lighting_normals(&[0; 24], 4, 4).is_err());
    assert!(lighting_intensity(&[0; 24], 4, 4).is_err());
}

#[test]
fn intensity_weights_neighbours_with_signed_shifts() {
    let mut shadows = vec![0u8; 25];
    shadows[6] = 64;
    shadows[1] = 0x80;
    shadows[7] = 8;
    let intensity = lighting_intensity(&shadows, 4, 4).unwrap();
    let expected = 1u8;
    assert_eq!(intensity[6], expected);
    assert_eq!(intensity[0], 0);
}
