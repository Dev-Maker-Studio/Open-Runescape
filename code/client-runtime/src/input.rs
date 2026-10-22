use crate::menu_targets::{self, MenuState, Target};
use crate::minimenu::{MenuConfig, MenuInput, MiniMenu};
use openrs_protocol::client_packets::menu::process_menuclick;
use openrs_protocol::client_packets::{
    ClientBody, KeyPress, KeyRecorder, MenuContext, MouseRecord, MouseSubmitter,
};
use std::collections::VecDeque;

const MAX_HELD_KEYS: usize = 75;
const MAX_MOVEMENTS: usize = 10;
const MAX_PENDING: usize = 4096;

pub const CLICK_MOVE: i32 = -1;
pub const CLICK_LEFT_DOWN: i32 = 0;
pub const CLICK_MIDDLE_DOWN: i32 = 1;
pub const CLICK_RIGHT_DOWN: i32 = 2;
pub const CLICK_LEFT_UP: i32 = 3;
pub const CLICK_MIDDLE_UP: i32 = 4;
pub const CLICK_RIGHT_UP: i32 = 5;
pub const CLICK_WHEEL: i32 = 6;

fn meta_for(click_type: i32) -> Option<i32> {
    match click_type {
        CLICK_MOVE => Some(512),
        CLICK_LEFT_DOWN => Some(513),
        CLICK_MIDDLE_DOWN => Some(519),
        CLICK_RIGHT_DOWN => Some(516),
        CLICK_LEFT_UP => Some(514),
        CLICK_MIDDLE_UP => Some(520),
        CLICK_RIGHT_UP => Some(517),
        CLICK_WHEEL => Some(522),
        _ => None,
    }
}

#[cfg_attr(not(target_arch = "wasm32"), allow(dead_code))]
pub fn awt_key_code(code: &str) -> Option<i32> {
    let letter = code.strip_prefix("Key").filter(|rest| rest.len() == 1);
    if let Some(rest) = letter {
        return Some(i32::from(rest.as_bytes()[0]));
    }
    if let Some(rest) = code.strip_prefix("Digit").filter(|rest| rest.len() == 1) {
        return Some(i32::from(rest.as_bytes()[0]));
    }
    if let Some(rest) = code.strip_prefix("Numpad") {
        if rest.len() == 1 && rest.as_bytes()[0].is_ascii_digit() {
            return Some(96 + i32::from(rest.as_bytes()[0] - b'0'));
        }
        return match rest {
            "Multiply" => Some(106),
            "Add" => Some(107),
            "Subtract" => Some(109),
            "Decimal" => Some(110),
            "Divide" => Some(111),
            "Enter" => Some(10),
            _ => None,
        };
    }
    if let Some(n) = code
        .strip_prefix('F')
        .and_then(|rest| rest.parse::<i32>().ok())
        .filter(|n| (1..=12).contains(n))
    {
        return Some(111 + n);
    }
    Some(match code {
        "Backspace" => 8,
        "Tab" => 9,
        "Enter" => 10,
        "ShiftLeft" | "ShiftRight" => 16,
        "ControlLeft" | "ControlRight" => 17,
        "AltLeft" | "AltRight" => 18,
        "CapsLock" => 20,
        "Escape" => 27,
        "Space" => 32,
        "PageUp" => 33,
        "PageDown" => 34,
        "End" => 35,
        "Home" => 36,
        "ArrowLeft" => 37,
        "ArrowUp" => 38,
        "ArrowRight" => 39,
        "ArrowDown" => 40,
        "Comma" => 44,
        "Minus" => 45,
        "Period" => 46,
        "Slash" => 47,
        "Semicolon" => 59,
        "Equal" => 61,
        "BracketLeft" => 91,
        "Backslash" => 92,
        "BracketRight" => 93,
        "Delete" => 127,
        "Insert" => 155,
        "Backquote" => 192,
        "Quote" => 222,
        _ => return None,
    })
}

#[derive(Clone, Copy, Debug)]
struct Pending {
    click_type: i32,
    x: i32,
    y: i32,
    time: i64,
    meta: i32,
}

pub struct ClientInput {
    submitter: MouseSubmitter,
    keys: KeyRecorder,
    pending: VecDeque<Pending>,
    movements: VecDeque<MouseRecord>,
    typed: VecDeque<u16>,
    camera: Option<(f32, f32)>,
    focus: bool,
    wheel: i32,
    menu: Option<MiniMenu>,
    menu_state: MenuState,
    menu_context: MenuContext,
    targets: Vec<Target>,
    mouse_at: (i32, i32),
}

