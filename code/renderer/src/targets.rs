use crate::pipelines::DEPTH;
use wgpu::*;

pub struct Views<'a> {
    pub shadow: &'a TextureView,
    pub scene_color: &'a TextureView,
    pub scene_depth: &'a TextureView,
    pub reflection: &'a TextureView,
}

struct Target {
    texture: Texture,
    view: TextureView,
}

pub struct Targets {
    format: TextureFormat,
    depth: Option<Target>,
    shadow: Option<Target>,
    copy: Option<(Target, Target)>,
    reflection: Option<(Target, Target)>,
}

fn create(device: &Device, width: u32, height: u32, depth: u32, format: TextureFormat, usage: TextureUsages) -> Target {
    let texture = device.create_texture(&TextureDescriptor {
        label: None,
        size: Extent3d { width, height, depth_or_array_layers: depth },
        mip_level_count: 1,
        sample_count: 1,
        dimension: TextureDimension::D2,
        format,
        usage,
        view_formats: &[],
    });
    let view = texture.create_view(&Default::default());
    Target { texture, view }
}

fn size_of(t: &Option<Target>) -> (u32, u32) {
    t.as_ref().map_or((0, 0), |t| (t.texture.width(), t.texture.height()))
}

impl Targets {
    pub fn new(format: TextureFormat) -> Self {
        Targets { format, depth: None, shadow: None, copy: None, reflection: None }
    }

    pub fn ensure(&mut self, device: &Device, width: u32, height: u32, shadow: u32) -> bool {
        let mut changed = false;
        if size_of(&self.depth) != (width, height) {
            self.depth = Some(create(device, width, height, 1, DEPTH, TextureUsages::RENDER_ATTACHMENT | TextureUsages::COPY_SRC));
            self.copy = None;
            self.reflection = None;
            changed = true;
        }
        if size_of(&self.shadow).0 < shadow {
            self.shadow = Some(create(device, shadow, shadow, 1, DEPTH, TextureUsages::RENDER_ATTACHMENT | TextureUsages::TEXTURE_BINDING));
            changed = true;
        }
        if self.copy.is_none() {
            let usage = TextureUsages::TEXTURE_BINDING | TextureUsages::COPY_DST;
            self.copy = Some((create(device, width, height, 1, self.format, usage), create(device, width, height, 1, DEPTH, usage)));
            changed = true;
        }
        if self.reflection.is_none() {
            let (w, h) = ((width / 2).max(1), (height / 2).max(1));
            let usage = TextureUsages::RENDER_ATTACHMENT | TextureUsages::TEXTURE_BINDING;
            self.reflection = Some((create(device, w, h, 1, self.format, usage), create(device, w, h, 1, DEPTH, TextureUsages::RENDER_ATTACHMENT)));
            changed = true;
        }
        changed
    }

    pub fn reflection(&self) -> (&TextureView, &TextureView) {
        let (color, depth) = self.reflection.as_ref().unwrap();
        (&color.view, &depth.view)
    }

    pub fn depth(&self) -> &TextureView {
        &self.depth.as_ref().unwrap().view
    }

    pub fn shadow(&self) -> &TextureView {
        &self.shadow.as_ref().unwrap().view
    }

    pub fn views(&self) -> Views<'_> {
        let (color, depth) = self.copy.as_ref().unwrap();
        Views { shadow: self.shadow(), scene_color: &color.view, scene_depth: &depth.view, reflection: self.reflection().0 }
    }

    pub fn copy_scene(&self, encoder: &mut CommandEncoder, target: &Texture) {
        let (color, depth) = self.copy.as_ref().unwrap();
        let size = Extent3d { width: target.width(), height: target.height(), depth_or_array_layers: 1 };
        if target.usage().contains(TextureUsages::COPY_SRC) && target.format() == self.format {
            encoder.copy_texture_to_texture(target.as_image_copy(), color.texture.as_image_copy(), size);
        }
        encoder.copy_texture_to_texture(self.depth.as_ref().unwrap().texture.as_image_copy(), depth.texture.as_image_copy(), size);
    }
}
