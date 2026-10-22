use crate::input::{plan, Rect, Step};
use crate::{fleet, fleet_up, keys, proc};
use serde_json::{json, Value};

pub static ENV: std::sync::Mutex<()> = std::sync::Mutex::new(());

fn s(a: &[&str]) -> Vec<String> {
    a.iter().map(|x| x.to_string()).collect()
}

fn rect() -> Rect {
    Rect::from(&json!({ "left": 10.0, "top": 20.0, "width": 320.0, "height": 240.0, "cw": 640, "ch": 480 })).unwrap()
}

fn sent(steps: &[Step]) -> Vec<(&'static str, Value)> {
    steps.iter().filter_map(|s| if let Step::Send(m, p) = s { Some((*m, p.clone())) } else { None }).collect()
}

#[test]
fn canvas_pixels_map_to_css_pixels() {
    assert_eq!(rect().css(0.0, 0.0), (10.0, 20.0));
    assert_eq!(rect().css(640.0, 480.0), (330.0, 260.0));
    assert_eq!(rect().css(100.0, 50.0), (60.0, 45.0));
    assert!(Rect::from(&json!({ "left": 0, "top": 0, "width": 0, "height": 1, "cw": 1, "ch": 1 })).is_err());
}

#[test]
fn click_is_move_press_release_at_the_mapped_point() {
    let e = sent(&plan("click", &s(&["100", "50", "right"]), &rect()).unwrap());
    let kinds: Vec<&str> = e.iter().map(|x| x.1["type"].as_str().unwrap()).collect();
    assert_eq!(kinds, ["mouseMoved", "mousePressed", "mouseReleased"]);
    assert!(e.iter().all(|x| x.1["x"] == 60.0 && x.1["y"] == 45.0));
    assert_eq!((e[1].1["button"].clone(), e[1].1["buttons"].clone(), e[1].1["clickCount"].clone()), (json!("right"), json!(2), json!(1)));
    assert_eq!(e[2].1["buttons"], 0);
    assert!(plan("click", &s(&["1"]), &rect()).is_err());
    assert!(plan("click", &s(&["1", "2", "fourth"]), &rect()).is_err());
}

#[test]
fn dblclick_counts_two_clicks() {
    let e = sent(&plan("dblclick", &s(&["1", "1"]), &rect()).unwrap());
    assert_eq!(e.iter().map(|x| x.1["clickCount"].as_u64().unwrap()).collect::<Vec<_>>(), [0, 1, 1, 2, 2]);
}

#[test]
fn drag_holds_the_button_while_moving() {
    let steps = plan("drag", &s(&["0", "0", "640", "480"]), &rect()).unwrap();
    let e = sent(&steps);
    assert_eq!(e.len(), 11);
    assert_eq!(steps.iter().filter(|s| **s == Step::Pause(16)).count(), 8);
    assert!(e[2..10].iter().all(|x| x.1["type"] == "mouseMoved" && x.1["buttons"] == 1));
    assert_eq!((e[10].1["x"].clone(), e[10].1["y"].clone(), e[10].1["type"].clone()), (json!(330.0), json!(260.0), json!("mouseReleased")));
}

#[test]
fn scroll_defaults_to_the_canvas_centre() {
    let e = sent(&plan("scroll", &s(&["0", "120"]), &rect()).unwrap());
    assert_eq!((e[1].1["type"].clone(), e[1].1["x"].clone(), e[1].1["deltaY"].clone()), (json!("mouseWheel"), json!(170.0), json!(120.0)));
    let e = sent(&plan("scroll", &s(&["5", "-5", "0", "0"]), &rect()).unwrap());
    assert_eq!((e[1].1["x"].clone(), e[1].1["deltaX"].clone()), (json!(10.0), json!(5.0)));
}

#[test]
fn key_events_carry_code_text_and_modifiers() {
    let e = sent(&plan("key", &s(&["press", "KeyA"]), &rect()).unwrap());
    assert_eq!((e[0].1["type"].clone(), e[0].1["key"].clone(), e[0].1["text"].clone(), e[0].1["windowsVirtualKeyCode"].clone()), (json!("keyDown"), json!("a"), json!("a"), json!(65)));
    assert_eq!(e[1].1["type"], "keyUp");
    let e = sent(&plan("key", &s(&["down", "ArrowLeft"]), &rect()).unwrap());
    assert_eq!((e[0].1["type"].clone(), e[0].1.get("text").cloned()), (json!("rawKeyDown"), None));
    let e = sent(&plan("key", &s(&["press", "Shift+KeyA"]), &rect()).unwrap());
    assert_eq!((e[0].1["key"].clone(), e[0].1["modifiers"].clone()), (json!("A"), json!(8)));
    let e = sent(&plan("key", &s(&["down", "Ctrl+KeyC"]), &rect()).unwrap());
    assert!(e[0].1.get("text").is_none());
    assert!(plan("key", &s(&["press", "Nope"]), &rect()).is_err());
    assert!(plan("key", &s(&["hold", "KeyA"]), &rect()).is_err());
    assert!(plan("key", &s(&["press", "Hyper+KeyA"]), &rect()).is_err());
}

