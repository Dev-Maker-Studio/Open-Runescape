use renderer::{capture, frame, scene};
use std::path::Path;

const FIXTURE: &str = "tests/fixtures/pixel-hashes.txt";
const DUMPS: &str = "../harness/target/bench";
const SIZE: (u32, u32) = (765, 503);

fn fnv(bytes: &[u8]) -> u64 {
    bytes.iter().fold(0xcbf29ce484222325u64, |h, b| {
        (h ^ u64::from(*b)).wrapping_mul(0x100000001b3)
    })
}

fn hash(
    renderer: &mut renderer::renderer::Renderer,
    mut source: scene::Source,
    w: u32,
    h: u32,
) -> u64 {
    let mut rgba = vec![];
    for i in 0..3 {
        rgba = capture::render_rgba(renderer, source.next(i, w, h));
    }
    fnv(&rgba)
}

#[test]
fn stored_frames_render_the_recorded_pixels() {
    let Some(mut renderer) = pollster::block_on(capture::try_headless()) else {
        println!("skip: no gpu adapter");
        return;
    };
    let adapter = renderer.gpu.adapter.get_info().name;
    let mut lines = vec![adapter.clone()];
    for name in scene::NAMES {
        lines.push(format!(
            "scene:{name} {:016x}",
            hash(&mut renderer, scene::Source::scene(name), SIZE.0, SIZE.1)
        ));
    }
    let mut dumps: Vec<_> = std::fs::read_dir(DUMPS)
        .into_iter()
        .flatten()
        .flatten()
        .map(|e| e.path())
        .filter(|p| p.extension().is_some_and(|e| e == "orf"))
        .collect();
    dumps.sort();
    for path in dumps {
        let f = frame::parse(&std::fs::read(&path).unwrap()).unwrap();
        let (w, h) = (f.width, f.height);
        lines.push(format!(
            "{} {:016x}",
            path.file_name().unwrap().to_string_lossy(),
            hash(&mut renderer, scene::Source::replay(f), w, h)
        ));
    }
    let text = lines.join("\n") + "\n";
    if std::env::var_os("PIXELS_WRITE").is_some() {
        std::fs::write(FIXTURE, &text).unwrap();
        return;
    }
    let recorded = std::fs::read_to_string(FIXTURE).unwrap_or_default();
    let Some((recorded_adapter, rest)) = recorded.split_once('\n') else {
        panic!("{FIXTURE} missing, record it with PIXELS_WRITE=1")
    };
    if recorded_adapter != adapter {
        println!("skip: hashes recorded on {recorded_adapter:?}, this adapter is {adapter:?}");
        return;
    }
    let want: std::collections::HashMap<&str, &str> =
        rest.lines().filter_map(|l| l.split_once(' ')).collect();
    for line in &lines[1..] {
        let (name, h) = line.split_once(' ').unwrap();
        match want.get(name) {
            Some(r) => assert_eq!(
                *r, h,
                "{name}: pixels changed, re-record with PIXELS_WRITE=1 only if the change is intended"
            ),
            None => println!("skip: {name} has no recorded hash"),
        }
    }
    assert!(Path::new(FIXTURE).exists());
}
