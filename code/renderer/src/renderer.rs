use crate::{
    bloom::Bloom,
    frame::{Draw, Frame, NO_TEXTURE, REFLECTION, SHADOW_MAP},
    gpu::Gpu,
    pass::Pass,
    pipelines::Pipelines,
    resources::{Resources, Slot},
    stats::{Probe, Stats, finished, now},
    targets::{Targets, Views},
    water,
};
use std::{borrow::Cow, collections::HashMap};
use wgpu::*;

pub use crate::pass::SHADOW_MAP_SIZE;
pub const FRAME_GROUP: &str = "openrs";
pub const CAPTURE_MARKERS: [&str; 2] =
    ["capture-marker,begin_capture", "capture-marker,end_capture"];

pub struct Renderer {
    pub gpu: Gpu,
    pub capture: bool,
    pub stats: Stats,
    probe: Option<Probe>,
    submitted: f64,
    pipelines: Pipelines,
    resources: Resources,
    targets: Targets,
    format: TextureFormat,
    bloom: Option<Bloom>,
    scene: Option<Scene>,
}

struct Scene {
    texture: Texture,
    view: TextureView,
}

fn scene_texture(device: &Device, format: TextureFormat, width: u32, height: u32) -> Scene {
    let texture = device.create_texture(&TextureDescriptor {
        label: Some("bloom-scene"),
        size: Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
        mip_level_count: 1,
        sample_count: 1,
        dimension: TextureDimension::D2,
        format,
        usage: TextureUsages::RENDER_ATTACHMENT
            | TextureUsages::TEXTURE_BINDING
            | TextureUsages::COPY_SRC,
        view_formats: &[],
    });
    let view = texture.create_view(&Default::default());
    Scene { texture, view }
}

struct Item<'a> {
    draw: &'a Draw,
    pipeline: u32,
    slot: Slot,
}

struct Cursor {
    pipeline: u32,
    uniform: u64,
    textures: u32,
    arena: u32,
    instances: u32,
    viewport: [u32; 4],
    scissor: [u32; 4],
    calls: u32,
}

impl Cursor {
    fn new() -> Self {
        Cursor {
            pipeline: u32::MAX,
            uniform: u64::MAX,
            textures: u32::MAX,
            arena: u32::MAX,
            instances: u32::MAX,
            viewport: [0; 4],
            scissor: [0; 4],
            calls: 0,
        }
    }
}

fn begin<'a>(
    encoder: &'a mut CommandEncoder,
    color: Option<(&TextureView, Option<Color>)>,
    depth: &TextureView,
    clear_depth: bool,
    probe: &'a Option<Probe>,
) -> RenderPass<'a> {
    let color_attachments = color.map(|(view, clear)| RenderPassColorAttachment {
        view,
        depth_slice: None,
        resolve_target: None,
        ops: Operations {
            load: clear.map_or(LoadOp::Load, LoadOp::Clear),
            store: StoreOp::Store,
        },
    });
    let slots = [color_attachments];
    encoder.begin_render_pass(&RenderPassDescriptor {
        label: None,
        color_attachments: if slots[0].is_some() { &slots } else { &[] },
        depth_stencil_attachment: Some(RenderPassDepthStencilAttachment {
            view: depth,
            depth_ops: Some(Operations {
                load: if clear_depth {
                    LoadOp::Clear(1.0)
                } else {
                    LoadOp::Load
                },
                store: StoreOp::Store,
            }),
            stencil_ops: None,
        }),
        timestamp_writes: probe.as_ref().and_then(Probe::writes),
        occlusion_query_set: None,
        multiview_mask: None,
    })
}

fn mirror_state(d: &Draw, mirrored: bool) -> u32 {
    if mirrored && d.state & 0x30 != 0 {
        d.state ^ 64
    } else {
        d.state
    }
}

fn resolve<'a>(
    pipelines: &mut Pipelines,
    resources: &mut Resources,
    device: &Device,
    views: &Views,
    d: &'a Draw,
    mirror: bool,
) -> Option<Item<'a>> {
    let pipeline = pipelines.ensure(device, d.pass, mirror_state(d, mirror));
    resources
        .slot(device, pipelines, d, mirror, views)
        .map(|slot| Item {
            draw: d,
            pipeline,
            slot,
        })
}

