use crate::frame::TextureFormat as RecordedTextureFormat;
use crate::{
    frame::{Draw, Frame, NO_TEXTURE, REFLECTION, SHADOW_MAP, valid_sampler},
    pass::{Bind, Pass, WaterVertex},
    pipelines::Pipelines,
    stats::{Stats, now},
    targets::Views,
    water,
};
use std::collections::{HashMap, HashSet};
use wgpu::{util::*, *};

pub const EVICT_EVERY: u32 = 256;
const MIRRORED: u64 = 1 << 32;

#[derive(Clone, Copy)]
pub struct GpuMesh {
    pub arena: u32,
    pub first: u32,
    pub count: u32,
    pub base: i32,
}

#[derive(Clone, Copy)]
pub struct Instances {
    pub buffer: u32,
    pub first: u32,
    pub count: u32,
}

pub struct Arena {
    pub vertices: Buffer,
    pub indices: Buffer,
}

#[derive(Clone)]
struct GpuTexture {
    flat: TextureView,
    sampler: Option<Sampler>,
    array: TextureView,
    cube: Option<TextureView>,
    volume: Option<TextureView>,
}

pub struct Slot {
    pub uniform: BindGroup,
    pub uniform_key: u64,
    pub textures: u32,
    pub mesh: GpuMesh,
    pub instances: Option<Instances>,
}

struct WaterMesh {
    mesh: GpuMesh,
    vp: [u8; 64],
    viewport: [u32; 4],
    tess_pixels: f32,
}

struct Pool<T> {
    ids: HashMap<u32, u32>,
    hashes: HashMap<u64, u32>,
    items: Vec<Option<(u64, T)>>,
    free: Vec<u32>,
}

impl<T> Pool<T> {
    fn new() -> Self {
        Pool {
            ids: HashMap::new(),
            hashes: HashMap::new(),
            items: vec![],
            free: vec![],
        }
    }

    fn index(&self, id: u32) -> Option<u32> {
        self.ids.get(&id).copied()
    }

    fn item(&self, index: u32) -> Option<&T> {
        self.items.get(index as usize)?.as_ref().map(|(_, t)| t)
    }

    fn get(&self, id: u32) -> Option<&T> {
        self.item(self.index(id)?)
    }

    fn hash_of(&self, id: u32) -> Option<u64> {
        self.items
            .get(self.index(id)? as usize)?
            .as_ref()
            .map(|(h, _)| *h)
    }

    fn by_hash(&self, hash: u64) -> Option<&T> {
        self.item(*self.hashes.get(&hash)?)
    }

    fn bind(&mut self, id: u32, hash: u64) -> bool {
        match self.hashes.get(&hash) {
            Some(&i) => {
                self.ids.insert(id, i);
                true
            }
            None => false,
        }
    }

    fn insert(&mut self, id: u32, hash: u64, item: T) {
        let i = self.free.pop().unwrap_or_else(|| {
            self.items.push(None);
            self.items.len() as u32 - 1
        });
        self.items[i as usize] = Some((hash, item));
        self.hashes.insert(hash, i);
        self.ids.insert(id, i);
    }

    fn live(&self) -> impl Iterator<Item = &T> {
        self.items.iter().filter_map(|s| s.as_ref().map(|(_, t)| t))
    }

    fn evict(&mut self) {
        let live: HashSet<u32> = self.ids.values().copied().collect();
        for (i, slot) in self.items.iter_mut().enumerate() {
            if slot.is_some() && !live.contains(&(i as u32)) {
                let (h, _) = slot.take().unwrap();
                self.hashes.remove(&h);
                self.free.push(i as u32);
            }
        }
    }
}

pub fn content(parts: &[&[u8]]) -> u64 {
    let mut h: u64 = 0xcbf29ce484222325;
    let mut mix = |w: u64| h = (h.rotate_left(5) ^ w).wrapping_mul(0x517cc1b727220a95);
    for p in parts {
        let c = p.chunks_exact(8);
        let rest = c.remainder();
        c.for_each(|w| mix(u64::from_le_bytes(w.try_into().unwrap())));
        let mut tail = [0u8; 8];
        tail[..rest.len()].copy_from_slice(rest);
        mix(u64::from_le_bytes(tail) ^ (p.len() as u64) << 56);
    }
    h
}

