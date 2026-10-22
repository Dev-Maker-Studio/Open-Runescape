use std::cell::Cell;
use wgpu::*;

pub const MAX_PASSES: u32 = 16;

#[derive(Default, Clone, Copy, Debug)]
pub struct Stats {
    pub sync: f64,
    pub water: f64,
    pub groups: f64,
    pub encode: f64,
    pub submit: f64,
    pub gpu: f64,
    pub wall: f64,
    pub timestamps: bool,
    pub draws: u32,
    pub calls: u32,
    pub uploads: u32,
    pub bytes: u64,
}

impl Stats {
    pub fn cpu(&self) -> f64 {
        self.sync + self.water + self.groups + self.encode + self.submit
    }

    pub fn upload(&mut self, bytes: usize) {
        self.uploads += 1;
        self.bytes += bytes as u64;
    }
}

#[cfg(not(target_arch = "wasm32"))]
pub fn now() -> f64 {
    static START: std::sync::OnceLock<std::time::Instant> = std::sync::OnceLock::new();
    START
        .get_or_init(std::time::Instant::now)
        .elapsed()
        .as_secs_f64()
        * 1e3
}

#[cfg(target_arch = "wasm32")]
pub fn now() -> f64 {
    web_sys::window()
        .and_then(|w| w.performance())
        .map_or(0., |p| p.now())
}

pub struct Probe {
    pub set: QuerySet,
    resolve: Buffer,
    read: Buffer,
    passes: Cell<u32>,
    period: f64,
}

impl Probe {
    pub fn new(device: &Device, queue: &Queue) -> Option<Probe> {
        if !device.features().contains(Features::TIMESTAMP_QUERY) {
            return None;
        }
        let size = u64::from(MAX_PASSES) * 16;
        let set = device.create_query_set(&QuerySetDescriptor {
            label: None,
            ty: QueryType::Timestamp,
            count: MAX_PASSES * 2,
        });
        let resolve = device.create_buffer(&BufferDescriptor {
            label: None,
            size,
            usage: BufferUsages::QUERY_RESOLVE | BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });
        let read = device.create_buffer(&BufferDescriptor {
            label: None,
            size,
            usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });
        Some(Probe {
            set,
            resolve,
            read,
            passes: Cell::new(0),
            period: f64::from(queue.get_timestamp_period()),
        })
    }

    pub fn writes(&self) -> Option<RenderPassTimestampWrites<'_>> {
        let i = self.passes.get() * 2;
        if i >= MAX_PASSES * 2 {
            return None;
        }
        self.passes.set(i / 2 + 1);
        Some(RenderPassTimestampWrites {
            query_set: &self.set,
            beginning_of_pass_write_index: Some(i),
            end_of_pass_write_index: Some(i + 1),
        })
    }

    pub fn resolve(&self, encoder: &mut CommandEncoder) {
        let n = self.passes.get();
        if n > 0 {
            encoder.resolve_query_set(&self.set, 0..n * 2, &self.resolve, 0);
            encoder.copy_buffer_to_buffer(&self.resolve, 0, &self.read, 0, u64::from(n) * 16);
        }
    }

    pub async fn gpu_ms(&mut self, device: &Device) -> f64 {
        let n = self.passes.take() as usize * 2;
        if n == 0 {
            return 0.;
        }
        mapped(device, &self.read).await;
        let ms = {
            let data = self.read.get_mapped_range(..(n * 8) as u64).unwrap();
            let t: &[u64] = bytemuck::cast_slice(&data);
            let begin = t.iter().step_by(2).copied().min().unwrap_or(0);
            let end = t.iter().skip(1).step_by(2).copied().max().unwrap_or(0);
            end.saturating_sub(begin) as f64 * self.period / 1e6
        };
        self.read.unmap();
        ms
    }
}

#[cfg(not(target_arch = "wasm32"))]
pub async fn mapped(device: &Device, buffer: &Buffer) {
    buffer.map_async(MapMode::Read, .., |r| r.unwrap());
    device.poll(PollType::wait_indefinitely()).unwrap();
}

#[cfg(target_arch = "wasm32")]
pub async fn mapped(_: &Device, buffer: &Buffer) {
    let b = buffer.clone();
    let promise = js_sys::Promise::new(&mut |resolve, _| {
        b.map_async(MapMode::Read, .., move |r| {
            r.unwrap();
            let _ = resolve.call0(&wasm_bindgen::JsValue::NULL);
        });
    });
    let _ = wasm_bindgen_futures::JsFuture::from(promise).await;
}

#[cfg(not(target_arch = "wasm32"))]
pub async fn finished(device: &Device, _: &Queue) {
    device.poll(PollType::wait_indefinitely()).unwrap();
}

#[cfg(target_arch = "wasm32")]
pub async fn finished(_: &Device, queue: &Queue) {
    let promise = js_sys::Promise::new(&mut |resolve, _| {
        queue.on_submitted_work_done(move || {
            let _ = resolve.call0(&wasm_bindgen::JsValue::NULL);
        });
    });
    let _ = wasm_bindgen_futures::JsFuture::from(promise).await;
}

fn percentile(values: &mut [f64], p: f64) -> f64 {
    if values.is_empty() {
        return 0.;
    }
    values.sort_by(f64::total_cmp);
    values[((values.len() - 1) as f64 * p).round() as usize]
}

fn mb(bytes: u64) -> String {
    if bytes >= 1 << 20 {
        format!("{:.1} MB", bytes as f64 / (1 << 20) as f64)
    } else {
        format!("{bytes} B")
    }
}

fn line(label: &str, s: &[Stats], p: f64) -> String {
    let pick = |f: fn(&Stats) -> f64| percentile(&mut s.iter().map(f).collect::<Vec<_>>(), p);
    let max = |f: fn(&Stats) -> u64| s.iter().map(f).max().unwrap_or(0);
    let cpu = pick(Stats::cpu);
    let wall = pick(|s| s.wall);
    format!(
        "{label}: cpu {cpu:.2} ms (sync {:.2}, water {:.2}, groups {:.2}, encode {:.2}, submit {:.2}), gpu {:.2} ms, wall {wall:.2} ms, {} draws, {} calls, {} uploads, {}, {:.0} fps",
        pick(|s| s.sync),
        pick(|s| s.water),
        pick(|s| s.groups),
        pick(|s| s.encode),
        pick(|s| s.submit),
        pick(|s| s.gpu),
        max(|s| s.draws.into()),
        max(|s| s.calls.into()),
        max(|s| s.uploads.into()),
        mb(max(|s| s.bytes)),
        if wall > 0. { 1e3 / wall } else { 0. }
    )
}

pub fn report(frames: &[Stats], summary: &str) -> String {
    let Some((first, rest)) = frames.split_first() else {
        return "bench: no frames".into();
    };
    let gpu = if first.timestamps {
        "timestamp queries"
    } else {
        "wall time to the fence"
    };
    let mut out = vec![
        format!(
            "bench: {} frames, gpu time from {gpu}, {summary}",
            frames.len()
        ),
        line("frame 1", std::slice::from_ref(first), 0.5),
    ];
    if !rest.is_empty() {
        out.push(line(&format!("frames 2-{} p50", frames.len()), rest, 0.5));
        out.push(line(&format!("frames 2-{} p95", frames.len()), rest, 0.95));
    }
    out.join("\n")
}