impl Renderer {
    pub fn new(gpu: Gpu, format: TextureFormat) -> Self {
        let pipelines = Pipelines::new(&gpu.device, format);
        let resources = Resources::new(&gpu.device, &gpu.queue);
        Renderer {
            gpu,
            capture: false,
            stats: Stats::default(),
            probe: None,
            submitted: 0.,
            pipelines,
            resources,
            targets: Targets::new(format),
            format,
            bloom: None,
            scene: None,
        }
    }

    pub fn reset_stream_resources(&mut self) {
        self.resources = Resources::new(&self.gpu.device, &self.gpu.queue);
    }

    pub fn probe(&mut self, on: bool) -> bool {
        self.probe = on
            .then(|| Probe::new(&self.gpu.device, &self.gpu.queue))
            .flatten();
        self.probe.is_some()
    }

    pub async fn finish(&mut self) {
        finished(&self.gpu.device, &self.gpu.queue).await;
        let wall = now() - self.submitted;
        self.stats.gpu = match &mut self.probe {
            Some(p) => p.gpu_ms(&self.gpu.device).await,
            None => wall,
        };
    }

    fn draw(
        &self,
        pass: &mut RenderPass,
        cur: &mut Cursor,
        it: &Item,
        width: u32,
        height: u32,
        mirrored: bool,
    ) {
        let (d, slot) = (it.draw, &it.slot);
        let scale = if mirrored { 2 } else { 1 };
        let viewport = if d.viewport[2] > 0 && !d.pass.offscreen() {
            d.viewport.map(|v| v / scale)
        } else {
            [0, 0, width, height]
        };
        let scissor = if d.scissor[2] > 0 && !d.pass.offscreen() {
            d.scissor.map(|v| v / scale)
        } else {
            [0, 0, width, height]
        };
        if cur.viewport != viewport {
            let [x, y, w, h] = viewport;
            pass.set_viewport(x as f32, y as f32, w.max(1) as f32, h.max(1) as f32, 0., 1.);
            cur.viewport = viewport;
            cur.calls += 1;
        }
        if cur.scissor != scissor {
            let [sx, sy, sw, sh] = scissor;
            pass.set_scissor_rect(
                sx,
                sy,
                sw.min(width.saturating_sub(sx)),
                sh.min(height.saturating_sub(sy)),
            );
            cur.scissor = scissor;
            cur.calls += 1;
        }
        if cur.pipeline != it.pipeline {
            pass.set_pipeline(&self.pipelines.list[it.pipeline as usize]);
            cur.pipeline = it.pipeline;
            cur.calls += 1;
        }
        if cur.uniform != slot.uniform_key {
            pass.set_bind_group(0, &slot.uniform, &[]);
            cur.uniform = slot.uniform_key;
            cur.calls += 1;
        }
        if cur.textures != slot.textures {
            pass.set_bind_group(1, &self.resources.groups[slot.textures as usize], &[]);
            cur.textures = slot.textures;
            cur.calls += 1;
        }
        let mesh = slot.mesh;
        if cur.arena != mesh.arena {
            let a = self.resources.arenas[mesh.arena as usize].as_ref().unwrap();
            pass.set_vertex_buffer(0, a.vertices.slice(..));
            pass.set_index_buffer(a.indices.slice(..), IndexFormat::Uint32);
            cur.arena = mesh.arena;
            cur.calls += 2;
        }
        let indices = mesh.first..mesh.first + mesh.count;
        cur.calls += 1;
        match slot.instances {
            Some(i) => {
                if cur.instances != i.buffer {
                    pass.set_vertex_buffer(
                        1,
                        self.resources.instance_buffers[i.buffer as usize]
                            .as_ref()
                            .unwrap()
                            .slice(..),
                    );
                    cur.instances = i.buffer;
                    cur.calls += 1;
                }
                pass.draw_indexed(indices, mesh.base, i.first..i.first + i.count);
            }
            None => pass.draw_indexed(indices, mesh.base, 0..1),
        }
    }