pub struct Resources {
    linear: Sampler,
    nearest: Sampler,
    clamp: Sampler,
    white: GpuTexture,
    volume: TextureView,
    cube: TextureView,
    pub arenas: Vec<Option<Arena>>,
    pub instance_buffers: Vec<Option<Buffer>>,
    pub groups: Vec<BindGroup>,
    group_keys: HashMap<(Pass, [u64; 7]), u32>,
    meshes: Pool<GpuMesh>,
    instances: Pool<Instances>,
    textures: Pool<GpuTexture>,
    uniforms: Pool<(BindGroup, usize)>,
    water_sources: HashMap<u32, (Vec<WaterVertex>, Vec<u32>)>,
    water: HashMap<(u32, u32), WaterMesh>,
    mirrored: HashMap<u32, (u32, BindGroup, usize)>,
    frames: u32,
}

#[allow(clippy::too_many_arguments)]
fn texture(
    device: &Device,
    queue: &Queue,
    dimension: TextureDimension,
    width: u32,
    height: u32,
    depth: u32,
    rgba: &[u8],
    label: &str,
    format: RecordedTextureFormat,
) -> Texture {
    let generate_mips = dimension == TextureDimension::D2 && format == RecordedTextureFormat::Rgba8;
    let mips;
    let data = if generate_mips {
        mips = mip_chain(width, height, depth, rgba);
        &mips[..]
    } else {
        rgba
    };
    device.create_texture_with_data(
        queue,
        &TextureDescriptor {
            label: Some(label),
            size: Extent3d {
                width,
                height,
                depth_or_array_layers: depth,
            },
            mip_level_count: if generate_mips {
                width.max(height).ilog2() + 1
            } else {
                1
            },
            sample_count: 1,
            dimension,
            format: match format {
                RecordedTextureFormat::Rgba8 => TextureFormat::Rgba8Unorm,
                RecordedTextureFormat::Rgba16Float => TextureFormat::Rgba16Float,
            },
            usage: TextureUsages::TEXTURE_BINDING,
            view_formats: &[],
        },
        TextureDataOrder::MipMajor,
        data,
    )
}

fn mip_chain(width: u32, height: u32, layers: u32, rgba: &[u8]) -> Vec<u8> {
    let mut out = rgba.to_vec();
    let (mut w, mut h, mut at) = (width as usize, height as usize, 0usize);
    while w > 1 || h > 1 {
        let (nw, nh) = ((w / 2).max(1), (h / 2).max(1));
        let (ls, ns) = (w * h * 4, nw * nh * 4);
        let mut next = vec![0u8; ns * layers as usize];
        let px =
            |src: &[u8], i: usize| u32::from_le_bytes(src[i * 4..i * 4 + 4].try_into().unwrap());
        for l in 0..layers as usize {
            let src = &out[at + l * ls..at + (l + 1) * ls];
            let dst = &mut next[l * ns..(l + 1) * ns];
            for y in 0..nh {
                let (r0, r1) = ((2 * y).min(h - 1) * w, (2 * y + 1).min(h - 1) * w);
                for x in 0..nw {
                    let (c0, c1) = ((2 * x).min(w - 1), (2 * x + 1).min(w - 1));
                    let p = [
                        px(src, r0 + c0),
                        px(src, r0 + c1),
                        px(src, r1 + c0),
                        px(src, r1 + c1),
                    ];
                    let lanes = |shift: u32| {
                        (p.iter().map(|v| v >> shift & 0x00ff00ff).sum::<u32>() + 0x00020002) >> 2
                            & 0x00ff00ff
                    };
                    dst[(y * nw + x) * 4..(y * nw + x) * 4 + 4]
                        .copy_from_slice(&(lanes(0) | lanes(8) << 8).to_le_bytes());
                }
            }
        }
        at = out.len();
        out.extend_from_slice(&next);
        (w, h) = (nw, nh);
    }
    out
}

