mod bridge;

pub use bridge::{apply_state, component_state};

use openrs_cs2::FontMetrics;
use openrs_interface::def::Component;
use openrs_interface::draw::{self, Clip, DrawHost, DrawOutput};
use openrs_interface::input::{
    self, Bounds, Event, Frame, HookKind, HookRequest, InputHost, InputOutput, InputState, KeyPress,
    Queue,
};
use openrs_interface::layout::{self, LayoutHost, Nested};
use openrs_interface_raster::{decode_sprites, Assets, Bitmap, Canvas, Font, PaintReport, Painter};
use openrs_login::world_packets::WorldPacket;
use serde::Serialize;
use std::cell::{Cell, RefCell};
use std::collections::{BTreeMap, BTreeSet};

pub const FIXED_WIDTH: i32 = 765;
pub const FIXED_HEIGHT: i32 = 503;
pub const INTERFACE_INDEX: u32 = 3;
pub const SPRITE_INDEX: u32 = 8;
pub const FONT_METRICS_INDEX: u32 = 13;
pub const TYPED_CHAR_CODE: i32 = -1;
pub const HELD_CHAR: i32 = 0xffff;
const MAX_WANTS: usize = 256;

type ArchiveFile = (u32, Vec<u8>);

#[derive(Debug, Default, Clone, PartialEq, Eq)]
pub struct OpenInterfaces {
    pub top: Option<i32>,
    pub subs: BTreeMap<i32, (i32, u8)>,
    pub revision: u32,
}

impl OpenInterfaces {
    pub fn apply(&mut self, packet: &WorldPacket) -> bool {
        match packet {
            WorldPacket::OpenTop(open) => {
                self.top = Some(i32::from(open.interface_id));
                self.subs.clear();
            }
            WorldPacket::OpenSub(open) => {
                self.subs.insert(open.parent, (open.interface_id, open.overlay));
            }
            WorldPacket::CloseSub { parent } => {
                if self.subs.remove(parent).is_none() {
                    return false;
                }
            }
            WorldPacket::MoveSub { from, to } => {
                let Some(entry) = self.subs.remove(from) else {
                    return false;
                };
                self.subs.insert(*to, entry);
            }
            _ => return false,
        }
        self.revision = self.revision.wrapping_add(1);
        true
    }
}

#[derive(Default)]
struct UiAssets {
    sprites: BTreeMap<i32, Bitmap>,
    fonts: BTreeMap<i32, Font>,
    metrics: BTreeMap<i32, FontMetrics>,
    glyphs: BTreeMap<i32, openrs_interface_raster::SpriteGroup>,
    font_ids: BTreeSet<i32>,
    absent: BTreeSet<(u32, u32)>,
}

impl Assets for UiAssets {
    fn sprite(&self, id: i32) -> Option<&Bitmap> {
        self.sprites.get(&id)
    }

    fn font(&self, id: i32) -> Option<&Font> {
        self.fonts.get(&id)
    }
}

impl UiAssets {
    fn assemble(&mut self, id: i32) {
        if self.fonts.contains_key(&id) {
            return;
        }
        if let (Some(metrics), Some(group)) = (self.metrics.get(&id), self.glyphs.get(&id))
            && let Some(font) = Font::new(metrics.clone(), group)
        {
            self.fonts.insert(id, font);
            self.glyphs.remove(&id);
        }
    }
}

#[derive(Debug, Default, Clone)]
pub struct UiInput {
    pub mouse_x: i32,
    pub mouse_y: i32,
    pub left_down: bool,
    pub click: Option<(i32, i32)>,
    pub wheel: i32,
    pub keys: Vec<(i32, i32)>,
    pub held: Vec<i32>,
}

#[derive(Debug, Default, Serialize, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct UiReport {
    pub drawn: bool,
    pub game_area: Option<[i32; 5]>,
    pub specials: Vec<[i32; 5]>,
    pub commands: usize,
    pub hooks: usize,
    pub hovered: Option<i32>,
    pub cursor: Option<i32>,
    pub missing_sprites: Vec<i32>,
    pub missing_fonts: Vec<i32>,
    pub skipped_models: usize,
    pub skipped_items: usize,
    pub layout_errors: usize,
}

#[derive(Debug, Serialize, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct HookReport {
    pub source: i32,
    pub kind: &'static str,
    pub queue: &'static str,
    pub mouse: Option<[i32; 2]>,
    pub typed: Option<[i32; 2]>,
    pub args: usize,
}

