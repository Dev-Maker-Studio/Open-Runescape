use crate::{
    pass::{self, INSTANCE_STRIDE, Pass},
    shaders,
};
use std::{borrow::Cow, collections::HashMap};
use wgpu::*;

pub const DEPTH: TextureFormat = TextureFormat::Depth32Float;

pub struct Pipelines {
    pub uniform: BindGroupLayout,
    textures: Vec<BindGroupLayout>,
    format: TextureFormat,
    cache: HashMap<(Pass, u32), RenderPipeline>,
}

fn texture_layout(device: &Device, pass: Pass) -> BindGroupLayout {
    let entries: Vec<_> = pass
        .bindings()
        .iter()
        .enumerate()
        .flat_map(|(i, b)| {
            let i = i as u32;
            let filterable = b.filterable();
            [
                BindGroupLayoutEntry {
                    binding: 2 * i,
                    visibility: ShaderStages::VERTEX_FRAGMENT,
                    ty: BindingType::Texture { sample_type: TextureSampleType::Float { filterable }, view_dimension: b.dimension(), multisampled: false },
                    count: None,
                },
                BindGroupLayoutEntry {
                    binding: 2 * i + 1,
                    visibility: ShaderStages::VERTEX_FRAGMENT,
                    ty: BindingType::Sampler(if filterable { SamplerBindingType::Filtering } else { SamplerBindingType::NonFiltering }),
                    count: None,
                },
            ]
        })
        .collect();
    device.create_bind_group_layout(&BindGroupLayoutDescriptor { label: None, entries: &entries })
}

fn module(device: &Device, name: &str) -> ShaderModule {
    device.create_shader_module(ShaderModuleDescriptor { label: Some(name), source: ShaderSource::Wgsl(Cow::Borrowed(shaders::get(name))) })
}

fn blend(mode: u32) -> Option<BlendState> {
    let one = |src, dst| BlendComponent { src_factor: src, dst_factor: dst, operation: BlendOperation::Add };
    match mode {
        1 => Some(BlendState { color: one(BlendFactor::SrcAlpha, BlendFactor::OneMinusSrcAlpha), alpha: one(BlendFactor::SrcAlpha, BlendFactor::OneMinusSrcAlpha) }),
        2 => Some(BlendState { color: one(BlendFactor::SrcAlpha, BlendFactor::One), alpha: one(BlendFactor::One, BlendFactor::One) }),
        3 => Some(BlendState::PREMULTIPLIED_ALPHA_BLENDING),
        _ => None,
    }
}

fn compare(index: u32) -> CompareFunction {
    use CompareFunction::*;
    [Never, Less, Equal, LessEqual, Greater, NotEqual, GreaterEqual, Always][index as usize & 7]
}

impl Pipelines {
    pub fn new(device: &Device, format: TextureFormat) -> Self {
        let uniform = device.create_bind_group_layout(&BindGroupLayoutDescriptor {
            label: None,
            entries: &[BindGroupLayoutEntry {
                binding: 0,
                visibility: ShaderStages::VERTEX_FRAGMENT,
                ty: BindingType::Buffer { ty: BufferBindingType::Uniform, has_dynamic_offset: false, min_binding_size: None },
                count: None,
            }],
        });
        let textures = pass::ALL.iter().map(|p| texture_layout(device, *p)).collect();
        Pipelines { uniform, textures, format, cache: HashMap::new() }
    }

    pub fn texture_layout(&self, pass: Pass) -> &BindGroupLayout {
        &self.textures[pass as usize]
    }

    pub fn get(&self, pass: Pass, state: u32) -> &RenderPipeline {
        &self.cache[&(pass, state)]
    }

    pub fn ensure(&mut self, device: &Device, pass: Pass, state: u32) {
        if self.cache.contains_key(&(pass, state)) {
            return;
        }
        let layout = device.create_pipeline_layout(&PipelineLayoutDescriptor {
            label: None,
            bind_group_layouts: &[Some(&self.uniform), Some(&self.textures[pass as usize])],
            immediate_size: 0,
        });
        let (vs, fs) = (module(device, &format!("{}.vert", pass.shader())), module(device, &format!("{}.frag", pass.shader())));
        let mut buffers = vec![VertexBufferLayout { array_stride: pass.stride(), step_mode: VertexStepMode::Vertex, attributes: pass.attributes() }];
        buffers.extend(pass.instance_attributes().map(|attributes| VertexBufferLayout { array_stride: INSTANCE_STRIDE, step_mode: VertexStepMode::Instance, attributes }));
        let test = state & 1 != 0;
        let target = Some(ColorTargetState { format: self.format, blend: blend(state >> 2 & 3), write_mask: ColorWrites::ALL });
        let targets: &[Option<ColorTargetState>] = if pass.offscreen() { &[] } else { std::slice::from_ref(&target) };
        let pipeline = device.create_render_pipeline(&RenderPipelineDescriptor {
            label: Some(pass.shader()),
            layout: Some(&layout),
            vertex: VertexState {
                module: &vs,
                entry_point: Some("main"),
                compilation_options: Default::default(),
                buffers: &buffers.iter().cloned().map(Some).collect::<Vec<_>>(),
            },
            primitive: PrimitiveState {
                cull_mode: [None, Some(Face::Back), Some(Face::Front), None][(state >> 4 & 3) as usize],
                front_face: if state & 64 != 0 { FrontFace::Cw } else { FrontFace::Ccw },
                ..Default::default()
            },
            depth_stencil: Some(DepthStencilState {
                format: DEPTH,
                depth_write_enabled: Some(test && state & 2 != 0),
                depth_compare: Some(if test { compare(state >> 7) } else { CompareFunction::Always }),
                stencil: Default::default(),
                bias: Default::default(),
            }),
            multisample: MultisampleState::default(),
            fragment: Some(FragmentState { module: &fs, entry_point: Some("main"), compilation_options: Default::default(), targets }),
            multiview_mask: None,
            cache: None,
        });
        self.cache.insert((pass, state), pipeline);
    }
}
