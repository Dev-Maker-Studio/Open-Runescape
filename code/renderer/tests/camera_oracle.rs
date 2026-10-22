use renderer::camera::{self, CameraPose, Viewport};
use renderer::math::Mat4;
use std::collections::BTreeMap;

struct Case {
    input: Vec<i64>,
    output: Vec<i64>,
}

fn load() -> (BTreeMap<String, Vec<Case>>, usize) {
    let text = std::fs::read_to_string(concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/camera.native.tsv"
    ))
    .unwrap();
    let mut cases: BTreeMap<String, Vec<Case>> = BTreeMap::new();
    let mut trailer = None;
    let mut total = 0;
    for line in text.lines() {
        let fields: Vec<&str> = line.split('\t').collect();
        if fields[0] == "T" {
            trailer = Some(fields[1].parse::<usize>().unwrap());
            continue;
        }
        let bar = fields.iter().position(|f| *f == "|").unwrap();
        let parse = |slice: &[&str]| -> Vec<i64> {
            slice
                .iter()
                .filter(|f| !f.is_empty())
                .map(|f| f.parse().unwrap())
                .collect()
        };
        cases.entry(fields[1].to_string()).or_default().push(Case {
            input: parse(&fields[2..bar]),
            output: parse(&fields[bar + 1..]),
        });
        total += 1;
    }
    assert_eq!(trailer, Some(total));
    (cases, total)
}

fn canon(value: f32) -> i64 {
    if value.is_nan() {
        0x7fc0_0000
    } else {
        i64::from(value.to_bits() as i32)
    }
}

fn mat_bits(m: &Mat4) -> Vec<i64> {
    m.iter().map(|v| canon(*v)).collect()
}

fn float_in(bits: i64) -> f32 {
    f32::from_bits(bits as i32 as u32)
}

fn i(v: i64) -> i32 {
    v as i32
}

fn viewport(w: i32, h: i32, fov: i32, start: i32, end: i32) -> Viewport {
    Viewport {
        width: w,
        height: h,
        scaled_fov: fov,
        view_range_start: start,
        view_range_end: end,
    }
}

#[test]
fn theta_matches_native() {
    let (cases, _) = load();
    for c in &cases["theta"] {
        assert_eq!(vec![canon(camera::theta(i(c.input[0])))], c.output);
    }
}

#[test]
fn view_pose_matches_native() {
    let (cases, _) = load();
    for c in &cases["pose"] {
        let pose = CameraPose {
            x: i(c.input[0]),
            y: i(c.input[1]),
            z: i(c.input[2]),
            yaw: i(c.input[3]),
            roll: i(c.input[4]),
            pitch: i(c.input[5]),
        };
        let m = camera::view_matrix(&pose);
        let got: Vec<i64> = [
            m.xa, m.xb, m.xc, m.xd, m.ya, m.yb, m.yc, m.yd, m.za, m.zb, m.zc, m.zd,
        ]
        .iter()
        .map(|v| canon(*v))
        .collect();
        assert_eq!(got, c.output, "{:?}", c.input);
    }
}

#[test]
fn projection_matches_native() {
    let (cases, _) = load();
    for c in &cases["proj"] {
        let p = camera::projection(&viewport(
            i(c.input[0]),
            i(c.input[1]),
            i(c.input[2]),
            i(c.input[3]),
            i(c.input[4]),
        ));
        let mut got = mat_bits(&p);
        got.push(canon(camera::near_clip_distance(&p)));
        got.push(canon(camera::depth_perspective_cd(&p)));
        assert_eq!(got, c.output, "{:?}", c.input);
    }
}

#[test]
fn view_projection_matches_native() {
    let (cases, _) = load();
    for c in &cases["vp"] {
        let pose = CameraPose {
            x: i(c.input[0]),
            y: i(c.input[1]),
            z: i(c.input[2]),
            yaw: i(c.input[3]),
            roll: i(c.input[4]),
            pitch: i(c.input[5]),
        };
        let view = camera::view_matrix(&pose);
        let proj = camera::projection(&viewport(
            i(c.input[6]),
            i(c.input[7]),
            i(c.input[8]),
            i(c.input[9]),
            i(c.input[10]),
        ));
        let vp = camera::view_projection(&view, &proj);
        let mut got = mat_bits(&view.to_matrix44());
        got.extend(mat_bits(&vp));
        got.extend(mat_bits(&camera::batcher_view_projection(&vp)));
        assert_eq!(got, c.output, "{:?}", c.input);
    }
}

#[test]
fn fog_matches_native() {
    let (cases, _) = load();
    for c in &cases["fog"] {
        let mut model = [0.0f32; 16];
        for (k, slot) in model.iter_mut().enumerate() {
            *slot = float_in(c.input[k]);
        }
        let enabled = c.input[16] != 0;
        let (color, density, offset) = (i(c.input[17]), i(c.input[18]), i(c.input[19]));
        let f = camera::fog(&model, enabled, color, density, offset);
        let got = vec![
            canon(f.start),
            canon(f.end),
            i64::from(f.enabled),
            canon(f.color[0]),
            canon(f.color[1]),
            canon(f.color[2]),
        ];
        assert_eq!(got, c.output, "{:?}", c.input);
    }
}

#[test]
fn view_range_matches_native() {
    let (cases, _) = load();
    for c in &cases["range"] {
        let (start, end) = camera::view_range(i(c.input[0]), true, i(c.input[1]));
        assert_eq!(
            vec![i64::from(start), i64::from(end)],
            c.output,
            "{:?}",
            c.input
        );
    }
}

#[test]
fn environment_density_matches_formula() {
    let (cases, _) = load();
    for c in &cases["density"] {
        let d = camera::environment_density(i(c.input[0]), c.input[1] != 0);
        assert_eq!(vec![i64::from(d)], c.output);
    }
}

#[test]
fn clear_color_matches_formula() {
    let (cases, _) = load();
    for c in &cases["clear"] {
        let got: Vec<i64> = camera::clear_color(i(c.input[0]))
            .iter()
            .map(|v| canon(*v))
            .collect();
        assert_eq!(got, c.output);
    }
}

#[test]
fn case_count() {
    let (_, total) = load();
    assert_eq!(total, 32000);
}
