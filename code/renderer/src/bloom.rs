use wgpu::*;

pub const BLUR_BASE: u32 = 256;
pub const KERNEL: [f32; 9] = [
    0.091396265,
    0.088584304,
    0.08065692,
    0.068989515,
    0.055434637,
    0.04184426,
    0.029672023,
    0.019765828,
    0.012369139,
];
pub const LUMINANCE: [f32; 3] = [0.2126, 0.7152, 0.0722];
pub const INTERMEDIATE: TextureFormat = TextureFormat::Rgba8Unorm;

pub use crate::frame::BloomParams;

pub fn floor_pow2(value: u32) -> u32 {
    if value == 0 {
        0
    } else {
        1 << (31 - value.leading_zeros())
    }
}

pub fn levels(width: u32, height: u32) -> Vec<(u32, u32)> {
    let (mut w, mut h) = (floor_pow2(width), floor_pow2(height));
    let mut out = Vec::new();
    while w > BLUR_BASE || h > BLUR_BASE {
        out.push((w, h));
        if w > BLUR_BASE {
            w >>= 1;
        }
        if h > BLUR_BASE {
            h >>= 1;
        }
    }
    out
}

#[repr(C)]
#[derive(Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
struct Uniform {
    white_point: f32,
    brightpass: f32,
    bloom: f32,
    pad: f32,
    step: [f32; 2],
    pad2: [f32; 2],
}

struct Chain {
    size: (u32, u32),
    levels: Vec<TextureView>,
    a: TextureView,
    b: TextureView,
}

pub struct Bloom {
    layout: BindGroupLayout,
    sampler: Sampler,
    copy: RenderPipeline,
    bright: RenderPipeline,
    blur: RenderPipeline,
    composite: RenderPipeline,
    chain: Option<Chain>,
}