#[allow(clippy::too_many_arguments)]
fn upload(
    device: &Device,
    queue: &Queue,
    width: u32,
    height: u32,
    layers: u32,
    rgba: &[u8],
    cube: bool,
    volume: bool,
    format: RecordedTextureFormat,
    sampler_bits: u32,
) -> GpuTexture {
    let t = texture(
        device,
        queue,
        TextureDimension::D2,
        width,
        height,
        layers,
        rgba,
        "texture",
        format,
    );
    let view = |dimension, count| {
        t.create_view(&TextureViewDescriptor {
            dimension: Some(dimension),
            array_layer_count: Some(count),
            ..Default::default()
        })
    };
    let sampler = (format == RecordedTextureFormat::Rgba16Float).then(|| {
        assert!(valid_sampler(sampler_bits));
        let address = |bits| match bits {
            0 => AddressMode::Repeat,
            1 => AddressMode::ClampToEdge,
            _ => AddressMode::MirrorRepeat,
        };
        device.create_sampler(&SamplerDescriptor {
            mag_filter: if sampler_bits & 8 != 0 {
                FilterMode::Linear
            } else {
                FilterMode::Nearest
            },
            min_filter: if sampler_bits & 1 != 0 {
                FilterMode::Linear
            } else {
                FilterMode::Nearest
            },
            mipmap_filter: if sampler_bits & 7 >= 4 {
                MipmapFilterMode::Linear
            } else {
                MipmapFilterMode::Nearest
            },
            address_mode_u: address(sampler_bits >> 4 & 3),
            address_mode_v: address(sampler_bits >> 6 & 3),
            lod_min_clamp: 0.,
            lod_max_clamp: 0.,
            ..Default::default()
        })
    });
    GpuTexture {
        sampler,
        flat: view(TextureViewDimension::D2, 1),
        array: view(TextureViewDimension::D2Array, layers),
        cube: (cube && layers == 6 && width == height).then(|| view(TextureViewDimension::Cube, 6)),
        volume: volume.then(|| {
            texture(
                device,
                queue,
                TextureDimension::D3,
                width,
                height,
                layers,
                rgba,
                "volume",
                format,
            )
            .create_view(&Default::default())
        }),
    }
}

fn arena(device: &Device, vertices: &[u8], indices: &[u32]) -> Arena {
    let buffer = |contents: &[u8], usage| {
        device.create_buffer_init(&BufferInitDescriptor {
            label: None,
            contents,
            usage,
        })
    };
    Arena {
        vertices: buffer(vertices, BufferUsages::VERTEX),
        indices: buffer(bytemuck::cast_slice(indices), BufferUsages::INDEX),
    }
}

fn uniform_group(device: &Device, pipelines: &Pipelines, bytes: &[u8]) -> BindGroup {
    let buffer = device.create_buffer_init(&BufferInitDescriptor {
        label: None,
        contents: bytes,
        usage: BufferUsages::UNIFORM,
    });
    device.create_bind_group(&BindGroupDescriptor {
        label: None,
        layout: &pipelines.uniform,
        entries: &[BindGroupEntry {
            binding: 0,
            resource: buffer.as_entire_binding(),
        }],
    })
}

fn reserved(id: u32) -> bool {
    matches!(id, NO_TEXTURE | SHADOW_MAP | REFLECTION)
}

