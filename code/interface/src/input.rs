use std::collections::{HashMap, HashSet};

use crate::def::{Component, ComponentType, ContentType, Script};

pub const KEY_SHIFT: i32 = 81;
pub const KEY_CTRL: i32 = 82;
pub const KEY_ALT: i32 = 86;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpriteMask {
    pub width: i32,
    pub height: i32,
    pub row_start: Vec<i32>,
    pub row_width: Vec<i32>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Bounds {
    pub min_x: i32,
    pub min_y: i32,
    pub max_x: i32,
    pub max_y: i32,
}

impl Bounds {
    pub fn contains(self, x: i32, y: i32) -> bool {
        x >= self.min_x && y >= self.min_y && x < self.max_x && y < self.max_y
    }

    pub fn is_visible(self) -> bool {
        self.min_x < self.max_x && self.min_y < self.max_y
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct KeyPress {
    pub code: i32,
    pub character: i32,
}

#[derive(Debug, Clone)]
pub struct Frame {
    pub cycle: i32,
    pub mouse_x: i32,
    pub mouse_y: i32,
    pub left_down: bool,
    pub click: Option<(i32, i32)>,
    pub wheel_delta: i32,
    pub wheel_cycle: i32,
    pub recent_wheels: [i32; 32],
    pub key_cycle: i32,
    pub recent_keys: [i32; 32],
    pub pressed: Vec<KeyPress>,
    pub held: HashSet<i32>,
    pub console_open: bool,
    pub varp_cycle: i32,
    pub recent_varps: [i32; 32],
    pub container_cycle: i32,
    pub recent_containers: [i32; 32],
    pub stat_cycle: i32,
    pub recent_stats: [i32; 32],
}

impl Frame {
    pub fn new(cycle: i32) -> Self {
        Self {
            cycle,
            mouse_x: -1,
            mouse_y: -1,
            left_down: false,
            click: None,
            wheel_delta: 0,
            wheel_cycle: 0,
            recent_wheels: [0; 32],
            key_cycle: 0,
            recent_keys: [0; 32],
            pressed: Vec::new(),
            held: HashSet::new(),
            console_open: false,
            varp_cycle: 0,
            recent_varps: [0; 32],
            container_cycle: 0,
            recent_containers: [0; 32],
            stat_cycle: 0,
            recent_stats: [0; 32],
        }
    }

    fn held(&self, key: i32) -> bool {
        self.held.contains(&key)
    }
}

#[derive(Debug, Clone, Default)]
pub struct ComponentInput {
    pub pressed: bool,
    pub hooked: bool,
    pub cycles: Option<[i32; 11]>,
    pub last_key_cycle: i32,
    pub last_wheel_cycle: i32,
    pub last_varp_cycle: i32,
    pub last_container_cycle: i32,
    pub last_stat_cycle: i32,
}

#[derive(Debug, Default)]
pub struct InputState {
    pub components: HashMap<i32, ComponentInput>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Queue {
    Pending,
    Mouse,
    Tick,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum HookKind {
    Click,
    ClickRepeat,
    Release,
    Hold,
    MouseOver,
    Popup,
    MouseLeave,
    Timer,
    MouseMove,
    Key,
    ScrollWheel,
    VarpTransmit,
    ContainerTransmit,
    StatTransmit,
    KeyType,
}

#[derive(Debug, Clone, PartialEq)]
pub struct HookRequest {
    pub source: i32,
    pub queue: Queue,
    pub kind: HookKind,
    pub mouse: Option<(i32, i32)>,
    pub typed: Option<(i32, i32)>,
    pub params: Script,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Event {
    PreventClickThrough { id_hash: i32 },
    Hovered { id_hash: i32 },
    Cursor(i32),
    DragAnchor { x: i32, y: i32 },
    DragStart { id_hash: i32, dx: i32, dy: i32 },
    DragHover { id_hash: i32 },
    OptionInteract { op: i32, id_hash: i32, slot_id: i32 },
    UseSelect { id_hash: i32, slot_id: i32 },
    Selected { id_hash: i32, x: i32, y: i32, width: i32, height: i32 },
    Content { content: ContentType, id_hash: i32, bounds: Bounds, x: i32, y: i32, within: bool },
}

#[derive(Debug, Default)]
pub struct InputOutput {
    pub hooks: Vec<HookRequest>,
    pub events: Vec<Event>,
}

pub trait InputHost {
    fn is_hidden(&self, component: &Component) -> bool {
        component.hidden
    }

    fn is_passive(&self, component: &Component) -> bool {
        let _ = component;
        false
    }

    fn sprite_mask(&self, component: &Component) -> Option<SpriteMask> {
        let _ = component;
        None
    }

    fn menu_open(&self) -> bool {
        false
    }

    fn menu_item_count(&self) -> i32 {
        0
    }

    fn dragging(&self) -> Option<i32> {
        None
    }

    fn drag_depth_flags(&self, component: &Component) -> bool {
        let _ = component;
        false
    }

    fn drag_target_enabled(&self, component: &Component) -> bool {
        let _ = component;
        false
    }

    fn selected(&self) -> Option<i32> {
        None
    }

    fn prevent_interaction(&self) -> bool {
        false
    }

    fn selection_active(&self) -> bool {
        false
    }

    fn item_slots(&self, container_id_hash: i32) -> Option<&[Option<Component>]> {
        let _ = container_id_hash;
        None
    }

    fn sub_interface(&self, container_id_hash: i32) -> Option<&[Option<Component>]> {
        let _ = container_id_hash;
        None
    }
}

fn mask_hit(mask: &SpriteMask, x: i32, y: i32) -> bool {
    if y < 0 || y as usize >= mask.row_start.len() {
        return false;
    }
    let start = mask.row_start[y as usize];
    x >= start && x <= start + mask.row_width[y as usize]
}

pub fn sprite_hit_test(c: &Component) -> bool {
    c.click_mask
        && c.kind == Some(ComponentType::Sprite)
        && c.transparency == 0
        && c.container_item_id == -1
        && !c.tiling
        && c.angle_2d == 0
}

#[allow(clippy::too_many_arguments)]
pub fn point_hits(
    host: &dyn InputHost,
    c: &Component,
    bounds: Bounds,
    comp_x: i32,
    comp_y: i32,
    px: i32,
    py: i32,
    unsized_mask_hits: bool,
) -> bool {
    if !bounds.contains(px, py) {
        return false;
    }
    if !sprite_hit_test(c) {
        return true;
    }
    match host.sprite_mask(c) {
        Some(mask) => {
            if mask.width == c.width && mask.height == c.height {
                mask_hit(&mask, px.wrapping_sub(comp_x), py.wrapping_sub(comp_y))
            } else {
                true
            }
        }
        None => unsized_mask_hits,
    }
}

fn fires(last: &mut i32, now: i32, cache: &[i32; 32], filter: Option<&[i32]>) -> bool {
    if now <= *last {
        return false;
    }
    let fire = match filter {
        Some(filter) if now.wrapping_sub(*last) <= 32 => (*last..now)
            .any(|cycle| filter.contains(&cache[(cycle & 0x1f) as usize])),
        _ => true,
    };
    *last = now;
    fire
}

fn modifiers_match(frame: &Frame, mask: i8) -> bool {
    let m = mask as i32;
    if m == 0 {
        return true;
    }
    let alt = frame.held(KEY_ALT);
    let ctrl = frame.held(KEY_CTRL);
    let shift = frame.held(KEY_SHIFT);
    (m & 0x8 == 0 || !(alt || ctrl || shift))
        && (m & 0x2 == 0 || alt)
        && (m & 0x1 == 0 || ctrl)
        && (m & 0x4 == 0 || shift)
}

fn key_triggers(
    c: &Component,
    st: &mut ComponentInput,
    frame: &Frame,
    slot_id: i32,
    out: &mut InputOutput,
) {
    let (Some(actions), Some(params), Some(steps), codes) = (
        &c.key_trigger_action,
        &c.key_trigger_param,
        &c.cycle_steps,
        &c.key_char_codes,
    ) else {
        return;
    };
    for op in 0..actions.len() {
        let mut triggered = false;
        let mut has_action = false;
        let char_code = codes.map_or(0, |codes| codes[op]);
        if char_code > 0 {
            for key in &frame.pressed {
                if key.character == char_code {
                    triggered = true;
                    if st.cycles.is_none_or(|cycles| cycles[op] <= frame.cycle) {
                        has_action = true;
                    }
                    break;
                }
            }
        }
        if !triggered {
            if let Some(action) = actions[op] {
                if frame.held(action as i32) {
                    triggered = true;
                    let delayed = st.cycles.is_some_and(|cycles| cycles[op] > frame.cycle);
                    if !delayed && modifiers_match(frame, params[op].unwrap_or(0)) {
                        has_action = true;
                    }
                }
            }
        }
        if has_action {
            if op < 10 {
                out.events.push(Event::OptionInteract {
                    op: op as i32 + 1,
                    id_hash: c.id_hash,
                    slot_id,
                });
            } else {
                out.events.push(Event::UseSelect {
                    id_hash: c.id_hash,
                    slot_id,
                });
            }
            let delay = steps[op];
            let cycles = st.cycles.get_or_insert([0; 11]);
            cycles[op] = if delay != 0 {
                delay.wrapping_add(frame.cycle)
            } else {
                i32::MAX
            };
        }
        if !triggered {
            if let Some(cycles) = st.cycles.as_mut() {
                cycles[op] = 0;
            }
        }
    }
}

#[allow(clippy::too_many_arguments)]
pub fn process_components(
    host: &dyn InputHost,
    state: &mut InputState,
    frame: &Frame,
    components: &[Option<Component>],
    parent_id: i32,
    clip: Bounds,
    offset_x: i32,
    offset_y: i32,
    out: &mut InputOutput,
) {
    let mouse_x = frame.mouse_x;
    let mouse_y = frame.mouse_y;
    for slot in components {
        let Some(c) = slot else { continue };
        if c.parent_id != parent_id {
            continue;
        }
        let comp_x = offset_x.wrapping_add(c.x);
        let comp_y = offset_y.wrapping_add(c.y);
        let bounds = if c.kind == Some(ComponentType::Type2) {
            clip
        } else {
            let mut right = comp_x.wrapping_add(c.width);
            let mut bottom = comp_y.wrapping_add(c.height);
            if c.kind == Some(ComponentType::Line) {
                right = right.wrapping_add(1);
                bottom = bottom.wrapping_add(1);
            }
            Bounds {
                min_x: comp_x.max(clip.min_x),
                min_y: comp_y.max(clip.min_y),
                max_x: right.min(clip.max_x),
                max_y: bottom.min(clip.max_y),
            }
        };
        let within_clip = bounds.is_visible();
        let is_container = c.kind == Some(ComponentType::Container);
        if !is_container && host.is_passive(c) && !c.has_script {
            continue;
        }
        if host.is_hidden(c) {
            continue;
        }
        if let Some(dragging) = host.dragging() {
            if dragging == c.id_hash && host.drag_depth_flags(c) {
                out.events.push(Event::DragAnchor { x: comp_x, y: comp_y });
            }
        }
        if !(c.has_interaction || within_clip) {
            continue;
        }
        if c.prevent_click_through && bounds.contains(mouse_x, mouse_y) {
            out.events.push(Event::PreventClickThrough { id_hash: c.id_hash });
        }
        let mut within = point_hits(host, c, bounds, comp_x, comp_y, mouse_x, mouse_y, false);
        if !host.selection_active() && within {
            if c.move_over_cursor >= 0 {
                out.events.push(Event::Cursor(c.move_over_cursor));
            } else if c.prevent_click_through {
                out.events.push(Event::Cursor(-1));
            }
        }
        let interface_blocked = host.prevent_interaction();
        if !host.menu_open() && within && !interface_blocked {
            out.events.push(Event::Hovered { id_hash: c.id_hash });
        }
        let left_click = frame.left_down && within;
        let click = frame.click.filter(|&(x, y)| bounds.contains(x, y));
        let click_triggered = match click {
            Some((x, y)) => {
                !sprite_hit_test(c)
                    || match host.sprite_mask(c) {
                        Some(mask) if mask.width == c.width && mask.height == c.height => {
                            mask_hit(&mask, x.wrapping_sub(comp_x), y.wrapping_sub(comp_y))
                        }
                        _ => true,
                    }
            }
            None => false,
        };
        let st = state.components.entry(c.id_hash).or_default();
        if c.has_interaction && !frame.console_open {
            key_triggers(c, st, frame, c.slot_id, out);
        }
        if click_triggered {
            let (x, y) = click.unwrap();
            out.events.push(Event::DragStart {
                id_hash: c.id_hash,
                dx: x.wrapping_sub(comp_x),
                dy: y.wrapping_sub(comp_y),
            });
        }
        if host.dragging().is_some_and(|d| d != c.id_hash)
            && within
            && host.drag_target_enabled(c)
        {
            out.events.push(Event::DragHover { id_hash: c.id_hash });
        }
        if host.selected() == Some(c.id_hash) {
            out.events.push(Event::Selected {
                id_hash: c.id_hash,
                x: comp_x,
                y: comp_y,
                width: c.width,
                height: c.height,
            });
        }
        let rel = (
            mouse_x.wrapping_sub(comp_x),
            mouse_y.wrapping_sub(comp_y),
        );
        let mut hook_ignored_by_content = false;
        if c.has_script || c.content_type.is_some() {
            if within && frame.wheel_delta != 0 && c.on_mouse_move.is_some() {
                out.hooks.push(HookRequest {
                    source: c.id_hash,
                    queue: Queue::Pending,
                    kind: HookKind::MouseMove,
                    mouse: Some((0, frame.wheel_delta)),
                    typed: None,
                    params: c.on_mouse_move.clone(),
                });
            }
            let mut click_allowed = click_triggered;
            let mut left_allowed = left_click;
            if host.dragging().is_some() {
                click_allowed = false;
                left_allowed = false;
            } else if host.menu_open()
                || (c.content_type != Some(ContentType::WorldMap) && host.menu_item_count() > 0)
            {
                click_allowed = false;
                left_allowed = false;
                within = false;
            }
            if let Some(content) = c.content_type {
                if matches!(
                    content,
                    ContentType::GameWindow
                        | ContentType::Type1403
                        | ContentType::Minimap
                        | ContentType::WorldMap
                        | ContentType::WorldMapOverview
                        | ContentType::Type1406
                ) {
                    out.events.push(Event::Content {
                        content,
                        id_hash: c.id_hash,
                        bounds,
                        x: comp_x,
                        y: comp_y,
                        within: within && (click_allowed || left_allowed || bounds.contains(mouse_x, mouse_y)),
                    });
                    hook_ignored_by_content = true;
                }
            }
            if hook_ignored_by_content {
                continue;
            }
            if !st.pressed && click_allowed {
                st.pressed = true;
                if c.on_click.is_some() {
                    let (x, y) = click.unwrap();
                    out.hooks.push(HookRequest {
                        source: c.id_hash,
                        queue: Queue::Pending,
                        kind: HookKind::Click,
                        mouse: Some((x.wrapping_sub(comp_x), y.wrapping_sub(comp_y))),
                        typed: None,
                        params: c.on_click.clone(),
                    });
                }
            }
            let mut mouse_hook = |kind: HookKind, queue: Queue, params: &Script| {
                out.hooks.push(HookRequest {
                    source: c.id_hash,
                    queue,
                    kind,
                    mouse: Some(rel),
                    typed: None,
                    params: params.clone(),
                });
            };
            if st.pressed && left_allowed && c.on_click_repeat.is_some() {
                mouse_hook(HookKind::ClickRepeat, Queue::Pending, &c.on_click_repeat);
            }
            if st.pressed && !left_allowed {
                st.pressed = false;
                if c.on_release.is_some() {
                    mouse_hook(HookKind::Release, Queue::Mouse, &c.on_release);
                }
            }
            if left_allowed && c.on_hold.is_some() {
                mouse_hook(HookKind::Hold, Queue::Pending, &c.on_hold);
            }
            if !st.hooked && within {
                st.hooked = true;
                if c.on_mouse_over.is_some() {
                    mouse_hook(HookKind::MouseOver, Queue::Pending, &c.on_mouse_over);
                }
            }
            if st.hooked && within && c.popup_script.is_some() {
                mouse_hook(HookKind::Popup, Queue::Pending, &c.popup_script);
            }
            if st.hooked && !within {
                st.hooked = false;
                if c.on_mouse_leave.is_some() {
                    mouse_hook(HookKind::MouseLeave, Queue::Mouse, &c.on_mouse_leave);
                }
            }
            let mut plain = |kind: HookKind, queue: Queue, params: &Script| {
                out.hooks.push(HookRequest {
                    source: c.id_hash,
                    queue,
                    kind,
                    mouse: None,
                    typed: None,
                    params: params.clone(),
                });
            };
            if c.on_timer.is_some() {
                plain(HookKind::Timer, Queue::Tick, &c.on_timer);
            }
            if c.on_key.is_some()
                && fires(
                    &mut st.last_key_cycle,
                    frame.key_cycle,
                    &frame.recent_keys,
                    c.key_press_array.as_deref(),
                )
            {
                plain(HookKind::Key, Queue::Pending, &c.on_key);
            }
            if c.on_scroll_wheel.is_some()
                && fires(
                    &mut st.last_wheel_cycle,
                    frame.wheel_cycle,
                    &frame.recent_wheels,
                    c.mouse_wheel_array.as_deref(),
                )
            {
                plain(HookKind::ScrollWheel, Queue::Pending, &c.on_scroll_wheel);
            }
            if c.on_varp_transmit.is_some()
                && fires(
                    &mut st.last_varp_cycle,
                    frame.varp_cycle,
                    &frame.recent_varps,
                    c.varps.as_deref(),
                )
            {
                plain(HookKind::VarpTransmit, Queue::Pending, &c.on_varp_transmit);
            }
            if c.mouse_leave_script.is_some()
                && fires(
                    &mut st.last_container_cycle,
                    frame.container_cycle,
                    &frame.recent_containers,
                    c.mouse_leave_array_params.as_deref(),
                )
            {
                plain(HookKind::ContainerTransmit, Queue::Pending, &c.mouse_leave_script);
            }
            if c.on_stat_transmit.is_some()
                && fires(
                    &mut st.last_stat_cycle,
                    frame.stat_cycle,
                    &frame.recent_stats,
                    c.stat_transmit_filter.as_deref(),
                )
            {
                plain(HookKind::StatTransmit, Queue::Pending, &c.on_stat_transmit);
            }
            if c.on_key_type.is_some() {
                for key in &frame.pressed {
                    out.hooks.push(HookRequest {
                        source: c.id_hash,
                        queue: Queue::Pending,
                        kind: HookKind::KeyType,
                        mouse: None,
                        typed: Some((key.code, key.character)),
                        params: c.on_key_type.clone(),
                    });
                }
            }
        }
        if is_container {
            let inner = bounds;
            let inner_x = comp_x.wrapping_sub(c.scroll_x);
            let inner_y = comp_y.wrapping_sub(c.scroll_y);
            process_components(host, state, frame, components, c.id_hash, inner, inner_x, inner_y, out);
            if let Some(slots) = host.item_slots(c.id_hash) {
                process_components(host, state, frame, slots, c.id_hash, inner, inner_x, inner_y, out);
            }
            if let Some(sub) = host.sub_interface(c.id_hash) {
                process_components(host, state, frame, sub, -1, inner, comp_x, comp_y, out);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::def::ScriptArg;

    struct Host {
        mask: Option<SpriteMask>,
        menu: bool,
        dragging: Option<i32>,
    }

    impl Host {
        fn new() -> Self {
            Self {
                mask: None,
                menu: false,
                dragging: None,
            }
        }
    }

    impl InputHost for Host {
        fn sprite_mask(&self, _: &Component) -> Option<SpriteMask> {
            self.mask.clone()
        }
        fn menu_open(&self) -> bool {
            self.menu
        }
        fn dragging(&self) -> Option<i32> {
            self.dragging
        }
    }

    fn script() -> Script {
        Some(vec![ScriptArg::Int(7)])
    }

    fn scripted(id: i32, x: i32, y: i32, w: i32, h: i32) -> Component {
        let mut c = Component::blank(id);
        c.kind = Some(ComponentType::Figure);
        c.x = x;
        c.y = y;
        c.width = w;
        c.height = h;
        c.has_script = true;
        c
    }

    fn screen() -> Bounds {
        Bounds {
            min_x: 0,
            min_y: 0,
            max_x: 200,
            max_y: 200,
        }
    }

    fn run(host: &Host, state: &mut InputState, frame: &Frame, comps: &[Option<Component>]) -> InputOutput {
        let mut out = InputOutput::default();
        process_components(host, state, frame, comps, -1, screen(), 0, 0, &mut out);
        out
    }

    fn kinds(out: &InputOutput) -> Vec<HookKind> {
        out.hooks.iter().map(|h| h.kind).collect()
    }

    #[test]
    fn mouse_over_fires_once_then_popup_then_leave_on_exit() {
        let mut c = scripted(1, 10, 10, 20, 20);
        c.on_mouse_over = script();
        c.on_mouse_leave = script();
        c.popup_script = script();
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(1);
        frame.mouse_x = 15;
        frame.mouse_y = 12;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::MouseOver, HookKind::Popup]);
        assert_eq!(out.hooks[0].mouse, Some((5, 2)));
        assert!(out.events.contains(&Event::Hovered { id_hash: 1 }));
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::Popup]);
        frame.mouse_x = 100;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::MouseLeave]);
        assert_eq!(out.hooks[0].queue, Queue::Mouse);
    }

    #[test]
    fn click_repeat_hold_and_release_follow_the_button() {
        let mut c = scripted(1, 0, 0, 50, 50);
        c.on_click = script();
        c.on_click_repeat = script();
        c.on_hold = script();
        c.on_release = script();
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(1);
        frame.mouse_x = 5;
        frame.mouse_y = 6;
        frame.left_down = true;
        frame.click = Some((5, 6));
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(
            kinds(&out),
            vec![HookKind::Click, HookKind::ClickRepeat, HookKind::Hold]
        );
        assert!(out.events.contains(&Event::DragStart { id_hash: 1, dx: 5, dy: 6 }));
        frame.click = None;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::ClickRepeat, HookKind::Hold]);
        frame.left_down = false;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::Release]);
        assert_eq!(out.hooks[0].queue, Queue::Mouse);
        assert!(!state.components[&1].pressed);
    }

    #[test]
    fn open_menu_blocks_clicks_and_hover() {
        let mut c = scripted(1, 0, 0, 50, 50);
        c.on_click = script();
        c.on_mouse_over = script();
        let comps = [Some(c)];
        let mut host = Host::new();
        host.menu = true;
        let mut frame = Frame::new(1);
        frame.mouse_x = 5;
        frame.mouse_y = 5;
        frame.click = Some((5, 5));
        frame.left_down = true;
        let out = run(&host, &mut InputState::default(), &frame, &comps);
        assert!(out.hooks.is_empty());
        assert!(!out.events.iter().any(|e| matches!(e, Event::Hovered { .. })));
    }

    #[test]
    fn clicks_outside_the_clip_are_ignored() {
        let mut c = scripted(1, 150, 150, 100, 100);
        c.on_click = script();
        let comps = [Some(c)];
        let mut frame = Frame::new(1);
        frame.mouse_x = 210;
        frame.mouse_y = 210;
        frame.click = Some((210, 210));
        let out = run(&Host::new(), &mut InputState::default(), &frame, &comps);
        assert!(out.hooks.is_empty());
    }

    #[test]
    fn sprite_click_mask_rejects_transparent_pixels() {
        let mut c = scripted(1, 0, 0, 4, 2);
        c.kind = Some(ComponentType::Sprite);
        c.on_click = script();
        let comps = [Some(c)];
        let mut host = Host::new();
        host.mask = Some(SpriteMask {
            width: 4,
            height: 2,
            row_start: vec![2, 0],
            row_width: vec![1, 3],
        });
        let mut frame = Frame::new(1);
        frame.mouse_x = 0;
        frame.mouse_y = 0;
        frame.click = Some((0, 0));
        let out = run(&host, &mut InputState::default(), &frame, &comps);
        assert!(out.hooks.is_empty());
        frame.mouse_y = 1;
        frame.click = Some((0, 1));
        let out = run(&host, &mut InputState::default(), &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::Click]);
    }

    #[test]
    fn key_hook_filters_recent_keys_and_respects_cycles() {
        let mut c = scripted(1, 0, 0, 10, 10);
        c.on_key = script();
        c.key_press_array = Some(vec![13, 27]);
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(1);
        frame.key_cycle = 2;
        frame.recent_keys[0] = 65;
        frame.recent_keys[1] = 66;
        assert!(run(&host, &mut state, &frame, &comps).hooks.is_empty());
        frame.key_cycle = 3;
        frame.recent_keys[2] = 27;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::Key]);
        assert!(run(&host, &mut state, &frame, &comps).hooks.is_empty());
    }

    #[test]
    fn key_type_queues_one_hook_per_pressed_key_with_code_and_character() {
        let mut c = scripted(1, 0, 0, 10, 10);
        c.on_key_type = script();
        let comps = [Some(c)];
        let host = Host::new();
        let mut frame = Frame::new(1);
        assert!(run(&host, &mut InputState::default(), &frame, &comps).hooks.is_empty());
        frame.pressed.push(KeyPress { code: 88, character: 'x' as i32 });
        frame.pressed.push(KeyPress { code: 10, character: 10 });
        let out = run(&host, &mut InputState::default(), &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::KeyType, HookKind::KeyType]);
        assert_eq!(out.hooks[0].typed, Some((88, 'x' as i32)));
        assert_eq!(out.hooks[1].typed, Some((10, 10)));
        assert_eq!(out.hooks[0].queue, Queue::Pending);
    }

    #[test]
    fn scroll_wheel_without_filter_fires_for_any_new_wheel_cycle() {
        let mut c = scripted(1, 0, 0, 10, 10);
        c.on_scroll_wheel = script();
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(1);
        frame.wheel_cycle = 5;
        assert_eq!(kinds(&run(&host, &mut state, &frame, &comps)), vec![HookKind::ScrollWheel]);
        assert!(run(&host, &mut state, &frame, &comps).hooks.is_empty());
    }

    #[test]
    fn filter_fires_when_backlog_exceeds_the_ring() {
        let mut last = 0;
        let cache = [0; 32];
        assert!(fires(&mut last, 40, &cache, Some(&[9])));
        assert_eq!(last, 40);
        assert!(!fires(&mut last, 41, &cache, Some(&[9])));
    }

    #[test]
    fn varp_and_stat_transmits_are_tracked_independently() {
        let mut c = scripted(1, 0, 0, 10, 10);
        c.on_varp_transmit = script();
        c.varps = Some(vec![100]);
        c.on_stat_transmit = script();
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(1);
        frame.varp_cycle = 1;
        frame.recent_varps[0] = 100;
        frame.stat_cycle = 1;
        let out = run(&host, &mut state, &frame, &comps);
        assert_eq!(kinds(&out), vec![HookKind::VarpTransmit, HookKind::StatTransmit]);
    }

    #[test]
    fn key_trigger_runs_option_with_cooldown_and_modifier_rules() {
        let mut c = Component::blank(1);
        c.kind = Some(ComponentType::Figure);
        c.width = 10;
        c.height = 10;
        c.has_interaction = true;
        let mut actions = [None; 11];
        let mut params = [None; 11];
        actions[2] = Some(30i8);
        params[2] = Some(2i8);
        c.key_trigger_action = Some(actions);
        c.key_trigger_param = Some(params);
        let mut steps = [0; 11];
        steps[2] = 5;
        c.cycle_steps = Some(steps);
        c.slot_id = 4;
        let comps = [Some(c)];
        let host = Host::new();
        let mut state = InputState::default();
        let mut frame = Frame::new(10);
        frame.held.insert(30);
        assert!(run(&host, &mut state, &frame, &comps).events.is_empty());
        frame.held.insert(KEY_ALT);
        let out = run(&host, &mut state, &frame, &comps);
        assert!(out.events.contains(&Event::OptionInteract { op: 3, id_hash: 1, slot_id: 4 }));
        assert_eq!(state.components[&1].cycles.unwrap()[2], 15);
        frame.cycle = 12;
        let out = run(&host, &mut state, &frame, &comps);
        assert!(!out.events.iter().any(|e| matches!(e, Event::OptionInteract { .. })));
        frame.cycle = 15;
        let out = run(&host, &mut state, &frame, &comps);
        assert!(out.events.iter().any(|e| matches!(e, Event::OptionInteract { .. })));
        frame.held.clear();
        run(&host, &mut state, &frame, &comps);
        assert_eq!(state.components[&1].cycles.unwrap()[2], 0);
    }

    #[test]
    fn key_trigger_by_character_code_and_use_slot() {
        let mut c = Component::blank(1);
        c.kind = Some(ComponentType::Figure);
        c.width = 10;
        c.height = 10;
        c.has_interaction = true;
        c.key_trigger_action = Some([None; 11]);
        c.key_trigger_param = Some([None; 11]);
        c.cycle_steps = Some([0; 11]);
        let mut codes = [0; 11];
        codes[10] = 'x' as i32;
        c.key_char_codes = Some(codes);
        let comps = [Some(c)];
        let mut frame = Frame::new(1);
        frame.pressed.push(KeyPress { code: 88, character: 'x' as i32 });
        let mut state = InputState::default();
        let out = run(&Host::new(), &mut state, &frame, &comps);
        assert!(out.events.contains(&Event::UseSelect { id_hash: 1, slot_id: -1 }));
        assert_eq!(state.components[&1].cycles.unwrap()[10], i32::MAX);
        frame.console_open = true;
        let out = run(&Host::new(), &mut state, &frame, &comps);
        assert!(out.events.is_empty() || !out.events.iter().any(|e| matches!(e, Event::UseSelect { .. })));
    }

    #[test]
    fn prevent_click_through_and_cursor_events() {
        let mut c = Component::blank(1);
        c.kind = Some(ComponentType::Figure);
        c.width = 30;
        c.height = 30;
        c.prevent_click_through = true;
        c.has_interaction = true;
        c.move_over_cursor = 4;
        let comps = [Some(c)];
        let mut frame = Frame::new(1);
        frame.mouse_x = 3;
        frame.mouse_y = 3;
        let out = run(&Host::new(), &mut InputState::default(), &frame, &comps);
        assert!(out.events.contains(&Event::PreventClickThrough { id_hash: 1 }));
        assert!(out.events.contains(&Event::Cursor(4)));
    }

    #[test]
    fn container_children_use_scrolled_offsets_and_inherit_clip() {
        let mut parent = Component::blank(5);
        parent.kind = Some(ComponentType::Container);
        parent.x = 10;
        parent.y = 10;
        parent.width = 50;
        parent.height = 50;
        parent.scroll_y = 20;
        let mut child = scripted(6, 0, 30, 20, 20);
        child.parent_id = 5;
        child.on_click = script();
        let comps = [Some(parent), Some(child)];
        let mut frame = Frame::new(1);
        frame.mouse_x = 15;
        frame.mouse_y = 25;
        frame.click = Some((15, 25));
        let out = run(&Host::new(), &mut InputState::default(), &frame, &comps);
        let hook = &out.hooks[0];
        assert_eq!(hook.mouse, Some((5, 5)));
        frame.mouse_y = 70;
        frame.click = Some((15, 70));
        let out = run(&Host::new(), &mut InputState::default(), &frame, &comps);
        assert!(out.hooks.is_empty());
    }

    #[test]
    fn mouse_wheel_delta_becomes_a_mouse_move_hook_over_the_component() {
        let mut c = scripted(1, 0, 0, 10, 10);
        c.on_mouse_move = script();
        let comps = [Some(c)];
        let mut frame = Frame::new(1);
        frame.mouse_x = 2;
        frame.mouse_y = 2;
        frame.wheel_delta = -3;
        let out = run(&Host::new(), &mut InputState::default(), &frame, &comps);
        assert_eq!(out.hooks[0].kind, HookKind::MouseMove);
        assert_eq!(out.hooks[0].mouse, Some((0, -3)));
    }
}
