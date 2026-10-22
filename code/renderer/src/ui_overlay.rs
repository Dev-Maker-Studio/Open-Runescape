use crate::frame::{Draw, Frame, Mesh, Texture, TextureFormat, Uniform};
use crate::math;
use crate::pass::{Pass, SpriteVertex};
use crate::uniforms::SpriteUniform;
use bytemuck::{bytes_of, cast_slice};

pub const UI_MESH: u32 = 0x7000_0001;
pub const UI_UNIFORM: u32 = 0x7000_0001;
pub const UI_TEXTURE: u32 = 0x7000_0001;
const SAMPLER: u32 = 9;

#[derive(Clone, Debug, PartialEq)]
pub struct Overlay {
    pub width: u32,
    pub height: u32,
    pub rgba: Vec<u8>,
    pub game: Option<[u32; 4]>,
}

impl Overlay {
    pub fn new(width: u32, height: u32, rgba: Vec<u8>, game: Option<[u32; 4]>) -> Result<Self, String> {
        if width == 0 || height == 0 || width > 4096 || height > 4096 {
            return Err("interface overlay size out of range".into());
        }
        if rgba.len() != width as usize * height as usize * 4 {
            return Err(format!(
                "interface overlay has {} bytes, expected {}",
                rgba.len(),
                width as usize * height as usize * 4
            ));
        }
        if let Some([x, y, w, h]) = game
            && (w == 0 || h == 0 || x.saturating_add(w) > width || y.saturating_add(h) > height)
        {
            return Err("interface game area outside the overlay".into());
        }
        Ok(Self { width, height, rgba, game })
    }
}

fn quad(width: u32, height: u32) -> Mesh {
    let (w, h) = (width as f32, height as f32);
    let vertices: Vec<SpriteVertex> = [(0., 0.), (1., 0.), (1., 1.), (0., 1.)]
        .iter()
        .map(|&(u, v)| SpriteVertex {
            p: [u * w, v * h],
            uv: [u, v],
            c: [255; 4],
            tex: 0.,
            clip: [0., 0., 4096., 4096.],
        })
        .collect();
    Mesh {
        id: UI_MESH,
        pass: Pass::Sprite,
        vertices: cast_slice(&vertices).to_vec(),
        indices: vec![0, 1, 2, 0, 2, 3],
    }
}

pub fn compose(frame: &mut Frame, overlay: &Overlay) {
    if let Some(game) = overlay.game {
        for draw in &mut frame.draws {
            if !draw.pass.offscreen() && draw.pass != Pass::Sprite {
                draw.viewport = game;
                draw.scissor = game;
            }
        }
    }
    frame.width = overlay.width;
    frame.height = overlay.height;
    frame.meshes.push(quad(overlay.width, overlay.height));
    frame.textures.push(Texture {
        id: UI_TEXTURE,
        width: overlay.width,
        height: overlay.height,
        layers: 1,
        rgba: overlay.rgba.clone(),
        format: TextureFormat::Rgba8,
        sampler: SAMPLER,
    });
    frame.uniforms.push(Uniform {
        id: UI_UNIFORM,
        bytes: bytes_of(&SpriteUniform {
            projection: math::ortho_top_left(overlay.width as f32, overlay.height as f32),
        })
        .to_vec(),
    });
    frame.draws.push(Draw::new(Pass::Sprite, UI_MESH, UI_UNIFORM, vec![UI_TEXTURE]));
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::frame::NO_TEXTURE;

    fn overlay(game: Option<[u32; 4]>) -> Overlay {
        Overlay::new(8, 6, vec![0; 8 * 6 * 4], game).unwrap()
    }

    #[test]
    fn rejects_wrong_sizes_and_game_areas_outside_the_canvas() {
        assert!(Overlay::new(0, 6, vec![], None).is_err());
        assert!(Overlay::new(2, 2, vec![0; 3], None).is_err());
        assert!(Overlay::new(8, 6, vec![0; 192], Some([6, 0, 4, 2])).is_err());
        assert!(Overlay::new(8, 6, vec![0; 192], Some([0, 0, 8, 6])).is_ok());
    }

    #[test]
    fn composes_a_full_canvas_sprite_quad_last_and_confines_the_world() {
        let mut f = Frame { width: 4, height: 3, ..Default::default() };
        f.draws.push(Draw::new(Pass::Terrain, 1, 1000, vec![NO_TEXTURE]));
        f.draws.push(Draw::new(Pass::Shadow, 2, 1001, vec![NO_TEXTURE]));
        compose(&mut f, &overlay(Some([1, 1, 4, 3])));
        assert_eq!((f.width, f.height), (8, 6));
        assert_eq!(f.draws[0].viewport, [1, 1, 4, 3]);
        assert_eq!(f.draws[0].scissor, [1, 1, 4, 3]);
        assert_eq!(f.draws[1].viewport, [0; 4]);
        let last = f.draws.last().unwrap();
        assert_eq!(last.pass, Pass::Sprite);
        assert_eq!(last.viewport, [0; 4]);
        assert_eq!(f.meshes.len(), 1);
        assert_eq!(f.meshes[0].indices.len(), 6);
        assert_eq!(f.textures[0].rgba.len(), 192);
    }

    #[test]
    fn without_a_game_area_the_world_keeps_the_whole_target() {
        let mut f = Frame { width: 8, height: 6, ..Default::default() };
        f.draws.push(Draw::new(Pass::Terrain, 1, 1000, vec![NO_TEXTURE]));
        compose(&mut f, &overlay(None));
        assert_eq!(f.draws[0].viewport, [0; 4]);
    }
}
