use openrs_interface::layout::{update_dimensions, NoHost};
use openrs_interface::{Component, ParamValue, Script, ScriptArg};

const DECODE: &str = include_str!("fixtures/decode.native.tsv");
const LAYOUT: &str = include_str!("fixtures/layout.native.tsv");

fn unhex(text: &str) -> Vec<u8> {
    (0..text.len() / 2)
        .map(|i| u8::from_str_radix(&text[i * 2..i * 2 + 2], 16).unwrap())
        .collect()
}

fn hex(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

fn text(value: &str) -> String {
    format!("s{}", hex(value.as_bytes()))
}

fn opt_text(value: &Option<String>) -> String {
    value.as_deref().map_or("-".to_string(), text)
}

fn b(value: bool) -> String {
    if value { "1" } else { "0" }.to_string()
}

fn ints(values: &[i32]) -> String {
    if values.is_empty() {
        return "e".to_string();
    }
    values
        .iter()
        .map(i32::to_string)
        .collect::<Vec<_>>()
        .join(",")
}

fn opt_ints(values: &Option<Vec<i32>>) -> String {
    values.as_deref().map_or("-".to_string(), ints)
}

fn script(value: &Script) -> String {
    let Some(args) = value else {
        return "-".to_string();
    };
    args.iter()
        .map(|arg| match arg {
            ScriptArg::Null => "n".to_string(),
            ScriptArg::Int(v) => format!("i{v}"),
            ScriptArg::Str(v) => text(v),
        })
        .collect::<Vec<_>>()
        .join(",")
}

fn keys(c: &Component) -> String {
    let (Some(actions), Some(params)) = (&c.key_trigger_action, &c.key_trigger_param) else {
        return "-".to_string();
    };
    (0..11)
        .map(|i| match (actions[i], params[i]) {
            (Some(a), Some(p)) => format!("{a}/{p}"),
            _ => "n".to_string(),
        })
        .collect::<Vec<_>>()
        .join(",")
}

fn params(c: &Component) -> String {
    let mut out: Vec<String> = c
        .client_params
        .iter()
        .map(|(key, value)| match value {
            ParamValue::Int(v) => format!("{key}:i:{v}"),
            ParamValue::Str(v) => format!("{key}:s:{}", text(v)),
        })
        .collect();
    out.sort();
    if out.is_empty() {
        "-".to_string()
    } else {
        out.join(";")
    }
}

fn dump(c: &Component) -> String {
    let mut f: Vec<String> = Vec::new();
    f.push(c.kind.map_or(-1, |t| t.id()).to_string());
    f.push(opt_text(&c.name));
    f.push(c.content_type.map_or(-1, |t| t.id()).to_string());
    for v in [c.base_x, c.base_y, c.base_width, c.base_height] {
        f.push(v.to_string());
    }
    for v in [
        c.aspect_width_type,
        c.aspect_height_type,
        c.aspect_x_type,
        c.aspect_y_type,
    ] {
        f.push(v.to_string());
    }
    f.push(c.parent_id.to_string());
    f.push(b(c.hidden));
    f.push(b(c.prevent_click_through));
    f.push(c.scroll_width.to_string());
    f.push(c.scroll_height.to_string());
    f.push(c.sprite_id.to_string());
    f.push(c.angle_2d.to_string());
    f.push(b(c.tiling));
    f.push(b(c.alpha));
    f.push(c.transparency.to_string());
    f.push(c.border_thickness.to_string());
    f.push(c.sprite_shadow.to_string());
    f.push(b(c.flip_vertical));
    f.push(b(c.flip_horizontal));
    f.push(c.color.to_string());
    f.push(b(c.click_mask));
    f.push(c.primary_media_id.to_string());
    f.push(b(c.has_origin));
    f.push(b(c.uses_orthogonal));
    f.push(b(c.priority_render));
    for v in [c.origin_x, c.origin_y, c.origin_z] {
        f.push(v.to_string());
    }
    for v in [c.sprite_pitch, c.sprite_roll, c.sprite_yaw, c.sprite_scale] {
        f.push(v.to_string());
    }
    for v in [c.animation, c.aspect_width, c.aspect_height, c.font_id] {
        f.push(v.to_string());
    }
    f.push(b(c.monospaced));
    f.push(text(&c.text));
    for v in [c.line_spacing, c.text_horizontal_ali, c.text_vertical_ali] {
        f.push(v.to_string());
    }
    f.push(b(c.shadow));
    f.push(c.max_text_lines.to_string());
    f.push(b(c.filled));
    f.push(c.line_width.to_string());
    f.push(b(c.line_direction));
    f.push(keys(c));
    f.push(c.cycle_steps.map_or("-".to_string(), |v| ints(&v)));
    f.push(c.key_char_codes.map_or("-".to_string(), |v| ints(&v)));
    f.push(b(c.has_interaction));
    f.push(text(&c.op_base));
    f.push(match &c.options {
        None => "-".to_string(),
        Some(o) if o.is_empty() => "e".to_string(),
        Some(o) => o.iter().map(|s| text(s)).collect::<Vec<_>>().join(","),
    });
    f.push(opt_ints(&c.op_cursors));
    f.push(opt_text(&c.resume_text));
    f.push(c.drag_deadzone.to_string());
    f.push(c.drag_dead_time.to_string());
    f.push(c.drag_type.map_or(-1, |d| d.value()).to_string());
    f.push(text(&c.target_verb));
    for v in [
        c.target_over_cursor,
        c.target_leave_cursor,
        c.move_over_cursor,
        c.target_params.settings_hash,
        c.target_params.interface_id,
    ] {
        f.push(v.to_string());
    }
    f.push(params(c));
    for s in [
        &c.on_load,
        &c.on_mouse_over,
        &c.on_mouse_leave,
        &c.hook_params,
        &c.on_target_enter,
        &c.on_varp_transmit,
        &c.mouse_leave_script,
        &c.on_stat_transmit,
        &c.on_timer,
        &c.params,
        &c.on_target_leave,
        &c.popup_script,
        &c.on_click,
        &c.on_click_repeat,
        &c.on_release,
        &c.on_hold,
        &c.on_drag,
        &c.on_drag_complete,
        &c.on_mouse_move,
        &c.on_key,
        &c.on_scroll_wheel,
        &c.on_key_type,
    ] {
        f.push(script(s));
    }
    f.push(b(c.has_script));
    for v in [
        &c.varps,
        &c.mouse_leave_array_params,
        &c.stat_transmit_filter,
        &c.key_press_array,
        &c.mouse_wheel_array,
    ] {
        f.push(opt_ints(v));
    }
    f.join("\t")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn decode_matches_native_component_decode() {
    let mut ok = 0;
    let mut error = 0;
    let mut total = None;
    for line in DECODE.lines() {
        let cols: Vec<&str> = line.splitn(6, '\t').collect();
        match cols[0] {
            "T" => {
                let id_hash: i32 = cols[2].parse().unwrap();
                let bytes = unhex(cols[3]);
                let status = cols[4];
                let rest = cols[5];
                let result = Component::decode(&bytes, id_hash);
                if status == "ok" {
                    let component = result
                        .unwrap_or_else(|e| panic!("case {} native ok, rust error {e}", cols[1]));
                    let got = dump(&component);
                    assert_eq!(got, rest, "case {}", cols[1]);
                    ok += 1;
                } else {
                    assert!(
                        result.is_err(),
                        "case {} native {status}, rust decoded",
                        cols[1]
                    );
                    error += 1;
                }
            }
            "Z" => total = Some((cols[2].parse::<usize>().unwrap(), cols[3].parse::<usize>().unwrap())),
            other => panic!("unexpected record {other}"),
        }
    }
    assert_eq!(total, Some((ok, error)));
    assert!(ok > 1500 && error > 100);
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn layout_matches_native_update_interface_dimensions() {
    struct Case {
        width: i32,
        height: i32,
        components: Vec<Option<Component>>,
    }
    let mut case: Option<Case> = None;
    let mut native_ok = 0;
    let mut checked = 0;
    let mut expected_ok = None;
    let mut index_of = 0usize;
    let mut iface = 0;
    let mut ran: Option<Result<(), ()>> = None;
    for line in LAYOUT.lines() {
        let cols: Vec<&str> = line.split('\t').collect();
        match cols[0] {
            "L" => {
                iface = cols[2].parse::<i32>().unwrap();
                case = Some(Case {
                    width: cols[3].parse().unwrap(),
                    height: cols[4].parse().unwrap(),
                    components: Vec::new(),
                });
                index_of = 0;
                ran = None;
            }
            "N" => {
                case.as_mut().unwrap().components.push(None);
                index_of += 1;
            }
            "P" => {
                let bytes = unhex(cols[2]);
                let mut c = Component::decode(&bytes, (iface << 16) | index_of as i32).unwrap();
                c.aspect_x = cols[3].parse().unwrap();
                c.aspect_y = cols[4].parse().unwrap();
                c.scroll_x = cols[5].parse().unwrap();
                c.scroll_y = cols[6].parse().unwrap();
                case.as_mut().unwrap().components.push(Some(c));
                index_of += 1;
            }
            "R" => {
                let c = case.as_mut().unwrap();
                let result = update_dimensions(&mut c.components, -1, c.width, c.height, false, &mut NoHost);
                expected_ok = Some(cols[2] == "ok");
                ran = Some(result.map_err(|_| ()));
                if cols[2] == "ok" {
                    native_ok += 1;
                } else {
                    assert!(ran.unwrap().is_err(), "layout case {} native {}", cols[1], cols[2]);
                }
            }
            "V" => {
                if expected_ok == Some(true) {
                    assert_eq!(ran, Some(Ok(())), "layout case");
                    let k: usize = cols[1].parse().unwrap();
                    let c = case.as_ref().unwrap().components[k].as_ref().unwrap();
                    let got = [c.x, c.y, c.width, c.height, c.scroll_x, c.scroll_y];
                    let want: Vec<i32> = cols[2..8].iter().map(|v| v.parse().unwrap()).collect();
                    assert_eq!(got.to_vec(), want, "iface {iface} component {k}");
                    checked += 1;
                }
            }
            "Z" => {
                assert_eq!(cols[2].parse::<usize>().unwrap(), native_ok);
            }
            other => panic!("unexpected record {other}"),
        }
    }
    assert!(checked > 1000);
    assert!(ran.is_some());

}