impl Resources {
    pub fn new(device: &Device, queue: &Queue) -> Self {
        let sampler = |filter, address_mode| {
            device.create_sampler(&SamplerDescriptor {
                address_mode_u: address_mode,
                address_mode_v: address_mode,
                address_mode_w: address_mode,
                mag_filter: filter,
                min_filter: filter,
                mipmap_filter: if filter == FilterMode::Linear {
                    MipmapFilterMode::Linear
                } else {
                    MipmapFilterMode::Nearest
                },
                ..Default::default()
            })
        };
        let white = upload(
            device,
            queue,
            1,
            1,
            1,
            &[255; 4],
            false,
            true,
            RecordedTextureFormat::Rgba8,
            9,
        );
        let cube = texture(
            device,
            queue,
            TextureDimension::D2,
            1,
            1,
            6,
            &[255; 24],
            "cube",
            RecordedTextureFormat::Rgba8,
        )
        .create_view(&TextureViewDescriptor {
            dimension: Some(TextureViewDimension::Cube),
            ..Default::default()
        });
        Resources {
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
            volume: white.volume.clone().unwrap(),
            white,
            cube,
            arenas: vec![],
            instance_buffers: vec![],
            groups: vec![],
            group_keys: HashMap::new(),
            meshes: Pool::new(),
            instances: Pool::new(),
            textures: Pool::new(),
            uniforms: Pool::new(),
            water_sources: HashMap::new(),
            water: HashMap::new(),
            mirrored: HashMap::new(),
            frames: 0,
        }
    }

    pub fn clear_groups(&mut self) {
        self.groups.clear();
        self.group_keys.clear();
    }

    pub fn sync(
        &mut self,
        device: &Device,
        queue: &Queue,
        pipelines: &Pipelines,
        frame: &Frame,
        stats: &mut Stats,
    ) {
        let t = now();
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
        let mut pending: Vec<(u64, Vec<u8>, Vec<u32>)> = vec![];
        for m in &frame.meshes {
            let h = content(&[
                &[m.pass as u8],
                &m.vertices,
                bytemuck::cast_slice(&m.indices),
            ]);
            if m.pass == Pass::HdWater && self.meshes.hash_of(m.id) != Some(h) {
                self.water_sources.insert(
                    m.id,
                    (bytemuck::pod_collect_to_vec(&m.vertices), m.indices.clone()),
                );
                let arenas = &mut self.arenas;
                self.water.retain(|k, w| {
                    k.0 != m.id || {
                        arenas[w.mesh.arena as usize] = None;
                        false
                    }
                });
            }
            if self.meshes.bind(m.id, h) {
                continue;
            }
            let stride = m.pass.stride();
            let slot = pending
                .iter()
                .position(|p| p.0 == stride)
                .unwrap_or_else(|| {
                    pending.push((stride, vec![], vec![]));
                    pending.len() - 1
                });
            let (_, v, i) = &mut pending[slot];
            let mesh = GpuMesh {
                arena: (self.arenas.len() + slot) as u32,
                first: i.len() as u32,
                count: m.indices.len() as u32,
                base: (v.len() as u64 / stride) as i32,
            };
            v.extend_from_slice(&m.vertices);
            i.extend_from_slice(&m.indices);
            self.meshes.insert(m.id, h, mesh);
        }
        for (_, v, i) in &pending {
            self.arenas.push(Some(arena(device, v, i)));
            stats.upload(v.len() + i.len() * 4);
        }
        let mut bytes = vec![];
        for i in &frame.instances {
            let h = content(&[&i.count.to_le_bytes(), &i.bytes]);
            if self.instances.bind(i.id, h) {
                continue;
            }
            self.instances.insert(
                i.id,
                h,
                Instances {
                    buffer: self.instance_buffers.len() as u32,
                    first: (bytes.len() / 80) as u32,
                    count: i.count,
                },
            );
            bytes.extend_from_slice(&i.bytes);
        }
        if !bytes.is_empty() {
            self.instance_buffers
                .push(Some(device.create_buffer_init(&BufferInitDescriptor {
                    label: None,
                    contents: &bytes,
                    usage: BufferUsages::VERTEX,
                })));
            stats.upload(bytes.len());
        }
        for t in &frame.textures {
            let (cube, volume) = (cubes.contains(&t.id), volumes.contains(&t.id));
            let h = content(&[
                bytemuck::cast_slice(&[
                    t.width,
                    t.height,
                    t.layers,
                    t.format as u32,
                    if t.format == RecordedTextureFormat::Rgba8 {
                        0
                    } else {
                        t.sampler
                    },
                    cube as u32 | (volume as u32) << 1,
                ]),
                &t.rgba,
            ]);
            if self.textures.bind(t.id, h) {
                continue;
            }
            self.textures.insert(
                t.id,
                h,
                upload(
                    device, queue, t.width, t.height, t.layers, &t.rgba, cube, volume, t.format,
                    t.sampler,
                ),
            );
            stats.upload(t.rgba.len() * (1 + volume as usize));
        }
        for u in &frame.uniforms {
            let (before, h) = (self.uniforms.index(u.id), content(&[&u.bytes]));
            if !self.uniforms.bind(u.id, h) {
                self.uniforms.insert(
                    u.id,
                    h,
                    (uniform_group(device, pipelines, &u.bytes), u.bytes.len()),
                );
                stats.upload(u.bytes.len());
            }
            if self.uniforms.index(u.id) != before {
                self.mirrored.remove(&u.id);
            }
        }
        stats.sync += now() - t;
        let t = now();
        for d in frame
            .draws
            .iter()
            .filter(|d| d.skip.is_none() && d.pass == Pass::HdWater)
        {
            let Some(u) = frame.uniforms.iter().find(|u| u.id == d.uniform) else {
                continue;
            };
            let key = (d.mesh, d.uniform);
            let Some((vertices, indices)) = self.water_sources.get(&d.mesh) else {
                continue;
            };
            let Ok(vp) =
                <[u8; 64]>::try_from(&u.bytes[water::VIEW_PROJECTION..water::VIEW_PROJECTION + 64])
            else {
                continue;
            };
            let viewport = if d.viewport[2] > 0 {
                d.viewport
            } else {
                [0, 0, frame.width, frame.height]
            };
            let tess_pixels = water::tess_pixels(&u.bytes);
            if self.water.get(&key).is_some_and(|w| {
                water::waving(&u.bytes)
                    && w.vp == vp
                    && w.viewport == viewport
                    && w.tess_pixels == tess_pixels
            }) {
                continue;
            }
            if let Some(old) = self.water.remove(&key) {
                self.arenas[old.mesh.arena as usize] = None;
            }
            if !water::waving(&u.bytes) {
                continue;
            }
            let (v, i) = water::tessellate_with_pixels(
                vertices,
                indices,
                &bytemuck::pod_read_unaligned(&vp),
                viewport,
                tess_pixels,
            );
            let mesh = GpuMesh {
                arena: self.arenas.len() as u32,
                first: 0,
                count: i.len() as u32,
                base: 0,
            };
            self.arenas
                .push(Some(arena(device, bytemuck::cast_slice(&v), &i)));
            stats.upload(v.len() * size_of::<WaterVertex>() + i.len() * 4);
            self.water.insert(
                key,
                WaterMesh {
                    mesh,
                    vp,
                    viewport,
                    tess_pixels,
                },
            );
        }
        stats.water += now() - t;
        self.frames += 1;
        if self.frames.is_multiple_of(EVICT_EVERY) {
            self.evict();
        }
    }