#[test]
fn typing_maps_characters_to_key_presses() {
    let e = sent(&plan("type", &s(&["Hi", "5", "é"]), &rect()).unwrap());
    let codes: Vec<String> = e.iter().filter(|x| x.0 == "Input.dispatchKeyEvent" && x.1["type"] == "keyDown").map(|x| x.1["code"].as_str().unwrap().to_string()).collect();
    assert_eq!(codes, ["KeyH", "KeyI", "Space", "Digit5", "Space"]);
    assert_eq!(e[0].1["text"], "H");
    assert_eq!(e.last().unwrap().0, "Input.insertText");
    assert!(plan("type", &[], &rect()).is_err());
}

#[test]
fn key_table_covers_named_function_and_punctuation_keys() {
    assert_eq!(keys::lookup("F5", 0).unwrap().vk, 116);
    assert_eq!(keys::lookup("Enter", 0).unwrap().text.as_deref(), Some("\r"));
    assert_eq!(keys::lookup("Digit7", 0).unwrap().vk, 55);
    assert_eq!(keys::lookup("Minus", 0).unwrap().vk, 189);
    assert!(keys::lookup("F13", 0).is_none() && keys::lookup("KeyAB", 0).is_none());
    assert_eq!(keys::code_of('Z'), Some(("KeyZ".into(), keys::SHIFT)));
    assert_eq!(keys::code_of('/'), Some(("Slash".into(), 0)));
    assert_eq!(keys::code_of('!'), None);
    assert_eq!(keys::parse("Ctrl+Shift+KeyA").unwrap(), ("KeyA".into(), 10));
}

#[test]
fn sleep_pauses_without_events() {
    assert_eq!(plan("sleep", &s(&["25"]), &rect()).unwrap(), vec![Step::Pause(25)]);
    assert!(plan("sleep", &[], &rect()).is_err());
}

#[test]
fn fleet_arguments_scenes_and_windows() {
    let p = fleet_up::parse(&s(&["3", "--scene", "model", "--scene", "water", "--mute-audio"])).unwrap();
    assert_eq!(p.count, 3);
    assert_eq!((p.args(1), p.args(2), p.args(3)), (s(&["model", "--mute-audio"]), s(&["water", "--mute-audio"]), s(&["water", "--mute-audio"])));
    assert_eq!(fleet_up::parse(&[]).unwrap().count, 9);
    assert_eq!(fleet_up::parse(&[]).unwrap().args(4), Vec::<String>::new());
    assert!(fleet_up::parse(&s(&["10"])).is_err() && fleet_up::parse(&s(&["0"])).is_err() && fleet_up::parse(&s(&["--scene"])).is_err());
    assert_eq!(fleet_up::window(1).to_vec(), s(&["--window-position=0,0", "--window-size=480,340"]));
    assert_eq!(fleet_up::window(5)[0], "--window-position=480,340");
    assert_eq!(fleet_up::window(9)[0], "--window-position=960,680");
    let all: Vec<String> = (1..=9).map(|n| fleet_up::window(n)[0].clone()).collect();
    let mut unique = all.clone();
    unique.sort();
    unique.dedup();
    assert_eq!(unique.len(), 9);
}

#[test]
fn fleet_state_is_per_slot_and_crashes_are_found() {
    let _guard = ENV.lock().unwrap_or_else(|e| e.into_inner());
    let d = std::env::temp_dir().join(format!("openrs-web-fleet-{}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::env::set_var("OPENRS_WEB_FLEET_STATE", &d);
    assert_eq!(fleet::slot_dir(2), d.join("s2"));
    assert_eq!(fleet::crashed(), None);
    fleet::shared();
    crate::state::write("slots", "3");
    assert_eq!(fleet::slots(), 3);
    assert_eq!(fleet::crashed(), None);
    fleet::select(2);
    assert_eq!(std::env::var("OPENRS_WEB_FOOTPRINTS").unwrap(), d.join("s2/fp").display().to_string());
    crate::state::write("browser", "4000000000 9222 ws://127.0.0.1:9222/devtools/browser/x");
    assert_eq!(fleet::crashed(), Some(3));
    assert_eq!(fleet::run(&s(&["do", "10", "state"])), 2);
    assert_eq!(fleet::run(&s(&["nope"])), 2);
    std::fs::remove_dir_all(&d).unwrap();
}

#[test]
fn process_measuring_helpers() {
    let g = proc::parse_gpu("3881664, 134\n1015305, 67\nbad\n");
    assert_eq!((g.len(), g[&3881664], g[&1015305]), (2, 134, 67));
    let me = std::process::id();
    assert!(proc::alive(me));
    assert!(!proc::alive(4_000_000_000));
    assert!(proc::mem(me).0 > 0);
    assert!(proc::ticks(me) < u64::MAX);
}
