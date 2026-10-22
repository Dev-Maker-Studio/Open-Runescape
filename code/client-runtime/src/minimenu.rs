use openrs_protocol::client_packets::MenuClick;
use std::cell::{Cell, RefCell};
use std::rc::Rc;

pub const MAX_ENTRIES: i32 = 520;
pub const CACHE_CAPACITY: usize = 30;
pub const CANCEL_TYPE: i32 = 1006;
pub const CHOOSE_OPTION: &str = "Choose Option";
const DELIMITER: &str = " ";

#[derive(Clone, Debug)]
pub struct Font {
    pub widths: [i32; 256],
    pub top: i32,
    pub bottom: i32,
    pub bold: bool,
}

impl Font {
    pub fn uniform(width: i32, top: i32, bottom: i32, bold: bool) -> Self {
        Self {
            widths: [width; 256],
            top,
            bottom,
            bold,
        }
    }

    pub fn row_height(&self) -> i32 {
        self.bottom + self.top
    }

    pub fn header_height(&self) -> i32 {
        if self.bold { 26 } else { 22 }
    }

    pub fn text_width(&self, text: &str) -> i32 {
        let chars: Vec<char> = text.chars().collect();
        let mut tag_start: Option<usize> = None;
        let mut width = 0;
        for (i, &raw) in chars.iter().enumerate() {
            let mut c = raw;
            if c == '<' {
                tag_start = Some(i);
                continue;
            }
            if c == '>'
                && let Some(start) = tag_start
            {
                let tag: String = chars[start + 1..i].iter().collect();
                tag_start = None;
                c = match tag.as_str() {
                    "lt" => '<',
                    "gt" => '>',
                    "nbsp" => '\u{a0}',
                    "shy" => '\u{ad}',
                    "times" => '\u{d7}',
                    "euro" => '\u{20ac}',
                    "copy" => '\u{a9}',
                    "reg" => '\u{ae}',
                    _ => continue,
                };
            }
            if tag_start.is_none() {
                width += self.widths[usize::from(byte_for_char(c))];
            }
        }
        width
    }
}

fn byte_for_char(c: char) -> u8 {
    let code = c as u32;
    if code < 128 || (160..256).contains(&code) {
        code as u8
    } else if code == 8364 {
        128
    } else {
        63
    }
}

#[derive(Clone, Debug)]
pub struct MenuConfig {
    pub font: Font,
    pub screen_width: i32,
    pub screen_height: i32,
    pub max_visible: i32,
    pub menu_open_mode: i32,
    pub always_show_context: bool,
    pub show_face_here: bool,
    pub cutscene: i32,
    pub default_cursor: i32,
}

impl MenuConfig {
    pub fn new(screen_width: i32, screen_height: i32) -> Self {
        Self {
            font: Font::uniform(6, 12, 3, false),
            screen_width,
            screen_height,
            max_visible: -1,
            menu_open_mode: 0,
            always_show_context: false,
            show_face_here: false,
            cutscene: -1,
            default_cursor: -1,
        }
    }
}

#[derive(Clone, Debug, Default)]
pub struct EntrySpec {
    pub action: String,
    pub colored_name: String,
    pub cursor: i32,
    pub click_type: i32,
    pub item_id: i32,
    pub entity_uid: i64,
    pub param0: i32,
    pub param1: i32,
    pub selectable: bool,
    pub other_level: bool,
    pub bitpacked: i64,
    pub uncategorized: bool,
}

#[derive(Debug)]
pub struct Entry {
    pub id: u32,
    pub action: String,
    pub colored_name: String,
    pub cursor: i32,
    click_type: Cell<i32>,
    pub item_id: i32,
    pub entity_uid: i64,
    pub param0: i32,
    pub param1: i32,
    pub selectable: bool,
    pub other_level: bool,
    pub bitpacked: i64,
    pub uncategorized: bool,
    target_name: RefCell<Option<String>>,
}