impl Default for ClientInput {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg_attr(not(target_arch = "wasm32"), allow(dead_code))]
impl ClientInput {
    pub fn new() -> Self {
        Self {
            submitter: MouseSubmitter::new(),
            keys: KeyRecorder::new(),
            pending: VecDeque::new(),
            movements: VecDeque::new(),
            typed: VecDeque::new(),
            camera: None,
            focus: true,
            wheel: 0,
            menu: None,
            menu_state: MenuState::default(),
            menu_context: MenuContext::default(),
            targets: Vec::new(),
            mouse_at: (0, 0),
        }
    }

    pub fn enable_menu(&mut self, config: MenuConfig) {
        self.menu = Some(MiniMenu::new(config));
    }

    pub fn menu(&self) -> Option<&MiniMenu> {
        self.menu.as_ref()
    }

    pub fn menu_state_mut(&mut self) -> &mut MenuState {
        &mut self.menu_state
    }

    pub fn set_menu_context(&mut self, context: MenuContext) {
        self.menu_context = context;
    }

    pub fn set_hover(&mut self, targets: Vec<Target>) {
        self.targets = targets;
    }

    fn run_menu(&mut self, primary: bool, context: bool, run: bool) -> Option<ClientBody> {
        let menu = self.menu.as_mut()?;
        if !menu.is_open {
            menu.reset_entries();
            menu_targets::build(menu, &self.menu_state, &self.targets);
        }
        let outcome = menu.process_input(&MenuInput {
            primary,
            secondary: false,
            context,
            open_click: primary,
            minimenu_key: false,
            mouse: self.mouse_at,
            dragging: false,
            item_count: 0,
        });
        if outcome.cancel_selection {
            self.menu_state.use_on = None;
        }
        let click = outcome.click?;
        let ctx = MenuContext {
            run,
            container_item: outcome.item_id,
            ..self.menu_context
        };
        process_menuclick(&click, &ctx).ok().flatten()
    }

    pub fn mouse(&mut self, click_type: i32, x: i32, y: i32, time: i64, meta: i32) {
        if meta_for(click_type).is_none() || self.pending.len() >= MAX_PENDING {
            return;
        }
        self.pending.push_back(Pending {
            click_type,
            x,
            y,
            time,
            meta,
        });
    }

    pub fn key_down(&mut self, awt_code: i32, time: i64) -> bool {
        self.keys.key_pressed(awt_code, time)
    }

    pub fn key_up(&mut self, awt_code: i32, time: i64) -> bool {
        self.keys.key_released(awt_code, time)
    }

    pub fn key_typed(&mut self, unit: u16, time: i64) -> bool {
        self.keys.key_typed(unit, time)
    }

    pub fn focus_lost(&mut self, time: i64) {
        self.keys.focus_lost(time);
    }

    pub fn set_focus(&mut self, focus: bool) {
        self.focus = focus;
    }

    pub fn set_camera(&mut self, angle_x: f32, angle_y: f32) {
        self.camera = Some((angle_x, angle_y));
    }

    pub fn take_typed(&mut self) -> Option<u16> {
        self.typed.pop_front()
    }

    pub fn take_wheel(&mut self) -> i32 {
        std::mem::take(&mut self.wheel)
    }

