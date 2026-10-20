use crate::{
    frame::{Frame, NO_TEXTURE, REFLECTION, SHADOW_MAP},
    pass::{Bind, Pass, WaterVertex},
    pipelines::Pipelines,
    targets::Views,
    water,
};
use std::collections::{HashMap, HashSet};
use wgpu::{util::*, *};

pub struct GpuMesh {
    pub vertices: Buffer,
    pub indices: Buffer,
    pub count: u32,
}

struct GpuTexture {
    flat: TextureView,
    array: TextureView,
    cube: Option<TextureView>,
    volume: Option<TextureView>,
}

struct WaterMesh {
    mesh: GpuMesh,
    vp: [u8; 64],
    viewport: [u32; 4],
}

pub struct Resources {
    linear: Sampler,
    nearest: Sampler,
    clamp: Sampler,
    meshes: HashMap<u32, GpuMesh>,
    instances: HashMap<u32, (Buffer, u32)>,
    volume: TextureView,
    cube: TextureView,
    textures: HashMap<u32, GpuTexture>,
    uniforms: HashMap<u32, (Buffer, BindGroup)>,
    groups: HashMap<(Pass, Vec<u32>), BindGroup>,
    water_sources: HashMap<u32, (Vec<WaterVertex>, Vec<u32>)>,
    water: HashMap<(u32, u32), WaterMesh>,
    mirrored: HashMap<u32, (u32, BindGroup)>,
}

fn texture(device: &Device, queue: &Queue, dimension: TextureDimension, width: u32, height: u32, depth: u32, rgba: &[u8], label: &str) -> Texture {
    let mips = if dimension == TextureDimension::D2 { mip_chain(width, height, depth, rgba) } else { rgba.to_vec() };
    device.create_texture_with_data(
        queue,
        &TextureDescriptor {
            label: Some(label),
            size: Extent3d { width, height, depth_or_array_layers: depth },
            mip_level_count: if dimension == TextureDimension::D2 { width.max(height).ilog2() + 1 } else { 1 },
            sample_count: 1,
            dimension,
            format: TextureFormat::Rgba8Unorm,
            usage: TextureUsages::TEXTURE_BINDING,
            view_formats: &[],
        },
        TextureDataOrder::MipMajor,
        &mips,
    )
}

fn mip_chain(width: u32, height: u32, layers: u32, rgba: &[u8]) -> Vec<u8> {
    let mut out = rgba.to_vec();
    let (mut w, mut h, mut level) = (width as usize, height as usize, rgba.to_vec());
    while w > 1 || h > 1 {
        let (nw, nh) = ((w / 2).max(1), (h / 2).max(1));
        let (ls, ns) = (w * h * 4, nw * nh * 4);
        let mut next = vec![0u8; ns * layers as usize];
        for l in 0..layers as usize {
            let src = &level[l * ls..(l + 1) * ls];
            let dst = &mut next[l * ns..(l + 1) * ns];
            for y in 0..nh {
                let (y0, y1) = ((2 * y).min(h - 1), (2 * y + 1).min(h - 1));
                for x in 0..nw {
                    let (x0, x1) = ((2 * x).min(w - 1), (2 * x + 1).min(w - 1));
                    for c in 0..4 {
                        let sum = [(x0, y0), (x1, y0), (x0, y1), (x1, y1)].iter().map(|&(sx, sy)| src[(sy * w + sx) * 4 + c] as u32).sum::<u32>();
                        dst[(y * nw + x) * 4 + c] = ((sum + 2) / 4) as u8;
                    }
                }
            }
        }
        out.extend_from_slice(&next);
        (w, h, level) = (nw, nh, next);
    }
    out
}

fn upload(device: &Device, queue: &Queue, width: u32, height: u32, layers: u32, rgba: &[u8], cube: bool, volume: bool) -> GpuTexture {
    let t = texture(device, queue, TextureDimension::D2, width, height, layers, rgba, "texture");
    let view = |dimension, count| t.create_view(&TextureViewDescriptor { dimension: Some(dimension), array_layer_count: Some(count), ..Default::default() });
    GpuTexture {
        flat: view(TextureViewDimension::D2, 1),
        array: view(TextureViewDimension::D2Array, layers),
        cube: (cube && layers == 6 && width == height).then(|| view(TextureViewDimension::Cube, 6)),
        volume: volume.then(|| texture(device, queue, TextureDimension::D3, width, height, layers, rgba, "volume").create_view(&Default::default())),
    }
}

fn mesh(device: &Device, vertices: &[u8], indices: &[u32]) -> GpuMesh {
    let buffer = |contents: &[u8], usage| device.create_buffer_init(&BufferInitDescriptor { label: None, contents, usage });
    GpuMesh { vertices: buffer(vertices, BufferUsages::VERTEX), indices: buffer(bytemuck::cast_slice(indices), BufferUsages::INDEX), count: indices.len() as u32 }
}