impl Entry {
    fn from_spec(id: u32, spec: EntrySpec) -> Self {
        Self {
            id,
            action: spec.action,
            colored_name: spec.colored_name,
            cursor: spec.cursor,
            click_type: Cell::new(spec.click_type),
            item_id: spec.item_id,
            entity_uid: spec.entity_uid,
            param0: spec.param0,
            param1: spec.param1,
            selectable: spec.selectable,
            other_level: spec.other_level,
            bitpacked: spec.bitpacked,
            uncategorized: spec.uncategorized,
            target_name: RefCell::new(None),
        }
    }

    pub fn click_type(&self) -> i32 {
        self.click_type.get()
    }

    pub fn target_name(&self) -> Option<String> {
        self.target_name.borrow().clone()
    }

    pub fn set_target_name(&self, name: Option<String>) {
        *self.target_name.borrow_mut() = name;
    }

    pub fn text(&self) -> String {
        let target = self.target_name.borrow();
        let target = target.as_deref().filter(|t| !t.is_empty());
        let colored = Some(self.colored_name.as_str()).filter(|c| !c.is_empty());
        match (target, colored) {
            (Some(t), Some(c)) => format!("{}{DELIMITER}{c}{DELIMITER}{t}", self.action),
            (Some(t), None) => format!("{}{DELIMITER}{t}", self.action),
            (None, Some(c)) => format!("{}{DELIMITER}{c}", self.action),
            (None, None) => self.action.clone(),
        }
    }

    pub fn click(&self) -> MenuClick {
        MenuClick {
            click_type: self.click_type.get(),
            entity_uid: self.entity_uid,
            param0: self.param0,
            param1: self.param1,
        }
    }
}

#[derive(Debug)]
pub struct Group {
    pub tag: String,
    queue: RefCell<Vec<Rc<Entry>>>,
    queue_size: Cell<i32>,
}

pub fn is_interface_click_type(click_type: i32) -> bool {
    matches!(click_type, 59 | 2 | 8 | 17 | 15 | 16 | 58)
}

pub fn has_higher_priority(base: i32, sub: i32) -> bool {
    sub >= 1000 || (base < 1000 && (is_interface_click_type(base) || !is_interface_click_type(sub)))
}

impl Group {
    fn new(tag: &str) -> Self {
        Self {
            tag: tag.to_string(),
            queue: RefCell::new(Vec::new()),
            queue_size: Cell::new(0),
        }
    }

    pub fn queue_size(&self) -> i32 {
        self.queue_size.get()
    }

    pub fn entries(&self) -> Vec<Rc<Entry>> {
        self.queue.borrow().clone()
    }

    pub fn click_type(&self) -> i32 {
        self.queue.borrow().first().map_or(-1, |e| e.click_type())
    }

    fn add_entry(&self, base: &Rc<Entry>) -> bool {
        let mut is_first = true;
        let mut queue = self.queue.borrow_mut();
        for (index, sub) in queue.iter().enumerate() {
            if has_higher_priority(base.click_type(), sub.click_type()) {
                queue.insert(index, Rc::clone(base));
                self.queue_size.set(self.queue_size.get() + 1);
                return !is_first;
            }
            is_first = false;
        }
        queue.push(Rc::clone(base));
        self.queue_size.set(self.queue_size.get() + 1);
        is_first
    }
}

