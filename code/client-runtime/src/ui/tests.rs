use super::*;
use openrs_interface::def::ComponentType;
use openrs_login::world_packets::{OpenSub, OpenTop, SubTarget};

fn figure(file: i32, parent: i32, x: i32, y: i32, w: i32, h: i32, color: i32) -> Component {
    let mut c = Component::blank((548 << 16) | file);
    c.kind = Some(ComponentType::Figure);
    c.parent_id = parent;
    c.base_x = x;
    c.base_y = y;
    c.base_width = w;
    c.base_height = h;
    c.color = color;
    c.filled = true;
    c
}

fn container(file: i32, parent: i32, x: i32, y: i32, w: i32, h: i32) -> Component {
    let mut c = figure(file, parent, x, y, w, h, 0);
    c.kind = Some(ComponentType::Container);
    c
}

fn ui_with(components: Vec<Option<Component>>) -> InterfaceUi {
    let mut ui = InterfaceUi::new(40, 30);
    ui.defs.insert(548, components);
    ui.open.top = Some(548);
    ui
}

fn pixel(ui: &InterfaceUi, x: usize, y: usize) -> [u8; 4] {
    let rgba = ui.rgba();
    let at = (y * ui.size().0 as usize + x) * 4;
    [rgba[at], rgba[at + 1], rgba[at + 2], rgba[at + 3]]
}

#[test]
fn open_packets_track_the_interface_tree() {
    let mut open = OpenInterfaces::default();
    assert!(open.apply(&WorldPacket::OpenTop(OpenTop { interface_id: 548, kind: 0, xtea: [0; 4] })));
    let sub = OpenSub { interface_id: 752, parent: (548 << 16) | 20, overlay: 1, xtea: [0; 4], target: SubTarget::None };
    assert!(open.apply(&WorldPacket::OpenSub(sub)));
    assert_eq!(open.subs[&((548 << 16) | 20)], (752, 1));
    assert!(open.apply(&WorldPacket::MoveSub { from: (548 << 16) | 20, to: (548 << 16) | 21 }));
    assert!(!open.apply(&WorldPacket::CloseSub { parent: (548 << 16) | 20 }));
    assert!(open.apply(&WorldPacket::CloseSub { parent: (548 << 16) | 21 }));
    assert!(open.subs.is_empty());
    assert!(!open.apply(&WorldPacket::Empty));
    assert!(open.apply(&WorldPacket::OpenTop(OpenTop { interface_id: 161, kind: 0, xtea: [0; 4] })));
    assert_eq!(open.top, Some(161));
}

#[test]
fn draws_figures_into_the_overlay_and_leaves_the_rest_transparent() {
    let mut ui = ui_with(vec![Some(figure(0, -1, 2, 3, 10, 5, 0xff0000))]);
    let report = ui.frame(&UiInput::default());
    assert!(report.drawn);
    assert_eq!(pixel(&ui, 2, 3), [255, 0, 0, 255]);
    assert_eq!(pixel(&ui, 0, 0), [0, 0, 0, 0]);
    assert_eq!(pixel(&ui, 12, 3), [0, 0, 0, 0]);
}

#[test]
fn sub_interfaces_are_laid_out_inside_their_container() {
    let mut ui = ui_with(vec![Some(container(0, -1, 10, 10, 20, 12))]);
    ui.defs.insert(752, vec![Some(figure(0, -1, 1, 1, 4, 4, 0x00ff00))]);
    ui.open.subs.insert(548 << 16, (752, 1));
    ui.frame(&UiInput::default());
    assert_eq!(pixel(&ui, 11, 11), [0, 255, 0, 255]);
    assert_eq!(pixel(&ui, 10, 10), [0, 0, 0, 0]);
}

#[test]
fn missing_interfaces_are_requested_once_and_decoded_on_arrival() {
    let mut ui = InterfaceUi::new(40, 30);
    ui.open.top = Some(548);
    ui.frame(&UiInput::default());
    assert_eq!(ui.needs(), vec![(INTERFACE_INDEX, 548)]);
    ui.absent(INTERFACE_INDEX, 548);
    assert!(ui.needs().is_empty());
}

#[test]
fn clicks_reach_the_input_state_machine_and_report_hooks() {
    let mut c = figure(0, -1, 0, 0, 10, 10, 0);
    c.on_click = Some(vec![]);
    c.has_script = true;
    let mut ui = ui_with(vec![Some(c)]);
    let report = ui.frame(&UiInput { mouse_x: 3, mouse_y: 3, click: Some((3, 3)), ..UiInput::default() });
    assert_eq!(report.hooks, 1);
    let hooks = ui.hooks();
    assert_eq!(hooks[0].kind, "click");
    assert_eq!(hooks[0].mouse, Some([3, 3]));
}

#[test]
fn typed_keys_become_key_type_hooks_only_for_components_that_listen() {
    let mut c = figure(0, -1, 0, 0, 10, 10, 0);
    c.on_key_type = Some(vec![]);
    c.has_script = true;
    let mut ui = ui_with(vec![Some(c)]);
    let report = ui.frame(&UiInput { keys: vec![(TYPED_CHAR_CODE, 'h' as i32)], ..UiInput::default() });
    assert_eq!(report.hooks, 1);
    assert_eq!(ui.hooks()[0].typed, Some([-1, 'h' as i32]));
}

#[test]
fn rs_key_codes_follow_the_remap_table() {
    assert_eq!(rs_key_code(13, true), 0);
    assert_eq!(rs_key_code(27, true), 13);
    assert_eq!(rs_key_code(65, true), 48);
    assert_eq!(rs_key_code(37, true), 96);
    assert_eq!(rs_key_code(96, true), 0);
    assert_eq!(rs_key_code(96, false), 100);
    assert_eq!(rs_key_code(9999, true), 0);
    assert!(is_valid_key_char('a' as u32));
    assert!(!is_valid_key_char(0));
    assert!(is_valid_key_char(8364));
}
