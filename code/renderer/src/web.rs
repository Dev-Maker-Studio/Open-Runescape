use crate::{display::Display, gpu, renderer::Renderer, scene::Source};
use wasm_bindgen::prelude::*;
use web_sys::HtmlCanvasElement;

#[wasm_bindgen]
pub struct App {
    canvas: HtmlCanvasElement,
    display: Display,
    renderer: Renderer,
    source: Source,
    index: u32,
}

#[wasm_bindgen]
impl App {
    pub async fn create(canvas: HtmlCanvasElement, scene: Option<String>) -> Result<App, JsValue> {
        let instance = gpu::instance();
        let surface = instance.create_surface(wgpu::SurfaceTarget::Canvas(canvas.clone())).map_err(|e| e.to_string())?;
        let gpu = gpu::Gpu::new(&instance, Some(&surface)).await;
        let display = Display::new(&gpu, surface, canvas.width(), canvas.height());
        let renderer = Renderer::new(gpu, display.format());
        Ok(App { canvas, display, renderer, source: Source::scene(&scene.unwrap_or("all".into())), index: 0 })
    }

    pub fn frame(&mut self) {
        self.display.resize(&self.renderer.gpu, self.canvas.width(), self.canvas.height());
        let (w, h) = self.display.size();
        let frame = self.source.next(self.index, w, h);
        self.index += 1;
        self.display.present(&mut self.renderer, &frame);
    }
}