    pub fn render(&mut self, frame: &Frame, target: &Texture, width: u32, height: u32) {
        let mut stats = Stats::default();
        let device = &self.gpu.device;
        self.resources
            .sync(device, &self.gpu.queue, &self.pipelines, frame, &mut stats);
        let t = now();
        let uniforms: HashMap<u32, &[u8]> = frame
            .uniforms
            .iter()
            .map(|u| (u.id, u.bytes.as_slice()))
            .collect();
        let block = |d: &Draw| uniforms.get(&d.uniform).copied().unwrap_or(&[]);
        let planar = |d: &Draw| {
            d.pass == Pass::HdWater
                && d.textures.get(4).is_none_or(|t| *t == NO_TEXTURE)
                && water::flag(block(d), water::USE_PLANAR)
        };
        let plane = frame
            .draws
            .iter()
            .filter(|d| d.skip.is_none() && planar(d))
            .filter_map(|d| {
                self.resources
                    .water_source(d.mesh)
                    .map(|(v, i)| (i.len(), water::plane(v, i), block(d)))
            })
            .filter(|(_, p, _)| !p.is_nan())
            .max_by_key(|(n, ..)| *n)
            .map(|(_, p, u)| {
                (
                    p,
                    [
                        water::SKY_COLOUR,
                        water::SKY_COLOUR + 4,
                        water::SKY_COLOUR + 8,
                    ]
                    .map(|at| f64::from(water::f32_at(u, at))),
                )
            });
        let live: Vec<Cow<Draw>> = frame
            .draws
            .iter()
            .filter(|d| d.skip.is_none())
            .map(|d| match plane.is_some() && planar(d) {
                true => Cow::Owned(Draw {
                    textures: [
                        &d.textures[..4.min(d.textures.len())],
                        &[NO_TEXTURE; 4][d.textures.len().min(4)..],
                        &[REFLECTION],
                    ]
                    .concat(),
                    ..d.clone()
                }),
                false => Cow::Borrowed(d),
            })
            .collect();
        let mirrored: Vec<&Draw> = plane.map_or(vec![], |_| {
            live.iter()
                .map(Cow::as_ref)
                .filter(|d| matches!(d.pass, Pass::Instanced | Pass::Terrain))
                .collect()
        });
        let shadow_size = live
            .iter()
            .filter(|d| d.pass == Pass::Shadow)
            .map(|d| {
                if d.viewport[2] > 0 {
                    d.viewport[2]
                } else {
                    SHADOW_MAP_SIZE
                }
            })
            .max();
        let shadows =
            shadow_size.is_some() || live.iter().any(|d| d.textures.contains(&SHADOW_MAP));
        if self
            .targets
            .ensure(device, width, height, shadow_size.unwrap_or(1))
        {
            self.resources.clear_groups();
        }
        if let Some((plane, _)) = plane {
            self.resources.mirror(
                device,
                &self.pipelines,
                frame,
                mirrored.iter().map(|d| (d.pass, d.uniform)),
                plane,
            );
        }
        let views = self.targets.views();
        let (pipelines, resources) = (&mut self.pipelines, &mut self.resources);
        let items: Vec<Item> = live
            .iter()
            .filter_map(|d| resolve(pipelines, resources, device, &views, d, false))
            .collect();
        let reflected: Vec<Item> = mirrored
            .iter()
            .filter_map(|d| resolve(pipelines, resources, device, &views, d, true))
            .collect();
        stats.groups = now() - t;
        let t = now();
        let [r, g, b, a] = frame.clear.map(f64::from);
        let mut encoder = device.create_command_encoder(&Default::default());
        if self.capture {
            encoder.insert_debug_marker(CAPTURE_MARKERS[0]);
        }
        if cfg!(target_arch = "wasm32") {
            encoder.push_debug_group(FRAME_GROUP);
        }
        let mut cur = Cursor::new();
        if shadows {
            let size = shadow_size.unwrap_or(1);
            let mut pass = begin(&mut encoder, None, self.targets.shadow(), true, &self.probe);
            for it in items.iter().filter(|it| it.draw.pass == Pass::Shadow) {
                self.draw(&mut pass, &mut cur, it, size, size, false);
            }
        }
        if let Some((_, [r, g, b])) = plane {
            let (color, depth) = self.targets.reflection();
            let mut pass = begin(
                &mut encoder,
                Some((color, Some(Color { r, g, b, a: 0. }))),
                depth,
                true,
                &self.probe,
            );
            cur = Cursor {
                calls: cur.calls,
                ..Cursor::new()
            };
            for it in &reflected {
                self.draw(
                    &mut pass,
                    &mut cur,
                    it,
                    (width / 2).max(1),
                    (height / 2).max(1),
                    true,
                );
            }
        }
        let bloom = frame.bloom.filter(|p| p.valid());
        if bloom.is_some()
            && self
                .scene
                .as_ref()
                .is_none_or(|s| (s.texture.width(), s.texture.height()) != (width, height))
        {
            self.scene = Some(scene_texture(device, self.format, width, height));
        }
        let color = match (&bloom, &self.scene) {
            (Some(_), Some(s)) => &s.texture,
            _ => target,
        };
        let view = color.create_view(&Default::default());
        let mut pass = begin(
            &mut encoder,
            Some((&view, Some(Color { r, g, b, a }))),
            self.targets.depth(),
            true,
            &self.probe,
        );
        cur = Cursor {
            calls: cur.calls,
            ..Cursor::new()
        };
        let mut copies = water::SceneCopies::default();
        for it in items.iter().filter(|it| !it.draw.pass.offscreen()) {
            if copies.before(it.draw.pass, block(it.draw)) {
                drop(pass);
                self.targets.copy_scene(&mut encoder, color);
                pass = begin(
                    &mut encoder,
                    Some((&view, None)),
                    self.targets.depth(),
                    false,
                    &self.probe,
                );
                cur = Cursor {
                    calls: cur.calls,
                    ..Cursor::new()
                };
            }
            self.draw(&mut pass, &mut cur, it, width, height, false);
        }
        drop(pass);
        if let (Some(params), Some(scene)) = (bloom, &self.scene) {
            let format = self.format;
            self.bloom
                .get_or_insert_with(|| Bloom::new(device, format))
                .encode(
                    device,
                    &mut encoder,
                    &scene.view,
                    (width, height),
                    &target.create_view(&Default::default()),
                    params,
                );
        }
        if cfg!(target_arch = "wasm32") {
            encoder.pop_debug_group();
        }
        if std::mem::take(&mut self.capture) {
            encoder.insert_debug_marker(CAPTURE_MARKERS[1]);
        }
        if let Some(p) = &self.probe {
            p.resolve(&mut encoder);
        }
        stats.encode = now() - t;
        let t = now();
        self.gpu.queue.submit([encoder.finish()]);
        self.submitted = now();
        stats.submit = self.submitted - t;
        stats.draws = (items.len() + reflected.len()) as u32;
        stats.calls = cur.calls;
        stats.timestamps = self.probe.is_some();
        self.stats = stats;
    }
}

