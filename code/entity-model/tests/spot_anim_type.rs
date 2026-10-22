use openrs_entity::config::SpotAnimType;

#[test]
fn defaults_apply_when_the_entry_is_empty() {
    let ty = SpotAnimType::decode(&[0]).unwrap();
    assert_eq!(ty.sequence_id, -1);
    assert_eq!((ty.scale_xz, ty.scale_y), (128, 128));
    assert_eq!(ty.contour_modifier, -1);
    assert!(!ty.replay);
}

#[test]
fn opcodes_fill_the_fields() {
    let bytes = [
        1, 0x01, 0x00, 2, 0x00, 0x2a, 4, 0, 64, 5, 0, 200, 6, 0, 90, 10, 14, 3, 40, 1, 0, 5, 0, 9,
        0,
    ];
    let ty = SpotAnimType::decode(&bytes).unwrap();
    assert_eq!(ty.model_id, 256);
    assert_eq!(ty.sequence_id, 42);
    assert_eq!((ty.scale_xz, ty.scale_y), (64, 200));
    assert_eq!(ty.rotation, 90);
    assert_eq!(ty.fixed_rotation(), 4096);
    assert!(ty.replay);
    assert_eq!((ty.contour_type, ty.contour_modifier), (2, 768));
    assert_eq!(ty.recolor_src, Some(vec![5]));
    assert_eq!(ty.recolor_dst, Some(vec![9]));
}

#[test]
fn truncated_data_is_an_error() {
    assert!(SpotAnimType::decode(&[4, 0]).is_err());
}
