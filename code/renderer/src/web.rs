use crate::{
    display::Display,
    frame::{self, Frame},
    gpu,
    renderer::Renderer,
    scene::{self, Source},
};
use std::{
    collections::BTreeMap,
    sync::{
        Mutex,
        atomic::{AtomicU32, Ordering::Relaxed},
    },
};
use wasm_bindgen::prelude::*;
use web_sys::HtmlCanvasElement;

static GPU_ERROR: Mutex<String> = Mutex::new(String::new());
static FRAMES: AtomicU32 = AtomicU32::new(0);
static CAPTURE_AT: AtomicU32 = AtomicU32::new(u32::MAX);
static PENDING: Mutex<Option<Vec<u8>>> = Mutex::new(None);

#[wasm_bindgen]
pub fn frames() -> u32 {
    FRAMES.load(Relaxed)
}

#[wasm_bindgen]
pub fn capture_at(frame: u32) -> u32 {
    let at = frame.max(frames());
    CAPTURE_AT.store(at, Relaxed);
    at
}

#[wasm_bindgen]
pub fn replay_next(bytes: Vec<u8>) -> Result<(), JsValue> {
    frame::parse(&bytes).ok_or("invalid frame file")?;
    *PENDING.lock().unwrap() = Some(bytes);
    Ok(())
}

#[wasm_bindgen]
pub struct App {
    canvas: HtmlCanvasElement,
    display: Display,
    renderer: Renderer,
    source: Source,
    index: u32,
    loaded: String,
    summary: String,
    orf: Option<Vec<u8>>,
    size: (u32, u32),
}

#[wasm_bindgen]
pub struct Readback {
    buffer: wgpu::Buffer,
    row: u32,
    width: u32,
    height: u32,
    bgra: bool,
}

fn counts(f: &Frame) -> String {
    let mut drawn = BTreeMap::new();
    f.draws.iter().filter(|d| d.skip.is_none()).for_each(|d| *drawn.entry(d.pass.shader()).or_insert(0u32) += 1);
    let map = |m: BTreeMap<&str, u32>| m.iter().map(|(n, c)| format!("{n:?}:{c}")).collect::<Vec<_>>().join(",");
    format!("{{\"draws\":{},\"drawn\":{{{}}},\"skipped\":{{{}}}}}", f.draws.len(), map(drawn), map(f.skipped()))
}

#[wasm_bindgen]
impl App {
    pub async fn create(canvas: HtmlCanvasElement, scene: Option<String>) -> Result<App, JsValue> {
        let instance = gpu::instance();
        let surface = instance.create_surface(wgpu::SurfaceTarget::Canvas(canvas.clone())).map_err(|e| e.to_string())?;
        let gpu = gpu::Gpu::new(&instance, Some(&surface)).await;
        gpu.device.on_uncaptured_error(std::sync::Arc::new(|e| *GPU_ERROR.lock().unwrap() = e.to_string()));
        let display = Display::new(&gpu, surface, canvas.width(), canvas.height());
        let renderer = Renderer::new(gpu, display.format());
        let source = Source::scene("all");
        let mut app = App { canvas, display, renderer, source, index: 0, loaded: String::new(), summary: String::new(), orf: None, size: (0, 0) };
        app.scene(&scene.unwrap_or("all".into()))?;
        Ok(app)
    }

    pub fn scene(&mut self, name: &str) -> Result<(), JsValue> {
        let (w, h) = self.display.size();
        let first = scene::build(name, 0, w, h).ok_or_else(|| format!("unknown scene {name}, scenes: {}", scene::NAMES.join(" ")))?;
        self.summary = counts(&first);
        self.source = Source::scene(name);
        self.loaded = format!("scene:{name}");
        self.orf = None;
        self.size = (0, 0);
        Ok(())
    }

    pub fn replay(&mut self, bytes: Vec<u8>, label: String) -> Result<(), JsValue> {
        let f = frame::parse(&bytes).ok_or("invalid frame file")?;
        self.summary = counts(&f);
        self.size = (f.width, f.height);
        self.source = Source::replay(f);
        self.loaded = label;
        self.orf = Some(bytes);
        Ok(())
    }