    fn evict(&mut self) {
        self.meshes.evict();
        self.instances.evict();
        self.textures.evict();
        self.uniforms.evict();
        let arenas: HashSet<u32> = self
            .meshes
            .live()
            .map(|m| m.arena)
            .chain(self.water.values().map(|w| w.mesh.arena))
            .collect();
        self.arenas.iter_mut().enumerate().for_each(|(i, a)| {
            if !arenas.contains(&(i as u32)) {
                *a = None
            }
        });
        let buffers: HashSet<u32> = self.instances.live().map(|i| i.buffer).collect();
        self.instance_buffers
            .iter_mut()
            .enumerate()
            .for_each(|(i, b)| {
                if !buffers.contains(&(i as u32)) {
                    *b = None
                }
            });
        self.clear_groups();
    }

    fn group(
        &mut self,
        device: &Device,
        pipelines: &Pipelines,
        pass: Pass,
        ids: &[u32],
        views: &Views,
    ) -> u32 {
        let mut key = [0u64; 7];
        for (i, b) in pass.bindings().iter().enumerate() {
            let id = b
                .source()
                .and_then(|k| ids.get(k).copied())
                .unwrap_or(NO_TEXTURE);
            key[i] = if reserved(id) {
                u64::from(id)
            } else {
                self.textures.hash_of(id).unwrap_or(u64::from(NO_TEXTURE))
            };
        }
        if let Some(&g) = self.group_keys.get(&(pass, key)) {
            return g;
        }
        let entries: Vec<_> = pass
            .bindings()
            .iter()
            .enumerate()
            .flat_map(|(i, b)| {
                let id = key[i];
                let t = self.textures.by_hash(id).unwrap_or(&self.white);
                let view = match b {
                    Bind::Flat(_) | Bind::Clamp(_) => &t.flat,
                    Bind::Layered(_) => &t.array,
                    Bind::Volume(_) => t.volume.as_ref().unwrap_or(&self.volume),
                    Bind::Cube(_) => t.cube.as_ref().unwrap_or(&self.cube),
                    Bind::Depth(_) if id == u64::from(SHADOW_MAP) => views.shadow,
                    Bind::Depth(_) => &t.flat,
                    Bind::Reflection(_) if id == u64::from(REFLECTION) => views.reflection,
                    Bind::Reflection(_) => &t.flat,
                    Bind::SceneColor => views.scene_color,
                    Bind::SceneDepth => views.scene_depth,
                };
                let sampler = if let Some(s) = t.sampler.as_ref().filter(|_| b.filterable()) {
                    s
                } else {
                    match b {
                        Bind::Clamp(_) | Bind::SceneColor | Bind::Reflection(_) => &self.clamp,
                        _ if b.filterable() => &self.linear,
                        _ => &self.nearest,
                    }
                };
                [
                    BindGroupEntry {
                        binding: 2 * i as u32,
                        resource: BindingResource::TextureView(view),
                    },
                    BindGroupEntry {
                        binding: 2 * i as u32 + 1,
                        resource: BindingResource::Sampler(sampler),
                    },
                ]
            })
            .collect();
        self.groups
            .push(device.create_bind_group(&BindGroupDescriptor {
                label: None,
                layout: pipelines.texture_layout(pass),
                entries: &entries,
            }));
        let g = self.groups.len() as u32 - 1;
        self.group_keys.insert((pass, key), g);
        g
    }

