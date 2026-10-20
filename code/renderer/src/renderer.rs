use crate::{
    frame::{Draw, Frame, NO_TEXTURE, REFLECTION, SHADOW_MAP},
    gpu::Gpu,
    pass::Pass,
    pipelines::Pipelines,
    resources::Resources,
    targets::Targets,
    water,
};
use std::{borrow::Cow, collections::HashMap};
use wgpu::*;

pub const SHADOW_MAP_SIZE: u32 = 2048;
pub const FRAME_GROUP: &str = "openrs";
pub const CAPTURE_MARKERS: [&str; 2] = ["capture-marker,begin_capture", "capture-marker,end_capture"];

pub struct Renderer {
    pub gpu: Gpu,
    pub capture: bool,
    pipelines: Pipelines,
    resources: Resources,
    targets: Targets,
}

fn begin<'a>(encoder: &'a mut CommandEncoder, color: Option<(&TextureView, Option<Color>)>, depth: &TextureView, clear_depth: bool) -> RenderPass<'a> {
    let color_attachments = color.map(|(view, clear)| RenderPassColorAttachment {
        view,
        depth_slice: None,
        resolve_target: None,
        ops: Operations { load: clear.map_or(LoadOp::Load, LoadOp::Clear), store: StoreOp::Store },
    });
    let slots = [color_attachments];
    encoder.begin_render_pass(&RenderPassDescriptor {
        label: None,
        color_attachments: if slots[0].is_some() { &slots } else { &[] },
        depth_stencil_attachment: Some(RenderPassDepthStencilAttachment {
            view: depth,
            depth_ops: Some(Operations { load: if clear_depth { LoadOp::Clear(1.0) } else { LoadOp::Load }, store: StoreOp::Store }),
            stencil_ops: None,
        }),
        timestamp_writes: None,
        occlusion_query_set: None,
        multiview_mask: None,
    })
}

fn mirror_state(d: &Draw, mirrored: bool) -> u32 {
    if mirrored && d.state & 0x30 != 0 { d.state ^ 64 } else { d.state }
}

impl Renderer {
    pub fn new(gpu: Gpu, format: TextureFormat) -> Self {
        let pipelines = Pipelines::new(&gpu.device, format);
        let resources = Resources::new(&gpu.device, &gpu.queue);
        Renderer { gpu, capture: false, pipelines, resources, targets: Targets::new(format) }
    }

    fn draw(&self, pass: &mut RenderPass, d: &Draw, width: u32, height: u32, mirrored: bool) {
        let uniform = if mirrored { self.resources.mirrored(d.uniform) } else { self.resources.uniform(d.uniform) };
        let (Some(mesh), Some(uniform)) = (self.resources.mesh(d.pass, d.mesh, d.uniform), uniform) else { return };
        let instances = match self.resources.instances(d.instances) {
            Some((buffer, count)) => Some((buffer, *count)),
            None if d.pass == Pass::Instanced => return,
            None => None,
        };
        let scale = if mirrored { 2 } else { 1 };
        let [x, y, w, h] = if d.viewport[2] > 0 && !d.pass.offscreen() { d.viewport.map(|v| v / scale) } else { [0, 0, width, height] };
        let [sx, sy, sw, sh] = if d.scissor[2] > 0 && !d.pass.offscreen() { d.scissor.map(|v| v / scale) } else { [0, 0, width, height] };
        pass.set_viewport(x as f32, y as f32, w.max(1) as f32, h.max(1) as f32, 0., 1.);
        pass.set_scissor_rect(sx, sy, sw.min(width.saturating_sub(sx)), sh.min(height.saturating_sub(sy)));
        pass.set_pipeline(self.pipelines.get(d.pass, mirror_state(d, mirrored)));
        pass.set_bind_group(0, uniform, &[]);
        pass.set_bind_group(1, self.resources.textures(d.pass, &d.textures), &[]);
        pass.set_vertex_buffer(0, mesh.vertices.slice(..));
        pass.set_index_buffer(mesh.indices.slice(..), IndexFormat::Uint32);
        if let Some((buffer, count)) = instances {
            pass.set_vertex_buffer(1, buffer.slice(..));
            pass.draw_indexed(0..mesh.count, 0, 0..count);
        } else {
            pass.draw_indexed(0..mesh.count, 0, 0..1);
        }
    }

