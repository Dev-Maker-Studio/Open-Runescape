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
fn projection_truncates_toward_zero_like_the_float_to_int_cast() {
    let vp = viewport();
    assert_eq!(project_vertex(&IDENTITY, &vp, [0, 0, 0]), Some((100, 50)));
    assert_eq!(project_vertex(&IDENTITY, &vp, [1, 1, 0]), Some((200, 100)));
    let mut halved = IDENTITY;
    halved[15] = 3.;
    assert_eq!(project_vertex(&halved, &vp, [1, -1, 0]), Some((133, 33)));
}

#[test]
fn vertices_behind_the_near_plane_are_dropped() {
    let mut behind = IDENTITY;
    behind[10] = 1.;
    behind[14] = -2.;
    assert_eq!(project_vertex(&behind, &viewport(), [0, 0, 0]), None);
}

#[test]
fn hovering_is_the_native_screen_bounding_box_overlap() {
    assert!(is_hovering((5, 5), [0, 10, 3], [0, 3, 10]));
    assert!(!is_hovering((11, 5), [0, 10, 3], [0, 3, 10]));
    assert!(is_hovering((10, 10), [0, 10, 3], [0, 3, 10]));
    assert!(!is_hovering((5, -1), [0, 10, 3], [0, 3, 10]));
}

#[test]
fn a_triangle_with_a_vertex_behind_the_camera_never_hits() {
    let triangle = [0, 0, 0, 1, 0, 0, 0, 1, -2];
    assert!(!triangles_hit(&IDENTITY, &viewport(), &triangle, (150, 70)));
}

#[test]
fn a_front_triangle_hits_inside_its_box() {
    let triangle = [0, 0, 0, 1, 0, 0, 0, 1, 0];
    assert!(triangles_hit(&IDENTITY, &viewport(), &triangle, (150, 70)));
    assert!(!triangles_hit(&IDENTITY, &viewport(), &triangle, (250, 70)));
}