    pub fn slot(
        &mut self,
        device: &Device,
        pipelines: &Pipelines,
        d: &Draw,
        mirrored: bool,
        views: &Views,
    ) -> Option<Slot> {
        let (uniform, uniform_key, size) = if mirrored {
            let (_, g, size) = self.mirrored.get(&d.uniform)?;
            (g.clone(), MIRRORED | u64::from(d.uniform), *size)
        } else {
            let i = self.uniforms.index(d.uniform)?;
            let (g, size) = self.uniforms.item(i)?;
            (g.clone(), u64::from(i), *size)
        };
        if size < d.pass.block_size() {
            return None;
        }
        let water = (d.pass == Pass::HdWater)
            .then(|| self.water.get(&(d.mesh, d.uniform)).map(|w| w.mesh))
            .flatten();
        let mesh = water.or_else(|| self.meshes.get(d.mesh).copied())?;
        let instances = match self.instances.get(d.instances) {
            Some(p) => Some(*p),
            None if d.pass == Pass::Instanced => return None,
            None => None,
        };
        let textures = self.group(device, pipelines, d.pass, &d.textures, views);
        Some(Slot {
            uniform,
            uniform_key,
            textures,
            mesh,
            instances,
        })
    }

    pub fn mirror(
        &mut self,
        device: &Device,
        pipelines: &Pipelines,
        frame: &Frame,
        ids: impl Iterator<Item = (Pass, u32)>,
        plane: f32,
    ) {
        self.mirrored.retain(|_, (p, ..)| *p == plane.to_bits());
        for (pass, id) in ids {
            if self.mirrored.contains_key(&id) {
                continue;
            }
            let Some(u) = frame.uniforms.iter().find(|u| u.id == id) else {
                continue;
            };
            let mut bytes = u.bytes.clone();
            water::mirror(&mut bytes, plane, pass == Pass::Terrain);
            self.mirrored.insert(
                id,
                (
                    plane.to_bits(),
                    uniform_group(device, pipelines, &bytes),
                    bytes.len(),
                ),
            );
        }
    }

