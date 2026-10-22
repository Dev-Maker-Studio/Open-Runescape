use crate::packet::{Packet, PacketError};

pub const SCENERY_INTERACT_SHAPE: i32 = 10;
pub const EXAMINE: &str = "Examine";

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParamValue {
    Int(i32),
    Text(String),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Params {
    capacity: usize,
    buckets: Vec<Vec<(i32, ParamValue)>>,
}

impl Params {
    fn new(capacity: usize) -> Self {
        Self {
            capacity,
            buckets: vec![Vec::new(); capacity],
        }
    }

    fn put(&mut self, key: i32, value: ParamValue) -> Result<(), PacketError> {
        if self.capacity == 0 {
            return Err(PacketError::ParameterCapacity);
        }
        let bucket = (key as usize) & (self.capacity - 1);
        self.buckets[bucket].push((key, value));
        Ok(())
    }

    pub fn capacity(&self) -> usize {
        self.capacity
    }

    pub fn iter(&self) -> impl Iterator<Item = &(i32, ParamValue)> {
        self.buckets.iter().flatten()
    }

    pub fn get(&self, key: i32) -> Option<&ParamValue> {
        if self.capacity == 0 {
            return None;
        }
        self.buckets[(key as usize) & (self.capacity - 1)]
            .iter()
            .find(|(candidate, _)| *candidate == key)
            .map(|(_, value)| value)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LocType {
    pub id: i32,
    pub actions: Option<Vec<Option<String>>>,
    pub shapes: Option<Vec<i8>>,
    pub model_ids: Option<Vec<Vec<i32>>>,
    pub name: String,
    pub size_x: i32,
    pub size_y: i32,
    pub blocks_movement: i32,
    pub blocks_projectiles: bool,
    pub has_actions: i32,
    pub delay_shading: bool,
    pub occlusion_mode: i32,
    pub decor_displacement: i32,
    pub inverted: bool,
    pub static_shadow: bool,
    pub force_display_decoration: bool,
    pub ignore_clip_on_alt_route: bool,
    pub supports_items: i32,
    pub sound_id: i32,
    pub sound_radius: i32,
    pub sound_min_interval: i32,
    pub sound_max_interval: i32,
    pub sound_group_ids: Option<Vec<i32>>,
    pub alternative_ids: Option<Vec<i32>>,
    pub requires_textures: bool,
    pub dynamic_shadow: bool,
    pub replay_sequence: bool,
    pub members: bool,
    pub map_icon_rotates: bool,
    pub has_animation: bool,
    pub primary_cursor_action_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_action_index: i32,
    pub secondary_cursor: i32,
    pub map_icon_rotation: i32,
    pub map_icon_id: i32,
    pub sound_volume: i32,
    pub map_icon_flipped: bool,
    pub map_category_id: i32,
    pub quests: Option<Vec<i32>>,
    pub ground_decoration_height: i32,
    pub instrument_sound_effect: bool,
    pub instrument_ambient_sound: bool,
    pub cull_y: i32,
    pub cull_xz: i32,
    pub ambient_sound_max_delay: i32,
    pub ambient_sound_min_delay: i32,
    pub transforms: bool,
    pub ambient_sound_max_hear_distance: i32,
    pub dynamic_tint: bool,
    pub ground_contour_type: i8,
    pub animations: Option<Vec<i32>>,
    pub ambient: i32,
    pub contrast: i32,
    pub recolor_src: Option<Vec<i16>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retexture_src: Option<Vec<i16>>,
    pub retexture_dst: Option<Vec<i16>>,
    pub recolor_palette: Option<Vec<i8>>,
    pub resize_x: i32,
    pub resize_y: i32,
    pub resize_z: i32,
    pub offset_x: i32,
    pub offset_y: i32,
    pub offset_z: i32,
    pub varpbit_id: i32,
    pub varp_id: i32,
    pub ground_contour_modifier: i32,
    pub animation_odds: Option<Vec<i32>>,
    pub tint_hue: i8,
    pub tint_saturation: i8,
    pub tint_lightness: i8,
    pub tint_opacity: i8,
    pub shadow_offset_x: i32,
    pub shadow_offset_y: i32,
    pub shadow_offset_z: i32,
    pub parameters: Option<Params>,
}

impl LocType {
    fn defaults(id: i32) -> Self {
        Self {
            id,
            actions: None,
            shapes: None,
            model_ids: None,
            name: "null".to_string(),
            size_x: 1,
            size_y: 1,
            blocks_movement: 2,
            blocks_projectiles: true,
            has_actions: -1,
            delay_shading: false,
            occlusion_mode: -1,
            decor_displacement: 64,
            inverted: false,
            static_shadow: true,
            force_display_decoration: false,
            ignore_clip_on_alt_route: false,
            supports_items: -1,
            sound_id: -1,
            sound_radius: 0,
            sound_min_interval: 0,
            sound_max_interval: 0,
            sound_group_ids: None,
            alternative_ids: None,
            requires_textures: false,
            dynamic_shadow: true,
            replay_sequence: true,
            members: false,
            map_icon_rotates: false,
            has_animation: false,
            primary_cursor_action_index: -1,
            primary_cursor: -1,
            secondary_cursor_action_index: -1,
            secondary_cursor: -1,
            map_icon_rotation: 0,
            map_icon_id: -1,
            sound_volume: 255,
            map_icon_flipped: false,
            map_category_id: -1,
            quests: None,
            ground_decoration_height: 0,
            instrument_sound_effect: false,
            instrument_ambient_sound: false,
            cull_y: 960,
            cull_xz: 0,
            ambient_sound_max_delay: 256,
            ambient_sound_min_delay: 256,
            transforms: false,
            ambient_sound_max_hear_distance: 0,
            dynamic_tint: false,
            ground_contour_type: 0,
            animations: None,
            ambient: 0,
            contrast: 0,
            recolor_src: None,
            recolor_dst: None,
            retexture_src: None,
            retexture_dst: None,
            recolor_palette: None,
            resize_x: 128,
            resize_y: 128,
            resize_z: 128,
            offset_x: 0,
            offset_y: 0,
            offset_z: 0,
            varpbit_id: -1,
            varp_id: -1,
            ground_contour_modifier: -1,
            animation_odds: None,
            tint_hue: 0,
            tint_saturation: 0,
            tint_lightness: 0,
            tint_opacity: 0,
            shadow_offset_x: 0,
            shadow_offset_y: 0,
            shadow_offset_z: 0,
            parameters: None,
        }
    }

    pub fn param(&self, key: i32) -> Option<&ParamValue> {
        self.parameters.as_ref().and_then(|params| params.get(key))
    }

    fn action_slot(&mut self, index: usize, value: Option<String>) {
        if let Some(actions) = self.actions.as_mut() {
            actions[index] = value;
        }
    }

    fn decode_opcode(
        &mut self,
        packet: &mut Packet<'_>,
        opcode: i32,
        player_is_member: bool,
    ) -> Result<(), PacketError> {
        match opcode {
            1 => {
                let count = packet.u8()? as usize;
                let mut shapes = vec![0i8; count];
                let mut models = vec![Vec::new(); count];
                for shape in 0..count {
                    shapes[shape] = packet.i8()? as i8;
                    let model_count = packet.u8()? as usize;
                    let mut ids = vec![0; model_count];
                    for id in ids.iter_mut() {
                        *id = packet.big_smart()?;
                    }
                    models[shape] = ids;
                }
                self.shapes = Some(shapes);
                self.model_ids = Some(models);
            }
            2 => self.name = packet.string()?,
            14 => self.size_x = packet.u8()?,
            15 => self.size_y = packet.u8()?,
            17 => {
                self.blocks_movement = 0;
                self.blocks_projectiles = false;
            }
            18 => self.blocks_projectiles = false,
            19 => self.has_actions = packet.u8()?,
            21 => self.ground_contour_type = 1,
            22 => self.delay_shading = true,
            23 => self.occlusion_mode = 1,
            24 => {
                let animation = packet.big_smart()?;
                if animation != -1 {
                    self.animations = Some(vec![animation]);
                }
            }
            27 => self.blocks_movement = 1,
            28 => self.decor_displacement = packet.u8()? << 2,
            29 => self.ambient = packet.i8()?,
            39 => self.contrast = packet.i8()? * 5,
            30..=34 => {
                let text = packet.string()?;
                self.action_slot((opcode - 30) as usize, Some(text));
            }
            40 => {
                let count = packet.u8()? as usize;
                let mut source = vec![0i16; count];
                let mut destination = vec![0i16; count];
                for index in 0..count {
                    source[index] = packet.u16()? as u16 as i16;
                    destination[index] = packet.u16()? as u16 as i16;
                }
                self.recolor_src = Some(source);
                self.recolor_dst = Some(destination);
            }
            41 => {
                let count = packet.u8()? as usize;
                let mut source = vec![0i16; count];
                let mut destination = vec![0i16; count];
                for index in 0..count {
                    source[index] = packet.u16()? as u16 as i16;
                    destination[index] = packet.u16()? as u16 as i16;
                }
                self.retexture_src = Some(source);
                self.retexture_dst = Some(destination);
            }
            42 => {
                let count = packet.u8()? as usize;
                let mut palette = vec![0i8; count];
                for entry in palette.iter_mut() {
                    *entry = packet.i8()? as i8;
                }
                self.recolor_palette = Some(palette);
            }
            62 => self.inverted = true,
            64 => self.static_shadow = false,
            65 => self.resize_x = packet.u16()?,
            66 => self.resize_y = packet.u16()?,
            67 => self.resize_z = packet.u16()?,
            69 => {
                packet.u8()?;
            }
            70 => self.offset_x = packet.i16()? << 2,
            71 => self.offset_y = packet.i16()? << 2,
            72 => self.offset_z = packet.i16()? << 2,
            73 => self.force_display_decoration = true,
            74 => self.ignore_clip_on_alt_route = true,
            75 => self.supports_items = packet.u8()?,
            77 | 92 => {
                self.varpbit_id = packet.u16()?;
                if self.varpbit_id == 65535 {
                    self.varpbit_id = -1;
                }
                self.varp_id = packet.u16()?;
                if self.varp_id == 65535 {
                    self.varp_id = -1;
                }
                let mut last = -1;
                if opcode == 92 {
                    last = packet.big_smart()?;
                }
                let count = packet.u8()? as usize;
                let mut alternatives = vec![0; count + 2];
                for slot in alternatives.iter_mut().take(count + 1) {
                    *slot = packet.big_smart()?;
                }
                alternatives[count + 1] = last;
                self.alternative_ids = Some(alternatives);
            }
            78 => {
                self.sound_id = packet.u16()?;
                self.sound_radius = packet.u8()?;
            }
            79 => {
                self.sound_min_interval = packet.u16()?;
                self.sound_max_interval = packet.u16()?;
                self.sound_radius = packet.u8()?;
                let count = packet.u8()? as usize;
                let mut groups = vec![0; count];
                for group in groups.iter_mut() {
                    *group = packet.u16()?;
                }
                self.sound_group_ids = Some(groups);
            }
            81 => {
                self.ground_contour_type = 2;
                self.ground_contour_modifier = packet.u8()? * 256;
            }
            82 => self.requires_textures = true,
            88 => self.dynamic_shadow = false,
            89 => self.replay_sequence = false,
            91 => self.members = true,
            93 => {
                self.ground_contour_type = 3;
                self.ground_contour_modifier = packet.u16()?;
            }
            94 => self.ground_contour_type = 4,
            95 => {
                self.ground_contour_type = 5;
                self.ground_contour_modifier = packet.i16()?;
            }
            97 => self.map_icon_rotates = true,
            98 => self.has_animation = true,
            99 => {
                self.primary_cursor_action_index = packet.u8()?;
                self.primary_cursor = packet.u16()?;
            }
            100 => {
                self.secondary_cursor_action_index = packet.u8()?;
                self.secondary_cursor = packet.u16()?;
            }
            101 => self.map_icon_rotation = packet.u8()?,
            102 => self.map_icon_id = packet.u16()?,
            103 => self.occlusion_mode = 0,
            104 => self.sound_volume = packet.u8()?,
            105 => self.map_icon_flipped = true,
            106 => {
                let count = packet.u8()? as usize;
                let mut sum = 0i32;
                let mut animations = vec![0; count];
                let mut odds = vec![0; count];
                for index in 0..count {
                    animations[index] = packet.big_smart()?;
                    let odd = packet.u8()?;
                    odds[index] = odd;
                    sum += odd;
                }
                for odd in odds.iter_mut() {
                    if sum == 0 {
                        return Err(PacketError::ZeroOddsSum);
                    }
                    *odd = odd.wrapping_mul(65535) / sum;
                }
                self.animations = Some(animations);
                self.animation_odds = Some(odds);
            }
            107 => self.map_category_id = packet.u16()?,
            150..=154 => {
                let text = packet.string()?;
                let value = if player_is_member { Some(text) } else { None };
                self.action_slot((opcode - 150) as usize, value);
            }
            160 => {
                let count = packet.u8()? as usize;
                let mut quests = vec![0; count];
                for quest in quests.iter_mut() {
                    *quest = packet.u16()?;
                }
                self.quests = Some(quests);
            }
            162 => {
                self.ground_contour_type = 3;
                self.ground_contour_modifier = packet.i32()?;
            }
            163 => {
                self.tint_hue = packet.i8()? as i8;
                self.tint_saturation = packet.i8()? as i8;
                self.tint_lightness = packet.i8()? as i8;
                self.tint_opacity = packet.i8()? as i8;
            }
            164 => self.shadow_offset_x = packet.i16()?,
            165 => self.shadow_offset_y = packet.i16()?,
            166 => self.shadow_offset_z = packet.i16()?,
            167 => self.ground_decoration_height = packet.u16()?,
            168 => self.instrument_sound_effect = true,
            169 => self.instrument_ambient_sound = true,
            170 => self.cull_y = packet.usmart()?,
            171 => self.cull_xz = packet.usmart()?,
            173 => {
                self.ambient_sound_max_delay = packet.u16()?;
                self.ambient_sound_min_delay = packet.u16()?;
            }
            177 => self.transforms = true,
            178 => self.ambient_sound_max_hear_distance = packet.u8()?,
            189 => self.dynamic_tint = true,
            249 => {
                let count = packet.u8()? as usize;
                if self.parameters.is_none() {
                    let capacity = if count == 0 {
                        0
                    } else {
                        count.next_power_of_two()
                    };
                    self.parameters = Some(Params::new(capacity));
                }
                for _ in 0..count {
                    let is_text = packet.u8()? == 1;
                    let key = packet.u24()?;
                    let value = if is_text {
                        ParamValue::Text(packet.string()?)
                    } else {
                        ParamValue::Int(packet.i32()?)
                    };
                    if let Some(params) = self.parameters.as_mut() {
                        params.put(key, value)?;
                    }
                }
            }
            _ => {}
        }
        Ok(())
    }

    fn post_decode(&mut self) {
        if self.has_actions == -1 {
            self.has_actions = 0;
            if let Some(shapes) = &self.shapes {
                if shapes.len() == 1 && i32::from(shapes[0]) == SCENERY_INTERACT_SHAPE {
                    self.has_actions = 1;
                }
            }
            if let Some(actions) = &self.actions {
                if actions.iter().take(5).any(Option::is_some) {
                    self.has_actions = 1;
                }
            }
        }
        if self.supports_items == -1 {
            self.supports_items = i32::from(self.blocks_movement != 0);
        }
        if self.animations.is_some() || self.has_animation || self.alternative_ids.is_some() {
            self.transforms = true;
        }
    }
}

pub fn decode_loc_type(
    id: i32,
    data: Option<&[u8]>,
    player_is_member: bool,
) -> Result<LocType, PacketError> {
    let mut loc = LocType::defaults(id);
    let mut actions: Vec<Option<String>> = vec![None; 6];
    actions[5] = Some(EXAMINE.to_string());
    loc.actions = Some(actions);
    if let Some(data) = data {
        let mut packet = Packet::new(data);
        loop {
            let opcode = packet.u8()?;
            if opcode == 0 {
                break;
            }
            loc.decode_opcode(&mut packet, opcode, player_is_member)?;
        }
    }
    loc.post_decode();
    if loc.ignore_clip_on_alt_route {
        loc.blocks_movement = 0;
        loc.blocks_projectiles = false;
    }
    if !player_is_member && loc.members {
        loc.actions = None;
        loc.quests = None;
    }
    Ok(loc)
}

fn text(value: &str) -> String {
    let mut out = String::from("u");
    for (index, unit) in value.encode_utf16().enumerate() {
        if index > 0 {
            out.push('.');
        }
        out.push_str(&format!("{unit:x}"));
    }
    out
}

fn list<T: ToString>(values: &Option<Vec<T>>) -> String {
    match values {
        None => "null".to_string(),
        Some(values) => format!(
            "[{}]",
            values
                .iter()
                .map(ToString::to_string)
                .collect::<Vec<_>>()
                .join(",")
        ),
    }
}

fn flag(value: bool) -> i32 {
    i32::from(value)
}

pub fn canonical(loc: &LocType) -> String {
    let actions = match &loc.actions {
        None => "null".to_string(),
        Some(actions) => format!(
            "[{}]",
            actions
                .iter()
                .map(|action| action.as_deref().map_or("null".to_string(), text))
                .collect::<Vec<_>>()
                .join(",")
        ),
    };
    let models = match &loc.model_ids {
        None => "null".to_string(),
        Some(models) => format!(
            "[{}]",
            models
                .iter()
                .map(|ids| format!(
                    "[{}]",
                    ids.iter()
                        .map(ToString::to_string)
                        .collect::<Vec<_>>()
                        .join(",")
                ))
                .collect::<Vec<_>>()
                .join(",")
        ),
    };
    let parameters = match &loc.parameters {
        None => "null".to_string(),
        Some(params) => format!(
            "[{}]",
            params
                .iter()
                .map(|(key, value)| match value {
                    ParamValue::Int(value) => format!("{key}:i{value}"),
                    ParamValue::Text(value) => format!("{key}:{}", text(value)),
                })
                .collect::<Vec<_>>()
                .join(",")
        ),
    };
    let first = match &loc.parameters {
        None => "null".to_string(),
        Some(params) => {
            let mut seen = Vec::new();
            let mut parts = Vec::new();
            for (key, _) in params.iter() {
                if seen.contains(key) {
                    continue;
                }
                seen.push(*key);
                let value = match params.get(*key) {
                    Some(ParamValue::Int(value)) => format!("i{value}"),
                    Some(ParamValue::Text(value)) => text(value),
                    None => "missing".to_string(),
                };
                parts.push(format!("{key}:{value}"));
            }
            format!("[{}]", parts.join(","))
        }
    };
    let fields: Vec<(&str, String)> = vec![
        ("id", loc.id.to_string()),
        ("name", text(&loc.name)),
        ("actions", actions),
        ("shapes", list(&loc.shapes)),
        ("modelIds", models),
        ("sizeX", loc.size_x.to_string()),
        ("sizeY", loc.size_y.to_string()),
        ("blocksMovement", loc.blocks_movement.to_string()),
        (
            "blocksProjectiles",
            flag(loc.blocks_projectiles).to_string(),
        ),
        ("hasActions", loc.has_actions.to_string()),
        ("delayShading", flag(loc.delay_shading).to_string()),
        ("occlusionMode", loc.occlusion_mode.to_string()),
        ("decorDisplacement", loc.decor_displacement.to_string()),
        ("inverted", flag(loc.inverted).to_string()),
        ("staticShadow", flag(loc.static_shadow).to_string()),
        (
            "forceDisplayDecoration",
            flag(loc.force_display_decoration).to_string(),
        ),
        (
            "ignoreClipOnAltRoute",
            flag(loc.ignore_clip_on_alt_route).to_string(),
        ),
        ("supportsItems", loc.supports_items.to_string()),
        ("soundId", loc.sound_id.to_string()),
        ("soundRadius", loc.sound_radius.to_string()),
        ("soundMinInterval", loc.sound_min_interval.to_string()),
        ("soundMaxInterval", loc.sound_max_interval.to_string()),
        ("soundGroupIds", list(&loc.sound_group_ids)),
        ("alternativeIds", list(&loc.alternative_ids)),
        ("requiresTextures", flag(loc.requires_textures).to_string()),
        ("dynamicShadow", flag(loc.dynamic_shadow).to_string()),
        ("replaySequence", flag(loc.replay_sequence).to_string()),
        ("members", flag(loc.members).to_string()),
        ("mapIconRotates", flag(loc.map_icon_rotates).to_string()),
        ("hasAnimation", flag(loc.has_animation).to_string()),
        (
            "primaryCursorActionIndex",
            loc.primary_cursor_action_index.to_string(),
        ),
        ("primaryCursor", loc.primary_cursor.to_string()),
        (
            "secondaryCursorActionIndex",
            loc.secondary_cursor_action_index.to_string(),
        ),
        ("secondaryCursor", loc.secondary_cursor.to_string()),
        ("mapIconRotation", loc.map_icon_rotation.to_string()),
        ("mapIconId", loc.map_icon_id.to_string()),
        ("soundVolume", loc.sound_volume.to_string()),
        ("mapIconFlipped", flag(loc.map_icon_flipped).to_string()),
        ("mapCategoryId", loc.map_category_id.to_string()),
        ("quests", list(&loc.quests)),
        (
            "groundDecorationHeight",
            loc.ground_decoration_height.to_string(),
        ),
        (
            "instrumentSoundEffect",
            flag(loc.instrument_sound_effect).to_string(),
        ),
        (
            "instrumentAmbientSound",
            flag(loc.instrument_ambient_sound).to_string(),
        ),
        ("cullY", loc.cull_y.to_string()),
        ("cullXZ", loc.cull_xz.to_string()),
        (
            "ambientSoundMaxDelay",
            loc.ambient_sound_max_delay.to_string(),
        ),
        (
            "ambientSoundMinDelay",
            loc.ambient_sound_min_delay.to_string(),
        ),
        ("transforms", flag(loc.transforms).to_string()),
        (
            "ambientSoundMaxHearDistance",
            loc.ambient_sound_max_hear_distance.to_string(),
        ),
        ("dynamicTint", flag(loc.dynamic_tint).to_string()),
        ("groundContourType", loc.ground_contour_type.to_string()),
        ("animations", list(&loc.animations)),
        ("ambient", loc.ambient.to_string()),
        ("contrast", loc.contrast.to_string()),
        ("recolorSrc", list(&loc.recolor_src)),
        ("recolorDst", list(&loc.recolor_dst)),
        ("retextureSrc", list(&loc.retexture_src)),
        ("retextureDst", list(&loc.retexture_dst)),
        ("recolorPalette", list(&loc.recolor_palette)),
        ("resizeX", loc.resize_x.to_string()),
        ("resizeY", loc.resize_y.to_string()),
        ("resizeZ", loc.resize_z.to_string()),
        ("offsetX", loc.offset_x.to_string()),
        ("offsetY", loc.offset_y.to_string()),
        ("offsetZ", loc.offset_z.to_string()),
        ("varpbitId", loc.varpbit_id.to_string()),
        ("varpId", loc.varp_id.to_string()),
        (
            "groundContourModifier",
            loc.ground_contour_modifier.to_string(),
        ),
        ("animationOdds", list(&loc.animation_odds)),
        ("tintHue", loc.tint_hue.to_string()),
        ("tintSaturation", loc.tint_saturation.to_string()),
        ("tintLightness", loc.tint_lightness.to_string()),
        ("tintOpacity", loc.tint_opacity.to_string()),
        ("shadowOffsetX", loc.shadow_offset_x.to_string()),
        ("shadowOffsetY", loc.shadow_offset_y.to_string()),
        ("shadowOffsetZ", loc.shadow_offset_z.to_string()),
        ("parameters", parameters),
        ("parameterFirst", first),
    ];
    fields
        .into_iter()
        .map(|(name, value)| format!("{name}={value}"))
        .collect::<Vec<_>>()
        .join("\t")
}
