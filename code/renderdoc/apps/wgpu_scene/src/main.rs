use std::sync::Arc;
use wgpu::util::DeviceExt;
use winit::{application::ApplicationHandler, dpi::PhysicalSize, event::WindowEvent, event_loop::{ActiveEventLoop, EventLoop}, window::{Window, WindowId}};

const SHADER: &str = "
struct U { tint: vec4<f32>, scale: f32 }
@group(0) @binding(0) var<uniform> u: U;
struct Out { @builtin(position) pos: vec4<f32>, @location(0) v: vec4<f32> }
@vertex fn vs(@location(0) p: vec2<f32>, @location(1) c: vec3<f32>, @builtin(instance_index) i: u32) -> Out {
    var o: Out;
    o.v = vec4<f32>(c, 1.0) * u.tint;
    o.pos = vec4<f32>(p * u.scale + vec2<f32>(f32(i) * 0.5 - 0.25, 0.0), 0.0, 1.0);
    return o;
}
@fragment fn fs(i: Out) -> @location(0) vec4<f32> { return i.v; }
";

const TRI: [f32; 15] = [-0.5, -0.5, 1.0, 0.0, 0.0, 0.5, -0.5, 0.0, 1.0, 0.0, 0.0, 0.5, 0.0, 0.0, 1.0];
const QUAD: [f32; 20] = [-0.25, -0.25, 1.0, 1.0, 0.0, 0.25, -0.25, 0.0, 1.0, 1.0, 0.25, 0.25, 1.0, 0.0, 1.0, -0.25, 0.25, 1.0, 1.0, 1.0];
const IDX: [u16; 6] = [0, 1, 2, 0, 2, 3];

struct Gpu {
    window: Arc<Window>,
    surface: wgpu::Surface<'static>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    opaque: wgpu::RenderPipeline,
    blended: wgpu::RenderPipeline,
    tri: wgpu::Buffer,
    quad: wgpu::Buffer,
    idx: wgpu::Buffer,
    groups: [wgpu::BindGroup; 2],
}

struct App {
    gpu: Option<Gpu>,
    frames: u32,
    shown: u32,
}

fn uniform(device: &wgpu::Device, layout: &wgpu::BindGroupLayout, tint: [f32; 4], scale: f32) -> wgpu::BindGroup {
    let data: [f32; 8] = [tint[0], tint[1], tint[2], tint[3], scale, 0.0, 0.0, 0.0];
    let buf = device.create_buffer_init(&wgpu::util::BufferInitDescriptor { label: None, contents: bytemuck::cast_slice(&data), usage: wgpu::BufferUsages::UNIFORM });
    device.create_bind_group(&wgpu::BindGroupDescriptor { label: None, layout, entries: &[wgpu::BindGroupEntry { binding: 0, resource: buf.as_entire_binding() }] })
}

