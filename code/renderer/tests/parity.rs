use renderer::{
    capture, frame,
    metrics::{self, Rgba},
    pass::Pass,
};
use std::path::Path;

const ROOT: &str = "../harness/target";
const PAIRS: [(&str, &str); 4] = [
    (
        "twin/parity-ctl/client/frame.orf",
        "twin/parity-ctl/client/shot.png",
    ),
    (
        "loop/iteration4-comparison/client/frame-0000.orf",
        "loop/iteration4-comparison/client/frame-0000.png",
    ),
    (
        "loop/iteration4-comparison/client/frame-0001.orf",
        "loop/iteration4-comparison/client/frame-0001.png",
    ),
    (
        "loop/iteration4-comparison/client/frame-0002.orf",
        "loop/iteration4-comparison/client/frame-0002.png",
    ),
];
const WATER_RADIUS: usize = 3;

fn read_png(path: &Path) -> (usize, usize, Vec<u8>) {
    let mut reader = png::Decoder::new(std::io::BufReader::new(std::fs::File::open(path).unwrap()))
        .read_info()
        .unwrap();
    let mut buf = vec![0; reader.output_buffer_size().unwrap()];
    let info = reader.next_frame(&mut buf).unwrap();
    let (w, h) = (info.width as usize, info.height as usize);
    let rgba = match info.color_type {
        png::ColorType::Rgba => buf[..w * h * 4].to_vec(),
        png::ColorType::Rgb => buf[..w * h * 3]
            .chunks_exact(3)
            .flat_map(|p| [p[0], p[1], p[2], 255])
            .collect(),
        other => panic!("unsupported png color {other:?}"),
    };
    (w, h, rgba)
}

#[test]
fn water_region_parity_against_native_screenshots() {
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    for (orf, shot) in PAIRS {
        let (orf, shot) = (Path::new(ROOT).join(orf), Path::new(ROOT).join(shot));
        if !orf.exists() || !shot.exists() {
            println!("skip: {} missing", orf.display());
            continue;
        }
        let full = frame::parse(&std::fs::read(&orf).unwrap()).unwrap();
        let mut dry = full.clone();
        dry.draws.retain(|d| d.pass != Pass::HdWater);
        if dry.draws.len() == full.draws.len() {
            println!("skip: {} has no water draw", orf.display());
            continue;
        }
        for d in full.draws.iter().filter(|d| d.pass == Pass::HdWater) {
            println!(
                "  water draw mesh={} instances={} viewport={:?} scissor={:?} skip={:?}",
                d.mesh, d.instances, d.viewport, d.scissor, d.skip
            );
        }
        let (w, h, native) = read_png(&shot);
        assert_eq!((w as u32, h as u32), (full.width, full.height));
        let wet = capture::render_rgba(&mut renderer, &full);
        let dry = capture::render_rgba(&mut renderer, &dry);
        let mask = metrics::dilate(&metrics::changed(&wet, &dry), w, h, WATER_RADIUS);
        let native = Rgba {
            width: w,
            height: h,
            data: &native,
        };
        let rendered = Rgba {
            width: w,
            height: h,
            data: &wet,
        };
        let all = metrics::diff(&native, &rendered, None);
        let water = metrics::diff(&native, &rendered, Some(&mask));
        println!(
            "{}: all ssim={:.4} exact={}/{} over8={} | water px={} ssim={:.4} exact={} over8={} max={}",
            orf.file_name().unwrap().to_string_lossy(),
            all.ssim,
            all.exact,
            all.pixels,
            all.over_8,
            water.pixels,
            water.ssim,
            water.exact,
            water.over_8,
            water.max
        );
        println!("water mask pixels {}", water.pixels);
    }
}