impl Resources {
    pub fn new(device: &Device, queue: &Queue) -> Self {
        let sampler = |filter, address_mode| {
            device.create_sampler(&SamplerDescriptor { address_mode_u: address_mode, address_mode_v: address_mode, mag_filter: filter, min_filter: filter, mipmap_filter: if filter == FilterMode::Linear { MipmapFilterMode::Linear } else { MipmapFilterMode::Nearest }, ..Default::default() })
        };
        let white = upload(device, queue, 1, 1, 1, &[255; 4], false, true);
        let cube = texture(device, queue, TextureDimension::D2, 1, 1, 6, &[255; 24], "cube")
            .create_view(&TextureViewDescriptor { dimension: Some(TextureViewDimension::Cube), ..Default::default() });
        let mut r = Resources {
            linear: sampler(FilterMode::Linear, AddressMode::Repeat),
            nearest: sampler(FilterMode::Nearest, AddressMode::ClampToEdge),
            clamp: device.create_sampler(&SamplerDescriptor {
                address_mode_u: AddressMode::ClampToEdge,
                address_mode_v: AddressMode::ClampToEdge,
                mag_filter: FilterMode::Linear,
                min_filter: FilterMode::Linear,
                lod_max_clamp: 0.,
                ..Default::default()
            }),
            meshes: HashMap::new(),
            instances: HashMap::new(),
            volume: white.volume.clone().unwrap(),
            cube,
            textures: HashMap::new(),
            uniforms: HashMap::new(),
            groups: HashMap::new(),
            water_sources: HashMap::new(),
            water: HashMap::new(),
            mirrored: HashMap::new(),
        };
        r.textures.insert(NO_TEXTURE, white);
        r
    }

    pub fn clear_groups(&mut self) {
        self.groups.clear();
    }

    pub fn sync(&mut self, device: &Device, queue: &Queue, pipelines: &Pipelines, frame: &Frame) {
        let (mut cubes, mut volumes) = (HashSet::new(), HashSet::new());
        for d in frame.draws.iter().filter(|d| d.skip.is_none()) {
            for b in d.pass.bindings() {
                if let Some(id) = b.source().and_then(|k| d.textures.get(k)) {
                    match b {
                        Bind::Cube(_) => cubes.insert(*id),
                        Bind::Volume(_) => volumes.insert(*id),
                        _ => false,
                    };
                }
            }
        }
        for m in &frame.meshes {
            self.meshes.insert(m.id, mesh(device, &m.vertices, &m.indices));
            if m.pass == Pass::HdWater {
                self.water_sources.insert(m.id, (bytemuck::pod_collect_to_vec(&m.vertices), m.indices.clone()));
                self.water.retain(|k, _| k.0 != m.id);
            }
        }
        for i in &frame.instances {
            let buffer = device.create_buffer_init(&BufferInitDescriptor { label: None, contents: &i.bytes, usage: BufferUsages::VERTEX });
            self.instances.insert(i.id, (buffer, i.count));
        }
        for t in &frame.textures {
            self.textures.insert(t.id, upload(device, queue, t.width, t.height, t.layers, &t.rgba, cubes.contains(&t.id), volumes.contains(&t.id)));
            self.groups.clear();
        }
        self.mirrored.retain(|id, _| frame.uniforms.iter().all(|u| u.id != *id));
        for u in &frame.uniforms {
            match self.uniforms.get(&u.id) {
                Some((buffer, _)) if buffer.size() == u.bytes.len() as u64 => queue.write_buffer(buffer, 0, &u.bytes),
                _ => {
                    let buffer = device.create_buffer_init(&BufferInitDescriptor {
                        label: None,
                        contents: &u.bytes,
                        usage: BufferUsages::UNIFORM | BufferUsages::COPY_DST,
                    });
                    let group = device.create_bind_group(&BindGroupDescriptor {
                        label: None,
                        layout: &pipelines.uniform,
                        entries: &[BindGroupEntry { binding: 0, resource: buffer.as_entire_binding() }],
                    });
                    self.uniforms.insert(u.id, (buffer, group));
                }
            }
        }
        for d in frame.draws.iter().filter(|d| d.skip.is_none() && d.pass == Pass::HdWater) {
            let Some(u) = frame.uniforms.iter().find(|u| u.id == d.uniform) else { continue };
            let key = (d.mesh, d.uniform);
            let Some((vertices, indices)) = self.water_sources.get(&d.mesh) else { continue };
            let Ok(vp) = <[u8; 64]>::try_from(&u.bytes[water::VIEW_PROJECTION..water::VIEW_PROJECTION + 64]) else { continue };
            let viewport = if d.viewport[2] > 0 { d.viewport } else { [0, 0, frame.width, frame.height] };
            if !water::waving(&u.bytes) {
                self.water.remove(&key);
                continue;
            }
            if self.water.get(&key).is_some_and(|w| w.vp == vp && w.viewport == viewport) {
                continue;
            }
            let (v, i) = water::tessellate(vertices, indices, &bytemuck::pod_read_unaligned(&vp), viewport);
            self.water.insert(key, WaterMesh { mesh: mesh(device, bytemuck::cast_slice(&v), &i), vp, viewport });
        }
    }