fn kind_name(kind: HookKind) -> &'static str {
    match kind {
        HookKind::Click => "click",
        HookKind::ClickRepeat => "clickRepeat",
        HookKind::Release => "release",
        HookKind::Hold => "hold",
        HookKind::MouseOver => "mouseOver",
        HookKind::Popup => "popup",
        HookKind::MouseLeave => "mouseLeave",
        HookKind::Timer => "timer",
        HookKind::MouseMove => "mouseMove",
        HookKind::Key => "key",
        HookKind::ScrollWheel => "scrollWheel",
        HookKind::VarpTransmit => "varpTransmit",
        HookKind::ContainerTransmit => "containerTransmit",
        HookKind::StatTransmit => "statTransmit",
        HookKind::KeyType => "keyType",
    }
}

impl From<&HookRequest> for HookReport {
    fn from(hook: &HookRequest) -> Self {
        Self {
            source: hook.source,
            kind: kind_name(hook.kind),
            queue: match hook.queue {
                Queue::Pending => "pending",
                Queue::Mouse => "mouse",
                Queue::Tick => "tick",
            },
            mouse: hook.mouse.map(|m| [m.0, m.1]),
            typed: hook.typed.map(|t| [t.0, t.1]),
            args: hook.params.as_ref().map_or(0, Vec::len),
        }
    }
}

struct SubLayout<'a> {
    subs: &'a BTreeMap<i32, (i32, u8)>,
    live: &'a mut BTreeMap<i32, Vec<Option<Component>>>,
}

impl LayoutHost for SubLayout<'_> {
    fn take_nested(&mut self, container: i32) -> Vec<Nested> {
        let Some((interface, _)) = self.subs.get(&container) else {
            return Vec::new();
        };
        match self.live.remove(interface) {
            Some(components) => vec![Nested { key: *interface as u64, components, parent: -1 }],
            None => Vec::new(),
        }
    }

    fn restore_nested(&mut self, nested: Nested) {
        self.live.insert(nested.key as i32, nested.components);
    }
}

struct UiDraw<'a> {
    live: &'a BTreeMap<i32, Vec<Option<Component>>>,
    subs: &'a BTreeMap<i32, (i32, u8)>,
    assets: &'a UiAssets,
    next_reference: Cell<i32>,
    wants: RefCell<BTreeSet<i32>>,
}

impl DrawHost for UiDraw<'_> {
    fn assign_reference(&self, _: i32, _: i32, _: i32, _: i32) -> i32 {
        let n = self.next_reference.get();
        self.next_reference.set(n.wrapping_add(1));
        n
    }

    fn prev_on_top(&self, _: i32) -> bool {
        true
    }

    fn mark_on_top(&self, _: i32) {}

    fn sprite_size(&self, c: &Component) -> Option<(i32, i32)> {
        if c.container_item_id != -1 {
            return Some((36, 32));
        }
        if c.sprite_id < 0 {
            return None;
        }
        match self.assets.sprites.get(&c.sprite_id) {
            Some(b) => Some((b.width, b.height)),
            None => {
                self.wants.borrow_mut().insert(c.sprite_id);
                None
            }
        }
    }

    fn sub_interface(&self, container: i32) -> Option<&[Option<Component>]> {
        let (interface, _) = self.subs.get(&container)?;
        self.live.get(interface).map(Vec::as_slice)
    }
}

struct UiInputHost<'a> {
    live: &'a BTreeMap<i32, Vec<Option<Component>>>,
    subs: &'a BTreeMap<i32, (i32, u8)>,
}

impl InputHost for UiInputHost<'_> {
    fn sub_interface(&self, container: i32) -> Option<&[Option<Component>]> {
        let (interface, _) = self.subs.get(&container)?;
        self.live.get(interface).map(Vec::as_slice)
    }
}

pub struct InterfaceUi {
    width: i32,
    height: i32,
    open: OpenInterfaces,
    defs: BTreeMap<i32, Vec<Option<Component>>>,
    live: BTreeMap<i32, Vec<Option<Component>>>,
    incoming: BTreeMap<(u32, u32), Vec<ArchiveFile>>,
    assets: UiAssets,
    wanted: BTreeSet<(u32, u32)>,
    input_state: InputState,
    canvas: Canvas,
    cycle: i32,
    hooks: Vec<HookRequest>,
    events: Vec<Event>,
}

impl InterfaceUi {
    pub fn new(width: i32, height: i32) -> Self {
        Self {
            width,
            height,
            open: OpenInterfaces::default(),
            defs: BTreeMap::new(),
            live: BTreeMap::new(),
            incoming: BTreeMap::new(),
            assets: UiAssets::default(),
            wanted: BTreeSet::new(),
            input_state: InputState::default(),
            canvas: Canvas::new(width, height),
            cycle: 0,
            hooks: Vec::new(),
            events: Vec::new(),
        }
    }