    fn render(&mut self, readback: bool) -> Option<(wgpu::Buffer, u32)> {
        if let Some(bytes) = PENDING.lock().unwrap().take() {
            self.replay(bytes, "replay_next".into()).ok()?;
        }
        let (cw, ch) = if self.size.0 > 0 { self.size } else { (self.canvas.width(), self.canvas.height()) };
        self.display.resize(&self.renderer.gpu, cw, ch);
        let (w, h) = self.display.size();
        let frame = self.source.next(self.index, w, h);
        self.renderer.capture = FRAMES.load(Relaxed) == CAPTURE_AT.load(Relaxed);
        self.index += 1;
        FRAMES.fetch_add(1, Relaxed);
        self.display.present_into(&mut self.renderer, &frame, readback)
    }

    pub fn frame(&mut self) {
        self.render(false);
    }

    pub fn capture(&mut self) -> Option<Readback> {
        let (buffer, row) = self.render(true)?;
        let (width, height) = self.display.size();
        let bgra = matches!(self.display.format(), wgpu::TextureFormat::Bgra8Unorm | wgpu::TextureFormat::Bgra8UnormSrgb);
        Some(Readback { buffer, row, width, height, bgra })
    }

    pub fn dump(&self) -> Vec<u8> {
        if let Some(bytes) = &self.orf {
            return bytes.clone();
        }
        let name = self.loaded.trim_start_matches("scene:");
        let (w, h) = self.display.size();
        let mut f = scene::build(name, self.index, w, h).unwrap_or_default();
        if let Some(first) = scene::build(name, 0, w, h).filter(|_| self.index > 0) {
            f.meshes = first.meshes;
            f.textures = first.textures;
            f.instances = first.instances;
        }
        frame::write(&f)
    }

    pub fn info(&self) -> String {
        let a = self.renderer.gpu.adapter.get_info();
        let (w, h) = self.display.size();
        format!(
            "{{\"backend\":\"{:?}\",\"adapter\":{{\"name\":{:?},\"vendor\":{},\"device\":{},\"type\":\"{:?}\",\"driver\":{:?},\"driverInfo\":{:?}}},\"format\":\"{:?}\",\"frames\":{},\"loaded\":{:?},\"surface\":[{w},{h}],\"frameSize\":[{},{}],\"summary\":{},\"gpuError\":{:?}}}",
            a.backend,
            a.name,
            a.vendor,
            a.device,
            a.device_type,
            a.driver,
            a.driver_info,
            self.display.format(),
            self.index,
            self.loaded,
            self.size.0,
            self.size.1,
            self.summary,
            GPU_ERROR.lock().unwrap().as_str()
        )
    }
}

#[wasm_bindgen]
impl Readback {
    #[wasm_bindgen(getter)]
    pub fn width(&self) -> u32 {
        self.width
    }

    #[wasm_bindgen(getter)]
    pub fn height(&self) -> u32 {
        self.height
    }

    pub async fn read(self) -> Result<Vec<u8>, JsValue> {
        let buffer = self.buffer.clone();
        let promise = js_sys::Promise::new(&mut |resolve, reject| {
            buffer.map_async(wgpu::MapMode::Read, .., move |r| {
                let _ = match r {
                    Ok(()) => resolve.call0(&JsValue::NULL),
                    Err(e) => reject.call1(&JsValue::NULL, &e.to_string().into()),
                };
            });
        });
        wasm_bindgen_futures::JsFuture::from(promise).await?;
        let data = self.buffer.get_mapped_range(..).map_err(|e| e.to_string())?;
        let mut out = Vec::with_capacity((self.width * self.height * 4) as usize);
        for row in data.chunks(self.row as usize).take(self.height as usize) {
            for p in row[..(self.width * 4) as usize].chunks_exact(4) {
                out.extend(if self.bgra { [p[2], p[1], p[0], 255] } else { [p[0], p[1], p[2], 255] });
            }
        }
        Ok(out)
    }
}
