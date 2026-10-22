use naga::{ShaderStage, back::wgsl, front::glsl, valid};
use std::{collections::HashMap, fs, path::Path};

const PAIRS: [&str; 15] = [
    "batched",
    "ground",
    "groundTextured",
    "hdWater",
    "materialCubeSpec",
    "model",
    "particle",
    "shadow",
    "sprite",
    "waterfall",
    "water",
    "waterHD",
    "waterLapping",
    "waterReflecting",
    "waterRippling",
];

const PATCHES: [(&str, &str, &str); 21] = [
    (
        "ground.vert",
        "texture(uGroundShadowTex, aPosition.xz * uGroundShadowScale)",
        "textureLod(uGroundShadowTex, aPosition.xz * uGroundShadowScale, 0.0)",
    ),
    (
        "ground.frag",
        "texture(uShadowMap, projCoords.xy + offset)",
        "textureLod(uShadowMap, projCoords.xy + offset, 0.0)",
    ),
    (
        "sprite.frag",
        "uniform sampler2D uTextures[8];",
        "uniform sampler2DArray uTextures;",
    ),
    (
        "sprite.frag",
        "if (vTexIndex >= 0)",
        "if (vTexIndex >= 0 || vTexIndex == -3)",
    ),
    (
        "sprite.frag",
        "if (fragColor.a < 0.004)",
        "if (vTexIndex != -2 && vTexIndex != -3 && fragColor.a < 0.004)",
    ),
    (
        "shadow.frag",
        "texture(uTexture, vTexCoord).a",
        "textureLod(uTexture, vTexCoord, 0.0).a",
    ),
    (
        "groundTextured.vert",
        "gl_ClipDistance[0] = ",
        "vClipDistance = ",
    ),
    (
        "groundTextured.frag",
        "vec2 ndc = (gl_FragCoord.xy - uViewport.xy)",
        "vec2 ndc = (vec2(gl_FragCoord.x, uFramebufferSize.y - gl_FragCoord.y) - uViewport.xy)",
    ),
    (
        "hdWater.frag",
        "#version 330 core\n",
        "#version 330 core\n#define flipUv(p) (vec2((p).x, uFramebufferSize.y - (p).y) / uFramebufferSize)\nvec2 planarUv(vec2 p, vec2 n) { return flipUv(p) + n * uPlanarDistort * vec2(1.0, -1.0); }\n",
    ),
    (
        "hdWater.frag",
        "texture(uWaveNoise, vec3(xz / scale + shift, time)).r",
        "textureLod(uWaveNoise, vec3(xz / scale + shift, time), 0.0).r",
    ),
    (
        "hdWater.frag",
        "gl_FragCoord.xy",
        "vec2(gl_FragCoord.x, uFramebufferSize.y - gl_FragCoord.y)",
    ),
    ("hdWater.frag", "ndc.z * 0.5 + 0.5", "ndc.z"),
    (
        "hdWater.frag",
        "texture(uSceneDepth, fragXY / uFramebufferSize)",
        "texture(uSceneDepth, flipUv(fragXY))",
    ),
    (
        "hdWater.frag",
        "texture(uSceneColor, hit.xy / uFramebufferSize)",
        "texture(uSceneColor, flipUv(hit.xy))",
    ),
    (
        "hdWater.frag",
        "texture(uSceneColor, refracted / uFramebufferSize)",
        "texture(uSceneColor, flipUv(refracted))",
    ),
    (
        "hdWater.frag",
        "texture(uReflection, vec2(gl_FragCoord.x, uFramebufferSize.y - gl_FragCoord.y) / uFramebufferSize + n.xz * uPlanarDistort)",
        "texture(uReflection, planarUv(vec2(gl_FragCoord.x, uFramebufferSize.y - gl_FragCoord.y), n.xz))",
    ),
    ("hdWater.tese", "    vec3 b = gl_TessCoord;\n", ""),
    (
        "hdWater.tese",
        "teWorld[0] * b.x + teWorld[1] * b.y + teWorld[2] * b.z",
        "aPosition",
    ),
    (
        "hdWater.tese",
        "teEdge[0] * b.x + teEdge[1] * b.y + teEdge[2] * b.z",
        "aColor.a",
    ),
    (
        "hdWater.tese",
        "teDepth[0] * b.x + teDepth[1] * b.y + teDepth[2] * b.z",
        "aTexCoord.z",
    ),
    (
        "hdWater.tese",
        "texture(uWaveNoise, vec3(xz / scale + shift, time))",
        "textureLod(uWaveNoise, vec3(xz / scale + shift, time), 0.0)",
    ),
];

