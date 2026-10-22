use openrs_scene::loc_size::{
    loc_size2d, radius_2d, transformed_size2d, GROUND_INTERACT, SCENERY_INTERACT,
};
use openrs_scene::loc_type::{decode_loc_type, LocType};

fn loc() -> LocType {
    decode_loc_type(1, None, true).unwrap()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn radius_truncates_the_square_root_plus_point_nine_nine() {
    assert_eq!(radius_2d(&[100, -50], &[0, 30]), 100);
    assert_eq!(radius_2d(&[3], &[4]), 5);
    assert_eq!(radius_2d(&[], &[]), 0);
    assert_eq!(radius_2d(&[1], &[1]), 2);
    assert_eq!(radius_2d(&[0], &[2]), 2);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn rotation_by_quarter_turns_is_exact_and_matters_for_uneven_scale() {
    let xs = [100, -50];
    let zs = [0, 30];
    let mut def = loc();
    for rotation in 0..4 {
        assert_eq!(
            transformed_size2d(&def, SCENERY_INTERACT, rotation, &xs, &zs),
            100
        );
    }
    def.resize_z = 64;
    assert_eq!(transformed_size2d(&def, SCENERY_INTERACT, 0, &xs, &zs), 100);
    assert_eq!(transformed_size2d(&def, SCENERY_INTERACT, 1, &xs, &zs), 50);
    assert_eq!(transformed_size2d(&def, SCENERY_INTERACT, 2, &xs, &zs), 100);
    assert_eq!(transformed_size2d(&def, SCENERY_INTERACT, 3, &xs, &zs), 50);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn offsets_and_mirror_apply_before_the_ground_interact_turn() {
    let mut def = loc();
    def.offset_x = 40;
    assert_eq!(
        transformed_size2d(&def, SCENERY_INTERACT, 0, &[100, -50], &[0, 30]),
        140
    );
    let def = loc();
    assert_eq!(
        transformed_size2d(&def, GROUND_INTERACT, 0, &[100], &[0]),
        100
    );
    let mut mirrored = loc();
    mirrored.inverted = true;
    assert_eq!(
        transformed_size2d(&mirrored, SCENERY_INTERACT, 0, &[0], &[-7]),
        7
    );
    let mut shadow = loc();
    shadow.shadow_offset_z = 30;
    assert_eq!(
        transformed_size2d(&shadow, SCENERY_INTERACT, 0, &[0], &[10]),
        40
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn missing_shapes_models_or_meshes_give_no_size() {
    let mut def = loc();
    assert_eq!(loc_size2d(&def, SCENERY_INTERACT, 0, |_| None), None);
    def.shapes = Some(vec![0]);
    def.model_ids = Some(vec![vec![5]]);
    assert_eq!(loc_size2d(&def, SCENERY_INTERACT, 0, |_| None), None);
    def.shapes = Some(vec![10]);
    assert_eq!(loc_size2d(&def, SCENERY_INTERACT, 0, |_| None), None);
    let junk = [1u8, 2, 3];
    assert_eq!(
        loc_size2d(&def, SCENERY_INTERACT, 0, |_| Some(&junk[..])),
        None
    );
    def.model_ids = Some(vec![vec![]]);
    assert_eq!(
        loc_size2d(&def, GROUND_INTERACT, 0, |_| Some(&junk[..])),
        None
    );
}
