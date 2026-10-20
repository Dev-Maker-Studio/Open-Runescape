use renderer::{display::Display, gpu, renderer::Renderer, scene::Source};
use std::sync::Arc;
use wgpu::Instance;
use winit::{
    application::ApplicationHandler,
    dpi::PhysicalSize,
    event::WindowEvent,
    event_loop::{ActiveEventLoop, EventLoop},
    window::{Window, WindowId},
};

struct State {
    _instance: Instance,
    window: Arc<Window>,
    display: Display,
    renderer: Renderer,
}

struct App {
    source: Source,
    size: (u32, u32),
    limit: u32,
    index: u32,
    state: Option<State>,
}

impl ApplicationHandler for App {
    fn resumed(&mut self, el: &ActiveEventLoop) {
        let attributes = Window::default_attributes().with_title("openrs renderer").with_inner_size(PhysicalSize::new(self.size.0, self.size.1));
        let window = Arc::new(el.create_window(attributes).unwrap());
        let instance = gpu::instance();
        let surface = instance.create_surface(window.clone()).unwrap();
        let gpu = pollster::block_on(gpu::Gpu::new(&instance, Some(&surface)));
        let size = window.inner_size();
        let display = Display::new(&gpu, surface, size.width, size.height);
        let renderer = Renderer::new(gpu, display.format());
        window.request_redraw();
        self.state = Some(State { _instance: instance, window, display, renderer });
    }

    fn window_event(&mut self, el: &ActiveEventLoop, _: WindowId, event: WindowEvent) {
        let Some(s) = &mut self.state else { return };
        match event {
            WindowEvent::CloseRequested => el.exit(),
            WindowEvent::Resized(size) => s.display.resize(&s.renderer.gpu, size.width, size.height),
            WindowEvent::RedrawRequested => {
                let (w, h) = s.display.size();
                let frame = self.source.next(self.index, w, h);
                s.display.present(&mut s.renderer, &frame);
                self.index += 1;
                if self.index >= self.limit {
                    el.exit();
                } else {
                    s.window.request_redraw();
                }
            }
            _ => {}
        }
    }
}

pub fn run(source: Source, size: (u32, u32), limit: u32) {
    let mut app = App { source, size, limit, index: 0, state: None };
    EventLoop::new().unwrap().run_app(&mut app).unwrap();
}
