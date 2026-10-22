use openrs_cs2::FontMetrics;
use openrs_interface_raster::{decode_sprites, Font, Frame, SpriteGroup};

const FONT: &str = include_str!("fixtures/font.native.tsv");

fn unhex(hex: &str) -> Vec<u8> {
    (0..hex.len()).step_by(2).map(|i| u8::from_str_radix(&hex[i..i + 2], 16).unwrap()).collect()
}

fn units(hex: &str) -> Vec<u16> {
    unhex(hex).chunks(2).map(|c| u16::from_be_bytes([c[0], c[1]])).collect()
}

fn encode(units: &[u16]) -> String {
    units.iter().map(|u| format!("{:04x}", u)).collect()
}

fn lines(list: &[Vec<u16>]) -> String {
    if list.is_empty() {
        return "e".into();
    }
    list.iter().map(|l| encode(l)).collect::<Vec<_>>().join(";")
}

fn font(bytes: &[u8]) -> Font {
    let metrics = FontMetrics::decode(bytes).expect("native accepted the metrics");
    let frame = Frame { min_x: 0, min_y: 0, width: 1, height: 1, pixels: vec![0xffff_ffff], has_alpha: false, mask: vec![255] };
    let group = SpriteGroup { biggest_width: 1, biggest_height: 1, frames: vec![frame; 256] };
    Font::new(metrics, &group).unwrap()
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn layout_and_sprites_match_the_native_client() {
    let mut fonts: Vec<Font> = Vec::new();
    let mut cases = 0;
    let mut sprites = 0;
    let mut summary = None;
    for line in FONT.lines() {
        let cols: Vec<&str> = line.split('\t').collect();
        match cols[0] {
            "M" => fonts.push(font(&unhex(cols[2]))),
            "C" => {
                let f = &fonts[cols[1].parse::<usize>().unwrap()];
                let text = units(cols[2]);
                let limit: i32 = cols[3].parse().unwrap();
                assert_eq!(f.text_width(&text, &[]).to_string(), cols[4], "width of case {cases}");
                assert_eq!(encode(&f.fit_text(&text, limit, &[])), cols[5], "fit of case {cases}");
                assert_eq!(lines(&f.split_lines(&text, None, &[])), cols[6], "unwrapped lines of case {cases}");
                assert_eq!(lines(&f.split_lines(&text, Some(limit), &[])), cols[7], "wrapped lines of case {cases}");
                cases += 1;
            }
            "P" => {
                let group = decode_sprites(&unhex(cols[2])).expect("native decoded the sprite");
                let mut got = Vec::new();
                for f in &group.frames {
                    let pixels: Vec<String> = f.pixels.iter().map(|p| format!("{p:x}")).collect();
                    let mask: Vec<String> = f.mask.iter().map(|m| m.to_string()).collect();
                    got.push(format!(
                        "{},{},{},{},{},{},{},{}",
                        f.min_x, f.min_y, f.width, f.height, group.biggest_width, group.biggest_height,
                        pixels.join("."), mask.join(".")
                    ));
                }
                assert_eq!(got.join(";"), cols[3], "sprite {}", cols[1]);
                sprites += 1;
            }
            "Z" => summary = Some((cols[2].parse::<usize>().unwrap(), cols[3].parse::<usize>().unwrap(), cols[4].parse::<usize>().unwrap())),
            other => panic!("unknown record {other}"),
        }
    }
    assert_eq!(summary, Some((fonts.len(), cases, sprites)));
    assert!(cases >= 700);
}