#[derive(Clone, Copy, Debug, Default)]
pub struct MenuInput {
    pub primary: bool,
    pub secondary: bool,
    pub context: bool,
    pub open_click: bool,
    pub minimenu_key: bool,
    pub mouse: (i32, i32),
    pub dragging: bool,
    pub item_count: i32,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MenuOutcome {
    pub click: Option<MenuClick>,
    pub item_id: i32,
    pub cancel_selection: bool,
}

impl MenuOutcome {
    fn pick(&mut self, entry: Option<Rc<Entry>>) {
        if let Some(entry) = entry {
            self.click = Some(entry.click());
            self.item_id = entry.item_id;
        }
    }
}

pub struct MiniMenu {
    pub config: MenuConfig,
    list: Vec<Rc<Entry>>,
    sorted: Vec<Rc<Group>>,
    map: Vec<(i64, Rc<Group>)>,
    lru: Vec<(i64, Rc<Group>)>,
    cancel: Rc<Entry>,
    next_id: u32,
    pub entry_count: i32,
    pub colored_entry_count: i32,
    pub is_open: bool,
    pub is_collapsed: bool,
    active: Option<Rc<Entry>>,
    top: Option<Rc<Entry>>,
    secondary: Option<Rc<Entry>>,
    pub menu_open_x: i32,
    pub menu_open_y: i32,
    pub menu_width: i32,
    pub menu_height: i32,
    pub submenu_x: i32,
    pub submenu_y: i32,
    pub submenu_width: i32,
    pub submenu_height: i32,
    active_submenu: Option<Rc<Group>>,
    pub deferred_click_type: i32,
    deferred_entry: Option<Rc<Entry>>,
}

impl MiniMenu {
    pub fn new(config: MenuConfig) -> Self {
        let cancel = Rc::new(Entry::from_spec(
            0,
            EntrySpec {
                action: "Cancel".to_string(),
                cursor: 0,
                click_type: CANCEL_TYPE,
                item_id: -1,
                selectable: true,
                uncategorized: true,
                ..EntrySpec::default()
            },
        ));
        Self {
            config,
            list: Vec::new(),
            sorted: Vec::new(),
            map: Vec::new(),
            lru: Vec::new(),
            cancel,
            next_id: 1,
            entry_count: 0,
            colored_entry_count: 0,
            is_open: false,
            is_collapsed: false,
            active: None,
            top: None,
            secondary: None,
            menu_open_x: 0,
            menu_open_y: 0,
            menu_width: 0,
            menu_height: 0,
            submenu_x: 0,
            submenu_y: 0,
            submenu_width: 0,
            submenu_height: 0,
            active_submenu: None,
            deferred_click_type: 0,
            deferred_entry: None,
        }
    }

    pub fn entries(&self) -> Vec<Rc<Entry>> {
        self.list.clone()
    }

    pub fn groups(&self) -> Vec<Rc<Group>> {
        self.sorted.clone()
    }

    pub fn active(&self) -> Option<&Rc<Entry>> {
        self.active.as_ref()
    }

    pub fn top(&self) -> Option<&Rc<Entry>> {
        self.top.as_ref()
    }

    pub fn secondary(&self) -> Option<&Rc<Entry>> {
        self.secondary.as_ref()
    }

    pub fn deferred_entry(&self) -> Option<&Rc<Entry>> {
        self.deferred_entry.as_ref()
    }

    pub fn active_submenu_index(&self) -> i32 {
        self.active_submenu
            .as_ref()
            .and_then(|g| self.sorted.iter().position(|s| Rc::ptr_eq(s, g)))
            .map_or(-1, |i| i as i32)
    }

    pub fn open_submenu_group(&self) -> Option<&Rc<Group>> {
        self.active_submenu.as_ref()
    }

    pub fn action_text(&self, entry: Option<&Rc<Entry>>) -> String {
        match entry {
            Some(e) if !self.is_open => e.action.clone(),
            _ => String::new(),
        }
    }

    pub fn entry_width(&self, entry: &Entry) -> i32 {
        let mut width = self.config.font.text_width(&entry.text());
        if entry.other_level {
            width += 4;
        }
        width
    }

    pub fn format_tag(group: &Group) -> String {
        format!("{}<col=ffffff> >", group.tag)
    }

    fn lru_get(&mut self, key: i64) -> Option<Rc<Group>> {
        let index = self.lru.iter().position(|(k, _)| *k == key)?;
        let node = self.lru.remove(index);
        let group = Rc::clone(&node.1);
        self.lru.push(node);
        Some(group)
    }

    fn lru_put(&mut self, group: Rc<Group>, key: i64) {
        if let Some(index) = self.lru.iter().position(|(k, _)| *k == key) {
            self.lru.remove(index);
        }
        self.lru.push((key, group));
        while self.lru.len() > CACHE_CAPACITY {
            self.lru.remove(0);
        }
    }

    fn map_put(&mut self, group: Rc<Group>, key: i64) {
        self.map.retain(|(_, g)| !Rc::ptr_eq(g, &group));
        self.map.push((key, group));
    }

