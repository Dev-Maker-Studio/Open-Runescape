#[cfg(not(target_arch = "wasm32"))]
mod window;

#[cfg(not(target_arch = "wasm32"))]
fn main() {
    use renderer::{capture, frame, scene, shaders};
    use std::{fs, io::BufWriter};

    let args: Vec<String> = std::env::args().skip(1).collect();
    let arg = |key: &str| args.iter().position(|a| a == key).and_then(|i| args.get(i + 1)).map(String::as_str);
    let name = arg("--scene").unwrap_or("all");
    let present = args.iter().any(|a| a == "--present");
    let frames: u32 = arg("--frames").and_then(|v| v.parse().ok()).unwrap_or(if present { 30 } else { 1 });
    let mut dims = arg("--size").and_then(|v| v.split_once('x')).and_then(|(w, h)| Some((w.parse().ok()?, h.parse().ok()?)));
    let mut source = match arg("--replay") {
        Some(path) => {
            let f = frame::parse(&fs::read(path).expect("replay file")).expect("invalid frame file");
            println!("{}", f.summary());
            dims = dims.or(Some((f.width, f.height)));
            scene::Source::replay(f)
        }
        None => {
            assert!(scene::NAMES.contains(&name), "scenes: {:?}", scene::NAMES);
            scene::Source::scene(name)
        }
    };
    let (w, h) = dims.unwrap_or((800, 600));
    if args.iter().any(|a| a == "--check-shaders") {
        let renderer = pollster::block_on(capture::headless());
        for (n, src) in shaders::ALL {
            let guard = renderer.gpu.device.push_error_scope(wgpu::ErrorFilter::Validation);
            let _module = renderer.gpu.device.create_shader_module(wgpu::ShaderModuleDescriptor { label: None, source: wgpu::ShaderSource::Wgsl((*src).into()) });
            let err = pollster::block_on(guard.pop());
            println!("{n}\t{}", err.map_or("ok".into(), |e| e.to_string().replace('\n', " ")));
        }
        return;
    }
    if let Some(path) = arg("--dump") {
        fs::write(path, frame::write(&source.next(0, w, h))).unwrap();
        return;
    }
    let Some(out) = arg("--out").filter(|_| !present) else {
        return window::run(source, (w, h), if present || arg("--frames").is_some() { frames } else { u32::MAX });
    };
    let mut renderer = pollster::block_on(capture::headless());
    let mut rgba = vec![];
    let mut size = (w, h);
    for i in 0..frames {
        let f = source.next(i, w, h);
        size = (f.width, f.height);
        rgba = capture::render_rgba(&mut renderer, &f);
    }
    let mut enc = png::Encoder::new(BufWriter::new(fs::File::create(out).unwrap()), size.0, size.1);
    enc.set_color(png::ColorType::Rgba);
    enc.set_depth(png::BitDepth::Eight);
    enc.write_header().unwrap().write_image_data(&rgba).unwrap();
}

#[cfg(target_arch = "wasm32")]
fn main() {}
