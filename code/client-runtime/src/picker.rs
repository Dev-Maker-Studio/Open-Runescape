use crate::menu_targets::TileTarget;
use openrs_scene::picking::Matrix44;
use openrs_scene::picking::{
    HeightField, MinimapView, RaySurface, Viewport, minimap_tile, tile_under_mouse,
};

const PLANES: usize = 4;
const TILE_SHIFT: i32 = 9;
const NO_HEIGHT: i32 = i32::MIN;

pub struct Picker {
    size: i32,
    fields: Vec<HeightField>,
    bridge: Vec<bool>,
    plane: i32,
}

struct Surface<'a> {
    picker: &'a Picker,
}

impl RaySurface for Surface<'_> {
    fn scene_size(&self) -> (i32, i32) {
        (self.picker.size, self.picker.size)
    }

    fn player_plane(&self) -> i32 {
        self.picker.plane
    }

    fn bridged(&self, x: i32, y: i32) -> bool {
        self.picker
            .bridge
            .get((x * self.picker.size + y) as usize)
            .copied()
            .unwrap_or(false)
    }

    fn average_height(&self, plane: i32, x: i32, y: i32) -> i32 {
        self.picker
            .fields
            .get(plane as usize)
            .map_or(0, |field| field.average_height(x, y))
    }
}

impl Picker {
    pub fn new(tiles: usize, heights: &[i32], bridge: &[u8]) -> Option<Self> {
        let stride = tiles + 1;
        let per_plane = stride * stride;
        if tiles == 0 || heights.len() != PLANES * per_plane || bridge.len() != tiles * tiles {
            return None;
        }
        let fields = heights
            .chunks(per_plane)
            .map(|chunk| {
                let values = chunk
                    .iter()
                    .map(|h| if *h == NO_HEIGHT { 0 } else { *h })
                    .collect();
                HeightField::new(tiles as i32, tiles as i32, TILE_SHIFT, values)
            })
            .collect();
        Some(Self {
            size: tiles as i32,
            fields,
            bridge: bridge.iter().map(|b| *b != 0).collect(),
            plane: 0,
        })
    }

    pub fn set_plane(&mut self, plane: i32) {
        self.plane = plane.clamp(0, 3);
    }

    pub fn tile(
        &self,
        view_projection: &[f32],
        mouse: (i32, i32),
        viewport: Viewport,
        player_size: i32,
    ) -> Option<TileTarget> {
        let matrix: [f32; 16] = view_projection.try_into().ok()?;
        let mut inverse = Matrix44(matrix);
        inverse.invert();
        let surface = Surface { picker: self };
        let (x, y) =
            tile_under_mouse(&surface, &inverse, mouse.0, mouse.1, &viewport, player_size)?;
        (x >= 0 && y >= 0 && x < self.size && y < self.size).then_some(TileTarget { x, y })
    }
}

#[cfg_attr(not(target_arch = "wasm32"), allow(dead_code))]
pub fn minimap_target(view: &MinimapView, mouse: (i32, i32), sine: i32, cosine: i32) -> TileTarget {
    let (x, y) = minimap_tile(view, mouse.0, mouse.1, sine, cosine);
    TileTarget { x, y }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn flat(tiles: usize, height: i32) -> Vec<i32> {
        vec![height; PLANES * (tiles + 1) * (tiles + 1)]
    }

    #[test]
    fn rejects_mismatched_grids() {
        assert!(Picker::new(8, &flat(7, 0), &[0; 64]).is_none());
        assert!(Picker::new(8, &flat(8, 0), &[0; 63]).is_none());
        assert!(Picker::new(8, &flat(8, 0), &[0; 64]).is_some());
    }

    #[test]
    fn a_ray_straight_down_resolves_the_tile_beneath_it() {
        let picker = Picker::new(16, &flat(16, 0), &[0; 256]).unwrap();
        let centre = (5 * 512 + 100) as f32;
        let mut unproject = Matrix44::identity();
        unproject.0[0] = 4096.0;
        unproject.0[12] = centre;
        unproject.0[5] = 0.0;
        unproject.0[6] = 4096.0;
        unproject.0[14] = centre;
        unproject.0[10] = 0.0;
        unproject.0[9] = 1000.0;
        let mut forward = unproject;
        forward.invert();
        let viewport = Viewport {
            x: 0,
            y: 0,
            width: 400,
            height: 300,
        };
        let hit = picker.tile(&forward.0, (200, 150), viewport, 1).unwrap();
        assert_eq!((hit.x, hit.y), (5, 5));
        assert!(picker.tile(&forward.0, (-1, 150), viewport, 1).is_none());
        assert!(picker.tile(&[0.0; 3], (200, 150), viewport, 1).is_none());
        let surface = Surface { picker: &picker };
        assert_eq!(surface.scene_size(), (16, 16));
        assert_eq!(surface.average_height(9, 0, 0), 0);
    }

    #[test]
    fn minimap_target_uses_the_angle_tables() {
        let view = MinimapView {
            component_x: 100,
            component_y: 50,
            width: 150,
            height: 150,
            camera_yaw: 0,
            minimap_rotation: 0,
            minimap_zoom: 0,
            locked_camera: false,
            camera_target_x: 0,
            camera_target_y: 0,
            player_size: 1,
            player_x: 20 * 512 + 256,
            player_z: 30 * 512 + 256,
        };
        let target = minimap_target(&view, (175 + 40, 125), 0, 16384);
        assert_eq!((target.x, target.y), (30, 30));
        let angle = openrs_scene::picking::minimap_angle(&view) as usize;
        assert_eq!(openrs_entity::trig::sine()[angle], 0);
        assert_eq!(openrs_entity::trig::cosine()[angle], 16384);
    }
}

