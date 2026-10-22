use naga::{
    AddressSpace, ImageClass, ImageDimension, Module, ResourceBinding, ScalarKind, TypeInner,
    VectorSize,
};
use renderer::{
    pass::{self, Pass},
    shaders,
    uniforms::*,
    water,
};
use std::mem::offset_of;
use wgpu::{TextureViewDimension as D, VertexFormat as F};

fn module(name: &str) -> Module {
    naga::front::wgsl::parse_str(shaders::get(name)).unwrap()
}

fn block(m: &Module) -> Vec<(String, u32)> {
    let var = m
        .global_variables
        .iter()
        .find(|(_, v)| v.space == AddressSpace::Uniform)
        .map(|(_, v)| v)
        .unwrap();
    let TypeInner::Struct { members, .. } = &m.types[var.ty].inner else {
        panic!("uniform is not a struct")
    };
    members
        .iter()
        .map(|x| (x.name.clone().unwrap(), x.offset))
        .collect()
}

fn block_size(m: &Module) -> u32 {
    let var = m
        .global_variables
        .iter()
        .find(|(_, v)| v.space == AddressSpace::Uniform)
        .map(|(_, v)| v)
        .unwrap();
    m.types[var.ty].inner.size(m.to_ctx())
}

fn image_dimension(dim: ImageDimension, arrayed: bool) -> D {
    match (dim, arrayed) {
        (ImageDimension::D2, false) => D::D2,
        (ImageDimension::D2, true) => D::D2Array,
        (ImageDimension::D3, _) => D::D3,
        (ImageDimension::Cube, _) => D::Cube,
        _ => panic!("unexpected image dimension"),
    }
}

fn components(f: F) -> u32 {
    match f {
        F::Float32 => 1,
        F::Float32x2 => 2,
        F::Float32x3 => 3,
        F::Float32x4 | F::Unorm8x4 => 4,
        _ => panic!("unexpected format"),
    }
}

#[test]
fn texture_bindings_match_pass_table() {
    for p in pass::ALL {
        let mut seen = vec![None; p.bindings().len() * 2];
        for name in [
            format!("{}.vert", p.shader()),
            format!("{}.frag", p.shader()),
        ] {
            let m = module(&name);
            for (_, v) in m.global_variables.iter() {
                let Some(ResourceBinding { group: 1, binding }) = v.binding else {
                    continue;
                };
                let slot = seen
                    .get_mut(binding as usize)
                    .unwrap_or_else(|| panic!("{name}: binding {binding} beyond pass table"));
                let expected = p.bindings()[binding as usize / 2];
                match &m.types[v.ty].inner {
                    TypeInner::Image {
                        dim,
                        arrayed,
                        class: ImageClass::Sampled { multi: false, .. },
                    } => {
                        assert_eq!(binding % 2, 0, "{name}: texture at odd binding");
                        assert_eq!(
                            image_dimension(*dim, *arrayed),
                            expected.dimension(),
                            "{name}: binding {binding}"
                        );
                    }
                    TypeInner::Sampler { comparison: false } => {
                        assert_eq!(binding % 2, 1, "{name}: sampler at even binding")
                    }
                    other => panic!("{name}: unexpected binding type {other:?}"),
                }
                *slot = Some(());
            }
        }
        assert!(
            seen.iter().all(Option::is_some),
            "{}: pass table has bindings the shaders do not declare",
            p.shader()
        );
    }
}

#[test]
fn vertex_inputs_match_pass_table() {
    for p in pass::ALL {
        let m = module(&format!("{}.vert", p.shader()));
        let entry = &m.entry_points[0];
        let attributes: Vec<_> = p
            .attributes()
            .iter()
            .chain(p.instance_attributes().unwrap_or(&[]))
            .collect();
        for arg in &entry.function.arguments {
            let Some(naga::Binding::Location { location, .. }) = arg.binding else {
                continue;
            };
            let a = attributes
                .iter()
                .find(|a| a.shader_location == location)
                .unwrap_or_else(|| panic!("{}: no attribute for location {location}", p.shader()));
            let (kind, size) = match &m.types[arg.ty].inner {
                TypeInner::Scalar(s) => (s.kind, 1),
                TypeInner::Vector { size, scalar } => (scalar.kind, *size as u32),
                other => panic!("{}: unexpected input {other:?}", p.shader()),
            };
            assert_eq!(
                kind,
                ScalarKind::Float,
                "{}: location {location}",
                p.shader()
            );
            assert!(
                components(a.format) <= size,
                "{}: location {location} has more components than the shader reads",
                p.shader()
            );
        }
        for a in p.attributes() {
            assert!(
                a.offset + a.format.size() <= p.stride(),
                "{}: attribute at {} exceeds stride",
                p.shader(),
                a.offset
            );
        }
    }
}