#[cfg(all(test, not(target_arch = "wasm32")))]
mod lifecycle_tests {
    use super::*;
    use crate::{capture, gpu, scene};

    #[test]
    fn epoch_replacement_releases_gpu_objects_and_keeps_pipelines() {
        let instance = gpu::instance();
        let Some(gpu) = pollster::block_on(Gpu::try_new(&instance, None)) else {
            println!("skip: no gpu adapter");
            return;
        };
        let mut renderer = Renderer::new(gpu, capture::FORMAT);
        let mut baseline = None;
        for epoch in 0..64 {
            for n in 0..4 {
                let mut f = scene::build("model", 0, 64, 64).unwrap();
                let old = f.meshes[0].id;
                f.meshes[0].id += epoch * 4 + n;
                let id = f.meshes[0].id;
                for d in &mut f.draws {
                    if d.mesh == old {
                        d.mesh = id;
                    }
                }
                f.meshes[0].vertices[0] = (epoch * 4 + n) as u8;
                f.textures[0].rgba[0] = (epoch * 4 + n) as u8;
                let _ = capture::render_rgba(&mut renderer, &f);
            }
            assert!(!renderer.resources.arenas.is_empty());
            let pipelines = renderer.pipelines.list.len();
            renderer.reset_stream_resources();
            assert!(renderer.resources.arenas.is_empty());
            assert!(renderer.resources.groups.is_empty());
            assert!(renderer.resources.instance_buffers.is_empty());
            assert_eq!(renderer.pipelines.list.len(), pipelines);
            renderer.gpu.queue.submit([]);
            renderer
                .gpu
                .device
                .poll(PollType::wait_indefinitely())
                .unwrap();
            let report = instance.generate_report().unwrap().hub;
            let live = [
                report.buffers.num_allocated,
                report.textures.num_allocated,
                report.texture_views.num_allocated,
                report.bind_groups.num_allocated,
            ];
            assert_eq!(
                *baseline.get_or_insert(live),
                live,
                "GPU objects retained after epoch {epoch}"
            );
        }
    }
}
