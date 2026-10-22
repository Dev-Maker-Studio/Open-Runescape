use crate::{
    display::Display,
    frame::{self, Frame},
    gpu,
    renderer::Renderer,
    scene::{self, Source},
    stats,
    stream::Stream,
    terrain,
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
    stream: Stream,
    live: Option<Frame>,
    live_epoch: u32,
    layers: Vec<terrain::Layer>,
    scene_from: usize,
    bloom: Option<crate::frame::BloomParams>,
    orbit: crate::orbit::Orbit,
    scripted: crate::scripted_camera::ScriptedCamera,
    scene_base: (i32, i32),
    ground: Option<crate::orbit::SceneGround>,
    ground_plane: i32,
    scene_textures: std::collections::HashMap<i32, (i32, terrain::LayerTexture)>,
    ui: Option<crate::ui_overlay::Overlay>,
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
    f.draws
        .iter()
        .filter(|d| d.skip.is_none())
        .for_each(|d| *drawn.entry(d.pass.shader()).or_insert(0u32) += 1);
    let map = |m: BTreeMap<&str, u32>| {
        m.iter()
            .map(|(n, c)| format!("{n:?}:{c}"))
            .collect::<Vec<_>>()
            .join(",")
    };
    format!(
        "{{\"draws\":{},\"drawn\":{{{}}},\"skipped\":{{{}}}}}",
        f.draws.len(),
        map(drawn),
        map(f.skipped())
    )
}

#[wasm_bindgen]
impl App {
    pub async fn create(canvas: HtmlCanvasElement, scene: Option<String>) -> Result<App, JsValue> {
        let instance = gpu::instance();
        let surface = instance
            .create_surface(wgpu::SurfaceTarget::Canvas(canvas.clone()))
            .map_err(|e| e.to_string())?;
        let gpu = gpu::Gpu::new(&instance, Some(&surface)).await;
        gpu.device.on_uncaptured_error(std::sync::Arc::new(|e| {
            *GPU_ERROR.lock().unwrap() = e.to_string()
        }));
        let display = Display::new(&gpu, surface, canvas.width(), canvas.height());
        let renderer = Renderer::new(gpu, display.format());
        let source = Source::scene("all");
        let mut app = App {
            canvas,
            display,
            renderer,
            source,
            index: 0,
            loaded: String::new(),
            summary: String::new(),
            orf: None,
            size: (0, 0),
            stream: Stream::default(),
            live: None,
            live_epoch: 0,
            layers: Vec::new(),
            scene_from: usize::MAX,
            bloom: None,
            orbit: crate::orbit::Orbit::default(),
            scripted: crate::scripted_camera::ScriptedCamera::default(),
            scene_base: (0, 0),
            ground: None,
            ground_plane: 0,
            scene_textures: std::collections::HashMap::new(),
            ui: None,
        };
        app.scene(&scene.unwrap_or("all".into()))?;
        Ok(app)
    }