impl Gpu {
    fn new(window: Arc<Window>) -> Self {
        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor::new_without_display_handle());
        let surface = instance.create_surface(window.clone()).unwrap();
        let adapter = pollster::block_on(instance.request_adapter(&wgpu::RequestAdapterOptions { compatible_surface: Some(&surface), ..Default::default() })).unwrap();
        let (device, queue) = pollster::block_on(adapter.request_device(&wgpu::DeviceDescriptor::default())).unwrap();
        let size = window.inner_size();
        let caps = surface.get_capabilities(&adapter);
        let format = caps.formats.iter().copied().find(|f| !f.is_srgb()).unwrap_or(caps.formats[0]);
        surface.configure(&device, &wgpu::SurfaceConfiguration { usage: wgpu::TextureUsages::RENDER_ATTACHMENT, format, width: size.width, height: size.height, present_mode: wgpu::PresentMode::Fifo, alpha_mode: caps.alpha_modes[0], view_formats: vec![], desired_maximum_frame_latency: 2, color_space: Default::default() });
        let module = device.create_shader_module(wgpu::ShaderModuleDescriptor { label: None, source: wgpu::ShaderSource::Wgsl(SHADER.into()) });
        let bgl = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor { label: None, entries: &[wgpu::BindGroupLayoutEntry { binding: 0, visibility: wgpu::ShaderStages::VERTEX, ty: wgpu::BindingType::Buffer { ty: wgpu::BufferBindingType::Uniform, has_dynamic_offset: false, min_binding_size: None }, count: None }] });
        let layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor { label: None, bind_group_layouts: &[Some(&bgl)], immediate_size: 0 });
        let attrs = wgpu::vertex_attr_array![0 => Float32x2, 1 => Float32x3];
        let pipeline = |blend: Option<wgpu::BlendState>| {
            device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
                label: None,
                layout: Some(&layout),
                vertex: wgpu::VertexState { module: &module, entry_point: Some("vs"), compilation_options: Default::default(), buffers: &[Some(wgpu::VertexBufferLayout { array_stride: 20, step_mode: wgpu::VertexStepMode::Vertex, attributes: &attrs })] },
                fragment: Some(wgpu::FragmentState { module: &module, entry_point: Some("fs"), compilation_options: Default::default(), targets: &[Some(wgpu::ColorTargetState { format, blend, write_mask: wgpu::ColorWrites::ALL })] }),
                primitive: wgpu::PrimitiveState::default(),
                depth_stencil: None,
                multisample: wgpu::MultisampleState::default(),
                multiview_mask: None,
                cache: None,
            })
        };
        let buf = |data: &[u8], usage| device.create_buffer_init(&wgpu::util::BufferInitDescriptor { label: None, contents: data, usage });
        Gpu {
            opaque: pipeline(None),
            blended: pipeline(Some(wgpu::BlendState::ALPHA_BLENDING)),
            tri: buf(bytemuck::cast_slice(&TRI), wgpu::BufferUsages::VERTEX),
            quad: buf(bytemuck::cast_slice(&QUAD), wgpu::BufferUsages::VERTEX),
            idx: buf(bytemuck::cast_slice(&IDX), wgpu::BufferUsages::INDEX),
            groups: [uniform(&device, &bgl, [1.0; 4], 1.0), uniform(&device, &bgl, [1.0, 1.0, 1.0, 0.5], 1.5)],
            window,
            surface,
            device,
            queue,
        }
    }

    fn frame(&self) {
        let (wgpu::CurrentSurfaceTexture::Success(frame) | wgpu::CurrentSurfaceTexture::Suboptimal(frame)) = self.surface.get_current_texture() else { return };
        let view = frame.texture.create_view(&Default::default());
        let mut enc = self.device.create_command_encoder(&Default::default());
        {
            let mut pass = enc.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: None,
                color_attachments: &[Some(wgpu::RenderPassColorAttachment { view: &view, depth_slice: None, resolve_target: None, ops: wgpu::Operations { load: wgpu::LoadOp::Clear(wgpu::Color { r: 0.1, g: 0.2, b: 0.3, a: 1.0 }), store: wgpu::StoreOp::Store } })],
                depth_stencil_attachment: None,
                timestamp_writes: None,
                occlusion_query_set: None,
                multiview_mask: None,
            });
            pass.set_pipeline(&self.opaque);
            pass.set_bind_group(0, &self.groups[0], &[]);
            pass.set_vertex_buffer(0, self.tri.slice(..));
            pass.draw(0..3, 0..1);
            pass.set_pipeline(&self.blended);
            pass.set_bind_group(0, &self.groups[1], &[]);
            pass.set_vertex_buffer(0, self.quad.slice(..));
            pass.set_index_buffer(self.idx.slice(..), wgpu::IndexFormat::Uint16);
            pass.draw_indexed(0..6, 0, 0..2);
        }
        self.queue.submit([enc.finish()]);
        self.window.pre_present_notify();
        self.queue.present(frame);
    }
}

impl ApplicationHandler for App {
    fn resumed(&mut self, el: &ActiveEventLoop) {
        let window = Arc::new(el.create_window(Window::default_attributes().with_title("wgpu_scene").with_inner_size(PhysicalSize::new(256, 256)).with_resizable(false)).unwrap());
        self.gpu = Some(Gpu::new(window));
    }

    fn window_event(&mut self, el: &ActiveEventLoop, _: WindowId, event: WindowEvent) {
        match event {
            WindowEvent::CloseRequested => el.exit(),
            WindowEvent::RedrawRequested => {
                let gpu = self.gpu.as_ref().unwrap();
                gpu.frame();
                self.shown += 1;
                if self.shown >= self.frames {
                    el.exit();
                }
                gpu.window.request_redraw();
            }
            _ => {}
        }
    }
}

fn main() {
    let frames = std::env::args().nth(1).and_then(|a| a.parse().ok()).unwrap_or(600);
    EventLoop::new().unwrap().run_app(&mut App { gpu: None, frames, shown: 0 }).unwrap();
}
