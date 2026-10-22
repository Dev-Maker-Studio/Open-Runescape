use crate::{frame::Frame, gpu::Gpu, renderer::Renderer};
use wgpu::*;

pub struct Display {
    surface: Surface<'static>,
    config: SurfaceConfiguration,
}

impl Display {
    pub fn new(gpu: &Gpu, surface: Surface<'static>, width: u32, height: u32) -> Self {
        let caps = surface.get_capabilities(&gpu.adapter);
        let format = caps
            .formats
            .iter()
            .copied()
            .find(|f| !f.is_srgb())
            .unwrap_or(caps.formats[0]);
        let config = SurfaceConfiguration {
            usage: TextureUsages::RENDER_ATTACHMENT
                | if cfg!(target_arch = "wasm32") {
                    TextureUsages::COPY_SRC
                } else {
                    caps.usages & TextureUsages::COPY_SRC
                },
            format,
            width: width.max(1),
            height: height.max(1),
            present_mode: PresentMode::Fifo,
            desired_maximum_frame_latency: 2,
            alpha_mode: if caps.alpha_modes.contains(&CompositeAlphaMode::Opaque) {
                CompositeAlphaMode::Opaque
            } else {
                caps.alpha_modes[0]
            },
            view_formats: vec![],
            color_space: Default::default(),
        };
        surface.configure(&gpu.device, &config);
        Display { surface, config }
    }

    pub fn format(&self) -> TextureFormat {
        self.config.format
    }

    pub fn size(&self) -> (u32, u32) {
        (self.config.width, self.config.height)
    }

    pub fn resize(&mut self, gpu: &Gpu, width: u32, height: u32) {
        if (width, height) != self.size() && width > 0 && height > 0 {
            self.config.width = width;
            self.config.height = height;
            self.surface.configure(&gpu.device, &self.config);
        }
    }

    pub fn present(&mut self, renderer: &mut Renderer, frame: &Frame) {
        self.present_into(renderer, frame, false);
    }

    pub fn present_into(
        &mut self,
        renderer: &mut Renderer,
        frame: &Frame,
        readback: bool,
    ) -> Option<(Buffer, u32)> {
        let (CurrentSurfaceTexture::Success(texture) | CurrentSurfaceTexture::Suboptimal(texture)) =
            self.surface.get_current_texture()
        else {
            self.surface.configure(&renderer.gpu.device, &self.config);
            return None;
        };
        let (width, height) = self.size();
        renderer.render(frame, &texture.texture, width, height);
        let out = readback.then(|| copy(&renderer.gpu, &texture.texture, width, height));
        renderer.gpu.queue.present(texture);
        out
    }
}

pub fn copy(gpu: &Gpu, texture: &Texture, width: u32, height: u32) -> (Buffer, u32) {
    let row = (width * 4).next_multiple_of(COPY_BYTES_PER_ROW_ALIGNMENT);
    let buffer = gpu.device.create_buffer(&BufferDescriptor {
        label: None,
        size: u64::from(row * height),
        usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let mut encoder = gpu.device.create_command_encoder(&Default::default());
    encoder.copy_texture_to_buffer(
        texture.as_image_copy(),
        TexelCopyBufferInfo {
            buffer: &buffer,
            layout: TexelCopyBufferLayout {
                offset: 0,
                bytes_per_row: Some(row),
                rows_per_image: Some(height),
            },
        },
        Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
    );
    gpu.queue.submit([encoder.finish()]);
    (buffer, row)
}