    fn insert_sorted(&mut self, group: &Rc<Group>) {
        self.sorted.retain(|g| !Rc::ptr_eq(g, group));
        let base = group.click_type();
        let position = self
            .sorted
            .iter()
            .position(|existing| has_higher_priority(base, existing.click_type()));
        match position {
            Some(index) => self.sorted.insert(index + 1, Rc::clone(group)),
            None => self.sorted.push(Rc::clone(group)),
        }
    }

    fn add_entry_object(&mut self, entry: Rc<Entry>) {
        self.list.push(Rc::clone(&entry));
        self.entry_count += 1;
        let group = if !entry.uncategorized && !entry.colored_name.is_empty() {
            let hash = entry.bitpacked;
            let found = self
                .map
                .iter()
                .find(|(key, g)| *key == hash && g.tag == entry.colored_name)
                .map(|(_, g)| Rc::clone(g));
            match found {
                Some(group) => group,
                None => {
                    let cached = self.lru_get(hash).filter(|g| g.tag == entry.colored_name);
                    let group = cached.unwrap_or_else(|| Rc::new(Group::new(&entry.colored_name)));
                    self.map_put(Rc::clone(&group), hash);
                    self.colored_entry_count += 1;
                    group
                }
            }
        } else {
            self.colored_entry_count += 1;
            Rc::new(Group::new(&entry.colored_name))
        };
        if group.add_entry(&entry) {
            self.insert_sorted(&group);
        }
    }

    pub fn add_entry(&mut self, spec: EntrySpec) -> Option<Rc<Entry>> {
        if self.is_open || self.entry_count >= MAX_ENTRIES {
            return None;
        }
        let mut spec = spec;
        if spec.cursor == -1 {
            spec.cursor = self.config.default_cursor;
        }
        let entry = Rc::new(Entry::from_spec(self.next_id, spec));
        self.next_id += 1;
        self.add_entry_object(Rc::clone(&entry));
        Some(entry)
    }

    pub fn reset_entries(&mut self) {
        let sorted = std::mem::take(&mut self.sorted);
        for group in &sorted {
            if group.queue_size.get() > 1 {
                group.queue_size.set(0);
                let key = group.queue.borrow().first().map_or(0, |e| e.bitpacked);
                self.lru_put(Rc::clone(group), key);
                group.queue.borrow_mut().clear();
            }
        }
        self.colored_entry_count = 0;
        self.entry_count = 0;
        self.list.clear();
        self.map.clear();
        let cancel = Rc::clone(&self.cancel);
        self.add_entry_object(cancel);
    }

    pub fn close(&mut self) {
        self.close_submenu();
        self.is_open = false;
        self.active = None;
        self.secondary = None;
    }

    pub fn close_submenu(&mut self) {
        self.active_submenu = None;
    }

    pub fn open_context_menu(&mut self, mouse_x: i32, mouse_y: i32) {
        let top_is_face = self.top.as_ref().is_some_and(|t| t.action == "Face here");
        let always = self.config.always_show_context;
        let show_face = self.config.show_face_here;
        let open = always
            || self.entry_count != 1 && (!show_face || self.entry_count != 2 || !top_is_face);
        if !open {
            return;
        }
        let font = self.config.font.clone();
        let row = font.row_height();
        let mut w = font.text_width(CHOOSE_OPTION);
        let local_h;
        if !self.is_collapsed {
            for entry in &self.list {
                w = w.max(self.entry_width(entry));
            }
            w += 8;
            local_h = self.entry_count * row + 21;
            self.menu_height = font.header_height() + self.entry_count * row;
        } else {
            for group in &self.sorted {
                let width = if group.queue_size.get() == 1 {
                    group
                        .queue
                        .borrow()
                        .first()
                        .map_or(0, |e| self.entry_width(e))
                } else {
                    font.text_width(&Self::format_tag(group))
                };
                w = w.max(width);
            }
            w += 8;
            local_h = row * self.colored_entry_count + 21;
            self.menu_height = row * self.colored_entry_count + font.header_height();
        }
        w += 10;
        let mut x = mouse_x - w / 2;
        if x + w > self.config.screen_width {
            x = self.config.screen_width - w;
        }
        if x < 0 {
            x = 0;
        }
        let mut y = mouse_y;
        if y + local_h > self.config.screen_height {
            y = self.config.screen_height - local_h;
        }
        if y < 0 {
            y = 0;
        }
        self.menu_open_x = x;
        self.menu_open_y = y;
        self.menu_width = w;
        self.is_open = true;
    }

