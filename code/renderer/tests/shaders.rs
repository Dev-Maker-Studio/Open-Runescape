use naga::valid::{Capabilities, ValidationFlags, Validator};
use renderer::{pass, shaders};

fn validate(name: &str) -> naga::Module {
    let module =
        naga::front::wgsl::parse_str(shaders::get(name)).unwrap_or_else(|e| panic!("{name}: {e}"));
    Validator::new(ValidationFlags::all(), Capabilities::all())
        .validate(&module)
        .unwrap_or_else(|e| panic!("{name}: {e:?}"));
    module
}

#[test]
fn every_wgsl_module_validates() {
    for (name, _) in shaders::ALL {
        validate(name);
    }
    assert_eq!(shaders::ALL.len(), 34);
}

#[test]
fn every_pass_has_both_stages() {
    for p in pass::ALL {
        let vertex = validate(&format!("{}.vert", p.shader()));
        let fragment = validate(&format!("{}.frag", p.shader()));
        assert_eq!(
            vertex.entry_points[0].stage,
            naga::ShaderStage::Vertex,
            "{}",
            p.shader()
        );
        assert_eq!(
            fragment.entry_points[0].stage,
            naga::ShaderStage::Fragment,
            "{}",
            p.shader()
        );
        assert_eq!(
            fragment.entry_points[0].function.result.is_some(),
            !p.offscreen(),
            "{} color output",
            p.shader()
        );
    }
}