    pub fn scene(&mut self, name: &str) -> Result<(), JsValue> {
        let (w, h) = self.display.size();
        let first = scene::build(name, 0, w, h)
            .ok_or_else(|| format!("unknown scene {name}, scenes: {}", scene::NAMES.join(" ")))?;
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

    pub fn terrain_begin(&mut self) {
        self.layers.clear();
        self.scene_from = usize::MAX;
    }

    pub fn terrain_scene_texture(
        &mut self,
        material: i32,
        texture_scale: i32,
        texture_side: u32,
        rgba: Vec<u8>,
    ) -> Result<(), JsValue> {
        if material == terrain::UNTEXTURED_MATERIAL || texture_scale <= 0 || rgba.is_empty() {
            return Err("scene texture needs a material, a positive scale and pixels".into());
        }
        let texture = terrain::LayerTexture {
            width: texture_side,
            height: texture_side,
            rgba,
        };
        self.scene_textures
            .insert(material, (texture_scale, texture));
        Ok(())
    }

    pub fn terrain_scene_has_texture(&self, material: i32) -> bool {
        self.scene_textures.contains_key(&material)
    }

    pub fn terrain_scene(
        &mut self,
        positions: Vec<i32>,
        hsl: Vec<i16>,
        alphas: Vec<u8>,
        materials: Vec<i16>,
        rgb: Vec<u32>,
    ) -> Result<u32, JsValue> {
        self.push_scene(positions, hsl, alphas, materials, rgb, false)
    }

    pub fn terrain_scene_overlay(
        &mut self,
        positions: Vec<i32>,
        hsl: Vec<i16>,
        alphas: Vec<u8>,
        materials: Vec<i16>,
        rgb: Vec<u32>,
    ) -> Result<u32, JsValue> {
        self.push_scene(positions, hsl, alphas, materials, rgb, true)
    }

    fn push_scene(
        &mut self,
        positions: Vec<i32>,
        hsl: Vec<i16>,
        alphas: Vec<u8>,
        materials: Vec<i16>,
        rgb: Vec<u32>,
        overlay: bool,
    ) -> Result<u32, JsValue> {
        let batches = terrain::scene_batches(&positions, &hsl, &alphas, &materials, &rgb)?;
        if self.layers.len() + batches.len() > terrain::MAX_LAYERS {
            return Err("scene layers exceed the terrain layer limit".into());
        }
        self.scene_from = self.scene_from.min(self.layers.len());
        let count = batches.len() as u32;
        for batch in batches {
            let found = self.scene_textures.get(&batch.material);
            let (material, texture_scale, texture) = match found {
                Some((scale, texture)) => (batch.material, *scale, Some(texture.clone())),
                None => (terrain::UNTEXTURED_MATERIAL, -1, None),
            };
            self.layers.push(terrain::Layer {
                material,
                texture_scale,
                vertices: batch.vertices,
                indices: batch.indices,
                texture,
                overlay,
            });
        }
        Ok(count)
    }

    pub fn terrain_layer(
        &mut self,
        material: i32,
        texture_scale: i32,
        vertices: Vec<u8>,
        indices: Vec<u32>,
        texture_side: u32,
        rgba: Vec<u8>,
    ) -> Result<u32, JsValue> {
        let texture = (!rgba.is_empty()).then_some(terrain::LayerTexture {
            width: texture_side,
            height: texture_side,
            rgba,
        });
        self.layers.push(terrain::Layer {
            material,
            texture_scale,
            vertices,
            indices,
            texture,
            overlay: false,
        });
        Ok(self.layers.len() as u32)
    }

    pub fn terrain_show(
        &mut self,
        tile_x: i32,
        tile_z: i32,
        width: u32,
        height: u32,
    ) -> Result<String, JsValue> {
        let f = terrain::build_frame(&self.layers, [tile_x, tile_z], width, height)?;
        self.show_terrain(f)
    }

    #[allow(clippy::too_many_arguments)]
    pub fn terrain_show_live(
        &mut self,
        tile_x: i32,
        tile_z: i32,
        width: u32,
        height: u32,
        yaw: i32,
        roll: i32,
        pitch: i32,
        fog_enabled: bool,
        fog_color: i32,
        fog_depth: i32,
        fog_offset: i32,
        sky_argb: i32,
    ) -> Result<String, JsValue> {
        let ground_end = self.scene_from.min(self.layers.len());
        let ground = terrain::ground_at(&self.layers[..ground_end], [tile_x, tile_z])?;
        let view = terrain::View {
            camera: terrain::follow([tile_x, tile_z], ground, yaw, roll, pitch, height),
            environment: terrain::Environment {
                fog_enabled,
                fog_color,
                fog_depth,
                fog_offset,
                sky_argb,
            },
            bloom: self.bloom,
        };
        let f = terrain::build_frame_with(&self.layers, &view, width, height)?;
        self.show_terrain(f)
    }

    pub fn terrain_live_view_projection(
        &self,
        tile_x: i32,
        tile_z: i32,
        width: u32,
        height: u32,
        yaw: i32,
        roll: i32,
        pitch: i32,
    ) -> Result<Vec<f32>, JsValue> {
        let ground_end = self.scene_from.min(self.layers.len());
        let ground = terrain::ground_at(&self.layers[..ground_end], [tile_x, tile_z])?;
        let camera = terrain::follow([tile_x, tile_z], ground, yaw, roll, pitch, height);
        Ok(terrain::native_view_projection(&camera, width, height).to_vec())
    }

    pub fn set_bloom(
        &mut self,
        white_point: f32,
        bloom: f32,
        brightpass: f32,
    ) -> Result<(), JsValue> {
        let p = crate::frame::BloomParams {
            white_point,
            bloom,
            brightpass,
        };
        if !p.valid() {
            return Err(JsValue::from_str(
                "bloom needs finite parameters and a positive bloom",
            ));
        }
        self.bloom = Some(p);
        Ok(())
    }

    pub fn clear_bloom(&mut self) {
        self.bloom = None;
    }

    pub fn orbit_reset(&mut self, jitter_h: i32, jitter_v: i32, jitter_a: i32, angle_y: i32) {
        self.orbit = crate::orbit::Orbit::login(jitter_h, jitter_v, jitter_a, angle_y);
    }

    pub fn orbit_zoom(&mut self, rotation: i32) {
        self.orbit.wheel_zoom(rotation);
    }

    pub fn orbit_angle(&mut self, angle_x: i32, angle_y: i32) {
        self.orbit.angle_x = (angle_x << 3) as f32;
        self.orbit.angle_y = (angle_y << 3) as f32;
    }

    pub fn orbit_ground(
        &mut self,
        size: i32,
        plane: i32,
        heights: &[i32],
        bridges: &[u8],
    ) -> Result<(), JsValue> {
        if !(0..crate::orbit::PLANES as i32).contains(&plane) {
            return Err("orbit ground plane out of range".into());
        }
        self.ground = Some(crate::orbit::SceneGround::new(size, heights, bridges)?);
        self.ground_plane = plane;
        Ok(())
    }

    pub fn orbit_ground_lighting(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
        match self.ground.as_mut() {
            Some(ground) => Ok(ground.set_lighting(bytes)?),
            None => Err("orbit ground not set".into()),
        }
    }

    pub fn orbit_ground_clear(&mut self) {
        self.ground = None;
    }

    pub fn camera_base(&mut self, base_x: i32, base_y: i32) {
        self.scene_base = (base_x, base_y);
    }

    pub fn camera_spline(&mut self, path: usize, points: &[i32]) {
        if let Some(slot) = self.scripted.splines.get_mut(path) {
            *slot = if points.is_empty() {
                None
            } else {
                Some(
                    points
                        .chunks_exact(4)
                        .map(|p| [p[0], p[1], p[2], p[3]])
                        .collect(),
                )
            };
        }
    }

    pub fn camera_cutscene(
        &mut self,
        position_path: i32,
        position_keyframe: i32,
        speed_start: i32,
        speed_end: i32,
        look_path: i32,
        look_keyframe: i32,
    ) {
        self.scripted.start_cutscene(
            position_path,
            position_keyframe,
            speed_start,
            speed_end,
            look_path,
            look_keyframe,
        );
    }

    pub fn camera_apply(
        &mut self,
        kind: i32,
        a: i32,
        b: i32,
        c: i32,
        d: i32,
        e: i32,
    ) -> Result<(), JsValue> {
        let (ground, plane): (&dyn crate::orbit::Ground, i32) = match self.ground.as_ref() {
            Some(scene) => (scene, self.ground_plane),
            None => (
                &crate::orbit::FlatGround {
                    size_x: 104,
                    size_y: 104,
                    level: 0,
                },
                0,
            ),
        };
        match kind {
            crate::scripted_camera::COMMAND_FORCE_ANGLE => self
                .scripted
                .set_angle(&mut self.orbit, a, b, 0, ground, plane),
            crate::scripted_camera::COMMAND_SMOOTH_RESET => self.scripted.smooth_reset(),
            crate::scripted_camera::COMMAND_RESET => self.scripted.start_smooth_transition(),
            crate::scripted_camera::COMMAND_LOOK_AT => self.scripted.look_at(a, b, c, d, e, ground, plane),
            crate::scripted_camera::COMMAND_MOVE_TO => self
                .scripted
                .move_camera(a, b, c, d, e, true, ground, plane),
            crate::scripted_camera::COMMAND_SHAKE => self.scripted.shake(a as usize, b, c, d, e),
            crate::scripted_camera::COMMAND_TARGET => self.scripted.set_target(a, b),
            _ => return Err("unknown camera command".into()),
        }
        Ok(())
    }

    pub fn orbit_ground_live(&self) -> bool {
        self.ground.is_some()
    }

    #[allow(clippy::too_many_arguments)]
    pub fn terrain_show_orbit(
        &mut self,
        tile_x: i32,
        tile_z: i32,
        width: u32,
        height: u32,
        elapsed_ms: u32,
        cycles: u32,
        yaw_direction: i32,
        pitch_direction: i32,
        jitter_bits: u32,
        fog_enabled: bool,
        fog_color: i32,
        fog_depth: i32,
        fog_offset: i32,
        sky_argb: i32,
    ) -> Result<String, JsValue> {
        let ground = if self.ground.is_some() {
            0.0
        } else {
            let ground_end = self.scene_from.min(self.layers.len());
            terrain::ground_at(&self.layers[..ground_end], [tile_x, tile_z])?
        };
        let input = terrain::OrbitInput {
            tile: [tile_x, tile_z],
            ground,
            elapsed_ms: i64::from(elapsed_ms),
            cycles,
            yaw_direction,
            pitch_direction,
            jitter_bits: jitter_bits as i32,
        };
        let scene_ground = self.ground.as_ref();
        let flat = terrain::flat_ground(ground);
        let (ground_ref, plane): (&dyn crate::orbit::Ground, i32) = match scene_ground {
            Some(scene) => (scene, self.ground_plane),
            None => (&flat, 0),
        };
        let scripted = self.scripted.mode != crate::scripted_camera::MODE_FOLLOW
            || self.scripted.shakes.iter().any(|shake| shake.active);
        let camera = if scripted {
            let mut random = js_sys::Math::random;
            terrain::scripted_step(
                &mut self.orbit,
                &mut self.scripted,
                &input,
                self.scene_base,
                ground_ref,
                plane,
                height,
                &mut random,
            )
        } else {
            for _ in 0..cycles {
                self.scripted.advance_cycle();
            }
            terrain::orbit_step(&mut self.orbit, &input, ground_ref, plane, height)
        };
        let view = terrain::View {
            camera,
            environment: terrain::Environment {
                fog_enabled,
                fog_color,
                fog_depth,
                fog_offset,
                sky_argb,
            },
            bloom: self.bloom,
        };
        let f = terrain::build_frame_with(&self.layers, &view, width, height)?;
        self.show_terrain(f)
    }

    #[allow(clippy::too_many_arguments)]
    pub fn set_ui_overlay(
        &mut self,
        width: u32,
        height: u32,
        rgba: Vec<u8>,
        game_x: u32,
        game_y: u32,
        game_width: u32,
        game_height: u32,
    ) -> Result<(), JsValue> {
        let game = (game_width > 0 && game_height > 0).then_some([game_x, game_y, game_width, game_height]);
        self.ui = Some(crate::ui_overlay::Overlay::new(width, height, rgba, game)?);
        Ok(())
    }

    pub fn clear_ui_overlay(&mut self) {
        self.ui = None;
    }

    pub fn ui_overlay_active(&self) -> bool {
        self.ui.is_some()
    }

    fn show_terrain(&mut self, mut f: Frame) -> Result<String, JsValue> {
        if let Some(overlay) = &self.ui {
            crate::ui_overlay::compose(&mut f, overlay);
        }
        self.summary = counts(&f);
        self.size = (f.width, f.height);
        self.source = Source::replay(f);
        self.loaded = "terrain".into();
        self.orf = None;
        self.live = None;
        self.index = 0;
        self.layers.clear();
        Ok(self.summary.clone())
    }

    fn render(&mut self, readback: bool) -> Option<(wgpu::Buffer, u32)> {
        if let Some(bytes) = PENDING.lock().unwrap().take() {
            self.replay(bytes, "replay_next".into()).ok()?;
        }
        if let Some(f) = self.stream.take() {
            if self.live.is_none() || self.live_epoch != self.stream.epoch {
                self.renderer.reset_stream_resources();
                self.live_epoch = self.stream.epoch;
            }
            self.live = Some(f);
        }
        let (cw, ch) = if self.size.0 > 0 {
            self.size
        } else {
            (self.canvas.width(), self.canvas.height())
        };
        self.display.resize(&self.renderer.gpu, cw, ch);
        let (w, h) = self.display.size();
        let frame = match self.live.as_ref() {
            Some(f) => f,
            None => self.source.next(self.index, w, h),
        };
        self.renderer.capture = FRAMES.load(Relaxed) == CAPTURE_AT.load(Relaxed);
        self.index += 1;
        FRAMES.fetch_add(1, Relaxed);
        let out = self
            .display
            .present_into(&mut self.renderer, frame, readback);
        if let Some(f) = self.live.as_mut() {
            f.meshes.clear();
            f.textures.clear();
            f.instances.clear();
        }
        out
    }

    pub fn push(&mut self, bytes: &[u8]) -> Result<Option<Vec<u8>>, JsValue> {
        self.stream.push(bytes).map_err(JsValue::from)
    }

    pub fn disconnect(&mut self) {
        let active = self.stream.active() || self.live.is_some();
        self.stream = Stream::default();
        self.live = None;
        self.live_epoch = 0;
        if active {
            self.renderer.reset_stream_resources();
            self.index = 0;
            if let Some(f) = self.orf.as_deref().and_then(frame::parse) {
                self.source = Source::replay(f);
            }
        }
    }

    pub fn shown(&self) -> u32 {
        self.stream.stats.shown
    }

    pub fn stats(&self) -> String {
        self.stream.json()
    }

    pub fn frame(&mut self) {
        self.render(false);
    }

    pub async fn bench(&mut self, n: u32) -> String {
        self.renderer.probe(true);
        let mut all = vec![];
        for _ in 0..n {
            let t = stats::now();
            self.render(false);
            self.renderer.finish().await;
            self.renderer.stats.wall = stats::now() - t;
            all.push(self.renderer.stats);
        }
        self.renderer.probe(false);
        stats::report(&all, &format!("{} {}", self.loaded, self.summary))
    }

    pub fn capture(&mut self) -> Option<Readback> {
        let (buffer, row) = self.render(true)?;
        let (width, height) = self.display.size();
        let bgra = matches!(
            self.display.format(),
            wgpu::TextureFormat::Bgra8Unorm | wgpu::TextureFormat::Bgra8UnormSrgb
        );
        Some(Readback {
            buffer,
            row,
            width,
            height,
            bgra,
        })
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
        let (loaded, summary) = match &self.live {
            Some(f) => ("stream".to_string(), counts(f)),
            None => (self.loaded.clone(), self.summary.clone()),
        };
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
            loaded,
            self.size.0,
            self.size.1,
            summary,
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
        let data = self
            .buffer
            .get_mapped_range(..)
            .map_err(|e| e.to_string())?;
        let mut out = Vec::with_capacity((self.width * self.height * 4) as usize);
        for row in data.chunks(self.row as usize).take(self.height as usize) {
            for p in row[..(self.width * 4) as usize].chunks_exact(4) {
                out.extend(if self.bgra {
                    [p[2], p[1], p[0], 255]
                } else {
                    [p[0], p[1], p[2], 255]
                });
            }
        }
        Ok(out)
    }
}
