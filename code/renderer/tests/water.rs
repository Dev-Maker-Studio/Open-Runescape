use bytemuck::{Zeroable, bytes_of, pod_collect_to_vec};
use renderer::frame::TextureFormat as RecordedTextureFormat;
use renderer::{capture, math, shaders, uniforms::HdWaterUniform};
use wgpu::*;

#[test]
fn reflection_projection_matches_the_scene_depth_range() {
    let Some(renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    let device = &renderer.gpu.device;
    let source = format!(
        "{}\n@group(2) @binding(0) var<storage, read_write> depths: array<f32, 5>;\n\
        @compute @workgroup_size(1) fn projection_probe() {{\n\
        var screen: vec3<f32>;\n\
        let near = project(vec3<f32>(0.0, 0.0, -1.0), &screen); depths[0] = screen.z;\n\
        let middle = project(vec3<f32>(0.0, 0.0, -2.0), &screen); depths[1] = screen.z;\n\
        let far = project(vec3<f32>(0.0, 0.0, -10.0), &screen); depths[2] = screen.z;\n\
        let planar = planarUv(vec2<f32>(25.0, 20.0), vec2<f32>(0.3, 0.4)); depths[3] = planar.x; depths[4] = planar.y;\n\
        }}",
        shaders::get("hdWater.frag")
    );
    let shader = device.create_shader_module(ShaderModuleDescriptor {
        label: None,
        source: ShaderSource::Wgsl(source.into()),
    });
    let pipeline = device.create_compute_pipeline(&ComputePipelineDescriptor {
        label: None,
        layout: None,
        module: &shader,
        entry_point: Some("projection_probe"),
        compilation_options: Default::default(),
        cache: None,
    });
    let uniform = device.create_buffer(&BufferDescriptor {
        label: None,
        size: size_of::<HdWaterUniform>() as u64,
        usage: BufferUsages::UNIFORM | BufferUsages::COPY_DST,
        mapped_at_creation: false,
    });
    let block = HdWaterUniform {
        vp: math::perspective(1., 1., 1., 10.),
        viewport: [0., 0., 64., 64.],
        framebuffer_size: [100., 80.],
        planar_distort: 0.1,
        ..Zeroable::zeroed()
    };
    renderer
        .gpu
        .queue
        .write_buffer(&uniform, 0, bytes_of(&block));
    let output = device.create_buffer(&BufferDescriptor {
        label: None,
        size: 20,
        usage: BufferUsages::STORAGE | BufferUsages::COPY_SRC,
        mapped_at_creation: false,
    });
    let readback = device.create_buffer(&BufferDescriptor {
        label: None,
        size: 20,
        usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let groups = [
        device.create_bind_group(&BindGroupDescriptor {
            label: None,
            layout: &pipeline.get_bind_group_layout(0),
            entries: &[BindGroupEntry {
                binding: 0,
                resource: uniform.as_entire_binding(),
            }],
        }),
        device.create_bind_group(&BindGroupDescriptor {
            label: None,
            layout: &pipeline.get_bind_group_layout(1),
            entries: &[],
        }),
        device.create_bind_group(&BindGroupDescriptor {
            label: None,
            layout: &pipeline.get_bind_group_layout(2),
            entries: &[BindGroupEntry {
                binding: 0,
                resource: output.as_entire_binding(),
            }],
        }),
    ];
    let mut encoder = device.create_command_encoder(&Default::default());
    {
        let mut pass = encoder.begin_compute_pass(&Default::default());
        pass.set_pipeline(&pipeline);
        for (i, group) in groups.iter().enumerate() {
            pass.set_bind_group(i as u32, group, &[]);
        }
        pass.dispatch_workgroups(1, 1, 1);
    }
    encoder.copy_buffer_to_buffer(&output, 0, &readback, 0, 20);
    renderer.gpu.queue.submit([encoder.finish()]);
    readback.map_async(MapMode::Read, .., |r| r.unwrap());
    device.poll(PollType::wait_indefinitely()).unwrap();
    let depths: Vec<f32> = pod_collect_to_vec(&readback.get_mapped_range(..).unwrap());
    for (actual, expected) in depths.iter().zip([0., 5. / 9., 1., 0.28, 0.71]) {
        assert!(
            (actual - expected).abs() < 1e-6,
            "projected depth {actual} != {expected}"
        );
    }
}

#[test]
fn recorded_tessellation_size_controls_patch_density_and_keeps_legacy_default() {
    use renderer::{pass::WaterVertex, water};
    let vertices = [
        WaterVertex {
            p: [-1., -1., 0.],
            c: [0, 20, 40, 60],
            uv: [0., 1., 2.],
        },
        WaterVertex {
            p: [1., -1., 0.],
            c: [80, 100, 120, 140],
            uv: [3., 4., 5.],
        },
        WaterVertex {
            p: [-1., 1., 0.],
            c: [160, 180, 200, 220],
            uv: [6., 7., 8.],
        },
    ];
    let mut block = vec![0; 4432];
    assert_eq!(water::tess_pixels(&block), 24.);
    block[116..120].copy_from_slice(&12f32.to_le_bytes());
    assert_eq!(water::tess_pixels(&block), 12.);
    let fine = water::tessellate_with_pixels(
        &vertices,
        &[2, 0, 1],
        &math::IDENTITY,
        [0, 0, 96, 96],
        water::tess_pixels(&block),
    );
    let coarse = water::tessellate(&vertices, &[2, 0, 1], &math::IDENTITY, [0, 0, 96, 96]);
    assert!(fine.1.len() > coarse.1.len());
    assert_eq!(fine.0[0].p, vertices[2].p);
    assert_eq!(fine.0[0].uv, vertices[2].uv);
    assert_eq!(fine.0[0].c, vertices[2].c);
    for bad in [0f32, -1., f32::NAN, f32::INFINITY] {
        block[116..120].copy_from_slice(&bad.to_le_bytes());
        assert_eq!(water::tess_pixels(&block), 24.);
    }
}

#[test]
fn scene_copies_separate_terrain_depth_and_hd_color_depth_runs() {
    use renderer::{pass::Pass, water};
    let mut block = vec![0; 5552];
    block[water::TERRAIN_WATER_MODE..water::TERRAIN_WATER_MODE + 4]
        .copy_from_slice(&4u32.to_le_bytes());
    block[water::TERRAIN_DEPTH_FOG..water::TERRAIN_DEPTH_FOG + 4]
        .copy_from_slice(&1u32.to_le_bytes());
    let mut copies = water::SceneCopies::default();
    assert!(!copies.before(Pass::Instanced, &[]));
    assert!(copies.before(Pass::Terrain, &block));
    assert!(!copies.before(Pass::Terrain, &block));
    assert!(copies.before(Pass::HdWater, &[]));
    assert!(!copies.before(Pass::HdWater, &[]));
    assert!(!copies.before(Pass::Particle, &[]));
    assert!(copies.before(Pass::HdWater, &[]));
    assert!(copies.before(Pass::Terrain, &block));
    block[water::TERRAIN_DEPTH_FOG..water::TERRAIN_DEPTH_FOG + 4].fill(0);
    assert!(!copies.before(Pass::Terrain, &block));
}

#[test]
fn scene_attachment_copy_preserves_float_depth_and_color_in_command_order() {
    use renderer::targets::Targets;
    let renderer = pollster::block_on(capture::headless());
    let device = &renderer.gpu.device;
    let mut targets = Targets::new(capture::FORMAT);
    targets.ensure(device, 1, 1, 1);
    let scene = capture::target(device, 1, 1);
    let view = scene.create_view(&Default::default());
    let depth = 0.1234567f32;
    let mut encoder = device.create_command_encoder(&Default::default());
    {
        let _pass = encoder.begin_render_pass(&RenderPassDescriptor {
            color_attachments: &[Some(RenderPassColorAttachment {
                view: &view,
                depth_slice: None,
                resolve_target: None,
                ops: Operations {
                    load: LoadOp::Clear(Color {
                        r: 0.2,
                        g: 0.4,
                        b: 0.6,
                        a: 1.,
                    }),
                    store: StoreOp::Store,
                },
            })],
            depth_stencil_attachment: Some(RenderPassDepthStencilAttachment {
                view: targets.depth(),
                depth_ops: Some(Operations {
                    load: LoadOp::Clear(depth),
                    store: StoreOp::Store,
                }),
                stencil_ops: None,
            }),
            ..Default::default()
        });
    }
    targets.copy_scene(&mut encoder, &scene);
    let module = device.create_shader_module(ShaderModuleDescriptor {
        label: None,
        source: ShaderSource::Wgsl("@group(0) @binding(0) var depth: texture_depth_2d; @group(0) @binding(1) var color: texture_2d<f32>; @group(0) @binding(2) var<storage, read_write> out: array<f32, 5>; @compute @workgroup_size(1) fn probe() { out[0] = textureLoad(depth, vec2<i32>(0), 0); let c = textureLoad(color, vec2<i32>(0), 0); out[1] = c.r; out[2] = c.g; out[3] = c.b; out[4] = c.a; }".into()),
    });
    let pipeline = device.create_compute_pipeline(&ComputePipelineDescriptor {
        label: None,
        layout: None,
        module: &module,
        entry_point: Some("probe"),
        compilation_options: Default::default(),
        cache: None,
    });
    let output = device.create_buffer(&BufferDescriptor {
        label: None,
        size: 20,
        usage: BufferUsages::STORAGE | BufferUsages::COPY_SRC,
        mapped_at_creation: false,
    });
    let readback = device.create_buffer(&BufferDescriptor {
        label: None,
        size: 20,
        usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let views = targets.views();
    let group = device.create_bind_group(&BindGroupDescriptor {
        label: None,
        layout: &pipeline.get_bind_group_layout(0),
        entries: &[
            BindGroupEntry {
                binding: 0,
                resource: BindingResource::TextureView(views.scene_depth),
            },
            BindGroupEntry {
                binding: 1,
                resource: BindingResource::TextureView(views.scene_color),
            },
            BindGroupEntry {
                binding: 2,
                resource: output.as_entire_binding(),
            },
        ],
    });
    {
        let mut pass = encoder.begin_compute_pass(&Default::default());
        pass.set_pipeline(&pipeline);
        pass.set_bind_group(0, &group, &[]);
        pass.dispatch_workgroups(1, 1, 1);
    }
    encoder.copy_buffer_to_buffer(&output, 0, &readback, 0, 20);
    renderer.gpu.queue.submit([encoder.finish()]);
    readback.map_async(MapMode::Read, .., |r| r.unwrap());
    device.poll(PollType::wait_indefinitely()).unwrap();
    let values: Vec<f32> = pod_collect_to_vec(&readback.get_mapped_range(..).unwrap());
    assert_eq!(values[0].to_bits(), depth.to_bits());
    for (value, expected) in values[1..].iter().zip([0.2, 0.4, 0.6, 1.]) {
        assert!((value - expected).abs() < 1e-6);
    }
}

#[test]
fn layer_packed_wave_foam_cube_and_reflection_resources_render_without_fallback() {
    use renderer::{
        frame::{NO_TEXTURE, Texture},
        pass::Pass,
        scene,
    };
    let mut renderer = pollster::block_on(capture::headless());
    let mut frame = scene::build("water", 0, 64, 64).unwrap();
    let original = capture::render_rgba(&mut renderer, &frame);
    frame.textures.push(Texture {
        sampler: 9,
        format: RecordedTextureFormat::Rgba8,
        id: 70,
        width: 2,
        height: 2,
        layers: 6,
        rgba: (0..6)
            .flat_map(|face| [40 + face * 30, 20, 230, 255].repeat(4))
            .collect(),
    });
    frame.textures.push(Texture {
        sampler: 9,
        format: RecordedTextureFormat::Rgba8,
        id: 71,
        width: 2,
        height: 2,
        layers: 1,
        rgba: [220, 20, 10, 255].repeat(4),
    });
    let draw = frame
        .draws
        .iter_mut()
        .find(|d| d.pass == Pass::HdWater)
        .unwrap();
    draw.textures = vec![3, 3, 70, NO_TEXTURE, 71];
    let uniform = frame
        .uniforms
        .iter_mut()
        .find(|u| u.id == draw.uniform)
        .unwrap();
    for at in [96, 104, 108] {
        uniform.bytes[at..at + 4].copy_from_slice(&1u32.to_le_bytes());
    }
    uniform.bytes[116..120].copy_from_slice(&13f32.to_le_bytes());
    let actual = capture::render_rgba(&mut renderer, &frame);
    assert_eq!(actual.len(), original.len());
    assert_ne!(actual, original);
    assert!(frame.draws.iter().all(|d| d.skip.is_none()));
}