#[test]
fn uniform_blocks_match_pass_table() {
    for p in pass::ALL {
        let (v, f) = (
            module(&format!("{}.vert", p.shader())),
            module(&format!("{}.frag", p.shader())),
        );
        assert_eq!(
            block(&v),
            block(&f),
            "{}: stages disagree on the uniform block",
            p.shader()
        );
        assert_eq!(
            block_size(&v) as usize,
            p.block_size(),
            "{}: block size",
            p.shader()
        );
    }
}

macro_rules! members {
    ($pass:expr, $t:ty, [$(($name:literal, $field:ident)),* $(,)?]) => {{
        let m = module(&format!("{}.vert", $pass.shader()));
        assert_eq!(size_of::<$t>(), $pass.block_size(), "{}: struct size", stringify!($t));
        let offsets = block(&m);
        let expected: Vec<(&str, usize)> = vec![$(($name, offset_of!($t, $field))),*];
        assert_eq!(offsets.len(), expected.len(), "{}: member count", stringify!($t));
        for ((name, offset), (want_name, want_offset)) in offsets.iter().zip(expected) {
            assert_eq!((name.as_str(), *offset as usize), (want_name, want_offset), "{}", stringify!($t));
        }
    }};
}

#[test]
fn uniform_structs_match_shader_blocks() {
    members!(
        Pass::Model,
        ModelUniform,
        [
            ("uModelViewProjection", mvp),
            ("uModelView", mv),
            ("uNormalMatrix", normal),
            ("uSunDirection", sun_dir),
            ("uSunColor", sun_color),
            ("uAmbient", ambient),
            ("uContrast", contrast),
            ("uFogStart", fog_start),
            ("uFogEnd", fog_end),
            ("uFogEnabled_u", fog_enabled),
            ("uPreLitColors_u", pre_lit),
            ("uTextureEnabled_u", texture_enabled),
            ("uFogColor", fog_color),
            ("uAlphaThreshold", alpha_threshold),
        ]
    );
    members!(
        Pass::Ground,
        GroundUniform,
        [
            ("uModelViewProjection", mvp),
            ("uModelView", mv),
            ("uNormalMatrix", normal),
            ("uSunDirection", sun_dir),
            ("uSunColor", sun_color),
            ("uAmbient", ambient),
            ("uContrast", contrast),
            ("uFogStart", fog_start),
            ("uFogEnd", fog_end),
            ("uFogEnabled_u", fog_enabled),
            ("uShadowMatrix", shadow_matrix),
            ("uShadowEnabled_u", shadow_enabled),
            ("uPointLightCount", point_count),
            ("uPointLightPos", point_pos),
            ("uPointLightColor", point_color),
            ("uGroundShadowEnabled_u", ground_shadow_enabled),
            ("uGroundShadowScale", ground_shadow_scale),
            ("uFogColor", fog_color),
            ("uShadowBias", shadow_bias),
            ("uShadowIntensity", shadow_intensity),
            ("uShadowMapSize", shadow_map_size),
        ]
    );
    members!(Pass::Sprite, SpriteUniform, [("uProjection", projection)]);
    members!(
        Pass::Shadow,
        ShadowUniform,
        [
            ("uLightViewProjection", light_vp),
            ("uModelMatrix", model),
            ("uAlphaTestEnabled_u", alpha_test),
            ("uAlphaThreshold", alpha_threshold),
        ]
    );
    members!(
        Pass::Particle,
        ParticleUniform,
        [
            ("uViewProjection", vp),
            ("uTextureScroll", scroll),
            ("uAmbient", ambient),
            ("uFogEnabled_u", fog_enabled),
            ("uFogStart", fog_start),
            ("uFogEnd", fog_end),
            ("uTextureEnabled_u", texture_enabled),
            ("uCombineMode", combine_mode),
            ("uFogColor", fog_color),
        ]
    );
    members!(
        Pass::HdWater,
        HdWaterUniform,
        [
            ("uViewProjection", vp),
            ("uHasWaveNoise_u", has_wave_noise),
            ("uWaveScale", wave_scale),
            ("uTime", time),
            ("uFlow", flow),
            ("uWaveAmplitude", wave_amplitude),
            ("uHasWaterDepth_u", has_water_depth),
            ("uUsePlanar_u", use_planar),
            ("uPlanarDistort", planar_distort),
            ("uHasFoamNoise_u", has_foam_noise),
            ("uHasEnvMap_u", has_env_map),
            ("uShadowEnabled_u", shadow_enabled),
            ("uInvViewProjection", inv_vp),
            ("uViewport", viewport),
            ("uFramebufferSize", framebuffer_size),
            ("uSunDirection", sun_dir),
            ("uSunColor", sun_color),
            ("uAmbient", ambient),
            ("uSkyColour", sky_color),
            ("uWaterColour", water_color),
            ("uAbsorbDistance", absorb_distance),
            ("uScatter", scatter),
            ("uRefraction", refraction),
            ("uWaveStrength", wave_strength),
            ("uSlowTime", slow_time),
            ("uShadowScale", shadow_scale),
            ("uReflectionSteps", reflection_steps),
            ("uFogEnabled_u", fog_enabled),
            ("uFogStart", fog_start),
            ("uFogEnd", fog_end),
            ("uPointLightCount", point_count),
            ("uPointLightPos", point_pos),
            ("uPointLightColor", point_color),
        ]
    );
    members!(
        Pass::Material,
        MaterialUniform,
        [
            ("uModelViewProjection", mvp),
            ("uModelView", mv),
            ("uNormalMatrix", normal),
            ("uSunDirection", sun_dir),
            ("uSunColor", sun_color),
            ("uAmbient", ambient),
            ("uContrast", contrast),
            ("uPreLitColors_u", pre_lit),
            ("uFogStart", fog_start),
            ("uFogEnd", fog_end),
            ("uFogEnabled_u", fog_enabled),
            ("uTextureEnabled_u", texture_enabled),
            ("uFogColor", fog_color),
            ("uAlphaThreshold", alpha_threshold),
            ("uSpecularLevel", specular_level),
            ("uEffectEnabled_u", effect_enabled),
            ("uLimitedTextureUnits_u", limited_texture_units),
        ]
    );
    assert_eq!(offset_of!(HdWaterUniform, vp), water::VIEW_PROJECTION);
    assert_eq!(
        offset_of!(HdWaterUniform, has_wave_noise),
        water::HAS_WAVE_NOISE
    );
}