    pub fn prepare(&mut self, device: &Device, pipelines: &Pipelines, pass: Pass, ids: &[u32], views: &Views) {
        let key = (pass, ids.to_vec());
        if self.groups.contains_key(&key) {
            return;
        }
        let white = &self.textures[&NO_TEXTURE];
        let entries: Vec<_> = pass
            .bindings()
            .iter()
            .enumerate()
            .flat_map(|(i, b)| {
                let id = b.source().and_then(|k| ids.get(k).copied()).unwrap_or(NO_TEXTURE);
                let t = self.textures.get(&id).unwrap_or(white);
                let view = match b {
                    Bind::Flat(_) | Bind::Clamp(_) => &t.flat,
                    Bind::Layered(_) => &t.array,
                    Bind::Volume(_) => t.volume.as_ref().unwrap_or(&self.volume),
                    Bind::Cube(_) => t.cube.as_ref().unwrap_or(&self.cube),
                    Bind::Depth(_) if id == SHADOW_MAP => views.shadow,
                    Bind::Depth(_) => &t.flat,
                    Bind::Reflection(_) if id == REFLECTION => views.reflection,
                    Bind::Reflection(_) => &t.flat,
                    Bind::SceneColor => views.scene_color,
                    Bind::SceneDepth => views.scene_depth,
                };
                let sampler = match b {
                    Bind::Clamp(_) | Bind::SceneColor | Bind::Reflection(_) => &self.clamp,
                    _ if b.filterable() => &self.linear,
                    _ => &self.nearest,
                };
                [
                    BindGroupEntry { binding: 2 * i as u32, resource: BindingResource::TextureView(view) },
                    BindGroupEntry { binding: 2 * i as u32 + 1, resource: BindingResource::Sampler(sampler) },
                ]
            })
            .collect();
        let group = device.create_bind_group(&BindGroupDescriptor { label: None, layout: pipelines.texture_layout(pass), entries: &entries });
        self.groups.insert(key, group);
    }

    pub fn mesh(&self, pass: Pass, id: u32, uniform: u32) -> Option<&GpuMesh> {
        if pass == Pass::HdWater {
            if let Some(w) = self.water.get(&(id, uniform)) {
                return Some(&w.mesh);
            }
        }
        self.meshes.get(&id)
    }

    pub fn instances(&self, id: u32) -> Option<&(Buffer, u32)> {
        self.instances.get(&id)
    }

    pub fn uniform(&self, id: u32) -> Option<&BindGroup> {
        self.uniforms.get(&id).map(|(_, g)| g)
    }

    pub fn mirror(&mut self, device: &Device, pipelines: &Pipelines, frame: &Frame, ids: impl Iterator<Item = (Pass, u32)>, plane: f32) {
        self.mirrored.retain(|_, (p, _)| *p == plane.to_bits());
        for (pass, id) in ids {
            if self.mirrored.contains_key(&id) {
                continue;
            }
            let Some(u) = frame.uniforms.iter().find(|u| u.id == id) else { continue };
            let mut bytes = u.bytes.clone();
            water::mirror(&mut bytes, plane, pass == Pass::Terrain);
            let buffer = device.create_buffer_init(&BufferInitDescriptor { label: None, contents: &bytes, usage: BufferUsages::UNIFORM });
            let group = device.create_bind_group(&BindGroupDescriptor {
                label: None,
                layout: &pipelines.uniform,
                entries: &[BindGroupEntry { binding: 0, resource: buffer.as_entire_binding() }],
            });
            self.mirrored.insert(id, (plane.to_bits(), group));
        }
    }

    pub fn mirrored(&self, id: u32) -> Option<&BindGroup> {
        self.mirrored.get(&id).map(|(_, g)| g)
    }

    pub fn water_source(&self, id: u32) -> Option<&(Vec<WaterVertex>, Vec<u32>)> {
        self.water_sources.get(&id)
    }

    pub fn textures(&self, pass: Pass, ids: &[u32]) -> &BindGroup {
        &self.groups[&(pass, ids.to_vec())]
    }
}
