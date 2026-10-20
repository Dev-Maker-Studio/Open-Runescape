use crate::{frame::Frame, gpu::Gpu, renderer::Renderer};
use wgpu::*;

pub const FORMAT: TextureFormat = TextureFormat::Rgba8Unorm;

pub async fn headless() -> Renderer {
    try_headless().await.expect("no adapter or device")
}

pub async fn try_headless() -> Option<Renderer> {
    let instance = crate::gpu::instance();
    Some(Renderer::new(Gpu::try_new(&instance, None).await?, FORMAT))
}

pub fn render_rgba(renderer: &mut Renderer, frame: &Frame) -> Vec<u8> {
    let (width, height) = (frame.width, frame.height);
    let device = &renderer.gpu.device;
    let target = device.create_texture(&TextureDescriptor {
        label: Some("offscreen"),
        size: Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: TextureDimension::D2,
        format: FORMAT,
        usage: TextureUsages::RENDER_ATTACHMENT | TextureUsages::COPY_SRC,
        view_formats: &[],
    });
    renderer.render(frame, &target, width, height);
    let device = &renderer.gpu.device;
    let row = (width * 4).next_multiple_of(COPY_BYTES_PER_ROW_ALIGNMENT);
    let buffer = device.create_buffer(&BufferDescriptor {
        label: None,
        size: (row * height) as u64,
        usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let mut encoder = device.create_command_encoder(&Default::default());
    encoder.copy_texture_to_buffer(
        target.as_image_copy(),
        TexelCopyBufferInfo { buffer: &buffer, layout: TexelCopyBufferLayout { offset: 0, bytes_per_row: Some(row), rows_per_image: Some(height) } },
        Extent3d { width, height, depth_or_array_layers: 1 },
    );
    renderer.gpu.queue.submit([encoder.finish()]);
    buffer.map_async(MapMode::Read, .., |r| r.unwrap());
    renderer.gpu.device.poll(PollType::wait_indefinitely()).unwrap();
    let data = buffer.get_mapped_range(..).unwrap();
    data.chunks(row as usize).flat_map(|r| &r[..(width * 4) as usize]).copied().collect()
}