#[test]
fn client_fixture_matches_pass_table() {
    let path = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/../client/src/test/resources/pass-layouts.json"
    );
    let lines: Vec<String> = pass::ALL
        .iter()
        .map(|p| {
            let members: Vec<String> = block(&module(&format!("{}.vert", p.shader()))).iter().map(|(n, o)| format!("[\"{n}\",{o}]")).collect();
            let attrs: Vec<String> = p.attributes().iter().map(|a| format!("[{},{},{}]", a.shader_location, a.offset, a.format.size())).collect();
            format!(
                "{{\"pass\":\"{}\",\"id\":{},\"stride\":{},\"attrs\":[{}],\"size\":{},\"members\":[{}]}}",
                p.shader(),
                *p as u32,
                p.stride(),
                attrs.join(","),
                p.block_size(),
                members.join(",")
            )
        })
        .collect();
    let text = format!("[\n{}\n]\n", lines.join(",\n"));
    if std::env::var_os("PASS_LAYOUTS_WRITE").is_some() {
        std::fs::write(path, &text).unwrap();
    }
    assert_eq!(
        std::fs::read_to_string(path).unwrap_or_default(),
        text,
        "stale fixture, rerun with PASS_LAYOUTS_WRITE=1"
    );
}

#[test]
fn water_vertex_matches_shader_inputs() {
    let m = module("hdWater.vert");
    let inputs: Vec<_> = m.entry_points[0]
        .function
        .arguments
        .iter()
        .filter_map(|a| match (&a.binding, &m.types[a.ty].inner) {
            (Some(naga::Binding::Location { location, .. }), TypeInner::Vector { size, .. }) => {
                Some((*location, *size))
            }
            _ => None,
        })
        .collect();
    assert_eq!(
        inputs,
        [
            (0, VectorSize::Tri),
            (1, VectorSize::Quad),
            (2, VectorSize::Tri)
        ]
    );
}