    pub fn render(&mut self, frame: &Frame, target: &Texture, width: u32, height: u32) {
        let device = &self.gpu.device;
        self.resources.sync(device, &self.gpu.queue, &self.pipelines, frame);
        let uniforms: HashMap<u32, &[u8]> = frame.uniforms.iter().map(|u| (u.id, u.bytes.as_slice())).collect();
        let block = |d: &Draw| uniforms.get(&d.uniform).copied().unwrap_or(&[]);
        let planar = |d: &Draw| d.pass == Pass::HdWater && d.textures.get(4).is_none_or(|t| *t == NO_TEXTURE) && water::flag(block(d), water::USE_PLANAR);
        let plane = frame
            .draws
            .iter()
            .filter(|d| d.skip.is_none() && planar(d))
            .filter_map(|d| self.resources.water_source(d.mesh).map(|(v, i)| (i.len(), water::plane(v, i), block(d))))
            .filter(|(_, p, _)| !p.is_nan())
            .max_by_key(|(n, ..)| *n)
            .map(|(_, p, u)| (p, [water::SKY_COLOUR, water::SKY_COLOUR + 4, water::SKY_COLOUR + 8].map(|at| f64::from(water::f32_at(u, at)))));
        let live: Vec<Cow<Draw>> = frame
            .draws
            .iter()
            .filter(|d| d.skip.is_none())
            .map(|d| match plane.is_some() && planar(d) {
                true => Cow::Owned(Draw { textures: [&d.textures[..4.min(d.textures.len())], &[NO_TEXTURE; 4][d.textures.len().min(4)..], &[REFLECTION]].concat(), ..d.clone() }),
                false => Cow::Borrowed(d),
            })
            .collect();
        let mirrored: Vec<&Draw> = plane.map_or(vec![], |_| live.iter().map(Cow::as_ref).filter(|d| matches!(d.pass, Pass::Instanced | Pass::Terrain)).collect());
        let fogged = |d: &Draw| d.pass == Pass::Terrain && water::f32_at(block(d), water::TERRAIN_WATER_MODE).to_bits() == 4 && water::flag(block(d), water::TERRAIN_DEPTH_FOG);
        let shadow_size = live.iter().filter(|d| d.pass == Pass::Shadow).map(|d| if d.viewport[2] > 0 { d.viewport[2] } else { SHADOW_MAP_SIZE }).max();
        let shadows = shadow_size.is_some() || live.iter().any(|d| d.textures.contains(&SHADOW_MAP));
        if self.targets.ensure(device, width, height, shadow_size.unwrap_or(1)) {
            self.resources.clear_groups();
        }
        let views = self.targets.views();
        for d in &live {
            self.pipelines.ensure(device, d.pass, d.state);
            self.resources.prepare(device, &self.pipelines, d.pass, &d.textures, &views);
        }
        for d in &mirrored {
            self.pipelines.ensure(device, d.pass, mirror_state(d, true));
        }
        if let Some((plane, _)) = plane {
            self.resources.mirror(device, &self.pipelines, frame, mirrored.iter().map(|d| (d.pass, d.uniform)), plane);
        }
        let [r, g, b, a] = frame.clear.map(f64::from);
        let mut encoder = device.create_command_encoder(&Default::default());
        if self.capture {
            encoder.insert_debug_marker(CAPTURE_MARKERS[0]);
        }
        if cfg!(target_arch = "wasm32") {
            encoder.push_debug_group(FRAME_GROUP);
        }
        if shadows {
            let size = shadow_size.unwrap_or(1);
            let mut pass = begin(&mut encoder, None, self.targets.shadow(), true);
            for d in live.iter().filter(|d| d.pass == Pass::Shadow) {
                self.draw(&mut pass, d, size, size, false);
            }
        }
        if let Some((_, [r, g, b])) = plane {
            let (color, depth) = self.targets.reflection();
            let mut pass = begin(&mut encoder, Some((color, Some(Color { r, g, b, a: 0. }))), depth, true);
            for d in &mirrored {
                self.draw(&mut pass, d, (width / 2).max(1), (height / 2).max(1), true);
            }
        }
        let view = target.create_view(&Default::default());
        let mut pass = begin(&mut encoder, Some((&view, Some(Color { r, g, b, a }))), self.targets.depth(), true);
        let mut water = false;
        for d in live.iter().map(Cow::as_ref).filter(|d| !d.pass.offscreen()) {
            let reads_scene = d.pass == Pass::HdWater || fogged(d);
            if reads_scene && !water {
                drop(pass);
                self.targets.copy_scene(&mut encoder, target);
                pass = begin(&mut encoder, Some((&view, None)), self.targets.depth(), false);
            }
            water = reads_scene;
            self.draw(&mut pass, d, width, height, false);
        }
        drop(pass);
        if cfg!(target_arch = "wasm32") {
            encoder.pop_debug_group();
        }
        if std::mem::take(&mut self.capture) {
            encoder.insert_debug_marker(CAPTURE_MARKERS[1]);
        }
        self.gpu.queue.submit([encoder.finish()]);
    }
}