    pub fn resize(&mut self, width: i32, height: i32) {
        if width != self.width || height != self.height {
            self.width = width;
            self.height = height;
            self.canvas = Canvas::new(width, height);
        }
    }

    pub fn size(&self) -> (i32, i32) {
        (self.width, self.height)
    }

    pub fn set_open(&mut self, open: OpenInterfaces) {
        self.open = open;
    }

    pub fn open(&self) -> &OpenInterfaces {
        &self.open
    }

    pub fn rgba(&self) -> Vec<u8> {
        self.canvas.to_rgba()
    }

    pub fn hooks(&self) -> Vec<HookReport> {
        self.hooks.iter().map(HookReport::from).collect()
    }

    pub fn needs(&self) -> Vec<(u32, u32)> {
        self.wanted
            .iter()
            .filter(|key| !self.assets.absent.contains(key) && !self.incoming.contains_key(key))
            .copied()
            .collect()
    }

    pub fn add_file(&mut self, index: u32, group: u32, file: u32, bytes: Vec<u8>) {
        self.incoming.entry((index, group)).or_default().push((file, bytes));
    }

    pub fn absent(&mut self, index: u32, group: u32) {
        self.wanted.remove(&(index, group));
        self.assets.absent.insert((index, group));
    }

    pub fn finish_group(&mut self, index: u32, group: u32) {
        let files = self.incoming.remove(&(index, group)).unwrap_or_default();
        self.wanted.remove(&(index, group));
        let id = group as i32;
        match index {
            INTERFACE_INDEX => {
                let size = files.iter().map(|f| f.0 as usize + 1).max().unwrap_or(0);
                let mut components: Vec<Option<Component>> = vec![None; size];
                for (file, bytes) in files {
                    let hash = (id << 16) | file as i32;
                    components[file as usize] = Component::decode(&bytes, hash).ok();
                }
                self.defs.insert(id, components);
            }
            SPRITE_INDEX => {
                let Some((_, bytes)) = files.into_iter().find(|f| f.0 == 0) else {
                    self.assets.absent.insert((index, group));
                    return;
                };
                let Some(sprites) = decode_sprites(&bytes) else {
                    self.assets.absent.insert((index, group));
                    return;
                };
                if self.assets.font_ids.contains(&id) {
                    self.assets.glyphs.insert(id, sprites.clone());
                    self.assets.assemble(id);
                }
                if let Some(bitmap) = sprites.full(0) {
                    self.assets.sprites.insert(id, bitmap);
                }
            }
            FONT_METRICS_INDEX => {
                let metrics = files
                    .into_iter()
                    .find(|f| f.0 == 0)
                    .and_then(|f| FontMetrics::decode(&f.1));
                match metrics {
                    Some(metrics) => {
                        self.assets.metrics.insert(id, metrics);
                        self.assets.assemble(id);
                    }
                    None => {
                        self.assets.absent.insert((index, group));
                    }
                }
            }
            _ => {}
        }
    }

    fn want(&mut self, index: u32, group: u32) {
        if self.wanted.len() < MAX_WANTS && !self.assets.absent.contains(&(index, group)) {
            self.wanted.insert((index, group));
        }
    }

    fn want_font(&mut self, id: i32) {
        if id < 0 || self.assets.fonts.contains_key(&id) {
            return;
        }
        self.assets.font_ids.insert(id);
        if !self.assets.metrics.contains_key(&id) {
            self.want(FONT_METRICS_INDEX, id as u32);
        }
        if !self.assets.glyphs.contains_key(&id) {
            self.want(SPRITE_INDEX, id as u32);
        }
    }

    fn sync_live(&mut self) {
        let mut open_ids: BTreeSet<i32> = self.open.subs.values().map(|s| s.0).collect();
        if let Some(top) = self.open.top {
            open_ids.insert(top);
        }
        self.live.retain(|id, _| open_ids.contains(id));
        for id in open_ids {
            if id < 0 {
                continue;
            }
            if self.live.contains_key(&id) {
                continue;
            }
            match self.defs.get(&id) {
                Some(def) => {
                    self.live.insert(id, def.clone());
                }
                None => self.want(INTERFACE_INDEX, id as u32),
            }
        }
    }

