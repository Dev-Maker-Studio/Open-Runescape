use openrs_scene::picking::{
    entity_hovered, inverse_view_projection, minimap_angle, minimap_row_hit, minimap_tile,
    raycast_terrain_intersection, tile_under_mouse, BoundingBoxPolygon, HeightField, Matrix44,
    MinimapView, RaySurface, Viewport,
};
use std::collections::HashMap;

fn fixture(name: &str) -> String {
    let path = format!(
        "{}/tests/fixtures/picking-{name}.tsv",
        env!("CARGO_MANIFEST_DIR")
    );
    std::fs::read_to_string(path).unwrap()
}

fn fl(field: &str) -> f32 {
    f32::from_bits(field.parse::<i32>().unwrap() as u32)
}

fn same(a: f32, b: f32) -> bool {
    a.to_bits() == b.to_bits() || (a.is_nan() && b.is_nan())
}

fn floats(fields: &[&str]) -> Vec<f32> {
    fields.iter().map(|f| fl(f)).collect()
}

fn mat(fields: &[&str]) -> Matrix44 {
    let v = floats(fields);
    let mut e = [0.0f32; 16];
    e.copy_from_slice(&v);
    Matrix44(e)
}

fn same_all(a: &[f32], b: &[f32]) -> bool {
    a.len() == b.len() && a.iter().zip(b).all(|(x, y)| same(*x, *y))
}

fn ints(csv: &str) -> Vec<i32> {
    csv.split(',').map(|v| v.parse().unwrap()).collect()
}

fn field_for(sx: i32, sy: i32, scaling: i32, csv: &str) -> HeightField {
    HeightField::new(sx, sy, scaling.trailing_zeros() as i32, ints(csv))
}

struct Surface {
    size: (i32, i32),
    plane: i32,
    masks: Vec<i32>,
    fields: Vec<HeightField>,
}

impl RaySurface for Surface {
    fn scene_size(&self) -> (i32, i32) {
        self.size
    }
    fn player_plane(&self) -> i32 {
        self.plane
    }
    fn bridged(&self, x: i32, y: i32) -> bool {
        self.masks[(x * self.size.1 + y) as usize] & 2 != 0
    }
    fn average_height(&self, plane: i32, x: i32, y: i32) -> i32 {
        self.fields[plane as usize].average_height(x, y)
    }
}

#[test]
fn matrix_operations_match_kotlin() {
    let mut count = 0;
    for line in fixture("matrix").lines() {
        let f: Vec<&str> = line.split('\t').collect();
        assert_eq!(f.len(), 73);
        let a = mat(&f[1..17]);
        let b = mat(&f[17..33]);
        let mut product = a;
        product.multiply_by(&b);
        assert!(same_all(&product.0, &mat(&f[33..49]).0), "product {}", f[0]);
        let mut inverse = a;
        inverse.invert();
        assert!(same_all(&inverse.0, &mat(&f[49..65]).0), "inverse {}", f[0]);
        let extra = floats(&f[65..69]);
        assert!(same(a.determinant(), extra[0]), "determinant {}", f[0]);
        let scaled = a.scale(extra[1], extra[2], extra[3]);
        assert!(same_all(&scaled, &floats(&f[69..73])), "scale {}", f[0]);
        count += 1;
    }
    assert_eq!(count, 400);
}

#[test]
fn average_height_matches_kotlin() {
    let mut fields: HashMap<String, HeightField> = HashMap::new();
    let mut queries = 0;
    for line in fixture("height").lines() {
        let f: Vec<&str> = line.split('\t').collect();
        if f[0] == "G" {
            fields.insert(
                f[1].to_string(),
                field_for(
                    f[2].parse().unwrap(),
                    f[3].parse().unwrap(),
                    f[4].parse().unwrap(),
                    f[5],
                ),
            );
        } else {
            let field = &fields[f[1]];
            let got = field.average_height(f[2].parse().unwrap(), f[3].parse().unwrap());
            assert_eq!(got, f[4].parse::<i32>().unwrap(), "{line}");
            queries += 1;
        }
    }
    assert_eq!(queries, 1800);
}

#[derive(Default)]
struct RaySet {
    size: (i32, i32),
    fields: Vec<HeightField>,
    masks: Vec<i32>,
}

#[test]
fn terrain_raycast_matches_kotlin() {
    let mut scenes: HashMap<String, RaySet> = HashMap::new();
    let mut rays = 0;
    let mut hits = 0;
    for line in fixture("ray").lines() {
        let f: Vec<&str> = line.split('\t').collect();
        match f[0] {
            "S" => {
                let set = scenes.entry(f[1].to_string()).or_default();
                set.size = (f[2].parse().unwrap(), f[3].parse().unwrap());
            }
            "P" => {
                let set = scenes.get_mut(f[1]).unwrap();
                set.fields
                    .push(field_for(set.size.0, set.size.1, 512, f[3]));
            }
            "K" => {
                scenes.get_mut(f[1]).unwrap().masks = ints(f[2]);
            }
            _ => {
                let set = &scenes[f[1]];
                let surface = Surface {
                    size: set.size,
                    plane: f[2].parse().unwrap(),
                    masks: set.masks.clone(),
                    fields: set.fields.clone(),
                };
                let depth: i32 = f[3].parse().unwrap();
                let v = floats(&f[4..11]);
                let got = raycast_terrain_intersection(
                    &surface,
                    [v[0], v[1], v[2]],
                    [v[3], v[4], v[5]],
                    depth,
                );
                assert!(same(got, v[6]), "ray {line}");
                rays += 1;
                if v[6] > 0.0 {
                    hits += 1;
                }
            }
        }
    }
    assert_eq!(rays, 660);
    assert!(hits > 300);
}

