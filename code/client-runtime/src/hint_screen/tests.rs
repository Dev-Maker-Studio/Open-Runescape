use super::*;

const IDENTITY: [f32; 16] = [
    1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1.,
];

fn viewport() -> ProjectionViewport {
    ProjectionViewport {
        origin_x: 100.,
        origin_y: 50.,
        center_x: 100.,
        center_y: 50.,
    }
}

#[test]
fn world_to_screen_rejects_points_outside_the_clip_volume() {
    assert_eq!(
        world_to_screen(&IDENTITY, &viewport(), 0.5, 0.5, 0.),
        Some((150., 75.))
    );
    assert_eq!(world_to_screen(&IDENTITY, &viewport(), 2., 0., 0.), None);
    assert_eq!(world_to_screen(&IDENTITY, &viewport(), 0., 0., 2.), None);
}

#[test]
fn placement_rejects_positions_outside_the_scene_margin() {
    let offset = HintOffset { x: 4., y: 6. };
    assert!(hint_arrow_placement(&IDENTITY, &viewport(), (104, 104), (511, 0, 600), offset).is_none());
    assert!(hint_arrow_placement(&IDENTITY, &viewport(), (104, 104), (600, 0, 102 * 512 + 1), offset).is_none());
    assert_eq!(
        hint_arrow_placement(&IDENTITY, &viewport(), (104, 104), (600, 0, 600), offset),
        None
    );
}

#[test]
fn tile_arrows_blink_on_the_twenty_cycle_phase() {
    assert_eq!(tile_arrow_icon((50., 60.), 9), Some((38, 32)));
    assert_eq!(tile_arrow_icon((50., 60.), 10), None);
    assert_eq!(tile_arrow_icon((50., 60.), 30), None);
}

#[test]
fn overhead_colours_follow_the_type_table_and_the_progress_ramps() {
    assert_eq!(overhead_text_color(0, 0, 10, 10), 16_776_960);
    assert_eq!(overhead_text_color(3, 0, 10, 10), 65_535);
    assert_eq!(overhead_text_color(6, 3, 1, 1), 16_711_680);
    assert_eq!(overhead_text_color(6, 12, 1, 1), 16_776_960);
    assert_eq!(overhead_text_color(7, 12, 1, 1), 65_535);
    assert_eq!(overhead_text_color(8, 0, 1, 1), 45_056);
    assert_eq!(overhead_text_color(9, 0, 150, 150), 16_711_680);
    assert_eq!(overhead_text_color(9, 0, 100, 150), 16_776_960);
    assert_eq!(overhead_text_color(11, 0, 150, 150), 16_777_215);
    assert_eq!(overhead_text_color(99, 0, 1, 1), 16_776_960);
}

#[test]
fn stacked_labels_move_up_by_the_font_padding_until_they_clear() {
    let boxes = [
        LabelBox { x: 100, y: 200, half_width: 30 },
        LabelBox { x: 110, y: 200, half_width: 30 },
        LabelBox { x: 400, y: 200, half_width: 30 },
    ];
    assert_eq!(stack_labels(&boxes, 14), vec![200, 186, 200]);
    let triple = [boxes[0], boxes[1], LabelBox { x: 105, y: 200, half_width: 30 }];
    assert_eq!(stack_labels(&triple, 14), vec![200, 186, 172]);
}

#[test]
fn minimap_hints_fall_back_to_a_rotated_edge_marker_when_outside_the_mask() {
    let geometry = MinimapGeometry {
        component_width: 150,
        component_height: 150,
        sprite_width: 8,
        sprite_height: 8,
        locked_camera: true,
        minimap_zoom: 0,
        sine: 0,
        cosine: 16384,
    };
    let inside = minimap_hint(&geometry, 5, 10, 360_000, &|_, _| true);
    assert_eq!(inside, Some(MinimapHint::Marker { x: 5 + 75 - 4, y: -10 + 75 - 8 }));
    let outside = minimap_hint(&geometry, 0, 100, 360_000, &|_, _| false);
    assert_eq!(
        outside,
        Some(MinimapHint::Edge { x: 75., y: 75. - 69., rotation: 0 })
    );
    assert_eq!(minimap_hint(&geometry, 700, 700, 360_000, &|_, _| true), None);
}
