use renderer::bloom::{Bloom, BloomParams, KERNEL, LUMINANCE, levels};
use renderer::gpu::{Gpu, instance};
use wgpu::*;

const FORMAT: TextureFormat = TextureFormat::Rgba8Unorm;

fn quantize(v: f32) -> f32 {
    (v.clamp(0., 1.) * 255.).round() / 255.
}

struct Image {
    w: usize,
    h: usize,
    px: Vec<[f32; 4]>,
}

impl Image {
    fn from_bytes(w: usize, h: usize, data: &[u8]) -> Image {
        Image {
            w,
            h,
            px: data
                .chunks_exact(4)
                .map(|p| std::array::from_fn(|i| f32::from(p[i]) / 255.))
                .collect(),
        }
    }

    fn at(&self, x: isize, y: isize) -> [f32; 4] {
        let x = x.clamp(0, self.w as isize - 1) as usize;
        let y = y.clamp(0, self.h as isize - 1) as usize;
        self.px[y * self.w + x]
    }
}

fn cpu_downsample(src: &Image, w: usize, h: usize) -> Image {
    let (fx, fy) = (src.w as f32 / w as f32, src.h as f32 / h as f32);
    let mut px = Vec::new();
    for y in 0..h {
        for x in 0..w {
            let (sx, sy) = ((x as f32 + 0.5) * fx - 0.5, (y as f32 + 0.5) * fy - 0.5);
            let (x0, y0) = (sx.floor(), sy.floor());
            let (tx, ty) = (sx - x0, sy - y0);
            let mut out = [0f32; 4];
            for (dx, dy, wt) in [
                (0., 0., (1. - tx) * (1. - ty)),
                (1., 0., tx * (1. - ty)),
                (0., 1., (1. - tx) * ty),
                (1., 1., tx * ty),
            ] {
                let p = src.at((x0 + dx) as isize, (y0 + dy) as isize);
                for i in 0..4 {
                    out[i] += p[i] * wt;
                }
            }
            px.push(out.map(quantize));
        }
    }
    Image { w, h, px }
}

fn cpu_bloom(scene: &Image, p: BloomParams) -> Vec<[f32; 4]> {
    let mut src = Image {
        w: scene.w,
        h: scene.h,
        px: scene.px.clone(),
    };
    for (w, h) in levels(scene.w as u32, scene.h as u32) {
        src = cpu_downsample(&src, w as usize, h as usize);
    }
    let a = cpu_downsample(&src, 256, 256);
    let bright = Image {
        w: 256,
        h: 256,
        px: a
            .px
            .iter()
            .map(|c| {
                let lum: f32 = (0..3).map(|i| LUMINANCE[i] * c[i]).sum();
                let keep = if lum >= p.white_point { 1. } else { 0. };
                c.map(|v| quantize(v * keep))
            })
            .collect(),
    };
    let blur = |img: &Image, dx: isize, dy: isize| Image {
        w: 256,
        h: 256,
        px: (0..256isize)
            .flat_map(|y| (0..256isize).map(move |x| (x, y)))
            .map(|(x, y)| {
                let mut out = [0f32; 4];
                for (k, wt) in KERNEL.iter().enumerate() {
                    let taps: &[isize] = if k == 0 {
                        &[0]
                    } else {
                        &[-(k as isize), k as isize]
                    };
                    for t in taps {
                        let c = img.at(x + t * dx, y + t * dy);
                        for i in 0..4 {
                            out[i] += c[i] * wt;
                        }
                    }
                }
                out.map(quantize)
            })
            .collect(),
    };
    let blurred = blur(&blur(&bright, 1, 0), 0, 1);
    let up = cpu_downsample(&blurred, scene.w, scene.h);
    scene
        .px
        .iter()
        .zip(&up.px)
        .map(|(s, b)| {
            let lum: f32 = (0..3).map(|i| LUMINANCE[i] * s[i]).sum();
            let pre = 0.99 * lum + 0.01;
            let post = pre * (1. + pre / p.bloom) / (pre + 1.);
            let mut out = [0f32; 4];
            for i in 0..3 {
                out[i] = quantize(s[i] * (post / pre) + b[i] * p.brightpass);
            }
            out[3] = 1.;
            out
        })
        .collect()
}

fn scene(w: usize, h: usize) -> Vec<u8> {
    let mut data = Vec::new();
    for y in 0..h {
        for x in 0..w {
            let bright = (60..110).contains(&(x * 256 / w)) && (80..130).contains(&(y * 256 / h));
            let base = [(40 + x * 90 / w) as u8, (50 + y * 80 / h) as u8, 70];
            let c = if bright { [250, 240, 200] } else { base };
            data.extend([c[0], c[1], c[2], 200]);
        }
    }
    data
}