    pub fn frame(&mut self, user: &UiInput) -> UiReport {
        self.cycle = self.cycle.wrapping_add(1);
        self.canvas.clear();
        self.hooks.clear();
        self.events.clear();
        let mut report = UiReport::default();
        self.sync_live();
        let Some(top) = self.open.top else {
            return report;
        };
        let Some(mut top_components) = self.live.remove(&top) else {
            return report;
        };
        {
            let mut host = SubLayout { subs: &self.open.subs, live: &mut self.live };
            if layout::update_dimensions(&mut top_components, -1, self.width, self.height, false, &mut host).is_err() {
                report.layout_errors += 1;
            }
        }
        self.live.insert(top, top_components);
        let mut frame = Frame::new(self.cycle);
        frame.mouse_x = user.mouse_x;
        frame.mouse_y = user.mouse_y;
        frame.left_down = user.left_down;
        frame.click = user.click;
        frame.wheel_delta = user.wheel;
        frame.pressed = user.keys.iter().map(|k| KeyPress { code: k.0, character: k.1 }).collect();
        frame.held = user.held.iter().copied().collect();
        let mut out = InputOutput::default();
        {
            let host = UiInputHost { live: &self.live, subs: &self.open.subs };
            let components = &self.live[&top];
            input::process_components(
                &host,
                &mut self.input_state,
                &frame,
                components,
                -1,
                Bounds { min_x: 0, min_y: 0, max_x: self.width, max_y: self.height },
                0,
                0,
                &mut out,
            );
        }
        report.hooks = out.hooks.len();
        for event in &out.events {
            match event {
                Event::Hovered { id_hash } => report.hovered = Some(*id_hash),
                Event::Cursor(cursor) => report.cursor = Some(*cursor),
                _ => {}
            }
        }
        self.hooks = out.hooks;
        self.events = out.events;
        let mut drawn = DrawOutput::default();
        let wants;
        {
            let host = UiDraw {
                live: &self.live,
                subs: &self.open.subs,
                assets: &self.assets,
                next_reference: Cell::new(0),
                wants: RefCell::new(BTreeSet::new()),
            };
            draw::draw_components(
                &host,
                &self.live[&top],
                -1,
                Clip { left: 0, top: 0, right: self.width, bottom: self.height },
                0,
                0,
                draw::NO_REFERENCE,
                &mut drawn,
            );
            wants = host.wants.into_inner();
        }
        let mut fonts = BTreeSet::new();
        for cmd in &drawn.cmds {
            if let draw::DrawCmd::Text { font_id, .. } = cmd {
                fonts.insert(*font_id);
            }
        }
        let painted: PaintReport = Painter::paint(&mut self.canvas, &drawn.cmds, &self.assets);
        for id in wants.into_iter().chain(painted.missing_sprites.iter().copied()) {
            if id >= 0 {
                self.want(SPRITE_INDEX, id as u32);
            }
        }
        for id in fonts.into_iter().chain(painted.missing_fonts.iter().copied()) {
            self.want_font(id);
        }
        report.drawn = true;
        report.commands = drawn.cmds.len();
        report.game_area = painted.game_area.map(|g| [g.0, g.1, g.2, g.3, i32::from(g.4)]);
        report.specials = painted
            .specials
            .iter()
            .map(|s| [s.0 as i32, s.1, s.2, s.3, s.4])
            .collect();
        report.missing_sprites = painted.missing_sprites;
        report.missing_fonts = painted.missing_fonts;
        report.skipped_models = painted.skipped_models;
        report.skipped_items = painted.skipped_items;
        report
    }
}

const AWT_TO_RS: [(i32, i32); 94] = [
    (8, 85), (9, 80), (10, 84), (12, 91), (16, 81), (17, 82), (18, 86), (27, 13), (32, 83),
    (33, 104), (34, 105), (35, 103), (36, 102), (37, 96), (38, 98), (39, 97), (40, 99),
    (44, 71), (45, 26), (46, 72), (47, 73), (48, 25), (49, 16), (50, 17), (51, 18), (52, 19),
    (53, 20), (54, 21), (55, 22), (56, 23), (57, 24), (59, 57), (61, 27), (65, 48), (66, 68),
    (67, 66), (68, 50), (69, 34), (70, 51), (71, 52), (72, 53), (73, 39), (74, 54), (75, 55),
    (76, 56), (77, 70), (78, 69), (79, 40), (80, 41), (81, 32), (82, 35), (83, 49), (84, 36),
    (85, 38), (86, 67), (87, 33), (88, 65), (89, 37), (90, 64), (91, 42), (92, 74), (93, 43),
    (96, 228), (97, 231), (98, 227), (99, 233), (100, 224), (101, 219), (102, 225), (103, 230),
    (104, 226), (105, 232), (106, 89), (107, 87), (109, 88), (110, 229), (111, 90), (112, 1),
    (113, 2), (114, 3), (115, 4), (116, 5), (117, 6), (118, 7), (119, 8), (120, 9), (121, 10),
    (122, 11), (123, 12), (127, 101), (155, 100), (192, 28), (222, 58), (520, 59),
];