    pub fn cycle(&mut self, now: i64, rendering_scene: bool) -> Vec<ClientBody> {
        self.keys.prepare_awaiting_keys();
        let mut held: Vec<KeyPress> = Vec::new();
        while let Some(entry) = self.keys.pop_head() {
            match entry.entry_type {
                0 if held.len() < MAX_HELD_KEYS => held.push(KeyPress {
                    code: entry.code,
                    time: entry.time,
                }),
                3 => self.typed.push_back(entry.character),
                _ => {}
            }
        }
        let mut primary = false;
        let mut context = false;
        while let Some(p) = self.pending.pop_front() {
            self.mouse_at = (p.x, p.y);
            primary |= p.click_type == CLICK_LEFT_DOWN;
            context |= p.click_type == CLICK_RIGHT_DOWN;
            let Some(click_meta) = meta_for(p.click_type) else {
                continue;
            };
            let record = MouseRecord {
                x: p.x,
                y: p.y,
                time: p.time,
                click_meta,
                constant: 0,
            };
            match p.click_type {
                CLICK_MOVE if rendering_scene => self.submitter.append_movement_batch(record),
                CLICK_WHEEL => self.wheel += p.meta,
                CLICK_LEFT_DOWN | CLICK_MIDDLE_DOWN | CLICK_RIGHT_DOWN => {
                    self.movements.push_back(record);
                    if self.movements.len() > MAX_MOVEMENTS {
                        self.movements.pop_front();
                    }
                }
                _ => {}
            }
        }
        let mut bodies = self.submitter.submit_queued(
            now,
            self.movements.front().copied(),
            &held,
            &mut self.camera,
            self.focus,
        );
        self.movements.pop_front();
        let run = self.menu_context.run;
        bodies.extend(self.run_menu(primary, context, run));
        bodies
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_protocol::registry::ClientPacket;

    #[test]
    fn right_click_is_flagged_and_popped_once_per_cycle() {
        let mut input = ClientInput::new();
        input.mouse(CLICK_RIGHT_DOWN, 10, 20, 1000, 1);
        let first = input.cycle(1_700_000_000_000, true);
        let click = first
            .iter()
            .find(|b| b.packet == ClientPacket::MOUSE_CLICK)
            .unwrap();
        assert_eq!(click.body.len(), 6);
        assert_ne!(click.body[5] & 0x80, 0);
        let second = input.cycle(1_700_000_000_050, true);
        assert!(second.iter().all(|b| b.packet != ClientPacket::MOUSE_CLICK));
    }

    #[test]
    fn camera_is_rate_limited_and_keys_become_a_key_press() {
        let mut input = ClientInput::new();
        input.set_camera(800.0, 1600.0);
        assert!(input.key_down(65, 5));
        let out = input.cycle(1_700_000_000_000, true);
        assert!(out.iter().any(|b| b.packet == ClientPacket::KEY_PRESS));
        assert!(out.iter().any(|b| b.packet == ClientPacket::MOVE_CAMERA));
        input.set_camera(900.0, 1700.0);
        let out = input.cycle(1_700_000_000_020, true);
        assert!(out.iter().all(|b| b.packet != ClientPacket::MOVE_CAMERA));
    }

    fn menu_input() -> ClientInput {
        use crate::menu_targets::{NpcTarget, TileTarget};
        let mut input = ClientInput::new();
        input.enable_menu(MenuConfig::new(765, 503));
        input.menu_state_mut().uptext = "Walk here".to_string();
        input.menu_state_mut().default_attack_cursor = 1;
        input.set_menu_context(MenuContext {
            base_x: 3200,
            base_y: 3200,
            own_player_index: 1,
            ..MenuContext::default()
        });
        input.set_hover(vec![
            Target::Tile(TileTarget { x: 12, y: 13 }),
            Target::Npc(NpcTarget {
                index: 4,
                name: "Guard".to_string(),
                visible: true,
                actions: vec![Some("Talk-to".to_string()), Some("Pickpocket".to_string())],
                primary_cursor_index: -1,
                secondary_cursor_index: -1,
                ..NpcTarget::default()
            }),
        ]);
        input
    }

    fn packets(bodies: &[ClientBody]) -> Vec<ClientPacket> {
        bodies.iter().map(|b| b.packet).collect()
    }

    #[test]
    fn left_click_on_an_npc_sends_its_first_option() {
        let mut input = menu_input();
        input.mouse(CLICK_MOVE, 100, 100, 1000, 0);
        input.cycle(1_700_000_000_000, true);
        input.mouse(CLICK_LEFT_DOWN, 100, 100, 1010, 1);
        let out = input.cycle(1_700_000_000_050, true);
        assert!(packets(&out).contains(&ClientPacket::NPC_OP1));
        assert!(!packets(&out).contains(&ClientPacket::WALK));
    }

    #[test]
    fn right_click_opens_the_menu_and_a_row_click_resolves() {
        let mut input = menu_input();
        input.mouse(CLICK_RIGHT_DOWN, 100, 100, 1000, 1);
        let out = input.cycle(1_700_000_000_000, true);
        assert!(!packets(&out).contains(&ClientPacket::NPC_OP1));
        let menu = input.menu().unwrap();
        assert!(menu.is_open);
        let (x, y) = (menu.menu_open_x + 5, menu.menu_open_y + 25);
        input.mouse(CLICK_LEFT_DOWN, x, y, 1100, 1);
        let out = input.cycle(1_700_000_000_100, true);
        assert!(!input.menu().unwrap().is_open);
        assert!(packets(&out).iter().any(|p| matches!(
            p,
            ClientPacket::NPC_OP1 | ClientPacket::NPC_OP2 | ClientPacket::WALK
        )));
    }

    #[test]
    fn typed_characters_are_surfaced_and_codes_map() {
        let mut input = ClientInput::new();
        assert!(input.key_typed(u16::from(b'a'), 1));
        input.cycle(1_700_000_000_000, true);
        assert_eq!(input.take_typed(), Some(u16::from(b'a')));
        assert_eq!(awt_key_code("KeyW"), Some(87));
        assert_eq!(awt_key_code("F5"), Some(116));
        assert_eq!(awt_key_code("ArrowLeft"), Some(37));
        assert_eq!(awt_key_code("Nope"), None);
    }
}

#[cfg(target_arch = "wasm32")]
mod browser {
    use super::*;
    use crate::world_session::browser::BrowserWorldSession;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = ClientInput)]
    pub struct BrowserClientInput(ClientInput);

    impl Default for BrowserClientInput {
        fn default() -> Self {
            Self::new()
        }
    }

    #[wasm_bindgen(js_class = ClientInput)]
    impl BrowserClientInput {
        #[wasm_bindgen(constructor)]
        pub fn new() -> Self {
            Self(ClientInput::new())
        }

        pub fn mouse(&mut self, click_type: i32, x: i32, y: i32, time: f64, meta: i32) {
            self.0.mouse(click_type, x, y, time as i64, meta);
        }

        #[wasm_bindgen(js_name = keyDown)]
        pub fn key_down(&mut self, code: &str, time: f64) -> bool {
            awt_key_code(code).is_some_and(|awt| self.0.key_down(awt, time as i64))
        }

        #[wasm_bindgen(js_name = keyUp)]
        pub fn key_up(&mut self, code: &str, time: f64) -> bool {
            awt_key_code(code).is_some_and(|awt| self.0.key_up(awt, time as i64))
        }

        #[wasm_bindgen(js_name = keyTyped)]
        pub fn key_typed(&mut self, unit: u32, time: f64) -> bool {
            u16::try_from(unit).is_ok_and(|unit| self.0.key_typed(unit, time as i64))
        }

        #[wasm_bindgen(js_name = focusLost)]
        pub fn focus_lost(&mut self, time: f64) {
            self.0.focus_lost(time as i64);
        }

        #[wasm_bindgen(js_name = setFocus)]
        pub fn set_focus(&mut self, focus: bool) {
            self.0.set_focus(focus);
        }

        #[wasm_bindgen(js_name = setCamera)]
        pub fn set_camera(&mut self, angle_x: f32, angle_y: f32) {
            self.0.set_camera(angle_x, angle_y);
        }

        #[wasm_bindgen(js_name = takeTyped)]
        pub fn take_typed(&mut self) -> i32 {
            self.0.take_typed().map_or(-1, i32::from)
        }

        #[wasm_bindgen(js_name = takeWheel)]
        pub fn take_wheel(&mut self) -> i32 {
            self.0.take_wheel()
        }

        #[wasm_bindgen(js_name = enableMenu)]
        pub fn enable_menu(&mut self, width: i32, height: i32) {
            self.0.enable_menu(MenuConfig::new(width, height));
        }

        #[wasm_bindgen(js_name = setMenuContext)]
        #[allow(clippy::too_many_arguments)]
        pub fn set_menu_context(
            &mut self,
            run: bool,
            base_x: i32,
            base_y: i32,
            own_player_index: i32,
            selected_hash: i32,
            selected_item: i32,
            selected_slot: i32,
        ) {
            self.0.set_menu_context(MenuContext {
                run,
                base_x,
                base_y,
                own_player_index,
                selected_hash,
                selected_item,
                selected_slot,
                ..MenuContext::default()
            });
        }

        #[wasm_bindgen(js_name = setMenuState)]
        pub fn set_menu_state(&mut self, json: &str) -> Result<(), JsValue> {
            *self.0.menu_state_mut() =
                serde_json::from_str(json).map_err(|e| JsValue::from_str(&e.to_string()))?;
            Ok(())
        }

        #[wasm_bindgen(js_name = setHover)]
        pub fn set_hover(&mut self, json: &str) -> Result<(), JsValue> {
            let targets: Vec<Target> =
                serde_json::from_str(json).map_err(|e| JsValue::from_str(&e.to_string()))?;
            self.0.set_hover(targets);
            Ok(())
        }

        #[wasm_bindgen(js_name = menuOpen)]
        pub fn menu_open(&self) -> bool {
            self.0.menu().is_some_and(|m| m.is_open)
        }

        pub fn cycle(
            &mut self,
            session: &mut BrowserWorldSession,
            now: f64,
            rendering: bool,
        ) -> Result<(), JsValue> {
            let bodies = self.0.cycle(now as i64, rendering);
            session
                .0
                .send_client_bodies(&bodies)
                .map_err(JsValue::from_str)
        }
    }
}