    pub fn open_submenu(&mut self, group: &Rc<Group>, screen_y: i32) {
        if !self.is_open {
            return;
        }
        let font = self.config.font.clone();
        let row = font.row_height();
        let mut max_w = 0;
        for entry in group.queue.borrow().iter() {
            max_w = max_w.max(self.entry_width(entry));
        }
        max_w += 8;
        let size = group.queue_size.get();
        let h = size * row + 21;
        self.submenu_height = font.header_height() + row * size;
        let mut x = self.menu_open_x + self.menu_width;
        if max_w + x > self.config.screen_width {
            x = self.menu_open_x - max_w;
        }
        if x < 0 {
            x = 0;
        }
        let header = if font.bold { font.top + 1 + 20 } else { 31 };
        let mut y = screen_y - header + font.top + 1;
        if h + y > self.config.screen_height {
            y = self.config.screen_height - h;
        }
        if y < 0 {
            y = 0;
        }
        self.submenu_x = x;
        self.submenu_y = y;
        self.submenu_width = max_w;
        self.active_submenu = Some(Rc::clone(group));
    }

    fn is_top_entry_interface_action(&self) -> bool {
        match &self.top {
            None => false,
            Some(top) => {
                if top.click_type.get() >= 2000 {
                    top.click_type.set(top.click_type.get() - 2000);
                }
                top.click_type.get() == 1007
            }
        }
    }

    fn reorder(&mut self) {
        let mut keep = Vec::new();
        let mut low = Vec::new();
        let mut interface = Vec::new();
        for entry in self.list.drain(..) {
            let click_type = entry.click_type();
            if click_type >= 1000 {
                keep.push(entry);
            } else if is_interface_click_type(click_type) {
                interface.push(entry);
            } else {
                low.push(entry);
            }
        }
        keep.extend(low);
        keep.extend(interface);
        self.list = keep;
    }

    fn row_hit(&self, mouse_y: i32, row_index: i32, base_y: i32) -> bool {
        let font = &self.config.font;
        let row = font.row_height();
        if font.bold {
            let entry_y = row_index * row + font.top + base_y + 1 + 20;
            mouse_y > entry_y - font.top - 1 && mouse_y < entry_y + font.bottom
        } else {
            let entry_y = row_index * row + base_y + 31;
            mouse_y > entry_y - 13 && mouse_y < entry_y + 3
        }
    }

    pub fn process_input(&mut self, input: &MenuInput) -> MenuOutcome {
        let mut outcome = MenuOutcome::default();
        if self.config.cutscene >= 0 {
            return outcome;
        }
        let font = self.config.font.clone();
        let row = font.row_height();
        if !self.is_open {
            self.is_collapsed = (self.config.max_visible != -1
                && self.entry_count >= self.config.max_visible)
                || font.header_height() + row * self.entry_count > self.config.screen_height;
        }
        self.reorder();
        if self.entry_count <= 1 {
            self.active = None;
            self.top = None;
            self.secondary = None;
        } else {
            let n = self.list.len();
            self.top = self.list.last().cloned();
            self.active = if input.minimenu_key && self.entry_count > 2 {
                self.list.get(n - 2).cloned()
            } else {
                self.list.last().cloned()
            };
            self.secondary = if self.entry_count > 2 {
                self.list.get(n - 2).cloned()
            } else {
                None
            };
        }
        let (mx, my) = input.mouse;
        if self.is_open {
            if input.open_click {
                self.open_click(input, &mut outcome);
            } else {
                self.open_hover(mx, my);
            }
        } else {
            self.closed_click(input, &mut outcome);
        }
        outcome
    }