fn run(gpu: &Gpu, w: u32, h: u32, data: &[u8], params: BloomParams) -> Vec<u8> {
    let device = &gpu.device;
    let size = Extent3d {
        width: w,
        height: h,
        depth_or_array_layers: 1,
    };
    let source = device.create_texture(&TextureDescriptor {
        label: None,
        size,
        mip_level_count: 1,
        sample_count: 1,
        dimension: TextureDimension::D2,
        format: FORMAT,
        usage: TextureUsages::TEXTURE_BINDING | TextureUsages::COPY_DST,
        view_formats: &[],
    });
    gpu.queue.write_texture(
        source.as_image_copy(),
        data,
        TexelCopyBufferLayout {
            offset: 0,
            bytes_per_row: Some(w * 4),
            rows_per_image: Some(h),
        },
        size,
    );
    let target = renderer::capture::target(device, w, h);
    let mut bloom = Bloom::new(device, FORMAT);
    let mut encoder = device.create_command_encoder(&Default::default());
    bloom.encode(
        device,
        &mut encoder,
        &source.create_view(&Default::default()),
        (w, h),
        &target.create_view(&Default::default()),
        params,
    );
    let row = (w * 4).next_multiple_of(COPY_BYTES_PER_ROW_ALIGNMENT);
    let buffer = device.create_buffer(&BufferDescriptor {
        label: None,
        size: (row * h) as u64,
        usage: BufferUsages::COPY_DST | BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    encoder.copy_texture_to_buffer(
        target.as_image_copy(),
        TexelCopyBufferInfo {
            buffer: &buffer,
            layout: TexelCopyBufferLayout {
                offset: 0,
                bytes_per_row: Some(row),
                rows_per_image: Some(h),
            },
        },
        size,
    );
    gpu.queue.submit([encoder.finish()]);
    buffer.map_async(MapMode::Read, .., |r| r.unwrap());
    device.poll(PollType::wait_indefinitely()).unwrap();
    let mapped = buffer.get_mapped_range(..).unwrap();
    mapped
        .chunks(row as usize)
        .flat_map(|r| &r[..(w * 4) as usize])
        .copied()
        .collect()
}

fn compare(w: u32, h: u32, params: BloomParams) -> (usize, u8, f32) {
    let Some(gpu) = pollster::block_on(Gpu::try_new(&instance(), None)) else {
        println!("skip: no gpu adapter");
        return (0, 0, 0.);
    };
    let data = scene(w as usize, h as usize);
    let got = run(&gpu, w, h, &data, params);
    let want = cpu_bloom(&Image::from_bytes(w as usize, h as usize, &data), params);
    let (mut exact, mut max, mut lit) = (0, 0u8, 0f32);
    for (g, wnt) in got.chunks_exact(4).zip(&want) {
        let d = (0..4)
            .map(|i| g[i].abs_diff((wnt[i] * 255.).round() as u8))
            .max()
            .unwrap();
        exact += usize::from(d == 0);
        max = max.max(d);
        lit = lit.max(f32::from(g[0]));
    }
    println!("{w}x{h}: exact={exact}/{} max_diff={max}", got.len() / 4);
    (got.len() / 4, max, lit)
}

#[test]
fn bloom_matches_the_cpu_reference_at_the_blur_resolution() {
    let params = BloomParams {
        white_point: 0.7,
        bloom: 4.,
        brightpass: 0.6,
    };
    let (n, max, lit) = compare(256, 256, params);
    if n > 0 {
        assert!(max <= 2, "max diff {max}");
        assert!(lit > 200.);
    }
}

#[test]
fn bloom_matches_the_cpu_reference_through_the_downsample_chain() {
    let params = BloomParams {
        white_point: 0.7,
        bloom: 4.,
        brightpass: 0.6,
    };
    let (n, max, _) = compare(512, 334, params);
    if n > 0 {
        assert!(max <= 4, "max diff {max}");
    }
}

#[test]
fn composite_forces_alpha_to_one_and_tone_maps_dark_pixels_unchanged_without_bloom() {
    let Some(gpu) = pollster::block_on(Gpu::try_new(&instance(), None)) else {
        println!("skip: no gpu adapter");
        return;
    };
    let params = BloomParams {
        white_point: 2.,
        bloom: 1000.,
        brightpass: 1.,
    };
    let data = scene(256, 256);
    let got = run(&gpu, 256, 256, &data, params);
    for (g, s) in got.chunks_exact(4).zip(data.chunks_exact(4)) {
        assert_eq!(g[3], 255);
        let lum: f32 = (0..3).map(|i| LUMINANCE[i] * f32::from(s[i]) / 255.).sum();
        let pre = 0.99 * lum + 0.01;
        let scale = (1. + pre / params.bloom) / (pre + 1.);
        for i in 0..3 {
            let want = (f32::from(s[i]) * scale).round() as i32;
            assert!((i32::from(g[i]) - want).abs() <= 1);
        }
    }
}

#[test]
fn frame_bloom_parameters_drive_the_render_path_and_old_frames_are_untouched() {
    let Some(mut renderer) = pollster::block_on(renderer::capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    let plain = renderer::scene::build("model", 0, 128, 96).unwrap();
    assert!(plain.bloom.is_none());
    let base = renderer::capture::render_rgba(&mut renderer, &plain);
    assert_eq!(base, renderer::capture::render_rgba(&mut renderer, &plain));
    let params = BloomParams {
        white_point: 2.,
        bloom: 1000.,
        brightpass: 1.,
    };
    let mut lit = plain.clone();
    lit.bloom = Some(params);
    let got = renderer::capture::render_rgba(&mut renderer, &lit);
    assert_eq!(got.len(), base.len());
    let mut changed = 0;
    for (g, s) in got.chunks_exact(4).zip(base.chunks_exact(4)) {
        assert_eq!(g[3], 255);
        let lum: f32 = (0..3).map(|i| LUMINANCE[i] * f32::from(s[i]) / 255.).sum();
        let pre = 0.99 * lum + 0.01;
        let scale = (1. + pre / params.bloom) / (pre + 1.);
        for i in 0..3 {
            let want = (f32::from(s[i]) * scale).round() as i32;
            assert!((i32::from(g[i]) - want).abs() <= 1, "{} vs {want}", g[i]);
        }
        changed += usize::from(g[..3] != s[..3]);
    }
    assert!(changed > 0);
    assert_eq!(base, renderer::capture::render_rgba(&mut renderer, &plain));
    let mut resized = renderer::scene::build("model", 0, 64, 64).unwrap();
    resized.bloom = Some(params);
    assert_eq!(
        renderer::capture::render_rgba(&mut renderer, &resized).len(),
        64 * 64 * 4
    );
}