fn patch(name: &str, src: &str) -> String {
    let mut s = src.to_string();
    for (f, a, b) in PATCHES {
        if f == name {
            assert!(s.contains(a), "{name}: patch target missing: {a}");
            s = s.replace(a, b);
        }
    }
    if name == "sprite.frag" {
        let a = s.find("switch (vTexIndex)").unwrap();
        let b = s[a..]
            .find("texColor = texture(uTextures[7], vTexCoord); break;")
            .unwrap()
            + a;
        let end = s[b..].find('}').unwrap() + b + 1;
        s.replace_range(
            a..end,
            "texColor = textureLod(uTextures, vec3(vTexCoord, float(max(vTexIndex, 0))), 0.0);",
        );
    }
    if name == "groundTextured.vert" {
        s = s.replacen("void main", "out float vClipDistance;\nvoid main", 1);
    }
    if name == "groundTextured.frag" {
        s = s.replacen(
            "void main() {",
            "in float vClipDistance;\nvoid main() {\n    if (vClipDistance < 0.0) discard;",
            1,
        );
    }
    s
}

fn wave_vertex(dir: &Path) -> String {
    let read = |f: &str| fs::read_to_string(dir.join(f)).unwrap();
    let attributes: Vec<String> = read("hdWaterTess.vert")
        .lines()
        .filter(|l| l.starts_with("layout(location"))
        .map(str::to_owned)
        .collect();
    let tese = patch("hdWater.tese", &read("hdWater.tese"));
    let body = &tese[tese.find("uniform mat4 uViewProjection;").unwrap()..];
    format!(
        "#version 330 core\n{}\nout vec3 vWorld;\nout float vEdge;\n{body}",
        attributes.join("\n")
    )
}

fn split_uniform(line: &str) -> Option<(String, String)> {
    let t = line.trim();
    let body = t
        .strip_prefix("uniform ")?
        .split("//")
        .next()?
        .trim()
        .strip_suffix(';')?;
    let (ty, name) = body.split_once(' ')?;
    Some((ty.trim().to_string(), name.trim().to_string()))
}

fn collect(sources: &[&str]) -> (Vec<(String, String)>, Vec<(String, String)>) {
    let mut params: Vec<(String, String)> = vec![];
    let mut samplers: Vec<(String, String)> = vec![];
    for s in sources {
        for l in s.lines() {
            if let Some((ty, name)) = split_uniform(l) {
                let list = if ty.starts_with("sampler") {
                    &mut samplers
                } else {
                    &mut params
                };
                if !list.iter().any(|(_, n)| n == &name) {
                    list.push((ty, name));
                }
            }
        }
    }
    (params, samplers)
}

fn varying(l: &str, kw: &str) -> Option<(String, String)> {
    let body = l.split("//").next()?.trim();
    if body.starts_with("layout") || !body.ends_with(';') {
        return None;
    }
    let mut words: Vec<&str> = body.trim_end_matches(';').split_whitespace().collect();
    if words.first() == Some(&"flat") {
        words.remove(0);
    }
    (words.len() == 3 && words[0] == kw).then(|| (words[2].to_string(), words[1].to_string()))
}