#[test]
fn tile_under_mouse_matches_kotlin() {
    let mut size = (0, 0);
    let mut fields = Vec::new();
    let mut masks = Vec::new();
    let mut total = 0;
    let mut resolved = 0;
    for line in fixture("tile").lines() {
        let f: Vec<&str> = line.split('\t').collect();
        match f[0] {
            "S" => size = (f[1].parse().unwrap(), f[2].parse().unwrap()),
            "P" => fields.push(field_for(size.0, size.1, 512, f[2])),
            "K" => masks = ints(f[1]),
            _ => {
                let surface = Surface {
                    size,
                    plane: f[1].parse().unwrap(),
                    masks: masks.clone(),
                    fields: fields.clone(),
                };
                let player_size: i32 = f[2].parse().unwrap();
                let vx: i32 = f[3].parse().unwrap();
                let vy: i32 = f[4].parse().unwrap();
                let gw: i32 = f[5].parse().unwrap();
                let gh: i32 = f[6].parse().unwrap();
                let mx: i32 = f[7].parse().unwrap();
                let my: i32 = f[8].parse().unwrap();
                let sun = mat(&f[9..25]);
                let camera = mat(&f[25..41]);
                let tx: i32 = f[41].parse().unwrap();
                let ty: i32 = f[42].parse().unwrap();
                let inverse = inverse_view_projection(&sun, &camera);
                let viewport = Viewport {
                    x: vx,
                    y: vy,
                    width: gw,
                    height: gh,
                };
                let got = tile_under_mouse(&surface, &inverse, mx, my, &viewport, player_size);
                let want = if tx == -1 && ty == -1 {
                    None
                } else {
                    Some((tx, ty))
                };
                assert_eq!(got, want, "{line}");
                total += 1;
                if want.is_some() {
                    resolved += 1;
                }
            }
        }
    }
    assert_eq!(total, 400);
    assert!(resolved > 200);
}

#[test]
fn bounding_polygon_matches_kotlin() {
    let mut hits = 0;
    let mut lines = 0;
    for line in fixture("polygon").lines() {
        let f: Vec<i32> = line.split('\t').map(|v| v.parse().unwrap()).collect();
        let polygon = BoundingBoxPolygon {
            top: f[0],
            left: f[1],
            bottom: f[2],
            right: f[3],
            size: f[4],
            position_calculated: f[5] == 1,
        };
        let got = entity_hovered(&[polygon], f[6], 0, 0, f[7], f[8], || true);
        assert_eq!(got, f[9] == 1, "{line}");
        hits += f[9];
        lines += 1;
    }
    assert_eq!(lines, 3000);
    assert!(hits > 500);
}

fn view(locked: bool) -> MinimapView {
    MinimapView {
        component_x: 100,
        component_y: 50,
        width: 150,
        height: 150,
        camera_yaw: 0,
        minimap_rotation: 0,
        minimap_zoom: 0,
        locked_camera: locked,
        camera_target_x: 50 * 512 + 256,
        camera_target_y: 60 * 512 + 256,
        player_size: 1,
        player_x: 20 * 512 + 256,
        player_z: 30 * 512 + 256,
    }
}

#[test]
fn minimap_tile_north_up_walks_toward_the_click() {
    let v = view(false);
    let angle = minimap_angle(&v);
    assert_eq!(angle, 0);
    let (sine, cosine) = (0, 16384);
    assert_eq!(minimap_tile(&v, 175, 125, sine, cosine), (20, 30));
    assert_eq!(minimap_tile(&v, 175 + 40, 125, sine, cosine), (30, 30));
    assert_eq!(minimap_tile(&v, 175, 125 - 40, sine, cosine), (20, 40));
    assert_eq!(minimap_tile(&v, 175 - 8, 125 + 8, sine, cosine), (18, 28));
}

#[test]
fn minimap_tile_applies_zoom_and_size_offset() {
    let mut v = view(false);
    v.minimap_zoom = 256;
    v.player_size = 3;
    let got = minimap_tile(&v, 175 + 40, 125, 0, 16384);
    assert_eq!(got, (((20 * 512 + 256 - 512) >> 9) + 20, 29));
}

#[test]
fn minimap_tile_locked_camera_uses_target() {
    let v = view(true);
    assert_eq!(minimap_angle(&v), 0);
    assert_eq!(minimap_tile(&v, 175, 125, 0, 16384), (50, 60));
    assert_eq!(minimap_tile(&v, 175 + 20, 125 - 20, 0, 16384), (55, 65));
}

#[test]
fn minimap_angle_masks_rotation() {
    let mut v = view(false);
    v.camera_yaw = 16000;
    v.minimap_rotation = 1000;
    assert_eq!(minimap_angle(&v), (17000 & 0x3fff) as usize);
    v.locked_camera = true;
    assert_eq!(minimap_angle(&v), 16000);
}

#[test]
fn minimap_row_bounds() {
    assert!(minimap_row_hit(110, 100, 5, 20));
    assert!(!minimap_row_hit(104, 100, 5, 20));
    assert!(minimap_row_hit(125, 100, 5, 20));
    assert!(!minimap_row_hit(126, 100, 5, 20));
}