#[cfg(target_arch = "wasm32")]
mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = Picker)]
    pub struct BrowserPicker(Picker);

    #[wasm_bindgen(js_class = Picker)]
    impl BrowserPicker {
        #[wasm_bindgen(constructor)]
        pub fn new(tiles: u32, heights: &[i32], bridge: &[u8]) -> Result<BrowserPicker, JsValue> {
            Picker::new(tiles as usize, heights, bridge)
                .map(Self)
                .ok_or_else(|| JsValue::from_str("invalid picking grid"))
        }

        #[wasm_bindgen(js_name = setPlane)]
        pub fn set_plane(&mut self, plane: i32) {
            self.0.set_plane(plane);
        }

        pub fn tile(
            &self,
            view_projection: &[f32],
            mouse_x: i32,
            mouse_y: i32,
            width: i32,
            height: i32,
            player_size: i32,
        ) -> Vec<i32> {
            let viewport = Viewport {
                x: 0,
                y: 0,
                width,
                height,
            };
            self.0
                .tile(view_projection, (mouse_x, mouse_y), viewport, player_size)
                .map_or_else(Vec::new, |t| vec![t.x, t.y])
        }

        #[wasm_bindgen(js_name = minimapAngle)]
        pub fn minimap_angle_of(view: &[i32]) -> i32 {
            minimap_view(view).map_or(0, |v| openrs_scene::picking::minimap_angle(&v) as i32)
        }

        #[wasm_bindgen(js_name = minimapPick)]
        pub fn minimap_pick(view: &[i32], mouse_x: i32, mouse_y: i32) -> Vec<i32> {
            minimap_view(view).map_or_else(Vec::new, |v| {
                let angle = openrs_scene::picking::minimap_angle(&v) as i32;
                let target = minimap_target(
                    &v,
                    (mouse_x, mouse_y),
                    openrs_entity::trig::sine()[angle as usize],
                    openrs_entity::trig::cosine()[angle as usize],
                );
                vec![target.x, target.y]
            })
        }

        #[wasm_bindgen(js_name = minimapTile)]
        pub fn minimap_tile_of(
            view: &[i32],
            mouse_x: i32,
            mouse_y: i32,
            sine: i32,
            cosine: i32,
        ) -> Vec<i32> {
            minimap_view(view).map_or_else(Vec::new, |v| {
                let t = minimap_target(&v, (mouse_x, mouse_y), sine, cosine);
                vec![t.x, t.y]
            })
        }
    }

    fn minimap_view(v: &[i32]) -> Option<MinimapView> {
        let v: &[i32; 13] = v.try_into().ok()?;
        Some(MinimapView {
            component_x: v[0],
            component_y: v[1],
            width: v[2],
            height: v[3],
            camera_yaw: v[4],
            minimap_rotation: v[5],
            minimap_zoom: v[6],
            locked_camera: v[7] != 0,
            camera_target_x: v[8],
            camera_target_y: v[9],
            player_size: v[10],
            player_x: v[11],
            player_z: v[12],
        })
    }
}
