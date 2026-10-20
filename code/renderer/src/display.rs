use crate::{gpu::Gpu, renderer::Renderer, frame::Frame};
use wgpu::*;

pub struct Display {
    surface: Surface<'static>,
    config: SurfaceConfiguration,
}

impl Display {
    pub fn new(gpu: &Gpu, surface: Surface<'static>, width: u32, height: u32) -> Self {
        let caps = surface.get_capabilities(&gpu.adapter);
        let format = caps.formats.iter().copied().find(|f| !f.is_srgb()).unwrap_or(caps.formats[0]);
        let config = SurfaceConfiguration {
            usage: TextureUsages::RENDER_ATTACHMENT | (caps.usages & TextureUsages::COPY_SRC),
            format,
            width: width.max(1),
            height: height.max(1),
            present_mode: PresentMode::Fifo,
            desired_maximum_frame_latency: 2,
            alpha_mode: caps.alpha_modes[0],
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
        let (CurrentSurfaceTexture::Success(texture) | CurrentSurfaceTexture::Suboptimal(texture)) = self.surface.get_current_texture() else {
            self.surface.configure(&renderer.gpu.device, &self.config);
            return;
        };
        renderer.render(frame, &texture.texture, self.config.width, self.config.height);
        renderer.gpu.queue.present(texture);
    }
}