    fn open_click(&mut self, input: &MenuInput, outcome: &mut MenuOutcome) {
        let (mx, my) = input.mouse;
        let in_submenu = self.active_submenu.is_some()
            && mx >= self.submenu_x
            && mx <= self.submenu_x + self.submenu_width
            && my >= self.submenu_y
            && my <= self.submenu_y + self.submenu_height;
        if in_submenu {
            if let Some(group) = self.active_submenu.clone() {
                let mut hovered = -1;
                for i in 0..group.queue_size.get() {
                    if self.row_hit(my, i, self.submenu_y) {
                        hovered = i;
                    }
                }
                if hovered != -1 {
                    let entry = group.queue.borrow().get(hovered as usize).cloned();
                    outcome.pick(entry);
                }
            }
            self.close();
            return;
        }
        let in_main = mx >= self.menu_open_x
            && mx <= self.menu_open_x + self.menu_width
            && my >= self.menu_open_y
            && my <= self.menu_open_y + self.menu_height;
        if !in_main {
            return;
        }
        if !self.is_collapsed {
            let mut hovered = -1;
            for i in 0..self.entry_count {
                let from_bottom = self.entry_count - 1 - i;
                if self.row_hit(my, from_bottom, self.menu_open_y) {
                    hovered = i;
                }
            }
            if hovered != -1 {
                outcome.pick(self.list.get(hovered as usize).cloned());
            }
            self.close();
        } else {
            let mut hovered = -1;
            for i in 0..self.colored_entry_count {
                if self.row_hit(my, i, self.menu_open_y) {
                    hovered = i;
                    break;
                }
            }
            if hovered != -1
                && let Some(group) = self.sorted.get(hovered as usize).cloned()
            {
                let entry = group.queue.borrow().first().cloned();
                outcome.pick(entry);
                self.close();
            }
        }
    }

    fn open_hover(&mut self, mx: i32, my: i32) {
        let mut in_submenu = false;
        if self.active_submenu.is_some() {
            if mx >= self.submenu_x - 10
                && mx <= self.submenu_x + self.submenu_width + 10
                && my >= self.submenu_y - 10
                && my <= self.submenu_y + self.submenu_height + 10
            {
                in_submenu = true;
            } else {
                self.close_submenu();
            }
        }
        if in_submenu {
            return;
        }
        let in_main = mx >= self.menu_open_x - 10
            && mx <= self.menu_open_x + self.menu_width + 10
            && my >= self.menu_open_y - 10
            && my <= self.menu_open_y + self.menu_height + 10;
        if !in_main {
            self.close();
            return;
        }
        if self.is_collapsed {
            let font = self.config.font.clone();
            let row = font.row_height();
            let mut target = None;
            for i in 0..self.colored_entry_count {
                let hit = if font.bold {
                    let y = i * row + font.top + self.menu_open_y + 1 + 20;
                    (my > y - font.top - 1 && my < y + font.bottom).then_some(y - font.top - 1)
                } else {
                    let y = i * row + self.menu_open_y + 31;
                    (my > y - 13 && my < y + 3).then_some(y - 13)
                };
                if let Some(entry_y) = hit {
                    target = Some((i, entry_y));
                    break;
                }
            }
            if let Some((i, entry_y)) = target
                && let Some(group) = self.sorted.get(i as usize).cloned()
                && group.queue_size.get() > 1
            {
                self.open_submenu(&group, entry_y);
            }
        }
    }

    fn closed_click(&mut self, input: &MenuInput, outcome: &mut MenuOutcome) {
        let (mx, my) = input.mouse;
        let mut context = input.context;
        if (input.primary || input.secondary)
            && (self.config.menu_open_mode == 1 && self.entry_count > 2
                || self.is_top_entry_interface_action())
        {
            context = true;
        }
        let deferring = input.dragging || input.item_count > 0;
        if context && self.entry_count > 0 {
            if !deferring {
                self.open_context_menu(mx, my);
            } else {
                self.deferred_click_type = 2;
            }
        } else if input.secondary {
            outcome.pick(self.secondary.clone());
        } else if input.primary {
            match self.active.clone() {
                None => outcome.cancel_selection = true,
                Some(active) => {
                    if deferring {
                        self.deferred_click_type = 1;
                        self.deferred_entry = Some(active);
                    } else {
                        outcome.pick(Some(active));
                    }
                }
            }
        }
        if !deferring {
            self.deferred_click_type = 0;
            self.deferred_entry = None;
        }
    }
}