fn transform(
    src: &str,
    stage: ShaderStage,
    params: &[(String, String)],
    samplers: &[(String, String)],
    vout: &[String],
    defines: &[String],
) -> String {
    let mut head = String::from("#version 450\n");
    for l in defines {
        head += l;
        head += "\n";
    }
    head += &format!("layout(std140, set = 0, binding = 0) uniform P {{\n");
    let mut defs = String::new();
    for (ty, name) in params {
        if ty == "bool" {
            head += &format!("uint {name}_u;\n");
            defs += &format!("#define {name} ({name}_u != 0u)\n");
        } else if let Some((base, arr)) = name.split_once('[') {
            let ty = if ty == "vec2" { "vec4" } else { ty };
            head += &format!("{ty} {base}[{arr};\n");
        } else {
            head += &format!("{ty} {name};\n");
        }
    }
    head += "};\n";
    for (i, (ty, name)) in samplers.iter().enumerate() {
        head += &format!(
            "layout(set = 1, binding = {}) uniform {} {name};\n",
            2 * i,
            ty.replacen("sampler", "texture", 1)
        );
        head += &format!(
            "layout(set = 1, binding = {}) uniform sampler {name}_s;\n",
            2 * i + 1
        );
    }
    head += &defs;
    let mut out = head;
    let mut loc = 0;
    let mut src = src.to_string();
    for (ty, name) in samplers {
        for f in ["texture", "textureLod"] {
            src = src.replace(
                &format!("{f}({name},"),
                &format!("{f}({ty}({name}, {name}_s),"),
            );
        }
    }
    for l in src.lines() {
        if l.trim().starts_with("#version")
            || l.trim().starts_with("#define")
            || split_uniform(l).is_some()
        {
            continue;
        }
        let kw = if stage == ShaderStage::Vertex {
            "out"
        } else {
            "in"
        };
        if let Some((name, _)) = varying(l, kw) {
            let idx = vout
                .iter()
                .position(|n| n == &name)
                .unwrap_or(vout.len() + loc);
            loc += usize::from(!vout.contains(&name));
            out += &format!("layout(location = {idx}) {l}\n");
        } else if stage == ShaderStage::Fragment && varying(l, "out").is_some() {
            out += &format!("layout(location = 0) {l}\n");
        } else {
            out += l;
            out += "\n";
        }
    }
    out
}

fn compile(src: &str, stage: ShaderStage) -> Result<String, String> {
    let module = glsl::Frontend::default()
        .parse(&glsl::Options::from(stage), src)
        .map_err(|e| format!("{:?}", e.errors.first()))?;
    let info = valid::Validator::new(valid::ValidationFlags::all(), valid::Capabilities::all())
        .validate(&module)
        .map_err(|e| format!("{:?}", e.into_inner()))?;
    wgsl::write_string(&module, &info, wgsl::WriterFlags::empty()).map_err(|e| e.to_string())
}

fn main() {
    let dir = std::env::args()
        .nth(1)
        .unwrap_or_else(|| "../client/src/main/resources/shaders".into());
    let out = Path::new("shaders");
    let mut report: HashMap<String, String> = HashMap::new();
    for name in PAIRS {
        let vname = format!("{name}.vert");
        let fname = format!("{name}.frag");
        let v = if name == "hdWater" {
            wave_vertex(Path::new(&dir))
        } else {
            patch(
                &vname,
                &fs::read_to_string(Path::new(&dir).join(&vname)).unwrap(),
            )
        };
        let f = patch(
            &fname,
            &fs::read_to_string(Path::new(&dir).join(&fname)).unwrap(),
        );
        let (params, samplers) = collect(&[&v, &f]);
        let defines: Vec<String> = [&v, &f]
            .into_iter()
            .flat_map(|s| s.lines())
            .filter(|l| l.trim().starts_with("#define"))
            .map(String::from)
            .collect();
        let vout: Vec<String> = v
            .lines()
            .filter_map(|l| varying(l, "out"))
            .map(|(n, _)| n)
            .collect();
        for (file, src, stage) in [
            (vname, v, ShaderStage::Vertex),
            (fname, f, ShaderStage::Fragment),
        ] {
            let text = transform(&src, stage, &params, &samplers, &vout, &defines);
            if std::env::var("DUMP").is_ok() {
                fs::write(format!("/tmp/{file}.glsl"), &text).unwrap();
            }
            let status = match compile(&text, stage) {
                Ok(w) => {
                    let w = if stage == ShaderStage::Fragment {
                        format!("diagnostic(off, derivative_uniformity);\n\n{w}")
                    } else {
                        w
                    };
                    fs::write(out.join(format!("{file}.wgsl")), w).unwrap();
                    "ok".to_string()
                }
                Err(e) => format!("fail {e}"),
            };
            report.insert(file, status);
        }
    }
    let mut keys: Vec<_> = report.keys().cloned().collect();
    keys.sort();
    for k in keys {
        println!("{k}\t{}", report[&k]);
    }
}