pub fn rs_key_code(awt_key_code: i32, down: bool) -> i32 {
    let Some((_, code)) = AWT_TO_RS.iter().find(|(vk, _)| *vk == awt_key_code) else {
        return 0;
    };
    if down && code & 0x80 != 0 {
        return 0;
    }
    code & !0x80
}

pub fn is_valid_key_char(unit: u32) -> bool {
    matches!(unit, 1..=127 | 160..=255)
        || [
            8364u32, 8218, 402, 8222, 8230, 8224, 8225, 710, 8240, 352, 8249, 338, 381, 8216, 8217,
            8220, 8221, 8226, 8211, 8212, 732, 8482, 353, 8250, 339, 382, 376,
        ]
        .contains(&unit)
}

#[cfg(target_arch = "wasm32")]
pub mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = InterfaceUi)]
    pub struct BrowserInterfaceUi(InterfaceUi);

    fn fail(error: impl ToString) -> JsValue {
        JsValue::from_str(&error.to_string())
    }

    #[wasm_bindgen(js_class = InterfaceUi)]
    impl BrowserInterfaceUi {
        #[wasm_bindgen(constructor)]
        pub fn new(width: i32, height: i32) -> Result<BrowserInterfaceUi, JsValue> {
            if !(1..=4096).contains(&width) || !(1..=4096).contains(&height) {
                return Err(fail("interface size out of range"));
            }
            Ok(Self(InterfaceUi::new(width, height)))
        }

        pub fn resize(&mut self, width: i32, height: i32) -> Result<(), JsValue> {
            if !(1..=4096).contains(&width) || !(1..=4096).contains(&height) {
                return Err(fail("interface size out of range"));
            }
            self.0.resize(width, height);
            Ok(())
        }

        #[wasm_bindgen(js_name = setOpen)]
        pub fn set_open(&mut self, top: i32, subs: &[i32]) {
            let mut open = OpenInterfaces { top: (top >= 0).then_some(top), ..Default::default() };
            for chunk in subs.chunks_exact(3) {
                open.subs.insert(chunk[0], (chunk[1], chunk[2] as u8));
            }
            self.0.set_open(open);
        }

        pub fn needs(&self) -> Vec<u32> {
            self.0.needs().into_iter().flat_map(|(i, g)| [i, g]).collect()
        }

        #[wasm_bindgen(js_name = addFile)]
        pub fn add_file(&mut self, index: u32, group: u32, file: u32, bytes: &[u8]) {
            self.0.add_file(index, group, file, bytes.to_vec());
        }

        #[wasm_bindgen(js_name = finishGroup)]
        pub fn finish_group(&mut self, index: u32, group: u32) {
            self.0.finish_group(index, group);
        }

        pub fn absent(&mut self, index: u32, group: u32) {
            self.0.absent(index, group);
        }

        #[allow(clippy::too_many_arguments)]
        pub fn frame(
            &mut self,
            mouse_x: i32,
            mouse_y: i32,
            left_down: bool,
            click_x: i32,
            click_y: i32,
            wheel: i32,
            keys: &[i32],
            held: &[i32],
        ) -> Result<String, JsValue> {
            let input = UiInput {
                mouse_x,
                mouse_y,
                left_down,
                click: (click_x >= 0 && click_y >= 0).then_some((click_x, click_y)),
                wheel,
                keys: keys.chunks_exact(2).map(|k| (k[0], k[1])).collect(),
                held: held.to_vec(),
            };
            serde_json::to_string(&self.0.frame(&input)).map_err(fail)
        }

        pub fn rgba(&self) -> Vec<u8> {
            self.0.rgba()
        }

        pub fn width(&self) -> i32 {
            self.0.size().0
        }

        pub fn height(&self) -> i32 {
            self.0.size().1
        }

        pub fn hooks(&self) -> Result<String, JsValue> {
            serde_json::to_string(&self.0.hooks()).map_err(fail)
        }

        #[wasm_bindgen(js_name = rsKeyCode)]
        pub fn rs_key_code(awt_key_code: i32, down: bool) -> i32 {
            super::rs_key_code(awt_key_code, down)
        }

        #[wasm_bindgen(js_name = validKeyChar)]
        pub fn valid_key_char(unit: u32) -> bool {
            is_valid_key_char(unit)
        }
    }
}

#[cfg(test)]
mod tests;
