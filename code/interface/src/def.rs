use openrs_protocol::payload::{PayloadError, PayloadReader};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ComponentType {
    Container,
    Type1,
    Type2,
    Figure,
    Text,
    Sprite,
    Model,
    Type7,
    Type8,
    Line,
}

impl ComponentType {
    pub fn for_id(id: u8) -> Option<Self> {
        Some(match id {
            0 => Self::Container,
            1 => Self::Type1,
            2 => Self::Type2,
            3 => Self::Figure,
            4 => Self::Text,
            5 => Self::Sprite,
            6 => Self::Model,
            7 => Self::Type7,
            8 => Self::Type8,
            9 => Self::Line,
            _ => return None,
        })
    }

    pub fn id(self) -> i32 {
        match self {
            Self::Container => 0,
            Self::Type1 => 1,
            Self::Type2 => 2,
            Self::Figure => 3,
            Self::Text => 4,
            Self::Sprite => 5,
            Self::Model => 6,
            Self::Type7 => 7,
            Self::Type8 => 8,
            Self::Line => 9,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ContentType {
    Type328,
    GameWindow,
    Type1403,
    Minimap,
    Compass,
    WorldMap,
    WorldMapOverview,
    DebugInformation,
    Type1406,
    CutsceneFade,
    ColorPicker,
    Type1409,
}

impl ContentType {
    pub fn for_id(id: u16) -> Option<Self> {
        Some(match id {
            328 => Self::Type328,
            1337 => Self::GameWindow,
            1403 => Self::Type1403,
            1338 => Self::Minimap,
            1339 => Self::Compass,
            1400 => Self::WorldMap,
            1401 => Self::WorldMapOverview,
            1405 => Self::DebugInformation,
            1406 => Self::Type1406,
            1407 => Self::CutsceneFade,
            1408 => Self::ColorPicker,
            1409 => Self::Type1409,
            _ => return None,
        })
    }

    pub fn id(self) -> i32 {
        match self {
            Self::Type328 => 328,
            Self::GameWindow => 1337,
            Self::Type1403 => 1403,
            Self::Minimap => 1338,
            Self::Compass => 1339,
            Self::WorldMap => 1400,
            Self::WorldMapOverview => 1401,
            Self::DebugInformation => 1405,
            Self::Type1406 => 1406,
            Self::CutsceneFade => 1407,
            Self::ColorPicker => 1408,
            Self::Type1409 => 1409,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DragType {
    Type0,
    Type1,
    Type2,
}

impl DragType {
    pub fn for_id(id: u8) -> Option<Self> {
        match id {
            0 => Some(Self::Type0),
            1 => Some(Self::Type1),
            2 => Some(Self::Type2),
            _ => None,
        }
    }

    pub fn value(self) -> i32 {
        match self {
            Self::Type0 => 0,
            Self::Type1 => 1,
            Self::Type2 => 2,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ScriptArg {
    Int(i32),
    Str(String),
    Null,
}

pub type Script = Option<Vec<ScriptArg>>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParamValue {
    Int(i32),
    Str(String),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct IfEvents {
    pub settings_hash: i32,
    pub interface_id: i32,
}

impl IfEvents {
    pub fn use_option_flags(self) -> i32 {
        (self.settings_hash >> 11) & 0x7f
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DecodeError {
    Payload(PayloadError),
    KeySlot(i32),
    CursorIndex(usize),
}

impl From<PayloadError> for DecodeError {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

impl std::fmt::Display for DecodeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Payload(error) => error.fmt(f),
            Self::KeySlot(slot) => write!(f, "key trigger slot {slot} is outside 0..11"),
            Self::CursorIndex(index) => write!(f, "option cursor index {index} is out of bounds"),
        }
    }
}

impl std::error::Error for DecodeError {}

#[derive(Debug, Clone, PartialEq)]
pub struct Component {
    pub id_hash: i32,
    pub format: i32,
    pub kind: Option<ComponentType>,
    pub name: Option<String>,
    pub content_type: Option<ContentType>,
    pub base_x: i32,
    pub base_y: i32,
    pub base_width: i32,
    pub base_height: i32,
    pub aspect_width_type: i8,
    pub aspect_height_type: i8,
    pub aspect_x_type: i8,
    pub aspect_y_type: i8,
    pub parent_id: i32,
    pub hidden: bool,
    pub prevent_click_through: bool,
    pub scroll_width: i32,
    pub scroll_height: i32,
    pub sprite_id: i32,
    pub angle_2d: i32,
    pub tiling: bool,
    pub alpha: bool,
    pub transparency: i32,
    pub border_thickness: i32,
    pub sprite_shadow: i32,
    pub flip_vertical: bool,
    pub flip_horizontal: bool,
    pub color: i32,
    pub click_mask: bool,
    pub primary_media_id: i32,
    pub has_origin: bool,
    pub uses_orthogonal: bool,
    pub priority_render: bool,
    pub origin_x: i32,
    pub origin_y: i32,
    pub origin_z: i32,
    pub sprite_pitch: i32,
    pub sprite_roll: i32,
    pub sprite_yaw: i32,
    pub sprite_scale: i32,
    pub animation: i32,
    pub aspect_width: i32,
    pub aspect_height: i32,
    pub font_id: i32,
    pub monospaced: bool,
    pub text: String,
    pub line_spacing: i32,
    pub text_horizontal_ali: i32,
    pub text_vertical_ali: i32,
    pub shadow: bool,
    pub max_text_lines: i32,
    pub filled: bool,
    pub line_width: i32,
    pub line_direction: bool,
    pub key_trigger_action: Option<[Option<i8>; 11]>,
    pub key_trigger_param: Option<[Option<i8>; 11]>,
    pub cycle_steps: Option<[i32; 11]>,
    pub key_char_codes: Option<[i32; 11]>,
    pub has_interaction: bool,
    pub op_base: String,
    pub options: Option<Vec<String>>,
    pub op_cursors: Option<Vec<i32>>,
    pub resume_text: Option<String>,
    pub drag_deadzone: i32,
    pub drag_dead_time: i32,
    pub drag_type: Option<DragType>,
    pub target_verb: String,
    pub target_over_cursor: i32,
    pub target_leave_cursor: i32,
    pub move_over_cursor: i32,
    pub target_params: IfEvents,
    pub client_params: Vec<(i32, ParamValue)>,
    pub on_load: Script,
    pub on_mouse_over: Script,
    pub on_mouse_leave: Script,
    pub hook_params: Script,
    pub on_target_enter: Script,
    pub on_varp_transmit: Script,
    pub mouse_leave_script: Script,
    pub on_stat_transmit: Script,
    pub on_timer: Script,
    pub params: Script,
    pub on_target_leave: Script,
    pub popup_script: Script,
    pub on_click: Script,
    pub on_click_repeat: Script,
    pub on_release: Script,
    pub on_hold: Script,
    pub on_drag: Script,
    pub on_drag_complete: Script,
    pub on_mouse_move: Script,
    pub on_key: Script,
    pub on_scroll_wheel: Script,
    pub on_key_type: Script,
    pub has_script: bool,
    pub varps: Option<Vec<i32>>,
    pub mouse_leave_array_params: Option<Vec<i32>>,
    pub stat_transmit_filter: Option<Vec<i32>>,
    pub key_press_array: Option<Vec<i32>>,
    pub mouse_wheel_array: Option<Vec<i32>>,
    pub on_resize: Script,
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
    pub scroll_x: i32,
    pub scroll_y: i32,
    pub aspect_x: i32,
    pub aspect_y: i32,
    pub container_item_id: i32,
    pub item_stack_size: i32,
    pub slot_id: i32,
}

impl Component {
    pub fn blank(id_hash: i32) -> Self {
        Self {
            id_hash,
            format: -1,
            kind: None,
            name: None,
            content_type: None,
            base_x: 0,
            base_y: 0,
            base_width: 0,
            base_height: 0,
            aspect_width_type: 0,
            aspect_height_type: 0,
            aspect_x_type: 0,
            aspect_y_type: 0,
            parent_id: -1,
            hidden: false,
            prevent_click_through: false,
            scroll_width: 0,
            scroll_height: 0,
            sprite_id: -1,
            angle_2d: 0,
            tiling: false,
            alpha: false,
            transparency: 0,
            border_thickness: 0,
            sprite_shadow: 0,
            flip_vertical: false,
            flip_horizontal: false,
            color: 0,
            click_mask: true,
            primary_media_id: 0,
            has_origin: false,
            uses_orthogonal: false,
            priority_render: false,
            origin_x: 0,
            origin_y: 0,
            origin_z: 0,
            sprite_pitch: 0,
            sprite_roll: 0,
            sprite_yaw: 0,
            sprite_scale: 100,
            animation: -1,
            aspect_width: 0,
            aspect_height: 0,
            font_id: -1,
            monospaced: true,
            text: String::new(),
            line_spacing: 0,
            text_horizontal_ali: 0,
            text_vertical_ali: 0,
            shadow: false,
            max_text_lines: 0,
            filled: false,
            line_width: 1,
            line_direction: false,
            key_trigger_action: None,
            key_trigger_param: None,
            cycle_steps: None,
            key_char_codes: None,
            has_interaction: false,
            op_base: String::new(),
            options: None,
            op_cursors: None,
            resume_text: None,
            drag_deadzone: 0,
            drag_dead_time: 0,
            drag_type: Some(DragType::Type0),
            target_verb: String::new(),
            target_over_cursor: -1,
            target_leave_cursor: -1,
            move_over_cursor: -1,
            target_params: IfEvents {
                settings_hash: 0,
                interface_id: -1,
            },
            client_params: Vec::new(),
            on_load: None,
            on_mouse_over: None,
            on_mouse_leave: None,
            hook_params: None,
            on_target_enter: None,
            on_varp_transmit: None,
            mouse_leave_script: None,
            on_stat_transmit: None,
            on_timer: None,
            params: None,
            on_target_leave: None,
            popup_script: None,
            on_click: None,
            on_click_repeat: None,
            on_release: None,
            on_hold: None,
            on_drag: None,
            on_drag_complete: None,
            on_mouse_move: None,
            on_key: None,
            on_scroll_wheel: None,
            on_key_type: None,
            has_script: false,
            varps: None,
            mouse_leave_array_params: None,
            stat_transmit_filter: None,
            key_press_array: None,
            mouse_wheel_array: None,
            on_resize: None,
            x: 0,
            y: 0,
            width: 0,
            height: 0,
            scroll_x: 0,
            scroll_y: 0,
            aspect_x: 1,
            aspect_y: 1,
            container_item_id: -1,
            item_stack_size: 0,
            slot_id: -1,
        }
    }

    pub fn decode(bytes: &[u8], id_hash: i32) -> Result<Self, DecodeError> {
        let mut c = Self::blank(id_hash);
        c.read(&mut PayloadReader::new(bytes))?;
        Ok(c)
    }

    pub fn int_param(&self, key: i32, default: i32) -> i32 {
        match self.client_params.iter().find(|(k, _)| *k == key) {
            Some((_, ParamValue::Int(value))) => *value,
            _ => default,
        }
    }

    pub fn string_param<'a>(&'a self, key: i32, default: &'a str) -> &'a str {
        match self.client_params.iter().find(|(k, _)| *k == key) {
            Some((_, ParamValue::Str(value))) => value,
            _ => default,
        }
    }

    pub fn is_type(&self, kind: ComponentType) -> bool {
        self.kind == Some(kind)
    }

    fn read(&mut self, r: &mut PayloadReader) -> Result<(), DecodeError> {
        let mut format = i32::from(r.read_u8()?);
        if format == 255 {
            format = -1;
        }
        self.format = format;
        let mut type_id = r.read_u8()?;
        if type_id & 0x80 != 0 {
            type_id &= 0x7f;
            self.name = Some(r.read_cp1252()?);
        }
        self.kind = ComponentType::for_id(type_id);
        self.content_type = ContentType::for_id(r.read_u16_be()?);
        self.base_x = i32::from(r.read_i16_be()?);
        self.base_y = i32::from(r.read_i16_be()?);
        self.base_width = i32::from(r.read_u16_be()?);
        self.base_height = i32::from(r.read_u16_be()?);
        self.aspect_width_type = r.read_i8()?;
        self.aspect_height_type = r.read_i8()?;
        self.aspect_x_type = r.read_i8()?;
        self.aspect_y_type = r.read_i8()?;
        self.parent_id = i32::from(r.read_u16_be()?);
        if self.parent_id == 65535 {
            self.parent_id = -1;
        } else {
            self.parent_id = self.parent_id.wrapping_add(self.id_hash & -65536);
        }
        let interaction_mask = r.read_u8()?;
        self.hidden = interaction_mask & 0x1 != 0;
        if format >= 0 {
            self.prevent_click_through = interaction_mask & 0x2 != 0;
        }
        if self.kind == Some(ComponentType::Container) {
            self.scroll_width = i32::from(r.read_u16_be()?);
            self.scroll_height = i32::from(r.read_u16_be()?);
            if format < 0 {
                self.prevent_click_through = r.read_u8()? == 1;
            }
        }
        if self.kind == Some(ComponentType::Sprite) {
            self.sprite_id = r.read_i32_be()?;
            self.angle_2d = i32::from(r.read_u16_be()?);
            let display_mask = r.read_u8()?;
            self.tiling = display_mask & 0x1 != 0;
            self.alpha = display_mask & 0x2 != 0;
            self.transparency = i32::from(r.read_u8()?);
            self.border_thickness = i32::from(r.read_u8()?);
            self.sprite_shadow = r.read_i32_be()?;
            self.flip_vertical = r.read_u8()? == 1;
            self.flip_horizontal = r.read_u8()? == 1;
            self.color = r.read_i32_be()?;
            if format >= 3 {
                self.click_mask = r.read_u8()? == 1;
            }
        }
        if self.kind == Some(ComponentType::Model) {
            self.primary_media_id = r.read_big_smart()?;
            let flags = r.read_u8()?;
            let has_transform = flags & 0x1 == 1;
            self.has_origin = flags & 0x2 == 2;
            self.uses_orthogonal = flags & 0x4 == 4;
            self.priority_render = flags & 0x8 == 8;
            if has_transform {
                self.origin_x = i32::from(r.read_i16_be()?);
                self.origin_y = i32::from(r.read_i16_be()?);
                self.sprite_pitch = i32::from(r.read_u16_be()?);
                self.sprite_roll = i32::from(r.read_u16_be()?);
                self.sprite_yaw = i32::from(r.read_u16_be()?);
                self.sprite_scale = i32::from(r.read_u16_be()?);
            } else if self.has_origin {
                self.origin_x = i32::from(r.read_i16_be()?);
                self.origin_y = i32::from(r.read_i16_be()?);
                self.origin_z = i32::from(r.read_i16_be()?);
                self.sprite_pitch = i32::from(r.read_u16_be()?);
                self.sprite_roll = i32::from(r.read_u16_be()?);
                self.sprite_yaw = i32::from(r.read_u16_be()?);
                self.sprite_scale = i32::from(r.read_i16_be()?);
            }
            self.animation = r.read_big_smart()?;
            if self.aspect_width_type != 0 {
                self.aspect_width = i32::from(r.read_u16_be()?);
            }
            if self.aspect_height_type != 0 {
                self.aspect_height = i32::from(r.read_u16_be()?);
            }
        }
        if self.kind == Some(ComponentType::Text) {
            self.font_id = r.read_big_smart()?;
            if format >= 2 {
                self.monospaced = r.read_u8()? == 1;
            }
            self.text = brand_text(r.read_cp1252()?);
            self.line_spacing = i32::from(r.read_u8()?);
            self.text_horizontal_ali = i32::from(r.read_u8()?);
            self.text_vertical_ali = i32::from(r.read_u8()?);
            self.shadow = r.read_u8()? == 1;
            self.color = r.read_i32_be()?;
            self.transparency = i32::from(r.read_u8()?);
            if format >= 0 {
                self.max_text_lines = i32::from(r.read_u8()?);
            }
        }
        if self.kind == Some(ComponentType::Figure) {
            self.color = r.read_i32_be()?;
            self.filled = r.read_u8()? == 1;
            self.transparency = i32::from(r.read_u8()?);
        }
        if self.kind == Some(ComponentType::Line) {
            self.line_width = i32::from(r.read_u8()?);
            self.color = r.read_i32_be()?;
            self.line_direction = r.read_u8()? == 1;
        }
        let option_mask = r.read_u24_be()? as i32;
        let mut trigger = i32::from(r.read_u8()?);
        if trigger != 0 {
            let mut actions = [None; 11];
            let mut params = [None; 11];
            let mut steps = [0; 11];
            while trigger != 0 {
                let slot = (trigger >> 4) - 1;
                trigger = ((trigger << 8) | i32::from(r.read_u8()?)) & 0xfff;
                if trigger == 4095 {
                    trigger = -1;
                }
                let action = r.read_i8()?;
                if action != 0 {
                    self.has_interaction = true;
                }
                let param = r.read_i8()?;
                if !(0..11).contains(&slot) {
                    return Err(DecodeError::KeySlot(slot));
                }
                let slot = slot as usize;
                steps[slot] = trigger;
                actions[slot] = Some(action);
                params[slot] = Some(param);
                trigger = i32::from(r.read_u8()?);
            }
            self.key_trigger_action = Some(actions);
            self.key_trigger_param = Some(params);
            self.cycle_steps = Some(steps);
            self.key_char_codes = Some([0; 11]);
        }
        self.op_base = r.read_cp1252()?;
        let counts = r.read_u8()?;
        let option_count = usize::from(counts & 0xf);
        let cursor_count = counts >> 4;
        if option_count > 0 {
            let mut options = Vec::with_capacity(option_count);
            for _ in 0..option_count {
                options.push(r.read_cp1252()?);
            }
            self.options = Some(options);
        }
        if cursor_count > 0 {
            let index = usize::from(r.read_u8()?);
            let mut cursors = vec![-1; index + 1];
            cursors[index] = i32::from(r.read_u16_be()?);
            self.op_cursors = Some(cursors);
        }
        if cursor_count > 1 {
            let index = usize::from(r.read_u8()?);
            let value = i32::from(r.read_u16_be()?);
            let cursors = self.op_cursors.as_mut().ok_or(DecodeError::CursorIndex(index))?;
            *cursors
                .get_mut(index)
                .ok_or(DecodeError::CursorIndex(index))? = value;
        }
        let resume = r.read_cp1252()?;
        self.resume_text = if resume.is_empty() { None } else { Some(resume) };
        self.drag_deadzone = i32::from(r.read_u8()?);
        self.drag_dead_time = i32::from(r.read_u8()?);
        self.drag_type = DragType::for_id(r.read_u8()?);
        self.target_verb = r.read_cp1252()?;
        let mut interface_id = -1;
        if (option_mask >> 11) & 0x7f != 0 {
            interface_id = ushort_or_none(r.read_u16_be()?);
            self.target_over_cursor = ushort_or_none(r.read_u16_be()?);
            self.target_leave_cursor = ushort_or_none(r.read_u16_be()?);
        }
        if format >= 0 {
            self.move_over_cursor = ushort_or_none(r.read_u16_be()?);
        }
        self.target_params = IfEvents {
            settings_hash: option_mask,
            interface_id,
        };
        if format >= 0 {
            let count = r.read_u8()?;
            for _ in 0..count {
                let key = r.read_u24_be()? as i32;
                let value = r.read_i32_be()?;
                self.client_params.push((key, ParamValue::Int(value)));
            }
            let count = r.read_u8()?;
            for _ in 0..count {
                let key = r.read_u24_be()? as i32;
                let value = r.read_jag_cp1252()?;
                self.client_params.push((key, ParamValue::Str(value)));
            }
        }
        self.on_load = self.script(r)?;
        self.on_mouse_over = self.script(r)?;
        self.on_mouse_leave = self.script(r)?;
        self.hook_params = self.script(r)?;
        self.on_target_enter = self.script(r)?;
        self.on_varp_transmit = self.script(r)?;
        self.mouse_leave_script = self.script(r)?;
        self.on_stat_transmit = self.script(r)?;
        self.on_timer = self.script(r)?;
        self.params = self.script(r)?;
        if format >= 0 {
            self.on_target_leave = self.script(r)?;
        }
        self.popup_script = self.script(r)?;
        self.on_click = self.script(r)?;
        self.on_click_repeat = self.script(r)?;
        self.on_release = self.script(r)?;
        self.on_hold = self.script(r)?;
        self.on_drag = self.script(r)?;
        self.on_drag_complete = self.script(r)?;
        self.on_mouse_move = self.script(r)?;
        self.on_key = self.script(r)?;
        self.on_scroll_wheel = self.script(r)?;
        self.varps = int_array(r)?;
        self.mouse_leave_array_params = int_array(r)?;
        self.stat_transmit_filter = int_array(r)?;
        self.key_press_array = int_array(r)?;
        self.mouse_wheel_array = int_array(r)?;
        Ok(())
    }

    fn script(&mut self, r: &mut PayloadReader) -> Result<Script, DecodeError> {
        let count = r.read_u8()?;
        if count == 0 {
            return Ok(None);
        }
        let mut args = Vec::with_capacity(usize::from(count));
        for _ in 0..count {
            args.push(match r.read_u8()? {
                0 => ScriptArg::Int(r.read_i32_be()?),
                1 => ScriptArg::Str(r.read_cp1252()?),
                _ => ScriptArg::Null,
            });
        }
        self.has_script = true;
        Ok(Some(args))
    }
}

fn ushort_or_none(value: u16) -> i32 {
    if value == 65535 {
        -1
    } else {
        i32::from(value)
    }
}

fn int_array(r: &mut PayloadReader) -> Result<Option<Vec<i32>>, DecodeError> {
    let count = r.read_u8()?;
    if count == 0 {
        return Ok(None);
    }
    let mut values = Vec::with_capacity(usize::from(count));
    for _ in 0..count {
        values.push(r.read_i32_be()?);
    }
    Ok(Some(values))
}

fn brand_text(text: String) -> String {
    const NEEDLE: &[u8] = b"runescape";
    if !text.to_lowercase().contains("runescape") {
        return text;
    }
    let bytes = text.as_bytes();
    let mut out = String::with_capacity(text.len());
    let mut i = 0;
    while i < bytes.len() {
        if i + NEEDLE.len() <= bytes.len()
            && bytes[i..i + NEEDLE.len()].eq_ignore_ascii_case(NEEDLE)
        {
            out.push_str("Darkan");
            i += NEEDLE.len();
        } else {
            let ch = text[i..].chars().next().unwrap_or('?');
            out.push(ch);
            i += ch.len_utf8();
        }
    }
    out
}