fn texture(device: &Device, w: u32, h: u32) -> TextureView {
    device
        .create_texture(&TextureDescriptor {
            label: Some("bloom"),
            size: Extent3d {
                width: w,
                height: h,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: TextureDimension::D2,
            format: INTERMEDIATE,
            usage: TextureUsages::TEXTURE_BINDING | TextureUsages::RENDER_ATTACHMENT,
            view_formats: &[],
        })
        .create_view(&Default::default())
}

fn entry(binding: u32, ty: BindingType, visibility: ShaderStages) -> BindGroupLayoutEntry {
    BindGroupLayoutEntry {
        binding,
        visibility,
        ty,
        count: None,
    }
}

fn sampled() -> BindingType {
    BindingType::Texture {
        sample_type: TextureSampleType::Float { filterable: true },
        view_dimension: TextureViewDimension::D2,
        multisampled: false,
    }
}

impl Bloom {
    pub fn new(device: &Device, target: TextureFormat) -> Self {
        let layout = device.create_bind_group_layout(&BindGroupLayoutDescriptor {
            label: Some("bloom"),
            entries: &[
                entry(0, sampled(), ShaderStages::FRAGMENT),
                entry(1, sampled(), ShaderStages::FRAGMENT),
                entry(
                    2,
                    BindingType::Sampler(SamplerBindingType::Filtering),
                    ShaderStages::FRAGMENT,
                ),
                entry(
                    3,
                    BindingType::Buffer {
                        ty: BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    ShaderStages::FRAGMENT,
                ),
            ],
        });
        let module = device.create_shader_module(ShaderModuleDescriptor {
            label: Some("bloom"),
            source: ShaderSource::Wgsl(SHADER.into()),
        });
        let pipeline_layout = device.create_pipeline_layout(&PipelineLayoutDescriptor {
            label: Some("bloom"),
            bind_group_layouts: &[Some(&layout)],
            immediate_size: 0,
        });
        let make = |fs: &str, format: TextureFormat| {
            device.create_render_pipeline(&RenderPipelineDescriptor {
                label: Some(fs),
                layout: Some(&pipeline_layout),
                vertex: VertexState {
                    module: &module,
                    entry_point: Some("vs"),
                    compilation_options: Default::default(),
                    buffers: &[],
                },
                primitive: PrimitiveState::default(),
                depth_stencil: None,
                multisample: MultisampleState::default(),
                fragment: Some(FragmentState {
                    module: &module,
                    entry_point: Some(fs),
                    compilation_options: Default::default(),
                    targets: &[Some(format.into())],
                }),
                multiview_mask: None,
                cache: None,
            })
        };
        Bloom {
            copy: make("fs_copy", INTERMEDIATE),
            bright: make("fs_bright", INTERMEDIATE),
            blur: make("fs_blur", INTERMEDIATE),
            composite: make("fs_composite", target),
            sampler: device.create_sampler(&SamplerDescriptor {
                mag_filter: FilterMode::Linear,
                min_filter: FilterMode::Linear,
                ..Default::default()
            }),
            layout,
            chain: None,
        }
    }

    fn ensure(&mut self, device: &Device, width: u32, height: u32) {
        if self
            .chain
            .as_ref()
            .is_some_and(|c| c.size == (width, height))
        {
            return;
        }
        self.chain = Some(Chain {
            size: (width, height),
            levels: levels(width, height)
                .into_iter()
                .map(|(w, h)| texture(device, w, h))
                .collect(),
            a: texture(device, BLUR_BASE, BLUR_BASE),
            b: texture(device, BLUR_BASE, BLUR_BASE),
        });
    }

    #[allow(clippy::too_many_arguments)]
    fn pass(
        &self,
        device: &Device,
        encoder: &mut CommandEncoder,
        pipeline: &RenderPipeline,
        first: &TextureView,
        second: &TextureView,
        uniform: Uniform,
        target: &TextureView,
    ) {
        let buffer = device.create_buffer(&BufferDescriptor {
            label: Some("bloom"),
            size: std::mem::size_of::<Uniform>() as u64,
            usage: BufferUsages::UNIFORM,
            mapped_at_creation: true,
        });
        buffer
            .get_mapped_range_mut(..)
            .unwrap()
            .copy_from_slice(bytemuck::bytes_of(&uniform));
        buffer.unmap();
        let group = device.create_bind_group(&BindGroupDescriptor {
            label: Some("bloom"),
            layout: &self.layout,
            entries: &[
                BindGroupEntry {
                    binding: 0,
                    resource: BindingResource::TextureView(first),
                },
                BindGroupEntry {
                    binding: 1,
                    resource: BindingResource::TextureView(second),
                },
                BindGroupEntry {
                    binding: 2,
                    resource: BindingResource::Sampler(&self.sampler),
                },
                BindGroupEntry {
                    binding: 3,
                    resource: buffer.as_entire_binding(),
                },
            ],
        });
        let mut pass = encoder.begin_render_pass(&RenderPassDescriptor {
            label: Some("bloom"),
            color_attachments: &[Some(RenderPassColorAttachment {
                view: target,
                depth_slice: None,
                resolve_target: None,
                ops: Operations {
                    load: LoadOp::Clear(Color::BLACK),
                    store: StoreOp::Store,
                },
            })],
            depth_stencil_attachment: None,
            timestamp_writes: None,
            occlusion_query_set: None,
            multiview_mask: None,
        });
        pass.set_pipeline(pipeline);
        pass.set_bind_group(0, &group, &[]);
        pass.draw(0..3, 0..1);
    }

    pub fn encode(
        &mut self,
        device: &Device,
        encoder: &mut CommandEncoder,
        scene: &TextureView,
        size: (u32, u32),
        target: &TextureView,
        params: BloomParams,
    ) {
        self.ensure(device, size.0, size.1);
        let chain = self.chain.as_ref().unwrap();
        let base = Uniform {
            white_point: params.white_point,
            brightpass: params.brightpass,
            bloom: params.bloom,
            pad: 0.,
            step: [0.; 2],
            pad2: [0.; 2],
        };
        let mut source = scene;
        for level in &chain.levels {
            self.pass(device, encoder, &self.copy, source, source, base, level);
            source = level;
        }
        let step = 1. / BLUR_BASE as f32;
        self.pass(
            device,
            encoder,
            &self.bright,
            source,
            source,
            base,
            &chain.a,
        );
        let horizontal = Uniform {
            step: [step, 0.],
            ..base
        };
        self.pass(
            device, encoder, &self.blur, &chain.a, &chain.a, horizontal, &chain.b,
        );
        let vertical = Uniform {
            step: [0., step],
            ..base
        };
        self.pass(
            device, encoder, &self.blur, &chain.b, &chain.b, vertical, &chain.a,
        );
        self.pass(
            device,
            encoder,
            &self.composite,
            scene,
            &chain.a,
            base,
            target,
        );
    }
}

pub const SHADER: &str = r#"
struct Params {
    white_point: f32,
    brightpass: f32,
    bloom: f32,
    pad: f32,
    step: vec2<f32>,
    pad2: vec2<f32>,
}

@group(0) @binding(0) var tex0: texture_2d<f32>;
@group(0) @binding(1) var tex1: texture_2d<f32>;
@group(0) @binding(2) var samp: sampler;
@group(0) @binding(3) var<uniform> params: Params;

struct VOut {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}

@vertex
fn vs(@builtin(vertex_index) i: u32) -> VOut {
    let p = vec2<f32>(select(-1.0, 3.0, i == 1u), select(-1.0, 3.0, i == 2u));
    var out: VOut;
    out.uv = vec2<f32>(p.x * 0.5 + 0.5, 0.5 - p.y * 0.5);
    out.position = vec4<f32>(p, 0.0, 1.0);
    return out;
}

const LUM = vec3<f32>(0.2126, 0.7152, 0.0722);

@fragment
fn fs_copy(in: VOut) -> @location(0) vec4<f32> {
    return textureSampleLevel(tex0, samp, in.uv, 0.0);
}

@fragment
fn fs_bright(in: VOut) -> @location(0) vec4<f32> {
    let col = textureSampleLevel(tex0, samp, in.uv, 0.0);
    return col * step(params.white_point, dot(LUM, col.rgb));
}

@fragment
fn fs_blur(in: VOut) -> @location(0) vec4<f32> {
    let s = vec2<f32>(params.step.x, params.step.y);
    var c = textureSampleLevel(tex0, samp, in.uv, 0.0) * 0.091396265;
    c += textureSampleLevel(tex0, samp, in.uv - 1.0 * s, 0.0) * 0.088584304;
    c += textureSampleLevel(tex0, samp, in.uv + 1.0 * s, 0.0) * 0.088584304;
    c += textureSampleLevel(tex0, samp, in.uv - 2.0 * s, 0.0) * 0.08065692;
    c += textureSampleLevel(tex0, samp, in.uv + 2.0 * s, 0.0) * 0.08065692;
    c += textureSampleLevel(tex0, samp, in.uv - 3.0 * s, 0.0) * 0.068989515;
    c += textureSampleLevel(tex0, samp, in.uv + 3.0 * s, 0.0) * 0.068989515;
    c += textureSampleLevel(tex0, samp, in.uv - 4.0 * s, 0.0) * 0.055434637;
    c += textureSampleLevel(tex0, samp, in.uv + 4.0 * s, 0.0) * 0.055434637;
    c += textureSampleLevel(tex0, samp, in.uv - 5.0 * s, 0.0) * 0.04184426;
    c += textureSampleLevel(tex0, samp, in.uv + 5.0 * s, 0.0) * 0.04184426;
    c += textureSampleLevel(tex0, samp, in.uv - 6.0 * s, 0.0) * 0.029672023;
    c += textureSampleLevel(tex0, samp, in.uv + 6.0 * s, 0.0) * 0.029672023;
    c += textureSampleLevel(tex0, samp, in.uv - 7.0 * s, 0.0) * 0.019765828;
    c += textureSampleLevel(tex0, samp, in.uv + 7.0 * s, 0.0) * 0.019765828;
    c += textureSampleLevel(tex0, samp, in.uv - 8.0 * s, 0.0) * 0.012369139;
    c += textureSampleLevel(tex0, samp, in.uv + 8.0 * s, 0.0) * 0.012369139;
    return c;
}

@fragment
fn fs_composite(in: VOut) -> @location(0) vec4<f32> {
    let scene = textureSampleLevel(tex0, samp, in.uv, 0.0);
    let bloom = textureSampleLevel(tex1, samp, in.uv, 0.0);
    let pre = 0.99 * dot(LUM, scene.rgb) + 0.01;
    let post = pre * (1.0 + (pre / params.bloom)) / (pre + 1.0);
    let out = scene * (post / pre) + bloom * params.brightpass;
    return vec4<f32>(out.rgb, 1.0);
}
"#;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn floor_pow2_matches_the_kotlin_furthest_bit() {
        for (v, want) in [
            (1, 1),
            (255, 128),
            (256, 256),
            (512, 512),
            (518, 512),
            (1000, 512),
        ] {
            assert_eq!(floor_pow2(v), want);
        }
    }

    #[test]
    fn levels_halve_until_both_sides_reach_the_blur_base() {
        assert_eq!(levels(512, 334), vec![(512, 256)]);
        assert_eq!(levels(200, 200), vec![]);
        assert_eq!(
            levels(2048, 512),
            vec![(2048, 512), (1024, 256), (512, 256)]
        );
        assert_eq!(levels(1920, 1080), vec![(1024, 1024), (512, 512)]);
    }

    #[test]
    fn kernel_is_the_kotlin_taps_and_sums_to_the_unnormalised_total() {
        let sum: f32 = KERNEL[0] + 2. * KERNEL[1..].iter().sum::<f32>();
        assert!((sum - 0.88603).abs() < 1e-4);
    }
}