    pub fn water_source(&self, id: u32) -> Option<&(Vec<WaterVertex>, Vec<u32>)> {
        self.water_sources.get(&id)
    }
}

#[cfg(all(test, not(target_arch = "wasm32")))]
mod water_texture_tests {
    use super::*;
    use crate::{capture, frame};

    #[test]
    fn typed_fixture_samples_hdr_negative_zero_and_one() {
        let renderer = pollster::block_on(capture::headless());
        let device = &renderer.gpu.device;
        let features = renderer
            .gpu
            .adapter
            .get_texture_format_features(TextureFormat::Rgba16Float);
        assert!(
            features
                .flags
                .contains(TextureFormatFeatureFlags::FILTERABLE)
        );
        let frame = frame::parse(include_bytes!("../tests/fixtures/typed.orf")).unwrap();
        let t = &frame.textures[0];
        let raw = texture(
            device,
            &renderer.gpu.queue,
            TextureDimension::D2,
            t.width,
            t.height,
            t.layers,
            &t.rgba,
            "half mip limit",
            t.format,
        );
        assert_eq!(raw.mip_level_count(), 1);
        let image = upload(
            device,
            &renderer.gpu.queue,
            t.width,
            t.height,
            t.layers,
            &t.rgba,
            false,
            false,
            t.format,
            t.sampler,
        );
        let shader = device.create_shader_module(ShaderModuleDescriptor {
            label: None,
            source: ShaderSource::Wgsl("@group(0) @binding(0) var t: texture_2d<f32>; @group(0) @binding(1) var s: sampler; @group(0) @binding(2) var<storage, read_write> out: array<vec4<f32>, 2>; @compute @workgroup_size(1) fn probe() { out[0] = textureSampleLevel(t,s,vec2<f32>(0.25,0.5),0.0); out[1] = textureSampleLevel(t,s,vec2<f32>(1.5,-0.5),4.0); }".into()),
        });
        let pipeline = device.create_compute_pipeline(&ComputePipelineDescriptor {
            label: None,
            layout: None,
            module: &shader,
            entry_point: Some("probe"),
            compilation_options: Default::default(),
            cache: None,
        });
        let output = device.create_buffer(&BufferDescriptor {
            label: None,
            size: 32,
            usage: BufferUsages::STORAGE | BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });
        let readback = device.create_buffer(&BufferDescriptor {
            label: None,
            size: 32,
            usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });
        let group = device.create_bind_group(&BindGroupDescriptor {
            label: None,
            layout: &pipeline.get_bind_group_layout(0),
            entries: &[
                BindGroupEntry {
                    binding: 0,
                    resource: BindingResource::TextureView(&image.flat),
                },
                BindGroupEntry {
                    binding: 1,
                    resource: BindingResource::Sampler(image.sampler.as_ref().unwrap()),
                },
                BindGroupEntry {
                    binding: 2,
                    resource: output.as_entire_binding(),
                },
            ],
        });
        let mut encoder = device.create_command_encoder(&Default::default());
        {
            let mut pass = encoder.begin_compute_pass(&Default::default());
            pass.set_pipeline(&pipeline);
            pass.set_bind_group(0, &group, &[]);
            pass.dispatch_workgroups(1, 1, 1);
        }
        encoder.copy_buffer_to_buffer(&output, 0, &readback, 0, 32);
        renderer.gpu.queue.submit([encoder.finish()]);
        readback.map_async(MapMode::Read, .., |r| r.unwrap());
        device.poll(PollType::wait_indefinitely()).unwrap();
        let values: Vec<f32> =
            bytemuck::pod_collect_to_vec(&readback.get_mapped_range(..).unwrap());
        assert_eq!(values, [4., -0.5, 0., 1.].repeat(2));
    }

    #[test]
    fn packed_volume_wraps_r_and_cube_samples_positive_negative_xyz_faces() {
        let renderer = pollster::block_on(capture::headless());
        let device = &renderer.gpu.device;
        let resources = Resources::new(device, &renderer.gpu.queue);
        let volume = upload(
            device,
            &renderer.gpu.queue,
            1,
            1,
            2,
            &[0, 0, 0, 255, 255, 255, 255, 255],
            false,
            true,
            RecordedTextureFormat::Rgba8,
            9,
        );
        let faces: Vec<u8> = (1..=6).flat_map(|i| [i, 0, 0, 255]).collect();
        let cube = upload(
            device,
            &renderer.gpu.queue,
            1,
            1,
            6,
            &faces,
            true,
            false,
            RecordedTextureFormat::Rgba8,
            9,
        );
        let shader = device.create_shader_module(ShaderModuleDescriptor {
            label: None,
            source: ShaderSource::Wgsl("@group(0) @binding(0) var volume: texture_3d<f32>; @group(0) @binding(1) var s: sampler; @group(0) @binding(2) var cube: texture_cube<f32>; @group(0) @binding(3) var<storage, read_write> out: array<f32, 7>; @compute @workgroup_size(1) fn probe() { out[0] = textureSampleLevel(volume, s, vec3<f32>(0.5, 0.5, 0.0), 0.0).r; let dirs = array<vec3<f32>, 6>(vec3<f32>(1.,0.,0.),vec3<f32>(-1.,0.,0.),vec3<f32>(0.,1.,0.),vec3<f32>(0.,-1.,0.),vec3<f32>(0.,0.,1.),vec3<f32>(0.,0.,-1.)); for(var i=0u; i<6u; i++) { out[i+1u] = textureSampleLevel(cube, s, dirs[i], 0.0).r; } }".into()),
        });
        let pipeline = device.create_compute_pipeline(&ComputePipelineDescriptor {
            label: None,
            layout: None,
            module: &shader,
            entry_point: Some("probe"),
            compilation_options: Default::default(),
            cache: None,
        });
        let output = device.create_buffer(&BufferDescriptor {
            label: None,
            size: 28,
            usage: BufferUsages::STORAGE | BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });
        let readback = device.create_buffer(&BufferDescriptor {
            label: None,
            size: 28,
            usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });
        let group = device.create_bind_group(&BindGroupDescriptor {
            label: None,
            layout: &pipeline.get_bind_group_layout(0),
            entries: &[
                BindGroupEntry {
                    binding: 0,
                    resource: BindingResource::TextureView(volume.volume.as_ref().unwrap()),
                },
                BindGroupEntry {
                    binding: 1,
                    resource: BindingResource::Sampler(&resources.linear),
                },
                BindGroupEntry {
                    binding: 2,
                    resource: BindingResource::TextureView(cube.cube.as_ref().unwrap()),
                },
                BindGroupEntry {
                    binding: 3,
                    resource: output.as_entire_binding(),
                },
            ],
        });
        let mut encoder = device.create_command_encoder(&Default::default());
        {
            let mut pass = encoder.begin_compute_pass(&Default::default());
            pass.set_pipeline(&pipeline);
            pass.set_bind_group(0, &group, &[]);
            pass.dispatch_workgroups(1, 1, 1);
        }
        encoder.copy_buffer_to_buffer(&output, 0, &readback, 0, 28);
        renderer.gpu.queue.submit([encoder.finish()]);
        readback.map_async(MapMode::Read, .., |r| r.unwrap());
        device.poll(PollType::wait_indefinitely()).unwrap();
        let values: Vec<f32> =
            bytemuck::pod_collect_to_vec(&readback.get_mapped_range(..).unwrap());
        assert!(
            (values[0] - 0.5).abs() <= 1. / 65535.,
            "R wrap sampled {} instead of the slice-boundary average",
            values[0]
        );
        for (i, value) in values[1..].iter().enumerate() {
            assert!(
                (value - (i + 1) as f32 / 255.).abs() < 1e-6,
                "cube face {i}: {value}"
            );
        }
    }
}
